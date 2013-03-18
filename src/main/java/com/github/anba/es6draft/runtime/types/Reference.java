/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types;

import static com.github.anba.es6draft.runtime.AbstractOperations.Put;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToObject;
import static com.github.anba.es6draft.runtime.internal.Errors.throwReferenceError;
import static com.github.anba.es6draft.runtime.internal.Errors.throwTypeError;

import com.github.anba.es6draft.runtime.EnvironmentRecord;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.types.builtins.ExoticString;

/**
 * <h1>8 Types</h1><br>
 * <h2>8.2 ECMAScript Specification Types</h2>
 * <ul>
 * <li>8.2.4 The Reference Specification Type
 * </ul>
 */
public abstract class Reference {
    private Reference() {
    }

    public static final class IdentifierReference extends Reference {
        private final EnvironmentRecord base;
        private final String referencedName;
        private final boolean strictReference;

        public IdentifierReference(EnvironmentRecord base, String referencedName,
                boolean strictReference) {
            this.base = base;
            this.referencedName = referencedName;
            this.strictReference = strictReference;
        }

        @Override
        public EnvironmentRecord getBase() {
            return base;
        }

        @Override
        public String getReferencedName() {
            return referencedName;
        }

        @Override
        public boolean isStrictReference() {
            return strictReference;
        }

        @Override
        public boolean hasPrimitiveBase() {
            return false;
        }

        @Override
        public boolean isPropertyReference() {
            return false;
        }

        @Override
        public boolean isUnresolvableReference() {
            return (base == null);
        }

        @Override
        public boolean isSuperReference() {
            return false;
        }

        @Override
        public Object GetValue(Realm realm) {
            if (isUnresolvableReference()) {
                throw throwReferenceError(realm, Messages.Key.UnresolvableReference,
                        getReferencedName());
            }
            return getBase().getBindingValue(getReferencedName(), isStrictReference());
        }

        @Override
        public void PutValue(Object w, Realm realm) {
            assert Type.of(w) != null : "invalid value type";

            if (isUnresolvableReference()) {
                if (isStrictReference()) {
                    throw throwReferenceError(realm, Messages.Key.UnresolvableReference,
                            getReferencedName());
                }
                ScriptObject globalObj = realm.getGlobalThis(); // = GetGlobalObject()
                Put(realm, globalObj, getReferencedName(), w, false);
            } else {
                getBase().setMutableBinding(getReferencedName(), w, isStrictReference());
            }
        }

        @Override
        public EnvironmentRecord GetThisValue(Realm realm) {
            if (isUnresolvableReference()) {
                throw throwReferenceError(realm, Messages.Key.UnresolvableReference,
                        getReferencedName());
            }
            return getBase();
        }
    }

    public static final class PropertyReference extends Reference {
        private final Object base;
        private final Type type;
        private final String referencedName;
        private final boolean strictReference;

        public PropertyReference(Object base, String referencedName, boolean strictReference) {
            this.base = base;
            this.type = Type.of(base);
            this.referencedName = referencedName;
            this.strictReference = strictReference;
        }

        @Override
        public Object getBase() {
            return base;
        }

        @Override
        public String getReferencedName() {
            return referencedName;
        }

        @Override
        public boolean isStrictReference() {
            return strictReference;
        }

        @Override
        public boolean hasPrimitiveBase() {
            return type == Type.Boolean || type == Type.String || type == Type.Number;
        }

        @Override
        public boolean isPropertyReference() {
            return true;
        }

        @Override
        public boolean isUnresolvableReference() {
            return false;
        }

        @Override
        public boolean isSuperReference() {
            return false;
        }

        @Override
        public Object GetValue(Realm realm) {
            if (hasPrimitiveBase()) {
                // base = ToObject(realm, base);
                return GetValuePrimitive(realm);
            }
            return ((ScriptObject) getBase()).get(getReferencedName(), GetThisValue(realm));
        }

