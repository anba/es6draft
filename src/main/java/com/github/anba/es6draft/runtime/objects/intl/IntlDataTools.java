/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.intl;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.TimeZone.SystemTimeZoneType;
import com.ibm.icu.util.ULocale;

/**
 * Simple tools to generate the various language data for the intl package
 */
final class IntlDataTools {
    private IntlDataTools() {
    }

    public static void main(String[] args) throws IOException {
        // Path cldr = java.nio.file.Paths.get("/tmp/cldr32-core");
        // oldStyleLanguageTags(cldr);

        // Path currencyFile = java.nio.file.Paths.get("/tmp/iso_currency.xml");
        // currencyDigits(currencyFile);

        // Path tzdataDir = java.nio.file.Paths.get("/tmp/tzdata2013c.tar");
        // jdkTimezoneNames(tzdataDir);

        // Path langSubtagReg = java.nio.file.Paths.get("/tmp/language-subtag-registry.txt");
        // languageSubtagRegistry(langSubtagReg);

        // Path cldr = java.nio.file.Paths.get("/tmp/cldr32-core");
        // numberingSystems(cldr);
        // collationCase(cldr);
    }

    /**
     * {@link LanguageSubtagRegistryData}
     * 
     * @param langSubtagReg
     *            the language subtag registry file
     * @throws IOException
     *             if an I/O error occurs
     */
    static void languageSubtagRegistry(Path langSubtagReg) throws IOException {
        List<String> lines = Files.readAllLines(langSubtagReg, StandardCharsets.UTF_8);
        ArrayDeque<String> stack = new ArrayDeque<>(lines);

        ArrayList<Record> language = new ArrayList<>();
        ArrayList<Record> region = new ArrayList<>();
        ArrayList<Record> grandfathered = new ArrayList<>();
        ArrayList<Record> redundant = new ArrayList<>();

        ArrayList<Record> extlang = new ArrayList<>();
        ArrayList<Record> script = new ArrayList<>();
        ArrayList<Record> variant = new ArrayList<>();

        // skip first two lines (file date + %% separator)
        stack.pop();
        stack.pop();
        while (!stack.isEmpty()) {
            Record rec = readRecord(stack);
            String type = rec.get(Field.Type);
            assert type != null;
            if ("language".equals(type)) {
                if (rec.has(Field.PreferredValue)) {
                    language.add(rec);
                }
            }
            if ("region".equals(type)) {
                if (rec.has(Field.PreferredValue)) {
                    region.add(rec);
                }
            }
            if ("grandfathered".equals(type)) {
                grandfathered.add(rec);
            }
            if ("redundant".equals(type)) {
                if (rec.has(Field.PreferredValue)) {
                    redundant.add(rec);
                }
            }
            if ("extlang".equals(type)) {
                if (rec.has(Field.PreferredValue)) {
                    extlang.add(rec);
                }
            }
            if ("script".equals(type)) {
                if (rec.has(Field.PreferredValue)) {
                    script.add(rec);
                }
            }
            if ("variant".equals(type)) {
                if (rec.has(Field.PreferredValue)) {
                    variant.add(rec);
                }
            }
        }

        /* Generate LanguageSubtagRegistryData#scriptData entries */
        System.out.println("--- [LanguageSubtagRegistryData#scriptData] ---");
        for (Record record : script) {
            assert record.has(Field.Prefix);
            System.out.printf("%s -> %s [%s]%n", record.get(Field.Subtag), record.get(Field.PreferredValue),
                    record.get(Field.Prefix));
        }
        System.out.println();
        assert script.isEmpty() : "no preferred values for 'script' expected";

        /* Generate LanguageSubtagRegistryData#extlangData entries */
        System.out.println("--- [LanguageSubtagRegistryData#extlangData] ---");
        for (Record record : extlang) {
            assert record.has(Field.Prefix);
            assert record.get(Field.Subtag).equals(record.get(Field.PreferredValue)) : record.get(Field.Subtag);
            System.out.printf("map.put(\"%s\", \"%s\");%n", record.get(Field.Subtag), record.get(Field.Prefix));
        }
        System.out.println();

        /* Generate LanguageSubtagRegistryData#variantData entries */
        System.out.println("--- [LanguageSubtagRegistryData#variantData] ---");
        for (Record record : variant) {
            assert record.has(Field.Prefix);
            System.out.printf("%s -> %s [%s]%n", record.get(Field.Subtag), record.get(Field.PreferredValue),
                    record.get(Field.Prefix));
            System.out.printf("map.put(\"%s\", \"%s\");%n", record.get(Field.Subtag), record.get(Field.PreferredValue));
        }
        System.out.println();
        assert variant.size() == 1 : "Only one variant entry expected";
        assert variant.get(0).get(Field.Subtag).equals("heploc");
        assert variant.get(0).get(Field.PreferredValue).equals("alalc97");

        /* Generate LanguageSubtagRegistryData#regionData entries */
        System.out.println("--- [LanguageSubtagRegistryData#regionData] ---");
        for (Record record : region) {
            assert !record.has(Field.Prefix);
            System.out.printf("map.put(\"%s\", \"%s\");%n", record.get(Field.Subtag).toLowerCase(Locale.ROOT),
                    record.get(Field.PreferredValue));
        }
        System.out.println();

        /* Generate LanguageSubtagRegistryData#languageData entries */
        System.out.println("--- [LanguageSubtagRegistryData#languageData] ---");
        for (Record record : language) {
            assert !record.has(Field.Prefix);
            System.out.printf("map.put(\"%s\", \"%s\");%n", record.get(Field.Subtag), record.get(Field.PreferredValue));
        }
        System.out.println();

        /* Generate LanguageSubtagRegistryData#grandfatheredData entries */
        System.out.println("--- [LanguageSubtagRegistryData#grandfatheredData] ---");
        for (Record record : grandfathered) {
            assert !record.has(Field.Prefix);
            if (record.has(Field.PreferredValue)) {
                System.out.printf("map.put(\"%s\", \"%s\");%n", record.get(Field.Tag).toLowerCase(Locale.ROOT),
                        record.get(Field.PreferredValue));
            } else {
                System.out.printf("map.put(\"%s\", \"%s\");%n", record.get(Field.Tag).toLowerCase(Locale.ROOT),
                        record.get(Field.Tag));
            }
        }
        System.out.println();

        /* Generate LanguageSubtagRegistryData#redundantData entries */
        System.out.println("--- [LanguageSubtagRegistryData#redundantData] ---");
        for (Record record : redundant) {
            assert !record.has(Field.Prefix);
            System.out.printf("map.put(\"%s\", \"%s\");%n", record.get(Field.Tag).toLowerCase(Locale.ROOT),
                    record.get(Field.PreferredValue));
        }
        System.out.println();
    }

