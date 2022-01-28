/*
 * Copyright (c) 2020, 2022, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.truffle.js.test.builtins;

import static com.oracle.truffle.js.lang.JavaScriptLanguage.ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.junit.Assert;
import org.junit.Test;

import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalParserRecord;
import com.oracle.truffle.js.runtime.util.TemporalParser;
import com.oracle.truffle.js.test.JSTest;

public class TemporalBuiltinsTest extends JSTest {

    private static Context getJSContext() {
        return JSTest.newContextBuilder(ID).option("js.temporal", "true").build();
    }

    private static void validatePlainTime(Context ctx, long hour, long minute, long second, long millisecond, long microsecond,
                    long nanosecond) {
        final Value hourValue = ctx.eval(ID, "plainTime.hour");
        final Value minuteValue = ctx.eval(ID, "plainTime.minute");
        final Value secondValue = ctx.eval(ID, "plainTime.second");
        final Value millisecondValue = ctx.eval(ID, "plainTime.millisecond");
        final Value microsecondValue = ctx.eval(ID, "plainTime.microsecond");
        final Value nanosecondValue = ctx.eval(ID, "plainTime.nanosecond");

        assertEquals(hour, hourValue.asLong());
        assertEquals(minute, minuteValue.asLong());
        assertEquals(second, secondValue.asLong());
        assertEquals(millisecond, millisecondValue.asLong());
        assertEquals(microsecond, microsecondValue.asLong());
        assertEquals(nanosecond, nanosecondValue.asLong());
    }

    private static void validatePlainTimeISOFields(Context ctx, long hour, long minute, long second, long millisecond, long microsecond,
                    long nanosecond) {
        final Value hourValue = ctx.eval(ID, "plainTimeIsoFields.isoHour");
        final Value minuteValue = ctx.eval(ID, "plainTimeIsoFields.isoMinute");
        final Value secondValue = ctx.eval(ID, "plainTimeIsoFields.isoSecond");
        final Value millisecondValue = ctx.eval(ID, "plainTimeIsoFields.isoMillisecond");
        final Value microsecondValue = ctx.eval(ID, "plainTimeIsoFields.isoMicrosecond");
        final Value nanosecondValue = ctx.eval(ID, "plainTimeIsoFields.isoNanosecond");

        assertEquals(hour, hourValue.asLong());
        assertEquals(minute, minuteValue.asLong());
        assertEquals(second, secondValue.asLong());
        assertEquals(millisecond, millisecondValue.asLong());
        assertEquals(microsecond, microsecondValue.asLong());
        assertEquals(nanosecond, nanosecondValue.asLong());
    }

    private static void validateDuration(Context ctx, long years, long months, long weeks, long days, long hours, long minutes,
                    long seconds, long milliseconds, long microseconds, long nanoseconds) {
        final Value yearsValue = ctx.eval(ID, "duration.years");
        final Value monthsValue = ctx.eval(ID, "duration.months");
        final Value weeksValue = ctx.eval(ID, "duration.weeks");
        final Value daysValue = ctx.eval(ID, "duration.days");
        final Value hoursValue = ctx.eval(ID, "duration.hours");
        final Value minutesValue = ctx.eval(ID, "duration.minutes");
        final Value secondsValue = ctx.eval(ID, "duration.seconds");
        final Value millisecondsValue = ctx.eval(ID, "duration.milliseconds");
        final Value microsecondsValue = ctx.eval(ID, "duration.microseconds");
        final Value nanosecondsValue = ctx.eval(ID, "duration.nanoseconds");

        assertEquals(years, yearsValue.asLong());
        assertEquals(months, monthsValue.asLong());
        assertEquals(weeks, weeksValue.asLong());
        assertEquals(days, daysValue.asLong());
        assertEquals(hours, hoursValue.asLong());
        assertEquals(minutes, minutesValue.asLong());
        assertEquals(seconds, secondsValue.asLong());
        assertEquals(milliseconds, millisecondsValue.asLong());
        assertEquals(microseconds, microsecondsValue.asLong());
        assertEquals(nanoseconds, nanosecondsValue.asLong());
    }

    private static void validateCalendar(Context ctx) {
        final Value calendarIdValue = ctx.eval(ID, "calendar.id");

        assertEquals("iso8601", calendarIdValue.asString());
    }

    private static void validatePlainDate(Context ctx, long year, long month, long day) {
        Value yearValue = ctx.eval(ID, "plainDate.year");
        Value monthValue = ctx.eval(ID, "plainDate.month");
        Value dayValue = ctx.eval(ID, "plainDate.day");

        assertEquals(year, yearValue.asLong());
        assertEquals(month, monthValue.asLong());
        assertEquals(day, dayValue.asLong());
    }

    private static void validatePlainDateTime(Context ctx, long year, long month, long day, long hour, long minute, long second, long millisecond, long microsecond,
                    long nanosecond) {
        final Value hourValue = ctx.eval(ID, "plainDateTime.hour");
        final Value minuteValue = ctx.eval(ID, "plainDateTime.minute");
        final Value secondValue = ctx.eval(ID, "plainDateTime.second");
        final Value millisecondValue = ctx.eval(ID, "plainDateTime.millisecond");
        final Value microsecondValue = ctx.eval(ID, "plainDateTime.microsecond");
        final Value nanosecondValue = ctx.eval(ID, "plainDateTime.nanosecond");

        Value yearValue = ctx.eval(ID, "plainDateTime.year");
        Value monthValue = ctx.eval(ID, "plainDateTime.month");
        Value dayValue = ctx.eval(ID, "plainDateTime.day");

        assertEquals(year, yearValue.asLong());
        assertEquals(month, monthValue.asLong());
        assertEquals(day, dayValue.asLong());
        assertEquals(hour, hourValue.asLong());
        assertEquals(minute, minuteValue.asLong());
        assertEquals(second, secondValue.asLong());
        assertEquals(millisecond, millisecondValue.asLong());
        assertEquals(microsecond, microsecondValue.asLong());
        assertEquals(nanosecond, nanosecondValue.asLong());
    }

    private static void validatePlainYearMonth(Context ctx, long year, long month, String monthCode, long daysInYear,
                    long daysInMonth, long monthsInYear, boolean inLeapYear) {
        final Value yearValue = ctx.eval(ID, "plainYearMonth.year");
        final Value monthValue = ctx.eval(ID, "plainYearMonth.month");
        final Value monthCodeValue = ctx.eval(ID, "plainYearMonth.monthCode");
        final Value daysInYearValue = ctx.eval(ID, "plainYearMonth.daysInYear");
        final Value daysInMonthValue = ctx.eval(ID, "plainYearMonth.daysInMonth");
        final Value monthsInYearValue = ctx.eval(ID, "plainYearMonth.monthsInYear");
        final Value inLeapYearValue = ctx.eval(ID, "plainYearMonth.inLeapYear");

        assertEquals(year, yearValue.asLong());
        assertEquals(month, monthValue.asLong());
        assertEquals(monthCode, monthCodeValue.asString());
        assertEquals(daysInYear, daysInYearValue.asLong());
        assertEquals(daysInMonth, daysInMonthValue.asLong());
        assertEquals(monthsInYear, monthsInYearValue.asLong());
        assertEquals(inLeapYear, inLeapYearValue.asBoolean());
    }

    private static void validatePlainMonthDay(Context ctx, String monthCode, long day) {
        final Value monthCodeValue = ctx.eval(ID, "plainMonthDay.monthCode");
        final Value dayValue = ctx.eval(ID, "plainMonthDay.day");

        assertEquals(monthCode, monthCodeValue.asString());
        assertEquals(day, dayValue.asLong());
    }

// region PlainTime Tests
    @Test
    public void testPlainTimeCreation() {
        try (Context ctx = getJSContext()) {
            ctx.eval(ID, "let plainTime = new Temporal.PlainTime(12, 45, 35, 520, 450, 860);");
            validatePlainTime(ctx, 12, 45, 35, 520, 450, 860);
        }
    }

    @Test
    public void testPlainTimeFrom() {
        try (Context ctx = getJSContext()) {
            ctx.eval(ID, "let plainTime = Temporal.PlainTime.from({ hour: 12, minute: 45, second: 35, millisecond: 520, microsecond: 450, nanosecond: 860 });");
            validatePlainTime(ctx, 12, 45, 35, 520, 450, 860);

            ctx.eval(ID, "plainTime = Temporal.PlainTime.from('10:23:45')");
            validatePlainTime(ctx, 10, 23, 45, 0, 0, 0);

            ctx.eval(ID, "plainTime = Temporal.PlainTime.from('01:02:03.4')");
            validatePlainTime(ctx, 1, 2, 3, 400, 0, 0);

            ctx.eval(ID, "plainTime = Temporal.PlainTime.from('01:02:03.004')");
            validatePlainTime(ctx, 1, 2, 3, 4, 0, 0);

            ctx.eval(ID, "plainTime = Temporal.PlainTime.from('11:12:13.123456789')");
            validatePlainTime(ctx, 11, 12, 13, 123, 456, 789);

            ctx.eval(ID, " plainTime = Temporal.PlainTime.from('15:23');");
            validatePlainTime(ctx, 15, 23, 0, 0, 0, 0);
        }
    }

    @Test
    public void testPlainTimeCompare() {
        try (Context ctx = getJSContext()) {
            ctx.eval(ID, "let plainTime = new Temporal.PlainTime(12, 45, 35, 520, 450, 860);");
            Value compareResult = ctx.eval(ID, "Temporal.PlainTime.compare(plainTime, plainTime);");
            assertEquals(0, compareResult.asInt());
        }
    }

    @Test
    public void testPlainTimeWith() {
        try (Context ctx = getJSContext()) {
            ctx.eval(ID, "let plainTime = new Temporal.PlainTime(12, 45, 35, 520, 450, 860);");
            ctx.eval(ID, "plainTime = plainTime.with({ minute: 0, second: 0 });");
            validatePlainTime(ctx, 12, 0, 0, 520, 450, 860);
        }
    }

    @Test
    public void testPlainTimeAddDurationLikeObject() {
        try (Context ctx = getJSContext()) {
            ctx.eval(ID, "let plainTime = new Temporal.PlainTime(12, 45, 35);" +
                            "plainTime = plainTime.add({ seconds: 20 });");
            validatePlainTime(ctx, 12, 45, 55, 0, 0, 0);
        }
    }

    @Test
    public void testPlainTimeAddDurationObject() {
        try (Context ctx = getJSContext()) {
            ctx.eval(ID, "let duration = new Temporal.Duration(0, 0, 0, 0, 0, 0, 20);" +
                            "let plainTime = new Temporal.PlainTime(12, 45, 35);" +
                            "plainTime = plainTime.add(duration);");
            validatePlainTime(ctx, 12, 45, 55, 0, 0, 0);
        }
    }

    @Test
    public void testPlainTimeSubtractDurationLikeObject() {
        try (Context ctx = getJSContext()) {
            ctx.eval(ID, "let plainTime = new Temporal.PlainTime(12, 45, 35);" +
                            "plainTime = plainTime.subtract({ seconds: 20 });");
            validatePlainTime(ctx, 12, 45, 15, 0, 0, 0);
        }
    }

    @Test
    public void testPlainTimeUntilPlainTimeLikeObject() {
        try (Context ctx = getJSContext()) {
            ctx.eval(ID, "let plainTime = new Temporal.PlainTime(12, 45, 35);" +
                            "let duration = plainTime.until({ hour: 12, minute: 45, second: 55 });");
            validateDuration(ctx, 0, 0, 0, 0, 0, 0, 20, 0, 0, 0);
        }
    }

    @Test
    public void testPlainTimeSincePlainTimeLikeObject() {
        try (Context ctx = getJSContext()) {
            ctx.eval(ID, "let plainTime = new Temporal.PlainTime(12, 45, 35);" +
                            "let duration = plainTime.until({ hour: 12, minute: 45, second: 55 });");
            validateDuration(ctx, 0, 0, 0, 0, 0, 0, 20, 0, 0, 0);
        }
    }

    @Test
    public void testPlainTimeRound() {
        try (Context ctx = getJSContext()) {
            ctx.eval(ID, "let plainTime = new Temporal.PlainTime(12, 45, 35);" +
                            "plainTime = plainTime.round({ smallestUnit: 'hour' });");
            validatePlainTime(ctx, 13, 0, 0, 0, 0, 0);
        }
    }

    @Test
    public void testPlainTimeEquals() {
        try (Context ctx = getJSContext()) {
            ctx.eval(ID, "let plainTime = new Temporal.PlainTime(12, 45, 35);");
            Value equalsValue = ctx.eval(ID, "plainTime.equals(plainTime)");
            assertTrue(equalsValue.asBoolean());
        }
    }

    @Test
    public void testPlainTimeToString() {
        try (Context ctx = getJSContext()) {
            ctx.eval(ID, "let plainTime = new Temporal.PlainTime(10, 25, 5, 500, 400, 760);");
            Value str = ctx.eval(ID, "plainTime.toString();");
            assertEquals("10:25:05.50040076", str.asString());
        }
    }

    @Test
    public void testPlainTimeToLocaleString() {
        try (Context ctx = getJSContext()) {
            ctx.eval(ID, "let plainTime = new Temporal.PlainTime(10, 25, 5, 500, 400, 760);");
            Value str = ctx.eval(ID, "plainTime.toLocaleString();");
            assertEquals("10:25:05.50040076", str.asString());
        }
    }

    @Test
    public void testPlainTimeToJSON() {
        try (Context ctx = getJSContext()) {
            ctx.eval(ID, "let plainTime = new Temporal.PlainTime(10, 25, 5, 500, 400, 760);");
            Value str = ctx.eval(ID, "plainTime.toJSON();");
            assertEquals("10:25:05.50040076", str.asString());
        }
    }

    @Test
    public void testPlainTimeToValueOf() {
        try (Context ctx = getJSContext()) {
            ctx.eval(ID, "let plainTime = new Temporal.PlainTime()");
            ctx.eval(ID, "plainTime.valueOf();");
        } catch (PolyglotException e) {
            assertEquals(e.getMessage(), "TypeError: Not supported.");
        }
    }

    @Test
    public void testPlainTimeISOFields() {
        try (Context ctx = getJSContext()) {
            ctx.eval(ID, "let plainTime = new Temporal.PlainTime(12, 45, 35, 520, 450, 860);");
            ctx.eval(ID, "let plainTimeIsoFields = plainTime.getISOFields();");
            validatePlainTimeISOFields(ctx, 12, 45, 35, 520, 450, 860);
        }
    }
// endregion

// region Duration Tests
    @Test
    public void testDurationCreation() {
        try (Context ctx = getJSContext()) {
            ctx.eval(ID, "let duration = new Temporal.Duration(0, 0, 0, 0, 12, 45, 35, 520, 450, 860);");
            validateDuration(ctx, 0, 0, 0, 0, 12, 45, 35, 520, 450, 860);
        }
    }

    @Test
    public void testDurationFrom() {
        try (Context ctx = getJSContext()) {
            ctx.eval(ID, "let duration = Temporal.Duration.from({ years: 0, months: 0, weeks: 0, days: 0," +
                            " hours: 12, minutes: 45, seconds: 35, milliseconds: 520, microseconds: 450, nanoseconds: 860 });");
            validateDuration(ctx, 0, 0, 0, 0, 12, 45, 35, 520, 450, 860);
        }
    }

    @Test
    public void testDurationCompare() {
        try (Context ctx = getJSContext()) {
            ctx.eval(ID, "let duration = new Temporal.Duration(0, 0, 0, 0, 12, 45, 35, 520, 450, 860);");
            Value compareValue = ctx.eval(ID, "Temporal.Duration.compare(duration, duration);");
            assertEquals(0, compareValue.asInt());
        }
    }

    @Test
    public void testDurationSign() {
        try (Context ctx = getJSContext()) {
            ctx.eval(ID, "let duration = new Temporal.Duration(0, 0, 0, 0, 12, 45, 35, 520, 450, 860);");
            Value signValue = ctx.eval(ID, "duration.sign");
            assertEquals(1, signValue.asInt());
        }
    }

    @Test
    public void testDurationBlank() {
        try (Context ctx = getJSContext()) {
            ctx.eval(ID, "let duration = new Temporal.Duration(0, 0, 0, 0, 0, 0, 0, 0, 0, 0);");
            Value blankValue = ctx.eval(ID, "duration.blank");
            assertTrue(blankValue.asBoolean());
        }
    }

    @Test
    public void testDurationWith() {
        try (Context ctx = getJSContext()) {
            ctx.eval(ID, "let duration = new Temporal.Duration(0, 0, 0, 0, 12, 45, 35, 520, 450, 860);");
            ctx.eval(ID, "duration = duration.with({ years: 8 })");
            validateDuration(ctx, 8, 0, 0, 0, 12, 45, 35, 520, 450, 860);
        }
    }

    @Test
    public void testDurationAdd() {
        try (Context ctx = getJSContext()) {
            ctx.eval(ID, "let duration = new Temporal.Duration(0, 0, 0, 0, 12, 45, 35, 520, 450, 860);");
            ctx.eval(ID, "duration = duration.add({ seconds: 20 });");
            validateDuration(ctx, 0, 0, 0, 0, 12, 45, 55, 520, 450, 860);
        }
    }

    @Test
    public void testDurationSubtract() {
        try (Context ctx = getJSContext()) {
            ctx.eval(ID, "let duration = new Temporal.Duration(0, 0, 0, 0, 12, 45, 35, 520, 450, 860);");
            ctx.eval(ID, "duration = duration.subtract({ seconds: 20 });");
            validateDuration(ctx, 0, 0, 0, 0, 12, 45, 15, 520, 450, 860);
        }
    }

    @Test
    public void testDurationNegated() {
        try (Context ctx = getJSContext()) {
            ctx.eval(ID, "let duration = new Temporal.Duration(0, 0, 0, 0, 12, 45, 35, 520, 450, 860);");
            Value signValue = ctx.eval(ID, "duration.negated().sign");
            assertEquals(-1, signValue.asInt());
        }
    }

    @Test
    public void testDurationAbs() {
        try (Context ctx = getJSContext()) {
            ctx.eval(ID, "let duration = new Temporal.Duration(0, 0, 0, 0, 12, 45, 35, 520, 450, 860);");
            Value signValue = ctx.eval(ID, "duration.negated().abs().sign");
            assertEquals(1, signValue.asInt());
        }
    }

    @Test
    public void testDurationRound() {
        try (Context ctx = getJSContext()) {
            ctx.eval(ID, "let duration = new Temporal.Duration(0, 0, 0, 0, 12, 45, 35, 520, 450, 860);");
            ctx.eval(ID, "duration = duration.round({ smallestUnit: 'hours' }); print(duration.toString());");
            validateDuration(ctx, 0, 0, 0, 0, 13, 0, 0, 0, 0, 0);
        }
    }

    @Test
    public void testDurationTotal() {
        try (Context ctx = getJSContext()) {
            ctx.eval(ID, "let duration = Temporal.Duration.from({ hours: 130, minutes: 20 });");
            Value totalValue = ctx.eval(ID, "duration.total({ unit: 'seconds' });");
            assertEquals(469200, totalValue.asInt());
        }
    }

    @Test
    public void testDurationToJSON() {
        try (Context ctx = getJSContext()) {
            ctx.eval(ID, "let duration = Temporal.Duration.from({ years: 1, days: 1 });");
            Value jsonValue = ctx.eval(ID, "duration.toJSON();");
            assertEquals("P1Y1D", jsonValue.asString());
        }
    }

    @Test
    public void testDurationToLocaleString() {
        try (Context ctx = getJSContext()) {
            ctx.eval(ID, "let duration = Temporal.Duration.from({ years: 1, days: 1 });");
            Value toString = ctx.eval(ID, "duration.toLocaleString();");
            assertEquals("P1Y1D", toString.asString());
        }
    }

    @Test
    public void testDurationToString() {
        try (Context ctx = getJSContext()) {
            ctx.eval(ID, "let duration = Temporal.Duration.from({ years: 1, days: 1 });");
            Value toString = ctx.eval(ID, "duration.toString();");
            assertEquals("P1Y1D", toString.asString());
        }
    }

    @Test
    public void testDurationToValueOf() {
        try (Context ctx = getJSContext()) {
            ctx.eval(ID, "let duration = new Temporal.Duration()");
            ctx.eval(ID, "duration.valueOf();");
        } catch (PolyglotException e) {
            assertEquals(e.getMessage(), "TypeError: Not supported.");
        }
    }
// endregion

// region Calendar Tests
    @Test
    public void testCalendarCreation() {
        try (Context ctx = getJSContext()) {
            ctx.eval(ID, "let calendar = new Temporal.Calendar('iso8601');");
            validateCalendar(ctx);
        }
    }

    @Test
    public void testCalendarFrom() {
        try (Context ctx = getJSContext()) {
            ctx.eval(ID, "let calendar = Temporal.Calendar.from('iso8601');");
            validateCalendar(ctx);
        }
    }

    @Test
    public void testCalendarYear() {
        try (Context ctx = getJSContext()) {
            ctx.eval(ID, "let calendar = Temporal.Calendar.from('iso8601');");
            Value yearValue = ctx.eval(ID, "calendar.year({ year: 2021, month: 4, day: 22 });");
            assertEquals(2021, yearValue.asInt());
        }
    }

    @Test
    public void testCalendarMonth() {
        try (Context ctx = getJSContext()) {
            ctx.eval(ID, "let calendar = Temporal.Calendar.from('iso8601');");
            Value monthValue = ctx.eval(ID, "calendar.month({ year: 2021, month: 4, day: 22 });");
            assertEquals(4, monthValue.asInt());
        }
    }

    @Test
    public void testCalendarMonthCode() {
        try (Context ctx = getJSContext()) {
            ctx.eval(ID, "let calendar = Temporal.Calendar.from('iso8601');");
            Value monthCodeValue = ctx.eval(ID, "calendar.monthCode({ year: 2021, month: 4, day: 22 });");
            assertEquals("M04", monthCodeValue.asString());
        }
    }

    @Test
    public void testCalendarDay() {
        try (Context ctx = getJSContext()) {
            ctx.eval(ID, "let calendar = Temporal.Calendar.from('iso8601');");
            Value dayValue = ctx.eval(ID, "calendar.day({ year: 2021, month: 4, day: 22 });");
            assertEquals(22, dayValue.asInt());
        }
    }

    @Test
    public void testCalendarDayOfWeek() {
        try (Context ctx = getJSContext()) {
            ctx.eval(ID, "let calendar = Temporal.Calendar.from('iso8601');");
            Value dayOfWeekValue = ctx.eval(ID, "calendar.dayOfWeek({ year: 2021, month: 4, day: 22 });");
            assertEquals(4, dayOfWeekValue.asInt());
        }
    }

    @Test
    public void testCalendarDayOfYear() {
        try (Context ctx = getJSContext()) {
            ctx.eval(ID, "let calendar = Temporal.Calendar.from('iso8601');");
            Value dayOfYearValue = ctx.eval(ID, "calendar.dayOfYear({ year: 2021, month: 4, day: 22 });");
            assertEquals(112, dayOfYearValue.asInt());
        }
    }

    @Test
    public void testCalendarWeekOfYear() {
        try (Context ctx = getJSContext()) {
            ctx.eval(ID, "let calendar = Temporal.Calendar.from('iso8601');");
            Value weekOfYearValue = ctx.eval(ID, "calendar.weekOfYear({ year: 2021, month: 4, day: 22 });");
            assertEquals(16, weekOfYearValue.asInt());
        }
    }

    @Test
    public void testCalendarDaysInWeek() {
        try (Context ctx = getJSContext()) {
            ctx.eval(ID, "let calendar = Temporal.Calendar.from('iso8601');");
            Value daysInWeekValue = ctx.eval(ID, "calendar.daysInWeek({ year: 2021, month: 4, day: 22 });");
            assertEquals(7, daysInWeekValue.asInt());
        }
    }

    @Test
    public void testCalendarDaysInMonth() {
        try (Context ctx = getJSContext()) {
            ctx.eval(ID, "let calendar = Temporal.Calendar.from('iso8601');");
            Value daysInMonthValue = ctx.eval(ID, "calendar.daysInMonth({ year: 2021, month: 4, day: 22 });");
            assertEquals(30, daysInMonthValue.asInt());
        }
    }

    @Test
    public void testCalendarDaysInYear() {
        try (Context ctx = getJSContext()) {
            ctx.eval(ID, "let calendar = Temporal.Calendar.from('iso8601');");
            Value daysInYearValue = ctx.eval(ID, "calendar.daysInYear({ year: 2021, month: 4, day: 22 });");
            assertEquals(365, daysInYearValue.asInt());
        }
    }

    @Test
    public void testCalendarMonthsInYear() {
        try (Context ctx = getJSContext()) {
            ctx.eval(ID, "let calendar = Temporal.Calendar.from('iso8601');");
            Value monthsInYearValue = ctx.eval(ID, "calendar.monthsInYear({ year: 2021, month: 4, day: 22 });");
            assertEquals(12, monthsInYearValue.asInt());
        }
    }

    @Test
    public void testCalendarInLeapYear() {
        try (Context ctx = getJSContext()) {
            ctx.eval(ID, "let calendar = Temporal.Calendar.from('iso8601');");
            Value inLeapYearValue = ctx.eval(ID, "calendar.inLeapYear({ year: 2021, month: 4, day: 22 });");
            assertFalse(inLeapYearValue.asBoolean());
        }
    }

    @Test
    public void testCalendarDateFromFields() {
        try (Context ctx = getJSContext()) {
            ctx.eval(ID, "let calendar = Temporal.Calendar.from('iso8601');");
            ctx.eval(ID, "let plainDate = calendar.dateFromFields({ year: 2021, month: 4, day: 22 });");
            validatePlainDate(ctx, 2021, 4, 22);
        }
    }

    @Test
    public void testCalendarYearMonthFromFields() {
        try (Context ctx = getJSContext()) {
            ctx.eval(ID, "let calendar = Temporal.Calendar.from('iso8601');");
            ctx.eval(ID, "let plainYearMonth = calendar.yearMonthFromFields({ year: 2021, month: 4 });");
            validatePlainYearMonth(ctx, 2021, 4, "M04", 365, 30, 12, false);
        }
    }

    @Test
    public void testCalendarMonthDayFromFields() {
        try (Context ctx = getJSContext()) {
            ctx.eval(ID, "let calendar = Temporal.Calendar.from('iso8601');");
            ctx.eval(ID, "let plainMonthDay = calendar.monthDayFromFields({ month: 4, monthCode: 'M04', day: 22 });");
            validatePlainMonthDay(ctx, "M04", 22);
        }
    }

    @Test
    public void testCalendarDateAdd() {
        try (Context ctx = getJSContext()) {
            ctx.eval(ID, "let calendar = Temporal.Calendar.from('iso8601');");
            ctx.eval(ID, "let plainDate = calendar.dateAdd({ year: 2021, month: 4, day: 22 }, { days: 1 });");
            validatePlainDate(ctx, 2021, 4, 23);
        }
    }

    @Test
    public void testCalendarDateUntil() {
        try (Context ctx = getJSContext()) {
            ctx.eval(ID, "let calendar = Temporal.Calendar.from('iso8601');");
            ctx.eval(ID, "let duration = calendar.dateUntil({ year: 2021, month: 4, day: 22 }, { year: 2021, month: 4, day: 23 });");
            validateDuration(ctx, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0);
        }
    }

    @Test
    public void testCalendarToString() {
        try (Context ctx = getJSContext()) {
            ctx.eval(ID, "let calendar = Temporal.Calendar.from('iso8601');");
            Value toStringValue = ctx.eval(ID, "calendar.toString();");
            assertEquals("iso8601", toStringValue.asString());
        }
    }

    @Test
    public void testCalendarToJSON() {
        try (Context ctx = getJSContext()) {
            ctx.eval(ID, "let calendar = Temporal.Calendar.from('iso8601');");
            Value toJSONValue = ctx.eval(ID, "calendar.toJSON();");
            assertEquals("iso8601", toJSONValue.asString());
        }
    }
// endregion

// region PlainDate Tests
    @Test
    public void testPlainDateCreation() {
        try (Context ctx = getJSContext()) {
            ctx.eval(ID, "let plainDate = new Temporal.PlainDate(2021, 4, 22)");
            validatePlainDate(ctx, 2021, 4, 22);
        }
    }

    @Test
    public void testPlainDateFrom() {
        try (Context ctx = getJSContext()) {
            ctx.eval(ID, "let plainDate = Temporal.PlainDate.from('1982-11-26')");
            validatePlainDate(ctx, 1982, 11, 26);
        }
    }

// endregion

    // region PlainDateTime Tests
    @Test
    public void testPlainDateTimeCreation() {
        try (Context ctx = getJSContext()) {
            ctx.eval(ID, "let plainDateTime = new Temporal.PlainDateTime(2021, 04, 23, 12, 45, 35, 520, 450, 860);");
            validatePlainDateTime(ctx, 2021, 04, 23, 12, 45, 35, 520, 450, 860);
        }
    }

    @Test
    public void testPlainDateTimeFrom() {
        try (Context ctx = getJSContext()) {
            ctx.eval(ID, "let plainDateTime = Temporal.PlainDateTime.from({ year: 2021, month: 4, day: 23, hour: 12, minute: 45, second: 35, millisecond: 520, microsecond: 450, nanosecond: 860 });");
            validatePlainDateTime(ctx, 2021, 04, 23, 12, 45, 35, 520, 450, 860);
        }
    }

    @Test
    public void testPlainDateTimeCompare() {
        try (Context ctx = getJSContext()) {
            ctx.eval(ID, "let plainDateTime = new Temporal.PlainDateTime(2021, 04, 23, 12, 45, 35, 520, 450, 860);");
            Value compareResult = ctx.eval(ID, "Temporal.PlainDateTime.compare(plainDateTime, plainDateTime);");
            assertEquals(0, compareResult.asInt());
        }
    }

// region PlainYearMonth Tests
    @Test
    public void testPlainYearMonthCreation() {
        try (Context ctx = getJSContext()) {
            ctx.eval(ID, "let plainYearMonth = new Temporal.PlainYearMonth(2021, 4)");
            validatePlainYearMonth(ctx, 2021, 4, "M04", 365, 30, 12, false);
        }
    }

    @Test
    public void testPlainYearMonthToSTring() {
        try (Context ctx = getJSContext()) {
            Value result = ctx.eval(ID, "let plainYearMonth = new Temporal.PlainYearMonth(2021, 4); plainYearMonth.toString();");
            assertTrue(result.isString());
            assertEquals("2021-04", result.asString());
        }
    }

// endregion

// region PlainMonthDay Tests
    @Test
    public void testPlainMonthDayCreation() {
        try (Context ctx = getJSContext()) {
            ctx.eval(ID, "let plainMonthDay = new Temporal.PlainMonthDay(4, 22)");
            validatePlainMonthDay(ctx, "M04", 22);
        }
    }
// endregion

    @Test
    public void testParsing() {
        try (Context ctx = getJSContext()) {
            try {
                ctx.enter();
                ctx.initialize(ID);
                TemporalParser parser = new TemporalParser(Strings.fromJavaString("2019-11-18T15:23:30.123456789+01:00[Europe/Madrid][u-ca=gregory]"));
                JSTemporalParserRecord rec = parser.parseISODateTime();
                assertEquals(2019, rec.getYear());
                assertEquals(11, rec.getMonth());
                assertEquals(18, rec.getDay());
                assertEquals(Strings.fromJavaString("Europe/Madrid"), rec.getTimeZoneIANAName());
                assertEquals(Strings.fromJavaString("gregory"), rec.getCalendar());
            } finally {
                ctx.leave();
            }
        }
    }

    @Test
    public void testTimeZoneParsing() {
        testTimeZoneFailFrom("2021-08-19T17:30");

        testTimeZoneFailConstructor("+00:01.1");
        // testTimeZoneFailConstructor("-01.1"); //TODO should fail, but passes

        // testTimeZoneConstructor("-08", "-08:00"); //TODO not sure this is correct any longer
        // testTimeZoneFrom("-08", "-08:00"); //TODO not sure this is correct any longer

        testTimeZoneConstructor("-08:00", "-08:00");

        testTimeZoneFrom("2021-08-19T17:30Z", "UTC");
        testTimeZoneFrom("2021-08-19T17:30-07:00", "-07:00");
        testTimeZoneFrom("2021-08-19T17:30[America/Vancouver]", "America/Vancouver");
        testTimeZoneFrom("2021-08-19T17:30Z[America/Vancouver]", "America/Vancouver");
        testTimeZoneFrom("2021-08-19T17:30-07:00[America/Vancouver]", "America/Vancouver");
    }

    private static void testTimeZoneFailFrom(String code) {
        try (Context ctx = getJSContext()) {
            Value result = ctx.eval(ID, "try { Temporal.TimeZone.from('" + code + "'); false; } catch (ex) { true; }");
            assertTrue(result.asBoolean());
        }
    }

    private static void testTimeZoneFailConstructor(String code) {
        try (Context ctx = getJSContext()) {
            Value result = ctx.eval(ID, "try { new Temporal.TimeZone('" + code + "'); false; } catch (ex) { true; }");
            assertTrue(result.asBoolean());
        }
    }

    private static void testTimeZoneFrom(String code, String expected) {
        try (Context ctx = getJSContext()) {
            Value result = ctx.eval(ID, "Temporal.TimeZone.from('" + code + "').id;");
            assertEquals(expected, result.asString());
        }
    }

    private static void testTimeZoneConstructor(String code, String expected) {
        try (Context ctx = getJSContext()) {
            Value result = ctx.eval(ID, "(new Temporal.TimeZone('" + code + "')).id;");
            assertEquals(expected, result.asString());
        }
    }

    @Test
    public void testInstant() {
        try (Context ctx = getJSContext()) {
            Value result = ctx.eval(ID, "Temporal.Instant.from('1900-01-01T12:00Z').toString();");
            assertEquals("1900-01-01T12:00:00Z", result.toString());
        }
    }

    @Test
    public void testTemporalBalancing() {
        try (Context ctx = getJSContext()) {
            // normal operations
            assertEquals("PT80M30S", ctx.eval(ID, "Temporal.Duration.from({ minutes: 80, seconds: 30 }).toString()").toString());
            assertEquals("PT81M30S", ctx.eval(ID, "Temporal.Duration.from({ minutes: 80, seconds: 90 }).round({largestUnit:'auto'}).toString()").toString());
            assertEquals("PT1H21M30S", ctx.eval(ID, "Temporal.Duration.from({ minutes: 80, seconds: 90 }).round({largestUnit:'hour'}).toString()").toString());

            // with relativeTo
            assertEquals("P370D", ctx.eval(ID, "Temporal.Duration.from({ days: 370 }).toString()").toString());
            assertEquals("P1Y5D", ctx.eval(ID, "Temporal.Duration.from({ days: 370 }).round({largestUnit:'year', relativeTo: '2019-01-01'}).toString()").toString());
            assertEquals("P1Y4D", ctx.eval(ID, "Temporal.Duration.from({ days: 370 }).round({largestUnit:'year', relativeTo: '2020-01-01'}).toString()").toString());

            assertEquals("P2D", ctx.eval(ID, "Temporal.Duration.from({ hours:48}).round({largestUnit: 'day'}).toString()").toString());
            assertEquals("P2DT1H", ctx.eval(ID, "Temporal.Duration.from({ hours:48}).round({largestUnit: 'day', relativeTo:'2020-03-08T00:00-08:00[America/Los_Angeles]'}).toString()").toString());

            // balancing in arithmetics
            assertEquals("PT27H15M", ctx.eval(ID, "let d1=Temporal.Duration.from({ hours: 26, minutes: 45 });\n" +
                            "let d2=Temporal.Duration.from({ minutes: 30 });\n" +
                            "d1.add(d2).toString()").toString());
            assertEquals("PT3H1M45S", ctx.eval(ID, "let d3=Temporal.Duration.from({ minutes: 80, seconds: 90 });\n" +
                            "let d4= Temporal.Duration.from({ minutes: 100, seconds: 15 });\n" +
                            "d3.add(d4).round({ largestUnit: 'hour' }).toString()").toString());
        }
    }

    @Test
    public void testTemporalRounding() {
        String code = "  function test(time1,time2,options,expected) {\n" +
                        "  let earlier = Temporal.PlainTime.from(time1);\n" +
                        "  let later = Temporal.PlainTime.from(time2);\n" +
                        "  let result = `${earlier.until(later, options)}`;\n" +
                        "  if (result !== expected) { throw new Error(expected+' expected but was '+result); }\n" +
                        "};\n";

        try (Context ctx = getJSContext()) {
            ctx.eval(ID, code);

            ctx.eval(ID, "test('01:00:00','02:00:00',{ smallestUnit: 'hours' }, 'PT1H');");
            ctx.eval(ID, "test('02:00:00','01:00:00',{ smallestUnit: 'hours' }, '-PT1H');");

            ctx.eval(ID, "test('01:00:00','01:01:00',{ smallestUnit: 'minutes' }, 'PT1M');");
            ctx.eval(ID, "test('01:00:00','02:01:00',{ smallestUnit: 'minutes' }, 'PT1H1M');");

            ctx.eval(ID, "test('01:00:00','01:00:01',{ smallestUnit: 'seconds' }, 'PT1S');");
            ctx.eval(ID, "test('01:00:00','02:00:01',{ smallestUnit: 'seconds' }, 'PT1H1S');");

            ctx.eval(ID, "test('01:02:03.45','02:00:00.000',{ smallestUnit: 'milliseconds', roundingMode: 'floor' }, 'PT57M56.55S');");
            ctx.eval(ID, "test('02:00:00.000','01:02:03.45',{ smallestUnit: 'milliseconds' }, '-PT57M56.55S');");
            ctx.eval(ID, "test('01:02:03.45','02:00:00.000',{ smallestUnit: 'milliseconds' }, 'PT57M56.55S');");
        }
    }

    @Test
    public void testTemporalParsingCalendar() {
        testCalendarIntl("iso8601", "iso8601");
        testCalendarIntl("1994-11-05T08:15:30-05:00", "iso8601");
    }

    private static void testCalendarIntl(String calendarInput, String expected) {
        try (Context ctx = getJSContext()) {
            Value result = ctx.eval(ID, "Temporal.Calendar.from('" + calendarInput + "').toString()");
            assertEquals(expected, result.asString());
        }
    }

    @Test
    public void testTemporalParsingDuration() {
        parseDurationIntl("-PT24.567890123H", "-PT24H34M4.404442799S");
        parseDurationIntl("-PT1.03125H", "-PT1H1M52.5S"); // #1754
    }

    private static void parseDurationIntl(String code, String expected) {
        try (Context ctx = getJSContext()) {
            Value result = ctx.eval(ID, "Temporal.Duration.from('" + code + "').toString();");
            Assert.assertEquals(expected, result.toString());
        }
    }

    @Test
    public void testSinceMicoseconds() {
        String code = "const earlier = new Temporal.PlainDateTime(2000, 5, 2, 12, 34, 56, 0, 0, 0); \n" +
                        "const later = new Temporal.PlainDateTime(2000, 5, 3, 13, 35, 57, 987, 654, 321); \n" +
                        "const result = later.since(earlier, { smallestUnit: 'microsecond' }); \n" +
                        "result.microseconds";
        try (Context ctx = getJSContext()) {
            Value result = ctx.eval(ID, code);
            Assert.assertEquals(654, result.asInt());
        }
    }

    @Test
    public void testInstantMultipleOffsets() {
        String code = "const epoch = new Temporal.Instant(0n);\n" +
                        "const str = '1970-01-01T00:02:00.000000000+00:02[+00:01:30.987654321]';\n" +
                        "Temporal.Instant.compare(str, epoch) === 0;";
        try (Context ctx = getJSContext()) {
            Value result = ctx.eval(ID, code);
            Assert.assertEquals(true, result.asBoolean());
        }
    }

}
