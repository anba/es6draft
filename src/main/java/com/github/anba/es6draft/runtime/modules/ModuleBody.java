/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.modules;

import java.util.List;

public interface ModuleBody {
    /** BoundNames() */
    List<String> boundNames();

    /** KnownExportEntries() */
    List<ExportEntry> knownExportEntries();

    /** UnknownExportEntries() */
    List<ExportEntry> unknownExportEntries();

    /** ImportEntries() */
    List<ImportEntry> importEntries();
}
