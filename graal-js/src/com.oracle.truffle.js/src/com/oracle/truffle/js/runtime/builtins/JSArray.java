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
package com.oracle.truffle.js.runtime.builtins;

import static com.oracle.truffle.js.runtime.objects.JSObjectUtil.putConstructorProperty;
import static com.oracle.truffle.js.runtime.objects.JSObjectUtil.putDataProperty;
import static com.oracle.truffle.js.runtime.objects.JSObjectUtil.putFunctionsFromContainer;

import java.util.ArrayList;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.ArrayFunctionBuiltins;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltins;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.array.ArrayAllocationSite;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.array.SparseArray;
import com.oracle.truffle.js.runtime.array.dyn.ConstantByteArray;
import com.oracle.truffle.js.runtime.array.dyn.ConstantDoubleArray;
import com.oracle.truffle.js.runtime.array.dyn.ConstantEmptyPrototypeArray;
import com.oracle.truffle.js.runtime.array.dyn.ConstantIntArray;
import com.oracle.truffle.js.runtime.array.dyn.ConstantObjectArray;
import com.oracle.truffle.js.runtime.array.dyn.HolesObjectArray;
import com.oracle.truffle.js.runtime.array.dyn.LazyArray;
import com.oracle.truffle.js.runtime.array.dyn.LazyRegexResultArray;
import com.oracle.truffle.js.runtime.array.dyn.LazyRegexResultIndicesArray;
import com.oracle.truffle.js.runtime.array.dyn.ZeroBasedDoubleArray;
import com.oracle.truffle.js.runtime.array.dyn.ZeroBasedIntArray;
import com.oracle.truffle.js.runtime.array.dyn.ZeroBasedJSObjectArray;
import com.oracle.truffle.js.runtime.array.dyn.ZeroBasedObjectArray;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.JSProperty;
import com.oracle.truffle.js.runtime.objects.JSShape;
import com.oracle.truffle.js.runtime.objects.PropertyProxy;

public final class JSArray extends JSAbstractArray implements JSConstructorFactory.Default.WithFunctionsAndSpecies, PrototypeSupplier {

    public static final TruffleString CLASS_NAME = Strings.constant("Array");
    public static final TruffleString PROTOTYPE_NAME = Strings.constant("Array.prototype");
    public static final TruffleString ITERATOR_CLASS_NAME = Strings.constant("Array Iterator");
    public static final TruffleString ITERATOR_PROTOTYPE_NAME = Strings.constant("Array Iterator.prototype");
    public static final TruffleString ENTRIES = Strings.constant("entries");

    public static final JSArray INSTANCE = new JSArray();

    static final ArrayLengthProxyProperty ARRAY_LENGTH_PROPERTY_PROXY = new ArrayLengthProxyProperty();

    public static final HiddenKey ARRAY_ITERATION_KIND_ID = new HiddenKey("ArrayIterationKind");

    private JSArray() {
    }

    public static DynamicObject createConstant(JSContext context, JSRealm realm, Object[] elements) {
        return create(context, realm, ScriptArray.createConstantArray(elements), elements, elements.length);
    }

    public static DynamicObject createEmpty(JSContext context, JSRealm realm, int length) {
        if (length < 0) {
            throw Errors.createRangeErrorInvalidArrayLength();
        }
        return createEmptyChecked(context, realm, length);
    }

    /**
     * Creates an empty array of a certain size. The size is expected to be within the valid range
     * of JavaScript array sizes.
     */
    private static DynamicObject createEmptyChecked(JSContext context, JSRealm realm, int length) {
        return createConstantEmptyArray(context, realm, length);
    }

    public static DynamicObject createEmpty(JSContext context, JSRealm realm, long length) {
        if (!JSRuntime.isValidArrayLength(length)) {
            throw Errors.createRangeErrorInvalidArrayLength();
        } else if (length > Integer.MAX_VALUE) {
            return createSparseArray(context, realm, length);
        }
        return createEmptyChecked(context, realm, length);
    }

    /**
     * Creates an empty array of a certain size. The size is expected to be within the valid range
     * of JavaScript array sizes.
     */
    public static DynamicObject createEmptyChecked(JSContext context, JSRealm realm, long length) {
        assert 0 <= length && length <= Integer.MAX_VALUE;
        return createConstantEmptyArray(context, realm, (int) length);
    }

    public static DynamicObject createEmptyZeroLength(JSContext context, JSRealm realm) {
        return createConstantEmptyArray(context, realm);
    }

    public static DynamicObject create(JSContext context, JSRealm realm, ScriptArray arrayType, Object array, long length) {
        return create(context, realm, arrayType, array, length, 0);
    }

    public static DynamicObject create(JSContext context, JSRealm realm, ScriptArray arrayType, Object array, long length, int usedLength) {
        return create(context, realm, arrayType, array, length, usedLength, 0, 0);
    }

