/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime;

import static com.github.anba.es6draft.runtime.internal.Errors.throwRangeError;
import static com.github.anba.es6draft.runtime.internal.Errors.throwTypeError;
import static com.github.anba.es6draft.runtime.internal.ScriptRuntime.InstanceofOperator;
import static com.github.anba.es6draft.runtime.objects.BooleanObject.BooleanCreate;
import static com.github.anba.es6draft.runtime.objects.NumberObject.NumberCreate;
import static com.github.anba.es6draft.runtime.objects.SymbolObject.SymbolCreate;
import static com.github.anba.es6draft.runtime.objects.internal.ListIterator.FromListIterator;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.ExoticArray.ArrayCreate;
import static com.github.anba.es6draft.runtime.types.builtins.ExoticString.StringCreate;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject.ObjectCreate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.mozilla.javascript.DToA;
import org.mozilla.javascript.StringToNumber;
import org.mozilla.javascript.v8dtoa.FastDtoa;

import com.github.anba.es6draft.runtime.internal.Errors;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.internal.Strings;
import com.github.anba.es6draft.runtime.objects.FunctionPrototype;
import com.github.anba.es6draft.runtime.objects.internal.ListIterator;
import com.github.anba.es6draft.runtime.types.*;
import com.github.anba.es6draft.runtime.types.builtins.ExoticBoundFunction;
import com.github.anba.es6draft.runtime.types.builtins.FunctionObject;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;
import com.google.doubleconversion.DoubleConversion;

/**
 * <h1>7 Abstract Operations</h1>
 * <ul>
 * <li>7.1 Type Conversion and Testing
 * <li>7.2 Testing and Comparison Operations
 * <li>7.3 Operations on Objects
 * <li>7.4 Operations on Iterator Objects
 * </ul>
 */
public final class AbstractOperations {
    private AbstractOperations() {
    }

    /**
     * 7.1.1 ToPrimitive
     */
    public static Object ToPrimitive(ExecutionContext cx, Object val) {
        // null == no hint
        return ToPrimitive(cx, val, null);
    }

    /**
     * 7.1.1 ToPrimitive
     */
    public static Object ToPrimitive(ExecutionContext cx, Object argument, Type preferredType) {
        if (!Type.isObject(argument)) {
            return argument;
        }
        return ToPrimitive(cx, Type.objectValue(argument), preferredType);
    }

    /**
     * 7.1.1 ToPrimitive
     * <p>
     * ToPrimitive for the Object type
     */
    private static Object ToPrimitive(ExecutionContext cx, ScriptObject argument, Type preferredType) {
        // TODO: change Type to enum ToPrimitiveHint { Default, String, Number }
        /* steps 1-3 (moved) */
        /* steps 4-5 */
        Object exoticToPrim = Get(cx, argument, BuiltinSymbol.toPrimitive.get());
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
        /* step 7 */
        if (preferredType == null) {
            preferredType = Type.Number;
        }
        /* step 8 */
        return OrdinaryToPrimitive(cx, argument, preferredType);
    }

