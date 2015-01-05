/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static com.github.anba.es6draft.semantics.StaticSemantics.ExpectedArgumentCount;
import static com.github.anba.es6draft.semantics.StaticSemantics.IsStrict;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.github.anba.es6draft.ast.*;
import com.github.anba.es6draft.compiler.CodeGenerator.FunctionName;
import com.github.anba.es6draft.compiler.CodeGenerator.ModuleName;
import com.github.anba.es6draft.compiler.CodeGenerator.ScriptName;
import com.github.anba.es6draft.compiler.assembler.MethodName;
import com.github.anba.es6draft.compiler.assembler.Type;
import com.github.anba.es6draft.parser.Parser;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo.FunctionFlags;

/**
 * 
 */
final class RuntimeInfoGenerator {
    private static final class Methods {
        // class: DebugInfo
        static final MethodName DebugInfo_init = MethodName.findConstructor(Types.DebugInfo,
                Type.methodType(Type.VOID_TYPE));

        static final MethodName DebugInfo_addMethod = MethodName.findVirtual(Types.DebugInfo,
                "addMethod", Type.methodType(Type.VOID_TYPE, Types.Class, Types.String));

        // class: RuntimeInfo
        static final MethodName RTI_newScriptBody = MethodName.findStatic(Types.RuntimeInfo,
                "newScriptBody", Type.methodType(Types.RuntimeInfo$ScriptBody, Types.String,
                        Types.String, Type.BOOLEAN_TYPE, Types.MethodHandle, Types.MethodHandle,
                        Types.MethodHandle));

        static final MethodName RTI_newScriptBodyDebug = MethodName.findStatic(Types.RuntimeInfo,
                "newScriptBody", Type.methodType(Types.RuntimeInfo$ScriptBody, Types.String,
                        Types.String, Type.BOOLEAN_TYPE, Types.MethodHandle, Types.MethodHandle,
                        Types.MethodHandle, Types.MethodHandle));

        static final MethodName RTI_newModuleBody = MethodName.findStatic(Types.RuntimeInfo,
                "newModuleBody", Type.methodType(Types.RuntimeInfo$ModuleBody, Types.String,
                        Types.String, Types.MethodHandle, Types.MethodHandle));

        static final MethodName RTI_newModuleBodyDebug = MethodName.findStatic(Types.RuntimeInfo,
                "newModuleBody", Type.methodType(Types.RuntimeInfo$ModuleBody, Types.String,
                        Types.String, Types.MethodHandle, Types.MethodHandle, Types.MethodHandle));

        static final MethodName RTI_newFunction = MethodName.findStatic(Types.RuntimeInfo,
                "newFunction", Type.methodType(Types.RuntimeInfo$Function, Types.String,
                        Type.INT_TYPE, Type.INT_TYPE, Types.String, Type.INT_TYPE,
                        Types.MethodHandle, Types.MethodHandle));

        static final MethodName RTI_newFunctionDebug = MethodName.findStatic(Types.RuntimeInfo,
                "newFunction", Type.methodType(Types.RuntimeInfo$Function, Types.String,
                        Type.INT_TYPE, Type.INT_TYPE, Types.String, Type.INT_TYPE,
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
        if (codegen.isEnabled(Parser.Option.NativeFunction)) {
            functionFlags |= FunctionFlags.Native.getValue();
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
        mv._return();

        mv.end();
    }

    void runtimeInfo(Script node) {
        InstructionVisitor mv = new InstructionVisitor(codegen.newMethod(node, ScriptName.RTI));
        mv.begin();

        mv.aconst(node.getSource().getName());
        mv.aconst(node.getSource().getFileString());
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
        mv._return();

        mv.end();
    }

    void runtimeInfo(Module node) {
        InstructionVisitor mv = new InstructionVisitor(codegen.newMethod(node, ModuleName.RTI));
        mv.begin();

        mv.aconst(node.getSource().getName());
        mv.aconst(node.getSource().getFileString());
        mv.handle(codegen.methodDesc(node, ModuleName.Init));
        mv.handle(codegen.methodDesc(node, ModuleName.Code));
        if (codegen.isEnabled(Compiler.Option.DebugInfo)) {
            debugInfo(node);
            mv.handle(codegen.methodDesc(node, ModuleName.DebugInfo));
            mv.invoke(Methods.RTI_newModuleBodyDebug);
        } else {
            mv.invoke(Methods.RTI_newModuleBody);
        }
        mv._return();

        mv.end();
    }

    private void debugInfo(FunctionNode node) {
        InstructionVisitor mv = new InstructionVisitor(codegen.newMethod(node,
                FunctionName.DebugInfo));
        mv.begin();

        mv.anew(Types.DebugInfo, Methods.DebugInfo_init);

        MethodName callDesc = codegen.methodDesc(node, FunctionName.Call);
        mv.dup();
        mv.tconst(callDesc.owner);
        mv.aconst(callDesc.name);
        mv.invoke(Methods.DebugInfo_addMethod);

        MethodName initDesc = codegen.methodDesc(node, FunctionName.Init);
        mv.dup();
        mv.tconst(initDesc.owner);
        mv.aconst(initDesc.name);
        mv.invoke(Methods.DebugInfo_addMethod);

        MethodName codeDesc = codegen.methodDesc(node, FunctionName.Code);
        mv.dup();
        mv.tconst(codeDesc.owner);
        mv.aconst(codeDesc.name);
        mv.invoke(Methods.DebugInfo_addMethod);

        mv._return();
        mv.end();
    }

    private void debugInfo(Script node) {
        InstructionVisitor mv = new InstructionVisitor(
                codegen.newMethod(node, ScriptName.DebugInfo));
        mv.begin();

        mv.anew(Types.DebugInfo, Methods.DebugInfo_init);

        MethodName initDesc = codegen.methodDesc(node, ScriptName.Init);
        mv.dup();
        mv.tconst(initDesc.owner);
        mv.aconst(initDesc.name);
        mv.invoke(Methods.DebugInfo_addMethod);

        MethodName evalInitDesc = codegen.methodDesc(node, ScriptName.EvalInit);
        mv.dup();
        mv.tconst(evalInitDesc.owner);
        mv.aconst(evalInitDesc.name);
        mv.invoke(Methods.DebugInfo_addMethod);

        MethodName codeDesc = codegen.methodDesc(node, ScriptName.Code);
        mv.dup();
        mv.tconst(codeDesc.owner);
        mv.aconst(codeDesc.name);
        mv.invoke(Methods.DebugInfo_addMethod);

        mv._return();
        mv.end();
    }

    private void debugInfo(Module node) {
        InstructionVisitor mv = new InstructionVisitor(
                codegen.newMethod(node, ModuleName.DebugInfo));
        mv.begin();

        mv.anew(Types.DebugInfo, Methods.DebugInfo_init);

        MethodName initDesc = codegen.methodDesc(node, ModuleName.Init);
        mv.dup();
        mv.tconst(initDesc.owner);
        mv.aconst(initDesc.name);
        mv.invoke(Methods.DebugInfo_addMethod);

        MethodName codeDesc = codegen.methodDesc(node, ModuleName.Code);
        mv.dup();
        mv.tconst(codeDesc.owner);
        mv.aconst(codeDesc.name);
        mv.invoke(Methods.DebugInfo_addMethod);

        mv._return();
        mv.end();
    }
}
