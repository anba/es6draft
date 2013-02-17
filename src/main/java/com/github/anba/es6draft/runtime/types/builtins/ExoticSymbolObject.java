/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.BuiltinBrand;
import com.github.anba.es6draft.runtime.types.Property;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.Scriptable;
import com.github.anba.es6draft.runtime.types.Symbol;

/**
 * <h1>8 Types</h1><br>
 * <h2>8.4 Built-in Exotic Object Internal Methods and Data Fields</h2>
 * <ul>
 * <li>8.4.4 Exotic Symbol Objects
 * </ul>
 */
public class ExoticSymbolObject implements Scriptable, Symbol {

    /**
     * [[Private]]
     */
    @SuppressWarnings("unused")
    private final Object _private;

    private final boolean isPrivate;

    private final String name;

    public ExoticSymbolObject(String name, Object _private) {
        this.name = name;
        this._private = _private;
        this.isPrivate = true;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean isPrivate() {
        return isPrivate;
    }

    @Override
    public Scriptable newInstance(Realm realm) {
        throw new IllegalStateException();
    }

    /**
     * [[BuiltinBrand]]
     */
    @Override
    public BuiltinBrand getBuiltinBrand() {
        return null;
    }

    /**
     * 8.4.4.1 [[GetInheritance]] ( )
     */
    @Override
    public Scriptable getPrototype() {
        return null;
    }

    /**
     * 8.4.4.2 [[SetInheritance]] (V)
     */
    @Override
    public boolean setPrototype(Scriptable prototype) {
        return false;
    }

    /**
     * 8.4.4.3 [[IsExtensible]] ( )
     */
    @Override
    public boolean isExtensible() {
        return false;
    }

    /**
     * 8.4.4.4 [[PreventExtensions]] ( )
     */
    @Override
    public void preventExtensions() {
    }

    /**
     * 8.4.4.5 [[HasOwnProperty]] (P)
     */
    @Override
    public boolean hasOwnProperty(String propertyKey) {
        return false;
    }

    /**
     * 8.4.4.5 [[HasOwnProperty]] (P)
     */
    @Override
    public boolean hasOwnProperty(Symbol propertyKey) {
        return false;
    }

    /**
     * 8.4.4.6 [[GetOwnProperty]] (P)
     */
    @Override
    public Property getOwnProperty(String propertyKey) {
        return null;
    }

    /**
     * 8.4.4.6 [[GetOwnProperty]] (P)
     */
    @Override
    public Property getOwnProperty(Symbol propertyKey) {
        return null;
    }

    /**
     * 8.4.4.7 [[DefineOwnProperty]] (P, Desc)
     */
    @Override
    public boolean defineOwnProperty(String propertyKey, PropertyDescriptor desc) {
        return false;
    }

    /**
     * 8.4.4.7 [[DefineOwnProperty]] (P, Desc)
     */
    @Override
    public boolean defineOwnProperty(Symbol propertyKey, PropertyDescriptor desc) {
        return false;
    }

    /**
     * 8.4.4.8 [[HasProperty]] (P)
     */
    @Override
    public boolean hasProperty(String propertyKey) {
        return false;
    }

    /**
     * 8.4.4.8 [[HasProperty]] (P)
     */
    @Override
    public boolean hasProperty(Symbol propertyKey) {
        return false;
    }

    /**
     * [[GetP]] (P, Receiver)
     */
    @Override
    public Object get(String propertyKey, Object receiver) {
        if ("toString".equals(propertyKey)) {
            // TODO: implement
            assert false : "NYI";
        }
        return UNDEFINED;
    }

    /**
     * [[GetP]] (P, Receiver)
     */
    @Override
    public Object get(Symbol propertyKey, Object receiver) {
        return UNDEFINED;
    }

    /**
     * 8.4.4.8 [[SetP] ( P, V, Receiver)
     */
    @Override
    public boolean set(String propertyKey, Object value, Object receiver) {
        return false;
    }

    /**
     * 8.4.4.8 [[SetP] ( P, V, Receiver)
     */
    @Override
    public boolean set(Symbol propertyKey, Object value, Object receiver) {
        return false;
    }

    /**
     * 8.4.4.9 [[Delete]] (P)
     */
    @Override
    public boolean delete(String propertyKey) {
        return true;
    }

    /**
     * 8.4.4.9 [[Delete]] (P)
     */
    @Override
    public boolean delete(Symbol propertyKey) {
        return true;
    }

    /**
     * 8.4.4.11 [[Enumerate]] ()
     */
    @Override
    public Scriptable enumerate() {
        // FIXME: spec incomplete
        throw new IllegalStateException("NYI");
    }

    /**
     * 8.4.4.13 [[OwnPropertyKeys]] ( )
     */
    @Override
    public Scriptable ownPropertyKeys() {
        // FIXME: spec incomplete
        throw new IllegalStateException("NYI");
    }

    /**
     * 8.4.4.14 [[Freeze]] ( )
     */
    @Override
    public void freeze() {
        // FIXME: spec bug (return type)
    }

    /**
     * 8.4.4.15 [[Seal]] ( )
     */
    @Override
    public void seal() {
        // FIXME: spec bug (return type)
    }

    /**
     * 8.4.4.16 [[IsFrozen]] ( )
     */
    @Override
    public boolean isFrozen() {
        return true;
    }

    /**
     * 8.4.4.17 [[IsSealed]] ( )
     */
    @Override
    public boolean isSealed() {
        return true;
    }

}
