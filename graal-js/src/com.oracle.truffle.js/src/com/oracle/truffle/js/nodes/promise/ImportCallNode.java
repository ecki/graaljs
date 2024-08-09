/*
 * Copyright (c) 2019, 2024, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.nodes.promise;

import java.util.Map;
import java.util.Set;

import com.oracle.js.parser.ir.Module.ModuleRequest;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.EncapsulatingNodeReference;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.EnumerableOwnPropertyNamesNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.access.PropertySetNode;
import com.oracle.truffle.js.nodes.arguments.AccessIndexedArgumentNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.control.TryCatchNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSContext.BuiltinFunctionKey;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JavaScriptRealmBoundaryRootNode;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.builtins.JSPromise;
import com.oracle.truffle.js.runtime.builtins.JSPromiseObject;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSModuleRecord;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.PromiseCapabilityRecord;
import com.oracle.truffle.js.runtime.objects.ScriptOrModule;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.UnmodifiableArrayList;

/**
 * Represents the import call expression syntax: {@code import(specifier)}.
 */
public class ImportCallNode extends JavaScriptNode {

    private static final HiddenKey PROMISE_CAPABILITY_KEY = new HiddenKey("%promiseCapability");
    private static final HiddenKey LINK_AND_EVALUATE_KEY = new HiddenKey("%linkAndEvaluate");
    private static final TruffleString ASSERT = Strings.constant("assert");

    @Child private JavaScriptNode argRefNode;
    private final ScriptOrModule activeScriptOrModule;
    @Child private NewPromiseCapabilityNode newPromiseCapabilityNode;
    @Child private JSToStringNode toStringNode;
    @Child private PromiseReactionJobNode promiseReactionJobNode;
    @Child private JavaScriptNode optionsRefNode;

    // lazily initialized
    @Child private JSFunctionCallNode callRejectNode;
    @Child private TryCatchNode.GetErrorObjectNode getErrorObjectNode;
    @Child private EnumerableOwnPropertyNamesNode enumerableOwnPropertyNamesNode;
    @Child private PropertyGetNode getWithNode;
    @Child private PropertyGetNode getAssertNode;

    private final JSContext context;

    protected ImportCallNode(JSContext context, JavaScriptNode argRefNode, ScriptOrModule activeScriptOrModule, JavaScriptNode optionsRefNode) {
        this.context = context;
        this.argRefNode = argRefNode;
        this.activeScriptOrModule = activeScriptOrModule;
        this.optionsRefNode = optionsRefNode;
        this.newPromiseCapabilityNode = NewPromiseCapabilityNode.create(context);
        this.toStringNode = JSToStringNode.create();
        this.promiseReactionJobNode = PromiseReactionJobNode.create(context);
    }

    public static ImportCallNode create(JSContext context, JavaScriptNode argRefNode, ScriptOrModule activeScriptOrModule) {
        return new ImportCallNode(context, argRefNode, activeScriptOrModule, null);
    }

    public static ImportCallNode createWithOptions(JSContext context, JavaScriptNode specifierRefNode, ScriptOrModule activeScriptOrModule, JavaScriptNode optionsRefNode) {
        return new ImportCallNode(context, specifierRefNode, activeScriptOrModule, optionsRefNode);
    }

    @NeverDefault
    public static ImportCallNode create(JSContext context) {
        return create(context, null, null);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        ScriptOrModule referencingScriptOrModule = activeScriptOrModule;
        Object specifier = argRefNode.execute(frame);
        if (optionsRefNode != null) {
            return executeAttributes(frame, referencingScriptOrModule, specifier);
        } else {
            return executeWithoutAttributes(referencingScriptOrModule, specifier);
        }
    }

    private Object executeWithoutAttributes(ScriptOrModule referencingScriptOrModule, Object specifier) {
        PromiseCapabilityRecord promiseCapability = newPromiseCapability();
        TruffleString specifierString;
        try {
            specifierString = toStringNode.executeString(specifier);
        } catch (AbstractTruffleException ex) {
            return rejectPromise(promiseCapability, ex);
        }
        return hostImportModuleDynamicallyWithSite(referencingScriptOrModule, ModuleRequest.create(specifierString), promiseCapability);
    }

