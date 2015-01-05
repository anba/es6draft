/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import java.util.List;

import com.github.anba.es6draft.ast.ClassDefinition;
import com.github.anba.es6draft.ast.MethodDefinition;
import com.github.anba.es6draft.ast.Node;
import com.github.anba.es6draft.ast.PropertyDefinition;
import com.github.anba.es6draft.ast.synthetic.MethodDefinitionsMethod;
import com.github.anba.es6draft.compiler.assembler.Variable;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>14 ECMAScript Language: Functions and Classes</h1>
 * <ul>
 * <li>14.5 Class Definitions
 * </ul>
 */
final class ClassPropertyGenerator extends DefaultCodeGenerator<Void, ExpressionVisitor> {
    private final ClassDefinition classDefinition;
    private final Variable<OrdinaryFunction> F;
    private final Variable<OrdinaryObject> proto;

    private ClassPropertyGenerator(CodeGenerator codegen, ClassDefinition classDefinition,
            Variable<OrdinaryFunction> F, Variable<OrdinaryObject> proto) {
        super(codegen);
        this.classDefinition = classDefinition;
        this.F = F;
        this.proto = proto;
    }

    static void ClassPropertyEvaluation(CodeGenerator codegen, ClassDefinition def,
            List<? extends PropertyDefinition> properties, Variable<OrdinaryFunction> function,
            Variable<OrdinaryObject> proto, ExpressionVisitor mv) {
        ClassPropertyGenerator cdg = new ClassPropertyGenerator(codegen, def, function, proto);
        for (PropertyDefinition property : properties) {
            property.accept(cdg, mv);
        }
    }

    @Override
    protected Void visit(Node node, ExpressionVisitor mv) {
        throw new IllegalStateException(String.format("node-class: %s", node.getClass()));
    }

    @Override
    public Void visit(MethodDefinition node, ExpressionVisitor mv) {
        if (node != classDefinition.getConstructor()) {
            if (node.isStatic()) {
                mv.load(F);
            } else {
                mv.load(proto);
            }
            codegen.propertyDefinition(node, mv);
        }

        return null;
    }

    @Override
    public Void visit(MethodDefinitionsMethod node, ExpressionVisitor mv) {
        codegen.compile(classDefinition, node, mv);

        // stack: [] -> [cx, F, proto]
        mv.loadExecutionContext();
        mv.load(F);
        mv.load(proto);

        // stack: [cx, F, proto] -> []
        mv.invoke(codegen.methodDesc(node));

        return null;
    }
}
