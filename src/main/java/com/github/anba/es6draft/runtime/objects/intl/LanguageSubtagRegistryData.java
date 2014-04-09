/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.intl;

import java.util.HashMap;

/**
 * Supplemental data for canonicalization of language tags.
 * <p>
 * Data generated from: language-subtag-registry, 2014-02-18
 * 
 * @see <a
 *      href="http://www.iana.org/assignments/language-subtag-registry">language-subtag-registry</a>
 */
final class LanguageSubtagRegistryData {
    private LanguageSubtagRegistryData() {
    }

    static final String grandfathered(String languageTag) {
        String v = grandfatheredData.get(languageTag);
        if (v != null) {
            return v;
        }
        return null;
    }

    /**
     * Does not return input on mismatch.
     * 
     * @param languageTag
     *            the language tag
     * @return the canonicalized language tag or {@code null} if not applicable
     */
    static final String redundant(String languageTag) {
        String v = redundantData.get(languageTag);
        if (v != null) {
            return v;
        }
        return null;
    }

    /**
     * Returns input on mismatch.
     * 
     * @param language
     *            the language
     * @return the canonicalized language or the input if not applicable
     */
    static final String language(String language) {
        String v = languageData.get(language);
        if (v != null) {
            return v;
        }
        return language;
    }

    /**
     * Does not return input on mismatch.
     * 
     * @param prefix
     *            the prefix string
     * @param extlang
     *            the extended language
     * @return the canonicalized extended language or {@code null} if not applicable
     */
    static final String extlang(String prefix, String extlang) {
        String v = extlangData.get(extlang);
        if (v != null && prefix.equals(v)) {
            return extlang;
        }
        return null;
    }

    /**
     * Returns input on mismatch.
     * 
     * @param region
     *            the region
     * @return the canonicalized region or the input if not applicable
     */
    static final String region(String region) {
        String v = regionData.get(region);
        if (v != null) {
            return v;
        }
        return region;
    }

    /**
     * Does not return input on mismatch.
     * 
     * @param prefix
     *            the prefix string
     * @param variant
     *            the variant
     * @return the canonicalized variant or {@code null} if not applicable
     */
    static final String variant(StringBuilder prefix, String variant) {
        // handle this replacement manually
        if ("heploc".equals(variant) && "ja-Latn-hepburn".equals(prefix.toString())) {
            // apparently this should replace the complete tag...?
            return "ja-Latn-alalc97";
        }
        return null;
    }

    private static final HashMap<String, String> grandfatheredData;
    static {
        // preferred-values for grandfathered language tags
        HashMap<String, String> map = new HashMap<>();
        map.put("art-lojban", "jbo");
        map.put("cel-gaulish", "cel-gaulish");
        map.put("en-gb-oed", "en-GB-oed");
        map.put("i-ami", "ami");
        map.put("i-bnn", "bnn");
        map.put("i-default", "i-default");
        map.put("i-enochian", "i-enochian");
        map.put("i-hak", "hak");
        map.put("i-klingon", "tlh");
        map.put("i-lux", "lb");
        map.put("i-mingo", "i-mingo");
        map.put("i-navajo", "nv");
        map.put("i-pwn", "pwn");
        map.put("i-tao", "tao");
        map.put("i-tay", "tay");
        map.put("i-tsu", "tsu");
        map.put("no-bok", "nb");
        map.put("no-nyn", "nn");
        map.put("sgn-be-fr", "sfb");
        map.put("sgn-be-nl", "vgt");
        map.put("sgn-ch-de", "sgg");
        map.put("zh-guoyu", "cmn");
        map.put("zh-hakka", "hak");
        map.put("zh-min", "zh-min");
        map.put("zh-min-nan", "nan");
        map.put("zh-xiang", "hsn");
        grandfatheredData = map;
    }

    private static final HashMap<String, String> redundantData;
    static {
        // preferred-values for redundant language tags
        HashMap<String, String> map = new HashMap<>();
        map.put("sgn-br", "bzs");
        map.put("sgn-co", "csn");
        map.put("sgn-de", "gsg");
        map.put("sgn-dk", "dsl");
        map.put("sgn-es", "ssp");
        map.put("sgn-fr", "fsl");
        map.put("sgn-gb", "bfi");
        map.put("sgn-gr", "gss");
        map.put("sgn-ie", "isg");
        map.put("sgn-it", "ise");
        map.put("sgn-jp", "jsl");
        map.put("sgn-mx", "mfs");
        map.put("sgn-ni", "ncs");
        map.put("sgn-nl", "dse");
        map.put("sgn-no", "nsl");
        map.put("sgn-pt", "psr");
        map.put("sgn-se", "swl");
        map.put("sgn-us", "ase");
        map.put("sgn-za", "sfs");
        map.put("zh-cmn", "cmn");
        map.put("zh-cmn-hans", "cmn-Hans");
        map.put("zh-cmn-hant", "cmn-Hant");
        map.put("zh-gan", "gan");
        map.put("zh-wuu", "wuu");
        map.put("zh-yue", "yue");
        redundantData = map;
    }

