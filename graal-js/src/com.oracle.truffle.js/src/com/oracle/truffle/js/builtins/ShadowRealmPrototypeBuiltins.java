/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.builtins;

import com.oracle.js.parser.ir.Module.ModuleRequest;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.ShadowRealmPrototypeBuiltinsFactory.GetWrappedValueNodeGen;
import com.oracle.truffle.js.builtins.ShadowRealmPrototypeBuiltinsFactory.ShadowRealmEvaluateNodeGen;
import com.oracle.truffle.js.builtins.ShadowRealmPrototypeBuiltinsFactory.ShadowRealmImportValueNodeGen;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.ScriptNode;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.access.JSHasPropertyNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.access.PropertySetNode;
import com.oracle.truffle.js.nodes.access.ReadElementNode;
import com.oracle.truffle.js.nodes.arguments.AccessIndexedArgumentNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.promise.ImportCallNode;
import com.oracle.truffle.js.nodes.promise.NewPromiseCapabilityNode;
import com.oracle.truffle.js.nodes.promise.PerformPromiseThenNode;
import com.oracle.truffle.js.nodes.unary.IsCallableNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.Evaluator;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.builtins.JSShadowRealm;
import com.oracle.truffle.js.runtime.builtins.JSShadowRealmObject;
import com.oracle.truffle.js.runtime.objects.PromiseCapabilityRecord;

/**
 * Contains built-in functions of the {@code %ShadowRealm.prototype%}.
 */
public final class ShadowRealmPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<ShadowRealmPrototypeBuiltins.ShadowRealmPrototype> {

    public static final JSBuiltinsContainer BUILTINS = new ShadowRealmPrototypeBuiltins();

    protected ShadowRealmPrototypeBuiltins() {
        super(JSShadowRealm.PROTOTYPE_NAME, ShadowRealmPrototype.class);
    }

    public enum ShadowRealmPrototype implements BuiltinEnum<ShadowRealmPrototype> {
        evaluate(1),
        importValue(2);

        private final int length;

        ShadowRealmPrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, ShadowRealmPrototype builtinEnum) {
        switch (builtinEnum) {
            case evaluate:
                return ShadowRealmEvaluateNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case importValue:
                return ShadowRealmImportValueNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
        }
        return null;
    }

    abstract static class GetWrappedValueNode extends JavaScriptBaseNode {

        abstract Object execute(JSContext context, JSRealm callerRealm, Object value);

        @Specialization(guards = "isCallable.executeBoolean(value)", limit = "1")
        protected final Object objectCallable(JSContext context, JSRealm callerRealm, Object value,
                        @Cached @Shared("isCallable") @SuppressWarnings("unused") IsCallableNode isCallable) {
            return wrappedFunctionCreate(context, callerRealm, value);
        }

        private Object wrappedFunctionCreate(JSContext context, JSRealm callerRealm, Object target) {
            CompilerAsserts.partialEvaluationConstant(context);
            JSFunctionData functionData = context.getOrCreateBuiltinFunctionData(JSContext.BuiltinFunctionKey.OrdinaryWrappedFunctionCall, ShadowRealmPrototypeBuiltins::createWrappedFunctionImpl);
            return JSFunction.createWrapped(context, callerRealm, functionData, target);
        }

        @Specialization(guards = {"isObject.executeBoolean(value)", "!isCallable.executeBoolean(value)"}, limit = "1")
        protected final Object objectNotCallable(@SuppressWarnings("unused") JSContext context, @SuppressWarnings("unused") JSRealm callerRealm, Object value,
                        @Cached @Shared("isObject") @SuppressWarnings("unused") IsObjectNode isObject,
                        @Cached @Shared("isCallable") @SuppressWarnings("unused") IsCallableNode isCallable) {
            throw Errors.createTypeErrorNotAFunction(value, this);
        }

        @Specialization(guards = {"!isObject.executeBoolean(value)"}, limit = "1")
        protected static Object primitive(@SuppressWarnings("unused") JSContext context, @SuppressWarnings("unused") JSRealm callerRealm, Object value,
                        @Cached @Shared("isObject") @SuppressWarnings("unused") IsObjectNode isObject) {
            return value;
        }

        public static GetWrappedValueNode create() {
            return GetWrappedValueNodeGen.create();
        }
    }

