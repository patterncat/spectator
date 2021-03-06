/*
 * Copyright 2014-2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.spectator.atlas;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Parses an Atlas data or query expression.
 */
final class Parser {

  private Parser() {
  }

  /**
   * Parse an <a href="https://github.com/Netflix/atlas/wiki/Reference-data">Atlas data
   * expression</a>.
   */
  static DataExpr parseDataExpr(String expr) {
    return (DataExpr) parse(expr);
  }

  /**
   * Parse an <a href="https://github.com/Netflix/atlas/wiki/Reference-query">Atlas query
   * expression</a>.
   */
  static Query parseQuery(String expr) {
    return (Query) parse(expr);
  }

  @SuppressWarnings({"unchecked", "PMD"})
  private static Object parse(String expr) {
    DataExpr.AggregateFunction af;
    Query q, q1, q2;
    String k, v;
    List<String> vs = null;
    String[] parts = expr.split(",");
    Deque<Object> stack = new ArrayDeque<>(parts.length);
    for (String p : parts) {
      String token = p.trim();
      if (token.isEmpty()) {
        continue;
      }
      if (vs != null && !")".equals(token)) {
        vs.add(token);
        continue;
      }
      switch (token) {
        case "(":
          vs = new ArrayList<>();
          break;
        case ")":
          stack.push(vs);
          vs = null;
          break;
        case ":true":
          stack.push(Query.TRUE);
          break;
        case ":false":
          stack.push(Query.FALSE);
          break;
        case ":and":
          q2 = (Query) stack.pop();
          q1 = (Query) stack.pop();
          stack.push(new Query.And(q1, q2));
          break;
        case ":or":
          q2 = (Query) stack.pop();
          q1 = (Query) stack.pop();
          stack.push(new Query.Or(q1, q2));
          break;
        case ":not":
          q = (Query) stack.pop();
          stack.push(new Query.Not(q));
          break;
        case ":has":
          k = (String) stack.pop();
          stack.push(new Query.Has(k));
          break;
        case ":eq":
          v = (String) stack.pop();
          k = (String) stack.pop();
          stack.push(new Query.Equal(k, v));
          break;
        case ":in":
          vs = (List<String>) stack.pop();
          k = (String) stack.pop();
          stack.push(new Query.In(k, new TreeSet<>(vs)));
          break;
        case ":lt":
          v = (String) stack.pop();
          k = (String) stack.pop();
          stack.push(new Query.LessThan(k, v));
          break;
        case ":le":
          v = (String) stack.pop();
          k = (String) stack.pop();
          stack.push(new Query.LessThanEqual(k, v));
          break;
        case ":gt":
          v = (String) stack.pop();
          k = (String) stack.pop();
          stack.push(new Query.GreaterThan(k, v));
          break;
        case ":ge":
          v = (String) stack.pop();
          k = (String) stack.pop();
          stack.push(new Query.GreaterThanEqual(k, v));
          break;
        case ":re":
          v = (String) stack.pop();
          k = (String) stack.pop();
          stack.push(new Query.Regex(k, v));
          break;
        case ":reic":
          v = (String) stack.pop();
          k = (String) stack.pop();
          stack.push(new Query.Regex(k, v, Pattern.CASE_INSENSITIVE, ":reic"));
          break;
        case ":all":
          q = (Query) stack.pop();
          stack.push(new DataExpr.All(q));
          break;
        case ":sum":
          q = (Query) stack.pop();
          stack.push(new DataExpr.Sum(q));
          break;
        case ":min":
          q = (Query) stack.pop();
          stack.push(new DataExpr.Min(q));
          break;
        case ":max":
          q = (Query) stack.pop();
          stack.push(new DataExpr.Max(q));
          break;
        case ":count":
          q = (Query) stack.pop();
          stack.push(new DataExpr.Count(q));
          break;
        case ":by":
          vs = (List<String>) stack.pop();
          af = (DataExpr.AggregateFunction) stack.pop();
          stack.push(new DataExpr.GroupBy(af, vs));
          break;
        case ":rollup-drop":
          vs = (List<String>) stack.pop();
          af = (DataExpr.AggregateFunction) stack.pop();
          stack.push(new DataExpr.DropRollup(af, vs));
          break;
        case ":rollup-keep":
          vs = (List<String>) stack.pop();
          af = (DataExpr.AggregateFunction) stack.pop();
          stack.push(new DataExpr.KeepRollup(af, vs));
          break;
        default:
          if (token.startsWith(":")) {
            throw new IllegalArgumentException("unknown word '" + token + "'");
          }
          stack.push(token);
          break;
      }
    }
    Object obj = stack.pop();
    if (!stack.isEmpty()) {
      throw new IllegalArgumentException("too many items remaining on stack: " + stack);
    }
    return obj;
  }
}
