/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import java.util.List;

/**
 * 
 */
final class Functions {
    private Functions() {
    }

    static boolean isInlinable(FunctionNode node) {
        return InlineTest.isInlinable(node);
    }

    private static final class InlineTest extends DefaultNodeVisitor<Boolean, Void> {
        private static final InlineTest INSTANCE = new InlineTest();

        static boolean isInlinable(FunctionNode node) {
            return node.accept(INSTANCE, null);
        }

        @Override
        protected Boolean visit(Node node, Void value) {
            return false;
        }

        private boolean visitFunction(FunctionNode node, Void value) {
            if (!node.getParameters().getFormals().isEmpty()) {
                return false;
            }
            List<StatementListItem> statements = node.getStatements();
            if (statements.isEmpty()) {
                return true;
            }
            if (statements.size() == 1) {
                StatementListItem statement = statements.get(0);
                if (statement instanceof ReturnStatement) {
                    return statement.accept(this, value);
                }
            }
            return false;
        }

        @Override
        public Boolean visit(ArrowFunction node, Void value) {
            if (node.getExpression() != null) {
                return node.getParameters().getFormals().isEmpty() && InlineExprTest.isInlinable(node.getExpression());
            }
            return visitFunction(node, value);
        }

        @Override
        public Boolean visit(FunctionDeclaration node, Void value) {
            return visitFunction(node, value);
        }

        @Override
        public Boolean visit(FunctionExpression node, Void value) {
            return visitFunction(node, value);
        }

        @Override
        public Boolean visit(MethodDefinition node, Void value) {
            switch (node.getType()) {
            case Function:
            case Getter:
            case Setter:
                return visitFunction(node, value);
            default:
                return false;
            }
        }

        @Override
        public Boolean visit(ReturnStatement node, Void value) {
            if (node.getExpression() == null) {
                return true;
            }
            return InlineExprTest.isInlinable(node.getExpression());
        }
    }

    private static final class InlineExprTest extends DefaultNodeVisitor<Boolean, Void> {
        private static final InlineExprTest INSTANCE = new InlineExprTest();

        static boolean isInlinable(Expression node) {
            return node.accept(INSTANCE, null);
        }

        @Override
        protected Boolean visit(Node node, Void value) {
            return false;
        }

        @Override
        protected Boolean visit(Literal node, Void value) {
            return true;
        }

        @Override
        public Boolean visit(UnaryExpression node, Void value) {
            switch (node.getOperator()) {
            case BITNOT:
            case NEG:
            case NOT:
            case POS:
            case VOID:
                return node.getOperand().accept(this, value);
            default:
                return false;
            }
        }
    }
}
