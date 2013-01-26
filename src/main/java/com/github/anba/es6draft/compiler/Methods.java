/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import org.objectweb.asm.Type;

import com.github.anba.es6draft.compiler.InstructionVisitor.MethodDesc;

/**
 *
 */
final class Methods {
    private Methods() {
    }

    /* ----------------------------------------------------------------------------------------- */

    // class: AbstractOperations

    // Get()
    static final MethodDesc AbstractOperations_Get = MethodDesc.create(Types.AbstractOperations,
            "Get", Type.getMethodType(Types.Object, Types.Scriptable, Types.String));

    // Put()
    static final MethodDesc AbstractOperations_Put = MethodDesc.create(Types.AbstractOperations,
            "Put", Type.getMethodType(Type.VOID_TYPE, Types.Realm, Types.Scriptable, Types.String,
                    Types.Object, Type.BOOLEAN_TYPE));

    // CreateArrayFromList()
    static final MethodDesc AbstractOperations_CreateArrayFromList = MethodDesc.create(
            Types.AbstractOperations, "CreateArrayFromList",
            Type.getMethodType(Types.Scriptable, Types.Realm, Types.List));

    // ToXXX()
    static final MethodDesc AbstractOperations_ToPrimitive = MethodDesc.create(
            Types.AbstractOperations, "ToPrimitive",
            Type.getMethodType(Types.Object, Types.Realm, Types.Object, Types._Type));

    static final MethodDesc AbstractOperations_ToBoolean = MethodDesc.create(
            Types.AbstractOperations, "ToBoolean",
            Type.getMethodType(Type.BOOLEAN_TYPE, Types.Object));

    static final MethodDesc AbstractOperations_ToBoolean_double = MethodDesc.create(
            Types.AbstractOperations, "ToBoolean",
            Type.getMethodType(Type.BOOLEAN_TYPE, Type.DOUBLE_TYPE));

    static final MethodDesc AbstractOperations_ToNumber = MethodDesc.create(
            Types.AbstractOperations, "ToNumber",
            Type.getMethodType(Type.DOUBLE_TYPE, Types.Realm, Types.Object));

    static final MethodDesc AbstractOperations_ToNumber_CharSequence = MethodDesc.create(
            Types.AbstractOperations, "ToNumber",
            Type.getMethodType(Type.DOUBLE_TYPE, Types.CharSequence));

    static final MethodDesc AbstractOperations_ToInteger = MethodDesc.create(
            Types.AbstractOperations, "ToInteger",
            Type.getMethodType(Type.DOUBLE_TYPE, Types.Realm, Types.Object));

    static final MethodDesc AbstractOperations_ToInt32 = MethodDesc.create(
            Types.AbstractOperations, "ToInt32",
            Type.getMethodType(Type.INT_TYPE, Types.Realm, Types.Object));

    static final MethodDesc AbstractOperations_ToInt32_double = MethodDesc.create(
            Types.AbstractOperations, "ToInt32",
            Type.getMethodType(Type.INT_TYPE, Type.DOUBLE_TYPE));

    static final MethodDesc AbstractOperations_ToUint32 = MethodDesc.create(
            Types.AbstractOperations, "ToUint32",
            Type.getMethodType(Type.LONG_TYPE, Types.Realm, Types.Object));

    static final MethodDesc AbstractOperations_ToUint32_double = MethodDesc.create(
            Types.AbstractOperations, "ToUint32",
            Type.getMethodType(Type.LONG_TYPE, Type.DOUBLE_TYPE));

    static final MethodDesc AbstractOperations_ToUint16 = MethodDesc.create(
            Types.AbstractOperations, "ToUint16",
            Type.getMethodType(Type.CHAR_TYPE, Types.Realm, Types.Object));

    static final MethodDesc AbstractOperations_ToString = MethodDesc.create(
            Types.AbstractOperations, "ToString",
            Type.getMethodType(Types.CharSequence, Types.Realm, Types.Object));

