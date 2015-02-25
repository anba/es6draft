/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static com.github.anba.es6draft.semantics.StaticSemantics.*;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.github.anba.es6draft.ast.Declaration;
import com.github.anba.es6draft.ast.HoistableDeclaration;
import com.github.anba.es6draft.ast.Script;
import com.github.anba.es6draft.ast.StatementListItem;
import com.github.anba.es6draft.ast.VariableStatement;
import com.github.anba.es6draft.ast.scope.Name;
import com.github.anba.es6draft.compiler.CodeGenerator.ScriptName;
import com.github.anba.es6draft.compiler.assembler.Code.MethodCode;
import com.github.anba.es6draft.compiler.assembler.Jump;
import com.github.anba.es6draft.compiler.assembler.MethodName;
import com.github.anba.es6draft.compiler.assembler.Type;
import com.github.anba.es6draft.compiler.assembler.Variable;
import com.github.anba.es6draft.runtime.DeclarativeEnvironmentRecord;
import com.github.anba.es6draft.runtime.EnvironmentRecord;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.GlobalEnvironmentRecord;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.types.Undefined;
import com.github.anba.es6draft.runtime.types.builtins.FunctionObject;

/**
 * <h1>18 The Global Object</h1><br>
 * <h2>18.2 Function Properties of the Global Object</h2><br>
 * <h3>18.2.1 eval (x)</h3>
 * <ul>
 * <li>18.2.1.2 Runtime Semantics: EvalDeclarationInstantiation( body, varEnv, lexEnv, strict)
 * </ul>
 */
final class EvalDeclarationInstantiationGenerator extends DeclarationBindingInstantiationGenerator {
    private static final class Methods {
        // class: LexicalEnvironment
        static final MethodName DeclarativeEnvironmentRecord_isCatchEnvironment = MethodName
                .findVirtual(Types.DeclarativeEnvironmentRecord, "isCatchEnvironment",
                        Type.methodType(Type.BOOLEAN_TYPE));

        // class: LexicalEnvironment
        static final MethodName LexicalEnvironment_getOuter = MethodName.findVirtual(
                Types.LexicalEnvironment, "getOuter", Type.methodType(Types.LexicalEnvironment));

        // class: ScriptRuntime
        static final MethodName ScriptRuntime_canDeclareVarOrThrow = MethodName.findStatic(
                Types.ScriptRuntime, "canDeclareVarOrThrow", Type.methodType(Type.VOID_TYPE,
                        Types.ExecutionContext, Types.DeclarativeEnvironmentRecord, Types.String));
    }

    private static final int EXECUTION_CONTEXT = 0;
    private static final int VAR_ENV = 1;
    private static final int LEX_ENV = 2;

    private static final class EvalDeclInitMethodGenerator extends InstructionVisitor {
        EvalDeclInitMethodGenerator(MethodCode method) {
            super(method);
        }

        @Override
        public void begin() {
            super.begin();
            setParameterName("cx", EXECUTION_CONTEXT, Types.ExecutionContext);
            setParameterName("varEnv", VAR_ENV, Types.LexicalEnvironment);
            setParameterName("lexEnv", LEX_ENV, Types.LexicalEnvironment);
        }
    }

    EvalDeclarationInstantiationGenerator(CodeGenerator codegen) {
        super(codegen);
    }

    void generate(Script evalScript) {
        MethodCode method = codegen.newMethod(evalScript, ScriptName.EvalInit);
        InstructionVisitor mv = new EvalDeclInitMethodGenerator(method);

        mv.lineInfo(evalScript);
        mv.begin();
        // Only generate eval-script-init when needed.
        if (!evalScript.isEvalScript()) {
            generateExceptionThrower(mv);
        } else if (evalScript.isGlobalCode() && !evalScript.isStrict() && !evalScript.isScripting()) {
            generateGlobal(evalScript, mv);
        } else {
            generate(evalScript, mv);
        }
        mv.end();
    }

