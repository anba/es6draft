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
import static com.github.anba.es6draft.runtime.objects.promise.PromiseAbstractOperations.CreateRejectFunction;
import static com.github.anba.es6draft.runtime.objects.promise.PromiseAbstractOperations.CreateResolveFunction;
import static com.github.anba.es6draft.runtime.objects.promise.PromiseAbstractOperations.IsPromise;
import static com.github.anba.es6draft.runtime.objects.promise.PromiseAbstractOperations.NewPromiseCapability;
import static com.github.anba.es6draft.runtime.objects.promise.PromiseCapability.IfAbruptRejectPromise;
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
import com.github.anba.es6draft.runtime.objects.promise.PromiseAbstractOperations.PromiseRejectFunction;
import com.github.anba.es6draft.runtime.objects.promise.PromiseAbstractOperations.PromiseResolveFunction;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinConstructor;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinFunction;

/**
 * <h1>25 Control Abstraction Objects</h1><br>
 * <h2>25.4 Promise Objects</h2>
 * <ul>
 * <li>25.4.3 The Promise Constructor
 * <li>25.4.4 Properties of the Promise Constructor
 * </ul>
 */
public final class PromiseConstructor extends BuiltinConstructor implements Initialisable {
    public PromiseConstructor(Realm realm) {
        super(realm, "Promise");
    }

    @Override
    public void initialise(ExecutionContext cx) {
        createProperties(this, cx, Properties.class);
        AddRestrictedFunctionProperties(cx, this);
    }

