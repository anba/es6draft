/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler.analyzer;

import com.github.anba.es6draft.ast.BinaryExpression;
import com.github.anba.es6draft.ast.synthetic.ExpressionMethod;

/**
 * Replaces the left and/or right branch of a {@link BinaryExpression} with an
 * {@link ExpressionMethod} element
 */
final class BinaryExpressionSubMethod extends SubMethod<BinaryExpression> {
    @Override
    int processNode(BinaryExpression node, int oldSize) {
        CodeSizeVisitor visitor = new CodeSizeVisitor();
        CodeSizeHandler handler = new EmptyHandler();

        int accSize = oldSize;
        int leftSize = node.getLeft().accept(visitor, handler);
        int rightSize = node.getRight().accept(visitor, handler);

        if (leftSize >= rightSize) {
            node.setLeft(new ExpressionMethod(node.getLeft()));
            accSize = accSize - leftSize + EXPR_METHOD_SIZE;
            if (accSize > MAX_EXPR_SIZE) {
                node.setRight(new ExpressionMethod(node.getRight()));
                accSize = accSize - rightSize + EXPR_METHOD_SIZE;
            }
        } else {
            node.setRight(new ExpressionMethod(node.getRight()));
            accSize = accSize - rightSize + EXPR_METHOD_SIZE;
            if (accSize > MAX_EXPR_SIZE) {
                node.setLeft(new ExpressionMethod(node.getLeft()));
                accSize = accSize - leftSize + EXPR_METHOD_SIZE;
            }
        }

        return validateSize(node);
    }
}
