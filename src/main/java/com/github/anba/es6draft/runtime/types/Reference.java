/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types;

import static com.github.anba.es6draft.runtime.AbstractOperations.Put;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToObject;
import static com.github.anba.es6draft.runtime.internal.ScriptRuntime.throwReferenceError;
import static com.github.anba.es6draft.runtime.internal.ScriptRuntime.throwTypeError;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import com.github.anba.es6draft.runtime.EnvironmentRecord;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.builtins.ExoticString;

/**
 * <h1>8 Types</h1><br>
 * <h2>8.2 ECMAScript Specification Types</h2>
 * <ul>
 * <li>8.2.4 The Reference Specification Type
 * </ul>
 */
public final class Reference {
    private final Object base;
    private final Type type;
    private final String referencedName;
    private final boolean strictReference;
    private final Object thisValue;

    public Reference(Object base, String referencedName, boolean strictReference) {
        this(base, referencedName, strictReference, null);
    }

    public Reference(Object base, String referencedName, boolean strictReference, Object thisValue) {
        if (base == null) {
            base = UNDEFINED;
        }
        this.base = base;
        this.type = (base instanceof EnvironmentRecord ? null : Type.of(base));
        this.referencedName = referencedName;
        this.strictReference = strictReference;
        this.thisValue = thisValue;
    }

    /**
     * GetBase(V)
     */
    public Object getBase() {
        return base;
    }

    /**
     * GetReferencedName(V)
     */
    public String getReferencedName() {
        return referencedName;
    }

    /**
     * IsStrictReference(V)
     */
    public boolean isStrictReference() {
        return strictReference;
    }

    /**
     * HasPrimitiveBase(V)
     */
    public boolean hasPrimitiveBase() {
        return type == Type.Boolean || type == Type.String || type == Type.Number;
    }

    /**
     * IsPropertyReference(V)
     */
    public boolean isPropertyReference() {
        return type == Type.Object || hasPrimitiveBase();
    }

    /**
     * IsUnresolvableReference(V)
     */
    public boolean isUnresolvableReference() {
        return type == Type.Undefined;
    }

    /**
     * IsSuperReference(V)
     */
    public boolean isSuperReference() {
        return thisValue != null;
    }

    /**
     * [8.2.4.1] GetValue (V)
     */
    public static Object GetValue(Object v, Realm realm) {
        if (!(v instanceof Reference))
            return v;
        return ((Reference) v).GetValue(realm);
    }

    /**
     * [8.2.4.1] GetValue (V)
     */
    public Object GetValue(Realm realm) {
        Object base = getBase();
        if (isUnresolvableReference()) {
            throw throwReferenceError(realm, String.format("'%s' is not defined", referencedName));
        } else if (isPropertyReference()) {
            if (hasPrimitiveBase()) {
                // base = ToObject(realm, base);
                return GetValuePrimitive(realm);
            }
            return ((Scriptable) base).get(getReferencedName(), GetThisValue(realm));
        } else {
            return ((EnvironmentRecord) base).getBindingValue(getReferencedName(),
                    isStrictReference());
        }
    }

    private Object GetValuePrimitive(Realm realm) {
        Scriptable proto;
        switch (type) {
        case Boolean:
            proto = realm.getIntrinsic(Intrinsics.BooleanPrototype);
            break;
        case Number:
            proto = realm.getIntrinsic(Intrinsics.NumberPrototype);
            break;
        case String:
            if ("length".equals(getReferencedName())) {
                CharSequence str = Type.stringValue(getBase());
                return str.length();
            }
            int index = ExoticString.toStringIndex(getReferencedName());
            if (index >= 0) {
                CharSequence str = Type.stringValue(getBase());
                int len = str.length();
                if (index < len) {
                    return str.subSequence(index, index + 1);
                }
            }
            proto = realm.getIntrinsic(Intrinsics.StringPrototype);
            break;
        default:
            assert false : "invalid type";
            return null;
        }
        return proto.get(getReferencedName(), isSuperReference() ? thisValue : getBase());
    }

    /**
     * [8.2.4.1] PutValue (V, W)
     */
    public static void PutValue(Object v, Object w, Realm realm) {
        if (!(v instanceof Reference)) {
            throw throwReferenceError(realm, String.format("value is not a reference"));
        }
        ((Reference) v).PutValue(w, realm);
    }

    /**
     * [8.2.4.1] PutValue (V, W)
     */
    public void PutValue(Object w, Realm realm) {
        assert Type.of(w) != null : "invalid value type";

        Object base = getBase();
        if (isUnresolvableReference()) {
            if (isStrictReference()) {
                throw throwReferenceError(realm,
                        String.format("'%s' is not defined", referencedName));
            }
            Scriptable globalObj = realm.getGlobalThis(); // = GetGlobalObject()
            Put(realm, globalObj, getReferencedName(), w, false);
        } else if (isPropertyReference()) {
            if (hasPrimitiveBase()) {
                base = ToObject(realm, base);
            }
            boolean succeeded = ((Scriptable) base)
                    .set(getReferencedName(), w, GetThisValue(realm));
            if (!succeeded && isStrictReference()) {
                throw throwTypeError(realm, "");
            }
        } else {
            ((EnvironmentRecord) base).setMutableBinding(getReferencedName(), w,
                    isStrictReference());
        }
    }

    /**
     * [8.2.4.3] GetThisValue (V)
     */
    public static Object GetThisValue(Realm realm, Object v) {
        if (!(v instanceof Reference))
            return v;
        return ((Reference) v).GetThisValue(realm);
    }

    /**
     * [8.2.4.3] GetThisValue (V)
     */
    public Object GetThisValue(Realm realm) {
        if (isUnresolvableReference()) {
            throw throwReferenceError(realm, String.format("'%s' is not defined", referencedName));
        }
        if (isSuperReference()) {
            return thisValue;
        }
        return getBase();
    }
}