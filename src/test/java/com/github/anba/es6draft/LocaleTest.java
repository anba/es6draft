/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft;

import static org.junit.Assert.assertEquals;

import java.util.EnumSet;
import java.util.Locale;

import org.junit.Test;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.World;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.RuntimeContext;
import com.github.anba.es6draft.runtime.internal.ScriptLoading;

/**
 *
 */
public final class LocaleTest {
    private static final EnumSet<CompatibilityOption> intlExtensions = EnumSet.of(CompatibilityOption.PluralRules,
            CompatibilityOption.IntlSegmenter, CompatibilityOption.IntlListFormat);

    private static Realm newRealm(String languageTag) throws Exception {
        Locale locale = new Locale.Builder().setLanguageTag(languageTag).build();
        RuntimeContext context = new RuntimeContext.Builder().setLocale(locale).setOptions(intlExtensions).build();
        World world = new World(context);
        return Realm.InitializeHostDefinedRealm(world);
    }

    private enum Intl {
        Collator, DateTimeFormat, ListFormat, NumberFormat, PluralRules, Segmenter
    }

    private static String resolvedLocale(Realm realm, Intl constructor) {
        String sourceCode = String.format("new Intl.%s().resolvedOptions().locale", constructor);
        return (String) ScriptLoading.eval(realm, "eval-locale-test", sourceCode);
    }

    private static String resolvedLocaleLookup(Realm realm, Intl constructor) {
        String sourceCode = String.format("new Intl.%s({localeMatcher: 'lookup'}).resolvedOptions().locale",
                constructor);
        return (String) ScriptLoading.eval(realm, "eval-locale-test", sourceCode);
    }

    @Test
    public void testLocaleWithScript() throws Exception {
        String languageTag = "de-Latn";
        Realm realm = newRealm(languageTag);

        assertEquals(languageTag, realm.getLocale().toLanguageTag());
        for (Intl constructor : Intl.values()) {
            assertEquals("de", resolvedLocale(realm, constructor));
        }
    }

    @Test
    public void testLocaleWithRegion() throws Exception {
        String languageTag = "de-AT";
        Realm realm = newRealm(languageTag);

        assertEquals(languageTag, realm.getLocale().toLanguageTag());
        for (Intl constructor : Intl.values()) {
            assertEquals("de-AT", resolvedLocale(realm, constructor));
        }
    }

    @Test
    public void testLocaleWithVariant() throws Exception {
        String languageTag = "de-1996";
        Realm realm = newRealm(languageTag);

        assertEquals(languageTag, realm.getLocale().toLanguageTag());
        for (Intl constructor : Intl.values()) {
            assertEquals("de", resolvedLocale(realm, constructor));
        }
    }

    @Test
    public void testLocaleWithScriptAndRegion() throws Exception {
        String languageTag = "de-Latn-AT";
        Realm realm = newRealm(languageTag);

        assertEquals(languageTag, realm.getLocale().toLanguageTag());
        for (Intl constructor : Intl.values()) {
            assertEquals("de-AT", resolvedLocale(realm, constructor));
        }
    }

    @Test
    public void testLocaleWithScriptAndRegionAndVariant() throws Exception {
        String languageTag = "de-Latn-AT-1996";
        Realm realm = newRealm(languageTag);

        assertEquals(languageTag, realm.getLocale().toLanguageTag());
        for (Intl constructor : Intl.values()) {
            assertEquals("de-AT", resolvedLocale(realm, constructor));
        }
    }

    @Test
    public void testLocaleWithUnicodeExtension() throws Exception {
        String languageTag = "de-u-co-phonebk";
        Realm realm = newRealm(languageTag);

        assertEquals(languageTag, realm.getLocale().toLanguageTag());
        for (Intl constructor : Intl.values()) {
            assertEquals("de", resolvedLocale(realm, constructor));
        }
    }

