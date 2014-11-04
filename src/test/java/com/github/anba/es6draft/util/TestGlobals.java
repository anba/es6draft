/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.util;

import static com.github.anba.es6draft.runtime.modules.ModuleSemantics.LinkModules;
import static com.github.anba.es6draft.runtime.modules.ModuleSemantics.ModuleEvaluation;
import static com.github.anba.es6draft.runtime.modules.ModuleSemantics.NormalizeModuleName;
import static com.github.anba.es6draft.runtime.modules.ModuleSemantics.ParseModuleAndImports;
import static com.github.anba.es6draft.util.Functional.toStrings;
import static java.util.Collections.emptyList;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.configuration.Configuration;
import org.junit.rules.ExternalResource;

import com.github.anba.es6draft.Script;
import com.github.anba.es6draft.compiler.Compiler;
import com.github.anba.es6draft.parser.Parser;
import com.github.anba.es6draft.repl.console.ShellConsole;
import com.github.anba.es6draft.repl.global.ShellGlobalObject;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.ShadowRealm;
import com.github.anba.es6draft.runtime.World;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.FileModuleLoader;
import com.github.anba.es6draft.runtime.internal.ModuleLoader;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.internal.ScriptCache;
import com.github.anba.es6draft.runtime.internal.ScriptLoader;
import com.github.anba.es6draft.runtime.internal.Source;
import com.github.anba.es6draft.runtime.modules.ModuleRecord;
import com.github.anba.es6draft.runtime.objects.GlobalObject;

/**
 * {@link ExternalResource} sub-class to facilitate creation of {@link GlobalObject} instances
 */
