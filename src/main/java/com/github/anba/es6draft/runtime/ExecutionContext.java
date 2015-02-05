/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime;

import static com.github.anba.es6draft.runtime.AbstractOperations.ToObject;
import static com.github.anba.es6draft.runtime.LexicalEnvironment.newFunctionEnvironment;

import com.github.anba.es6draft.Executable;
import com.github.anba.es6draft.Module;
import com.github.anba.es6draft.Script;
import com.github.anba.es6draft.runtime.modules.ModuleRecord;
import com.github.anba.es6draft.runtime.objects.iteration.GeneratorObject;
import com.github.anba.es6draft.runtime.types.Constructor;
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
    private final LexicalEnvironment<FunctionEnvironmentRecord> funVarEnv;
    private final Executable executable;
    private final FunctionObject function;
    private GeneratorObject generator = null;

    private ExecutionContext(Realm realm, LexicalEnvironment<?> varEnv,
            LexicalEnvironment<?> lexEnv, LexicalEnvironment<FunctionEnvironmentRecord> funVarEnv,
            Executable executable, FunctionObject function) {
        this.realm = realm;
        this.varEnv = varEnv;
        this.lexEnv = lexEnv;
        this.funVarEnv = funVarEnv;
        this.executable = executable;
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

    public LexicalEnvironment<FunctionEnvironmentRecord> getFunctionVariableEnvironment() {
        return funVarEnv;
    }

    public OrdinaryObject getIntrinsic(Intrinsics id) {
        return realm.getIntrinsic(id);
    }

    public Executable getCurrentExecutable() {
        return executable;
    }

    // called from generated code
    public FunctionObject getCurrentFunction() {
        return function;
    }

    public GeneratorObject getCurrentGenerator() {
        assert generator != null;
        return generator;
    }

    public void setCurrentGenerator(GeneratorObject generator) {
        assert this.generator == null && generator != null;
        this.generator = generator;
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
    public void setVariableEnvironment(LexicalEnvironment<?> env) {
        this.varEnv = env;
    }

    /**
     * [Called from generated code]
     * 
     * @param env
     *            the new lexical environment
     */
    public void setLexicalEnvironment(LexicalEnvironment<?> env) {
        this.lexEnv = env;
    }

    /**
     * <ul>
     * <li>15 ECMAScript Language: Scripts and Modules
     * <ul>
     * <li>15.1 Script
     * </ul>
     * </ul>
     * <p>
     * 15.1.7 Runtime Semantics: ScriptEvaluation
     * 
     * @param realm
     *            the realm instance
     * @param script
     *            the script object
     * @return the new script execution context
     */
    public static ExecutionContext newScriptExecutionContext(Realm realm, Script script) {
        /* steps 3-6 */
        return new ExecutionContext(realm, realm.getGlobalEnv(), realm.getGlobalEnv(), null,
                script, null);
    }

    /**
     * <ul>
     * <li>15 ECMAScript Language: Scripts and Modules
     * <ul>
     * <li>15.2 Modules
     * <ul>
     * <li>15.2.1 Module Semantics
     * </ul>
     * </ul>
     * </ul>
     * <p>
     * 15.2.1.22 Runtime Semantics: ModuleEvaluation(module, realm)
     * 
     * @param realm
     *            the realm instance
     * @param module
     *            the module object
     * @return the new module execution context
     */
    public static ExecutionContext newModuleExecutionContext(Realm realm, ModuleRecord module) {
        /* steps 8-11 */
        return new ExecutionContext(realm, module.getEnvironment(), module.getEnvironment(), null,
                module.getScriptCode(), null);
    }

    /**
     * <ul>
     * <li>15 ECMAScript Language: Scripts and Modules
     * <ul>
     * <li>15.2 Modules
     * <ul>
     * <li>15.2.1 Module Semantics
     * </ul>
     * </ul>
     * </ul>
     * <p>
     * 15.2.1.22 Runtime Semantics: ModuleEvaluation(module, realm)
     * 
     * @param realm
     *            the realm instance
     * @param module
     *            the module object
     * @return the new module execution context
     */
    public static ExecutionContext newModuleDeclarationExecutionContext(Realm realm, Module module) {
        return new ExecutionContext(realm, realm.getGlobalEnv(), realm.getGlobalEnv(), null,
                module, null);
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
     * @param evalScript
     *            the eval script object
     * @param varEnv
     *            the current variable environment
     * @param lexEnv
     *            the current lexical environment
     * @return the new eval execution context
     */
    public static ExecutionContext newEvalExecutionContext(ExecutionContext callerContext,
            Script evalScript, LexicalEnvironment<?> varEnv, LexicalEnvironment<?> lexEnv) {
        /* steps 17-20 */
        return new ExecutionContext(callerContext.realm, varEnv, lexEnv, callerContext.funVarEnv,
                evalScript, callerContext.function);
    }

    /**
     * <ul>
     * <li>9 Ordinary and Exotic Objects Behaviours
     * <ul>
     * <li>9.2 ECMAScript Function Objects
     * </ul>
     * </ul>
     * <p>
     * 9.2.2.1 PrepareForOrdinaryCall( F, newTarget )
     * 
     * @param callerContext
     *            the caller execution context
     * @param f
     *            the callee function object
     * @param newTarget
     *            the newTarget for the function call
     * @return the new function execution context
     */
    public static ExecutionContext newFunctionExecutionContext(ExecutionContext callerContext,
            FunctionObject f, Constructor newTarget) {
        /* steps 1-3 (not applicable) */
        /* step 6 */
        Realm calleeRealm = f.getRealm();
        /* steps 8-12 */
        LexicalEnvironment<FunctionEnvironmentRecord> localEnv = newFunctionEnvironment(
                callerContext, f, newTarget);
        /* steps 4-5, 7, 13-16 */
        return new ExecutionContext(calleeRealm, localEnv, localEnv, localEnv, f.getExecutable(), f);
    }

    /**
     * <ul>
     * <li>9 Ordinary and Exotic Objects Behaviours
     * <ul>
     * <li>9.2 ECMAScript Function Objects
     * </ul>
     * </ul>
     * <p>
     * 9.2.2.1 PrepareForOrdinaryCall( F, newTarget )<br>
     * 9.2.2.2 OrdinaryCallBindThis ( F, calleeContext, thisArgument )
     * 
     * @param callerContext
     *            the caller execution context
     * @param f
     *            the callee function object
     * @param newTarget
     *            the newTarget for the function call
     * @param thisArgument
     *            the this-argument for the function call
     * @return the new function execution context
     */
    public static ExecutionContext newFunctionExecutionContext(ExecutionContext callerContext,
            FunctionObject f, Constructor newTarget, Object thisArgument) {
        /* steps 1-3 (not applicable) */
        /* step 6 */
        Realm calleeRealm = f.getRealm();
        Object thisValue = bindThisValue(f, thisArgument);
        /* steps 8-12 */
        LexicalEnvironment<FunctionEnvironmentRecord> localEnv = newFunctionEnvironment(
                callerContext, f, newTarget, thisValue);
        /* steps 4-5, 7, 13-16 */
        return new ExecutionContext(calleeRealm, localEnv, localEnv, localEnv, f.getExecutable(), f);
    }

    /**
     * 9.2.2.2 OrdinaryCallBindThis ( F, calleeContext, thisArgument )
     * 
     * @param calleeRealm
     * @param f
     * @param thisArgument
     * @return
     */
    private static Object bindThisValue(FunctionObject f, Object thisArgument) {
        /* step 1 */
        ThisMode thisMode = f.getThisMode();
        /* step 2 */
        if (thisMode == ThisMode.Lexical) {
            return null;
        }
        /* step 3 */
        Realm calleeRealm = f.getRealm();
        /* step 4 (not applicable) */
        /* steps 5-6 */
        Object thisValue;
        if (thisMode == ThisMode.Strict) {
            /* step 5 */
            thisValue = thisArgument;
        } else {
            /* step 6 */
            if (Type.isUndefinedOrNull(thisArgument)) {
                thisValue = calleeRealm.getGlobalThis();
            } else {
                thisValue = ToObject(calleeRealm.defaultContext(), thisArgument);
            }
        }
        /* steps 7-8 (not applicable) */
        return thisValue;
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
     * 8.3.1 ResolveBinding(name) Abstract Operation
     * 
     * @param name
     *            the binding name
     * @param strict
     *            the strict mode flag
     * @return the resolved reference
     */
    public Reference<? extends EnvironmentRecord, String> resolveBinding(String name, boolean strict) {
        /* steps 1-3 */
        return LexicalEnvironment.getIdentifierReference(lexEnv, name, strict);
    }

    /**
     * 8.3.2 GetThisEnvironment() Abstract Operation
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
     * 8.3.3 ResolveThisBinding() Abstract Operation
     * 
     * @return the this-binding object
     */
    public Object resolveThisBinding() {
        /* step 1 */
        EnvironmentRecord envRec = getThisEnvironment();
        /* step 2 */
        return envRec.getThisBinding();
    }

    /**
     * 8.3.4 GetNewTarget ( ) Abstract Operation
     * 
     * @return the NewTarget constructor object
     */
    public Constructor getNewTarget() {
        /* step 1 */
        EnvironmentRecord envRec = getThisEnvironment();
        /* step 2 */
        assert envRec instanceof FunctionEnvironmentRecord : String.format(
                "Wrong environment kind = %s", envRec.getClass().getSimpleName());
        /* step 3 */
        return ((FunctionEnvironmentRecord) envRec).getNewTarget();
    }

    /**
     * 8.3.5 GetGlobalObject() Abstract Operation
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
