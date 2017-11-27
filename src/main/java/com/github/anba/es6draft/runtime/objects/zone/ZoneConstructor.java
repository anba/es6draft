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

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Accessor;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Property;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinConstructor;

/**
 * <h1>Zones</h1><br>
 * <h2>Zone Objects</h2>
 * <ul>
 * <li>Zone Abstract Operations
 * <li>The Zone Constructor
 * <li>Properties of the Zone Constructor
 * </ul>
 */
public final class ZoneConstructor extends BuiltinConstructor implements Initializable {
    /**
     * Constructs a new Zone constructor function.
     * 
     * @param realm
     *            the realm object
     */
    public ZoneConstructor(Realm realm) {
        super(realm, "Zone", 1);
    }

    @Override
    public void initialize(Realm realm) {
        createProperties(realm, this, Properties.class);

        ZoneObject initialZone = new ZoneObject(realm, null, realm.getIntrinsic(Intrinsics.ZonePrototype));
        initialZone.infallibleDefineOwnProperty("name", new Property("(root zone)", false, false, true));

        realm.setCurrentZone(initialZone);
    }

    /**
     * CallInZone ( zone, callback, thisArg, argumentsList )
     * 
     * @param cx
     *            the execution context
     * @param zone
     *            the zone object
     * @param callback
     *            the callback function
     * @param thisArg
     *            the function this-value
     * @param argumentsList
     *            the function arguments
     * @return the function return value
     */
    public static Object CallInZone(ExecutionContext cx, ZoneObject zone, Callable callback, Object thisArg,
            Object... argumentsList) {
        /* step 1 */
        Realm zoneRealm = zone.getRealm();
        /* step 2 */
        ZoneObject beforeZone = zoneRealm.getCurrentZone();
        /* steps 3-6 */
        try {
            /* step 3 */
            zoneRealm.setCurrentZone(zone);
            /* steps 4, 6 */
            return Call(cx, callback, thisArg, argumentsList);
        } finally {
            /* step 5 */
            zoneRealm.setCurrentZone(beforeZone);
        }
    }

    /**
     * Zone ( options )
     */
    @Override
    public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
        /* step 1 */
        throw newTypeError(calleeContext(), Messages.Key.InvalidCall, "Zone");
    }

    /**
     * Zone ( options )
     */
    @Override
    public ZoneObject construct(ExecutionContext callerContext, Constructor newTarget, Object... args) {
        ExecutionContext calleeContext = calleeContext();
        Object options = argument(args, 0);

        /* step 1 (not applicable) */
        /* step 2 */
        Realm functionRealm = getRealm();
        /* step 3 */
        CharSequence name = "(unnamed zone)";
        /* step 4 */
        ZoneObject parent = null;
        /* step 5 */
        if (Type.isUndefined(options)) {
            options = ObjectCreate(calleeContext, Intrinsics.ObjectPrototype);
        }
        /* step 6 */
        RequireObjectCoercible(calleeContext, options);
        /* step 7 */
        Object nameOption = GetV(calleeContext, options, "name");
        /* step 8 */
        if (!Type.isUndefined(nameOption)) {
            name = ToString(calleeContext, nameOption);
        }
        /* step 9 */
        Object parentOption = GetV(calleeContext, options, "parent");
        /* steps 10-11 */
        if (!Type.isUndefinedOrNull(parentOption)) {
            if (!(parentOption instanceof ZoneObject)) {
                throw newTypeError(calleeContext, Messages.Key.IncompatibleObject);
            }
            parent = (ZoneObject) parentOption;
        }
        /* steps 12-14 */
        ZoneObject zone = new ZoneObject(functionRealm, parent,
                GetPrototypeFromConstructor(calleeContext, newTarget, Intrinsics.ZonePrototype));
        /* step 15 */
        DefinePropertyOrThrow(calleeContext, zone, "name", new PropertyDescriptor(name, false, false, true));
        /* step 16 (not applicable) */
        /* step 17 */
        return zone;
    }

    /**
     * Properties of the Zone Constructor
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.FunctionPrototype;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final int length = 1;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final String name = "Zone";

        /**
         * Zone.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static final Intrinsics prototype = Intrinsics.ZonePrototype;

        /**
         * get Zone.current
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the current zone object
         */
        @Accessor(name = "current", type = Accessor.Type.Getter)
        public static Object current(ExecutionContext cx, Object thisValue) {
            /* steps 1-2 */
            if (!IsCallable(thisValue)) {
                throw newTypeError(cx, Messages.Key.NotCallable);
            }
            /* step 3 */
            Realm thisRealm = GetFunctionRealm(cx, (Callable) thisValue);
            /* step 4 */
            return thisRealm.getCurrentZone();
        }
    }
}
