/**
 * Copyright (c) 2012-2016 André Bargull
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
    /**
     * Singleton instance to represent ambiguous module exports.
     */
    public static final ModuleExport AMBIGUOUS = new ModuleExport(null, null);

    private final ModuleRecord module;
    private final String bindingName;

    public ModuleExport(ModuleRecord module, String bindingName) {
        this.module = module;
        this.bindingName = bindingName;
    }

    public ModuleExport(ModuleRecord module) {
        this.module = module;
        this.bindingName = null;
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
     * @return the binding name or {@code null} for namespace exports
     */
    public String getBindingName() {
        return bindingName;
    }

    /**
     * Returns {@code true} if the module export exports a namespace object
     * 
     * @return {@code true} for namespace exports
     */
    public boolean isNameSpaceExport() {
        return bindingName == null;
    }

    /**
     * Returns {@code true} if the module export is ambiguous.
     * 
     * @return {@code true} if the module export is ambiguous
     */
    public boolean isAmbiguous() {
        return this == AMBIGUOUS;
    }
}
