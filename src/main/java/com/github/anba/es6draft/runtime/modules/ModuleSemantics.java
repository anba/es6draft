/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.modules;

import static com.github.anba.es6draft.runtime.ExecutionContext.newModuleDeclarationExecutionContext;
import static com.github.anba.es6draft.runtime.ExecutionContext.newModuleExecutionContext;
import static com.github.anba.es6draft.runtime.LexicalEnvironment.newModuleEnvironment;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.ModuleNamespaceObject.ModuleNamespaceCreate;
import static com.github.anba.es6draft.semantics.StaticSemantics.ExportEntries;
import static com.github.anba.es6draft.semantics.StaticSemantics.ImportEntries;
import static com.github.anba.es6draft.semantics.StaticSemantics.ModuleRequests;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.anba.es6draft.Module;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.ModuleEnvironmentRecord;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.ShadowRealm;
import com.github.anba.es6draft.runtime.internal.Errors;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.ModuleLoader;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.internal.ScriptLoader;
import com.github.anba.es6draft.runtime.internal.Source;
import com.github.anba.es6draft.runtime.types.builtins.ModuleNamespaceObject;

/**
 * <ul>
 * <li>8.6.1 HostNormalizeModuleName
 * <li>8.6.2 HostGetSource
 * <li>15.2.1.12.1 CreateModule
 * <li>15.2.1.12.2 ModuleAt
 * <li>15.2.1.13 ParseModuleAndImports
 * <li>15.2.1.13.1 NormalizeModuleName
 * <li>15.2.1.14 GetExportedNames
 * <li>15.2.1.15 ResolveExport
 * <li>15.2.1.16 ModuleEvaluationJob
 * <li>15.2.1.17 LinkModules
 * <li>15.2.1.18 ModuleDeclarationInstantiation
 * <li>15.2.1.19 ModuleEvaluation
 * </ul>
 */
public final class ModuleSemantics {
    private static final boolean DEBUG = false;

    private ModuleSemantics() {
    }

    private static boolean isHostNormalizedModuleName(String moduleId) {
        // TODO
        return moduleId != null;
    }

    /**
     * 8.6.1 HostNormalizeModuleName ( unnormalizedName, referrerId ) Abstract Operation
     * 
     * @param moduleLoader
     *            the module loader
     * @param unnormalizedName
     *            the unnormalized module name
     * @param referrerId
     *            the parent module identifier or {@code null}
     * @return the normalized module identifier or {@code null}
     */
    public static String HostNormalizeModuleName(ModuleLoader moduleLoader,
            String unnormalizedName, String referrerId) {
        assert referrerId == null || isHostNormalizedModuleName(referrerId);
        return moduleLoader.normalizeName(unnormalizedName, referrerId);
    }

    /**
     * 8.6.2 HostGetSource (moduleId) Abstract Operation
     * 
     * @param moduleLoader
     *            the module loader
     * @param moduleId
     *            the module identifier
     * @return the module source code
     * @throws IOException
     *             if there was any I/O error
     */
    public static String HostGetSource(ModuleLoader moduleLoader, String moduleId)
            throws IOException {
        return moduleLoader.getSource(moduleId);
    }

    /**
     * HostGetSourceFile (moduleId) Abstract Operation
     * 
     * @param moduleLoader
     *            the module loader
     * @param moduleId
     *            the module identifier
     * @return the module source code file
     */
    public static Path HostGetSourceFile(ModuleLoader moduleLoader, String moduleId) {
        return moduleLoader.getSourceFile(moduleId);
    }

    /**
     * 15.2.1.12.1 CreateModule(moduleId) Abstract Operation
     * 
     * @param moduleId
     *            the module identifier
     * @return a new module record
     */
    public static ModuleRecord CreateModule(String moduleId) {
        /* steps 1-5 */
        return new ModuleRecord(moduleId);
    }

