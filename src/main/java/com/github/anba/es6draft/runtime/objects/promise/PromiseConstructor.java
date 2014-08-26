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
import static com.github.anba.es6draft.runtime.objects.promise.PromiseAbstractOperations.CreateResolvingFunctions;
import static com.github.anba.es6draft.runtime.objects.promise.PromiseAbstractOperations.IsPromise;
import static com.github.anba.es6draft.runtime.objects.promise.PromiseAbstractOperations.NewPromiseCapability;
import static com.github.anba.es6draft.runtime.objects.promise.PromiseCapability.IfAbruptRejectPromise;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.ExoticArray.ArrayCreate;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.objects.promise.PromiseAbstractOperations.ResolvingFunctions;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinConstructor;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinFunction;
import com.github.anba.es6draft.runtime.types.builtins.ExoticArray;

/**
 * <h1>25 Control Abstraction Objects</h1><br>
 * <h2>25.4 Promise Objects</h2>
 * <ul>
 * <li>25.4.3 The Promise Constructor
 * <li>25.4.4 Properties of the Promise Constructor
 * </ul>
 */
public final class PromiseConstructor extends BuiltinConstructor implements Initializable {
    /**
     * Constructs a new Promise constructor function.
     * 
     * @param realm
     *            the realm object
     */
    public PromiseConstructor(Realm realm) {
        super(realm, "Promise");
    }

    @Override
    public void initialize(ExecutionContext cx) {
        addRestrictedFunctionProperties(cx);
        createProperties(cx, this, Properties.class);
    }

    @Override
    public PromiseConstructor clone() {
        return new PromiseConstructor(getRealm());
    }

    /**
     * 25.4.3.1 Promise ( executor )
     */
    @Override
    public PromiseObject call(ExecutionContext callerContext, Object thisValue, Object... args) {
        ExecutionContext calleeContext = calleeContext();
        Object executor = argument(args, 0);
        /* steps 2-3 */
        if (!(thisValue instanceof PromiseObject)) {
            throw newTypeError(calleeContext, Messages.Key.IncompatibleObject);
        }
        /* step 1 */
        PromiseObject promise = (PromiseObject) thisValue;
        /* step 4 */
        if (promise.getState() != null) {
            throw newTypeError(calleeContext, Messages.Key.InitializedObject);
        }
        /* step 5 */
        if (!IsCallable(executor)) {
            throw newTypeError(calleeContext, Messages.Key.NotCallable);
        }
        /* step 6 */
        return InitializePromise(calleeContext, promise, (Callable) executor);
    }

    /**
     * 25.4.3.1.1 InitializePromise ( promise, executor )
     * 
     * @param cx
     *            the execution context
     * @param promise
     *            the promise object
     * @param executor
     *            the promise executor function
     * @return the promise object
     */
    public static PromiseObject InitializePromise(ExecutionContext cx, PromiseObject promise,
            Callable executor) {
        /* step 1 */
        assert promise.getState() == null;
        /* step 2 (not applicable) */
        /* steps 3-5 */
        promise.initialize();
        /* step 6 */
        ResolvingFunctions resolvingFunctions = CreateResolvingFunctions(cx, promise);
        /* step 7 */
        try {
            executor.call(cx, UNDEFINED, resolvingFunctions.getResolve(),
                    resolvingFunctions.getReject());
        } catch (ScriptException e) {
            /* step 8 */
            resolvingFunctions.getReject().call(cx, UNDEFINED, e.getValue());
        }
        /* step 9 */
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
         * 25.4.4.2 Promise.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.PromisePrototype;

        /**
         * 25.4.4.1 Promise.all ( iterable )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param iterable
         *            the iterable
         * @return the promise object
         */
        @Function(name = "all", arity = 1)
        public static Object all(ExecutionContext cx, Object thisValue, Object iterable) {
            /* step 1 */
            Object c = thisValue;
            /* steps 2-3 */
            PromiseCapability<?> promiseCapability = NewPromiseCapability(cx, c);
            /* step 4 */
            ScriptObject iterator;
            try {
                iterator = GetIterator(cx, iterable);
            } catch (ScriptException e) {
                /* step 5 */
                return IfAbruptRejectPromise(cx, e, promiseCapability);
            }
            /* step 6 */
            ExoticArray values = ArrayCreate(cx, 0);
            /* step 7 */
            AtomicInteger remainingElementsCount = new AtomicInteger(1);
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
                    if (remainingElementsCount.decrementAndGet() == 0) {
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
                    nextPromise = Invoke(cx, c, "resolve", nextValue);
                } catch (ScriptException e) {
                    return IfAbruptRejectPromise(cx, e, promiseCapability);
                }
                /* steps 9.h-9.m */
                PromiseAllResolveElementFunction resolveElement = new PromiseAllResolveElementFunction(
                        cx.getRealm(), index, values, promiseCapability, remainingElementsCount,
                        new AtomicBoolean(false));
                /* step 9.n */
                remainingElementsCount.incrementAndGet();
                /* steps 9.o-9.p */
                try {
                    Invoke(cx, nextPromise, "then", resolveElement, promiseCapability.getReject());
                } catch (ScriptException e) {
                    return IfAbruptRejectPromise(cx, e, promiseCapability);
                }
                /* step 9.q */
                index += 1;
            }
            // prevent integer overflow for 'index'
            throw newInternalError(cx, Messages.Key.InternalError, "integer overflow");
        }

