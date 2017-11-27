/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static com.github.anba.es6draft.semantics.StaticSemantics.*;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.HashSet;
import java.util.List;
import java.util.function.Function;

import com.github.anba.es6draft.ast.*;
import com.github.anba.es6draft.ast.Module;
import com.github.anba.es6draft.ast.scope.Name;
import com.github.anba.es6draft.compiler.CodeGenerator.FunctionCode;
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
        // class: RuntimeInfo
        static final MethodName RTI_newScriptBody = MethodName.findStatic(Types.RuntimeInfo, "newScriptBody",
                Type.methodType(Types.RuntimeInfo$ScriptBody, Types.MethodHandle));

        static final MethodName RTI_newScriptBodyDebug = MethodName.findStatic(Types.RuntimeInfo, "newScriptBody",
                Type.methodType(Types.RuntimeInfo$ScriptBody, Types.MethodHandle, Types.MethodHandle));

        static final MethodName RTI_newModuleBody = MethodName.findStatic(Types.RuntimeInfo, "newModuleBody",
                Type.methodType(Types.RuntimeInfo$ModuleBody, Types.MethodHandle, Types.MethodHandle));

        static final MethodName RTI_newModuleBodyDebug = MethodName.findStatic(Types.RuntimeInfo, "newModuleBody", Type
                .methodType(Types.RuntimeInfo$ModuleBody, Types.MethodHandle, Types.MethodHandle, Types.MethodHandle));

        static final MethodName RTI_newFunction = MethodName.findStatic(Types.RuntimeInfo, "newFunction",
                Type.methodType(Types.RuntimeInfo$Function, Types.Object, Types.String, Type.INT_TYPE, Type.INT_TYPE,
                        Types.String_, Types.String, Types.MethodHandle, Types.MethodHandle, Types.MethodHandle));

        static final MethodName RTI_newFunctionDebug = MethodName.findStatic(Types.RuntimeInfo, "newFunction",
                Type.methodType(Types.RuntimeInfo$Function, Types.Object, Types.String, Type.INT_TYPE, Type.INT_TYPE,
                        Types.String_, Types.String, Types.MethodHandle, Types.MethodHandle, Types.MethodHandle,
                        Types.MethodHandle));
    }

    private final CodeGenerator codegen;

    RuntimeInfoGenerator(CodeGenerator codegen) {
        this.codegen = codegen;
    }

    private static final Handle RUNTIME_INFO_BOOTSTRAP = MethodName
            .findStatic(RuntimeInfo.class, "bootstrap",
                    MethodType.methodType(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class))
            .toHandle();

    static void runtimeInfo(Script node, MethodCode method, MethodName scriptCode,
            Function<MethodName, MethodName> debugInfo) {
        InstructionAssembler asm = new InstructionAssembler(method);
        asm.begin();

        asm.handle(scriptCode);
        if (debugInfo != null) {
            asm.handle(debugInfo.apply(method.name()));
            asm.invoke(Methods.RTI_newScriptBodyDebug);
        } else {
            asm.invoke(Methods.RTI_newScriptBody);
        }

        asm._return();
        asm.end();
    }

    static void runtimeInfo(Module node, MethodCode method, MethodName moduleInit, MethodName moduleBody,
            Function<MethodName, MethodName> debugInfo) {
        InstructionAssembler asm = new InstructionAssembler(method);
        asm.begin();

        asm.handle(moduleInit);
        asm.handle(moduleBody);
        if (debugInfo != null) {
            asm.handle(debugInfo.apply(method.name()));
            asm.invoke(Methods.RTI_newModuleBodyDebug);
        } else {
            asm.invoke(Methods.RTI_newModuleBody);
        }

        asm._return();
        asm.end();
    }

    void runtimeInfo(FunctionNode node, MethodCode method, FunctionCode call, FunctionCode construct, String source,
            Function<MethodName, MethodName> debugInfo) {
        InstructionAssembler asm = new InstructionAssembler(method);
        asm.begin();

        asm.invokedynamic("methodInfo", Type.methodType(Types.Object), RUNTIME_INFO_BOOTSTRAP);
        asm.aconst(node.getFunctionName());
        asm.iconst(functionFlags(node, call.tailCall, construct != null && construct.tailCall));
        asm.iconst(ExpectedArgumentCount(node.getParameters()));
        if (hasMappedOrLegacyArguments(node)) {
            // TODO: Make this a compact string (to save bytecode and memory size)?
            newStringArray(asm, mappedNames(node.getParameters()));
        } else {
            asm.anull();
        }
        asm.aconst(source);
        if (node.isAsync() || node.isGenerator()) {
            asm.handle(call.body);
        } else {
            asm.anull();
        }
        asm.handle(call.entry);
        if (construct != null) {
            asm.handle(construct.entry);
        } else {
            asm.anull();
        }
        if (debugInfo != null) {
            asm.handle(debugInfo.apply(method.name()));
            asm.invoke(Methods.RTI_newFunctionDebug);
        } else {
            asm.invoke(Methods.RTI_newFunction);
        }

        asm._return();
        asm.end();
    }

    private int functionFlags(FunctionNode node, boolean tailCall, boolean tailConstruct) {
        boolean strict = IsStrict(node);
        int functionFlags = 0;
        if (strict) {
            functionFlags |= FunctionFlags.Strict.getValue();
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
        if (node instanceof MethodDefinition) {
            MethodDefinition method = (MethodDefinition) node;
            if (method.isClassConstructor()) {
                functionFlags |= FunctionFlags.Class.getValue();
            } else {
                functionFlags |= FunctionFlags.Method.getValue();
            }
            if (method.getType() == MethodDefinition.MethodType.Getter) {
                functionFlags |= FunctionFlags.Getter.getValue();
            }
            if (method.getType() == MethodDefinition.MethodType.Setter) {
                functionFlags |= FunctionFlags.Setter.getValue();
            }
        }
        if (!IsStrict(node) && isLegacy(node)) {
            functionFlags |= FunctionFlags.Legacy.getValue();
        }
        if (hasScopedName(node)) {
            functionFlags |= FunctionFlags.ScopedName.getValue();
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
        if (hasMappedOrLegacyArguments(node)) {
            functionFlags |= FunctionFlags.MappedArguments.getValue();
        }
        return functionFlags;
    }

    private boolean isLegacy(FunctionNode node) {
        if (!(node instanceof FunctionDeclaration || node instanceof FunctionExpression)) {
            return false;
        }
        return codegen.isEnabled(CompatibilityOption.FunctionArguments)
                || codegen.isEnabled(CompatibilityOption.FunctionCaller);
    }

    private static boolean hasScopedName(FunctionNode node) {
        return node instanceof Expression && node.getIdentifier() != null;
    }

    private boolean hasLegacyArguments(FunctionNode node) {
        if (!(node instanceof FunctionDeclaration || node instanceof FunctionExpression)) {
            return false;
        }
        return codegen.isEnabled(CompatibilityOption.FunctionArguments);
    }

    private boolean hasMappedOrLegacyArguments(FunctionNode node) {
        // Strict or arrow functions never have mapped arguments.
        if (IsStrict(node) || node.getThisMode() == FunctionNode.ThisMode.Lexical) {
            return false;
        }
        // Functions with non-simple parameters (or no parameters at all) also never have mapped arguments.
        FormalParameterList formals = node.getParameters();
        if (formals.getFormals().isEmpty() || !IsSimpleParameterList(formals)) {
            return false;
        }
        // Legacy functions always need the argument name mapping.
        if (hasLegacyArguments(node)) {
            return true;
        }
        // No mapping needed when 'arguments' is never accessed.
        boolean argumentsObjectNeeded = node.getScope().needsArguments();
        Name arguments = node.getScope().arguments();
        if (!argumentsObjectNeeded || arguments == null) {
            return false;
        }
        // Or a parameter named 'arguments' is present.
        if (BoundNames(formals).contains(arguments)) {
            return false;
        }
        // Or a lexical variable named 'arguments' is present.
        if (LexicallyDeclaredNames(node).contains(arguments)) {
            return false;
        }
        // Or a function named 'arguments' is present.
        for (StatementListItem item : VarScopedDeclarations(node)) {
            if (item instanceof HoistableDeclaration) {
                HoistableDeclaration d = (HoistableDeclaration) item;
                if (arguments.equals(BoundName(d))) {
                    return false;
                }
            }
        }
        return true;
    }

    private static String[] mappedNames(FormalParameterList formals) {
        List<FormalParameter> list = formals.getFormals();
        int numberOfParameters = list.size();
        HashSet<String> mappedNames = new HashSet<>();
        String[] names = new String[numberOfParameters];
        for (int index = numberOfParameters - 1; index >= 0; --index) {
            BindingElementItem element = list.get(index).getElement();
            assert element instanceof BindingElement : element.getClass().toString();
            Binding binding = ((BindingElement) element).getBinding();
            assert binding instanceof BindingIdentifier : binding.getClass().toString();
            String name = ((BindingIdentifier) binding).getName().getIdentifier();
            if (mappedNames.add(name)) {
                names[index] = name;
            }
        }
        return names;
    }

    private static void newStringArray(InstructionAssembler mv, String[] strings) {
        mv.anewarray(strings.length, Types.String);
        int index = 0;
        for (String string : strings) {
            mv.astore(index++, string);
        }
    }
}
