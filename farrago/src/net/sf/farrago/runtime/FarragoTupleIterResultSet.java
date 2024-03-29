/*
// $Id$
// Farrago is an extensible data management system.
// Copyright (C) 2005 The Eigenbase Project
// Copyright (C) 2005 SQLstream, Inc.
// Copyright (C) 2005 Dynamo BI Corporation
// Portions Copyright (C) 2003 John V. Sichi
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
package net.sf.farrago.runtime;

import java.sql.*;

import java.util.*;
import java.util.logging.*;

import net.sf.farrago.jdbc.*;
import net.sf.farrago.session.*;
import net.sf.farrago.trace.*;
import net.sf.farrago.type.*;
import net.sf.farrago.type.runtime.*;

import org.eigenbase.reltype.*;
import org.eigenbase.runtime.*;


/**
 * FarragoTupleIterResultSet is a refinement of TupleIterResultSet which exposes
 * Farrago datatype semantics.
 *
 * @author John V. Sichi, Stephan Zuercher
 * @version $Id$
 */
public class FarragoTupleIterResultSet
    extends TupleIterResultSet
{
    //~ Static fields/initializers ---------------------------------------------

    protected static final Logger tracer =
        FarragoTrace.getFarragoTupleIterResultSetTracer();
    private static final Logger jdbcTracer =
        FarragoTrace.getFarragoJdbcEngineDriverTracer();

    //~ Instance fields --------------------------------------------------------

    private FarragoSessionRuntimeContext runtimeContext;
    private final RelDataType rowType;
    private final List<List<String>> fieldOrigins;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new FarragoTupleIterResultSet object. (Called from generated
     * code.)
     *
     * @param tupleIter underlying iterator
     * @param clazz Class for objects which iterator will produce
     * @param rowType type info for rows produced
     * @param runtimeContext runtime context for this execution
     */
    public FarragoTupleIterResultSet(
        TupleIter tupleIter,
        Class<?> clazz,
        RelDataType rowType,
        FarragoSessionRuntimeContext runtimeContext)
    {
        this(
            tupleIter,
            clazz,
            rowType,
            Collections.<List<String>>nCopies(rowType.getFieldCount(), null),
            runtimeContext,
            new SyntheticColumnGetter(clazz));
    }

    /**
     * Creates a new FarragoTupleIterResultSet object.
     *
     * @param tupleIter underlying iterator
     * @param clazz Class for objects which iterator will produce
     * @param rowType type info for rows produced
     * @param fieldOrigins Origin of each field in a column of a catalog object
     * @param runtimeContext runtime context for this execution
     * @param columnGetter object used to read individual columns from the the
     * underlying iterator
     */
    public FarragoTupleIterResultSet(
        TupleIter tupleIter,
        Class<?> clazz,
        RelDataType rowType,
        List<List<String>> fieldOrigins,
        FarragoSessionRuntimeContext runtimeContext,
        ColumnGetter columnGetter)
    {
        super(
            tupleIter,
            columnGetter == null
            ? new SyntheticColumnGetter(clazz)
            : columnGetter);
        assert rowType != null;
        this.rowType = rowType;
        if (fieldOrigins == null) {
            this.fieldOrigins =
                Collections.nCopies(rowType.getFieldCount(), null);
        } else {
            assert fieldOrigins.size() == rowType.getFieldCount();
            this.fieldOrigins = fieldOrigins;
        }
        this.runtimeContext = runtimeContext;
        if (tracer.isLoggable(Level.FINE)) {
            tracer.fine(toString());
        }
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * Signals that all aspects of opening this ResultSet have completed
     * successfully. After this method is called, the ResultSet must
     * (eventually) be closed or else resources may be leaked.
     */
    public void setOpened()
    {
        if (runtimeContext != null) {
            // Immediately detach session.  Another thread (think RMI) may be
            // the one to call next, we'll re-attach this session then.
            runtimeContext.detachMdrSession();
        }
    }

    // implement ResultSet
    public boolean next()
        throws SQLException
    {
        boolean detachMdrSession = false;
        try {
            if (tracer.isLoggable(Level.FINE)) {
                tracer.fine(toString());
            }
            if (runtimeContext != null) {
                // Inform context that cursor is becoming active, so any
                // subsequent cancel request has to wait until the
                // corresponding call in the finally block before cleaning up
                // the cursor.  This also checks for any pending cancel.
                runtimeContext.setCursorState(true);

                runtimeContext.reattachMdrSession();
                detachMdrSession = true;
            }
            boolean rc = super.next();
            if (!rc) {
                if (runtimeContext != null) {
                    FarragoSession session = runtimeContext.getSession();
                    if (session.isAutoCommit()) {
                        // According to the Javadoc for
                        // Connection.setAutoCommit, returning the last
                        // row of a cursor in autocommit mode ends
                        // the transaction.
                        if (detachMdrSession) {
                            // Close expects the session to be detached.
                            runtimeContext.detachMdrSession();
                            detachMdrSession = false;
                        }
                        runtimeContext.setCursorState(false);
                        close();
                    }
                }
            }
            return rc;
        } catch (Throwable ex) {
            // trace exceptions as part of JDBC API
            throw FarragoJdbcUtil.newSqlException(ex, jdbcTracer);
        } finally {
            if (runtimeContext != null) {
                if (detachMdrSession) {
                    runtimeContext.detachMdrSession();
                    detachMdrSession = false;
                }

                // Inform context that we're done with cursor processing until
                // next fetch call.
                try {
                    runtimeContext.setCursorState(false);
                } catch (Exception ex) {
                    // trace exceptions as part of JDBC API
                    throw FarragoJdbcUtil.newSqlException(ex, jdbcTracer);
                }
            }
        }
    }

    // implement ResultSet
    public ResultSetMetaData getMetaData()
        throws SQLException
    {
        return new FarragoResultSetMetaData(rowType, fieldOrigins);
    }

    // implement ResultSet
    public void close()
        throws SQLException
    {
        if (tracer.isLoggable(Level.FINE)) {
            tracer.fine(toString());
        }
        FarragoSessionRuntimeContext allocationToClose = runtimeContext;
        if (allocationToClose != null) {
            // NOTE:  this may be called reentrantly for daemon stmts,
            // so need special handling
            runtimeContext = null;

            // Lock session before sessionCtxt, to be consistent with global
            // locking strategy. In particular, FarragoDbStmtContext.close()
            // locks the session before it calls
            // FarragoSessionRuntimeContext.closeAllocation().
            synchronized (allocationToClose.getSession()) {
                allocationToClose.closeAllocation();
            }
        }
        super.close();
    }

    // implement AbstractResultSet
    protected Object getRaw(int columnIndex)
    {
        Object obj = super.getRaw(columnIndex);
        if (obj instanceof SpecialDataValue) {
            SpecialDataValue specialValue = (SpecialDataValue) obj;
            obj = specialValue.getSpecialData();
            wasNull = (obj == null);
        } else if (obj instanceof DataValue) {
            DataValue nullableValue = (DataValue) obj;
            obj = nullableValue.getNullableData();
            wasNull = (obj == null);
        } else {
            wasNull = false;
        }
        return obj;
    }
}

// End FarragoTupleIterResultSet.java
