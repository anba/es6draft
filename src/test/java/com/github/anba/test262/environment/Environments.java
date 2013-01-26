/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.test262.environment;

import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.configuration.Configuration;
import org.junit.Assert;

import com.github.anba.es6draft.Script;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.test262.util.Test262AssertionError;
import com.github.anba.test262.util.Test262Info;

/**
 * 
 */
public final class Environments {
    private Environments() {
    }

    public interface EnvironmentProvider<T extends GlobalObject> {
        Environment<T> environment(String testsuite, String sourceName, Test262Info info);
    }

    /**
     * Amended {@link Assert#fail()} method
     */
    private static void failWith(String message, String sourceName) {
        String msg = String.format("%s [file: %s]", message, sourceName);
        throw new Test262AssertionError(msg);
    }

    /**
     * Returns the default environment provider
     */
    public static EnvironmentProvider<? extends GlobalObject> get(Configuration configuration) {
        String library = configuration.getString("test.provider", "es6draft");
        switch (library) {
        case "es6draft":
            return es6draft(configuration);
        default:
            throw new IllegalArgumentException(library);
        }
    }

    /**
     * Creates a new es6draft environment
     */
    public static EnvironmentProvider<ES6DraftGlobalObject> es6draft(
            final Configuration configuration) {
        return new EnvironmentProvider<ES6DraftGlobalObject>() {
            @SuppressWarnings("serial")
            class ScriptCache extends LinkedHashMap<Path, Script> {
                final int capacity = 10;

                public ScriptCache() {
                    super(16, 0.75f, true);
                }

                @Override
                protected boolean removeEldestEntry(java.util.Map.Entry<Path, Script> eldest) {
                    return size() >= capacity;
                }
            }

            // provider-wide script cache (380ms -> 210ms duration)
            private final ScriptCache cache = new ScriptCache();

            @Override
            public ES6DraftEnv<ES6DraftGlobalObject> environment(final String testsuite,
                    final String sourceName, final Test262Info info) {
                Configuration c = configuration.subset(testsuite);
                final boolean strictSupported = c.getBoolean("strict", false);
                final String encoding = c.getString("encoding", "UTF-8");
                final String libpath = c.getString("lib_path");
                final AtomicReference<ES6DraftGlobalObject> $global = new AtomicReference<>();
                final AtomicReference<Realm> $realm = new AtomicReference<>();

                final ES6DraftEnv<ES6DraftGlobalObject> environment = new ES6DraftEnv<ES6DraftGlobalObject>() {
                    @Override
                    public ES6DraftGlobalObject global() {
                        return $global.get();
                    }

                    @Override
                    public Realm realm() {
                        return $realm.get();
                    }

                    @Override
                    protected String getCharsetName() {
                        return encoding;
                    }
                };

                final Realm realm = Realm
                        .newRealm(new Realm.GlobalObjectCreator<ES6DraftGlobalObject>() {
                            @Override
                            public ES6DraftGlobalObject createGlobal(final Realm realm) {
                                return new ES6DraftGlobalObject(realm) {
                                    @Override
                                    protected boolean isStrictSupported() {
                                        return strictSupported;
                                    }

                                    @Override
                                    protected String getDescription() {
                                        return info.getDescription();
                                    }

                                    @Override
                                    protected void failure(String message) {
                                        failWith(message, sourceName);
                                    }

                                    @Override
                                    protected void include(Path path) throws IOException {
                                        // resolve the input file against the library path
                                        Path file = Paths.get(libpath).resolve(path);
                                        if (!cache.containsKey(file)) {
                                            String sourceName = file.getFileName().toString();
                                            InputStream source = Files.newInputStream(file);
                                            Script script = environment.script(sourceName, source);
                                            cache.put(file, script);
                                        }
                                        environment.eval(cache.get(file));
                                        // environment.eval(file.getFileName().toString(), source);
                                    }

                                    @Override
                                    public void setUp() {
                                        createProperties(this, realm, ES6DraftGlobalObject.class);
                                    }
                                };
                            }
                        });

                ES6DraftGlobalObject global = (ES6DraftGlobalObject) realm.getGlobalThis();
                $global.set(global);
                $realm.set(realm);

                return environment;
            }
        };
    }
}
