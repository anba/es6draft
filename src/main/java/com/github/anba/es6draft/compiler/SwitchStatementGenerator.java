/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static com.github.anba.es6draft.semantics.StaticSemantics.LexicalDeclarations;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.objectweb.asm.Label;
import org.objectweb.asm.Type;

import com.github.anba.es6draft.ast.AbruptNode.Abrupt;
import com.github.anba.es6draft.ast.Declaration;
import com.github.anba.es6draft.ast.Expression;
import com.github.anba.es6draft.ast.Node;
import com.github.anba.es6draft.ast.NumericLiteral;
import com.github.anba.es6draft.ast.StatementListItem;
import com.github.anba.es6draft.ast.StringLiteral;
import com.github.anba.es6draft.ast.SwitchClause;
import com.github.anba.es6draft.ast.SwitchStatement;
import com.github.anba.es6draft.compiler.InstructionVisitor.MethodDesc;
import com.github.anba.es6draft.compiler.InstructionVisitor.MethodType;
import com.github.anba.es6draft.compiler.InstructionVisitor.Variable;
import com.github.anba.es6draft.compiler.JumpLabel.BreakLabel;
import com.github.anba.es6draft.compiler.StatementGenerator.Completion;
import com.github.anba.es6draft.runtime.LexicalEnvironment;

/**
 * <h1>13 ECMAScript Language: Statements and Declarations</h1>
 * <ul>
 * <li>13.11 The switch Statement
 * </ul>
 */
