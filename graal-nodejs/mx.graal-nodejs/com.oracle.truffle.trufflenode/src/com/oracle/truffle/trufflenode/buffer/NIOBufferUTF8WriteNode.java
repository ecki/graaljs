/*
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.trufflenode.buffer;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferObject;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferView;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.builtins.JSTypedArrayObject;
import com.oracle.truffle.trufflenode.GraalJSAccess;

public abstract class NIOBufferUTF8WriteNode extends NIOBufferAccessNode {

    protected final BranchProfile nativePath = BranchProfile.create();
    protected final BranchProfile errorBranch = BranchProfile.create();
    protected final BranchProfile interopBranch = BranchProfile.create();

    public NIOBufferUTF8WriteNode(JSContext context, JSBuiltin builtin) {
        super(context, builtin);
    }

    private JSFunctionObject getNativeUtf8Write() {
        return GraalJSAccess.getRealmEmbedderData(getRealm()).getNativeUtf8Write();
    }

    @Specialization
    public Object write(JSTypedArrayObject target, TruffleString str, int destOffset, int bytes) {
        try {
            return doWrite(target, str, destOffset, bytes);
        } catch (CharacterCodingException e) {
            return doNativeFallback(target, str, destOffset, bytes);
        }
    }

    @Specialization(guards = {"isUndefined(bytes)"})
    public Object writeDefaultOffset(JSTypedArrayObject target, TruffleString str, int destOffset, Object bytes) {
        try {
            return doWrite(target, str, destOffset, getBytes(str).length);
        } catch (CharacterCodingException e) {
            return doNativeFallback(target, str, destOffset, bytes);
        }
    }

    @Specialization(guards = {"isUndefined(destOffset)", "isUndefined(bytes)"})
    public Object writeDefaultValues(JSTypedArrayObject target, TruffleString str, Object destOffset, Object bytes) {
        try {
            return doWrite(target, str, 0, getBytes(str).length);
        } catch (CharacterCodingException e) {
            return doNativeFallback(target, str, destOffset, bytes);
        }
    }

    @Specialization
    public Object write(JSTypedArrayObject target, TruffleString str, double destOffset, double bytes) {
        try {
            return doWrite(target, str, (int) destOffset, (int) bytes);
        } catch (CharacterCodingException e) {
            return doNativeFallback(target, str, destOffset, bytes);
        }
    }

    @Specialization
    public Object writeDefault(JSTypedArrayObject target, Object str, Object destOffset, Object bytes) {
        return JSFunction.call(getNativeUtf8Write(), target, new Object[]{str, destOffset, bytes});
    }

    @Specialization(guards = {"!isJSArrayBufferView(target)"})
    @SuppressWarnings("unused")
    public Object writeAbort(Object target, Object str, Object destOffset, Object bytes) {
        throw Errors.createTypeErrorArrayBufferViewExpected();
    }

    private Object doNativeFallback(JSTypedArrayObject target, TruffleString str, Object destOffset, Object bytes) {
        nativePath.enter();
        return JSFunction.call(getNativeUtf8Write(), target, new Object[]{str, destOffset, bytes});
    }

    private int doWrite(JSTypedArrayObject target, TruffleString str, int destOffset, int bytes) throws CharacterCodingException {
        JSArrayBufferObject arrayBuffer = JSArrayBufferView.getArrayBuffer(target);
        int bufferOffset = getOffset(target);
        int bufferLen = getLength(target);

        if (destOffset < 0) {
            errorBranch.enter();
            throw indexOutOfRange();
        } else if (destOffset > bufferLen) {
            errorBranch.enter();
            throw offsetOutOfBounds();
        }
        if (bytes < 0) {
            errorBranch.enter();
            throw indexOutOfRange();
        }

        ByteBuffer rawBuffer = getDirectByteBuffer(arrayBuffer);
        boolean interopBuffer = false;
        if (rawBuffer == null) {
            interopBranch.enter();
            interopBuffer = true;
            rawBuffer = interopArrayBufferGetContents(arrayBuffer);
        }
        int destLimit = destOffset + bytes;
        if (destLimit > bufferLen || destLimit < 0) {
            destLimit = bufferLen;
        }
        ByteBuffer buffer = Boundaries.byteBufferSlice(rawBuffer, bufferOffset + destOffset, bufferOffset + destLimit);
        doEncode(str, buffer);
        if (interopBuffer) {
            // Write the data to the original interop buffer
            InteropLibrary interop = InteropLibrary.getUncached(arrayBuffer);
            try {
                for (int i = 0; i < buffer.position(); i++) {
                    interop.writeBufferByte(arrayBuffer, bufferOffset + destOffset + i, buffer.get(i));
                }
            } catch (InteropException iex) {
                throw Errors.shouldNotReachHere(iex);
            }
        }
        return buffer.position();
    }

    @TruffleBoundary
    private static CoderResult doEncode(TruffleString str, ByteBuffer buffer) throws CharacterCodingException {
        CharsetEncoder encoder = StandardCharsets.UTF_8.newEncoder();
        encoder.onMalformedInput(CodingErrorAction.REPORT);
        encoder.onUnmappableCharacter(CodingErrorAction.REPORT);
        CharBuffer cb = CharBuffer.wrap(Strings.toJavaString(str));
        CoderResult res = encoder.encode(cb, buffer, true);

        if (res.isUnderflow()) {
            encoder.encode(cb, buffer, true);
            res = encoder.flush(buffer);
        }

        assert res.isError() == (res.isMalformed() || res.isUnmappable());
        if (res.isError()) {
            res.throwException();
        }

        return res;
    }

    @TruffleBoundary
    private static byte[] getBytes(TruffleString str) {
        return Strings.toJavaString(str).getBytes(StandardCharsets.UTF_8);
    }

}