    @SuppressWarnings("unchecked")
    private Object executeAttributes(VirtualFrame frame, ScriptOrModule referencingScriptOrModule, Object specifier) {
        assert optionsRefNode != null;
        if (enumerableOwnPropertyNamesNode == null || getWithNode == null || getAssertNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            enumerableOwnPropertyNamesNode = insert(EnumerableOwnPropertyNamesNode.createKeys(context));
            getWithNode = insert(PropertyGetNode.create(Strings.WITH, context));
            getAssertNode = insert(PropertyGetNode.create(ASSERT, context));
        }
        Object options = optionsRefNode.execute(frame);
        PromiseCapabilityRecord promiseCapability = newPromiseCapability();
        TruffleString specifierString;
        try {
            specifierString = toStringNode.executeString(specifier);
        } catch (AbstractTruffleException ex) {
            return rejectPromise(promiseCapability, ex);
        }
        Map.Entry<TruffleString, TruffleString>[] attributes = null;
        if (options != Undefined.instance) {
            if (!JSRuntime.isObject(options)) {
                return rejectPromiseWithTypeError(promiseCapability, "The second argument to import() must be an object");
            }
            Object attributesObj = Undefined.instance;
            try {
                if (context.getLanguageOptions().importAttributes()) {
                    attributesObj = getWithNode.getValue(options);
                }
                if (attributesObj == Undefined.instance) {
                    if (context.getLanguageOptions().importAssertions()) {
                        attributesObj = getAssertNode.getValue(options);
                    }
                }
            } catch (AbstractTruffleException ex) {
                return rejectPromise(promiseCapability, ex);
            }
            if (attributesObj != Undefined.instance) {
                if (!(attributesObj instanceof JSObject obj)) {
                    return rejectPromiseWithTypeError(promiseCapability, "The 'assert' option must be an object");
                }
                UnmodifiableArrayList<? extends Object> keys;
                try {
                    keys = enumerableOwnPropertyNamesNode.execute(obj);
                } catch (AbstractTruffleException ex) {
                    return rejectPromise(promiseCapability, ex);
                }
                attributes = (Map.Entry<TruffleString, TruffleString>[]) new Map.Entry<?, ?>[keys.size()];
                boolean allStrings = true;
                for (int i = 0; i < keys.size(); i++) {
                    TruffleString key = (TruffleString) keys.get(i);
                    Object value;
                    try {
                        value = JSObject.get(obj, key);
                    } catch (AbstractTruffleException ex) {
                        return rejectPromise(promiseCapability, ex);
                    }
                    if (value instanceof TruffleString valueStr) {
                        attributes[i] = Boundaries.mapEntry(key, valueStr);
                    } else {
                        // Read all values before rejecting the promise,
                        // we were supposed to do EnumerableOwnProperties(KEY+VALUE) above.
                        allStrings = false;
                    }
                }
                if (!allStrings) {
                    return rejectPromiseWithTypeError(promiseCapability, "Import assertion value must be a string");
                }
            }
        }
        ModuleRequest moduleRequest = attributes == null ? ModuleRequest.create(specifierString) : createModuleRequestWithAttributes(specifierString, attributes);
        return hostImportModuleDynamicallyWithSite(referencingScriptOrModule, moduleRequest, promiseCapability);
    }

    private JSDynamicObject hostImportModuleDynamicallyWithSite(ScriptOrModule referrer, ModuleRequest moduleRequest, PromiseCapabilityRecord promiseCapability) {
        EncapsulatingNodeReference current = EncapsulatingNodeReference.getCurrent();
        Node prev = current.set(this);
        try {
            return hostImportModuleDynamically(referrer, moduleRequest, promiseCapability);
        } finally {
            current.set(prev);
        }
    }

    @TruffleBoundary
    private static ModuleRequest createModuleRequestWithAttributes(TruffleString specifierString, Map.Entry<TruffleString, TruffleString>[] attributes) {
        return ModuleRequest.create(specifierString, attributes);
    }

    public final JSDynamicObject hostImportModuleDynamically(ScriptOrModule referencingScriptOrModule, ModuleRequest moduleRequest, PromiseCapabilityRecord promiseCapability) {
        JSRealm realm = getRealm();
        if (context.hasImportModuleDynamicallyCallbackBeenSet()) {
            JSDynamicObject promise = context.hostImportModuleDynamically(realm, referencingScriptOrModule, moduleRequest);
            if (promise == null) {
                return rejectPromise(promiseCapability, createTypeErrorCannotImport(moduleRequest.specifier()));
            }
            assert JSPromise.isJSPromise(promise);
            return promise;
        } else {
            var payload = new ContinueDynamicImportPayload(promiseCapability, createContinueDynamicImportHandler(realm));
            context.getEvaluator().hostLoadImportedModule(realm, referencingScriptOrModule, moduleRequest, Undefined.instance, payload);
            return promiseCapability.getPromise();
        }
    }

