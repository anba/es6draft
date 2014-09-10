/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.modules;

import com.github.anba.es6draft.ast.scope.Name;

public final class ExportBinding {
    /** [[Module]] */
    final ModuleLinkage module;

    /** [[LocalName]] */
    final Name localName;

    public ExportBinding(ModuleLinkage module, Name localName) {
        this.module = module;
        this.localName = localName;
    }
}
