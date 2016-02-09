/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler.analyzer;

import java.util.List;

import com.github.anba.es6draft.ast.ArrowFunction;
import com.github.anba.es6draft.ast.AsyncArrowFunction;
import com.github.anba.es6draft.ast.FunctionNode;
import com.github.anba.es6draft.ast.Module;
import com.github.anba.es6draft.ast.ModuleItem;
import com.github.anba.es6draft.ast.Script;
import com.github.anba.es6draft.ast.StatementListItem;
import com.github.anba.es6draft.ast.TopLevelNode;
import com.github.anba.es6draft.ast.synthetic.StatementListMethod;

/**
 * Subdivides statements into {@link StatementListMethod} elements.
 */
abstract class TopLevelSubMethod<STATEMENT extends ModuleItem, NODE extends TopLevelNode<STATEMENT>>
        extends SubMethod<NODE> {
    static final class FunctionSubMethod extends TopLevelSubMethod<StatementListItem, FunctionNode> {
        @Override
        int processNode(FunctionNode node, int oldSize) {
            assert !(node instanceof ArrowFunction && ((ArrowFunction) node).getExpression() != null);
            assert !(node instanceof AsyncArrowFunction && ((AsyncArrowFunction) node).getExpression() != null);

            List<StatementListItem> newStatements = subdivide(node.getStatements(), new StatementListItemConflater(),
                    MAX_STATEMENT_SIZE);
            node.setStatements(newStatements);
            return validateSize(node, node.getStatements());
        }
    }

    static final class ModuleSubMethod extends TopLevelSubMethod<ModuleItem, Module> {
        @Override
        int processNode(Module node, int oldSize) {
            List<ModuleItem> newStatements = subdivide(node.getStatements(), new ModuleItemConflater(),
                    MAX_STATEMENT_SIZE);
            node.setStatements(newStatements);
            return validateSize(node, node.getStatements());
        }
    }

    static final class ScriptSubMethod extends TopLevelSubMethod<StatementListItem, Script> {
        @Override
        int processNode(Script node, int oldSize) {
            List<StatementListItem> newStatements = subdivide(node.getStatements(), new StatementListItemConflater(),
                    MAX_STATEMENT_SIZE);
            node.setStatements(newStatements);
            return validateSize(node, node.getStatements());
        }
    }

    private static abstract class StatementConflater<STATEMENT extends ModuleItem>
            extends Conflater<STATEMENT, STATEMENT> {
        final CodeSizeVisitor visitor = new CodeSizeVisitor();
        final CodeSizeHandler handler = new EmptyHandler();

        @Override
        protected final int getSourceSize(STATEMENT source) {
            return source.accept(visitor, handler);
        }

        @Override
        protected final int getTargetSize() {
            return STMT_METHOD_SIZE;
        }
    }

    private static final class StatementListItemConflater extends StatementConflater<StatementListItem> {
        @Override
        protected StatementListItem newTarget(List<StatementListItem> list) {
            return new StatementListMethod(list);
        }
    }

    private static final class ModuleItemConflater extends StatementConflater<ModuleItem> {
        @Override
        protected ModuleItem newTarget(List<ModuleItem> list) {
            return new StatementListMethod(list);
        }
    }
}
