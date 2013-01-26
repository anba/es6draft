/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.semantics;

import java.util.HashMap;
import java.util.Map;

import com.github.anba.es6draft.ast.*;
import com.github.anba.es6draft.ast.MethodDefinition.MethodType;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.parser.ParserException.ExceptionType;

/**
 * Static Semantics: Object Literal
 */
public final class ObjectLiteralStaticSemantics {
    private ObjectLiteralStaticSemantics() {
    }

    private static ParserException reportSyntaxError(String message, Node node) {
        throw new ParserException(message, node.getLine(), ExceptionType.SyntaxError);
    }

    private static ParserException reportStrictModeSyntaxError(String message, Node node) {
        throw new ParserException(message, node.getLine(), ExceptionType.SyntaxError);
    }

    public static void validate(ObjectLiteral object, boolean strict) {
        final int VALUE = 0, GETTER = 1, SETTER = 2;
        Map<String, Integer> values = new HashMap<>();
        for (PropertyDefinition def : object.getProperties()) {
            PropertyName propertyName = def.getPropertyName();
            String key = propertyName.getName();
            final int kind;
            if (def instanceof PropertyValueDefinition || def instanceof PropertyNameDefinition) {
                kind = VALUE;
            } else if (def instanceof MethodDefinition) {
                MethodDefinition method = (MethodDefinition) def;
                if (method.hasSuperReference()) {
                    throw reportSyntaxError("'super' in method-definition outside of class", def);
                }
                MethodDefinition.MethodType type = method.getType();
                kind = type == MethodType.Getter ? GETTER : type == MethodType.Setter ? SETTER
                        : VALUE;
            } else {
                assert def instanceof CoverInitialisedName;
                // Always throw a Syntax Error if this production is present
                throw reportSyntaxError("missing ':' after property-id", def);
            }
            // It is a Syntax Error if PropertyNameList of PropertyDefinitionList contains any
            // duplicate entries [...]
            if (values.containsKey(key)) {
                int prev = values.get(key);
                if (kind == VALUE && prev != VALUE) {
                    reportSyntaxError("duplicate property definition", def);
                }
                if (kind == VALUE && prev == VALUE && strict) {
                    reportStrictModeSyntaxError("duplicate property definition", def);
                }
                if (kind == GETTER && prev != SETTER) {
                    reportSyntaxError("duplicate property definition", def);
                }
                if (kind == SETTER && prev != GETTER) {
                    reportSyntaxError("duplicate property definition", def);
                }
                values.put(key, prev | kind);
            } else {
                values.put(key, kind);
            }
        }
    }
}
