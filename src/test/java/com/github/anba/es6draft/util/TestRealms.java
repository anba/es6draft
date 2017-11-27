/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.util;

import static java.util.Collections.emptyList;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.commons.configuration.Configuration;
import org.junit.rules.ExternalResource;

import com.github.anba.es6draft.Script;
import com.github.anba.es6draft.compiler.Compiler;
import com.github.anba.es6draft.parser.Parser;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.RealmData;
import com.github.anba.es6draft.runtime.World;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.Console;
import com.github.anba.es6draft.runtime.internal.RuntimeContext;
import com.github.anba.es6draft.runtime.internal.ScriptCache;
import com.github.anba.es6draft.runtime.internal.ScriptLoader;
import com.github.anba.es6draft.runtime.internal.Source;
import com.github.anba.es6draft.runtime.modules.MalformedNameException;
import com.github.anba.es6draft.runtime.modules.ModuleRecord;
import com.github.anba.es6draft.runtime.modules.ModuleSource;
import com.github.anba.es6draft.runtime.modules.ResolutionException;
import com.github.anba.es6draft.runtime.modules.SourceIdentifier;
import com.github.anba.es6draft.runtime.modules.loader.FileSourceIdentifier;
import com.github.anba.es6draft.runtime.modules.loader.URLSourceIdentifier;
import com.github.anba.es6draft.runtime.types.ScriptObject;

/**
 * {@link ExternalResource} sub-class to facilitate creation of {@link RealmData} instances
 */
public class TestRealms<TEST extends TestInfo> extends ExternalResource {
    private static final String DEFAULT_MODE = "web-compatibility";
    private static final String DEFAULT_VERSION = CompatibilityOption.Version.ECMAScript2018.name();
    private static final String DEFAULT_STAGE = CompatibilityOption.Stage.Finished.name();
    private static final List<String> DEFAULT_FEATURES = emptyList();

    private final Configuration configuration;
    private final Function<Realm, ? extends RealmData> realmData;
    private EnumSet<CompatibilityOption> options;
    private ScriptCache scriptCache;
    private List<Script> scripts;
    private PreloadModules modules;

    public TestRealms(Configuration configuration, Function<Realm, ? extends RealmData> realmData) {
        this.configuration = configuration;
        this.realmData = realmData;
    }

    protected EnumSet<CompatibilityOption> getOptions() {
        return EnumSet.copyOf(options);
    }

    protected EnumSet<Parser.Option> getParserOptions() {
        return EnumSet.noneOf(Parser.Option.class);
    }

    protected EnumSet<Compiler.Option> getCompilerOptions() {
        return EnumSet.noneOf(Compiler.Option.class);
    }

    protected Path getBaseDirectory() {
        return Resources.getTestSuitePath(configuration);
    }

    protected BiFunction<RuntimeContext, ScriptLoader, TestModuleLoader<?>> getModuleLoader() {
        return TestFileModuleLoader::new;
    }

    protected Supplier<? extends RuntimeContext.Data> getRuntimeData() {
        return RuntimeContext.Data::new;
    }

    private RuntimeContext createContext() {
        /* @formatter:off */
        return new RuntimeContext.Builder()
                                 .setBaseDirectory(getBaseDirectory())
                                 .setOptions(getOptions())
                                 .setParserOptions(getParserOptions())
                                 .setCompilerOptions(getCompilerOptions())
                                 .setScriptCache(scriptCache)
                                 .setErrorReporter(this::errorReporter)
                                 .setWorkerErrorReporter(this::workerErrorReporter)
                                 .setNativeCallResolver(getNativeCallResolver())
                                 .setImportMeta(getImportMeta())
                                 .build();
        /* @formatter:on */
    }

