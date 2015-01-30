/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.modules;

/**
 * 15.2.1.5 Static Semantics: ImportEntries<br>
 * 15.2.2.3 Static Semantics: ImportEntries<br>
 * 15.2.1.12 Static and Runtime Semantics: Module Records
 */
public final class ImportEntry {
    /** [[ModuleRequest]] */
    private final String moduleRequest;

    /** [[ImportName]] */
    private final String importName;

    /** [[LocalName]] */
    private final String localName;

    /** [[ImportModule]] */
    private ModuleRecord importModule;

    private final long sourcePosition;

    public ImportEntry(String moduleRequest, String importName, String localName,
            long sourcePosition) {
        this.moduleRequest = moduleRequest;
        this.importName = importName;
        this.localName = localName;
        this.sourcePosition = sourcePosition;
    }

    public boolean isStarImport() {
        return "*".equals(importName);
    }

    /**
     * [[ModuleRequest]]
     * 
     * @return the module request name
     */
    public String getModuleRequest() {
        return moduleRequest;
    }

    /**
     * [[ImportModule]]
     * 
     * @return the resolved import module
     */
    public ModuleRecord getImportModule() {
        assert importModule != null;
        return importModule;
    }

    /**
     * [[ImportModule]]
     * 
     * @param importModule
     *            the resolved import module
     */
    public void setImportModule(ModuleRecord importModule) {
        assert this.importModule == null && importModule != null;
        this.importModule = importModule;
    }

    /**
     * [[ImportName]]
     * 
     * @return the import name
     */
    public String getImportName() {
        return importName;
    }

    /**
     * [[LocalName]]
     * 
     * @return the local name
     */
    public String getLocalName() {
        return localName;
    }

    /**
     * Returns the source line position.
     * 
     * @return the source line
     */
    public int getLine() {
        return (int) sourcePosition;
    }

    /**
     * Returns the source column position.
     * 
     * @return the source column
     */
    public int getColumn() {
        return (int) (sourcePosition >>> 32);
    }
}
