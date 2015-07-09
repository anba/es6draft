/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static com.github.anba.es6draft.compiler.DefaultCodeGenerator.SetFunctionName;
import static com.github.anba.es6draft.compiler.DefaultCodeGenerator.ToPropertyKey;
import static com.github.anba.es6draft.semantics.StaticSemantics.BindingPropertyNames;
import static com.github.anba.es6draft.semantics.StaticSemantics.BoundNames;
import static com.github.anba.es6draft.semantics.StaticSemantics.IsAnonymousFunctionDefinition;
import static com.github.anba.es6draft.semantics.StaticSemantics.PropName;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.github.anba.es6draft.ast.*;
import com.github.anba.es6draft.ast.scope.Name;
import com.github.anba.es6draft.ast.scope.Scope;
import com.github.anba.es6draft.compiler.DefaultCodeGenerator.ValType;
import com.github.anba.es6draft.compiler.Labels.TempLabel;
import com.github.anba.es6draft.compiler.StatementGenerator.Completion;
import com.github.anba.es6draft.compiler.assembler.Field;
import com.github.anba.es6draft.compiler.assembler.FieldName;
import com.github.anba.es6draft.compiler.assembler.InstructionAssembler;
import com.github.anba.es6draft.compiler.assembler.Jump;
import com.github.anba.es6draft.compiler.assembler.MethodName;
import com.github.anba.es6draft.compiler.assembler.Type;
import com.github.anba.es6draft.compiler.assembler.Value;
import com.github.anba.es6draft.compiler.assembler.Variable;
import com.github.anba.es6draft.runtime.EnvironmentRecord;
import com.github.anba.es6draft.runtime.FunctionEnvironmentRecord;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.internal.ScriptIterator;

/**
 * <h1>Runtime Semantics: BindingInitialization</h1>
 * <ul>
 * <li>12.1.2 Runtime Semantics: BindingInitialization
 * <li>12.2.4.2.2 Runtime Semantics: BindingInitialization
 * <li>13.2.2.2 Runtime Semantics: BindingInitialization
 * <li>13.2.3.5 Runtime Semantics: BindingInitialization
 * <li>13.14.3 Runtime Semantics: BindingInitialization
 * </ul>
 * 
 * <h2>Runtime Semantics: IteratorBindingInitialization</h2>
 * <ul>
 * <li>13.2.3.6 Runtime Semantics: IteratorBindingInitialization
 * <li>14.1.20 Runtime Semantics: IteratorBindingInitialization
 * <li>14.2.16 Runtime Semantics: IteratorBindingInitialization
 * <li>
 * </ul>
 * 
 * <h2>Runtime Semantics: KeyedBindingInitialization</h2>
 * <ul>
 * <li>13.2.3.7 Runtime Semantics: KeyedBindingInitialization
 * </ul>
 */
final class BindingInitializationGenerator {
    private static final class Fields {
        static final FieldName Undefined_UNDEFINED = FieldName.findStatic(Types.Undefined,
                "UNDEFINED", Types.Undefined);
    }

    private static final class Methods {
        // class: AbstractOperations
        static final MethodName AbstractOperations_GetV = MethodName.findStatic(
                Types.AbstractOperations, "GetV",
                Type.methodType(Types.Object, Types.ExecutionContext, Types.Object, Types.Object));

        static final MethodName AbstractOperations_GetV_String = MethodName.findStatic(
                Types.AbstractOperations, "GetV",
                Type.methodType(Types.Object, Types.ExecutionContext, Types.Object, Types.String));

        static final MethodName AbstractOperations_RequireObjectCoercible = MethodName.findStatic(
                Types.AbstractOperations, "RequireObjectCoercible",
                Type.methodType(Types.Object, Types.ExecutionContext, Types.Object));

        // class: ExecutionContext
        static final MethodName ExecutionContext_setVariableAndLexicalEnvironment = MethodName
                .findVirtual(Types.ExecutionContext, "setVariableAndLexicalEnvironment",
                        Type.methodType(Type.VOID_TYPE, Types.LexicalEnvironment));

        // class: LexicalEnvironment
        static final MethodName LexicalEnvironment_newDeclarativeEnvironment = MethodName
                .findStatic(Types.LexicalEnvironment, "newDeclarativeEnvironment",
                        Type.methodType(Types.LexicalEnvironment, Types.LexicalEnvironment));

