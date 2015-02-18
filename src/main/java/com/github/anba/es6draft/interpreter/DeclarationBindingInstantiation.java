/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.interpreter;

import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.semantics.StaticSemantics.*;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.github.anba.es6draft.ast.Script;
import com.github.anba.es6draft.ast.StatementListItem;
import com.github.anba.es6draft.ast.VariableStatement;
import com.github.anba.es6draft.ast.scope.Name;
import com.github.anba.es6draft.runtime.DeclarativeEnvironmentRecord;
import com.github.anba.es6draft.runtime.EnvironmentRecord;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.GlobalEnvironmentRecord;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.internal.ScriptRuntime;

/**
 * <h1>Declaration Binding Instantiation</h1>
 * <ul>
 * <li>15.1.8 Runtime Semantics: GlobalDeclarationInstantiation (script, env)
 * <li>18.2.1.2 Runtime Semantics: EvalDeclarationInstantiation (body, varEnv, lexEnv, strict)
 * </ul>
 */
final class DeclarationBindingInstantiation {
    private DeclarationBindingInstantiation() {
    }

    /**
     * 15.1.8 Runtime Semantics: GlobalDeclarationInstantiation (script, env)
     * 
     * @param cx
     *            the execution context
     * @param script
     *            the global script to instantiate
     * @param globalEnv
     *            the global environment
     */
    public static void GlobalDeclarationInstantiation(ExecutionContext cx, Script script,
            LexicalEnvironment<GlobalEnvironmentRecord> env) {
        /* step 1 */
        @SuppressWarnings("unused")
        boolean strict = script.isStrict();
        /* steps 2-3 */
        GlobalEnvironmentRecord envRec = env.getEnvRec();
        /* step 4 (not applicable) */
        /* step 5 */
        Set<Name> varNames = VarDeclaredNames(script);
        /* step 6 (not applicable) */
        /* step 7 */
        for (Name name : varNames) {
            ScriptRuntime.canDeclareVarScopedOrThrow(cx, envRec, name.getIdentifier());
        }
        /* step 8 */
        List<StatementListItem> varDeclarations = VarScopedDeclarations(script);
        /* steps 9-11 (not applicable) */
        /* step 12 */
        LinkedHashSet<Name> declaredVarNames = new LinkedHashSet<>();
        /* step 13 */
        for (StatementListItem d : varDeclarations) {
            assert d instanceof VariableStatement;
            for (Name vn : BoundNames((VariableStatement) d)) {
                ScriptRuntime.canDeclareGlobalVarOrThrow(cx, envRec, vn.getIdentifier());
                declaredVarNames.add(vn);
            }
        }
        /* step 14 (NOTE) */
        /* step 15-17 (not applicable) */
        /* step 18 */
        for (Name vn : declaredVarNames) {
            envRec.createGlobalVarBinding(vn.getIdentifier(), false);
        }
        /* step 19 (return) */
    }

    /**
     * 18.2.1.2 Runtime Semantics: EvalDeclarationInstantiation (body, varEnv, lexEnv, strict)
     * 
     * @param cx
     *            the execution context
     * @param script
     *            the global script to instantiate
     * @param varEnv
     *            the current variable environment
     * @param lexEnv
     *            the current lexical environment
     * @param deletableBindings
     *            the deletable flag for bindings
     */
    public static void EvalDeclarationInstantiation(ExecutionContext cx, Script script,
            LexicalEnvironment<?> varEnv, LexicalEnvironment<DeclarativeEnvironmentRecord> lexEnv) {
        boolean strict = script.isStrict();
        boolean nonStrictGlobal = !strict && script.isGlobalCode() && !script.isScripting();

        /* step 1 */
        assert LexicallyDeclaredNames(script).isEmpty();
        /* step 2 */
        Set<Name> varNames = VarDeclaredNames(script);
        /* step 3 */
        List<StatementListItem> varDeclarations = VarScopedDeclarations(script);
        /* step 4 (not applicable) */
        /* step 5 */
        EnvironmentRecord varEnvRec = varEnv.getEnvRec();
        assert !nonStrictGlobal || varEnvRec instanceof GlobalEnvironmentRecord : String.format(
                "Unexpected environment record type: %s", varEnvRec);
        /* step 6 */
        if (!strict) {
            if (nonStrictGlobal) {
                /* step 6.a */
                GlobalEnvironmentRecord gEnvRec = (GlobalEnvironmentRecord) varEnvRec;
                for (Name name : varNames) {
                    ScriptRuntime.canDeclareVarScopedOrThrow(cx, gEnvRec, name.getIdentifier());
                }
            }
            /* steps 6.b-d */
            // NB: Skip the initial lexEnv which is empty by construction. (TODO: Add assertion)
            for (LexicalEnvironment<?> thisLex = lexEnv; (thisLex = thisLex.getOuter()) != varEnv;) {
                EnvironmentRecord envRec = thisLex.getEnvRec();
                for (Name name : varNames) {
                    ScriptRuntime.canDeclareVarOrThrow(cx, envRec, name.getIdentifier());
                }
            }
        }
        /* steps 7-9 (not applicable) */
        /* step 10 */
        LinkedHashSet<Name> declaredNames = new LinkedHashSet<>();
        /* step 11 */
        for (StatementListItem d : varDeclarations) {
            assert d instanceof VariableStatement;
            for (Name vn : BoundNames((VariableStatement) d)) {
                if (nonStrictGlobal) {
                    GlobalEnvironmentRecord gEnvRec = (GlobalEnvironmentRecord) varEnvRec;
                    ScriptRuntime.canDeclareGlobalVarOrThrow(cx, gEnvRec, vn.getIdentifier());
                }
                declaredNames.add(vn);
            }
        }
        /* step 12 (note) */
        /* step 13 */
        assert LexicallyScopedDeclarations(script).isEmpty();
        /* steps 14-15 (not applicable) */
        /* step 16 */
        for (Name vn : declaredNames) {
            if (nonStrictGlobal) {
                GlobalEnvironmentRecord gEnvRec = (GlobalEnvironmentRecord) varEnvRec;
                gEnvRec.createGlobalVarBinding(vn.getIdentifier(), true);
            } else {
                boolean bindingExists = varEnvRec.hasBinding(vn.getIdentifier());
                if (!bindingExists) {
                    varEnvRec.createMutableBinding(vn.getIdentifier(), true);
                    varEnvRec.initializeBinding(vn.getIdentifier(), UNDEFINED);
                }
            }
        }
        /* step 17 (return) */
    }
}
