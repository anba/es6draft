/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static com.github.anba.es6draft.compiler.ArrayComprehensionGenerator.EvaluateArrayComprehension;
import static com.github.anba.es6draft.semantics.StaticSemantics.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.github.anba.es6draft.ast.*;
import com.github.anba.es6draft.ast.scope.BlockScope;
import com.github.anba.es6draft.ast.scope.Name;
import com.github.anba.es6draft.ast.scope.Scope;
import com.github.anba.es6draft.ast.scope.TopLevelScope;
import com.github.anba.es6draft.ast.scope.WithScope;
import com.github.anba.es6draft.ast.synthetic.ElementAccessorValue;
import com.github.anba.es6draft.ast.synthetic.ExpressionMethod;
import com.github.anba.es6draft.ast.synthetic.IdentifierReferenceValue;
import com.github.anba.es6draft.ast.synthetic.PropertyAccessorValue;
import com.github.anba.es6draft.ast.synthetic.SpreadArrayLiteral;
import com.github.anba.es6draft.ast.synthetic.SpreadElementMethod;
import com.github.anba.es6draft.ast.synthetic.SuperElementAccessorValue;
import com.github.anba.es6draft.ast.synthetic.SuperPropertyAccessorValue;
import com.github.anba.es6draft.compiler.CodeGenerator.FunctionName;
import com.github.anba.es6draft.compiler.DefaultCodeGenerator.ValType;
import com.github.anba.es6draft.compiler.assembler.FieldName;
import com.github.anba.es6draft.compiler.assembler.Jump;
import com.github.anba.es6draft.compiler.assembler.MethodName;
import com.github.anba.es6draft.compiler.assembler.Type;
import com.github.anba.es6draft.compiler.assembler.Variable;
import com.github.anba.es6draft.parser.Parser;
import com.github.anba.es6draft.runtime.internal.Bootstrap;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.NativeCalls;
import com.github.anba.es6draft.runtime.objects.Eval.EvalFlags;

/**
 *
 */
final class ExpressionGenerator extends DefaultCodeGenerator<ValType, ExpressionVisitor> {
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

        // class: EnvironmentRecord
        static final MethodName EnvironmentRecord_createMutableBinding = MethodName.findInterface(
                Types.EnvironmentRecord, "createMutableBinding",
                Type.methodType(Type.VOID_TYPE, Types.String, Type.BOOLEAN_TYPE));

        static final MethodName EnvironmentRecord_withBaseObject = MethodName.findInterface(
                Types.EnvironmentRecord, "withBaseObject", Type.methodType(Types.ScriptObject));

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

        // class: LexicalEnvironment
        static final MethodName LexicalEnvironment_getEnvRec = MethodName.findVirtual(
                Types.LexicalEnvironment, "getEnvRec", Type.methodType(Types.EnvironmentRecord));

        // class: Math
        static final MethodName Math_pow = MethodName.findStatic(Types.Math, "pow",
                Type.methodType(Type.DOUBLE_TYPE, Type.DOUBLE_TYPE, Type.DOUBLE_TYPE));

        // class: OrdinaryObject
        static final MethodName OrdinaryObject_ObjectCreate = MethodName.findStatic(
                Types.OrdinaryObject, "ObjectCreate",
                Type.methodType(Types.OrdinaryObject, Types.ExecutionContext, Types.Intrinsics));

        // class: Reference
        static final MethodName Reference_getBase = MethodName.findVirtual(Types.Reference,
                "getBase", Type.methodType(Types.Object));

        static final MethodName Reference_getThisValue = MethodName.findVirtual(Types.Reference,
                "getThisValue", Type.methodType(Types.Object));

        static final MethodName Reference_getValue = MethodName.findVirtual(Types.Reference,
                "getValue", Type.methodType(Types.Object, Types.ExecutionContext));

        static final MethodName Reference_putValue = MethodName.findVirtual(Types.Reference,
                "putValue", Type.methodType(Type.VOID_TYPE, Types.Object, Types.ExecutionContext));

