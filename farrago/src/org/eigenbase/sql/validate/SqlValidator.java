/*
// $Id$
// Package org.eigenbase is a class library of data management components.
// Copyright (C) 2004 The Eigenbase Project
// Copyright (C) 2004 SQLstream, Inc.
// Copyright (C) 2005 Dynamo BI Corporation
//
// This program is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License as published by the Free
// Software Foundation; either version 2 of the License, or (at your option)
// any later version approved by The Eigenbase Project.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/
package org.eigenbase.sql.validate;

import java.util.List;
import java.util.Map;

import org.eigenbase.reltype.RelDataType;
import org.eigenbase.reltype.RelDataTypeFactory;
import org.eigenbase.reltype.RelDataTypeField;
import org.eigenbase.sql.SqlCall;
import org.eigenbase.sql.SqlDataTypeSpec;
import org.eigenbase.sql.SqlDelete;
import org.eigenbase.sql.SqlDynamicParam;
import org.eigenbase.sql.SqlFunction;
import org.eigenbase.sql.SqlIdentifier;
import org.eigenbase.sql.SqlInsert;
import org.eigenbase.sql.SqlIntervalQualifier;
import org.eigenbase.sql.SqlLiteral;
import org.eigenbase.sql.SqlMerge;
import org.eigenbase.sql.SqlNode;
import org.eigenbase.sql.SqlNodeList;
import org.eigenbase.sql.SqlOperatorTable;
import org.eigenbase.sql.SqlSelect;
import org.eigenbase.sql.SqlUpdate;
import org.eigenbase.sql.SqlWindow;
import org.eigenbase.util.EigenbaseException;


/**
 * Validates the parse tree of a SQL statement, and provides semantic
 * information about the parse tree.
 *
 * <p>To create an instance of the default validator implementation, call {@link
 * SqlValidatorUtil#newValidator}.
 *
 * <h2>Visitor pattern</h2>
 *
 * <p>The validator interface is an instance of the {@link
 * org.eigenbase.util.Glossary#VisitorPattern visitor pattern}. Implementations
 * of the {@link SqlNode#validate} method call the <code>validateXxx</code>
 * method appropriate to the kind of node: {@link
 * SqlLiteral#validate(SqlValidator, SqlValidatorScope)} calls {@link
 * #validateLiteral(org.eigenbase.sql.SqlLiteral)}; {@link
 * SqlCall#validate(SqlValidator, SqlValidatorScope)} calls {@link
 * #validateCall(SqlCall,SqlValidatorScope)}; and so forth.
 *
 * <p>The {@link SqlNode#validateExpr(SqlValidator, SqlValidatorScope)} method
 * is as {@link SqlNode#validate(SqlValidator, SqlValidatorScope)} but is called
 * when the node is known to be a scalar expression.
 *
 * <h2>Scopes and namespaces</h2>
 *
 * <p>In order to resolve names to objects, the validator builds a map of the
 * structure of the query. This map consists of two types of objects. A {@link
 * SqlValidatorScope} describes the tables and columns accessible at a
 * particular point in the query; and a {@link SqlValidatorNamespace} is a
 * description of a data source used in a query.
 *
 * <p>There are different kinds of namespace for different parts of the query.
 * for example {@link IdentifierNamespace} for table names, {@link
 * SelectNamespace} for SELECT queries, {@link SetopNamespace} for UNION, EXCEPT
 * and INTERSECT. A validator is allowed to wrap namespaces in other objects
 * which implement {@link SqlValidatorNamespace}, so don't try to cast your
 * namespace or use <code>instanceof</code>; use {@link
 * SqlValidatorNamespace#unwrap(Class)} and {@link
 * SqlValidatorNamespace#isWrapperFor(Class)} instead.</p>
 *
 * <p>The validator builds the map by making a quick scan over the query when
 * the root {@link SqlNode} is first provided. Thereafter, it supplies the
 * correct scope or namespace object when it calls validation methods.</p>
 *
 * <p>The methods {@link #getSelectScope}, {@link #getFromScope}, {@link
 * #getWhereScope}, {@link #getGroupScope}, {@link #getHavingScope}, {@link
 * #getOrderScope} and {@link #getJoinScope} get the correct scope to resolve
 * names in a particular clause of a SQL statement.</p>
 *
 * @author jhyde
 * @version $Id$
 * @since Oct 28, 2004
 */