    /**
     * 7.1.1 ToPrimitive
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
        /* step 5 (first try) */
        Object first = Get(cx, object, tryFirst);
        if (IsCallable(first)) {
            Object result = ((Callable) first).call(cx, object);
            if (!Type.isObject(result)) {
                return result;
            }
        }
        /* step 5 (second try) */
        Object second = Get(cx, object, trySecond);
        if (IsCallable(second)) {
            Object result = ((Callable) second).call(cx, object);
            if (!Type.isObject(result)) {
                return result;
            }
        }
        /* step 6 */
        throw throwTypeError(cx, Messages.Key.NoPrimitiveRepresentation);
    }

    /**
     * 7.1.2 ToBoolean
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
     * 7.1.2 ToBoolean
     */
    public static boolean ToBoolean(double d) {
        return !(d == 0 || Double.isNaN(d));
    }

    /**
     * 7.1.3 ToNumber
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
     * 7.1.3.1 ToNumber Applied to the String Type
     */
    public static double ToNumber(CharSequence s) {
        return NumberParser.parse(s.toString());
    }

    /**
     * 7.1.4 ToInteger
     */
    public static double ToInteger(ExecutionContext cx, Object val) {
        /* steps 1-2 */
        double number = ToNumber(cx, val);
        /* step 3 */
        if (Double.isNaN(number))
            return +0.0;
        /* step 4 */
        if (number == 0.0 || Double.isInfinite(number))
            return number;
        /* step 5 */
        return Math.signum(number) * Math.floor(Math.abs(number));
    }

    /**
     * 7.1.4 ToInteger
     */
    public static double ToInteger(double number) {
        /* steps 1-2 (not applicable) */
        /* step 3 */
        if (Double.isNaN(number))
            return +0.0;
        /* step 4 */
        if (number == 0.0 || Double.isInfinite(number))
            return number;
        /* step 5 */
        return Math.signum(number) * Math.floor(Math.abs(number));
    }

    /**
     * 7.1.5 ToInt32: (Signed 32 Bit Integer)
     */
    public static int ToInt32(ExecutionContext cx, Object val) {
        /* steps 1-2 */
        double number = ToNumber(cx, val);
        /* steps 3-6 */
        return DoubleConversion.doubleToInt32(number);
    }

    /**
     * 7.1.5 ToInt32: (Signed 32 Bit Integer)
     */
    public static int ToInt32(double number) {
        /* steps 1-2 (not applicable) */
        /* steps 3-6 */
        return DoubleConversion.doubleToInt32(number);
    }

    /**
     * 7.1.6 ToUint32: (Unsigned 32 Bit Integer)
     */
    public static long ToUint32(ExecutionContext cx, Object val) {
        /* steps 1-2 */
        double number = ToNumber(cx, val);
        /* steps 3-6 */
        return DoubleConversion.doubleToInt32(number) & 0xffffffffL;
    }

    /**
     * 7.1.6 ToUint32: (Unsigned 32 Bit Integer)
     */
    public static long ToUint32(double number) {
        /* steps 1-2 (not applicable) */
        /* steps 3-6 */
        return DoubleConversion.doubleToInt32(number) & 0xffffffffL;
    }

    /**
     * 7.1.7 ToUint16: (Unsigned 16 Bit Integer)
     */
    public static char ToUint16(ExecutionContext cx, Object val) {
        /* steps 1-2 */
        double number = ToNumber(cx, val);
        /* steps 3-6 */
        return (char) DoubleConversion.doubleToInt32(number);
    }

    /**
     * 7.1.7 ToUint16: (Unsigned 16 Bit Integer)
     */
    public static char ToUint16(double number) {
        /* steps 1-2 (not applicable) */
        /* steps 3-6 */
        return (char) DoubleConversion.doubleToInt32(number);
    }

    /**
     * 7.1.8 ToInt8: (Signed 8 Bit Integer)
     */
    public static byte ToInt8(ExecutionContext cx, Object val) {
        /* steps 1-2 */
        double number = ToNumber(cx, val);
        /* steps 3-6 */
        return (byte) DoubleConversion.doubleToInt32(number);
    }

    /**
     * 7.1.8 ToInt8: (Signed 8 Bit Integer)
     */
    public static byte ToInt8(double number) {
        /* steps 1-2 (not applicable) */
        /* steps 3-6 */
        return (byte) DoubleConversion.doubleToInt32(number);
    }

    /**
     * 7.1.9 ToUint8: (Unsigned 8 Bit Integer)
     */
    public static int ToUint8(ExecutionContext cx, Object val) {
        /* steps 1-2 */
        double number = ToNumber(cx, val);
        /* steps 3-6 */
        return DoubleConversion.doubleToInt32(number) & 0xFF;
    }

    /**
     * 7.1.9 ToUint8: (Unsigned 8 Bit Integer)
     */
    public static int ToUint8(double number) {
        /* steps 1-2 (not applicable) */
        /* steps 3-6 */
        return DoubleConversion.doubleToInt32(number) & 0xFF;
    }

    /**
     * 7.1.10 ToUint8Clamp: (Unsigned 8 Bit Integer, Clamped)
     */
    public static int ToUint8Clamp(ExecutionContext cx, Object val) {
        /* steps 1-2 */
        double number = ToNumber(cx, val);
        /* steps 3-8 */
        return number <= 0 ? +0 : number > 255 ? 255 : (int) Math.rint(number);
    }

    /**
     * 7.1.10 ToUint8Clamp: (Unsigned 8 Bit Integer, Clamped)
     */
    public static int ToUint8Clamp(double number) {
        /* steps 1-2 (not applicable) */
        /* steps 3-8 */
        return number <= 0 ? +0 : number > 255 ? 255 : (int) Math.rint(number);
    }

    /**
     * 7.1.11 ToString
     */
    public static String ToFlatString(ExecutionContext cx, Object val) {
        return ToString(cx, val).toString();
    }

    /**
     * 7.1.11 ToString
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
            throw Errors.throwTypeError(cx, Messages.Key.SymbolString);
        case Object:
        default:
            Object primValue = ToPrimitive(cx, val, Type.String);
            return ToString(cx, primValue);
        }
    }

    /**
     * 7.1.11.1 ToString Applied to the Number Type
     */
    public static String ToString(int val) {
        return Integer.toString(val);
    }

    /**
     * 7.1.11.1 ToString Applied to the Number Type
     */
    public static String ToString(long val) {
        if ((int) val == val) {
            return Integer.toString((int) val);
        } else if (-0x1F_FFFF_FFFF_FFFFL <= val && val <= 0x1F_FFFF_FFFF_FFFFL) {
            return Long.toString(val);
        }
        return ToString((double) val);
    }

    /**
     * 7.1.11.1 ToString Applied to the Number Type
     */
    public static String ToString(double val) {
        /* steps 1-4 (+ shortcut for integer values) */
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
     * 7.1.12 ToObject
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
            return SymbolCreate(cx, Type.symbolValue(val));
        case Object:
        default:
            return Type.objectValue(val);
        }
    }

    /**
     * 7.1.13 ToPropertyKey
     */
    public static Object ToPropertyKey(ExecutionContext cx, Object val) {
        if (val instanceof Symbol) {
            return val;
        }
        return ToFlatString(cx, val);
    }

    /**
     * 7.1.14 ToLength
     */
    public static long ToLength(ExecutionContext cx, Object val) {
        /* steps 1-2 */
        double len = ToInteger(cx, val);
        /* step 3 */
        if (len <= 0) {
            return 0;
        }
        /* step 4 */
        return (long) Math.min(len, 0x1F_FFFF_FFFF_FFFFL);
    }

    /**
     * 7.1.14 ToLength
     */
    public static long ToLength(double len) {
        /* steps 1-2 (not applicable) */
        /* step 3 */
        if (len <= 0) {
            return 0;
        }
        /* step 4 */
        return (long) Math.min(len, 0x1F_FFFF_FFFF_FFFFL);
    }

    /**
     * 7.2.1 CheckObjectCoercible
     */
    public static Object CheckObjectCoercible(ExecutionContext cx, Object val) {
        if (Type.isUndefinedOrNull(val)) {
            throw throwTypeError(cx, Messages.Key.UndefinedOrNull);
        }
        return val;
    }

    /**
     * 7.2.2 IsCallable
     */
    public static boolean IsCallable(Object val) {
        return val instanceof Callable;
    }

    /**
     * 7.2.3 SameValue(x, y)
     */
    public static boolean SameValue(Object x, Object y) {
        /* same reference shortcuts */
        if (x == y) {
            return true;
        } else if (x == null || y == null) {
            return false;
        }
        Type tx = Type.of(x);
        Type ty = Type.of(y);
        /* steps 1-2 (not applicable) */
        /* step 3 */
        if (tx != ty) {
            return false;
        }
        /* step 4 */
        if (tx == Type.Undefined) {
            return true;
        }
        /* step 5 */
        if (tx == Type.Null) {
            return true;
        }
        /* step 6 */
        if (tx == Type.Number) {
            double dx = Type.numberValue(x);
            double dy = Type.numberValue(y);
            return Double.compare(dx, dy) == 0;
        }
        /* step 7 */
        if (tx == Type.String) {
            return Type.stringValue(x).toString().contentEquals(Type.stringValue(y));
        }
        /* step 8 */
        if (tx == Type.Boolean) {
            return Type.booleanValue(x) == Type.booleanValue(y);
        }
        /* steps 9-10 */
        assert tx == Type.Object || tx == Type.Symbol;
        return (x == y);
    }

    /**
     * 7.2.3 SameValue(x, y)
     */
    public static boolean SameValue(double x, double y) {
        /* steps 1-5, 7-10 (not applicable) */
        /* step 6 */
        return Double.compare(x, y) == 0;
    }

    /**
     * 7.2.3 SameValue(x, y)
     */
    public static boolean SameValue(ScriptObject x, ScriptObject y) {
        /* steps 1-9 (not applicable) */
        /* step 10 */
        return (x == y);
    }

    /**
     * 7.2.4 SameValueZero(x, y)
     */
    public static boolean SameValueZero(Object x, Object y) {
        /* same reference shortcuts */
        if (x == y) {
            return true;
        } else if (x == null || y == null) {
            return false;
        }
        Type tx = Type.of(x);
        Type ty = Type.of(y);
        /* steps 1-2 (not applicable) */
        /* step 3 */
        if (tx != ty) {
            return false;
        }
        /* step 4 */
        if (tx == Type.Undefined) {
            return true;
        }
        /* step 5 */
        if (tx == Type.Null) {
            return true;
        }
        /* step 6 */
        if (tx == Type.Number) {
            double dx = Type.numberValue(x);
            double dy = Type.numberValue(y);
            if (dx == 0) {
                return (dy == 0);
            }
            return Double.compare(dx, dy) == 0;
        }
        /* step 7 */
        if (tx == Type.String) {
            return Type.stringValue(x).toString().contentEquals(Type.stringValue(y));
        }
        /* step 8 */
        if (tx == Type.Boolean) {
            return Type.booleanValue(x) == Type.booleanValue(y);
        }
        /* steps 9-10 */
        assert tx == Type.Object || tx == Type.Symbol;
        return (x == y);
    }

    /**
     * 7.2.5 IsConstructor
     */
    public static boolean IsConstructor(Object val) {
        /* steps 1-4 */
        return val instanceof Constructor && ((Constructor) val).isConstructor();
    }

    /**
     * 7.2.6 IsPropertyKey
     */
    public static boolean IsPropertyKey(Object val) {
        /* steps 1-3 */
        if (Type.isString(val) || val instanceof Symbol) {
            return true;
        }
        /* step 4 */
        return false;
    }

    /**
     * 7.2.7 IsExtensible (O)
     */
    public static boolean IsExtensible(ExecutionContext cx, ScriptObject object) {
        /* steps 1-2 */
        return object.isExtensible(cx);
    }

    /**
     * 7.2.8 Abstract Relational Comparison
     */
    public static int RelationalComparison(ExecutionContext cx, Object x, Object y,
            boolean leftFirst) {
        // true -> 1
        // false -> 0
        // undefined -> -1
        /* steps 1-2 (not applicable) */
        /* steps 3-4 */
        Object px, py;
        if (leftFirst) {
            px = ToPrimitive(cx, x, Type.Number);
            py = ToPrimitive(cx, y, Type.Number);
        } else {
            py = ToPrimitive(cx, y, Type.Number);
            px = ToPrimitive(cx, x, Type.Number);
        }
        /* steps 5-6 */
        if (!(Type.isString(px) && Type.isString(py))) {
            double nx = ToNumber(cx, px);
            double ny = ToNumber(cx, py);
            if (Double.isNaN(nx) || Double.isNaN(ny)) {
                return -1;
            }
            if (nx == ny) {
                return 0;
            }
            if (nx == Double.POSITIVE_INFINITY) {
                return 0;
            }
            if (ny == Double.POSITIVE_INFINITY) {
                return 1;
            }
            if (ny == Double.NEGATIVE_INFINITY) {
                return 0;
            }
            if (nx == Double.NEGATIVE_INFINITY) {
                return 1;
            }
            return (nx < ny ? 1 : 0);
        } else {
            int c = Type.stringValue(px).toString().compareTo(Type.stringValue(py).toString());
            return c < 0 ? 1 : 0;
        }
    }

    /**
     * 7.2.9 Abstract Equality Comparison
     */
    public static boolean EqualityComparison(ExecutionContext cx, Object x, Object y) {
        // fast path for same reference
        if (x == y) {
            if (x instanceof Double) {
                return !((Double) x).isNaN();
            }
            return true;
        }
        Type tx = Type.of(x);
        Type ty = Type.of(y);
        /* step 1 */
        if (tx == ty) {
            return StrictEqualityComparison(x, y);
        }
        /* step 2 */
        if (tx == Type.Null && ty == Type.Undefined) {
            return true;
        }
        /* step 3 */
        if (tx == Type.Undefined && ty == Type.Null) {
            return true;
        }
        /* step 4 */
        if (tx == Type.Number && ty == Type.String) {
            // return EqualityComparison(cx, x, ToNumber(cx, y));
            return Type.numberValue(x) == ToNumber(cx, y);
        }
        /* step 5 */
        if (tx == Type.String && ty == Type.Number) {
            // return EqualityComparison(cx, ToNumber(cx, x), y);
            return ToNumber(cx, x) == Type.numberValue(y);
        }
        /* step 6 */
        if (tx == Type.Boolean) {
            return EqualityComparison(cx, ToNumber(cx, x), y);
        }
        /* step 7 */
        if (ty == Type.Boolean) {
            return EqualityComparison(cx, x, ToNumber(cx, y));
        }
        /* step 8 */
        if ((tx == Type.String || tx == Type.Number) && ty == Type.Object) {
            return EqualityComparison(cx, x, ToPrimitive(cx, y, null));
        }
        /* step 9 */
        if (tx == Type.Object && (ty == Type.String || ty == Type.Number)) {
            return EqualityComparison(cx, ToPrimitive(cx, x, null), y);
        }
        /* step 10 */
        return false;
    }

    /**
     * 7.2.10 Strict Equality Comparison
     */
    public static boolean StrictEqualityComparison(Object x, Object y) {
        // fast path for same reference
        if (x == y) {
            if (x instanceof Double) {
                return !((Double) x).isNaN();
            }
            return true;
        }
        Type tx = Type.of(x);
        Type ty = Type.of(y);
        /* step 1 */
        if (tx != ty) {
            return false;
        }
        /* step 2 */
        if (tx == Type.Undefined) {
            return true;
        }
        /* step 3 */
        if (tx == Type.Null) {
            return true;
        }
        /* step 4 */
        if (tx == Type.Number) {
            return Type.numberValue(x) == Type.numberValue(y);
        }
        /* step 5 */
        if (tx == Type.String) {
            return Type.stringValue(x).toString().equals(Type.stringValue(y).toString());
        }
        /* step 6 */
        if (tx == Type.Boolean) {
            return Type.booleanValue(x) == Type.booleanValue(y);
        }
        assert tx == Type.Object || tx == Type.Symbol;
        /* steps 7-9 */
        return (x == y);
    }

    /**
     * 7.3.1 Get (O, P)
     */
    public static Object Get(ExecutionContext cx, ScriptObject object, Object propertyKey) {
        /* steps 1-3 */
        if (propertyKey instanceof String) {
            return Get(cx, object, (String) propertyKey);
        } else {
            return Get(cx, object, (Symbol) propertyKey);
        }
    }

    /**
     * 7.3.1 Get (O, P)
     */
    public static Object Get(ExecutionContext cx, ScriptObject object, String propertyKey) {
        /* steps 1-3 */
        return object.get(cx, propertyKey, object);
    }

    /**
     * 7.3.1 Get (O, P)
     */
    public static Object Get(ExecutionContext cx, ScriptObject object, Symbol propertyKey) {
        /* steps 1-3 */
        return object.get(cx, propertyKey, object);
    }

    /**
     * 7.3.2 Put (O, P, V, Throw)
     */
    public static void Put(ExecutionContext cx, ScriptObject object, Object propertyKey,
            Object value, boolean _throw) {
        /* steps 1-7 */
        if (propertyKey instanceof String) {
            Put(cx, object, (String) propertyKey, value, _throw);
        } else {
            Put(cx, object, (Symbol) propertyKey, value, _throw);
        }
    }

    /**
     * 7.3.2 Put (O, P, V, Throw)
     */
    public static void Put(ExecutionContext cx, ScriptObject object, String propertyKey,
            Object value, boolean _throw) {
        /* steps 1-5 */
        boolean success = object.set(cx, propertyKey, value, object);
        /* step 6 */
        if (!success && _throw) {
            throw throwTypeError(cx, Messages.Key.PropertyNotModifiable, propertyKey);
        }
        /* step 7 (not applicable) */
    }

    /**
     * 7.3.2 Put (O, P, V, Throw)
     */
    public static void Put(ExecutionContext cx, ScriptObject object, Symbol propertyKey,
            Object value, boolean _throw) {
        /* steps 1-5 */
        boolean success = object.set(cx, propertyKey, value, object);
        /* step 6 */
        if (!success && _throw) {
            throw throwTypeError(cx, Messages.Key.PropertyNotModifiable, propertyKey.toString());
        }
        /* step 7 (not applicable) */
    }

    /**
     * 7.3.3 CreateDataProperty (O, P, V)
     */
    public static boolean CreateDataProperty(ExecutionContext cx, ScriptObject object,
            Object propertyKey, Object value) {
        if (propertyKey instanceof String) {
            return CreateDataProperty(cx, object, (String) propertyKey, value);
        } else {
            return CreateDataProperty(cx, object, (Symbol) propertyKey, value);
        }
    }

    /**
     * 7.3.3 CreateDataProperty (O, P, V)
     */
    public static boolean CreateDataProperty(ExecutionContext cx, ScriptObject object,
            String propertyKey, Object value) {
        /* steps 1-2 (not applicable) */
        /* step 3 */
        PropertyDescriptor newDesc = new PropertyDescriptor(value, true, true, true);
        /* step 4 */
        return object.defineOwnProperty(cx, propertyKey, newDesc);
    }

    /**
     * 7.3.3 CreateDataProperty (O, P, V)
     */
    public static boolean CreateDataProperty(ExecutionContext cx, ScriptObject object,
            Symbol propertyKey, Object value) {
        /* steps 1-2 (not applicable) */
        /* step 3 */
        PropertyDescriptor newDesc = new PropertyDescriptor(value, true, true, true);
        /* step 4 */
        return object.defineOwnProperty(cx, propertyKey, newDesc);
    }

    /**
     * 7.3.4 CreateDataPropertyOrThrow (O, P, V)
     */
    public static void CreateDataPropertyOrThrow(ExecutionContext cx, ScriptObject object,
            Object propertyKey, Object value) {
        if (propertyKey instanceof String) {
            CreateDataPropertyOrThrow(cx, object, (String) propertyKey, value);
        } else {
            CreateDataPropertyOrThrow(cx, object, (Symbol) propertyKey, value);
        }
    }

    /**
     * 7.3.4 CreateDataPropertyOrThrow (O, P, V)
     */
    public static void CreateDataPropertyOrThrow(ExecutionContext cx, ScriptObject object,
            String propertyKey, Object value) {
        /* steps 1-2 (not applicable) */
        /* steps 3-4 */
        boolean success = CreateDataProperty(cx, object, propertyKey, value);
        /* step 5 */
        if (!success) {
            throw throwTypeError(cx, Messages.Key.PropertyNotCreatable, propertyKey);
        }
        /* step 6 */
    }

    /**
     * 7.3.4 CreateDataPropertyOrThrow (O, P, V)
     */
    public static void CreateDataPropertyOrThrow(ExecutionContext cx, ScriptObject object,
            Symbol propertyKey, Object value) {
        /* steps 1-2 (not applicable) */
        /* steps 3-4 */
        boolean success = CreateDataProperty(cx, object, propertyKey, value);
        /* step 5 */
        if (!success) {
            throw throwTypeError(cx, Messages.Key.PropertyNotCreatable, propertyKey.toString());
        }
        /* step 6 */
    }

    /**
     * 7.3.5 DefinePropertyOrThrow (O, P, desc)
     */
    public static void DefinePropertyOrThrow(ExecutionContext cx, ScriptObject object,
            Object propertyKey, PropertyDescriptor desc) {
        if (propertyKey instanceof String) {
            DefinePropertyOrThrow(cx, object, (String) propertyKey, desc);
        } else {
            DefinePropertyOrThrow(cx, object, (Symbol) propertyKey, desc);
        }
    }

    /**
     * 7.3.5 DefinePropertyOrThrow (O, P, desc)
     */
    public static void DefinePropertyOrThrow(ExecutionContext cx, ScriptObject object,
            String propertyKey, PropertyDescriptor desc) {
        /* steps 1-4 */
        boolean success = object.defineOwnProperty(cx, propertyKey, desc);
        /* step 5 */
        if (!success) {
            throw throwTypeError(cx, Messages.Key.PropertyNotCreatable, propertyKey);
        }
        /* step 6 (not applicable) */
    }

    /**
     * 7.3.5 DefinePropertyOrThrow (O, P, desc)
     */
    public static void DefinePropertyOrThrow(ExecutionContext cx, ScriptObject object,
            Symbol propertyKey, PropertyDescriptor desc) {
        /* steps 1-4 */
        boolean success = object.defineOwnProperty(cx, propertyKey, desc);
        /* step 5 */
        if (!success) {
            throw throwTypeError(cx, Messages.Key.PropertyNotCreatable, propertyKey.toString());
        }
        /* step 6 (not applicable) */
    }

    /**
     * 7.3.6 DeletePropertyOrThrow (O, P)
     */
    public static void DeletePropertyOrThrow(ExecutionContext cx, ScriptObject object,
            Object propertyKey) {
        if (propertyKey instanceof String) {
            DeletePropertyOrThrow(cx, object, (String) propertyKey);
        } else {
            DeletePropertyOrThrow(cx, object, (Symbol) propertyKey);
        }
    }

    /**
     * 7.3.6 DeletePropertyOrThrow (O, P)
     */
    public static void DeletePropertyOrThrow(ExecutionContext cx, ScriptObject object,
            String propertyKey) {
        /* steps 1-4 */
        boolean success = object.delete(cx, propertyKey);
        /* step 5 */
        if (!success) {
            throw throwTypeError(cx, Messages.Key.PropertyNotDeletable, propertyKey);
        }
        /* step 6 (not applicable) */
    }

    /**
     * 7.3.6 DeletePropertyOrThrow (O, P)
     */
    public static void DeletePropertyOrThrow(ExecutionContext cx, ScriptObject object,
            Symbol propertyKey) {
        /* steps 1-4 */
        boolean success = object.delete(cx, propertyKey);
        /* step 5 */
        if (!success) {
            throw throwTypeError(cx, Messages.Key.PropertyNotDeletable, propertyKey.toString());
        }
        /* step 6 (not applicable) */
    }

    /**
     * 7.3.7 HasProperty (O, P)
     */
    public static boolean HasProperty(ExecutionContext cx, ScriptObject object, Object propertyKey) {
        if (propertyKey instanceof String) {
            return HasProperty(cx, object, (String) propertyKey);
        } else {
            return HasProperty(cx, object, (Symbol) propertyKey);
        }
    }

    /**
     * 7.3.7 HasProperty (O, P)
     */
    public static boolean HasProperty(ExecutionContext cx, ScriptObject object, String propertyKey) {
        /* steps 1-3 */
        return object.hasProperty(cx, propertyKey);
    }

    /**
     * 7.3.7 HasProperty (O, P)
     */
    public static boolean HasProperty(ExecutionContext cx, ScriptObject object, Symbol propertyKey) {
        /* steps 1-3 */
        return object.hasProperty(cx, propertyKey);
    }

    /**
     * 7.3.8 HasOwnProperty (O, P)
     */
    public static boolean HasOwnProperty(ExecutionContext cx, ScriptObject object,
            Object propertyKey) {
        if (propertyKey instanceof String) {
            return HasOwnProperty(cx, object, (String) propertyKey);
        } else {
            return HasOwnProperty(cx, object, (Symbol) propertyKey);
        }
    }

    /**
     * 7.3.8 HasOwnProperty (O, P)
     */
    public static boolean HasOwnProperty(ExecutionContext cx, ScriptObject object,
            String propertyKey) {
        /* steps 1-2 (not applicable) */
        /* steps 3-4 */
        Property desc = object.getOwnProperty(cx, propertyKey);
        /* steps 5-6 */
        return desc != null;
    }

    /**
     * 7.3.8 HasOwnProperty (O, P)
     */
    public static boolean HasOwnProperty(ExecutionContext cx, ScriptObject object,
            Symbol propertyKey) {
        /* steps 1-2 (not applicable) */
        /* steps 3-4 */
        Property desc = object.getOwnProperty(cx, propertyKey);
        /* steps 5-6 */
        return desc != null;
    }

    /**
     * 7.3.9 GetMethod (O, P)
     */
    public static Callable GetMethod(ExecutionContext cx, ScriptObject object, Object propertyKey) {
        if (propertyKey instanceof String) {
            return GetMethod(cx, object, (String) propertyKey);
        } else {
            return GetMethod(cx, object, (Symbol) propertyKey);
        }
    }

    /**
     * 7.3.9 GetMethod (O, P)
     */
    public static Callable GetMethod(ExecutionContext cx, ScriptObject object, String propertyKey) {
        /* steps 1-4 */
        Object func = object.get(cx, propertyKey, object);
        /* step 5 */
        if (Type.isUndefined(func)) {
            return null;
        }
        /* step 6 */
        if (!IsCallable(func)) {
            throw throwTypeError(cx, Messages.Key.NotCallable);
        }
        /* step 7 */
        return (Callable) func;
    }

    /**
     * 7.3.9 GetMethod (O, P)
     */
    public static Callable GetMethod(ExecutionContext cx, ScriptObject object, Symbol propertyKey) {
        /* steps 1-4 */
        Object func = object.get(cx, propertyKey, object);
        /* step 5 */
        if (Type.isUndefined(func)) {
            return null;
        }
        /* step 6 */
        if (!IsCallable(func)) {
            throw throwTypeError(cx, Messages.Key.NotCallable);
        }
        /* step 7 */
        return (Callable) func;
    }

    /**
     * 7.3.10 Invoke(O,P [,args])
     */
    public static Object Invoke(ExecutionContext cx, Object object, Object propertyKey,
            Object... args) {
        if (propertyKey instanceof String) {
            return Invoke(cx, object, (String) propertyKey, args);
        } else {
            return Invoke(cx, object, (Symbol) propertyKey, args);
        }
    }

    /**
     * 7.3.10 Invoke(O,P [,args])
     */
    public static Object Invoke(ExecutionContext cx, Object object, String propertyKey,
            Object... args) {
        /* steps 1-5 */
        ScriptObject base;
        if (Type.isObject(object)) {
            base = Type.objectValue(object);
        } else {
            base = ToObject(cx, object);
        }
        /* step 6 */
        return base.invoke(cx, propertyKey, args, object);
    }

    /**
     * 7.3.10 Invoke(O,P [,args])
     */
    public static Object Invoke(ExecutionContext cx, ScriptObject object, String propertyKey,
            Object... args) {
        /* steps 1-5 (not applicable) */
        /* step 6 */
        return object.invoke(cx, propertyKey, args, object);
    }

    /**
     * 7.3.10 Invoke(O,P [,args])
     */
    public static Object Invoke(ExecutionContext cx, Object object, Symbol propertyKey,
            Object... args) {
        /* steps 1-5 */
        ScriptObject base;
        if (Type.isObject(object)) {
            base = Type.objectValue(object);
        } else {
            base = ToObject(cx, object);
        }
        /* step 6 */
        return base.invoke(cx, propertyKey, args, object);
    }

    /**
     * 7.3.10 Invoke(O,P [,args])
     */
    public static Object Invoke(ExecutionContext cx, ScriptObject object, Symbol propertyKey,
            Object... args) {
        /* steps 1-5 (not applicable) */
        /* step 6 */
        return object.invoke(cx, propertyKey, args, object);
    }

    /**
     * 7.3.11 SetIntegrityLevel (O, level)
     */
    public static boolean SetIntegrityLevel(ExecutionContext cx, ScriptObject object,
            IntegrityLevel level) {
        /* steps 1-2 */
        assert level == IntegrityLevel.Sealed || level == IntegrityLevel.Frozen;
        /* steps 3-4 */
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
     * 7.3.12 TestIntegrityLevel (O, level)
     */
    public static boolean TestIntegrityLevel(ExecutionContext cx, ScriptObject object,
            IntegrityLevel level) {
        /* steps 1-2 */
        assert level == IntegrityLevel.Sealed || level == IntegrityLevel.Frozen;
        /* steps 3-4 */
        boolean status = IsExtensible(cx, object);
        /* steps 5-6 */
        if (status) {
            return false;
        }
        /* steps 7-8 */
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
     * 7.3.13 CreateArrayFromList (elements)
     */
    public static ScriptObject CreateArrayFromList(ExecutionContext cx, List<?> elements) {
        /* step 1 (not applicable) */
        /* step 2 */
        ScriptObject array = ArrayCreate(cx, 0);
        /* step 3 */
        int n = 0;
        /* step 4 */
        for (Object e : elements) {
            boolean status = CreateDataProperty(cx, array, ToString(n), e);
            assert status;
            n += 1;
        }
        /* step 5 */
        return array;
    }

    /**
     * 7.3.14 CreateListFromArrayLike (obj)
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
        long n = ToLength(cx, len);
        // CreateListFromArrayLike() is (currently) only used for argument arrays
        if (n > FunctionPrototype.getMaxArguments()) {
            throw throwRangeError(cx, Messages.Key.FunctionTooManyArguments);
        }
        int length = (int) n;
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
     * 7.3.15 OrdinaryHasInstance (C, O)
     */
    public static boolean OrdinaryHasInstance(ExecutionContext cx, Object c, Object o) {
        /* step 1 */
        if (!IsCallable(c)) {
            return false;
        }
        /* step 2 */
        if (c instanceof ExoticBoundFunction) {
            Callable bc = ((ExoticBoundFunction) c).getBoundTargetFunction();
            return InstanceofOperator(o, bc, cx);
        }
        /* step 3 */
        if (!Type.isObject(o)) {
            return false;
        }
        /* steps 4-5 */
        Object p = Get(cx, (ScriptObject) c, "prototype");
        if (!Type.isObject(p)) {
            throw throwTypeError(cx, Messages.Key.NotObjectType);
        }
        /* step 7 */
        for (ScriptObject obj = Type.objectValue(o), proto = Type.objectValue(p);;) {
            obj = obj.getPrototypeOf(cx);
            if (obj == null) {
                return false;
            }
            if (SameValue(proto, obj)) {
                return true;
            }
        }
    }

    /**
     * 7.3.16 GetPrototypeFromConstructor ( constructor, intrinsicDefaultProto )
     */
    public static ScriptObject GetPrototypeFromConstructor(ExecutionContext cx, Object constructor,
            Intrinsics intrinsicDefaultProto) {
        /* step 1 (not applicable) */
        /* step 2 */
        if (!IsConstructor(constructor)) {
            throw throwTypeError(cx, Messages.Key.NotConstructor);
        }
        /* steps 3-4 */
        Object proto = Get(cx, Type.objectValue(constructor), "prototype");
        /* step 5 */
        if (!Type.isObject(proto)) {
            Realm realm;
            if (constructor instanceof FunctionObject) {
                realm = ((FunctionObject) constructor).getRealm();
            } else {
                realm = cx.getRealm();
            }
            proto = realm.getIntrinsic(intrinsicDefaultProto);
        }
        /* step 6 */
        return Type.objectValue(proto);
    }

    /**
     * 7.3.17 OrdinaryCreateFromConstructor ( constructor, intrinsicDefaultProto )
     */
    public static OrdinaryObject OrdinaryCreateFromConstructor(ExecutionContext cx,
            Object constructor, Intrinsics intrinsicDefaultProto) {
        /* step 1 (not applicable) */
        /* steps 2-3 */
        ScriptObject proto = GetPrototypeFromConstructor(cx, constructor, intrinsicDefaultProto);
        /* step 4 */
        return ObjectCreate(cx, proto);
    }

    /**
     * 7.3.17 OrdinaryCreateFromConstructor ( constructor, intrinsicDefaultProto, internalDataList )
     */
    public static <OBJECT extends OrdinaryObject> OBJECT OrdinaryCreateFromConstructor(
            ExecutionContext cx, Object constructor, Intrinsics intrinsicDefaultProto,
            ObjectAllocator<OBJECT> allocator) {
        /* step 1 (not applicable) */
        /* steps 2-3 */
        ScriptObject proto = GetPrototypeFromConstructor(cx, constructor, intrinsicDefaultProto);
        /* step 4 */
        return ObjectCreate(cx, proto, allocator);
    }

    /**
     * 7.4.1 GetIterator ( obj )
     */
    public static ScriptObject GetIterator(ExecutionContext cx, Object obj) {
        /* steps 1-2 */
        Object iterator = Invoke(cx, obj, BuiltinSymbol.iterator.get());
        /* step 3 */
        if (!Type.isObject(iterator)) {
            throw throwTypeError(cx, Messages.Key.NotObjectType);
        }
        /* step 4 */
        return Type.objectValue(iterator);
    }

    /**
     * 7.4.2 IteratorNext ( iterator, value )
     */
    public static ScriptObject IteratorNext(ExecutionContext cx, ScriptObject iterator) {
        return IteratorNext(cx, iterator, UNDEFINED);
    }

    /**
     * 7.4.2 IteratorNext ( iterator, value )
     */
    public static ScriptObject IteratorNext(ExecutionContext cx, ScriptObject iterator, Object value) {
        /* step 1 (not applicable) */
        /* steps 2-3 */
        Object result = Invoke(cx, iterator, "next", value);
        /* step 4 */
        if (!Type.isObject(result)) {
            throw throwTypeError(cx, Messages.Key.NotObjectType);
        }
        /* step 5 */
        return Type.objectValue(result);
    }

    /**
     * FIXME: Not in spec<br>
     * 7.4.? IteratorThrow ( iterator, value )
     */
    public static ScriptObject IteratorThrow(ExecutionContext cx, ScriptObject iterator,
            Object value) {
        Object result = Invoke(cx, iterator, "throw", value);
        if (!Type.isObject(result)) {
            throw throwTypeError(cx, Messages.Key.NotObjectType);
        }
        return Type.objectValue(result);
    }

    /**
     * 7.4.3 IteratorComplete (iterResult)
     */
    public static boolean IteratorComplete(ExecutionContext cx, ScriptObject iterResult) {
        /* step 1 (not applicable) */
        /* step 2 */
        Object done = Get(cx, iterResult, "done");
        /* step 3 */
        return ToBoolean(done);
    }

    /**
     * 7.4.4 IteratorValue (iterResult)
     */
    public static Object IteratorValue(ExecutionContext cx, ScriptObject iterResult) {
        /* step 1 (not applicable) */
        /* step 2 */
        return Get(cx, iterResult, "value");
    }

    /**
     * 7.4.5 IteratorStep ( iterator, value )
     */
    public static ScriptObject IteratorStep(ExecutionContext cx, ScriptObject iterator) {
        return IteratorStep(cx, iterator, UNDEFINED);
    }

    /**
     * 7.4.5 IteratorStep ( iterator, value )
     */
    public static ScriptObject IteratorStep(ExecutionContext cx, ScriptObject iterator, Object value) {
        /* step 1 (not applicable) */
        /* steps 2-3 */
        ScriptObject result = IteratorNext(cx, iterator, value);
        /* steps 4-5 */
        boolean done = IteratorComplete(cx, result);
        /* step 6 */
        if (done) {
            return null;
        }
        /* step 7 */
        return result;
    }

    /**
     * 7.4.6 CreateIterResultObject (value, done)
     */
    public static ScriptObject CreateIterResultObject(ExecutionContext cx, Object value,
            boolean done) {
        /* step 1 (not applicable) */
        /* step 2 */
        OrdinaryObject obj = ObjectCreate(cx, Intrinsics.ObjectPrototype);
        /* step 3 */
        CreateDataProperty(cx, obj, "value", value);
        /* step 4 */
        CreateDataProperty(cx, obj, "done", done);
        /* step 5 */
        return obj;
    }

    /**
     * 7.4.7 CreateListIterator (list)
     */
    public static ScriptObject CreateListIterator(ExecutionContext cx, Iterable<?> list) {
        // TODO: check implementation for conformance
        return ListIterator.MakeListIterator(cx, list.iterator());
    }

    /**
     * 7.4.8 CreateEmptyIterator ( )
     */
    public static ScriptObject CreateEmptyIterator(ExecutionContext cx) {
        /* step 1 */
        List<?> empty = Collections.emptyList();
        /* step 2 */
        return CreateListIterator(cx, empty);
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
    public static List<String> GetOwnEnumerablePropertyNames(ExecutionContext cx, ScriptObject obj) {
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

    /**
     * Returns a list of all enumerable [[OwnPropertyKeys]] of {@code obj}
     */
    public static List<Object> GetOwnEnumerablePropertyKeys(ExecutionContext cx, ScriptObject obj) {
        // FIXME: spec clean-up (Bug 1142)
        Iterator<?> keys = FromListIterator(cx, obj.ownPropertyKeys(cx));
        List<Object> nameList = new ArrayList<>();
        while (keys.hasNext()) {
            Object key = ToPropertyKey(cx, keys.next());
            Property desc;
            if (key instanceof String) {
                desc = obj.getOwnProperty(cx, (String) key);
            } else {
                desc = obj.getOwnProperty(cx, (Symbol) key);
            }
            if (desc != null && desc.isEnumerable()) {
                nameList.add(key);
            }
        }
        return nameList;
    }

    /**
     * 7.1.3.1 ToNumber Applied to the String Type
     */
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
                while (index < end) {
                    c = s.charAt(index++);
                    if (c == 'e' || c == 'E') {
                        break;
                    }
                    if (!(c >= '0' && c <= '9')) {
                        return Double.NaN;
                    }
                }
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
