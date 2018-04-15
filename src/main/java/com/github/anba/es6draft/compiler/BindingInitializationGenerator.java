/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static com.github.anba.es6draft.compiler.DefaultCodeGenerator.SetFunctionName;
import static com.github.anba.es6draft.compiler.DefaultCodeGenerator.ToPropertyKey;
import static com.github.anba.es6draft.semantics.StaticSemantics.BoundNames;
import static com.github.anba.es6draft.semantics.StaticSemantics.IsAnonymousFunctionDefinition;
import static com.github.anba.es6draft.semantics.StaticSemantics.PropName;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import com.github.anba.es6draft.ast.*;
import com.github.anba.es6draft.ast.scope.Name;
import com.github.anba.es6draft.ast.scope.ParameterScope;
import com.github.anba.es6draft.compiler.DefaultCodeGenerator.ValType;
import com.github.anba.es6draft.compiler.Labels.TempLabel;
import com.github.anba.es6draft.compiler.StatementGenerator.Completion;
import com.github.anba.es6draft.compiler.assembler.FieldName;
import com.github.anba.es6draft.compiler.assembler.Jump;
import com.github.anba.es6draft.compiler.assembler.MethodName;
import com.github.anba.es6draft.compiler.assembler.MutableValue;
import com.github.anba.es6draft.compiler.assembler.Type;
import com.github.anba.es6draft.compiler.assembler.Value;
import com.github.anba.es6draft.compiler.assembler.Variable;
import com.github.anba.es6draft.runtime.DeclarativeEnvironmentRecord;
import com.github.anba.es6draft.runtime.EnvironmentRecord;
import com.github.anba.es6draft.runtime.FunctionEnvironmentRecord;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.IndexedMap;
import com.github.anba.es6draft.runtime.internal.ScriptIterator;

/**
 * <h1>Runtime Semantics: BindingInitialization</h1>
 * <ul>
 * <li>12.1.5 Runtime Semantics: BindingInitialization
 * <li>13.3.3.5 Runtime Semantics: BindingInitialization
 * </ul>
 * 
 * <h2>Runtime Semantics: IteratorBindingInitialization</h2>
 * <ul>
 * <li>13.3.3.6 Runtime Semantics: IteratorBindingInitialization
 * <li>14.1.18 Runtime Semantics: IteratorBindingInitialization
 * <li>14.2.14 Runtime Semantics: IteratorBindingInitialization
 * </ul>
 * 
 * <h2>Runtime Semantics: KeyedBindingInitialization</h2>
 * <ul>
 * <li>13.3.3.7 Runtime Semantics: KeyedBindingInitialization
 * </ul>
 */
final class BindingInitializationGenerator {
    private static final class Fields {
        static final FieldName Collections_EMPTY_SET = FieldName.findStatic(Types.Collections, "EMPTY_SET", Types.Set);
    }

    private static final class Methods {
        // class: AbstractOperations
        static final MethodName AbstractOperations_GetV = MethodName.findStatic(Types.AbstractOperations, "GetV",
                Type.methodType(Types.Object, Types.ExecutionContext, Types.Object, Types.Object));

        static final MethodName AbstractOperations_GetV_long = MethodName.findStatic(Types.AbstractOperations, "GetV",
                Type.methodType(Types.Object, Types.ExecutionContext, Types.Object, Type.LONG_TYPE));

        static final MethodName AbstractOperations_GetV_String = MethodName.findStatic(Types.AbstractOperations, "GetV",
                Type.methodType(Types.Object, Types.ExecutionContext, Types.Object, Types.String));

        static final MethodName AbstractOperations_RequireObjectCoercible = MethodName.findStatic(
                Types.AbstractOperations, "RequireObjectCoercible",
                Type.methodType(Types.Object, Types.ExecutionContext, Types.Object));

        // class: ExecutionContext
        static final MethodName ExecutionContext_getVariableEnvironmentRecord = MethodName.findVirtual(
                Types.ExecutionContext, "getVariableEnvironmentRecord", Type.methodType(Types.EnvironmentRecord));

        static final MethodName ExecutionContext_setVariableAndLexicalEnvironment = MethodName.findVirtual(
                Types.ExecutionContext, "setVariableAndLexicalEnvironment",
                Type.methodType(Type.VOID_TYPE, Types.LexicalEnvironment));

