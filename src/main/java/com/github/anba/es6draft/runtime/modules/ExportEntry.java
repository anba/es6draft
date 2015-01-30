/**
 * Copyright (c) 2012-2015 André Bargull
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

    /** [[ImportModule]] */
    private ModuleRecord importModule;

    private final long sourcePosition;

    public ExportEntry(String moduleRequest, String importName, String localName,
            String exportName, long sourcePosition) {
        this.moduleRequest = moduleRequest;
        this.importName = importName;
        this.localName = localName;
        this.exportName = exportName;
        this.sourcePosition = sourcePosition;
    }

    @Override
    public String toString() {
        return String
                .format("ExportEntry {moduleRequest=%s, importName=%s, localName=%s, exportName=%s, moduleRequestId=%s}",
                        moduleRequest, importName, localName, exportName, importModule);
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
        this.importModule = importModule;
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
