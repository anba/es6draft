/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.language;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Errors.newInternalError;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.math.BigInteger;

import org.mozilla.javascript.ConsString;

import com.github.anba.es6draft.runtime.AbstractOperations.ToPrimitiveHint;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.internal.IndexedMap;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.ReturnValue;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.objects.bigint.BigIntType;
import com.github.anba.es6draft.runtime.objects.simd.SIMDType;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.HTMLDDAObject;
import com.github.anba.es6draft.runtime.types.Reference;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.StringObject;

/**
 * <ul>
 * <li>12.5.5 The typeof Operator
 * <li>12.8.3 The Addition operator
 * <li>12.10.3 Runtime Semantics: Evaluation
 * <li>12.10.4 Runtime Semantics: InstanceofOperator(O, C)
 * <li>12.11 Equality Operators
 * <li>14.4.13 Runtime Semantics: Evaluation
 * </ul>
 */
public final class Operators {
    private Operators() {
    }

    public static BigInteger toBigIntOrThrow(Object val, ExecutionContext cx) {
        Object prim = ToPrimitive(cx, val, ToPrimitiveHint.Number);
        if (prim instanceof BigInteger) {
            return (BigInteger) prim;
        }
        throw newTypeError(cx, Messages.Key.BigIntNumber);
    }

    /**
     * 12.8 Additive Operators<br>
     * 12.8.3 The Addition operator ( + )
     * 
     * @param lval
     *            the left-hand side operand
     * @param rval
     *            the right-hand side operand
     * @param cx
     *            the execution context
     * @return the operation result
     */
    public static Object add(Object lval, Object rval, ExecutionContext cx) {
        /* steps 1-4 (generated code) */
        /* step 5 */
        Object lprim = ToPrimitive(cx, lval);
        /* step 6 */
        Object rprim = ToPrimitive(cx, rval);
        /* step 7 */
        if (Type.isString(lprim) || Type.isString(rprim)) {
            CharSequence lstr = ToString(cx, lprim);
            CharSequence rstr = ToString(cx, rprim);
            return add(lstr, rstr, cx);
        }

        // Extension: BigInt
        Number lnum = ToNumeric(cx, lprim);
        Number rnum = ToNumeric(cx, rprim);

        if (lnum.getClass() != rnum.getClass()) {
            throw newTypeError(cx, Messages.Key.BigIntNumber);
        }
        if (lnum.getClass() == Double.class) {
            return lnum.doubleValue() + rnum.doubleValue();
        }
        return BigIntType.add((BigInteger) lnum, (BigInteger) rnum);
    }

    /**
     * 12.8 Additive Operators<br>
     * 12.8.3 The Addition operator ( + )
     * 
     * @param lstr
     *            the left-hand side operand
     * @param rstr
     *            the right-hand side operand
     * @param cx
     *            the execution context
     * @return the concatenated string
     */
    public static CharSequence add(CharSequence lstr, CharSequence rstr, ExecutionContext cx) {
        int llen = lstr.length(), rlen = rstr.length();
        if (llen == 0) {
            return rstr;
        }
        if (rlen == 0) {
            return lstr;
        }
        int newlen = llen + rlen;
        if (newlen < 0 || newlen > StringObject.MAX_LENGTH) {
            throw newInternalError(cx, Messages.Key.InvalidStringSize);
        }
        if (newlen <= 10) {
            // return new StringBuilder(newlen).append(lstr).append(rstr).toString();
            return inlineString(lstr, rstr, llen, rlen);
        }
        return new ConsString(lstr, rstr);
    }

    private static String inlineString(CharSequence lstr, CharSequence rstr, int llen, int rlen) {
        char[] ca = new char[llen + rlen];
        lstr.toString().getChars(0, llen, ca, 0);
        rstr.toString().getChars(0, rlen, ca, llen);
        return new String(ca);
    }

