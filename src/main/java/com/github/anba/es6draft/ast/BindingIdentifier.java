/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import com.github.anba.es6draft.ast.scope.Name;

/**
 * <h1>13 ECMAScript Language: Statements and Declarations</h1><br>
 * <h2>13.2 Declarations and the Variable Statement</h2>
 * <ul>
 * <li>13.2.1 Let and Const Declarations
 * </ul>
 */
public final class BindingIdentifier extends Binding {
    private final Name name;
    private Name resolvedName;

    public BindingIdentifier(long beginPosition, long endPosition, String name) {
        super(beginPosition, endPosition);
        this.name = new Name(name);
    }

    public Name getName() {
        return name;
    }

    public Name getResolvedName() {
        return resolvedName;
    }

    public void setResolvedName(Name resolvedName) {
        assert resolvedName != null && resolvedName.isResolved();
        assert this.resolvedName == null : String.format("%s: <%s> != <%s>", name,
                this.resolvedName.getScope(), resolvedName.getScope());
        this.resolvedName = resolvedName;
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