    public static DynamicObject create(JSContext context, JSRealm realm, ScriptArray arrayType, Object array, long length, int usedLength, int indexOffset, int arrayOffset) {
        return create(context, realm, arrayType, array, length, usedLength, indexOffset, arrayOffset, 0);
    }

    public static DynamicObject create(JSContext context, JSRealm realm, ScriptArray arrayType, Object array, long length, int usedLength, int indexOffset, int arrayOffset, int holeCount) {
        return createImpl(context, realm, arrayType, array, null, length, usedLength, indexOffset, arrayOffset, holeCount);
    }

    public static DynamicObject create(JSContext context, JSRealm realm, ScriptArray arrayType, Object array, ArrayAllocationSite site, long length, int usedLength, int indexOffset, int arrayOffset,
                    int holeCount) {
        return createImpl(context, realm, arrayType, array, site, length, usedLength, indexOffset, arrayOffset, holeCount);
    }

    private static DynamicObject createImpl(JSContext context, JSRealm realm, ScriptArray arrayType, Object array, ArrayAllocationSite site, long length, int usedLength, int indexOffset,
                    int arrayOffset, int holeCount) {
        // (array, arrayType, allocSite, length, usedLength, indexOffset, arrayOffset, holeCount)
        assert JSRuntime.isRepresentableAsUnsignedInt(length);
        JSObjectFactory factory = context.getArrayFactory();
        DynamicObject obj = JSArrayObject.create(factory.getShape(realm), arrayType, array, site, length, usedLength, indexOffset, arrayOffset, holeCount);
        factory.initProto(obj, realm);
        assert isJSArray(obj);
        return context.trackAllocation(obj);
    }

    public static boolean isJSArray(Object obj) {
        return obj instanceof JSArrayObject;
    }

    public static boolean isJSFastArray(Object obj) {
        return isJSArray(obj) && isJSFastArray((JSArrayObject) obj);
    }

    public static boolean isJSFastArray(DynamicObject obj) {
        return isInstance(obj, INSTANCE);
    }

    @Override
    public TruffleString getClassName() {
        return CLASS_NAME;
    }

    @Override
    public TruffleString getClassName(DynamicObject object) {
        return getClassName();
    }

    @Override
    public DynamicObject createPrototype(JSRealm realm, DynamicObject ctor) {
        JSContext ctx = realm.getContext();

        Shape protoShape = JSShape.createPrototypeShape(ctx, INSTANCE, realm.getObjectPrototype());
        DynamicObject arrayPrototype = JSArrayObject.createEmpty(protoShape, ConstantEmptyPrototypeArray.createConstantEmptyPrototypeArray());
        JSObjectUtil.setOrVerifyPrototype(ctx, arrayPrototype, realm.getObjectPrototype());

        putConstructorProperty(ctx, arrayPrototype, ctor);
        putFunctionsFromContainer(realm, arrayPrototype, ArrayPrototypeBuiltins.BUILTINS);
        // sets the length just for the prototype
        // putProxyProperty(arrayPrototype, ARRAY_LENGTH_PROXY_PROPERTY);
        JSObjectUtil.putProxyProperty(arrayPrototype, LENGTH, ARRAY_LENGTH_PROPERTY_PROXY, JSAttributes.notConfigurableNotEnumerableWritable());
        if (ctx.getEcmaScriptVersion() >= 6) {
            // The initial value of the @@iterator property is the same function object as the
            // initial value of the Array.prototype.values property.
            putDataProperty(ctx, arrayPrototype, Symbol.SYMBOL_ITERATOR, JSDynamicObject.getOrNull(arrayPrototype, Strings.VALUES), JSAttributes.getDefaultNotEnumerable());
            putDataProperty(ctx, arrayPrototype, Symbol.SYMBOL_UNSCOPABLES, createUnscopables(ctx, unscopableNameList(ctx)), JSAttributes.configurableNotEnumerableNotWritable());
        }
        return arrayPrototype;
    }

    private static List<TruffleString> unscopableNameList(JSContext context) {
        List<TruffleString> names = new ArrayList<>();
        if (context.getEcmaScriptVersion() >= JSConfig.ECMAScript2022) {
            names.add(Strings.AT);
        }
        names.add(Strings.COPY_WITHIN);
        names.add(JSArray.ENTRIES);
        names.add(Strings.FILL);
        names.add(Strings.FIND);
        names.add(Strings.FIND_INDEX);
        if (context.getEcmaScriptVersion() >= JSConfig.StagingECMAScriptVersion) {
            names.add(Strings.FIND_LAST);
            names.add(Strings.FIND_LAST_INDEX);
        }
        if (context.getEcmaScriptVersion() >= JSConfig.ECMAScript2019) {
            names.add(Strings.FLAT);
            names.add(Strings.FLAT_MAP);
        }
        if (context.getEcmaScriptVersion() >= JSConfig.StagingECMAScriptVersion) {
            names.add(Strings.GROUP_BY);
            names.add(Strings.GROUP_BY_TO_MAP);
        }
        if (context.getEcmaScriptVersion() >= 7) {
            names.add(Strings.INCLUDES);
        }
        names.add(Strings.KEYS);
        names.add(Strings.VALUES);
        assert isSorted(names);
        return names;
    }

