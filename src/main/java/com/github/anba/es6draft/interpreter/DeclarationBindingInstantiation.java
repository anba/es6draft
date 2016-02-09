/**
 * Copyright (c) 2012-2016 André Bargull
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
import com.github.anba.es6draft.parser.Parser;
import com.github.anba.es6draft.runtime.DeclarativeEnvironmentRecord;
import com.github.anba.es6draft.runtime.EnvironmentRecord;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.GlobalEnvironmentRecord;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.ObjectEnvironmentRecord;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
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
     * @param env
     *            the global environment
     */
    public static void GlobalDeclarationInstantiation(ExecutionContext cx, Script script,
            LexicalEnvironment<GlobalEnvironmentRecord> env) {
        /* steps 1-2 */
        GlobalEnvironmentRecord envRec = env.getEnvRec();
        /* step 3 */
        assert LexicallyDeclaredNames(script).isEmpty();
        /* step 4 */
        Set<Name> varNames = VarDeclaredNames(script);
        /* step 5 (not applicable) */
        /* step 6 */
        for (Name name : varNames) {
            ScriptRuntime.canDeclareVarScopedOrThrow(cx, envRec, name.getIdentifier());
        }
        /* step 7 */
        List<StatementListItem> varDeclarations = VarScopedDeclarations(script);
        /* steps 8-10 (not applicable) */
        /* step 11 */
        LinkedHashSet<Name> declaredVarNames = new LinkedHashSet<>();
        /* step 12 */
        for (StatementListItem d : varDeclarations) {
            assert d instanceof VariableStatement;
            for (Name vn : BoundNames((VariableStatement) d)) {
                ScriptRuntime.canDeclareGlobalVarOrThrow(cx, envRec, vn.getIdentifier());
                declaredVarNames.add(vn);
            }
        }
        /* step 13 (NOTE) */
        /* steps 14-16 (not applicable) */
        /* step 17 */
        for (Name vn : declaredVarNames) {
            envRec.createGlobalVarBinding(vn.getIdentifier(), false);
        }
        /* step 18 (return) */
    }

    /**
     * 18.2.1.2 Runtime Semantics: EvalDeclarationInstantiation (body, varEnv, lexEnv, strict)
     * 
     * @param cx
     *            the execution context
     * @param evalScript
     *            the global script to instantiate
     * @param varEnv
     *            the variable environment
     * @param lexEnv
     *            the lexical environment
     */
    public static void EvalDeclarationInstantiation(ExecutionContext cx, Script evalScript,
            LexicalEnvironment<?> varEnv, LexicalEnvironment<DeclarativeEnvironmentRecord> lexEnv) {
        boolean strict = evalScript.isStrict();
        boolean nonStrictGlobal = !strict && evalScript.isGlobalCode() && !evalScript.isScripting();

        /* step 1 */
        Set<Name> varNames = VarDeclaredNames(evalScript);
        /* step 2 */
        List<StatementListItem> varDeclarations = VarScopedDeclarations(evalScript);
        /* step 3 (not applicable) */
        /* step 4 */
        EnvironmentRecord varEnvRec = varEnv.getEnvRec();
        assert !nonStrictGlobal || varEnvRec instanceof GlobalEnvironmentRecord : String.format(
                "Unexpected environment record type: %s", varEnvRec);
        /* step 5 */
        if (!strict) {
            if (nonStrictGlobal) {
                /* step 5.a */
                GlobalEnvironmentRecord gEnvRec = (GlobalEnvironmentRecord) varEnvRec;
                for (Name name : varNames) {
                    ScriptRuntime.canDeclareVarScopedOrThrow(cx, gEnvRec, name.getIdentifier());
                }
            }
            /* steps 5.b-d */
            if (!evalScript.isScripting() && !varNames.isEmpty()
                    && isEnclosedByLexicalOrHasVarForOf(evalScript)) {
                checkLexicalRedeclaration(cx, varEnv, lexEnv, varNames);
            }
        }
        /* steps 6-8 (not applicable) */
        /* step 9 */
        LinkedHashSet<Name> declaredNames = new LinkedHashSet<>();
        /* step 10 */
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
        /* step 11 (note) */
        /* steps 12-13 */
        assert LexicallyScopedDeclarations(evalScript).isEmpty();
        /* step 13 (not applicable) */
        /* step 15 */
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
        /* step 16 (return) */
    }

    private static boolean isEnclosedByLexicalOrHasVarForOf(Script evalScript) {
        assert evalScript.getScope().varForOfDeclaredNames().isEmpty();
        return evalScript.getParserOptions().contains(Parser.Option.EnclosedByLexicalDeclaration);
    }

    private static void checkLexicalRedeclaration(ExecutionContext cx, LexicalEnvironment<?> varEnv,
            LexicalEnvironment<DeclarativeEnvironmentRecord> lexEnv, Set<Name> varNames) {
        // Skip the initial lexEnv which is empty by construction.
        assert lexEnv.getEnvRec().bindingNames().isEmpty();
        final boolean catchVar = cx.getRealm().isEnabled(CompatibilityOption.CatchVarStatement);
        for (LexicalEnvironment<?> thisLex = lexEnv; (thisLex = thisLex.getOuter()) != varEnv;) {
            EnvironmentRecord thisEnvRec = thisLex.getEnvRec();
            if (!(thisEnvRec instanceof ObjectEnvironmentRecord)) {
                assert thisEnvRec instanceof DeclarativeEnvironmentRecord;
                DeclarativeEnvironmentRecord envRec = (DeclarativeEnvironmentRecord) thisEnvRec;
                for (Name name : varNames) {
                    ScriptRuntime.canDeclareVarOrThrow(cx, envRec, name.getIdentifier(), catchVar);
                }
            }
        }
    }
}
