/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.language;

import static com.github.anba.es6draft.runtime.AbstractOperations.CreateArrayFromList;
import static com.github.anba.es6draft.runtime.AbstractOperations.GetIterator;
import static com.github.anba.es6draft.runtime.AbstractOperations.IsCallable;
import static com.github.anba.es6draft.runtime.AbstractOperations.IsConstructor;
import static com.github.anba.es6draft.runtime.internal.Errors.newInternalError;
import static com.github.anba.es6draft.runtime.internal.Errors.newRangeError;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.TailCallInvocation.newTailCallInvocation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Consumer;

import com.github.anba.es6draft.runtime.EnvironmentRecord;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.internal.ScriptIterator;
import com.github.anba.es6draft.runtime.internal.ScriptIterators;
import com.github.anba.es6draft.runtime.objects.FunctionPrototype;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Reference;
import com.github.anba.es6draft.runtime.types.builtins.ArgumentsObject;
import com.github.anba.es6draft.runtime.types.builtins.ArrayObject;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * 
 */
public final class CallOperations {
    private CallOperations() {
    }

    public static final Object[] EMPTY_ARRAY = new Object[0];

    /**
     * 12.3.3 The new Operator
     * <p>
     * 12.3.3.1 Runtime Semantics: Evaluation<br>
     * 12.3.5.1 Runtime Semantics: Evaluation
     * <ul>
     * <li>NewExpression : new NewExpression
     * <li>MemberExpression : new MemberExpression Arguments
     * <li>MemberExpression : NewSuper Arguments<span><sub>opt</sub></span>
     * </ul>
     * 
     * @param constructor
     *            the constructor function object
     * @param cx
     *            the execution context
     * @return the constructor function object
     * @throws ScriptException
     *             if <var>constructor</var> is not a constructor function
     */
    public static Constructor CheckConstructor(Object constructor, ExecutionContext cx) throws ScriptException {
        /* steps 4/6/5 */
        if (!IsConstructor(constructor)) {
            throw newTypeError(cx, Messages.Key.NotConstructor);
        }
        return (Constructor) constructor;
    }

    /**
     * 12.3.4 Function Calls
     * <p>
     * 12.3.4.3 Runtime Semantics: EvaluateDirectCall( func, thisValue, arguments, tailPosition )
     * 
     * @param func
     *            the function object
     * @param cx
     *            the execution context
     * @return the function object
     * @throws ScriptException
     *             if <var>func</var> is not a function
     */
    public static Callable CheckCallable(Object func, ExecutionContext cx) throws ScriptException {
        /* steps 3-4 */
        if (!IsCallable(func)) {
            throw newTypeError(cx, Messages.Key.NotCallable);
        }
        return (Callable) func;
    }

    /**
     * 12.3.4 Function Calls
     * <p>
     * 12.3.4.1 Runtime Semantics: Evaluation
     * 
     * @param ref
     *            the reference value
     * @param f
     *            the function object
     * @param cx
     *            the execution context
     * @return {@code true} if <var>f</var> is the built-in eval function
     */
    public static boolean IsBuiltinEval(Object ref, Callable f, ExecutionContext cx) {
        /* step 4 */
        if (ref instanceof Reference) {
            Reference<?, ?> r = (Reference<?, ?>) ref;
            if (!r.isPropertyReference()) {
                assert !r.isUnresolvableReference() && r.getBase() instanceof EnvironmentRecord;
                return f == cx.getRealm().getBuiltinEval();
            }
        }
        return false;
    }

    /**
     * 12.3.4 Function Calls
     * <p>
     * 12.3.4.1 Runtime Semantics: Evaluation
     * 
     * @param f
     *            the function object
     * @param cx
     *            the execution context
     * @return {@code true} if <var>f</var> is the built-in eval function
     */
    public static boolean IsBuiltinEval(Callable f, ExecutionContext cx) {
        /* step 4 */
        return f == cx.getRealm().getBuiltinEval();
    }

    /**
     * 12.3.4 Function Calls
     * 
     * @param cx
     *            the execution context
     * @return the direct eval fallback hook
     */
    public static Callable directEvalFallbackHook(ExecutionContext cx) {
        return cx.getRealm().getNonEvalFallback();
    }

    /**
     * 12.3.4 Function Calls
     * 
     * @param callee
     *            the function callee
     * @param cx
     *            the execution context
     * @param thisValue
     *            the function this-value
     * @param args
     *            the function call arguments
     * @return the direct eval fallback arguments
     */
    public static Object[] directEvalFallbackArguments(Callable callee, ExecutionContext cx, Object thisValue,
            Object[] args) {
        return new Object[] { callee, thisValue, CreateArrayFromList(cx, Arrays.asList(args)) };
    }

