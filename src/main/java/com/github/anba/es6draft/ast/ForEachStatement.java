/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import java.util.EnumSet;
import java.util.Set;

import com.github.anba.es6draft.ast.scope.BlockScope;

/**
 * <h1>13 ECMAScript Language: Statements and Declarations</h1><br>
 * <h2>13.6 Iteration Statements</h2>
 * <ul>
 * <li>Extension: 'for-each' statement
 * </ul>
 */
public final class ForEachStatement extends IterationStatement implements ScopedNode {
    private final BlockScope scope;
    private final Node head;
    private final Expression expression;
    private final Statement statement;

    public ForEachStatement(long beginPosition, long endPosition, BlockScope scope,
            EnumSet<Abrupt> abrupt, Set<String> labelSet, Node head, Expression expression,
            Statement statement) {
        super(beginPosition, endPosition, abrupt, labelSet);
        this.scope = scope;
        this.head = head;
        this.expression = expression;
        this.statement = statement;
    }

    @Override
    public BlockScope getScope() {
        return scope;
    }

    public Node getHead() {
        return head;
    }

    public Expression getExpression() {
        return expression;
    }

    public Statement getStatement() {
        return statement;
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }

    @Override
    public <V> int accept(IntNodeVisitor<V> visitor, V value) {
        return visitor.visit(this, value);
    }
}