    private PromiseCapabilityRecord newPromiseCapability() {
        if (newPromiseCapabilityNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            newPromiseCapabilityNode = insert(NewPromiseCapabilityNode.create(context));
        }
        return newPromiseCapabilityNode.executeDefault();
    }

    private JSDynamicObject rejectPromise(PromiseCapabilityRecord promiseCapability, AbstractTruffleException ex) {
        if (callRejectNode == null || getErrorObjectNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            callRejectNode = insert(JSFunctionCallNode.createCall());
            getErrorObjectNode = insert(TryCatchNode.GetErrorObjectNode.create(context));
        }
        Object error = getErrorObjectNode.execute(ex);
        callRejectNode.executeCall(JSArguments.createOneArg(Undefined.instance, promiseCapability.getReject(), error));
        return promiseCapability.getPromise();
    }

    private Object rejectPromiseWithTypeError(PromiseCapabilityRecord promiseCapability, String errorMessage) {
        if (callRejectNode == null) {
            // Just to cut off before createTypeError. Nodes are initialized in rejectPromise().
            CompilerDirectives.transferToInterpreterAndInvalidate();
        }
        return rejectPromise(promiseCapability, Errors.createTypeError(errorMessage, this));
    }

    @TruffleBoundary
    private static JSException createTypeErrorCannotImport(TruffleString specifier) {
        return Errors.createError("Cannot dynamically import module: " + specifier);
    }

    /**
     * Payload to be passed via HostLoadImportedModule to FinishLoadingImportedModule.
     */
    public record ContinueDynamicImportPayload(
                    PromiseCapabilityRecord promiseCapability,
                    JSFunctionObject continueDynamicImportCallback) {
    }

    /**
     * Captures of linkAndEvaluateClosure.
     */
    private record LinkAndEvaluateArgs(
                    JSModuleRecord moduleRecord,
                    PromiseCapabilityRecord promiseCapability,
                    JSFunctionObject onRejected) {
    }

    /**
     * Compilable helper function used to implement ContinueDynamicImport; it is passed as the
     * payload to HostLoadImportedModule together with the promiseCapability, and will be called
     * with the module result on normal completion.
     */
    private JSFunctionObject createContinueDynamicImportHandler(JSRealm realm) {
        JSFunctionData functionData = context.getOrCreateBuiltinFunctionData(BuiltinFunctionKey.ContinueDynamicImport, (c) -> createContinueDynamicImportHandlerImpl(c));
        return JSFunction.create(realm, functionData);
    }

