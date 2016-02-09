/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import com.github.anba.es6draft.ast.scope.Name;
import com.github.anba.es6draft.compiler.assembler.MethodName;
import com.github.anba.es6draft.compiler.assembler.Type;
import com.github.anba.es6draft.compiler.assembler.Value;
import com.github.anba.es6draft.runtime.EnvironmentRecord;

/** 
 *
 */
abstract class BindingOp<ENVREC extends EnvironmentRecord> {
    /**
     * <h1>8.1.1 Environment Records - CreateImmutableBinding(N, S)</h1>
     * <p>
     * Emit function call for: {@link EnvironmentRecord#createImmutableBinding(String, boolean)}
     * <p>
     * stack: [] {@literal ->} []
     * 
     * @param envRec
     *            the environment record
     * @param name
     *            the binding name
     * @param strict
     *            {@code true} for strict immutable bindings
     * @param mv
     *            the instruction visitor
     */
    abstract void createImmutableBinding(Value<? extends ENVREC> envRec, Name name, boolean strict,
            InstructionVisitor mv);

    /**
     * <h1>8.1.1 Environment Records - CreateMutableBinding(N, D)</h1>
     * <p>
     * Emit function call for: {@link EnvironmentRecord#createMutableBinding(String, boolean)}
     * <p>
     * stack: [] {@literal ->} []
     * 
     * @param envRec
     *            the environment record
     * @param name
     *            the binding name
     * @param deletable
     *            {@code true} for deletable mutable bindings
     * @param mv
     *            the instruction visitor
     */
    abstract void createMutableBinding(Value<? extends ENVREC> envRec, Name name, boolean deletable,
            InstructionVisitor mv);

    /**
     * <h1>8.1.1 Environment Records - InitializeBinding(N, V)</h1>
     * <p>
     * Emit function call for: {@link EnvironmentRecord#initializeBinding(String, Object)}
     * <p>
     * stack: [] {@literal ->} []
     * 
     * @param envRec
     *            the environment record
     * @param name
     *            the binding name
     * @param value
     *            the initial binding value
     * @param mv
     *            the instruction visitor
     */
    abstract void initializeBinding(Value<? extends ENVREC> envRec, Name name, Value<?> value,
            InstructionVisitor mv);

    /**
     * <h1>8.1.1 Environment Records - HasBinding(N)</h1>
     * <p>
     * Emit function call for: {@link EnvironmentRecord#hasBinding(String)}
     * <p>
     * stack: [] {@literal ->} [boolean]
     * 
     * @param envRec
     *            the environment record
     * @param name
     *            the binding name
     * @param mv
     *            the instruction visitor
     */
    abstract void hasBinding(Value<? extends ENVREC> envRec, Name name, InstructionVisitor mv);

    /**
     * <h1>8.1.1 Environment Records - GetBindingValue(N,S)</h1>
     * <p>
     * Emit function call for: {@link EnvironmentRecord#getBindingValue(String, boolean)}
     * <p>
     * stack: [] {@literal ->} [{@literal <value>}]
     * 
     * @param envRec
     *            the environment record
     * @param name
     *            the binding name
     * @param strict
     *            {@code true} if in strict-mode code
     * @param mv
     *            the instruction visitor
     */
    abstract void getBindingValue(Value<? extends ENVREC> envRec, Name name, boolean strict,
            InstructionVisitor mv);

    /**
     * <h1>8.1.1 Environment Records - SetMutableBinding(N, V, S)</h1>
     * <p>
     * Emit function call for: {@link EnvironmentRecord#setMutableBinding(String, Object, boolean)}
     * <p>
     * stack: [] {@literal ->} []
     * 
     * @param envRec
     *            the environment record
     * @param name
     *            the binding name
     * @param value
     *            the binding value
     * @param strict
     *            {@code true} if in strict-mode code
     * @param mv
     *            the instruction visitor
     */
    abstract void setMutableBinding(Value<? extends ENVREC> envRec, Name name, Value<?> value,
            boolean strict, InstructionVisitor mv);

    /**
     * Returns the {@code BindingOp} implementation for the binding name.
     * 
     * @param envRec
     *            the environment record
     * @param name
     *            the binding name
     * @return the {@code BindingOp}
     */
    @SuppressWarnings("unchecked")
    static <ENV extends EnvironmentRecord, E extends ENV> BindingOp<ENV> of(Value<E> envRec,
            Name name) {
        return (BindingOp<ENV>) LOOKUP;
    }

    private static final class Methods {
        // class: EnvironmentRecord
        static final MethodName EnvironmentRecord_hasBinding = MethodName.findInterface(
                Types.EnvironmentRecord, "hasBinding",
                Type.methodType(Type.BOOLEAN_TYPE, Types.String));

