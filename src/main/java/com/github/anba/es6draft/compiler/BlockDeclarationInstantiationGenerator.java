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
import com.github.anba.es6draft.compiler.assembler.Variable;
import com.github.anba.es6draft.runtime.DeclarativeEnvironmentRecord;
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

    BlockDeclarationInstantiationGenerator(CodeGenerator codegen) {
        super(codegen);
    }

    /**
     * stack: [env] {@literal ->} [env]
     * 
     * @param node
     *            the block statement
     * @param mv
     *            the statement visitor
     */
    void generate(BlockStatement node, StatementVisitor mv) {
        int declarations = LexicallyDeclaredNames(node).size();
        if (declarations > INLINE_LIMIT) {
            codegen.compile(node, mv, this);

            mv.loadExecutionContext();
            mv.swap();
            mv.invoke(codegen.methodDesc(node));
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
     *            the statement visitor
     */
    void generate(SwitchStatement node, StatementVisitor mv) {
        int declarations = LexicallyDeclaredNames(node).size();
        if (declarations > INLINE_LIMIT) {
            codegen.compile(node, mv, this);

            mv.loadExecutionContext();
            mv.swap();
            mv.invoke(codegen.methodDesc(node));
        } else {
            generateInline(LexicallyScopedDeclarations(node), mv);
        }
    }

    /**
     * stack: [env] {@literal ->} [env]
     * 
     * @param node
     *            the block statement
     * @param mv
     *            the expression visitor
     */
    void generateMethod(BlockStatement node, ExpressionVisitor mv) {
        // TODO: split into multiple methods if there are still too many declarations
        generateInline(LexicallyScopedDeclarations(node), mv);
    }

    /**
     * stack: [env] {@literal ->} [env]
     * 
     * @param node
     *            the switch statement
     * @param mv
     *            the expression visitor
     */
    void generateMethod(SwitchStatement node, ExpressionVisitor mv) {
        // TODO: split into multiple methods if there are still too many declarations
        generateInline(LexicallyScopedDeclarations(node), mv);
    }

    /**
     * stack: [env] {@literal ->} [env]
     * 
     * @param declarations
     *            the block scoped declarations
     * @param mv
     *            the expression visitor
     */
    private void generateInline(List<Declaration> declarations, ExpressionVisitor mv) {
        mv.enterVariableScope();
        Variable<LexicalEnvironment<DeclarativeEnvironmentRecord>> env = mv.newVariable("env",
                LexicalEnvironment.class).uncheckedCast();
        Variable<DeclarativeEnvironmentRecord> envRec = mv.newVariable("envRec",
                DeclarativeEnvironmentRecord.class);
        Variable<FunctionObject> fo = null;

        // stack: [env] -> []
        mv.store(env);
        getEnvironmentRecord(env, envRec, mv);

        /* steps 1-2 */
        for (Declaration d : declarations) {
            if (!(d instanceof HoistableDeclaration)) {
                for (Name dn : BoundNames(d)) {
                    BindingOp<DeclarativeEnvironmentRecord> op = BindingOp.of(envRec, dn);
                    if (IsConstantDeclaration(d)) {
                        // FIXME: spec bug (CreateImmutableBinding concrete method of `env`)
                        op.createImmutableBinding(envRec, dn, true, mv);
                    } else {
                        // FIXME: spec bug (CreateMutableBinding concrete method of `env`)
                        op.createMutableBinding(envRec, dn, false, mv);
                    }
                }
            } else {
                Name fn = BoundName((HoistableDeclaration) d);
                BindingOp<DeclarativeEnvironmentRecord> op = BindingOp.of(envRec, fn);

                op.createMutableBinding(envRec, fn, false, mv);

                InstantiateFunctionObject(mv.executionContext(), env, d, mv);
                if (fo == null) {
                    fo = mv.newVariable("fo", FunctionObject.class);
                }
                mv.store(fo);

                op.initializeBinding(envRec, fn, fo, mv);
            }
        }

        // stack: [] -> [env]
        mv.load(env);

        mv.exitVariableScope();
    }
}
