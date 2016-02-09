/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler.assembler;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * 
 */
public final class VariablesSnapshot implements Iterable<Variable<?>> {
    private final int startSlot;
    private final BitSet variables;
    private final BitSet active;
    private final Type[] types;

    VariablesSnapshot(int startSlot, BitSet variables, BitSet active, Type[] types) {
        this.startSlot = startSlot;
        this.variables = variables;
        this.active = active;
        this.types = types;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != VariablesSnapshot.class) {
            return false;
        }
        VariablesSnapshot other = (VariablesSnapshot) obj;
        return startSlot == other.startSlot && variables.equals(other.variables) && active.equals(other.active)
                && Arrays.equals(types, other.types);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + startSlot;
        result = prime * result + variables.hashCode();
        result = prime * result + active.hashCode();
        result = prime * result + Arrays.hashCode(types);
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append("\n\tstartSlot=").append(startSlot);
        sb.append("\n\tvariables=").append(variables);
        sb.append("\n\tactive=").append(active);
        sb.append("\n\ttypes=").append(Arrays.toString(types));
        return sb.toString();
    }

    int getStartSlot() {
        return startSlot;
    }

    BitSet getVariables() {
        return variables;
    }

    BitSet getActive() {
        return active;
    }

    Type[] getTypes() {
        return types;
    }

    private Variable<?> getVariable(int slot) {
        assert 0 <= slot && slot <= types.length : String.format("slot=%d not in [%d, %d]", slot, 0, types.length);
        assert variables.get(slot) && types[slot] != null && types[slot] != Type.RESERVED : String
                .format("slot=%d, used=%b, type=%s", slot, variables.get(slot), types[slot]);
        return new Variable<>(null, types[slot], slot);
    }

    private int getNextInitializedSlot(int slot) {
        assert 0 <= slot && slot <= types.length : String.format("slot=%d not in [%d, %d]", slot, 0, types.length);
        assert variables.get(slot) && active.get(slot) && types[slot] != null && types[slot] != Type.RESERVED : String
                .format("slot=%d, used=%b, active=%b, type=%s", slot, variables.get(slot), active.get(slot),
                        types[slot]);
        return active.nextSetBit(slot + types[slot].getSize());
    }

    /**
     * Returns the number of initialized variables.
     * 
     * @return the total variable count
     */
    public int getSize() {
        return active.cardinality();
    }

    @Override
    public Iterator<Variable<?>> iterator() {
        return new SnapshotIterator();
    }

    private final class SnapshotIterator implements Iterator<Variable<?>> {
        private int slot = active.nextSetBit(startSlot);

        @Override
        public boolean hasNext() {
            return slot != -1;
        }

        @Override
        public Variable<?> next() {
            if (slot == -1) {
                throw new NoSuchElementException();
            }
            Variable<?> v = getVariable(slot);
            slot = getNextInitializedSlot(slot);
            return v;
        }
    }
}
