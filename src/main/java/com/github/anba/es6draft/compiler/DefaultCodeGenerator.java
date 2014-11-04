/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static com.github.anba.es6draft.compiler.ClassPropertyGenerator.ClassPropertyEvaluation;
import static com.github.anba.es6draft.semantics.StaticSemantics.ConstructorMethod;

import java.util.EnumSet;

import org.objectweb.asm.Type;

import com.github.anba.es6draft.ast.*;
import com.github.anba.es6draft.ast.AbruptNode.Abrupt;
import com.github.anba.es6draft.ast.scope.Name;
import com.github.anba.es6draft.compiler.CodeGenerator.FunctionName;
import com.github.anba.es6draft.compiler.assembler.FieldDesc;
import com.github.anba.es6draft.compiler.assembler.Jump;
import com.github.anba.es6draft.compiler.assembler.MethodDesc;
import com.github.anba.es6draft.compiler.assembler.Variable;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.types.Null;
import com.github.anba.es6draft.runtime.types.Reference;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Undefined;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * Abstract base class for specialised generators
 */
abstract class DefaultCodeGenerator<R, V extends ExpressionVisitor> extends
        DefaultNodeVisitor<R, V> {
    private static final class Fields {
        static final FieldDesc Double_NaN = FieldDesc.create(FieldDesc.Allocation.Static,
                Types.Double, "NaN", Type.DOUBLE_TYPE);
    }

    private static final class Methods {
        // class: AbstractOperations
        static final MethodDesc AbstractOperations_CreateIterResultObject = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.AbstractOperations, "CreateIterResultObject", Type
                        .getMethodType(Types.OrdinaryObject, Types.ExecutionContext, Types.Object,
                                Type.BOOLEAN_TYPE));

        static final MethodDesc AbstractOperations_HasOwnProperty = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.AbstractOperations, "HasOwnProperty", Type
                        .getMethodType(Type.BOOLEAN_TYPE, Types.ExecutionContext,
                                Types.ScriptObject, Types.String));

        static final MethodDesc AbstractOperations_HasProperty = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.AbstractOperations, "HasProperty", Type
                        .getMethodType(Type.BOOLEAN_TYPE, Types.ExecutionContext,
                                Types.ScriptObject, Types.String));

        static final MethodDesc AbstractOperations_IteratorComplete = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.AbstractOperations, "IteratorComplete",
                Type.getMethodType(Type.BOOLEAN_TYPE, Types.ExecutionContext, Types.ScriptObject));

        static final MethodDesc AbstractOperations_IteratorNext = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.AbstractOperations, "IteratorNext", Type
                        .getMethodType(Types.ScriptObject, Types.ExecutionContext,
                                Types.ScriptObject, Types.Object));

        static final MethodDesc AbstractOperations_IteratorReturn = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.AbstractOperations, "IteratorReturn", Type
                        .getMethodType(Types.Object, Types.ExecutionContext, Types.ScriptObject,
                                Types.Object));

        static final MethodDesc AbstractOperations_IteratorThrow = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.AbstractOperations, "IteratorThrow", Type
                        .getMethodType(Type.VOID_TYPE, Types.ExecutionContext, Types.ScriptObject,
                                Types.Object));

        static final MethodDesc AbstractOperations_IteratorValue = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.AbstractOperations, "IteratorValue",
                Type.getMethodType(Types.Object, Types.ExecutionContext, Types.ScriptObject));

        static final MethodDesc AbstractOperations_ToPrimitive = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.AbstractOperations, "ToPrimitive",
                Type.getMethodType(Types.Object, Types.ExecutionContext, Types.Object));

        static final MethodDesc AbstractOperations_ToBoolean = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.AbstractOperations, "ToBoolean",
                Type.getMethodType(Type.BOOLEAN_TYPE, Types.Object));

        static final MethodDesc AbstractOperations_ToBoolean_double = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.AbstractOperations, "ToBoolean",
                Type.getMethodType(Type.BOOLEAN_TYPE, Type.DOUBLE_TYPE));

        static final MethodDesc AbstractOperations_ToFlatString = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.AbstractOperations, "ToFlatString",
                Type.getMethodType(Types.String, Types.ExecutionContext, Types.Object));

        static final MethodDesc AbstractOperations_ToNumber = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.AbstractOperations, "ToNumber",
                Type.getMethodType(Type.DOUBLE_TYPE, Types.ExecutionContext, Types.Object));

        static final MethodDesc AbstractOperations_ToNumber_CharSequence = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.AbstractOperations, "ToNumber",
                Type.getMethodType(Type.DOUBLE_TYPE, Types.CharSequence));

        static final MethodDesc AbstractOperations_ToInt32 = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.AbstractOperations, "ToInt32",
                Type.getMethodType(Type.INT_TYPE, Types.ExecutionContext, Types.Object));

        static final MethodDesc AbstractOperations_ToInt32_double = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.AbstractOperations, "ToInt32",
                Type.getMethodType(Type.INT_TYPE, Type.DOUBLE_TYPE));

        static final MethodDesc AbstractOperations_ToUint32 = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.AbstractOperations, "ToUint32",
                Type.getMethodType(Type.LONG_TYPE, Types.ExecutionContext, Types.Object));

        static final MethodDesc AbstractOperations_ToUint32_double = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.AbstractOperations, "ToUint32",
                Type.getMethodType(Type.LONG_TYPE, Type.DOUBLE_TYPE));

        static final MethodDesc AbstractOperations_ToObject = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.AbstractOperations, "ToObject",
                Type.getMethodType(Types.ScriptObject, Types.ExecutionContext, Types.Object));

        static final MethodDesc AbstractOperations_ToPropertyKey = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.AbstractOperations, "ToPropertyKey",
                Type.getMethodType(Types.Object, Types.ExecutionContext, Types.Object));

        static final MethodDesc AbstractOperations_ToString = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.AbstractOperations, "ToString",
                Type.getMethodType(Types.CharSequence, Types.ExecutionContext, Types.Object));

        static final MethodDesc AbstractOperations_ToString_int = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.AbstractOperations, "ToString",
                Type.getMethodType(Types.String, Type.INT_TYPE));

        static final MethodDesc AbstractOperations_ToString_long = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.AbstractOperations, "ToString",
                Type.getMethodType(Types.String, Type.LONG_TYPE));

        static final MethodDesc AbstractOperations_ToString_double = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.AbstractOperations, "ToString",
                Type.getMethodType(Types.String, Type.DOUBLE_TYPE));

        // class: Boolean
        static final MethodDesc Boolean_toString = MethodDesc.create(MethodDesc.Invoke.Static,
                Types.Boolean, "toString", Type.getMethodType(Types.String, Type.BOOLEAN_TYPE));

        // class: CharSequence
        static final MethodDesc CharSequence_length = MethodDesc.create(
                MethodDesc.Invoke.Interface, Types.CharSequence, "length",
                Type.getMethodType(Type.INT_TYPE));
        static final MethodDesc CharSequence_toString = MethodDesc.create(
                MethodDesc.Invoke.Interface, Types.CharSequence, "toString",
                Type.getMethodType(Types.String));

        // class: EnvironmentRecord
        static final MethodDesc EnvironmentRecord_createImmutableBinding = MethodDesc.create(
                MethodDesc.Invoke.Interface, Types.EnvironmentRecord, "createImmutableBinding",
                Type.getMethodType(Type.VOID_TYPE, Types.String));

        static final MethodDesc EnvironmentRecord_initializeBinding = MethodDesc.create(
                MethodDesc.Invoke.Interface, Types.EnvironmentRecord, "initializeBinding",
                Type.getMethodType(Type.VOID_TYPE, Types.String, Types.Object));

        // class: ExecutionContext
        static final MethodDesc ExecutionContext_getLexicalEnvironment = MethodDesc.create(
                MethodDesc.Invoke.Virtual, Types.ExecutionContext, "getLexicalEnvironment",
                Type.getMethodType(Types.LexicalEnvironment));

        static final MethodDesc ExecutionContext_pushLexicalEnvironment = MethodDesc.create(
                MethodDesc.Invoke.Virtual, Types.ExecutionContext, "pushLexicalEnvironment",
                Type.getMethodType(Type.VOID_TYPE, Types.LexicalEnvironment));

        static final MethodDesc ExecutionContext_popLexicalEnvironment = MethodDesc.create(
                MethodDesc.Invoke.Virtual, Types.ExecutionContext, "popLexicalEnvironment",
                Type.getMethodType(Type.VOID_TYPE));

        static final MethodDesc ExecutionContext_replaceLexicalEnvironment = MethodDesc.create(
                MethodDesc.Invoke.Virtual, Types.ExecutionContext, "replaceLexicalEnvironment",
                Type.getMethodType(Type.VOID_TYPE, Types.LexicalEnvironment));

        static final MethodDesc ExecutionContext_restoreLexicalEnvironment = MethodDesc.create(
                MethodDesc.Invoke.Virtual, Types.ExecutionContext, "restoreLexicalEnvironment",
                Type.getMethodType(Type.VOID_TYPE, Types.LexicalEnvironment));

        // class: LexicalEnvironment
        static final MethodDesc LexicalEnvironment_getEnvRec = MethodDesc.create(
                MethodDesc.Invoke.Virtual, Types.LexicalEnvironment, "getEnvRec",
                Type.getMethodType(Types.EnvironmentRecord));

        static final MethodDesc LexicalEnvironment_cloneDeclarativeEnvironment = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.LexicalEnvironment, "cloneDeclarativeEnvironment",
                Type.getMethodType(Types.LexicalEnvironment, Types.LexicalEnvironment));

        static final MethodDesc LexicalEnvironment_newDeclarativeEnvironment = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.LexicalEnvironment, "newDeclarativeEnvironment",
                Type.getMethodType(Types.LexicalEnvironment, Types.LexicalEnvironment));

        static final MethodDesc LexicalEnvironment_newObjectEnvironment = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.LexicalEnvironment, "newObjectEnvironment", Type
                        .getMethodType(Types.LexicalEnvironment, Types.ScriptObject,
                                Types.LexicalEnvironment, Type.BOOLEAN_TYPE));

        // class: OrdinaryFunction
        static final MethodDesc OrdinaryFunction_SetFunctionName_String = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.OrdinaryFunction, "SetFunctionName",
                Type.getMethodType(Type.VOID_TYPE, Types.FunctionObject, Types.String));

        static final MethodDesc OrdinaryFunction_SetFunctionName_Symbol = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.OrdinaryFunction, "SetFunctionName",
                Type.getMethodType(Type.VOID_TYPE, Types.FunctionObject, Types.Symbol));

        // class: ReturnValue
        static final MethodDesc ReturnValue_getValue = MethodDesc.create(MethodDesc.Invoke.Virtual,
                Types.ReturnValue, "getValue", Type.getMethodType(Types.Object));

        // class: ScriptException
        static final MethodDesc ScriptException_getValue = MethodDesc.create(
                MethodDesc.Invoke.Virtual, Types.ScriptException, "getValue",
                Type.getMethodType(Types.Object));

        // class: ScriptRuntime
        static final MethodDesc ScriptRuntime_CreateDefaultConstructor = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.ScriptRuntime, "CreateDefaultConstructor",
                Type.getMethodType(Types.RuntimeInfo$Function));

        static final MethodDesc ScriptRuntime_CreateDefaultEmptyConstructor = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.ScriptRuntime, "CreateDefaultEmptyConstructor",
                Type.getMethodType(Types.RuntimeInfo$Function));

        static final MethodDesc ScriptRuntime_delegatedYield = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.ScriptRuntime, "delegatedYield",
                Type.getMethodType(Types.Object, Types.Object, Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_EvaluateConstructorMethod = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.ScriptRuntime, "EvaluateConstructorMethod", Type
                        .getMethodType(Types.OrdinaryFunction, Types.ScriptObject,
                                Types.OrdinaryObject, Types.RuntimeInfo$Function,
                                Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_getClassProto = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.ScriptRuntime, "getClassProto",
                Type.getMethodType(Types.ScriptObject_, Types.Object, Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_getDefaultClassProto = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.ScriptRuntime, "getDefaultClassProto",
                Type.getMethodType(Types.ScriptObject_, Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_getIteratorObject = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.ScriptRuntime, "getIteratorObject",
                Type.getMethodType(Types.ScriptObject, Types.Object, Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_yield = MethodDesc.create(MethodDesc.Invoke.Static,
                Types.ScriptRuntime, "yield",
                Type.getMethodType(Types.Object, Types.Object, Types.ExecutionContext));

        // class: Type
        static final MethodDesc Type_isUndefinedOrNull = MethodDesc.create(
                MethodDesc.Invoke.Static, Types._Type, "isUndefinedOrNull",
                Type.getMethodType(Type.BOOLEAN_TYPE, Types.Object));
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
        mv.invoke(Methods.ExecutionContext_getLexicalEnvironment);
        mv.invoke(Methods.LexicalEnvironment_getEnvRec);
    }

    /**
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
        assert from != ValType.Reference;
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
        assert from != ValType.Reference;
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
        assert from != ValType.Reference;
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
        assert from != ValType.Reference;
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
        assert from != ValType.Reference;
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
        assert from != ValType.Reference;
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
        assert from != ValType.Reference;
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
        assert from != ValType.Reference;
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
        assert from != ValType.Reference;
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
        assert from != ValType.Reference;
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
     * 14.5.1.2 Runtime Semantics<br>
     * Runtime Semantics: ClassDefinitionEvaluation
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
        mv.checkcast(Types.OrdinaryObject);

        // stack: [ctor, proto] -> [proto, ctor, proto]
        mv.dupX1();

        // steps 4-5
        if (className != null) {
            // stack: [proto, ctor, proto] -> [proto, ctor, proto, scope]
            newDeclarativeEnvironment(mv);

            // stack: [proto, ctor, proto, scope] -> [proto, ctor, proto, scope]
            mv.dup();
            mv.invoke(Methods.LexicalEnvironment_getEnvRec);
            mv.aconst(className);
            mv.invoke(Methods.EnvironmentRecord_createImmutableBinding);

            // stack: [proto, ctor, proto, scope] -> [proto, ctor, proto]
            pushLexicalEnvironment(mv);
            mv.enterScope(def);
        }

        // steps 6-7
        MethodDefinition constructor = ConstructorMethod(def);
        if (constructor != null) {
            codegen.compile(constructor);
            // Runtime Semantics: Evaluation -> MethodDefinition
            mv.invoke(codegen.methodDesc(constructor, FunctionName.RTI));
        } else {
            // step 8
            if (def.getHeritage() != null) {
                // FIXME: spec bug? - `new (class extends null {})` throws TypeError
                mv.invoke(Methods.ScriptRuntime_CreateDefaultConstructor);
            } else {
                mv.invoke(Methods.ScriptRuntime_CreateDefaultEmptyConstructor);
            }
        }

        // step 9 (empty)
        // steps 10-14
        // stack: [proto, ctor, proto, <rti>] -> [proto, F]
        mv.loadExecutionContext();
        mv.invoke(Methods.ScriptRuntime_EvaluateConstructorMethod);

        Variable<OrdinaryFunction> F = mv.newScratchVariable(OrdinaryFunction.class);
        Variable<OrdinaryObject> proto = mv.newScratchVariable(OrdinaryObject.class);

        // stack: [proto, F] -> []
        mv.store(F);
        mv.store(proto);

        // steps 15-17
        ClassPropertyEvaluation(codegen, def, def.getProperties(), F, proto, mv);

        // step 19
        if (className != null) {
            // stack: [] -> [envRec, name, F]
            getLexicalEnvironment(mv);
            mv.invoke(Methods.LexicalEnvironment_getEnvRec);
            mv.aconst(className);
            mv.load(F);

            // stack: [envRec, name, F] -> []
            mv.invoke(Methods.EnvironmentRecord_initializeBinding);
        }

        // step 18
        if (className != null) {
            mv.exitScope();
            // restore previous lexical environment
            popLexicalEnvironment(mv);
        }

        // stack: [] -> [F]
        mv.load(F);

        mv.freeVariable(proto);
        mv.freeVariable(F);

        // step 20 (return F)
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
            Jump done = new Jump();

            mv.enterVariableScope();
            Variable<ScriptObject> iterator = mv.newVariable("iterator", ScriptObject.class);
            Variable<ScriptObject> innerResult = mv.newVariable("innerResult", ScriptObject.class);
            Variable<Object> received = mv.newVariable("received", Object.class);

            /* steps 3-4 */
            // stack: [value] -> []
            mv.loadExecutionContext();
            mv.invoke(Methods.ScriptRuntime_getIteratorObject);
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
                Jump hasThrow = new Jump();
                mv.loadExecutionContext();
                mv.load(iterator);
                mv.aconst("throw");
                mv.invoke(Methods.AbstractOperations_HasProperty);
                mv.ifeq(hasThrow);
                {
                    mv.loadExecutionContext();
                    mv.load(iterator);
                    mv.load(received);
                    mv.checkcast(Types.ScriptException);
                    mv.invoke(Methods.ScriptException_getValue);
                    mv.invoke(Methods.AbstractOperations_IteratorThrow);
                }
                mv.mark(hasThrow);
                mv.load(received);
                mv.checkcast(Types.ScriptException);
                mv.athrow();
            }
            mv.mark(isException);

            /* step 6.c */
            mv.load(received);
            mv.instanceOf(Types.ReturnValue);
            mv.ifeq(iteratorNext);
            {
                Jump hasReturn = new Jump();
                mv.loadExecutionContext();
                mv.load(iterator);
                mv.aconst("return");
                mv.invoke(Methods.AbstractOperations_HasProperty);
                mv.ifeq(hasReturn);
                {
                    mv.loadExecutionContext();
                    mv.load(iterator);
                    mv.load(received);
                    mv.checkcast(Types.ReturnValue);
                    mv.invoke(Methods.ReturnValue_getValue);
                    mv.invoke(Methods.AbstractOperations_IteratorReturn);
                    popStackAndReturn(mv);
                }
                mv.mark(hasReturn);
                mv.load(received);
                mv.checkcast(Types.ReturnValue);
                mv.invoke(Methods.ReturnValue_getValue);
                popStackAndReturn(mv);
            }

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
