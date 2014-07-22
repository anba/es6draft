/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import java.util.EnumSet;
import java.util.Set;

/**
 * <h1>Annex B - Additional ECMAScript Features for Web Browsers</h1>
 * <ul>
 * <li>B.1 Additional Syntax
 * <li>B.2 Additional Built-in Properties
 * <li>B.3 Other Additional Features
 * </ul>
 */
public enum CompatibilityOption {
    /**
     * B.1.1 Numeric Literals
     */
    LegacyOctalIntegerLiteral,

    /**
     * B.1.2 String Literals
     */
    OctalEscapeSequence,

    /**
     * B.1.3 HTML-like Comments
     */
    HTMLComments,

    /**
     * B.2.1 Additional Properties of the Global Object
     */
    GlobalObject,

    /**
     * B.2.2 Additional Properties of the Object.prototype Object
     */
    ObjectPrototype,

    /**
     * B.2.3 Additional Properties of the String.prototype Object
     */
    StringPrototype,

    /**
     * B.2.4 Additional Properties of the Date.prototype Object
     */
    DatePrototype,

    /**
     * B.2.5 Additional Properties of the RegExp.prototype Object
     */
    RegExpPrototype,

    /**
     * RegExp statics
     */
    RegExpStatics,

    /**
     * Function.prototype.caller and Function.prototype.arguments
     */
    FunctionPrototype,

    /**
     * B.3.1 __proto___ Property Names in Object Initializers
     */
    ProtoInitializer,

    /**
     * B.3.2 Web Legacy Compatibility for Block-Level Function Declarations
     */
    BlockFunctionDeclaration,

    /**
     * B.3.3 var statements in Catch blocks
     */
    CatchVarStatement,

    /**
     * Moz-Extension: for-each statement
     */
    ForEachStatement,

    /**
     * Moz-Extension: guarded catch
     */
    GuardedCatch,

    /**
     * Moz-Extension: expression closure
     */
    ExpressionClosure,

    /**
     * Moz-Extension: let statement
     */
    LetStatement,

    /**
     * Moz-Extension: let expression
     */
    LetExpression,

    /**
     * Moz-Extension: legacy (star-less) generators
     */
    LegacyGenerator,

    /**
     * Moz-Extension: legacy comprehension forms
     */
    LegacyComprehension,

    /**
     * Moz-Extension: Reflect.parse() function
     */
    ReflectParse,

    /**
     * Moz-Extension: Extended precision for toFixed, toExponential, toPrecision
     */
    ExtendedPrecision,

    /**
     * Moz-Extension: 8 and 9 in legacy octal integer literal are reported as errors
     */
    StrictLegacyOctalIntegerLiteral,

    /**
     * Moz-Extension: Implicit strict functions include <tt>"use strict;"</tt> directive in source
     */
    ImplicitStrictDirective,

    /**
     * Extension: Async Function Definitions
     */
    AsyncFunction,

    /**
     * Extension: arguments.caller (not implemented)
     */
    ArgumentsCaller;

    /**
     * Returns a set of all options for strict-compatibility.
     * 
     * @return the options set for strict-compatibility
     */
    public static final Set<CompatibilityOption> StrictCompatibility() {
        return EnumSet.noneOf(CompatibilityOption.class);
    }

    /**
     * Returns a set of all options for web-compatibility.
     * 
     * @return the options set for web-compatibility
     */
    public static final Set<CompatibilityOption> WebCompatibility() {
        return EnumSet.range(LegacyOctalIntegerLiteral, CatchVarStatement);
    }

    /**
     * Returns a set of all options for mozilla-compatibility.
     * 
     * @return the options set for mozilla-compatibility
     */
    public static final Set<CompatibilityOption> MozCompatibility() {
        return EnumSet.range(LegacyOctalIntegerLiteral, ImplicitStrictDirective);
    }
}
