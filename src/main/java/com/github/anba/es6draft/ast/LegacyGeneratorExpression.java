/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import java.util.List;

import com.github.anba.es6draft.ast.scope.FunctionScope;

/**
 * <h1>14 ECMAScript Language: Functions and Classes</h1>
 * <ul>
 * <li>14.4 Generator Functions Definitions
 * </ul>
 */
public final class LegacyGeneratorExpression extends GeneratorExpression {
    public LegacyGeneratorExpression(long beginPosition, long endPosition, FunctionScope scope,
            BindingIdentifier identifier, FormalParameterList parameters,
            List<StatementListItem> statements, String headerSource, String bodySource) {
        super(beginPosition, endPosition, scope, identifier, parameters, statements, headerSource,
                bodySource);
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }

    @Override
    public <V> int accept(IntNodeVisitor<V> visitor, V value) {
        return visitor.visit(this, value);
    }

    @Override
    public <V> void accept(VoidNodeVisitor<V> visitor, V value) {
        visitor.visit(this, value);
    }
}