        // class: ScriptRuntime
        static final MethodName ScriptRuntime_createRestArray = MethodName.findStatic(
                Types.ScriptRuntime, "createRestArray",
                Type.methodType(Types.ArrayObject, Types.Iterator, Types.ExecutionContext));

        static final MethodName ScriptRuntime_createRestObject = MethodName.findStatic(
                Types.ScriptRuntime, "createRestObject", Type.methodType(Types.OrdinaryObject,
                        Types.Object, Types.String_, Types.ExecutionContext));

        static final MethodName ScriptRuntime_iterate = MethodName.findStatic(Types.ScriptRuntime,
                "iterate",
                Type.methodType(Types.ScriptIterator, Types.Object, Types.ExecutionContext));

        static final MethodName ScriptRuntime_iteratorNextAndIgnore = MethodName.findStatic(
                Types.ScriptRuntime, "iteratorNextAndIgnore",
                Type.methodType(Type.VOID_TYPE, Types.Iterator));

        static final MethodName ScriptRuntime_iteratorNextOrUndefined = MethodName.findStatic(
                Types.ScriptRuntime, "iteratorNextOrUndefined",
                Type.methodType(Types.Object, Types.Iterator));
    }

    private BindingInitializationGenerator() {
    }

    /**
     * 12.1.5.1 Runtime Semantics: InitializeBoundName(name, value, environment)
     * <p>
     * stack: [value] {@literal ->} []
     * 
     * @param node
     *            the binding identifier
     * @param mv
     *            the expression visitor
     */
    static <ENVREC extends EnvironmentRecord> void InitializeBoundName(BindingIdentifier node,
            ExpressionVisitor mv) {
        IdReferenceOp op = IdReferenceOp.of(node);

        /* steps 1-2 (not applicable) */
        /* step 3 */
        // stack: [value] -> [reference, value]
        ValType refType = op.resolveBinding(node, mv);
        mv.swap(ValType.Any, refType);
        // stack: [reference, value] -> []
        op.putValue(node, ValType.Any, mv);
    }

    /**
     * 12.1.5.1 Runtime Semantics: InitializeBoundName(name, value, environment)
     * <p>
     * stack: [] {@literal ->} []
     * 
     * @param envRec
     *            the environment record
     * @param name
     *            the binding name
     * @param value
     *            the value
     * @param mv
     *            the expression visitor
     */
    static <ENVREC extends EnvironmentRecord> void InitializeBoundName(
            Variable<? extends ENVREC> envRec, Name name, Value<?> value, ExpressionVisitor mv) {
        BindingOp<ENVREC> op = BindingOp.of(envRec, name);
        op.initializeBinding(envRec, name, value, mv);
    }

    /**
     * 12.1.5.1 Runtime Semantics: InitializeBoundName(name, value, environment)
     * <p>
     * stack: [] {@literal ->} []
     * 
     * @param envRec
     *            the environment record
     * @param name
     *            the binding name
     * @param mv
     *            the statement visitor
     */
    static <ENVREC extends EnvironmentRecord> void InitializeBoundNameWithUndefined(
            Variable<? extends ENVREC> envRec, Name name, ExpressionVisitor mv) {
        InitializeBoundName(envRec, name, new Field<>(Fields.Undefined_UNDEFINED), mv);
    }

    /**
     * 12.1.5.1 Runtime Semantics: InitializeBoundName(name, value, environment)
     * <p>
     * stack: [] {@literal ->} []
     * 
     * @param mv
     *            the expression visitor
     */
    static <ENVREC extends EnvironmentRecord> void InitializeBoundNameWithInitializer(
            final CodeGenerator codegen, Variable<? extends ENVREC> envRec, final Name name,
            final Expression initializer, final ExpressionVisitor mv) {
        InitializeBoundName(envRec, name, new Value<Object>() {
            @Override
            protected void load(InstructionAssembler assembler) {
                codegen.expressionBoxed(initializer, mv);
                if (IsAnonymousFunctionDefinition(initializer)) {
                    SetFunctionName(initializer, name, mv);
                }
            }
        }, mv);
    }

    /**
     * stack: [value] {@literal ->} []
     * 
     * @param codegen
     *            the code generator
     * @param node
     *            the binding node
     * @param mv
     *            the expression visitor
     */
    static void BindingInitialization(CodeGenerator codegen, Binding node, ExpressionVisitor mv) {
        if (node instanceof BindingIdentifier) {
            InitializeBoundName((BindingIdentifier) node, mv);
        } else {
            BindingInitialization(codegen, (BindingPattern) node, mv);
        }
    }

