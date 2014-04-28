/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.reflect;

import static com.github.anba.es6draft.runtime.AbstractOperations.CreateDataProperty;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.ExoticProxy.ProxyCreate;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.AddRestrictedFunctionProperties;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Undefined;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinConstructor;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinFunction;
import com.github.anba.es6draft.runtime.types.builtins.ExoticProxy;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>26 Reflection</h1><br>
 * <h2>26.5 Proxy Objects</h2>
 * <ul>
 * <li>26.5.1 The Proxy Constructor Function
 * <li>26.5.2 Properties of the Proxy Constructor Function
 * </ul>
 */
public final class ProxyConstructorFunction extends BuiltinConstructor implements Initializable {
    public ProxyConstructorFunction(Realm realm) {
        super(realm, "Proxy");
    }

    @Override
    public void initialize(ExecutionContext cx) {
        createProperties(cx, this, Properties.class);
        AddRestrictedFunctionProperties(cx, this);
    }

    @Override
    public ProxyConstructorFunction clone() {
        return new ProxyConstructorFunction(getRealm());
    }

    /**
     * 26.5.1.1 Proxy (target, handler)
     */
    @Override
    public ExoticProxy call(ExecutionContext callerContext, Object thisValue, Object... args) {
        throw newTypeError(calleeContext(), Messages.Key.ProxyNew);
    }

    /**
     * 26.5.1.2 new Proxy ( target, handler )
     */
    @Override
    public ExoticProxy construct(ExecutionContext callerContext, Object... args) {
        Object target = args.length > 0 ? args[0] : UNDEFINED;
        Object handler = args.length > 1 ? args[1] : UNDEFINED;
        /* step 1 */
        return ProxyCreate(callerContext, target, handler);
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
            ExoticProxy p = ProxyCreate(cx, target, handler);
            /* steps 3-4 */
            ProxyRevocationFunction revoker = new ProxyRevocationFunction(cx.getRealm(), p);
            /* step 5 */
            OrdinaryObject result = ObjectCreate(cx);
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
        private ExoticProxy revokableProxy;

        public ProxyRevocationFunction(Realm realm, ExoticProxy revokableProxy) {
            this(realm, revokableProxy, null);
            createDefaultFunctionProperties(ANONYMOUS, 0);
        }

        private ProxyRevocationFunction(Realm realm, ExoticProxy revokableProxy, Void ignore) {
            super(realm, ANONYMOUS);
            this.revokableProxy = revokableProxy;
        }

        @Override
        public ProxyRevocationFunction clone() {
            return new ProxyRevocationFunction(getRealm(), revokableProxy, null);
        }

        @Override
        public Undefined call(ExecutionContext callerContext, Object thisValue, Object... args) {
            /* step 1 */
            ExoticProxy p = revokableProxy;
            /* step 2 */
            if (p == null) {
                return UNDEFINED;
            }
            /* step 3 */
            revokableProxy = null;
            /* step 4 (implicit) */
            /* steps 5-6 */
            p.revoke();
            /* step 7 */
            return UNDEFINED;
        }
    }
}
