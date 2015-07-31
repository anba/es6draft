/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler.analyzer;

import java.util.List;

import com.github.anba.es6draft.ast.Expression;
import com.github.anba.es6draft.ast.TemplateLiteral;
import com.github.anba.es6draft.ast.synthetic.ExpressionMethod;

/**
 * Inserts {@link ExpressionMethod}s into {@link TemplateLiteral} nodes
 */
final class TemplateLiteralSubMethod extends ListSubMethod<TemplateLiteral> {
    private static final class TemplateElement extends NodeElement<Expression> {
        TemplateElement(Expression node, int size, int index) {
            super(node, size, index);
        }

        @Override
        protected Expression createReplacement() {
            return new ExpressionMethod(getNode());
        }

        @Override
        protected int getReplacementSize() {
            return EXPR_METHOD_SIZE;
        }
    }

    private static final class TemplateElementMapper implements
            NodeElementMapper<Expression, TemplateElement> {
        @Override
        public TemplateElement map(Expression node, int size, int index) {
            return new TemplateElement(node, size, index);
        }
    }

    private static final class TemplateConflater extends Conflater<TemplateElement, Expression> {
        @Override
        protected int getSourceSize(TemplateElement source) {
            return source.getSize();
        }

        @Override
        protected int getTargetSize() {
            return EXPR_METHOD_SIZE;
        }

        @Override
        protected Expression newTarget(List<Expression> list) {
            assert !list.isEmpty();
            long beginPosition = list.get(0).getBeginPosition();
            long endPosition = list.get(list.size() - 1).getEndPosition();
            return new ExpressionMethod(
                    new TemplateLiteral(beginPosition, endPosition, false, list));
        }
    }

    @Override
    int processNode(TemplateLiteral node, int oldSize) {
        assert !node.isTagged();
        List<Expression> newElements = newNodes(oldSize, node.getElements(),
                new TemplateElementMapper(), new TemplateConflater(), MAX_EXPR_SIZE, MAX_EXPR_SIZE,
                MAX_EXPR_SIZE);
        node.setElements(newElements);
        return validateSize(node);
    }
}
