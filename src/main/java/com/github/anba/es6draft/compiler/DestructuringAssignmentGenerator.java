/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static com.github.anba.es6draft.runtime.AbstractOperations.ToString;
import static com.github.anba.es6draft.semantics.StaticSemantics.PropName;

import org.objectweb.asm.Label;

import com.github.anba.es6draft.ast.*;
import com.github.anba.es6draft.compiler.DefaultCodeGenerator.ValType;
import com.github.anba.es6draft.compiler.MethodGenerator.Register;

/**
 * <h1>11 Expressions</h1><br>
 * <h2>11.13 Assignment Operators</h2>
 * <ul>
 * <li>11.13.1 Destructuring Assignment (Runtime Semantics)
 * </ul>
 */
class DestructuringAssignmentGenerator {
    private final CodeGenerator codegen;

    DestructuringAssignmentGenerator(CodeGenerator codegen) {
        this.codegen = codegen;
    }

    public void generate(AssignmentPattern node, MethodGenerator mv) {
        DestructuringAssignmentEvaluation init = new DestructuringAssignmentEvaluation(codegen, mv);
        node.accept(init, null);
    }

    private static void PutValue(Expression node, ValType type, MethodGenerator mv) {
        assert !node.accept(IsReference.INSTANCE, null)
                || (type == ValType.Any || type == ValType.Reference) : type;
        assert !(type == ValType.Reference) || node.accept(IsReference.INSTANCE, null) : type;

        if (node.accept(IsReference.INSTANCE, null)) {
            if (type == ValType.Reference) {
                mv.load(Register.Realm);
                mv.invokevirtual(Methods.Reference_PutValue_);
            } else {
                mv.load(Register.Realm);
                mv.invokestatic(Methods.Reference_PutValue);
            }
        }
    }

    private abstract static class RuntimeSemantics<R, V> extends DefaultNodeVisitor<R, V> {
        protected final CodeGenerator codegen;
        protected final MethodGenerator mv;

        protected RuntimeSemantics(CodeGenerator codegen, MethodGenerator mv) {
            this.codegen = codegen;
            this.mv = mv;
        }

        protected final void DestructuringAssignmentEvaluation(Node node) {
            DestructuringAssignmentEvaluation init = new DestructuringAssignmentEvaluation(codegen,
                    mv);
            node.accept(init, null);
        }

        protected final void IndexedDestructuringAssignmentEvaluation(Node node, int index) {
            IndexedDestructuringAssignmentEvaluation init = new IndexedDestructuringAssignmentEvaluation(
                    codegen, mv);
            node.accept(init, index);
        }

        protected final void KeyedDestructuringAssignmentEvaluation(Node node, String key) {
            KeyedDestructuringAssignmentEvaluation init = new KeyedDestructuringAssignmentEvaluation(
                    codegen, mv);
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
    }

    private static final class DestructuringAssignmentEvaluation extends
            RuntimeSemantics<Void, Void> {
        protected DestructuringAssignmentEvaluation(CodeGenerator codegen, MethodGenerator mv) {
            super(codegen, mv);
        }

        @Override
        public Void visit(ArrayAssignmentPattern node, Void value) {
            IndexedDestructuringAssignmentEvaluation(node, 0);
            return null;
        }

        @Override
        public Void visit(ObjectAssignmentPattern node, Void value) {
            for (AssignmentProperty property : node.getProperties()) {
                // stack: [obj] -> [obj, obj]
                mv.dup();
                if (property.getPropertyName() == null) {
                    // AssignmentProperty : Identifier Initialiser{opt}
                    assert property.getTarget() instanceof Identifier;
                    String name = ((Identifier) property.getTarget()).getName();
                    // stack: [obj, obj] -> [obj]
                    KeyedDestructuringAssignmentEvaluation(property, name);
                } else {
                    // AssignmentProperty : PropertyName : AssignmentElement
                    String name = PropName(property.getPropertyName());
                    // stack: [obj, obj] -> [obj]
                    KeyedDestructuringAssignmentEvaluation(property, name);
                }
            }
            // stack: [obj] -> []
            mv.pop();

            return null;
        }
    }

    private static final class IndexedDestructuringAssignmentEvaluation extends
            RuntimeSemantics<Void, Integer> {
        protected IndexedDestructuringAssignmentEvaluation(CodeGenerator codegen, MethodGenerator mv) {
            super(codegen, mv);
        }

        @Override
        public Void visit(ArrayAssignmentPattern node, Integer index) {
            for (AssignmentElementItem element : node.getElements()) {
                if (element instanceof Elision) {
                    index = index + 1;
                    continue;
                }

                // stack: [obj] -> [obj, obj]
                mv.dup();
                // stack: [obj, obj] -> [obj]
                element.accept(this, index);
                index = index + 1;
            }
            // stack: [obj] -> []
            mv.pop();

            return null;
        }

        @Override
        public Void visit(AssignmentElement node, Integer index) {
            String name = ToString(index);
            KeyedDestructuringAssignmentEvaluation(node, name);

            return null;
        }

        @Override
        public Void visit(AssignmentRestElement node, Integer index) {
            // stack: [obj] -> [lref, obj]
            ValType valType = codegen.expression(node.getTarget(), mv);
            assert !valType.isPrimitive() : "lhs is primitive";
            mv.swap();

            mv.iconst(index);
            mv.load(Register.Realm);
            // stack: [lref, obj, index, cx] -> [lref, rest]
            mv.invokestatic(Methods.ScriptRuntime_createRestArray);

            // stack: [lref, rest] -> []
            PutValue(node.getTarget(), valType, mv);

            return null;
        }
    }

    private static final class KeyedDestructuringAssignmentEvaluation extends
            RuntimeSemantics<Void, String> {
        protected KeyedDestructuringAssignmentEvaluation(CodeGenerator codegen, MethodGenerator mv) {
            super(codegen, mv);
        }

        @Override
        public Void visit(AssignmentElement node, String propertyName) {
            generate(node.getTarget(), node.getInitialiser(), propertyName);
            return null;
        }

        @Override
        public Void visit(AssignmentProperty node, String propertyName) {
            generate(node.getTarget(), node.getInitialiser(), propertyName);
            return null;
        }

        private void generate(LeftHandSideExpression target, Expression initialiser,
                String propertyName) {
            // step 1-2:
            // stack: [obj] -> [v]
            mv.aconst(propertyName);
            mv.invokestatic(Methods.AbstractOperations_Get);

            // step 3:
            // stack: [v] -> [v']
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
                }
                mv.mark(undef);
            }

            // step 4:
            if (target instanceof AssignmentPattern) {
                // stack: [v'] -> [vObj]
                mv.load(Register.Realm);
                mv.swap();
                mv.invokestatic(Methods.AbstractOperations_ToObject);

                // stack: [vObj] -> []
                DestructuringAssignmentEvaluation(target);
            } else {
                // stack: [v'] -> [lref, 'v]
                ValType refType = codegen.expression(target, mv);
                assert !refType.isPrimitive() : "lhs is primitive";
                mv.swap();

                // stack: [lref, 'v] -> []
                PutValue(target, refType, mv);
            }
        }
    }
}