    private enum Field {
        Type("Type"), Tag("Tag"), Subtag("Subtag"), Description("Description"), Added("Added"),
        Deprecated("Deprecated"), PreferredValue("Preferred-Value"), Prefix("Prefix"), SupressScript("Suppress-Script"),
        Macrolanguage("Macrolanguage"), Scope("Scope"), Comments("Comments");

        private final String name;

        private Field(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        static final HashMap<String, Field> byName;

        static {
            HashMap<String, Field> map = new HashMap<>();
            for (Field field : Field.values()) {
                map.put(field.getName(), field);
            }
            byName = map;
        }

        public static Field forName(String name) {
            return byName.get(name);
        }
    }

    private static final class Record {
        EnumMap<Field, String> entries = new EnumMap<>(Field.class);

        boolean has(Field field) {
            return entries.containsKey(field);
        }

        String get(Field field) {
            return entries.get(field);
        }
    }

    private static Record readRecord(ArrayDeque<String> stack) {
        Record rec = new Record();
        for (;;) {
            if (stack.isEmpty()) {
                return rec;
            }
            String line = stack.pop();
            assert !line.isEmpty();
            if ("%%".equals(line)) {
                return rec;
            }
            if (line.charAt(0) == ' ') {
                // continuation
                continue;
            }
            int sep = line.indexOf(':');
            String name = line.substring(0, sep).trim();
            String value = line.substring(sep + 1).trim();
            Field field = Field.forName(name);
            assert field != null;
            switch (field) {
            case Deprecated:
            case PreferredValue:
            case Prefix:
            case Subtag:
            case Tag:
            case Type:
                rec.entries.put(field, value);
                break;
            case Added:
            case Comments:
            case Description:
            case Macrolanguage:
            case Scope:
            case SupressScript:
            default:
                // ignore these
                break;
            }
        }
    }

