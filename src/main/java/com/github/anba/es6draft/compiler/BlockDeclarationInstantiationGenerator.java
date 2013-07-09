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

import org.objectweb.asm.Type;

import com.github.anba.es6draft.ast.Declaration;
import com.github.anba.es6draft.ast.FunctionDeclaration;
import com.github.anba.es6draft.ast.GeneratorDeclaration;
import com.github.anba.es6draft.compiler.InstructionVisitor.MethodDesc;
import com.github.anba.es6draft.compiler.InstructionVisitor.MethodType;

/**
 * <h1>10 Executable Code and Execution Contexts</h1><br>
 * <h2>10.5 Declaration Binding Instantiation</h2>
 * <ul>
 * <li>10.5.4 Block Declaration Instantiation
 * </ul>
 */
class BlockDeclarationInstantiationGenerator extends DeclarationBindingInstantiationGenerator {
    private static class Methods {
        // class: LexicalEnvironment
        static final MethodDesc LexicalEnvironment_getEnvRec = MethodDesc.create(
                MethodType.Virtual, Types.LexicalEnvironment, "getEnvRec",
                Type.getMethodType(Types.EnvironmentRecord));
    }

    BlockDeclarationInstantiationGenerator(CodeGenerator codegen) {
        super(codegen);
    }

    /**
     * stack: [env] -> [env]
     */
    void generate(Collection<Declaration> declarations, StatementVisitor mv) {
        /* steps 1-2 */
        List<Declaration> functionsToInitialize = new ArrayList<>();

        // stack: [env] -> [env, envRec]
        mv.dup();
        mv.invoke(Methods.LexicalEnvironment_getEnvRec);

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
                functionsToInitialize.add(d);
            }
        }

        if (!functionsToInitialize.isEmpty()) {
            // stack: [env, envRec] -> [envRec, env]
            mv.swap();

            /* step 4 */
            for (Declaration f : functionsToInitialize) {
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

            // stack: [envRec, env] -> [env]
            mv.swap();
        }

        mv.pop();
    }
}
