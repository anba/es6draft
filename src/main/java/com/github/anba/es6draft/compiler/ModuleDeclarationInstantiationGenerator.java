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
import java.util.Map;

import com.github.anba.es6draft.ast.Declaration;
import com.github.anba.es6draft.ast.ExportDefaultExpression;
import com.github.anba.es6draft.ast.HoistableDeclaration;
import com.github.anba.es6draft.ast.Module;
import com.github.anba.es6draft.ast.StatementListItem;
import com.github.anba.es6draft.ast.VariableStatement;
import com.github.anba.es6draft.ast.scope.Name;
import com.github.anba.es6draft.compiler.CodeGenerator.ModuleName;
import com.github.anba.es6draft.compiler.assembler.Code.MethodCode;
import com.github.anba.es6draft.compiler.assembler.MethodName;
import com.github.anba.es6draft.compiler.assembler.Type;
import com.github.anba.es6draft.compiler.assembler.Variable;
import com.github.anba.es6draft.runtime.EnvironmentRecord;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.ModuleEnvironmentRecord;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.modules.ExportEntry;
import com.github.anba.es6draft.runtime.modules.ImportEntry;
import com.github.anba.es6draft.runtime.modules.ModuleExport;
import com.github.anba.es6draft.runtime.modules.ModuleRecord;
import com.github.anba.es6draft.runtime.types.Undefined;

/**
 * <h1>15 ECMAScript Language: Scripts and Modules</h1><br>
 * <h2>15.2 Modules</h2><br>
 * <h3>15.2.1 Module Semantics</h3>
 * <ul>
 * <li>15.2.1.18 Runtime Semantics: ModuleDeclarationInstantiation( module, realm, moduleSet )
 * </ul>
 */