    /**
     * {@link IntlAbstractOperations#JDK_TIMEZONE_NAMES}
     * 
     * @param tzdataDir
     *            the tzdata directory
     * @throws IOException
     *             if an I/O error occurs
     */
    static void jdkTimezoneNames(Path tzdataDir) throws IOException {
        Pattern pZone = Pattern.compile("Zone\\s+([a-zA-Z0-9_+\\-/]+)\\s+.*");
        Pattern pLink = Pattern.compile("Link\\s+([a-zA-Z0-9_+\\-/]+)\\s+([a-zA-Z0-9_+\\-/]+)(?:\\s+#.*)?");
        Pattern pFileName = Pattern.compile("[a-z0-9]+");

        HashSet<String> ignoreFiles = new HashSet<>(Arrays.asList("backzone"));
        TreeSet<String> names = new TreeSet<>();
        TreeMap<String, String> links = new TreeMap<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(tzdataDir)) {
            for (Path path : stream) {
                String filename = Objects.requireNonNull(path.getFileName()).toString();
                if (pFileName.matcher(filename).matches() && !ignoreFiles.contains(filename)) {
                    Files.lines(path, StandardCharsets.UTF_8).forEach(line -> {
                        if (line.startsWith("Zone")) {
                            Matcher m = pZone.matcher(line);
                            if (!m.matches()) {
                                System.out.println(line);
                            }
                            String name = m.group(1);
                            boolean changed = names.add(name);
                            assert changed : line;
                        } else if (line.startsWith("Link")) {
                            Matcher m = pLink.matcher(line);
                            if (!m.matches()) {
                                System.out.println(line);
                            }
                            String target = m.group(1);
                            String source = m.group(2);
                            boolean changed = links.put(source, target) == null;
                            assert changed : String.format("%s: %s", filename, line);
                        }
                    });
                }
            }
        }

        TreeSet<String> allnames = new TreeSet<>();
        allnames.addAll(names);
        for (Map.Entry<String, String> link : links.entrySet()) {
            assert allnames.contains(link.getValue());
            boolean changed = allnames.add(link.getKey());
            assert changed : link;
        }

        TreeSet<String> ids = new TreeSet<>(TimeZone.getAvailableIDs(SystemTimeZoneType.ANY, null, null));
        ids.removeIf(id -> id.startsWith("SystemV/"));

        System.out.println(allnames);
        System.out.println(ids.size());
        System.out.println(allnames.size());

        TreeSet<String> jdkTimeZones = new TreeSet<>(ids);
        jdkTimeZones.removeAll(allnames);
        for (String name : jdkTimeZones) {
            System.out.printf("\"%s\",", name);
        }
    }

    /**
     * {@link NumberFormatConstructor#CurrencyDigits(String)}
     * 
     * @param currencyFile
     *            the currency xml-file
     * @throws IOException
     *             if an I/O error occurs
     */
    static void currencyDigits(Path currencyFile) throws IOException {
        try (Reader reader = Files.newBufferedReader(currencyFile, StandardCharsets.UTF_8)) {
            LinkedHashMap<String, Integer> map = new LinkedHashMap<>();
            Document document = document(reader);
            elementsByTagName(document.getDocumentElement(), "CcyNtry").forEach(entry -> {
                elementByTagName(entry, "Ccy").ifPresent(currency -> {
                    String code = currency.getTextContent();
                    int minor = 2;
                    try {
                        minor = Integer.parseInt(elementByTagName(entry, "CcyMnrUnts").get().getTextContent());
                    } catch (NumberFormatException e) {
                    }
                    if (map.containsKey(code) && map.get(code) != minor) {
                        System.err.println(code);
                    }
                    if (minor != 2) {
                        map.putIfAbsent(code, minor);
                    }
                });
            });
            map.entrySet().stream()
                    .collect(Collectors.groupingBy(Map.Entry::getValue, TreeMap::new,
                            Collectors.mapping(Map.Entry::getKey, Collectors.toList())))
                    .forEach((minor, currencies) -> {
                        currencies.stream().sorted().forEach(c -> System.out.printf("case \"%s\":%n", c));
                        System.out.printf("    return %d;%n", minor);
                    });
            System.out.println("default:\n    return 2;");
        }
    }

