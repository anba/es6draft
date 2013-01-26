/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.semantics;

import java.util.List;

import com.github.anba.es6draft.ast.*;

/**
 * Static Semantics: VarScopedDeclarations
 */
class VarScopedDeclarations extends
        StaticSemanticsVisitor<List<StatementListItem>, List<StatementListItem>> {
    static final NodeVisitor<List<StatementListItem>, List<StatementListItem>> INSTANCE = new VarScopedDeclarations();

    @Override
    protected List<StatementListItem> visit(Declaration node, List<StatementListItem> value) {
        return value;
    }

    @Override
    public List<StatementListItem> visit(EmptyStatement node, List<StatementListItem> value) {
        return value;
    }

    @Override
    public List<StatementListItem> visit(ExpressionStatement node, List<StatementListItem> value) {
        return value;
    }

    @Override
    public List<StatementListItem> visit(ContinueStatement node, List<StatementListItem> value) {
        return value;
    }

    @Override
    public List<StatementListItem> visit(BreakStatement node, List<StatementListItem> value) {
        return value;
    }

    @Override
    public List<StatementListItem> visit(ReturnStatement node, List<StatementListItem> value) {
        return value;
    }

    @Override
    public List<StatementListItem> visit(ThrowStatement node, List<StatementListItem> value) {
        return value;
    }

    @Override
    public List<StatementListItem> visit(DebuggerStatement node, List<StatementListItem> value) {
        return value;
    }

    @Override
    public List<StatementListItem> visit(BlockStatement node, List<StatementListItem> value) {
        forEach(this, node.getStatements(), value);
        return value;
    }

    @Override
    public List<StatementListItem> visit(VariableStatement node, List<StatementListItem> value) {
        // TODO: Missing in spec!?
        value.add(node);
        return value;
    }

    @Override
    public List<StatementListItem> visit(IfStatement node, List<StatementListItem> value) {
        if (node.getOtherwise() != null) {
            node.getThen().accept(this, value);
            return node.getOtherwise().accept(this, value);
        }
        return node.getThen().accept(this, value);
    }

    @Override
    public List<StatementListItem> visit(DoWhileStatement node, List<StatementListItem> value) {
        return node.getStatement().accept(this, value);
    }

    @Override
    public List<StatementListItem> visit(WhileStatement node, List<StatementListItem> value) {
        return node.getStatement().accept(this, value);
    }

    @Override
    public List<StatementListItem> visit(ForStatement node, List<StatementListItem> value) {
        if (node.getHead() instanceof VariableStatement) {
            node.getHead().accept(this, value);
        }
        return node.getStatement().accept(this, value);
    }

    @Override
    public List<StatementListItem> visit(ForInStatement node, List<StatementListItem> value) {
        if (node.getHead() instanceof VariableStatement) {
            node.getHead().accept(this, value);
        }
        return node.getStatement().accept(this, value);
    }

    @Override
    public List<StatementListItem> visit(ForOfStatement node, List<StatementListItem> value) {
        if (node.getHead() instanceof VariableStatement) {
            node.getHead().accept(this, value);
        }
        return node.getStatement().accept(this, value);
    }

    @Override
    public List<StatementListItem> visit(WithStatement node, List<StatementListItem> value) {
        return node.getStatement().accept(this, value);
    }

    @Override
    public List<StatementListItem> visit(SwitchStatement node, List<StatementListItem> value) {
        forEach(this, node.getClauses(), value);
        return value;
    }

    @Override
    public List<StatementListItem> visit(SwitchClause node, List<StatementListItem> value) {
        forEach(this, node.getStatements(), value);
        return value;
    }

    @Override
    public List<StatementListItem> visit(LabelledStatement node, List<StatementListItem> value) {
        return node.getStatement().accept(this, value);
    }

    @Override
    public List<StatementListItem> visit(TryStatement node, List<StatementListItem> value) {
        node.getTryBlock().accept(this, value);
        if (node.getCatchBlock() != null) {
            node.getCatchBlock().accept(this, value);
        }
        if (node.getFinallyBlock() != null) {
            node.getFinallyBlock().accept(this, value);
        }
        return value;
    }
}