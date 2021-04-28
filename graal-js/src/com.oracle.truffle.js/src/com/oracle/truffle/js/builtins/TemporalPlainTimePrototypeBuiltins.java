/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.js.runtime.util.TemporalConstants.DAYS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.HOUR;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.HOURS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MICROSECOND;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MICROSECONDS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MILLISECOND;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MILLISECONDS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MINUTE;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MINUTES;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MONTHS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.NANOSECOND;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.NANOSECONDS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.REJECT;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.SECOND;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.SECONDS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.WEEKS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.YEARS;
import static com.oracle.truffle.js.runtime.util.TemporalUtil.getInt;
import static com.oracle.truffle.js.runtime.util.TemporalUtil.getLong;
import static com.oracle.truffle.js.runtime.util.TemporalUtil.getOrDefault;

import java.util.Collections;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.builtins.TemporalPlainTimePrototypeBuiltinsFactory.JSTemporalPlainTimeAddNodeGen;
import com.oracle.truffle.js.builtins.TemporalPlainTimePrototypeBuiltinsFactory.JSTemporalPlainTimeEqualsNodeGen;
import com.oracle.truffle.js.builtins.TemporalPlainTimePrototypeBuiltinsFactory.JSTemporalPlainTimeGetISOFieldsNodeGen;
import com.oracle.truffle.js.builtins.TemporalPlainTimePrototypeBuiltinsFactory.JSTemporalPlainTimeRoundNodeGen;
import com.oracle.truffle.js.builtins.TemporalPlainTimePrototypeBuiltinsFactory.JSTemporalPlainTimeSinceNodeGen;
import com.oracle.truffle.js.builtins.TemporalPlainTimePrototypeBuiltinsFactory.JSTemporalPlainTimeSubtractNodeGen;
import com.oracle.truffle.js.builtins.TemporalPlainTimePrototypeBuiltinsFactory.JSTemporalPlainTimeToLocaleStringNodeGen;
import com.oracle.truffle.js.builtins.TemporalPlainTimePrototypeBuiltinsFactory.JSTemporalPlainTimeToPlainDateTimeNodeGen;
import com.oracle.truffle.js.builtins.TemporalPlainTimePrototypeBuiltinsFactory.JSTemporalPlainTimeToStringNodeGen;
import com.oracle.truffle.js.builtins.TemporalPlainTimePrototypeBuiltinsFactory.JSTemporalPlainTimeToZonedDateTimeNodeGen;
import com.oracle.truffle.js.builtins.TemporalPlainTimePrototypeBuiltinsFactory.JSTemporalPlainTimeUntilNodeGen;
import com.oracle.truffle.js.builtins.TemporalPlainTimePrototypeBuiltinsFactory.JSTemporalPlainTimeValueOfNodeGen;
import com.oracle.truffle.js.builtins.TemporalPlainTimePrototypeBuiltinsFactory.JSTemporalPlainTimeWithNodeGen;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.cast.JSToBooleanNode;
import com.oracle.truffle.js.nodes.cast.JSToIntegerAsLongNode;
import com.oracle.truffle.js.nodes.cast.JSToNumberNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.unary.IsConstructorNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSTemporalDuration;
import com.oracle.truffle.js.runtime.builtins.JSTemporalPlainDate;
import com.oracle.truffle.js.runtime.builtins.JSTemporalPlainDateObject;
import com.oracle.truffle.js.runtime.builtins.JSTemporalPlainTime;
import com.oracle.truffle.js.runtime.builtins.JSTemporalPlainTimeObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateTimePluralRecord;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.TemporalUtil;

public class TemporalPlainTimePrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<TemporalPlainTimePrototypeBuiltins.TemporalPlainTimePrototype> {

    public static final JSBuiltinsContainer BUILTINS = new TemporalPlainTimePrototypeBuiltins();

    protected TemporalPlainTimePrototypeBuiltins() {
        super(JSTemporalPlainTime.PROTOTYPE_NAME, TemporalPlainTimePrototype.class);
    }

