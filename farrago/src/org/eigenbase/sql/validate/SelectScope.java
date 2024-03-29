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

import java.util.*;

import org.eigenbase.sql.*;
import org.eigenbase.sql.fun.*;
import org.eigenbase.sql.parser.*;
import org.eigenbase.util.*;


/**
 * The name-resolution scope of a SELECT clause. The objects visible are those
 * in the FROM clause, and objects inherited from the parent scope.
 *
 * <p/>
 * <p>This object is both a {@link SqlValidatorScope} and a {@link
 * SqlValidatorNamespace}. In the query
 *
 * <p/>
 * <blockquote>
 * <pre>SELECT name FROM (
 *     SELECT *
 *     FROM emp
 *     WHERE gender = 'F')</code></blockquote>
 * <p/>
 * <p>we need to use the {@link SelectScope} as a
 * {@link SqlValidatorNamespace} when resolving 'name', and
 * as a {@link SqlValidatorScope} when resolving 'gender'.</p>
 * <p/>
 * <h3>Scopes</h3>
 * <p/>
 * <p>In the query
 * <p/>
 * <blockquote>
 * <pre>
 * SELECT expr1
 * FROM t1,
 *     t2,
 *     (SELECT expr2 FROM t3) AS q3
 * WHERE c1 IN (SELECT expr3 FROM t4)
 * ORDER BY expr4</pre>
 * </blockquote>
 *
 * <p/>The scopes available at various points of the query are as follows:
 *
 * <ul>
 * <li>expr1 can see t1, t2, q3</li>
 * <li>expr2 can see t3</li>
 * <li>expr3 can see t4, t1, t2</li>
 * <li>expr4 can see t1, t2, q3, plus (depending upon the dialect) any aliases
 * defined in the SELECT clause</li>
 * </ul>
 *
 * <p/>
 * <h3>Namespaces</h3>
 *
 * <p/>
 * <p>In the above query, there are 4 namespaces:
 *
 * <ul>
 * <li>t1</li>
 * <li>t2</li>
 * <li>(SELECT expr2 FROM t3) AS q3</li>
 * <li>(SELECT expr3 FROM t4)</li>
 * </ul>
 *
 * @author jhyde
 * @version $Id$
 * @see SelectNamespace
 * @since Mar 25, 2003
 */
public class SelectScope
    extends ListScope
{
    //~ Instance fields --------------------------------------------------------

    private final SqlSelect select;
    protected final List<String> windowNames = new ArrayList<String>();

    private List<SqlNode> expandedSelectList = null;

    /**
     * List of column names which sort this scope. Empty if this scope is not
     * sorted. Null if has not been computed yet.
     */
    private SqlNodeList orderList;

    /**
     * Scope to use to resolve windows
     */
    private final SqlValidatorScope windowParent;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a scope corresponding to a SELECT clause.
     *
     * @param parent Parent scope, must not be null
     * @param winParent Scope for window parent, may be null
     * @param select Select clause
     */
    SelectScope(
        SqlValidatorScope parent,
        SqlValidatorScope winParent,
        SqlSelect select)
    {
        super(parent);
        this.select = select;
        this.windowParent = winParent;
    }

    //~ Methods ----------------------------------------------------------------

    public SqlValidatorTable getTable()
    {
        return null;
    }

    public SqlSelect getNode()
    {
        return select;
    }

    public SqlWindow lookupWindow(String name)
    {
        final SqlNodeList windowList = select.getWindowList();
        for (int i = 0; i < windowList.size(); i++) {
            SqlWindow window = (SqlWindow) windowList.get(i);
            final SqlIdentifier declId = window.getDeclName();
            assert declId.isSimple();
            if (declId.names[0].equals(name)) {
                return window;
            }
        }

        // if not in the select scope, then check window scope
        if (windowParent != null) {
            return windowParent.lookupWindow(name);
        } else {
            return null;
        }
    }

    public SqlMonotonicity getMonotonicity(SqlNode expr)
    {
        SqlMonotonicity monotonicity = expr.getMonotonicity(this);
        if (monotonicity != SqlMonotonicity.NotMonotonic) {
            return monotonicity;
        }

        // TODO: compare fully qualified names
        final SqlNodeList orderList = getOrderList();
        if ((orderList.size() > 0)) {
            SqlNode order0 = (SqlNode) orderList.get(0);
            monotonicity = SqlMonotonicity.Increasing;
            if ((order0 instanceof SqlCall)
                && (((SqlCall) order0).getOperator()
                    == SqlStdOperatorTable.descendingOperator))
            {
                monotonicity = monotonicity.reverse();
                order0 = ((SqlCall) order0).getOperands()[0];
            }
            if (expr.equalsDeep(order0, false)) {
                return monotonicity;
            }
        }

        return SqlMonotonicity.NotMonotonic;
    }

    public SqlNodeList getOrderList()
    {
        if (orderList == null) {
            // Compute on demand first call.
            orderList = new SqlNodeList(SqlParserPos.ZERO);
            if (children.size() == 1) {
                final List<Pair<SqlNode, SqlMonotonicity>> monotonicExprs =
                    children.get(0).getMonotonicExprs();
                if (monotonicExprs.size() > 0) {
                    orderList.add(monotonicExprs.get(0).left);
                }
            }
        }
        return orderList;
    }

    public void addWindowName(String winName)
    {
        windowNames.add(winName);
    }

    public boolean existingWindowName(String winName)
    {
        String listName;
        ListIterator<String> entry = windowNames.listIterator();
        while (entry.hasNext()) {
            listName = entry.next();
            if (0 == listName.compareToIgnoreCase(winName)) {
                return true;
            }
        }

        // if the name wasn't found then check the parent(s)
        SqlValidatorScope walker = parent;
        while ((null != walker) && !(walker instanceof EmptyScope)) {
            if (walker instanceof SelectScope) {
                final SelectScope parentScope = (SelectScope) walker;
                return parentScope.existingWindowName(winName);
            }
            walker = parent;
        }

        return false;
    }

    public List<SqlNode> getExpandedSelectList()
    {
        return expandedSelectList;
    }

    public void setExpandedSelectList(List<SqlNode> selectList)
    {
        expandedSelectList = selectList;
    }
}

// End SelectScope.java
