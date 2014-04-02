/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.modules;

import static com.github.anba.es6draft.semantics.StaticSemantics.DeclaredNames;
import static com.github.anba.es6draft.semantics.StaticSemantics.KnownExportEntries;
import static com.github.anba.es6draft.semantics.StaticSemantics.UnknownExportEntries;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.github.anba.es6draft.ast.Module;
import com.github.anba.es6draft.runtime.DeclarativeEnvironmentRecord;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.GlobalEnvironmentRecord;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.objects.modules.ModuleObject;

/**
 * <h1>15 ECMAScript Language: Modules and Scripts</h1><br>
 * <h2>15.2 Modules</h2><br>
 * <h3>15.2.5 Runtime Semantics: Module Linking</h3>
 * <ul>
 * <li>15.2.5.1 ModuleLinkage Record
 * </ul>
 */
public final class ModuleLinkage {
    /** [[Body]] */
    private final Module body;

    /** [[Environment]] */
    private final LexicalEnvironment<DeclarativeEnvironmentRecord> environment;

    /** [[LinkErrors]] */
    private final List<ScriptException> linkErrors;

    /** [[ExportDefinitions]] */
    private List<ExportDefinition> exportDefinitions;

    /** [[Exports]] */
    private Map<String, ExportBinding> exports;

    /** [[Dependencies]] */
    private LinkedHashMap<String, ModuleLinkage> dependencies;

    /** [[UnlinkedDependencies]] */
    private List<Load> unlinkedDependencies;

    /** [[ImportEntries]] */
    private List<ImportEntry> importEntries;

    /** [[ImportDefinitions]] */
    private List<ImportDefinition> importDefinitions;

    /** [[Evaluated]] */
    private boolean evaluated;

    // FIXME: spec does not properly differentiate between module linkage records and module objects

    /** [[ModuleObj]] */
    private final ModuleObject moduleObject;

    /** [[ModuleObj]] */
    public ModuleObject getModuleObject() {
        return moduleObject;
    }

    public ModuleLinkage(ModuleObject moduleObject, Module body,
            LexicalEnvironment<DeclarativeEnvironmentRecord> environment) {
        this.moduleObject = moduleObject;
        this.body = body;
        this.environment = environment;
        this.linkErrors = new ArrayList<>();
    }

    /** [[Body]] */
    public Module getBody() {
        return body;
    }

    /** [[BoundNames]] */
    public List<String> getBoundNames() {
        assert body != null;
        return DeclaredNames(body);
    }

    /** [[KnownExportEntries]] */
    public List<ExportEntry> getKnownExportEntries() {
        assert body != null;
        return KnownExportEntries(body);
    }

    /** [[UnknownExportEntries]] */
    public List<ExportEntry> getUnknownExportEntries() {
        assert body != null;
        return UnknownExportEntries(body);
    }

    /** [[ExportDefinitions]] */
    public List<ExportDefinition> getExportDefinitions() {
        return exportDefinitions;
    }

    /** [[ExportDefinitions]] */
    public void setExportDefinitions(List<ExportDefinition> exportDefinitions) {
        assert this.exportDefinitions == null && exportDefinitions != null;
        this.exportDefinitions = exportDefinitions;
    }

    /** [[Exports]] */
    public Map<String, ExportBinding> getExports() {
        return exports;
    }

    /** [[Dependencies]] */
    public LinkedHashMap<String, ModuleLinkage> getDependencies() {
        return dependencies;
    }

    /** [[Dependencies]] */
    public void setDependencies(LinkedHashMap<String, ModuleLinkage> dependencies) {
        this.dependencies = dependencies;
    }

    /** [[UnlinkedDependencies]] */
    public List<Load> getUnlinkedDependencies() {
        return unlinkedDependencies;
    }

    /** [[UnlinkedDependencies]] */
    public void setUnlinkedDependencies(List<Load> unlinkedDependencies) {
        assert this.unlinkedDependencies == null && unlinkedDependencies != null;
        this.unlinkedDependencies = unlinkedDependencies;
    }

    /** [[ImportEntries]] */
    public List<ImportEntry> getImportEntries() {
        return importEntries;
    }

    /** [[ImportEntries]] */
    public void setImportEntries(List<ImportEntry> importEntries) {
        this.importEntries = importEntries;
    }

    /** [[ImportDefinitions]] */
    public List<ImportDefinition> getImportDefinitions() {
        return importDefinitions;
    }

    /** [[ImportDefinitions]] */
    public void setImportDefinitions(List<ImportDefinition> importDefinitions) {
        this.importDefinitions = importDefinitions;
    }

    /** [[LinkErrors]] */
    public List<ScriptException> getLinkErrors() {
        return linkErrors;
    }

    /** [[Environment]] */
    public LexicalEnvironment<DeclarativeEnvironmentRecord> getEnvironment() {
        return this.environment;
    }

    /** [[Evaluated]] */
    public boolean isEvaluated() {
        return evaluated;
    }

    /** [[Evaluated]] */
    public void setEvaluated(boolean evaluated) {
        assert !this.evaluated && evaluated;
        this.evaluated = evaluated;
    }

    public static ModuleLinkage CreateLinkedModuleInstance(ExecutionContext cx) {
        // TODO: not yet specified
        ModuleObject module = new ModuleObject(cx.getRealm());
        ModuleLinkage moduleLinkage = new ModuleLinkage(module, null, null);
        module.setModuleLinkage(moduleLinkage);
        // no dependencies for linked modules
        moduleLinkage.setDependencies(new LinkedHashMap<String, ModuleLinkage>());
        return moduleLinkage;
    }

    /**
     * 15.2.5.1.1 CreateModuleLinkageRecord (loader, body) Abstract Operation
     */
    public static ModuleLinkage CreateModuleLinkageRecord(Loader loader, Module body) {
        /* step 1 (not applicable) */
        /* step 14 */
        Realm realm = loader.getRealm();
        /* step 15 */
        LexicalEnvironment<GlobalEnvironmentRecord> globalEnv = realm.getGlobalEnv();
        /* step 16 */
        LexicalEnvironment<DeclarativeEnvironmentRecord> env = LexicalEnvironment
                .newModuleEnvironment(globalEnv);
        /* steps 2-13, 17 */
        ModuleObject module = new ModuleObject(realm);
        ModuleLinkage moduleLinkage = new ModuleLinkage(module, body, env);
        module.setModuleLinkage(moduleLinkage);
        /* step 18 */
        return moduleLinkage;
    }

    /**
     * 15.2.5.1.2 LookupExport ( M, exportName )
     */
    public static ExportBinding LookupExport(ModuleLinkage module, String exportName) {
        assert module.getExports() != null;
        /* step 1 */
        if (!module.getExports().containsKey(exportName)) {
            return null;
        }
        /* steps 2-3 */
        return module.getExports().get(exportName);
    }

    /**
     * 15.2.5.1.3 LookupModuleDependency ( M, requestName )
     */
    public static ModuleLinkage LookupModuleDependency(ModuleLinkage module, String requestName) {
        /* step 1 (not applicable) */
        /* step 2 */
        if (requestName == null) {
            return module;
        }
        assert module.getDependencies() != null;
        /* steps 3-4 */
        return module.getDependencies().get(requestName);
    }
}
