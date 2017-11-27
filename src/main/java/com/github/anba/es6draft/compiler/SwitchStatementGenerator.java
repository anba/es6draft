/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

import com.github.anba.es6draft.ast.BinaryExpression;
import com.github.anba.es6draft.ast.Expression;
import com.github.anba.es6draft.ast.Node;
import com.github.anba.es6draft.ast.NumericLiteral;
import com.github.anba.es6draft.ast.StringLiteral;
import com.github.anba.es6draft.ast.SwitchClause;
import com.github.anba.es6draft.ast.SwitchStatement;
import com.github.anba.es6draft.ast.UnaryExpression;
import com.github.anba.es6draft.ast.scope.BlockScope;
import com.github.anba.es6draft.compiler.CodeVisitor.OutlinedCall;
import com.github.anba.es6draft.compiler.Labels.BreakLabel;
import com.github.anba.es6draft.compiler.StatementGenerator.Completion;
import com.github.anba.es6draft.compiler.assembler.Code.MethodCode;
import com.github.anba.es6draft.compiler.assembler.Jump;
import com.github.anba.es6draft.compiler.assembler.MethodName;
import com.github.anba.es6draft.compiler.assembler.MethodTypeDescriptor;
import com.github.anba.es6draft.compiler.assembler.MutableValue;
import com.github.anba.es6draft.compiler.assembler.Type;
import com.github.anba.es6draft.compiler.assembler.Variable;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.internal.Bootstrap;

/**
 * <h1>13 ECMAScript Language: Statements and Declarations</h1>
 * <ul>
 * <li>13.12 The switch Statement
 * </ul>
 */
final class SwitchStatementGenerator extends DefaultCodeGenerator<StatementGenerator.Completion> {
    private static final class Methods {
        // class: CharSequence
        static final MethodName CharSequence_charAt = MethodName.findInterface(Types.CharSequence, "charAt",
                Type.methodType(Type.CHAR_TYPE, Type.INT_TYPE));
        static final MethodName CharSequence_length = MethodName.findInterface(Types.CharSequence, "length",
                Type.methodType(Type.INT_TYPE));
        static final MethodName CharSequence_toString = MethodName.findInterface(Types.CharSequence, "toString",
                Type.methodType(Types.String));

        // class: Number
        static final MethodName Number_doubleValue = MethodName.findVirtual(Types.Number, "doubleValue",
                Type.methodType(Type.DOUBLE_TYPE));

        // class: String
        static final MethodName String_equals = MethodName.findVirtual(Types.String, "equals",
                Type.methodType(Type.BOOLEAN_TYPE, Types.Object));
        static final MethodName String_hashCode = MethodName.findVirtual(Types.String, "hashCode",
                Type.methodType(Type.INT_TYPE));
    }

    private enum SwitchType {
        Int, Char, String, Generic;

        private static boolean isIntSwitch(List<SwitchClause> clauses) {
            for (SwitchClause switchClause : clauses) {
                if (!switchClause.isDefaultClause()) {
                    Expression expr = switchClause.getExpression();
                    if (expr instanceof NumericLiteral && ((NumericLiteral) expr).isInt()) {
                        continue;
                    }
                    if (expr instanceof UnaryExpression
                            && ((UnaryExpression) expr).getOperator() == UnaryExpression.Operator.NEG
                            && ((UnaryExpression) expr).getOperand() instanceof NumericLiteral
                            && ((NumericLiteral) ((UnaryExpression) expr).getOperand()).isInt()
                            && ((NumericLiteral) ((UnaryExpression) expr).getOperand()).intValue() != 0) {
                        continue;
                    }
                    return false;
                }
            }
            return true;
        }

        private static boolean isCharSwitch(List<SwitchClause> clauses) {
            for (SwitchClause switchClause : clauses) {
                if (!switchClause.isDefaultClause()) {
                    Expression expr = switchClause.getExpression();
                    if (expr instanceof StringLiteral && ((StringLiteral) expr).getValue().length() == 1) {
                        continue;
                    }
                    return false;
                }
            }
            return true;
        }

        private static boolean isStringSwitch(List<SwitchClause> clauses) {
            for (SwitchClause switchClause : clauses) {
                if (!switchClause.isDefaultClause() && !(switchClause.getExpression() instanceof StringLiteral)) {
                    return false;
                }
            }
            return true;
        }

        static SwitchType of(SwitchStatement node) {
            return of(node.getClauses());
        }

        static SwitchType of(List<SwitchClause> clauses) {
            if (isIntSwitch(clauses)) {
                return Int;
            }
            if (isCharSwitch(clauses)) {
                return Char;
            }
            if (isStringSwitch(clauses)) {
                return String;
            }
            return Generic;
        }
    }