    /**
     * stack: [value] {@literal ->} []
     * 
     * @param codegen
     *            the code generator
     * @param node
     *            the binding node
     * @param mv
     *            the expression visitor
     */
    static void BindingInitialization(CodeGenerator codegen, BindingPattern node,
            ExpressionVisitor mv) {
        BindingInitialization init = new BindingInitialization(codegen, mv, null);
        node.accept(init, null);
    }

    /**
     * stack: [] {@literal ->} []
     * 
     * @param codegen
     *            the code generator
     * @param envRec
     *            the environment record
     * @param node
     *            the binding node
     * @param value
     *            the value
     * @param mv
     *            the expression visitor
     */
    static <ENVREC extends EnvironmentRecord> void BindingInitialization(CodeGenerator codegen,
            Variable<? extends ENVREC> envRec, Binding node, Value<?> value, ExpressionVisitor mv) {
        if (node instanceof BindingIdentifier) {
            InitializeBoundName(envRec, ((BindingIdentifier) node).getName(), value, mv);
        } else {
            BindingInitialization(codegen, envRec, (BindingPattern) node, value, mv);
        }
    }

    /**
     * stack: [] {@literal ->} []
     * 
     * @param codegen
     *            the code generator
     * @param envRec
     *            the environment record
     * @param node
     *            the binding node
     * @param value
     *            the value
     * @param mv
     *            the expression visitor
     */
    static <ENVREC extends EnvironmentRecord> void BindingInitialization(CodeGenerator codegen,
            Variable<? extends ENVREC> envRec, BindingPattern node, Value<?> value,
            ExpressionVisitor mv) {
        mv.load(value);
        BindingInitialization init = new BindingInitialization(codegen, mv, envRec);
        node.accept(init, null);
    }

    /**
     * stack: [value] {@literal ->} []
     * 
     * @param codegen
     *            the code generator
     * @param envRec
     *            the environment record
     * @param node
     *            the binding node
     * @param mv
     *            the expression visitor
     */
    static <ENVREC extends EnvironmentRecord> void BindingInitialization(CodeGenerator codegen,
            Variable<? extends ENVREC> envRec, BindingPattern node, ExpressionVisitor mv) {
        BindingInitialization init = new BindingInitialization(codegen, mv, envRec);
        node.accept(init, null);
    }

    /**
     * stack: [] {@literal ->} []
     * 
     * @param codegen
     *            the code generator
     * @param node
     *            the function node
     * @param env
     *            the current lexical and variable environment
     * @param iterator
     *            the arguments iterator
     * @param mv
     *            the expression visitor
     */
    static void BindingInitialization(CodeGenerator codegen, FunctionNode node,
            Variable<LexicalEnvironment<FunctionEnvironmentRecord>> env,
            Variable<Iterator<?>> iterator, ExpressionVisitor mv) {
        FormalsIteratorBindingInitialization init = new FormalsIteratorBindingInitialization(
                codegen, mv, env, null);
        node.getParameters().accept(init, iterator);
    }

    /**
     * stack: [] {@literal ->} []
     * 
     * @param codegen
     *            the code generator
     * @param node
     *            the function node
     * @param env
     *            the current lexical and variable environment
     * @param envRec
     *            the current environment record
     * @param iterator
     *            the arguments iterator
     * @param mv
     *            the expression visitor
     */
    static void BindingInitialization(CodeGenerator codegen, FunctionNode node,
            Variable<LexicalEnvironment<FunctionEnvironmentRecord>> env,
            Variable<? extends EnvironmentRecord> envRec, Variable<Iterator<?>> iterator,
            ExpressionVisitor mv) {
        FormalsIteratorBindingInitialization init = new FormalsIteratorBindingInitialization(
                codegen, mv, env, envRec);
        node.getParameters().accept(init, iterator);
    }

