/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.promise;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.objects.promise.PromiseConstructor.InitializePromise;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject.OrdinaryCreateFromConstructor;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.Task;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.MutRef;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.Undefined;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinFunction;

/**
 * <h1>25 Control Abstraction Objects</h1><br>
 * <h2>25.4 Promise Objects</h2>
 * <ul>
 * <li>25.4.1 Promise Abstract Operations
 * <li>25.4.2 Promise Tasks
 * </ul>
 */
public final class PromiseAbstractOperations {
    private PromiseAbstractOperations() {
    }

    private static final class PromiseObjectAllocator implements ObjectAllocator<PromiseObject> {
        static final ObjectAllocator<PromiseObject> INSTANCE = new PromiseObjectAllocator();

        @Override
        public PromiseObject newInstance(Realm realm) {
            return new PromiseObject(realm);
        }
    }

    /**
     * <h2>25.4.1 Promise Abstract Operations</h2>
     * <p>
     * 25.4.1.3 AllocatePromise ( constructor )
     * 
     * @param cx
     *            the execution context
     * @param constructor
     *            the constructor function
     * @return the new promise object
     */
    public static PromiseObject AllocatePromise(ExecutionContext cx, Constructor constructor) {
        /* step 1 */
        PromiseObject obj = OrdinaryCreateFromConstructor(cx, constructor,
                Intrinsics.PromisePrototype, PromiseObjectAllocator.INSTANCE);
        /* step 2 */
        obj.setConstructor(constructor);
        /* step 3 */
        return obj;
    }

    public static final class ResolvingFunctions {
        /** [[Resolve]] */
        private final PromiseResolveFunction resolve;

        /** [[Reject]] */
        private final PromiseRejectFunction reject;

        ResolvingFunctions(PromiseResolveFunction resolve, PromiseRejectFunction reject) {
            this.resolve = resolve;
            this.reject = reject;
        }

        /**
         * [[Resolve]]
         *
         * @return the resolve function
         */
        public PromiseResolveFunction getResolve() {
            return resolve;
        }

        /**
         * [[Reject]]
         *
         * @return the reject function
         */
        public PromiseRejectFunction getReject() {
            return reject;
        }
    }

    /**
     * <h2>25.4.1 Promise Abstract Operations</h2>
     * <p>
     * 25.4.1.4 CreateResolvingFunctions ( promise )
     * 
     * @param cx
     *            the execution context
     * @param promise
     *            the promise object
     * @return the resolving functions tuple
     */
    public static ResolvingFunctions CreateResolvingFunctions(ExecutionContext cx,
            PromiseObject promise) {
        assert promise.getState() != null : "Promise not initialized";
        /* step 1 */
        AtomicBoolean alreadyResolved = new AtomicBoolean(false);
        /* steps 2-4 */
        PromiseResolveFunction resolve = new PromiseResolveFunction(cx.getRealm(), promise,
                alreadyResolved);
        /* steps 5-7 */
        PromiseRejectFunction reject = new PromiseRejectFunction(cx.getRealm(), promise,
                alreadyResolved);
        /* step 8 */
        return new ResolvingFunctions(resolve, reject);
    }

    /**
     * 25.4.1.4.1 Promise Reject Functions
     */
    public static final class PromiseRejectFunction extends BuiltinFunction {
        /** [[Promise]] */
        private final PromiseObject promise;
        /** [[AlreadyResolved]] */
        private final AtomicBoolean alreadyResolved;

        public PromiseRejectFunction(Realm realm, PromiseObject promise,
                AtomicBoolean alreadyResolved) {
            this(realm, promise, alreadyResolved, null);
            createDefaultFunctionProperties();
        }

        private PromiseRejectFunction(Realm realm, PromiseObject promise,
                AtomicBoolean alreadyResolved, Void ignore) {
            super(realm, ANONYMOUS, 1);
            assert promise.getState() != null : "Promise not initialized";
            this.promise = promise;
            this.alreadyResolved = alreadyResolved;
        }

        @Override
        public PromiseRejectFunction clone() {
            return new PromiseRejectFunction(getRealm(), promise, alreadyResolved, null);
        }

        @Override
        public Undefined call(ExecutionContext callerContext, Object thisValue, Object... args) {
            ExecutionContext calleeContext = calleeContext();
            Object reason = argument(args, 0);
            /* step 1 (not applicable) */
            /* step 2 */
            PromiseObject promise = this.promise;
            /* steps 3-5 */
            if (!alreadyResolved.compareAndSet(false, true)) {
                return UNDEFINED;
            }
            /* step 6 */
            RejectPromise(calleeContext, promise, reason);
            return UNDEFINED;
        }
    }

