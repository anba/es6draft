/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static com.github.anba.es6draft.semantics.StaticSemantics.IsStrict;
import static com.github.anba.es6draft.semantics.StaticSemantics.TailCallNodes;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.anba.es6draft.ast.*;
import com.github.anba.es6draft.ast.scope.Scope;
import com.github.anba.es6draft.compiler.DefaultCodeGenerator.ValType;
import com.github.anba.es6draft.compiler.Labels.BreakLabel;
import com.github.anba.es6draft.compiler.Labels.ContinueLabel;
import com.github.anba.es6draft.compiler.Labels.ReturnLabel;
import com.github.anba.es6draft.compiler.Labels.TempLabel;
import com.github.anba.es6draft.compiler.StatementGenerator.Completion;
import com.github.anba.es6draft.compiler.assembler.*;
import com.github.anba.es6draft.compiler.assembler.Code.MethodCode;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.internal.InlineArrayList;
import com.github.anba.es6draft.runtime.internal.ResumptionPoint;
import com.github.anba.es6draft.runtime.internal.ScriptRuntime;

/**
 * 
 */
abstract class CodeVisitor extends InstructionVisitor {
    private static final class Methods {
        // class: ResumptionPoint
        static final MethodName ResumptionPoint_create = MethodName.findStatic(Types.ResumptionPoint, "create",
                Type.methodType(Types.ResumptionPoint, Types.Object_, Types.Object_, Type.INT_TYPE));

        static final MethodName ResumptionPoint_createWithNext = MethodName.findStatic(Types.ResumptionPoint, "create",
                Type.methodType(Types.ResumptionPoint, Types.Object_, Types.Object_, Type.INT_TYPE,
                        Types.ResumptionPoint));

        static final MethodName ResumptionPoint_getLocals = MethodName.findVirtual(Types.ResumptionPoint, "getLocals",
                Type.methodType(Types.Object_));

        static final MethodName ResumptionPoint_getOffset = MethodName.findVirtual(Types.ResumptionPoint, "getOffset",
                Type.methodType(Type.INT_TYPE));

        static final MethodName ResumptionPoint_getStack = MethodName.findVirtual(Types.ResumptionPoint, "getStack",
                Type.methodType(Types.Object_));
    }

    private static final class Labels {
        // unlabelled breaks and continues
        final ArrayDeque<Jump> breakTargets = new ArrayDeque<>(4);
        final ArrayDeque<Jump> continueTargets = new ArrayDeque<>(4);

        // labelled breaks and continues
        final HashMap<String, Jump> namedBreakLabels = new HashMap<>(4);
        final HashMap<String, Jump> namedContinueLabels = new HashMap<>(4);

        Jump returnLabel = null;

        // temporary labels in try-catch-finally blocks
        ArrayList<TempLabel> tempLabels = null;

        final Labels parent;
        final MutableValue<Object> completion;

        Labels(Labels parent, MutableValue<Object> completion) {
            this.parent = parent;
            this.completion = completion;
        }

        private TempLabel newTemp(Jump actual) {
            assert actual != null;
            TempLabel temp = new TempLabel(actual);
            if (tempLabels == null) {
                tempLabels = new ArrayList<>(4);
            }
            tempLabels.add(temp);
            return temp;
        }

        private Jump getBreakLabel(String name) {
            return name == null ? breakTargets.peek() : namedBreakLabels.get(name);
        }

        private void setBreakLabel(String name, Jump label) {
            if (name == null) {
                assert breakTargets.isEmpty();
                breakTargets.push(label);
            } else {
                namedBreakLabels.put(name, label);
            }
        }

        private Jump getContinueLabel(String name) {
            return name == null ? continueTargets.peek() : namedContinueLabels.get(name);
        }

        private void setContinueLabel(String name, Jump label) {
            if (name == null) {
                assert continueTargets.isEmpty();
                continueTargets.push(label);
            } else {
                namedContinueLabels.put(name, label);
            }
        }

        Jump returnLabel() {
            Jump lbl = returnLabel;
            if (lbl == null) {
                assert parent != null : "return outside of function";
                lbl = newTemp(parent.returnLabel());
                returnLabel = lbl;
            }
            return lbl;
        }

        Jump breakLabel(String name) {
            Jump label = getBreakLabel(name);
            if (label == null) {
                assert parent != null : "Label not found: " + name;
                label = newTemp(parent.breakLabel(name));
                setBreakLabel(name, label);
            }
            return label;
        }

        Jump continueLabel(String name) {
            Jump label = getContinueLabel(name);
            if (label == null) {
                assert parent != null : "Label not found: " + name;
                label = newTemp(parent.continueLabel(name));
                setContinueLabel(name, label);
            }
            return label;
        }
    }

    private static final int CONTEXT_SLOT = 0;
    private static final int RESUME_SLOT = 1;
    private static final int MIN_RECOVER_SLOT = 2;

    static {
        assert CONTEXT_SLOT < RESUME_SLOT;
        assert RESUME_SLOT < MIN_RECOVER_SLOT;
    }

    private final CodeVisitor parent;
    private final TopLevelNode<?> topLevelNode;
    private final boolean strict;
    private int classDefDepth = 0;
    private Scope scope;
    private Labels labels = new Labels(null, null);

    // tail-call support
    private boolean hasTailCalls = false;
    private int wrapped = 0;
    private Set<Expression> tailCallNodes = Collections.emptySet();

    private Variable<ExecutionContext> executionContext;
    private MutableValue<Object> completionValue;
    private boolean hasCompletion;