    /**
     * 15.2.1.12.2 ModuleAt( list, moduleId )
     * 
     * @param list
     *            the module list
     * @param moduleId
     *            the module identifier
     * @return the requested module or {@code null}
     */
    public static ModuleRecord ModuleAt(Map<String, ModuleRecord> list, String moduleId) {
        /* step 1 (not applicable) */
        /* step 2 */
        assert isHostNormalizedModuleName(moduleId);
        /* steps 3-4 */
        return list.get(moduleId);
    }

    /**
     * 15.2.1.13 ParseModuleAndImports ( realm, moduleId, visited )
     * 
     * @param realm
     *            the realm instance
     * @param moduleId
     *            the module identifier
     * @param visited
     *            the list of previously visited modules
     * @return the parsed module record
     * @throws IOException
     *             if there was any I/O error
     * @throws MalformedNameException
     *             if any imported module request cannot be normalized
     * @throws ParserException
     *             if the module source contains any syntax errors
     */
    public static ModuleRecord ParseModuleAndImports(ShadowRealm realm, String moduleId,
            Map<String, ModuleRecord> visited) throws IOException, MalformedNameException,
            ParserException {
        /* step 1 (not applicable) */
        /* step 2 */
        assert isHostNormalizedModuleName(moduleId);
        /* step 3 */
        ModuleRecord visitedModule = ModuleAt(visited, moduleId);
        /* step 4 */
        if (visitedModule != null) {
            if (DEBUG) {
                System.out.printf("Module '%s' in visited%n", moduleId);
            }
            return visitedModule;
        }
        /* step 5 */
        Map<String, ModuleRecord> mods = realm.getModules();
        /* step 6 */
        ModuleRecord realmModule = ModuleAt(mods, moduleId);
        /* step 7 */
        if (realmModule != null) {
            if (DEBUG) {
                System.out.printf("Module '%s' in realm.[[modules]]%n", moduleId);
            }
            return realmModule;
        }
        /* steps 10-11 */
        String src = HostGetSource(realm.getModuleLoader(), moduleId);
        Path file = HostGetSourceFile(realm.getModuleLoader(), moduleId);
        /* steps 8-9, 12-30 */
        return ParseModuleAndImports(realm, moduleId, src, file, visited);
    }

    /**
     * 15.2.1.13 ParseModuleAndImports ( realm, moduleId, visited )
     * 
     * @param realm
     *            the realm instance
     * @param moduleId
     *            the module identifier
     * @param src
     *            the module source code
     * @param file
     *            the module source file
     * @param visited
     *            the list of previously visited modules
     * @return the parsed module record
     * @throws IOException
     *             if there was any I/O error
     * @throws MalformedNameException
     *             if any imported module request cannot be normalized
     * @throws ParserException
     *             if the module source contains any syntax errors
     */
    private static ModuleRecord ParseModuleAndImports(ShadowRealm realm, String moduleId,
            String src, Path file, Map<String, ModuleRecord> visited) throws IOException,
            MalformedNameException, ParserException {
        HashMap<String, String> normalizedNames = new HashMap<>();
        /* steps 1-7 (not applicable) */
        /* step 8 */
        ModuleRecord m = CreateModule(moduleId);
        /* step 9 */
        visited.put(moduleId, m);
        /* steps 10-11 (not applicable) */
        /* steps 12-13 */
        Source source = new Source(file, moduleId, 1);
        ScriptLoader scriptLoader = realm.getScriptLoader();
        com.github.anba.es6draft.ast.Module parsedBody = scriptLoader.parseModule(source, src);
        /* step 14 (moved) */
        /* step 15 */
        Set<String> requestedModules = ModuleRequests(parsedBody);
        /* step 16 */
        ArrayList<String> importedModules = new ArrayList<>();
        /* step 17 */
        for (String requestedName : requestedModules) {
            String requestedModuleId = NormalizeModuleName(realm, normalizedNames, requestedName,
                    moduleId);
            importedModules.add(requestedModuleId);
            ParseModuleAndImports(realm, requestedModuleId, visited);
        }
        /* step 18 */
        m.setImportedModules(importedModules);
        /* step 19 */
        List<ImportEntry> importEntries = ImportEntries(parsedBody);
        /* step 20 */
        for (ImportEntry importEntry : importEntries) {
            String requestedModuleId = NormalizeModuleName(realm, normalizedNames,
                    importEntry.getModuleRequest(), moduleId);
            importEntry.setModuleRequestId(requestedModuleId);
        }
        /* step 21 */
        m.setImportEntries(importEntries);
        /* step 22 */
        ArrayList<ExportEntry> indirectExportEntries = new ArrayList<>();
        /* step 23 */
        ArrayList<ExportEntry> localExportEntries = new ArrayList<>();
        /* step 24 */
        ArrayList<ExportEntry> starExportEntries = new ArrayList<>();
        /* step 25 */
        List<ExportEntry> exportEntries = ExportEntries(parsedBody);
        /* step 26 */
        for (ExportEntry exportEntry : exportEntries) {
            if (exportEntry.getModuleRequest() == null) {
                localExportEntries.add(exportEntry);
            } else {
                String requestedModuleId = NormalizeModuleName(realm, normalizedNames,
                        exportEntry.getModuleRequest(), moduleId);
                exportEntry.setModuleRequestId(requestedModuleId);
                if (exportEntry.isStarExport()) {
                    starExportEntries.add(exportEntry);
                } else {
                    indirectExportEntries.add(exportEntry);
                }
            }
        }
        /* step 27 */
        m.setLocalExportEntries(localExportEntries);
        /* step 28 */
        m.setIndirectExportEntries(indirectExportEntries);
        /* step 29 */
        m.setStarExportEntries(starExportEntries);
        /* step 14 */
        Module compiledBody = scriptLoader.load(parsedBody, m);
        m.setScriptCode(compiledBody);
        /* step 30 */
        return m;
    }