public interface SqlValidator
{
    //~ Methods ----------------------------------------------------------------

    /**
     * Returns the dialect of SQL (SQL:2003, etc.) this validator recognizes.
     * Default is {@link SqlConformance#Default}.
     *
     * @return dialect of SQL this validator recognizes
     */
    SqlConformance getConformance();

    /**
     * Returns the catalog reader used by this validator.
     *
     * @return catalog reader
     */
    SqlValidatorCatalogReader getCatalogReader();

    /**
     * Returns the operator table used by this validator.
     *
     * @return operator table
     */
    SqlOperatorTable getOperatorTable();

    /**
     * Validates an expression tree. You can call this method multiple times,
     * but not reentrantly.
     *
     * @param topNode top of expression tree to be validated
     *
     * @return validated tree (possibly rewritten)
     *
     * @pre outermostNode == null
     */
    SqlNode validate(SqlNode topNode);

    /**
     * Validates an expression tree. You can call this method multiple times,
     * but not reentrantly.
     *
     * @param topNode top of expression tree to be validated
     * @param nameToTypeMap map of simple name to {@link RelDataType}; used to
     * resolve {@link SqlIdentifier} references
     *
     * @return validated tree (possibly rewritten)
     *
     * @pre outermostNode == null
     */
    SqlNode validateParameterizedExpression(
        SqlNode topNode,
        Map<String, RelDataType> nameToTypeMap);

    /**
     * Checks that a query is valid.
     *
     * <p>Valid queries include:
     *
     * <ul>
     * <li><code>SELECT</code> statement,
     * <li>set operation (<code>UNION</code>, <code>INTERSECT</code>, <code>
     * EXCEPT</code>)
     * <li>identifier (e.g. representing use of a table in a FROM clause)
     * <li>query aliased with the <code>AS</code> operator
     * </ul>
     *
     * @param node Query node
     * @param scope Scope in which the query occurs
     *
     * @throws RuntimeException if the query is not valid
     */
    void validateQuery(SqlNode node, SqlValidatorScope scope);

    /**
     * Returns the type of the root node of a query.
     *
     * <p>The default implementation returns the same value as
     * {@link #getValidatedNodeType(org.eigenbase.sql.SqlNode)}. Derived classes
     * may return a type that is similar, but lacking system fields.
     *
     * @param node Root node of query
     *
     * @return validated type, never null
     */
    RelDataType getRootNodeType(SqlNode node);

    /**
     * Returns the type assigned to a node by validation.
     *
     * @param node the node of interest
     *
     * @return validated type, never null
     */
    RelDataType getValidatedNodeType(SqlNode node);

    /**
     * Returns the type assigned to a node by validation, or null if unknown.
     * This allows for queries against nodes such as aliases, which have no type
     * of their own. If you want to assert that the node of interest must have a
     * type, use {@link #getValidatedNodeType} instead.
     *
     * @param node the node of interest
     *
     * @return validated type, or null if unknown or not applicable
     */
    RelDataType getValidatedNodeTypeIfKnown(SqlNode node);

    /**
     * Resolves an identifier to a fully-qualified name.
     *
     * @param id Identifier
     * @param scope Naming scope
     */
    void validateIdentifier(SqlIdentifier id, SqlValidatorScope scope);

    /**
     * Validates a literal.
     *
     * @param literal Literal
     */
    void validateLiteral(SqlLiteral literal);

    /**
     * Validates a {@link SqlIntervalQualifier}
     *
     * @param qualifier Interval qualifier
     */
    void validateIntervalQualifier(SqlIntervalQualifier qualifier);

    /**
     * Validates an INSERT statement.
     *
     * @param insert INSERT statement
     */
    void validateInsert(SqlInsert insert);

    /**
     * Validates an UPDATE statement.
     *
     * @param update UPDATE statement
     */
    void validateUpdate(SqlUpdate update);

    /**
     * Validates a DELETE statement.
     *
     * @param delete DELETE statement
     */
    void validateDelete(SqlDelete delete);

