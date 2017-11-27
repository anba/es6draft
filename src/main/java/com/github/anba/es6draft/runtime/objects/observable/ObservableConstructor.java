/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.observable;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.observable.SubscriptionAbstractOperations.SubscriptionClosed;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.internal.ScriptIterator;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.Undefined;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinConstructor;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinFunction;

/**
 * <h1>Observable</h1>
 * <ul>
 * <li>The Observable Constructor
 * <li>Properties of the Observable Constructor
 * </ul>
 */
public final class ObservableConstructor extends BuiltinConstructor implements Initializable {
    /**
     * Constructs a new Observable constructor function.
     * 
     * @param realm
     *            the realm object
     */
    public ObservableConstructor(Realm realm) {
        super(realm, "Observable", 1);
    }

    @Override
    public void initialize(Realm realm) {
        createProperties(realm, this, Properties.class);
    }

    @Override
    public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
        /* step 1 */
        throw newTypeError(calleeContext(), Messages.Key.InvalidCall, "Observable");
    }

    /**
     * Observable ( subscriber )
     */
    @Override
    public ObservableObject construct(ExecutionContext callerContext, Constructor newTarget, Object... args) {
        ExecutionContext calleeContext = calleeContext();
        Object subscriber = argument(args, 0);
        /* step 1 (not applicable) */
        /* step 2 */
        if (!IsCallable(subscriber)) {
            throw newTypeError(calleeContext, Messages.Key.NotCallable);
        }
        /* steps 3-4 */
        ObservableObject observable = new ObservableObject(calleeContext.getRealm(), (Callable) subscriber,
                GetPrototypeFromConstructor(calleeContext, newTarget, Intrinsics.ObservablePrototype));
        /* step 5 */
        return observable;
    }

    /**
     * Properties of the Observable Constructor
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.FunctionPrototype;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final int length = 1;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final String name = "Observable";

        /**
         * Observable.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static final Intrinsics prototype = Intrinsics.ObservablePrototype;

        /**
         * Observable.from ( x )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param x
         *            the observable object
         * @return the new observable object
         */
        @Function(name = "from", arity = 1)
        public static Object from(ExecutionContext cx, Object thisValue, Object x) {
            /* steps 1-2 */
            Constructor c = (Constructor) (IsConstructor(thisValue) ? thisValue
                    : cx.getIntrinsic(Intrinsics.Observable));
            /* step 3 */
            Callable observableMethod = GetMethod(cx, x, BuiltinSymbol.observable.get());
            /* steps 4-5 */
            Callable subscriber;
            if (observableMethod != null) {
                /* step 4 */
                /* step 4.a */
                Object observable = observableMethod.call(cx, x);
                /* step 4.b */
                if (!Type.isObject(observable)) {
                    throw newTypeError(cx, Messages.Key.NotObjectType);
                }
                ScriptObject observableObj = Type.objectValue(observable);
                /* step 4.c */
                Object constructor = Get(cx, observableObj, "constructor");
                /* step 4.d */
                if (constructor == c) {
                    return observable;
                }
                /* steps 4.e-f */
                subscriber = new ObservableFromDelegatingFunction(cx.getRealm(), observableObj);
            } else {
                /* step 5 */
                /* step 5.a */
                Callable iteratorMethod = GetMethod(cx, x, BuiltinSymbol.iterator.get());
                /* step 5.b */
                if (iteratorMethod == null) {
                    throw newTypeError(cx, Messages.Key.PropertyNotCallable, BuiltinSymbol.iterator.toString());
                }
                /* steps 5.c-e */
                subscriber = new ObservableFromIterationFunction(cx.getRealm(), x, iteratorMethod);
            }
            /* step 6 */
            return c.construct(cx, subscriber);
        }

        /**
         * Observable.of ( ...items )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param items
         *            the element values
         * @return the new observable object
         */
        @Function(name = "of", arity = 0)
        public static Object of(ExecutionContext cx, Object thisValue, Object... items) {
            /* steps 1-2 */
            Constructor c = (Constructor) (IsConstructor(thisValue) ? thisValue
                    : cx.getIntrinsic(Intrinsics.Observable));
            /* steps 3-4 */
            Callable subscriber = new ObservableOfSubscriberFunction(cx.getRealm(), items);
            /* step 5 */
            return c.construct(cx, subscriber);
        }
    }

    /**
     * Observable.from Delegating Functions
     */
    public static final class ObservableFromDelegatingFunction extends BuiltinFunction {
        /** [[Observable]] */
        private final ScriptObject observable;

        public ObservableFromDelegatingFunction(Realm realm, ScriptObject observable) {
            super(realm, ANONYMOUS, 1);
            this.observable = observable;
            createDefaultFunctionProperties();
        }

        @Override
        public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
            ExecutionContext calleeContext = calleeContext();
            Object observer = argument(args, 0);
            /* step 1 */
            ScriptObject observable = this.observable;
            /* step 2 */
            return Invoke(calleeContext, observable, "subscribe", observer);
        }
    }

    /**
     * Observable.from Iteration Functions
     */
    public static final class ObservableFromIterationFunction extends BuiltinFunction {
        /** [[Iterable]] */
        private final Object iterable;

        /** [[IteratorMethod]] */
        private final Callable iteratorMethod;

        public ObservableFromIterationFunction(Realm realm, Object iterable, Callable iteratorMethod) {
            super(realm, ANONYMOUS, 1);
            this.iterable = iterable;
            this.iteratorMethod = iteratorMethod;
            createDefaultFunctionProperties();
        }

        @Override
        public Undefined call(ExecutionContext callerContext, Object thisValue, Object... args) {
            ExecutionContext calleeContext = calleeContext();
            Object observer = argument(args, 0);
            /* step 1 */
            Object iterable = this.iterable;
            /* step 2 */
            Callable iteratorMethod = this.iteratorMethod;
            /* step 3 */
            ScriptIterator<?> iterator = GetIterator(calleeContext, iterable, iteratorMethod);
            /* step 4 */
            // FIXME: spec bug - missing type checks
            if (!(observer instanceof SubscriptionObserverObject)) {
                throw newTypeError(calleeContext, Messages.Key.IncompatibleArgument, "Observable.from",
                        Type.of(observer).toString());
            }
            SubscriptionObject subscription = ((SubscriptionObserverObject) observer).getSubscription();
            /* step 5 */
            while (iterator.hasNext()) {
                /* steps 5.a-c */
                Object nextValue = iterator.next();
                /* step 5.d */
                // FIXME: spec bug - cannot annotate Invoke as infallible.
                Invoke(calleeContext, observer, "next", nextValue);
                /* step 5.e */
                if (SubscriptionClosed(subscription)) {
                    iterator.close();
                    return UNDEFINED;
                }
            }
            /* step 5.b.i */
            // FIXME: spec bug - cannot annotate Invoke as infallible.
            Invoke(calleeContext, observer, "complete");
            /* step 5.b.ii */
            return UNDEFINED;
        }
    }

    /**
     * Observable.of Subscriber Functions
     */
    public static final class ObservableOfSubscriberFunction extends BuiltinFunction {
        /** [[Items]] */
        private final Object[] items;

        public ObservableOfSubscriberFunction(Realm realm, Object[] items) {
            super(realm, ANONYMOUS, 1);
            this.items = items;
            createDefaultFunctionProperties();
        }

        @Override
        public Undefined call(ExecutionContext callerContext, Object thisValue, Object... args) {
            ExecutionContext calleeContext = calleeContext();
            Object observer = argument(args, 0);
            // FIXME: spec bug - missing type check
            if (!Type.isObject(observer)) {
                throw newTypeError(calleeContext, Messages.Key.NotObjectType);
            }
            /* step 1 */
            Object[] items = this.items;
            /* step 2 */
            // FIXME: spec bug - missing type checks
            if (!(observer instanceof SubscriptionObserverObject)) {
                throw newTypeError(calleeContext, Messages.Key.IncompatibleArgument, "Observable.from",
                        Type.of(observer).toString());
            }
            SubscriptionObject subscription = ((SubscriptionObserverObject) observer).getSubscription();
            /* step 3 */
            // FIXME: spec issue - is it valid to use an ecmaSpeak for-each loop here?
            for (Object value : items) {
                /* step 3.a */
                // FIXME: spec bug - cannot annotate Invoke as infallible.
                Invoke(calleeContext, observer, "next", value);
                /* step 3.b */
                if (SubscriptionClosed(subscription)) {
                    return UNDEFINED;
                }
            }
            /* step 4 */
            // FIXME: spec bug - cannot annotate Invoke as infallible.
            Invoke(calleeContext, observer, "complete");
            /* step 5 */
            return UNDEFINED;
        }
    }
}
