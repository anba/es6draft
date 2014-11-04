/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static com.github.anba.es6draft.compiler.ArrayComprehensionGenerator.EvaluateArrayComprehension;
import static com.github.anba.es6draft.semantics.StaticSemantics.BoundNames;
import static com.github.anba.es6draft.semantics.StaticSemantics.IsAnonymousFunctionDefinition;
import static com.github.anba.es6draft.semantics.StaticSemantics.IsIdentifierRef;
import static com.github.anba.es6draft.semantics.StaticSemantics.Substitutions;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.objectweb.asm.Type;

import com.github.anba.es6draft.ast.*;
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
import com.github.anba.es6draft.compiler.assembler.FieldDesc;
import com.github.anba.es6draft.compiler.assembler.Jump;
import com.github.anba.es6draft.compiler.assembler.MethodDesc;
import com.github.anba.es6draft.runtime.internal.Bootstrap;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.NativeCalls;
import com.github.anba.es6draft.runtime.objects.Eval.EvalFlags;

/**
 *
 */
final class ExpressionGenerator extends DefaultCodeGenerator<ValType, ExpressionVisitor> {
    private static final class Fields {
        static final FieldDesc Intrinsics_ObjectPrototype = FieldDesc.create(
                FieldDesc.Allocation.Static, Types.Intrinsics, "ObjectPrototype", Types.Intrinsics);

        static final FieldDesc ScriptRuntime_EMPTY_ARRAY = FieldDesc.create(
                FieldDesc.Allocation.Static, Types.ScriptRuntime, "EMPTY_ARRAY", Types.Object_);
    }

    private static final class Methods {
        // class: Eval
        static final MethodDesc Eval_directEvalWithTranslate = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.Eval, "directEval", Type.getMethodType(
                        Types.Object, Types.Object_, Types.ExecutionContext, Type.INT_TYPE));

        static final MethodDesc Eval_directEval = MethodDesc.create(MethodDesc.Invoke.Static,
                Types.Eval, "directEval", Type.getMethodType(Types.Object, Types.Object,
                        Types.ExecutionContext, Type.INT_TYPE));

        // class: EnvironmentRecord
        static final MethodDesc EnvironmentRecord_createMutableBinding = MethodDesc.create(
                MethodDesc.Invoke.Interface, Types.EnvironmentRecord, "createMutableBinding",
                Type.getMethodType(Type.VOID_TYPE, Types.String, Type.BOOLEAN_TYPE));

        static final MethodDesc EnvironmentRecord_withBaseObject = MethodDesc.create(
                MethodDesc.Invoke.Interface, Types.EnvironmentRecord, "withBaseObject",
                Type.getMethodType(Types.ScriptObject));

        // class: ExecutionContext
        static final MethodDesc ExecutionContext_resolveThisBinding = MethodDesc.create(
                MethodDesc.Invoke.Virtual, Types.ExecutionContext, "resolveThisBinding",
                Type.getMethodType(Types.Object));

