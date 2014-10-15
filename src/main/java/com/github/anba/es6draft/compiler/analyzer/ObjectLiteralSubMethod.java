/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler.analyzer;

import java.util.List;

import com.github.anba.es6draft.ast.ObjectLiteral;
import com.github.anba.es6draft.ast.PropertyDefinition;
import com.github.anba.es6draft.ast.PropertyValueDefinition;
import com.github.anba.es6draft.ast.synthetic.ExpressionMethod;
import com.github.anba.es6draft.ast.synthetic.PropertyDefinitionsMethod;

/**
 * Inserts {@link PropertyDefinitionsMethod}s into {@link ObjectLiteral} nodes
 */
final class ObjectLiteralSubMethod extends ListSubMethod<ObjectLiteral> {
    private static final int MAX_OBJECT_ELEMENT_SIZE = MAX_EXPR_SIZE;
    private static final int MAX_OBJECT_SIZE = 8 * MAX_OBJECT_ELEMENT_SIZE;
    private static final int MAX_SPREAD_SIZE = 4 * MAX_OBJECT_ELEMENT_SIZE;
    private static final int PROPDEF_METHOD_SIZE = 10;

    private static final class ObjectElement extends NodeElement<PropertyDefinition> {
        ObjectElement(PropertyDefinition node, int size, int index) {
            super(node, size, index);
        }

        @Override
        protected final PropertyDefinition createReplacement() {
            assert getNode() instanceof PropertyValueDefinition : getNode().getClass();
            PropertyValueDefinition valDef = (PropertyValueDefinition) getNode();
            return new PropertyValueDefinition(valDef.getBeginPosition(), valDef.getEndPosition(),
                    valDef.getPropertyName(), new ExpressionMethod(valDef.getPropertyValue()));
        }

        @Override
        protected final int getReplacementSize() {
            return EXPR_METHOD_SIZE;
        }
    }

    private static final class ObjectElementMapper implements
            NodeElementMapper<PropertyDefinition, ObjectElement> {
        @Override
        public ObjectElement map(PropertyDefinition node, int size, int index) {
            return new ObjectElement(node, size, index);
        }
    }

    private static final class ObjectConflater extends Conflater<ObjectElement, PropertyDefinition> {
        @Override
        protected int getSourceSize(ObjectElement source) {
            return source.getSize();
        }

        @Override
        protected int getTargetSize() {
            return PROPDEF_METHOD_SIZE;
        }

        @Override
        protected PropertyDefinition newTarget(List<PropertyDefinition> list) {
            return new PropertyDefinitionsMethod(list);
        }
    }

    @Override
    int processNode(ObjectLiteral node, int oldSize) {
        List<PropertyDefinition> newProperties = newNodes(oldSize, node.getProperties(),
                new ObjectElementMapper(), new ObjectConflater(), MAX_OBJECT_ELEMENT_SIZE,
                MAX_OBJECT_SIZE, MAX_SPREAD_SIZE);
        node.setProperties(newProperties);
        return validateSize(node);
    }
}
