/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.modules;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.modules.ModuleLinkage;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>15 ECMAScript Language: Scripts and Modules</h1><br>
 * <h2>15.2 Modules</h2><br>
 * <h3>15.2.5 Runtime Semantics: Module Linking</h3>
 * <ul>
 * <li>15.2.5.1 ModuleLinkage Record
 * </ul>
 */
public final class ModuleObject extends OrdinaryObject {
    // FIXME: spec does not properly differentiate between module linkage records and module objects

    /** [[ModuleLinkage]] */
    private ModuleLinkage moduleLinkage;

    /**
     * [[ModuleLinkage]]
     *
     * @return the module linkage record
     */
    public ModuleLinkage getModuleLinkage() {
        return moduleLinkage;
    }

    /**
     * [[ModuleLinkage]]
     *
     * @param moduleLinkage
     *            the new module linkage record
     */
    public void setModuleLinkage(ModuleLinkage moduleLinkage) {
        assert this.moduleLinkage == null && moduleLinkage != null : "ModuleObject already initialized";
        this.moduleLinkage = moduleLinkage;
    }

    public ModuleObject(Realm realm) {
        super(realm);
    }
}
