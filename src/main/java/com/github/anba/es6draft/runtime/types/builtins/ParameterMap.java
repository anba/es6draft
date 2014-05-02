/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import java.util.BitSet;

import com.github.anba.es6draft.runtime.DeclarativeEnvironmentRecord;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.internal.Strings;

/**
 * <h1>9 Ordinary and Exotic Objects Behaviours</h1><br>
 * <h2>9.4 Built-in Exotic Object Internal Methods and Data Fields</h2>
 * <ul>
 * <li>9.4.4 Exotic Arguments Objects
 * </ul>
 */
final class ParameterMap {
    private final LexicalEnvironment<? extends DeclarativeEnvironmentRecord> env;
    private final int length;
    private final String[] parameters;
    private final BitSet legacyUnmapped;

    private ParameterMap(LexicalEnvironment<? extends DeclarativeEnvironmentRecord> env, int length) {
        this.env = env;
        this.length = length;
        this.parameters = new String[length];
        this.legacyUnmapped = new BitSet();
    }

    /**
     * Returns a non-negative integer if {@code p} is a valid argument index, otherwise
     * <code>-1</code>.
     * 
     * @param p
     *            the property key
     * @return the integer index or {@code -1}
     */
    static int toArgumentIndex(String p) {
        return Strings.toIndex(p);
    }

    /**
     * 9.4.4.7 CreateMappedArgumentsObject ( func, formals, argumentsList, env ) Abstract Operation
     * <p>
     * Returns a new {@link ParameterMap} if there are any mapped arguments, otherwise
     * <code>null</code>.
     * 
     * @param len
     *            the actual number of function arguments
     * @param parameterNames
     *            the formal parameter names
     * @param env
     *            the current lexical environment
     * @return a new parameter map if mapped parameters are present, otherwise {@code null}
     */
    static ParameterMap create(int len, String[] parameterNames,
            LexicalEnvironment<? extends DeclarativeEnvironmentRecord> env) {
        boolean hasMapped = false;
        /* step 13 */
        int numberOfParameters = parameterNames.length;
        /* step 17 */
        ParameterMap map = new ParameterMap(env, len);
        /* steps 18-20 */
        for (int index = numberOfParameters - 1; index >= 0; --index) {
            String name = parameterNames[index];
            if (name != null && index < len) {
                hasMapped = true;
                map.defineOwnProperty(index, name);
            }
        }
        return hasMapped ? map : null;
    }

    /**
     * Makes {@code arguments[propertyKey]} a mapped argument.
     * 
     * @param propertyKey
     *            the property key
     * @param name
     *            the formal parameter name
     */
    private void defineOwnProperty(int propertyKey, String name) {
        parameters[propertyKey] = name;
    }

    /**
     * Tests whether {@code propertyKey} is an array index for a mapped argument.
     * 
     * @param propertyKey
     *            the property key
     * @param isLegacy
     *            flag for legacy arguments objects
     * @return {@code true} if the property key is mapped
     */
    boolean hasOwnProperty(String propertyKey, boolean isLegacy) {
        int index = toArgumentIndex(propertyKey);
        if (index >= 0 && index < length && !(isLegacy && legacyUnmapped.get(index))) {
            return parameters[index] != null;
        }
        return false;
    }

    /**
     * 9.4.4.7.1 MakeArgGetter (name, env) Abstract Operation
     * 
     * @param propertyKey
     *            the property key
     * @return the mapped argument
     */
    Object get(String propertyKey) {
        int index = toArgumentIndex(propertyKey);
        assert (index >= 0 && index < length && parameters[index] != null);
        String name = parameters[index];
        return env.getEnvRec().getBindingValue(name, false);
    }

    /**
     * 9.4.4.7.2 MakeArgSetter (name, env) Abstract Operation
     * 
     * @param propertyKey
     *            the property key
     * @param value
     *            the new value for the mapped argument
     */
    void put(String propertyKey, Object value) {
        int index = toArgumentIndex(propertyKey);
        assert (index >= 0 && index < length && parameters[index] != null);
        legacyUnmapped.set(index);
        String name = parameters[index];
        env.getEnvRec().setMutableBinding(name, value, false);
    }

    /**
     * Removes mapping for {@code arguments[propertyKey]}.
     * 
     * @param propertyKey
     *            the property key
     */
    void delete(String propertyKey) {
        int index = toArgumentIndex(propertyKey);
        assert (index >= 0 && index < length && parameters[index] != null);
        legacyUnmapped.set(index);
        parameters[index] = null;
    }
}
