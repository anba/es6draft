/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static com.github.anba.es6draft.semantics.StaticSemantics.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.objectweb.asm.Type;

import com.github.anba.es6draft.ast.Declaration;
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
import com.github.anba.es6draft.runtime.GlobalEnvironmentRecord;
import com.github.anba.es6draft.runtime.LexicalEnvironment;

/**
 * <h1>15 ECMAScript Language: Modules and Scripts</h1><br>
 * <h2>15.2 Scripts</h2>
 * <ul>
 * <li>15.2.8 Runtime Semantics: GlobalDeclarationInstantiation
 * </ul>
 */
final class GlobalDeclarationInstantiationGenerator extends
        DeclarationBindingInstantiationGenerator {
    private static final class Methods {
        // class: ScriptRuntime
        static final MethodDesc ScriptRuntime_canDeclareLexicalScopedOrThrow = MethodDesc.create(
                MethodType.Static, Types.ScriptRuntime, "canDeclareLexicalScopedOrThrow", Type
                        .getMethodType(Type.VOID_TYPE, Types.ExecutionContext,
                                Types.GlobalEnvironmentRecord, Types.String));

        static final MethodDesc ScriptRuntime_canDeclareVarScopedOrThrow = MethodDesc.create(
                MethodType.Static, Types.ScriptRuntime, "canDeclareVarScopedOrThrow", Type
                        .getMethodType(Type.VOID_TYPE, Types.ExecutionContext,
                                Types.GlobalEnvironmentRecord, Types.String));

        static final MethodDesc ScriptRuntime_canDeclareGlobalFunctionOrThrow = MethodDesc.create(
                MethodType.Static, Types.ScriptRuntime, "canDeclareGlobalFunctionOrThrow", Type
                        .getMethodType(Type.VOID_TYPE, Types.ExecutionContext,
                                Types.GlobalEnvironmentRecord, Types.String));

        static final MethodDesc ScriptRuntime_canDeclareGlobalVarOrThrow = MethodDesc.create(
                MethodType.Static, Types.ScriptRuntime, "canDeclareGlobalVarOrThrow", Type
                        .getMethodType(Type.VOID_TYPE, Types.ExecutionContext,
                                Types.GlobalEnvironmentRecord, Types.String));

        // class: GlobalEnvironmentRecord
        static final MethodDesc GlobalEnvironmentRecord_createGlobalVarBinding = MethodDesc.create(
                MethodType.Virtual, Types.GlobalEnvironmentRecord, "createGlobalVarBinding",
                Type.getMethodType(Type.VOID_TYPE, Types.String, Type.BOOLEAN_TYPE));

        static final MethodDesc GlobalEnvironmentRecord_createGlobalFunctionBinding = MethodDesc
                .create(MethodType.Virtual, Types.GlobalEnvironmentRecord,
                        "createGlobalFunctionBinding", Type.getMethodType(Type.VOID_TYPE,
                                Types.String, Types.Object, Type.BOOLEAN_TYPE));
    }

    private static final int EXECUTION_CONTEXT = 0;
    private static final int GLOBAL_ENV = 1;
    private static final int LEXICAL_ENV = 2;
    private static final int DELETABLE_BINDINGS = 3;

    private static final class GlobalDeclInitMethodGenerator extends ExpressionVisitor {
        GlobalDeclInitMethodGenerator(MethodCode method, Script node) {
            super(method, IsStrict(node), false, false);
        }

        @Override
        public void begin() {
            super.begin();
            setParameterName("cx", EXECUTION_CONTEXT, Types.ExecutionContext);
            setParameterName("globalEnv", GLOBAL_ENV, Types.LexicalEnvironment);
            setParameterName("lexicalEnv", LEXICAL_ENV, Types.LexicalEnvironment);
            setParameterName("deletableBindings", DELETABLE_BINDINGS, Type.BOOLEAN_TYPE);
        }
    }

    GlobalDeclarationInstantiationGenerator(CodeGenerator codegen) {
        super(codegen);
    }

    void generate(Script script) {
        MethodCode method = codegen.newMethod(script, ScriptName.Init);
        ExpressionVisitor mv = new GlobalDeclInitMethodGenerator(method, script);

        mv.lineInfo(script);
        mv.begin();
        generate(script, mv);
        mv.end();
    }

    private void generate(Script script, InstructionVisitor mv) {
        Variable<ExecutionContext> context = mv.getParameter(EXECUTION_CONTEXT,
                ExecutionContext.class);
        Variable<LexicalEnvironment<?>> env = mv.getParameter(GLOBAL_ENV, LexicalEnvironment.class)
                .uncheckedCast();
        Variable<LexicalEnvironment<?>> lexEnv = mv.getParameter(LEXICAL_ENV,
                LexicalEnvironment.class).uncheckedCast();
        Variable<Boolean> deletableBindings = mv.getParameter(DELETABLE_BINDINGS, boolean.class);

        Variable<GlobalEnvironmentRecord> envRec = mv.newVariable("envRec",
                GlobalEnvironmentRecord.class);
        getEnvironmentRecord(env, mv);
        mv.checkcast(Types.GlobalEnvironmentRecord);
        mv.store(envRec);

        Variable<EnvironmentRecord> lexEnvRec = mv
                .newVariable("lexEnvRec", EnvironmentRecord.class);
        getEnvironmentRecord(lexEnv, mv);
        mv.store(lexEnvRec);

        // Throughout this algorithm `env == lexEnv` holds for ScriptEvaluation, the `env != lexEnv`
        // case applies only to EvalScriptEvaluation, cf. runtime.objects.Eval.

        /* step 1 */
        @SuppressWarnings("unused")
        boolean strict = script.isStrict();
        /* step 2 */
        Set<String> lexNames = LexicallyDeclaredNames(script); // note: unordered set!
        /* step 3 */
        Set<String> varNames = VarDeclaredNames(script); // note: unordered set!
        /* step 4 */
        if (script.isGlobalScope()) {
            // Perform this step only for ScriptEvaluation, EvalScriptEvaluation places lexical
            // declarations in a fresh environment
            for (String name : lexNames) {
                canDeclareLexicalScopedOrThrow(context, envRec, name, mv);
            }
        }
        /* step 5 */
        for (String name : varNames) {
            canDeclareVarScopedOrThrow(context, envRec, name, mv);
        }
        /* step 6 */
        List<StatementListItem> varDeclarations = VarScopedDeclarations(script);
        /* step 7 */
        List<Declaration> functionsToInitialise = new ArrayList<>();
        /* step 8 */
        Set<String> declaredFunctionNames = new HashSet<>();
        /* step 9 */
        for (StatementListItem item : reverse(varDeclarations)) {
            if (isFunctionDeclaration(item)) {
                Declaration d = (Declaration) item;
                String fn = BoundName(d);
                if (!declaredFunctionNames.contains(fn)) {
                    canDeclareGlobalFunctionOrThrow(context, envRec, fn, mv);
                    declaredFunctionNames.add(fn);
                    functionsToInitialise.add(d);
                }
            }
        }
        /* step 10 */
        Set<String> declaredVarNames = new HashSet<>();
        /* step 11 */
        for (StatementListItem d : varDeclarations) {
            if (d instanceof VariableStatement) {
                for (String vn : BoundNames((VariableStatement) d)) {
                    if (!declaredFunctionNames.contains(vn)) {
                        canDeclareGlobalVarOrThrow(context, envRec, vn, mv);
                        if (!declaredVarNames.contains(vn)) {
                            declaredVarNames.add(vn);
                        }
                    }
                }
            }
        }
        /* steps 12-13 */
        for (Declaration f : functionsToInitialise) {
            String fn = BoundName(f);
            // stack: [] -> [fo]
            InstantiateFunctionObject(context, lexEnv, f, mv);
            createGlobalFunctionBinding(envRec, fn, deletableBindings, mv);
        }
        /* step 14 */
        for (String vn : declaredVarNames) {
            createGlobalVarBinding(envRec, vn, deletableBindings, mv);
        }
        /* step 15 */
        List<Declaration> lexDeclarations = LexicallyScopedDeclarations(script);
        /* step 16 */
        for (Declaration d : lexDeclarations) {
            assert !isFunctionDeclaration(d);
            for (String dn : BoundNames(d)) {
                if (d.isConstDeclaration()) {
                    createImmutableBinding(lexEnvRec, dn, mv);
                } else {
                    createMutableBinding(lexEnvRec, dn, false, mv);
                }
            }
        }
        /* step 17 */
        mv.areturn();
    }

    /**
     * <code>
     * ScriptRuntime.canDeclareLexicalScopedOrThrow(cx, envRec, name)
     * </code>
     * 
     * @param context
     *            the variable which holds the execution context
     * @param envRec
     *            the variable which holds the environment record
     * @param name
     *            the binding name
     * @param mv
     *            the instruction visitor
     */
    private void canDeclareLexicalScopedOrThrow(Variable<ExecutionContext> context,
            Variable<GlobalEnvironmentRecord> envRec, String name, InstructionVisitor mv) {
        mv.load(context);
        mv.load(envRec);
        mv.aconst(name);
        mv.invoke(Methods.ScriptRuntime_canDeclareLexicalScopedOrThrow);
    }

    /**
     * <code>
     * ScriptRuntime.canDeclareVarScopedOrThrow(cx, envRec, name)
     * </code>
     * 
     * @param context
     *            the variable which holds the execution context
     * @param envRec
     *            the variable which holds the environment record
     * @param name
     *            the binding name
     * @param mv
     *            the instruction visitor
     */
    private void canDeclareVarScopedOrThrow(Variable<ExecutionContext> context,
            Variable<GlobalEnvironmentRecord> envRec, String name, InstructionVisitor mv) {
        mv.load(context);
        mv.load(envRec);
        mv.aconst(name);
        mv.invoke(Methods.ScriptRuntime_canDeclareVarScopedOrThrow);
    }

    /**
     * <code>
     * ScriptRuntime.canDeclareGlobalFunctionOrThrow(cx, envRec, name)
     * </code>
     * 
     * @param context
     *            the variable which holds the execution context
     * @param envRec
     *            the variable which holds the environment record
     * @param name
     *            the binding name
     * @param mv
     *            the instruction visitor
     */
    private void canDeclareGlobalFunctionOrThrow(Variable<ExecutionContext> context,
            Variable<GlobalEnvironmentRecord> envRec, String name, InstructionVisitor mv) {
        mv.load(context);
        mv.load(envRec);
        mv.aconst(name);
        mv.invoke(Methods.ScriptRuntime_canDeclareGlobalFunctionOrThrow);
    }

    /**
     * <code>
     * ScriptRuntime.canDeclareGlobalVarOrThrow(cx, envRec, name)
     * </code>
     * 
     * @param context
     *            the variable which holds the execution context
     * @param envRec
     *            the variable which holds the environment record
     * @param name
     *            the binding name
     * @param mv
     *            the instruction visitor
     */
    private void canDeclareGlobalVarOrThrow(Variable<ExecutionContext> context,
            Variable<GlobalEnvironmentRecord> envRec, String name, InstructionVisitor mv) {
        mv.load(context);
        mv.load(envRec);
        mv.aconst(name);
        mv.invoke(Methods.ScriptRuntime_canDeclareGlobalVarOrThrow);
    }

    /**
     * <code>
     * envRec.createGlobalVarBinding(name, deletableBindings)
     * </code>
     * 
     * @param envRec
     *            the variable which holds the environment record
     * @param name
     *            the binding name
     * @param deletableBindings
     *            the variable which holds the deletable flag
     * @param mv
     *            the instruction visitor
     */
    private void createGlobalVarBinding(Variable<GlobalEnvironmentRecord> envRec, String name,
            Variable<Boolean> deletableBindings, InstructionVisitor mv) {
        mv.load(envRec);
        mv.aconst(name);
        mv.load(deletableBindings);
        mv.invoke(Methods.GlobalEnvironmentRecord_createGlobalVarBinding);
    }

    /**
     * <code>
     * envRec.createGlobalFunctionBinding(name, functionObject, deletableBindings)
     * </code>
     * 
     * @param envRec
     *            the variable which holds the environment record
     * @param name
     *            the binding name
     * @param deletableBindings
     *            the variable which holds the deletable flag
     * @param mv
     *            the instruction visitor
     */
    private void createGlobalFunctionBinding(Variable<GlobalEnvironmentRecord> envRec, String name,
            Variable<Boolean> deletableBindings, InstructionVisitor mv) {
        // stack: [fo] -> []
        mv.load(envRec);
        mv.swap();
        mv.aconst(name);
        mv.swap();
        mv.load(deletableBindings);
        mv.invoke(Methods.GlobalEnvironmentRecord_createGlobalFunctionBinding);
    }
}
