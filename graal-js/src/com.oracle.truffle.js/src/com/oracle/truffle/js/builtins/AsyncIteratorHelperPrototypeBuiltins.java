/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayDeque;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.CreateIterResultObjectNode;
import com.oracle.truffle.js.nodes.access.CreateObjectNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.access.PropertySetNode;
import com.oracle.truffle.js.nodes.control.AsyncGeneratorAwaitReturnNode;
import com.oracle.truffle.js.nodes.control.TryCatchNode;
import com.oracle.truffle.js.nodes.function.InternalCallNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.promise.NewPromiseCapabilityNode;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.objects.AsyncGeneratorRequest;
import com.oracle.truffle.js.runtime.objects.Completion;
import com.oracle.truffle.js.runtime.objects.IteratorRecord;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.PromiseCapabilityRecord;
import com.oracle.truffle.js.runtime.objects.Undefined;

public class AsyncIteratorHelperPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<AsyncIteratorHelperPrototypeBuiltins.HelperIteratorPrototype> {

    public static final JSBuiltinsContainer BUILTINS = new AsyncIteratorHelperPrototypeBuiltins();

    public static final TruffleString PROTOTYPE_NAME = Strings.constant("AsyncIteratorHelper.prototype");

    public static final TruffleString CLASS_NAME = Strings.constant("AsyncIteratorHelper");

    public static final TruffleString TO_STRING_TAG = Strings.constant("Async Iterator Helper");

    private static final HiddenKey ITERATED_ID = new HiddenKey("Iterated");
    public static final HiddenKey IMPL_ID = new HiddenKey("ResumptionTarget");

    protected AsyncIteratorHelperPrototypeBuiltins() {
        super(PROTOTYPE_NAME, AsyncIteratorHelperPrototypeBuiltins.HelperIteratorPrototype.class);
    }

    public enum HelperIteratorPrototype implements BuiltinEnum<AsyncIteratorHelperPrototypeBuiltins.HelperIteratorPrototype> {
        next(0),
        return_(0);

        private final int length;

        HelperIteratorPrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, AsyncIteratorHelperPrototypeBuiltins.HelperIteratorPrototype builtinEnum) {
        switch (builtinEnum) {
            case next:
                return AsyncIteratorHelperPrototypeBuiltinsFactory.AsyncIteratorHelperNextNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case return_:
                return AsyncIteratorHelperPrototypeBuiltinsFactory.IteratorHelperReturnNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
        }
        return null;
    }

    protected static class CreateAsyncIteratorHelperNode extends JavaScriptBaseNode {
        @Child private CreateObjectNode.CreateObjectWithPrototypeNode createObjectNode;
        @Child private PropertySetNode setIteratedNode;
        @Child private PropertySetNode setGeneratorResumptionTargetNode;
        @Child private PropertySetNode setGeneratorState;
        @Child private PropertySetNode setGeneratorQueue;
        @Child private PropertySetNode setThisNode;

        public CreateAsyncIteratorHelperNode(JSContext context) {
            this.createObjectNode = CreateObjectNode.createOrdinaryWithPrototype(context);
            this.setIteratedNode = PropertySetNode.createSetHidden(ITERATED_ID, context);
            this.setGeneratorResumptionTargetNode = PropertySetNode.createSetHidden(IMPL_ID, context);

            this.setGeneratorState = PropertySetNode.createSetHidden(JSFunction.ASYNC_GENERATOR_STATE_ID, context);
            this.setGeneratorQueue = PropertySetNode.createSetHidden(JSFunction.ASYNC_GENERATOR_QUEUE_ID, context);
            this.setThisNode = PropertySetNode.createSetHidden(AsyncIteratorPrototypeBuiltins.AsyncIteratorAwaitNode.THIS_ID, context);
        }

