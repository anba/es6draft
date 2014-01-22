/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.promise;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Errors.newInternalError;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.promise.PromiseAbstractOperations.GetDeferred;
import static com.github.anba.es6draft.runtime.objects.promise.PromiseAbstractOperations.IsPromise;
import static com.github.anba.es6draft.runtime.objects.promise.PromiseAbstractOperations.PromiseReject;
import static com.github.anba.es6draft.runtime.objects.promise.PromiseAbstractOperations.RejectIfAbrupt;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.ExoticArray.ArrayCreate;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.AddRestrictedFunctionProperties;

import java.util.concurrent.atomic.AtomicInteger;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.objects.promise.PromiseAbstractOperations.PromiseAllCountdownFunction;
import com.github.anba.es6draft.runtime.objects.promise.PromiseAbstractOperations.RejectPromiseFunction;
import com.github.anba.es6draft.runtime.objects.promise.PromiseAbstractOperations.ResolvePromiseFunction;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinConstructor;

/**
 * <h1>Promise Objects</h1><br>
 * <ul>
 * <li>The Promise Constructor
 * <li>Properties of the Promise Constructor
 * </ul>
 */
public class PromiseConstructor extends BuiltinConstructor implements Initialisable {
    public PromiseConstructor(Realm realm) {
        super(realm, "Promise");
    }

    @Override
    public void initialise(ExecutionContext cx) {
        createProperties(this, cx, Properties.class);
        AddRestrictedFunctionProperties(cx, this);
    }

