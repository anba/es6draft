/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import static com.github.anba.es6draft.semantics.StaticSemantics.ContainsExpression;
import static com.github.anba.es6draft.semantics.StaticSemantics.IsSimpleParameterList;

import java.util.Iterator;
import java.util.List;

/**
 * <h1>14 ECMAScript Language: Functions and Classes</h1>
 * <ul>
 * <li>14.1 Function Definitions
 * </ul>
 */
public final class FormalParameterList extends AstNode implements Iterable<FormalParameter> {
    private final List<FormalParameter> formals;
    private final boolean simpleParameterList;
    private final boolean containsExpression;

    public FormalParameterList(long beginPosition, long endPosition, List<FormalParameter> formals) {
        super(beginPosition, endPosition);
        this.formals = formals;
        this.simpleParameterList = IsSimpleParameterList(formals);
        this.containsExpression = ContainsExpression(formals);
    }

    /**
     * Returns the list of formal parameters.
     * 
     * @return the list of formal parameters
     */
    public List<FormalParameter> getFormals() {
        return formals;
    }

    /**
     * Returns {@code true} for simple parameter lists.
     * 
     * @return {@code true} if this parameter list contains only simple parameters
     */
    public boolean isSimpleParameterList() {
        return simpleParameterList;
    }

    /**
     * Returns {@code true} for parameter lists with expressions.
     * 
     * @return {@code true} if this parameter list contains expression nodes
     */
    public boolean containsExpression() {
        return containsExpression;
    }

    @Override
    public Iterator<FormalParameter> iterator() {
        return formals.iterator();
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
