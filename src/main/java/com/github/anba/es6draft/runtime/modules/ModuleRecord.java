/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.modules;

import static java.util.Collections.unmodifiableList;

import java.util.List;

import com.github.anba.es6draft.Module;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.ModuleEnvironmentRecord;
import com.github.anba.es6draft.runtime.types.builtins.ModuleNamespaceObject;

/**
 * 15.2.1.12 Static and Runtime Semantics: Module Records
 */
public final class ModuleRecord implements Cloneable {
    /**
     * [[ModuleId]]
     */
    private final String moduleId;

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

    /**
     * Creates a module record.
     * 
     * @param moduleId
     *            the normalized module identifier
     */
    public ModuleRecord(String moduleId) {
        assert moduleId != null;
        this.moduleId = moduleId;
    }

    @Override
    public ModuleRecord clone() {
        ModuleRecord newModule = new ModuleRecord(moduleId);
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
     * [[ModuleId]]
     * 
     * @return the module identifier
     */
    public String getModuleId() {
        return moduleId;
    }

    /**
     * [[ImportedModules]]
     * 
     * @return the list of imported modules
     */
    public List<String> getImportedModules() {
        return unmodifiableList(importedModules);
    }

    /**
     * [[ImportedModules]]
     * 
     * @param importedModules
     *            the list of imported modules
     */
    public void setImportedModules(List<String> importedModules) {
        assert this.importedModules == null && importedModules != null;
        this.importedModules = importedModules;
    }

    /**
     * [[ECMAScriptCode]]
     * 
     * @return the script code for this module
     */
    public Module getScriptCode() {
        return scriptCode;
    }

    /**
     * [[ECMAScriptCode]]
     * 
     * @param scriptCode
     *            the script code
     */
    public void setScriptCode(Module scriptCode) {
        assert this.scriptCode == null && scriptCode != null;
        this.scriptCode = scriptCode;
    }

    /**
     * [[ImportEntries]]
     * 
     * @return the list of {@code import} entries
     */
    public List<ImportEntry> getImportEntries() {
        return unmodifiableList(importEntries);
    }

    /**
     * [[ImportEntries]]
     * 
     * @param importEntries
     *            the list of {@code import} entries
     */
    public void setImportEntries(List<ImportEntry> importEntries) {
        assert this.importEntries == null && importEntries != null;
        this.importEntries = importEntries;
    }

    /**
     * [[LocalExportEntries]]
     * 
     * @return the list of local {@code export} entries
     */
    public List<ExportEntry> getLocalExportEntries() {
        return unmodifiableList(localExportEntries);
    }

    /**
     * [[LocalExportEntries]]
     * 
     * @param localExportEntries
     *            the list of local {@code export} entries
     */
    public void setLocalExportEntries(List<ExportEntry> localExportEntries) {
        assert this.localExportEntries == null && localExportEntries != null;
        this.localExportEntries = localExportEntries;
    }

    /**
     * [[IndirectExportEntries]]
     * 
     * @return the list of indirect {@code export} entries
     */
    public List<ExportEntry> getIndirectExportEntries() {
        return unmodifiableList(indirectExportEntries);
    }

    /**
     * [[IndirectExportEntries]]
     * 
     * @param indirectExportEntries
     *            the list of indirect {@code export} entries
     */
    public void setIndirectExportEntries(List<ExportEntry> indirectExportEntries) {
        assert this.indirectExportEntries == null && indirectExportEntries != null;
        this.indirectExportEntries = indirectExportEntries;
    }

    /**
     * [[StarExportEntries]]
     * 
     * @return the list of {@code export*} entries
     */
    public List<ExportEntry> getStarExportEntries() {
        return unmodifiableList(starExportEntries);
    }

    /**
     * [[StarExportEntries]]
     * 
     * @param starExportEntries
     *            the list of {@code export*} entries
     */
    public void setStarExportEntries(List<ExportEntry> starExportEntries) {
        assert this.starExportEntries == null && starExportEntries != null;
        this.starExportEntries = starExportEntries;
    }

    /**
     * [[Environment]]
     * 
     * @return the lexical environment of this module
     */
    public LexicalEnvironment<ModuleEnvironmentRecord> getEnvironment() {
        assert environment != null : "module not instantiated";
        return environment;
    }

    /**
     * [[Environment]]
     * 
     * @param environment
     *            the lexical environment of this module
     */
    public void setEnvironment(LexicalEnvironment<ModuleEnvironmentRecord> environment) {
        assert this.environment == null && environment != null : "module already instantiated";
        this.environment = environment;
    }

    /**
     * [[Namespace]]
     * 
     * @return the module namespace object
     */
    public ModuleNamespaceObject getNamespace() {
        return namespace;
    }

    /**
     * [[Namespace]]
     * 
     * @param namespace
     *            the module namespace object
     */
    public void setNamespace(ModuleNamespaceObject namespace) {
        assert this.namespace == null && namespace != null;
        this.namespace = namespace;
    }

    /**
     * [[Evaluated]]
     * 
     * @return the evaluated flag
     */
    public boolean isEvaluated() {
        return evaluated;
    }

    /**
     * [[Evaluated]]
     * 
     * @param evaluated
     *            the evaluated flag
     */
    public void setEvaluated(boolean evaluated) {
        assert !this.evaluated && evaluated;
        this.evaluated = evaluated;
    }
}
