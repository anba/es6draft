/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler.analyzer;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

import com.github.anba.es6draft.ast.CommaExpression;
import com.github.anba.es6draft.ast.Expression;
import com.github.anba.es6draft.ast.synthetic.ExpressionMethod;

/**
 * Inserts {@link ExpressionMethod}s into {@link CommaExpression} nodes
 */
class CommaExpressionSubMethod extends SubMethod<CommaExpression> {
    private static class ExpressionElement extends NodeElement<Expression> {
        ExpressionElement(Expression node, int index, int size) {
            super(node, index, size);
        }

        int export() {
            int savedSize = -size + EXPR_METHOD_SIZE;
            this.node = new ExpressionMethod(node);
            this.size = EXPR_METHOD_SIZE;
            return savedSize;
        }

        static List<ExpressionElement> from(List<Expression> elements) {
            CodeSizeVisitor visitor = new CodeSizeVisitor();
            CodeSizeHandler handler = new EmptyHandler();
            List<ExpressionElement> list = new ArrayList<>(elements.size());
            for (int i = 0, len = elements.size(); i < len; i++) {
                Expression expression = elements.get(i);
                int size = expression.accept(visitor, handler);
                list.add(new ExpressionElement(expression, i, size));
            }
            return list;
        }
    }

    private static class ExpressionConflater extends Conflater<ExpressionElement, Expression> {
        @Override
        protected int getSize(ExpressionElement source) {
            return source.size;
        }

        @Override
        protected Expression newTarget(List<Expression> list) {
            return new ExpressionMethod(new CommaExpression(list));
        }
    }

    @Override
    int processNode(CommaExpression node, int oldSize) {
        List<Expression> newOperands = new ArrayList<>(node.getOperands());
        List<ExpressionElement> list = ExpressionElement.from(newOperands);
        int accSize = oldSize;

        // replace single big elements with method-expressions
        PriorityQueue<ExpressionElement> pq = new PriorityQueue<>(list);
        while (!pq.isEmpty() && pq.peek().size > MAX_EXPR_SIZE) {
            ExpressionElement element = pq.remove();

            // export and update entry
            accSize += element.export();
            newOperands.set(element.index, element.node);
        }

        if (accSize > MAX_EXPR_SIZE) {
            // compact multiple elements with inner comma expressions
            new ExpressionConflater().conflate(list, newOperands, MAX_EXPR_SIZE);
        }

        node.setOperands(newOperands);

        return validateSize(node);
    }
}
