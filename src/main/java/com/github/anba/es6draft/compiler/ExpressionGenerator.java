/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static com.github.anba.es6draft.compiler.ArrayComprehensionGenerator.EvaluateArrayComprehension;
import static com.github.anba.es6draft.compiler.BindingInitializationGenerator.InitializeBoundNameWithInitializer;
import static com.github.anba.es6draft.compiler.BindingInitializationGenerator.InitializeBoundNameWithUndefined;
import static com.github.anba.es6draft.semantics.StaticSemantics.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;

import com.github.anba.es6draft.ast.*;
import com.github.anba.es6draft.ast.scope.BlockScope;
import com.github.anba.es6draft.ast.scope.Name;
import com.github.anba.es6draft.ast.scope.Scope;
import com.github.anba.es6draft.ast.scope.TopLevelScope;
import com.github.anba.es6draft.ast.scope.WithScope;
import com.github.anba.es6draft.ast.synthetic.ExpressionMethod;
import com.github.anba.es6draft.ast.synthetic.SpreadArrayLiteral;
import com.github.anba.es6draft.ast.synthetic.SpreadElementMethod;
import com.github.anba.es6draft.compiler.DefaultCodeGenerator.ValType;
import com.github.anba.es6draft.compiler.assembler.FieldName;
import com.github.anba.es6draft.compiler.assembler.Jump;
import com.github.anba.es6draft.compiler.assembler.MethodName;
import com.github.anba.es6draft.compiler.assembler.MethodTypeDescriptor;
import com.github.anba.es6draft.compiler.assembler.Type;
import com.github.anba.es6draft.compiler.assembler.Variable;
import com.github.anba.es6draft.parser.Parser;
import com.github.anba.es6draft.runtime.DeclarativeEnvironmentRecord;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.internal.Bootstrap;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.NativeCalls;
import com.github.anba.es6draft.runtime.internal.ResumptionPoint;
import com.github.anba.es6draft.runtime.objects.Eval.EvalFlags;
import com.github.anba.es6draft.runtime.objects.simd.SIMDType;
import com.github.anba.es6draft.runtime.types.builtins.ArrayObject;

/**
 *
 */
final class ExpressionGenerator extends DefaultCodeGenerator<ValType> {
    private static final class Fields {
        static final FieldName Intrinsics_ObjectPrototype = FieldName.findStatic(Types.Intrinsics,
                "ObjectPrototype", Types.Intrinsics);

        static final FieldName ScriptRuntime_EMPTY_ARRAY = FieldName.findStatic(
                Types.ScriptRuntime, "EMPTY_ARRAY", Types.Object_);
    }

    private static final class Methods {
        // class: Eval
        static final MethodName Eval_directEvalWithTranslate = MethodName
                .findStatic(Types.Eval, "directEval", Type.methodType(Types.Object, Types.Object_,
                        Types.ExecutionContext, Type.INT_TYPE));

        static final MethodName Eval_directEval = MethodName.findStatic(Types.Eval, "directEval",
                Type.methodType(Types.Object, Types.Object, Types.ExecutionContext, Type.INT_TYPE));

        // class: ExecutionContext
        static final MethodName ExecutionContext_resolveThisBinding = MethodName.findVirtual(
                Types.ExecutionContext, "resolveThisBinding", Type.methodType(Types.Object));

        // class: ArrayObject
        static final MethodName ArrayObject_ArrayCreate = MethodName.findStatic(Types.ArrayObject,
                "ArrayCreate",
                Type.methodType(Types.ArrayObject, Types.ExecutionContext, Type.LONG_TYPE));

        static final MethodName ArrayObject_DenseArrayCreate = MethodName.findStatic(
                Types.ArrayObject, "DenseArrayCreate",
                Type.methodType(Types.ArrayObject, Types.ExecutionContext, Types.Object_));

        static final MethodName ArrayObject_SparseArrayCreate = MethodName.findStatic(
                Types.ArrayObject, "SparseArrayCreate",
                Type.methodType(Types.ArrayObject, Types.ExecutionContext, Types.Object_));

        // class: Math
        static final MethodName Math_pow = MethodName.findStatic(Types.Math, "pow",
                Type.methodType(Type.DOUBLE_TYPE, Type.DOUBLE_TYPE, Type.DOUBLE_TYPE));

        // class: OrdinaryObject
        static final MethodName OrdinaryObject_ObjectCreate = MethodName.findStatic(
                Types.OrdinaryObject, "ObjectCreate",
                Type.methodType(Types.OrdinaryObject, Types.ExecutionContext, Types.Intrinsics));

        // class: RegExpConstructor
        static final MethodName RegExpConstructor_RegExpCreate = MethodName.findStatic(
                Types.RegExpConstructor, "RegExpCreate", Type.methodType(Types.RegExpObject,
                        Types.ExecutionContext, Types.Object, Types.Object));

        // class: ScriptRuntime
        static final MethodName ScriptRuntime_add_str = MethodName.findStatic(Types.ScriptRuntime,
                "add", Type.methodType(Types.CharSequence, Types.CharSequence, Types.CharSequence,
                        Types.ExecutionContext));

        static final MethodName ScriptRuntime_in = MethodName.findStatic(Types.ScriptRuntime, "in",
                Type.methodType(Type.BOOLEAN_TYPE, Types.Object, Types.Object,
                        Types.ExecutionContext));

        static final MethodName ScriptRuntime_typeof = MethodName.findStatic(Types.ScriptRuntime,
                "typeof", Type.methodType(Types.String, Types.Object));

        static final MethodName ScriptRuntime_typeof_Reference = MethodName.findStatic(
                Types.ScriptRuntime, "typeof",
                Type.methodType(Types.String, Types.Reference, Types.ExecutionContext));

        static final MethodName ScriptRuntime_InstanceofOperator = MethodName.findStatic(
                Types.ScriptRuntime, "InstanceofOperator", Type.methodType(Type.BOOLEAN_TYPE,
                        Types.Object, Types.Object, Types.ExecutionContext));

        static final MethodName ScriptRuntime_ArrayAccumulationSpreadElement = MethodName
                .findStatic(Types.ScriptRuntime, "ArrayAccumulationSpreadElement", Type.methodType(
                        Type.INT_TYPE, Types.ArrayObject, Type.INT_TYPE, Types.Object,
                        Types.ExecutionContext));

        static final MethodName ScriptRuntime_CheckCallable = MethodName.findStatic(
                Types.ScriptRuntime, "CheckCallable",
                Type.methodType(Types.Callable, Types.Object, Types.ExecutionContext));

        static final MethodName ScriptRuntime_defineLength = MethodName.findStatic(
                Types.ScriptRuntime, "defineLength",
                Type.methodType(Type.VOID_TYPE, Types.ArrayObject, Type.INT_TYPE));

        static final MethodName ScriptRuntime_defineProperty__int = MethodName.findStatic(
                Types.ScriptRuntime, "defineProperty", Type.methodType(Type.VOID_TYPE,
                        Types.ArrayObject, Type.INT_TYPE, Types.Object));

        static final MethodName ScriptRuntime_directEvalFallbackArguments = MethodName.findStatic(
                Types.ScriptRuntime, "directEvalFallbackArguments", Type.methodType(Types.Object_,
                        Types.Callable, Types.ExecutionContext, Types.Object, Types.Object_));

        static final MethodName ScriptRuntime_directEvalFallbackThisArgument = MethodName
                .findStatic(Types.ScriptRuntime, "directEvalFallbackThisArgument",
                        Type.methodType(Types.Object, Types.ExecutionContext));

        static final MethodName ScriptRuntime_directEvalFallbackHook = MethodName.findStatic(
                Types.ScriptRuntime, "directEvalFallbackHook",
                Type.methodType(Types.Callable, Types.ExecutionContext));

        static final MethodName ScriptRuntime_EvaluateArrowFunction = MethodName
                .findStatic(Types.ScriptRuntime, "EvaluateArrowFunction", Type.methodType(
                        Types.OrdinaryFunction, Types.RuntimeInfo$Function, Types.ExecutionContext));

        static final MethodName ScriptRuntime_EvaluateAsyncArrowFunction = MethodName.findStatic(
                Types.ScriptRuntime, "EvaluateAsyncArrowFunction", Type.methodType(
                        Types.OrdinaryAsyncFunction, Types.RuntimeInfo$Function,
                        Types.ExecutionContext));

        static final MethodName ScriptRuntime_EvaluateAsyncFunctionExpression = MethodName
                .findStatic(Types.ScriptRuntime, "EvaluateAsyncFunctionExpression", Type
                        .methodType(Types.OrdinaryAsyncFunction, Types.RuntimeInfo$Function,
                                Types.ExecutionContext));

        static final MethodName ScriptRuntime_EvaluateFunctionExpression = MethodName.findStatic(
                Types.ScriptRuntime, "EvaluateFunctionExpression", Type.methodType(
                        Types.OrdinaryConstructorFunction, Types.RuntimeInfo$Function,
                        Types.ExecutionContext));

        static final MethodName ScriptRuntime_EvaluateLegacyFunctionExpression = MethodName.findStatic(
                Types.ScriptRuntime, "EvaluateLegacyFunctionExpression", Type.methodType(
                        Types.LegacyConstructorFunction, Types.RuntimeInfo$Function, Types.ExecutionContext));

        static final MethodName ScriptRuntime_EvaluateConstructorGeneratorComprehension = MethodName.findStatic(
                Types.ScriptRuntime, "EvaluateConstructorGeneratorComprehension",
                Type.methodType(Types.GeneratorObject, Types.RuntimeInfo$Function, Types.ExecutionContext));

        static final MethodName ScriptRuntime_EvaluateGeneratorComprehension = MethodName.findStatic(
                Types.ScriptRuntime, "EvaluateGeneratorComprehension",
                Type.methodType(Types.GeneratorObject, Types.RuntimeInfo$Function, Types.ExecutionContext));

        static final MethodName ScriptRuntime_EvaluateLegacyGeneratorComprehension = MethodName
                .findStatic(Types.ScriptRuntime, "EvaluateLegacyGeneratorComprehension", Type
                        .methodType(Types.GeneratorObject, Types.RuntimeInfo$Function,
                                Types.ExecutionContext));

        static final MethodName ScriptRuntime_EvaluateConstructorGeneratorExpression = MethodName.findStatic(
                Types.ScriptRuntime, "EvaluateConstructorGeneratorExpression",
                Type.methodType(Types.OrdinaryConstructorGenerator, Types.RuntimeInfo$Function, Types.ExecutionContext));

        static final MethodName ScriptRuntime_EvaluateGeneratorExpression = MethodName.findStatic(
                Types.ScriptRuntime, "EvaluateGeneratorExpression",
                Type.methodType(Types.OrdinaryGenerator, Types.RuntimeInfo$Function, Types.ExecutionContext));

        static final MethodName ScriptRuntime_EvaluateLegacyGeneratorExpression = MethodName
                .findStatic(Types.ScriptRuntime, "EvaluateLegacyGeneratorExpression", Type
                        .methodType(Types.OrdinaryConstructorGenerator, Types.RuntimeInfo$Function,
                                Types.ExecutionContext));

        static final MethodName ScriptRuntime_EvaluateMethodDecorators = MethodName.findStatic(
                Types.ScriptRuntime, "EvaluateMethodDecorators", Type.methodType(Type.VOID_TYPE,
                        Types.OrdinaryObject, Types.ArrayList, Types.ExecutionContext));

        static final MethodName ScriptRuntime_BindThisValue = MethodName.findStatic(
                Types.ScriptRuntime, "BindThisValue",
                Type.methodType(Type.VOID_TYPE, Types.ScriptObject, Types.ExecutionContext));

        static final MethodName ScriptRuntime_GetNewTargetOrUndefined = MethodName.findStatic(
                Types.ScriptRuntime, "GetNewTargetOrUndefined",
                Type.methodType(Types.Object, Types.ExecutionContext));

        static final MethodName ScriptRuntime_GetNewTarget = MethodName.findStatic(
                Types.ScriptRuntime, "GetNewTarget",
                Type.methodType(Types.Constructor, Types.ExecutionContext));

        static final MethodName ScriptRuntime_GetSuperConstructor = MethodName.findStatic(
                Types.ScriptRuntime, "GetSuperConstructor",
                Type.methodType(Types.Constructor, Types.ExecutionContext));

        static final MethodName ScriptRuntime_IsBuiltinEval = MethodName.findStatic(
                Types.ScriptRuntime, "IsBuiltinEval",
                Type.methodType(Type.BOOLEAN_TYPE, Types.Callable, Types.ExecutionContext));

        static final MethodName ScriptRuntime_PrepareForTailCall = MethodName.findStatic(
                Types.ScriptRuntime, "PrepareForTailCall", Type.methodType(Types.Object,
                        Types.Callable, Types.ExecutionContext, Types.Object, Types.Object_));

        static final MethodName ScriptRuntime_PrepareForTailCallUnchecked = MethodName.findStatic(
                Types.ScriptRuntime, "PrepareForTailCall", Type.methodType(Types.Object,
                        Types.Object, Types.ExecutionContext, Types.Object, Types.Object_));

        static final MethodName ScriptRuntime_SpreadArray = MethodName.findStatic(
                Types.ScriptRuntime, "SpreadArray",
                Type.methodType(Types.Object_, Types.Object, Types.ExecutionContext));

        static final MethodName ScriptRuntime_NativeCallSpreadArray = MethodName.findStatic(
                Types.ScriptRuntime, "NativeCallSpreadArray",
                Type.methodType(Types.Object_, Types.Object, Types.ExecutionContext));

        static final MethodName ScriptRuntime_toFlatArray = MethodName.findStatic(
                Types.ScriptRuntime, "toFlatArray",
                Type.methodType(Types.Object_, Types.Object_, Types.ExecutionContext));

        static final MethodName ScriptRuntime_toStr = MethodName.findStatic(Types.ScriptRuntime,
                "toStr", Type.methodType(Types.CharSequence, Types.Object, Types.ExecutionContext));

        static final MethodName ScriptRuntime_functionSent = MethodName.findStatic(Types.ScriptRuntime,
                "functionSent", Type.methodType(Types.Object, Types.ExecutionContext));

        // class: StringBuilder
        static final MethodName StringBuilder_append_Charsequence = MethodName.findVirtual(
                Types.StringBuilder, "append",
                Type.methodType(Types.StringBuilder, Types.CharSequence));

        static final MethodName StringBuilder_append_String = MethodName.findVirtual(
                Types.StringBuilder, "append", Type.methodType(Types.StringBuilder, Types.String));

        static final MethodName StringBuilder_init = MethodName.findConstructor(
                Types.StringBuilder, Type.methodType(Type.VOID_TYPE));

        static final MethodName StringBuilder_toString = MethodName.findVirtual(
                Types.StringBuilder, "toString", Type.methodType(Types.String));
    }

    private static final int MAX_JVM_ARGUMENTS = 255;
    private static final int BOOTSTRAP_ARGUMENTS = 3;
    private static final int MAX_DYN_ARGUMENTS = MAX_JVM_ARGUMENTS - BOOTSTRAP_ARGUMENTS;

    public ExpressionGenerator(CodeGenerator codegen) {
        super(codegen);
    }