        public JSDynamicObject execute(IteratorRecord iterated, JSFunctionObject start) {
            JSDynamicObject iterator = createObjectNode.execute(getRealm().getAsyncIteratorHelperPrototype());
            setIteratedNode.setValue(iterator, iterated);
            setGeneratorResumptionTargetNode.setValue(iterator, start);
            setGeneratorState.setValue(iterator, JSFunction.AsyncGeneratorState.SuspendedStart);
            setGeneratorQueue.setValue(iterator, new ArrayDeque<AsyncGeneratorRequest>(4));

            setThisNode.setValue(start, iterator);
            return iterator;
        }

        public static CreateAsyncIteratorHelperNode create(JSContext context) {
            return new CreateAsyncIteratorHelperNode(context);
        }
    }

    protected abstract static class AsyncIteratorHelperResumeNode extends JSBuiltinNode {
        @Child private NewPromiseCapabilityNode newPromiseCapabilityNode;
        @Child private PropertyGetNode getGeneratorResumptionTargetNode;
        @Child protected PropertyGetNode getAsyncGeneratorStateNode;
        @Child private PropertyGetNode getAsyncGeneratorQueueNode;
        @Child private InternalCallNode internalCallNode;
        @Child private JSFunctionCallNode callNode;
        @Child private TryCatchNode.GetErrorObjectNode getErrorObjectNode;

        protected AsyncIteratorHelperResumeNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);

