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
import com.github.anba.es6draft.compiler.ExpressionVisitor.Register;
import com.github.anba.es6draft.compiler.InstructionVisitor.MethodDesc;
import com.github.anba.es6draft.compiler.InstructionVisitor.MethodType;

/**
 *
 */
class PropertyGenerator extends DefaultCodeGenerator<Void, ExpressionVisitor> {
    private static class Methods {
        // class: ScriptRuntime
        static final MethodDesc ScriptRuntime_EvaluatePropertyDefinition = MethodDesc.create(
                MethodType.Static, Types.ScriptRuntime, "EvaluatePropertyDefinition", Type
                        .getMethodType(Type.VOID_TYPE, Types.ScriptObject, Types.String,
                                Types.RuntimeInfo$Function, Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_EvaluatePropertyDefinitionGenerator = MethodDesc
                .create(MethodType.Static, Types.ScriptRuntime,
                        "EvaluatePropertyDefinitionGenerator", Type.getMethodType(Type.VOID_TYPE,
                                Types.ScriptObject, Types.String, Types.RuntimeInfo$Function,
                                Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_EvaluatePropertyDefinitionGetter = MethodDesc.create(
                MethodType.Static, Types.ScriptRuntime, "EvaluatePropertyDefinitionGetter", Type
                        .getMethodType(Type.VOID_TYPE, Types.ScriptObject, Types.String,
                                Types.RuntimeInfo$Function, Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_EvaluatePropertyDefinitionSetter = MethodDesc.create(
                MethodType.Static, Types.ScriptRuntime, "EvaluatePropertyDefinitionSetter", Type
                        .getMethodType(Type.VOID_TYPE, Types.ScriptObject, Types.String,
                                Types.RuntimeInfo$Function, Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_defineProperty = MethodDesc.create(MethodType.Static,
                Types.ScriptRuntime, "defineProperty", Type.getMethodType(Type.VOID_TYPE,
                        Types.ScriptObject, Types.String, Types.Object, Types.Realm));

        static final MethodDesc ScriptRuntime_defineProtoProperty = MethodDesc.create(
                MethodType.Static, Types.ScriptRuntime, "defineProtoProperty",
                Type.getMethodType(Type.VOID_TYPE, Types.ScriptObject, Types.Object, Types.Realm));
    }

    public PropertyGenerator(CodeGenerator codegen) {
        super(codegen);
    }

    @Override
    protected Void visit(Node node, ExpressionVisitor mv) {
        throw new IllegalStateException(String.format("node-class: %s", node.getClass()));
    }

    @Override
    protected Void visit(Expression node, ExpressionVisitor mv) {
        ValType type = codegen.expression(node, mv);
        mv.toBoxed(type);
        return null;
    }

    @Override
    public Void visit(MethodDefinition node, ExpressionVisitor mv) {
        codegen.compile(node);

        // Runtime Semantics: Evaluation -> MethodDefinition
        // stack: [<object>]
        mv.aconst(PropName(node));
        mv.invokestatic(codegen.getClassName(), codegen.methodName(node) + "_rti",
                Type.getMethodDescriptor(Types.RuntimeInfo$Function));
        mv.load(Register.ExecutionContext);

        switch (node.getType()) {
        case Function:
            mv.invoke(Methods.ScriptRuntime_EvaluatePropertyDefinition);
            break;
        case Generator:
            mv.invoke(Methods.ScriptRuntime_EvaluatePropertyDefinitionGenerator);
            break;
        case Getter:
            mv.invoke(Methods.ScriptRuntime_EvaluatePropertyDefinitionGetter);
            break;
        case Setter:
            mv.invoke(Methods.ScriptRuntime_EvaluatePropertyDefinitionSetter);
            break;
        default:
            assert false : "invalid method type";
            throw new IllegalStateException();
        }

        return null;
    }

    @Override
    public Void visit(PropertyNameDefinition node, ExpressionVisitor mv) {
        Identifier propertyName = node.getPropertyName();

        String propName = propertyName.getName();
        if ("__proto__".equals(propName)) {
            propertyName.accept(this, mv);
            invokeGetValue(propertyName, mv);
            mv.load(Register.Realm);
            mv.invoke(Methods.ScriptRuntime_defineProtoProperty);
        } else {
            mv.aconst(propName);
            propertyName.accept(this, mv);
            invokeGetValue(propertyName, mv);
            mv.load(Register.Realm);
            mv.invoke(Methods.ScriptRuntime_defineProperty);
        }

        return null;
    }

    @Override
    public Void visit(PropertyValueDefinition node, ExpressionVisitor mv) {
        PropertyName propertyName = node.getPropertyName();
        Expression propertyValue = node.getPropertyValue();

        String propName = PropName(propertyName);
        if ("__proto__".equals(propName)) {
            propertyValue.accept(this, mv);
            invokeGetValue(propertyValue, mv);
            mv.load(Register.Realm);
            mv.invoke(Methods.ScriptRuntime_defineProtoProperty);
        } else {
            mv.aconst(propName);
            propertyValue.accept(this, mv);
            invokeGetValue(propertyValue, mv);
            mv.load(Register.Realm);
            mv.invoke(Methods.ScriptRuntime_defineProperty);
        }

        return null;
    }
}
