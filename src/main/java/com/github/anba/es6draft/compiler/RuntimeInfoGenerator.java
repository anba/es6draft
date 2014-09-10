/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static com.github.anba.es6draft.semantics.StaticSemantics.ExpectedArgumentCount;
import static com.github.anba.es6draft.semantics.StaticSemantics.IsStrict;

import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.objectweb.asm.Type;

import com.github.anba.es6draft.ast.*;
import com.github.anba.es6draft.compiler.CodeGenerator.FunctionName;
import com.github.anba.es6draft.compiler.CodeGenerator.ScriptName;
import com.github.anba.es6draft.compiler.InstructionVisitor.MethodDesc;
import com.github.anba.es6draft.compiler.InstructionVisitor.MethodType;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo.FunctionFlags;

/**
 * 
 */
final class RuntimeInfoGenerator {
    private static final class Methods {
        // class: DebugInfo
        static final MethodDesc DebugInfo_init = MethodDesc.create(MethodType.Special,
                Types.DebugInfo, "<init>", Type.getMethodType(Type.VOID_TYPE));

        static final MethodDesc DebugInfo_addMethod = MethodDesc.create(MethodType.Virtual,
                Types.DebugInfo, "addMethod",
                Type.getMethodType(Type.VOID_TYPE, Types.Class, Types.String));

        // class: RuntimeInfo
        static final MethodDesc RTI_newScriptBody = MethodDesc.create(MethodType.Static,
                Types.RuntimeInfo, "newScriptBody", Type.getMethodType(
                        Types.RuntimeInfo$ScriptBody, Types.String, Types.String,
                        Type.BOOLEAN_TYPE, Types.MethodHandle, Types.MethodHandle,
                        Types.MethodHandle));

        static final MethodDesc RTI_newScriptBodyDebug = MethodDesc.create(MethodType.Static,
                Types.RuntimeInfo, "newScriptBody", Type.getMethodType(
                        Types.RuntimeInfo$ScriptBody, Types.String, Types.String,
                        Type.BOOLEAN_TYPE, Types.MethodHandle, Types.MethodHandle,
                        Types.MethodHandle, Types.MethodHandle));

        static final MethodDesc RTI_newFunction = MethodDesc.create(MethodType.Static,
                Types.RuntimeInfo, "newFunction", Type.getMethodType(Types.RuntimeInfo$Function,
                        Types.String, Type.INT_TYPE, Type.INT_TYPE, Types.String, Type.INT_TYPE,
                        Types.MethodHandle, Types.MethodHandle));

        static final MethodDesc RTI_newFunctionDebug = MethodDesc.create(MethodType.Static,
                Types.RuntimeInfo, "newFunction", Type.getMethodType(Types.RuntimeInfo$Function,
                        Types.String, Type.INT_TYPE, Type.INT_TYPE, Types.String, Type.INT_TYPE,
                        Types.MethodHandle, Types.MethodHandle, Types.MethodHandle));
    }

    private final CodeGenerator codegen;

    RuntimeInfoGenerator(CodeGenerator codegen) {
        this.codegen = codegen;
    }

    private int functionFlags(FunctionNode node, boolean tailCall) {
        boolean strict = IsStrict(node);
        int functionFlags = 0;
        if (strict) {
            functionFlags |= FunctionFlags.Strict.getValue();
        }
        if (strict && node.getStrictMode() == FunctionNode.StrictMode.ImplicitStrict) {
            functionFlags |= FunctionFlags.ImplicitStrict.getValue();
        }
        if (node.isGenerator()) {
            functionFlags |= FunctionFlags.Generator.getValue();
        }
        if (node.isAsync()) {
            functionFlags |= FunctionFlags.Async.getValue();
        }
        if (node.getThisMode() == FunctionNode.ThisMode.Lexical) {
            functionFlags |= FunctionFlags.Arrow.getValue();
        }
        if (node instanceof Declaration) {
            functionFlags |= FunctionFlags.Declaration.getValue();
        }
        if (node instanceof Expression) {
            functionFlags |= FunctionFlags.Expression.getValue();
        }
        if (node instanceof ArrowFunction && ((ArrowFunction) node).getExpression() != null) {
            functionFlags |= FunctionFlags.ConciseBody.getValue();
        } else if (node instanceof AsyncArrowFunction
                && ((AsyncArrowFunction) node).getExpression() != null) {
            functionFlags |= FunctionFlags.ConciseBody.getValue();
        }
        if (node instanceof MethodDefinition) {
            functionFlags |= FunctionFlags.Method.getValue();
            if (((MethodDefinition) node).isStatic()) {
                functionFlags |= FunctionFlags.Static.getValue();
            }
        }
        if (node instanceof LegacyGeneratorDeclaration
                || node instanceof LegacyGeneratorExpression
                || (node instanceof GeneratorComprehension && ((GeneratorComprehension) node)
                        .getComprehension() instanceof LegacyComprehension)) {
            functionFlags |= FunctionFlags.LegacyGenerator.getValue();
        }
        if (isLegacy(node)) {
            functionFlags |= FunctionFlags.Legacy.getValue();
        }
        if (hasScopedName(node)) {
            functionFlags |= FunctionFlags.ScopedName.getValue();
        }
        if (node.getScope().hasSuperReference()) {
            functionFlags |= FunctionFlags.Super.getValue();
        }
        if (!node.hasSyntheticNodes() && !codegen.isEnabled(Compiler.Option.NoResume)) {
            functionFlags |= FunctionFlags.ResumeGenerator.getValue();
        }
        if (tailCall) {
            assert !node.isGenerator() && !node.isAsync() && strict;
            functionFlags |= FunctionFlags.TailCall.getValue();
        }
        return functionFlags;
    }

