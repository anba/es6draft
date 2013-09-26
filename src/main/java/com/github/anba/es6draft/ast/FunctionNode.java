/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

/**
 * <h1>14 ECMAScript Language: Functions and Classes</h1>
 */
public interface FunctionNode extends TopLevelNode, ScopedNode {
    String getFunctionName();

    FormalParameterList getParameters();

    enum StrictMode {
        NonStrict, ImplicitStrict, ExplicitStrict
    }

    StrictMode getStrictMode();

    void setStrictMode(StrictMode strictMode);

    String getHeaderSource();

    String getBodySource();

    @Override
    FunctionScope getScope();
}
