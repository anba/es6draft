/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.observable;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.util.concurrent.atomic.AtomicBoolean;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.Task;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.internal.ScriptException;
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
    protected ObservableConstructor clone() {
        return new ObservableConstructor(getRealm());
    }

    @Override
    public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
        /* step 1 */
        throw newTypeError(calleeContext(), Messages.Key.InvalidCall, "Observable");
    }

    @Override
    public ObservableObject construct(ExecutionContext callerContext, Constructor newTarget, Object... args) {
        ExecutionContext calleeContext = calleeContext();
        Object subscriber = argument(args, 0);
        /* step 1 (not applicable) */
        /* step 2 */
        if (!IsCallable(subscriber)) {
            throw newTypeError(calleeContext, Messages.Key.NotCallable);
        }
        /* steps 3-5 */
        ObservableObject observable = new ObservableObject(calleeContext.getRealm(), (Callable) subscriber,
                GetPrototypeFromConstructor(calleeContext, newTarget, Intrinsics.ObservablePrototype));
        /* step 6 */
        return observable;
    }

    /**
     * Properties of the Observable Constructor
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.FunctionPrototype;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false, configurable = true) )
        public static final int length = 1;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false, configurable = true) )
        public static final String name = "Observable";

        /**
         * Observable.prototype
         */
        @Value(name = "prototype",
                attributes = @Attributes(writable = false, enumerable = false, configurable = false) )
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
            /* steps 3-4 */
            Callable observableMethod = GetMethod(cx, x, BuiltinSymbol.observable.get());
            /* steps 5-6 */
            Callable subscriber;
            if (observableMethod != null) {
                /* step 5 */
                /* steps 5.a-b */
                Object observable = observableMethod.call(cx, x);
                /* step 5.c */
                if (!Type.isObject(observable)) {
                    throw newTypeError(cx, Messages.Key.NotObjectType);
                }
                ScriptObject observableObj = Type.objectValue(observable);
                /* steps 5.d-e */
                Object constructor = Get(cx, observableObj, "constructor");
                /* step 5.f */
                if (constructor == c) {
                    return observable;
                }
                /* steps 5.g-h */
                subscriber = new ObservableFromDelegatingFunction(cx.getRealm(), observableObj);
            } else {
                /* step 6 */
                /* steps 6.a-b */
                subscriber = new ObservableFromIterationFunction(cx.getRealm(), x);
            }
            /* step 7 */
            return c.construct(cx, c, subscriber);
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
            return c.construct(cx, c, subscriber);
        }
    }

    /**
     * Observable.from Delegating Functions
     */
    public static final class ObservableFromDelegatingFunction extends BuiltinFunction {
        /** [[Observable]] */
        private final ScriptObject observable;

        public ObservableFromDelegatingFunction(Realm realm, ScriptObject observable) {
            this(realm, observable, null);
            createDefaultFunctionProperties();
        }

        private ObservableFromDelegatingFunction(Realm realm, ScriptObject observable, Void ignore) {
            super(realm, ANONYMOUS, 1);
            this.observable = observable;
        }

        @Override
        protected ObservableFromDelegatingFunction clone() {
            return new ObservableFromDelegatingFunction(getRealm(), observable, null);
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
        /** [[Items]] */
        private final Object items;

        public ObservableFromIterationFunction(Realm realm, Object items) {
            this(realm, items, null);
            createDefaultFunctionProperties();
        }

        private ObservableFromIterationFunction(Realm realm, Object items, Void ignore) {
            super(realm, ANONYMOUS, 1);
            this.items = items;
        }

        @Override
        protected ObservableFromIterationFunction clone() {
            return new ObservableFromIterationFunction(getRealm(), items, null);
        }

        @Override
        public Callable call(ExecutionContext callerContext, Object thisValue, Object... args) {
            ExecutionContext calleeContext = calleeContext();
            Realm realm = calleeContext.getRealm();
            Object observer = argument(args, 0);
            // FIXME: spec bug - missing type check
            if (!Type.isObject(observer)) {
                throw newTypeError(calleeContext, Messages.Key.NotObjectType);
            }
            /* step 1 */
            Object items = this.items;
            /* steps 2-3 */
            SubscriberClosedFunction cleanup = new SubscriberClosedFunction(realm);
            /* step 4 */
            realm.enqueuePromiseTask(new ObservableFromJob(realm, Type.objectValue(observer), items, cleanup));
            /* step 5 */
            return cleanup;
        }
    }

    public static final class SubscriberClosedFunction extends BuiltinFunction {
        /** [[SubscriptionClosed]] */
        private final AtomicBoolean subscriptionClosed;

        public SubscriberClosedFunction(Realm realm) {
            this(realm, new AtomicBoolean(false));
            createDefaultFunctionProperties();
        }

        private SubscriberClosedFunction(Realm realm, AtomicBoolean subscriptionClosed) {
            super(realm, ANONYMOUS, 0);
            this.subscriptionClosed = subscriptionClosed;
        }

        public boolean isSubscriptionClosed() {
            return subscriptionClosed.get();
        }

        @Override
        protected SubscriberClosedFunction clone() {
            return new SubscriberClosedFunction(getRealm(), subscriptionClosed);
        }

        @Override
        public Undefined call(ExecutionContext callerContext, Object thisValue, Object... args) {
            /* step 1 */
            subscriptionClosed.set(true);
            /* step 2 */
            return UNDEFINED;
        }
    }

    /**
     * ObservableFromJob ( observer, items )
     */
    public static final class ObservableFromJob implements Task {
        private final Realm realm;
        private final ScriptObject observer;
        private final Object items;
        private final SubscriberClosedFunction cleanup;

        ObservableFromJob(Realm realm, ScriptObject observer, Object items, SubscriberClosedFunction cleanup) {
            this.realm = realm;
            this.observer = observer;
            this.items = items;
            this.cleanup = cleanup;
        }

        @Override
        public void execute() {
            ExecutionContext cx = realm.defaultContext();
            /* step 1 */
            if (cleanup.isSubscriptionClosed()) {
                return;
            }
            /* steps 2-4 */
            ScriptIterator<?> iterator;
            try {
                /* steps 2, 4 */
                // FIXME: spec bug - typo 'observer' instead of 'items'
                iterator = GetScriptIterator(cx, items);
            } catch (ScriptException e) {
                /* step 3 */
                Invoke(cx, observer, "error", e.getValue());
                return;
            }
            /* step 5 */
            boolean rethrow = false;
            try {
                while (iterator.hasNext()) {
                    /* steps 5.a-g */
                    Object nextValue = iterator.next();
                    /* step 5.h */
                    {
                        rethrow = true;
                        Invoke(cx, observer, "next", nextValue);
                        rethrow = false;
                    }
                    /* step 5.j */
                    if (cleanup.isSubscriptionClosed()) {
                        return;
                    }
                }
            } catch (ScriptException e) {
                if (rethrow) {
                    /* step 5.i */
                    iterator.close(e);
                    throw e;
                }
                /* steps 5.b.i, 5.f.i */
                Invoke(cx, observer, "error", e.getValue());
                return;
            }
            /* step 5.d.i */
            Invoke(cx, observer, "complete");
        }
    }

    /**
     * Observable.of Subscriber Functions
     */
    public static final class ObservableOfSubscriberFunction extends BuiltinFunction {
        /** [[Items]] */
        private final Object[] items;

        public ObservableOfSubscriberFunction(Realm realm, Object[] items) {
            this(realm, items, null);
            createDefaultFunctionProperties();
        }

        private ObservableOfSubscriberFunction(Realm realm, Object[] items, Void ignore) {
            super(realm, ANONYMOUS, 1);
            this.items = items;
        }

        @Override
        protected ObservableOfSubscriberFunction clone() {
            return new ObservableOfSubscriberFunction(getRealm(), items, null);
        }

        @Override
        public Callable call(ExecutionContext callerContext, Object thisValue, Object... args) {
            ExecutionContext calleeContext = calleeContext();
            Realm realm = calleeContext.getRealm();
            Object observer = argument(args, 0);
            // FIXME: spec bug - missing type check
            if (!Type.isObject(observer)) {
                throw newTypeError(calleeContext, Messages.Key.NotObjectType);
            }
            /* step 1 */
            Object[] items = this.items;
            /* steps 2-3 */
            SubscriberClosedFunction cleanup = new SubscriberClosedFunction(realm);
            /* step 4 */
            realm.enqueuePromiseTask(new ObservableOfJob(realm, Type.objectValue(observer), items, cleanup));
            /* step 5 */
            return cleanup;
        }
    }

    /**
     * ObservableOfJob ( observer, items )
     */
    public static final class ObservableOfJob implements Task {
        private final Realm realm;
        private final ScriptObject observer;
        private final Object[] items;
        private final SubscriberClosedFunction cleanup;

        ObservableOfJob(Realm realm, ScriptObject observer, Object[] items, SubscriberClosedFunction cleanup) {
            this.realm = realm;
            this.observer = observer;
            this.items = items;
            this.cleanup = cleanup;
        }

        @Override
        public void execute() {
            ExecutionContext cx = realm.defaultContext();
            /* step 1 */
            if (cleanup.isSubscriptionClosed()) {
                return;
            }
            /* steps 2-4, 4.a, 4.e */
            for (Object kValue : items) {
                /* steps 4.b-c */
                Invoke(cx, observer, "next", kValue);
                /* step 4.d */
                if (cleanup.isSubscriptionClosed()) {
                    return;
                }
            }
            /* step 5 */
            Invoke(cx, observer, "complete");
        }
    }
}
