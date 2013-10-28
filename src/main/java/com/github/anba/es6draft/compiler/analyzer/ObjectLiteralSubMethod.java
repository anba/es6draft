/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler.analyzer;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

import com.github.anba.es6draft.ast.ObjectLiteral;
import com.github.anba.es6draft.ast.PropertyDefinition;
import com.github.anba.es6draft.ast.PropertyValueDefinition;
import com.github.anba.es6draft.ast.synthetic.ExpressionMethod;
import com.github.anba.es6draft.ast.synthetic.PropertyDefinitionsMethod;

/**
 * Inserts {@link PropertyDefinitionsMethod}s into {@link ObjectLiteral} nodes
 */
public class ObjectLiteralSubMethod extends SubMethod<ObjectLiteral> {
    private static final int MAX_OBJECT_ELEMENT_SIZE = MAX_EXPR_SIZE;
    private static final int MAX_OBJECT_SIZE = 8 * MAX_OBJECT_ELEMENT_SIZE;
    private static final int MAX_SPREAD_SIZE = 4 * MAX_OBJECT_ELEMENT_SIZE;

    private static class ObjectElement extends NodeElement<PropertyDefinition> {
        ObjectElement(PropertyDefinition node, int index, int size) {
            super(node, index, size);
        }

        int export() {
            assert node instanceof PropertyValueDefinition : node.getClass();
            PropertyValueDefinition valDef = (PropertyValueDefinition) node;

            int savedSize = -size + EXPR_METHOD_SIZE;
            this.node = new PropertyValueDefinition(valDef.getBeginPosition(),
                    valDef.getEndPosition(), valDef.getPropertyName(), new ExpressionMethod(
                            valDef.getPropertyValue()));
            this.size = EXPR_METHOD_SIZE;
            return savedSize;
        }

        static List<ObjectElement> from(List<PropertyDefinition> properties) {
            CodeSizeVisitor visitor = new CodeSizeVisitor();
            CodeSizeHandler handler = new EmptyHandler();
            List<ObjectElement> list = new ArrayList<>(properties.size());
            for (int i = 0, len = properties.size(); i < len; i++) {
                PropertyDefinition property = properties.get(i);
                int size = property.accept(visitor, handler);
                list.add(new ObjectElement(property, i, size));
            }
            return list;
        }
    }

    private static class ObjectConflater extends Conflater<ObjectElement, PropertyDefinition> {
        @Override
        protected int getSize(ObjectElement source) {
            return source.size;
        }

        @Override
        protected PropertyDefinition newTarget(List<PropertyDefinition> list) {
            return new PropertyDefinitionsMethod(list);
        }
    }

    @Override
    int processNode(ObjectLiteral node, int oldSize) {
        List<PropertyDefinition> newProperties = new ArrayList<>(node.getProperties());
        List<ObjectElement> list = ObjectElement.from(newProperties);
        int accSize = oldSize;

        // replace single big elements with method-expressions
        PriorityQueue<ObjectElement> pq = new PriorityQueue<>(list);
        while (!pq.isEmpty() && pq.peek().size > MAX_OBJECT_ELEMENT_SIZE) {
            ObjectElement element = pq.remove();

            // export and update entry
            accSize += element.export();
            newProperties.set(element.index, element.node);
        }

        if (accSize > MAX_OBJECT_SIZE) {
            // compact multiple elements with object-spreads
            new ObjectConflater().conflate(list, newProperties, MAX_SPREAD_SIZE);
        }

        node.setProperties(newProperties);

        return validateSize(node);
    }
}
