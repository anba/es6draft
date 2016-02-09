/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static com.github.anba.es6draft.semantics.StaticSemantics.IsAnonymousFunctionDefinition;
import static com.github.anba.es6draft.semantics.StaticSemantics.PropName;

import java.util.ArrayList;

import com.github.anba.es6draft.ast.ComputedPropertyName;
import com.github.anba.es6draft.ast.Expression;
import com.github.anba.es6draft.ast.IdentifierReference;
import com.github.anba.es6draft.ast.MethodDefinition;
import com.github.anba.es6draft.ast.Node;
import com.github.anba.es6draft.ast.PropertyName;
import com.github.anba.es6draft.ast.PropertyNameDefinition;
import com.github.anba.es6draft.ast.PropertyValueDefinition;
import com.github.anba.es6draft.ast.SpreadProperty;
import com.github.anba.es6draft.ast.synthetic.PropertyDefinitionsMethod;
import com.github.anba.es6draft.compiler.assembler.MethodName;
import com.github.anba.es6draft.compiler.assembler.Type;
import com.github.anba.es6draft.compiler.assembler.Value;
import com.github.anba.es6draft.compiler.assembler.Variable;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.IndexedMap;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * 12.2.5.8 Runtime Semantics: PropertyDefinitionEvaluation<br>
 * 14.3.9 Runtime Semantics: PropertyDefinitionEvaluation<br>
 * 14.4.13 Runtime Semantics: PropertyDefinitionEvaluation
 */
final class PropertyGenerator extends DefaultCodeGenerator<DefaultCodeGenerator.ValType> {
    private static final class Methods {
        // class: ScriptRuntime
        static final MethodName ScriptRuntime_EvaluatePropertyDefinition = MethodName.findStatic(
                Types.ScriptRuntime, "EvaluatePropertyDefinition", Type.methodType(Type.VOID_TYPE,
                        Types.OrdinaryObject, Types.Object, Type.BOOLEAN_TYPE,
                        Types.RuntimeInfo$Function, Types.ExecutionContext));

        static final MethodName ScriptRuntime_EvaluatePropertyDefinition_String = MethodName
                .findStatic(Types.ScriptRuntime, "EvaluatePropertyDefinition", Type.methodType(
                        Type.VOID_TYPE, Types.OrdinaryObject, Types.String, Type.BOOLEAN_TYPE,
                        Types.RuntimeInfo$Function, Types.ExecutionContext));

        static final MethodName ScriptRuntime_EvaluatePropertyDefinitionAsync = MethodName
                .findStatic(Types.ScriptRuntime, "EvaluatePropertyDefinitionAsync", Type
                        .methodType(Type.VOID_TYPE, Types.OrdinaryObject, Types.Object,
                                Type.BOOLEAN_TYPE, Types.RuntimeInfo$Function,
                                Types.ExecutionContext));

        static final MethodName ScriptRuntime_EvaluatePropertyDefinitionAsync_String = MethodName
                .findStatic(Types.ScriptRuntime, "EvaluatePropertyDefinitionAsync", Type
                        .methodType(Type.VOID_TYPE, Types.OrdinaryObject, Types.String,
                                Type.BOOLEAN_TYPE, Types.RuntimeInfo$Function,
                                Types.ExecutionContext));

        static final MethodName ScriptRuntime_EvaluatePropertyDefinitionAsyncGenerator = MethodName
                .findStatic(Types.ScriptRuntime, "EvaluatePropertyDefinitionAsyncGenerator", Type
                        .methodType(Type.VOID_TYPE, Types.OrdinaryObject, Types.Object,
                                Type.BOOLEAN_TYPE, Types.RuntimeInfo$Function,
                                Types.ExecutionContext));

        static final MethodName ScriptRuntime_EvaluatePropertyDefinitionAsyncGenerator_String = MethodName
                .findStatic(Types.ScriptRuntime, "EvaluatePropertyDefinitionAsyncGenerator", Type
                        .methodType(Type.VOID_TYPE, Types.OrdinaryObject, Types.String,
                                Type.BOOLEAN_TYPE, Types.RuntimeInfo$Function,
                                Types.ExecutionContext));

