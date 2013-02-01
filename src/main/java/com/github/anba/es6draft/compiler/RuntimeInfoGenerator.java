/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static com.github.anba.es6draft.semantics.StaticSemantics.*;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import com.github.anba.es6draft.ast.*;
import com.github.anba.es6draft.ast.MethodDefinition.MethodType;
import com.github.anba.es6draft.compiler.InstructionVisitor.MethodDesc;

/**
 * 
 */
public class RuntimeInfoGenerator {

    private static class Methods {
        // class: RuntimeInfo
        static MethodDesc RTI_newCode = MethodDesc.create(Types.RuntimeInfo, "newCode", Type
                .getMethodType(Types.RuntimeInfo$Code, Types.String,
                        Types.RuntimeInfo$Declaration_, Types.String_,
                        Types.RuntimeInfo$Declaration_, Types.Map, Types.MethodHandle));

        static MethodDesc RTI_newScriptBody = MethodDesc.create(Types.RuntimeInfo, "newScriptBody",
                Type.getMethodType(Types.RuntimeInfo$ScriptBody, Type.BOOLEAN_TYPE, Types.String_,
                        Types.RuntimeInfo$Declaration_, Types.String_,
                        Types.RuntimeInfo$Declaration_, Types.Map, Types.MethodHandle));

        static MethodDesc RTI_newFunction = MethodDesc.create(Types.RuntimeInfo, "newFunction",
                Type.getMethodType(Types.RuntimeInfo$Function, Types.String, Type.BOOLEAN_TYPE,
                        Type.BOOLEAN_TYPE, Type.BOOLEAN_TYPE,
                        Types.RuntimeInfo$FormalParameterList, Types.RuntimeInfo$Code));

        static MethodDesc RTI_newFormalParameterList = MethodDesc.create(Types.RuntimeInfo,
                "newFormalParameterList", Type.getMethodType(Types.RuntimeInfo$FormalParameterList,
                        Types.String_, Type.INT_TYPE, Type.INT_TYPE,
                        Types.RuntimeInfo$FormalParameter_, Types.MethodHandle));

        static MethodDesc RTI_newFormalParameter = MethodDesc.create(Types.RuntimeInfo,
                "newFormalParameter", Type.getMethodType(Types.RuntimeInfo$FormalParameter,
                        Types.String_, Type.BOOLEAN_TYPE));

        static MethodDesc RTI_newDeclaration = MethodDesc.create(Types.RuntimeInfo,
                "newDeclaration", Type.getMethodType(Types.RuntimeInfo$Declaration, Types.String_,
                        Type.BOOLEAN_TYPE, Type.BOOLEAN_TYPE, Type.BOOLEAN_TYPE));

        // class: HashMap
        static MethodDesc HashMap_put = MethodDesc.create(Types.HashMap, "put",
                Type.getMethodType(Types.Object, Types.Object, Types.Object));

        static MethodDesc HashMap_init = MethodDesc.create(Types.HashMap, "<init>",
                Type.getMethodType(Type.VOID_TYPE));

        // Method descriptors

        static String binding = Type.getMethodDescriptor(Type.VOID_TYPE, Types.ExecutionContext,
                Types.Scriptable, Types.LexicalEnvironment);
        static String functionCode = Type.getMethodDescriptor(Types.Object, Types.ExecutionContext);
        static String scriptCode = Type.getMethodDescriptor(Types.Object, Types.ExecutionContext);
        static String returnsFunction = Type.getMethodDescriptor(Types.RuntimeInfo$Function);
        static String returnsScriptBody = Type.getMethodDescriptor(Types.RuntimeInfo$ScriptBody);
    }

    private final CodeGenerator codegen;

    RuntimeInfoGenerator(CodeGenerator codegen) {
        this.codegen = codegen;
    }

    private static boolean isGenerator(FunctionNode node) {
        if (node instanceof GeneratorDefinition) {
            return true;
        } else if (node instanceof MethodDefinition) {
            return ((MethodDefinition) node).getType() == MethodType.Generator;
        } else {
            return false;
        }
    }

