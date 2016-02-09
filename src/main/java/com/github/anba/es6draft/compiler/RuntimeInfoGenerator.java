/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static com.github.anba.es6draft.semantics.StaticSemantics.ExpectedArgumentCount;
import static com.github.anba.es6draft.semantics.StaticSemantics.IsStrict;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.function.BiConsumer;

import com.github.anba.es6draft.ast.*;
import com.github.anba.es6draft.compiler.CodeGenerator.FunctionName;
import com.github.anba.es6draft.compiler.CodeGenerator.ModuleName;
import com.github.anba.es6draft.compiler.CodeGenerator.ScriptName;
import com.github.anba.es6draft.compiler.assembler.Code.MethodCode;
import com.github.anba.es6draft.compiler.assembler.Handle;
import com.github.anba.es6draft.compiler.assembler.InstructionAssembler;
import com.github.anba.es6draft.compiler.assembler.MethodName;
import com.github.anba.es6draft.compiler.assembler.Type;
import com.github.anba.es6draft.parser.Parser;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;
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
                "addMethod", Type.methodType(Type.VOID_TYPE, Types.MethodHandle));

        // class: RuntimeInfo
        static final MethodName RTI_newScriptBody = MethodName.findStatic(Types.RuntimeInfo,
                "newScriptBody", Type.methodType(Types.RuntimeInfo$ScriptBody, Types.String,
                        Types.String, Types.MethodHandle));

        static final MethodName RTI_newScriptBodyDebug = MethodName.findStatic(Types.RuntimeInfo,
                "newScriptBody", Type.methodType(Types.RuntimeInfo$ScriptBody, Types.String,
                        Types.String, Types.MethodHandle, Types.MethodHandle));

        static final MethodName RTI_newModuleBody = MethodName.findStatic(Types.RuntimeInfo,
                "newModuleBody", Type.methodType(Types.RuntimeInfo$ModuleBody, Types.String,
                        Types.String, Types.MethodHandle, Types.MethodHandle));

        static final MethodName RTI_newModuleBodyDebug = MethodName.findStatic(Types.RuntimeInfo,
                "newModuleBody", Type.methodType(Types.RuntimeInfo$ModuleBody, Types.String,
                        Types.String, Types.MethodHandle, Types.MethodHandle, Types.MethodHandle));

        static final MethodName RTI_newFunction = MethodName.findStatic(Types.RuntimeInfo,
                "newFunction", Type.methodType(Types.RuntimeInfo$Function, Types.Object,
                        Types.String, Type.INT_TYPE, Type.INT_TYPE, Types.String, Type.INT_TYPE,
                        Types.MethodHandle, Types.MethodHandle, Types.MethodHandle));

        static final MethodName RTI_newFunctionDebug = MethodName.findStatic(Types.RuntimeInfo,
                "newFunction", Type.methodType(Types.RuntimeInfo$Function, Types.Object,
                        Types.String, Type.INT_TYPE, Type.INT_TYPE, Types.String, Type.INT_TYPE,
                        Types.MethodHandle, Types.MethodHandle, Types.MethodHandle,
                        Types.MethodHandle));
    }

    private final CodeGenerator codegen;

    RuntimeInfoGenerator(CodeGenerator codegen) {
        this.codegen = codegen;
    }

    private int functionFlags(FunctionNode node, boolean tailCall, boolean tailConstruct) {
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
        if (hasConciseBody(node)) {
            functionFlags |= FunctionFlags.ConciseBody.getValue();
        }
        if (node instanceof MethodDefinition) {
            MethodDefinition method = (MethodDefinition) node;
            if (method.isClassConstructor()) {
                functionFlags |= FunctionFlags.Class.getValue();
            } else {
                functionFlags |= FunctionFlags.Method.getValue();
                if (method.isStatic()) {
                    functionFlags |= FunctionFlags.Static.getValue();
                }
            }
        }
        if (isLegacyGenerator(node)) {
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
        if (tailConstruct) {
            assert !node.isGenerator() && !node.isAsync() && strict;
            functionFlags |= FunctionFlags.TailConstruct.getValue();
        }
        if (codegen.isEnabled(Parser.Option.NativeFunction)) {
            functionFlags |= FunctionFlags.Native.getValue();
        }
        if (node.getScope().hasEval()) {
            functionFlags |= FunctionFlags.Eval.getValue();
        }
        return functionFlags;
    }

    private boolean hasConciseBody(FunctionNode node) {
        if (node instanceof ArrowFunction) {
            return ((ArrowFunction) node).getExpression() != null;
        }
        if (node instanceof AsyncArrowFunction) {
            return ((AsyncArrowFunction) node).getExpression() != null;
        }
        return false;
    }

    private boolean isLegacy(FunctionNode node) {
        if (IsStrict(node)) {
            return false;
        }
        if (!(node instanceof FunctionDeclaration || node instanceof FunctionExpression)) {
            return false;
        }
        return codegen.isEnabled(CompatibilityOption.FunctionArguments)
                || codegen.isEnabled(CompatibilityOption.FunctionCaller);
    }

    private boolean isLegacyGenerator(FunctionNode node) {
        if (node instanceof LegacyGeneratorDeclaration || node instanceof LegacyGeneratorExpression) {
            return true;
        }
        if (node instanceof GeneratorComprehension) {
            return ((GeneratorComprehension) node).getComprehension() instanceof LegacyComprehension;
        }
        return false;
    }

    private static boolean hasScopedName(FunctionNode node) {
        return node instanceof Expression && node.getIdentifier() != null;
    }

    private static final Handle RUNTIME_INFO_BOOTSTRAP = MethodName
            .findStatic(RuntimeInfo.class, "bootstrap",
                    MethodType.methodType(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class))
            .toHandle();

    void runtimeInfo(ClassDefinition node, boolean tailCall, boolean tailConstruct, String source) {
        MethodDefinition constructor = node.getConstructor();
        // Cf. CodeGenerator#getSource(ClassDefinition)
        int split = "constructor{}".length() + constructor.getHeaderSource().length()
                + constructor.getBodySource().length();
        runtimeInfo(node, constructor, tailCall, tailConstruct, source, split, this::debugInfo);
    }

    void runtimeInfo(FunctionNode node, boolean tailCall, String source) {
        runtimeInfo(node, node, tailCall, tailCall, source, node.getHeaderSource().length(), this::debugInfo);
    }

    private <T extends Node> void runtimeInfo(T def, FunctionNode node, boolean tailCall, boolean tailConstruct,
            String source, int sourceSplit, BiConsumer<T, FunctionName> debugInfo) {
        FunctionName constructName = tailConstruct ? FunctionName.ConstructTailCall : FunctionName.Construct;
        InstructionAssembler asm = new InstructionAssembler(codegen.newMethod(node, FunctionName.RTI));
        asm.begin();

        asm.invokedynamic("methodInfo", Type.methodType(Types.Object), RUNTIME_INFO_BOOTSTRAP);
        asm.aconst(node.getFunctionName());
        asm.iconst(functionFlags(node, tailCall, tailConstruct));
        asm.iconst(ExpectedArgumentCount(node.getParameters()));
        asm.aconst(source);
        asm.iconst(sourceSplit);
        if (node.isAsync() || node.isGenerator()) {
            asm.handle(codegen.methodDesc(node, FunctionName.Code));
        } else {
            asm.anull();
        }
        asm.handle(codegen.methodDesc(node, FunctionName.Call));
        if (node.isConstructor()) {
            asm.handle(codegen.methodDesc(node, constructName));
        } else {
            asm.anull();
        }
        if (codegen.isEnabled(Compiler.Option.DebugInfo)) {
            debugInfo.accept(def, constructName);
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
        asm.handle(codegen.methodDesc(node, ScriptName.Eval));
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

    private void debugInfo(FunctionNode node, FunctionName constructName) {
        if (node.isConstructor()) {
            debugInfo(codegen.newMethod(node, FunctionName.DebugInfo),
                    codegen.methodDesc(node, FunctionName.RTI),
                    codegen.methodDesc(node, FunctionName.Call),
                    codegen.methodDesc(node, constructName),
                    codegen.methodDesc(node, FunctionName.Init),
                    codegen.methodDesc(node, FunctionName.Code));
        } else {
            debugInfo(codegen.newMethod(node, FunctionName.DebugInfo),
                    codegen.methodDesc(node, FunctionName.RTI),
                    codegen.methodDesc(node, FunctionName.Call),
                    codegen.methodDesc(node, FunctionName.Init),
                    codegen.methodDesc(node, FunctionName.Code));
        }
    }

    private void debugInfo(ClassDefinition node, FunctionName constructName) {
        MethodDefinition constructor = node.getConstructor();
        MethodDefinition callConstructor = node.getCallConstructor();
        if (callConstructor == null) {
            debugInfo(codegen.newMethod(constructor, FunctionName.DebugInfo),
                    codegen.methodDesc(constructor, FunctionName.RTI),
                    codegen.methodDesc(constructor, FunctionName.Call),
                    codegen.methodDesc(constructor, constructName),
                    codegen.methodDesc(constructor, FunctionName.Init),
                    codegen.methodDesc(constructor, FunctionName.Code));
        } else {
            debugInfo(codegen.newMethod(constructor, FunctionName.DebugInfo),
                    codegen.methodDesc(constructor, FunctionName.RTI),
                    codegen.methodDesc(constructor, FunctionName.Call),
                    codegen.methodDesc(callConstructor, FunctionName.Init),
                    codegen.methodDesc(callConstructor, FunctionName.Code),
                    codegen.methodDesc(constructor, constructName),
                    codegen.methodDesc(constructor, FunctionName.Init),
                    codegen.methodDesc(constructor, FunctionName.Code));
        }
    }

    private void debugInfo(Script node) {
        debugInfo(codegen.newMethod(node, ScriptName.DebugInfo),
                codegen.methodDesc(node, ScriptName.RTI),
                codegen.methodDesc(node, ScriptName.Eval),
                codegen.methodDesc(node, ScriptName.Init),
                codegen.methodDesc(node, ScriptName.Code));
    }

    private void debugInfo(Module node) {
        debugInfo(codegen.newMethod(node, ModuleName.DebugInfo),
                codegen.methodDesc(node, ModuleName.RTI),
                codegen.methodDesc(node, ModuleName.Init),
                codegen.methodDesc(node, ModuleName.Code));
    }

    private void debugInfo(MethodCode code, MethodName... names) {
        InstructionAssembler asm = new InstructionAssembler(code);
        asm.begin();

        asm.anew(Types.DebugInfo, Methods.DebugInfo_init);
        for (MethodName name : names) {
            asm.dup();
            asm.handle(name);
            asm.invoke(Methods.DebugInfo_addMethod);
        }

        asm._return();
        asm.end();
    }
}
