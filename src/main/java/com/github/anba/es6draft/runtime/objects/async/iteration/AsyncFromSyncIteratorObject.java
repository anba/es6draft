/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.async.iteration;

import static com.github.anba.es6draft.runtime.AbstractOperations.Get;
import static com.github.anba.es6draft.runtime.AbstractOperations.IteratorComplete;
import static com.github.anba.es6draft.runtime.AbstractOperations.IteratorNext;
import static com.github.anba.es6draft.runtime.AbstractOperations.IteratorValue;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.objects.promise.PromiseAbstractOperations.PromiseBuiltinCapability;
import static com.github.anba.es6draft.runtime.objects.promise.PromiseCapability.IfAbruptRejectPromise;
import static com.github.anba.es6draft.runtime.objects.promise.PromisePrototype.PerformPromiseThen;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.internal.ScriptIterator;
import com.github.anba.es6draft.runtime.internal.ScriptIterators;
import com.github.anba.es6draft.runtime.objects.async.iteration.AsyncGeneratorAbstractOperations.AsyncFromSyncIteratorValueUnwrapFunction;
import com.github.anba.es6draft.runtime.objects.promise.PromiseCapability;
import com.github.anba.es6draft.runtime.objects.promise.PromiseObject;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * Properties of Async-from-Sync Iterator Instances
 */
public final class AsyncFromSyncIteratorObject extends OrdinaryObject {
    /** [[SyncIterator]] */
    private final ScriptObject syncIterator;

    /** [[SyncIteratorRecord]] */
    private final ScriptIterator<?> syncIteratorRec;

    AsyncFromSyncIteratorObject(Realm realm, ScriptObject syncIterator, Object nextMethod, ScriptObject prototype) {
        super(realm, prototype);
        this.syncIterator = syncIterator;
        this.syncIteratorRec = ScriptIterators.ToScriptIterator(realm.defaultContext(), syncIterator, nextMethod);
    }

    /**
     * [[SyncIterator]]
     * 
     * @return the sync-iterator object
     */
    public ScriptObject getSyncIterator() {
        return syncIterator;
    }

    /**
     * [[SyncIteratorRecord]]
     * 
     * @return the sync-iterator record
     */
    public ScriptIterator<?> getSyncIteratorRecord() {
        return syncIteratorRec;
    }

    /**
     * %AsyncFromSyncIteratorPrototype%.next ( value )
     * 
     * @param cx
     *            the execution context
     * @param value
     *            the next value
     * @return the this-value
     */
    public PromiseObject next(ExecutionContext cx, Object value) {
        /* step 1 (not applicable) */
        /* step 2 */
        PromiseCapability<PromiseObject> promiseCapability = PromiseBuiltinCapability(cx);
        /* step 3 (not applicable) */
        /* step 4 */
        ScriptIterator<?> syncIterator = this.getSyncIteratorRecord();
        /* steps 5-10 */
        boolean nextDone;
        Object nextValue;
        try {
            /* step 5 */
            ScriptObject nextResult = IteratorNext(cx, syncIterator, value);
            /* step 7 */
            nextDone = IteratorComplete(cx, nextResult);
            /* step 9 */
            nextValue = IteratorValue(cx, nextResult);
        } catch (ScriptException e) {
            /* steps 6, 8, 10  */
            return IfAbruptRejectPromise(cx, e, promiseCapability);
        }
        /* step 11 */
        PromiseCapability<PromiseObject> valueWrapperCapability = PromiseBuiltinCapability(cx);
        /* step 12 */
        valueWrapperCapability.getResolve().call(cx, UNDEFINED, nextValue);
        /* steps 13-14 */
        AsyncFromSyncIteratorValueUnwrapFunction onFulfilled = new AsyncFromSyncIteratorValueUnwrapFunction(
                cx.getRealm(), nextDone);
        /* step 15 */
        PerformPromiseThen(cx, valueWrapperCapability.getPromise(), onFulfilled, UNDEFINED, promiseCapability);
        /* step 16 */
        return promiseCapability.getPromise();
    }

    /**
     * CreateAsyncFromSyncIterator(syncIterator) Abstract Operation
     * 
     * @param cx
     *            the execution context
     * @param syncIterator
     *            the synchronous iterator
     * @return the asynchronous iterator
     */
    public static AsyncFromSyncIteratorObject CreateAsyncFromSyncIterator(ExecutionContext cx, Object syncIterator) {
        /* step 1 */
        if (!Type.isObject(syncIterator)) {
            throw newTypeError(cx, Messages.Key.NotObjectTypeReturned, BuiltinSymbol.iterator.toString());
        }
        Object nextMethod = Get(cx, Type.objectValue(syncIterator), "next");
        /* steps 2-4 */
        return new AsyncFromSyncIteratorObject(cx.getRealm(), Type.objectValue(syncIterator), nextMethod,
                cx.getIntrinsic(Intrinsics.AsyncFromSyncIteratorPrototype));
    }
}
