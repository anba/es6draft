/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

/**
 * <h1>12 Statements and Declarations</h1>
 * <ul>
 * <li>12.14 The try Statement
 * </ul>
 */
public class TryStatement extends Statement {
    private BlockStatement tryBlock;
    private CatchNode catchNode;
    private BlockStatement finallyBlock;

    public TryStatement(BlockStatement tryBlock, CatchNode catchNode, BlockStatement finallyBlock) {
        this.tryBlock = tryBlock;
        this.catchNode = catchNode;
        this.finallyBlock = finallyBlock;
    }

    public BlockStatement getTryBlock() {
        return tryBlock;
    }

    public CatchNode getCatchNode() {
        return catchNode;
    }

    public BlockStatement getFinallyBlock() {
        return finallyBlock;
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }
}
