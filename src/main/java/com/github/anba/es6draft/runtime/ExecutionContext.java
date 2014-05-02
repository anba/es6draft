/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime;

import static com.github.anba.es6draft.runtime.AbstractOperations.ToObject;
import static com.github.anba.es6draft.runtime.LexicalEnvironment.newDeclarativeEnvironment;
import static com.github.anba.es6draft.runtime.LexicalEnvironment.newFunctionEnvironment;
import static java.util.Objects.requireNonNull;

import com.github.anba.es6draft.Script;
import com.github.anba.es6draft.runtime.objects.iteration.GeneratorObject;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Reference;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.FunctionObject;
import com.github.anba.es6draft.runtime.types.builtins.FunctionObject.ThisMode;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>8 Executable Code and Execution Contexts</h1>
 * <ul>
 * <li>8.3 Execution Contexts
 * </ul>
 */
public final class ExecutionContext {
    private final Realm realm;
    private LexicalEnvironment<?> varEnv;
    private LexicalEnvironment<?> lexEnv;
    private final Script script;
    private final FunctionObject function;
    private GeneratorObject generator = null;

    private ExecutionContext(Realm realm, LexicalEnvironment<?> varEnv,
            LexicalEnvironment<?> lexEnv, Script script, FunctionObject function) {
        this.realm = realm;
        this.varEnv = varEnv;
        this.lexEnv = lexEnv;
        this.script = script;
        this.function = function;
    }

    public Realm getRealm() {
        return realm;
    }

    public LexicalEnvironment<?> getLexicalEnvironment() {
        return lexEnv;
    }

    public LexicalEnvironment<?> getVariableEnvironment() {
        return varEnv;
    }

    public OrdinaryObject getIntrinsic(Intrinsics id) {
        return realm.getIntrinsic(id);
    }

    public Script getCurrentScript() {
        return script;
    }

    public FunctionObject getCurrentFunction() {
        return function;
    }

    public GeneratorObject getCurrentGenerator() {
        assert generator != null;
        return generator;
    }

    public void setCurrentGenerator(GeneratorObject generator) {
        assert this.generator == null;
        this.generator = requireNonNull(generator);
    }

    /**
     * [Called from generated code]
     * 
     * @param lexEnv
     *            the new lexical environment
     */
    public void pushLexicalEnvironment(LexicalEnvironment<?> lexEnv) {
        assert lexEnv.getOuter() == this.lexEnv;
        this.lexEnv = lexEnv;
    }

    /**
     * [Called from generated code]
     */
    public void popLexicalEnvironment() {
        this.lexEnv = lexEnv.getOuter();
    }

    /**
     * [Called from generated code]
     * 
     * @param lexEnv
     *            the new lexical environment
     */
    public void restoreLexicalEnvironment(LexicalEnvironment<?> lexEnv) {
        this.lexEnv = lexEnv;
    }

    /**
     * [Called from generated code]
     * 
     * @param lexEnv
     *            the new lexical environment
     */
    public void replaceLexicalEnvironment(LexicalEnvironment<?> lexEnv) {
        assert lexEnv.getOuter() == this.lexEnv.getOuter();
        this.lexEnv = lexEnv;
    }

    /**
     * [Called from generated code]
     * 
     * @param env
     *            the new lexical environment
     */
    public void setEnvironment(LexicalEnvironment<?> env) {
        assert env.getOuter() == this.lexEnv;
        assert this.varEnv == this.lexEnv;
        this.varEnv = env;
        this.lexEnv = env;
    }

    /**
     * <ul>
     * <li>15 ECMAScript Language: Scripts and Modules
     * <ul>
     * <li>15.1 Script
     * <ul>
     * <li>15.1.2 Runtime Semantics
     * </ul>
     * </ul>
     * </ul>
     * <p>
     * Runtime Semantics: Script Evaluation
     * 
     * @param realm
     *            the realm instance
     * @param script
     *            the script object
     * @return the new script execution context
     */
    public static ExecutionContext newScriptExecutionContext(Realm realm, Script script) {
        /* steps 3-6 */
        return new ExecutionContext(realm, realm.getGlobalEnv(), realm.getGlobalEnv(), script, null);
    }