    /**
     * 25.4.1.4.2 Promise Resolve Functions
     */
    public static final class PromiseResolveFunction extends BuiltinFunction {
        /** [[Promise]] */
        private final PromiseObject promise;
        /** [[AlreadyResolved]] */
        private final AtomicBoolean alreadyResolved;

        public PromiseResolveFunction(Realm realm, PromiseObject promise,
                AtomicBoolean alreadyResolved) {
            this(realm, promise, alreadyResolved, null);
            createDefaultFunctionProperties();
        }

        private PromiseResolveFunction(Realm realm, PromiseObject promise,
                AtomicBoolean alreadyResolved, Void ignore) {
            super(realm, ANONYMOUS, 1);
            assert promise.getState() != null : "Promise not initialized";
            this.promise = promise;
            this.alreadyResolved = alreadyResolved;
        }

        @Override
        public PromiseResolveFunction clone() {
            return new PromiseResolveFunction(getRealm(), promise, alreadyResolved, null);
        }

        @Override
        public Undefined call(ExecutionContext callerContext, Object thisValue, Object... args) {
            ExecutionContext calleeContext = calleeContext();
            Object resolution = argument(args, 0);
            /* step 1 (not applicable) */
            /* step 2 */
            PromiseObject promise = this.promise;
            /* steps 3-5 */
            if (!alreadyResolved.compareAndSet(false, true)) {
                return UNDEFINED;
            }
            /* step 6 */
            if (SameValue(resolution, promise)) {
                ScriptException selfResolutionError = newTypeError(calleeContext,
                        Messages.Key.PromiseSelfResolution);
                RejectPromise(calleeContext, promise, selfResolutionError.getValue());
                return UNDEFINED;
            }
            /* step 7 */
            if (!Type.isObject(resolution)) {
                FulfillPromise(calleeContext, promise, resolution);
                return UNDEFINED;
            }
            /* steps 8-10 */
            Object then;
            try {
                then = Get(calleeContext, Type.objectValue(resolution), "then");
            } catch (ScriptException e) {
                /* step 9 */
                RejectPromise(calleeContext, promise, e.getValue());
                return UNDEFINED;
            }
            /* step 11 */
            if (!IsCallable(then)) {
                FulfillPromise(calleeContext, promise, resolution);
                return UNDEFINED;
            }
            /* step 12 */
            Realm realm = calleeContext.getRealm();
            realm.enqueuePromiseTask(new PromiseResolveThenableTask(realm, promise, Type
                    .objectValue(resolution), (Callable) then));
            /* step 13 */
            return UNDEFINED;
        }
    }

    /**
     * <h2>25.4.1 Promise Abstract Operations</h2>
     * <p>
     * 25.4.1.5 FulfillPromise (promise, value)
     * 
     * @param cx
     *            the execution context
     * @param promise
     *            the promise object
     * @param value
     *            the resolve value
     */
    public static void FulfillPromise(ExecutionContext cx, PromiseObject promise, Object value) {
        List<PromiseReaction> reactions = promise.fufill(value);
        TriggerPromiseReactions(cx, reactions, value);
    }

    /**
     * <h2>25.4.1 Promise Abstract Operations</h2>
     * <p>
     * 25.4.1.6 NewPromiseCapability ( C )
     * 
     * @param cx
     *            the execution context
     * @param c
     *            the promise constructor function
     * @return the new promise capability record
     */
    public static PromiseCapability<ScriptObject> NewPromiseCapability(ExecutionContext cx, Object c) {
        /* step 1 */
        if (!IsConstructor(c)) {
            throw newTypeError(cx, Messages.Key.NotConstructor);
        }
        Constructor constructor = (Constructor) c;
        /* step 2 (not applicable) */
        /* steps 3-4 */
        ScriptObject promise = CreateFromConstructor(cx, constructor);
        /* step 5 */
        return CreatePromiseCapabilityRecord(cx, promise, constructor);
    }

    /**
     * <h2>25.4.1 Promise Abstract Operations</h2>
     * <p>
     * 25.4.1.6 NewPromiseCapability ( C )
     * 
     * @param cx
     *            the execution context
     * @param c
     *            the promise constructor function
     * @return the new promise capability record
     */
    public static PromiseCapability<ScriptObject> NewPromiseCapability(ExecutionContext cx,
            Constructor c) {
        /* steps 1-2 (not applicable) */
        /* steps 3-4 */
        ScriptObject promise = CreateFromConstructor(cx, c);
        /* step 5 */
        return CreatePromiseCapabilityRecord(cx, promise, c);
    }