    /**
     * 15.2.1.13.1 NormalizeModuleName ( unnormalizedName, referrerId )
     * 
     * @param cx
     *            the execution context
     * @param realm
     *            the realm instance
     * @param unnormalizedName
     *            the unnormalized module name
     * @param referrerId
     *            the parent module identifier
     * @return the normalized module identifier
     * @throws ScriptException
     *             if the module name cannot be normalized
     */
    public static String NormalizeModuleName(ExecutionContext cx, Realm realm,
            String unnormalizedName, String referrerId) throws ScriptException {
        try {
            return NormalizeModuleName(realm, unnormalizedName, referrerId);
        } catch (MalformedNameException e) {
            throw e.toScriptException(cx);
        }
    }

    /**
     * 15.2.1.13.1 NormalizeModuleName ( unnormalizedName, referrerId )
     * 
     * @param realm
     *            the realm instance
     * @param normalizedNames
     *            the map of already normalized module names
     * @param unnormalizedName
     *            the unnormalized module name
     * @param referrerId
     *            the parent module identifier
     * @return the normalized module identifier
     * @throws MalformedNameException
     *             if the module name cannot be normalized
     */
    private static String NormalizeModuleName(ShadowRealm realm,
            Map<String, String> normalizedNames, String unnormalizedName, String referrerId)
            throws MalformedNameException {
        String moduleId = normalizedNames.get(unnormalizedName);
        if (moduleId == null) {
            moduleId = NormalizeModuleName(realm, unnormalizedName, referrerId);
            normalizedNames.put(unnormalizedName, moduleId);
        }
        return moduleId;
    }

    /**
     * 15.2.1.13.1 NormalizeModuleName ( unnormalizedName, referrerId )
     * 
     * @param realm
     *            the realm instance
     * @param unnormalizedName
     *            the unnormalized module name
     * @param referrerId
     *            the parent module identifier
     * @return the normalized module identifier
     * @throws MalformedNameException
     *             if the module name cannot be normalized
     */
    public static String NormalizeModuleName(ShadowRealm realm, String unnormalizedName,
            String referrerId) throws MalformedNameException {
        /* step 1 */
        String moduleId = HostNormalizeModuleName(realm.getModuleLoader(), unnormalizedName,
                referrerId);
        if (DEBUG) {
            System.out.printf("Normalized '%s' -> '%s'%n", unnormalizedName, moduleId);
        }
        /* step 2 */
        if (moduleId == null) {
            throw new MalformedNameException(unnormalizedName);
        }
        /* step 3 */
        return moduleId;
    }

