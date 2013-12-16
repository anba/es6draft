/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler.analyzer;

import java.util.List;

import com.github.anba.es6draft.ast.CommaExpression;
import com.github.anba.es6draft.ast.Expression;
import com.github.anba.es6draft.ast.synthetic.ExpressionMethod;

/**
 * Inserts {@link ExpressionMethod}s into {@link CommaExpression} nodes
 */
final class CommaExpressionSubMethod extends ListSubMethod<CommaExpression> {
    private static class ExpressionElement extends NodeElement<Expression> {
        ExpressionElement(Expression node, int index, int size) {
            super(node, index, size);
        }

        @Override
        protected final Expression getReplacement(Expression node) {
            return new ExpressionMethod(node);
        }

        @Override
        protected final int getReplacementSize() {
            return EXPR_METHOD_SIZE;
        }
    }

    private static class ExpressionElementMapper implements
            NodeElementMapper<Expression, ExpressionElement> {
        @Override
        public ExpressionElement map(Expression node, int index, int size) {
            return new ExpressionElement(node, index, size);
        }
    }

    private static class ExpressionConflater extends Conflater<ExpressionElement, Expression> {
        @Override
        protected int getSourceSize(ExpressionElement source) {
            return source.size;
        }

        @Override
        protected int getTargetSize() {
            return EXPR_METHOD_SIZE;
        }

        @Override
        protected Expression newTarget(List<Expression> list) {
            return new ExpressionMethod(new CommaExpression(list));
        }
    }

    @Override
    int processNode(CommaExpression node, int oldSize) {
        List<Expression> newOperands = newNodes(oldSize, node.getOperands(),
                new ExpressionElementMapper(), new ExpressionConflater(), MAX_EXPR_SIZE,
                MAX_EXPR_SIZE, MAX_EXPR_SIZE);
        node.setOperands(newOperands);
        return validateSize(node);
    }
}
