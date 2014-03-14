/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static com.github.anba.es6draft.semantics.StaticSemantics.ConstructorMethod;
import static com.github.anba.es6draft.semantics.StaticSemantics.PrototypeMethodDefinitions;
import static com.github.anba.es6draft.semantics.StaticSemantics.StaticMethodDefinitions;

import java.util.EnumSet;
import java.util.List;

import org.objectweb.asm.Label;
import org.objectweb.asm.Type;

import com.github.anba.es6draft.ast.*;
import com.github.anba.es6draft.ast.AbruptNode.Abrupt;
import com.github.anba.es6draft.compiler.CodeGenerator.FunctionName;
import com.github.anba.es6draft.compiler.InstructionVisitor.FieldDesc;
import com.github.anba.es6draft.compiler.InstructionVisitor.FieldType;
import com.github.anba.es6draft.compiler.InstructionVisitor.MethodDesc;
import com.github.anba.es6draft.compiler.InstructionVisitor.MethodType;
import com.github.anba.es6draft.compiler.InstructionVisitor.Variable;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.types.Null;
import com.github.anba.es6draft.runtime.types.Reference;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Undefined;

/**
 * Abstract base class for specialised generators
 */
abstract class DefaultCodeGenerator<R, V extends ExpressionVisitor> extends
        DefaultNodeVisitor<R, V> {
    private static final class Fields {
        static final FieldDesc Double_NaN = FieldDesc.create(FieldType.Static, Types.Double, "NaN",
                Type.DOUBLE_TYPE);
    }

    private static final class Methods {
        // class: AbstractOperations
        static final MethodDesc AbstractOperations_CreateIterResultObject = MethodDesc.create(
                MethodType.Static, Types.AbstractOperations, "CreateIterResultObject", Type
                        .getMethodType(Types.ScriptObject, Types.ExecutionContext, Types.Object,
                                Type.BOOLEAN_TYPE));

        static final MethodDesc AbstractOperations_GetIterator = MethodDesc.create(
                MethodType.Static, Types.AbstractOperations, "GetIterator",
                Type.getMethodType(Types.ScriptObject, Types.ExecutionContext, Types.Object));

        static final MethodDesc AbstractOperations_HasOwnProperty = MethodDesc.create(
                MethodType.Static, Types.AbstractOperations, "HasOwnProperty", Type
                        .getMethodType(Type.BOOLEAN_TYPE, Types.ExecutionContext,
                                Types.ScriptObject, Types.String));

        static final MethodDesc AbstractOperations_HasProperty = MethodDesc.create(
                MethodType.Static, Types.AbstractOperations, "HasProperty", Type
                        .getMethodType(Type.BOOLEAN_TYPE, Types.ExecutionContext,
                                Types.ScriptObject, Types.String));

        static final MethodDesc AbstractOperations_IsExtensible = MethodDesc.create(
                MethodType.Static, Types.AbstractOperations, "IsExtensible",
                Type.getMethodType(Type.BOOLEAN_TYPE, Types.ExecutionContext, Types.ScriptObject));

        static final MethodDesc AbstractOperations_IteratorComplete = MethodDesc.create(
                MethodType.Static, Types.AbstractOperations, "IteratorComplete",
                Type.getMethodType(Type.BOOLEAN_TYPE, Types.ExecutionContext, Types.ScriptObject));

        static final MethodDesc AbstractOperations_IteratorNext = MethodDesc.create(
                MethodType.Static, Types.AbstractOperations, "IteratorNext", Type.getMethodType(
                        Types.ScriptObject, Types.ExecutionContext, Types.ScriptObject,
                        Types.Object));

        static final MethodDesc AbstractOperations_IteratorThrow = MethodDesc.create(
                MethodType.Static, Types.AbstractOperations, "IteratorThrow", Type.getMethodType(
                        Types.ScriptObject, Types.ExecutionContext, Types.ScriptObject,
                        Types.Object));

        static final MethodDesc AbstractOperations_IteratorValue = MethodDesc.create(
                MethodType.Static, Types.AbstractOperations, "IteratorValue",
                Type.getMethodType(Types.Object, Types.ExecutionContext, Types.ScriptObject));

        static final MethodDesc AbstractOperations_ToPrimitive = MethodDesc.create(
                MethodType.Static, Types.AbstractOperations, "ToPrimitive",
                Type.getMethodType(Types.Object, Types.ExecutionContext, Types.Object));

        static final MethodDesc AbstractOperations_ToBoolean = MethodDesc.create(MethodType.Static,
                Types.AbstractOperations, "ToBoolean",
                Type.getMethodType(Type.BOOLEAN_TYPE, Types.Object));

        static final MethodDesc AbstractOperations_ToBoolean_double = MethodDesc.create(
                MethodType.Static, Types.AbstractOperations, "ToBoolean",
                Type.getMethodType(Type.BOOLEAN_TYPE, Type.DOUBLE_TYPE));

        static final MethodDesc AbstractOperations_ToFlatString = MethodDesc.create(
                MethodType.Static, Types.AbstractOperations, "ToFlatString",
                Type.getMethodType(Types.String, Types.ExecutionContext, Types.Object));

        static final MethodDesc AbstractOperations_ToNumber = MethodDesc.create(MethodType.Static,
                Types.AbstractOperations, "ToNumber",
                Type.getMethodType(Type.DOUBLE_TYPE, Types.ExecutionContext, Types.Object));

        static final MethodDesc AbstractOperations_ToNumber_CharSequence = MethodDesc.create(
                MethodType.Static, Types.AbstractOperations, "ToNumber",
                Type.getMethodType(Type.DOUBLE_TYPE, Types.CharSequence));

        static final MethodDesc AbstractOperations_ToInt32 = MethodDesc.create(MethodType.Static,
                Types.AbstractOperations, "ToInt32",
                Type.getMethodType(Type.INT_TYPE, Types.ExecutionContext, Types.Object));

        static final MethodDesc AbstractOperations_ToInt32_double = MethodDesc.create(
                MethodType.Static, Types.AbstractOperations, "ToInt32",
                Type.getMethodType(Type.INT_TYPE, Type.DOUBLE_TYPE));

        static final MethodDesc AbstractOperations_ToUint32 = MethodDesc.create(MethodType.Static,
                Types.AbstractOperations, "ToUint32",
                Type.getMethodType(Type.LONG_TYPE, Types.ExecutionContext, Types.Object));

        static final MethodDesc AbstractOperations_ToUint32_double = MethodDesc.create(
                MethodType.Static, Types.AbstractOperations, "ToUint32",
                Type.getMethodType(Type.LONG_TYPE, Type.DOUBLE_TYPE));

        static final MethodDesc AbstractOperations_ToObject = MethodDesc.create(MethodType.Static,
                Types.AbstractOperations, "ToObject",
                Type.getMethodType(Types.ScriptObject, Types.ExecutionContext, Types.Object));

        static final MethodDesc AbstractOperations_ToPropertyKey = MethodDesc.create(
                MethodType.Static, Types.AbstractOperations, "ToPropertyKey",
                Type.getMethodType(Types.Object, Types.ExecutionContext, Types.Object));

        static final MethodDesc AbstractOperations_ToString = MethodDesc.create(MethodType.Static,
                Types.AbstractOperations, "ToString",
                Type.getMethodType(Types.CharSequence, Types.ExecutionContext, Types.Object));

        static final MethodDesc AbstractOperations_ToString_int = MethodDesc.create(
                MethodType.Static, Types.AbstractOperations, "ToString",
                Type.getMethodType(Types.String, Type.INT_TYPE));

        static final MethodDesc AbstractOperations_ToString_long = MethodDesc.create(
                MethodType.Static, Types.AbstractOperations, "ToString",
                Type.getMethodType(Types.String, Type.LONG_TYPE));

        static final MethodDesc AbstractOperations_ToString_double = MethodDesc.create(
                MethodType.Static, Types.AbstractOperations, "ToString",
                Type.getMethodType(Types.String, Type.DOUBLE_TYPE));

        // class: Boolean
        static final MethodDesc Boolean_toString = MethodDesc.create(MethodType.Static,
                Types.Boolean, "toString", Type.getMethodType(Types.String, Type.BOOLEAN_TYPE));

        // class: CharSequence
        static final MethodDesc CharSequence_length = MethodDesc.create(MethodType.Interface,
                Types.CharSequence, "length", Type.getMethodType(Type.INT_TYPE));
        static final MethodDesc CharSequence_toString = MethodDesc.create(MethodType.Interface,
                Types.CharSequence, "toString", Type.getMethodType(Types.String));

        // class: EnvironmentRecord
        static final MethodDesc EnvironmentRecord_createImmutableBinding = MethodDesc.create(
                MethodType.Interface, Types.EnvironmentRecord, "createImmutableBinding",
                Type.getMethodType(Type.VOID_TYPE, Types.String));

        static final MethodDesc EnvironmentRecord_initialiseBinding = MethodDesc.create(
                MethodType.Interface, Types.EnvironmentRecord, "initialiseBinding",
                Type.getMethodType(Type.VOID_TYPE, Types.String, Types.Object));

        // class: ExecutionContext
        static final MethodDesc ExecutionContext_getLexicalEnvironment = MethodDesc.create(
                MethodType.Virtual, Types.ExecutionContext, "getLexicalEnvironment",
                Type.getMethodType(Types.LexicalEnvironment));

        static final MethodDesc ExecutionContext_pushLexicalEnvironment = MethodDesc.create(
                MethodType.Virtual, Types.ExecutionContext, "pushLexicalEnvironment",
                Type.getMethodType(Type.VOID_TYPE, Types.LexicalEnvironment));

        static final MethodDesc ExecutionContext_popLexicalEnvironment = MethodDesc.create(
                MethodType.Virtual, Types.ExecutionContext, "popLexicalEnvironment",
                Type.getMethodType(Type.VOID_TYPE));

        static final MethodDesc ExecutionContext_replaceLexicalEnvironment = MethodDesc.create(
                MethodType.Virtual, Types.ExecutionContext, "replaceLexicalEnvironment",
                Type.getMethodType(Type.VOID_TYPE, Types.LexicalEnvironment));

        static final MethodDesc ExecutionContext_restoreLexicalEnvironment = MethodDesc.create(
                MethodType.Virtual, Types.ExecutionContext, "restoreLexicalEnvironment",
                Type.getMethodType(Type.VOID_TYPE, Types.LexicalEnvironment));

        // class: LexicalEnvironment
        static final MethodDesc LexicalEnvironment_getEnvRec = MethodDesc.create(
                MethodType.Virtual, Types.LexicalEnvironment, "getEnvRec",
                Type.getMethodType(Types.EnvironmentRecord));

        static final MethodDesc LexicalEnvironment_cloneDeclarativeEnvironment = MethodDesc.create(
                MethodType.Static, Types.LexicalEnvironment, "cloneDeclarativeEnvironment",
                Type.getMethodType(Types.LexicalEnvironment, Types.LexicalEnvironment));

        static final MethodDesc LexicalEnvironment_newDeclarativeEnvironment = MethodDesc.create(
                MethodType.Static, Types.LexicalEnvironment, "newDeclarativeEnvironment",
                Type.getMethodType(Types.LexicalEnvironment, Types.LexicalEnvironment));

        static final MethodDesc LexicalEnvironment_newObjectEnvironment = MethodDesc.create(
                MethodType.Static, Types.LexicalEnvironment, "newObjectEnvironment", Type
                        .getMethodType(Types.LexicalEnvironment, Types.ScriptObject,
                                Types.LexicalEnvironment, Type.BOOLEAN_TYPE));

        // class: OrdinaryFunction
        static final MethodDesc OrdinaryFunction_SetFunctionName_String = MethodDesc.create(
                MethodType.Static, Types.OrdinaryFunction, "SetFunctionName",
                Type.getMethodType(Type.VOID_TYPE, Types.FunctionObject, Types.String));

        static final MethodDesc OrdinaryFunction_SetFunctionName_Symbol = MethodDesc.create(
                MethodType.Static, Types.OrdinaryFunction, "SetFunctionName",
                Type.getMethodType(Type.VOID_TYPE, Types.FunctionObject, Types.Symbol));

        // class: ScriptException
        static final MethodDesc ScriptException_getValue = MethodDesc.create(MethodType.Virtual,
                Types.ScriptException, "getValue", Type.getMethodType(Types.Object));

        // class: ScriptRuntime
        static final MethodDesc ScriptRuntime_CreateDefaultConstructor = MethodDesc.create(
                MethodType.Static, Types.ScriptRuntime, "CreateDefaultConstructor",
                Type.getMethodType(Types.RuntimeInfo$Function));

        static final MethodDesc ScriptRuntime_CreateDefaultEmptyConstructor = MethodDesc.create(
                MethodType.Static, Types.ScriptRuntime, "CreateDefaultEmptyConstructor",
                Type.getMethodType(Types.RuntimeInfo$Function));

        static final MethodDesc ScriptRuntime_delegatedYield = MethodDesc.create(MethodType.Static,
                Types.ScriptRuntime, "delegatedYield",
                Type.getMethodType(Types.Object, Types.Object, Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_EvaluateConstructorMethod = MethodDesc.create(
                MethodType.Static, Types.ScriptRuntime, "EvaluateConstructorMethod", Type
                        .getMethodType(Types.OrdinaryFunction, Types.ScriptObject,
                                Types.ScriptObject, Types.RuntimeInfo$Function,
                                Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_getClassProto = MethodDesc.create(MethodType.Static,
                Types.ScriptRuntime, "getClassProto",
                Type.getMethodType(Types.ScriptObject_, Types.Object, Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_getDefaultClassProto = MethodDesc.create(
                MethodType.Static, Types.ScriptRuntime, "getDefaultClassProto",
                Type.getMethodType(Types.ScriptObject_, Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_yield = MethodDesc.create(MethodType.Static,
                Types.ScriptRuntime, "yield",
                Type.getMethodType(Types.Object, Types.Object, Types.ExecutionContext));

        // class: Type
        static final MethodDesc Type_isUndefinedOrNull = MethodDesc.create(MethodType.Static,
                Types._Type, "isUndefinedOrNull",
                Type.getMethodType(Type.BOOLEAN_TYPE, Types.Object));
    }

    protected final CodeGenerator codegen;

    protected DefaultCodeGenerator(CodeGenerator codegen) {
        this.codegen = codegen;
    }

    /**
     * stack: [] -> [value|reference]
     */
    protected final ValType expression(Expression node, ExpressionVisitor mv) {
        return codegen.expression(node, mv);
    }

    /**
     * stack: [] -> [value]
     */
    protected final ValType expressionValue(Expression node, ExpressionVisitor mv) {
        return codegen.expressionValue(node, mv);
    }

    /**
     * stack: [] -> [boxed(value)]
     */
    protected final ValType expressionBoxedValue(Expression node, ExpressionVisitor mv) {
        return codegen.expressionBoxedValue(node, mv);
    }

    /**
     * stack: [] -> []
     */
    protected final Variable<LexicalEnvironment> saveEnvironment(AbruptNode node,
            StatementVisitor mv) {
        EnumSet<Abrupt> abrupt = node.getAbrupt();
        if (abrupt.contains(Abrupt.Break) || abrupt.contains(Abrupt.Continue)) {
            return saveEnvironment(mv);
        }
        return null;
    }

    /**
     * stack: [] -> []
     */
    protected final Variable<LexicalEnvironment> saveEnvironment(StatementVisitor mv) {
        Variable<LexicalEnvironment> savedEnv = mv
                .newVariable("savedEnv", LexicalEnvironment.class);
        saveEnvironment(savedEnv, mv);
        return savedEnv;
    }

    /**
     * stack: [] -> []
     */
    protected final void saveEnvironment(Variable<LexicalEnvironment> savedEnv, StatementVisitor mv) {
        getLexicalEnvironment(mv);
        mv.store(savedEnv);
    }

    /**
     * stack: [] -> []
     */
    protected final void restoreEnvironment(AbruptNode node, Abrupt abrupt,
            Variable<LexicalEnvironment> savedEnv, StatementVisitor mv) {
        assert node.getAbrupt().contains(abrupt);
        assert savedEnv != null;
        restoreEnvironment(savedEnv, mv);
    }

    /**
     * stack: [] -> []
     */
    protected final void restoreEnvironment(Variable<LexicalEnvironment> savedEnv,
            StatementVisitor mv) {
        mv.loadExecutionContext();
        mv.load(savedEnv);
        mv.invoke(Methods.ExecutionContext_restoreLexicalEnvironment);
    }

    /**
     * stack: [] -> []
     */
    protected final void replaceLexicalEnvironment(Variable<LexicalEnvironment> savedEnv,
            StatementVisitor mv) {
        mv.loadExecutionContext();
        mv.load(savedEnv);
        mv.invoke(Methods.ExecutionContext_replaceLexicalEnvironment);
    }

    /**
     * stack: [] -> [lexEnv]
     */
    protected final void getLexicalEnvironment(ExpressionVisitor mv) {
        mv.loadExecutionContext();
        mv.invoke(Methods.ExecutionContext_getLexicalEnvironment);
    }

    /**
     * stack: [] -> [envRec]
     */
    protected final void getEnvironmentRecord(ExpressionVisitor mv) {
        mv.loadExecutionContext();
        mv.invoke(Methods.ExecutionContext_getLexicalEnvironment);
        mv.invoke(Methods.LexicalEnvironment_getEnvRec);
    }

    /**
     * stack: [obj] -> [lexEnv]
     */
    protected final void newObjectEnvironment(ExpressionVisitor mv, boolean withEnvironment) {
        mv.loadExecutionContext();
        mv.invoke(Methods.ExecutionContext_getLexicalEnvironment);
        mv.iconst(withEnvironment);
        mv.invoke(Methods.LexicalEnvironment_newObjectEnvironment);
    }

    /**
     * stack: [] -> [lexEnv]
     */
    protected final void newDeclarativeEnvironment(ExpressionVisitor mv) {
        mv.loadExecutionContext();
        mv.invoke(Methods.ExecutionContext_getLexicalEnvironment);
        mv.invoke(Methods.LexicalEnvironment_newDeclarativeEnvironment);
    }

    /**
     * stack: [] -> [lexEnv]
     */
    protected final void cloneDeclarativeEnvironment(ExpressionVisitor mv) {
        mv.loadExecutionContext();
        mv.invoke(Methods.ExecutionContext_getLexicalEnvironment);
        mv.invoke(Methods.LexicalEnvironment_cloneDeclarativeEnvironment);
    }

    /**
     * stack: [lexEnv] -> []
     */
    protected final void pushLexicalEnvironment(ExpressionVisitor mv) {
        mv.loadExecutionContext();
        mv.swap();
        mv.invoke(Methods.ExecutionContext_pushLexicalEnvironment);
    }

    /**
     * stack: [] -> []
     */
    protected final void popLexicalEnvironment(ExpressionVisitor mv) {
        mv.loadExecutionContext();
        mv.invoke(Methods.ExecutionContext_popLexicalEnvironment);
    }

    /**
     * stack: [object] -> [boolean]
     */
    protected final void isUndefinedOrNull(ExpressionVisitor mv) {
        mv.invoke(Methods.Type_isUndefinedOrNull);
    }

    enum ValType {
        Undefined, Null, Boolean, Number, Number_int, Number_uint, String, Object, Reference, Any;

        public int size() {
            switch (this) {
            case Number:
            case Number_uint:
                return 2;
            case Number_int:
            case Undefined:
            case Null:
            case Boolean:
            case String:
            case Object:
            case Reference:
            case Any:
            default:
                return 1;
            }
        }

        public boolean isNumeric() {
            switch (this) {
            case Number:
            case Number_int:
            case Number_uint:
                return true;
            case Undefined:
            case Null:
            case Boolean:
            case String:
            case Object:
            case Reference:
            case Any:
            default:
                return false;
            }
        }

        public boolean isPrimitive() {
            switch (this) {
            case Undefined:
            case Null:
            case Boolean:
            case Number:
            case Number_int:
            case Number_uint:
            case String:
                return true;
            case Object:
            case Reference:
            case Any:
            default:
                return false;
            }
        }

        public boolean isJavaPrimitive() {
            switch (this) {
            case Boolean:
            case Number:
            case Number_int:
            case Number_uint:
                return true;
            case Undefined:
            case Null:
            case String:
            case Object:
            case Reference:
            case Any:
            default:
                return false;
            }
        }

        public Class<?> toClass() {
            switch (this) {
            case Boolean:
                return boolean.class;
            case String:
                return CharSequence.class;
            case Number:
                return double.class;
            case Number_int:
                return int.class;
            case Number_uint:
                return long.class;
            case Object:
                return ScriptObject.class;
            case Reference:
                return Reference.class;
            case Null:
                return Null.class;
            case Undefined:
                return Undefined.class;
            case Any:
            default:
                return Object.class;
            }
        }

        public Type toType() {
            switch (this) {
            case Boolean:
                return Type.BOOLEAN_TYPE;
            case String:
                return Types.CharSequence;
            case Number:
                return Type.DOUBLE_TYPE;
            case Number_int:
                return Type.INT_TYPE;
            case Number_uint:
                return Type.LONG_TYPE;
            case Object:
                return Types.ScriptObject;
            case Reference:
                return Types.Reference;
            case Null:
                return Types.Null;
            case Undefined:
                return Types.Undefined;
            case Any:
            default:
                return Types.Object;
            }
        }

        public Type toBoxedType() {
            switch (this) {
            case Boolean:
                return Types.Boolean;
            case String:
                return Types.CharSequence;
            case Number:
                return Types.Double;
            case Number_int:
                return Types.Integer;
            case Number_uint:
                return Types.Long;
            case Object:
                return Types.ScriptObject;
            case Reference:
                return Types.Reference;
            case Null:
                return Types.Null;
            case Undefined:
                return Types.Undefined;
            case Any:
            default:
                return Types.Object;
            }
        }
    }

    /**
     * stack: [Object] -> [boolean]
     */
    protected final ValType ToPrimitive(ValType from, ExpressionVisitor mv) {
        switch (from) {
        case Number:
        case Number_int:
        case Number_uint:
        case Undefined:
        case Null:
        case Boolean:
        case String:
            return from;
        case Object:
        case Any:
        default:
            mv.loadExecutionContext();
            mv.swap();
            mv.invoke(Methods.AbstractOperations_ToPrimitive);
            return ValType.Any;
        }
    }

    /**
     * stack: [Object] -> [boolean]
     */
    protected final void ToBoolean(ValType from, ExpressionVisitor mv) {
        switch (from) {
        case Number:
            mv.invoke(Methods.AbstractOperations_ToBoolean_double);
            return;
        case Number_int:
            mv.cast(Type.INT_TYPE, Type.DOUBLE_TYPE);
            mv.invoke(Methods.AbstractOperations_ToBoolean_double);
            return;
        case Number_uint:
            mv.cast(Type.LONG_TYPE, Type.DOUBLE_TYPE);
            mv.invoke(Methods.AbstractOperations_ToBoolean_double);
            return;
        case Undefined:
        case Null:
            mv.pop();
            mv.iconst(false);
            return;
        case Boolean:
            return;
        case String: {
            Label l0 = new Label(), l1 = new Label();
            mv.invoke(Methods.CharSequence_length);
            mv.ifeq(l0);
            mv.iconst(true);
            mv.goTo(l1);
            mv.mark(l0);
            mv.iconst(false);
            mv.mark(l1);
            return;
        }
        case Object:
            mv.pop();
            mv.iconst(true);
            return;
        case Any:
        default:
            mv.invoke(Methods.AbstractOperations_ToBoolean);
            return;
        }
    }

    /**
     * stack: [Object] -> [double]
     */
    protected final void ToNumber(ValType from, ExpressionVisitor mv) {
        switch (from) {
        case Number:
            return;
        case Number_int:
            mv.cast(Type.INT_TYPE, Type.DOUBLE_TYPE);
            return;
        case Number_uint:
            mv.cast(Type.LONG_TYPE, Type.DOUBLE_TYPE);
            return;
        case Undefined:
            mv.pop();
            mv.get(Fields.Double_NaN);
            return;
        case Null:
            mv.pop();
            mv.dconst(0);
            return;
        case Boolean:
            mv.cast(Type.INT_TYPE, Type.DOUBLE_TYPE);
            return;
        case String:
            mv.invoke(Methods.AbstractOperations_ToNumber_CharSequence);
            return;
        case Object:
        case Any:
        default:
            mv.loadExecutionContext();
            mv.swap();
            mv.invoke(Methods.AbstractOperations_ToNumber);
            return;
        }
    }

    /**
     * stack: [Object] -> [int]
     */
    protected final void ToInt32(ValType from, ExpressionVisitor mv) {
        switch (from) {
        case Number:
            mv.invoke(Methods.AbstractOperations_ToInt32_double);
            return;
        case Number_int:
            return;
        case Number_uint:
            mv.cast(Type.LONG_TYPE, Type.INT_TYPE);
            return;
        case Undefined:
        case Null:
            mv.pop();
            mv.iconst(0);
            return;
        case Boolean:
            return;
        case String:
            mv.invoke(Methods.AbstractOperations_ToNumber_CharSequence);
            mv.invoke(Methods.AbstractOperations_ToInt32_double);
            return;
        case Object:
        case Any:
        default:
            mv.loadExecutionContext();
            mv.swap();
            mv.invoke(Methods.AbstractOperations_ToInt32);
            return;
        }
    }

    /**
     * stack: [Object] -> [long]
     */
    protected final void ToUint32(ValType from, ExpressionVisitor mv) {
        switch (from) {
        case Number:
            mv.invoke(Methods.AbstractOperations_ToUint32_double);
            return;
        case Number_int:
            mv.cast(Type.INT_TYPE, Type.LONG_TYPE);
            mv.lconst(0xffff_ffffL);
            mv.and(Type.LONG_TYPE);
            return;
        case Number_uint:
            return;
        case Undefined:
        case Null:
            mv.pop();
            mv.lconst(0);
            return;
        case Boolean:
            mv.cast(Type.INT_TYPE, Type.LONG_TYPE);
            return;
        case String:
            mv.invoke(Methods.AbstractOperations_ToNumber_CharSequence);
            mv.invoke(Methods.AbstractOperations_ToUint32_double);
            return;
        case Object:
        case Any:
        default:
            mv.loadExecutionContext();
            mv.swap();
            mv.invoke(Methods.AbstractOperations_ToUint32);
            return;
        }
    }

    /**
     * stack: [Object] -> [CharSequence]
     */
    protected final void ToString(ValType from, ExpressionVisitor mv) {
        switch (from) {
        case Number:
            mv.invoke(Methods.AbstractOperations_ToString_double);
            return;
        case Number_int:
            mv.invoke(Methods.AbstractOperations_ToString_int);
            return;
        case Number_uint:
            mv.invoke(Methods.AbstractOperations_ToString_long);
            return;
        case Undefined:
            mv.pop();
            mv.aconst("undefined");
            return;
        case Null:
            mv.pop();
            mv.aconst("null");
            return;
        case Boolean:
            mv.invoke(Methods.Boolean_toString);
            return;
        case String:
            return;
        case Object:
        case Any:
        default:
            mv.loadExecutionContext();
            mv.swap();
            mv.invoke(Methods.AbstractOperations_ToString);
            return;
        }
    }

    /**
     * stack: [Object] -> [String]
     */
    protected final void ToFlatString(ValType from, ExpressionVisitor mv) {
        switch (from) {
        case Number:
            mv.invoke(Methods.AbstractOperations_ToString_double);
            return;
        case Number_int:
            mv.invoke(Methods.AbstractOperations_ToString_int);
            return;
        case Number_uint:
            mv.invoke(Methods.AbstractOperations_ToString_long);
            return;
        case Undefined:
            mv.pop();
            mv.aconst("undefined");
            return;
        case Null:
            mv.pop();
            mv.aconst("null");
            return;
        case Boolean:
            mv.invoke(Methods.Boolean_toString);
            return;
        case String:
            mv.invoke(Methods.CharSequence_toString);
            return;
        case Object:
        case Any:
        default:
            mv.loadExecutionContext();
            mv.swap();
            mv.invoke(Methods.AbstractOperations_ToFlatString);
            return;
        }
    }

    /**
     * stack: [Object] -> [ScriptObject]
     */
    protected final void ToObject(ValType from, ExpressionVisitor mv) {
        switch (from) {
        case Number:
        case Number_int:
        case Number_uint:
        case Boolean:
            mv.toBoxed(from);
            break;
        case Object:
            return;
        case Undefined:
        case Null:
        case String:
        case Any:
        default:
            break;
        }

        mv.loadExecutionContext();
        mv.swap();
        mv.invoke(Methods.AbstractOperations_ToObject);
    }

    /**
     * stack: [Object] -> [String|ExoticSymbol]
     */
    protected static final void ToPropertyKey(ValType from, ExpressionVisitor mv) {
        switch (from) {
        case Number:
            mv.invoke(Methods.AbstractOperations_ToString_double);
            return;
        case Number_int:
            mv.invoke(Methods.AbstractOperations_ToString_int);
            return;
        case Number_uint:
            mv.invoke(Methods.AbstractOperations_ToString_long);
            return;
        case Undefined:
            mv.pop();
            mv.aconst("undefined");
            return;
        case Null:
            mv.pop();
            mv.aconst("null");
            return;
        case Boolean:
            mv.invoke(Methods.Boolean_toString);
            return;
        case String:
            mv.invoke(Methods.CharSequence_toString);
            return;
        case Object:
            mv.loadExecutionContext();
            mv.swap();
            mv.invoke(Methods.AbstractOperations_ToFlatString);
            return;
        case Any:
        default:
            mv.loadExecutionContext();
            mv.swap();
            mv.invoke(Methods.AbstractOperations_ToPropertyKey);
            return;
        }
    }

    /**
     * stack: [propertyKey, function] -> [propertyKey, function]
     */
    protected static void SetFunctionName(Node node, ValType propertyKeyType, ExpressionVisitor mv) {
        assert node instanceof ClassDefinition || node instanceof FunctionNode;

        Label hasOwnName = null;
        if (node instanceof ClassDefinition) {
            hasOwnName = new Label();
            // stack: [propertyKey, function] -> [propertyKey, function, cx, function, "name"]
            mv.dup();
            mv.loadExecutionContext();
            mv.swap();
            mv.aconst("name");
            // stack: [propertyKey, function, cx, function, "name"] -> [propertyKey, function]
            mv.invoke(Methods.AbstractOperations_HasOwnProperty);
            mv.ifne(hasOwnName);
        }

        // stack: [propertyKey, function] -> [propertyKey, function, function, propertyKey]
        mv.dup2();
        mv.swap();

        if (propertyKeyType == ValType.String) {
            mv.invoke(Methods.OrdinaryFunction_SetFunctionName_String);
        } else {
            assert propertyKeyType == ValType.Any;
            Label isString = new Label(), afterSetFunctionName = new Label();
            mv.dup();
            mv.instanceOf(Types.String);
            mv.ifeq(isString);
            {
                // stack: [propertyKey, function, function, propertyKey] -> [propertyKey, function]
                mv.checkcast(Types.String);
                mv.invoke(Methods.OrdinaryFunction_SetFunctionName_String);
                mv.goTo(afterSetFunctionName);
            }
            {
                mv.mark(isString);
                mv.checkcast(Types.Symbol);
                mv.invoke(Methods.OrdinaryFunction_SetFunctionName_Symbol);
            }
            mv.mark(afterSetFunctionName);
        }

        if (hasOwnName != null) {
            mv.mark(hasOwnName);
        }
    }

    /**
     * stack: [function] -> [function]
     */
    protected static void SetFunctionName(Node node, String name, ExpressionVisitor mv) {
        assert node instanceof ClassDefinition || node instanceof FunctionNode;

        Label hasOwnName = null;
        if (node instanceof ClassDefinition) {
            hasOwnName = new Label();

            // FIXME: workaround for https://bugs.ecmascript.org/show_bug.cgi?id=2578
            if (((ClassDefinition) node).getName() != null) {
                // Call to SetFunctionName for non-anonymous class definition
                // stack: [function] -> [function, cx, function]
                mv.dup();
                mv.loadExecutionContext();
                mv.swap();
                // stack: [function, cx, function] -> [function]
                mv.invoke(Methods.AbstractOperations_IsExtensible);
                mv.ifeq(hasOwnName);
            }

            // stack: [function] -> [function, cx, function, "name"]
            mv.dup();
            mv.loadExecutionContext();
            mv.swap();
            mv.aconst("name");
            // stack: [function, cx, function, "name"] -> [function]
            mv.invoke(Methods.AbstractOperations_HasOwnProperty);
            mv.ifne(hasOwnName);
        }

        // stack: [function] -> [function, function, name]
        mv.dup();
        mv.aconst(name);
        // stack: [function, function, name] -> [function]
        mv.invoke(Methods.OrdinaryFunction_SetFunctionName_String);

        if (hasOwnName != null) {
            mv.mark(hasOwnName);
        }
    }

    /**
     * stack: [value] -> []
     */
    protected final void BindingInitialisation(Binding node, ExpressionVisitor mv) {
        new BindingInitialisationGenerator(codegen).generate(node, mv);
    }

    /**
     * stack: [envRec, value] -> []
     */
    protected final void BindingInitialisationWithEnvironment(Binding node, ExpressionVisitor mv) {
        new BindingInitialisationGenerator(codegen).generateWithEnvironment(node, mv);
    }

    /**
     * stack: [value] -> []
     */
    protected final void DestructuringAssignment(AssignmentPattern node, ExpressionVisitor mv) {
        new DestructuringAssignmentGenerator(codegen).generate(node, mv);
    }

    /**
     * 14.5.1.2 Runtime Semantics<br>
     * Runtime Semantics: ClassDefinitionEvaluation
     */
    protected final void ClassDefinitionEvaluation(ClassDefinition def, String className,
            ExpressionVisitor mv) {
        mv.enterClassDefinition();
        // steps 1-3
        // stack: [] -> [<proto,ctor>]
        if (def.getHeritage() == null) {
            mv.loadExecutionContext();
            mv.invoke(Methods.ScriptRuntime_getDefaultClassProto);
        } else {
            expressionBoxedValue(def.getHeritage(), mv);
            mv.loadExecutionContext();
            mv.invoke(Methods.ScriptRuntime_getClassProto);
        }

        // stack: [<proto,ctor>] -> [ctor, proto]
        mv.dup();
        mv.iconst(1);
        mv.aload(Types.ScriptObject_);
        mv.swap();
        mv.iconst(0);
        mv.aload(Types.ScriptObject_);

        // steps 4-5
        if (className != null) {
            // stack: [ctor, proto] -> [ctor, proto, scope]
            newDeclarativeEnvironment(mv);

            // stack: [ctor, proto, scope] -> [ctor, proto, scope]
            mv.dup();
            mv.invoke(Methods.LexicalEnvironment_getEnvRec);
            mv.aconst(className);
            mv.invoke(Methods.EnvironmentRecord_createImmutableBinding);

            // stack: [ctor, proto, scope] -> [ctor, proto]
            pushLexicalEnvironment(mv);
            mv.enterScope(def);
        }

        // stack: [ctor, proto] -> [proto, ctor, proto]
        mv.dupX1();

        // step 6
        MethodDefinition constructor = ConstructorMethod(def);
        if (constructor != null) {
            codegen.compile(constructor);
            // Runtime Semantics: Evaluation -> MethodDefinition
            mv.invoke(codegen.methodDesc(constructor, FunctionName.RTI));
        } else {
            // step 7
            if (def.getHeritage() != null) {
                // FIXME: spec bug? - `new (class extends null {})` throws TypeError
                mv.invoke(Methods.ScriptRuntime_CreateDefaultConstructor);
            } else {
                mv.invoke(Methods.ScriptRuntime_CreateDefaultEmptyConstructor);
            }
        }

        // step 8 (empty)
        // steps 9-11, steps 13-14
        // stack: [proto, ctor, proto, <rti>] -> [proto, F]
        mv.loadExecutionContext();
        mv.invoke(Methods.ScriptRuntime_EvaluateConstructorMethod);

        // step 12
        if (className != null) {
            // stack: [proto, F] -> [proto, F, F, envRec]
            mv.dup();
            getLexicalEnvironment(mv);
            mv.invoke(Methods.LexicalEnvironment_getEnvRec);

            // stack: [proto, F, F, envRec] -> [proto, F, envRec, name, F]
            mv.swap();
            mv.aconst(className);
            mv.swap();

            // stack: [proto, F, envRec, name, F] -> [proto, F]
            mv.invoke(Methods.EnvironmentRecord_initialiseBinding);
        }

        // stack: [proto, F] -> [F, proto]
        mv.swap();

        // steps 15-16
        List<MethodDefinition> protoMethods = PrototypeMethodDefinitions(def);
        for (MethodDefinition method : protoMethods) {
            if (method == constructor) {
                continue;
            }
            mv.dup();
            codegen.propertyDefinition(method, mv);
        }

        // stack: [F, proto] -> [F]
        mv.pop();

        // steps 17-18
        List<MethodDefinition> staticMethods = StaticMethodDefinitions(def);
        for (MethodDefinition method : staticMethods) {
            mv.dup();
            codegen.propertyDefinition(method, mv);
        }

        // step 19
        if (className != null) {
            mv.exitScope();
            // restore previous lexical environment
            popLexicalEnvironment(mv);
        }
        // step 20 (empty)
        mv.exitClassDefinition();
    }

    /**
     * 14.4 Generator Function Definitions
     * <p>
     * 14.4.14 Runtime Semantics: Evaluation
     * <ul>
     * <li>YieldExpression : yield * AssignmentExpression
     * </ul>
     * <p>
     * stack: [value] -> [value]
     */
    protected final void delegatedYield(Expression node, ExpressionVisitor mv) {
        mv.lineInfo(node);
        if (!mv.hasSyntheticMethods() && mv.hasStack()) {
            Label iteratorNext = new Label(), iteratorThrow = new Label(), iteratorComplete = new Label();
            Label done = new Label();

            mv.enterVariableScope();
            Variable<ScriptObject> iterator = mv.newVariable("iterator", ScriptObject.class);
            Variable<ScriptObject> innerResult = mv.newVariable("innerResult", ScriptObject.class);
            Variable<Object> received = mv.newVariable("received", Object.class);

            /* steps 4-5 */
            // stack: [value] -> []
            mv.loadExecutionContext();
            mv.swap();
            mv.invoke(Methods.AbstractOperations_GetIterator);
            mv.store(iterator);

            /* step 6 */
            // stack: [] -> []
            mv.loadUndefined();
            mv.store(received);

            /* step 7a */
            // stack: [] -> [innerResult]
            mv.mark(iteratorNext);
            mv.loadExecutionContext();
            mv.load(iterator);
            mv.load(received);
            mv.invoke(Methods.AbstractOperations_IteratorNext);
            mv.goToAndSetStack(iteratorComplete, iteratorNext);

            /* step 7b (I) */
            // stack: [] -> [innerResult]
            mv.mark(iteratorThrow);
            mv.loadExecutionContext();
            mv.load(iterator);
            mv.load(received);
            mv.invoke(Methods.AbstractOperations_IteratorThrow);

            /* steps 7c-7d */
            // stack: [innerResult] -> [done]
            mv.mark(iteratorComplete);
            mv.store(innerResult);
            mv.loadExecutionContext();
            mv.load(innerResult);
            mv.invoke(Methods.AbstractOperations_IteratorComplete);
            mv.ifne(done);

            /* step 7f */
            // stack: [] -> [Object(innerResult)]
            // force stack top to Object-type
            mv.load(innerResult);
            mv.checkcast(Types.Object);
            mv.newResumptionPoint();
            mv.store(received);

            /* step 7b (II) */
            mv.load(received);
            mv.instanceOf(Types.ScriptException);
            mv.ifeq(iteratorNext);
            {
                mv.load(received);
                mv.checkcast(Types.ScriptException);

                Label hasThrow = new Label();
                mv.loadExecutionContext();
                mv.load(iterator);
                mv.aconst("throw");
                mv.invoke(Methods.AbstractOperations_HasProperty);
                mv.ifeq(hasThrow);
                {
                    mv.invoke(Methods.ScriptException_getValue);
                    mv.store(received);
                    mv.goTo(iteratorThrow);
                }
                mv.mark(hasThrow);
                mv.athrow();
            }

            /* step 7e */
            mv.mark(done);
            mv.loadExecutionContext();
            mv.load(innerResult);
            mv.invoke(Methods.AbstractOperations_IteratorValue);

            mv.exitVariableScope();
        } else {
            // call runtime
            mv.loadExecutionContext();
            mv.invoke(Methods.ScriptRuntime_delegatedYield);
        }
    }

    /**
     * 14.4 Generator Function Definitions
     * <p>
     * 14.4.14 Runtime Semantics: Evaluation
     * <ul>
     * <li>YieldExpression : yield
     * <li>YieldExpression : yield AssignmentExpression
     * </ul>
     * <p>
     * stack: [value] -> [value]
     */
    protected final void yield(Expression node, ExpressionVisitor mv) {
        mv.lineInfo(node);
        if (!mv.hasSyntheticMethods() && mv.hasStack()) {
            mv.loadExecutionContext();
            mv.swap();
            mv.iconst(false);
            mv.invoke(Methods.AbstractOperations_CreateIterResultObject);

            // force stack top to Object-type
            mv.checkcast(Types.Object);
            mv.newResumptionPoint();

            // check for exception
            Label isException = new Label();
            mv.dup();
            mv.instanceOf(Types.ScriptException);
            mv.ifeq(isException);
            {
                mv.checkcast(Types.ScriptException);
                mv.athrow();
            }
            mv.mark(isException);
        } else {
            // call runtime
            mv.loadExecutionContext();
            mv.invoke(Methods.ScriptRuntime_yield);
        }
    }
}
