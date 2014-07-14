/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import java.util.EnumSet;
import java.util.Set;

/**
 * <h1>13 ECMAScript Language: Statements and Declarations</h1><br>
 * <h2>13.6 Iteration Statements</h2>
 * <ul>
 * <li>13.6.1 The do-while Statement
 * </ul>
 */
public final class DoWhileStatement extends IterationStatement {
    private final Expression test;
    private final Statement statement;

    public DoWhileStatement(long beginPosition, long endPosition, EnumSet<Abrupt> abrupt,
            Set<String> labelSet, Expression test, Statement statement) {
        super(beginPosition, endPosition, abrupt, labelSet);
        this.test = test;
        this.statement = statement;
    }

    public Expression getTest() {
        return test;
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