    /**
     * 15.2.1.14 Static Semantics: GetExportedNames(modules, moduleId, circularitySet)
     * 
     * @param modules
     *            the list of available modules
     * @param moduleId
     *            the module identifier
     * @param circularitySet
     *            the list of previously visited modules
     * @return the list of exported names for {@code moduleId}
     */
    public static Set<String> GetExportedNames(Map<String, ModuleRecord> modules, String moduleId,
            Map<String, ModuleRecord> circularitySet) {
        /* step 1 */
        assert modules.containsKey(moduleId);
        /* step 2 */
        if (ModuleAt(circularitySet, moduleId) != null) {
            return Collections.emptySet();
        }
        /* step 3 */
        ModuleRecord m = ModuleAt(modules, moduleId);
        /* step 4 */
        circularitySet.put(moduleId, m);
        /* step 5 */
        HashSet<String> exportedNames = new HashSet<>();
        /* step 6 */
        for (ExportEntry exportEntry : m.getLocalExportEntries()) {
            /* step 6.a (not applicable) */
            /* step 6.b */
            exportedNames.add(exportEntry.getExportName());
        }
        /* step 7 */
        for (ExportEntry exportEntry : m.getIndirectExportEntries()) {
            /* step 7.a (not applicable) */
            /* step 7.b */
            exportedNames.add(exportEntry.getExportName());
        }
        /* step 8 */
        for (ExportEntry exportEntry : m.getStarExportEntries()) {
            /* step 8.a */
            HashMap<String, ModuleRecord> circularitySetCopy = new HashMap<>(circularitySet);
            /* step 8.b */
            Set<String> starNames = GetExportedNames(modules, exportEntry.getModuleRequestId(),
                    circularitySetCopy);
            /* step 8.c */
            for (String n : starNames) {
                if (!exportedNames.contains(n) && !"default".equals(n)) {
                    exportedNames.add(n);
                }
            }
        }
        /* step 9 */
        return exportedNames;
    }

    /**
     * 15.2.1.15 Static Semantics: ResolveExport( modules, moduleId, exportName, circularitySet)
     * 
     * @param modules
     *            the list of available modules
     * @param moduleId
     *            the module identifier
     * @param exportName
     *            the requested export name
     * @param circularitySet
     *            the list of previously visited modules
     * @return the list of exported names for {@code moduleId}
     * @throws ResolutionException
     *             if the export cannot be resolved
     */
    public static ModuleExport ResolveExport(Map<String, ModuleRecord> modules, String moduleId,
            String exportName, Map<String, Set<String>> circularitySet) throws ResolutionException {
        /* step 1 */
        assert modules.containsKey(moduleId);
        /* step 2 */
        ModuleRecord m = ModuleAt(modules, moduleId);
        /* step 3 */
        Set<String> resolvedExports = circularitySet.get(moduleId);
        if (resolvedExports == null) {
            circularitySet.put(moduleId, resolvedExports = new HashSet<>());
        } else if (resolvedExports.contains(exportName)) {
            throw new ResolutionException(Messages.Key.ModulesCyclicExport, exportName);
        }
        /* step 4 */
        resolvedExports.add(exportName);
        /* step 5 */
        for (ExportEntry exportEntry : m.getLocalExportEntries()) {
            if (exportName.equals(exportEntry.getExportName())) {
                /* step 5.a.i (not applicable) */
                /* step 5.a.ii */
                return new ModuleExport(m, exportEntry.getLocalName());
            }
        }
        /* step 6 */
        for (ExportEntry exportEntry : m.getIndirectExportEntries()) {
            if (exportName.equals(exportEntry.getExportName())) {
                /* step 6.a.i (not applicable) */
                /* step 6.a.ii */
                return ResolveExport(modules, exportEntry.getModuleRequestId(),
                        exportEntry.getImportName(), circularitySet);
            }
        }
        /* step 7 */
        if ("default".equals(exportName)) {
            /* step 7.a (not applicable) */
            /* step 7.b */
            throw new ResolutionException(Messages.Key.ModulesMissingDefaultExport, moduleId);
        }
        /* step 8 */
        ModuleExport starResolution = null;
        /* step 9 */
        for (ExportEntry exportEntry : m.getStarExportEntries()) {
            /* step 9.a */
            Map<String, Set<String>> circularitySetCopy = copy(circularitySet);
            /* steps 9.b-9.c */
            ModuleExport resolution = ResolveExport(modules, exportEntry.getModuleRequestId(),
                    exportName, circularitySetCopy);
            /* step 9.d */
            if (resolution != null) {
                if (starResolution != null) {
                    /* step 9.d.i.1 (not applicable) */
                    /* step 9.d.i.2 */
                    throw new ResolutionException(Messages.Key.ModulesDuplicateStarExport,
                            exportName);
                }
                starResolution = resolution;
            }
        }
        /* step 10 */
        return starResolution;
    }

