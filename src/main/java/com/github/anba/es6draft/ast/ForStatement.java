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
 * <h1>13 ECMAScript Language: Statements and Declarations</h1><br>
 * <h2>13.7 Iteration Statements</h2>
 * <ul>
 * <li>13.7.4 The for Statement
 * </ul>
 */
public final class ForStatement extends IterationStatement implements ScopedNode {
    private final BlockScope scope;
    private final Node head;
    private final Expression test;
    private final Expression step;
    private Statement statement;

    public ForStatement(long beginPosition, long endPosition, BlockScope scope,
            EnumSet<Abrupt> abrupt, Set<String> labelSet, Node head, Expression test,
            Expression step, Statement statement) {
        super(beginPosition, endPosition, abrupt, labelSet);
        this.scope = scope;
        this.head = head;
        this.test = test;
        this.step = step;
        this.statement = statement;
    }

    @Override
    public BlockScope getScope() {
        return scope;
    }

    /**
     * Returns the <code>for</code>-statement's head node. The head node is one of the following types:
     * <ul>
     * <li>{@link VariableStatement}:&emsp;{@code for (var decl; ...)}
     * <li>{@link LexicalDeclaration}:&emsp;{@code for (let/const decl; ...)}
     * <li>{@link Expression}:&emsp;{@code for (expr; ...)}
     * <li>or {@code null} if not present
     * </ul>
     * 
     * @return the head node or {@code null} if not present
     */
    public Node getHead() {
        return head;
    }

    /**
     * Returns the <code>for</code>-statement's test expression node.
     * 
     * @return the expression node or {@code null} if not present
     */
    public Expression getTest() {
        return test;
    }

    /**
     * Returns the <code>for</code>-statement's step expression node.
     * 
     * @return the expression node or {@code null} if not present
     */
    public Expression getStep() {
        return step;
    }

    /**
     * Returns the <code>for</code>-statement's statement node.
     * 
     * @return the statement node
     */
    @Override
    public Statement getStatement() {
        return statement;
    }

    /**
     * Sets the <code>for</code>-statement's statement node.
     * 
     * @param statement
     *            the new statement node
     */
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
