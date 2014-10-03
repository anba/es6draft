/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler.assembler;

import org.objectweb.asm.Type;

/**
 * Field descriptor object.
 */
public final class FieldDesc {
    public enum Allocation {
        Instance, Static
    }

    /**
     * The field allocation type.
     */
    public final FieldDesc.Allocation allocation;

    /**
     * Type descriptor of the declaring class.
     */
    public final String owner;

    /**
     * The field name.
     */
    public final String name;

    /**
     * Type descriptor of the field.
     */
    public final String desc;

    private FieldDesc(FieldDesc.Allocation allocation, String owner, String name, String desc) {
        this.allocation = allocation;
        this.owner = owner;
        this.name = name;
        this.desc = desc;
    }

    /**
     * Creates a new field descriptor.
     * 
     * @param type
     *            the field type
     * @param owner
     *            the owner class
     * @param name
     *            the field name
     * @param desc
     *            the field type descriptor
     * @return the field descriptor
     */
    public static FieldDesc create(FieldDesc.Allocation type, Type owner, String name, Type desc) {
        return new FieldDesc(type, owner.getInternalName(), name, desc.getDescriptor());
    }
}
