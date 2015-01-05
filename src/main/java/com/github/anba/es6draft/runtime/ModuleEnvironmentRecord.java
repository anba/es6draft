/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime;

import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import com.github.anba.es6draft.runtime.modules.ModuleRecord;

/**
 * <h1>8 Executable Code and Execution Contexts</h1><br>
 * <h2>8.1 Lexical Environments</h2><br>
 * <h3>8.1.1 Environment Records</h3>
 * <ul>
 * <li>8.1.1.5 Module Environment Records
 * </ul>
 */
public final class ModuleEnvironmentRecord extends DeclarativeEnvironmentRecord {
    private static final class IndirectBinding extends Binding {
        private final ModuleRecord module;
        private final String otherName;

        IndirectBinding(ModuleRecord module, String otherName) {
            super(false, false, false);
            this.module = module;
            this.otherName = otherName;
        }

        @Override
        public Binding clone() {
            throw new AssertionError();
        }

        @Override
        public boolean isInitialized() {
            return true;
        }

        @Override
        void initialize(Object value) {
            throw new AssertionError();
        }

        @Override
        public void setValue(Object value) {
            throw new AssertionError();
        }

        @Override
        public Object getValue() {
            /* 8.1.1.5.1 GetBindingValue(N,S), step 3 */
            return module.getEnvironment().getEnvRec().getBindingValue(otherName, true);
        }
    }

    public ModuleEnvironmentRecord(ExecutionContext cx) {
        super(cx);
    }

    // Implicitly defined methods:
    // 8.1.1.5.1 GetBindingValue(N,S)
    // 8.1.1.5.2 DeleteBinding (N)

    /**
     * 8.1.1.5.3 HasThisBinding ()
     */
    @Override
    public boolean hasThisBinding() {
        return true;
    }

    /**
     * 8.1.1.5.4 GetThisBinding ()
     */
    @Override
    public Object getThisBinding() {
        return UNDEFINED;
    }

    /**
     * 8.1.1.5.5 CreateImportBinding (N, M, N2)
     * 
     * @param name
     *            the binding name
     * @param module
     *            the module record
     * @param otherName
     *            the binding name in the module
     */
    public void createImportBinding(String name, ModuleRecord module, String otherName) {
        /* step 1 (omitted) */
        /* step 2 */
        assert !hasBinding(name);
        /* step 3 (not applicable) */
        /* step 4 */
        // FIXME: spec issue (bug 3479)
        // assert module.getEnvironment() != null : "module not initialized";
        // assert module.getEnvironment().getEnvRec().hasBinding(otherName) : "Missing binding: "
        // + otherName;
        /* step 5 */
        createBinding(name, new IndirectBinding(module, otherName));
    }
}
