/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import static com.github.anba.es6draft.runtime.internal.Errors.throwTypeError;
import static com.github.anba.es6draft.runtime.types.Null.NULL;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.util.Collection;

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

/**
 * <h1>9 Ordinary and Exotic Objects Behaviours</h1><br>
 * <ul>
 * <li>9.2 ECMAScript Function Objects
 * </ul>
 */
public abstract class FunctionObject extends OrdinaryObject implements Callable {
    private static final String SOURCE_NOT_AVAILABLE = "function F() { /* source not available */ }";

    protected static final MethodHandle uninitialisedFunctionMH;
    protected static final MethodHandle uninitialisedGeneratorMH;
    static {
        Lookup lookup = MethodHandles.lookup();
        try {
            MethodHandle mh = lookup.findStatic(FunctionObject.class,
                    "uninitialisedFunctionObject",
                    MethodType.methodType(Object.class, ExecutionContext.class));
            mh = MethodHandles.dropArguments(mh, 1, Object.class, Object[].class);
            uninitialisedFunctionMH = MethodHandles.dropArguments(mh, 0, OrdinaryFunction.class);
            uninitialisedGeneratorMH = MethodHandles.dropArguments(mh, 0, OrdinaryGenerator.class);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    @SuppressWarnings("unused")
    private static final Object uninitialisedFunctionObject(ExecutionContext cx) {
        throw throwTypeError(cx, Messages.Key.UninitialisedObject);
    }

    /** [[Scope]] */
    private LexicalEnvironment scope;
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
    /** [[HomeObject]] */
    private ScriptObject homeObject;
    /** [[MethodName]] */
    private Object /* String|ExoticSymbol */methodName;

    protected boolean isConstructor = false;
    private boolean legacy;
    private String source = null;

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

    public final MethodHandle getCallMethod() {
        return callMethod;
    }

    public final MethodHandle getTailCallMethod() {
        return tailCallMethod;
    }

    public final Object getLegacyCaller() {
        return caller.getValue();
    }

    public final Object getLegacyArguments() {
        return arguments.getValue();
    }

    public final void setLegacyCaller(FunctionObject caller) {
        if (caller == null || caller.isStrict()) {
            this.caller.applyValue(NULL);
        } else {
            this.caller.applyValue(caller);
        }
    }

    public final void setLegacyArguments(ExoticArguments arguments) {
        this.arguments.applyValue(arguments);
    }

    public final void restoreLegacyProperties(Object oldCaller, Object oldArguments) {
        this.caller.applyValue(oldCaller);
        this.arguments.applyValue(oldArguments);
    }

    @Override
    public final String toSource() {
        if (!isInitialised()) {
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
    public boolean defineOwnProperty(ExecutionContext cx, String propertyKey,
            PropertyDescriptor desc) {
        Property current = getOwnProperty(cx, propertyKey);
        boolean extensible = isExtensible();
        return ValidateAndApplyPropertyDescriptor(this, propertyKey, extensible, desc, current);
    }

    @Override
    protected boolean hasOwnProperty(ExecutionContext cx, String propertyKey) {
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
    protected Collection<Object> enumerateOwnKeys() {
        Collection<Object> ownKeys = super.enumerateOwnKeys();
        if (isLegacy()) {
            ownKeys.add("caller");
            ownKeys.add("arguments");
        }
        return ownKeys;
    }

    /**
     * 9.2.3 [[GetOwnProperty]] (P)
     */
    @Override
    public Property getOwnProperty(ExecutionContext cx, String propertyKey) {
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

    /**
     * Returns a copy of this function object with the [[HomeObject]] property set to
     * {@code newHomeObject}
     */
    protected final FunctionObject rebind(ScriptObject newHomeObject) {
        assert isInitialised() : "uninitialised function object";
        Object methodName = getMethodName();
        FunctionObject copy = allocateCopy();
        copy.initialise(getFunctionKind(), getFunction(), getScope(), newHomeObject, methodName);
        copy.isConstructor = isConstructor;
        return copy;
    }

    /**
     * Allocates a new, uninitialised copy of this function object
     */
    protected abstract FunctionObject allocateCopy();

    /**
     * 9.2.5 FunctionAllocate Abstract Operation
     */
    protected final void allocate(Realm realm, ScriptObject functionPrototype, boolean strict,
            FunctionKind kind, MethodHandle defaultCallMethod) {
        this.callMethod = defaultCallMethod;
        this.tailCallMethod = defaultCallMethod;
        /* step 13 (moved) */
        this.realm = realm;
        /* step 9 */
        this.setStrict(strict);
        /* step 10 */
        this.functionKind = kind;
        /* step 11 */
        this.setPrototype(functionPrototype);
        /* step 12 */
        // f.[[Extensible]] = true (implicit)
    }

    /**
     * 9.2.6 FunctionInitialise Abstract Operation
     */
    protected final void initialise(FunctionKind kind, RuntimeInfo.Function function,
            LexicalEnvironment scope, ScriptObject homeObject, Object methodName) {
        assert this.function == null && function != null;
        /* step 6 */
        this.scope = scope;
        /* steps 7-8 */
        this.function = function;
        this.callMethod = tailCallAdapter(function);
        this.tailCallMethod = function.callMethod();
        /* step 9 */
        this.homeObject = homeObject;
        this.methodName = methodName;
        /* steps 10-12 */
        if (kind == FunctionKind.Arrow) {
            this.thisMode = ThisMode.Lexical;
        } else if (strict) {
            this.thisMode = ThisMode.Strict;
        } else {
            this.thisMode = ThisMode.Global;
        }
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

    public final FunctionKind getFunctionKind() {
        return functionKind;
    }

    protected final RuntimeInfo.Function getFunction() {
        return function;
    }

    protected final boolean isInitialised() {
        return function != null;
    }

    public final boolean isLegacy() {
        return legacy;
    }

    /**
     * [[Scope]]
     */
    public final LexicalEnvironment getScope() {
        return scope;
    }

    /**
     * [[Code]]
     */
    public final RuntimeInfo.Function getCode() {
        return function;
    }

    /**
     * [[Realm]]
     */
    public final Realm getRealm() {
        return realm;
    }

    /**
     * [[ThisMode]]
     */
    public final ThisMode getThisMode() {
        return thisMode;
    }

    /**
     * [[Strict]]
     */
    public final boolean isStrict() {
        return strict;
    }

    /**
     * [[Strict]]
     */
    public final void setStrict(boolean strict) {
        assert realm != null : "[[Realm]] not set";
        this.strict = strict;
        // support for legacy 'caller' and 'arguments' properties
        // TODO: 'caller' and 'arguments' properties are never updated for generator functions
        this.legacy = !strict && realm.isEnabled(CompatibilityOption.FunctionPrototype);
    }

    /**
     * [[HomeObject]]
     */
    public final ScriptObject getHomeObject() {
        return homeObject;
    }

    /**
     * [[MethodName]]
     */
    public final Object getMethodName() {
        return methodName;
    }
}
