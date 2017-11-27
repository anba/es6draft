/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.regexp;

import java.lang.ref.SoftReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntPredicate;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.UnicodeSet;

/**
 * 
 */
final class UnicodeData {
    private UnicodeData() {
    }

    @SuppressWarnings("deprecation")
    private static final int BINARY_PROPERTY_LIMIT = UProperty.BINARY_LIMIT;
    @SuppressWarnings("deprecation")
    private static final int INT_PROPERTY_LIMIT = UProperty.INT_LIMIT;

    interface Property {
        String getName();

        static Property from(String name) {
            // Don't allow loose matching.
            int property;
            CHECK: try {
                property = UCharacter.getPropertyEnum(name);
                // Filter out synthetic names.
                if (property == UProperty.GENERAL_CATEGORY_MASK) {
                    return null;
                }
                String shortName = UCharacter.getPropertyName(property, UProperty.NameChoice.SHORT);
                if (shortName != null && shortName.equals(name)) {
                    break CHECK;
                }
                for (int i = 0;; ++i) {
                    String longName = UCharacter.getPropertyName(property, UProperty.NameChoice.LONG + i);
                    if (longName != null && longName.equals(name)) {
                        break CHECK;
                    }
                }
            } catch (IllegalArgumentException e) {
                return null;
            }
            if (property >= UProperty.BINARY_START && property < BINARY_PROPERTY_LIMIT) {
                return BinaryProperty.forId(property);
            }
            return EnumProperty.forId(property);
        }
    }

    enum BinaryProperty implements Property {
        Alphabetic(UProperty.ALPHABETIC),

        // "Any" special-cased in RegExpParser.

        // TODO: spec issue - /\p{ASCII}/ui.test("\u212A") - do we still want unicode-ignoreCase behaviour here?
        // "ASCII" special-cased in RegExpParser.

        ASCII_Hex_Digit(UProperty.ASCII_HEX_DIGIT),

        // "Assigned" special-cased in RegExpParser.

        Bidi_Control(UProperty.BIDI_CONTROL),

        Bidi_Mirrored(UProperty.BIDI_MIRRORED),

        Case_Ignoreable(UProperty.CASE_IGNORABLE),

        Cased(UProperty.CASED),

        Changes_When_Casefolded(UProperty.CHANGES_WHEN_CASEFOLDED),

        Changes_When_Casemapped(UProperty.CHANGES_WHEN_CASEMAPPED),

        Changes_When_Lowercased(UProperty.CHANGES_WHEN_LOWERCASED),

        Changes_When_NFKC_Casefolded(UProperty.CHANGES_WHEN_NFKC_CASEFOLDED),

        Changes_When_Titlecased(UProperty.CHANGES_WHEN_TITLECASED),

        Changes_When_Uppercased(UProperty.CHANGES_WHEN_UPPERCASED),

        Dash(UProperty.DASH),

        Default_Ignorable_Code_Point(UProperty.DEFAULT_IGNORABLE_CODE_POINT),

        Deprecated(UProperty.DEPRECATED),

        Diacritic(UProperty.DIACRITIC),

        Emoji(UProperty.EMOJI),

        Emoji_Component(UProperty.EMOJI_COMPONENT),

        Emoji_Modifier_Base(UProperty.EMOJI_MODIFIER_BASE),

        Emoji_Modifier(UProperty.EMOJI_MODIFIER),

        Emoji_Presentation(UProperty.EMOJI_PRESENTATION),

        Extender(UProperty.EXTENDER),

        Grapheme_Base(UProperty.GRAPHEME_BASE),

        Grapheme_Extend(UProperty.GRAPHEME_EXTEND),

        Hex_Digit(UProperty.HEX_DIGIT),

        ID_Continue(UProperty.ID_CONTINUE),

        ID_Start(UProperty.ID_START),

        Ideographic(UProperty.IDEOGRAPHIC),

        IDS_Binary_Operator(UProperty.IDS_BINARY_OPERATOR),

        IDS_Trinary_Operator(UProperty.IDS_TRINARY_OPERATOR),

        Join_Control(UProperty.JOIN_CONTROL),

        Logical_Order_Exception(UProperty.LOGICAL_ORDER_EXCEPTION),