    /**
     * 15.2.1.16 Runtime Semantics: ModuleEvaluationJob ( moduleId )
     * 
     * @param cx
     *            the execution context
     * @param realm
     *            the realm instance
     * @param moduleId
     *            the module identifier
     * @return the module record
     * @throws ScriptException
     *             if the module cannot be loaded
     */
    public static ModuleRecord LoadModule(ExecutionContext cx, Realm realm, String moduleId)
            throws ScriptException {
        try {
            return LoadModule(realm, moduleId);
        } catch (MalformedNameException | ParserException | ResolutionException e) {
            throw e.toScriptException(cx);
        } catch (IOException e) {
            throw Errors.newInternalError(cx, Messages.Key.ModulesIOException, e.getMessage());
        }
    }

    /**
     * 15.2.1.16 Runtime Semantics: ModuleEvaluationJob ( moduleId )
     * 
     * @param realm
     *            the realm instance
     * @param moduleId
     *            the module identifier
     * @return the module record
     * @throws IOException
     *             if there was any I/O error
     * @throws MalformedNameException
     *             if any imported module request cannot be normalized
     * @throws ParserException
     *             if the module source contains any syntax errors
     * @throws ResolutionException
     *             if any export binding cannot be resolved
     */
    public static ModuleRecord LoadModule(Realm realm, String moduleId) throws IOException,
            MalformedNameException, ParserException, ResolutionException {
        /* step 1 (not applicable) */
        /* step 2 */
        assert isHostNormalizedModuleName(moduleId);
        /* step 3 (not applicable) */
        /* step 4 */
        Map<String, ModuleRecord> modules = realm.getModules();
        /* step 5 */
        ModuleRecord m = ModuleAt(modules, moduleId);
        /* step 6 */
        if (m == null) {
            /* step 6.a */
            HashMap<String, ModuleRecord> newModules = new HashMap<>();
            /* steps 6.b-6.c */
            m = ParseModuleAndImports(realm, moduleId, newModules);
            /* steps 6.d-6.e */
            LinkModules(realm.defaultContext(), realm, newModules);
        }
        return m;
    }

    /**
     * 15.2.1.16 Runtime Semantics: ModuleEvaluationJob ( moduleId )
     * 
     * @param cx
     *            the execution context
     * @param realm
     *            the realm instance
     * @param moduleId
     *            the module identifier
     * @throws ScriptException
     *             if the module cannot be evaluated
     */
    public static void ModuleEvaluationJob(ExecutionContext cx, Realm realm, String moduleId)
            throws ScriptException {
        try {
            ModuleEvaluationJob(realm, moduleId);
        } catch (MalformedNameException | ParserException | ResolutionException e) {
            throw e.toScriptException(cx);
        } catch (IOException e) {
            throw Errors.newInternalError(cx, Messages.Key.ModulesIOException, e.getMessage());
        }
    }

