/**
 * Copyright (c) André Bargull
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
import com.github.anba.es6draft.runtime.internal.ScriptException;

/**
 * 
 */
final class InterpretedScriptBody implements RuntimeInfo.ScriptBody {
    private static final String INTERPRETER_SCRIPTBODY = InterpretedScriptBody.class.getName();
    private final com.github.anba.es6draft.ast.Script parsedScript;

    InterpretedScriptBody(com.github.anba.es6draft.ast.Script parsedScript) {
        this.parsedScript = parsedScript;
    }

    @Override
    public Object evaluate(ExecutionContext cx, Script script) {
        assert script.getScriptBody() == this;
        Interpreter interpreter = new Interpreter(parsedScript);
        try {
            if (parsedScript.isScripting()) {
                return scriptingEvaluation(cx, interpreter);
            }
            if (parsedScript.isEvalScript()) {
                return evalScriptEvaluation(cx, script, interpreter);
            }
            return scriptEvaluation(cx, script, interpreter);
        } catch (ScriptException e) {
            throw interpreterException(interpreter, e);
        }
    }

    private ScriptException interpreterException(Interpreter interpreter, ScriptException e) {
        StackTraceElement[] elements = e.getStackTrace();
        int entry = -1;
        for (int i = 0; i < elements.length; ++i) {
            StackTraceElement element = elements[i];
            if (INTERPRETER_SCRIPTBODY.equals(element.getClassName()) && "evaluate".equals(element.getMethodName())) {
                entry = i;
                break;
            }
        }
        // Replace entry frame with script file information.
        if (entry != -1) {
            StackTraceElement[] newElements = elements.clone();
            newElements[entry] = new StackTraceElement("#Interpreter", "~interpreter",
                    parsedScript.getSource().getName(), interpreter.getCurrentLine());
            e.setStackTrace(newElements);
        }
        return e;
    }

    /**
     * 15.1.7 Runtime Semantics: ScriptEvaluation
     * 
     * @param cx
     *            the execution context
     * @param script
     *            the script object
     * @param interpreter
     *            the interpreter
     * @return the script evaluation result
     */
    private Object scriptEvaluation(ExecutionContext cx, Script script, Interpreter interpreter) {
        Realm realm = cx.getRealm();
        /* step 1 (not applicable) */
        /* step 2 */
        LexicalEnvironment<GlobalEnvironmentRecord> globalEnv = realm.getGlobalEnv();
        /* steps 3-7 */
        ExecutionContext scriptCxt = newScriptExecutionContext(realm, script);
        /* steps 8-9 */
        ExecutionContext oldScriptContext = realm.getWorld().getScriptContext();
        try {
            realm.getWorld().setScriptContext(scriptCxt);
            /* step 10 */
            GlobalDeclarationInstantiation(scriptCxt, parsedScript, globalEnv);
            /* steps 11-12 */
            Object result = parsedScript.accept(interpreter, scriptCxt);
            /* step 16 */
            return result;
        } finally {
            /* steps 13-15  */
            realm.getWorld().setScriptContext(oldScriptContext);
        }
    }

    /**
     * 18.2.1.1 Runtime Semantics: PerformEval( x, evalRealm, strictCaller, direct)
     * 
     * @param cx
     *            the execution context
     * @param script
     *            the script object
     * @param interpreter
     *            the interpreter
     * @return the script evaluation result
     */
    private Object evalScriptEvaluation(ExecutionContext cx, Script script, Interpreter interpreter) {
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
        /* steps 12-19 */
        ExecutionContext evalCxt = newEvalExecutionContext(cx, script, varEnv, lexEnv);
        /* step 20 */
        EvalDeclarationInstantiation(evalCxt, parsedScript, varEnv, lexEnv);
        /* steps 21-25 */
        return parsedScript.accept(interpreter, evalCxt);
    }

    private Object scriptingEvaluation(ExecutionContext cx, Interpreter interpreter) {
        // NB: Don't need to create a new execution context here, cf. ScriptEngineImpl.
        // TODO: Skip allocating lex-env if not needed
        LexicalEnvironment<?> varEnv = cx.getVariableEnvironment();
        LexicalEnvironment<DeclarativeEnvironmentRecord> lexEnv = newDeclarativeEnvironment(cx.getLexicalEnvironment());
        cx.setLexicalEnvironment(lexEnv);
        EvalDeclarationInstantiation(cx, parsedScript, varEnv, lexEnv);
        return parsedScript.accept(interpreter, cx);
    }

    @Override
    public DebugInfo debugInfo() {
        return null;
    }
}
