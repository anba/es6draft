/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static com.github.anba.es6draft.semantics.StaticSemantics.BoundNames;
import static com.github.anba.es6draft.semantics.StaticSemantics.IsConstantDeclaration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.github.anba.es6draft.ast.Declaration;
import com.github.anba.es6draft.ast.FunctionDeclaration;
import com.github.anba.es6draft.ast.GeneratorDeclaration;

/**
 * <h1>13 ECMAScript Language: Statements and Declarations</h1><br>
 * <h2>13.1 Block</h2>
 * <ul>
 * <li>13.1.11 Runtime Semantics: Block Declaration Instantiation
 * </ul>
 */
class BlockDeclarationInstantiationGenerator extends DeclarationBindingInstantiationGenerator {
    BlockDeclarationInstantiationGenerator(CodeGenerator codegen) {
        super(codegen);
    }

    /**
     * stack: [env] -> [env]
     */
    void generate(Collection<Declaration> declarations, StatementVisitor mv) {
        /* steps 1-2 */
        List<Declaration> functionsToInitialise = new ArrayList<>();

        // stack: [env] -> [env, envRec]
        mv.dup();
        getEnvironmentRecord(mv);

        /* step 3 */
        for (Declaration d : declarations) {
            for (String dn : BoundNames(d)) {
                mv.dup();
                if (IsConstantDeclaration(d)) {
                    // FIXME: spec bug (CreateImmutableBinding concrete method of `env`)
                    createImmutableBinding(dn, mv);
                } else {
                    // FIXME: spec bug (CreateMutableBinding concrete method of `env`)
                    createMutableBinding(dn, false, mv);
                }
            }
            if (d instanceof FunctionDeclaration || d instanceof GeneratorDeclaration) {
                functionsToInitialise.add(d);
            }
        }

        if (!functionsToInitialise.isEmpty()) {
            // stack: [env, envRec] -> [envRec, env]
            mv.swap();

            /* step 4 */
            for (Declaration f : functionsToInitialise) {
                String fn = BoundName(f);

                // stack: [envRec, env] -> [envRec, env, envRec, env, cx]
                mv.dup2();
                mv.loadExecutionContext();

                // stack: [envRec, env, envRec, env, cx] -> [envRec, env, envRec, fo]
                if (f instanceof FunctionDeclaration) {
                    InstantiateFunctionObject((FunctionDeclaration) f, mv);
                } else {
                    InstantiateGeneratorObject((GeneratorDeclaration) f, mv);
                }

                // stack: [envRec, env, envRec, fo] -> [envRec, env]
                initialiseBinding(fn, mv);
            }

            // stack: [envRec, env] -> [env, envRec]
            mv.swap();
        }

        // stack: [env, envRec] -> [env]
        mv.pop();
    }
}
