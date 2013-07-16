/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime;

import static com.github.anba.es6draft.runtime.internal.Errors.throwRangeError;
import static com.github.anba.es6draft.runtime.internal.Errors.throwTypeError;
import static com.github.anba.es6draft.runtime.internal.ScriptRuntime.instanceOfOperator;
import static com.github.anba.es6draft.runtime.objects.BooleanObject.BooleanCreate;
import static com.github.anba.es6draft.runtime.objects.NumberObject.NumberCreate;
import static com.github.anba.es6draft.runtime.objects.internal.ListIterator.FromListIterator;
import static com.github.anba.es6draft.runtime.types.builtins.ExoticArray.ArrayCreate;
import static com.github.anba.es6draft.runtime.types.builtins.ExoticString.StringCreate;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject.ObjectCreate;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.mozilla.javascript.DToA;
import org.mozilla.javascript.StringToNumber;
import org.mozilla.javascript.v8dtoa.FastDtoa;

import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.internal.Strings;
import com.github.anba.es6draft.runtime.objects.FunctionPrototype;
import com.github.anba.es6draft.runtime.types.*;
import com.github.anba.es6draft.runtime.types.builtins.ExoticBoundFunction;
import com.github.anba.es6draft.runtime.types.builtins.FunctionObject;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;
import com.google.doubleconversion.DoubleConversion;

/**
 * <h1>9 Abstract Operations</h1>
 * <ul>
 * <li>9.1 Type Conversion and Testing
 * <li>9.2 Testing and Comparison Operations
 * <li>9.3 Operations on Objects
 * </ul>
 */
public final class AbstractOperations {
    private AbstractOperations() {
    }

    /**
     * 9.1.1 ToPrimitive
     */
    public static Object ToPrimitive(ExecutionContext cx, Object val) {
        // null == no hint
        return ToPrimitive(cx, val, null);
    }

    /**
     * 9.1.1 ToPrimitive
     */
    public static Object ToPrimitive(ExecutionContext cx, Object argument, Type preferredType) {
        if (!Type.isObject(argument)) {
            return argument;
        }
        return ToPrimitive(cx, Type.objectValue(argument), preferredType);
    }

    /**
     * 9.1.1 ToPrimitive
     * <p>
     * ToPrimitive for the Object type
     */
    private static Object ToPrimitive(ExecutionContext cx, ScriptObject argument, Type preferredType) {
        /* steps 4-5 */
        Object exoticToPrim = Get(cx, argument, BuiltinSymbol.ToPrimitive.get());
        /* step 6 */
        if (!Type.isUndefined(exoticToPrim)) {
            if (!IsCallable(exoticToPrim))
                throw throwTypeError(cx, Messages.Key.NotCallable);
            /* steps 1-3 */
            String hint;
            if (preferredType == null) {
                hint = "default";
            } else if (preferredType == Type.String) {
                hint = "string";
            } else {
                assert preferredType == Type.Number;
                hint = "number";
            }
            Object result = ((Callable) exoticToPrim).call(cx, argument, hint);
            if (!Type.isObject(result)) {
                return result;
            }
            throw throwTypeError(cx, Messages.Key.NotPrimitiveType);
        }
        /* steps 7-8 */
        if (preferredType == null) {
            preferredType = Type.Number;
        }
        return OrdinaryToPrimitive(cx, argument, preferredType);
    }

    /**
     * 9.1.1 ToPrimitive
     * <p>
     * OrdinaryToPrimitive
     */
    public static Object OrdinaryToPrimitive(ExecutionContext cx, ScriptObject object, Type hint) {
        /* steps 1-2 */
        assert hint == Type.String || hint == Type.Number;
        /* steps 3-4 */
        String tryFirst, trySecond;
        if (hint == Type.String) {
            tryFirst = "toString";
            trySecond = "valueOf";
        } else {
            tryFirst = "valueOf";
            trySecond = "toString";
        }
        /* steps 5-7 */
        Object first = Get(cx, object, tryFirst);
        if (IsCallable(first)) {
            Object result = ((Callable) first).call(cx, object);
            if (!Type.isObject(result)) {
                return result;
            }
        }
        /* steps 8-10 */
        Object second = Get(cx, object, trySecond);
        if (IsCallable(second)) {
            Object result = ((Callable) second).call(cx, object);
            if (!Type.isObject(result)) {
                return result;
            }
        }
        /* step 10 */
        throw throwTypeError(cx, Messages.Key.NoPrimitiveRepresentation);
    }