    /**
     * 15.2.1.16 Runtime Semantics: ModuleEvaluationJob ( moduleId )
     * 
     * @param realm
     *            the realm instance
     * @param moduleId
     *            the module identifier
     * @throws IOException
     *             if there was any I/O error
     * @throws MalformedNameException
     *             if any imported module request cannot be normalized
     * @throws ParserException
     *             if the module source contains any syntax errors
     * @throws ResolutionException
     *             if any export binding cannot be resolved
     */
    public static void ModuleEvaluationJob(Realm realm, String moduleId) throws IOException,
            MalformedNameException, ParserException, ResolutionException {
        /* steps 1-6 */
        ModuleRecord m = LoadModule(realm, moduleId);
        /* steps 7-8 */
        ModuleEvaluation(m, realm);
    }

    /**
     * 15.2.1.16 Runtime Semantics: ModuleEvaluationJob ( moduleId )
     * 
     * @param cx
     *            the execution context
     * @param realm
     *            the realm instance
     * @param moduleId
     *            the module identifier
     * @param sourceCode
     *            the module source code
     * @throws ScriptException
     *             if the module cannot be evaluated
     */
    public static void ModuleEvaluationJob(ExecutionContext cx, Realm realm, String moduleId,
            String sourceCode) throws ScriptException {
        try {
            ModuleEvaluationJob(realm, moduleId, sourceCode);
        } catch (MalformedNameException | ParserException | ResolutionException e) {
            throw e.toScriptException(cx);
        } catch (IOException e) {
            throw Errors.newInternalError(cx, Messages.Key.ModulesIOException, e.getMessage());
        }
    }

    /**
     * 15.2.1.16 Runtime Semantics: ModuleEvaluationJob ( moduleId )
     * 
     * @param realm
     *            the realm instance
     * @param moduleId
     *            the module identifier
     * @param sourceCode
     *            the module source code
     * @throws IOException
     *             if there was any I/O error
     * @throws MalformedNameException
     *             if any imported module request cannot be normalized
     * @throws ParserException
     *             if the module source contains any syntax errors
     * @throws ResolutionException
     *             if any export binding cannot be resolved
     */
    public static void ModuleEvaluationJob(Realm realm, String moduleId, String sourceCode)
            throws IOException, MalformedNameException, ParserException, ResolutionException {
        /* step 1 (not applicable) */
        /* step 2 */
        assert isHostNormalizedModuleName(moduleId);
        /* step 3 (not applicable) */
        /* step 4 */
        Map<String, ModuleRecord> modules = realm.getModules();
        /* step 5 */
        ModuleRecord m = ModuleAt(modules, moduleId);
        /* step 6 */
        if (m == null) {
            /* step 6.a */
            HashMap<String, ModuleRecord> newModules = new HashMap<>();
            /* steps 6.b-6.c */
            m = ParseModuleAndImports(realm, moduleId, sourceCode, null, newModules);
            /* steps 6.d-6.e */
            LinkModules(realm.defaultContext(), realm, newModules);
        }
        /* steps 7-8 */
        ModuleEvaluation(m, realm);
    }

    /**
     * 15.2.1.17 Runtime Semantics: LinkModules( realm, newModuleSet)
     * 
     * @param cx
     *            the execution context
     * @param realm
     *            the realm instance
     * @param newModuleSet
     *            the list of modules to link
     * @throws ResolutionException
     *             if any export binding cannot be resolved
     */
    public static void LinkModules(ExecutionContext cx, Realm realm,
            Map<String, ModuleRecord> newModuleSet) throws ResolutionException {
        /* step 1 */
        HashMap<String, ModuleRecord> modules = new HashMap<>(realm.getModules());
        /* step 2 */
        modules.putAll(newModuleSet);
        /* step 3 */
        for (ModuleRecord m : newModuleSet.values()) {
            ModuleDeclarationInstantiation(cx, m, realm, modules);
        }
        /* step 4 (not applicable) */
        /* step 5 */
        realm.getModules().putAll(newModuleSet);
        /* step 6 (return) */
    }