    static final MethodDesc AbstractOperations_ToString_double = MethodDesc.create(
            Types.AbstractOperations, "ToString",
            Type.getMethodType(Types.String, Type.DOUBLE_TYPE));

    static final MethodDesc AbstractOperations_ToObject = MethodDesc.create(
            Types.AbstractOperations, "ToObject",
            Type.getMethodType(Types.Scriptable, Types.Realm, Types.Object));

    /* ----------------------------------------------------------------------------------------- */

    // class: ArrayList

    static final MethodDesc ArrayList_init = MethodDesc.create(Types.ArrayList, "<init>",
            Type.getMethodType(Type.VOID_TYPE));

    /* ----------------------------------------------------------------------------------------- */

    // class: Boolean

    static final MethodDesc Boolean_toString = MethodDesc.create(Types.Boolean, "toString",
            Type.getMethodType(Types.String, Type.BOOLEAN_TYPE));

    /* ----------------------------------------------------------------------------------------- */

    // class: Callable

    static final MethodDesc Callable_call = MethodDesc.create(Types.Callable, "call",
            Type.getMethodType(Types.Object, Types.Object, Types.Object_));

    /* ----------------------------------------------------------------------------------------- */

    // class: CharSequence

    static final MethodDesc CharSequence_length = MethodDesc.create(Types.CharSequence, "length",
            Type.getMethodType(Type.INT_TYPE));

    /* ----------------------------------------------------------------------------------------- */

    // class: CompiledScript

    static final MethodDesc CompiledScript_Constructor = MethodDesc.create(Types.CompiledScript,
            "<init>", Type.getMethodType(Type.VOID_TYPE, Types.RuntimeInfo$ScriptBody));

    /* ----------------------------------------------------------------------------------------- */

    // class: EnvironmentRecord

    static final MethodDesc EnvironmentRecord_createImmutableBinding = MethodDesc.create(
            Types.EnvironmentRecord, "createImmutableBinding",
            Type.getMethodType(Type.VOID_TYPE, Types.String));

    static final MethodDesc EnvironmentRecord_createMutableBinding = MethodDesc.create(
            Types.EnvironmentRecord, "createMutableBinding",
            Type.getMethodType(Type.VOID_TYPE, Types.String, Type.BOOLEAN_TYPE));

    static final MethodDesc EnvironmentRecord_initializeBinding = MethodDesc.create(
            Types.EnvironmentRecord, "initializeBinding",
            Type.getMethodType(Type.VOID_TYPE, Types.String, Types.Object));

    /* ----------------------------------------------------------------------------------------- */

    // class: Eval

    static final MethodDesc Eval_directEval = MethodDesc.create(Types.Eval, "directEval", Type
            .getMethodType(Types.Object, Types.Object, Types.ExecutionContext, Type.BOOLEAN_TYPE,
                    Type.BOOLEAN_TYPE));

    /* ----------------------------------------------------------------------------------------- */

    // class: ExecutionContext

    // {get,push,pop,restore}LexicalEnvironment()
    static final MethodDesc ExecutionContext_getLexicalEnvironment = MethodDesc.create(
            Types.ExecutionContext, "getLexicalEnvironment",
            Type.getMethodType(Types.LexicalEnvironment));

    static final MethodDesc ExecutionContext_pushLexicalEnvironment = MethodDesc.create(
            Types.ExecutionContext, "pushLexicalEnvironment",
            Type.getMethodType(Type.VOID_TYPE, Types.LexicalEnvironment));

    static final MethodDesc ExecutionContext_popLexicalEnvironment = MethodDesc.create(
            Types.ExecutionContext, "popLexicalEnvironment", Type.getMethodType(Type.VOID_TYPE));

    static final MethodDesc ExecutionContext_restoreLexicalEnvironment = MethodDesc.create(
            Types.ExecutionContext, "restoreLexicalEnvironment",
            Type.getMethodType(Type.VOID_TYPE, Types.LexicalEnvironment));

