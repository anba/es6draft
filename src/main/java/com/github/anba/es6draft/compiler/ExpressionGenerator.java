/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static com.github.anba.es6draft.semantics.StaticSemantics.BoundNames;
import static com.github.anba.es6draft.semantics.StaticSemantics.IsAnonymousFunctionDefinition;
import static com.github.anba.es6draft.semantics.StaticSemantics.IsIdentifierRef;
import static com.github.anba.es6draft.semantics.StaticSemantics.Substitutions;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.objectweb.asm.Label;
import org.objectweb.asm.Type;

import com.github.anba.es6draft.ast.*;
import com.github.anba.es6draft.ast.synthetic.ElementAccessorValue;
import com.github.anba.es6draft.ast.synthetic.ExpressionMethod;
import com.github.anba.es6draft.ast.synthetic.IdentifierValue;
import com.github.anba.es6draft.ast.synthetic.PropertyAccessorValue;
import com.github.anba.es6draft.ast.synthetic.SpreadArrayLiteral;
import com.github.anba.es6draft.ast.synthetic.SpreadElementMethod;
import com.github.anba.es6draft.ast.synthetic.SuperExpressionValue;
import com.github.anba.es6draft.compiler.CodeGenerator.FunctionName;
import com.github.anba.es6draft.compiler.DefaultCodeGenerator.ValType;
import com.github.anba.es6draft.compiler.InstructionVisitor.FieldDesc;
import com.github.anba.es6draft.compiler.InstructionVisitor.FieldType;
import com.github.anba.es6draft.compiler.InstructionVisitor.MethodDesc;
import com.github.anba.es6draft.compiler.InstructionVisitor.MethodType;
import com.github.anba.es6draft.runtime.internal.Bootstrap;
import com.github.anba.es6draft.runtime.objects.Eval.EvalFlags;
import com.github.anba.es6draft.runtime.types.builtins.ExoticArray;

/**
 *
 */
final class ExpressionGenerator extends DefaultCodeGenerator<ValType, ExpressionVisitor> {
    private static final class Fields {
        static final FieldDesc Intrinsics_ObjectPrototype = FieldDesc.create(FieldType.Static,
                Types.Intrinsics, "ObjectPrototype", Types.Intrinsics);

        static final FieldDesc ScriptRuntime_EMPTY_ARRAY = FieldDesc.create(FieldType.Static,
                Types.ScriptRuntime, "EMPTY_ARRAY", Types.Object_);
    }

    private static final class Methods {
        // class: AbstractOperations
        static final MethodDesc AbstractOperations_Put = MethodDesc.create(MethodType.Static,
                Types.AbstractOperations, "Put", Type.getMethodType(Type.VOID_TYPE,
                        Types.ExecutionContext, Types.ScriptObject, Types.String, Types.Object,
                        Type.BOOLEAN_TYPE));

        // class: Eval
        static final MethodDesc Eval_directEval = MethodDesc.create(MethodType.Static, Types.Eval,
                "directEval", Type.getMethodType(Types.Object, Types.Object_,
                        Types.ExecutionContext, Type.INT_TYPE));

        // class: EnvironmentRecord
        static final MethodDesc EnvironmentRecord_createMutableBinding = MethodDesc.create(
                MethodType.Interface, Types.EnvironmentRecord, "createMutableBinding",
                Type.getMethodType(Type.VOID_TYPE, Types.String, Type.BOOLEAN_TYPE));

        static final MethodDesc EnvironmentRecord_withBaseObject = MethodDesc.create(
                MethodType.Interface, Types.EnvironmentRecord, "withBaseObject",
                Type.getMethodType(Types.ScriptObject));

        // class: ExecutionContext
        static final MethodDesc ExecutionContext_resolveThisBinding = MethodDesc.create(
                MethodType.Virtual, Types.ExecutionContext, "resolveThisBinding",
                Type.getMethodType(Types.Object));

        // class: ExoticArray
        static final MethodDesc ExoticArray_ArrayCreate = MethodDesc.create(MethodType.Static,
                Types.ExoticArray, "ArrayCreate",
                Type.getMethodType(Types.ExoticArray, Types.ExecutionContext, Type.LONG_TYPE));

        static final MethodDesc ExoticArray_DenseArrayCreate = MethodDesc.create(MethodType.Static,
                Types.ExoticArray, "DenseArrayCreate",
                Type.getMethodType(Types.ExoticArray, Types.ExecutionContext, Types.Object_));

        static final MethodDesc ExoticArray_SparseArrayCreate = MethodDesc.create(
                MethodType.Static, Types.ExoticArray, "SparseArrayCreate",
                Type.getMethodType(Types.ExoticArray, Types.ExecutionContext, Types.Object_));

        // class: LexicalEnvironment
        static final MethodDesc LexicalEnvironment_getEnvRec = MethodDesc.create(
                MethodType.Virtual, Types.LexicalEnvironment, "getEnvRec",
                Type.getMethodType(Types.EnvironmentRecord));

        // class: OrdinaryObject
        static final MethodDesc OrdinaryObject_ObjectCreate = MethodDesc.create(MethodType.Static,
                Types.OrdinaryObject, "ObjectCreate",
                Type.getMethodType(Types.OrdinaryObject, Types.ExecutionContext, Types.Intrinsics));

        // class: Reference
        static final MethodDesc Reference_getBase = MethodDesc.create(MethodType.Virtual,
                Types.Reference, "getBase", Type.getMethodType(Types.Object));

        static final MethodDesc Reference_getThisValue = MethodDesc.create(MethodType.Virtual,
                Types.Reference, "getThisValue",
                Type.getMethodType(Types.Object, Types.ExecutionContext));

        static final MethodDesc Reference_getValue = MethodDesc.create(MethodType.Virtual,
                Types.Reference, "getValue",
                Type.getMethodType(Types.Object, Types.ExecutionContext));

        static final MethodDesc Reference_putValue = MethodDesc.create(MethodType.Virtual,
                Types.Reference, "putValue",
                Type.getMethodType(Type.VOID_TYPE, Types.Object, Types.ExecutionContext));

        // class: RegExpConstructor
        static final MethodDesc RegExpConstructor_RegExpCreate = MethodDesc.create(
                MethodType.Static, Types.RegExpConstructor, "RegExpCreate", Type.getMethodType(
                        Types.RegExpObject, Types.ExecutionContext, Types.Object, Types.Object));

