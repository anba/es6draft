/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.modules;

import static java.util.Collections.unmodifiableList;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.github.anba.es6draft.Module;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.ModuleEnvironmentRecord;
import com.github.anba.es6draft.runtime.types.builtins.ModuleNamespaceObject;

/**
 * 
 */
public final class ModuleRecord implements Cloneable {
    /**
     * [[Name]]
     */
    private final String name;

    /**
     * [[ImportedModules]]
     */
    private List<String> importedModules;

    /**
     * [[ECMAScriptCode]]
     */
    private Module scriptCode;

    /**
     * [[ImportEntries]]
     */
    private List<ImportEntry> importEntries;

    /**
     * [[LocalExportEntries]]
     */
    private List<ExportEntry> localExportEntries;

    /**
     * [[IndirectExportEntries]]
     */
    private List<ExportEntry> indirectExportEntries;

    /**
     * [[StarExportEntries]]
     */
    private List<ExportEntry> starExportEntries;

    /**
     * [[Environment]]
     */
    private LexicalEnvironment<ModuleEnvironmentRecord> environment;

    /**
     * [[Namespace]]
     */
    private ModuleNamespaceObject namespace;

    /**
     * [[Evaluated]]
     */
    private boolean evaluated;

    public ModuleRecord(String name) {
        this.name = name;
    }

    @Override
    public ModuleRecord clone() {
        ModuleRecord newModule = new ModuleRecord(name);
        newModule.importedModules = importedModules;
        newModule.scriptCode = scriptCode;
        newModule.importEntries = importEntries;
        newModule.localExportEntries = localExportEntries;
        newModule.indirectExportEntries = indirectExportEntries;
        newModule.starExportEntries = starExportEntries;
        newModule.environment = null;
        newModule.namespace = null;
        newModule.evaluated = false;
        return newModule;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the importedModules
     */
    public List<String> getImportedModules() {
        return importedModules;
    }

    public void setImportedModules(Set<String> importedModules) {
        this.importedModules = unmodifiableList(new ArrayList<>(importedModules));
    }

    /**
     * @return the scriptCode
     */
    public Module getScriptCode() {
        return scriptCode;
    }

    public void setScriptCode(Module scriptCode) {
        this.scriptCode = scriptCode;
    }

    /**
     * @return the importEntries
     */
    public List<ImportEntry> getImportEntries() {
        return importEntries;
    }

    public void setImportEntries(List<ImportEntry> importEntries) {
        this.importEntries = unmodifiableList(importEntries);
    }

    /**
     * @return the localExportEntries
     */
    public List<ExportEntry> getLocalExportEntries() {
        return localExportEntries;
    }

    public void setLocalExportEntries(List<ExportEntry> localExportEntries) {
        this.localExportEntries = unmodifiableList(localExportEntries);
    }

    /**
     * @return the indirectExportEntries
     */
    public List<ExportEntry> getIndirectExportEntries() {
        return indirectExportEntries;
    }

    public void setIndirectExportEntries(List<ExportEntry> indirectExportEntries) {
        this.indirectExportEntries = unmodifiableList(indirectExportEntries);
    }

    /**
     * @return the starExportEntries
     */
    public List<ExportEntry> getStarExportEntries() {
        return starExportEntries;
    }

    public void setStarExportEntries(List<ExportEntry> starExportEntries) {
        this.starExportEntries = unmodifiableList(starExportEntries);
    }

    /**
     * @return the environment
     */
    public LexicalEnvironment<ModuleEnvironmentRecord> getEnvironment() {
        return environment;
    }

    public void setEnvironment(LexicalEnvironment<ModuleEnvironmentRecord> environment) {
        this.environment = environment;
    }

    /**
     * @return the namespace
     */
    public ModuleNamespaceObject getNamespace() {
        return namespace;
    }

    public void setNamespace(ModuleNamespaceObject namespace) {
        assert this.namespace == null && namespace != null;
        this.namespace = namespace;
    }

    /**
     * @return the evaluated
     */
    public boolean isEvaluated() {
        return evaluated;
    }

    public void setEvaluated(boolean evaluated) {
        this.evaluated = evaluated;
    }
}
