/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static com.github.anba.es6draft.compiler.DefaultCodeGenerator.SetFunctionName;
import static com.github.anba.es6draft.compiler.DefaultCodeGenerator.ToPropertyKey;
import static com.github.anba.es6draft.semantics.StaticSemantics.IsAnonymousFunctionDefinition;
import static com.github.anba.es6draft.semantics.StaticSemantics.IsIdentifierRef;
import static com.github.anba.es6draft.semantics.StaticSemantics.PropName;

import java.util.HashSet;
import java.util.List;

import com.github.anba.es6draft.ast.*;
import com.github.anba.es6draft.ast.scope.Name;
import com.github.anba.es6draft.compiler.DefaultCodeGenerator.ValType;
import com.github.anba.es6draft.compiler.Labels.TempLabel;
import com.github.anba.es6draft.compiler.StatementGenerator.Completion;
import com.github.anba.es6draft.compiler.assembler.FieldName;
import com.github.anba.es6draft.compiler.assembler.Jump;
import com.github.anba.es6draft.compiler.assembler.MethodName;
import com.github.anba.es6draft.compiler.assembler.MutableValue;
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
    private static final class Fields {
        static final FieldName Collections_EMPTY_SET = FieldName.findStatic(Types.Collections, "EMPTY_SET", Types.Set);
    }

    private static final class Methods {
        // class: AbstractOperations
        static final MethodName AbstractOperations_GetV = MethodName.findStatic(Types.AbstractOperations, "GetV",
                Type.methodType(Types.Object, Types.ExecutionContext, Types.Object, Types.Object));

        static final MethodName AbstractOperations_GetV_String = MethodName.findStatic(Types.AbstractOperations, "GetV",
                Type.methodType(Types.Object, Types.ExecutionContext, Types.Object, Types.String));

        static final MethodName AbstractOperations_RequireObjectCoercible = MethodName.findStatic(
                Types.AbstractOperations, "RequireObjectCoercible",
                Type.methodType(Types.Object, Types.ExecutionContext, Types.Object));

        // class: ScriptRuntime
        static final MethodName ScriptRuntime_createRestArray = MethodName.findStatic(Types.ScriptRuntime,
                "createRestArray", Type.methodType(Types.ArrayObject, Types.Iterator, Types.ExecutionContext));

        static final MethodName ScriptRuntime_createRestObject = MethodName.findStatic(Types.ScriptRuntime,
                "createRestObject",
                Type.methodType(Types.OrdinaryObject, Types.Object, Types.Set, Types.ExecutionContext));

        static final MethodName ScriptRuntime_iterate = MethodName.findStatic(Types.ScriptRuntime, "iterate",
                Type.methodType(Types.ScriptIterator, Types.Object, Types.ExecutionContext));

        static final MethodName ScriptRuntime_iteratorNextAndIgnore = MethodName.findStatic(Types.ScriptRuntime,
                "iteratorNextAndIgnore", Type.methodType(Type.VOID_TYPE, Types.Iterator));

        static final MethodName ScriptRuntime_iteratorNextOrUndefined = MethodName.findStatic(Types.ScriptRuntime,
                "iteratorNextOrUndefined", Type.methodType(Types.Object, Types.Iterator));

        // class: HashSet
        static final MethodName HashSet_init = MethodName.findConstructor(Types.HashSet,
                Type.methodType(Type.VOID_TYPE));

        static final MethodName HashSet_add = MethodName.findVirtual(Types.HashSet, "add",
                Type.methodType(Type.BOOLEAN_TYPE, Types.Object));
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
     *            the code visitor
     */
    static void DestructuringAssignment(CodeGenerator codegen, AssignmentPattern node, CodeVisitor mv) {
        DestructuringAssignmentEvaluation init = new DestructuringAssignmentEvaluation(codegen, mv);
        node.accept(init, null);
    }

    private static abstract class RuntimeSemantics<V> extends DefaultVoidNodeVisitor<V> {
        protected final CodeGenerator codegen;
        protected final CodeVisitor mv;

        RuntimeSemantics(CodeGenerator codegen, CodeVisitor mv) {
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

        protected final void KeyedDestructuringAssignmentEvaluation(AssignmentProperty node, String key,
                Variable<Object> value, Variable<HashSet<?>> propertyNames) {
            node.accept(new LiteralKeyedDestructuringAssignmentEvaluation(codegen, mv, value, propertyNames), key);
        }

        protected final void KeyedDestructuringAssignmentEvaluation(AssignmentProperty node, ComputedPropertyName key,
                Variable<Object> value, Variable<HashSet<?>> propertyNames) {
            node.accept(new ComputedKeyedDestructuringAssignmentEvaluation(codegen, mv, value, propertyNames), key);
        }

        protected final ValType expression(Expression node, CodeVisitor mv) {
            return codegen.expression(node, mv);
        }

        protected final ValType expressionBoxed(Expression node, CodeVisitor mv) {
            return codegen.expressionBoxed(node, mv);
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
        DestructuringAssignmentEvaluation(CodeGenerator codegen, CodeVisitor mv) {
            super(codegen, mv);
        }

        @Override
        public void visit(ArrayAssignmentPattern node, Void value) {
            // stack: [value] -> []
            mv.enterVariableScope();
            Variable<ScriptIterator<?>> iterator = mv.newVariable("iterator", ScriptIterator.class)
                    .uncheckedCast();
            mv.loadExecutionContext();
            mv.lineInfo(node);
            mv.invoke(Methods.ScriptRuntime_iterate);
            mv.store(iterator);

            new IterationGenerator<ArrayAssignmentPattern, CodeVisitor>(codegen) {
                @Override
                protected Completion iterationBody(ArrayAssignmentPattern node, Variable<ScriptIterator<?>> iterator,
                        CodeVisitor mv) {
                    for (AssignmentElementItem element : node.getElements()) {
                        IteratorDestructuringAssignmentEvaluation(element, iterator);
                    }
                    return Completion.Normal;
                }

                @Override
                protected void epilogue(ArrayAssignmentPattern node, Variable<ScriptIterator<?>> iterator,
                        CodeVisitor mv) {
                    IteratorClose(node, iterator, mv);
                }

                @Override
                protected MutableValue<Object> enterIteration(ArrayAssignmentPattern node, CodeVisitor mv) {
                    return mv.enterIteration();
                }

                @Override
                protected List<TempLabel> exitIteration(ArrayAssignmentPattern node, CodeVisitor mv) {
                    return mv.exitIteration();
                }
            }.generate(node, iterator, mv);

            mv.exitVariableScope();
        }

        @Override
        public void visit(ObjectAssignmentPattern node, Void value) {
            // stack: [value] -> [value]
            mv.loadExecutionContext();
            mv.swap();
            mv.lineInfo(node);
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

            Variable<HashSet<?>> propertyNames = null;
            if (!node.getProperties().isEmpty() && node.getRest() != null) {
                propertyNames = mv.newVariable("propertyNames", HashSet.class).uncheckedCast();
                mv.anew(Types.HashSet, Methods.HashSet_init);
                mv.store(propertyNames);
            }

            // ObjectAssignmentPattern : { AssignmentPropertyList }
            for (AssignmentProperty property : node.getProperties()) {
                if (property.getPropertyName() == null) {
                    // AssignmentProperty : IdentifierReference Initializer{opt}
                    assert property.getTarget() instanceof IdentifierReference;
                    String name = ((IdentifierReference) property.getTarget()).getName();
                    KeyedDestructuringAssignmentEvaluation(property, name, val, propertyNames);
                } else {
                    // AssignmentProperty : PropertyName : AssignmentElement
                    String name = PropName(property.getPropertyName());
                    if (name != null) {
                        KeyedDestructuringAssignmentEvaluation(property, name, val, propertyNames);
                    } else {
                        PropertyName propertyName = property.getPropertyName();
                        assert propertyName instanceof ComputedPropertyName;
                        KeyedDestructuringAssignmentEvaluation(property, (ComputedPropertyName) propertyName, val,
                                propertyNames);
                    }
                }
            }

            // ObjectAssignmentPattern : { ... DestructuringAssignmentTarget }
            // ObjectAssignmentPattern: { AssignmentPropertyList , ... DestructuringAssignmentTarget }
            AssignmentRestProperty rest = node.getRest();
            if (rest != null) {
                LeftHandSideExpression target = rest.getTarget();
                ReferenceOp<LeftHandSideExpression> op = null;

                /* steps 1, 1-2 (not applicable) */
                /* step 2, 3 */
                ValType refType = null;
                if (!(target instanceof AssignmentPattern)) {
                    // stack: [] -> [lref]
                    op = ReferenceOp.of(target);
                    refType = op.reference(target, mv, codegen);
                }

                /* steps 3-5, 4-6 */
                // stack: [lref?] -> [lref?, restObj]
                mv.load(val);
                if (propertyNames != null) {
                    mv.load(propertyNames);
                } else {
                    mv.get(Fields.Collections_EMPTY_SET);
                }
                mv.loadExecutionContext();
                mv.lineInfo(rest);
                mv.invoke(Methods.ScriptRuntime_createRestObject);

                // Exit the variable scope early to avoid restoring the value/propertyNames slots after a yield.
                mv.exitVariableScope();

                /* steps 6-8, 7-9 */
                if (!(target instanceof AssignmentPattern)) {
                    /* step 6, 7 */
                    // stack: [lref, restObj] -> []
                    op.putValue(target, refType, ValType.Object, mv);
                } else {
                    /* steps 7-8, 8-9 */
                    // stack: [restObj] -> []
                    DestructuringAssignmentEvaluation((AssignmentPattern) target);
                }
            } else {
                mv.exitVariableScope();
            }
        }
    }

    /**
     * 12.14.5.3 Runtime Semantics: IteratorDestructuringAssignmentEvaluation
     */
    private static final class IteratorDestructuringAssignmentEvaluation extends
            RuntimeSemantics<Variable<ScriptIterator<?>>> {
        IteratorDestructuringAssignmentEvaluation(CodeGenerator codegen, CodeVisitor mv) {
            super(codegen, mv);
        }

        @Override
        public void visit(Elision node, Variable<ScriptIterator<?>> iterator) {
            // stack: [] -> []
            mv.load(iterator);
            mv.lineInfo(node);
            mv.invoke(Methods.ScriptRuntime_iteratorNextAndIgnore);
        }

        @Override
        public void visit(AssignmentElement node, Variable<ScriptIterator<?>> iterator) {
            LeftHandSideExpression target = node.getTarget();
            Expression initializer = node.getInitializer();
            ReferenceOp<LeftHandSideExpression> op = null;

            /* step 1 */
            ValType refType = null;
            if (!(target instanceof AssignmentPattern)) {
                // stack: [] -> [lref]
                op = ReferenceOp.of(target);
                refType = op.reference(target, mv, codegen);
            }

            /* steps 2-3 */
            // stack: [(lref)] -> [(lref), v]
            mv.load(iterator);
            mv.lineInfo(node);
            mv.invoke(Methods.ScriptRuntime_iteratorNextOrUndefined);

            /* steps 4-5 */
            // stack: [(lref), v] -> [(lref), v']
            if (initializer != null) {
                Jump undef = new Jump();
                mv.dup();
                mv.loadUndefined();
                mv.ifacmpne(undef);
                {
                    mv.pop();
                    expressionBoxed(initializer, mv);
                    /* step 7 (moved) */
                    if (IsAnonymousFunctionDefinition(initializer) && IsIdentifierRef(target)) {
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
                op.putValue(target, refType, ValType.Any, mv);
            }
        }

        @Override
        public void visit(AssignmentRestElement node, Variable<ScriptIterator<?>> iterator) {
            LeftHandSideExpression target = node.getTarget();
            ReferenceOp<LeftHandSideExpression> op = null;

            /* step 1 */
            ValType refType = null;
            if (!(target instanceof AssignmentPattern)) {
                // stack: [] -> [lref]
                op = ReferenceOp.of(target);
                refType = op.reference(target, mv, codegen);
            }

            /* steps 2-4 */
            // stack: [(lref)] -> [(lref), rest]
            mv.load(iterator);
            mv.loadExecutionContext();
            mv.lineInfo(node);
            mv.invoke(Methods.ScriptRuntime_createRestArray);

            /* steps 5-7 */
            if (!(target instanceof AssignmentPattern)) {
                // stack: [lref, rest] -> []
                op.putValue(target, refType, ValType.Object, mv);
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

        KeyedDestructuringAssignmentEvaluation(CodeGenerator codegen, CodeVisitor mv, Variable<Object> value) {
            super(codegen, mv);
            this.value = value;
        }

        abstract ValType evaluatePropertyName(PROPERTYNAME propertyName);

        abstract boolean isSimplePropertyName(PROPERTYNAME propertyName);

        final boolean isSimplePropertyNameOrTarget(LeftHandSideExpression target,
                PROPERTYNAME propertyName) {
            if (isSimplePropertyName(propertyName)) {
                return true;
            }
            if (target instanceof IdentifierReference) {
                Name resolvedName = ((IdentifierReference) target).getResolvedName();
                return resolvedName != null && resolvedName.isLocal();
            }
            return false;
        }

        @Override
        public void visit(AssignmentProperty node, PROPERTYNAME propertyName) {
            LeftHandSideExpression target = node.getTarget();
            Expression initializer = node.getInitializer();
            ReferenceOp<LeftHandSideExpression> op;

            ValType type, refType;
            if (target instanceof AssignmentPattern) {
                /* step 1 (not applicable) */
                op = null;
                refType = null;

                // stack: [] -> [cx, value]
                mv.loadExecutionContext();
                mv.load(value);

                // stack: [cx, value] -> [cx, value, propertyName]
                type = evaluatePropertyName(propertyName);
            } else if (isSimplePropertyNameOrTarget(target, propertyName)) {
                /* step 1 */
                // stack: [] -> [lref]
                op = ReferenceOp.of(target);
                refType = op.reference(target, mv, codegen);

                // stack: [lref] -> [lref, cx, value]
                mv.loadExecutionContext();
                mv.load(value);

                // stack: [lref, cx, value] -> [lref, cx, value, propertyName]
                type = evaluatePropertyName(propertyName);
            } else {
                // stack: [] -> []
                type = evaluatePropertyName(propertyName);
                Variable<?> propertyNameVar = mv.newScratchVariable(type.toClass());
                mv.store(propertyNameVar);

                /* step 1 */
                // stack: [] -> [lref]
                op = ReferenceOp.of(target);
                refType = op.reference(target, mv, codegen);

                // stack: [lref] -> [lref, cx, value, propertyName]
                mv.loadExecutionContext();
                mv.load(value);
                mv.load(propertyNameVar);
                mv.freeVariable(propertyNameVar);
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
                mv.loadUndefined();
                mv.ifacmpne(undef);
                {
                    mv.pop();
                    expressionBoxed(initializer, mv);
                    /* step 7 (moved) */
                    if (IsAnonymousFunctionDefinition(initializer) && IsIdentifierRef(target)) {
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
                op.putValue(target, refType, ValType.Any, mv);
            }
        }
    }

    /**
     * 12.14.5.4 Runtime Semantics: KeyedDestructuringAssignmentEvaluation
     */
    private static final class LiteralKeyedDestructuringAssignmentEvaluation extends
            KeyedDestructuringAssignmentEvaluation<String> {
        private final Variable<HashSet<?>> propertyNames;

        LiteralKeyedDestructuringAssignmentEvaluation(CodeGenerator codegen, CodeVisitor mv, Variable<Object> value,
                Variable<HashSet<?>> propertyNames) {
            super(codegen, mv, value);
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
        private final Variable<HashSet<?>> propertyNames;

        ComputedKeyedDestructuringAssignmentEvaluation(CodeGenerator codegen, CodeVisitor mv, Variable<Object> value,
                Variable<HashSet<?>> propertyNames) {
            super(codegen, mv, value);
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
            ValType keyType = ToPropertyKey(propType, mv);
            if (propertyNames != null) {
                mv.dupX1();
                mv.invoke(Methods.HashSet_add);
                mv.pop();
            }
            return keyType;
        }

        @Override
        boolean isSimplePropertyName(ComputedPropertyName propertyName) {
            return propertyName.getExpression() instanceof Literal;
        }
    }
}
