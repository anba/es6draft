/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import java.util.ArrayList;
import java.util.List;

import com.github.anba.es6draft.ast.MethodDefinition;
import com.github.anba.es6draft.ast.Node;
import com.github.anba.es6draft.ast.PropertyDefinition;
import com.github.anba.es6draft.ast.PropertyValueDefinition;
import com.github.anba.es6draft.ast.synthetic.MethodDefinitionsMethod;
import com.github.anba.es6draft.compiler.assembler.Variable;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryConstructorFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>14 ECMAScript Language: Functions and Classes</h1>
 * <ul>
 * <li>14.5 Class Definitions
 * </ul>
 */
final class ClassPropertyGenerator extends DefaultCodeGenerator<Void, ExpressionVisitor> {
    private final Variable<OrdinaryConstructorFunction> constructor;
    private final Variable<OrdinaryObject> prototype;
    private final Variable<ArrayList<Object>> decorators;
    private final PropertyGenerator propgen;

    private ClassPropertyGenerator(CodeGenerator codegen, Variable<OrdinaryConstructorFunction> constructor,
            Variable<OrdinaryObject> prototype, Variable<ArrayList<Object>> decorators) {
        super(codegen);
        this.constructor = constructor;
        this.prototype = prototype;
        this.decorators = decorators;
        this.propgen = codegen.propertyGenerator(decorators);
    }

    static void ClassPropertyEvaluation(CodeGenerator codegen, List<? extends PropertyDefinition> properties,
            Variable<OrdinaryConstructorFunction> constructor, Variable<OrdinaryObject> prototype,
            Variable<ArrayList<Object>> decorators, ExpressionVisitor mv) {
        ClassPropertyGenerator classgen = new ClassPropertyGenerator(codegen, constructor, prototype, decorators);
        for (PropertyDefinition property : properties) {
            property.accept(classgen, mv);
        }
    }

    @Override
    protected Void visit(Node node, ExpressionVisitor mv) {
        throw new IllegalStateException(String.format("node-class: %s", node.getClass()));
    }

    @Override
    public Void visit(MethodDefinition node, ExpressionVisitor mv) {
        if (!node.isClassConstructor()) {
            Variable<? extends OrdinaryObject> obj = node.isStatic() ? constructor : prototype;
            if (!node.getDecorators().isEmpty()) {
                addDecoratorObject(decorators, obj, mv);
            }
            // stack: [] -> []
            mv.load(obj);
            node.accept(propgen, mv);
        }
        return null;
    }

    @Override
    public Void visit(PropertyValueDefinition node, ExpressionVisitor mv) {
        // stack: [] -> []
        mv.load(constructor);
        node.accept(propgen, mv);
        return null;
    }

    @Override
    public Void visit(MethodDefinitionsMethod node, ExpressionVisitor mv) {
        codegen.compile(node, decorators != null, mv);

        // stack: [] -> []
        mv.loadExecutionContext();
        mv.load(constructor);
        mv.load(prototype);
        if (decorators != null) {
            mv.load(decorators);
        } else {
            mv.anull();
        }
        mv.invoke(codegen.methodDesc(node));

        return null;
    }
}
