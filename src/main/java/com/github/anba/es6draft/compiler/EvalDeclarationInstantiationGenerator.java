/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static com.github.anba.es6draft.semantics.StaticSemantics.*;

import java.util.List;

import org.objectweb.asm.Label;
import org.objectweb.asm.Type;

import com.github.anba.es6draft.ast.Declaration;
import com.github.anba.es6draft.ast.FunctionDeclaration;
import com.github.anba.es6draft.ast.GeneratorDeclaration;
import com.github.anba.es6draft.ast.Script;
import com.github.anba.es6draft.ast.StatementListItem;
import com.github.anba.es6draft.ast.VariableStatement;
import com.github.anba.es6draft.compiler.CodeGenerator.ScriptName;
import com.github.anba.es6draft.compiler.InstructionVisitor.MethodDesc;
import com.github.anba.es6draft.compiler.InstructionVisitor.MethodType;
import com.github.anba.es6draft.compiler.InstructionVisitor.Variable;
import com.github.anba.es6draft.runtime.EnvironmentRecord;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.LexicalEnvironment;

/**
 * <h1>10 Executable Code and Execution Contexts</h1><br>
 * <h2>10.5 Declaration Binding Instantiation</h2>
 * <ul>
 * <li>10.5.5 Eval Declaration Instantiation
 * </ul>
 */
class EvalDeclarationInstantiationGenerator extends DeclarationBindingInstantiationGenerator {
    private static class Methods {
        // class: IllegalStateException
        static final MethodDesc IllegalStateException_init = MethodDesc.create(MethodType.Special,
                Types.IllegalStateException, "<init>", Type.getMethodType(Type.VOID_TYPE));

        // class: LexicalEnvironment
        static final MethodDesc LexicalEnvironment_getEnvRec = MethodDesc.create(
                MethodType.Virtual, Types.LexicalEnvironment, "getEnvRec",
                Type.getMethodType(Types.EnvironmentRecord));

        // class: ScriptRuntime
        static final MethodDesc ScriptRuntime_bindingNotPresentOrThrow = MethodDesc.create(
                MethodType.Static, Types.ScriptRuntime, "bindingNotPresentOrThrow", Type
                        .getMethodType(Type.VOID_TYPE, Types.ExecutionContext,
                                Types.EnvironmentRecord, Types.String));
    }

    private static class EvalDeclInitMethodGenerator extends ExpressionVisitor {
        static final Type methodDescriptor = Type.getMethodType(Type.VOID_TYPE,
                Types.ExecutionContext, Types.LexicalEnvironment, Types.LexicalEnvironment,
                Type.BOOLEAN_TYPE);

        EvalDeclInitMethodGenerator(CodeGenerator codegen, String methodName, boolean strict) {
            super(codegen.publicStaticMethod(methodName, methodDescriptor.getInternalName()),
                    methodName, methodDescriptor, strict, false);
        }
    }

    EvalDeclarationInstantiationGenerator(CodeGenerator codegen) {
        super(codegen);
    }

    private static final int EXECUTION_CONTEXT = 0;
    private static final int LEX_ENV = 1;
    private static final int VAR_ENV = 2;
    private static final int DELETABLE_BINDINGS = 3;

    void generate(Script evalScript) {
        String methodName = codegen.methodName(evalScript, ScriptName.EvalInit);
        ExpressionVisitor mv = new EvalDeclInitMethodGenerator(codegen, methodName,
                IsStrict(evalScript));

        mv.lineInfo(evalScript.getLine());
        mv.begin();
        // only generate eval-script-init when requested
        if (evalScript.isEvalScript()) {
            generate(evalScript, mv);
        } else {
            generateExceptionThrower(mv);
        }
        mv.end();
    }

    private void generateExceptionThrower(ExpressionVisitor mv) {
        mv.anew(Types.IllegalStateException);
        mv.dup();
        mv.invoke(Methods.IllegalStateException_init);
        mv.athrow();
    }

