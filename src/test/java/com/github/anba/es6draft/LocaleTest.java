/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft;

import static org.junit.Assert.assertEquals;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

import org.junit.Test;

import com.github.anba.es6draft.compiler.Compiler;
import com.github.anba.es6draft.parser.Parser;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.World;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.ScriptLoader;
import com.github.anba.es6draft.runtime.internal.Source;
import com.github.anba.es6draft.runtime.modules.ModuleLoader;
import com.github.anba.es6draft.runtime.modules.loader.FileModuleLoader;
import com.github.anba.es6draft.runtime.objects.GlobalObject;

/**
 *
 */
public final class LocaleTest {
    private static World<GlobalObject> newWorld(Locale locale) {
        Set<CompatibilityOption> options = CompatibilityOption.StrictCompatibility();
        Set<Parser.Option> parserOptions = EnumSet.noneOf(Parser.Option.class);
        Set<Compiler.Option> compilerOptions = EnumSet.noneOf(Compiler.Option.class);
        Path baseDir = Paths.get("").toAbsolutePath();
        ScriptLoader scriptLoader = new ScriptLoader(options, parserOptions, compilerOptions);
        ModuleLoader moduleLoader = new FileModuleLoader(scriptLoader, baseDir);
        World<GlobalObject> world = new World<>(World.getDefaultGlobalObjectAllocator(),
                moduleLoader, scriptLoader, locale, TimeZone.getDefault());
        return world;
    }

    private static Realm newRealm(String languageTag) throws Exception {
        Locale locale = new Locale.Builder().setLanguageTag(languageTag).build();
        return newWorld(locale).newInitializedGlobal().getRealm();
    }

    private static Object eval(Realm realm, String sourceCode) {
        Source source = new Source("eval-locale-test", 1);
        return Scripts.ScriptEvaluation(realm.getScriptLoader().script(source, sourceCode), realm);
    }

    private enum Intl {
        Collator, DateTimeFormat, NumberFormat
    }

    private static String resolvedLocale(Realm realm, Intl constructor) {
        String sourceCode = String.format("new Intl.%s().resolvedOptions().locale", constructor);
        return (String) eval(realm, sourceCode);
    }

    private static String resolvedLocaleLookup(Realm realm, Intl constructor) {
        String sourceCode = String.format(
                "new Intl.%s({localeMatcher: 'lookup'}).resolvedOptions().locale", constructor);
        return (String) eval(realm, sourceCode);
    }

    @Test
    public void testLocaleWithScript() throws Exception {
        String languageTag = "de-Latn";
        Realm realm = newRealm(languageTag);

        assertEquals(languageTag, realm.getLocale().toLanguageTag());
        assertEquals("de", resolvedLocale(realm, Intl.Collator));
        assertEquals("de", resolvedLocale(realm, Intl.DateTimeFormat));
        assertEquals("de", resolvedLocale(realm, Intl.NumberFormat));
    }

    @Test
    public void testLocaleWithRegion() throws Exception {
        String languageTag = "de-AT";
        Realm realm = newRealm(languageTag);

        assertEquals(languageTag, realm.getLocale().toLanguageTag());
        assertEquals("de-AT", resolvedLocale(realm, Intl.Collator));
        assertEquals("de-AT", resolvedLocale(realm, Intl.DateTimeFormat));
        assertEquals("de-AT", resolvedLocale(realm, Intl.NumberFormat));
    }

    @Test
    public void testLocaleWithVariant() throws Exception {
        String languageTag = "de-1996";
        Realm realm = newRealm(languageTag);

        assertEquals(languageTag, realm.getLocale().toLanguageTag());
        assertEquals("de", resolvedLocale(realm, Intl.Collator));
        assertEquals("de", resolvedLocale(realm, Intl.DateTimeFormat));
        assertEquals("de", resolvedLocale(realm, Intl.NumberFormat));
    }

    @Test
    public void testLocaleWithScriptAndRegion() throws Exception {
        String languageTag = "de-Latn-AT";
        Realm realm = newRealm(languageTag);

        assertEquals(languageTag, realm.getLocale().toLanguageTag());
        assertEquals("de-AT", resolvedLocale(realm, Intl.Collator));
        assertEquals("de-AT", resolvedLocale(realm, Intl.DateTimeFormat));
        assertEquals("de-AT", resolvedLocale(realm, Intl.NumberFormat));
    }

    @Test
    public void testLocaleWithScriptAndRegionAndVariant() throws Exception {
        String languageTag = "de-Latn-AT-1996";
        Realm realm = newRealm(languageTag);

        assertEquals(languageTag, realm.getLocale().toLanguageTag());
        assertEquals("de-AT", resolvedLocale(realm, Intl.Collator));
        assertEquals("de-AT", resolvedLocale(realm, Intl.DateTimeFormat));
        assertEquals("de-AT", resolvedLocale(realm, Intl.NumberFormat));
    }

