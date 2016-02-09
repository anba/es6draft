/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Property;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;

/**
 * <h1>9 Ordinary and Exotic Objects Behaviours</h1>
 * <ul>
 * <li>9.3 Built-in Function Objects
 * </ul>
 */
public abstract class BuiltinFunction extends OrdinaryObject implements Callable, Cloneable {
    /**
     * Function name for anonymous functions.
     */
    protected static final String ANONYMOUS = "";

    /** [[Realm]] */
    private final Realm realm;

    private final String name;
    private final int arity;

    // Store default "name" and "length" inline to avoid allocating properties table space.
    private boolean hasDefaultName, hasDefaultLength;

    private MethodHandle callMethod;
    private Object methodInfo;

    /**
     * Creates a new built-in function.
     * 
     * @param realm
     *            the realm instance
     * @param name
     *            the function name
     * @param arity
     *            the function arity
     */
    protected BuiltinFunction(Realm realm, String name, int arity) {
        super(realm);
        this.realm = realm;
        this.name = name;
        this.arity = arity;
    }

    /**
     * Returns the i-th argument or {@code undefined} if the argument index is out of bounds.
     * 
     * @param arguments
     *            the function arguments
     * @param index
     *            the argument index
     * @return the requested argument or undefined if not present
     */
    protected static final Object argument(Object[] arguments, int index) {
        return arguments.length > index ? arguments[index] : UNDEFINED;
    }

    /**
     * Returns the i-th argument or {@code defaultValue} if the argument index is out of bounds.
     * 
     * @param arguments
     *            the function arguments
     * @param index
     *            the argument index
     * @param defaultValue
     *            the default value for absent arguments
     * @return the requested argument or <var>defaultValue</var> if not present
     */
    protected static final Object argument(Object[] arguments, int index, Object defaultValue) {
        return arguments.length > index ? arguments[index] : defaultValue;
    }

    /**
     * Returns the callee execution context.
     * 
     * @return the callee execution context
     */
    protected final ExecutionContext calleeContext() {
        return realm.defaultContext();
    }

    /**
     * Creates the default function properties, i.e. 'name' and 'length', and initializes the [[Prototype]] slot to the
     * <code>%FunctionPrototype%</code> object.
     */
    protected final void createDefaultFunctionProperties() {
        // Function.prototype is the [[Prototype]] for built-in functions, cf. 17
        setPrototype(realm.getIntrinsic(Intrinsics.FunctionPrototype));
        // "length" property of function objects, cf. 19.2.4.1
        hasDefaultLength = true;
        // anonymous functions do not have an own "name" property, cf. 19.2.4.2
        if (!name.isEmpty()) {
            hasDefaultName = true;
        }
    }

    @Override
    protected abstract BuiltinFunction clone();

    @Override
    public final BuiltinFunction clone(ExecutionContext cx) {
        BuiltinFunction f = clone();
        f.setPrototype(getPrototype());
        return f;
    }

    @Override
    public final String toSource(ExecutionContext cx) {
        return FunctionSource.nativeCode(name);
    }

    /**
     * Returns the function's name.
     * 
     * @return the function name
     */
    public final String getName() {
        return name;
    }

    /**
     * Returns the function's arity.
     * 
     * @return the function arity
     */
    public final int getArity() {
        return arity;
    }

    /**
     * [[Realm]]
     * 
     * @return the bound realm
     */
    public final Realm getRealm() {
        return realm;
    }

    @Override
    public final Realm getRealm(ExecutionContext cx) {
        /* 7.3.22 GetFunctionRealm ( obj ) */
        return realm;
    }

    protected MethodHandles.Lookup lookup() {
        return MethodHandles.publicLookup();
    }

