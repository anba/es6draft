/**
 * Copyright (c) 2012-2015 André Bargull
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
import static com.github.anba.es6draft.runtime.objects.promise.PromiseAbstractOperations.GetPromiseAllocator;
import static com.github.anba.es6draft.runtime.objects.promise.PromiseAbstractOperations.IsPromise;
import static com.github.anba.es6draft.runtime.objects.promise.PromiseAbstractOperations.NewPromiseCapability;
import static com.github.anba.es6draft.runtime.objects.promise.PromiseCapability.IfAbruptRejectPromise;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Accessor;
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
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.ArrayObject;
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
public final class PromiseConstructor extends BuiltinConstructor implements Initializable {
    /**
     * Constructs a new Promise constructor function.
     * 
     * @param realm
     *            the realm object
     */
    public PromiseConstructor(Realm realm) {
        super(realm, "Promise", 1);
    }

    @Override
    public void initialize(Realm realm) {
        createProperties(realm, this, Properties.class);
    }

    @Override
    public PromiseConstructor clone() {
        return new PromiseConstructor(getRealm());
    }

    /**
     * 25.4.3.1 Promise ( executor )
     */
    @Override
    public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
        ExecutionContext calleeContext = calleeContext();
        /* step 1 */
        throw newTypeError(calleeContext, Messages.Key.InvalidCall, "Promise");
    }

    /**
     * 25.4.3.1 Promise ( executor )
     */
    @Override
    public PromiseObject construct(ExecutionContext callerContext, Constructor newTarget,
            Object... args) {
        ExecutionContext calleeContext = calleeContext();
        Object executor = argument(args, 0);

        /* step 1 (not applicable) */
        /* step 2 */
        if (!IsCallable(executor)) {
            throw newTypeError(calleeContext, Messages.Key.NotCallable);
        }
        /* step 3 */
        PromiseObject promise = OrdinaryCreateFromConstructor(calleeContext, newTarget,
                Intrinsics.PromisePrototype, GetPromiseAllocator(calleeContext.getRealm()));
        /* steps 4-7 */
        promise.initialize(newTarget);
        /* step 8 */
        ResolvingFunctions resolvingFunctions = CreateResolvingFunctions(calleeContext, promise);
        /* steps 9-10 */
        try {
            /* step 9 */
            ((Callable) executor).call(calleeContext, UNDEFINED, resolvingFunctions.getResolve(),
                    resolvingFunctions.getReject());
        } catch (ScriptException e) {
            /* step 10 */
            resolvingFunctions.getReject().call(calleeContext, UNDEFINED, e.getValue());
        }
        /* step 11 */
        return promise;
    }

    /**
     * 25.4.4 Properties of the Promise Constructor
     */
    public enum Properties {
        ;

        private static Constructor promiseConstructorFromSpecies(ExecutionContext cx, Object c) {
            /* step 1 */
            if (!Type.isObject(c)) {
                throw newTypeError(cx, Messages.Key.NotObjectType);
            }
            /* steps 2-3 */
            Object species = Get(cx, Type.objectValue(c), BuiltinSymbol.species.get());
            /* step 4 */
            Object constructor = !Type.isUndefinedOrNull(species) ? species : c;
            /* (type check from NewPromiseCapability) */
            if (!IsConstructor(constructor)) {
                throw newTypeError(cx, Messages.Key.NotConstructor);
            }
            return (Constructor) constructor;
        }

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
            /* steps 1-5 */
            Constructor c = promiseConstructorFromSpecies(cx, thisValue);
            /* steps 6-7 */
            PromiseCapability<?> promiseCapability = NewPromiseCapability(cx, c);
            /* step 8 */
            ScriptObject iterator;
            try {
                iterator = GetIterator(cx, iterable);
            } catch (ScriptException e) {
                /* step 9 */
                return IfAbruptRejectPromise(cx, e, promiseCapability);
            }
            /* step 10 */
            return PerformPromiseAll(cx, iterator, c, promiseCapability);
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
            /* steps 1-5 */
            Constructor c = promiseConstructorFromSpecies(cx, thisValue);
            /* steps 6-7 */
            PromiseCapability<?> promiseCapability = NewPromiseCapability(cx, c);
            /* step 8 */
            ScriptObject iterator;
            try {
                iterator = GetIterator(cx, iterable);
            } catch (ScriptException e) {
                /* step 9 */
                return IfAbruptRejectPromise(cx, e, promiseCapability);
            }
            /* step 10 */
            for (;;) {
                /* steps 10.a-10.b */
                ScriptObject next;
                try {
                    next = IteratorStep(cx, iterator);
                } catch (ScriptException e) {
                    return IfAbruptRejectPromise(cx, e, promiseCapability);
                }
                /* step 10.c */
                if (next == null) {
                    return promiseCapability.getPromise();
                }
                /* steps 10.d-10.e */
                Object nextValue;
                try {
                    nextValue = IteratorValue(cx, next);
                } catch (ScriptException e) {
                    return IfAbruptRejectPromise(cx, e, promiseCapability);
                }
                /* steps 10.f-10.g */
                Object nextPromise;
                try {
                    nextPromise = Invoke(cx, c, "resolve", nextValue);
                } catch (ScriptException e) {
                    return IfAbruptRejectPromise(cx, e, promiseCapability);
                }
                /* steps 10.h-10.i */
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
            /* steps 1-5 */
            Constructor c = promiseConstructorFromSpecies(cx, thisValue);
            /* steps 6-7 */
            PromiseCapability<?> promiseCapability = NewPromiseCapability(cx, c);
            /* steps 8-9 */
            promiseCapability.getReject().call(cx, UNDEFINED, r);
            /* step 10 */
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
            /* step 2 */
            if (IsPromise(x)) {
                Constructor constructor = ((PromiseObject) x).getConstructor();
                if (constructor == thisValue) { // SameValue
                    return x;
                }
            }
            /* steps 1, 3-6 */
            Constructor c = promiseConstructorFromSpecies(cx, thisValue);
            /* steps 7-8 */
            PromiseCapability<?> promiseCapability = NewPromiseCapability(cx, c);
            /* steps 9-10 */
            promiseCapability.getResolve().call(cx, UNDEFINED, x);
            /* step 11 */
            return promiseCapability.getPromise();
        }

        /**
         * 25.4.4.6 get Promise [ @@species ]
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the species object
         */
        @Accessor(name = "get [Symbol.species]", symbol = BuiltinSymbol.species,
                type = Accessor.Type.Getter)
        public static Object species(ExecutionContext cx, Object thisValue) {
            /* step 1 */
            return thisValue;
        }
    }

    /**
     * 25.4.4.1.1 PerformPromiseAll( iterator, constructor, resultCapability) Abstract Operation
     * 
     * @param <PROMISE>
     *            the promise type
     * @param cx
     *            the execution context
     * @param iterator
     *            the iterator object
     * @param constructor
     *            the constructor object
     * @param resultCapability
     *            the new promise capability record
     * @return the new promise object
     */
    public static <PROMISE extends ScriptObject> PROMISE PerformPromiseAll(ExecutionContext cx,
            ScriptObject iterator, Constructor constructor,
            PromiseCapability<PROMISE> resultCapability) {
        /* steps 1-3 (not applicable) */
        /* step 4 */
        ArrayList<Object> values = new ArrayList<>();
        /* step 5 */
        AtomicInteger remainingElementsCount = new AtomicInteger(1);
        /* steps 6-7 */
        for (int index = 0; index + 1 > 0;) {
            /* steps 7.a-7.b */
            ScriptObject next;
            try {
                next = IteratorStep(cx, iterator);
            } catch (ScriptException e) {
                return IfAbruptRejectPromise(cx, e, resultCapability);
            }
            /* step 7.c */
            if (next == null) {
                if (remainingElementsCount.decrementAndGet() == 0) {
                    ArrayObject valuesArray = CreateArrayFromList(cx, values);
                    resultCapability.getResolve().call(cx, UNDEFINED, valuesArray);
                }
                return resultCapability.getPromise();
            }
            /* steps 7.d-7.e */
            Object nextValue;
            try {
                nextValue = IteratorValue(cx, next);
            } catch (ScriptException e) {
                return IfAbruptRejectPromise(cx, e, resultCapability);
            }
            /* step 7.f */
            // Using 'null' instead of undefined to be able to verify that no values are overwritten
            values.add(null);
            /* steps 7.g-7.h */
            Object nextPromise;
            try {
                nextPromise = Invoke(cx, constructor, "resolve", nextValue);
            } catch (ScriptException e) {
                return IfAbruptRejectPromise(cx, e, resultCapability);
            }
            /* steps 7.i-7.n */
            PromiseAllResolveElementFunction resolveElement = new PromiseAllResolveElementFunction(
                    cx.getRealm(), new AtomicBoolean(false), index, values, resultCapability,
                    remainingElementsCount);
            /* step 7.o */
            remainingElementsCount.incrementAndGet();
            /* steps 7.p-7.q */
            try {
                Invoke(cx, nextPromise, "then", resolveElement, resultCapability.getReject());
            } catch (ScriptException e) {
                return IfAbruptRejectPromise(cx, e, resultCapability);
            }
            /* step 7.r */
            index += 1;
        }
        // prevent integer overflow for 'index'
        throw newInternalError(cx, Messages.Key.InternalError, "integer overflow");
    }

    /**
     * 25.4.4.1.1 Promise.all Resolve Element Functions
     */
    public static final class PromiseAllResolveElementFunction extends BuiltinFunction {
        /** [[AlreadyCalled]] */
        private final AtomicBoolean alreadyCalled;

        /** [[Index]] */
        private final int index;

        /** [[Values]] */
        private final ArrayList<Object> values;

        /** [[Capabilities]] */
        private final PromiseCapability<?> capabilities;

        /** [[RemainingElements]] */
        private final AtomicInteger remainingElements;

        public PromiseAllResolveElementFunction(Realm realm, AtomicBoolean alreadyCalled,
                int index, ArrayList<Object> values, PromiseCapability<?> capabilities,
                AtomicInteger remainingElements) {
            this(realm, alreadyCalled, index, values, capabilities, remainingElements, null);
            createDefaultFunctionProperties();
        }

        private PromiseAllResolveElementFunction(Realm realm, AtomicBoolean alreadyCalled,
                int index, ArrayList<Object> values, PromiseCapability<?> capabilities,
                AtomicInteger remainingElements, Void ignore) {
            super(realm, ANONYMOUS, 1);
            this.alreadyCalled = alreadyCalled;
            this.index = index;
            this.values = values;
            this.capabilities = capabilities;
            this.remainingElements = remainingElements;
        }

        @Override
        public PromiseAllResolveElementFunction clone() {
            return new PromiseAllResolveElementFunction(getRealm(), alreadyCalled, index, values,
                    capabilities, remainingElements, null);
        }

        @Override
        public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
            ExecutionContext calleeContext = calleeContext();
            Object x = argument(args, 0);
            /* steps 1-3 */
            if (!alreadyCalled.compareAndSet(false, true)) {
                return UNDEFINED;
            }
            /* step 4 */
            int index = this.index;
            /* step 5 */
            ArrayList<Object> values = this.values;
            /* step 6 */
            PromiseCapability<?> promiseCapability = this.capabilities;
            /* step 7 */
            AtomicInteger remainingElementsCount = this.remainingElements;
            /* step 8 */
            assert values.get(index) == null : String.format("values[%d] = %s", index,
                    values.get(index));
            values.set(index, x);
            /* steps 9-10 */
            if (remainingElementsCount.decrementAndGet() == 0) {
                ArrayObject valuesArray = CreateArrayFromList(calleeContext, values);
                return promiseCapability.getResolve().call(calleeContext, UNDEFINED, valuesArray);
            }
            /* step 11 */
            return UNDEFINED;
        }
    }
}
