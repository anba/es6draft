/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.intl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.TimeZone.SystemTimeZoneType;

/**
 * Simple tools to generate the various language data for the intl package
 */
class IntlDataTools {

    public static void main(String[] args) throws Exception {
        // Path cldrMainDir = Paths.get("/tmp/cldr-2.0.0-core--main");
        // oldStyleLanguageTags(cldrMainDir);
        //
        // Path currencyFile = Paths.get("/tmp/iso_currency.xml");
        // currencyDigits(currencyFile);
        //
        // Path tzdataDir = Paths.get("/tmp/tzdata2013c.tar");
        // jdkTimezoneNames(tzdataDir);
        //
        // Path langSubtagReg = Paths.get("/language-subtag-registry.txt");
        // languageSubtagRegistry(langSubtagReg);
    }

    /**
     * {@link LanguageSubtagRegistryData}
     */
    static void languageSubtagRegistry(Path langSubtagReg) throws Exception {
        List<String> lines = Files.readAllLines(langSubtagReg, StandardCharsets.UTF_8);
        ArrayDeque<String> stack = new ArrayDeque<>(lines);

        List<Record> language = new ArrayList<>();
        List<Record> region = new ArrayList<>();
        List<Record> grandfathered = new ArrayList<>();
        List<Record> redundant = new ArrayList<>();

        List<Record> extlang = new ArrayList<>();
        List<Record> script = new ArrayList<>();
        List<Record> variant = new ArrayList<>();

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

        System.out.println("---");

        for (Record record : script) {
            assert record.has(Field.Prefix);
            System.out.printf("%s -> %s [%s]\n", record.get(Field.Subtag),
                    record.get(Field.PreferredValue), record.get(Field.Prefix));
        }
        System.out.println("---");

        for (Record record : extlang) {
            assert record.has(Field.Prefix);
            assert record.get(Field.Subtag).equals(record.get(Field.PreferredValue)) : record
                    .get(Field.Subtag);
            System.out.printf("map.put(\"%s\", \"%s\");\n", record.get(Field.Subtag),
                    record.get(Field.Prefix));
        }
        System.out.println("---");

        for (Record record : variant) {
            assert record.has(Field.Prefix);
            System.out.printf("%s -> %s [%s]\n", record.get(Field.Subtag),
                    record.get(Field.PreferredValue), record.get(Field.Prefix));
            System.out.printf("map.put(\"%s\", \"%s\");\n", record.get(Field.Subtag),
                    record.get(Field.PreferredValue));
        }
        System.out.println("---");

        for (Record record : region) {
            assert !record.has(Field.Prefix);
            System.out.printf("map.put(\"%s\", \"%s\");\n",
                    record.get(Field.Subtag).toLowerCase(Locale.ROOT),
                    record.get(Field.PreferredValue));
        }
        System.out.println("---");

        for (Record record : language) {
            assert !record.has(Field.Prefix);
            System.out.printf("map.put(\"%s\", \"%s\");\n", record.get(Field.Subtag),
                    record.get(Field.PreferredValue));
        }
        System.out.println("---");

        for (Record record : grandfathered) {
            assert !record.has(Field.Prefix);
            if (record.has(Field.PreferredValue)) {
                System.out.printf("map.put(\"%s\", \"%s\");\n",
                        record.get(Field.Tag).toLowerCase(Locale.ROOT),
                        record.get(Field.PreferredValue));
            } else {
                System.out.printf("map.put(\"%s\", \"%s\");\n",
                        record.get(Field.Tag).toLowerCase(Locale.ROOT), record.get(Field.Tag));
            }
        }
        System.out.println("---");

        for (Record record : redundant) {
            assert !record.has(Field.Prefix);
            System.out.printf("map.put(\"%s\", \"%s\");\n",
                    record.get(Field.Tag).toLowerCase(Locale.ROOT),
                    record.get(Field.PreferredValue));
        }
        System.out.println("---");
    }

    private enum Field {
        Type("Type"), Tag("Tag"), Subtag("Subtag"), Description("Description"), Added("Added"),
        Deprecated("Deprecated"), PreferredValue("Preferred-Value"), Prefix("Prefix"),
        SupressScript("Suppress-Script"), Macrolanguage("Macrolanguage"), Scope("Scope"), Comments(
                "Comments");

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

