/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.regexp;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.github.anba.es6draft.regexp.UnicodeData.EnumProperty;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;

/**
 * 
 */
public final class UnicodeDataTest {
    @SuppressWarnings("deprecation")
    @Test
    public void testLimits() {
        // integer valued properties
        for (int p = UProperty.INT_START; p < UProperty.INT_LIMIT; ++p) {
            int min = UCharacter.getIntPropertyMinValue(p);
            int max = UCharacter.getIntPropertyMaxValue(p);

            assertTrue(String.format("min=%d", min), min >= 0);
            assertTrue(String.format("min=%d, max=%d", min, max), min <= max);
            assertTrue(String.format("max=%d", max), max < 512); // BINARY_MASK in UEncoding
        }
    }

    @Test
    public void testLimitsGeneralCategoryMask() {
        int min = UCharacter.getIntPropertyMinValue(UProperty.GENERAL_CATEGORY);
        int max = UCharacter.getIntPropertyMaxValue(UProperty.GENERAL_CATEGORY);
        for (int i = min; i <= max; ++i) {
            String name = UCharacter.getPropertyValueName(UProperty.GENERAL_CATEGORY, i, UProperty.NameChoice.SHORT);
            int maskValue = UCharacter.getPropertyValueEnum(UProperty.GENERAL_CATEGORY_MASK, name);
            assertTrue(String.format("%s -> %d", name, maskValue), Integer.numberOfLeadingZeros(maskValue) >= 2);
        }

        for (String name : new String[] { "C", "L", "LC", "M", "N", "P", "S", "Z" }) {
            int maskValue = UCharacter.getPropertyValueEnum(UProperty.GENERAL_CATEGORY_MASK, name);
            assertTrue(String.format("%s -> %d", name, maskValue), Integer.numberOfLeadingZeros(maskValue) >= 2);
        }
    }

