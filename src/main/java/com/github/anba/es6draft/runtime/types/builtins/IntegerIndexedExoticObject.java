/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.ScriptObject;

/**
 * <h1>8 Types</h1><br>
 * <h2>8.4 Built-in Exotic Object Internal Methods and Data Fields</h2>
 * <ul>
 * <li>8.4.5 Integer Indexed Delegation Exotic Objects
 * </ul>
 */
public class IntegerIndexedExoticObject extends OrdinaryObject {
    public IntegerIndexedExoticObject(Realm realm) {
        super(realm);
    }

    public static double toIntegerIndex(String propertyKey) {
        double intIndex = ToInteger(ToNumber(propertyKey));
        if (ToString(intIndex).equals(propertyKey)) {
            return intIndex;
        }
        return Double.NaN;
    }

    // FIXME: spec bug - needs overrides for [[HasOwnProperty]] etc.

    /**
     * 8.4.5.1 [[Get]] (P, Receiver)
     */
    @Override
    public Object get(ExecutionContext cx, String propertyKey, Object receiver) {
        if (SameValue(this, receiver)) {
            double intIndex = toIntegerIndex(propertyKey);
            if (!Double.isNaN(intIndex)) {
                return Invoke(cx, this, BuiltinSymbol.elementGet.get(), intIndex);
            }
        }
        return super.get(cx, propertyKey, receiver);
    }

    /**
     * 8.4.5.2 [[Set]] ( P, V, Receiver)
     */
    @Override
    public boolean set(ExecutionContext cx, String propertyKey, Object value, Object receiver) {
        if (SameValue(this, receiver)) {
            double intIndex = toIntegerIndex(propertyKey);
            if (!Double.isNaN(intIndex)) {
                return ToBoolean(Invoke(cx, this, BuiltinSymbol.elementSet.get(), intIndex, value));
            }
        }
        return super.set(cx, propertyKey, value, receiver);
    }

    /**
     * 8.4.5.3 IntegerIndexedObjectCreate Abstract Operation
     */
    public static ScriptObject IntegerIndexedObjectCreate(ExecutionContext cx,
            ScriptObject prototype) {
        /* step 1-4 (implicit) */
        ScriptObject obj = new IntegerIndexedExoticObject(cx.getRealm());
        /* step 5 */
        obj.setInheritance(cx, prototype);
        /* step 6 (implicit) */
        // obj.[[Extensible]] = true;
        return obj;
    }
}