    // getRealm()
    static final MethodDesc ExecutionContext_getRealm = MethodDesc.create(Types.ExecutionContext,
            "getRealm", Type.getMethodType(Types.Realm));

    // identifierResolution()
    static final MethodDesc ExecutionContext_identifierResolution = MethodDesc.create(
            Types.ExecutionContext, "identifierResolution",
            Type.getMethodType(Types.Reference, Types.String, Type.BOOLEAN_TYPE));

    static final MethodDesc ExecutionContext_strictIdentifierResolution = MethodDesc.create(
            Types.ExecutionContext, "strictIdentifierResolution",
            Type.getMethodType(Types.Reference, Types.String));

    static final MethodDesc ExecutionContext_nonstrictIdentifierResolution = MethodDesc.create(
            Types.ExecutionContext, "nonstrictIdentifierResolution",
            Type.getMethodType(Types.Reference, Types.String));

    // identifierValue()
    static final MethodDesc ExecutionContext_identifierValue = MethodDesc.create(
            Types.ExecutionContext, "identifierValue",
            Type.getMethodType(Types.Object, Types.String, Type.BOOLEAN_TYPE));

    static final MethodDesc ExecutionContext_strictIdentifierValue = MethodDesc.create(
            Types.ExecutionContext, "strictIdentifierValue",
            Type.getMethodType(Types.Object, Types.String));

    static final MethodDesc ExecutionContext_nonstrictIdentifierValue = MethodDesc.create(
            Types.ExecutionContext, "nonstrictIdentifierValue",
            Type.getMethodType(Types.Object, Types.String));

    // thisResolution()
    static final MethodDesc ExecutionContext_thisResolution = MethodDesc.create(
            Types.ExecutionContext, "thisResolution", Type.getMethodType(Types.Object));

    /* ----------------------------------------------------------------------------------------- */

    // class: OrdinaryObject

    static final MethodDesc ExoticArray_ArrayCreate = MethodDesc.create(Types.ExoticArray,
            "ArrayCreate", Type.getMethodType(Types.Scriptable, Types.Realm, Type.LONG_TYPE));

    /* ----------------------------------------------------------------------------------------- */

    // class: Iterator

    static final MethodDesc Iterator_hasNext = MethodDesc.create(Types.Iterator, "hasNext",
            Type.getMethodType(Type.BOOLEAN_TYPE));

    static final MethodDesc Iterator_next = MethodDesc.create(Types.Iterator, "next",
            Type.getMethodType(Types.Object));

    /* ----------------------------------------------------------------------------------------- */

    // class: LexicalEnvironment

    static final MethodDesc LexicalEnvironment_getEnvRec = MethodDesc.create(
            Types.LexicalEnvironment, "getEnvRec", Type.getMethodType(Types.EnvironmentRecord));

    // new{Declarative,Object}Environment()
    static final MethodDesc LexicalEnvironment_newDeclarativeEnvironment = MethodDesc.create(
            Types.LexicalEnvironment, "newDeclarativeEnvironment",
            Type.getMethodType(Types.LexicalEnvironment, Types.LexicalEnvironment));

    static final MethodDesc LexicalEnvironment_newObjectEnvironment = MethodDesc.create(
            Types.LexicalEnvironment, "newObjectEnvironment", Type.getMethodType(
                    Types.LexicalEnvironment, Types.Scriptable, Types.LexicalEnvironment,
                    Type.BOOLEAN_TYPE));

    /* ----------------------------------------------------------------------------------------- */

    // class: List

    static final MethodDesc List_add = MethodDesc.create(Types.List, "add",
            Type.getMethodType(Type.BOOLEAN_TYPE, Types.Object));

    /* ----------------------------------------------------------------------------------------- */

    // class: OrdinaryFunction

