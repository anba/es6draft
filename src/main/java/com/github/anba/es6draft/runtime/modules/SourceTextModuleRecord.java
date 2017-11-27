/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.modules;

import static com.github.anba.es6draft.runtime.ExecutionContext.newModuleDeclarationExecutionContext;
import static com.github.anba.es6draft.runtime.ExecutionContext.newModuleExecutionContext;
import static com.github.anba.es6draft.runtime.LexicalEnvironment.newModuleEnvironment;
import static com.github.anba.es6draft.runtime.modules.ModuleSemantics.HostResolveImportedModule;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.semantics.StaticSemantics.ExportEntries;
import static com.github.anba.es6draft.semantics.StaticSemantics.ImportEntries;
import static com.github.anba.es6draft.semantics.StaticSemantics.ImportedLocalNames;
import static com.github.anba.es6draft.semantics.StaticSemantics.ModuleRequests;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.github.anba.es6draft.Module;
import com.github.anba.es6draft.compiler.CompilationException;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.ModuleEnvironmentRecord;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.internal.ScriptLoader;
import com.github.anba.es6draft.runtime.types.ScriptObject;

/**
 * 15.2.1.16 Source Text Module Records
 */
public final class SourceTextModuleRecord implements ModuleRecord, Cloneable {
    /**
     * [[SourceCodeId]]
     */
    private final SourceIdentifier sourceCodeId;

    /**
     * [[Realm]]
     */
    private Realm realm;

    /**
     * [[Environment]]
     */
    private LexicalEnvironment<ModuleEnvironmentRecord> environment;

    /**
     * [[Namespace]]
     */
    private ScriptObject namespace;

    /**
     * [[Meta]]
     */
    private ScriptObject meta;

    /**
     * [[ECMAScriptCode]]
     */
    private Module scriptCode;

    /**
     * [[RequestedModules]]
     */
    private final Set<String> requestedModules;

    /**
     * [[ImportEntries]]
     */
    private final List<ImportEntry> importEntries;

    /**
     * [[LocalExportEntries]]
     */
    private final List<ExportEntry> localExportEntries;

    /**
     * [[IndirectExportEntries]]
     */
    private final List<ExportEntry> indirectExportEntries;

    /**
     * [[StarExportEntries]]
     */
    private final List<ExportEntry> starExportEntries;

    /**
     * Extension:<br>
     * [[NameSpaceExportEntries]]
     */
    private final List<ExportEntry> nameSpaceExportEntries;

    /**
     * [[Status]]
     */
    private Status status = Status.Uninstantiated;

    public enum Status {
        Uninstantiated, Instantiating, Instantiated, Evaluating, Evaluated
    }

    /**
     * [[EvaluationError]]
     */
    private Exception evaluationError = null;

    /**
     * [[DFSIndex]]
     */
    private int dfsIndex = -1;

    /**
     * [[DFSAncestorIndex]]
     */
    private int dfsAncestorIndex = -1;

    private SourceTextModuleRecord(SourceIdentifier sourceCodeId, Set<String> requestedModules,
            List<ImportEntry> importEntries, List<ExportEntry> localExportEntries,
            List<ExportEntry> indirectExportEntries, List<ExportEntry> starExportEntries,
            List<ExportEntry> nameSpaceExportEntries) {
        assert sourceCodeId != null;
        this.sourceCodeId = sourceCodeId;
        this.requestedModules = unmodifiableOrEmpty(requestedModules);
        this.importEntries = unmodifiableOrEmpty(importEntries);
        this.localExportEntries = unmodifiableOrEmpty(localExportEntries);
        this.indirectExportEntries = unmodifiableOrEmpty(indirectExportEntries);
        this.starExportEntries = unmodifiableOrEmpty(starExportEntries);
        this.nameSpaceExportEntries = unmodifiableOrEmpty(nameSpaceExportEntries);
    }