    /**
     * 9.1.2 ToBoolean
     */
    public static boolean ToBoolean(Object val) {
        switch (Type.of(val)) {
        case Undefined:
            return false;
        case Null:
            return false;
        case Boolean:
            return Type.booleanValue(val);
        case Number:
            double d = Type.numberValue(val);
            return !(d == 0 || Double.isNaN(d));
        case String:
            return Type.stringValue(val).length() != 0;
        case Symbol:
            return true;
        case Object:
        default:
            return true;
        }
    }

    /**
     * 9.1.2 ToBoolean
     */
    public static boolean ToBoolean(double d) {
        return !(d == 0 || Double.isNaN(d));
    }

    /**
     * 9.1.3 ToNumber
     */
    public static double ToNumber(ExecutionContext cx, Object val) {
        switch (Type.of(val)) {
        case Undefined:
            return Double.NaN;
        case Null:
            return +0;
        case Boolean:
            return Type.booleanValue(val) ? 1 : +0;
        case Number:
            return Type.numberValue(val);
        case String:
            return ToNumber(Type.stringValue(val));
        case Symbol:
            return Double.NaN;
        case Object:
        default:
            Object primValue = ToPrimitive(cx, val, Type.Number);
            return ToNumber(cx, primValue);
        }
    }

    /**
     * 9.1.3.1 ToNumber Applied to the String Type
     */
    public static double ToNumber(CharSequence s) {
        return NumberParser.parse(s.toString());
    }

    /**
     * 9.1.4 ToInteger
     */
    public static double ToInteger(ExecutionContext cx, Object val) {
        double number = ToNumber(cx, val);
        if (Double.isNaN(number))
            return +0.0;
        if (number == 0.0 || Double.isInfinite(number))
            return number;
        return Math.signum(number) * Math.floor(Math.abs(number));
    }

    /**
     * 9.1.4 ToInteger
     */
    public static double ToInteger(double number) {
        if (Double.isNaN(number))
            return +0.0;
        if (number == 0.0 || Double.isInfinite(number))
            return number;
        return Math.signum(number) * Math.floor(Math.abs(number));
    }

    /**
     * 9.1.5 ToInt32: (Signed 32 Bit Integer)
     */
    public static int ToInt32(ExecutionContext cx, Object val) {
        double number = ToNumber(cx, val);
        return DoubleConversion.doubleToInt32(number);
    }

    /**
     * 9.1.5 ToInt32: (Signed 32 Bit Integer)
     */
    public static int ToInt32(double number) {
        return DoubleConversion.doubleToInt32(number);
    }

    /**
     * 9.1.6 ToUint32: (Unsigned 32 Bit Integer)
     */
    public static long ToUint32(ExecutionContext cx, Object val) {
        double number = ToNumber(cx, val);
        return DoubleConversion.doubleToInt32(number) & 0xffffffffL;
    }

    /**
     * 9.1.6 ToUint32: (Unsigned 32 Bit Integer)
     */
    public static long ToUint32(double number) {
        return DoubleConversion.doubleToInt32(number) & 0xffffffffL;
    }

    /**
     * 9.1.7 ToUint16: (Unsigned 16 Bit Integer)
     */
    public static char ToUint16(ExecutionContext cx, Object val) {
        double number = ToNumber(cx, val);
        return (char) DoubleConversion.doubleToInt32(number);
    }

    /**
     * 9.1.7 ToUint16: (Unsigned 16 Bit Integer)
     */
    public static char ToUint16(double number) {
        return (char) DoubleConversion.doubleToInt32(number);
    }