    static final MethodDesc OrdinaryFunction_InstantiateFunctionObject = MethodDesc.create(
            Types.OrdinaryFunction, "InstantiateFunctionObject", Type.getMethodType(Types.Function,
                    Types.Realm, Types.LexicalEnvironment, Types.RuntimeInfo$Function));

    /* ----------------------------------------------------------------------------------------- */

    // class: OrdinaryGenerator

    static final MethodDesc OrdinaryGenerator_InstantiateGeneratorObject = MethodDesc.create(
            Types.OrdinaryGenerator, "InstantiateGeneratorObject", Type.getMethodType(
                    Types.Generator, Types.Realm, Types.LexicalEnvironment,
                    Types.RuntimeInfo$Function));

    /* ----------------------------------------------------------------------------------------- */

    // class: OrdinaryObject

    static final MethodDesc OrdinaryObject_ObjectCreate = MethodDesc.create(Types.OrdinaryObject,
            "ObjectCreate", Type.getMethodType(Types.Scriptable, Types.Realm));

    /* ----------------------------------------------------------------------------------------- */

    // class: Reference

    static final MethodDesc Reference_GetValue = MethodDesc.create(Types.Reference, "GetValue",
            Type.getMethodType(Types.Object, Types.Object, Types.Realm));

    static final MethodDesc Reference_PutValue = MethodDesc.create(Types.Reference, "PutValue",
            Type.getMethodType(Type.VOID_TYPE, Types.Object, Types.Object, Types.Realm));

    static final MethodDesc Reference_GetValue_ = MethodDesc.create(Types.Reference, "GetValue",
            Type.getMethodType(Types.Object, Types.Realm));

    static final MethodDesc Reference_PutValue_ = MethodDesc.create(Types.Reference, "PutValue",
            Type.getMethodType(Type.VOID_TYPE, Types.Object, Types.Realm));

    /* ----------------------------------------------------------------------------------------- */

    // class: ScriptException

    static final MethodDesc ScriptException_getValue = MethodDesc.create(Types.ScriptException,
            "getValue", Type.getMethodType(Types.Object));

    /* ----------------------------------------------------------------------------------------- */

    // class: ScriptRuntime

    // add
    static final MethodDesc ScriptRuntime_add = MethodDesc.create(Types.ScriptRuntime, "add",
            Type.getMethodType(Types.Object, Types.Object, Types.Object, Types.Realm));

    static final MethodDesc ScriptRuntime_add_str = MethodDesc.create(Types.ScriptRuntime, "add",
            Type.getMethodType(Types.CharSequence, Types.CharSequence, Types.CharSequence));

    // delete
    static final MethodDesc ScriptRuntime_delete = MethodDesc.create(Types.ScriptRuntime, "delete",
            Type.getMethodType(Type.BOOLEAN_TYPE, Types.Object, Types.Realm));

    // throw
    static final MethodDesc ScriptRuntime_throw = MethodDesc.create(Types.ScriptRuntime, "_throw",
            Type.getMethodType(Types.ScriptException, Types.Object));

    // in
    static final MethodDesc ScriptRuntime_in = MethodDesc.create(Types.ScriptRuntime, "in",
            Type.getMethodType(Type.BOOLEAN_TYPE, Types.Object, Types.Object, Types.Realm));

    // typeof
    static final MethodDesc ScriptRuntime_typeof = MethodDesc.create(Types.ScriptRuntime, "typeof",
            Type.getMethodType(Types.String, Types.Object, Types.Realm));

    // yield / delegatedYield
    static final MethodDesc ScriptRuntime_yield = MethodDesc.create(Types.ScriptRuntime, "yield",
            Type.getMethodType(Types.Object, Types.Object, Types.ExecutionContext));

    static final MethodDesc ScriptRuntime_delegatedYield = MethodDesc.create(Types.ScriptRuntime,
            "delegatedYield",
            Type.getMethodType(Types.Object, Types.Object, Types.ExecutionContext));

