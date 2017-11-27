/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.modules.loader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import com.github.anba.es6draft.runtime.internal.Source;
import com.github.anba.es6draft.runtime.modules.ModuleSource;

/**
 * 
 */
public final class URLModuleSource implements ModuleSource {
    private final URL sourceURL;
    private final String sourceName;

    public URLModuleSource(URL sourceURL, String sourceName) {
        this.sourceURL = sourceURL;
        this.sourceName = sourceName;
    }

    @Override
    public String sourceCode() throws IOException {
        return readFully(sourceURL);
    }

    @Override
    public Source toSource() {
        return new Source(new URLSourceIdentifier(sourceURL), sourceName, 1);
    }

    private static String readFully(URL url) throws IOException {
        try (Reader reader = new BufferedReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder(4096);
            char cbuf[] = new char[4096];
            for (int len; (len = reader.read(cbuf)) != -1;) {
                sb.append(cbuf, 0, len);
            }
            return sb.toString();
        }
    }
}