    public enum TemporalPlainTimePrototype implements BuiltinEnum<TemporalPlainTimePrototype> {
        add(1),
        subtract(1),
        with(2),
        until(2),
        since(2),
        round(1),
        equals(1),
        toPlainDateTime(1),
        toZonedDateTime(1),
        getISOFields(0),
        toString(1),
        toLocaleString(0),
        toJSON(0),
        valueOf(0);

        private final int length;

        TemporalPlainTimePrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, TemporalPlainTimePrototype builtinEnum) {
        switch (builtinEnum) {
            case add:
                return JSTemporalPlainTimeAddNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case subtract:
                return JSTemporalPlainTimeSubtractNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case with:
                return JSTemporalPlainTimeWithNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case until:
                return JSTemporalPlainTimeUntilNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case since:
                return JSTemporalPlainTimeSinceNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case round:
                return JSTemporalPlainTimeRoundNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case equals:
                return JSTemporalPlainTimeEqualsNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case toPlainDateTime:
                return JSTemporalPlainTimeToPlainDateTimeNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case toZonedDateTime:
                return JSTemporalPlainTimeToZonedDateTimeNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case getISOFields:
                return JSTemporalPlainTimeGetISOFieldsNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case toString:
                return JSTemporalPlainTimeToStringNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case toLocaleString:
            case toJSON:
                return JSTemporalPlainTimeToLocaleStringNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case valueOf:
                return JSTemporalPlainTimeValueOfNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
        }
        return null;
    }

    // 4.3.10
    public abstract static class JSTemporalPlainTimeAdd extends JSBuiltinNode {

        protected JSTemporalPlainTimeAdd(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public DynamicObject add(DynamicObject thisObj, DynamicObject temporalDurationLike,
                        @Cached("create()") IsObjectNode isObject,
                        @Cached("create()") JSToStringNode toString,
                        @Cached("create()") JSToIntegerAsLongNode toInt,
                        @Cached("createNew()") JSFunctionCallNode callNode) {
            JSTemporalPlainTimeObject temporalTime = (JSTemporalPlainTimeObject) thisObj;
            DynamicObject duration = JSTemporalDuration.toLimitedTemporalDuration(temporalDurationLike,
                            Collections.emptySet(), getContext(), isObject, toString, toInt);
            JSTemporalDuration.rejectDurationSign(
                            getLong(duration, YEARS),
                            getLong(duration, MONTHS),
                            getLong(duration, WEEKS),
                            getLong(duration, DAYS),
                            getLong(duration, HOURS),
                            getLong(duration, MINUTES),
                            getLong(duration, SECONDS),
                            getLong(duration, MILLISECONDS),
                            getLong(duration, MICROSECONDS),
                            getLong(duration, NANOSECONDS));
            JSTemporalPlainDateTimePluralRecord result = JSTemporalPlainTime.addTime(
                            temporalTime.getHours(), temporalTime.getMinutes(), temporalTime.getSeconds(),
                            temporalTime.getMilliseconds(), temporalTime.getMicroseconds(), temporalTime.getNanoseconds(),
                            getLong(duration, HOURS),
                            getLong(duration, MINUTES),
                            getLong(duration, SECONDS),
                            getLong(duration, MILLISECONDS),
                            getLong(duration, MICROSECONDS),
                            getLong(duration, NANOSECONDS));
            JSTemporalPlainDateTimePluralRecord result2 = TemporalUtil.regulateTime(
                            result.getHours(), result.getMinutes(), result.getSeconds(), result.getMilliseconds(), result.getMicroseconds(), result.getNanoseconds(),
                            REJECT);
            return JSTemporalPlainTime.createTemporalTimeFromInstance(
                            result2.getHours(), result2.getMinutes(), result2.getSeconds(), result2.getMilliseconds(), result2.getMicroseconds(), result2.getNanoseconds(),
                            getContext().getRealm(), callNode);
        }
    }

