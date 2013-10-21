/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import static com.github.anba.es6draft.ScriptLoader.ScriptEvaluation;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.util.EnumSet;

import com.github.anba.es6draft.Script;
import com.github.anba.es6draft.ScriptLoader;
import com.github.anba.es6draft.compiler.CompilationException;
import com.github.anba.es6draft.parser.Parser;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.Type;

/**
 * <h1>18 The Global Object</h1><br>
 * <h2>18.2 Function Properties of the Global Object</h2>
 * <ul>
 * <li>18.2.1 eval (x)
 * </ul>
 */
public final class Eval {
    private Eval() {
    }

    public enum EvalFlags {
        /**
         * Flag for direct eval calls
         */
        Direct(0b0001),

        /**
         * Flag for strict-mode eval calls
         */
        Strict(0b0010),

        /**
         * Flag for global code eval calls
         */
        GlobalCode(0b0100),

        /**
         * Flag for eval calls enclosed by with-statement
         */
        EnclosedByWithStatement(0b1000);

        private final int value;

        private EvalFlags(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public boolean isSet(int bitmask) {
            return (value & bitmask) != 0;
        }
    }

    /**
     * 18.2.1 eval (x)
     */
    public static Object indirectEval(ExecutionContext cx, Object source) {
        return eval(cx, source, EvalFlags.GlobalCode.getValue());
    }

    /**
     * 18.2.1 eval (x)
     */
    public static Object directEval(Object source, ExecutionContext cx, int flags) {
        return eval(cx, source, flags | EvalFlags.Direct.getValue());
    }

    private static Object eval(ExecutionContext cx, Object source, int flags) {
        boolean direct = EvalFlags.Direct.isSet(flags);
        boolean strictCaller = EvalFlags.Strict.isSet(flags);
        boolean globalCode = EvalFlags.GlobalCode.isSet(flags);
        boolean withStatement = EvalFlags.EnclosedByWithStatement.isSet(flags);
        /* step 1 */
        if (!Type.isString(source)) {
            return source;
        }
        /* step 5 */
        Realm evalRealm = cx.getRealm();
        boolean globalScope = direct && (cx.getLexicalEnvironment() == evalRealm.getGlobalEnv());

        /* step 2 */
        Script script = script(cx, Type.stringValue(source), strictCaller, globalCode, direct,
                globalScope, withStatement);
        /* step 3 */
        if (script == null) {
            return UNDEFINED;
        }
        /* step 4 */
        boolean strictScript = script.getScriptBody().isStrict();
        /* steps 6-8 (implicit) */
        /* step 9 */
        if (!direct && !strictScript) {
            return ScriptEvaluation(script, ctx, true);
        }
        /* step 10 */
        if (direct && !strictScript && !strictCaller && globalScope) {
            return ScriptEvaluation(script, ctx, true);
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
        /* steps 15-16 */
        script.getScriptBody().evalDeclarationInstantiation(ctx, lexEnv, varEnv, true);
        /* steps 17-20 */
        ExecutionContext evalCxt = ExecutionContext.newEvalExecutionContext(ctx, lexEnv, varEnv);
        /* steps 21-25 */
        Object result = script.evaluate(evalCxt);
        /* step 26 */
        return result;
    }

    private static Script script(ExecutionContext cx, CharSequence source, boolean strict,
            boolean globalCode, boolean directEval, boolean globalScope, boolean withStatement) {
        try {
            Realm realm = cx.getRealm();
            EnumSet<Parser.Option> options = Parser.Option.from(realm.getOptions());
            options.add(Parser.Option.EvalScript);
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
            if (withStatement) {
                options.add(Parser.Option.EnclosedByWithStatement);
            }
            Parser parser = new Parser("<eval>", 1, options);
            com.github.anba.es6draft.ast.Script parsedScript = parser.parseScript(source);
            if (parsedScript.getStatements().isEmpty()) {
                return null;
            }
            String className = realm.nextEvalName();
            return ScriptLoader.load(className, parsedScript, realm.getCompilerOptions());
        } catch (ParserException | CompilationException e) {
            throw e.toScriptException(cx);
        }
    }
}