    /**
     * 25.4.1.6.1 CreatePromiseCapabilityRecord( promise, constructor )
     * 
     * @param <PROMISE>
     *            the promise type
     * @param cx
     *            the execution context
     * @param promise
     *            the promise object
     * @param constructor
     *            the promise constructor function
     * @return the new promise capability record
     */
    public static <PROMISE extends ScriptObject> PromiseCapability<PROMISE> CreatePromiseCapabilityRecord(
            ExecutionContext cx, PROMISE promise, Constructor constructor) {
        /* steps 1-2 (not applicable) */
        /* step 2 (not applicable) */
        /* steps 4-5 */
        GetCapabilitiesExecutor executor = new GetCapabilitiesExecutor(cx.getRealm());
        /* steps 6-7 */
        Object constructorResult = constructor.call(cx, promise, executor);
        /* step 8 */
        Object resolve = executor.resolve.get();
        if (!IsCallable(resolve)) {
            throw newTypeError(cx, Messages.Key.NotCallable);
        }
        /* step 9 */
        Object reject = executor.reject.get();
        if (!IsCallable(reject)) {
            throw newTypeError(cx, Messages.Key.NotCallable);
        }
        /* step 10 */
        if (Type.isObject(constructorResult) && !SameValue(promise, constructorResult)) {
            throw newTypeError(cx, Messages.Key.PromiseConstructorSameObject);
        }
        /* steps 3, 11 */
        return new PromiseCapability<>(promise, (Callable) resolve, (Callable) reject);
    }

    /**
     * 25.4.1.6.2 GetCapabilitiesExecutor Functions
     */
    public static final class GetCapabilitiesExecutor extends BuiltinFunction {
        /** [[Resolve]] */
        private final MutRef<Object> resolve;

        /** [[Reject]] */
        private final MutRef<Object> reject;

        public GetCapabilitiesExecutor(Realm realm) {
            this(realm, new MutRef<Object>(UNDEFINED), new MutRef<Object>(UNDEFINED));
            createDefaultFunctionProperties();
        }

        private GetCapabilitiesExecutor(Realm realm, MutRef<Object> resolve, MutRef<Object> reject) {
            super(realm, ANONYMOUS, 2);
            this.resolve = resolve;
            this.reject = reject;
        }

        @Override
        public GetCapabilitiesExecutor clone() {
            return new GetCapabilitiesExecutor(getRealm(), resolve, reject);
        }

        @Override
        public Undefined call(ExecutionContext callerContext, Object thisValue, Object... args) {
            ExecutionContext calleeContext = calleeContext();
            Object resolve = argument(args, 0);
            Object reject = argument(args, 1);
            /* step 1 (not applicable) */
            /* step 2 (omitted) */
            /* step 3 */
            if (!Type.isUndefined(this.resolve.get())) {
                throw newTypeError(calleeContext, Messages.Key.NotUndefined);
            }
            /* step 4 */
            if (!Type.isUndefined(this.reject.get())) {
                throw newTypeError(calleeContext, Messages.Key.NotUndefined);
            }
            /* step 5 */
            this.resolve.set(resolve);
            /* step 6 */
            this.reject.set(reject);
            /* step 7 */
            return UNDEFINED;
        }
    }

    /**
     * <h2>25.4.1 Promise Abstract Operations</h2>
     * <p>
     * 25.4.1.7 IsPromise ( x )
     * 
     * @param x
     *            the object
     * @return {@code true} if <var>x</var> is an initialized promise object
     */
    public static boolean IsPromise(Object x) {
        /* steps 1-2 */
        if (!(x instanceof PromiseObject)) {
            return false;
        }
        /* steps 3-4 */
        return ((PromiseObject) x).getState() != null;
    }

    /**
     * <h2>25.4.1 Promise Abstract Operations</h2>
     * <p>
     * 25.4.1.8 RejectPromise (promise, reason)
     * 
     * @param cx
     *            the execution context
     * @param promise
     *            the promise object
     * @param reason
     *            the rejection reason
     */
    public static void RejectPromise(ExecutionContext cx, PromiseObject promise, Object reason) {
        List<PromiseReaction> reactions = promise.reject(reason);
        TriggerPromiseReactions(cx, reactions, reason);
    }

    /**
     * <h2>25.4.1 Promise Abstract Operations</h2>
     * <p>
     * 25.4.1.9 TriggerPromiseReactions ( reactions, argument )
     * 
     * @param cx
     *            the execution context
     * @param reactions
     *            the list of promise reactions
     * @param argument
     *            the reaction task argument
     */
    public static void TriggerPromiseReactions(ExecutionContext cx,
            List<PromiseReaction> reactions, Object argument) {
        Realm realm = cx.getRealm();
        for (PromiseReaction reaction : reactions) {
            realm.enqueuePromiseTask(new PromiseReactionTask(realm, reaction, argument));
        }
    }

    /**
     * <h2>25.4.2 Promise Jobs</h2>
     * <p>
     * 25.4.2.1 PromiseReactionJob( reaction, argument )
     */
    public static final class PromiseReactionTask implements Task {
        private final Realm realm;
        private final PromiseReaction reaction;
        private final Object argument;

