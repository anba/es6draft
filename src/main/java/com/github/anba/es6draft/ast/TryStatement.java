/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import java.util.List;

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
    private List<GuardedCatchNode> guardedCatchNodes;

    public TryStatement(BlockStatement tryBlock, CatchNode catchNode,
            List<GuardedCatchNode> guardedCatchNodes, BlockStatement finallyBlock) {
        this.tryBlock = tryBlock;
        this.catchNode = catchNode;
        this.guardedCatchNodes = guardedCatchNodes;
        this.finallyBlock = finallyBlock;
    }

    public BlockStatement getTryBlock() {
        return tryBlock;
    }

    public CatchNode getCatchNode() {
        return catchNode;
    }

    public List<GuardedCatchNode> getGuardedCatchNodes() {
        return guardedCatchNodes;
    }

    public BlockStatement getFinallyBlock() {
        return finallyBlock;
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }
}
