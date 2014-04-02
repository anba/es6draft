/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.interpreter;

import static com.github.anba.es6draft.runtime.internal.Errors.newSyntaxError;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.semantics.StaticSemantics.BoundNames;
import static com.github.anba.es6draft.semantics.StaticSemantics.VarDeclaredNames;
import static com.github.anba.es6draft.semantics.StaticSemantics.VarScopedDeclarations;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.github.anba.es6draft.ast.Script;
import com.github.anba.es6draft.ast.StatementListItem;
import com.github.anba.es6draft.ast.VariableStatement;
import com.github.anba.es6draft.runtime.EnvironmentRecord;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.GlobalEnvironmentRecord;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.internal.Messages;

/**
 * <h1>Declaration Binding Instantiation</h1>
 * <ul>
 * <li>15.2.8 Runtime Semantics: GlobalDeclarationInstantiation
 * <li>18.2.1.2 Eval Declaration Instantiation
 * </ul>
 */
final class DeclarationBindingInstantiation {
    private DeclarationBindingInstantiation() {
    }

    /**
     * [15.2.8 Runtime Semantics: GlobalDeclarationInstantiation]
     */
    public static void GlobalDeclarationInstantiation(ExecutionContext cx, Script script,
            LexicalEnvironment<GlobalEnvironmentRecord> globalEnv,
            LexicalEnvironment<?> lexicalEnv, boolean deletableBindings) {
        LexicalEnvironment<GlobalEnvironmentRecord> env = globalEnv;
        GlobalEnvironmentRecord envRec = env.getEnvRec();

        for (String name : VarDeclaredNames(script)) {
            if (envRec.hasLexicalDeclaration(name)) {
                throw newSyntaxError(cx, Messages.Key.VariableRedeclaration, name);
            }
        }
        List<StatementListItem> varDeclarations = VarScopedDeclarations(script);
        Set<String> declaredVarNames = new HashSet<>();
        for (StatementListItem d : varDeclarations) {
            assert d instanceof VariableStatement;
            for (String vn : BoundNames((VariableStatement) d)) {
                boolean vnDefinable = envRec.canDeclareGlobalVar(vn);
                if (!vnDefinable) {
                    throw newTypeError(cx, Messages.Key.InvalidDeclaration, vn);
                }
                if (!declaredVarNames.contains(vn)) {
                    declaredVarNames.add(vn);
                }
            }
        }
        for (String vn : declaredVarNames) {
            envRec.createGlobalVarBinding(vn, deletableBindings);
        }
    }

    /**
     * [18.2.1.2 Eval Declaration Instantiation]
     */
    public static void EvalDeclarationInstantiation(ExecutionContext cx, Script script,
            LexicalEnvironment<?> varEnv, LexicalEnvironment<?> lexEnv, boolean deletableBindings) {
        // FIXME: spec incomplete (using modified ES5.1 algorithm for now...)

        LexicalEnvironment<?> env = varEnv;
        EnvironmentRecord envRec = env.getEnvRec();
        // boolean strict = script.isStrict();
        for (StatementListItem d : VarScopedDeclarations(script)) {
            assert d instanceof VariableStatement;
            for (String dn : BoundNames((VariableStatement) d)) {
                boolean varAlreadyDeclared = envRec.hasBinding(dn);
                if (!varAlreadyDeclared) {
                    envRec.createMutableBinding(dn, deletableBindings);
                    // envRec.setMutableBinding(dn, UNDEFINED, strict);
                    envRec.initialiseBinding(dn, UNDEFINED);
                }
            }
        }
    }
}