    // equalityComparison
    static final MethodDesc ScriptRuntime_equalityComparison = MethodDesc.create(
            Types.ScriptRuntime, "equalityComparison",
            Type.getMethodType(Type.BOOLEAN_TYPE, Types.Object, Types.Object, Types.Realm));

    // instanceOfOperator
    static final MethodDesc ScriptRuntime_instanceOfOperator = MethodDesc.create(
            Types.ScriptRuntime, "instanceOfOperator",
            Type.getMethodType(Type.BOOLEAN_TYPE, Types.Object, Types.Object, Types.Realm));

    // relationalComparison
    static final MethodDesc ScriptRuntime_relationalComparison = MethodDesc.create(
            Types.ScriptRuntime, "relationalComparison", Type.getMethodType(Type.INT_TYPE,
                    Types.Object, Types.Object, Type.BOOLEAN_TYPE, Types.Realm));

    // strictEqualityComparison()
    static final MethodDesc ScriptRuntime_strictEqualityComparison = MethodDesc.create(
            Types.ScriptRuntime, "strictEqualityComparison",
            Type.getMethodType(Type.BOOLEAN_TYPE, Types.Object, Types.Object));

    // ArrayAccumulationSpreadElement()
    static final MethodDesc ScriptRuntime_ArrayAccumulationSpreadElement = MethodDesc.create(
            Types.ScriptRuntime, "ArrayAccumulationSpreadElement", Type.getMethodType(
                    Type.INT_TYPE, Types.Scriptable, Type.INT_TYPE, Types.Object, Types.Realm));

    // CheckCallable()
    static final MethodDesc ScriptRuntime_CheckCallable = MethodDesc.create(Types.ScriptRuntime,
            "CheckCallable", Type.getMethodType(Types.Callable, Types.Object, Types.Realm));

    // CreateDefaultConstructor
    static final MethodDesc ScriptRuntime_CreateDefaultConstructor = MethodDesc.create(
            Types.ScriptRuntime, "CreateDefaultConstructor",
            Type.getMethodType(Types.RuntimeInfo$Function));

    static final MethodDesc ScriptRuntime_createRestArray = MethodDesc.create(Types.ScriptRuntime,
            "createRestArray",
            Type.getMethodType(Types.Scriptable, Types.Scriptable, Type.INT_TYPE, Types.Realm));

    // defineProperty()
    static final MethodDesc ScriptRuntime_defineProperty = MethodDesc.create(Types.ScriptRuntime,
            "defineProperty",
            Type.getMethodType(Type.VOID_TYPE, Types.Scriptable, Types.String, Types.Object));

    static final MethodDesc ScriptRuntime_defineProperty__int = MethodDesc.create(
            Types.ScriptRuntime, "defineProperty",
            Type.getMethodType(Type.VOID_TYPE, Types.Scriptable, Type.INT_TYPE, Types.Object));

    // enumerate
    static final MethodDesc ScriptRuntime_enumerate = MethodDesc.create(Types.ScriptRuntime,
            "enumerate", Type.getMethodType(Types.Iterator, Types.Object, Types.Realm));

    // EvaluateArrowFunction
    static final MethodDesc ScriptRuntime_EvaluateArrowFunction = MethodDesc.create(
            Types.ScriptRuntime, "EvaluateArrowFunction",
            Type.getMethodType(Types.Object, Types.RuntimeInfo$Function, Types.ExecutionContext));

    // EvaluateConstructorCall
    static final MethodDesc ScriptRuntime_EvaluateConstructorCall = MethodDesc.create(
            Types.ScriptRuntime, "EvaluateConstructorCall",
            Type.getMethodType(Types.Object, Types.Object, Types.Object_, Types.Realm));

    // EvaluateConstructorMethod
    static final MethodDesc ScriptRuntime_EvaluateConstructorMethod = MethodDesc.create(
            Types.ScriptRuntime, "EvaluateConstructorMethod", Type.getMethodType(Types.Function,
                    Types.Scriptable, Types.Scriptable, Types.RuntimeInfo$Function,
                    Types.ExecutionContext));

