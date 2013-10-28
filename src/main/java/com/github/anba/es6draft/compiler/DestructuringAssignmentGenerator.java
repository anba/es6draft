/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static com.github.anba.es6draft.compiler.DefaultCodeGenerator.ToPropertyKey;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToString;
import static com.github.anba.es6draft.semantics.StaticSemantics.PropName;

import org.objectweb.asm.Label;
import org.objectweb.asm.Type;

import com.github.anba.es6draft.ast.*;
import com.github.anba.es6draft.compiler.DefaultCodeGenerator.ValType;
import com.github.anba.es6draft.compiler.InstructionVisitor.MethodDesc;
import com.github.anba.es6draft.compiler.InstructionVisitor.MethodType;

/**
 * <h1>12 ECMAScript Language: Expressions</h1><br>
 * <h2>12.13 Assignment Operators</h2>
 * <ul>
 * <li>12.13.1 Destructuring Assignment (Runtime Semantics)
 * </ul>
 */
class DestructuringAssignmentGenerator {
    private static class Methods {
        // class: AbstractOperations
        static final MethodDesc AbstractOperations_Get = MethodDesc.create(MethodType.Static,
                Types.AbstractOperations, "Get", Type.getMethodType(Types.Object,
                        Types.ExecutionContext, Types.ScriptObject, Types.Object));

        static final MethodDesc AbstractOperations_Get_String = MethodDesc.create(
                MethodType.Static, Types.AbstractOperations, "Get", Type.getMethodType(
                        Types.Object, Types.ExecutionContext, Types.ScriptObject, Types.String));

        // class: Reference
        static final MethodDesc Reference_PutValue = MethodDesc.create(MethodType.Virtual,
                Types.Reference, "PutValue",
                Type.getMethodType(Type.VOID_TYPE, Types.Object, Types.ExecutionContext));

        // class: ScriptRuntime
        static final MethodDesc ScriptRuntime_createRestArray = MethodDesc.create(
                MethodType.Static, Types.ScriptRuntime, "createRestArray", Type.getMethodType(
                        Types.ScriptObject, Types.ScriptObject, Type.INT_TYPE,
                        Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_ensureObject = MethodDesc.create(MethodType.Static,
                Types.ScriptRuntime, "ensureObject",
                Type.getMethodType(Types.ScriptObject, Types.Object, Types.ExecutionContext));

        // class: Type
        static final MethodDesc Type_isUndefined = MethodDesc.create(MethodType.Static,
                Types._Type, "isUndefined", Type.getMethodType(Type.BOOLEAN_TYPE, Types.Object));
    }

    private final CodeGenerator codegen;

    DestructuringAssignmentGenerator(CodeGenerator codegen) {
        this.codegen = codegen;
    }

    public void generate(AssignmentPattern node, ExpressionVisitor mv) {
        DestructuringAssignmentEvaluation init = new DestructuringAssignmentEvaluation(codegen, mv);
        node.accept(init, null);
    }

    private static void PutValue(Expression node, ValType type, ExpressionVisitor mv) {
        assert type == ValType.Reference : "lhs is not reference: " + type;

        mv.loadExecutionContext();
        mv.invoke(Methods.Reference_PutValue);
    }

    private abstract static class RuntimeSemantics<R, V> extends DefaultNodeVisitor<R, V> {
        protected final CodeGenerator codegen;
        protected final ExpressionVisitor mv;

        protected RuntimeSemantics(CodeGenerator codegen, ExpressionVisitor mv) {
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

        protected final void ComputedKeyedDestructuringAssignmentEvaluation(Node node,
                ComputedPropertyName key) {
            ComputedKeyedDestructuringAssignmentEvaluation init = new ComputedKeyedDestructuringAssignmentEvaluation(
                    codegen, mv);
            node.accept(init, key);
        }

        protected final ValType expression(Expression node, ExpressionVisitor mv) {
            return codegen.expression(node, mv);
        }

        protected final ValType expressionValue(Expression node, ExpressionVisitor mv) {
            return codegen.expressionValue(node, mv);
        }

        protected final ValType expressionBoxedValue(Expression node, ExpressionVisitor mv) {
            return codegen.expressionBoxedValue(node, mv);
        }

        @Override
        protected final R visit(Node node, V value) {
            throw new IllegalStateException();
        }
    }

    private static final class DestructuringAssignmentEvaluation extends
            RuntimeSemantics<Void, Void> {
        protected DestructuringAssignmentEvaluation(CodeGenerator codegen, ExpressionVisitor mv) {
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
                    if (name != null) {
                        KeyedDestructuringAssignmentEvaluation(property, name);
                    } else {
                        PropertyName propertyName = property.getPropertyName();
                        assert propertyName instanceof ComputedPropertyName;
                        ComputedKeyedDestructuringAssignmentEvaluation(property,
                                (ComputedPropertyName) propertyName);
                    }
                }
            }
            // stack: [obj] -> []
            mv.pop();

            return null;
        }
    }