    // 4.3.11
    public abstract static class JSTemporalPlainTimeSubtract extends JSBuiltinNode {

        protected JSTemporalPlainTimeSubtract(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public DynamicObject subtract(DynamicObject thisObj, DynamicObject temporalDurationLike,
                        @Cached("create()") IsObjectNode isObject,
                        @Cached("create()") JSToStringNode toString,
                        @Cached("create()") JSToIntegerAsLongNode toInt,
                        @Cached("createNew()") JSFunctionCallNode callNode) {
            JSTemporalPlainTimeObject temporalTime = (JSTemporalPlainTimeObject) thisObj;
            DynamicObject duration = JSTemporalDuration.toLimitedTemporalDuration(temporalDurationLike,
                            Collections.emptySet(), getContext(), isObject, toString, toInt);
            JSTemporalDuration.rejectDurationSign(
                            getLong(duration, YEARS),
                            getLong(duration, MONTHS),
                            getLong(duration, WEEKS),
                            getLong(duration, DAYS),
                            getLong(duration, HOURS),
                            getLong(duration, MINUTES),
                            getLong(duration, SECONDS),
                            getLong(duration, MILLISECONDS),
                            getLong(duration, MICROSECONDS),
                            getLong(duration, NANOSECONDS));
            JSTemporalPlainDateTimePluralRecord result = JSTemporalPlainTime.addTime(
                            temporalTime.getHours(), temporalTime.getMinutes(), temporalTime.getSeconds(),
                            temporalTime.getMilliseconds(), temporalTime.getMicroseconds(), temporalTime.getNanoseconds(),
                            -getLong(duration, HOURS),
                            -getLong(duration, MINUTES),
                            -getLong(duration, SECONDS),
                            -getLong(duration, MILLISECONDS),
                            -getLong(duration, MICROSECONDS),
                            -getLong(duration, NANOSECONDS));
            JSTemporalPlainDateTimePluralRecord result2 = TemporalUtil.regulateTime(
                            result.getHours(), result.getMinutes(), result.getSeconds(), result.getMilliseconds(), result.getMicroseconds(), result.getNanoseconds(),
                            REJECT);
            return JSTemporalPlainTime.createTemporalTimeFromInstance(
                            result2.getHours(), result2.getMinutes(), result2.getSeconds(), result2.getMilliseconds(), result2.getMicroseconds(), result2.getNanoseconds(),
                            getContext().getRealm(), callNode);
        }
    }

    // 4.3.12
    public abstract static class JSTemporalPlainTimeWith extends JSBuiltinNode {