    private SourceTextModuleRecord(SourceTextModuleRecord module) {
        this.sourceCodeId = module.sourceCodeId;
        this.scriptCode = module.scriptCode;
        this.requestedModules = module.requestedModules;
        this.importEntries = module.importEntries;
        this.localExportEntries = module.localExportEntries;
        this.indirectExportEntries = module.indirectExportEntries;
        this.starExportEntries = module.starExportEntries;
        this.nameSpaceExportEntries = module.nameSpaceExportEntries;
    }

    private static <T> Set<T> unmodifiableOrEmpty(Set<T> set) {
        if (set.isEmpty()) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(set);
    }

    private static <T> List<T> unmodifiableOrEmpty(List<T> list) {
        if (list.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(list);
    }

    @Override
    public SourceTextModuleRecord clone() {
        return new SourceTextModuleRecord(this);
    }

    @Override
    public String toString() {
        return String.format("[Module = %s]", sourceCodeId);
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this;
    }

    @Override
    public int hashCode() {
        return sourceCodeId.hashCode();
    }

    @Override
    public Realm getRealm() {
        return realm;
    }

    public void setRealm(Realm realm) {
        assert this.realm == null : "module already linked";
        this.realm = Objects.requireNonNull(realm);
    }

    /**
     * [[SourceCodeId]]
     * 
     * @return the module source code identifier
     */
    @Override
    public SourceIdentifier getSourceCodeId() {
        return sourceCodeId;
    }

    /**
     * [[ECMAScriptCode]]
     * 
     * @return the script code for this module
     */
    public Module getScriptCode() {
        return scriptCode;
    }

    /**
     * [[RequestedModules]]
     * 
     * @return the list of requested modules
     */
    public Set<String> getRequestedModules() {
        return requestedModules;
    }

    /**
     * [[ImportEntries]]
     * 
     * @return the list of {@code import} entries
     */
    public List<ImportEntry> getImportEntries() {
        return importEntries;
    }

    /**
     * [[LocalExportEntries]]
     * 
     * @return the list of local {@code export} entries
     */
    public List<ExportEntry> getLocalExportEntries() {
        return localExportEntries;
    }

    /**
     * [[IndirectExportEntries]]
     * 
     * @return the list of indirect {@code export} entries
     */
    public List<ExportEntry> getIndirectExportEntries() {
        return indirectExportEntries;
    }

    /**
     * [[StarExportEntries]]
     * 
     * @return the list of {@code export*} entries
     */
    public List<ExportEntry> getStarExportEntries() {
        return starExportEntries;
    }

    /**
     * Extension:<br>
     * [[NameSpaceExportEntries]]
     * 
     * @return the list of {@code export* as} entries
     */
    public List<ExportEntry> getNameSpaceExportEntries() {
        return nameSpaceExportEntries;
    }

    @Override
    public LexicalEnvironment<ModuleEnvironmentRecord> getEnvironment() {
        return environment;
    }

    @Override
    public ScriptObject getNamespace() {
        return namespace;
    }

    @Override
    public void setNamespace(ScriptObject namespace) {
        assert this.namespace == null : "namespace already created";
        this.namespace = Objects.requireNonNull(namespace);
    }

    @Override
    public ScriptObject getMeta() {
        return meta;
    }

    @Override
    public void setMeta(ScriptObject meta) {
        assert this.meta == null : "meta already created";
        this.meta = Objects.requireNonNull(meta);
    }

    /**
     * [[Status]]
     * 
     * @return the current module status
     */
    public Status getStatus() {
        return status;
    }

    /**
     * [[EvaluationError]]
     * 
     * @return the evaluation error or {@code null} if none present
     */
    public Exception getEvaluationError() {
        return evaluationError;
    }

    /**
     * 15.2.1.16.1 Runtime Semantics: ParseModule ( sourceText )
     * 
     * @param scriptLoader
     *            the script loader
     * @param sourceCodeId
     *            the source code identifier
     * @param source
     *            the module source code
     * @return the parsed module record
     * @throws IOException
     *             if there was any I/O error
     * @throws ParserException
     *             if the module source contains any syntax errors
     * @throws CompilationException
     *             if the parsed module source cannot be compiled
     */
    public static SourceTextModuleRecord ParseModule(ScriptLoader scriptLoader, SourceIdentifier sourceCodeId,
            ModuleSource source) throws IOException, ParserException, CompilationException {
        /* step 1 (not applicable) */
        /* steps 2-3 */
        com.github.anba.es6draft.ast.Module parsedBody = scriptLoader.parseModule(source.toSource(),
                source.sourceCode());
        /* steps 4-12 */
        return ParseModule(scriptLoader, sourceCodeId, parsedBody);
    }

    /**
     * 15.2.1.16.1 Runtime Semantics: ParseModule ( sourceText )
     * 
     * @param scriptLoader
     *            the script loader
     * @param sourceCodeId
     *            the source code identifier
     * @param parsedBody
     *            the parsed module source code
     * @return the parsed module record
     * @throws CompilationException
     *             if the parsed module source cannot be compiled
     */
    public static SourceTextModuleRecord ParseModule(ScriptLoader scriptLoader, SourceIdentifier sourceCodeId,
            com.github.anba.es6draft.ast.Module parsedBody) throws CompilationException {
        /* steps 1-3 (not applicable) */
        /* step 4 */
        Set<String> requestedModules = ModuleRequests(parsedBody);
        /* step 5 */
        List<ImportEntry> importEntries = ImportEntries(parsedBody);
        /* step 6 */
        Map<String, ImportEntry> importedBoundNames = ImportedLocalNames(importEntries);
        /* step 7 */
        ArrayList<ExportEntry> indirectExportEntries = new ArrayList<>();
        /* step 8 */
        ArrayList<ExportEntry> localExportEntries = new ArrayList<>();
        /* step 9 */
        ArrayList<ExportEntry> starExportEntries = new ArrayList<>();
        /* step ? (Extension: Export From) */
        ArrayList<ExportEntry> nameSpaceExportEntries = new ArrayList<>();
        /* step 10 */
        List<ExportEntry> exportEntries = ExportEntries(parsedBody);
        /* step 11 */
        for (ExportEntry exportEntry : exportEntries) {
            if (exportEntry.getModuleRequest() == null) {
                ImportEntry importEntry = importedBoundNames.get(exportEntry.getLocalName());
                if (importEntry == null || importEntry.isStarImport()) {
                    localExportEntries.add(exportEntry);
                } else {
                    indirectExportEntries
                            .add(new ExportEntry(exportEntry.getSourcePosition(), importEntry.getModuleRequest(),
                                    importEntry.getImportName(), null, exportEntry.getExportName()));
                }
            } else if (exportEntry.isStarExport()) {
                starExportEntries.add(exportEntry);
            } else if (exportEntry.isNameSpaceExport()) {
                nameSpaceExportEntries.add(exportEntry);
            } else {
                indirectExportEntries.add(exportEntry);
            }
        }
        /* step 12 */
        SourceTextModuleRecord m = new SourceTextModuleRecord(sourceCodeId, requestedModules, importEntries,
                localExportEntries, indirectExportEntries, starExportEntries, nameSpaceExportEntries);
        m.scriptCode = scriptLoader.load(parsedBody, m);
        return m;
    }

    /**
     * 15.2.1.16.2 GetExportedNames( exportStarSet ) Concrete Method
     */
    @Override
    public Set<String> getExportedNames(Set<ModuleRecord> exportStarSet)
            throws IOException, MalformedNameException, ResolutionException {
        /* step 1 */
        SourceTextModuleRecord module = this;
        /* steps 2-3 */
        if (!exportStarSet.add(module)) {
            return Collections.emptySet();
        }
        /* step 4 */
        HashSet<String> exportedNames = new HashSet<>();
        /* step 5 */
        for (ExportEntry exportEntry : module.localExportEntries) {
            /* step 5.a (not applicable) */
            /* step 5.b */
            exportedNames.add(exportEntry.getExportName());
        }
        /* step 6 */
        for (ExportEntry exportEntry : module.indirectExportEntries) {
            /* step 6.a (not applicable) */
            /* step 6.b */
            exportedNames.add(exportEntry.getExportName());
        }
        /* step 7 */
        for (ExportEntry exportEntry : module.starExportEntries) {
            /* steps 7.a-b */
            ModuleRecord requestedModule = HostResolveImportedModule(module, exportEntry.getModuleRequest());
            /* step 7.c */
            Set<String> starNames = requestedModule.getExportedNames(exportStarSet);
            /* step 7.d */
            for (String n : starNames) {
                if (!"default".equals(n)) {
                    exportedNames.add(n);
                }
            }
        }
        /* step ? (Extension: Export From) */
        for (ExportEntry exportEntry : module.nameSpaceExportEntries) {
            exportedNames.add(exportEntry.getExportName());
        }
        /* step 8 */
        return exportedNames;
    }

    /**
     * 15.2.1.16.3 ResolveExport(exportName, resolveSet) Concrete Method
     */
    @Override
    public ResolvedBinding resolveExport(String exportName, Map<ModuleRecord, Set<String>> resolveSet)
            throws IOException, MalformedNameException, ResolutionException {
        /* step 1 */
        SourceTextModuleRecord module = this;
        /* step 2 */
        Set<String> resolvedExports = resolveSet.get(module);
        if (resolvedExports == null) {
            resolveSet.put(module, resolvedExports = new HashSet<>());
        } else if (resolvedExports.contains(exportName)) {
            return null;
        }
        /* step 3 */
        resolvedExports.add(exportName);
        /* step 4 */
        for (ExportEntry exportEntry : module.localExportEntries) {
            if (exportName.equals(exportEntry.getExportName())) {
                /* step 4.a.i (not applicable) */
                /* step 4.a.ii */
                return new ResolvedBinding(module, exportEntry.getLocalName());
            }
        }
        /* step 5 */
        for (ExportEntry exportEntry : module.indirectExportEntries) {
            if (exportName.equals(exportEntry.getExportName())) {
                /* step 5.a.i (not applicable) */
                /* step 5.a.ii */
                ModuleRecord importedModule = HostResolveImportedModule(module, exportEntry.getModuleRequest());
                /* step 5.a.iii */
                return importedModule.resolveExport(exportEntry.getImportName(), resolveSet);
            }
        }
        /* step ? (Extension: Export From) */
        for (ExportEntry exportEntry : module.nameSpaceExportEntries) {
            if (exportName.equals(exportEntry.getExportName())) {
                ModuleRecord importedModule = HostResolveImportedModule(module, exportEntry.getModuleRequest());
                return new ResolvedBinding(importedModule);
            }
        }
        /* step 6 */
        if ("default".equals(exportName)) {
            return null;
        }
        /* step 7 */
        ResolvedBinding starResolution = null;
        /* step 8 */
        for (ExportEntry exportEntry : module.starExportEntries) {
            /* step 8.a */
            ModuleRecord importedModule = HostResolveImportedModule(module, exportEntry.getModuleRequest());
            /* step 8.b */
            ResolvedBinding resolution = importedModule.resolveExport(exportName, resolveSet);
            /* step 8.c */
            if (resolution == ResolvedBinding.AMBIGUOUS) {
                return ResolvedBinding.AMBIGUOUS;
            }
            /* step 8.d */
            if (resolution != null) {
                if (starResolution == null) {
                    starResolution = resolution;
                } else {
                    if (resolution.getModule() != starResolution.getModule()) {
                        return ResolvedBinding.AMBIGUOUS;
                    }
                    if (!Objects.equals(resolution.getBindingName(), starResolution.getBindingName())) {
                        return ResolvedBinding.AMBIGUOUS;
                    }
                }
            }
        }
        /* step 9 */
        return starResolution;
    }

    /**
     * 15.2.1.16.4 ModuleDeclarationInstantiation( ) Concrete Method
     */
    @Override
    public void instantiate() throws IOException, MalformedNameException, ResolutionException {
        /* step 1 */
        SourceTextModuleRecord module = this;
        /* step 2 */
        assert module.status != Status.Instantiating && module.status != Status.Evaluating;
        /* step 3 */
        ArrayDeque<SourceTextModuleRecord> stack = new ArrayDeque<>();
        /* steps 4-5 */
        try {
            InnerModuleInstantiation(module, stack, 0);
        } catch (IOException | MalformedNameException | ResolutionException | ParserException | ScriptException e) {
            /* step 5.a */
            for (SourceTextModuleRecord m : stack) {
                /* step 5.a.i */
                assert m.status == Status.Instantiating;
                /* step 5.a.ii */
                m.status = Status.Uninstantiated;
                /* step 5.a.iii */
                m.environment = null;
                /* step 5.a.iv */
                m.dfsIndex = -1;
                /* step 5.a.v */
                m.dfsAncestorIndex = -1;
            }
            /* step 5.b */
            assert module.status == Status.Uninstantiated;
            /* step 5.c */
            throw e;
        }
        /* step 6 */
        assert module.status == Status.Instantiated || module.status == Status.Evaluated;
        /* step 7 */
        assert stack.isEmpty();
        /* step 8 (return) */
    }

    /**
     * 15.2.1.16.4.1 InnerModuleInstantiation( module, stack, index )
     */
    private static int InnerModuleInstantiation(ModuleRecord module, ArrayDeque<SourceTextModuleRecord> stack,
            int index) throws IOException, MalformedNameException, ResolutionException {
        /* step 1 */
        if (!(module instanceof SourceTextModuleRecord)) {
            /* step 1.a */
            module.instantiate();
            /* step 1.b */
            return index;
        }
        SourceTextModuleRecord sourceModule = (SourceTextModuleRecord) module;
        /* steps 3-4 */
        switch (sourceModule.status) {
        case Instantiating:
        case Instantiated:
        case Evaluated:
            return index;
        case Uninstantiated:
            sourceModule.status = Status.Instantiating;
            break;
        case Evaluating:
        default:
            throw new AssertionError();
        }
        /* step 5 */
        sourceModule.dfsIndex = index;
        /* step 6 */
        sourceModule.dfsAncestorIndex = index;
        /* step 7 */
        index += 1;
        /* step 8 */
        stack.push(sourceModule);
        /* step 9 */
        for (String required : sourceModule.requestedModules) {
            /* step 9.a */
            ModuleRecord requiredModule = HostResolveImportedModule(sourceModule, required);
            /* step 9.b */
            index = InnerModuleInstantiation(requiredModule, stack, index);
            /* step 9.c */
            // FIXME: spec bug - [[Status]] only defined for Source Text Module Records.
            assert !(requiredModule instanceof SourceTextModuleRecord)
                    || ((SourceTextModuleRecord) requiredModule).status == Status.Instantiating
                    || ((SourceTextModuleRecord) requiredModule).status == Status.Instantiated
                    || ((SourceTextModuleRecord) requiredModule).status == Status.Evaluated;
            /* step 9.d */
            // FIXME: spec bug - [[Status]] only defined for Source Text Module Records.
            assert !(requiredModule instanceof SourceTextModuleRecord)
                    || ((((SourceTextModuleRecord) requiredModule).status == Status.Instantiating) == stack
                            .contains(requiredModule));
            /* step 9.e */
            if (requiredModule instanceof SourceTextModuleRecord
                    && ((SourceTextModuleRecord) requiredModule).status == Status.Instantiating) {
                /* step 9.e.i (omitted) */
                /* step 9.e.ii */
                SourceTextModuleRecord requiredSourceModule = (SourceTextModuleRecord) requiredModule;
                requiredSourceModule.dfsAncestorIndex = Math.min(sourceModule.dfsAncestorIndex,
                        requiredSourceModule.dfsAncestorIndex);
            }
        }
        /* step 10 */
        ModuleDeclarationEnvironmentSetup(sourceModule);
        /* step 11 */
        assert stack.stream().filter(m -> m == sourceModule).count() == 1;
        /* step 12 */
        assert sourceModule.dfsAncestorIndex <= sourceModule.dfsIndex;
        /* step 13 */
        if (sourceModule.dfsAncestorIndex == sourceModule.dfsIndex) {
            while (true) {
                /* steps 13.b.i-ii */
                assert !stack.isEmpty();
                SourceTextModuleRecord requiredModule = stack.pop();
                /* step 13.b.iii */
                requiredModule.status = Status.Instantiated;
                /* step 13.b.iv */
                if (requiredModule == sourceModule) {
                    break;
                }
            }
        }
        /* step 14 */
        return index;
    }

    /**
     * 15.2.1.16.4.2 ModuleDeclarationEnvironmentSetup( module )
     */
    private static void ModuleDeclarationEnvironmentSetup(SourceTextModuleRecord module)
            throws IOException, ResolutionException, MalformedNameException {
        /* step 3 */
        Realm realm = module.realm;
        /* step 4 */
        assert realm != null : "module is not linked";
        /* step 5 */
        LexicalEnvironment<ModuleEnvironmentRecord> env = newModuleEnvironment(realm.getGlobalEnv());
        /* step 6 */
        module.environment = env;
        /* steps 1-2, 7-14 (generated code) */
        Module code = module.scriptCode;
        ExecutionContext context = newModuleDeclarationExecutionContext(realm, code);
        code.getModuleBody().moduleDeclarationInstantiation(context, module, env);
    }

    /**
     * 15.2.1.16.5 ModuleEvaluation() Concrete Method
     */
    @Override
    public Object evaluate() throws IOException, MalformedNameException, ResolutionException {
        /* step 1 */
        SourceTextModuleRecord module = this;
        /* step 2 */
        assert module.status == Status.Instantiated || module.status == Status.Evaluated;
        /* step 3 */
        ArrayDeque<SourceTextModuleRecord> stack = new ArrayDeque<>();
        /* steps 4-5 */
        try {
            InnerModuleEvaluation(module, stack, 0);
        } catch (IOException | MalformedNameException | ResolutionException | ParserException | ScriptException e) {
            /* step 5.a */
            for (SourceTextModuleRecord m : stack) {
                /* step 5.a.i */
                assert m.status == Status.Evaluating;
                /* step 5.a.ii */
                m.status = Status.Evaluated;
                /* step 5.a.iii */
                m.evaluationError = e;
            }
            /* step 5.b */
            assert module.status == Status.Evaluated;
            /* step 5.c */
            throw e;
        }
        /* step 6 */
        assert module.status == Status.Evaluated && module.evaluationError == null;
        /* step 7 */
        assert stack.isEmpty();
        /* step 8 */
        return UNDEFINED;
    }

    /**
     * 15.2.1.16.5.1 InnerModuleEvaluation( module, stack, index )
     */
    private static int InnerModuleEvaluation(ModuleRecord module, ArrayDeque<SourceTextModuleRecord> stack, int index)
            throws IOException, MalformedNameException, ResolutionException {
        /* step 1 */
        if (!(module instanceof SourceTextModuleRecord)) {
            /* step 1.a */
            module.evaluate();
            /* step 1.b */
            return index;
        }
        SourceTextModuleRecord sourceModule = (SourceTextModuleRecord) module;
        /* step 2 */
        if (sourceModule.status == Status.Evaluated) {
            /* step 2.a */
            if (sourceModule.evaluationError == null) {
                return index;
            }
            /* step 2.b */
            // TODO: Create new exception and set cause to sourceModule.evaluationError for better stacktraces?
            throw SourceTextModuleRecord.<RuntimeException> rethrow(sourceModule.evaluationError);
        }
        /* step 3 */
        if (sourceModule.status == Status.Evaluating) {
            return index;
        }
        /* step 4 */
        assert sourceModule.status == Status.Instantiated;
        /* step 5 */
        sourceModule.status = Status.Evaluating;
        /* step 6 */
        sourceModule.dfsIndex = index;
        /* step 7 */
        sourceModule.dfsAncestorIndex = index;
        /* step 8 */
        index += 1;
        /* step 9 */
        stack.push(sourceModule);
        /* step 10 */
        for (String required : sourceModule.requestedModules) {
            /* steps 10.a-b */
            ModuleRecord requiredModule = HostResolveImportedModule(module, required);
            /* step 10.c */
            index = InnerModuleEvaluation(requiredModule, stack, index);
            /* step 10.d */
            // FIXME: spec bug - [[Status]] only defined for Source Text Module Records.
            assert !(requiredModule instanceof SourceTextModuleRecord)
                    || ((SourceTextModuleRecord) requiredModule).status == Status.Evaluating
                    || ((SourceTextModuleRecord) requiredModule).status == Status.Evaluated;
            /* step 10.e */
            // FIXME: spec bug - [[Status]] only defined for Source Text Module Records.
            assert !(requiredModule instanceof SourceTextModuleRecord)
                    || ((((SourceTextModuleRecord) requiredModule).status == Status.Evaluating) == stack
                            .contains(requiredModule));
            /* step 10.f */
            if (requiredModule instanceof SourceTextModuleRecord
                    && ((SourceTextModuleRecord) requiredModule).status == Status.Evaluating) {
                /* step 10.f.i (omitted) */
                /* step 10.f.ii */
                SourceTextModuleRecord requiredSourceModule = (SourceTextModuleRecord) requiredModule;
                requiredSourceModule.dfsAncestorIndex = Math.min(sourceModule.dfsAncestorIndex,
                        requiredSourceModule.dfsAncestorIndex);
            }
        }
        /* step 11 */
        ModuleExecution(sourceModule);
        /* step 12 */
        assert stack.stream().filter(m -> m == sourceModule).count() == 1;
        /* step 13 */
        assert sourceModule.dfsAncestorIndex <= sourceModule.dfsIndex;
        /* step 14 */
        if (sourceModule.dfsAncestorIndex == sourceModule.dfsIndex) {
            while (true) {
                /* steps 14.b.i-ii */
                assert !stack.isEmpty();
                SourceTextModuleRecord requiredModule = stack.pop();
                /* step 14.b.iii */
                requiredModule.status = Status.Evaluated;
                /* step 14.b.iv */
                if (requiredModule == sourceModule) {
                    break;
                }
            }
        }
        /* step 15 */
        return index;
    }

    /**
     * 15.2.1.16.5.2 ModuleExecution( module )
     */
    private static Object ModuleExecution(SourceTextModuleRecord module) {
        /* step 3 */
        Realm realm = module.realm;
        assert realm != null : "module is not linked";
        /* steps 1-2, 4-8 */
        ExecutionContext moduleContext = newModuleExecutionContext(realm, module);
        /* step 9 */
        ExecutionContext oldScriptContext = realm.getWorld().getScriptContext();
        try {
            /* step 10 */
            realm.getWorld().setScriptContext(moduleContext);
            /* step 11 */
            Object result = module.scriptCode.evaluate(moduleContext);
            /* step 14 */
            return result;
        } finally {
            /* steps 12-13 */
            realm.getWorld().setScriptContext(oldScriptContext);
        }
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> E rethrow(Throwable e) throws E {
        throw (E) e;
    }
}