    protected RuntimeContext createContext(Console console, TEST test) {
        /* @formatter:off */
        return new RuntimeContext.Builder()
                                 .setLocale(getLocale(test))
                                 .setTimeZone(getTimeZone(test))
                                 .setRealmData(realmData)
                                 .setRuntimeData(getRuntimeData())
                                 .setModuleLoader(getModuleLoader())
                                 .setBaseDirectory(test.getBaseDir())
                                 .setConsole(console)
                                 .setOptions(getOptions())
                                 .setParserOptions(getParserOptions())
                                 .setCompilerOptions(getCompilerOptions())
                                 .setScriptCache(scriptCache)
                                 .setErrorReporter(this::errorReporter)
                                 .setWorkerErrorReporter(this::workerErrorReporter)
                                 .setNativeCallResolver(getNativeCallResolver())
                                 .setImportMeta(getImportMeta())
                                 .build();
        /* @formatter:on */
    }

    protected Locale getLocale(TEST test) {
        return Locale.getDefault();
    }

    protected TimeZone getTimeZone(TEST test) {
        return TimeZone.getDefault();
    }

    protected void errorReporter(ExecutionContext cx, Throwable t) {
        t.printStackTrace();
    }

    protected void workerErrorReporter(ExecutionContext cx, Throwable t) {
        t.printStackTrace();
    }

    protected BiFunction<String, MethodType, MethodHandle> getNativeCallResolver() {
        return (name, type) -> null;
    }

    protected BiConsumer<ScriptObject, ModuleRecord> getImportMeta() {
        return (meta, module) -> {
            // empty
        };
    }

    @Override
    protected void before() throws Throwable {
        if (!Resources.isEnabled(configuration)) {
            // skip initialization if test suite not enabled
            return;
        }
        scriptCache = new ScriptCache();

        // read options ...
        EnumSet<CompatibilityOption> compatibilityOptions = EnumSet.noneOf(CompatibilityOption.class);
        optionsFromMode(compatibilityOptions, configuration.getString("mode", DEFAULT_MODE));
        optionsFromVersion(compatibilityOptions, configuration.getString("version", DEFAULT_VERSION));
        optionsFromStage(compatibilityOptions, configuration.getString("stage", DEFAULT_STAGE));
        optionsFromFeatures(compatibilityOptions, Resources.list(configuration, "features", DEFAULT_FEATURES));
        options = compatibilityOptions;

        // pre-compile initialization scripts and modules
        scripts = compileScripts();
        modules = compileModules();
    }

    public final RuntimeContext newContext(Console console, TEST test) {
        return createContext(console, test);
    }

    public final Realm newRealm(Console console, TEST test)
            throws MalformedNameException, ResolutionException, IOException {
        RuntimeContext context = createContext(console, test);
        World world = new World(context);
        Realm realm = Realm.InitializeHostDefinedRealm(world);

        // Evaluate additional initialization scripts and modules
        TestModuleLoader<?> moduleLoader = (TestModuleLoader<?>) world.getModuleLoader();
        for (ModuleRecord module : modules.allModules) {
            moduleLoader.defineFromTemplate(module, realm);
        }
        for (ModuleRecord module : modules.mainModules) {
            ModuleRecord testModule = moduleLoader.get(module.getSourceCodeId(), realm);
            testModule.instantiate();
            testModule.evaluate();
        }
        for (Script script : scripts) {
            script.evaluate(realm);
        }

        return realm;
    }

    public void release(Realm realm) {
        if (realm != null) {
            realm.getRuntimeContext().getExecutor().shutdown();
            realm.getRuntimeContext().getWorkerExecutor().shutdown();
        }
    }

    private static void optionsFromMode(EnumSet<CompatibilityOption> options, String mode) {
        switch (mode) {
        case "moz-compatibility":
            options.addAll(CompatibilityOption.MozCompatibility());
            break;
        case "web-compatibility":
            options.addAll(CompatibilityOption.WebCompatibility());
            break;
        case "strict-compatibility":
            options.addAll(CompatibilityOption.StrictCompatibility());
            break;
        default:
            throw new IllegalArgumentException(String.format("Unsupported mode: '%s'", mode));
        }
    }

    private static void optionsFromVersion(EnumSet<CompatibilityOption> options, String version) {
        options.addAll(Arrays.stream(CompatibilityOption.Version.values()).filter(v -> {
            return v.name().equalsIgnoreCase(version);
        }).findAny().map(CompatibilityOption::Version).orElseThrow(IllegalArgumentException::new));
    }