    private static abstract class RuntimeSemantics<V> extends
            com.github.anba.es6draft.ast.DefaultVoidNodeVisitor<V> {
        protected final CodeGenerator codegen;
        protected final ExpressionVisitor mv;
        protected final Variable<? extends EnvironmentRecord> envRec;

        RuntimeSemantics(CodeGenerator codegen, ExpressionVisitor mv,
                Variable<? extends EnvironmentRecord> envRec) {
            this.codegen = codegen;
            this.mv = mv;
            this.envRec = envRec;
        }

        protected final void BindingInitialization(BindingPattern node) {
            node.accept(new BindingInitialization(codegen, mv, envRec), null);
        }

        protected final void IteratorBindingInitialization(ArrayBindingPattern node,
                Variable<? extends Iterator<?>> iterator) {
            node.accept(new IteratorBindingInitialization(codegen, mv, envRec), iterator);
        }

        protected final void KeyedBindingInitialization(BindingProperty node,
                Variable<Object> value, String key) {
            node.accept(new LiteralKeyedBindingInitialization(codegen, mv, envRec, value), key);
        }

        protected final void KeyedBindingInitialization(BindingProperty node,
                Variable<Object> value, ComputedPropertyName key) {
            node.accept(new ComputedKeyedBindingInitialization(codegen, mv, envRec, value), key);
        }

        @Override
        protected final void visit(Node node, V value) {
            throw new IllegalStateException();
        }

        protected final ValType expression(Expression node, ExpressionVisitor mv) {
            return codegen.expression(node, mv);
        }

        protected final ValType expressionBoxed(Expression node, ExpressionVisitor mv) {
            return codegen.expressionBoxed(node, mv);
        }

        protected final void emitDefaultInitializer(Expression initializer) {
            // stack: [value] -> [value']
            Jump undef = new Jump();
            mv.dup();
            mv.loadUndefined();
            mv.ifacmpne(undef);
            {
                mv.pop();
                expressionBoxed(initializer, mv);
            }
            mv.mark(undef);
        }

        protected final void emitDefaultInitializer(Expression initializer,
                BindingIdentifier bindingId) {
            // stack: [value] -> [value']
            Jump undef = new Jump();
            mv.dup();
            mv.loadUndefined();
            mv.ifacmpne(undef);
            {
                mv.pop();
                expressionBoxed(initializer, mv);
                if (IsAnonymousFunctionDefinition(initializer)) {
                    SetFunctionName(initializer, bindingId.getName(), mv);
                }
            }
            mv.mark(undef);
        }
    }

    /**
     * <h1>Runtime Semantics: BindingInitialization</h1>
     * <ul>
     * <li>13.2.3.5 Runtime Semantics: BindingInitialization
     * </ul>
     */
    private static final class BindingInitialization extends RuntimeSemantics<Void> {
        BindingInitialization(CodeGenerator codegen, ExpressionVisitor mv,
                Variable<? extends EnvironmentRecord> envRec) {
            super(codegen, mv, envRec);
        }

        @Override
        public void visit(ArrayBindingPattern node, Void value) {
            // step 1-3:
            // stack: [value] -> []
            mv.enterVariableScope();
            Variable<ScriptIterator<?>> iterator = mv.newVariable("iterator", ScriptIterator.class)
                    .uncheckedCast();
            mv.loadExecutionContext();
            mv.lineInfo(node);
            mv.invoke(Methods.ScriptRuntime_iterate);
            mv.store(iterator);

            new IterationGenerator<ArrayBindingPattern, ExpressionVisitor>(codegen) {
                @Override
                protected Completion iterationBody(ArrayBindingPattern node,
                        Variable<ScriptIterator<?>> iterator, ExpressionVisitor mv) {
                    // step 4
                    IteratorBindingInitialization(node, iterator);
                    return Completion.Normal;
                }

                @Override
                protected void epilogue(ArrayBindingPattern node,
                        Variable<ScriptIterator<?>> iterator, ExpressionVisitor mv) {
                    // step 5
                    IteratorClose(node, iterator, mv);
                }

                @Override
                protected Variable<Object> enterIteration(ArrayBindingPattern node,
                        ExpressionVisitor mv) {
                    return mv.enterIteration();
                }

                @Override
                protected List<TempLabel> exitIteration(ArrayBindingPattern node,
                        ExpressionVisitor mv) {
                    return mv.exitIteration();
                }
            }.generate(node, iterator, mv);

            mv.exitVariableScope();
        }

