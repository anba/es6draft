/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static com.github.anba.es6draft.semantics.StaticSemantics.ConstructorMethod;
import static com.github.anba.es6draft.semantics.StaticSemantics.IsAnonymousFunctionDefinition;
import static com.github.anba.es6draft.semantics.StaticSemantics.PropName;

import org.objectweb.asm.Type;

import com.github.anba.es6draft.ast.*;
import com.github.anba.es6draft.ast.synthetic.PropertyDefinitionsMethod;
import com.github.anba.es6draft.compiler.CodeGenerator.FunctionName;
import com.github.anba.es6draft.compiler.InstructionVisitor.MethodDesc;
import com.github.anba.es6draft.compiler.InstructionVisitor.MethodType;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;

/**
 * 12.1.5.8 Runtime Semantics: PropertyDefinitionEvaluation<br>
 * 14.3.9 Runtime Semantics: PropertyDefinitionEvaluation<br>
 * 14.4.13 Runtime Semantics: PropertyDefinitionEvaluation
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

        static final MethodDesc ScriptRuntime_EvaluatePropertyDefinitionAsync = MethodDesc.create(
                MethodType.Static, Types.ScriptRuntime, "EvaluatePropertyDefinitionAsync", Type
                        .getMethodType(Type.VOID_TYPE, Types.ScriptObject, Types.Object,
                                Types.RuntimeInfo$Function, Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_EvaluatePropertyDefinitionAsync_String = MethodDesc
                .create(MethodType.Static, Types.ScriptRuntime, "EvaluatePropertyDefinitionAsync",
                        Type.getMethodType(Type.VOID_TYPE, Types.ScriptObject, Types.String,
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

        static final MethodDesc ScriptRuntime_updateMethod = MethodDesc.create(MethodType.Static,
                Types.ScriptRuntime, "updateMethod", Type.getMethodType(Type.VOID_TYPE,
                        Types.ScriptObject, Types.Object, Types.FunctionObject));
    }

    public PropertyGenerator(CodeGenerator codegen) {
        super(codegen);
    }

    @Override
    protected ValType visit(Node node, ExpressionVisitor mv) {
        throw new IllegalStateException(String.format("node-class: %s", node.getClass()));
    }

    /**
     * 12.1.5.7 Runtime Semantics: Evaluation
     * <p>
     * ComputedPropertyName : [ AssignmentExpression ]
     */
    @Override
    public ValType visit(ComputedPropertyName node, ExpressionVisitor mv) {
        /* steps 1-3 */
        ValType type = expressionValue(node.getExpression(), mv);
        /* step 4 */
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
     * 14.3.9 Runtime Semantics: PropertyDefinitionEvaluation<br>
     * 14.4.13 Runtime Semantics: PropertyDefinitionEvaluation
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
            case AsyncFunction:
                mv.invoke(Methods.ScriptRuntime_EvaluatePropertyDefinitionAsync);
                break;
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
            case AsyncFunction:
                mv.invoke(Methods.ScriptRuntime_EvaluatePropertyDefinitionAsync_String);
                break;
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
     * 12.1.5.8 Runtime Semantics: PropertyDefinitionEvaluation
     * <p>
     * PropertyDefinition : IdentifierReference
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
     * 12.1.5.8 Runtime Semantics: PropertyDefinitionEvaluation
     * <p>
     * PropertyDefinition : PropertyName : AssignmentExpression
     */
    @Override
    public ValType visit(PropertyValueDefinition node, ExpressionVisitor mv) {
        // Runtime Semantics: Evaluation -> Property Definition Evaluation
        // stack: [<object>]

        PropertyName propertyName = node.getPropertyName();
        Expression propertyValue = node.getPropertyValue();

        boolean isAnonymousFunctionDefinition = IsAnonymousFunctionDefinition(propertyValue);
        boolean updateMethodFields = false;
        if (isAnonymousFunctionDefinition && !(propertyValue instanceof ArrowFunction)) {
            if (propertyValue instanceof ClassExpression) {
                ClassExpression classExpr = (ClassExpression) propertyValue;
                // FIXME: spec bug - not useful to update class constructor methods
                // Broken test case with this: `new ({c: class extends Object {}}).c`
                MethodDefinition constructorMethod = ConstructorMethod(classExpr);
                if (constructorMethod != null) {
                    updateMethodFields = constructorMethod.hasSuperReference();
                } else {
                    updateMethodFields = classExpr.getHeritage() != null;
                }
                // Disable for now:
                updateMethodFields = false;
            } else {
                assert propertyValue instanceof FunctionExpression
                        || propertyValue instanceof GeneratorExpression
                        || propertyValue instanceof AsyncFunctionExpression;
                updateMethodFields = ((FunctionNode) propertyValue).hasSuperReference();
            }
        }

        String propName = PropName(propertyName);
        if (propName == null) {
            assert propertyName instanceof ComputedPropertyName;
            ValType type = propertyName.accept(this, mv);

            if (updateMethodFields) {
                // stack: [<object>, pk] -> [<object>, pk, <object>, pk]
                mv.dup2();
            }
            // stack: [<object>, pk]
            expressionBoxedValue(propertyValue, mv);
            if (updateMethodFields) {
                // stack: [<object>, pk, <object>, pk, value] -> [<object>, pk, value]
                mv.dupX2();
                mv.invoke(Methods.ScriptRuntime_updateMethod);
            }
            // stack: [<object>, pk, value]
            if (isAnonymousFunctionDefinition) {
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
            if (updateMethodFields) {
                // stack: [<object>, pk] -> [<object>, pk, <object>, pk]
                mv.dup2();
            }
            expressionBoxedValue(propertyValue, mv);
            if (updateMethodFields) {
                // stack: [<object>, pk, <object>, pk, value] -> [<object>, pk, value]
                mv.dupX2();
                mv.invoke(Methods.ScriptRuntime_updateMethod);
            }
            if (isAnonymousFunctionDefinition) {
                SetFunctionName(propertyValue, propName, mv);
            }
            mv.loadExecutionContext();
            mv.invoke(Methods.ScriptRuntime_defineProperty_String);
        }

        return null;
    }
}
