/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.util;

import java.io.IOException;

import org.junit.rules.ExternalResource;

import com.github.anba.es6draft.compiler.CompilationException;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Console;
import com.github.anba.es6draft.runtime.internal.JobSource;
import com.github.anba.es6draft.runtime.internal.ScriptLoading;
import com.github.anba.es6draft.runtime.modules.MalformedNameException;
import com.github.anba.es6draft.runtime.modules.ResolutionException;

/** 
 *
 */
public final class TestRealm<TEST extends TestInfo> extends ExternalResource {
    private final TestRealms<TEST> testRealms;
    private Realm realm;

    public TestRealm(TestRealms<TEST> testRealms) {
        this.testRealms = testRealms;
    }

    public Realm get() {
        assert realm != null;
        return realm;
    }

    @Override
    protected void after() {
        testRealms.release(realm);
        realm = null;
    }

    /**
     * Initializes this test realm resource.
     * 
     * @param console
     *            the console object
     * @param test
     *            the test-info object
     * @throws IOException
     *             if there was any I/O error
     * @throws MalformedNameException
     *             if the module specifier cannot be normalized
     * @throws ResolutionException
     *             if the export cannot be resolved
     */
    public void initialize(Console console, TEST test) throws MalformedNameException, ResolutionException, IOException {
        realm = testRealms.newRealm(console, test);
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
    public void execute(TEST test)
            throws ParserException, CompilationException, IOException, MalformedNameException, ResolutionException {
        Realm realm = get();

        // Evaluate actual test-script
        if (test.isModule()) {
            ScriptLoading.evalModule(realm, test.toModuleName());
        } else {
            ScriptLoading.eval(realm, test.getScript(), test.toFile());
        }

        // Wait for pending jobs to finish
        realm.getWorld().runEventLoop();
    }

    /**
     * Executes the supplied test object.
     * 
     * @param test
     *            the test-info object
     * @param jobSource
     *            the job source
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
     * @throws InterruptedException
     *             if interrupted while waiting
     */
    public void execute(TEST test, JobSource jobSource) throws ParserException, CompilationException, IOException,
            MalformedNameException, ResolutionException, InterruptedException {
        Realm realm = get();

        // Evaluate actual test-script
        if (test.isModule()) {
            ScriptLoading.evalModule(realm, test.toModuleName());
        } else {
            ScriptLoading.eval(realm, test.getScript(), test.toFile());
        }

        // Wait for pending jobs to finish
        realm.getWorld().runEventLoop(jobSource);
    }
}