        @Override
        public void visit(final ObjectBindingPattern node, Void value) {
            // stack: [value] -> [value]
            mv.loadExecutionContext();
            mv.swap();
            mv.lineInfo(node);
            mv.invoke(Methods.AbstractOperations_RequireObjectCoercible);

            if (node.getProperties().isEmpty() && node.getRest() == null) {
                // stack: [value] -> []
                mv.pop();
                return;
            }

            // stack: [value] -> []
            mv.enterVariableScope();
            final Variable<Object> val = mv.newVariable("value", Object.class);
            mv.store(val);

            // step 1: [...]
            for (BindingProperty property : node.getProperties()) {
                if (property.getPropertyName() == null) {
                    // BindingProperty : SingleNameBinding
                    Name name = BoundNames(property.getBinding()).get(0);
                    KeyedBindingInitialization(property, val, name.getIdentifier());
                } else {
                    // BindingProperty : PropertyName : BindingElement
                    String name = PropName(property.getPropertyName());
                    if (name != null) {
                        KeyedBindingInitialization(property, val, name);
                    } else {
                        PropertyName propertyName = property.getPropertyName();
                        assert propertyName instanceof ComputedPropertyName;
                        KeyedBindingInitialization(property, val,
                                (ComputedPropertyName) propertyName);
                    }
                }
            }

            BindingRestProperty rest = node.getRest();
            if (rest != null) {
                BindingIdentifier identifier = rest.getBindingIdentifier();
                if (envRec == null) {
                    // stack: [] -> [ref]
                    IdReferenceOp op = IdReferenceOp.of(identifier);
                    op.resolveBinding(identifier, mv);

                    // stack: [ref] -> [ref, object]
                    emitCreateRestObject(node, val);

                    // stack: [ref, value] -> []
                    op.putValue(identifier, ValType.Any, mv);
                } else {
                    BindingOp<EnvironmentRecord> op = BindingOp.of(envRec, identifier.getName());
                    op.initializeBinding(envRec, identifier.getName(), new Value<Object>() {
                        @Override
                        protected void load(InstructionAssembler assembler) {
                            emitCreateRestObject(node, val);
                        }
                    }, mv);
                }
            }

            mv.exitVariableScope();
        }

        private void emitCreateRestObject(ObjectBindingPattern node, Variable<Object> val) {
            // stack: [] -> [object]
            mv.load(val);
            newStringArray(mv, BindingPropertyNames(node));
            mv.loadExecutionContext();
            mv.lineInfo(node.getRest());
            mv.invoke(Methods.ScriptRuntime_createRestObject);
        }

        private static void newStringArray(InstructionVisitor mv, Set<String> strings) {
            mv.anewarray(strings.size(), Types.String);
            int index = 0;
            for (String string : strings) {
                mv.astore(index++, string);
            }
        }
    }

    /**
     * <h2>Runtime Semantics: IteratorBindingInitialization</h2>
     * <ul>
     * <li>13.2.3.6 Runtime Semantics: IteratorBindingInitialization
     * <li>14.1.21 Runtime Semantics: IteratorBindingInitialization
     * <li>14.2.15 Runtime Semantics: IteratorBindingInitialization
     * </ul>
     */
    private static final class FormalsIteratorBindingInitialization extends
            RuntimeSemantics<Variable<? extends Iterator<?>>> {
        private final Variable<LexicalEnvironment<FunctionEnvironmentRecord>> env;
        private final IteratorBindingInitialization iteratorBindingInit;

        FormalsIteratorBindingInitialization(CodeGenerator codegen, ExpressionVisitor mv,

        Variable<LexicalEnvironment<FunctionEnvironmentRecord>> env,
                Variable<? extends EnvironmentRecord> envRec) {
            super(codegen, mv, envRec);
            this.env = env;
            this.iteratorBindingInit = new IteratorBindingInitialization(codegen, mv, envRec);
        }

        @Override
        public void visit(FormalParameterList node, Variable<? extends Iterator<?>> iterator) {
            for (FormalParameter formal : node) {
                formal.accept(this, iterator);
            }
        }

        @Override
        public void visit(FormalParameter node, Variable<? extends Iterator<?>> iterator) {
            Scope scope = node.getScope();
            if (scope != null) {
                mv.enterScope(node);
            }
            if (scope == null || !scope.isPresent()) {
                /* step 1 (+ optimization if no direct eval present in formal parameter) */
                node.getElement().accept(iteratorBindingInit, iterator);
            } else {
                /* steps 2-5 (not applicable) */
                /* steps 6-8 */
                newParameterEnvironment(env);
                /* step 9 */
                node.getElement().accept(iteratorBindingInit, iterator);
                /* steps 10-11 */
                setVariableAndLexicalEnvironment(env);
            }
            if (scope != null) {
                mv.exitScope();
            }
        }

        private void newParameterEnvironment(Variable<? extends LexicalEnvironment<?>> env) {
            // stack: [] -> []
            mv.loadExecutionContext();
            mv.load(env);
            mv.invoke(Methods.LexicalEnvironment_newDeclarativeEnvironment);
            mv.invoke(Methods.ExecutionContext_setVariableAndLexicalEnvironment);
        }

        private void setVariableAndLexicalEnvironment(Variable<? extends LexicalEnvironment<?>> env) {
            // stack: [] -> []
            mv.loadExecutionContext();
            mv.load(env);
            mv.invoke(Methods.ExecutionContext_setVariableAndLexicalEnvironment);
        }
    }

