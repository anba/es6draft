/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.util;

import static com.github.anba.es6draft.util.Functional.intoCollection;
import static com.github.anba.es6draft.util.Functional.toStrings;
import static java.util.Collections.emptyList;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.Configuration;

import com.github.anba.es6draft.Script;
import com.github.anba.es6draft.repl.console.ShellConsole;
import com.github.anba.es6draft.repl.global.ShellGlobalObject;
import com.github.anba.es6draft.runtime.World;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.internal.ScriptCache;

/**
 * {@link TestGlobals} sub-class to facilitate creation of {@link ShellGlobalObject} instances
 */
public abstract class TestShellGlobals<GLOBAL extends ShellGlobalObject> extends
        TestGlobals<GLOBAL> {
    private List<Script> initScripts;
    private List<String> includes;

    public TestShellGlobals(Configuration configuration) {
        super(configuration);
    }

    @Override
    protected void before() throws Throwable {
        super.before();

        // pre-compile init scripts
        List<?> initScriptNames = getConfiguration().getList("scripts.init", emptyList());
        List<Script> scripts = new ArrayList<>();
        for (String scriptName : toStrings(initScriptNames)) {
            scripts.add(ShellGlobalObject.compileScript(getScriptCache(), scriptName));
        }
        this.initScripts = scripts;

        List<?> includeScripts = getConfiguration().getList("scripts.include", emptyList());
        this.includes = intoCollection(toStrings(includeScripts), new ArrayList<String>());
    }

    public GLOBAL newGlobal(ShellConsole console, TestInfo test) throws IOException {
        ObjectAllocator<GLOBAL> allocator = newAllocator(console, test, getScriptCache());
        World<GLOBAL> world = new World<>(allocator, getOptions(), getCompilerOptions());
        GLOBAL global = world.newGlobal();
        // evaluate initialisation scripts
        for (Script script : initScripts) {
            global.eval(script);
        }
        // load and execute additional includes
        for (String file : includes) {
            global.include(Paths.get(file));
        }
        return global;
    }

    protected abstract ObjectAllocator<GLOBAL> newAllocator(ShellConsole console, TestInfo test,
            ScriptCache scriptCache);
}
