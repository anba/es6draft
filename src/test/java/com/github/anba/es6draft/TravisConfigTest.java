/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Test;
import org.yaml.snakeyaml.Yaml;

/**
 * Test travis configuration file is valid YAML.
 */
public class TravisConfigTest {
    private static final String CONFIG_FILE_NAME = ".travis.yml";

    @Test
    public void testConfiguration() throws IOException {
        try (InputStream is = Files.newInputStream(Paths.get(CONFIG_FILE_NAME))) {
            new Yaml().load(is);
        }
    }
}