    /**
     * {@link IntlAbstractOperations#oldStyleLanguageTags}
     * 
     * @param cldrMainDir
     *            the CLDR directory
     * @throws IOException
     *             if an I/O error occurs
     */
    static void oldStyleLanguageTags(Path cldr) throws IOException {
        LinkedHashMap<String, String> likelySubtags = new LinkedHashMap<>();
        try (Reader reader = Files.newBufferedReader(cldr.resolve("supplemental/likelySubtags.xml"),
                StandardCharsets.UTF_8)) {
            Document document = document(reader);
            elementsByTagName(document, "likelySubtag").forEach(likelySubtag -> {
                String from = likelySubtag.getAttribute("from").replace('_', '-');
                String to = likelySubtag.getAttribute("to").replace('_', '-');
                likelySubtags.put(from, to);
            });
        }

        Set<String> allTags = Files.walk(cldr.resolve("main")).filter(Files::isRegularFile).map(Path::getFileName)
                .map(Path::toString).map(p -> p.substring(0, p.indexOf(".xml")).replace('_', '-'))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        class Entry implements Comparable<Entry> {
            final String tag;
            final String languageRegion;
            final int priority;

            Entry(String tag, String languageRegion, int priority) {
                this.tag = tag;
                this.languageRegion = languageRegion;
                this.priority = priority;
            }

            @Override
            public int compareTo(Entry o) {
                int c = languageRegion.compareTo(o.languageRegion);
                return c < 0 ? -1 : c > 0 ? 1 : Integer.compare(priority, o.priority);
            }

            @Override
            public boolean equals(Object obj) {
                if (obj instanceof Entry) {
                    return languageRegion.equals(((Entry) obj).languageRegion);
                }
                return false;
            }

            @Override
            public int hashCode() {
                return languageRegion.hashCode();
            }
        }
        Function<Locale, String> toLanguageScript = locale -> new Locale.Builder().setLanguage(locale.getLanguage())
                .setScript(locale.getScript()).build().toLanguageTag();
        Function<Locale, String> toLanguageRegion = locale -> new Locale.Builder().setLanguage(locale.getLanguage())
                .setRegion(locale.getCountry()).build().toLanguageTag();
        Function<Locale, String> toLanguage = locale -> new Locale.Builder().setLanguage(locale.getLanguage()).build()
                .toLanguageTag();

        System.out.printf("private static final String[] oldStyleLanguageTags = {%n");
        allTags.stream().map(Locale::forLanguageTag)
                .filter(locale -> !locale.getScript().isEmpty() && !locale.getCountry().isEmpty())
                .filter(locale -> allTags.contains(toLanguageScript.apply(locale))).map(locale -> {
                    String languageTag = locale.toLanguageTag();
                    String languageScript = toLanguageScript.apply(locale);
                    String languageRegion = toLanguageRegion.apply(locale);
                    String language = toLanguage.apply(locale);

                    int prio;
                    if (languageTag.equals(likelySubtags.get(languageScript))) {
                        prio = 1;
                    } else if (languageTag.equals(likelySubtags.get(languageRegion))) {
                        prio = 2;
                    } else if (languageTag.equals(likelySubtags.get(language))) {
                        prio = 3;
                    } else if (likelySubtags.getOrDefault(language, "").startsWith(languageScript)) {
                        prio = 4;
                    } else {
                        prio = 5;
                    }
                    return new Entry(languageTag, languageRegion, prio);
                }).sorted().distinct().forEach(e -> {
                    System.out.printf("    \"%s\", \"%s\",%n", e.tag, e.languageRegion);
                });
        System.out.printf("};%n");
    }

