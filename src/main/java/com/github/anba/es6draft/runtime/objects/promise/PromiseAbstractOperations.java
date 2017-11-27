/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.promise;

import static com.github.anba.es6draft.runtime.AbstractOperations.Get;
import static com.github.anba.es6draft.runtime.AbstractOperations.IsCallable;
import static com.github.anba.es6draft.runtime.AbstractOperations.IsConstructor;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.objects.zone.ZoneConstructor.CallInZone;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Job;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.objects.zone.ZoneObject;
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
 * <li>25.4.2 Promise Jobs
 * </ul>
 */
public final class PromiseAbstractOperations {
    private PromiseAbstractOperations() {
    }

    /**
     * Returns the Promise object allocator for the realm object.
     * 
     * @param realm
     *            the realm instance
     * @return the promise allocator
     */
    public static ObjectAllocator<? extends PromiseObject> GetPromiseAllocator(Realm realm) {
        if (realm.getRuntimeContext().isEnabled(CompatibilityOption.PromiseRejection)) {
            return FinalizablePromiseObject::new;
        }
        return PromiseObject::new;
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
     * 25.4.1.3 CreateResolvingFunctions ( promise )
     * 
     * @param cx
     *            the execution context
     * @param promise
     *            the promise object
     * @return the resolving functions tuple
     */
    public static ResolvingFunctions CreateResolvingFunctions(ExecutionContext cx, PromiseObject promise) {
        /* step 1 */
        AtomicBoolean alreadyResolved = new AtomicBoolean(false);
        /* steps 2-4 */
        PromiseResolveFunction resolve = new PromiseResolveFunction(cx.getRealm(), promise, alreadyResolved);
        /* steps 5-7 */
        PromiseRejectFunction reject = new PromiseRejectFunction(cx.getRealm(), promise, alreadyResolved);
        /* step 8 */
        return new ResolvingFunctions(resolve, reject);
    }

    /**
     * 25.4.1.3.1 Promise Reject Functions
     */
    public static final class PromiseRejectFunction extends BuiltinFunction {
        /** [[Promise]] */
        private final PromiseObject promise;
        /** [[AlreadyResolved]] */
        private final AtomicBoolean alreadyResolved;

        public PromiseRejectFunction(Realm realm, PromiseObject promise, AtomicBoolean alreadyResolved) {
            super(realm, ANONYMOUS, 1);
            this.promise = promise;
            this.alreadyResolved = alreadyResolved;
            createDefaultFunctionProperties();
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
     * 25.4.1.3.2 Promise Resolve Functions
     */
    public static final class PromiseResolveFunction extends BuiltinFunction {
        /** [[Promise]] */
        private final PromiseObject promise;
        /** [[AlreadyResolved]] */
        private final AtomicBoolean alreadyResolved;

        public PromiseResolveFunction(Realm realm, PromiseObject promise, AtomicBoolean alreadyResolved) {
            super(realm, ANONYMOUS, 1);
            this.promise = promise;
            this.alreadyResolved = alreadyResolved;
            createDefaultFunctionProperties();
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
            if (resolution == promise) { // SameValue
                ScriptException selfResolutionError = newTypeError(calleeContext, Messages.Key.PromiseSelfResolution);
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
            realm.enqueuePromiseJob(
                    new PromiseResolveThenableJob(realm, promise, Type.objectValue(resolution), (Callable) then));
            /* step 13 */
            return UNDEFINED;
        }
    }

    /**
     * <h2>25.4.1 Promise Abstract Operations</h2>
     * <p>
     * 25.4.1.4 FulfillPromise (promise, value)
     * 
     * @param cx
     *            the execution context
     * @param promise
     *            the promise object
     * @param value
     *            the resolve value
     */
    public static void FulfillPromise(ExecutionContext cx, PromiseObject promise, Object value) {
        /* steps 1-6 */
        List<PromiseReaction> reactions = promise.fufill(value);
        /* step 7 */
        TriggerPromiseReactions(cx, reactions, value);
    }

    /**
     * <h2>25.4.1 Promise Abstract Operations</h2>
     * <p>
     * 25.4.1.5 NewPromiseCapability ( C )
     * 
     * @param cx
     *            the execution context
     * @param c
     *            the promise constructor function
     * @return the new promise capability record
     */
    public static PromiseCapability<ScriptObject> NewPromiseCapability(ExecutionContext cx, ScriptObject c) {
        /* step 1 */
        if (!IsConstructor(c)) {
            throw newTypeError(cx, Messages.Key.NotConstructor);
        }
        /* steps 2-10 */
        return NewPromiseCapability(cx, (Constructor) c);
    }

    /**
     * <h2>25.4.1 Promise Abstract Operations</h2>
     * <p>
     * 25.4.1.5 NewPromiseCapability ( C )
     * 
     * @param cx
     *            the execution context
     * @param c
     *            the promise constructor function
     * @return the new promise capability record
     */
    public static PromiseCapability<ScriptObject> NewPromiseCapability(ExecutionContext cx, Constructor c) {
        /* steps 1-2 (not applicable) */
        /* step 3 (moved) */
        /* steps 4-5 */
        GetCapabilitiesExecutor executor = new GetCapabilitiesExecutor(cx.getRealm());
        /* step 6 */
        ScriptObject promise = c.construct(cx, executor);
        /* step 7 */
        Object resolve = executor.resolve;
        if (!IsCallable(resolve)) {
            throw newTypeError(cx, Messages.Key.NotCallable);
        }
        /* step 8 */
        Object reject = executor.reject;
        if (!IsCallable(reject)) {
            throw newTypeError(cx, Messages.Key.NotCallable);
        }
        /* steps 3, 9-10 */
        return new PromiseCapability<>(promise, (Callable) resolve, (Callable) reject);
    }

    /**
     * 25.4.1.5.1 GetCapabilitiesExecutor Functions
     */
    public static final class GetCapabilitiesExecutor extends BuiltinFunction {
        /** [[Resolve]] */
        private Object resolve;

        /** [[Reject]] */
        private Object reject;

        public GetCapabilitiesExecutor(Realm realm) {
            super(realm, ANONYMOUS, 2);
            this.resolve = UNDEFINED;
            this.reject = UNDEFINED;
            createDefaultFunctionProperties();
        }

        @Override
        public Undefined call(ExecutionContext callerContext, Object thisValue, Object... args) {
            ExecutionContext calleeContext = calleeContext();
            Object resolve = argument(args, 0);
            Object reject = argument(args, 1);
            /* step 1 (not applicable) */
            /* step 2 (omitted) */
            /* step 3 */
            if (!Type.isUndefined(this.resolve)) {
                throw newTypeError(calleeContext, Messages.Key.NotUndefined);
            }
            /* step 4 */
            if (!Type.isUndefined(this.reject)) {
                throw newTypeError(calleeContext, Messages.Key.NotUndefined);
            }
            /* step 5 */
            this.resolve = resolve;
            /* step 6 */
            this.reject = reject;
            /* step 7 */
            return UNDEFINED;
        }
    }

    /**
     * <h2>25.4.1 Promise Abstract Operations</h2>
     * <p>
     * 25.4.1.6 IsPromise ( x )
     * 
     * @param x
     *            the object
     * @return {@code true} if <var>x</var> is a promise object
     */
    public static boolean IsPromise(Object x) {
        /* steps 1-3 */
        return x instanceof PromiseObject;
    }

    /**
     * <h2>25.4.1 Promise Abstract Operations</h2>
     * <p>
     * 25.4.1.7 RejectPromise (promise, reason)
     * 
     * @param cx
     *            the execution context
     * @param promise
     *            the promise object
     * @param reason
     *            the rejection reason
     */
    public static void RejectPromise(ExecutionContext cx, PromiseObject promise, Object reason) {
        /* steps 1-6 */
        List<PromiseReaction> reactions = promise.reject(reason);
        /* step 7 (not applicable) */
        /* step 8 */
        TriggerPromiseReactions(cx, reactions, reason);
    }

    /**
     * <h2>25.4.1 Promise Abstract Operations</h2>
     * <p>
     * 25.4.1.8 TriggerPromiseReactions ( reactions, argument )
     * 
     * @param cx
     *            the execution context
     * @param reactions
     *            the list of promise reactions
     * @param argument
     *            the reaction job argument
     */
    public static void TriggerPromiseReactions(ExecutionContext cx, List<PromiseReaction> reactions, Object argument) {
        /* step 1 */
        Realm realm = cx.getRealm();
        for (PromiseReaction reaction : reactions) {
            realm.enqueuePromiseJob(new PromiseReactionJob(realm, reaction, argument));
        }
        /* step 2 (return) */
    }

    /**
     * <h2>25.4.2 Promise Jobs</h2>
     * <p>
     * 25.4.2.1 PromiseReactionJob( reaction, argument )
     */
    public static final class PromiseReactionJob implements Job {
        private final Realm realm;
        private final PromiseReaction reaction;
        private final Object argument;

        public PromiseReactionJob(Realm realm, PromiseReaction reaction, Object argument) {
            this.realm = realm;
            this.reaction = reaction;
            this.argument = argument;
        }

        @Override
        public void execute() {
            ExecutionContext cx = realm.defaultContext();
            /* step 1 (not applicable) */
            /* step 2 */
            PromiseCapability<?> promiseCapability = reaction.getCapability();
            /* step 3 */
            PromiseReaction.Type type = reaction.getType();
            /* step 4 */
            Callable handler = reaction.getHandler();
            /* steps 5-8 */
            if (handler == null) {
                if (type == PromiseReaction.Type.Fulfill) {
                    /* steps 5.a, 8 */
                    promiseCapability.getResolve().call(cx, UNDEFINED, argument);
                } else {
                    assert type == PromiseReaction.Type.Reject;
                    /* steps 5.b, 7 */
                    promiseCapability.getReject().call(cx, UNDEFINED, argument);
                }
            } else {
                /* steps 6-7 */
                Object handlerResult;
                try {
                    ZoneObject zone = reaction.getZone();
                    if (zone == null) {
                        handlerResult = handler.call(cx, UNDEFINED, argument);
                    } else {
                        handlerResult = CallInZone(cx, zone, handler, UNDEFINED, argument);
                    }
                } catch (ScriptException e) {
                    /* step 7 */
                    promiseCapability.getReject().call(cx, UNDEFINED, e.getValue());
                    return;
                }
                /* step 8 */
                promiseCapability.getResolve().call(cx, UNDEFINED, handlerResult);
            }
            /* step 9 (return) */
        }
    }

    /**
     * PromiseResolve ( C, x )
     * 
     * @param cx
     *            the execution context
     * @param x
     *            the resolved value
     * @return the new promise object
     */
    public static PromiseObject BuiltinPromiseResolve(ExecutionContext cx, Object x) {
        /* step 1 (implicit) */
        /* step 2 */
        PromiseCapability<PromiseObject> promiseCapability = PromiseBuiltinCapability(cx);
        /* step 3 */
        promiseCapability.getResolve().call(cx, UNDEFINED, x);
        /* step 4 */
        return promiseCapability.getPromise();
    }

    /**
     * <h2>25.4.2 Promise Jobs</h2>
     * <p>
     * 25.4.2.2 PromiseResolveThenableJob ( promiseToResolve, thenable, then )
     */
    public static final class PromiseResolveThenableJob implements Job {
        private final Realm realm;
        private final PromiseObject promise;
        private final ScriptObject thenable;
        private final Callable then;

        public PromiseResolveThenableJob(Realm realm, PromiseObject promise, ScriptObject thenable, Callable then) {
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
                then.call(cx, thenable, resolvingFunctions.getResolve(), resolvingFunctions.getReject());
            } catch (ScriptException e) {
                /* step 3 */
                resolvingFunctions.getReject().call(cx, UNDEFINED, e.getValue());
            }
        }
    }

    /**
     * PromiseBuiltinCapability ()
     * 
     * @param cx
     *            the execution context
     * @return the promise capability record
     */
    @SuppressWarnings("unchecked")
    public static PromiseCapability<PromiseObject> PromiseBuiltinCapability(ExecutionContext cx) {
        PromiseConstructor constructor = (PromiseConstructor) cx.getIntrinsic(Intrinsics.Promise);
        PromiseCapability<ScriptObject> capability = NewPromiseCapability(cx, constructor);
        assert capability.getPromise() instanceof PromiseObject;
        return (PromiseCapability<PromiseObject>) (PromiseCapability<?>) capability;
    }

    /**
     * PromiseOf (value)
     * 
     * @param cx
     *            the execution context
     * @param value
     *            the resolved value
     * @return the new promise object
     */
    public static PromiseObject PromiseOf(ExecutionContext cx, Object value) {
        PromiseCapability<PromiseObject> capability = PromiseBuiltinCapability(cx);
        capability.getResolve().call(cx, UNDEFINED, value);
        return capability.getPromise();
    }

    /**
     * PromiseOf (value)
     * 
     * @param cx
     *            the execution context
     * @param e
     *            the exception object
     * @return the new promise object
     */
    public static PromiseObject PromiseOf(ExecutionContext cx, ScriptException e) {
        PromiseCapability<PromiseObject> capability = PromiseBuiltinCapability(cx);
        capability.getReject().call(cx, UNDEFINED, e.getValue());
        return capability.getPromise();
    }
}