    /**
     * Validates a MERGE statement.
     *
     * @param merge MERGE statement
     */
    void validateMerge(SqlMerge merge);

    /**
     * Validates a data type expression.
     *
     * @param dataType Data type
     */
    void validateDataType(SqlDataTypeSpec dataType);

    /**
     * Validates a dynamic parameter.
     *
     * @param dynamicParam Dynamic parameter
     */
    void validateDynamicParam(SqlDynamicParam dynamicParam);

    /**
     * Validates the right-hand side of an OVER expression. It might be either
     * an {@link SqlIdentifier identifier} referencing a window, or an {@link
     * SqlWindow inline window specification}.
     *
     * @param windowOrId SqlNode that can be either SqlWindow with all the
     * components of a window spec or a SqlIdentifier with the name of a window
     * spec.
     * @param scope Naming scope
     * @param call is the SqlNode if a function call if the window is attached
     * to one.
     */
    void validateWindow(
        SqlNode windowOrId,
        SqlValidatorScope scope,
        SqlCall call);

    /**
     * Validates a call to an operator.
     *
     * @param call Operator call
     * @param scope Naming scope
     */
    void validateCall(
        SqlCall call,
        SqlValidatorScope scope);

    /**
     * Validates parameters for aggregate function.
     *
     * @param aggFunction function containing COLUMN_LIST parameter
     * @param isOver is this part of OVER clause
     * @param scope Syntactic scope
     */
    void validateAggregateParams(
        SqlCall aggFunction, boolean isOver, SqlValidatorScope scope);

    /**
     * Validates a COLUMN_LIST parameter
     *
     * @param function function containing COLUMN_LIST parameter
     * @param argTypes function arguments
     * @param operands operands passed into the function call
     */
    void validateColumnListParams(
        SqlFunction function,
        RelDataType [] argTypes,
        SqlNode [] operands);

    /**
     * Derives the type of a node in a given scope. If the type has already been
     * inferred, returns the previous type.
     *
     * @param scope Syntactic scope
     * @param operand Parse tree node
     *
     * @return Type of the SqlNode. Should never return <code>NULL</code>
     */
    RelDataType deriveType(
        SqlValidatorScope scope,
        SqlNode operand);

    /**
     * Adds "line x, column y" context to a validator exception.
     *
     * <p>Note that the input exception is checked (it derives from {@link
     * Exception}) and the output exception is unchecked (it derives from {@link
     * RuntimeException}). This is intentional -- it should remind code authors
     * to provide context for their validation errors.
     *
     * @param e The validation error
     * @param node The place where the exception occurred
     *
     * @return Exception containing positional information
     *
     * @pre node != null
     * @post return != null
     */
    EigenbaseException newValidationError(
        SqlNode node,
        SqlValidatorException e);

    /**
     * Returns whether a SELECT statement is an aggregation. Criteria are: (1)
     * contains GROUP BY, or (2) contains HAVING, or (3) SELECT or ORDER BY
     * clause contains aggregate functions. (Windowed aggregate functions, such
     * as <code>SUM(x) OVER w</code>, don't count.)
     *
     * @param select SELECT statement
     *
     * @return whether SELECT statement is an aggregation
     */
    boolean isAggregate(SqlSelect select);

    /**
     * Returns whether a select list expression is an aggregate function.
     *
     * @param selectNode Expression in SELECT clause
     *
     * @return whether expression is an aggregate function
     */
    boolean isAggregate(SqlNode selectNode);

    /**
     * Returns whether a parse tree node -- typically a SELECT, but may be
     * another query operation such as a UNION or VALUES -- is a cursor.
     *
     * <p>The root of a statement is always a cursor; and any query that is
     * inside the CURSOR function as an argument to a function.
     *
     * <blockquote><code>SELECT e.name, d.name<br/>
     * FROM TABLE(upperCase(SELECT * FROM emp)) AS e<br/>
     * JOIN (SELECT * FROM dept WHERE location = 'USA') AS d<br/>
     * USING (deptno)</code></blockquote>
     *
     * <ul>
     * <li>'SELECT e.name ...' is a cursor (because it is the root);
     * <li>'SELECT * FROM emp' is a cursor (because it is an argument to the
     *     CURSOR function);
     * <li>'SELECT * FROM dept' is not a cursor.
     * </ul>
     *
     * <p>Note that in {@link #shouldAllowIntermediateOrderBy()}, an
     * 'intermediate' query is any query that is not a cursor.
     *
     * @param node Parse tree node representing a query
     * @return Whether parse tree node is a cursor
     */
    boolean isCursor(SqlNode node);

