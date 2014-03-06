/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.modules;

/**
 * 15.2.0.4 Static Semantics: ExportEntries<br>
 * 15.2.2.3 Static Semantics: ExportEntries
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

    public ExportEntry(String moduleRequest, String importName, String localName, String exportName) {
        this.moduleRequest = moduleRequest;
        this.importName = importName;
        this.localName = localName;
        this.exportName = exportName;
    }

    /** [[ModuleRequest]] */
    public String getModuleRequest() {
        return moduleRequest;
    }

    /** [[ImportName]] */
    public String getImportName() {
        return importName;
    }

    /** [[LocalName]] */
    public String getLocalName() {
        return localName;
    }

    /** [[ExportName]] */
    public String getExportName() {
        return exportName;
    }
}
