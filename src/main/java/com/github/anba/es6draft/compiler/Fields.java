/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import org.objectweb.asm.Type;

import com.github.anba.es6draft.compiler.InstructionVisitor.FieldDesc;

/**
 *
 */
final class Fields {
    private Fields() {
    }

    static final FieldDesc Double_NaN = FieldDesc.create(Types.Double, "NaN", Type.DOUBLE_TYPE);

    static final FieldDesc Null_NULL = FieldDesc.create(Types.Null, "NULL", Types.Null);

    static final FieldDesc Undefined_UNDEFINED = FieldDesc.create(Types.Undefined, "UNDEFINED",
            Types.Undefined);

    static final FieldDesc ScriptRuntime_EMPTY_ARRAY = FieldDesc.create(Types.ScriptRuntime,
            "EMPTY_ARRAY", Types.Object_);
}
