/*
 * Copyright (c) 2018, 2024, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.builtins.math;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.SafeInteger;

/**
 * Math.ceil(x). Returns the smallest (closest to -Infinity) Number value that is not less than x
 * and is an integer. If x is already an integer, the result is x.
 *
 * The value of {@code Math.ceil(x)} is the same as the value of {@code -Math.floor(-x)}.
 */
public abstract class CeilNode extends MathOperation {

    public CeilNode(JSContext context, JSBuiltin builtin) {
        super(context, builtin);
    }

    @Specialization
    protected static int ceilInt(int a) {
        return a;
    }

    @Specialization
    protected static SafeInteger ceilSafeInt(SafeInteger a) {
        return a;
    }

    @Specialization
    protected final Object ceilDouble(double d,
                    @Cached @Shared InlinedConditionProfile isZero,
                    @Cached @Shared InlinedConditionProfile requiresNegativeZero,
                    @Cached @Shared InlinedConditionProfile fitsInt,
                    @Cached @Shared InlinedConditionProfile fitsSafeLong) {
        if (isZero.profile(this, d == 0.0)) {
            // ceil(-0.0) => -0.0
            // ceil(+0.0) => +0.0
            return d;
        } else if (fitsInt.profile(this, d >= Integer.MIN_VALUE && d <= Integer.MAX_VALUE)) {
            int i = (int) d;
            int result = d > i ? i + 1 : i;
            if (requiresNegativeZero.profile(this, result == 0 && d < 0)) {
                return -0.0;
            }
            return result;
        } else if (fitsSafeLong.profile(this, JSRuntime.isSafeInteger(d))) {
            long i = (long) d;
            long result = d > i ? i + 1 : i;
            if (requiresNegativeZero.profile(this, result == 0 && d < 0)) {
                return -0.0;
            }
            return SafeInteger.valueOf(result);
        } else {
            return Math.ceil(d);
        }
    }

    @Specialization(replaces = "ceilDouble")
    protected final Object ceilToDouble(Object a,
                    @Cached @Shared InlinedConditionProfile isZero,
                    @Cached @Shared InlinedConditionProfile requiresNegativeZero,
                    @Cached @Shared InlinedConditionProfile fitsInt,
                    @Cached @Shared InlinedConditionProfile fitsSafeLong) {
        double d = toDouble(a);
        return ceilDouble(d, isZero, requiresNegativeZero, fitsInt, fitsSafeLong);
    }
}
