/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.types.Null.NULL;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.util.List;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;
import com.github.anba.es6draft.runtime.internal.SourceCompressor;
import com.github.anba.es6draft.runtime.internal.TailCallInvocation;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Property;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Symbol;

/**
 * <h1>9 Ordinary and Exotic Objects Behaviours</h1><br>
 * <ul>
 * <li>9.2 ECMAScript Function Objects
 * </ul>
 */
public abstract class FunctionObject extends OrdinaryObject implements Callable {
    private static final String SOURCE_NOT_AVAILABLE = "function F() { /* source not available */ }";

    protected static final MethodHandle uninitializedFunctionMH;
    protected static final MethodHandle uninitializedGeneratorMH;
    protected static final MethodHandle uninitializedAsyncFunctionMH;
    static {
        Lookup lookup = MethodHandles.lookup();
        try {
            MethodHandle mh = lookup.findStatic(FunctionObject.class,
                    "uninitializedFunctionObject",
                    MethodType.methodType(Object.class, ExecutionContext.class));
            mh = MethodHandles.dropArguments(mh, 1, Object.class, Object[].class);
            uninitializedFunctionMH = MethodHandles.dropArguments(mh, 0, OrdinaryFunction.class);
            uninitializedGeneratorMH = MethodHandles.dropArguments(mh, 0, OrdinaryGenerator.class);
            uninitializedAsyncFunctionMH = MethodHandles.dropArguments(mh, 0,
                    OrdinaryAsyncFunction.class);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    @SuppressWarnings("unused")
    private static final Object uninitializedFunctionObject(ExecutionContext cx) {
        throw newTypeError(cx, Messages.Key.UninitializedObject);
    }

    /** [[Environment]] */
    private LexicalEnvironment<?> environment;
    /** [[FunctionKind]] */
    private FunctionKind functionKind;
    /** [[FormalParameters]] / [[Code]] */
    private RuntimeInfo.Function function;
    /** [[Realm]] */
    private Realm realm;
    /** [[ThisMode]] */
    private ThisMode thisMode;
    /** [[Strict]] */
    private boolean strict;
    /** [[NeedsSuper]] */
    private boolean needsSuper;
    /** [[HomeObject]] */
    private ScriptObject homeObject;
    /** [[MethodName]] */
    private Object /* String|ExoticSymbol */methodName;

    private boolean isConstructor;
    private String source;

    private MethodHandle callMethod;
    private MethodHandle tailCallMethod;

    private Property caller = new Property(NULL, false, false, false);
    private Property arguments = new Property(NULL, false, false, false);

    protected FunctionObject(Realm realm) {
        super(realm);
    }

    public enum FunctionKind {
        Normal, ConstructorMethod, Method, Arrow
    }

    public enum ThisMode {
        Lexical, Strict, Global
    }

    public static boolean isStrictFunction(Object v) {
        return v instanceof FunctionObject && ((FunctionObject) v).isStrict();
    }

    private final boolean isInitialized() {
        return function != null;
    }

    /**
     * Returns {@code true} if legacy .caller and .arguments properties are available for this
     * function object.
     * 
     * @return {@code true} if legacy properties are supported
     */
    private final boolean isLegacy() {
        // TODO: 'caller' and 'arguments' properties are never updated for generator functions
        // Uninitialized and non-strict functions have legacy support
        return !(isInitialized() && strict)
                && realm.isEnabled(CompatibilityOption.FunctionPrototype);
    }

    /**
     * Returns {@code true} if the [[Construct]] method is attached to this function object.
     * 
     * @return {@code true} if this function is a constructor
     */
    protected boolean isConstructor() {
        return isConstructor;
    }

    /**
     * Sets the [[Construct]] flag for this function object.
     * 
     * @param isConstructor
     *            the new constructor flag
     */
    protected final void setConstructor(boolean isConstructor) {
        this.isConstructor = isConstructor;
    }

    /**
     * Returns the {@link MethodHandle} for the function entry method.
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
            this.caller.applyValue(NULL);
        } else {
            this.caller.applyValue(caller);
        }
    }

    /**
     * [Called from generated code]
     * 
     * @param arguments
     *            the new arguments value
     */
    public final void setLegacyArguments(ExoticLegacyArguments arguments) {
        this.arguments.applyValue(arguments);
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
        this.caller.applyValue(oldCaller);
        this.arguments.applyValue(oldArguments);
    }

    @Override
    public final String toSource() {
        if (!isInitialized()) {
            return SOURCE_NOT_AVAILABLE;
        }
        String source = this.source;
        if (source == null) {
            String src = function.source();
            if (src != null) {
                try {
                    source = SourceCompressor.decompress(src).call();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                source = SOURCE_NOT_AVAILABLE;
            }
            this.source = source;
        }
        return source;
    }

    @Override
    protected final boolean hasOwnProperty(ExecutionContext cx, String propertyKey) {
        boolean has = super.hasOwnProperty(cx, propertyKey);
        if (has) {
            return true;
        }
        if (isLegacy() && ("caller".equals(propertyKey) || "arguments".equals(propertyKey))) {
            return true;
        }
        return false;
    }

    @Override
    protected final List<Object> enumerateOwnKeys(ExecutionContext cx) {
        List<Object> ownKeys = super.enumerateOwnKeys(cx);
        if (isLegacy()) {
            if (!super.hasOwnProperty(cx, "caller")) {
                ownKeys.add("caller");
            }
            if (!super.hasOwnProperty(cx, "arguments")) {
                ownKeys.add("arguments");
            }
        }
        return ownKeys;
    }

    /**
     * 9.2.2 [[GetOwnProperty]] (P)
     */
    @Override
    public final Property getOwnProperty(ExecutionContext cx, String propertyKey) {
        Property desc = super.getOwnProperty(cx, propertyKey);
        if (desc != null) {
            return desc;
        }
        if (isLegacy()) {
            if ("caller".equals(propertyKey)) {
                assert !isStrictFunction(caller.getValue());
                return caller;
            }
            if ("arguments".equals(propertyKey)) {
                return arguments;
            }
        }
        return null;
    }

    @Override
    public final FunctionObject clone(ExecutionContext cx) {
        /* steps 1-3 (not applicable) */
        /* steps 4-6 */
        FunctionObject clone = allocateNew();
        if (isInitialized()) {
            clone.initialize(getFunctionKind(), getCode(), getEnvironment());
        }
        clone.setConstructor(isConstructor());
        /* step 7 */
        clone.setExtensible(true);
        /* step 8 (not applicable) */
        /* step 9 */
        if (isStrict()) {
            clone.addRestrictedFunctionProperties(cx);
        }
        /* step 10 */
        return clone;
    }

    /**
     * Allocates a new, uninitialized copy of this function object.
     * 
     * @return a new uninitialized function object
     */
    protected abstract FunctionObject allocateNew();

    /**
     * 9.2.3 FunctionAllocate Abstract Operation
     * 
     * @param realm
     *            the realm instance
     * @param functionPrototype
     *            the prototype object
     * @param strict
     *            the strict mode flag
     * @param kind
     *            the function kind
     * @param defaultCallMethod
     *            the default call method handle
     */
    protected final void allocate(Realm realm, ScriptObject functionPrototype, boolean strict,
            FunctionKind kind, MethodHandle defaultCallMethod) {
        assert this.realm == null && realm != null : "function object already allocated";
        this.callMethod = defaultCallMethod;
        this.tailCallMethod = defaultCallMethod;
        /* step 9 */
        this.setStrict(strict);
        /* step 10 */
        this.functionKind = kind;
        /* step 11 */
        this.setPrototype(functionPrototype);
        /* step 12 */
        // f.[[Extensible]] = true (implicit)
        /* step 13 */
        this.realm = realm;
    }

    /**
     * 9.2.5 FunctionInitialize Abstract Operation
     * 
     * @param kind
     *            the function kind
     * @param function
     *            the function code
     * @param scope
     *            the function scope
     */
    protected final void initialize(FunctionKind kind, RuntimeInfo.Function function,
            LexicalEnvironment<?> scope) {
        assert this.function == null && function != null : "function object already initialized";
        assert this.functionKind == kind : String.format("%s != %s", functionKind, kind);
        /* step 6 */
        this.environment = scope;
        /* steps 7-8 */
        this.function = function;
        this.callMethod = tailCallAdapter(function);
        this.tailCallMethod = function.callMethod();
        /* steps 9-11 */
        if (kind == FunctionKind.Arrow) {
            this.thisMode = ThisMode.Lexical;
        } else if (strict) {
            this.thisMode = ThisMode.Strict;
        } else {
            this.thisMode = ThisMode.Global;
        }
    }

    /**
     * 9.2.8 AddRestrictedFunctionProperties Abstract Operation
     * 
     * @param cx
     *            the execution context
     */
    protected final void addRestrictedFunctionProperties(ExecutionContext cx) {
        // Modified AddRestrictedFunctionProperties() to bypass .caller and .arguments properties
        // from FunctionObject

        // TODO: %ThrowTypeError% from current realm or function's realm? (current realm per spec)
        /* step 1 */
        Callable thrower = cx.getRealm().getThrowTypeError();
        /* step 2 */
        ordinaryDefinePropertyOrThrow(cx, "caller", new PropertyDescriptor(thrower, thrower, false,
                false));
        /* step 3 */
        ordinaryDefinePropertyOrThrow(cx, "arguments", new PropertyDescriptor(thrower, thrower,
                false, false));
    }

    private void ordinaryDefinePropertyOrThrow(ExecutionContext cx, String propertyKey,
            PropertyDescriptor desc) {
        /* step 1 */
        Property current = ordinaryGetOwnProperty(propertyKey);
        /* step 2 */
        boolean extensible = isExtensible();
        /* step 3 */
        boolean success = ValidateAndApplyPropertyDescriptor(this, propertyKey, extensible, desc,
                current);
        if (!success) {
            throw newTypeError(cx, Messages.Key.PropertyNotCreatable, propertyKey);
        }
    }

    /**
     * 9.2.10 MakeMethod ( F, methodName, homeObject ) Abstract Operation
     * 
     * @param methodName
     *            the new method name
     * @param homeObject
     *            the new home object
     */
    protected final void toMethod(Object methodName, ScriptObject homeObject) {
        assert isInitialized() : "uninitialized function object";
        assert !needsSuper : "function object already method";
        this.needsSuper = true;
        this.methodName = methodName;
        this.homeObject = homeObject;
    }

    /**
     * Updates the method name and home object fields.
     * 
     * @param methodName
     *            the method name
     * @param homeObject
     *            the home object
     */
    public final void updateMethod(Object methodName, ScriptObject homeObject) {
        assert isInitialized() : "uninitialized function object";
        assert needsSuper : "function object not method";
        assert methodName != null && homeObject != null;
        assert methodName instanceof String || methodName instanceof Symbol;
        this.methodName = methodName;
        this.homeObject = homeObject;
    }

    private static MethodHandle tailCallAdapter(RuntimeInfo.Function function) {
        MethodHandle mh = function.callMethod();
        if (function.hasTailCall()) {
            assert !function.isGenerator() && function.isStrict();
            MethodHandle result = TailCallInvocation.getTailCallHandler();
            result = MethodHandles.dropArguments(result, 1, OrdinaryFunction.class);
            result = MethodHandles.dropArguments(result, 3, Object.class, Object[].class);
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
     * [[Realm]]
     * 
     * @return the realm field
     */
    public final Realm getRealm() {
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
     * [[Strict]]
     * 
     * @param strict
     *            the new strict mode flag
     */
    public final void setStrict(boolean strict) {
        this.strict = strict;
    }

    /**
     * [[NeedsSuper]]
     * 
     * @return the needs-super field
     */
    public final boolean isNeedsSuper() {
        return needsSuper;
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
     * [[MethodName]]
     * 
     * @return the method name field
     */
    public final Object getMethodName() {
        return methodName;
    }
}
