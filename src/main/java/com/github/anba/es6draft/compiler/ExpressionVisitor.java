/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static com.github.anba.es6draft.semantics.StaticSemantics.TailCallNodes;
import static java.util.Collections.emptySet;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import com.github.anba.es6draft.ast.Expression;
import com.github.anba.es6draft.ast.Node;
import com.github.anba.es6draft.ast.ScopedNode;
import com.github.anba.es6draft.ast.scope.Scope;
import com.github.anba.es6draft.compiler.assembler.Code.MethodCode;
import com.github.anba.es6draft.compiler.assembler.FieldDesc;
import com.github.anba.es6draft.compiler.assembler.Jump;
import com.github.anba.es6draft.compiler.assembler.MethodDesc;
import com.github.anba.es6draft.compiler.assembler.Variable;
import com.github.anba.es6draft.compiler.assembler.VariablesSnapshot;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.internal.ResumptionPoint;
import com.github.anba.es6draft.runtime.internal.ScriptRuntime;

/**
 * 
 */
abstract class ExpressionVisitor extends InstructionVisitor {
    private static final class Fields {
        static final FieldDesc Null_NULL = FieldDesc.create(FieldDesc.Allocation.Static,
                Types.Null, "NULL", Types.Null);

        static final FieldDesc Undefined_UNDEFINED = FieldDesc.create(FieldDesc.Allocation.Static,
                Types.Undefined, "UNDEFINED", Types.Undefined);
    }

    private static final class Methods {
        // class: ResumptionPoint
        static final MethodDesc ResumptionPoint_create = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.ResumptionPoint, "create", Type.getMethodType(
                        Types.ResumptionPoint, Types.Object_, Types.Object_, Type.INT_TYPE));

        static final MethodDesc ResumptionPoint_getLocals = MethodDesc.create(
                MethodDesc.Invoke.Virtual, Types.ResumptionPoint, "getLocals",
                Type.getMethodType(Types.Object_));

        static final MethodDesc ResumptionPoint_getOffset = MethodDesc.create(
                MethodDesc.Invoke.Virtual, Types.ResumptionPoint, "getOffset",
                Type.getMethodType(Type.INT_TYPE));

        static final MethodDesc ResumptionPoint_getStack = MethodDesc.create(
                MethodDesc.Invoke.Virtual, Types.ResumptionPoint, "getStack",
                Type.getMethodType(Types.Object_));
    }

    private static final int CONTEXT_SLOT = 0;

    private final ExpressionVisitor parent;
    private final boolean strict;
    private final boolean globalCode;
    private final boolean syntheticMethod;
    private int classDefDepth = 0;
    private Variable<ExecutionContext> executionContext;
    private Scope scope;
    // tail-call support
    private boolean hasTailCalls = false;
    private Set<Expression> tailCallNodes = emptySet();

    protected ExpressionVisitor(MethodCode method, ExpressionVisitor parent) {
        super(method);
        this.parent = parent;
        this.strict = parent.isStrict();
        this.globalCode = parent.isGlobalCode();
        this.syntheticMethod = true;
        this.classDefDepth = parent.classDefDepth;
        this.scope = parent.scope;
        this.tailCallNodes = parent.tailCallNodes;
    }

    protected ExpressionVisitor(MethodCode method, boolean strict, boolean globalCode,
            boolean syntheticMethods) {
        super(method);
        this.parent = null;
        this.strict = strict;
        this.globalCode = globalCode;
        this.syntheticMethod = syntheticMethods;
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
     * &#x2205; → undefined
     */
    final void loadUndefined() {
        get(Fields.Undefined_UNDEFINED);
    }

    /**
     * &#x2205; → null
     */
    final void loadNull() {
        get(Fields.Null_NULL);
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

        ExecutionState(VariablesSnapshot locals, Type[] stack, Jump instruction, int offset,
                int line) {
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
    private ArrayList<ExecutionState> states = null;

    /**
     * Create a new object to save the current execution state.
     * 
     * @return the current execution state
     */
    private ExecutionState newExecutionState() {
        if (states == null) {
            states = new ArrayList<>();
        }
        ExecutionState state = new ExecutionState(getVariablesSnapshot(), getStack(), new Jump(),
                states.size(), getLastLineNumber());
        states.add(state);
        return state;
    }

    static final class GeneratorState {
        private final Jump resumeSwitch = new Jump(), startBody = new Jump();
    }

    private static final class RuntimeBootstrap {
        static final boolean ENABLED = false;
        static final Object[] EMPTY_BSM_ARGS = new Object[] {};
        static final String STACK = "rt:stack";
        static final String LOCALS = "rt:locals";

        private static final Handle BOOTSTRAP;
        static {
            java.lang.invoke.MethodType mt = java.lang.invoke.MethodType.methodType(CallSite.class,
                    MethodHandles.Lookup.class, String.class, java.lang.invoke.MethodType.class);
            BOOTSTRAP = new Handle(Opcodes.H_INVOKESTATIC,
                    Type.getInternalName(ScriptRuntime.class), "runtimeBootstrap",
                    mt.toMethodDescriptorString());
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
            assert !states.isEmpty();
            int count = states.size();
            Jump[] restore = new Jump[count];
            for (int i = 0; i < count; ++i) {
                restore[i] = new Jump();
            }
            load(resume);
            invoke(Methods.ResumptionPoint_getOffset);
            tableswitch(0, count - 1, state.startBody, restore);
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
        assert hasStack() && isResumable();
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
        if (RuntimeBootstrap.ENABLED) {
            invokedynamic(RuntimeBootstrap.STACK, Type.getMethodDescriptor(Types.Object_, stack),
                    RuntimeBootstrap.BOOTSTRAP, RuntimeBootstrap.EMPTY_BSM_ARGS);
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
            invokedynamic(RuntimeBootstrap.LOCALS,
                    Type.getMethodDescriptor(Types.Object_, llocals), RuntimeBootstrap.BOOTSTRAP,
                    RuntimeBootstrap.EMPTY_BSM_ARGS);
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
        assert getStack().length == 0;

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

        assert getStack().length == 1 && getStack()[0].equals(Types.ResumptionPoint) : Arrays
                .toString(getStack());

        // (2) Restore stack
        // stack: [r] -> [<stack>]
        invoke(Methods.ResumptionPoint_getStack);
        Type[] stack = state.stack;
        if (stack.length == 1) {
            // stack: [<stack>] -> [v]
            loadFromArray(0, stack[0]);
        } else {
            enterVariableScope();
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
            exitVariableScope();
        }

        assert Arrays.deepEquals(stack, getStack()) : String.format("%s != %s",
                Arrays.toString(stack), Arrays.toString(getStack()));

        goTo(state.instruction);
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
        if (type.getSort() < Type.ARRAY) {
            checkcast(wrapper(type));
            toUnboxed(type);
        } else if (!Types.Object.equals(type)) {
            checkcast(type);
        }
    }
}
