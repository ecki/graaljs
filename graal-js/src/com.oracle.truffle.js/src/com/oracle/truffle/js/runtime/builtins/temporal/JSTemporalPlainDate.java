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
import static com.oracle.truffle.js.runtime.util.TemporalConstants.CONSTRAIN;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.DAY;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.DAYS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.DAYS_IN_MONTH;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.DAYS_IN_WEEK;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.DAYS_IN_YEAR;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.DAY_OF_WEEK;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.DAY_OF_YEAR;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.HOURS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.IN_LEAP_YEAR;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MINUTES;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MONTH;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MONTHS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MONTHS_IN_YEAR;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MONTH_CODE;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.SECONDS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.WEEKS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.WEEK_OF_YEAR;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.YEAR;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.YEARS;

import java.util.Set;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDateFunctionBuiltins;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDatePrototypeBuiltins;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.cast.JSToBooleanNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.JSConstructor;
import com.oracle.truffle.js.runtime.builtins.JSConstructorFactory;
import com.oracle.truffle.js.runtime.builtins.JSDate;
import com.oracle.truffle.js.runtime.builtins.JSNonProxy;
import com.oracle.truffle.js.runtime.builtins.JSObjectFactory;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.builtins.PrototypeSupplier;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.TemporalErrors;
import com.oracle.truffle.js.runtime.util.TemporalUtil;

