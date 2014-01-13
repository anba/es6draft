/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static com.github.anba.es6draft.semantics.StaticSemantics.BoundNames;
import static com.github.anba.es6draft.semantics.StaticSemantics.IsStrict;
import static com.github.anba.es6draft.semantics.StaticSemantics.LexicallyScopedDeclarations;
import static com.github.anba.es6draft.semantics.StaticSemantics.VarScopedDeclarations;

import java.util.List;

import org.objectweb.asm.Label;
import org.objectweb.asm.Type;

import com.github.anba.es6draft.ast.Declaration;
import com.github.anba.es6draft.ast.FunctionDeclaration;
import com.github.anba.es6draft.ast.GeneratorDeclaration;
import com.github.anba.es6draft.ast.Script;
import com.github.anba.es6draft.ast.StatementListItem;
import com.github.anba.es6draft.ast.VariableStatement;
import com.github.anba.es6draft.compiler.Code.MethodCode;
import com.github.anba.es6draft.compiler.CodeGenerator.ScriptName;
import com.github.anba.es6draft.compiler.InstructionVisitor.MethodDesc;
import com.github.anba.es6draft.compiler.InstructionVisitor.MethodType;
import com.github.anba.es6draft.compiler.InstructionVisitor.Variable;
import com.github.anba.es6draft.runtime.EnvironmentRecord;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.LexicalEnvironment;

/**
 * <h1>18 The Global Object</h1><br>
 * <h2>18.2 Function Properties of the Global Object</h2><br>
 * <h3>18.2.1 eval (x)</h3>
 * <ul>
 * <li>18.2.1.2 Eval Declaration Instantiation
 * </ul>
 */
final class EvalDeclarationInstantiationGenerator extends DeclarationBindingInstantiationGenerator {
    private static final class Methods {
        // class: IllegalStateException
        static final MethodDesc IllegalStateException_init = MethodDesc.create(MethodType.Special,
                Types.IllegalStateException, "<init>", Type.getMethodType(Type.VOID_TYPE));
    }

    private static final int EXECUTION_CONTEXT = 0;
    private static final int VAR_ENV = 1;
    private static final int LEX_ENV = 2;
    private static final int DELETABLE_BINDINGS = 3;

    private static final class EvalDeclInitMethodGenerator extends ExpressionVisitor {
        EvalDeclInitMethodGenerator(MethodCode method, Script node) {
            super(method, IsStrict(node), false, false);
        }

        @Override
        public void begin() {
            super.begin();
            setParameterName("cx", EXECUTION_CONTEXT, Types.ExecutionContext);
            setParameterName("variableEnv", VAR_ENV, Types.LexicalEnvironment);
            setParameterName("lexicalEnv", LEX_ENV, Types.LexicalEnvironment);
            setParameterName("deletableBindings", DELETABLE_BINDINGS, Type.BOOLEAN_TYPE);
        }
    }

    EvalDeclarationInstantiationGenerator(CodeGenerator codegen) {
        super(codegen);
    }

    void generate(Script evalScript) {
        MethodCode method = codegen.newMethod(evalScript, ScriptName.EvalInit);
        ExpressionVisitor mv = new EvalDeclInitMethodGenerator(method, evalScript);

        mv.lineInfo(evalScript);
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
        Variable<LexicalEnvironment> varEnv = mv.getParameter(VAR_ENV, LexicalEnvironment.class);
        Variable<LexicalEnvironment> lexEnv = mv.getParameter(LEX_ENV, LexicalEnvironment.class);
        Variable<Boolean> deletableBindings = mv.getParameter(DELETABLE_BINDINGS, boolean.class);
        Variable<LexicalEnvironment> env = varEnv;

        Variable<EnvironmentRecord> envRec = mv.newVariable("envRec", EnvironmentRecord.class);
        getEnvironmentRecord(env, mv);
        mv.store(envRec);

        Variable<EnvironmentRecord> lexEnvRec = mv
                .newVariable("lexEnvRec", EnvironmentRecord.class);
        getEnvironmentRecord(lexEnv, mv);
        mv.store(lexEnvRec);

        // Lexical declarations are always placed into new lexical environment, see Eval#eval()
        // // begin-modification
        // for (String name : LexicallyDeclaredNames(evalScript)) {
        // mv.load(context);
        // mv.load(lexEnvRec);
        // mv.aconst(name);
        // mv.invoke(Methods.ScriptRuntime_bindingNotPresentOrThrow);
        // }
        // // end-modification

        /* steps 1-2 (not applicable) */
        /* step 3 */
        boolean strict = evalScript.isStrict();
        /* step 4 (not applicable) */
        /* step 5 */
        List<StatementListItem> varDeclarations = VarScopedDeclarations(evalScript);
        for (StatementListItem item : varDeclarations) {
            if (item instanceof FunctionDeclaration || item instanceof GeneratorDeclaration) {
                Declaration f = (Declaration) item;
                String fn = BoundName(f);

                // stack: [] -> [fo]
                if (f instanceof GeneratorDeclaration) {
                    InstantiateGeneratorObject(context, lexEnv, (GeneratorDeclaration) f, mv);
                } else {
                    InstantiateFunctionObject(context, lexEnv, (FunctionDeclaration) f, mv);
                }

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
        /* steps 6-7 (not applicable) */
        /* step 8 */
        for (StatementListItem d : varDeclarations) {
            if (d instanceof VariableStatement) {
                for (String dn : BoundNames((VariableStatement) d)) {
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
            assert !(d instanceof GeneratorDeclaration);
            for (String dn : BoundNames(d)) {
                if (d.isConstDeclaration()) {
                    createImmutableBinding(lexEnvRec, dn, mv);
                } else {
                    createMutableBinding(lexEnvRec, dn, false, mv);
                }
            }
        }
        // end-modification

        mv.areturn();
    }
}