    protected CodeVisitor(MethodCode method, CodeVisitor parent) {
        super(method);
        this.parent = parent;
        this.topLevelNode = parent.topLevelNode;
        this.strict = parent.strict;
        this.classDefDepth = parent.classDefDepth;
        this.scope = parent.scope;
        this.wrapped = parent.wrapped;
        this.tailCallNodes = parent.tailCallNodes;
        // no return in script/module code
        this.labels.returnLabel = isFunction() ? new ReturnLabel() : null;
    }

    protected CodeVisitor(MethodCode method, TopLevelNode<?> topLevelNode) {
        super(method);
        this.parent = null;
        this.topLevelNode = topLevelNode;
        this.strict = isStrict(topLevelNode);
        // no return in script/module code
        this.labels.returnLabel = isFunction() ? new ReturnLabel() : null;
    }

    private static boolean isStrict(TopLevelNode<?> node) {
        if (node instanceof Script) {
            return IsStrict((Script) node);
        }
        if (node instanceof Module) {
            return IsStrict((Module) node);
        }
        assert node instanceof FunctionNode;
        return IsStrict((FunctionNode) node);
    }

    @Override
    public void begin() {
        super.begin();
        this.executionContext = getParameter(CONTEXT_SLOT, ExecutionContext.class);
        this.completionValue = createCompletionVariable();
        this.hasCompletion = hasCompletionValue();
        assert !(hasCompletion && completionValue == null);
    }

    @Override
    public void end() {
        if (parent != null) {
            parent.hasTailCalls |= hasTailCalls;
        }
        super.end();
    }

    @Override
    protected final Stack createStack(Variables variables) {
        if (isGeneratorOrAsync()) {
            return new StackImpl(variables);
        }
        return super.createStack(variables);
    }

    /**
     * Returns the completion value variable or {@code null} if completion values are not saved.
     * 
     * @return the completion value variable
     */
    protected MutableValue<Object> createCompletionVariable() {
        return null;
    }

    /**
     * Returns {@code true} if the current method stores statement completion values.
     * 
     * @return {@code true} if statement completion values are stored
     */
    protected boolean hasCompletionValue() {
        return false;
    }

    /**
     * Returns the parent visitor or {@code null} if not present
     * 
     * @return the parent visitor or {@code null}
     */
    protected final CodeVisitor getParent() {
        return parent;
    }

    /**
     * Returns the {@link TopLevelNode} for this visitor.
     * 
     * @return the top level node for this visitor
     */
    final TopLevelNode<?> getTopLevelNode() {
        return topLevelNode;
    }

    /**
     * Returns the execution context.
     * 
     * @return the execution context
     */
    final Variable<ExecutionContext> executionContext() {
        return executionContext;
    }

    /**
     * &#x2205; → cx
     */
    final void loadExecutionContext() {
        load(executionContext);
    }

    /**
     * Returns {@code true} if currently emitting strict-mode code.
     * 
     * @return {@code true} if emitting strict-mode code
     */
    final boolean isStrict() {
        return strict || classDefDepth != 0;
    }

    /**
     * Returns {@code true} if emitting global script or module code.
     * 
     * @return {@code true} if in global script or module code
     */
    final boolean isGlobalCode() {
        if (topLevelNode instanceof Script) {
            return ((Script) topLevelNode).isGlobalCode();
        }
        if (topLevelNode instanceof Module) {
            return true;
        }
        assert topLevelNode instanceof FunctionNode;
        return false;
    }

    /**
     * Returns {@code true} if compiling function code.
     * 
     * @return {@code true} if compiling function code
     */
    final boolean isFunction() {
        return topLevelNode instanceof FunctionNode;
    }

    /**
     * Returns {@code true} if compiling async function code.
     * 
     * @return {@code true} if compiling async function code
     */
    final boolean isAsync() {
        if (topLevelNode instanceof FunctionNode) {
            return ((FunctionNode) topLevelNode).isAsync();
        }
        return false;
    }

    /**
     * Returns {@code true} if compiling generator function code.
     * 
     * @return {@code true} if compiling generator function code
     */
    final boolean isGenerator() {
        if (topLevelNode instanceof FunctionNode) {
            return ((FunctionNode) topLevelNode).isGenerator();
        }
        return false;
    }

    /**
     * Returns {@code true} if compiling generator or async function code.
     * 
     * @return {@code true} if compiling generator or async function code
     */
    final boolean isGeneratorOrAsync() {
        if (topLevelNode instanceof FunctionNode) {
            return ((FunctionNode) topLevelNode).isGenerator() || ((FunctionNode) topLevelNode).isAsync();
        }
        return false;
    }

    /**
     * Enters a class definition.
     */
    final void enterClassDefinition() {
        ++classDefDepth;
    }

    /**
     * Exits a class definition.
     */
    final void exitClassDefinition() {
        --classDefDepth;
    }

    /**
     * Returns <code>true</code> when currently within a wrapped block.
     * 
     * @return <code>true</code> if currently wrapped
     */
    private boolean isWrapped() {
        return wrapped != 0;
    }

    /**
     * Enters a try-catch-finally wrapped block.
     */
    final void enterWrapped() {
        ++wrapped;
    }

    /**
     * Exits a try-catch-finally wrapped block.
     */
    final void exitWrapped() {
        --wrapped;
    }

    /**
     * Returns the current scope.
     * 
     * @return the current scope
     */
    final Scope getScope() {
        return scope;
    }

    /**
     * Convenience method for: {@code enterScope(node.getScope())}.
     * 
     * @param node
     *            the scoped node
     * @see #enterScope(Scope)
     */
    final void enterScope(ScopedNode node) {
        assert node.getScope().getParent() == this.scope;
        this.scope = node.getScope();
    }

    /**
     * Enters a new scope.
     * 
     * @param scope
     *            the new scope
     */
    final void enterScope(Scope scope) {
        assert scope.getParent() == this.scope;
        this.scope = scope;
    }