        private Object GetValuePrimitive(Realm realm) {
            ScriptObject proto;
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
            return proto.get(getReferencedName(), getBase());
        }

        @Override
        public void PutValue(Object w, Realm realm) {
            assert Type.of(w) != null : "invalid value type";

            Object base = getBase();
            if (hasPrimitiveBase()) {
                base = ToObject(realm, base);
            }
            boolean succeeded = ((ScriptObject) base).set(getReferencedName(), w,
                    GetThisValue(realm));
            if (!succeeded && isStrictReference()) {
                throw throwTypeError(realm, Messages.Key.PropertyNotModifiable, getReferencedName());
            }
        }

        @Override
        public Object GetThisValue(Realm realm) {
            return getBase();
        }
    }

    public static final class SuperReference extends Reference {
        private final ScriptObject base;
        private final String referencedName;
        private final boolean strictReference;
        private final Object thisValue;

        public SuperReference(ScriptObject base, String referencedName, boolean strictReference,
                Object thisValue) {
            this.base = base;
            this.referencedName = referencedName;
            this.strictReference = strictReference;
            this.thisValue = thisValue;
        }

        @Override
        public ScriptObject getBase() {
            return base;
        }

        @Override
        public String getReferencedName() {
            return referencedName;
        }

        @Override
        public boolean isStrictReference() {
            return strictReference;
        }

        @Override
        public boolean hasPrimitiveBase() {
            return false;
        }

        @Override
        public boolean isPropertyReference() {
            return true;
        }

        @Override
        public boolean isUnresolvableReference() {
            return false;
        }

        @Override
        public boolean isSuperReference() {
            return true;
        }

        @Override
        public Object GetValue(Realm realm) {
            return getBase().get(getReferencedName(), GetThisValue(realm));
        }

        @Override
        public void PutValue(Object w, Realm realm) {
            assert Type.of(w) != null : "invalid value type";

            boolean succeeded = getBase().set(getReferencedName(), w, GetThisValue(realm));
            if (!succeeded && isStrictReference()) {
                throw throwTypeError(realm, Messages.Key.PropertyNotModifiable, getReferencedName());
            }
        }

        @Override
        public Object GetThisValue(Realm realm) {
            return thisValue;
        }
    }

    /**
     * GetBase(V)
     */
    public abstract Object getBase();

    /**
     * GetReferencedName(V)
     */
    public abstract String getReferencedName();

    /**
     * IsStrictReference(V)
     */
    public abstract boolean isStrictReference();

    /**
     * HasPrimitiveBase(V)
     */
    public abstract boolean hasPrimitiveBase();

    /**
     * IsPropertyReference(V)
     */
    public abstract boolean isPropertyReference();

    /**
     * IsUnresolvableReference(V)
     */
    public abstract boolean isUnresolvableReference();

    /**
     * IsSuperReference(V)
     */
    public abstract boolean isSuperReference();

    /**
     * [8.2.4.1] GetValue (V)
     */
    public abstract Object GetValue(Realm realm);

    /**
     * [8.2.4.1] PutValue (V, W)
     */
    public abstract void PutValue(Object w, Realm realm);

    /**
     * [8.2.4.3] GetThisValue (V)
     */
    public abstract Object GetThisValue(Realm realm);

    /**
     * [8.2.4.1] GetValue (V)
     */
    public static Object GetValue(Object v, Realm realm) {
        if (!(v instanceof Reference))
            return v;
        return ((Reference) v).GetValue(realm);
    }

    /**
     * [8.2.4.1] PutValue (V, W)
     */
    public static void PutValue(Object v, Object w, Realm realm) {
        if (!(v instanceof Reference)) {
            throw throwReferenceError(realm, Messages.Key.InvalidReference);
        }
        ((Reference) v).PutValue(w, realm);
    }

    /**
     * [8.2.4.3] GetThisValue (V)
     */
    public static Object GetThisValue(Realm realm, Object v) {
        if (!(v instanceof Reference))
            return v;
        return ((Reference) v).GetThisValue(realm);
    }
}