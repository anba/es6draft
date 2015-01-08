/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.intl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;

/**
 * Parser for BCP47 language tags
 * 
 * @see <a href="http://tools.ietf.org/html/rfc5646">RFC-5646</a>
 */
final class LanguageTagParser {
    private static final HashSet<String> irregular = set("en-gb-oed", "i-ami", "i-bnn",
            "i-default", "i-enochian", "i-hak", "i-klingon", "i-lux", "i-mingo", "i-navajo",
            "i-pwn", "i-tao", "i-tay", "i-tsu", "sgn-be-fr", "sgn-be-nl", "sgn-ch-de");

    private static final HashSet<String> regular = set("art-lojban", "cel-gaulish", "no-bok",
            "no-nyn", "zh-guoyu", "zh-hakka", "zh-min", "zh-min-nan", "zh-xiang");

    private static final HashSet<String> grandfathered = new HashSet<>();
    static {
        grandfathered.addAll(irregular);
        grandfathered.addAll(regular);
    }

    @SafeVarargs
    private static <T> HashSet<T> set(T... elements) {
        return new HashSet<>(Arrays.asList(elements));
    }

    private static final int NONE = 0b00, ALPHA = 0b01, DIGIT = 0b10, ALPHA_DIGIT = 0b11;

    private int token;
    private int tokenStart;
    private int tokenLength;

    private final String input;
    private final int length;
    private int pos;
    private boolean duplicateVariants = false;
    private boolean duplicateExtensions = false;

    private LanguageTag tag;

    public static final class LanguageTag {
        private final String input;
        private boolean grandfathered;

        private String language;
        private String extLang1, extLang2, extLang3;
        private String script;
        private String region;
        private HashSet<String> variants = null;
        private TreeMap<Character, String> extensions = null;
        private String privateuse;

        private LanguageTag(String input) {
            this.input = input;
        }

        /**
         * Canonicalization for language tags.
         * 
         * @return the canonicalized language tag
         * @see <a href="http://tools.ietf.org/html/rfc5646#section-4.5">RFC 5646</a>
         */
        public String canonicalize() {
            assert grandfathered || language != null || privateuse != null;
            if (grandfathered) {
                String r = LanguageSubtagRegistryData.grandfathered(input);
                assert r != null : "grandfathered with no canonicalized value";
                return r;
            }
            if (language == null) {
                return "x-" + privateuse;
            }

            String redundant = LanguageSubtagRegistryData.redundant(input);
            if (redundant != null) {
                // replace complete tag if redundant
                return redundant;
            }

            StringBuilder sb = new StringBuilder();
            String lang = LanguageSubtagRegistryData.language(language);
            sb.append(lang);
            if (extLang1 != null) {
                String ext = LanguageSubtagRegistryData.extlang(lang, extLang1);
                if (ext != null) {
                    // replaces language subtag
                    sb.setLength(0);
                    sb.append(ext);
                } else {
                    sb.append('-').append(extLang1);
                }
            }
            if (extLang2 != null) {
                sb.append('-').append(extLang2);
            }
            if (extLang3 != null) {
                sb.append('-').append(extLang3);
            }
            if (script != null) {
                // titlecase
                sb.append('-').append((char) (script.charAt(0) & ~0x20))
                        .append(script, 1, script.length());
            }
            if (region != null) {
                if (region.length() == 2) {
                    // uppercase
                    String reg = LanguageSubtagRegistryData.region(region);
                    sb.append('-').append((char) (reg.charAt(0) & ~0x20))
                            .append((char) (reg.charAt(1) & ~0x20));
                } else {
                    sb.append('-').append(region);
                }
            }
            if (variants != null) {
                for (String variant : variants) {
                    String var = LanguageSubtagRegistryData.variant(sb, variant);
                    if (var != null) {
                        // replace complete string
                        sb.setLength(0);
                        sb.append(var);
                    } else {
                        sb.append('-').append(variant);
                    }
                }
            }
            if (extensions != null) {
                // sorted by singleton
                for (Map.Entry<Character, String> entry : extensions.entrySet()) {
                    char singleton = entry.getKey().charValue();
                    String value = entry.getValue();
                    if (singleton == 'u') {
                        value = canonicalizeUnicodeExtension(value);
                    }
                    sb.append('-').append(singleton).append('-').append(value);
                }
            }
            if (privateuse != null) {
                sb.append("-x-").append(privateuse);
            }
            return sb.toString();
        }