    /**
     * Exits the current scope.
     */
    final void exitScope() {
        scope = scope.getParent();
    }

    /**
     * Enters a function body code.
     * 
     * @param node
     *            the function node
     */
    final void enterFunction(FunctionNode node) {
        assert this.scope == null;
        this.scope = node.getScope().lexicalScope();
    }

    /**
     * Exits a function body code.
     */
    final void exitFunction() {
        scope = null;
    }

    /**
     * Enters a tail call position.
     * 
     * @param expr
     *            the expression node
     */
    final void enterTailCallPosition(Expression expr) {
        assert isFunction();
        if (isStrict() && !isWrapped() && !isGeneratorOrAsync()) {
            // Tail calls are only enabled in strict-mode code [14.6.1 Tail Position, step 2]
            this.tailCallNodes = TailCallNodes(expr);
        }
    }

    /**
     * Exits a tail call position.
     */
    final void exitTailCallPosition() {
        this.tailCallNodes = Collections.emptySet();
    }

    /**
     * Returns {@code true} if the current method has tail-calls.
     * 
     * @return {@code true} if tail-calls have been emitted
     */
    final boolean hasTailCalls() {
        return hasTailCalls;
    }

    /**
     * Returns {@code true} if the expression is in a tail-call position.
     * 
     * @param expr
     *            the expression node
     * @return {@code true} if in tail-call position
     */
    final boolean isTailCall(Expression expr) {
        boolean isTaillCall = tailCallNodes.contains(expr);
        hasTailCalls |= isTaillCall;
        return isTaillCall;
    }

    /**
     * Emit goto instruction to jump to {@code label}'s wrapped target.
     * 
     * @param label
     *            the target instruction
     * @param completion
     *            the variable which holds the current completion value
     */
    final void goTo(TempLabel label, Value<Object> completion) {
        if (label.isReturn()) {
            // Specialize return label to emit return completion.
            returnCompletion(completion);
        } else {
            goTo(label.getWrapped());
        }
    }

    /**
     * Emits a return instruction.
     */
    final void returnCompletion(Value<? extends Object> returnValue) {
        if (isAbruptRegion()) {
            // If currently enclosed by a finally scoped block, store value in completion register
            // and jump to (temporary) return label.
            store(labels.completion, returnValue);
            goTo(labels.returnLabel());
        } else {
            // Otherwise emit a return instruction.
            load(returnValue);
            returnForCompletion();
        }
    }

    /**
     * Pops the stack's top element and emits a return instruction.
     */
    final void returnCompletion() {
        if (isAbruptRegion()) {
            // If currently enclosed by a finally scoped block, store value in completion register
            // and jump to (temporary) return label.
            store(labels.completion);
            goTo(labels.returnLabel());
        } else {
            // Otherwise emit a return instruction.
            returnForCompletion();
        }
    }

    /**
     * Extension point for subclasses.
     * <p>
     * stack: [value] {@literal ->} []
     */
    protected void returnForCompletion() {
        // Emit direct return instruction.
        _return();
    }

    /**
     * Returns {@code true} when currently within a finally scoped block.
     * 
     * @return {@code true} if currently in a finally block
     */
    private boolean isAbruptRegion() {
        return labels.parent != null;
    }

    private MutableValue<Object> enterAbruptRegion(boolean statementCompletion) {
        assert labels != null;
        MutableValue<Object> completion;
        if (hasCompletion() && !statementCompletion) {
            // Re-use the existing completion variable if the statement doesn't track its completion state.
            completion = completionValue();
        } else {
            completion = newVariable("completion", Object.class);
            loadCompletionValue();
            store(completion);
        }
        labels = new Labels(labels, completion);
        return completion;
    }

    private List<TempLabel> exitAbruptRegion() {
        assert isAbruptRegion();
        ArrayList<TempLabel> tempLabels = labels.tempLabels;
        labels = labels.parent;
        return tempLabels != null ? tempLabels : Collections.<TempLabel> emptyList();
    }

    final MutableValue<Object> completionValue() {
        assert hasCompletion();
        if (isAbruptRegion()) {
            return labels.completion;
        }
        return completionValue;
    }

    /**
     * Returns {@code true} if the current method stores statement completion values.
     * 
     * @return {@code true} if statement completion values are stored
     */
    final boolean hasCompletion() {
        return hasCompletion;
    }

    /**
     * Pushes the completion value onto the stack.
     */
    final void loadCompletionValue() {
        if (hasCompletion()) {
            load(completionValue());
        } else {
            aconst(null);
        }
    }

    /**
     * Pops the stack's top element and stores it into the completion value.
     * 
     * @param type
     *            the value type of top stack value
     */
    final void storeCompletionValue(ValType type) {
        if (hasCompletion()) {
            toBoxed(type);
            store(completionValue());
        } else {
            pop(type);
        }
    }

    /**
     * Stores the variable's value as the new completion value.
     * 
     * @param completionValue
     *            the new completion value
     */
    final void storeCompletionValue(Value<Object> completionValue) {
        if (hasCompletion()) {
            load(completionValue);
            store(completionValue());
        }
    }

    /**
     * Stores {@code undefined} as the new completion value.
     */
    final void storeUndefinedAsCompletionValue() {
        if (hasCompletion()) {
            loadUndefined();
            store(completionValue());
        }
    }

    /**
     * Enters iteration code.
     * 
     * @return the temporary completion value variable or {@code null} if not applicable for the current method
     */
    final MutableValue<Object> enterIteration() {
        if (isGeneratorOrAsync()) {
            return enterAbruptRegion(false);
        }
        return null;
    }