    private static class Record {
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
            if (line.equals("%%")) {
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
     */
    static void jdkTimezoneNames(Path tzdataDir) throws Exception {
        Pattern pZone = Pattern.compile("Zone\\s+([a-zA-Z0-9_+\\-/]+)\\s+.*");
        Pattern pLink = Pattern
                .compile("Link\\s+([a-zA-Z0-9_+\\-/]+)\\s+([a-zA-Z0-9_+\\-/]+)(?:\\s+#.*)?");
        Pattern pFileName = Pattern.compile("[a-z0-9]+");

        TreeSet<String> names = new TreeSet<>();
        TreeMap<String, String> links = new TreeMap<>();

        Path dir = Paths.get("C:\\Users\\André\\Downloads\\tzdata2013c.tar");
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path path : stream) {
                String filename = path.getFileName().toString();
                if (pFileName.matcher(filename).matches()) {
                    try (BufferedReader reader = Files.newBufferedReader(path,
                            StandardCharsets.UTF_8)) {
                        for (String line; (line = reader.readLine()) != null;) {
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
                                assert changed : line;
                            }
                        }
                    }
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

        Set<String> ids = TimeZone.getAvailableIDs(SystemTimeZoneType.ANY, null, null);
        ids = new TreeSet<String>(ids);
        for (String id : new HashSet<>(ids)) {
            if (id.startsWith("SystemV/")) {
                ids.remove(id);
            }
        }

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
     */
    static void currencyDigits(Path currencyFile) throws Exception {
        try (Reader reader = Files.newBufferedReader(currencyFile, StandardCharsets.UTF_8)) {
            LinkedHashMap<String, Integer> map = new LinkedHashMap<>();
            Document xml = xml(reader);
            NodeList list = xml.getDocumentElement().getElementsByTagName("ISO_CURRENCY");
            for (int i = 0, len = list.getLength(); i < len; ++i) {
                Element item = (Element) list.item(i);
                Element code = getElementByTagName(item, "ALPHABETIC_CODE");
                Element minor = getElementByTagName(item, "MINOR_UNIT");
                String scode = code.getTextContent();
                int iminor = 2;
                try {
                    iminor = Integer.parseInt(minor.getTextContent());
                } catch (NumberFormatException e) {
                }
                if (map.containsKey(scode) && map.get(scode) != iminor) {
                    System.out.println(scode);
                }
                if (iminor != 2 && !map.containsKey(scode)) {
                    map.put(scode, iminor);
                }
            }
            TreeMap<Integer, List<String>> sorted = new TreeMap<>();
            for (Map.Entry<String, Integer> entry : map.entrySet()) {
                List<String> currencies = sorted.get(entry.getValue());
                if (currencies == null) {
                    currencies = new ArrayList<>();
                }
                currencies.add(entry.getKey());
                sorted.put(entry.getValue(), currencies);
            }
            for (Map.Entry<Integer, List<String>> entry : sorted.entrySet()) {
                for (String c : entry.getValue()) {
                    System.out.printf("case \"%s\":\n", c);
                }
                System.out.printf("    return %d;\n", entry.getKey());
            }
            System.out.println("default:\n    return 2;");
        }
    }

    /**
     * {@link IntlAbstractOperations#oldStyleLanguageTags}
     */
    static void oldStyleLanguageTags(Path cldrMainDir) throws Exception {
        try (DirectoryStream<Path> newDirectoryStream = Files.newDirectoryStream(cldrMainDir)) {
            Map<String, String> names = new LinkedHashMap<>();
            Map<String, String> aliased = new LinkedHashMap<>();
            for (Path path : newDirectoryStream) {
                try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                    Document xml = xml(reader);
                    Element identity = getElementByTagName(xml.getDocumentElement(), "identity");
                    assert identity != null;
                    Element language = getElementByTagName(xml.getDocumentElement(), "language");
                    Element script = getElementByTagName(xml.getDocumentElement(), "script");
                    Element territory = getElementByTagName(xml.getDocumentElement(), "territory");

                    String tag = language.getAttribute("type");
                    if (script != null) {
                        tag += "-" + script.getAttribute("type");
                    }
                    if (territory != null) {
                        tag += "-" + territory.getAttribute("type");
                    }

                    String filename = path.getFileName().toString();
                    filename = filename.substring(0, filename.lastIndexOf('.'));
                    names.put(filename, tag);

                    Element alias = getElementByTagName(xml.getDocumentElement(), "alias");
                    if (alias != null && script == null && territory != null) {
                        aliased.put(tag, alias.getAttribute("source"));
                    }
                }
            }
            Map<String, String> result = new LinkedHashMap<>();
            for (Map.Entry<String, String> entry : aliased.entrySet()) {
                String from = entry.getKey();
                String to = names.get(entry.getValue());

                String value = result.get(to);
                if (value == null) {
                    value = "";
                } else {
                    value += ", ";
                }
                value += "\"" + from + "\"";
                result.put(to, value);
            }

            for (Map.Entry<String, String> entry : result.entrySet()) {
                System.out.printf("map.put(\"%s\", new String[]{%s});%n", entry.getKey(),
                        entry.getValue());
            }
        }
    }

    private static Element getElementByTagName(Element element, String tagName) {
        return (Element) element.getElementsByTagName(tagName).item(0);
    }

    private static Document xml(Reader xml) throws IOException {
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
            Document doc = builder.parse(source);
            return doc;
        } catch (ParserConfigurationException | SAXException e) {
            throw new IOException(e);
        }
    }
}