    private static JSFunctionData createWrappedFunctionImpl(JSContext context) {
        final class WrappedFunctionRootNode extends JavaScriptRootNode {
            @Child private JSFunctionCallNode callWrappedTargetFunction = JSFunctionCallNode.createCall();
            @Child private GetWrappedValueNode getWrappedValue = GetWrappedValueNode.create();

            @Override
            public Object execute(VirtualFrame frame) {
                Object[] args = frame.getArguments();
                JSFunctionObject.Wrapped functionObject = (JSFunctionObject.Wrapped) JSArguments.getFunctionObject(args);
                Object target = functionObject.getWrappedTargetFunction();
                assert JSRuntime.isCallable(target) : target;
                JSRealm callerRealm = functionObject.getRealm();
                JSRealm targetRealm = JSRuntime.getFunctionRealm(target, callerRealm);
                int argCount = JSArguments.getUserArgumentCount(args);
                Object wrappedThisArgument = getWrappedValue.execute(context, targetRealm, JSArguments.getThisObject(args));
                Object[] wrappedArgs = JSArguments.createInitial(wrappedThisArgument, target, argCount);
                for (int i = 0; i < argCount; i++) {
                    JSArguments.setUserArgument(wrappedArgs, i, getWrappedValue.execute(context, targetRealm, JSArguments.getUserArgument(args, i)));
                }
                Object result;
                try {
                    JSRealm mainRealm = JSRealm.getMain(this);
                    JSRealm prevRealm = mainRealm.enterRealm(this, targetRealm);
                    try {
                        result = callWrappedTargetFunction.executeCall(wrappedArgs);
                    } finally {
                        mainRealm.leaveRealm(this, prevRealm);
                    }
                } catch (AbstractTruffleException ex) {
                    throw wrapErrorFromShadowRealm(ex);
                }
                return getWrappedValue.execute(context, callerRealm, result);
            }

            @TruffleBoundary
            private JSException wrapErrorFromShadowRealm(AbstractTruffleException ex) {
                return Errors.createTypeError(String.format("Wrapped function call failed with: %s", ex.getMessage()), ex, this);
            }
        }
        return JSFunctionData.createCallOnly(context, new WrappedFunctionRootNode().getCallTarget(), 0, Strings.EMPTY_STRING);
    }

    @ImportStatic(JSShadowRealm.class)
    public abstract static class ShadowRealmEvaluateNode extends JSBuiltinNode {

        public ShadowRealmEvaluateNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @TruffleBoundary
        @Specialization
        protected Object evaluate(JSShadowRealmObject thisObj, TruffleString sourceText,
                        @Cached IndirectCallNode callNode,
                        @Cached GetWrappedValueNode getWrappedValue) {
            JSRealm callerRealm = getRealm();
            JSRealm evalRealm = thisObj.getShadowRealm();
            getContext().checkEvalAllowed();
            var script = parseScript(Strings.toJavaString(sourceText));
            Object result;
            try {
                JSRealm mainRealm = JSRealm.getMain(this);
                JSRealm prevRealm = mainRealm.enterRealm(this, evalRealm);
                try {
                    result = script.runEval(callNode, evalRealm);
                } finally {
                    mainRealm.leaveRealm(this, prevRealm);
                }
            } catch (AbstractTruffleException ex) {
                throw wrapErrorFromShadowRealm(ex);
            }
            return getWrappedValue.execute(getContext(), callerRealm, result);
        }

        private JSException wrapErrorFromShadowRealm(AbstractTruffleException ex) {
            CompilerAsserts.neverPartOfCompilation();
            return Errors.createTypeError(String.format("ShadowRealm.prototype.evaluate failed with: %s", ex.getMessage()), ex, this);
        }

        private ScriptNode parseScript(String sourceCode) {
            CompilerAsserts.neverPartOfCompilation();
            assert !getContext().getContextOptions().isDisableEval();
            Source source = Source.newBuilder(JavaScriptLanguage.ID, sourceCode, Evaluator.EVAL_SOURCE_NAME).build();
            return getContext().getEvaluator().parseEval(getContext(), this, source);
        }

        @TruffleBoundary
        @Specialization(guards = "!isString(sourceText)")
        protected Object invalidSourceText(@SuppressWarnings("unused") JSShadowRealmObject thisObj, @SuppressWarnings("unused") Object sourceText) {
            throw Errors.createTypeErrorNotAString(sourceText);
        }