        // class: LexicalEnvironment
        static final MethodName LexicalEnvironment_newDeclarativeEnvironment = MethodName.findStatic(
                Types.LexicalEnvironment, "newDeclarativeEnvironment",
                Type.methodType(Types.LexicalEnvironment, Types.LexicalEnvironment));

        // class: ArrayOperations
        static final MethodName ArrayOperations_createRestArray = MethodName.findStatic(Types.ArrayOperations,
                "createRestArray", Type.methodType(Types.ArrayObject, Types.Iterator, Types.ExecutionContext));

        // class: ObjectOperations
        static final MethodName ObjectOperations_createRestObject = MethodName.findStatic(Types.ObjectOperations,
                "createRestObject",
                Type.methodType(Types.OrdinaryObject, Types.Object, Types.Set, Types.ExecutionContext));

        // class: IteratorOperations
        static final MethodName IteratorOperations_iterate = MethodName.findStatic(Types.IteratorOperations, "iterate",
                Type.methodType(Types.ScriptIterator, Types.Object, Types.ExecutionContext));

        static final MethodName IteratorOperations_iteratorNextAndIgnore = MethodName.findStatic(
                Types.IteratorOperations, "iteratorNextAndIgnore", Type.methodType(Type.VOID_TYPE, Types.Iterator));

        static final MethodName IteratorOperations_iteratorNextOrUndefined = MethodName.findStatic(
                Types.IteratorOperations, "iteratorNextOrUndefined", Type.methodType(Types.Object, Types.Iterator));

        // class: HashSet
        static final MethodName HashSet_new = MethodName.findConstructor(Types.HashSet,
                Type.methodType(Type.VOID_TYPE));

        static final MethodName HashSet_add = MethodName.findVirtual(Types.HashSet, "add",
                Type.methodType(Type.BOOLEAN_TYPE, Types.Object));
    }

