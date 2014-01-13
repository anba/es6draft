/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.modules;

public final class ExportDefinition {
    /** [[Module]] */
    final ModuleObject module;

    /** [[ImportName]] */
    final String importName;

    /** [[LocalName]] */
    final String localName;

    /** [[ExportName]] */
    final String exportName;

    /** [[Explicit]] */
    final boolean explicit;

    public ExportDefinition(ModuleObject module, String importName, String localName,
            String exportName, boolean explicit) {
        this.module = module;
        this.importName = importName;
        this.localName = localName;
        this.exportName = exportName;
        this.explicit = explicit;
    }
}
