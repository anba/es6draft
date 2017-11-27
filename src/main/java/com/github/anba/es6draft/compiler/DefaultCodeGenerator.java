/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static com.github.anba.es6draft.compiler.ClassPropertyGenerator.ClassPropertyEvaluation;
import static com.github.anba.es6draft.semantics.StaticSemantics.DecoratedMethods;
import static com.github.anba.es6draft.semantics.StaticSemantics.PrivateBoundNames;

import java.math.BigInteger;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.PrimitiveIterator;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import com.github.anba.es6draft.ast.*;
import com.github.anba.es6draft.ast.AbruptNode.Abrupt;
import com.github.anba.es6draft.ast.scope.BlockScope;
import com.github.anba.es6draft.ast.scope.ModuleScope;
import com.github.anba.es6draft.ast.scope.Name;
import com.github.anba.es6draft.ast.scope.Scope;
import com.github.anba.es6draft.ast.scope.ScriptScope;
import com.github.anba.es6draft.ast.scope.WithScope;
import com.github.anba.es6draft.ast.synthetic.MethodDefinitionsMethod;
import com.github.anba.es6draft.compiler.CodeVisitor.GeneratorState;
import com.github.anba.es6draft.compiler.CodeVisitor.LabelState;
import com.github.anba.es6draft.compiler.CodeVisitor.LabelledHashKey;
import com.github.anba.es6draft.compiler.CodeVisitor.OutlinedCall;
import com.github.anba.es6draft.compiler.StatementGenerator.Completion;
import com.github.anba.es6draft.compiler.assembler.*;
import com.github.anba.es6draft.compiler.assembler.Code.MethodCode;
import com.github.anba.es6draft.runtime.DeclarativeEnvironmentRecord;
import com.github.anba.es6draft.runtime.EnvironmentRecord;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.GlobalEnvironmentRecord;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.ModuleEnvironmentRecord;
import com.github.anba.es6draft.runtime.ObjectEnvironmentRecord;
import com.github.anba.es6draft.runtime.internal.Bootstrap;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.internal.ScriptIterator;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Null;
import com.github.anba.es6draft.runtime.types.Reference;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Undefined;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryConstructorFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * Abstract base class for specialised generators
 */
abstract class DefaultCodeGenerator<RETURN> extends DefaultNodeVisitor<RETURN, CodeVisitor> {
    private static final class Fields {
        static final FieldName Double_NaN = FieldName.findStatic(Types.Double, "NaN", Type.DOUBLE_TYPE);

        static final FieldName CallOperations_EMPTY_ARRAY = FieldName.findStatic(Types.CallOperations, "EMPTY_ARRAY",
                Types.Object_);
    }

    private static final class Methods {
        // class: AbstractOperations
        static final MethodName AbstractOperations_CreateIterResultObject = MethodName.findStatic(
                Types.AbstractOperations, "CreateIterResultObject",
                Type.methodType(Types.OrdinaryObject, Types.ExecutionContext, Types.Object, Type.BOOLEAN_TYPE));

        static final MethodName AbstractOperations_HasOwnProperty = MethodName.findStatic(Types.AbstractOperations,
                "HasOwnProperty",
                Type.methodType(Type.BOOLEAN_TYPE, Types.ExecutionContext, Types.ScriptObject, Types.String));

        static final MethodName AbstractOperations_GetMethod = MethodName.findStatic(Types.AbstractOperations,
                "GetMethod", Type.methodType(Types.Callable, Types.ExecutionContext, Types.ScriptObject, Types.String));

        static final MethodName AbstractOperations_Invoke = MethodName.findStatic(Types.AbstractOperations, "Invoke",
                Type.methodType(Types.Object, Types.ExecutionContext, Types.ScriptObject, Types.String, Types.Object_));

        static final MethodName AbstractOperations_IteratorComplete = MethodName.findStatic(Types.AbstractOperations,
                "IteratorComplete", Type.methodType(Type.BOOLEAN_TYPE, Types.ExecutionContext, Types.ScriptObject));

        static final MethodName AbstractOperations_IteratorNext_Object = MethodName.findStatic(Types.AbstractOperations,
                "IteratorNext",
                Type.methodType(Types.ScriptObject, Types.ExecutionContext, Types.ScriptIterator, Types.Object));

        static final MethodName AbstractOperations_IteratorValue = MethodName.findStatic(Types.AbstractOperations,
                "IteratorValue", Type.methodType(Types.Object, Types.ExecutionContext, Types.ScriptObject));

        static final MethodName AbstractOperations_ToPrimitive = MethodName.findStatic(Types.AbstractOperations,
                "ToPrimitive", Type.methodType(Types.Object, Types.ExecutionContext, Types.Object));

        static final MethodName AbstractOperations_ToBoolean = MethodName.findStatic(Types.AbstractOperations,
                "ToBoolean", Type.methodType(Type.BOOLEAN_TYPE, Types.Object));

        static final MethodName AbstractOperations_ToBoolean_int = MethodName.findStatic(Types.AbstractOperations,
                "ToBoolean", Type.methodType(Type.BOOLEAN_TYPE, Type.INT_TYPE));

        static final MethodName AbstractOperations_ToBoolean_long = MethodName.findStatic(Types.AbstractOperations,
                "ToBoolean", Type.methodType(Type.BOOLEAN_TYPE, Type.LONG_TYPE));

        static final MethodName AbstractOperations_ToBoolean_double = MethodName.findStatic(Types.AbstractOperations,
                "ToBoolean", Type.methodType(Type.BOOLEAN_TYPE, Type.DOUBLE_TYPE));

        static final MethodName AbstractOperations_ToBoolean_BigInteger = MethodName.findStatic(
                Types.AbstractOperations, "ToBoolean", Type.methodType(Type.BOOLEAN_TYPE, Types.BigInteger));

        static final MethodName AbstractOperations_ToFlatString = MethodName.findStatic(Types.AbstractOperations,
                "ToFlatString", Type.methodType(Types.String, Types.ExecutionContext, Types.Object));

        static final MethodName AbstractOperations_ToNumber = MethodName.findStatic(Types.AbstractOperations,
                "ToNumber", Type.methodType(Type.DOUBLE_TYPE, Types.ExecutionContext, Types.Object));

        static final MethodName AbstractOperations_ToNumber_CharSequence = MethodName.findStatic(
                Types.AbstractOperations, "ToNumber", Type.methodType(Type.DOUBLE_TYPE, Types.CharSequence));

        static final MethodName AbstractOperations_ToNumeric = MethodName.findStatic(Types.AbstractOperations,
                "ToNumeric", Type.methodType(Types.Number, Types.ExecutionContext, Types.Object));

        static final MethodName AbstractOperations_ToNumericInt32 = MethodName.findStatic(Types.AbstractOperations,
                "ToNumericInt32", Type.methodType(Types.Number, Types.ExecutionContext, Types.Object));

        static final MethodName AbstractOperations_ToInt32 = MethodName.findStatic(Types.AbstractOperations, "ToInt32",
                Type.methodType(Type.INT_TYPE, Types.ExecutionContext, Types.Object));

        static final MethodName AbstractOperations_ToInt32_double = MethodName.findStatic(Types.AbstractOperations,
                "ToInt32", Type.methodType(Type.INT_TYPE, Type.DOUBLE_TYPE));

        static final MethodName AbstractOperations_ToUint32 = MethodName.findStatic(Types.AbstractOperations,
                "ToUint32", Type.methodType(Type.LONG_TYPE, Types.ExecutionContext, Types.Object));

        static final MethodName AbstractOperations_ToUint32_double = MethodName.findStatic(Types.AbstractOperations,
                "ToUint32", Type.methodType(Type.LONG_TYPE, Type.DOUBLE_TYPE));

        static final MethodName AbstractOperations_ToObject = MethodName.findStatic(Types.AbstractOperations,
                "ToObject", Type.methodType(Types.ScriptObject, Types.ExecutionContext, Types.Object));

        static final MethodName AbstractOperations_ToPropertyKey = MethodName.findStatic(Types.AbstractOperations,
                "ToPropertyKey", Type.methodType(Types.Object, Types.ExecutionContext, Types.Object));

        static final MethodName AbstractOperations_ToString = MethodName.findStatic(Types.AbstractOperations,
                "ToString", Type.methodType(Types.CharSequence, Types.ExecutionContext, Types.Object));

        static final MethodName AbstractOperations_ToString_int = MethodName.findStatic(Types.AbstractOperations,
                "ToString", Type.methodType(Types.String, Type.INT_TYPE));

        static final MethodName AbstractOperations_ToString_long = MethodName.findStatic(Types.AbstractOperations,
                "ToString", Type.methodType(Types.String, Type.LONG_TYPE));

        static final MethodName AbstractOperations_ToString_double = MethodName.findStatic(Types.AbstractOperations,
                "ToString", Type.methodType(Types.String, Type.DOUBLE_TYPE));

        static final MethodName AbstractOperations_ToString_BigInteger = MethodName.findStatic(Types.AbstractOperations,
                "ToString", Type.methodType(Types.String, Types.BigInteger));

        // class: AsyncAbstractOperations
        static final MethodName AsyncAbstractOperations_AsyncFunctionAwait = MethodName.findStatic(
                Types.AsyncAbstractOperations, "AsyncFunctionAwait",
                Type.methodType(Type.VOID_TYPE, Types.ExecutionContext, Types.Object));

        // class: Boolean
        static final MethodName Boolean_toString = MethodName.findStatic(Types.Boolean, "toString",
                Type.methodType(Types.String, Type.BOOLEAN_TYPE));