    /**
     * Returns `(? extends BuiltinFunction, ExecutionContext, Object, Object[]) {@literal ->} Object` method-handle.
     * 
     * @return the call method handle
     */
    public MethodHandle getCallMethod() {
        if (callMethod == null) {
            try {
                Method method = getClass().getDeclaredMethod("call", ExecutionContext.class, Object.class,
                        Object[].class);
                callMethod = lookup().unreflect(method);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }
        return callMethod;
    }

    /**
     * Returns the method info object.
     * 
     * @return the method info object
     */
    public final Object getMethodInfo() {
        if (methodInfo == null) {
            methodInfo = new Object();
        }
        return methodInfo;
    }

    /**
     * 9.3.1 [[Call]] (thisArgument, argumentsList)
     */
    @Override
    public Object tailCall(ExecutionContext callerContext, Object thisValue, Object... args) throws Throwable {
        return call(callerContext, thisValue, args);
    }

    @Override
    public String toString() {
        return String.format("%s, name=%s, arity=%d", super.toString(), name, arity);
    }

    @Override
    public long getLength() {
        if (hasDefaultLength) {
            return arity;
        }
        return super.getLength();
    }

    @Override
    protected boolean setPropertyValue(ExecutionContext cx, String propertyKey, Object value, Property current) {
        assert !(hasDefaultName && "name".equals(propertyKey));
        assert !(hasDefaultLength && "length".equals(propertyKey));
        return super.setPropertyValue(cx, propertyKey, value, current);
    }

    @Override
    protected boolean has(ExecutionContext cx, String propertyKey) {
        if (hasDefaultName && "name".equals(propertyKey)) {
            return true;
        }
        if (hasDefaultLength && "length".equals(propertyKey)) {
            return true;
        }
        return super.has(cx, propertyKey);
    }

    @Override
    protected boolean hasOwnProperty(ExecutionContext cx, String propertyKey) {
        if (hasDefaultName && "name".equals(propertyKey)) {
            return true;
        }
        if (hasDefaultLength && "length".equals(propertyKey)) {
            return true;
        }
        return super.hasOwnProperty(cx, propertyKey);
    }

    @Override
    protected Property getProperty(ExecutionContext cx, String propertyKey) {
        if (hasDefaultName && "name".equals(propertyKey)) {
            return new Property(name, false, false, true);
        }
        if (hasDefaultLength && "length".equals(propertyKey)) {
            return new Property(arity, false, false, true);
        }
        return super.getProperty(cx, propertyKey);
    }

    @Override
    protected boolean defineProperty(ExecutionContext cx, String propertyKey, PropertyDescriptor desc) {
        if (hasDefaultName && "name".equals(propertyKey)) {
            hasDefaultName = false;
            defineOwnPropertyUnchecked(propertyKey, new Property(name, false, false, true));
        }
        if (hasDefaultLength && "length".equals(propertyKey)) {
            hasDefaultLength = false;
            defineOwnPropertyUnchecked(propertyKey, new Property(arity, false, false, true));
        }
        return super.defineProperty(cx, propertyKey, desc);
    }

    @Override
    protected boolean deleteProperty(ExecutionContext cx, String propertyKey) {
        if (hasDefaultName && "name".equals(propertyKey)) {
            hasDefaultName = false;
            return true;
        }
        if (hasDefaultLength && "length".equals(propertyKey)) {
            hasDefaultLength = false;
            return true;
        }
        return super.deleteProperty(cx, propertyKey);
    }

    @Override
    protected List<String> getEnumerableKeys(ExecutionContext cx) {
        if (hasDefaultLength || hasDefaultName) {
            int totalSize = countProperties(false);
            if (hasDefaultLength) {
                totalSize += 1;
            }
            if (hasDefaultName) {
                totalSize += 1;
            }
            ArrayList<String> keys = new ArrayList<>(totalSize);
            appendIndexedProperties(keys);
            if (hasDefaultLength) {
                keys.add("length");
            }
            if (hasDefaultName) {
                keys.add("name");
            }
            appendProperties(keys);
            return keys;
        }
        return super.getEnumerableKeys(cx);
    }

    @Override
    protected Enumerability isEnumerableOwnProperty(String propertyKey) {
        if (hasDefaultName && "name".equals(propertyKey)) {
            return Enumerability.NonEnumerable;
        }
        if (hasDefaultLength && "length".equals(propertyKey)) {
            return Enumerability.NonEnumerable;
        }
        return super.isEnumerableOwnProperty(propertyKey);
    }

    @Override
    protected List<Object> getOwnPropertyKeys(ExecutionContext cx) {
        if (hasDefaultLength || hasDefaultName) {
            int totalSize = countProperties(true);
            if (hasDefaultLength) {
                totalSize += 1;
            }
            if (hasDefaultName) {
                totalSize += 1;
            }
            /* step 1 */
            ArrayList<Object> ownKeys = new ArrayList<>(totalSize);
            /* step 2 */
            appendIndexedProperties(ownKeys);
            /* step 3 */
            if (hasDefaultLength) {
                ownKeys.add("length");
            }
            if (hasDefaultName) {
                ownKeys.add("name");
            }
            appendProperties(ownKeys);
            /* step 4 */
            appendSymbolProperties(ownKeys);
            /* step 5 */
            return ownKeys;
        }
        return super.getOwnPropertyKeys(cx);
    }
}