    /**
     * Converts a window specification or window name into a fully-resolved
     * window specification. For example, in <code>SELECT sum(x) OVER (PARTITION
     * BY x ORDER BY y), sum(y) OVER w1, sum(z) OVER (w ORDER BY y) FROM t
     * WINDOW w AS (PARTITION BY x)</code> all aggregations have the same
     * resolved window specification <code>(PARTITION BY x ORDER BY y)</code>.
     *
     * @param windowOrRef Either the name of a window (a {@link SqlIdentifier})
     * or a window specification (a {@link SqlWindow}).
     * @param scope Scope in which to resolve window names
     * @param populateBounds Whether to populate bounds. Doing so may alter the
     * definition of the window. It is recommended that populate bounds when
     * translating to physical algebra, but not when validating.
     *
     * @return A window
     *
     * @throws RuntimeException Validation exception if window does not exist
     */
    SqlWindow resolveWindow(
        SqlNode windowOrRef,
        SqlValidatorScope scope,
        boolean populateBounds);

    /**
     * Finds the namespace corresponding to a given node.
     *
     * <p>For example, in the query <code>SELECT * FROM (SELECT * FROM t), t1 AS
     * alias</code>, the both items in the FROM clause have a corresponding
     * namespace.
     *
     * @param node Parse tree node
     *
     * @return namespace of node
     */
    SqlValidatorNamespace getNamespace(SqlNode node);

    /**
     * Derives an alias for an expression. If no alias can be derived, returns
     * null if <code>ordinal</code> is less than zero, otherwise generates an
     * alias <code>EXPR$<i>ordinal</i></code>.
     *
     * @param node Expression
     * @param ordinal Ordinal of expression
     *
     * @return derived alias, or null if no alias can be derived and ordinal is
     * less than zero
     */
    String deriveAlias(
        SqlNode node,
        int ordinal);

    /**
     * Returns the select list of a query, with every occurrence of "&#42;" or
     * "TABLE.&#42;" expanded.
     *
     * <p>Caches the results to save effort when the expanded select list is
     * used multiple times during the validation and translation of a query.</p>
     *
     * @param query Query
     * @return expanded select clause
     */
    SqlNodeList getExpandedSelectList(SqlSelect query);

    /**
     * Returns the scope that expressions in the WHERE and GROUP BY clause of
     * this query should use. This scope consists of the tables in the FROM
     * clause, and the enclosing scope.
     *
     * @param select Query
     *
     * @return naming scope of WHERE clause
     */
    SqlValidatorScope getWhereScope(SqlSelect select);

    /**
     * Returns the type factory used by this validator.
     *
     * @return type factory
     */
    RelDataTypeFactory getTypeFactory();

    /**
     * Saves the type of a {@link SqlNode}, now that it has been validated.
     *
     * <p>NOTE: Use with caution! Generally only the validator should call this
     * method. Code outside the validator should treat derived type information
     * as read-only, so it is consistent throughout the validation process.
     *
     * @param node A SQL parse tree node
     * @param type Its type; must not be null
     *
     * @pre type != null
     * @pre node != null
     */
    void setValidatedNodeType(
        SqlNode node,
        RelDataType type);

    /**
     * Removes a node from the set of validated nodes
     *
     * @param node node to be removed
     */
    void removeValidatedNodeType(SqlNode node);

    /**
     * Returns an object representing the "unknown" type.
     *
     * @return unknown type
     */
    RelDataType getUnknownType();