    private void generate(Script evalScript, ExpressionVisitor mv) {
        // FIXME: spec incomplete (using modified ES5.1 algorithm for now...)

        Variable<ExecutionContext> context = mv.getParameter(EXECUTION_CONTEXT,
                ExecutionContext.class);
        Variable<LexicalEnvironment> lexEnv = mv.getParameter(LEX_ENV, LexicalEnvironment.class);
        Variable<LexicalEnvironment> varEnv = mv.getParameter(VAR_ENV, LexicalEnvironment.class);
        Variable<Boolean> deletableBindings = mv.getParameter(DELETABLE_BINDINGS, boolean.class);

        Variable<LexicalEnvironment> env = varEnv;
        Variable<EnvironmentRecord> envRec = mv.newVariable("envRec", EnvironmentRecord.class);
        mv.load(env);
        mv.invoke(Methods.LexicalEnvironment_getEnvRec);
        mv.store(envRec);

        Variable<EnvironmentRecord> lexEnvRec = mv
                .newVariable("lexEnvRec", EnvironmentRecord.class);
        mv.load(lexEnv);
        mv.invoke(Methods.LexicalEnvironment_getEnvRec);
        mv.store(lexEnvRec);

        // begin-modification
        for (String name : LexicallyDeclaredNames(evalScript)) {
            mv.load(context);
            mv.load(lexEnvRec);
            mv.aconst(name);
            mv.invoke(Methods.ScriptRuntime_bindingNotPresentOrThrow);
        }
        // end-modification

        /* step 1-2 (not applicable) */
        /* step 3 */
        boolean strict = evalScript.isStrict();
        /* step 4 (not applicable) */
        /* step 5 */
        List<StatementListItem> varDeclarations = VarScopedDeclarations(evalScript);
        for (StatementListItem item : varDeclarations) {
            if (item instanceof FunctionDeclaration) {
                FunctionDeclaration f = (FunctionDeclaration) item;
                String fn = BoundName(f);

                // stack: [] -> [fo]
                InstantiateFunctionObject(context, lexEnv, f, mv);

                hasBinding(envRec, fn, mv);

                Label funcAlreadyDeclared = new Label(), after = new Label();
                mv.ifne(funcAlreadyDeclared);
                createMutableBinding(envRec, fn, deletableBindings, mv);
                initialiseBinding(envRec, fn, mv);
                mv.goTo(after);
                mv.mark(funcAlreadyDeclared);
                setMutableBinding(envRec, fn, strict, mv);
                mv.mark(after);
            }
        }
        /* step 6-7 (not applicable) */
        /* step 8 */
        for (StatementListItem d : varDeclarations) {
            if (d instanceof VariableStatement) {
                for (String dn : BoundNames(d)) {
                    hasBinding(envRec, dn, mv);

                    Label varAlreadyDeclared = new Label();
                    mv.ifne(varAlreadyDeclared);
                    createMutableBinding(envRec, dn, deletableBindings, mv);
                    mv.loadUndefined();
                    // setMutableBinding(envRec, dn, strict, mv);
                    initialiseBinding(envRec, dn, mv);
                    mv.mark(varAlreadyDeclared);
                }
            }
        }

        // begin-modification
        for (Declaration d : LexicallyScopedDeclarations(evalScript)) {
            for (String dn : BoundNames(d)) {
                if (d.isConstDeclaration()) {
                    createImmutableBinding(lexEnvRec, dn, mv);
                } else {
                    createMutableBinding(lexEnvRec, dn, false, mv);
                }
            }
            if (d instanceof GeneratorDeclaration) {
                String fn = BoundName(d);
                // stack: [] -> [fo]
                InstantiateGeneratorObject(context, lexEnv, (GeneratorDeclaration) d, mv);
                // setMutableBinding(lexEnvRec, fn, false, mv);
                initialiseBinding(lexEnvRec, fn, mv);
            }
        }
        // end-modification

        mv.areturn();
    }
}
