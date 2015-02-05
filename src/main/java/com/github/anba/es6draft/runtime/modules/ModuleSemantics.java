/**
 * Copyright (c) 2012-2015 André Bargull
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.internal.ScriptLoader;
import com.github.anba.es6draft.runtime.types.builtins.ModuleNamespaceObject;

/**
 * <ul>
 * <li>8.6.1 HostNormalizeModuleName
 * <li>8.6.2 HostGetSource
 * <li>15.2.1.15.1 CreateModule
 * <li>15.2.1.15.2 ModuleAt
 * <li>15.2.1.16 ParseModuleAndImports
 * <li>15.2.1.16.1 NormalizeModuleName
 * <li>15.2.1.17 GetExportedNames
 * <li>15.2.1.18 ResolveExport
 * <li>15.2.1.19 ModuleEvaluationJob
 * <li>15.2.1.20 LinkModules
 * <li>15.2.1.21 ModuleDeclarationInstantiation
 * <li>15.2.1.22 ModuleEvaluation
 * </ul>
 */
public final class ModuleSemantics {
    private static final boolean DEBUG = false;

    private ModuleSemantics() {
    }

    /**
     * 8.6.1 HostGetSource (sourceCodeId) Abstract Operation
     * 
     * @param moduleLoader
     *            the module loader
     * @param sourceCodeId
     *            the module source code identifier
     * @return the module source code
     * @throws IOException
     *             if there was any I/O error
     */
    public static ModuleSource HostGetSource(ModuleLoader moduleLoader,
            SourceIdentifier sourceCodeId) throws IOException {
        return moduleLoader.getSource(sourceCodeId);
    }

    /**
     * 8.6.2 HostNormalizeModuleName ( unnormalizedName, referrerId) Abstract Operation
     * 
     * @param moduleLoader
     *            the module loader
     * @param unnormalizedName
     *            the unnormalized module name
     * @param referrerId
     *            the parent module identifier or {@code null}
     * @return the normalized module identifier or {@code null}
     */
    public static SourceIdentifier HostNormalizeModuleName(ModuleLoader moduleLoader,
            String unnormalizedName, SourceIdentifier referrerId) {
        return moduleLoader.normalizeName(unnormalizedName, referrerId);
    }

    /**
     * 15.2.1.15.1 CreateModule(moduleId) Abstract Operation
     * 
     * @param sourceCodeId
     *            the module source code identifier
     * @return a new module record
     */
    public static ModuleRecord CreateModule(SourceIdentifier sourceCodeId) {
        /* steps 1-5 */
        return new ModuleRecord(sourceCodeId);
    }

    /**
     * 15.2.1.15.2 ModuleAt( list, moduleId )
     * 
     * @param list
     *            the module list
     * @param sourceCodeId
     *            the module source code identifier
     * @return the requested module or {@code null}
     */
    public static ModuleRecord ModuleAt(Map<SourceIdentifier, ModuleRecord> list,
            SourceIdentifier sourceCodeId) {
        /* steps 1-2 (not applicable) */
        /* steps 3-4 */
        return list.get(sourceCodeId);
    }