    private static final int SWITCH_CASE_LIMIT = 768; // sync with CodeSize

    public SwitchStatementGenerator(CodeGenerator codegen) {
        super(codegen);
    }

    @Override
    protected Completion visit(Node node, CodeVisitor mv) {
        throw new IllegalStateException(String.format("node-class: %s", node.getClass()));
    }

    /**
     * 13.12.11 Runtime Semantics: Evaluation
     */
    @Override
    public Completion visit(SwitchClause node, CodeVisitor mv) {
        return statements(node.getStatements(), mv);
    }

    /**
     * 13.12.11 Runtime Semantics: Evaluation
     */
    @Override
    public Completion visit(SwitchStatement node, CodeVisitor mv) {
        final boolean defaultClausePresent = hasDefaultClause(node);
        final SwitchType type = SwitchType.of(node);

        // stack -> switchValue
        ValType switchValueType = expression(node.getExpression(), mv);

        boolean defaultOrReturn = false;
        if (isDefaultSwitch(node)) {
            defaultOrReturn = true;
        } else if (type == SwitchType.Int) {
            if (!switchValueType.isNumber() && switchValueType != ValType.Any) {
                defaultOrReturn = true;
            }
        } else if (type == SwitchType.Char) {
            if (switchValueType != ValType.String && switchValueType != ValType.Any) {
                defaultOrReturn = true;
            }
        } else if (type == SwitchType.String) {
            if (switchValueType != ValType.String && switchValueType != ValType.Any) {
                defaultOrReturn = true;
            }
        } else {
            mv.toBoxed(switchValueType);
            switchValueType = ValType.Any;
        }

        if (defaultOrReturn) {
            // never true -> emit default switch or return
            mv.pop(switchValueType);
            if (!defaultClausePresent) {
                return Completion.Normal;
            }
        }

        mv.enterVariableScope();
        Variable<LexicalEnvironment<?>> savedEnv = saveEnvironment(node, mv);

        Variable<?> switchValue = null;
        if (!defaultOrReturn) {
            switchValue = mv.newVariable("switchValue", switchValueType.toClass());
            mv.store(switchValue);
        }

        BlockScope scope = node.getScope();
        if (scope.isPresent()) {
            newDeclarativeEnvironment(scope, mv);
            codegen.blockInit(node, mv);
            pushLexicalEnvironment(mv);
        }

        Jump lblExit = new Jump();
        BreakLabel lblBreak = new BreakLabel();
        mv.enterScope(node);
        mv.enterBreakable(node, lblBreak);
        Completion result;
        if (defaultOrReturn) {
            result = DefaultCaseBlockEvaluation(node, mv);
        } else {
            result = CaseBlockEvaluation(node, type, defaultClausePresent, lblExit, switchValue, mv);
        }
        mv.exitBreakable(node);
        mv.exitScope();

        if (!defaultClausePresent) {
            mv.mark(lblExit);
        }
        if (scope.isPresent() && !result.isAbrupt()) {
            popLexicalEnvironment(mv);
        }
        if (lblBreak.isTarget()) {
            mv.mark(lblBreak);
            restoreEnvironment(savedEnv, mv);
        }
        mv.exitVariableScope();

        return result.normal(lblBreak.isTarget() || !defaultClausePresent);
    }

    /**
     * 13.12.9 Runtime Semantics: CaseBlockEvaluation
     * 
     * @param node
     *            the switch statement
     * @param mv
     *            the code visitor
     * @return the completion value
     */
    private Completion DefaultCaseBlockEvaluation(SwitchStatement node, CodeVisitor mv) {
        // Skip leading clauses until default clause found.
        List<SwitchClause> clauses = node.getClauses();
        for (int i = 0; i < clauses.size(); ++i) {
            if (clauses.get(i).isDefaultClause()) {
                clauses = clauses.subList(i, clauses.size());
                break;
            }
        }
        assert !clauses.isEmpty() && clauses.get(0).isDefaultClause();

        Completion lastResult = Completion.Normal;
        if (clauses.size() > SWITCH_CASE_LIMIT) {
            List<CaseBlock> blocks = computeCaseBlocks(node, list -> {
                return new CaseBlock(list, null, null);
            });

            for (CaseBlock caseBlock : blocks) {
                OutlinedCall call = mv.compile(caseBlock.blockKey(), () -> defaultCaseBlock(caseBlock, mv));

                // Pessimistically assume we need to save the completion value.
                lastResult = mv.invokeCompletion(call, mv.hasCompletion());
                if (lastResult.isAbrupt()) {
                    break;
                }
            }
        } else {
            // Handle clauses following default clause until abrupt completion found.
            for (SwitchClause switchClause : clauses) {
                lastResult = switchClause.accept(this, mv);
                if (lastResult.isAbrupt()) {
                    break;
                }
            }
        }
        return lastResult;
    }

