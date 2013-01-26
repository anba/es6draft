/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static com.github.anba.es6draft.semantics.StaticSemantics.BoundNames;

import java.util.Iterator;

import org.objectweb.asm.Label;

import com.github.anba.es6draft.ast.ArrayComprehension;
import com.github.anba.es6draft.ast.ComprehensionFor;
import com.github.anba.es6draft.ast.Node;
import com.github.anba.es6draft.compiler.CodeGenerator.Register;
import com.github.anba.es6draft.compiler.DefaultCodeGenerator.ValType;

/**
 * TODO: current draft [rev. 13] does not specify the runtime semantics for array-comprehensions,
 * therefore the translation from
 * http://wiki.ecmascript.org/doku.php?id=harmony:array_comprehensions is used
 */
class ArrayComprehensionGenerator extends DefaultCodeGenerator<ValType> {
    private final CodeGenerator codegen;

    ArrayComprehensionGenerator(CodeGenerator codegen) {
        this.codegen = codegen;
    }

    @Override
    protected ValType visit(Node node, MethodGenerator value) {
        throw new IllegalStateException();
    }

    @Override
    public ValType visit(ArrayComprehension node, MethodGenerator mv) {
        int result = mv.newVariable(Types.List);
        mv.anew(Types.ArrayList);
        mv.dup();
        mv.invokespecial(Methods.ArrayList_init);
        mv.store(result, Types.List);

        visitArrayComprehension(node, result, node.getList().iterator(), mv);

        mv.load(result, Types.List);
        mv.freeVariable(result);
        mv.load(Register.Realm);
        mv.swap();
        mv.invokestatic(Methods.AbstractOperations_CreateArrayFromList);

        return ValType.Object;
    }

    private void visitArrayComprehension(ArrayComprehension node, int result, MethodGenerator mv) {
        Label l0 = null;
        if (node.getTest() != null) {
            l0 = new Label();
            node.getTest().accept(codegen, mv);
            invokeGetValue(node.getTest(), mv);
            ToBoolean(ValType.Any, mv);
            mv.ifeq(l0);
        }

        node.getExpression().accept(codegen, mv);
        invokeGetValue(node.getExpression(), mv);
        mv.load(result, Types.List);
        mv.swap();
        mv.invokeinterface(Methods.List_add);
        mv.pop();

        if (node.getTest() != null) {
            mv.mark(l0);
        }
    }

    private void visitArrayComprehension(ArrayComprehension comprehension, int result,
            Iterator<ComprehensionFor> iterator, MethodGenerator mv) {
        Label lblContinue = new Label(), lblBreak = new Label();
        Label loopstart = new Label();

        assert iterator.hasNext();
        ComprehensionFor comprehensionFor = iterator.next();

        comprehensionFor.getExpression().accept(codegen, mv);
        invokeGetValue(comprehensionFor.getExpression(), mv);

        // FIXME: translation into for-of per
        // http://wiki.ecmascript.org/doku.php?id=harmony:array_comprehensions means adding
        // additional isUndefinedOrNull() check, but Spidermonkey reports an error in this case!

        mv.dup();
        isUndefinedOrNull(mv);
        mv.ifeq(loopstart);
        mv.pop();
        mv.goTo(lblBreak);
        mv.mark(loopstart);
        mv.load(Register.Realm);
        mv.invokestatic(Methods.ScriptRuntime_iterate);

        int var = mv.newVariable(Types.Iterator);
        mv.store(var, Types.Iterator);

        mv.mark(lblContinue);
        mv.load(var, Types.Iterator);
        mv.invokeinterface(Methods.Iterator_hasNext);
        mv.ifeq(lblBreak);
        mv.load(var, Types.Iterator);
        mv.invokeinterface(Methods.Iterator_next);

        // FIXME: translation into for-of per
        // http://wiki.ecmascript.org/doku.php?id=harmony:array_comprehensions means using a fresh
        // lexical/declarative environment for each inner loop, but Spidermonkey creates a single
        // environment for the whole array comprehension

        // create new declarative lexical environment
        // stack: [nextValue] -> [nextValue, iterEnv]
        newDeclarativeEnvironment(mv);
        {
            // stack: [nextValue, iterEnv] -> [iterEnv, iterEnv, nextValue, envRec]
            mv.dupX1();
            mv.dupX1();
            mv.invokevirtual(Methods.LexicalEnvironment_getEnvRec);

            // stack: [iterEnv, iterEnv, nextValue, envRec] -> [iterEnv, iterEnv, nextValue]
            for (String name : BoundNames(comprehensionFor.getBinding())) {
                mv.dup();
                mv.aconst(name);
                mv.iconst(false);
                mv.invokeinterface(Methods.EnvironmentRecord_createMutableBinding);
            }
            mv.pop();

            // FIXME: spec bug (missing ToObject() call?)

            // stack: [iterEnv, iterEnv, nextValue] -> [iterEnv]
            codegen.BindingInitialisationWithEnvironment(comprehensionFor.getBinding(), mv);
        }
        // stack: [iterEnv] -> []
        pushLexicalEnvironment(mv);

        if (iterator.hasNext()) {
            visitArrayComprehension(comprehension, result, iterator, mv);
        } else {
            visitArrayComprehension(comprehension, result, mv);
        }

        // restore previous lexical environment
        popLexicalEnvironment(mv);

        mv.goTo(lblContinue);
        mv.mark(lblBreak);
        mv.freeVariable(var);
    }
}
