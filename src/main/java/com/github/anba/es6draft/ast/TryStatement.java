/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import java.util.List;

/**
 * <h1>13 ECMAScript Language: Statements and Declarations</h1>
 * <ul>
 * <li>13.14 The try Statement
 * </ul>
 */
public final class TryStatement extends Statement {
    private final BlockStatement tryBlock;
    private final CatchNode catchNode;
    private final BlockStatement finallyBlock;
    private final List<GuardedCatchNode> guardedCatchNodes;

    public TryStatement(long beginPosition, long endPosition, BlockStatement tryBlock,
            CatchNode catchNode, List<GuardedCatchNode> guardedCatchNodes,
            BlockStatement finallyBlock) {
        super(beginPosition, endPosition);
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

    @Override
    public <V> int accept(IntNodeVisitor<V> visitor, V value) {
        return visitor.visit(this, value);
    }
}
