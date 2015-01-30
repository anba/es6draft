/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.Intrinsics;
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
     */
    public SymbolObject(Realm realm, Symbol symbolData) {
        super(realm);
        this.symbolData = symbolData;
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
     * Custom helper function
     * 
     * @param cx
     *            the execution context
     * @param symbolData
     *            the symbol value
     * @return the new symbol object
     */
    public static SymbolObject SymbolCreate(ExecutionContext cx, Symbol symbolData) {
        SymbolObject obj = new SymbolObject(cx.getRealm(), symbolData);
        obj.setPrototype(cx.getIntrinsic(Intrinsics.SymbolPrototype));
        return obj;
    }
}