    private BindingInitializationGenerator() {
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
     *            the code visitor
     */
    static <ENVREC extends EnvironmentRecord> void InitializeBoundName(Value<? extends ENVREC> envRec, Name name,
            Value<?> value, CodeVisitor mv) {
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
     *            the code visitor
     */
    static <ENVREC extends EnvironmentRecord> void InitializeBoundNameWithUndefined(Variable<? extends ENVREC> envRec,
            Name name, CodeVisitor mv) {
        InitializeBoundName(envRec, name, mv.undefinedValue(), mv);
    }

    /**
     * 12.1.5.1 Runtime Semantics: InitializeBoundName(name, value, environment)
     * <p>
     * stack: [] {@literal ->} []
     * 
     * @param mv
     *            the code visitor
     */
    static <ENVREC extends EnvironmentRecord> void InitializeBoundNameWithInitializer(CodeGenerator codegen,
            Variable<? extends ENVREC> envRec, Name name, Expression initializer, CodeVisitor mv) {
        InitializeBoundName(envRec, name, __ -> {
            codegen.expressionBoxed(initializer, mv);
            if (IsAnonymousFunctionDefinition(initializer)) {
                SetFunctionName(initializer, name, mv);
            }
        }, mv);
    }

    /**
     * 13.3.3.5 Runtime Semantics: BindingInitialization
     * <p>
     * stack: [value] {@literal ->} []
     * 
     * @param codegen
     *            the code generator
     * @param node
     *            the binding node
     * @param mv
     *            the code visitor
     */
    static void BindingInitialization(CodeGenerator codegen, BindingPattern node, CodeVisitor mv) {
        BindingInitialization init = new BindingInitialization(codegen, mv, null);
        node.accept(init, null);
    }

    /**
     * 12.1.5 Runtime Semantics: BindingInitialization<br>
     * 13.3.3.5 Runtime Semantics: BindingInitialization
     * <p>
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
     *            the code visitor
     */
    static <ENVREC extends EnvironmentRecord> void BindingInitialization(CodeGenerator codegen,
            Variable<? extends ENVREC> envRec, Binding node, Value<?> value, CodeVisitor mv) {
        if (node instanceof BindingIdentifier) {
            InitializeBoundName(envRec, ((BindingIdentifier) node).getName(), value, mv);
        } else {
            BindingInitialization(codegen, envRec, (BindingPattern) node, value, mv);
        }
    }

    /**
     * 13.3.3.5 Runtime Semantics: BindingInitialization
     * <p>
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
     *            the code visitor
     */
    static <ENVREC extends EnvironmentRecord> void BindingInitialization(CodeGenerator codegen,
            Variable<? extends ENVREC> envRec, BindingPattern node, Value<?> value, CodeVisitor mv) {
        mv.load(value);
        BindingInitialization init = new BindingInitialization(codegen, mv, envRec);
        node.accept(init, null);
    }

    /**
     * 13.3.3.5 Runtime Semantics: BindingInitialization
     * <p>
     * stack: [value] {@literal ->} []
     * 
     * @param codegen
     *            the code generator
     * @param envRec
     *            the environment record
     * @param node
     *            the binding node
     * @param mv
     *            the code visitor
     */
    static <ENVREC extends EnvironmentRecord> void BindingInitialization(CodeGenerator codegen,
            Variable<? extends ENVREC> envRec, BindingPattern node, CodeVisitor mv) {
        BindingInitialization init = new BindingInitialization(codegen, mv, envRec);
        node.accept(init, null);
    }

    /**
     * 14.1.18 Runtime Semantics: IteratorBindingInitialization
     * <p>
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
     *            the code visitor
     */
    static void BindingInitialization(CodeGenerator codegen, FunctionNode node,
            Variable<LexicalEnvironment<FunctionEnvironmentRecord>> env, Variable<Iterator<?>> iterator,
            CodeVisitor mv) {
        FormalsIteratorBindingInitialization init = new FormalsIteratorBindingInitialization(codegen, mv, env, null);
        node.getParameters().accept(init, iterator);
    }

    /**
     * 14.1.18 Runtime Semantics: IteratorBindingInitialization
     * <p>
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
     *            the code visitor
     */
    static void BindingInitialization(CodeGenerator codegen, FunctionNode node,
            Variable<LexicalEnvironment<FunctionEnvironmentRecord>> env, Variable<? extends EnvironmentRecord> envRec,
            Variable<Iterator<?>> iterator, CodeVisitor mv) {
        FormalsIteratorBindingInitialization init = new FormalsIteratorBindingInitialization(codegen, mv, env, envRec);
        node.getParameters().accept(init, iterator);
    }

    private static abstract class RuntimeSemantics<V> extends com.github.anba.es6draft.ast.DefaultVoidNodeVisitor<V> {
        protected final CodeGenerator codegen;
        protected final CodeVisitor mv;
        protected final Variable<? extends EnvironmentRecord> envRec;

        RuntimeSemantics(CodeGenerator codegen, CodeVisitor mv, Variable<? extends EnvironmentRecord> envRec) {
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

        protected final void KeyedBindingInitialization(BindingProperty node, String key, Variable<Object> value,
                Variable<HashSet<?>> propertyNames) {
            node.accept(new LiteralKeyedBindingInitialization(codegen, mv, envRec, value, propertyNames), key);
        }

        protected final void KeyedBindingInitialization(BindingProperty node, ComputedPropertyName key,
                Variable<Object> value, Variable<HashSet<?>> propertyNames) {
            node.accept(new ComputedKeyedBindingInitialization(codegen, mv, envRec, value, propertyNames), key);
        }

        protected final void RestBindingInitialization(Binding node, Variable<Object> value,
                Variable<HashSet<?>> propertyNames) {
            node.accept(new RestBindingInitialization(codegen, mv, envRec, value, propertyNames), null);
        }

        @Override
        protected final void visit(Node node, V value) {
            throw new IllegalStateException();
        }

        protected final ValType expression(Expression node, CodeVisitor mv) {
            return codegen.expression(node, mv);
        }

        protected final ValType expressionBoxed(Expression node, CodeVisitor mv) {
            return codegen.expressionBoxed(node, mv);
        }

        protected final boolean isUndefined(Expression initializer) {
            if (initializer instanceof UnaryExpression) {
                UnaryExpression unary = (UnaryExpression) initializer;
                return unary.getOperator() == UnaryExpression.Operator.VOID && unary.getOperand() instanceof Literal;
            }
            return false;
        }

        protected final void emitDefaultInitializer(Expression initializer) {
            // stack: [value] -> [value']
            if (!isUndefined(initializer)) {
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
        }

        protected final void emitDefaultInitializer(Expression initializer, BindingIdentifier bindingId) {
            // stack: [value] -> [value']
            if (!isUndefined(initializer)) {
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
    }

    /**
     * <h1>Runtime Semantics: BindingInitialization</h1>
     * <ul>
     * <li>13.3.3.5 Runtime Semantics: BindingInitialization
     * </ul>
     */
    private static final class BindingInitialization extends RuntimeSemantics<Void> {
        BindingInitialization(CodeGenerator codegen, CodeVisitor mv, Variable<? extends EnvironmentRecord> envRec) {
            super(codegen, mv, envRec);
        }

        @Override
        public void visit(ArrayBindingPattern node, Void value) {
            // steps 1-3:
            // stack: [value] -> []
            mv.enterVariableScope();
            Variable<ScriptIterator<?>> iterator = mv.newVariable("iterator", ScriptIterator.class).uncheckedCast();
            mv.loadExecutionContext();
            mv.lineInfo(node);
            mv.invoke(Methods.IteratorOperations_iterate);
            mv.store(iterator);

            new IterationGenerator<ArrayBindingPattern>(codegen) {
                @Override
                protected Completion iterationBody(ArrayBindingPattern node, Variable<ScriptIterator<?>> iterator,
                        CodeVisitor mv) {
                    // step 4
                    IteratorBindingInitialization(node, iterator);
                    return Completion.Normal;
                }

                @Override
                protected void epilogue(ArrayBindingPattern node, Variable<ScriptIterator<?>> iterator,
                        CodeVisitor mv) {
                    // step 5
                    IteratorClose(node, iterator, mv);
                }

                @Override
                protected MutableValue<Object> enterIteration(ArrayBindingPattern node, CodeVisitor mv) {
                    return mv.enterIteration();
                }

                @Override
                protected List<TempLabel> exitIteration(ArrayBindingPattern node, CodeVisitor mv) {
                    return mv.exitIteration();
                }
            }.generate(node, iterator, mv);

            mv.exitVariableScope();
        }

        @Override
        public void visit(ObjectBindingPattern node, Void value) {
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
            Variable<Object> val = mv.newVariable("value", Object.class);
            mv.store(val);

            Variable<HashSet<?>> propertyNames;
            if (!node.getProperties().isEmpty() && node.getRest() != null) {
                propertyNames = mv.newVariable("propertyNames", HashSet.class).uncheckedCast();
                mv.anew(Methods.HashSet_new);
                mv.store(propertyNames);
            } else {
                propertyNames = null;
            }

            // step 1: [...]
            for (BindingProperty property : node.getProperties()) {
                if (property.getPropertyName() == null) {
                    // BindingProperty : SingleNameBinding
                    Name name = BoundNames(property.getBinding()).get(0);
                    KeyedBindingInitialization(property, name.getIdentifier(), val, propertyNames);
                } else {
                    // BindingProperty : PropertyName : BindingElement
                    String name = PropName(property.getPropertyName());
                    if (name != null) {
                        KeyedBindingInitialization(property, name, val, propertyNames);
                    } else {
                        PropertyName propertyName = property.getPropertyName();
                        assert propertyName instanceof ComputedPropertyName;
                        KeyedBindingInitialization(property, (ComputedPropertyName) propertyName, val, propertyNames);
                    }
                }
            }

            BindingRestProperty rest = node.getRest();
            if (rest != null) {
                RestBindingInitialization(rest.getBinding(), val, propertyNames);
            }

            mv.exitVariableScope();
        }
    }

    /**
     * <h2>Runtime Semantics: IteratorBindingInitialization</h2>
     * <ul>
     * <li>13.3.3.6 Runtime Semantics: IteratorBindingInitialization
     * <li>14.1.18 Runtime Semantics: IteratorBindingInitialization
     * <li>14.2.14 Runtime Semantics: IteratorBindingInitialization
     * </ul>
     */
    private static final class FormalsIteratorBindingInitialization
            extends RuntimeSemantics<Variable<? extends Iterator<?>>> {
        private final Variable<LexicalEnvironment<FunctionEnvironmentRecord>> env;
        private final IteratorBindingInitialization iteratorBindingInit;

        FormalsIteratorBindingInitialization(CodeGenerator codegen, CodeVisitor mv,
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
            ParameterScope scope = node.getScope();
            if (scope != null) {
                mv.enterScope(node);
            }
            if (scope == null || !scope.isPresent()) {
                /* step 1 (+ optimization if no direct eval present in formal parameter) */
                node.getElement().accept(iteratorBindingInit, iterator);
            } else {
                assert !codegen.isEnabled(CompatibilityOption.SingleParameterEnvironment);

                /* steps 2-5 (not applicable) */
                /* steps 6-8 */
                newParameterEnvironment(env);
                if (!scope.varDeclaredNames().isEmpty()) {
                    Variable<DeclarativeEnvironmentRecord> paramEnvRec = mv.newVariable("paramEnvRec",
                            DeclarativeEnvironmentRecord.class);
                    getVariableEnvironmentRecord(paramEnvRec, mv);
                    for (Name varName : scope.varDeclaredNames()) {
                        BindingOp<DeclarativeEnvironmentRecord> op = BindingOp.of(paramEnvRec, varName);
                        op.createMutableBinding(paramEnvRec, varName, false, mv);
                        op.initializeBinding(paramEnvRec, varName, mv.undefinedValue(), mv);
                    }
                }
                /* step 9 */
                node.getElement().accept(iteratorBindingInit, iterator);
                /* steps 10-11 */
                setVariableAndLexicalEnvironment(env);
                /* step 12 (return) */
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

        private <R extends EnvironmentRecord> void getVariableEnvironmentRecord(Variable<? extends R> envRec,
                CodeVisitor mv) {
            mv.loadExecutionContext();
            mv.invoke(Methods.ExecutionContext_getVariableEnvironmentRecord);
            if (envRec.getType() != Types.EnvironmentRecord) {
                mv.checkcast(envRec.getType());
            }
            mv.store(envRec);
        }
    }

    /**
     * <h2>Runtime Semantics: IteratorBindingInitialization</h2>
     * <ul>
     * <li>13.3.3.6 Runtime Semantics: IteratorBindingInitialization
     * </ul>
     */
    private static final class IteratorBindingInitialization extends RuntimeSemantics<Variable<? extends Iterator<?>>> {
        IteratorBindingInitialization(CodeGenerator codegen, CodeVisitor mv,
                Variable<? extends EnvironmentRecord> envRec) {
            super(codegen, mv, envRec);
        }

        @Override
        public void visit(ArrayBindingPattern node, Variable<? extends Iterator<?>> iterator) {
            for (BindingElementItem element : node.getElements()) {
                element.accept(this, iterator);
            }
        }

        @Override
        public void visit(BindingElision node, Variable<? extends Iterator<?>> iterator) {
            mv.load(iterator);
            mv.lineInfo(node);
            mv.invoke(Methods.IteratorOperations_iteratorNextAndIgnore);
        }

        @Override
        public void visit(BindingElement node, Variable<? extends Iterator<?>> iterator) {
            Binding binding = node.getBinding();
            Expression initializer = node.getInitializer();

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
                BindingIdentifier bindingId = (BindingIdentifier) binding;

                /* steps 2-3, 7-8 */
                BindingOp<EnvironmentRecord> op = BindingOp.of(envRec, bindingId.getName());
                op.initializeBinding(envRec, bindingId.getName(), __ -> {
                    /* steps 4-5 */
                    emitIteratorNext(node, iterator);

                    /* step 6 */
                    // stack: [<env, id>|ref, v] -> [<env, id>|ref, v']
                    if (initializer != null) {
                        emitDefaultInitializer(initializer, bindingId);
                    }
                }, mv);
            }
        }

        private void emitIteratorNext(BindingElement node, Variable<? extends Iterator<?>> iterator) {
            mv.load(iterator);
            mv.lineInfo(node);
            mv.invoke(Methods.IteratorOperations_iteratorNextOrUndefined);
        }

        @Override
        public void visit(BindingRestElement node, Variable<? extends Iterator<?>> iterator) {
            Binding binding = node.getBinding();
            if (binding instanceof BindingPattern) {
                // BindingRestElement : ... BindingPattern
                // stack: [] -> [array]
                emitCreateRestArray(node, iterator);

                // stack: [array] -> []
                BindingInitialization((BindingPattern) binding);
                return;
            }

            // BindingRestElement : ... BindingIdentifier
            BindingIdentifier identifier = (BindingIdentifier) binding;
            if (envRec == null) {
                /* steps 1-2 */
                // stack: [] -> [ref]
                IdReferenceOp op = IdReferenceOp.of(identifier);
                op.resolveBinding(identifier, mv);

                /* steps 3-5 */
                // stack: [ref] -> [ref, array]
                emitCreateRestArray(node, iterator);

                /* step 5.b */
                // stack: [ref, array] -> []
                op.putValue(identifier, ValType.Any, mv);
            } else {
                /* steps 1-2, 5.b */
                BindingOp<EnvironmentRecord> op = BindingOp.of(envRec, identifier.getName());
                op.initializeBinding(envRec, identifier.getName(), __ -> {
                    /* steps 3-5 */
                    emitCreateRestArray(node, iterator);
                }, mv);
            }
        }

        private void emitCreateRestArray(BindingRestElement node, Variable<? extends Iterator<?>> iterator) {
            // stack: [] -> [array]
            mv.load(iterator);
            mv.loadExecutionContext();
            mv.lineInfo(node);
            mv.invoke(Methods.ArrayOperations_createRestArray);
        }
    }

    private static final class RestBindingInitialization extends RuntimeSemantics<Void> {
        private final Variable<Object> value;
        private final Variable<HashSet<?>> propertyNames;

        RestBindingInitialization(CodeGenerator codegen, CodeVisitor mv, Variable<? extends EnvironmentRecord> envRec,
                Variable<Object> value, Variable<HashSet<?>> propertyNames) {
            super(codegen, mv, envRec);
            this.value = value;
            this.propertyNames = propertyNames;
        }

        @Override
        public void visit(BindingIdentifier node, Void value) {
            // FIXME: spec bug? - evaluation order for resolving ref and CopyDataProperties
            // The current spec calls CopyDataProperties before resolving the reference.
            if (envRec == null) {
                // stack: [] -> [ref]
                IdReferenceOp op = IdReferenceOp.of(node);
                op.resolveBinding(node, mv);

                // stack: [ref] -> [ref, object]
                emitCreateRestObject(node);

                // stack: [ref, object] -> []
                op.putValue(node, ValType.Any, mv);
            } else {
                BindingOp<EnvironmentRecord> op = BindingOp.of(envRec, node.getName());
                op.initializeBinding(envRec, node.getName(), __ -> emitCreateRestObject(node), mv);
            }
        }

        @Override
        protected void visit(BindingPattern node, Void value) {
            // stack: [] -> [object]
            emitCreateRestObject(node);

            // stack: [object] -> []
            BindingInitialization(node);
        }

        private void emitCreateRestObject(Node node) {
            // stack: [] -> [object]
            mv.load(value);
            if (propertyNames != null) {
                mv.load(propertyNames);
            } else {
                mv.get(Fields.Collections_EMPTY_SET);
            }
            mv.loadExecutionContext();
            mv.lineInfo(node);
            mv.invoke(Methods.ObjectOperations_createRestObject);
        }
    }

    /**
     * <h2>Runtime Semantics: KeyedBindingInitialization</h2>
     * <ul>
     * <li>13.3.3.7 Runtime Semantics: KeyedBindingInitialization
     * </ul>
     */
    private static abstract class KeyedBindingInitialization<PROPERTYNAME> extends RuntimeSemantics<PROPERTYNAME> {
        private final Variable<Object> value;

        KeyedBindingInitialization(CodeGenerator codegen, CodeVisitor mv, Variable<? extends EnvironmentRecord> envRec,
                Variable<Object> value) {
            super(codegen, mv, envRec);
            this.value = value;
        }

        abstract ValType evaluatePropertyName(PROPERTYNAME propertyName);

        abstract boolean isSimplePropertyName(PROPERTYNAME propertyName);

        final boolean isSimplePropertyNameOrTarget(BindingIdentifier target, PROPERTYNAME propertyName) {
            if (isSimplePropertyName(propertyName)) {
                return true;
            }
            return false;
        }

        final void emitGetV(BindingProperty node, ValType type) {
            // stack: [cx, value, propertyName] -> [v]
            mv.lineInfo(node);
            switch (type) {
            case Number_uint:
                mv.invoke(Methods.AbstractOperations_GetV_long);
                break;
            case String:
                mv.invoke(Methods.AbstractOperations_GetV_String);
                break;
            case Any:
                mv.invoke(Methods.AbstractOperations_GetV);
                break;
            default:
                throw new AssertionError();
            }
        }

        @Override
        public void visit(BindingProperty node, PROPERTYNAME propertyName) {
            Binding binding = node.getBinding();
            Expression initializer = node.getInitializer();

            if (binding instanceof BindingPattern) {
                // stack: [] -> [cx, value]
                mv.loadExecutionContext();
                mv.load(value);

                /* steps 1-2 (Runtime Semantics: BindingInitialization 13.3.3.5) */
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
                    /* steps 2-3 */
                    // stack: [] -> [ref]
                    op.resolveBinding(bindingId, mv);

                    // stack: [ref] -> [ref, cx, value]
                    mv.loadExecutionContext();
                    mv.load(value);

                    /* steps 1-2 (Runtime Semantics: BindingInitialization 13.3.3.5) */
                    // stack: [ref, cx, value] -> [ref, cx, value, propertyName]
                    type = evaluatePropertyName(propertyName);
                } else {
                    /* steps 1-2 (Runtime Semantics: BindingInitialization 13.3.3.5) */
                    // stack: [] -> []
                    type = evaluatePropertyName(propertyName);
                    Value<?> propertyNameVar = mv.storeTemporary(type.toClass());

                    /* steps 2-3 */
                    // stack: [] -> [ref]
                    op.resolveBinding(bindingId, mv);

                    // stack: [ref] -> [ref, cx, value, propertyName]
                    mv.loadExecutionContext();
                    mv.load(value);
                    mv.load(propertyNameVar);
                }

                /* steps 4-5 */
                // stack: [ref, cx, value, propertyName] -> [ref, v]
                emitGetV(node, type);

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
                BindingIdentifier bindingId = (BindingIdentifier) binding;
                /* steps 2-3, 7-8 */
                BindingOp<EnvironmentRecord> op = BindingOp.of(envRec, bindingId.getName());
                op.initializeBinding(envRec, bindingId.getName(), __ -> {
                    // stack: [] -> [cx, value]
                    mv.loadExecutionContext();
                    mv.load(value);

                    /* steps 1-2 (Runtime Semantics: BindingInitialization 13.3.3.5) */
                    // stack: [cx, value] -> [cx, value, propertyName]
                    ValType type = evaluatePropertyName(propertyName);

                    /* steps 4-5 */
                    // stack: [cx, value, propertyName] -> [v]
                    emitGetV(node, type);

                    /* step 6 */
                    // stack: [v] -> [v']
                    if (initializer != null) {
                        emitDefaultInitializer(initializer, bindingId);
                    }
                }, mv);
            }
        }
    }

    /**
     * <h2>Runtime Semantics: KeyedBindingInitialization</h2>
     * <ul>
     * <li>13.3.3.7 Runtime Semantics: KeyedBindingInitialization
     * </ul>
     */
    private static final class LiteralKeyedBindingInitialization extends KeyedBindingInitialization<String> {
        private final Variable<HashSet<?>> propertyNames;

        LiteralKeyedBindingInitialization(CodeGenerator codegen, CodeVisitor mv,
                Variable<? extends EnvironmentRecord> envRec, Variable<Object> value,
                Variable<HashSet<?>> propertyNames) {
            super(codegen, mv, envRec, value);
            this.propertyNames = propertyNames;
        }

        @Override
        ValType evaluatePropertyName(String propertyName) {
            if (propertyNames != null) {
                mv.load(propertyNames);
                mv.aconst(propertyName);
                mv.invoke(Methods.HashSet_add);
                mv.pop();
            }
            long index = IndexedMap.toIndex(propertyName);
            if (IndexedMap.isIndex(index)) {
                mv.lconst(index);
                return ValType.Number_uint;
            }
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
     * <li>13.3.3.7 Runtime Semantics: KeyedBindingInitialization
     * </ul>
     */
    private static final class ComputedKeyedBindingInitialization
            extends KeyedBindingInitialization<ComputedPropertyName> {
        private final Variable<HashSet<?>> propertyNames;

        ComputedKeyedBindingInitialization(CodeGenerator codegen, CodeVisitor mv,
                Variable<? extends EnvironmentRecord> envRec, Variable<Object> value,
                Variable<HashSet<?>> propertyNames) {
            super(codegen, mv, envRec, value);
            this.propertyNames = propertyNames;
        }

        @Override
        ValType evaluatePropertyName(ComputedPropertyName propertyName) {
            if (propertyNames != null) {
                mv.load(propertyNames);
            }
            // Runtime Semantics: Evaluation
            // ComputedPropertyName : [ AssignmentExpression ]
            ValType propType = expression(propertyName.getExpression(), mv);
            ToPropertyKey(propType, mv);
            if (propertyNames != null) {
                mv.dupX1();
                mv.invoke(Methods.HashSet_add);
                mv.pop();
            }
            return ValType.Any;
        }

        @Override
        boolean isSimplePropertyName(ComputedPropertyName propertyName) {
            return propertyName.getExpression() instanceof Literal;
        }
    }
}
