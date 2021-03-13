package com.oracle.truffle.js.builtins;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.js.builtins.TemporalCalendarPrototypeBuiltinsFactory.JSTemporalCalendarDateAddNodeGen;
import com.oracle.truffle.js.builtins.TemporalCalendarPrototypeBuiltinsFactory.JSTemporalCalendarDateFromFieldsNodeGen;
import com.oracle.truffle.js.builtins.TemporalCalendarPrototypeBuiltinsFactory.JSTemporalCalendarDateUntilNodeGen;
import com.oracle.truffle.js.builtins.TemporalCalendarPrototypeBuiltinsFactory.JSTemporalCalendarMonthDayFromFieldsNodeGen;
import com.oracle.truffle.js.builtins.TemporalCalendarPrototypeBuiltinsFactory.JSTemporalCalendarToStringNodeGen;
import com.oracle.truffle.js.builtins.TemporalCalendarPrototypeBuiltinsFactory.JSTemporalCalendarYearMonthFromFieldsNodeGen;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.binary.JSIdenticalNode;
import com.oracle.truffle.js.nodes.cast.JSStringToNumberNode;
import com.oracle.truffle.js.nodes.cast.JSToBooleanNode;
import com.oracle.truffle.js.nodes.cast.JSToIntegerAsLongNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.unary.IsConstructorNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSTemporalCalendar;
import com.oracle.truffle.js.runtime.builtins.JSTemporalCalendarObject;
import com.oracle.truffle.js.runtime.builtins.JSTemporalDuration;
import com.oracle.truffle.js.runtime.builtins.JSTemporalDurationObject;
import com.oracle.truffle.js.runtime.builtins.JSTemporalPlainDate;
import com.oracle.truffle.js.runtime.builtins.JSTemporalPlainDateObject;
import com.oracle.truffle.js.runtime.util.TemporalUtil;

public class TemporalCalendarPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<TemporalCalendarPrototypeBuiltins.TemporalCalendarPrototype> {

    public static final TemporalCalendarPrototypeBuiltins INSTANCE = new TemporalCalendarPrototypeBuiltins();

    protected TemporalCalendarPrototypeBuiltins() {
        super(JSTemporalCalendar.PROTOTYPE_NAME, TemporalCalendarPrototype.class);
    }

    public enum TemporalCalendarPrototype implements BuiltinEnum<TemporalCalendarPrototype> {
        dateFromFields(3),
        yearMonthFromFields(3),
        monthDayFromFields(3),
        dateAdd(4),
        dateUntil(3),
        toString(0),
        toJSON(0);

        private final int length;

        TemporalCalendarPrototype(int length) {
            this.length = length;
        }


        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, TemporalCalendarPrototype builtinEnum) {
        switch (builtinEnum) {
            case dateFromFields:
                return JSTemporalCalendarDateFromFieldsNodeGen.create(context, builtin, args().withThis().fixedArgs(3).createArgumentNodes(context));
            case yearMonthFromFields:
                return JSTemporalCalendarYearMonthFromFieldsNodeGen.create(context, builtin, args().withThis().fixedArgs(3).createArgumentNodes(context));
            case monthDayFromFields:
                return JSTemporalCalendarMonthDayFromFieldsNodeGen.create(context, builtin, args().withThis().fixedArgs(3).createArgumentNodes(context));
            case dateAdd:
                return JSTemporalCalendarDateAddNodeGen.create(context, builtin, args().withThis().fixedArgs(4).createArgumentNodes(context));
            case dateUntil:
                return JSTemporalCalendarDateUntilNodeGen.create(context, builtin, args().withThis().fixedArgs(3).createArgumentNodes(context));
            case toString:
            case toJSON:
                return JSTemporalCalendarToStringNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
        }
        return null;
    }

    // 12.4.4
    public abstract static class JSTemporalCalendarDateFromFields extends JSBuiltinNode {

