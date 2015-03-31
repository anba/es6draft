/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static com.github.anba.es6draft.semantics.StaticSemantics.TailCallNodes;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.github.anba.es6draft.ast.Expression;
import com.github.anba.es6draft.ast.FunctionNode;
import com.github.anba.es6draft.ast.Module;
import com.github.anba.es6draft.ast.ScopedNode;
import com.github.anba.es6draft.ast.Script;
import com.github.anba.es6draft.ast.TopLevelNode;
import com.github.anba.es6draft.ast.scope.Scope;
import com.github.anba.es6draft.compiler.Labels.ReturnLabel;
import com.github.anba.es6draft.compiler.Labels.TempLabel;
import com.github.anba.es6draft.compiler.assembler.Code.MethodCode;
import com.github.anba.es6draft.compiler.assembler.Handle;
import com.github.anba.es6draft.compiler.assembler.Jump;
import com.github.anba.es6draft.compiler.assembler.MethodName;
import com.github.anba.es6draft.compiler.assembler.Type;
import com.github.anba.es6draft.compiler.assembler.Variable;
import com.github.anba.es6draft.compiler.assembler.VariablesSnapshot;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.internal.InlineArrayList;
import com.github.anba.es6draft.runtime.internal.ResumptionPoint;
import com.github.anba.es6draft.runtime.internal.ScriptRuntime;

/**
 * 
 */
abstract class ExpressionVisitor extends InstructionVisitor {
    private static final class Methods {
        // class: ResumptionPoint
        static final MethodName ResumptionPoint_create = MethodName
                .findStatic(Types.ResumptionPoint, "create", Type.methodType(Types.ResumptionPoint,
                        Types.Object_, Types.Object_, Type.INT_TYPE));

        static final MethodName ResumptionPoint_getLocals = MethodName.findVirtual(
                Types.ResumptionPoint, "getLocals", Type.methodType(Types.Object_));

        static final MethodName ResumptionPoint_getOffset = MethodName.findVirtual(
                Types.ResumptionPoint, "getOffset", Type.methodType(Type.INT_TYPE));

        static final MethodName ResumptionPoint_getStack = MethodName.findVirtual(
                Types.ResumptionPoint, "getStack", Type.methodType(Types.Object_));
    }

    private static final int CONTEXT_SLOT = 0;

    private final ExpressionVisitor parent;
    private final TopLevelNode<?> topLevelNode;
    private final boolean strict;
    private final boolean globalCode;
    private final boolean syntheticMethod;
    private int classDefDepth = 0;
    private Variable<ExecutionContext> executionContext;
    private Scope scope;
    // tail-call support
    private boolean hasTailCalls = false;
    private Set<Expression> tailCallNodes = Collections.emptySet();

    protected ExpressionVisitor(MethodCode method, ExpressionVisitor parent) {
        super(method);
        this.parent = parent;
        this.topLevelNode = parent.topLevelNode;
        this.strict = parent.isStrict();
        this.globalCode = parent.isGlobalCode();
        this.syntheticMethod = true;
        this.classDefDepth = parent.classDefDepth;
        this.scope = parent.scope;
        this.tailCallNodes = parent.tailCallNodes;
    }

    protected ExpressionVisitor(MethodCode method, TopLevelNode<?> topLevelNode, boolean strict) {
        super(method);
        this.parent = null;
        this.topLevelNode = topLevelNode;
        this.strict = strict;
        this.globalCode = isGlobalCode(topLevelNode);
        this.syntheticMethod = topLevelNode.hasSyntheticNodes();
    }

    private static boolean isGlobalCode(TopLevelNode<?> node) {
        if (node instanceof Script) {
            return ((Script) node).isGlobalCode();
        }
        if (node instanceof Module) {
            return true;
        }
        assert node instanceof FunctionNode;
        return false;
    }

    @Override
    public void begin() {
        super.begin();
        this.executionContext = getParameter(CONTEXT_SLOT, ExecutionContext.class);
    }