    /**
     * <h2>Runtime Semantics: IteratorBindingInitialization</h2>
     * <ul>
     * <li>13.3.3.6 Runtime Semantics: IteratorBindingInitialization
     * </ul>
     */
    private static final class IteratorBindingInitialization extends
            RuntimeSemantics<Variable<? extends Iterator<?>>> {
        IteratorBindingInitialization(CodeGenerator codegen, ExpressionVisitor mv,
                Variable<? extends EnvironmentRecord> envRec) {
            super(codegen, mv, envRec);
        }

        @Override
        public void visit(ArrayBindingPattern node, Variable<? extends Iterator<?>> iterator) {
            // stack: [] -> []
            for (BindingElementItem element : node.getElements()) {
                element.accept(this, iterator);
            }
        }

        @Override
        public void visit(BindingElision node, Variable<? extends Iterator<?>> iterator) {
            // stack: [] -> []
            mv.load(iterator);
            mv.lineInfo(node);
            mv.invoke(Methods.ScriptRuntime_iteratorNextAndIgnore);
        }

        @Override
        public void visit(final BindingElement node, final Variable<? extends Iterator<?>> iterator) {
            final Binding binding = node.getBinding();
            final Expression initializer = node.getInitializer();

            if (binding instanceof BindingPattern) {
                // BindingElement : BindingPattern Initializer{opt}
                /* steps 1-2 */
                emitIteratorNext(node, iterator);

                /* step 3 */
                // stack: [v] -> [v']
                if (initializer != null) {
                    emitDefaultInitializer(initializer);
                }

                /* step 4 */
                // stack: [v'] -> []
                BindingInitialization((BindingPattern) binding);
                return;
            }

            // BindingElement : SingleNameBinding
            // SingleNameBinding : BindingIdentifier Initializer{opt}
            assert binding instanceof BindingIdentifier;

            if (envRec == null) {
                /* step 1 */
                BindingIdentifier bindingId = (BindingIdentifier) binding;

                /* steps 2-3 */
                // stack: [] -> [ref]
                IdReferenceOp op = IdReferenceOp.of(bindingId);
                op.resolveBinding(bindingId, mv);

                /* steps 4-5 */
                emitIteratorNext(node, iterator);

                /* step 6 */
                // stack: [ref, v] -> [ref, v']
                if (initializer != null) {
                    emitDefaultInitializer(initializer, bindingId);
                }

                /* steps 7-8 */
                // stack: [ref, v'] -> []
                op.putValue(bindingId, ValType.Any, mv);
            } else {
                /* step 1 */
                final BindingIdentifier bindingId = (BindingIdentifier) binding;

                /* steps 2-3, 7-8 */
                BindingOp<EnvironmentRecord> op = BindingOp.of(envRec, bindingId.getName());
                op.initializeBinding(envRec, bindingId.getName(), new Value<Object>() {
                    @Override
                    protected void load(InstructionAssembler assembler) {
                        /* steps 4-5 */
                        emitIteratorNext(node, iterator);

                        /* step 6 */
                        // stack: [<env, id>|ref, v] -> [<env, id>|ref, v']
                        if (initializer != null) {
                            emitDefaultInitializer(initializer, bindingId);
                        }
                    }
                }, mv);
            }
        }

        private void emitIteratorNext(BindingElement node, Variable<? extends Iterator<?>> iterator) {
            mv.load(iterator);
            mv.lineInfo(node);
            mv.invoke(Methods.ScriptRuntime_iteratorNextOrUndefined);
        }

        @Override
        public void visit(final BindingRestElement node,
                final Variable<? extends Iterator<?>> iterator) {
            BindingIdentifier identifier = node.getBindingIdentifier();
            if (envRec == null) {
                /* steps 1-2 */
                // stack: [] -> [<env, id>|ref]
                IdReferenceOp op = IdReferenceOp.of(identifier);
                op.resolveBinding(identifier, mv);

                /* steps 3-5 */
                // stack: [<env, id>|ref] -> [<env, id>|ref, array]
                emitCreateRestArray(node, iterator);

                /* step 5.b */
                // stack: [<env, id>|ref, array] -> []
                op.putValue(identifier, ValType.Any, mv);
            } else {
                /* steps 1-2, 5.b */
                BindingOp<EnvironmentRecord> op = BindingOp.of(envRec, identifier.getName());
                op.initializeBinding(envRec, identifier.getName(), new Value<Object>() {
                    @Override
                    protected void load(InstructionAssembler assembler) {
                        /* steps 3-5 */
                        emitCreateRestArray(node, iterator);
                    }
                }, mv);
            }
        }

        private void emitCreateRestArray(BindingRestElement node,
                Variable<? extends Iterator<?>> iterator) {
            // stack: [] -> [array]
            mv.load(iterator);
            mv.loadExecutionContext();
            mv.lineInfo(node);
            mv.invoke(Methods.ScriptRuntime_createRestArray);
        }
    }

