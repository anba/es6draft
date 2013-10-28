/**
 * Copyright (c) 2012-2013 André Bargull
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
import com.github.anba.es6draft.ast.GeneratorDefinition;
import com.github.anba.es6draft.ast.GeneratorExpression;
import com.github.anba.es6draft.ast.MethodDefinition;
import com.github.anba.es6draft.ast.Script;
import com.github.anba.es6draft.compiler.CodeGenerator.FunctionName;
import com.github.anba.es6draft.compiler.CodeGenerator.ScriptName;
import com.github.anba.es6draft.compiler.InstructionVisitor.MethodDesc;
import com.github.anba.es6draft.compiler.InstructionVisitor.MethodType;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo.FunctionFlags;

/**
 * 
 */
class RuntimeInfoGenerator {
    private static class Methods {
        // class: RuntimeInfo
        static final MethodDesc RTI_newScriptBody = MethodDesc.create(MethodType.Static,
                Types.RuntimeInfo, "newScriptBody", Type.getMethodType(
                        Types.RuntimeInfo$ScriptBody, Type.BOOLEAN_TYPE, Types.MethodHandle,
                        Types.MethodHandle, Types.MethodHandle));

        static final MethodDesc RTI_newFunction = MethodDesc.create(MethodType.Static,
                Types.RuntimeInfo, "newFunction", Type.getMethodType(Types.RuntimeInfo$Function,
                        Types.String, Type.INT_TYPE, Type.INT_TYPE, Types.MethodHandle,
                        Types.MethodHandle, Types.String));
    }

    private final CodeGenerator codegen;

    RuntimeInfoGenerator(CodeGenerator codegen) {
        this.codegen = codegen;
    }

    private static int functionFlags(FunctionNode node) {
        int functionFlags = 0;
        if (IsStrict(node)) {
            functionFlags |= FunctionFlags.Strict.getValue();
        }
        if (hasSuperReference(node)) {
            functionFlags |= FunctionFlags.Super.getValue();
        }
        if (hasScopedName(node)) {
            functionFlags |= FunctionFlags.ScopedName.getValue();
        }
        if (isGenerator(node)) {
            functionFlags |= FunctionFlags.Generator.getValue();
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
        if (node instanceof GeneratorDefinition) {
            return true;
        } else if (node instanceof MethodDefinition) {
            return ((MethodDefinition) node).getType() == MethodDefinition.MethodType.Generator;
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

    void runtimeInfo(FunctionNode node, Future<String> source) {
        String className = codegen.getClassName();
        InstructionVisitor mv = codegen.publicStaticMethod(
                codegen.methodName(node, FunctionName.RTI),
                codegen.methodType(node, FunctionName.RTI));

        mv.begin();

        mv.aconst(node.getFunctionName());
        mv.iconst(functionFlags(node));
        mv.iconst(ExpectedArgumentCount(node.getParameters()));
        mv.invokeStaticMH(className, codegen.methodName(node, FunctionName.Init),
                codegen.methodDescriptor(node, FunctionName.Init));
        mv.invokeStaticMH(className, codegen.methodName(node, FunctionName.Code),
                codegen.methodDescriptor(node, FunctionName.Code));
        mv.aconst(get(source));
        mv.invoke(Methods.RTI_newFunction);
        mv.areturn();

        mv.end();
    }

    void runtimeInfo(Script node) {
        String className = codegen.getClassName();
        InstructionVisitor mv = codegen.publicStaticMethod(
                codegen.methodName(node, ScriptName.RTI), codegen.methodType(node, ScriptName.RTI));

        mv.begin();

        mv.iconst(IsStrict(node));
        mv.invokeStaticMH(className, codegen.methodName(node, ScriptName.Init),
                codegen.methodDescriptor(node, ScriptName.Init));
        mv.invokeStaticMH(className, codegen.methodName(node, ScriptName.EvalInit),
                codegen.methodDescriptor(node, ScriptName.EvalInit));
        mv.invokeStaticMH(className, codegen.methodName(node, ScriptName.Code),
                codegen.methodDescriptor(node, ScriptName.Code));
        mv.invoke(Methods.RTI_newScriptBody);
        mv.areturn();

        mv.end();
    }
}
