/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.repl.loader;

import static com.github.anba.es6draft.runtime.AbstractOperations.Call;
import static com.github.anba.es6draft.runtime.AbstractOperations.Get;
import static com.github.anba.es6draft.runtime.LexicalEnvironment.newObjectEnvironment;
import static com.github.anba.es6draft.runtime.objects.FunctionConstructor.CreateDynamicFunction;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject.ObjectCreate;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.github.anba.es6draft.compiler.CompilationException;
import com.github.anba.es6draft.compiler.CompiledFunction;
import com.github.anba.es6draft.parser.JSONBuilder;
import com.github.anba.es6draft.parser.JSONParser;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.ObjectEnvironmentRecord;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.ScriptLoader;
import com.github.anba.es6draft.runtime.internal.Source;
import com.github.anba.es6draft.runtime.modules.ModuleExport;
import com.github.anba.es6draft.runtime.modules.ModuleRecord;
import com.github.anba.es6draft.runtime.modules.ModuleSource;
import com.github.anba.es6draft.runtime.modules.SourceIdentifier;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;

/**
 * Module record implementation for standard node modules (CommonJS modules).
 */
public final class NodeModuleRecord implements ModuleRecord {
    private final SourceIdentifier sourceId;
    private final Source source;
    private final CompiledFunction function;

    private Realm realm;
    private ScriptObject moduleObject;
    private LexicalEnvironment<ObjectEnvironmentRecord> environment;
    private ScriptObject namespace;
    private HashSet<String> exportedNames;
    private boolean instantiated;
    private boolean evaluated;

    NodeModuleRecord(SourceIdentifier sourceId, Source source, CompiledFunction function) {
        this.sourceId = sourceId;
        this.source = source;
        this.function = function;
    }

    /*package*/Source getSource() {
        return this.source;
    }

    /*package*/Object getModuleExports() {
        return Get(realm.defaultContext(), moduleObject, "exports");
    }

    private ScriptObject getModuleExportsOrNull() {
        Object exports = getModuleExports();
        if (Type.isObject(exports)) {
            return Type.objectValue(exports);
        }
        return null;
    }

    private ScriptObject getModuleExportsOrEmpty() {
        Object exports = getModuleExports();
        if (Type.isObject(exports)) {
            return Type.objectValue(exports);
        }
        // Return an empty object as a placeholder when module exports is a primitive.
        return ObjectCreate(realm, (ScriptObject) null);
    }

    private HashSet<String> ownNames(ScriptObject object) {
        HashSet<String> names = new HashSet<>();
        for (Object key : object.ownPropertyKeys(realm.defaultContext())) {
            if (key instanceof String) {
                names.add((String) key);
            }
        }
        return names;
    }

    private Set<String> instantiateAndGetExportedNames() {
        instantiate();
        if (!instantiated) {
            // Recursive call - return the own names of the current module exports.
            return ownNames(getModuleExportsOrEmpty());
        }
        return exportedNames;
    }

    @Override
    public SourceIdentifier getSourceCodeId() {
        return sourceId;
    }

    @Override
    public Realm getRealm() {
        return realm;
    }

    public void setRealm(Realm realm, ScriptObject moduleObject) {
        assert this.realm == null : "module already linked";
        this.realm = Objects.requireNonNull(realm);
        this.moduleObject = Objects.requireNonNull(moduleObject);
    }

    @Override
    public LexicalEnvironment<ObjectEnvironmentRecord> getEnvironment() {
        return environment;
    }

    @Override
    public ScriptObject getNamespace() {
        assert realm != null : "module is not linked";
        ScriptObject exports = getModuleExportsOrNull();
        if (exports != null) {
            return exports;
        }
        // Something went wrong, return the current namespace object.
        return namespace;
    }

    @Override
    public void setNamespace(ScriptObject namespace) {
        assert this.namespace == null : "namespace already created";
        this.namespace = Objects.requireNonNull(namespace);
    }

    @Override
    public boolean isEvaluated() {
        return evaluated;
    }

    @Override
    public boolean isInstantiated() {
        return instantiated;
    }

    @Override
    public Set<String> getExportedNames(Set<ModuleRecord> exportStarSet) {
        assert realm != null : "module is not linked";
        return Collections.unmodifiableSet(instantiateAndGetExportedNames());
    }

