/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime;

import static com.github.anba.es6draft.runtime.AbstractOperations.ToObject;
import static com.github.anba.es6draft.runtime.LexicalEnvironment.newDeclarativeEnvironment;
import static com.github.anba.es6draft.runtime.LexicalEnvironment.newFunctionEnvironment;
import static java.util.Objects.requireNonNull;

import com.github.anba.es6draft.runtime.types.Reference;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.FunctionObject;
import com.github.anba.es6draft.runtime.types.builtins.FunctionObject.ThisMode;
import com.github.anba.es6draft.runtime.types.builtins.GeneratorObject;

/**
 * <h1>10 Executable Code and Execution Contexts</h1>
 * <ul>
 * <li>10.4 Execution Contexts
 * </ul>
 */
public final class ExecutionContext {
    private Realm realm;
    private LexicalEnvironment lexEnv;
    private LexicalEnvironment varEnv;
    private GeneratorObject generator = null;

    public ExecutionContext() {
    }

    public Realm getRealm() {
        return realm;
    }

    public LexicalEnvironment getLexicalEnvironment() {
        return lexEnv;
    }

    public LexicalEnvironment getVariableEnvironment() {
        return varEnv;
    }

    // Helper
    public void pushLexicalEnvironment(LexicalEnvironment lexEnv) {
        assert lexEnv.getOuter() == this.lexEnv;
        this.lexEnv = lexEnv;
    }

    // Helper
    public void popLexicalEnvironment() {
        this.lexEnv = lexEnv.getOuter();
    }

    // Helper
    public void restoreLexicalEnvironment(LexicalEnvironment lexEnv) {
        this.lexEnv = lexEnv;
    }

    /**
     * [14] Runtime Semantics: Script Evaluation
     */
    public static ExecutionContext newScriptExecutionContext(Realm realm) {
        /* step 3-6 */
        ExecutionContext progCxt = new ExecutionContext();
        progCxt.realm = realm;
        progCxt.lexEnv = realm.getGlobalEnv();
        progCxt.varEnv = realm.getGlobalEnv();
        return progCxt;
    }

    /**
     * 15.1.2.1 eval (x)
     */
    public static ExecutionContext newEvalExecutionContext(Realm realm, LexicalEnvironment lexEnv,
            LexicalEnvironment varEnv) {
        /* step 20-23 */
        ExecutionContext progCxt = new ExecutionContext();
        progCxt.realm = realm;
        progCxt.lexEnv = lexEnv;
        progCxt.varEnv = varEnv;
        return progCxt;
    }

    /**
     * 11.1.7 Generator Comprehensions
     * <p>
     * Runtime Semantics: Evaluation<br>
     * TODO: not yet defined in draft [rev. 13]
     */
    public static ExecutionContext newGeneratorComprehensionContext(ExecutionContext callerContext) {
        ExecutionContext progCxt = new ExecutionContext();
        progCxt.realm = callerContext.realm;
        progCxt.lexEnv = callerContext.lexEnv;
        progCxt.varEnv = callerContext.varEnv;
        return progCxt;
    }

    /**
     * 8.3.19.1 [[Call]] Internal Method
     */
    public static ExecutionContext newFunctionExecutionContext(FunctionObject f, Object thisArgument) {
        /* 13.6.1, step 1-10 */
        ExecutionContext calleeContext = new ExecutionContext();
        Realm calleeRealm = f.getRealm();
        calleeContext.realm = calleeRealm;
        ThisMode thisMode = f.getThisMode();
        LexicalEnvironment localEnv;
        if (thisMode == ThisMode.Lexical) {
            localEnv = newDeclarativeEnvironment(f.getScope());
        } else {
            Object thisValue;
            if (thisMode == ThisMode.Strict) {
                thisValue = thisArgument;
            } else {
                assert thisArgument != null;
                switch (Type.of(thisArgument)) {
                case Undefined:
                case Null:
                    thisValue = calleeRealm.getGlobalThis();
                    break;
                case Boolean:
                case Number:
                case String:
                    thisValue = ToObject(calleeRealm, thisArgument);
                    break;
                case Object:
                default:
                    thisValue = thisArgument;
                    break;
                }
            }
            localEnv = newFunctionEnvironment(f, thisValue);
        }
        calleeContext.lexEnv = localEnv;
        calleeContext.varEnv = localEnv;
        return calleeContext;
    }

    /**
     * 10.4.1 Identifier Resolution
     */
    public Reference identifierResolution(String name, boolean strict) {
        return LexicalEnvironment.getIdentifierReference(lexEnv, name, strict);
    }

    public Object identifierValue(String name, boolean strict) {
        return LexicalEnvironment.getIdentifierValueOrThrow(lexEnv, name, strict);
    }

    /**
     * 10.4.2 GetThisEnvironment
     */
    public EnvironmentRecord getThisEnvironment() {
        LexicalEnvironment lex = lexEnv;
        for (;;) {
            EnvironmentRecord envRec = lex.getEnvRec();
            boolean exists = envRec.hasThisBinding();
            if (exists) {
                return envRec;
            }
            lex = lex.getOuter();
        }
    }

    /**
     * 10.4.3 This Resolution
     */
    public Object thisResolution() {
        EnvironmentRecord env = getThisEnvironment();
        return env.getThisBinding();
    }

    /**
     * 10.4.4 GetGlobalObject
     */
    public ScriptObject getGlobalObject() {
        Realm currentRealm = realm;
        return currentRealm.getGlobalThis();
    }

    public GeneratorObject getCurrentGenerator() {
        assert generator != null;
        return generator;
    }

    public void setCurrentGenerator(GeneratorObject generator) {
        assert this.generator == null;
        this.generator = requireNonNull(generator);
    }
}