    /**
     * <h2>Runtime Semantics: KeyedBindingInitialization</h2>
     * <ul>
     * <li>13.2.3.7 Runtime Semantics: KeyedBindingInitialization
     * </ul>
     */
    private static abstract class KeyedBindingInitialization<PROPERTYNAME> extends
            RuntimeSemantics<PROPERTYNAME> {
        private final Variable<Object> value;

        KeyedBindingInitialization(CodeGenerator codegen, ExpressionVisitor mv,
                Variable<? extends EnvironmentRecord> envRec, Variable<Object> value) {
            super(codegen, mv, envRec);
            this.value = value;
        }

        abstract ValType evaluatePropertyName(PROPERTYNAME propertyName);

        abstract boolean isSimplePropertyName(PROPERTYNAME propertyName);

        final boolean isSimplePropertyNameOrTarget(BindingIdentifier target,
                PROPERTYNAME propertyName) {
            if (isSimplePropertyName(propertyName)) {
                return true;
            }
            Name resolvedName = target.getResolvedName();
            return resolvedName != null && resolvedName.isLocal();
        }

        final void emitGetV(BindingProperty node, ValType type) {
            // stack: [cx, value, propertyName] -> [v]
            mv.lineInfo(node);
            if (type == ValType.String) {
                mv.invoke(Methods.AbstractOperations_GetV_String);
            } else {
                mv.invoke(Methods.AbstractOperations_GetV);
            }
        }

        @Override
        public void visit(final BindingProperty node, final PROPERTYNAME propertyName) {
            final Binding binding = node.getBinding();
            final Expression initializer = node.getInitializer();

            if (binding instanceof BindingPattern) {
                // stack: [] -> [cx, value]
                mv.loadExecutionContext();
                mv.load(value);

                /* steps 1-2 (Runtime Semantics: BindingInitialization 13.2.3.5) */
                // stack: [cx, value] -> [cx, value, propertyName]
                ValType type = evaluatePropertyName(propertyName);

                /* steps 1-2 */
                // stack: [cx, value, propertyName] -> [v]
                emitGetV(node, type);

                /* step 3 */
                // stack: [v] -> [v']
                if (initializer != null) {
                    emitDefaultInitializer(initializer);
                }

                /* step 4 */
                // stack: [v'] -> []
                BindingInitialization((BindingPattern) binding);
                return;
            }
            assert binding instanceof BindingIdentifier;

            if (envRec == null) {
                /* step 1 */
                BindingIdentifier bindingId = (BindingIdentifier) binding;
                IdReferenceOp op = IdReferenceOp.of(bindingId);

                ValType type;
                if (isSimplePropertyNameOrTarget(bindingId, propertyName)) {
                    /* step 2 */
                    // stack: [] -> [ref]
                    op.resolveBinding(bindingId, mv);

                    // stack: [ref] -> [ref, cx, value]
                    mv.loadExecutionContext();
                    mv.load(value);

                    /* steps 1-2 (Runtime Semantics: BindingInitialization 13.2.3.5) */
                    // stack: [ref, cx, value] -> [ref, cx, value, propertyName]
                    type = evaluatePropertyName(propertyName);
                } else {
                    /* steps 1-2 (Runtime Semantics: BindingInitialization 13.2.3.5) */
                    // stack: [] -> []
                    type = evaluatePropertyName(propertyName);
                    Variable<?> propertyNameVar = mv.newScratchVariable(type.toClass());
                    mv.store(propertyNameVar);

                    /* step 2 */
                    // stack: [] -> [ref]
                    op.resolveBinding(bindingId, mv);

                    // stack: [ref] -> [ref, cx, value, propertyName]
                    mv.loadExecutionContext();
                    mv.load(value);
                    mv.load(propertyNameVar);
                    mv.freeVariable(propertyNameVar);
                }

                /* steps 3-4 */
                // stack: [ref, cx, value, propertyName] -> [ref, v]
                emitGetV(node, type);

                /* step 5 */
                // stack: [ref, v] -> [ref, v']
                if (initializer != null) {
                    emitDefaultInitializer(initializer, bindingId);
                }

                /* steps 6-7 */
                // stack: [ref, v'] -> []
                op.putValue(bindingId, ValType.Any, mv);
            } else {
                /* step 1 */
                final BindingIdentifier bindingId = (BindingIdentifier) binding;
                /* steps 2, 6-7 */
                BindingOp<EnvironmentRecord> op = BindingOp.of(envRec, bindingId.getName());
                op.initializeBinding(envRec, bindingId.getName(), new Value<Object>() {
                    @Override
                    protected void load(InstructionAssembler assembler) {
                        // stack: [] -> [cx, value]
                        mv.loadExecutionContext();
                        mv.load(value);

                        /* steps 1-2 (Runtime Semantics: BindingInitialization 13.2.3.5) */
                        // stack: [cx, value] -> [cx, value, propertyName]
                        ValType type = evaluatePropertyName(propertyName);

                        /* steps 3-4 */
                        // stack: [<env, id>|ref, cx, value, propertyName] -> [<env, id>|ref, v]
                        emitGetV(node, type);

                        /* step 5 */
                        // stack: [<env, id>|ref, v] -> [<env, id>|ref, v']
                        if (initializer != null) {
                            emitDefaultInitializer(initializer, bindingId);
                        }
                    }
                }, mv);
            }
        }
    }

