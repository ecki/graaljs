/*
 * Copyright (c) 2018, 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oracle.truffle.js.builtins.helper;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;
import com.oracle.truffle.api.profiles.InlinedCountingConditionProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.helper.JSRegExpExecIntlNodeGen.BuildGroupsObjectNodeGen;
import com.oracle.truffle.js.builtins.helper.JSRegExpExecIntlNodeGen.JSRegExpExecBuiltinNodeGen;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.IsJSClassNode;
import com.oracle.truffle.js.nodes.access.IsJSObjectNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.access.PropertySetNode;
import com.oracle.truffle.js.nodes.cast.JSToLengthNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.unary.IsCallableNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSArrayObject;
import com.oracle.truffle.js.runtime.builtins.JSObjectFactory;
import com.oracle.truffle.js.runtime.builtins.JSRegExp;
import com.oracle.truffle.js.runtime.builtins.JSRegExpObject;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.JSProperty;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.TRegexUtil;

/**
 * Implements ES6 21.2.5.2.1 Runtime Semantics: RegExpExec ( R, S ).
 */
public abstract class JSRegExpExecIntlNode extends JavaScriptBaseNode {

    static final int LIMIT = 3;

    private final JSContext context;
    @Child private JSRegExpExecBuiltinNode regExpBuiltinNode;
    @Child private PropertyGetNode getExecNode;
    @Child private IsJSObjectNode isJSObjectNode;
    @Child private IsPristineObjectNode isPristineObjectNode;
    @Child private IsCallableNode isCallableNode = IsCallableNode.create();
    @Child private JSFunctionCallNode specialCallNode;

    JSRegExpExecIntlNode(JSContext context) {
        this.context = context;
    }

    @NeverDefault
    public static JSRegExpExecIntlNode create(JSContext context) {
        return JSRegExpExecIntlNodeGen.create(context);
    }

    public abstract Object execute(JSDynamicObject regExp, Object input);

    @Specialization
    Object doGeneric(JSDynamicObject regExp, TruffleString input,
                    @Cached InlinedConditionProfile isPristineProfile,
                    @Cached InlinedConditionProfile isCallableProfile,
                    @Cached InlinedConditionProfile validResultProfile,
                    @Cached InlinedConditionProfile isRegExpProfile) {
        if (context.getEcmaScriptVersion() >= 6) {
            if (isPristineProfile.profile(this, isPristine(regExp))) {
                return builtinExec((JSRegExpObject) regExp, input);
            } else {
                Object exec = getExecProperty(regExp);
                if (isCallableProfile.profile(this, isCallable(exec))) {
                    return callJSFunction(regExp, input, exec, validResultProfile);
                }
            }
        }
        if (!isRegExpProfile.profile(this, JSRegExp.isJSRegExp(regExp))) {
            throw Errors.createTypeError("RegExp expected");
        }
        return builtinExec((JSRegExpObject) regExp, input);
    }

    private Object callJSFunction(JSDynamicObject regExp, Object input, Object exec, InlinedConditionProfile validResultProfile) {
        Object result = doCallJSFunction(exec, regExp, input);
        if (validResultProfile.profile(this, result == Null.instance || isJSObject(result) && result != Undefined.instance)) {
            return result;
        }
        throw Errors.createTypeError("object or null expected");
    }

    private boolean isPristine(JSDynamicObject regExp) {
        if (isPristineObjectNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            isPristineObjectNode = insert(IsPristineObjectNode.createRegExpExecAndMatch(context));
        }
        return isPristineObjectNode.execute(regExp);
    }

    private Object getExecProperty(JSDynamicObject regExp) {
        if (getExecNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            getExecNode = insert(PropertyGetNode.create(Strings.EXEC, false, context));
        }
        return getExecNode.getValue(regExp);
    }

    private boolean isCallable(Object obj) {
        if (isCallableNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            isCallableNode = insert(IsCallableNode.create());
        }
        return isCallableNode.executeBoolean(obj);
    }

    private Object builtinExec(JSRegExpObject regExp, TruffleString input) {
        if (regExpBuiltinNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            regExpBuiltinNode = insert(JSRegExpExecBuiltinNode.create(context));
        }
        return regExpBuiltinNode.execute(regExp, input);
    }