    private static final HashMap<String, String> languageData;
    static {
        // preferred-values for language subtag
        HashMap<String, String> map = new HashMap<>();
        map.put("in", "id");
        map.put("iw", "he");
        map.put("ji", "yi");
        map.put("jw", "jv");
        map.put("mo", "ro");
        map.put("ayx", "nun");
        map.put("bjd", "drl");
        map.put("ccq", "rki");
        map.put("cjr", "mom");
        map.put("cka", "cmr");
        map.put("cmk", "xch");
        map.put("drh", "khk");
        map.put("drw", "prs");
        map.put("gav", "dev");
        map.put("hrr", "jal");
        map.put("ibi", "opa");
        map.put("ilw", "gal");
        map.put("kgh", "kml");
        map.put("meg", "cir");
        map.put("mst", "mry");
        map.put("myt", "mry");
        map.put("pcr", "adx");
        map.put("ppr", "lcq");
        map.put("sca", "hle");
        map.put("tie", "ras");
        map.put("tkk", "twm");
        map.put("tlw", "weo");
        map.put("tnf", "prs");
        map.put("xia", "acn");
        map.put("ybd", "rki");
        map.put("yma", "lrr");
        map.put("yos", "zom");
        languageData = map;
    }

    private static final HashMap<String, String> regionData;
    static {
        // preferred-values for region subtag
        HashMap<String, String> map = new HashMap<>();
        map.put("bu", "MM");
        map.put("dd", "DE");
        map.put("fx", "FR");
        map.put("tp", "TL");
        map.put("yd", "YE");
        map.put("zr", "CD");
        regionData = map;
    }