    /**
     * 9.1.8 ToString
     */
    public static CharSequence ToString(ExecutionContext cx, Object val) {
        switch (Type.of(val)) {
        case Undefined:
            return "undefined";
        case Null:
            return "null";
        case Boolean:
            return Type.booleanValue(val) ? "true" : "false";
        case Number:
            return ToString(Type.numberValue(val));
        case String:
            return Type.stringValue(val);
        case Symbol:
            return "[object Symbol]";
        case Object:
        default:
            Object primValue = ToPrimitive(cx, val, Type.String);
            return ToString(cx, primValue);
        }
    }

    /**
     * 9.1.8 ToString
     */
    public static String ToFlatString(ExecutionContext cx, Object val) {
        return ToString(cx, val).toString();
    }

    /**
     * 9.1.8.1 ToString Applied to the Number Type
     */
    public static String ToString(double val) {
        if ((int) val == val) {
            return Integer.toString((int) val);
        } else if (val != val) {
            return "NaN";
        } else if (val == Double.POSITIVE_INFINITY) {
            return "Infinity";
        } else if (val == Double.NEGATIVE_INFINITY) {
            return "-Infinity";
        } else if (val == 0.0) {
            return "0";
        }

        // call DToA for general number-to-string
        String result = FastDtoa.numberToString(val);
        if (result != null) {
            return result;
        }
        StringBuilder buffer = new StringBuilder();
        DToA.JS_dtostr(buffer, DToA.DTOSTR_STANDARD, 0, val);
        return buffer.toString();
    }

    /**
     * 9.1.9 ToObject
     */
    public static ScriptObject ToObject(ExecutionContext cx, Object val) {
        switch (Type.of(val)) {
        case Undefined:
        case Null:
            throw throwTypeError(cx, Messages.Key.UndefinedOrNull);
        case Boolean:
            return BooleanCreate(cx, Type.booleanValue(val));
        case Number:
            return NumberCreate(cx, Type.numberValue(val));
        case String:
            return StringCreate(cx, Type.stringValue(val));
        case Symbol:
            throw throwTypeError(cx, Messages.Key.SymbolObject);
        case Object:
        default:
            return Type.objectValue(val);
        }
    }

    /**
     * 9.1.10 ToPropertyKey
     */
    public static Object ToPropertyKey(ExecutionContext cx, Object val) {
        if (Type.isSymbol(val)) {
            return val;
        }
        return ToFlatString(cx, val);
    }

    /**
     * 9.2.1 CheckObjectCoercible
     */
    public static Object CheckObjectCoercible(ExecutionContext cx, Object val) {
        if (Type.isUndefinedOrNull(val)) {
            throw throwTypeError(cx, Messages.Key.UndefinedOrNull);
        }
        if (Type.isSymbol(val)) {
            throw throwTypeError(cx, Messages.Key.SymbolObject);
        }
        return val;
    }

    /**
     * 9.2.2 IsCallable
     */
    public static boolean IsCallable(Object val) {
        return Type.isObject(val) && (val instanceof Callable);
    }

    /**
     * 9.2.3 SameValue(x, y)
     */
    public static boolean SameValue(Object x, Object y) {
        if (x == y) {
            return true;
        } else if (x == null || y == null) {
            return false;
        }
        Type tx = Type.of(x);
        Type ty = Type.of(y);
        if (tx != ty) {
            return false;
        }
        if (tx == Type.Undefined) {
            return true;
        }
        if (tx == Type.Null) {
            return true;
        }
        if (tx == Type.Number) {
            double dx = Type.numberValue(x);
            double dy = Type.numberValue(y);
            return Double.compare(dx, dy) == 0;
        }
        if (tx == Type.String) {
            return Type.stringValue(x).toString().contentEquals(Type.stringValue(y));
        }
        if (tx == Type.Boolean) {
            return Type.booleanValue(x) == Type.booleanValue(y);
        }
        assert tx == Type.Object || tx == Type.Symbol;
        return (x == y);
    }

