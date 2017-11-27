/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.language;

import static com.github.anba.es6draft.runtime.AbstractOperations.ToFlatString;
import static com.github.anba.es6draft.runtime.internal.Errors.newInternalError;
import static com.github.anba.es6draft.runtime.internal.Errors.newReferenceError;
import static com.github.anba.es6draft.runtime.modules.ModuleSemantics.GetModuleNamespace;
import static com.github.anba.es6draft.runtime.modules.ModuleSemantics.HostResolveImportedModule;
import static com.github.anba.es6draft.runtime.objects.promise.PromiseAbstractOperations.PromiseBuiltinCapability;
import static com.github.anba.es6draft.runtime.objects.promise.PromiseCapability.IfAbruptRejectPromise;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject.ObjectCreate;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.CompletionException;

import com.github.anba.es6draft.Executable;
import com.github.anba.es6draft.Module;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.ModuleEnvironmentRecord;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.InternalThrowable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.internal.Source;
import com.github.anba.es6draft.runtime.modules.MalformedNameException;
import com.github.anba.es6draft.runtime.modules.ModuleLoader;
import com.github.anba.es6draft.runtime.modules.ModuleRecord;
import com.github.anba.es6draft.runtime.modules.ResolutionException;
import com.github.anba.es6draft.runtime.modules.ResolvedBinding;
import com.github.anba.es6draft.runtime.modules.SourceIdentifier;
import com.github.anba.es6draft.runtime.modules.SourceTextModuleRecord;
import com.github.anba.es6draft.runtime.objects.promise.PromiseCapability;
import com.github.anba.es6draft.runtime.objects.promise.PromiseObject;
import com.github.anba.es6draft.runtime.types.ScriptObject;

/**
 * 
 */
public final class ModuleOperations {
    private ModuleOperations() {
    }

    /**
     * 15.2.1.16.4.2 ModuleDeclarationEnvironmentSetup( module )
     * 
     * @param module
     *            the module record
     * @param exportName
     *            the export name
     * @throws IOException
     *             if there was any I/O error
     * @throws MalformedNameException
     *             if the module specifier cannot be normalized
     * @throws ResolutionException
     *             if the export cannot be resolved
     */
    public static void resolveExportOrThrow(SourceTextModuleRecord module, String exportName)
            throws IOException, MalformedNameException, ResolutionException {
        /* step 1.a */
        ResolvedBinding resolution = module.resolveExport(exportName, new HashMap<>());
        /* step 1.b */
        if (resolution == null) {
            throw new ResolutionException(Messages.Key.ModulesUnresolvedExport, exportName);
        }
        if (resolution.isAmbiguous()) {
            throw new ResolutionException(Messages.Key.ModulesAmbiguousExport, exportName);
        }
        /* step 1.c (implicit) */
    }

    /**
     * 15.2.1.16.4.2 ModuleDeclarationEnvironmentSetup( module )
     * 
     * @param module
     *            the module record
     * @param moduleRequest
     *            the module specifier string
     * @param importName
     *            the import name
     * @return the resolved module import
     * @throws IOException
     *             if there was any I/O error
     * @throws MalformedNameException
     *             if the module specifier cannot be normalized
     * @throws ResolutionException
     *             if the export cannot be resolved
     */
    public static ResolvedBinding resolveImportOrThrow(SourceTextModuleRecord module, String moduleRequest,
            String importName) throws IOException, MalformedNameException, ResolutionException {
        /* steps 8.a-b */
        ModuleRecord importedModule = HostResolveImportedModule(module, moduleRequest);
        /* step 8.c (not applicable) */
        /* step 8.d.i */
        ResolvedBinding resolution = importedModule.resolveExport(importName, new HashMap<>());
        /* step 8.d.ii */
        if (resolution == null) {
            throw new ResolutionException(Messages.Key.ModulesUnresolvedImport, importName,
                    importedModule.getSourceCodeId().toString());
        }
        if (resolution.isAmbiguous()) {
            throw new ResolutionException(Messages.Key.ModulesAmbiguousImport, importName,
                    importedModule.getSourceCodeId().toString());
        }
        return resolution;
    }