        /**
         * Additional canonicalization for unicode extension sequences.
         * 
         * @param ext
         *            the unicode extension
         * @return the canonicalized unicode extension
         * @see <a href="http://tools.ietf.org/html/rfc6067#section-2.1.1">RFC 6067</a>
         */
        private String canonicalizeUnicodeExtension(String ext) {
            StringBuilder sb = new StringBuilder(ext.length() + 1);
            int start = 0, index = indexOf(ext, '-', start);
            if ((index - start) > 2) {
                // attributes
                ArrayList<String> attributes = new ArrayList<>(5);
                for (;;) {
                    attributes.add(ext.substring(start, index));
                    start = index + 1;
                    index = indexOf(ext, '-', start);
                    if ((index - start) <= 2) {
                        break;
                    }
                }
                appendSorted(sb, attributes);
            }
            if ((index - start) == 2) {
                // keywords
                ArrayList<String> keywords = new ArrayList<>(5);
                for (int keystart = start;;) {
                    start = index + 1;
                    index = indexOf(ext, '-', start);
                    if ((index - start) <= 2) {
                        keywords.add(ext.substring(keystart, start - 1));
                        if ((index - start) == 2) {
                            keystart = start;
                        } else {
                            break;
                        }
                    }
                }
                appendSorted(sb, keywords);
            }
            assert sb.length() == ext.length() + 1;
            sb.setLength(ext.length());
            return sb.toString();
        }

        private static final int indexOf(String s, int ch, int fromIndex) {
            int index = s.indexOf(ch, fromIndex);
            return (index < 0 ? s.length() : index);
        }

        private StringBuilder appendSorted(StringBuilder sb, ArrayList<String> list) {
            String[] ks = list.toArray(new String[list.size()]);
            Arrays.sort(ks);
            for (String s : ks) {
                sb.append(s).append('-');
            }
            return sb;
        }
    }

    public static LanguageTag parse(String locale) {
        String lower = toLowerASCIIOrNull(locale);
        if (lower == null) {
            // input contains invalid characters
            return null;
        }
        return new LanguageTagParser(lower).parse();
    }

    private LanguageTagParser(String input) {
        this.input = input;
        this.length = input.length();
    }

    private LanguageTag parse() {
        tag = new LanguageTag(input);
        rollback(0);
        if (languageTag() && pos >= length && token == NONE) {
            // successfully parsed the language tag, no characters left unparsed and the last token
            // was consumed as well
            return tag;
        }
        return null;
    }

    private void rollback(int p) {
        pos = p;
        next();
    }

    private void consume() {
        next();
    }

    private void next() {
        int tok = NONE, start = pos, len = 0;
        for (; pos < length; ++len) {
            char c = input.charAt(pos++);
            if (isAlpha(c)) {
                tok |= ALPHA;
            } else if (isDigit(c)) {
                tok |= DIGIT;
            } else {
                assert c == '-';
                break;
            }
        }
        token = tok;
        tokenStart = start;
        tokenLength = len;
        // System.out.printf("token=%d, tokenStart=%d, tokenLength=%d%n", token, tokenStart,
        // tokenLength);
    }

    private char tokenStartChar() {
        return input.charAt(tokenStart);
    }

    private String tokenString() {
        return input.substring(tokenStart, tokenStart + tokenLength);
    }

    private String consumeTokenString() {
        String s = tokenString();
        consume();
        return s;
    }

    private static String toLowerASCIIOrNull(String s) {
        int i = 0, len = s.length();
        if (len == 0 || s.charAt(len - 1) == '-') {
            // handle '-' as last character early to reduce additional efforts in next()
            return null;
        }
        lower: {
            for (; i < len; ++i) {
                char c = s.charAt(i);
                if (isUpper(c)) {
                    break lower;
                } else if (!(isAlpha(c) || isDigit(c) || c == '-')) {
                    return null;
                }
            }
            return s;
        }
        char[] ca = s.toCharArray();
        for (; i < len; ++i) {
            char c = ca[i];
            if (isUpper(c)) {
                ca[i] = (char) ('a' + (c - 'A'));
            } else if (!(isAlpha(c) || isDigit(c) || c == '-')) {
                return null;
            }
        }
        return String.valueOf(ca);
    }