    private boolean isJSObject(Object obj) {
        if (isJSObjectNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            isJSObjectNode = insert(IsJSObjectNode.create());
        }
        return isJSObjectNode.executeBoolean(obj);
    }

    private Object doCallJSFunction(Object exec, JSDynamicObject regExp, Object input) {
        if (specialCallNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            specialCallNode = insert(JSFunctionCallNode.createCall());
        }
        return specialCallNode.executeCall(JSArguments.createOneArg(regExp, exec, input));
    }

    @NeverDefault
    public static IsJSClassNode createIsJSRegExpNode() {
        return IsJSClassNode.create(JSRegExp.INSTANCE);
    }

    /**
     * Ignores the {@code lastIndex} and {@code global} properties of the RegExp during matching.
     */
    @ImportStatic({JSRegExp.class, JSRegExpExecIntlNode.class})
    public abstract static class JSRegExpExecIntlIgnoreLastIndexNode extends JavaScriptBaseNode {

        private final JSContext context;
        private final boolean doStaticResultUpdate;

        JSRegExpExecIntlIgnoreLastIndexNode(JSContext context, boolean doStaticResultUpdate) {
            this.context = context;
            this.doStaticResultUpdate = doStaticResultUpdate;
        }

        public static JSRegExpExecIntlIgnoreLastIndexNode create(JSContext context, boolean doStaticResultUpdate) {
            return JSRegExpExecIntlNodeGen.JSRegExpExecIntlIgnoreLastIndexNodeGen.create(context, doStaticResultUpdate);
        }

        public abstract Object execute(JSDynamicObject regExp, Object input, long lastIndex);

        @SuppressWarnings("truffle-static-method")
        @Specialization(guards = "getCompiledRegex(regExp) == cachedCompiledRegex", limit = "LIMIT")
        Object doCached(JSDynamicObject regExp, TruffleString input, long lastIndex,
                        @Cached("getCompiledRegex(regExp)") Object cachedCompiledRegex,
                        @Bind("this") Node node,
                        @Cached @Shared("legacyFeaturesEnabled") InlinedConditionProfile areLegacyFeaturesEnabled,
                        @Cached @Shared("compiledRegexAccessor") TRegexUtil.TRegexCompiledRegexAccessor compiledRegexAccessor,
                        @Cached @Shared("regexResultAccessor") TRegexUtil.TRegexResultAccessor regexResultAccessor) {
            return doExec(regExp, input, lastIndex, cachedCompiledRegex, compiledRegexAccessor, regexResultAccessor, node, areLegacyFeaturesEnabled);
        }

        @Specialization
        Object doGeneric(JSDynamicObject regExp, TruffleString input, long lastIndex,
                        @Cached @Shared("legacyFeaturesEnabled") InlinedConditionProfile areLegacyFeaturesEnabled,
                        @Cached @Shared("compiledRegexAccessor") TRegexUtil.TRegexCompiledRegexAccessor compiledRegexAccessor,
                        @Cached @Shared("regexResultAccessor") TRegexUtil.TRegexResultAccessor regexResultAccessor) {
            Object compiledRegex = JSRegExp.getCompiledRegex(regExp);
            return doExec(regExp, input, lastIndex, compiledRegex, compiledRegexAccessor, regexResultAccessor, this, areLegacyFeaturesEnabled);
        }

        private Object doExec(JSDynamicObject regExp, TruffleString input, long lastIndex, Object compiledRegex,
                        TRegexUtil.TRegexCompiledRegexAccessor compiledRegexAccessor,
                        TRegexUtil.TRegexResultAccessor regexResultAccessor,
                        Node node, InlinedConditionProfile areLegacyFeaturesEnabled) {
            Object result = executeCompiledRegex(compiledRegex, input, lastIndex, compiledRegexAccessor);
            if (doStaticResultUpdate && context.isOptionRegexpStaticResult() && regexResultAccessor.isMatch(result)) {
                JSRealm thisRealm = getRealm();
                if (thisRealm == JSRegExp.getRealm(regExp)) {
                    if (areLegacyFeaturesEnabled.profile(node, JSRegExp.getLegacyFeaturesEnabled(regExp))) {
                        thisRealm.setStaticRegexResult(context, compiledRegex, input, lastIndex, result);
                    } else {
                        thisRealm.invalidateStaticRegexResult();
                    }
                }
            }
            return result;
        }
    }

