/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.language;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Errors.newReferenceError;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.types.PropertyDescriptor.AccessorPropertyDescriptor;
import static com.github.anba.es6draft.runtime.types.builtins.FunctionObject.MakeClassConstructor;
import static com.github.anba.es6draft.runtime.types.builtins.FunctionObject.MakeConstructor;
import static com.github.anba.es6draft.runtime.types.builtins.FunctionObject.MakeMethod;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryConstructorFunction.ConstructorFunctionCreate;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.FunctionCreate;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject.ObjectCreate;

import java.util.HashSet;

import com.github.anba.es6draft.runtime.EnvironmentRecord;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.FunctionEnvironmentRecord;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.PrivateName;
import com.github.anba.es6draft.runtime.types.Property;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.FunctionObject;
import com.github.anba.es6draft.runtime.types.builtins.FunctionObject.ConstructorKind;
import com.github.anba.es6draft.runtime.types.builtins.FunctionObject.FunctionKind;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryConstructorFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * 
 */
public final class ClassOperations {
    private ClassOperations() {
    }

    /**
     * 12.3.5 The super Keyword
     * <p>
     * 12.3.5.1 Runtime Semantics: Evaluation
     * <p>
     * SuperCall : {@code super} Arguments
     * 
     * @param cx
     *            the execution context
     * @return the NewTarget constructor object
     */
    public static Constructor GetNewTargetOrThrow(ExecutionContext cx) {
        /* step 1 */
        Constructor newTarget = cx.getNewTarget();
        /* step 2 */
        if (newTarget == null) {
            throw newReferenceError(cx, Messages.Key.MissingNewTarget);
        }
        return newTarget;
    }

    /**
     * 12.3.5 The super Keyword
     * <p>
     * 12.3.5.1 Runtime Semantics: Evaluation
     * <p>
     * SuperCall : {@code super} Arguments
     * 
     * @param result
     *            the new {@code this} binding value
     * @param cx
     *            the execution context
     */
    public static void BindThisValue(ScriptObject result, ExecutionContext cx) {
        /* step 7 */
        EnvironmentRecord thisEnvironment = cx.getThisEnvironment();
        assert thisEnvironment instanceof FunctionEnvironmentRecord;
        /* step 8 */
        ((FunctionEnvironmentRecord) thisEnvironment).bindThisValue(cx, result);
    }

    /**
     * 12.3.5 The super Keyword
     * <p>
     * 12.3.5.2 Runtime Semantics: GetSuperConstructor ( )
     * 
     * @param cx
     *            the execution context
     * @return the super reference
     */
    public static Constructor GetSuperConstructor(ExecutionContext cx) {
        /* step 1 */
        EnvironmentRecord envRec = cx.getThisEnvironment();
        /* step 2 */
        assert envRec instanceof FunctionEnvironmentRecord;
        FunctionEnvironmentRecord fEnvRec = (FunctionEnvironmentRecord) envRec;
        /* step 3 */
        FunctionObject activeFunction = fEnvRec.getFunctionObject();
        /* step 4 */
        ScriptObject superConstructor = activeFunction.getPrototypeOf(cx);
        /* step 5 */
        if (!IsConstructor(superConstructor)) {
            throw newTypeError(cx, Messages.Key.NotConstructor);
        }
        /* step 6 */
        return (Constructor) superConstructor;
    }

    /**
     * 14.5 Class Definitions
     * <p>
     * 14.5.14 Runtime Semantics: ClassDefinitionEvaluation
     * 
     * @param cx
     *            the execution context
     * @return the tuple (protoParent, constructorParent)
     */
    public static ScriptObject[] getDefaultClassProto(ExecutionContext cx) {
        // step 5
        ScriptObject protoParent = cx.getIntrinsic(Intrinsics.ObjectPrototype);
        ScriptObject constructorParent = cx.getIntrinsic(Intrinsics.FunctionPrototype);
        return new ScriptObject[] { protoParent, constructorParent };
    }

    /**
     * 14.5 Class Definitions
     * <p>
     * 14.5.14 Runtime Semantics: ClassDefinitionEvaluation
     * 
     * @param cx
     *            the execution context
     * @return the tuple (protoParent, constructorParent)
     */
    public static ScriptObject[] getClassProto(ExecutionContext cx) {
        // step 6
        ScriptObject protoParent = null;
        ScriptObject constructorParent = cx.getIntrinsic(Intrinsics.FunctionPrototype);
        return new ScriptObject[] { protoParent, constructorParent };
    }