    // EvaluateFunctionExpression
    static final MethodDesc ScriptRuntime_EvaluateFunctionExpression = MethodDesc.create(
            Types.ScriptRuntime, "EvaluateFunctionExpression",
            Type.getMethodType(Types.Object, Types.RuntimeInfo$Function, Types.ExecutionContext));

    // EvaluateGeneratorComprehension
    static final MethodDesc ScriptRuntime_EvaluateGeneratorComprehension = MethodDesc.create(
            Types.ScriptRuntime, "EvaluateGeneratorComprehension",
            Type.getMethodType(Types.Scriptable, Types.MethodHandle, Types.ExecutionContext));

    // EvaluateGeneratorExpression
    static final MethodDesc ScriptRuntime_EvaluateGeneratorExpression = MethodDesc.create(
            Types.ScriptRuntime, "EvaluateGeneratorExpression",
            Type.getMethodType(Types.Object, Types.RuntimeInfo$Function, Types.ExecutionContext));

    // EvaluatePropertyDefinition
    static final MethodDesc ScriptRuntime_EvaluatePropertyDefinition = MethodDesc.create(
            Types.ScriptRuntime, "EvaluatePropertyDefinition", Type.getMethodType(Type.VOID_TYPE,
                    Types.Scriptable, Types.String, Types.RuntimeInfo$Function,
                    Types.ExecutionContext));

    static final MethodDesc ScriptRuntime_EvaluatePropertyDefinitionGenerator = MethodDesc.create(
            Types.ScriptRuntime, "EvaluatePropertyDefinitionGenerator", Type.getMethodType(
                    Type.VOID_TYPE, Types.Scriptable, Types.String, Types.RuntimeInfo$Function,
                    Types.ExecutionContext));

    static final MethodDesc ScriptRuntime_EvaluatePropertyDefinitionGetter = MethodDesc.create(
            Types.ScriptRuntime, "EvaluatePropertyDefinitionGetter", Type.getMethodType(
                    Type.VOID_TYPE, Types.Scriptable, Types.String, Types.RuntimeInfo$Function,
                    Types.ExecutionContext));

    static final MethodDesc ScriptRuntime_EvaluatePropertyDefinitionSetter = MethodDesc.create(
            Types.ScriptRuntime, "EvaluatePropertyDefinitionSetter", Type.getMethodType(
                    Type.VOID_TYPE, Types.Scriptable, Types.String, Types.RuntimeInfo$Function,
                    Types.ExecutionContext));

    // GetCallThisValue
    static final MethodDesc ScriptRuntime_GetCallThisValue = MethodDesc.create(Types.ScriptRuntime,
            "GetCallThisValue", Type.getMethodType(Types.Object, Types.Object, Types.Realm));

    // getClassProto
    static final MethodDesc ScriptRuntime_getClassProto = MethodDesc.create(Types.ScriptRuntime,
            "getClassProto", Type.getMethodType(Types.Scriptable_, Types.Object, Types.Realm));

    // getDefaultClassProto
    static final MethodDesc ScriptRuntime_getDefaultClassProto = MethodDesc.create(
            Types.ScriptRuntime, "getDefaultClassProto",
            Type.getMethodType(Types.Scriptable_, Types.Realm));

    // getProperty / getElement
    static final MethodDesc ScriptRuntime_getElement = MethodDesc.create(Types.ScriptRuntime,
            "getElement", Type.getMethodType(Types.Reference, Types.Object, Types.Object,
                    Types.Realm, Type.BOOLEAN_TYPE));

    static final MethodDesc ScriptRuntime_getProperty = MethodDesc.create(Types.ScriptRuntime,
            "getProperty", Type.getMethodType(Types.Reference, Types.Object, Types.String,
                    Types.Realm, Type.BOOLEAN_TYPE));

