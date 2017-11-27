/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.modules;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import com.github.anba.es6draft.runtime.EnvironmentRecord;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.ScriptObject;

/**
 * 15.2.1.15 Abstract Module Records
 */
public interface ModuleRecord {
    /**
     * Returns the source identifier of this module record.
     * 
     * @return the source identifier
     */
    SourceIdentifier getSourceCodeId();

    /**
     * [[Realm]]
     * 
     * @return the realm instance or {@code null} if the module is not linked to a realm
     */
    Realm getRealm();

    /**
     * [[Environment]]
     * 
     * @return the lexical environment of this module or {@code null} if not instantiated
     */
    LexicalEnvironment<? extends EnvironmentRecord> getEnvironment();

    /**
     * [[Namespace]]
     * 
     * @return the module namespace object or {@code null}
     */
    ScriptObject getNamespace();

    /**
     * [[Namespace]]
     * 
     * @param namespace
     *            the module namespace object
     */
    void setNamespace(ScriptObject namespace);

    /**
     * [[Meta]]
     * 
     * @return the module meta object or {@code null}
     */
    ScriptObject getMeta();

    /**
     * [[Meta]]
     * 
     * @param meta
     *            the module meta object
     */
    void setMeta(ScriptObject meta);

    /**
     * GetExportedNames(exportStarSet)
     * 
     * @param exportStarSet
     *            the list of previously visited modules
     * @return the list of names that are directly or indirectly exported from this module
     * @throws IOException
     *             if there was any I/O error
     * @throws MalformedNameException
     *             if the module specifier cannot be normalized
     * @throws ResolutionException
     *             if the export cannot be resolved
     */
    Set<String> getExportedNames(Set<ModuleRecord> exportStarSet)
            throws IOException, MalformedNameException, ResolutionException;

    /**
     * ResolveExport(exportName, resolveSet)
     * 
     * @param exportName
     *            the requested export name
     * @param resolveSet
     *            the list of previously visited modules
     * @return the resolved export binding
     * @throws IOException
     *             if there was any I/O error
     * @throws MalformedNameException
     *             if the module specifier cannot be normalized
     * @throws ResolutionException
     *             if the export cannot be resolved
     */
    ResolvedBinding resolveExport(String exportName, Map<ModuleRecord, Set<String>> resolveSet)
            throws IOException, MalformedNameException, ResolutionException;

    /**
     * ModuleDeclarationInstantiation()
     * 
     * @throws IOException
     *             if there was any I/O error
     * @throws MalformedNameException
     *             if the module specifier cannot be normalized
     * @throws ResolutionException
     *             if the export cannot be resolved
     */
    void instantiate() throws IOException, MalformedNameException, ResolutionException;

    /**
     * ModuleEvaluation()
     * 
     * @return the module evaluation result
     * @throws IOException
     *             if there was any I/O error
     * @throws MalformedNameException
     *             if the module specifier cannot be normalized
     * @throws ResolutionException
     *             if the export cannot be resolved
     */
    Object evaluate() throws IOException, MalformedNameException, ResolutionException;
}
