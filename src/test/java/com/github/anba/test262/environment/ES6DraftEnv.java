/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.test262.environment;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BOMInputStream;

import com.github.anba.es6draft.Script;
import com.github.anba.es6draft.ScriptLoader;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.objects.ErrorObject;
import com.github.anba.es6draft.runtime.types.Scriptable;
import com.github.anba.test262.util.ScriptErrorMatcher;

/**
 * 
 */
abstract class ES6DraftEnv<GLOBAL extends Scriptable & GlobalObject> implements Environment<GLOBAL> {

    @Override
    public abstract GLOBAL global();

    public abstract Realm realm();

    protected abstract String getCharsetName();

    @Override
    public Class<?>[] exceptions() {
        return new Class[] { ScriptException.class, ParserException.class };
    }

    @Override
    public ScriptErrorMatcher<RuntimeException> matcher(String errorType) {
        return new ScriptErrorMatcher<RuntimeException>() {
            @Override
            public boolean matches(RuntimeException error, String errorType) {
                // errorType is now a regular expression
                Pattern p = Pattern.compile(errorType, Pattern.CASE_INSENSITIVE);
                String name;
                if (error instanceof ScriptException) {
                    Object value = ((ScriptException) error).getValue();
                    if (value instanceof ErrorObject) {
                        name = value.toString();
                    } else {
                        name = "";
                    }
                } else if (error instanceof ParserException) {
                    name = ((ParserException) error).getPlainMessage();
                } else {
                    name = "";
                }
                return p.matcher(name).find();
            }

            @Override
            public Class<? extends RuntimeException> exception() {
                return RuntimeException.class;
            }
        };
    }

    private AtomicInteger scriptCounter = new AtomicInteger(0);

    private String nextScriptName() {
        return "Script_" + scriptCounter.incrementAndGet();
    }

    /**
     * Parses, compiles and executes the javascript file
     */
    @Override
    public void eval(String sourceName, InputStream source) throws IOException {
        eval(script(sourceName, source));
    }

    /**
     * Parses and compiles the javascript file
     */
    public Script script(String sourceName, InputStream source) throws IOException {
        String className = nextScriptName();
        Reader reader = newReader(source, getCharsetName());
        return ScriptLoader.load(sourceName, className, IOUtils.toString(reader), false);
    }

    /**
     * Executes the javascript file
     */
    public void eval(Script script) {
        ScriptLoader.ScriptEvaluation(script, realm(), false);
    }

    /**
     * Returns a new {@link Reader} for the {@code stream} parameter
     */
    private static Reader newReader(InputStream stream, String defaultCharset) throws IOException {
        BOMInputStream bomstream = new BOMInputStream(stream, ByteOrderMark.UTF_8,
                ByteOrderMark.UTF_16LE, ByteOrderMark.UTF_16BE);
        String charset = defaultIfNull(bomstream.getBOMCharsetName(), defaultCharset);
        return new InputStreamReader(bomstream, charset);
    }
}