    /**
     * 12.3.4 Function Calls
     * 
     * @param cx
     *            the execution context
     * @return the direct eval fallback this-argument
     */
    public static Object directEvalFallbackThisArgument(ExecutionContext cx) {
        return cx.getRealm().getRealmObject();
    }

    /**
     * 12.3.6 Argument Lists
     * <p>
     * 12.3.6.1 Runtime Semantics: ArgumentListEvaluation
     * 
     * @param spreadObj
     *            the spread object
     * @param cx
     *            the execution context
     * @return the spread object elements
     */
    public static Object[] spreadArray(Object spreadObj, ExecutionContext cx) {
        final int MAX_ARGS = FunctionPrototype.getMaxArguments();
        if (isSpreadableObject(spreadObj)) {
            OrdinaryObject object = (OrdinaryObject) spreadObj;
            long length = object.getLength();
            if (0 <= length && length <= MAX_ARGS && ScriptIterators.isBuiltinArrayIterator(cx, object, length)) {
                return object.toArray(length);
            }
        }
        /* steps 1-3 (cf. generated code) */
        /* steps 4-5 */
        ScriptIterator<?> iterator = GetIterator(cx, spreadObj);
        /* step 6 */
        class ArgList<T> implements Consumer<T> {
            final ArrayList<T> list = new ArrayList<>();

            @Override
            public void accept(T t) {
                if (list.size() > MAX_ARGS) {
                    throw newRangeError(cx, Messages.Key.FunctionTooManyArguments);
                }
                list.add(t);
            }
        }
        ArgList<Object> args = new ArgList<>();
        iterator.forEachRemaining(args);
        return args.list.toArray();
    }

    private static boolean isSpreadableObject(Object object) {
        return object instanceof ArrayObject || object instanceof ArgumentsObject
                || object.getClass() == OrdinaryObject.class;
    }

    /**
     * 12.3.6 Argument Lists
     * <p>
     * 12.3.6.1 Runtime Semantics: ArgumentListEvaluation
     * 
     * @param spreadObj
     *            the spread object
     * @param cx
     *            the execution context
     * @return the spread object elements
     */
    public static Object[] nativeCallSpreadArray(Object spreadObj, ExecutionContext cx) {
        final int MAX_ARGS = FunctionPrototype.getMaxArguments();
        if (spreadObj instanceof ArrayObject) {
            ArrayObject array = (ArrayObject) spreadObj;
            long length = array.getLength();
            if (array.isDenseArray(length) && length <= MAX_ARGS) {
                return array.toArray(length);
            }
        }
        throw newInternalError(cx, Messages.Key.InternalError, "Invalid native call");
    }

    /**
     * 12.3.6 Argument Lists
     * <p>
     * 12.3.6.1 Runtime Semantics: ArgumentListEvaluation
     * 
     * @param array
     *            the array
     * @param cx
     *            the execution context
     * @return the flattened array
     */
    public static Object[] toFlatArray(Object[] array, ExecutionContext cx) {
        final int MAX_ARGS = FunctionPrototype.getMaxArguments();
        int newlen = array.length;
        for (int i = 0, len = array.length; i < len; ++i) {
            if (array[i] instanceof Object[]) {
                newlen += ((Object[]) array[i]).length - 1;
                if (newlen > MAX_ARGS) {
                    throw newRangeError(cx, Messages.Key.FunctionTooManyArguments);
                }
            }
        }
        Object[] result = new Object[newlen];
        for (int i = 0, j = 0, len = array.length; i < len; ++i) {
            if (array[i] instanceof Object[]) {
                Object[] a = (Object[]) array[i];
                System.arraycopy(a, 0, result, j, a.length);
                j += a.length;
            } else {
                result[j++] = array[i];
            }
        }
        return result;
    }

    /**
     * 14.6 Tail Position Calls
     * <p>
     * 14.6.1 Runtime Semantics: PrepareForTailCall
     * 
     * @param args
     *            the function arguments
     * @param thisValue
     *            the function this-value
     * @param function
     *            the tail call function
     * @return the tail call trampoline object
     */
    public static Object PrepareForTailCall(Callable function, Object thisValue, Object[] args) {
        return newTailCallInvocation(function, thisValue, args);
    }

    // Called from generated code
    public static Object PrepareForTailCall(Callable function, ExecutionContext cx, Object thisValue, Object[] args) {
        return newTailCallInvocation(function, thisValue, args);
    }

    // Called from generated code
    public static Object PrepareForTailCall(Object function, ExecutionContext cx, Object thisValue, Object[] args) {
        return newTailCallInvocation(CheckCallable(function, cx), thisValue, args);
    }
}
