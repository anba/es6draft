/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import java.util.List;

/**
 * <h1>14 ECMAScript Language: Functions and Classes</h1>
 */
public interface FunctionNode extends TopLevelNode<StatementListItem>, ScopedNode {
    public enum ThisMode {
        Lexical, Strict, Global
    }

    /**
     * Returns the function name.
     * 
     * @return the function name
     */
    String getFunctionName();

    /**
     * Returns the formal parameter list for this function.
     * 
     * @return the parameters
     */
    FormalParameterList getParameters();

    enum StrictMode {
        NonStrict, ImplicitStrict, ExplicitStrict
    }

    /**
     * Retrieves the strict-mode for this object.
     * 
     * @return the strict-mode
     */
    StrictMode getStrictMode();

    /**
     * Sets the strict-mode for this object.
     * 
     * @param strictMode
     *            the new strict-mode
     */
    void setStrictMode(StrictMode strictMode);

    /**
     * Returns the source string representation for the function's header.
     * 
     * @return the header source string
     */
    String getHeaderSource();

    /**
     * Returns the source string representation for the function's body.
     * 
     * @return the body source string
     */
    String getBodySource();

    @Override
    public List<StatementListItem> getStatements();

    @Override
    void setStatements(List<StatementListItem> statements);

    @Override
    FunctionScope getScope();

    /**
     * Returns the [[ThisMode]] of this function.
     * 
     * @return the this-mode field
     */
    ThisMode getThisMode();

    /**
     * Returns <code>true</code> if this function is a generator function.
     * 
     * @return <code>true</code> if this node is a generator
     */
    boolean isGenerator();

    /**
     * Returns <code>true</code> if this function is an async function.
     * 
     * @return <code>true</code> if this node is an async function
     */
    boolean isAsync();

    /**
     * Returns <code>true</code> if the <code>"super"</code> keyword is used within this function.
     * 
     * @return <code>true</code> if <code>"super"</code> reference is present
     */
    boolean hasSuperReference();
}