    // getSuperProperty, getSuperElement, getSuperMethod
    static final MethodDesc ScriptRuntime_getSuperElement = MethodDesc.create(Types.ScriptRuntime,
            "getSuperElement", Type.getMethodType(Types.Reference, Types.EnvironmentRecord,
                    Types.Object, Types.Realm, Type.BOOLEAN_TYPE));

    static final MethodDesc ScriptRuntime_getSuperMethod = MethodDesc.create(Types.ScriptRuntime,
            "getSuperMethod",
            Type.getMethodType(Types.Reference, Types.EnvironmentRecord, Type.BOOLEAN_TYPE));

    static final MethodDesc ScriptRuntime_getSuperProperty = MethodDesc.create(Types.ScriptRuntime,
            "getSuperProperty", Type.getMethodType(Types.Reference, Types.EnvironmentRecord,
                    Types.String, Type.BOOLEAN_TYPE));

    // GetTemplateCallSite
    static final MethodDesc ScriptRuntime_GetTemplateCallSite = MethodDesc.create(
            Types.ScriptRuntime, "GetTemplateCallSite", Type.getMethodType(Types.Scriptable,
                    Types.String, Types.MethodHandle, Types.ExecutionContext));

    // GetThisEnvironmentOrThrow
    static final MethodDesc ScriptRuntime_GetThisEnvironmentOrThrow = MethodDesc.create(
            Types.ScriptRuntime, "GetThisEnvironmentOrThrow",
            Type.getMethodType(Types.EnvironmentRecord, Types.ExecutionContext));

    // IsBuiltinEval
    static final MethodDesc ScriptRuntime_IsBuiltinEval = MethodDesc.create(Types.ScriptRuntime,
            "IsBuiltinEval",
            Type.getMethodType(Type.BOOLEAN_TYPE, Types.Object, Types.Callable, Types.Realm));

    // iterate
    static final MethodDesc ScriptRuntime_iterate = MethodDesc.create(Types.ScriptRuntime,
            "iterate", Type.getMethodType(Types.Iterator, Types.Object, Types.Realm));

    // RegExp()
    static final MethodDesc ScriptRuntime_RegExp = MethodDesc.create(Types.ScriptRuntime, "RegExp",
            Type.getMethodType(Types.Object, Types.Realm, Types.String, Types.String));

    // SpreadArray, toFlatArray
    static final MethodDesc ScriptRuntime_SpreadArray = MethodDesc.create(Types.ScriptRuntime,
            "SpreadArray", Type.getMethodType(Types.Object_, Types.Object, Types.Realm));

    static final MethodDesc ScriptRuntime_toFlatArray = MethodDesc.create(Types.ScriptRuntime,
            "toFlatArray", Type.getMethodType(Types.Object_, Types.Object_));

    /* ----------------------------------------------------------------------------------------- */

    // class: StringBuilder

    static final MethodDesc StringBuilder_append_Charsequence = MethodDesc.create(
            Types.StringBuilder, "append",
            Type.getMethodType(Types.StringBuilder, Types.CharSequence));

    static final MethodDesc StringBuilder_append_String = MethodDesc.create(Types.StringBuilder,
            "append", Type.getMethodType(Types.StringBuilder, Types.String));

    static final MethodDesc StringBuilder_init = MethodDesc.create(Types.StringBuilder, "<init>",
            Type.getMethodType(Type.VOID_TYPE));

    static final MethodDesc StringBuilder_toString = MethodDesc.create(Types.StringBuilder,
            "toString", Type.getMethodType(Types.String));

    /* ----------------------------------------------------------------------------------------- */

    // class: Type

    static final MethodDesc Type_isUndefined = MethodDesc.create(Types._Type, "isUndefined",
            Type.getMethodType(Type.BOOLEAN_TYPE, Types.Object));

    static final MethodDesc Type_isUndefinedOrNull = MethodDesc.create(Types._Type,
            "isUndefinedOrNull", Type.getMethodType(Type.BOOLEAN_TYPE, Types.Object));
}
