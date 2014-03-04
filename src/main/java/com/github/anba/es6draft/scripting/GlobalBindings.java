/**
 * Copyright (c) 2012-2014 André Bargull
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

import com.github.anba.es6draft.runtime.EnvironmentRecord;
import com.github.anba.es6draft.runtime.objects.GlobalObject;

/**
 *
 */
final class GlobalBindings implements Bindings {
    private final GlobalObject globalObject;
    private final EnvironmentRecord envRec;

    public GlobalBindings(GlobalObject globalObject) {
        this.globalObject = globalObject;
        this.envRec = globalObject.getRealm().getGlobalEnv().getEnvRec();
    }

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

    GlobalObject getGlobalObject() {
        return globalObject;
    }

    @Override
    public Object put(String key, Object value) {
        String name = toBindingName(key);
        Object oldValue = getOrNull(name);
        envRec.setMutableBinding(name, fromJava(value), false);
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
        return envRec.hasBinding(name);
    }

    @Override
    public Object get(Object key) {
        String name = toBindingName(key);
        Object value = envRec.getBindingValue(name, false);
        return toJava(value);
    }

    @Override
    public Object remove(Object key) {
        String name = toBindingName(key);
        Object oldValue = getOrNull(name);
        envRec.deleteBinding(name);
        return oldValue;
    }

    @Override
    public Set<String> keySet() {
        return Collections.unmodifiableSet(envRec.bindingNames());
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