    private static void invokeDynamicCall(CodeVisitor mv) {
        // stack: [func(Callable), cx, thisValue, args] -> [result]
        mv.invokedynamic(Bootstrap.getCallName(), Bootstrap.getCallMethodDescriptor(),
                Bootstrap.getCallBootstrap());
    }

    private static void invokeDynamicConstruct(CodeVisitor mv) {
        // stack: [constructor(Constructor), cx, args] -> [result]
        mv.invokedynamic(Bootstrap.getConstructName(), Bootstrap.getConstructMethodDescriptor(),
                Bootstrap.getConstructBootstrap());
    }

    private static void invokeDynamicSuper(CodeVisitor mv) {
        // stack: [constructor(Constructor), cx, newTarget, args] -> [result]
        mv.invokedynamic(Bootstrap.getSuperName(), Bootstrap.getSuperMethodDescriptor(),
                Bootstrap.getSuperBootstrap());
    }

    private static ValType invokeDynamicNativeCall(String name, Type[] arguments, CodeVisitor mv) {
        // stack: [cx, ...args] -> [result]
        MethodTypeDescriptor desc = MethodTypeDescriptor.methodType(Types.Object, arguments);
        mv.invokedynamic(NativeCalls.getNativeCallName(name), desc, NativeCalls.getNativeCallBootstrap());
        return ValType.of(desc.returnType());
    }

    private static void invokeDynamicOperator(BinaryExpression.Operator operator, CodeVisitor mv) {
        // stack: [lval, rval, cx?] -> [result]
        mv.invokedynamic(Bootstrap.getName(operator), Bootstrap.getMethodDescriptor(operator),
                Bootstrap.getBootstrap(operator));
    }

    private static void invokeDynamicConcat(int numberOfStrings, CodeVisitor mv) {
        mv.invokedynamic(Bootstrap.getConcatName(),
                Bootstrap.getConcatMethodDescriptor(numberOfStrings),
                Bootstrap.getConcatBootstrap());
    }

    private boolean isTailCall(Expression node, CodeVisitor mv) {
        return !codegen.isEnabled(Compiler.Option.NoTailCall) && mv.isTailCall(node);
    }

    /**
     * [12.3.3.1.1 Runtime Semantics: EvaluateNew(thisCall, constructProduction, arguments)]
     * 
     * @param node
     *            the <code>NewExpression</code> node
     * @param mv
     *            the code visitor
     * @return the returned value type
     */
    private ValType EvaluateNew(NewExpression node, CodeVisitor mv) {
        /* steps 1-2 (not applicable) */
        /* steps 3-5 */
        ValType type = node.getExpression().accept(this, mv);
        mv.toBoxed(type);
        mv.loadExecutionContext();
        /* steps 6-7 */
        ArgumentListEvaluation(node, node.getArguments(), mv);
        /* steps 8-9 */
        mv.lineInfo(node);
        invokeDynamicConstruct(mv);
        return ValType.Object;
    }

    private boolean isEnclosedByWithStatement(Name name, Scope currentScope) {
        for (Scope scope : currentScope) {
            if (scope instanceof WithScope) {
                return true;
            }
            if (scope.isDeclared(name)) {
                return false;
            }
        }
        return codegen.isEnabled(Parser.Option.EnclosedByWithStatement);
    }

    private boolean isEnclosedByWithStatement(Scope currentScope) {
        for (Scope scope : currentScope) {
            if (scope instanceof WithScope) {
                return true;
            }
        }
        return codegen.isEnabled(Parser.Option.EnclosedByWithStatement);
    }

    private boolean isEnclosedByLexicalDeclaration(Scope currentScope) {
        final boolean catchVar = codegen.isEnabled(CompatibilityOption.CatchVarStatement);
        final boolean catchPattern = codegen.isEnabled(CompatibilityOption.CatchVarPattern);
        TopLevelScope top = currentScope.getTop();
        for (Scope scope : currentScope) {
            if (scope instanceof BlockScope) {
                if (catchVar) {
                    ScopedNode node = scope.getNode();
                    if (node instanceof CatchClause) {
                        if (!catchPattern || ((CatchClause) node).getCatchParameter() instanceof BindingIdentifier) {
                            continue;
                        }
                    }
                }
                if (!((BlockScope) scope).lexicallyDeclaredNames().isEmpty()) {
                    return true;
                }
            } else if (scope == top) {
                break;
            }
        }
        if (!top.lexicallyDeclaredNames().isEmpty()) {
            TopLevelNode<?> topNode = top.getNode();
            if (topNode instanceof Script) {
                return ((Script) topNode).isEvalScript();
            }
            return true;
        }
        return codegen.isEnabled(Parser.Option.EnclosedByLexicalDeclaration);
    }

    private static boolean isGlobalScope(Scope currentScope) {
        ScopedNode node = currentScope.getNode();
        if (node instanceof Script) {
            return ((Script) node).isGlobalScope();
        }
        return false;
    }

    private static boolean isGlobalThis(Scope currentScope) {
        for (Scope scope = currentScope;;) {
            TopLevelScope top = scope.getTop();
            TopLevelNode<?> node = top.getNode();
            if (node instanceof Script) {
                return ((Script) node).isGlobalThis();
            }
            if (node instanceof Module) {
                return true;
            }
            assert node instanceof FunctionNode : "class=" + node.getClass();
            if (((FunctionNode) node).getThisMode() != FunctionNode.ThisMode.Lexical) {
                return false;
            }
            scope = top.getEnclosingScope();
        }
    }

    private enum CallType {
        Identifier, IdentifierWith, Eval, EvalWith, Property, Value
    }

    private CallType callTypeOf(Expression call, Expression base, Scope scope) {
        if (base instanceof IdentifierReference) {
            IdentifierReference ident = (IdentifierReference) base;
            boolean directEval = call instanceof CallExpression && "eval".equals(ident.getName());
            if (isEnclosedByWithStatement(ident.toName(), scope)) {
                return directEval ? CallType.EvalWith : CallType.IdentifierWith;
            }
            return directEval ? CallType.Eval : CallType.Identifier;
        }
        if (base instanceof ElementAccessor || base instanceof PropertyAccessor) {
            return CallType.Property;
        }
        if (base instanceof SuperElementAccessor || base instanceof SuperPropertyAccessor) {
            return CallType.Property;
        }
        return CallType.Value;
    }

    /**
     * [12.3.4.2 Runtime Semantics: EvaluateCall( ref, arguments, tailPosition )]
     * 
     * @param call
     *            the function call expression
     * @param base
     *            the call expression's base node
     * @param arguments
     *            the list of function call arguments
     * @param mv
     *            the code visitor
     */
    private ValType EvaluateCall(Expression call, Expression base, List<Expression> arguments, CodeVisitor mv) {
        switch (callTypeOf(call, base, mv.getScope())) {
        case Identifier:
            return EvaluateCallIdent(call, (IdentifierReference) base, arguments, mv);
        case IdentifierWith:
            return EvaluateCallIdentWith(call, (IdentifierReference) base, arguments, mv);
        case Eval:
            return EvaluateCallEval(call, (IdentifierReference) base, arguments, mv);
        case EvalWith:
            return EvaluateCallEvalWith(call, (IdentifierReference) base, arguments, mv);
        case Property:
            return EvaluateCallProperty(call, (LeftHandSideExpression) base, arguments, mv);
        case Value:
            return EvaluateCallValue(call, base, arguments, mv);
        default:
            throw new AssertionError();
        }
    }

    private ValType EvaluateCallProperty(Expression call, LeftHandSideExpression base, List<Expression> arguments,
            CodeVisitor mv) {
        /* steps 1-4 */
        // stack: [] -> [func, thisValue]
        ReferenceOp.propertyOp(base).referenceValueAndThis(base, mv, codegen);

        // stack: [func, thisValue] -> [func, cx, thisValue]
        mv.loadExecutionContext();
        mv.swap();

        /* step 5 */
        // stack: [func, cx, thisValue] -> [result]
        return EvaluateDirectCall(call, arguments, mv);
    }

    private ValType EvaluateCallValue(Expression call, Expression base, List<Expression> arguments, CodeVisitor mv) {
        // stack: [] -> [func]
        ValType type = base.accept(this, mv);
        mv.toBoxed(type);

        /* steps 1-2 (not applicable) */
        // GetValue(...)

        /* steps 3-4 */
        // stack: [func] -> [func, cx, thisValue]
        mv.loadExecutionContext();
        mv.loadUndefined();

        /* step 5 */
        // stack: [func, cx, thisValue] -> [result]
        return EvaluateDirectCall(call, arguments, mv);
    }

    private ValType EvaluateCallIdent(Expression call, IdentifierReference base, List<Expression> arguments,
            CodeVisitor mv) {
        /* steps 1-2 */
        // stack: [] -> [func]
        ReferenceOp.of(base).referenceValue(base, mv, codegen);

        /* steps 3-4 */
        // stack: [func] -> [func, cx, thisValue]
        mv.loadExecutionContext();
        mv.loadUndefined();

        /* step 5 */
        // stack: [func, cx, thisValue] -> [result]
        return EvaluateDirectCall(call, arguments, mv);
    }

    private ValType EvaluateCallIdentWith(Expression call, IdentifierReference base, List<Expression> arguments,
            CodeVisitor mv) {
        /* steps 1-4 */
        // stack: [] -> [func, thisValue]
        ReferenceOp.LOOKUP.referenceValueAndThis(base, mv, codegen);

        // stack: [func, thisValue] -> [func, cx, thisValue]
        mv.loadExecutionContext();
        mv.swap();

        /* step 5 */
        // stack: [func, cx, thisValue] -> [result]
        return EvaluateDirectCall(call, arguments, mv);
    }

    private ValType EvaluateCallEval(Expression call, IdentifierReference base, List<Expression> arguments,
            CodeVisitor mv) {
        /* steps 1-2 */
        // stack: [] -> [func]
        ReferenceOp.of(base).referenceValue(base, mv, codegen);

        /* steps 3-4 (omitted) */
        /* step 5 */
        // stack: [func] -> [result]
        return EvaluateDirectCallEval(call, arguments, false, mv);
    }

    private ValType EvaluateCallEvalWith(Expression call, IdentifierReference base, List<Expression> arguments,
            CodeVisitor mv) {
        /* steps 1-4 */
        // stack: [] -> [func, thisValue]
        ReferenceOp.LOOKUP.referenceValueAndThis(base, mv, codegen);

        /* step 5 */
        // stack: [func, thisValue] -> [result]
        return EvaluateDirectCallEval(call, arguments, true, mv);
    }

    /**
     * [12.3.4.3 Runtime Semantics: EvaluateDirectCall( func, thisValue, arguments, tailPosition )]
     * 
     * @param call
     *            the function call expression
     * @param arguments
     *            the function arguments
     * @param mv
     *            the code visitor
     */
    private ValType EvaluateDirectCall(Expression call, List<Expression> arguments, CodeVisitor mv) {
        /* steps 1-2 */
        // stack: [func, cx, thisValue] -> [func, cx, thisValue, args]
        ArgumentListEvaluation(call, arguments, mv);

        /* steps 3-9 */
        mv.lineInfo(call);
        if (isTailCall(call, mv)) {
            // stack: [func, cx, thisValue, args] -> [<func(Callable), thisValue, args>]
            mv.invoke(Methods.ScriptRuntime_PrepareForTailCallUnchecked);
        } else {
            // stack: [func, cx, thisValue, args] -> [result]
            invokeDynamicCall(mv);
        }
        return ValType.Any;
    }

    /**
     * [12.3.4.3 Runtime Semantics: EvaluateDirectCall( func, thisValue, arguments, tailPosition )]
     * 
     * @param call
     *            the function call expression
     * @param arguments
     *            the function arguments
     * @param hasThisValue
     *            {@code true} if the thisValue is on the stack
     * @param mv
     *            the code visitor
     */
    private ValType EvaluateDirectCallEval(Expression call, List<Expression> arguments, boolean hasThisValue,
            CodeVisitor mv) {
        Jump afterCall = new Jump(), notEval = new Jump();
        if (hasThisValue) {
            // stack: [func, thisValue] -> [thisValue, func]
            mv.swap();
        }

        /* steps 1-2 (EvaluateDirectCall) */
        // stack: [thisValue?, func] -> [thisValue?, args?, func]
        boolean constantArguments = hasConstantArguments(arguments);
        if (!constantArguments) {
            ArgumentListEvaluation(call, arguments, mv);
            mv.swap();
        }

        // Emit line info after evaluating arguments.
        mv.lineInfo(call);

        /* steps 3-4 (EvaluateDirectCall) */
        // stack: [thisValue?, args?, func] -> [thisValue?, args?, func(Callable)]
        mv.loadExecutionContext();
        mv.invoke(Methods.ScriptRuntime_CheckCallable);

        // stack: [thisValue?, args?, func(Callable)] -> [thisValue?, args?, func(Callable)]
        mv.dup();
        mv.loadExecutionContext();
        mv.invoke(Methods.ScriptRuntime_IsBuiltinEval);
        mv.ifeq(notEval);
        {
            PerformEval(call, arguments, hasThisValue, afterCall, mv);
        }
        mv.mark(notEval);

        // stack: [thisValue?, args?, func(Callable)] -> [func(Callable), cx, thisValue, args]
        if (constantArguments) {
            if (hasThisValue) {
                // stack: [thisValue, func(Callable)] -> [...]
                mv.loadExecutionContext();
                mv.swap1_2();
                ArgumentListEvaluation(call, arguments, mv);
            } else {
                // stack: [func(Callable)] -> [...]
                mv.loadExecutionContext();
                mv.loadUndefined();
                ArgumentListEvaluation(call, arguments, mv);
            }
        } else {
            if (hasThisValue) {
                // stack: [thisValue, args, func(Callable)] -> [...]
                mv.loadExecutionContext();
                mv.swap2();
            } else {
                // stack: [args, func(Callable)] -> [...]
                mv.swap();
                mv.loadExecutionContext();
                mv.loadUndefined();
                mv.swap1_2();
            }
        }

        if (codegen.isEnabled(CompatibilityOption.Realm)) {
            // direct-eval fallback hook
            Jump noEvalHook = new Jump();
            mv.loadExecutionContext();
            mv.invoke(Methods.ScriptRuntime_directEvalFallbackHook);
            mv.ifnull(noEvalHook);
            {
                // stack: [func(Callable), cx, thisValue, args] -> [args']
                mv.invoke(Methods.ScriptRuntime_directEvalFallbackArguments);

                // stack: [args'] -> []
                Variable<Object[]> fallbackArguments = mv.newScratchVariable(Object[].class);
                mv.store(fallbackArguments);

                // stack: [] -> [func(Callable), cx, thisValue, args']
                mv.loadExecutionContext();
                mv.invoke(Methods.ScriptRuntime_directEvalFallbackHook);
                mv.loadExecutionContext();
                mv.loadExecutionContext();
                mv.invoke(Methods.ScriptRuntime_directEvalFallbackThisArgument);
                mv.load(fallbackArguments);

                mv.freeVariable(fallbackArguments);
            }
            mv.mark(noEvalHook);
        }

        /* steps 5-9 (EvaluateDirectCall) */
        if (isTailCall(call, mv)) {
            // stack: [func(Callable), cx, thisValue, args'] -> [<func(Callable), thisValue, args>]
            mv.invoke(Methods.ScriptRuntime_PrepareForTailCall);
        } else {
            // stack: [func(Callable), cx, thisValue, args] -> [result]
            invokeDynamicCall(mv);
        }

        mv.mark(afterCall);
        return ValType.Any;
    }

