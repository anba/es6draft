/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.modules;

/**
 * 15.2.1.6 Static Semantics: ImportEntries<br>
 * 15.2.2.3 Static Semantics: ImportEntries<br>
 * 15.2.1.15 Static and Runtme Semantics: Module Records
 */
public final class ImportEntry {
    /** [[ModuleRequest]] */
    private final String moduleRequest;

    /** [[ImportName]] */
    private final String importName;

    /** [[LocalName]] */
    private final String localName;

    /** [[ModuleRequest]] */
    private String normalizedModuleRequest;

    public ImportEntry(String moduleRequest, String importName, String localName) {
        this.moduleRequest = moduleRequest;
        this.importName = importName;
        this.localName = localName;
    }

    public boolean isStarImport() {
        return "*".equals(importName);
    }

    /**
     * [[ModuleRequest]]
     * 
     * @return the module request
     */
    public String getModuleRequest() {
        return moduleRequest;
    }

    /**
     * [[ModuleRequest]]
     * 
     * @return the module request
     */
    public String getNormalizedModuleRequest() {
        assert normalizedModuleRequest != null;
        return normalizedModuleRequest;
    }

    public void setNormalizedModuleRequest(String moduleRequest) {
        this.normalizedModuleRequest = moduleRequest;
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
}
