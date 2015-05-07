/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static com.github.anba.es6draft.compiler.DefaultCodeGenerator.SetFunctionName;
import static com.github.anba.es6draft.compiler.DefaultCodeGenerator.ToPropertyKey;
import static com.github.anba.es6draft.semantics.StaticSemantics.AssignmentPropertyNames;
import static com.github.anba.es6draft.semantics.StaticSemantics.IsAnonymousFunctionDefinition;
import static com.github.anba.es6draft.semantics.StaticSemantics.IsIdentifierRef;
import static com.github.anba.es6draft.semantics.StaticSemantics.PropName;

import java.util.List;
import java.util.Set;

import com.github.anba.es6draft.ast.*;
import com.github.anba.es6draft.compiler.DefaultCodeGenerator.ValType;
import com.github.anba.es6draft.compiler.Labels.TempLabel;
import com.github.anba.es6draft.compiler.StatementGenerator.Completion;
import com.github.anba.es6draft.compiler.assembler.Jump;
import com.github.anba.es6draft.compiler.assembler.MethodName;
import com.github.anba.es6draft.compiler.assembler.Type;
import com.github.anba.es6draft.compiler.assembler.Variable;
import com.github.anba.es6draft.runtime.internal.ScriptIterator;

/**
 * <h1>12 ECMAScript Language: Expressions</h1><br>
 * <h2>12.14 Assignment Operators</h2>
 * <ul>
 * <li>12.14.5 Destructuring Assignment
 * </ul>
 */
final class DestructuringAssignmentGenerator {
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

        // class: Reference
        static final MethodName Reference_putValue = MethodName.findVirtual(Types.Reference,
                "putValue", Type.methodType(Type.VOID_TYPE, Types.Object, Types.ExecutionContext));

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

