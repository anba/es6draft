/**
 * Copyright (c) André Bargull
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
     * B.3.6 Allow initializer expression in for-in variable declarations.
     */
    ForInVarInitializer,

    /**
     * B.3.7 The [[IsHTMLDDA]] Internal Slot
     */
    IsHTMLDDAObjects,

    /**
     * Intl - Normative optional: Intl constructor fallbacks
     */
    IntlConstructorLegacyFallback,

    /**
     * Web-Extension: RegExp statics
     */
    RegExpStatics,

    /**
     * Web-Extension: Function.prototype.arguments
     */
    FunctionArguments,

    /**
     * Web-Extension: Function.prototype.caller
     */
    FunctionCaller,

    /**
     * Moz-Extension: guarded catch
     */
    GuardedCatch,

    /**
     * Moz-Extension: expression closure
     */
    ExpressionClosure,

    /**
     * Moz-Extension: Reflect.parse() function
     */
    ReflectParse,

    /**
     * {@code function.sent} meta property (Stage 2 proposal)
     */
    FunctionSent,

    /**
     * {@code export namspace from} statements (Stage 1 proposal)
     */
    ExportNamespaceFrom,

    /**
     * {@code export default from} statements (Stage 1 proposal)
     */
    ExportDefaultFrom,

    /**
     * ArrayBuffer.transfer (Stage 0 proposal)
     */
    ArrayBufferTransfer,

    /**
     * Decorators (Stage 2 proposal)
     */
    Decorator,

    /**
     * Object Rest/Spread Properties (Stage 3 proposal)
     */
    ObjectRestSpreadProperties,

    /**
     * String.prototype.trimStart and trimEnd (Stage 2 proposal)
     */
    StringTrim,

    /**
     * Class Fields (Stage 3 proposal)
     */
    ClassFields,

    /**
     * String.prototype.matchAll (Stage 2 proposal)
     */
    StringMatchAll,

    /**
     * Call constructor [withdrawn]
     */
    CallConstructor,

    /**
     * Observable (Stage 1 proposal)
     */
    Observable,

    /**
     * {@code do}-expressions (Stage 1 proposal)
     */
    DoExpression,

    /**
     * Intl.PluralRules (Stage 4 proposal)
     */
    PluralRules,

    /**
     * Locale Operations (Stage 2 proposal)
     */
    Locale,

    /**
     * Asynchronous Iteration (Stage 3 proposal)
     */
    AsyncIteration,

    /**
     * Function.prototype.toString revision (Stage 3 proposal)
     */
    FunctionToString,

    /**
     * RegExp Lookbehind Assertions (Stage 3 proposal)<br>
     * <strong>NOTE: Only a very rudimentary implementation is available due to Joni limitations.</strong>
     */
    RegExpLookBehind,

    /**
     * RegExp Named Capture Groups (Stage 3 proposal)
     */
    RegExpNamedCapture,

    /**
     * RegExp Unicode Properties (Stage 3 proposal)
     */
    RegExpUnicodeProperties,

    /**
     * RegExp Possessive Quantifier (not proposed)
     */
    RegExpPossessive,

    /**
     * Weak references (Stage 1 proposal)
     */
    WeakReference,

    /**
     * Frozen Realms (Stage 1 proposal)
     */
    FrozenRealm,

    /**
     * Syntactic Tail Calls (inactive)
     */
    SyntacticTailCalls,

    /**
     * String.prototype.at (Stage 0 proposal)
     */
    StringAt,

    /**
     * Zones (Stage 0 proposal)
     */
    Zones,

    /**
     * Math Extensions (Stage 1 proposal)
     */
    MathExtensions,

    /**
     * Promise.prototype.finally (Stage 3 proposal)
     */
    PromiseFinally,

    /**
     * Global (Stage 3 proposal)
     */
    GlobalProperty,

    /**
     * Dynamic import (Stage 3 proposal)
     */
    DynamicImport,

    /**
     * Legacy RegExp features in JavaScript (Stage 3 proposal)
     */
    LegacyRegExp,

    /**
     * Intl.Segmenter (Stage 3 proposal)
     */
    IntlSegmenter,

    /**
     * Intl.ListFormat (Stage 2 proposal)
     */
    IntlListFormat,

    /**
     * Error Stacks (Stage 1 proposal)
     */
    ErrorStacks,

    /**
     * Math.signbit (Stage 1 proposal)
     */
    MathSignbit,

    /**
     * Promise.try (Stage 1 proposal)
     */
    PromiseTry,

    /**
     * Set and Map .of and .from (Stage 1 proposal)
     */
    CollectionsOfAndFrom,

    /**
     * s/dotAll flag for regular expressions (Stage 3 proposal)
     */
    RegExpDotAll,

    /**
     * Optional catch binding (Stage 3 proposal)
     */
    OptionalCatchBinding,

    /**
     * BigInt (Stage 3 proposal)
     */
    BigInt,

    /**
     * Numeric Separators (Stage 2 proposal)
     */
    NumericSeparators,

    /**
     * Private methods and accessors (Stage 3 proposal)
     */
    PrivateMethods,

    /**
     * import.meta (Stage 3 proposal)
     */
    ImportMeta,

    /**
     * Array.prototype.flat{Map,ten} (Stage 2 proposal)
     */
    ArrayPrototypeFlatMapFlatten,

    /**
     * Throw expressions (Stage 2 proposal)
     */
    ThrowExpression,

    /**
     * SIMD (deferred)
     */
    SIMD,

    /**
     * SIMD (Float64x2, Bool64x2, selectBits)
     */
    SIMD_Phase2,

    /**
     * Atomics.fence() function
     */
    AtomicsFence,

    /**
     * Type annotations (limited parser support only).
     */
    TypeAnnotation,

    /**
     * Array and Generator Comprehension (deferred extension)
     */
    Comprehension,

    /**
     * new super() (deferred extension)
     */
    NewSuper,

    /**
     * Realm Objects (deferred extension)
     */
    Realm,

    /**
     * Loader Objects (deferred extension)
     */
    Loader,

    /**
     * System Object (deferred extension)
     */
    System,

    /**
     * System.global (deferred)
     */
    SystemGlobal,

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
        return addAll(AnnexB(), EnumSet.range(RegExpStatics, FunctionCaller));
    }

    /**
     * Returns a set of all options for mozilla-compatibility.
     * 
     * @return the options set for mozilla-compatibility
     */
    public static final Set<CompatibilityOption> MozCompatibility() {
        return addAll(WebCompatibility(), MozExtensions(),
                EnumSet.of(Comprehension, SIMD, SIMD_Phase2, AtomicsFence, StringTrim, FunctionToString, AsyncIteration,
                        ObjectRestSpreadProperties, PluralRules, PromiseFinally, OptionalCatchBinding));
    }

    /**
     * Returns a set of all mozilla extensions.
     * 
     * @return the options set for mozilla extensions
     */
    public static final Set<CompatibilityOption> MozExtensions() {
        return EnumSet.of(GuardedCatch, ExpressionClosure, ReflectParse);
    }

    /**
     * Returns a set of all options for Annex B features.
     * 
     * @return the options set for Annex B features
     */
    public static final Set<CompatibilityOption> AnnexB() {
        return EnumSet.range(LegacyOctalIntegerLiteral, IntlConstructorLegacyFallback);
    }

    /**
     * Staging level of ECMAScript proposals.
     * 
     * @see <a href="https://tc39.github.io/process-document/">The TC39 Process</a>
     */
    public enum Stage {
        /**
         * Stage 0 (Strawman)
         */
        Strawman(0),

        /**
         * Stage 1 (Proposal)
         */
        Proposal(1),

        /**
         * Stage 2 (Draft)
         */
        Draft(2),

        /**
         * Stage 3 (Candidate)
         */
        Candidate(3),

        /**
         * Stage 4 (Finished)
         */
        Finished(4)

        ;

        private final int level;

        private Stage(int level) {
            this.level = level;
        }

        /**
         * Returns the staging level.
         * 
         * @return the staging level
         */
        public int getLevel() {
            return level;
        }
    }

    /**
     * Returns a set of all options for proposed stage extensions.
     * 
     * @param stage
     *            the requested staging level
     * @return the options set for proposed stage extensions
     */
    public static final Set<CompatibilityOption> of(Stage stage) {
        switch (stage) {
        case Strawman:
            return EnumSet.of(StringAt, Zones, ArrayBufferTransfer);
        case Proposal:
            return EnumSet.of(ExportNamespaceFrom, ExportDefaultFrom, Observable, WeakReference, FrozenRealm,
                    MathExtensions, CollectionsOfAndFrom, PromiseTry, MathSignbit, ErrorStacks, DoExpression);
        case Draft:
            return EnumSet.of(FunctionSent, StringTrim, Decorator, NumericSeparators, ArrayPrototypeFlatMapFlatten,
                    ThrowExpression, StringMatchAll, IntlListFormat, Locale);
        case Candidate:
            return EnumSet.of(FunctionToString, GlobalProperty, ObjectRestSpreadProperties, AsyncIteration,
                    DynamicImport, RegExpLookBehind, RegExpUnicodeProperties, RegExpNamedCapture, RegExpDotAll,
                    LegacyRegExp, PromiseFinally, BigInt, ClassFields, OptionalCatchBinding, ImportMeta, PrivateMethods,
                    IntlSegmenter);
        case Finished:
            return EnumSet.of(PluralRules);
        default:
            throw new AssertionError();
        }
    }

    /**
     * Returns a set of all options for proposed stage extensions.
     * 
     * @param stage
     *            the required staging level
     * @return the options set for proposed stage extensions
     */
    public static final Set<CompatibilityOption> Stage(Stage stage) {
        EnumSet<CompatibilityOption> options = EnumSet.noneOf(CompatibilityOption.class);
        switch (stage) {
        case Strawman:
            options.addAll(of(Stage.Strawman));
        case Proposal:
            options.addAll(of(Stage.Proposal));
        case Draft:
            options.addAll(of(Stage.Draft));
        case Candidate:
            options.addAll(of(Stage.Candidate));
        case Finished:
            options.addAll(of(Stage.Finished));
        default:
            return options;
        }
    }

    /**
     * ECMAScript version.
     */
    public enum Version {
        /**
         * ECMAScript 2018
         */
        ECMAScript2018,

        ;
    }

    /**
     * Returns a set of all options for the requested language version.
     * 
     * @param version
     *            the language version
     * @return the options set for the language version
     */
    public static final Set<CompatibilityOption> of(Version version) {
        switch (version) {
        case ECMAScript2018:
            return EnumSet.noneOf(CompatibilityOption.class);
        default:
            throw new AssertionError();
        }
    }

    /**
     * Returns a set of all options for the requested language version.
     * 
     * @param version
     *            the language version
     * @return the options set for the language version
     */
    public static final Set<CompatibilityOption> Version(Version version) {
        EnumSet<CompatibilityOption> options = EnumSet.noneOf(CompatibilityOption.class);
        switch (version) {
        case ECMAScript2018:
            options.addAll(of(Version.ECMAScript2018));
            break;
        default:
            throw new AssertionError();
        }
        return options;
    }

    /**
     * Returns a set of all experimental options.
     * 
     * @return the options set for experimental features
     */
    public static final Set<CompatibilityOption> Experimental() {
        return EnumSet.range(SIMD, SystemGlobal);
    }

    @SafeVarargs
    private static <E extends Enum<E>> Set<E> addAll(Set<E> set, Set<E>... sets) {
        for (Set<E> set2 : sets) {
            set.addAll(set2);
        }
        return set;
    }
}
