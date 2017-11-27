/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static com.github.anba.es6draft.semantics.StaticSemantics.DecoratedMethods;
import static com.github.anba.es6draft.semantics.StaticSemantics.IsAnonymousFunctionDefinition;
import static com.github.anba.es6draft.semantics.StaticSemantics.PropName;

import java.util.List;

import com.github.anba.es6draft.ast.*;
import com.github.anba.es6draft.ast.synthetic.PropertyDefinitionsMethod;
import com.github.anba.es6draft.compiler.CodeVisitor.OutlinedCall;
import com.github.anba.es6draft.compiler.StatementGenerator.Completion;
import com.github.anba.es6draft.compiler.assembler.Code.MethodCode;
import com.github.anba.es6draft.compiler.assembler.MethodName;
import com.github.anba.es6draft.compiler.assembler.MethodTypeDescriptor;
import com.github.anba.es6draft.compiler.assembler.Type;
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
        // class: FunctionOperations
        static final MethodName FunctionOperations_EvaluatePropertyDefinition = MethodName.findStatic(
                Types.FunctionOperations, "EvaluatePropertyDefinition", Type.methodType(Types.OrdinaryFunction,
                        Types.OrdinaryObject, Types.Object, Types.RuntimeInfo$Function, Types.ExecutionContext));

        static final MethodName FunctionOperations_EvaluatePropertyDefinitionAsync = MethodName.findStatic(
                Types.FunctionOperations, "EvaluatePropertyDefinitionAsync",
                Type.methodType(Types.OrdinaryAsyncFunction, Types.OrdinaryObject, Types.Object,
                        Types.RuntimeInfo$Function, Types.ExecutionContext));

        static final MethodName FunctionOperations_EvaluatePropertyDefinitionAsyncGenerator = MethodName.findStatic(
                Types.FunctionOperations, "EvaluatePropertyDefinitionAsyncGenerator",
                Type.methodType(Types.OrdinaryAsyncGenerator, Types.OrdinaryObject, Types.Object,
                        Types.RuntimeInfo$Function, Types.ExecutionContext));

        static final MethodName FunctionOperations_EvaluatePropertyDefinitionGenerator = MethodName.findStatic(
                Types.FunctionOperations, "EvaluatePropertyDefinitionGenerator",
                Type.methodType(Types.OrdinaryGenerator, Types.OrdinaryObject, Types.Object, Types.RuntimeInfo$Function,
                        Types.ExecutionContext));

        static final MethodName FunctionOperations_EvaluatePropertyDefinitionGetter = MethodName.findStatic(
                Types.FunctionOperations, "EvaluatePropertyDefinitionGetter", Type.methodType(Types.OrdinaryFunction,
                        Types.OrdinaryObject, Types.Object, Types.RuntimeInfo$Function, Types.ExecutionContext));

        static final MethodName FunctionOperations_EvaluatePropertyDefinitionSetter = MethodName.findStatic(
                Types.FunctionOperations, "EvaluatePropertyDefinitionSetter", Type.methodType(Types.OrdinaryFunction,
                        Types.OrdinaryObject, Types.Object, Types.RuntimeInfo$Function, Types.ExecutionContext));

        // class: ObjectOperations
        static final MethodName ObjectOperations_defineMethod = MethodName.findStatic(Types.ObjectOperations,
                "defineMethod", Type.methodType(Type.VOID_TYPE, Types.OrdinaryObject, Types.Object,
                        Types.FunctionObject, Types.ExecutionContext));

        static final MethodName ObjectOperations_defineGetter = MethodName.findStatic(Types.ObjectOperations,
                "defineGetter", Type.methodType(Type.VOID_TYPE, Types.OrdinaryObject, Types.Object,
                        Types.FunctionObject, Types.ExecutionContext));

        static final MethodName ObjectOperations_defineSetter = MethodName.findStatic(Types.ObjectOperations,
                "defineSetter", Type.methodType(Type.VOID_TYPE, Types.OrdinaryObject, Types.Object,
                        Types.FunctionObject, Types.ExecutionContext));

        static final MethodName ObjectOperations_defineProperty = MethodName.findStatic(Types.ObjectOperations,
                "defineProperty", Type.methodType(Type.VOID_TYPE, Types.OrdinaryObject, Types.Object, Types.Object,
                        Types.ExecutionContext));

        static final MethodName ObjectOperations_defineProperty_String = MethodName.findStatic(Types.ObjectOperations,
                "defineProperty", Type.methodType(Type.VOID_TYPE, Types.OrdinaryObject, Types.String, Types.Object,
                        Types.ExecutionContext));

        static final MethodName ObjectOperations_defineProperty_long = MethodName.findStatic(Types.ObjectOperations,
                "defineProperty", Type.methodType(Type.VOID_TYPE, Types.OrdinaryObject, Type.LONG_TYPE, Types.Object,
                        Types.ExecutionContext));

        static final MethodName ObjectOperations_defineProtoProperty = MethodName.findStatic(Types.ObjectOperations,
                "defineProtoProperty",
                Type.methodType(Type.VOID_TYPE, Types.OrdinaryObject, Types.Object, Types.ExecutionContext));

        static final MethodName ObjectOperations_defineSpreadProperty = MethodName.findStatic(Types.ObjectOperations,
                "defineSpreadProperty",
                Type.methodType(Type.VOID_TYPE, Types.OrdinaryObject, Types.Object, Types.ExecutionContext));
    }

    private final StoreToArray<Object> decorators;

    public PropertyGenerator(CodeGenerator codegen) {
        this(codegen, StoreToArray.empty());
    }

    public PropertyGenerator(CodeGenerator codegen, StoreToArray<Object> decorators) {
        super(codegen);
        this.decorators = decorators;
    }

    static void PropertyEvaluation(CodeGenerator codegen, List<PropertyDefinition> properties, CodeVisitor mv) {
        PropertyGenerator propgen = codegen.propertyGenerator();
        for (PropertyDefinition property : properties) {
            mv.dup();
            property.accept(propgen, mv);
        }
    }

    static void PropertyEvaluation(CodeGenerator codegen, List<PropertyDefinition> properties,
            Variable<OrdinaryObject> object, StoreToArray<Object> decorators, CodeVisitor mv) {
        PropertyGenerator propgen = new PropertyGenerator(codegen, decorators);
        for (PropertyDefinition property : properties) {
            mv.load(object);
            property.accept(propgen, mv);
        }
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

    /**
     * 14.3.9 Runtime Semantics: PropertyDefinitionEvaluation<br>
     * 14.4.13 Runtime Semantics: PropertyDefinitionEvaluation
     */
    @Override
    public ValType visit(MethodDefinition node, CodeVisitor mv) {
        MethodName method = mv.compile(node, codegen::methodDefinition);

        for (Expression decorator : node.getDecorators()) {
            decorators.store(__ -> {
                expressionBoxed(decorator, mv);
                CheckCallable(decorator, mv);
            }, mv);
        }

        // stack: [<object>] -> [<object>, propertyName]
        String propName = PropName(node);
        if (propName == null) {
            assert node.getPropertyName() instanceof ComputedPropertyName;
            node.getPropertyName().accept(this, mv);
            if (!node.getDecorators().isEmpty()) {
                mv.dup();
                mv.store(decorators.element(Object.class, mv));
            }
        } else {
            if (!node.getDecorators().isEmpty()) {
                decorators.store(mv.vconst(propName), mv);
            }
            mv.aconst(propName);
        }

        mv.dup2();
        mv.invoke(method);
        mv.loadExecutionContext();
        mv.lineInfo(node);

        switch (node.getType()) {
        case AsyncFunction:
            mv.invoke(Methods.FunctionOperations_EvaluatePropertyDefinitionAsync);
            break;
        case AsyncGenerator:
            mv.invoke(Methods.FunctionOperations_EvaluatePropertyDefinitionAsyncGenerator);
            break;
        case Function:
            mv.invoke(Methods.FunctionOperations_EvaluatePropertyDefinition);
            break;
        case Generator:
            mv.invoke(Methods.FunctionOperations_EvaluatePropertyDefinitionGenerator);
            break;
        case Getter:
            mv.invoke(Methods.FunctionOperations_EvaluatePropertyDefinitionGetter);
            break;
        case Setter:
            mv.invoke(Methods.FunctionOperations_EvaluatePropertyDefinitionSetter);
            break;
        case CallConstructor:
        case ClassConstructor:
        default:
            throw new AssertionError("invalid method type");
        }

        mv.loadExecutionContext();
        switch (node.getType()) {
        case AsyncFunction:
        case AsyncGenerator:
        case Function:
        case Generator:
            mv.invoke(Methods.ObjectOperations_defineMethod);
            break;
        case Getter:
            mv.invoke(Methods.ObjectOperations_defineGetter);
            break;
        case Setter:
            mv.invoke(Methods.ObjectOperations_defineSetter);
            break;
        case CallConstructor:
        case ClassConstructor:
        default:
            throw new AssertionError("invalid method type");
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
        mv.invoke(Methods.ObjectOperations_defineProperty_String);

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
            mv.invoke(Methods.ObjectOperations_defineProperty);
        } else if ("__proto__".equals(propName) && codegen.isEnabled(CompatibilityOption.ProtoInitializer)) {
            expressionBoxed(propertyValue, mv);
            mv.loadExecutionContext();
            mv.lineInfo(node);
            mv.invoke(Methods.ObjectOperations_defineProtoProperty);
        } else if (IndexedMap.isIndex(propIndex)) {
            mv.lconst(propIndex);
            expressionBoxed(propertyValue, mv);
            if (IsAnonymousFunctionDefinition(propertyValue)) {
                SetFunctionName(propertyValue, propName, mv);
            }
            mv.loadExecutionContext();
            mv.lineInfo(node);
            mv.invoke(Methods.ObjectOperations_defineProperty_long);
        } else {
            mv.aconst(propName);
            expressionBoxed(propertyValue, mv);
            if (IsAnonymousFunctionDefinition(propertyValue)) {
                SetFunctionName(propertyValue, propName, mv);
            }
            mv.loadExecutionContext();
            mv.lineInfo(node);
            mv.invoke(Methods.ObjectOperations_defineProperty_String);
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
        mv.invoke(Methods.ObjectOperations_defineSpreadProperty);

        return null;
    }

    @Override
    public ValType visit(PropertyDefinitionsMethod node, CodeVisitor mv) {
        // Sync array indices on recompilation.
        if (mv.isCompiled(node)) {
            if (decorators.array != null) {
                DecoratedMethods(node.getProperties(), method -> decorators.skip(method.getDecorators().size()));
            }
        }

        // stack: [<object>] -> []
        mv.enterVariableScope();
        Variable<OrdinaryObject> object = mv.newVariable("object", OrdinaryObject.class);
        mv.store(object);

        OutlinedCall call = mv.compile(node, this::propertyDefinitions);
        mv.invoke(call, false, object, decorators);

        mv.exitVariableScope();

        return null;
    }

    private OutlinedCall propertyDefinitions(PropertyDefinitionsMethod node, CodeVisitor mv) {
        MethodTypeDescriptor methodDescriptor = PropertyDefinitionsCodeVisitor.methodDescriptor(mv);
        MethodCode method = codegen.method(mv, "propdef", methodDescriptor);
        return outlined(new PropertyDefinitionsCodeVisitor(node, method, mv), body -> {
            Variable<OrdinaryObject> object = body.getObjectParameter();
            StoreToArray<Object> decorators = this.decorators.from(body.getDecoratorsParameter());

            PropertyEvaluation(codegen, node.getProperties(), object, decorators, body);

            return Completion.Normal;
        });
    }

    private static final class PropertyDefinitionsCodeVisitor extends OutlinedCodeVisitor {
        PropertyDefinitionsCodeVisitor(PropertyDefinitionsMethod node, MethodCode method, CodeVisitor parent) {
            super(node, method, parent);
        }

        @Override
        public void begin() {
            super.begin();
            setParameterName("object", parameter(0), Types.OrdinaryObject);
            setParameterName("decorators", parameter(1), Types.Object_);
        }

        Variable<OrdinaryObject> getObjectParameter() {
            return getParameter(parameter(0), OrdinaryObject.class);
        }

        Variable<Object[]> getDecoratorsParameter() {
            return getParameter(parameter(1), Object[].class);
        }

        static MethodTypeDescriptor methodDescriptor(CodeVisitor mv) {
            MethodTypeDescriptor methodDescriptor = OutlinedCodeVisitor.outlinedMethodDescriptor(mv);
            return methodDescriptor.appendParameterTypes(Types.OrdinaryObject, Types.Object_);
        }
    }
}
