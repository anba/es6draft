/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.promise;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.util.List;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.Task;
import com.github.anba.es6draft.runtime.World;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
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

    /**
     * <h2>25.4.1 Promise Abstract Operations</h2>
     * <p>
     * 25.4.1.3 CreateRejectFunction ( promise )
     */
    public static PromiseRejectFunction CreateRejectFunction(ExecutionContext cx,
            PromiseObject promise) {
        /* steps 1-3 */
        return new PromiseRejectFunction(cx.getRealm(), promise);
    }

    /**
     * 25.4.1.3.1 Promise Reject Functions
     */
    public static final class PromiseRejectFunction extends BuiltinFunction {
        /** [[Promise]] */
        private final PromiseObject promise;

        public PromiseRejectFunction(Realm realm, PromiseObject promise) {
            super(realm, ANONYMOUS, 1);
            this.promise = promise;
        }

        @Override
        public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
            ExecutionContext calleeContext = calleeContext();
            Object reason = args.length > 0 ? args[0] : UNDEFINED;
            /* step 1 (not applicable) */
            /* step 2 */
            PromiseObject promise = this.promise;
            /* step 3 */
            if (promise.getStatus() != PromiseObject.Status.Unresolved) {
                return UNDEFINED;
            }
            /* steps 4-8 */
            List<PromiseReaction> reactions = promise.reject(reason);
            /* step 9 */
            TriggerPromiseReactions(calleeContext, reactions, reason);
            return UNDEFINED;
        }
    }

    /**
     * <h2>25.4.1 Promise Abstract Operations</h2>
     * <p>
     * 25.4.1.4 CreateResolveFunction ( promise )
     */
    public static PromiseResolveFunction CreateResolveFunction(ExecutionContext cx,
            PromiseObject promise) {
        /* steps 1-3 */
        return new PromiseResolveFunction(cx.getRealm(), promise);
    }

    /**
     * 25.4.1.4.1 Promise Resolve Functions
     */
    public static final class PromiseResolveFunction extends BuiltinFunction {
        /** [[Promise]] */
        private final PromiseObject promise;

        public PromiseResolveFunction(Realm realm, PromiseObject promise) {
            super(realm, ANONYMOUS, 1);
            this.promise = promise;
        }

        @Override
        public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
            ExecutionContext calleeContext = calleeContext();
            Object resolution = args.length > 0 ? args[0] : UNDEFINED;
            /* step 1 (not applicable) */
            /* step 2 */
            PromiseObject promise = this.promise;
            /* step 3 */
            if (promise.getStatus() != PromiseObject.Status.Unresolved) {
                return UNDEFINED;
            }
            /* steps 4-8 */
            List<PromiseReaction> reactions = promise.resolve(resolution);
            /* step 9 */
            TriggerPromiseReactions(calleeContext, reactions, resolution);
            return UNDEFINED;
        }
    }

    /**
     * <h2>25.4.1 Promise Abstract Operations</h2>
     * <p>
     * 25.4.1.5 NewPromiseCapability ( C )
     */
    public static PromiseCapability NewPromiseCapability(ExecutionContext cx, Object c) {
        /* step 1 */
        if (!IsConstructor(c)) {
            throw newTypeError(cx, Messages.Key.NotConstructor);
        }
        Constructor constructor = (Constructor) c;
        /* step 2 (not applicable) */
        /* steps 3-4 */
        ScriptObject promise = CreateFromConstructor(cx, constructor);
        // FIXME: spec bug -> throw TypeError if Type(promise)=Undefined
        if (promise == null) {
            throw newTypeError(cx, Messages.Key.NotObjectType);
        }
        /* step 5 */
        return CreatePromiseCapabilityRecord(cx, promise, constructor);
    }

    /**
     * 25.4.1.5.1 CreatePromiseCapabilityRecord( promise, constructor ) Abstract Operation
     */
    public static PromiseCapability CreatePromiseCapabilityRecord(ExecutionContext cx,
            ScriptObject promise, Constructor constructor) {
        /* steps 2-3 */
        GetCapabilitiesExecutor executor = new GetCapabilitiesExecutor(cx.getRealm());
        /* steps 4-5 */
        Object constructorResult = constructor.call(cx, promise, executor);
        /* step 6 */
        Object resolve = executor.resolve;
        if (!IsCallable(resolve)) {
            throw newTypeError(cx, Messages.Key.NotCallable);
        }
        /* step 7 */
        Object reject = executor.reject;
        if (!IsCallable(reject)) {
            throw newTypeError(cx, Messages.Key.NotCallable);
        }
        /* step 8 */
        if (Type.isObject(constructorResult) && !SameValue(promise, constructorResult)) {
            throw newTypeError(cx, Messages.Key.IncompatibleObject);
        }
        /* steps 1, 9 */
        return new PromiseCapability(promise, (Callable) resolve, (Callable) reject);
    }

    /**
     * 25.4.1.5.2 GetCapabilitiesExecutor Functions
     */
    public static final class GetCapabilitiesExecutor extends BuiltinFunction {
        /** [[Resolve]] */
        private Object resolve = UNDEFINED;

        /** [[Reject]] */
        private Object reject = UNDEFINED;

        public GetCapabilitiesExecutor(Realm realm) {
            super(realm, ANONYMOUS, 2);
        }

        @Override
        public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
            ExecutionContext calleeContext = calleeContext();
            Object resolve = args.length > 0 ? args[0] : UNDEFINED;
            Object reject = args.length > 1 ? args[1] : UNDEFINED;
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
     * <h2>25.4.1 Promise Abstract Operations</h2>
     * <p>
     * 25.4.1.7 TriggerPromiseReactions ( reactions, argument )
     */
    public static void TriggerPromiseReactions(ExecutionContext cx,
            List<PromiseReaction> reactions, Object argument) {
        Realm realm = cx.getRealm();
        World<?> world = realm.getWorld();
        for (PromiseReaction reaction : reactions) {
            world.enqueuePromiseTask(new PromiseReactionTask(realm, reaction, argument));
        }
    }

    /**
     * <h2>25.4.1 Promise Abstract Operations</h2>
     * <p>
     * 25.4.1.8 UpdatePromiseFromPotentialThenable ( x, promiseCapability)
     */
    public static boolean UpdatePromiseFromPotentialThenable(ExecutionContext cx, Object x,
            PromiseCapability promiseCapability) {
        /* step 1 */
        if (!Type.isObject(x)) {
            return false; /* "not a thenable" */
        }
        ScriptObject thenable = Type.objectValue(x);
        /* steps 2-4 */
        Object then;
        try {
            then = Get(cx, thenable, "then");
        } catch (ScriptException e) {
            /* step 3 */
            promiseCapability.getReject().call(cx, UNDEFINED, e.getValue());
            return true;
        }
        /* step 5 */
        if (!IsCallable(then)) {
            return false; /* "not a thenable" */
        }
        /* steps 6-7 */
        try {
            ((Callable) then).call(cx, thenable, promiseCapability.getResolve(),
                    promiseCapability.getReject());
        } catch (ScriptException e) {
            /* step 7 */
            promiseCapability.getReject().call(cx, UNDEFINED, e.getValue());
        }
        /* step 8 */
        return true;
    }

    /**
     * <h2>25.4.2 Promise Tasks</h2>
     * <p>
     * 25.4.2.1 PromiseReactionTask( reaction, argument )
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
            PromiseCapability promiseCapability = reaction.getCapabilities();
            /* step 3 */
            Callable handler = reaction.getHandler();
            /* steps 4-6 */
            Object handlerResult;
            try {
                handlerResult = handler.call(cx, UNDEFINED, argument);
            } catch (ScriptException e) {
                /* step 5 */
                promiseCapability.getReject().call(cx, UNDEFINED, e.getValue());
                return;
            }
            /* step 7 */
            if (SameValue(handlerResult, promiseCapability.getPromise())) {
                ScriptException selfResolutionError = newTypeError(cx,
                        Messages.Key.PromiseSelfResolution);
                promiseCapability.getReject().call(cx, UNDEFINED, selfResolutionError.getValue());
                return;
            }
            /* steps 8-10 */
            boolean updateResult = UpdatePromiseFromPotentialThenable(cx, handlerResult,
                    promiseCapability);
            /* step 11 */
            if (!updateResult) {
                promiseCapability.getResolve().call(cx, UNDEFINED, handlerResult);
            }
            /* step 12 (return) */
        }
    }

    /* ***************************************************************************************** */

    /**
     * <h2>Modules</h2>
     * <p>
     * PromiseCreate ( f )
     */
    public static ScriptObject PromiseCreate(ExecutionContext cx, Callable f) {
        // TODO: make safe
        ScriptObject p = Construct(cx, (PromiseConstructor) cx.getIntrinsic(Intrinsics.Promise),
                new Object[] { f });
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
}