    /**
     * 9.2.4 SameValueZero(x, y)
     */
    public static boolean SameValueZero(Object x, Object y) {
        if (x == y) {
            return true;
        } else if (x == null || y == null) {
            return false;
        }
        Type tx = Type.of(x);
        Type ty = Type.of(y);
        if (tx != ty) {
            return false;
        }
        if (tx == Type.Undefined) {
            return true;
        }
        if (tx == Type.Null) {
            return true;
        }
        if (tx == Type.Number) {
            double dx = Type.numberValue(x);
            double dy = Type.numberValue(y);
            if (dx == 0) {
                return (dy == 0);
            }
            return Double.compare(dx, dy) == 0;
        }
        if (tx == Type.String) {
            return Type.stringValue(x).toString().contentEquals(Type.stringValue(y));
        }
        if (tx == Type.Boolean) {
            return Type.booleanValue(x) == Type.booleanValue(y);
        }
        assert tx == Type.Object || tx == Type.Symbol;
        return (x == y);
    }

    /**
     * 9.2.5 IsConstructor
     */
    public static boolean IsConstructor(Object val) {
        return Type.isObject(val) && (val instanceof Constructor);
    }

    /**
     * 9.2.6 IsPropertyKey
     */
    public static boolean IsPropertyKey(Object val) {
        if (Type.isString(val) || Type.isSymbol(val)) {
            return true;
        }
        return false;
    }

    /**
     * 9.2.7 IsExtensible (O)
     */
    public static boolean IsExtensible(ExecutionContext cx, ScriptObject object) {
        return object.isExtensible(cx);
    }

    /**
     * 9.3.1 Get (O, P)
     */
    public static Object Get(ExecutionContext cx, ScriptObject object, String propertyKey) {
        return object.get(cx, propertyKey, object);
    }

    /**
     * 9.3.1 Get (O, P)
     */
    public static Object Get(ExecutionContext cx, ScriptObject object, Symbol propertyKey) {
        return object.get(cx, propertyKey, object);
    }

    /**
     * 9.3.2 Put (O, P, V, Throw)
     */
    public static void Put(ExecutionContext cx, ScriptObject object, String propertyKey,
            Object value, boolean _throw) {
        boolean success = object.set(cx, propertyKey, value, object);
        if (!success && _throw) {
            throw throwTypeError(cx, Messages.Key.PropertyNotModifiable, propertyKey);
        }
    }

    /**
     * 9.3.2 Put (O, P, V, Throw)
     */
    public static void Put(ExecutionContext cx, ScriptObject object, Symbol propertyKey,
            Object value, boolean _throw) {
        boolean success = object.set(cx, propertyKey, value, object);
        if (!success && _throw) {
            throw throwTypeError(cx, Messages.Key.PropertyNotModifiable, propertyKey.toString());
        }
    }

    /**
     * 9.3.3 CreateOwnDataProperty (O, P, V)
     */
    public static boolean CreateOwnDataProperty(ExecutionContext cx, ScriptObject object,
            String propertyKey, Object value) {
        assert !object.hasOwnProperty(cx, propertyKey);
        PropertyDescriptor newDesc = new PropertyDescriptor(value, true, true, true);
        return object.defineOwnProperty(cx, propertyKey, newDesc);
    }

    /**
     * 9.3.3 CreateOwnDataProperty (O, P, V)
     */
    public static boolean CreateOwnDataProperty(ExecutionContext cx, ScriptObject object,
            Symbol propertyKey, Object value) {
        assert !object.hasOwnProperty(cx, propertyKey);
        PropertyDescriptor newDesc = new PropertyDescriptor(value, true, true, true);
        return object.defineOwnProperty(cx, propertyKey, newDesc);
    }

    /**
     * 9.3.4 DefinePropertyOrThrow (O, P, desc)
     */
    public static void DefinePropertyOrThrow(ExecutionContext cx, ScriptObject object,
            String propertyKey, PropertyDescriptor desc) {
        boolean success = object.defineOwnProperty(cx, propertyKey, desc);
        if (!success) {
            throw throwTypeError(cx, Messages.Key.PropertyNotCreatable, propertyKey);
        }
    }