        // class: CharSequence
        static final MethodName CharSequence_length = MethodName.findInterface(Types.CharSequence, "length",
                Type.methodType(Type.INT_TYPE));
        static final MethodName CharSequence_toString = MethodName.findInterface(Types.CharSequence, "toString",
                Type.methodType(Types.String));

        // class: ExecutionContext
        static final MethodName ExecutionContext_getLexicalEnvironment = MethodName.findVirtual(Types.ExecutionContext,
                "getLexicalEnvironment", Type.methodType(Types.LexicalEnvironment));

        static final MethodName ExecutionContext_getLexicalEnvironmentRecord = MethodName.findVirtual(
                Types.ExecutionContext, "getLexicalEnvironmentRecord", Type.methodType(Types.EnvironmentRecord));

        static final MethodName ExecutionContext_getVariableEnvironmentRecord = MethodName.findVirtual(
                Types.ExecutionContext, "getVariableEnvironmentRecord", Type.methodType(Types.EnvironmentRecord));

        static final MethodName ExecutionContext_pushLexicalEnvironment = MethodName.findVirtual(Types.ExecutionContext,
                "pushLexicalEnvironment", Type.methodType(Type.VOID_TYPE, Types.LexicalEnvironment));

        static final MethodName ExecutionContext_popLexicalEnvironment = MethodName.findVirtual(Types.ExecutionContext,
                "popLexicalEnvironment", Type.methodType(Type.VOID_TYPE));

        static final MethodName ExecutionContext_replaceLexicalEnvironment = MethodName.findVirtual(
                Types.ExecutionContext, "replaceLexicalEnvironment",
                Type.methodType(Type.VOID_TYPE, Types.LexicalEnvironment));

        static final MethodName ExecutionContext_restoreLexicalEnvironment = MethodName.findVirtual(
                Types.ExecutionContext, "restoreLexicalEnvironment",
                Type.methodType(Type.VOID_TYPE, Types.LexicalEnvironment));

        // class: LexicalEnvironment
        static final MethodName LexicalEnvironment_getEnvRec = MethodName.findVirtual(Types.LexicalEnvironment,
                "getEnvRec", Type.methodType(Types.EnvironmentRecord));

        static final MethodName LexicalEnvironment_cloneDeclarativeEnvironment = MethodName.findStatic(
                Types.LexicalEnvironment, "cloneDeclarativeEnvironment",
                Type.methodType(Types.LexicalEnvironment, Types.LexicalEnvironment));

        static final MethodName LexicalEnvironment_newDeclarativeEnvironment = MethodName.findStatic(
                Types.LexicalEnvironment, "newDeclarativeEnvironment",
                Type.methodType(Types.LexicalEnvironment, Types.LexicalEnvironment));

        static final MethodName LexicalEnvironment_newCatchDeclarativeEnvironment = MethodName.findStatic(
                Types.LexicalEnvironment, "newCatchDeclarativeEnvironment",
                Type.methodType(Types.LexicalEnvironment, Types.LexicalEnvironment));

        static final MethodName LexicalEnvironment_newObjectEnvironment = MethodName
                .findStatic(Types.LexicalEnvironment, "newObjectEnvironment", Type.methodType(Types.LexicalEnvironment,
                        Types.ScriptObject, Types.LexicalEnvironment, Type.BOOLEAN_TYPE));

        // class: FunctionObject
        static final MethodName FunctionObject_SetFunctionName = MethodName.findStatic(Types.FunctionObject,
                "SetFunctionName", Type.methodType(Type.VOID_TYPE, Types.OrdinaryObject, Types.Object));

        static final MethodName FunctionObject_SetFunctionName_String = MethodName.findStatic(Types.FunctionObject,
                "SetFunctionName", Type.methodType(Type.VOID_TYPE, Types.OrdinaryObject, Types.String));

        // class: ReturnValue
        static final MethodName ReturnValue_new = MethodName.findConstructor(Types.ReturnValue,
                Type.methodType(Type.VOID_TYPE, Types.Object));

        static final MethodName ReturnValue_getValue = MethodName.findVirtual(Types.ReturnValue, "getValue",
                Type.methodType(Types.Object));

        // class: ScriptException
        static final MethodName ScriptException_getValue = MethodName.findVirtual(Types.ScriptException, "getValue",
                Type.methodType(Types.Object));

        // class: CallOperations
        static final MethodName CallOperations_CheckCallable = MethodName.findStatic(Types.CallOperations,
                "CheckCallable", Type.methodType(Types.Callable, Types.Object, Types.ExecutionContext));

        // class: ClassOperations
        static final MethodName ClassOperations_CreateStaticClassFieldInitializer = MethodName.findStatic(
                Types.ClassOperations, "CreateStaticClassFieldInitializer", Type.methodType(Types.OrdinaryFunction,
                        Types.ScriptObject, Types.RuntimeInfo$Function, Types.Object_, Types.ExecutionContext));

        static final MethodName ClassOperations_CreateClassFieldInitializer = MethodName.findStatic(
                Types.ClassOperations, "CreateClassFieldInitializer",
                Type.methodType(Types.OrdinaryFunction, Types.ScriptObject, Types.RuntimeInfo$Function, Types.Object_,
                        Types.ClassOperations$InstanceMethod_, Types.ExecutionContext));

        static final MethodName ClassOperations_EvaluateConstructorMethod = MethodName.findStatic(Types.ClassOperations,
                "EvaluateConstructorMethod", Type.methodType(Types.OrdinaryConstructorFunction, Types.ScriptObject,
                        Types.OrdinaryObject, Types.RuntimeInfo$Function, Type.BOOLEAN_TYPE, Types.ExecutionContext));

        static final MethodName ClassOperations_createProto = MethodName.findStatic(Types.ClassOperations,
                "createProto", Type.methodType(Types.OrdinaryObject, Types.ScriptObject, Types.ExecutionContext));

        static final MethodName ClassOperations_getClassProto = MethodName.findStatic(Types.ClassOperations,
                "getClassProto", Type.methodType(Types.ScriptObject_, Types.Object, Types.ExecutionContext));

        static final MethodName ClassOperations_getClassProto_Null = MethodName.findStatic(Types.ClassOperations,
                "getClassProto", Type.methodType(Types.ScriptObject_, Types.ExecutionContext));

        static final MethodName ClassOperations_getDefaultClassProto = MethodName.findStatic(Types.ClassOperations,
                "getDefaultClassProto", Type.methodType(Types.ScriptObject_, Types.ExecutionContext));

        static final MethodName ClassOperations_setInstanceFieldsInitializer = MethodName.findStatic(
                Types.ClassOperations, "setInstanceFieldsInitializer",
                Type.methodType(Type.VOID_TYPE, Types.OrdinaryConstructorFunction, Types.OrdinaryFunction));

        // class: DecoratorOperations
        static final MethodName DecoratorOperations_propertyDescriptor = MethodName.findStatic(
                Types.DecoratorOperations, "propertyDescriptor",
                Type.methodType(Types.Object, Types.OrdinaryObject, Types.Object, Types.ExecutionContext));

        static final MethodName DecoratorOperations_defineProperty = MethodName.findStatic(Types.DecoratorOperations,
                "defineProperty", Type.methodType(Type.VOID_TYPE, Types.OrdinaryObject, Types.Object, Types.Object,
                        Types.ExecutionContext));

        // class: IteratorOperations
        static final MethodName IteratorOperations_iterate = MethodName.findStatic(Types.IteratorOperations, "iterate",
                Type.methodType(Types.ScriptIterator, Types.Object, Types.ExecutionContext));

        static final MethodName IteratorOperations_asyncIterate = MethodName.findStatic(Types.IteratorOperations,
                "asyncIterate", Type.methodType(Types.ScriptIterator, Types.Object, Types.ExecutionContext));

        // class: Operators
        static final MethodName Operators_yieldThrowCompletion = MethodName.findStatic(Types.Operators,
                "yieldThrowCompletion",
                Type.methodType(Types.ScriptObject, Types.ExecutionContext, Types.ScriptObject, Types.ScriptException));

        static final MethodName Operators_yieldReturnCompletion = MethodName.findStatic(Types.Operators,
                "yieldReturnCompletion",
                Type.methodType(Types.ScriptObject, Types.ExecutionContext, Types.ScriptObject, Types.ReturnValue));

        static final MethodName Operators_reportPropertyNotCallable = MethodName.findStatic(Types.Operators,
                "reportPropertyNotCallable",
                Type.methodType(Types.ScriptException, Types.String, Types.ExecutionContext));

        static final MethodName Operators_requireObjectResult = MethodName.findStatic(Types.Operators,
                "requireObjectResult",
                Type.methodType(Types.ScriptObject, Types.Object, Types.String, Types.ExecutionContext));

        // class: Type
        static final MethodName Type_isObject = MethodName.findStatic(Types._Type, "isObject",
                Type.methodType(Type.BOOLEAN_TYPE, Types.Object));

        static final MethodName Type_isUndefinedOrNull = MethodName.findStatic(Types._Type, "isUndefinedOrNull",
                Type.methodType(Type.BOOLEAN_TYPE, Types.Object));

        // class: ScriptIterator
        static final MethodName ScriptIterator_getScriptObject = MethodName.findInterface(Types.ScriptIterator,
                "getScriptObject", Type.methodType(Types.ScriptObject));

        static final MethodName ScriptIterator_nextIterResult = MethodName.findInterface(Types.ScriptIterator,
                "nextIterResult", Type.methodType(Types.Object, Types.Object));

