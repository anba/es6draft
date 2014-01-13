/*
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

function drain(g, args = [[], [0], [""], ["abc"]]) {
  for (let a of args) [...g(...a)];
}

// default typed switch statements
drain(function*(v){ switch(v){ default: yield; } });

// typed switch statements, default first
drain(function*(v){ switch(v){ default: yield; case "": } });
drain(function*(v){ switch(v){ default: yield; case "a": } });
drain(function*(v){ switch(v){ default: yield; case "abc": } });
drain(function*(v){ switch(v){ default: yield; case 0: } });

// typed switch statements, default last
drain(function*(v){ switch(v){ case "": yield; default: } });
drain(function*(v){ switch(v){ case "a": yield; default: } });
drain(function*(v){ switch(v){ case "abc": yield; default: } });
drain(function*(v){ switch(v){ case 0: yield; default: } });

// try-catch
drain(function*(v){ try { } catch (e) { } });
drain(function*(v){ try { } catch (e) { yield } });
drain(function*(v){ try { yield } catch (e) { } });
drain(function*(v){ try { yield } catch (e) { yield } });

// try-finally
drain(function*(v){ try { } finally { } });
drain(function*(v){ try { } finally { yield } });
drain(function*(v){ try { yield } finally { } });
drain(function*(v){ try { yield } finally { yield } });

// try-catch-finally
drain(function*(v){ try { } catch (e) { } finally { } });
drain(function*(v){ try { } catch (e) { } finally { yield } });
drain(function*(v){ try { } catch (e) { yield } finally { } });
drain(function*(v){ try { } catch (e) { yield } finally { yield } });
drain(function*(v){ try { yield } catch (e) { } finally { } });
drain(function*(v){ try { yield } catch (e) { } finally { yield } });
drain(function*(v){ try { yield } catch (e) { yield } finally { } });
drain(function*(v){ try { yield } catch (e) { yield } finally { yield } });

// finally in yield insertion
drain(function*(v){ try { if (v) yield; } finally { yield } });
drain(function*(v){ try { if (v) return; } finally { yield } });
drain(function*(v){ L1: try { if (v) break L1; } finally { yield } });
drain(function*(v){ do try { if (v) break; } finally { yield } while (false) });
drain(function*(v){ do try { if (v) continue; } finally { yield } while (false) });
drain(function*(v){ L1: try { if (v) break L1; if (v) break L1; } finally { yield* [1, 2, 3] } });
