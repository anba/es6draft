/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static com.github.anba.es6draft.semantics.StaticSemantics.LexicallyScopedDeclarations;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.objectweb.asm.Type;

import com.github.anba.es6draft.ast.AbruptNode.Abrupt;
import com.github.anba.es6draft.ast.Expression;
import com.github.anba.es6draft.ast.Node;
import com.github.anba.es6draft.ast.NumericLiteral;
import com.github.anba.es6draft.ast.StringLiteral;
import com.github.anba.es6draft.ast.SwitchClause;
import com.github.anba.es6draft.ast.SwitchStatement;
import com.github.anba.es6draft.compiler.JumpLabels.BreakLabel;
import com.github.anba.es6draft.compiler.StatementGenerator.Completion;
import com.github.anba.es6draft.compiler.assembler.Jump;
import com.github.anba.es6draft.compiler.assembler.MethodDesc;
import com.github.anba.es6draft.compiler.assembler.Variable;
import com.github.anba.es6draft.runtime.LexicalEnvironment;

/**
 * <h1>13 ECMAScript Language: Statements and Declarations</h1>
 * <ul>
 * <li>13.11 The switch Statement
 * </ul>
 */
final class SwitchStatementGenerator extends
        DefaultCodeGenerator<StatementGenerator.Completion, StatementVisitor> {
    private static final class Methods {
        // class: AbstractOperations
        static final MethodDesc AbstractOperations_StrictEqualityComparison = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.AbstractOperations, "StrictEqualityComparison",
                Type.getMethodType(Type.BOOLEAN_TYPE, Types.Object, Types.Object));

        // class: CharSequence
        static final MethodDesc CharSequence_charAt = MethodDesc.create(
                MethodDesc.Invoke.Interface, Types.CharSequence, "charAt",
                Type.getMethodType(Type.CHAR_TYPE, Type.INT_TYPE));
        static final MethodDesc CharSequence_length = MethodDesc.create(
                MethodDesc.Invoke.Interface, Types.CharSequence, "length",
                Type.getMethodType(Type.INT_TYPE));
        static final MethodDesc CharSequence_toString = MethodDesc.create(
                MethodDesc.Invoke.Interface, Types.CharSequence, "toString",
                Type.getMethodType(Types.String));

        // class: Number
        static final MethodDesc Number_doubleValue = MethodDesc.create(MethodDesc.Invoke.Virtual,
                Types.Number, "doubleValue", Type.getMethodType(Type.DOUBLE_TYPE));

        // class: String
        static final MethodDesc String_equals = MethodDesc.create(MethodDesc.Invoke.Virtual,
                Types.String, "equals", Type.getMethodType(Type.BOOLEAN_TYPE, Types.Object));
        static final MethodDesc String_hashCode = MethodDesc.create(MethodDesc.Invoke.Virtual,
                Types.String, "hashCode", Type.getMethodType(Type.INT_TYPE));
    }

    private enum SwitchType {
        Int, Char, String, Generic, Default;

        private static boolean isIntSwitch(SwitchStatement node) {
            for (SwitchClause switchClause : node.getClauses()) {
                Expression expr = switchClause.getExpression();
                if (expr != null) {
                    // TODO: does not handle negative numbers (UnaryExpression -> (NumericLiteral))
                    if (!(expr instanceof NumericLiteral && ((NumericLiteral) expr).isInt())) {
                        return false;
                    }
                }
            }
            return true;
        }

        private static boolean isCharSwitch(SwitchStatement node) {
            for (SwitchClause switchClause : node.getClauses()) {
                Expression expr = switchClause.getExpression();
                if (expr != null) {
                    if (!(expr instanceof StringLiteral)) {
                        return false;
                    }
                    if (((StringLiteral) expr).getValue().length() != 1) {
                        return false;
                    }
                }
            }
            return true;
        }

        private static boolean isStringSwitch(SwitchStatement node) {
            for (SwitchClause switchClause : node.getClauses()) {
                Expression expr = switchClause.getExpression();
                if (expr != null && !(expr instanceof StringLiteral)) {
                    return false;
                }
            }
            return true;
        }

        static SwitchType of(SwitchStatement node) {
            List<SwitchClause> clauses = node.getClauses();
            if (clauses.size() == 0 || clauses.size() == 1 && clauses.get(0).isDefaultClause()) {
                // empty or only default clause
                return Default;
            }
            if (isIntSwitch(node)) {
                return Int;
            }
            if (isCharSwitch(node)) {
                return Char;
            }
            if (isStringSwitch(node)) {
                return String;
            }
            return Generic;
        }
    }

    public SwitchStatementGenerator(CodeGenerator codegen) {
        super(codegen);
    }

    @Override
    protected Completion visit(Node node, StatementVisitor mv) {
        throw new IllegalStateException(String.format("node-class: %s", node.getClass()));
    }

    /**
     * 13.11.7 Runtime Semantics: Evaluation
     */
    @Override
    public Completion visit(SwitchClause node, StatementVisitor mv) {
        return codegen.statements(node.getStatements(), mv);
    }

    /**
     * 13.11.7 Runtime Semantics: Evaluation
     */
    @Override
    public Completion visit(SwitchStatement node, StatementVisitor mv) {
        // stack -> switchValue
        ValType switchValueType = expressionValue(node.getExpression(), mv);

        SwitchType type = SwitchType.of(node);
        boolean defaultOrReturn = false;
        if (type == SwitchType.Int) {
            if (!switchValueType.isNumeric() && switchValueType != ValType.Any) {
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
        } else if (type == SwitchType.Generic) {
            mv.toBoxed(switchValueType);
            switchValueType = ValType.Any;
        } else {
            assert type == SwitchType.Default;
            defaultOrReturn = true;
        }

        final boolean defaultClausePresent = hasDefaultClause(node);
        if (defaultOrReturn) {
            // never true -> emit default switch or return
            mv.pop(switchValueType);
            if (defaultClausePresent) {
                type = SwitchType.Default;
            } else {
                return Completion.Normal;
            }
        }

        mv.enterVariableScope();
        Variable<LexicalEnvironment<?>> savedEnv = saveEnvironment(node, mv);

        Variable<?> switchValue = null;
        if (type != SwitchType.Default) {
            switchValue = mv.newVariable("switchValue", switchValueType.toClass());
            mv.store(switchValue);
        }

        boolean hasDeclarations = !LexicallyScopedDeclarations(node).isEmpty();
        if (hasDeclarations) {
            newDeclarativeEnvironment(mv);
            new BlockDeclarationInstantiationGenerator(codegen).generate(node, mv);
            pushLexicalEnvironment(mv);
        }

        Jump lblExit = new Jump();
        BreakLabel lblBreak = new BreakLabel();
        mv.enterScope(node);
        mv.enterBreakable(node, lblBreak);
        Completion result = CaseBlockEvaluation(node, type, lblExit, switchValue, mv);
        mv.exitBreakable(node);
        mv.exitScope();

        if (!defaultClausePresent) {
            mv.mark(lblExit);
        }
        if (hasDeclarations && !result.isAbrupt()) {
            popLexicalEnvironment(mv);
        }
        if (lblBreak.isTarget()) {
            mv.mark(lblBreak);
            restoreEnvironment(node, Abrupt.Break, savedEnv, mv);
        }
        mv.exitVariableScope();

        return result.normal(lblBreak.isTarget());
    }

    /**
     * 13.11.5 Runtime Semantics: CaseBlockEvaluation
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
     *            the statement visitor
     * @return the completion value
     */
    private Completion CaseBlockEvaluation(SwitchStatement node, SwitchType type, Jump lblExit,
            Variable<?> switchValue, StatementVisitor mv) {
        List<SwitchClause> clauses = node.getClauses();
        Jump lblDefault = null;
        Jump[] labels = new Jump[clauses.size()];
        for (int i = 0, size = clauses.size(); i < size; ++i) {
            labels[i] = new Jump();
            if (clauses.get(i).isDefaultClause()) {
                assert lblDefault == null;
                lblDefault = labels[i];
            }
        }

        if (type == SwitchType.Int) {
            emitIntSwitch(clauses, labels, lblDefault, lblExit, switchValue, mv);
        } else if (type == SwitchType.Char) {
            emitCharSwitch(clauses, labels, lblDefault, lblExit, switchValue, mv);
        } else if (type == SwitchType.String) {
            emitStringSwitch(clauses, labels, lblDefault, lblExit, switchValue, mv);
        } else if (type == SwitchType.Generic) {
            emitGenericSwitch(clauses, labels, lblDefault, lblExit, switchValue, mv);
        } else {
            assert type == SwitchType.Default;
            emitDefaultSwitch(clauses, labels, lblDefault, lblExit, switchValue, mv);
        }

        Completion result = Completion.Normal, lastResult = Completion.Normal;
        if (type == SwitchType.Default) {
            Iterator<SwitchClause> iter = clauses.iterator();
            // skip leading clauses until default clause found
            while (iter.hasNext()) {
                SwitchClause switchClause = iter.next();
                if (switchClause.isDefaultClause()) {
                    lastResult = switchClause.accept(this, mv);
                    break;
                }
            }
            // handle clauses following default clause until abrupt completion
            while (iter.hasNext() && !lastResult.isAbrupt()) {
                lastResult = iter.next().accept(this, mv);
            }
            result = lastResult;
        } else {
            int index = 0;
            for (SwitchClause switchClause : clauses) {
                Jump caseLabel = labels[index++];
                if (caseLabel != null) {
                    mv.mark(caseLabel);
                } else if (lastResult.isAbrupt()) {
                    // Ignore unreachable targets
                    continue;
                }
                Completion innerResult = switchClause.accept(this, mv);
                if (innerResult.isAbrupt()) {
                    // not fall-thru
                    result = result.isAbrupt() ? result.select(innerResult) : innerResult;
                }
                lastResult = innerResult;
            }
        }

        return result.normal(lblDefault == null || !lastResult.isAbrupt());
    }

    private static boolean hasDefaultClause(SwitchStatement node) {
        for (SwitchClause switchClause : node.getClauses()) {
            if (switchClause.isDefaultClause()) {
                return true;
            }
        }
        return false;
    }

    /**
     * <h3>default-switch</h3>
     * 
     * <pre>
     * switch (v) {
     * case key1: ...
     * case key2: ...
     * default: ...
     * }
     * 
     * goto :default
     * </pre>
     * 
     * @param clauses
     *            the switch clauses
     * @param labels
     *            the labels for each switch clause
     * @param defaultClause
     *            the label for the default clause
     * @param lblExit
     *            the exit label
     * @param switchValue
     *            the variable which holds the switch value
     * @param mv
     *            the statement visitor
     */
    private void emitDefaultSwitch(List<SwitchClause> clauses, Jump[] labels, Jump defaultClause,
            Jump lblExit, Variable<?> switchValue, StatementVisitor mv) {
        assert switchValue == null;
        // directly jump to default clause; since switch clauses before default clause are not
        // emitted, jump instruction can be elided as well, so we directly fall into the
        // default clause
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
     * @param labels
     *            the labels for each switch clause
     * @param defaultClause
     *            the label for the default clause
     * @param lblExit
     *            the exit label
     * @param switchValue
     *            the variable which holds the switch value
     * @param mv
     *            the statement visitor
     */
    private void emitGenericSwitch(List<SwitchClause> clauses, Jump[] labels, Jump defaultClause,
            Jump lblExit, Variable<?> switchValue, StatementVisitor mv) {
        assert switchValue.getType().equals(Types.Object);
        Jump switchDefault = defaultClause != null ? defaultClause : lblExit;

        int index = 0;
        for (SwitchClause switchClause : clauses) {
            Jump caseLabel = labels[index++];
            Expression expr = switchClause.getExpression();
            if (expr != null) {
                mv.load(switchValue);
                expressionBoxedValue(expr, mv);
                mv.invoke(Methods.AbstractOperations_StrictEqualityComparison);
                mv.ifne(caseLabel);
            }
        }

        mv.goTo(switchDefault);
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
     * @param labels
     *            the labels for each switch clause
     * @param defaultClause
     *            the label for the default clause
     * @param lblExit
     *            the exit label
     * @param switchValue
     *            the variable which holds the switch value
     * @param mv
     *            the statement visitor
     */
    private void emitStringSwitch(List<SwitchClause> clauses, Jump[] labels, Jump defaultClause,
            Jump lblExit, Variable<?> switchValue, StatementVisitor mv) {
        Jump switchDefault = defaultClause != null ? defaultClause : lblExit;
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
            mv.ifeq(switchDefault);

            mv.load(switchValue);
            mv.checkcast(Types.CharSequence);
            mv.invoke(Methods.CharSequence_toString);
            mv.dup();
            mv.store(switchValueString);
            mv.invoke(Methods.String_hashCode);
        }

        long[] entries = stringSwitchEntries(clauses, defaultClause != null);
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
        mv.lookupswitch(switchDefault, switchKeys, switchLabels);

        // add String.equals() calls
        for (int i = 0, j = 0, lastValue = 0, length = entries.length; i < length; ++i) {
            int value = Value(entries[i]);
            int index = Index(entries[i]);
            if (i == 0 || value != lastValue) {
                if (i != 0) {
                    mv.goTo(switchDefault);
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
        mv.goTo(switchDefault);
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
     * @param labels
     *            the labels for each switch clause
     * @param defaultClause
     *            the label for the default clause
     * @param lblExit
     *            the exit label
     * @param switchValue
     *            the variable which holds the switch value
     * @param mv
     *            the statement visitor
     */
    private void emitCharSwitch(List<SwitchClause> clauses, Jump[] labels, Jump defaultClause,
            Jump lblExit, Variable<?> switchValue, StatementVisitor mv) {
        Jump switchDefault = defaultClause != null ? defaultClause : lblExit;
        if (switchValue.getType().equals(Types.CharSequence)) {
            // test for char: value is character (string with only one character)
            mv.load(switchValue);
            mv.invoke(Methods.CharSequence_length);
            mv.iconst(1);
            mv.ificmpne(switchDefault);

            mv.load(switchValue);
            mv.iconst(0);
            mv.invoke(Methods.CharSequence_charAt);
            // mv.cast(Type.CHAR_TYPE, Type.INT_TYPE);
        } else {
            assert switchValue.getType().equals(Types.Object);

            // test for char: type is java.lang.CharSequence
            mv.load(switchValue);
            mv.instanceOf(Types.CharSequence);
            mv.ifeq(switchDefault);

            // test for char: value is character (string with only one character)
            mv.enterVariableScope();
            Variable<CharSequence> switchValueChar = mv.newVariable("switchValueChar",
                    CharSequence.class);
            mv.load(switchValue);
            mv.checkcast(Types.CharSequence);
            mv.dup();
            mv.store(switchValueChar);
            mv.invoke(Methods.CharSequence_length);
            mv.iconst(1);
            mv.ificmpne(switchDefault);

            mv.load(switchValueChar);
            mv.iconst(0);
            mv.invoke(Methods.CharSequence_charAt);
            // mv.cast(Type.CHAR_TYPE, Type.INT_TYPE);
            mv.exitVariableScope();
        }

        // emit tableswitch or lookupswitch
        long[] entries = charSwitchEntries(clauses, defaultClause != null);
        switchInstruction(switchDefault, labels, entries, mv);
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
     * @param labels
     *            the labels for each switch clause
     * @param defaultClause
     *            the label for the default clause
     * @param lblExit
     *            the exit label
     * @param switchValue
     *            the variable which holds the switch value
     * @param mv
     *            the statement visitor
     */
    private void emitIntSwitch(List<SwitchClause> clauses, Jump[] labels, Jump defaultClause,
            Jump lblExit, Variable<?> switchValue, StatementVisitor mv) {
        Jump switchDefault = defaultClause != null ? defaultClause : lblExit;
        if (switchValue.getType().equals(Type.INT_TYPE)) {
            mv.load(switchValue);
        } else if (switchValue.getType().equals(Type.LONG_TYPE)) {
            // test for int: value is integer
            mv.load(switchValue);
            mv.dup2();
            mv.l2i();
            mv.i2l();
            mv.lcmp();
            mv.ifne(switchDefault);

            mv.load(switchValue);
            mv.l2i();
        } else if (switchValue.getType().equals(Type.DOUBLE_TYPE)) {
            // test for int: value is integer
            mv.load(switchValue);
            mv.dup2();
            mv.d2i();
            mv.i2d();
            mv.dcmpl();
            mv.ifne(switchDefault);

            mv.load(switchValue);
            mv.d2i();
        } else {
            assert switchValue.getType().equals(Types.Object);

            // test for int: type is java.lang.Number
            mv.load(switchValue);
            mv.instanceOf(Types.Number);
            mv.ifeq(switchDefault);

            // test for int: value is integer
            mv.enterVariableScope();
            Variable<Double> switchValueNum = mv.newVariable("switchValueNum", double.class);
            mv.load(switchValue);
            mv.checkcast(Types.Number);
            mv.invoke(Methods.Number_doubleValue);
            mv.dup2();
            mv.dup2();
            mv.store(switchValueNum);
            mv.d2i();
            mv.i2d();
            mv.dcmpl();
            mv.ifne(switchDefault);

            mv.load(switchValueNum);
            mv.d2i();
            mv.exitVariableScope();
        }

        // emit tableswitch or lookupswitch
        long[] entries = intSwitchEntries(clauses, defaultClause != null);
        switchInstruction(switchDefault, labels, entries, mv);
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
     *            the statement visitor
     */
    private static void switchInstruction(Jump switchDefault, Jump[] labels, long[] entries,
            StatementVisitor mv) {
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
                } else {
                    // Duplicate case value
                    labels[index] = null;
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
                } else {
                    // Duplicate case value
                    labels[index] = null;
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

    private static long[] stringSwitchEntries(List<SwitchClause> clauses, boolean hasDefault) {
        long[] entries = new long[clauses.size() - (hasDefault ? 1 : 0)];
        for (int i = 0, j = 0, size = clauses.size(); i < size; ++i) {
            Expression expr = clauses.get(i).getExpression();
            if (expr != null) {
                entries[j++] = Entry(((StringLiteral) expr).getValue().hashCode(), i);
            }
        }
        // sort values in ascending order
        Arrays.sort(entries);
        return entries;
    }

    private static long[] intSwitchEntries(List<SwitchClause> clauses, boolean hasDefault) {
        long[] entries = new long[clauses.size() - (hasDefault ? 1 : 0)];
        for (int i = 0, j = 0, size = clauses.size(); i < size; ++i) {
            Expression expr = clauses.get(i).getExpression();
            if (expr != null) {
                entries[j++] = Entry(((NumericLiteral) expr).intValue(), i);
            }
        }
        // sort values in ascending order
        Arrays.sort(entries);
        return entries;
    }

    private static long[] charSwitchEntries(List<SwitchClause> clauses, boolean hasDefault) {
        long[] entries = new long[clauses.size() - (hasDefault ? 1 : 0)];
        for (int i = 0, j = 0, size = clauses.size(); i < size; ++i) {
            Expression expr = clauses.get(i).getExpression();
            if (expr != null) {
                entries[j++] = Entry(((StringLiteral) expr).getValue().charAt(0), i);
            }
        }
        // sort values in ascending order
        Arrays.sort(entries);
        return entries;
    }

    private static final long Entry(int value, int index) {
        return ((long) value) << 32 | index;
    }

    private static final int Index(long entry) {
        return (int) entry;
    }

    private static final int Value(long entry) {
        return (int) (entry >> 32);
    }
}