    private static boolean isSorted(List<TruffleString> list) {
        for (int i = 0; i < list.size() - 1; i++) {
            if (list.get(i).compareCharsUTF16Uncached(list.get(i + 1)) > 0) {
                return false;
            }
        }
        return true;
    }

    private static DynamicObject createUnscopables(JSContext context, List<TruffleString> unscopableNames) {
        DynamicObject unscopables = JSOrdinary.createWithNullPrototypeInit(context);
        for (Object name : unscopableNames) {
            putDataProperty(context, unscopables, name, true, JSAttributes.getDefault());
        }
        return unscopables;
    }

    @Override
    public Shape makeInitialShape(JSContext context, DynamicObject prototype) {
        Shape initialShape = JSObjectUtil.getProtoChildShape(prototype, INSTANCE, context);
        initialShape = Shape.newBuilder(initialShape).addConstantProperty(LENGTH, ARRAY_LENGTH_PROPERTY_PROXY, JSAttributes.notConfigurableNotEnumerableWritable() | JSProperty.PROXY).build();
        return initialShape;
    }

    @Override
    public List<Object> getOwnPropertyKeys(DynamicObject thisObj, boolean strings, boolean symbols) {
        return ownPropertyKeysFastArray(thisObj, strings, symbols);
    }

    public static JSConstructor createConstructor(JSRealm realm) {
        return INSTANCE.createConstructorAndPrototype(realm, ArrayFunctionBuiltins.BUILTINS);
    }

    public static class ArrayLengthProxyProperty implements PropertyProxy {
        @Override
        public Object get(DynamicObject store) {
            assert isJSArray(store);
            long length = JSArray.INSTANCE.getLength(store);
            return (double) length;
        }

        @Override
        public boolean set(DynamicObject store, Object value) {
            assert isJSArray(store);
            return JSArray.setLength(store, value);
        }
    }

    @TruffleBoundary
    public static boolean setLength(DynamicObject store, Object value) {
        long arrLength = 0;
        if (value instanceof Integer && (int) value >= 0) {
            arrLength = (int) value;
        } else {
            arrLength = toArrayLengthOrRangeError(value);
        }

        assert arrLength >= 0;
        return !JSAbstractArray.arrayGetArrayType(store).isLengthNotWritable() && ((JSAbstractArray) JSObject.getJSClass(store)).setLength(store, arrLength, false);
    }

    public static DynamicObject createConstantEmptyArray(JSContext context, JSRealm realm, int capacity) {
        ScriptArray arrayType = ScriptArray.createConstantEmptyArray();
        return create(context, realm, arrayType, ScriptArray.EMPTY_OBJECT_ARRAY, capacity);
    }

    public static DynamicObject createConstantEmptyArray(JSContext context, JSRealm realm) {
        return createConstantEmptyArray(context, realm, 0);
    }

    public static DynamicObject createConstantEmptyArray(JSContext context, JSRealm realm, ArrayAllocationSite site) {
        return createConstantEmptyArray(context, realm, site, 0);
    }

    public static DynamicObject createConstantEmptyArray(JSContext context, JSRealm realm, ArrayAllocationSite site, int capacity) {
        ScriptArray arrayType = ScriptArray.createConstantEmptyArray();
        return create(context, realm, arrayType, ScriptArray.EMPTY_OBJECT_ARRAY, site, capacity, 0, 0, 0, 0);
    }

    public static DynamicObject createConstantByteArray(JSContext context, JSRealm realm, byte[] byteArray) {
        ScriptArray arrayType = ConstantByteArray.createConstantByteArray();
        return create(context, realm, arrayType, byteArray, byteArray.length);
    }

    public static DynamicObject createConstantIntArray(JSContext context, JSRealm realm, int[] intArray) {
        ScriptArray arrayType = ConstantIntArray.createConstantIntArray();
        return create(context, realm, arrayType, intArray, intArray.length);
    }

    public static DynamicObject createConstantDoubleArray(JSContext context, JSRealm realm, double[] doubleArray) {
        ScriptArray arrayType = ConstantDoubleArray.createConstantDoubleArray();
        return create(context, realm, arrayType, doubleArray, doubleArray.length);
    }

    public static DynamicObject createConstantObjectArray(JSContext context, JSRealm realm, Object[] objectArray) {
        ScriptArray arrayType = ConstantObjectArray.createConstantObjectArray();
        return create(context, realm, arrayType, objectArray, objectArray.length);
    }

