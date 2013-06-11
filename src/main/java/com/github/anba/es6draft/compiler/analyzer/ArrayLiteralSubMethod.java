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

import com.github.anba.es6draft.ast.ArrayLiteral;
import com.github.anba.es6draft.ast.Expression;
import com.github.anba.es6draft.ast.synthetic.ExpressionMethod;
import com.github.anba.es6draft.ast.synthetic.SpreadArrayLiteral;
import com.github.anba.es6draft.ast.synthetic.SpreadElementMethod;

/**
 * Inserts {@link SpreadElementMethod}s into {@link ArrayLiteral} nodes
 */
class ArrayLiteralSubMethod extends SubMethod<ArrayLiteral> {
    private static final int MAX_ARRAY_ELEMENT_SIZE = MAX_EXPR_SIZE;
    private static final int MAX_ARRAY_SIZE = 8 * MAX_ARRAY_ELEMENT_SIZE;
    private static final int MAX_SPREAD_SIZE = 4 * MAX_ARRAY_ELEMENT_SIZE;

    private static class ArrayElement extends NodeElement<Expression> {
        ArrayElement(Expression node, int index, int size) {
            super(node, index, size);
        }

        int export() {
            int savedSize = -size + EXPR_METHOD_SIZE;
            this.node = new ExpressionMethod(node);
            this.size = EXPR_METHOD_SIZE;
            return savedSize;
        }

        static List<ArrayElement> from(List<Expression> elements) {
            CodeSizeVisitor visitor = new CodeSizeVisitor();
            CodeSizeHandler handler = new EmptyHandler();
            List<ArrayElement> list = new ArrayList<>(elements.size());
            for (int i = 0, len = elements.size(); i < len; i++) {
                Expression expression = elements.get(i);
                int size = expression.accept(visitor, handler);
                list.add(new ArrayElement(expression, i, size));
            }
            return list;
        }
    }

    private static class ArrayConflater extends Conflater<ArrayElement, Expression> {
        @Override
        protected int getSize(ArrayElement source) {
            return source.size;
        }

        @Override
        protected Expression newTarget(List<Expression> list) {
            return new SpreadElementMethod(new SpreadArrayLiteral(list));
        }
    }

    @Override
    int processNode(ArrayLiteral node, int oldSize) {
        List<Expression> newElements = new ArrayList<>(node.getElements());
        List<ArrayElement> list = ArrayElement.from(newElements);
        int accSize = oldSize;

        // replace single big elements with method-expressions
        PriorityQueue<ArrayElement> pq = new PriorityQueue<>(list);
        while (!pq.isEmpty() && pq.peek().size > MAX_ARRAY_ELEMENT_SIZE) {
            ArrayElement element = pq.remove();

            // export and update entry
            accSize += element.export();
            newElements.set(element.index, element.node);
        }

        if (accSize > MAX_ARRAY_SIZE) {
            // compact multiple elements with array-spreads
            new ArrayConflater().conflate(list, newElements, MAX_SPREAD_SIZE);
        }

        node.setElements(newElements);

        return validateSize(node);
    }
}
