/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.semantics;

import com.github.anba.es6draft.ast.*;

/**
 * <h1>Static Semantics</h1>
 * <ul>
 * <li>IsValidSimpleAssignmentTarget
 * </ul>
 */
class IsValidSimpleAssignmentTarget extends StaticSemanticsVisitor<Boolean, Boolean> {
    static final NodeVisitor<Boolean, Boolean> INSTANCE = new IsValidSimpleAssignmentTarget();

    @Override
    public Boolean visit(ThisExpression node, Boolean strict) {
        return false;
    }

    @Override
    protected Boolean visit(Literal node, Boolean strict) {
        return false;
    }

    @Override
    public Boolean visit(ArrayInitialiser node, Boolean strict) {
        return false;
    }

    @Override
    public Boolean visit(ObjectLiteral node, Boolean strict) {
        return false;
    }

    @Override
    public Boolean visit(FunctionExpression node, Boolean strict) {
        return false;
    }

    @Override
    public Boolean visit(ClassExpression node, Boolean strict) {
        return false;
    }

    @Override
    public Boolean visit(GeneratorExpression node, Boolean strict) {
        return false;
    }

    @Override
    public Boolean visit(GeneratorComprehension node, Boolean strict) {
        return false;
    }

    @Override
    public Boolean visit(RegularExpressionLiteral node, Boolean strict) {
        return false;
    }

    @Override
    public Boolean visit(TemplateLiteral node, Boolean strict) {
        return false;
    }

    @Override
    public Boolean visit(Identifier node, Boolean strict) {
        if (strict) {
            String name = node.getName();
            if ("eval".equals(name) || "arguments".equals(name)) {
                return false;
            }
        }
        return true;
    }

    // @Override
    // public Boolean visit(ParenthesisedExpression node, Boolean strict) {
    // return node.getExpression().accept(this, strict);
    // }

    @Override
    public Boolean visit(ElementAccessor node, Boolean strict) {
        return true;
    }

    @Override
    public Boolean visit(PropertyAccessor node, Boolean strict) {
        return true;
    }

    @Override
    public Boolean visit(SuperExpression node, Boolean strict) {
        return true;
    }

    @Override
    public Boolean visit(CallExpression node, Boolean strict) {
        return true;
    }

    @Override
    public Boolean visit(TemplateCallExpression node, Boolean strict) {
        return false;
    }

    @Override
    public Boolean visit(NewExpression node, Boolean strict) {
        return false;
    }

    @Override
    public Boolean visit(UnaryExpression node, Boolean strict) {
        return false;
    }

    @Override
    public Boolean visit(BinaryExpression node, Boolean strict) {
        return false;
    }

    @Override
    public Boolean visit(ConditionalExpression node, Boolean strict) {
        return false;
    }

    @Override
    public Boolean visit(YieldExpression node, Boolean strict) {
        return false;
    }

    @Override
    public Boolean visit(ArrowFunction node, Boolean strict) {
        return false;
    }

    @Override
    public Boolean visit(AssignmentExpression node, Boolean strict) {
        return false;
    }

    @Override
    public Boolean visit(CommaExpression node, Boolean strict) {
        return false;
    }
}
