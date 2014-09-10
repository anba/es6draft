/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import static com.github.anba.es6draft.Scripts.ScriptEvaluation;
import static com.github.anba.es6draft.runtime.ExecutionContext.newEvalExecutionContext;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.util.EnumSet;

import com.github.anba.es6draft.Script;
import com.github.anba.es6draft.compiler.CompilationException;
import com.github.anba.es6draft.parser.Parser;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.runtime.DeclarativeEnvironmentRecord;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.GlobalEnvironmentRecord;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;
import com.github.anba.es6draft.runtime.internal.ScriptLoader;
import com.github.anba.es6draft.runtime.internal.Source;
import com.github.anba.es6draft.runtime.types.Callable;
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
        Direct(0b00001),

        /**
         * Flag for strict-mode eval calls
         */
        Strict(0b00010),

        /**
         * Flag for global code eval calls
         */
        GlobalCode(0b00100),

        /**
         * Flag for global scope eval calls
         */
        GlobalScope(0b01000),

        /**
         * Flag for eval calls enclosed by with-statement
         */
        EnclosedByWithStatement(0b10000);

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
     * 
     * @param cx
     *            the execution context
     * @param caller
     *            the caller context
     * @param arguments
     *            the arguments
     * @return the evaluation result
     */
    public static Object indirectEval(ExecutionContext cx, ExecutionContext caller,
            Object... arguments) {
        Object source;
        Callable indirectEval = cx.getRealm().getIndirectEval();
        if (indirectEval != null) {
            source = indirectEval.call(cx, cx.getRealm().getRealmObject(), arguments);
        } else {
            source = arguments.length > 0 ? arguments[0] : UNDEFINED;
        }
        return indirectEval(cx, caller, source);
    }

    /**
     * 18.2.1 eval (x)
     * 
     * @param cx
     *            the execution context
     * @param caller
     *            the caller context
     * @param source
     *            the source string
     * @return the evaluation result
     */
    public static Object indirectEval(ExecutionContext cx, ExecutionContext caller, Object source) {
        // TODO: let's assume for now that this is the no-hook entry point (probably rename method)
        return eval(cx, caller, source,
                EvalFlags.GlobalCode.getValue() | EvalFlags.GlobalScope.getValue());
    }

    /**
     * 18.2.1 eval (x)
     * <p>
     * [Called from generated code]
     * 
     * @param arguments
     *            the arguments
     * @param cx
     *            the execution context
     * @param flags
     *            the eval flags
     * @return the evaluation result
     */
    public static Object directEval(Object[] arguments, ExecutionContext cx, int flags) {
        Object source;
        Callable translate = cx.getRealm().getDirectEvalTranslate();
        if (translate != null) {
            source = translate.call(cx, cx.getRealm().getRealmObject(), arguments);
        } else {
            source = arguments.length > 0 ? arguments[0] : UNDEFINED;
        }
        return eval(cx, cx, source, flags | EvalFlags.Direct.getValue());
    }

    private static Object eval(ExecutionContext cx, ExecutionContext caller, Object source,
            int flags) {
        boolean direct = EvalFlags.Direct.isSet(flags);
        boolean strictCaller = EvalFlags.Strict.isSet(flags);
        boolean globalCode = EvalFlags.GlobalCode.isSet(flags);
        boolean globalScope = EvalFlags.GlobalScope.isSet(flags);
        assert direct || cx == cx.getRealm().defaultContext() : "indirect eval with non-default context";
        /* step 1 */
        if (!Type.isString(source)) {
            return source;
        }
        /* step 2 */
        Script script = script(cx, caller, Type.stringValue(source).toString(), flags);
        /* step 3 */
        if (script == null) {
            return UNDEFINED;
        }
        /* step 4 */
        boolean strictScript = script.getScriptBody().isStrict();
        // strictCaller implies strictScript, but no such assertion in the specification
        assert !strictCaller || strictScript : "'strictCaller => strictScript' does not hold";
        /* steps 6-7 (implicit) */
        /* step 8 */
        Realm evalRealm = cx.getRealm();
        /* step 9 */
        if (!direct && !strictScript) {
            assert cx.getVariableEnvironment() == null;
            assert cx.getLexicalEnvironment() == null;
            return ScriptEvaluation(script, evalRealm, true);
        }
        /* step 10 */
        if (direct && !strictScript && !strictCaller && globalScope) {
            assert cx.getVariableEnvironment() == evalRealm.getGlobalEnv();
            assert cx.getLexicalEnvironment() == evalRealm.getGlobalEnv();
            return ScriptEvaluation(script, evalRealm, true);
        }
        // This step is missing the specification, depends on how eval + lexical declarations work.
        if (direct && !strictScript && !strictCaller && globalCode) {
            assert cx.getVariableEnvironment() == evalRealm.getGlobalEnv();
            // The assertion does not hold in this implementation because lexical environments
            // are only emitted when lexical declarations are present (optimization strikes back!)
            // assert cx.getLexicalEnvironment() != evalRealm.getGlobalEnv();
            return EvalScriptEvaluation(script, cx, true);
        }
        /* step 11 */
        if (direct) {
            // FIXME: ValidInFunction and ValidInModule missing in spec (Bug 949)
        }
        LexicalEnvironment<?> lexEnv, varEnv;
        if (direct) {
            /* step 12 */
            lexEnv = cx.getLexicalEnvironment();
            varEnv = cx.getVariableEnvironment();
        } else {
            /* step 13 */
            lexEnv = evalRealm.getGlobalEnv();
            varEnv = evalRealm.getGlobalEnv();
        }
        /* step 14 */
        if (strictScript || (direct && strictCaller)) {
            LexicalEnvironment<DeclarativeEnvironmentRecord> strictVarEnv = LexicalEnvironment
                    .newDeclarativeEnvironment(lexEnv);
            lexEnv = strictVarEnv;
            varEnv = strictVarEnv;
        } else {
            // begin-modification
            // lexically declared variables are placed into a new declarative environment
            lexEnv = LexicalEnvironment.newDeclarativeEnvironment(lexEnv);
            // end-modification
        }
        /* steps 17-20 (moved) */
        ExecutionContext evalCxt = newEvalExecutionContext(cx, script, varEnv, lexEnv);
        /* steps 15-16 */
        script.getScriptBody().evalDeclarationInstantiation(evalCxt, varEnv, lexEnv, true);
        /* steps 21-25 */
        Object result = script.evaluate(evalCxt);
        /* step 26 */
        return result;
    }

    /**
     * Slightly modified {@link ScriptLoader#ScriptEvaluation(Script, Realm, boolean)} method to
     * create a separate lexical environment for lexically declared variables
     * 
     * @param script
     *            the script object
     * @param cx
     *            the execution context
     * @param deletableBindings
     *            the deletableBindings flag
     * @return the script evaluation result
     */
    private static Object EvalScriptEvaluation(Script script, ExecutionContext cx,
            boolean deletableBindings) {
        Realm realm = cx.getRealm();
        /* steps 1-2 */
        RuntimeInfo.ScriptBody scriptBody = script.getScriptBody();
        /* step 3 */
        assert cx.getVariableEnvironment() == realm.getGlobalEnv();
        LexicalEnvironment<GlobalEnvironmentRecord> variableEnv = realm.getGlobalEnv();
        LexicalEnvironment<?> lexicalEnv = cx.getLexicalEnvironment();

        // begin-modification
        // lexically declared variables are placed into a new declarative environment
        lexicalEnv = LexicalEnvironment.newDeclarativeEnvironment(lexicalEnv);
        // end-modification

        /* steps 6-9 (moved) */
        ExecutionContext progCxt = newEvalExecutionContext(cx, script, variableEnv, lexicalEnv);
        /* steps 4-5 */
        scriptBody.globalDeclarationInstantiation(progCxt, variableEnv, lexicalEnv,
                deletableBindings);
        /* steps 10-14 */
        Object result = script.evaluate(progCxt);
        /* step 15 */
        return result;
    }

    private static Script script(ExecutionContext cx, ExecutionContext caller, String sourceCode,
            int flags) {
        try {
            Realm realm = cx.getRealm();
            EnumSet<Parser.Option> options = EnumSet.of(Parser.Option.EvalScript);
            if (EvalFlags.Strict.isSet(flags)) {
                options.add(Parser.Option.Strict);
            }
            if (!EvalFlags.GlobalCode.isSet(flags)) {
                options.add(Parser.Option.FunctionCode);
            }
            if (EvalFlags.Direct.isSet(flags)) {
                options.add(Parser.Option.DirectEval);
            }
            if (!EvalFlags.GlobalScope.isSet(flags)) {
                options.add(Parser.Option.LocalScope);
            }
            if (EvalFlags.EnclosedByWithStatement.isSet(flags)) {
                options.add(Parser.Option.EnclosedByWithStatement);
            }
            Source source = evalSource(realm, caller);
            return realm.getScriptLoader().evalScript(source, sourceCode, options);
        } catch (ParserException | CompilationException e) {
            throw e.toScriptException(cx);
        }
    }

    private static Source evalSource(Realm realm, ExecutionContext caller) {
        Source baseSource = realm.sourceInfo(caller);
        String sourceName;
        if (baseSource != null) {
            sourceName = String.format("<eval> (%s)", baseSource.getName());
        } else {
            sourceName = "<eval>";
        }
        return new Source(baseSource, sourceName, 1);
    }
}
