/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.zone;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.zone.ZoneConstructor.CallInZone;
import static com.github.anba.es6draft.runtime.types.Null.NULL;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Accessor;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;
import com.github.anba.es6draft.runtime.types.builtins.StringObject;

/**
 * <h1>Zones</h1><br>
 * <h2>Zone Objects</h2>
 * <ul>
 * <li>Properties of the Zone Prototype Object
 * </ul>
 */
public final class ZonePrototype extends OrdinaryObject implements Initializable {
    /**
     * Constructs a new Zone prototype object.
     * 
     * @param realm
     *            the realm object
     */
    public ZonePrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialize(Realm realm) {
        createProperties(realm, this, Properties.class);
    }

    /**
     * Properties of the Zone Prototype Object
     */
    public enum Properties {
        ;

        private static ZoneObject thisZoneObject(ExecutionContext cx, Object value, String method) {
            if (value instanceof ZoneObject) {
                return (ZoneObject) value;
            }
            throw newTypeError(cx, Messages.Key.IncompatibleThis, method, Type.of(value).toString());
        }

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * Zone.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.Zone;

        /**
         * get Zone.prototype.parent
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the current zone object
         */
        @Accessor(name = "parent", type = Accessor.Type.Getter)
        public static Object parent(ExecutionContext cx, Object thisValue) {
            /* steps 1-3 */
            ZoneObject zone = thisZoneObject(cx, thisValue, "Zone.prototype.parent");
            /* step 4 */
            ZoneObject parent = zone.getParentZone();
            return parent != null ? parent : NULL;
        }

        /**
         * Zone.prototype.fork ( options )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param options
         *            the options object
         * @return the new zone object
         */
        @Function(name = "fork", arity = 1)
        public static Object fork(ExecutionContext cx, Object thisValue, Object options) {
            /* steps 1-3 */
            ZoneObject zone = thisZoneObject(cx, thisValue, "Zone.prototype.fork");
            /* step 4 */
            CharSequence name = null;
            /* step 5 */
            if (Type.isUndefined(options)) {
                options = ObjectCreate(cx, Intrinsics.ObjectPrototype);
            }
            /* step 6 */
            RequireObjectCoercible(cx, options);
            /* step 7 */
            Object nameOption = GetV(cx, options, "name");
            /* steps 8-9 */
            if (Type.isUndefined(nameOption)) {
                /* step 8 */
                CharSequence parentName = ToString(cx, Get(cx, zone, "name"));
                name = StringObject.validateLength(cx, parentName + " child");
            } else {
                /* step 9 */
                name = ToString(cx, nameOption);
            }
            /* step 10 */
            OrdinaryObject constructorOptions = ObjectCreate(cx, Intrinsics.ObjectPrototype);
            /* step 11 */
            CreateDataProperty(cx, constructorOptions, "parent", zone);
            /* step 12 */
            CreateDataProperty(cx, constructorOptions, "name", name);
            /* step 13 */
            Object constructor = Get(cx, zone, "constructor");
            /* step 14 */
            if (!IsConstructor(constructor)) {
                throw newTypeError(cx, Messages.Key.NotConstructor);
            }
            return Construct(cx, (Constructor) constructor, constructorOptions);
        }

        /**
         * Zone.prototype.run ( callback )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param callback
         *            the callback function
         * @return the new zone object
         */
        @Function(name = "run", arity = 1)
        public static Object run(ExecutionContext cx, Object thisValue, Object callback) {
            /* steps 1-3 */
            ZoneObject zone = thisZoneObject(cx, thisValue, "Zone.prototype.run");
            /* step 4 */
            if (!IsCallable(callback)) {
                throw newTypeError(cx, Messages.Key.NotCallable);
            }
            /* step 5 */
            return CallInZone(cx, zone, (Callable) callback, UNDEFINED);
        }

        /**
         * Zone.prototype.wrap ( callback )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param callback
         *            the callback function
         * @return the new zone object
         */
        @Function(name = "wrap", arity = 1)
        public static Object wrap(ExecutionContext cx, Object thisValue, Object callback) {
            /* steps 1-3 */
            ZoneObject zone = thisZoneObject(cx, thisValue, "Zone.prototype.wrap");
            if (!IsCallable(callback)) {
                throw newTypeError(cx, Messages.Key.NotCallable);
            }
            /* steps 5-6 */
            ZonePrototypeWrapWrapper wrapped = new ZonePrototypeWrapWrapper(cx.getRealm(), zone, (Callable) callback);
            /* step 7 */
            return wrapped;
        }
    }

    /**
     * Zone.prototype.wrap Wrapper Functions
     */
    public static final class ZonePrototypeWrapWrapper extends BuiltinFunction {
        /** [[Zone]] */
        private final ZoneObject zone;
        /** [[Callback]] */
        private final Callable callback;

        public ZonePrototypeWrapWrapper(Realm realm, ZoneObject zone, Callable callback) {
            super(realm, ANONYMOUS, 1);
            this.zone = zone;
            this.callback = callback;
            createDefaultFunctionProperties();
        }

        @Override
        public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
            ExecutionContext calleeContext = calleeContext();
            Object thisArg = argument(args, 0);
            Object[] argumentsList = CreateListFromArrayLike(calleeContext, argument(args, 1));

            /* step 1 (implicit) */
            /* step 2 */
            ZoneObject zone = this.zone;
            /* step 3 (implicit) */
            /* step 4 */
            Callable callback = this.callback;
            /* step 5 (implicit) */
            /* step 6 */
            return CallInZone(calleeContext, zone, callback, thisArg, argumentsList);
        }
    }
}
