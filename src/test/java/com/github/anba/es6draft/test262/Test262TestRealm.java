/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.test262;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;

import org.junit.rules.ExternalResource;

import com.github.anba.es6draft.Script;
import com.github.anba.es6draft.compiler.CompilationException;
import com.github.anba.es6draft.parser.Parser;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.ScriptLoader;
import com.github.anba.es6draft.runtime.internal.ScriptLoading;
import com.github.anba.es6draft.runtime.internal.Source;
import com.github.anba.es6draft.runtime.modules.MalformedNameException;
import com.github.anba.es6draft.runtime.modules.ModuleLoader;
import com.github.anba.es6draft.runtime.modules.ModuleRecord;
import com.github.anba.es6draft.runtime.modules.ModuleSource;
import com.github.anba.es6draft.runtime.modules.ResolutionException;
import com.github.anba.es6draft.runtime.modules.SourceIdentifier;
import com.github.anba.es6draft.runtime.modules.loader.StringModuleSource;
import com.github.anba.es6draft.test262.Test262Info.MalformedDataException;
import com.github.anba.es6draft.util.SystemConsole;
import com.github.anba.es6draft.util.TestRealms;

/** 
 *
 */
final class Test262TestRealm extends ExternalResource {
    private final TestRealms<Test262Info> testRealms;
    private Realm realm;
    private String sourceCode;
    private Test262Async async;

    Test262TestRealm(TestRealms<Test262Info> testRealms) {
        this.testRealms = testRealms;
    }

    Realm get() {
        assert realm != null;
        return realm;
    }

    @Override
    protected void after() {
        testRealms.release(realm);
        realm = null;
        sourceCode = null;
        async = null;
    }

    @FunctionalInterface
    interface ReadFile {
        String apply(Test262Info test) throws IOException, MalformedDataException;
    }

    /**
     * Initializes this test realm resource.
     * 
     * @param test
     *            the test-info object
     * @param read
     *            the read file function
     * @param validate
     *            function to validate the test configuration
     * @return the return value from {@code validate}
     * @throws IOException
     *             if there was any I/O error
     * @throws MalformedDataException
     *             if the test file information cannot be parsed
     * @throws MalformedNameException
     *             if the module specifier cannot be normalized
     * @throws ResolutionException
     *             if the export cannot be resolved
     */
    boolean initialize(Test262Info test, ReadFile read, BooleanSupplier validate)
            throws MalformedNameException, ResolutionException, IOException, MalformedDataException {
        String fileContent = read.apply(test);
        if (!validate.getAsBoolean()) {
            return false;
        }
        sourceCode = fileContent;
        realm = testRealms.newRealm(new SystemConsole(), test);
        if (test.isAsync()) {
            async = realm.createGlobalProperties(new Test262Async(), Test262Async.class);
        }
        return true;
    }

    /**
     * Loads and evaluates the requested test harness file.
     * 
     * @param harnesses
     *            the list of harness directories
     * @param file
     *            the file name
     * @throws IOException
     *             if there was any I/O error
     */
    void include(List<Path> harnesses, String file) throws IOException, ParserException, CompilationException {
        Optional<Path> path = harnesses.stream().map(p -> p.resolve(file)).filter(Files::isRegularFile).findFirst();
        ScriptLoading.include(realm, path.orElseThrow(FileNotFoundException::new));
    }

    /**
     * Executes the supplied test object.
     * 
     * @param test
     *            the test-info object
     * @throws IOException
     *             if there was any I/O error
     * @throws MalformedNameException
     *             if any imported module request cannot be normalized
     * @throws ResolutionException
     *             if any export binding cannot be resolved
     * @throws ParserException
     *             if the module source contains any syntax errors
     * @throws CompilationException
     *             if the parsed module source cannot be compiled
     */
    void execute(Test262Info test)
            throws ParserException, CompilationException, IOException, MalformedNameException, ResolutionException {
        // Return early if no source code is available.
        if (sourceCode == null) {
            return;
        }

        assert !test.isAsync() || async != null;
        Realm realm = get();

        // Parse and evaluate the test-script
        if (test.isModule()) {
            ModuleLoader moduleLoader = realm.getModuleLoader();
            String moduleName = test.toModuleName();
            SourceIdentifier moduleId = moduleLoader.normalizeName(moduleName, null);
            ModuleSource source = new StringModuleSource(moduleId, moduleName, sourceCode);
            ModuleRecord module = moduleLoader.define(moduleId, source, realm);
            try {
                module.instantiate();
                module.evaluate();
            } catch (ResolutionException e) {
                throw e.toScriptException(realm.defaultContext());
            }
        } else {
            Source source = new Source(test.toFile(), test.getScript().toString(), 1);
            Script script = realm.getScriptLoader().script(source, sourceCode);
            script.evaluate(realm);
        }

        // Wait for pending jobs to finish
        waitForPendingJobs(realm, test);
    }

    /**
     * Executes the supplied test object.
     * 
     * @param test
     *            the test-info object
     * @throws IOException
     *             if there was any I/O error
     * @throws MalformedNameException
     *             if any imported module request cannot be normalized
     * @throws ResolutionException
     *             if any export binding cannot be resolved
     * @throws ParserException
     *             if the module source contains any syntax errors
     * @throws CompilationException
     *             if the parsed module source cannot be compiled
     */
    void executeStrict(Test262Info test)
            throws ParserException, CompilationException, IOException, MalformedNameException, ResolutionException {
        // Return early if no source code is available.
        if (sourceCode == null) {
            return;
        }

        assert !test.isAsync() || async != null;
        assert !(test.isModule() || test.isRaw());
        Realm realm = get();

        // Parse the test-script
        Source source = new Source(test.toFile(), test.getScript().toString(), 1);
        ScriptLoader scriptLoader = realm.getScriptLoader();
        EnumSet<Parser.Option> parserOptions = EnumSet.copyOf(realm.getRuntimeContext().getParserOptions());
        parserOptions.add(Parser.Option.Strict);
        Parser parser = new Parser(realm.getRuntimeContext(), source, parserOptions);
        com.github.anba.es6draft.ast.Script parsedScript = parser.parseScript(sourceCode);

        // Evaluate the test-script
        Script script = scriptLoader.load(parsedScript);
        script.evaluate(realm);

        // Wait for pending jobs to finish
        waitForPendingJobs(realm, test);
    }

    private void waitForPendingJobs(Realm realm, Test262Info test) {
        if (test.isAsync()) {
            assertFalse("$DONE not called before script execution", async.isDone());
            realm.getWorld().runEventLoop();
            assertTrue("$DONE called after script execution", async.isDone());
        } else {
            realm.getWorld().runEventLoop();
        }
    }
}
