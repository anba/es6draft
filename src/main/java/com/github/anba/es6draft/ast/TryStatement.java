/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import java.util.List;

/**
 * <h1>13 ECMAScript Language: Statements and Declarations</h1>
 * <ul>
 * <li>13.15 The try Statement
 * </ul>
 */
public final class TryStatement extends Statement {
    private final BlockStatement tryBlock;
    private final CatchNode catchNode;
    private final BlockStatement finallyBlock;
    private final List<GuardedCatchNode> guardedCatchNodes;

    public TryStatement(long beginPosition, long endPosition, BlockStatement tryBlock, CatchNode catchNode,
            List<GuardedCatchNode> guardedCatchNodes, BlockStatement finallyBlock) {
        super(beginPosition, endPosition);
        this.tryBlock = tryBlock;
        this.catchNode = catchNode;
        this.guardedCatchNodes = guardedCatchNodes;
        this.finallyBlock = finallyBlock;
    }

    /**
     * Returns the {@code try} block.
     * 
     * @return the {@code try} block
     */
    public BlockStatement getTryBlock() {
        return tryBlock;
    }

    /**
     * Returns the optional <code>Catch</code> block.
     * 
     * @return the <code>Catch</code> block or {@code null}
     */
    public CatchNode getCatchNode() {
        return catchNode;
    }

    /**
     * Returns the list of guarded <code>Catch</code> blocks.
     * 
     * @return the list of guarded <code>Catch</code> blocks
     */
    public List<GuardedCatchNode> getGuardedCatchNodes() {
        return guardedCatchNodes;
    }

    /**
     * Returns the optional <code>Finally</code> block.
     * 
     * @return the <code>Finally</code> block or {@code null}
     */
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

    @Override
    public <V> void accept(VoidNodeVisitor<V> visitor, V value) {
        visitor.visit(this, value);
    }
}
