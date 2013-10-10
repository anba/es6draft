/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

/**
 * <h1>12 ECMAScript Language: Expressions</h1><br>
 * <h2>12.1 Primary Expressions</h2>
 * <ul>
 * <li>12.1.8 Regular Expression Literals
 * </ul>
 */
public class RegularExpressionLiteral extends Expression {
    private String flags;
    private String regexp;

    public RegularExpressionLiteral(long sourcePosition, String regexp, String flags) {
        super(sourcePosition);
        this.regexp = regexp;
        this.flags = flags;
    }

    public String getRegexp() {
        return regexp;
    }

    public String getFlags() {
        return flags;
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }
}