    static void collationCase(Path cldr) throws IOException {
        Files.walk(cldr.resolve("collation")).filter(Files::isRegularFile).forEach(p -> {
            try (Reader reader = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
                Document document = document(reader);
                elementsByTagName(document, "collation").filter(e -> "standard".equals(e.getAttribute("type")))
                        .forEach(e -> {
                            elementByTagName(e, "cr").ifPresent(cr -> {
                                String text = cr.getTextContent();
                                if (text.contains("caseFirst")) {
                                    System.out.println(p.getFileName());
                                }
                            });
                        });
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });

        for (ULocale locale : Collator.getAvailableULocales()) {
            Collator collator = Collator.getInstance(locale);
            if (collator instanceof RuleBasedCollator) {
                RuleBasedCollator ruleBasedCollator = (RuleBasedCollator) collator;
                if (ruleBasedCollator.isUpperCaseFirst()) {
                    System.out.printf("upper-first = %s%n", locale);
                } else if (ruleBasedCollator.isLowerCaseFirst()) {
                    System.out.printf("lower-first = %s%n", locale);
                }
            }
        }
    }

    static void numberingSystems(Path cldr) throws IOException {
        // Late additions? [bali, limb]
        LinkedHashSet<String> bcp47Numbers = new LinkedHashSet<>();
        Path bcp47 = cldr.resolve("bcp47/number.xml");
        try (Reader reader = Files.newBufferedReader(bcp47, StandardCharsets.UTF_8)) {
            Document document = document(reader);
            elementsByTagName(document, "type").map(type -> type.getAttribute("name")).forEach(bcp47Numbers::add);
        }
        System.out.println(bcp47Numbers.size());
        System.out.println(bcp47Numbers);

        LinkedHashSet<String> numberingSystems = new LinkedHashSet<>();
        Path supplemental = cldr.resolve("supplemental/numberingSystems.xml");
        try (Reader reader = Files.newBufferedReader(supplemental, StandardCharsets.UTF_8)) {
            Document document = document(reader);
            elementsByTagName(document, "numberingSystem").filter(ns -> !"algorithmic".equals(ns.getAttribute("type")))
                    .peek(ns -> {
                        assert "numeric".equals(ns.getAttribute("type"));
                        String digits = ns.getAttribute("digits");
                        int radix = Character.codePointCount(digits, 0, digits.length());
                        if (radix != 10) {
                            System.out.printf("%s - %s [%d]%n", ns.getAttribute("id"), digits, radix);
                        }
                    }).map(ns -> ns.getAttribute("id")).forEach(numberingSystems::add);
        }
        System.out.println(numberingSystems.size());
        System.out.println(numberingSystems);

        // numberingSystems.forEach(s -> System.out.printf("\"%s\",", s));

        TreeSet<String> defaultNames = new TreeSet<>();
        TreeSet<String> otherNames = new TreeSet<>();
        Files.walk(cldr.resolve("main")).filter(Files::isRegularFile).forEach(p -> {
            try (Reader reader = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
                Document document = document(reader);
                elementByTagName(document, "numbers").ifPresent(numbers -> {
                    elementByTagName(numbers, "defaultNumberingSystem").map(Element::getTextContent)
                            .ifPresent(defaultNames::add);
                    elementByTagName(numbers, "otherNumberingSystems").ifPresent(otherNumberingSystems -> {
                        Stream.of("finance", "native", "traditional")
                                .map(name -> elementByTagName(otherNumberingSystems, name)).filter(Optional::isPresent)
                                .map(Optional::get).map(Element::getTextContent).forEach(otherNames::add);
                    });
                });
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        System.out.println(defaultNames);
        System.out.println(otherNames);

        TreeSet<String> allNames = new TreeSet<>();
        allNames.addAll(defaultNames);
        allNames.addAll(otherNames);

        System.out.println(allNames.stream().filter(n -> numberingSystems.contains(n)).collect(Collectors.toList()));
        System.out.println(allNames.stream().filter(n -> !numberingSystems.contains(n)).collect(Collectors.toList()));
    }

    private static Optional<Element> elementByTagName(Document document, String tagName) {
        return elementByTagName(document::getElementsByTagName, tagName);
    }

    private static Optional<Element> elementByTagName(Element element, String tagName) {
        return elementByTagName(element::getElementsByTagName, tagName);
    }

    private static Optional<Element> elementByTagName(Function<String, NodeList> fn, String tagName) {
        NodeList list = fn.apply(tagName);
        if (list.getLength() == 0) {
            return Optional.empty();
        }
        if (list.getLength() == 1) {
            return Optional.of((Element) list.item(0));
        }
        throw new IllegalArgumentException("Too many elements: " + list.getLength());
    }

    private static Stream<Element> elementsByTagName(Document document, String tagName) {
        return elementsByTagName(document::getElementsByTagName, tagName);
    }

    private static Stream<Element> elementsByTagName(Element element, String tagName) {
        return elementsByTagName(element::getElementsByTagName, tagName);
    }

    private static Stream<Element> elementsByTagName(Function<String, NodeList> fn, String tagName) {
        Iterator<Element> iterator = new Iterator<Element>() {
            final NodeList list = fn.apply(tagName);
            int i = 0;

            @Override
            public boolean hasNext() {
                for (; i < list.getLength(); ++i) {
                    if (list.item(i) instanceof Element) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public Element next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return (Element) list.item(i++);
            }
        };
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator,
                Spliterator.ORDERED | Spliterator.NONNULL | Spliterator.DISTINCT), false);
    }

    private static Document document(Reader xml) throws IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        // turn off any validation or namespace features
        factory.setNamespaceAware(false);
        factory.setValidating(false);
        List<String> features = Arrays.asList("http://xml.org/sax/features/namespaces",
                "http://xml.org/sax/features/validation",
                "http://apache.org/xml/features/nonvalidating/load-dtd-grammar",
                "http://apache.org/xml/features/nonvalidating/load-external-dtd");
        for (String feature : features) {
            try {
                factory.setFeature(feature, false);
            } catch (ParserConfigurationException e) {
                // ignore invalid feature names
            }
        }

        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource source = new InputSource(xml);
            return builder.parse(source);
        } catch (ParserConfigurationException | SAXException e) {
            throw new RuntimeException(e);
        }
    }
}