    public static DynamicObject createZeroBasedHolesObjectArray(JSContext context, JSRealm realm, Object[] objectArray, int usedLength, int arrayOffset, int holeCount) {
        return create(context, realm, HolesObjectArray.createHolesObjectArray(), objectArray, objectArray.length, usedLength, 0, arrayOffset, holeCount);
    }

    public static DynamicObject createZeroBasedIntArray(JSContext context, JSRealm realm, int[] intArray) {
        return create(context, realm, ZeroBasedIntArray.createZeroBasedIntArray(), intArray, intArray.length, intArray.length, 0, 0);
    }

    public static DynamicObject createZeroBasedDoubleArray(JSContext context, JSRealm realm, double[] doubleArray) {
        return create(context, realm, ZeroBasedDoubleArray.createZeroBasedDoubleArray(), doubleArray, doubleArray.length, doubleArray.length, 0, 0);
    }

    public static DynamicObject createZeroBasedObjectArray(JSContext context, JSRealm realm, Object[] objectArray) {
        return create(context, realm, ZeroBasedObjectArray.createZeroBasedObjectArray(), objectArray, objectArray.length, objectArray.length, 0, 0);
    }

    public static DynamicObject createZeroBasedJSObjectArray(JSContext context, JSRealm realm, DynamicObject[] objectArray) {
        return create(context, realm, ZeroBasedJSObjectArray.createZeroBasedJSObjectArray(), objectArray, objectArray.length, objectArray.length, 0, 0);
    }

    public static DynamicObject createSparseArray(JSContext context, JSRealm realm, long length) {
        return create(context, realm, SparseArray.createSparseArray(), SparseArray.createArrayMap(), length);
    }

    public static DynamicObject createLazyRegexArray(JSContext context, JSRealm realm, int length) {
        assert JSRuntime.isRepresentableAsUnsignedInt(length);
        Object[] array = new Object[length];
        return create(context, realm, LazyRegexResultArray.createLazyRegexResultArray(), array, length);
    }

    public static DynamicObject createLazyRegexArray(JSContext context, JSRealm realm, int length, Object regexResult, TruffleString input, DynamicObject groups, DynamicObject indicesGroups) {
        assert JSRuntime.isRepresentableAsUnsignedInt(length);
        DynamicObject obj = createLazyRegexArray(context, realm, length);
        JSObjectUtil.putHiddenProperty(obj, JSArray.LAZY_REGEX_RESULT_ID, regexResult);
        JSObjectUtil.putHiddenProperty(obj, JSArray.LAZY_REGEX_ORIGINAL_INPUT_ID, input);
        JSObjectUtil.putProxyProperty(obj, JSRegExp.INDEX, JSRegExp.LAZY_INDEX_PROXY, JSAttributes.getDefault());
        JSObjectUtil.putDataProperty(context, obj, JSRegExp.INPUT, input, JSAttributes.getDefault());
        JSObjectUtil.putDataProperty(context, obj, JSRegExp.GROUPS, groups, JSAttributes.getDefault());
        if (context.isOptionRegexpMatchIndices()) {
            DynamicObject indices = createLazyRegexIndicesArray(context, realm, length, regexResult, indicesGroups);
            JSObjectUtil.putDataProperty(context, obj, JSRegExp.INDICES, indices, JSAttributes.getDefault());
        }
        assert isJSArray(obj);
        return obj;
    }

    public static DynamicObject createLazyRegexIndicesArray(JSContext context, JSRealm realm, int length) {
        assert JSRuntime.isRepresentableAsUnsignedInt(length);
        Object[] array = new Object[length];
        return create(context, realm, LazyRegexResultIndicesArray.createLazyRegexResultIndicesArray(), array, length);
    }

    private static DynamicObject createLazyRegexIndicesArray(JSContext context, JSRealm realm, int length, Object regexResult, DynamicObject indicesGroups) {
        assert JSRuntime.isRepresentableAsUnsignedInt(length);
        Object[] array = new Object[length];
        DynamicObject obj = create(context, realm, LazyRegexResultIndicesArray.createLazyRegexResultIndicesArray(), array, length);
        JSObjectUtil.putHiddenProperty(obj, JSArray.LAZY_REGEX_RESULT_ID, regexResult);
        JSObjectUtil.putDataProperty(context, obj, JSRegExp.GROUPS, indicesGroups, JSAttributes.getDefault());
        assert isJSArray(obj);
        return obj;
    }

    public static DynamicObject createLazyArray(JSContext context, JSRealm realm, List<?> list, int size) {
        assert list.size() == size;
        return create(context, realm, LazyArray.createLazyArray(), list, size);
    }

    @Override
    public DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
        return realm.getArrayPrototype();
    }
}