    /**
     * Exits iteration code.
     * 
     * @return the list of generated labels
     */
    final List<TempLabel> exitIteration() {
        if (isGeneratorOrAsync()) {
            return exitAbruptRegion();
        }
        return Collections.emptyList();
    }

    /**
     * Enters an iteration body block.
     * 
     * @param <FORSTATEMENT>
     *            the for-statement node type
     * @param node
     *            the iteration statement
     * @return the temporary completion object variable
     */
    final <FORSTATEMENT extends IterationStatement & ForIterationNode> MutableValue<Object> enterIterationBody(
            FORSTATEMENT node) {
        MutableValue<Object> completion = enterAbruptRegion(false);
        if (node.hasContinue()) {
            // Copy current continue labels from parent to new labelset, so we won't create
            // temp-labels for own continue labels.
            Labels labels = this.labels;
            assert !labels.parent.continueTargets.isEmpty();
            Jump lblContinue = labels.parent.continueTargets.peek();
            assert lblContinue instanceof ContinueLabel;
            labels.continueTargets.push(lblContinue);
            for (String label : node.getLabelSet()) {
                labels.namedContinueLabels.put(label, lblContinue);
            }
        }
        return completion;
    }

    /**
     * Exits an iteration body block.
     * 
     * @param <FORSTATEMENT>
     *            the for-statement node type
     * @param node
     *            the iteration statement
     * @return the list of generated labels
     */
    final <FORSTATEMENT extends IterationStatement & ForIterationNode> List<TempLabel> exitIterationBody(
            FORSTATEMENT node) {
        return exitAbruptRegion();
    }

    /**
     * Enters a finally scoped block.
     * 
     * @param node
     *            the try-statement
     * @return the temporary completion object variable
     */
    final MutableValue<Object> enterFinallyScoped(TryStatement node) {
        return enterAbruptRegion(node.hasCompletionValue());
    }

    /**
     * Exits a finally scoped block.
     * 
     * @return the list of generated labels
     */
    final List<TempLabel> exitFinallyScoped() {
        return exitAbruptRegion();
    }

    /**
     * Starts code generation for {@link IterationStatement} nodes.
     * 
     * @param node
     *            the iteration statement
     * @param lblBreak
     *            the break label for the statement
     * @param lblContinue
     *            the continue label for the statement
     */
    final void enterIteration(IterationStatement node, BreakLabel lblBreak, ContinueLabel lblContinue) {
        boolean hasBreak = node.hasBreak();
        boolean hasContinue = node.hasContinue();
        if (!(hasBreak || hasContinue))
            return;
        Labels labels = this.labels;
        if (hasBreak)
            labels.breakTargets.push(lblBreak);
        if (hasContinue)
            labels.continueTargets.push(lblContinue);
        for (String label : node.getLabelSet()) {
            if (hasBreak)
                labels.namedBreakLabels.put(label, lblBreak);
            if (hasContinue)
                labels.namedContinueLabels.put(label, lblContinue);
        }
    }

    /**
     * Stops code generation for {@link IterationStatement} nodes.
     * 
     * @param node
     *            the iteration statement
     */
    final void exitIteration(IterationStatement node) {
        boolean hasBreak = node.hasBreak();
        boolean hasContinue = node.hasContinue();
        if (!(hasBreak || hasContinue))
            return;
        Labels labels = this.labels;
        if (hasBreak)
            labels.breakTargets.pop();
        if (hasContinue)
            labels.continueTargets.pop();
        for (String label : node.getLabelSet()) {
            if (hasBreak)
                labels.namedBreakLabels.remove(label);
            if (hasContinue)
                labels.namedContinueLabels.remove(label);
        }
    }

    /**
     * Starts code generation for {@link BreakableStatement} nodes.
     * 
     * @param node
     *            the breakable statement
     * @param lblBreak
     *            the break label for the statement
     */
    final void enterBreakable(BreakableStatement node, BreakLabel lblBreak) {
        if (!node.hasBreak())
            return;
        Labels labels = this.labels;
        labels.breakTargets.push(lblBreak);
        for (String label : node.getLabelSet()) {
            labels.namedBreakLabels.put(label, lblBreak);
        }
    }

    /**
     * Stops code generation for {@link BreakableStatement} nodes.
     * 
     * @param node
     *            the breakable statement
     */
    final void exitBreakable(BreakableStatement node) {
        if (!node.hasBreak())
            return;
        Labels labels = this.labels;
        labels.breakTargets.pop();
        for (String label : node.getLabelSet()) {
            labels.namedBreakLabels.remove(label);
        }
    }

    /**
     * Starts code generation for {@link LabelledStatement} nodes.
     * 
     * @param node
     *            the labelled statement
     * @param lblBreak
     *            the break label for the statement
     */
    final void enterLabelled(LabelledStatement node, BreakLabel lblBreak) {
        if (!node.hasBreak())
            return;
        Labels labels = this.labels;
        for (String label : node.getLabelSet()) {
            labels.namedBreakLabels.put(label, lblBreak);
        }
    }

    /**
     * Stops code generation for {@link LabelledStatement} nodes.
     * 
     * @param node
     *            the labelled statement
     */
    final void exitLabelled(LabelledStatement node) {
        if (!node.hasBreak())
            return;
        Labels labels = this.labels;
        for (String label : node.getLabelSet()) {
            labels.namedBreakLabels.remove(label);
        }
    }

    /**
     * Returns the break-label for the given {@link BreakStatement}.
     * 
     * @param node
     *            the break statement
     * @return the label to the target instruction
     */
    final Jump breakLabel(BreakStatement node) {
        String name = node.getLabel();
        return labels.breakLabel(name);
    }

