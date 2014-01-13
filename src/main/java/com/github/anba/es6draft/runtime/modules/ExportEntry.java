/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.modules;

public final class ExportEntry {
    /** [[ModuleRequest]] */
    final String moduleRequest;

    /** [[ImportName]] */
    final String importName;

    /** [[LocalName]] */
    final String localName;

    /** [[ExportName]] */
    final String exportName;

    public ExportEntry(String moduleRequest, String importName, String localName, String exportName) {
        this.moduleRequest = moduleRequest;
        this.importName = importName;
        this.localName = localName;
        this.exportName = exportName;
    }
}
