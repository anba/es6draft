/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import java.util.List;

/**
 * <h1>14 ECMAScript Language: Functions and Classes</h1>
 * <ul>
 * <li>14.5 Class Definitions
 * </ul>
 */
public final class ClassDeclaration extends Declaration implements ClassDefinition {
    private final BlockScope scope;
    private final BindingIdentifier name;
    private final Expression heritage;
    private final List<MethodDefinition> staticMethods;
    private final List<MethodDefinition> prototypeMethods;

    public ClassDeclaration(long beginPosition, long endPosition, BlockScope scope,
            BindingIdentifier name, Expression heritage, List<MethodDefinition> staticMethods,
            List<MethodDefinition> prototypeMethods) {
        super(beginPosition, endPosition);
        this.scope = scope;
        this.name = name;
        this.heritage = heritage;
        this.staticMethods = staticMethods;
        this.prototypeMethods = prototypeMethods;
    }

    @Override
    public BlockScope getScope() {
        return scope;
    }

    @Override
    public BindingIdentifier getName() {
        return name;
    }

    @Override
    public Expression getHeritage() {
        return heritage;
    }

    @Override
    public List<MethodDefinition> getStaticMethods() {
        return staticMethods;
    }

    @Override
    public List<MethodDefinition> getPrototypeMethods() {
        return prototypeMethods;
    }

    @Override
    public boolean isConstDeclaration() {
        return false;
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }
}
