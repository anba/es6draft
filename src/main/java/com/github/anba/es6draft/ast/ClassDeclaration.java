/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import java.util.List;

import com.github.anba.es6draft.ast.scope.BlockScope;

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
    private final List<MethodDefinition> methods;
    private final MethodDefinition constructor;
    private List<PropertyDefinition> properties;

    public ClassDeclaration(long beginPosition, long endPosition, BlockScope scope,
            BindingIdentifier name, Expression heritage, List<MethodDefinition> methods) {
        super(beginPosition, endPosition);
        this.scope = scope;
        this.name = name;
        this.heritage = heritage;
        this.methods = methods;
        this.constructor = ConstructorMethod(methods);
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
    public List<MethodDefinition> getMethods() {
        return methods;
    }

    @Override
    public MethodDefinition getConstructor() {
        return constructor;
    }

    @Override
    public List<? extends PropertyDefinition> getProperties() {
        if (properties == null) {
            return methods;
        }
        return properties;
    }

    @Override
    public void setProperties(List<PropertyDefinition> properties) {
        this.properties = properties;
    }

    @Override
    public boolean isConstDeclaration() {
        return false;
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

    // 14.5.3 Static Semantics: ConstructorMethod
    private static MethodDefinition ConstructorMethod(List<MethodDefinition> methods) {
        for (MethodDefinition m : methods) {
            if (!m.isStatic() && "constructor".equals(m.getPropertyName().getName())) {
                return m;
            }
        }
        return null;
    }
}
