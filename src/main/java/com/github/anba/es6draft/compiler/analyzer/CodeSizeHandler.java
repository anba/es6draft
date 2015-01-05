/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler.analyzer;

import java.util.List;

import com.github.anba.es6draft.ast.Node;
import com.github.anba.es6draft.ast.TopLevelNode;

/**
 *
 */
interface CodeSizeHandler {
    int reportSize(Node node, int size);

    void submit(TopLevelNode<?> node, List<? extends Node> children);
}