        static final MethodName ScriptRuntime_EvaluatePropertyDefinitionConstructorGenerator = MethodName
                .findStatic(Types.ScriptRuntime, "EvaluatePropertyDefinitionConstructorGenerator", Type
                        .methodType(Type.VOID_TYPE, Types.OrdinaryObject, Types.Object,
                                Type.BOOLEAN_TYPE, Types.RuntimeInfo$Function,
                                Types.ExecutionContext));

        static final MethodName ScriptRuntime_EvaluatePropertyDefinitionConstructorGenerator_String = MethodName
                .findStatic(Types.ScriptRuntime, "EvaluatePropertyDefinitionConstructorGenerator", Type
                        .methodType(Type.VOID_TYPE, Types.OrdinaryObject, Types.String,
                                Type.BOOLEAN_TYPE, Types.RuntimeInfo$Function,
                                Types.ExecutionContext));

        static final MethodName ScriptRuntime_EvaluatePropertyDefinitionGenerator = MethodName.findStatic(
                Types.ScriptRuntime, "EvaluatePropertyDefinitionGenerator",
                Type.methodType(Type.VOID_TYPE, Types.OrdinaryObject, Types.Object, Type.BOOLEAN_TYPE,
                        Types.RuntimeInfo$Function, Types.ExecutionContext));

        static final MethodName ScriptRuntime_EvaluatePropertyDefinitionGenerator_String = MethodName.findStatic(
                Types.ScriptRuntime, "EvaluatePropertyDefinitionGenerator",
                Type.methodType(Type.VOID_TYPE, Types.OrdinaryObject, Types.String, Type.BOOLEAN_TYPE,
                        Types.RuntimeInfo$Function, Types.ExecutionContext));

        static final MethodName ScriptRuntime_EvaluatePropertyDefinitionGetter = MethodName
                .findStatic(Types.ScriptRuntime, "EvaluatePropertyDefinitionGetter", Type
                        .methodType(Type.VOID_TYPE, Types.OrdinaryObject, Types.Object,
                                Type.BOOLEAN_TYPE, Types.RuntimeInfo$Function,
                                Types.ExecutionContext));

        static final MethodName ScriptRuntime_EvaluatePropertyDefinitionGetter_String = MethodName
                .findStatic(Types.ScriptRuntime, "EvaluatePropertyDefinitionGetter", Type
                        .methodType(Type.VOID_TYPE, Types.OrdinaryObject, Types.String,
                                Type.BOOLEAN_TYPE, Types.RuntimeInfo$Function,
                                Types.ExecutionContext));

        static final MethodName ScriptRuntime_EvaluatePropertyDefinitionSetter = MethodName
                .findStatic(Types.ScriptRuntime, "EvaluatePropertyDefinitionSetter", Type
                        .methodType(Type.VOID_TYPE, Types.OrdinaryObject, Types.Object,
                                Type.BOOLEAN_TYPE, Types.RuntimeInfo$Function,
                                Types.ExecutionContext));

        static final MethodName ScriptRuntime_EvaluatePropertyDefinitionSetter_String = MethodName
                .findStatic(Types.ScriptRuntime, "EvaluatePropertyDefinitionSetter", Type
                        .methodType(Type.VOID_TYPE, Types.OrdinaryObject, Types.String,
                                Type.BOOLEAN_TYPE, Types.RuntimeInfo$Function,
                                Types.ExecutionContext));

        static final MethodName ScriptRuntime_defineProperty = MethodName.findStatic(
                Types.ScriptRuntime, "defineProperty", Type.methodType(Type.VOID_TYPE,
                        Types.OrdinaryObject, Types.Object, Types.Object, Types.ExecutionContext));

        static final MethodName ScriptRuntime_defineProperty_String = MethodName.findStatic(
                Types.ScriptRuntime, "defineProperty", Type.methodType(Type.VOID_TYPE,
                        Types.OrdinaryObject, Types.String, Types.Object, Types.ExecutionContext));

        static final MethodName ScriptRuntime_defineProperty_long = MethodName
                .findStatic(Types.ScriptRuntime, "defineProperty", Type.methodType(Type.VOID_TYPE,
                        Types.OrdinaryObject, Type.LONG_TYPE, Types.Object, Types.ExecutionContext));