    /**
     * 14.5 Class Definitions
     * <p>
     * 14.5.14 Runtime Semantics: ClassDefinitionEvaluation
     * 
     * @param superClass
     *            the super class object
     * @param cx
     *            the execution context
     * @return the tuple (protoParent, constructorParent)
     */
    public static ScriptObject[] getClassProto(Object superClass, ExecutionContext cx) {
        ScriptObject protoParent;
        ScriptObject constructorParent;
        // step 6
        if (Type.isNull(superClass)) {
            protoParent = null;
            constructorParent = cx.getIntrinsic(Intrinsics.FunctionPrototype);
        } else if (!IsConstructor(superClass)) {
            throw newTypeError(cx, Messages.Key.NotConstructor);
        } else {
            Constructor superClassObj = (Constructor) superClass;
            Object p = Get(cx, superClassObj, "prototype");
            if (!Type.isObjectOrNull(p)) {
                throw newTypeError(cx, Messages.Key.NotObjectOrNull);
            }
            protoParent = Type.objectValueOrNull(p);
            constructorParent = superClassObj;
        }
        return new ScriptObject[] { protoParent, constructorParent };
    }

    /**
     * 14.5 Class Definitions
     * <p>
     * 14.5.14 Runtime Semantics: ClassDefinitionEvaluation
     * 
     * @param protoParent
     *            the parent prototype object
     * @param cx
     *            the execution context
     * @return the new prototype object
     */
    public static OrdinaryObject createProto(ScriptObject protoParent, ExecutionContext cx) {
        // step 7
        return ObjectCreate(cx, protoParent);
    }

    /**
     * 14.3 Method Definitions, 14.5 Class Definitions
     * <p>
     * 14.3.8 Runtime Semantics: DefineMethod<br>
     * 14.5.14 Runtime Semantics: ClassDefinitionEvaluation
     * 
     * @param constructorParent
     *            the constructor prototype
     * @param proto
     *            the class prototype
     * @param fd
     *            the function runtime info object
     * @param isDerived
     *            {@code true} if evaluating the constructor of a derived class
     * @param cx
     *            the execution context
     * @return the new function instance
     */
    public static OrdinaryConstructorFunction EvaluateConstructorMethod(ScriptObject constructorParent,
            OrdinaryObject proto, RuntimeInfo.Function fd, boolean isDerived, ExecutionContext cx) {
        // ClassDefinitionEvaluation - steps 12-14 (call DefineMethod)
        LexicalEnvironment<?> scope = cx.getLexicalEnvironment();
        ConstructorKind constructorKind = isDerived ? ConstructorKind.Derived : ConstructorKind.Base;
        OrdinaryConstructorFunction constructor = ConstructorFunctionCreate(cx, FunctionKind.ClassConstructor,
                constructorKind, fd, scope, constructorParent);
        MakeMethod(constructor, proto);

        // ClassDefinitionEvaluation - step 15 (not applicable, cf. ConstructorFunctionCreate)

        // ClassDefinitionEvaluation - step 16
        MakeConstructor(constructor, false, proto);

        // ClassDefinitionEvaluation - step 17
        MakeClassConstructor(constructor);

        // ClassDefinitionEvaluation - step 18
        proto.defineOwnProperty(cx, "constructor", new PropertyDescriptor(constructor, true, false, true));

        return constructor;
    }

    /**
     * 14.3 Method Definitions
     * <p>
     * 14.3.8 Runtime Semantics: PropertyDefinitionEvaluation
     * <ul>
     * <li>MethodDefinition: PropertyName ( StrictFormalParameters ) { FunctionBody }
     * <li>GeneratorMethod: * PropertyName ( StrictFormalParameters ) { FunctionBody }
     * <li>AsyncMethod: async [no LineTerminator here] PropertyName ( UniqueFormalParameters ) { AsyncFunctionBody }
     * <li>AsyncGeneratorMethod: async [no LT here] * PropertyName ( UniqueFormalParameters ) { AsyncGeneratorBody }
     * </ul>
     * 
     * @param object
     *            the script object
     * @param propKey
     *            the property key
     * @param method
     *            the method object
     * @param cx
     *            the execution context
     */
    public static void defineMethod(OrdinaryObject object, Object propKey, FunctionObject method, ExecutionContext cx) {
        /* step 4 */
        PropertyDescriptor desc = new PropertyDescriptor(method, true, false, true);
        /* step 5 */
        DefinePropertyOrThrow(cx, object, propKey, desc);
    }

