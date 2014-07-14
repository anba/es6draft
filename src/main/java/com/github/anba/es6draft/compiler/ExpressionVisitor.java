/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static com.github.anba.es6draft.semantics.StaticSemantics.TailCallNodes;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.Label;
import org.objectweb.asm.Type;

import com.github.anba.es6draft.ast.Expression;
import com.github.anba.es6draft.ast.Node;
import com.github.anba.es6draft.ast.ScopedNode;
import com.github.anba.es6draft.ast.scope.Scope;
import com.github.anba.es6draft.compiler.Code.MethodCode;
import com.github.anba.es6draft.runtime.DeclarativeEnvironmentRecord;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.internal.ResumptionPoint;

/**
 * 
 */
abstract class ExpressionVisitor extends InstructionVisitor {
    private static final class Fields {
        static final FieldDesc Null_NULL = FieldDesc.create(FieldType.Static, Types.Null, "NULL",
                Types.Null);

        static final FieldDesc Undefined_UNDEFINED = FieldDesc.create(FieldType.Static,
                Types.Undefined, "UNDEFINED", Types.Undefined);
    }

    private static final class Methods {
        // class: ResumptionPoint
        static final MethodDesc ResumptionPoint_init = MethodDesc.create(MethodType.Special,
                Types.ResumptionPoint, "<init>",
                Type.getMethodType(Type.VOID_TYPE, Types.Object_, Types.Object_, Type.INT_TYPE));

        static final MethodDesc ResumptionPoint_getLocals = MethodDesc.create(MethodType.Virtual,
                Types.ResumptionPoint, "getLocals", Type.getMethodType(Types.Object_));

        static final MethodDesc ResumptionPoint_getOffset = MethodDesc.create(MethodType.Virtual,
                Types.ResumptionPoint, "getOffset", Type.getMethodType(Type.INT_TYPE));

        static final MethodDesc ResumptionPoint_getStack = MethodDesc.create(MethodType.Virtual,
                Types.ResumptionPoint, "getStack", Type.getMethodType(Types.Object_));
    }

    private static final int CONTEXT_SLOT = 0;

    private final boolean strict;
    private final boolean globalCode;
    private final boolean syntheticMethods;
    private int classDef = 0;
    private Variable<ExecutionContext> executionContext;
    private Scope scope;
    private Map<String, Variable<DeclarativeEnvironmentRecord.Binding>> variables = emptyMap();
    // tail-call support
    private boolean hasTailCalls = false;
    private Set<Expression> tailCallNodes = emptySet();

    protected ExpressionVisitor(MethodCode method, ExpressionVisitor parent) {
        super(method);
        this.strict = parent.isStrict();
        this.globalCode = parent.isGlobalCode();
        this.syntheticMethods = true;
    }

    protected ExpressionVisitor(MethodCode method, boolean strict, boolean globalCode,
            boolean syntheticMethods) {
        super(method);
        this.strict = strict;
        this.globalCode = globalCode;
        this.syntheticMethods = syntheticMethods;
    }

    @Override
    public void begin() {
        super.begin();
        this.executionContext = getParameter(CONTEXT_SLOT, ExecutionContext.class);
    }

    /**
     * Update additional state information after nested {@link ExpressionVisitor} has finished its
     * pass.
     * 
     * @param nested
     *            the nested expression visitor
     */
    void updateInfo(ExpressionVisitor nested) {
        hasTailCalls |= nested.hasTailCalls;
    }

    /**
     * &#x2205; → cx
     */
    void loadExecutionContext() {
        load(executionContext);
    }

    /**
     * &#x2205; → undefined
     */
    void loadUndefined() {
        get(Fields.Undefined_UNDEFINED);
    }

    /**
     * &#x2205; → null
     */
    void loadNull() {
        get(Fields.Null_NULL);
    }

    boolean isStrict() {
        return strict || classDef != 0;
    }

    boolean isGlobalCode() {
        return globalCode;
    }