    /**
     * [18.2.1.1] Direct Call to Eval
     * 
     * @param call
     *            the function call expression
     * @param arguments
     *            the list of function call arguments
     * @param hasThisValue
     *            {@code true} if the thisValue is on the stack
     * @param afterCall
     *            the label after the call instruction
     * @param mv
     *            the code visitor
     */
    private void PerformEval(Expression call, List<Expression> arguments, boolean hasThisValue, Jump afterCall,
            CodeVisitor mv) {
        int evalFlags = EvalFlags.Direct.getValue();
        if (mv.isStrict()) {
            evalFlags |= EvalFlags.Strict.getValue();
        }
        if (mv.isGlobalCode()) {
            evalFlags |= EvalFlags.GlobalCode.getValue();
        }
        if (isGlobalScope(mv.getScope())) {
            evalFlags |= EvalFlags.GlobalScope.getValue();
        }
        if (isGlobalThis(mv.getScope())) {
            evalFlags |= EvalFlags.GlobalThis.getValue();
        }
        if (isEnclosedByWithStatement(mv.getScope())) {
            evalFlags |= EvalFlags.EnclosedByWithStatement.getValue();
        }
        if (!mv.isStrict() && isEnclosedByLexicalDeclaration(mv.getScope())) {
            evalFlags |= EvalFlags.EnclosedByLexicalDeclaration.getValue();
        }

        // stack: [thisValue?, args?, func(Callable)] -> [thisValue?, args?]
        mv.pop();

        // stack: [thisValue?, args?] -> [args?]
        boolean constantArguments = hasConstantArguments(arguments);
        if (hasThisValue) {
            if (constantArguments) {
                // stack: [thisValue] -> []
                mv.pop();
            } else {
                // stack: [thisValue, args] -> [args]
                mv.swap();
                mv.pop();
            }
        }

        if (codegen.isEnabled(CompatibilityOption.Realm)) {
            if (constantArguments) {
                // stack: [] -> [args]
                ArgumentListEvaluation(call, arguments, mv);
            }
            // stack: [args] -> [result]
            mv.loadExecutionContext();
            mv.iconst(evalFlags);
            mv.invoke(Methods.Eval_directEvalWithTranslate);
            mv.goTo(afterCall);
        } else {
            if (arguments.isEmpty()) {
                assert constantArguments : "empty arguments list is constant";
                // stack: [] -> [result]
                mv.loadUndefined();
                mv.goTo(afterCall);
            } else if (hasArguments(arguments)) {
                if (constantArguments) {
                    // stack: [] -> [arg_0]
                    ValType type = arguments.get(0).accept(this, mv);
                    mv.toBoxed(type);
                } else {
                    // stack: [args] -> [arg_0]
                    mv.iconst(0);
                    mv.aaload();
                }
                mv.loadExecutionContext();
                mv.iconst(evalFlags);
                mv.invoke(Methods.Eval_directEval);
                mv.goTo(afterCall);
            } else {
                assert !constantArguments : "spread arguments list is not constant";
                Jump emptyArguments = new Jump();
                mv.dup();
                mv.arraylength();
                mv.ifeq(emptyArguments);
                {
                    mv.iconst(0);
                    mv.aaload();
                    mv.loadExecutionContext();
                    mv.iconst(evalFlags);
                    mv.invoke(Methods.Eval_directEval);
                    mv.goTo(afterCall);
                }
                mv.mark(emptyArguments);
                mv.pop();
                mv.loadUndefined();
                mv.goTo(afterCall);
            }
        }
    }

    private static boolean hasArguments(List<Expression> arguments) {
        for (Expression argument : arguments) {
            if (!(argument instanceof CallSpreadElement)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasConstantArguments(List<Expression> arguments) {
        for (Expression argument : arguments) {
            if (!(argument instanceof Literal)) {
                return false;
            }
        }
        return true;
    }

    /**
     * [12.3.6.1 ArgumentListEvaluation]
     * 
     * @param call
     *            the call expression node
     * @param arguments
     *            the list of function call arguments
     * @param mv
     *            the code visitor
     */
    private void ArgumentListEvaluation(Expression call, List<Expression> arguments, CodeVisitor mv) {
        if (arguments.isEmpty()) {
            mv.get(Fields.ScriptRuntime_EMPTY_ARRAY);
        } else if (arguments.size() == 1 && arguments.get(0) instanceof CallSpreadElement) {
            ((CallSpreadElement) arguments.get(0)).accept(this, mv);
        } else {
            boolean hasSpread = false;
            mv.anewarray(arguments.size(), Types.Object);
            for (int i = 0, size = arguments.size(); i < size; ++i) {
                mv.dup();
                mv.iconst(i);
                /* [12.3.5 Argument Lists] ArgumentListEvaluation */
                Expression argument = arguments.get(i);
                hasSpread |= (argument instanceof CallSpreadElement);
                ValType argType = argument.accept(this, mv);
                mv.toBoxed(argType);
                mv.astore(Types.Object);
            }
            if (hasSpread) {
                mv.loadExecutionContext();
                mv.lineInfo(call);
                mv.invoke(Methods.ScriptRuntime_toFlatArray);
            }
        }
    }

    /* ----------------------------------------------------------------------------------------- */

    @Override
    protected ValType visit(Node node, CodeVisitor mv) {
        throw new IllegalStateException(String.format("node-class: %s", node.getClass()));
    }

    /**
     * 12.2.4.2.5 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(ArrayComprehension node, CodeVisitor mv) {
        return EvaluateArrayComprehension(codegen, node, mv);
    }

    /**
     * 12.2.5 Array Initializer
     * <p>
     * 12.2.5.3 Runtime Semantics: Evaluation<br>
     * 12.2.5.2 Runtime Semantics: ArrayAccumulation
     */
    @Override
    public ValType visit(ArrayLiteral node, CodeVisitor mv) {
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
            // Try to initialize array with faster {Dense, Sparse}ArrayCreate methods
            int length = node.getElements().size();
            float density = (float) (length - elision) / length;
            if ((density >= 0.25f && length < 0x10) || (density >= 0.75f && length < 0x1000)) {
                mv.loadExecutionContext();
                mv.anewarray(length, Types.Object);
                int nextIndex = 0;
                for (Expression element : node.getElements()) {
                    if (element instanceof Elision) {
                        // Elision
                    } else {
                        mv.dup();
                        mv.iconst(nextIndex);
                        ValType elementType = element.accept(this, mv);
                        mv.toBoxed(elementType);
                        mv.astore(Types.Object);
                    }
                    nextIndex += 1;
                }
                if (elision == 0) {
                    mv.invoke(Methods.ArrayObject_DenseArrayCreate);
                } else {
                    mv.invoke(Methods.ArrayObject_SparseArrayCreate);
                }
                return ValType.Object;
            }
        }

        if (!hasSpread) {
            int length = node.getElements().size();
            mv.loadExecutionContext();
            mv.lconst(length); // initialize with correct "length"
            mv.invoke(Methods.ArrayObject_ArrayCreate);

            int nextIndex = 0;
            for (Expression element : node.getElements()) {
                if (element instanceof Elision) {
                    // Elision
                } else {
                    mv.dup();
                    mv.iconst(nextIndex);
                    ValType elementType = element.accept(this, mv);
                    mv.toBoxed(elementType);
                    mv.invoke(Methods.ScriptRuntime_defineProperty__int);
                }
                nextIndex += 1;
            }
            assert nextIndex == length;
            // Skip Put(array, "length", nextIndex, false), array is initialized with fixed length.
        } else {
            mv.loadExecutionContext();
            mv.lconst(0);
            mv.invoke(Methods.ArrayObject_ArrayCreate);
            mv.dup();

            // stack: [array, array, nextIndex]
            mv.iconst(0); // nextIndex

            arrayLiteralWithSpread(node, mv);

            // stack: [array, array, nextIndex] -> [array]
            mv.invoke(Methods.ScriptRuntime_defineLength);
        }

        return ValType.Object;
    }

    /**
     * 12.2.4.1.3 Runtime Semantics: Evaluation<br>
     * 12.2.4.1.2 Runtime Semantics: Array Accumulation
     */
    @Override
    public ValType visit(SpreadArrayLiteral node, CodeVisitor mv) {
        // stack: [array, nextIndex]
        arrayLiteralWithSpread(node, mv);

        // stack: [array, nextIndex] -> [nextIndex]
        mv.swap();
        mv.pop();

        return ValType.Any;
    }

    /**
     * 12.2.4.1.3 Runtime Semantics: Evaluation<br>
     * 12.2.4.1.2 Runtime Semantics: Array Accumulation
     * 
     * @param node
     *            the array literal
     * @param mv
     *            the expresion visitor
     */
    private void arrayLiteralWithSpread(ArrayLiteral node, CodeVisitor mv) {
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
                mv.iadd();
                elisionWidth = 0;
            }
            if (element instanceof SpreadElement) {
                element.accept(this, mv);
            } else {
                // stack: [array, nextIndex] -> [array, nextIndex, array, nextIndex]
                mv.dup2();
                ValType elementType = element.accept(this, mv);
                mv.toBoxed(elementType);
                // stack: [array, nextIndex, array, nextIndex, value] -> [array, nextIndex]
                mv.invoke(Methods.ScriptRuntime_defineProperty__int);
                elisionWidth += 1;
            }
        }
        if (elisionWidth != 0) {
            mv.iconst(elisionWidth);
            mv.iadd();
        }
    }

    /**
     * 12.2.4.1.2 Runtime Semantics: Array Accumulation
     */
    @Override
    public ValType visit(SpreadElement node, CodeVisitor mv) {
        // stack: [array, nextIndex] -> [array, array, nextIndex]
        mv.swap();
        mv.dupX1();
        mv.swap();

        // stack: [array, array, nextIndex] -> [array, array, nextIndex, value]
        Expression spread = node.getExpression();
        ValType spreadType = spread.accept(this, mv);
        mv.toBoxed(spreadType);

        // stack: [array, array, nextIndex, value] -> [array, nextIndex']
        mv.loadExecutionContext();
        mv.lineInfo(node);
        mv.invoke(Methods.ScriptRuntime_ArrayAccumulationSpreadElement);

        return ValType.Any;
    }

    /**
     * 12.2.4.1.3 Runtime Semantics: Evaluation<br>
     * 12.2.4.1.2 Runtime Semantics: Array Accumulation
     */
    @Override
    public ValType visit(SpreadElementMethod node, CodeVisitor mv) {
        MethodName method = codegen.compile(node, mv);
        boolean hasResume = node.hasResumePoint();

        mv.enterVariableScope();
        Variable<ArrayObject> array = mv.newVariable("array", ArrayObject.class);
        Variable<Integer> nextIndex = mv.newVariable("nextIndex", int.class);

        // stack: [array, nextIndex] -> [array]
        mv.store(nextIndex);
        mv.dup();
        mv.store(array);

        // stack: [array] -> [array, nextIndex']
        mv.lineInfo(0); // 0 = hint for stacktraces to omit this frame
        if (hasResume) {
            mv.callWithSuspend(method, array, nextIndex);
        } else {
            mv.call(method, array, nextIndex);
        }
        mv.exitVariableScope();

        return ValType.Any;
    }

    /**
     * 14.2.10 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(ArrowFunction node, CodeVisitor mv) {
        MethodName method = codegen.compile(node);

        /* steps 1-4 */
        mv.invoke(method);
        mv.loadExecutionContext();
        mv.invoke(Methods.ScriptRuntime_EvaluateArrowFunction);

