/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import java.util.BitSet;

import com.github.anba.es6draft.runtime.DeclarativeEnvironmentRecord;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;

/**
 * <h1>9 Ordinary and Exotic Objects Behaviours</h1><br>
 * <h2>9.4 Built-in Exotic Object Internal Methods and Data Fields</h2>
 * <ul>
 * <li>9.4.4 Exotic Arguments Objects
 * </ul>
 */
final class ParameterMap {
    private static final long MAX_LENGTH = 0x7FFF_FFFF;
    private final LexicalEnvironment<? extends DeclarativeEnvironmentRecord> env;
    private final String[] parameters;
    private final int length;
    private BitSet unmapped; // lazily instantiated

    private ParameterMap(LexicalEnvironment<? extends DeclarativeEnvironmentRecord> env, String[] parameterNames,
            int length) {
        this.env = env;
        this.parameters = parameterNames;
        this.length = length;
    }

    private boolean isUnmapped(int index) {
        return unmapped != null && unmapped.get(index);
    }

    private void setUnmapped(int index) {
        if (unmapped == null) {
            unmapped = new BitSet();
        }
        unmapped.set(index);
    }

    /**
     * Returns a non-negative integer if {@code p} is a valid argument index, otherwise <code>-1</code>.
     * 
     * @param p
     *            the property key
     * @return the integer index or {@code -1}
     */
    static int toArgumentIndex(long p) {
        return 0 <= p && p < MAX_LENGTH ? (int) p : -1;
    }

    /**
     * 9.4.4.7 CreateMappedArgumentsObject ( func, formals, argumentsList, env )
     * <p>
     * Returns a new {@link ParameterMap} if there are any mapped arguments, otherwise <code>null</code>.
     * 
     * @param func
     *            the function object
     * @param len
     *            the actual number of function arguments
     * @param env
     *            the current lexical environment
     * @return a new parameter map if mapped parameters are present, otherwise {@code null}
     */
    static ParameterMap create(FunctionObject func, int len,
            LexicalEnvironment<? extends DeclarativeEnvironmentRecord> env) {
        assert func.getCode().is(RuntimeInfo.FunctionFlags.MappedArguments);
        // Directly return null if no arguments were passed.
        if (len == 0) {
            return null;
        }
        /* steps 17-20 */
        String[] parameterNames = func.getCode().parameters();
        for (int index = Math.min(len, parameterNames.length) - 1; index >= 0; --index) {
            if (parameterNames[index] != null) {
                // Found a mapped argument.
                return new ParameterMap(env, parameterNames, index + 1);
            }
        }
        // No mapped arguments found.
        return null;
    }

    /**
     * Tests whether {@code propertyKey} is an array index for a mapped argument.
     * 
     * @param propertyKey
     *            the property key
     * @return {@code true} if the property key is mapped
     */
    boolean hasOwnProperty(long propertyKey) {
        int index = toArgumentIndex(propertyKey);
        if (0 <= index && index < length && !isUnmapped(index)) {
            return parameters[index] != null;
        }
        return false;
    }

    /**
     * 9.4.4.7.1 MakeArgGetter (name, env)
     * 
     * @param propertyKey
     *            the property key
     * @return the mapped argument
     */
    Object get(long propertyKey) {
        int index = toArgumentIndex(propertyKey);
        assert (0 <= index && index < length && parameters[index] != null);
        assert !isUnmapped(index);
        String name = parameters[index];
        return env.getEnvRec().getBindingValue(name, false);
    }

    /**
     * 9.4.4.7.2 MakeArgSetter (name, env)
     * 
     * @param propertyKey
     *            the property key
     * @param value
     *            the new value for the mapped argument
     */
    void put(long propertyKey, Object value) {
        int index = toArgumentIndex(propertyKey);
        assert (0 <= index && index < length && parameters[index] != null);
        assert !isUnmapped(index);
        String name = parameters[index];
        env.getEnvRec().setMutableBinding(name, value, false);
    }

    /**
     * Removes the mapping for {@code arguments[propertyKey]}.
     * 
     * @param propertyKey
     *            the property key
     */
    void delete(long propertyKey) {
        int index = toArgumentIndex(propertyKey);
        assert (0 <= index && index < length && parameters[index] != null);
        assert !isUnmapped(index);
        setUnmapped(index);
    }
}
