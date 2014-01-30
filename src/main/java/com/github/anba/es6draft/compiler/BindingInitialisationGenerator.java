/**
 * Copyright (c) 2012-2014 André Bargull
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

import org.objectweb.asm.Label;
import org.objectweb.asm.Type;

import com.github.anba.es6draft.ast.*;
import com.github.anba.es6draft.compiler.DefaultCodeGenerator.ValType;
import com.github.anba.es6draft.compiler.InstructionVisitor.MethodDesc;
import com.github.anba.es6draft.compiler.InstructionVisitor.MethodType;
import com.github.anba.es6draft.compiler.InstructionVisitor.Variable;

/**
 * <h1>14 ECMAScript Language: Functions and Classes</h1><br>
 * <h2>14.1 Function Definitions</h2>
 * <ul>
 * <li>Runtime Semantics: Binding Initialisation
 * <li>Runtime Semantics: Indexed Binding Initialisation
 * <li>Runtime Semantics: Keyed Binding Initialisation
 * </ul>
 */
final class BindingInitialisationGenerator {
    private static final class Methods {
        // class: AbstractOperations
        static final MethodDesc AbstractOperations_Get = MethodDesc.create(MethodType.Static,
                Types.AbstractOperations, "Get", Type.getMethodType(Types.Object,
                        Types.ExecutionContext, Types.ScriptObject, Types.Object));

        static final MethodDesc AbstractOperations_Get_String = MethodDesc.create(
                MethodType.Static, Types.AbstractOperations, "Get", Type.getMethodType(
                        Types.Object, Types.ExecutionContext, Types.ScriptObject, Types.String));

        // class: EnvironmentRecord
        static final MethodDesc EnvironmentRecord_initialiseBinding = MethodDesc.create(
                MethodType.Interface, Types.EnvironmentRecord, "initialiseBinding",
                Type.getMethodType(Type.VOID_TYPE, Types.String, Types.Object));

        // class: Reference
        static final MethodDesc Reference_putValue = MethodDesc.create(MethodType.Virtual,
                Types.Reference, "putValue",
                Type.getMethodType(Type.VOID_TYPE, Types.Object, Types.ExecutionContext));

