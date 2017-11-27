/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.intl;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;
import com.ibm.icu.impl.ICUData;
import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.text.ListFormatter;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;

/**
 * <h1>ListFormat Objects</h1>
 * <ul>
 * <li>Properties of Intl.ListFormat Instances
 * </ul>
 */
public final class ListFormatObject extends OrdinaryObject {
    /** [[Locale]] */
    private String locale;

    /** [[Type]] */
    private String type;

    /** [[Style]] */
    private String style;

    private ListFormatter listFormatter;

    /**
     * Constructs a new ListFormat object.
     * 
     * @param realm
     *            the realm object
     */
    public ListFormatObject(Realm realm) {
        super(realm);
    }

    /**
     * Returns the ICU {@link ListFormatter} instance.
     * 
     * @return the ListFormatter instance
     */
    public ListFormatter getListFormatter() {
        if (listFormatter == null) {
            listFormatter = createListFormatter();
        }
        return listFormatter;
    }

    @SuppressWarnings("deprecation")
    private ListFormatter createListFormatter() {
        ULocale locale = ULocale.forLanguageTag(this.locale);
        ICUResourceBundle r = (ICUResourceBundle) UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, locale);
        String resourceStyle = resourceStyle();
        return new ListFormatter(r.getWithFallback("listPattern/" + resourceStyle + "/2").getString(),
                r.getWithFallback("listPattern/" + resourceStyle + "/start").getString(),
                r.getWithFallback("listPattern/" + resourceStyle + "/middle").getString(),
                r.getWithFallback("listPattern/" + resourceStyle + "/end").getString());
    }

    private String resourceStyle() {
        if ("regular".equals(type)) {
            switch (style) {
            case "long":
                return "standard";
            case "short":
            case "narrow":
                return "standard-short";
            default:
                throw new AssertionError();
            }
        }
        assert "unit".equals(type);
        switch (style) {
        case "long":
            return "unit";
        case "short":
            return "unit-short";
        case "narrow":
            return "unit-narrow";
        default:
            throw new AssertionError();
        }
    }

    /**
     * [[Locale]]
     * 
     * @return the locale
     */
    public String getLocale() {
        return locale;
    }

    /**
     * [[Locale]]
     * 
     * @param locale
     *            the new locale
     */
    public void setLocale(String locale) {
        this.locale = locale;
    }

    /**
     * [[Type]]
     * 
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * [[Type]]
     * 
     * @param type
     *            the new type
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * [[Style]]
     * 
     * @return the style
     */
    public String getStyle() {
        return style;
    }

    /**
     * [[Style]]
     * 
     * @param style
     *            the new style
     */
    public void setStyle(String style) {
        this.style = style;
    }

    @Override
    public String toString() {
        return String.format("%s, locale=%s, type=%s, style=%s, listFormatter=%s", super.toString(), locale, type,
                style, listFormatter);
    }
}
