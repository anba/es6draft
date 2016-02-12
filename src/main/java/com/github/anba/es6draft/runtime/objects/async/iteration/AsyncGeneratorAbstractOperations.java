/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.async.iteration;

import static com.github.anba.es6draft.runtime.AbstractOperations.GetMethod;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.ReturnValue;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.objects.promise.PromiseObject;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;

/**
 * AsyncGenerator Abstract Operations
 */
public final class AsyncGeneratorAbstractOperations {
    private AsyncGeneratorAbstractOperations() {
    }

    /**
     * GetIterator ( obj [ , hint ] )
     * 
     * @param cx
     *            the execution context
     * @param obj
     *            the script object
     * @return the script iterator object
     */
    public static ScriptObject GetAsyncIterator(ExecutionContext cx, Object obj) {
        /* steps 1-2 (not applicable) */
        /* step 3 */
        Callable method = GetMethod(cx, obj, BuiltinSymbol.asyncIterator.get());
        /* step 4 (not applicable) */
        /* step 5 (inlined Call operation) */
        if (method == null) {
            throw newTypeError(cx, Messages.Key.PropertyNotCallable, BuiltinSymbol.asyncIterator.toString());
        }
        Object iterator = method.call(cx, obj);
        /* step 6 */
        if (!Type.isObject(iterator)) {
            throw newTypeError(cx, Messages.Key.NotObjectTypeReturned, "[Symbol.asyncIterator]");
        }
        /* step 7 */
        return Type.objectValue(iterator);
    }

    /**
     * AsyncGeneratorStart ( generator, generatorBody )
     * 
     * @param cx
     *            the execution context
     * @param generator
     *            the async generator object
     * @param generatorBody
     *            the runtime function code
     */
    public static void AsyncGeneratorStart(ExecutionContext cx, AsyncGeneratorObject generator,
            RuntimeInfo.Function generatorBody) {
        generator.start(cx, generatorBody);
    }

    /**
     * AsyncGeneratorEnqueue ( generator, completion )
     * 
     * @param cx
     *            the execution context
     * @param generator
     *            the async generator object
     * @param completion
     *            the completion record
     * @return the promise object
     */
    public static PromiseObject AsyncGeneratorEnqueue(ExecutionContext cx, Object generator, Object completion) {
        /* step 1 (not applicable) */
        /* step 2 */
        if (!Type.isObject(generator)) {
            throw newTypeError(cx, Messages.Key.NotObjectType);
        }
        /* step 3 */
        if (!(generator instanceof AsyncGeneratorObject)) {
            throw newTypeError(cx, Messages.Key.IncompatibleObject);
        }
        /* steps 4-10 */
        return ((AsyncGeneratorObject) generator).enqueue(cx, AsyncGeneratorRequest.CompletionType.Normal, completion);
    }

    /**
     * AsyncGeneratorEnqueue ( generator, completion )
     * 
     * @param cx
     *            the execution context
     * @param generator
     *            the async generator object
     * @param completion
     *            the completion record
     * @return the promise object
     */
    public static PromiseObject AsyncGeneratorEnqueue(ExecutionContext cx, Object generator, ReturnValue completion) {
        /* step 1 (not applicable) */
        /* step 2 */
        if (!Type.isObject(generator)) {
            throw newTypeError(cx, Messages.Key.NotObjectType);
        }
        /* step 3 */
        if (!(generator instanceof AsyncGeneratorObject)) {
            throw newTypeError(cx, Messages.Key.IncompatibleObject);
        }
        /* steps 4-10 */
        return ((AsyncGeneratorObject) generator).enqueue(cx, AsyncGeneratorRequest.CompletionType.Return, completion);
    }

    /**
     * AsyncGeneratorEnqueue ( generator, completion )
     * 
     * @param cx
     *            the execution context
     * @param generator
     *            the async generator object
     * @param completion
     *            the completion record
     * @return the promise object
     */
    public static PromiseObject AsyncGeneratorEnqueue(ExecutionContext cx, Object generator,
            ScriptException completion) {
        /* step 1 (not applicable) */
        /* step 2 */
        if (!Type.isObject(generator)) {
            throw newTypeError(cx, Messages.Key.NotObjectType);
        }
        /* step 3 */
        if (!(generator instanceof AsyncGeneratorObject)) {
            throw newTypeError(cx, Messages.Key.IncompatibleObject);
        }
        /* steps 4-10 */
        return ((AsyncGeneratorObject) generator).enqueue(cx, AsyncGeneratorRequest.CompletionType.Throw, completion);
    }
}