    private static boolean isUpper(char c) {
        return ('A' <= c && c <= 'Z');
    }

    private static boolean isAlpha(char c) {
        return ('a' <= c && c <= 'z');
    }

    private static boolean isDigit(char c) {
        return ('0' <= c && c <= '9');
    }

    /**
     * <pre>
     *  Language-Tag  = langtag             ; normal language tags
     *                / privateuse          ; private use tag
     *                / grandfathered       ; grandfathered tags
     * </pre>
     * 
     * @return {@code true} if parsed successfully
     */
    private boolean languageTag() {
        return grandfathered() || privateuse() || langtag();
    }

    /**
     * <pre>
     *  grandfathered = irregular           ; non-redundant tags registered
     *                / regular             ; during the RFC 3066 era
     * 
     *  irregular     = "en-GB-oed"         ; irregular tags do not match
     *                / "i-ami"             ; the 'langtag' production and
     *                / "i-bnn"             ; would not otherwise be
     *                / "i-default"         ; considered 'well-formed'
     *                / "i-enochian"        ; These tags are all valid,
     *                / "i-hak"             ; but most are deprecated
     *                / "i-klingon"         ; in favor of more modern
     *                / "i-lux"             ; subtags or subtag
     *                / "i-mingo"           ; combination
     *                / "i-navajo"
     *                / "i-pwn"
     *                / "i-tao"
     *                / "i-tay"
     *                / "i-tsu"
     *                / "sgn-BE-FR"
     *                / "sgn-BE-NL"
     *                / "sgn-CH-DE"
     * 
     *  regular       = "art-lojban"        ; these tags match the 'langtag'
     *                / "cel-gaulish"       ; production, but their subtags
     *                / "no-bok"            ; are not extended language
     *                / "no-nyn"            ; or variant subtags: their meaning
     *                / "zh-guoyu"          ; is defined by their registration
     *                / "zh-hakka"          ; and all of these are deprecated
     *                / "zh-min"            ; in favor of a more modern
     *                / "zh-min-nan"        ; subtag or sequence of subtags
     *                / "zh-xiang"
     * </pre>
     * 
     * @return {@code true} if parsed successfully
     */
    private boolean grandfathered() {
        if (grandfathered.contains(input)) {
            tag.grandfathered = true;
            pos = length; // consume complete input
            token = NONE; // consume complete input
            return true;
        }
        return false;
    }

    /**
     * <pre>
     *  langtag       = language
     *                  ["-" script]
     *                  ["-" region]
     *                  *("-" variant)
     *                  *("-" extension)
     *                  ["-" privateuse]
     * </pre>
     * 
     * @return {@code true} if parsed successfully
     */
    private boolean langtag() {
        if (language()) {
            script();
            region();
            while (variant()) {
                if (duplicateVariants) {
                    return false;
                }
            }
            while (extension()) {
                if (duplicateExtensions) {
                    return false;
                }
            }
            privateuse();
            return true;
        }
        return false;
    }

    /**
     * <pre>
     *  language      = 2*3ALPHA            ; shortest ISO 639 code
     *                  ["-" extlang]       ; sometimes followed by
     *                                      ; extended language subtags
     *                / 4ALPHA              ; or reserved for future use
     *                / 5*8ALPHA            ; or registered language subtag
     * </pre>
     * 
     * @return {@code true} if parsed successfully
     */
    private boolean language() {
        if (token == ALPHA) {
            if (tokenLength >= 2 && tokenLength <= 3) {
                tag.language = consumeTokenString();
                extlang();
                return true;
            } else if (tokenLength >= 4 && tokenLength <= 8) {
                tag.language = consumeTokenString();
                return true;
            }
        }
        return false;
    }

    /**
     * <pre>
     *  extlang       = 3ALPHA              ; selected ISO 639 codes
     *                  *2("-" 3ALPHA)      ; permanently reserved
     * </pre>
     * 
     * @return {@code true} if parsed successfully
     */
    private boolean extlang() {
        if (token == ALPHA && tokenLength == 3) {
            tag.extLang1 = consumeTokenString();
            if (token == ALPHA && tokenLength == 3) {
                tag.extLang2 = consumeTokenString();
                if (token == ALPHA && tokenLength == 3) {
                    tag.extLang3 = consumeTokenString();
                }
            }
            return true;
        }
        return false;
    }