    /**
     * 13.12.9 Runtime Semantics: CaseBlockEvaluation
     * 
     * @param node
     *            the switch statement
     * @param type
     *            the switch statement type
     * @param lblExit
     *            the exit label
     * @param switchValue
     *            the variable which holds the switch value
     * @param mv
     *            the code visitor
     * @return the completion value
     */
    private Completion CaseBlockEvaluation(SwitchStatement node, SwitchType type, boolean defaultClausePresent,
            Jump lblExit, Variable<?> switchValue, CodeVisitor mv) {
        List<SwitchClause> clauses = node.getClauses();

        Completion lastResult = Completion.Normal;
        if (clauses.size() > SWITCH_CASE_LIMIT) {
            List<CaseBlock> blocks = computeCaseBlocks(node, list -> {
                int targetCounter = 1; // 0 is reserved
                int[] switchTargets = new int[list.size()];
                for (int i = 0; i < list.size(); ++i) {
                    if (list.get(i).getStatements().isEmpty()) {
                        switchTargets[i] = targetCounter;
                    } else {
                        switchTargets[i] = targetCounter++;
                    }
                }
                return new CaseBlock(list, switchTargets, new Jump());
            });

            mv.enterVariableScope();
            Variable<int[]> switchTargetRef = mv.newVariable("switchTarget", int[].class);
            mv.newarray(1, Type.INT_TYPE);
            mv.store(switchTargetRef);
            MutableValue<Integer> switchTarget = mv.iarrayElement(switchTargetRef, 0);

            for (CaseBlock caseBlock : blocks) {
                OutlinedCall call = mv.compile(caseBlock.selectKey(),
                        () -> caseSelect(caseBlock, switchValue.getType(), mv));

                mv.invokeCompletion(call, false, switchValue, switchTargetRef);
                mv.load(switchTarget);
                mv.ifne(caseBlock.entry);
            }

            if (!defaultClausePresent) {
                mv.goTo(lblExit);
            } else {
                for (CaseBlock caseBlock : blocks) {
                    int index = indexOf(caseBlock.clauses, SwitchClause::isDefaultClause);
                    if (index >= 0) {
                        mv.store(switchTarget, mv.vconst(caseBlock.switchTargets[index]));
                        mv.goTo(caseBlock.entry);
                        break;
                    }
                }
            }

            for (CaseBlock caseBlock : blocks) {
                OutlinedCall call = mv.compile(caseBlock.blockKey(), () -> caseBlock(caseBlock, mv));

                // Pessimistically assume we need to save the completion value.
                mv.mark(caseBlock.entry);
                lastResult = mv.invokeCompletion(call, mv.hasCompletion(), switchTarget);
                if (!lastResult.isAbrupt()) {
                    // Clear switchTarget on fall-thru.
                    mv.store(switchTarget, mv.vconst(0));
                }
            }
            mv.exitVariableScope();
        } else {
            Jump lblDefault = null;
            Jump[] labels = new Jump[clauses.size()];
            for (int i = 0, size = clauses.size(); i < size; ++i) {
                labels[i] = new Jump();
                if (clauses.get(i).isDefaultClause()) {
                    assert lblDefault == null;
                    lblDefault = labels[i];
                }
            }
            assert defaultClausePresent == (lblDefault != null);

            caseSelector(type).select(clauses, switchValue, labels, defaultClausePresent ? lblDefault : lblExit, mv);

            int index = 0;
            for (SwitchClause switchClause : clauses) {
                Jump caseLabel = labels[index++];
                if (caseLabel.isTarget()) {
                    mv.mark(caseLabel);
                } else if (lastResult.isAbrupt()) {
                    // Ignore unreachable targets.
                    continue;
                }
                lastResult = switchClause.accept(this, mv);
            }
        }
        return lastResult;
    }