    /**
     * Handles ContinueDynamicImport with normal module completion.
     */
    private static JSFunctionData createContinueDynamicImportHandlerImpl(JSContext context) {
        class ContinueDynamicImportRootNode extends JavaScriptRealmBoundaryRootNode {
            @Child protected JavaScriptNode promiseCapabilityArgument = AccessIndexedArgumentNode.create(0);
            @Child protected JavaScriptNode moduleRecordArgument = AccessIndexedArgumentNode.create(1);

            @Child private PerformPromiseThenNode promiseThenNode = PerformPromiseThenNode.create(context);
            @Child private PropertySetNode setPromiseCapability = PropertySetNode.createSetHidden(PROMISE_CAPABILITY_KEY, context);
            @Child private PropertySetNode setLinkAndEvaluateCaptures = PropertySetNode.createSetHidden(LINK_AND_EVALUATE_KEY, context);

            protected ContinueDynamicImportRootNode(JavaScriptLanguage lang) {
                super(lang);
            }

            @Override
            public Object executeInRealm(VirtualFrame frame) {
                PromiseCapabilityRecord importPromiseCapability = (PromiseCapabilityRecord) promiseCapabilityArgument.execute(frame);
                JSModuleRecord module = (JSModuleRecord) moduleRecordArgument.execute(frame);
                JSRealm realm = getRealm();

                JSPromiseObject loadPromise = (JSPromiseObject) context.getEvaluator().loadRequestedModules(realm, module, Undefined.instance).getPromise();
                JSFunctionObject onRejected = createOnRejectedClosure(context, realm, importPromiseCapability);
                JSFunctionObject linkAndEvaluate = createLinkAndEvaluateClosure(context, realm, module, importPromiseCapability, onRejected);

                promiseThenNode.execute(loadPromise, linkAndEvaluate, onRejected);
                return Undefined.instance;
            }

            private JSFunctionObject createOnRejectedClosure(JSContext cx, JSRealm realm, PromiseCapabilityRecord promiseCapability) {
                JSFunctionData functionData = cx.getOrCreateBuiltinFunctionData(BuiltinFunctionKey.ContinueDynamicImportRejectedClosure, (c) -> createOnRejectedImpl(c));
                JSFunctionObject rejectedClosure = JSFunction.create(realm, functionData);
                setPromiseCapability.setValue(rejectedClosure, promiseCapability);
                return rejectedClosure;
            }

            private JSFunctionObject createLinkAndEvaluateClosure(JSContext cx, JSRealm realm, JSModuleRecord module, PromiseCapabilityRecord promiseCapability, JSFunctionObject onRejected) {
                JSFunctionData functionData = cx.getOrCreateBuiltinFunctionData(BuiltinFunctionKey.ContinueDynamicImportLinkAndEvaluateClosure, (c) -> createLinkAndEvaluateImpl(c));
                JSFunctionObject linkAndEvaluateClosure = JSFunction.create(realm, functionData);
                setLinkAndEvaluateCaptures.setValue(linkAndEvaluateClosure, new LinkAndEvaluateArgs(module, promiseCapability, onRejected));
                return linkAndEvaluateClosure;
            }
        }

        JavaScriptRootNode root = new ContinueDynamicImportRootNode(context.getLanguage());
        return JSFunctionData.createCallOnly(context, root.getCallTarget(), 0, Strings.EMPTY_STRING);
    }

    private static JSFunctionData createLinkAndEvaluateImpl(JSContext context) {
        class LinkAndEvaluateRootNode extends JavaScriptRealmBoundaryRootNode {

            @Child private PropertyGetNode getCaptures = PropertyGetNode.createGetHidden(LINK_AND_EVALUATE_KEY, context);
            @Child private PropertySetNode setCaptures = PropertySetNode.createSetHidden(LINK_AND_EVALUATE_KEY, context);

            @Child private PerformPromiseThenNode promiseThenNode = PerformPromiseThenNode.create(context);
            @Child private JSFunctionCallNode callPromiseResolve = JSFunctionCallNode.createCall();
            @Child private JSFunctionCallNode callPromiseReject;
            @Child private TryCatchNode.GetErrorObjectNode getErrorObjectNode;

            protected LinkAndEvaluateRootNode(JavaScriptLanguage lang) {
                super(lang);
            }

            @Override
            protected Object executeInRealm(VirtualFrame frame) {
                JSDynamicObject thisFunction = (JSDynamicObject) JSArguments.getFunctionObject(frame.getArguments());
                LinkAndEvaluateArgs captures = (LinkAndEvaluateArgs) getCaptures.getValue(thisFunction);
                JSModuleRecord moduleRecord = captures.moduleRecord;
                PromiseCapabilityRecord importPromiseCapability = captures.promiseCapability;
                JSFunctionObject onRejected = captures.onRejected;

                JSRealm realm = getRealm();
                assert realm == JSFunction.getRealm(JSFrameUtil.getFunctionObject(frame));
                try {
                    // If link is an abrupt completion, reject the promise from import().
                    context.getEvaluator().moduleLinking(realm, moduleRecord);

                    // Evaluate() should always return a promise.
                    // Yet, if top-level-await is disabled, returns/throws the result instead.
                    Object evaluatePromise = context.getEvaluator().moduleEvaluation(realm, moduleRecord);
                    if (context.isOptionTopLevelAwait()) {
                        assert evaluatePromise instanceof JSPromiseObject : evaluatePromise;
                        JSFunctionObject onFulfilled = createFulfilledClosure(context, realm, captures);
                        promiseThenNode.execute((JSPromiseObject) evaluatePromise, onFulfilled, onRejected);
                    } else {
                        // Rethrow any previous execution errors.
                        moduleRecord.getExecutionResultOrThrow();
                        var namespace = context.getEvaluator().getModuleNamespace(moduleRecord);
                        callPromiseResolve.executeCall(JSArguments.createOneArg(Undefined.instance, importPromiseCapability.getResolve(), namespace));
                    }
                } catch (AbstractTruffleException ex) {
                    rejectPromise(importPromiseCapability, ex);
                }
                return Undefined.instance;
            }

            private JSFunctionObject createFulfilledClosure(JSContext cx, JSRealm realm, LinkAndEvaluateArgs captures) {
                JSFunctionData functionData = cx.getOrCreateBuiltinFunctionData(BuiltinFunctionKey.ContinueDynamicImportFulfilledClosure, (c) -> createOnFulfilledImpl(c));
                JSFunctionObject closure = JSFunction.create(realm, functionData);
                setCaptures.setValue(closure, captures);
                return closure;
            }

            private void rejectPromise(PromiseCapabilityRecord moduleLoadedCapability, AbstractTruffleException ex) {
                if (getErrorObjectNode == null || callPromiseReject == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    getErrorObjectNode = insert(TryCatchNode.GetErrorObjectNode.create(context));
                    callPromiseReject = insert(JSFunctionCallNode.createCall());
                }
                Object errorObject = getErrorObjectNode.execute(ex);
                callPromiseReject.executeCall(JSArguments.createOneArg(Undefined.instance, moduleLoadedCapability.getReject(), errorObject));
            }
        }
        return JSFunctionData.createCallOnly(context, new LinkAndEvaluateRootNode(context.getLanguage()).getCallTarget(), 0, Strings.EMPTY_STRING);
    }

