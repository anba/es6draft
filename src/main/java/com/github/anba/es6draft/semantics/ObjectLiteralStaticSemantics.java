/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.semantics;

import java.util.HashMap;
import java.util.Map;

import com.github.anba.es6draft.ast.CoverInitialisedName;
import com.github.anba.es6draft.ast.MethodDefinition;
import com.github.anba.es6draft.ast.MethodDefinition.MethodType;
import com.github.anba.es6draft.ast.Node;
import com.github.anba.es6draft.ast.ObjectLiteral;
import com.github.anba.es6draft.ast.PropertyDefinition;
import com.github.anba.es6draft.ast.PropertyName;
import com.github.anba.es6draft.ast.PropertyNameDefinition;
import com.github.anba.es6draft.ast.PropertyValueDefinition;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.parser.ParserException.ExceptionType;
import com.github.anba.es6draft.runtime.internal.Messages;

/**
 * Static Semantics: Object Literal
 */
public final class ObjectLiteralStaticSemantics {
    private ObjectLiteralStaticSemantics() {
    }

    private static ParserException reportSyntaxError(Node node, Messages.Key messageKey,
            String... args) {
        throw new ParserException(ExceptionType.SyntaxError, node.getLine(), messageKey, args);
    }

    private static ParserException reportStrictModeSyntaxError(Node node, Messages.Key messageKey,
            String... args) {
        throw new ParserException(ExceptionType.SyntaxError, node.getLine(), messageKey, args);
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
                    throw reportSyntaxError(def, Messages.Key.SuperOutsideClass);
                }
                MethodDefinition.MethodType type = method.getType();
                kind = type == MethodType.Getter ? GETTER : type == MethodType.Setter ? SETTER
                        : VALUE;
            } else {
                assert def instanceof CoverInitialisedName;
                // Always throw a Syntax Error if this production is present
                throw reportSyntaxError(def, Messages.Key.MissingColonAfterPropertyId, key);
            }
            // It is a Syntax Error if PropertyNameList of PropertyDefinitionList contains any
            // duplicate entries [...]
            if (values.containsKey(key)) {
                int prev = values.get(key);
                if (kind == VALUE && prev != VALUE) {
                    reportSyntaxError(def, Messages.Key.DuplicatePropertyDefinition, key);
                }
                if (kind == VALUE && prev == VALUE && strict) {
                    reportStrictModeSyntaxError(def, Messages.Key.DuplicatePropertyDefinition, key);
                }
                if (kind == GETTER && prev != SETTER) {
                    reportSyntaxError(def, Messages.Key.DuplicatePropertyDefinition, key);
                }
                if (kind == SETTER && prev != GETTER) {
                    reportSyntaxError(def, Messages.Key.DuplicatePropertyDefinition, key);
                }
                values.put(key, prev | kind);
            } else {
                values.put(key, kind);
            }
        }
    }
}
