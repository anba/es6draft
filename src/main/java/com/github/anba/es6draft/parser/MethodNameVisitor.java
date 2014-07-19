/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.parser;

import com.github.anba.es6draft.ast.DefaultNodeVisitor;
import com.github.anba.es6draft.ast.ElementAccessor;
import com.github.anba.es6draft.ast.IdentifierReference;
import com.github.anba.es6draft.ast.LeftHandSideExpression;
import com.github.anba.es6draft.ast.Node;
import com.github.anba.es6draft.ast.NumericLiteral;
import com.github.anba.es6draft.ast.PropertyAccessor;
import com.github.anba.es6draft.ast.StringLiteral;
import com.github.anba.es6draft.runtime.internal.Strings;

/**
 * 
 */
final class MethodNameVisitor extends DefaultNodeVisitor<String, Void> {
    private static final MethodNameVisitor INSTANCE = new MethodNameVisitor();

    /**
     * Returns the inferred method name for the node
     * 
     * @param node
     *            the AST node
     * @return the inferred method name
     */
    static String toMethodName(LeftHandSideExpression node) {
        return node.accept(INSTANCE, null);
    }

    @Override
    protected String visit(Node node, Void value) {
        return null;
    }

    @Override
    public String visit(ElementAccessor node, Void value) {
        String baseName = node.getBase().accept(this, value);
        String elementName = node.getElement().accept(this, value);
        if (baseName == null || elementName == null) {
            return null;
        }
        if (node.getElement() instanceof StringLiteral) {
            if (isIdentifier(elementName)) {
                return baseName + "." + elementName;
            }
            return baseName + "[" + Strings.quote(elementName) + "]";
        }
        return baseName + "[" + elementName + "]";
    }

    @Override
    public String visit(PropertyAccessor node, Void value) {
        String baseName = node.getBase().accept(this, value);
        if (baseName == null) {
            return null;
        }
        return baseName + "." + node.getName();
    }

    @Override
    public String visit(IdentifierReference node, Void value) {
        return node.getName();
    }

    @Override
    public String visit(StringLiteral node, Void value) {
        return node.getName();
    }

    @Override
    public String visit(NumericLiteral node, Void value) {
        return node.getName();
    }

    private static boolean isIdentifier(String name) {
        if (name.isEmpty() || !Character.isJavaIdentifierStart(name.charAt(0))) {
            return false;
        }
        for (int i = 1; i < name.length(); ++i) {
            if (!Character.isJavaIdentifierPart(name.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
