/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast.synthetic;

import com.github.anba.es6draft.ast.Node;

/**
 * TODO: Rename to "OutlinableNode" / "OutlinedNode" (and let DoExpression inherit from it?).
 */
public interface SyntheticNode extends Node {
    /**
     * Returns {@code true} if a resume point is created before calling the synthetic method.
     * 
     * @return if {@code true} a resume point is created before calling the synthetic method
     */
    boolean hasResumePoint();

    /**
     * Enables or disables the creation of a resume point prior to calling the synthetic method.
     * 
     * @param resumePoint
     *            {@code true} to create a resume point before calling the synthetic method
     */
    void setResumePoint(boolean resumePoint);
}
