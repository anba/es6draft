/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.semantics;

import java.util.List;

import com.github.anba.es6draft.ast.Declaration;
import com.github.anba.es6draft.ast.FunctionDeclaration;
import com.github.anba.es6draft.ast.GeneratorDeclaration;
import com.github.anba.es6draft.ast.NodeVisitor;
import com.github.anba.es6draft.ast.Statement;

/**
 * Static Semantics: TopLevelLexicallyDeclaredNames
 */
class TopLevelLexicallyDeclaredNames extends StaticSemanticsVisitor<List<String>, List<String>> {
    static final NodeVisitor<List<String>, List<String>> INSTANCE = new TopLevelLexicallyDeclaredNames();

    @Override
    public List<String> visit(Statement node, List<String> value) {
        return value;
    }

    @Override
    public List<String> visit(FunctionDeclaration node, List<String> value) {
        return value;
    }

    @Override
    public List<String> visit(GeneratorDeclaration node, List<String> value) {
        // TODO: for now same rules as FunctionDeclaration
        return value;
    }

    @Override
    public List<String> visit(Declaration node, List<String> value) {
        node.accept(BoundNames.INSTANCE, value);
        return value;
    }
}
