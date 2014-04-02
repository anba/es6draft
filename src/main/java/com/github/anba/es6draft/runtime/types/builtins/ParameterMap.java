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
     */
    static int toArgumentIndex(String p) {
        return Strings.toIndex(p);
    }

    /**
     * Returns a new {@link ParameterMap} if there are any mapped arguments, otherwise
     * <code>null</code>.
     */
    static ParameterMap create(int len, String[] formals,
            LexicalEnvironment<? extends DeclarativeEnvironmentRecord> env) {
        boolean hasMapped = false;
        int numberOfNonRestFormals = formals.length;
        ParameterMap map = new ParameterMap(env, len);
        // FIXME: spec bug duplicate arguments vs mapped arguments (bug 1240)
        for (int index = numberOfNonRestFormals - 1; index >= 0; --index) {
            String name = formals[index];
            if (name != null && index < len) {
                hasMapped = true;
                map.defineOwnProperty(index, name);
            }
        }
        return hasMapped ? map : null;
    }

    /**
     * Makes {@code arguments[propertyKey]} a mapped argument
     */
    void defineOwnProperty(int propertyKey, String name) {
        parameters[propertyKey] = name;
    }

    /**
     * Tests whether {@code propertyKey} is an array index for a mapped argument
     */
    boolean hasOwnProperty(String propertyKey, boolean isLegacy) {
        int index = toArgumentIndex(propertyKey);
        if (index >= 0 && index < length && !(isLegacy && legacyUnmapped.get(index))) {
            return parameters[index] != null;
        }
        return false;
    }

    /**
     * See MakeArgGetter abstract operation
     */
    Object get(String propertyKey) {
        int index = toArgumentIndex(propertyKey);
        assert (index >= 0 && index < length && parameters[index] != null);
        String name = parameters[index];
        return env.getEnvRec().getBindingValue(name, true);
    }

    /**
     * See MakeArgSetter abstract operation
     */
    void put(String propertyKey, Object value) {
        int index = toArgumentIndex(propertyKey);
        assert (index >= 0 && index < length && parameters[index] != null);
        legacyUnmapped.set(index);
        String name = parameters[index];
        env.getEnvRec().setMutableBinding(name, value, true);
    }

    /**
     * Removes mapping for {@code arguments[propertyKey]}
     */
    boolean delete(String propertyKey) {
        int index = toArgumentIndex(propertyKey);
        assert (index >= 0 && index < length && parameters[index] != null);
        legacyUnmapped.set(index);
        parameters[index] = null;
        return true;
    }
}
