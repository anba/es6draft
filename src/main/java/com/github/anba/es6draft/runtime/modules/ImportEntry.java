/**
 * Copyright (c) 2012-2014 André Bargull
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

    /** [[ModuleRequestId]] */
    private String moduleRequestId;

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
     * @return the module request name
     */
    public String getModuleRequest() {
        return moduleRequest;
    }

    /**
     * [[ModuleRequestId]]
     * 
     * @return the module request identifier
     */
    public String getModuleRequestId() {
        assert moduleRequestId != null;
        return moduleRequestId;
    }

    /**
     * [[ModuleRequestId]]
     * 
     * @param moduleRequestId
     *            the module request identifier
     */
    public void setModuleRequestId(String moduleRequestId) {
        assert this.moduleRequestId == null && moduleRequestId != null;
        this.moduleRequestId = moduleRequestId;
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