    /**
     * Generates a case-select method.
     * 
     * @param caseBlock
     *            the case-block
     * @param switchVarType
     *            the switch-var type
     * @param mv
     *            the code visitor
     * @return the outlined-call object
     */
    private OutlinedCall caseSelect(SwitchStatementGenerator.CaseBlock caseBlock, Type switchVarType, CodeVisitor mv) {
        SwitchClause firstClause = caseBlock.clauses.get(0);
        MethodTypeDescriptor methodDescriptor = SwitchSelectCodeVisitor.methodDescriptor(switchVarType, mv);
        MethodCode method = codegen.method(mv, "select", methodDescriptor);
        return outlined(new SwitchSelectCodeVisitor(firstClause, method, mv), body -> {
            Variable<?> switchValue = body.getSwitchValueParameter();
            MutableValue<Integer> switchTarget = body.iarrayElement(body.getSwitchTargetParameter(), 0);

            List<SwitchClause> clauses = caseBlock.clauses;
            int[] switchTargets = caseBlock.switchTargets;
            int numTargets = caseBlock.numTargets();

            Jump[] targetLabels = new Jump[numTargets];
            for (int i = 0; i < targetLabels.length; ++i) {
                targetLabels[i] = new Jump();
            }

            Jump lblExit = new Jump();
            Jump[] labels = new Jump[clauses.size()];
            for (int i = 0; i < clauses.size(); ++i) {
                labels[i] = targetLabels[switchTargets[i] - 1];
            }

            caseSelector(SwitchType.of(clauses)).select(clauses, switchValue, labels, lblExit, body);

            Jump setSwitchTarget = new Jump();
            for (int i = 0; i < targetLabels.length; ++i) {
                // targetLabels[i] is not reachable if only used by the default clause.
                if (targetLabels[i].isTarget()) {
                    body.mark(targetLabels[i]);
                    body.iconst(i + 1);
                    body.goTo(setSwitchTarget);
                }
            }
            if (setSwitchTarget.isTarget()) {
                // stack: [newSwitchTarget] -> []
                body.mark(setSwitchTarget);
                body.store(switchTarget);
            }
            body.mark(lblExit);

            return Completion.Normal;
        });
    }

    /**
     * Generates a case-block method.
     * 
     * @param caseBlock
     *            the case-block
     * @param mv
     *            the code visitor
     * @return the outlined-call object
     */
    private OutlinedCall caseBlock(SwitchStatementGenerator.CaseBlock caseBlock, CodeVisitor mv) {
        SwitchClause firstClause = caseBlock.clauses.get(0);
        MethodTypeDescriptor methodDescriptor = SwitchBlockCodeVisitor.methodDescriptor(mv);
        MethodCode method = codegen.method(mv, "case", methodDescriptor);
        return outlined(new SwitchBlockCodeVisitor(firstClause, method, mv), body -> {
            Variable<Integer> switchTarget = body.getSwitchTargetParameter();

            List<SwitchClause> clauses = caseBlock.clauses;
            int[] switchTargets = caseBlock.switchTargets;
            int numTargets = caseBlock.numTargets();

            Completion lastResult = Completion.Normal;
            if (numTargets > 1) {
                Jump[] labels = new Jump[numTargets];
                for (int i = 0; i < labels.length; ++i) {
                    labels[i] = new Jump();
                }

                Jump defaultInstr = new Jump();
                body.load(switchTarget);
                body.tableswitch(1, numTargets, defaultInstr, labels);
                body.mark(defaultInstr);

                for (int i = 0, lastTarget = 0; i < clauses.size(); ++i) {
                    if (lastTarget != switchTargets[i]) {
                        lastTarget = switchTargets[i];
                        body.mark(labels[lastTarget - 1]);
                    }
                    lastResult = clauses.get(i).accept(this, body);
                }
            } else {
                for (SwitchClause clause : clauses) {
                    lastResult = clause.accept(this, body);
                }
            }

            return lastResult;
        });
    }

    /**
     * Generates a case-block method.
     * 
     * @param caseBlock
     *            the case-block
     * @param mv
     *            the code visitor
     * @return the outlined-call object
     */
    private OutlinedCall defaultCaseBlock(SwitchStatementGenerator.CaseBlock caseBlock, CodeVisitor mv) {
        SwitchClause firstClause = caseBlock.clauses.get(0);
        MethodTypeDescriptor methodDescriptor = DefaultSwitchBlockCodeVisitor.methodDescriptor(mv);
        MethodCode method = codegen.method(mv, "case", methodDescriptor);
        return outlined(new DefaultSwitchBlockCodeVisitor(firstClause, method, mv), body -> {
            Completion lastResult = Completion.Normal;
            for (SwitchClause clause : caseBlock.clauses) {
                lastResult = clause.accept(this, body);
                if (lastResult.isAbrupt()) {
                    break;
                }
            }
            return lastResult;
        });
    }

    private static final class CaseBlock {
        final List<SwitchClause> clauses;
        final int[] switchTargets;
        final Jump entry;