    private static void optionsFromStage(EnumSet<CompatibilityOption> options, String stage) {
        options.addAll(Arrays.stream(CompatibilityOption.Stage.values()).filter(s -> {
            if (stage.length() == 1 && Character.isDigit(stage.charAt(0))) {
                return s.getLevel() == Character.digit(stage.charAt(0), 10);
            } else {
                return s.name().equalsIgnoreCase(stage);
            }
        }).findAny().map(CompatibilityOption::Stage).orElseThrow(IllegalArgumentException::new));
    }

    private static void optionsFromFeatures(EnumSet<CompatibilityOption> options, List<String> features) {
        features.stream().map(TestRealms::optionFromFeature).forEach(options::add);
    }

    private static CompatibilityOption optionFromFeature(String feature) {
        return Arrays.stream(CompatibilityOption.values()).filter(o -> {
            return o.name().equalsIgnoreCase(feature);
        }).findAny().orElseThrow(IllegalArgumentException::new);
    }

    private List<Script> compileScripts() throws IOException {
        List<String> scriptNames = Resources.list(configuration, "scripts", emptyList());
        if (scriptNames.isEmpty()) {
            return Collections.emptyList();
        }
        Path basedir = getBaseDirectory();
        RuntimeContext context = createContext();
        ScriptLoader scriptLoader = new ScriptLoader(context);
        ArrayList<Script> scripts = new ArrayList<>();
        for (String scriptName : scriptNames) {
            Map.Entry<Either<Path, URL>, InputStream> resourceScript = Resources.resourceScript(scriptName, basedir);
            Source source = resourceScript.getKey().map(path -> new Source(path, scriptName, 1),
                    url -> new Source(new URLSourceIdentifier(url), scriptName, 1));
            Script script = scriptLoader.script(source, resourceScript.getValue());
            scripts.add(script);
        }
        return scripts;
    }

    private PreloadModules compileModules() throws IOException, MalformedNameException {
        List<String> moduleNames = Resources.list(configuration, "modules", emptyList());
        if (moduleNames.isEmpty()) {
            return new PreloadModules(Collections.<ModuleRecord> emptyList(), Collections.<ModuleRecord> emptyList());
        }
        Path basedir = getBaseDirectory();
        RuntimeContext context = createContext();
        ScriptLoader scriptLoader = new ScriptLoader(context);
        TestModuleLoader<?> moduleLoader = getModuleLoader().apply(context, scriptLoader);
        ArrayList<ModuleRecord> modules = new ArrayList<>();
        for (String moduleName : moduleNames) {
            Map.Entry<Path, String> resourceModule = Resources.resourceModule(moduleName);
            ModuleRecord module;
            if (resourceModule == null) {
                SourceIdentifier moduleId = moduleLoader.normalizeName(moduleName, null);
                module = moduleLoader.load(moduleId);
            } else {
                Path modulePath = basedir.resolve(resourceModule.getKey());
                String sourceCode = resourceModule.getValue();
                ResourceModuleSource moduleSource = new ResourceModuleSource(modulePath, sourceCode);
                FileSourceIdentifier sourceId = new FileSourceIdentifier(modulePath);
                module = moduleLoader.defineUnlinked(sourceId, moduleSource);
            }
            modules.add(module);
        }
        return new PreloadModules(modules, moduleLoader.getModules());
    }

    private static final class ResourceModuleSource implements ModuleSource {
        private final Path modulePath;
        private final String sourceCode;

        ResourceModuleSource(Path modulePath, String sourceCode) {
            this.modulePath = modulePath;
            this.sourceCode = sourceCode;
        }

        @Override
        public String sourceCode() throws IOException {
            return sourceCode;
        }

        @Override
        public Source toSource() {
            return new Source(modulePath, modulePath.getFileName().toString(), 1);
        }
    }

    private static final class PreloadModules {
        private final List<ModuleRecord> mainModules;
        private final Collection<ModuleRecord> allModules;

        PreloadModules(List<ModuleRecord> modules, Collection<? extends ModuleRecord> requires) {
            assert modules.size() <= requires.size();
            this.mainModules = Collections.unmodifiableList(modules);
            this.allModules = Collections.<ModuleRecord> unmodifiableCollection(requires);
        }
    }
}
