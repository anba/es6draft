/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.modules;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.ModuleEnvironmentRecord;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.builtins.ModuleNamespaceObject;

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
    LexicalEnvironment<ModuleEnvironmentRecord> getEnvironment();

    /**
     * [[Namespace]]
     * 
     * @return the module namespace object or {@code null}
     */
    ModuleNamespaceObject getNamespace();

    /**
     * Creates the module namespace object of this module record.
     * 
     * @param cx
     *            the current execution context
     * @param exports
     *            the list of exported names
     * @return the new module namespace object
     */
    ModuleNamespaceObject createNamespace(ExecutionContext cx, Set<String> exports);

    /**
     * [[Evaluated]]
     * 
     * @return {@code true} if module evaluation has started
     */
    boolean isEvaluated();

    /**
     * Returns {@code true} if the module is instantiated.
     * 
     * @return {@code true} if the module is instantiated
     */
    boolean isInstantiated();

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
    Set<String> getExportedNames(Set<ModuleRecord> exportStarSet) throws IOException,
            MalformedNameException, ResolutionException;

    /**
     * ResolveExport(exportName, resolveSet, exportStarSet)
     * 
     * @param exportName
     *            the requested export name
     * @param resolveSet
     *            the list of previously visited modules
     * @param exportStarSet
     *            the list of previously visited modules
     * @return the resolved export binding
     * @throws IOException
     *             if there was any I/O error
     * @throws MalformedNameException
     *             if the module specifier cannot be normalized
     * @throws ResolutionException
     *             if the export cannot be resolved
     */
    ModuleExport resolveExport(String exportName, Map<ModuleRecord, Set<String>> resolveSet,
            Set<ModuleRecord> exportStarSet) throws IOException, MalformedNameException,
            ResolutionException;

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