        // class: Throwable
        static final MethodName Throwable_addSuppressed = MethodName.findVirtual(Types.Throwable, "addSuppressed",
                Type.methodType(Type.VOID_TYPE, Types.Throwable));
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
     *            the code visitor
     * @return the value type returned by the expression
     */
    protected final ValType expression(Expression node, CodeVisitor mv) {
        return codegen.expression(node, mv);
    }

    /**
     * stack: [] {@literal ->} [boxed(value)]
     * 
     * @param node
     *            the expression node
     * @param mv
     *            the code visitor
     * @return the value type returned by the expression
     */
    protected final ValType expressionBoxed(Expression node, CodeVisitor mv) {
        return codegen.expressionBoxed(node, mv);
    }

    /**
     * stack: [] {@literal ->} []
     * 
     * @param node
     *            the module-item node
     * @param mv
     *            the code visitor
     * @return the completion type
     */
    protected final Completion statement(ModuleItem node, CodeVisitor mv) {
        return codegen.statement(node, mv);
    }

    /**
     * stack: [] {@literal ->} []
     * 
     * @param statements
     *            the statements list
     * @param mv
     *            the code visitor
     * @return the completion type
     */
    protected final Completion statements(List<? extends ModuleItem> statements, CodeVisitor mv) {
        return codegen.statements(statements, mv);
    }

