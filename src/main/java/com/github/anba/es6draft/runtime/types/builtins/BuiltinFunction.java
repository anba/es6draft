/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.util.ArrayList;
import java.util.List;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.IndexedMap;
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

    // Store default "name" and "length" inline to avoid allocating properties table space
    private boolean hasDefaultName, hasDefaultLength;

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
     * @return the callee context
     */
    protected final ExecutionContext calleeContext() {
        return realm.defaultContext();
    }

    /**
     * Creates the default function properties, i.e. 'name' and 'length', and initializes the
     * [[Prototype]] slot to the <code>%FunctionPrototype%</code> object.
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
    public final String toSource(SourceSelector selector) {
        return FunctionSource.nativeCode(selector, name);
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
        /* 7.3.21 GetFunctionRealm ( obj ) Abstract Operation */
        return realm;
    }

    /**
     * 9.3.1 [[Call]] (thisArgument, argumentsList)
     */
    @Override
    public Object tailCall(ExecutionContext callerContext, Object thisValue, Object... args)
            throws Throwable {
        return call(callerContext, thisValue, args);
    }

    @Override
    public String toString() {
        return String.format("%s, name=%s, arity=%d", super.toString(), name, arity);
    }

    private void addProperty(String propertKey, Object value) {
        assert !IndexedMap.isIndex(IndexedMap.toIndex(propertKey));
        assert !properties().containsKey(propertKey);
        properties().put(propertKey, new Property(value, false, false, true));
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
    protected boolean defineProperty(ExecutionContext cx, String propertyKey,
            PropertyDescriptor desc) {
        if (hasDefaultName && "name".equals(propertyKey)) {
            hasDefaultName = false;
            addProperty(propertyKey, name);
        }
        if (hasDefaultLength && "length".equals(propertyKey)) {
            hasDefaultLength = false;
            addProperty(propertyKey, arity);
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
            int indexedSize = indexedProperties().size();
            int propertiesSize = properties().size();
            int totalSize = indexedSize + propertiesSize;
            if (hasDefaultLength) {
                totalSize += 1;
            }
            if (hasDefaultName) {
                totalSize += 1;
            }
            ArrayList<String> keys = new ArrayList<>(totalSize);
            if (indexedSize != 0) {
                keys.addAll(indexedProperties().keys());
            }
            if (hasDefaultLength) {
                keys.add("length");
            }
            if (hasDefaultName) {
                keys.add("name");
            }
            if (propertiesSize != 0) {
                keys.addAll(properties().keySet());
            }
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
            int indexedSize = indexedProperties().size();
            int propertiesSize = properties().size();
            int symbolsSize = symbolProperties().size();
            int totalSize = indexedSize + propertiesSize + symbolsSize;
            if (hasDefaultLength) {
                totalSize += 1;
            }
            if (hasDefaultName) {
                totalSize += 1;
            }
            /* step 1 */
            ArrayList<Object> ownKeys = new ArrayList<>(totalSize);
            /* step 2 */
            if (indexedSize != 0) {
                ownKeys.addAll(indexedProperties().keys());
            }
            /* step 3 */
            if (hasDefaultLength) {
                ownKeys.add("length");
            }
            if (hasDefaultName) {
                ownKeys.add("name");
            }
            if (propertiesSize != 0) {
                ownKeys.addAll(properties().keySet());
            }
            /* step 4 */
            if (symbolsSize != 0) {
                ownKeys.addAll(symbolProperties().keySet());
            }
            /* step 5 */
            return ownKeys;
        }
        return super.getOwnPropertyKeys(cx);
    }

    // TODO: spec bug? [[GetOwnProperty]] override necessary, cf.
    // AddRestrictedFunctionProperties (Bug 1223)

    // /**
    // * 9.2.3 [[GetOwnProperty]] (P)
    // */
    // @Override
    // protected Property getProperty(ExecutionContext cx, String propertyKey) {
    // /* steps 1-2 */
    // Property v = super.getProperty(cx, propertyKey);
    // /* step 3 */
    // if (v != null && v.isDataDescriptor()) {
    // if ("caller".equals(propertyKey) && isStrictFunction(v.getValue())
    // && getRealm().isEnabled(CompatibilityOption.FunctionPrototype)) {
    // PropertyDescriptor desc = v.toPropertyDescriptor();
    // desc.setValue(NULL);
    // v = desc.toProperty();
    // }
    // }
    // /* step 4 */
    // return v;
    // }
}
