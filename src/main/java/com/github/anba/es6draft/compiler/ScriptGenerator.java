/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import java.util.List;

import org.objectweb.asm.Type;

import com.github.anba.es6draft.ast.DefaultNodeVisitor;
import com.github.anba.es6draft.ast.Node;
import com.github.anba.es6draft.ast.Script;
import com.github.anba.es6draft.ast.StatementListItem;
import com.github.anba.es6draft.compiler.CodeGenerator.Register;

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

        List<StatementListItem> statements = node.getStatements();
        int num = statements.size();
        if (num < STATEMENTS_THRESHOLD) {
            mv = scriptGenerator(node);
            mv.visitCode();

            mv.getstatic(Fields.Undefined_UNDEFINED);
            mv.store(Register.CompletionValue);
            mv.load(Register.ExecutionContext);
            mv.invokevirtual(Methods.ExecutionContext_getRealm);
            mv.store(Register.Realm);
            for (StatementListItem stmt : statements) {
                stmt.accept(this, mv);
            }

            mv.load(Register.CompletionValue);
            mv.areturn(Types.Object);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        } else {
            // split script into several parts to pass all test262 test cases
            String desc = Type.getMethodDescriptor(Types.Object, Types.ExecutionContext,
                    Types.Object);

            int index = 0;
            for (int end = 0, start = 0; end < num; start += STATEMENTS_THRESHOLD, ++index) {
                end = Math.min(start + STATEMENTS_THRESHOLD, num);
                mv = scriptGenerator(node, index);
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

            mv = scriptGenerator(node);
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

        new RuntimeInfoGenerator(codegen).runtimeInfo(node);

        return null;
    }
}