    /**
     * 15.2.1.16 Static Semantics: ParseModuleAndImports ( realm, moduleSrcId, visited )
     * 
     * @param realm
     *            the realm instance
     * @param moduleSrcId
     *            the module source code identifier
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
    public static ModuleRecord ParseModuleAndImports(ShadowRealm realm,
            SourceIdentifier moduleSrcId, LinkedHashMap<SourceIdentifier, ModuleRecord> visited)
            throws IOException, MalformedNameException, ParserException {
        /* steps 1-2 (not applicable) */
        /* step 3 */
        ModuleRecord visitedModule = ModuleAt(visited, moduleSrcId);
        /* step 4 */
        if (visitedModule != null) {
            if (DEBUG) {
                System.out.printf("Module '%s' in visited%n", moduleSrcId);
            }
            return visitedModule;
        }
        /* step 5 */
        Map<SourceIdentifier, ModuleRecord> mods = realm.getModules();
        /* step 6 */
        ModuleRecord realmModule = ModuleAt(mods, moduleSrcId);
        /* step 7 */
        if (realmModule != null) {
            if (DEBUG) {
                System.out.printf("Module '%s' in realm.[[modules]]%n", moduleSrcId);
            }
            return realmModule;
        }
        /* steps 10-11 */
        ModuleSource src = HostGetSource(realm.getModuleLoader(), moduleSrcId);
        /* steps 8-9, 12-30 */
        return ParseModuleAndImports(realm, moduleSrcId, src, visited);
    }

    /**
     * 15.2.1.16 Static Semantics: ParseModuleAndImports ( realm, moduleSrcId, visited )
     * 
     * @param realm
     *            the realm instance
     * @param moduleSrcId
     *            the module source code identifier
     * @param source
     *            the module source code
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
    private static ModuleRecord ParseModuleAndImports(ShadowRealm realm,
            SourceIdentifier moduleSrcId, ModuleSource source,
            LinkedHashMap<SourceIdentifier, ModuleRecord> visited) throws IOException,
            MalformedNameException, ParserException {
        HashMap<String, SourceIdentifier> normalizedNames = new HashMap<>();
        /* steps 1-7 (not applicable) */
        /* step 8 */
        ModuleRecord m = CreateModule(moduleSrcId);
        /* step 9 */
        visited.put(moduleSrcId, m);
        /* steps 10-11 (not applicable) */
        /* steps 12-13 */
        ScriptLoader scriptLoader = realm.getScriptLoader();
        com.github.anba.es6draft.ast.Module parsedBody = scriptLoader.parseModule(
                source.toSource(), source.sourceCode());
        /* step 14 (moved) */
        /* step 15 */
        Set<String> requestedModules = ModuleRequests(parsedBody);
        /* step 16 */
        LinkedHashMap<SourceIdentifier, ModuleRecord> importedModules = new LinkedHashMap<>();
        /* step 17 */
        for (String requestedName : requestedModules) {
            SourceIdentifier requestedSrcId = NormalizeModuleName(realm, normalizedNames,
                    requestedName, moduleSrcId);
            ModuleRecord importedModule = ParseModuleAndImports(realm, requestedSrcId, visited);
            importedModules.put(requestedSrcId, importedModule);
        }
        /* step 18 */
        m.setImportedModules(new ArrayList<>(importedModules.values()));
        /* step 19 */
        List<ImportEntry> importEntries = ImportEntries(parsedBody);
        /* step 20 */
        for (ImportEntry importEntry : importEntries) {
            SourceIdentifier requestedSrcId = NormalizeModuleName(realm, normalizedNames,
                    importEntry.getModuleRequest(), moduleSrcId);
            ModuleRecord importedModule = importedModules.get(requestedSrcId);
            assert importedModule != null;
            importEntry.setImportModule(importedModule);
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
                SourceIdentifier requestedSrcId = NormalizeModuleName(realm, normalizedNames,
                        exportEntry.getModuleRequest(), moduleSrcId);
                ModuleRecord importedModule = importedModules.get(requestedSrcId);
                assert importedModule != null;
                exportEntry.setImportModule(importedModule);
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
     * 15.2.1.16.1 NormalizeModuleName(unnormalizedName, referrerSrcId )
     * 
     * @param cx
     *            the execution context
     * @param realm
     *            the realm instance
     * @param unnormalizedName
     *            the unnormalized module name
     * @param referrerSrcId
     *            the parent module source code identifier
     * @return the normalized module identifier
     * @throws ScriptException
     *             if the module name cannot be normalized
     */
    public static SourceIdentifier NormalizeModuleName(ExecutionContext cx, Realm realm,
            String unnormalizedName, SourceIdentifier referrerSrcId) throws ScriptException {
        try {
            return NormalizeModuleName(realm, unnormalizedName, referrerSrcId);
        } catch (MalformedNameException e) {
            throw e.toScriptException(cx);
        }
    }

    /**
     * 15.2.1.16.1 NormalizeModuleName ( unnormalizedName, referrerId )
     * 
     * @param realm
     *            the realm instance
     * @param normalizedNames
     *            the map of already normalized module names
     * @param unnormalizedName
     *            the unnormalized module name
     * @param referrerSrcId
     *            the parent module source code identifier
     * @return the normalized module identifier
     * @throws MalformedNameException
     *             if the module name cannot be normalized
     */
    private static SourceIdentifier NormalizeModuleName(ShadowRealm realm,
            Map<String, SourceIdentifier> normalizedNames, String unnormalizedName,
            SourceIdentifier referrerSrcId) throws MalformedNameException {
        SourceIdentifier moduleSrcId = normalizedNames.get(unnormalizedName);
        if (moduleSrcId == null) {
            moduleSrcId = NormalizeModuleName(realm, unnormalizedName, referrerSrcId);
            normalizedNames.put(unnormalizedName, moduleSrcId);
        }
        return moduleSrcId;
    }

    /**
     * 15.2.1.16.1 NormalizeModuleName ( unnormalizedName, referrerId )
     * 
     * @param realm
     *            the realm instance
     * @param unnormalizedName
     *            the unnormalized module name
     * @param referrerSrcId
     *            the parent module source code identifier
     * @return the normalized module identifier
     * @throws MalformedNameException
     *             if the module name cannot be normalized
     */
    public static SourceIdentifier NormalizeModuleName(ShadowRealm realm, String unnormalizedName,
            SourceIdentifier referrerSrcId) throws MalformedNameException {
        /* step 1 */
        SourceIdentifier moduleSrcId = HostNormalizeModuleName(realm.getModuleLoader(),
                unnormalizedName, referrerSrcId);
        if (DEBUG) {
            System.out.printf("Normalized '%s' -> '%s'%n", unnormalizedName, moduleSrcId);
        }
        /* step 2 */
        if (moduleSrcId == null) {
            throw new MalformedNameException(unnormalizedName);
        }
        /* step 3 */
        return moduleSrcId;
    }

    /**
     * 15.2.1.17 Static Semantics: GetExportedNames(module, circularitySet)
     * 
     * @param module
     *            the module record
     * @param circularitySet
     *            the list of previously visited modules
     * @return the list of exported names for {@code moduleId}
     */
    public static Set<String> GetExportedNames(ModuleRecord module, Set<ModuleRecord> circularitySet) {
        /* step 1 (not applicable) */
        /* step 2 */
        if (circularitySet.contains(module)) {
            return Collections.emptySet();
        }
        /* step 3 */
        circularitySet.add(module);
        /* step 4 */
        HashSet<String> exportedNames = new HashSet<>();
        /* step 5 */
        for (ExportEntry exportEntry : module.getLocalExportEntries()) {
            /* step 5.a (not applicable) */
            /* step 5.b */
            exportedNames.add(exportEntry.getExportName());
        }
        /* step 6 */
        for (ExportEntry exportEntry : module.getIndirectExportEntries()) {
            /* step 6.a (not applicable) */
            /* step 6.b */
            exportedNames.add(exportEntry.getExportName());
        }
        /* step 7 */
        for (ExportEntry exportEntry : module.getStarExportEntries()) {
            /* step 7.a */
            HashSet<ModuleRecord> circularitySetCopy = new HashSet<>(circularitySet);
            /* step 7.b */
            Set<String> starNames = GetExportedNames(exportEntry.getImportModule(),
                    circularitySetCopy);
            /* step 7.c */
            for (String n : starNames) {
                if (!exportedNames.contains(n) && !"default".equals(n)) {
                    exportedNames.add(n);
                }
            }
        }
        /* step 8 */
        return exportedNames;
    }

    /**
     * 15.2.1.18 Static Semantics: ResolveExport( module, exportName, circularitySet)
     * 
     * @param module
     *            the module record
     * @param exportName
     *            the requested export name
     * @param circularitySet
     *            the list of previously visited modules
     * @return the list of exported names for {@code moduleId}
     * @throws ResolutionException
     *             if the export cannot be resolved
     */
    public static ModuleExport ResolveExport(ModuleRecord module, String exportName,
            Map<ModuleRecord, Set<String>> circularitySet) throws ResolutionException {
        /* step 1 (not applicable) */
        /* step 2 */
        Set<String> resolvedExports = circularitySet.get(module);
        if (resolvedExports == null) {
            circularitySet.put(module, resolvedExports = new HashSet<>());
        } else if (resolvedExports.contains(exportName)) {
            throw new ResolutionException(Messages.Key.ModulesCyclicExport, exportName);
        }
        /* step 3 */
        resolvedExports.add(exportName);
        /* step 4 */
        for (ExportEntry exportEntry : module.getLocalExportEntries()) {
            if (exportName.equals(exportEntry.getExportName())) {
                /* step 4.a.i (not applicable) */
                /* step 4.a.ii */
                return new ModuleExport(module, exportEntry.getLocalName());
            }
        }
        /* step 5 */
        for (ExportEntry exportEntry : module.getIndirectExportEntries()) {
            if (exportName.equals(exportEntry.getExportName())) {
                /* step 5.a.i (not applicable) */
                /* step 5.a.ii */
                return ResolveExport(exportEntry.getImportModule(), exportEntry.getImportName(),
                        circularitySet);
            }
        }
        /* step 6 */
        if ("default".equals(exportName)) {
            /* step 6.a (not applicable) */
            /* step 6.b */
            throw new ResolutionException(Messages.Key.ModulesMissingDefaultExport, module
                    .getSourceCodeId().toString());
        }
        /* step 7 */
        ModuleExport starResolution = null;
        /* step 8 */
        for (ExportEntry exportEntry : module.getStarExportEntries()) {
            /* step 8.a */
            Map<ModuleRecord, Set<String>> circularitySetCopy = copy(circularitySet);
            /* steps 8.b-8.c */
            ModuleExport resolution = ResolveExport(exportEntry.getImportModule(), exportName,
                    circularitySetCopy);
            /* step 8.d */
            if (resolution != null) {
                if (starResolution != null) {
                    /* step 8.d.i.1 (not applicable) */
                    /* step 8.d.i.2 */
                    throw new ResolutionException(Messages.Key.ModulesDuplicateStarExport,
                            exportName);
                }
                starResolution = resolution;
            }
        }
        /* step 9 */
        return starResolution;
    }

    /**
     * 15.2.1.19 Runtime Semantics: ModuleEvaluationJob ( sourceCodeId )
     * 
     * @param cx
     *            the execution context
     * @param realm
     *            the realm instance
     * @param sourceCodeId
     *            the module source code identifier
     * @return the module record
     * @throws ScriptException
     *             if the module cannot be loaded
     */
    public static ModuleRecord LoadModule(ExecutionContext cx, Realm realm,
            SourceIdentifier sourceCodeId) throws ScriptException {
        try {
            return LoadModule(realm, sourceCodeId);
        } catch (MalformedNameException | ParserException | ResolutionException e) {
            throw e.toScriptException(cx);
        } catch (IOException e) {
            throw Errors.newInternalError(cx, Messages.Key.ModulesIOException, e.getMessage());
        }
    }

    /**
     * 15.2.1.19 Runtime Semantics: ModuleEvaluationJob ( sourceCodeId )
     * 
     * @param realm
     *            the realm instance
     * @param sourceCodeId
     *            the module source code identifier
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
    public static ModuleRecord LoadModule(Realm realm, SourceIdentifier sourceCodeId)
            throws IOException, MalformedNameException, ParserException, ResolutionException {
        /* steps 1-2 (not applicable) */
        /* step 3 (not applicable) */
        /* step 4 */
        Map<SourceIdentifier, ModuleRecord> modules = realm.getModules();
        /* step 5 */
        ModuleRecord m = ModuleAt(modules, sourceCodeId);
        /* step 6 */
        if (m == null) {
            /* step 6.a */
            LinkedHashMap<SourceIdentifier, ModuleRecord> newModules = new LinkedHashMap<>();
            /* steps 6.b-6.c */
            m = ParseModuleAndImports(realm, sourceCodeId, newModules);
            /* steps 6.d-6.e */
            LinkModules(realm, newModules);
        }
        return m;
    }

    /**
     * 15.2.1.19 Runtime Semantics: ModuleEvaluationJob ( sourceCodeId )
     * 
     * @param cx
     *            the execution context
     * @param realm
     *            the realm instance
     * @param sourceCodeId
     *            the module source code identifier
     * @throws ScriptException
     *             if the module cannot be evaluated
     */
    public static void ModuleEvaluationJob(ExecutionContext cx, Realm realm,
            SourceIdentifier sourceCodeId) throws ScriptException {
        try {
            ModuleEvaluationJob(realm, sourceCodeId);
        } catch (MalformedNameException | ParserException | ResolutionException e) {
            throw e.toScriptException(cx);
        } catch (IOException e) {
            throw Errors.newInternalError(cx, Messages.Key.ModulesIOException, e.getMessage());
        }
    }

    /**
     * 15.2.1.19 Runtime Semantics: ModuleEvaluationJob ( sourceCodeId )
     * 
     * @param realm
     *            the realm instance
     * @param sourceCodeId
     *            the module source code identifier
     * @throws IOException
     *             if there was any I/O error
     * @throws MalformedNameException
     *             if any imported module request cannot be normalized
     * @throws ParserException
     *             if the module source contains any syntax errors
     * @throws ResolutionException
     *             if any export binding cannot be resolved
     */
    public static void ModuleEvaluationJob(Realm realm, SourceIdentifier sourceCodeId)
            throws IOException, MalformedNameException, ParserException, ResolutionException {
        /* steps 1-6 */
        ModuleRecord m = LoadModule(realm, sourceCodeId);
        /* steps 7-8 */
        ModuleEvaluation(m, realm);
    }

    /**
     * 15.2.1.19 Runtime Semantics: ModuleEvaluationJob ( sourceCodeId )
     * 
     * @param cx
     *            the execution context
     * @param realm
     *            the realm instance
     * @param sourceCodeId
     *            the module source code identifier
     * @param source
     *            the module source code
     * @throws ScriptException
     *             if the module cannot be evaluated
     */
    public static void ModuleEvaluationJob(ExecutionContext cx, Realm realm,
            SourceIdentifier sourceCodeId, ModuleSource source) throws ScriptException {
        try {
            ModuleEvaluationJob(realm, sourceCodeId, source);
        } catch (MalformedNameException | ParserException | ResolutionException e) {
            throw e.toScriptException(cx);
        } catch (IOException e) {
            throw Errors.newInternalError(cx, Messages.Key.ModulesIOException, e.getMessage());
        }
    }

    /**
     * 15.2.1.19 Runtime Semantics: ModuleEvaluationJob ( sourceCodeId )
     * 
     * @param realm
     *            the realm instance
     * @param sourceCodeId
     *            the module source code identifier
     * @param source
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
    public static void ModuleEvaluationJob(Realm realm, SourceIdentifier sourceCodeId,
            ModuleSource source) throws IOException, MalformedNameException, ParserException,
            ResolutionException {
        /* steps 1-2 (not applicable) */
        /* step 3 (not applicable) */
        /* step 4 */
        Map<SourceIdentifier, ModuleRecord> modules = realm.getModules();
        /* step 5 */
        ModuleRecord m = ModuleAt(modules, sourceCodeId);
        /* step 6 */
        if (m == null) {
            /* step 6.a */
            LinkedHashMap<SourceIdentifier, ModuleRecord> newModules = new LinkedHashMap<>();
            /* steps 6.b-6.c */
            m = ParseModuleAndImports(realm, sourceCodeId, source, newModules);
            /* steps 6.d-6.e */
            LinkModules(realm, newModules);
        }
        /* steps 7-8 */
        ModuleEvaluation(m, realm);
    }

    /**
     * 15.2.1.20 Runtime Semantics: LinkModules( realm, newModuleSet)
     * 
     * @param realm
     *            the realm instance
     * @param newModuleSet
     *            the list of modules to link
     * @throws ResolutionException
     *             if any export binding cannot be resolved
     */
    public static void LinkModules(Realm realm,
            LinkedHashMap<SourceIdentifier, ModuleRecord> newModuleSet) throws ResolutionException {
        /* step 1 */
        HashMap<SourceIdentifier, ModuleRecord> modules = new HashMap<>(realm.getModules());
        /* step 2 */
        modules.putAll(newModuleSet);
        /* step 3 */
        for (ModuleRecord m : newModuleSet.values()) {
            ModuleDeclarationInstantiation(m, realm, modules);
        }
        /* step 4 (not applicable) */
        /* step 5 */
        realm.getModules().putAll(newModuleSet);
        /* step 6 (return) */
    }

    /**
     * 15.2.1.21 Runtime Semantics: ModuleDeclarationInstantiation( module, realm, moduleSet )
     * 
     * @param module
     *            the module record
     * @param realm
     *            the realm instance
     * @param moduleSet
     *            the list of available modules
     * @throws ResolutionException
     *             if any export binding cannot be resolved
     */
    public static void ModuleDeclarationInstantiation(ModuleRecord module, Realm realm,
            Map<SourceIdentifier, ModuleRecord> moduleSet) throws ResolutionException {
        /* step 1 */
        Module code = module.getScriptCode();
        /* step 4 */
        LexicalEnvironment<ModuleEnvironmentRecord> env = newModuleEnvironment(realm.getGlobalEnv());
        /* step 5 (not applicable) */
        /* step 6 */
        module.setEnvironment(env);
        /* steps 2-3, 7-12 */
        ExecutionContext context = newModuleDeclarationExecutionContext(realm, code);
        code.getModuleBody().moduleDeclarationInstantiation(context, env, moduleSet,
                sourceIdentifierMap(module));
    }

    /**
     * 15.2.1.21 Runtime Semantics: ModuleDeclarationInstantiation( module, realm, moduleSet )
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
            SourceIdentifier moduleId) {
        /* steps 7.a.i-7.a.iv */
        return GetModuleNamespace(cx, realm.getModules(), moduleId);
    }

    /**
     * 15.2.1.21 Runtime Semantics: ModuleDeclarationInstantiation( module, realm, moduleSet )
     * 
     * @param cx
     *            the execution context
     * @param moduleSet
     *            the list of modules
     * @param moduleId
     *            the module identifier
     * @return the module namespace object
     */
    public static ModuleNamespaceObject GetModuleNamespace(ExecutionContext cx,
            Map<SourceIdentifier, ModuleRecord> moduleSet, SourceIdentifier moduleId) {
        /* step 7.a.i */
        ModuleRecord importedModule = ModuleAt(moduleSet, moduleId);
        /* step 7.a.ii */
        assert importedModule != null;
        /* step 7.a.iii */
        ModuleNamespaceObject namespace = importedModule.getNamespace();
        /* step 7.a.iv */
        if (namespace == null) {
            Set<String> exportedNames = GetExportedNames(importedModule,
                    new HashSet<ModuleRecord>());
            namespace = ModuleNamespaceCreate(cx, importedModule, exportedNames);
        }
        return namespace;
    }

    /**
     * 15.2.1.22 Runtime Semantics: ModuleEvaluation(module, realm)
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
        for (ModuleRecord required : module.getImportedModules()) {
            assert realm.getModules().get(required.getSourceCodeId()) == required;
            ModuleEvaluation(required, realm);
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

    private static Map<String, SourceIdentifier> sourceIdentifierMap(ModuleRecord module) {
        HashMap<String, SourceIdentifier> identifierMap = new HashMap<>();
        identifierMap.put(module.getSourceCodeId().toString(), module.getSourceCodeId());
        for (ImportEntry importEntry : module.getImportEntries()) {
            ModuleRecord importedModule = importEntry.getImportModule();
            SourceIdentifier id = importedModule.getSourceCodeId();
            identifierMap.put(id.toString(), id);
        }
        return identifierMap;
    }
}
