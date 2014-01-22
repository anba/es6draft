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
 * <ul>
 * <li>14.4 Generator Functions Definitions
 * </ul>
 */
public class LegacyGeneratorDeclaration extends GeneratorDeclaration {
    public LegacyGeneratorDeclaration(long beginPosition, long endPosition, FunctionScope scope,
            BindingIdentifier identifier, FormalParameterList parameters,
            List<StatementListItem> statements, boolean superReference, String headerSource,
            String bodySource) {
        super(beginPosition, endPosition, scope, identifier, parameters, statements,
                superReference, headerSource, bodySource);
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }
}
