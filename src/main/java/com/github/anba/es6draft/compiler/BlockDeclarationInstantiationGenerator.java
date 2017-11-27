/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static com.github.anba.es6draft.semantics.StaticSemantics.*;

import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;

import com.github.anba.es6draft.ast.BlockStatement;
import com.github.anba.es6draft.ast.Declaration;
import com.github.anba.es6draft.ast.FunctionDeclaration;
import com.github.anba.es6draft.ast.HoistableDeclaration;
import com.github.anba.es6draft.ast.Node;
import com.github.anba.es6draft.ast.SwitchStatement;
import com.github.anba.es6draft.ast.scope.Name;
import com.github.anba.es6draft.compiler.assembler.Code.MethodCode;
import com.github.anba.es6draft.compiler.assembler.MethodName;
import com.github.anba.es6draft.compiler.assembler.MethodTypeDescriptor;
import com.github.anba.es6draft.compiler.assembler.Type;
import com.github.anba.es6draft.compiler.assembler.Variable;
import com.github.anba.es6draft.runtime.DeclarativeEnvironmentRecord;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.types.builtins.FunctionObject;

/**
 * <h1>13 ECMAScript Language: Statements and Declarations</h1><br>
 * <h2>13.2 Block</h2>
 * <ul>
 * <li>13.2.14 Runtime Semantics: BlockDeclarationInstantiation( code, env )
 * </ul>
 */
final class BlockDeclarationInstantiationGenerator extends DeclarationBindingInstantiationGenerator {
    private static final int INLINE_LIMIT = 1 << 5;
    private static final int METHOD_LIMIT = 1 << 12;

    BlockDeclarationInstantiationGenerator(CodeGenerator codegen) {
        super(codegen);
    }

    /**
     * stack: [env] {@literal ->} [env]
     * 
     * @param node
     *            the block statement
     * @param mv
     *            the code visitor
     */
    void generate(BlockStatement node, CodeVisitor mv) {
        int declarations = LexicallyDeclaredNames(node).size();
        if (declarations > INLINE_LIMIT) {
            MethodName method = mv.compile(node, this::blockStatement);

            // stack: [env] -> [env]
            mv.dup();
            mv.loadExecutionContext();
            mv.invoke(method);
        } else {
            generateInline(LexicallyScopedDeclarations(node), mv);
        }
    }

    /**
     * stack: [env] {@literal ->} [env]
     * 
     * @param node
     *            the switch statement
     * @param mv
     *            the code visitor
     */
    void generate(SwitchStatement node, CodeVisitor mv) {
        int declarations = LexicallyDeclaredNames(node).size();
        if (declarations > INLINE_LIMIT) {
            MethodName method = mv.compile(node, this::switchStatement);

            // stack: [env] -> [env]
            mv.dup();
            mv.loadExecutionContext();
            mv.invoke(method);
        } else {
            generateInline(LexicallyScopedDeclarations(node), mv);
        }
    }

    private void generateInline(List<Declaration> declarations, CodeVisitor mv) {
        mv.enterVariableScope();
        Variable<LexicalEnvironment<DeclarativeEnvironmentRecord>> env = mv.newVariable("env", LexicalEnvironment.class)
                .uncheckedCast();

        // stack: [env] -> []
        mv.store(env);

        generate(declarations, new HashSet<>(), mv.executionContext(), env, mv);

        // stack: [] -> [env]
        mv.load(env);

        mv.exitVariableScope();
    }

    private MethodName blockStatement(BlockStatement node) {
        return blockInstantiation(node,
                mv -> generateOrSplit(node, LexicallyScopedDeclarations(node), new HashSet<>(), mv));
    }

    private MethodName switchStatement(SwitchStatement node) {
        return blockInstantiation(node,
                mv -> generateOrSplit(node, LexicallyScopedDeclarations(node), new HashSet<>(), mv));
    }

