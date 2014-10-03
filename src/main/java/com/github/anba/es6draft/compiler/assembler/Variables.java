/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler.assembler;

import java.util.Arrays;
import java.util.BitSet;

import org.objectweb.asm.Type;

/**
 * 
 */
public final class Variables {
    private static final Type RESERVED = Type.getType("reserved");
    private static final int INITIAL_SIZE = 8;
    private final BitSet variables = new BitSet();
    private final BitSet active = new BitSet();
    private Type[] types = new Type[INITIAL_SIZE];
    private VariableScope varScope = null;
    private int varCount = 0;

    private static Type[] grow(Type[] types) {
        int newLength = types.length + (types.length >>> 1);
        return Arrays.copyOf(types, newLength, Type[].class);
    }

    private void assign(int slot, Type type) {
        if (type.getSize() == 1) {
            types[slot] = type;
            variables.set(slot);
        } else {
            types[slot] = type;
            types[slot + 1] = RESERVED;
            variables.set(slot, slot + 2);
        }
    }

    /**
     * Returns the type information for the requested local variable.
     * 
     * @param slot
     *            the variable index slot
     * @return the variable type
     */
    Type getVariable(int slot) {
        assert 0 <= slot && slot <= types.length : String.format("slot=%d not in [%d, %d]", slot,
                0, types.length);
        assert variables.get(slot) && types[slot] != null && types[slot] != RESERVED : String
                .format("slot=%d, used=%b, type=%s", slot, variables.get(slot), types[slot]);
        return types[slot];
    }

    VariablesSnapshot snapshot() {
        return new VariablesSnapshot((BitSet) variables.clone(), (BitSet) active.clone(),
                Arrays.copyOf(types, types.length));
    }

    void restore(VariablesSnapshot snapshot) {
        variables.clear();
        variables.or(snapshot.getVariables());
        active.clear();
        active.or(snapshot.getActive());
        types = Arrays.copyOf(snapshot.getTypes(), snapshot.getTypes().length);
    }

    VariableScope enter() {
        return varScope = new VariableScope(varScope, variables.length());
    }

    VariableScope exit() {
        VariableScope scope = varScope;
        varScope = scope.parent;
        // clear stored variable type info
        for (Variable<?> v : scope) {
            v.free();
        }
        Arrays.fill(types, scope.firstSlot, types.length, null);
        variables.clear(scope.firstSlot, variables.size());
        active.clear(scope.firstSlot, active.size());
        return scope;
    }

    void close() {
        assert varScope == null : "unclosed variable scopes";
    }

    void activate(int slot) {
        active.set(slot);
    }

    boolean isActive(int slot) {
        return active.get(slot);
    }

    /**
     * Adds a new entry to the variable map.
     * 
     * @param <T>
     *            the variable class type
     * @param name
     *            the variable name
     * @param type
     *            the variable type
     * @param slot
     *            the variable slot
     * @return the new variable object
     */
    <T> Variable<T> addVariable(String name, Type type, int slot) {
        assert variables.get(slot) && types[slot].equals(type);
        return varScope.add(name, type, slot);
    }

    /**
     * Sets the variable information for a fixed slot.
     * 
     * @param type
     *            the variable type
     * @param slot
     *            the variable slot
     */
    void reserveSlot(Type type, int slot) {
        assert slot >= 0;
        if (slot + type.getSize() > types.length) {
            types = grow(types);
        }
        assert types[slot] == null && (type.getSize() == 1 || types[slot + 1] == null);
        assign(slot, type);
        activate(slot);
    }

    /**
     * Sets the variable information for a fixed slot and adds an entry to the variable map.
     * 
     * @param <T>
     *            the variable class type
     * @param name
     *            the variable name
     * @param type
     *            the variable type
     * @param slot
     *            the variable slot
     * @return the variable object
     */
    <T> Variable<T> reserveSlot(String name, Type type, int slot) {
        reserveSlot(type, slot);
        return varScope.add(name, type, slot);
    }

    /**
     * Creates a new variable and adds it to the variable map.
     * 
     * @param <T>
     *            the variable class type
     * @param name
     *            the variable name
     * @param type
     *            the variable type
     * @return the new variable object
     */
    <T> Variable<T> newVariable(String name, Type type) {
        int slot = newVariable(type);
        if (slot < 0) {
            // reusing an existing slot
            return varScope.add("<shared>", type, slot);
        }
        if (name == null) {
            // null-name indicates scratch variable, don't create variable map entry
            return varScope.add("<scratch>", type, -slot);
        }
        String uniqueName = name + "$" + varCount++;
        return varScope.add(uniqueName, type, slot);
    }

    /**
     * Marks the given variable as no longer being used, only applicable for scratch variables.
     * 
     * @param variable
     *            the variable to be freed
     */
    void freeVariable(Variable<?> variable) {
        int slot = variable.getSlot();
        assert variable.isAlive();
        assert "<scratch>".equals(variable.getName()) || "<shared>".equals(variable.getName());
        assert variables.get(slot);
        assert types[slot].equals(variable.getType());
        variables.clear(slot);
        active.clear(slot);
        variable.free();
    }

    private int newVariable(Type type) {
        for (int slot = varScope.firstSlot, size = type.getSize();;) {
            slot = variables.nextClearBit(slot);
            if (slot + size > types.length) {
                types = grow(types);
            }
            Type old = types[slot];
            if (old == null) {
                if (size == 1 || types[slot + 1] == null) {
                    assign(slot, type);
                    return slot;
                }
            } else if (old.equals(type)) {
                assert size == 1 || types[slot + 1] == RESERVED;
                assign(slot, type);
                return -slot;
            }
            // try next index
            slot += 1;
        }
    }
}
