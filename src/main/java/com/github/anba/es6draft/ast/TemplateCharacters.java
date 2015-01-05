/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

/**
 * <h1>12 ECMAScript Language: Expressions</h1><br>
 * <h2>12.2 Primary Expression</h2>
 * <ul>
 * <li>12.2.9 Template Literals
 * </ul>
 */
public final class TemplateCharacters extends Expression {
    private final String value;
    private final String rawValue;

    public TemplateCharacters(long beginPosition, long endPosition, String value, String rawValue) {
        super(beginPosition, endPosition);
        this.value = value;
        this.rawValue = rawValue;
    }

    public String getValue() {
        return value;
    }

    public String getRawValue() {
        return rawValue;
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
