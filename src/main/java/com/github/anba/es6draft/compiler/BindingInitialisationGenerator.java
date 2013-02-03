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

import com.github.anba.es6draft.ast.*;
import com.github.anba.es6draft.compiler.DefaultCodeGenerator.ValType;
import com.github.anba.es6draft.compiler.MethodGenerator.Register;

/**
 * <h1>13 Functions and Generators</h1><br>
 * <h2>13.1 Function Definitions</h2>
 * <ul>
 * <li>Runtime Semantics: Binding Initialisation
 * </ul>
 */
class BindingInitialisationGenerator {
    private final CodeGenerator codegen;

    BindingInitialisationGenerator(CodeGenerator codegen) {
        this.codegen = codegen;
    }

    public void generate(FunctionNode node, MethodGenerator mv) {
        BindingInitialisation init = new BindingInitialisation(codegen, mv,
                EnvironmentType.NoEnvironment);

        node.getParameters().accept(init, null);
    }

    public void generate(Binding node, MethodGenerator mv) {
        BindingInitialisation init = new BindingInitialisation(codegen, mv,
                EnvironmentType.NoEnvironment);
        node.accept(init, null);
    }

    public void generateWithEnvironment(Binding node, MethodGenerator mv) {
        BindingInitialisation init = new BindingInitialisation(codegen, mv,
                EnvironmentType.EnvironmentFromStack);
        node.accept(init, null);
    }

    private enum EnvironmentType {
        NoEnvironment, EnvironmentFromStack, EnvironmentFromParameter
    }

    private abstract static class RuntimeSemantics<R, V> extends DefaultNodeVisitor<R, V> {
        protected final CodeGenerator codegen;
        protected final MethodGenerator mv;
        protected final EnvironmentType environment;

        protected RuntimeSemantics(CodeGenerator codegen, MethodGenerator mv,
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

        /**
         * Calls <code>GetValue(o)</code> if the expression could possibly be a reference
         */
        protected final void invokeGetValue(Expression node, MethodGenerator mv) {
            if (node.accept(IsReference.INSTANCE, null)) {
                mv.load(Register.Realm);
                mv.invokestatic(Methods.Reference_GetValue);
            }
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
        protected BindingInitialisation(CodeGenerator codegen, MethodGenerator mv,
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
            mv.instanceOf(Types.Scriptable);
            mv.ifne(assertion);
            mv.aconst("Type(value) is Object");
            mv.invokestatic(Methods.ScriptRuntime_throw);
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
            mv.instanceOf(Types.Scriptable);
            mv.ifne(assertion);
            mv.aconst("Type(value) is Object");
            mv.invokestatic(Methods.ScriptRuntime_throw);
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
                mv.invokevirtual(Methods.LexicalEnvironment_getEnvRec);
                mv.aconst(node.getName());

                // [value, envRec, id] -> [envRec, id, value]
                mv.dup2X1();
                mv.pop2();

                // stack: [envRec, id, value] -> []
                mv.invokeinterface(Methods.EnvironmentRecord_initializeBinding);
            } else if (environment == EnvironmentType.EnvironmentFromStack) {
                // stack: [env, value] -> [value, envRec, id]
                mv.swap();
                mv.invokevirtual(Methods.LexicalEnvironment_getEnvRec);
                mv.aconst(node.getName());

                // [value, envRec, id] -> [envRec, id, value]
                mv.dup2X1();
                mv.pop2();

                // stack: [envRec, id, value] -> []
                mv.invokeinterface(Methods.EnvironmentRecord_initializeBinding);
            } else {
                assert environment == EnvironmentType.NoEnvironment;
                // stack: [value] -> [ref, value]
                mv.load(Register.ExecutionContext);
                mv.aconst(node.getName());
                if (mv.isStrict()) {
                    mv.invokevirtual(Methods.ExecutionContext_strictIdentifierResolution);
                } else {
                    mv.invokevirtual(Methods.ExecutionContext_nonstrictIdentifierResolution);
                }
                mv.swap();
                // stack: [ref, value] -> []
                mv.load(Register.Realm);
                mv.invokevirtual(Methods.Reference_PutValue_);
            }

            return null;
        }
    }

    private static final class IndexedBindingInitialisation extends RuntimeSemantics<Void, Integer> {
        protected IndexedBindingInitialisation(CodeGenerator codegen, MethodGenerator mv,
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
                mv.aconst(name);
                mv.invokestatic(Methods.AbstractOperations_Get);

                // step 4-5:
                // stack: [(env), v] -> [(env), v']
                if (initialiser != null) {
                    Label undef = new Label();
                    mv.dup();
                    mv.invokestatic(Methods.Type_isUndefined);
                    mv.ifeq(undef);
                    {
                        mv.pop();
                        ValType type = codegen.expression(initialiser, mv);
                        mv.toBoxed(type);
                        // FIXME: spec bug - missing GetValue() call (Bug 1242)
                        invokeGetValue(initialiser, mv);
                        mv.load(Register.Realm);
                        mv.swap();
                        mv.invokestatic(Methods.AbstractOperations_ToObject);
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
            mv.load(Register.Realm);
            // stack: [(env), value, index, cx] -> [(env), rest]
            mv.invokestatic(Methods.ScriptRuntime_createRestArray);

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
        protected KeyedBindingInitialisation(CodeGenerator codegen, MethodGenerator mv,
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
            mv.aconst(propertyName);
            mv.invokestatic(Methods.AbstractOperations_Get);

            // step 3-4:
            // stack: [(env), value] -> [(env), v']
            if (initialiser != null) {
                Label undef = new Label();
                mv.dup();
                mv.invokestatic(Methods.Type_isUndefined);
                mv.ifeq(undef);
                {
                    mv.pop();
                    ValType type = codegen.expression(initialiser, mv);
                    mv.toBoxed(type);
                    // FIXME: spec bug - missing GetValue() call (Bug 1242)
                    invokeGetValue(initialiser, mv);
                    if (binding instanceof BindingPattern) {
                        mv.load(Register.Realm);
                        mv.swap();
                        mv.invokestatic(Methods.AbstractOperations_ToObject);
                    }
                }
                mv.mark(undef);
            }

            // step 5:
            // stack: [(env), v'] -> []
            BindingInitialisation(binding);
        }
    }
}
