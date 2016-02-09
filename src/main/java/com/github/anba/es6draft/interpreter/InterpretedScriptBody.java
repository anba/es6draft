/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.interpreter;

import static com.github.anba.es6draft.interpreter.DeclarationBindingInstantiation.EvalDeclarationInstantiation;
import static com.github.anba.es6draft.interpreter.DeclarationBindingInstantiation.GlobalDeclarationInstantiation;
import static com.github.anba.es6draft.runtime.ExecutionContext.newEvalExecutionContext;
import static com.github.anba.es6draft.runtime.ExecutionContext.newScriptExecutionContext;
import static com.github.anba.es6draft.runtime.LexicalEnvironment.newDeclarativeEnvironment;

import com.github.anba.es6draft.Script;
import com.github.anba.es6draft.runtime.DeclarativeEnvironmentRecord;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.GlobalEnvironmentRecord;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.DebugInfo;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;
import com.github.anba.es6draft.runtime.internal.Source;

/**
 * 
 */
final class InterpretedScriptBody implements RuntimeInfo.ScriptBody {
    private final com.github.anba.es6draft.ast.Script parsedScript;

    InterpretedScriptBody(com.github.anba.es6draft.ast.Script parsedScript) {
        this.parsedScript = parsedScript;
    }

    @Override
    public Source toSource() {
        return new Source(parsedScript.getSource().getFile(), parsedScript.getSource().getName(), 1);
    }

    @Override
    public Object evaluate(ExecutionContext cx, Script script) {
        assert script.getScriptBody() == this;
        if (parsedScript.isScripting()) {
            return scriptingEvaluation(cx);
        }
        if (parsedScript.isEvalScript()) {
            return evalScriptEvaluation(cx, script);
        }
        return scriptEvaluation(cx, script);
    }

    /**
     * 15.1.7 Runtime Semantics: ScriptEvaluation
     * 
     * @param cx
     *            the execution context
     * @param script
     *            the script object
     * @return the script evaluation result
     */
    private Object scriptEvaluation(ExecutionContext cx, Script script) {
        Realm realm = cx.getRealm();
        /* step 1 (not applicable) */
        /* step 2 */
        LexicalEnvironment<GlobalEnvironmentRecord> globalEnv = realm.getGlobalEnv();
        /* steps 3-7 */
        ExecutionContext scriptCxt = newScriptExecutionContext(realm, script);
        /* steps 8-9 */
        ExecutionContext oldScriptContext = realm.getScriptContext();
        try {
            realm.setScriptContext(scriptCxt);
            /* step 10 */
            GlobalDeclarationInstantiation(scriptCxt, parsedScript, globalEnv);
            /* steps 11-12 */
            Object result = parsedScript.accept(new Interpreter(parsedScript), scriptCxt);
            /* step 16 */
            return result;
        } finally {
            /* steps 13-15  */
            realm.setScriptContext(oldScriptContext);
        }
    }

    /**
     * 18.2.1.1 Runtime Semantics: PerformEval( x, evalRealm, strictCaller, direct)
     * 
     * @param cx
     *            the execution context
     * @param script
     *            the script object
     * @return the script evaluation result
     */
    private Object evalScriptEvaluation(ExecutionContext cx, Script script) {
        // TODO: Skip allocating lex-env if not needed
        /* steps 1-5 (not applicable) */
        /* steps 6-7 */
        boolean strictEval = parsedScript.isStrict();
        /* step 8 (omitted) */
        /* steps 9-10 */
        LexicalEnvironment<DeclarativeEnvironmentRecord> lexEnv;
        LexicalEnvironment<?> varEnv;
        if (parsedScript.isDirectEval()) {
            /* step 9 */
            lexEnv = newDeclarativeEnvironment(cx.getLexicalEnvironment());
            varEnv = cx.getVariableEnvironment();
        } else {
            Realm evalRealm = cx.getRealm();
            /* step 10 */
            lexEnv = newDeclarativeEnvironment(evalRealm.getGlobalEnv());
            varEnv = evalRealm.getGlobalEnv();
        }
        /* step 11 */
        if (strictEval) {
            varEnv = lexEnv;
        }
        /* steps 12-17 */
        ExecutionContext evalCxt = newEvalExecutionContext(cx, script, varEnv, lexEnv);
        /* step 18 */
        EvalDeclarationInstantiation(evalCxt, parsedScript, varEnv, lexEnv);
        /* steps 19-23 */
        return parsedScript.accept(new Interpreter(parsedScript), evalCxt);
    }

    private Object scriptingEvaluation(ExecutionContext cx) {
        // NB: Don't need to create a new execution context here, cf. ScriptEngineImpl.
        // TODO: Skip allocating lex-env if not needed
        LexicalEnvironment<?> varEnv = cx.getVariableEnvironment();
        LexicalEnvironment<DeclarativeEnvironmentRecord> lexEnv = newDeclarativeEnvironment(cx
                .getLexicalEnvironment());
        cx.setLexicalEnvironment(lexEnv);
        EvalDeclarationInstantiation(cx, parsedScript, varEnv, lexEnv);
        return parsedScript.accept(new Interpreter(parsedScript), cx);
    }

    @Override
    public DebugInfo debugInfo() {
        return null;
    }
}
