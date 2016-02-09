/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static com.github.anba.es6draft.semantics.StaticSemantics.*;

import java.util.List;

import com.github.anba.es6draft.ast.BlockStatement;
import com.github.anba.es6draft.ast.Declaration;
import com.github.anba.es6draft.ast.HoistableDeclaration;
import com.github.anba.es6draft.ast.SwitchStatement;
import com.github.anba.es6draft.ast.scope.Name;
import com.github.anba.es6draft.compiler.assembler.MethodName;
import com.github.anba.es6draft.compiler.assembler.Variable;
import com.github.anba.es6draft.runtime.DeclarativeEnvironmentRecord;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.types.builtins.FunctionObject;

/**
 * <h1>13 ECMAScript Language: Statements and Declarations</h1><br>
 * <h2>13.2 Block</h2>
 * <ul>
 * <li>13.2.14 Runtime Semantics: BlockDeclarationInstantiation( code, env )
 * </ul>
 */
final class BlockDeclarationInstantiationGenerator extends DeclarationBindingInstantiationGenerator {
    private static final int INLINE_LIMIT = 1 << 5;
    private static final int METHOD_LIMIT = 1 << 12;

    BlockDeclarationInstantiationGenerator(CodeGenerator codegen) {
        super(codegen);
    }

    /**
     * stack: [env] {@literal ->} [env]
     * 
     * @param node
     *            the block statement
     * @param mv
     *            the code visitor
     */
    void generate(BlockStatement node, CodeVisitor mv) {
        int declarations = LexicallyDeclaredNames(node).size();
        if (declarations > INLINE_LIMIT) {
            MethodName method = codegen.compile(node, this);

            // stack: [env] -> [env]
            mv.dup();
            mv.loadExecutionContext();
            mv.invoke(method);
        } else {
            generateInline(LexicallyScopedDeclarations(node), mv);
        }
    }

    /**
     * stack: [env] {@literal ->} [env]
     * 
     * @param node
     *            the switch statement
     * @param mv
     *            the code visitor
     */
    void generate(SwitchStatement node, CodeVisitor mv) {
        int declarations = LexicallyDeclaredNames(node).size();
        if (declarations > INLINE_LIMIT) {
            MethodName method = codegen.compile(node, this);

            // stack: [env] -> [env]
            mv.dup();
            mv.loadExecutionContext();
            mv.invoke(method);
        } else {
            generateInline(LexicallyScopedDeclarations(node), mv);
        }
    }

    /**
     * stack: [] {@literal ->} []
     * 
     * @param node
     *            the block statement
     * @param cx
     *            the execution context
     * @param env
     *            the lexical environment
     * @param mv
     *            the instruction visitor
     */
    void generateMethod(BlockStatement node, Variable<ExecutionContext> cx,
            Variable<LexicalEnvironment<DeclarativeEnvironmentRecord>> env, InstructionVisitor mv) {
        List<Declaration> declarations = LexicallyScopedDeclarations(node);
        if (declarations.size() <= METHOD_LIMIT) {
            generate(declarations, cx, env, mv);
        } else {
            for (int i = 0, size = declarations.size(); i < size; i += METHOD_LIMIT) {
                List<Declaration> sublist = declarations.subList(i, Math.min(i + METHOD_LIMIT, size));

                MethodName method = codegen.compile(node, sublist, this);
                mv.load(env);
                mv.load(cx);
                mv.invoke(method);
            }
        }
    }

    /**
     * stack: [] {@literal ->} []
     * 
     * @param node
     *            the switch statement
     * @param cx
     *            the execution context
     * @param env
     *            the lexical environment
     * @param mv
     *            the instruction visitor
     */
    void generateMethod(SwitchStatement node, Variable<ExecutionContext> cx,
            Variable<LexicalEnvironment<DeclarativeEnvironmentRecord>> env, InstructionVisitor mv) {
        List<Declaration> declarations = LexicallyScopedDeclarations(node);
        if (declarations.size() <= METHOD_LIMIT) {
            generate(declarations, cx, env, mv);
        } else {
            for (int i = 0, size = declarations.size(); i < size; i += METHOD_LIMIT) {
                List<Declaration> sublist = declarations.subList(i, Math.min(i + METHOD_LIMIT, size));

                MethodName method = codegen.compile(node, sublist, this);
                mv.load(env);
                mv.load(cx);
                mv.invoke(method);
            }
        }
    }

    /**
     * stack: [] {@literal ->} []
     * 
     * @param declarations
     *            the block declarations
     * @param cx
     *            the execution context
     * @param env
     *            the lexical environment
     * @param mv
     *            the instruction visitor
     */
    void generateMethod(List<Declaration> declarations, Variable<ExecutionContext> cx,
            Variable<LexicalEnvironment<DeclarativeEnvironmentRecord>> env, InstructionVisitor mv) {
        assert declarations.size() <= METHOD_LIMIT : declarations.size();
        generate(declarations, cx, env, mv);
    }

    private void generateInline(List<Declaration> declarations, CodeVisitor mv) {
        mv.enterVariableScope();
        Variable<LexicalEnvironment<DeclarativeEnvironmentRecord>> env = mv.newVariable("env", LexicalEnvironment.class)
                .uncheckedCast();

        // stack: [env] -> []
        mv.store(env);

        generate(declarations, mv.executionContext(), env, mv);

        // stack: [] -> [env]
        mv.load(env);

        mv.exitVariableScope();
    }

    private void generate(List<Declaration> declarations, Variable<ExecutionContext> cx,
            Variable<LexicalEnvironment<DeclarativeEnvironmentRecord>> env, InstructionVisitor mv) {
        Variable<DeclarativeEnvironmentRecord> envRec = mv.newVariable("envRec", DeclarativeEnvironmentRecord.class);
        Variable<FunctionObject> fo = null;

        getEnvironmentRecord(env, envRec, mv);

        /* steps 1-2 */
        for (Declaration d : declarations) {
            if (!(d instanceof HoistableDeclaration)) {
                for (Name dn : BoundNames(d)) {
                    BindingOp<DeclarativeEnvironmentRecord> op = BindingOp.of(envRec, dn);
                    if (IsConstantDeclaration(d)) {
                        op.createImmutableBinding(envRec, dn, true, mv);
                    } else {
                        op.createMutableBinding(envRec, dn, false, mv);
                    }
                }
            } else {
                Name fn = BoundName((HoistableDeclaration) d);
                BindingOp<DeclarativeEnvironmentRecord> op = BindingOp.of(envRec, fn);

                op.createMutableBinding(envRec, fn, false, mv);

                InstantiateFunctionObject(cx, env, d, mv);
                if (fo == null) {
                    fo = mv.newVariable("fo", FunctionObject.class);
                }
                mv.store(fo);

                op.initializeBinding(envRec, fn, fo, mv);
            }
        }
    }
}