    @Test
    public void testGeneralCategory() {
        // http://www.unicode.org/reports/tr44/tr44-16.html#GC_Values_Table

        assertFalse(EnumProperty.General_Category.isValue(""));

        assertTrue(EnumProperty.General_Category.isValue("Lu"));
        assertFalse(EnumProperty.General_Category.isValue("lu"));
        assertTrue(EnumProperty.General_Category.isValue("Uppercase_Letter"));

        assertTrue(EnumProperty.General_Category.isValue("Ll"));
        assertTrue(EnumProperty.General_Category.isValue("Lowercase_Letter"));

        assertTrue(EnumProperty.General_Category.isValue("Lt"));
        assertTrue(EnumProperty.General_Category.isValue("Titlecase_Letter"));

        assertTrue(EnumProperty.General_Category.isValue("LC"));
        assertTrue(EnumProperty.General_Category.isValue("Cased_Letter"));

        assertTrue(EnumProperty.General_Category.isValue("Lm"));
        assertTrue(EnumProperty.General_Category.isValue("Modifier_Letter"));

        assertTrue(EnumProperty.General_Category.isValue("Lo"));
        assertTrue(EnumProperty.General_Category.isValue("Other_Letter"));

        assertTrue(EnumProperty.General_Category.isValue("L"));
        assertTrue(EnumProperty.General_Category.isValue("Letter"));

        assertTrue(EnumProperty.General_Category.isValue("Mn"));
        assertTrue(EnumProperty.General_Category.isValue("Nonspacing_Mark"));

        assertTrue(EnumProperty.General_Category.isValue("Mc"));
        assertTrue(EnumProperty.General_Category.isValue("Spacing_Mark"));

        assertTrue(EnumProperty.General_Category.isValue("Me"));
        assertTrue(EnumProperty.General_Category.isValue("Enclosing_Mark"));

        assertTrue(EnumProperty.General_Category.isValue("M"));
        assertTrue(EnumProperty.General_Category.isValue("Mark"));

        assertTrue(EnumProperty.General_Category.isValue("Nd"));
        assertTrue(EnumProperty.General_Category.isValue("Decimal_Number"));

        assertTrue(EnumProperty.General_Category.isValue("Nl"));
        assertTrue(EnumProperty.General_Category.isValue("Letter_Number"));

        assertTrue(EnumProperty.General_Category.isValue("No"));
        assertTrue(EnumProperty.General_Category.isValue("Other_Number"));

        assertTrue(EnumProperty.General_Category.isValue("N"));
        assertTrue(EnumProperty.General_Category.isValue("Number"));

        assertTrue(EnumProperty.General_Category.isValue("Pc"));
        assertTrue(EnumProperty.General_Category.isValue("Connector_Punctuation"));

        assertTrue(EnumProperty.General_Category.isValue("Pd"));
        assertTrue(EnumProperty.General_Category.isValue("Dash_Punctuation"));

        assertTrue(EnumProperty.General_Category.isValue("Ps"));
        assertTrue(EnumProperty.General_Category.isValue("Open_Punctuation"));

        assertTrue(EnumProperty.General_Category.isValue("Pe"));
        assertTrue(EnumProperty.General_Category.isValue("Close_Punctuation"));

        assertTrue(EnumProperty.General_Category.isValue("Pi"));
        assertTrue(EnumProperty.General_Category.isValue("Initial_Punctuation"));

        assertTrue(EnumProperty.General_Category.isValue("Pf"));
        assertTrue(EnumProperty.General_Category.isValue("Final_Punctuation"));

        assertTrue(EnumProperty.General_Category.isValue("Po"));
        assertTrue(EnumProperty.General_Category.isValue("Other_Punctuation"));

        assertTrue(EnumProperty.General_Category.isValue("P"));
        assertTrue(EnumProperty.General_Category.isValue("Punctuation"));

        assertTrue(EnumProperty.General_Category.isValue("Sm"));
        assertTrue(EnumProperty.General_Category.isValue("Math_Symbol"));

        assertTrue(EnumProperty.General_Category.isValue("Sc"));
        assertTrue(EnumProperty.General_Category.isValue("Currency_Symbol"));

        assertTrue(EnumProperty.General_Category.isValue("Sk"));
        assertTrue(EnumProperty.General_Category.isValue("Modifier_Symbol"));

        assertTrue(EnumProperty.General_Category.isValue("So"));
        assertTrue(EnumProperty.General_Category.isValue("Other_Symbol"));

        assertTrue(EnumProperty.General_Category.isValue("S"));
        assertTrue(EnumProperty.General_Category.isValue("Symbol"));

        assertTrue(EnumProperty.General_Category.isValue("Zs"));
        assertTrue(EnumProperty.General_Category.isValue("Space_Separator"));

        assertTrue(EnumProperty.General_Category.isValue("Zl"));
        assertTrue(EnumProperty.General_Category.isValue("Line_Separator"));

        assertTrue(EnumProperty.General_Category.isValue("Zp"));
        assertTrue(EnumProperty.General_Category.isValue("Paragraph_Separator"));

        assertTrue(EnumProperty.General_Category.isValue("Z"));
        assertTrue(EnumProperty.General_Category.isValue("Separator"));

        assertTrue(EnumProperty.General_Category.isValue("Cc"));
        assertTrue(EnumProperty.General_Category.isValue("Control"));

        assertTrue(EnumProperty.General_Category.isValue("Cf"));
        assertTrue(EnumProperty.General_Category.isValue("Format"));

        assertTrue(EnumProperty.General_Category.isValue("Cs"));
        assertTrue(EnumProperty.General_Category.isValue("Surrogate"));

        assertTrue(EnumProperty.General_Category.isValue("Co"));
        assertTrue(EnumProperty.General_Category.isValue("Private_Use"));

        assertTrue(EnumProperty.General_Category.isValue("Cn"));
        assertTrue(EnumProperty.General_Category.isValue("Unassigned"));

        assertTrue(EnumProperty.General_Category.isValue("C"));
        assertTrue(EnumProperty.General_Category.isValue("Other"));
    }