            this.newPromiseCapabilityNode = NewPromiseCapabilityNode.create(context);
            this.getGeneratorResumptionTargetNode = PropertyGetNode.createGetHidden(IMPL_ID, context);
            this.getAsyncGeneratorStateNode = PropertyGetNode.createGetHidden(JSFunction.ASYNC_GENERATOR_STATE_ID, context);
            this.getAsyncGeneratorQueueNode = PropertyGetNode.createGetHidden(JSFunction.ASYNC_GENERATOR_QUEUE_ID, context);
            this.internalCallNode = InternalCallNode.create();
            this.callNode = JSFunctionCallNode.createCall();
        }

        protected Object validateAndResume(VirtualFrame frame, Object thisObj) {
            PromiseCapabilityRecord promiseCapability = newPromiseCapabilityNode.executeDefault();
            try {
                // AsyncGeneratorValidate(generator, brand)
                // We assume that the 'brand' field being there implies all the fields are there.
                if (!JSObject.isJSObject(thisObj) || getGeneratorResumptionTargetNode.getValue(thisObj) == Undefined.instance) {
                    throw Errors.createTypeErrorIncompatibleReceiver(thisObj);
                }
                JSObject generator = (JSObject) thisObj;
                performNextOrReturn(frame, generator, promiseCapability);
            } catch (AbstractTruffleException ex) {
                if (getErrorObjectNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    getErrorObjectNode = insert(TryCatchNode.GetErrorObjectNode.create(getContext()));
                }
                Object error = getErrorObjectNode.execute(ex);
                callNode.executeCall(JSArguments.createOneArg(Undefined.instance, promiseCapability.getReject(), error));
            }
            return promiseCapability.getPromise();
        }

        protected abstract void performNextOrReturn(VirtualFrame frame, JSObject generator, PromiseCapabilityRecord promiseCapability);

        protected final void performResumeNext(JSDynamicObject iterator, Completion completion, JSFunction.AsyncGeneratorState state) {
            assert state == JSFunction.AsyncGeneratorState.SuspendedStart || state == JSFunction.AsyncGeneratorState.SuspendedYield : state;
            JSFunctionObject resumptionClosure = (JSFunctionObject) getGeneratorResumptionTargetNode.getValue(iterator);
            ArrayDeque<AsyncGeneratorRequest> queue = getAsyncGeneratorQueue(iterator);
            assert !queue.isEmpty();

            CallTarget resumptionTarget = JSFunction.getCallTarget(resumptionClosure);
            internalCallNode.execute(resumptionTarget, JSArguments.createOneArg(iterator, resumptionClosure, completion));
        }

        @SuppressWarnings("unchecked")
        protected final ArrayDeque<AsyncGeneratorRequest> getAsyncGeneratorQueue(JSDynamicObject iterator) {
            return (ArrayDeque<AsyncGeneratorRequest>) getAsyncGeneratorQueueNode.getValue(iterator);
        }
    }

    public abstract static class IteratorHelperReturnNode extends AsyncIteratorHelperResumeNode {

        @Child private AsyncGeneratorAwaitReturnNode asyncGeneratorAwaitReturnNode;

        protected IteratorHelperReturnNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);

            this.asyncGeneratorAwaitReturnNode = AsyncGeneratorAwaitReturnNode.create(context);
        }

        @Specialization
        @Override
        protected final Object validateAndResume(VirtualFrame frame, Object thisObj) {
            return super.validateAndResume(frame, thisObj);
        }

        @Override
        protected final void performNextOrReturn(VirtualFrame frame, JSObject generator, PromiseCapabilityRecord promiseCapability) {
            Completion completion = Completion.forReturn(Undefined.instance);
            // Perform AsyncGeneratorEnqueue(generator, completion, promiseCapability).
            ArrayDeque<AsyncGeneratorRequest> queue = getAsyncGeneratorQueue(generator);
            AsyncGeneratorRequest request = AsyncGeneratorRequest.create(completion, promiseCapability);
            Boundaries.queueAdd(queue, request);
            JSFunction.AsyncGeneratorState state = (JSFunction.AsyncGeneratorState) getAsyncGeneratorStateNode.getValue(generator);
            if (state == JSFunction.AsyncGeneratorState.SuspendedStart || state == JSFunction.AsyncGeneratorState.Completed) {
                // Perform ! AsyncGeneratorAwaitReturn(generator).
                asyncGeneratorAwaitReturnNode.executeAsyncGeneratorAwaitReturn(frame, generator, queue);
            } else if (state == JSFunction.AsyncGeneratorState.SuspendedYield) {
                // Perform AsyncGeneratorResume(generator, completion).
                performResumeNext(generator, completion, state);
            } else {
                assert state == JSFunction.AsyncGeneratorState.Executing || state == JSFunction.AsyncGeneratorState.AwaitingReturn;
            }
        }
    }

    public abstract static class AsyncIteratorHelperNextNode extends AsyncIteratorHelperResumeNode {
        @Child private JSFunctionCallNode callResolveNode;

        @Child private CreateIterResultObjectNode createIterResultObjectNode;

        protected AsyncIteratorHelperNextNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);

            this.callResolveNode = JSFunctionCallNode.createCall();
            this.getAsyncGeneratorStateNode = PropertyGetNode.createGetHidden(JSFunction.ASYNC_GENERATOR_STATE_ID, context);
            this.createIterResultObjectNode = CreateIterResultObjectNode.create(context);
        }

        @Specialization
        @Override
        protected final Object validateAndResume(VirtualFrame frame, Object thisObj) {
            return super.validateAndResume(frame, thisObj);
        }

        @Override
        protected final void performNextOrReturn(VirtualFrame frame, JSObject generator, PromiseCapabilityRecord promiseCapability) {
            JSFunction.AsyncGeneratorState state = (JSFunction.AsyncGeneratorState) getAsyncGeneratorStateNode.getValue(generator);
            if (state == JSFunction.AsyncGeneratorState.Completed) {
                Object iteratorResult = createIterResultObjectNode.execute(frame, Undefined.instance, true);
                callResolveNode.executeCall(JSArguments.createOneArg(Undefined.instance, promiseCapability.getResolve(), iteratorResult));
            } else {
                Completion completion = Completion.forNormal(Undefined.instance);
                ArrayDeque<AsyncGeneratorRequest> queue = getAsyncGeneratorQueue(generator);
                AsyncGeneratorRequest request = AsyncGeneratorRequest.create(completion, promiseCapability);
                Boundaries.queueAdd(queue, request);
                if (state == JSFunction.AsyncGeneratorState.SuspendedStart || state == JSFunction.AsyncGeneratorState.SuspendedYield) {
                    performResumeNext(generator, completion, state);
                } else {
                    assert state == JSFunction.AsyncGeneratorState.Executing || state == JSFunction.AsyncGeneratorState.AwaitingReturn;
                }
            }
        }
    }
}