        Lowercase(UProperty.LOWERCASE),

        Math(UProperty.MATH),

        Noncharacter_Code_Point(UProperty.NONCHARACTER_CODE_POINT),

        Pattern_Syntax(UProperty.PATTERN_SYNTAX),

        Pattern_White_Space(UProperty.PATTERN_WHITE_SPACE),

        Quotation_Mark(UProperty.QUOTATION_MARK),

        Radical(UProperty.RADICAL),

        Regional_Indicator(UProperty.REGIONAL_INDICATOR),

        Sentence_Terminal(UProperty.S_TERM),

        Soft_Dotted(UProperty.SOFT_DOTTED),

        Terminal_Punctuation(UProperty.TERMINAL_PUNCTUATION),

        Unified_Ideograph(UProperty.UNIFIED_IDEOGRAPH),

        Uppercase(UProperty.UPPERCASE),

        Variation_Selector(UProperty.VARIATION_SELECTOR),

        White_Space(UProperty.WHITE_SPACE),

        XID_Continue(UProperty.XID_CONTINUE),

        XID_Start(UProperty.XID_START),

        ;

        private final int propertyId;
        private SoftReference<int[]> refRange;

        private BinaryProperty(int propertyId) {
            assert propertyId >= UProperty.BINARY_START && propertyId < BINARY_PROPERTY_LIMIT;
            this.propertyId = propertyId;
        }

        @Override
        public String getName() {
            return UCharacter.getPropertyName(propertyId, UProperty.NameChoice.LONG);
        }

        public boolean has(int codePoint) {
            return UCharacter.hasBinaryProperty(codePoint, propertyId);
        }

        protected UnicodeSet set() {
            UnicodeSet set = new UnicodeSet();
            set.applyIntPropertyValue(propertyId, 1); // 1 = True
            return set;
        }

        public IntPredicate predicate() {
            UnicodeSet set = set();
            set.freeze();
            return set::contains;
        }

        public int[] range() {
            SoftReference<int[]> ref = refRange;
            int[] range = ref != null ? ref.get() : null;
            if (range == null) {
                refRange = new SoftReference<int[]>(range = codeRange(set()));
            }
            return range;
        }

        static BinaryProperty forId(int propertyId) {
            if (propertyId >= UProperty.BINARY_START && propertyId < BINARY_PROPERTY_LIMIT) {
                for (BinaryProperty binary : BinaryProperty.values()) {
                    if (binary.propertyId == propertyId) {
                        return binary;
                    }
                }
            }
            return null;
        }
    }

    enum EnumProperty implements Property {
        // Required enumeration properties.

        General_Category(UProperty.GENERAL_CATEGORY_MASK) {
            @Override
            public String getName() {
                return UCharacter.getPropertyName(UProperty.GENERAL_CATEGORY, UProperty.NameChoice.LONG);
            }

            @Override
            public boolean has(int codePoint, int value) {
                return ((1 << UCharacter.getType(codePoint)) & value) != 0;
            }
        },

        Script(UProperty.SCRIPT) {
            @Override
            public boolean has(int codePoint, int value) {
                // See https://ssl.icu-project.org/trac/ticket/13462
                switch (codePoint) {
                case 0x3000:
                case 0x3004:
                case 0x3012:
                case 0x3020:
                case 0x3036:
                    return value == UScript.COMMON;
                }
                return super.has(codePoint, value);
            }

            @Override
            protected UnicodeSet set(int value) {
                // See https://ssl.icu-project.org/trac/ticket/13462
                UnicodeSet set = super.set(value);
                if (value == UScript.COMMON) {
                    set.add(0x3000);
                    set.add(0x3004);
                    set.add(0x3012);
                    set.add(0x3020);
                    set.add(0x3036);
                }
                return set;
            }
        },

