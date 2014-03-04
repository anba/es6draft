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
        throw newTypeError(cx, Messages.Key.UninitialisedObject);
    }

    /** [[Environment]] */
    private LexicalEnvironment environment;
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

    private final boolean isInitialised() {
        return function != null;
    }

    /**
     * Returns {@code true} iff legacy .caller and .arguments properties are available for this
     * function object
     */
    private final boolean isLegacy() {
        // TODO: 'caller' and 'arguments' properties are never updated for generator functions
        // Uninitialised and non-strict functions have legacy support
        return !(isInitialised() && strict)
                && realm.isEnabled(CompatibilityOption.FunctionPrototype);
    }

    /**
     * Returns {@code true} iff the [[Construct]] method is attached to this function object
     */
    protected boolean isConstructor() {
        return isConstructor;
    }

    /**
     * Sets the [[Construct]] flag for this function object
     */
    protected final void setConstructor(boolean isConstructor) {
        this.isConstructor = isConstructor;
    }

    /**
     * Returns the {@link MethodHandle} for the function entry method
     */
    public final MethodHandle getCallMethod() {
        return callMethod;
    }

    /**
     * Returns the {@link MethodHandle} for the function tail-call entry method
     */
    public final MethodHandle getTailCallMethod() {
        return tailCallMethod;
    }

    /**
     * [Called from generated code]
     */
    public final Object getLegacyCaller() {
        return caller.getValue();
    }

    /**
     * [Called from generated code]
     */
    public final Object getLegacyArguments() {
        return arguments.getValue();
    }

    /**
     * [Called from generated code]
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
     */
    public final void setLegacyArguments(ExoticArguments arguments) {
        this.arguments.applyValue(arguments);
    }

    /**
     * [Called from generated code]
     */
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
    protected boolean hasOwnProperty(String propertyKey) {
        boolean has = super.hasOwnProperty(propertyKey);
        if (has) {
            return true;
        }
        if (isLegacy() && ("caller".equals(propertyKey) || "arguments".equals(propertyKey))) {
            return true;
        }
        return false;
    }

    @Override
    protected List<Object> enumerateOwnKeys() {
        List<Object> ownKeys = super.enumerateOwnKeys();
        if (isLegacy()) {
            if (!super.hasOwnProperty("caller")) {
                ownKeys.add("caller");
            }
            if (!super.hasOwnProperty("arguments")) {
                ownKeys.add("arguments");
            }
        }
        return ownKeys;
    }

    /**
     * 9.2.2 [[GetOwnProperty]] (P)
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
     * {@code newHomeObject} and the [[MethodName]] property set to {@code newMethodName}
     */
    protected final FunctionObject clone(ScriptObject newHomeObject, Object newMethodName) {
        FunctionObject clone = allocateNew();
        if (isInitialised()) {
            clone.initialise(getFunctionKind(), getCode(), getEnvironment());
        }
        clone.inheritProperties(this);
        clone.setExtensible(isExtensible());
        clone.setConstructor(isConstructor());
        if (isNeedsSuper()) {
            assert isInitialised() : "uninitialised function object with [[NeedsSuper]] = true";
            if (newMethodName != null) {
                clone.toMethod(newMethodName, newHomeObject);
            } else {
                clone.toMethod(getMethodName(), newHomeObject);
            }
        }
        return clone;
    }

    /**
     * Allocates a new, uninitialised copy of this function object
     */
    protected abstract FunctionObject allocateNew();

    /**
     * 9.2.3 FunctionAllocate Abstract Operation
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
     * 9.2.5 FunctionInitialise Abstract Operation
     */
    protected final void initialise(FunctionKind kind, RuntimeInfo.Function function,
            LexicalEnvironment scope) {
        assert this.function == null && function != null : "function object already initialised";
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
     * 9.2.10 MakeMethod ( F, methodName, homeObject ) Abstract Operation
     */
    protected final void toMethod(Object methodName, ScriptObject homeObject) {
        assert isInitialised() : "uninitialised function object";
        assert !needsSuper : "function object already method";
        this.needsSuper = true;
        this.methodName = methodName;
        this.homeObject = homeObject;
    }

    /**
     * 
     */
    public final void updateMethod(Object methodName, ScriptObject homeObject) {
        assert isInitialised() : "uninitialised function object";
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
     */
    public final LexicalEnvironment getEnvironment() {
        return environment;
    }

    /**
     * [[FunctionKind]]
     */
    public final FunctionKind getFunctionKind() {
        return functionKind;
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
        this.strict = strict;
    }

    /**
     * [[NeedsSuper]]
     */
    public final boolean isNeedsSuper() {
        return needsSuper;
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