    /**
     * 14.3 Method Definitions
     * <p>
     * 14.3.8 Runtime Semantics: PropertyDefinitionEvaluation
     * <ul>
     * <li>MethodDefinition: get PropertyName ( ) { FunctionBody }
     * </ul>
     * 
     * @param object
     *            the script object
     * @param propKey
     *            the property key
     * @param method
     *            the method object
     * @param cx
     *            the execution context
     */
    public static void defineGetter(OrdinaryObject object, Object propKey, FunctionObject method, ExecutionContext cx) {
        /* step 4 */
        PropertyDescriptor desc = AccessorPropertyDescriptor(method, null, false, true);
        /* step 5 */
        DefinePropertyOrThrow(cx, object, propKey, desc);
    }

    /**
     * 14.3 Method Definitions
     * <p>
     * 14.3.8 Runtime Semantics: PropertyDefinitionEvaluation
     * <ul>
     * <li>MethodDefinition: set PropertyName ( PropertySetParameterList ) { FunctionBody }
     * </ul>
     * 
     * @param object
     *            the script object
     * @param propKey
     *            the property key
     * @param method
     *            the method object
     * @param cx
     *            the execution context
     */
    public static void defineSetter(OrdinaryObject object, Object propKey, FunctionObject method, ExecutionContext cx) {
        /* step 4 */
        PropertyDescriptor desc = AccessorPropertyDescriptor(null, method, false, true);
        /* step 5 */
        DefinePropertyOrThrow(cx, object, propKey, desc);
    }

    /**
     * 14.3 Method Definitions
     * <p>
     * 14.3.8 Runtime Semantics: PropertyDefinitionEvaluation
     * <ul>
     * <li>MethodDefinition: PropertyName ( StrictFormalParameters ) { FunctionBody }
     * <li>GeneratorMethod: * PropertyName ( StrictFormalParameters ) { FunctionBody }
     * <li>AsyncMethod: async [no LineTerminator here] PropertyName ( UniqueFormalParameters ) { AsyncFunctionBody }
     * <li>AsyncGeneratorMethod: async [no LT here] * PropertyName ( UniqueFormalParameters ) { AsyncGeneratorBody }
     * </ul>
     * 
     * @param object
     *            the script object
     * @param propKey
     *            the property key
     * @param method
     *            the method object
     * @param cx
     *            the execution context
     */
    public static void defineMethod(OrdinaryObject object, PrivateName propKey, FunctionObject method,
            ExecutionContext cx) {
        assert object.get(propKey) == null;
        object.define(propKey, new Property(method, false, false, false));
    }

    /**
     * 14.3 Method Definitions
     * <p>
     * 14.3.8 Runtime Semantics: PropertyDefinitionEvaluation
     * <ul>
     * <li>MethodDefinition: get PropertyName ( ) { FunctionBody }
     * </ul>
     * 
     * @param object
     *            the script object
     * @param propKey
     *            the property key
     * @param method
     *            the method object
     * @param cx
     *            the execution context
     */
    public static void defineGetter(OrdinaryObject object, PrivateName propKey, FunctionObject method,
            ExecutionContext cx) {
        Property property = object.get(propKey);
        if (property == null) {
            object.define(propKey, new Property(method, null, false, false));
        } else {
            addGetter(property, method);
        }
    }

    /**
     * 14.3 Method Definitions
     * <p>
     * 14.3.8 Runtime Semantics: PropertyDefinitionEvaluation
     * <ul>
     * <li>MethodDefinition: set PropertyName ( PropertySetParameterList ) { FunctionBody }
     * </ul>
     * 
     * @param object
     *            the script object
     * @param propKey
     *            the property key
     * @param method
     *            the method object
     * @param cx
     *            the execution context
     */
    public static void defineSetter(OrdinaryObject object, PrivateName propKey, FunctionObject method,
            ExecutionContext cx) {
        Property property = object.get(propKey);
        if (property == null) {
            object.define(propKey, new Property(null, method, false, false));
        } else {
            addSetter(property, method);
        }
    }

