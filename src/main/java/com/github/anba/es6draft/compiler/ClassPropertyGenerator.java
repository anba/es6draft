/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static com.github.anba.es6draft.semantics.StaticSemantics.ClassProperties;
import static com.github.anba.es6draft.semantics.StaticSemantics.DecoratedMethods;
import static com.github.anba.es6draft.semantics.StaticSemantics.PropName;

import java.util.List;
import java.util.function.Predicate;

import com.github.anba.es6draft.ast.*;
import com.github.anba.es6draft.ast.scope.Name;
import com.github.anba.es6draft.ast.synthetic.MethodDefinitionsMethod;
import com.github.anba.es6draft.compiler.CodeVisitor.OutlinedCall;
import com.github.anba.es6draft.compiler.StatementGenerator.Completion;
import com.github.anba.es6draft.compiler.assembler.Code.MethodCode;
import com.github.anba.es6draft.compiler.assembler.FieldName;
import com.github.anba.es6draft.compiler.assembler.MethodName;
import com.github.anba.es6draft.compiler.assembler.MethodTypeDescriptor;
import com.github.anba.es6draft.compiler.assembler.MutableValue;
import com.github.anba.es6draft.compiler.assembler.Type;
import com.github.anba.es6draft.compiler.assembler.Value;
import com.github.anba.es6draft.compiler.assembler.Variable;
import com.github.anba.es6draft.runtime.DeclarativeEnvironmentRecord;
import com.github.anba.es6draft.runtime.language.ClassOperations.InstanceMethod;
import com.github.anba.es6draft.runtime.types.PrivateName;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryConstructorFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>14 ECMAScript Language: Functions and Classes</h1>
 * <ul>
 * <li>14.5 Class Definitions
 * </ul>
 */
final class ClassPropertyGenerator extends DefaultCodeGenerator<Void> {
    private static final class Fields {
        static final FieldName InstanceMethodKind_Method = FieldName.findStatic(
                Types.ClassOperations$InstanceMethodKind, "Method", Types.ClassOperations$InstanceMethodKind);

        static final FieldName InstanceMethodKind_Getter = FieldName.findStatic(
                Types.ClassOperations$InstanceMethodKind, "Getter", Types.ClassOperations$InstanceMethodKind);

        static final FieldName InstanceMethodKind_Setter = FieldName.findStatic(
                Types.ClassOperations$InstanceMethodKind, "Setter", Types.ClassOperations$InstanceMethodKind);
    }

    private static final class Methods {
        // class: PrivateName
        static final MethodName PrivateName_new = MethodName.findConstructor(Types.PrivateName,
                Type.methodType(Type.VOID_TYPE, Types.String));

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

        // class: ClassOperations
        static final MethodName ClassOperations_defineMethod = MethodName.findStatic(Types.ClassOperations,
                "defineMethod", Type.methodType(Type.VOID_TYPE, Types.OrdinaryObject, Types.Object,
                        Types.FunctionObject, Types.ExecutionContext));

        static final MethodName ClassOperations_defineGetter = MethodName.findStatic(Types.ClassOperations,
                "defineGetter", Type.methodType(Type.VOID_TYPE, Types.OrdinaryObject, Types.Object,
                        Types.FunctionObject, Types.ExecutionContext));

        static final MethodName ClassOperations_defineSetter = MethodName.findStatic(Types.ClassOperations,
                "defineSetter", Type.methodType(Type.VOID_TYPE, Types.OrdinaryObject, Types.Object,
                        Types.FunctionObject, Types.ExecutionContext));

        static final MethodName ClassOperations_definePrivateMethod = MethodName.findStatic(Types.ClassOperations,
                "defineMethod", Type.methodType(Type.VOID_TYPE, Types.OrdinaryObject, Types.PrivateName,
                        Types.FunctionObject, Types.ExecutionContext));

        static final MethodName ClassOperations_definePrivateGetter = MethodName.findStatic(Types.ClassOperations,
                "defineGetter", Type.methodType(Type.VOID_TYPE, Types.OrdinaryObject, Types.PrivateName,
                        Types.FunctionObject, Types.ExecutionContext));

        static final MethodName ClassOperations_definePrivateSetter = MethodName.findStatic(Types.ClassOperations,
                "defineSetter", Type.methodType(Type.VOID_TYPE, Types.OrdinaryObject, Types.PrivateName,
                        Types.FunctionObject, Types.ExecutionContext));

