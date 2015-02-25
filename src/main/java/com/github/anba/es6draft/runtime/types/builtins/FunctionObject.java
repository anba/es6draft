/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import static com.github.anba.es6draft.runtime.types.Null.NULL;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import com.github.anba.es6draft.Executable;
import com.github.anba.es6draft.compiler.CompiledObject;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;
import com.github.anba.es6draft.runtime.internal.TailCallInvocation;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Property;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
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
    private MethodHandle tailConstructMethod;

    private Property caller = new Property(NULL, true, false, false);
    private Property arguments = new Property(NULL, true, false, false);

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

    private static boolean isNonStrictFunctionOrNull(Object v) {
        return v == NULL || (v instanceof FunctionObject && !((FunctionObject) v).isStrict());
    }

    /**
     * Returns {@code true} if legacy .caller and .arguments properties are available for this
     * function object.
     * 
     * @return {@code true} if legacy properties are supported
     */
    private final boolean isLegacy() {
        // non-strict functions have legacy support
        return !strict && !isClone && functionKind == FunctionKind.Normal
                && this instanceof OrdinaryConstructorFunction
                && realm.isEnabled(CompatibilityOption.FunctionPrototype);
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

    /**
     * Returns the {@link MethodHandle} for the function tail-construct entry method.
     * 
     * @return the tail-call method handle
     */
    public final MethodHandle getTailConstructMethod() {
        return tailConstructMethod;
    }

    /**
     * [Called from generated code]
     * 
     * @return the legacy caller value
     */
    public final Object getLegacyCaller() {
        return caller.getValue();
    }

    /**
     * [Called from generated code]
     * 
     * @return the legacy arguments value
     */
    public final Object getLegacyArguments() {
        return arguments.getValue();
    }

    /**
     * [Called from generated code]
     * 
     * @param caller
     *            the new caller value
     */
    public final void setLegacyCaller(FunctionObject caller) {
        if (caller == null || caller.isStrict()) {
            setPropertyValueIfWritable(this.caller, NULL);
        } else {
            setPropertyValueIfWritable(this.caller, caller);
        }
    }

    /**
     * [Called from generated code]
     * 
     * @param arguments
     *            the new arguments value
     */
    public final void setLegacyArguments(LegacyArgumentsObject arguments) {
        setPropertyValueIfWritable(this.arguments, arguments);
    }

    /**
     * [Called from generated code]
     * 
     * @param oldCaller
     *            the old caller value
     * @param oldArguments
     *            the old arguments value
     */
    public final void restoreLegacyProperties(Object oldCaller, Object oldArguments) {
        setPropertyValueIfWritable(this.caller, oldCaller);
        setPropertyValueIfWritable(this.arguments, oldArguments);
    }

    private void setPropertyValueIfWritable(Property property, Object value) {
        if (property.isWritable()) {
            property.setValue(value);
        }
    }

    @Override
    public final String toSource(SourceSelector selector) {
        if (selector == SourceSelector.Body) {
            return FunctionSource.toSourceString(selector, this);
        }
        // Complete source string is cached
        String source = this.source;
        if (source == null) {
            this.source = source = FunctionSource.toSourceString(selector, this);
        }
        return source;
    }

    @Override
    protected final boolean has(ExecutionContext cx, String propertyKey) {
        if (isLegacy() && ("arguments".equals(propertyKey) || "caller".equals(propertyKey))) {
            return true;
        }
        return super.has(cx, propertyKey);
    }

    @Override
    protected final boolean hasOwnProperty(ExecutionContext cx, String propertyKey) {
        if (isLegacy() && ("arguments".equals(propertyKey) || "caller".equals(propertyKey))) {
            return true;
        }
        return super.hasOwnProperty(cx, propertyKey);
    }

    /**
     * 9.2.1 [[GetOwnProperty]] (P)
     */
    @Override
    protected final Property getProperty(ExecutionContext cx, String propertyKey) {
        if (isLegacy()) {
            if ("arguments".equals(propertyKey)) {
                return arguments;
            }
            if ("caller".equals(propertyKey)) {
                assert isNonStrictFunctionOrNull(caller.getValue());
                return caller;
            }
        }
        return ordinaryGetOwnProperty(propertyKey);
    }

    @Override
    protected boolean defineProperty(ExecutionContext cx, String propertyKey,
            PropertyDescriptor desc) {
        if (isLegacy()) {
            if ("arguments".equals(propertyKey)) {
                return defineLegacyProperty(arguments, desc);
            }
            if ("caller".equals(propertyKey)) {
                return defineLegacyProperty(caller, desc);
            }
        }
        return super.defineProperty(cx, propertyKey, desc);
    }

    private boolean defineLegacyProperty(Property property, PropertyDescriptor desc) {
        // If the property descriptor is compatible and the [[Writable]] field is present, assume
        // this call to [[DefineOwnProperty]] is meant to freeze the property value. Also reset the
        // property value by setting its value to `null`, so we won't leak previous .arguments or
        // .caller objects.
        boolean compatible = IsCompatiblePropertyDescriptor(isExtensible(), desc, property);
        if (compatible && desc.hasWritable() && !desc.isWritable()) {
            property.apply(new PropertyDescriptor(NULL, false, false, false));
        }
        return compatible;
    }

    @Override
    protected boolean setPropertyValue(ExecutionContext cx, String propertyKey, Object value,
            Property current) {
        if (isLegacy()) {
            // Disallow direct [[Set]] on .arguments and .caller, but still return `true` so the
            // result value is consistent with [[DefineOwnProperty]].
            if ("arguments".equals(propertyKey) || "caller".equals(propertyKey)) {
                return true;
            }
        }
        return super.setPropertyValue(cx, propertyKey, value, current);
    }

    @Override
    protected final List<Object> getOwnPropertyKeys(ExecutionContext cx) {
        boolean isLegacy = isLegacy();
        int totalSize = countProperties(true) + (isLegacy ? 2 : 0);
        ArrayList<Object> ownKeys = new ArrayList<>(totalSize);
        appendIndexedProperties(ownKeys);
        if (isLegacy) {
            // TODO: add test case for property order
            ownKeys.add("arguments");
            ownKeys.add("caller");
        }
        appendProperties(ownKeys);
        appendSymbolProperties(ownKeys);
        return ownKeys;
    }

    @Override
    public final FunctionObject clone(ExecutionContext cx) {
        /* steps 1-3 (not applicable) */
        /* steps 4-6 */
        FunctionObject clone = allocateNew();
        clone.isClone = true;
        clone.initialize(getFunctionKind(), getCode(), getEnvironment(), getExecutable());
        /* step 7 */
        assert clone.isExtensible() : "cloned function not extensible";
        /* step 8 (not applicable) */
        /* step 9 */
        return clone;
    }

    /**
     * Allocates a new, uninitialized copy of this function object.
     * 
     * @return a new uninitialized function object
     */
    protected abstract FunctionObject allocateNew();

    /**
     * 9.2.4 FunctionAllocate (functionPrototype, strict)
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
        /* step 11 */
        this.constructorKind = constructorKind;
        /* step 12 */
        this.strict = strict;
        /* step 13 */
        this.functionKind = functionKind;
        /* step 14 */
        this.setPrototype(functionPrototype);
        /* step 15 */
        // f.[[Extensible]] = true (implicit)
        /* step 16 */
        this.realm = realm;
    }

    /**
     * 9.2.5 FunctionInitialize (F, kind, ParameterList, Body, Scope)
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
    protected final void initialize(FunctionKind kind, RuntimeInfo.Function function,
            LexicalEnvironment<?> scope, Executable executable) {
        assert this.function == null && function != null : "function object already initialized";
        assert this.functionKind == kind : String.format("%s != %s", functionKind, kind);
        assert executable instanceof CompiledObject : "Executable=" + executable;
        /* step 6 */
        boolean strict = this.strict;
        /* step 7 */
        this.environment = scope;
        /* steps 8-9 */
        this.function = function;
        this.callMethod = tailCallAdapter(function);
        this.tailCallMethod = function.callMethod();
        this.constructMethod = tailConstructAdapter(function);
        this.tailConstructMethod = dropConstructReturnType(function);
        /* steps 10-12 */
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
     * 9.2.11 MakeMethod ( F, homeObject)
     * 
     * @param homeObject
     *            the new home object
     */
    protected final void toMethod(ScriptObject homeObject) {
        assert homeObject != null;
        assert this.homeObject == null : "function object already method";
        this.homeObject = homeObject;
    }

    private static MethodHandle tailCallAdapter(RuntimeInfo.Function function) {
        MethodHandle mh = function.callMethod();
        if (function.hasTailCall()) {
            assert !function.isGenerator() && !function.isAsync() && function.isStrict();
            MethodHandle result = TailCallInvocation.getTailCallHandler();
            result = MethodHandles.dropArguments(result, 1, OrdinaryFunction.class);
            result = MethodHandles.dropArguments(result, 3, Object.class, Object[].class);
            result = MethodHandles.foldArguments(result, mh);
            return result;
        }
        return mh;
    }

    private static MethodHandle tailConstructAdapter(RuntimeInfo.Function function) {
        MethodHandle mh = function.constructMethod();
        if (mh != null && function.hasTailCall()) {
            assert !function.isGenerator() && !function.isAsync() && function.isStrict();
            MethodHandle result = TailCallInvocation.getTailConstructHandler();
            result = MethodHandles.dropArguments(result, 1, OrdinaryConstructorFunction.class);
            result = MethodHandles.dropArguments(result, 3, Constructor.class, Object[].class);
            result = MethodHandles.foldArguments(result, mh);
            return result;
        }
        return mh;
    }

    private static MethodHandle dropConstructReturnType(RuntimeInfo.Function function) {
        MethodHandle mh = function.constructMethod();
        if (mh != null && !function.isGenerator() && !function.isAsync() && !function.hasTailCall()) {
            // Non-tail-call constructor functions return ScriptObject, but in order to use
            // invokeExact() the return-type needs to be changed from ScriptObject to Object.
            return mh.asType(mh.type().changeReturnType(Object.class));
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
        return String.format("%s, functionKind=%s, constructorKind=%s, thisMode=%s, cloned=%b",
                super.toString(), functionKind, constructorKind, thisMode, isClone);
    }
}
