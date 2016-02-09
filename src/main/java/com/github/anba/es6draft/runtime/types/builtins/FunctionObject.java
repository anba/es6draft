/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

import com.github.anba.es6draft.Executable;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;
import com.github.anba.es6draft.runtime.internal.Source;
import com.github.anba.es6draft.runtime.internal.TailCallInvocation;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.ScriptObject;

/**
 * <h1>9 Ordinary and Exotic Objects Behaviours</h1><br>
 * <ul>
 * <li>9.2 ECMAScript Function Objects
 * </ul>
 */
public abstract class FunctionObject extends OrdinaryObject implements Callable {
    /** [[Environment]] */
    private LexicalEnvironment<?> environment;
    /** [[FunctionKind]] */
    private FunctionKind functionKind;
    /** [[FormalParameters]] / [[Code]] */
    private RuntimeInfo.Function function;
    /** [[ConstructorKind]] */
    private ConstructorKind constructorKind;
    /** [[Realm]] */
    private Realm realm;
    /** [[ThisMode]] */
    private ThisMode thisMode;
    /** [[Strict]] */
    private boolean strict;
    /** [[HomeObject]] */
    private ScriptObject homeObject;

    private boolean isClone;
    private Executable executable;
    private String source;
    private MethodHandle callMethod;
    private MethodHandle tailCallMethod;
    private MethodHandle constructMethod;

    /**
     * Constructs a new Function object.
     * 
     * @param realm
     *            the realm object
     */
    protected FunctionObject(Realm realm) {
        super(realm);
    }

    public enum FunctionKind {
        Normal, ClassConstructor, Method, Arrow
    }

    public enum ConstructorKind {
        Base, Derived
    }

    public enum ThisMode {
        Lexical, Strict, Global
    }

    @SuppressWarnings("unchecked")
    protected static final <E extends Throwable> E rethrow(Throwable e) throws E {
        throw (E) e;
    }

    /**
     * Returns the {@link MethodHandle} for the function call entry method.
     * 
     * @return the call method handle
     */
    public final MethodHandle getCallMethod() {
        return callMethod;
    }

    /**
     * Returns the {@link MethodHandle} for the function tail-call entry method.
     * 
     * @return the tail-call method handle
     */
    public final MethodHandle getTailCallMethod() {
        return tailCallMethod;
    }

    /**
     * Returns the {@link MethodHandle} for the function construct entry method.
     * 
     * @return the call method handle
     */
    public final MethodHandle getConstructMethod() {
        return constructMethod;
    }

    @Override
    public final String toSource(ExecutionContext cx) {
        String source = this.source;
        if (source == null) {
            this.source = source = FunctionSource.toSourceString(this);
        }
        return source;
    }

    @Override
    public final FunctionObject clone(ExecutionContext cx) {
        FunctionObject clone = allocateNew();
        clone.isClone = true;
        clone.initialize(getFunctionKind(), getCode(), getEnvironment(), getExecutable());
        return clone;
    }

    /**
     * Returns a copy of this function object with the same internal methods and internal slots.
     * 
     * @param cx
     *            the execution context
     * @param newHome
     *            the new home object
     * @return the cloned function object
     */
    public final FunctionObject clone(ExecutionContext cx, ScriptObject newHome) {
        FunctionObject clone = clone(cx);
        if (getHomeObject() != null) {
            clone.toMethod(newHome);
        }
        return clone;
    }

    /**
     * Allocates a new, uninitialized copy of this function object.
     * 
     * @return a new uninitialized function object
     */
    protected abstract FunctionObject allocateNew();

    /**
     * 9.2.3 FunctionAllocate (functionPrototype, strict)
     * 
     * @param realm
     *            the realm instance
     * @param functionPrototype
     *            the prototype object
     * @param strict
     *            the strict mode flag
     * @param functionKind
     *            the function kind
     * @param constructorKind
     *            the constructor kind
     */
    protected final void allocate(Realm realm, ScriptObject functionPrototype, boolean strict,
            FunctionKind functionKind, ConstructorKind constructorKind) {
        assert this.realm == null && realm != null : "function object already allocated";
        /* step 9 */
        this.constructorKind = constructorKind;
        /* step 10 */
        this.strict = strict;
        /* step 11 */
        this.functionKind = functionKind;
        /* step 12 */
        this.setPrototype(functionPrototype);
        /* step 13 */
        // f.[[Extensible]] = true (implicit)
        /* step 14 */
        this.realm = realm;
    }

