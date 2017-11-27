/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft;

import static org.junit.Assert.assertEquals;

import java.util.Locale;
import java.util.TimeZone;

import org.junit.Test;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.World;
import com.github.anba.es6draft.runtime.internal.RuntimeContext;
import com.github.anba.es6draft.runtime.internal.ScriptLoading;

/**
 *
 */
public final class DefaultLocaleTimezone {
    private static Realm newRealm(Locale locale) throws Exception {
        RuntimeContext context = new RuntimeContext.Builder().setLocale(locale).build();
        World world = new World(context);
        return Realm.InitializeHostDefinedRealm(world);
    }

    private static Realm newRealm(TimeZone timeZone) throws Exception {
        RuntimeContext context = new RuntimeContext.Builder().setTimeZone(timeZone).build();
        World world = new World(context);
        return Realm.InitializeHostDefinedRealm(world);
    }

    private enum Intl {
        Collator, DateTimeFormat, NumberFormat
    }

    private static String resolvedLocale(Realm realm, Intl constructor) {
        String sourceCode = String.format("new Intl.%s().resolvedOptions().locale", constructor);
        return (String) ScriptLoading.eval(realm, "eval-locale-test", sourceCode);
    }

    private static String resolvedTimeZone(Realm realm) {
        String sourceCode = "new Intl.DateTimeFormat().resolvedOptions().timeZone";
        return (String) ScriptLoading.eval(realm, "eval-locale-test", sourceCode);
    }

    @Test
    public void testInvalidLocale() throws Exception {
        Realm realm = newRealm(new Locale("invalid"));
        assertEquals("en", resolvedLocale(realm, Intl.Collator));
        assertEquals("en", resolvedLocale(realm, Intl.DateTimeFormat));
        assertEquals("en", resolvedLocale(realm, Intl.NumberFormat));
    }

    @Test
    public void testInvalidTimezone() throws Exception {
        Realm realm = newRealm(TimeZone.getTimeZone("invalid"));
        assertEquals("UTC", resolvedTimeZone(realm));
    }

    @Test
    public void testUTC() throws Exception {
        Realm realm = newRealm(TimeZone.getTimeZone("UTC"));
        assertEquals("UTC", resolvedTimeZone(realm));
    }

    @Test
    public void testEtc_UCT() throws Exception {
        Realm realm = newRealm(TimeZone.getTimeZone("Etc/UCT"));
        assertEquals("UTC", resolvedTimeZone(realm));
    }

    @Test
    public void testUCT() throws Exception {
        Realm realm = newRealm(TimeZone.getTimeZone("UCT"));
        assertEquals("UTC", resolvedTimeZone(realm));
    }

    @Test
    public void testEtc_Zulu() throws Exception {
        Realm realm = newRealm(TimeZone.getTimeZone("Etc/Zulu"));
        assertEquals("UTC", resolvedTimeZone(realm));
    }

    @Test
    public void testZulu() throws Exception {
        Realm realm = newRealm(TimeZone.getTimeZone("Zulu"));
        assertEquals("UTC", resolvedTimeZone(realm));
    }

    @Test
    public void testEtc_Greenwich() throws Exception {
        Realm realm = newRealm(TimeZone.getTimeZone("Etc/Greenwich"));
        assertEquals("UTC", resolvedTimeZone(realm));
    }

    @Test
    public void testGreenwich() throws Exception {
        Realm realm = newRealm(TimeZone.getTimeZone("Greenwich"));
        assertEquals("UTC", resolvedTimeZone(realm));
    }

    @Test
    public void testUniversal() throws Exception {
        Realm realm = newRealm(TimeZone.getTimeZone("Universal"));
        assertEquals("UTC", resolvedTimeZone(realm));
    }

    @Test
    public void testJapan() throws Exception {
        Realm realm = newRealm(TimeZone.getTimeZone("Japan"));
        assertEquals("Asia/Tokyo", resolvedTimeZone(realm));
    }

    @Test
    public void testWSU() throws Exception {
        Realm realm = newRealm(TimeZone.getTimeZone("W-SU"));
        assertEquals("Europe/Moscow", resolvedTimeZone(realm));
    }

    @Test
    public void testAustralia_Tasmania() throws Exception {
        Realm realm = newRealm(TimeZone.getTimeZone("Australia/Tasmania"));
        assertEquals("Australia/Hobart", resolvedTimeZone(realm));
    }

    @Test
    public void testGMT() throws Exception {
        Realm realm = newRealm(TimeZone.getTimeZone("GMT"));
        assertEquals("UTC", resolvedTimeZone(realm));
    }

    @Test
    public void testGMT0() throws Exception {
        Realm realm = newRealm(TimeZone.getTimeZone("GMT0"));
        assertEquals("UTC", resolvedTimeZone(realm));
    }

    @Test
    public void testGMT_plus_0() throws Exception {
        Realm realm = newRealm(TimeZone.getTimeZone("GMT+0"));
        assertEquals("UTC", resolvedTimeZone(realm));
    }

    @Test
    public void testGMT_minus_0() throws Exception {
        Realm realm = newRealm(TimeZone.getTimeZone("GMT-0"));
        assertEquals("UTC", resolvedTimeZone(realm));
    }

    @Test
    public void testEtc_GMTWithHourOffset() throws Exception {
        Realm realm = newRealm(TimeZone.getTimeZone("Etc/GMT-8"));
        assertEquals("Etc/GMT-8", resolvedTimeZone(realm));
    }

    @Test
    public void testGMTWithHourOffset() throws Exception {
        Realm realm = newRealm(TimeZone.getTimeZone("GMT-8"));
        assertEquals("UTC", resolvedTimeZone(realm));
    }

    @Test
    public void testGMTWithOffset() throws Exception {
        Realm realm = newRealm(TimeZone.getTimeZone("GMT-08:00"));
        assertEquals("UTC", resolvedTimeZone(realm));
    }
}
