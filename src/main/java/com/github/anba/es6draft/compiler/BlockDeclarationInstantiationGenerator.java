/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static com.github.anba.es6draft.semantics.StaticSemantics.BoundNames;
import static com.github.anba.es6draft.semantics.StaticSemantics.IsConstantDeclaration;
import static com.github.anba.es6draft.semantics.StaticSemantics.LexicallyDeclaredNames;
import static com.github.anba.es6draft.semantics.StaticSemantics.LexicallyScopedDeclarations;

import java.util.List;

import com.github.anba.es6draft.ast.BlockStatement;
import com.github.anba.es6draft.ast.Declaration;
import com.github.anba.es6draft.ast.SwitchStatement;
import com.github.anba.es6draft.compiler.InstructionVisitor.Variable;
import com.github.anba.es6draft.runtime.EnvironmentRecord;
import com.github.anba.es6draft.runtime.LexicalEnvironment;

/**
 * <h1>13 ECMAScript Language: Statements and Declarations</h1><br>
 * <h2>13.1 Block</h2>
 * <ul>
 * <li>13.1.11 Runtime Semantics: Block Declaration Instantiation
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
        Variable<EnvironmentRecord> envRec = mv.newScratchVariable(EnvironmentRecord.class);
        Variable<LexicalEnvironment<?>> env = mv.newScratchVariable(LexicalEnvironment.class)
                .uncheckedCast();

        // stack: [env] -> [env, envRec]
        mv.dup();
        getEnvironmentRecord(mv);

        // stack: [env, envRec] -> []
        mv.store(envRec);
        mv.store(env);

        /* steps 1-3 */
        for (Declaration d : declarations) {
            if (isFunctionDeclaration(d)) {
                String fn = BoundName(d);

                // FIXME: spec bug - CreateMutableBinding not called (bug 3029)
                mv.load(envRec);
                createMutableBinding(fn, false, mv);

                // stack: [] -> [envRec, env, cx]
                mv.load(envRec);
                mv.load(env);
                mv.loadExecutionContext();

                // stack: [envRec, env, cx] -> [envRec, fo]
                InstantiateFunctionObject(d, mv);

                // stack: [envRec, fo] -> []
                initializeBinding(fn, mv);
            } else {
                for (String dn : BoundNames(d)) {
                    mv.load(envRec);
                    if (IsConstantDeclaration(d)) {
                        // FIXME: spec bug (CreateImmutableBinding concrete method of `env`)
                        createImmutableBinding(dn, mv);
                    } else {
                        // FIXME: spec bug (CreateMutableBinding concrete method of `env`)
                        createMutableBinding(dn, false, mv);
                    }
                }
            }
        }

        // stack: [] -> [env]
        mv.load(env);

        mv.freeVariable(env);
        mv.freeVariable(envRec);
    }
}