    /**
     * Returns the appropriate scope for validating a particular clause of a
     * SELECT statement.
     *
     * <p>Consider
     *
     * <blockquote><code>
     * <pre>SELECT *
     * FROM foo
     * WHERE EXISTS (
     *    SELECT deptno AS x
     *    FROM emp
     *       JOIN dept ON emp.deptno = dept.deptno
     *    WHERE emp.deptno = 5
     *    GROUP BY deptno
     *    ORDER BY x)</pre>
     * </code></blockquote>
     *
     * What objects can be seen in each part of the sub-query?
     *
     * <ul>
     * <li>In FROM ({@link #getFromScope} , you can only see 'foo'.
     * <li>In WHERE ({@link #getWhereScope}), GROUP BY ({@link #getGroupScope}),
     * SELECT ({@code getSelectScope}), and the ON clause of the JOIN ({@link
     * #getJoinScope}) you can see 'emp', 'dept', and 'foo'.
     * <li>In ORDER BY ({@link #getOrderScope}), you can see the column alias
     * 'x'; and tables 'emp', 'dept', and 'foo'.
     * </ul>
     *
     * @param select SELECT statement
     *
     * @return naming scope for SELECT statement
     */
    SqlValidatorScope getSelectScope(SqlSelect select);

    /**
     * Returns the scope for resolving the SELECT, GROUP BY and HAVING clauses.
     * Always a {@link SelectScope}; if this is an aggregation query, the {@link
     * AggregatingScope} is stripped away.
     *
     * @param select SELECT statement
     *
     * @return naming scope for SELECT statement, sans any aggregating scope
     */
    SelectScope getRawSelectScope(SqlSelect select);

    /**
     * Returns a scope containing the objects visible from the FROM clause of a
     * query.
     *
     * @param select SELECT statement
     *
     * @return naming scope for FROM clause
     */
    SqlValidatorScope getFromScope(SqlSelect select);

    /**
     * Returns a scope containing the objects visible from the ON and USING
     * sections of a JOIN clause.
     *
     * @param node The item in the FROM clause which contains the ON or USING
     * expression
     *
     * @return naming scope for JOIN clause
     *
     * @see #getFromScope
     */
    SqlValidatorScope getJoinScope(SqlNode node);

    /**
     * Returns a scope containing the objects visible from the GROUP BY clause
     * of a query.
     *
     * @param select SELECT statement
     *
     * @return naming scope for GROUP BY clause
     */
    SqlValidatorScope getGroupScope(SqlSelect select);

    /**
     * Returns a scope containing the objects visible from the HAVING clause of
     * a query.
     *
     * @param select SELECT statement
     *
     * @return naming scope for HAVING clause
     */
    SqlValidatorScope getHavingScope(SqlSelect select);

    /**
     * Returns the scope that expressions in the SELECT and HAVING clause of
     * this query should use. This scope consists of the FROM clause and the
     * enclosing scope. If the query is aggregating, only columns in the GROUP
     * BY clause may be used.
     *
     * @param select SELECT statement
     *
     * @return naming scope for ORDER BY clause
     */
    SqlValidatorScope getOrderScope(SqlSelect select);

    /**
     * Declares a SELECT expression as a cursor.
     *
     * @param select select expression associated with the cursor
     * @param scope scope of the parent query associated with the cursor
     */
    void declareCursor(SqlSelect select, SqlValidatorScope scope);

    /**
     * Pushes a new instance of a function call on to a function call stack.
     */
    void pushFunctionCall();

    /**
     * Removes the topmost entry from the function call stack.
     */
    void popFunctionCall();

    /**
     * Retrieves the name of the parent cursor referenced by a column list
     * parameter.
     *
     * @param columnListParamName name of the column list parameter
     *
     * @return name of the parent cursor
     */
    String getParentCursor(String columnListParamName);

    /**
     * Enables or disables expansion of identifiers other than column
     * references.
     *
     * @param expandIdentifiers new setting
     */
    void setIdentifierExpansion(boolean expandIdentifiers);

    /**
     * Enables or disables expansion of column references. (Currently this does
     * not apply to the ORDER BY clause; may be fixed in the future.)
     *
     * @param expandColumnReferences new setting
     */
    void setColumnReferenceExpansion(boolean expandColumnReferences);

    /**
     * @return whether column reference expansion is enabled
     */
    boolean getColumnReferenceExpansion();

