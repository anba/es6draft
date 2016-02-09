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
 * <li>12.2.8 Regular Expression Literals
 * </ul>
 */
public final class RegularExpressionLiteral extends Expression {
    private final String flags;
    private final String regexp;

    public RegularExpressionLiteral(long beginPosition, long endPosition, String regexp,
            String flags) {
        super(beginPosition, endPosition);
        this.regexp = regexp;
        this.flags = flags;
    }

    /**
     * Returns the regular expression pattern.
     * 
     * @return the regular expression pattern
     */
    public String getRegexp() {
        return regexp;
    }

    /**
     * Returns the regular expression flags.
     * 
     * @return the regular expression flags
     */
    public String getFlags() {
        return flags;
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
