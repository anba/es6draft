/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast.synthetic;

import java.util.Collections;
import java.util.List;

import com.github.anba.es6draft.ast.IntNodeVisitor;
import com.github.anba.es6draft.ast.NodeVisitor;
import com.github.anba.es6draft.ast.PropertyDefinition;
import com.github.anba.es6draft.ast.PropertyName;
import com.github.anba.es6draft.ast.VoidNodeVisitor;

/**
 * List of {@link PropertyDefinition}s as an external Java method.
 */
public final class PropertyDefinitionsMethod extends PropertyDefinition {
    private final List<PropertyDefinition> properties;

    public PropertyDefinitionsMethod(PropertyDefinition property) {
        super(property.getBeginPosition(), property.getEndPosition());
        this.properties = Collections.singletonList(property);
    }

    public PropertyDefinitionsMethod(List<PropertyDefinition> properties) {
        super(first(properties).getBeginPosition(), last(properties).getEndPosition());
        this.properties = properties;
    }

    public List<PropertyDefinition> getProperties() {
        return properties;
    }

    @Override
    public PropertyName getPropertyName() {
        throw new AssertionError();
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

    private static PropertyDefinition first(List<PropertyDefinition> properties) {
        assert !properties.isEmpty();
        return properties.get(0);
    }

    private static PropertyDefinition last(List<PropertyDefinition> properties) {
        assert !properties.isEmpty();
        return properties.get(properties.size() - 1);
    }
}