    /**
     * 9.2.4 FunctionInitialize (F, kind, ParameterList, Body, Scope)
     * 
     * @param kind
     *            the function kind
     * @param function
     *            the function code
     * @param scope
     *            the function scope
     * @param executable
     *            the source executable
     */
    protected final void initialize(FunctionKind kind, RuntimeInfo.Function function, LexicalEnvironment<?> scope,
            Executable executable) {
        assert this.function == null && function != null : "function object already initialized";
        assert this.functionKind == kind : String.format("%s != %s", functionKind, kind);
        /* step 5 */
        boolean strict = this.strict;
        /* step 6 */
        this.environment = scope;
        /* steps 7-8 */
        this.function = function;
        this.callMethod = tailCallAdapter(function, this);
        this.tailCallMethod = function.callMethod();
        this.constructMethod = tailConstructAdapter(function);
        /* steps 9-11 */
        if (kind == FunctionKind.Arrow) {
            this.thisMode = ThisMode.Lexical;
        } else if (strict) {
            this.thisMode = ThisMode.Strict;
        } else {
            this.thisMode = ThisMode.Global;
        }
        this.executable = executable;
    }

    /**
     * 9.2.10 MakeMethod ( F, homeObject)
     * 
     * @param homeObject
     *            the new home object
     */
    protected final void toMethod(ScriptObject homeObject) {
        assert homeObject != null;
        assert this.homeObject == null : "function object already method";
        this.homeObject = homeObject;
    }

    private static MethodHandle tailCallAdapter(RuntimeInfo.Function function, FunctionObject functionObject) {
        MethodHandle mh = function.callMethod();
        if (function.is(RuntimeInfo.FunctionFlags.TailCall)) {
            assert !function.isGenerator() && !function.isAsync() && function.isStrict();
            MethodHandle result = TailCallInvocation.getTailCallHandler();
            result = MethodHandles.dropArguments(result, 1, functionObject.getClass());
            result = MethodHandles.dropArguments(result, 3, Object.class, Object[].class);
            result = MethodHandles.foldArguments(result, mh);
            return result;
        }
        return mh;
    }

    private static MethodHandle tailConstructAdapter(RuntimeInfo.Function function) {
        MethodHandle mh = function.constructMethod();
        if (mh != null && function.is(RuntimeInfo.FunctionFlags.TailConstruct)) {
            assert !function.isGenerator() && !function.isAsync() && function.isStrict();
            MethodHandle result = TailCallInvocation.getTailConstructHandler();
            result = MethodHandles.dropArguments(result, 1, OrdinaryConstructorFunction.class);
            result = MethodHandles.dropArguments(result, 3, Constructor.class, Object[].class);
            result = MethodHandles.foldArguments(result, mh);
            return result;
        }
        return mh;
    }

    /**
     * [[Environment]]
     * 
     * @return the environment field
     */
    public final LexicalEnvironment<?> getEnvironment() {
        return environment;
    }

    /**
     * [[FunctionKind]]
     * 
     * @return the function kind field
     */
    public final FunctionKind getFunctionKind() {
        return functionKind;
    }

    /**
     * [[Code]]
     * 
     * @return the function code field
     */
    public final RuntimeInfo.Function getCode() {
        return function;
    }

    /**
     * Returns the method info object.
     * 
     * @return the method info object
     */
    public final Object getMethodInfo() {
        return function.methodInfo();
    }

    /**
     * Returns the executable object.
     * 
     * @return the executable object
     */
    public final Executable getExecutable() {
        return executable;
    }

    /**
     * Returns {@code true} if this is a cloned function object.
     * 
     * @return {@code true} if cloned function
     */
    public boolean isClone() {
        return isClone;
    }

    /**
     * [[Realm]]
     * 
     * @return the realm field
     */
    public final Realm getRealm() {
        return realm;
    }

    @Override
    public final Realm getRealm(ExecutionContext cx) {
        /* 7.3.22 GetFunctionRealm ( obj ) */
        return realm;
    }

    /**
     * [[ThisMode]]
     * 
     * @return the this-mode field
     */
    public final ThisMode getThisMode() {
        return thisMode;
    }

    /**
     * [[Strict]]
     * 
     * @return the strict mode field
     */
    public final boolean isStrict() {
        return strict;
    }

    /**
     * [[HomeObject]]
     * 
     * @return the home object field
     */
    public final ScriptObject getHomeObject() {
        return homeObject;
    }

    /**
     * [[ConstructorKind]]
     * 
     * @return the constructor kind field
     */
    public final ConstructorKind getConstructorKind() {
        return constructorKind;
    }

    @Override
    public String toString() {
        Source source = executable.getSourceObject().toSource();
        return String.format("%s, functionKind=%s, constructorKind=%s, thisMode=%s, cloned=%b, source=%s",
                super.toString(), functionKind, constructorKind, thisMode, isClone, source);
    }
}