    /**
     * 9.3.4 DefinePropertyOrThrow (O, P, desc)
     */
    public static void DefinePropertyOrThrow(ExecutionContext cx, ScriptObject object,
            Symbol propertyKey, PropertyDescriptor desc) {
        boolean success = object.defineOwnProperty(cx, propertyKey, desc);
        if (!success) {
            throw throwTypeError(cx, Messages.Key.PropertyNotCreatable, propertyKey.toString());
        }
    }

    /**
     * 9.3.5 DeletePropertyOrThrow (O, P)
     */
    public static void DeletePropertyOrThrow(ExecutionContext cx, ScriptObject object,
            String propertyKey) {
        boolean success = object.delete(cx, propertyKey);
        if (!success) {
            throw throwTypeError(cx, Messages.Key.PropertyNotDeletable, propertyKey);
        }
    }

    /**
     * 9.3.5 DeletePropertyOrThrow (O, P)
     */
    public static void DeletePropertyOrThrow(ExecutionContext cx, ScriptObject object,
            Symbol propertyKey) {
        boolean success = object.delete(cx, propertyKey);
        if (!success) {
            throw throwTypeError(cx, Messages.Key.PropertyNotDeletable, propertyKey.toString());
        }
    }

    /**
     * 9.3.6 HasProperty (O, P)
     */
    public static boolean HasProperty(ExecutionContext cx, ScriptObject object, String propertyKey) {
        return object.hasProperty(cx, propertyKey);
    }

    /**
     * 9.3.6 HasProperty (O, P)
     */
    public static boolean HasProperty(ExecutionContext cx, ScriptObject object, Symbol propertyKey) {
        return object.hasProperty(cx, propertyKey);
    }

    /**
     * 9.3.7 GetMethod (O, P)
     */
    public static Callable GetMethod(ExecutionContext cx, ScriptObject object, String propertyKey) {
        Object func = object.get(cx, propertyKey, object);
        if (Type.isUndefined(func)) {
            return null;
        }
        if (!IsCallable(func)) {
            throw throwTypeError(cx, Messages.Key.NotCallable);
        }
        return (Callable) func;
    }

    /**
     * 9.3.7 GetMethod (O, P)
     */
    public static Callable GetMethod(ExecutionContext cx, ScriptObject object, Symbol propertyKey) {
        Object func = object.get(cx, propertyKey, object);
        if (Type.isUndefined(func)) {
            return null;
        }
        if (!IsCallable(func)) {
            throw throwTypeError(cx, Messages.Key.NotCallable);
        }
        return (Callable) func;
    }

    /**
     * 9.3.8 Invoke(O,P [,args])
     */
    public static Object Invoke(ExecutionContext cx, Object object, String propertyKey,
            Object... args) {
        ScriptObject base;
        if (Type.isObject(object)) {
            base = Type.objectValue(object);
        } else {
            base = ToObject(cx, object);
        }
        return base.invoke(cx, propertyKey, args, object);
    }

    /**
     * 9.3.8 Invoke(O,P [,args])
     */
    public static Object Invoke(ExecutionContext cx, Object object, Symbol propertyKey,
            Object... args) {
        ScriptObject base;
        if (Type.isObject(object)) {
            base = Type.objectValue(object);
        } else {
            base = ToObject(cx, object);
        }
        return base.invoke(cx, propertyKey, args, object);
    }