        CaseBlock(List<SwitchClause> clauses, int[] switchTargets, Jump caseBlock) {
            this.clauses = clauses;
            this.switchTargets = switchTargets;
            this.entry = caseBlock;
        }

        int numTargets() {
            return switchTargets[switchTargets.length - 1];
        }

        CodeVisitor.HashKey selectKey() {
            return new CodeVisitor.LabelledHashKey(clauses.get(0), "select");
        }

        CodeVisitor.HashKey blockKey() {
            return new CodeVisitor.LabelledHashKey(clauses.get(0), "block");
        }
    }

    private static List<CaseBlock> computeCaseBlocks(SwitchStatement node, Function<List<SwitchClause>, CaseBlock> fn) {
        final int stepSize = SWITCH_CASE_LIMIT;
        List<SwitchClause> clauses = node.getClauses();
        int size = clauses.size();
        assert size >= 2 : "too few clauses: " + size;
        ArrayList<CaseBlock> blocks = new ArrayList<>();
        for (int i = 0, sizeMinusOne = size - 1; i < sizeMinusOne; i += stepSize) {
            int end = Math.min(i + stepSize, size);
            if (end + 1 == size) {
                // Avoid emitting a separate method for a single switch clause.
                end += 1;
            }
            assert (end - i) >= 2;
            blocks.add(fn.apply(clauses.subList(i, end)));
        }
        return blocks;
    }

    private static <T> int indexOf(List<T> list, Predicate<T> predicate) {
        for (int i = 0; i < list.size(); ++i) {
            if (predicate.test(list.get(i))) {
                return i;
            }
        }
        return -1;
    }

    private static final class SwitchSelectCodeVisitor extends OutlinedCodeVisitor {
        SwitchSelectCodeVisitor(SwitchClause node, MethodCode method, CodeVisitor parent) {
            super(node, method, parent);
        }

        @Override
        public void begin() {
            super.begin();
            setParameterNameUnchecked("switchValue", parameter(0));
            setParameterName("switchTarget", parameter(1), Types.int_);
        }

        Variable<?> getSwitchValueParameter() {
            return getParameterUnchecked(parameter(0));
        }

        Variable<int[]> getSwitchTargetParameter() {
            return getParameter(parameter(1), int[].class);
        }

        static MethodTypeDescriptor methodDescriptor(Type switchVarType, CodeVisitor mv) {
            MethodTypeDescriptor methodDescriptor = OutlinedCodeVisitor.outlinedMethodDescriptor(mv);
            return methodDescriptor.appendParameterTypes(switchVarType, Types.int_);
        }
    }

    private static final class SwitchBlockCodeVisitor extends OutlinedCodeVisitor {
        SwitchBlockCodeVisitor(SwitchClause node, MethodCode method, CodeVisitor parent) {
            super(node, method, parent);
        }

        @Override
        public void begin() {
            super.begin();
            setParameterName("switchTarget", parameter(0), Type.INT_TYPE);
        }

        Variable<Integer> getSwitchTargetParameter() {
            return getParameter(parameter(0), int.class);
        }

        static MethodTypeDescriptor methodDescriptor(CodeVisitor mv) {
            MethodTypeDescriptor methodDescriptor = OutlinedCodeVisitor.outlinedMethodDescriptor(mv);
            return methodDescriptor.appendParameterTypes(Type.INT_TYPE);
        }
    }

    private static final class DefaultSwitchBlockCodeVisitor extends OutlinedCodeVisitor {
        DefaultSwitchBlockCodeVisitor(SwitchClause node, MethodCode method, CodeVisitor parent) {
            super(node, method, parent);
        }

        static MethodTypeDescriptor methodDescriptor(CodeVisitor mv) {
            return OutlinedCodeVisitor.outlinedMethodDescriptor(mv);
        }
    }

    private static boolean isDefaultSwitch(SwitchStatement node) {
        // Empty or only default clause.
        List<SwitchClause> clauses = node.getClauses();
        return clauses.size() == 0 || clauses.size() == 1 && clauses.get(0).isDefaultClause();
    }

    private static boolean hasDefaultClause(SwitchStatement node) {
        return hasDefaultClause(node.getClauses());
    }

    private static boolean hasDefaultClause(List<SwitchClause> clauses) {
        for (SwitchClause switchClause : clauses) {
            if (switchClause.isDefaultClause()) {
                return true;
            }
        }
        return false;
    }

    private void invokeDynamicOperator(BinaryExpression.Operator operator, CodeVisitor mv) {
        // stack: [lval, rval, cx?] -> [result]
        mv.invokedynamic(Bootstrap.getName(operator), Bootstrap.getMethodDescriptor(operator),
                Bootstrap.getBootstrap(operator));
    }

