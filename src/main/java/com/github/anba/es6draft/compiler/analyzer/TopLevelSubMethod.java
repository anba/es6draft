/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler.analyzer;

import java.util.ArrayList;
import java.util.List;

import com.github.anba.es6draft.ast.ArrowFunction;
import com.github.anba.es6draft.ast.FunctionNode;
import com.github.anba.es6draft.ast.Script;
import com.github.anba.es6draft.ast.StatementListItem;
import com.github.anba.es6draft.ast.TopLevelNode;
import com.github.anba.es6draft.ast.synthetic.StatementListMethod;

/**
 * Subdivides statements into {@link StatementListMethod} elements.
 */
abstract class TopLevelSubMethod<NODE extends TopLevelNode<?>> extends SubMethod<NODE> {
    static final class FunctionSubMethod extends TopLevelSubMethod<FunctionNode> {
        @Override
        int processNode(FunctionNode node, int oldSize) {
            assert !(node instanceof ArrowFunction && ((ArrowFunction) node).getExpression() != null);

            List<StatementListItem> newStatements = super.visitTopLevel(node.getStatements());
            node.setStatements(newStatements);
            return validateSize(node, node.getStatements());
        }
    }

    static final class ScriptSubMethod extends TopLevelSubMethod<Script> {
        @Override
        int processNode(Script node, int oldSize) {
            List<StatementListItem> newStatements = super.visitTopLevel(node.getStatements());
            node.setStatements(newStatements);
            return validateSize(node, node.getStatements());
        }
    }

    private static final class StatementConflater extends
            Conflater<StatementListItem, StatementListItem> {
        CodeSizeVisitor visitor = new CodeSizeVisitor();
        CodeSizeHandler handler = new EmptyHandler();

        @Override
        protected int getSourceSize(StatementListItem source) {
            return source.accept(visitor, handler);
        }

        @Override
        protected int getTargetSize() {
            return STMT_METHOD_SIZE;
        }

        @Override
        protected StatementListItem newTarget(List<StatementListItem> list) {
            return new StatementListMethod(list);
        }
    }

    private List<StatementListItem> visitTopLevel(List<StatementListItem> statements) {
        // Don't need to consider break/continue statements at top-level, simply
        // subdivide statement list into smaller parts.
        StatementConflater conflater = new StatementConflater();
        boolean needsRerun;
        do {
            List<StatementListItem> newStatements = new ArrayList<>(statements);
            needsRerun = conflater.conflate(newStatements, newStatements, MAX_STATEMENT_SIZE);
            statements = newStatements;
        } while (needsRerun);
        return statements;
    }
}
