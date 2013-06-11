/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import java.util.List;

/**
 * <h1>13 Functions and Generators</h1>
 */
public interface FunctionNode extends ScopedNode {
    String getFunctionName();

    FormalParameterList getParameters();

    List<StatementListItem> getStatements();

    void setStatements(List<StatementListItem> statements);

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
