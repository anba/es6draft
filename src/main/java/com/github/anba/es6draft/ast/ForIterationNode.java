/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import com.github.anba.es6draft.ast.scope.BlockScope;

/**
 * <h1>13 ECMAScript Language: Statements and Declarations</h1><br>
 * <h2>13.7 Iteration Statements</h2>
 * <ul>
 * <li>13.7.5 The for-in and for-of Statements
 * </ul>
 */
public interface ForIterationNode extends ScopedNode {
    @Override
    BlockScope getScope();

    /**
     * Returns the <code>for in/of/each</code>-statement's head node. The head node is one of the
     * following types:
     * <ul>
     * <li>{@link VariableStatement}:&emsp;{@code for (var decl in/of expr)}
     * <li>{@link LexicalDeclaration}:&emsp;{@code for (let/const decl in/of expr)}
     * <li>{@link LeftHandSideExpression}:&emsp;{@code for (lhs in/of expr)}
     * </ul>
     * 
     * @return the head node
     */
    Node getHead();

    /**
     * Returns the <code>for in/of/each</code>-statement's expression node.
     * 
     * @return the expression node
     */
    Expression getExpression();

    /**
     * Returns the <code>for in/of/each</code>-statement's statement node.
     * 
     * @return the statement node
     */
    Statement getStatement();

    /**
     * Sets the <code>for for in/of/each</code>-statement's statement node.
     * 
     * @param statement
     *            the new statement node
     */
    void setStatement(Statement statement);
}