    boolean hasSyntheticMethods() {
        return syntheticMethods;
    }

    void enterClassDefinition() {
        ++classDef;
    }

    void exitClassDefinition() {
        --classDef;
    }

    Variable<DeclarativeEnvironmentRecord.Binding> getVariable(String name) {
        return variables.get(name);
    }

    void setVariables(Map<String, Variable<DeclarativeEnvironmentRecord.Binding>> variables) {
        this.variables = variables;
    }

    Scope getScope() {
        return scope;
    }

    void setScope(Scope scope) {
        this.scope = scope;
    }

    void enterScope(ScopedNode node) {
        assert node.getScope().getParent() == this.scope;
        this.scope = node.getScope();
    }

    void exitScope() {
        scope = scope.getParent();
    }

    void lineInfo(Node node) {
        lineInfo(node.getBeginLine());
    }

    void enterTailCallPosition(Expression expr) {
        if (isStrict()) {
            // Tail calls are only enabled in strict-mode code [14.6.1 Tail Position, step 2]
            this.tailCallNodes = TailCallNodes(expr);
        }
    }

    final void exitTailCallPosition() {
        this.tailCallNodes = Collections.emptySet();
    }

    final boolean hasTailCalls() {
        return hasTailCalls;
    }

    final boolean isTailCall(Expression expr) {
        boolean isTaillCall = tailCallNodes.contains(expr);
        hasTailCalls |= isTaillCall;
        return isTaillCall;
    }

    /**
     * Current execution state (locals + stack + ip)
     */
    private static final class ExecutionState {
        private final VariablesView locals;
        private final Type[] stack;
        private final Label instruction;
        private final int offset;
        private final int line;

        ExecutionState(VariablesView locals, Type[] stack, Label instruction, int offset, int line) {
            this.locals = locals;
            this.stack = stack;
            this.instruction = instruction;
            this.offset = offset;
            this.line = line;
        }
    }

    /**
     * List of saved execution states
     */
    private List<ExecutionState> states = null;

    /**
     * Create a new object to save the current execution state.
     * 
     * @return the current execution state
     */
    private ExecutionState newExecutionState() {
        if (states == null) {
            states = new ArrayList<>();
        }
        ExecutionState state = new ExecutionState(getVariables(), getStack(), new Label(),
                states.size(), getLastLineNumber());
        states.add(state);
        return state;
    }

    static final class GeneratorState {
        private final Label resumeSwitch = new Label(), start = new Label();
    }

    /**
     * Generate prologue code for generator functions.
     * 
     * @param resume
     *            the variable which holds the resumption point
     * @return the generator state
     */
    GeneratorState prologue(Variable<ResumptionPoint> resume) {
        GeneratorState state = new GeneratorState();
        load(resume);
        ifnonnull(state.resumeSwitch);
        mark(state.start);
        return state;
    }

    /**
     * Generate epilogue code for generator functions.
     * 
     * @param resume
     *            the variable which holds the resumption point
     * @param state
     *            the generator state
     */
    void epilogue(Variable<ResumptionPoint> resume, GeneratorState state) {
        mark(state.resumeSwitch);
        List<ExecutionState> states = this.states;
        if (states == null) {
            goTo(state.start);
        } else {
            assert !states.isEmpty();
            int count = states.size();
            Label[] restore = new Label[count];
            for (int i = 0; i < count; ++i) {
                restore[i] = new Label();
            }
            load(resume);
            invoke(Methods.ResumptionPoint_getOffset);
            tableswitch(0, count - 1, state.start, restore);
            for (int i = 0; i < count; ++i) {
                mark(restore[i]);
                resume(resume, states.get(i));
            }
        }
    }

    /**
     * Create a new resumption point at the current instruction offset.
     */
    void newResumptionPoint() {
        assert hasStack() && !hasSyntheticMethods();
        ExecutionState state = suspend();
        // manually restore stack type information
        restoreStack(state.stack);
        // mark the resumption point for further method execution
        mark(state.instruction);
    }