public abstract class TestGlobals<GLOBAL extends ShellGlobalObject, TEST extends TestInfo> extends
        ExternalResource {
    private final Configuration configuration;
    private Set<CompatibilityOption> options;
    private ScriptCache scriptCache;
    private List<Script> scripts;
    private List<PreloadedModule> modules;

    protected TestGlobals(Configuration configuration) {
        this.configuration = configuration;
    }

    protected Set<CompatibilityOption> getOptions() {
        return options;
    }

    protected Set<Parser.Option> getParserOptions() {
        return EnumSet.noneOf(Parser.Option.class);
    }

    protected Set<Compiler.Option> getCompilerOptions() {
        return EnumSet.noneOf(Compiler.Option.class);
    }

    protected Path getBaseDirectory() {
        return Resources.getTestSuitePath(configuration);
    }

    protected ScriptLoader createScriptLoader() {
        return new ScriptLoader(getOptions(), getParserOptions(), getCompilerOptions());
    }

    protected ModuleLoader createModuleLoader(Path baseDirectory) {
        return new FileModuleLoader(baseDirectory);
    }

    @Override
    protected void before() throws Throwable {
        if (!Resources.isEnabled(configuration)) {
            // skip initialization if test suite not enabled
            return;
        }

        // read options ...
        options = compatibilityOptions(configuration.getString("mode", ""));
        scriptCache = new ScriptCache();

        // pre-compile initialization scripts and modules
        scripts = compileScripts();
        modules = compileModules();
    }

    public final GLOBAL newGlobal(ShellConsole console, TEST test) throws IOException,
            URISyntaxException {
        ObjectAllocator<GLOBAL> allocator = newAllocator(console, test, scriptCache);
        World<GLOBAL> world = new World<>(allocator, createModuleLoader(getBaseDirectory()),
                createScriptLoader());
        GLOBAL global = world.newInitializedGlobal();
        // Evaluate additional initialization scripts and modules
        for (PreloadedModule preloadModule : modules) {
            Realm realm = global.getRealm();
            String moduleName = preloadModule.getModuleName();
            Map<String, ModuleRecord> newModuleSet = preloadModule.copyRequires();
            assert newModuleSet.containsKey(moduleName);
            ModuleRecord module = newModuleSet.get(moduleName);
            LinkModules(realm.defaultContext(), realm, moduleName, newModuleSet);
            ModuleEvaluation(module, realm);
        }
        for (Script script : scripts) {
            global.eval(script);
        }
        return global;
    }

    protected abstract ObjectAllocator<GLOBAL> newAllocator(ShellConsole console, TEST test,
            ScriptCache scriptCache);

    private static Set<CompatibilityOption> compatibilityOptions(String mode) {
        switch (mode) {
        case "moz-compatibility":
            return CompatibilityOption.MozCompatibility();
        case "web-compatibility":
            return CompatibilityOption.WebCompatibility();
        case "strict-compatibility":
        default:
            return CompatibilityOption.StrictCompatibility();
        }
    }

    private List<Script> compileScripts() throws IOException {
        List<?> scriptNames = configuration.getList("scripts", emptyList());
        if (scriptNames.isEmpty()) {
            return Collections.emptyList();
        }
        Path basedir = getBaseDirectory();
        ScriptLoader scriptLoader = createScriptLoader();
        ArrayList<Script> scripts = new ArrayList<>();
        for (String scriptName : toStrings(scriptNames)) {
            Script script = scriptLoader.script(
                    new Source(Resources.resourcePath(scriptName, basedir), scriptName, 1),
                    Resources.resource(scriptName, basedir));
            scripts.add(script);
        }
        return scripts;
    }

    private List<PreloadedModule> compileModules() throws IOException {
        List<?> moduleNames = configuration.getList("modules", emptyList());
        if (moduleNames.isEmpty()) {
            return Collections.emptyList();
        }
        ModuleLoader moduleLoader = createModuleLoader(getBaseDirectory());
        ScriptLoader scriptLoader = createScriptLoader();
        PreloaderRealm realm = new PreloaderRealm(moduleLoader, scriptLoader);
        ArrayList<PreloadedModule> modules = new ArrayList<>();
        for (String moduleName : toStrings(moduleNames)) {
            String normalizedModuleName = NormalizeModuleName(realm, "", moduleName);
            if (!realm.getModules().containsKey(normalizedModuleName)) {
                HashMap<String, ModuleRecord> newModules = new HashMap<>();
                ModuleRecord module = ParseModuleAndImports(realm, normalizedModuleName, newModules);
                realm.getModules().putAll(newModules);
                modules.add(new PreloadedModule(module, newModules));
            }
        }
        return modules;
    }

    private static final class PreloadedModule {
        private final ModuleRecord module;
        private final Map<String, ModuleRecord> requires;

        PreloadedModule(ModuleRecord module, Map<String, ModuleRecord> requires) {
            this.module = module;
            this.requires = requires;
        }

        public String getModuleName() {
            return module.getName();
        }

        public Map<String, ModuleRecord> copyRequires() {
            HashMap<String, ModuleRecord> requiresCopy = new HashMap<>(requires);
            for (Map.Entry<String, ModuleRecord> entry : requiresCopy.entrySet()) {
                entry.setValue(entry.getValue().clone());
            }
            return requiresCopy;
        }
    }

    private static final class PreloaderRealm implements ShadowRealm {
        private final ModuleLoader moduleLoader;
        private final ScriptLoader scriptLoader;
        private final HashMap<String, ModuleRecord> modules = new HashMap<>();
        private final HashMap<String, String> nameMap = new HashMap<>();

        PreloaderRealm(ModuleLoader moduleLoader, ScriptLoader scriptLoader) {
            this.moduleLoader = moduleLoader;
            this.scriptLoader = scriptLoader;
        }

        @Override
        public ModuleLoader getModuleLoader() {
            return moduleLoader;
        }

        @Override
        public ScriptLoader getScriptLoader() {
            return scriptLoader;
        }

        @Override
        public Map<String, ModuleRecord> getModules() {
            return modules;
        }

        @Override
        public Map<String, String> getNameMap() {
            return nameMap;
        }
    }
}
