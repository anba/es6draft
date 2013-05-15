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
import com.github.anba.es6draft.runtime.ExecutionContext;
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

    /**
     * GetBase(V)
     */
    public abstract Object getBase();

    /**
     * GetReferencedName(V)
     */
    public abstract Object getReferencedName();

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
    public abstract Object GetValue(ExecutionContext cx);

    /**
     * [8.2.4.1] PutValue (V, W)
     */
    public abstract void PutValue(Object w, ExecutionContext cx);

    /**
     * [8.2.4.3] GetThisValue (V)
     */
    public abstract Object GetThisValue(ExecutionContext cx);

    /**
     * [8.2.4.1] GetValue (V)
     */
    public static Object GetValue(Object v, ExecutionContext cx) {
        if (!(v instanceof Reference))
            return v;
        return ((Reference) v).GetValue(cx);
    }

    /**
     * [8.2.4.1] PutValue (V, W)
     */
    public static void PutValue(Object v, Object w, ExecutionContext cx) {
        if (!(v instanceof Reference)) {
            throw throwReferenceError(cx, Messages.Key.InvalidReference);
        }
        ((Reference) v).PutValue(w, cx);
    }

    /**
     * [8.2.4.3] GetThisValue (V)
     */
    public static Object GetThisValue(ExecutionContext cx, Object v) {
        if (!(v instanceof Reference))
            return v;
        return ((Reference) v).GetThisValue(cx);
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
        public Object GetValue(ExecutionContext cx) {
            if (isUnresolvableReference()) {
                throw throwReferenceError(cx, Messages.Key.UnresolvableReference,
                        getReferencedName());
            }
            return getBase().getBindingValue(getReferencedName(), isStrictReference());
        }

        @Override
        public void PutValue(Object w, ExecutionContext cx) {
            assert Type.of(w) != null : "invalid value type";

            if (isUnresolvableReference()) {
                if (isStrictReference()) {
                    throw throwReferenceError(cx, Messages.Key.UnresolvableReference,
                            getReferencedName());
                }
                ScriptObject globalObj = cx.getGlobalObject();
                Put(cx, globalObj, getReferencedName(), w, false);
            } else {
                getBase().setMutableBinding(getReferencedName(), w, isStrictReference());
            }
        }

        @Override
        public EnvironmentRecord GetThisValue(ExecutionContext cx) {
            if (isUnresolvableReference()) {
                throw throwReferenceError(cx, Messages.Key.UnresolvableReference,
                        getReferencedName());
            }
            return getBase();
        }
    }

    protected static abstract class PropertyReference extends Reference {
        protected final Object base;
        protected final Type type;
        protected final boolean strictReference;

        protected PropertyReference(Object base, boolean strictReference) {
            this.base = base;
            this.type = Type.of(base);
            this.strictReference = strictReference;
            assert !(type == Type.Undefined || type == Type.Null || type == Type.Symbol);
        }

        @Override
        public Object getBase() {
            return base;
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
        public Object GetThisValue(ExecutionContext cx) {
            return getBase();
        }

        protected final ScriptObject getPrimitiveBaseProto(ExecutionContext cx) {
            switch (type) {
            case Boolean:
                return cx.getIntrinsic(Intrinsics.BooleanPrototype);
            case Number:
                return cx.getIntrinsic(Intrinsics.NumberPrototype);
            case String:
                return cx.getIntrinsic(Intrinsics.StringPrototype);
            default:
                assert false : "invalid type";
                return null;
            }
        }
    }

    public static final class PropertyNameReference extends PropertyReference {
        private String referencedName;

        public PropertyNameReference(Object base, String referencedName, boolean strictReference) {
            super(base, strictReference);
            this.referencedName = referencedName;
        }

        @Override
        public String getReferencedName() {
            return referencedName;
        }

        @Override
        public Object GetValue(ExecutionContext cx) {
            if (hasPrimitiveBase()) {
                // base = ToObject(realm, base);
                return GetValuePrimitive(cx);
            }
            return ((ScriptObject) getBase()).get(cx, getReferencedName(), GetThisValue(cx));
        }

        @Override
        public void PutValue(Object w, ExecutionContext cx) {
            assert Type.of(w) != null : "invalid value type";

            ScriptObject base = (hasPrimitiveBase() ? ToObject(cx, getBase())
                    : (ScriptObject) getBase());
            boolean succeeded = base.set(cx, getReferencedName(), w, GetThisValue(cx));
            if (!succeeded && isStrictReference()) {
                throw throwTypeError(cx, Messages.Key.PropertyNotModifiable, getReferencedName());
            }
        }

        private Object GetValuePrimitive(ExecutionContext cx) {
            if (type == Type.String) {
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
            }
            return getPrimitiveBaseProto(cx).get(cx, getReferencedName(), getBase());
        }
    }

    public static final class PropertySymbolReference extends PropertyReference {
        private Symbol referencedName;

        public PropertySymbolReference(Object base, Symbol referencedName, boolean strictReference) {
            super(base, strictReference);
            this.referencedName = referencedName;
        }

        @Override
        public Symbol getReferencedName() {
            return referencedName;
        }

        @Override
        public Object GetValue(ExecutionContext cx) {
            if (hasPrimitiveBase()) {
                // base = ToObject(realm, base);
                return GetValuePrimitive(cx);
            }
            return ((ScriptObject) getBase()).get(cx, getReferencedName(), GetThisValue(cx));
        }

        @Override
        public void PutValue(Object w, ExecutionContext cx) {
            assert Type.of(w) != null : "invalid value type";

            ScriptObject base = (hasPrimitiveBase() ? ToObject(cx, getBase())
                    : (ScriptObject) getBase());
            boolean succeeded = base.set(cx, getReferencedName(), w, GetThisValue(cx));
            if (!succeeded && isStrictReference()) {
                throw throwTypeError(cx, Messages.Key.PropertyNotModifiable, getReferencedName()
                        .toString());
            }
        }

        private Object GetValuePrimitive(ExecutionContext cx) {
            return getPrimitiveBaseProto(cx).get(cx, getReferencedName(), getBase());
        }
    }

    protected static abstract class SuperReference extends Reference {
        private final ScriptObject base;
        private final boolean strictReference;
        private final Object thisValue;

        protected SuperReference(ScriptObject base, boolean strictReference, Object thisValue) {
            this.base = base;
            this.strictReference = strictReference;
            this.thisValue = thisValue;
        }

        @Override
        public ScriptObject getBase() {
            return base;
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
        public Object GetThisValue(ExecutionContext cx) {
            return thisValue;
        }
    }

    public static final class SuperNameReference extends SuperReference {
        private String referencedName;

        public SuperNameReference(ScriptObject base, String referencedName,
                boolean strictReference, Object thisValue) {
            super(base, strictReference, thisValue);
            this.referencedName = referencedName;
        }

        @Override
        public String getReferencedName() {
            return referencedName;
        }

        @Override
        public Object GetValue(ExecutionContext cx) {
            return getBase().get(cx, getReferencedName(), GetThisValue(cx));
        }

        @Override
        public void PutValue(Object w, ExecutionContext cx) {
            assert Type.of(w) != null : "invalid value type";

            boolean succeeded = getBase().set(cx, getReferencedName(), w, GetThisValue(cx));
            if (!succeeded && isStrictReference()) {
                throw throwTypeError(cx, Messages.Key.PropertyNotModifiable, getReferencedName()
                        .toString());
            }
        }
    }

    public static final class SuperSymbolReference extends SuperReference {
        private Symbol referencedName;

        public SuperSymbolReference(ScriptObject base, Symbol referencedName,
                boolean strictReference, Object thisValue) {
            super(base, strictReference, thisValue);
            this.referencedName = referencedName;
        }

        @Override
        public Symbol getReferencedName() {
            return referencedName;
        }

        @Override
        public Object GetValue(ExecutionContext cx) {
            return getBase().get(cx, getReferencedName(), GetThisValue(cx));
        }

        @Override
        public void PutValue(Object w, ExecutionContext cx) {
            assert Type.of(w) != null : "invalid value type";

            boolean succeeded = getBase().set(cx, getReferencedName(), w, GetThisValue(cx));
            if (!succeeded && isStrictReference()) {
                throw throwTypeError(cx, Messages.Key.PropertyNotModifiable, getReferencedName()
                        .toString());
            }
        }
    }
}