/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import com.github.anba.es6draft.ast.scope.BlockScope;

/**
 * <h1>13 ECMAScript Language: Statements and Declarations</h1><br>
 * <h2>13.6 Iteration Statements</h2>
 * <ul>
 * <li>13.6.4 The for-in and for-of Statements
 * </ul>
 */
public interface ForIterationNode extends ScopedNode {
    @Override
    public BlockScope getScope();

    /**
     * Returns the <tt>for in/of/each</tt>-statement's head node. The head node is one of the
     * following types:
     * <ul>
     * <li>{@link VariableStatement}:&emsp;{@code for (var decl in/of/each expr)}
     * <li>{@link LexicalDeclaration}:&emsp;{@code for (let/const decl in/of/each expr)}
     * <li>{@link LeftHandSideExpression}:&emsp;{@code for (lhs in/of/each expr)}
     * </ul>
     * 
     * @return the head node
     */
    public Node getHead();

    /**
     * Returns the <tt>for in/of/each</tt>-statement's expression node.
     * 
     * @return the expression node
     */
    public Expression getExpression();

    /**
     * Returns the <tt>for in/of/each</tt>-statement's statement node.
     * 
     * @return the statement node
     */
    public Statement getStatement();
}
