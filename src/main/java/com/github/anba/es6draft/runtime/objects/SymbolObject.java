/**
 * Copyright (c) 2012-2014 André Bargull
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
    private Symbol symbolData;

    public SymbolObject(Realm realm) {
        super(realm);
    }

    /**
     * [[SymbolData]]
     * 
     * @return the symbol value
     */
    public Symbol getSymbolData() {
        assert symbolData != null : "SymbolData not initialised";
        return symbolData;
    }

    /**
     * [[SymbolData]]
     * 
     * @param symbolData
     *            the new symbol value
     */
    public void setSymbolData(Symbol symbolData) {
        assert this.symbolData == null : "SymbolData already initialised";
        this.symbolData = symbolData;
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
        SymbolObject obj = new SymbolObject(cx.getRealm());
        obj.setPrototype(cx.getIntrinsic(Intrinsics.SymbolPrototype));
        obj.setSymbolData(symbolData);
        return obj;
    }
}
