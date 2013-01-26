/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.semantics;

import java.util.List;

import com.github.anba.es6draft.ast.Declaration;
import com.github.anba.es6draft.ast.NodeVisitor;
import com.github.anba.es6draft.ast.Statement;
import com.github.anba.es6draft.ast.SwitchClause;

/**
 * Static Semantics: LexicalDeclarations
 */
class LexicalDeclarations extends StaticSemanticsVisitor<List<Declaration>, List<Declaration>> {
    static final NodeVisitor<List<Declaration>, List<Declaration>> INSTANCE = new LexicalDeclarations();

    @Override
    public List<Declaration> visit(Statement node, List<Declaration> value) {
        return value;
    }

    @Override
    public List<Declaration> visit(Declaration node, List<Declaration> value) {
        value.add(node);
        return value;
    }

    @Override
    public List<Declaration> visit(SwitchClause node, List<Declaration> value) {
        forEach(this, node.getStatements(), value);
        return value;
    }
}