        // class: ArrayObject
        static final MethodDesc ArrayObject_ArrayCreate = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.ArrayObject, "ArrayCreate",
                Type.getMethodType(Types.ArrayObject, Types.ExecutionContext, Type.LONG_TYPE));

        static final MethodDesc ArrayObject_DenseArrayCreate = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.ArrayObject, "DenseArrayCreate",
                Type.getMethodType(Types.ArrayObject, Types.ExecutionContext, Types.Object_));

        static final MethodDesc ArrayObject_SparseArrayCreate = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.ArrayObject, "SparseArrayCreate",
                Type.getMethodType(Types.ArrayObject, Types.ExecutionContext, Types.Object_));

        // class: LexicalEnvironment
        static final MethodDesc LexicalEnvironment_getEnvRec = MethodDesc.create(
                MethodDesc.Invoke.Virtual, Types.LexicalEnvironment, "getEnvRec",
                Type.getMethodType(Types.EnvironmentRecord));

        // class: Math
        static final MethodDesc Math_pow = MethodDesc.create(MethodDesc.Invoke.Static, Types.Math,
                "pow", Type.getMethodType(Type.DOUBLE_TYPE, Type.DOUBLE_TYPE, Type.DOUBLE_TYPE));

        // class: OrdinaryObject
        static final MethodDesc OrdinaryObject_ObjectCreate = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.OrdinaryObject, "ObjectCreate",
                Type.getMethodType(Types.OrdinaryObject, Types.ExecutionContext, Types.Intrinsics));

        // class: Reference
        static final MethodDesc Reference_getBase = MethodDesc.create(MethodDesc.Invoke.Virtual,
                Types.Reference, "getBase", Type.getMethodType(Types.Object));

        static final MethodDesc Reference_getThisValue = MethodDesc.create(
                MethodDesc.Invoke.Virtual, Types.Reference, "getThisValue",
                Type.getMethodType(Types.Object, Types.ExecutionContext));

        static final MethodDesc Reference_getValue = MethodDesc.create(MethodDesc.Invoke.Virtual,
                Types.Reference, "getValue",
                Type.getMethodType(Types.Object, Types.ExecutionContext));

        static final MethodDesc Reference_putValue = MethodDesc.create(MethodDesc.Invoke.Virtual,
                Types.Reference, "putValue",
                Type.getMethodType(Type.VOID_TYPE, Types.Object, Types.ExecutionContext));

        static final MethodDesc Reference_delete = MethodDesc.create(MethodDesc.Invoke.Virtual,
                Types.Reference, "delete",
                Type.getMethodType(Type.BOOLEAN_TYPE, Types.ExecutionContext));

        // class: RegExpConstructor
        static final MethodDesc RegExpConstructor_RegExpCreate = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.RegExpConstructor, "RegExpCreate", Type
                        .getMethodType(Types.RegExpObject, Types.ExecutionContext, Types.Object,
                                Types.Object));

        // class: ScriptRuntime
        static final MethodDesc ScriptRuntime_add_str = MethodDesc.create(MethodDesc.Invoke.Static,
                Types.ScriptRuntime, "add", Type.getMethodType(Types.CharSequence,
                        Types.CharSequence, Types.CharSequence, Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_in = MethodDesc.create(MethodDesc.Invoke.Static,
                Types.ScriptRuntime, "in", Type.getMethodType(Type.BOOLEAN_TYPE, Types.Object,
                        Types.Object, Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_typeof = MethodDesc.create(MethodDesc.Invoke.Static,
                Types.ScriptRuntime, "typeof",
                Type.getMethodType(Types.String, Types.Object, Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_InstanceofOperator = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.ScriptRuntime, "InstanceofOperator", Type
                        .getMethodType(Type.BOOLEAN_TYPE, Types.Object, Types.Object,
                                Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_ArrayAccumulationSpreadElement = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.ScriptRuntime, "ArrayAccumulationSpreadElement",
                Type.getMethodType(Type.INT_TYPE, Types.ArrayObject, Type.INT_TYPE, Types.Object,
                        Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_CheckCallable = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.ScriptRuntime, "CheckCallable",
                Type.getMethodType(Types.Callable, Types.Object, Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_defineLength = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.ScriptRuntime, "defineLength", Type
                        .getMethodType(Types.ArrayObject, Types.ArrayObject, Type.INT_TYPE,
                                Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_defineProperty__int = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.ScriptRuntime, "defineProperty", Type
                        .getMethodType(Type.VOID_TYPE, Types.ArrayObject, Type.INT_TYPE,
                                Types.Object, Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_directEvalFallbackArguments = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.ScriptRuntime, "directEvalFallbackArguments", Type
                        .getMethodType(Types.Object_, Types.Object_, Types.Object, Types.Callable,
                                Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_directEvalFallbackThisArgument = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.ScriptRuntime, "directEvalFallbackThisArgument",
                Type.getMethodType(Types.Object, Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_directEvalFallbackHook = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.ScriptRuntime, "directEvalFallbackHook",
                Type.getMethodType(Types.Callable, Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_EvaluateArrowFunction = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.ScriptRuntime, "EvaluateArrowFunction", Type
                        .getMethodType(Types.OrdinaryFunction, Types.RuntimeInfo$Function,
                                Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_EvaluateAsyncArrowFunction = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.ScriptRuntime, "EvaluateAsyncArrowFunction", Type
                        .getMethodType(Types.OrdinaryAsyncFunction, Types.RuntimeInfo$Function,
                                Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_EvaluateAsyncFunctionExpression = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.ScriptRuntime, "EvaluateAsyncFunctionExpression",
                Type.getMethodType(Types.OrdinaryAsyncFunction, Types.RuntimeInfo$Function,
                        Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_EvaluateConstructorCall = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.ScriptRuntime, "EvaluateConstructorCall", Type
                        .getMethodType(Types.ScriptObject, Types.Object, Types.Object_,
                                Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_EvaluateConstructorTailCall = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.ScriptRuntime, "EvaluateConstructorTailCall", Type
                        .getMethodType(Types.Object, Types.Object, Types.Object_,
                                Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_EvaluateFunctionExpression = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.ScriptRuntime, "EvaluateFunctionExpression", Type
                        .getMethodType(Types.OrdinaryFunction, Types.RuntimeInfo$Function,
                                Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_EvaluateGeneratorComprehension = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.ScriptRuntime, "EvaluateGeneratorComprehension",
                Type.getMethodType(Types.GeneratorObject, Types.RuntimeInfo$Function,
                        Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_EvaluateLegacyGeneratorComprehension = MethodDesc
                .create(MethodDesc.Invoke.Static, Types.ScriptRuntime,
                        "EvaluateLegacyGeneratorComprehension", Type.getMethodType(
                                Types.GeneratorObject, Types.RuntimeInfo$Function,
                                Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_EvaluateGeneratorExpression = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.ScriptRuntime, "EvaluateGeneratorExpression", Type
                        .getMethodType(Types.OrdinaryGenerator, Types.RuntimeInfo$Function,
                                Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_EvaluateLegacyGeneratorExpression = MethodDesc
                .create(MethodDesc.Invoke.Static, Types.ScriptRuntime,
                        "EvaluateLegacyGeneratorExpression", Type.getMethodType(
                                Types.OrdinaryGenerator, Types.RuntimeInfo$Function,
                                Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_getElement = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.ScriptRuntime, "getElement", Type.getMethodType(
                        Types.Reference, Types.Object, Types.Object, Types.ExecutionContext,
                        Type.BOOLEAN_TYPE));

        static final MethodDesc ScriptRuntime_getElementValue = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.ScriptRuntime, "getElementValue", Type
                        .getMethodType(Types.Object, Types.Object, Types.Object,
                                Types.ExecutionContext, Type.BOOLEAN_TYPE));

        static final MethodDesc ScriptRuntime_getProperty = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.ScriptRuntime, "getProperty", Type.getMethodType(
                        Types.Reference, Types.Object, Types.String, Types.ExecutionContext,
                        Type.BOOLEAN_TYPE));

        static final MethodDesc ScriptRuntime_getProperty_int = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.ScriptRuntime, "getProperty", Type.getMethodType(
                        Types.Reference, Types.Object, Type.INT_TYPE, Types.ExecutionContext,
                        Type.BOOLEAN_TYPE));

        static final MethodDesc ScriptRuntime_getProperty_long = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.ScriptRuntime, "getProperty", Type.getMethodType(
                        Types.Reference, Types.Object, Type.LONG_TYPE, Types.ExecutionContext,
                        Type.BOOLEAN_TYPE));

        static final MethodDesc ScriptRuntime_getProperty_double = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.ScriptRuntime, "getProperty", Type.getMethodType(
                        Types.Reference, Types.Object, Type.DOUBLE_TYPE, Types.ExecutionContext,
                        Type.BOOLEAN_TYPE));

        static final MethodDesc ScriptRuntime_getPropertyValue = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.ScriptRuntime, "getPropertyValue", Type
                        .getMethodType(Types.Object, Types.Object, Types.String,
                                Types.ExecutionContext, Type.BOOLEAN_TYPE));

        static final MethodDesc ScriptRuntime_getPropertyValue_int = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.ScriptRuntime, "getPropertyValue", Type
                        .getMethodType(Types.Object, Types.Object, Type.INT_TYPE,
                                Types.ExecutionContext, Type.BOOLEAN_TYPE));

        static final MethodDesc ScriptRuntime_getPropertyValue_long = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.ScriptRuntime, "getPropertyValue", Type
                        .getMethodType(Types.Object, Types.Object, Type.LONG_TYPE,
                                Types.ExecutionContext, Type.BOOLEAN_TYPE));

        static final MethodDesc ScriptRuntime_getPropertyValue_double = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.ScriptRuntime, "getPropertyValue", Type
                        .getMethodType(Types.Object, Types.Object, Type.DOUBLE_TYPE,
                                Types.ExecutionContext, Type.BOOLEAN_TYPE));

        static final MethodDesc ScriptRuntime_GetSuperConstructor = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.ScriptRuntime, "GetSuperConstructor",
                Type.getMethodType(Types.Callable, Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_getSuperPropertyReferenceValue = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.ScriptRuntime, "getSuperPropertyReferenceValue",
                Type.getMethodType(Types.Object, Types.ExecutionContext, Types.Object,
                        Type.BOOLEAN_TYPE));

        static final MethodDesc ScriptRuntime_getSuperPropertyReferenceValue_String = MethodDesc
                .create(MethodDesc.Invoke.Static, Types.ScriptRuntime,
                        "getSuperPropertyReferenceValue", Type.getMethodType(Types.Object,
                                Types.ExecutionContext, Types.String, Type.BOOLEAN_TYPE));

        static final MethodDesc ScriptRuntime_IsBuiltinEval = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.ScriptRuntime, "IsBuiltinEval",
                Type.getMethodType(Type.BOOLEAN_TYPE, Types.Callable, Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_MakeSuperPropertyReference = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.ScriptRuntime, "MakeSuperPropertyReference", Type
                        .getMethodType(Types.Reference, Types.ExecutionContext, Types.Object,
                                Type.BOOLEAN_TYPE));

        static final MethodDesc ScriptRuntime_MakeSuperPropertyReference_String = MethodDesc
                .create(MethodDesc.Invoke.Static, Types.ScriptRuntime,
                        "MakeSuperPropertyReference", Type.getMethodType(Types.Reference,
                                Types.ExecutionContext, Types.String, Type.BOOLEAN_TYPE));

        static final MethodDesc ScriptRuntime_PrepareForTailCall = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.ScriptRuntime, "PrepareForTailCall",
                Type.getMethodType(Types.Object, Types.Object_, Types.Object, Types.Callable));

        static final MethodDesc ScriptRuntime_SpreadArray = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.ScriptRuntime, "SpreadArray",
                Type.getMethodType(Types.Object_, Types.Object, Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_toFlatArray = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.ScriptRuntime, "toFlatArray",
                Type.getMethodType(Types.Object_, Types.Object_, Types.ExecutionContext));

        // class: StringBuilder
        static final MethodDesc StringBuilder_append_Charsequence = MethodDesc.create(
                MethodDesc.Invoke.Virtual, Types.StringBuilder, "append",
                Type.getMethodType(Types.StringBuilder, Types.CharSequence));

        static final MethodDesc StringBuilder_append_String = MethodDesc.create(
                MethodDesc.Invoke.Virtual, Types.StringBuilder, "append",
                Type.getMethodType(Types.StringBuilder, Types.String));

        static final MethodDesc StringBuilder_init = MethodDesc.create(MethodDesc.Invoke.Special,
                Types.StringBuilder, "<init>", Type.getMethodType(Type.VOID_TYPE));

        static final MethodDesc StringBuilder_toString = MethodDesc.create(
                MethodDesc.Invoke.Virtual, Types.StringBuilder, "toString",
                Type.getMethodType(Types.String));
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

    private void invokeDynamicNativeCall(String name, ExpressionVisitor mv) {
        mv.invokedynamic(NativeCalls.getNativeCallName(name),
                NativeCalls.getNativeCallMethodDescriptor(), NativeCalls.getNativeCallBootstrap(),
                EMPTY_BSM_ARGS);
    }

    private void invokeDynamicOperator(BinaryExpression.Operator operator, ExpressionVisitor mv) {
        mv.invokedynamic(Bootstrap.getName(operator), Bootstrap.getMethodDescriptor(operator),
                Bootstrap.getBootstrap(operator), EMPTY_BSM_ARGS);
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

    private ValType GetValue(LeftHandSideExpression node, ValType type, ExpressionVisitor mv) {
        assert type == ValType.Reference : "type is not reference: " + type;
        mv.loadExecutionContext();
        mv.invoke(Methods.Reference_getValue);
        return ValType.Any;
    }

    private ValType GetValue(SuperCallExpression node, ValType type, ExpressionVisitor mv) {
        assert type == ValType.Reference : "type is not reference: " + type;
        mv.loadExecutionContext();
        mv.invoke(Methods.Reference_getValue);
        return ValType.Any;
    }

    private void PutValue(LeftHandSideExpression node, ValType type, ExpressionVisitor mv) {
        assert type == ValType.Reference : "type is not reference: " + type;
        mv.loadExecutionContext();
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

    /**
     * [12.3.3.1.1 Runtime Semantics: EvaluateNew(thisCall, constructProduction, arguments)]
     * 
     * @return
     */
    private ValType EvaluateNew(NewExpression node, ExpressionVisitor mv) {
        /* steps 1-3 (not applicable) */
        /* steps 4-6 */
        evalAndGetBoxedValue(node.getExpression(), mv);
        /* steps 7-8 */
        ArgumentListEvaluation(node.getArguments(), mv);
        /* steps 9-14 */
        mv.lineInfo(node);
        mv.loadExecutionContext();
        if (!codegen.isEnabled(Compiler.Option.NoTailCall) && mv.isTailCall(node)) {
            mv.invoke(Methods.ScriptRuntime_EvaluateConstructorTailCall);
            return ValType.Any;
        }
        mv.invoke(Methods.ScriptRuntime_EvaluateConstructorCall);
        return ValType.Object;
    }

    private static boolean isPropertyReference(Expression base, ValType type) {
        return type == ValType.Reference && !(base instanceof IdentifierReference);
    }

    private static boolean isEnclosedByWithStatement(Name name, ExpressionVisitor mv) {
        for (Scope scope = mv.getScope();;) {
            if (scope instanceof WithScope) {
                return true;
            }
            if (scope.isDeclared(name)) {
                return false;
            }
            Scope nextScope = scope.getParent();
            if (nextScope == null) {
                assert scope instanceof TopLevelScope;
                nextScope = ((TopLevelScope) scope).getEnclosingScope();
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

    private static boolean isEnclosedByWithStatement(ExpressionVisitor mv) {
        for (Scope scope = mv.getScope();;) {
            if (scope instanceof WithScope) {
                return true;
            }
            Scope nextScope = scope.getParent();
            if (nextScope == null) {
                assert scope instanceof TopLevelScope;
                nextScope = ((TopLevelScope) scope).getEnclosingScope();
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
     * [12.3.4.2 Runtime Semantics: EvaluateCall( ref, arguments, tailPosition )]<br>
     * [12.3.4.3 Runtime Semantics: EvaluateDirectCall( func, thisValue, arguments, tailPosition )]
     * 
     * @param call
     *            the function call expression
     * @param base
     *            the call expression's base node
     * @param type
     *            the value type of the base node
     * @param arguments
     *            the list of function call arguments
     * @param directEval
     *            the strict-mode flag
     * @param mv
     *            the expression visitor
     */
    private ValType EvaluateCall(Expression call, Expression base, ValType type,
            List<Expression> arguments, boolean directEval, ExpressionVisitor mv) {
        if (type == ValType.Reference) {
            if (base instanceof SuperCallExpression) {
                assert call == base;
                EvaluateCallSuper((SuperCallExpression) call, type, arguments, mv);
            } else {
                assert base instanceof LeftHandSideExpression;
                LeftHandSideExpression lhs = (LeftHandSideExpression) base;
                if (isPropertyReference(lhs, type)) {
                    EvaluateCallPropRef(call, lhs, type, arguments, mv);
                } else {
                    IdentifierReference ident = (IdentifierReference) base;
                    Name name = ident.toName();
                    if (isEnclosedByWithStatement(name, mv)) {
                        EvaluateCallWithIdentRef(call, ident, type, arguments, directEval, mv);
                    } else {
                        EvaluateCallIdentRef(call, ident, type, arguments, directEval, mv);
                    }
                }
            }
        } else {
            EvaluateCallWithValue(call, base, type, arguments, mv);
        }
        return ValType.Any;
    }

    /**
     * [12.3.4.2 Runtime Semantics: EvaluateCall( ref, arguments, tailPosition )]
     * 
     * @param call
     *            the function call expression
     * @param base
     *            the call expression's base node
     * @param type
     *            the value type of the base node
     * @param arguments
     *            the list of function call arguments
     * @param mv
     *            the expression visitor
     */
    private void EvaluateCallPropRef(Expression call, LeftHandSideExpression base, ValType type,
            List<Expression> arguments, ExpressionVisitor mv) {
        // Only called for the property reference case (`obj.method(...)` or `obj[method](...)`).
        assert isPropertyReference(base, type);
        assert base instanceof ElementAccessor || base instanceof PropertyAccessor
                || base instanceof SuperElementAccessor || base instanceof SuperPropertyAccessor;

        // stack: [ref] -> [ref, ref]
        mv.dup();

        /* steps 1-2 */
        // stack: [ref, ref] -> [ref, func]
        GetValue(base, type, mv);

        /* steps 3-4 */
        // stack: [ref, func] -> [thisValue, func]
        mv.swap();
        mv.loadExecutionContext();
        mv.invoke(Methods.Reference_getThisValue);
        mv.swap();

        // stack: [thisValue, func] -> [result]
        EvaluateDirectCall(call, arguments, mv);
    }

    /**
     * [12.3.4.2 Runtime Semantics: EvaluateCall]
     * 
     * @param call
     *            the function call expression
     * @param type
     *            the value type of the base node
     * @param arguments
     *            the list of function call arguments
     * @param mv
     *            the expression visitor
     */
    private void EvaluateCallSuper(SuperCallExpression call, ValType type,
            List<Expression> arguments, ExpressionVisitor mv) {
        // stack: [ref] -> [ref, ref]
        mv.dup();

        /* steps 1-2 */
        // stack: [ref, ref] -> [ref, func]
        GetValue(call, type, mv);

        /* steps 3-4 */
        // stack: [ref, func] -> [thisValue, func]
        mv.swap();
        mv.loadExecutionContext();
        mv.invoke(Methods.Reference_getThisValue);
        mv.swap();

        /* step 5 */
        // stack: [thisValue, func] -> result
        EvaluateDirectCall(call, arguments, mv);
    }

    /**
     * [12.3.4.2 Runtime Semantics: EvaluateCall]
     * 
     * @param call
     *            the function call expression
     * @param base
     *            the call expression's base node
     * @param type
     *            the value type of the base node
     * @param arguments
     *            the list of function call arguments
     * @param mv
     *            the expression visitor
     */
    private void EvaluateCallWithValue(Expression call, Expression base, ValType type,
            List<Expression> arguments, ExpressionVisitor mv) {
        assert type != ValType.Reference;

        /* steps 1-2 (not applicable) */
        // GetValue(...)

        /* steps 3-4 */
        // stack: [func] -> [thisValue, func]
        mv.loadUndefined();
        mv.swap();

        /* steps 9-13 */
        // stack: [args, thisValue, func(Callable)] -> result
        EvaluateDirectCall(call, arguments, mv);
    }

    /**
     * [12.3.4.2 Runtime Semantics: EvaluateCall]
     * 
     * @param call
     *            the function call expression
     * @param base
     *            the call expression's base node
     * @param type
     *            the value type of the base node
     * @param arguments
     *            the list of function call arguments
     * @param directEval
     *            the strict-mode flag
     * @param mv
     *            the expression visitor
     */
    private void EvaluateCallIdentRef(Expression call, IdentifierReference base, ValType type,
            List<Expression> arguments, boolean directEval, ExpressionVisitor mv) {
        assert type == ValType.Reference;

        Jump afterCall = new Jump();

        /* steps 1-2 */
        // stack: [ref] -> [func]
        GetValue(base, type, mv);

        /* steps 3-4 */
        // stack: [func] -> [thisValue, func]
        mv.loadUndefined();
        mv.swap();

        /* steps 1-2 (EvaluateDirectCall) */
        // stack: [thisValue, func] -> [args, thisValue, func]
        ArgumentListEvaluation(arguments, mv);
        mv.dupX2();
        mv.pop();

        // stack: [args, thisValue, func]
        mv.lineInfo(call);

        /* steps 3-4 (EvaluateDirectCall) */
        // stack: [args, thisValue, func] -> [args, thisValue, func(Callable)]
        mv.loadExecutionContext();
        mv.invoke(Methods.ScriptRuntime_CheckCallable);

        if (directEval) {
            directEvalCall(arguments, afterCall, mv);
        }

        /* steps 5-9 (EvaluateDirectCall) */
        // stack: [args, thisValue, func(Callable)] -> result
        EvaluateDirectCall(call, mv);

        if (directEval) {
            mv.mark(afterCall);
        }
    }

    /**
     * [12.3.4.2 Runtime Semantics: EvaluateCall]
     * 
     * @param call
     *            the function call expression
     * @param base
     *            the call expression's base node
     * @param type
     *            the value type of the base node
     * @param arguments
     *            the list of function call arguments
     * @param directEval
     *            the strict-mode flag
     * @param mv
     *            the expression visitor
     */
    private void EvaluateCallWithIdentRef(Expression call, IdentifierReference base, ValType type,
            List<Expression> arguments, boolean directEval, ExpressionVisitor mv) {
        assert type == ValType.Reference;

        Jump afterCall = new Jump(), baseObjNotNull = new Jump();

        // stack: [ref] -> [ref, ref]
        mv.dup();

        /* steps 1-2 */
        // stack: [ref, ref] -> [ref, func]
        GetValue(base, type, mv);

        /* step 3-4 */
        // stack: [ref, func] -> [func, baseObj?]
        mv.swap();
        mv.invoke(Methods.Reference_getBase);
        mv.checkcast(Types.EnvironmentRecord);
        mv.invoke(Methods.EnvironmentRecord_withBaseObject);

        // stack: [func, baseObj?] -> [thisValue, func]
        mv.dup();
        mv.ifnonnull(baseObjNotNull);
        {
            mv.pop();
            mv.loadUndefined();
        }
        mv.mark(baseObjNotNull);
        mv.swap();

        /* steps 1-2 (EvaluateDirectCall) */
        // stack: [thisValue, func] -> [args, thisValue, func]
        ArgumentListEvaluation(arguments, mv);
        mv.dupX2();
        mv.pop();

        // stack: [args, thisValue, func]
        mv.lineInfo(call);

        /* steps 3-4 (EvaluateDirectCall) */
        // stack: [args, thisValue, func] -> [args, thisValue, func(Callable)]
        mv.loadExecutionContext();
        mv.invoke(Methods.ScriptRuntime_CheckCallable);

        if (directEval) {
            directEvalCall(arguments, afterCall, mv);
        }

        /* steps 5-9 (EvaluateDirectCall) */
        // stack: [args, thisValue, func(Callable)] -> result
        EvaluateDirectCall(call, mv);

        if (directEval) {
            mv.mark(afterCall);
        }
    }

    /**
     * [18.2.1.1] Direct Call to Eval
     * 
     * @param arguments
     *            the list of function call arguments
     * 
     * @param afterCall
     *            the label after the call instruction
     * @param mv
     *            the expression visitor
     */
    private void directEvalCall(List<Expression> arguments, Jump afterCall, ExpressionVisitor mv) {
        // test for possible direct-eval call
        Jump notEval = new Jump();

        // stack: [args, thisValue, func(Callable)] -> [args, thisValue, func(Callable)]
        mv.dup();
        mv.loadExecutionContext();
        mv.invoke(Methods.ScriptRuntime_IsBuiltinEval);
        mv.ifeq(notEval);
        {
            PerformEval(arguments, afterCall, mv);
        }
        mv.mark(notEval);

        if (codegen.isEnabled(CompatibilityOption.Realm)) {
            // direct-eval fallback hook
            Jump noEvalHook = new Jump();
            mv.loadExecutionContext();
            mv.invoke(Methods.ScriptRuntime_directEvalFallbackHook);
            mv.ifnull(noEvalHook);
            {
                // stack: [args, thisValue, func(Callable)] -> [args']
                mv.loadExecutionContext();
                mv.invoke(Methods.ScriptRuntime_directEvalFallbackArguments);
                // stack: [args'] -> [args', thisValue']
                mv.loadExecutionContext();
                mv.invoke(Methods.ScriptRuntime_directEvalFallbackThisArgument);
                // stack: [args', thisValue'] -> [args', thisValue', fallback(Callable)]
                mv.loadExecutionContext();
                mv.invoke(Methods.ScriptRuntime_directEvalFallbackHook);
            }
            mv.mark(noEvalHook);
        }
    }

    /**
     * [18.2.1.1] Direct Call to Eval
     * 
     * @param arguments
     *            the list of function call arguments
     * @param afterCall
     *            the label after the call instruction
     * @param mv
     *            the expression visitor
     */
    private void PerformEval(List<Expression> arguments, Jump afterCall, ExpressionVisitor mv) {
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

        // stack: [args, thisValue, func(Callable)] -> [args]
        mv.pop2();

        if (codegen.isEnabled(CompatibilityOption.Realm)) {
            // stack: [args] -> [result]
            mv.loadExecutionContext();
            mv.iconst(evalFlags);
            mv.invoke(Methods.Eval_directEvalWithTranslate);
            mv.goTo(afterCall);
        } else {
            if (arguments.isEmpty()) {
                // stack: [args(empty)] -> [result]
                mv.pop();
                mv.loadUndefined();
                mv.goTo(afterCall);
            } else if (hasArguments(arguments)) {
                // stack: [args] -> [arg_0]
                mv.iconst(0);
                mv.aaload();
                mv.loadExecutionContext();
                mv.iconst(evalFlags);
                mv.invoke(Methods.Eval_directEval);
                mv.goTo(afterCall);
            } else {
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

    /**
     * [12.3.4.3 Runtime Semantics: EvaluateDirectCall( func, thisValue, arguments, tailPosition )]
     * 
     * @param call
     *            the function call expression
     * @param mv
     *            the expression visitor
     */
    private ValType EvaluateDirectCall(Expression call, List<Expression> arguments,
            ExpressionVisitor mv) {
        /* steps 1-2 */
        // stack: [thisValue, func] -> [args, thisValue, func]
        ArgumentListEvaluation(arguments, mv);
        mv.dupX2();
        mv.pop();

        // stack: [args, thisValue, func]
        mv.lineInfo(call);

        /* steps 3-4 */
        // stack: [args, thisValue, func] -> [args, thisValue, func(Callable)]
        mv.loadExecutionContext();
        mv.invoke(Methods.ScriptRuntime_CheckCallable);

        /* steps 5-9 */
        // stack: [args, thisValue, func(Callable)] -> result
        return EvaluateDirectCall(call, mv);
    }

    /**
     * [12.3.4.3 Runtime Semantics: EvaluateDirectCall( func, thisValue, arguments, tailPosition )]
     * 
     * @param call
     *            the function call expression
     * @param mv
     *            the expression visitor
     */
    private ValType EvaluateDirectCall(Expression call, ExpressionVisitor mv) {
        /* steps 5-9 */
        if (!codegen.isEnabled(Compiler.Option.NoTailCall) && mv.isTailCall(call)) {
            // stack: [args, thisValue, func(Callable)] -> [<func(Callable), thisValue, args>]
            mv.invoke(Methods.ScriptRuntime_PrepareForTailCall);
            return ValType.Any;
        }

        // stack: [args, thisValue, func(Callable)] -> [func(Callable), cx, thisValue, args]
        mv.loadExecutionContext();
        mv.dup2X2();
        mv.pop2();
        mv.swap();

        /* steps 6, 8-9 */
        // stack: [func(Callable), cx, thisValue, args] -> [result]
        invokeDynamicCall(mv);
        return ValType.Any;
    }

    /**
     * [12.3.5.1 ArgumentListEvaluation]
     * 
     * @param arguments
     *            the list of function call arguments
     * @param mv
     *            the expression visitor
     */
    private void ArgumentListEvaluation(List<Expression> arguments, ExpressionVisitor mv) {
        if (arguments.isEmpty()) {
            mv.get(Fields.ScriptRuntime_EMPTY_ARRAY);
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

            // stack: [array, nextIndex]
            mv.iconst(0); // nextIndex

            arrayLiteralWithSpread(node, mv);

            // stack: [array, nextIndex] -> [array]
            mv.loadExecutionContext();
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

        mv.loadExecutionContext();

        // stack: [array, array, nextIndex, value, cx] -> [array, nextIndex']
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

                ToObject(left, rtype, mv);
                dup(node, mv);

                DestructuringAssignment((AssignmentPattern) left, mv);

                return completion(node, ValType.Object);
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
                    vtype = ToPrimitive(vtype, mv);
                    ToString(vtype, mv);
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
            if (ltype.isNumeric() && right instanceof Literal) {
                ToNumber(ltype, mv);
                ValType rtype = evalAndGetValue(right, mv);
                ToNumber(rtype, mv);
                mv.dadd();
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
            mv.invoke(Methods.ScriptRuntime_InstanceofOperator);
            return ValType.Boolean;
        }
        case IN: {
            // 12.9 Relational Operators ( in )
            evalAndGetBoxedValue(left, mv);
            evalAndGetBoxedValue(right, mv);

            mv.loadExecutionContext();
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
            throw new AssertionError(Objects.toString(node.getOperator(), "<null>"));
        }
    }

    private ValType evalToString(Expression node, ExpressionVisitor mv) {
        ValType type = evalAndGetValue(node, mv);
        type = ToPrimitive(type, mv);
        ToString(type, mv);
        return ValType.String;
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
        ValType type = node.getBase().accept(this, mv);
        mv.toBoxed(type);

        // direct call to eval?
        boolean directEval = (node.getBase() instanceof IdentifierReference && "eval"
                .equals(((IdentifierReference) node.getBase()).getName()));
        return EvaluateCall(node, node.getBase(), type, node.getArguments(), directEval, mv);
    }

    /**
     * 12.3.6.1 Runtime Semantics: ArgumentListEvaluation
     */
    @Override
    public ValType visit(CallSpreadElement node, ExpressionVisitor mv) {
        evalAndGetBoxedValue(node.getExpression(), mv);
        mv.loadExecutionContext();
        mv.invoke(Methods.ScriptRuntime_SpreadArray);

        return ValType.Any; // actually Object[]
    }

    /**
     * 14.5.16 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(ClassExpression node, ExpressionVisitor mv) {
        /* steps 1-2 */
        String className = node.getName() != null ? node.getName().getName().getIdentifier() : null;
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
        evalAndGetBoxedValue(node.getThen(), mv);
        mv.goTo(l1);
        /* step 4 */
        mv.mark(l0);
        evalAndGetBoxedValue(node.getOtherwise(), mv);
        mv.mark(l1);

        return ValType.Any;
    }

    /**
     * 12.3.2.1 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(ElementAccessor node, ExpressionVisitor mv) {
        /* steps 1-3 */
        evalAndGetBoxedValue(node.getBase(), mv);
        /* steps 4-6 */
        ValType elementType = evalAndGetValue(node.getElement(), mv);
        /* steps 7-11 */
        switch (elementType) {
        case Number:
            mv.loadExecutionContext();
            mv.iconst(mv.isStrict());
            mv.invoke(Methods.ScriptRuntime_getProperty_double);
            break;
        case Number_int:
            mv.loadExecutionContext();
            mv.iconst(mv.isStrict());
            mv.invoke(Methods.ScriptRuntime_getProperty_int);
            break;
        case Number_uint:
            mv.loadExecutionContext();
            mv.iconst(mv.isStrict());
            mv.invoke(Methods.ScriptRuntime_getProperty_long);
            break;
        case Boolean:
        case Null:
        case String:
        case Undefined:
            ToFlatString(elementType, mv);
            mv.loadExecutionContext();
            mv.iconst(mv.isStrict());
            mv.invoke(Methods.ScriptRuntime_getProperty);
            break;
        default:
            mv.loadExecutionContext();
            mv.iconst(mv.isStrict());
            mv.invoke(Methods.ScriptRuntime_getElement);
            break;
        }
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
        ValType elementType = evalAndGetValue(node.getElement(), mv);
        /* steps 7-11 */
        switch (elementType) {
        case Number:
            mv.loadExecutionContext();
            mv.iconst(mv.isStrict());
            mv.invoke(Methods.ScriptRuntime_getPropertyValue_double);
            break;
        case Number_int:
            mv.loadExecutionContext();
            mv.iconst(mv.isStrict());
            mv.invoke(Methods.ScriptRuntime_getPropertyValue_int);
            break;
        case Number_uint:
            mv.loadExecutionContext();
            mv.iconst(mv.isStrict());
            mv.invoke(Methods.ScriptRuntime_getPropertyValue_long);
            break;
        case Boolean:
        case Null:
        case String:
        case Undefined:
            ToFlatString(elementType, mv);
            mv.loadExecutionContext();
            mv.iconst(mv.isStrict());
            mv.invoke(Methods.ScriptRuntime_getPropertyValue);
            break;
        default:
            mv.loadExecutionContext();
            mv.iconst(mv.isStrict());
            mv.invoke(Methods.ScriptRuntime_getElementValue);
            break;
        }
        /* step 11 */
        return ValType.Any;
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
        // create new declarative lexical environment
        // stack: [] -> [env]
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
                for (Name name : BoundNames(binding.getBinding())) {
                    createMutableBinding(name, false, mv);
                }

                Expression initializer = binding.getInitializer();
                if (initializer != null) {
                    ValType type = expressionBoxedValue(initializer, mv);
                    if (binding.getBinding() instanceof BindingPattern) {
                        ToObject(binding.getBinding(), type, mv);
                    }
                } else {
                    assert binding.getBinding() instanceof BindingIdentifier;
                    mv.loadUndefined();
                }

                // stack: [env, envRec, envRec, value] -> [env, envRec]
                BindingInitializationWithEnvironment(binding.getBinding(), mv);
            }
            mv.pop();
        }
        // stack: [env] -> []
        pushLexicalEnvironment(mv);

        mv.enterScope(node);
        ValType type = evalAndGetValue(node.getExpression(), mv);
        mv.exitScope();

        // restore previous lexical environment
        popLexicalEnvironment(mv);

        return type;
    }

    @Override
    public ValType visit(NativeCallExpression node, ExpressionVisitor mv) {
        String nativeName = node.getBase().getName();
        ArgumentListEvaluation(node.getArguments(), mv);
        mv.lineInfo(node);
        mv.loadExecutionContext();
        invokeDynamicNativeCall(nativeName, mv);
        return ValType.Any;
    }

    /**
     * 12.3.3.1 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(NewExpression node, ExpressionVisitor mv) {
        /* steps 1-2 */
        return EvaluateNew(node, mv);
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
        /* step 1 */
        mv.loadExecutionContext();
        mv.get(Fields.Intrinsics_ObjectPrototype);
        mv.invoke(Methods.OrdinaryObject_ObjectCreate);
        /* steps 2-3 */
        for (PropertyDefinition property : node.getProperties()) {
            mv.dup();
            codegen.propertyDefinition(property, mv);
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
        mv.iconst(mv.isStrict());
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
     * 12.3.5.2 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(SuperCallExpression node, ExpressionVisitor mv) {
        /* steps 1-2 */
        // stack: [] -> [func(Callable)]
        mv.loadExecutionContext();
        mv.invoke(Methods.ScriptRuntime_GetSuperConstructor);

        /* step 3 */
        // stack: [] -> [thisValue, func(Callable)]
        mv.loadExecutionContext();
        mv.invoke(Methods.ExecutionContext_resolveThisBinding);
        mv.swap();

        /* steps 4-6 */
        return EvaluateDirectCall(node, node.getArguments(), mv);
    }

    /**
     * 12.3.5.2 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(SuperElementAccessor node, ExpressionVisitor mv) {
        /* steps 1-5 */
        mv.loadExecutionContext();
        ValType type = evalAndGetValue(node.getExpression(), mv);
        ToPropertyKey(type, mv);
        mv.iconst(mv.isStrict());
        mv.invoke(Methods.ScriptRuntime_MakeSuperPropertyReference);

        return ValType.Reference;
    }

    /**
     * 12.3.5.2 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(SuperElementAccessorValue node, ExpressionVisitor mv) {
        /* steps 1-5 */
        mv.loadExecutionContext();
        ValType type = evalAndGetValue(node.getExpression(), mv);
        ToPropertyKey(type, mv);
        mv.iconst(mv.isStrict());
        mv.invoke(Methods.ScriptRuntime_getSuperPropertyReferenceValue);

        return ValType.Any;
    }

    /**
     * 12.3.5.2 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(SuperNewExpression node, ExpressionVisitor mv) {
        /* steps 1-2 */
        mv.loadExecutionContext();
        mv.invoke(Methods.ScriptRuntime_GetSuperConstructor);

        /* steps 3-4 */
        ArgumentListEvaluation(node.getArguments(), mv);

        /* steps 5-11 */
        mv.lineInfo(node);
        mv.loadExecutionContext();
        if (!codegen.isEnabled(Compiler.Option.NoTailCall) && mv.isTailCall(node)) {
            mv.invoke(Methods.ScriptRuntime_EvaluateConstructorTailCall);
            return ValType.Any;
        }
        mv.invoke(Methods.ScriptRuntime_EvaluateConstructorCall);
        return ValType.Object;
    }

    /**
     * 12.3.5.2 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(SuperPropertyAccessor node, ExpressionVisitor mv) {
        /* steps 1-3 */
        mv.loadExecutionContext();
        mv.aconst(node.getName());
        mv.iconst(mv.isStrict());
        mv.invoke(Methods.ScriptRuntime_MakeSuperPropertyReference_String);

        return ValType.Reference;
    }

    /**
     * 12.3.5.2 Runtime Semantics: Evaluation
     */
    @Override
    public ValType visit(SuperPropertyAccessorValue node, ExpressionVisitor mv) {
        /* steps 1-3 */
        mv.loadExecutionContext();
        mv.aconst(node.getName());
        mv.iconst(mv.isStrict());
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
        // 12.2.9.2.2 Runtime Semantics: GetTemplateCallSite
        // 12.2.9.2.3 Runtime Semantics: SubstitutionEvaluation
        TemplateLiteral template = node.getTemplate();
        List<Expression> substitutions = Substitutions(template);
        ArrayList<Expression> arguments = new ArrayList<>(substitutions.size() + 1);
        arguments.add(template);
        arguments.addAll(substitutions);

        /* step 1 */
        ValType type = node.getBase().accept(this, mv);
        mv.toBoxed(type);
        /* steps 2-4 */
        return EvaluateCall(node, node.getBase(), type, arguments, false, mv);
    }

    /**
     * 12.2.9.2.4 Runtime Semantics: Evaluation
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