    @ImportStatic({JSRegExp.class, JSRegExpExecIntlNode.class})
    public abstract static class BuildGroupsObjectNode extends JavaScriptBaseNode {

        public static BuildGroupsObjectNode create() {
            return BuildGroupsObjectNodeGen.create();
        }

        public abstract JSDynamicObject execute(JSContext context, JSDynamicObject regExp, Object regexResult, Object input, boolean isIndices);

        // We can reuse the cachedGroupsFactory even if the new groups factory is different, as long
        // as the compiledRegex is the same. This can happen if a new RegExp instance is repeatedly
        // created for the same regular expression.
        @Specialization(guards = "getGroupsFactory(regExp) == cachedGroupsFactory || getCompiledRegex(regExp) == cachedCompiledRegex", limit = "LIMIT")
        final JSDynamicObject doCachedGroupsFactory(JSContext context,
                        @SuppressWarnings("unused") JSDynamicObject regExp,
                        Object regexResult,
                        TruffleString input,
                        boolean isIndices,
                        @Cached("getCompiledRegex(regExp)") @SuppressWarnings("unused") Object cachedCompiledRegex,
                        @Cached("getGroupsFactory(regExp)") JSObjectFactory cachedGroupsFactory,
                        @Cached("createIsJSRegExpNode()") @SuppressWarnings("unused") IsJSClassNode isJSRegExpNode) {
            return doIt(context, getRealm(), cachedGroupsFactory, regexResult, input, isIndices);
        }

        @Specialization
        @TruffleBoundary
        final JSDynamicObject doVaryingGroupsFactory(JSContext context, JSDynamicObject regExp, Object regexResult, TruffleString input, boolean isIndices) {
            return doIt(context, getRealm(), JSRegExp.getGroupsFactory(regExp), regexResult, input, isIndices);
        }

        private static JSDynamicObject doIt(JSContext context, JSRealm realm, JSObjectFactory groupsFactory, Object regexResult, TruffleString input, boolean isIndices) {
            if (groupsFactory == null) {
                return Undefined.instance;
            } else {
                return JSRegExp.createGroupsObject(context, realm, groupsFactory, regexResult, input, isIndices);
            }
        }
    }

    // implements ES6 21.2.5.2.2 Runtime Semantics: RegExpBuiltinExec ( R, S )
    @ImportStatic({JSRegExp.class, JSRegExpExecIntlNode.class})
    public abstract static class JSRegExpExecBuiltinNode extends JavaScriptBaseNode {

        private final JSContext context;
        @Child private PropertySetNode setRegexResultNode;
        @Child private PropertySetNode setRegexOriginalInputNode;
        @Child private DynamicObjectLibrary setInputNode;
        @Child private DynamicObjectLibrary setIndexNode;
        @Child private DynamicObjectLibrary setGroupsNode;
        @Child private DynamicObjectLibrary setIndicesNode;
        @Child private DynamicObjectLibrary setIndicesRegexResultNode;
        @Child private DynamicObjectLibrary setIndicesGroupsNode;
        private final int ecmaScriptVersion;
        @Child private JSToLengthNode toLengthNode;
        @Child private PropertyGetNode getLastIndexNode;
        @Child private PropertySetNode setLastIndexNode;
        @Child private TRegexUtil.TRegexCompiledRegexAccessor compiledRegexAccessor = TRegexUtil.TRegexCompiledRegexAccessor.create();
        @Child private TRegexUtil.TRegexFlagsAccessor flagsAccessor = TRegexUtil.TRegexFlagsAccessor.create();
        @Child private TRegexUtil.TRegexResultAccessor regexResultAccessor = TRegexUtil.TRegexResultAccessor.create();
        @Child private BuildGroupsObjectNode groupsBuilder;

