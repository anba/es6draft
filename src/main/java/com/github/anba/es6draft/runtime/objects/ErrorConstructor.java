/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import static com.github.anba.es6draft.runtime.AbstractOperations.ToString;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.AddRestrictedFunctionProperties;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.BuiltinBrand;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.Scriptable;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>15 Standard Built-in ECMAScript Objects</h1><br>
 * <h2>15.11 Error Objects</h2>
 * <ul>
 * <li>15.11.1 The Error Constructor Called as a Function
 * <li>15.11.2 The Error Constructor
 * <li>15.11.3 Properties of the Error Constructor
 * </ul>
 */
public class ErrorConstructor extends OrdinaryObject implements Scriptable, Callable, Constructor,
        Initialisable {
    public ErrorConstructor(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(Realm realm) {
        createProperties(this, realm, Properties.class);
        AddRestrictedFunctionProperties(realm, this);
    }

    /**
     * [[BuiltinBrand]]
     */
    @Override
    public BuiltinBrand getBuiltinBrand() {
        return BuiltinBrand.BuiltinFunction;
    }

    @Override
    public String toSource() {
        return "function Error() { /* native code */ }";
    }

    /**
     * 15.11.1.1 Error (message)
     */
    @Override
    public Object call(Object thisValue, Object... args) {
        Object message = args.length > 0 ? args[0] : UNDEFINED;
        ErrorObject obj = new ErrorObject(realm());
        obj.setPrototype(realm().getIntrinsic(Intrinsics.ErrorPrototype));
        if (!Type.isUndefined(message)) {
            CharSequence msg = ToString(realm(), message);
            obj.defineOwnProperty("message", new PropertyDescriptor(msg, true, false, true));
        }
        return obj;
    }

    /**
     * 15.11.2.1 new Error (message)
     */
    @Override
    public Object construct(Object... args) {
        Object message = args.length > 0 ? args[0] : UNDEFINED;
        ErrorObject obj = new ErrorObject(realm());
        obj.setPrototype(realm().getIntrinsic(Intrinsics.ErrorPrototype));
        if (!Type.isUndefined(message)) {
            CharSequence msg = ToString(realm(), message);
            obj.defineOwnProperty("message", new PropertyDescriptor(msg, true, false, true));
        }
        return obj;
    }

    /**
     * 15.11.3 Properties of the Error Constructor
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.FunctionPrototype;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final int length = 1;

        /**
         * 15.11.3.1 Error.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.ErrorPrototype;
    }
}