    /**
     * 15.2.1.16.4.2 ModuleDeclarationEnvironmentSetup( module )
     * 
     * @param cx
     *            the execution context
     * @param module
     *            the module record
     * @param moduleRequest
     *            the module specifier string
     * @return the module namespace object
     * @throws IOException
     *             if there was any I/O error
     * @throws MalformedNameException
     *             if the module specifier cannot be normalized
     * @throws ResolutionException
     *             if the export cannot be resolved
     */
    public static ScriptObject getModuleNamespace(ExecutionContext cx, SourceTextModuleRecord module,
            String moduleRequest) throws IOException, MalformedNameException, ResolutionException {
        /* steps 8.a-b */
        ModuleRecord importedModule = HostResolveImportedModule(module, moduleRequest);
        /* step 8.c.i */
        return GetModuleNamespace(cx, importedModule);
    }

    /**
     * 15.2.1.16.4 ModuleDeclarationInstantiation( ) Concrete Method
     * 
     * @param cx
     *            the execution context
     * @param envRec
     *            the module environment record
     * @param name
     *            the import name
     * @param resolved
     *            the resolved module export
     * @throws IOException
     *             if there was any I/O error
     * @throws MalformedNameException
     *             if the module specifier cannot be normalized
     * @throws ResolutionException
     *             if the export cannot be resolved
     */
    public static void createImportBinding(ExecutionContext cx, ModuleEnvironmentRecord envRec, String name,
            ResolvedBinding resolved) throws IOException, MalformedNameException, ResolutionException {
        assert !resolved.isAmbiguous();
        if (resolved.isNameSpaceExport()) {
            envRec.createImmutableBinding(name, true);
            envRec.initializeBinding(name, GetModuleNamespace(cx, resolved.getModule()));
        } else {
            envRec.createImportBinding(name, resolved.getModule(), resolved.getBindingName());
        }
    }

    public static PromiseObject dynamicImport(Object value, ExecutionContext cx) {
        PromiseCapability<PromiseObject> promiseCapability = PromiseBuiltinCapability(cx);
        String specifier;
        try {
            specifier = ToFlatString(cx, value);
        } catch (ScriptException e) {
            return IfAbruptRejectPromise(cx, e, promiseCapability);
        }
        HostImportModuleDynamically(cx, specifier, promiseCapability);
        return promiseCapability.getPromise();
    }

    /**
     * Runtime Semantics: HostImportModuleDynamically ( referencingScriptOrModule, specifier, promiseCapability )
     * 
     * @param cx
     *            the execution context
     * @param specifier
     *            the module specifier
     * @param promiseCapability
     *            the promise capability
     */
    private static void HostImportModuleDynamically(ExecutionContext cx, String specifier,
            PromiseCapability<PromiseObject> promiseCapability) {
        Source source = cx.getCurrentExecutable().getSource();
        assert source != null : "HostImportModuleDynamically is only called from compiled code";

        // FIXME: spec bug - cannot assert referencingScriptOrModule is non-null, e.g. consider:
        // Promise.resolve(`import("./t.js").catch(e => print("caught", e))`).then(eval);
        // Or:
        // Promise.resolve(`import("./t.js").catch(e => print("caught",
        // e))`).then(Function).then(Function.prototype.call.bind(Function.prototype.call));
        SourceIdentifier referredId = source.getSourceId();
        if (referredId == null) {
            ScriptException exception = newReferenceError(cx, Messages.Key.ModulesInvalidName, specifier);
            FinishDynamicImport(cx, specifier, promiseCapability, exception);
            return;
        }

        Realm realm = cx.getRealm();
        ModuleLoader moduleLoader = realm.getModuleLoader();
        SourceIdentifier moduleId;
        try {
            moduleId = moduleLoader.normalizeName(specifier, referredId);
        } catch (MalformedNameException e) {
            FinishDynamicImport(cx, specifier, promiseCapability, e.toScriptException(cx));
            return;
        }

        moduleLoader.resolveAsync(moduleId, realm).whenComplete((module, err) -> {
            realm.enqueueAsyncJob(() -> {
                Throwable error = err;
                if (module != null) {
                    try {
                        module.instantiate();
                        module.evaluate();
                    } catch (ScriptException | IOException | MalformedNameException | ResolutionException e) {
                        assert error == null;
                        error = e;
                    }
                }

                if (error != null) {
                    ScriptException exception = toScriptException(cx, error, moduleId, referredId);
                    FinishDynamicImport(cx, specifier, promiseCapability, exception);
                } else {
                    FinishDynamicImport(cx, specifier, promiseCapability, module, referredId);
                }
            });
        });
    }

