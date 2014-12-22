/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import static com.github.anba.es6draft.semantics.StaticSemantics.ConstructorMethod;

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
    private final BindingIdentifier identifier;
    private final Expression heritage;
    private final List<MethodDefinition> methods;
    private final MethodDefinition constructor;
    private final String className;
    private List<PropertyDefinition> properties;

    public ClassDeclaration(long beginPosition, long endPosition, BlockScope scope,
            BindingIdentifier identifier, Expression heritage, List<MethodDefinition> methods,
            String className) {
        super(beginPosition, endPosition);
        this.scope = scope;
        this.identifier = identifier;
        this.heritage = heritage;
        this.methods = methods;
        this.constructor = ConstructorMethod(methods);
        this.className = className;
    }

    @Override
    public BlockScope getScope() {
        return scope;
    }

    @Override
    public String getClassName() {
        return className;
    }

    @Override
    public BindingIdentifier getIdentifier() {
        return identifier;
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
}