    private static final class IndexedDestructuringAssignmentEvaluation extends
            RuntimeSemantics<Void, Integer> {
        protected IndexedDestructuringAssignmentEvaluation(CodeGenerator codegen,
                ExpressionVisitor mv) {
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
            ValType refType = expression(node.getTarget(), mv);
            mv.swap();

            mv.iconst(index);
            mv.loadExecutionContext();
            // stack: [lref, obj, index, cx] -> [lref, rest]
            mv.invoke(Methods.ScriptRuntime_createRestArray);

            // stack: [lref, rest] -> []
            PutValue(node.getTarget(), refType, mv);

            return null;
        }
    }

    private static final class KeyedDestructuringAssignmentEvaluation extends
            RuntimeSemantics<Void, String> {
        protected KeyedDestructuringAssignmentEvaluation(CodeGenerator codegen, ExpressionVisitor mv) {
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
            // stack: [obj] -> [cx, obj]
            mv.loadExecutionContext();
            mv.swap();

            // stack: [cx, obj] -> [cx, obj, propertyName]
            mv.aconst(propertyName);

            // steps 1-2
            // stack: [cx, obj, propertyName] -> [v]
            mv.invoke(Methods.AbstractOperations_Get_String);

            // step 3
            // stack: [v] -> [v']
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

            // steps 4-6
            if (target instanceof AssignmentPattern) {
                // stack: [v'] -> [v']
                mv.loadExecutionContext();
                mv.invoke(Methods.ScriptRuntime_ensureObject);

                // stack: [v'] -> []
                DestructuringAssignmentEvaluation(target);
            } else {
                // stack: [v'] -> [lref, 'v]
                ValType refType = expression(target, mv);
                mv.swap();

                // stack: [lref, 'v] -> []
                PutValue(target, refType, mv);
            }
        }
    }

    private static final class ComputedKeyedDestructuringAssignmentEvaluation extends
            RuntimeSemantics<Void, ComputedPropertyName> {
        protected ComputedKeyedDestructuringAssignmentEvaluation(CodeGenerator codegen,
                ExpressionVisitor mv) {
            super(codegen, mv);
        }

        @Override
        public Void visit(AssignmentProperty node, ComputedPropertyName propertyName) {
            generate(node.getTarget(), node.getInitialiser(), propertyName);
            return null;
        }

        private void generate(LeftHandSideExpression target, Expression initialiser,
                ComputedPropertyName propertyName) {
            // stack: [obj] -> [cx, obj]
            mv.loadExecutionContext();
            mv.swap();

            // stack: [cx, obj] -> [cx, obj, propertyName]
            // Runtime Semantics: Evaluation
            // ComputedPropertyName : [ AssignmentExpression ]
            ValType propType = expressionValue(propertyName.getExpression(), mv);
            ToPropertyKey(propType, mv);

            // steps 1-2
            // stack: [cx, obj, propertyName] -> [v]
            mv.invoke(Methods.AbstractOperations_Get);

            // step 3
            // stack: [v] -> [v']
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

            // steps 4-6
            if (target instanceof AssignmentPattern) {
                // stack: [v'] -> [v']
                mv.loadExecutionContext();
                mv.invoke(Methods.ScriptRuntime_ensureObject);

                // stack: [v'] -> []
                DestructuringAssignmentEvaluation(target);
            } else {
                // stack: [v'] -> [lref, 'v]
                ValType refType = expression(target, mv);
                mv.swap();

                // stack: [lref, 'v] -> []
                PutValue(target, refType, mv);
            }
        }
    }
}