    @FunctionalInterface
    private interface CaseSelector {
        void select(List<SwitchClause> clauses, Variable<?> switchValue, Jump[] labels, Jump defaultOrExit,
                CodeVisitor mv);
    }

    private CaseSelector caseSelector(SwitchType type) {
        switch (type) {
        case Generic:
            return this::emitGenericSwitch;
        case String:
            return this::emitStringSwitch;
        case Char:
            return this::emitCharSwitch;
        case Int:
            return this::emitIntSwitch;
        default:
            throw new AssertionError();
        }
    }

    /**
     * <h3>Generic-switch</h3>
     * 
     * <pre>
     * switch (v) {
     * case key1: ...
     * case key2: ...
     * }
     * 
     * var $v = v;
     * if (strictEquals($v, key1)) goto L1
     * if (strictEquals($v, key2)) goto L2
     * goTo (default | break)
     * L1: ...
     * L2: ...
     * </pre>
     * 
     * @param clauses
     *            the switch clauses
     * @param switchValue
     *            the variable which holds the switch value
     * @param labels
     *            the labels for each switch clause
     * @param defaultOrExit
     *            the default clause or exit label
     * @param mv
     *            the code visitor
     */
    private void emitGenericSwitch(List<SwitchClause> clauses, Variable<?> switchValue, Jump[] labels,
            Jump defaultOrExit, CodeVisitor mv) {
        assert switchValue.getType().equals(Types.Object);

        int index = 0;
        for (SwitchClause switchClause : clauses) {
            Jump caseLabel = labels[index++];
            if (!switchClause.isDefaultClause()) {
                mv.load(switchValue);
                // 13.11.10 Runtime Semantics: CaseSelectorEvaluation
                expressionBoxed(switchClause.getExpression(), mv);
                invokeDynamicOperator(BinaryExpression.Operator.SHEQ, mv);
                mv.ifne(caseLabel);
            }
        }

        mv.goTo(defaultOrExit);
    }

    /**
     * <h3>String-switch</h3>
     * 
     * <pre>
     * switch (v) {
     * case "key1": ...
     * case "key2": ...
     * }
     * 
     * var $v = v;
     * if (typeof $v == 'string') {
     *   lookupswitch(hashCode($v)) {
     *     hashCode("key1"): goto L1
     *     hashCode("key2"): goto L2
     *   }
     *   L1: if (equals($v, "key1")) ...
     *   L2: if (equals($v, "key2")) ...
     * }
     * </pre>
     * 
     * @param clauses
     *            the switch clauses
     * @param switchValue
     *            the variable which holds the switch value
     * @param labels
     *            the labels for each switch clause
     * @param defaultOrExit
     *            the default clause or exit label
     * @param mv
     *            the code visitor
     */
    private void emitStringSwitch(List<SwitchClause> clauses, Variable<?> switchValue, Jump[] labels,
            Jump defaultOrExit, CodeVisitor mv) {
        mv.enterVariableScope();
        Variable<String> switchValueString = mv.newVariable("switchValueString", String.class);
        if (switchValue.getType().equals(Types.CharSequence)) {
            mv.load(switchValue);
            mv.invoke(Methods.CharSequence_toString);
            mv.dup();
            mv.store(switchValueString);
            mv.invoke(Methods.String_hashCode);
        } else {
            assert switchValue.getType().equals(Types.Object);

            // test for string: type is java.lang.CharSequence
            mv.load(switchValue);
            mv.instanceOf(Types.CharSequence);
            mv.ifeq(defaultOrExit);

            mv.load(switchValue);
            mv.checkcast(Types.CharSequence);
            mv.invoke(Methods.CharSequence_toString);
            mv.dup();
            mv.store(switchValueString);
            mv.invoke(Methods.String_hashCode);
        }

        long[] entries = Entries(clauses, expr -> ((StringLiteral) expr).getValue().hashCode());
        int distinctValues = distinctValues(entries);
        Jump[] switchLabels = new Jump[distinctValues];
        int[] switchKeys = new int[distinctValues];
        for (int i = 0, j = 0, lastValue = 0, length = entries.length; i < length; ++i) {
            int value = Value(entries[i]);
            if (i == 0 || value != lastValue) {
                switchLabels[j] = new Jump();
                switchKeys[j] = value;
                j += 1;
            }
            lastValue = value;
        }

        // emit lookupswitch
        mv.lookupswitch(defaultOrExit, switchKeys, switchLabels);

        // add String.equals() calls
        for (int i = 0, j = 0, lastValue = 0, length = entries.length; i < length; ++i) {
            int value = Value(entries[i]);
            int index = Index(entries[i]);
            if (i == 0 || value != lastValue) {
                if (i != 0) {
                    mv.goTo(defaultOrExit);
                }
                mv.mark(switchLabels[j++]);
            }
            String string = ((StringLiteral) clauses.get(index).getExpression()).getValue();
            mv.load(switchValueString);
            mv.aconst(string);
            mv.invoke(Methods.String_equals);
            mv.ifne(labels[index]);
            lastValue = value;
        }
        mv.goTo(defaultOrExit);
        mv.exitVariableScope();
    }

