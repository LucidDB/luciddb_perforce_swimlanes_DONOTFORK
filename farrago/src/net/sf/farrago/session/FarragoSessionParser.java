/*
// Farrago is a relational database management system.
// Copyright (C) 2003-2004 John V. Sichi.
// Copyright (C) 2003-2004 Disruptive Tech
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
package net.sf.farrago.session;

import net.sf.farrago.catalog.FarragoReposTxnContext;


/**
 * FarragoSessionParser represents an object capable of parsing Farrago
 * SQL statements.
 *
 * @author John V. Sichi
 * @version $Id$
 */
public interface FarragoSessionParser
{
    //~ Methods ---------------------------------------------------------------

    /**
     * Parses an SQL statement.  If a DDL statement, implicitly
     * performs uncommitted catalog updates.
     *
     * @param ddlValidator the validator to use for lookup during parsing
     * if this turns out to be a DDL statement
     *
     * @param sql the SQL text to be parsed
     *
     * @return for DDL, a FarragoSessionDdlStmt; for DML or query, top-level
     * SqlNode
     */
    public Object parseSqlStatement(
        FarragoSessionDdlValidator ddlValidator,
        String sql);

    /**
     * @return the current position, or null if done parsing
     */
    public FarragoSessionParserPosition getCurrentPosition();

    /**
     * @return a comma-separated list of all a database's SQL keywords that are
     * NOT also SQL92 keywords.
     */
    public String getSQLKeywords();

    /**
     * @return a comma-separated list of string functions available with this
     * database
     */
    public String getStringFunctions();

    /**
     * @return a comma-separated list of math functions available with this
     * database
     */
    public String getNumericFunctions();

    /**
     * @return a comma-separated list of the time and date functions available
     * with this database
     */
    public String getTimeDateFunctions();

    /**
     * @return a comma-separated list of system functions available with this
     * database
     */
    public String getSystemFunctions();

    /**
     * @return validator to use for validating DDL statements as they are
     * parsed
     */
    public FarragoSessionDdlValidator getDdlValidator();

    /**
     * @return validator to use for validating statements as they are parse
     */
    public FarragoSessionStmtValidator getStmtValidator();
}


// End FarragoSessionParser.java
