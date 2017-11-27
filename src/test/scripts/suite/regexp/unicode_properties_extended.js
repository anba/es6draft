/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertFalse, assertTrue, assertNotSame, assertThrows
} = Assert;

// Binary properties
for (let {names, positive, negative} of [
  {
    names: ["Cased"],
    positive: ["\u0049", "\u00B5", "\u212A", "\u{1F170}"],
    negative: [],
  },
  {
    names: ["Case_Ignorable", "CI"],
    positive: ["\u0027", "\u00B7", "\uFF70", "\u{1F3FB}"],
    negative: [],
  },
  {
    names: ["Changes_When_Lowercased", "CWL"],
    positive: ["\u004F", "\u0106", "\uA69A", "\u{118A0}"],
    negative: [],
  },
  {
    names: ["Changes_When_Uppercased", "CWU"],
    positive: ["\u0062", "\u00B5", "\u03E3", "\u{1044E}"],
    negative: [],
  },
  {
    names: ["Changes_When_Titlecased", "CWT"],
    positive: ["\u0066", "\u00F6", "\uA699", "\u{10CF0}"],
    negative: [],
  },
  {
    names: ["Changes_When_Casefolded", "CWCF"],
    positive: ["\u0050", "\u00D8", "\u03D5", "\u{118A0}"],
    negative: [],
  },
  {
    names: ["Changes_When_Casemapped", "CWCM"],
    positive: ["\u005A", "\u00C0", "\u10A0", "\u{10CDF}"],
    negative: [],
  },
  {
    names: ["Grapheme_Base", "Gr_Base"],
    positive: ["\u0024", "\u00A8", "\u102A", "\u{1EE47}"],
    negative: [],
  },
  {
    names: ["Grapheme_Extend", "Gr_Ext"],
    positive: ["\u0300", "\u0D4D", "\u{1DAA1}"],
    negative: [],
  },
  {
    names: ["Math"],
    positive: ["\u002B", "\u00F7", "\u03D5", "\u{1D4A2}"],
    negative: [],
  },
  {
    names: ["ID_Start", "IDS"],
    positive: ["\u0041", "\u00AA", "\u05D0", "\u{13000}"],
    negative: [],
  },
  {
    names: ["ID_Continue", "IDC"],
    positive: ["\u0039", "\u00BA", "\u0BA8", "\u{1EEAB}"],
    negative: [],
  },
  {
    names: ["XID_Start", "XIDS"],
    positive: ["\u007A", "\u00D8", "\u064A", "\u{10330}"],
    negative: [],
  },
  {
    names: ["XID_Continue", "XIDC"],
    positive: ["\u0030", "\u005F", "\u00F6", "\u0E01", "\u{11000}"],
    negative: [],
  },
  // {
  //   names: ["Full_Composition_Exclusion", "Comp_Ex"],
  //   positive: ["\u0340", "\u1FBB", "\u{2F9EA}"],
  //   negative: [],
  // },
  {
    names: ["Changes_When_NFKC_Casefolded", "CWKCF"],
    positive: ["\u0045", "\u00AF", "\u03E2", "\u{1EE39}"],
    negative: [],
  },
  {
    names: ["ASCII_Hex_Digit", "AHex"],
    positive: ["\u0038", "\u0045"],
    negative: [],
  },
  {
    names: ["Bidi_Control", "Bidi_C"],
    positive: ["\u061C", "\u200E"],
    negative: [],
  },
  {
    names: ["Dash"],
    positive: ["\u002D", "\u1806", "\u30A0"],
    negative: [],
  },
  {
    names: ["Deprecated", "Dep"],
    positive: ["\u0149", "\u2329", "\u{E0001}"],
    negative: [],
  },
  {
    names: ["Diacritic", "Dia"],
    positive: ["\u005E", "\u00B8", "\u0559", "\u{11235}"],
    negative: [],
  },
  {
    names: ["Extender", "Ext"],
    positive: ["\u00B7", "\u30FC", "\u{115C8}"],
    negative: [],
  },
  {
    names: ["Hex_Digit", "Hex"],
    positive: ["\u0030", "\u0064", "\uFF12", "\uFF26"],
    negative: [],
  },
  {
    names: ["Ideographic", "Ideo"],
    positive: ["\u3006", "\u3400", "\u{2F800}"],
    negative: [],
  },
  {
    names: ["IDS_Binary_Operator", "IDSB"],
    positive: ["\u2FF0", "\u2FFB"],
    negative: [],
  },
  {
    names: ["IDS_Trinary_Operator", "IDST"],
    positive: ["\u2FF2", "\u2FF3"],
    negative: [],
  },
  {
    names: ["Join_Control", "Join_C"],
    positive: ["\u200C"],
    negative: [],
  },
  {
    names: ["Logical_Order_Exception", "LOE"],
    positive: ["\u0E41", "\uAAB6"],
    negative: [],
  },
  {
    names: ["Pattern_Syntax", "Pat_Syn"],
    positive: ["\u0023", "\u002D", "\u00A9", "\u2047", "\u3030"],
    negative: [],
  },
  {
    names: ["Pattern_White_Space", "Pat_WS"],
    positive: ["\u000A", "\u0020", "\u0085", "\u2028"],
    negative: [],
  },
  {
    names: ["Quotation_Mark", "QMark"],
    positive: ["\u0022", "\u0027", "\u00AB", "\uFF63"],
    negative: [],
  },
  {
    names: ["Radical"],
    positive: ["\u2E85", "\u2FD4"],
    negative: [],
  },
  {
    names: ["Soft_Dotted", "SD"],
    positive: ["\u0069", "\u006A", "\u012F", "\u2071", "\u{1D456}"],
    negative: [],
  },
  {
    names: ["STerm"],
    positive: ["\u0021", "\u003F", "\u1803", "\uFF0E", "\u{1DA88}"],
    negative: [],
  },
  {
    names: ["Terminal_Punctuation", "Term"],
    positive: ["\u0021", "\u002E", "\u060C", "\u16EB", "\u{11047}"],
    negative: [],
  },
  {
    names: ["Unified_Ideograph", "UIdeo"],
    positive: ["\u34FF", "\uFA28", "\u{2A700}"],
    negative: [],
  },
  {
    names: ["Variation_Selector", "VS"],
    positive: ["\u180C", "\uFE0A", "\u{E01AB}"],
    negative: [],
  },
  {
    names: ["Bidi_Mirrored", "Bidi_M"],
    positive: ["\u0028", "\u00BB", "\u0F3C", "\u2140", "\u{1D6DB}"],
    negative: [],
  },
]) {
  assertNotSame(0, names.length);
  assertNotSame(0, positive.length, `No positive cases for: ${names[0]}`);
  for (let name of names) {
    for (let t of positive) {
      assertTrue(RegExp(`\\p{${name}}`, "u").test(t), t);
      assertFalse(RegExp(`\\P{${name}}`, "u").test(t), t);

      assertTrue(RegExp(`[\\p{${name}}]`, "u").test(t), t);
      assertFalse(RegExp(`[\\P{${name}}]`, "u").test(t), t);

      assertFalse(RegExp(`[^\\p{${name}}]`, "u").test(t), t);
      assertTrue(RegExp(`[^\\P{${name}}]`, "u").test(t), t);
    }
    for (let t of negative) {
      assertFalse(RegExp(`\\p{${name}}`, "u").test(t), t);
      assertTrue(RegExp(`\\P{${name}}`, "u").test(t), t);

      assertFalse(RegExp(`[\\p{${name}}]`, "u").test(t), t);
      assertTrue(RegExp(`[\\P{${name}}]`, "u").test(t), t);

      assertTrue(RegExp(`[^\\p{${name}}]`, "u").test(t), t);
      assertFalse(RegExp(`[^\\P{${name}}]`, "u").test(t), t);
    }
  }
}