    /**
     * Returns the continue-label for the given {@link ContinueStatement}.
     * 
     * @param node
     *            the continue statement
     * @return the label to the target instruction
     */
    final Jump continueLabel(ContinueStatement node) {
        String name = node.getLabel();
        return labels.continueLabel(name);
    }

    /**
     * Current execution state (locals + stack + ip)
     */
    private static final class ExecutionState {
        private final VariablesSnapshot locals;
        private final Type[] stack;
        private final Jump instruction;
        private final int offset;
        private final int line;
        private InlineArrayList<ExecutionState> shared;

        ExecutionState(VariablesSnapshot locals, Type[] stack, Jump instruction, int offset, int line) {
            this.locals = locals;
            this.stack = stack;
            this.instruction = instruction;
            this.offset = offset;
            this.line = line;
        }

        boolean isCompatible(VariablesSnapshot locals, Type[] stack) {
            return this.locals.equals(locals) && Arrays.equals(this.stack, stack);
        }

        ExecutionState addShared(Jump instruction, int offset, int line) {
            if (shared == null) {
                shared = new InlineArrayList<>();
            }
            ExecutionState state = new ExecutionState(locals, stack, instruction, offset, line);
            shared.add(state);
            return state;
        }
    }

    /**
     * List of saved execution states
     */
    private ArrayList<ExecutionState> states = null;
    private int stateOffsetCounter = 0;

    /**
     * Creates a new object to save the current execution state.
     * 
     * @param instruction
     *            the resume instruction
     * @return the current execution state
     */
    private ExecutionState newExecutionState(Jump instruction) {
        if (states == null) {
            states = new ArrayList<>();
        }
        assert hasParameter(CONTEXT_SLOT, ExecutionContext.class);
        assert hasParameter(RESUME_SLOT, ResumptionPoint.class) || hasParameter(RESUME_SLOT, ResumptionPoint[].class);
        VariablesSnapshot locals = getVariablesSnapshot(MIN_RECOVER_SLOT);
        Type[] stack = getStack();
        int offset = stateOffsetCounter++;
        int line = getLastLineNumber();
        for (ExecutionState state : states) {
            if (state.isCompatible(locals, stack)) {
                return state.addShared(instruction, offset, line);
            }
        }
        ExecutionState state = new ExecutionState(locals, stack, instruction, offset, line);
        states.add(state);
        return state;
    }

    static final class GeneratorState {
        private final Jump resumeSwitch = new Jump(), startBody = new Jump();
    }

    private static final class RuntimeBootstrap {
        static final boolean ENABLED = true;
        static final String STACK = "rt:stack";
        static final String LOCALS = "rt:locals";

        private static final Handle BOOTSTRAP;

        static {
            MethodType mt = MethodType.methodType(CallSite.class, MethodHandles.Lookup.class, String.class,
                    MethodType.class);
            BOOTSTRAP = MethodName.findStatic(ScriptRuntime.class, "runtimeBootstrap", mt).toHandle();
        }
    }

    /**
     * Returns the resumption point mutable value.
     * 
     * @return the resumption point value
     */
    protected MutableValue<ResumptionPoint> resumptionPoint() {
        return getParameter(RESUME_SLOT, ResumptionPoint.class);
    }

    /**
     * Generates prologue code for generator functions.
     * 
     * @return the generator state
     */
    final GeneratorState generatorPrologue() {
        GeneratorState state = new GeneratorState();
        load(resumptionPoint());
        ifnonnull(state.resumeSwitch);
        mark(state.startBody);
        return state;
    }

    /**
     * Generates epilogue code for generator functions.
     * 
     * @param state
     *            the generator state
     */
    final void generatorEpilogue(GeneratorState state) {
        mark(state.resumeSwitch);
        ArrayList<ExecutionState> states = this.states;
        if (states == null) {
            goTo(state.startBody);
        } else {
            MutableValue<ResumptionPoint> resume = resumptionPoint();
            int count = stateOffsetCounter;
            int uniqueCount = states.size();
            assert count > 0 && uniqueCount > 0;
            if (uniqueCount == 1) {
                resume(resume, states.get(0));
            } else {
                Jump[] restore = new Jump[count];
                for (ExecutionState execState : states) {
                    Jump target = new Jump();
                    restore[execState.offset] = target;
                    if (execState.shared != null) {
                        for (ExecutionState shared : execState.shared) {
                            restore[shared.offset] = target;
                        }
                    }
                }
                load(resume);
                invoke(Methods.ResumptionPoint_getOffset);
                tableswitch(0, count - 1, state.startBody, restore);
                for (ExecutionState execState : states) {
                    mark(restore[execState.offset]);
                    resume(resume, execState);
                }
            }
        }
    }

    /**
     * Calls a runtime function.
     * 
     * @param method
     *            the method name
     * @param arguments
     *            the argument values
     */
    final void call(MethodName method, Value<?>... arguments) {
        loadExecutionContext();
        for (Value<?> value : arguments) {
            load(value);
        }
        invoke(method);
    }

    /**
     * Calls a runtime function and stores the return value in {@code result}.
     * 
     * @param method
     *            the method name
     * @param result
     *            the result value
     * @param arguments
     *            the argument values
     */
    final void callWithResult(MethodName method, MutableValue<?> result, Value<?>... arguments) {
        call(method, arguments);
        store(result);
    }

