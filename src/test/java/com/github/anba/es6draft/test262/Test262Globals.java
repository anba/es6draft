/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.test262;

import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.configuration.Configuration;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.World;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.util.TestGlobals;

/**
 * {@link TestGlobals} sub-class to facilitate creation of {@link Test262GlobalObject} instances
 */
public class Test262Globals extends TestGlobals<Test262GlobalObject> {
    public Test262Globals(Configuration configuration) {
        super(configuration);
    }

    public Test262GlobalObject newGlobal(final Test262Info test) throws IOException {
        test.readFileInformation();

        final Path libpath = Paths.get(getConfiguration().getString("lib_path"));
        World<Test262GlobalObject> world = new World<>(new ObjectAllocator<Test262GlobalObject>() {
            @Override
            public Test262GlobalObject newInstance(Realm realm) {
                return new Test262GlobalObject(realm, libpath, getScriptCache(), test);
            }
        }, getOptions(), getCompilerOptions());
        Test262GlobalObject global = world.newGlobal();

        // start initialization
        ExecutionContext cx = global.getRealm().defaultContext();
        global.include(cx, "sta.js");
        createProperties(global, global, cx, Test262GlobalObject.class);

        return global;
    }
}