    private void generateGlobal(Script evalScript, InstructionVisitor mv) {
        assert evalScript.isGlobalCode() && !evalScript.isStrict() && !evalScript.isScripting();

        Variable<ExecutionContext> context = mv.getParameter(EXECUTION_CONTEXT,
                ExecutionContext.class);
        Variable<LexicalEnvironment<GlobalEnvironmentRecord>> varEnv = mv.getParameter(VAR_ENV,
                LexicalEnvironment.class).uncheckedCast();
        Variable<LexicalEnvironment<DeclarativeEnvironmentRecord>> lexEnv = mv.getParameter(
                LEX_ENV, LexicalEnvironment.class).uncheckedCast();

        /* step 1 */
        @SuppressWarnings("unused")
        Set<Name> lexNames = LexicallyDeclaredNames(evalScript);
        /* step 2 */
        Set<Name> varNames = VarDeclaredNames(evalScript);
        /* step 3 */
        List<StatementListItem> varDeclarations = VarScopedDeclarations(evalScript);
        /* step 4 */
        Variable<DeclarativeEnvironmentRecord> lexEnvRec = mv.newVariable("lexEnvRec",
                DeclarativeEnvironmentRecord.class);
        storeEnvironmentRecord(lexEnvRec, lexEnv, mv);
        /* step 5 */
        Variable<GlobalEnvironmentRecord> varEnvRec = mv.newVariable("varEnvRec",
                GlobalEnvironmentRecord.class);
        storeEnvironmentRecord(varEnvRec, varEnv, mv);
        /* step 6 */
        if (!varNames.isEmpty()) {
            /* step 6.a */
            for (Name name : varNames) {
                canDeclareVarScopedOrThrow(context, varEnvRec, name, mv);
            }
            /* steps 6.b-d */
            if (evalScript.isEnclosedByLexicalDeclaration()) {
                checkLexicalRedeclaration(context, varEnv, lexEnv, varNames,
                        evalScript.isEnclosedByWithStatement(), mv);
            }
        }
        /* step 7 */
        ArrayDeque<HoistableDeclaration> functionsToInitialize = new ArrayDeque<>();
        /* step 8 */
        HashSet<Name> declaredFunctionNames = new HashSet<>();
        /* step 9 */
        for (StatementListItem item : reverse(varDeclarations)) {
            if (item instanceof HoistableDeclaration) {
                HoistableDeclaration d = (HoistableDeclaration) item;
                Name fn = BoundName(d);
                if (declaredFunctionNames.add(fn)) {
                    canDeclareGlobalFunctionOrThrow(context, varEnvRec, fn, mv);
                    functionsToInitialize.addFirst(d);
                }
            }
        }
        /* step 10 */
        LinkedHashSet<Name> declaredVarNames = new LinkedHashSet<>();
        /* step 11 */
        for (StatementListItem d : varDeclarations) {
            if (d instanceof VariableStatement) {
                for (Name vn : BoundNames((VariableStatement) d)) {
                    if (!declaredFunctionNames.contains(vn)) {
                        canDeclareGlobalVarOrThrow(context, varEnvRec, vn, mv);
                        declaredVarNames.add(vn);
                    }
                }
            }
        }
        /* step 12 (note) */
        /* step 13 */
        List<Declaration> lexDeclarations = LexicallyScopedDeclarations(evalScript);
        /* step 14 */
        for (Declaration d : lexDeclarations) {
            assert !(d instanceof HoistableDeclaration);
            for (Name dn : BoundNames(d)) {
                if (d.isConstDeclaration()) {
                    createImmutableBinding(lexEnvRec, dn, true, mv);
                } else {
                    createMutableBinding(lexEnvRec, dn, false, mv);
                }
            }
        }
        /* steps 15 */
        for (HoistableDeclaration f : functionsToInitialize) {
            Name fn = BoundName(f);
            // stack: [] -> [fo]
            InstantiateFunctionObject(context, lexEnv, f, mv);
            createGlobalFunctionBinding(varEnvRec, fn, true, mv);
        }
        /* step 16 */
        for (Name vn : declaredVarNames) {
            createGlobalVarBinding(varEnvRec, vn, true, mv);
        }
        /* step 17 */
        mv._return();
    }

