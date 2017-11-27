/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import static com.github.anba.es6draft.runtime.types.Null.NULL;

import java.util.List;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Property;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;

/**
 * <h1>9 Ordinary and Exotic Objects Behaviours</h1><br>
 * <ul>
 * <li>9.2 ECMAScript Function Objects
 * </ul>
 */
public final class LegacyConstructorFunction extends FunctionObject implements Constructor {
    private FunctionObject caller;
    private Arguments arguments;
    private boolean callerWritable = true;
    private boolean argumentsWritable = true;

    public static final class Arguments {
        private final Object[] arguments;

        public Arguments(Object[] arguments) {
            this.arguments = arguments;
        }

        ArgumentsObject createArgumentsObject(ExecutionContext cx, LegacyConstructorFunction callee) {
            return ArgumentsObject.CreateMappedArgumentsObject(cx, callee, arguments);
        }
    }

    /**
     * Constructs a new legacy Constructor Function object.
     * 
     * @param realm
     *            the realm object
     */
    public LegacyConstructorFunction(Realm realm) {
        super(realm);
    }

    private static boolean isNonStrictFunctionOrNull(FunctionObject v) {
        return v == null || !v.isStrict();
    }

    /**
     * Returns {@code true} if legacy .arguments property is available for this function object.
     * 
     * @return {@code true} if legacy .arguments is supported
     */
    private boolean hasArguments() {
        return getRealm().getRuntimeContext().isEnabled(CompatibilityOption.FunctionArguments);
    }

    /**
     * Returns {@code true} if legacy .caller property is available for this function object.
     * 
     * @return {@code true} if legacy .caller is supported
     */
    private boolean hasCaller() {
        return getRealm().getRuntimeContext().isEnabled(CompatibilityOption.FunctionCaller);
    }

    /**
     * [Called from generated code]
     * 
     * @return the legacy caller value
     */
    public FunctionObject getLegacyCaller() {
        return caller;
    }

    /**
     * [Called from generated code]
     * 
     * @return the legacy arguments value
     */
    public Arguments getLegacyArguments() {
        return arguments;
    }

    /**
     * [Called from generated code]
     * 
     * @param caller
     *            the new caller value
     */
    public void setLegacyCaller(FunctionObject caller) {
        if (callerWritable) {
            this.caller = isNonStrictFunctionOrNull(caller) ? caller : null;
        }
    }

    /**
     * [Called from generated code]
     * 
     * @param arguments
     *            the new arguments value
     */
    public void setLegacyArguments(Arguments arguments) {
        if (argumentsWritable) {
            this.arguments = arguments;
        }
    }

    private Property callerProperty() {
        Object callerObj = caller != null ? caller : NULL;
        return new Property(callerObj, callerWritable, false, false);
    }

    private Property argumentsProperty(ExecutionContext cx) {
        Object argumentsObj = arguments != null ? arguments.createArgumentsObject(cx, this) : NULL;
        return new Property(argumentsObj, argumentsWritable, false, false);
    }

    @Override
    public boolean hasOwnProperty(ExecutionContext cx, String propertyKey) {
        if (("arguments".equals(propertyKey) && hasArguments()) || ("caller".equals(propertyKey) && hasCaller())) {
            return true;
        }
        return super.hasOwnProperty(cx, propertyKey);
    }

    @Override
    public Property getOwnProperty(ExecutionContext cx, String propertyKey) {
        if ("arguments".equals(propertyKey) && hasArguments()) {
            return argumentsProperty(cx);
        }
        if ("caller".equals(propertyKey) && hasCaller()) {
            assert isNonStrictFunctionOrNull(caller);
            return callerProperty();
        }
        return ordinaryGetOwnProperty(propertyKey);
    }