public final class JSTemporalPlainDate extends JSNonProxy implements JSConstructorFactory.Default.WithFunctionsAndSpecies,
                PrototypeSupplier {

    public static final JSTemporalPlainDate INSTANCE = new JSTemporalPlainDate();

    public static final String CLASS_NAME = "TemporalPlainDate";
    public static final String PROTOTYPE_NAME = "TemporalPlainDate.prototype";

    private JSTemporalPlainDate() {
    }

    @Override
    public String getClassName(DynamicObject object) {
        return "Temporal.PlainDate";
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

        JSObjectUtil.putBuiltinAccessorProperty(prototype, CALENDAR, realm.lookupAccessor(TemporalPlainDatePrototypeBuiltins.BUILTINS, CALENDAR));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, YEAR, realm.lookupAccessor(TemporalPlainDatePrototypeBuiltins.BUILTINS, YEAR));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, MONTH, realm.lookupAccessor(TemporalPlainDatePrototypeBuiltins.BUILTINS, MONTH));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, MONTH_CODE, realm.lookupAccessor(TemporalPlainDatePrototypeBuiltins.BUILTINS, MONTH_CODE));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, DAY, realm.lookupAccessor(TemporalPlainDatePrototypeBuiltins.BUILTINS, DAY));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, DAY_OF_WEEK, realm.lookupAccessor(TemporalPlainDatePrototypeBuiltins.BUILTINS, DAY_OF_WEEK));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, DAY_OF_YEAR, realm.lookupAccessor(TemporalPlainDatePrototypeBuiltins.BUILTINS, DAY_OF_YEAR));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, WEEK_OF_YEAR, realm.lookupAccessor(TemporalPlainDatePrototypeBuiltins.BUILTINS, WEEK_OF_YEAR));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, DAYS_IN_WEEK, realm.lookupAccessor(TemporalPlainDatePrototypeBuiltins.BUILTINS, DAYS_IN_WEEK));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, DAYS_IN_MONTH, realm.lookupAccessor(TemporalPlainDatePrototypeBuiltins.BUILTINS, DAYS_IN_MONTH));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, DAYS_IN_YEAR, realm.lookupAccessor(TemporalPlainDatePrototypeBuiltins.BUILTINS, DAYS_IN_YEAR));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, MONTHS_IN_YEAR, realm.lookupAccessor(TemporalPlainDatePrototypeBuiltins.BUILTINS, MONTHS_IN_YEAR));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, IN_LEAP_YEAR, realm.lookupAccessor(TemporalPlainDatePrototypeBuiltins.BUILTINS, IN_LEAP_YEAR));

        JSObjectUtil.putFunctionsFromContainer(realm, prototype, TemporalPlainDatePrototypeBuiltins.BUILTINS);
        JSObjectUtil.putToStringTag(prototype, "Temporal.PlainDate");
        return prototype;
    }

    @Override
    public Shape makeInitialShape(JSContext context, DynamicObject prototype) {
        Shape initialShape = JSObjectUtil.getProtoChildShape(prototype, JSTemporalPlainDate.INSTANCE, context);
        return initialShape;
    }

    @Override
    public DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
        return realm.getTemporalPlainDatePrototype();
    }

    public static JSConstructor createConstructor(JSRealm realm) {
        return INSTANCE.createConstructorAndPrototype(realm, TemporalPlainDateFunctionBuiltins.BUILTINS);
    }

    public static boolean isJSTemporalPlainDate(Object obj) {
        return obj instanceof JSTemporalPlainDateObject;
    }

    public static DynamicObject createTemporalDate(JSContext context, long y, long m, long d, DynamicObject calendar) {
        rejectDate(y, m, d);
        if (!dateTimeWithinLimits(y, m, d, 12, 0, 0, 0, 0, 0)) {
            throw TemporalErrors.createRangeErrorDateOutsideRange();
        }
        JSRealm realm = context.getRealm();
        JSObjectFactory factory = context.getTemporalPlainDateFactory();
        DynamicObject object = factory.initProto(new JSTemporalPlainDateObject(factory.getShape(realm),
                        (int) y, (int) m, (int) d, calendar), realm);
        return context.trackAllocation(object);
    }

    private static void rejectDate(long year, long month, long day) {
        if (!validateDate(year, month, day)) {
            throw TemporalErrors.createRangeErrorDateTimeOutsideRange();
        }
    }

    private static boolean validateDate(long year, long month, long day) {
        if (month < 1 || month > 12) {
            return false;
        }
        long daysInMonth = daysInMonth(year, month);
        if (day < 1 || day > daysInMonth) {
            return false;
        }
        return true;
    }

    private static int daysInMonth(long year, long month) {
        assert month >= 1;
        assert month <= 12;
        if (month == 1 || month == 3 || month == 5 || month == 7 || month == 8 || month == 10 || month == 12) {
            return 31;
        }
        if (month == 4 || month == 6 || month == 9 || month == 11) {
            return 30;
        }
        if (isLeapYear(year)) {
            return 29;
        }
        return 28;
    }

    private static boolean isLeapYear(long year) {
        if (year % 4 != 0) {
            return false;
        }
        if (year % 400 == 0) {
            return true;
        }
        if (year % 100 == 0) {
            return false;
        }
        return true;
    }

    private static boolean dateTimeWithinLimits(long year, long month, long day, long hour, long minute, long second,
                    long millisecond, long microsecond, long nanosecond) {
        long ns = getEpochFromParts(year, month, day, hour, minute, second, millisecond, microsecond, nanosecond);
        if ((ns / 100_000_000_000_000L) <= -864_000L - 864L) {
            return false;
        } else if ((ns / 100_000_000_000_000L) >= 864_000L + 864L) {
            return false;
        }
        return true;
    }

    private static long getEpochFromParts(long year, long month, long day, long hour, long minute, long second,
                    long millisecond, long microsecond, long nanosecond) {
        assert month >= 1 && month <= 12;
        assert day >= 1 && day <= daysInMonth(year, month);
        assert TemporalUtil.validateTime(hour, minute, second, millisecond, microsecond, nanosecond);
        double date = JSDate.makeDay(year, month, day);
        double time = JSDate.makeTime(hour, minute, second, millisecond);
        double ms = JSDate.makeDate(date, time);
        assert isFinite(ms);
        return (long) ((ms * 1_000_000) + (microsecond * 1_000) + nanosecond);
    }

    private static boolean isFinite(double d) {
        return !(Double.isNaN(d) || Double.isInfinite(d));
    }

    // 3.5.4
    public static DynamicObject toTemporalDate(Object item, Object optionsParam,
                    JSContext ctx, IsObjectNode isObject, JSToBooleanNode toBoolean, JSToStringNode toString) {
        DynamicObject options = optionsParam != Undefined.instance ? (DynamicObject) optionsParam : JSOrdinary.createWithNullPrototype(ctx);
        if (isObject.executeBoolean(item)) {
            DynamicObject itemObj = (DynamicObject) item;
            if (isJSTemporalPlainDate(item)) {
                return itemObj;
            }
            DynamicObject calendar = TemporalUtil.getOptionalTemporalCalendar(itemObj, ctx);
            Set<String> fieldNames = TemporalUtil.calendarFields(calendar, new String[]{"day", "month", "monthCode", "year"}, ctx);
            DynamicObject fields = TemporalUtil.prepareTemporalFields(itemObj, fieldNames, TemporalUtil.toSet(), ctx);
            return TemporalUtil.dateFromFields(calendar, fields, options);
        }
        TemporalUtil.toTemporalOverflow(options, toBoolean, toString);
        JSTemporalDateTimeRecord result = TemporalUtil.parseTemporalDateString(toString.executeString(item), ctx);
        assert TemporalUtil.validateISODate(result.getYear(), result.getMonth(), result.getDay());
        DynamicObject calendar = TemporalUtil.getOptionalTemporalCalendar(result.getCalendar(), ctx);
        return createTemporalDate(ctx, result.getYear(), result.getMonth(), result.getDay(), calendar);
    }

    // 3.5.5
    public static JSTemporalDurationRecord differenceISODate(long y1, long m1, long d1, long y2, long m2, long d2, String largestUnitParam) {
        String largestUnit = largestUnitParam;
        assert largestUnit.equals(YEARS) || largestUnit.equals(MONTHS) ||
                        largestUnit.equals(WEEKS) || largestUnit.equals(DAYS) ||
                        largestUnit.equals(HOURS) || largestUnit.equals(MINUTES) ||
                        largestUnit.equals(SECONDS);
        if (!largestUnit.equals(YEARS) && !largestUnit.equals(MONTHS) && !largestUnit.equals(WEEKS)) {
            largestUnit = DAYS;
        }
        if (largestUnit.equals(YEARS) || largestUnit.equals(MONTHS)) {
            long sign = TemporalUtil.compareISODate(y1, m1, d1, y2, m2, d2);
            if (sign == 0) {
                return toRecordWeeksPlural(0, 0, 0, 0);
            }
            JSTemporalDateTimeRecord start = toRecord(y1, m1, d1);
            JSTemporalDateTimeRecord end = toRecord(y2, m2, d2);
            long years = end.getYear() - start.getYear();
            JSTemporalDateTimeRecord mid = TemporalUtil.addISODate(y1, m1, d1, years, 0, 0, 0, CONSTRAIN);
            long midSign = TemporalUtil.compareISODate(mid.getYear(), mid.getMonth(), mid.getDay(), y2, m2, d2);
            if (midSign == 0) {
                if (largestUnit.equals(YEARS)) {
                    return toRecordWeeksPlural(years, 0, 0, 0);
                } else {
                    return toRecordWeeksPlural(0, years * 12, 0, 0); // sic!
                }
            }
            long months = end.getMonth() - start.getMonth();
            if (midSign != sign) {
                years = years - sign;
                months = months + (sign * 12);
            }
            mid = TemporalUtil.addISODate(y1, m1, d1, years, months, 0, 0, CONSTRAIN);
            midSign = TemporalUtil.compareISODate(mid.getYear(), mid.getMonth(), mid.getDay(), y2, m2, d2);
            if (midSign == 0) {
                if (largestUnit.equals(YEARS)) {
                    return toRecordPlural(years, months, 0);
                } else {
                    return toRecordWeeksPlural(0, months + (years * 12), 0, 0); // sic!
                }
            }
            if (midSign != sign) {
                months = months - sign;
                if (months == -sign) {
                    years = years - sign;
                    months = 11 * sign;
                }
                mid = TemporalUtil.addISODate(y1, m1, d1, years, months, 0, 0, CONSTRAIN);
                midSign = TemporalUtil.compareISODate(mid.getYear(), mid.getMonth(), mid.getDay(), y2, m2, d2);
            }
            long days = 0;
            if (mid.getMonth() == end.getMonth() && mid.getYear() == end.getYear()) {
                days = end.getDay() - mid.getDay();
            } else if (sign < 0) {
                days = -mid.getDay() -
                                (JSTemporalCalendar.isoDaysInMonth(
                                                end.getYear(), end.getMonth()) - end.getDay());
            } else {
                days = end.getDay() +
                                (JSTemporalCalendar.isoDaysInMonth(
                                                mid.getYear(), mid.getMonth()) - mid.getDay());
            }
            if (largestUnit.equals(MONTHS)) {
                months = months + (years * 12);
                years = 0;
            }
            return toRecordWeeksPlural(years, months, 0, days);
        }
        if (largestUnit.equals(DAYS) || largestUnit.equals(WEEKS)) {
            JSTemporalDateTimeRecord smaller;
            JSTemporalDateTimeRecord greater;
            long sign;
            if (TemporalUtil.compareISODate(y1, m1, d1, y2, m2, d2) < 0) {
                smaller = toRecord(y1, m1, d1);
                greater = toRecord(y2, m2, d2);
                sign = 1;
            } else {
                smaller = toRecord(y2, m2, d2);
                greater = toRecord(y1, m1, d1);
                sign = -1;
            }
            long years = greater.getYear() - smaller.getYear();
            long days = JSTemporalCalendar.toISODayOfYear(
                            greater.getYear(), greater.getMonth(), greater.getDay()) -
                            JSTemporalCalendar.toISODayOfYear(
                                            smaller.getYear(), smaller.getMonth(), smaller.getDay());
            assert years >= 0;
            while (years > 0) {
                days = days + JSTemporalCalendar.isoDaysInYear(smaller.getYear() + years - 1);
                years = years - 1;
            }
            long weeks = 0;
            if (largestUnit.equals(WEEKS)) {
                weeks = Math.floorDiv(days, 7);
                days = days % 7;
            }
            return toRecordWeeksPlural(0, 0, weeks * sign, days * sign);
        }
        CompilerDirectives.transferToInterpreter();
        throw Errors.shouldNotReachHere();
    }

    private static JSTemporalDurationRecord toRecordPlural(long year, long month, long day) {
        return JSTemporalDurationRecord.create(year, month, day, 0, 0, 0, 0, 0, 0);
    }

    private static JSTemporalDurationRecord toRecordWeeksPlural(long year, long month, long weeks, long day) {
        return JSTemporalDurationRecord.createWeeks(year, month, weeks, day, 0, 0, 0, 0, 0, 0);
    }

    public static JSTemporalDateTimeRecord toRecord(long year, long month, long day) {
        return JSTemporalDateTimeRecord.create(year, month, day, 0, 0, 0, 0, 0, 0);
    }

    @TruffleBoundary
    public static String temporalDateToString(TemporalDate date, String showCalendar) {
        String yearString = TemporalUtil.padISOYear(date.getISOYear());
        String monthString = String.format("%1$2d", date.getISOMonth()).replace(" ", "0");
        String dayString = String.format("%1$2d", date.getISODay()).replace(" ", "0");

        String calendarID = JSRuntime.toString(date.getCalendar());
        Object calendar = TemporalUtil.formatCalendarAnnotation(calendarID, showCalendar);

        return String.format("%s-%s-%s%s", yearString, monthString, dayString, calendar);
    }

}