    private void generate(Script evalScript, InstructionVisitor mv) {
        assert evalScript.isFunctionCode() || evalScript.isStrict() || evalScript.isScripting();
        final boolean strict = IsStrict(evalScript);
        final boolean nonStrictOrScripting = !strict || evalScript.isScripting();

        Variable<ExecutionContext> context = mv.getParameter(EXECUTION_CONTEXT,
                ExecutionContext.class);
        Variable<LexicalEnvironment<EnvironmentRecord>> varEnv = mv.getParameter(VAR_ENV,
                LexicalEnvironment.class).uncheckedCast();
        Variable<LexicalEnvironment<DeclarativeEnvironmentRecord>> lexEnv = mv.getParameter(
                LEX_ENV, LexicalEnvironment.class).uncheckedCast();
        Variable<FunctionObject> fo = null;

        Variable<Undefined> undef = mv.newVariable("undef", Undefined.class);
        mv.loadUndefined();
        mv.store(undef);

        /* step 1 */
        @SuppressWarnings("unused")
        Set<Name> lexNames = LexicallyDeclaredNames(evalScript);
        /* step 2 */
        Set<Name> varNames = VarDeclaredNames(evalScript);
        /* step 3 */
        List<StatementListItem> varDeclarations = VarScopedDeclarations(evalScript);
        /* step 4 */
        Variable<DeclarativeEnvironmentRecord> lexEnvRec = mv.newVariable("lexEnvRec",
                DeclarativeEnvironmentRecord.class);
        storeEnvironmentRecord(lexEnvRec, lexEnv, mv);
        /* step 5 */
        Variable<EnvironmentRecord> varEnvRec = mv
                .newVariable("varEnvRec", EnvironmentRecord.class);
        storeEnvironmentRecord(varEnvRec, varEnv, mv);
        /* step 6 */
        if (!strict && !varNames.isEmpty() && evalScript.isEnclosedByLexicalDeclaration()) {
            /* step 6.a (not applicable) */
            /* steps 6.b-d */
            checkLexicalRedeclaration(context, varEnv, lexEnv, varNames,
                    evalScript.isEnclosedByWithStatement(), mv);
        }
        /* step 7 */
        ArrayDeque<HoistableDeclaration> functionsToInitialize = new ArrayDeque<>();
        /* step 8 */
        HashSet<Name> declaredFunctionNames = new HashSet<>();
        /* step 9 */
        for (StatementListItem item : reverse(varDeclarations)) {
            if (item instanceof HoistableDeclaration) {
                HoistableDeclaration d = (HoistableDeclaration) item;
                Name fn = BoundName(d);
                if (declaredFunctionNames.add(fn)) {
                    functionsToInitialize.addFirst(d);
                }
            }
        }
        if (!functionsToInitialize.isEmpty()) {
            fo = mv.newVariable("fo", FunctionObject.class);
        }
        /* step 10 */
        LinkedHashSet<Name> declaredVarNames = new LinkedHashSet<>();
        /* step 11 */
        for (StatementListItem d : varDeclarations) {
            if (d instanceof VariableStatement) {
                for (Name vn : BoundNames((VariableStatement) d)) {
                    if (!declaredFunctionNames.contains(vn)) {
                        declaredVarNames.add(vn);
                    }
                }
            }
        }
        /* step 12 (note) */
        /* step 13 */
        List<Declaration> lexDeclarations = LexicallyScopedDeclarations(evalScript);
        /* step 14 */
        for (Declaration d : lexDeclarations) {
            assert !(d instanceof HoistableDeclaration);
            for (Name dn : BoundNames(d)) {
                if (d.isConstDeclaration()) {
                    createImmutableBinding(lexEnvRec, dn, true, mv);
                } else {
                    createMutableBinding(lexEnvRec, dn, false, mv);
                }
            }
        }
        /* steps 15 */
        for (HoistableDeclaration f : functionsToInitialize) {
            Name fn = BoundName(f);

            // stack: [] -> []
            InstantiateFunctionObject(context, lexEnv, f, mv);
            mv.store(fo);

            if (nonStrictOrScripting) {
                hasBinding(varEnvRec, fn, mv);

                Jump funcAlreadyDeclared = new Jump(), after = new Jump();
                mv.ifne(funcAlreadyDeclared);
                createMutableBinding(varEnvRec, fn, true, mv);
                initializeBinding(varEnvRec, fn, fo, mv);
                mv.goTo(after);
                mv.mark(funcAlreadyDeclared);
                setMutableBinding(varEnvRec, fn, fo, strict, mv);
                mv.mark(after);
            } else {
                // Early error semantics ensure that fn does not already exist in varEnvRec.
                createMutableBinding(varEnvRec, fn, true, mv);
                initializeBinding(varEnvRec, fn, fo, mv);
            }
        }
        /* step 16 */
        for (Name vn : declaredVarNames) {
            if (nonStrictOrScripting) {
                hasBinding(varEnvRec, vn, mv);

                Jump varAlreadyDeclared = new Jump();
                mv.ifne(varAlreadyDeclared);
                createMutableBinding(varEnvRec, vn, true, mv);
                initializeBinding(varEnvRec, vn, undef, mv);
                mv.mark(varAlreadyDeclared);
            } else {
                // Early error semantics ensure that vn does not already exist in varEnvRec.
                createMutableBinding(varEnvRec, vn, true, mv);
                initializeBinding(varEnvRec, vn, undef, mv);
            }
        }
        /* step 17 */
        mv._return();
    }