    @Override
    public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
        ExecutionContext calleeContext = calleeContext();
        Realm realm = calleeContext.getRealm();
        Object resolver = args.length > 0 ? args[0] : UNDEFINED;
        /* steps 2-3 */
        if (!(thisValue instanceof PromiseObject)) {
            throw newTypeError(calleeContext, Messages.Key.IncompatibleObject);
        }
        /* step 1 */
        PromiseObject promise = (PromiseObject) thisValue;
        /* step 4 */
        if (promise.getStatus() != null) {
            throw newTypeError(calleeContext, Messages.Key.InitialisedObject);
        }
        /* step 5 */
        if (!IsCallable(resolver)) {
            throw newTypeError(calleeContext, Messages.Key.NotCallable);
        }
        /* steps 6-8 */
        promise.initialise();
        /* steps 9-10 */
        ResolvePromiseFunction resolve = new ResolvePromiseFunction(realm, promise);
        /* steps 11-12 */
        RejectPromiseFunction reject = new RejectPromiseFunction(realm, promise);
        /* step 13 */
        try {
            ((Callable) resolver).call(calleeContext, UNDEFINED, resolve, reject);
        } catch (ScriptException e) {
            /* step 14 */
            PromiseReject(calleeContext, promise, e.getValue());
        }
        /* step 15 */
        return promise;
    }

    /**
     * new Promise (... argumentsList)
     */
    @Override
    public ScriptObject construct(ExecutionContext callerContext, Object... args) {
        /* steps 1-3 */
        return Construct(callerContext, this, args);
    }

    /**
     * Properties of the Promise Constructor
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.FunctionPrototype;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false,
                configurable = true))
        public static final int length = 1;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false,
                configurable = true))
        public static final String name = "Promise";

        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.PromisePrototype;

        /**
         * Promise [ @@create ] ( )
         */
        @Function(name = "[Symbol.create]", symbol = BuiltinSymbol.create, arity = 0,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static Object create(ExecutionContext cx, Object thisValue) {
            /* step 1 */
            Object f = thisValue;
            /* step 2 */
            PromiseObject obj = OrdinaryCreateFromConstructor(cx, f, Intrinsics.PromisePrototype,
                    PromiseObjectAllocator.INSTANCE);
            /* step 3 */
            assert IsConstructor(f);
            obj.setConstructor((Constructor) f);
            /* step 4 */
            return obj;
        }

        /**
         * Promise.all ( iterable )
         */
        @Function(name = "all", arity = 1)
        public static Object all(ExecutionContext cx, Object thisValue, Object iterable) {
            /* step 1 */
            Object c = thisValue;
            /* steps 2-3 */
            Deferred deferred = GetDeferred(cx, c);
            /* step 4 */
            ScriptObject iterator;
            try {
                iterator = GetIterator(cx, iterable);
            } catch (ScriptException e) {
                /* step 5 */
                return RejectIfAbrupt(cx, e, deferred);
            }
            /* step 6 */
            ScriptObject values = ArrayCreate(cx, 0);
            /* step 7 */
            AtomicInteger countdownHolder = new AtomicInteger(0);
            /* steps 8-9 */
            for (int index = 0; index + 1 > 0;) {
                /* steps 9.i-9.ii */
                ScriptObject next;
                try {
                    next = IteratorStep(cx, iterator);
                } catch (ScriptException e) {
                    return RejectIfAbrupt(cx, e, deferred);
                }
                /* step 9.iii */
                if (next == null) {
                    if (index == 0) {
                        deferred.getResolve().call(cx, UNDEFINED, values);
                    }
                    return deferred.getPromise();
                }
                /* steps 9.iv-9.v */
                Object nextValue;
                try {
                    nextValue = IteratorValue(cx, next);
                } catch (ScriptException e) {
                    return RejectIfAbrupt(cx, e, deferred);
                }
                /* steps 9.vi-9.vii */
                Object nextPromise;
                try {
                    nextPromise = Invoke(cx, c, "cast", nextValue);
                } catch (ScriptException e) {
                    return RejectIfAbrupt(cx, e, deferred);
                }
                /* steps 9.viii-9.xii */
                PromiseAllCountdownFunction countdownFunction = new PromiseAllCountdownFunction(
                        cx.getRealm(), index, values, deferred, countdownHolder);
                /* steps 9.xiii-9.xiv */
                try {
                    Invoke(cx, nextPromise, "then", countdownFunction, deferred.getReject());
                } catch (ScriptException e) {
                    return RejectIfAbrupt(cx, e, deferred);
                }
                /* step 9.xv */
                index += 1;
                /* step 9.xvi */
                countdownHolder.incrementAndGet();
            }
            // prevent integer overflow for 'index'
            throw newInternalError(cx, Messages.Key.InternalError, "integer overflow");
        }

        /**
         * Promise.cast ( x )
         */
        @Function(name = "cast", arity = 1)
        public static Object cast(ExecutionContext cx, Object thisValue, Object x) {
            /* step 1 */
            Object c = thisValue;
            /* step 2 */
            if (IsPromise(x)) {
                Constructor constructor = ((PromiseObject) x).getConstructor();
                if (SameValue(constructor, c)) {
                    return x;
                }
            }
            /* steps 3-4 */
            Deferred deferred = GetDeferred(cx, c);
            /* steps 5-6 */
            deferred.getResolve().call(cx, UNDEFINED, x);
            /* step 7 */
            return deferred.getPromise();
        }

        /**
         * Promise.race ( iterable )
         */
        @Function(name = "race", arity = 1)
        public static Object race(ExecutionContext cx, Object thisValue, Object iterable) {
            /* step 1 */
            Object c = thisValue;
            /* steps 2-3 */
            Deferred deferred = GetDeferred(cx, c);
            /* step 4 */
            ScriptObject iterator;
            try {
                iterator = GetIterator(cx, iterable);
            } catch (ScriptException e) {
                /* step 5 */
                return RejectIfAbrupt(cx, e, deferred);
            }
            /* step 6 */
            for (;;) {
                /* steps 6.i-6.ii */
                ScriptObject next;
                try {
                    next = IteratorStep(cx, iterator);
                } catch (ScriptException e) {
                    return RejectIfAbrupt(cx, e, deferred);
                }
                /* step 6.iii */
                if (next == null) {
                    return deferred.getPromise();
                }
                /* steps 6.iv-6.v */
                Object nextValue;
                try {
                    nextValue = IteratorValue(cx, next);
                } catch (ScriptException e) {
                    return RejectIfAbrupt(cx, e, deferred);
                }
                /* steps 6.vi-6.vii */
                Object nextPromise;
                try {
                    nextPromise = Invoke(cx, c, "cast", nextValue);
                } catch (ScriptException e) {
                    return RejectIfAbrupt(cx, e, deferred);
                }
                /* steps 6.viii-6.ix */
                try {
                    Invoke(cx, nextPromise, "then", deferred.getResolve(), deferred.getReject());
                } catch (ScriptException e) {
                    return RejectIfAbrupt(cx, e, deferred);
                }
            }
        }

        /**
         * Promise.reject ( r )
         */
        @Function(name = "reject", arity = 1)
        public static Object reject(ExecutionContext cx, Object thisValue, Object r) {
            /* step 1 */
            Object c = thisValue;
            /* steps 2-3 */
            Deferred deferred = GetDeferred(cx, c);
            /* steps 4-5 */
            deferred.getReject().call(cx, UNDEFINED, r);
            /* step 6 */
            return deferred.getPromise();
        }

        /**
         * Promise.resolve ( x )
         */
        @Function(name = "resolve", arity = 1)
        public static Object resolve(ExecutionContext cx, Object thisValue, Object x) {
            /* step 1 */
            Object c = thisValue;
            /* steps 2-3 */
            Deferred deferred = GetDeferred(cx, c);
            /* steps 4-5 */
            deferred.getResolve().call(cx, UNDEFINED, x);
            /* step 6 */
            return deferred.getPromise();
        }
    }

    private static class PromiseObjectAllocator implements ObjectAllocator<PromiseObject> {
        static final ObjectAllocator<PromiseObject> INSTANCE = new PromiseObjectAllocator();

        @Override
        public PromiseObject newInstance(Realm realm) {
            return new PromiseObject(realm);
        }
    }
}