        protected JSTemporalPlainTimeWith(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected JSTemporalPlainTimeObject with(DynamicObject thisObj, DynamicObject temporalTimeLike, DynamicObject options,
                        @Cached("create()") IsObjectNode isObject,
                        @Cached("create()") JSToIntegerAsLongNode toInt,
                        @Cached("create()") JSToBooleanNode toBoolean,
                        @Cached("create()") JSToStringNode toString,
                        @Cached("createNew()") JSFunctionCallNode callNode) {
            JSTemporalPlainTimeObject temporalTime = (JSTemporalPlainTimeObject) thisObj;
            if (!isObject.executeBoolean(temporalTimeLike)) {
                throw Errors.createTypeError("Temporal.Time like object expected.");
            }
            // TODO: Get calendar property.
            // TODO: Check calendar property is not undefined.
            // TODO: Get time zone property.
            // TODO: Check time zone property is not undefined.
            // TODO: Get calendar value.
            DynamicObject partialTime = JSTemporalPlainTime.toPartialTime(temporalTimeLike, isObject, toInt, getContext());
            DynamicObject normalizedOptions = TemporalUtil.getOptionsObject(options, getContext().getRealm(),
                            isObject);
            String overflow = TemporalUtil.toTemporalOverflow(normalizedOptions, isObject, toBoolean, toString);
            long hour, minute, second, millisecond, microsecond, nanosecond;
            Object tempValue = JSObject.get(partialTime, HOUR);
            if (tempValue != Undefined.instance) {
                hour = toInt.executeLong(tempValue);
            } else {
                hour = temporalTime.getHours();
            }
            tempValue = JSObject.get(partialTime, MINUTE);
            if (tempValue != Undefined.instance) {
                minute = toInt.executeLong(tempValue);
            } else {
                minute = temporalTime.getMinutes();
            }
            tempValue = JSObject.get(partialTime, SECOND);
            if (tempValue != Undefined.instance) {
                second = toInt.executeLong(tempValue);
            } else {
                second = temporalTime.getSeconds();
            }
            tempValue = JSObject.get(partialTime, MILLISECOND);
            if (tempValue != Undefined.instance) {
                millisecond = toInt.executeLong(tempValue);
            } else {
                millisecond = temporalTime.getMilliseconds();
            }
            tempValue = JSObject.get(partialTime, MICROSECOND);
            if (tempValue != Undefined.instance) {
                microsecond = toInt.executeLong(tempValue);
            } else {
                microsecond = temporalTime.getMicroseconds();
            }
            tempValue = JSObject.get(partialTime, NANOSECOND);
            if (tempValue != Undefined.instance) {
                nanosecond = toInt.executeLong(tempValue);
            } else {
                nanosecond = temporalTime.getNanoseconds();
            }
            JSTemporalPlainDateTimePluralRecord result = TemporalUtil.regulateTime(hour, minute, second, millisecond, microsecond,
                            nanosecond, overflow);
            return JSTemporalPlainTime.createTemporalTimeFromInstance(
                            result.getHours(), result.getMinutes(), result.getSeconds(), result.getMilliseconds(), result.getMicroseconds(), result.getNanoseconds(),
                            getContext().getRealm(), callNode);
        }
    }

    // 4.3.13
    public abstract static class JSTemporalPlainTimeUntil extends JSBuiltinNode {

        protected JSTemporalPlainTimeUntil(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public DynamicObject until(DynamicObject thisObj, DynamicObject otherObj, DynamicObject optionsParam,
                        @Cached("create()") IsObjectNode isObject,
                        @Cached("create()") JSToStringNode toString,
                        @Cached("create()") JSToBooleanNode toBoolean,
                        @Cached("create()") JSToNumberNode toNumber,
                        @Cached("create()") IsConstructorNode isConstructor,
                        @Cached("createNew()") JSFunctionCallNode callNode) {
            JSTemporalPlainTimeObject temporalTime = (JSTemporalPlainTimeObject) thisObj;
            JSTemporalPlainTimeObject other = (JSTemporalPlainTimeObject) JSTemporalPlainTime.toTemporalTime(otherObj,
                            null, null, getContext(), isObject, toString, isConstructor, callNode);
            DynamicObject options = TemporalUtil.getOptionsObject(optionsParam, getContext().getRealm(), isObject);
            String smallestUnit = TemporalUtil.toSmallestTemporalDurationUnit(options, NANOSECONDS,
                            TemporalUtil.toSet(YEARS, MONTHS, WEEKS, DAYS),
                            isObject, toBoolean, toString);
            String largestUnit = TemporalUtil.toLargestTemporalUnit(options,
                            TemporalUtil.toSet(YEARS, MONTHS, WEEKS, DAYS),
                            HOURS, isObject, toBoolean, toString);
            TemporalUtil.validateTemporalUnitRange(largestUnit, smallestUnit);
            String roundingMode = TemporalUtil.toTemporalRoundingMode(options, "trunc", isObject, toBoolean, toString);
            double maximum = TemporalUtil.maximumTemporalDurationRoundingIncrement(smallestUnit);
            long roundingIncrement = (long) TemporalUtil.toTemporalRoundingIncrement(options, maximum, false, isObject, toNumber);
            JSTemporalPlainDateTimePluralRecord result = JSTemporalPlainTime.differenceTime(
                            temporalTime.getHours(), temporalTime.getMinutes(), temporalTime.getSeconds(), temporalTime.getMilliseconds(), temporalTime.getMicroseconds(),
                            temporalTime.getNanoseconds(),
                            other.getHours(), other.getMinutes(), other.getSeconds(), other.getMilliseconds(), other.getMicroseconds(), other.getNanoseconds());
            JSTemporalPlainDateTimePluralRecord result2 = JSTemporalDuration.roundDuration(
                            0, 0, 0, 0,
                            result.getHours(), result.getMinutes(), result.getSeconds(), result.getMilliseconds(), result.getMicroseconds(), result.getNanoseconds(),
                            roundingIncrement, smallestUnit, roundingMode, null, getContext());
            JSTemporalPlainDateTimePluralRecord result3 = JSTemporalDuration.balanceDuration(
                            0,
                            result2.getHours(), result2.getMinutes(), result2.getSeconds(), result2.getMilliseconds(), result2.getMicroseconds(), result2.getNanoseconds(),
                            largestUnit, null);
            return JSTemporalDuration.createTemporalDuration(
                            0, 0, 0, 0,
                            result3.getHours(), result3.getMinutes(), result3.getSeconds(), result3.getMilliseconds(), result3.getMicroseconds(), result3.getNanoseconds(),
                            getContext());
        }
    }