    private void checkLexicalRedeclaration(Variable<ExecutionContext> context,
            Variable<? extends LexicalEnvironment<? extends EnvironmentRecord>> varEnv,
            Variable<LexicalEnvironment<DeclarativeEnvironmentRecord>> lexEnv, Set<Name> varNames,
            boolean hasWith, InstructionVisitor mv) {
        Variable<LexicalEnvironment<EnvironmentRecord>> thisLex = mv.newVariable("thisLex",
                LexicalEnvironment.class).uncheckedCast();
        Variable<EnvironmentRecord> thisEnvRec = mv.newVariable("thisEnvRec",
                EnvironmentRecord.class).uncheckedCast();
        Variable<DeclarativeEnvironmentRecord> envRec = mv.newVariable("envRec",
                DeclarativeEnvironmentRecord.class).uncheckedCast();
        boolean catchVar = codegen.isEnabled(CompatibilityOption.CatchVarStatement);

        Jump loopTest = new Jump(), loop = new Jump();
        mv.load(lexEnv);
        mv.store(thisLex);
        mv.nonDestructiveGoTo(loopTest);
        {
            mv.mark(loop);
            storeEnvironmentRecord(thisEnvRec, thisLex, mv);
            if (hasWith) {
                mv.load(thisEnvRec);
                mv.instanceOf(Types.ObjectEnvironmentRecord);
                mv.ifne(loopTest);
            }
            mv.load(thisEnvRec);
            mv.checkcast(Types.DeclarativeEnvironmentRecord);
            mv.store(envRec);
            if (catchVar) {
                mv.load(envRec);
                mv.invoke(Methods.DeclarativeEnvironmentRecord_isCatchEnvironment);
                mv.ifne(loopTest);
            }
            for (Name name : varNames) {
                mv.load(context);
                mv.load(envRec);
                mv.aconst(name.getIdentifier());
                mv.invoke(Methods.ScriptRuntime_canDeclareVarOrThrow);
            }
        }
        mv.mark(loopTest);
        mv.load(thisLex);
        mv.invoke(Methods.LexicalEnvironment_getOuter);
        mv.store(thisLex);
        mv.load(thisLex);
        mv.load(varEnv);
        mv.ifacmpne(loop);
    }
}
