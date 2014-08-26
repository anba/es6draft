/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.modules;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * <h1>15 ECMAScript Language: Scripts and Modules</h1><br>
 * <h2>15.2 Modules</h2><br>
 * <h3>15.2.5 Runtime Semantics: Module Linking</h3>
 * <ul>
 * <li>15.2.5.3 Module Linking Groups
 * </ul>
 */
public final class ModuleLinkingGroups {
    private ModuleLinkingGroups() {
    }

    /**
     * 15.2.5.3.1 LinkageGroups ( start )
     * 
     * @param start
     *            the list of initial load records
     * @return the list of linkage groups
     */
    public static List<List<Load>> LinkageGroups(List<Load> start) {
        // TODO: implement
        /* step 1 */
        /* step 2 */
        /* step 3 */
        /* step 4 */
        int declarativeGroupCount = 0;
        /* step 5 */
        List<List<Load>> declarativeGroups = newList(declarativeGroupCount);
        /* step 6 */
        int dynamicGroupCount = 0;
        /* step 7 */
        List<List<Load>> dynamicGroups = newList(dynamicGroupCount);
        /* step 8 */
        HashSet<String> visited = new HashSet<>();
        /* step 9 */
        for (Load load : start) {
            BuildLinkageGroups(load, declarativeGroups, dynamicGroups, visited);
        }
        /* step 10 */
        ArrayList<List<Load>> groups = new ArrayList<>();
        /* step 11 */
        return groups;
    }

    /**
     * 15.2.5.3.2 BuildLinkageGroups ( load, declarativeGroups, dynamicGroups, visited )
     * 
     * @param load
     *            the load record
     * @param declarativeGroups
     *            ?
     * @param dynamicGroups
     *            ?
     * @param visited
     *            the set of already visited modules
     */
    public static void BuildLinkageGroups(Load load, Object declarativeGroups,
            Object dynamicGroups, Set<String> visited) {
        // TODO: unsafe getName() - maybe null?
        // TODO: implement
        /* step 1 */
        if (visited.contains(load.getName())) {
            return;
        }
        /* step 2 */
        visited.add(load.getName());
        /* step 3 */
        // FIXME: spec bug: [[UnlinkedDependencies]] -> [[Dependencies]]
    }

    private static <T> List<List<T>> newList(int count) {
        ArrayList<List<T>> list = new ArrayList<>(count);
        for (int i = 0; i < count; ++i) {
            list.add(new ArrayList<T>());
        }
        return list;
    }
}