        @TruffleBoundary
        @Specialization(guards = "!isJSShadowRealm(thisObj)")
        protected Object invalidReceiver(Object thisObj, @SuppressWarnings("unused") Object sourceText) {
            throw Errors.createTypeErrorIncompatibleReceiver(getBuiltin().getFullName(), thisObj);
        }
    }

    @ImportStatic(JSShadowRealm.class)
    public abstract static class ShadowRealmImportValueNode extends JSBuiltinNode {
        protected static final HiddenKey EXPORT_NAME_STRING = new HiddenKey("ExportNameString");

        public ShadowRealmImportValueNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @TruffleBoundary
        @Specialization
        protected Object importValue(@SuppressWarnings("unused") JSShadowRealmObject thisObj, Object specifier, Object exportName,
                        @Cached JSToStringNode toStringNode,
                        @Cached("create(getContext())") NewPromiseCapabilityNode newPromiseCapabilityNode,
                        @Cached("create(getContext())") PerformPromiseThenNode performPromiseThenNode,
                        @Cached("createSetHidden(EXPORT_NAME_STRING, getContext())") PropertySetNode setExportNameStringNode,
                        @Cached("create(getContext())") ImportCallNode importNode) {
            TruffleString specifierString = toStringNode.executeString(specifier);
            if (!JSGuards.isString(exportName)) {
                throw Errors.createTypeErrorNotAString(exportName);
            }
            TruffleString exportNameString = (TruffleString) exportName;
            JSRealm callerRealm = getRealm();
            JSRealm evalRealm = thisObj.getShadowRealm();

            PromiseCapabilityRecord innerCapability = newPromiseCapabilityNode.executeDefault();
            JSRealm mainRealm = JSRealm.getMain(this);
            JSRealm prevRealm = mainRealm.enterRealm(this, evalRealm);
            try {
                importNode.hostImportModuleDynamically(null, ModuleRequest.create(specifierString), innerCapability);
            } finally {
                mainRealm.leaveRealm(this, prevRealm);
            }

            JSFunctionData functionData = getContext().getOrCreateBuiltinFunctionData(JSContext.BuiltinFunctionKey.ExportGetter, ShadowRealmImportValueNode::createExportGetterImpl);
            var onFulfilled = JSFunction.create(callerRealm, functionData);
            setExportNameStringNode.setValue(onFulfilled, exportNameString);
            PromiseCapabilityRecord promiseCapability = newPromiseCapabilityNode.executeDefault();
            return performPromiseThenNode.execute(innerCapability.getPromise(), onFulfilled, callerRealm.getThrowerFunction(), promiseCapability);
        }

        private static JSFunctionData createExportGetterImpl(JSContext context) {
            final class ExportGetterRootNode extends JavaScriptRootNode {
                @Child private JavaScriptNode argumentNode = AccessIndexedArgumentNode.create(0);
                @Child private PropertyGetNode getExportNameString = PropertyGetNode.createGetHidden(EXPORT_NAME_STRING, context);
                @Child private JSHasPropertyNode hasOwnProperty = JSHasPropertyNode.create(true);
                @Child private ReadElementNode getExport = ReadElementNode.create(context);
                @Child private GetWrappedValueNode getWrappedValue = GetWrappedValueNode.create();

                @Override
                public Object execute(VirtualFrame frame) {
                    JSFunctionObject functionObject = JSFrameUtil.getFunctionObject(frame);
                    TruffleString exportNameString = (TruffleString) getExportNameString.getValue(functionObject);
                    Object exports = argumentNode.execute(frame);
                    if (!hasOwnProperty.executeBoolean(exports, exportNameString)) {
                        throw Errors.createTypeErrorCannotGetProperty(context, exportNameString, exports, false, this);
                    }
                    Object value = getExport.executeWithTargetAndIndex(exports, exportNameString);
                    return getWrappedValue.execute(context, functionObject.getRealm(), value);
                }
            }
            return JSFunctionData.createCallOnly(context, new ExportGetterRootNode().getCallTarget(), 1, Strings.EMPTY_STRING);
        }

        @TruffleBoundary
        @Specialization(guards = "!isJSShadowRealm(thisObj)")
        protected Object invalidReceiver(Object thisObj, @SuppressWarnings("unused") Object specifier, @SuppressWarnings("unused") Object exportName) {
            throw Errors.createTypeErrorIncompatibleReceiver(getBuiltin().getFullName(), thisObj);
        }
    }
}
