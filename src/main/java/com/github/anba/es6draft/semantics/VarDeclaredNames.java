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
 * Static Semantics: VarDeclaredNames
 */
class VarDeclaredNames extends StaticSemanticsVisitor<List<String>, List<String>> {
    static final NodeVisitor<List<String>, List<String>> INSTANCE = new VarDeclaredNames();

    @Override
    protected List<String> visit(Declaration node, List<String> value) {
        return value;
    }

    @Override
    public List<String> visit(EmptyStatement node, List<String> value) {
        return value;
    }

    @Override
    public List<String> visit(ExpressionStatement node, List<String> value) {
        return value;
    }

    @Override
    public List<String> visit(ContinueStatement node, List<String> value) {
        return value;
    }

    @Override
    public List<String> visit(BreakStatement node, List<String> value) {
        return value;
    }

    @Override
    public List<String> visit(ReturnStatement node, List<String> value) {
        return value;
    }

    @Override
    public List<String> visit(ThrowStatement node, List<String> value) {
        return value;
    }

    @Override
    public List<String> visit(DebuggerStatement node, List<String> value) {
        return value;
    }

    @Override
    public List<String> visit(BlockStatement node, List<String> value) {
        forEach(this, node.getStatements(), value);
        return value;
    }

    @Override
    public List<String> visit(VariableStatement node, List<String> value) {
        node.accept(BoundNames.INSTANCE, value);
        return value;
    }

    @Override
    public List<String> visit(IfStatement node, List<String> value) {
        node.getThen().accept(this, value);
        if (node.getOtherwise() != null) {
            node.getOtherwise().accept(this, value);
        }
        return value;
    }

    @Override
    public List<String> visit(DoWhileStatement node, List<String> value) {
        return node.getStatement().accept(this, value);
    }

    @Override
    public List<String> visit(WhileStatement node, List<String> value) {
        return node.getStatement().accept(this, value);
    }

    @Override
    public List<String> visit(ForStatement node, List<String> value) {
        if (node.getHead() instanceof VariableStatement) {
            node.getHead().accept(this, value);
        }
        return node.getStatement().accept(this, value);
    }

    @Override
    public List<String> visit(ForInStatement node, List<String> value) {
        if (node.getHead() instanceof VariableStatement) {
            node.getHead().accept(this, value);
        }
        return node.getStatement().accept(this, value);
    }

    @Override
    public List<String> visit(ForOfStatement node, List<String> value) {
        if (node.getHead() instanceof VariableStatement) {
            node.getHead().accept(this, value);
        }
        return node.getStatement().accept(this, value);
    }

    @Override
    public List<String> visit(WithStatement node, List<String> value) {
        return node.getStatement().accept(this, value);
    }

    @Override
    public List<String> visit(SwitchStatement node, List<String> value) {
        forEach(this, node.getClauses(), value);
        return value;
    }

    @Override
    public List<String> visit(SwitchClause node, List<String> value) {
        forEach(this, node.getStatements(), value);
        return value;
    }

    @Override
    public List<String> visit(LabelledStatement node, List<String> value) {
        return node.getStatement().accept(this, value);
    }

    @Override
    public List<String> visit(TryStatement node, List<String> value) {
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
