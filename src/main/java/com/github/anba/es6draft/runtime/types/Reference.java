/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types;

import static com.github.anba.es6draft.runtime.AbstractOperations.Put;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToObject;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToString;
import static com.github.anba.es6draft.runtime.internal.Errors.newReferenceError;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;

import com.github.anba.es6draft.runtime.DeclarativeEnvironmentRecord;
import com.github.anba.es6draft.runtime.DeclarativeEnvironmentRecord.Binding;
import com.github.anba.es6draft.runtime.EnvironmentRecord;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.types.builtins.ExoticString;

/**
 * <h1>6 ECMAScript Data Types and Values</h1><br>
 * <h2>6.2 ECMAScript Specification Types</h2>
 * <ul>
 * <li>6.2.3 The Reference Specification Type
 * </ul>
 */
public abstract class Reference<BASE, NAME> {
    private Reference() {
    }

    /**
     * GetBase(V)
     * 
     * @return the reference base
     */
    public abstract BASE getBase();

    /**
     * GetReferencedName(V)
     * 
     * @return the reference name
     */
    public abstract NAME getReferencedName();

    /**
     * IsStrictReference(V)
     * 
     * @return {@code true} if is strict mode reference
     */
    public abstract boolean isStrictReference();

    /**
     * HasPrimitiveBase(V)
     * 
     * @return {@code true} if the base is a primitive value
     */
    public abstract boolean hasPrimitiveBase();

    /**
     * IsPropertyReference(V)
     * 
     * @return {@code true} if this is a property reference
     */
    public abstract boolean isPropertyReference();

    /**
     * IsUnresolvableReference(V)
     * 
     * @return {@code true} if the reference is unresolvable
     */
    public abstract boolean isUnresolvableReference();

    /**
     * IsSuperReference(V)
     * 
     * @return {@code true} if this is a super reference
     */
    public abstract boolean isSuperReference();

    /**
     * [6.2.3.1] GetValue (V)
     * 
     * @param cx
     *            the execution context
     * @return the reference value
     */
    public abstract Object getValue(ExecutionContext cx);

    /**
     * [6.2.3.2] PutValue (V, W)
     * 
     * @param w
     *            the new reference value
     * @param cx
     *            the execution context
     */
    public abstract void putValue(Object w, ExecutionContext cx);

    /**
     * [6.2.3.3] GetThisValue (V)
     * 
     * @param cx
     *            the execution context
     * @return the reference this value
     */
    public abstract Object getThisValue(ExecutionContext cx);

    /**
     * [6.2.3.1] GetValue (V)
     * 
     * @param v
     *            the reference
     * @param cx
     *            the execution context
     * @return the reference value
     */
    public static Object GetValue(Object v, ExecutionContext cx) {
        /* step 1 (not applicable) */
        /* step 2 */
        if (!(v instanceof Reference))
            return v;
        /* steps 3-6 */
        return ((Reference<?, ?>) v).getValue(cx);
    }

    /**
     * [6.2.3.2] PutValue (V, W)
     * 
     * @param v
     *            the reference
     * @param w
     *            the new reference value
     * @param cx
     *            the execution context
     */
    public static void PutValue(Object v, Object w, ExecutionContext cx) {
        /* steps 1-2 (not applicable) */
        /* step 3 */
        if (!(v instanceof Reference)) {
            throw newReferenceError(cx, Messages.Key.InvalidReference);
        }
        /* steps 4-8 */
        ((Reference<?, ?>) v).putValue(w, cx);
    }

    /**
     * [6.2.3.3] GetThisValue (V)
     * 
     * @param v
     *            the reference
     * @param cx
     *            the execution context
     * @return the reference this value
     */
    public static Object GetThisValue(ExecutionContext cx, Object v) {
        /* step 1 */
        assert v instanceof Reference && ((Reference<?, ?>) v).isPropertyReference();
        /* steps 2-3 */
        return ((Reference<?, ?>) v).getThisValue(cx);
    }

    public static final class BindingReference extends
            Reference<DeclarativeEnvironmentRecord.Binding, String> {
        private final DeclarativeEnvironmentRecord.Binding base;
        private final String referencedName;
        private final boolean strictReference;

        public BindingReference(DeclarativeEnvironmentRecord.Binding base, String referencedName,
                boolean strictReference) {
            this.base = base;
            this.referencedName = referencedName;
            this.strictReference = strictReference;
        }

        @Override
        public Binding getBase() {
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
            return false;
        }

        @Override
        public boolean isSuperReference() {
            return false;
        }

        @Override
        public Object getValue(ExecutionContext cx) {
            return base.getValue(cx, referencedName, strictReference);
        }

        @Override
        public void putValue(Object w, ExecutionContext cx) {
            base.setValue(cx, referencedName, w, strictReference);
        }

        @Override
        public Object getThisValue(ExecutionContext cx) {
            throw new AssertionError();
        }
    }

