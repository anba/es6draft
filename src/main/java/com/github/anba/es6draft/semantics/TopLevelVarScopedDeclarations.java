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
import com.github.anba.es6draft.ast.StatementListItem;
import com.github.anba.es6draft.ast.VariableStatement;

/**
 * Static Semantics: TopLevelVarScopedDeclarations
 */
class TopLevelVarScopedDeclarations extends
        StaticSemanticsVisitor<List<StatementListItem>, List<StatementListItem>> {
    static final NodeVisitor<List<StatementListItem>, List<StatementListItem>> INSTANCE = new TopLevelVarScopedDeclarations();

//    @Override
//    public List<StatementListItem> visit(Statement node, List<StatementListItem> value) {
//        return value;
//    }

    @Override
    public List<StatementListItem> visit(Statement node, List<StatementListItem> value) {
        // FIXME: use block scoped declarations
        node.accept(VarScopedDeclarations.INSTANCE, value);
        return value;
    }

    @Override
    public List<StatementListItem> visit(VariableStatement node, List<StatementListItem> value) {
        value.add(node);
        return value;
    }

    @Override
    public List<StatementListItem> visit(FunctionDeclaration node, List<StatementListItem> value) {
        value.add(node);
        return value;
    }

    @Override
    public List<StatementListItem> visit(GeneratorDeclaration node, List<StatementListItem> value) {
        // TODO: for now same rules as FunctionDeclaration
        value.add(node);
        return value;
    }

    @Override
    public List<StatementListItem> visit(Declaration node, List<StatementListItem> value) {
        return value;
    }
}