    /**
     * stack: [] {@literal ->} []
     * 
     * @param node
     *            the abrupt node
     * @param mv
     *            the code visitor
     * @return the variable holding the saved environment or {@code null}
     */
    protected final Variable<LexicalEnvironment<?>> saveEnvironment(AbruptNode node, CodeVisitor mv) {
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
     *            the code visitor
     * @return the variable holding the saved environment
     */
    protected final Variable<LexicalEnvironment<?>> saveEnvironment(CodeVisitor mv) {
        Variable<LexicalEnvironment<?>> savedEnv = mv.newVariable("savedEnv", LexicalEnvironment.class).uncheckedCast();
        getLexicalEnvironment(mv);
        mv.store(savedEnv);
        return savedEnv;
    }

    /**
     * stack: [] {@literal ->} []
     * 
     * @param savedEnv
     *            the variable which holds the saved environment
     * @param mv
     *            the code visitor
     */
    protected final void restoreEnvironment(Variable<LexicalEnvironment<?>> savedEnv, CodeVisitor mv) {
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
     *            the code visitor
     */
    protected final void replaceLexicalEnvironment(Variable<LexicalEnvironment<?>> savedEnv, CodeVisitor mv) {
        mv.loadExecutionContext();
        mv.load(savedEnv);
        mv.invoke(Methods.ExecutionContext_replaceLexicalEnvironment);
    }

    /**
     * stack: [] {@literal ->} [lexEnv]
     * 
     * @param mv
     *            the code visitor
     */
    protected final void getLexicalEnvironment(CodeVisitor mv) {
        mv.loadExecutionContext();
        mv.invoke(Methods.ExecutionContext_getLexicalEnvironment);
    }

    /**
     * stack: [] {@literal ->} []
     * 
     * @param envRec
     *            the variable which holds the lexical environment record
     * @param mv
     *            the code visitor
     */
    protected final <R extends EnvironmentRecord> void getLexicalEnvironmentRecord(Variable<? extends R> envRec,
            CodeVisitor mv) {
        mv.loadExecutionContext();
        mv.invoke(Methods.ExecutionContext_getLexicalEnvironmentRecord);
        if (envRec.getType() != Types.EnvironmentRecord) {
            mv.checkcast(envRec.getType());
        }
        mv.store(envRec);
    }

    /**
     * stack: [] {@literal ->} []
     * 
     * @param <R>
     *            the environment record type
     * @param type
     *            the environment record type
     * @param mv
     *            the code visitor
     */
    protected final <R extends EnvironmentRecord> Value<R> getLexicalEnvironmentRecord(Type type, CodeVisitor mv) {
        return __ -> {
            mv.loadExecutionContext();
            mv.invoke(Methods.ExecutionContext_getLexicalEnvironmentRecord);
            if (type != Types.EnvironmentRecord) {
                mv.checkcast(type);
            }
        };
    }

    /**
     * stack: [] {@literal ->} []
     * 
     * @param envRec
     *            the variable which holds the variable environment record
     * @param mv
     *            the code visitor
     */
    protected final <R extends EnvironmentRecord> void getVariableEnvironmentRecord(Variable<? extends R> envRec,
            CodeVisitor mv) {
        mv.loadExecutionContext();
        mv.invoke(Methods.ExecutionContext_getVariableEnvironmentRecord);
        if (envRec.getType() != Types.EnvironmentRecord) {
            mv.checkcast(envRec.getType());
        }
        mv.store(envRec);
    }

    /**
     * stack: [] {@literal ->} []
     * 
     * @param <R>
     *            the environment record type
     * @param type
     *            the environment record type
     * @param mv
     *            the code visitor
     */
    protected final <R extends EnvironmentRecord> Value<R> getVariableEnvironmentRecord(Type type, CodeVisitor mv) {
        return __ -> {
            mv.loadExecutionContext();
            mv.invoke(Methods.ExecutionContext_getVariableEnvironmentRecord);
            if (type != Types.EnvironmentRecord) {
                mv.checkcast(type);
            }
        };
    }

    /**
     * Returns the current environment record type.
     * 
     * @param mv
     *            the code visitor
     * @return the current environment record type
     */
    protected final Class<? extends EnvironmentRecord> getEnvironmentRecordClass(CodeVisitor mv) {
        Scope scope = mv.getScope();
        while (!scope.isPresent()) {
            scope = scope.getParent();
        }
        if (scope instanceof ScriptScope) {
            Script script = ((ScriptScope) scope).getNode();
            if (!(script.isEvalScript() || script.isScripting())) {
                return GlobalEnvironmentRecord.class;
            }
        } else if (scope instanceof ModuleScope) {
            return ModuleEnvironmentRecord.class;
        } else if (scope instanceof WithScope) {
            return ObjectEnvironmentRecord.class;
        }
        return DeclarativeEnvironmentRecord.class;
    }

    /**
     * Creates a new object environment.
     * <p>
     * stack: [obj] {@literal ->} [lexEnv]
     * 
     * @param mv
     *            the code visitor
     * @param withEnvironment
     *            the withEnvironment flag
     */
    protected final void newObjectEnvironment(CodeVisitor mv, boolean withEnvironment) {
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
     *            the code visitor
     */
    protected final void newDeclarativeEnvironment(BlockScope scope, CodeVisitor mv) {
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
     *            the code visitor
     */
    protected final void newCatchDeclarativeEnvironment(BlockScope scope, CodeVisitor mv) {
        mv.loadExecutionContext();
        mv.invoke(Methods.ExecutionContext_getLexicalEnvironment);
        mv.invoke(Methods.LexicalEnvironment_newCatchDeclarativeEnvironment);
    }

    /**
     * stack: [] {@literal ->} [lexEnv]
     * 
     * @param mv
     *            the code visitor
     */
    protected final void cloneDeclarativeEnvironment(CodeVisitor mv) {
        mv.loadExecutionContext();
        mv.invoke(Methods.ExecutionContext_getLexicalEnvironment);
        mv.invoke(Methods.LexicalEnvironment_cloneDeclarativeEnvironment);
    }

    /**
     * stack: [lexEnv] {@literal ->} []
     * 
     * @param mv
     *            the code visitor
     */
    protected final void pushLexicalEnvironment(CodeVisitor mv) {
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
     *            the code visitor
     */
    protected final void popLexicalEnvironment(CodeVisitor mv) {
        mv.loadExecutionContext();
        mv.invoke(Methods.ExecutionContext_popLexicalEnvironment);
    }

    /**
     * Emit function call for: {@link LexicalEnvironment#getEnvRec()}
     * <p>
     * stack: [] {@literal ->} []
     * 
     * @param env
     *            the variable which holds the lexical environment
     * @param envRec
     *            the variable which holds the environment record
     * @param mv
     *            the instruction visitor
     */
    protected final <R extends EnvironmentRecord, R2 extends R> void getEnvRec(
            Variable<? extends LexicalEnvironment<? extends R2>> env, Variable<? extends R> envRec,
            InstructionVisitor mv) {
        mv.load(env);
        mv.invoke(Methods.LexicalEnvironment_getEnvRec);
        if (envRec.getType() != Types.EnvironmentRecord) {
            mv.checkcast(envRec.getType());
        }
        mv.store(envRec);
    }

    /**
     * Emit function call for: {@link LexicalEnvironment#getEnvRec()}
     * <p>
     * stack: [env] {@literal ->} [env]
     * 
     * @param env
     *            the variable which holds the lexical environment
     * @param envRec
     *            the variable which holds the environment record
     * @param mv
     *            the instruction visitor
     */
    protected final <R extends EnvironmentRecord> void getEnvRec(Variable<? extends R> envRec, InstructionVisitor mv) {
        mv.dup(); // TODO: Remove dup?!
        mv.invoke(Methods.LexicalEnvironment_getEnvRec);
        if (envRec.getType() != Types.EnvironmentRecord) {
            mv.checkcast(envRec.getType());
        }
        mv.store(envRec);
    }

    /**
     * stack: [object] {@literal ->} [boolean]
     * 
     * @param mv
     *            the code visitor
     */
    protected final void isUndefinedOrNull(CodeVisitor mv) {
        mv.invoke(Methods.Type_isUndefinedOrNull);
    }

    enum ValType {
        Undefined, Null, Boolean, Number, Number_int, Number_uint, BigInt, String, Object, Reference, Any, Empty;

        public int size() {
            switch (this) {
            case Number:
            case Number_uint:
                return 2;
            case Number_int:
            case BigInt:
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
            case BigInt:
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

        public boolean isNumber() {
            switch (this) {
            case Number:
            case Number_int:
            case Number_uint:
                return true;
            case BigInt:
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
            case BigInt:
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
            case BigInt:
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
            case BigInt:
                return BigInteger.class;
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
            case BigInt:
                return Types.BigInteger;
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
            case BigInt:
                return Types.BigInteger;
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

        public static ValType of(Type type) {
            if (type.isPrimitive()) {
                if (Type.BOOLEAN_TYPE.equals(type)) {
                    return ValType.Boolean;
                }
                if (Type.INT_TYPE.equals(type)) {
                    return ValType.Number_int;
                }
                if (Type.LONG_TYPE.equals(type)) {
                    return ValType.Number_uint;
                }
                if (Type.DOUBLE_TYPE.equals(type)) {
                    return ValType.Number;
                }
                return ValType.Any;
            }
            if (Types.Boolean.equals(type)) {
                return ValType.Boolean;
            }
            if (Types.Integer.equals(type)) {
                return ValType.Number_int;
            }
            if (Types.Long.equals(type)) {
                return ValType.Number_uint;
            }
            if (Types.Double.equals(type)) {
                return ValType.Number;
            }
            if (Types.BigInteger.equals(type)) {
                return ValType.BigInt;
            }
            if (Types.Null.equals(type)) {
                return ValType.Null;
            }
            if (Types.Undefined.equals(type)) {
                return ValType.Undefined;
            }
            if (Types.String.equals(type) || Types.CharSequence.equals(type)) {
                return ValType.String;
            }
            if (Types.ScriptObject.equals(type)) {
                return ValType.Object;
            }
            if (Types.OrdinaryObject.equals(type)) {
                return ValType.Object;
            }
            return ValType.Any;
        }
    }

    /**
     * stack: [Object] {@literal ->} [boolean]
     * 
     * @param from
     *            the input value type
     * @param mv
     *            the code visitor
     * @return the returned value type
     */
    protected static final ValType ToPrimitive(ValType from, CodeVisitor mv) {
        switch (from) {
        case Number:
        case Number_int:
        case Number_uint:
        case BigInt:
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
     *            the code visitor
     */
    protected final void ToBoolean(ValType from, CodeVisitor mv) {
        switch (from) {
        case Number:
            mv.invoke(Methods.AbstractOperations_ToBoolean_double);
            return;
        case Number_int:
            mv.invoke(Methods.AbstractOperations_ToBoolean_int);
            return;
        case Number_uint:
            mv.invoke(Methods.AbstractOperations_ToBoolean_long);
            return;
        case BigInt:
            mv.invoke(Methods.AbstractOperations_ToBoolean_BigInteger);
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
            if (codegen.isEnabled(CompatibilityOption.IsHTMLDDAObjects)) {
                mv.instanceOf(Types.HTMLDDAObject);
                mv.not();
            } else {
                mv.pop();
                mv.iconst(true);
            }
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
     *            the code visitor
     */
    protected static final void ToNumber(ValType from, CodeVisitor mv) {
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
        case BigInt:
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
     * stack: [Object] {@literal ->} [double or BigInteger]
     * 
     * @param from
     *            the input value type
     * @param mv
     *            the code visitor
     * @return the returned value type
     */
    protected static final ValType ToNumeric(ValType from, CodeVisitor mv) {
        switch (from) {
        case Number:
        case Number_int:
        case Number_uint:
        case Undefined:
        case Null:
        case Boolean:
        case String:
            ToNumber(from, mv);
            return ValType.Number;
        case BigInt:
            return ValType.BigInt;
        case Object:
        case Any:
            mv.loadExecutionContext();
            mv.swap();
            mv.invoke(Methods.AbstractOperations_ToNumeric);
            return ValType.Any;
        case Empty:
        case Reference:
        default:
            throw new AssertionError();
        }
    }

    /**
     * stack: [Object] {@literal ->} [double or BigInteger]
     * 
     * @param from
     *            the input value type
     * @param mv
     *            the code visitor
     * @return the returned value type
     */
    protected static final ValType ToNumericInt32(ValType from, CodeVisitor mv) {
        switch (from) {
        case Number:
        case Number_int:
        case Number_uint:
        case Undefined:
        case Null:
        case Boolean:
        case String:
            ToInt32(from, mv);
            return ValType.Number_int;
        case BigInt:
            return ValType.BigInt;
        case Object:
        case Any:
            mv.loadExecutionContext();
            mv.swap();
            mv.invoke(Methods.AbstractOperations_ToNumericInt32);
            return ValType.Any;
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
     *            the code visitor
     */
    protected static final void ToInt32(ValType from, CodeVisitor mv) {
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
        case BigInt:
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
     *            the code visitor
     */
    protected static final void ToUint32(ValType from, CodeVisitor mv) {
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
        case BigInt:
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
     *            the code visitor
     */
    protected static final void ToString(ValType from, CodeVisitor mv) {
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
        case BigInt:
            mv.invoke(Methods.AbstractOperations_ToString_BigInteger);
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
     *            the code visitor
     */
    protected static final void ToFlatString(ValType from, CodeVisitor mv) {
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
        case BigInt:
            mv.invoke(Methods.AbstractOperations_ToString_BigInteger);
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
     *            the code visitor
     */
    protected static final void ToObject(ValType from, CodeVisitor mv) {
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
        case BigInt:
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
     *            the code visitor
     */
    protected static final void ToObject(Node node, ValType from, CodeVisitor mv) {
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
        case BigInt:
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
     *            the code visitor
     */
    protected static final ValType ToPropertyKey(ValType from, CodeVisitor mv) {
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
        case BigInt:
            mv.invoke(Methods.AbstractOperations_ToString_BigInteger);
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
     *            the code visitor
     */
    protected static final void SetFunctionName(Node node, ValType propertyKeyType, CodeVisitor mv) {
        Jump hasOwnName = null;
        switch (hasOwnNameProperty(node)) {
        case HasOwn:
            return;
        case HasComputed:
            emitHasOwnNameProperty(mv);

            hasOwnName = new Jump();
            mv.ifne(hasOwnName);
        default:
        }

        // stack: [propertyKey, function] -> [propertyKey, function, function, propertyKey]
        mv.dup2();
        mv.swap();

        if (propertyKeyType == ValType.String) {
            mv.invoke(Methods.FunctionObject_SetFunctionName_String);
        } else {
            assert propertyKeyType == ValType.Any;

            // stack: [propertyKey, function, function, propertyKey] -> [propertyKey, function]
            mv.invoke(Methods.FunctionObject_SetFunctionName);
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
     *            the code visitor
     */
    protected static final void SetFunctionName(Node node, Name name, CodeVisitor mv) {
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
     *            the code visitor
     */
    protected static final void SetFunctionName(Node node, String name, CodeVisitor mv) {
        Jump hasOwnName = null;
        switch (hasOwnNameProperty(node)) {
        case HasOwn:
            return;
        case HasComputed:
            emitHasOwnNameProperty(mv);

            hasOwnName = new Jump();
            mv.ifne(hasOwnName);
        default:
        }

        // stack: [function] -> [function, function, name]
        mv.dup();
        mv.aconst(name);
        // stack: [function, function, name] -> [function]
        mv.invoke(Methods.FunctionObject_SetFunctionName_String);

        if (hasOwnName != null) {
            mv.mark(hasOwnName);
        }
    }

    private static void emitHasOwnNameProperty(CodeVisitor mv) {
        // stack: [function] -> [function, cx, function, "name"]
        mv.dup();
        mv.loadExecutionContext();
        mv.swap();
        mv.aconst("name");
        // stack: [function, cx, function, "name"] -> [function, hasOwn]
        mv.invoke(Methods.AbstractOperations_HasOwnProperty);
    }

    private enum NameProperty {
        HasOwn, HasComputed, None
    }

    private static NameProperty hasOwnNameProperty(Node node) {
        if (node instanceof FunctionNode) {
            return NameProperty.None;
        }

        assert node instanceof ClassDefinition : node.getClass();
        for (PropertyDefinition property : ((ClassDefinition) node).getProperties()) {
            if (property instanceof MethodDefinition) {
                MethodDefinition methodDefinition = (MethodDefinition) property;
                if (methodDefinition.isStatic()) {
                    String methodName = methodDefinition.getPropertyName().getName();
                    if (methodName == null) {
                        return NameProperty.HasComputed;
                    }
                    if ("name".equals(methodName)) {
                        return NameProperty.HasOwn;
                    }
                }
                if (!methodDefinition.getDecorators().isEmpty()) {
                    // Decorator expressions are like computed names.
                    return NameProperty.HasComputed;
                }
            } else if (property instanceof ClassFieldDefinition) {
                ClassFieldDefinition fieldDefinition = (ClassFieldDefinition) property;
                if (fieldDefinition.isStatic()) {
                    String fieldName = fieldDefinition.getPropertyName().getName();
                    if (fieldName == null) {
                        return NameProperty.HasComputed;
                    }
                    if ("name".equals(fieldName)) {
                        return NameProperty.HasOwn;
                    }
                    if (fieldDefinition.getInitializer() != null) {
                        // Initializer expression can define "name" property.
                        return NameProperty.HasComputed;
                    }
                }
            } else {
                // Take the slow path when the class was split into multiple methods.
                assert property instanceof MethodDefinitionsMethod;
                return NameProperty.HasComputed;
            }
        }
        return NameProperty.None;
    }

    /**
     * 14.5.14 Runtime Semantics: ClassDefinitionEvaluation
     * 
     * @param def
     *            the class definition node
     * @param className
     *            the class name or {@code null} if not present
     * @param mv
     *            the code visitor
     */
    protected final void ClassDefinitionEvaluation(ClassDefinition def, Name className, CodeVisitor mv) {
        mv.enterVariableScope();

        List<Expression> classDecoratorsList = def.getDecorators();
        Variable<Callable[]> classDecorators = null;
        if (!classDecoratorsList.isEmpty()) {
            classDecorators = mv.newVariable("classDecorators", Callable[].class);
            mv.anewarray(classDecoratorsList.size(), Types.Callable);
            mv.store(classDecorators);

            int index = 0;
            for (Expression decorator : classDecoratorsList) {
                mv.astore(classDecorators, index++, __ -> {
                    expressionBoxed(decorator, mv);
                    CheckCallable(decorator, mv);
                });
            }
        }

        mv.enterClassDefinition();

        // step 1 (not applicable)
        // steps 2-4
        BlockScope scope = def.getScope();
        Variable<DeclarativeEnvironmentRecord> classScopeEnvRec = null;
        if (scope != null) {
            assert scope.isPresent() == (className != null);

            if (scope.isPresent()) {
                // stack: [] -> [classScope]
                newDeclarativeEnvironment(scope, mv);

                classScopeEnvRec = mv.newVariable("classScopeEnvRec", DeclarativeEnvironmentRecord.class);
                getEnvRec(classScopeEnvRec, mv);

                if (className != null) {
                    // stack: [classScope] -> [classScope]
                    Name innerName = scope.resolveName(className);
                    BindingOp<DeclarativeEnvironmentRecord> op = BindingOp.of(classScopeEnvRec, innerName);
                    op.createImmutableBinding(classScopeEnvRec, innerName, true, mv);
                }

                // stack: [classScope] -> []
                pushLexicalEnvironment(mv);
            }
            mv.enterScope(def);
        }

        // steps 5-7
        // stack: [] -> [<constructorParent,proto>]
        Expression classHeritage = def.getHeritage();
        if (classHeritage == null) {
            mv.loadExecutionContext();
            mv.invoke(Methods.ClassOperations_getDefaultClassProto);
        } else if (classHeritage instanceof NullLiteral) {
            mv.loadExecutionContext();
            mv.invoke(Methods.ClassOperations_getClassProto_Null);
        } else {
            expressionBoxed(classHeritage, mv);
            mv.loadExecutionContext();
            mv.lineInfo(def);
            mv.invoke(Methods.ClassOperations_getClassProto);
        }

        // stack: [<protoParent,constructorParent>] -> [<protoParent,constructorParent>]
        Variable<OrdinaryObject> proto = mv.newVariable("proto", OrdinaryObject.class);
        mv.dup();
        mv.aload(0, Types.ScriptObject);
        mv.loadExecutionContext();
        mv.invoke(Methods.ClassOperations_createProto);
        mv.store(proto);

        // stack: [<protoParent,constructorParent>] -> [constructorParent, proto]
        mv.aload(1, Types.ScriptObject);
        mv.load(proto);

        // Push the private-name environment to ensure private names are accessible in the constructor.
        BlockScope bodyScope = def.getBodyScope();
        if (bodyScope != null) {
            List<Name> privateBoundNames = PrivateBoundNames(def);
            assert bodyScope.isPresent() == !privateBoundNames.isEmpty();

            if (bodyScope.isPresent()) {
                // stack: [] -> [classPrivateEnv]
                newDeclarativeEnvironment(bodyScope, mv);

                Variable<DeclarativeEnvironmentRecord> classPrivateEnvRec = mv.newVariable("classPrivateEnvRec",
                        DeclarativeEnvironmentRecord.class);
                getEnvRec(classPrivateEnvRec, mv);

                HashSet<Name> declaredPrivateNames = new HashSet<>();
                for (Name name : privateBoundNames) {
                    // FIXME: spec bug - missing check for already declared private names for getter/setter pairs
                    if (declaredPrivateNames.add(name)) {
                        BindingOp<DeclarativeEnvironmentRecord> op = BindingOp.of(classPrivateEnvRec, name);
                        op.createImmutableBinding(classPrivateEnvRec, name, true, mv);
                    }
                }

                // stack: [classPrivateEnv] -> []
                pushLexicalEnvironment(mv);
            }
            mv.enterScope(bodyScope);
        }

        // steps 8-9
        // stack: [constructorParent, proto] -> [constructorParent, proto, <rti>]
        MethodName method = mv.compile(def, codegen::classDefinition);
        // Runtime Semantics: Evaluation -> MethodDefinition
        mv.invoke(method);

        // step 10 (not applicable)
        // steps 11-18
        // stack: [constructorParent, proto, <rti>] -> [F]
        mv.iconst(classHeritage != null);
        mv.loadExecutionContext();
        mv.lineInfo(def);
        mv.invoke(Methods.ClassOperations_EvaluateConstructorMethod);

        // stack: [F] -> []
        Variable<OrdinaryConstructorFunction> F = mv.newVariable("F", OrdinaryConstructorFunction.class);
        mv.store(F);

        // steps 19-21
        ClassPropertyGenerator.Result result = ClassPropertyEvaluation(codegen, def, F, proto, mv);
        Variable<Object[]> methodDecorators = result.methodDecorators;

        if (!classDecoratorsList.isEmpty()) {
            int index = 0;
            for (Expression decorator : classDecoratorsList) {
                mv.aload(classDecorators, index++, Types.Callable);
                invokeDynamicCall(mv, decorator, mv.executionContext(), mv.undefinedValue(), F);
                mv.pop();
            }
        }

        if (methodDecorators != null) {
            LabelledHashKey hashKey = new LabelledHashKey(def, "decorators");
            MethodName decoratorsMethod = mv.compile(hashKey, () -> classMethodDecorators(def, mv));
            mv.lineInfo(0); // 0 = hint for stacktraces to omit this frame
            mv.invoke(decoratorsMethod, mv.executionContext(), F, proto, methodDecorators);
        }

        if (scope != null) {
            // steps 22-23 (moved)
            if (className != null) {
                // stack: [] -> []
                Name innerName = scope.resolveName(className);
                BindingOp<DeclarativeEnvironmentRecord> op = BindingOp.of(classScopeEnvRec, innerName);
                op.initializeBinding(classScopeEnvRec, innerName, F, mv);
            }
        }

        if (result.instanceClassField != null || result.instanceClassMethods != null) {
            MethodName initializer = compileClassFieldInitializer(def, MethodDefinition.MethodAllocation.Prototype, mv);

            // stack: [] -> [F]
            mv.load(F);

            // stack: [F] -> [F, initializer]
            mv.load(proto);
            mv.invoke(initializer);
            if (result.instanceClassField != null) {
                mv.load(result.instanceClassField);
            } else {
                mv.anull();
            }
            if (result.instanceClassMethods != null) {
                mv.load(result.instanceClassMethods);
            } else {
                mv.anull();
            }
            mv.loadExecutionContext();
            mv.invoke(Methods.ClassOperations_CreateClassFieldInitializer);

            // stack: [F, initializer] -> []
            mv.invoke(Methods.ClassOperations_setInstanceFieldsInitializer);
        }

        // Class fields: Call InitializeStaticFields.
        if (result.staticClassField != null) {
            MethodName initializer = compileClassFieldInitializer(def, MethodDefinition.MethodAllocation.Class, mv);

            // stack: [] -> [staticInitializer]
            mv.load(F);
            mv.invoke(initializer);
            mv.load(result.staticClassField);
            mv.loadExecutionContext();
            mv.invoke(Methods.ClassOperations_CreateStaticClassFieldInitializer);

            // stack: [staticInitializer] -> []
            invokeDynamicCall(mv, def, mv.executionContext(), F);
            mv.pop(ValType.Any);
        }

        if (bodyScope != null) {
            mv.exitScope();
            if (bodyScope.isPresent()) {
                popLexicalEnvironment(mv);
            }
        }

        if (scope != null) {
            mv.exitScope();
            if (scope.isPresent()) {
                popLexicalEnvironment(mv);
            }
        }

        // stack: [] -> [F]
        mv.load(F);

        mv.exitVariableScope();

        // step 24 (return F)
        mv.exitClassDefinition();
    }

    private MethodName compileClassFieldInitializer(ClassDefinition def, MethodDefinition.MethodAllocation allocation,
            CodeVisitor mv) {
        Predicate<MethodDefinition> p = f -> f.isSynthetic() && f.getAllocation() == allocation
                && f.getType() == MethodDefinition.MethodType.Function;
        assert def.getMethods().stream().filter(p).count() == 1;

        MethodDefinition initializer = def.getMethods().stream().filter(p).findAny().get();
        return mv.compile(initializer, codegen::methodDefinition);
    }

    protected static final void invokeDynamicCall(InstructionVisitor mv, Node node, Value<ExecutionContext> cx,
            Value<?> thisValue, Value<?>... arguments) {
        mv.load(cx);
        mv.load(thisValue);
        mv.anewarray(Types.Object, arguments);
        mv.lineInfo(node);

        // stack: [func(Callable), cx, thisValue, args] -> [result]
        mv.invokedynamic(Bootstrap.getCallName(), Bootstrap.getCallMethodDescriptor(), Bootstrap.getCallBootstrap());
    }

    protected static final void invokeDynamicCall(InstructionVisitor mv, Node node, Value<?> function,
            Value<ExecutionContext> cx, Value<?> thisValue, Value<?>... arguments) {
        mv.load(function);
        mv.load(cx);
        mv.load(thisValue);
        mv.anewarray(Types.Object, arguments);
        mv.lineInfo(node);

        // stack: [func(Callable), cx, thisValue, args] -> [result]
        mv.invokedynamic(Bootstrap.getCallName(), Bootstrap.getCallMethodDescriptor(), Bootstrap.getCallBootstrap());
    }

    /**
     * Checks if the top-stack element is callable by invoking {@code CallOperations.CheckCallable(...)}.
     * 
     * @param node
     *            the ast node
     * @param mv
     *            the code visitor
     */
    protected static final void CheckCallable(Node node, CodeVisitor mv) {
        // stack: [object] -> [Callable<object>]
        mv.loadExecutionContext();
        mv.lineInfo(node);
        mv.invoke(Methods.CallOperations_CheckCallable);
    }

    private MethodName classMethodDecorators(ClassDefinition node, CodeVisitor parent) {
        MethodTypeDescriptor descriptor = ClassMethodDecoratorsVisitor.methodDescriptor();
        MethodCode method = codegen.method(parent, "classdecorators", descriptor);

        ClassMethodDecoratorsVisitor mv = new ClassMethodDecoratorsVisitor(method);
        mv.begin();
        Variable<ExecutionContext> cx = mv.getExecutionContext();
        Variable<OrdinaryConstructorFunction> constructor = mv.getConstructor();
        Variable<OrdinaryObject> prototype = mv.getPrototype();
        // List of <1..n callable, property key>.
        Variable<Object[]> decorators = mv.getDecorators();

        Variable<Object> propertyKey = mv.newVariable("propertyKey", Object.class);
        Variable<Object> propertyDesc = mv.newVariable("propertyDesc", Object.class);
        Variable<Object> result = mv.newVariable("result", Object.class);

        int index = 0;
        for (MethodDefinition methodDef : DecoratedMethods(node.getMethods())) {
            List<Expression> decoratorsList = methodDef.getDecorators();
            assert !decoratorsList.isEmpty();
            assert !methodDef.isCallConstructor();
            Variable<? extends OrdinaryObject> object;
            if (methodDef.isStatic()) {
                object = constructor;
            } else {
                object = prototype;
            }

            mv.store(propertyKey, mv.arrayElement(decorators, index + decoratorsList.size(), Object.class));

            mv.lineInfo(methodDef);
            mv.invoke(Methods.DecoratorOperations_propertyDescriptor, object, propertyKey, cx);
            mv.store(propertyDesc);

            for (Expression decoratorExpr : decoratorsList) {
                Value<Object> decorator = mv.arrayElement(decorators, index++, Object.class);
                invokeDynamicCall(mv, decoratorExpr, decorator, cx, mv.undefinedValue(), object, propertyKey,
                        propertyDesc);
                mv.store(result);

                Jump isObject = new Jump();
                mv.invoke(Methods.Type_isObject, result);
                mv.ifeq(isObject);
                {
                    mv.store(propertyDesc, result);
                }
                mv.mark(isObject);
            }

            Jump isObject = new Jump();
            mv.invoke(Methods.Type_isObject, propertyDesc);
            mv.ifeq(isObject);
            {
                mv.lineInfo(methodDef);
                mv.invoke(Methods.DecoratorOperations_defineProperty, object, propertyKey, propertyDesc, cx);
            }
            mv.mark(isObject);

            index += 1; // Skip over property key element.
        }
        mv._return();
        mv.end();

        return method.name();
    }

    private static final class ClassMethodDecoratorsVisitor extends InstructionVisitor {
        ClassMethodDecoratorsVisitor(MethodCode method) {
            super(method);
        }

        @Override
        public void begin() {
            super.begin();
            setParameterName("cx", 0, Types.ExecutionContext);
            setParameterName("constructor", 1, Types.OrdinaryConstructorFunction);
            setParameterName("prototype", 2, Types.OrdinaryObject);
            setParameterName("decorators", 3, Types.Object_);
        }

        Variable<ExecutionContext> getExecutionContext() {
            return getParameter(0, ExecutionContext.class);
        }

        Variable<OrdinaryConstructorFunction> getConstructor() {
            return getParameter(1, OrdinaryConstructorFunction.class);
        }

        Variable<OrdinaryObject> getPrototype() {
            return getParameter(2, OrdinaryObject.class);
        }

        Variable<Object[]> getDecorators() {
            return getParameter(3, Object[].class);
        }

        static MethodTypeDescriptor methodDescriptor() {
            return Type.methodType(Type.VOID_TYPE, Types.ExecutionContext, Types.OrdinaryConstructorFunction,
                    Types.OrdinaryObject, Types.Object_);
        }
    }

    static final class StoreToArray<T> implements Value<T> {
        final Variable<T[]> array;
        final PrimitiveIterator.OfInt index;

        private StoreToArray(Variable<T[]> array, PrimitiveIterator.OfInt index) {
            this.array = array;
            this.index = index;
        }

        @Override
        public void load(InstructionAssembler assembler) {
            if (isEmpty()) {
                assembler.anull();
            } else {
                assembler.load(array);
            }
        }

        void store(Value<? extends T> value, CodeVisitor mv) {
            assert !isEmpty();
            mv.astore(array, index.nextInt(), value);
        }

        MutableValue<T> element(Class<T> clazz, CodeVisitor mv) {
            return mv.arrayElement(array, index.nextInt(), clazz);
        }

        void skip(int n) {
            for (int i = 0; i < n; ++i) {
                index.nextInt();
            }
        }

        boolean isEmpty() {
            return array == null;
        }

        StoreToArray<T> from(Variable<T[]> array) {
            if (isEmpty()) {
                return new StoreToArray<>(null, null);
            }
            return new StoreToArray<>(array, index);
        }

        static <T> StoreToArray<T> empty() {
            return new StoreToArray<>(null, null);
        }

        static <T> StoreToArray<T> create(String name, int length, Class<T[]> clazz, CodeVisitor mv) {
            if (length == 0) {
                return empty();
            }
            Variable<T[]> array = mv.newVariable(name, clazz);
            mv.anewarray(length, Type.of(clazz.getComponentType()));
            mv.store(array);
            return new StoreToArray<>(array, IntStream.range(0, length).iterator());
        }
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
     *            the code visitor
     */
    protected final void delegatedYield(Expression node, CodeVisitor mv) {
        if (!mv.isAsync()) {
            delegatedYield(node, () -> {
                mv.loadExecutionContext();
                mv.invoke(Methods.IteratorOperations_iterate);
            }, (iterator, received) -> {
                mv.loadExecutionContext();
                mv.load(iterator);
                mv.load(received);
                mv.lineInfo(node);
                mv.invoke(Methods.AbstractOperations_IteratorNext_Object);
            }, (iterator, received) -> {
                mv.loadExecutionContext();
                mv.load(iterator);
                mv.load(received);
                mv.checkcast(Types.ScriptException);
                mv.invoke(Methods.Operators_yieldThrowCompletion);
            }, (iterator, received) -> {
                mv.loadExecutionContext();
                mv.load(iterator);
                mv.load(received);
                mv.checkcast(Types.ReturnValue);
                mv.invoke(Methods.Operators_yieldReturnCompletion);
            }, (result, received) -> {
                // force stack top to Object-type
                mv.load(result);
                mv.checkcast(Types.Object);
                mv.suspend();
                mv.store(received);
            }, received -> {
                mv.load(received);
                mv.checkcast(Types.ReturnValue);
                mv.invoke(Methods.ReturnValue_getValue);
                mv.store(received);
            }, mv);
        } else {
            delegatedYield(node, () -> {
                mv.loadExecutionContext();
                mv.invoke(Methods.IteratorOperations_asyncIterate);
            }, (iterator, received) -> {
                mv.load(iterator);
                mv.load(received);
                mv.invoke(Methods.ScriptIterator_nextIterResult);
                await(node, mv);
                requireObjectResult(node, "next", mv);
            }, (iterator, received) -> {
                mv.enterVariableScope();
                Variable<Callable> throwMethod = mv.newVariable("throwMethod", Callable.class);

                GetMethod(node, iterator, "throw", mv);
                mv.store(throwMethod);

                Jump noThrow = new Jump(), nextYield = new Jump();
                mv.load(throwMethod);
                mv.ifnull(noThrow);
                {
                    InvokeMethod(node, mv, throwMethod, iterator, __ -> {
                        mv.load(received);
                        mv.checkcast(Types.ScriptException);
                        mv.invoke(Methods.ScriptException_getValue);
                    });
                    await(node, mv);
                    requireObjectResult(node, "throw", mv);
                    mv.goTo(nextYield);
                }
                mv.mark(noThrow);
                {
                    asyncIteratorClose(node, iterator, mv);

                    mv.aconst("throw");
                    mv.loadExecutionContext();
                    mv.lineInfo(node);
                    mv.invoke(Methods.Operators_reportPropertyNotCallable);
                    mv.athrow();
                }
                mv.mark(nextYield);

                mv.exitVariableScope();
            }, (iterator, received) -> {
                mv.enterVariableScope();
                Variable<Callable> returnMethod = mv.newVariable("returnMethod", Callable.class);

                GetMethod(node, iterator, "return", mv);
                mv.store(returnMethod);

                Jump noReturn = new Jump(), nextYield = new Jump();
                mv.load(returnMethod);
                mv.ifnull(noReturn);
                {
                    InvokeMethod(node, mv, returnMethod, iterator, __ -> {
                        mv.load(received);
                        mv.checkcast(Types.ReturnValue);
                        mv.invoke(Methods.ReturnValue_getValue);
                    });
                    await(node, mv);
                    requireObjectResult(node, "return", mv);
                    mv.goTo(nextYield);
                }
                mv.mark(noReturn);
                {
                    mv.anull();
                }
                mv.mark(nextYield);

                mv.exitVariableScope();
            }, (result, received) -> {
                Jump storeReceived = new Jump();
                Runnable await = () -> {
                    // stack: [value] -> [value']
                    mv.loadExecutionContext();
                    mv.swap();
                    mv.lineInfo(node);
                    mv.invoke(Methods.AsyncAbstractOperations_AsyncFunctionAwait);

                    // Reserve stack space for await return value.
                    mv.anull();
                    mv.suspend();

                    // check for exception
                    mv.dup();
                    mv.instanceOf(Types.ScriptException);
                    mv.ifne(storeReceived);
                };
                Runnable asyncYield = () -> {
                    // stack: [value] -> [value']
                    await.run();

                    // force stack top to Object-type
                    mv.checkcast(Types.Object);
                    mv.suspend();

                    // check for return value
                    mv.dup();
                    mv.instanceOf(Types.ReturnValue);
                    mv.ifeq(storeReceived);
                    {
                        mv.checkcast(Types.ReturnValue);
                        mv.invoke(Methods.ReturnValue_getValue);

                        // stack: [value] -> [value']
                        await.run();

                        // stack: [value'] -> [ReturnValue(value')]
                        mv.anew(Types.ReturnValue);
                        mv.dup();
                        mv.swap1_2();
                        mv.invoke(Methods.ReturnValue_new);
                    }
                };

                IteratorValue(node, result, mv);
                asyncYield.run();
                mv.mark(storeReceived);
                mv.store(received);
            }, received -> {
                mv.load(received);
                mv.checkcast(Types.ReturnValue);
                mv.invoke(Methods.ReturnValue_getValue);
                await(node, mv);
                mv.store(received);
            }, mv);
        }
    }

    private void delegatedYield(Expression node, Runnable getIterator,
            BiConsumer<Variable<ScriptIterator<?>>, Variable<Object>> iterNext,
            BiConsumer<Variable<ScriptObject>, Variable<Object>> iterThrow,
            BiConsumer<Variable<ScriptObject>, Variable<Object>> iterReturn,
            BiConsumer<Variable<ScriptObject>, Variable<Object>> yield, Consumer<Variable<Object>> returnValue,
            CodeVisitor mv) {
        Jump iteratorNext = new Jump();
        Jump generatorYield = new Jump();
        Jump done = new Jump();

        mv.lineInfo(node);
        mv.enterVariableScope();
        Variable<ScriptIterator<?>> iteratorRec = mv.newVariable("iteratorRec", ScriptIterator.class).uncheckedCast();
        Variable<ScriptObject> iterator = mv.newVariable("iterator", ScriptObject.class);
        Variable<ScriptObject> innerResult = mv.newVariable("innerResult", ScriptObject.class);
        Variable<Object> received = mv.newVariable("received", Object.class);

        /* steps 1-2 (callers) */
        /* step 3 */
        // stack: [value] -> []
        getIterator.run();
        mv.store(iteratorRec);

        mv.load(iteratorRec);
        mv.invoke(Methods.ScriptIterator_getScriptObject);
        mv.store(iterator);

        /* step 4 */
        // stack: [] -> []
        mv.loadUndefined();
        mv.store(received);

        /* step 5.a.i */
        // stack: [] -> []
        mv.mark(iteratorNext);
        iterNext.accept(iteratorRec, received);
        mv.store(innerResult);

        /* steps 5.a.ii-iii */
        // stack: [] -> []
        IteratorComplete(node, innerResult, mv);
        mv.ifne(done);

        /* steps 5.a.iv, 5.b.ii.6, 5.c.viii */
        mv.mark(generatorYield);
        yield.accept(innerResult, received);

        /* step 5.b */
        Jump isException = new Jump();
        mv.load(received);
        mv.instanceOf(Types.ScriptException);
        mv.ifeq(isException);
        {
            /* steps 5.b.i, 5.b.ii.1-3, 5.b.iii */
            iterThrow.accept(iterator, received);
            mv.store(innerResult);

            /* steps 5.b.ii.4-6 */
            IteratorComplete(node, innerResult, mv);
            mv.ifeq(generatorYield);
            mv.goTo(done);
        }
        mv.mark(isException);

        /* step 5.c */
        mv.load(received);
        mv.instanceOf(Types.ReturnValue);
        mv.ifeq(iteratorNext);
        {
            /* steps 5.c.i-v */
            Jump returnCompletion = new Jump(), returnIterResult = new Jump();
            iterReturn.accept(iterator, received);
            mv.store(innerResult);
            mv.load(innerResult);
            mv.ifnonnull(returnIterResult);

            /* step 5.c.iii */
            returnValue.accept(received);
            mv.goTo(returnCompletion);

            /* steps 5.c.vi-viii */
            mv.mark(returnIterResult);
            IteratorComplete(node, innerResult, mv);
            mv.ifeq(generatorYield);

            /* step 5.c.vii.1 */
            IteratorValue(node, innerResult, mv);
            mv.store(received);

            /* steps 5.c.iii, step 5.c.vii */
            mv.mark(returnCompletion);
            mv.popStack();
            mv.returnCompletion(received);
        }

        /* steps 5.a.iii, 5.b.ii.5 */
        mv.mark(done);
        IteratorValue(node, innerResult, mv);

        mv.exitVariableScope();
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
     *            the code visitor
     */
    protected final void yield(Expression node, CodeVisitor mv) {
        assert mv.isGenerator();

        if (!mv.isAsync()) {
            mv.lineInfo(node);
            mv.loadExecutionContext();
            mv.swap();
            mv.iconst(false);
            mv.invoke(Methods.AbstractOperations_CreateIterResultObject);
        } else {
            // TODO: Move awaiting before and after yield to runtime?
            await(node, mv);
        }

        // force stack top to Object-type
        mv.checkcast(Types.Object);
        mv.suspend();

        // check for exception
        throwAfterResume(mv);

        // check for return value
        returnAfterResume(node, mv);
    }

    /**
     * Extension: Async Function Definitions
     * <p>
     * stack: [value] {@literal ->} [value']
     * 
     * @param node
     *            the expression node
     * @param mv
     *            the code visitor
     */
    protected final void await(Node node, CodeVisitor mv) {
        // stack: [value] -> [value']
        mv.loadExecutionContext();
        mv.swap();
        mv.lineInfo(node);
        mv.invoke(Methods.AsyncAbstractOperations_AsyncFunctionAwait);

        // Reserve stack space for await return value.
        mv.anull();
        mv.suspend();

        // check for exception
        throwAfterResume(mv);
    }

    private void throwAfterResume(CodeVisitor mv) {
        Jump isException = new Jump();
        mv.dup();
        mv.instanceOf(Types.ScriptException);
        mv.ifeq(isException);
        {
            mv.checkcast(Types.ScriptException);
            mv.athrow();
        }
        mv.mark(isException);
    }

    private void returnAfterResume(Node node, CodeVisitor mv) {
        Jump isReturn = new Jump();
        mv.dup();
        mv.instanceOf(Types.ReturnValue);
        mv.ifeq(isReturn);
        {
            mv.checkcast(Types.ReturnValue);
            mv.invoke(Methods.ReturnValue_getValue);

            if (mv.isAsyncGenerator()) {
                await(node, mv);
            }

            if (mv.getStackSize() == 1) {
                mv.returnCompletion();
            } else {
                mv.enterVariableScope();
                Variable<Object> returnValue = mv.newVariable("returnValue", Object.class);
                mv.store(returnValue);
                mv.popStack();
                mv.returnCompletion(returnValue);
                mv.exitVariableScope();
            }
        }
        mv.mark(isReturn);
    }

    /**
     * IteratorComplete (iterResult)
     * <p>
     * stack: [] {@literal ->} [complete]
     * 
     * @param node
     *            the ast node
     * @param iterResult
     *            the iterator result object
     * @param mv
     *            the code visitor
     */
    protected final void IteratorComplete(Node node, Variable<ScriptObject> iterResult, CodeVisitor mv) {
        mv.loadExecutionContext();
        mv.load(iterResult);
        mv.lineInfo(node);
        mv.invoke(Methods.AbstractOperations_IteratorComplete);
    }

    /**
     * IteratorValue (iterResult)
     * <p>
     * stack: [] {@literal ->} [value]
     * 
     * @param node
     *            the ast node
     * @param iterResult
     *            the iterator result object
     * @param mv
     *            the code visitor
     */
    protected final void IteratorValue(Node node, Variable<ScriptObject> iterResult, CodeVisitor mv) {
        mv.loadExecutionContext();
        mv.load(iterResult);
        mv.lineInfo(node);
        mv.invoke(Methods.AbstractOperations_IteratorValue);
    }

    /**
     * GetMethod (O, P)
     * <p>
     * stack: [] {@literal ->} [method]
     * 
     * @param node
     *            the ast node
     * @param object
     *            the script object
     * @param methodName
     *            the method name
     * @param mv
     *            the code visitor
     */
    private void GetMethod(Node node, Variable<ScriptObject> object, String methodName, CodeVisitor mv) {
        mv.loadExecutionContext();
        mv.load(object);
        mv.aconst(methodName);
        mv.lineInfo(node);
        mv.invoke(Methods.AbstractOperations_GetMethod);
    }

    /**
     * Emit: {@code method.call(cx, thisValue, arguments)}
     * 
     * @param node
     *            the ast node
     * @param mv
     *            the code visitor
     * @param method
     *            the callable object
     * @param thisValue
     *            the call this-value
     * @param arguments
     *            the method call arguments
     */
    private void InvokeMethod(Node node, CodeVisitor mv, Value<Callable> method, Value<?> thisValue,
            Value<?>... arguments) {
        mv.load(method);
        mv.loadExecutionContext();
        mv.load(thisValue);
        if (arguments.length == 0) {
            mv.get(Fields.CallOperations_EMPTY_ARRAY);
        } else {
            mv.anewarray(Types.Object, arguments);
        }
        mv.lineInfo(node);
        mv.invokedynamic(Bootstrap.getCallName(), Bootstrap.getCallMethodDescriptor(), Bootstrap.getCallBootstrap());
    }

    /**
     * Invoke(O, P, [argumentsList])
     * <p>
     * stack: [] {@literal ->} [value]
     * 
     * @param node
     *            the ast node
     * @param mv
     *            the code visitor
     * @param object
     *            the script object
     * @param methodName
     *            the method name
     * @param arguments
     *            the method call arguments
     */
    protected final void Invoke(Node node, CodeVisitor mv, Variable<ScriptObject> object, String methodName,
            Value<?>... arguments) {
        mv.loadExecutionContext();
        mv.load(object);
        mv.aconst(methodName);
        if (arguments.length == 0) {
            mv.get(Fields.CallOperations_EMPTY_ARRAY);
        } else {
            mv.anewarray(Types.Object, arguments);
        }
        mv.lineInfo(node);
        mv.invoke(Methods.AbstractOperations_Invoke);
    }

    /**
     * Emit:
     * 
     * <pre>
     * Callable returnMethod = GetMethod(cx, iterator, "return");
     * if (returnMethod != null) {
     *   Object innerResult = returnMethod.call(cx, iterator);
     *   await;
     *   if (!(innerResult instanceof ScriptObject)) {
     *     throw newTypeError(cx, Messages.Key.NotObjectTypeReturned, "return");
     *   }
     * }
     * </pre>
     * 
     * @param node
     *            the ast node
     * @param iterator
     *            the script iterator object
     * @param mv
     *            the code visitor
     */
    final void asyncIteratorClose(Node node, Variable<ScriptObject> iterator, CodeVisitor mv) {
        /* steps 1-2 (not applicable) */
        /* steps 3-4 */
        IteratorClose(node, iterator, returnMethod -> {
            /* step 5 */
            InvokeMethod(node, mv, returnMethod, iterator);
            /* step 6 */
            await(node, mv);
            /* step 7 (not applicable) */
            /* step 8 (implicit) */
            /* step 9 */
            requireObjectResult(node, "return", mv);
            mv.pop();
        }, mv);
    }

    /**
     * Emit:
     * 
     * <pre>
     * Callable returnMethod = GetMethod(cx, iterator, "return");
     * if (returnMethod != null) {
     *   try {
     *     returnMethod.call(cx, iterator);
     *     await;
     *   } catch (ScriptException e) {
     *     if (throwable != e) {
     *       throwable.addSuppressed(e);
     *     }
     *   }
     * }
     * </pre>
     * 
     * @param node
     *            the ast node
     * @param iterator
     *            the script iterator object
     * @param mv
     *            the code visitor
     */
    final void asyncIteratorClose(Node node, Variable<ScriptObject> iterator, Variable<? extends Throwable> throwable,
            CodeVisitor mv) {
        /* steps 1-2 (not applicable) */
        /* steps 3-4 */
        IteratorClose(node, iterator, returnMethod -> {
            TryCatchLabel startCatch = new TryCatchLabel();
            TryCatchLabel endCatch = new TryCatchLabel(), handlerCatch = new TryCatchLabel();
            Jump noException = new Jump();

            mv.mark(startCatch);
            {
                /* step 5 */
                InvokeMethod(node, mv, returnMethod, iterator);
                /* step 6 */
                await(node, mv);
                mv.pop();

                mv.goTo(noException);
            }
            mv.mark(endCatch);

            mv.catchHandler(handlerCatch, Types.ScriptException);
            {
                mv.enterVariableScope();
                Variable<ScriptException> exception = mv.newVariable("exception", ScriptException.class);
                mv.store(exception);

                mv.load(throwable);
                mv.load(exception);
                mv.ifacmpeq(noException);
                {
                    mv.load(throwable);
                    mv.load(exception);
                    mv.invoke(Methods.Throwable_addSuppressed);
                }

                mv.exitVariableScope();
            }
            mv.tryCatch(startCatch, endCatch, handlerCatch, Types.ScriptException);
            mv.mark(noException);

            /* step 7 (in caller) */
            /* steps 8-10 (not applicable) */
        }, mv);
    }

    /**
     * <pre>
     * Callable returnMethod = GetMethod(cx, iterator, "return");
     * if (returnMethod != null) {
     *   &lt;invoke return&gt;
     * }
     * </pre>
     * 
     * @param node
     *            the ast node
     * @param iterator
     *            the script iterator object
     * @param invokeReturn
     *            the code snippet to invoke return()
     * @param mv
     *            the code visitor
     */
    private void IteratorClose(Node node, Variable<ScriptObject> iterator, Consumer<Variable<Callable>> invokeReturn,
            CodeVisitor mv) {
        mv.enterVariableScope();
        Variable<Callable> returnMethod = mv.newVariable("returnMethod", Callable.class);

        GetMethod(node, iterator, "return", mv);
        mv.store(returnMethod);

        Jump done = new Jump();
        mv.load(returnMethod);
        mv.ifnull(done);
        {
            invokeReturn.accept(returnMethod);
        }
        mv.mark(done);

        mv.exitVariableScope();
    }

    /**
     * Extension: Async Generator Function Definitions
     * <p>
     * stack: [value] {@literal ->} []
     * 
     * @param node
     *            the ast node
     * @param methodName
     *            the method name
     * @param mv
     *            the code visitor
     */
    protected final void requireObjectResult(Node node, String methodName, CodeVisitor mv) {
        mv.aconst(methodName);
        mv.loadExecutionContext();
        mv.lineInfo(node);
        mv.invoke(Methods.Operators_requireObjectResult);
    }

    /**
     * Compiles an outlined method.
     * 
     * @param mv
     *            the code visitor
     * @param compiler
     *            the compiler function
     * @return the outlined-call object
     */
    protected final <VISITOR extends OutlinedCodeVisitor> OutlinedCall outlined(VISITOR mv,
            Function<VISITOR, Completion> compiler) {
        mv.lineInfo(mv.getNode());
        mv.nop(); // force line-number entry
        mv.begin();
        GeneratorState generatorState = null;
        if (mv.hasResume()) {
            generatorState = mv.generatorPrologue();
        }
        mv.labelPrologue();

        Completion result = compiler.apply(mv);
        if (!result.isAbrupt()) {
            // fall-thru, return `0`.
            mv.iconst(0);
            mv._return();
        }

        LabelState labelState = mv.labelEpilogue(result, mv.hasResume());
        if (generatorState != null) {
            mv.generatorEpilogue(generatorState);
        }
        mv.end();
        return new OutlinedCall(mv.getMethod().name(), labelState);
    }

    /**
     * If <var>node</var> evaluates to {@code true}, continue execution; otherwise jump to <var>ifFalse</var>.
     * 
     * @param node
     *            the expression node
     * @param ifFalse
     *            the target label
     * @param mv
     *            the code visitor
     */
    protected final void testExpressionBailout(Expression node, Jump ifFalse, CodeVisitor mv) {
        Jump ifTrue = new Jump();
        if (testExpression(node, ifTrue, ifFalse, mv)) {
            mv.ifeq(ifFalse);
        } else {
            mv.ifne(ifFalse);
        }
        mv.mark(ifTrue);
    }

    /**
     * If <var>node</var> evaluates to {@code true}, jump to <var>ifTrue</var>; otherwise continue execution.
     * 
     * @param node
     *            the expression node
     * @param ifFalse
     *            the target label
     * @param mv
     *            the code visitor
     */
    protected final void testExpressionFallthrough(Expression node, Jump ifTrue, CodeVisitor mv) {
        Jump ifFalse = new Jump();
        if (testExpression(node, ifTrue, ifFalse, mv)) {
            mv.ifne(ifTrue);
        } else {
            mv.ifeq(ifTrue);
        }
        mv.mark(ifFalse);
    }

    private boolean testExpression(Expression node, Jump ifTrue, Jump ifFalse, CodeVisitor mv) {
        if (node instanceof BinaryExpression) {
            BinaryExpression binary = (BinaryExpression) node;
            if (binary.getOperator() == BinaryExpression.Operator.AND) {
                testExpressionBailout(binary.getLeft(), ifFalse, mv);
                return testExpression(binary.getRight(), ifTrue, ifFalse, mv);
            } else if (binary.getOperator() == BinaryExpression.Operator.OR) {
                testExpressionFallthrough(binary.getLeft(), ifTrue, mv);
                return testExpression(binary.getRight(), ifTrue, ifFalse, mv);
            }
        } else if (node instanceof UnaryExpression) {
            UnaryExpression unary = (UnaryExpression) node;
            if (unary.getOperator() == UnaryExpression.Operator.NOT) {
                return !testExpression(unary.getOperand(), ifFalse, ifTrue, mv);
            }
        }
        ValType type = codegen.expression(node, mv);
        ToBoolean(type, mv);
        return true;
    }
}
