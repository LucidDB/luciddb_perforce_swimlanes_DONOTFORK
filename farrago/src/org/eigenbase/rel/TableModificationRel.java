/*
// $Id$
// Saffron preprocessor and data engine
// (C) Copyright 2002-2003 Disruptive Technologies, Inc.
// (C) Copyright 2003-2004 John V. Sichi
// You must accept the terms in LICENSE.html to use this software.
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public License
// as published by the Free Software Foundation; either version 2.1
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
*/

package org.eigenbase.rel;

import org.eigenbase.relopt.RelOptPlanWriter;
import org.eigenbase.relopt.RelOptConnection;
import org.eigenbase.relopt.RelOptTable;
import org.eigenbase.reltype.RelDataType;
import org.eigenbase.relopt.RelOptCluster;
import org.eigenbase.relopt.RelOptUtil;
import org.eigenbase.util.EnumeratedValues;

import java.util.*;

/**
 * TableModificationRel is like TableAccessRel, but represents a request to
 * modify a table rather than read from it.  It takes one child which produces
 * the modified rows.  (For INSERT, the new values; for DELETE, the old values;
 * for UPDATE, all old values plus updated new values.)
 *
 * @version $Id$
 */
public class TableModificationRel extends SingleRel
{
    //~ Instance fields -------------------------------------------------------

    /** The connection to the optimizing session. */
    protected RelOptConnection connection;
    /** The table definition. */
    protected RelOptTable table;
    private Operation operation;
    private List updateColumnList;
    private RelDataType inputRowType;

    //~ Constructors ----------------------------------------------------------

    public TableModificationRel(
        RelOptCluster cluster,
        RelOptTable table,
        RelOptConnection connection,
        RelNode child,
        Operation operation,
        List updateColumnList)
    {
        super(cluster,child);
        this.table = table;
        this.connection = connection;
        this.operation = operation;
        this.updateColumnList = updateColumnList;
        if (table.getRelOptSchema() != null) {
            cluster.getPlanner().registerSchema(table.getRelOptSchema());
        }
    }

    //~ Methods ---------------------------------------------------------------

    public RelOptConnection getConnection()
    {
        return connection;
    }

    public RelOptTable getTable()
    {
        return table;
    }

    public List getUpdateColumnList()
    {
        return updateColumnList;
    }

    // implement Cloneable
    public Object clone()
    {
        return new TableModificationRel(
            cluster,
            table,
            connection,
            RelOptUtil.clone(child),
            operation,
            updateColumnList);
    }

    public Operation getOperation()
    {
        return operation;
    }

    // implement RelNode
    public RelDataType deriveRowType()
    {
        RelDataType [] types = new RelDataType[1];
        String [] fieldNames = new String[1];
        types[0] = cluster.typeFactory.createJavaType(Long.TYPE);
        fieldNames[0] = "ROWCOUNT";
        return cluster.typeFactory.createProjectType(types,fieldNames);
    }

    // override RelNode
    public RelDataType getExpectedInputRowType(int ordinalInParent)
    {
        assert(ordinalInParent == 0);

        if (inputRowType != null) {
            return inputRowType;
        }

        if (operation.equals(Operation.UPDATE)) {
            inputRowType = cluster.typeFactory.createJoinType(
                new RelDataType[]{
                    table.getRowType(),
                    RelOptUtil.createTypeFromProjection(
                        table.getRowType(),
                        updateColumnList)
                });
        } else {
            inputRowType = table.getRowType();
        }

        return inputRowType;
    }

    public void explain(RelOptPlanWriter pw)
    {
        pw.explain(
            this,
            new String [] { "child","table","operation","updateColumnList" },
            new Object [] {
                Arrays.asList(table.getQualifiedName()),
                getOperation(),
                (updateColumnList == null)
                ? Collections.EMPTY_LIST : updateColumnList
            });
    }

    /**
     * Enumeration of supported modification operations.
     */
    public static class Operation extends EnumeratedValues.BasicValue
    {
        public static final int INSERT_ORDINAL = 1;
        public static final Operation INSERT = new Operation("INSERT",INSERT_ORDINAL);
        public static final int UPDATE_ORDINAL = 2;
        public static final Operation UPDATE = new Operation("UPDATE",UPDATE_ORDINAL);
        public static final int DELETE_ORDINAL = 3;
        public static final Operation DELETE = new Operation("DELETE",DELETE_ORDINAL);

        private Operation(String name,int ordinal)
        {
            super(name,ordinal,null);
        }
        /**
         * List of all allowable {@link TableModificationRel.Operation} values.
         */
        public static final EnumeratedValues enumeration = new EnumeratedValues(
                new Operation[] {INSERT,UPDATE,DELETE});
        /**
         * Looks up a operation from its ordinal.
         */
        public static Operation get(int ordinal) {
            return (Operation) enumeration.getValue(ordinal);
        }
        /**
         * Looks up an operation from its name.
         */
        public static Operation get(String name) {
            return (Operation) enumeration.getValue(name);
        }
    }

}


// End TableModificationRel.java