    /**
     * 12.8 Additive Operators<br>
     * 12.8.3 The Addition operator ( + )
     * 
     * @param value
     *            the argument value
     * @param cx
     *            the execution context
     * @return the result of ToString(ToPrimitive(value))
     */
    public static CharSequence toStr(Object value, ExecutionContext cx) {
        return ToString(cx, ToPrimitive(cx, value));
    }

    /**
     * 12.10 Relational Operators<br>
     * 12.10.3 Runtime Semantics: Evaluation
     * 
     * @param lval
     *            the left-hand side operand
     * @param rval
     *            the right-hand side operand
     * @param cx
     *            the execution context
     * @return the operation result
     */
    public static boolean in(int lval, Object rval, ExecutionContext cx) {
        /* steps 1-4 (generated code) */
        /* step 5 */
        if (!Type.isObject(rval)) {
            throw newTypeError(cx, Messages.Key.InNotObject);
        }
        /* step 6 */
        if (lval >= 0) {
            return HasProperty(cx, Type.objectValue(rval), lval);
        }
        return HasProperty(cx, Type.objectValue(rval), Integer.toString(lval));
    }

    /**
     * 12.10 Relational Operators<br>
     * 12.10.3 Runtime Semantics: Evaluation
     * 
     * @param lval
     *            the left-hand side operand
     * @param rval
     *            the right-hand side operand
     * @param cx
     *            the execution context
     * @return the operation result
     */
    public static boolean in(long lval, Object rval, ExecutionContext cx) {
        /* steps 1-4 (generated code) */
        /* step 5 */
        if (!Type.isObject(rval)) {
            throw newTypeError(cx, Messages.Key.InNotObject);
        }
        /* step 6 */
        return HasProperty(cx, Type.objectValue(rval), lval);
    }

    /**
     * 12.10 Relational Operators<br>
     * 12.10.3 Runtime Semantics: Evaluation
     * 
     * @param lval
     *            the left-hand side operand
     * @param rval
     *            the right-hand side operand
     * @param cx
     *            the execution context
     * @return the operation result
     */
    public static boolean in(double lval, Object rval, ExecutionContext cx) {
        /* steps 1-4 (generated code) */
        /* step 5 */
        if (!Type.isObject(rval)) {
            throw newTypeError(cx, Messages.Key.InNotObject);
        }
        /* step 6 */
        long index = (long) lval;
        if (index == lval && IndexedMap.isIndex(index)) {
            return HasProperty(cx, Type.objectValue(rval), index);
        }
        return HasProperty(cx, Type.objectValue(rval), ToString(lval));
    }

    /**
     * 12.10 Relational Operators<br>
     * 12.10.3 Runtime Semantics: Evaluation
     * 
     * @param lval
     *            the left-hand side operand
     * @param rval
     *            the right-hand side operand
     * @param cx
     *            the execution context
     * @return the operation result
     */
    public static boolean in(String lval, Object rval, ExecutionContext cx) {
        /* steps 1-4 (generated code) */
        /* step 5 */
        if (!Type.isObject(rval)) {
            throw newTypeError(cx, Messages.Key.InNotObject);
        }
        /* step 6 */
        long index = IndexedMap.toIndex(lval);
        if (IndexedMap.isIndex(index)) {
            return HasProperty(cx, Type.objectValue(rval), index);
        }
        return HasProperty(cx, Type.objectValue(rval), lval);
    }

    /**
     * 12.10 Relational Operators<br>
     * 12.10.3 Runtime Semantics: Evaluation
     * 
     * @param lval
     *            the left-hand side operand
     * @param rval
     *            the right-hand side operand
     * @param cx
     *            the execution context
     * @return the operation result
     */
    public static boolean in(Object lval, Object rval, ExecutionContext cx) {
        /* steps 1-4 (generated code) */
        /* step 5 */
        if (!Type.isObject(rval)) {
            throw newTypeError(cx, Messages.Key.InNotObject);
        }
        /* step 6 */
        return HasProperty(cx, Type.objectValue(rval), ToPropertyKey(cx, lval));
    }

