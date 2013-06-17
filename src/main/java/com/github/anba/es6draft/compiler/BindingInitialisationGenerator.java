/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static com.github.anba.es6draft.runtime.AbstractOperations.ToString;
import static com.github.anba.es6draft.semantics.StaticSemantics.BoundNames;
import static com.github.anba.es6draft.semantics.StaticSemantics.PropName;

import org.objectweb.asm.Label;
import org.objectweb.asm.Type;

import com.github.anba.es6draft.ast.*;
import com.github.anba.es6draft.compiler.DefaultCodeGenerator.ValType;
import com.github.anba.es6draft.compiler.InstructionVisitor.MethodDesc;
import com.github.anba.es6draft.compiler.InstructionVisitor.MethodType;

/**
 * <h1>13 Functions and Generators</h1><br>
 * <h2>13.1 Function Definitions</h2>
 * <ul>
 * <li>Runtime Semantics: Binding Initialisation
 * </ul>
 */
class BindingInitialisationGenerator {
    private static class Methods {
        // class: AbstractOperations
        static final MethodDesc AbstractOperations_Get = MethodDesc.create(MethodType.Static,
                Types.AbstractOperations, "Get", Type.getMethodType(Types.Object,
                        Types.ExecutionContext, Types.ScriptObject, Types.String));

        static final MethodDesc AbstractOperations_ToObject = MethodDesc.create(MethodType.Static,
                Types.AbstractOperations, "ToObject",
                Type.getMethodType(Types.ScriptObject, Types.ExecutionContext, Types.Object));

        // class: EnvironmentRecord
        static final MethodDesc EnvironmentRecord_initialiseBinding = MethodDesc.create(
                MethodType.Interface, Types.EnvironmentRecord, "initialiseBinding",
                Type.getMethodType(Type.VOID_TYPE, Types.String, Types.Object));

        // class: LexicalEnvironment
        static final MethodDesc LexicalEnvironment_getEnvRec = MethodDesc.create(
                MethodType.Virtual, Types.LexicalEnvironment, "getEnvRec",
                Type.getMethodType(Types.EnvironmentRecord));

        // class: Reference
        static final MethodDesc Reference_PutValue = MethodDesc.create(MethodType.Virtual,
                Types.Reference, "PutValue",
                Type.getMethodType(Type.VOID_TYPE, Types.Object, Types.ExecutionContext));