    // 4.3.14
    public abstract static class JSTemporalPlainTimeSince extends JSBuiltinNode {

        protected JSTemporalPlainTimeSince(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public DynamicObject since(DynamicObject thisObj, DynamicObject otherObj, DynamicObject optionsParam,
                        @Cached("create()") IsObjectNode isObject,
                        @Cached("create()") JSToStringNode toString,
                        @Cached("create()") JSToBooleanNode toBoolean,
                        @Cached("create()") JSToNumberNode toNumber,
                        @Cached("create()") IsConstructorNode isConstructor,
                        @Cached("createNew()") JSFunctionCallNode callNode) {
            JSTemporalPlainTimeObject temporalTime = (JSTemporalPlainTimeObject) thisObj;
            JSTemporalPlainTimeObject other = (JSTemporalPlainTimeObject) JSTemporalPlainTime.toTemporalTime(otherObj,
                            null, null, getContext(), isObject, toString,
                            isConstructor, callNode);
            DynamicObject options = TemporalUtil.getOptionsObject(optionsParam, getContext().getRealm(), isObject);
            String smallestUnit = TemporalUtil.toSmallestTemporalDurationUnit(options, NANOSECONDS,
                            TemporalUtil.toSet(YEARS, MONTHS, WEEKS, DAYS),
                            isObject, toBoolean, toString);
            String largestUnit = TemporalUtil.toLargestTemporalUnit(options,
                            TemporalUtil.toSet(YEARS, MONTHS, WEEKS, DAYS),
                            HOURS, isObject, toBoolean, toString);
            TemporalUtil.validateTemporalUnitRange(largestUnit, smallestUnit);
            String roundingMode = TemporalUtil.toTemporalRoundingMode(options, "trunc", isObject, toBoolean, toString);
            Double max = TemporalUtil.maximumTemporalDurationRoundingIncrement(smallestUnit);
            double maximum = max == null ? Double.POSITIVE_INFINITY : max.doubleValue();
            long roundingIncrement = (long) TemporalUtil.toTemporalRoundingIncrement(options, maximum, false, isObject, toNumber);
            JSTemporalPlainDateTimePluralRecord result = JSTemporalPlainTime.differenceTime(
                            temporalTime.getHours(), temporalTime.getMinutes(), temporalTime.getSeconds(), temporalTime.getMilliseconds(), temporalTime.getMicroseconds(),
                            temporalTime.getNanoseconds(),
                            other.getHours(), other.getMinutes(), other.getSeconds(), other.getMilliseconds(), other.getMicroseconds(), other.getNanoseconds());
            JSTemporalPlainDateTimePluralRecord result2 = JSTemporalDuration.roundDuration(
                            0, 0, 0, 0,
                            -result.getHours(), -result.getMinutes(), -result.getSeconds(), -result.getMilliseconds(), -result.getMicroseconds(), -result.getNanoseconds(),
                            roundingIncrement, smallestUnit, roundingMode, null, getContext());
            JSTemporalPlainDateTimePluralRecord result3 = JSTemporalDuration.balanceDuration(
                            0,
                            -result2.getHours(), -result2.getMinutes(), -result2.getSeconds(), -result2.getMilliseconds(), -result2.getMicroseconds(), -result2.getNanoseconds(),
                            largestUnit, null);
            return JSTemporalDuration.createTemporalDuration(
                            0, 0, 0, 0,
                            result3.getHours(), result3.getMinutes(), result3.getSeconds(), result3.getMilliseconds(), result3.getMicroseconds(), result3.getNanoseconds(),
                            getContext());
        }
    }