    /**
     * Calls a suspendable runtime function.
     * 
     * @param method
     *            the method name
     * @param arguments
     *            the argument values
     */
    final void callWithSuspend(MethodName method, Value<?>... arguments) {
        enterVariableScope();

        Variable<ResumptionPoint[]> resumeRef = newVariable("resume", ResumptionPoint[].class);
        anewarray(1, Types.ResumptionPoint);
        store(resumeRef);

        Variable<Object[]> completionRef = newVariable("completion", Object[].class);
        anewarray(1, Types.Object);
        store(completionRef);

        Jump resumeLabel = new Jump();
        mark(resumeLabel);

        loadExecutionContext();
        load(resumeRef);
        load(completionRef);
        for (Value<?> value : arguments) {
            load(value);
        }
        invoke(method);

        MutableValue<ResumptionPoint> resume = arrayElement(resumeRef, 0, ResumptionPoint.class);
        Jump noSuspend = new Jump();
        load(resume);
        ifnull(noSuspend);
        {
            pop(method.descriptor.returnType());
            suspend(resumeLabel, resume);
        }
        mark(noSuspend);

        MutableValue<Object> completion = arrayElement(completionRef, 0, Object.class);
        Jump noReturn = new Jump();
        load(completion);
        ifnull(noReturn);
        {
            pop(method.descriptor.returnType());
            // Pop remaining stack entries before returning the completion value.
            popStack();
            returnCompletion(completion);
        }
        mark(noReturn);

        exitVariableScope();
    }

    /**
     * Calls a suspendable runtime function.
     * 
     * @param method
     *            the method name
     * @param result
     *            the result value
     * @param arguments
     *            the argument values
     */
    final void callWithSuspendInt(MethodName method, MutableValue<Integer> result, Value<?>... arguments) {
        enterVariableScope();

        Variable<ResumptionPoint[]> resumeRef = newVariable("resume", ResumptionPoint[].class);
        anewarray(1, Types.ResumptionPoint);
        store(resumeRef);

        Jump resumeLabel = new Jump();
        mark(resumeLabel);

        loadExecutionContext();
        load(resumeRef);
        for (Value<?> value : arguments) {
            load(value);
        }
        invoke(method);
        store(result);

        MutableValue<ResumptionPoint> resume = arrayElement(resumeRef, 0, ResumptionPoint.class);
        Jump noSuspend = new Jump();
        load(result);
        ifge(noSuspend);
        {
            suspend(resumeLabel, resume);
        }
        mark(noSuspend);

        exitVariableScope();
    }

    /**
     * Suspend: Saves the current stack and locals and emits a return instruction.
     * 
     * @param instruction
     *            the resume instruction
     * @param next
     *            the next resumption point
     */
    private void suspend(Jump instruction, Value<ResumptionPoint> next) {
        assert hasStack();
        assert instruction.isResolved();
        assert isEqualStack(instruction, getStack());
        assert next != null;

        // Create a new resumption point.
        ExecutionState state = newExecutionState(instruction);
        createResumptionPoint(state, next);

        returnForSuspend();
    }

    /**
     * Suspend: Saves the current stack and locals and emits a return instruction.
     */
    final void suspend() {
        assert hasStack();

        // Create a new resumption point.
        ExecutionState state = newExecutionState(new Jump());
        createResumptionPoint(state, null);

        returnForSuspend();

        // Manually restore stack type information after return.
        restoreStack(state.stack);
        // Mark the resumption point for further method execution.
        mark(state.instruction);
        // Emit line info for debugging.
        lineInfo(state.line);
    }

    /**
     * Extension point for subclasses.
     * <p>
     * stack: [resumptionPoint] {@literal ->} []
     */
    protected void returnForSuspend() {
        // Emit direct return instruction.
        _return();
    }

    /**
     * Create a new resumption point at the current instruction offset.
     * <p>
     * stack: [] -> [resumptionPoint]
     * 
     * @param state
     *            the current execution state
     * @param next
     *            the next resumption point or {@code null}
     */
    private void createResumptionPoint(ExecutionState state, Value<ResumptionPoint> next) {
        // stack: [...] -> [<stack>]
        saveStack(state);
        assert getStackSize() == 1;
        assert getStack()[0].equals(Types.Object_) : Arrays.toString(getStack());

        // stack: [<stack>] -> [<stack>, <locals>]
        saveLocals(state);

        // stack: [<stack>, <locals>] -> [r]
        iconst(state.offset);
        if (next == null) {
            invoke(Methods.ResumptionPoint_create);
        } else {
            load(next);
            invoke(Methods.ResumptionPoint_createWithNext);
        }
    }

    /**
     * stack: [...] -> [{@literal <stack>}]
     * 
     * @param state
     *            the current execution state
     */
    private void saveStack(ExecutionState state) {
        Type[] stack = state.stack;
        if (RuntimeBootstrap.ENABLED) {
            invokedynamic(RuntimeBootstrap.STACK, Type.methodType(Types.Object_, stack), RuntimeBootstrap.BOOTSTRAP);
        } else {
            anewarray(stack.length, Types.Object);
            for (int sp = stack.length - 1; sp >= 0; --sp) {
                Type t = stack[sp];
                // stack: [?, array] -> [array, array, ?]
                if (t.getSize() == 1) {
                    dupX1();
                    swap();
                } else {
                    dup();
                    swap2();
                }
                // stack: [array, array, ?] -> [array, array, boxed(?)]
                toBoxed(t);
                // stack: [array, array, boxed(?)] -> [array, array, index, boxed(?)]
                iconst(sp);
                swap();
                // stack: [array, array, index, boxed(?)] -> [array]
                astore(Types.Object);
            }
        }
    }

