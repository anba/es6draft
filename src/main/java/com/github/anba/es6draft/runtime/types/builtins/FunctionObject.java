/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import static com.github.anba.es6draft.runtime.types.Null.NULL;

import java.util.Collection;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;
import com.github.anba.es6draft.runtime.internal.SourceCompressor;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Property;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;

/**
 * <h1>9 ECMAScript Ordinary and Exotic Objects Behaviours</h1><br>
 * <h2>9.1 Ordinary Object Internal Methods and Internal Data Properties</h2>
 * <ul>
 * <li>9.1.16 Ordinary Function Objects
 * </ul>
 */
public abstract class FunctionObject extends OrdinaryObject implements Callable {
    private static final String SOURCE_NOT_AVAILABLE = "function F() { /* source not available */ }";

    /** [[Scope]] */
    protected LexicalEnvironment scope;
    /** [[FunctionKind]] */
    protected FunctionKind functionKind;
    /** [[FormalParameters]] / [[Code]] */
    protected RuntimeInfo.Function function;
    /** [[Realm]] */
    protected Realm realm;
    /** [[ThisMode]] */
    protected ThisMode thisMode;
    /** [[Strict]] */
    protected boolean strict;
    /** [[HomeObject]] */
    protected ScriptObject homeObject;
    /** [[MethodName]] */
    protected Object /* String|ExoticSymbol */methodName;

    protected boolean isConstructor = false;
    protected boolean legacy;
    protected String source = null;

    protected Property caller = new PropertyDescriptor(NULL, false, false, false).toProperty();
    protected Property arguments = new PropertyDescriptor(NULL, false, false, false).toProperty();

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
        if (!isInitialised()) {
            return SOURCE_NOT_AVAILABLE;
        }
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
                source = SOURCE_NOT_AVAILABLE;
            }
            this.source = source;
        }
        return source;
    }

    @Override
    public boolean defineOwnProperty(ExecutionContext cx, String propertyKey,
            PropertyDescriptor desc) {
        Property current = getOwnProperty(cx, propertyKey);
        boolean extensible = isExtensible();
        return ValidateAndApplyPropertyDescriptor(this, propertyKey, extensible, desc, current);
    }

    @Override
    public boolean hasOwnProperty(ExecutionContext cx, String propertyKey) {
        boolean has = super.hasOwnProperty(cx, propertyKey);
        if (has) {
            return true;
        }
        if (isLegacy() && ("caller".equals(propertyKey) || "arguments".equals(propertyKey))) {
            return true;
        }
        return false;
    }

    @Override
    protected Collection<Object> enumerateOwnKeys() {
        Collection<Object> ownKeys = super.enumerateOwnKeys();
        if (isLegacy()) {
            ownKeys.add("caller");
            ownKeys.add("arguments");
        }
        return ownKeys;
    }

    /**
     * 9.1.16.3 [[Get]] (P, Receiver)
     */
    @Override
    public Object get(ExecutionContext cx, String propertyKey, Object receiver) {
        // no override necessary (!)
        return super.get(cx, propertyKey, receiver);
    }

    /**
     * 9.1.16.4 [[GetOwnProperty]] (P)
     */
    @Override
    public Property getOwnProperty(ExecutionContext cx, String propertyKey) {
        Property desc = super.getOwnProperty(cx, propertyKey);
        if (desc != null) {
            return desc;
        }
        if (isLegacy()) {
            if ("caller".equals(propertyKey)) {
                assert !isStrictFunction(caller.getValue());
                return caller;
            }
            if ("arguments".equals(propertyKey)) {
                return arguments;
            }
        }
        return null;
    }

    /**
     * Returns a copy of this function object with the [[HomeObject]] property set to
     * {@code newHomeObject}
     */
    public abstract FunctionObject rebind(ExecutionContext cx, ScriptObject newHomeObject);

    public final FunctionKind getFunctionKind() {
        return functionKind;
    }

    public final RuntimeInfo.Function getFunction() {
        return function;
    }

    public final boolean isInitialised() {
        return function != null;
    }

    public final boolean isLegacy() {
        return legacy;
    }

    /**
     * [[Scope]]
     */
    public final LexicalEnvironment getScope() {
        return scope;
    }

    /**
     * [[Code]]
     */
    public final RuntimeInfo.Code getCode() {
        return function;
    }

    /**
     * [[Realm]]
     */
    public final Realm getRealm() {
        return realm;
    }

    /**
     * [[ThisMode]]
     */
    public final ThisMode getThisMode() {
        return thisMode;
    }

    /**
     * [[Strict]]
     */
    public final boolean isStrict() {
        return strict;
    }

    /**
     * [[HomeObject]]
     */
    public final ScriptObject getHomeObject() {
        return homeObject;
    }

    /**
     * [[MethodName]]
     */
    public final Object getMethodName() {
        return methodName;
    }
}
