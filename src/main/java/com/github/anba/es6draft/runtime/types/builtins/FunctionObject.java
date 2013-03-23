/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import static com.github.anba.es6draft.runtime.types.Null.NULL;

import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;
import com.github.anba.es6draft.runtime.internal.SourceCompressor;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Property;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;

/**
 * <h1>8 Types</h1><br>
 * <h2>8.3 Ordinary Object Internal Methods and Internal Data Properties</h2>
 * <ul>
 * <li>8.3.15 Ordinary Function Objects
 * </ul>
 */
public abstract class FunctionObject extends OrdinaryObject implements ScriptObject, Callable {
    protected FunctionKind kind;
    protected RuntimeInfo.Function function;
    protected LexicalEnvironment scope;
    protected boolean strict;
    protected ScriptObject home;
    protected String methodName;
    protected Realm realm;
    protected ThisMode thisMode;
    protected String source = null;

    protected FunctionObject(Realm realm) {
        super(realm);
    }

    public enum FunctionKind {
        Normal, ConstructorMethod, Method, Arrow
    }

    public enum ThisMode {
        Lexical, Strict, Global
    }

    public static boolean isStrictFunction(Object v) {
        return v instanceof FunctionObject && ((FunctionObject) v).isStrict();
    }

    @Override
    public String toSource() {
        String source = this.source;
        if (source == null) {
            String src = function.source();
            if (src != null) {
                try {
                    source = SourceCompressor.decompress(src).call();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                source = "function F() { /* source not available */ }";
            }
            this.source = source;
        }
        return source;
    }

    /**
     * 8.3.15.3 [[Get]] (P, Receiver)
     */
    @Override
    public Object get(String propertyKey, Object receiver) {
        /* step 1-2 */
        Object v = super.get(propertyKey, receiver);
        /* step 3 */
        if ("caller".equals(propertyKey) && isStrictFunction(v)) {
            return NULL;
        }
        /* step 4 */
        return v;
    }

    // FIXME: spec bug (caption not updated from 8.3.19.4 to 8.3.15.4)
    /**
     * 8.3.15.4 [[GetOwnProperty]] (P)
     */
    @Override
    public Property getOwnProperty(String propertyKey) {
        /* step 1-2 */
        Property v = super.getOwnProperty(propertyKey);
        if (v != null && v.isDataDescriptor()) {
            if ("caller".equals(propertyKey) && isStrictFunction(v)) {
                PropertyDescriptor desc = v.toPropertyDescriptor();
                desc.setValue(NULL);
                v = desc.toProperty();
            }
        }
        return v;
    }

    public FunctionKind getFunctionKind() {
        return kind;
    }

    public RuntimeInfo.Function getFunction() {
        return function;
    }

    /**
     * [[Scope]]
     */
    public LexicalEnvironment getScope() {
        return scope;
    }

    /**
     * [[Code]]
     */
    public RuntimeInfo.Code getCode() {
        return function;
    }

    /**
     * [[Realm]]
     */
    public Realm getRealm() {
        return realm;
    }

    /**
     * [[ThisMode]]
     */
    public ThisMode getThisMode() {
        return thisMode;
    }

    /**
     * [[Strict]]
     */
    public boolean isStrict() {
        return strict;
    }

    /**
     * [[Home]]
     */
    public ScriptObject getHome() {
        return home;
    }

    /**
     * [[MethodName]]
     */
    public String getMethodName() {
        return methodName;
    }
}
