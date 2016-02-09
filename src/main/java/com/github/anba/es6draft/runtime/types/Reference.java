/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types;

import static com.github.anba.es6draft.runtime.AbstractOperations.Set;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToObject;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToString;
import static com.github.anba.es6draft.runtime.internal.Errors.newReferenceError;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;

import com.github.anba.es6draft.runtime.DeclarativeEnvironmentRecord;
import com.github.anba.es6draft.runtime.EnvironmentRecord;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Strings;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

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
     * @return the reference this value
     */
    public abstract Object getThisValue();

    /**
     * [6.2.3.4] InitializeReferencedBinding (V, W)
     * 
     * 
     * @param w
     *            the new reference value
     */
    public abstract void initializeReferencedBinding(Object w);

    /**
     * 12.5.4 The delete Operator<br>
     * Runtime Semantics: Evaluation 12.5.4.2
     * 
     * @param cx
     *            the execution context
     * @return {@code true} on success
     */
    public abstract boolean delete(ExecutionContext cx);

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
     * [6.2.3.1] GetValue (V)
     * 
     * @param v
     *            the reference
     * @param cx
     *            the execution context
     * @return the reference value
     */
    public static Object GetValue(Reference<?, ?> v, ExecutionContext cx) {
        /* steps 1-2 (not applicable) */
        /* steps 3-6 */
        return v.getValue(cx);
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
     * [6.2.3.2] PutValue (V, W)
     * 
     * @param v
     *            the reference
     * @param w
     *            the new reference value
     * @param cx
     *            the execution context
     */
    public static void PutValue(Reference<?, ?> v, Object w, ExecutionContext cx) {
        /* steps 1-3 (not applicable) */
        /* steps 4-8 */
        v.putValue(w, cx);
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
        return ((Reference<?, ?>) v).getThisValue();
    }

    /**
     * Reference specialization for binding references.
     */
    public static final class BindingReference extends Reference<DeclarativeEnvironmentRecord, String> {
        private final DeclarativeEnvironmentRecord base;
        private final DeclarativeEnvironmentRecord.Binding binding;
        private final String referencedName;
        private final boolean strictReference;

        public BindingReference(DeclarativeEnvironmentRecord base, DeclarativeEnvironmentRecord.Binding binding,
                String referencedName, boolean strictReference) {
            this.base = base;
            this.binding = binding;
            this.referencedName = referencedName;
            this.strictReference = strictReference;
        }

        @Override
        public DeclarativeEnvironmentRecord getBase() {
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
            if (!binding.isInitialized()) {
                throw newReferenceError(cx, Messages.Key.UninitializedBinding, referencedName);
            }
            return binding.getValue();
        }

        @Override
        public void putValue(Object w, ExecutionContext cx) {
            if (!binding.isInitialized()) {
                throw newReferenceError(cx, Messages.Key.UninitializedBinding, referencedName);
            } else if (binding.isMutable()) {
                binding.setValue(w);
            } else if (strictReference || binding.isStrict()) {
                throw newTypeError(cx, Messages.Key.ImmutableBinding, referencedName);
            }
        }

        @Override
        public boolean delete(ExecutionContext cx) {
            return false;
        }

        @Override
        public Object getThisValue() {
            throw new AssertionError();
        }

        @Override
        public void initializeReferencedBinding(Object w) {
            binding.initialize(w);
        }
    }

    /**
     * Reference specialization for identifier references.
     */
    public static final class IdentifierReference<RECORD extends EnvironmentRecord> extends Reference<RECORD, String> {
        private final RECORD base;
        private final String referencedName;
        private final boolean strictReference;

        public IdentifierReference(RECORD base, String referencedName, boolean strictReference) {
            this.base = base;
            this.referencedName = referencedName;
            this.strictReference = strictReference;
        }

        @Override
        public RECORD getBase() {
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
            /* steps 4-5 (not applicable) */
            /* steps 3, 6 */
            return getBase().getBindingValue(getReferencedName(), isStrictReference());
        }

        @Override
        public void putValue(Object w, ExecutionContext cx) {
            assert Type.isType(w) : "invalid value type";

            /* steps 5-6 (not applicable) */
            /* steps 4, 7 */
            getBase().setMutableBinding(getReferencedName(), w, isStrictReference());
        }

        @Override
        public boolean delete(ExecutionContext cx) {
            /* steps 1-3 (generated code) */
            /* steps 4-5 (not applicable) */
            /* step 6 */
            return getBase().deleteBinding(getReferencedName());
        }

        @Override
        public EnvironmentRecord getThisValue() {
            throw new AssertionError();
        }

        @Override
        public void initializeReferencedBinding(Object w) {
            /* steps 1-4 (not applicable) */
            /* steps 5-7 */
            getBase().initializeBinding(getReferencedName(), w);
        }
    }

    /**
     * Reference specialization for unresolvable references.
     */
    public static final class UnresolvableReference extends Reference<Void, String> {
        private final String referencedName;
        private final boolean strictReference;

        public UnresolvableReference(String referencedName, boolean strictReference) {
            this.referencedName = referencedName;
            this.strictReference = strictReference;
        }

        @Override
        public Void getBase() {
            return null;
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
            return true;
        }

        @Override
        public boolean isSuperReference() {
            return false;
        }

        @Override
        public Object getValue(ExecutionContext cx) {
            /* step 4 */
            throw newReferenceError(cx, Messages.Key.UnresolvableReference, getReferencedName());
        }

        @Override
        public void putValue(Object w, ExecutionContext cx) {
            assert Type.isType(w) : "invalid value type";

            /* step 5 */
            if (isStrictReference()) {
                throw newReferenceError(cx, Messages.Key.UnresolvableReference, getReferencedName());
            }
            Set(cx, cx.getGlobalObject(), getReferencedName(), w, false);
        }

        @Override
        public boolean delete(ExecutionContext cx) {
            /* steps 1-3 (generated code) */
            /* steps 5-6 (not applicable) */
            /* step 4 */
            assert !isStrictReference();
            return true;
        }

        @Override
        public EnvironmentRecord getThisValue() {
            throw new AssertionError();
        }

        @Override
        public void initializeReferencedBinding(Object w) {
            throw new AssertionError();
        }
    }

    /**
     * Reference specialization for property references.
     *
     * @param <NAME>
     *            the reference name type
     */
    protected static abstract class PropertyReference<NAME> extends Reference<Object, NAME> {
        protected final Object base;
        protected final Type type;
        protected final boolean strictReference;

        /**
         * Constructs a new property reference.
         * 
         * @param base
         *            the base object
         * @param strictReference
         *            the strict mode flag
         */
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
            return type != Type.Object;
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
        public final Object getThisValue() {
            /* steps 1-2 (not applicable) */
            /* step 3 */
            return getBase();
        }

        @Override
        public final void initializeReferencedBinding(Object w) {
            throw new AssertionError();
        }

        protected final OrdinaryObject getPrimitiveBaseProto(ExecutionContext cx) {
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
                throw new AssertionError();
            }
        }

        protected static final OrdinaryObject getPrimitiveBaseProto(ExecutionContext cx, Type type) {
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
                throw new AssertionError();
            }
        }
    }

    /**
     * Reference specialization for indexed property references.
     */
    public static final class PropertyIndexReference extends PropertyReference<String> {
        private final long referencedName;

        /**
         * Constructs a new property reference.
         * 
         * @param base
         *            the base object
         * @param referencedName
         *            the referenced name
         * @param strictReference
         *            the strict mode flag
         */
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
            return ((ScriptObject) getBase()).get(cx, referencedName, getThisValue());
        }

        @Override
        public void putValue(Object w, ExecutionContext cx) {
            assert Type.isType(w) : "invalid value type";

            ScriptObject base = hasPrimitiveBase() ? ToObject(cx, getBase()) : (ScriptObject) getBase();
            boolean succeeded = base.set(cx, referencedName, w, getThisValue());
            if (!succeeded && isStrictReference()) {
                throw newTypeError(cx, Messages.Key.PropertyNotModifiable, getReferencedName());
            }
        }

        @Override
        public boolean delete(ExecutionContext cx) {
            /* steps 1-3 (generated code) */
            /* steps 4, 6 (not applicable) */
            /* step 5 */
            ScriptObject base = hasPrimitiveBase() ? ToObject(cx, getBase()) : (ScriptObject) getBase();
            boolean deleteStatus = base.delete(cx, referencedName);
            if (!deleteStatus && isStrictReference()) {
                throw newTypeError(cx, Messages.Key.PropertyNotDeletable, getReferencedName());
            }
            return deleteStatus;
        }

        private Object GetValuePrimitive(ExecutionContext cx) {
            long refName = referencedName;
            if (type == Type.String && 0 <= refName && refName < 0x7FFF_FFFFL) {
                int index = (int) refName;
                CharSequence str = Type.stringValue(getBase());
                if (index < str.length()) {
                    return String.valueOf(str.charAt(index));
                }
            }
            return getPrimitiveBaseProto(cx).get(cx, refName, getBase());
        }

        public static Object GetValue(ExecutionContext cx, Object base, long referencedName) {
            assert !Type.isUndefinedOrNull(base);
            if (base instanceof ScriptObject) {
                return ((ScriptObject) base).get(cx, referencedName, base);
            }
            return GetValuePrimitive(cx, base, referencedName);
        }

        private static Object GetValuePrimitive(ExecutionContext cx, Object base, long referencedName) {
            if (Type.isString(base) && 0 <= referencedName && referencedName < 0x7FFF_FFFFL) {
                int index = (int) referencedName;
                CharSequence str = Type.stringValue(base);
                if (index < str.length()) {
                    return String.valueOf(str.charAt(index));
                }
            }
            return getPrimitiveBaseProto(cx, Type.of(base)).get(cx, referencedName, base);
        }

        public static void PutValue(ExecutionContext cx, Object base, long referencedName, Object value,
                boolean strict) {
            assert !Type.isUndefinedOrNull(base);
            boolean succeeded;
            if (base instanceof ScriptObject) {
                succeeded = ((ScriptObject) base).set(cx, referencedName, value, base);
            } else {
                succeeded = ToObject(cx, base).set(cx, referencedName, value, base);
            }
            if (!succeeded && strict) {
                throw newTypeError(cx, Messages.Key.PropertyNotModifiable, ToString(referencedName));
            }
        }
    }

    /**
     * Reference specialization for string-valued property references.
     */
    public static final class PropertyNameReference extends PropertyReference<String> {
        private final String referencedName;

        /**
         * Constructs a new property reference.
         * 
         * @param base
         *            the base object
         * @param referencedName
         *            the referenced name
         * @param strictReference
         *            the strict mode flag
         */
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
            return ((ScriptObject) getBase()).get(cx, referencedName, getThisValue());
        }

        @Override
        public void putValue(Object w, ExecutionContext cx) {
            assert Type.isType(w) : "invalid value type";

            ScriptObject base = hasPrimitiveBase() ? ToObject(cx, getBase()) : (ScriptObject) getBase();
            boolean succeeded = base.set(cx, referencedName, w, getThisValue());
            if (!succeeded && isStrictReference()) {
                throw newTypeError(cx, Messages.Key.PropertyNotModifiable, getReferencedName());
            }
        }

        @Override
        public boolean delete(ExecutionContext cx) {
            /* steps 1-3 (generated code) */
            /* steps 4, 6 (not applicable) */
            /* step 5 */
            ScriptObject base = hasPrimitiveBase() ? ToObject(cx, getBase()) : (ScriptObject) getBase();
            boolean deleteStatus = base.delete(cx, referencedName);
            if (!deleteStatus && isStrictReference()) {
                throw newTypeError(cx, Messages.Key.PropertyNotDeletable, getReferencedName());
            }
            return deleteStatus;
        }

        private Object GetValuePrimitive(ExecutionContext cx) {
            if (type == Type.String) {
                if ("length".equals(referencedName)) {
                    CharSequence str = Type.stringValue(getBase());
                    return str.length();
                }
                int index = Strings.toStringIndex(referencedName);
                if (index >= 0) {
                    CharSequence str = Type.stringValue(getBase());
                    if (index < str.length()) {
                        return String.valueOf(str.charAt(index));
                    }
                }
            }
            return getPrimitiveBaseProto(cx).get(cx, referencedName, getBase());
        }

        public static Object GetValue(ExecutionContext cx, Object base, String referencedName) {
            assert !Type.isUndefinedOrNull(base);
            if (base instanceof ScriptObject) {
                return ((ScriptObject) base).get(cx, referencedName, base);
            }
            return GetValuePrimitive(cx, base, referencedName);
        }

        private static Object GetValuePrimitive(ExecutionContext cx, Object base, String referencedName) {
            if (Type.isString(base)) {
                if ("length".equals(referencedName)) {
                    CharSequence str = Type.stringValue(base);
                    return str.length();
                }
                int index = Strings.toStringIndex(referencedName);
                if (index >= 0) {
                    CharSequence str = Type.stringValue(base);
                    if (index < str.length()) {
                        return String.valueOf(str.charAt(index));
                    }
                }
            }
            return getPrimitiveBaseProto(cx, Type.of(base)).get(cx, referencedName, base);
        }

        public static void PutValue(ExecutionContext cx, Object base, String referencedName, Object value,
                boolean strict) {
            assert !Type.isUndefinedOrNull(base);
            boolean succeeded;
            if (base instanceof ScriptObject) {
                succeeded = ((ScriptObject) base).set(cx, referencedName, value, base);
            } else {
                succeeded = ToObject(cx, base).set(cx, referencedName, value, base);
            }
            if (!succeeded && strict) {
                throw newTypeError(cx, Messages.Key.PropertyNotModifiable, referencedName);
            }
        }
    }

    /**
     * Reference specialization for symbol-valued property references.
     */
    public static final class PropertySymbolReference extends PropertyReference<Symbol> {
        private final Symbol referencedName;

        /**
         * Constructs a new property reference.
         * 
         * @param base
         *            the base object
         * @param referencedName
         *            the referenced name
         * @param strictReference
         *            the strict mode flag
         */
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
            return ((ScriptObject) getBase()).get(cx, referencedName, getThisValue());
        }

        @Override
        public void putValue(Object w, ExecutionContext cx) {
            assert Type.isType(w) : "invalid value type";

            ScriptObject base = hasPrimitiveBase() ? ToObject(cx, getBase()) : (ScriptObject) getBase();
            boolean succeeded = base.set(cx, referencedName, w, getThisValue());
            if (!succeeded && isStrictReference()) {
                throw newTypeError(cx, Messages.Key.PropertyNotModifiable, getReferencedName().toString());
            }
        }

        @Override
        public boolean delete(ExecutionContext cx) {
            /* steps 1-3 (generated code) */
            /* steps 4, 6 (not applicable) */
            /* step 5 */
            ScriptObject base = hasPrimitiveBase() ? ToObject(cx, getBase()) : (ScriptObject) getBase();
            boolean deleteStatus = base.delete(cx, referencedName);
            if (!deleteStatus && isStrictReference()) {
                throw newTypeError(cx, Messages.Key.PropertyNotDeletable, getReferencedName().toString());
            }
            return deleteStatus;
        }

        private Object GetValuePrimitive(ExecutionContext cx) {
            return getPrimitiveBaseProto(cx).get(cx, referencedName, getBase());
        }

        public static Object GetValue(ExecutionContext cx, Object base, Symbol referencedName) {
            assert !Type.isUndefinedOrNull(base);
            if (base instanceof ScriptObject) {
                return ((ScriptObject) base).get(cx, referencedName, base);
            }
            return GetValuePrimitive(cx, base, referencedName);
        }

        private static Object GetValuePrimitive(ExecutionContext cx, Object base, Symbol referencedName) {
            return getPrimitiveBaseProto(cx, Type.of(base)).get(cx, referencedName, base);
        }

        public static void PutValue(ExecutionContext cx, Object base, Symbol referencedName, Object value,
                boolean strict) {
            assert !Type.isUndefinedOrNull(base);
            boolean succeeded;
            if (base instanceof ScriptObject) {
                succeeded = ((ScriptObject) base).set(cx, referencedName, value, base);
            } else {
                succeeded = ToObject(cx, base).set(cx, referencedName, value, base);
            }
            if (!succeeded && strict) {
                throw newTypeError(cx, Messages.Key.PropertyNotModifiable, referencedName.toString());
            }
        }
    }

    /**
     * Reference specialization for <code>super</code> references.
     *
     * @param <NAME>
     *            the reference name type
     */
    protected static abstract class SuperReference<NAME> extends Reference<ScriptObject, NAME> {
        private final ScriptObject base;
        private final boolean strictReference;
        private final Object thisValue;

        /**
         * Constructs a new <code>super</code> reference.
         * 
         * @param base
         *            the base object
         * @param strictReference
         *            the strict mode flag
         * @param thisValue
         *            the this-binding
         */
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
        public final Object getThisValue() {
            /* steps 1, 3 (not applicable) */
            /* step 2 */
            return thisValue;
        }

        @Override
        public final boolean delete(ExecutionContext cx) {
            throw newReferenceError(cx, Messages.Key.SuperDelete);
        }

        @Override
        public final void initializeReferencedBinding(Object w) {
            throw new AssertionError();
        }
    }

    /**
     * Reference specialization for string-valued <code>super</code> references.
     */
    public static final class SuperNameReference extends SuperReference<String> {
        private final String referencedName;

        /**
         * Constructs a new <code>super</code> reference.
         * 
         * @param base
         *            the base object
         * @param referencedName
         *            the referenced name
         * @param strictReference
         *            the strict mode flag
         * @param thisValue
         *            the this-binding
         */
        public SuperNameReference(ScriptObject base, String referencedName, boolean strictReference, Object thisValue) {
            super(base, strictReference, thisValue);
            this.referencedName = referencedName;
        }

        @Override
        public String getReferencedName() {
            return referencedName;
        }

        @Override
        public Object getValue(ExecutionContext cx) {
            return getBase().get(cx, getReferencedName(), getThisValue());
        }

        @Override
        public void putValue(Object w, ExecutionContext cx) {
            assert Type.isType(w) : "invalid value type";

            boolean succeeded = getBase().set(cx, getReferencedName(), w, getThisValue());
            if (!succeeded && isStrictReference()) {
                throw newTypeError(cx, Messages.Key.PropertyNotModifiable, getReferencedName());
            }
        }
    }

    /**
     * Reference specialization for symbol-valued <code>super</code> references.
     */
    public static final class SuperSymbolReference extends SuperReference<Symbol> {
        private final Symbol referencedName;

        /**
         * Constructs a new <code>super</code> reference.
         * 
         * @param base
         *            the base object
         * @param referencedName
         *            the referenced name
         * @param strictReference
         *            the strict mode flag
         * @param thisValue
         *            the this-binding
         */
        public SuperSymbolReference(ScriptObject base, Symbol referencedName, boolean strictReference,
                Object thisValue) {
            super(base, strictReference, thisValue);
            this.referencedName = referencedName;
        }

        @Override
        public Symbol getReferencedName() {
            return referencedName;
        }

        @Override
        public Object getValue(ExecutionContext cx) {
            return getBase().get(cx, getReferencedName(), getThisValue());
        }

        @Override
        public void putValue(Object w, ExecutionContext cx) {
            assert Type.isType(w) : "invalid value type";

            boolean succeeded = getBase().set(cx, getReferencedName(), w, getThisValue());
            if (!succeeded && isStrictReference()) {
                throw newTypeError(cx, Messages.Key.PropertyNotModifiable, getReferencedName().toString());
            }
        }
    }
}
