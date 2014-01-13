/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.modules;

public final class ImportDefinition {
    /** [[Module]] */
    final ModuleObject module;

    /** [[ImportName]] */
    final String importName;

    /** [[LocalName]] */
    final String localName;

    public ImportDefinition(ModuleObject module, String importName, String localName) {
        this.module = module;
        this.importName = importName;
        this.localName = localName;
    }
}