    private void generateOrSplit(Node node, List<Declaration> declarations, HashSet<Name> instantiatedNames,
            BlockInstantiationVisitor mv) {
        Variable<ExecutionContext> cx = mv.getExecutionContext();
        Variable<LexicalEnvironment<DeclarativeEnvironmentRecord>> env = mv.getLexicalEnvironment();

        if (declarations.size() <= METHOD_LIMIT) {
            generate(declarations, instantiatedNames, cx, env, mv);
        } else {
            for (int i = 0, size = declarations.size(); i < size; i += METHOD_LIMIT) {
                List<Declaration> sublist = declarations.subList(i, Math.min(i + METHOD_LIMIT, size));

                MethodName method = compile(node, sublist, instantiatedNames);
                mv.load(env);
                mv.load(cx);
                mv.invoke(method);
            }
        }
    }

    private void generate(List<Declaration> declarations, HashSet<Name> instantiatedNames,
            Variable<ExecutionContext> cx, Variable<LexicalEnvironment<DeclarativeEnvironmentRecord>> env,
            InstructionVisitor mv) {
        assert declarations.size() <= METHOD_LIMIT : declarations.size();
        Variable<DeclarativeEnvironmentRecord> envRec = mv.newVariable("envRec", DeclarativeEnvironmentRecord.class);
        Variable<FunctionObject> fo = null;

        getEnvironmentRecord(env, envRec, mv);

        /* steps 1-2 */
        for (Declaration d : declarations) {
            if (!(d instanceof HoistableDeclaration)) {
                for (Name dn : BoundNames(d)) {
                    BindingOp<DeclarativeEnvironmentRecord> op = BindingOp.of(envRec, dn);
                    if (IsConstantDeclaration(d)) {
                        op.createImmutableBinding(envRec, dn, true, mv);
                    } else {
                        op.createMutableBinding(envRec, dn, false, mv);
                    }
                }
            } else {
                Name fn = BoundName((HoistableDeclaration) d);
                BindingOp<DeclarativeEnvironmentRecord> op = BindingOp.of(envRec, fn);

                boolean alreadyDeclared = !instantiatedNames.add(fn);
                assert !alreadyDeclared || d instanceof FunctionDeclaration;

                if (!alreadyDeclared) {
                    op.createMutableBinding(envRec, fn, false, mv);
                }

                InstantiateFunctionObject(cx, env, d, mv);
                if (fo == null) {
                    fo = mv.newVariable("fo", FunctionObject.class);
                }
                mv.store(fo);

                if (!alreadyDeclared) {
                    op.initializeBinding(envRec, fn, fo, mv);
                } else {
                    op.setMutableBinding(envRec, fn, fo, false, mv);
                }
            }
        }
    }

    private MethodName compile(Node node, List<Declaration> declarations, HashSet<Name> instantiatedNames) {
        return blockInstantiation(node, mv -> {
            Variable<ExecutionContext> cx = mv.getExecutionContext();
            Variable<LexicalEnvironment<DeclarativeEnvironmentRecord>> env = mv.getLexicalEnvironment();

            generate(declarations, instantiatedNames, cx, env, mv);
        });
    }

    private MethodName blockInstantiation(Node node, Consumer<BlockInstantiationVisitor> compiler) {
        MethodCode method = codegen.method("!block", BlockInstantiationVisitor.BlockInstantiation);
        BlockInstantiationVisitor body = new BlockInstantiationVisitor(method);
        body.lineInfo(node);
        body.begin();

        compiler.accept(body);

        body._return();
        body.end();
        return method.name();
    }

    private static final class BlockInstantiationVisitor extends InstructionVisitor {
        static final MethodTypeDescriptor BlockInstantiation = Type.methodType(Type.VOID_TYPE, Types.LexicalEnvironment,
                Types.ExecutionContext);

        BlockInstantiationVisitor(MethodCode method) {
            super(method);
        }

        @Override
        public void begin() {
            super.begin();
            setParameterName("env", 0, Types.LexicalEnvironment);
            setParameterName("cx", 1, Types.ExecutionContext);
        }

        Variable<LexicalEnvironment<DeclarativeEnvironmentRecord>> getLexicalEnvironment() {
            return getParameter(0, LexicalEnvironment.class).uncheckedCast();
        }

        Variable<ExecutionContext> getExecutionContext() {
            return getParameter(1, ExecutionContext.class);
        }
    }
}