    /**
     * 25.4.3.1 Promise ( executor )
     */
    @Override
    public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
        ExecutionContext calleeContext = calleeContext();
        Object executor = args.length > 0 ? args[0] : UNDEFINED;
        /* step 2 */
        if (!IsCallable(executor)) {
            throw newTypeError(calleeContext, Messages.Key.NotCallable);
        }
        /* steps 3-4 */
        if (!(thisValue instanceof PromiseObject)) {
            throw newTypeError(calleeContext, Messages.Key.IncompatibleObject);
        }
        /* step 1 */
        PromiseObject promise = (PromiseObject) thisValue;
        /* step 5 */
        if (promise.getStatus() != null) {
            throw newTypeError(calleeContext, Messages.Key.InitialisedObject);
        }
        /* step 6 */
        return InitialisePromise(calleeContext, promise, (Callable) executor);
    }

    /**
     * 25.4.3.1.1 InitialisePromise( promise, executor) Abstract Operation
     */
    public static PromiseObject InitialisePromise(ExecutionContext cx, PromiseObject promise,
            Callable executor) {
        /* step 1 */
        assert promise.getStatus() == null;
        /* step 2 (not applicable) */
        /* steps 3-5 */
        promise.initialise();
        /* step 6 */
        PromiseResolveFunction resolve = CreateResolveFunction(cx, promise);
        /* step 7 */
        PromiseRejectFunction reject = CreateRejectFunction(cx, promise);
        /* step 8 */
        try {
            executor.call(cx, UNDEFINED, resolve, reject);
        } catch (ScriptException e) {
            /* step 9 */
            reject.call(cx, UNDEFINED, e.getValue());
        }
        /* step 10 */
        return promise;
    }

    /**
     * 25.4.3.2 new Promise ( ... argumentsList )
     */
    @Override
    public ScriptObject construct(ExecutionContext callerContext, Object... args) {
        /* steps 1-3 */
        return Construct(callerContext, this, args);
    }

    /**
     * 25.4.4 Properties of the Promise Constructor
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

        /**
         * 25.4.4.3 Promise.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.PromisePrototype;

        /**
         * 25.4.4.1 Promise.all ( iterable )
         */
        @Function(name = "all", arity = 1)
        public static Object all(ExecutionContext cx, Object thisValue, Object iterable) {
            /* step 1 */
            Object c = thisValue;
            /* steps 2-3 */
            PromiseCapability promiseCapability = NewPromiseCapability(cx, c);
            /* step 4 */
            ScriptObject iterator;
            try {
                iterator = GetIterator(cx, iterable);
            } catch (ScriptException e) {
                /* step 5 */
                return IfAbruptRejectPromise(cx, e, promiseCapability);
            }
            /* step 6 */
            ScriptObject values = ArrayCreate(cx, 0);
            /* step 7 */
            AtomicInteger remainingElementsCount = new AtomicInteger(0);
            /* steps 8-9 */
            for (int index = 0; index + 1 > 0;) {
                /* steps 9.a-9.b */
                ScriptObject next;
                try {
                    next = IteratorStep(cx, iterator);
                } catch (ScriptException e) {
                    return IfAbruptRejectPromise(cx, e, promiseCapability);
                }
                /* step 9.c */
                if (next == null) {
                    if (index == 0) {
                        promiseCapability.getResolve().call(cx, UNDEFINED, values);
                    }
                    return promiseCapability.getPromise();
                }
                /* steps 9.d-9.e */
                Object nextValue;
                try {
                    nextValue = IteratorValue(cx, next);
                } catch (ScriptException e) {
                    return IfAbruptRejectPromise(cx, e, promiseCapability);
                }
                /* steps 9.f-9.g */
                Object nextPromise;
                try {
                    nextPromise = Invoke(cx, c, "cast", nextValue);
                } catch (ScriptException e) {
                    return IfAbruptRejectPromise(cx, e, promiseCapability);
                }
                /* steps 9.h-9.l */
                PromiseAllResolveElementFunction resolveElement = new PromiseAllResolveElementFunction(
                        cx.getRealm(), index, values, promiseCapability, remainingElementsCount);
                /* steps 9.m-9.n */
                try {
                    Invoke(cx, nextPromise, "then", resolveElement, promiseCapability.getReject());
                } catch (ScriptException e) {
                    return IfAbruptRejectPromise(cx, e, promiseCapability);
                }
                /* step 9.o */
                index += 1;
                /* step 9.p */
                remainingElementsCount.incrementAndGet();
            }
            // prevent integer overflow for 'index'
            throw newInternalError(cx, Messages.Key.InternalError, "integer overflow");
        }

        /**
         * 25.4.4.2 Promise.cast ( x )
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
            PromiseCapability promiseCapability = NewPromiseCapability(cx, c);
            /* steps 5-6 */
            promiseCapability.getResolve().call(cx, UNDEFINED, x);
            /* step 7 */
            return promiseCapability.getPromise();
        }

        /**
         * 25.4.4.4 Promise.race ( iterable )
         */
        @Function(name = "race", arity = 1)
        public static Object race(ExecutionContext cx, Object thisValue, Object iterable) {
            /* step 1 */
            Object c = thisValue;
            /* steps 2-3 */
            PromiseCapability promiseCapability = NewPromiseCapability(cx, c);
            /* step 4 */
            ScriptObject iterator;
            try {
                iterator = GetIterator(cx, iterable);
            } catch (ScriptException e) {
                /* step 5 */
                return IfAbruptRejectPromise(cx, e, promiseCapability);
            }
            /* step 6 */
            for (;;) {
                /* steps 6.a-6.b */
                ScriptObject next;
                try {
                    next = IteratorStep(cx, iterator);
                } catch (ScriptException e) {
                    return IfAbruptRejectPromise(cx, e, promiseCapability);
                }
                /* step 6.c */
                if (next == null) {
                    return promiseCapability.getPromise();
                }
                /* steps 6.d-6.e */
                Object nextValue;
                try {
                    nextValue = IteratorValue(cx, next);
                } catch (ScriptException e) {
                    return IfAbruptRejectPromise(cx, e, promiseCapability);
                }
                /* steps 6.f-6.g */
                Object nextPromise;
                try {
                    nextPromise = Invoke(cx, c, "cast", nextValue);
                } catch (ScriptException e) {
                    return IfAbruptRejectPromise(cx, e, promiseCapability);
                }
                /* steps 6.h-6.i */
                try {
                    Invoke(cx, nextPromise, "then", promiseCapability.getResolve(),
                            promiseCapability.getReject());
                } catch (ScriptException e) {
                    return IfAbruptRejectPromise(cx, e, promiseCapability);
                }
            }
        }

        /**
         * 25.4.4.5 Promise.reject ( r )
         */
        @Function(name = "reject", arity = 1)
        public static Object reject(ExecutionContext cx, Object thisValue, Object r) {
            /* step 1 */
            Object c = thisValue;
            /* steps 2-3 */
            PromiseCapability promiseCapability = NewPromiseCapability(cx, c);
            /* steps 4-5 */
            promiseCapability.getReject().call(cx, UNDEFINED, r);
            /* step 6 */
            return promiseCapability.getPromise();
        }

        /**
         * 25.4.4.6 Promise.resolve ( x )
         */
        @Function(name = "resolve", arity = 1)
        public static Object resolve(ExecutionContext cx, Object thisValue, Object x) {
            /* step 1 */
            Object c = thisValue;
            /* steps 2-3 */
            PromiseCapability promiseCapability = NewPromiseCapability(cx, c);
            /* steps 4-5 */
            promiseCapability.getResolve().call(cx, UNDEFINED, x);
            /* step 6 */
            return promiseCapability.getPromise();
        }

        /**
         * 25.4.4.7 Promise [ @@create ] ( )
         */
        @Function(name = "[Symbol.create]", symbol = BuiltinSymbol.create, arity = 0,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static Object create(ExecutionContext cx, Object thisValue) {
            /* steps 1-2 */
            return AllocatePromise(cx, thisValue);
        }
    }

    /**
     * 25.4.4.7.1 AllocatePromise( constructor ) Abstraction Operation
     */
    public static PromiseObject AllocatePromise(ExecutionContext cx, Object constructor) {
        /* step 1 */
        PromiseObject obj = OrdinaryCreateFromConstructor(cx, constructor,
                Intrinsics.PromisePrototype, PromiseObjectAllocator.INSTANCE);
        /* step 2 */
        assert IsConstructor(constructor);
        obj.setConstructor((Constructor) constructor);
        /* step 3 */
        return obj;
    }

    private static final class PromiseObjectAllocator implements ObjectAllocator<PromiseObject> {
        static final ObjectAllocator<PromiseObject> INSTANCE = new PromiseObjectAllocator();

        @Override
        public PromiseObject newInstance(Realm realm) {
            return new PromiseObject(realm);
        }
    }

    /**
     * 25.4.4.1.1 Promise.all Resolve Element Functions
     */
    public static final class PromiseAllResolveElementFunction extends BuiltinFunction {
        /** [[Index]] */
        private final int index;

        /** [[Values]] */
        private final ScriptObject values;

        /** [[Capabilities]] */
        private final PromiseCapability capabilities;

        /** [[RemainingElements]] */
        private final AtomicInteger remainingElements;

        public PromiseAllResolveElementFunction(Realm realm, int index, ScriptObject values,
                PromiseCapability capabilities, AtomicInteger remainingElements) {
            super(realm, ANONYMOUS, 1);
            this.index = index;
            this.values = values;
            this.capabilities = capabilities;
            this.remainingElements = remainingElements;
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
            PromiseCapability promiseCapability = this.capabilities;
            /* step 4 */
            AtomicInteger remainingElementsCount = this.remainingElements;
            /* step 5 */
            try {
                CreateDataProperty(calleeContext, values, ToString(index), x);
            } catch (ScriptException e) {
                /* step 6 */
                return IfAbruptRejectPromise(calleeContext, e, promiseCapability);
            }
            if (remainingElementsCount.get() == Integer.MIN_VALUE) {
                // prevent integer overflow for 'remainingElementsCount'
                throw newInternalError(calleeContext, Messages.Key.InternalError,
                        "integer overflow");
            }
            /* steps 7-8 */
            if (remainingElementsCount.decrementAndGet() == 0) {
                return promiseCapability.getResolve().call(calleeContext, UNDEFINED, values);
            }
            /* step 9 */
            return UNDEFINED;
        }
    }
}
