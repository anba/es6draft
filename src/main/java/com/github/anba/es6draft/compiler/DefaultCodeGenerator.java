/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static com.github.anba.es6draft.compiler.ClassPropertyGenerator.ClassPropertyEvaluation;
import static com.github.anba.es6draft.semantics.StaticSemantics.ConstructorMethod;

import java.util.EnumSet;

import com.github.anba.es6draft.ast.*;
import com.github.anba.es6draft.ast.AbruptNode.Abrupt;
import com.github.anba.es6draft.ast.scope.Name;
import com.github.anba.es6draft.compiler.CodeGenerator.FunctionName;
import com.github.anba.es6draft.compiler.assembler.FieldName;
import com.github.anba.es6draft.compiler.assembler.Jump;
import com.github.anba.es6draft.compiler.assembler.MethodName;
import com.github.anba.es6draft.compiler.assembler.Type;
import com.github.anba.es6draft.compiler.assembler.Variable;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.types.Null;
import com.github.anba.es6draft.runtime.types.Reference;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Undefined;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryConstructorFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * Abstract base class for specialised generators
 */
abstract class DefaultCodeGenerator<R, V extends ExpressionVisitor> extends
        DefaultNodeVisitor<R, V> {
    private static final class Fields {
        static final FieldName Double_NaN = FieldName.findStatic(Types.Double, "NaN",
                Type.DOUBLE_TYPE);
    }

    private static final class Methods {
        // class: AbstractOperations
        static final MethodName AbstractOperations_CreateIterResultObject = MethodName.findStatic(
                Types.AbstractOperations, "CreateIterResultObject", Type.methodType(
                        Types.OrdinaryObject, Types.ExecutionContext, Types.Object,
                        Type.BOOLEAN_TYPE));

        static final MethodName AbstractOperations_HasOwnProperty = MethodName.findStatic(
                Types.AbstractOperations, "HasOwnProperty", Type.methodType(Type.BOOLEAN_TYPE,
                        Types.ExecutionContext, Types.ScriptObject, Types.String));

        static final MethodName AbstractOperations_GetIterator = MethodName.findStatic(
                Types.AbstractOperations, "GetIterator",
                Type.methodType(Types.ScriptObject, Types.ExecutionContext, Types.Object));

        static final MethodName AbstractOperations_IteratorComplete = MethodName.findStatic(
                Types.AbstractOperations, "IteratorComplete",
                Type.methodType(Type.BOOLEAN_TYPE, Types.ExecutionContext, Types.ScriptObject));

        static final MethodName AbstractOperations_IteratorNext = MethodName.findStatic(
                Types.AbstractOperations, "IteratorNext", Type.methodType(Types.ScriptObject,
                        Types.ExecutionContext, Types.ScriptObject, Types.Object));

        static final MethodName AbstractOperations_IteratorValue = MethodName.findStatic(
                Types.AbstractOperations, "IteratorValue",
                Type.methodType(Types.Object, Types.ExecutionContext, Types.ScriptObject));

        static final MethodName AbstractOperations_ToPrimitive = MethodName.findStatic(
                Types.AbstractOperations, "ToPrimitive",
                Type.methodType(Types.Object, Types.ExecutionContext, Types.Object));

        static final MethodName AbstractOperations_ToBoolean = MethodName.findStatic(
                Types.AbstractOperations, "ToBoolean",
                Type.methodType(Type.BOOLEAN_TYPE, Types.Object));

        static final MethodName AbstractOperations_ToBoolean_double = MethodName.findStatic(
                Types.AbstractOperations, "ToBoolean",
                Type.methodType(Type.BOOLEAN_TYPE, Type.DOUBLE_TYPE));

        static final MethodName AbstractOperations_ToFlatString = MethodName.findStatic(
                Types.AbstractOperations, "ToFlatString",
                Type.methodType(Types.String, Types.ExecutionContext, Types.Object));

        static final MethodName AbstractOperations_ToNumber = MethodName.findStatic(
                Types.AbstractOperations, "ToNumber",
                Type.methodType(Type.DOUBLE_TYPE, Types.ExecutionContext, Types.Object));

        static final MethodName AbstractOperations_ToNumber_CharSequence = MethodName.findStatic(
                Types.AbstractOperations, "ToNumber",
                Type.methodType(Type.DOUBLE_TYPE, Types.CharSequence));

        static final MethodName AbstractOperations_ToInt32 = MethodName.findStatic(
                Types.AbstractOperations, "ToInt32",
                Type.methodType(Type.INT_TYPE, Types.ExecutionContext, Types.Object));

        static final MethodName AbstractOperations_ToInt32_double = MethodName.findStatic(
                Types.AbstractOperations, "ToInt32",
                Type.methodType(Type.INT_TYPE, Type.DOUBLE_TYPE));

        static final MethodName AbstractOperations_ToUint32 = MethodName.findStatic(
                Types.AbstractOperations, "ToUint32",
                Type.methodType(Type.LONG_TYPE, Types.ExecutionContext, Types.Object));

        static final MethodName AbstractOperations_ToUint32_double = MethodName.findStatic(
                Types.AbstractOperations, "ToUint32",
                Type.methodType(Type.LONG_TYPE, Type.DOUBLE_TYPE));

        static final MethodName AbstractOperations_ToObject = MethodName.findStatic(
                Types.AbstractOperations, "ToObject",
                Type.methodType(Types.ScriptObject, Types.ExecutionContext, Types.Object));

        static final MethodName AbstractOperations_ToPropertyKey = MethodName.findStatic(
                Types.AbstractOperations, "ToPropertyKey",
                Type.methodType(Types.Object, Types.ExecutionContext, Types.Object));

        static final MethodName AbstractOperations_ToString = MethodName.findStatic(
                Types.AbstractOperations, "ToString",
                Type.methodType(Types.CharSequence, Types.ExecutionContext, Types.Object));

        static final MethodName AbstractOperations_ToString_int = MethodName.findStatic(
                Types.AbstractOperations, "ToString", Type.methodType(Types.String, Type.INT_TYPE));

        static final MethodName AbstractOperations_ToString_long = MethodName
                .findStatic(Types.AbstractOperations, "ToString",
                        Type.methodType(Types.String, Type.LONG_TYPE));

        static final MethodName AbstractOperations_ToString_double = MethodName.findStatic(
                Types.AbstractOperations, "ToString",
                Type.methodType(Types.String, Type.DOUBLE_TYPE));

        // class: Boolean
        static final MethodName Boolean_toString = MethodName.findStatic(Types.Boolean, "toString",
                Type.methodType(Types.String, Type.BOOLEAN_TYPE));

        // class: CharSequence
        static final MethodName CharSequence_length = MethodName.findInterface(Types.CharSequence,
                "length", Type.methodType(Type.INT_TYPE));
        static final MethodName CharSequence_toString = MethodName.findInterface(
                Types.CharSequence, "toString", Type.methodType(Types.String));

        // class: EnvironmentRecord
        static final MethodName EnvironmentRecord_createImmutableBinding = MethodName
                .findInterface(Types.EnvironmentRecord, "createImmutableBinding",
                        Type.methodType(Type.VOID_TYPE, Types.String, Type.BOOLEAN_TYPE));

        static final MethodName EnvironmentRecord_initializeBinding = MethodName.findInterface(
                Types.EnvironmentRecord, "initializeBinding",
                Type.methodType(Type.VOID_TYPE, Types.String, Types.Object));

        // class: ExecutionContext
        static final MethodName ExecutionContext_getLexicalEnvironment = MethodName.findVirtual(
                Types.ExecutionContext, "getLexicalEnvironment",
                Type.methodType(Types.LexicalEnvironment));

        static final MethodName ExecutionContext_getLexicalEnvironmentRecord = MethodName
                .findVirtual(Types.ExecutionContext, "getLexicalEnvironmentRecord",
                        Type.methodType(Types.EnvironmentRecord));

        static final MethodName ExecutionContext_pushLexicalEnvironment = MethodName.findVirtual(
                Types.ExecutionContext, "pushLexicalEnvironment",
                Type.methodType(Type.VOID_TYPE, Types.LexicalEnvironment));

        static final MethodName ExecutionContext_popLexicalEnvironment = MethodName.findVirtual(
                Types.ExecutionContext, "popLexicalEnvironment", Type.methodType(Type.VOID_TYPE));

        static final MethodName ExecutionContext_replaceLexicalEnvironment = MethodName
                .findVirtual(Types.ExecutionContext, "replaceLexicalEnvironment",
                        Type.methodType(Type.VOID_TYPE, Types.LexicalEnvironment));

        static final MethodName ExecutionContext_restoreLexicalEnvironment = MethodName
                .findVirtual(Types.ExecutionContext, "restoreLexicalEnvironment",
                        Type.methodType(Type.VOID_TYPE, Types.LexicalEnvironment));

        // class: LexicalEnvironment
        static final MethodName LexicalEnvironment_getEnvRec = MethodName.findVirtual(
                Types.LexicalEnvironment, "getEnvRec", Type.methodType(Types.EnvironmentRecord));

        static final MethodName LexicalEnvironment_cloneDeclarativeEnvironment = MethodName
                .findStatic(Types.LexicalEnvironment, "cloneDeclarativeEnvironment",
                        Type.methodType(Types.LexicalEnvironment, Types.LexicalEnvironment));

        static final MethodName LexicalEnvironment_newDeclarativeEnvironment = MethodName
                .findStatic(Types.LexicalEnvironment, "newDeclarativeEnvironment",
                        Type.methodType(Types.LexicalEnvironment, Types.LexicalEnvironment));

        static final MethodName LexicalEnvironment_newCatchDeclarativeEnvironment = MethodName
                .findStatic(Types.LexicalEnvironment, "newCatchDeclarativeEnvironment",
                        Type.methodType(Types.LexicalEnvironment, Types.LexicalEnvironment));

        static final MethodName LexicalEnvironment_newObjectEnvironment = MethodName.findStatic(
                Types.LexicalEnvironment, "newObjectEnvironment", Type.methodType(
                        Types.LexicalEnvironment, Types.ScriptObject, Types.LexicalEnvironment,
                        Type.BOOLEAN_TYPE));

        // class: OrdinaryFunction
        static final MethodName OrdinaryFunction_SetFunctionName_String = MethodName.findStatic(
                Types.OrdinaryFunction, "SetFunctionName",
                Type.methodType(Type.VOID_TYPE, Types.OrdinaryObject, Types.String));

        static final MethodName OrdinaryFunction_SetFunctionName_Symbol = MethodName.findStatic(
                Types.OrdinaryFunction, "SetFunctionName",
                Type.methodType(Type.VOID_TYPE, Types.OrdinaryObject, Types.Symbol));

        // class: ReturnValue
        static final MethodName ReturnValue_getValue = MethodName.findVirtual(Types.ReturnValue,
                "getValue", Type.methodType(Types.Object));

        // class: ScriptRuntime
        static final MethodName ScriptRuntime_delegatedYield = MethodName.findStatic(
                Types.ScriptRuntime, "delegatedYield",
                Type.methodType(Types.Object, Types.Object, Types.ExecutionContext));

        static final MethodName ScriptRuntime_EvaluateConstructorMethod = MethodName.findStatic(
                Types.ScriptRuntime, "EvaluateConstructorMethod", Type.methodType(
                        Types.OrdinaryConstructorFunction, Types.ScriptObject,
                        Types.OrdinaryObject, Types.RuntimeInfo$Function, Type.BOOLEAN_TYPE,
                        Types.ExecutionContext));

        static final MethodName ScriptRuntime_getClassProto = MethodName.findStatic(
                Types.ScriptRuntime, "getClassProto",
                Type.methodType(Types.ScriptObject_, Types.Object, Types.ExecutionContext));

        static final MethodName ScriptRuntime_getClassProto_Null = MethodName.findStatic(
                Types.ScriptRuntime, "getClassProto",
                Type.methodType(Types.ScriptObject_, Types.ExecutionContext));

        static final MethodName ScriptRuntime_getDefaultClassProto = MethodName.findStatic(
                Types.ScriptRuntime, "getDefaultClassProto",
                Type.methodType(Types.ScriptObject_, Types.ExecutionContext));

        static final MethodName ScriptRuntime_yield = MethodName.findStatic(Types.ScriptRuntime,
                "yield", Type.methodType(Types.Object, Types.Object, Types.ExecutionContext));

        static final MethodName ScriptRuntime_yieldThrowCompletion = MethodName.findStatic(
                Types.ScriptRuntime, "yieldThrowCompletion", Type.methodType(Types.ScriptObject,
                        Types.ExecutionContext, Types.ScriptObject, Types.ScriptException));

        static final MethodName ScriptRuntime_yieldReturnCompletion = MethodName.findStatic(
                Types.ScriptRuntime, "yieldReturnCompletion", Type.methodType(Types.ScriptObject,
                        Types.ExecutionContext, Types.ScriptObject, Types.ReturnValue));

        // class: Type
        static final MethodName Type_isUndefinedOrNull = MethodName.findStatic(Types._Type,
                "isUndefinedOrNull", Type.methodType(Type.BOOLEAN_TYPE, Types.Object));
    }

    protected final CodeGenerator codegen;

    protected DefaultCodeGenerator(CodeGenerator codegen) {
        this.codegen = codegen;
    }

    /**
     * stack: [] {@literal ->} [value|reference]
     * 
     * @param node
     *            the expression node
     * @param mv
     *            the expression visitor
     * @return the value type returned by the expression
     */
    protected final ValType expression(Expression node, ExpressionVisitor mv) {
        return codegen.expression(node, mv);
    }

    /**
     * stack: [] {@literal ->} [value]
     * 
     * @param node
     *            the expression node
     * @param mv
     *            the expression visitor
     * @return the value type returned by the expression
     */
    protected final ValType expressionValue(Expression node, ExpressionVisitor mv) {
        return codegen.expressionValue(node, mv);
    }

    /**
     * stack: [] {@literal ->} [boxed(value)]
     * 
     * @param node
     *            the expression node
     * @param mv
     *            the expression visitor
     * @return the value type returned by the expression
     */
    protected final ValType expressionBoxedValue(Expression node, ExpressionVisitor mv) {
        return codegen.expressionBoxedValue(node, mv);
    }

    /**
     * stack: [] {@literal ->} []
     * 
     * @param node
     *            the abrupt node
     * @param mv
     *            the statement visitor
     * @return the variable holding the saved environment or {@code null}
     */
    protected final Variable<LexicalEnvironment<?>> saveEnvironment(AbruptNode node,
            StatementVisitor mv) {
        EnumSet<Abrupt> abrupt = node.getAbrupt();
        if (abrupt.contains(Abrupt.Break) || abrupt.contains(Abrupt.Continue)) {
            return saveEnvironment(mv);
        }
        return null;
    }

    /**
     * stack: [] {@literal ->} []
     * 
     * @param mv
     *            the statement visitor
     * @return the variable holding the saved environment
     */
    protected final Variable<LexicalEnvironment<?>> saveEnvironment(StatementVisitor mv) {
        Variable<LexicalEnvironment<?>> savedEnv = mv.newVariable("savedEnv",
                LexicalEnvironment.class).uncheckedCast();
        saveEnvironment(savedEnv, mv);
        return savedEnv;
    }

    /**
     * stack: [] {@literal ->} []
     * 
     * @param savedEnv
     *            the variable to hold the saved environment
     * @param mv
     *            the statement visitor
     */
    protected final void saveEnvironment(Variable<LexicalEnvironment<?>> savedEnv,
            StatementVisitor mv) {
        getLexicalEnvironment(mv);
        mv.store(savedEnv);
    }

    /**
     * stack: [] {@literal ->} []
     * 
     * @param node
     *            the abrupt node
     * @param abrupt
     *            the abrupt completion type
     * @param savedEnv
     *            the variable which holds the saved environment
     * @param mv
     *            the statement visitor
     */
    protected final void restoreEnvironment(AbruptNode node, Abrupt abrupt,
            Variable<LexicalEnvironment<?>> savedEnv, StatementVisitor mv) {
        assert node.getAbrupt().contains(abrupt);
        assert savedEnv != null;
        restoreEnvironment(savedEnv, mv);
    }

    /**
     * stack: [] {@literal ->} []
     * 
     * @param savedEnv
     *            the variable which holds the saved environment
     * @param mv
     *            the statement visitor
     */
    protected final void restoreEnvironment(Variable<LexicalEnvironment<?>> savedEnv,
            StatementVisitor mv) {
        mv.loadExecutionContext();
        mv.load(savedEnv);
        mv.invoke(Methods.ExecutionContext_restoreLexicalEnvironment);
    }

    /**
     * stack: [] {@literal ->} []
     * 
     * @param savedEnv
     *            the variable which holds the saved environment
     * @param mv
     *            the statement visitor
     */
    protected final void replaceLexicalEnvironment(Variable<LexicalEnvironment<?>> savedEnv,
            StatementVisitor mv) {
        mv.loadExecutionContext();
        mv.load(savedEnv);
        mv.invoke(Methods.ExecutionContext_replaceLexicalEnvironment);
    }

    /**
     * stack: [] {@literal ->} [lexEnv]
     * 
     * @param mv
     *            the expression visitor
     */
    protected final void getLexicalEnvironment(ExpressionVisitor mv) {
        mv.loadExecutionContext();
        mv.invoke(Methods.ExecutionContext_getLexicalEnvironment);
    }

    /**
     * stack: [] {@literal ->} [envRec]
     * 
     * @param mv
     *            the expression visitor
     */
    protected final void getEnvironmentRecord(ExpressionVisitor mv) {
        mv.loadExecutionContext();
        mv.invoke(Methods.ExecutionContext_getLexicalEnvironmentRecord);
    }

    /**
     * Creates a new object environment.
     * <p>
     * stack: [obj] {@literal ->} [lexEnv]
     * 
     * @param mv
     *            the expression visitor
     * @param withEnvironment
     *            the withEnvironment flag
     */
    protected final void newObjectEnvironment(ExpressionVisitor mv, boolean withEnvironment) {
        mv.loadExecutionContext();
        mv.invoke(Methods.ExecutionContext_getLexicalEnvironment);
        mv.iconst(withEnvironment);
        mv.invoke(Methods.LexicalEnvironment_newObjectEnvironment);
    }

    /**
     * Creates a new declarative environment.
     * <p>
     * stack: [] {@literal ->} [lexEnv]
     * 
     * @param mv
     *            the expression visitor
     */
    protected final void newDeclarativeEnvironment(ExpressionVisitor mv) {
        mv.loadExecutionContext();
        mv.invoke(Methods.ExecutionContext_getLexicalEnvironment);
        mv.invoke(Methods.LexicalEnvironment_newDeclarativeEnvironment);
    }

    /**
     * Creates a new declarative environment for a {@code Catch} clause.
     * <p>
     * stack: [] {@literal ->} [lexEnv]
     * 
     * @param mv
     *            the expression visitor
     */
    protected final void newCatchDeclarativeEnvironment(ExpressionVisitor mv) {
        mv.loadExecutionContext();
        mv.invoke(Methods.ExecutionContext_getLexicalEnvironment);
        mv.invoke(Methods.LexicalEnvironment_newCatchDeclarativeEnvironment);
    }

    /**
     * stack: [] {@literal ->} [lexEnv]
     * 
     * @param mv
     *            the expression visitor
     */
    protected final void cloneDeclarativeEnvironment(ExpressionVisitor mv) {
        mv.loadExecutionContext();
        mv.invoke(Methods.ExecutionContext_getLexicalEnvironment);
        mv.invoke(Methods.LexicalEnvironment_cloneDeclarativeEnvironment);
    }

    /**
     * stack: [lexEnv] {@literal ->} []
     * 
     * @param mv
     *            the expression visitor
     */
    protected final void pushLexicalEnvironment(ExpressionVisitor mv) {
        mv.loadExecutionContext();
        mv.swap();
        mv.invoke(Methods.ExecutionContext_pushLexicalEnvironment);
    }

    /**
     * Restores the previous lexical environment.
     * <p>
     * stack: [] {@literal ->} []
     * 
     * @param mv
     *            the expression visitor
     */
    protected final void popLexicalEnvironment(ExpressionVisitor mv) {
        mv.loadExecutionContext();
        mv.invoke(Methods.ExecutionContext_popLexicalEnvironment);
    }

    /**
     * stack: [object] {@literal ->} [boolean]
     * 
     * @param mv
     *            the expression visitor
     */
    protected final void isUndefinedOrNull(ExpressionVisitor mv) {
        mv.invoke(Methods.Type_isUndefinedOrNull);
    }

    enum ValType {
        Undefined, Null, Boolean, Number, Number_int, Number_uint, String, Object, Reference, Any,
        Empty;

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
                return 1;
            case Empty:
            default:
                return 0;
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
            case Empty:
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
            case Empty:
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
            case Empty:
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
                return Object.class;
            case Empty:
            default:
                throw new AssertionError();
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
                return Types.Object;
            case Empty:
            default:
                throw new AssertionError();
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
                return Types.Object;
            case Empty:
            default:
                throw new AssertionError();
            }
        }
    }

    /**
     * stack: [Object] {@literal ->} [boolean]
     * 
     * @param from
     *            the input value type
     * @param mv
     *            the expression visitor
     * @return the returned value type
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
            mv.loadExecutionContext();
            mv.swap();
            mv.invoke(Methods.AbstractOperations_ToPrimitive);
            return ValType.Any;
        case Empty:
        case Reference:
        default:
            throw new AssertionError();
        }
    }

    /**
     * stack: [Object] {@literal ->} [boolean]
     * 
     * @param from
     *            the input value type
     * @param mv
     *            the expression visitor
     */
    protected final void ToBoolean(ValType from, ExpressionVisitor mv) {
        switch (from) {
        case Number:
            mv.invoke(Methods.AbstractOperations_ToBoolean_double);
            return;
        case Number_int:
            mv.i2d();
            mv.invoke(Methods.AbstractOperations_ToBoolean_double);
            return;
        case Number_uint:
            mv.l2d();
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
            Jump l0 = new Jump(), l1 = new Jump();
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
            mv.invoke(Methods.AbstractOperations_ToBoolean);
            return;
        case Empty:
        case Reference:
        default:
            throw new AssertionError();
        }
    }

    /**
     * stack: [Object] {@literal ->} [double]
     * 
     * @param from
     *            the input value type
     * @param mv
     *            the expression visitor
     */
    protected final void ToNumber(ValType from, ExpressionVisitor mv) {
        switch (from) {
        case Number:
            return;
        case Number_int:
            mv.i2d();
            return;
        case Number_uint:
            mv.l2d();
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
            mv.i2d();
            return;
        case String:
            mv.invoke(Methods.AbstractOperations_ToNumber_CharSequence);
            return;
        case Object:
        case Any:
            mv.loadExecutionContext();
            mv.swap();
            mv.invoke(Methods.AbstractOperations_ToNumber);
            return;
        case Empty:
        case Reference:
        default:
            throw new AssertionError();
        }
    }

    /**
     * stack: [Object] {@literal ->} [int]
     * 
     * @param from
     *            the input value type
     * @param mv
     *            the expression visitor
     */
    protected final void ToInt32(ValType from, ExpressionVisitor mv) {
        switch (from) {
        case Number:
            mv.invoke(Methods.AbstractOperations_ToInt32_double);
            return;
        case Number_int:
            return;
        case Number_uint:
            mv.l2i();
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
            mv.loadExecutionContext();
            mv.swap();
            mv.invoke(Methods.AbstractOperations_ToInt32);
            return;
        case Empty:
        case Reference:
        default:
            throw new AssertionError();
        }
    }

    /**
     * stack: [Object] {@literal ->} [long]
     * 
     * @param from
     *            the input value type
     * @param mv
     *            the expression visitor
     */
    protected final void ToUint32(ValType from, ExpressionVisitor mv) {
        switch (from) {
        case Number:
            mv.invoke(Methods.AbstractOperations_ToUint32_double);
            return;
        case Number_int:
            mv.i2l();
            mv.lconst(0xffff_ffffL);
            mv.land();
            return;
        case Number_uint:
            return;
        case Undefined:
        case Null:
            mv.pop();
            mv.lconst(0);
            return;
        case Boolean:
            mv.i2l();
            return;
        case String:
            mv.invoke(Methods.AbstractOperations_ToNumber_CharSequence);
            mv.invoke(Methods.AbstractOperations_ToUint32_double);
            return;
        case Object:
        case Any:
            mv.loadExecutionContext();
            mv.swap();
            mv.invoke(Methods.AbstractOperations_ToUint32);
            return;
        case Empty:
        case Reference:
        default:
            throw new AssertionError();
        }
    }

    /**
     * stack: [Object] {@literal ->} [CharSequence]
     * 
     * @param from
     *            the input value type
     * @param mv
     *            the expression visitor
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
            mv.loadExecutionContext();
            mv.swap();
            mv.invoke(Methods.AbstractOperations_ToString);
            return;
        case Empty:
        case Reference:
        default:
            throw new AssertionError();
        }
    }

    /**
     * stack: [Object] {@literal ->} [String]
     * 
     * @param from
     *            the input value type
     * @param mv
     *            the expression visitor
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
            mv.loadExecutionContext();
            mv.swap();
            mv.invoke(Methods.AbstractOperations_ToFlatString);
            return;
        case Empty:
        case Reference:
        default:
            throw new AssertionError();
        }
    }

    /**
     * stack: [Object] {@literal ->} [ScriptObject]
     * 
     * @param from
     *            the input value type
     * @param mv
     *            the expression visitor
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
            break;
        case Empty:
        case Reference:
        default:
            throw new AssertionError();
        }

        mv.loadExecutionContext();
        mv.swap();
        mv.invoke(Methods.AbstractOperations_ToObject);
    }

    /**
     * stack: [Object] {@literal ->} [ScriptObject]
     * 
     * @param node
     *            the current node
     * @param from
     *            the input value type
     * @param mv
     *            the expression visitor
     */
    protected final void ToObject(Node node, ValType from, ExpressionVisitor mv) {
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
            break;
        case Empty:
        case Reference:
        default:
            throw new AssertionError();
        }

        mv.lineInfo(node);
        mv.loadExecutionContext();
        mv.swap();
        mv.invoke(Methods.AbstractOperations_ToObject);
    }

    /**
     * stack: [Object] {@literal ->} [String|Symbol]
     * 
     * @param from
     *            the input value type
     * @param mv
     *            the expression visitor
     */
    protected static final ValType ToPropertyKey(ValType from, ExpressionVisitor mv) {
        switch (from) {
        case Number:
            mv.invoke(Methods.AbstractOperations_ToString_double);
            return ValType.String;
        case Number_int:
            mv.invoke(Methods.AbstractOperations_ToString_int);
            return ValType.String;
        case Number_uint:
            mv.invoke(Methods.AbstractOperations_ToString_long);
            return ValType.String;
        case Undefined:
            mv.pop();
            mv.aconst("undefined");
            return ValType.String;
        case Null:
            mv.pop();
            mv.aconst("null");
            return ValType.String;
        case Boolean:
            mv.invoke(Methods.Boolean_toString);
            return ValType.String;
        case String:
            mv.invoke(Methods.CharSequence_toString);
            return ValType.String;
        case Object:
            mv.loadExecutionContext();
            mv.swap();
            mv.invoke(Methods.AbstractOperations_ToFlatString);
            return ValType.String;
        case Any:
            mv.loadExecutionContext();
            mv.swap();
            mv.invoke(Methods.AbstractOperations_ToPropertyKey);
            return ValType.Any;
        case Empty:
        case Reference:
        default:
            throw new AssertionError();
        }
    }

    /**
     * stack: [propertyKey, function] {@literal ->} [propertyKey, function]
     * 
     * @param node
     *            the function or class node
     * @param propertyKeyType
     *            the property key value type
     * @param mv
     *            the expression visitor
     */
    protected static void SetFunctionName(Node node, ValType propertyKeyType, ExpressionVisitor mv) {
        assert node instanceof ClassDefinition || node instanceof FunctionNode : node.getClass();

        Jump hasOwnName = null;
        if (node instanceof ClassDefinition && hasOwnNameProperty((ClassDefinition) node)) {
            hasOwnName = new Jump();
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
            Jump isString = new Jump(), afterSetFunctionName = new Jump();
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
     * stack: [function] {@literal ->} [function]
     * 
     * @param node
     *            the function or class node
     * @param name
     *            the new function name
     * @param mv
     *            the expression visitor
     */
    protected static void SetFunctionName(Node node, Name name, ExpressionVisitor mv) {
        SetFunctionName(node, name.getIdentifier(), mv);
    }

    /**
     * stack: [function] {@literal ->} [function]
     * 
     * @param node
     *            the function or class node
     * @param name
     *            the new function name
     * @param mv
     *            the expression visitor
     */
    protected static void SetFunctionName(Node node, String name, ExpressionVisitor mv) {
        assert node instanceof ClassDefinition || node instanceof FunctionNode : node.getClass();

        Jump hasOwnName = null;
        if (node instanceof ClassDefinition && hasOwnNameProperty((ClassDefinition) node)) {
            hasOwnName = new Jump();

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

    private static boolean hasOwnNameProperty(ClassDefinition node) {
        for (MethodDefinition methodDefinition : node.getMethods()) {
            if (methodDefinition.isStatic()) {
                String methodName = methodDefinition.getPropertyName().getName();
                if (methodName == null || "name".equals(methodName)) {
                    // Computed property name or method name is "name"
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * stack: [value] {@literal ->} []
     * 
     * @param node
     *            the binding node
     * @param mv
     *            the expression visitor
     */
    protected final void BindingInitialization(Binding node, ExpressionVisitor mv) {
        BindingInitializationGenerator.BindingInitialization(codegen, node, mv);
    }

    /**
     * stack: [envRec, value] {@literal ->} []
     * 
     * @param node
     *            the binding node
     * @param mv
     *            the expression visitor
     */
    protected final void BindingInitializationWithEnvironment(Binding node, ExpressionVisitor mv) {
        BindingInitializationGenerator.BindingInitializationWithEnvironment(codegen, node, mv);
    }

    /**
     * stack: [envRec, value] {@literal ->} []
     * 
     * @param name
     *            the binding name
     * @param mv
     *            the expression visitor
     */
    protected final void BindingInitializationWithEnvironment(Name name, ExpressionVisitor mv) {
        BindingInitializationGenerator.BindingInitializationWithEnvironment(codegen, name, mv);
    }

    /**
     * stack: [value] {@literal ->} []
     * 
     * @param node
     *            the assignment pattern node
     * @param mv
     *            the expression visitor
     */
    protected final void DestructuringAssignment(AssignmentPattern node, ExpressionVisitor mv) {
        DestructuringAssignmentGenerator.DestructuringAssignment(codegen, node, mv);
    }

    /**
     * 14.5.14 Runtime Semantics: ClassDefinitionEvaluation
     * 
     * @param def
     *            the class definition node
     * @param className
     *            the class name or {@code null} if not present
     * @param mv
     *            the expression visitor
     */
    protected final void ClassDefinitionEvaluation(ClassDefinition def, String className,
            ExpressionVisitor mv) {
        mv.enterClassDefinition();
        mv.enterVariableScope();

        // step 1 (not applicable)
        // steps 2-4
        assert (def.getScope() != null && def.getScope().isPresent()) == (className != null);
        if (className != null) {
            // stack: [] -> [classScope]
            newDeclarativeEnvironment(mv);

            // stack: [classScope] -> [classScope]
            mv.dup();
            mv.invoke(Methods.LexicalEnvironment_getEnvRec);
            mv.aconst(className);
            mv.iconst(true);
            mv.invoke(Methods.EnvironmentRecord_createImmutableBinding);

            // stack: [classScope] -> []
            pushLexicalEnvironment(mv);
            mv.enterScope(def);
        }

        // steps 5-7
        // stack: [] -> [<constructorParent,proto>]
        Expression classHeritage = def.getHeritage();
        if (classHeritage == null) {
            mv.loadExecutionContext();
            mv.invoke(Methods.ScriptRuntime_getDefaultClassProto);
        } else if (classHeritage instanceof NullLiteral) {
            mv.loadExecutionContext();
            mv.invoke(Methods.ScriptRuntime_getClassProto_Null);
        } else {
            expressionBoxedValue(classHeritage, mv);
            mv.loadExecutionContext();
            mv.lineInfo(def);
            mv.invoke(Methods.ScriptRuntime_getClassProto);
        }

        // stack: [<constructorParent,proto>] -> [<constructorParent,proto>]
        Variable<OrdinaryObject> proto = mv.newVariable("proto", OrdinaryObject.class);
        mv.dup();
        mv.aload(1, Types.ScriptObject);
        mv.checkcast(Types.OrdinaryObject);
        mv.store(proto);

        // stack: [<constructorParent,proto>] -> [constructorParent, proto]
        mv.aload(0, Types.ScriptObject);
        mv.load(proto);

        // steps 8-9
        // stack: [constructorParent, proto] -> [constructorParent, proto, <rti>]
        MethodDefinition constructor = ConstructorMethod(def);
        assert constructor != null;
        codegen.compile(constructor);
        // Runtime Semantics: Evaluation -> MethodDefinition
        mv.invoke(codegen.methodDesc(constructor, FunctionName.RTI));

        // step 10 (not applicable)
        // steps 11-18
        // stack: [constructorParent, proto, <rti>] -> [F]
        mv.iconst(classHeritage != null);
        mv.loadExecutionContext();
        mv.lineInfo(def);
        mv.invoke(Methods.ScriptRuntime_EvaluateConstructorMethod);

        // stack: [F] -> []
        Variable<OrdinaryConstructorFunction> F = mv.newVariable("F",
                OrdinaryConstructorFunction.class);
        mv.store(F);

        // steps 19-21
        ClassPropertyEvaluation(codegen, def.getProperties(), F, proto, mv);

        // step 23 (moved)
        if (className != null) {
            // stack: [] -> [envRec, name, F]
            getEnvironmentRecord(mv);
            mv.aconst(className);
            mv.load(F);

            // stack: [envRec, name, F] -> []
            mv.invoke(Methods.EnvironmentRecord_initializeBinding);
        }

        // step 22
        if (className != null) {
            mv.exitScope();
            popLexicalEnvironment(mv);
        }

        // stack: [] -> [F]
        mv.load(F);

        mv.exitVariableScope();

        // step 24 (return F)
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
     * stack: [value] {@literal ->} [value']
     * 
     * @param node
     *            the expression node
     * @param mv
     *            the expression visitor
     */
    protected final void delegatedYield(Expression node, ExpressionVisitor mv) {
        mv.lineInfo(node);
        if (mv.isResumable() && !codegen.isEnabled(Compiler.Option.NoResume)) {
            assert mv.hasStack();
            Jump iteratorNext = new Jump();
            Jump generatorYield = new Jump();
            Jump generatorYieldOrReturn = new Jump();
            Jump done = new Jump();

            mv.enterVariableScope();
            Variable<ScriptObject> iterator = mv.newVariable("iterator", ScriptObject.class);
            Variable<ScriptObject> innerResult = mv.newVariable("innerResult", ScriptObject.class);
            Variable<Object> received = mv.newVariable("received", Object.class);

            /* steps 3-4 */
            // stack: [value] -> []
            mv.loadExecutionContext();
            mv.swap();
            mv.invoke(Methods.AbstractOperations_GetIterator);
            mv.store(iterator);

            /* step 5 */
            // stack: [] -> []
            mv.loadUndefined();
            mv.store(received);

            /* step 6.a.i-6.a.ii */
            // stack: [] -> []
            mv.mark(iteratorNext);
            mv.loadExecutionContext();
            mv.load(iterator);
            mv.load(received);
            mv.invoke(Methods.AbstractOperations_IteratorNext);
            mv.store(innerResult);

            /* steps 6.a.iii-6.a.v */
            // stack: [] -> []
            mv.loadExecutionContext();
            mv.load(innerResult);
            mv.invoke(Methods.AbstractOperations_IteratorComplete);
            mv.ifne(done);

            /* step 6.a.vi */
            // stack: [] -> [Object(innerResult)]
            // force stack top to Object-type
            mv.mark(generatorYield);
            mv.load(innerResult);
            mv.checkcast(Types.Object);
            mv.newResumptionPoint();
            mv.store(received);

            /* step 6.b */
            Jump isException = new Jump();
            mv.load(received);
            mv.instanceOf(Types.ScriptException);
            mv.ifeq(isException);
            {
                /* steps 6.b.iii.1-4, 6.b.iv */
                mv.loadExecutionContext();
                mv.load(iterator);
                mv.load(received);
                mv.checkcast(Types.ScriptException);
                mv.invoke(Methods.ScriptRuntime_yieldThrowCompletion);
                mv.store(innerResult);

                mv.goTo(generatorYieldOrReturn);
            }
            mv.mark(isException);

            /* step 6.c */
            mv.load(received);
            mv.instanceOf(Types.ReturnValue);
            mv.ifeq(iteratorNext);
            {
                /* steps 6.c.i-vii */
                mv.loadExecutionContext();
                mv.load(iterator);
                mv.load(received);
                mv.checkcast(Types.ReturnValue);
                mv.invoke(Methods.ScriptRuntime_yieldReturnCompletion);
                mv.store(innerResult);

                mv.load(innerResult);
                mv.ifnonnull(generatorYieldOrReturn);
                {
                    /* step 6.c.iv */
                    mv.load(received);
                    mv.checkcast(Types.ReturnValue);
                    mv.invoke(Methods.ReturnValue_getValue);
                    popStackAndReturn(mv);
                }
            }

            mv.mark(generatorYieldOrReturn);

            /* steps 6.b.iii.5-6, 6.c.viii-ix */
            mv.loadExecutionContext();
            mv.load(innerResult);
            mv.invoke(Methods.AbstractOperations_IteratorComplete);
            mv.ifeq(generatorYield);

            /* step 6.b.iii.7, 6.c.x */
            mv.loadExecutionContext();
            mv.load(innerResult);
            mv.invoke(Methods.AbstractOperations_IteratorValue);
            popStackAndReturn(mv);

            /* step 6.a.v */
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
     * stack: [value] {@literal ->} [value']
     * 
     * @param node
     *            the expression node
     * @param mv
     *            the expression visitor
     */
    protected final void yield(Expression node, ExpressionVisitor mv) {
        mv.lineInfo(node);
        if (mv.isResumable() && !codegen.isEnabled(Compiler.Option.NoResume)) {
            assert mv.hasStack();
            mv.loadExecutionContext();
            mv.swap();
            mv.iconst(false);
            mv.invoke(Methods.AbstractOperations_CreateIterResultObject);

            // force stack top to Object-type
            mv.checkcast(Types.Object);
            mv.newResumptionPoint();

            // check for exception
            Jump isException = new Jump();
            mv.dup();
            mv.instanceOf(Types.ScriptException);
            mv.ifeq(isException);
            {
                mv.checkcast(Types.ScriptException);
                mv.athrow();
            }
            mv.mark(isException);

            // check for return value
            Jump isReturn = new Jump();
            mv.dup();
            mv.instanceOf(Types.ReturnValue);
            mv.ifeq(isReturn);
            {
                mv.checkcast(Types.ReturnValue);
                mv.invoke(Methods.ReturnValue_getValue);
                popStackAndReturn(mv);
            }
            mv.mark(isReturn);
        } else {
            // call runtime
            mv.loadExecutionContext();
            mv.invoke(Methods.ScriptRuntime_yield);
        }
    }

    private void popStackAndReturn(ExpressionVisitor mv) {
        // stack: [..., returnValue] -> [returnValue]
        Type[] stack = mv.getStack();
        assert stack.length != 0 && stack[stack.length - 1].equals(Types.Object);
        if (stack.length > 1) {
            // pop all remaining entries from stack before emitting return instruction
            mv.enterVariableScope();
            Variable<Object> returnValue = mv.newVariable("returnValue", Object.class);
            mv.store(returnValue);
            for (int i = stack.length - 2; i >= 0; --i) {
                mv.pop(stack[i]);
            }
            mv.load(returnValue);
            mv.exitVariableScope();
        }
        mv.returnCompletion();
    }
}