    /**
     * 12.10 Relational Operators<br>
     * 12.10.4 Runtime Semantics: InstanceofOperator(O, C)
     * 
     * @param obj
     *            the object
     * @param constructor
     *            the constructor function
     * @param cx
     *            the execution context
     * @return the operation result
     */
    public static boolean InstanceofOperator(Object obj, Object constructor, ExecutionContext cx) {
        /* step 1 */
        if (!Type.isObject(constructor)) {
            throw newTypeError(cx, Messages.Key.InstanceofNotObject);
        }
        /* step 2 */
        Callable instOfHandler = GetMethod(cx, Type.objectValue(constructor), BuiltinSymbol.hasInstance.get());
        /* step 3 */
        if (instOfHandler != null) {
            return ToBoolean(instOfHandler.call(cx, constructor, obj));
        }
        /* step 4 */
        if (!IsCallable(constructor)) {
            throw newTypeError(cx, Messages.Key.InstanceofNotCallable);
        }
        /* step 5 */
        return OrdinaryHasInstance(cx, constructor, obj);
    }

    /**
     * 12.11 Equality Operators
     * 
     * @param lval
     *            the first string
     * @param rval
     *            the second string
     * @return the operation result
     */
    public static boolean compare(CharSequence lval, CharSequence rval) {
        return lval.length() == rval.length() && lval.toString().equals(rval.toString());
    }

    /**
     * 12.5 Unary Operators<br>
     * 12.5.5 The typeof Operator
     * 
     * @param val
     *            the value
     * @param cx
     *            the execution context
     * @return the typeof descriptor string
     */
    public static String typeof(Object val, ExecutionContext cx) {
        /* step 1 (generated code) */
        /* step 2 */
        if (val instanceof Reference) {
            Reference<?, ?> ref = (Reference<?, ?>) val;
            if (ref.isUnresolvableReference()) {
                return "undefined";
            }
            /* step 3 */
            val = ref.getValue(cx);
        }
        /* step 4 */
        return typeof(val);
    }

    /**
     * 12.5 Unary Operators<br>
     * 12.5.5 The typeof Operator
     * 
     * @param ref
     *            the reference
     * @param cx
     *            the execution context
     * @return the typeof descriptor string
     */
    public static String typeof(Reference<?, ?> ref, ExecutionContext cx) {
        /* step 2 */
        if (ref.isUnresolvableReference()) {
            return "undefined";
        }
        /* steps 3-4 */
        return typeof(ref.getValue(cx));
    }

    /**
     * 12.5 Unary Operators<br>
     * 12.5.5 The typeof Operator
     * 
     * @param val
     *            the value
     * @return the typeof descriptor string
     */
    public static String typeof(Object val) {
        /* steps 1-3 (generated code) */
        /* step 4 */
        switch (Type.of(val)) {
        case Undefined:
            return "undefined";
        case Null:
            return "object";
        case Boolean:
            return "boolean";
        case Number:
            return "number";
        case String:
            return "string";
        case Symbol:
            return "symbol";
        case SIMD:
            return Type.simdValue(val).getType().typeof();
        case BigInt:
            return "bigint";
        case Object:
            if (val instanceof HTMLDDAObject) {
                return "undefined";
            }
            if (IsCallable(val)) {
                return "function";
            }
            return "object";
        default:
            throw new AssertionError();
        }
    }

    /**
     * 12.5 Unary Operators<br>
     * 12.5.5 The typeof Operator
     * 
     * @param val
     *            the value
     * @return {@code true} on success
     */
    public static boolean isNonCallableObjectOrNull(Object val) {
        return Type.isNull(val) || (Type.isObject(val) && !IsCallable(val));
    }

    /**
     * 12.5 Unary Operators<br>
     * 12.5.5 The typeof Operator
     * 
     * @param val
     *            the value
     * @param type
     *            the SIMD type
     * @return {@code true} on success
     */
    public static boolean isSIMDType(Object val, SIMDType type) {
        return Type.isSIMD(val) && Type.simdValue(val).getType() == type;
    }

