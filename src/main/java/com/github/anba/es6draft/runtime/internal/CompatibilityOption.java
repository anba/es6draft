/**
 * Copyright (c) 2012-2015 André Bargull
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
     * B.1.4 Regular Expressions Patterns
     */
    WebRegularExpressions,

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
     * B.3.1 __proto___ Property Names in Object Initializers
     */
    ProtoInitializer,

    /**
     * B.3.2 Labelled Function Declarations
     */
    LabelledFunctionDeclaration,

    /**
     * B.3.3 Block-Level Function Declarations Web Legacy Compatibility Semantics
     */
    BlockFunctionDeclaration,

    /**
     * B.3.4 FunctionDeclarations in IfStatement Statement Clauses
     */
    IfStatementFunctionDeclaration,

    /**
     * B.3.5 VariableStatements in Catch blocks
     */
    CatchVarStatement,

    /**
     * Extension: RegExp statics
     */
    RegExpStatics,

    /**
     * Extension: Function.prototype.caller and Function.prototype.arguments
     */
    FunctionPrototype,

    /**
     * Extension: arguments.caller (not implemented)
     */
    ArgumentsCaller,

    /**
     * Extension: Detect duplicate property definitions
     */
    DuplicateProperties,

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
     * Moz-Extension: Implicit strict functions include <tt>"use strict;"</tt> directive in source
     */
    ImplicitStrictDirective,

    /**
     * Moz-Extension: Don't call [[Enumerate]] on Proxy objects in the prototype chain
     */
    ProxyProtoSkipEnumerate,

    /**
     * ES7-Extension: Async Function Definitions
     */
    AsyncFunction,

    /**
     * ES7-Extension: Array and Generator Comprehension
     */
    Comprehension,

    /**
     * ES7-Extension: Exponentiation operator {@code **}
     */
    Exponentiation,

    /**
     * ES7-Extension: Realm Objects
     */
    Realm,

    /**
     * ES7-Extension: Loader Objects
     */
    Loader,

    /**
     * ES7-Extension: System Object
     */
    System,

    /**
     * ES7-Extension: Array.prototype.includes
     */
    ArrayIncludes,

    /**
     * Track unhandled rejected promise objects
     */
    PromiseRejection,

    ;

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
        return addAll(AnnexB(), EnumSet.range(RegExpStatics, FunctionPrototype));
    }

    /**
     * Returns a set of all options for mozilla-compatibility.
     * 
     * @return the options set for mozilla-compatibility
     */
    public static final Set<CompatibilityOption> MozCompatibility() {
        return addAll(WebCompatibility(), EnumSet.range(ForEachStatement, ProxyProtoSkipEnumerate),
                EnumSet.of(Comprehension));
    }

    /**
     * Returns a set of all options for Annex B features.
     * 
     * @return the options set for proposed Annex B features
     */
    public static final Set<CompatibilityOption> AnnexB() {
        return EnumSet.range(LegacyOctalIntegerLiteral, CatchVarStatement);
    }

    /**
     * Returns a set of all options for proposed ES7 extensions.
     * 
     * @return the options set for proposed ES7 extensions
     */
    public static final Set<CompatibilityOption> ECMAScript7() {
        return EnumSet.range(AsyncFunction, ArrayIncludes);
    }

    @SafeVarargs
    private static <E extends Enum<E>> Set<E> addAll(Set<E> set, Set<E>... sets) {
        for (Set<E> set2 : sets) {
            set.addAll(set2);
        }
        return set;
    }
}