    /**
     * <h3>char-switch</h3>
     * 
     * <pre>
     * switch (v) {
     * case "a": ...
     * case "b": ...
     * }
     * 
     * var $v = v;
     * if (typeof $v == 'string' {@literal &&} length($v) == 1) {
     *   tableswitch|lookupswitch(charCodeAt($v, 0)) {
     *     charCodeAt("a", 0): goto L1
     *     charCodeAt("b", 0): goto L2
     *   }
     *   L1: ...
     *   L2: ...
     * }
     * </pre>
     * 
     * @param clauses
     *            the switch clauses
     * @param switchValue
     *            the variable which holds the switch value
     * @param labels
     *            the labels for each switch clause
     * @param defaultOrExit
     *            the default clause or exit label
     * @param mv
     *            the code visitor
     */
    private void emitCharSwitch(List<SwitchClause> clauses, Variable<?> switchValue, Jump[] labels, Jump defaultOrExit,
            CodeVisitor mv) {
        if (switchValue.getType().equals(Types.CharSequence)) {
            // test for char: value is character (string with only one character)
            mv.load(switchValue);
            mv.invoke(Methods.CharSequence_length);
            mv.iconst(1);
            mv.ificmpne(defaultOrExit);

            mv.load(switchValue);
            mv.iconst(0);
            mv.invoke(Methods.CharSequence_charAt);
            // mv.cast(Type.CHAR_TYPE, Type.INT_TYPE);
        } else {
            assert switchValue.getType().equals(Types.Object);

            // test for char: type is java.lang.CharSequence
            mv.load(switchValue);
            mv.instanceOf(Types.CharSequence);
            mv.ifeq(defaultOrExit);

            // test for char: value is character (string with only one character)
            mv.enterVariableScope();
            Variable<CharSequence> switchValueChar = mv.newVariable("switchValueChar", CharSequence.class);
            mv.load(switchValue);
            mv.checkcast(Types.CharSequence);
            mv.dup();
            mv.store(switchValueChar);
            mv.invoke(Methods.CharSequence_length);
            mv.iconst(1);
            mv.ificmpne(defaultOrExit);

            mv.load(switchValueChar);
            mv.iconst(0);
            mv.invoke(Methods.CharSequence_charAt);
            // mv.cast(Type.CHAR_TYPE, Type.INT_TYPE);
            mv.exitVariableScope();
        }

        // emit tableswitch or lookupswitch
        long[] entries = Entries(clauses, expr -> ((StringLiteral) expr).getValue().charAt(0));
        switchInstruction(defaultOrExit, labels, entries, mv);
    }

