/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import com.github.anba.es6draft.compiler.assembler.Stack;
import com.github.anba.es6draft.compiler.assembler.Type;
import com.github.anba.es6draft.compiler.assembler.Variables;

/**
 * Application specific override of {@link Stack}.
 */
final class StackImpl extends Stack {
    public StackImpl(Variables variables) {
        super(variables);
    }

    @Override
    protected Type commonType(Type left, Type right) {
        // Hard coded type relationships to avoid dynamic class loading...
        Type commonType = Types.Object;
        if (isCharSequence(left)) {
            if (isCharSequence(right)) {
                commonType = Types.CharSequence;
            }
        } else if (isNumber(left)) {
            if (isNumber(right)) {
                commonType = Types.Number;
            }
        } else if (isFunctionObject(left)) {
            if (isFunctionObject(right)) {
                commonType = Types.FunctionObject;
            } else if (isOrdinaryObject(right)) {
                commonType = Types.OrdinaryObject;
            } else if (isScriptObject(right)) {
                commonType = Types.ScriptObject;
            }
        } else if (isOrdinaryObject(left)) {
            if (isFunctionObject(right) || isOrdinaryObject(right)) {
                commonType = Types.OrdinaryObject;
            } else if (isScriptObject(right)) {
                commonType = Types.ScriptObject;
            }
        } else if (isScriptObject(left)) {
            if (isFunctionObject(right) || isOrdinaryObject(right) || isScriptObject(right)) {
                commonType = Types.ScriptObject;
            }
        }
        return commonType;
    }

    private static boolean isCharSequence(Type type) {
        return Types.String.equals(type) || Types.CharSequence.equals(type);
    }

    private static boolean isNumber(Type type) {
        return Types.Integer.equals(type) || Types.Long.equals(type) || Types.Double.equals(type);
    }

    private static boolean isFunctionObject(Type type) {
        return Types.OrdinaryFunction.equals(type) || Types.OrdinaryGenerator.equals(type)
                || Types.OrdinaryAsyncFunction.equals(type) || Types.FunctionObject.equals(type);
    }

    private static boolean isOrdinaryObject(Type type) {
        return Types.OrdinaryObject.equals(type) || Types.ArrayObject.equals(type)
                || Types.ArgumentsObject.equals(type) || Types.LegacyArgumentsObject.equals(type)
                || Types.RegExpObject.equals(type) || Types.GeneratorObject.equals(type);
    }

    private static boolean isScriptObject(Type type) {
        return Types.ScriptObject.equals(type);
    }
}