        protected JSTemporalCalendarDateFromFields(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(limit = "3")
        public Object dateFromFields(DynamicObject thisObj, DynamicObject fields, DynamicObject options,
                                            DynamicObject constructor,
                                            @Cached("create()") IsObjectNode isObject,
                                            @Cached("create()") IsConstructorNode isConstructor,
                                            @Cached("createSameValue()") JSIdenticalNode identicalNode,
                                            @Cached("create()") JSToBooleanNode toBoolean,
                                            @Cached("create()") JSToStringNode toString,
                                            @Cached("create()") JSStringToNumberNode stringToNumber,
                                            @Cached("createNew()") JSFunctionCallNode callNode,
                                            @CachedLibrary("thisObj") DynamicObjectLibrary dol) {
            try {
                JSTemporalCalendarObject calendar = (JSTemporalCalendarObject) thisObj;
                assert calendar.getId().equals("iso8601");
                if (!isObject.executeBoolean(fields)) {
                    throw Errors.createRangeError("Given fields is not an object.");
                }
                options = TemporalUtil.normalizeOptionsObject(options, getContext().getRealm(), isObject);
                DynamicObject result = JSTemporalCalendar.isoDateFromFields(fields, options, getContext().getRealm(),
                        isObject, dol, toBoolean, toString, stringToNumber, identicalNode);
                return JSTemporalPlainDate.createTemporalDateFromStatic(constructor,
                        dol.getLongOrDefault(result, JSTemporalPlainDate.YEAR, 0),
                        dol.getLongOrDefault(result, JSTemporalPlainDate.MONTH, 0),
                        dol.getLongOrDefault(result, JSTemporalPlainDate.DAY, 0),
                        calendar, isConstructor, callNode
                );
            } catch (UnexpectedResultException e) {
                throw new RuntimeException(e);
            }
        }
    }

    // 12.4.5
    public abstract static class JSTemporalCalendarYearMonthFromFields extends JSBuiltinNode {

        protected JSTemporalCalendarYearMonthFromFields(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(limit = "3")
        public Object yearMonthFromFields(DynamicObject thisObj, DynamicObject fields, DynamicObject options,
                                          DynamicObject constructor,
                                          @Cached("create()") IsObjectNode isObject,
                                          @Cached("create()") IsConstructorNode isConstructor,
                                          @Cached("createSameValue()") JSIdenticalNode identicalNode,
                                          @Cached("create()") JSToBooleanNode toBoolean,
                                          @Cached("create()") JSToStringNode toString,
                                          @Cached("create()") JSStringToNumberNode stringToNumber,
                                          @Cached("createNew()") JSFunctionCallNode callNode,
                                          @CachedLibrary("thisObj") DynamicObjectLibrary dol) {
            JSTemporalCalendarObject calendar = (JSTemporalCalendarObject) thisObj;
            assert calendar.getId().equals("iso8601");
            if (!isObject.executeBoolean(fields)) {
                throw Errors.createTypeError("Given fields is not an object.");
            }
            options = TemporalUtil.normalizeOptionsObject(options, getContext().getRealm(), isObject);
            DynamicObject result = JSTemporalCalendar.isoYearMonthFromFields(fields, options, getContext().getRealm(),
                    isObject, dol, toBoolean, toString, stringToNumber, identicalNode);
            return null;    // TODO: Call JSTemporalYearMonth.createTemporalYearMonthFromStatic()
        }
    }

    // 12.4.6
    public abstract static class JSTemporalCalendarMonthDayFromFields extends JSBuiltinNode {

        protected JSTemporalCalendarMonthDayFromFields(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(limit = "3")
        public Object monthDayFromFields(DynamicObject thisObj, DynamicObject fields, DynamicObject options,
                                         DynamicObject constructor,
                                         @Cached("create()") IsObjectNode isObject,
                                         @Cached("create()") IsConstructorNode isConstructor,
                                         @Cached("createSameValue()") JSIdenticalNode identicalNode,
                                         @Cached("create()") JSToBooleanNode toBoolean,
                                         @Cached("create()") JSToStringNode toString,
                                         @Cached("create()") JSStringToNumberNode stringToNumber,
                                         @Cached("createNew()") JSFunctionCallNode callNode,
                                         @CachedLibrary("thisObj") DynamicObjectLibrary dol) {
            JSTemporalCalendarObject calendar = (JSTemporalCalendarObject) thisObj;
            assert calendar.getId().equals("iso8601");
            if (!isObject.executeBoolean(fields)) {
                throw Errors.createTypeError("Given fields is not an object.");
            }
            options = TemporalUtil.normalizeOptionsObject(options, getContext().getRealm(), isObject);
            DynamicObject result = JSTemporalCalendar.isoMonthDayFromFields(fields, options, getContext().getRealm(),
                    isObject, dol, toBoolean, toString, stringToNumber, identicalNode);
            return null;    // TODO: Call JSTemporalPlainMonthDay.createTemporalMonthDayFromStatic()
        }
    }

    // 12.4.7
    public abstract static class JSTemporalCalendarDateAdd extends JSBuiltinNode {

