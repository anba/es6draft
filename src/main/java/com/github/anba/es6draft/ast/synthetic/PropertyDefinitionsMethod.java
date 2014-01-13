/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast.synthetic;

import java.util.List;

import com.github.anba.es6draft.ast.NodeVisitor;
import com.github.anba.es6draft.ast.PropertyDefinition;
import com.github.anba.es6draft.ast.PropertyName;

/**
 * List of {@link PropertyDefinition}s as an external Java method
 */
public class PropertyDefinitionsMethod extends PropertyDefinition {
    private List<PropertyDefinition> properties;

    public PropertyDefinitionsMethod(List<PropertyDefinition> properties) {
        super(first(properties).getBeginPosition(), last(properties).getEndPosition());
        this.properties = properties;
    }

    public List<PropertyDefinition> getProperties() {
        return properties;
    }

    @Override
    public PropertyName getPropertyName() {
        throw new IllegalStateException();
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
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
