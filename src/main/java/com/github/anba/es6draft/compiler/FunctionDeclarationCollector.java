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
final class FunctionDeclarationCollector extends DefaultNodeVisitor<Void, Boolean> {
    private final ArrayList<FunctionDeclaration> declarations = new ArrayList<>();

    private FunctionDeclarationCollector() {
    }

    static List<FunctionDeclaration> functionDeclarations(FunctionDefinition f) {
        FunctionDeclarationCollector collector = new FunctionDeclarationCollector();
        collector.forEach(f.getStatements(), Boolean.FALSE);
        return collector.declarations;
    }

    private Void forEach(Iterable<? extends Node> list, Boolean value) {
        for (Node node : list) {
            node.accept(this, value);
        }
        return null;
    }

    @Override
    protected Void visit(Node node, Boolean value) {
        throw new IllegalStateException(String.format("node-class: %s", node.getClass()));
    }

    @Override
    protected Void visit(StatementListItem node, Boolean value) {
        return null;
    }

    @Override
    public Void visit(FunctionDeclaration node, Boolean value) {
        if (value) {
            declarations.add(node);
        }
        return null;
    }

    @Override
    public Void visit(BlockStatement node, Boolean value) {
        return forEach(node.getStatements(), Boolean.TRUE);
    }

    @Override
    public Void visit(IfStatement node, Boolean value) {
        node.getThen().accept(this, value);
        if (node.getOtherwise() != null) {
            node.getOtherwise().accept(this, value);
        }
        return null;
    }

    @Override
    public Void visit(DoWhileStatement node, Boolean value) {
        return node.getStatement().accept(this, value);
    }

    @Override
    public Void visit(ForEachStatement node, Boolean value) {
        return node.getStatement().accept(this, value);
    }

    @Override
    public Void visit(ForOfStatement node, Boolean value) {
        return node.getStatement().accept(this, value);
    }

    @Override
    public Void visit(ForInStatement node, Boolean value) {
        return node.getStatement().accept(this, value);
    }

    @Override
    public Void visit(ForStatement node, Boolean value) {
        return node.getStatement().accept(this, value);
    }

    @Override
    public Void visit(WhileStatement node, Boolean value) {
        return node.getStatement().accept(this, value);
    }

    @Override
    public Void visit(LabelledStatement node, Boolean value) {
        return node.getStatement().accept(this, value);
    }

    @Override
    public Void visit(WithStatement node, Boolean value) {
        return node.getStatement().accept(this, value);
    }

    @Override
    public Void visit(SwitchStatement node, Boolean value) {
        return forEach(node.getClauses(), value);
    }

    @Override
    public Void visit(SwitchClause node, Boolean value) {
        return forEach(node.getStatements(), Boolean.TRUE);
    }

    @Override
    public Void visit(StatementListMethod node, Boolean value) {
        return forEach(node.getStatements(), value);
    }

    @Override
    public Void visit(LetStatement node, Boolean value) {
        return node.getStatement().accept(this, value);
    }

    @Override
    public Void visit(TryStatement node, Boolean value) {
        node.getTryBlock().accept(this, value);
        if (node.getCatchNode() != null) {
            node.getCatchNode().accept(this, value);
        }
        if (node.getFinallyBlock() != null) {
            node.getFinallyBlock().accept(this, value);
        }
        return null;
    }

    @Override
    public Void visit(GuardedCatchNode node, Boolean value) {
        return node.getCatchBlock().accept(this, value);
    }

    @Override
    public Void visit(CatchNode node, Boolean value) {
        return node.getCatchBlock().accept(this, value);
    }
}
