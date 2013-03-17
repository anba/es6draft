/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import static com.github.anba.es6draft.runtime.AbstractOperations.Invoke;
import static com.github.anba.es6draft.runtime.AbstractOperations.SameValue;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToBoolean;
import static com.github.anba.es6draft.runtime.types.builtins.ExoticArray.isArrayIndex;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.ScriptObject;

/**
 * <h1>8 Types</h1><br>
 * <h2>8.4 Built-in Exotic Object Internal Methods and Data Fields</h2>
 * <ul>
 * <li>8.4.6 Indexed Delegation Exotic Objects
 * </ul>
 */
public class IndexedDelegationExoticObject extends OrdinaryObject {
    public IndexedDelegationExoticObject(Realm realm) {
        super(realm);
    }

    /**
     * 8.4.6.1 [[Get]] (P, Receiver)
     */
    @Override
    public Object get(String propertyKey, Object receiver) {
        if (SameValue(this, receiver) && isArrayIndex(propertyKey)) {
            // FIXME: spec bug (variable 'index' not defined) (bug 1207)
            return Invoke(realm(), this, BuiltinSymbol.elementGet.get(), propertyKey);
        }
        return super.get(propertyKey, receiver);
    }

    /**
     * 8.4.6.2 [[Set]] ( P, V, Receiver)
     */
    @Override
    public boolean set(String propertyKey, Object value, Object receiver) {
        if (SameValue(this, receiver) && isArrayIndex(propertyKey)) {
            // FIXME: spec bug (variable 'index' not defined) (bug 1207)
            // FIXME: spec bug (missing ToBoolean() conversion?) (bug 1207)
            return ToBoolean(Invoke(realm(), this, BuiltinSymbol.elementSet.get(), propertyKey,
                    value));
        }
        return super.set(propertyKey, value, receiver);
    }

    /**
     * 8.4.6.3 IndexedDelegator Create Abstract Operation
     */
    public static ScriptObject IndexedDelegatorCreate(Realm realm, ScriptObject prototype) {
        // FIXME: spec bug stray '(' is introductory text (bug 1172)
        /* step 1-4 (implicit) */
        ScriptObject obj = new IndexedDelegationExoticObject(realm);
        /* step 5 */
        obj.setPrototype(prototype);
        /* step 6 (implicit) */
        // obj.[[Extensible]] = true;
        return obj;
    }
}