        public PromiseReactionTask(Realm realm, PromiseReaction reaction, Object argument) {
            this.realm = realm;
            this.reaction = reaction;
            this.argument = argument;
        }

        @Override
        public void execute() {
            ExecutionContext cx = realm.defaultContext();
            /* step 1 (not applicable) */
            /* step 2 */
            PromiseCapability<?> promiseCapability = reaction.getCapabilities();
            /* steps 3-8 */
            Object handlerResult;
            if (reaction.getType() == PromiseReaction.Type.Identity) {
                /* steps 4, 8 */
                handlerResult = argument;
            } else if (reaction.getType() == PromiseReaction.Type.Thrower) {
                /* steps 5, 7 */
                promiseCapability.getReject().call(cx, UNDEFINED, argument);
                return;
            } else {
                /* step 3 */
                Callable handler = reaction.getHandler();
                /* steps 6-8 */
                try {
                    handlerResult = handler.call(cx, UNDEFINED, argument);
                } catch (ScriptException e) {
                    /* step 7 */
                    promiseCapability.getReject().call(cx, UNDEFINED, e.getValue());
                    return;
                }
            }
            /* steps 9-10 */
            promiseCapability.getResolve().call(cx, UNDEFINED, handlerResult);
        }
    }

    /**
     * <h2>25.4.2 Promise Jobs</h2>
     * <p>
     * 25.4.2.2 PromiseResolveThenableJob ( promiseToResolve, thenable, then )
     */
    public static final class PromiseResolveThenableTask implements Task {
        private final Realm realm;
        private final PromiseObject promise;
        private final ScriptObject thenable;
        private final Callable then;

        public PromiseResolveThenableTask(Realm realm, PromiseObject promise,
                ScriptObject thenable, Callable then) {
            assert promise.getState() != null : "Promise not initialized";
            this.realm = realm;
            this.promise = promise;
            this.thenable = thenable;
            this.then = then;
        }

        @Override
        public void execute() {
            ExecutionContext cx = realm.defaultContext();
            /* step 1 */
            ResolvingFunctions resolvingFunctions = CreateResolvingFunctions(cx, promise);
            /* steps 2-4 */
            try {
                /* step 2 */
                then.call(cx, thenable, resolvingFunctions.getResolve(),
                        resolvingFunctions.getReject());
            } catch (ScriptException e) {
                /* step 3 */
                resolvingFunctions.getReject().call(cx, UNDEFINED, e.getValue());
            }
        }
    }

    /**
     * PromiseNew ( executor ) Abstract Operation
     * 
     * @param cx
     *            the execution context
     * @param executor
     *            the executor function
     * @return the new promise object
     */
    public static PromiseObject PromiseNew(ExecutionContext cx, Callable executor) {
        /* step 1 */
        PromiseObject promise = AllocatePromise(cx,
                (Constructor) cx.getIntrinsic(Intrinsics.Promise));
        /* step 2 */
        return InitializePromise(cx, promise, executor);
    }

    /**
     * PromiseBuiltinCapability () Abstract Operation
     * 
     * @param cx
     *            the execution context
     * @return the promise capability record
     */
    public static PromiseCapability<PromiseObject> PromiseBuiltinCapability(ExecutionContext cx) {
        /* step 1 */
        PromiseObject promise = AllocatePromise(cx,
                (Constructor) cx.getIntrinsic(Intrinsics.Promise));
        /* step 2 */
        return CreatePromiseCapabilityRecord(cx, promise,
                (Constructor) cx.getIntrinsic(Intrinsics.Promise));
    }

    /**
     * PromiseOf (value) Abstract Operation
     * 
     * @param cx
     *            the execution context
     * @param value
     *            the resolved value
     * @return the new promise object
     */
    public static PromiseObject PromiseOf(ExecutionContext cx, Object value) {
        /* steps 1-2 */
        PromiseCapability<PromiseObject> capability = PromiseBuiltinCapability(cx);
        /* steps 3-4 */
        capability.getResolve().call(cx, UNDEFINED, value);
        /* step 5 */
        return capability.getPromise();
    }

    /**
     * PromiseOf (value) Abstract Operation
     * 
     * @param cx
     *            the execution context
     * @param e
     *            the exception object
     * @return the new promise object
     */
    public static PromiseObject PromiseOf(ExecutionContext cx, ScriptException e) {
        /* steps 1-2 */
        PromiseCapability<PromiseObject> capability = PromiseBuiltinCapability(cx);
        /* steps 3-4 */
        capability.getReject().call(cx, UNDEFINED, e.getValue());
        /* step 5 */
        return capability.getPromise();
    }
}
