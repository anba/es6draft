/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import java.util.List;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import com.github.anba.es6draft.ast.DefaultNodeVisitor;
import com.github.anba.es6draft.ast.Node;
import com.github.anba.es6draft.ast.Script;
import com.github.anba.es6draft.ast.StatementListItem;
import com.github.anba.es6draft.compiler.MethodGenerator.Register;

/**
 *
 */
class ScriptGenerator extends DefaultNodeVisitor<Void, MethodGenerator> {
    private final CodeGenerator codegen;

    public ScriptGenerator(CodeGenerator codegen) {
        this.codegen = codegen;
    }

    @Override
    protected Void visit(Node node, MethodGenerator mv) {
        node.accept(codegen, mv);
        return null;
    }

    private MethodGenerator scriptGenerator(Script node) {
        return codegen.scriptGenerator(node);
    }

    private MethodGenerator scriptGenerator(Script node, int index) {
        return codegen.scriptGenerator(node, index);
    }

    private static final int STATEMENTS_THRESHOLD = 300;

    @Override
    public Void visit(Script node, MethodGenerator mv) {
        assert mv == null;

        // initialisation method
        scriptInit(node);
        evalScriptInit(node);

        // runtime method
        if (node.getStatements().size() < STATEMENTS_THRESHOLD) {
            singleScript(node);
        } else {
            multiScript(node);
        }

        // runtime-info method
        new RuntimeInfoGenerator(codegen).runtimeInfo(node);

        return null;
    }

    private void scriptInit(Script node) {
        String methodName = "script_init";
        String desc = Type.getMethodDescriptor(Type.VOID_TYPE, Types.Realm,
                Types.LexicalEnvironment, Type.BOOLEAN_TYPE);
        MethodVisitor mv = codegen.publicStaticMethod(methodName, desc);
        InstructionVisitor init = new InstructionVisitor(mv, methodName, desc);
        init.initVariable(0, Types.Realm);
        init.initVariable(1, Types.LexicalEnvironment);
        init.initVariable(2, Type.BOOLEAN_TYPE);
        init.visitCode();
        // - start -
        new GlobalDeclarationInstantiationGenerator(codegen).generate(node, init);
        init.areturn(Type.VOID_TYPE);
        // - end -
        init.visitMaxs(0, 0);
        init.visitEnd();
    }

    private void evalScriptInit(Script node) {
        String methodName = "script_evalinit";
        String desc = Type.getMethodDescriptor(Type.VOID_TYPE, Types.Realm,
                Types.LexicalEnvironment, Types.LexicalEnvironment, Type.BOOLEAN_TYPE);
        MethodVisitor mv = codegen.publicStaticMethod(methodName, desc);
        InstructionVisitor init = new InstructionVisitor(mv, methodName, desc);
        init.initVariable(0, Types.Realm);
        init.initVariable(1, Types.LexicalEnvironment);
        init.initVariable(2, Types.LexicalEnvironment);
        init.initVariable(3, Type.BOOLEAN_TYPE);
        init.visitCode();
        // - start -
        new EvalDeclarationInstantiationGenerator(codegen).generate(node, init);
        init.areturn(Type.VOID_TYPE);
        // - end -
        init.visitMaxs(0, 0);
        init.visitEnd();
    }

    private void singleScript(Script node) {
        MethodGenerator mv = scriptGenerator(node);
        mv.visitCode();

        mv.getstatic(Fields.Undefined_UNDEFINED);
        mv.store(Register.CompletionValue);
        mv.load(Register.ExecutionContext);
        mv.invokevirtual(Methods.ExecutionContext_getRealm);
        mv.store(Register.Realm);
        for (StatementListItem stmt : node.getStatements()) {
            stmt.accept(this, mv);
        }

        mv.load(Register.CompletionValue);
        mv.areturn(Types.Object);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void multiScript(Script node) {
        // split script into several parts to pass all test262 test cases
        String desc = Type.getMethodDescriptor(Types.Object, Types.ExecutionContext, Types.Object);

        List<StatementListItem> statements = node.getStatements();
        int num = statements.size();
        int index = 0;
        for (int end = 0, start = 0; end < num; start += STATEMENTS_THRESHOLD, ++index) {
            end = Math.min(start + STATEMENTS_THRESHOLD, num);
            MethodGenerator mv = scriptGenerator(node, index);
            mv.visitCode();

            mv.load(Register.ExecutionContext);
            mv.invokevirtual(Methods.ExecutionContext_getRealm);
            mv.store(Register.Realm);
            for (StatementListItem stmt : statements.subList(start, end)) {
                stmt.accept(this, mv);
            }

            mv.load(Register.CompletionValue);
            mv.areturn(Types.Object);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        MethodGenerator mv = scriptGenerator(node);
        mv.visitCode();
        mv.getstatic(Fields.Undefined_UNDEFINED);
        mv.store(Register.CompletionValue);
        for (int i = 0; i < index; ++i) {
            mv.load(Register.ExecutionContext);
            mv.load(Register.CompletionValue);
            mv.invokestatic(codegen.getClassName(), "script_" + i, desc);
            mv.store(Register.CompletionValue);
        }
        mv.load(Register.CompletionValue);
        mv.areturn(Types.Object);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
}
