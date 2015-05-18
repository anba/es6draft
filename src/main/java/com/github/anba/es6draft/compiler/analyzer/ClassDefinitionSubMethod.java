/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler.analyzer;

import java.util.List;

import com.github.anba.es6draft.ast.ClassDefinition;
import com.github.anba.es6draft.ast.PropertyDefinition;
import com.github.anba.es6draft.ast.synthetic.MethodDefinitionsMethod;

/**
 * Inserts {@link MethodDefinitionsMethod}s into {@link ClassDefinition} nodes
 */
final class ClassDefinitionSubMethod extends ListSubMethod<ClassDefinition> {
    private static final int MAX_CLASS_SIZE = 8 * MAX_EXPR_SIZE;
    private static final int MAX_SPREAD_SIZE = 4 * MAX_EXPR_SIZE;
    private static final int PROPDEF_METHOD_SIZE = 10;

    private static final class ClassElement extends NodeElement<PropertyDefinition> {
        ClassElement(PropertyDefinition node, int size, int index) {
            super(node, size, index);
        }

        @Override
        protected final PropertyDefinition createReplacement() {
            throw new AssertionError();
        }

        @Override
        protected final int getReplacementSize() {
            throw new AssertionError();
        }
    }

    private static final class ClassElementMapper implements
            NodeElementMapper<PropertyDefinition, ClassElement> {
        @Override
        public ClassElement map(PropertyDefinition node, int size, int index) {
            return new ClassElement(node, size, index);
        }
    }

    private static final class ClassConflater extends Conflater<ClassElement, PropertyDefinition> {
        @Override
        protected int getSourceSize(ClassElement source) {
            return source.getSize();
        }

        @Override
        protected int getTargetSize() {
            return PROPDEF_METHOD_SIZE;
        }

        @Override
        protected PropertyDefinition newTarget(List<PropertyDefinition> list) {
            return new MethodDefinitionsMethod(list);
        }
    }

    @Override
    int processNode(ClassDefinition node, int oldSize) {
        List<PropertyDefinition> newProperties = newNodes(oldSize, node.getProperties(),
                new ClassElementMapper(), new ClassConflater(), MAX_CLASS_SIZE, MAX_SPREAD_SIZE);
        node.setProperties(newProperties);
        return validateSize(node);
    }
}
