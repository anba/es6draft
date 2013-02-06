/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.interpreter;

import static com.github.anba.es6draft.runtime.internal.Errors.throwSyntaxError;
import static com.github.anba.es6draft.runtime.internal.Errors.throwTypeError;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.semantics.StaticSemantics.BoundNames;
import static com.github.anba.es6draft.semantics.StaticSemantics.VarDeclaredNames;
import static com.github.anba.es6draft.semantics.StaticSemantics.VarScopedDeclarations;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.github.anba.es6draft.ast.Script;
import com.github.anba.es6draft.ast.StatementListItem;
import com.github.anba.es6draft.runtime.EnvironmentRecord;
import com.github.anba.es6draft.runtime.GlobalEnvironmentRecord;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Messages;

/**
 * <h1>10 Executable Code and Execution Contexts</h1><br>
 * <h2>10.5 Declaration Binding Instantiation</h2>
 * <ul>
 * <li>10.5.1 Global Declaration Instantiation
 * <li>10.5.5 Eval Declaration Instantiation
 * </ul>
 */
final class DeclarationBindingInstantiation {
    private DeclarationBindingInstantiation() {
    }

    /**
     * [10.5.1 Global Declaration Instantiation]
     */
    public static void GlobalDeclarationInstantiation(Realm realm, LexicalEnvironment globalEnv,
            Script script, boolean deletableBindings) {
        LexicalEnvironment env = globalEnv;
        GlobalEnvironmentRecord envRec = (GlobalEnvironmentRecord) env.getEnvRec();

        for (String name : VarDeclaredNames(script)) {
            if (envRec.hasLexicalDeclaration(name)) {
                throw throwSyntaxError(realm, Messages.Key.VariableRedeclaration, name);
            }
        }
        List<StatementListItem> varDeclarations = VarScopedDeclarations(script);
        Set<String> declaredVarNames = new HashSet<>();
        for (StatementListItem d : varDeclarations) {
            for (String vn : BoundNames(d)) {
                boolean vnDefinable = envRec.canDeclareGlobalVar(vn);
                if (!vnDefinable) {
                    throw throwTypeError(realm, Messages.Key.InvalidDeclaration, vn);
                }
                if (!declaredVarNames.contains(vn)) {
                    declaredVarNames.add(vn);
                }
            }
        }
        for (String vn : declaredVarNames) {
            envRec.createGlobalVarBinding(vn, deletableBindings);
        }
        return;
    }

    /**
     * [10.5.5 Eval Declaration Instantiation]
     */
    public static void EvalDeclarationInstantiation(Realm realm, LexicalEnvironment lexEnv,
            LexicalEnvironment varEnv, Script script, boolean deletableBindings) {
        // FIXME: spec incomplete (using modified ES5.1 algorithm for now...)

        LexicalEnvironment env = varEnv;
        EnvironmentRecord envRec = env.getEnvRec();
        boolean strict = script.isStrict();
        for (StatementListItem d : VarScopedDeclarations(script)) {
            for (String dn : BoundNames(d)) {
                boolean varAlreadyDeclared = envRec.hasBinding(dn);
                if (!varAlreadyDeclared) {
                    envRec.createMutableBinding(dn, deletableBindings);
                    envRec.setMutableBinding(dn, UNDEFINED, strict);
                }
            }
        }
    }
}