    /**
     * 14.4 Generator Function Definitions
     * <p>
     * 14.4.13 Runtime Semantics: Evaluation
     * <ul>
     * <li>YieldExpression : yield * AssignmentExpression
     * </ul>
     * 
     * @param cx
     *            the execution context
     * @param iterator
     *            the iterator object
     * @param e
     *            the script exception
     * @return the {@code throw} method result value
     */
    public static ScriptObject yieldThrowCompletion(ExecutionContext cx, ScriptObject iterator, ScriptException e) {
        /* step 5.b */
        /* step 5.b.i */
        Callable throwMethod = GetMethod(cx, iterator, "throw");
        /* steps 5.b.ii-iii */
        if (throwMethod != null) {
            /* step 5.b.ii */
            /* step 5.b.ii.1 */
            Object innerThrowResult = throwMethod.call(cx, iterator, e.getValue());
            /* step 5.b.ii.2 (note) */
            /* step 5.b.ii.3 */
            return requireObjectResult(innerThrowResult, "throw", cx);
        } else {
            /* step 5.b.iii */
            /* step 5.b.iii.1 (note) */
            /* step 5.b.iii.2 */
            IteratorClose(cx, iterator);
            /* step 5.b.iii.3 (note) */
            /* step 5.b.iii.4 */
            throw reportPropertyNotCallable("throw", cx);
        }
    }

    /**
     * 14.4 Generator Function Definitions
     * <p>
     * 14.4.13 Runtime Semantics: Evaluation
     * <ul>
     * <li>YieldExpression : yield * AssignmentExpression
     * </ul>
     * 
     * @param cx
     *            the execution context
     * @param iterator
     *            the iterator object
     * @param e
     *            the return value wrapper
     * @return the {@code return} method result value or {@code null}
     */
    public static ScriptObject yieldReturnCompletion(ExecutionContext cx, ScriptObject iterator, ReturnValue e) {
        /* step 5.c */
        /* step 5.c.i (not applicable) */
        /* step 5.c.ii */
        Callable returnMethod = GetMethod(cx, iterator, "return");
        /* step 5.c.iii */
        if (returnMethod == null) {
            return null;
        }
        /* step 5.c.iv */
        Object innerReturnResult = returnMethod.call(cx, iterator, e.getValue());
        /* step 5.c.v */
        return requireObjectResult(innerReturnResult, "return", cx);
    }

    public static ScriptObject requireObjectResult(Object resultValue, String methodName, ExecutionContext cx) {
        if (!Type.isObject(resultValue)) {
            throw newTypeError(cx, Messages.Key.NotObjectTypeReturned, methodName);
        }
        return Type.objectValue(resultValue);
    }

    public static ScriptException reportPropertyNotCallable(String methodName, ExecutionContext cx) {
        throw newTypeError(cx, Messages.Key.PropertyNotCallable, methodName);
    }

    public static CallSite runtimeBootstrap(MethodHandles.Lookup caller, String name, MethodType type) {
        assert "rt:stack".equals(name) || "rt:locals".equals(name);
        MethodHandle mh = MethodHandles.identity(Object[].class);
        mh = mh.asCollector(Object[].class, type.parameterCount());
        mh = mh.asType(type);
        return new ConstantCallSite(mh);
    }

    /**
     * Extension: 'function.sent' meta property
     * 
     * @param cx
     *            the execution context
     * @return the last yield value
     */
    public static Object functionSent(ExecutionContext cx) {
        return cx.getCurrentGenerator().getLastYieldValue();
    }

    /**
     * 12.3.8 Meta Properties
     * 
     * @param cx
     *            the execution context
     * @return the NewTarget constructor object
     */
    public static Object GetNewTargetOrUndefined(ExecutionContext cx) {
        Constructor newTarget = cx.getNewTarget();
        if (newTarget == null) {
            return UNDEFINED;
        }
        return newTarget;
    }

    /**
     * Extension: ThrowExpression
     * 
     * @param value
     *            the value to throw
     * @return always throws an exception
     */
    public static Object _throw(Object value) {
        throw ScriptException.create(value);
    }
}