        static final MethodName ScriptRuntime_defineProtoProperty = MethodName.findStatic(
                Types.ScriptRuntime, "defineProtoProperty", Type.methodType(Type.VOID_TYPE,
                        Types.OrdinaryObject, Types.Object, Types.ExecutionContext));

        static final MethodName ScriptRuntime_defineSpreadProperty = MethodName.findStatic(
                Types.ScriptRuntime, "defineSpreadProperty", Type.methodType(Type.VOID_TYPE,
                        Types.OrdinaryObject, Types.Object, Types.ExecutionContext));
    }

    private final Variable<ArrayList<Object>> decorators;

    public PropertyGenerator(CodeGenerator codegen) {
        super(codegen);
        this.decorators = null;
    }

    public PropertyGenerator(CodeGenerator codegen, Variable<ArrayList<Object>> decorators) {
        super(codegen);
        this.decorators = decorators;
    }

    private Value<ArrayList<Object>> decoratorsOrNull(CodeVisitor mv) {
        return this.decorators != null ? this.decorators : mv.anullValue();
    }

    @Override
    protected ValType visit(Node node, CodeVisitor mv) {
        throw new IllegalStateException(String.format("node-class: %s", node.getClass()));
    }

    /**
     * 12.2.5.7 Runtime Semantics: Evaluation
     * <p>
     * ComputedPropertyName : [ AssignmentExpression ]
     */
    @Override
    public ValType visit(ComputedPropertyName node, CodeVisitor mv) {
        /* steps 1-3 */
        ValType type = expression(node.getExpression(), mv);
        /* step 4 */
        return ToPropertyKey(type, mv);
    }

    @Override
    public ValType visit(PropertyDefinitionsMethod node, CodeVisitor mv) {
        MethodName method = codegen.compile(node, decorators != null, mv);
        boolean hasResume = node.hasResumePoint();

        mv.enterVariableScope();
        Variable<OrdinaryObject> object = mv.newVariable("object", OrdinaryObject.class);

        // stack: [<object>] -> []
        mv.store(object);

        // stack: [] -> []
        mv.lineInfo(0); // 0 = hint for stacktraces to omit this frame
        if (hasResume) {
            mv.callWithSuspend(method, object, decoratorsOrNull(mv));
        } else {
            mv.call(method, object, decoratorsOrNull(mv));
        }
        mv.exitVariableScope();

        return null;
    }

    /**
     * 14.3.9 Runtime Semantics: PropertyDefinitionEvaluation<br>
     * 14.4.13 Runtime Semantics: PropertyDefinitionEvaluation
     */
    @Override
    public ValType visit(MethodDefinition node, CodeVisitor mv) {
        MethodName method = codegen.compile(node);

        boolean hasDecorators = !node.getDecorators().isEmpty();
        if (hasDecorators) {
            evaluateDecorators(decorators, node.getDecorators(), mv);
        }

        // stack: [<object>] -> []
        String propName = PropName(node);
        if (propName == null) {
            assert node.getPropertyName() instanceof ComputedPropertyName;
            ValType propKey = node.getPropertyName().accept(this, mv);
            if (hasDecorators) {
                addDecoratorKey(decorators, propKey, mv);
            }
            mv.iconst(node.getAllocation() == MethodDefinition.MethodAllocation.Object);
            mv.invoke(method);
            mv.loadExecutionContext();
            mv.lineInfo(node);

            switch (node.getType()) {
            case AsyncFunction:
                mv.invoke(Methods.ScriptRuntime_EvaluatePropertyDefinitionAsync);
                break;
            case AsyncGenerator:
                mv.invoke(Methods.ScriptRuntime_EvaluatePropertyDefinitionAsyncGenerator);
                break;
            case Function:
                mv.invoke(Methods.ScriptRuntime_EvaluatePropertyDefinition);
                break;
            case ConstructorGenerator:
                mv.invoke(Methods.ScriptRuntime_EvaluatePropertyDefinitionConstructorGenerator);
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
            case BaseConstructor:
            case DerivedConstructor:
            case CallConstructor:
            default:
                throw new AssertionError("invalid method type");
            }
        } else {
            if (hasDecorators) {
                addDecoratorKey(decorators, propName, mv);
            }
            mv.aconst(propName);
            mv.iconst(node.getAllocation() == MethodDefinition.MethodAllocation.Object);
            mv.invoke(method);
            mv.loadExecutionContext();
            mv.lineInfo(node);

            switch (node.getType()) {
            case AsyncFunction:
                mv.invoke(Methods.ScriptRuntime_EvaluatePropertyDefinitionAsync_String);
                break;
            case AsyncGenerator:
                mv.invoke(Methods.ScriptRuntime_EvaluatePropertyDefinitionAsyncGenerator_String);
                break;
            case Function:
                mv.invoke(Methods.ScriptRuntime_EvaluatePropertyDefinition_String);
                break;
            case ConstructorGenerator:
                mv.invoke(Methods.ScriptRuntime_EvaluatePropertyDefinitionConstructorGenerator_String);
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
            case BaseConstructor:
            case DerivedConstructor:
            case CallConstructor:
            default:
                throw new AssertionError("invalid method type");
            }
        }

        return null;
    }