    /**
     * 9.3.9 SetIntegrityLevel (O, level)
     */
    public static boolean SetIntegrityLevel(ExecutionContext cx, ScriptObject object,
            IntegrityLevel level) {
        /* step 1-2 */
        assert level == IntegrityLevel.Sealed || level == IntegrityLevel.Frozen;
        /* step 3-4 */
        Iterator<?> keys = FromListIterator(cx, object.ownPropertyKeys(cx));
        /* step 5 */
        ScriptException pendingException = null;
        if (level == IntegrityLevel.Sealed) {
            /* step 6 */
            PropertyDescriptor nonConfigurable = new PropertyDescriptor();
            nonConfigurable.setConfigurable(false);
            while (keys.hasNext()) {
                // FIXME: spec bug? (missing call to ToPropertyKey()?)
                Object key = ToPropertyKey(cx, keys.next());
                try {
                    if (key instanceof String) {
                        DefinePropertyOrThrow(cx, object, (String) key, nonConfigurable);
                    } else {
                        assert key instanceof Symbol;
                        DefinePropertyOrThrow(cx, object, (Symbol) key, nonConfigurable);
                    }
                } catch (ScriptException e) {
                    if (pendingException == null) {
                        pendingException = e;
                    }
                }
            }
        } else {
            /* step 7 */
            PropertyDescriptor nonConfigurable = new PropertyDescriptor();
            nonConfigurable.setConfigurable(false);
            PropertyDescriptor nonConfigurableWritable = new PropertyDescriptor();
            nonConfigurableWritable.setConfigurable(false);
            nonConfigurableWritable.setWritable(false);
            while (keys.hasNext()) {
                try {
                    // FIXME: spec bug? (missing call to ToPropertyKey()?)
                    Object key = ToPropertyKey(cx, keys.next());
                    Property currentDesc;
                    if (key instanceof String) {
                        currentDesc = object.getOwnProperty(cx, (String) key);
                    } else {
                        assert key instanceof Symbol;
                        currentDesc = object.getOwnProperty(cx, (Symbol) key);
                    }
                    if (currentDesc != null) {
                        PropertyDescriptor desc;
                        if (currentDesc.isAccessorDescriptor()) {
                            desc = nonConfigurable;
                        } else {
                            desc = nonConfigurableWritable;
                        }
                        if (key instanceof String) {
                            DefinePropertyOrThrow(cx, object, (String) key, desc);
                        } else {
                            assert key instanceof Symbol;
                            DefinePropertyOrThrow(cx, object, (Symbol) key, desc);
                        }
                    }
                } catch (ScriptException e) {
                    if (pendingException == null) {
                        pendingException = e;
                    }
                }
            }
        }
        /* step 8 */
        if (pendingException != null) {
            throw pendingException;
        }
        /* step 9 */
        return object.preventExtensions(cx);
    }

    /**
     * 9.3.10 TestIntegrityLevel (O, level)
     */
    public static boolean TestIntegrityLevel(ExecutionContext cx, ScriptObject object,
            IntegrityLevel level) {
        /* step 1-2 */
        assert level == IntegrityLevel.Sealed || level == IntegrityLevel.Frozen;
        /* step 3-4 */
        boolean status = IsExtensible(cx, object);
        /* step 5-6 */
        if (status) {
            return false;
        }
        /* step 7-8 */
        Iterator<?> keys = FromListIterator(cx, object.ownPropertyKeys(cx));
        /* step 9 */
        ScriptException pendingException = null;
        /* step 10 */
        boolean configurable = false;
        /* step 11 */
        boolean writable = false;
        while (keys.hasNext()) {
            // FIXME: spec bug? (missing call to ToPropertyKey()?)
            Object key = ToPropertyKey(cx, keys.next());
            /* step 12 */
            try {
                Property currentDesc;
                if (key instanceof String) {
                    currentDesc = object.getOwnProperty(cx, (String) key);
                } else {
                    assert key instanceof Symbol;
                    currentDesc = object.getOwnProperty(cx, (Symbol) key);
                }
                if (currentDesc != null) {
                    configurable |= currentDesc.isConfigurable();
                    if (currentDesc.isDataDescriptor()) {
                        writable |= currentDesc.isWritable();
                    }
                }
            } catch (ScriptException e) {
                if (pendingException == null) {
                    pendingException = e;
                }
                configurable = true;
            }
        }
        /* step 13 */
        if (pendingException != null) {
            throw pendingException;
        }
        /* step 14 */
        if (level == IntegrityLevel.Frozen && writable) {
            return false;
        }
        /* step 15 */
        if (configurable) {
            return false;
        }
        /* step 16 */
        return true;
    }

    /**
     * 9.3.11 CreateArrayFromList (elements)
     */
    public static ScriptObject CreateArrayFromList(ExecutionContext cx, List<?> elements) {
        /* step 2 */
        ScriptObject array = ArrayCreate(cx, 0);
        /* step 3 */
        int n = 0;
        /* step 4 */
        for (Object e : elements) {
            boolean status = CreateOwnDataProperty(cx, array, ToString(n), e);
            assert status;
            n += 1;
        }
        /* step 5 */
        return array;
    }

