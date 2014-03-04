/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime;

import java.util.Set;

import com.github.anba.es6draft.runtime.types.ScriptObject;

/**
 * <h1>8 Executable Code and Execution Contexts</h1><br>
 * <h2>8.1 Lexical Environments</h2>
 * <ul>
 * <li>8.1.1 Environment Records
 * </ul>
 */
public interface EnvironmentRecord {
    /**
     * Returns an iterable for all binding names of this environment record
     */
    Set<String> bindingNames();

    /**
     * HasBinding(N)
     */
    boolean hasBinding(String name);

    /**
     * CreateMutableBinding(N,D)
     */
    void createMutableBinding(String name, boolean deletable);

    /**
     * CreateImmutableBinding(N)
     */
    void createImmutableBinding(String name);

    /**
     * InitialiseBinding(N,V)
     */
    void initialiseBinding(String name, Object value);

    /**
     * SetMutableBinding(N,V,S)
     */
    void setMutableBinding(String name, Object value, boolean strict);

    /**
     * GetBindingValue(N,S)
     */
    Object getBindingValue(String name, boolean strict);

    /**
     * DeleteBinding(N)
     */
    boolean deleteBinding(String name);

    /**
     * HasThisBinding()
     */
    boolean hasThisBinding();

    /**
     * HasSuperBinding()
     */
    boolean hasSuperBinding();

    /**
     * WithBaseObject ()
     */
    ScriptObject withBaseObject();

    /**
     * GetThisBinding ()
     */
    // assert: FunctionEnvironmentRecord or GlobalEnvironmentRecord
    Object getThisBinding();
}
