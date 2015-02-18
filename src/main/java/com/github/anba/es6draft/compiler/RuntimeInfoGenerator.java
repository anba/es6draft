/**
 * Copyright (c) 2012-2015 André Bargull
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
import com.github.anba.es6draft.compiler.assembler.Code.MethodCode;
import com.github.anba.es6draft.compiler.assembler.InstructionAssembler;
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
                        Types.MethodHandle, Types.MethodHandle, Types.MethodHandle));

        static final MethodName RTI_newFunctionDebug = MethodName.findStatic(Types.RuntimeInfo,
                "newFunction", Type.methodType(Types.RuntimeInfo$Function, Types.String,
                        Type.INT_TYPE, Type.INT_TYPE, Types.String, Type.INT_TYPE,
                        Types.MethodHandle, Types.MethodHandle, Types.MethodHandle,
                        Types.MethodHandle));
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
        InstructionAssembler asm = new InstructionAssembler(codegen.newMethod(node,
                FunctionName.RTI));
        asm.begin();

        asm.aconst(node.getFunctionName());
        asm.iconst(functionFlags(node, tailCall));
        asm.iconst(ExpectedArgumentCount(node.getParameters()));
        asm.aconst(get(source));
        asm.iconst(node.getHeaderSource().length());
        asm.handle(codegen.methodDesc(node, FunctionName.Code));
        asm.handle(codegen.methodDesc(node, FunctionName.Call));
        if (node.isConstructor()) {
            FunctionName constructName = tailCall ? FunctionName.ConstructTailCall
                    : FunctionName.Construct;
            asm.handle(codegen.methodDesc(node, constructName));
        } else {
            asm.anull();
        }
        if (codegen.isEnabled(Compiler.Option.DebugInfo)) {
            debugInfo(node, tailCall);
            asm.handle(codegen.methodDesc(node, FunctionName.DebugInfo));
            asm.invoke(Methods.RTI_newFunctionDebug);
        } else {
            asm.invoke(Methods.RTI_newFunction);
        }
        asm._return();

        asm.end();
    }

    void runtimeInfo(Script node) {
        InstructionAssembler asm = new InstructionAssembler(codegen.newMethod(node, ScriptName.RTI));
        asm.begin();

        asm.aconst(node.getSource().getName());
        asm.aconst(node.getSource().getFileString());
        asm.iconst(IsStrict(node));
        asm.handle(codegen.methodDesc(node, ScriptName.Init));
        asm.handle(codegen.methodDesc(node, ScriptName.EvalInit));
        asm.handle(codegen.methodDesc(node, ScriptName.Code));
        if (codegen.isEnabled(Compiler.Option.DebugInfo)) {
            debugInfo(node);
            asm.handle(codegen.methodDesc(node, ScriptName.DebugInfo));
            asm.invoke(Methods.RTI_newScriptBodyDebug);
        } else {
            asm.invoke(Methods.RTI_newScriptBody);
        }
        asm._return();

        asm.end();
    }

    void runtimeInfo(Module node) {
        InstructionAssembler asm = new InstructionAssembler(codegen.newMethod(node, ModuleName.RTI));
        asm.begin();

        asm.aconst(node.getSource().getName());
        asm.aconst(node.getSource().getFileString());
        asm.handle(codegen.methodDesc(node, ModuleName.Init));
        asm.handle(codegen.methodDesc(node, ModuleName.Code));
        if (codegen.isEnabled(Compiler.Option.DebugInfo)) {
            debugInfo(node);
            asm.handle(codegen.methodDesc(node, ModuleName.DebugInfo));
            asm.invoke(Methods.RTI_newModuleBodyDebug);
        } else {
            asm.invoke(Methods.RTI_newModuleBody);
        }
        asm._return();

        asm.end();
    }

    private void debugInfo(FunctionNode node, boolean tailCall) {
        if (node.isConstructor()) {
            FunctionName constructName = tailCall ? FunctionName.ConstructTailCall
                    : FunctionName.Construct;
            debugInfo(codegen.newMethod(node, FunctionName.DebugInfo),
                    codegen.methodDesc(node, FunctionName.Call),
                    codegen.methodDesc(node, constructName),
                    codegen.methodDesc(node, FunctionName.Init),
                    codegen.methodDesc(node, FunctionName.Code));
        } else {
            debugInfo(codegen.newMethod(node, FunctionName.DebugInfo),
                    codegen.methodDesc(node, FunctionName.Call),
                    codegen.methodDesc(node, FunctionName.Init),
                    codegen.methodDesc(node, FunctionName.Code));
        }
    }

    private void debugInfo(Script node) {
        debugInfo(codegen.newMethod(node, ScriptName.DebugInfo),
                codegen.methodDesc(node, ScriptName.Init),
                codegen.methodDesc(node, ScriptName.EvalInit),
                codegen.methodDesc(node, ScriptName.Code));
    }

    private void debugInfo(Module node) {
        debugInfo(codegen.newMethod(node, ModuleName.DebugInfo),
                codegen.methodDesc(node, ModuleName.Init),
                codegen.methodDesc(node, ModuleName.Code));
    }

    private void debugInfo(MethodCode code, MethodName... names) {
        InstructionAssembler asm = new InstructionAssembler(code);
        asm.begin();

        asm.anew(Types.DebugInfo, Methods.DebugInfo_init);
        for (MethodName name : names) {
            asm.dup();
            asm.tconst(name.owner);
            asm.aconst(name.name);
            asm.invoke(Methods.DebugInfo_addMethod);
        }

        asm._return();
        asm.end();
    }
}