    /**
     * <h3>int-switch</h3>
     * 
     * <pre>
     * switch (v) {
     * case 0: ...
     * case 1: ...
     * }
     * 
     * var $v = v;
     * if (typeof $v == 'number' {@literal &&} isInt($v)) {
     *   tableswitch|lookupswitch(int($v)) {
     *     int(0): goto L1
     *     int(1): goto L2
     *   }
     *   L1: ...
     *   L2: ...
     * }
     * </pre>
     * 
     * @param clauses
     *            the switch clauses
     * @param switchValue
     *            the variable which holds the switch value
     * @param labels
     *            the labels for each switch clause
     * @param defaultOrExit
     *            the default clause or exit label
     * @param mv
     *            the code visitor
     */
    private void emitIntSwitch(List<SwitchClause> clauses, Variable<?> switchValue, Jump[] labels, Jump defaultOrExit,
            CodeVisitor mv) {
        if (switchValue.getType().equals(Type.INT_TYPE)) {
            mv.load(switchValue);
        } else if (switchValue.getType().equals(Type.LONG_TYPE)) {
            // test for int: value is integer
            mv.load(switchValue);
            mv.dup2();
            mv.l2i();
            mv.i2l();
            mv.lcmp();
            mv.ifne(defaultOrExit);

            mv.load(switchValue);
            mv.l2i();
        } else if (switchValue.getType().equals(Type.DOUBLE_TYPE)) {
            // test for int: value is integer
            mv.load(switchValue);
            mv.dup2();
            mv.d2i();
            mv.i2d();
            mv.dcmpl();
            mv.ifne(defaultOrExit);

            mv.load(switchValue);
            mv.d2i();
        } else {
            assert switchValue.getType().equals(Types.Object);

            // test for int: type is java.lang.Number
            mv.load(switchValue);
            mv.instanceOf(Types.Number);
            mv.ifeq(defaultOrExit);

            // test for int: value is integer
            mv.enterVariableScope();
            Variable<Integer> switchValueNum = mv.newVariable("switchValueNum", int.class);
            mv.load(switchValue);
            mv.checkcast(Types.Number);
            mv.invoke(Methods.Number_doubleValue);
            mv.dup2();
            mv.d2i();
            mv.dup();
            mv.store(switchValueNum);
            mv.i2d();
            mv.dcmpl();
            mv.ifne(defaultOrExit);

            mv.load(switchValueNum);
            mv.exitVariableScope();
        }

        // emit tableswitch or lookupswitch
        long[] entries = Entries(clauses, expr -> {
            if (expr instanceof NumericLiteral) {
                return ((NumericLiteral) expr).intValue();
            }
            return -((NumericLiteral) ((UnaryExpression) expr).getOperand()).intValue();
        });
        switchInstruction(defaultOrExit, labels, entries, mv);
    }

    /**
     * Shared implementation for int- and char-switches.
     * 
     * @param switchDefault
     *            the switch default instruction label
     * @param labels
     *            the switch labels
     * @param entries
     *            the switch entries, value-index pairs
     * @param mv
     *            the code visitor
     */
    private static void switchInstruction(Jump switchDefault, Jump[] labels, long[] entries, CodeVisitor mv) {
        int entriesLength = entries.length;
        int distinctValues = distinctValues(entries);
        int minValue = Value(entries[0]);
        int maxValue = Value(entries[entriesLength - 1]);
        int range = maxValue - minValue + 1;
        float density = (float) distinctValues / range;
        if (range > 0 && (range <= 5 || density >= 0.5f)) {
            // System.out.printf("tableswitch [%d: %d - %d]\n", entriesLength, minValue, maxValue);
            Jump[] switchLabels = new Jump[range];
            Arrays.fill(switchLabels, switchDefault);
            for (int i = 0, lastValue = 0; i < entriesLength; ++i) {
                int value = Value(entries[i]);
                int index = Index(entries[i]);
                if (i == 0 || value != lastValue) {
                    switchLabels[value - minValue] = labels[index];
                }
                lastValue = value;
            }
            mv.tableswitch(minValue, maxValue, switchDefault, switchLabels);
        } else {
            // System.out.printf("lookupswitch [%d: %d - %d]\n", entriesLength, minValue, maxValue);
            Jump[] switchLabels = new Jump[distinctValues];
            int[] switchKeys = new int[distinctValues];
            for (int i = 0, j = 0, lastValue = 0; i < entriesLength; ++i) {
                int value = Value(entries[i]);
                int index = Index(entries[i]);
                if (i == 0 || value != lastValue) {
                    switchLabels[j] = labels[index];
                    switchKeys[j] = value;
                    j += 1;
                }
                lastValue = value;
            }
            mv.lookupswitch(switchDefault, switchKeys, switchLabels);
        }
    }

    private static int distinctValues(long[] entries) {
        int distinctValues = 0;
        for (int i = 0, lastValue = 0, length = entries.length; i < length; ++i) {
            int value = Value(entries[i]);
            if (i == 0 || value != lastValue) {
                distinctValues += 1;
            }
            lastValue = value;
        }
        return distinctValues;
    }

    private static long[] Entries(List<SwitchClause> clauses, ToIntFunction<Expression> value) {
        boolean hasDefault = hasDefaultClause(clauses);
        long[] entries = new long[clauses.size() - (hasDefault ? 1 : 0)];
        for (int i = 0, j = 0, size = clauses.size(); i < size; ++i) {
            SwitchClause switchClause = clauses.get(i);
            if (!switchClause.isDefaultClause()) {
                entries[j++] = Entry(value.applyAsInt(switchClause.getExpression()), i);
            }
        }
        // sort values in ascending order
        Arrays.sort(entries);
        return entries;
    }

    private static long Entry(int value, int index) {
        return ((long) value) << 32 | index;
    }

    private static int Index(long entry) {
        return (int) entry;
    }

    private static int Value(long entry) {
        return (int) (entry >> 32);
    }
}
