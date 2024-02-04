/*
 * Copyright (c) 2022, 2024, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.nodes.temporal;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.builtins.temporal.CalendarMethodsRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDurationObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDate;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateObject;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.util.TemporalUtil;
import com.oracle.truffle.js.runtime.util.TemporalUtil.Overflow;
import com.oracle.truffle.js.runtime.util.TemporalUtil.Unit;

/**
 * Implementation of the Temporal AddDate operation.
 */
public abstract class TemporalAddDateNode extends JavaScriptBaseNode {

    protected TemporalAddDateNode() {
    }

    public abstract JSTemporalPlainDateObject execute(CalendarMethodsRecord calendarRec, JSTemporalPlainDateObject plainDate, JSTemporalDurationObject duration, JSDynamicObject optionsOpt);

    @Specialization
    protected JSTemporalPlainDateObject addDate(CalendarMethodsRecord calendarRec, JSTemporalPlainDateObject plainDate, JSTemporalDurationObject duration, JSDynamicObject options,
                    @Cached ToTemporalCalendarObjectNode toCalendarObject,
                    @Cached("createCall()") JSFunctionCallNode callDateAddNode,
                    @Cached InlinedBranchProfile errorBranch,
                    @Cached TemporalGetOptionNode getOptionNode) {
        if (duration.getYears() != 0 || duration.getMonths() != 0 || duration.getWeeks() != 0) {
            // CalendarDateAdd(calendarRec, plainDate, duration, options).
            Object calendar = toCalendarObject.execute(calendarRec.receiver());
            Object addedDate = callDateAddNode.executeCall(JSArguments.create(calendar, calendarRec.dateAdd(), plainDate, duration, options));
            return TemporalUtil.requireTemporalDate(addedDate, this, errorBranch);
        } else {
            JSContext ctx = getLanguage().getJSContext();
            JSRealm realm = getRealm();
            Overflow overflow = TemporalUtil.toTemporalOverflow(options, getOptionNode);
            double days = TemporalUtil.balanceTimeDuration(duration.getDays(),
                            duration.getHours(), duration.getMinutes(), duration.getSeconds(),
                            duration.getMilliseconds(), duration.getMicroseconds(), duration.getNanoseconds(), Unit.DAY).days();
            var result = TemporalUtil.addISODate(plainDate.getYear(), plainDate.getMonth(), plainDate.getDay(), 0, 0, 0, days, overflow);
            return JSTemporalPlainDate.create(ctx, realm, result.year(), result.month(), result.day(), calendarRec.receiver(), this, errorBranch);
        }
    }

}
