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
import com.github.anba.es6draft.compiler.assembler.Variable;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.GlobalEnvironmentRecord;
import com.github.anba.es6draft.runtime.LexicalEnvironment;

/**
 * <h1>15 ECMAScript Language: Scripts and Modules</h1><br>
 * <h2>15.1 Scripts</h2>
 * <ul>
 * <li>15.1.8 Runtime Semantics: GlobalDeclarationInstantiation (script, env)
 * </ul>
 */
final class GlobalDeclarationInstantiationGenerator extends
        DeclarationBindingInstantiationGenerator {
    private static final int EXECUTION_CONTEXT = 0;
    private static final int GLOBAL_ENV = 1;

    private static final class GlobalDeclInitMethodGenerator extends ExpressionVisitor {
        GlobalDeclInitMethodGenerator(MethodCode method, Script node) {
            super(method, IsStrict(node), false, false);
        }

        @Override
        public void begin() {
            super.begin();
            setParameterName("cx", EXECUTION_CONTEXT, Types.ExecutionContext);
            setParameterName("globalEnv", GLOBAL_ENV, Types.LexicalEnvironment);
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
        // Only generate global-script-init when needed.
        if (!script.isEvalScript()) {
            generate(script, mv);
        } else {
            generateExceptionThrower(mv);
        }
        mv.end();
    }

    private void generate(Script script, InstructionVisitor mv) {
        Variable<ExecutionContext> context = mv.getParameter(EXECUTION_CONTEXT,
                ExecutionContext.class);
        Variable<LexicalEnvironment<GlobalEnvironmentRecord>> env = mv.getParameter(GLOBAL_ENV,
                LexicalEnvironment.class).uncheckedCast();

        Variable<GlobalEnvironmentRecord> envRec = mv.newVariable("envRec",
                GlobalEnvironmentRecord.class);
        storeEnvironmentRecord(envRec, env, mv);

        /* steps 1-2 (omitted) */
        /* step 3 */
        Set<Name> lexNames = LexicallyDeclaredNames(script); // note: unordered set!
        /* step 4 */
        Set<Name> varNames = VarDeclaredNames(script); // note: unordered set!
        /* step 5 */
        for (Name name : lexNames) {
            canDeclareLexicalScopedOrThrow(context, envRec, name, mv);
        }
        /* step 6 */
        for (Name name : varNames) {
            canDeclareVarScopedOrThrow(context, envRec, name, mv);
        }
        /* step 7 */
        List<StatementListItem> varDeclarations = VarScopedDeclarations(script);
        /* step 8 */
        ArrayDeque<HoistableDeclaration> functionsToInitialize = new ArrayDeque<>();
        /* step 9 */
        HashSet<Name> declaredFunctionNames = new HashSet<>();
        /* step 10 */
        for (StatementListItem item : reverse(varDeclarations)) {
            if (item instanceof HoistableDeclaration) {
                HoistableDeclaration d = (HoistableDeclaration) item;
                Name fn = BoundName(d);
                if (declaredFunctionNames.add(fn)) {
                    canDeclareGlobalFunctionOrThrow(context, envRec, fn, mv);
                    functionsToInitialize.addFirst(d);
                }
            }
        }
        /* step 11 */
        LinkedHashSet<Name> declaredVarNames = new LinkedHashSet<>();
        /* step 12 */
        for (StatementListItem d : varDeclarations) {
            if (d instanceof VariableStatement) {
                for (Name vn : BoundNames((VariableStatement) d)) {
                    if (!declaredFunctionNames.contains(vn)) {
                        canDeclareGlobalVarOrThrow(context, envRec, vn, mv);
                        declaredVarNames.add(vn);
                    }
                }
            }
        }
        /* step 13 (note) */
        /* step 14 */
        List<Declaration> lexDeclarations = LexicallyScopedDeclarations(script);
        /* step 15 */
        for (Declaration d : lexDeclarations) {
            assert !(d instanceof HoistableDeclaration);
            for (Name dn : BoundNames(d)) {
                if (d.isConstDeclaration()) {
                    createImmutableBinding(envRec, dn, true, mv);
                } else {
                    createMutableBinding(envRec, dn, false, mv);
                }
            }
        }
        /* steps 16 */
        for (HoistableDeclaration f : functionsToInitialize) {
            Name fn = BoundName(f);
            // stack: [] -> [fo]
            InstantiateFunctionObject(context, env, f, mv);
            createGlobalFunctionBinding(envRec, fn, false, mv);
        }
        /* step 17 */
        for (Name vn : declaredVarNames) {
            createGlobalVarBinding(envRec, vn, false, mv);
        }
        /* step 18 */
        mv._return();
    }
}
