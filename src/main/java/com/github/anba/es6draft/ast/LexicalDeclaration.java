/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import java.util.List;

/**
 * <h1>13 ECMAScript Language: Statements and Declarations</h1><br>
 * <h2>13.2 Declarations and the Variable Statement</h2>
 * <ul>
 * <li>13.2.1 Let and Const Declarations
 * </ul>
 */
public final class LexicalDeclaration extends Declaration {
    private final Type type;
    private final List<LexicalBinding> elements;

    public enum Type {
        Let, Const
    }

    public LexicalDeclaration(long beginPosition, long endPosition, Type type,
            List<LexicalBinding> elements) {
        super(beginPosition, endPosition);
        this.type = type;
        this.elements = elements;
    }

    public Type getType() {
        return type;
    }

    public List<LexicalBinding> getElements() {
        return elements;
    }

    @Override
    public boolean isConstDeclaration() {
        return type == Type.Const;
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }

    @Override
    public <V> int accept(IntNodeVisitor<V> visitor, V value) {
        return visitor.visit(this, value);
    }

    @Override
    public <V> void accept(VoidNodeVisitor<V> visitor, V value) {
        visitor.visit(this, value);
    }
}