    /**
     * stack: [] -> [{@literal <locals>}]
     * 
     * @param state
     *            the current execution state
     */
    private void saveLocals(ExecutionState state) {
        VariablesSnapshot locals = state.locals;
        int numLocals = locals.getSize();
        if (numLocals > 0) {
            if (RuntimeBootstrap.ENABLED) {
                int i = 0;
                Type[] llocals = new Type[numLocals];
                for (Variable<?> v : locals) {
                    llocals[i++] = v.getType();
                    load(v);
                }
                invokedynamic(RuntimeBootstrap.LOCALS, Type.methodType(Types.Object_, llocals),
                        RuntimeBootstrap.BOOTSTRAP);
            } else {
                int i = 0;
                anewarray(numLocals, Types.Object);
                for (Variable<?> v : locals) {
                    // stack: [array] -> [array, index, v]
                    dup();
                    iconst(i++);
                    load(v);
                    toBoxed(v.getType());
                    astore(Types.Object);
                }
            }
        } else {
            anull();
        }
    }

    /**
     * Resume: Restores the locals and stack and jumps to the resumption point.
     * 
     * @param resume
     *            the variable which holds the resumption point
     * @param state
     *            the execution state
     */
    private void resume(MutableValue<ResumptionPoint> resume, ExecutionState state) {
        assert hasStack();
        assert getStackSize() == 0;

        VariablesSnapshot variables = state.locals;
        boolean hasLocals = variables.getSize() > 0;

        // Emit line info for debugging.
        lineInfo(state.line);

        // (1) Restore locals
        if (hasLocals) {
            // stack: [] -> [<locals>]
            load(resume);
            invoke(Methods.ResumptionPoint_getLocals);
            // manually restore locals type information
            restoreVariables(variables);
            int index = 0;
            for (Variable<?> v : variables) {
                // stack: [<locals>] -> [<locals>, value]
                dup();
                loadFromArray(index++, v.getType());
                // stack: [<locals>, value] -> [<locals>]
                store(v);
            }
            // stack: [<locals>] -> []
            pop();
        }

        // (2) Restore stack
        // stack: [] -> [<stack>]
        assert getStackSize() == 0;
        restoreStack(resume, state);
        assert Arrays.equals(state.stack, getStack()) : String.format("%s != %s", Arrays.toString(state.stack),
                Arrays.toString(getStack()));

        // stack: [...] -> [..., offset]
        boolean hasShared = state.shared != null;
        if (hasShared) {
            load(resume);
            invoke(Methods.ResumptionPoint_getOffset);
        }

        // Clear resume parameter to avoid leaking saved state.
        store(resume, anullValue());

        if (hasShared) {
            // stack: [..., offset] -> [...]
            InlineArrayList<ExecutionState> sharedStates = state.shared;
            int sharedSize = sharedStates.size();
            assert sharedSize > 0;

            if (sharedSize == 1) {
                iconst(state.offset);
                ificmpeq(state.instruction);
                goTo(sharedStates.get(0).instruction);
            } else {
                Jump[] restore = new Jump[sharedSize];
                for (int i = 0; i < sharedSize; ++i) {
                    restore[i] = sharedStates.get(i).instruction;
                }

                int firstOffset = sharedStates.get(0).offset;
                int lastOffset = sharedStates.get(sharedSize - 1).offset;
                boolean consecutiveOffsets = (lastOffset - firstOffset) == (sharedSize - 1);
                if (consecutiveOffsets) {
                    tableswitch(firstOffset, lastOffset, state.instruction, restore);
                } else {
                    int[] keys = new int[sharedSize];
                    for (int i = 0; i < sharedSize; ++i) {
                        keys[i] = sharedStates.get(i).offset;
                    }
                    lookupswitch(state.instruction, keys, restore);
                }
            }
        } else {
            goTo(state.instruction);
        }
    }

    private void restoreStack(Value<ResumptionPoint> resume, ExecutionState state) {
        Type[] stack = state.stack;
        if (stack.length == 0) {
            // Nothing to do.
        } else if (stack.length == 1) {
            // stack: [] -> [v]
            load(resume);
            invoke(Methods.ResumptionPoint_getStack);
            loadFromArray(0, stack[0]);
        } else {
            assert stack.length > 1;
            enterVariableScope();
            Variable<Object[]> stackContent = newVariable("stack", Object[].class);
            load(resume);
            invoke(Methods.ResumptionPoint_getStack);
            store(stackContent);
            for (int sp = 0, len = stack.length; sp < len; ++sp) {
                // stack: [] -> [<stack>]
                load(stackContent);
                // stack: [<stack>] -> [value]
                loadFromArray(sp, stack[sp]);
            }
            // Set 'stackContent' variable to null to avoid leaking the saved stack
            aconst(null);
            store(stackContent);
            exitVariableScope();
        }
    }

    /**
     * stack: [array] {@literal ->} [value]
     * 
     * @param index
     *            array index
     * @param type
     *            array element type
     */
    private void loadFromArray(int index, Type type) {
        aload(index, Types.Object);
        if (type.isPrimitive()) {
            checkcast(wrapper(type));
            toUnboxed(type);
        } else if (!Types.Object.equals(type)) {
            checkcast(type);
        }
    }

    private enum LabelKind {
        Break, Continue, Return
    }

    static final class LabelState {
        final Completion completion;
        private final ArrayList<Map.Entry<LabelKind, String>> labels;

        LabelState(Completion completion, ArrayList<Map.Entry<LabelKind, String>> labels) {
            this.completion = completion;
            this.labels = labels;
        }

        int size() {
            return labels.size();
        }

        Map.Entry<LabelKind, String> get(int index) {
            return labels.get(index);
        }

        boolean hasReturn() {
            return size() > 0 && get(0).getKey() == LabelKind.Return;
        }

        boolean hasTargetInstruction() {
            return size() > 1 || (size() == 1 && !completion.isAbrupt());
        }
    }

    /**
     * Generates prologue code for label functions.
     */
    final void labelPrologue() {
        assert parent != null;
        assert labels.parent == null;

        labels = new Labels(parent.labels, completionValue);
    }

