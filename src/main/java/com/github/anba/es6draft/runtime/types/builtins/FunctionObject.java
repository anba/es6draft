/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.types.Null.NULL;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;

import com.github.anba.es6draft.Executable;
import com.github.anba.es6draft.compiler.CompiledObject;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.MethodLookup;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;
import com.github.anba.es6draft.runtime.internal.TailCallInvocation;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Creatable;
import com.github.anba.es6draft.runtime.types.CreateAction;
import com.github.anba.es6draft.runtime.types.Property;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;

/**
 * <h1>9 Ordinary and Exotic Objects Behaviours</h1><br>
 * <ul>
 * <li>9.2 ECMAScript Function Objects
 * </ul>
 */
public abstract class FunctionObject extends OrdinaryObject implements Callable,
        Creatable<ScriptObject> {
    protected static final MethodHandle uninitializedFunctionMH;
    protected static final MethodHandle uninitializedGeneratorMH;
    protected static final MethodHandle uninitializedAsyncFunctionMH;
    static {
        MethodHandle mh = MethodLookup.findStatic(MethodHandles.lookup(),
                "uninitializedFunctionObject",
                MethodType.methodType(Object.class, ExecutionContext.class));
        mh = MethodHandles.dropArguments(mh, 1, Object.class, Object[].class);
        uninitializedFunctionMH = MethodHandles.dropArguments(mh, 0, OrdinaryFunction.class);
        uninitializedGeneratorMH = MethodHandles.dropArguments(mh, 0, OrdinaryGenerator.class);
        uninitializedAsyncFunctionMH = MethodHandles.dropArguments(mh, 0,
                OrdinaryAsyncFunction.class);
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
    /** [[CreateAction]] */
    private CreateAction<?> createAction;

    private boolean isClone;
    private Executable executable;
    private String source;
    private MethodHandle callMethod;
    private MethodHandle tailCallMethod;

    private Property caller = new Property(NULL, false, false, false);
    private Property arguments = new Property(NULL, false, false, false);

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
        // Uninitialized and non-strict functions have legacy support
        return !(isInitialized() && strict) && !isClone
                && (functionKind == FunctionKind.Normal && this instanceof OrdinaryFunction)
                && realm.isEnabled(CompatibilityOption.FunctionPrototype);
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
            this.caller.setValue(NULL);
        } else {
            this.caller.setValue(caller);
        }
    }

    /**
     * [Called from generated code]
     * 
     * @param arguments
     *            the new arguments value
     */
    public final void setLegacyArguments(LegacyArgumentsObject arguments) {
        this.arguments.setValue(arguments);
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
        this.caller.setValue(oldCaller);
        this.arguments.setValue(oldArguments);
    }

    @Override
    public final String toSource(SourceSelector selector) {
        if (!isInitialized()) {
            return FunctionSource.noSource(selector);
        }
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

    /**
     * 9.2.1 [[GetOwnProperty]] (P)
     */
    @Override
    protected final Property getProperty(ExecutionContext cx, String propertyKey) {
        Property desc = ordinaryGetOwnProperty(propertyKey);
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
    protected final List<Object> getOwnPropertyKeys(ExecutionContext cx) {
        ArrayList<Object> ownKeys = new ArrayList<>();
        if (!indexedProperties().isEmpty()) {
            ownKeys.addAll(indexedProperties().keys());
        }
        if (!properties().isEmpty()) {
            ownKeys.addAll(properties().keySet());
        }
        if (isLegacy()) {
            // TODO: add test case for property order
            if (!ordinaryHasOwnProperty("caller")) {
                ownKeys.add("caller");
            }
            if (!ordinaryHasOwnProperty("arguments")) {
                ownKeys.add("arguments");
            }
        }
        if (!symbolProperties().isEmpty()) {
            ownKeys.addAll(symbolProperties().keySet());
        }
        return ownKeys;
    }

    @Override
    public final FunctionObject clone(ExecutionContext cx) {
        /* steps 1-3 (not applicable) */
        /* steps 4-6 */
        FunctionObject clone = allocateNew();
        clone.isClone = true;
        clone.createAction = createAction;
        if (isInitialized()) {
            clone.initialize(getFunctionKind(), isStrict(), getCode(), getEnvironment(),
                    getExecutable());
        }
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
     * 9.2.4 FunctionAllocate (functionPrototype, strict) Abstract Operation
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
        this.strict = strict;
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
     * 9.2.5 FunctionInitialize (F, kind, Strict, ParameterList, Body, Scope) Abstract Operation
     * 
     * @param kind
     *            the function kind
     * @param strict
     *            the strict mode flag
     * @param function
     *            the function code
     * @param scope
     *            the function scope
     * @param executable
     *            the source executable
     */
    protected final void initialize(FunctionKind kind, boolean strict,
            RuntimeInfo.Function function, LexicalEnvironment<?> scope, Executable executable) {
        assert this.function == null && function != null : "function object already initialized";
        assert this.functionKind == kind : String.format("%s != %s", functionKind, kind);
        assert executable instanceof CompiledObject : "Executable=" + executable;
        /* step 6 */
        this.strict = strict;
        /* step 7 */
        this.environment = scope;
        /* steps 8-9 */
        this.function = function;
        this.callMethod = tailCallAdapter(function);
        this.tailCallMethod = function.callMethod();
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

    protected final boolean infallibleDefineOwnProperty(String propertyKey, PropertyDescriptor desc) {
        // Same as ordinaryDefineOwnProperty(), except infallible ordinaryGetOwnProperty() is used.
        /* step 1 */
        Property current = ordinaryGetOwnProperty(propertyKey);
        /* step 2 */
        boolean extensible = isExtensible();
        /* step 3 */
        return ValidateAndApplyPropertyDescriptor(this, propertyKey, extensible, desc, current);
    }

    /**
     * 9.2.10 MakeMethod ( F, homeObject ) Abstract Operation
     * 
     * @param homeObject
     *            the new home object
     */
    protected final void toMethod(ScriptObject homeObject) {
        assert isInitialized() : "uninitialized function object";
        assert !needsSuper : "function object already method";
        this.needsSuper = true;
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
        /* 7.3.21 GetFunctionRealm ( obj ) Abstract Operation */
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
     * [[HomeObject]]
     * 
     * @param homeObject
     *            the new home object
     */
    public final void setHomeObject(OrdinaryObject homeObject) {
        assert isInitialized() : "uninitialized function object";
        assert needsSuper : "function object not method";
        assert homeObject != null;
        this.homeObject = homeObject;
    }

    /**
     * [[CreateAction]]
     * 
     * @return the create action operation
     */
    @Override
    public final CreateAction<?> createAction() {
        return createAction;
    }

    /**
     * [[CreateAction]]
     * 
     * @param createAction
     *            the new create action operation for this function
     */
    protected final void setCreateAction(CreateAction<?> createAction) {
        assert this instanceof Constructor : "[[CreateAction]] set on non-Constructor";
        // assert this.createAction == null : "[[CreateAction]] already defined";
        assert createAction != null;
        this.createAction = createAction;
    }

    @Override
    public String toString() {
        return String.format("%s, kind=%s, mode=%s, create=%s, cloned=%b", super.toString(),
                functionKind, thisMode, classNameWithEnclosing(createAction), isClone);
    }

    private static String classNameWithEnclosing(Object object) {
        if (object == null) {
            return "<null>";
        }
        Class<?> clazz = object.getClass();
        Class<?> enclosing = clazz.getEnclosingClass();
        if (enclosing == null) {
            return clazz.getSimpleName();
        }
        return enclosing.getSimpleName() + "." + clazz.getSimpleName();
    }
}
