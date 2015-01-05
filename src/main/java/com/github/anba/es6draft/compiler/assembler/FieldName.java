/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler.assembler;

/**
 * Field descriptor object.
 */
public final class FieldName {
    public enum Allocation {
        Instance, Static
    }

    /**
     * The field allocation type.
     */
    public final FieldName.Allocation allocation;

    /**
     * Type descriptor of the declaring class.
     */
    public final Type owner;

    /**
     * The field name.
     */
    public final String name;

    /**
     * Type descriptor of the field.
     */
    public final Type descriptor;

    private FieldName(FieldName.Allocation allocation, Type owner, String name, Type descriptor) {
        this.allocation = allocation;
        this.owner = owner;
        this.name = name;
        this.descriptor = descriptor;
    }

    /**
     * Creates a new field descriptor.
     * 
     * @param owner
     *            the owner class
     * @param name
     *            the field name
     * @param descriptor
     *            the field type descriptor
     * @return the field descriptor
     */
    public static FieldName findStatic(Type owner, String name, Type descriptor) {
        return new FieldName(Allocation.Static, owner, name, descriptor);
    }

    /**
     * Creates a new field descriptor.
     * 
     * @param owner
     *            the owner class
     * @param name
     *            the field name
     * @param descriptor
     *            the field type descriptor
     * @return the field descriptor
     */
    public static FieldName findField(Type owner, String name, Type descriptor) {
        return new FieldName(Allocation.Instance, owner, name, descriptor);
    }
}