        /* step 5 */
        return ValType.Object;
    }

    /**
     * 12.14 Assignment Operators
     * <p>
     * 12.14.4 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(AssignmentExpression node, CodeVisitor mv) {
        return assignmentOp(node).emit(node, mv, this);
    }

    private static AssignmentOp assignmentOp(AssignmentExpression node) {
        switch (node.getOperator()) {
        case ASSIGN_ADD:
            return CompoundAssignmentOp.ADD;
        case ASSIGN_BITAND:
            return CompoundAssignmentOp.BITAND;
        case ASSIGN_BITOR:
            return CompoundAssignmentOp.BITOR;
        case ASSIGN_BITXOR:
            return CompoundAssignmentOp.BITXOR;
        case ASSIGN_DIV:
            return CompoundAssignmentOp.DIV;
        case ASSIGN_EXP:
            return CompoundAssignmentOp.EXP;
        case ASSIGN_MOD:
            return CompoundAssignmentOp.MOD;
        case ASSIGN_MUL:
            return CompoundAssignmentOp.MUL;
        case ASSIGN_SHL:
            return CompoundAssignmentOp.SHL;
        case ASSIGN_SHR:
            return CompoundAssignmentOp.SHR;
        case ASSIGN_SUB:
            return CompoundAssignmentOp.SUB;
        case ASSIGN_USHR:
            return CompoundAssignmentOp.USHR;
        case ASSIGN:
            if (node.getLeft() instanceof AssignmentPattern) {
                return AssignmentOp.DESTRUCTURING;
            }
            return AssignmentOp.ASSIGN;
        default:
            throw new AssertionError(Objects.toString(node.getOperator(), "<null>"));
        }
    }

    private static abstract class AssignmentOp {
        abstract ValType emit(AssignmentExpression node, CodeVisitor mv, ExpressionGenerator gen);

        static final AssignmentOp DESTRUCTURING = new AssignmentOp() {
            @Override
            ValType emit(AssignmentExpression node, CodeVisitor mv, ExpressionGenerator gen) {
                AssignmentPattern left = (AssignmentPattern) node.getLeft();
                Expression right = node.getRight();

                ValType rtype = right.accept(gen, mv);
                if (node.hasCompletion()) {
                    mv.dup(rtype);
                }
                mv.toBoxed(rtype);
                DestructuringAssignmentGenerator.DestructuringAssignment(gen.codegen, left, mv);
                return node.hasCompletion() ? rtype : ValType.Empty;
            }
        };

        static final AssignmentOp ASSIGN = new AssignmentOp() {
            @Override
            ValType emit(AssignmentExpression node, CodeVisitor mv, ExpressionGenerator gen) {
                LeftHandSideExpression left = node.getLeft();
                Expression right = node.getRight();
                ReferenceOp<LeftHandSideExpression> op = ReferenceOp.of(left);

                // stack: [] -> [lref, rval]
                ValType ltype = op.reference(left, mv, gen.codegen);
                ValType rtype = right.accept(gen, mv);
                if (IsAnonymousFunctionDefinition(right) && IsIdentifierRef(left)) {
                    SetFunctionName(right, ((IdentifierReference) left).getName(), mv);
                }
                return op.putValue(left, ltype, rtype, node.hasCompletion(), mv);
            }
        };
    }

    private static abstract class CompoundAssignmentOp extends AssignmentOp {
        abstract ValType convertLeft(ValType type, CodeVisitor mv);

        abstract ValType convertRight(ValType type, CodeVisitor mv);

        abstract ValType operation(CodeVisitor mv);

        @Override
        final ValType emit(AssignmentExpression node, CodeVisitor mv, ExpressionGenerator gen) {
            LeftHandSideExpression left = node.getLeft();
            Expression right = node.getRight();
            ReferenceOp<LeftHandSideExpression> op = ReferenceOp.of(left);

            ValType ltype = op.referenceForUpdate(left, mv, gen.codegen);
            ValType vtype = op.getValue(left, ltype, mv);
            // lref lval
            if (right instanceof Literal) {
                vtype = convertLeft(vtype, mv);
            }
            ValType rtype = right.accept(gen, mv);
            if (!(right instanceof Literal)) {
                mv.swap(vtype, rtype);
                vtype = convertLeft(vtype, mv);
                mv.swap(rtype, vtype);
            }
            convertRight(rtype, mv);
            // lref lval rval
            ValType result = operation(mv);
            // r lref r
            return op.putValue(left, ltype, result, node.hasCompletion(), mv);
        }

        static abstract class ArithmeticCompoundAssignmentOp extends CompoundAssignmentOp {
            @Override
            final ValType convertLeft(ValType type, CodeVisitor mv) {
                ToNumber(type, mv);
                return ValType.Number;
            }

            @Override
            final ValType convertRight(ValType type, CodeVisitor mv) {
                ToNumber(type, mv);
                return ValType.Number;
            }
        }

        static abstract class IntegerCompoundAssignmentOp extends CompoundAssignmentOp {
            @Override
            final ValType convertLeft(ValType type, CodeVisitor mv) {
                ToInt32(type, mv);
                return ValType.Number_int;
            }

            @Override
            final ValType convertRight(ValType type, CodeVisitor mv) {
                ToInt32(type, mv);
                return ValType.Number_int;
            }
        }

        // Extension: Exponentiation Operator
        static final CompoundAssignmentOp EXP = new ArithmeticCompoundAssignmentOp() {
            @Override
            ValType operation(CodeVisitor mv) {
                mv.invoke(Methods.Math_pow);
                return ValType.Number;
            }
        };
        // 12.6 Multiplicative Operators
        static final CompoundAssignmentOp MUL = new ArithmeticCompoundAssignmentOp() {
            @Override
            ValType operation(CodeVisitor mv) {
                mv.dmul();
                return ValType.Number;
            }
        };
        // 12.6 Multiplicative Operators
        static final CompoundAssignmentOp DIV = new ArithmeticCompoundAssignmentOp() {
            @Override
            ValType operation(CodeVisitor mv) {
                mv.ddiv();
                return ValType.Number;
            }
        };
        // 12.6 Multiplicative Operators
        static final CompoundAssignmentOp MOD = new ArithmeticCompoundAssignmentOp() {
            @Override
            ValType operation(CodeVisitor mv) {
                mv.drem();
                return ValType.Number;
            }
        };
        // 12.7.3 The Addition operator ( + )
        static final AssignmentOp ADD = new AssignmentOp() {
            @Override
            ValType emit(AssignmentExpression node, CodeVisitor mv, ExpressionGenerator gen) {
                LeftHandSideExpression left = node.getLeft();
                Expression right = node.getRight();
                ReferenceOp<LeftHandSideExpression> op = ReferenceOp.of(left);

                ValType ltype = op.referenceForUpdate(left, mv, gen.codegen);
                ValType vtype = op.getValue(left, ltype, mv);

                ValType result;
                if (right instanceof StringLiteral || right instanceof TemplateLiteral) {
                    assert !(right instanceof TemplateLiteral && ((TemplateLiteral) right)
                            .isTagged());
                    // x += "..."
                    toStringForConcat(left, vtype, mv);
                    // lref lval(string)
                    if (!(right instanceof StringLiteral && ((StringLiteral) right).getValue()
                            .isEmpty())) {
                        ValType rtype = right.accept(gen, mv);
                        addStrings(ValType.String, rtype, mv);
                    }
                    result = ValType.String;
                } else {
                    // lref lval
                    ValType rtype = right.accept(gen, mv);
                    mv.toBoxed(rtype);
                    // lref lval rval
                    mv.loadExecutionContext();
                    invokeDynamicOperator(BinaryExpression.Operator.ADD, mv);
                    result = ValType.Any;
                }

                // r lref r
                return op.putValue(left, ltype, result, node.hasCompletion(), mv);
            }
        };
        // 12.7.4 The Subtraction Operator ( - )
        static final CompoundAssignmentOp SUB = new ArithmeticCompoundAssignmentOp() {
            @Override
            ValType operation(CodeVisitor mv) {
                mv.dsub();
                return ValType.Number;
            }
        };
        // 12.8.3 The Left Shift Operator ( << )
        static final CompoundAssignmentOp SHL = new IntegerCompoundAssignmentOp() {
            @Override
            ValType operation(CodeVisitor mv) {
                mv.iconst(0x1F);
                mv.iand();
                mv.ishl();
                return ValType.Number_int;
            }
        };
        // 12.8.4 The Signed Right Shift Operator ( >> )
        static final CompoundAssignmentOp SHR = new IntegerCompoundAssignmentOp() {
            @Override
            ValType operation(CodeVisitor mv) {
                mv.iconst(0x1F);
                mv.iand();
                mv.ishr();
                return ValType.Number_int;
            }
        };
        // 12.8.5 The Unsigned Right Shift Operator ( >>> )
        static final CompoundAssignmentOp USHR = new CompoundAssignmentOp() {
            @Override
            ValType convertLeft(ValType type, CodeVisitor mv) {
                ToUint32(type, mv);
                return ValType.Number_uint;
            }

            @Override
            ValType convertRight(ValType type, CodeVisitor mv) {
                ToInt32(type, mv);
                return ValType.Number_int;
            }

            @Override
            ValType operation(CodeVisitor mv) {
                mv.iconst(0x1F);
                mv.iand();
                mv.lushr();
                return ValType.Number_uint;
            }
        };
        // 12.11 Binary Bitwise Operators ( & )
        static final CompoundAssignmentOp BITAND = new IntegerCompoundAssignmentOp() {
            @Override
            ValType operation(CodeVisitor mv) {
                mv.iand();
                return ValType.Number_int;
            }
        };
        // 12.11 Binary Bitwise Operators ( ^ )
        static final CompoundAssignmentOp BITXOR = new IntegerCompoundAssignmentOp() {
            @Override
            ValType operation(CodeVisitor mv) {
                mv.ixor();
                return ValType.Number_int;
            }
        };
        // 12.11 Binary Bitwise Operators ( | )
        static final CompoundAssignmentOp BITOR = new IntegerCompoundAssignmentOp() {
            @Override
            ValType operation(CodeVisitor mv) {
                mv.ior();
                return ValType.Number_int;
            }
        };
    }

    @Override
    public ValType visit(AsyncArrowFunction node, CodeVisitor mv) {
        MethodName method = codegen.compile(node);

        /* steps 1-4 */
        mv.invoke(method);
        mv.loadExecutionContext();
        mv.invoke(Methods.ScriptRuntime_EvaluateAsyncArrowFunction);

        /* step 5 */
        return ValType.Object;
    }

    /**
     * Extension: Async Function Definitions
     */
    @Override
    public ValType visit(AsyncFunctionExpression node, CodeVisitor mv) {
        MethodName method = codegen.compile(node);

        /* steps 1-5/10 */
        mv.invoke(method);
        mv.loadExecutionContext();
        mv.invoke(Methods.ScriptRuntime_EvaluateAsyncFunctionExpression);

        /* step 6/11 */
        return ValType.Object;
    }

    /**
     * Extension: Async Function Definitions
     */
    @Override
    public ValType visit(AwaitExpression node, CodeVisitor mv) {
        ValType type = node.getExpression().accept(this, mv);
        mv.toBoxed(type);

        await(node, mv);

        return ValType.Any;
    }

    /**
     * 12.6.3 Runtime Semantics: Evaluation<br>
     * 12.7.3.1 Runtime Semantics: Evaluation<br>
     * 12.7.4.1 Runtime Semantics: Evaluation<br>
     * 12.8.3.1 Runtime Semantics: Evaluation<br>
     * 12.8.4.1 Runtime Semantics: Evaluation<br>
     * 12.8.5.1 Runtime Semantics: Evaluation<br>
     * 12.9.3 Runtime Semantics: Evaluation<br>
     * 12.10.3 Runtime Semantics: Evaluation<br>
     * 12.11.3 Runtime Semantics: Evaluation<br>
     * 12.12.3 Runtime Semantics: Evaluation<br>
     */
    @Override
    public ValType visit(BinaryExpression node, CodeVisitor mv) {
        return binaryOp(node).emit(node, mv, this);
    }

    private static BinaryOp binaryOp(BinaryExpression node) {
        switch (node.getOperator()) {
        case ADD:
            return BinaryOp.ADD;
        case AND:
            return BinaryOp.AND;
        case BITAND:
            return BinaryOp.BITAND;
        case BITOR:
            return BinaryOp.BITOR;
        case BITXOR:
            return BinaryOp.BITXOR;
        case DIV:
            return BinaryOp.DIV;
        case EQ:
            return BinaryOp.EQ;
        case EXP:
            return BinaryOp.EXP;
        case GE:
            return BinaryOp.GE;
        case GT:
            return BinaryOp.GT;
        case IN:
            return BinaryOp.IN;
        case INSTANCEOF:
            return BinaryOp.INSTANCEOF;
        case LE:
            return BinaryOp.LE;
        case LT:
            return BinaryOp.LT;
        case MOD:
            return BinaryOp.MOD;
        case MUL:
            return BinaryOp.MUL;
        case NE:
            return BinaryOp.NE;
        case OR:
            return BinaryOp.OR;
        case SHEQ:
            return BinaryOp.SHEQ;
        case SHL:
            return BinaryOp.SHL;
        case SHNE:
            return BinaryOp.SHNE;
        case SHR:
            return BinaryOp.SHR;
        case SUB:
            return BinaryOp.SUB;
        case USHR:
            return BinaryOp.USHR;
        default:
            throw new AssertionError(Objects.toString(node.getOperator(), "<null>"));
        }
    }

    private static abstract class BinaryOp {
        abstract ValType emit(BinaryExpression node, CodeVisitor mv, ExpressionGenerator gen);

        static abstract class ConvertOp extends BinaryOp {
            abstract ValType operation(CodeVisitor mv);

            abstract ValType convertLeft(ValType type, CodeVisitor mv);

            abstract ValType convertRight(ValType type, CodeVisitor mv);

            @Override
            final ValType emit(BinaryExpression node, CodeVisitor mv, ExpressionGenerator gen) {
                Expression left = node.getLeft();
                Expression right = node.getRight();

                ValType ltype = left.accept(gen, mv);
                if (ltype.isPrimitive() || right instanceof Literal) {
                    ltype = convertLeft(ltype, mv);
                }
                ValType rtype = right.accept(gen, mv);
                if (!(ltype.isPrimitive() || right instanceof Literal)) {
                    mv.swap(ltype, rtype);
                    ltype = convertLeft(ltype, mv);
                    mv.swap(rtype, ltype);
                }
                convertRight(rtype, mv);
                return operation(mv);
            }
        }

        static abstract class ArithmeticOp extends ConvertOp {
            @Override
            final ValType convertLeft(ValType type, CodeVisitor mv) {
                ToNumber(type, mv);
                return ValType.Number;
            }

            @Override
            final ValType convertRight(ValType type, CodeVisitor mv) {
                ToNumber(type, mv);
                return ValType.Number;
            }
        }

        static abstract class IntegerOp extends ConvertOp {
            @Override
            final ValType convertLeft(ValType type, CodeVisitor mv) {
                ToInt32(type, mv);
                return ValType.Number_int;
            }

            @Override
            final ValType convertRight(ValType type, CodeVisitor mv) {
                ToInt32(type, mv);
                return ValType.Number_int;
            }
        }

        static abstract class RelationalOp extends BinaryOp {
            abstract void operation(CodeVisitor mv);

            @Override
            final ValType emit(BinaryExpression node, CodeVisitor mv, ExpressionGenerator gen) {
                mv.toBoxed(node.getLeft().accept(gen, mv));
                mv.toBoxed(node.getRight().accept(gen, mv));
                mv.lineInfo(node);
                operation(mv);
                return ValType.Boolean;
            }
        }

        static final class EqMethods {
            // class: AbstractOperations
            static final MethodName AbstractOperations_IsCallable = MethodName.findStatic(Types.AbstractOperations,
                    "IsCallable", Type.methodType(Type.BOOLEAN_TYPE, Types.Object));

            // class: Type
            static final MethodName Type_isBoolean = MethodName.findStatic(Types._Type, "isBoolean",
                    Type.methodType(Type.BOOLEAN_TYPE, Types.Object));

            static final MethodName Type_isNull = MethodName.findStatic(Types._Type, "isNull",
                    Type.methodType(Type.BOOLEAN_TYPE, Types.Object));

            static final MethodName Type_isNumber = MethodName.findStatic(Types._Type, "isNumber",
                    Type.methodType(Type.BOOLEAN_TYPE, Types.Object));

            static final MethodName Type_isString = MethodName.findStatic(Types._Type, "isString",
                    Type.methodType(Type.BOOLEAN_TYPE, Types.Object));

            static final MethodName Type_isSymbol = MethodName.findStatic(Types._Type, "isSymbol",
                    Type.methodType(Type.BOOLEAN_TYPE, Types.Object));

            static final MethodName Type_isUndefinedOrNull = MethodName.findStatic(Types._Type, "isUndefinedOrNull",
                    Type.methodType(Type.BOOLEAN_TYPE, Types.Object));

            static final MethodName Type_isUndefined = MethodName.findStatic(Types._Type, "isUndefined",
                    Type.methodType(Type.BOOLEAN_TYPE, Types.Object));

            // class: Reference
            static final MethodName Reference_isUnresolvableReference = MethodName.findVirtual(Types.Reference,
                    "isUnresolvableReference", Type.methodType(Type.BOOLEAN_TYPE));

            // class: ScriptRuntime
            static final MethodName ScriptRuntime_compare = MethodName.findStatic(Types.ScriptRuntime, "compare",
                    Type.methodType(Type.BOOLEAN_TYPE, Types.CharSequence, Types.CharSequence));

            static final MethodName ScriptRuntime_isNonCallableObjectOrNull = MethodName.findStatic(Types.ScriptRuntime,
                    "isNonCallableObjectOrNull", Type.methodType(Type.BOOLEAN_TYPE, Types.Object));

            static final MethodName ScriptRuntime_isSIMDType = MethodName.findStatic(Types.ScriptRuntime, "isSIMDType",
                    Type.methodType(Type.BOOLEAN_TYPE, Types.Object, Types.SIMDType));
        }

        static abstract class EqualityOp extends BinaryOp {
            @Override
            final ValType emit(BinaryExpression node, CodeVisitor mv, ExpressionGenerator gen) {
                Expression left = node.getLeft();
                Expression right = node.getRight();
                if (left instanceof Literal) {
                    emitLiteral(node, (Literal) left, right, mv, gen);
                } else if (right instanceof Literal) {
                    emitLiteral(node, (Literal) right, left, mv, gen);
                } else {
                    ValType ltype = left.accept(gen, mv);
                    if (ltype.isPrimitive()) {
                        emitPrimitive(node, ltype, right, mv, gen);
                    } else {
                        emitGeneric(node, ltype, right, mv, gen);
                    }
                }
                return ValType.Boolean;
            }

            protected abstract void emitGeneric(BinaryExpression node, ValType ltype, Expression right, CodeVisitor mv,
                    ExpressionGenerator gen);

            protected abstract void emitLiteral(BinaryExpression node, ValType type, NullLiteral literal,
                    CodeVisitor mv, ExpressionGenerator gen);

            protected abstract void emitLiteral(BinaryExpression node, ValType type, BooleanLiteral literal,
                    CodeVisitor mv, ExpressionGenerator gen);

            protected abstract void emitLiteral(BinaryExpression node, ValType type, NumericLiteral literal,
                    CodeVisitor mv, ExpressionGenerator gen);

            protected abstract void emitLiteral(BinaryExpression node, ValType type, StringLiteral literal,
                    CodeVisitor mv, ExpressionGenerator gen);

            private void emitLiteral(BinaryExpression node, Literal literal, Expression expr, CodeVisitor mv,
                    ExpressionGenerator gen) {
                if (literal instanceof NullLiteral) {
                    ValType type = expr.accept(gen, mv);
                    emitLiteral(node, type, (NullLiteral) literal, mv, gen);
                } else if (literal instanceof BooleanLiteral) {
                    BooleanLiteral bool = (BooleanLiteral) literal;
                    ValType type = expr.accept(gen, mv);
                    if (type == ValType.Boolean) {
                        if (bool.getValue() == false) {
                            mv.not();
                        }
                    } else {
                        emitLiteral(node, type, bool, mv, gen);
                    }
                } else if (literal instanceof NumericLiteral) {
                    NumericLiteral numeric = (NumericLiteral) literal;
                    ValType type = expr.accept(gen, mv);
                    if (type.isNumeric()) {
                        if (type == ValType.Number_int && numeric.isInt()) {
                            if (numeric.intValue() == 0) {
                                compareInt0(mv);
                            } else {
                                mv.iconst(numeric.intValue());
                                compareInt(mv);
                            }
                        } else if (type == ValType.Number_uint && numeric.isInt()) {
                            mv.lconst(numeric.intValue());
                            compareLong(mv);
                        } else {
                            ToNumber(type, mv);
                            mv.dconst(numeric.doubleValue());
                            compareDouble(mv);
                        }
                    } else {
                        emitLiteral(node, type, numeric, mv, gen);
                    }
                } else {
                    assert literal instanceof StringLiteral;
                    StringLiteral string = (StringLiteral) literal;
                    if (isTypeof(expr) && isTypeString(string.getValue())) {
                        emitTypeCheck(string.getValue(), ((UnaryExpression) expr).getOperand(), mv, gen);
                    } else {
                        ValType type = expr.accept(gen, mv);
                        if (type == ValType.String) {
                            mv.aconst(string.getValue());
                            compareString(mv);
                        } else {
                            emitLiteral(node, type, string, mv, gen);
                        }
                    }
                }
            }

            private void emitPrimitive(BinaryExpression node, ValType ltype, Expression right, CodeVisitor mv,
                    ExpressionGenerator gen) {
                ValType expected = expressionType(right);
                if (ltype == expected) {
                    ValType rtype = right.accept(gen, mv);
                    assert rtype == expected;
                    switch (ltype) {
                    case Boolean:
                    case Number_int:
                        compareInt(mv);
                        break;
                    case Number_uint:
                        compareLong(mv);
                        break;
                    case Number:
                        compareDouble(mv);
                        break;
                    case String:
                        compareString(mv);
                        break;
                    case Undefined:
                    case Null:
                        compareReference(mv);
                        break;
                    default:
                        throw new AssertionError();
                    }
                } else if (ltype.isNumeric() && expected.isNumeric()) {
                    ToNumber(ltype, mv);
                    ValType rtype = right.accept(gen, mv);
                    assert rtype == expected;
                    ToNumber(rtype, mv);
                    compareDouble(mv);
                } else {
                    emitGeneric(node, ltype, right, mv, gen);
                }
            }

            private void compareString(CodeVisitor mv) {
                mv.invoke(EqMethods.ScriptRuntime_compare);
            }

            private void compareReference(CodeVisitor mv) {
                Jump notSame = new Jump(), end = new Jump();
                mv.ifacmpne(notSame);
                compareTail(end, notSame, mv);
            }

            private void compareInt0(CodeVisitor mv) {
                Jump notSame = new Jump(), end = new Jump();
                mv.ifne(notSame);
                compareTail(end, notSame, mv);
            }

            private void compareInt(CodeVisitor mv) {
                Jump notSame = new Jump(), end = new Jump();
                mv.ificmpne(notSame);
                compareTail(end, notSame, mv);
            }

            private void compareLong(CodeVisitor mv) {
                Jump notSame = new Jump(), end = new Jump();
                mv.lcmp();
                mv.ifne(notSame);
                compareTail(end, notSame, mv);
            }

            private void compareDouble(CodeVisitor mv) {
                Jump notSame = new Jump(), end = new Jump();
                mv.dcmpl();
                mv.ifne(notSame);
                compareTail(end, notSame, mv);
            }

            private void compareTail(Jump end, Jump notSame, CodeVisitor mv) {
                mv.iconst(true);
                mv.goTo(end);
                mv.mark(notSame);
                mv.iconst(false);
                mv.mark(end);
            }

            private void emitTypeCheck(String name, Expression operand, CodeVisitor mv, ExpressionGenerator gen) {
                if (operand instanceof IdentifierReference) {
                    IdentifierReference ident = (IdentifierReference) operand;
                    Name resolvedName = ident.getResolvedName();
                    if (resolvedName == null || !resolvedName.isLocal()) {
                        // stack: [] -> [ref, ref]
                        ValType reference = ReferenceOp.LOOKUP.reference(ident, mv, gen.codegen);
                        mv.dup(reference);

                        // stack: [ref, ref] -> [ref, unresolvable]
                        Jump unresolvable = new Jump(), end = new Jump();
                        mv.invoke(EqMethods.Reference_isUnresolvableReference);
                        mv.ifeq(unresolvable);
                        {
                            mv.pop(reference);
                            mv.iconst("undefined".equals(name));
                            mv.goTo(end);
                        }
                        mv.mark(unresolvable);
                        // stack: [ref] -> [val]
                        ReferenceOp.LOOKUP.getValue(ident, reference, mv);
                        emitTypeCheckGeneric(name, mv);
                        mv.mark(end);
                        return;
                    }
                }
                ValType type = operand.accept(gen, mv);
                if (type != ValType.Any) {
                    emitTypeCheckConst(name, type, mv);
                } else {
                    emitTypeCheckGeneric(name, mv);
                }
            }

            private void emitTypeCheckConst(String name, ValType type, CodeVisitor mv) {
                assert type != ValType.Any;
                boolean result;
                switch (name) {
                case "undefined":
                    result = type == ValType.Undefined;
                    break;
                case "object":
                    if (type == ValType.Object) {
                        emitTypeCheckGeneric(name, mv);
                        return;
                    }
                    result = type == ValType.Null;
                    break;
                case "boolean":
                    result = type == ValType.Boolean;
                    break;
                case "number":
                    result = type.isNumeric();
                    break;
                case "string":
                    result = type == ValType.String;
                    break;
                case "function": {
                    if (type == ValType.Object) {
                        emitTypeCheckGeneric(name, mv);
                        return;
                    }
                    result = false;
                    break;
                }
                default:
                    result = false;
                    break;
                }
                mv.pop(type);
                mv.iconst(result);
            }

            private void emitTypeCheckGeneric(String name, CodeVisitor mv) {
                MethodName typeCheck = typeCheck(name);
                if (typeCheck == EqMethods.ScriptRuntime_isSIMDType) {
                    mv.getstatic(Types.SIMDType, SIMDType.from(name).name(), Types.SIMDType);
                }
                mv.invoke(typeCheck);
            }

            private MethodName typeCheck(String name) {
                switch (name) {
                case "undefined":
                    return EqMethods.Type_isUndefined;
                case "object":
                    return EqMethods.ScriptRuntime_isNonCallableObjectOrNull;
                case "boolean":
                    return EqMethods.Type_isBoolean;
                case "number":
                    return EqMethods.Type_isNumber;
                case "string":
                    return EqMethods.Type_isString;
                case "symbol":
                    return EqMethods.Type_isSymbol;
                case "function":
                    return EqMethods.AbstractOperations_IsCallable;
                default:
                    return EqMethods.ScriptRuntime_isSIMDType;
                }
            }

            private boolean isTypeof(Expression expr) {
                if (!(expr instanceof UnaryExpression)) {
                    return false;
                }
                return ((UnaryExpression) expr).getOperator() == UnaryExpression.Operator.TYPEOF;
            }

            private boolean isTypeString(String s) {
                switch (s) {
                case "undefined":
                case "object":
                case "boolean":
                case "number":
                case "string":
                case "symbol":
                case "function":
                    return true;
                case "float64x2":
                case "float32x4":
                case "int32x4":
                case "int16x8":
                case "int8x16":
                case "uint32x4":
                case "uint16x8":
                case "uint8x16":
                case "bool64x2":
                case "bool32x4":
                case "bool16x8":
                case "bool8x16":
                    return true;
                default:
                    return false;
                }
            }
        }

        static abstract class LogicalOp extends BinaryOp {
            abstract void operation(Jump jump, CodeVisitor mv);

            @Override
            final ValType emit(BinaryExpression node, CodeVisitor mv, ExpressionGenerator gen) {
                Expression left = node.getLeft();
                Expression right = node.getRight();
                Jump after = new Jump();

                ValType ltype = left.accept(gen, mv);
                if (!node.hasCompletion()) {
                    ToBoolean(ltype, mv);
                    operation(after, mv);
                    ValType rtype = right.emptyCompletion().accept(gen, mv);
                    mv.pop(rtype);
                    mv.mark(after);
                    return ValType.Empty;
                }
                if (ltype == ValType.Boolean && expressionType(right) == ValType.Boolean) {
                    mv.dup();
                    operation(after, mv);
                    mv.pop();
                    ValType rtype = right.accept(gen, mv);
                    assert rtype == ValType.Boolean;
                    mv.mark(after);
                    return ValType.Boolean;
                }
                mv.toBoxed(ltype);
                mv.dup();
                ToBoolean(ValType.Any, mv);
                operation(after, mv);
                mv.pop();
                ValType rtype = right.accept(gen, mv);
                mv.toBoxed(rtype);
                mv.mark(after);
                return ValType.Any;
            }
        }

        // Extension: Exponentiation Operator
        static final ArithmeticOp EXP = new ArithmeticOp() {
            @Override
            ValType operation(CodeVisitor mv) {
                mv.invoke(Methods.Math_pow);
                return ValType.Number;
            }
        };
        // 12.6 Multiplicative Operators
        static final ArithmeticOp MUL = new ArithmeticOp() {
            @Override
            ValType operation(CodeVisitor mv) {
                mv.dmul();
                return ValType.Number;
            }
        };
        // 12.6 Multiplicative Operators
        static final ArithmeticOp DIV = new ArithmeticOp() {
            @Override
            ValType operation(CodeVisitor mv) {
                mv.ddiv();
                return ValType.Number;
            }
        };
        // 12.6 Multiplicative Operators
        static final ArithmeticOp MOD = new ArithmeticOp() {
            @Override
            ValType operation(CodeVisitor mv) {
                mv.drem();
                return ValType.Number;
            }
        };
        // 12.7.3 The Addition operator ( + )
        static final BinaryOp ADD = new BinaryOp() {
            @Override
            ValType emit(BinaryExpression node, CodeVisitor mv, ExpressionGenerator gen) {
                Expression left = node.getLeft();
                Expression right = node.getRight();

                // Handle 'a' + b + c + ...
                if (left instanceof BinaryExpression && isStringConcat((BinaryExpression) left)
                        && stringConcat(node, mv, gen)) {
                    return ValType.String;
                }
                // Handle 'a' + b
                if (left instanceof StringLiteral) {
                    String leftValue = ((StringLiteral) left).getValue();
                    if (right instanceof StringLiteral) {
                        // 'a' + 'b'
                        mv.aconst(leftValue + ((StringLiteral) right).getValue());
                        return ValType.String;
                    }
                    if (leftValue.isEmpty()) {
                        // "" + x
                        return evalToString(right, mv, gen);
                    }
                    // "..." + x
                    ValType ltype = left.accept(gen, mv);
                    ValType rtype = evalToString(right, mv, gen);
                    return addStrings(ltype, rtype, mv);
                } else if (right instanceof StringLiteral) {
                    if (((StringLiteral) right).getValue().isEmpty()) {
                        // x + ""
                        return evalToString(left, mv, gen);
                    }
                    // x + "..."
                    ValType ltype = evalToString(left, mv, gen);
                    ValType rtype = right.accept(gen, mv);
                    return addStrings(ltype, rtype, mv);
                } else if (left instanceof TemplateLiteral) {
                    // `...` + x
                    assert !((TemplateLiteral) left).isTagged();
                    ValType ltype = left.accept(gen, mv);
                    ValType rtype = evalToString(right, mv, gen);
                    return addStrings(ltype, rtype, mv);
                } else if (right instanceof TemplateLiteral) {
                    // x + `...`
                    assert !((TemplateLiteral) right).isTagged();
                    ValType ltype = left.accept(gen, mv);
                    if (ltype.isPrimitive()) {
                        ToString(ltype, mv);
                    }
                    ValType rtype = right.accept(gen, mv);
                    if (!ltype.isPrimitive()) {
                        mv.swap(ltype, rtype);
                        toStringForConcat(left, ltype, mv);
                        mv.swap(rtype, ValType.String);
                    }
                    return addStrings(ValType.String, rtype, mv);
                }

                ValType ltype = left.accept(gen, mv);
                if (ltype == ValType.String) {
                    ValType rtype = evalToString(right, mv, gen);
                    return addStrings(ltype, rtype, mv);
                }
                if (ltype.isNumeric()) {
                    ValType expected = expressionType(right);
                    if (expected.isPrimitive() && expected != ValType.String) {
                        ToNumber(ltype, mv);
                        ValType rtype = right.accept(gen, mv);
                        assert rtype.isPrimitive() && rtype != ValType.String : String.format(
                                "expected=%s, actual=%s", expected, rtype);
                        ToNumber(rtype, mv);
                        mv.dadd();
                        return ValType.Number;
                    }
                }
                if (right instanceof BinaryExpression && isStringConcat((BinaryExpression) right)) {
                    if (ltype.isPrimitive()) {
                        ToString(ltype, mv);
                    }
                    ValType rtype = right.accept(gen, mv);
                    assert rtype == ValType.String;
                    if (!ltype.isPrimitive()) {
                        mv.swap(ltype, rtype);
                        toStringForConcat(left, ltype, mv);
                        mv.swap(rtype, ValType.String);
                    }
                    return addStrings(ValType.String, rtype, mv);
                }
                mv.toBoxed(ltype);
                ValType rtype = right.accept(gen, mv);
                if (rtype == ValType.String) {
                    mv.swap(ValType.Any, rtype);
                    if (ltype.isPrimitive()) {
                        mv.toUnboxed(ltype);
                    }
                    toStringForConcat(left, ltype, mv);
                    mv.swap(rtype, ValType.String);
                    return addStrings(ValType.String, rtype, mv);
                }
                mv.toBoxed(rtype);

                mv.loadExecutionContext();
                invokeDynamicOperator(node.getOperator(), mv);

                return ValType.Any;
            }

            private boolean isStringConcat(BinaryExpression binary) {
                for (;;) {
                    if (binary.getOperator() != BinaryExpression.Operator.ADD) {
                        return false;
                    }
                    Expression left = binary.getLeft();
                    if (isString(left) || isString(binary.getRight())) {
                        return true;
                    }
                    if (!(left instanceof BinaryExpression)) {
                        return false;
                    }
                    binary = (BinaryExpression) left;
                }
            }

            private boolean isString(Expression node) {
                return node instanceof StringLiteral || node instanceof TemplateLiteral;
            }

            private boolean stringConcat(BinaryExpression binary, CodeVisitor mv, ExpressionGenerator gen) {
                if (isConstantStringConcat(binary)) {
                    StringBuilder sb = new StringBuilder();
                    stringConcat(binary, sb);
                    mv.aconst(sb.toString());
                    return true;
                }
                int strings = countStringConcats(binary);
                if (strings > MAX_DYN_ARGUMENTS) {
                    return false;
                }
                if (strings == 0) {
                    mv.aconst("");
                } else if (strings == 1) {
                    stringConcatWith(binary, mv, gen);
                } else {
                    mv.loadExecutionContext();
                    stringConcatWith(binary, mv, gen);
                    invokeDynamicConcat(strings, mv);
                }
                return true;
            }

            private boolean isConstantStringConcat(BinaryExpression node) {
                for (;;) {
                    if (node.getOperator() != BinaryExpression.Operator.ADD) {
                        return false;
                    }
                    Expression left = node.getLeft();
                    Expression right = node.getRight();
                    if (left instanceof StringLiteral && right instanceof StringLiteral) {
                        return true;
                    } else if (left instanceof BinaryExpression && right instanceof StringLiteral) {
                        node = (BinaryExpression) left;
                    } else if (left instanceof StringLiteral && right instanceof BinaryExpression) {
                        node = (BinaryExpression) right;
                    } else if (left instanceof BinaryExpression
                            && right instanceof BinaryExpression
                            && isConstantStringConcat((BinaryExpression) right)) {
                        node = (BinaryExpression) left;
                    } else {
                        return false;
                    }
                }
            }

            private void stringConcat(Expression node, StringBuilder sb) {
                if (node instanceof StringLiteral) {
                    sb.append(((StringLiteral) node).getValue());
                } else {
                    assert node instanceof BinaryExpression;
                    stringConcat(((BinaryExpression) node).getLeft(), sb);
                    stringConcat(((BinaryExpression) node).getRight(), sb);
                }
            }

            private ValType stringConcatWith(BinaryExpression node, CodeVisitor mv, ExpressionGenerator gen) {
                ArrayList<Expression> list = new ArrayList<>();
                for (;;) {
                    Expression left = node.getLeft();
                    Expression right = node.getRight();
                    if (isString(left) || isString(right)) {
                        ValType ltype = stringConcatWith(left, mv, gen);
                        if (ltype.isPrimitive() || right instanceof Literal) {
                            toStringForConcat(left, ltype, mv);
                        }
                        ValType rtype = stringConcatWith(right, mv, gen);
                        assert ltype == ValType.String || rtype == ValType.String;
                        if (!(ltype.isPrimitive() || right instanceof Literal)) {
                            mv.swap(ltype, rtype);
                            toStringForConcat(left, ltype, mv);
                            mv.swap(rtype, ValType.String);
                        }
                        toStringForConcat(right, rtype, mv);
                        break;
                    }
                    node = (BinaryExpression) left;
                    list.add(right);
                }
                for (int i = list.size() - 1; i >= 0; --i) {
                    Expression e = list.get(i);
                    ValType rtype = stringConcatWith(e, mv, gen);
                    toStringForConcat(e, rtype, mv);
                }
                return ValType.String;
            }

            private ValType stringConcatWith(Expression node, CodeVisitor mv, ExpressionGenerator gen) {
                if (node instanceof StringLiteral) {
                    if (!((StringLiteral) node).getValue().isEmpty()) {
                        node.accept(gen, mv);
                    }
                    return ValType.String;
                }
                if (node instanceof TemplateLiteral) {
                    node.accept(gen, mv);
                    return ValType.String;
                }
                if (node instanceof BinaryExpression && isStringConcat((BinaryExpression) node)) {
                    return stringConcatWith((BinaryExpression) node, mv, gen);
                }
                return node.accept(gen, mv);
            }

            private int countStringConcats(BinaryExpression node) {
                int c = 0;
                for (BinaryExpression binary = (BinaryExpression) node;;) {
                    Expression left = binary.getLeft();
                    Expression right = binary.getRight();
                    if (isString(left) || isString(right)) {
                        return c + countStringConcats(left) + countStringConcats(right);
                    }
                    binary = (BinaryExpression) left;
                    c += countStringConcats(right);
                }
            }

            private int countStringConcats(Expression node) {
                if (node instanceof BinaryExpression && isStringConcat((BinaryExpression) node)) {
                    return countStringConcats((BinaryExpression) node);
                }
                // Empty string literals are omitted.
                if (node instanceof StringLiteral && ((StringLiteral) node).getValue().isEmpty()) {
                    return 0;
                }
                return 1;
            }

            private ValType evalToString(Expression node, CodeVisitor mv, ExpressionGenerator gen) {
                ValType type = node.accept(gen, mv);
                return toStringForConcat(node, type, mv);
            }
        };
        // 12.7.4 The Subtraction Operator ( - )
        static final ArithmeticOp SUB = new ArithmeticOp() {
            @Override
            ValType operation(CodeVisitor mv) {
                mv.dsub();
                return ValType.Number;
            }
        };
        // 12.8.3 The Left Shift Operator ( << )
        static final IntegerOp SHL = new IntegerOp() {
            @Override
            ValType operation(CodeVisitor mv) {
                mv.iconst(0x1F);
                mv.iand();
                mv.ishl();
                return ValType.Number_int;
            }
        };
        // 12.8.4 The Signed Right Shift Operator ( >> )
        static final IntegerOp SHR = new IntegerOp() {
            @Override
            ValType operation(CodeVisitor mv) {
                mv.iconst(0x1F);
                mv.iand();
                mv.ishr();
                return ValType.Number_int;
            }
        };
        // 12.8.5 The Unsigned Right Shift Operator ( >>> )
        static final ConvertOp USHR = new ConvertOp() {
            @Override
            ValType convertLeft(ValType type, CodeVisitor mv) {
                ToUint32(type, mv);
                return ValType.Number_uint;
            }

            @Override
            ValType convertRight(ValType type, CodeVisitor mv) {
                ToInt32(type, mv); // ToUint32()
                return ValType.Number_int;
            }

            @Override
            ValType operation(CodeVisitor mv) {
                mv.iconst(0x1F);
                mv.iand();
                mv.lushr();
                return ValType.Number_uint;
            }
        };
        // 12.9 Relational Operators ( < )
        static final RelationalOp LT = new RelationalOp() {
            @Override
            void operation(CodeVisitor mv) {
                mv.loadExecutionContext();
                invokeDynamicOperator(BinaryExpression.Operator.LT, mv);
            }
        };
        // 12.9 Relational Operators ( > )
        static final RelationalOp GT = new RelationalOp() {
            @Override
            void operation(CodeVisitor mv) {
                mv.swap();
                mv.loadExecutionContext();
                invokeDynamicOperator(BinaryExpression.Operator.GT, mv);
            }
        };
        // 12.9 Relational Operators ( <= )
        static final RelationalOp LE = new RelationalOp() {
            @Override
            void operation(CodeVisitor mv) {
                mv.swap();
                mv.loadExecutionContext();
                invokeDynamicOperator(BinaryExpression.Operator.LE, mv);
            }
        };
        // 12.9 Relational Operators ( >= )
        static final RelationalOp GE = new RelationalOp() {
            @Override
            void operation(CodeVisitor mv) {
                mv.loadExecutionContext();
                invokeDynamicOperator(BinaryExpression.Operator.GE, mv);
            }
        };
        // 12.9 Relational Operators ( instanceof )
        static final RelationalOp INSTANCEOF = new RelationalOp() {
            @Override
            void operation(CodeVisitor mv) {
                mv.loadExecutionContext();
                mv.invoke(Methods.ScriptRuntime_InstanceofOperator);
            }
        };
        // 12.9 Relational Operators ( in )
        static final RelationalOp IN = new RelationalOp() {
            @Override
            void operation(CodeVisitor mv) {
                mv.loadExecutionContext();
                mv.invoke(Methods.ScriptRuntime_in);
            }
        };
        // 12.10 Equality Operators ( == )
        static final EqualityOp EQ = new EqualityOp() {
            @Override
            protected void emitGeneric(BinaryExpression node, ValType ltype, Expression right, CodeVisitor mv,
                    ExpressionGenerator gen) {
                mv.toBoxed(ltype);
                mv.toBoxed(right.accept(gen, mv));
                mv.lineInfo(node);
                mv.loadExecutionContext();
                invokeDynamicOperator(BinaryExpression.Operator.EQ, mv);
            }

            @Override
            protected void emitLiteral(BinaryExpression node, ValType type, NullLiteral literal, CodeVisitor mv,
                    ExpressionGenerator gen) {
                if (type == ValType.Any) {
                    mv.invoke(EqMethods.Type_isUndefinedOrNull);
                } else {
                    mv.pop(type);
                    mv.iconst(type == ValType.Undefined || type == ValType.Null);
                }
            }

            @Override
            protected void emitLiteral(BinaryExpression node, ValType type, BooleanLiteral literal, CodeVisitor mv,
                    ExpressionGenerator gen) {
                assert type != ValType.Boolean;
                emitGeneric(node, type, literal, mv, gen);
            }

            @Override
            protected void emitLiteral(BinaryExpression node, ValType type, NumericLiteral literal, CodeVisitor mv,
                    ExpressionGenerator gen) {
                assert !type.isNumeric();
                emitGeneric(node, type, literal, mv, gen);
            }

            @Override
            protected void emitLiteral(BinaryExpression node, ValType type, StringLiteral literal, CodeVisitor mv,
                    ExpressionGenerator gen) {
                assert type != ValType.String;
                emitGeneric(node, type, literal, mv, gen);
            }
        };
        // 12.10 Equality Operators ( != )
        static final BinaryOp NE = new BinaryOp() {
            @Override
            ValType emit(BinaryExpression node, CodeVisitor mv, ExpressionGenerator gen) {
                BinaryOp.EQ.emit(node, mv, gen);
                mv.not();
                return ValType.Boolean;
            }
        };
        // 12.10 Equality Operators ( === )
        static final EqualityOp SHEQ = new EqualityOp() {
            @Override
            protected void emitGeneric(BinaryExpression node, ValType ltype, Expression right, CodeVisitor mv,
                    ExpressionGenerator gen) {
                mv.toBoxed(ltype);
                mv.toBoxed(right.accept(gen, mv));
                mv.lineInfo(node);
                invokeDynamicOperator(BinaryExpression.Operator.SHEQ, mv);
            }

            @Override
            protected void emitLiteral(BinaryExpression node, ValType type, NullLiteral literal, CodeVisitor mv,
                    ExpressionGenerator gen) {
                if (type == ValType.Any) {
                    mv.invoke(EqMethods.Type_isNull);
                } else {
                    mv.pop(type);
                    mv.iconst(type == ValType.Null);
                }
            }

            @Override
            protected void emitLiteral(BinaryExpression node, ValType type, BooleanLiteral literal, CodeVisitor mv,
                    ExpressionGenerator gen) {
                assert type != ValType.Boolean;
                if (type == ValType.Any) {
                    emitGeneric(node, type, literal, mv, gen);
                } else {
                    mv.pop(type);
                    mv.iconst(false);
                }
            }

            @Override
            protected void emitLiteral(BinaryExpression node, ValType type, NumericLiteral literal, CodeVisitor mv,
                    ExpressionGenerator gen) {
                assert !type.isNumeric();
                if (type == ValType.Any) {
                    emitGeneric(node, type, literal, mv, gen);
                } else {
                    mv.pop(type);
                    mv.iconst(false);
                }
            }

            @Override
            protected void emitLiteral(BinaryExpression node, ValType type, StringLiteral literal, CodeVisitor mv,
                    ExpressionGenerator gen) {
                assert type != ValType.String;
                if (type == ValType.Any) {
                    emitGeneric(node, type, literal, mv, gen);
                } else {
                    mv.pop(type);
                    mv.iconst(false);
                }
            }
        };
        // 12.10 Equality Operators ( !== )
        static final BinaryOp SHNE = new BinaryOp() {
            @Override
            ValType emit(BinaryExpression node, CodeVisitor mv, ExpressionGenerator gen) {
                BinaryOp.SHEQ.emit(node, mv, gen);
                mv.not();
                return ValType.Boolean;
            }
        };
        // 12.11 Binary Bitwise Operators ( & )
        static final IntegerOp BITAND = new IntegerOp() {
            @Override
            ValType operation(CodeVisitor mv) {
                mv.iand();
                return ValType.Number_int;
            }
        };
        // 12.11 Binary Bitwise Operators ( ^ )
        static final IntegerOp BITXOR = new IntegerOp() {
            @Override
            ValType operation(CodeVisitor mv) {
                mv.ixor();
                return ValType.Number_int;
            }
        };
        // 12.11 Binary Bitwise Operators ( | )
        static final IntegerOp BITOR = new IntegerOp() {
            @Override
            ValType operation(CodeVisitor mv) {
                mv.ior();
                return ValType.Number_int;
            }
        };
        // 12.12 Binary Logical Operators
        static final LogicalOp AND = new LogicalOp() {
            @Override
            void operation(Jump jump, CodeVisitor mv) {
                mv.ifeq(jump);
            }
        };
        // 12.12 Binary Logical Operators
        static final LogicalOp OR = new LogicalOp() {
            @Override
            void operation(Jump jump, CodeVisitor mv) {
                mv.ifne(jump);
            }
        };
    }

    private static ValType toStringForConcat(Expression node, ValType type, CodeVisitor mv) {
        // ToString(ToPrimitive(type, mv), mv);
        if (type.isPrimitive()) {
            ToString(type, mv);
        } else {
            mv.loadExecutionContext();
            mv.lineInfo(node);
            mv.invoke(Methods.ScriptRuntime_toStr);
        }
        return ValType.String;
    }

    private static ValType addStrings(ValType left, ValType right, CodeVisitor mv) {
        assert left == ValType.String && right == ValType.String;
        mv.loadExecutionContext();
        mv.invoke(Methods.ScriptRuntime_add_str);
        return ValType.String;
    }

    private static ValType expressionType(Expression node) {
        return node.accept(ExpressionTypeVisitor.INSTANCE, null);
    }

    private static final class ExpressionTypeVisitor extends DefaultNodeVisitor<ValType, Void> {
        static final ExpressionTypeVisitor INSTANCE = new ExpressionTypeVisitor();

        @Override
        protected ValType visit(Node node, Void value) {
            return ValType.Any;
        }

        @Override
        public ValType visit(ArrayComprehension node, Void value) {
            return ValType.Object;
        }

        @Override
        public ValType visit(ArrayLiteral node, Void value) {
            return ValType.Object;
        }

        @Override
        public ValType visit(ArrowFunction node, Void value) {
            return ValType.Object;
        }

        @Override
        public ValType visit(AssignmentExpression node, Void value) {
            switch (node.getOperator()) {
            case ASSIGN_BITAND:
            case ASSIGN_BITOR:
            case ASSIGN_BITXOR:
            case ASSIGN_SHL:
            case ASSIGN_SHR:
                return ValType.Number_int;
            case ASSIGN_USHR:
                return ValType.Number_uint;
            case ASSIGN_DIV:
            case ASSIGN_EXP:
            case ASSIGN_MOD:
            case ASSIGN_MUL:
            case ASSIGN_SUB:
                return ValType.Number;
            case ASSIGN_ADD: {
                ValType rtype = expressionType(node.getRight());
                if (rtype == ValType.String) {
                    return ValType.String;
                }
                // Pessimistically assume any-type
                return ValType.Any;
            }
            case ASSIGN:
                return expressionType(node.getRight());
            default:
                throw new AssertionError();
            }
        }

        @Override
        public ValType visit(AsyncArrowFunction node, Void value) {
            return ValType.Object;
        }

        @Override
        public ValType visit(AsyncFunctionExpression node, Void value) {
            return ValType.Object;
        }

        @Override
        public ValType visit(BinaryExpression node, Void value) {
            switch (node.getOperator()) {
            case BITAND:
            case BITOR:
            case BITXOR:
            case SHL:
            case SHR:
                return ValType.Number_int;
            case USHR:
                return ValType.Number_uint;
            case DIV:
            case EXP:
            case MOD:
            case MUL:
            case SUB:
                return ValType.Number;
            case EQ:
            case GE:
            case GT:
            case IN:
            case INSTANCEOF:
            case LE:
            case LT:
            case NE:
            case SHEQ:
            case SHNE:
                return ValType.Boolean;
            case ADD: {
                ValType ltype = expressionType(node.getLeft());
                ValType rtype = expressionType(node.getRight());
                if (ltype == ValType.String || rtype == ValType.String) {
                    return ValType.String;
                } else if (ltype.isNumeric() && rtype.isPrimitive()) {
                    return ValType.Number;
                }
                // Pessimistically assume any-type
                return ValType.Any;
            }
            case AND:
            case OR:
                return expressionType(node.getLeft()) == ValType.Boolean
                        && expressionType(node.getRight()) == ValType.Boolean ? ValType.Boolean
                        : ValType.Any;
            default:
                throw new AssertionError();
            }
        }

        @Override
        public ValType visit(BooleanLiteral node, Void value) {
            return ValType.Boolean;
        }

        @Override
        public ValType visit(ClassExpression node, Void value) {
            return ValType.Object;
        }

        @Override
        public ValType visit(CommaExpression node, Void value) {
            List<Expression> operands = node.getOperands();
            return expressionType(operands.get(operands.size() - 1));
        }

        @Override
        public ValType visit(ConditionalExpression node, Void value) {
            ValType ltype = expressionType(node.getThen());
            ValType rtype = expressionType(node.getOtherwise());
            if (ltype != rtype && ltype.isNumeric() && rtype.isNumeric()) {
                return ValType.Number;
            }
            return ltype == rtype ? ltype : ValType.Any;
        }

        @Override
        public ValType visit(FunctionExpression node, Void value) {
            return ValType.Object;
        }

        @Override
        public ValType visit(GeneratorComprehension node, Void value) {
            return ValType.Object;
        }

        @Override
        public ValType visit(GeneratorExpression node, Void value) {
            return ValType.Object;
        }

        @Override
        public ValType visit(LetExpression node, Void value) {
            return expressionType(node.getExpression());
        }

        @Override
        public ValType visit(NewExpression node, Void value) {
            return ValType.Object;
        }

        @Override
        public ValType visit(NullLiteral node, Void value) {
            return ValType.Null;
        }

        @Override
        public ValType visit(NumericLiteral node, Void value) {
            return node.isInt() ? ValType.Number_int : ValType.Number;
        }

        @Override
        public ValType visit(ObjectLiteral node, Void value) {
            return ValType.Object;
        }

        @Override
        public ValType visit(RegularExpressionLiteral node, Void value) {
            return ValType.Object;
        }

        @Override
        public ValType visit(StringLiteral node, Void value) {
            return ValType.String;
        }

        @Override
        public ValType visit(SuperCallExpression node, Void value) {
            return ValType.Object;
        }

        @Override
        public ValType visit(SuperNewExpression node, Void value) {
            return ValType.Object;
        }

        @Override
        public ValType visit(TemplateLiteral node, Void value) {
            return ValType.String;
        }

        @Override
        public ValType visit(UnaryExpression node, Void value) {
            switch (node.getOperator()) {
            case BITNOT:
                return ValType.Number_int;
            case NEG:
            case POS:
            case POST_DEC:
            case POST_INC:
            case PRE_DEC:
            case PRE_INC:
                return ValType.Number;
            case DELETE:
            case NOT:
                return ValType.Boolean;
            case VOID:
                return ValType.Undefined;
            case TYPEOF:
                return ValType.String;
            default:
                throw new AssertionError();
            }
        }
    }

    /**
     * 12.2.4 Literals
     * <p>
     * 12.2.4.1 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(BooleanLiteral node, CodeVisitor mv) {
        /* steps 1-2 */
        mv.iconst(node.getValue());
        return ValType.Boolean;
    }

    /**
     * 12.3.4 Function Calls
     * <p>
     * 12.3.4.1 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(CallExpression node, CodeVisitor mv) {
        return EvaluateCall(node, node.getBase(), node.getArguments(), mv);
    }

    /**
     * 12.3.6.1 Runtime Semantics: ArgumentListEvaluation
     */
    @Override
    public ValType visit(CallSpreadElement node, CodeVisitor mv) {
        ValType type = node.getExpression().accept(this, mv);
        mv.toBoxed(type);
        mv.loadExecutionContext();
        mv.lineInfo(node);
        mv.invoke(Methods.ScriptRuntime_SpreadArray);

        return ValType.Any; // actually Object[]
    }

    /**
     * 14.5.16 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(ClassExpression node, CodeVisitor mv) {
        /* steps 1-2 */
        Name className = node.getIdentifier() != null ? node.getIdentifier().getName() : null;
        /* steps 3-4 */
        ClassDefinitionEvaluation(node, className, mv);
        /* step 5 */
        if (className != null) {
            SetFunctionName(node, className, mv);
        }
        /* step 6 */
        return ValType.Object;
    }

    /**
     * 12.15.3 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(CommaExpression node, CodeVisitor mv) {
        assert !node.getOperands().isEmpty() : "empty comma expression";
        int count = node.getOperands().size();
        for (Expression e : node.getOperands()) {
            if (--count == 0) {
                return e.accept(this, mv);
            }
            ValType type = e.emptyCompletion().accept(this, mv);
            mv.pop(type);
        }
        return null;
    }

    /**
     * 12.13 Conditional Operator ( ? : )
     * <p>
     * 12.13.3 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(ConditionalExpression node, CodeVisitor mv) {
        Jump l0 = new Jump(), l1 = new Jump();

        /* steps 1-2 */
        ValType typeTest = node.getTest().accept(this, mv);
        /* step 2 */
        ToBoolean(typeTest, mv);
        /* step 3 */
        mv.ifeq(l0);
        ValType typeThen = node.getThen().accept(this, mv);
        if (typeThen.isJavaPrimitive()) {
            // Try to avoid boxing if then-and-otherwise are both compatible primitive types.
            ValType expected = expressionType(node.getOtherwise());
            boolean sameType = typeThen == expected;
            if (sameType || (typeThen.isNumeric() && expected.isNumeric())) {
                if (!sameType) {
                    ToNumber(typeThen, mv);
                }
                mv.goTo(l1);
                mv.mark(l0);
                ValType typeOtherwise = node.getOtherwise().accept(this, mv);
                assert expected == typeOtherwise : String.format("expected=%s, got=%s", expected,
                        typeOtherwise);
                if (!sameType) {
                    ToNumber(typeOtherwise, mv);
                }
                mv.mark(l1);
                return sameType ? typeThen : ValType.Number;
            }
        }
        mv.toBoxed(typeThen);
        mv.goTo(l1);
        /* step 4 */
        mv.mark(l0);
        ValType typeOtherwise = node.getOtherwise().accept(this, mv);
        mv.toBoxed(typeOtherwise);
        mv.mark(l1);

        return typeThen == typeOtherwise ? typeThen : ValType.Any;
    }

    /**
     * 12.3.2 Property Accessors
     * <p>
     * 12.3.2.1 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(ElementAccessor node, CodeVisitor mv) {
        ReferenceOp<ElementAccessor> op = ReferenceOp.propertyOp(node);
        return op.referenceValue(node, mv, codegen);
    }

    @Override
    public ValType visit(EmptyExpression node, CodeVisitor mv) {
        return ValType.Empty;
    }

    @Override
    public ValType visit(ExpressionMethod node, CodeVisitor mv) {
        MethodName method = codegen.compile(node, mv);
        boolean hasResume = node.hasResumePoint();

        // stack: [] -> [result]
        mv.lineInfo(0); // 0 = hint for stacktraces to omit this frame
        if (hasResume) {
            mv.callWithSuspend(method);
        } else {
            mv.call(method);
        }

        return ValType.Any;
    }

    /**
     * 14.1.17 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(FunctionExpression node, CodeVisitor mv) {
        MethodName method = codegen.compile(node);

        /* steps 1-5/10 */
        mv.invoke(method);
        mv.loadExecutionContext();
        if (isLegacy(node)) {
            mv.invoke(Methods.ScriptRuntime_EvaluateLegacyFunctionExpression);
        } else {
            mv.invoke(Methods.ScriptRuntime_EvaluateFunctionExpression);
        }

        /* step 6/11 */
        return ValType.Object;
    }

    private boolean isLegacy(FunctionExpression node) {
        if (IsStrict(node)) {
            return false;
        }
        return codegen.isEnabled(CompatibilityOption.FunctionArguments)
                || codegen.isEnabled(CompatibilityOption.FunctionCaller);
    }

    /**
     * Extension: 'function.sent' meta property
     */
    @Override
    public ValType visit(FunctionSent node, CodeVisitor mv) {
        mv.loadExecutionContext();
        mv.lineInfo(node);
        mv.invoke(Methods.ScriptRuntime_functionSent);

        return ValType.Any;
    }

    /**
     * 12.2.7.2 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(GeneratorComprehension node, CodeVisitor mv) {
        MethodName method = codegen.compile(node);

        /* steps 1-8 */
        mv.invoke(method);
        mv.loadExecutionContext();
        if (node.getComprehension() instanceof LegacyComprehension) {
            mv.invoke(Methods.ScriptRuntime_EvaluateLegacyGeneratorComprehension);
        } else if (node.isConstructor()) {
            mv.invoke(Methods.ScriptRuntime_EvaluateConstructorGeneratorComprehension);
        } else {
            mv.invoke(Methods.ScriptRuntime_EvaluateGeneratorComprehension);
        }

        /* step 9 */
        return ValType.Object;
    }

    /**
     * 14.4.14 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(GeneratorExpression node, CodeVisitor mv) {
        MethodName method = codegen.compile(node);

        /* steps 1-7/11 */
        mv.invoke(method);
        mv.loadExecutionContext();
        if (node.isConstructor()) {
            mv.invoke(Methods.ScriptRuntime_EvaluateConstructorGeneratorExpression);
        } else {
            mv.invoke(Methods.ScriptRuntime_EvaluateGeneratorExpression);
        }

        /* step 8/12 */
        return ValType.Object;
    }

    /**
     * 14.4.14 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(LegacyGeneratorExpression node, CodeVisitor mv) {
        MethodName method = codegen.compile(node);

        /* steps 1-7 */
        mv.invoke(method);
        mv.loadExecutionContext();
        mv.invoke(Methods.ScriptRuntime_EvaluateLegacyGeneratorExpression);

        /* step 8 */
        return ValType.Object;
    }

    /**
     * 12.1 Identifiers
     * <p>
     * 12.1.6 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(IdentifierReference node, CodeVisitor mv) {
        /* steps 1-2 */
        ReferenceOp<IdentifierReference> op = ReferenceOp.of(node);
        return op.referenceValue(node, mv, codegen);
    }

    /**
     * Extension: 'let' expression
     */
    @Override
    public ValType visit(LetExpression node, CodeVisitor mv) {
        BlockScope scope = node.getScope();
        if (scope.isPresent()) {
            mv.enterVariableScope();
            Variable<LexicalEnvironment<DeclarativeEnvironmentRecord>> env = mv.newVariable("env",
                    LexicalEnvironment.class).uncheckedCast();
            Variable<DeclarativeEnvironmentRecord> envRec = mv.newVariable("envRec",
                    DeclarativeEnvironmentRecord.class);

            newDeclarativeEnvironment(scope, mv);
            mv.store(env);
            getEnvRec(env, envRec, mv);

            for (LexicalBinding lexical : node.getBindings()) {
                Binding binding = lexical.getBinding();
                Expression initializer = lexical.getInitializer();
                for (Name name : BoundNames(binding)) {
                    BindingOp<DeclarativeEnvironmentRecord> op = BindingOp.of(envRec, name);
                    op.createMutableBinding(envRec, name, false, mv);
                }
                if (initializer == null) {
                    // LexicalBinding : BindingIdentifier
                    assert binding instanceof BindingIdentifier;
                    Name name = ((BindingIdentifier) binding).getName();
                    /* steps 1-2 */
                    // stack: [] -> []
                    InitializeBoundNameWithUndefined(envRec, name, mv);
                } else if (binding instanceof BindingIdentifier) {
                    // LexicalBinding : BindingIdentifier Initializer
                    Name name = ((BindingIdentifier) binding).getName();
                    /* steps 1-7 */
                    InitializeBoundNameWithInitializer(codegen, envRec, name, initializer, mv);
                } else {
                    // LexicalBinding : BindingPattern Initializer
                    assert binding instanceof BindingPattern;
                    /* steps 1-3 */
                    expressionBoxed(initializer, mv);
                    /* steps 4-5 */
                    BindingInitializationGenerator.BindingInitialization(codegen, envRec,
                            (BindingPattern) binding, mv);
                }
            }

            mv.load(env);
            pushLexicalEnvironment(mv);

            mv.exitVariableScope();
        }

        mv.enterScope(node);
        ValType type = node.getExpression().accept(this, mv);
        mv.exitScope();

        if (scope.isPresent()) {
            popLexicalEnvironment(mv);
        }
        return type;
    }

    @Override
    public ValType visit(NativeCallExpression node, CodeVisitor mv) {
        String nativeName = node.getBase().getName();
        List<Expression> arguments = node.getArguments();
        Type[] parameters = new Type[1 + arguments.size()];
        mv.loadExecutionContext();
        parameters[0] = Types.ExecutionContext;
        for (int i = 0; i < arguments.size(); ++i) {
            parameters[i + 1] = nativeCallArgument(arguments.get(i), mv);
        }
        mv.lineInfo(node);
        return invokeDynamicNativeCall(nativeName, parameters, mv);
    }

    private Type nativeCallArgument(Expression argument, CodeVisitor mv) {
        if (argument instanceof CallSpreadElement) {
            CallSpreadElement spread = (CallSpreadElement) argument;
            ValType type = spread.getExpression().accept(this, mv);
            mv.toBoxed(type);
            mv.loadExecutionContext();
            mv.lineInfo(spread);
            mv.invoke(Methods.ScriptRuntime_NativeCallSpreadArray);

            return Types.Object_;
        }
        return argument.accept(this, mv).toType();
    }

    /**
     * 12.3.3 The new Operator
     * <p>
     * 12.3.3.1 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(NewExpression node, CodeVisitor mv) {
        /* step 1 */
        return EvaluateNew(node, mv);
    }

    /**
     * 12.3.8 Meta Properties
     * <p>
     * 12.3.8.1 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(NewTarget node, CodeVisitor mv) {
        mv.loadExecutionContext();
        mv.invoke(Methods.ScriptRuntime_GetNewTargetOrUndefined);
        return ValType.Any;
    }

    /**
     * 12.2.4 Literals
     * <p>
     * 12.2.4.1 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(NullLiteral node, CodeVisitor mv) {
        /* step 1 */
        mv.loadNull();
        return ValType.Null;
    }

    /**
     * 12.2.4 Literals
     * <p>
     * 12.2.4.1 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(NumericLiteral node, CodeVisitor mv) {
        if (node.isInt()) {
            /* step 1 */
            mv.iconst(node.intValue());
            return ValType.Number_int;
        } else {
            /* step 1 */
            mv.dconst(node.doubleValue());
            return ValType.Number;
        }
    }

    /**
     * 12.2.6 Object Initializer
     * <p>
     * 12.2.6.8 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(ObjectLiteral node, CodeVisitor mv) {
        Variable<ArrayList<Object>> decorators = null;
        boolean hasDecorators = HasDecorators(node);
        if (hasDecorators) {
            mv.enterVariableScope();
            decorators = newDecoratorVariable("decorators", mv);
        }
        PropertyGenerator propgen = codegen.propertyGenerator(decorators);
        /* step 1 */
        mv.loadExecutionContext();
        mv.get(Fields.Intrinsics_ObjectPrototype);
        mv.invoke(Methods.OrdinaryObject_ObjectCreate);
        /* steps 2-3 */
        for (PropertyDefinition property : node.getProperties()) {
            mv.dup();
            property.accept(propgen, mv);
        }
        if (hasDecorators) {
            mv.dup();
            mv.load(decorators);
            mv.loadExecutionContext();
            mv.lineInfo(node);
            mv.invoke(Methods.ScriptRuntime_EvaluateMethodDecorators);
            mv.exitVariableScope();
        }
        /* step 4 */
        return ValType.Object;
    }

    /**
     * 12.3.2 Property Accessors
     * <p>
     * 12.3.2.1 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(PropertyAccessor node, CodeVisitor mv) {
        ReferenceOp<PropertyAccessor> op = ReferenceOp.propertyOp(node);
        return op.referenceValue(node, mv, codegen);
    }

    /**
     * 12.2.8 Regular Expression Literals
     * <p>
     * 12.2.8.2 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(RegularExpressionLiteral node, CodeVisitor mv) {
        mv.loadExecutionContext();
        /* step 1 */
        mv.aconst(node.getRegexp());
        /* step 2 */
        mv.aconst(node.getFlags());
        /* step 3 */
        mv.lineInfo(node);
        mv.invoke(Methods.RegExpConstructor_RegExpCreate);
        return ValType.Object;
    }

    /**
     * 12.2.4 Literals
     * <p>
     * 12.2.4.1 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(StringLiteral node, CodeVisitor mv) {
        /* step 1 */
        mv.aconst(node.getValue());
        return ValType.String;
    }

    /**
     * 12.3.5.1 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(SuperCallExpression node, CodeVisitor mv) {
        /* steps 1-2 */
        // stack: [] -> [newTarget]
        mv.loadExecutionContext();
        mv.lineInfo(node);
        mv.invoke(Methods.ScriptRuntime_GetNewTarget);

        /* steps 3-4 */
        // stack: [newTarget] -> [constructor, newTarget]
        mv.loadExecutionContext();
        mv.lineInfo(node);
        mv.invoke(Methods.ScriptRuntime_GetSuperConstructor);
        mv.swap();

        // stack: [constructor, newTarget] -> [constructor, cx, newTarget]
        mv.loadExecutionContext();
        mv.swap();

        /* steps 5-6 */
        // stack: [constructor, cx, newTarget] -> [constructor, cx, newTarget, argList]
        ArgumentListEvaluation(node, node.getArguments(), mv);

        /* steps 7-8 */
        // stack: [constructor, cx, newTarget, argList] -> [result]
        mv.lineInfo(node);
        invokeDynamicSuper(mv);

        /* steps 9-10 */
        // stack: [result] -> [result]
        mv.dup();
        mv.loadExecutionContext();
        mv.lineInfo(node);
        mv.invoke(Methods.ScriptRuntime_BindThisValue);

        return ValType.Object;
    }

    /**
     * 12.3.5.1 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(SuperElementAccessor node, CodeVisitor mv) {
        ReferenceOp<SuperElementAccessor> op = ReferenceOp.propertyOp(node);
        return op.referenceValue(node, mv, codegen);
    }

    /**
     * 12.3.5.1 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(SuperNewExpression node, CodeVisitor mv) {
        /* steps 1-2 */
        // stack: [] -> [newTarget]
        mv.loadExecutionContext();
        mv.lineInfo(node);
        mv.invoke(Methods.ScriptRuntime_GetNewTarget);

        /* steps 3-4 */
        // stack: [newTarget] -> [constructor, newTarget]
        mv.loadExecutionContext();
        mv.lineInfo(node);
        mv.invoke(Methods.ScriptRuntime_GetSuperConstructor);
        mv.swap();

        // stack: [constructor, newTarget] -> [constructor, cx, newTarget]
        mv.loadExecutionContext();
        mv.swap();

        /* steps 5-6 */
        // stack: [constructor, cx, newTarget] -> [constructor, cx, newTarget, argList]
        ArgumentListEvaluation(node, node.getArguments(), mv);

        /* steps 7-13 */
        // stack: [constructor, cx, newTarget, argList] -> [result]
        mv.lineInfo(node);
        invokeDynamicSuper(mv);
        return ValType.Object;
    }

    /**
     * 12.3.5.1 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(SuperPropertyAccessor node, CodeVisitor mv) {
        ReferenceOp<SuperPropertyAccessor> op = ReferenceOp.propertyOp(node);
        return op.referenceValue(node, mv, codegen);
    }

    /**
     * 12.3.7 Tagged Templates
     * <p>
     * 12.3.7.1 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(TemplateCallExpression node, CodeVisitor mv) {
        codegen.compile(node.getTemplate());

        // 12.2.9.2.1 Runtime Semantics: ArgumentListEvaluation
        // 12.2.8.2.2 Runtime Semantics: GetTemplateObject
        // 12.2.9.2.3 Runtime Semantics: SubstitutionEvaluation
        TemplateLiteral template = node.getTemplate();
        List<Expression> substitutions = Substitutions(template);
        ArrayList<Expression> arguments = new ArrayList<>(substitutions.size() + 1);
        arguments.add(template);
        arguments.addAll(substitutions);

        /* steps 1-4 */
        return EvaluateCall(node, node.getBase(), arguments, mv);
    }

    /**
     * 12.2.9 Template Literals
     * <p>
     * 12.2.9.5 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(TemplateLiteral node, CodeVisitor mv) {
        if (node.isTagged()) {
            codegen.GetTemplateObject(node, mv);
            return ValType.Object;
        }

        List<Expression> elements = node.getElements();
        if (elements.size() == 1) {
            assert elements.get(0) instanceof TemplateCharacters;
            TemplateCharacters chars = (TemplateCharacters) elements.get(0);
            mv.aconst(chars.getValue());
        } else {
            // TODO: change to expression::concat?
            mv.anew(Types.StringBuilder, Methods.StringBuilder_init);
            for (Expression expr : elements) {
                if (expr instanceof TemplateCharacters) {
                    String value = ((TemplateCharacters) expr).getValue();
                    if (!value.isEmpty()) {
                        mv.aconst(value);
                        mv.invoke(Methods.StringBuilder_append_String);
                    }
                } else {
                    ValType type = expr.accept(this, mv);
                    ToString(type, mv);
                    mv.invoke(Methods.StringBuilder_append_Charsequence);
                }
            }
            mv.invoke(Methods.StringBuilder_toString);
        }
        return ValType.String;
    }

    /**
     * 12.2.2 The this Keyword
     * <p>
     * 12.2.2.1 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(ThisExpression node, CodeVisitor mv) {
        /* step 1 */
        mv.loadExecutionContext();
        mv.lineInfo(node);
        mv.invoke(Methods.ExecutionContext_resolveThisBinding);
        return ValType.Any;
    }

    /**
     * 12.4.4.1 Runtime Semantics: Evaluation<br>
     * 12.4.5.1 Runtime Semantics: Evaluation<br>
     * 12.5.4.2 Runtime Semantics: Evaluation<br>
     * 12.5.5.1 Runtime Semantics: Evaluation<br>
     * 12.5.6.1 Runtime Semantics: Evaluation<br>
     * 12.5.7.1 Runtime Semantics: Evaluation<br>
     * 12.5.8.1 Runtime Semantics: Evaluation<br>
     * 12.5.9.1 Runtime Semantics: Evaluation<br>
     * 12.5.10.1 Runtime Semantics: Evaluation<br>
     * 12.5.11.1 Runtime Semantics: Evaluation<br>
     * 12.5.12.1 Runtime Semantics: Evaluation<br>
     */
    @Override
    public ValType visit(UnaryExpression node, CodeVisitor mv) {
        switch (node.getOperator()) {
        case POST_INC:
        case POST_DEC:
        case PRE_INC:
        case PRE_DEC:
            return unaryUpdateOp(node).emit(node, mv, this);
        case DELETE: {
            // 12.5.4 The delete Operator
            Expression operand = node.getOperand();
            if (operand instanceof LeftHandSideExpression) {
                LeftHandSideExpression lhs = (LeftHandSideExpression) operand;
                return ReferenceOp.of(lhs).delete(lhs, mv, codegen);
            }
            ValType type = operand.emptyCompletion().accept(this, mv);
            assert type != ValType.Reference;
            mv.pop(type);
            mv.iconst(true);
            return ValType.Boolean;
        }
        case VOID: {
            // 12.5.5 The void Operator
            ValType type = node.getOperand().emptyCompletion().accept(this, mv);
            mv.pop(type);
            if (node.hasCompletion()) {
                mv.loadUndefined();
                return ValType.Undefined;
            }
            return ValType.Empty;
        }
        case TYPEOF: {
            // 12.5.6 The typeof Operator
            Expression operand = node.getOperand();
            if (operand instanceof IdentifierReference) {
                IdentifierReference ident = (IdentifierReference) operand;
                Name resolvedName = ident.getResolvedName();
                if (resolvedName == null || !resolvedName.isLocal()) {
                    // TODO: Add referenceValueOrUndefined() method
                    ReferenceOp.LOOKUP.reference(ident, mv, codegen);
                    mv.loadExecutionContext();
                    mv.lineInfo(node);
                    mv.invoke(Methods.ScriptRuntime_typeof_Reference);
                    return ValType.String;
                }
            }
            ValType type = operand.accept(this, mv);
            mv.toBoxed(type);
            mv.invoke(Methods.ScriptRuntime_typeof);
            return ValType.String;
        }
        case POS: {
            // 12.5.9 Unary + Operator
            ValType type = node.getOperand().accept(this, mv);
            ToNumber(type, mv);
            return ValType.Number;
        }
        case NEG: {
            // 12.5.10 Unary - Operator
            ValType type = node.getOperand().accept(this, mv);
            ToNumber(type, mv);
            mv.dneg();
            return ValType.Number;
        }
        case BITNOT: {
            // 12.5.11 Bitwise NOT Operator ( ~ )
            ValType type = node.getOperand().accept(this, mv);
            ToInt32(type, mv);
            mv.bitnot();
            return ValType.Number_int;
        }
        case NOT: {
            // 12.5.12 Logical NOT Operator ( ! )
            if (!node.hasCompletion()) {
                ValType type = node.getOperand().emptyCompletion().accept(this, mv);
                mv.pop(type);
                return ValType.Empty;
            }
            ValType type = node.getOperand().accept(this, mv);
            ToBoolean(type, mv);
            mv.not();
            return ValType.Boolean;
        }
        default:
            throw new AssertionError(Objects.toString(node.getOperator(), "<null>"));
        }
    }

    private static UnaryUpdateOp unaryUpdateOp(UnaryExpression node) {
        switch (node.getOperator()) {
        case PRE_INC:
        case POST_INC:
            return UnaryUpdateOp.INCREMENT;
        case PRE_DEC:
        case POST_DEC:
            return UnaryUpdateOp.DECREMENT;
        default:
            throw new AssertionError(Objects.toString(node.getOperator(), "<null>"));
        }
    }

    private static abstract class UnaryUpdateOp {
        abstract void operation(CodeVisitor mv);

        final ValType emit(UnaryExpression node, CodeVisitor mv, ExpressionGenerator gen) {
            LeftHandSideExpression expr = (LeftHandSideExpression) node.getOperand();
            ReferenceOp<LeftHandSideExpression> op = ReferenceOp.of(expr);

            ValType type = op.referenceForUpdate(expr, mv, gen.codegen);
            ValType vtype = op.getValue(expr, type, mv);
            ToNumber(vtype, mv);

            if (!node.getOperator().isPostfix()) {
                operation(mv);
                return op.putValue(expr, type, ValType.Number, node.hasCompletion(), mv);
            }
            if (node.hasCompletion()) {
                Variable<?> saved = op.saveValue(type, ValType.Number, mv);
                operation(mv);
                op.putValue(expr, type, ValType.Number, mv);
                op.restoreValue(saved, mv);
                return ValType.Number;
            }
            operation(mv);
            op.putValue(expr, type, ValType.Number, mv);
            return ValType.Empty;
        }

        // 12.4.4 Postfix Increment Operator
        // 12.5.7 Prefix Increment Operator
        static final UnaryUpdateOp INCREMENT = new UnaryUpdateOp() {
            @Override
            void operation(CodeVisitor mv) {
                mv.dconst(1d);
                mv.dadd();
            }
        };
        // 12.4.5 Postfix Decrement Operator
        // 12.5.8 Prefix Decrement Operator
        static final UnaryUpdateOp DECREMENT = new UnaryUpdateOp() {
            @Override
            void operation(CodeVisitor mv) {
                mv.dconst(1d);
                mv.dsub();
            }
        };
    }

    /**
     * 14.4 Generator Function Definitions
     * <p>
     * 14.4.14 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(YieldExpression node, CodeVisitor mv) {
        Expression expr = node.getExpression();
        if (expr != null) {
            ValType type = expr.accept(this, mv);
            mv.toBoxed(type);
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
