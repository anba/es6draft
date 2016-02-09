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
import com.github.anba.es6draft.compiler.assembler.MutableValue;
import com.github.anba.es6draft.compiler.assembler.TryCatchLabel;
import com.github.anba.es6draft.compiler.assembler.Type;
import com.github.anba.es6draft.compiler.assembler.Value;
import com.github.anba.es6draft.compiler.assembler.Variable;
import com.github.anba.es6draft.runtime.internal.ScriptException;

/** 
 *
 */
abstract class AbstractIterationGenerator<NODE extends Node, ITERATOR> {
    private static final class Methods {
        // class: ScriptRuntime
        static final MethodName ScriptRuntime_stackOverflowError = MethodName.findStatic(Types.ScriptRuntime,
                "stackOverflowError", Type.methodType(Types.StackOverflowError, Types.Error));
    }

    private final CodeGenerator codegen;

    AbstractIterationGenerator(CodeGenerator codegen) {
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
     *            the code visitor
     * @return the completion value
     */
    protected abstract Completion iterationBody(NODE node, Variable<ITERATOR> iterator, CodeVisitor mv);

    /**
     * Emit code for the iteration epilogue.
     * 
     * @param node
     *            the ast node
     * @param iterator
     *            the iterator variable
     * @param mv
     *            the code visitor
     */
    protected void epilogue(NODE node, Variable<ITERATOR> iterator, CodeVisitor mv) {
        // Default implementation is empty.
    }

    /**
     * Called before emitting the iteration body.
     * 
     * @param node
     *            the ast node
     * @param mv
     *            the code visitor
     * @return the temporary completion object variable or {@code null}
     */
    protected abstract MutableValue<Object> enterIteration(NODE node, CodeVisitor mv);

    /**
     * Called after emitting the iteration body.
     * 
     * @param node
     *            the ast node
     * @param mv
     *            the code visitor
     * @return the list of generated labels
     */
    protected abstract List<TempLabel> exitIteration(NODE node, CodeVisitor mv);

    /**
     * Emit code for the wrapped iteration.
     * 
     * @param node
     *            the ast node
     * @param iterator
     *            the iterator variable
     * @param mv
     *            the code visitor
     * @return the completion value
     */
    public final Completion generate(NODE node, Variable<ITERATOR> iterator, CodeVisitor mv) {
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
     *            the code visitor
     * @return the completion value
     */
    public final Completion generate(NODE node, Variable<ITERATOR> iterator, Jump target, CodeVisitor mv) {
        TryCatchLabel startIteration = new TryCatchLabel(), endIteration = new TryCatchLabel();
        TryCatchLabel handlerCatch = new TryCatchLabel();
        TryCatchLabel handlerCatchStackOverflow = null;
        if (codegen.isEnabled(Compiler.Option.IterationCatchStackOverflow)) {
            handlerCatchStackOverflow = new TryCatchLabel();
        }
        boolean hasTarget = target != null;
        if (!hasTarget) {
            target = new Jump();
        }

        mv.enterVariableScope();
        MutableValue<Object> completion = enterIteration(node, mv);

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
        Completion throwResult = emitThrowHandler(node, iterator, handlerCatch, handlerCatchStackOverflow, mv);

        // Emit return handler
        Completion returnResult = emitReturnHandler(node, iterator, completion, tempLabels, mv);

        mv.exitVariableScope();
        mv.tryCatch(startIteration, endIteration, handlerCatch, Types.ScriptException);
        if (handlerCatchStackOverflow != null) {
            mv.tryCatch(startIteration, endIteration, handlerCatchStackOverflow, Types.Error);
        }

        if (!hasTarget) {
            mv.mark(target);
            epilogue(node, iterator, mv);
        }

        if (tempLabels.isEmpty()) {
            // No Return handler installed
            return throwResult.select(loopBodyResult);
        }
        return returnResult.select(throwResult.select(loopBodyResult));
    }

    private Completion emitThrowHandler(NODE node, Variable<ITERATOR> iterator, TryCatchLabel handlerCatch,
            TryCatchLabel handlerCatchStackOverflow, CodeVisitor mv) {
        mv.enterVariableScope();
        Variable<? extends Throwable> throwable;
        if (handlerCatchStackOverflow == null) {
            throwable = mv.newVariable("throwable", ScriptException.class);
        } else {
            throwable = mv.newVariable("throwable", Throwable.class);
        }

        if (handlerCatchStackOverflow != null) {
            mv.catchHandler(handlerCatchStackOverflow, Types.Error);
            mv.invoke(Methods.ScriptRuntime_stackOverflowError);
        }
        mv.catchHandler(handlerCatch, Types.ScriptException);
        mv.store(throwable);

        IteratorClose(node, iterator, throwable, mv);

        mv.load(throwable);
        mv.athrow();
        mv.exitVariableScope();

        return Completion.Throw;
    }

    private Completion emitReturnHandler(NODE node, Variable<ITERATOR> iterator, Value<Object> completion,
            List<TempLabel> tempLabels, CodeVisitor mv) {
        // (1) Intercept return instructions
        assert tempLabels.isEmpty() || completion != null;
        for (TempLabel temp : tempLabels) {
            if (temp.isTarget()) {
                mv.mark(temp);

                IteratorClose(node, iterator, mv);

                mv.goTo(temp, completion);
            }
        }

        return Completion.Abrupt; // Return or Break
    }

    protected abstract void IteratorClose(NODE node, Variable<ITERATOR> iterator,
            Variable<? extends Throwable> throwable, CodeVisitor mv);

    protected abstract void IteratorClose(NODE node, Variable<ITERATOR> iterator, CodeVisitor mv);
}
