/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.scripting;

import static com.github.anba.es6draft.scripting.TypeConverter.fromJava;
import static com.github.anba.es6draft.scripting.TypeConverter.toJava;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.script.Bindings;

import com.github.anba.es6draft.runtime.GlobalEnvironmentRecord;
import com.github.anba.es6draft.runtime.objects.GlobalObject;

/**
 * Concrete implementation of the {@link Bindings} interface.
 */
final class GlobalBindings implements Bindings {
    private final GlobalObject globalObject;
    private final GlobalEnvironmentRecord globalEnvRec;

    public GlobalBindings(GlobalObject globalObject) {
        this.globalObject = globalObject;
        this.globalEnvRec = globalObject.getRealm().getGlobalEnv().getEnvRec();
    }

    /**
     * Converts <var>key</var> to a binding name.
     * 
     * @param key
     *            the key
     * @return the binding name
     * @throws ClassCastException
     *             if <var>key</var> is not a {@link String}
     * @throws NullPointerException
     *             if <var>key</var> is {@code null}
     * @throws IllegalArgumentException
     *             if <var>key</var> is the empty string
     */
    private static String toBindingName(Object key) {
        String name = (String) key;
        if (name.isEmpty()) {
            throw new IllegalArgumentException();
        }
        return name;
    }

    private Object getOrNull(String name) {
        return containsKey(name) ? get(name) : null;
    }

    /**
     * Returns the global object this {@link Bindings} instance is bound to.
     * 
     * @return the global object
     */
    GlobalObject getGlobalObject() {
        return globalObject;
    }

    @Override
    public Object put(String key, Object value) {
        String name = toBindingName(key);
        Object oldValue = getOrNull(name);
        globalEnvRec.setMutableBinding(name, fromJava(value), false);
        return oldValue;
    }

    @Override
    public void putAll(Map<? extends String, ? extends Object> toMerge) {
        for (Map.Entry<? extends String, ? extends Object> entry : toMerge.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public boolean containsKey(Object key) {
        String name = toBindingName(key);
        return globalEnvRec.hasBinding(name);
    }

    @Override
    public Object get(Object key) {
        String name = toBindingName(key);
        Object value = globalEnvRec.getBindingValue(name, false);
        return toJava(value);
    }

    @Override
    public Object remove(Object key) {
        String name = toBindingName(key);
        Object oldValue = getOrNull(name);
        globalEnvRec.deleteBinding(name);
        return oldValue;
    }

    @Override
    public Set<String> keySet() {
        return Collections.unmodifiableSet(globalEnvRec.bindingNames());
    }

    @Override
    public int size() {
        return keySet().size();
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean containsValue(Object value) {
        return values().contains(value);
    }

    @Override
    public void clear() {
        for (String key : keySet()) {
            remove(key);
        }
    }

    @Override
    public Collection<Object> values() {
        ArrayList<Object> values = new ArrayList<>();
        for (String key : keySet()) {
            values.add(get(key));
        }
        return Collections.unmodifiableList(values);
    }

    @Override
    public Set<Map.Entry<String, Object>> entrySet() {
        HashSet<Map.Entry<String, Object>> entries = new HashSet<>();
        for (String key : keySet()) {
            entries.add(new SimpleImmutableEntry<>(key, get(key)));
        }
        return Collections.unmodifiableSet(entries);
    }
}