    /**
     * 15.2.1.18 Runtime Semantics: ModuleDeclarationInstantiation( module, realm, moduleSet )
     * 
     * @param cx
     *            the execution context
     * @param module
     *            the module record
     * @param realm
     *            the realm instance
     * @param moduleSet
     *            the list of available modules
     * @throws ResolutionException
     *             if any export binding cannot be resolved
     */
    public static void ModuleDeclarationInstantiation(ExecutionContext cx, ModuleRecord module,
            Realm realm, Map<String, ModuleRecord> moduleSet) throws ResolutionException {
        /* steps 1, 3-4, 6 (not applicable) */
        /* step 2 */
        Module code = module.getScriptCode();
        /* step 5 */
        LexicalEnvironment<ModuleEnvironmentRecord> env = newModuleEnvironment(realm.getGlobalEnv());
        /* step 7 */
        module.setEnvironment(env);
        /* steps 8-13 */
        ExecutionContext context = newModuleDeclarationExecutionContext(realm, code);
        code.getModuleBody().moduleDeclarationInstantiation(context, env, realm, moduleSet);
    }

    /**
     * 15.2.1.18 Runtime Semantics: ModuleDeclarationInstantiation( module, realm, moduleSet )
     * 
     * @param cx
     *            the execution context
     * @param realm
     *            the realm instance
     * @param moduleId
     *            the module identifier
     * @return the module namespace object
     */
    public static ModuleNamespaceObject GetModuleNamespace(ExecutionContext cx, Realm realm,
            String moduleId) {
        /* steps 8.a.ii-8.a.v */
        return GetModuleNamespace(cx, realm, realm.getModules(), moduleId);
    }

    /**
     * 15.2.1.18 Runtime Semantics: ModuleDeclarationInstantiation( module, realm, moduleSet )
     * 
     * @param cx
     *            the execution context
     * @param realm
     *            the realm instance
     * @param moduleSet
     *            the list of modules
     * @param moduleId
     *            the module identifier
     * @return the module namespace object
     */
    public static ModuleNamespaceObject GetModuleNamespace(ExecutionContext cx, Realm realm,
            Map<String, ModuleRecord> moduleSet, String moduleId) {
        /* step 8.a.ii */
        ModuleRecord importedModule = ModuleAt(moduleSet, moduleId);
        /* step 8.a.iii */
        assert importedModule != null;
        /* step 8.a.iv */
        ModuleNamespaceObject namespace = importedModule.getNamespace();
        /* step 8.a.v */
        if (namespace == null) {
            Set<String> exportedNames = GetExportedNames(moduleSet, moduleId,
                    new HashMap<String, ModuleRecord>());
            namespace = ModuleNamespaceCreate(cx, importedModule, realm, exportedNames);
        }
        return namespace;
    }

    /**
     * 15.2.1.19 Runtime Semantics: ModuleEvaluation(module, realm)
     * 
     * @param module
     *            the module record
     * @param realm
     *            the realm instance
     * @return the module evaluation result
     */
    public static Object ModuleEvaluation(ModuleRecord module, Realm realm) {
        /* step 1 */
        if (module.isEvaluated()) {
            return UNDEFINED;
        }
        /* step 2 */
        module.setEvaluated(true);
        /* step 3 */
        for (String id : module.getImportedModules()) {
            assert realm.getModules().containsKey(id);
            ModuleRecord requires = ModuleAt(realm.getModules(), id);
            ModuleEvaluation(requires, realm);
        }
        /* steps 4-9 */
        ExecutionContext moduleContext = newModuleExecutionContext(realm, module);
        /* steps 10-11 */
        ExecutionContext oldScriptContext = realm.getScriptContext();
        try {
            realm.setScriptContext(moduleContext);
            /* step 12 */
            Object result = module.getScriptCode().evaluate(moduleContext);
            /* step 15 */
            return result;
        } finally {
            /* steps 13-14 */
            realm.setScriptContext(oldScriptContext);
        }
    }

    private static <K, V> Map<K, Set<V>> copy(Map<K, Set<V>> source) {
        HashMap<K, Set<V>> copy = new HashMap<>(source);
        for (Map.Entry<K, Set<V>> entry : copy.entrySet()) {
            entry.setValue(new HashSet<>(entry.getValue()));
        }
        return copy;
    }
}
