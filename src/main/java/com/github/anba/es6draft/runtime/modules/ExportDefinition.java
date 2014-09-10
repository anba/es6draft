/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.modules;

import com.github.anba.es6draft.ast.scope.Name;

public final class ExportDefinition {
    /** [[Module]] */
    final ModuleLinkage module;

    /** [[ImportName]] */
    final String importName;

    /** [[LocalName]] */
    final Name localName;

    /** [[ExportName]] */
    final String exportName;

    /** [[Explicit]] */
    final boolean explicit;

    public ExportDefinition(ModuleLinkage module, String importName, Name localName,
            String exportName, boolean explicit) {
        this.module = module;
        this.importName = importName;
        this.localName = localName;
        this.exportName = exportName;
        this.explicit = explicit;
    }
}