// Enumeration properties
for (let {names, values} of [
  {
    names: ["Block", "blk"],
    values: [
      {
        names: ["ASCII", "Basic_Latin"],
        positive: ["\u0000", "\u005F", "\u007F"],
        negative: [],
      },
      {
        names: ["Latin_1_Sup", "Latin_1_Supplement", "Latin_1"],
        positive: ["\u0080", "\u00AB", "\u00FF"],
        negative: [],
      },
      {
        names: ["Arabic"],
        positive: ["\u0600", "\u06C0", "\u06FF"],
        negative: [],
      },
      {
        names: ["Emoticons"],
        positive: ["\u{1F600}", "\u{1F620}", "\u{1F64F}"],
        negative: [],
      },
    ],
  },
  {
    names: ["Joining_Type", "jt"],
    values: [
      {
        names: ["Join_Causing", "C"],
        positive: ["\u0640", "\u200D"],
        negative: [],
      },
      {
        names: ["Dual_Joining", "D"],
        positive: ["\u0628", "\u{10B90}"],
        negative: [],
      },
      {
        names: ["Left_Joining", "L"],
        positive: ["\uA872", "\u{10ACD}"],
        negative: [],
      },
      {
        names: ["Right_Joining", "R"],
        positive: ["\u0625", "\u{10AE1}"],
        negative: [],
      },
      {
        names: ["Transparent", "T"],
        positive: ["\u00AD", "\u05BF", "\u{10A3F}"],
        negative: [],
      },
      {
        names: ["Non_Joining", "U"],
        positive: ["\u0061", "\u{1F620}"],
        negative: [],
      },
    ],
  },
  {
    names: ["Joining_Group", "jg"],
    values: [
      {
        names: ["No_Joining_Group"],
        positive: ["\u0061"],
        negative: [],
      },
      {
        names: ["Ain"],
        positive: ["\u0639", "\u08B3"],
        negative: [],
      },
      {
        names: ["Yeh"],
        positive: ["\u0626", "\u0777"],
        negative: [],
      },
      {
        names: ["Manichaean_Qoph"],
        positive: ["\u{10AE0}"],
        negative: [],
      },
    ],
  },
  {
    names: ["Bidi_Paired_Bracket_Type", "bpt"],
    values: [
      {
        names: ["Close", "c"],
        positive: ["\u0029", "\u2309"],
        negative: [],
      },
      {
        names: ["None", "n"],
        positive: ["\u0061"],
        negative: [],
      },
      {
        names: ["Open", "o"],
        positive: ["\u0028", "\u2308"],
        negative: [],
      },
    ],
  },
  {
    names: ["East_Asian_Width", "ea"],
    values: [
      {
        names: ["Ambiguous", "A"],
        positive: ["\u00A1", "\u2016", "\u{1F10A}"],
        negative: [],
      },
      {
        names: ["Fullwidth", "F"],
        positive: ["\uFF04", "\uFF3F"],
        negative: [],
      },
      {
        names: ["Halfwidth", "H"],
        positive: ["\uFF62", "\uFF73"],
        negative: [],
      },
      {
        names: ["Neutral", "N"],
        positive: ["\u0009", "\u00A0", "\u27C5", "\u{10107}"],
        negative: [],
      },
      {
        names: ["Narrow", "Na"],
        positive: ["\u0061", "\u27E6"],
        negative: [],
      },
      {
        names: ["Wide", "W"],
        positive: ["\u3041", "\u30A1", "\uA960", "\u{1B000}"],
        negative: [],
      },
    ],
  },
  {
    names: ["Hangul_Syllable_Type", "hst"],
    values: [
      {
        names: ["Leading_Jamo", "L"],
        positive: ["\u1100", "\uA970", "\uA97C"],
        negative: [],
      },
      {
        names: ["LV_Syllable", "LV"],
        positive: ["\uAC00", "\uBF78", "\uD788"],
        negative: [],
      },
      {
        names: ["LVT_Syllable", "LVT"],
        positive: ["\uAC01", "\uB9C1", "\uD7A3"],
        negative: [],
      },
      {
        names: ["Not_Applicable", "NA"],
        positive: ["\u0041", "\u{1B00A}"],
        negative: [],
      },
      {
        names: ["Trailing_Jamo", "T"],
        positive: ["\u11A8", "\u11F1", "\uD7FB"],
        negative: [],
      },
      {
        names: ["Vowel_Jamo", "V"],
        positive: ["\u1160", "\uD7BF", "\uD7C6"],
        negative: [],
      },
    ],
  },
  {
    names: ["Line_Break", "lb"],
    values: [
      {
        names: ["Alphabetic", "AL"],
        positive: ["\u005E", "\u00B5", "\u070F", "\u{16B40}"],
        negative: [],
      },
      {
        names: ["Mandatory_Break", "BK"],
        positive: ["\u000B", "\u000C", "\u2028"],
        negative: [],
      },
      {
        names: ["Carriage_Return", "CR"],
        positive: ["\u000D"],
        negative: [],
      },
      {
        names: ["Conditional_Japanese_Starter", "CJ"],
        positive: ["\u3087", "\u30E7", "\uFF70"],
        negative: [],
      },
    ],
  },
  {
    names: ["Grapheme_Cluster_Break", "GCB"],
    values: [
      {
        names: ["LF"],
        positive: ["\u000A"],
        negative: [],
      },
      {
        names: ["Control", "CN"],
        positive: ["\u0009", "\u180E", "\u{E0001}"],
        negative: [],
      },
      {
        names: ["SpacingMark", "SM"],
        positive: ["\u1084", "\u{11726}"],
        negative: [],
      },
      {
        names: ["Other", "XX"],
        positive: ["\u0041", "\u{16B40}"],
        negative: [],
      },
    ],
  },
  {
    names: ["Sentence_Break", "SB"],
    values: [
      {
        names: ["CR"],
        positive: ["\u000D"],
        negative: [],
      },
      {
        names: ["Extend", "EX"],
        positive: ["\u0483", "\uA66F", "\u{11182}"],
        negative: [],
      },
      {
        names: ["Lower", "LO"],
        positive: ["\u0061", "\u00F0", "\u1E53", "\u{1D56B}"],
        negative: [],
      },
    ],
  },
  {
    names: ["Word_Break", "WB"],
    values: [
      {
        names: ["Double_Quote", "DQ"],
        positive: ["\u0022"],
        negative: [],
      },
      {
        names: ["Hebrew_Letter", "HL"],
        positive: ["\u05D0", "\uFB48"],
        negative: [],
      },
      {
        names: ["Katakana", "KA"],
        positive: ["\u3033", "\uFF92", "\u{1B000}"],
        negative: [],
      },
    ],
  },
  {
    names: ["Numeric_Type", "nt"],
    values: [
      {
        names: ["Decimal", "De"],
        positive: ["\u0030", "\u0660", "\u{16A69}"],
        negative: [],
      },
      {
        names: ["Digit", "Di"],
        positive: ["\u00B3", "\u278A", "\u{1F100}"],
        negative: [],
      },
      {
        names: ["None"],
        positive: ["\u0041"],
        negative: [],
      },
      {
        names: ["Numeric", "Nu"],
        positive: ["\u00BC", "\u3405", "\u{109CF}"],
        negative: [],
      },
    ],
  },
  {
    names: ["NFD_Quick_Check", "NFD_QC"],
    values: [
      {
        names: ["No", "N"],
        positive: ["\u00C0", "\u0457", "\u3067", "\u{114BB}"],
        negative: [],
      },
      {
        names: ["Yes", "Y"],
        positive: ["\u0041", "\u30B5", "\u{114BA}"],
        negative: [],
      },
    ],
  },
  {
    names: ["NFKD_Quick_Check", "NFKD_QC"],
    values: [
      {
        names: ["No", "N"],
        positive: ["\u00B2", "\u2260", "\u3070", "\u{1F100}"],
        negative: [],
      },
      {
        names: ["Yes", "Y"],
        positive: ["\u0042", "\u3068", "\u{1F252}"],
        negative: [],
      },
    ],
  },
  {
    names: ["NFC_Quick_Check", "NFC_QC"],
    values: [
      {
        names: ["Maybe", "M"],
        positive: ["\u0323", "\u11A8", "\u{11357}"],
        negative: [],
      },
      {
        names: ["No", "N"],
        positive: ["\u0374", "\u2ADC", "\u{2F900}"],
        negative: [],
      },
      {
        names: ["Yes", "Y"],
        positive: ["\u0041", "\u30B7", "\u{1D15A}"],
        negative: [],
      },
    ],
  },
  {
    names: ["NFKC_Quick_Check", "NFKC_QC"],
    values: [
      {
        names: ["Maybe", "M"],
        positive: ["\u0338", "\u102E", "\u{11127}"],
        negative: [],
      },
      {
        names: ["No", "N"],
        positive: ["\u00BA", "\u03F0", "\uFFDA", "\u{1D7A9}"],
        negative: [],
      },
      {
        names: ["Yes", "Y"],
        positive: ["\u0049", "\u30B6", "\u{11128}"],
        negative: [],
      },
    ],
  },
  {
    names: ["Bidi_Class", "bc"],
    values: [
      {
        names: ["Left_To_Right", "L"],
        positive: ["\u0041", "\u00AA", "\u0E01", "\u{10320}"],
        negative: [],
      },
      {
        names: ["Right_To_Left", "R"],
        positive: ["\u05D0", "\uFB3E", "\u{10857}"],
        negative: [],
      },
      {
        names: ["European_Separator", "ES"],
        positive: ["\u002B", "\uFB29"],
        negative: [],
      },
      {
        names: ["Arabic_Letter", "AL"],
        positive: ["\u0620", "\uFDF0", "\u{1EE24}"],
        negative: [],
      },
    ],
  },
  {
    names: ["Decomposition_Type", "dt"],
    values: [
      {
        names: ["Canonical", "Can", "can"],
        positive: ["\u00C0", "\u1FBE", "\u{2F800}"],
        negative: [],
      },
      {
        names: ["Compat", "Com", "com"],
        positive: ["\u00A8", "\u2103", "\u{1F240}"],
        negative: [],
      },
      {
        names: ["Square", "Sqr", "sqr"],
        positive: ["\u3250", "\u33FF", "\u{1F202}"],
        negative: [],
      },
    ],
  },
  {
    names: ["Canonical_Combining_Class", "ccc"],
    values: [
      {
        names: ["Not_Reordered", "NR"],
        positive: ["\u0040", "\u0061", "\u09EF", "\u{102E1}"],
        negative: [],
      },
      {
        names: ["Kana_Voicing", "KV"],
        positive: ["\u3099"],
        negative: [],
      },
      {
        names: ["Attached_Above_Right", "ATAR"],
        positive: ["\u031B", "\u{1D166}"],
        negative: [],
      },
    ],
  },
]) {
  assertNotSame(0, names.length);
  assertNotSame(0, values.length, `No property values for: ${names[0]}`);
  for (let name of names) {
    for (let {names: valueNames, positive, negative} of values) {
      assertNotSame(0, valueNames.length, `No property value names: ${names[0]}`);
      assertNotSame(0, positive.length, `No positive cases for: ${names[0]}=${valueNames[0]}`);
      for (let valueName of valueNames) {
        assertThrows(SyntaxError, () => RegExp(`\\p{${name}=${valueName}}`, "u"));
        assertThrows(SyntaxError, () => RegExp(`\\P{${name}=${valueName}}`, "u"));

        assertThrows(SyntaxError, () => RegExp(`[\\p{${name}=${valueName}}]`, "u"));
        assertThrows(SyntaxError, () => RegExp(`[\\P{${name}=${valueName}}]`, "u"));

        assertThrows(SyntaxError, () => RegExp(`[^\\p{${name}=${valueName}}]`, "u"));
        assertThrows(SyntaxError, () => RegExp(`[^\\P{${name}=${valueName}}]`, "u"));
      }
    }
  }
}