    @Override
    public ModuleExport resolveExport(String exportName, Map<ModuleRecord, Set<String>> resolveSet,
            Set<ModuleRecord> exportStarSet) {
        assert realm != null : "module is not linked";
        Set<String> resolvedExports = resolveSet.get(this);
        if (resolvedExports == null) {
            resolveSet.put(this, resolvedExports = new HashSet<>());
        } else if (resolvedExports.contains(exportName)) {
            return null;
        }
        resolvedExports.add(exportName);
        if (instantiateAndGetExportedNames().contains(exportName)) {
            return new ModuleExport(this, exportName);
        }
        return null;
    }

    @Override
    public void instantiate() {
        assert realm != null : "module is not linked";
        if (environment == null) {
            ScriptObject exports = getModuleExportsOrEmpty();
            environment = newObjectEnvironment(exports, realm.getGlobalEnv());

            // Compile the module.
            ExecutionContext cx = realm.defaultContext();
            Object compile = Get(cx, moduleObject, "compile");
            Callable moduleFn = CreateDynamicFunction(cx, source, function.getFunction());
            Callable requireFn = NodeFunctions.createRequireFunction(this);
            Call(cx, compile, moduleObject, moduleFn, requireFn);

            // Create the module bindings.
            ScriptObject currentExports = getModuleExportsOrEmpty();
            if (currentExports != exports) {
                // Module exports property has changed, update environment with new bindings.
                environment = newObjectEnvironment(currentExports, realm.getGlobalEnv());
            }
            exportedNames = ownNames(currentExports);

            instantiated = true;
        }
    }

    @Override
    public Object evaluate() {
        assert realm != null : "module is not linked";
        assert environment != null : "module is not instantiated";
        evaluated = true;
        return UNDEFINED;
    }

    /**
     * ParseModule ( sourceText )
     * 
     * @param scriptLoader
     *            the script loader
     * @param identifier
     *            the source code identifier
     * @param moduleSource
     *            the module source code
     * @return the parsed module record
     * @throws IOException
     *             if there was any I/O error
     * @throws ParserException
     *             if the module source contains any syntax errors
     * @throws CompilationException
     *             if the parsed module source cannot be compiled
     */
    public static NodeModuleRecord ParseModule(ScriptLoader scriptLoader, SourceIdentifier identifier,
            ModuleSource moduleSource) throws IOException, ParserException, CompilationException {
        Source source = moduleSource.toSource();
        String sourceCode = moduleSource.sourceCode();
        if (identifier.toUri().toString().endsWith(".json")) {
            String jsonScript = JSONParser.parse(sourceCode, new ScriptJSONBuilder());
            sourceCode = String.format("module.exports = %s", jsonScript);
        }
        String parameters = "exports, require, module, __filename, __dirname";
        CompiledFunction function = scriptLoader.function(source, parameters, sourceCode);
        return new NodeModuleRecord(identifier, source, function);
    }

    private static final class ScriptJSONBuilder implements JSONBuilder<String, Void, Void, Void> {
        private final StringBuilder sb = new StringBuilder();

        private static void appendJsonString(StringBuilder sb, String s) {
            final int len = s.length();
            int begin = 0;
            for (int i = 0; i < len; ++i) {
                char c = s.charAt(i);
                if (c == '\u2028' || c == '\u2029') {
                    sb.append(s, begin, i);
                    sb.append('\\').append(c);
                    begin = i + 1;
                }
            }
            sb.append(s, begin, len);
        }

        @Override
        public String createDocument(Void value) {
            return sb.toString();
        }

        @Override
        public Void newObject() {
            sb.append('{');
            return null;
        }

        @Override
        public Void finishObject(Void object) {
            sb.append('}');
            return null;
        }

        @Override
        public void newProperty(Void object, String name, String rawName, long index) {
            if (index != 0) {
                sb.append(',');
            }
            appendJsonString(sb, rawName);
            sb.append(':');
        }

        @Override
        public void finishProperty(Void object, String name, String rawName, long index, Void value) {
            // empty
        }

        @Override
        public Void newArray() {
            sb.append('[');
            return null;
        }

        @Override
        public Void finishArray(Void array) {
            sb.append(']');
            return null;
        }

        @Override
        public void newElement(Void array, long index) {
            if (index != 0) {
                sb.append(',');
            }
        }

        @Override
        public void finishElement(Void array, long index, Void value) {
            // empty
        }

        @Override
        public Void newNull() {
            sb.append("null");
            return null;
        }

        @Override
        public Void newBoolean(boolean value) {
            sb.append(value);
            return null;
        }

        @Override
        public Void newNumber(double value, String rawValue) {
            sb.append(rawValue);
            return null;
        }

        @Override
        public Void newString(String value, String rawValue) {
            appendJsonString(sb, rawValue);
            return null;
        }
    }
}