    public static final class IdentifierReference extends Reference<EnvironmentRecord, String> {
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
            return base == null;
        }

        @Override
        public boolean isSuperReference() {
            return false;
        }

        @Override
        public Object getValue(ExecutionContext cx) {
            /* step 4 */
            if (isUnresolvableReference()) {
                throw newReferenceError(cx, Messages.Key.UnresolvableReference, getReferencedName());
            }
            /* step 5 (not applicable) */
            /* steps 3, 6 */
            return getBase().getBindingValue(getReferencedName(), isStrictReference());
        }

        @Override
        public void putValue(Object w, ExecutionContext cx) {
            assert Type.of(w) != null : "invalid value type";

            /* step 6 (not applicable) */
            /* step 4-5, 7-8 */
            if (isUnresolvableReference()) {
                /* steps 5, 8 */
                if (isStrictReference()) {
                    throw newReferenceError(cx, Messages.Key.UnresolvableReference,
                            getReferencedName());
                }
                ScriptObject globalObj = cx.getGlobalObject();
                Put(cx, globalObj, getReferencedName(), w, false);
            } else {
                /* steps 4, 7-8 */
                getBase().setMutableBinding(getReferencedName(), w, isStrictReference());
            }
        }

        @Override
        public EnvironmentRecord getThisValue(ExecutionContext cx) {
            throw new AssertionError();
        }
    }

    protected static abstract class PropertyReference<NAME> extends Reference<Object, NAME> {
        protected final Object base;
        protected final Type type;
        protected final boolean strictReference;

        protected PropertyReference(Object base, boolean strictReference) {
            this.base = base;
            this.type = Type.of(base);
            this.strictReference = strictReference;
            assert !(type == Type.Undefined || type == Type.Null);
        }

        @Override
        public final Object getBase() {
            return base;
        }

        @Override
        public final boolean isStrictReference() {
            return strictReference;
        }

        @Override
        public final boolean hasPrimitiveBase() {
            return type == Type.Boolean || type == Type.String || type == Type.Symbol
                    || type == Type.Number;
        }

        @Override
        public final boolean isPropertyReference() {
            return true;
        }

        @Override
        public final boolean isUnresolvableReference() {
            return false;
        }

        @Override
        public final boolean isSuperReference() {
            return false;
        }

        @Override
        public final Object getThisValue(ExecutionContext cx) {
            /* steps 1-2 (not applicable) */
            /* step 3 */
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
            case Symbol:
                return cx.getIntrinsic(Intrinsics.SymbolPrototype);
            default:
                assert false : "invalid type";
                return null;
            }
        }
    }

    public static final class PropertyIndexReference extends PropertyReference<String> {
        private final long referencedName;

        public PropertyIndexReference(Object base, long referencedName, boolean strictReference) {
            super(base, strictReference);
            this.referencedName = referencedName;
        }

        @Override
        public String getReferencedName() {
            return ToString(referencedName);
        }

        @Override
        public Object getValue(ExecutionContext cx) {
            /* steps 4, 6 (not applicable) */
            /* steps 3, 5.a */
            if (hasPrimitiveBase()) {
                // base = ToObject(realm, base);
                return GetValuePrimitive(cx);
            }
            /* steps 3, 5.b */
            return ((ScriptObject) getBase()).get(cx, referencedName, getThisValue(cx));
        }

        @Override
        public void putValue(Object w, ExecutionContext cx) {
            assert Type.of(w) != null : "invalid value type";

            ScriptObject base = (hasPrimitiveBase() ? ToObject(cx, getBase())
                    : (ScriptObject) getBase());
            boolean succeeded = base.set(cx, referencedName, w, getThisValue(cx));
            if (!succeeded && isStrictReference()) {
                throw newTypeError(cx, Messages.Key.PropertyNotModifiable, getReferencedName());
            }
        }

        private Object GetValuePrimitive(ExecutionContext cx) {
            if (type == Type.String) {
                int index = ExoticString.toStringIndex(referencedName);
                if (index >= 0) {
                    CharSequence str = Type.stringValue(getBase());
                    int len = str.length();
                    if (index < len) {
                        return str.subSequence(index, index + 1);
                    }
                }
            }
            return getPrimitiveBaseProto(cx).get(cx, referencedName, getBase());
        }
    }

    public static final class PropertyNameReference extends PropertyReference<String> {
        private final String referencedName;

        public PropertyNameReference(Object base, String referencedName, boolean strictReference) {
            super(base, strictReference);
            this.referencedName = referencedName;
        }

        @Override
        public String getReferencedName() {
            return referencedName;
        }

        @Override
        public Object getValue(ExecutionContext cx) {
            /* steps 4, 6 (not applicable) */
            /* steps 3, 5.a */
            if (hasPrimitiveBase()) {
                // base = ToObject(realm, base);
                return GetValuePrimitive(cx);
            }
            /* steps 3, 5.b */
            return ((ScriptObject) getBase()).get(cx, getReferencedName(), getThisValue(cx));
        }

        @Override
        public void putValue(Object w, ExecutionContext cx) {
            assert Type.of(w) != null : "invalid value type";

            ScriptObject base = (hasPrimitiveBase() ? ToObject(cx, getBase())
                    : (ScriptObject) getBase());
            boolean succeeded = base.set(cx, getReferencedName(), w, getThisValue(cx));
            if (!succeeded && isStrictReference()) {
                throw newTypeError(cx, Messages.Key.PropertyNotModifiable, getReferencedName());
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

    public static final class PropertySymbolReference extends PropertyReference<Symbol> {
        private final Symbol referencedName;

        public PropertySymbolReference(Object base, Symbol referencedName, boolean strictReference) {
            super(base, strictReference);
            this.referencedName = referencedName;
        }

        @Override
        public Symbol getReferencedName() {
            return referencedName;
        }

        @Override
        public Object getValue(ExecutionContext cx) {
            /* steps 4, 6 (not applicable) */
            /* steps 3, 5.a */
            if (hasPrimitiveBase()) {
                // base = ToObject(realm, base);
                return GetValuePrimitive(cx);
            }
            /* steps 3, 5.b */
            return ((ScriptObject) getBase()).get(cx, getReferencedName(), getThisValue(cx));
        }

        @Override
        public void putValue(Object w, ExecutionContext cx) {
            assert Type.of(w) != null : "invalid value type";

            ScriptObject base = (hasPrimitiveBase() ? ToObject(cx, getBase())
                    : (ScriptObject) getBase());
            boolean succeeded = base.set(cx, getReferencedName(), w, getThisValue(cx));
            if (!succeeded && isStrictReference()) {
                throw newTypeError(cx, Messages.Key.PropertyNotModifiable, getReferencedName()
                        .toString());
            }
        }

        private Object GetValuePrimitive(ExecutionContext cx) {
            return getPrimitiveBaseProto(cx).get(cx, getReferencedName(), getBase());
        }
    }

    protected static abstract class SuperReference<NAME> extends Reference<ScriptObject, NAME> {
        private final ScriptObject base;
        private final boolean strictReference;
        private final Object thisValue;

        protected SuperReference(ScriptObject base, boolean strictReference, Object thisValue) {
            this.base = base;
            this.strictReference = strictReference;
            this.thisValue = thisValue;
        }

        @Override
        public final ScriptObject getBase() {
            return base;
        }

        @Override
        public final boolean isStrictReference() {
            return strictReference;
        }

        @Override
        public final boolean hasPrimitiveBase() {
            return false;
        }

        @Override
        public final boolean isPropertyReference() {
            return true;
        }

        @Override
        public final boolean isUnresolvableReference() {
            return false;
        }

        @Override
        public final boolean isSuperReference() {
            return true;
        }

        @Override
        public final Object getThisValue(ExecutionContext cx) {
            /* steps 1, 3 (not applicable) */
            /* step 2 */
            return thisValue;
        }
    }

    public static final class SuperNameReference extends SuperReference<String> {
        private final String referencedName;

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
        public Object getValue(ExecutionContext cx) {
            return getBase().get(cx, getReferencedName(), getThisValue(cx));
        }

        @Override
        public void putValue(Object w, ExecutionContext cx) {
            assert Type.of(w) != null : "invalid value type";

            boolean succeeded = getBase().set(cx, getReferencedName(), w, getThisValue(cx));
            if (!succeeded && isStrictReference()) {
                throw newTypeError(cx, Messages.Key.PropertyNotModifiable, getReferencedName());
            }
        }
    }

    public static final class SuperSymbolReference extends SuperReference<Symbol> {
        private final Symbol referencedName;

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
        public Object getValue(ExecutionContext cx) {
            return getBase().get(cx, getReferencedName(), getThisValue(cx));
        }

        @Override
        public void putValue(Object w, ExecutionContext cx) {
            assert Type.of(w) != null : "invalid value type";

            boolean succeeded = getBase().set(cx, getReferencedName(), w, getThisValue(cx));
            if (!succeeded && isStrictReference()) {
                throw newTypeError(cx, Messages.Key.PropertyNotModifiable, getReferencedName()
                        .toString());
            }
        }
    }
}