final class ModuleDeclarationInstantiationGenerator extends
        DeclarationBindingInstantiationGenerator {
    private static final class Methods {
        // class: ModuleEnvironmentRecord
        static final MethodName ModuleEnvironmentRecord_createImportBinding = MethodName
                .findVirtual(Types.ModuleEnvironmentRecord, "createImportBinding", Type.methodType(
                        Type.VOID_TYPE, Types.String, Types.ModuleRecord, Types.String));

        // class: ModuleSemantics
        static final MethodName ModuleSemantics_GetModuleNamespace = MethodName.findStatic(
                Types.ModuleSemantics, "GetModuleNamespace", Type.methodType(
                        Types.ModuleNamespaceObject, Types.ExecutionContext, Types.Realm,
                        Types.Map, Types.String));

        // class: ResolvedExport
        static final MethodName ResolvedExport_getModule = MethodName.findVirtual(
                Types.ModuleExport, "getModule", Type.methodType(Types.ModuleRecord));
        static final MethodName ResolvedExport_getBindingName = MethodName.findVirtual(
                Types.ModuleExport, "getBindingName", Type.methodType(Types.String));

        // class: ScriptRuntime
        static final MethodName ScriptRuntime_resolveExportOrThrow = MethodName.findStatic(
                Types.ScriptRuntime, "resolveExportOrThrow", Type.methodType(Type.VOID_TYPE,
                        Types.ExecutionContext, Types.Map, Types.String, Types.String));

        static final MethodName ScriptRuntime_resolveImportOrThrow = MethodName.findStatic(
                Types.ScriptRuntime, "resolveImportOrThrow", Type.methodType(Types.ModuleExport,
                        Types.ExecutionContext, Types.Map, Types.String, Types.String));
    }

    private static final int EXECUTION_CONTEXT = 0;
    private static final int MODULE_ENV = 1;
    private static final int REALM = 2;
    private static final int MODULE_SET = 3;

    private static final class ModuleDeclInitMethodGenerator extends ExpressionVisitor {
        ModuleDeclInitMethodGenerator(MethodCode method, Module module) {
            super(method, true, false, false);
        }

        @Override
        public void begin() {
            super.begin();
            setParameterName("cx", EXECUTION_CONTEXT, Types.ExecutionContext);
            setParameterName("moduleEnv", MODULE_ENV, Types.LexicalEnvironment);
            setParameterName("realm", REALM, Types.Realm);
            setParameterName("moduleSet", MODULE_SET, Types.Map);
        }
    }

    ModuleDeclarationInstantiationGenerator(CodeGenerator codegen) {
        super(codegen);
    }

    void generate(Module module, ModuleRecord moduleRecord) {
        MethodCode method = codegen.newMethod(module, ModuleName.Init);
        ExpressionVisitor mv = new ModuleDeclInitMethodGenerator(method, module);

        mv.lineInfo(module);
        mv.begin();
        generate(module, moduleRecord, mv);
        mv.end();
    }

    private void generate(Module module, ModuleRecord moduleRecord, ExpressionVisitor mv) {
        Variable<ExecutionContext> context = mv.getParameter(EXECUTION_CONTEXT,
                ExecutionContext.class);
        Variable<LexicalEnvironment<ModuleEnvironmentRecord>> env = mv.getParameter(MODULE_ENV,
                LexicalEnvironment.class).uncheckedCast();
        Variable<Realm> realm = mv.getParameter(REALM, Realm.class);
        Variable<Map<String, ModuleRecord>> moduleSet = mv.getParameter(MODULE_SET, Map.class)
                .uncheckedCast();

        Variable<ModuleEnvironmentRecord> envRec = mv.newVariable("envRec",
                ModuleEnvironmentRecord.class);
        storeEnvironmentRecord(envRec, env, mv);

        Variable<ModuleExport> resolved = mv.newVariable("resolved", ModuleExport.class);

        Variable<Undefined> undef = mv.newVariable("undef", Undefined.class);
        mv.loadUndefined();
        mv.store(undef);

        /* step 1 */
        String moduleName = moduleRecord.getModuleId();
        /* step 2 (not applicable) */
        /* step 3 */
        for (ExportEntry exportEntry : moduleRecord.getIndirectExportEntries()) {
            mv.lineInfo(exportEntry.getLine());
            mv.load(context);
            mv.load(moduleSet);
            mv.aconst(moduleName);
            mv.aconst(exportEntry.getExportName());
            mv.invoke(Methods.ScriptRuntime_resolveExportOrThrow);
        }
        /* steps 4-7 (not applicable) */
        /* step 8 */
        for (ImportEntry importEntry : moduleRecord.getImportEntries()) {
            if (importEntry.isStarImport()) {
                createImmutableBinding(envRec, importEntry.getLocalName(), true, mv);

                mv.load(envRec);
                mv.aconst(importEntry.getLocalName());
                {
                    mv.lineInfo(importEntry.getLine());
                    mv.load(context);
                    mv.load(realm);
                    mv.load(moduleSet);
                    mv.aconst(importEntry.getModuleRequestId());
                    mv.invoke(Methods.ModuleSemantics_GetModuleNamespace);
                }
                initializeBinding(mv);
            } else {
                mv.lineInfo(importEntry.getLine());
                mv.load(context);
                mv.load(moduleSet);
                mv.aconst(importEntry.getModuleRequestId());
                mv.aconst(importEntry.getImportName());
                mv.invoke(Methods.ScriptRuntime_resolveImportOrThrow);
                mv.store(resolved);

                createImportBinding(envRec, importEntry.getLocalName(), resolved, mv);
            }
        }
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
                    createImmutableBinding(envRec, dn, true, mv);
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
        ExportDefaultExpression defaultExport = module.getScope().getDefaultExportExpression();
        if (defaultExport != null) {
            createImmutableBinding(envRec, defaultExport.getBinding().getName(), true, mv);
        }
        /* step 13 */
        mv._return();
    }

    private void createImmutableBinding(Variable<? extends EnvironmentRecord> envRec, String name,
            boolean strict, InstructionVisitor mv) {
        createImmutableBinding(envRec, new Name(name), strict, mv);
    }

    private void createImportBinding(Variable<? extends EnvironmentRecord> envRec, String name,
            Variable<ModuleExport> resolved, InstructionVisitor mv) {
        mv.load(envRec);
        mv.aconst(name);

        mv.load(resolved);
        mv.invoke(Methods.ResolvedExport_getModule);

        mv.load(resolved);
        mv.invoke(Methods.ResolvedExport_getBindingName);

        mv.invoke(Methods.ModuleEnvironmentRecord_createImportBinding);
    }
}