        Script_Extensions(UProperty.SCRIPT_EXTENSIONS) {
            @Override
            public int getValue(String value) {
                return Script.getValue(value);
            }

            @Override
            public boolean isValue(String value) {
                return Script.isValue(value);
            }

            @Override
            public boolean has(int codePoint, int value) {
                // See https://ssl.icu-project.org/trac/ticket/13462
                switch (codePoint) {
                case 0x3000:
                case 0x3004:
                case 0x3012:
                case 0x3020:
                case 0x3036:
                    return value == UScript.COMMON;
                }
                return UScript.hasScript(codePoint, value);
            }

            @Override
            protected UnicodeSet set(int value) {
                // See https://ssl.icu-project.org/trac/ticket/13462
                UnicodeSet set = super.set(value);
                switch (value) {
                case UScript.COMMON:
                    set.add(0x3000);
                    set.add(0x3004);
                    set.add(0x3012);
                    set.add(0x3020);
                    set.add(0x3036);
                    break;
                case UScript.BOPOMOFO:
                case UScript.HAN:
                case UScript.HANGUL:
                case UScript.HIRAGANA:
                case UScript.KATAKANA:
                case UScript.YI:
                    set.remove(0x3000);
                    set.remove(0x3004);
                    set.remove(0x3012);
                    set.remove(0x3020);
                    set.remove(0x3036);
                    break;
                }
                return set;
            }
        };

        private final int propertyId;
        private final ConcurrentHashMap<Integer, SoftReference<int[]>> ranges = new ConcurrentHashMap<>();

        private EnumProperty(int propertyId) {
            assert (propertyId >= UProperty.INT_START && propertyId < INT_PROPERTY_LIMIT)
                    || propertyId == UProperty.GENERAL_CATEGORY_MASK || propertyId == UProperty.SCRIPT_EXTENSIONS;
            this.propertyId = propertyId;
        }

        @Override
        public String getName() {
            return UCharacter.getPropertyName(propertyId, UProperty.NameChoice.LONG);
        }

        public boolean isValue(String valueAlias) {
            // Don't allow loose matching.
            try {
                int value = UCharacter.getPropertyValueEnum(propertyId, valueAlias);
                String shortName = UCharacter.getPropertyValueName(propertyId, value, UProperty.NameChoice.SHORT);
                if (shortName != null && shortName.equals(valueAlias)) {
                    return true;
                }
                for (int i = 0;; ++i) {
                    String longName = UCharacter.getPropertyValueName(propertyId, value, UProperty.NameChoice.LONG + i);
                    if (longName != null && longName.equals(valueAlias)) {
                        return true;
                    }
                }
            } catch (IllegalArgumentException e) {
                return false;
            }
        }

        public int getValue(String value) {
            return UCharacter.getPropertyValueEnum(propertyId, value);
        }

        public boolean has(int codePoint, int value) {
            return UCharacter.getIntPropertyValue(codePoint, propertyId) == value;
        }

        protected UnicodeSet set(int value) {
            UnicodeSet set = new UnicodeSet();
            set.applyIntPropertyValue(propertyId, value);
            return set;
        }

        public final IntPredicate predicate(String value) {
            UnicodeSet set = set(getValue(value));
            set.freeze();
            return set::contains;
        }

        public final int[] range(int value) {
            SoftReference<int[]> ref = ranges.get(value);
            int[] range = ref != null ? ref.get() : null;
            if (range == null) {
                ranges.put(value, new SoftReference<>(range = codeRange(set(value))));
            }
            return range;
        }

        static EnumProperty forId(int propertyId) {
            if ((propertyId >= UProperty.INT_START && propertyId < INT_PROPERTY_LIMIT)
                    || propertyId == UProperty.GENERAL_CATEGORY_MASK || propertyId == UProperty.SCRIPT_EXTENSIONS) {
                if (propertyId == UProperty.GENERAL_CATEGORY) {
                    return General_Category;
                }
                for (EnumProperty e : EnumProperty.values()) {
                    if (e.propertyId == propertyId) {
                        return e;
                    }
                }
            }
            return null;
        }
    }

    private static int[] codeRange(UnicodeSet set) {
        // codeRange = { numRanges, (start..end)* }
        int rangeCount = set.getRangeCount();
        int[] codeRange = new int[1 + rangeCount * 2];
        codeRange[0] = rangeCount;
        for (int i = 0, j = 1; i < rangeCount; ++i) {
            codeRange[j++] = set.getRangeStart(i);
            codeRange[j++] = set.getRangeEnd(i);
        }
        return codeRange;
    }
}