    /**
     * 9.3.12 CreateListFromArrayLike (obj)
     */
    public static Object[] CreateListFromArrayLike(ExecutionContext cx, Object obj) {
        /* step 1 */
        if (!Type.isObject(obj)) {
            throwTypeError(cx, Messages.Key.NotObjectType);
        }
        ScriptObject object = Type.objectValue(obj);
        /* step 2 */
        Object len = Get(cx, object, "length");
        /* steps 3-4 */
        double n = ToInteger(cx, len);
        // CreateListFromArrayLike() is (currently) only used for argument arrays
        if (n > FunctionPrototype.getMaxArguments()) {
            throw throwRangeError(cx, Messages.Key.FunctionTooManyArguments);
        }
        int length = n > 0 ? (int) n : 0;
        /* step 5 */
        Object[] list = new Object[length];
        /* steps 6-7 */
        for (int index = 0; index < length; ++index) {
            String indexName = ToString(index);
            Object next = Get(cx, object, indexName);
            list[index] = next;
        }
        /* step 8 */
        return list;
    }

    /**
     * 9.3.13 OrdinaryHasInstance (C, O)
     */
    public static boolean OrdinaryHasInstance(ExecutionContext cx, Object c, Object o) {
        /* step 1 */
        if (!IsCallable(c)) {
            return false;
        }
        /* step 2 */
        if (c instanceof ExoticBoundFunction) {
            Callable bc = ((ExoticBoundFunction) c).getBoundTargetFunction();
            return instanceOfOperator(o, bc, cx);
        }
        /* step 3 */
        if (!Type.isObject(o)) {
            return false;
        }
        /* step 4-5 */
        Object p = Get(cx, (ScriptObject) c, "prototype");
        if (!Type.isObject(p)) {
            throw throwTypeError(cx, Messages.Key.NotObjectType);
        }
        /* step 7 */
        for (ScriptObject obj = Type.objectValue(o);;) {
            obj = obj.getInheritance(cx);
            if (obj == null) {
                return false;
            }
            if (SameValue(p, obj)) {
                return true;
            }
        }
    }

    /**
     * 9.3.14 GetPrototypeFromConstructor ( constructor, intrinsicDefaultProto )
     */
    public static ScriptObject GetPrototypeFromConstructor(ExecutionContext cx, Object constructor,
            Intrinsics intrinsicDefaultProto) {
        if (!IsConstructor(constructor)) {
            throw throwTypeError(cx, Messages.Key.NotConstructor);
        }
        Object proto = Get(cx, Type.objectValue(constructor), "prototype");
        if (!Type.isObject(proto)) {
            Realm realm;
            if (constructor instanceof FunctionObject) {
                realm = ((FunctionObject) constructor).getRealm();
            } else {
                realm = cx.getRealm();
            }
            proto = realm.getIntrinsic(intrinsicDefaultProto);
        }
        return Type.objectValue(proto);
    }

    /**
     * 9.3.15 OrdinaryCreateFromConstructor ( constructor, intrinsicDefaultProto )
     */
    public static OrdinaryObject OrdinaryCreateFromConstructor(ExecutionContext cx,
            Object constructor, Intrinsics intrinsicDefaultProto) {
        ScriptObject proto = GetPrototypeFromConstructor(cx, constructor, intrinsicDefaultProto);
        return ObjectCreate(cx, proto);
    }

    /**
     * 9.3.15 OrdinaryCreateFromConstructor ( constructor, intrinsicDefaultProto, internalDataList )
     */
    public static <OBJECT extends OrdinaryObject> OBJECT OrdinaryCreateFromConstructor(
            ExecutionContext cx, Object constructor, Intrinsics intrinsicDefaultProto,
            ObjectAllocator<OBJECT> allocator) {
        ScriptObject proto = GetPrototypeFromConstructor(cx, constructor, intrinsicDefaultProto);
        return ObjectCreate(cx, proto, allocator);
    }

