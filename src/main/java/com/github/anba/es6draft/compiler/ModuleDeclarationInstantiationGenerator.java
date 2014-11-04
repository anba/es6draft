/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static com.github.anba.es6draft.semantics.StaticSemantics.BoundNames;
import static com.github.anba.es6draft.semantics.StaticSemantics.LexicallyScopedDeclarations;
import static com.github.anba.es6draft.semantics.StaticSemantics.VarScopedDeclarations;

import java.util.List;

import com.github.anba.es6draft.ast.Declaration;
import com.github.anba.es6draft.ast.HoistableDeclaration;
import com.github.anba.es6draft.ast.Module;
import com.github.anba.es6draft.ast.StatementListItem;
import com.github.anba.es6draft.ast.VariableStatement;
import com.github.anba.es6draft.ast.scope.Name;
import com.github.anba.es6draft.compiler.CodeGenerator.ModuleName;
import com.github.anba.es6draft.compiler.assembler.Code.MethodCode;
import com.github.anba.es6draft.compiler.assembler.Variable;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.ModuleEnvironmentRecord;
import com.github.anba.es6draft.runtime.types.Undefined;

/**
 * <h1>15 ECMAScript Language: Scripts and Modules</h1><br>
 * <h2>15.2 Modules</h2><br>
 * <h3>15.2.1 Module Semantics</h3>
 * <ul>
 * <li>15.2.1.21 Runtime Semantics: ModuleDeclarationInstantiation( module, realm, moduleSet )
 * </ul>
 */
final class ModuleDeclarationInstantiationGenerator extends
        DeclarationBindingInstantiationGenerator {
    private static final int EXECUTION_CONTEXT = 0;
    private static final int MODULE_ENV = 1;

    private static final class ModuleDeclInitMethodGenerator extends ExpressionVisitor {
        ModuleDeclInitMethodGenerator(MethodCode method, Module module) {
            super(method, true, false, false);
        }

        @Override
        public void begin() {
            super.begin();
            setParameterName("cx", EXECUTION_CONTEXT, Types.ExecutionContext);
            setParameterName("moduleEnv", MODULE_ENV, Types.LexicalEnvironment);
        }
    }

    ModuleDeclarationInstantiationGenerator(CodeGenerator codegen) {
        super(codegen);
    }

    void generate(Module module) {
        MethodCode method = codegen.newMethod(module, ModuleName.Init);
        ExpressionVisitor mv = new ModuleDeclInitMethodGenerator(method, module);

        mv.lineInfo(module);
        mv.begin();
        generate(module, mv);
        mv.end();
    }

    private void generate(Module module, ExpressionVisitor mv) {
        Variable<ExecutionContext> context = mv.getParameter(EXECUTION_CONTEXT,
                ExecutionContext.class);
        Variable<LexicalEnvironment<ModuleEnvironmentRecord>> env = mv.getParameter(MODULE_ENV,
                LexicalEnvironment.class).uncheckedCast();

        Variable<ModuleEnvironmentRecord> envRec = mv.newVariable("envRec",
                ModuleEnvironmentRecord.class);
        storeEnvironmentRecord(envRec, env, mv);

        Variable<Undefined> undef = mv.newVariable("undef", Undefined.class);
        mv.loadUndefined();
        mv.store(undef);

        /* steps 1-8 */
        /* step 9 */
        List<StatementListItem> varDeclarations = VarScopedDeclarations(module);
        /* step 10 */
        for (StatementListItem d : varDeclarations) {
            assert d instanceof VariableStatement;
            for (Name dn : BoundNames((VariableStatement) d)) {
                createMutableBinding(envRec, dn, false, mv);
                initializeBinding(envRec, dn, undef, mv);
            }
        }
        /* step 11 */
        // FIXME: LexicallyScopedDeclarations includes ImportDeclaration!
        // FIXME: LexicallyScopedDeclarations includes ExportDeclaration!
        List<Declaration> lexDeclarations = LexicallyScopedDeclarations(module);
        /* step 12 */
        for (Declaration d : lexDeclarations) {
            for (Name dn : BoundNames(d)) {
                if (d.isConstDeclaration()) {
                    createImmutableBinding(envRec, dn, mv);
                } else {
                    createMutableBinding(envRec, dn, false, mv);
                }

                if (d instanceof HoistableDeclaration) {
                    // stack: [] -> [envRec, name]
                    mv.load(envRec);
                    mv.aconst(dn.getIdentifier());

                    // stack: [envRec, name] -> [envRec, name, fo]
                    InstantiateFunctionObject(context, env, d, mv);

                    // stack: [envRec, name, fo] -> []
                    initializeBinding(mv);
                }
            }
        }
        // Emit binding for 'export default AssignmentExpression'.
        if (module.getScope().getDefaultExportExpression() != null) {
            createImmutableBinding(envRec, new Name("*default*"), mv);
        }
        /* step 13 */
        mv._return();
    }
}
