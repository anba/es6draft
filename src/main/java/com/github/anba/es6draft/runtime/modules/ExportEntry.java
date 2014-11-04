/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.modules;

/**
 * 15.2.1.4 Static Semantics: ExportEntries<br>
 * 15.2.3.4 Static Semantics: ExportEntries<br>
 * 15.2.1.15 Static and Runtme Semantics: Module Records
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

    /** [[ModuleRequest]] */
    private String normalizedModuleRequest;

    public ExportEntry(String moduleRequest, String importName, String localName, String exportName) {
        this.moduleRequest = moduleRequest;
        this.importName = importName;
        this.localName = localName;
        this.exportName = exportName;
    }

    @Override
    public String toString() {
        return String
                .format("ExportEntry {moduleRequest=%s, importName=%s, localName=%s, exportName=%s, normalizedModuleRequest=%s}",
                        moduleRequest, importName, localName, exportName, normalizedModuleRequest);
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
     * @return the normalized module request
     */
    public String getNormalizedModuleRequest() {
        assert normalizedModuleRequest != null;
        return normalizedModuleRequest;
    }

    /**
     * [[ModuleRequest]]
     * 
     * @param moduleRequest
     *            the new module request
     */
    public void setNormalizedModuleRequest(String moduleRequest) {
        this.normalizedModuleRequest = moduleRequest;
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