    @Test
    public void testLocaleWithRegionAndUnicodeExtension() throws Exception {
        String languageTag = "de-AT-u-co-phonebk";
        Realm realm = newRealm(languageTag);

        assertEquals(languageTag, realm.getLocale().toLanguageTag());
        for (Intl constructor : Intl.values()) {
            assertEquals("de-AT", resolvedLocale(realm, constructor));
        }
    }

    @Test
    public void testLocaleWithPrivateExtension() throws Exception {
        String languageTag = "de-x-private";
        Realm realm = newRealm(languageTag);

        assertEquals(languageTag, realm.getLocale().toLanguageTag());
        for (Intl constructor : Intl.values()) {
            assertEquals("de", resolvedLocale(realm, constructor));
        }
    }

    @Test
    public void testUnsupportedLocale() throws Exception {
        String languageTag = "tlh"; // = i-klingon
        Realm realm = newRealm(languageTag);

        assertEquals(languageTag, realm.getLocale().toLanguageTag());
        for (Intl constructor : Intl.values()) {
            assertEquals("en", resolvedLocale(realm, constructor));
        }
    }

    @Test
    public void testOnlyPrivateExtension() throws Exception {
        String languageTag = "x-private";
        Realm realm = newRealm(languageTag);

        assertEquals(languageTag, realm.getLocale().toLanguageTag());
        for (Intl constructor : Intl.values()) {
            assertEquals("en", resolvedLocale(realm, constructor));
        }
    }

    @Test
    public void test_en_GB() throws Exception {
        String languageTag = "en-GB";
        Realm realm = newRealm(languageTag);

        assertEquals(languageTag, realm.getLocale().toLanguageTag());
        for (Intl constructor : Intl.values()) {
            assertEquals("en-GB", resolvedLocale(realm, constructor));
        }
        for (Intl constructor : Intl.values()) {
            assertEquals("en-GB", resolvedLocaleLookup(realm, constructor));
        }
    }

    @Test
    public void test_de_CH() throws Exception {
        String languageTag = "de-CH";
        Realm realm = newRealm(languageTag);

        assertEquals(languageTag, realm.getLocale().toLanguageTag());
        for (Intl constructor : Intl.values()) {
            assertEquals("de-CH", resolvedLocale(realm, constructor));
        }
        for (Intl constructor : Intl.values()) {
            assertEquals("de-CH", resolvedLocaleLookup(realm, constructor));
        }
    }

    @Test
    public void test_br() throws Exception {
        // 'br' is not supported by Collator, fallback is 'fr'.
        String languageTag = "br";
        Realm realm = newRealm(languageTag);

        assertEquals(languageTag, realm.getLocale().toLanguageTag());
        for (Intl constructor : Intl.values()) {
            assertEquals("fr", resolvedLocale(realm, constructor));
        }
        for (Intl constructor : Intl.values()) {
            assertEquals("fr", resolvedLocaleLookup(realm, constructor));
        }
    }

    @Test
    public void test_ce() throws Exception {
        // 'ce' is not supported by Collator, no fallback is available, hence defaults to 'en'.
        String languageTag = "ce";
        Realm realm = newRealm(languageTag);

        assertEquals(languageTag, realm.getLocale().toLanguageTag());
        for (Intl constructor : Intl.values()) {
            assertEquals("en", resolvedLocale(realm, constructor));
        }
        for (Intl constructor : Intl.values()) {
            assertEquals("en", resolvedLocaleLookup(realm, constructor));
        }
    }

    @Test
    public void test_kok() throws Exception {
        // 'kok' is not supported by PluralRules, no fallback is available, hence defaults to 'en'.
        String languageTag = "kok";
        Realm realm = newRealm(languageTag);

        assertEquals(languageTag, realm.getLocale().toLanguageTag());
        for (Intl constructor : Intl.values()) {
            assertEquals("en", resolvedLocale(realm, constructor));
        }
        for (Intl constructor : Intl.values()) {
            assertEquals("en", resolvedLocaleLookup(realm, constructor));
        }
    }
}
