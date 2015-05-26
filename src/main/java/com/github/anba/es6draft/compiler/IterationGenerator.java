/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import java.util.List;

import com.github.anba.es6draft.ast.Node;
import com.github.anba.es6draft.compiler.Labels.TempLabel;
import com.github.anba.es6draft.compiler.StatementGenerator.Completion;
import com.github.anba.es6draft.compiler.assembler.Jump;
import com.github.anba.es6draft.compiler.assembler.MethodName;
import com.github.anba.es6draft.compiler.assembler.TryCatchLabel;
import com.github.anba.es6draft.compiler.assembler.Type;
import com.github.anba.es6draft.compiler.assembler.Variable;
import com.github.anba.es6draft.runtime.internal.ReturnValue;
import com.github.anba.es6draft.runtime.internal.ScriptIterator;

/** 
 *
 */
abstract class IterationGenerator<NODE extends Node, VISITOR extends ExpressionVisitor> {
    private static final class Methods {
        // class: AbstractOperations
        static final MethodName AbstractOperations_IteratorClose = MethodName.findStatic(
                Types.AbstractOperations, "IteratorClose", Type.methodType(Type.VOID_TYPE,
                        Types.ExecutionContext, Types.ScriptIterator, Type.BOOLEAN_TYPE));

        // class: ScriptRuntime
        static final MethodName ScriptRuntime_getStackOverflowError = MethodName.findStatic(
                Types.ScriptRuntime, "getStackOverflowError",
                Type.methodType(Types.StackOverflowError, Types.Error));
    }

    private final CodeGenerator codegen;

    IterationGenerator(CodeGenerator codegen) {
        this.codegen = codegen;
    }

    /**
     * Emit code for the iteration body.
     * 
     * @param node
     *            the ast node
     * @param iterator
     *            the iterator variable
     * @param mv
     *            the expression visitor
     * @return the completion value
     */
    protected abstract Completion iterationBody(NODE node, Variable<ScriptIterator<?>> iterator,
            VISITOR mv);

    /**
     * Emit code for the iteration epilogue.
     * 
     * @param node
     *            the ast node
     * @param iterator
     *            the iterator variable
     * @param mv
     *            the expression visitor
     */
    protected void epilogue(NODE node, Variable<ScriptIterator<?>> iterator, VISITOR mv) {
        // Default implementation is empty.
    }

    /**
     * Called before emitting the iteration body.
     * 
     * @param node
     *            the ast node
     * @param mv
     *            the expression visitor
     * @return the temporary completion object variable or {@code null}
     */
    protected abstract Variable<Object> enterIteration(NODE node, VISITOR mv);

    /**
     * Called after emitting the iteration body.
     * 
     * @param node
     *            the ast node
     * @param mv
     *            the expression visitor
     * @return the list of generated labels
     */
    protected abstract List<TempLabel> exitIteration(NODE node, VISITOR mv);

    /**
     * Emit code for the wrapped iteration.
     * 
     * @param node
     *            the ast node
     * @param iterator
     *            the iterator variable
     * @param mv
     *            the expression visitor
     * @return the completion value
     */
    public final Completion generate(NODE node, Variable<ScriptIterator<?>> iterator, VISITOR mv) {
        return generate(node, iterator, null, mv);
    }