    /**
     * <ul>
     * <li>15 ECMAScript Language: Scripts and Modules
     * <ul>
     * <li>15.2 Modules
     * <ul>
     * <li>15.2.6 Runtime Semantics: Module Evaluation
     * </ul>
     * </ul>
     * </ul>
     * <p>
     * 15.2.6.2 EnsureEvaluated(mod, seen, loader) Abstract Operation
     * 
     * @param realm
     *            the realm instance
     * @param env
     *            the current lexical environment
     * @return the new module execution context
     */
    public static ExecutionContext newModuleExecutionContext(Realm realm,
            LexicalEnvironment<DeclarativeEnvironmentRecord> env) {
        /* steps 8-11 */
        return new ExecutionContext(realm, env, env, null, null);
    }

    /**
     * <ul>
     * <li>18 The Global Object
     * <ul>
     * <li>18.2 Function Properties of the Global Object
     * </ul>
     * </ul>
     * <p>
     * 18.2.1 eval (x)
     * 
     * @param callerContext
     *            the caller execution context
     * @param varEnv
     *            the current variable environment
     * @param lexEnv
     *            the current lexical environment
     * @return the new eval execution context
     */
    public static ExecutionContext newEvalExecutionContext(ExecutionContext callerContext,
            LexicalEnvironment<?> varEnv, LexicalEnvironment<?> lexEnv) {
        /* steps 17-20 */
        return new ExecutionContext(callerContext.realm, varEnv, lexEnv, callerContext.script,
                callerContext.function);
    }

    /**
     * <ul>
     * <li>9 Ordinary and Exotic Objects Behaviours
     * <ul>
     * <li>9.2 ECMAScript Function Objects
     * </ul>
     * </ul>
     * <p>
     * 9.2.4 [[Call]] (thisArgument, argumentsList)
     * 
     * @param callerContext
     *            the caller execution context
     * @param f
     *            the callee function object
     * @param thisArgument
     *            the this-argument for the function call
     * @return the new function execution context
     */
    public static ExecutionContext newFunctionExecutionContext(ExecutionContext callerContext,
            FunctionObject f, Object thisArgument) {
        /* 9.2.4, steps 4-12, 14 */
        Realm calleeRealm = f.getRealm();
        ThisMode thisMode = f.getThisMode();
        LexicalEnvironment<? extends DeclarativeEnvironmentRecord> localEnv;
        if (thisMode == ThisMode.Lexical) {
            localEnv = newDeclarativeEnvironment(f.getEnvironment());
        } else {
            Object thisValue;
            if (thisMode == ThisMode.Strict) {
                thisValue = thisArgument;
            } else {
                if (Type.isUndefinedOrNull(thisArgument)) {
                    thisValue = calleeRealm.getGlobalThis();
                } else if (Type.isObject(thisArgument)) {
                    thisValue = thisArgument;
                } else {
                    /*  step 14 */
                    thisValue = ToObject(f.getRealm().defaultContext(), thisArgument);
                }
            }
            localEnv = newFunctionEnvironment(callerContext, f, thisValue);
        }
        return new ExecutionContext(calleeRealm, localEnv, localEnv, null, f);
    }

    /**
     * Combined {@link #resolveBinding(String, boolean)} with
     * {@link Reference#GetValue(Object, ExecutionContext)} internal method.
     * 
     * @param name
     *            the binding name
     * @param strict
     *            the strict mode flag
     * @return the resolved reference value
     */
    public Object resolveBindingValue(String name, boolean strict) {
        return LexicalEnvironment.getIdentifierValueOrThrow(lexEnv, name, strict);
    }

    /**
     * 8.3.1 ResolveBinding(name)
     * 
     * @param name
     *            the binding name
     * @param strict
     *            the strict mode flag
     * @return the resolved reference
     */
    public Reference<EnvironmentRecord, String> resolveBinding(String name, boolean strict) {
        /* steps 1-3 */
        return LexicalEnvironment.getIdentifierReference(lexEnv, name, strict);
    }

    /**
     * 8.3.2 GetThisEnvironment
     * 
     * @return the first environment record with a this-binding
     */
    public EnvironmentRecord getThisEnvironment() {
        /* step 1 */
        LexicalEnvironment<?> lex = lexEnv;
        /* step 2 */
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
     * 8.3.3 ResolveThisBinding
     * 
     * @return the this-binding object
     */
    public Object resolveThisBinding() {
        /* step 1 */
        EnvironmentRecord env = getThisEnvironment();
        /* step 2 */
        return env.getThisBinding();
    }

    /**
     * 8.3.4 GetGlobalObject
     * 
     * @return the global object instance
     */
    public ScriptObject getGlobalObject() {
        /* steps 1-2 */
        Realm currentRealm = realm;
        /* step 3 */
        return currentRealm.getGlobalThis();
    }
}
