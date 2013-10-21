/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static com.github.anba.es6draft.semantics.StaticSemantics.PropName;

import org.objectweb.asm.Type;

import com.github.anba.es6draft.ast.ComputedPropertyName;
import com.github.anba.es6draft.ast.Expression;
import com.github.anba.es6draft.ast.Identifier;
import com.github.anba.es6draft.ast.MethodDefinition;
import com.github.anba.es6draft.ast.Node;
import com.github.anba.es6draft.ast.PropertyName;
import com.github.anba.es6draft.ast.PropertyNameDefinition;
import com.github.anba.es6draft.ast.PropertyValueDefinition;
import com.github.anba.es6draft.ast.synthetic.PropertyDefinitionsMethod;
import com.github.anba.es6draft.compiler.CodeGenerator.FunctionName;
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
                        .getMethodType(Type.VOID_TYPE, Types.ScriptObject, Types.Object,
                                Types.RuntimeInfo$Function, Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_EvaluatePropertyDefinition_String = MethodDesc
                .create(MethodType.Static, Types.ScriptRuntime, "EvaluatePropertyDefinition", Type
                        .getMethodType(Type.VOID_TYPE, Types.ScriptObject, Types.String,
                                Types.RuntimeInfo$Function, Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_EvaluatePropertyDefinitionGenerator = MethodDesc
                .create(MethodType.Static, Types.ScriptRuntime,
                        "EvaluatePropertyDefinitionGenerator", Type.getMethodType(Type.VOID_TYPE,
                                Types.ScriptObject, Types.Object, Types.RuntimeInfo$Function,
                                Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_EvaluatePropertyDefinitionGenerator_String = MethodDesc
                .create(MethodType.Static, Types.ScriptRuntime,
                        "EvaluatePropertyDefinitionGenerator", Type.getMethodType(Type.VOID_TYPE,
                                Types.ScriptObject, Types.String, Types.RuntimeInfo$Function,
                                Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_EvaluatePropertyDefinitionGetter = MethodDesc.create(
                MethodType.Static, Types.ScriptRuntime, "EvaluatePropertyDefinitionGetter", Type
                        .getMethodType(Type.VOID_TYPE, Types.ScriptObject, Types.Object,
                                Types.RuntimeInfo$Function, Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_EvaluatePropertyDefinitionGetter_String = MethodDesc
                .create(MethodType.Static, Types.ScriptRuntime, "EvaluatePropertyDefinitionGetter",
                        Type.getMethodType(Type.VOID_TYPE, Types.ScriptObject, Types.String,
                                Types.RuntimeInfo$Function, Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_EvaluatePropertyDefinitionSetter = MethodDesc.create(
                MethodType.Static, Types.ScriptRuntime, "EvaluatePropertyDefinitionSetter", Type
                        .getMethodType(Type.VOID_TYPE, Types.ScriptObject, Types.Object,
                                Types.RuntimeInfo$Function, Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_EvaluatePropertyDefinitionSetter_String = MethodDesc
                .create(MethodType.Static, Types.ScriptRuntime, "EvaluatePropertyDefinitionSetter",
                        Type.getMethodType(Type.VOID_TYPE, Types.ScriptObject, Types.String,
                                Types.RuntimeInfo$Function, Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_defineProperty = MethodDesc.create(MethodType.Static,
                Types.ScriptRuntime, "defineProperty", Type.getMethodType(Type.VOID_TYPE,
                        Types.ScriptObject, Types.Object, Types.Object, Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_defineProperty_String = MethodDesc.create(
                MethodType.Static, Types.ScriptRuntime, "defineProperty", Type.getMethodType(
                        Type.VOID_TYPE, Types.ScriptObject, Types.String, Types.Object,
                        Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_defineProtoProperty = MethodDesc.create(
                MethodType.Static, Types.ScriptRuntime, "defineProtoProperty", Type.getMethodType(
                        Type.VOID_TYPE, Types.ScriptObject, Types.Object, Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_ensureNewProperty = MethodDesc.create(
                MethodType.Static, Types.ScriptRuntime, "ensureNewProperty", Type.getMethodType(
                        Type.VOID_TYPE, Types.ScriptObject, Types.Object, Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_ensureNewPropertyGet = MethodDesc.create(
                MethodType.Static, Types.ScriptRuntime, "ensureNewPropertyGet", Type.getMethodType(
                        Type.VOID_TYPE, Types.ScriptObject, Types.Object, Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_ensureNewPropertySet = MethodDesc.create(
                MethodType.Static, Types.ScriptRuntime, "ensureNewPropertySet", Type.getMethodType(
                        Type.VOID_TYPE, Types.ScriptObject, Types.Object, Types.ExecutionContext));

    }

    public PropertyGenerator(CodeGenerator codegen) {
        super(codegen);
    }

    @Override
    protected Void visit(Node node, ExpressionVisitor mv) {
        throw new IllegalStateException(String.format("node-class: %s", node.getClass()));
    }

    @Override
    public Void visit(ComputedPropertyName node, ExpressionVisitor mv) {
        ValType type = expressionValue(node.getExpression(), mv);
        ToPropertyKey(type, mv);

        return null;
    }

    @Override
    public Void visit(PropertyDefinitionsMethod node, ExpressionVisitor mv) {
        codegen.compile(node, mv);

        // stack: [<object>] -> [cx, <object>]
        mv.loadExecutionContext();
        mv.swap();

        // stack: [<object>] -> []
        String desc = Type.getMethodDescriptor(Type.VOID_TYPE, Types.ExecutionContext,
                Types.ScriptObject);
        mv.invokestatic(codegen.getClassName(), codegen.methodName(node), desc);

        return null;
    }

    @Override
    public Void visit(MethodDefinition node, ExpressionVisitor mv) {
        codegen.compile(node);

        // Runtime Semantics: Evaluation -> MethodDefinition
        // stack: [<object>]

        String propName = PropName(node);
        if (propName == null) {
            assert node.getPropertyName() instanceof ComputedPropertyName;
            node.getPropertyName().accept(this, mv);

            // stack: [<object>, pk] -> [<object>, pk, <object>, pk]
            mv.dup2();

            // stack: [<object>, pk, <object>, pk] -> [<object>, pk]
            mv.loadExecutionContext();
            switch (node.getType()) {
            case Function:
            case Generator:
                mv.invoke(Methods.ScriptRuntime_ensureNewProperty);
                break;
            case Getter:
                mv.invoke(Methods.ScriptRuntime_ensureNewPropertyGet);
                break;
            case Setter:
                mv.invoke(Methods.ScriptRuntime_ensureNewPropertySet);
                break;
            }

            mv.invokestatic(codegen.getClassName(), codegen.methodName(node, FunctionName.RTI),
                    Type.getMethodDescriptor(Types.RuntimeInfo$Function));
            mv.loadExecutionContext();

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
        } else {
            mv.aconst(propName);
            mv.invokestatic(codegen.getClassName(), codegen.methodName(node, FunctionName.RTI),
                    Type.getMethodDescriptor(Types.RuntimeInfo$Function));
            mv.loadExecutionContext();

            switch (node.getType()) {
            case Function:
                mv.invoke(Methods.ScriptRuntime_EvaluatePropertyDefinition_String);
                break;
            case Generator:
                mv.invoke(Methods.ScriptRuntime_EvaluatePropertyDefinitionGenerator_String);
                break;
            case Getter:
                mv.invoke(Methods.ScriptRuntime_EvaluatePropertyDefinitionGetter_String);
                break;
            case Setter:
                mv.invoke(Methods.ScriptRuntime_EvaluatePropertyDefinitionSetter_String);
                break;
            default:
                assert false : "invalid method type";
                throw new IllegalStateException();
            }
        }

        return null;
    }

    @Override
    public Void visit(PropertyNameDefinition node, ExpressionVisitor mv) {
        Identifier propertyName = node.getPropertyName();
        String propName = PropName(propertyName);
        assert propName != null;

        mv.aconst(propName);
        ValType type = expressionValue(propertyName, mv);
        mv.toBoxed(type);
        mv.loadExecutionContext();
        mv.invoke(Methods.ScriptRuntime_defineProperty_String);

        return null;
    }

    @Override
    public Void visit(PropertyValueDefinition node, ExpressionVisitor mv) {
        // Runtime Semantics: Evaluation -> Property Definition Evaluation
        // stack: [<object>]

        PropertyName propertyName = node.getPropertyName();
        Expression propertyValue = node.getPropertyValue();

        String propName = PropName(propertyName);
        if (propName == null) {
            assert propertyName instanceof ComputedPropertyName;
            propertyName.accept(this, mv);
            if (mv.isStrict()) {
                // stack: [<object>, pk] -> [<object>, pk, <object>, pk]
                mv.dup2();
            }
            ValType type = expressionValue(propertyValue, mv);
            mv.toBoxed(type);
            if (mv.isStrict()) {
                // stack: [<object>, pk, <object>, pk, value] -> [<object>, pk, value, <object>, pk]
                mv.dupX2();
                mv.pop();
                // stack: [<object>, pk, value, <object>, pk] -> [<object>, pk, value]
                mv.loadExecutionContext();
                mv.invoke(Methods.ScriptRuntime_ensureNewProperty);
            }
            // stack: [<object>, pk, value]
            mv.loadExecutionContext();
            mv.invoke(Methods.ScriptRuntime_defineProperty);
        } else if ("__proto__".equals(propName)) {
            ValType type = expressionValue(propertyValue, mv);
            mv.toBoxed(type);
            mv.loadExecutionContext();
            mv.invoke(Methods.ScriptRuntime_defineProtoProperty);
        } else {
            mv.aconst(propName);
            ValType type = expressionValue(propertyValue, mv);
            mv.toBoxed(type);
            mv.loadExecutionContext();
            mv.invoke(Methods.ScriptRuntime_defineProperty_String);
        }

        return null;
    }
}
