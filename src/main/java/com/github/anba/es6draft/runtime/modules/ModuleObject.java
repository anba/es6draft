/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.modules;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>1 Modules: Semantics</h1><br>
 * <h2>1.4 Module Objects</h2>
 */
public class ModuleObject extends OrdinaryObject {
    /** [[Body]] */
    private final ModuleBody body;

    /** [[Environment]] */
    private final LexicalEnvironment environment;

    /** [[LinkErrors]] */
    private final List<ScriptException> linkErrors;

    /** [[ExportDefinitions]] */
    private List<ExportDefinition> exportDefinitions;

    /** [[Exports]] */
    private Map<String, ExportBinding> exports;

    /** [[Dependencies]] */
    private LinkedHashMap<String, ModuleObject> dependencies;

    /** [[UnlinkedDependencies]] */
    private List<Load> unlinkedDependencies;

    /** [[ImportEntries]] */
    private List<ImportEntry> importEntries;

    /** [[ImportDefinitions]] */
    private List<ImportDefinition> importDefinitions;

    /** [[Evaluated]] */
    private boolean evaluated;

    public ModuleObject(Realm realm, ModuleBody body, LexicalEnvironment environment) {
        super(realm);
        this.body = body;
        this.environment = environment;
        this.linkErrors = new ArrayList<>();
    }

    /** [[Body]] */
    public ModuleBody getBody() {
        return body;
    }

    /** [[BoundNames]] */
    public List<String> getBoundNames() {
        assert body != null;
        return body.boundNames();
    }

    /** [[KnownExportEntries]] */
    public List<ExportEntry> getKnownExportEntries() {
        assert body != null;
        return body.knownExportEntries();
    }

    /** [[UnknownExportEntries]] */
    public List<ExportEntry> getUnknownExportEntries() {
        assert body != null;
        return body.unknownExportEntries();
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
    public LinkedHashMap<String, ModuleObject> getDependencies() {
        return dependencies;
    }

    /** [[Dependencies]] */
    public void setDependencies(LinkedHashMap<String, ModuleObject> dependencies) {
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
    public LexicalEnvironment getEnvironment() {
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
}