    private static void addGetter(Property property, Callable getter) {
        assert property != null && property.isAccessorDescriptor() && property.getGetter() == null;
        PropertyDescriptor desc = new PropertyDescriptor();
        desc.setGetter(getter);
        property.apply(desc);
    }

    private static void addSetter(Property property, Callable setter) {
        assert property != null && property.isAccessorDescriptor() && property.getSetter() == null;
        PropertyDescriptor desc = new PropertyDescriptor();
        desc.setSetter(setter);
        property.apply(desc);
    }

    private static final PrivateName CLASS_FIELDS = new PrivateName("class-fields");
    private static final PrivateName INSTANCE_METHODS = new PrivateName("instance-methods");

    /**
     * Runtime Semantics: ClassFieldDefinitionEvaluation
     * 
     * @param homeObject
     *            the home object
     * @param fd
     *            the function runtime info object
     * @param fieldNames
     *            the field names
     * @param cx
     *            the execution context
     * @return the class field initializer
     */
    public static OrdinaryFunction CreateStaticClassFieldInitializer(ScriptObject homeObject, RuntimeInfo.Function fd,
            Object[] fieldNames, ExecutionContext cx) {
        /* step 3.a */
        LexicalEnvironment<?> lex = cx.getLexicalEnvironment();
        /* steps 3.b-c */
        OrdinaryFunction initializer = FunctionCreate(cx, FunctionKind.Method, fd, lex);
        /* step 3.d */
        MakeMethod(initializer, homeObject);

        assert fieldNames != null && fieldNames.length > 0 : "zero fields should be omitted";
        initializer.define(CLASS_FIELDS, new Property(fieldNames, false, false, false));

        return initializer;
    }

    /**
     * Runtime Semantics: ClassFieldDefinitionEvaluation
     * 
     * @param homeObject
     *            the home object
     * @param fd
     *            the function runtime info object
     * @param fieldNames
     *            the field names
     * @param instanceMethods
     *            the instance methods
     * @param cx
     *            the execution context
     * @return the class field initializer
     */
    public static OrdinaryFunction CreateClassFieldInitializer(ScriptObject homeObject, RuntimeInfo.Function fd,
            Object[] fieldNames, InstanceMethod[] instanceMethods, ExecutionContext cx) {
        /* step 3.a */
        LexicalEnvironment<?> lex = cx.getLexicalEnvironment();
        /* steps 3.b-c */
        OrdinaryFunction initializer = FunctionCreate(cx, FunctionKind.Method, fd, lex);
        /* step 3.d */
        MakeMethod(initializer, homeObject);

        assert fieldNames == null || fieldNames.length > 0 : "zero fields should be omitted";
        initializer.define(CLASS_FIELDS, new Property(fieldNames, false, false, false));

        // Stash the instance methods into the class field initializer for now, probably need to revisit when updating
        // to the newer decorators proposal.
        assert instanceMethods == null || instanceMethods.length > 0 : "zero methods should be omitted";
        initializer.define(INSTANCE_METHODS, new Property(instanceMethods, false, false, false));

        return initializer;
    }

    /**
     * PrivateFieldDefine (P, O, desc)
     * 
     * @param cx
     *            the execution context
     * @param object
     *            the script object
     * @param name
     *            the private name
     * @param desc
     *            the property descriptor
     */
    private static void privateFieldDefine(ExecutionContext cx, ScriptObject object, PrivateName name, Property desc) {
        /* steps 1-3 (not applicable) */
        /* step 4 */
        Property entry = object.get(name);
        /* step 5 */
        if (entry != null) {
            throw newTypeError(cx, Messages.Key.PrivateFieldPresent, name.toString());
        }
        /* step 6 */
        object.define(name, desc);
    }

    private static <T> T[] arrayValue(Property desc, Class<T[]> clazz) {
        assert desc != null && desc.isDataDescriptor();
        Object value = desc.getValue();
        assert clazz.isInstance(value);
        return clazz.cast(value);
    }

    public static Object[] GetClassFields(FunctionObject function) {
        return arrayValue(function.get(CLASS_FIELDS), Object[].class);
    }

