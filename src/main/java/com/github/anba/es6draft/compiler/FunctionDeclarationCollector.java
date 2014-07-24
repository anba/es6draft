/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.github.anba.es6draft.ast.*;
import com.github.anba.es6draft.ast.scope.FunctionScope;
import com.github.anba.es6draft.ast.scope.Scope;
import com.github.anba.es6draft.ast.synthetic.StatementListMethod;

/**
 * 
 */
final class FunctionDeclarationCollector extends DefaultVoidNodeVisitor<Void> {
    private final ArrayList<FunctionDeclaration> declarations = new ArrayList<>();
    private final FunctionScope topScope;

    private FunctionDeclarationCollector(FunctionNode f) {
        topScope = f.getScope();
    }

    static List<FunctionDeclaration> functionDeclarations(FunctionNode f) {
        List<StatementListItem> statements = f.getStatements();
        if (statements == null) {
            assert (f instanceof ArrowFunction && ((ArrowFunction) f).getExpression() != null)
                    || (f instanceof AsyncArrowFunction && ((AsyncArrowFunction) f).getExpression() != null)
                    || f instanceof GeneratorComprehension;
            return Collections.emptyList();
        }
        FunctionDeclarationCollector collector = new FunctionDeclarationCollector(f);
        collector.forEach(statements, null);
        return collector.declarations;
    }

    private void forEach(Iterable<? extends Node> list, Void ignore) {
        for (Node node : list) {
            node.accept(this, ignore);
        }
    }

    @Override
    protected void visit(Node node, Void ignore) {
        throw new IllegalStateException(String.format("node-class: %s", node.getClass()));
    }

    @Override
    protected void visit(StatementListItem node, Void ignore) {
    }

    @Override
    public void visit(FunctionDeclaration node, Void ignore) {
        String name = node.getIdentifier().getName();
        Scope enclosingScope = node.getScope().getEnclosingScope();
        FunctionScope topScope = this.topScope;
        if (enclosingScope == topScope) {
            // Top-level function declarations are not applicable for legacy semantics
            return;
        }
        assert enclosingScope.isDeclared(name) : "undeclared block scoped function: " + name;
        // Walk scope chain
        for (Scope scope = enclosingScope; (scope = scope.getParent()) != topScope;) {
            // See 13.1.1 Static Semantics: Early Errors
            // See 13.11.1 Static Semantics: Early Errors
            if (scope.isDeclared(name)) {
                // Found a block scoped, lexical declaration - cannot declare function as var.
                return;
            }
        }
        // See 14.1.2 Static Semantics: Early Errors
        if (topScope.lexicallyDeclaredNames().contains(name)) {
            return;
        }
        // Function declaration is applicable for legacy semantics
        node.setLegacyBlockScoped(true);
        declarations.add(node);
    }

    @Override
    public void visit(BlockStatement node, Void ignore) {
        forEach(node.getStatements(), null);
    }

    @Override
    public void visit(IfStatement node, Void ignore) {
        node.getThen().accept(this, ignore);
        if (node.getOtherwise() != null) {
            node.getOtherwise().accept(this, ignore);
        }
    }

    @Override
    public void visit(DoWhileStatement node, Void ignore) {
        node.getStatement().accept(this, ignore);
    }

    @Override
    public void visit(ForEachStatement node, Void ignore) {
        node.getStatement().accept(this, ignore);
    }

    @Override
    public void visit(ForOfStatement node, Void ignore) {
        node.getStatement().accept(this, ignore);
    }

    @Override
    public void visit(ForInStatement node, Void ignore) {
        node.getStatement().accept(this, ignore);
    }

    @Override
    public void visit(ForStatement node, Void ignore) {
        node.getStatement().accept(this, ignore);
    }

    @Override
    public void visit(WhileStatement node, Void ignore) {
        node.getStatement().accept(this, ignore);
    }

    @Override
    public void visit(LabelledFunctionStatement node, Void ignore) {
        node.getFunction().accept(this, ignore);
    }

    @Override
    public void visit(LabelledStatement node, Void ignore) {
        node.getStatement().accept(this, ignore);
    }

    @Override
    public void visit(WithStatement node, Void ignore) {
        node.getStatement().accept(this, ignore);
    }

    @Override
    public void visit(SwitchStatement node, Void ignore) {
        forEach(node.getClauses(), null);
    }

    @Override
    public void visit(SwitchClause node, Void ignore) {
        forEach(node.getStatements(), null);
    }

    @Override
    public void visit(StatementListMethod node, Void ignore) {
        forEach(node.getStatements(), null);
    }

    @Override
    public void visit(LetStatement node, Void ignore) {
        node.getStatement().accept(this, ignore);
    }

    @Override
    public void visit(TryStatement node, Void ignore) {
        node.getTryBlock().accept(this, ignore);
        if (node.getCatchNode() != null) {
            node.getCatchNode().accept(this, ignore);
        }
        if (node.getFinallyBlock() != null) {
            node.getFinallyBlock().accept(this, ignore);
        }
    }

    @Override
    public void visit(GuardedCatchNode node, Void ignore) {
        node.getCatchBlock().accept(this, ignore);
    }

    @Override
    public void visit(CatchNode node, Void ignore) {
        node.getCatchBlock().accept(this, ignore);
    }
}
