/**
 * Copyright (c) André Bargull
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
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;
import com.github.anba.es6draft.runtime.internal.Source;
import com.github.anba.es6draft.runtime.internal.TailCallInvocation;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.PrivateName;
import com.github.anba.es6draft.runtime.types.Property;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Symbol;

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
        return StringObject.validateLength(cx, source);
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
        Source source = executable.getSource();
        return String.format("%s, functionKind=%s, constructorKind=%s, thisMode=%s, source=%s", super.toString(),
                functionKind, constructorKind, thisMode, source);
    }

    /* ***************************************************************************************** */

    /**
     * 9.2.3 FunctionAllocate (functionPrototype, strict, functionKind)
     * 
     * @param <FUNCTION>
     *            the function type
     * @param cx
     *            the execution context
     * @param allocator
     *            the function allocator
     * @param functionPrototype
     *            the function prototype
     * @param strict
     *            the strict mode flag
     * @param functionKind
     *            the function kind
     * @return the new function object
     */
    public static <FUNCTION extends FunctionObject> FUNCTION FunctionAllocate(ExecutionContext cx,
            ObjectAllocator<FUNCTION> allocator, ScriptObject functionPrototype, boolean strict,
            FunctionKind functionKind) {
        return FunctionAllocate(cx, allocator, functionPrototype, strict, functionKind, ConstructorKind.Base);
    }

    /**
     * 9.2.3 FunctionAllocate (functionPrototype, strict, functionKind)
     * 
     * @param <FUNCTION>
     *            the function type
     * @param cx
     *            the execution context
     * @param allocator
     *            the function allocator
     * @param functionPrototype
     *            the function prototype
     * @param strict
     *            the strict mode flag
     * @param functionKind
     *            the function kind
     * @param constructorKind
     *            the constructor kind
     * @return the new function object
     */
    public static <FUNCTION extends FunctionObject> FUNCTION FunctionAllocate(ExecutionContext cx,
            ObjectAllocator<FUNCTION> allocator, ScriptObject functionPrototype, boolean strict,
            FunctionKind functionKind, ConstructorKind constructorKind) {
        assert constructorKind != ConstructorKind.Derived || functionKind == FunctionKind.ClassConstructor;
        Realm realm = cx.getRealm();
        /* steps 1-5 (implicit) */
        /* steps 6-9 */
        FUNCTION f = allocator.newInstance(realm);
        FunctionObject fo = (FunctionObject) f;
        fo.constructorKind = constructorKind;
        /* step 10 */
        fo.strict = strict;
        /* step 11 */
        fo.functionKind = functionKind;
        /* step 12 */
        fo.setPrototype(functionPrototype);
        /* step 13 */
        // f.[[Extensible]] = true (implicit)
        /* step 14 */
        fo.realm = realm;
        /* step 15 */
        return f;
    }

    /**
     * 9.2.4 FunctionInitialize (F, kind, ParameterList, Body, Scope)
     * 
     * @param <FUNCTION>
     *            the function type
     * @param f
     *            the function object
     * @param kind
     *            the function kind
     * @param function
     *            the function code
     * @param scope
     *            the lexical environment
     * @param executable
     *            the executable object
     * @return the function object
     */
    public static <FUNCTION extends FunctionObject> FUNCTION FunctionInitialize(FUNCTION f, FunctionKind kind,
            RuntimeInfo.Function function, LexicalEnvironment<?> scope, Executable executable) {
        FunctionObject fo = (FunctionObject) f;
        assert fo.function == null && function != null : "function object already initialized";
        assert fo.functionKind == kind : String.format("%s != %s", fo.functionKind, kind);
        /* step 1 */
        assert f.isExtensible() && !f.ordinaryHasOwnProperty("length");
        /* step 2 */
        int len = function.expectedArgumentCount();
        /* step 3 */
        f.infallibleDefineOwnProperty("length", new Property(len, false, false, true));
        /* step 4 */
        boolean strict = fo.strict;
        /* step 5 */
        fo.environment = scope;
        /* steps 6-8 */
        fo.function = function;
        fo.callMethod = tailCallAdapter(function, f);
        fo.tailCallMethod = function.callMethod();
        fo.constructMethod = tailConstructAdapter(function, f);
        /* steps 9-11 */
        if (kind == FunctionKind.Arrow) {
            fo.thisMode = ThisMode.Lexical;
        } else if (strict) {
            fo.thisMode = ThisMode.Strict;
        } else {
            fo.thisMode = ThisMode.Global;
        }
        fo.executable = executable;
        /* step 12 */
        return f;
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

    private static MethodHandle tailConstructAdapter(RuntimeInfo.Function function, FunctionObject functionObject) {
        MethodHandle mh = function.constructMethod();
        if (mh != null && function.is(RuntimeInfo.FunctionFlags.TailConstruct)) {
            assert !function.isGenerator() && !function.isAsync() && function.isStrict();
            assert functionObject.getClass() == OrdinaryConstructorFunction.class;
            MethodHandle result = TailCallInvocation.getTailConstructHandler();
            result = MethodHandles.dropArguments(result, 1, OrdinaryConstructorFunction.class);
            result = MethodHandles.dropArguments(result, 3, Constructor.class, Object[].class);
            result = MethodHandles.foldArguments(result, mh);
            return result;
        }
        return mh;
    }

    /**
     * 9.2.7 AddRestrictedFunctionProperties ( F, realm )
     * 
     * @param <FUNCTION>
     *            the function type
     * @param f
     *            the function object
     * @param realm
     *            the realm object
     */
    public static <FUNCTION extends OrdinaryObject & Callable> void AddRestrictedFunctionProperties(FUNCTION f,
            Realm realm) {
        /* steps 1-2 */
        Callable thrower = realm.getThrowTypeError();
        /* step 3 */
        f.infallibleDefineOwnProperty("caller", new Property(thrower, thrower, false, true));
        /* step 4 */
        f.infallibleDefineOwnProperty("arguments", new Property(thrower, thrower, false, true));
    }

    /**
     * 9.2.8 MakeConstructor (F, writablePrototype, prototype)
     * 
     * @param <CONSTRUCTOR>
     *            the constructor function type
     * @param cx
     *            the execution context
     * @param f
     *            the function object
     */
    public static <CONSTRUCTOR extends FunctionObject & Constructor> void MakeConstructor(ExecutionContext cx,
            CONSTRUCTOR f) {
        /* steps 1-2 (not applicable) */
        /* step 3 */
        assert f.isExtensible() && !f.ordinaryHasOwnProperty("prototype");
        /* step 4 */
        boolean writablePrototype = true;
        /* step 5.a */
        OrdinaryObject prototype = ObjectCreate(cx, Intrinsics.ObjectPrototype);
        /* step 5.b */
        prototype.infallibleDefineOwnProperty("constructor", new Property(f, writablePrototype, false, true));
        /* step 6 */
        f.infallibleDefineOwnProperty("prototype", new Property(prototype, writablePrototype, false, false));
        /* step 7 (return) */
    }

    /**
     * 9.2.8 MakeConstructor (F, writablePrototype, prototype)
     * 
     * @param <CONSTRUCTOR>
     *            the constructor function type
     * @param f
     *            the function object
     * @param writablePrototype
     *            the writable flag for the .prototype property
     * @param prototype
     *            the prototype object
     */
    public static <CONSTRUCTOR extends FunctionObject & Constructor> void MakeConstructor(CONSTRUCTOR f,
            boolean writablePrototype, ScriptObject prototype) {
        /* steps 1-2 (not applicable) */
        /* step 3 */
        assert f.isExtensible() && !f.ordinaryHasOwnProperty("prototype");
        /* steps 4-5 (not applicable) */
        /* step 6 */
        f.infallibleDefineOwnProperty("prototype", new Property(prototype, writablePrototype, false, false));
        /* step 7 (return) */
    }

    /**
     * 9.2.9 MakeClassConstructor (F)
     * 
     * @param f
     *            the function object
     */
    public static void MakeClassConstructor(OrdinaryConstructorFunction f) {
        /* step 1 (not applicable) */
        /* steps 2-3 */
        assert f.getFunctionKind() == FunctionKind.ClassConstructor;
        /* step 4 (return) */
    }

    /**
     * 9.2.10 MakeMethod (F, homeObject)
     * 
     * @param f
     *            the function object
     * @param homeObject
     *            the home object
     */
    public static void MakeMethod(FunctionObject f, ScriptObject homeObject) {
        assert homeObject != null;
        assert f.homeObject == null : "function object already method";
        /* steps 1-2 (not applicable) */
        /* step 3 */
        f.homeObject = homeObject;
        /* step 4 (return) */
    }

    /**
     * 9.2.11 SetFunctionName (F, name, prefix)
     * 
     * @param <FUNCTION>
     *            the function type
     * @param f
     *            the function object
     * @param name
     *            the function name
     */
    public static <FUNCTION extends OrdinaryObject & Callable> void SetFunctionName(FUNCTION f, String name) {
        SetFunctionName(f, name, null);
    }

    /**
     * 9.2.11 SetFunctionName (F, name, prefix)
     * 
     * @param <FUNCTION>
     *            the function type
     * @param f
     *            the function object
     * @param name
     *            the function name
     * @param prefix
     *            the function name prefix
     */
    public static <FUNCTION extends OrdinaryObject & Callable> void SetFunctionName(FUNCTION f, String name,
            String prefix) {
        /* step 1 */
        assert f.isExtensible() && !f.ordinaryHasOwnProperty("name");
        /* steps 2-4 (not applicable) */
        /* step 5 */
        if (prefix != null) {
            name = prefix + " " + name;
        }
        /* step 6 */
        assert name.length() <= StringObject.MAX_LENGTH;
        f.infallibleDefineOwnProperty("name", new Property(name, false, false, true));
    }

    /**
     * 9.2.11 SetFunctionName (F, name, prefix)
     * 
     * @param <FUNCTION>
     *            the function type
     * @param f
     *            the function object
     * @param name
     *            the function name
     */
    public static <FUNCTION extends OrdinaryObject & Callable> void SetFunctionName(FUNCTION f, Symbol name) {
        SetFunctionName(f, name, null);
    }

    /**
     * 9.2.11 SetFunctionName (F, name, prefix)
     * 
     * @param <FUNCTION>
     *            the function type
     * @param f
     *            the function object
     * @param name
     *            the function name
     * @param prefix
     *            the function name prefix
     */
    public static <FUNCTION extends OrdinaryObject & Callable> void SetFunctionName(FUNCTION f, Symbol name,
            String prefix) {
        /* step 1 */
        assert f.isExtensible() && !f.ordinaryHasOwnProperty("name");
        /* steps 2-3 (not applicable) */
        /* step 4 */
        String description = name.getDescription();
        String sname = description == null ? "" : "[" + description + "]";
        /* step 5 */
        if (prefix != null) {
            sname = prefix + " " + sname;
        }
        /* step 6 */
        assert sname.length() <= StringObject.MAX_LENGTH;
        f.infallibleDefineOwnProperty("name", new Property(sname, false, false, true));
    }

    /**
     * 9.2.11 SetFunctionName (F, name, prefix)
     * 
     * @param <FUNCTION>
     *            the function type
     * @param f
     *            the function object
     * @param name
     *            the function name
     */
    public static <FUNCTION extends OrdinaryObject & Callable> void SetFunctionName(FUNCTION f, PrivateName name) {
        SetFunctionName(f, name, null);
    }

    /**
     * 9.2.11 SetFunctionName (F, name, prefix)
     * 
     * @param <FUNCTION>
     *            the function type
     * @param f
     *            the function object
     * @param name
     *            the function name
     * @param prefix
     *            the function name prefix
     */
    public static <FUNCTION extends OrdinaryObject & Callable> void SetFunctionName(FUNCTION f, PrivateName name,
            String prefix) {
        /* step 1 */
        assert f.isExtensible() && !f.ordinaryHasOwnProperty("name");
        /* steps 2-4 (not applicable) */
        /* step 5 */
        String sname = name.toString();
        /* step 6 */
        if (prefix != null) {
            sname = prefix + " " + sname;
        }
        /* step 7 */
        assert sname.length() <= StringObject.MAX_LENGTH;
        f.infallibleDefineOwnProperty("name", new Property(sname, false, false, true));
    }

    /**
     * 9.2.11 SetFunctionName (F, name, prefix)
     * 
     * @param <FUNCTION>
     *            the function type
     * @param f
     *            the function object
     * @param name
     *            the function name
     */
    public static <FUNCTION extends OrdinaryObject & Callable> void SetFunctionName(FUNCTION f, Object name) {
        if (name instanceof String) {
            SetFunctionName(f, (String) name, null);
        } else if (name instanceof Symbol) {
            SetFunctionName(f, (Symbol) name, null);
        } else {
            assert name instanceof PrivateName;
            SetFunctionName(f, (PrivateName) name, null);
        }
    }

    /**
     * 9.2.11 SetFunctionName (F, name, prefix)
     * 
     * @param <FUNCTION>
     *            the function type
     * @param f
     *            the function object
     * @param name
     *            the function name
     * @param prefix
     *            the function name prefix
     */
    public static <FUNCTION extends OrdinaryObject & Callable> void SetFunctionName(FUNCTION f, Object name,
            String prefix) {
        if (name instanceof String) {
            SetFunctionName(f, (String) name, prefix);
        } else if (name instanceof Symbol) {
            SetFunctionName(f, (Symbol) name, prefix);
        } else {
            assert name instanceof PrivateName;
            SetFunctionName(f, (PrivateName) name, prefix);
        }
    }
}