        JSRegExpExecBuiltinNode(JSContext context) {
            this.context = context;
            this.ecmaScriptVersion = context.getEcmaScriptVersion();
            this.setRegexResultNode = PropertySetNode.createSetHidden(JSArray.LAZY_REGEX_RESULT_ID, context);
            this.setRegexOriginalInputNode = PropertySetNode.createSetHidden(JSArray.LAZY_REGEX_ORIGINAL_INPUT_ID, context);
            this.setInputNode = JSObjectUtil.createDispatched(JSRegExp.INPUT);
            this.setIndexNode = JSObjectUtil.createDispatched(JSRegExp.INDEX);
            this.setGroupsNode = JSObjectUtil.createDispatched(JSRegExp.GROUPS);
            this.setIndicesNode = JSObjectUtil.createDispatched(JSRegExp.INDICES);
            this.setIndicesRegexResultNode = JSObjectUtil.createDispatched(JSArray.LAZY_REGEX_RESULT_ID);
            this.setIndicesGroupsNode = JSObjectUtil.createDispatched(JSRegExp.GROUPS);
        }

        public static JSRegExpExecBuiltinNode create(JSContext context) {
            return JSRegExpExecBuiltinNodeGen.create(context);
        }

        private Object getEmptyResult() {
            return ecmaScriptVersion >= 6 ? Null.instance : context.getTRegexEmptyResult();
        }

        public abstract Object execute(JSRegExpObject regExp, TruffleString input);

        @SuppressWarnings("truffle-static-method")
        @Specialization(guards = "getCompiledRegex(regExp) == cachedCompiledRegex", limit = "LIMIT")
        Object doCached(JSRegExpObject regExp, TruffleString input,
                        @Bind("this") Node node,
                        @Cached("getCompiledRegex(regExp)") Object cachedCompiledRegex,
                        @Cached @Shared("invalidLastIndex") InlinedConditionProfile invalidLastIndex,
                        @Cached @Shared("match") InlinedCountingConditionProfile match,
                        @Cached @Shared("legacyFeaturesEnabled") InlinedConditionProfile areLegacyFeaturesEnabled) {
            return doExec(regExp, cachedCompiledRegex, input,
                            node, invalidLastIndex, match, areLegacyFeaturesEnabled);
        }

        @Specialization(replaces = "doCached")
        Object doDynamic(JSRegExpObject regExp, TruffleString input,
                        @Bind("this") Node node,
                        @Cached @Shared("invalidLastIndex") InlinedConditionProfile invalidLastIndex,
                        @Cached @Shared("match") InlinedCountingConditionProfile match,
                        @Cached @Shared("legacyFeaturesEnabled") InlinedConditionProfile areLegacyFeaturesEnabled) {
            return doExec(regExp, JSRegExp.getCompiledRegex(regExp), input,
                            node, invalidLastIndex, match, areLegacyFeaturesEnabled);
        }

        // Implements 21.2.5.2.2 Runtime Semantics: RegExpBuiltinExec ( R, S )
        private Object doExec(JSRegExpObject regExp, Object compiledRegex, TruffleString input,
                        Node node, InlinedConditionProfile invalidLastIndex, InlinedCountingConditionProfile match, InlinedConditionProfile areLegacyFeaturesEnabled) {
            Object flags = compiledRegexAccessor.flags(compiledRegex);
            boolean global = flagsAccessor.global(flags);
            boolean sticky = ecmaScriptVersion >= 6 && flagsAccessor.sticky(flags);
            boolean hasIndices = flagsAccessor.hasIndices(flags);
            long lastIndex = getLastIndex(regExp);
            if (global || sticky) {
                if (invalidLastIndex.profile(node, lastIndex < 0 || lastIndex > Strings.length(input))) {
                    setLastIndex(regExp, 0);
                    return getEmptyResult();
                }
            } else {
                lastIndex = 0;
            }

            JSRealm thisRealm = getRealm();
            Object result = executeCompiledRegex(compiledRegex, input, lastIndex, compiledRegexAccessor);
            if (context.isOptionRegexpStaticResult() && regexResultAccessor.isMatch(result)) {
                if (isSameRealm(regExp, thisRealm)) {
                    if (areLegacyFeaturesEnabled.profile(node, JSRegExp.getLegacyFeaturesEnabled(regExp))) {
                        thisRealm.setStaticRegexResult(context, compiledRegex, input, lastIndex, result);
                    } else {
                        thisRealm.invalidateStaticRegexResult();
                    }
                }
            }
            if (match.profile(node, regexResultAccessor.isMatch(result))) {
                assert !sticky || regexResultAccessor.captureGroupStart(result, 0) == lastIndex;
                if (global || sticky) {
                    setLastIndex(regExp, regexResultAccessor.captureGroupEnd(result, 0));
                }
                if (ecmaScriptVersion < 6) {
                    return result;
                }
                int groupCount = compiledRegexAccessor.groupCount(compiledRegex);
                return getMatchResult(regExp, result, groupCount, input, hasIndices, thisRealm);
            } else {
                if (ecmaScriptVersion < 8 || global || sticky) {
                    setLastIndex(regExp, 0);
                }
                return getEmptyResult();
            }
        }