        // class: ScriptRuntime
        static final MethodDesc ScriptRuntime_createRestArray = MethodDesc.create(
                MethodType.Static, Types.ScriptRuntime, "createRestArray", Type.getMethodType(
                        Types.ScriptObject, Types.ScriptObject, Type.INT_TYPE,
                        Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_throw = MethodDesc.create(MethodType.Static,
                Types.ScriptRuntime, "_throw",
                Type.getMethodType(Types.ScriptException, Types.Object));

        // class: Type
        static final MethodDesc Type_isUndefined = MethodDesc.create(MethodType.Static,
                Types._Type, "isUndefined", Type.getMethodType(Type.BOOLEAN_TYPE, Types.Object));
    }

    private final CodeGenerator codegen;

    BindingInitialisationGenerator(CodeGenerator codegen) {
        this.codegen = codegen;
    }

    void generate(FunctionNode node, ExpressionVisitor mv) {
        BindingInitialisation init = new BindingInitialisation(codegen, mv,
                EnvironmentType.NoEnvironment);

        node.getParameters().accept(init, null);
    }

    void generate(Binding node, ExpressionVisitor mv) {
        BindingInitialisation init = new BindingInitialisation(codegen, mv,
                EnvironmentType.NoEnvironment);
        node.accept(init, null);
    }

    void generateWithEnvironment(Binding node, ExpressionVisitor mv) {
        BindingInitialisation init = new BindingInitialisation(codegen, mv,
                EnvironmentType.EnvironmentFromStack);
        node.accept(init, null);
    }

    private enum EnvironmentType {
        NoEnvironment, EnvironmentFromStack, EnvironmentFromParameter
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

        protected final void IndexedBindingInitialisation(Node node, int index) {
            IndexedBindingInitialisation init = new IndexedBindingInitialisation(codegen, mv,
                    environment);
            node.accept(init, index);
        }

        protected final void KeyedBindingInitialisation(Node node, String key) {
            KeyedBindingInitialisation init = new KeyedBindingInitialisation(codegen, mv,
                    environment);
            node.accept(init, key);
        }

        @Override
        protected final R visit(Node node, V value) {
            throw new IllegalStateException();
        }

        protected final ValType expressionValue(Expression node, ExpressionVisitor mv) {
            return codegen.expressionValue(node, mv);
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
    }

    private static final class BindingInitialisation extends RuntimeSemantics<Void, Void> {
        private static IdentifierResolution identifierResolution = new IdentifierResolution();

        protected BindingInitialisation(CodeGenerator codegen, ExpressionVisitor mv,
                EnvironmentType environment) {
            super(codegen, mv, environment);
        }

        @Override
        public Void visit(FormalParameterList node, Void value) {
            IndexedBindingInitialisation(node, 0);

            return null;
        }

        @Override
        public Void visit(ArrayBindingPattern node, Void _) {
            // step 1: Assert: Type(value) is Object

            // FIXME: spec bug -> internal `Assert` should never occur
            Label assertion = new Label();
            mv.dup();
            mv.instanceOf(Types.ScriptObject);
            mv.ifne(assertion);
            mv.aconst("Type(value) is Object");
            mv.invoke(Methods.ScriptRuntime_throw);
            mv.pop(); // explicit pop required
            mv.mark(assertion);

            // step 2:
            // stack: [(env), value] -> []
            IndexedBindingInitialisation(node, 0);

            return null;
        }

        @Override
        public Void visit(ObjectBindingPattern node, Void _) {
            // step 1: Assert: Type(value) is Object

            // FIXME: spec bug -> internal `Assert` should never occur
            Label assertion = new Label();
            mv.dup();
            mv.instanceOf(Types.ScriptObject);
            mv.ifne(assertion);
            mv.aconst("Type(value) is Object");
            mv.invoke(Methods.ScriptRuntime_throw);
            mv.pop(); // explicit pop required
            mv.mark(assertion);

            // step 2: [...]
            for (BindingProperty property : node.getProperties()) {
                // stack: [(env), value] -> [(env), value, (env), value]
                dupArgs();
                if (property.getPropertyName() == null) {
                    // BindingProperty : SingleNameBinding
                    String name = BoundNames(property.getBinding()).get(0);
                    // stack: [(env), value, (env), value] -> [(env), value]
                    KeyedBindingInitialisation(property, name);
                } else {
                    // BindingProperty : PropertyName : BindingElement
                    String name = PropName(property.getPropertyName());
                    // stack: [(env), value, (env), value] -> [(env), value]
                    KeyedBindingInitialisation(property, name);
                }
            }
            // stack: [(env), value] -> []
            popArgs();

            return null;
        }

        @Override
        public Void visit(BindingIdentifier node, Void _) {
            if (environment == EnvironmentType.EnvironmentFromParameter) {
                // stack: [value] -> [value, envRec, id]
                assert false : "unused";

                mv.load(2, Types.LexicalEnvironment);
                mv.invoke(Methods.LexicalEnvironment_getEnvRec);
                mv.aconst(node.getName());

                // [value, envRec, id] -> [envRec, id, value]
                mv.dup2X1();
                mv.pop2();

                // stack: [envRec, id, value] -> []
                mv.invoke(Methods.EnvironmentRecord_initialiseBinding);
            } else if (environment == EnvironmentType.EnvironmentFromStack) {
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
                mv.invoke(Methods.Reference_PutValue);
            }

            return null;
        }
    }

    private static final class IndexedBindingInitialisation extends RuntimeSemantics<Void, Integer> {
        protected IndexedBindingInitialisation(CodeGenerator codegen, ExpressionVisitor mv,
                EnvironmentType environment) {
            super(codegen, mv, environment);
        }

        @Override
        public Void visit(FormalParameterList node, Integer index) {
            assert environment != EnvironmentType.EnvironmentFromStack;

            // stack: [value]
            for (FormalParameter formal : node) {
                // stack: [value] -> [value, value]
                mv.dup();
                // stack: [value, value] -> [value]
                formal.accept(this, index);
                index = index + 1;
            }
            // stack: [value] -> []
            mv.pop();

            return null;
        }

        @Override
        public Void visit(BindingElement node, Integer index) {
            Binding binding = node.getBinding();
            if (binding instanceof BindingIdentifier) {
                // step 1:
                // stack: [(env), value] -> []
                KeyedBindingInitialisation(node, ToString(index));
            } else {
                assert binding instanceof BindingPattern;
                Expression initialiser = node.getInitialiser();

                // step 1-3:
                // stack: [(env), value] -> [(env), v]
                String name = ToString(index);
                mv.loadExecutionContext();
                mv.swap();
                mv.aconst(name);
                mv.invoke(Methods.AbstractOperations_Get);

                // step 4-5:
                // stack: [(env), v] -> [(env), v']
                if (initialiser != null) {
                    Label undef = new Label();
                    mv.dup();
                    mv.invoke(Methods.Type_isUndefined);
                    mv.ifeq(undef);
                    {
                        mv.pop();
                        ValType type = expressionValue(initialiser, mv);
                        mv.toBoxed(type);
                        mv.loadExecutionContext();
                        mv.swap();
                        mv.invoke(Methods.AbstractOperations_ToObject);
                    }
                    mv.mark(undef);
                }

                // step 6:
                // stack: [(env), v'] -> []
                BindingInitialisation(binding);
            }
            return null;
        }

        @Override
        public Void visit(BindingRestElement node, Integer index) {
            mv.iconst(index);
            mv.loadExecutionContext();
            // stack: [(env), value, index, cx] -> [(env), rest]
            mv.invoke(Methods.ScriptRuntime_createRestArray);

            // stack: [(env), rest] -> []
            BindingInitialisation(node.getBindingIdentifier());

            return null;
        }

        @Override
        public Void visit(ArrayBindingPattern node, Integer index) {
            for (BindingElementItem element : node.getElements()) {
                if (element instanceof BindingElision) {
                    index = index + 1;
                    continue;
                }

                // stack: [(env), value] -> [(env), value, (env), value]
                dupArgs();
                // stack: [(env), value, (env), value] -> [(env), value]
                element.accept(this, index);
                index = index + 1;
            }
            // stack: [(env), value] -> []
            popArgs();
            return null;
        }
    }

    private static final class KeyedBindingInitialisation extends RuntimeSemantics<Void, String> {
        protected KeyedBindingInitialisation(CodeGenerator codegen, ExpressionVisitor mv,
                EnvironmentType environment) {
            super(codegen, mv, environment);
        }

        @Override
        public Void visit(BindingElement node, String key) {
            generate(node.getBinding(), node.getInitialiser(), key);
            return null;
        }

        @Override
        public Void visit(BindingProperty node, String key) {
            generate(node.getBinding(), node.getInitialiser(), key);
            return null;
        }

        private void generate(Binding binding, Expression initialiser, String propertyName) {
            // step 1-2:
            // stack: [(env), value] -> [(env), v]
            mv.loadExecutionContext();
            mv.swap();
            mv.aconst(propertyName);
            mv.invoke(Methods.AbstractOperations_Get);

            // step 3-4:
            // stack: [(env), value] -> [(env), v']
            if (initialiser != null) {
                Label undef = new Label();
                mv.dup();
                mv.invoke(Methods.Type_isUndefined);
                mv.ifeq(undef);
                {
                    mv.pop();
                    ValType type = expressionValue(initialiser, mv);
                    mv.toBoxed(type);
                    if (binding instanceof BindingPattern) {
                        mv.loadExecutionContext();
                        mv.swap();
                        mv.invoke(Methods.AbstractOperations_ToObject);
                    }
                }
                mv.mark(undef);
            }
            // FIXME: spec bug missing ToObject call
            if (binding instanceof BindingPattern) {
                mv.loadExecutionContext();
                mv.swap();
                mv.invoke(Methods.AbstractOperations_ToObject);
            }

            // step 5:
            // stack: [(env), v'] -> []
            BindingInitialisation(binding);
        }
    }
}
