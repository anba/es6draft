/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import static com.github.anba.es6draft.ScriptLoader.ScriptEvaluation;
import static com.github.anba.es6draft.runtime.internal.Errors.throwReferenceError;
import static com.github.anba.es6draft.runtime.internal.Errors.throwSyntaxError;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.util.EnumSet;

import com.github.anba.es6draft.Script;
import com.github.anba.es6draft.ScriptLoader;
import com.github.anba.es6draft.parser.Parser;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.parser.ParserException.ExceptionType;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.Type;

/**
 * <h1>15 Standard Built-in ECMAScript Objects</h1><br>
 * <h2>15.1 The Global Object</h2><br>
 * <h3>15.1.2 Function Properties of the Global Object</h3>
 * <ul>
 * <li>15.1.2.1 eval
 * </ul>
 */
public final class Eval {
    private Eval() {
    }

    /**
     * 15.1.2.1 eval (x)
     */
    public static Object indirectEval(Realm evalRealm, Object source) {
        return eval(evalRealm, null, false, true, source);
    }

    /**
     * 15.1.2.1 eval (x)
     */
    public static Object directEval(Object source, ExecutionContext ctx, boolean strictCaller,
            boolean globalCode) {
        return eval(ctx.getRealm(), ctx, strictCaller, globalCode, source);
    }

    private static Object eval(Realm evalRealm, ExecutionContext ctx, boolean strictCaller,
            boolean globalCode, Object source) {
        assert !(ctx == null && strictCaller);
        /* step 1 */
        if (!Type.isString(source)) {
            return source;
        }
        /* step 5 */
        boolean direct = (ctx != null);
        boolean globalScope = direct && (ctx.getLexicalEnvironment() == evalRealm.getGlobalEnv());

        /* step 2 */
        Script script = script(evalRealm, Type.stringValue(source), strictCaller, globalCode,
                direct, globalScope);
        /* step 3 */
        if (script == null) {
            return UNDEFINED;
        }
        /* step 4 */
        boolean strictScript = script.getScriptBody().isStrict();
        /* step 6-8 (implicit) */
        /* step 9 */
        if (!direct && !strictScript) {
            return ScriptEvaluation(script, evalRealm, true);
        }
        /* step 10 */
        if (direct && !strictScript && !strictCaller && globalScope) {
            return ScriptEvaluation(script, evalRealm, true);
        }
        /* step 11 */
        if (direct) {
            // FIXME: ValidInFunction and ValidInModule missing in spec (Bug 949)
        }
        LexicalEnvironment lexEnv, varEnv;
        if (direct) {
            /* step 12 */
            lexEnv = ctx.getLexicalEnvironment();
            varEnv = ctx.getVariableEnvironment();
        } else {
            /* step 13 */
            lexEnv = evalRealm.getGlobalEnv();
            varEnv = evalRealm.getGlobalEnv();
        }
        /* step 14 */
        if (strictScript || (direct && strictCaller)) {
            LexicalEnvironment strictVarEnv = LexicalEnvironment.newDeclarativeEnvironment(lexEnv);
            lexEnv = strictVarEnv;
            varEnv = strictVarEnv;
        } else {
            // begin-modification
            // lexically declared variables are being placed into a new declarative environment
            lexEnv = LexicalEnvironment.newDeclarativeEnvironment(lexEnv);
            // end-modification
        }
        /* step 15-16 */
        script.getScriptBody().evalDeclarationInstantiation(evalRealm, lexEnv, varEnv, true);
        /* step 17-20 */
        ExecutionContext evalCxt = ExecutionContext.newEvalExecutionContext(evalRealm, lexEnv,
                varEnv);
        /* step 21-25 */
        Object result = script.evaluate(evalCxt);
        /* step 26 */
        return result;
    }

    private static Script script(Realm realm, CharSequence source, boolean strict,
            boolean globalCode, boolean directEval, boolean globalScope) {
        try {
            EnumSet<Parser.Option> options = EnumSet.of(Parser.Option.EvalScript);
            if (strict) {
                options.add(Parser.Option.Strict);
            }
            if (!globalCode) {
                options.add(Parser.Option.FunctionCode);
            }
            if (directEval) {
                options.add(Parser.Option.DirectEval);
            }
            if (!globalScope) {
                options.add(Parser.Option.LocalScope);
            }
            Parser parser = new Parser("<eval>", 1, options);
            com.github.anba.es6draft.ast.Script parsedScript = parser.parse(source);
            if (parsedScript.getStatements().isEmpty()) {
                return null;
            }
            String className = realm.nextEvalName();
            return ScriptLoader.load(className, parsedScript);
        } catch (ParserException e) {
            if (e.getExceptionType() == ExceptionType.ReferenceError) {
                throw throwReferenceError(realm, e.getMessageKey(), e.getMessageArguments());
            }
            throw throwSyntaxError(realm, e.getMessageKey(), e.getMessageArguments());
        }
    }
}
