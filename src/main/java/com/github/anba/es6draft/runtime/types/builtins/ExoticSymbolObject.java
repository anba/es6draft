/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.ListIterator.MakeListIterator;
import static java.util.Collections.emptyIterator;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.IntegrityLevel;
import com.github.anba.es6draft.runtime.types.Property;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Symbol;

/**
 * <h1>8 Types</h1><br>
 * <h2>8.4 Built-in Exotic Object Internal Methods and Data Fields</h2>
 * <ul>
 * <li>8.4.4 Exotic Symbol Objects
 * </ul>
 */
public class ExoticSymbolObject implements ScriptObject, Symbol {
    /** [[Private]] */
    private final boolean _private;

    private final String name;

    public ExoticSymbolObject(String name, boolean _private) {
        this.name = name;
        this._private = _private;
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * [[Private]]
     */
    @Override
    public boolean isPrivate() {
        return _private;
    }

    /**
     * 8.4.4.1 [[GetInheritance]] ( )
     */
    @Override
    public ScriptObject getPrototype(Realm realm) {
        return null;
    }

    /**
     * 8.4.4.2 [[SetInheritance]] (V)
     */
    @Override
    public boolean setPrototype(Realm realm, ScriptObject prototype) {
        return false;
    }

    /**
     * 8.4.4.3 [[HasIntegrity]] ( Level )
     */
    @Override
    public boolean hasIntegrity(Realm realm, IntegrityLevel level) {
        return true;
    }

    /**
     * 8.4.4.4 [[setIntegrity]] ( Level )
     */
    @Override
    public boolean setIntegrity(Realm realm, IntegrityLevel level) {
        return true;
    }

    /**
     * 8.4.4.5 [[HasOwnProperty]] (P)
     */
    @Override
    public boolean hasOwnProperty(Realm realm, String propertyKey) {
        return false;
    }

    /**
     * 8.4.4.5 [[HasOwnProperty]] (P)
     */
    @Override
    public boolean hasOwnProperty(Realm realm, Symbol propertyKey) {
        return false;
    }

    /**
     * 8.4.4.6 [[GetOwnProperty]] (P)
     */
    @Override
    public Property getOwnProperty(Realm realm, String propertyKey) {
        return null;
    }

    /**
     * 8.4.4.6 [[GetOwnProperty]] (P)
     */
    @Override
    public Property getOwnProperty(Realm realm, Symbol propertyKey) {
        return null;
    }

    /**
     * 8.4.4.7 [[DefineOwnProperty]] (P, Desc)
     */
    @Override
    public boolean defineOwnProperty(Realm realm, String propertyKey, PropertyDescriptor desc) {
        return false;
    }

    /**
     * 8.4.4.7 [[DefineOwnProperty]] (P, Desc)
     */
    @Override
    public boolean defineOwnProperty(Realm realm, Symbol propertyKey, PropertyDescriptor desc) {
        return false;
    }

    /**
     * 8.4.4.8 [[HasProperty]] (P)
     */
    @Override
    public boolean hasProperty(Realm realm, String propertyKey) {
        return false;
    }

    /**
     * 8.4.4.8 [[HasProperty]] (P)
     */
    @Override
    public boolean hasProperty(Realm realm, Symbol propertyKey) {
        return false;
    }

    /**
     * [[Get]] (P, Receiver)
     */
    @Override
    public Object get(Realm realm, String propertyKey, Object receiver) {
        if ("toString".equals(propertyKey)) {
            // TODO: implement
        }
        return UNDEFINED;
    }

    /**
     * [[Get]] (P, Receiver)
     */
    @Override
    public Object get(Realm realm, Symbol propertyKey, Object receiver) {
        return UNDEFINED;
    }

    /**
     * 8.4.4.8 [[Set] ( P, V, Receiver)
     */
    @Override
    public boolean set(Realm realm, String propertyKey, Object value, Object receiver) {
        return false;
    }

    /**
     * 8.4.4.8 [[Set] ( P, V, Receiver)
     */
    @Override
    public boolean set(Realm realm, Symbol propertyKey, Object value, Object receiver) {
        return false;
    }

    /**
     * 8.4.4.9 [[Delete]] (P)
     */
    @Override
    public boolean delete(Realm realm, String propertyKey) {
        return true;
    }

    /**
     * 8.4.4.9 [[Delete]] (P)
     */
    @Override
    public boolean delete(Realm realm, Symbol propertyKey) {
        return true;
    }

    /**
     * 8.4.4.11 [[Enumerate]] ()
     */
    @Override
    public ScriptObject enumerate(Realm realm) {
        // FIXME: spec incomplete
        return MakeListIterator(realm, emptyIterator());
    }

    /**
     * 8.4.4.13 [[OwnPropertyKeys]] ( )
     */
    @Override
    public ScriptObject ownPropertyKeys(Realm realm) {
        // FIXME: spec incomplete
        return MakeListIterator(realm, emptyIterator());
    }
}
