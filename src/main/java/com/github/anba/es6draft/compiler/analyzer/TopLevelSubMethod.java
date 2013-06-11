/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler.analyzer;

import java.util.ArrayList;
import java.util.List;

import com.github.anba.es6draft.ast.*;
import com.github.anba.es6draft.ast.synthetic.StatementListMethod;

/**
 * Subdivides statements into {@link StatementListMethod} elements
 */
abstract class TopLevelSubMethod<NODE extends Node> extends SubMethod<NODE> {
    static class FunctionSubMethod extends TopLevelSubMethod<FunctionNode> {
        @Override
        int processNode(FunctionNode node, int oldSize) {
            assert !(node instanceof ArrowFunction && ((ArrowFunction) node).getExpression() != null);

            List<StatementListItem> newStatements = super.visitTopLevel(node.getStatements());
            node.setStatements(newStatements);
            return validateSize(node, node.getStatements());
        }
    }

    static class ScriptSubMethod extends TopLevelSubMethod<Script> {
        @Override
        int processNode(Script node, int oldSize) {
            List<StatementListItem> newStatements = super.visitTopLevel(node.getStatements());
            node.setStatements(newStatements);
            return validateSize(node, node.getStatements());
        }
    }

    private static class StatementConflater extends Conflater<StatementListItem, StatementListItem> {
        CodeSizeVisitor visitor = new CodeSizeVisitor();
        CodeSizeHandler handler = new EmptyHandler();

        @Override
        protected int getSize(StatementListItem source) {
            return source.accept(visitor, handler);
        }

        @Override
        protected StatementListItem newTarget(List<StatementListItem> list) {
            return new StatementListMethod(list);
        }
    }

    private List<StatementListItem> visitTopLevel(List<StatementListItem> statements) {
        // don't need to consider break/continue statements at top-level, simply
        // subdivide statement list into smaller parts
        List<StatementListItem> newStatements = new ArrayList<>(statements);
        new StatementConflater().conflate(newStatements, newStatements, MAX_STATEMENT_SIZE);
        return newStatements;
    }
}