        static final MethodName ClassOperations_newInstanceMethod = MethodName.findStatic(Types.ClassOperations,
                "newInstanceMethod", Type.methodType(Types.ClassOperations$InstanceMethod, Types.PrivateName,
                        Types.FunctionObject, Types.ClassOperations$InstanceMethodKind));
    }

    private final ClassDefinition classDefinition;
    private final Variable<OrdinaryConstructorFunction> constructor;
    private final Variable<OrdinaryObject> prototype;
    private final StoreToArray<Object> staticFields;
    private final StoreToArray<Object> instanceFields;
    private final StoreToArray<InstanceMethod> instanceMethods;
    private final StoreToArray<Object> decorators;

    private ClassPropertyGenerator(CodeGenerator codegen, ClassDefinition classDefinition,
            Variable<OrdinaryConstructorFunction> constructor, Variable<OrdinaryObject> prototype,
            StoreToArray<Object> staticFields, StoreToArray<Object> instanceFields,
            StoreToArray<InstanceMethod> instanceMethods, StoreToArray<Object> decorators) {
        super(codegen);
        this.classDefinition = classDefinition;
        this.constructor = constructor;
        this.prototype = prototype;
        this.staticFields = staticFields;
        this.instanceFields = instanceFields;
        this.instanceMethods = instanceMethods;
        this.decorators = decorators;
    }

    static final class Result {
        final Variable<Object[]> staticClassField;
        final Variable<Object[]> instanceClassField;
        final Variable<InstanceMethod[]> instanceClassMethods;
        final Variable<Object[]> methodDecorators;

        Result(Variable<Object[]> staticClassField, Variable<Object[]> instanceClassField,
                Variable<InstanceMethod[]> instanceClassMethods, Variable<Object[]> methodDecorators) {
            this.staticClassField = staticClassField;
            this.instanceClassField = instanceClassField;
            this.instanceClassMethods = instanceClassMethods;
            this.methodDecorators = methodDecorators;
        }
    }

    private static <T> Predicate<T> not(Predicate<T> pred) {
        return pred.negate();
    }

    private static int countStaticFields(List<PropertyDefinition> list) {
        return (int) ClassProperties(list).filter(ClassFieldDefinition.class::isInstance)
                .map(ClassFieldDefinition.class::cast).filter(ClassFieldDefinition::isStatic).count();
    }

    private static int countInstanceFields(List<PropertyDefinition> list) {
        return (int) ClassProperties(list).filter(ClassFieldDefinition.class::isInstance)
                .map(ClassFieldDefinition.class::cast).filter(not(ClassFieldDefinition::isStatic)).count();
    }

    private static int countInstanceMethods(List<PropertyDefinition> list) {
        return (int) ClassProperties(list).filter(MethodDefinition.class::isInstance).map(MethodDefinition.class::cast)
                .filter(not(MethodDefinition::isStatic)
                        .and(m -> m.getClassElementName() instanceof PrivateNameProperty))
                .count();
    }

    static Result ClassPropertyEvaluation(CodeGenerator codegen, ClassDefinition def,
            Variable<OrdinaryConstructorFunction> constructor, Variable<OrdinaryObject> prototype, CodeVisitor mv) {
        List<PropertyDefinition> properties = def.getProperties();
        int staticFieldsLength = countStaticFields(properties);
        int instanceFieldsLength = countInstanceFields(properties);
        int instanceMethodsLength = countInstanceMethods(properties);

        List<MethodDefinition> decoratedMethods = DecoratedMethods(properties);
        int decoratorsLength = 0;
        if (!decoratedMethods.isEmpty()) {
            int decoratorsCount = decoratedMethods.stream().mapToInt(m -> m.getDecorators().size()).sum();
            decoratorsLength = decoratorsCount + decoratedMethods.size();
        }

        StoreToArray<Object> staticFields = StoreToArray.create("staticFields", staticFieldsLength, Object[].class, mv);
        StoreToArray<Object> instanceFields = StoreToArray.create("instanceFields", instanceFieldsLength,
                Object[].class, mv);
        StoreToArray<InstanceMethod> instanceMethods = StoreToArray.create("instanceMethods", instanceMethodsLength,
                InstanceMethod[].class, mv);
        StoreToArray<Object> decorators = StoreToArray.create("decorators", decoratorsLength, Object[].class, mv);

        ClassPropertyEvaluation(codegen, def, properties, constructor, prototype, staticFields, instanceFields,
                instanceMethods, decorators, mv);

        return new Result(staticFields.array, instanceFields.array, instanceMethods.array, decorators.array);
    }

