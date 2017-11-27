/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.repl.loader;

import static com.github.anba.es6draft.runtime.AbstractOperations.Construct;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;

import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Errors;
import com.github.anba.es6draft.runtime.internal.RuntimeContext;
import com.github.anba.es6draft.runtime.internal.ScriptLoader;
import com.github.anba.es6draft.runtime.internal.ScriptLoading;
import com.github.anba.es6draft.runtime.modules.MalformedNameException;
import com.github.anba.es6draft.runtime.modules.ModuleRecord;
import com.github.anba.es6draft.runtime.modules.ModuleSource;
import com.github.anba.es6draft.runtime.modules.ResolutionException;
import com.github.anba.es6draft.runtime.modules.SourceIdentifier;
import com.github.anba.es6draft.runtime.modules.loader.AbstractFileModuleLoader;
import com.github.anba.es6draft.runtime.modules.loader.FileSourceIdentifier;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.ScriptObject;

/**
 *
 */
public class NodeModuleLoader extends AbstractFileModuleLoader<ModuleRecord> {
    private final HashMap<SourceIdentifier, ModuleRecord> modules = new HashMap<>();
    private final WeakHashMap<Realm, Constructor> moduleConstructors = new WeakHashMap<>();
    private final ScriptLoader scriptLoader;

    public NodeModuleLoader(RuntimeContext context, ScriptLoader scriptLoader) {
        super(context);
        this.scriptLoader = scriptLoader;
    }

    private ScriptObject createModuleObject(NodeModuleRecord module, Realm realm) {
        Constructor moduleConstructor = moduleConstructors.computeIfAbsent(realm, r -> {
            try {
                ModuleRecord mod = ScriptLoading.evalNativeModule(r, "module.jsm");
                return (Constructor) ScriptLoading.getModuleExport(mod, "default");
            } catch (MalformedNameException | ResolutionException e) {
                throw e.toScriptException(r.defaultContext());
            } catch (IOException e) {
                throw Errors.newError(r.defaultContext(), e.getMessage());
            }
        });
        Path file = module.getSource().getFile();
        String fileName = Objects.toString(file, "");
        String dirName = file != null ? Objects.toString(file.getParent(), "") : "";
        return Construct(realm.defaultContext(), moduleConstructor, fileName, dirName);
    }

    @Override
    protected void defineModule(ModuleRecord module) {
        SourceIdentifier identifier = module.getSourceCodeId();
        if (modules.containsKey(identifier)) {
            throw new IllegalArgumentException();
        }
        modules.put(identifier, module);
    }

    @Override
    protected ModuleRecord getModule(SourceIdentifier identifier) {
        return modules.get(identifier);
    }

    @Override
    protected ModuleRecord parseModule(SourceIdentifier identifier, ModuleSource source) throws IOException {
        try {
            return NodeModuleRecord.ParseModule(scriptLoader, identifier, source);
        } catch (ParserException e) {
            // ignore
        }
        return NodeSourceTextModuleRecord.ParseModule(scriptLoader, identifier, source);
    }

    @Override
    protected void linkModule(ModuleRecord module, Realm realm) {
        if (module.getRealm() == null) {
            if (module instanceof NodeSourceTextModuleRecord) {
                ((NodeSourceTextModuleRecord) module).setRealm(realm);
            } else if (module instanceof NodeModuleRecord) {
                ScriptObject moduleObject = createModuleObject((NodeModuleRecord) module, realm);
                ((NodeModuleRecord) module).setRealm(realm, moduleObject);
            }
        }
    }

    @Override
    protected Set<String> getRequestedModules(ModuleRecord module) {
        if (module instanceof NodeSourceTextModuleRecord) {
            return ((NodeSourceTextModuleRecord) module).getRequestedModules();
        }
        return Collections.emptySet();
    }

    @Override
    protected Collection<ModuleRecord> getModules() {
        return Collections.unmodifiableCollection(modules.values());
    }

    @Override
    public FileSourceIdentifier normalizeName(String unnormalizedName, SourceIdentifier referrerId)
            throws MalformedNameException {
        FileSourceIdentifier normalizedName = super.normalizeName(unnormalizedName, referrerId);
        return NodeModuleResolution.resolve(getBaseDirectory(), normalizedName, unnormalizedName, referrerId);
    }
}
