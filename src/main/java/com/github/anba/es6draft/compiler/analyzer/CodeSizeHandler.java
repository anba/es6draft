/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler.analyzer;

import java.util.List;

import com.github.anba.es6draft.ast.Node;

/**
 *
 */
interface CodeSizeHandler {
    int reportSize(Node node, int size);

    void submit(Node node, List<? extends Node> children);
}