    public static void DefineField(Object fieldName, Object initValue, ExecutionContext cx) {
        EnvironmentRecord envRec = cx.getThisEnvironment();
        assert envRec instanceof FunctionEnvironmentRecord;
        FunctionEnvironmentRecord fEnvRec = (FunctionEnvironmentRecord) envRec;
        assert fEnvRec.getThisBindingStatus() == FunctionEnvironmentRecord.ThisBindingStatus.Initialized;
        assert fEnvRec.getThisValue() instanceof ScriptObject;
        ScriptObject receiver = (ScriptObject) fEnvRec.getThisValue();

        if (fieldName instanceof PrivateName) {
            PrivateName name = (PrivateName) fieldName;
            Property desc = new Property(initValue, true, false, false);
            privateFieldDefine(cx, receiver, name, desc);
        } else {
            assert IsPropertyKey(fieldName);
            CreateDataPropertyOrThrow(cx, receiver, fieldName, initValue);
        }
    }

    private static final PrivateName CLASS_INSTANCE_INITIALIZER = new PrivateName("class-instance-initializer");

    public static void setInstanceFieldsInitializer(OrdinaryConstructorFunction f,
            OrdinaryFunction instanceFieldsInitializer) {
        f.define(CLASS_INSTANCE_INITIALIZER, new Property(instanceFieldsInitializer, false, false, false));
    }

    private static void initializeInstanceMethods(ScriptObject thisValue, InstanceMethod[] instanceMethods,
            ExecutionContext cx) {
        HashSet<PrivateName> accessors = new HashSet<>();
        for (InstanceMethod instanceMethod : instanceMethods) {
            PrivateName name = instanceMethod.name;
            FunctionObject method = instanceMethod.method;
            switch (instanceMethod.kind) {
            case Method: {
                Property desc = new Property(method, false, false, false);
                privateFieldDefine(cx, thisValue, name, desc);
                break;
            }
            case Getter:
                if (accessors.add(name)) {
                    Property desc = new Property(method, null, false, false);
                    privateFieldDefine(cx, thisValue, name, desc);
                } else {
                    addGetter(thisValue.get(name), method);
                }
                break;
            case Setter:
                if (accessors.add(name)) {
                    Property desc = new Property(null, method, false, false);
                    privateFieldDefine(cx, thisValue, name, desc);
                } else {
                    addSetter(thisValue.get(name), method);
                }
                break;
            default:
                throw new AssertionError();
            }
        }
    }

    public static void InitializeInstanceFields(ScriptObject thisValue, OrdinaryConstructorFunction constructor,
            ExecutionContext cx) {
        Property initializerDesc = constructor.get(CLASS_INSTANCE_INITIALIZER);
        if (initializerDesc != null) {
            assert initializerDesc.isDataDescriptor();
            Object initializer = initializerDesc.getValue();
            assert initializer instanceof OrdinaryFunction;
            OrdinaryFunction initializerFn = (OrdinaryFunction) initializer;
            Property instanceMethods = initializerFn.get(INSTANCE_METHODS);
            assert instanceMethods != null && instanceMethods.isDataDescriptor();
            if (instanceMethods.getValue() != null) {
                initializeInstanceMethods(thisValue, arrayValue(instanceMethods, InstanceMethod[].class), cx);
            }

            // Call the instance fields initializer.
            Property classFields = initializerFn.get(CLASS_FIELDS);
            assert classFields != null && classFields.isDataDescriptor();
            if (classFields.getValue() != null) {
                initializerFn.call(cx, thisValue);
            }
        }
    }

    public static void InitializeInstanceFields(ScriptObject thisValue, ExecutionContext cx) {
        EnvironmentRecord envRec = cx.getThisEnvironment();
        assert envRec instanceof FunctionEnvironmentRecord;
        FunctionEnvironmentRecord fEnvRec = (FunctionEnvironmentRecord) envRec;
        FunctionObject activeFunction = fEnvRec.getFunctionObject();
        assert activeFunction instanceof OrdinaryConstructorFunction;
        OrdinaryConstructorFunction constructor = (OrdinaryConstructorFunction) activeFunction;

        InitializeInstanceFields(thisValue, constructor, cx);
    }

    public enum InstanceMethodKind {
        Method, Getter, Setter
    }

    public static InstanceMethod newInstanceMethod(PrivateName name, FunctionObject method, InstanceMethodKind kind) {
        return new InstanceMethod(name, method, kind);
    }

    public static final class InstanceMethod {
        final PrivateName name;
        final FunctionObject method;
        final InstanceMethodKind kind;

        InstanceMethod(PrivateName name, FunctionObject method, InstanceMethodKind kind) {
            this.name = name;
            this.method = method;
            this.kind = kind;
        }
    }
}
