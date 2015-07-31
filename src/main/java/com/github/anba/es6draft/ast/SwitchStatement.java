/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.github.anba.es6draft.ast.scope.BlockScope;

/**
 * <h1>13 ECMAScript Language: Statements and Declarations</h1>
 * <ul>
 * <li>13.12 The switch Statement
 * </ul>
 */
public final class SwitchStatement extends BreakableStatement implements ScopedNode {
    private final BlockScope scope;
    private final Expression expression;
    private final List<SwitchClause> clauses;

    public SwitchStatement(long beginPosition, long endPosition, BlockScope scope,
            EnumSet<Abrupt> abrupt, Set<String> labelSet, Expression expression,
            List<SwitchClause> clauses) {
        super(beginPosition, endPosition, abrupt, labelSet);
        this.scope = scope;
        this.expression = expression;
        this.clauses = clauses;
    }

    @Override
    public BlockScope getScope() {
        return scope;
    }

    /**
     * Returns the {@code switch} expression node.
     * 
     * @return the expression
     */
    public Expression getExpression() {
        return expression;
    }

    /**
     * Returns the list of <tt>CaseClause</tt> nodes.
     * 
     * @return the list of <tt>CaseClause</tt> nodes
     */
    public List<SwitchClause> getClauses() {
        return clauses;
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
