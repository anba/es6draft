/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.modules.loader;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.RuntimeContext;
import com.github.anba.es6draft.runtime.modules.MalformedNameException;
import com.github.anba.es6draft.runtime.modules.ModuleLoader;
import com.github.anba.es6draft.runtime.modules.ModuleRecord;
import com.github.anba.es6draft.runtime.modules.ModuleSource;
import com.github.anba.es6draft.runtime.modules.SourceIdentifier;

/**
 * 
 */
public abstract class AbstractModuleLoader<MODULE extends ModuleRecord> implements ModuleLoader {
    private final RuntimeContext context;

    protected AbstractModuleLoader(RuntimeContext context) {
        this.context = context;
    }

    /**
     * Returns the runtime context for this loader.
     * 
     * @return the runtime context
     */
    protected RuntimeContext getContext() {
        return context;
    }

    /**
     * Loads the module source.
     * 
     * @param identifier
     *            the source identifier
     * @return the module source object
     * @throws IOException
     *             if there was any I/O error
     */
    protected abstract ModuleSource loadSource(SourceIdentifier identifier) throws IOException;

    /**
     * Adds a module record to the internal modules list.
     * 
     * @param module
     *            the module record
     */
    protected abstract void defineModule(MODULE module);

    /**
     * Retrieves a module record from the internal modules list.
     * 
     * @param identifier
     *            the source identifier
     * @return the module record or {@code null} if not found
     */
    protected abstract MODULE getModule(SourceIdentifier identifier);

    /**
     * Parses a module record.
     * 
     * @param identifier
     *            the source identifier
     * @param source
     *            the module source
     * @return the parsed module record
     * @throws IOException
     *             if there was any I/O error
     */
    protected abstract MODULE parseModule(SourceIdentifier identifier, ModuleSource source) throws IOException;

    /**
     * Links the module against the realm instance.
     * 
     * @param module
     *            the module record
     * @param realm
     *            the realm instance
     */
    protected abstract void linkModule(MODULE module, Realm realm);

    /**
     * Returns the list of requested modules.
     * 
     * @param module
     *            the module record
     * @return the list of unnormalized module names
     */
    protected abstract Set<String> getRequestedModules(MODULE module);

    /**
     * Returns an unmodifiable collection of the currently loaded modules.
     * <p>
     * Optional operation: Returns an empty collection if not supported.
     * 
     * @return the module collection
     */
    protected Collection<MODULE> getModules() {
        return Collections.emptySet();
    }

    @Override
    public MODULE get(SourceIdentifier identifier, Realm realm) {
        MODULE module = getModule(identifier);
        if (module != null) {
            linkModule(module, realm);
        }
        return module;
    }

    @Override
    public MODULE define(SourceIdentifier identifier, ModuleSource source, Realm realm) throws IOException {
        MODULE module = getModule(identifier);
        if (module == null) {
            module = parseModule(identifier, source);
            defineModule(module);
        }
        linkModule(module, realm);
        return module;
    }

    @Override
    public MODULE resolve(SourceIdentifier identifier, Realm realm) throws IOException {
        MODULE module = loadIfAbsent(identifier);
        linkModule(module, realm);
        return module;
    }

    @Override
    public MODULE load(SourceIdentifier identifier) throws MalformedNameException, IOException {
        MODULE module = loadIfAbsent(identifier);
        loadRequested(module, new HashSet<>());
        return module;
    }

    private MODULE loadIfAbsent(SourceIdentifier identifier) throws IOException {
        MODULE module = getModule(identifier);
        if (module == null) {
            module = parseModule(identifier, loadSource(identifier));
            defineModule(module);
        }
        return module;
    }

    private void loadRequested(MODULE module, HashSet<ModuleRecord> visited)
            throws MalformedNameException, IOException {
        if (visited.add(module)) {
            SourceIdentifier referrerId = module.getSourceCodeId();
            for (String specifier : getRequestedModules(module)) {
                SourceIdentifier identifier = normalizeName(specifier, referrerId);
                loadRequested(loadIfAbsent(identifier), visited);
            }
        }
    }
}