        /**
         * 25.4.4.3 Promise.race ( iterable )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param iterable
         *            the iterable
         * @return the promise object
         */
        @Function(name = "race", arity = 1)
        public static Object race(ExecutionContext cx, Object thisValue, Object iterable) {
            /* step 1 */
            Object c = thisValue;
            /* steps 2-3 */
            PromiseCapability<?> promiseCapability = NewPromiseCapability(cx, c);
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
                    nextPromise = Invoke(cx, c, "resolve", nextValue);
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
         * 25.4.4.4 Promise.reject ( r )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param r
         *            the rejected value
         * @return the new promise object
         */
        @Function(name = "reject", arity = 1)
        public static Object reject(ExecutionContext cx, Object thisValue, Object r) {
            /* step 1 */
            Object c = thisValue;
            /* steps 2-3 */
            PromiseCapability<?> promiseCapability = NewPromiseCapability(cx, c);
            /* steps 4-5 */
            promiseCapability.getReject().call(cx, UNDEFINED, r);
            /* step 6 */
            return promiseCapability.getPromise();
        }

        /**
         * 25.4.4.5 Promise.resolve ( x )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param x
         *            the resolved value
         * @return the new promise object
         */
        @Function(name = "resolve", arity = 1)
        public static Object resolve(ExecutionContext cx, Object thisValue, Object x) {
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
            PromiseCapability<?> promiseCapability = NewPromiseCapability(cx, c);
            /* steps 5-6 */
            promiseCapability.getResolve().call(cx, UNDEFINED, x);
            /* step 7 */
            return promiseCapability.getPromise();
        }

        /**
         * 25.4.4.6 Promise [ @@create ] ( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the new promise object
         */
        @Function(name = "[Symbol.create]", symbol = BuiltinSymbol.create, arity = 0,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static Object create(ExecutionContext cx, Object thisValue) {
            /* steps 1-2 */
            return AllocatePromise(cx, thisValue);
        }
    }

    /**
     * 25.4.4.6.1 AllocatePromise( constructor ) Abstraction Operation
     * 
     * @param cx
     *            the execution context
     * @param constructor
     *            the constructor function
     * @return the new promise object
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
        private final ExoticArray values;

        /** [[Capabilities]] */
        private final PromiseCapability<?> capabilities;

        /** [[RemainingElements]] */
        private final AtomicInteger remainingElements;

        /** [[AlreadyCalled]] */
        private final AtomicBoolean alreadyCalled;

        public PromiseAllResolveElementFunction(Realm realm, int index, ExoticArray values,
                PromiseCapability<?> capabilities, AtomicInteger remainingElements,
                AtomicBoolean alreadyCalled) {
            this(realm, index, values, capabilities, remainingElements, alreadyCalled, null);
            createDefaultFunctionProperties(ANONYMOUS, 1);
        }

        private PromiseAllResolveElementFunction(Realm realm, int index, ExoticArray values,
                PromiseCapability<?> capabilities, AtomicInteger remainingElements,
                AtomicBoolean alreadyCalled, Void ignore) {
            super(realm, ANONYMOUS);
            this.index = index;
            this.values = values;
            this.capabilities = capabilities;
            this.remainingElements = remainingElements;
            this.alreadyCalled = alreadyCalled;
        }

        @Override
        public PromiseAllResolveElementFunction clone() {
            return new PromiseAllResolveElementFunction(getRealm(), index, values, capabilities,
                    remainingElements, alreadyCalled, null);
        }

        @Override
        public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
            ExecutionContext calleeContext = calleeContext();
            Object x = argument(args, 0);
            /* steps 1-2 */
            if (!alreadyCalled.compareAndSet(false, true)) {
                return UNDEFINED;
            }
            /* step 3 */
            int index = this.index;
            /* step 4 */
            ExoticArray values = this.values;
            /* step 5 */
            PromiseCapability<?> promiseCapability = this.capabilities;
            /* step 6 */
            AtomicInteger remainingElementsCount = this.remainingElements;
            /* step 7 */
            try {
                CreateDataProperty(calleeContext, values, index, x);
            } catch (ScriptException e) {
                /* step 8 */
                return IfAbruptRejectPromise(calleeContext, e, promiseCapability);
            }
            /* steps 9-10 */
            if (remainingElementsCount.decrementAndGet() == 0) {
                return promiseCapability.getResolve().call(calleeContext, UNDEFINED, values);
            }
            /* step 11 */
            return UNDEFINED;
        }
    }
}