        static final MethodName EnvironmentRecord_createMutableBinding = MethodName.findInterface(
                Types.EnvironmentRecord, "createMutableBinding",
                Type.methodType(Type.VOID_TYPE, Types.String, Type.BOOLEAN_TYPE));

        static final MethodName EnvironmentRecord_createImmutableBinding = MethodName.findInterface(
                Types.EnvironmentRecord, "createImmutableBinding",
                Type.methodType(Type.VOID_TYPE, Types.String, Type.BOOLEAN_TYPE));

        static final MethodName EnvironmentRecord_initializeBinding = MethodName.findInterface(
                Types.EnvironmentRecord, "initializeBinding",
                Type.methodType(Type.VOID_TYPE, Types.String, Types.Object));

        static final MethodName EnvironmentRecord_getBindingValue = MethodName.findInterface(
                Types.EnvironmentRecord, "getBindingValue",
                Type.methodType(Types.Object, Types.String, Type.BOOLEAN_TYPE));

        static final MethodName EnvironmentRecord_setMutableBinding = MethodName.findInterface(
                Types.EnvironmentRecord, "setMutableBinding",
                Type.methodType(Type.VOID_TYPE, Types.String, Types.Object, Type.BOOLEAN_TYPE));
    }

    protected final void createImmutableBindingShared(Value<? extends ENVREC> envRec, Name name,
            boolean strict, InstructionVisitor mv) {
        mv.load(envRec);
        mv.aconst(name.getIdentifier());
        mv.iconst(strict);
        mv.invoke(Methods.EnvironmentRecord_createImmutableBinding);
    }

    protected final void createMutableBindingShared(Value<? extends ENVREC> envRec, Name name,
            boolean deletable, InstructionVisitor mv) {
        mv.load(envRec);
        mv.aconst(name.getIdentifier());
        mv.iconst(deletable);
        mv.invoke(Methods.EnvironmentRecord_createMutableBinding);
    }

    protected final void initializeBindingShared(Value<? extends ENVREC> envRec, Name name,
            Value<?> value, InstructionVisitor mv) {
        mv.load(envRec);
        mv.aconst(name.getIdentifier());
        mv.load(value);
        mv.invoke(Methods.EnvironmentRecord_initializeBinding);
    }

    protected final void hasBindingShared(Value<? extends EnvironmentRecord> envRec, Name name,
            InstructionVisitor mv) {
        mv.load(envRec);
        mv.aconst(name.getIdentifier());
        mv.invoke(Methods.EnvironmentRecord_hasBinding);
    }

    protected final void getBindingShared(Value<? extends ENVREC> envRec, Name name, boolean strict,
            InstructionVisitor mv) {
        mv.load(envRec);
        mv.aconst(name.getIdentifier());
        mv.iconst(strict);
        mv.invoke(Methods.EnvironmentRecord_getBindingValue);
    }

    protected final void setMutableBindingShared(Value<? extends ENVREC> envRec, Name name,
            Value<?> value, boolean strict, InstructionVisitor mv) {
        mv.load(envRec);
        mv.aconst(name.getIdentifier());
        mv.load(value);
        mv.iconst(strict);
        mv.invoke(Methods.EnvironmentRecord_setMutableBinding);
    }

    static final BindingOp<EnvironmentRecord> LOOKUP = new BindingOp<EnvironmentRecord>() {
        @Override
        void createImmutableBinding(Value<? extends EnvironmentRecord> envRec, Name name,
                boolean strict, InstructionVisitor mv) {
            createImmutableBindingShared(envRec, name, strict, mv);
        }

        @Override
        void createMutableBinding(Value<? extends EnvironmentRecord> envRec, Name name,
                boolean deletable, InstructionVisitor mv) {
            createMutableBindingShared(envRec, name, deletable, mv);
        }

        @Override
        void initializeBinding(Value<? extends EnvironmentRecord> envRec, Name name, Value<?> value,
                InstructionVisitor mv) {
            initializeBindingShared(envRec, name, value, mv);
        }

        @Override
        void hasBinding(Value<? extends EnvironmentRecord> envRec, Name name,
                InstructionVisitor mv) {
            hasBindingShared(envRec, name, mv);
        }

        @Override
        void getBindingValue(Value<? extends EnvironmentRecord> envRec, Name name, boolean strict,
                InstructionVisitor mv) {
            getBindingShared(envRec, name, strict, mv);
        }

        @Override
        void setMutableBinding(Value<? extends EnvironmentRecord> envRec, Name name, Value<?> value,
                boolean strict, InstructionVisitor mv) {
            setMutableBindingShared(envRec, name, value, strict, mv);
        }
    };
}
