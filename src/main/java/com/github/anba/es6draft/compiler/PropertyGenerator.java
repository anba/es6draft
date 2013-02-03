/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static com.github.anba.es6draft.semantics.StaticSemantics.PropName;

import org.objectweb.asm.Type;

import com.github.anba.es6draft.ast.Expression;
import com.github.anba.es6draft.ast.Identifier;
import com.github.anba.es6draft.ast.MethodDefinition;
import com.github.anba.es6draft.ast.Node;
import com.github.anba.es6draft.ast.PropertyName;
import com.github.anba.es6draft.ast.PropertyNameDefinition;
import com.github.anba.es6draft.ast.PropertyValueDefinition;
import com.github.anba.es6draft.compiler.MethodGenerator.Register;

/**
 *
 */
class PropertyGenerator extends DefaultCodeGenerator<Void, MethodGenerator> {

    /* ----------------------------------------------------------------------------------------- */

    public PropertyGenerator(CodeGenerator codegen) {
        super(codegen);
    }

    @Override
    protected Void visit(Node node, MethodGenerator mv) {
        throw new IllegalStateException(String.format("node-class: %s", node.getClass()));
    }

    @Override
    protected Void visit(Expression node, MethodGenerator mv) {
        ValType type = codegen.expression(node, mv);
        mv.toBoxed(type);
        return null;
    }

    /* ----------------------------------------------------------------------------------------- */

    @Override
    public Void visit(MethodDefinition node, MethodGenerator mv) {
        codegen.compile(node);

        // Runtime Semantics: Evaluation -> MethodDefinition
        // stack: [<object>]
        mv.aconst(PropName(node));
        mv.invokestatic(codegen.getClassName(), codegen.methodName(node) + "_rti",
                Type.getMethodDescriptor(Types.RuntimeInfo$Function));
        mv.load(Register.ExecutionContext);

        switch (node.getType()) {
        case Function:
            mv.invokestatic(Methods.ScriptRuntime_EvaluatePropertyDefinition);
            break;
        case Generator:
            mv.invokestatic(Methods.ScriptRuntime_EvaluatePropertyDefinitionGenerator);
            break;
        case Getter:
            mv.invokestatic(Methods.ScriptRuntime_EvaluatePropertyDefinitionGetter);
            break;
        case Setter:
            mv.invokestatic(Methods.ScriptRuntime_EvaluatePropertyDefinitionSetter);
            break;
        default:
            assert false : "invalid method type";
            throw new IllegalStateException();
        }

        return null;
    }

    @Override
    public Void visit(PropertyNameDefinition node, MethodGenerator mv) {
        Identifier propertyName = node.getPropertyName();

        String propName = propertyName.getName();
        mv.aconst(propName);
        // performs "Identifier Resolution" automatically
        propertyName.accept(this, mv);
        invokeGetValue(propertyName, mv);
        mv.invokestatic(Methods.ScriptRuntime_defineProperty);

        return null;
    }

    @Override
    public Void visit(PropertyValueDefinition node, MethodGenerator mv) {
        PropertyName propertyName = node.getPropertyName();
        Expression propertyValue = node.getPropertyValue();

        String propName = PropName(propertyName);
        mv.aconst(propName);
        propertyValue.accept(this, mv);
        invokeGetValue(propertyValue, mv);
        mv.invokestatic(Methods.ScriptRuntime_defineProperty);

        return null;
    }
}