    /**
     * <h2>Runtime Semantics: KeyedBindingInitialization</h2>
     * <ul>
     * <li>13.2.3.7 Runtime Semantics: KeyedBindingInitialization
     * </ul>
     */
    private static final class LiteralKeyedBindingInitialization extends
            KeyedBindingInitialization<String> {
        LiteralKeyedBindingInitialization(CodeGenerator codegen, ExpressionVisitor mv,
                Variable<? extends EnvironmentRecord> envRec, Variable<Object> value) {
            super(codegen, mv, envRec, value);
        }

        @Override
        ValType evaluatePropertyName(String propertyName) {
            mv.aconst(propertyName);
            return ValType.String;
        }

        @Override
        boolean isSimplePropertyName(String propertyName) {
            return true;
        }
    }

    /**
     * <h2>Runtime Semantics: KeyedBindingInitialization</h2>
     * <ul>
     * <li>13.2.3.7 Runtime Semantics: KeyedBindingInitialization
     * </ul>
     */
    private static final class ComputedKeyedBindingInitialization extends
            KeyedBindingInitialization<ComputedPropertyName> {
        ComputedKeyedBindingInitialization(CodeGenerator codegen, ExpressionVisitor mv,
                Variable<? extends EnvironmentRecord> envRec, Variable<Object> value) {
            super(codegen, mv, envRec, value);
        }

        @Override
        ValType evaluatePropertyName(ComputedPropertyName propertyName) {
            // Runtime Semantics: Evaluation
            // ComputedPropertyName : [ AssignmentExpression ]
            ValType propType = expression(propertyName.getExpression(), mv);
            return ToPropertyKey(propType, mv);
        }

        @Override
        boolean isSimplePropertyName(ComputedPropertyName propertyName) {
            return propertyName.getExpression() instanceof Literal;
        }
    }
}
