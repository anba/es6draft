/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.modules;

/**
 * 15.2.1.18 Static Semantics: ResolveExport( modules, moduleName, exportName, circularitySet)
 *
 */
public final class ModuleExport {
    private final ModuleRecord module;
    private final String bindingName;

    ModuleExport(ModuleRecord module, String bindingName) {
        this.module = module;
        this.bindingName = bindingName;
    }

    /**
     * Returns the resolved module record.
     * 
     * @return the module record
     */
    public ModuleRecord getModule() {
        return module;
    }

    /**
     * Returns the resolved binding name.
     * 
     * @return the binding name
     */
    public String getBindingName() {
        return bindingName;
    }
}
