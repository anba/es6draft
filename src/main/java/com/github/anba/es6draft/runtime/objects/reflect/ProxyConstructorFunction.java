/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.reflect;

import static com.github.anba.es6draft.runtime.AbstractOperations.CreateDataProperty;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.ProxyObject.ProxyCreate;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.internal.Ref;
import com.github.anba.es6draft.runtime.types.Creatable;
import com.github.anba.es6draft.runtime.types.CreateAction;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Undefined;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinConstructor;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;
import com.github.anba.es6draft.runtime.types.builtins.ProxyObject;

/**
 * <h1>26 Reflection</h1><br>
 * <h2>26.5 Proxy Objects</h2>
 * <ul>
 * <li>26.5.1 The Proxy Constructor Function
 * <li>26.5.2 Properties of the Proxy Constructor Function
 * </ul>
 */
public final class ProxyConstructorFunction extends BuiltinConstructor implements Initializable,
        Creatable<ProxyObject> {
    /**
     * Constructs a new Proxy constructor function.
     * 
     * @param realm
     *            the realm object
     */
    public ProxyConstructorFunction(Realm realm) {
        super(realm, "Proxy", 2);
    }

    @Override
    public void initialize(ExecutionContext cx) {
        createProperties(cx, this, Properties.class);
    }

    @Override
    public ProxyConstructorFunction clone() {
        return new ProxyConstructorFunction(getRealm());
    }

    /**
     * 26.5.1.1 Proxy (target, handler)
     */
    @Override
    public ProxyObject call(ExecutionContext callerContext, Object thisValue, Object... args) {
        throw newTypeError(calleeContext(), Messages.Key.ProxyNew);
    }

    /**
     * 26.5.1.2 new Proxy ( target, handler )
     */
    @Override
    public ProxyObject construct(ExecutionContext callerContext, Object... args) {
        Object target = argument(args, 0);
        Object handler = argument(args, 1);
        /* step 1 */
        return ProxyCreate(callerContext, target, handler);
    }

    @Override
    public CreateAction<ProxyObject> createAction() {
        // FIXME: spec bug - not defined
        return null;
    }

    /**
     * 26.5.2 Properties of the Proxy Constructor Function
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.FunctionPrototype;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false,
                configurable = true))
        public static final int length = 2;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false,
                configurable = true))
        public static final String name = "Proxy";

        /**
         * 26.5.2.1 Proxy.revocable ( target, handler )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param target
         *            the proxy target object
         * @param handler
         *            the proxy handler object
         * @return the revocable proxy
         */
        @Function(name = "revocable", arity = 2)
        public static Object revocable(ExecutionContext cx, Object thisValue, Object target,
                Object handler) {
            /* steps 1-2 */
            ProxyObject p = ProxyCreate(cx, target, handler);
            /* steps 3-4 */
            ProxyRevocationFunction revoker = new ProxyRevocationFunction(cx.getRealm(), p);
            /* step 5 */
            OrdinaryObject result = ObjectCreate(cx, Intrinsics.ObjectPrototype);
            /* step 6 */
            CreateDataProperty(cx, result, "proxy", p);
            /* step 7 */
            CreateDataProperty(cx, result, "revoke", revoker);
            /* step 8 */
            return result;
        }
    }

    /**
     * <h1>26.5.2.1.1 Proxy Revocation Functions</h1>
     */
    private static final class ProxyRevocationFunction extends BuiltinFunction {
        /** [[RevokableProxy]] */
        private Ref<ProxyObject> revokableProxy;

        public ProxyRevocationFunction(Realm realm, ProxyObject revokableProxy) {
            this(realm, new Ref<>(revokableProxy));
            createDefaultFunctionProperties();
        }

        private ProxyRevocationFunction(Realm realm, Ref<ProxyObject> revokableProxy) {
            super(realm, ANONYMOUS, 0);
            this.revokableProxy = revokableProxy;
        }

        @Override
        public ProxyRevocationFunction clone() {
            return new ProxyRevocationFunction(getRealm(), revokableProxy);
        }

        @Override
        public Undefined call(ExecutionContext callerContext, Object thisValue, Object... args) {
            /* step 1 */
            Ref<ProxyObject> p = revokableProxy;
            /* step 2 */
            if (p == null || p.get() == null) {
                return UNDEFINED;
            }
            /* step 3 */
            revokableProxy = null;
            /* step 4 (implicit) */
            /* steps 5-6 */
            p.getAndClear().revoke();
            /* step 7 */
            return UNDEFINED;
        }
    }
}