    @Override
    public boolean defineOwnProperty(ExecutionContext cx, String propertyKey, PropertyDescriptor desc) {
        // If the property descriptor is compatible and the [[Writable]] field is present, assume
        // this call to [[DefineOwnProperty]] is meant to freeze the property value. Also reset the
        // property value by setting its value to `null`, so we won't leak previous .arguments or
        // .caller objects.
        if ("arguments".equals(propertyKey) && hasArguments()) {
            boolean compatible = IsCompatiblePropertyDescriptor(isExtensible(), desc, argumentsProperty(cx));
            if (compatible && desc.hasWritable() && !desc.isWritable()) {
                arguments = null;
                argumentsWritable = false;
            }
            return compatible;
        }
        if ("caller".equals(propertyKey) && hasCaller()) {
            boolean compatible = IsCompatiblePropertyDescriptor(isExtensible(), desc, callerProperty());
            if (compatible && desc.hasWritable() && !desc.isWritable()) {
                caller = null;
                callerWritable = false;
            }
            return compatible;
        }
        return super.defineOwnProperty(cx, propertyKey, desc);
    }

    @Override
    public boolean set(ExecutionContext cx, String propertyKey, Object value, Object receiver) {
        // Disallow direct [[Set]] on .arguments and .caller, but still return `true` so the
        // result value is consistent with [[DefineOwnProperty]].
        if (receiver == this && "arguments".equals(propertyKey) && hasArguments()) {
            return argumentsWritable;
        }
        if (receiver == this && "caller".equals(propertyKey) && hasCaller()) {
            return callerWritable;
        }
        return super.set(cx, propertyKey, value, receiver);
    }

    @Override
    public Enumerability isEnumerableOwnProperty(ExecutionContext cx, String propertyKey) {
        // Overridden to avoid creating the arguments object, cf. getOwnProperty(...) method above.
        if ("arguments".equals(propertyKey) && hasArguments()) {
            return Enumerability.NonEnumerable;
        }
        if ("caller".equals(propertyKey) && hasCaller()) {
            return Enumerability.NonEnumerable;
        }
        return super.isEnumerableOwnProperty(cx, propertyKey);
    }

    @Override
    protected void ownPropertyNames(List<? super String> list) {
        // TODO: add test case for property order
        if (hasArguments()) {
            list.add("arguments");
        }
        if (hasCaller()) {
            list.add("caller");
        }
        super.ownPropertyNames(list);
    }

    /**
     * 9.2.1 [[Call]] (thisArgument, argumentsList)
     */
    @Override
    public Object call(ExecutionContext callerContext, Object thisValue, Object... argumentsList) {
        try {
            return getCallMethod().invokeExact(this, callerContext, thisValue, argumentsList);
        } catch (Throwable e) {
            throw FunctionObject.<RuntimeException> rethrow(e);
        }
    }

    /**
     * 9.2.1 [[Call]] (thisArgument, argumentsList)
     */
    @Override
    public Object tailCall(ExecutionContext callerContext, Object thisValue, Object... argumentsList) throws Throwable {
        return getTailCallMethod().invokeExact(this, callerContext, thisValue, argumentsList);
    }

    /**
     * 9.2.2 [[Construct]] (argumentsList)
     */
    @Override
    public ScriptObject construct(ExecutionContext callerContext, Constructor newTarget, Object... argumentsList) {
        try {
            return (ScriptObject) getConstructMethod().invokeExact(this, callerContext, newTarget, argumentsList);
        } catch (Throwable e) {
            throw FunctionObject.<RuntimeException> rethrow(e);
        }
    }

    /**
     * 9.2.5 FunctionCreate (kind, ParameterList, Body, Scope, Strict)
     * 
     * @param cx
     *            the execution context
     * @param function
     *            the function code
     * @param scope
     *            the lexical environment
     * @return the new function object
     */
    public static LegacyConstructorFunction LegacyFunctionCreate(ExecutionContext cx, RuntimeInfo.Function function,
            LexicalEnvironment<?> scope) {
        assert !function.isStrict();
        /* step 1 */
        ScriptObject prototype = cx.getIntrinsic(Intrinsics.FunctionPrototype);
        /* steps 2-3 */
        assert !function.isGenerator() && !function.isAsync();
        /* step 4 */
        LegacyConstructorFunction f = FunctionAllocate(cx, LegacyConstructorFunction::new, prototype, false,
                FunctionKind.Normal);
        /* step 5 */
        FunctionInitialize(f, FunctionKind.Normal, function, scope, cx.getCurrentExecutable());
        return f;
    }
}
