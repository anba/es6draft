/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.reflect;

import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Accessor;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.CompatibilityExtension;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.modules.Loader;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>26 Reflection</h1><br>
 * <h2>26.? Loader Objects</h2>
 * <ul>
 * <li>26.?.3 Properties of the Reflect.Loader Prototype Object
 * </ul>
 */
public final class LoaderPrototype extends OrdinaryObject implements Initializable {
    /**
     * Constructs a new Loader prototype object.
     * 
     * @param realm
     *            the realm object
     */
    public LoaderPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialize(Realm realm) {
        createProperties(realm, this, Properties.class);
        createProperties(realm, this, RealmProperty.class);
    }

    /**
     * 26.?.3 Properties of the Reflect.Loader Prototype Object
     */
    public enum Properties {
        ;

        /**
         * Abstract Operation: thisLoader(value)
         * 
         * @param cx
         *            the execution context
         * @param value
         *            the argument value
         * @param method
         *            the method
         * @return the loader object
         */
        private static LoaderObject thisLoader(ExecutionContext cx, Object value, String method) {
            if (value instanceof LoaderObject) {
                return (LoaderObject) value;
            }
            throw newTypeError(cx, Messages.Key.IncompatibleThis, method, Type.of(value).toString());
        }

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * 26.?.3.1 Reflect.Loader.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.Loader;

        /**
         * 26.?.3.6 get Reflect.Loader.prototype.global
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the global object
         */
        @Accessor(name = "global", type = Accessor.Type.Getter)
        public static Object global(ExecutionContext cx, Object thisValue) {
            /* steps 1-2 */
            LoaderObject loader = thisLoader(cx, thisValue, "Reflect.Loader.prototype.global");
            /* step 3 */
            Loader loaderRecord = loader.getLoader();
            /* steps 4-5 */
            return loaderRecord.getRealm().getGlobalThis();
        }

        /**
         * 26.?.3.17 Reflect.Loader.prototype [ @@toStringTag ]
         */
        @Value(name = "[Symbol.toStringTag]", symbol = BuiltinSymbol.toStringTag,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final String toStringTag = "Reflect.Loader";
    }

    /**
     * Additional Properties of the Reflect.Loader Prototype Object
     */
    @CompatibilityExtension(CompatibilityOption.Realm)
    public enum RealmProperty {
        ;

        /**
         * Abstract Operation: thisLoader(value)
         * 
         * @param cx
         *            the execution context
         * @param value
         *            the argument value
         * @param method
         *            the method
         * @return the loader object
         */
        private static LoaderObject thisLoader(ExecutionContext cx, Object value, String method) {
            if (value instanceof LoaderObject) {
                return (LoaderObject) value;
            }
            throw newTypeError(cx, Messages.Key.IncompatibleThis, method, Type.of(value).toString());
        }

        /**
         * 26.?.3.13 get Reflect.Loader.prototype.realm
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the realm object
         */
        @Accessor(name = "realm", type = Accessor.Type.Getter)
        public static Object realm(ExecutionContext cx, Object thisValue) {
            /* steps 1-2 */
            LoaderObject loader = thisLoader(cx, thisValue, "Reflect.Loader.prototype.realm");
            /* step 3 */
            Loader loaderRecord = loader.getLoader();
            /* step 4 */
            return loaderRecord.getRealm().getRealmObject();
        }
    }
}
