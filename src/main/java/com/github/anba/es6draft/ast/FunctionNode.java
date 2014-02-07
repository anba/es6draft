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
    String getFunctionName();

    /**
     * Returns the formal parameter list for this function.
     */
    FormalParameterList getParameters();

    enum StrictMode {
        NonStrict, ImplicitStrict, ExplicitStrict
    }

    /**
     * Retrieves the strict-mode for this object.
     */
    StrictMode getStrictMode();

    /**
     * Sets the strict-mode for this object.
     */
    void setStrictMode(StrictMode strictMode);

    /**
     * Returns the source string representation for the function's header.
     */
    String getHeaderSource();

    /**
     * Returns the source string representation for the function's body.
     */
    String getBodySource();

    @Override
    public List<StatementListItem> getStatements();

    @Override
    void setStatements(List<StatementListItem> statements);

    @Override
    FunctionScope getScope();

    /**
     * Returns <code>true</code> if this function is a generator function.
     */
    boolean isGenerator();

    /**
     * Returns <code>true</code> if the <code>"super"</code> keyword is used within this function.
     */
    boolean hasSuperReference();
}