        // class: Type
        static final MethodName Type_isUndefined = MethodName.findStatic(Types._Type,
                "isUndefined", Type.methodType(Type.BOOLEAN_TYPE, Types.Object));
    }

    private DestructuringAssignmentGenerator() {
    }

    /**
     * stack: [value] {@literal ->} []
     * 
     * @param codegen
     *            the code generator
     * @param node
     *            the assignment pattern node
     * @param mv
     *            the expression visitor
     */
    static void DestructuringAssignment(CodeGenerator codegen, AssignmentPattern node,
            ExpressionVisitor mv) {
        DestructuringAssignmentEvaluation init = new DestructuringAssignmentEvaluation(codegen, mv);
        node.accept(init, null);
    }

    private static void PutValue(LeftHandSideExpression node, ValType type, ExpressionVisitor mv) {
        assert type == ValType.Reference : "lhs is not reference: " + type;
        mv.lineInfo(node);
        mv.loadExecutionContext();
        mv.invoke(Methods.Reference_putValue);
    }

    private static abstract class RuntimeSemantics<V> extends DefaultVoidNodeVisitor<V> {
        protected final CodeGenerator codegen;
        protected final ExpressionVisitor mv;

        RuntimeSemantics(CodeGenerator codegen, ExpressionVisitor mv) {
            this.codegen = codegen;
            this.mv = mv;
        }

        protected final void DestructuringAssignmentEvaluation(AssignmentPattern node) {
            node.accept(new DestructuringAssignmentEvaluation(codegen, mv), null);
        }

        protected final void IteratorDestructuringAssignmentEvaluation(AssignmentElementItem node,
                Variable<ScriptIterator<?>> iterator) {
            node.accept(new IteratorDestructuringAssignmentEvaluation(codegen, mv), iterator);
        }

        protected final void KeyedDestructuringAssignmentEvaluation(AssignmentProperty node,
                String key, Variable<Object> value) {
            node.accept(new LiteralKeyedDestructuringAssignmentEvaluation(codegen, mv, value), key);
        }

        protected final void KeyedDestructuringAssignmentEvaluation(AssignmentProperty node,
                ComputedPropertyName key, Variable<Object> value) {
            node.accept(new ComputedKeyedDestructuringAssignmentEvaluation(codegen, mv, value), key);
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
        protected final void visit(Node node, V value) {
            throw new IllegalStateException();
        }
    }

    /**
     * 12.14.5.2 Runtime Semantics: DestructuringAssignmentEvaluation
     */
    private static final class DestructuringAssignmentEvaluation extends RuntimeSemantics<Void> {
        DestructuringAssignmentEvaluation(CodeGenerator codegen, ExpressionVisitor mv) {
            super(codegen, mv);
        }

        @Override
        public void visit(ArrayAssignmentPattern node, Void value) {
            // stack: [value] -> []
            mv.enterVariableScope();
            Variable<ScriptIterator<?>> iterator = mv.newVariable("iterator", ScriptIterator.class)
                    .uncheckedCast();
            mv.lineInfo(node);
            mv.loadExecutionContext();
            mv.invoke(Methods.ScriptRuntime_iterate);
            mv.store(iterator);

            new IterationGenerator<ArrayAssignmentPattern, ExpressionVisitor>(codegen) {
                @Override
                protected Completion iterationBody(ArrayAssignmentPattern node,
                        Variable<ScriptIterator<?>> iterator, ExpressionVisitor mv) {
                    for (AssignmentElementItem element : node.getElements()) {
                        IteratorDestructuringAssignmentEvaluation(element, iterator);
                    }
                    return Completion.Normal;
                }

                @Override
                protected void epilogue(ArrayAssignmentPattern node,
                        Variable<ScriptIterator<?>> iterator, ExpressionVisitor mv) {
                    IteratorClose(node, iterator, false, mv);
                }

                @Override
                protected Variable<Object> enterIteration(ArrayAssignmentPattern node,
                        ExpressionVisitor mv) {
                    return mv.enterIteration();
                }

                @Override
                protected List<TempLabel> exitIteration(ArrayAssignmentPattern node,
                        ExpressionVisitor mv) {
                    return mv.exitIteration();
                }
            }.generate(node, iterator, mv);

            mv.exitVariableScope();
        }

        @Override
        public void visit(ObjectAssignmentPattern node, Void value) {
            // stack: [value] -> [value]
            mv.lineInfo(node);
            mv.loadExecutionContext();
            mv.swap();
            mv.invoke(Methods.AbstractOperations_RequireObjectCoercible);

            // ObjectAssignmentPattern : { }
            if (node.getProperties().isEmpty() && node.getRest() == null) {
                // stack: [value] -> []
                mv.pop();
                return;
            }

            // stack: [value] -> []
            mv.enterVariableScope();
            Variable<Object> val = mv.newVariable("value", Object.class);
            mv.store(val);

            // ObjectAssignmentPattern : { AssignmentPropertyList }
            for (AssignmentProperty property : node.getProperties()) {
                if (property.getPropertyName() == null) {
                    // AssignmentProperty : IdentifierReference Initializer{opt}
                    assert property.getTarget() instanceof IdentifierReference;
                    String name = ((IdentifierReference) property.getTarget()).getName();
                    KeyedDestructuringAssignmentEvaluation(property, name, val);
                } else {
                    // AssignmentProperty : PropertyName : AssignmentElement
                    String name = PropName(property.getPropertyName());
                    if (name != null) {
                        KeyedDestructuringAssignmentEvaluation(property, name, val);
                    } else {
                        PropertyName propertyName = property.getPropertyName();
                        assert propertyName instanceof ComputedPropertyName;
                        KeyedDestructuringAssignmentEvaluation(property,
                                (ComputedPropertyName) propertyName, val);
                    }
                }
            }

            // ObjectAssignmentPattern : { ... IdentifierReference }
            // ObjectAssignmentPattern: { AssignmentPropertyList , ... IdentifierReference }
            AssignmentRestProperty rest = node.getRest();
            if (rest != null) {
                // stack: [] -> [lref]
                ValType refType = expression(rest.getTarget(), mv);

                // stack: [] -> [lref, restObj]
                mv.lineInfo(rest);
                mv.load(val);
                newStringArray(mv, AssignmentPropertyNames(node));
                mv.loadExecutionContext();
                mv.invoke(Methods.ScriptRuntime_createRestObject);

                // stack: [lref, restObj] -> []
                PutValue(rest.getTarget(), refType, mv);
            }

            mv.exitVariableScope();
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
     * 12.14.5.3 Runtime Semantics: IteratorDestructuringAssignmentEvaluation
     */
    private static final class IteratorDestructuringAssignmentEvaluation extends
            RuntimeSemantics<Variable<ScriptIterator<?>>> {
        IteratorDestructuringAssignmentEvaluation(CodeGenerator codegen, ExpressionVisitor mv) {
            super(codegen, mv);
        }

        @Override
        public void visit(Elision node, Variable<ScriptIterator<?>> iterator) {
            // stack: [] -> []
            mv.lineInfo(node);
            mv.load(iterator);
            mv.invoke(Methods.ScriptRuntime_iteratorNextAndIgnore);
        }

        @Override
        public void visit(AssignmentElement node, Variable<ScriptIterator<?>> iterator) {
            LeftHandSideExpression target = node.getTarget();
            Expression initializer = node.getInitializer();

            /* step 1 */
            ValType refType = null;
            if (!(target instanceof AssignmentPattern)) {
                // stack: [] -> [lref]
                refType = expression(target, mv);
            }

            /* steps 2-3 */
            // stack: [(lref)] -> [(lref), v]
            mv.lineInfo(node);
            mv.load(iterator);
            mv.invoke(Methods.ScriptRuntime_iteratorNextOrUndefined);

            /* steps 4-5 */
            // stack: [(lref), v] -> [(lref), v']
            if (initializer != null) {
                Jump undef = new Jump();
                mv.dup();
                mv.invoke(Methods.Type_isUndefined);
                mv.ifeq(undef);
                {
                    mv.pop();
                    expressionBoxedValue(initializer, mv);
                    /* step 7 (moved) */
                    if (!(target instanceof AssignmentPattern)
                            && IsAnonymousFunctionDefinition(initializer)
                            && IsIdentifierRef(target)) {
                        SetFunctionName(initializer, ((IdentifierReference) target).getName(), mv);
                    }
                }
                mv.mark(undef);
            }

            /* steps 6-8 */
            if (target instanceof AssignmentPattern) {
                // stack: [v'] -> []
                DestructuringAssignmentEvaluation((AssignmentPattern) target);
            } else {
                // stack: [lref, 'v] -> []
                PutValue(target, refType, mv);
            }
        }

        @Override
        public void visit(AssignmentRestElement node, Variable<ScriptIterator<?>> iterator) {
            LeftHandSideExpression target = node.getTarget();

            /* step 1 */
            ValType refType = null;
            if (!(target instanceof AssignmentPattern)) {
                // stack: [] -> [lref]
                refType = expression(target, mv);
            }

            /* steps 2-4 */
            // stack: [(lref)] -> [(lref), rest]
            mv.lineInfo(node);
            mv.load(iterator);
            mv.loadExecutionContext();
            mv.invoke(Methods.ScriptRuntime_createRestArray);

            /* steps 5-7 */
            if (!(target instanceof AssignmentPattern)) {
                // stack: [lref, rest] -> []
                PutValue(target, refType, mv);
            } else {
                // stack: [rest] -> []
                DestructuringAssignmentEvaluation((AssignmentPattern) target);
            }
        }
    }

    /**
     * 12.14.5.4 Runtime Semantics: KeyedDestructuringAssignmentEvaluation
     */
    private static abstract class KeyedDestructuringAssignmentEvaluation<PROPERTYNAME> extends
            RuntimeSemantics<PROPERTYNAME> {
        private final Variable<Object> value;

        KeyedDestructuringAssignmentEvaluation(CodeGenerator codegen, ExpressionVisitor mv,
                Variable<Object> value) {
            super(codegen, mv);
            this.value = value;
        }

        abstract ValType evaluatePropertyName(PROPERTYNAME propertyName);

        abstract boolean isSimplePropertyName(PROPERTYNAME propertyName);

        @Override
        public void visit(AssignmentProperty node, PROPERTYNAME propertyName) {
            LeftHandSideExpression target = node.getTarget();
            Expression initializer = node.getInitializer();

            ValType type, refType;
            if (target instanceof AssignmentPattern) {
                /* step 1 (not applicable) */
                refType = null;

                // stack: [] -> [cx, value]
                mv.loadExecutionContext();
                mv.load(value);

                // stack: [cx, value] -> [cx, value, propertyName]
                type = evaluatePropertyName(propertyName);
            } else if (isSimplePropertyName(propertyName)) {
                /* step 1 */
                // stack: [] -> [lref]
                refType = expression(target, mv);

                // stack: [lref] -> [lref, cx, value]
                mv.loadExecutionContext();
                mv.load(value);

                // stack: [lref, cx, value] -> [lref, cx, value, propertyName]
                type = evaluatePropertyName(propertyName);
            } else {
                // stack: [] -> [propertyName]
                type = evaluatePropertyName(propertyName);

                /* step 1 */
                // stack: [propertyName] -> [lref, propertyName]
                refType = expression(target, mv);
                mv.swap();

                // stack: [lref, propertyName] -> [lref, cx, value, propertyName]
                mv.loadExecutionContext();
                mv.swap();
                mv.load(value);
                mv.swap();
            }

            /* steps 2-3 */
            // stack: [(lref), cx, value, propertyName] -> [(lref), v]
            mv.lineInfo(node);
            if (type == ValType.String) {
                mv.invoke(Methods.AbstractOperations_GetV_String);
            } else {
                mv.invoke(Methods.AbstractOperations_GetV);
            }

            /* steps 4-5 */
            // stack: [(lref), v] -> [(lref), v']
            if (initializer != null) {
                Jump undef = new Jump();
                mv.dup();
                mv.invoke(Methods.Type_isUndefined);
                mv.ifeq(undef);
                {
                    mv.pop();
                    expressionBoxedValue(initializer, mv);
                    /* step 7 (moved) */
                    if (!(target instanceof AssignmentPattern)
                            && IsAnonymousFunctionDefinition(initializer)
                            && IsIdentifierRef(target)) {
                        SetFunctionName(initializer, ((IdentifierReference) target).getName(), mv);
                    }
                }
                mv.mark(undef);
            }

            /* steps 6-8 */
            if (target instanceof AssignmentPattern) {
                // stack: [v'] -> []
                DestructuringAssignmentEvaluation((AssignmentPattern) target);
            } else {
                // stack: [lref, 'v] -> []
                PutValue(target, refType, mv);
            }
        }
    }

    /**
     * 12.14.5.4 Runtime Semantics: KeyedDestructuringAssignmentEvaluation
     */
    private static final class LiteralKeyedDestructuringAssignmentEvaluation extends
            KeyedDestructuringAssignmentEvaluation<String> {
        LiteralKeyedDestructuringAssignmentEvaluation(CodeGenerator codegen, ExpressionVisitor mv,
                Variable<Object> value) {
            super(codegen, mv, value);
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
     * 12.14.5.4 Runtime Semantics: KeyedDestructuringAssignmentEvaluation
     */
    private static final class ComputedKeyedDestructuringAssignmentEvaluation extends
            KeyedDestructuringAssignmentEvaluation<ComputedPropertyName> {
        ComputedKeyedDestructuringAssignmentEvaluation(CodeGenerator codegen, ExpressionVisitor mv,
                Variable<Object> value) {
            super(codegen, mv, value);
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