    @Override
    public void end() {
        if (parent != null) {
            parent.hasTailCalls |= hasTailCalls;
        }
        super.end();
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

    final boolean isStrict() {
        return strict || classDefDepth != 0;
    }

    final boolean isGlobalCode() {
        return globalCode;
    }

    final boolean isResumable() {
        return !syntheticMethod;
    }

    /**
     * Returns {@code true} if compiling generator or async function code.
     * 
     * @return {@code true} if generator or async function
     */
    boolean isGeneratorOrAsync() {
        return false;
    }

    void enterClassDefinition() {
        ++classDefDepth;
    }

    void exitClassDefinition() {
        --classDefDepth;
    }

    Scope getScope() {
        return scope;
    }

    void enterScope(ScopedNode node) {
        assert node.getScope().getParent() == this.scope;
        this.scope = node.getScope();
    }

    void enterScope(Scope scope) {
        assert scope.getParent() == this.scope;
        this.scope = scope;
    }

    void exitScope() {
        scope = scope.getParent();
    }

    void enterFunction(FunctionNode node) {
        assert this.scope == null;
        this.scope = node.getScope().lexicalScope();
    }

    void exitFunction() {
        scope = null;
    }

    void enterTailCallPosition(Expression expr) {
        if (isStrict()) {
            // Tail calls are only enabled in strict-mode code [14.6.1 Tail Position, step 2]
            this.tailCallNodes = TailCallNodes(expr);
        }
    }

    void exitTailCallPosition() {
        this.tailCallNodes = Collections.emptySet();
    }

    boolean hasTailCalls() {
        return hasTailCalls;
    }

    boolean isTailCall(Expression expr) {
        boolean isTaillCall = tailCallNodes.contains(expr);
        hasTailCalls |= isTaillCall;
        return isTaillCall;
    }

    Variable<Object> enterIteration() {
        return null;
    }

    List<TempLabel> exitIteration() {
        return Collections.emptyList();
    }

    /**
     * Emit goto instruction to jump to {@code label}'s wrapped target.
     * 
     * @param label
     *            the target instruction
     * @param completion
     *            the variable which holds the current completion value
     */
    void goTo(TempLabel label, Variable<Object> completion) {
        Jump wrapped = label.getWrapped();
        if (wrapped instanceof ReturnLabel) {
            // specialize return label to emit direct return instruction
            load(completion);
            _return();
        } else {
            goTo(wrapped);
        }
    }

    /**
     * Pops the stack's top element and emits a return instruction.
     */
    void returnCompletion() {
        _return();
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
        private InlineArrayList<ExecutionState> shared = null;

        ExecutionState(VariablesSnapshot locals, Type[] stack, Jump instruction, int offset,
                int line) {
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
     * Create a new object to save the current execution state.
     * 
     * @return the current execution state
     */
    private ExecutionState newExecutionState() {
        if (states == null) {
            states = new ArrayList<>();
        }
        VariablesSnapshot locals = getVariablesSnapshot();
        Type[] stack = getStack();
        Jump instruction = new Jump();
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
            MethodType mt = MethodType.methodType(CallSite.class, MethodHandles.Lookup.class,
                    String.class, MethodType.class);
            BOOTSTRAP = MethodName.findStatic(ScriptRuntime.class, "runtimeBootstrap", mt)
                    .toHandle();
        }
    }

    /**
     * Generate prologue code for generator functions.
     * 
     * @param resume
     *            the variable which holds the resumption point
     * @return the generator state
     */
    GeneratorState prologue(Variable<ResumptionPoint> resume) {
        assert isResumable();
        GeneratorState state = new GeneratorState();
        load(resume);
        ifnonnull(state.resumeSwitch);
        mark(state.startBody);
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
        assert isResumable();
        mark(state.resumeSwitch);
        ArrayList<ExecutionState> states = this.states;
        if (states == null) {
            goTo(state.startBody);
        } else {
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
     * Create a new resumption point at the current instruction offset.
     */
    void newResumptionPoint() {
        assert hasStack() && isResumable();
        ExecutionState state = suspend();
        // manually restore stack type information
        restoreStack(state.stack);
        // mark the resumption point for further method execution
        mark(state.instruction);
        // emit line info for debugging
        lineInfo(state.line);
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
        if (RuntimeBootstrap.ENABLED) {
            invokedynamic(RuntimeBootstrap.STACK, Type.methodType(Types.Object_, stack),
                    RuntimeBootstrap.BOOTSTRAP);
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
        assert getStack().length == 1 && getStack()[0].equals(Types.Object_) : Arrays
                .toString(getStack());

        // (2) Save locals
        VariablesSnapshot locals = state.locals;
        if (RuntimeBootstrap.ENABLED) {
            int i = 0;
            Type[] llocals = new Type[locals.getSize()];
            for (Variable<?> v : locals) {
                llocals[i++] = v.getType();
                load(v);
            }
            invokedynamic(RuntimeBootstrap.LOCALS, Type.methodType(Types.Object_, llocals),
                    RuntimeBootstrap.BOOTSTRAP);
        } else {
            int i = 0;
            anewarray(locals.getSize(), Types.Object);
            for (Variable<?> v : locals) {
                // stack: [array] -> [array, index, v]
                dup();
                iconst(i++);
                load(v);
                toBoxed(v.getType());
                astore(Types.Object);
            }
        }

        // stack: [stack, locals] -> [r]
        iconst(state.offset);
        invoke(Methods.ResumptionPoint_create);

        _return();

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
        assert hasStack() && isResumable();
        assert getStackSize() == 0;

        // emit line info for debugging
        lineInfo(state.line);

        // stack: [] -> [r, r]
        load(resume);
        dup();

        // (1) Restore locals
        // stack: [r, r] -> [r, <locals>]
        invoke(Methods.ResumptionPoint_getLocals);
        VariablesSnapshot variables = state.locals;
        // manually restore locals type information
        restoreVariables(variables);
        int index = 0;
        for (Variable<?> v : variables) {
            // stack: [<locals>] -> [<locals>, value]
            dup();
            loadFromArray(index++, v.getType());
            // stack: [r, <locals>, value] -> [r, <locals>]
            store(v);
        }
        // stack: [r, <locals>] -> [r]
        pop();

        assert getStackSize() == 1 && getStack()[0].equals(Types.ResumptionPoint) : Arrays
                .toString(getStack());

        boolean hasSimpleStack = state.stack.length == 1;
        if (!hasSimpleStack) {
            enterVariableScope();
        }

        // stack: [r] -> [r?, r]
        boolean hasShared = state.shared != null;
        Variable<Integer> offset = null;
        if (hasShared) {
            // stack: [r] -> [r, r]
            dup();
            if (!hasSimpleStack) {
                // stack: [r, r] -> [r]
                invoke(Methods.ResumptionPoint_getOffset);
                offset = newVariable("offset", int.class);
                store(offset);
            }
        }

        // (2) Restore stack
        // stack: [r] -> [<stack>]
        invoke(Methods.ResumptionPoint_getStack);
        Type[] stack = state.stack;
        if (hasSimpleStack) {
            // stack: [<stack>] -> [v]
            loadFromArray(0, stack[0]);
        } else {
            Variable<Object[]> stackContent = newVariable("stack", Object[].class);
            // stack: [<stack>] -> []
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
        }

        // stack: [...] -> [..., offset]
        if (hasShared) {
            if (!hasSimpleStack) {
                load(offset);
            } else {
                swap(Types.ResumptionPoint, stack[0]);
                invoke(Methods.ResumptionPoint_getOffset);
            }
        }

        if (!hasSimpleStack) {
            exitVariableScope();
        }

        if (hasShared) {
            assert getStackSize() >= 1
                    && Arrays.equals(stack, Arrays.copyOf(getStack(), getStackSize() - 1)) : String
                    .format("%s != %s", Arrays.toString(stack), Arrays.toString(getStack()));

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
            assert Arrays.equals(stack, getStack()) : String.format("%s != %s",
                    Arrays.toString(stack), Arrays.toString(getStack()));

            goTo(state.instruction);
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
}