    // 4.3.15
    public abstract static class JSTemporalPlainTimeRound extends JSBuiltinNode {

        protected JSTemporalPlainTimeRound(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected JSTemporalPlainTimeObject round(DynamicObject thisObj, DynamicObject options,
                        @Cached("create()") IsObjectNode isObject,
                        @Cached("create()") JSToBooleanNode toBoolean,
                        @Cached("create()") JSToStringNode toString,
                        @Cached("create()") JSToNumberNode toNumber,
                        @Cached("createNew()") JSFunctionCallNode callNode) {
            JSTemporalPlainTimeObject temporalTime = (JSTemporalPlainTimeObject) thisObj;
            if (options == Undefined.instance) {
                throw Errors.createTypeError("Options should not be null.");
            }
            DynamicObject normalizedOptions = TemporalUtil.getOptionsObject(options, getContext().getRealm(), isObject);
            String smallestUnit = TemporalUtil.toSmallestTemporalUnit(normalizedOptions, Collections.singleton("day"),
                            isObject, toBoolean, toString);
            String roundingMode = TemporalUtil.toTemporalRoundingMode(options, "nearest", isObject, toBoolean, toString);
            int maximum;
            if (smallestUnit.equals("hour")) {
                maximum = 24;
            } else if (smallestUnit.equals("minute") || smallestUnit.equals("second")) {
                maximum = 60;
            } else {
                maximum = 1000;
            }
            double roundingIncrement = TemporalUtil.toTemporalRoundingIncrement(normalizedOptions, (double) maximum,
                            false, isObject, toNumber);
            JSTemporalPlainDateTimePluralRecord result = JSTemporalPlainTime.roundTime(temporalTime.getHours(), temporalTime.getMinutes(),
                            temporalTime.getSeconds(), temporalTime.getMilliseconds(), temporalTime.getMicroseconds(),
                            temporalTime.getNanoseconds(), roundingIncrement, smallestUnit, roundingMode, null);
            return JSTemporalPlainTime.createTemporalTimeFromInstance(
                            result.getHours(), result.getMinutes(), result.getSeconds(), result.getMilliseconds(), result.getMicroseconds(), result.getNanoseconds(),
                            getContext().getRealm(), callNode);
        }
    }

    // 4.3.16
    public abstract static class JSTemporalPlainTimeEquals extends JSBuiltinNode {