    private static JSFunctionData createOnFulfilledImpl(JSContext cx) {
        class FinishDynamicImportNormalRootNode extends JavaScriptRootNode {
            @Child private PropertyGetNode getCaptures = PropertyGetNode.createGetHidden(LINK_AND_EVALUATE_KEY, cx);
            @Child private JSFunctionCallNode callPromiseResolve = JSFunctionCallNode.createCall();

            @Override
            public Object execute(VirtualFrame frame) {
                JSFunctionObject thisFunction = (JSFunctionObject) JSArguments.getFunctionObject(frame.getArguments());
                LinkAndEvaluateArgs captures = (LinkAndEvaluateArgs) getCaptures.getValue(thisFunction);
                PromiseCapabilityRecord promiseCapability = captures.promiseCapability;
                JSModuleRecord moduleRecord = captures.moduleRecord;

                var namespace = cx.getEvaluator().getModuleNamespace(moduleRecord);
                callPromiseResolve.executeCall(JSArguments.createOneArg(Undefined.instance, promiseCapability.getResolve(), namespace));
                return Undefined.instance;
            }
        }
        return JSFunctionData.createCallOnly(cx, new FinishDynamicImportNormalRootNode().getCallTarget(), 0, Strings.EMPTY_STRING);
    }

    private static JSFunctionData createOnRejectedImpl(JSContext cx) {
        class RejectDynamicImportRootNode extends JavaScriptRootNode {
            @Child protected JavaScriptNode reasonArgument = AccessIndexedArgumentNode.create(0);
            @Child private PropertyGetNode getPromiseCapability = PropertyGetNode.createGetHidden(PROMISE_CAPABILITY_KEY, cx);
            @Child private JSFunctionCallNode callPromiseReject = JSFunctionCallNode.createCall();

            @Override
            public Object execute(VirtualFrame frame) {
                Object reason = reasonArgument.execute(frame);
                JSFunctionObject thisFunction = (JSFunctionObject) JSArguments.getFunctionObject(frame.getArguments());
                PromiseCapabilityRecord promiseCapability = (PromiseCapabilityRecord) getPromiseCapability.getValue(thisFunction);

                callPromiseReject.executeCall(JSArguments.createOneArg(Undefined.instance, promiseCapability.getReject(), reason));
                return Undefined.instance;
            }
        }
        return JSFunctionData.createCallOnly(cx, new RejectDynamicImportRootNode().getCallTarget(), 1, Strings.EMPTY_STRING);
    }

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        if (optionsRefNode == null) {
            return ImportCallNode.create(context, cloneUninitialized(argRefNode, materializedTags), activeScriptOrModule);
        } else {
            return ImportCallNode.createWithOptions(context, cloneUninitialized(argRefNode, materializedTags), activeScriptOrModule,
                            cloneUninitialized(optionsRefNode, materializedTags));
        }
    }
}