        private boolean isSameRealm(JSRegExpObject regExp, JSRealm thisRealm) {
            if (context.isSingleRealm()) {
                assert JSRegExp.getRealm(regExp) == thisRealm;
                return true;
            }
            return JSRegExp.getRealm(regExp) == thisRealm;
        }

        // converts RegexResult into JSDynamicObject
        private JSArrayObject getMatchResult(JSRegExpObject regExp, Object regexResult, int groupCount, TruffleString inputStr, boolean hasIndices, JSRealm realm) {
            JSDynamicObject groups = getGroupsObject(regExp, regexResult, inputStr, false);
            JSArrayObject resultArray = JSArray.createLazyRegexArray(context, realm, groupCount);
            setRegexResultNode.setValue(resultArray, regexResult);
            setRegexOriginalInputNode.setValue(resultArray, inputStr);
            setIndexNode.putConstant(resultArray, JSRegExp.INDEX, JSRegExp.LAZY_INDEX_PROXY, JSAttributes.getDefault() | JSProperty.PROXY);
            setInputNode.put(resultArray, JSRegExp.INPUT, inputStr);
            setGroupsNode.put(resultArray, JSRegExp.GROUPS, groups);
            if (context.isOptionRegexpMatchIndices() && hasIndices) {
                JSDynamicObject indicesGroups = getGroupsObject(regExp, regexResult, inputStr, true);
                JSArrayObject indicesArray = JSArray.createLazyRegexIndicesArray(context, realm, groupCount);
                setIndicesRegexResultNode.put(indicesArray, JSRegExp.GROUPS_RESULT_ID, regexResult);
                setIndicesGroupsNode.put(indicesArray, JSRegExp.GROUPS, indicesGroups);
                setIndicesNode.put(resultArray, JSRegExp.INDICES, indicesArray);
            }
            return resultArray;
        }

        // builds the object containing the matches of the named capture groups
        private JSDynamicObject getGroupsObject(JSDynamicObject regExp, Object result, Object input, boolean isIndices) {
            if (groupsBuilder == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                groupsBuilder = insert(BuildGroupsObjectNode.create());
            }
            return groupsBuilder.execute(context, regExp, result, input, isIndices);
        }

        private long getLastIndex(JSDynamicObject regExp) {
            if (getLastIndexNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getLastIndexNode = insert(PropertyGetNode.create(JSRegExp.LAST_INDEX, false, context));
            }
            Object lastIndex = getLastIndexNode.getValue(regExp);
            if (ecmaScriptVersion < 6) {
                return JSRuntime.toInteger(lastIndex);
            } else {
                if (toLengthNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    toLengthNode = insert(JSToLengthNode.create());
                }
                return toLengthNode.executeLong(lastIndex);
            }
        }

        private void setLastIndex(JSDynamicObject regExp, int value) {
            if (setLastIndexNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                setLastIndexNode = insert(PropertySetNode.create(JSRegExp.LAST_INDEX, false, context, true));
            }
            setLastIndexNode.setValueInt(regExp, value);
        }
    }

    private static Object executeCompiledRegex(Object compiledRegex, Object input, long fromIndex,
                    TRegexUtil.TRegexCompiledRegexAccessor compiledRegexAccessor) {
        return compiledRegexAccessor.exec(compiledRegex, input, fromIndex);
    }
}
