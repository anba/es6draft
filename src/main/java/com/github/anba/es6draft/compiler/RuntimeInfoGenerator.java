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

import org.objectweb.asm.Type;

import com.github.anba.es6draft.ast.FunctionExpression;
import com.github.anba.es6draft.ast.FunctionNode;
import com.github.anba.es6draft.ast.GeneratorComprehension;
import com.github.anba.es6draft.ast.GeneratorExpression;
import com.github.anba.es6draft.ast.MethodDefinition;
import com.github.anba.es6draft.ast.Script;
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
        // class: RuntimeInfo
        static final MethodDesc RTI_newScriptBody = MethodDesc.create(MethodType.Static,
                Types.RuntimeInfo, "newScriptBody", Type.getMethodType(
                        Types.RuntimeInfo$ScriptBody, Types.String, Type.BOOLEAN_TYPE,
                        Types.MethodHandle, Types.MethodHandle, Types.MethodHandle));

        static final MethodDesc RTI_newFunction = MethodDesc.create(MethodType.Static,
                Types.RuntimeInfo, "newFunction", Type.getMethodType(Types.RuntimeInfo$Function,
                        Types.String, Type.INT_TYPE, Type.INT_TYPE, Types.String,
                        Types.MethodHandle, Types.MethodHandle));
    }

    private final CodeGenerator codegen;

    RuntimeInfoGenerator(CodeGenerator codegen) {
        this.codegen = codegen;
    }

    private int functionFlags(GeneratorComprehension node) {
        int functionFlags = 0;
        // TODO: revisit decision to censor .caller by making generator expr strict by default?
        functionFlags |= FunctionFlags.Strict.getValue();
        functionFlags |= FunctionFlags.Generator.getValue();
        if (node.hasSyntheticNodes()) {
            functionFlags |= FunctionFlags.SyntheticMethods.getValue();
        }
        return functionFlags;
    }

    private int functionFlags(FunctionNode node, boolean tailCall) {
        boolean strict = IsStrict(node);
        boolean generator = isGenerator(node);
        boolean legacy = !strict && codegen.isEnabled(CompatibilityOption.FunctionPrototype);
        int functionFlags = 0;
        if (strict) {
            functionFlags |= FunctionFlags.Strict.getValue();
        }
        if (hasSuperReference(node)) {
            functionFlags |= FunctionFlags.Super.getValue();
        }
        if (hasScopedName(node)) {
            functionFlags |= FunctionFlags.ScopedName.getValue();
        }
        if (generator) {
            functionFlags |= FunctionFlags.Generator.getValue();
        }
        if (node.hasSyntheticNodes()) {
            functionFlags |= FunctionFlags.SyntheticMethods.getValue();
        }
        if (tailCall) {
            assert !generator && strict;
            functionFlags |= FunctionFlags.TailCall.getValue();
        }
        if (legacy) {
            functionFlags |= FunctionFlags.Legacy.getValue();
        }
        return functionFlags;
    }

    private static boolean hasSuperReference(FunctionNode node) {
        if (node instanceof MethodDefinition) {
            return ((MethodDefinition) node).hasSuperReference();
        } else {
            return false;
        }
    }

    private static boolean hasScopedName(FunctionNode node) {
        if (node instanceof FunctionExpression) {
            return ((FunctionExpression) node).getIdentifier() != null;
        } else if (node instanceof GeneratorExpression) {
            return ((GeneratorExpression) node).getIdentifier() != null;
        } else {
            return false;
        }
    }

    private static boolean isGenerator(FunctionNode node) {
        return node.isGenerator();
    }

    private static <T> T get(Future<T> future) {
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    void runtimeInfo(GeneratorComprehension node, Future<String> source) {
        final String functionName = "";
        final int expectedArgumentCount = 0;

        InstructionVisitor mv = new InstructionVisitor(codegen.newMethod(node, FunctionName.RTI));
        mv.begin();

        mv.aconst(functionName);
        mv.iconst(functionFlags(node));
        mv.iconst(expectedArgumentCount);
        mv.aconst(get(source));
        mv.handle(codegen.methodDesc(node, FunctionName.Code));
        mv.handle(codegen.methodDesc(node, FunctionName.Call));
        mv.invoke(Methods.RTI_newFunction);
        mv.areturn();

        mv.end();
    }

    void runtimeInfo(FunctionNode node, boolean tailCall, Future<String> source) {
        InstructionVisitor mv = new InstructionVisitor(codegen.newMethod(node, FunctionName.RTI));
        mv.begin();

        mv.aconst(node.getFunctionName());
        mv.iconst(functionFlags(node, tailCall));
        mv.iconst(ExpectedArgumentCount(node.getParameters()));
        mv.aconst(get(source));
        mv.handle(codegen.methodDesc(node, FunctionName.Code));
        mv.handle(codegen.methodDesc(node, FunctionName.Call));
        mv.invoke(Methods.RTI_newFunction);
        mv.areturn();

        mv.end();
    }

    void runtimeInfo(Script node) {
        InstructionVisitor mv = new InstructionVisitor(codegen.newMethod(node, ScriptName.RTI));
        mv.begin();

        mv.aconst(node.getSourceFile());
        mv.iconst(IsStrict(node));
        mv.handle(codegen.methodDesc(node, ScriptName.Init));
        mv.handle(codegen.methodDesc(node, ScriptName.EvalInit));
        mv.handle(codegen.methodDesc(node, ScriptName.Code));
        mv.invoke(Methods.RTI_newScriptBody);
        mv.areturn();

        mv.end();
    }
}
