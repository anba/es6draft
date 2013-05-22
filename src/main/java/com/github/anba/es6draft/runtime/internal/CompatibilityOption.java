/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import static java.util.EnumSet.allOf;

import java.util.EnumSet;

/**
 * <h1>Annex B - Additional ECMAScript Features for Web Browsers</h1>
 * <ul>
 * <li>B.1 Additional Syntax
 * <li>B.2 Additional Properties
 * <li>B.3 Other Additional Features
 * </ul>
 */
public enum CompatibilityOption {
    /**
     * B.1.1 Numeric Literals
     */
    OctalInteger,

    /**
     * B.1.2 String Literals
     */
    OctalEscape,

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
     * B.3.1 __proto___ Property Names in Object Initialisers
     */
    ProtoInitialiser,

    /**
     * B.3.2 Web Legacy Compatibility for Block-Level Function Declarations
     */
    BlockFunctionDeclaration;

    public static final EnumSet<CompatibilityOption> WebCompatibility = allOf(CompatibilityOption.class);
}
