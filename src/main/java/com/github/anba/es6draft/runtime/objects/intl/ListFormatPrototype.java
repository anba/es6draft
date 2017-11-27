/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.intl;

import static com.github.anba.es6draft.runtime.AbstractOperations.CreateDataProperty;
import static com.github.anba.es6draft.runtime.AbstractOperations.CreateListFromArrayLike;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToFlatString;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.types.builtins.ArrayObject.ArrayCreate;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.ArrayObject;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;
import com.ibm.icu.text.SimpleFormatter;

/**
 * <h1>ListFormat Objects</h1>
 * <ul>
 * <li>Properties of the Intl.ListFormat Prototype Object
 * </ul>
 */
// FIXME: spec issue - prototype should be ordinary object
public final class ListFormatPrototype extends OrdinaryObject implements Initializable {
    /**
     * Constructs a new ListFormat prototype object.
     * 
     * @param realm
     *            the realm object
     */
    public ListFormatPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialize(Realm realm) {
        createProperties(realm, this, Properties.class);
    }

    private static List<String> InvokeToLocaleString(ExecutionContext cx, ListFormatObject listFormat,
            List<Object> list) {
        ArrayList<String> formattedList = new ArrayList<>();
        for (Object element : list) {
            if (!Type.isUndefinedOrNull(element)) {
                // TODO: spec issue - toLocaleString() problematic when default number format is non-latin, e.g. arabic
                formattedList.add(ToFlatString(cx, element));
            }
        }
        return formattedList;
    }

    /**
     * FormatList(listFormat, list)
     * 
     * @param cx
     *            the execution context
     * @param listFormat
     *            the ListFormat object
     * @param list
     *            the list argument
     * @return the formatted string
     */
    public static String FormatList(ExecutionContext cx, ListFormatObject listFormat, List<Object> list) {
        List<String> formattedList = InvokeToLocaleString(cx, listFormat, list);
        return listFormat.getListFormatter().format(formattedList);
    }

    /**
     * FormatListToParts(listFormat, list)
     * 
     * @param cx
     *            the execution context
     * @param listFormat
     *            the ListFormat object
     * @param list
     *            the list argument
     * @return the formatted string parts
     */
    public static ArrayObject FormatListToParts(ExecutionContext cx, ListFormatObject listFormat, List<Object> list) {
        List<String> formattedList = InvokeToLocaleString(cx, listFormat, list);
        List<Map.Entry<String, String>> parts = CreatePartsFromList(listFormat, formattedList);
        ArrayObject result = ArrayCreate(cx, 0);
        int n = 0;
        for (Map.Entry<String, String> part : parts) {
            OrdinaryObject o = ObjectCreate(cx, Intrinsics.ObjectPrototype);
            CreateDataProperty(cx, o, "type", part.getKey());
            CreateDataProperty(cx, o, "value", part.getValue());
            CreateDataProperty(cx, result, n++, o);
        }
        return result;
    }

    /**
     * CreatePartsFromList (listFormat, list)
     * 
     * @param listFormat
     *            the ListFormat object
     * @param list
     *            the list argument
     * @return the list parts
     */
    private static List<Map.Entry<String, String>> CreatePartsFromList(ListFormatObject listFormat, List<String> list) {
        if (list.size() == 0) {
            return Collections.emptyList();
        }

        String pattern = listFormat.getListFormatter().getPatternForNumItems(list.size());
        SimpleFormatter formatter = SimpleFormatter.compile(pattern);
        int[] offsets = new int[list.size()];
        String result = formatter.formatAndAppend(new StringBuilder(), offsets, list.toArray(new String[0])).toString();

        ArrayList<Map.Entry<String, String>> parts = new ArrayList<>();
        int lastOffset = 0;
        for (int i = 0; i < offsets.length; i++) {
            int offset = offsets[i];
            if (offset != lastOffset) {
                parts.add(new SimpleImmutableEntry<>("literal", result.substring(lastOffset, offset)));
            }
            String element = list.get(i);
            parts.add(new SimpleImmutableEntry<>("element", element));
            lastOffset = offset + element.length();
        }
        if (lastOffset != result.length()) {
            parts.add(new SimpleImmutableEntry<>("literal", result.substring(lastOffset)));
        }

        return parts;
    }

    /**
     * Properties of the Intl.ListFormat Prototype Object
     */
    public enum Properties {
        ;

        private static ListFormatObject thisListFormatObject(ExecutionContext cx, Object value, String method) {
            if (value instanceof ListFormatObject) {
                return (ListFormatObject) value;
            }
            throw newTypeError(cx, Messages.Key.IncompatibleThis, method, Type.of(value).toString());
        }

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * Intl.ListFormat.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.Intl_ListFormat;

        /**
         * Intl.ListFormat.prototype[@@toStringTag]
         */
        @Value(name = "[Symbol.toStringTag]", symbol = BuiltinSymbol.toStringTag,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final String toStringTag = "Object";

        /**
         * Intl.ListFormat.prototype.format ([ list ])
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param list
         *            the list argument
         * @return the formatted string
         */
        @Function(name = "format", arity = 0)
        public static Object format(ExecutionContext cx, Object thisValue, Object list) {
            /* steps 1-3 */
            ListFormatObject listFormat = thisListFormatObject(cx, thisValue, "Intl.ListFormat.prototype.format");
            /* step 4 */
            if (Type.isUndefined(list)) {
                return "";
            }
            /* step 5 */
            List<Object> x = Arrays.asList(CreateListFromArrayLike(cx, list));
            /* step 6 */
            return FormatList(cx, listFormat, x);
        }

        /**
         * Intl.ListFormat.prototype.formatToParts ([ list ])
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param list
         *            the list argument
         * @return the formatted string
         */
        @Function(name = "formatToParts", arity = 0)
        public static Object formatToParts(ExecutionContext cx, Object thisValue, Object list) {
            /* steps 1-3 */
            ListFormatObject listFormat = thisListFormatObject(cx, thisValue,
                    "Intl.ListFormat.prototype.formatToParts");
            /* step 4 */
            if (Type.isUndefined(list)) {
                return ArrayCreate(cx, 0);
            }
            /* step 5 */
            List<Object> x = Arrays.asList(CreateListFromArrayLike(cx, list));
            /* step 6 */
            return FormatListToParts(cx, listFormat, x);
        }

        /**
         * Intl.ListFormat.prototype.resolvedOptions ()
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the resolved options object
         */
        @Function(name = "resolvedOptions", arity = 0)
        public static Object resolvedOptions(ExecutionContext cx, Object thisValue) {
            ListFormatObject listFormat = thisListFormatObject(cx, thisValue,
                    "Intl.ListFormat.prototype.resolvedOptions");
            OrdinaryObject object = ObjectCreate(cx, Intrinsics.ObjectPrototype);
            CreateDataProperty(cx, object, "locale", listFormat.getLocale());
            CreateDataProperty(cx, object, "type", listFormat.getType());
            CreateDataProperty(cx, object, "style", listFormat.getStyle());
            return object;
        }
    }
}
