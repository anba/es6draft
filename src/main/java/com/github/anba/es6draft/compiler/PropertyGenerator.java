/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static com.github.anba.es6draft.semantics.StaticSemantics.IsAnonymousFunctionDefinition;
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
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;

/**
 * 12.1.5.7 Runtime Semantics: PropertyDefinitionEvaluation<br>
 * 14.3.7 Runtime Semantics: PropertyDefinitionEvaluation<br>
 * 14.4.11 Runtime Semantics: PropertyDefinitionEvaluation
 */
final class PropertyGenerator extends
        DefaultCodeGenerator<DefaultCodeGenerator.ValType, ExpressionVisitor> {
    private static final class Methods {
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
    }

    public PropertyGenerator(CodeGenerator codegen) {
        super(codegen);
    }

    @Override
    protected ValType visit(Node node, ExpressionVisitor mv) {
        throw new IllegalStateException(String.format("node-class: %s", node.getClass()));
    }

    /**
     * 12.1.5.6 Runtime Semantics: Evaluation
     * <p>
     * ComputedPropertyName : [ AssignmentExpression ]
     */
    @Override
    public ValType visit(ComputedPropertyName node, ExpressionVisitor mv) {
        ValType type = expressionValue(node.getExpression(), mv);
        ToPropertyKey(type, mv);

        return type != ValType.Any ? ValType.String : ValType.Any;
    }

    @Override
    public ValType visit(PropertyDefinitionsMethod node, ExpressionVisitor mv) {
        codegen.compile(node, mv);

        // stack: [<object>] -> [cx, <object>]
        mv.loadExecutionContext();
        mv.swap();

        // stack: [<object>] -> []
        mv.invoke(codegen.methodDesc(node));

        return null;
    }

    /**
     * 14.3.7 Runtime Semantics: PropertyDefinitionEvaluation<br>
     * 14.4.11 Runtime Semantics: PropertyDefinitionEvaluation
     */
    @Override
    public ValType visit(MethodDefinition node, ExpressionVisitor mv) {
        codegen.compile(node);

        // Runtime Semantics: Evaluation -> MethodDefinition
        // stack: [<object>]

        String propName = PropName(node);
        if (propName == null) {
            assert node.getPropertyName() instanceof ComputedPropertyName;
            node.getPropertyName().accept(this, mv);

            mv.invoke(codegen.methodDesc(node, FunctionName.RTI));
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
            mv.invoke(codegen.methodDesc(node, FunctionName.RTI));
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

    /**
     * 12.1.5.7 Runtime Semantics: PropertyDefinitionEvaluation
     * <p>
     * PropertyDefinition : IdentifierName
     */
    @Override
    public ValType visit(PropertyNameDefinition node, ExpressionVisitor mv) {
        Identifier propertyName = node.getPropertyName();
        String propName = PropName(propertyName);
        assert propName != null;

        mv.aconst(propName);
        expressionBoxedValue(propertyName, mv);
        mv.loadExecutionContext();
        mv.invoke(Methods.ScriptRuntime_defineProperty_String);

        return null;
    }

    /**
     * 12.1.5.7 Runtime Semantics: PropertyDefinitionEvaluation
     * <p>
     * PropertyDefinition : PropertyName : AssignmentExpression
     */
    @Override
    public ValType visit(PropertyValueDefinition node, ExpressionVisitor mv) {
        // Runtime Semantics: Evaluation -> Property Definition Evaluation
        // stack: [<object>]

        PropertyName propertyName = node.getPropertyName();
        Expression propertyValue = node.getPropertyValue();

        String propName = PropName(propertyName);
        if (propName == null) {
            assert propertyName instanceof ComputedPropertyName;
            ValType type = propertyName.accept(this, mv);

            // stack: [<object>, pk]
            expressionBoxedValue(propertyValue, mv);
            // stack: [<object>, pk, value]
            if (IsAnonymousFunctionDefinition(propertyValue)) {
                SetFunctionName(propertyValue, type, mv);
            }
            mv.loadExecutionContext();
            mv.invoke(Methods.ScriptRuntime_defineProperty);
        } else if ("__proto__".equals(propName)
                && codegen.isEnabled(CompatibilityOption.ProtoInitialiser)) {
            expressionBoxedValue(propertyValue, mv);
            // TODO: SetFunctionName() ?
            mv.loadExecutionContext();
            mv.invoke(Methods.ScriptRuntime_defineProtoProperty);
        } else {
            mv.aconst(propName);
            expressionBoxedValue(propertyValue, mv);
            if (IsAnonymousFunctionDefinition(propertyValue)) {
                SetFunctionName(propertyValue, propName, mv);
            }
            mv.loadExecutionContext();
            mv.invoke(Methods.ScriptRuntime_defineProperty_String);
        }

        return null;
    }
}
