/*
 * Copyright (c) 2022, 2023, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.nodes.function;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.HiddenKey;

import java.util.ArrayList;
import java.util.List;

public class ClassElementDefinitionRecord {

    public enum Kind {
        Method,
        Field,
        Getter,
        Setter,
        AutoAccessor,
    }

    private static final Object[] EMPTY = new Object[0];

    private final Kind kind;
    private final Object key;
    private final boolean anonymousFunctionDefinition;
    private final boolean isPrivate;

    /** The function for a method definition. */
    private Object value;
    /** The getter function for an accessor definition. */
    private Object getter;
    /** The setter function for an accessor definition. */
    private Object setter;
    /** The decorators applied to the class element, if any. */
    private Object[] decorators;
    private List<Object> appendedInitializers;
    /** The initializers of the field or accessor, if any. */
    private Object[] initializers;

    public static ClassElementDefinitionRecord createField(Object key, Object value, boolean isPrivate, boolean anonymousFunctionDefinition, Object[] decorators) {
        return new ClassElementDefinitionRecord(Kind.Field, key, value, isPrivate, anonymousFunctionDefinition, decorators);
    }

    public static ClassElementDefinitionRecord createPublicMethod(Object key, Object value, boolean anonymousFunctionDefinition, Object[] decorators) {
        return new ClassElementDefinitionRecord(Kind.Method, key, value, false, anonymousFunctionDefinition, decorators);
    }

    public static ClassElementDefinitionRecord createPublicAccessor(Object key, Object getter, Object setter, boolean anonymousFunctionDefinition, Object[] decorators) {
        return new ClassElementDefinitionRecord(Kind.Getter, key, null, getter, setter, false, anonymousFunctionDefinition, decorators);
    }

    public static ClassElementDefinitionRecord createPublicGetter(Object key, Object getter, boolean anonymousFunctionDefinition, Object[] decorators) {
        return new ClassElementDefinitionRecord(Kind.Getter, key, null, getter, null, false, anonymousFunctionDefinition, decorators);
    }

    public static ClassElementDefinitionRecord createPublicSetter(Object key, Object setter, boolean anonymousFunctionDefinition, Object[] decorators) {
        return new ClassElementDefinitionRecord(Kind.Setter, key, null, null, setter, false, anonymousFunctionDefinition, decorators);
    }

    public static ClassElementDefinitionRecord createPrivateMethod(Object key, int frameSlot, int brandSlot, Object value, boolean anonymousFunctionDefinition, Object[] decorators) {
        return new PrivateFrameBasedElementDefinitionRecord(Kind.Method, key, value, null, null, frameSlot, brandSlot, anonymousFunctionDefinition, decorators);
    }

    public static ClassElementDefinitionRecord createPrivateAccessor(Object key, int frameSlot, int brandSlot, Object getter, Object setter, boolean anonymousFunctionDefinition, Object[] decorators) {
        return new PrivateFrameBasedElementDefinitionRecord(Kind.Getter, key, null, getter, setter, frameSlot, brandSlot, anonymousFunctionDefinition, decorators);
    }

    public static ClassElementDefinitionRecord createPrivateGetter(Object key, int frameSlot, int brandSlot, Object getter, boolean anonymousFunctionDefinition, Object[] decorators) {
        return new PrivateFrameBasedElementDefinitionRecord(Kind.Getter, key, null, getter, null, frameSlot, brandSlot, anonymousFunctionDefinition, decorators);
    }

    public static ClassElementDefinitionRecord createPrivateSetter(Object key, int frameSlot, int brandSlot, Object setter, boolean anonymousFunctionDefinition, Object[] decorators) {
        return new PrivateFrameBasedElementDefinitionRecord(Kind.Setter, key, null, null, setter, frameSlot, brandSlot, anonymousFunctionDefinition, decorators);
    }

    public static ClassElementDefinitionRecord createAutoAccessor(Object key, HiddenKey backingStorageKey, Object value, Object getter, Object setter, boolean isPrivate,
                    boolean anonymousFunctionDefinition, Object[] decorators) {
        return new AutoAccessor(key, backingStorageKey, value, getter, setter, isPrivate, anonymousFunctionDefinition, decorators);
    }

    protected ClassElementDefinitionRecord(Kind kind, Object key, Object value, boolean isPrivate, boolean anonymousFunctionDefinition, Object[] decorators) {
        this(kind, key, value, null, null, isPrivate, anonymousFunctionDefinition, decorators);
    }

    protected ClassElementDefinitionRecord(Kind kind, Object key, Object value, Object getter, Object setter, boolean isPrivate, boolean anonymousFunctionDefinition, Object[] decorators) {
        this.kind = kind;
        this.key = key;
        this.value = value;
        this.getter = getter;
        this.setter = setter;
        this.anonymousFunctionDefinition = anonymousFunctionDefinition;
        this.isPrivate = isPrivate;
        this.decorators = decorators;
        assert kind == Kind.AutoAccessor
                        ? (value != null && getter != null && setter != null)
                        : (value != null) != (getter != null || setter != null);
    }

    public final boolean isMethod() {
        return this.kind == Kind.Method;
    }

    public final boolean isGetter() {
        return this.kind == Kind.Getter;
    }

    public final boolean isSetter() {
        return this.kind == Kind.Setter;
    }

    public final boolean isAccessor() {
        return isGetter() || isSetter();
    }

    public final boolean isAutoAccessor() {
        return this.kind == Kind.AutoAccessor;
    }

    public final boolean isField() {
        return this.kind == Kind.Field;
    }

    public final Kind getKind() {
        return kind;
    }

    public final boolean isPrivate() {
        return isPrivate;
    }

    public Object[] getDecorators() {
        return decorators;
    }

    public boolean hasDecorators() {
        return decorators != null;
    }

    public Object getKey() {
        return key;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object newValue) {
        this.value = newValue;
    }

    public Object[] getInitializers() {
        return initializers == null ? EMPTY : initializers;
    }

    @TruffleBoundary
    public void appendInitializer(Object initializer) {
        if (appendedInitializers == null) {
            appendedInitializers = new ArrayList<>();
        }
        appendedInitializers.add(initializer);
    }

    @TruffleBoundary
    public void cleanDecorator() {
        this.decorators = EMPTY;
        this.initializers = appendedInitializers == null ? EMPTY : appendedInitializers.toArray(EMPTY);
    }

    public Object isAnonymousFunction() {
        return anonymousFunctionDefinition;
    }

    public void setGetter(Object newGetter) {
        this.getter = newGetter;
    }

    public void setSetter(Object newSetter) {
        this.setter = newSetter;
    }

    public Object getGetter() {
        return getter;
    }

    public Object getSetter() {
        return setter;
    }

    @Override
    public String toString() {
        return "ClassElementDefinitionRecord [kind=" + kind + ", key=" + key + ", value=" + value + ", getter=" + getter + ", setter=" + setter + "]";
    }

    public static final class PrivateFrameBasedElementDefinitionRecord extends ClassElementDefinitionRecord {

        private final int keySlot;
        private final int brandSlot;

        private PrivateFrameBasedElementDefinitionRecord(Kind kind, Object key, Object value, Object getter, Object setter, int keySlot, int brandSlot, boolean anonymousFunctionDefinition,
                        Object[] decorators) {
            super(kind, key, value, getter, setter, true, anonymousFunctionDefinition, decorators);
            this.keySlot = keySlot;
            this.brandSlot = brandSlot;
        }

        public int getKeySlot() {
            return keySlot;
        }

        public int getBrandSlot() {
            return brandSlot;
        }
    }

    public static class AutoAccessor extends ClassElementDefinitionRecord {

        private final HiddenKey backingStorageKey;

        protected AutoAccessor(Object key, HiddenKey backingStorageKey, Object value, Object getter, Object setter, boolean isPrivate, boolean anonymousFunctionDefinition, Object[] decorators) {
            super(Kind.AutoAccessor, key, value, getter, setter, isPrivate, anonymousFunctionDefinition, decorators);
            this.backingStorageKey = backingStorageKey;
        }

        public HiddenKey getBackingStorageKey() {
            return backingStorageKey;
        }
    }
}
