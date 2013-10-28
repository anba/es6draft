/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

/**
 * {@link java.util.ResourceBundle.Control} subclass to create {@link PropertyResourceBundle}
 * objects which use a configurable encoding instead of the default ISO-8859-1 encoding.
 */
public class PropertiesReaderControl extends ResourceBundle.Control {
    private final Charset charset;

    /**
     * Creates a new {@link PropertiesReaderControl} instance with the supplied character encoding
     */
    public PropertiesReaderControl(Charset charset) {
        this.charset = charset;
    }

    @Override
    public List<String> getFormats(String baseName) {
        return FORMAT_PROPERTIES;
    }

    @Override
    public ResourceBundle newBundle(String baseName, Locale locale, String format,
            ClassLoader loader, boolean reload) throws IOException {
        if ("java.properties".equals(format)) {
            String bundleName = toBundleName(baseName, locale);
            String resourceName = toResourceName(bundleName, "properties");
            InputStream stream = getInputStream(loader, resourceName, reload);
            if (stream == null) {
                return null;
            }
            try (Reader reader = new InputStreamReader(stream, charset)) {
                return new PropertyResourceBundle(reader);
            }
        } else {
            throw new IllegalArgumentException("unknown format: " + format);
        }
    }

    private static InputStream getInputStream(ClassLoader loader, String resourceName,
            boolean reload) throws IOException {
        URL url = loader.getResource(resourceName);
        if (url == null) {
            return null;
        }
        URLConnection connection = url.openConnection();
        connection.setUseCaches(!reload);
        return connection.getInputStream();
    }
}