    private static final HashMap<String, String> extlangData;
    static {
        // preferred-value is equal to extlang, therefore only store [extlang, prefix] pairs, where
        // prefix is the language subtag
        HashMap<String, String> map = new HashMap<>();
        map.put("aao", "ar");
        map.put("abh", "ar");
        map.put("abv", "ar");
        map.put("acm", "ar");
        map.put("acq", "ar");
        map.put("acw", "ar");
        map.put("acx", "ar");
        map.put("acy", "ar");
        map.put("adf", "ar");
        map.put("ads", "sgn");
        map.put("aeb", "ar");
        map.put("aec", "ar");
        map.put("aed", "sgn");
        map.put("aen", "sgn");
        map.put("afb", "ar");
        map.put("afg", "sgn");
        map.put("ajp", "ar");
        map.put("apc", "ar");
        map.put("apd", "ar");
        map.put("arb", "ar");
        map.put("arq", "ar");
        map.put("ars", "ar");
        map.put("ary", "ar");
        map.put("arz", "ar");
        map.put("ase", "sgn");
        map.put("asf", "sgn");
        map.put("asp", "sgn");
        map.put("asq", "sgn");
        map.put("asw", "sgn");
        map.put("auz", "ar");
        map.put("avl", "ar");
        map.put("ayh", "ar");
        map.put("ayl", "ar");
        map.put("ayn", "ar");
        map.put("ayp", "ar");
        map.put("bbz", "ar");
        map.put("bfi", "sgn");
        map.put("bfk", "sgn");
        map.put("bjn", "ms");
        map.put("bog", "sgn");
        map.put("bqn", "sgn");
        map.put("bqy", "sgn");
        map.put("btj", "ms");
        map.put("bve", "ms");
        map.put("bvl", "sgn");
        map.put("bvu", "ms");
        map.put("bzs", "sgn");
        map.put("cdo", "zh");
        map.put("cds", "sgn");
        map.put("cjy", "zh");
        map.put("cmn", "zh");
        map.put("coa", "ms");
        map.put("cpx", "zh");
        map.put("csc", "sgn");
        map.put("csd", "sgn");
        map.put("cse", "sgn");
        map.put("csf", "sgn");
        map.put("csg", "sgn");
        map.put("csl", "sgn");
        map.put("csn", "sgn");
        map.put("csq", "sgn");
        map.put("csr", "sgn");
        map.put("czh", "zh");
        map.put("czo", "zh");
        map.put("doq", "sgn");
        map.put("dse", "sgn");
        map.put("dsl", "sgn");
        map.put("dup", "ms");
        map.put("ecs", "sgn");
        map.put("esl", "sgn");
        map.put("esn", "sgn");
        map.put("eso", "sgn");
        map.put("eth", "sgn");
        map.put("fcs", "sgn");
        map.put("fse", "sgn");
        map.put("fsl", "sgn");
        map.put("fss", "sgn");
        map.put("gan", "zh");
        map.put("gds", "sgn");
        map.put("gom", "kok");
        map.put("gse", "sgn");
        map.put("gsg", "sgn");
        map.put("gsm", "sgn");
        map.put("gss", "sgn");
        map.put("gus", "sgn");
        map.put("hab", "sgn");
        map.put("haf", "sgn");
        map.put("hak", "zh");
        map.put("hds", "sgn");
        map.put("hji", "ms");
        map.put("hks", "sgn");
        map.put("hos", "sgn");
        map.put("hps", "sgn");
        map.put("hsh", "sgn");
        map.put("hsl", "sgn");
        map.put("hsn", "zh");
        map.put("icl", "sgn");
        map.put("ils", "sgn");
        map.put("inl", "sgn");
        map.put("ins", "sgn");
        map.put("ise", "sgn");
        map.put("isg", "sgn");
        map.put("isr", "sgn");
        map.put("jak", "ms");
        map.put("jax", "ms");
        map.put("jcs", "sgn");
        map.put("jhs", "sgn");
        map.put("jls", "sgn");
        map.put("jos", "sgn");
        map.put("jsl", "sgn");
        map.put("jus", "sgn");
        map.put("kgi", "sgn");
        map.put("knn", "kok");
        map.put("kvb", "ms");
        map.put("kvk", "sgn");
        map.put("kvr", "ms");
        map.put("kxd", "ms");
        map.put("lbs", "sgn");
        map.put("lce", "ms");
        map.put("lcf", "ms");
        map.put("liw", "ms");
        map.put("lls", "sgn");
        map.put("lsg", "sgn");
        map.put("lsl", "sgn");
        map.put("lso", "sgn");
        map.put("lsp", "sgn");
        map.put("lst", "sgn");
        map.put("lsy", "sgn");
        map.put("ltg", "lv");
        map.put("lvs", "lv");
        map.put("lzh", "zh");
        map.put("max", "ms");
        map.put("mdl", "sgn");
        map.put("meo", "ms");
        map.put("mfa", "ms");
        map.put("mfb", "ms");
        map.put("mfs", "sgn");
        map.put("min", "ms");
        map.put("mnp", "zh");
        map.put("mqg", "ms");
        map.put("mre", "sgn");
        map.put("msd", "sgn");
        map.put("msi", "ms");
        map.put("msr", "sgn");
        map.put("mui", "ms");
        map.put("mzc", "sgn");
        map.put("mzg", "sgn");
        map.put("mzy", "sgn");
        map.put("nan", "zh");
        map.put("nbs", "sgn");
        map.put("ncs", "sgn");
        map.put("nsi", "sgn");
        map.put("nsl", "sgn");
        map.put("nsp", "sgn");
        map.put("nsr", "sgn");
        map.put("nzs", "sgn");
        map.put("okl", "sgn");
        map.put("orn", "ms");
        map.put("ors", "ms");
        map.put("pel", "ms");
        map.put("pga", "ar");
        map.put("pks", "sgn");
        map.put("prl", "sgn");
        map.put("prz", "sgn");
        map.put("psc", "sgn");
        map.put("psd", "sgn");
        map.put("pse", "ms");
        map.put("psg", "sgn");
        map.put("psl", "sgn");
        map.put("pso", "sgn");
        map.put("psp", "sgn");
        map.put("psr", "sgn");
        map.put("pys", "sgn");
        map.put("rms", "sgn");
        map.put("rsi", "sgn");
        map.put("rsl", "sgn");
        map.put("sdl", "sgn");
        map.put("sfb", "sgn");
        map.put("sfs", "sgn");
        map.put("sgg", "sgn");
        map.put("sgx", "sgn");
        map.put("shu", "ar");
        map.put("slf", "sgn");
        map.put("sls", "sgn");
        map.put("sqk", "sgn");
        map.put("sqs", "sgn");
        map.put("ssh", "ar");
        map.put("ssp", "sgn");
        map.put("ssr", "sgn");
        map.put("svk", "sgn");
        map.put("swc", "sw");
        map.put("swh", "sw");
        map.put("swl", "sgn");
        map.put("syy", "sgn");
        map.put("tmw", "ms");
        map.put("tse", "sgn");
        map.put("tsm", "sgn");
        map.put("tsq", "sgn");
        map.put("tss", "sgn");
        map.put("tsy", "sgn");
        map.put("tza", "sgn");
        map.put("ugn", "sgn");
        map.put("ugy", "sgn");
        map.put("ukl", "sgn");
        map.put("uks", "sgn");
        map.put("urk", "ms");
        map.put("uzn", "uz");
        map.put("uzs", "uz");
        map.put("vgt", "sgn");
        map.put("vkk", "ms");
        map.put("vkt", "ms");
        map.put("vsi", "sgn");
        map.put("vsl", "sgn");
        map.put("vsv", "sgn");
        map.put("wuu", "zh");
        map.put("xki", "sgn");
        map.put("xml", "sgn");
        map.put("xmm", "ms");
        map.put("xms", "sgn");
        map.put("yds", "sgn");
        map.put("ysl", "sgn");
        map.put("yue", "zh");
        map.put("zib", "sgn");
        map.put("zlm", "ms");
        map.put("zmi", "ms");
        map.put("zsl", "sgn");
        map.put("zsm", "ms");
        extlangData = map;
    }
}
