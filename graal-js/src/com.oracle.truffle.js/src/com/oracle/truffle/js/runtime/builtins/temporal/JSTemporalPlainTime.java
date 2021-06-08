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
package com.oracle.truffle.js.runtime.builtins.temporal;

import static com.oracle.truffle.js.runtime.util.TemporalConstants.CALENDAR;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.DAY;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.HOUR;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MICROSECOND;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MILLISECOND;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MINUTE;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.NANOSECOND;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.SECOND;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainTimeFunctionBuiltins;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainTimePrototypeBuiltins;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.cast.JSToIntegerAsLongNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.JSConstructor;
import com.oracle.truffle.js.runtime.builtins.JSConstructorFactory;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSNonProxy;
import com.oracle.truffle.js.runtime.builtins.JSObjectFactory;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.builtins.PrototypeSupplier;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.TemporalConstants;
import com.oracle.truffle.js.runtime.util.TemporalErrors;
import com.oracle.truffle.js.runtime.util.TemporalUtil;

public final class JSTemporalPlainTime extends JSNonProxy implements JSConstructorFactory.Default.WithFunctionsAndSpecies,
                PrototypeSupplier {

    public static final JSTemporalPlainTime INSTANCE = new JSTemporalPlainTime();

    public static final String CLASS_NAME = "TemporalPlainTime";
    public static final String PROTOTYPE_NAME = "TemporalPlainTime.prototype";

    private JSTemporalPlainTime() {
    }

    public static DynamicObject create(JSContext context, long hours, long minutes, long seconds, long milliseconds,
                    long microseconds, long nanoseconds) {
        if (!TemporalUtil.validateTime(hours, minutes, seconds, milliseconds, microseconds, nanoseconds)) {
            throw TemporalErrors.createRangeErrorTimeOutsideRange();
        }
        DynamicObject calendar = TemporalUtil.getISO8601Calendar(context);
        JSRealm realm = context.getRealm();
        JSObjectFactory factory = context.getTemporalPlainTimeFactory();
        DynamicObject obj = factory.initProto(new JSTemporalPlainTimeObject(factory.getShape(realm),
                        hours, minutes, seconds, milliseconds, microseconds, nanoseconds, calendar), realm);
        return context.trackAllocation(obj);
    }

    @Override
    public String getClassName(DynamicObject object) {
        return "Temporal.PlainTime";
    }

    @Override
    public String getClassName() {
        return CLASS_NAME;
    }

    @Override
    public DynamicObject createPrototype(JSRealm realm, DynamicObject constructor) {
        JSContext ctx = realm.getContext();
        DynamicObject prototype = JSObjectUtil.createOrdinaryPrototypeObject(realm);
        JSObjectUtil.putConstructorProperty(ctx, prototype, constructor);
        JSObjectUtil.putFunctionsFromContainer(realm, prototype, TemporalPlainTimePrototypeBuiltins.BUILTINS);

        JSObjectUtil.putBuiltinAccessorProperty(prototype, CALENDAR, realm.lookupAccessor(TemporalPlainTimePrototypeBuiltins.BUILTINS, CALENDAR));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, HOUR, realm.lookupAccessor(TemporalPlainTimePrototypeBuiltins.BUILTINS, HOUR));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, MINUTE, realm.lookupAccessor(TemporalPlainTimePrototypeBuiltins.BUILTINS, MINUTE));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, SECOND, realm.lookupAccessor(TemporalPlainTimePrototypeBuiltins.BUILTINS, SECOND));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, MILLISECOND, realm.lookupAccessor(TemporalPlainTimePrototypeBuiltins.BUILTINS, MILLISECOND));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, MICROSECOND, realm.lookupAccessor(TemporalPlainTimePrototypeBuiltins.BUILTINS, MICROSECOND));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, NANOSECOND, realm.lookupAccessor(TemporalPlainTimePrototypeBuiltins.BUILTINS, NANOSECOND));

        JSObjectUtil.putToStringTag(prototype, "Temporal.PlainTime");

        return prototype;
    }

    @Override
    public Shape makeInitialShape(JSContext context, DynamicObject prototype) {
        Shape initialShape = JSObjectUtil.getProtoChildShape(prototype, JSTemporalPlainTime.INSTANCE, context);
        return initialShape;
    }

    @Override
    public DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
        return realm.getTemporalPlainTimePrototype();
    }

    @Override
    public void fillConstructor(JSRealm realm, DynamicObject constructor) {
        WithFunctionsAndSpecies.super.fillConstructor(realm, constructor);
    }

    public static JSConstructor createConstructor(JSRealm realm) {
        return INSTANCE.createConstructorAndPrototype(realm, TemporalPlainTimeFunctionBuiltins.BUILTINS);
    }

    public static boolean isJSTemporalPlainTime(Object obj) {
        return obj instanceof JSTemporalPlainTimeObject;
    }

    // region Abstract methods

    // 4.5.1
    public static JSTemporalDurationRecord differenceTime(long h1, long min1, long s1, long ms1, long mus1, long ns1,
                    long h2, long min2, long s2, long ms2, long mus2, long ns2) {
        long hours = h2 - h1;
        long minutes = min2 - min1;
        long seconds = s2 - s1;
        long milliseconds = ms2 - ms1;
        long microseconds = mus2 - mus1;
        long nanoseconds = ns2 - ns1;
        long sign = JSTemporalDuration.durationSign(0, 0, 0, 0, hours, minutes, seconds, milliseconds, microseconds, nanoseconds);
        JSTemporalDurationRecord bt = balanceTime(hours, minutes, seconds, milliseconds, microseconds, nanoseconds);

        return JSTemporalDurationRecord.create(0, 0, bt.getDays() * sign, bt.getHours() * sign, bt.getMinutes() * sign, bt.getSeconds() * sign,
                        bt.getMilliseconds() * sign, bt.getMicroseconds() * sign, bt.getNanoseconds() * sign);
    }

    // 4.5.2
    public static Object toTemporalTime(Object item, String overflowParam, JSContext ctx, IsObjectNode isObject, JSToStringNode toString) {
        String overflow = overflowParam == null ? TemporalConstants.CONSTRAIN : overflowParam;
        assert overflow.equals(TemporalConstants.CONSTRAIN) || overflow.equals(TemporalConstants.REJECT);
        JSTemporalDurationRecord result2 = null;
        if (isObject.executeBoolean(item)) {
            if (isJSTemporalPlainTime(item)) {
                return item;
            }
            // TODO 3.b If item has an [[InitializedTemporalZonedDateTime]] internal slot, then
            if (JSTemporalPlainDateTime.isJSTemporalPlainDateTime(item)) {
                TemporalDateTime dt = (TemporalDateTime) item;
                return TemporalUtil.createTemporalTime(ctx, dt.getHours(), dt.getMinutes(), dt.getSeconds(), dt.getMilliseconds(), dt.getMicroseconds(), dt.getNanoseconds());
            }
            DynamicObject calendar = TemporalUtil.getOptionalTemporalCalendar(item, ctx);
            if (!JSRuntime.toString(calendar).equals(TemporalConstants.ISO8601)) {
                throw TemporalErrors.createTypeErrorTemporalISO8601Expected();
            }
            JSTemporalDateTimeRecord result = TemporalUtil.toTemporalTimeRecord((DynamicObject) item);
            result2 = TemporalUtil.regulateTime(
                            result.getHour(), result.getMinute(), result.getSecond(), result.getMillisecond(),
                            result.getMicrosecond(), result.getNanosecond(),
                            overflow);
        } else {
            String string = toString.executeString(item);
            JSTemporalDateTimeRecord result = TemporalUtil.parseTemporalTimeString(string, ctx);
            if (!TemporalUtil.validateTime(
                            result.getHour(), result.getMinute(), result.getSecond(), result.getMillisecond(),
                            result.getMicrosecond(), result.getNanosecond())) {
                throw TemporalErrors.createRangeErrorTimeOutsideRange();
            }
            if (result.hasCalendar() && !JSRuntime.toString(result.getCalendar()).equals(TemporalConstants.ISO8601)) {
                throw TemporalErrors.createTypeErrorTemporalISO8601Expected();
            }
            result2 = JSTemporalDurationRecord.create(result);
        }
        return create(ctx, result2.getHours(), result2.getMinutes(), result2.getSeconds(), result2.getMilliseconds(),
                        result2.getMicroseconds(), result2.getNanoseconds());
    }

    // 4.5.3
    public static DynamicObject toPartialTime(DynamicObject temporalTimeLike, IsObjectNode isObject, JSToIntegerAsLongNode toInt, JSContext ctx) {
        if (!isObject.executeBoolean(temporalTimeLike)) {
            throw TemporalErrors.createTypeErrorTemporalTimeExpected();
        }
        DynamicObject result = JSOrdinary.create(ctx);
        boolean any = false;
        for (String property : TemporalUtil.TIME_LIKE_PROPERTIES) {
            Object value = JSObject.get(temporalTimeLike, property);
            if (value != Undefined.instance) {
                any = true;
                value = toInt.executeLong(value);
                JSObjectUtil.putDataProperty(ctx, result, property, value);
            }
        }
        if (!any) {
            throw TemporalErrors.createTypeErrorTemporalTimePropertyExpected();
        }
        return result;
    }

    // 4.5.9
    public static JSTemporalPlainTimeObject createTemporalTimeFromInstance(long hour, long minute, long second,
                    long millisecond, long microsecond,
                    long nanosecond, JSRealm realm,
                    JSFunctionCallNode callNode) {
        assert TemporalUtil.validateTime(hour, minute, second, millisecond, microsecond, nanosecond);
        DynamicObject constructor = realm.getTemporalPlainTimeConstructor();
        Object[] ctorArgs = new Object[]{hour, minute, second, millisecond, microsecond, nanosecond};
        Object[] args = JSArguments.createInitial(JSFunction.CONSTRUCT, constructor, ctorArgs.length);
        System.arraycopy(ctorArgs, 0, args, JSArguments.RUNTIME_ARGUMENT_COUNT, ctorArgs.length);
        return (JSTemporalPlainTimeObject) callNode.executeCall(args);
    }

    // 4.5.12
    @TruffleBoundary
    public static String temporalTimeToString(long hour, long minute, long second, long millisecond, long microsecond,
                    long nanosecond, Object precision) {
        String hourString = String.format("%1$2d", hour).replace(" ", "0");
        String minuteString = String.format("%1$2d", minute).replace(" ", "0");
        String secondString = TemporalUtil.formatSecondsStringPart(second, millisecond, microsecond, nanosecond, precision);
        return String.format("%s:%s%s", hourString, minuteString, secondString);
    }

    // 4.5.13
    public static int compareTemporalTime(long h1, long min1, long s1, long ms1, long mus1, long ns1,
                    long h2, long min2, long s2, long ms2, long mus2, long ns2) {
        if (h1 > h2) {
            return 1;
        }
        if (h1 < h2) {
            return -1;
        }
        if (min1 > min2) {
            return 1;
        }
        if (min1 < min2) {
            return -1;
        }
        if (s1 > s2) {
            return 1;
        }
        if (s1 < s2) {
            return -1;
        }
        if (ms1 > ms2) {
            return 1;
        }
        if (ms1 < ms2) {
            return -1;
        }
        if (mus1 > mus2) {
            return 1;
        }
        if (mus1 < mus2) {
            return -1;
        }
        if (ns1 > ns2) {
            return 1;
        }
        if (ns1 < ns2) {
            return -1;
        }
        return 0;
    }

    // 4.5.14
    public static JSTemporalDurationRecord addTime(long hour, long minute, long second, long millisecond, long microsecond,
                    long nanosecond, long hours, long minutes, long seconds, long milliseconds,
                    long microseconds, long nanoseconds) {
        return balanceTime(hour + hours, minute + minutes, second + seconds, millisecond + milliseconds,
                        microsecond + microseconds, nanosecond + nanoseconds);
    }

    // 4.5.15
    public static JSTemporalDurationRecord roundTime(long hours, long minutes, long seconds, long milliseconds, long microseconds,
                    long nanoseconds, double increment, String unit, String roundingMode,
                    Long dayLengthNsParam) {
        double fractionalSecond = ((double) nanoseconds / 1_000_000_000) + ((double) microseconds / 1_000_000) +
                        ((double) milliseconds / 1_000) + seconds;
        double quantity;
        if (unit.equals(DAY)) {
            long dayLengthNs = dayLengthNsParam == null ? 86_300_000_000_000L : (long) dayLengthNsParam;
            quantity = ((double) (((((hours * 60 + minutes) * 60 + seconds) * 1000 + milliseconds) * 1000 + microseconds) * 1000 + nanoseconds)) / dayLengthNs;
        } else if (unit.equals(HOUR)) {
            quantity = (fractionalSecond / 60 + minutes) / 60 + hours;
        } else if (unit.equals(MINUTE)) {
            quantity = fractionalSecond / 60 + minutes;
        } else if (unit.equals(SECOND)) {
            quantity = fractionalSecond;
        } else if (unit.equals(MILLISECOND)) {
            quantity = ((double) nanoseconds / 1_000_000) + ((double) microseconds / 1_000) + milliseconds;
        } else if (unit.equals(MICROSECOND)) {
            quantity = ((double) nanoseconds / 1_000) + microseconds;
        } else {
            assert unit.equals(NANOSECOND);
            quantity = nanoseconds;
        }
        long result = (long) TemporalUtil.roundNumberToIncrement(quantity, increment, roundingMode);
        if (unit.equals(DAY)) {
            return JSTemporalDurationRecord.create(0, 0, result, 0, 0, 0, 0, 0, 0);
        }
        if (unit.equals(HOUR)) {
            return balanceTime(result, 0, 0, 0, 0, 0);
        }
        if (unit.equals(MINUTE)) {
            return balanceTime(hours, result, 0, 0, 0, 0);
        }
        if (unit.equals(SECOND)) {
            return balanceTime(hours, minutes, result, 0, 0, 0);
        }
        if (unit.equals(MILLISECOND)) {
            return balanceTime(hours, minutes, seconds, result, 0, 0);
        }
        if (unit.equals(MICROSECOND)) {
            return balanceTime(hours, minutes, seconds, milliseconds, result, 0);
        }
        assert unit.equals(NANOSECOND);
        return balanceTime(hours, minutes, seconds, milliseconds, microseconds, result);
    }
    // endregion

    // 4.5.6
    public static JSTemporalDurationRecord balanceTime(long h, long min, long sec, long mils, long mics, long ns) {
        if (h == Double.POSITIVE_INFINITY || h == Double.NEGATIVE_INFINITY ||
                        min == Double.POSITIVE_INFINITY || min == Double.NEGATIVE_INFINITY ||
                        sec == Double.POSITIVE_INFINITY || sec == Double.NEGATIVE_INFINITY ||
                        mils == Double.POSITIVE_INFINITY || mils == Double.NEGATIVE_INFINITY ||
                        mics == Double.POSITIVE_INFINITY || mics == Double.NEGATIVE_INFINITY ||
                        ns == Double.POSITIVE_INFINITY || ns == Double.NEGATIVE_INFINITY) {
            throw Errors.createRangeError("Time is infinite");
        }
        long microseconds = mics;
        long milliseconds = mils;
        long nanoseconds = ns;
        long seconds = sec;
        long minutes = min;
        long hours = h;
        microseconds = microseconds + (long) Math.floor(nanoseconds / 1000.0);
        nanoseconds = (long) TemporalUtil.nonNegativeModulo(nanoseconds, 1000);
        milliseconds = milliseconds + (long) Math.floor(microseconds / 1000.0);
        microseconds = (long) TemporalUtil.nonNegativeModulo(microseconds, 1000);
        seconds = seconds + (long) Math.floor(milliseconds / 1000.0);
        milliseconds = (long) TemporalUtil.nonNegativeModulo(milliseconds, 1000);
        minutes = minutes + (long) Math.floor(seconds / 60.0);
        seconds = (long) TemporalUtil.nonNegativeModulo(seconds, 60);
        hours = hours + (long) Math.floor(minutes / 60.0);
        minutes = (long) TemporalUtil.nonNegativeModulo(minutes, 60);
        long days = (long) Math.floor(hours / 24.0);
        hours = (long) TemporalUtil.nonNegativeModulo(hours, 24);

        // TODO [[Days]] is plural, rest is singular WTF
        return JSTemporalDurationRecord.create(0, 0, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds);

    }
}
