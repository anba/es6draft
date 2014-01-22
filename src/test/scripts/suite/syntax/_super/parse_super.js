/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSyntaxError
} = Assert;

// FIXME: https://bugs.ecmascript.org/show_bug.cgi?id=2338

// 'super' is allowed in object literals
{
  ({m(){super()}});
  ({m(){super.x}});
  ({m(){super["x"]}});
  ({m(){super.x()}});
  ({m(){super["x"]()}});
}

// CallExpression: 'super' Arguments
// CallExpression: CallExpression Arguments
// CallExpression: CallExpression '[' Expression ']'
// CallExpression: CallExpression '.' IdentifierName
// CallExpression: CallExpression TemplateLiteral
{
  (class {m(){super()}});
  (class {m(){super()()}});
  (class {m(){super()["x"]}});
  (class {m(){super().x}});
  (class {m(){super().if}});
  (class {m(){super()``}});
}

// CallExpression: MemberExpression Arguments
{
  (class {m(){super["x"]()}});
  (class {m(){super.x()}});
  (class {m(){super.if()}});
}

// MemberExpression: 'super' '[' Expression ']'
// MemberExpression: MemberExpression '[' Expression ']'
// MemberExpression: MemberExpression '.' IdentifierName
// MemberExpression: MemberExpression TemplateLiteral
{
  (class {m(){super["x"]}});
  (class {m(){super["x"]["x"]}});
  (class {m(){super["x"].x}});
  (class {m(){super["x"].if}});
  (class {m(){super["x"]``}});
}

// MemberExpression: 'super' '.' IdentifierName
// MemberExpression: MemberExpression '[' Expression ']'
// MemberExpression: MemberExpression '.' IdentifierName
// MemberExpression: MemberExpression TemplateLiteral
{
  (class {m(){super.x}});
  (class {m(){super.x["x"]}});
  (class {m(){super.x.x}});
  (class {m(){super.x.if}});
  (class {m(){super.x``}});

  (class {m(){super.if}});
  (class {m(){super.if["x"]}});
  (class {m(){super.if.x}});
  (class {m(){super.if.if}});
  (class {m(){super.if``}});
}

// MemberExpression: 'new' 'super' Arguments{opt}
// MemberExpression: MemberExpression '[' Expression ']'
// MemberExpression: MemberExpression '.' IdentifierName
// MemberExpression: MemberExpression TemplateLiteral
{
  (class {m(){new super}});
  (class {m(){new super["x"]}}); // <- shift/reduce conflict
  (class {m(){new super.x}});
  (class {m(){new super.if}});
  (class {m(){new super``}});

  (class {m(){new super()}});
  (class {m(){new super()["x"]}});
  (class {m(){new super().x}});
  (class {m(){new super().if}});
  (class {m(){new super()``}});
}

// NewExpression: 'new' NewExpression
// NewExpression: MemberExpression
{
  (class {m(){new super["x"]}}); // <- shift/reduce conflict
  (class {m(){new super.x}});
  (class {m(){new super.if}});
}

// 'super' TemplateLiteral is not allowed
{
  assertSyntaxError("(class {m(){super``}});");
}