        protected JSTemporalPlainTimeEquals(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isJSTemporalTime(otherObj)")
        protected static boolean equals(DynamicObject thisObj, DynamicObject otherObj) {
            JSTemporalPlainTimeObject temporalTime = (JSTemporalPlainTimeObject) thisObj;
            JSTemporalPlainTimeObject other = (JSTemporalPlainTimeObject) otherObj;
            if (temporalTime.getHours() != other.getHours()) {
                return false;
            }
            if (temporalTime.getMinutes() != other.getMinutes()) {
                return false;
            }
            if (temporalTime.getSeconds() != other.getSeconds()) {
                return false;
            }
            if (temporalTime.getMilliseconds() != other.getMilliseconds()) {
                return false;
            }
            if (temporalTime.getMicroseconds() != other.getMicroseconds()) {
                return false;
            }
            if (temporalTime.getNanoseconds() != other.getNanoseconds()) {
                return false;
            }
            return true;
        }

        @Specialization(guards = {"!isJSTemporalTime(otherObj) || isJavaPrimitive(otherObj)"})
        protected static boolean otherNotTemporalTime(@SuppressWarnings("unused") DynamicObject thisObj, @SuppressWarnings("unused") DynamicObject otherObj) {
            return false;
        }
    }

    // 4.3.17
    public abstract static class JSTemporalPlainTimeToPlainDateTime extends JSBuiltinNode {

        protected JSTemporalPlainTimeToPlainDateTime(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public DynamicObject toPlainDateTime(DynamicObject temporalTime, DynamicObject temporalDateObj,
                        @Cached("create()") IsObjectNode isObject,
                        @Cached("create()") JSToBooleanNode toBoolean,
                        @Cached("create()") JSToStringNode toString) {

            TemporalUtil.requireInternalSlot(temporalTime, "InitializedTemporalTime");

            DynamicObject temporalDate = JSTemporalPlainDate.toTemporalDate(temporalDateObj, null,
                            getContext(), isObject, toBoolean, toString);

            JSTemporalPlainTimeObject time = (JSTemporalPlainTimeObject) temporalTime;
            JSTemporalPlainDateObject date = (JSTemporalPlainDateObject) temporalDate;

            return TemporalUtil.createTemporalDateTime(date.getYear(), date.getMonth(), date.getDay(), time.getHours(),
                            time.getMinutes(), time.getSeconds(), time.getMilliseconds(), time.getMicroseconds(), time.getNanoseconds(),
                            date.getCalendar());

        }
    }

    // 4.3.18
    public abstract static class JSTemporalPlainTimeToZonedDateTime extends JSBuiltinNode {

        protected JSTemporalPlainTimeToZonedDateTime(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public DynamicObject toZonedDateTime(DynamicObject thisObj, DynamicObject item,
                        @Cached("create()") IsObjectNode isObject,
                        @Cached("create()") JSToBooleanNode toBoolean,
                        @Cached("create()") JSToStringNode toString) {
            JSTemporalPlainTimeObject time = (JSTemporalPlainTimeObject) thisObj;
            DynamicObject temporalDateLike = (DynamicObject) JSObject.get(item, "plainDate");
            if (temporalDateLike == Undefined.instance) {
                throw Errors.createTypeError("Plain date is not present.");
            }
            JSTemporalPlainDateObject date = (JSTemporalPlainDateObject) JSTemporalPlainDate.toTemporalDate(temporalDateLike, null,
                            getContext(), isObject, toBoolean, toString);
            DynamicObject temporalTimeZoneLike = (DynamicObject) JSObject.get(item, "timeZone", null);
            DynamicObject timeZone = TemporalUtil.toTemporalTimeZone(temporalTimeZoneLike);

            DynamicObject temporalDateTime = TemporalUtil.createTemporalDateTime(date.getYear(), date.getMonth(), date.getDay(), time.getHours(),
                            time.getMinutes(), time.getSeconds(), time.getMilliseconds(), time.getMicroseconds(), time.getNanoseconds(),
                            date.getCalendar());
            DynamicObject instant = TemporalUtil.builtinTimeZoneGetInstantFor(timeZone, temporalDateTime, "compatible");
            long ns = getLong(instant, NANOSECOND);
            return TemporalUtil.createTemporalZonedDateTime(ns, timeZone, date.getCalendar());
        }
    }