        static final MethodName Reference_delete = MethodName.findVirtual(Types.Reference,
                "delete", Type.methodType(Type.BOOLEAN_TYPE, Types.ExecutionContext));

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
                "typeof", Type.methodType(Types.String, Types.Object, Types.ExecutionContext));

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
                        Types.ArrayObject, Type.INT_TYPE, Types.Object, Types.ExecutionContext));

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

        static final MethodName ScriptRuntime_EvaluateGeneratorComprehension = MethodName
                .findStatic(Types.ScriptRuntime, "EvaluateGeneratorComprehension", Type.methodType(
                        Types.GeneratorObject, Types.RuntimeInfo$Function, Types.ExecutionContext));

        static final MethodName ScriptRuntime_EvaluateLegacyGeneratorComprehension = MethodName
                .findStatic(Types.ScriptRuntime, "EvaluateLegacyGeneratorComprehension", Type
                        .methodType(Types.GeneratorObject, Types.RuntimeInfo$Function,
                                Types.ExecutionContext));

        static final MethodName ScriptRuntime_EvaluateGeneratorExpression = MethodName.findStatic(
                Types.ScriptRuntime, "EvaluateGeneratorExpression", Type
                        .methodType(Types.OrdinaryGenerator, Types.RuntimeInfo$Function,
                                Types.ExecutionContext));

        static final MethodName ScriptRuntime_EvaluateLegacyGeneratorExpression = MethodName
                .findStatic(Types.ScriptRuntime, "EvaluateLegacyGeneratorExpression", Type
                        .methodType(Types.OrdinaryGenerator, Types.RuntimeInfo$Function,
                                Types.ExecutionContext));

        static final MethodName ScriptRuntime_EvaluateMethodDecorators = MethodName.findStatic(
                Types.ScriptRuntime, "EvaluateMethodDecorators", Type.methodType(Type.VOID_TYPE,
                        Types.OrdinaryObject, Types.ArrayList, Types.ExecutionContext));

        static final MethodName ScriptRuntime_getElement = MethodName.findStatic(
                Types.ScriptRuntime, "getElement", Type.methodType(Types.Reference, Types.Object,
                        Types.Object, Types.ExecutionContext, Type.BOOLEAN_TYPE));

        static final MethodName ScriptRuntime_getElementValue = MethodName.findStatic(
                Types.ScriptRuntime, "getElementValue",
                Type.methodType(Types.Object, Types.Object, Types.Object, Types.ExecutionContext));

        static final MethodName ScriptRuntime_getProperty = MethodName.findStatic(
                Types.ScriptRuntime, "getProperty", Type.methodType(Types.Reference, Types.Object,
                        Types.String, Types.ExecutionContext, Type.BOOLEAN_TYPE));

        static final MethodName ScriptRuntime_getProperty_int = MethodName.findStatic(
                Types.ScriptRuntime, "getProperty", Type.methodType(Types.Reference, Types.Object,
                        Type.INT_TYPE, Types.ExecutionContext, Type.BOOLEAN_TYPE));

        static final MethodName ScriptRuntime_getProperty_long = MethodName.findStatic(
                Types.ScriptRuntime, "getProperty", Type.methodType(Types.Reference, Types.Object,
                        Type.LONG_TYPE, Types.ExecutionContext, Type.BOOLEAN_TYPE));

        static final MethodName ScriptRuntime_getProperty_double = MethodName.findStatic(
                Types.ScriptRuntime, "getProperty", Type.methodType(Types.Reference, Types.Object,
                        Type.DOUBLE_TYPE, Types.ExecutionContext, Type.BOOLEAN_TYPE));

        static final MethodName ScriptRuntime_getPropertyValue = MethodName.findStatic(
                Types.ScriptRuntime, "getPropertyValue",
                Type.methodType(Types.Object, Types.Object, Types.String, Types.ExecutionContext));

        static final MethodName ScriptRuntime_getPropertyValue_int = MethodName.findStatic(
                Types.ScriptRuntime, "getPropertyValue",
                Type.methodType(Types.Object, Types.Object, Type.INT_TYPE, Types.ExecutionContext));

        static final MethodName ScriptRuntime_getPropertyValue_long = MethodName
                .findStatic(Types.ScriptRuntime, "getPropertyValue", Type.methodType(Types.Object,
                        Types.Object, Type.LONG_TYPE, Types.ExecutionContext));

        static final MethodName ScriptRuntime_getPropertyValue_double = MethodName.findStatic(
                Types.ScriptRuntime, "getPropertyValue", Type.methodType(Types.Object,
                        Types.Object, Type.DOUBLE_TYPE, Types.ExecutionContext));

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

        static final MethodName ScriptRuntime_getSuperPropertyReferenceValue = MethodName
                .findStatic(Types.ScriptRuntime, "getSuperPropertyReferenceValue", Type.methodType(
                        Types.Object, Types.ExecutionContext, Types.Object, Type.BOOLEAN_TYPE));

        static final MethodName ScriptRuntime_getSuperPropertyReferenceValue_String = MethodName
                .findStatic(Types.ScriptRuntime, "getSuperPropertyReferenceValue", Type.methodType(
                        Types.Object, Types.ExecutionContext, Types.String, Type.BOOLEAN_TYPE));

        static final MethodName ScriptRuntime_IsBuiltinEval = MethodName.findStatic(
                Types.ScriptRuntime, "IsBuiltinEval",
                Type.methodType(Type.BOOLEAN_TYPE, Types.Callable, Types.ExecutionContext));

        static final MethodName ScriptRuntime_MakeSuperPropertyReference = MethodName.findStatic(
                Types.ScriptRuntime, "MakeSuperPropertyReference", Type.methodType(Types.Reference,
                        Types.ExecutionContext, Types.Object, Type.BOOLEAN_TYPE));

        static final MethodName ScriptRuntime_MakeSuperPropertyReference_String = MethodName
                .findStatic(Types.ScriptRuntime, "MakeSuperPropertyReference", Type.methodType(
                        Types.Reference, Types.ExecutionContext, Types.String, Type.BOOLEAN_TYPE));

        static final MethodName ScriptRuntime_PrepareForTailCall = MethodName.findStatic(
                Types.ScriptRuntime, "PrepareForTailCall", Type.methodType(Types.Object,
                        Types.Callable, Types.ExecutionContext, Types.Object, Types.Object_));

        static final MethodName ScriptRuntime_PrepareForTailCallUnchecked = MethodName.findStatic(
                Types.ScriptRuntime, "PrepareForTailCall", Type.methodType(Types.Object,
                        Types.Object, Types.ExecutionContext, Types.Object, Types.Object_));

        static final MethodName ScriptRuntime_SpreadArray = MethodName.findStatic(
                Types.ScriptRuntime, "SpreadArray",
                Type.methodType(Types.Object_, Types.Object, Types.ExecutionContext));

        static final MethodName ScriptRuntime_toFlatArray = MethodName.findStatic(
                Types.ScriptRuntime, "toFlatArray",
                Type.methodType(Types.Object_, Types.Object_, Types.ExecutionContext));

        static final MethodName ScriptRuntime_toStr = MethodName.findStatic(Types.ScriptRuntime,
                "toStr", Type.methodType(Types.CharSequence, Types.Object, Types.ExecutionContext));

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

    private static final IdentifierResolution identifierResolution = new IdentifierResolution();

    public ExpressionGenerator(CodeGenerator codegen) {
        super(codegen);
    }

    private static void invokeDynamicCall(ExpressionVisitor mv) {
        // stack: [func(Callable), cx, thisValue, args] -> [result]
        mv.invokedynamic(Bootstrap.getCallName(), Bootstrap.getCallMethodDescriptor(),
                Bootstrap.getCallBootstrap());
    }

    private static void invokeDynamicConstruct(ExpressionVisitor mv) {
        // stack: [constructor(Constructor), cx, args] -> [result]
        mv.invokedynamic(Bootstrap.getConstructName(), Bootstrap.getConstructMethodDescriptor(),
                Bootstrap.getConstructBootstrap());
    }

    private static void invokeDynamicSuper(ExpressionVisitor mv) {
        // stack: [constructor(Constructor), cx, newTarget, args] -> [result]
        mv.invokedynamic(Bootstrap.getSuperName(), Bootstrap.getSuperMethodDescriptor(),
                Bootstrap.getSuperBootstrap());
    }

    private static void invokeDynamicNativeCall(String name, ExpressionVisitor mv) {
        // stack: [args, cx] -> [result]
        mv.invokedynamic(NativeCalls.getNativeCallName(name),
                NativeCalls.getNativeCallMethodDescriptor(), NativeCalls.getNativeCallBootstrap());
    }

    private static void invokeDynamicOperator(BinaryExpression.Operator operator,
            ExpressionVisitor mv) {
        // stack: [lval, rval, cx?] -> [result]
        mv.invokedynamic(Bootstrap.getName(operator), Bootstrap.getMethodDescriptor(operator),
                Bootstrap.getBootstrap(operator));
    }

    private static void invokeDynamicConcat(int numberOfStrings, ExpressionVisitor mv) {
        mv.invokedynamic(Bootstrap.getConcatName(),
                Bootstrap.getConcatMethodDescriptor(numberOfStrings),
                Bootstrap.getConcatBootstrap());
    }

    /**
     * ref = `eval` {@code node}<br>
     * GetValue(ref)<br>
     * 
     * @param node
     *            the expression node to evaluate
     * @param mv
     *            the expression visitor
     */
    private void evalAndGetBoxedValue(Expression node, ExpressionVisitor mv) {
        ValType type = evalAndGetValue(node, mv);
        mv.toBoxed(type);
    }

    /**
     * ref = `eval` {@code node}<br>
     * GetValue(ref)<br>
     * 
     * @param node
     *            the expression node to evaluate
     * @param mv
     *            the expression visitor
     * @return the value type of the expression
     */
    private ValType evalAndGetValue(Expression node, ExpressionVisitor mv) {
        Expression valueNode = node.asValue();
        ValType type = valueNode.accept(this, mv);
        assert type != ValType.Reference : "value node returned reference: " + valueNode.getClass();
        return type;
    }

    private static ValType GetValue(LeftHandSideExpression node, ValType type, ExpressionVisitor mv) {
        assert type == ValType.Reference : "type is not reference: " + type;
        mv.loadExecutionContext();
        mv.lineInfo(node);
        mv.invoke(Methods.Reference_getValue);
        return ValType.Any;
    }

    private static void PutValue(LeftHandSideExpression node, ValType type, ExpressionVisitor mv) {
        assert type == ValType.Reference : "type is not reference: " + type;
        mv.loadExecutionContext();
        mv.lineInfo(node);
        mv.invoke(Methods.Reference_putValue);
    }

    /**
     * stack: [envRec] {@literal ->} [envRec].
     * 
     * @param name
     *            the binding name
     * @param deletable
     *            the deletable flag
     * @param mv
     *            the expression visitor
     */
    private void createMutableBinding(Name name, boolean deletable, ExpressionVisitor mv) {
        mv.dup();
        mv.aconst(name.getIdentifier());
        mv.iconst(deletable);
        mv.invoke(Methods.EnvironmentRecord_createMutableBinding);
    }

    private boolean isTailCall(Expression node, ExpressionVisitor mv) {
        return !codegen.isEnabled(Compiler.Option.NoTailCall) && mv.isTailCall(node);
    }

    /**
     * [12.3.3.1.1 Runtime Semantics: EvaluateNew(thisCall, constructProduction, arguments)]
     * 
     * @return
     */
    private ValType EvaluateNew(NewExpression node, ExpressionVisitor mv) {
        /* steps 1-2 (not applicable) */
        /* steps 3-5 */
        evalAndGetBoxedValue(node.getExpression(), mv);
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
        TopLevelScope top = currentScope.getTop();
        for (Scope scope : currentScope) {
            if (scope instanceof BlockScope) {
                if (catchVar) {
                    ScopedNode node = scope.getNode();
                    if (node instanceof CatchNode || node instanceof GuardedCatchNode) {
                        continue;
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
        Property, SuperProperty, Identifier, IdentifierWith, Eval, EvalWith, Value
    }

    private CallType callTypeOf(Expression call, Expression base, Scope scope) {
        if (base instanceof ElementAccessor || base instanceof PropertyAccessor) {
            return CallType.Property;
        }
        if (base instanceof SuperElementAccessor || base instanceof SuperPropertyAccessor) {
            return CallType.SuperProperty;
        }
        if (base instanceof IdentifierReference) {
            IdentifierReference ident = (IdentifierReference) base;
            boolean directEval = call instanceof CallExpression && "eval".equals(ident.getName());
            Name name = ident.toName();
            if (isEnclosedByWithStatement(name, scope)) {
                return directEval ? CallType.EvalWith : CallType.IdentifierWith;
            }
            return directEval ? CallType.Eval : CallType.Identifier;
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
     *            the expression visitor
     */
    private ValType EvaluateCall(Expression call, Expression base, List<Expression> arguments,
            ExpressionVisitor mv) {
        switch (callTypeOf(call, base, mv.getScope())) {
        case Property:
            return EvaluateCallProperty(call, (LeftHandSideExpression) base, arguments, mv);
        case SuperProperty:
            return EvaluateCallSuperProperty(call, (LeftHandSideExpression) base, arguments, mv);
        case Value:
            return EvaluateCallValue(call, base, arguments, mv);
        case Identifier:
            return EvaluateCallIdent(call, (IdentifierReference) base, arguments, mv);
        case IdentifierWith:
            return EvaluateCallIdentWith(call, (IdentifierReference) base, arguments, mv);
        case Eval:
            return EvaluateCallEval(call, (IdentifierReference) base, arguments, mv);
        case EvalWith:
            return EvaluateCallEvalWith(call, (IdentifierReference) base, arguments, mv);
        default:
            throw new AssertionError();
        }
    }

    private ValType EvaluateCallProperty(Expression call, LeftHandSideExpression base,
            List<Expression> arguments, ExpressionVisitor mv) {
        // Only called for the property reference case (`obj.method(...)` or `obj[method](...)`).

        // Inlined: Property accessor evaluation (12.3.2.1)
        // stack: [] -> [thisValue, func]
        if (base instanceof PropertyAccessor) {
            PropertyAccessor property = (PropertyAccessor) base;
            /* steps 1-3 (12.3.2.1) */
            evalAndGetBoxedValue(property.getBase(), mv);
            mv.dup();
            /* steps 4-6 (12.3.2.1) */
            mv.aconst(property.getName());
            /* steps 7-11 (12.3.2.1) */
            mv.loadExecutionContext();
            mv.lineInfo(property);
            mv.invoke(Methods.ScriptRuntime_getPropertyValue);
            /* step 11 (12.3.2.1, return) */
        } else {
            assert base instanceof ElementAccessor;
            ElementAccessor element = (ElementAccessor) base;
            /* steps 1-3 (12.3.2.1) */
            evalAndGetBoxedValue(element.getBase(), mv);
            mv.dup();
            /* steps 4-6 (12.3.2.1) */
            ValType elementType = toPropertyKey(element, evalAndGetValue(element.getElement(), mv),
                    mv);
            /* steps 7-11 (12.3.2.1) */
            mv.loadExecutionContext();
            mv.lineInfo(element);
            mv.invoke(elementValueMethod(elementType));
            /* step 11 (12.3.2.1, return) */
        }

        // stack: [thisValue, func] -> [func, cx, thisValue]
        mv.swap();
        mv.loadExecutionContext();
        mv.swap();

        /* steps 1-2 (not applicable) */
        /* steps 3-4 (not applicable) */
        /* step 5 */
        // stack: [func, cx, thisValue] -> [result]
        return EvaluateDirectCall(call, arguments, mv);
    }

    private ValType EvaluateCallSuperProperty(Expression call, LeftHandSideExpression base,
            List<Expression> arguments, ExpressionVisitor mv) {
        // Only called for the super property case (`super.method(...)` or `super[method](...)`).

        // stack: [] -> [ref]
        ValType type = base.accept(this, mv);
        assert type == ValType.Reference;

        /* steps 1-2 */
        // stack: [ref] -> [func, ref]
        mv.dup();
        GetValue(base, type, mv);
        mv.swap();

        /* steps 3-4 */
        // stack: [func, ref] -> [func, thisValue]
        mv.invoke(Methods.Reference_getThisValue);

        // stack: [func, thisValue] -> [func, cx, thisValue]
        mv.loadExecutionContext();
        mv.swap();

        /* step 5 */
        // stack: [func, cx, thisValue] -> [result]
        return EvaluateDirectCall(call, arguments, mv);
    }

    private ValType EvaluateCallValue(Expression call, Expression base, List<Expression> arguments,
            ExpressionVisitor mv) {
        // stack: [] -> [func]
        ValType type = base.accept(this, mv);
        mv.toBoxed(type);
        assert type != ValType.Reference;

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

    private ValType EvaluateCallIdent(Expression call, IdentifierReference base,
            List<Expression> arguments, ExpressionVisitor mv) {
        /* steps 1-2 */
        // stack: [] -> [func]
        evalAndGetValue(base, mv);

        /* steps 3-4 */
        // stack: [func] -> [func, cx, thisValue]
        mv.loadExecutionContext();
        mv.loadUndefined();

        /* step 5 */
        // stack: [func, cx, thisValue] -> [result]
        return EvaluateDirectCall(call, arguments, mv);
    }

    private ValType EvaluateCallIdentWith(Expression call, IdentifierReference base,
            List<Expression> arguments, ExpressionVisitor mv) {
        // stack: [] -> [ref]
        ValType type = base.accept(this, mv);
        assert type == ValType.Reference;

        // stack: [ref] -> [ref, ref]
        mv.dup();

        /* steps 1-2 */
        // stack: [ref, ref] -> [func, ref]
        GetValue(base, type, mv);
        mv.swap();

        /* step 3-4 */
        // stack: [func, ref] -> [func, thisValue]
        withBaseObject(mv);

        // stack: [func, thisValue] -> [func, cx, thisValue]
        mv.loadExecutionContext();
        mv.swap();

        /* step 5 */
        // stack: [func, cx, thisValue] -> [result]
        return EvaluateDirectCall(call, arguments, mv);
    }

    private ValType EvaluateCallEval(Expression call, IdentifierReference base,
            List<Expression> arguments, ExpressionVisitor mv) {
        /* steps 1-2 */
        // stack: [] -> [func]
        evalAndGetValue(base, mv);

        /* steps 3-4 (omitted) */
        /* step 5 */
        // stack: [func] -> [result]
        return EvaluateDirectCallEval(call, arguments, false, mv);
    }

    private ValType EvaluateCallEvalWith(Expression call, IdentifierReference base,
            List<Expression> arguments, ExpressionVisitor mv) {
        // stack: [] -> [ref]
        ValType type = base.accept(this, mv);
        assert type == ValType.Reference;

        // stack: [ref] -> [ref, ref]
        mv.dup();

        /* step 3-4 */
        // stack: [ref, ref] -> [thisValue, ref]
        withBaseObject(mv);
        mv.swap();

        /* steps 1-2 */
        // stack: [thisValue, ref] -> [thisValue, func]
        GetValue(base, type, mv);

        /* step 5 */
        // stack: [thisValue, func] -> [result]
        return EvaluateDirectCallEval(call, arguments, true, mv);
    }

    private void withBaseObject(ExpressionVisitor mv) {
        // stack: [ref] -> [baseObj?]
        mv.invoke(Methods.Reference_getBase);
        mv.checkcast(Types.EnvironmentRecord);
        mv.invoke(Methods.EnvironmentRecord_withBaseObject);

        // stack: [baseObj?] -> [thisValue]
        Jump baseObjNotNull = new Jump();
        mv.dup();
        mv.ifnonnull(baseObjNotNull);
        {
            mv.pop();
            mv.loadUndefined();
        }
        mv.mark(baseObjNotNull);
    }

    /**
     * [12.3.4.3 Runtime Semantics: EvaluateDirectCall( func, thisValue, arguments, tailPosition )]
     * 
     * @param call
     *            the function call expression
     * @param arguments
     *            the function arguments
     * @param mv
     *            the expression visitor
     */
    private ValType EvaluateDirectCall(Expression call, List<Expression> arguments,
            ExpressionVisitor mv) {
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
     *            the expression visitor
     */
    private ValType EvaluateDirectCallEval(Expression call, List<Expression> arguments,
            boolean hasThisValue, ExpressionVisitor mv) {
        Jump afterCall = new Jump(), notEval = new Jump();

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
     *            the expression visitor
     */
    private void PerformEval(Expression call, List<Expression> arguments, boolean hasThisValue,
            Jump afterCall, ExpressionVisitor mv) {
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
        if (isEnclosedByLexicalDeclaration(mv.getScope())) {
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
                    evalAndGetBoxedValue(arguments.get(0), mv);
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
     *            the expression visitor
     */
    private void ArgumentListEvaluation(Expression call, List<Expression> arguments,
            ExpressionVisitor mv) {
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
                evalAndGetBoxedValue(argument, mv);
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
    protected ValType visit(Node node, ExpressionVisitor mv) {
        throw new IllegalStateException(String.format("node-class: %s", node.getClass()));
    }

    /**
     * 12.2.4.2.5 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(ArrayComprehension node, ExpressionVisitor mv) {
        return EvaluateArrayComprehension(codegen, node, mv);
    }

    /**
     * 12.2.4.1.3 Runtime Semantics: Evaluation<br>
     * 12.2.4.1.2 Runtime Semantics: Array Accumulation
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
                        evalAndGetBoxedValue(element, mv);
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
                    evalAndGetBoxedValue(element, mv);
                    mv.loadExecutionContext();
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
    public ValType visit(SpreadArrayLiteral node, ExpressionVisitor mv) {
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
                mv.iadd();
                elisionWidth = 0;
            }
            if (element instanceof SpreadElement) {
                element.accept(this, mv);
            } else {
                // stack: [array, nextIndex] -> [array, nextIndex, array, nextIndex]
                mv.dup2();
                evalAndGetBoxedValue(element, mv);
                mv.loadExecutionContext();
                // stack: [array, nextIndex, array, nextIndex, value, cx] -> [array, nextIndex]
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
    public ValType visit(SpreadElement node, ExpressionVisitor mv) {
        // stack: [array, nextIndex] -> [array, array, nextIndex]
        mv.swap();
        mv.dupX1();
        mv.swap();

        // stack: [array, array, nextIndex] -> [array, array, nextIndex, value]
        Expression spread = node.getExpression();
        evalAndGetBoxedValue(spread, mv);

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

        /* steps 1-4 */
        mv.invoke(codegen.methodDesc(node, FunctionName.RTI));
        mv.loadExecutionContext();
        mv.invoke(Methods.ScriptRuntime_EvaluateArrowFunction);

        /* step 5 */
        return ValType.Object;
    }

    /**
     * 12.14.4 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(AssignmentExpression node, ExpressionVisitor mv) {
        LeftHandSideExpression left = node.getLeft();
        Expression right = node.getRight();
        if (node.getOperator() == AssignmentExpression.Operator.ASSIGN) {
            if (left instanceof AssignmentPattern) {
                ValType rtype = evalAndGetValue(right, mv);

                dup(node, mv);
                mv.toBoxed(rtype);
                DestructuringAssignment((AssignmentPattern) left, mv);

                return completion(node, rtype);
            } else {
                ValType ltype = left.accept(this, mv);
                ValType rtype = evalAndGetValue(right, mv);

                if (IsAnonymousFunctionDefinition(right) && IsIdentifierRef(left)) {
                    SetFunctionName(right, ((IdentifierReference) left).getName(), mv);
                }

                // lref rval
                dupX(node, ltype, rtype, mv);
                mv.toBoxed(rtype);
                PutValue(left, ltype, mv);

                return completion(node, rtype);
            }
        } else {
            switch (node.getOperator()) {
            case ASSIGN_EXP: {
                // Extension: Exponentiation Operator
                ValType ltype = left.accept(this, mv);
                mv.dup();
                ValType vtype = GetValue(left, ltype, mv);
                // lref lval
                if (right instanceof Literal) {
                    ToNumber(vtype, mv);
                }
                ValType rtype = evalAndGetValue(right, mv);
                if (!(right instanceof Literal)) {
                    mv.swap(ltype, rtype);
                    ToNumber(vtype, mv);
                    mv.swap(rtype, ValType.Number);
                }
                ToNumber(rtype, mv);
                // lref lval rval
                mv.invoke(Methods.Math_pow);
                // r lref r
                dupX(node, ltype, ValType.Number, mv);
                mv.toBoxed(ValType.Number);
                PutValue(left, ltype, mv);
                return completion(node, ValType.Number);
            }
            case ASSIGN_MUL: {
                // 12.6 Multiplicative Operators
                ValType ltype = left.accept(this, mv);
                mv.dup();
                ValType vtype = GetValue(left, ltype, mv);
                // lref lval
                if (right instanceof Literal) {
                    ToNumber(vtype, mv);
                }
                ValType rtype = evalAndGetValue(right, mv);
                if (!(right instanceof Literal)) {
                    mv.swap(ltype, rtype);
                    ToNumber(vtype, mv);
                    mv.swap(rtype, ValType.Number);
                }
                ToNumber(rtype, mv);
                // lref lval rval
                mv.dmul();
                // r lref r
                dupX(node, ltype, ValType.Number, mv);
                mv.toBoxed(ValType.Number);
                PutValue(left, ltype, mv);
                return completion(node, ValType.Number);
            }
            case ASSIGN_DIV: {
                // 12.6 Multiplicative Operators
                ValType ltype = left.accept(this, mv);
                mv.dup();
                ValType vtype = GetValue(left, ltype, mv);
                // lref lval
                if (right instanceof Literal) {
                    ToNumber(vtype, mv);
                }
                ValType rtype = evalAndGetValue(right, mv);
                if (!(right instanceof Literal)) {
                    mv.swap(ltype, rtype);
                    ToNumber(vtype, mv);
                    mv.swap(rtype, ValType.Number);
                }
                ToNumber(rtype, mv);
                // lref lval rval
                mv.ddiv();
                // r lref r
                dupX(node, ltype, ValType.Number, mv);
                mv.toBoxed(ValType.Number);
                PutValue(left, ltype, mv);
                return completion(node, ValType.Number);
            }
            case ASSIGN_MOD: {
                // 12.6 Multiplicative Operators
                ValType ltype = left.accept(this, mv);
                mv.dup();
                ValType vtype = GetValue(left, ltype, mv);
                // lref lval
                if (right instanceof Literal) {
                    ToNumber(vtype, mv);
                }
                ValType rtype = evalAndGetValue(right, mv);
                if (!(right instanceof Literal)) {
                    mv.swap(ltype, rtype);
                    ToNumber(vtype, mv);
                    mv.swap(rtype, ValType.Number);
                }
                ToNumber(rtype, mv);
                // lref lval rval
                mv.drem();
                // r lref r
                dupX(node, ltype, ValType.Number, mv);
                mv.toBoxed(ValType.Number);
                PutValue(left, ltype, mv);
                return completion(node, ValType.Number);
            }
            case ASSIGN_ADD: {
                // 12.7.1 The Addition operator ( + )
                if (right instanceof StringLiteral || right instanceof TemplateLiteral) {
                    assert !(right instanceof TemplateLiteral && ((TemplateLiteral) right)
                            .isTagged());
                    // x += "..."
                    ValType ltype = left.accept(this, mv);
                    mv.dup();
                    ValType vtype = GetValue(left, ltype, mv);
                    toStringForConcat(left, vtype, mv);
                    // lref lval(string)
                    if (!(right instanceof StringLiteral && ((StringLiteral) right).getValue()
                            .isEmpty())) {
                        ValType rtype = right.accept(this, mv);
                        addStrings(ValType.String, rtype, mv);
                    }
                    // r lref r
                    dupX1(node, mv);
                    PutValue(left, ltype, mv);
                    return completion(node, ValType.String);
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
                dupX1(node, mv);
                PutValue(left, ltype, mv);
                return completion(node, ValType.Any);
            }
            case ASSIGN_SUB: {
                // 12.7.2 The Subtraction Operator ( - )
                ValType ltype = left.accept(this, mv);
                mv.dup();
                ValType vtype = GetValue(left, ltype, mv);
                // lref lval
                if (right instanceof Literal) {
                    ToNumber(vtype, mv);
                }
                ValType rtype = evalAndGetValue(right, mv);
                if (!(right instanceof Literal)) {
                    mv.swap(ltype, rtype);
                    ToNumber(vtype, mv);
                    mv.swap(rtype, ValType.Number);
                }
                ToNumber(rtype, mv);
                // lref lval rval
                mv.dsub();
                // r lref r
                dupX(node, ltype, ValType.Number, mv);
                mv.toBoxed(ValType.Number);
                PutValue(left, ltype, mv);
                return completion(node, ValType.Number);
            }
            case ASSIGN_SHL: {
                // 12.8.1 The Left Shift Operator ( << )
                ValType ltype = left.accept(this, mv);
                mv.dup();
                ValType vtype = GetValue(left, ltype, mv);
                // lref lval
                if (right instanceof Literal) {
                    ToInt32(vtype, mv);
                }
                ValType rtype = evalAndGetValue(right, mv);
                if (!(right instanceof Literal)) {
                    mv.swap(ltype, rtype);
                    ToInt32(vtype, mv);
                    mv.swap(rtype, ValType.Number_int);
                }
                ToInt32(rtype, mv); // ToUint32()
                // lref lval rval
                mv.iconst(0x1F);
                mv.iand();
                mv.ishl();
                // r lref r
                dupX(node, ltype, ValType.Number_int, mv);
                mv.toBoxed(ValType.Number_int);
                PutValue(left, ltype, mv);
                return completion(node, ValType.Number_int);
            }
            case ASSIGN_SHR: {
                // 12.8.2 The Signed Right Shift Operator ( >> )
                ValType ltype = left.accept(this, mv);
                mv.dup();
                ValType vtype = GetValue(left, ltype, mv);
                // lref lval
                if (right instanceof Literal) {
                    ToInt32(vtype, mv);
                }
                ValType rtype = evalAndGetValue(right, mv);
                if (!(right instanceof Literal)) {
                    mv.swap(ltype, rtype);
                    ToInt32(vtype, mv);
                    mv.swap(rtype, ValType.Number_int);
                }
                ToInt32(rtype, mv); // ToUint32()
                // lref lval rval
                mv.iconst(0x1F);
                mv.iand();
                mv.ishr();
                // r lref r
                dupX(node, ltype, ValType.Number_int, mv);
                mv.toBoxed(ValType.Number_int);
                PutValue(left, ltype, mv);
                return completion(node, ValType.Number_int);
            }
            case ASSIGN_USHR: {
                // 12.8.3 The Unsigned Right Shift Operator ( >>> )
                ValType ltype = left.accept(this, mv);
                mv.dup();
                ValType vtype = GetValue(left, ltype, mv);
                // lref lval
                if (right instanceof Literal) {
                    ToUint32(vtype, mv);
                }
                ValType rtype = evalAndGetValue(right, mv);
                if (!(right instanceof Literal)) {
                    mv.swap(ltype, rtype);
                    ToUint32(vtype, mv);
                    mv.swap(rtype, ValType.Number_uint);
                }
                ToInt32(rtype, mv); // ToUint32()
                // lref lval rval
                mv.iconst(0x1F);
                mv.iand();
                mv.lushr();
                // r lref r
                dupX(node, ltype, ValType.Number_uint, mv);
                mv.toBoxed(ValType.Number_uint);
                PutValue(left, ltype, mv);
                return completion(node, ValType.Number_uint);
            }
            case ASSIGN_BITAND: {
                // 12.11 Binary Bitwise Operators ( & )
                ValType ltype = left.accept(this, mv);
                mv.dup();
                ValType vtype = GetValue(left, ltype, mv);
                // lref lval
                if (right instanceof Literal) {
                    ToInt32(vtype, mv);
                }
                ValType rtype = evalAndGetValue(right, mv);
                if (!(right instanceof Literal)) {
                    mv.swap(ltype, rtype);
                    ToInt32(vtype, mv);
                    mv.swap(rtype, ValType.Number_int);
                }
                ToInt32(rtype, mv); // ToUint32()
                // lref lval rval
                mv.iand();
                // r lref r
                dupX(node, ltype, ValType.Number_int, mv);
                mv.toBoxed(ValType.Number_int);
                PutValue(left, ltype, mv);
                return completion(node, ValType.Number_int);
            }
            case ASSIGN_BITXOR: {
                // 12.11 Binary Bitwise Operators ( ^ )
                ValType ltype = left.accept(this, mv);
                mv.dup();
                ValType vtype = GetValue(left, ltype, mv);
                // lref lval
                if (right instanceof Literal) {
                    ToInt32(vtype, mv);
                }
                ValType rtype = evalAndGetValue(right, mv);
                if (!(right instanceof Literal)) {
                    mv.swap(ltype, rtype);
                    ToInt32(vtype, mv);
                    mv.swap(rtype, ValType.Number_int);
                }
                ToInt32(rtype, mv); // ToUint32()
                // lref lval rval
                mv.ixor();
                // r lref r
                dupX(node, ltype, ValType.Number_int, mv);
                mv.toBoxed(ValType.Number_int);
                PutValue(left, ltype, mv);
                return completion(node, ValType.Number_int);
            }
            case ASSIGN_BITOR: {
                // 12.11 Binary Bitwise Operators ( | )
                ValType ltype = left.accept(this, mv);
                mv.dup();
                ValType vtype = GetValue(left, ltype, mv);
                // lref lval
                if (right instanceof Literal) {
                    ToInt32(vtype, mv);
                }
                ValType rtype = evalAndGetValue(right, mv);
                if (!(right instanceof Literal)) {
                    mv.swap(ltype, rtype);
                    ToInt32(vtype, mv);
                    mv.swap(rtype, ValType.Number_int);
                }
                ToInt32(rtype, mv); // ToUint32()
                // lref lval rval
                mv.ior();
                // r lref r
                dupX(node, ltype, ValType.Number_int, mv);
                mv.toBoxed(ValType.Number_int);
                PutValue(left, ltype, mv);
                return completion(node, ValType.Number_int);
            }
            case ASSIGN:
            default:
                throw new AssertionError(Objects.toString(node.getOperator(), "<null>"));
            }
        }
    }

    private void dup(AssignmentExpression node, ExpressionVisitor mv) {
        if (node.hasCompletion()) {
            mv.dup();
        }
    }

    private void dupX1(AssignmentExpression node, ExpressionVisitor mv) {
        if (node.hasCompletion()) {
            mv.dupX1();
        }
    }

    private void dupX(AssignmentExpression node, ValType ltype, ValType rtype, ExpressionVisitor mv) {
        if (node.hasCompletion()) {
            mv.dupX(ltype, rtype);
        }
    }

    private ValType completion(AssignmentExpression node, ValType type) {
        return node.hasCompletion() ? type : ValType.Empty;
    }

    @Override
    public ValType visit(AsyncArrowFunction node, ExpressionVisitor mv) {
        codegen.compile(node);

        /* steps 1-4 */
        mv.invoke(codegen.methodDesc(node, FunctionName.RTI));
        mv.loadExecutionContext();
        mv.invoke(Methods.ScriptRuntime_EvaluateAsyncArrowFunction);

        /* step 5 */
        return ValType.Object;
    }

    /**
     * Extension: Async Function Definitions
     */
    @Override
    public ValType visit(AsyncFunctionExpression node, ExpressionVisitor mv) {
        codegen.compile(node);

        /* steps 1-5/10 */
        mv.invoke(codegen.methodDesc(node, FunctionName.RTI));
        mv.loadExecutionContext();
        mv.invoke(Methods.ScriptRuntime_EvaluateAsyncFunctionExpression);

        /* step 6/11 */
        return ValType.Object;
    }

    /**
     * Extension: Async Function Definitions
     */
    @Override
    public ValType visit(AwaitExpression node, ExpressionVisitor mv) {
        Expression expr = node.getExpression();
        evalAndGetBoxedValue(expr, mv);

        yield(node, mv);

        return ValType.Any;
    }

    /**
     * 12.6.2 Runtime Semantics: Evaluation<br>
     * 12.7.2.1 Runtime Semantics: Evaluation<br>
     * 12.7.3.1 Runtime Semantics: Evaluation<br>
     * 12.8.2.1 Runtime Semantics: Evaluation<br>
     * 12.8.3.1 Runtime Semantics: Evaluation<br>
     * 12.8.4.1 Runtime Semantics: Evaluation<br>
     * 12.9.2 Runtime Semantics: Evaluation<br>
     * 12.10.2 Runtime Semantics: Evaluation<br>
     * 12.11.2 Runtime Semantics: Evaluation<br>
     * 12.12.3 Runtime Semantics: Evaluation<br>
     */
    @Override
    public ValType visit(BinaryExpression node, ExpressionVisitor mv) {
        Expression left = node.getLeft();
        Expression right = node.getRight();

        switch (node.getOperator()) {
        case EXP: {
            // Extension: Exponentiation Operator
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
            mv.invoke(Methods.Math_pow);
            return ValType.Number;
        }
        case MUL: {
            // 12.6 Multiplicative Operators
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
            mv.dmul();
            return ValType.Number;
        }
        case DIV: {
            // 12.6 Multiplicative Operators
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
            mv.ddiv();
            return ValType.Number;
        }
        case MOD: {
            // 12.6 Multiplicative Operators
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
            mv.drem();
            return ValType.Number;
        }
        case ADD: {
            // 12.7.1 The Addition operator ( + )
            // Handle 'a' + b + c + ...
            if (left instanceof BinaryExpression && isStringConcat((BinaryExpression) left)
                    && stringConcat(node, mv)) {
                return ValType.String;
            }
            // Handle 'a' + b
            if (left instanceof StringLiteral) {
                if (((StringLiteral) left).getValue().isEmpty()) {
                    // "" + x
                    return evalToString(right, mv);
                }
                // "..." + x
                return addStringLeft(left, right, mv);
            } else if (right instanceof StringLiteral) {
                if (((StringLiteral) right).getValue().isEmpty()) {
                    // x + ""
                    return evalToString(left, mv);
                }
                // x + "..."
                return addStringRight(left, right, mv);
            } else if (left instanceof TemplateLiteral) {
                // `...` + x
                assert !((TemplateLiteral) left).isTagged();
                return addStringLeft(left, right, mv);
            } else if (right instanceof TemplateLiteral) {
                // x + `...`
                assert !((TemplateLiteral) right).isTagged();
                return addStringRight(left, right, mv);
            }

            ValType ltype = evalAndGetValue(left, mv);
            if (ltype == ValType.String) {
                ValType rtype = evalAndGetValue(right, mv);
                toStringForConcat(right, rtype, mv);
                addStrings(ltype, ValType.String, mv);
                return ValType.String;
            }
            if (ltype.isNumeric()) {
                ValType expected = expressionType(right);
                if (expected.isPrimitive() && expected != ValType.String) {
                    ToNumber(ltype, mv);
                    ValType rtype = evalAndGetValue(right, mv);
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
                ValType rtype = evalAndGetValue(right, mv);
                assert rtype == ValType.String;
                if (!ltype.isPrimitive()) {
                    mv.swap(ValType.Any, rtype);
                    toStringForConcat(left, ltype, mv);
                    mv.swap(rtype, ValType.String);
                }
                addStrings(ValType.String, rtype, mv);
                return ValType.String;
            }
            mv.toBoxed(ltype);
            ValType rtype = evalAndGetValue(right, mv);
            if (rtype == ValType.String) {
                mv.swap(ValType.Any, rtype);
                if (ltype.isPrimitive()) {
                    mv.toUnboxed(ltype);
                }
                toStringForConcat(left, ltype, mv);
                mv.swap(rtype, ValType.String);
                addStrings(ValType.String, rtype, mv);
                return ValType.String;
            }
            mv.toBoxed(rtype);

            mv.loadExecutionContext();
            invokeDynamicOperator(node.getOperator(), mv);

            return ValType.Any;
        }
        case SUB: {
            // 12.7.2 The Subtraction Operator ( - )
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
            mv.dsub();
            return ValType.Number;
        }
        case SHL: {
            // 12.8.1 The Left Shift Operator ( << )
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
            mv.iand();
            mv.ishl();
            return ValType.Number_int;
        }
        case SHR: {
            // 12.8.2 The Signed Right Shift Operator ( >> )
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
            mv.iand();
            mv.ishr();
            return ValType.Number_int;
        }
        case USHR: {
            // 12.8.3 The Unsigned Right Shift Operator ( >>> )
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
            mv.iand();
            mv.lushr();
            return ValType.Number_uint;
        }
        case LT: {
            // 12.9 Relational Operators ( < )
            evalAndGetBoxedValue(left, mv);
            evalAndGetBoxedValue(right, mv);

            mv.loadExecutionContext();
            invokeDynamicOperator(node.getOperator(), mv);
            return ValType.Boolean;
        }
        case GT: {
            // 12.9 Relational Operators ( > )
            evalAndGetBoxedValue(left, mv);
            evalAndGetBoxedValue(right, mv);
            mv.swap();

            mv.loadExecutionContext();
            invokeDynamicOperator(node.getOperator(), mv);
            return ValType.Boolean;
        }
        case LE: {
            // 12.9 Relational Operators ( <= )
            evalAndGetBoxedValue(left, mv);
            evalAndGetBoxedValue(right, mv);
            mv.swap();

            mv.loadExecutionContext();
            invokeDynamicOperator(node.getOperator(), mv);
            return ValType.Boolean;
        }
        case GE: {
            // 12.9 Relational Operators ( >= )
            evalAndGetBoxedValue(left, mv);
            evalAndGetBoxedValue(right, mv);

            mv.loadExecutionContext();
            invokeDynamicOperator(node.getOperator(), mv);
            return ValType.Boolean;
        }
        case INSTANCEOF: {
            // 12.9 Relational Operators ( instanceof )
            evalAndGetBoxedValue(left, mv);
            evalAndGetBoxedValue(right, mv);

            mv.loadExecutionContext();
            mv.lineInfo(node);
            mv.invoke(Methods.ScriptRuntime_InstanceofOperator);
            return ValType.Boolean;
        }
        case IN: {
            // 12.9 Relational Operators ( in )
            evalAndGetBoxedValue(left, mv);
            evalAndGetBoxedValue(right, mv);

            mv.loadExecutionContext();
            mv.lineInfo(node);
            mv.invoke(Methods.ScriptRuntime_in);
            return ValType.Boolean;
        }
        case EQ: {
            // 12.10 Equality Operators ( == )
            evalAndGetBoxedValue(left, mv);
            evalAndGetBoxedValue(right, mv);

            mv.loadExecutionContext();
            invokeDynamicOperator(node.getOperator(), mv);

            return ValType.Boolean;
        }
        case NE: {
            // 12.10 Equality Operators ( != )
            evalAndGetBoxedValue(left, mv);
            evalAndGetBoxedValue(right, mv);

            mv.loadExecutionContext();
            invokeDynamicOperator(node.getOperator(), mv);

            mv.not();
            return ValType.Boolean;
        }
        case SHEQ: {
            // 12.10 Equality Operators ( === )
            evalAndGetBoxedValue(left, mv);
            evalAndGetBoxedValue(right, mv);

            invokeDynamicOperator(node.getOperator(), mv);

            return ValType.Boolean;
        }
        case SHNE: {
            // 12.10 Equality Operators ( !== )
            evalAndGetBoxedValue(left, mv);
            evalAndGetBoxedValue(right, mv);

            invokeDynamicOperator(node.getOperator(), mv);

            mv.not();
            return ValType.Boolean;
        }
        case BITAND: {
            // 12.11 Binary Bitwise Operators ( & )
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
            mv.iand();
            return ValType.Number_int;
        }
        case BITXOR: {
            // 12.11 Binary Bitwise Operators ( ^ )
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
            mv.ixor();
            return ValType.Number_int;
        }
        case BITOR: {
            // 12.11 Binary Bitwise Operators ( | )
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
            mv.ior();
            return ValType.Number_int;
        }

        case AND:
        case OR: {
            // 12.12 Binary Logical Operators
            Jump after = new Jump();

            ValType ltype = evalAndGetValue(left, mv);
            if (ltype == ValType.Boolean && expressionType(right) == ValType.Boolean) {
                mv.dup();
                if (node.getOperator() == BinaryExpression.Operator.AND) {
                    mv.ifeq(after);
                } else {
                    mv.ifne(after);
                }
                mv.pop();
                ValType rtype = evalAndGetValue(right, mv);
                assert rtype == ValType.Boolean;
                mv.mark(after);
                return ValType.Boolean;
            }
            mv.toBoxed(ltype);
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
            throw new AssertionError(Objects.toString(node.getOperator(), "<null>"));
        }
    }

    private static final int MAX_JVM_ARGUMENTS = 255;
    private static final int MAX_DYN_ARGUMENTS = 127;
    static {
        // JVM max-arguments is fixed to 255, invokedynamic can also receive up to 255 arguments,
        // but for now restrict to the half.
        assert MAX_DYN_ARGUMENTS < MAX_JVM_ARGUMENTS;
    }

    private boolean stringConcat(BinaryExpression binary, ExpressionVisitor mv) {
        int strings = countStringConcats(binary);
        if (strings > MAX_DYN_ARGUMENTS) {
            return false;
        }
        if (strings == 0) {
            mv.aconst("");
        } else if (strings == 1) {
            stringConcatWith(binary, mv);
        } else {
            mv.loadExecutionContext();
            stringConcatWith(binary, mv);
            invokeDynamicConcat(strings, mv);
        }
        return true;
    }

    private ValType stringConcatWith(Expression node, ExpressionVisitor mv) {
        if (node instanceof StringLiteral) {
            if (!((StringLiteral) node).getValue().isEmpty()) {
                node.accept(this, mv);
            }
            return ValType.String;
        } else if (node instanceof TemplateLiteral) {
            node.accept(this, mv);
            return ValType.String;
        } else if (node instanceof BinaryExpression && isStringConcat((BinaryExpression) node)) {
            Expression left = ((BinaryExpression) node).getLeft();
            Expression right = ((BinaryExpression) node).getRight();

            ValType ltype = stringConcatWith(left, mv);
            if (ltype.isPrimitive() || right instanceof Literal) {
                toStringForConcat(left, ltype, mv);
            }
            ValType rtype = stringConcatWith(right, mv);
            assert ltype == ValType.String || rtype == ValType.String;
            if (!(ltype.isPrimitive() || right instanceof Literal)) {
                mv.swap(ltype, rtype);
                toStringForConcat(left, ltype, mv);
                mv.swap(rtype, ValType.String);
            }
            toStringForConcat(right, rtype, mv);
            return ValType.String;
        } else {
            return evalAndGetValue(node, mv);
        }
    }

    private int countStringConcats(Expression node) {
        if (node instanceof StringLiteral && ((StringLiteral) node).getValue().isEmpty()) {
            return 0;
        } else if (node instanceof BinaryExpression && isStringConcat((BinaryExpression) node)) {
            return countStringConcats(((BinaryExpression) node).getLeft())
                    + countStringConcats(((BinaryExpression) node).getRight());
        }
        return 1;
    }

    private boolean isStringConcat(BinaryExpression binary) {
        if (binary.getOperator() != BinaryExpression.Operator.ADD) {
            return false;
        }
        Expression left = binary.getLeft();
        Expression right = binary.getRight();
        if (left instanceof StringLiteral || left instanceof TemplateLiteral) {
            return true;
        }
        if (right instanceof StringLiteral || right instanceof TemplateLiteral) {
            return true;
        }
        if (left instanceof BinaryExpression && isStringConcat((BinaryExpression) left)) {
            return true;
        }
        return false;
    }

    private ValType toStringForConcat(Expression node, ValType type, ExpressionVisitor mv) {
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

    private ValType evalToString(Expression node, ExpressionVisitor mv) {
        ValType type = evalAndGetValue(node, mv);
        return toStringForConcat(node, type, mv);
    }

    private ValType addStringLeft(Expression left, Expression right, ExpressionVisitor mv) {
        ValType ltype = left.accept(this, mv);
        ValType rtype = evalToString(right, mv);
        return addStrings(ltype, rtype, mv);
    }

    private ValType addStringRight(Expression left, Expression right, ExpressionVisitor mv) {
        ValType ltype = evalToString(left, mv);
        ValType rtype = right.accept(this, mv);
        return addStrings(ltype, rtype, mv);
    }

    private ValType addStrings(ValType left, ValType right, ExpressionVisitor mv) {
        assert left == ValType.String && right == ValType.String;
        mv.loadExecutionContext();
        mv.invoke(Methods.ScriptRuntime_add_str);
        return ValType.String;
    }

    private static ValType expressionType(Expression node) {
        if (node instanceof Literal) {
            if (node instanceof NullLiteral) {
                return ValType.Null;
            } else {
                assert node instanceof ValueLiteral;
                if (node instanceof NumericLiteral) {
                    return ((NumericLiteral) node).isInt() ? ValType.Number_int : ValType.Number;
                }
                if (node instanceof StringLiteral) {
                    return ValType.String;
                }
                assert node instanceof BooleanLiteral;
                return ValType.Boolean;
            }
        }
        if (node instanceof BinaryExpression) {
            BinaryExpression binary = (BinaryExpression) node;
            switch (binary.getOperator()) {
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
            case ADD:
                // Pessimistically assume any-type
                return ValType.Any;
            case AND:
            case OR:
                return expressionType(binary.getLeft()) == ValType.Boolean
                        && expressionType(binary.getRight()) == ValType.Boolean ? ValType.Boolean
                        : ValType.Any;
            default:
                throw new AssertionError();
            }
        }
        if (node instanceof UnaryExpression) {
            UnaryExpression unary = (UnaryExpression) node;
            switch (unary.getOperator()) {
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
        if (node instanceof ConditionalExpression) {
            ConditionalExpression conditional = (ConditionalExpression) node;
            ValType ltype = expressionType(conditional.getThen());
            ValType rtype = expressionType(conditional.getOtherwise());
            if (ltype != rtype && (ltype.isNumeric() && rtype.isNumeric())) {
                return ValType.Number;
            }
            return ltype == rtype ? ltype : ValType.Any;
        }
        if (node instanceof TemplateLiteral) {
            return ValType.String;
        }
        return ValType.Any;
    }

    /**
     * 12.2.3.1 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(BooleanLiteral node, ExpressionVisitor mv) {
        mv.iconst(node.getValue());

        return ValType.Boolean;
    }

    /**
     * 12.3.4.1 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(CallExpression node, ExpressionVisitor mv) {
        return EvaluateCall(node, node.getBase(), node.getArguments(), mv);
    }

    /**
     * 12.3.6.1 Runtime Semantics: ArgumentListEvaluation
     */
    @Override
    public ValType visit(CallSpreadElement node, ExpressionVisitor mv) {
        evalAndGetBoxedValue(node.getExpression(), mv);
        mv.loadExecutionContext();
        mv.lineInfo(node);
        mv.invoke(Methods.ScriptRuntime_SpreadArray);

        return ValType.Any; // actually Object[]
    }

    /**
     * 14.5.16 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(ClassExpression node, ExpressionVisitor mv) {
        /* steps 1-2 */
        String className = node.getIdentifier() != null ? node.getIdentifier().getName()
                .getIdentifier() : null;
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
    public ValType visit(CommaExpression node, ExpressionVisitor mv) {
        assert !node.getOperands().isEmpty() : "empty comma expression";
        int count = node.getOperands().size();
        for (Expression e : node.getOperands()) {
            if (--count == 0) {
                return evalAndGetValue(e, mv);
            }
            ValType type = evalAndGetValue(e.emptyCompletion(), mv);
            mv.pop(type);
        }
        return null;
    }

    /**
     * 12.13.3 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(ConditionalExpression node, ExpressionVisitor mv) {
        Jump l0 = new Jump(), l1 = new Jump();

        /* steps 1-2 */
        ValType typeTest = evalAndGetValue(node.getTest(), mv);
        /* step 2 */
        ToBoolean(typeTest, mv);
        /* step 3 */
        mv.ifeq(l0);
        ValType typeThen = evalAndGetValue(node.getThen(), mv);
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
                ValType typeOtherwise = evalAndGetValue(node.getOtherwise(), mv);
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
        ValType typeOtherwise = evalAndGetValue(node.getOtherwise(), mv);
        mv.toBoxed(typeOtherwise);
        mv.mark(l1);

        return typeThen == typeOtherwise ? typeThen : ValType.Any;
    }

    /**
     * 12.3.2.1 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(ElementAccessor node, ExpressionVisitor mv) {
        /* steps 1-3 */
        evalAndGetBoxedValue(node.getBase(), mv);
        /* steps 4-6 */
        ValType elementType = toPropertyKey(node, evalAndGetValue(node.getElement(), mv), mv);
        /* steps 7-11 */
        mv.loadExecutionContext();
        mv.iconst(mv.isStrict());
        mv.lineInfo(node);
        mv.invoke(elementMethod(elementType));
        /* step 11 */
        return ValType.Reference;
    }

    /**
     * 12.3.2.1 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(ElementAccessorValue node, ExpressionVisitor mv) {
        /* steps 1-3 */
        evalAndGetBoxedValue(node.getBase(), mv);
        /* steps 4-6 */
        ValType elementType = toPropertyKey(node, evalAndGetValue(node.getElement(), mv), mv);
        /* steps 7-11 */
        mv.loadExecutionContext();
        mv.lineInfo(node);
        mv.invoke(elementValueMethod(elementType));
        /* step 11 */
        return ValType.Any;
    }

    private ValType toPropertyKey(ElementAccessor node, ValType elementType, ExpressionVisitor mv) {
        switch (elementType) {
        case String:
            if (node.getElement() instanceof StringLiteral) {
                return elementType;
            }
            // fall-thru if string is not flat
        case Boolean:
        case Null:
        case Undefined:
            ToFlatString(elementType, mv);
            return ValType.String;
        default:
            return elementType;
        }
    }

    private static MethodName elementMethod(ValType elementType) {
        switch (elementType) {
        case Number:
            return Methods.ScriptRuntime_getProperty_double;
        case Number_int:
            return Methods.ScriptRuntime_getProperty_int;
        case Number_uint:
            return Methods.ScriptRuntime_getProperty_long;
        case String:
            return Methods.ScriptRuntime_getProperty;
        case Any:
        case Object:
            return Methods.ScriptRuntime_getElement;
        default:
            throw new AssertionError();
        }
    }

    private static MethodName elementValueMethod(ValType elementType) {
        switch (elementType) {
        case Number:
            return Methods.ScriptRuntime_getPropertyValue_double;
        case Number_int:
            return Methods.ScriptRuntime_getPropertyValue_int;
        case Number_uint:
            return Methods.ScriptRuntime_getPropertyValue_long;
        case String:
            return Methods.ScriptRuntime_getPropertyValue;
        case Any:
        case Object:
            return Methods.ScriptRuntime_getElementValue;
        default:
            throw new AssertionError();
        }
    }

    @Override
    public ValType visit(EmptyExpression node, ExpressionVisitor mv) {
        return ValType.Empty;
    }

    @Override
    public ValType visit(ExpressionMethod node, ExpressionVisitor mv) {
        codegen.compile(node, mv);

        mv.loadExecutionContext();
        mv.invoke(codegen.methodDesc(node));

        return ValType.Any;
    }

    /**
     * 14.1.17 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(FunctionExpression node, ExpressionVisitor mv) {
        codegen.compile(node);

        /* steps 1-5/10 */
        mv.invoke(codegen.methodDesc(node, FunctionName.RTI));
        mv.loadExecutionContext();
        mv.invoke(Methods.ScriptRuntime_EvaluateFunctionExpression);

        /* step 6/11 */
        return ValType.Object;
    }

    /**
     * 12.2.7.2 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(GeneratorComprehension node, ExpressionVisitor mv) {
        codegen.compile(node);

        /* steps 1-8 */
        mv.invoke(codegen.methodDesc(node, FunctionName.RTI));
        mv.loadExecutionContext();
        if (!(node.getComprehension() instanceof LegacyComprehension)) {
            mv.invoke(Methods.ScriptRuntime_EvaluateGeneratorComprehension);
        } else {
            mv.invoke(Methods.ScriptRuntime_EvaluateLegacyGeneratorComprehension);
        }

        /* step 9 */
        return ValType.Object;
    }

    /**
     * 14.4.14 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(GeneratorExpression node, ExpressionVisitor mv) {
        codegen.compile(node);

        /* steps 1-7/11 */
        mv.invoke(codegen.methodDesc(node, FunctionName.RTI));
        mv.loadExecutionContext();
        mv.invoke(Methods.ScriptRuntime_EvaluateGeneratorExpression);

        /* step 8/12 */
        return ValType.Object;
    }

    /**
     * 14.4.14 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(LegacyGeneratorExpression node, ExpressionVisitor mv) {
        codegen.compile(node);

        /* steps 1-7 */
        mv.invoke(codegen.methodDesc(node, FunctionName.RTI));
        mv.loadExecutionContext();
        mv.invoke(Methods.ScriptRuntime_EvaluateLegacyGeneratorExpression);

        /* step 8 */
        return ValType.Object;
    }

    /**
     * 12.2.2.1 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(IdentifierReference node, ExpressionVisitor mv) {
        /* steps 1-2 */
        return identifierResolution.resolve(node, mv);
    }

    /**
     * 12.2.2.1 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(IdentifierReferenceValue node, ExpressionVisitor mv) {
        /* steps 1-2 */
        return identifierResolution.resolveValue(node, mv);
    }

    /**
     * Extension: 'let' expression
     */
    @Override
    public ValType visit(LetExpression node, ExpressionVisitor mv) {
        if (node.getScope().isPresent()) {
            // stack: [] -> [env]
            newDeclarativeEnvironment(mv);
            // stack: [env] -> [env, envRec]
            mv.dup();
            mv.invoke(Methods.LexicalEnvironment_getEnvRec);

            // stack: [env, envRec] -> [env]
            for (LexicalBinding binding : node.getBindings()) {
                // stack: [env, envRec] -> [env, envRec, envRec]
                mv.dup();

                // stack: [env, envRec, envRec] -> [env, envRec, envRec]
                for (Name name : BoundNames(binding.getBinding())) {
                    createMutableBinding(name, false, mv);
                }

                Expression initializer = binding.getInitializer();
                if (initializer != null) {
                    evalAndGetBoxedValue(initializer, mv);
                } else {
                    assert binding.getBinding() instanceof BindingIdentifier;
                    mv.loadUndefined();
                }

                // stack: [env, envRec, envRec, value] -> [env, envRec]
                BindingInitializationWithEnvironment(binding.getBinding(), mv);
            }
            mv.pop();
            // stack: [env] -> []
            pushLexicalEnvironment(mv);
        }

        mv.enterScope(node);
        ValType type = evalAndGetValue(node.getExpression(), mv);
        mv.exitScope();

        if (node.getScope().isPresent()) {
            popLexicalEnvironment(mv);
        }

        return type;
    }

    @Override
    public ValType visit(NativeCallExpression node, ExpressionVisitor mv) {
        String nativeName = node.getBase().getName();
        ArgumentListEvaluation(node, node.getArguments(), mv);
        mv.loadExecutionContext();
        mv.lineInfo(node);
        invokeDynamicNativeCall(nativeName, mv);
        return ValType.Any;
    }

    /**
     * 12.3.3.1 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(NewExpression node, ExpressionVisitor mv) {
        /* step 1 */
        return EvaluateNew(node, mv);
    }

    /**
     * 12.3.8.1 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(NewTarget node, ExpressionVisitor mv) {
        mv.loadExecutionContext();
        mv.invoke(Methods.ScriptRuntime_GetNewTargetOrUndefined);
        return ValType.Any;
    }

    /**
     * 12.2.3.1 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(NullLiteral node, ExpressionVisitor mv) {
        /* step 1 */
        mv.loadNull();
        return ValType.Null;
    }

    /**
     * 12.2.3.1 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(NumericLiteral node, ExpressionVisitor mv) {
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
     * 12.2.5.6 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(ObjectLiteral node, ExpressionVisitor mv) {
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
            mv.invoke(Methods.ScriptRuntime_EvaluateMethodDecorators);
            mv.exitVariableScope();
        }
        /* step 4 */
        return ValType.Object;
    }

    /**
     * 12.3.2.1 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(PropertyAccessor node, ExpressionVisitor mv) {
        /* steps 1-3 */
        evalAndGetBoxedValue(node.getBase(), mv);
        /* steps 4-6 */
        mv.aconst(node.getName());
        /* steps 7-11 */
        mv.loadExecutionContext();
        mv.iconst(mv.isStrict());
        mv.lineInfo(node);
        mv.invoke(Methods.ScriptRuntime_getProperty);
        /* step 11 */
        return ValType.Reference;
    }

    /**
     * 12.3.2.1 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(PropertyAccessorValue node, ExpressionVisitor mv) {
        /* steps 1-3 */
        evalAndGetBoxedValue(node.getBase(), mv);
        /* steps 4-6 */
        mv.aconst(node.getName());
        /* steps 7-11 */
        mv.loadExecutionContext();
        mv.lineInfo(node);
        mv.invoke(Methods.ScriptRuntime_getPropertyValue);
        /* step 11 */
        return ValType.Any;
    }

    /**
     * 12.2.8.2 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(RegularExpressionLiteral node, ExpressionVisitor mv) {
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
     * 12.2.3.1 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(StringLiteral node, ExpressionVisitor mv) {
        /* step 1 */
        mv.aconst(node.getValue());
        return ValType.String;
    }

    /**
     * 12.3.5.1 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(SuperCallExpression node, ExpressionVisitor mv) {
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
    public ValType visit(SuperElementAccessor node, ExpressionVisitor mv) {
        /* steps 1-5 */
        mv.loadExecutionContext();
        ValType type = evalAndGetValue(node.getExpression(), mv);
        ToPropertyKey(type, mv);
        mv.iconst(mv.isStrict());
        mv.lineInfo(node);
        mv.invoke(Methods.ScriptRuntime_MakeSuperPropertyReference);

        return ValType.Reference;
    }

    /**
     * 12.3.5.1 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(SuperElementAccessorValue node, ExpressionVisitor mv) {
        /* steps 1-5 */
        mv.loadExecutionContext();
        ValType type = evalAndGetValue(node.getExpression(), mv);
        ToPropertyKey(type, mv);
        mv.iconst(mv.isStrict());
        mv.lineInfo(node);
        mv.invoke(Methods.ScriptRuntime_getSuperPropertyReferenceValue);

        return ValType.Any;
    }

    /**
     * 12.3.5.1 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(SuperNewExpression node, ExpressionVisitor mv) {
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
    public ValType visit(SuperPropertyAccessor node, ExpressionVisitor mv) {
        /* steps 1-3 */
        mv.loadExecutionContext();
        mv.aconst(node.getName());
        mv.iconst(mv.isStrict());
        mv.lineInfo(node);
        mv.invoke(Methods.ScriptRuntime_MakeSuperPropertyReference_String);

        return ValType.Reference;
    }

    /**
     * 12.3.5.1 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(SuperPropertyAccessorValue node, ExpressionVisitor mv) {
        /* steps 1-3 */
        mv.loadExecutionContext();
        mv.aconst(node.getName());
        mv.iconst(mv.isStrict());
        mv.lineInfo(node);
        mv.invoke(Methods.ScriptRuntime_getSuperPropertyReferenceValue_String);

        return ValType.Any;
    }

    /**
     * 12.3.7.1 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(TemplateCallExpression node, ExpressionVisitor mv) {
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
     * 12.2.9.2.4 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(TemplateLiteral node, ExpressionVisitor mv) {
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
                    ValType type = evalAndGetValue(expr, mv);
                    ToString(type, mv);
                    mv.invoke(Methods.StringBuilder_append_Charsequence);
                }
            }

            mv.invoke(Methods.StringBuilder_toString);
        }

        return ValType.String;
    }

    /**
     * 12.2.1.1 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(ThisExpression node, ExpressionVisitor mv) {
        /* step 1 */
        mv.loadExecutionContext();
        mv.lineInfo(node);
        mv.invoke(Methods.ExecutionContext_resolveThisBinding);
        return ValType.Any;
    }

    /**
     * 12.4.3.1 Runtime Semantics: Evaluation<br>
     * 12.4.3.1 Runtime Semantics: Evaluation<br>
     * 12.5.3.2 Runtime Semantics: Evaluation<br>
     * 12.5.4.1 Runtime Semantics: Evaluation<br>
     * 12.5.5.1 Runtime Semantics: Evaluation<br>
     * 12.5.6.1 Runtime Semantics: Evaluation<br>
     * 12.5.7.1 Runtime Semantics: Evaluation<br>
     * 12.5.8.1 Runtime Semantics: Evaluation<br>
     * 12.5.9.1 Runtime Semantics: Evaluation<br>
     * 12.5.10.1 Runtime Semantics: Evaluation<br>
     * 12.5.11.1 Runtime Semantics: Evaluation<br>
     */
    @Override
    public ValType visit(UnaryExpression node, ExpressionVisitor mv) {
        switch (node.getOperator()) {
        case POST_INC: {
            // 12.4.3 Postfix Increment Operator
            assert node.getOperand() instanceof LeftHandSideExpression;
            LeftHandSideExpression expr = (LeftHandSideExpression) node.getOperand();
            ValType type = expr.accept(this, mv);
            mv.dup();
            ValType vtype = GetValue(expr, type, mv);
            ToNumber(vtype, mv);
            dupX(node, type, ValType.Number, mv);
            mv.dconst(1d);
            mv.dadd();
            mv.toBoxed(ValType.Number);
            PutValue(expr, type, mv);
            return completion(node, ValType.Number);
        }
        case POST_DEC: {
            // 12.4.4 Postfix Decrement Operator
            assert node.getOperand() instanceof LeftHandSideExpression;
            LeftHandSideExpression expr = (LeftHandSideExpression) node.getOperand();
            ValType type = expr.accept(this, mv);
            mv.dup();
            ValType vtype = GetValue(expr, type, mv);
            ToNumber(vtype, mv);
            dupX(node, type, ValType.Number, mv);
            mv.dconst(1d);
            mv.dsub();
            mv.toBoxed(ValType.Number);
            PutValue(expr, type, mv);
            return completion(node, ValType.Number);
        }
        case DELETE: {
            // 12.5.3 The delete Operator
            Expression expr = node.getOperand().emptyCompletion();
            ValType type = expr.accept(this, mv);
            if (type != ValType.Reference) {
                mv.pop(type);
                mv.iconst(true);
            } else {
                mv.loadExecutionContext();
                mv.lineInfo(node);
                mv.invoke(Methods.Reference_delete);
            }
            return ValType.Boolean;
        }
        case VOID: {
            // 12.5.4 The void Operator
            Expression expr = node.getOperand().emptyCompletion();
            ValType type = evalAndGetValue(expr, mv);
            mv.pop(type);
            mv.loadUndefined();
            return ValType.Undefined;
        }
        case TYPEOF: {
            // 12.5.5 The typeof Operator
            Expression expr = node.getOperand();
            ValType type = expr.accept(this, mv);
            mv.toBoxed(type);
            mv.loadExecutionContext();
            mv.lineInfo(node);
            mv.invoke(Methods.ScriptRuntime_typeof);
            return ValType.String;
        }
        case PRE_INC: {
            // 12.5.6 Prefix Increment Operator
            assert node.getOperand() instanceof LeftHandSideExpression;
            LeftHandSideExpression expr = (LeftHandSideExpression) node.getOperand();
            ValType type = expr.accept(this, mv);
            mv.dup();
            ValType vtype = GetValue(expr, type, mv);
            ToNumber(vtype, mv);
            mv.dconst(1d);
            mv.dadd();
            dupX(node, type, ValType.Number, mv);
            mv.toBoxed(ValType.Number);
            PutValue(expr, type, mv);
            return completion(node, ValType.Number);
        }
        case PRE_DEC: {
            // 12.5.7 Prefix Decrement Operator
            assert node.getOperand() instanceof LeftHandSideExpression;
            LeftHandSideExpression expr = (LeftHandSideExpression) node.getOperand();
            ValType type = expr.accept(this, mv);
            mv.dup();
            ValType vtype = GetValue(expr, type, mv);
            ToNumber(vtype, mv);
            mv.dconst(1d);
            mv.dsub();
            dupX(node, type, ValType.Number, mv);
            mv.toBoxed(ValType.Number);
            PutValue(expr, type, mv);
            return completion(node, ValType.Number);
        }
        case POS: {
            // 12.5.8 Unary + Operator
            Expression expr = node.getOperand();
            ValType type = evalAndGetValue(expr, mv);
            ToNumber(type, mv);
            return ValType.Number;
        }
        case NEG: {
            // 12.5.9 Unary - Operator
            Expression expr = node.getOperand();
            ValType type = evalAndGetValue(expr, mv);
            ToNumber(type, mv);
            mv.dneg();
            return ValType.Number;
        }
        case BITNOT: {
            // 12.5.10 Bitwise NOT Operator ( ~ )
            Expression expr = node.getOperand();
            ValType type = evalAndGetValue(expr, mv);
            ToInt32(type, mv);
            mv.bitnot();
            return ValType.Number_int;
        }
        case NOT: {
            // 12.5.11 Logical NOT Operator ( ! )
            Expression expr = node.getOperand();
            ValType type = evalAndGetValue(expr, mv);
            ToBoolean(type, mv);
            mv.not();
            return ValType.Boolean;
        }
        default:
            throw new AssertionError(Objects.toString(node.getOperator(), "<null>"));
        }
    }

    private void dupX(UnaryExpression node, ValType ltype, ValType rtype, ExpressionVisitor mv) {
        if (node.hasCompletion()) {
            mv.dupX(ltype, rtype);
        }
    }

    private ValType completion(UnaryExpression node, ValType type) {
        return node.hasCompletion() ? type : ValType.Empty;
    }

    /**
     * 14.4.14 Runtime Semantics: Evaluation
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