    private boolean isLegacy(FunctionNode node) {
        return !IsStrict(node)
                && (node instanceof FunctionDeclaration || node instanceof FunctionExpression)
                && codegen.isEnabled(CompatibilityOption.FunctionPrototype);
    }

    private static boolean hasScopedName(FunctionNode node) {
        if (node instanceof FunctionExpression) {
            return ((FunctionExpression) node).getIdentifier() != null;
        } else if (node instanceof GeneratorExpression) {
            return ((GeneratorExpression) node).getIdentifier() != null;
        } else if (node instanceof AsyncFunctionExpression) {
            return ((AsyncFunctionExpression) node).getIdentifier() != null;
        } else {
            return false;
        }
    }

    private static <T> T get(Future<T> future) {
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    void runtimeInfo(FunctionNode node, boolean tailCall, Future<String> source) {
        InstructionVisitor mv = new InstructionVisitor(codegen.newMethod(node, FunctionName.RTI));
        mv.begin();

        mv.aconst(node.getFunctionName());
        mv.iconst(functionFlags(node, tailCall));
        mv.iconst(ExpectedArgumentCount(node.getParameters()));
        mv.aconst(get(source));
        mv.iconst(node.getHeaderSource().length());
        mv.handle(codegen.methodDesc(node, FunctionName.Code));
        mv.handle(codegen.methodDesc(node, FunctionName.Call));
        if (codegen.isEnabled(Compiler.Option.DebugInfo)) {
            debugInfo(node);
            mv.handle(codegen.methodDesc(node, FunctionName.DebugInfo));
            mv.invoke(Methods.RTI_newFunctionDebug);
        } else {
            mv.invoke(Methods.RTI_newFunction);
        }
        mv.areturn();

        mv.end();
    }

    void runtimeInfo(Script node) {
        InstructionVisitor mv = new InstructionVisitor(codegen.newMethod(node, ScriptName.RTI));
        mv.begin();

        mv.aconst(node.getSource().getName());
        mv.aconst(Objects.toString(node.getSource().getFile(), null));
        mv.iconst(IsStrict(node));
        mv.handle(codegen.methodDesc(node, ScriptName.Init));
        mv.handle(codegen.methodDesc(node, ScriptName.EvalInit));
        mv.handle(codegen.methodDesc(node, ScriptName.Code));
        if (codegen.isEnabled(Compiler.Option.DebugInfo)) {
            debugInfo(node);
            mv.handle(codegen.methodDesc(node, ScriptName.DebugInfo));
            mv.invoke(Methods.RTI_newScriptBodyDebug);
        } else {
            mv.invoke(Methods.RTI_newScriptBody);
        }
        mv.areturn();

        mv.end();
    }

    private void debugInfo(FunctionNode node) {
        InstructionVisitor mv = new InstructionVisitor(codegen.newMethod(node,
                FunctionName.DebugInfo));
        mv.begin();

        mv.anew(Types.DebugInfo);
        mv.dup();
        mv.invoke(Methods.DebugInfo_init);

        mv.dup();
        MethodDesc callDesc = codegen.methodDesc(node, FunctionName.Call);
        mv.aconst(Type.getObjectType(callDesc.owner));
        mv.aconst(callDesc.name);
        mv.invoke(Methods.DebugInfo_addMethod);

        mv.dup();
        MethodDesc initDesc = codegen.methodDesc(node, FunctionName.Init);
        mv.aconst(Type.getObjectType(initDesc.owner));
        mv.aconst(initDesc.name);
        mv.invoke(Methods.DebugInfo_addMethod);

        mv.dup();
        MethodDesc codeDesc = codegen.methodDesc(node, FunctionName.Code);
        mv.aconst(Type.getObjectType(codeDesc.owner));
        mv.aconst(codeDesc.name);
        mv.invoke(Methods.DebugInfo_addMethod);

        mv.areturn();
        mv.end();
    }

    private void debugInfo(Script node) {
        InstructionVisitor mv = new InstructionVisitor(
                codegen.newMethod(node, ScriptName.DebugInfo));
        mv.begin();

        mv.anew(Types.DebugInfo);
        mv.dup();
        mv.invoke(Methods.DebugInfo_init);

        mv.dup();
        MethodDesc initDesc = codegen.methodDesc(node, ScriptName.Init);
        mv.aconst(Type.getObjectType(initDesc.owner));
        mv.aconst(initDesc.name);
        mv.invoke(Methods.DebugInfo_addMethod);

        mv.dup();
        MethodDesc evalInitDesc = codegen.methodDesc(node, ScriptName.EvalInit);
        mv.aconst(Type.getObjectType(evalInitDesc.owner));
        mv.aconst(evalInitDesc.name);
        mv.invoke(Methods.DebugInfo_addMethod);

        mv.dup();
        MethodDesc codeDesc = codegen.methodDesc(node, ScriptName.Code);
        mv.aconst(Type.getObjectType(codeDesc.owner));
        mv.aconst(codeDesc.name);
        mv.invoke(Methods.DebugInfo_addMethod);

        mv.areturn();
        mv.end();
    }
}