    static boolean isBinaryProperty(String name) {
        UnicodeData.Property property = UnicodeData.Property.from(name);
        return property instanceof UnicodeData.BinaryProperty;
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testAllICUBinaryProperties() {
        for (int p = UProperty.BINARY_START; p < UProperty.BINARY_LIMIT; ++p) {
            String shortName = UCharacter.getPropertyName(p, UProperty.NameChoice.SHORT);
            if (shortName != null) {
                // Does not throw.
                isBinaryProperty(shortName);
            }
            String longName = UCharacter.getPropertyName(p, UProperty.NameChoice.LONG);
            if (longName != null) {
                // Does not throw.
                isBinaryProperty(longName);
            }
        }
    }

    @Test
    public void testBinaryPropertyUnsupported() {
        // Not supported in ICU4J through UProperty.

        assertFalse(isBinaryProperty("CE"));
        assertFalse(isBinaryProperty("Composition_Exclusion"));

        assertFalse(isBinaryProperty("OAlpha"));
        assertFalse(isBinaryProperty("Other_Alphabetic"));

        assertFalse(isBinaryProperty("ODI"));
        assertFalse(isBinaryProperty("Other_Default_Ignorable_Code_Point"));

        assertFalse(isBinaryProperty("OGr_Ext"));
        assertFalse(isBinaryProperty("Other_Grapheme_Extend"));

        assertFalse(isBinaryProperty("OIDC"));
        assertFalse(isBinaryProperty("Other_ID_Continue"));

        assertFalse(isBinaryProperty("OIDS"));
        assertFalse(isBinaryProperty("Other_ID_Start"));

        assertFalse(isBinaryProperty("OLower"));
        assertFalse(isBinaryProperty("Other_Lowercase"));

        assertFalse(isBinaryProperty("OMath"));
        assertFalse(isBinaryProperty("Other_Math"));

        assertFalse(isBinaryProperty("OUpper"));
        assertFalse(isBinaryProperty("Other_Uppercase"));

        assertFalse(isBinaryProperty("PCM"));
        assertFalse(isBinaryProperty("Prepended_Concatenation_Mark"));

        assertFalse(isBinaryProperty("XO_NFC"));
        assertFalse(isBinaryProperty("Expands_On_NFC"));

        assertFalse(isBinaryProperty("XO_NFD"));
        assertFalse(isBinaryProperty("Expands_On_NFD"));

        assertFalse(isBinaryProperty("XO_NFKC"));
        assertFalse(isBinaryProperty("Expands_On_NFKC"));

        assertFalse(isBinaryProperty("XO_NFKD"));
        assertFalse(isBinaryProperty("Expands_On_NFKD"));

        // Intentionally not supported.

        assertFalse(isBinaryProperty("Gr_Link"));
        assertFalse(isBinaryProperty("Grapheme_Link"));
        assertFalse(isBinaryProperty("gr_link"));
        assertFalse(isBinaryProperty("grapheme_link"));

        assertFalse(isBinaryProperty("Hyphen"));
        assertFalse(isBinaryProperty("hyphen"));

        assertFalse(isBinaryProperty("Comp_Ex"));
        assertFalse(isBinaryProperty("Full_Composition_Exclusion"));
        assertFalse(isBinaryProperty("comp_ex"));
        assertFalse(isBinaryProperty("full_composition_exclusion"));
    }

    @Test
    public void testBinaryPropertyPosix() {
        // http://unicode.org/reports/tr18/#Compatibility_Properties
        assertFalse(isBinaryProperty("alpha"));
        assertFalse(isBinaryProperty("lower"));
        assertFalse(isBinaryProperty("upper"));
        assertFalse(isBinaryProperty("punct"));
        assertFalse(isBinaryProperty("digit"));
        assertFalse(isBinaryProperty("d"));
        assertFalse(isBinaryProperty("xdigit"));
        assertFalse(isBinaryProperty("alnum"));
        // "space" is also defined in PropertyAliases.txt
        // assertFalse(isBinaryProperty("space"));
        assertFalse(isBinaryProperty("s"));
        assertFalse(isBinaryProperty("blank"));
        assertFalse(isBinaryProperty("cntrl"));
        assertFalse(isBinaryProperty("graph"));
        assertFalse(isBinaryProperty("print"));
        assertFalse(isBinaryProperty("word"));
        assertFalse(isBinaryProperty("w"));
        assertFalse(isBinaryProperty("X"));
        assertFalse(isBinaryProperty("b"));
    }

    @Test
    public void testBinaryPropertyICU() {
        // ICU specific binary properties.

        assertFalse(isBinaryProperty("Sensitive"));
        assertFalse(isBinaryProperty("sensitive"));
        assertFalse(isBinaryProperty("Case_Sensitive"));
        assertFalse(isBinaryProperty("case_sensitive"));
        assertFalse(isBinaryProperty("casesensitive"));

        assertFalse(isBinaryProperty("nfdinert"));
        assertFalse(isBinaryProperty("NFD_Inert"));

        assertFalse(isBinaryProperty("nfkdinert"));
        assertFalse(isBinaryProperty("NFKD_Inert"));

        assertFalse(isBinaryProperty("nfcinert"));
        assertFalse(isBinaryProperty("NFC_Inert"));

        assertFalse(isBinaryProperty("nfkcinert"));
        assertFalse(isBinaryProperty("NFKC_Inert"));

        assertFalse(isBinaryProperty("segstart"));
        assertFalse(isBinaryProperty("Segment_Starter"));
    }

    @Test
    public void testBinaryPropertyWithAliases() {
        // Binary properties listed in PropertyAliases.txt.

        assertTrue(isBinaryProperty("AHex"));
        assertTrue(isBinaryProperty("ASCII_Hex_Digit"));
        assertFalse(isBinaryProperty("ahex"));
        assertFalse(isBinaryProperty("ASCII_hex_digit"));
        assertFalse(isBinaryProperty("ascii_hex_digit"));
        assertFalse(isBinaryProperty("ascii_hexdigit"));
        assertFalse(isBinaryProperty("asciihexdigit"));

        assertTrue(isBinaryProperty("Alpha"));
        assertTrue(isBinaryProperty("Alphabetic"));
        assertFalse(isBinaryProperty("alpha"));
        assertFalse(isBinaryProperty("alphabetic"));

        assertTrue(isBinaryProperty("Bidi_C"));
        assertTrue(isBinaryProperty("Bidi_Control"));
        assertFalse(isBinaryProperty("bidi_c"));
        assertFalse(isBinaryProperty("bidic"));
        assertFalse(isBinaryProperty("bidi_control"));
        assertFalse(isBinaryProperty("bidicontrol"));

        assertTrue(isBinaryProperty("Bidi_M"));
        assertTrue(isBinaryProperty("Bidi_Mirrored"));
        assertFalse(isBinaryProperty("bidi_m"));
        assertFalse(isBinaryProperty("bidim"));
        assertFalse(isBinaryProperty("bidi_mirrored"));
        assertFalse(isBinaryProperty("bidimirrored"));

        assertTrue(isBinaryProperty("Cased"));
        assertFalse(isBinaryProperty("cased"));

        assertTrue(isBinaryProperty("CI"));
        assertTrue(isBinaryProperty("Case_Ignorable"));
        assertFalse(isBinaryProperty("ci"));
        assertFalse(isBinaryProperty("case_ignorable"));

        assertTrue(isBinaryProperty("CWCF"));
        assertTrue(isBinaryProperty("Changes_When_Casefolded"));
        assertFalse(isBinaryProperty("cwcf"));
        assertFalse(isBinaryProperty("changes_when_casefolded"));

        assertTrue(isBinaryProperty("CWCM"));
        assertTrue(isBinaryProperty("Changes_When_Casemapped"));
        assertFalse(isBinaryProperty("cwcm"));
        assertFalse(isBinaryProperty("changes_when_casemapped"));

        assertTrue(isBinaryProperty("CWKCF"));
        assertTrue(isBinaryProperty("Changes_When_NFKC_Casefolded"));
        assertFalse(isBinaryProperty("cwkcf"));
        assertFalse(isBinaryProperty("changes_when_NFKC_casefolded"));
        assertFalse(isBinaryProperty("changes_when_nfkc_casefolded"));

        assertTrue(isBinaryProperty("CWL"));
        assertTrue(isBinaryProperty("Changes_When_Lowercased"));
        assertFalse(isBinaryProperty("cwl"));
        assertFalse(isBinaryProperty("changes_when_lowercased"));

        assertTrue(isBinaryProperty("CWT"));
        assertTrue(isBinaryProperty("Changes_When_Titlecased"));
        assertFalse(isBinaryProperty("cwt"));
        assertFalse(isBinaryProperty("changes_when_titlecased"));

        assertTrue(isBinaryProperty("CWU"));
        assertTrue(isBinaryProperty("Changes_When_Uppercased"));
        assertFalse(isBinaryProperty("cwu"));
        assertFalse(isBinaryProperty("changes_when_uppercased"));

        assertTrue(isBinaryProperty("Dash"));
        assertFalse(isBinaryProperty("dash"));

        assertTrue(isBinaryProperty("Dep"));
        assertTrue(isBinaryProperty("Deprecated"));
        assertFalse(isBinaryProperty("dep"));
        assertFalse(isBinaryProperty("deprecated"));

        assertTrue(isBinaryProperty("DI"));
        assertTrue(isBinaryProperty("Default_Ignorable_Code_Point"));
        assertFalse(isBinaryProperty("di"));
        assertFalse(isBinaryProperty("default_ignorable_code_point"));
        assertFalse(isBinaryProperty("default_ignorable_codepoint"));

        assertTrue(isBinaryProperty("Dia"));
        assertTrue(isBinaryProperty("Diacritic"));
        assertFalse(isBinaryProperty("dia"));
        assertFalse(isBinaryProperty("diacritic"));

        assertTrue(isBinaryProperty("Ext"));
        assertTrue(isBinaryProperty("Extender"));
        assertFalse(isBinaryProperty("ext"));
        assertFalse(isBinaryProperty("extender"));

        assertTrue(isBinaryProperty("Gr_Base"));
        assertTrue(isBinaryProperty("Grapheme_Base"));
        assertFalse(isBinaryProperty("gr_base"));
        assertFalse(isBinaryProperty("grapheme_base"));

        assertTrue(isBinaryProperty("Gr_Ext"));
        assertTrue(isBinaryProperty("Grapheme_Extend"));
        assertFalse(isBinaryProperty("gr_ext"));
        assertFalse(isBinaryProperty("grapheme_extend"));

        assertTrue(isBinaryProperty("Hex"));
        assertTrue(isBinaryProperty("Hex_Digit"));
        assertFalse(isBinaryProperty("hex"));
        assertFalse(isBinaryProperty("hex_digit"));
        assertFalse(isBinaryProperty("hexdigit"));

        assertTrue(isBinaryProperty("IDC"));
        assertTrue(isBinaryProperty("ID_Continue"));
        assertFalse(isBinaryProperty("idc"));
        assertFalse(isBinaryProperty("ID_continue"));
        assertFalse(isBinaryProperty("id_continue"));
        assertFalse(isBinaryProperty("idcontinue"));

        assertTrue(isBinaryProperty("Ideo"));
        assertTrue(isBinaryProperty("Ideographic"));
        assertFalse(isBinaryProperty("ideo"));
        assertFalse(isBinaryProperty("ideographic"));

        assertTrue(isBinaryProperty("IDS"));
        assertTrue(isBinaryProperty("ID_Start"));
        assertFalse(isBinaryProperty("ids"));
        assertFalse(isBinaryProperty("ID_start"));
        assertFalse(isBinaryProperty("id_start"));
        assertFalse(isBinaryProperty("idstart"));

        assertTrue(isBinaryProperty("IDSB"));
        assertTrue(isBinaryProperty("IDS_Binary_Operator"));
        assertFalse(isBinaryProperty("idsb"));
        assertFalse(isBinaryProperty("IDS_binary_operator"));
        assertFalse(isBinaryProperty("ids_binary_operator"));
        assertFalse(isBinaryProperty("ids_binaryoperator"));
        assertFalse(isBinaryProperty("idsbinaryoperator"));

        assertTrue(isBinaryProperty("IDST"));
        assertTrue(isBinaryProperty("IDS_Trinary_Operator"));
        assertFalse(isBinaryProperty("idst"));
        assertFalse(isBinaryProperty("IDS_trinary_operator"));
        assertFalse(isBinaryProperty("ids_trinary_operator"));
        assertFalse(isBinaryProperty("ids_trinaryoperator"));
        assertFalse(isBinaryProperty("idstrinaryoperator"));

        assertTrue(isBinaryProperty("Join_C"));
        assertTrue(isBinaryProperty("Join_Control"));
        assertFalse(isBinaryProperty("join_c"));
        assertFalse(isBinaryProperty("joinc"));
        assertFalse(isBinaryProperty("join_control"));
        assertFalse(isBinaryProperty("joincontrol"));

        assertTrue(isBinaryProperty("LOE"));
        assertTrue(isBinaryProperty("Logical_Order_Exception"));
        assertFalse(isBinaryProperty("loe"));
        assertFalse(isBinaryProperty("logical_order_exception"));
        assertFalse(isBinaryProperty("logicalorderexception"));

        assertTrue(isBinaryProperty("Lower"));
        assertTrue(isBinaryProperty("Lowercase"));
        assertFalse(isBinaryProperty("lower"));
        assertFalse(isBinaryProperty("lowercase"));

        assertTrue(isBinaryProperty("Math"));
        assertFalse(isBinaryProperty("math"));

        assertTrue(isBinaryProperty("NChar"));
        assertTrue(isBinaryProperty("Noncharacter_Code_Point"));
        assertFalse(isBinaryProperty("nchar"));
        assertFalse(isBinaryProperty("Noncharacter_Codepoint"));
        assertFalse(isBinaryProperty("noncharacter_Code_Point"));
        assertFalse(isBinaryProperty("Noncharacter_code_point"));
        assertFalse(isBinaryProperty("noncharacter_code_point"));
        assertFalse(isBinaryProperty("noncharacter_codepoint"));
        assertFalse(isBinaryProperty("noncharactercodepoint"));

        assertTrue(isBinaryProperty("Pat_Syn"));
        assertTrue(isBinaryProperty("Pattern_Syntax"));
        assertFalse(isBinaryProperty("pat_syn"));
        assertFalse(isBinaryProperty("patsyn"));
        assertFalse(isBinaryProperty("pattern_syntax"));
        assertFalse(isBinaryProperty("patternsyntax"));

        assertTrue(isBinaryProperty("Pat_WS"));
        assertTrue(isBinaryProperty("Pattern_White_Space"));
        assertFalse(isBinaryProperty("pat_ws"));
        assertFalse(isBinaryProperty("patws"));
        assertFalse(isBinaryProperty("pattern_white_space"));
        assertFalse(isBinaryProperty("pattern_whitespace"));
        assertFalse(isBinaryProperty("patternwhitespace"));

        assertTrue(isBinaryProperty("QMark"));
        assertTrue(isBinaryProperty("Quotation_Mark"));
        assertFalse(isBinaryProperty("qmark"));
        assertFalse(isBinaryProperty("Qmark"));
        assertFalse(isBinaryProperty("quotation_mark"));
        assertFalse(isBinaryProperty("quotationmark"));
        assertFalse(isBinaryProperty("Quotationmark"));
        assertFalse(isBinaryProperty("QuotationMark"));

        assertTrue(isBinaryProperty("Radical"));
        assertFalse(isBinaryProperty("radical"));

        assertTrue(isBinaryProperty("SD"));
        assertTrue(isBinaryProperty("Soft_Dotted"));
        assertFalse(isBinaryProperty("sd"));
        assertFalse(isBinaryProperty("soft_dotted"));
        assertFalse(isBinaryProperty("softdotted"));

        assertTrue(isBinaryProperty("STerm"));
        assertTrue(isBinaryProperty("Sentence_Terminal"));
        assertFalse(isBinaryProperty("Sterm"));
        assertFalse(isBinaryProperty("sterm"));

        assertTrue(isBinaryProperty("Term"));
        assertTrue(isBinaryProperty("Terminal_Punctuation"));
        assertFalse(isBinaryProperty("term"));
        assertFalse(isBinaryProperty(""));

        assertTrue(isBinaryProperty("UIdeo"));
        assertTrue(isBinaryProperty("Unified_Ideograph"));
        assertFalse(isBinaryProperty("uideo"));
        assertFalse(isBinaryProperty("unified_ideograph"));
        assertFalse(isBinaryProperty("unifiedideograph"));
        assertFalse(isBinaryProperty("UnifiedIdeograph"));

        assertTrue(isBinaryProperty("Upper"));
        assertTrue(isBinaryProperty("Uppercase"));
        assertFalse(isBinaryProperty("upper"));
        assertFalse(isBinaryProperty("uppercase"));

        assertTrue(isBinaryProperty("VS"));
        assertTrue(isBinaryProperty("Variation_Selector"));
        assertFalse(isBinaryProperty("vs"));
        assertFalse(isBinaryProperty("variation_selector"));
        assertFalse(isBinaryProperty("variationselector"));
        assertFalse(isBinaryProperty("VariationSelector"));

        assertTrue(isBinaryProperty("WSpace"));
        assertTrue(isBinaryProperty("White_Space"));
        assertTrue(isBinaryProperty("space"));
        assertFalse(isBinaryProperty("wspace"));
        assertFalse(isBinaryProperty("Space"));
        assertFalse(isBinaryProperty("WhiteSpace"));
        assertFalse(isBinaryProperty("Whitespace"));
        assertFalse(isBinaryProperty("whitespace"));

        assertTrue(isBinaryProperty("XIDC"));
        assertTrue(isBinaryProperty("XID_Continue"));
        assertFalse(isBinaryProperty("xidc"));
        assertFalse(isBinaryProperty("XID_continue"));
        assertFalse(isBinaryProperty("xid_continue"));
        assertFalse(isBinaryProperty("xidcontinue"));

        assertTrue(isBinaryProperty("XIDS"));
        assertTrue(isBinaryProperty("XID_Start"));
        assertFalse(isBinaryProperty("xids"));
        assertFalse(isBinaryProperty("XID_start"));
        assertFalse(isBinaryProperty("xid_start"));
        assertFalse(isBinaryProperty("xidstart"));
    }

    @Test
    public void testBinaryPropertyEmoji() {
        assertTrue(isBinaryProperty("Emoji"));
        assertTrue(isBinaryProperty("Emoji_Presentation"));
        assertTrue(isBinaryProperty("Emoji_Modifier"));
        assertTrue(isBinaryProperty("Emoji_Modifier_Base"));
    }
}