    /**
     * 12.2.5.8 Runtime Semantics: PropertyDefinitionEvaluation
     * <p>
     * PropertyDefinition : IdentifierReference
     */
    @Override
    public ValType visit(PropertyNameDefinition node, CodeVisitor mv) {
        IdentifierReference propertyName = node.getPropertyName();
        String propName = PropName(propertyName);
        assert propName != null;

        // stack: [<object>] -> []
        mv.aconst(propName);
        expressionBoxed(propertyName, mv);
        mv.loadExecutionContext();
        mv.lineInfo(node);
        mv.invoke(Methods.ScriptRuntime_defineProperty_String);

        return null;
    }

    /**
     * 12.2.5.8 Runtime Semantics: PropertyDefinitionEvaluation
     * <p>
     * PropertyDefinition : PropertyName : AssignmentExpression
     */
    @Override
    public ValType visit(PropertyValueDefinition node, CodeVisitor mv) {
        Expression propertyValue = node.getPropertyValue();
        PropertyName propertyName = node.getPropertyName();
        String propName = PropName(propertyName);
        long propIndex = propName != null ? IndexedMap.toIndex(propName) : -1;

        // stack: [<object>] -> []
        if (propName == null) {
            assert propertyName instanceof ComputedPropertyName;
            ValType type = propertyName.accept(this, mv);
            expressionBoxed(propertyValue, mv);
            if (IsAnonymousFunctionDefinition(propertyValue)) {
                SetFunctionName(propertyValue, type, mv);
            }
            mv.loadExecutionContext();
            mv.lineInfo(node);
            mv.invoke(Methods.ScriptRuntime_defineProperty);
        } else if ("__proto__".equals(propName) && codegen.isEnabled(CompatibilityOption.ProtoInitializer)) {
            expressionBoxed(propertyValue, mv);
            mv.loadExecutionContext();
            mv.lineInfo(node);
            mv.invoke(Methods.ScriptRuntime_defineProtoProperty);
        } else if (IndexedMap.isIndex(propIndex)) {
            mv.lconst(propIndex);
            expressionBoxed(propertyValue, mv);
            if (IsAnonymousFunctionDefinition(propertyValue)) {
                SetFunctionName(propertyValue, propName, mv);
            }
            mv.loadExecutionContext();
            mv.lineInfo(node);
            mv.invoke(Methods.ScriptRuntime_defineProperty_long);
        } else {
            mv.aconst(propName);
            expressionBoxed(propertyValue, mv);
            if (IsAnonymousFunctionDefinition(propertyValue)) {
                SetFunctionName(propertyValue, propName, mv);
            }
            mv.loadExecutionContext();
            mv.lineInfo(node);
            mv.invoke(Methods.ScriptRuntime_defineProperty_String);
        }

        return null;
    }

    @Override
    public ValType visit(SpreadProperty node, CodeVisitor mv) {
        // stack: [<object>] -> [<object>, value]
        expressionBoxed(node.getExpression(), mv);

        // stack: [<object>, value] -> []
        mv.loadExecutionContext();
        mv.lineInfo(node);
        mv.invoke(Methods.ScriptRuntime_defineSpreadProperty);

        return null;
    }
}
