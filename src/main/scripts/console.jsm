/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

class AssertionError extends Error {
  get name() {
    return "AssertionError";
  }
}

function stackFrames(e) {
  let frames = e.stackTrace;
  if (frames.length === 1) {
    // Add interpreter frame.
    frames.push({methodName: "", fileName: "<Interpreter>", lineNumber: 1});
  }
  return frames;
}

class Console {
  constructor(print, printErr = print) {
    this._print = print;
    this._printErr = printErr;
    this._groups = 0;
    this._labels = new Map();
    this._timers = new Map();
  }

  _inspect(value) {
    return String(value);
  }

  _format(message, ...args) {
    if (typeof message !== "string") {
      return [message, ...args].map(this._inspect).join(" ");
    }
    let values = args.values();
    let formatNext = (t, s) => {
      let {value, done} = values.next();
      return !done ? t(value) : s;
    };
    let formatNumber = (v, digits = 6) => Number(v).toFixed(digits);
    message = message.replace(/%(?:\.(\d+))?([%dfijos])/g, (s, p, f) => {
      switch (f) {
        case "%":
          return "%";
        case "d":
          return formatNext(Number, s);
        case "i":
          return formatNext(v => formatNumber(v, 0), s);
        case "f":
          return formatNext(v => formatNumber(v, Math.min(p | 0, 21)), s);
        case "j":
          try {
            return formatNext(JSON.stringify, s);
          } catch (e) {
            return "[]";
          }
        case "o":
          return formatNext(this._inspect, s);
        case "s":
        default:
          return formatNext(String, s);
      }
    });
    [...args] = values;
    return [message, ...args.map(this._inspect)].join(" ");
  }

  assert(expression, message = "", ...args) {
    if (!expression) {
      throw new AssertionError(this._format(message, ...args));
    }
  }

  log(message = "", ...args) {
    this._print("  ".repeat(this._groups) + this._format(message, ...args));
  }

  info(message = "", ...args) {
    return this.log(message, ...args);
  }

  debug(message = "", ...args) {
    return this.log(message, ...args);
  }

  warn(message = "", ...args) {
    this._printErr("  ".repeat(this._groups) + this._format(message, ...args));
  }

  error(message = "", ...args) {
    return this.warn(message, ...args);
  }

  dir(object) {
    this.log(this._inspect(object));
  }

  trace() {
    let out = "Trace";
    let prefix = `\n${"  ".repeat(this._groups)}    at`;
    for (let frame of stackFrames(new Error()).slice(1)) {
      out += `${prefix} ${frame.methodName} (${frame.fileName}:${frame.lineNumber})`;
    }
    this.error(out);
  }

  group(message = "", ...args) {
    this._groups += 1;
    this.log(message, ...args);
  }

  groupCollapsed(message = "", ...args) {
    return this.group(message, ...args);
  }

  groupEnd() {
    this._groups = Math.max(this._groups - 1, 0);
  }

  count(label = undefined) {
    let message;
    if (label === undefined) {
      let frame = stackFrames(new Error())[1];
      label = `${frame.methodName} (${frame.fileName}:${frame.lineNumber})`;
      message = "<no label>";
    } else {
      label = String(label);
      message = label;
    }
    let count = this._labels.get(label) || 0;
    this._labels.set(label, count + 1);
    this.log(`${message}: ${count}`);
  }

  time(name) {
    if (name !== undefined) {
      this._timers.set(name, Date.now());
    }
  }

  timeEnd(name) {
    if (name !== undefined) {
      let end = Date.now();
      let start = this._timers.get(name);
      if (this._timers.delete(name)) {
        this.log(`${name}: ${end - start} ms`);
      }
    }
  }
}

export default new Console(print);