    /**
     * Emit code for the wrapped iteration.
     * 
     * @param node
     *            the ast node
     * @param iterator
     *            the iterator variable
     * @param target
     *            the target label
     * @param mv
     *            the expression visitor
     * @return the completion value
     */
    public final Completion generate(NODE node, Variable<ScriptIterator<?>> iterator, Jump target,
            VISITOR mv) {
        TryCatchLabel startIteration = new TryCatchLabel(), endIteration = new TryCatchLabel();
        TryCatchLabel handlerCatch = new TryCatchLabel();
        TryCatchLabel handlerCatchStackOverflow = null;
        if (codegen.isEnabled(Compiler.Option.IterationCatchStackOverflow)) {
            handlerCatchStackOverflow = new TryCatchLabel();
        }
        TryCatchLabel handlerReturn = null;
        if (mv.isGeneratorOrAsync()
                && !(mv.isResumable() && !codegen.isEnabled(Compiler.Option.NoResume))) {
            handlerReturn = new TryCatchLabel();
        }
        boolean hasTarget = target != null;
        if (!hasTarget) {
            target = new Jump();
        }

        mv.enterVariableScope();
        Variable<Object> completion = enterIteration(node, mv);

        // Emit loop body
        mv.mark(startIteration);
        Completion loopBodyResult = iterationBody(node, iterator, mv);
        if (!loopBodyResult.isAbrupt()) {
            mv.goTo(target);
        }
        mv.mark(endIteration);

        // Restore temporary abrupt targets
        List<TempLabel> tempLabels = exitIteration(node, mv);

        // Emit throw handler
        Completion throwResult = emitThrowHandler(node, iterator, handlerCatch,
                handlerCatchStackOverflow, mv);

        // Emit return handler
        Completion returnResult = emitReturnHandler(node, iterator, completion, handlerReturn,
                tempLabels, mv);

        mv.exitVariableScope();
        mv.tryCatch(startIteration, endIteration, handlerCatch, Types.ScriptException);
        if (handlerCatchStackOverflow != null) {
            mv.tryCatch(startIteration, endIteration, handlerCatchStackOverflow, Types.Error);
        }
        if (handlerReturn != null) {
            mv.tryCatch(startIteration, endIteration, handlerReturn, Types.ReturnValue);
        }

        if (!hasTarget) {
            mv.mark(target);
        }
        epilogue(node, iterator, mv);

        if (handlerReturn == null && tempLabels.isEmpty()) {
            // No Return handler installed
            return throwResult.select(loopBodyResult);
        }
        return returnResult.select(throwResult.select(loopBodyResult));
    }

    private Completion emitThrowHandler(Node node, Variable<ScriptIterator<?>> iterator,
            TryCatchLabel handlerCatch, TryCatchLabel handlerCatchStackOverflow,
            ExpressionVisitor mv) {
        mv.enterVariableScope();
        Variable<Throwable> throwable = mv.newVariable("throwable", Throwable.class);

        if (handlerCatchStackOverflow != null) {
            mv.catchHandler(handlerCatchStackOverflow, Types.Error);
            mv.invoke(Methods.ScriptRuntime_getStackOverflowError);
        }
        mv.catchHandler(handlerCatch, Types.ScriptException);
        mv.store(throwable);

        IteratorClose(node, iterator, true, mv);

        mv.load(throwable);
        mv.athrow();
        mv.exitVariableScope();

        return Completion.Throw;
    }

    private Completion emitReturnHandler(Node node, Variable<ScriptIterator<?>> iterator,
            Variable<Object> completion, TryCatchLabel handlerReturn, List<TempLabel> tempLabels,
            ExpressionVisitor mv) {
        // (1) Optional ReturnValue exception handler
        if (handlerReturn != null) {
            mv.enterVariableScope();
            Variable<ReturnValue> returnValue = mv.newVariable("returnValue", ReturnValue.class);
            mv.catchHandler(handlerReturn, Types.ReturnValue);
            mv.store(returnValue);

            IteratorClose(node, iterator, false, mv);

            mv.load(returnValue);
            mv.athrow();
            mv.exitVariableScope();
        }

        // (2) Intercept return instructions
        assert tempLabels.isEmpty() || completion != null;
        for (TempLabel temp : tempLabels) {
            if (temp.isTarget()) {
                mv.mark(temp);

                IteratorClose(node, iterator, false, mv);

                mv.goTo(temp, completion);
            }
        }

        return Completion.Abrupt; // Return or Break
    }

    protected final void IteratorClose(Node node, Variable<ScriptIterator<?>> iterator,
            boolean throwCompletion, ExpressionVisitor mv) {
        mv.loadExecutionContext();
        mv.load(iterator);
        mv.iconst(throwCompletion);
        mv.lineInfo(node);
        mv.invoke(Methods.AbstractOperations_IteratorClose);
    }
}