    // 4.3.19
    public abstract static class JSTemporalPlainTimeGetISOFields extends JSBuiltinNode {

        protected JSTemporalPlainTimeGetISOFields(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected DynamicObject getISOFields(DynamicObject thisObj) {
            JSTemporalPlainTimeObject temporalTime = (JSTemporalPlainTimeObject) thisObj;
            DynamicObject fields = JSObjectUtil.createOrdinaryPrototypeObject(getContext().getRealm());
            // TODO: Add calendar
            JSObjectUtil.putDataProperty(getContext(), fields, "isoHour", temporalTime.getHours());
            JSObjectUtil.putDataProperty(getContext(), fields, "isoMinute", temporalTime.getMinutes());
            JSObjectUtil.putDataProperty(getContext(), fields, "isoSecond", temporalTime.getSeconds());
            JSObjectUtil.putDataProperty(getContext(), fields, "isoMillisecond", temporalTime.getMilliseconds());
            JSObjectUtil.putDataProperty(getContext(), fields, "isoMicrosecond", temporalTime.getMicroseconds());
            JSObjectUtil.putDataProperty(getContext(), fields, "isoNanosecond", temporalTime.getNanoseconds());
            return fields;
        }
    }

    // 4.3.20
    public abstract static class JSTemporalPlainTimeToString extends JSBuiltinNode {

        protected JSTemporalPlainTimeToString(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected String toString(DynamicObject thisObj, DynamicObject optionsParam,
                        @Cached("create()") IsObjectNode isObject,
                        @Cached("create()") JSToStringNode toString,
                        @Cached("create()") JSToNumberNode toNumber,
                        @Cached("create()") JSToBooleanNode toBoolean) {
            JSTemporalPlainTimeObject temporalTime = (JSTemporalPlainTimeObject) thisObj;
            DynamicObject options = TemporalUtil.getOptionsObject(optionsParam, getContext().getRealm(), isObject);
            DynamicObject precision = TemporalUtil.toSecondsStringPrecision(options, isObject, toBoolean, toString, toNumber, getContext());
            String roundingMode = TemporalUtil.toTemporalRoundingMode(options, "trunc", isObject, toBoolean, toString);
            JSTemporalPlainDateTimePluralRecord roundResult = JSTemporalPlainTime.roundTime(
                            temporalTime.getHours(), temporalTime.getMinutes(), temporalTime.getSeconds(),
                            temporalTime.getMilliseconds(), temporalTime.getMicroseconds(), temporalTime.getNanoseconds(),
                            getInt(precision, "increment"),
                            (String) getOrDefault(precision, "unit", ""), roundingMode,
                            null);
            return JSTemporalPlainTime.temporalTimeToString(
                            roundResult.getHours(), roundResult.getMinutes(), roundResult.getSeconds(), roundResult.getMilliseconds(), roundResult.getMicroseconds(), roundResult.getNanoseconds(),
                            JSObject.get(precision, "precision"));
        }
    }

    // 4.3.21
    // 4.3.22
    public abstract static class JSTemporalPlainTimeToLocaleString extends JSBuiltinNode {

        protected JSTemporalPlainTimeToLocaleString(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public String toLocaleString(DynamicObject thisObj) {
            JSTemporalPlainTimeObject temporalTime = (JSTemporalPlainTimeObject) thisObj;
            return JSTemporalPlainTime.temporalTimeToString(
                            temporalTime.getHours(), temporalTime.getMinutes(), temporalTime.getSeconds(),
                            temporalTime.getMilliseconds(), temporalTime.getMicroseconds(), temporalTime.getNanoseconds(),
                            "auto");
        }
    }

    // 4.3.23
    public abstract static class JSTemporalPlainTimeValueOf extends JSBuiltinNode {

        protected JSTemporalPlainTimeValueOf(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object valueOf(@SuppressWarnings("unused") DynamicObject thisObj) {
            throw Errors.createTypeError("Not supported.");
        }
    }
}