    private static void ClassPropertyEvaluation(CodeGenerator codegen, ClassDefinition def,
            List<PropertyDefinition> properties, Variable<OrdinaryConstructorFunction> constructor,
            Variable<OrdinaryObject> prototype, StoreToArray<Object> staticFieldNames,
            StoreToArray<Object> instanceFieldNames, StoreToArray<InstanceMethod> instanceMethods,
            StoreToArray<Object> decorators, CodeVisitor mv) {
        ClassPropertyGenerator classgen = new ClassPropertyGenerator(codegen, def, constructor, prototype,
                staticFieldNames, instanceFieldNames, instanceMethods, decorators);
        for (PropertyDefinition property : properties) {
            property.accept(classgen, mv);
        }
    }

    @Override
    protected Void visit(Node node, CodeVisitor mv) {
        throw new IllegalStateException(String.format("node-class: %s", node.getClass()));
    }

    private void InitializeOrGetPrivateName(PrivateNameProperty privateName, boolean isAlreadyDefined,
            MutableValue<PrivateName> privateNameVar, CodeVisitor mv) {
        // TODO: The spec uses a separate lexical environment for private names.
        Name name = privateName.getName();
        Value<DeclarativeEnvironmentRecord> scopeEnvRec = getLexicalEnvironmentRecord(
                Types.DeclarativeEnvironmentRecord, mv);
        BindingOp<DeclarativeEnvironmentRecord> op = BindingOp.of(scopeEnvRec, name);

        if (isAlreadyDefined) {
            op.getBindingValue(scopeEnvRec, name, true, mv);
            mv.checkcast(Types.PrivateName);
            mv.store(privateNameVar);
        } else {
            mv.anew(Methods.PrivateName_new, mv.vconst(name.getIdentifier()));
            mv.store(privateNameVar);
            op.initializeBinding(scopeEnvRec, name, privateNameVar, mv);
        }
    }

    private void InitializePrivateName(StoreToArray<Object> target, PrivateNameProperty privateName, CodeVisitor mv) {
        // TODO: The spec uses a separate lexical environment for private names.
        Name name = privateName.getName();
        Value<DeclarativeEnvironmentRecord> scopeEnvRec = getLexicalEnvironmentRecord(
                Types.DeclarativeEnvironmentRecord, mv);
        BindingOp<DeclarativeEnvironmentRecord> op = BindingOp.of(scopeEnvRec, name);

        MutableValue<Object> arrayElement = target.element(Object.class, mv);
        arrayElement.store(mv, __ -> {
            mv.anew(Methods.PrivateName_new, mv.vconst(name.getIdentifier()));
        });
        op.initializeBinding(scopeEnvRec, name, arrayElement, mv);
    }

    private void InitializeFieldName(StoreToArray<Object> target, PropertyName propertyName, CodeVisitor mv) {
        // TODO: Actually we only need to compute fields with computed property names here.
        target.store(__ -> {
            String propName = PropName(propertyName);
            if (propName == null) {
                assert propertyName instanceof ComputedPropertyName;
                propertyName.accept(this, mv);
            } else {
                mv.aconst(propName);
            }
        }, mv);
    }

    /**
     * 12.2.5.7 Runtime Semantics: Evaluation
     * <p>
     * ComputedPropertyName : [ AssignmentExpression ]
     */
    @Override
    public Void visit(ComputedPropertyName node, CodeVisitor mv) {
        /* steps 1-3 */
        ValType type = expression(node.getExpression(), mv);
        /* step 4 */
        ToPropertyKey(type, mv);
        return null;
    }

