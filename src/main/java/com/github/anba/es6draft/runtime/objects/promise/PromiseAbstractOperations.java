/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.promise;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Errors.throwTypeError;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.OrdinaryConstruct;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Errors;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Microtask;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinFunction;
import com.github.anba.es6draft.runtime.types.builtins.FunctionObject;

/**
 * <h1>Promise Objects</h1><br>
 * <ul>
 * <li>Abstract Operations for Promise Objects
 * <li>Built-in Functions for Promise Objects
 * <li>Microtasks for Promise Objects
 * </ul>
 */
public final class PromiseAbstractOperations {
    private PromiseAbstractOperations() {
    }

    private static ScriptException newTypeError(ExecutionContext cx, Messages.Key key) {
        return Errors.newError(cx, Intrinsics.TypeError, key);
    }

    /**
     * RejectIfAbrupt
     */
    public static ScriptObject RejectIfAbrupt(ExecutionContext cx, ScriptException e,
            Deferred deferred) {
        /* step 1.i-ii */
        deferred.getReject().call(cx, UNDEFINED, e.getValue());
        /* step 1.iii */
        return deferred.getPromise();
    }

    /**
     * QueueMicrotask ( microtask, argumentsList )
     */
    public static void QueueMicrotask(ExecutionContext cx, Microtask task) {
        cx.getRealm().getWorld().enqueueTask(task);
    }

    /**
     * ThenableCoercionsGet ( realm, thenable )
     */
    public static ScriptObject ThenableCoercionsGet(Realm realm, ScriptObject thenable) {
        return thenable.thenableCoercionsGet(realm);
    }

    /**
     * ThenableCoercionsSet ( realm, thenable, promise )
     */
    public static void ThenableCoercionsSet(Realm realm, ScriptObject thenable, ScriptObject promise) {
        thenable.thenableCoercionsSet(realm, promise);
    }

    /**
     * <h2>Modules</h2>
     * <p>
     * PromiseCreate ( f )
     */
    public static ScriptObject PromiseCreate(ExecutionContext cx, Callable f) {
        // TODO: make safe
        ScriptObject p = OrdinaryConstruct(cx,
                (PromiseConstructor) cx.getIntrinsic(Intrinsics.Promise), new Object[] { f });
        return p;
    }

    /**
     * <h2>Modules</h2>
     * <p>
     * PromiseThen ( promise, onFulfilled )
     */
    public static ScriptObject PromiseThen(ExecutionContext cx, ScriptObject promise,
            Callable onFulfilled) {
        // TODO: make safe
        Object p = PromisePrototype.Properties.then(cx, promise, onFulfilled, UNDEFINED);
        assert p instanceof ScriptObject;
        return (ScriptObject) p;
    }

    /**
     * <h2>Modules</h2>
     * <p>
     * PromiseThen ( promise, onFulfilled, onRejected )
     */
    public static ScriptObject PromiseThen(ExecutionContext cx, ScriptObject promise,
            Callable onFulfilled, Callable onRejected) {
        // TODO: make safe
        Object p = PromisePrototype.Properties.then(cx, promise, onFulfilled, onRejected);
        assert p instanceof ScriptObject;
        return (ScriptObject) p;
    }

    /**
     * <h2>Modules</h2>
     * <p>
     * PromiseCatch ( promise, onRejected )
     */
    public static ScriptObject PromiseCatch(ExecutionContext cx, ScriptObject promise,
            Callable onRejected) {
        // TODO: make safe
        Object p = PromisePrototype.Properties._catch(cx, promise, onRejected);
        assert p instanceof ScriptObject;
        return (ScriptObject) p;
    }

    /**
     * <h2>Modules</h2>
     * <p>
     * PromiseResolve ( x )
     */
    public static ScriptObject PromiseResolve(ExecutionContext cx, Object x) {
        // TODO: make safe
        ScriptObject promiseConstructor = cx.getIntrinsic(Intrinsics.Promise);
        Object p = PromiseConstructor.Properties.resolve(cx, promiseConstructor, x);
        assert p instanceof ScriptObject;
        return (ScriptObject) p;
    }