    @Test
    public void testLocaleWithUnicodeExtension() throws Exception {
        String languageTag = "de-u-co-phonebk";
        Realm realm = newRealm(languageTag);

        assertEquals(languageTag, realm.getLocale().toLanguageTag());
        assertEquals("de", resolvedLocale(realm, Intl.Collator));
        assertEquals("de", resolvedLocale(realm, Intl.DateTimeFormat));
        assertEquals("de", resolvedLocale(realm, Intl.NumberFormat));
    }

    @Test
    public void testLocaleWithRegionAndUnicodeExtension() throws Exception {
        String languageTag = "de-AT-u-co-phonebk";
        Realm realm = newRealm(languageTag);

        assertEquals(languageTag, realm.getLocale().toLanguageTag());
        assertEquals("de-AT", resolvedLocale(realm, Intl.Collator));
        assertEquals("de-AT", resolvedLocale(realm, Intl.DateTimeFormat));
        assertEquals("de-AT", resolvedLocale(realm, Intl.NumberFormat));
    }

    @Test
    public void testLocaleWithPrivateExtension() throws Exception {
        String languageTag = "de-x-private";
        Realm realm = newRealm(languageTag);

        assertEquals(languageTag, realm.getLocale().toLanguageTag());
        assertEquals("de", resolvedLocale(realm, Intl.Collator));
        assertEquals("de", resolvedLocale(realm, Intl.DateTimeFormat));
        assertEquals("de", resolvedLocale(realm, Intl.NumberFormat));
    }

    @Test
    public void testUnsupportedLocale() throws Exception {
        String languageTag = "tlh"; // = i-klingon
        Realm realm = newRealm(languageTag);

        assertEquals(languageTag, realm.getLocale().toLanguageTag());
        assertEquals("en", resolvedLocale(realm, Intl.Collator));
        assertEquals("en", resolvedLocale(realm, Intl.DateTimeFormat));
        assertEquals("en", resolvedLocale(realm, Intl.NumberFormat));
    }

    @Test
    public void testOnlyPrivateExtension() throws Exception {
        String languageTag = "x-private";
        Realm realm = newRealm(languageTag);

        assertEquals(languageTag, realm.getLocale().toLanguageTag());
        assertEquals("en", resolvedLocale(realm, Intl.Collator));
        assertEquals("en", resolvedLocale(realm, Intl.DateTimeFormat));
        assertEquals("en", resolvedLocale(realm, Intl.NumberFormat));
    }

    @Test
    public void test_en_GB() throws Exception {
        String languageTag = "en-GB";
        Realm realm = newRealm(languageTag);

        assertEquals(languageTag, realm.getLocale().toLanguageTag());
        assertEquals("en-GB", resolvedLocale(realm, Intl.Collator));
        assertEquals("en-GB", resolvedLocale(realm, Intl.DateTimeFormat));
        assertEquals("en-GB", resolvedLocale(realm, Intl.NumberFormat));
    }

    @Test
    public void testLookup_en_GB() throws Exception {
        String languageTag = "en-GB";
        Realm realm = newRealm(languageTag);

        assertEquals(languageTag, realm.getLocale().toLanguageTag());
        assertEquals("en-GB", resolvedLocaleLookup(realm, Intl.Collator));
        assertEquals("en-GB", resolvedLocaleLookup(realm, Intl.DateTimeFormat));
        assertEquals("en-GB", resolvedLocaleLookup(realm, Intl.NumberFormat));
    }

    @Test
    public void test_de_CH() throws Exception {
        String languageTag = "de-CH";
        Realm realm = newRealm(languageTag);

        assertEquals(languageTag, realm.getLocale().toLanguageTag());
        assertEquals("de-CH", resolvedLocale(realm, Intl.Collator));
        assertEquals("de-CH", resolvedLocale(realm, Intl.DateTimeFormat));
        assertEquals("de-CH", resolvedLocale(realm, Intl.NumberFormat));
    }

    @Test
    public void testLookup_de_CH() throws Exception {
        String languageTag = "de-CH";
        Realm realm = newRealm(languageTag);

        assertEquals(languageTag, realm.getLocale().toLanguageTag());
        assertEquals("de-CH", resolvedLocaleLookup(realm, Intl.Collator));
        assertEquals("de-CH", resolvedLocaleLookup(realm, Intl.DateTimeFormat));
        assertEquals("de-CH", resolvedLocaleLookup(realm, Intl.NumberFormat));
    }
}
