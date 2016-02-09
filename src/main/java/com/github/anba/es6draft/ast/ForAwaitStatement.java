/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import java.util.EnumSet;
import java.util.Set;

import com.github.anba.es6draft.ast.scope.BlockScope;

/**
 * <h1>Extension: Async Generator Functions</h1>
 * <ul>
 * <li>The for-await Statement
 * </ul>
 */
public final class ForAwaitStatement extends IterationStatement implements ForIterationNode {
    private final BlockScope scope;
    private final Node head;
    private final Expression expression;
    private Statement statement;

    public ForAwaitStatement(long beginPosition, long endPosition, BlockScope scope, EnumSet<Abrupt> abrupt,
            Set<String> labelSet, Node head, Expression expression, Statement stmt) {
        super(beginPosition, endPosition, abrupt, labelSet);
        this.scope = scope;
        this.head = head;
        this.expression = expression;
        this.statement = stmt;
    }

    @Override
    public BlockScope getScope() {
        return scope;
    }

    @Override
    public Node getHead() {
        return head;
    }

    @Override
    public Expression getExpression() {
        return expression;
    }

    @Override
    public Statement getStatement() {
        return statement;
    }

    @Override
    public void setStatement(Statement statement) {
        this.statement = statement;
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