    private boolean isPrivateNameInitialized(MethodDefinition node) {
        assert node.getClassElementName() instanceof PrivateNameProperty;
        MethodDefinition.MethodType otherType;
        switch (node.getType()) {
        case Getter:
            otherType = MethodDefinition.MethodType.Setter;
            break;
        case Setter:
            otherType = MethodDefinition.MethodType.Getter;
            break;
        default:
            return false;
        }
        PrivateNameProperty nodeName = (PrivateNameProperty) node.getClassElementName();
        for (MethodDefinition method : classDefinition.getMethods()) {
            if (method == node) {
                break;
            }
            ClassElementName name = method.getClassElementName();
            if (method.getType() == otherType && name instanceof PrivateNameProperty
                    && ((PrivateNameProperty) name).getName().equals(nodeName.getName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Void visit(MethodDefinition node, CodeVisitor mv) {
        // Synthetic methods are handled elsewhere.
        if (node.isSynthetic()) {
            return null;
        }

        if (node.isClassConstructor() || node.isCallConstructor()) {
            if (!node.getDecorators().isEmpty()) {
                assert node.isClassConstructor() : "call constructors cannot have decorators";

                // Evaluate constructor decorators in source order.
                for (Expression decorator : node.getDecorators()) {
                    decorators.store(__ -> {
                        expressionBoxed(decorator, mv);
                        CheckCallable(decorator, mv);
                    }, mv);
                }
                decorators.store(mv.vconst("constructor"), mv);
            }
            return null;
        }

        MethodName method = mv.compile(node, codegen::methodDefinition);
        ClassElementName classElementName = node.getClassElementName();
        boolean isPrivateName = classElementName instanceof PrivateNameProperty;
        assert !isPrivateName || node.getDecorators().isEmpty();

        for (Expression decorator : node.getDecorators()) {
            decorators.store(__ -> {
                expressionBoxed(decorator, mv);
                CheckCallable(decorator, mv);
            }, mv);
        }

        mv.load(node.isStatic() ? constructor : prototype);

        if (!isPrivateName) {
            // stack: [<object>] -> [<object>, propertyName, <object>, propertyName]
            String propName = PropName(classElementName.toPropertyName());
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
        } else {
            mv.enterVariableScope();
            Variable<PrivateName> privateName = mv.newVariable("privateName", PrivateName.class);

            // stack: [<object>] -> [<object>]
            InitializeOrGetPrivateName((PrivateNameProperty) classElementName, isPrivateNameInitialized(node),
                    privateName, mv);

            // stack: [<object>] -> [<object>, privateName]
            mv.load(privateName);

            if (node.isStatic()) {
                // stack: [<object>, privateName] -> [<object>, privateName, <object>, privateName]
                mv.dup2();
            } else {
                // stack: [<object>, privateName] -> [privateName, <object>, privateName]
                mv.dupX1();
            }
            mv.exitVariableScope();
        }

        mv.invoke(method);
        mv.loadExecutionContext();
        mv.lineInfo(node);

        // stack: [..., <object>, propertyName, rti, cx] -> [..., method]
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

        if (!isPrivateName) {
            // stack: [<object>, propertyName, method] -> []
            mv.loadExecutionContext();
            switch (node.getType()) {
            case AsyncFunction:
            case AsyncGenerator:
            case Function:
            case Generator:
                mv.invoke(Methods.ClassOperations_defineMethod);
                break;
            case Getter:
                mv.invoke(Methods.ClassOperations_defineGetter);
                break;
            case Setter:
                mv.invoke(Methods.ClassOperations_defineSetter);
                break;
            case CallConstructor:
            case ClassConstructor:
            default:
                throw new AssertionError("invalid method type");
            }
        } else {
            if (node.isStatic()) {
                // Static class methods can be defined directly.

                // stack: [<object>, privateName, method] -> []
                mv.loadExecutionContext();
                switch (node.getType()) {
                case AsyncFunction:
                case AsyncGenerator:
                case Function:
                case Generator:
                    mv.invoke(Methods.ClassOperations_definePrivateMethod);
                    break;
                case Getter:
                    mv.invoke(Methods.ClassOperations_definePrivateGetter);
                    break;
                case Setter:
                    mv.invoke(Methods.ClassOperations_definePrivateSetter);
                    break;
                case CallConstructor:
                case ClassConstructor:
                default:
                    throw new AssertionError("invalid method type");
                }
            } else {
                // Instance class methods are defined whenever a new class is created.

                // stack: [privateName, method] -> []
                switch (node.getType()) {
                case AsyncFunction:
                case AsyncGenerator:
                case Function:
                case Generator:
                    mv.get(Fields.InstanceMethodKind_Method);
                    break;
                case Getter:
                    mv.get(Fields.InstanceMethodKind_Getter);
                    break;
                case Setter:
                    mv.get(Fields.InstanceMethodKind_Setter);
                    break;
                case CallConstructor:
                case ClassConstructor:
                default:
                    throw new AssertionError("invalid method type");
                }
                mv.invoke(Methods.ClassOperations_newInstanceMethod);
                instanceMethods.element(InstanceMethod.class, mv).store(mv);
            }
        }

        return null;
    }

    @Override
    public Void visit(ClassFieldDefinition node, CodeVisitor mv) {
        StoreToArray<Object> target = node.isStatic() ? staticFields : instanceFields;
        ClassElementName classElementName = node.getClassElementName();
        if (classElementName instanceof PrivateNameProperty) {
            InitializePrivateName(target, (PrivateNameProperty) classElementName, mv);
        } else {
            InitializeFieldName(target, node.getPropertyName(), mv);
        }
        return null;
    }

    @Override
    public Void visit(MethodDefinitionsMethod node, CodeVisitor mv) {
        // Sync array indices on recompilation.
        if (mv.isCompiled(node)) {
            if (!staticFields.isEmpty()) {
                staticFields.skip(countStaticFields(node.getProperties()));
            }
            if (!instanceFields.isEmpty()) {
                instanceFields.skip(countInstanceFields(node.getProperties()));
            }
            if (!instanceMethods.isEmpty()) {
                instanceMethods.skip(countInstanceMethods(node.getProperties()));
            }
            if (!decorators.isEmpty()) {
                DecoratedMethods(node.getProperties(), method -> decorators.skip(method.getDecorators().size()));
            }
        }

        OutlinedCall call = mv.compile(node, this::methodDefinitions);
        mv.invoke(call, false, constructor, prototype, staticFields, instanceFields, instanceMethods, decorators);
        return null;
    }

    private OutlinedCall methodDefinitions(MethodDefinitionsMethod node, CodeVisitor mv) {
        MethodTypeDescriptor methodDescriptor = MethodDefinitionsCodeVisitor.methodDescriptor(mv);
        MethodCode method = codegen.method(mv, "mdef", methodDescriptor);
        return outlined(new MethodDefinitionsCodeVisitor(node, method, mv), body -> {
            Variable<OrdinaryConstructorFunction> function = body.getFunctionParameter();
            Variable<OrdinaryObject> proto = body.getPrototypeParameter();
            StoreToArray<Object> staticFields = this.staticFields.from(body.getStaticFieldsParameter());
            StoreToArray<Object> instanceFields = this.instanceFields.from(body.getInstanceFieldsParameter());
            StoreToArray<InstanceMethod> instanceMethods = this.instanceMethods
                    .from(body.getInstanceMethodsParameter());
            StoreToArray<Object> decorators = this.decorators.from(body.getDecoratorsParameter());

            ClassPropertyEvaluation(codegen, classDefinition, node.getProperties(), function, proto, staticFields,
                    instanceFields, instanceMethods, decorators, body);

            return Completion.Normal;
        });
    }

    private static final class MethodDefinitionsCodeVisitor extends OutlinedCodeVisitor {
        MethodDefinitionsCodeVisitor(MethodDefinitionsMethod node, MethodCode method, CodeVisitor parent) {
            super(node, method, parent);
        }

        @Override
        public void begin() {
            super.begin();
            setParameterName("F", parameter(0), Types.OrdinaryConstructorFunction);
            setParameterName("proto", parameter(1), Types.OrdinaryObject);
            setParameterName("staticFields", parameter(2), Types.Object_);
            setParameterName("instanceFields", parameter(3), Types.Object_);
            setParameterName("instanceMethods", parameter(4), Types.ClassOperations$InstanceMethod_);
            setParameterName("decorators", parameter(5), Types.Object_);
        }

        Variable<OrdinaryConstructorFunction> getFunctionParameter() {
            return getParameter(parameter(0), OrdinaryConstructorFunction.class);
        }

        Variable<OrdinaryObject> getPrototypeParameter() {
            return getParameter(parameter(1), OrdinaryObject.class);
        }

        Variable<Object[]> getStaticFieldsParameter() {
            return getParameter(parameter(2), Object[].class);
        }

        Variable<Object[]> getInstanceFieldsParameter() {
            return getParameter(parameter(3), Object[].class);
        }

        public Variable<InstanceMethod[]> getInstanceMethodsParameter() {
            return getParameter(parameter(4), InstanceMethod[].class);
        }

        Variable<Object[]> getDecoratorsParameter() {
            return getParameter(parameter(5), Object[].class);
        }

        static MethodTypeDescriptor methodDescriptor(CodeVisitor mv) {
            MethodTypeDescriptor methodDescriptor = OutlinedCodeVisitor.outlinedMethodDescriptor(mv);
            return methodDescriptor.appendParameterTypes(Types.OrdinaryConstructorFunction, Types.OrdinaryObject,
                    Types.Object_, Types.Object_, Types.ClassOperations$InstanceMethod_, Types.Object_);
        }
    }
}