    /**
     * <pre>
     *  script        = 4ALPHA              ; ISO 15924 code
     * </pre>
     * 
     * @return {@code true} if parsed successfully
     */
    private boolean script() {
        if (token == ALPHA && tokenLength == 4) {
            tag.script = consumeTokenString();
            return true;
        }
        return false;
    }

    /**
     * <pre>
     *  region        = 2ALPHA              ; ISO 3166-1 code
     *                / 3DIGIT              ; UN M.49 code
     * </pre>
     * 
     * @return {@code true} if parsed successfully
     */
    private boolean region() {
        if ((token == ALPHA && tokenLength == 2) || (token == DIGIT && tokenLength == 3)) {
            tag.region = consumeTokenString();
            return true;
        }
        return false;
    }

    /**
     * <pre>
     *  variant       = 5*8alphanum         ; registered variants
     *                / (DIGIT 3alphanum)
     * </pre>
     * 
     * @return {@code true} if parsed successfully
     */
    private boolean variant() {
        if (alphanum() && tokenLength >= 5 && tokenLength <= 8) {
            storeVariant(consumeTokenString());
            return true;
        }
        if (alphanum() && tokenLength == 4 && isDigit(tokenStartChar())) {
            storeVariant(consumeTokenString());
            return true;
        }
        return false;
    }

    /**
     * <pre>
     *  extension     = singleton 1*("-" (2*8alphanum))
     * 
     *                                      ; Single alphanumerics
     *                                      ; "x" reserved for private use
     *  singleton     = DIGIT               ; 0 - 9
     *                / %x41-57             ; A - W
     *                / %x59-5A             ; Y - Z
     *                / %x61-77             ; a - w
     *                / %x79-7A             ; y - z
     * </pre>
     * 
     * @return {@code true} if parsed successfully
     */
    private boolean extension() {
        int saved = tokenStart;
        if (alphanum() && tokenLength == 1 && tokenStartChar() != 'x') {
            char singleton = tokenStartChar();
            consume();
            if (alphanum() && tokenLength >= 2 && tokenLength <= 8) {
                int startExtension = tokenStart;
                int len = tokenLength;
                consume();
                while (alphanum() && tokenLength >= 2 && tokenLength <= 8) {
                    len += tokenLength + 1; // token + separator
                    consume();
                }
                storeExtension(singleton, input.substring(startExtension, startExtension + len));
                return true;
            }
            rollback(saved);
        }
        return false;
    }

    /**
     * <pre>
     *  privateuse    = "x" 1*("-" (1*8alphanum))
     * </pre>
     * 
     * @return {@code true} if parsed successfully
     */
    private boolean privateuse() {
        int saved = tokenStart;
        if (token == ALPHA && tokenLength == 1 && tokenStartChar() == 'x') {
            consume();
            if (alphanum() && tokenLength >= 1 && tokenLength <= 8) {
                int startPrivateuse = tokenStart;
                consume();
                while (alphanum() && tokenLength >= 1 && tokenLength <= 8) {
                    consume();
                }
                // always last subtag, so just use the remaining string
                tag.privateuse = input.substring(startPrivateuse);
                return true;
            }
            rollback(saved);
        }
        return false;
    }

    /**
     * <pre>
     *  alphanum      = (ALPHA / DIGIT)     ; letters and numbers
     * </pre>
     * 
     * @return {@code true} if the current token is an alphanumeric character
     */
    private boolean alphanum() {
        return (token & ALPHA_DIGIT) != NONE;
    }

    private void storeVariant(String variant) {
        if (tag.variants == null) {
            tag.variants = new HashSet<>();
        }
        boolean changed = tag.variants.add(variant);
        duplicateVariants |= !changed;
    }

    private void storeExtension(char singleton, String value) {
        assert singleton != 'x';
        if (tag.extensions == null) {
            tag.extensions = new TreeMap<>();
        }
        boolean changed = tag.extensions.put(singleton, value) == null;
        duplicateExtensions |= !changed;
    }
}
