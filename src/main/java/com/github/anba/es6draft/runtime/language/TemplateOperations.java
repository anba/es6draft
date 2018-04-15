/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.language;

import static com.github.anba.es6draft.runtime.AbstractOperations.SetIntegrityLevel;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.ArrayObject.ArrayCreate;

import java.lang.invoke.MethodHandle;
import java.util.Map;

import com.github.anba.es6draft.compiler.CompiledObject;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.types.IntegrityLevel;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.builtins.ArrayObject;

/**
 * 
 */
public final class TemplateOperations {
    private TemplateOperations() {
    }

    /**
     * 12.2.9 Template Literals
     * <p>
     * 12.2.9.3 Runtime Semantics: GetTemplateObject ( templateLiteral )
     * 
     * @param key
     *            the template literal key
     * @param handle
     *            the method handle for the template literal data
     * @param cx
     *            the execution context
     * @return the template call site object
     */
    public static ArrayObject GetTemplateObject(int key, MethodHandle handle, ExecutionContext cx) {
        assert cx.getCurrentExecutable() instanceof CompiledObject : cx.getCurrentExecutable();
        CompiledObject compiledObject = (CompiledObject) cx.getCurrentExecutable();
        ArrayObject template = compiledObject.getTemplateObject(key);
        if (template == null) {
            template = GetTemplateObject(handle, cx);
            compiledObject.setTemplateObject(key, template);
        }
        return template;
    }

    /**
     * 12.2.9 Template Literals
     * <p>
     * 12.2.9.3 Runtime Semantics: GetTemplateObject ( templateLiteral )
     * 
     * @param handle
     *            the method handle for the template literal data
     * @param cx
     *            the execution context
     * @return the template call site object
     */
    private static ArrayObject GetTemplateObject(MethodHandle handle, ExecutionContext cx) {
        /* steps 1, 5 */
        String[] strings = evaluateTemplateStrings(handle);
        assert (strings.length & 1) == 0;

        boolean templateParseNodeCache = cx.getRuntimeContext().isEnabled(CompatibilityOption.TemplateParseNodeCache);
        Map<String, ArrayObject> templateRegistry;
        String templateKey;
        if (!templateParseNodeCache) {
            /* steps 2-3 */
            templateRegistry = cx.getRealm().getTemplateMap();
            /* step 4 */
            templateKey = templateStringKey(strings);
            if (templateRegistry.containsKey(templateKey)) {
                return templateRegistry.get(templateKey);
            }
        } else {
            templateRegistry = null;
            templateKey = null;
        }

        /* step 6 */
        int count = strings.length >>> 1;
        /* steps 7-8 */
        ArrayObject template = ArrayCreate(cx, count);
        ArrayObject rawObj = ArrayCreate(cx, count);
        /* steps 9-10 */
        for (int i = 0, n = strings.length; i < n; i += 2) {
            int index = i >>> 1;
            int prop = index;
            String cookedString = strings[i];
            Object cookedValue = cookedString != null ? cookedString : UNDEFINED;
            template.defineOwnProperty(cx, prop, new PropertyDescriptor(cookedValue, false, true, false));
            String rawValue = strings[i + 1];
            assert rawValue != null;
            rawObj.defineOwnProperty(cx, prop, new PropertyDescriptor(rawValue, false, true, false));
        }
        /* steps 11-13 */
        SetIntegrityLevel(cx, rawObj, IntegrityLevel.Frozen);
        template.defineOwnProperty(cx, "raw", new PropertyDescriptor(rawObj, false, false, false));
        SetIntegrityLevel(cx, template, IntegrityLevel.Frozen);
        /* step 14 */
        if (!templateParseNodeCache) {
            templateRegistry.put(templateKey, template);
        }
        /* step 15 */
        return template;
    }

    private static String templateStringKey(String[] strings) {
        assert (strings.length & 1) == 0;
        StringBuilder raw = new StringBuilder();
        for (int i = 0, n = strings.length; i < n; i += 2) {
            // Template string normalization removes any \r character in the source string, so it's
            // safe to use that character as a delimiter here.
            String rawValue = strings[i + 1];
            raw.append(rawValue).append('\r');
        }
        return raw.toString();
    }

    private static String[] evaluateTemplateStrings(MethodHandle handle) {
        try {
            return (String[]) handle.invokeExact();
        } catch (Throwable e) {
            throw TemplateOperations.<RuntimeException> rethrow(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> E rethrow(Throwable e) throws E {
        throw (E) e;
    }
}