    /**
     * Runtime Semantics: FinishDynamicImport ( referencingScriptOrModule, specifier, promiseCapability, completion )
     */
    private static void FinishDynamicImport(ExecutionContext cx, String specifier,
            PromiseCapability<PromiseObject> promiseCapability, ScriptException exception) {
        promiseCapability.getReject().call(cx, UNDEFINED, exception.getValue());
    }

    /**
     * Runtime Semantics: FinishDynamicImport ( referencingScriptOrModule, specifier, promiseCapability, completion )
     */
    private static void FinishDynamicImport(ExecutionContext cx, String specifier,
            PromiseCapability<PromiseObject> promiseCapability, ModuleRecord module, SourceIdentifier referredId) {
        ScriptObject namespace;
        try {
            // FIXME: spec issue - module namespace with "then" property vs. promise thenables
            namespace = GetModuleNamespace(cx, module);
        } catch (ScriptException | IOException | MalformedNameException | ResolutionException e) {
            ScriptException exception = toScriptException(cx, e, module.getSourceCodeId(), referredId);
            promiseCapability.getReject().call(cx, UNDEFINED, exception.getValue());
            return;
        }

        promiseCapability.getResolve().call(cx, UNDEFINED, namespace);
    }

    private static ScriptException toScriptException(ExecutionContext cx, Throwable e, SourceIdentifier moduleId,
            SourceIdentifier referredId) {
        if (e instanceof CompletionException && e.getCause() != null) {
            e = e.getCause();
        }
        ScriptException exception;
        if (e instanceof NoSuchFileException) {
            exception = new ResolutionException(Messages.Key.ModulesUnresolvedModule, moduleId.toString(),
                    referredId.toString()).toScriptException(cx);
        } else if (e instanceof IOException) {
            exception = newInternalError(cx, e, Messages.Key.ModulesIOException, Objects.toString(e.getMessage(), ""));
        } else if (e instanceof InternalThrowable) {
            exception = ((InternalThrowable) e).toScriptException(cx);
        } else {
            cx.getRuntimeContext().getErrorReporter().accept(cx, e);
            exception = newInternalError(cx, e, Messages.Key.InternalError, Objects.toString(e.getMessage(), ""));
        }
        return exception;
    }

    public static ScriptObject importMeta(ExecutionContext cx) {
        /* steps 1-2 */
        Executable executable = cx.getActiveScriptOrModule();
        assert executable instanceof Module;
        SourceIdentifier moduleId = executable.getSource().getSourceId();
        Realm realm = cx.getRealm();
        ModuleRecord moduleRecord = realm.getModuleLoader().get(moduleId, realm);
        /* step 3 */
        ScriptObject importMeta = moduleRecord.getMeta();
        /* step 4 */
        if (importMeta == null) {
            /* step 4.a */
            importMeta = ObjectCreate(cx, (ScriptObject) null);
            /* steps 4.b-e */
            cx.getRuntimeContext().getImportMeta().accept(importMeta, moduleRecord);
            /* step 4.f */
            moduleRecord.setMeta(importMeta);
            /* step 4.g */
            return importMeta;
        }
        /* step 5 */
        return importMeta;
    }
}