class SwitchStatementGenerator extends
        DefaultCodeGenerator<StatementGenerator.Completion, StatementVisitor> {
    private static class Methods {
        // class: CharSequence
        static final MethodDesc CharSequence_charAt = MethodDesc.create(MethodType.Interface,
                Types.CharSequence, "charAt", Type.getMethodType(Type.CHAR_TYPE, Type.INT_TYPE));
        static final MethodDesc CharSequence_length = MethodDesc.create(MethodType.Interface,
                Types.CharSequence, "length", Type.getMethodType(Type.INT_TYPE));
        static final MethodDesc CharSequence_toString = MethodDesc.create(MethodType.Interface,
                Types.CharSequence, "toString", Type.getMethodType(Types.String));

        // class: Number
        static final MethodDesc Number_doubleValue = MethodDesc.create(MethodType.Virtual,
                Types.Number, "doubleValue", Type.getMethodType(Type.DOUBLE_TYPE));

        // class: String
        static final MethodDesc String_equals = MethodDesc.create(MethodType.Virtual, Types.String,
                "equals", Type.getMethodType(Type.BOOLEAN_TYPE, Types.Object));
        static final MethodDesc String_hashCode = MethodDesc.create(MethodType.Virtual,
                Types.String, "hashCode", Type.getMethodType(Type.INT_TYPE));

        // class: ScriptRuntime
        static final MethodDesc ScriptRuntime_strictEqualityComparison = MethodDesc.create(
                MethodType.Static, Types.ScriptRuntime, "strictEqualityComparison",
                Type.getMethodType(Type.BOOLEAN_TYPE, Types.Object, Types.Object));
    }

    private enum SwitchType {
        Int, Char, String, Generic, Default;

        private static boolean isIntSwitch(SwitchStatement node) {
            for (SwitchClause switchClause : node.getClauses()) {
                Expression expr = switchClause.getExpression();
                if (expr != null) {
                    if (!(expr instanceof NumericLiteral)) {
                        return false;
                    }
                    double value = ((NumericLiteral) expr).getValue();
                    if (value != (int) value) {
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

    @Override
    protected Completion visit(StatementListItem node, StatementVisitor mv) {
        return codegen.statement(node, mv);
    }

    /**
     * 13.11.7 Runtime Semantics: Evaluation
     */
    @Override
    public Completion visit(SwitchClause node, StatementVisitor mv) {
        Completion result = Completion.Normal;
        for (StatementListItem stmt : node.getStatements()) {
            if ((result = result.then(stmt.accept(this, mv))).isAbrupt()) {
                break;
            }
        }
        return result;
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

        if (defaultOrReturn) {
            // never true -> emit default switch or return
            mv.pop(switchValueType);
            if (hasDefaultClause(node)) {
                type = SwitchType.Default;
            } else {
                return Completion.Normal;
            }
        }

        mv.enterVariableScope();
        Variable<LexicalEnvironment> savedEnv = saveEnvironment(node, mv);

        Variable<?> switchValue = null;
        if (type != SwitchType.Default) {
            switchValue = mv.newVariable("switchValue", switchValueType.toClass());
            mv.store(switchValue);
        }

        mv.enterScope(node);
        Collection<Declaration> declarations = LexicalDeclarations(node);
        if (!declarations.isEmpty()) {
            newDeclarativeEnvironment(mv);
            new BlockDeclarationInstantiationGenerator(codegen).generate(declarations, mv);
            pushLexicalEnvironment(mv);
        }

        BreakLabel lblBreak = new BreakLabel();
        Completion result = CaseBlockEvaluation(node, type, lblBreak, switchValue, mv);

        if (!declarations.isEmpty() && !result.isAbrupt()) {
            popLexicalEnvironment(mv);
        }
        mv.exitScope();

        mv.mark(lblBreak);
        if (lblBreak.isUsed()) {
            restoreEnvironment(node, Abrupt.Break, savedEnv, mv);
        }
        mv.exitVariableScope();

        return result;
    }

    /**
     * 13.11.5 Runtime Semantics: CaseBlockEvaluation
     */
    private Completion CaseBlockEvaluation(SwitchStatement node, SwitchType type,
            BreakLabel lblBreak, Variable<?> switchValue, StatementVisitor mv) {
        List<SwitchClause> clauses = node.getClauses();
        Label lblDefault = null;
        Label[] labels = new Label[clauses.size()];
        for (int i = 0, size = clauses.size(); i < size; ++i) {
            labels[i] = new Label();
            if (clauses.get(i).isDefaultClause()) {
                assert lblDefault == null;
                lblDefault = labels[i];
            }
        }

        if (type == SwitchType.Int) {
            emitIntSwitch(clauses, labels, lblDefault, lblBreak, switchValue, mv);
        } else if (type == SwitchType.Char) {
            emitCharSwitch(clauses, labels, lblDefault, lblBreak, switchValue, mv);
        } else if (type == SwitchType.String) {
            emitStringSwitch(clauses, labels, lblDefault, lblBreak, switchValue, mv);
        } else if (type == SwitchType.Generic) {
            emitGenericSwitch(clauses, labels, lblDefault, lblBreak, switchValue, mv);
        } else {
            assert type == SwitchType.Default;
            emitDefaultSwitch(clauses, labels, lblDefault, lblBreak, switchValue, mv);
        }

        Completion result = Completion.Normal, lastResult = Completion.Normal;
        mv.enterBreakable(node, lblBreak);
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
                mv.mark(labels[index++]);
                Completion innerResult = switchClause.accept(this, mv);
                if (innerResult.isAbrupt()) {
                    // not fall-thru
                    result = result.isAbrupt() ? result.select(innerResult) : innerResult;
                }
                lastResult = innerResult;
            }
        }
        mv.exitBreakable(node);

        return result.normal(lblBreak.isUsed() || lblDefault == null || !lastResult.isAbrupt());
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
     */
    private void emitDefaultSwitch(List<SwitchClause> clauses, Label[] labels, Label defaultClause,
            Label lblBreak, Variable<?> switchValue, StatementVisitor mv) {
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
     * L1: ...
     * L2: ...
     * </pre>
     */
    private void emitGenericSwitch(List<SwitchClause> clauses, Label[] labels, Label defaultClause,
            Label lblBreak, Variable<?> switchValue, StatementVisitor mv) {
        assert switchValue.getType().equals(Types.Object);

        int index = 0;
        for (SwitchClause switchClause : clauses) {
            Label caseLabel = labels[index++];
            Expression expr = switchClause.getExpression();
            if (expr != null) {
                mv.load(switchValue);
                expressionBoxedValue(expr, mv);
                mv.invoke(Methods.ScriptRuntime_strictEqualityComparison);
                mv.ifne(caseLabel);
            }
        }

        if (defaultClause != null) {
            mv.goTo(defaultClause);
        } else {
            mv.goTo(lblBreak);
        }
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
     */
    private void emitStringSwitch(List<SwitchClause> clauses, Label[] labels, Label defaultClause,
            Label lblBreak, Variable<?> switchValue, StatementVisitor mv) {
        Label switchDefault = defaultClause != null ? defaultClause : lblBreak;
        Variable<String> switchValueString = mv.newVariable("switchValueString", String.class);
        if (switchValue.getType().equals(Types.CharSequence)) {
            mv.load(switchValue);
            mv.invoke(Methods.CharSequence_toString);
            mv.dup();
            mv.store(switchValueString);
            mv.invoke(Methods.String_hashCode);
        } else {
            assert switchValue.getType().equals(Types.Object);

            // test for string-ness: type is java.lang.CharSequence
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
        Label[] switchLabels = new Label[distinctValues];
        int[] switchKeys = new int[distinctValues];
        for (int i = 0, j = 0, lastValue = 0, length = entries.length; i < length; ++i) {
            int value = Value(entries[i]);
            if (i == 0 || value != lastValue) {
                switchLabels[j] = new Label();
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
     * if (typeof $v == 'string' && length($v) == 1) {
     *   tableswitch|lookupswitch(charCodeAt($v, 0)) {
     *     charCodeAt("a", 0): goto L1
     *     charCodeAt("b", 0): goto L2
     *   }
     *   L1: ...
     *   L2: ...
     * }
     * </pre>
     */
    private void emitCharSwitch(List<SwitchClause> clauses, Label[] labels, Label defaultClause,
            Label lblBreak, Variable<?> switchValue, StatementVisitor mv) {
        Label switchDefault = defaultClause != null ? defaultClause : lblBreak;
        if (switchValue.getType().equals(Types.CharSequence)) {
            // test for char-ness: value is char (string with only one character)
            mv.load(switchValue);
            mv.invoke(Methods.CharSequence_length);
            mv.iconst(1);
            mv.ificmpne(switchDefault);

            mv.load(switchValue);
            mv.iconst(0);
            mv.invoke(Methods.CharSequence_charAt);
            mv.cast(Type.CHAR_TYPE, Type.INT_TYPE);
        } else {
            assert switchValue.getType().equals(Types.Object);

            // test for char-ness: type is java.lang.CharSequence
            mv.load(switchValue);
            mv.instanceOf(Types.CharSequence);
            mv.ifeq(switchDefault);

            // test for char-ness: value is char (string with only one character)
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
            mv.cast(Type.CHAR_TYPE, Type.INT_TYPE);
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
     * if (typeof $v == 'number' && isInt($v)) {
     *   tableswitch|lookupswitch(int($v)) {
     *     int(0): goto L1
     *     int(1): goto L2
     *   }
     *   L1: ...
     *   L2: ...
     * }
     * </pre>
     */
    private void emitIntSwitch(List<SwitchClause> clauses, Label[] labels, Label defaultClause,
            Label lblBreak, Variable<?> switchValue, StatementVisitor mv) {
        Label switchDefault = defaultClause != null ? defaultClause : lblBreak;
        if (switchValue.getType().equals(Type.INT_TYPE)) {
            mv.load(switchValue);
        } else if (switchValue.getType().equals(Type.LONG_TYPE)) {
            // test for int-ness: value is integer
            mv.load(switchValue);
            mv.dup2();
            mv.cast(Type.LONG_TYPE, Type.INT_TYPE);
            mv.cast(Type.INT_TYPE, Type.LONG_TYPE);
            mv.lcmp();
            mv.ifne(switchDefault);

            mv.load(switchValue);
            mv.cast(Type.LONG_TYPE, Type.INT_TYPE);
        } else if (switchValue.getType().equals(Type.DOUBLE_TYPE)) {
            // test for int-ness: value is integer
            mv.load(switchValue);
            mv.dup2();
            mv.cast(Type.DOUBLE_TYPE, Type.INT_TYPE);
            mv.cast(Type.INT_TYPE, Type.DOUBLE_TYPE);
            mv.cmpl(Type.DOUBLE_TYPE);
            mv.ifne(switchDefault);

            mv.load(switchValue);
            mv.cast(Type.DOUBLE_TYPE, Type.INT_TYPE);
        } else {
            assert switchValue.getType().equals(Types.Object);

            // test for int-ness: type is java.lang.Number
            mv.load(switchValue);
            mv.instanceOf(Types.Number);
            mv.ifeq(switchDefault);

            // test for int-ness: value is integer
            Variable<Double> switchValueNum = mv.newVariable("switchValueNum", double.class);
            mv.load(switchValue);
            mv.checkcast(Types.Number);
            mv.invoke(Methods.Number_doubleValue);
            mv.dup2();
            mv.dup2();
            mv.store(switchValueNum);
            mv.cast(Type.DOUBLE_TYPE, Type.INT_TYPE);
            mv.cast(Type.INT_TYPE, Type.DOUBLE_TYPE);
            mv.cmpl(Type.DOUBLE_TYPE);
            mv.ifne(switchDefault);

            mv.load(switchValueNum);
            mv.cast(Type.DOUBLE_TYPE, Type.INT_TYPE);
        }

        // emit tableswitch or lookupswitch
        long[] entries = intSwitchEntries(clauses, defaultClause != null);
        switchInstruction(switchDefault, labels, entries, mv);
    }

    /**
     * Shared implementation for int- and char-switches
     */
    private static void switchInstruction(Label switchDefault, Label[] labels, long[] entries,
            StatementVisitor mv) {
        int entriesLength = entries.length;
        int distinctValues = distinctValues(entries);
        int minValue = Value(entries[0]);
        int maxValue = Value(entries[entriesLength - 1]);
        int range = maxValue - minValue + 1;
        float density = (float) distinctValues / range;
        if (range > 0 && (range <= 5 || density >= 0.5f)) {
            // System.out.printf("tableswitch [%d: %d - %d]\n", entriesLength, minValue, maxValue);
            Label[] switchLabels = new Label[range];
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
            Label[] switchLabels = new Label[distinctValues];
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
                entries[j++] = Entry(((NumericLiteral) expr).getValue().intValue(), i);
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