    /**
     * Returns expansion of identifiers.
     *
     * @return whether this validator should expand identifiers
     */
    boolean shouldExpandIdentifiers();

    /**
     * Whether to allow an ORDER BY clause in a subquery.
     *
     * <p>The default implementation says yes; the SQL standard says no.
     *
     * @return whether to allow an ORDER BY clause in a subquery
     */
    boolean shouldAllowIntermediateOrderBy();

    /**
     * Enables or disables rewrite of "macro-like" calls such as COALESCE.
     *
     * @param rewriteCalls new setting
     */
    void setCallRewrite(boolean rewriteCalls);

    /**
     * Derives the type of a constructor.
     *
     * @param scope Scope
     * @param call Call
     * @param unresolvedConstructor TODO
     * @param resolvedConstructor TODO
     * @param argTypes Types of arguments
     *
     * @return Resolved type of constructor
     */
    RelDataType deriveConstructorType(
        SqlValidatorScope scope,
        SqlCall call,
        SqlFunction unresolvedConstructor,
        SqlFunction resolvedConstructor,
        RelDataType [] argTypes);

    /**
     * Handles a call to a function which cannot be resolved, throwing an
     * appropriately descriptive error.
     *
     * @param call Call
     * @param unresolvedFunction Overloaded function which is the target of the
     * call
     * @param argTypes Types of arguments
     */
    void handleUnresolvedFunction(
        SqlCall call,
        SqlFunction unresolvedFunction,
        RelDataType [] argTypes);

    /**
     * Expands an expression in the ORDER BY clause into an expression with the
     * same semantics as expressions in the SELECT clause.
     *
     * <p>This is made necessary by a couple of dialect 'features':
     *
     * <ul>
     * <li><b>ordinal expressions</b>: In "SELECT x, y FROM t ORDER BY 2", the
     * expression "2" is shorthand for the 2nd item in the select clause, namely
     * "y".
     * <li><b>alias references</b>: In "SELECT x AS a, y FROM t ORDER BY a", the
     * expression "a" is shorthand for the item in the select clause whose alias
     * is "a"
     * </ul>
     *
     * @param select Select statement which contains ORDER BY
     * @param orderExpr Expression in the ORDER BY clause.
     *
     * @return Expression translated into SELECT clause semantics
     */
    SqlNode expandOrderExpr(SqlSelect select, SqlNode orderExpr);

    /**
     * Expands an expression.
     *
     * @param expr Expression
     * @param scope Scope
     *
     * @return Expanded expression
     */
    SqlNode expand(SqlNode expr, SqlValidatorScope scope);

    /**
     * Returns whether a field is a system field. Such fields may have
     * particular properties such as sortedness and nullability.
     *
     * <p>In the default implementation, always returns {@code false}.
     *
     * @param field Field
     *
     * @return whether field is a system field
     */
    boolean isSystemField(RelDataTypeField field);

    /**
     * Returns a list of fields to be prefixed to each relational expression.
     *
     * <p>The default implementation returns the empty list.
     *
     * @return List of system fields
     */
    List<RelDataTypeField> getSystemFields();

    /**
     * Returns a description of how each field in the row type maps to a
     * catalog, schema, table and column in the schema.
     *
     * <p>The returned list is never null, and has one element for each field
     * in the row type. Each element is a list of four elements (catalog,
     * schema, table, column), or may be null if the column is an expression.
     *
     * @param sqlQuery Query
     * @return Description of how each field in the row type maps to a schema
     *     object
     */
    List<List<String>> getFieldOrigins(SqlNode sqlQuery);

    /**
     * Returns the row type of a cursor argument to a UDX call.
     *
     * @param cursorCall Call to the CURSOR operator wrapped around a query
     * @return Row type of the cursor
     */
    RelDataType getCursorRowType(SqlCall cursorCall);

    /**
     * Calculates the monotonicity of an AS expression.
     *
     * @param exprMonotonicity
     *            monotonicity of expression
     * @param alias
     *            name of alias
     * @return monotonicity of AS expression
     */
    SqlMonotonicity getAliasedMonotonicity(
        SqlMonotonicity exprMonotonicity, SqlIdentifier alias);
}

// End SqlValidator.java
