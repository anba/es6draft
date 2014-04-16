/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import java.util.ArrayList;
import java.util.List;

import com.github.anba.es6draft.ast.*;
import com.github.anba.es6draft.ast.synthetic.StatementListMethod;

/**
 * 
 */
final class FunctionDeclarationCollector extends
        DefaultNodeVisitor<List<FunctionDeclaration>, List<FunctionDeclaration>> {
    private FunctionDeclarationCollector() {
    }

    static List<FunctionDeclaration> functionDeclarations(FunctionDefinition f) {
        FunctionDeclarationCollector collector = new FunctionDeclarationCollector();
        List<FunctionDeclaration> declarations = new ArrayList<>();
        return collector.forEach(f.getStatements(), declarations);
    }

    private List<FunctionDeclaration> forEach(Iterable<? extends Node> list,
            List<FunctionDeclaration> value) {
        for (Node node : list) {
            node.accept(this, value);
        }
        return value;
    }

    @Override
    protected List<FunctionDeclaration> visit(Node node, List<FunctionDeclaration> value) {
        throw new IllegalStateException(String.format("node-class: %s", node.getClass()));
    }

    @Override
    protected List<FunctionDeclaration> visit(StatementListItem node,
            List<FunctionDeclaration> value) {
        return value;
    }

    @Override
    public List<FunctionDeclaration> visit(FunctionDeclaration node, List<FunctionDeclaration> value) {
        value.add(node);
        return value;
    }

    @Override
    public List<FunctionDeclaration> visit(BlockStatement node, List<FunctionDeclaration> value) {
        return forEach(node.getStatements(), value);
    }

    @Override
    public List<FunctionDeclaration> visit(IfStatement node, List<FunctionDeclaration> value) {
        node.getThen().accept(this, value);
        if (node.getOtherwise() != null) {
            node.getOtherwise().accept(this, value);
        }
        return value;
    }

    @Override
    public List<FunctionDeclaration> visit(DoWhileStatement node, List<FunctionDeclaration> value) {
        return node.getStatement().accept(this, value);
    }

    @Override
    public List<FunctionDeclaration> visit(ForEachStatement node, List<FunctionDeclaration> value) {
        return node.getStatement().accept(this, value);
    }

    @Override
    public List<FunctionDeclaration> visit(ForOfStatement node, List<FunctionDeclaration> value) {
        return node.getStatement().accept(this, value);
    }

    @Override
    public List<FunctionDeclaration> visit(ForInStatement node, List<FunctionDeclaration> value) {
        return node.getStatement().accept(this, value);
    }

    @Override
    public List<FunctionDeclaration> visit(ForStatement node, List<FunctionDeclaration> value) {
        return node.getStatement().accept(this, value);
    }

    @Override
    public List<FunctionDeclaration> visit(WhileStatement node, List<FunctionDeclaration> value) {
        return node.getStatement().accept(this, value);
    }

    @Override
    public List<FunctionDeclaration> visit(LabelledStatement node, List<FunctionDeclaration> value) {
        return node.getStatement().accept(this, value);
    }

    @Override
    public List<FunctionDeclaration> visit(WithStatement node, List<FunctionDeclaration> value) {
        return node.getStatement().accept(this, value);
    }

    @Override
    public List<FunctionDeclaration> visit(SwitchStatement node, List<FunctionDeclaration> value) {
        return forEach(node.getClauses(), value);
    }

    @Override
    public List<FunctionDeclaration> visit(SwitchClause node, List<FunctionDeclaration> value) {
        return forEach(node.getStatements(), value);
    }

    @Override
    public List<FunctionDeclaration> visit(StatementListMethod node, List<FunctionDeclaration> value) {
        return forEach(node.getStatements(), value);
    }

    @Override
    public List<FunctionDeclaration> visit(LetStatement node, List<FunctionDeclaration> value) {
        return node.getStatement().accept(this, value);
    }

    @Override
    public List<FunctionDeclaration> visit(TryStatement node, List<FunctionDeclaration> value) {
        node.getTryBlock().accept(this, value);
        if (node.getCatchNode() != null) {
            node.getCatchNode().accept(this, value);
        }
        if (node.getFinallyBlock() != null) {
            node.getFinallyBlock().accept(this, value);
        }
        return value;
    }

    @Override
    public List<FunctionDeclaration> visit(GuardedCatchNode node, List<FunctionDeclaration> value) {
        return node.getCatchBlock().accept(this, value);
    }

    @Override
    public List<FunctionDeclaration> visit(CatchNode node, List<FunctionDeclaration> value) {
        return node.getCatchBlock().accept(this, value);
    }
}
