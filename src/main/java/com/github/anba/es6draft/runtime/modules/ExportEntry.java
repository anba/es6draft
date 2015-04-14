/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.modules;

import com.github.anba.es6draft.ast.Node;

/**
 * 15.2.1.7 Static Semantics: ExportEntries<br>
 * 15.2.3.5 Static Semantics: ExportEntries<br>
 * 15.2.1.16 Source Text Module Records
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

    private final long sourcePosition;

    public ExportEntry(Node node, String moduleRequest, String importName, String localName,
            String exportName) {
        this.moduleRequest = moduleRequest;
        this.importName = importName;
        this.localName = localName;
        this.exportName = exportName;
        this.sourcePosition = node.getBeginPosition();
    }

    @Override
    public String toString() {
        return String
                .format("ExportEntry {moduleRequest=%s, importName=%s, localName=%s, exportName=%s, moduleRequest=%s}",
                        moduleRequest, importName, localName, exportName, moduleRequest);
    }

    /**
     * [[ModuleRequest]]
     * 
     * @return the module request name
     */
    public String getModuleRequest() {
        return moduleRequest;
    }

    public boolean isStarExport() {
        return "*".equals(importName) && exportName == null;
    }

    public boolean isNameSpaceExport() {
        return "*".equals(importName) && exportName != null;
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