    /**
     * Generates epilogue code for label functions.
     * 
     * @param target
     *            the target array variable
     * @param completion
     *            the completion value
     * @return the label state
     */
    final LabelState labelEpilogue(Completion completion) {
        assert parent != null;
        assert labels.parent != null;

        ArrayList<Map.Entry<LabelKind, String>> usedLabels = new ArrayList<>();
        if (labels.returnLabel != null) {
            labelReturn(labels.returnLabel, usedLabels, LabelKind.Return, null);
        }
        if (!labels.breakTargets.isEmpty()) {
            labelReturn(labels.breakTargets.peek(), usedLabels, LabelKind.Break, null);
        }
        if (!labels.continueTargets.isEmpty()) {
            labelReturn(labels.continueTargets.peek(), usedLabels, LabelKind.Continue, null);
        }
        for (Map.Entry<String, Jump> named : labels.namedBreakLabels.entrySet()) {
            labelReturn(named.getValue(), usedLabels, LabelKind.Break, named.getKey());
        }
        for (Map.Entry<String, Jump> named : labels.namedContinueLabels.entrySet()) {
            labelReturn(named.getValue(), usedLabels, LabelKind.Continue, named.getKey());
        }
        assert usedLabels.isEmpty()
                || (labels.tempLabels != null && usedLabels.size() <= labels.tempLabels.size()) : String
                        .format("%s != %s", usedLabels, labels.tempLabels);
        return new LabelState(completion, usedLabels);
    }

    private void labelReturn(Jump label, ArrayList<Map.Entry<LabelKind, String>> labels, LabelKind kind, String name) {
        if (label.isTarget()) {
            labels.add(new SimpleImmutableEntry<>(kind, name));

            mark(label);
            iconst(labels.size());
            _return();
        }
    }

    /**
     * Generates the label switch code.
     * 
     * @param labelState
     *            the label state
     * @param target
     *            the target array variable
     * @param completion
     *            the completion value
     * @param expression
     *            if {@code true} emits the label switch at expression level
     */
    final void labelSwitch(LabelState labelState, Value<Integer> target, Value<Object> completion, boolean expression) {
        assert hasStack();
        if (expression) {
            labelSwitchExpr(labelState, target, completion);
        } else {
            labelSwitchStmt(labelState, target, completion);
        }
    }

    private void labelSwitchStmt(LabelState labelState, Value<Integer> target, Value<Object> returnValue) {
        if (labelState.size() == 0) {
            if (labelState.completion.isAbrupt()) {
                // Unreachable code, create 'throw' completion for bytecode verifier.
                unreachable();
            }
        } else if (labelState.size() == 1) {
            Map.Entry<LabelKind, String> entry = labelState.get(0);
            if (entry.getKey() == LabelKind.Return) {
                if (labelState.completion.isAbrupt()) {
                    returnCompletion(returnValue);
                } else {
                    Jump noReturn = new Jump();

                    load(target);
                    ifeq(noReturn);
                    {
                        returnCompletion(returnValue);
                    }
                    mark(noReturn);
                }
            } else {
                Jump targetInstr = targetInstruction(entry);

                if (labelState.completion.isAbrupt()) {
                    goTo(targetInstr);
                } else {
                    load(target);
                    ifne(targetInstr);
                }
            }
        } else if (labelState.size() > 1) {
            emitLabelSwitch(labelState, target, returnValue);
            if (labelState.completion.isAbrupt()) {
                // Unreachable code, create 'throw' completion for bytecode verifier.
                unreachable();
            }
        }
    }

    private void labelSwitchExpr(LabelState labelState, Value<Integer> target, Value<Object> returnValue) {
        assert hasStack();
        if (labelState.size() == 1) {
            popStack(target, () -> {
                Map.Entry<LabelKind, String> entry = labelState.get(0);
                if (entry.getKey() == LabelKind.Return) {
                    returnCompletion(returnValue);
                } else {
                    Jump targetInstr = targetInstruction(entry);
                    goTo(targetInstr);
                }
            });
        } else if (labelState.size() > 1) {
            popStack(target, () -> {
                emitLabelSwitch(labelState, target, returnValue);
                // Unreachable code, create 'throw' completion for bytecode verifier.
                unreachable();
            });
        }
    }

    private Jump targetInstruction(Map.Entry<LabelKind, String> entry) {
        assert entry.getKey() != LabelKind.Return;
        if (entry.getKey() == LabelKind.Break) {
            return labels.breakLabel(entry.getValue());
        }
        return labels.continueLabel(entry.getValue());
    }

    private void emitLabelSwitch(LabelState labelState, Value<Integer> target, Value<Object> returnValue) {
        Jump defaultInstr = new Jump();
        Jump returnInstr = null;
        Jump[] targetInstrs = new Jump[labelState.size()];
        for (int i = 0; i < labelState.size(); ++i) {
            Map.Entry<LabelKind, String> entry = labelState.get(i);
            if (entry.getKey() == LabelKind.Return) {
                assert returnInstr == null;
                targetInstrs[i] = returnInstr = new Jump();
            } else {
                targetInstrs[i] = targetInstruction(entry);
            }
        }
        load(target);
        tableswitch(1, labelState.size(), defaultInstr, targetInstrs);
        if (returnInstr != null) {
            mark(returnInstr);
            returnCompletion(returnValue);
        }
        mark(defaultInstr);
    }

    private void unreachable() {
        anull();
        athrow();
    }

    @FunctionalInterface
    private interface Action {
        void perform();
    }

    private void popStack(Value<Integer> target, Action then) {
        Jump noAbrupt = new Jump();

        load(target);
        ifeq(noAbrupt);
        {
            popStack();
            then.perform();
        }
        mark(noAbrupt);
    }
}
