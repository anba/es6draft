/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime;

import com.github.anba.es6draft.Executable;
import com.github.anba.es6draft.Module;
import com.github.anba.es6draft.Script;
import com.github.anba.es6draft.runtime.internal.RuntimeContext;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo.SourceObject;
import com.github.anba.es6draft.runtime.modules.SourceTextModuleRecord;
import com.github.anba.es6draft.runtime.objects.async.AsyncObject;
import com.github.anba.es6draft.runtime.objects.iteration.GeneratorObject;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Reference;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.builtins.FunctionObject;
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
    private GeneratorObject generator;
    private AsyncObject async;

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

    /**
     * Returns the {@code Realm} component of this execution context.
     * 
     * @return the {@code Realm} component
     */
    public Realm getRealm() {
        return realm;
    }

    /**
     * Returns the runtime context of this execution context.
     * 
     * @return the runtime context
     */
    public RuntimeContext getRuntimeContext() {
        return realm.getWorld().getContext();
    }

    /**
     * Returns the requested intrinsic from this execution context's {@code Realm}.
     * 
     * @param id
     *            the intrinsic identifier
     * @return the intrinsic object
     * @see Realm#getIntrinsic(Intrinsics)
     */
    public OrdinaryObject getIntrinsic(Intrinsics id) {
        return realm.getIntrinsic(id);
    }

    /**
     * Returns the requested intrinsic from this execution context's {@code Realm}.
     * 
     * @param <T>
     *            the intrisic's type
     * @param id
     *            the intrinsic identifier
     * @param klass
     *            the intrinsic's class
     * @return the intrinsic object
     * @see Realm#getIntrinsic(Intrinsics)
     */
    @SuppressWarnings("unchecked")
    public <T extends OrdinaryObject> OrdinaryObject getIntrinsic(Intrinsics id, Class<T> klass) {
        OrdinaryObject intrinsic = realm.getIntrinsic(id);
        assert klass.isInstance(intrinsic) : "Unexpected type: " + intrinsic.getClass();
        return (T) intrinsic;
    }

    /**
     * Returns the current executable object.
     * 
     * @return the executable object
     */
    public Executable getCurrentExecutable() {
        return executable;
    }

    /**
     * Returns the {@code Function} component of this execution context.
     * 
     * @return the {@code Function} component or {@code null} if not evaluating a function
     */
    public FunctionObject getCurrentFunction() {
        return function;
    }

    /**
     * Returns the {@code Generator} component of this execution context.
     * 
     * @return the {@code Generator} component or {@code null} if not evaluating a generator
     */
    public GeneratorObject getCurrentGenerator() {
        return generator;
    }

    /**
     * Sets the {@code Generator} component of this execution context.
     * 
     * @param generator
     *            the {@code Generator} component
     */
    public void setCurrentGenerator(GeneratorObject generator) {
        assert this.generator == null && generator != null;
        this.generator = generator;
    }

    /**
     * Returns the {@code AsyncObject} component of this execution context.
     * 
     * @return the {@code AsyncObject} component or {@code null} if not evaluating an async function
     */
    public AsyncObject getCurrentAsync() {
        return async;
    }

    /**
     * Sets the {@code AsyncObject} component of this execution context.
     * 
     * @param async
     *            the {@code AsyncObject} component
     */
    public void setCurrentAsync(AsyncObject async) {
        assert this.async == null && async != null;
        this.async = async;
    }

    /**
     * Returns the {@code LexicalEnvironment} component of this execution context.
     * 
     * @return the {@code LexicalEnvironment} component or {@code null} if not evaluating ECMAScript
     *         code
     */
    public LexicalEnvironment<?> getLexicalEnvironment() {
        return lexEnv;
    }

    /**
     * Returns the {@code VariableEnvironment} component of this execution context.
     * 
     * @return the {@code VariableEnvironment} component or {@code null} if not evaluating
     *         ECMAScript code
     */
    public LexicalEnvironment<?> getVariableEnvironment() {
        return varEnv;
    }

    /**
     * Returns the {@code FunctionVariableEnvironment} component of this execution context.
     * 
     * @return the {@code FunctionVariableEnvironment} component or {@code null} if not evaluating
     *         ECMAScript function code
     */
    public LexicalEnvironment<FunctionEnvironmentRecord> getFunctionVariableEnvironment() {
        return funVarEnv;
    }

    /**
     * Returns the environment record of the {@code LexicalEnvironment} component of this execution
     * context. Must not be called if not evaluating ECMAScript code.
     * 
     * @return the environment record
     */
    public EnvironmentRecord getLexicalEnvironmentRecord() {
        return lexEnv.getEnvRec();
    }

    /**
     * Returns the environment record of the {@code VariableEnvironment} component of this execution
     * context. Must not be called if not evaluating ECMAScript code.
     * 
     * @return the environment record
     */
    public EnvironmentRecord getVariableEnvironmentRecord() {
        return varEnv.getEnvRec();
    }

    /**
     * Returns the environment record of the {@code FunctionVariableEnvironment} component of this
     * execution context. Must not be called if not evaluating ECMAScript function code.
     * 
     * @return the environment record
     */
    public FunctionEnvironmentRecord getFunctionVariableEnvironmentRecord() {
        return funVarEnv.getEnvRec();
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
     * [Called from generated code]
     * 
     * @param env
     *            the new lexical environment
     */
    public void setVariableAndLexicalEnvironment(LexicalEnvironment<?> env) {
        assert this.varEnv == this.lexEnv;
        this.varEnv = env;
        this.lexEnv = env;
    }

    /**
     * Creates a new default execution context.
     * 
     * @param realm
     *            the realm instance
     * @return the new default execution context
     */
    static ExecutionContext newDefaultExecutionContext(Realm realm) {
        return new ExecutionContext(realm, null, null, null, DefaultExecutable.INSTANCE, null);
    }

    private static final class DefaultExecutable implements Executable {
        private static final DefaultExecutable INSTANCE = new DefaultExecutable();

        @Override
        public SourceObject getSourceObject() {
            return null;
        }
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
     * 15.2.1.21 Runtime Semantics: ModuleDeclarationInstantiation( module, realm, moduleSet )
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
    public static ExecutionContext newModuleExecutionContext(Realm realm,
            SourceTextModuleRecord module) {
        /* steps 4-9 */
        return new ExecutionContext(realm, module.getEnvironment(), module.getEnvironment(), null,
                module.getScriptCode(), null);
    }

    /**
     * <ul>
     * <li>18 The Global Object
     * <ul>
     * <li>18.2 Function Properties of the Global Object
     * </ul>
     * </ul>
     * <p>
     * 18.2.1.1 Runtime Semantics: PerformEval( x, evalRealm, strictCaller, direct)
     * 
     * @param callerContext
     *            the caller execution context
     * @param evalScript
     *            the eval script object
     * @param varEnv
     *            the current variable environment
     * @param lexEnv
     *            the new lexical environment
     * @return the new eval execution context
     */
    public static ExecutionContext newEvalExecutionContext(ExecutionContext callerContext,
            Script evalScript, LexicalEnvironment<?> varEnv, LexicalEnvironment<?> lexEnv) {
        /* steps 12-17 */
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
     * @param f
     *            the callee function object
     * @param localEnv
     *            the function environment
     * @return the new function execution context
     */
    public static ExecutionContext newFunctionExecutionContext(FunctionObject f,
            LexicalEnvironment<FunctionEnvironmentRecord> localEnv) {
        /* steps 1-2, 7 (not applicable) */
        /* steps 3-6, 8-13 */
        return new ExecutionContext(f.getRealm(), localEnv, localEnv, localEnv, f.getExecutable(),
                f);
    }

    /**
     * Returns a new execution context for JSR-223 scripting.
     * 
     * @param realm
     *            the realm instance
     * @param script
     *            the script object
     * @param varEnv
     *            the variable environment
     * @return the new scripting execution context
     */
    public static ExecutionContext newScriptingExecutionContext(Realm realm, Script script,
            LexicalEnvironment<?> varEnv) {
        return new ExecutionContext(realm, varEnv, varEnv, null, script, null);
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
    public Reference<?, String> resolveBinding(String name, boolean strict) {
        /* steps 1-3 */
        return LexicalEnvironment.getIdentifierReference(lexEnv, name, strict);
    }

    /**
     * 8.3.2 GetThisEnvironment()
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
     * 8.3.3 ResolveThisBinding()
     * 
     * @return the this-binding object
     */
    public Object resolveThisBinding() {
        /* step 1 */
        EnvironmentRecord envRec = getThisEnvironment();
        /* step 2 */
        return envRec.getThisBinding(this);
    }

    /**
     * 8.3.4 GetNewTarget ( )
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
     * 8.3.5 GetGlobalObject()
     * 
     * @return the global object instance
     */
    public ScriptObject getGlobalObject() {
        /* steps 1-2 */
        Realm currentRealm = realm;
        /* step 3 */
        return currentRealm.getGlobalObject();
    }
}
