/**
 * Copyright (c) 2012-2015 André Bargull
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

import java.util.Iterator;

import com.github.anba.es6draft.ast.*;
import com.github.anba.es6draft.ast.scope.Name;
import com.github.anba.es6draft.compiler.DefaultCodeGenerator.ValType;
import com.github.anba.es6draft.compiler.assembler.Jump;
import com.github.anba.es6draft.compiler.assembler.MethodName;
import com.github.anba.es6draft.compiler.assembler.Type;
import com.github.anba.es6draft.compiler.assembler.Variable;
import com.github.anba.es6draft.runtime.EnvironmentRecord;

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
    private static final class Methods {
        // class: AbstractOperations
        static final MethodName AbstractOperations_GetV = MethodName.findStatic(
                Types.AbstractOperations, "GetV",
                Type.methodType(Types.Object, Types.ExecutionContext, Types.Object, Types.Object));

        static final MethodName AbstractOperations_GetV_String = MethodName.findStatic(
                Types.AbstractOperations, "GetV",
                Type.methodType(Types.Object, Types.ExecutionContext, Types.Object, Types.String));

        // class: EnvironmentRecord
        static final MethodName EnvironmentRecord_initializeBinding = MethodName.findInterface(
                Types.EnvironmentRecord, "initializeBinding",
                Type.methodType(Type.VOID_TYPE, Types.String, Types.Object));

        // class: Reference
        static final MethodName Reference_putValue = MethodName.findVirtual(Types.Reference,
                "putValue", Type.methodType(Type.VOID_TYPE, Types.Object, Types.ExecutionContext));

        // class: ScriptRuntime
        static final MethodName ScriptRuntime_createRestArray = MethodName.findStatic(
                Types.ScriptRuntime, "createRestArray",
                Type.methodType(Types.ArrayObject, Types.Iterator, Types.ExecutionContext));

        static final MethodName ScriptRuntime_getIterator = MethodName.findStatic(
                Types.ScriptRuntime, "getIterator",
                Type.methodType(Types.Iterator, Types.Object, Types.ExecutionContext));

        static final MethodName ScriptRuntime_iteratorNextAndIgnore = MethodName.findStatic(
                Types.ScriptRuntime, "iteratorNextAndIgnore",
                Type.methodType(Type.VOID_TYPE, Types.Iterator));

        static final MethodName ScriptRuntime_iteratorNextOrUndefined = MethodName.findStatic(
                Types.ScriptRuntime, "iteratorNextOrUndefined",
                Type.methodType(Types.Object, Types.Iterator));

        // class: Type
        static final MethodName Type_isUndefined = MethodName.findStatic(Types._Type,
                "isUndefined", Type.methodType(Type.BOOLEAN_TYPE, Types.Object));
    }

    private static final IdentifierResolution identifierResolution = new IdentifierResolution();

    private BindingInitializationGenerator() {
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
            BindingInitialization init = new BindingInitialization(codegen, mv,
                    EnvironmentType.NoEnvironment, null);
            node.accept(init, null);
        }
    }

    /**
     * stack: [envRec, value] {@literal ->} []
     * 
     * @param codegen
     *            the code generator
     * @param node
     *            the binding node
     * @param mv
     *            the expression visitor
     */
    static void BindingInitializationWithEnvironment(CodeGenerator codegen, Binding node,
            ExpressionVisitor mv) {
        if (node instanceof BindingIdentifier) {
            InitializeBoundNameWithEnvironment((BindingIdentifier) node, mv);
        } else {
            // stack: [env, value] -> []
            BindingInitialization init = new BindingInitialization(codegen, mv,
                    EnvironmentType.EnvironmentFromStack, null);
            node.accept(init, null);
        }
    }

    /**
     * stack: [envRec, value] {@literal ->} []
     * 
     * @param codegen
     *            the code generator
     * @param node
     *            the binding node
     * @param mv
     *            the expression visitor
     */
    static void BindingInitializationWithEnvironment(CodeGenerator codegen, Name name,
            ExpressionVisitor mv) {
        // stack: [envRec, value] -> [envRec, id, value]
        mv.aconst(name.getIdentifier());
        mv.swap();
        // stack: [envRec, id, value] -> []
        mv.invoke(Methods.EnvironmentRecord_initializeBinding);
    }

    /**
     * stack: [] {@literal ->} []
     * 
     * @param codegen
     *            the code generator
     * @param node
     *            the function node
     * @param iterator
     *            the arguments iterator
     * @param mv
     *            the expression visitor
     */
    static void BindingInitialization(CodeGenerator codegen, FunctionNode node,
            Variable<Iterator<?>> iterator, ExpressionVisitor mv) {
        IteratorBindingInitialization init = new IteratorBindingInitialization(codegen, mv,
                EnvironmentType.NoEnvironment, null);
        node.getParameters().accept(init, iterator);
    }

    /**
     * stack: [] {@literal ->} []
     * 
     * @param codegen
     *            the code generator
     * @param node
     *            the function node
     * @param iterator
     *            the arguments iterator
     * @param mv
     *            the expression visitor
     */
    static void BindingInitializationWithEnvironment(CodeGenerator codegen,
            Variable<? extends EnvironmentRecord> envRec, FunctionNode node,
            Variable<Iterator<?>> iterator, ExpressionVisitor mv) {
        IteratorBindingInitialization init = new IteratorBindingInitialization(codegen, mv,
                EnvironmentType.EnvironmentFromLocal, envRec);
        node.getParameters().accept(init, iterator);
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
    static void InitializeBoundName(BindingIdentifier identifier, ExpressionVisitor mv) {
        // stack: [value] -> [reference, value]
        ResolveBinding(identifier, mv);
        mv.swap();
        // stack: [reference, value] -> []
        PutValue(mv);
    }

    /**
     * 12.1.5.1 Runtime Semantics: InitializeBoundName(name, value, environment)
     * <p>
     * stack: [envRec, value] {@literal ->} []
     * 
     * @param node
     *            the binding identifier
     * @param mv
     *            the expression visitor
     */
    static void InitializeBoundNameWithEnvironment(BindingIdentifier identifier,
            ExpressionVisitor mv) {
        // stack: [envRec, value] -> [envRec, id, value]
        mv.aconst(identifier.getName().getIdentifier());
        mv.swap();
        // stack: [envRec, id, value] -> []
        mv.invoke(Methods.EnvironmentRecord_initializeBinding);
    }

    /**
     * 12.1.5.1 Runtime Semantics: InitializeBoundName(name, value, environment)
     * <p>
     * stack: [envRec, name, value] {@literal ->} []
     * 
     * @param name
     *            the binding name
     * @param mv
     *            the expression visitor
     */
    static void InitializeBoundNameWithValue(ExpressionVisitor mv) {
        // stack: [envRec, name, value] -> []
        mv.invoke(Methods.EnvironmentRecord_initializeBinding);
    }

    /**
     * stack: [] {@literal ->} [Reference]
     * 
     * @param node
     *            the binding identifier node
     * @param mv
     *            the expression visitor
     */
    static void ResolveBinding(BindingIdentifier node, ExpressionVisitor mv) {
        identifierResolution.resolve(node, mv);
    }

    /**
     * stack: [Reference, Object] {@literal ->} []
     * 
     * @param mv
     *            the expression visitor
     */
    private static void PutValue(ExpressionVisitor mv) {
        mv.loadExecutionContext();
        mv.invoke(Methods.Reference_putValue);
    }

    private enum EnvironmentType {
        NoEnvironment, EnvironmentFromStack, EnvironmentFromLocal
    }

    private static abstract class RuntimeSemantics<V> extends DefaultVoidNodeVisitor<V> {
        protected final CodeGenerator codegen;
        protected final ExpressionVisitor mv;
        protected final EnvironmentType environment;
        protected final Variable<? extends EnvironmentRecord> envRec;

        RuntimeSemantics(CodeGenerator codegen, ExpressionVisitor mv, EnvironmentType environment,
                Variable<? extends EnvironmentRecord> envRec) {
            this.codegen = codegen;
            this.mv = mv;
            this.environment = environment;
            this.envRec = envRec;
            assert (environment == EnvironmentType.EnvironmentFromLocal) == (envRec != null);
        }

        protected final void BindingInitialization(BindingPattern node) {
            node.accept(new BindingInitialization(codegen, mv, environment, envRec), null);
        }

        protected final void IteratorBindingInitialization(ArrayBindingPattern node,
                Variable<Iterator<?>> iterator) {
            node.accept(new IteratorBindingInitialization(codegen, mv, environment, envRec),
                    iterator);
        }

        protected final void KeyedBindingInitialization(BindingProperty node,
                Variable<Object> value, String key) {
            node.accept(new LiteralKeyedBindingInitialization(codegen, mv, environment, envRec,
                    value), key);
        }

        protected final void KeyedBindingInitialization(BindingProperty node,
                Variable<Object> value, ComputedPropertyName key) {
            node.accept(new ComputedKeyedBindingInitialization(codegen, mv, environment, envRec,
                    value), key);
        }

        @Override
        protected final void visit(Node node, V value) {
            throw new IllegalStateException();
        }

        protected final ValType expressionValue(Expression node, ExpressionVisitor mv) {
            return codegen.expressionValue(node, mv);
        }

        protected final ValType expressionBoxedValue(Expression node, ExpressionVisitor mv) {
            return codegen.expressionBoxedValue(node, mv);
        }

        protected final void dupEnvIfPresent() {
            if (environment == EnvironmentType.EnvironmentFromStack) {
                mv.dup();
            }
        }

        protected final void popEnvIfPresent() {
            if (environment == EnvironmentType.EnvironmentFromStack) {
                mv.pop();
            }
        }

        protected final void prepareInitializeReferencedBindingOrPut(BindingIdentifier identifier) {
            // TODO: Enable asserts for non-legacy code
            // Load environment record and binding identifier on stack to avoid swap instructions.
            if (environment == EnvironmentType.EnvironmentFromLocal) {
                // assert mv.getScope().isDeclared(identifier.getName());
                // stack: [] -> [envRec, id]
                mv.load(envRec);
                mv.aconst(identifier.getName().getIdentifier());
            } else if (environment == EnvironmentType.EnvironmentFromStack) {
                // assert mv.getScope().isDeclared(identifier.getName());
                // stack: [envRec] -> [envRec, id]
                mv.aconst(identifier.getName().getIdentifier());
            } else {
                assert environment == EnvironmentType.NoEnvironment;
                // stack: [] -> [ref]
                ResolveBinding(identifier, mv);
            }
        }

        protected final void initializeReferencedBindingOrPut(BindingIdentifier identifier) {
            if (environment == EnvironmentType.NoEnvironment) {
                // stack: [ref, value] -> []
                PutValue(mv);
            } else {
                // stack: [envRec, id, value] -> []
                mv.invoke(Methods.EnvironmentRecord_initializeBinding);
            }
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
                EnvironmentType environment, Variable<? extends EnvironmentRecord> envRec) {
            super(codegen, mv, environment, envRec);
        }

        @Override
        public void visit(ArrayBindingPattern node, Void value) {
            // step 1-2:
            // stack: [(env), value] -> [(env)]
            Variable<Iterator<?>> iterator = mv.newScratchVariable(Iterator.class).uncheckedCast();
            mv.lineInfo(node);
            mv.loadExecutionContext();
            mv.invoke(Methods.ScriptRuntime_getIterator);
            mv.store(iterator);

            // step 3:
            IteratorBindingInitialization(node, iterator);

            mv.freeVariable(iterator);

            // stack: [(env)] -> []
            popEnvIfPresent();
        }

        @Override
        public void visit(ObjectBindingPattern node, Void value) {
            // stack: [(env), value] -> [(env)]
            Variable<Object> val = mv.newScratchVariable(Object.class);
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

            mv.freeVariable(val);

            // stack: [(env)] -> []
            popEnvIfPresent();
        }
    }

    /**
     * <h2>Runtime Semantics: IteratorBindingInitialization</h2>
     * <ul>
     * <li>13.2.3.6 Runtime Semantics: IteratorBindingInitialization
     * <li>14.1.21 Runtime Semantics: IteratorBindingInitialization
     * <li>14.2.15 Runtime Semantics: IteratorBindingInitialization
     * <li>
     * </ul>
     */
    private static final class IteratorBindingInitialization extends
            RuntimeSemantics<Variable<Iterator<?>>> {
        IteratorBindingInitialization(CodeGenerator codegen, ExpressionVisitor mv,
                EnvironmentType environment, Variable<? extends EnvironmentRecord> envRec) {
            super(codegen, mv, environment, envRec);
        }

        @Override
        public void visit(FormalParameterList node, Variable<Iterator<?>> iterator) {
            // stack: [(env)] -> [(env)]
            for (FormalParameter formal : node) {
                formal.accept(this, iterator);
            }
        }

        @Override
        public void visit(ArrayBindingPattern node, Variable<Iterator<?>> iterator) {
            // stack: [(env)] -> [(env)]
            for (BindingElementItem element : node.getElements()) {
                element.accept(this, iterator);
            }
        }

        @Override
        public void visit(BindingElision node, Variable<Iterator<?>> iterator) {
            // stack: [(env)] -> [(env)]
            mv.load(iterator);
            mv.invoke(Methods.ScriptRuntime_iteratorNextAndIgnore);
        }

        @Override
        public void visit(BindingElement node, Variable<Iterator<?>> iterator) {
            Binding binding = node.getBinding();
            Expression initializer = node.getInitializer();

            if (binding instanceof BindingIdentifier) {
                // BindingElement : SingleNameBinding
                // SingleNameBinding : BindingIdentifier Initializer{opt}
                /* step 1 */
                BindingIdentifier bindingIdentifier = (BindingIdentifier) binding;

                /* step 2 */
                // stack: [(env)] -> [(env), <env, id>|ref]
                dupEnvIfPresent();
                prepareInitializeReferencedBindingOrPut(bindingIdentifier);

                /* steps 3-6 */
                mv.load(iterator);
                mv.invoke(Methods.ScriptRuntime_iteratorNextOrUndefined);

                /* step 7 */
                // stack: [(env), <env, id>|ref, v] -> [(env), <env, id>|ref, v']
                if (initializer != null) {
                    Jump undef = new Jump();
                    mv.dup();
                    mv.invoke(Methods.Type_isUndefined);
                    mv.ifeq(undef);
                    {
                        mv.pop();
                        expressionBoxedValue(initializer, mv);
                        if (IsAnonymousFunctionDefinition(initializer)) {
                            SetFunctionName(initializer, bindingIdentifier.getName(), mv);
                        }
                    }
                    mv.mark(undef);
                }

                /* steps 8-9 */
                // stack: [(env), <env, id>|ref, v'] -> [(env)]
                initializeReferencedBindingOrPut(bindingIdentifier);
            } else {
                // BindingElement : BindingPattern Initializer{opt}
                assert binding instanceof BindingPattern;

                // stack: [(env)] -> [(env), (env)]
                dupEnvIfPresent();

                /* steps 1-4 */
                mv.load(iterator);
                mv.invoke(Methods.ScriptRuntime_iteratorNextOrUndefined);

                /* step 5 */
                // stack: [(env), (env), v] -> [(env), (env), v']
                if (initializer != null) {
                    Jump undef = new Jump();
                    mv.dup();
                    mv.invoke(Methods.Type_isUndefined);
                    mv.ifeq(undef);
                    {
                        mv.pop();
                        expressionBoxedValue(initializer, mv);
                    }
                    mv.mark(undef);
                }

                /* step 6 */
                // stack: [(env), (env), v'] -> [(env)]
                BindingInitialization((BindingPattern) binding);
            }
        }

        @Override
        public void visit(BindingRestElement node, Variable<Iterator<?>> iterator) {
            /* step 1 */
            // stack: [(env)] -> [(env), <env, id>|ref]
            dupEnvIfPresent();
            prepareInitializeReferencedBindingOrPut(node.getBindingIdentifier());

            /* steps 2-4 */
            mv.load(iterator);
            mv.loadExecutionContext();
            // stack: [(env), <env, id>|ref, iterator, cx] -> [(env), <env, id>|ref, array]
            mv.invoke(Methods.ScriptRuntime_createRestArray);

            /* step 4.c */
            // stack: [(env), <env, id>|ref, array] -> [(env)]
            initializeReferencedBindingOrPut(node.getBindingIdentifier());
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
                EnvironmentType environment, Variable<? extends EnvironmentRecord> envRec,
                Variable<Object> value) {
            super(codegen, mv, environment, envRec);
            this.value = value;
        }

        abstract ValType evaluatePropertyName(PROPERTYNAME propertyName);

        abstract boolean isSimplePropertyName(PROPERTYNAME propertyName);

        @Override
        public void visit(BindingProperty node, PROPERTYNAME propertyName) {
            Binding binding = node.getBinding();
            Expression initializer = node.getInitializer();

            if (binding instanceof BindingPattern) {
                // stack: [(env)] -> [(env), (env), cx, value]
                dupEnvIfPresent();
                mv.loadExecutionContext();
                mv.load(value);

                /* steps 1-2 (Runtime Semantics: BindingInitialization 13.2.3.5) */
                // stack: [(env), (env), cx, value] -> [(env), (env), cx, value, propertyName]
                ValType type = evaluatePropertyName(propertyName);

                /* steps 1-2 */
                // stack: [(env), (env), cx, value, propertyName] -> [(env), (env), v]
                if (type == ValType.String) {
                    mv.invoke(Methods.AbstractOperations_GetV_String);
                } else {
                    mv.invoke(Methods.AbstractOperations_GetV);
                }

                /* step 3 */
                // stack: [(env), (env), v] -> [(env), (env), v']
                if (initializer != null) {
                    Jump undef = new Jump();
                    mv.dup();
                    mv.invoke(Methods.Type_isUndefined);
                    mv.ifeq(undef);
                    {
                        mv.pop();
                        expressionBoxedValue(initializer, mv);
                    }
                    mv.mark(undef);
                }

                /* step 4 */
                // stack: [(env), (env), v'] -> [(env)]
                BindingInitialization((BindingPattern) binding);
                return;
            }
            assert binding instanceof BindingIdentifier;
            /* step 1 */
            BindingIdentifier bindingId = (BindingIdentifier) binding;

            ValType type;
            if (environment != EnvironmentType.NoEnvironment) {
                /* step 2 */
                // stack: [(env)] -> [(env), env, id]
                dupEnvIfPresent();
                prepareInitializeReferencedBindingOrPut(bindingId);

                // stack: [(env), env, id] -> [(env), env, id, cx, value]
                mv.loadExecutionContext();
                mv.load(value);

                /* steps 1-2 (Runtime Semantics: BindingInitialization 13.2.3.5) */
                // stack: [(env), env, id, cx, value] -> [(env), env, id, cx, value, propertyName]
                type = evaluatePropertyName(propertyName);
            } else if (isSimplePropertyName(propertyName)) {
                /* step 2 */
                // stack: [] -> [ref]
                prepareInitializeReferencedBindingOrPut(bindingId);

                // stack: [ref] -> [ref, cx, value]
                mv.loadExecutionContext();
                mv.load(value);

                /* steps 1-2 (Runtime Semantics: BindingInitialization 13.2.3.5) */
                // stack: [ref, cx, value] -> [ref, cx, value, propertyName]
                type = evaluatePropertyName(propertyName);
            } else {
                /* steps 1-2 (Runtime Semantics: BindingInitialization 13.2.3.5) */
                // stack: [] -> [propertyName]
                type = evaluatePropertyName(propertyName);

                /* step 2 */
                // stack: [propertyName] -> [ref, propertyName]
                prepareInitializeReferencedBindingOrPut(bindingId);
                mv.swap();

                // stack: [ref, propertyName] -> [ref, cx, value, propertyName]
                mv.loadExecutionContext();
                mv.swap();
                mv.load(value);
                mv.swap();
            }

            /* steps 3-4 */
            // stack: [(env), <env, id>|ref, cx, value, propertyName] -> [(env), <env, id>|ref, v]
            if (type == ValType.String) {
                mv.invoke(Methods.AbstractOperations_GetV_String);
            } else {
                mv.invoke(Methods.AbstractOperations_GetV);
            }

            /* step 5 */
            // stack: [(env), <env, id>|ref, v] -> [(env), <env, id>|ref, v']
            if (initializer != null) {
                Jump undef = new Jump();
                mv.dup();
                mv.invoke(Methods.Type_isUndefined);
                mv.ifeq(undef);
                {
                    mv.pop();
                    expressionBoxedValue(initializer, mv);
                    if (IsAnonymousFunctionDefinition(initializer)) {
                        SetFunctionName(initializer, bindingId.getName(), mv);
                    }
                }
                mv.mark(undef);
            }

            /* steps 6-7 */
            // stack: [(env), <env, id>|ref, v'] -> [(env)]
            initializeReferencedBindingOrPut(bindingId);
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
                EnvironmentType environment, Variable<? extends EnvironmentRecord> envRec,
                Variable<Object> value) {
            super(codegen, mv, environment, envRec, value);
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
                EnvironmentType environment, Variable<? extends EnvironmentRecord> envRec,
                Variable<Object> value) {
            super(codegen, mv, environment, envRec, value);
        }

        @Override
        ValType evaluatePropertyName(ComputedPropertyName propertyName) {
            // Runtime Semantics: Evaluation
            // ComputedPropertyName : [ AssignmentExpression ]
            ValType propType = expressionValue(propertyName.getExpression(), mv);
            return ToPropertyKey(propType, mv);
        }

        @Override
        boolean isSimplePropertyName(ComputedPropertyName propertyName) {
            return propertyName.getExpression() instanceof Literal;
        }
    }
}