        // class: ScriptRuntime
        static final MethodDesc ScriptRuntime_add_str = MethodDesc.create(MethodType.Static,
                Types.ScriptRuntime, "add", Type.getMethodType(Types.CharSequence,
                        Types.CharSequence, Types.CharSequence, Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_delete = MethodDesc.create(MethodType.Static,
                Types.ScriptRuntime, "delete",
                Type.getMethodType(Type.BOOLEAN_TYPE, Types.Object, Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_in = MethodDesc.create(MethodType.Static,
                Types.ScriptRuntime, "in", Type.getMethodType(Type.BOOLEAN_TYPE, Types.Object,
                        Types.Object, Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_typeof = MethodDesc.create(MethodType.Static,
                Types.ScriptRuntime, "typeof",
                Type.getMethodType(Types.String, Types.Object, Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_InstanceofOperator = MethodDesc.create(
                MethodType.Static, Types.ScriptRuntime, "InstanceofOperator", Type.getMethodType(
                        Type.BOOLEAN_TYPE, Types.Object, Types.Object, Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_ArrayAccumulationSpreadElement = MethodDesc.create(
                MethodType.Static, Types.ScriptRuntime, "ArrayAccumulationSpreadElement", Type
                        .getMethodType(Type.INT_TYPE, Types.ScriptObject, Type.INT_TYPE,
                                Types.Object, Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_CheckCallable = MethodDesc.create(MethodType.Static,
                Types.ScriptRuntime, "CheckCallable",
                Type.getMethodType(Types.Callable, Types.Object, Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_defineProperty__int = MethodDesc.create(
                MethodType.Static, Types.ScriptRuntime, "defineProperty", Type.getMethodType(
                        Type.VOID_TYPE, Types.ScriptObject, Type.INT_TYPE, Types.Object,
                        Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_directEvalFallbackArguments = MethodDesc.create(
                MethodType.Static, Types.ScriptRuntime, "directEvalFallbackArguments",
                Type.getMethodType(Types.Object_, Types.Object_, Types.Object, Types.Callable));

        static final MethodDesc ScriptRuntime_directEvalFallbackHook = MethodDesc.create(
                MethodType.Static, Types.ScriptRuntime, "directEvalFallbackHook",
                Type.getMethodType(Types.Callable, Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_ensureObject = MethodDesc.create(MethodType.Static,
                Types.ScriptRuntime, "ensureObject",
                Type.getMethodType(Types.ScriptObject, Types.Object, Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_EvaluateArrowFunction = MethodDesc.create(
                MethodType.Static, Types.ScriptRuntime, "EvaluateArrowFunction", Type
                        .getMethodType(Types.OrdinaryFunction, Types.RuntimeInfo$Function,
                                Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_EvaluateConstructorCall = MethodDesc.create(
                MethodType.Static, Types.ScriptRuntime, "EvaluateConstructorCall", Type
                        .getMethodType(Types.Object, Types.Object, Types.Object_,
                                Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_EvaluateConstructorTailCall = MethodDesc.create(
                MethodType.Static, Types.ScriptRuntime, "EvaluateConstructorTailCall", Type
                        .getMethodType(Types.Object, Types.Object, Types.Object_,
                                Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_EvaluateFunctionExpression = MethodDesc.create(
                MethodType.Static, Types.ScriptRuntime, "EvaluateFunctionExpression", Type
                        .getMethodType(Types.OrdinaryFunction, Types.RuntimeInfo$Function,
                                Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_EvaluateGeneratorComprehension = MethodDesc.create(
                MethodType.Static, Types.ScriptRuntime, "EvaluateGeneratorComprehension", Type
                        .getMethodType(Types.ScriptObject, Types.RuntimeInfo$Function,
                                Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_EvaluateLegacyGeneratorComprehension = MethodDesc
                .create(MethodType.Static, Types.ScriptRuntime,
                        "EvaluateLegacyGeneratorComprehension", Type.getMethodType(
                                Types.ScriptObject, Types.RuntimeInfo$Function,
                                Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_EvaluateGeneratorExpression = MethodDesc.create(
                MethodType.Static, Types.ScriptRuntime, "EvaluateGeneratorExpression", Type
                        .getMethodType(Types.OrdinaryGenerator, Types.RuntimeInfo$Function,
                                Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_EvaluateLegacyGeneratorExpression = MethodDesc
                .create(MethodType.Static, Types.ScriptRuntime,
                        "EvaluateLegacyGeneratorExpression", Type.getMethodType(
                                Types.OrdinaryGenerator, Types.RuntimeInfo$Function,
                                Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_getElement = MethodDesc.create(MethodType.Static,
                Types.ScriptRuntime, "getElement", Type.getMethodType(Types.Reference,
                        Types.Object, Types.Object, Types.ExecutionContext, Type.BOOLEAN_TYPE));

        static final MethodDesc ScriptRuntime_getElementValue = MethodDesc.create(
                MethodType.Static, Types.ScriptRuntime, "getElementValue", Type.getMethodType(
                        Types.Object, Types.Object, Types.Object, Types.ExecutionContext,
                        Type.BOOLEAN_TYPE));

        static final MethodDesc ScriptRuntime_getProperty = MethodDesc.create(MethodType.Static,
                Types.ScriptRuntime, "getProperty", Type.getMethodType(Types.Reference,
                        Types.Object, Types.String, Types.ExecutionContext, Type.BOOLEAN_TYPE));

        static final MethodDesc ScriptRuntime_getPropertyValue = MethodDesc.create(
                MethodType.Static, Types.ScriptRuntime, "getPropertyValue", Type.getMethodType(
                        Types.Object, Types.Object, Types.String, Types.ExecutionContext,
                        Type.BOOLEAN_TYPE));

        static final MethodDesc ScriptRuntime_IsBuiltinEval = MethodDesc.create(MethodType.Static,
                Types.ScriptRuntime, "IsBuiltinEval",
                Type.getMethodType(Type.BOOLEAN_TYPE, Types.Callable, Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_MakeSuperReference = MethodDesc.create(
                MethodType.Static, Types.ScriptRuntime, "MakeSuperReference", Type.getMethodType(
                        Types.Reference, Types.ExecutionContext, Types.Object, Type.BOOLEAN_TYPE));

        static final MethodDesc ScriptRuntime_MakeStringSuperReference = MethodDesc.create(
                MethodType.Static, Types.ScriptRuntime, "MakeSuperReference", Type.getMethodType(
                        Types.Reference, Types.ExecutionContext, Types.String, Type.BOOLEAN_TYPE));

        static final MethodDesc ScriptRuntime_PrepareForTailCall = MethodDesc.create(
                MethodType.Static, Types.ScriptRuntime, "PrepareForTailCall",
                Type.getMethodType(Types.Object, Types.Object_, Types.Object, Types.Callable));

        static final MethodDesc ScriptRuntime_SpreadArray = MethodDesc.create(MethodType.Static,
                Types.ScriptRuntime, "SpreadArray",
                Type.getMethodType(Types.Object_, Types.Object, Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_toFlatArray = MethodDesc.create(MethodType.Static,
                Types.ScriptRuntime, "toFlatArray",
                Type.getMethodType(Types.Object_, Types.Object_, Types.ExecutionContext));

        // class: StringBuilder
        static final MethodDesc StringBuilder_append_Charsequence = MethodDesc.create(
                MethodType.Virtual, Types.StringBuilder, "append",
                Type.getMethodType(Types.StringBuilder, Types.CharSequence));

        static final MethodDesc StringBuilder_append_String = MethodDesc.create(MethodType.Virtual,
                Types.StringBuilder, "append",
                Type.getMethodType(Types.StringBuilder, Types.String));

        static final MethodDesc StringBuilder_init = MethodDesc.create(MethodType.Special,
                Types.StringBuilder, "<init>", Type.getMethodType(Type.VOID_TYPE));

        static final MethodDesc StringBuilder_toString = MethodDesc.create(MethodType.Virtual,
                Types.StringBuilder, "toString", Type.getMethodType(Types.String));
    }

    private final IdentifierResolution identifierResolution;

    public ExpressionGenerator(CodeGenerator codegen) {
        super(codegen);
        this.identifierResolution = new IdentifierResolution();
    }

    private static final Object[] EMPTY_BSM_ARGS = new Object[] {};

    private void invokeDynamicCall(ExpressionVisitor mv) {
        mv.invokedynamic(Bootstrap.getCallName(), Bootstrap.getCallMethodDescriptor(),
                Bootstrap.getCallBootstrap(), EMPTY_BSM_ARGS);
    }

    private void invokeDynamicOperator(BinaryExpression.Operator operator, ExpressionVisitor mv) {
        mv.invokedynamic(Bootstrap.getName(operator), Bootstrap.getMethodDescriptor(operator),
                Bootstrap.getBootstrap(operator), EMPTY_BSM_ARGS);
    }

    /**
     * ref = `eval` {@code node}<br>
     * GetValue(ref)<br>
     */
    private void evalAndGetBoxedValue(Expression node, ExpressionVisitor mv) {
        ValType type = evalAndGetValue(node, mv);
        mv.toBoxed(type);
    }

    /**
     * ref = `eval` {@code node}<br>
     * GetValue(ref)<br>
     */
    private ValType evalAndGetValue(Expression node, ExpressionVisitor mv) {
        Expression valueNode = node.asValue();
        ValType type = valueNode.accept(this, mv);
        if (type == ValType.Reference) {
            GetValue(valueNode, type, mv);
            return ValType.Any;
        }
        return type;
    }

    private void GetValue(Expression node, ValType type, ExpressionVisitor mv) {
        assert type == ValType.Reference : "type is not reference: " + type;
        mv.loadExecutionContext();
        mv.invoke(Methods.Reference_getValue);
    }

    private void PutValue(Expression node, ValType type, ExpressionVisitor mv) {
        assert type == ValType.Reference : "type is not reference: " + type;
        mv.loadExecutionContext();
        mv.invoke(Methods.Reference_putValue);
    }

    /**
     * stack: [envRec] -> [envRec]
     */
    private void createMutableBinding(String name, boolean deletable, ExpressionVisitor mv) {
        mv.dup();
        mv.aconst(name);
        mv.iconst(deletable);
        mv.invoke(Methods.EnvironmentRecord_createMutableBinding);
    }

    private static boolean isPropertyReference(Expression base, ValType type) {
        return type == ValType.Reference && !(base instanceof Identifier);
    }

    private static boolean isEnclosedByWithStatement(ExpressionVisitor mv) {
        for (Scope scope = mv.getScope();;) {
            Scope nextScope;
            if (scope instanceof BlockScope) {
                BlockScope blockScope = (BlockScope) scope;
                if (blockScope.isDynamic()) {
                    return true;
                }
                nextScope = scope.getParent();
            } else if (scope instanceof TopLevelScope) {
                TopLevelScope topScope = (TopLevelScope) scope;
                assert topScope.getParent() == null;
                nextScope = topScope.getEnclosingScope();
            } else {
                assert false : "unknown scope class: " + scope.getClass().getName();
                return false;
            }
            if (nextScope == null) {
                ScopedNode node = scope.getNode();
                if (node instanceof Script) {
                    return ((Script) node).isEnclosedByWithStatement();
                }
                return false;
            }
            scope = nextScope;
        }
    }

    private static boolean isGlobalScope(ExpressionVisitor mv) {
        ScopedNode node = mv.getScope().getNode();
        if (node instanceof Script) {
            return ((Script) node).isGlobalScope();
        }
        return false;
    }

    /**
     * [12.2.3 EvaluateCall Abstract Operation]
     */
    private void EvaluateCall(Expression call, Expression base, ValType type,
            List<Expression> arguments, boolean directEval, ExpressionVisitor mv) {
        if (type == ValType.Reference) {
            if (isPropertyReference(base, type)) {
                EvaluateCallPropRef(call, base, type, arguments, mv);
            } else if (isEnclosedByWithStatement(mv)) {
                EvaluateCallWithIdentRef(call, base, type, arguments, directEval, mv);
            } else {
                EvaluateCallIdentRef(call, base, type, arguments, directEval, mv);
            }
        } else {
            EvaluateCallWithValue(call, base, type, arguments, mv);
        }
    }

    /**
     * [12.2.4.2 Runtime Semantics: EvaluateCall]
     */
    private void EvaluateCallPropRef(Expression call, Expression base, ValType type,
            List<Expression> arguments, ExpressionVisitor mv) {
        // only called for the property reference case (`obj.method(...)` or `obj[method](...)`)
        assert isPropertyReference(base, type);

        // stack: [ref] -> [ref, ref]
        mv.dup();

        /* steps 1-2 */
        // stack: [ref, ref] -> [ref, func]
        GetValue(base, type, mv);

        /* steps 3-4 */
        // stack: [ref, func] -> [args, ref, func]
        ArgumentListEvaluation(arguments, mv);
        mv.dupX2();
        mv.pop();

        // stack: [args, ref, func]
        mv.lineInfo(call);

        /* steps 5-6 */
        // stack: [args, ref, func] -> [args, ref, func(Callable)]
        mv.loadExecutionContext();
        mv.invoke(Methods.ScriptRuntime_CheckCallable);

        /* step 7-8 */
        // stack: [args, ref, func(Callable)] -> [args, thisValue, func(Callable)]
        mv.swap();
        mv.loadExecutionContext();
        mv.invoke(Methods.Reference_getThisValue);
        mv.swap();

        /* steps 9-13 */
        // stack: [args, thisValue, func(Callable)] -> result
        standardCall(call, mv);
    }

    /**
     * [12.2.4.2 Runtime Semantics: EvaluateCall]
     */
    private void EvaluateCallWithValue(Expression call, Expression base, ValType type,
            List<Expression> arguments, ExpressionVisitor mv) {
        assert type != ValType.Reference;

        /* steps 1-2 (not applicable) */
        // GetValue(...)

        /* steps 3-4 */
        // stack: [func] -> [args, func]
        ArgumentListEvaluation(arguments, mv);
        mv.swap();

        // stack: [args, func]
        mv.lineInfo(call);

        /* steps 5-6 */
        // stack: [args, func] -> [args, func(Callable)]
        mv.loadExecutionContext();
        mv.invoke(Methods.ScriptRuntime_CheckCallable);

        /* steps 7-8 */
        // stack: [args, func(Callable)] -> [args, thisValue, func(Callable)]
        mv.loadUndefined();
        mv.swap();

        /* steps 9-13 */
        // stack: [args, thisValue, func(Callable)] -> result
        standardCall(call, mv);
    }

    /**
     * [12.2.4.2 Runtime Semantics: EvaluateCall]
     */
    private void EvaluateCallIdentRef(Expression call, Expression base, ValType type,
            List<Expression> arguments, boolean directEval, ExpressionVisitor mv) {
        assert type == ValType.Reference && base instanceof Identifier;

        Label afterCall = new Label();

        /* steps 1-2 */
        // stack: [ref] -> [func]
        GetValue(base, type, mv);

        /* steps 3-4 */
        // stack: [func] -> [args, func]
        boolean hasSpread = ArgumentListEvaluation(arguments, mv);
        mv.swap();

        // stack: [args, func]
        mv.lineInfo(call);

        /* steps 5-6 */
        // stack: [args, func] -> [args, func(Callable)]
        mv.loadExecutionContext();
        mv.invoke(Methods.ScriptRuntime_CheckCallable);

        /* step 7-8 */
        // stack: [args, func(Callable)] -> [args, thisValue, func(Callable)]
        mv.loadUndefined();
        mv.swap();

        if (directEval) {
            directEvalCall(call, base, type, arguments, hasSpread, afterCall, mv);
        }

        /* steps 9-13 */
        // stack: [args, thisValue, func(Callable)] -> result
        standardCall(call, mv);

        mv.mark(afterCall);
    }

    /**
     * [12.2.4.2 Runtime Semantics: EvaluateCall]
     */
    private void EvaluateCallWithIdentRef(Expression call, Expression base, ValType type,
            List<Expression> arguments, boolean directEval, ExpressionVisitor mv) {
        assert type == ValType.Reference && base instanceof Identifier;

        Label afterCall = new Label(), baseObjNotNull = new Label();

        // stack: [ref] -> [ref, ref]
        mv.dup();

        /* steps 1-2 */
        // stack: [ref, ref] -> [ref, func]
        GetValue(base, type, mv);

        /* steps 3-4 */
        // stack: [ref, func] -> [args, ref, func]
        boolean hasSpread = ArgumentListEvaluation(arguments, mv);
        mv.dupX2();
        mv.pop();

        // stack: [args, ref, func]
        mv.lineInfo(call);

        /* steps 5-6 */
        // stack: [args, ref, func] -> [args, ref, func(Callable)]
        mv.loadExecutionContext();
        mv.invoke(Methods.ScriptRuntime_CheckCallable);

        /* step 7-8 */
        // stack: [args, ref, func(Callable)] -> [args, func(Callable), baseObj?]
        mv.swap();
        mv.invoke(Methods.Reference_getBase);
        mv.checkcast(Types.EnvironmentRecord);
        mv.invoke(Methods.EnvironmentRecord_withBaseObject);

        // stack: [args, func(Callable), baseObj?] -> [args, thisValue, func(Callable)]
        mv.dup();
        mv.ifnonnull(baseObjNotNull);
        {
            mv.pop();
            mv.loadUndefined();
        }
        mv.mark(baseObjNotNull);
        mv.swap();

        if (directEval) {
            directEvalCall(call, base, type, arguments, hasSpread, afterCall, mv);
        }

        /* steps 9-13 */
        // stack: [args, thisValue, func(Callable)] -> result
        standardCall(call, mv);

        mv.mark(afterCall);
    }

    /**
     * [18.2.1.1] Direct Call to Eval
     */
    private void directEvalCall(Expression call, Expression base, ValType type,
            List<Expression> arguments, boolean hasSpread, Label afterCall, ExpressionVisitor mv) {
        assert type == ValType.Reference && base instanceof Identifier;

        // test for possible direct-eval call
        Label notEval = new Label();

        // stack: [args, thisValue, func(Callable)] -> [args, thisValue, func(Callable)]
        mv.dup();
        mv.loadExecutionContext();
        mv.invoke(Methods.ScriptRuntime_IsBuiltinEval);
        mv.ifeq(notEval);

        // stack: [args, thisValue, func(Callable)] -> [args]
        mv.pop2();

        // stack: [args] -> [result]
        mv.loadExecutionContext();
        int evalFlags = EvalFlags.Direct.getValue();
        if (mv.isStrict()) {
            evalFlags |= EvalFlags.Strict.getValue();
        }
        if (mv.isGlobalCode()) {
            evalFlags |= EvalFlags.GlobalCode.getValue();
        }
        if (isGlobalScope(mv)) {
            evalFlags |= EvalFlags.GlobalScope.getValue();
        }
        if (isEnclosedByWithStatement(mv)) {
            evalFlags |= EvalFlags.EnclosedByWithStatement.getValue();
        }
        mv.iconst(evalFlags);
        mv.invoke(Methods.Eval_directEval);

        mv.goTo(afterCall);
        mv.mark(notEval);

        // direct-eval fallback hook
        Label noEvalHook = new Label();
        mv.loadExecutionContext();
        mv.invoke(Methods.ScriptRuntime_directEvalFallbackHook);
        mv.ifnull(noEvalHook);
        {
            // stack: [args, thisValue, func(Callable)] -> [args']
            mv.invoke(Methods.ScriptRuntime_directEvalFallbackArguments);
            mv.loadUndefined(); // FIXME: unspecified
            mv.loadExecutionContext();
            mv.invoke(Methods.ScriptRuntime_directEvalFallbackHook);
            // stack: [args', undefined, fallback(Callable)]
        }
        mv.mark(noEvalHook);
    }

    /**
     * [12.2.3 EvaluateCall Abstract Operation]
     */
    private void standardCall(Expression call, ExpressionVisitor mv) {
        // stack: [args, thisValue, func(Callable)]

        /* steps 10, 12-13 */
        if (mv.isTailCall(call)) {
            // stack: [args, thisValue, func(Callable)] -> [<func(Callable), thisValue, args>]
            mv.invoke(Methods.ScriptRuntime_PrepareForTailCall);
            return;
        }

        // stack: [args, thisValue, func(Callable)] -> [func(Callable), cx, thisValue, args]
        mv.loadExecutionContext();
        mv.dup2X2();
        mv.pop2();
        mv.swap();

        /* steps 11, 14 */
        // stack: [func(Callable), cx, thisValue, args] -> [result]
        invokeDynamicCall(mv);
    }

    /**
     * [12.2.5.1 ArgumentListEvaluation]
     */
    private boolean ArgumentListEvaluation(List<Expression> arguments, ExpressionVisitor mv) {
        boolean hasSpread = false;
        if (arguments.isEmpty()) {
            mv.get(Fields.ScriptRuntime_EMPTY_ARRAY);
        } else {
            mv.newarray(arguments.size(), Types.Object);
            for (int i = 0, size = arguments.size(); i < size; ++i) {
                mv.dup();
                mv.iconst(i);
                /* [12.2.5 Argument Lists] ArgumentListEvaluation */
                Expression argument = arguments.get(i);
                hasSpread |= (argument instanceof CallSpreadElement);
                evalAndGetBoxedValue(argument, mv);
                mv.astore(Types.Object);
            }
            if (hasSpread) {
                mv.loadExecutionContext();
                mv.invoke(Methods.ScriptRuntime_toFlatArray);
            }
        }
        return hasSpread;
    }

    /* ----------------------------------------------------------------------------------------- */

    @Override
    protected ValType visit(Node node, ExpressionVisitor mv) {
        throw new IllegalStateException(String.format("node-class: %s", node.getClass()));
    }

    /**
     * 12.1.4.2.5 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(ArrayComprehension node, ExpressionVisitor mv) {
        node.accept(new ArrayComprehensionGenerator(codegen), mv);

        return ValType.Object;
    }

    /**
     * 12.1.4.1.3 Runtime Semantics: Evaluation<br>
     * 12.1.4.1.2 Runtime Semantics: Array Accumulation
     */
    @Override
    public ValType visit(ArrayLiteral node, ExpressionVisitor mv) {
        int elision = 0;
        boolean hasSpread = false;
        for (Expression element : node.getElements()) {
            if (element instanceof Elision) {
                elision += 1;
            } else if (element instanceof SpreadElement) {
                hasSpread = true;
            }
        }

        if (!hasSpread) {
            // Try to initialise array with faster {Dense, Sparse}ArrayCreate methods
            int length = node.getElements().size();
            float density = (float) (length - elision) / length;
            if ((density >= 0.25f && length < 0x10) || (density >= 0.75f && length < 0x1000)) {
                mv.loadExecutionContext();
                mv.newarray(length, Types.Object);
                int nextIndex = 0;
                for (Expression element : node.getElements()) {
                    if (element instanceof Elision) {
                        // Elision
                    } else {
                        mv.dup();
                        mv.iconst(nextIndex);
                        evalAndGetBoxedValue(element, mv);
                        mv.astore(Types.Object);
                    }
                    nextIndex += 1;
                }
                if (elision == 0) {
                    mv.invoke(Methods.ExoticArray_DenseArrayCreate);
                } else {
                    mv.invoke(Methods.ExoticArray_SparseArrayCreate);
                }
                return ValType.Object;
            }
        }

        if (!hasSpread) {
            int length = node.getElements().size();
            mv.loadExecutionContext();
            mv.lconst(length); // initialise with correct "length"
            mv.invoke(Methods.ExoticArray_ArrayCreate);

            int nextIndex = 0;
            for (Expression element : node.getElements()) {
                if (element instanceof Elision) {
                    // Elision
                } else {
                    mv.dup();
                    mv.iconst(nextIndex);
                    evalAndGetBoxedValue(element, mv);
                    mv.loadExecutionContext();
                    mv.invoke(Methods.ScriptRuntime_defineProperty__int);
                }
                nextIndex += 1;
            }
            assert nextIndex == length;
            // omit call Put(array, "length", nextIndex false), array is initialised with length
        } else {
            mv.loadExecutionContext();
            mv.lconst(0);
            mv.invoke(Methods.ExoticArray_ArrayCreate);

            // stack: [array, nextIndex]
            mv.iconst(0); // nextIndex

            arrayLiteralWithSpread(node, mv);

            // stack: [array, nextIndex] -> [array, 'length', (nextIndex), false, cx]
            mv.toBoxed(Type.INT_TYPE);
            mv.swap();
            mv.dup();
            mv.loadExecutionContext();
            // stack: [(nextIndex), array, array, cx] -> [array, cx, array, (nextIndex)]
            mv.dup2X2();
            mv.pop2();
            mv.swap();
            // stack: [array, cx, array, (nextIndex)] -> [array, cx, array, pk, (nextIndex), false]
            mv.aconst("length");
            mv.swap();
            mv.iconst(false);
            // stack: [array, cx, array, pk, (nextIndex), false] -> [array]
            mv.invoke(Methods.AbstractOperations_Put);
        }

        return ValType.Object;
    }

    /**
     * 12.1.4.1.3 Runtime Semantics: Evaluation<br>
     * 12.1.4.1.2 Runtime Semantics: Array Accumulation
     */
    @Override
    public ValType visit(SpreadArrayLiteral node, ExpressionVisitor mv) {
        // stack: [] -> [array, nextIndex]
        mv.loadParameter(1, ExoticArray.class);
        mv.loadParameter(2, int.class);

        arrayLiteralWithSpread(node, mv);

        // stack: [array, nextIndex] -> [nextIndex]
        mv.swap();
        mv.pop();

        return ValType.Any;
    }

    /**
     * 12.1.4.1.3 Runtime Semantics: Evaluation<br>
     * 12.1.4.1.2 Runtime Semantics: Array Accumulation
     */
    private void arrayLiteralWithSpread(ArrayLiteral node, ExpressionVisitor mv) {
        // stack: [array, nextIndex]
        int elisionWidth = 0;
        for (Expression element : node.getElements()) {
            if (element instanceof Elision) {
                // Elision
                elisionWidth += 1;
                continue;
            }
            if (elisionWidth != 0) {
                mv.iconst(elisionWidth);
                mv.add(Type.INT_TYPE);
                elisionWidth = 0;
            }
            if (element instanceof SpreadElement) {
                element.accept(this, mv);
            } else {
                // stack: [array, nextIndex] -> [array, nextIndex, array, nextIndex]
                mv.dup2();
                evalAndGetBoxedValue(element, mv);
                mv.loadExecutionContext();
                // stack: [array, nextIndex, array, nextIndex, obj] -> [array, nextIndex]
                mv.invoke(Methods.ScriptRuntime_defineProperty__int);
                elisionWidth += 1;
            }
        }
        if (elisionWidth != 0) {
            mv.iconst(elisionWidth);
            mv.add(Type.INT_TYPE);
            elisionWidth = 0;
        }
    }

    /**
     * 12.1.4.1.2 Runtime Semantics: Array Accumulation
     */
    @Override
    public ValType visit(SpreadElement node, ExpressionVisitor mv) {
        // stack: [array, nextIndex] -> [array, array, nextIndex]
        mv.swap();
        mv.dupX1();
        mv.swap();

        // stack: [array, array, nextIndex] -> [array, array, nextIndex, obj]
        Expression spread = node.getExpression();
        evalAndGetBoxedValue(spread, mv);

        mv.loadExecutionContext();

        // stack: [array, array, nextIndex, obj, cx] -> [array, nextIndex']
        mv.invoke(Methods.ScriptRuntime_ArrayAccumulationSpreadElement);

        return ValType.Any;
    }

    /**
     * 12.1.4.1.3 Runtime Semantics: Evaluation<br>
     * 12.1.4.1.2 Runtime Semantics: Array Accumulation
     */
    @Override
    public ValType visit(SpreadElementMethod node, ExpressionVisitor mv) {
        codegen.compile(node, mv);

        // stack: [array, nextIndex] -> [array, array, nextIndex]
        mv.swap();
        mv.dupX1();
        mv.swap();

        // stack: [array, array, nextIndex] -> [array, cx, array, nextIndex]
        mv.loadExecutionContext();
        mv.dupX2();
        mv.pop();

        // stack: [array, cx, array, nextIndex] -> [array, nextIndex']
        mv.invoke(codegen.methodDesc(node));

        return ValType.Any;
    }

    /**
     * 14.2.10 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(ArrowFunction node, ExpressionVisitor mv) {
        codegen.compile(node);

        // Runtime Semantics: Evaluation -> ArrowFunction
        mv.invoke(codegen.methodDesc(node, FunctionName.RTI));
        mv.loadExecutionContext();
        mv.invoke(Methods.ScriptRuntime_EvaluateArrowFunction);

        return ValType.Object;
    }

    /**
     * 12.13.3 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(AssignmentExpression node, ExpressionVisitor mv) {
        LeftHandSideExpression left = node.getLeft();
        Expression right = node.getRight();
        if (node.getOperator() == AssignmentExpression.Operator.ASSIGN) {
            if (left instanceof AssignmentPattern) {
                ValType rtype = evalAndGetValue(right, mv);

                if (rtype != ValType.Object) {
                    mv.toBoxed(rtype);
                    mv.loadExecutionContext();
                    mv.invoke(Methods.ScriptRuntime_ensureObject);
                }

                mv.dup();
                DestructuringAssignment((AssignmentPattern) left, mv);

                return ValType.Object;
            } else {
                ValType ltype = left.accept(this, mv);
                ValType rtype = evalAndGetValue(right, mv);

                if (IsAnonymousFunctionDefinition(right) && IsIdentifierRef(left)) {
                    SetFunctionName(right, ((Identifier) left).getName(), mv);
                }

                // lref rval
                mv.dupX(ltype, rtype);
                mv.toBoxed(rtype);
                PutValue(left, ltype, mv);

                return rtype;
            }
        } else {
            switch (node.getOperator()) {
            case ASSIGN_MUL: {
                // 12.5 Multiplicative Operators
                ValType ltype = left.accept(this, mv);
                mv.dup();
                GetValue(left, ltype, mv);
                // lref lval
                if (right instanceof Literal) {
                    ToNumber(ltype, mv);
                }
                ValType rtype = evalAndGetValue(right, mv);
                if (!(right instanceof Literal)) {
                    mv.swap(ltype, rtype);
                    ToNumber(ltype, mv);
                    mv.swap(rtype, ValType.Number);
                }
                ToNumber(rtype, mv);
                // lref lval rval
                mv.mul(Type.DOUBLE_TYPE);
                // r lref r
                mv.dupX(ltype, ValType.Number);
                mv.toBoxed(ValType.Number);
                PutValue(left, ltype, mv);
                return ValType.Number;
            }
            case ASSIGN_DIV: {
                // 12.5 Multiplicative Operators
                ValType ltype = left.accept(this, mv);
                mv.dup();
                GetValue(left, ltype, mv);
                // lref lval
                if (right instanceof Literal) {
                    ToNumber(ltype, mv);
                }
                ValType rtype = evalAndGetValue(right, mv);
                if (!(right instanceof Literal)) {
                    mv.swap(ltype, rtype);
                    ToNumber(ltype, mv);
                    mv.swap(rtype, ValType.Number);
                }
                ToNumber(rtype, mv);
                // lref lval rval
                mv.div(Type.DOUBLE_TYPE);
                // r lref r
                mv.dupX(ltype, ValType.Number);
                mv.toBoxed(ValType.Number);
                PutValue(left, ltype, mv);
                return ValType.Number;
            }
            case ASSIGN_MOD: {
                // 12.5 Multiplicative Operators
                ValType ltype = left.accept(this, mv);
                mv.dup();
                GetValue(left, ltype, mv);
                // lref lval
                if (right instanceof Literal) {
                    ToNumber(ltype, mv);
                }
                ValType rtype = evalAndGetValue(right, mv);
                if (!(right instanceof Literal)) {
                    mv.swap(ltype, rtype);
                    ToNumber(ltype, mv);
                    mv.swap(rtype, ValType.Number);
                }
                ToNumber(rtype, mv);
                // lref lval rval
                mv.rem(Type.DOUBLE_TYPE);
                // r lref r
                mv.dupX(ltype, ValType.Number);
                mv.toBoxed(ValType.Number);
                PutValue(left, ltype, mv);
                return ValType.Number;
            }
            case ASSIGN_ADD: {
                // 12.6.1 The Addition operator ( + )
                if (right instanceof StringLiteral) {
                    // x += "..."
                    ValType ltype = left.accept(this, mv);
                    mv.dup();
                    GetValue(left, ltype, mv);
                    ToPrimitive(ltype, mv);
                    ToString(ltype, mv);
                    // lref lval(string)
                    if (!((StringLiteral) right).getValue().isEmpty()) {
                        right.accept(this, mv);
                        mv.loadExecutionContext();
                        mv.invoke(Methods.ScriptRuntime_add_str);
                    }
                    // r lref r
                    mv.dupX1();
                    PutValue(left, ltype, mv);
                    return ValType.String;
                }

                ValType ltype = left.accept(this, mv);
                mv.dup();
                GetValue(left, ltype, mv);
                // lref lval
                ValType rtype = evalAndGetValue(right, mv);
                mv.toBoxed(rtype);
                // lref lval rval
                mv.loadExecutionContext();
                invokeDynamicOperator(BinaryExpression.Operator.ADD, mv);
                // r lref r
                mv.dupX1();
                PutValue(left, ltype, mv);
                return ValType.Any;
            }
            case ASSIGN_SUB: {
                // 12.6.2 The Subtraction Operator ( - )
                ValType ltype = left.accept(this, mv);
                mv.dup();
                GetValue(left, ltype, mv);
                // lref lval
                if (right instanceof Literal) {
                    ToNumber(ltype, mv);
                }
                ValType rtype = evalAndGetValue(right, mv);
                if (!(right instanceof Literal)) {
                    mv.swap(ltype, rtype);
                    ToNumber(ltype, mv);
                    mv.swap(rtype, ValType.Number);
                }
                ToNumber(rtype, mv);
                // lref lval rval
                mv.sub(Type.DOUBLE_TYPE);
                // r lref r
                mv.dupX(ltype, ValType.Number);
                mv.toBoxed(ValType.Number);
                PutValue(left, ltype, mv);
                return ValType.Number;
            }
            case ASSIGN_SHL: {
                // 12.7.1 The Left Shift Operator ( << )
                ValType ltype = left.accept(this, mv);
                mv.dup();
                GetValue(left, ltype, mv);
                // lref lval
                if (right instanceof Literal) {
                    ToInt32(ltype, mv);
                }
                ValType rtype = evalAndGetValue(right, mv);
                if (!(right instanceof Literal)) {
                    mv.swap(ltype, rtype);
                    ToInt32(ltype, mv);
                    mv.swap(rtype, ValType.Number_int);
                }
                ToInt32(rtype, mv); // ToUint32()
                // lref lval rval
                mv.iconst(0x1F);
                mv.and(Type.INT_TYPE);
                mv.shl(Type.INT_TYPE);
                // r lref r
                mv.dupX(ltype, ValType.Number_int);
                mv.toBoxed(ValType.Number_int);
                PutValue(left, ltype, mv);
                return ValType.Number_int;
            }
            case ASSIGN_SHR: {
                // 12.7.2 The Signed Right Shift Operator ( >> )
                ValType ltype = left.accept(this, mv);
                mv.dup();
                GetValue(left, ltype, mv);
                // lref lval
                if (right instanceof Literal) {
                    ToInt32(ltype, mv);
                }
                ValType rtype = evalAndGetValue(right, mv);
                if (!(right instanceof Literal)) {
                    mv.swap(ltype, rtype);
                    ToInt32(ltype, mv);
                    mv.swap(rtype, ValType.Number_int);
                }
                ToInt32(rtype, mv); // ToUint32()
                // lref lval rval
                mv.iconst(0x1F);
                mv.and(Type.INT_TYPE);
                mv.shr(Type.INT_TYPE);
                // r lref r
                mv.dupX(ltype, ValType.Number_int);
                mv.toBoxed(ValType.Number_int);
                PutValue(left, ltype, mv);
                return ValType.Number_int;
            }
            case ASSIGN_USHR: {
                // 12.7.3 The Unsigned Right Shift Operator ( >>> )
                ValType ltype = left.accept(this, mv);
                mv.dup();
                GetValue(left, ltype, mv);
                // lref lval
                if (right instanceof Literal) {
                    ToUint32(ltype, mv);
                }
                ValType rtype = evalAndGetValue(right, mv);
                if (!(right instanceof Literal)) {
                    mv.swap(ltype, rtype);
                    ToUint32(ltype, mv);
                    mv.swap(rtype, ValType.Number_uint);
                }
                ToInt32(rtype, mv); // ToUint32()
                // lref lval rval
                mv.iconst(0x1F);
                mv.and(Type.INT_TYPE);
                mv.ushr(Type.LONG_TYPE);
                // r lref r
                mv.dupX(ltype, ValType.Number_uint);
                mv.toBoxed(ValType.Number_uint);
                PutValue(left, ltype, mv);
                return ValType.Number_uint;
            }
            case ASSIGN_BITAND: {
                // 12.10 Binary Bitwise Operators ( & )
                ValType ltype = left.accept(this, mv);
                mv.dup();
                GetValue(left, ltype, mv);
                // lref lval
                if (right instanceof Literal) {
                    ToInt32(ltype, mv);
                }
                ValType rtype = evalAndGetValue(right, mv);
                if (!(right instanceof Literal)) {
                    mv.swap(ltype, rtype);
                    ToInt32(ltype, mv);
                    mv.swap(rtype, ValType.Number_int);
                }
                ToInt32(rtype, mv); // ToUint32()
                // lref lval rval
                mv.and(Type.INT_TYPE);
                // r lref r
                mv.dupX(ltype, ValType.Number_int);
                mv.toBoxed(ValType.Number_int);
                PutValue(left, ltype, mv);
                return ValType.Number_int;
            }
            case ASSIGN_BITXOR: {
                // 12.10 Binary Bitwise Operators ( ^ )
                ValType ltype = left.accept(this, mv);
                mv.dup();
                GetValue(left, ltype, mv);
                // lref lval
                if (right instanceof Literal) {
                    ToInt32(ltype, mv);
                }
                ValType rtype = evalAndGetValue(right, mv);
                if (!(right instanceof Literal)) {
                    mv.swap(ltype, rtype);
                    ToInt32(ltype, mv);
                    mv.swap(rtype, ValType.Number_int);
                }
                ToInt32(rtype, mv); // ToUint32()
                // lref lval rval
                mv.xor(Type.INT_TYPE);
                // r lref r
                mv.dupX(ltype, ValType.Number_int);
                mv.toBoxed(ValType.Number_int);
                PutValue(left, ltype, mv);
                return ValType.Number_int;
            }
            case ASSIGN_BITOR: {
                // 12.10 Binary Bitwise Operators ( | )
                ValType ltype = left.accept(this, mv);
                mv.dup();
                GetValue(left, ltype, mv);
                // lref lval
                if (right instanceof Literal) {
                    ToInt32(ltype, mv);
                }
                ValType rtype = evalAndGetValue(right, mv);
                if (!(right instanceof Literal)) {
                    mv.swap(ltype, rtype);
                    ToInt32(ltype, mv);
                    mv.swap(rtype, ValType.Number_int);
                }
                ToInt32(rtype, mv); // ToUint32()
                // lref lval rval
                mv.or(Type.INT_TYPE);
                // r lref r
                mv.dupX(ltype, ValType.Number_int);
                mv.toBoxed(ValType.Number_int);
                PutValue(left, ltype, mv);
                return ValType.Number_int;
            }
            case ASSIGN:
            default:
                throw new IllegalStateException(Objects.toString(node.getOperator(), "<null>"));
            }
        }
    }

    /**
     * 12.5.2 Runtime Semantics: Evaluation<br>
     * 12.6.2.1 Runtime Semantics: Evaluation<br>
     * 12.6.3.1 Runtime Semantics: Evaluation<br>
     * 12.7.2.1 Runtime Semantics: Evaluation<br>
     * 12.7.3.1 Runtime Semantics: Evaluation<br>
     * 12.7.4.1 Runtime Semantics: Evaluation<br>
     * 12.8.2 Runtime Semantics: Evaluation<br>
     * 12.9.2 Runtime Semantics: Evaluation<br>
     * 12.10.2 Runtime Semantics: Evaluation<br>
     * 12.11.2 Runtime Semantics: Evaluation<br>
     */
    @Override
    public ValType visit(BinaryExpression node, ExpressionVisitor mv) {
        Expression left = node.getLeft();
        Expression right = node.getRight();

        switch (node.getOperator()) {
        case MUL: {
            // 12.5 Multiplicative Operators
            ValType ltype = evalAndGetValue(left, mv);
            if (ltype.isPrimitive() || right instanceof Literal) {
                ToNumber(ltype, mv);
            }
            ValType rtype = evalAndGetValue(right, mv);
            if (!(ltype.isPrimitive() || right instanceof Literal)) {
                mv.swap(ltype, rtype);
                ToNumber(ltype, mv);
                mv.swap(rtype, ValType.Number);
            }
            ToNumber(rtype, mv);
            mv.mul(Type.DOUBLE_TYPE);
            return ValType.Number;
        }
        case DIV: {
            // 12.5 Multiplicative Operators
            ValType ltype = evalAndGetValue(left, mv);
            if (ltype.isPrimitive() || right instanceof Literal) {
                ToNumber(ltype, mv);
            }
            ValType rtype = evalAndGetValue(right, mv);
            if (!(ltype.isPrimitive() || right instanceof Literal)) {
                mv.swap(ltype, rtype);
                ToNumber(ltype, mv);
                mv.swap(rtype, ValType.Number);
            }
            ToNumber(rtype, mv);
            mv.div(Type.DOUBLE_TYPE);
            return ValType.Number;
        }
        case MOD: {
            // 12.5 Multiplicative Operators
            ValType ltype = evalAndGetValue(left, mv);
            if (ltype.isPrimitive() || right instanceof Literal) {
                ToNumber(ltype, mv);
            }
            ValType rtype = evalAndGetValue(right, mv);
            if (!(ltype.isPrimitive() || right instanceof Literal)) {
                mv.swap(ltype, rtype);
                ToNumber(ltype, mv);
                mv.swap(rtype, ValType.Number);
            }
            ToNumber(rtype, mv);
            mv.rem(Type.DOUBLE_TYPE);
            return ValType.Number;
        }
        case ADD: {
            // 12.6.1 The Addition operator ( + )
            if (left instanceof StringLiteral) {
                if (((StringLiteral) left).getValue().isEmpty()) {
                    // "" + x
                    ValType rtype = evalAndGetValue(right, mv);
                    rtype = ToPrimitive(rtype, mv);
                    ToString(rtype, mv);
                } else {
                    // "..." + x
                    left.accept(this, mv);
                    ValType rtype = evalAndGetValue(right, mv);
                    rtype = ToPrimitive(rtype, mv);
                    ToString(rtype, mv);
                    mv.loadExecutionContext();
                    mv.invoke(Methods.ScriptRuntime_add_str);
                }
                return ValType.String;
            } else if (right instanceof StringLiteral) {
                if (((StringLiteral) right).getValue().isEmpty()) {
                    // x + ""
                    ValType ltype = evalAndGetValue(left, mv);
                    ltype = ToPrimitive(ltype, mv);
                    ToString(ltype, mv);
                } else {
                    // x + "..."
                    ValType ltype = evalAndGetValue(left, mv);
                    ltype = ToPrimitive(ltype, mv);
                    ToString(ltype, mv);
                    right.accept(this, mv);
                    mv.loadExecutionContext();
                    mv.invoke(Methods.ScriptRuntime_add_str);
                }
                return ValType.String;
            }

            ValType ltype = evalAndGetValue(left, mv);
            if (ltype.isNumeric() && right instanceof Literal) {
                ToNumber(ltype, mv);
                ValType rtype = evalAndGetValue(right, mv);
                ToNumber(rtype, mv);
                mv.add(Type.DOUBLE_TYPE);
                return ValType.Number;
            }
            mv.toBoxed(ltype);
            ValType rtype = evalAndGetValue(right, mv);
            mv.toBoxed(rtype);

            mv.loadExecutionContext();
            invokeDynamicOperator(node.getOperator(), mv);

            return ValType.Any;
        }
        case SUB: {
            // 12.6.2 The Subtraction Operator ( - )
            ValType ltype = evalAndGetValue(left, mv);
            if (ltype.isPrimitive() || right instanceof Literal) {
                ToNumber(ltype, mv);
            }
            ValType rtype = evalAndGetValue(right, mv);
            if (!(ltype.isPrimitive() || right instanceof Literal)) {
                mv.swap(ltype, rtype);
                ToNumber(ltype, mv);
                mv.swap(rtype, ValType.Number);
            }
            ToNumber(rtype, mv);
            mv.sub(Type.DOUBLE_TYPE);
            return ValType.Number;
        }
        case SHL: {
            // 12.7.1 The Left Shift Operator ( << )
            ValType ltype = evalAndGetValue(left, mv);
            if (ltype.isPrimitive() || right instanceof Literal) {
                ToInt32(ltype, mv);
            }
            ValType rtype = evalAndGetValue(right, mv);
            if (!(ltype.isPrimitive() || right instanceof Literal)) {
                mv.swap(ltype, rtype);
                ToInt32(ltype, mv);
                mv.swap(rtype, ValType.Number_int);
            }
            ToInt32(rtype, mv); // ToUint32()
            mv.iconst(0x1F);
            mv.and(Type.INT_TYPE);
            mv.shl(Type.INT_TYPE);
            return ValType.Number_int;
        }
        case SHR: {
            // 12.7.2 The Signed Right Shift Operator ( >> )
            ValType ltype = evalAndGetValue(left, mv);
            if (ltype.isPrimitive() || right instanceof Literal) {
                ToInt32(ltype, mv);
            }
            ValType rtype = evalAndGetValue(right, mv);
            if (!(ltype.isPrimitive() || right instanceof Literal)) {
                mv.swap(ltype, rtype);
                ToInt32(ltype, mv);
                mv.swap(rtype, ValType.Number_int);
            }
            ToInt32(rtype, mv); // ToUint32()
            mv.iconst(0x1F);
            mv.and(Type.INT_TYPE);
            mv.shr(Type.INT_TYPE);
            return ValType.Number_int;
        }
        case USHR: {
            // 12.7.3 The Unsigned Right Shift Operator ( >>> )
            ValType ltype = evalAndGetValue(left, mv);
            if (ltype.isPrimitive() || right instanceof Literal) {
                ToUint32(ltype, mv);
            }
            ValType rtype = evalAndGetValue(right, mv);
            if (!(ltype.isPrimitive() || right instanceof Literal)) {
                mv.swap(ltype, rtype);
                ToUint32(ltype, mv);
                mv.swap(rtype, ValType.Number_uint);
            }
            ToInt32(rtype, mv); // ToUint32()
            mv.iconst(0x1F);
            mv.and(Type.INT_TYPE);
            mv.ushr(Type.LONG_TYPE);
            return ValType.Number_uint;
        }
        case LT: {
            // 12.8 Relational Operators ( < )
            evalAndGetBoxedValue(left, mv);
            evalAndGetBoxedValue(right, mv);

            mv.loadExecutionContext();
            invokeDynamicOperator(node.getOperator(), mv);

            {
                Label lbl1 = new Label(), lbl2 = new Label();
                mv.ifgt(lbl1);
                mv.iconst(false);
                mv.goTo(lbl2);
                mv.mark(lbl1);
                mv.iconst(true);
                mv.mark(lbl2);
            }
            return ValType.Boolean;
        }
        case GT: {
            // 12.8 Relational Operators ( > )
            evalAndGetBoxedValue(left, mv);
            evalAndGetBoxedValue(right, mv);
            mv.swap();

            mv.loadExecutionContext();
            invokeDynamicOperator(node.getOperator(), mv);

            {
                Label lbl1 = new Label(), lbl2 = new Label();
                mv.ifgt(lbl1);
                mv.iconst(false);
                mv.goTo(lbl2);
                mv.mark(lbl1);
                mv.iconst(true);
                mv.mark(lbl2);
            }
            return ValType.Boolean;
        }
        case LE: {
            // 12.8 Relational Operators ( <= )
            evalAndGetBoxedValue(left, mv);
            evalAndGetBoxedValue(right, mv);
            mv.swap();

            mv.loadExecutionContext();
            invokeDynamicOperator(node.getOperator(), mv);

            {
                Label lbl1 = new Label(), lbl2 = new Label();
                mv.ifeq(lbl1);
                mv.iconst(false);
                mv.goTo(lbl2);
                mv.mark(lbl1);
                mv.iconst(true);
                mv.mark(lbl2);
            }
            return ValType.Boolean;
        }
        case GE: {
            // 12.8 Relational Operators ( >= )
            evalAndGetBoxedValue(left, mv);
            evalAndGetBoxedValue(right, mv);

            mv.loadExecutionContext();
            invokeDynamicOperator(node.getOperator(), mv);

            {
                Label lbl1 = new Label(), lbl2 = new Label();
                mv.ifeq(lbl1);
                mv.iconst(false);
                mv.goTo(lbl2);
                mv.mark(lbl1);
                mv.iconst(true);
                mv.mark(lbl2);
            }
            return ValType.Boolean;
        }
        case INSTANCEOF: {
            // 12.8 Relational Operators ( instanceof )
            evalAndGetBoxedValue(left, mv);
            evalAndGetBoxedValue(right, mv);

            mv.loadExecutionContext();
            mv.invoke(Methods.ScriptRuntime_InstanceofOperator);
            return ValType.Boolean;
        }
        case IN: {
            // 12.8 Relational Operators ( in )
            evalAndGetBoxedValue(left, mv);
            evalAndGetBoxedValue(right, mv);

            mv.loadExecutionContext();
            mv.invoke(Methods.ScriptRuntime_in);
            return ValType.Boolean;
        }
        case EQ: {
            // 12.9 Equality Operators ( == )
            evalAndGetBoxedValue(left, mv);
            evalAndGetBoxedValue(right, mv);

            mv.loadExecutionContext();
            invokeDynamicOperator(node.getOperator(), mv);

            return ValType.Boolean;
        }
        case NE: {
            // 12.9 Equality Operators ( != )
            evalAndGetBoxedValue(left, mv);
            evalAndGetBoxedValue(right, mv);

            mv.loadExecutionContext();
            invokeDynamicOperator(node.getOperator(), mv);

            mv.not();
            return ValType.Boolean;
        }
        case SHEQ: {
            // 12.9 Equality Operators ( === )
            evalAndGetBoxedValue(left, mv);
            evalAndGetBoxedValue(right, mv);

            invokeDynamicOperator(node.getOperator(), mv);

            return ValType.Boolean;
        }
        case SHNE: {
            // 12.9 Equality Operators ( !== )
            evalAndGetBoxedValue(left, mv);
            evalAndGetBoxedValue(right, mv);

            invokeDynamicOperator(node.getOperator(), mv);

            mv.not();
            return ValType.Boolean;
        }
        case BITAND: {
            // 12.10 Binary Bitwise Operators ( & )
            ValType ltype = evalAndGetValue(left, mv);
            if (ltype.isPrimitive() || right instanceof Literal) {
                ToInt32(ltype, mv);
            }
            ValType rtype = evalAndGetValue(right, mv);
            if (!(ltype.isPrimitive() || right instanceof Literal)) {
                mv.swap(ltype, rtype);
                ToInt32(ltype, mv);
                mv.swap(rtype, ValType.Number_int);
            }
            ToInt32(rtype, mv);
            mv.and(Type.INT_TYPE);
            return ValType.Number_int;
        }
        case BITXOR: {
            // 12.10 Binary Bitwise Operators ( ^ )
            ValType ltype = evalAndGetValue(left, mv);
            if (ltype.isPrimitive() || right instanceof Literal) {
                ToInt32(ltype, mv);
            }
            ValType rtype = evalAndGetValue(right, mv);
            if (!(ltype.isPrimitive() || right instanceof Literal)) {
                mv.swap(ltype, rtype);
                ToInt32(ltype, mv);
                mv.swap(rtype, ValType.Number_int);
            }
            ToInt32(rtype, mv);
            mv.xor(Type.INT_TYPE);
            return ValType.Number_int;
        }
        case BITOR: {
            // 12.10 Binary Bitwise Operators ( | )
            ValType ltype = evalAndGetValue(left, mv);
            if (ltype.isPrimitive() || right instanceof Literal) {
                ToInt32(ltype, mv);
            }
            ValType rtype = evalAndGetValue(right, mv);
            if (!(ltype.isPrimitive() || right instanceof Literal)) {
                mv.swap(ltype, rtype);
                ToInt32(ltype, mv);
                mv.swap(rtype, ValType.Number_int);
            }
            ToInt32(rtype, mv);
            mv.or(Type.INT_TYPE);
            return ValType.Number_int;
        }

        case AND:
        case OR: {
            // 12.11 Binary Logical Operators
            Label after = new Label();

            evalAndGetBoxedValue(left, mv);
            mv.dup();
            ToBoolean(ValType.Any, mv);
            if (node.getOperator() == BinaryExpression.Operator.AND) {
                mv.ifeq(after);
            } else {
                mv.ifne(after);
            }
            mv.pop();
            evalAndGetBoxedValue(right, mv);
            mv.mark(after);

            return ValType.Any;
        }
        default:
            throw new IllegalStateException(Objects.toString(node.getOperator(), "<null>"));
        }
    }

    /**
     * 12.1.3.1 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(BooleanLiteral node, ExpressionVisitor mv) {
        mv.iconst(node.getValue());

        return ValType.Boolean;
    }

    /**
     * 12.2.4.1 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(CallExpression node, ExpressionVisitor mv) {
        ValType type = node.getBase().accept(this, mv);
        mv.toBoxed(type);

        // direct call to eval?
        boolean directEval = (node.getBase() instanceof Identifier && "eval"
                .equals(((Identifier) node.getBase()).getName()));
        EvaluateCall(node, node.getBase(), type, node.getArguments(), directEval, mv);

        return ValType.Any;
    }

    /**
     * 12.2.6.1 Runtime Semantics: ArgumentListEvaluation
     */
    @Override
    public ValType visit(CallSpreadElement node, ExpressionVisitor mv) {
        evalAndGetBoxedValue(node.getExpression(), mv);
        mv.loadExecutionContext();
        mv.invoke(Methods.ScriptRuntime_SpreadArray);

        return ValType.Any; // actually Object[]
    }

    /**
     * 14.5.15 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(ClassExpression node, ExpressionVisitor mv) {
        String className = (node.getName() != null ? node.getName().getName() : null);
        ClassDefinitionEvaluation(node, className, mv);

        if (node.getName() != null) {
            SetFunctionName(node, node.getName().getName(), mv);
        }

        return ValType.Object;
    }

    /**
     * 12.14.2 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(CommaExpression node, ExpressionVisitor mv) {
        ValType type = null;
        List<Expression> list = node.getOperands();
        for (Expression e : list) {
            if (type != null) {
                mv.pop(type);
            }
            type = evalAndGetValue(e, mv);
        }
        assert type != null;
        return type;
    }

    /**
     * 12.12.2 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(ConditionalExpression node, ExpressionVisitor mv) {
        Label l0 = new Label(), l1 = new Label();

        ValType typeTest = evalAndGetValue(node.getTest(), mv);
        ToBoolean(typeTest, mv);
        mv.ifeq(l0);
        evalAndGetBoxedValue(node.getThen(), mv);
        mv.goTo(l1);
        mv.mark(l0);
        evalAndGetBoxedValue(node.getOtherwise(), mv);
        mv.mark(l1);

        return ValType.Any;
    }

    /**
     * 12.2.2.1 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(ElementAccessor node, ExpressionVisitor mv) {
        evalAndGetBoxedValue(node.getBase(), mv);
        ValType elementType = evalAndGetValue(node.getElement(), mv);
        if (elementType.isPrimitive()) {
            ToFlatString(elementType, mv);
            mv.loadExecutionContext();
            mv.iconst(mv.isStrict());
            mv.invoke(Methods.ScriptRuntime_getProperty);
        } else {
            mv.loadExecutionContext();
            mv.iconst(mv.isStrict());
            mv.invoke(Methods.ScriptRuntime_getElement);
        }

        return ValType.Reference;
    }

    /**
     * 12.2.2.1 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(ElementAccessorValue node, ExpressionVisitor mv) {
        evalAndGetBoxedValue(node.getBase(), mv);
        ValType elementType = evalAndGetValue(node.getElement(), mv);
        if (elementType.isPrimitive()) {
            ToFlatString(elementType, mv);
            mv.loadExecutionContext();
            mv.iconst(mv.isStrict());
            mv.invoke(Methods.ScriptRuntime_getPropertyValue);
        } else {
            mv.loadExecutionContext();
            mv.iconst(mv.isStrict());
            mv.invoke(Methods.ScriptRuntime_getElementValue);
        }

        return ValType.Any;
    }

    @Override
    public ValType visit(ExpressionMethod node, ExpressionVisitor mv) {
        codegen.compile(node, mv);

        mv.loadExecutionContext();
        mv.invoke(codegen.methodDesc(node));

        return ValType.Any;
    }

    /**
     * 14.1.15 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(FunctionExpression node, ExpressionVisitor mv) {
        codegen.compile(node);

        // Runtime Semantics: Evaluation -> FunctionExpression
        mv.invoke(codegen.methodDesc(node, FunctionName.RTI));
        mv.loadExecutionContext();
        mv.invoke(Methods.ScriptRuntime_EvaluateFunctionExpression);

        return ValType.Object;
    }

    /**
     * 12.1.7.2 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(GeneratorComprehension node, ExpressionVisitor mv) {
        codegen.compile(node);

        mv.invoke(codegen.methodDesc(node, FunctionName.RTI));
        mv.loadExecutionContext();
        if (!(node.getComprehension() instanceof LegacyComprehension)) {
            mv.invoke(Methods.ScriptRuntime_EvaluateGeneratorComprehension);
        } else {
            mv.invoke(Methods.ScriptRuntime_EvaluateLegacyGeneratorComprehension);
        }

        return ValType.Object;
    }

    /**
     * 14.4.12 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(GeneratorExpression node, ExpressionVisitor mv) {
        codegen.compile(node);

        // Runtime Semantics: Evaluation -> FunctionExpression
        mv.invoke(codegen.methodDesc(node, FunctionName.RTI));
        mv.loadExecutionContext();
        mv.invoke(Methods.ScriptRuntime_EvaluateGeneratorExpression);

        return ValType.Object;
    }

    /**
     * 14.4.12 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(LegacyGeneratorExpression node, ExpressionVisitor mv) {
        codegen.compile(node);

        // Runtime Semantics: Evaluation -> FunctionExpression
        mv.invoke(codegen.methodDesc(node, FunctionName.RTI));
        mv.loadExecutionContext();
        mv.invoke(Methods.ScriptRuntime_EvaluateLegacyGeneratorExpression);

        return ValType.Object;
    }

    /**
     * 12.1.2.2 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(Identifier node, ExpressionVisitor mv) {
        return identifierResolution.resolve(node, mv);
    }

    /**
     * 12.1.2.2 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(IdentifierValue node, ExpressionVisitor mv) {
        return identifierResolution.resolveValue(node, mv);
    }

    /**
     * Extension: 'let' expression
     */
    @Override
    public ValType visit(LetExpression node, ExpressionVisitor mv) {
        // create new declarative lexical environment
        // stack: [] -> [env]
        mv.enterScope(node);
        newDeclarativeEnvironment(mv);
        {
            // stack: [env] -> [env, envRec]
            mv.dup();
            mv.invoke(Methods.LexicalEnvironment_getEnvRec);

            // stack: [env, envRec] -> [env]
            for (LexicalBinding binding : node.getBindings()) {
                // stack: [env, envRec] -> [env, envRec, envRec]
                mv.dup();

                // stack: [env, envRec, envRec] -> [env, envRec, envRec]
                for (String name : BoundNames(binding.getBinding())) {
                    createMutableBinding(name, false, mv);
                }

                Expression initialiser = binding.getInitialiser();
                if (initialiser != null) {
                    ValType type = expressionBoxedValue(initialiser, mv);
                    if (binding.getBinding() instanceof BindingPattern) {
                        ToObject(type, mv);
                    }
                } else {
                    assert binding.getBinding() instanceof BindingIdentifier;
                    mv.loadUndefined();
                }

                // stack: [env, envRec, envRec, value] -> [env, envRec]
                BindingInitialisationWithEnvironment(binding.getBinding(), mv);
            }
            mv.pop();
        }
        // stack: [env] -> []
        pushLexicalEnvironment(mv);

        ValType type = evalAndGetValue(node.getExpression(), mv);

        // restore previous lexical environment
        popLexicalEnvironment(mv);
        mv.exitScope();

        return type;
    }

    /**
     * 12.2.3.1 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(NewExpression node, ExpressionVisitor mv) {
        evalAndGetBoxedValue(node.getExpression(), mv);
        ArgumentListEvaluation(node.getArguments(), mv);
        mv.lineInfo(node);
        mv.loadExecutionContext();
        if (mv.isTailCall(node)) {
            mv.invoke(Methods.ScriptRuntime_EvaluateConstructorTailCall);
        } else {
            mv.invoke(Methods.ScriptRuntime_EvaluateConstructorCall);
        }
        return ValType.Any;
    }

    /**
     * 12.1.3.1 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(NullLiteral node, ExpressionVisitor mv) {
        mv.loadNull();

        return ValType.Null;
    }

    /**
     * 12.1.3.1 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(NumericLiteral node, ExpressionVisitor mv) {
        double v = node.getValue();
        if ((int) v == v && (v != 0 || Double.doubleToRawLongBits(v) == 0L)) {
            mv.iconst((int) v);
            return ValType.Number_int;
        } else {
            mv.dconst(v);
            return ValType.Number;
        }
    }

    /**
     * 12.1.5.6 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(ObjectLiteral node, ExpressionVisitor mv) {
        mv.loadExecutionContext();
        mv.get(Fields.Intrinsics_ObjectPrototype);
        mv.invoke(Methods.OrdinaryObject_ObjectCreate);
        for (PropertyDefinition property : node.getProperties()) {
            mv.dup();
            codegen.propertyDefinition(property, mv);
        }

        return ValType.Object;
    }

    /**
     * 12.2.2.1 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(PropertyAccessor node, ExpressionVisitor mv) {
        evalAndGetBoxedValue(node.getBase(), mv);
        mv.aconst(node.getName());
        mv.loadExecutionContext();
        mv.iconst(mv.isStrict());
        mv.invoke(Methods.ScriptRuntime_getProperty);

        return ValType.Reference;
    }

    /**
     * 12.2.2.1 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(PropertyAccessorValue node, ExpressionVisitor mv) {
        evalAndGetBoxedValue(node.getBase(), mv);
        mv.aconst(node.getName());
        mv.loadExecutionContext();
        mv.iconst(mv.isStrict());
        mv.invoke(Methods.ScriptRuntime_getPropertyValue);

        return ValType.Any;
    }

    /**
     * 12.1.8.2 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(RegularExpressionLiteral node, ExpressionVisitor mv) {
        mv.loadExecutionContext();
        mv.aconst(node.getRegexp());
        mv.aconst(node.getFlags());
        mv.invoke(Methods.RegExpConstructor_RegExpCreate);

        return ValType.Object;
    }

    /**
     * 12.1.3.1 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(StringLiteral node, ExpressionVisitor mv) {
        mv.aconst(node.getValue());

        return ValType.String;
    }

    /**
     * 12.2.5.2 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(SuperExpression node, ExpressionVisitor mv) {
        switch (node.getType()) {
        case PropertyAccessor: {
            mv.loadExecutionContext();
            mv.aconst(node.getName());
            mv.iconst(mv.isStrict());
            mv.invoke(Methods.ScriptRuntime_MakeStringSuperReference);

            return ValType.Reference;
        }
        case ElementAccessor: {
            mv.loadExecutionContext();
            ValType type = evalAndGetValue(node.getExpression(), mv);
            ToPropertyKey(type, mv);
            mv.iconst(mv.isStrict());
            mv.invoke(Methods.ScriptRuntime_MakeSuperReference);

            return ValType.Reference;
        }
        case CallExpression: {
            mv.loadExecutionContext();
            mv.aconst(null);
            mv.iconst(mv.isStrict());
            mv.invoke(Methods.ScriptRuntime_MakeSuperReference);

            EvaluateCall(node, node, ValType.Reference, node.getArguments(), false, mv);

            return ValType.Any;
        }
        case NewExpression: {
            mv.loadExecutionContext();
            mv.aconst(null);
            mv.iconst(mv.isStrict());
            mv.invoke(Methods.ScriptRuntime_MakeSuperReference);

            return ValType.Reference;
        }
        default:
            throw new IllegalStateException();
        }
    }

    /**
     * 12.2.5.2 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(SuperExpressionValue node, ExpressionVisitor mv) {
        switch (node.getType()) {
        case PropertyAccessor: {
            mv.loadExecutionContext();
            mv.aconst(node.getName());
            mv.iconst(mv.isStrict());
            mv.invoke(Methods.ScriptRuntime_MakeStringSuperReference);

            GetValue(node, ValType.Reference, mv);

            return ValType.Any;
        }
        case ElementAccessor: {
            mv.loadExecutionContext();
            ValType type = evalAndGetValue(node.getExpression(), mv);
            ToPropertyKey(type, mv);
            mv.iconst(mv.isStrict());
            mv.invoke(Methods.ScriptRuntime_MakeSuperReference);

            GetValue(node, ValType.Reference, mv);

            return ValType.Any;
        }
        case CallExpression: {
            mv.loadExecutionContext();
            mv.aconst(null);
            mv.iconst(mv.isStrict());
            mv.invoke(Methods.ScriptRuntime_MakeSuperReference);

            EvaluateCall(node, node, ValType.Reference, node.getArguments(), false, mv);

            return ValType.Any;
        }
        case NewExpression: {
            mv.loadExecutionContext();
            mv.aconst(null);
            mv.iconst(mv.isStrict());
            mv.invoke(Methods.ScriptRuntime_MakeSuperReference);

            GetValue(node, ValType.Reference, mv);

            return ValType.Any;
        }
        default:
            throw new IllegalStateException();
        }
    }

    /**
     * 12.2.7.1 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(TemplateCallExpression node, ExpressionVisitor mv) {
        codegen.compile(node.getTemplate());

        TemplateLiteral template = node.getTemplate();
        List<Expression> substitutions = Substitutions(template);
        List<Expression> arguments = new ArrayList<>(substitutions.size() + 1);
        arguments.add(template);
        arguments.addAll(substitutions);

        ValType type = node.getBase().accept(this, mv);
        mv.toBoxed(type);
        EvaluateCall(node, node.getBase(), type, arguments, false, mv);

        return ValType.Any;
    }

    /**
     * 12.1.9.2.4 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(TemplateLiteral node, ExpressionVisitor mv) {
        if (node.isTagged()) {
            codegen.GetTemplateCallSite(node, mv);
            return ValType.Object;
        }

        List<Expression> elements = node.getElements();
        if (elements.size() == 1) {
            assert elements.get(0) instanceof TemplateCharacters;
            TemplateCharacters chars = (TemplateCharacters) elements.get(0);
            mv.aconst(chars.getValue());
        } else {
            mv.anew(Types.StringBuilder);
            mv.dup();
            mv.invoke(Methods.StringBuilder_init);

            boolean chars = true;
            for (Expression expr : elements) {
                assert chars == (expr instanceof TemplateCharacters);
                if (chars) {
                    String value = ((TemplateCharacters) expr).getValue();
                    if (!value.isEmpty()) {
                        mv.aconst(value);
                        mv.invoke(Methods.StringBuilder_append_String);
                    }
                } else {
                    ValType type = evalAndGetValue(expr, mv);
                    ToString(type, mv);
                    mv.invoke(Methods.StringBuilder_append_Charsequence);
                }
                chars = !chars;
            }

            mv.invoke(Methods.StringBuilder_toString);
        }

        return ValType.String;
    }

    /**
     * 12.1.1.1 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(ThisExpression node, ExpressionVisitor mv) {
        mv.loadExecutionContext();
        mv.invoke(Methods.ExecutionContext_resolveThisBinding);

        return ValType.Any;
    }

    /**
     * 12.3.3.1 Runtime Semantics: Evaluation<br>
     * 12.3.3.1 Runtime Semantics: Evaluation<br>
     * 12.4.3.2 Runtime Semantics: Evaluation<br>
     * 12.4.4.1 Runtime Semantics: Evaluation<br>
     * 12.4.5.1 Runtime Semantics: Evaluation<br>
     * 12.4.6.1 Runtime Semantics: Evaluation<br>
     * 12.4.7.1 Runtime Semantics: Evaluation<br>
     * 12.4.8.1 Runtime Semantics: Evaluation<br>
     * 12.4.9.1 Runtime Semantics: Evaluation<br>
     * 12.4.10.1 Runtime Semantics: Evaluation<br>
     * 12.4.11.1 Runtime Semantics: Evaluation<br>
     */
    @Override
    public ValType visit(UnaryExpression node, ExpressionVisitor mv) {
        switch (node.getOperator()) {
        case POST_INC: {
            // 12.3.3 Postfix Increment Operator
            Expression expr = node.getOperand();
            ValType type = expr.accept(this, mv);
            mv.dup();
            GetValue(expr, type, mv);
            ToNumber(type, mv);
            mv.dupX(type, ValType.Number);
            mv.dconst(1d);
            mv.add(Type.DOUBLE_TYPE);
            mv.toBoxed(ValType.Number);
            PutValue(expr, type, mv);
            return ValType.Number;
        }
        case POST_DEC: {
            // 12.3.4 Postfix Decrement Operator
            Expression expr = node.getOperand();
            ValType type = expr.accept(this, mv);
            mv.dup();
            GetValue(expr, type, mv);
            ToNumber(type, mv);
            mv.dupX(type, ValType.Number);
            mv.dconst(1d);
            mv.sub(Type.DOUBLE_TYPE);
            mv.toBoxed(ValType.Number);
            PutValue(expr, type, mv);
            return ValType.Number;
        }
        case DELETE: {
            // 12.4.3 The delete Operator
            Expression expr = node.getOperand();
            ValType type = expr.accept(this, mv);
            mv.toBoxed(type);
            mv.loadExecutionContext();
            mv.invoke(Methods.ScriptRuntime_delete);
            return ValType.Boolean;
        }
        case VOID: {
            // 12.4.4 The void Operator
            Expression expr = node.getOperand();
            if (!(expr instanceof Literal)) {
                ValType type = evalAndGetValue(expr, mv);
                mv.pop(type);
            }
            mv.loadUndefined();
            return ValType.Undefined;
        }
        case TYPEOF: {
            // 12.4.5 The typeof Operator
            Expression expr = node.getOperand();
            ValType type = expr.accept(this, mv);
            mv.toBoxed(type);
            mv.loadExecutionContext();
            mv.invoke(Methods.ScriptRuntime_typeof);
            return ValType.String;
        }
        case PRE_INC: {
            // 12.4.6 Prefix Increment Operator
            Expression expr = node.getOperand();
            ValType type = expr.accept(this, mv);
            mv.dup();
            GetValue(expr, type, mv);
            ToNumber(type, mv);
            mv.dconst(1d);
            mv.add(Type.DOUBLE_TYPE);
            mv.dupX(type, ValType.Number);
            mv.toBoxed(ValType.Number);
            PutValue(expr, type, mv);
            return ValType.Number;
        }
        case PRE_DEC: {
            // 12.4.7 Prefix Decrement Operator
            Expression expr = node.getOperand();
            ValType type = expr.accept(this, mv);
            mv.dup();
            GetValue(expr, type, mv);
            ToNumber(type, mv);
            mv.dconst(1d);
            mv.sub(Type.DOUBLE_TYPE);
            mv.dupX(type, ValType.Number);
            mv.toBoxed(ValType.Number);
            PutValue(expr, type, mv);
            return ValType.Number;
        }
        case POS: {
            // 12.4.8 Unary + Operator
            Expression expr = node.getOperand();
            ValType type = evalAndGetValue(expr, mv);
            ToNumber(type, mv);
            return ValType.Number;
        }
        case NEG: {
            // 12.4.9 Unary - Operator
            Expression expr = node.getOperand();
            ValType type = evalAndGetValue(expr, mv);
            ToNumber(type, mv);
            mv.neg(Type.DOUBLE_TYPE);
            return ValType.Number;
        }
        case BITNOT: {
            // 12.4.10 Bitwise NOT Operator ( ~ )
            Expression expr = node.getOperand();
            ValType type = evalAndGetValue(expr, mv);
            ToInt32(type, mv);
            mv.bitnot();
            return ValType.Number_int;
        }
        case NOT: {
            // 12.4.11 Logical NOT Operator ( ! )
            Expression expr = node.getOperand();
            ValType type = evalAndGetValue(expr, mv);
            ToBoolean(type, mv);
            mv.not();
            return ValType.Boolean;
        }
        default:
            throw new IllegalStateException(Objects.toString(node.getOperator(), "<null>"));
        }
    }

    /**
     * 14.4.12 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(YieldExpression node, ExpressionVisitor mv) {
        Expression expr = node.getExpression();
        if (expr != null) {
            evalAndGetBoxedValue(expr, mv);
        } else {
            mv.loadUndefined();
        }

        if (node.isDelegatedYield()) {
            delegatedYield(node, mv);
        } else {
            yield(node, mv);
        }

        return ValType.Any;
    }
}
