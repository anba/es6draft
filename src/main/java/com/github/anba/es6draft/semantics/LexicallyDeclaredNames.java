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
 * Static Semantics: LexicallyDeclaredNames
 */
class LexicallyDeclaredNames extends StaticSemanticsVisitor<List<String>, List<String>> {
    static final NodeVisitor<List<String>, List<String>> INSTANCE = new LexicallyDeclaredNames();

    @Override
    public List<String> visit(Statement node, List<String> value) {
        return value;
    }

    @Override
    public List<String> visit(Declaration node, List<String> value) {
        node.accept(BoundNames.INSTANCE, value);
        return value;
    }

    @Override
    public List<String> visit(SwitchClause node, List<String> value) {
        forEach(this, node.getStatements(), value);
        return value;
    }
}