    private static boolean hasSuperReference(FunctionNode node) {
        if (node instanceof MethodDefinition) {
            return ((MethodDefinition) node).hasSuperReference();
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
        String name = node.accept(FunctionName.INSTANCE, null);
        boolean isGenerator = isGenerator(node);
        boolean hasSuperReference = hasSuperReference(node);

        String className = codegen.getClassName();
        String methodName = codegen.methodName(node);

        MethodVisitor rti = codegen
                .publicStaticMethod(methodName + "_rti", Methods.returnsFunction);
        InstructionVisitor mv = new InstructionVisitor(rti, methodName + "_rti",
                Methods.returnsFunction);

        mv.visitCode();
        // -start-
        mv.anew(Types.HashMap);
        mv.dup();
        mv.invokespecial(Methods.HashMap_init);
        mv.store(0, Types.HashMap);

        mv.aconst(name);
        mv.iconst(isGenerator);
        mv.iconst(hasSuperReference);
        mv.iconst(IsStrict(node));
        {
            // FormalParameterList
            FormalParameterList parameters = node.getParameters();
            astore_string(mv, BoundNames(parameters));
            mv.iconst(ExpectedArgumentCount(parameters));
            mv.iconst(NumberOfParameters(parameters));
            {
                // FormalParameter
                astore_parameters(mv, parameters.getFormals());
            }
            mv.invokeStaticMH(className, methodName + "_binding", Methods.binding);
            mv.invokestatic(Methods.RTI_newFormalParameterList);
        }
        {
            // Code
            // FIXME: use "" instead of "_LexicalDeclarations" (spec bug?)
            mv.aconst(get(source));
            astore_declarations(mv, LexicallyScopedDeclarations(node));
            astore_string(mv, VarDeclaredNames(node));
            astore_declarations(mv, VarScopedDeclarations(node));
            mv.load(0, Types.HashMap);
            mv.invokeStaticMH(className, methodName, Methods.functionCode);
            mv.invokestatic(Methods.RTI_newCode);
        }
        mv.invokestatic(Methods.RTI_newFunction);

        mv.areturn(Types.Object);
        // -end-
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    void runtimeInfo(Script node) {
        String className = codegen.getClassName();
        MethodVisitor rti = codegen.publicStaticMethod("script_rti", Methods.returnsScriptBody);
        InstructionVisitor mv = new InstructionVisitor(rti, "script_rti", Methods.returnsScriptBody);

        mv.visitCode();
        // -start-
        mv.anew(Types.HashMap);
        mv.dup();
        mv.invokespecial(Methods.HashMap_init);
        mv.store(0, Types.HashMap);

        mv.iconst(IsStrict(node));
        astore_string(mv, LexicallyDeclaredNames(node));
        astore_declarations(mv, LexicallyScopedDeclarations(node));
        astore_string(mv, VarDeclaredNames(node));
        astore_declarations(mv, VarScopedDeclarations(node));
        mv.load(0, Types.HashMap);
        mv.invokeStaticMH(className, "script", Methods.scriptCode);
        mv.invokestatic(Methods.RTI_newScriptBody);

        mv.areturn(Types.Object);
        // -end-
        mv.visitMaxs(1, 1);
        mv.visitEnd();
    }

    private void astore_parameters(InstructionVisitor mv, List<FormalParameter> list) {
        mv.iconst(list.size());
        mv.newarray(Types.RuntimeInfo$FormalParameter);
        for (int i = 0, size = list.size(); i < size; ++i) {
            FormalParameter node = list.get(i);
            mv.dup();
            mv.iconst(i);
            astore_string(mv, BoundNames(node));
            mv.iconst(isBindingIdentifier(node));
            mv.invokestatic(Methods.RTI_newFormalParameter);
            mv.astore(Types.RuntimeInfo$FormalParameter);
        }
    }

    private void astore_declarations(InstructionVisitor mv,
            Collection<? extends StatementListItem> list) {
        String className = codegen.getClassName();
        mv.iconst(list.size());
        mv.newarray(Types.RuntimeInfo$Declaration);
        int index = 0;
        for (StatementListItem node : list) {
            boolean funDecl = isFunctionDecl(node);
            mv.dup();
            mv.iconst(index++);
            astore_string(mv, BoundNames(node));
            mv.iconst(node instanceof Declaration && IsConstantDeclaration((Declaration) node));
            mv.iconst(funDecl);
            mv.iconst(!funDecl);
            mv.invokestatic(Methods.RTI_newDeclaration);
            if (funDecl) {
                String methodName;
                if (node instanceof FunctionDeclaration) {
                    methodName = codegen.methodName((FunctionDeclaration) node);
                } else {
                    assert node instanceof GeneratorDeclaration;
                    methodName = codegen.methodName((GeneratorDeclaration) node);
                }
                mv.dup();
                mv.load(0, Types.HashMap);
                mv.swap();
                mv.invokeStaticMH(className, methodName + "_rti", Methods.returnsFunction);
                mv.invokevirtual(Methods.HashMap_put);
                mv.pop();
            }
            mv.astore(Types.RuntimeInfo$Declaration);
        }
    }

    private void astore_string(InstructionVisitor mv, Collection<String> list) {
        mv.iconst(list.size());
        mv.newarray(Types.String);
        int index = 0;
        for (String string : list) {
            mv.dup();
            mv.iconst(index++);
            mv.aconst(string);
            mv.astore(Types.String);
        }
    }

    private static boolean isBindingIdentifier(FormalParameter formal) {
        if (formal instanceof BindingElement) {
            return ((BindingElement) formal).getBinding() instanceof BindingIdentifier;
        }
        return false;
    }

    private static boolean isFunctionDecl(Node node) {
        return node instanceof FunctionDeclaration || node instanceof GeneratorDeclaration;
    }
}
