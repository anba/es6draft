/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Symbol;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>19 Fundamental Objects</h1><br>
 * <h2>19.4 Symbol Objects</h2>
 * <ul>
 * <li>19.4.4 Properties of Symbol Instances
 * </ul>
 */
public final class SymbolObject extends OrdinaryObject {
    /** [[SymbolData]] */
    private final Symbol symbolData;

    /**
     * Constructs a new Symbol object.
     * 
     * @param realm
     *            the realm object
     * @param symbolData
     *            the symbol data
     * @param prototype
     *            the prototype object
     */
    public SymbolObject(Realm realm, Symbol symbolData, ScriptObject prototype) {
        super(realm);
        this.symbolData = symbolData;
        setPrototype(prototype);
    }

    /**
     * [[SymbolData]]
     * 
     * @return the symbol value
     */
    public Symbol getSymbolData() {
        return symbolData;
    }

    /**
     * Creates a new Symbol object.
     * 
     * @param cx
     *            the execution context
     * @param symbolData
     *            the symbol value
     * @return the new symbol object
     */
    public static SymbolObject SymbolCreate(ExecutionContext cx, Symbol symbolData) {
        return new SymbolObject(cx.getRealm(), symbolData,
                cx.getIntrinsic(Intrinsics.SymbolPrototype));
    }
}