        // class: ScriptRuntime
        static final MethodDesc ScriptRuntime_createRestArray = MethodDesc.create(
                MethodType.Static, Types.ScriptRuntime, "createRestArray",
                Type.getMethodType(Types.ScriptObject, Types.Iterator, Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_ensureObject = MethodDesc.create(MethodType.Static,
                Types.ScriptRuntime, "ensureObject",
                Type.getMethodType(Types.ScriptObject, Types.Object, Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_getIterator = MethodDesc.create(MethodType.Static,
                Types.ScriptRuntime, "getIterator",
                Type.getMethodType(Types.Iterator, Types.ScriptObject, Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_iteratorNextAndIgnore = MethodDesc.create(
                MethodType.Static, Types.ScriptRuntime, "iteratorNextAndIgnore",
                Type.getMethodType(Type.VOID_TYPE, Types.Iterator));

        static final MethodDesc ScriptRuntime_iteratorNextOrUndefined = MethodDesc.create(
                MethodType.Static, Types.ScriptRuntime, "iteratorNextOrUndefined",
                Type.getMethodType(Types.Object, Types.Iterator));

        // class: Type
        static final MethodDesc Type_isUndefined = MethodDesc.create(MethodType.Static,
                Types._Type, "isUndefined", Type.getMethodType(Type.BOOLEAN_TYPE, Types.Object));
    }

    private final CodeGenerator codegen;

    BindingInitialisationGenerator(CodeGenerator codegen) {
        this.codegen = codegen;
    }

    void generate(FunctionNode node, Variable<Iterator<?>> iterator, ExpressionVisitor mv) {
        IteratorBindingInitialisation init = new IteratorBindingInitialisation(codegen, mv,
                EnvironmentType.NoEnvironment);

        node.getParameters().accept(init, iterator);
    }

    void generate(Binding node, ExpressionVisitor mv) {
        // stack: [value] -> []
        BindingInitialisation init = new BindingInitialisation(codegen, mv,
                EnvironmentType.NoEnvironment);
        node.accept(init, null);
    }

    void generateWithEnvironment(Binding node, ExpressionVisitor mv) {
        // stack: [env, value] -> []
        BindingInitialisation init = new BindingInitialisation(codegen, mv,
                EnvironmentType.EnvironmentFromStack);
        node.accept(init, null);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static final Variable<Iterator<?>> uncheckedCast(Variable<Iterator> o) {
        return (Variable<Iterator<?>>) (Variable<?>) o;
    }

    private enum EnvironmentType {
        NoEnvironment, EnvironmentFromStack
    }

    private abstract static class RuntimeSemantics<R, V> extends DefaultNodeVisitor<R, V> {
        protected final CodeGenerator codegen;
        protected final ExpressionVisitor mv;
        protected final EnvironmentType environment;

        protected RuntimeSemantics(CodeGenerator codegen, ExpressionVisitor mv,
                EnvironmentType environment) {
            this.codegen = codegen;
            this.mv = mv;
            this.environment = environment;
        }

        protected final void BindingInitialisation(Node node) {
            BindingInitialisation init = new BindingInitialisation(codegen, mv, environment);
            node.accept(init, null);
        }

        protected final void IteratorBindingInitialisation(Node node, Variable<Iterator<?>> iterator) {
            IteratorBindingInitialisation init = new IteratorBindingInitialisation(codegen, mv,
                    environment);
            node.accept(init, iterator);
        }

        protected final void KeyedBindingInitialisation(Node node, String key) {
            KeyedBindingInitialisation init = new KeyedBindingInitialisation(codegen, mv,
                    environment);
            node.accept(init, key);
        }

        protected final void ComputedKeyedBindingInitialisation(Node node, ComputedPropertyName key) {
            ComputedKeyedBindingInitialisation init = new ComputedKeyedBindingInitialisation(
                    codegen, mv, environment);
            node.accept(init, key);
        }

        @Override
        protected final R visit(Node node, V value) {
            throw new IllegalStateException();
        }

        protected final ValType expressionValue(Expression node, ExpressionVisitor mv) {
            return codegen.expressionValue(node, mv);
        }

        protected final ValType expressionBoxedValue(Expression node, ExpressionVisitor mv) {
            return codegen.expressionBoxedValue(node, mv);
        }

        protected final void dupArgs() {
            if (environment == EnvironmentType.EnvironmentFromStack) {
                mv.dup2();
            } else {
                mv.dup();
            }
        }

        protected final void popArgs() {
            if (environment == EnvironmentType.EnvironmentFromStack) {
                mv.pop2();
            } else {
                mv.pop();
            }
        }

        protected final void dupEnv() {
            if (environment == EnvironmentType.EnvironmentFromStack) {
                mv.dup();
            }
        }

        protected final void popEnv() {
            if (environment == EnvironmentType.EnvironmentFromStack) {
                mv.pop();
            }
        }
    }

    private static final class BindingInitialisation extends RuntimeSemantics<Void, Void> {
        private static IdentifierResolution identifierResolution = new IdentifierResolution();

        protected BindingInitialisation(CodeGenerator codegen, ExpressionVisitor mv,
                EnvironmentType environment) {
            super(codegen, mv, environment);
        }

        @Override
        public Void visit(ArrayBindingPattern node, Void _) {
            // step 1: Assert: Type(value) is Object

            // step 2-3:
            // stack: [(env), value] -> [(env)]
            Variable<Iterator<?>> iterator = uncheckedCast(mv.newScratchVariable(Iterator.class));
            mv.loadExecutionContext();
            mv.invoke(Methods.ScriptRuntime_getIterator);
            mv.store(iterator);

            // step 4:
            IteratorBindingInitialisation(node, iterator);

            mv.freeVariable(iterator);

            // stack: [(env)] -> []
            popEnv();

            return null;
        }

        @Override
        public Void visit(ObjectBindingPattern node, Void _) {
            // step 1: Assert: Type(value) is Object

            // step 2: [...]
            for (BindingProperty property : node.getProperties()) {
                // stack: [(env), value] -> [(env), value, (env), value]
                dupArgs();
                // stack: [(env), value, (env), value] -> [(env), value]
                if (property.getPropertyName() == null) {
                    // BindingProperty : SingleNameBinding
                    String name = BoundNames(property.getBinding()).get(0);
                    KeyedBindingInitialisation(property, name);
                } else {
                    // BindingProperty : PropertyName : BindingElement
                    String name = PropName(property.getPropertyName());
                    if (name != null) {
                        KeyedBindingInitialisation(property, name);
                    } else {
                        PropertyName propertyName = property.getPropertyName();
                        assert propertyName instanceof ComputedPropertyName;
                        ComputedKeyedBindingInitialisation(property,
                                (ComputedPropertyName) propertyName);
                    }
                }
            }
            // stack: [(env), value] -> []
            popArgs();

            return null;
        }

        /**
         * 13.2.1.5 Runtime Semantics: BindingInitialisation
         */
        @Override
        public Void visit(BindingIdentifier node, Void _) {
            if (environment == EnvironmentType.EnvironmentFromStack) {
                // stack: [envRec, value] -> [envRec, id, value]
                mv.aconst(node.getName());
                mv.swap();

                // stack: [envRec, id, value] -> []
                mv.invoke(Methods.EnvironmentRecord_initialiseBinding);
            } else {
                assert environment == EnvironmentType.NoEnvironment;
                // stack: [value] -> [ref, value]
                identifierResolution.resolve(node, mv);
                mv.swap();
                // stack: [ref, value] -> []
                mv.loadExecutionContext();
                mv.invoke(Methods.Reference_putValue);
            }

            return null;
        }
    }

    /**
     * 13.2.3.5 Runtime Semantics: IteratorBindingInitialisation<br>
     * 14.1.13 Runtime Semantics: IteratorBindingInitialisation<br>
     * 14.2.8 Runtime Semantics: IteratorBindingInitialisation
     */
    private static final class IteratorBindingInitialisation extends
            RuntimeSemantics<Void, Variable<Iterator<?>>> {
        protected IteratorBindingInitialisation(CodeGenerator codegen, ExpressionVisitor mv,
                EnvironmentType environment) {
            super(codegen, mv, environment);
        }

        @Override
        public Void visit(FormalParameterList node, Variable<Iterator<?>> iterator) {
            assert environment != EnvironmentType.EnvironmentFromStack;

            // stack: [] -> []
            for (FormalParameter formal : node) {
                formal.accept(this, iterator);
            }

            return null;
        }

        @Override
        public Void visit(ArrayBindingPattern node, Variable<Iterator<?>> iterator) {
            // stack: [(env)] -> [(env)]
            for (BindingElementItem element : node.getElements()) {
                dupEnv();
                element.accept(this, iterator);
            }

            return null;
        }

        @Override
        public Void visit(BindingElision node, Variable<Iterator<?>> iterator) {
            // stack: [(env)] -> []
            mv.load(iterator);
            mv.invoke(Methods.ScriptRuntime_iteratorNextAndIgnore);
            popEnv();

            return null;
        }

        @Override
        public Void visit(BindingElement node, Variable<Iterator<?>> iterator) {
            Binding binding = node.getBinding();
            Expression initialiser = node.getInitialiser();

            if (binding instanceof BindingIdentifier) {
                // BindingElement : SingleNameBinding
                // SingleNameBinding : BindingIdentifier Initialiser{opt}

                // steps 1-4
                mv.load(iterator);
                mv.invoke(Methods.ScriptRuntime_iteratorNextOrUndefined);

                // step 5
                // stack: [(env), v] -> [(env), v']
                if (initialiser != null) {
                    Label undef = new Label();
                    mv.dup();
                    mv.invoke(Methods.Type_isUndefined);
                    mv.ifeq(undef);
                    {
                        mv.pop();
                        expressionBoxedValue(initialiser, mv);
                        if (IsAnonymousFunctionDefinition(initialiser)) {
                            SetFunctionName(initialiser, ((BindingIdentifier) binding).getName(),
                                    mv);
                        }
                    }
                    mv.mark(undef);
                }

                // step 6
                // stack: [(env), v'] -> []
                BindingInitialisation(binding);
            } else {
                // BindingElement : BindingPattern Initialiser{opt}
                assert binding instanceof BindingPattern;

                // steps 1-4
                mv.load(iterator);
                mv.invoke(Methods.ScriptRuntime_iteratorNextOrUndefined);

                // step 5
                // stack: [(env), v] -> [(env), v']
                if (initialiser != null) {
                    Label undef = new Label();
                    mv.dup();
                    mv.invoke(Methods.Type_isUndefined);
                    mv.ifeq(undef);
                    {
                        mv.pop();
                        expressionBoxedValue(initialiser, mv);
                    }
                    mv.mark(undef);
                }

                // step 7
                // stack: [(env), v'] -> [(env), v']
                mv.loadExecutionContext();
                mv.invoke(Methods.ScriptRuntime_ensureObject);

                // step 8
                // stack: [(env), v'] -> []
                BindingInitialisation(binding);
            }

            return null;
        }

        @Override
        public Void visit(BindingRestElement node, Variable<Iterator<?>> iterator) {
            mv.load(iterator);
            mv.loadExecutionContext();
            // stack: [(env), array, iterator, cx] -> [(env), rest]
            mv.invoke(Methods.ScriptRuntime_createRestArray);

            // stack: [(env), rest] -> []
            BindingInitialisation(node.getBindingIdentifier());

            return null;
        }
    }

    /**
     * 13.2.3.6 Runtime Semantics: KeyedBindingInitialisation
     */
    private static final class KeyedBindingInitialisation extends RuntimeSemantics<Void, String> {
        protected KeyedBindingInitialisation(CodeGenerator codegen, ExpressionVisitor mv,
                EnvironmentType environment) {
            super(codegen, mv, environment);
        }

        @Override
        public Void visit(BindingProperty node, String propertyName) {
            Binding binding = node.getBinding();
            Expression initialiser = node.getInitialiser();

            // stack: [(env), obj] -> [(env), cx, obj]
            mv.loadExecutionContext();
            mv.swap();

            // stack: [(env), cx, obj] -> [(env), cx, obj, propertyName]
            mv.aconst(propertyName);

            // steps 1-2
            // stack: [(env), cx, obj, propertyName] -> [(env), v]
            mv.invoke(Methods.AbstractOperations_Get_String);

            // step 3
            // stack: [(env), v] -> [(env), v']
            if (initialiser != null) {
                Label undef = new Label();
                mv.dup();
                mv.invoke(Methods.Type_isUndefined);
                mv.ifeq(undef);
                {
                    mv.pop();
                    expressionBoxedValue(initialiser, mv);
                    if (binding instanceof BindingIdentifier
                            && IsAnonymousFunctionDefinition(initialiser)) {
                        SetFunctionName(initialiser, ((BindingIdentifier) binding).getName(), mv);
                    }
                }
                mv.mark(undef);
            }

            if (binding instanceof BindingPattern) {
                // step 4
                // stack: [(env), v'] -> [(env), v']
                mv.loadExecutionContext();
                mv.invoke(Methods.ScriptRuntime_ensureObject);
            }

            // step 5
            // stack: [(env), v'] -> []
            BindingInitialisation(binding);

            return null;
        }
    }

    /**
     * 13.2.3.6 Runtime Semantics: KeyedBindingInitialisation
     */
    private static final class ComputedKeyedBindingInitialisation extends
            RuntimeSemantics<Void, ComputedPropertyName> {
        protected ComputedKeyedBindingInitialisation(CodeGenerator codegen, ExpressionVisitor mv,
                EnvironmentType environment) {
            super(codegen, mv, environment);
        }

        @Override
        public Void visit(BindingProperty node, ComputedPropertyName propertyName) {
            Binding binding = node.getBinding();
            Expression initialiser = node.getInitialiser();

            // stack: [(env), obj] -> [(env), cx, obj]
            mv.loadExecutionContext();
            mv.swap();

            // stack: [(env), cx, obj] -> [(env), cx, obj, propertyName]
            // Runtime Semantics: Evaluation
            // ComputedPropertyName : [ AssignmentExpression ]
            ValType propType = expressionValue(propertyName.getExpression(), mv);
            ToPropertyKey(propType, mv);

            // steps 1-2
            // stack: [(env), cx, obj, propertyName] -> [(env), v]
            mv.invoke(Methods.AbstractOperations_Get);

            // step 3
            // stack: [(env), v] -> [(env), v']
            if (initialiser != null) {
                Label undef = new Label();
                mv.dup();
                mv.invoke(Methods.Type_isUndefined);
                mv.ifeq(undef);
                {
                    mv.pop();
                    expressionBoxedValue(initialiser, mv);
                    if (binding instanceof BindingIdentifier
                            && IsAnonymousFunctionDefinition(initialiser)) {
                        SetFunctionName(initialiser, ((BindingIdentifier) binding).getName(), mv);
                    }
                }
                mv.mark(undef);
            }

            if (binding instanceof BindingPattern) {
                // step 4
                // stack: [(env), v'] -> [(env), v']
                mv.loadExecutionContext();
                mv.invoke(Methods.ScriptRuntime_ensureObject);
            }

            // step 5
            // stack: [(env), v'] -> []
            BindingInitialisation(binding);

            return null;
        }
    }
}