        protected JSTemporalCalendarDateAdd(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(limit = "3")
        public Object dateAdd(DynamicObject thisObj, DynamicObject dateObj, DynamicObject durationObj, DynamicObject options,
                              DynamicObject constructor,
                              @Cached("create()") IsObjectNode isObject,
                              @Cached("create()") IsConstructorNode isConstructor,
                              @Cached("create()") JSToBooleanNode toBoolean,
                              @Cached("create()") JSToStringNode toString,
                              @Cached("create()") JSToIntegerAsLongNode toInt,
                              @Cached("createNew()") JSFunctionCallNode callNode,
                              @CachedLibrary("thisObj") DynamicObjectLibrary dol) {
            try {
                JSTemporalCalendarObject calendar = (JSTemporalCalendarObject) thisObj;
                assert calendar.getId().equals("iso8601");
                JSTemporalPlainDateObject date = (JSTemporalPlainDateObject) JSTemporalPlainDate.toTemporalDate(dateObj,
                        null, null, getContext().getRealm(), isObject, dol, toBoolean, toString,
                        isConstructor, callNode);
                JSTemporalDurationObject duration = (JSTemporalDurationObject) JSTemporalDuration.toTemporalDuration(
                        durationObj, null, getContext().getRealm(), isObject, toInt, dol, toString, isConstructor, callNode);
                options = TemporalUtil.normalizeOptionsObject(options, getContext().getRealm(), isObject);
                String overflow = TemporalUtil.toTemporalOverflow(options, dol, isObject, toBoolean, toString);
                DynamicObject result = JSTemporalPlainDate.addISODate(date.getYear(), date.getMonth(), date.getDay(),
                        duration.getYears(), duration.getMonths(), duration.getWeeks(), duration.getDays(), overflow,
                        getContext().getRealm(), dol);
                return JSTemporalPlainDate.createTemporalDateFromStatic(constructor,
                        dol.getLongOrDefault(result, JSTemporalPlainDate.YEAR, 0L),
                        dol.getLongOrDefault(result, JSTemporalPlainDate.MONTH, 0L),
                        dol.getLongOrDefault(result, JSTemporalPlainDate.DAY, 0L),
                        calendar, isConstructor, callNode);
            } catch (UnexpectedResultException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public abstract static class JSTemporalCalendarDateUntil extends JSBuiltinNode {

        protected JSTemporalCalendarDateUntil(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(limit = "3")
        public Object dateUntil(DynamicObject thisObj, DynamicObject oneObj, DynamicObject twoObj, DynamicObject options,
                                @Cached("create()") IsObjectNode isObject,
                                @Cached("create()") IsConstructorNode isConstructor,
                                @Cached("create()") JSToBooleanNode toBoolean,
                                @Cached("create()") JSToStringNode toString,
                                @Cached("createNew()") JSFunctionCallNode callNode,
                                @CachedLibrary("thisObj") DynamicObjectLibrary dol) {
            try {
                JSTemporalCalendarObject calendar = (JSTemporalCalendarObject) thisObj;
                assert calendar.getId().equals("iso8601");
                JSTemporalPlainDateObject one = (JSTemporalPlainDateObject) JSTemporalPlainDate.toTemporalDate(oneObj,
                        null, null, getContext().getRealm(), isObject, dol, toBoolean, toString,
                        isConstructor, callNode);
                JSTemporalPlainDateObject two = (JSTemporalPlainDateObject) JSTemporalPlainDate.toTemporalDate(twoObj,
                        null, null, getContext().getRealm(), isObject, dol, toBoolean, toString,
                        isConstructor, callNode);
                options = TemporalUtil.normalizeOptionsObject(options, getContext().getRealm(), isObject);
                String largestUnit = TemporalUtil.toLargestTemporalUnit(options,
                        TemporalUtil.toSet(JSTemporalDuration.HOURS, JSTemporalDuration.MINUTES, JSTemporalDuration.SECONDS,
                                JSTemporalDuration.MILLISECONDS, JSTemporalDuration.MICROSECONDS,
                                JSTemporalDuration.NANOSECONDS), JSTemporalDuration.DAYS, dol, isObject, toBoolean, toString);
                DynamicObject result = JSTemporalPlainDate.differenceISODate(
                        one.getYear(), one.getMonth(), one.getDay(), two.getYear(), two.getMonth(), two.getDay(),
                        largestUnit, getContext().getRealm(), dol
                );
                return JSTemporalDuration.createTemporalDuration(
                        dol.getLongOrDefault(result, JSTemporalDuration.YEARS, 0L),
                        dol.getLongOrDefault(result, JSTemporalDuration.MONTHS, 0L),
                        dol.getLongOrDefault(result, JSTemporalDuration.WEEKS, 0L),
                        dol.getLongOrDefault(result, JSTemporalDuration.DAYS, 0L),
                        0, 0, 0, 0, 0, 0,
                        getContext().getRealm()
                );
            } catch (UnexpectedResultException e) {
                throw new RuntimeException(e);
            }
        }
    }

    // 12.4.23
    public abstract static class JSTemporalCalendarToString extends JSBuiltinNode {

        protected JSTemporalCalendarToString(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public String toString(DynamicObject thisObj) {
            JSTemporalCalendarObject calendar = (JSTemporalCalendarObject) thisObj;
            return calendar.getId();
        }
    }
}
