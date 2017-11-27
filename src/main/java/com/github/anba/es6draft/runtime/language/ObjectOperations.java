/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.language;

import static com.github.anba.es6draft.runtime.AbstractOperations.CopyDataProperties;
import static com.github.anba.es6draft.runtime.AbstractOperations.CreateDataPropertyOrThrow;
import static com.github.anba.es6draft.runtime.AbstractOperations.DefinePropertyOrThrow;
import static com.github.anba.es6draft.runtime.types.PropertyDescriptor.AccessorPropertyDescriptor;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject.ObjectCreate;

import java.util.Collections;
import java.util.Set;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.FunctionObject;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * 
 */
public final class ObjectOperations {
    private ObjectOperations() {
    }

    /**
     * 12.2.6 Object Initializer
     * <p>
     * 12.2.6.8 Runtime Semantics: PropertyDefinitionEvaluation
     * 
     * @param object
     *            the script object
     * @param propertyName
     *            the property name
     * @param value
     *            the property value
     * @param cx
     *            the execution context
     */
    public static void defineProperty(OrdinaryObject object, Object propertyName, Object value, ExecutionContext cx) {
        CreateDataPropertyOrThrow(cx, object, propertyName, value);
    }

    /**
     * 12.2.6 Object Initializer
     * <p>
     * 12.2.6.8 Runtime Semantics: PropertyDefinitionEvaluation
     * 
     * @param object
     *            the script object
     * @param propertyName
     *            the property name
     * @param value
     *            the property value
     * @param cx
     *            the execution context
     */
    public static void defineProperty(OrdinaryObject object, String propertyName, Object value, ExecutionContext cx) {
        CreateDataPropertyOrThrow(cx, object, propertyName, value);
    }

    /**
     * 12.2.6 Object Initializer
     * <p>
     * 12.2.6.8 Runtime Semantics: PropertyDefinitionEvaluation
     * 
     * @param object
     *            the script object
     * @param propertyName
     *            the property name
     * @param value
     *            the property value
     * @param cx
     *            the execution context
     */
    public static void defineProperty(OrdinaryObject object, long propertyName, Object value, ExecutionContext cx) {
        CreateDataPropertyOrThrow(cx, object, propertyName, value);
    }

    /**
     * B.3.1 __proto___ Property Names in Object Initializers
     * 
     * @param object
     *            the object instance
     * @param value
     *            the new prototype
     * @param cx
     *            the execution context
     */
    public static void defineProtoProperty(OrdinaryObject object, Object value, ExecutionContext cx) {
        if (Type.isObjectOrNull(value)) {
            object.setPrototypeOf(cx, Type.objectValueOrNull(value));
        }
    }

    /**
     * Extension: Object Spread Initializer
     * <p>
     * Runtime Semantics: PropertyDefinitionEvaluation
     * <ul>
     * <li>PropertyDefinition : ... AssignmentExpression
     * </ul>
     * 
     * @param object
     *            the script object
     * @param value
     *            the spread value
     * @param cx
     *            the execution context
     */
    public static void defineSpreadProperty(OrdinaryObject object, Object value, ExecutionContext cx) {
        CopyDataProperties(cx, object, value, Collections.EMPTY_SET);
    }

    /**
     * Extension: Object Rest Destructuring
     * <ul>
     * <li>Runtime Semantics: DestructuringAssignmentEvaluation
     * <li>Runtime Semantics: BindingInitialization
     * </ul>
     * 
     * @param value
     *            the rest property
     * @param excludedNames
     *            the excluded property names
     * @param cx
     *            the execution context
     * @return the rest object
     */
    public static OrdinaryObject createRestObject(Object value, Set<?> excludedNames, ExecutionContext cx) {
        OrdinaryObject restObj = ObjectCreate(cx, Intrinsics.ObjectPrototype);
        return CopyDataProperties(cx, restObj, value, excludedNames);
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
        PropertyDescriptor desc = new PropertyDescriptor(method, true, true, true);
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
        PropertyDescriptor desc = AccessorPropertyDescriptor(method, null, true, true);
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
        PropertyDescriptor desc = AccessorPropertyDescriptor(null, method, true, true);
        /* step 5 */
        DefinePropertyOrThrow(cx, object, propKey, desc);
    }
}
