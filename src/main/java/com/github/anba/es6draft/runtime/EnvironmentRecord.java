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
     * Returns a set for all binding names of this environment record.
     * 
     * @return the binding names set
     */
    Set<String> bindingNames();

    /**
     * HasBinding(N)
     * 
     * @param name
     *            the binding name
     * @return {@code true} if the binding is present
     */
    boolean hasBinding(String name);

    /**
     * CreateMutableBinding(N,D)
     * 
     * @param name
     *            the binding name
     * @param deletable
     *            the deletable flag
     */
    void createMutableBinding(String name, boolean deletable);

    /**
     * CreateImmutableBinding(N)
     * 
     * @param name
     *            the binding name
     */
    void createImmutableBinding(String name);

    /**
     * InitializeBinding(N,V)
     * 
     * @param name
     *            the binding name
     * @param value
     *            the new binding value
     */
    void initializeBinding(String name, Object value);

    /**
     * SetMutableBinding(N,V,S)
     * 
     * @param name
     *            the binding name
     * @param value
     *            the new binding value
     * @param strict
     *            the strict mode flag
     */
    void setMutableBinding(String name, Object value, boolean strict);

    /**
     * GetBindingValue(N,S)
     * 
     * @param name
     *            the binding name
     * @param strict
     *            the strict mode flag
     * @return the binding value
     */
    Object getBindingValue(String name, boolean strict);

    /**
     * DeleteBinding(N)
     * 
     * @param name
     *            the binding name
     * @return {@code true} on success
     */
    boolean deleteBinding(String name);

    /**
     * HasThisBinding()
     * 
     * @return {@code true} if the environment record has a this-binding
     */
    boolean hasThisBinding();

    /**
     * HasSuperBinding()
     * 
     * @return {@code true} if the environment record has a super-binding
     */
    boolean hasSuperBinding();

    /**
     * WithBaseObject ()
     * 
     * @return the base object or {@code null} if not present
     */
    ScriptObject withBaseObject();

    /**
     * GetThisBinding ()
     * 
     * @return the this-binding
     * @throws IllegalStateException
     *             if this operation is not valid
     */
    // assert: FunctionEnvironmentRecord or GlobalEnvironmentRecord
    Object getThisBinding() throws IllegalStateException;
}