    /**
     * <h2>Modules</h2>
     * <p>
     * PromiseAll ( x )
     */
    public static ScriptObject PromiseAll(ExecutionContext cx, List<ScriptObject> list) {
        // TODO: make safe
        ScriptObject promiseConstructor = cx.getIntrinsic(Intrinsics.Promise);
        ScriptObject iterator = CreateListIterator(cx, list);
        // Promote Iterator to Iterable
        CreateDataProperty(cx, iterator, BuiltinSymbol.iterator.get(),
                new ConstantFunction(cx.getRealm(), iterator));
        Object p = PromiseConstructor.Properties.all(cx, promiseConstructor, iterator);
        assert p instanceof ScriptObject;
        return (ScriptObject) p;
    }

    private static class ConstantFunction extends BuiltinFunction {
        /** [[ConstantValue]] */
        private final Object constantValue;

        public ConstantFunction(Realm realm, Object constantValue) {
            super(realm, ANONYMOUS, 0);
            this.constantValue = constantValue;
        }

        @Override
        public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
            return constantValue;
        }
    }

    /**
     * <h2>Abstract Operations for Promise Objects</h2>
     * <p>
     * GetDeferred ( C )
     */
    public static Deferred GetDeferred(ExecutionContext cx, Object c) {
        /* step 1 */
        if (!IsConstructor(c)) {
            throw throwTypeError(cx, Messages.Key.NotConstructor);
        }
        /* step 2 */
        Deferred deferred = new Deferred();
        /* steps 3-4 */
        DeferredConstructionFunction resolver = new DeferredConstructionFunction(cx.getRealm(),
                deferred);
        /* steps 5-6 */
        ScriptObject promise = ((Constructor) c).construct(cx, resolver);
        /* step 7 */
        if (deferred.getResolve() == null) {
            throw throwTypeError(cx, Messages.Key.NotCallable);
        }
        /* step 8 */
        if (deferred.getReject() == null) {
            throw throwTypeError(cx, Messages.Key.NotCallable);
        }
        /* step 9 */
        deferred.setPromise(promise);
        /* step 10 */
        return deferred;
    }

    /**
     * <h2>Abstract Operations for Promise Objects</h2>
     * <p>
     * IsPromise ( x )
     */
    public static boolean IsPromise(Object x) {
        /* step 1 */
        if (!Type.isObject(x)) {
            return false;
        }
        /* step 2 */
        if (!(x instanceof PromiseObject)) {
            return false;
        }
        /* step 3 */
        if (((PromiseObject) x).getStatus() == null) {
            return false;
        }
        /* step 4 */
        return true;
    }

    /**
     * <h2>Abstract Operations for Promise Objects</h2>
     * <p>
     * PromiseReject ( promise, reason )
     */
    public static void PromiseReject(ExecutionContext cx, PromiseObject promise, Object reason) {
        /* step 1 */
        if (promise.getStatus() != PromiseObject.Status.Unresolved) {
            return;
        }
        /* step 2-6 */
        List<PromiseReaction> reactions = promise.reject(reason);
        /* step 7 */
        TriggerPromiseReactions(cx, reactions, reason);
    }

    /**
     * <h2>Abstract Operations for Promise Objects</h2>
     * <p>
     * PromiseResolve ( promise, resolution )
     */
    public static void PromiseResolve(ExecutionContext cx, PromiseObject promise, Object resolution) {
        /* step 1 */
        if (promise.getStatus() != PromiseObject.Status.Unresolved) {
            return;
        }
        /* steps 2-6 */
        List<PromiseReaction> reactions = promise.resolve(resolution);
        /* step 7 */
        TriggerPromiseReactions(cx, reactions, resolution);
    }

    /**
     * <h2>Abstract Operations for Promise Objects</h2>
     * <p>
     * TriggerPromiseReactions ( reactions, argument )
     */
    public static void TriggerPromiseReactions(ExecutionContext cx,
            List<PromiseReaction> reactions, Object argument) {
        for (PromiseReaction reaction : reactions) {
            QueueMicrotask(cx, new ExecutePromiseReaction(reaction, argument));
        }
    }

    /**
     * <h2>Abstract Operations for Promise Objects</h2>
     * <p>
     * UpdateDeferredFromPotentialThenable ( x, deferred, realm )
     */
    public static boolean UpdateDeferredFromPotentialThenable(ExecutionContext cx, Object x,
            Deferred deferred, Realm realm) {
        /* step 1 */
        if (!Type.isObject(x)) {
            return false; /* "not a thenable" */
        }
        ScriptObject thenable = Type.objectValue(x);
        /* step 2 */
        ScriptObject coercedAlready = ThenableCoercionsGet(realm, thenable);
        /* step 3 */
        if (coercedAlready != null) {
            Invoke(cx, coercedAlready, "then", deferred.getResolve(), deferred.getReject());
            return true;
        }
        /* step 4, 6 */
        Object then;
        try {
            then = Get(cx, thenable, "then");
        } catch (ScriptException e) {
            /* step 5 */
            deferred.getReject().call(cx, UNDEFINED, e.getValue());
            return true;
        }
        /* step 7 */
        if (!IsCallable(then)) {
            return false; /* "not a thenable" */
        }
        /* step 8 */
        ThenableCoercionsSet(realm, thenable, deferred.getPromise());
        /* step 9 */
        try {
            ((Callable) then).call(cx, thenable, deferred.getResolve(), deferred.getReject());
        } catch (ScriptException e) {
            /* step 10 */
            deferred.getReject().call(cx, UNDEFINED, e.getValue());
        }
        return true;
    }

    /**
     * <h2>Built-in Functions for Promise Objects</h2>
     * <p>
     * Deferred Construction Functions
     */
    public static final class DeferredConstructionFunction extends BuiltinFunction {
        /** [[Deferred]] */
        private final Deferred deferred;

        public DeferredConstructionFunction(Realm realm, Deferred deferred) {
            super(realm, ANONYMOUS, 2);
            this.deferred = deferred;
        }

        @Override
        public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
            Object resolve = args.length > 0 ? args[0] : UNDEFINED;
            Object reject = args.length > 1 ? args[1] : UNDEFINED;
            /* step 1 */
            Deferred deferred = this.deferred;
            /* step 2 */
            if (IsCallable(resolve)) {
                deferred.setResolve((Callable) resolve);
            } else {
                deferred.setResolve(null);
            }
            /* step 3 */
            if (IsCallable(reject)) {
                deferred.setReject((Callable) reject);
            } else {
                deferred.setReject(null);
            }
            /* step 4 */
            return UNDEFINED;
        }
    }

    /**
     * <h2>Built-in Functions for Promise Objects</h2>
     * <p>
     * Identity Functions
     */
    public static final class IdentityFunction extends BuiltinFunction {
        public IdentityFunction(Realm realm) {
            super(realm, ANONYMOUS, 1);
        }

        @Override
        public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
            Object x = args.length > 0 ? args[0] : UNDEFINED;
            /* step 1 */
            return x;
        }
    }

    /**
     * <h2>Built-in Functions for Promise Objects</h2>
     * <p>
     * Promise.all Countdown Functions
     */
    public static final class PromiseAllCountdownFunction extends BuiltinFunction {
        /** [[Index]] */
        private final int index;

        /** [[Values]] */
        private final ScriptObject values;

        /** [[Deferred]] */
        private final Deferred deferred;

        /** [[CountdownHolder]] */
        private final AtomicInteger countdownHolder;

        public PromiseAllCountdownFunction(Realm realm, int index, ScriptObject values,
                Deferred deferred, AtomicInteger countdownHolder) {
            super(realm, ANONYMOUS, 1);
            this.index = index;
            this.values = values;
            this.deferred = deferred;
            this.countdownHolder = countdownHolder;
        }

        @Override
        public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
            ExecutionContext calleeContext = calleeContext();
            Object x = args.length > 0 ? args[0] : UNDEFINED;
            /* step 1 */
            int index = this.index;
            /* step 2 */
            ScriptObject values = this.values;
            /* step 3 */
            Deferred deferred = this.deferred;
            /* step 4 */
            AtomicInteger countdownHolder = this.countdownHolder;
            /* step 5 */
            try {
                CreateDataProperty(calleeContext, values, ToString(index), x);
            } catch (ScriptException e) {
                /* step 6 */
                return RejectIfAbrupt(calleeContext, e, deferred);
            }
            /* steps 7-8 */
            if (countdownHolder.decrementAndGet() == 0) {
                return deferred.getResolve().call(calleeContext, UNDEFINED, values);
            }
            /* step 9 */
            // FIXME: missing in spec
            return UNDEFINED;
        }
    }

    /**
     * <h2>Built-in Functions for Promise Objects</h2>
     * <p>
     * Promise Resolution Handler Functions
     */
    public static final class PromiseResolutionHandlerFunction extends BuiltinFunction {
        /** [[Promise]] */
        private final PromiseObject promise;

        /** [[FulfillmentHandler]] */
        private final Callable fulfillmentHandler;

        /** [[RejectionHandler]] */
        private final Callable rejectionHandler;

        public PromiseResolutionHandlerFunction(Realm realm, PromiseObject promise,
                Callable fulfillmentHandler, Callable rejectionHandler) {
            super(realm, ANONYMOUS, 1);
            this.promise = promise;
            this.fulfillmentHandler = fulfillmentHandler;
            this.rejectionHandler = rejectionHandler;
        }

        @Override
        public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
            ExecutionContext calleeContext = calleeContext();
            Object x = args.length > 0 ? args[0] : UNDEFINED;
            /* step 1 */
            PromiseObject promise = this.promise;
            /* step 2 */
            Callable fulfillmentHandler = this.fulfillmentHandler;
            /* step 3 */
            Callable rejectionHandler = this.rejectionHandler;
            /* step 4 */
            Realm realm = getRealm();
            /* step 5 */
            if (SameValue(x, promise)) {
                ScriptException selfResolutionError = newTypeError(calleeContext,
                        Messages.Key.PromiseSelfResolution);
                return rejectionHandler.call(calleeContext, UNDEFINED,
                        selfResolutionError.getValue());
            }
            /* step 6 */
            Constructor c = promise.getConstructor();
            /* step 7 */
            if (IsPromise(x)) {
                PromiseObject xPromise = (PromiseObject) x;
                Constructor xConstructor = xPromise.getConstructor();
                if (SameValue(xConstructor, c)) {
                    return Invoke(calleeContext, xPromise, "then", fulfillmentHandler,
                            rejectionHandler);
                }
            }
            /* step 8 */
            if (Type.isObject(x)) {
                ScriptObject coercedAlready = ThenableCoercionsGet(realm, Type.objectValue(x));
                if (coercedAlready != null) {
                    return Invoke(calleeContext, coercedAlready, "then", fulfillmentHandler,
                            rejectionHandler);
                }
            }
            /* steps 9-10 */
            Deferred deferred = GetDeferred(calleeContext, c);
            /* steps 11-12 */
            boolean updateResult = UpdateDeferredFromPotentialThenable(calleeContext, x, deferred,
                    realm);
            /* step 13 */
            if (updateResult) {
                return Invoke(calleeContext, deferred.getPromise(), "then", fulfillmentHandler,
                        rejectionHandler);
            }
            /* step 14 */
            return fulfillmentHandler.call(calleeContext, UNDEFINED, x);
        }
    }

    /**
     * <h2>Built-in Functions for Promise Objects</h2>
     * <p>
     * Reject Promise Functions
     */
    public static final class RejectPromiseFunction extends BuiltinFunction {
        /** [[Promise]] */
        private final PromiseObject promise;

        public RejectPromiseFunction(Realm realm, PromiseObject promise) {
            super(realm, ANONYMOUS, 1);
            this.promise = promise;
        }

        @Override
        public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
            ExecutionContext calleeContext = calleeContext();
            Object reason = args.length > 0 ? args[0] : UNDEFINED;
            /* step 1 */
            PromiseObject promise = this.promise;
            /* step 2 */
            PromiseReject(calleeContext, promise, reason);
            // FIXME: missing in spec, PromiseReject does not return a completion value
            return UNDEFINED;
        }
    }

    /**
     * <h2>Built-in Functions for Promise Objects</h2>
     * <p>
     * Resolve Promise Functions
     */
    public static final class ResolvePromiseFunction extends BuiltinFunction {
        /** [[Promise]] */
        private final PromiseObject promise;

        public ResolvePromiseFunction(Realm realm, PromiseObject promise) {
            super(realm, ANONYMOUS, 1);
            this.promise = promise;
        }

        @Override
        public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
            ExecutionContext calleeContext = calleeContext();
            Object resolution = args.length > 0 ? args[0] : UNDEFINED;
            /* step 1 */
            PromiseObject promise = this.promise;
            /* step 2 */
            PromiseResolve(calleeContext, promise, resolution);
            // FIXME: missing in spec, PromiseResolve does not return a completion value
            return UNDEFINED;
        }
    }

    /**
     * <h2>Built-in Functions for Promise Objects</h2>
     * <p>
     * Thrower Functions
     */
    public static final class ThrowerFunction extends BuiltinFunction {
        public ThrowerFunction(Realm realm) {
            super(realm, ANONYMOUS, 1);
        }

        @Override
        public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
            Object e = args.length > 0 ? args[0] : UNDEFINED;
            /* step 1 */
            throw ScriptException.create(e);
        }
    }

    /**
     * <h2>Microtasks for Promise Objects</h2>
     * <p>
     * Microtask ExecutePromiseReaction( reaction, argument )
     */
    public static final class ExecutePromiseReaction implements Microtask {
        private final PromiseReaction reaction;
        private final Object argument;

        public ExecutePromiseReaction(PromiseReaction reaction, Object argument) {
            this.reaction = reaction;
            this.argument = argument;
        }

        @Override
        public void execute(ExecutionContext cx) {
            // FIXME: current execution context for micro-tasks?
            /* step 1 */
            Deferred deferred = reaction.getDeferred();
            /* step 2 */
            Callable handler = reaction.getHandler();
            /* step 3 */
            // FIXME: spec bug [[Realm]] not mandatory property for Callable objects
            // Realm realm = handler.getRealm();
            Realm realm;
            if (handler instanceof FunctionObject) {
                realm = ((FunctionObject) handler).getRealm();
            } else if (handler instanceof BuiltinFunction) {
                realm = ((BuiltinFunction) handler).getRealm();
            } else {
                realm = cx.getRealm();
            }
            /* step 4, 6 */
            Object handlerResult;
            try {
                handlerResult = handler.call(cx, UNDEFINED, argument);
            } catch (ScriptException e) {
                /* step 5 */
                deferred.getReject().call(cx, UNDEFINED, e.getValue());
                return;
            }
            /* step 7 */
            if (SameValue(handlerResult, deferred.getPromise())) {
                ScriptException selfResolutionError = newTypeError(cx,
                        Messages.Key.PromiseSelfResolution);
                deferred.getReject().call(cx, UNDEFINED, selfResolutionError.getValue());
                return;
            }
            /* steps 8-9 */
            boolean updateResult = UpdateDeferredFromPotentialThenable(cx, handlerResult, deferred,
                    realm);
            /* step 10 */
            if (!updateResult) {
                deferred.getResolve().call(cx, UNDEFINED, handlerResult);
            }
        }
    }
}
