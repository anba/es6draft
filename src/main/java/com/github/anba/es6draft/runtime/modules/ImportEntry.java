/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.modules;

/**
 * 15.2.0.6 Static Semantics: ImportEntries<br>
 * 15.2.1.3 Static Semantics: ImportEntries
 */
public final class ImportEntry {
    /** [[ModuleRequest]] */
    private final String moduleRequest;

    /** [[ImportName]] */
    private final String importName;

    /** [[LocalName]] */
    private final String localName;

    public ImportEntry(String moduleRequest, String importName, String localName) {
        this.moduleRequest = moduleRequest;
        this.importName = importName;
        this.localName = localName;
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
}
