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

package net.sf.saffron.ext;

import org.eigenbase.relopt.RelOptConnection;
import org.eigenbase.relopt.RelOptSchema;


/**
 * A <code>JdbcConnection</code> is an implementation of {@link
 * RelOptConnection} which gets its data from a JDBC database.
 * 
 * <p>
 * Derived classes must implement {@link #getRelOptSchema} and {@link
 * #getRelOptSchemaStatic}.
 * </p>
 *
 * @author jhyde
 * @version $Id$
 *
 * @since 10 November, 2001
 */
public abstract class JdbcConnection implements RelOptConnection
{
    //~ Instance fields -------------------------------------------------------

    java.sql.Connection sqlConnection;

    //~ Constructors ----------------------------------------------------------

    public JdbcConnection(java.sql.Connection sqlConnection)
    {
        this.sqlConnection = sqlConnection;
    }

    //~ Methods ---------------------------------------------------------------

    public void setConnection(java.sql.Connection sqlConnection)
    {
        this.sqlConnection = sqlConnection;
    }

    public java.sql.Connection getConnection()
    {
        return sqlConnection;
    }

    // for Connection
    public static RelOptSchema getRelOptSchemaStatic()
    {
        throw new UnsupportedOperationException(
            "Derived class must implement "
            + "public static Schema getRelOptSchemaStatic()");
    }

    // implement Connection
    public Object contentsAsArray(String qualifier,String tableName)
    {
        throw new UnsupportedOperationException(
            "JdbcConnection.contentsAsArray() should have been replaced");
    }
}


// End JdbcConnection.java