    /**
     * Suspend: Saves the current stack and locals and emits a return instruction.
     * 
     * @return the current execution state
     */
    private ExecutionState suspend() {
        ExecutionState state = newExecutionState();

        // (1) Save stack
        Type[] stack = state.stack;
        newarray(stack.length, Types.Object);
        for (int i = 0, sp = stack.length - 1; i < stack.length; ++i, --sp) {
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
            iconst(i);
            swap();
            // stack: [array, array, index, boxed(?)] -> [array]
            astore(Types.Object);
        }
        assert getStack().length == 1 && getStack()[0].equals(Types.Object_) : Arrays
                .toString(getStack());

        // (2) Save locals
        VariablesView locals = state.locals;
        newarray(locals.getVariableCount(), Types.Object);
        for (int i = 0, slot = 0; slot != -1; ++i, slot = locals.getNextActiveSlot(slot)) {
            Type t = locals.getVariable(slot);
            // stack: [array] -> [array, index, v]
            dup();
            iconst(i);
            load(slot, t);
            toBoxed(t);
            astore(Types.Object);
        }

        // stack: [stack, locals] -> [r, r, stack, locals]
        anew(Types.ResumptionPoint);
        dup();
        swap2();
        iconst(state.offset);
        invoke(Methods.ResumptionPoint_init);

        areturn();

        return state;
    }

    /**
     * Resume: Restores the locals and stack and jumps to the resumption point.
     * 
     * @param resume
     *            the variable which holds the resumption point
     * @param state
     *            the execution state
     */
    private void resume(Variable<ResumptionPoint> resume, ExecutionState state) {
        assert hasStack() && !hasSyntheticMethods();
        assert getStack().length == 0;

        // emit line info for debugging
        lineInfo(state.line);

        // stack: [] -> [r, r]
        load(resume);
        dup();

        // (1) Restore locals
        // stack: [r, r] -> [r, <locals>]
        invoke(Methods.ResumptionPoint_getLocals);
        VariablesView variables = state.locals;
        // manually restore locals type information
        restoreVariables(variables);
        for (int i = 0, slot = 0; slot != -1; ++i, slot = variables.getNextActiveSlot(slot)) {
            Type t = variables.getVariable(slot);
            // stack: [<locals>] -> [<locals>, value]
            dup();
            aload(i, Types.Object);
            // stack: [<locals>, value] -> [<locals>, value']
            if (t.getSort() < Type.ARRAY) {
                checkcast(getWrapper(t));
                toUnboxed(t);
            } else if (!Types.Object.equals(t)) {
                checkcast(t);
            }
            // stack: [r, <locals>, value'] -> [<locals>]
            store(slot, t);
        }
        // stack: [r, <locals>] -> [r]
        pop();

        assert getStack().length == 1 && getStack()[0].equals(Types.ResumptionPoint) : Arrays
                .toString(getStack());

        // (2) Restore stack
        // stack: [r] -> [<stack>]
        invoke(Methods.ResumptionPoint_getStack);
        Type[] stack = state.stack;
        for (int i = stack.length - 1, sp = 0; i >= 0; --i, ++sp) {
            Type t = stack[sp];
            // stack: [<stack>] -> [<stack>, value]
            dup();
            aload(i, Types.Object);
            // stack: [<stack>, value] -> [<stack>, value']
            if (t.getSort() < Type.ARRAY) {
                checkcast(getWrapper(t));
                toUnboxed(t);
            } else if (!Types.Object.equals(t)) {
                checkcast(t);
            }
            // stack: [<stack>, value'] -> [value', <stack>]
            if (t.getSize() == 1) {
                swap();
            } else {
                swap1_2();
            }
        }
        // stack: [<stack>] -> []
        pop();

        assert Arrays.deepEquals(stack, getStack()) : String.format("%s != %s",
                Arrays.toString(stack), Arrays.toString(getStack()));

        goTo(state.instruction);
    }
}