    /**
     * Returns a list of all string-valued [[OwnPropertyKeys]] of {@code obj}
     */
    public static List<String> GetOwnPropertyNames(ExecutionContext cx, ScriptObject obj) {
        // FIXME: spec clean-up (Bug 1142)
        Iterator<?> keys = FromListIterator(cx, obj.ownPropertyKeys(cx));
        List<String> nameList = new ArrayList<>();
        while (keys.hasNext()) {
            Object key = ToPropertyKey(cx, keys.next());
            if (key instanceof String) {
                nameList.add((String) key);
            }
        }
        return nameList;
    }

    /**
     * Returns a list of all string-valued, enumerable [[OwnPropertyKeys]] of {@code obj}
     */
    public static List<String> GetOwnPropertyKeys(ExecutionContext cx, ScriptObject obj) {
        // FIXME: spec clean-up (Bug 1142)
        Iterator<?> keys = FromListIterator(cx, obj.ownPropertyKeys(cx));
        List<String> nameList = new ArrayList<>();
        while (keys.hasNext()) {
            Object key = ToPropertyKey(cx, keys.next());
            if (key instanceof String) {
                String skey = (String) key;
                Property desc = obj.getOwnProperty(cx, skey);
                if (desc != null && desc.isEnumerable()) {
                    nameList.add(skey);
                }
            }
        }
        return nameList;
    }

    private static final class NumberParser {
        private NumberParser() {
        }

        static double parse(String s) {
            s = Strings.trim(s);
            int len = s.length();
            if (len == 0) {
                return 0;
            }
            if (s.charAt(0) == '0' && len > 1) {
                char c = s.charAt(1);
                if (c == 'x' || c == 'X') {
                    return readHexIntegerLiteral(s, 2, len);
                }
            }
            return readDecimalLiteral(s, 0, len);
        }

        private static double readHexIntegerLiteral(String s, int start, int end) {
            for (int index = start; index < end; ++index) {
                char c = s.charAt(index);
                if (!((c >= '0' && c <= '9') || (c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f'))) {
                    return Double.NaN;
                }
            }
            return StringToNumber.stringToNumber(s, start, 16);
        }

        private static double readDecimalLiteral(String s, int start, int end) {
            final int Infinity_length = "Infinity".length();

            int index = start;
            int c = s.charAt(index++);
            boolean isPos = true;
            if (c == '+' || c == '-') {
                if (index >= end)
                    return Double.NaN;
                isPos = (c == '+');
                c = s.charAt(index++);
            }
            if (c == 'I') {
                // Infinity
                if (index - 1 + Infinity_length == end
                        && s.regionMatches(index - 1, "Infinity", 0, Infinity_length)) {
                    return isPos ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                }
                return Double.NaN;
            }
            if (c == '.') {
                if (index >= end)
                    return Double.NaN;
                char d = s.charAt(index);
                if (!(d >= '0' && d <= '9')) {
                    return Double.NaN;
                }
            } else {
                do {
                    if (!(c >= '0' && c <= '9')) {
                        return Double.NaN;
                    }
                    if (index >= end) {
                        break;
                    }
                    c = s.charAt(index++);
                } while (c != '.' && c != 'e' && c != 'E');
            }
            if (c == '.') {
                do {
                    if (index >= end) {
                        break;
                    }
                    c = s.charAt(index++);
                } while (c >= '0' && c <= '9');
            }
            if (c == 'e' || c == 'E') {
                if (index >= end)
                    return Double.NaN;
                c = s.charAt(index++);
                if (c == '+' || c == '-') {
                    if (index >= end)
                        return Double.NaN;
                    c = s.charAt(index++);
                }
                do {
                    if (!(c >= '0' && c <= '9')) {
                        return Double.NaN;
                    }
                    if (index >= end) {
                        break;
                    }
                    c = s.charAt(index++);
                } while (true);
            }
            if (index == end) {
                return Double.parseDouble(s.substring(start, end));
            }
            return Double.NaN;
        }
    }
}
