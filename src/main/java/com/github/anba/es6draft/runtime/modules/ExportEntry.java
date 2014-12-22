/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.modules;

/**
 * 15.2.1.3 Static Semantics: ExportEntries<br>
 * 15.2.3.4 Static Semantics: ExportEntries<br>
 * 15.2.1.12 Static and Runtime Semantics: Module Records
 */
public final class ExportEntry {
    /** [[ModuleRequest]] */
    private final String moduleRequest;

    /** [[ImportName]] */
    private final String importName;

    /** [[LocalName]] */
    private final String localName;

    /** [[ExportName]] */
    private final String exportName;

    /** [[ModuleRequestId]] */
    private String moduleRequestId;

    public ExportEntry(String moduleRequest, String importName, String localName, String exportName) {
        this.moduleRequest = moduleRequest;
        this.importName = importName;
        this.localName = localName;
        this.exportName = exportName;
    }

    @Override
    public String toString() {
        return String
                .format("ExportEntry {moduleRequest=%s, importName=%s, localName=%s, exportName=%s, moduleRequestId=%s}",
                        moduleRequest, importName, localName, exportName, moduleRequestId);
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
        this.moduleRequestId = moduleRequestId;
    }

    public boolean isStarExport() {
        return "*".equals(importName);
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
     * [[ExportName]]
     * 
     * @return the export name
     */
    public String getExportName() {
        return exportName;
    }
}
