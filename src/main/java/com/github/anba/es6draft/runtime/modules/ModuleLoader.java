/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.modules;

import java.io.IOException;
import java.util.Collection;

import com.github.anba.es6draft.compiler.CompilationException;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.runtime.Realm;

/**
 * 
 */
public interface ModuleLoader {
    /**
     * Returns the normalized module identifier.
     * 
     * @param unnormalizedName
     *            the unnormalized module name
     * @param referrerId
     *            the identifier of the including module or {@code null}
     * @return the normalized module identifier
     * @throws MalformedNameException
     *             if the name cannot be normalized
     */
    SourceIdentifier normalizeName(String unnormalizedName, SourceIdentifier referrerId)
            throws MalformedNameException;

    /**
     * Retrieves the source code for the requested module.
     * 
     * @param identifier
     *            the module identifier
     * @return the module source code
     * @throws IllegalArgumentException
     *             if the source identifier cannot be processed by this loader
     */
    ModuleSource getSource(SourceIdentifier identifier) throws IllegalArgumentException;

    /**
     * Resolves, but does not link, the requested module record.
     * 
     * @param identifier
     *            the module source identifier
     * @return the parsed module record
     * @throws IOException
     *             if there was any I/O error
     * @throws ParserException
     *             if the module source contains any syntax errors
     * @throws CompilationException
     *             if the parsed module source cannot be compiled
     */
    ModuleRecord resolve(SourceIdentifier identifier) throws IOException, ParserException,
            CompilationException;

    /**
     * Retrieves the requested module record.
     * 
     * @param identifier
     *            the module source identifier
     * @return the module record or {@code null} if not found
     */
    ModuleRecord get(SourceIdentifier identifier);

    /**
     * Defines a new module record in this module loader.
     * 
     * @param identifier
     *            the module source identifier
     * @param module
     *            the module record
     * @throws IllegalArgumentException
     *             if the module record cannot be processed by this loader
     */
    void define(SourceIdentifier identifier, ModuleRecord module) throws IllegalArgumentException;

    /**
     * Defines a new module record in this module loader.
     * 
     * @param identifier
     *            the module source identifier
     * @param source
     *            the module source code
     * @return the new module record
     * @throws IOException
     *             if there was any I/O error
     * @throws ParserException
     *             if the module source contains any syntax errors
     * @throws CompilationException
     *             if the parsed module source cannot be compiled
     */
    ModuleRecord define(SourceIdentifier identifier, ModuleSource source) throws IOException,
            ParserException, CompilationException;

    /**
     * Resolves all dependencies of a module record.
     * 
     * @param module
     *            the module record
     * @throws IOException
     *             if there was any I/O error
     * @throws MalformedNameException
     *             if the module specifier cannot be normalized
     * @throws IllegalArgumentException
     *             if the module record cannot be processed by this loader
     */
    void fetch(ModuleRecord module) throws IOException, MalformedNameException,
            IllegalArgumentException;

    /**
     * Links the module record to a realm instance.
     * 
     * @param module
     *            the module record
     * @param realm
     *            the realm record
     * @return {@code true} if the module was successfully linked to the realm
     * @throws IllegalArgumentException
     *             if the module record cannot be processed by this loader
     */
    boolean link(ModuleRecord module, Realm realm) throws IllegalArgumentException;

    /**
     * Returns the collection of modules hold by this module loader.
     * 
     * @return the module collection
     */
    Collection<? extends ModuleRecord> getModules();
}
