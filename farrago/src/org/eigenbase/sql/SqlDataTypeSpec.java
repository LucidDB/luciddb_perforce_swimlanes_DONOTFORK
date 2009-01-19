/*
// $Id$
// Package org.eigenbase is a class library of data management components.
// Copyright (C) 2005-2007 The Eigenbase Project
// Copyright (C) 2002-2007 Disruptive Tech
// Copyright (C) 2005-2007 LucidEra, Inc.
// Portions Copyright (C) 2003-2007 John V. Sichi
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
package org.eigenbase.sql;

import java.nio.charset.*;

import java.util.*;

import org.eigenbase.reltype.*;
import org.eigenbase.resource.*;
import org.eigenbase.sql.parser.*;
import org.eigenbase.sql.type.*;
import org.eigenbase.sql.util.*;
import org.eigenbase.sql.validate.*;
import org.eigenbase.util.*;


/**
 * Represents a SQL data type specification in a parse tree.
 *
 * <p>A <code>SqlDataTypeSpec</code> is immutable; once created, you cannot
 * change any of the fields.
 *
 * <p>todo: This should really be a subtype of {@link SqlCall}.
 *
 * <p>In its full glory, we will have to support complex type expressions like
 *
 * <blockquote><code>ROW( NUMBER(5,2) NOT NULL AS foo, ROW( BOOLEAN AS b, MyUDT
 * NOT NULL AS i ) AS rec )</code></blockquote>
 *
 * <p>Currently it only supports simple datatypes like CHAR, VARCHAR and DOUBLE,
 * with optional precision and scale.
 *
 * @author Lee Schumacher
 * @version $Id$
 * @since Jun 4, 2004
 */
public class SqlDataTypeSpec
    extends SqlNode
{
    //~ Instance fields --------------------------------------------------------

    private final SqlIdentifier collectionsTypeName;
    private final SqlIdentifier typeName;
    private final int scale;
    private final int precision;
    private final String charSetName;
    private final TimeZone timeZone;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a type specification.
     */
    public SqlDataTypeSpec(
        final SqlIdentifier typeName,
        int precision,
        int scale,
        String charSetName,
        TimeZone timeZone,
        SqlParserPos pos)
    {
        super(pos);
        this.collectionsTypeName = null;
        this.typeName = typeName;
        this.scale = scale;
        this.precision = precision;
        this.charSetName = charSetName;
        this.timeZone = timeZone;
    }

    /**
     * Creates a type specification representing a collection type.
     */
    public SqlDataTypeSpec(
        SqlIdentifier collectionsTypeName,
        SqlIdentifier typeName,
        int precision,
        int scale,
        String charSetName,
        SqlParserPos pos)
    {
        super(pos);
        this.collectionsTypeName = collectionsTypeName;
        this.typeName = typeName;
        this.scale = scale;
        this.precision = precision;
        this.charSetName = charSetName;
        this.timeZone = null;
    }

    //~ Methods ----------------------------------------------------------------

    public SqlNode clone(SqlParserPos pos)
    {
        return (collectionsTypeName == null)
            ? new SqlDataTypeSpec(
                collectionsTypeName,
                typeName,
                precision,
                scale,
                charSetName,
                pos)
            : new SqlDataTypeSpec(
                typeName,
                precision,
                scale,
                charSetName,
                timeZone,
                pos);
    }

    public SqlMonotonicity getMonotonicity(SqlValidatorScope scope)
    {
        return SqlMonotonicity.Constant;
    }

    public SqlIdentifier getCollectionsTypeName()
    {
        return collectionsTypeName;
    }

    public SqlIdentifier getTypeName()
    {
        return typeName;
    }

    public int getScale()
    {
        return scale;
    }

    public int getPrecision()
    {
        return precision;
    }

    public String getCharSetName()
    {
        return charSetName;
    }

    public TimeZone getTimeZone()
    {
        return timeZone;
    }

    /**
     * Returns a new SqlDataTypeSpec corresponding to the component type if the
     * type spec is a collections type spec.<br>
     * Collection types are <code>ARRAY</code> and <code>MULTISET</code>.
     *
     * @pre null != getCollectionsTypeName()
     */
    public SqlDataTypeSpec getComponentTypeSpec()
    {
        Util.pre(
            null != getCollectionsTypeName(),
            "null != getCollectionsTypeName()");
        return new SqlDataTypeSpec(
            typeName,
            precision,
            scale,
            charSetName,
            timeZone,
            getParserPosition());
    }

    public void unparse(
        SqlWriter writer,
        int leftPrec,
        int rightPrec)
    {
        String name = typeName.getSimple();
        if (SqlTypeName.get(name) != null) {
            SqlTypeName sqlTypeName = SqlTypeName.get(name);

            // we have a built-in data type
            writer.keyword(name);

            if (sqlTypeName.allowsPrec() && (precision >= 0)) {
                final SqlWriter.Frame frame =
                    writer.startList(SqlWriter.FrameTypeEnum.FunCall, "(", ")");
                writer.print(precision);
                if (sqlTypeName.allowsScale() && (scale >= 0)) {
                    writer.sep(",", true);
                    writer.print(scale);
                }
                writer.endList(frame);
            }

            if (charSetName != null) {
                writer.keyword("CHARACTER SET");
                writer.identifier(charSetName);
            }

            if (collectionsTypeName != null) {
                writer.keyword(collectionsTypeName.getSimple());
            }
        } else {
            // else we have a user defined type
            typeName.unparse(writer, leftPrec, rightPrec);
        }
    }

    public void validate(SqlValidator validator, SqlValidatorScope scope)
    {
        validator.validateDataType(this);
    }

    public <R> R accept(SqlVisitor<R> visitor)
    {
        return visitor.visit(this);
    }

    public boolean equalsDeep(SqlNode node, boolean fail)
    {
        if (!(node instanceof SqlDataTypeSpec)) {
            assert !fail : this + "!=" + node;
            return false;
        }
        SqlDataTypeSpec that = (SqlDataTypeSpec) node;
        if (!SqlNode.equalDeep(
                this.collectionsTypeName,
                that.collectionsTypeName,
                fail))
        {
            return false;
        }
        if (!this.typeName.equalsDeep(that.typeName, fail)) {
            return false;
        }
        if (this.precision != that.precision) {
            assert !fail : this + "!=" + node;
            return false;
        }
        if (this.scale != that.scale) {
            assert !fail : this + "!=" + node;
            return false;
        }
        if (!Util.equal(this.timeZone, that.timeZone)) {
            assert !fail : this + "!=" + node;
            return false;
        }
        if (!Util.equal(this.charSetName, that.charSetName)) {
            assert !fail : this + "!=" + node;
            return false;
        }
        return true;
    }

    /**
     * Throws an error if the type is not built-in.
     */
    public RelDataType deriveType(SqlValidator validator)
    {
        String name = typeName.getSimple();

        //for now we only support builtin datatypes
        if (SqlTypeName.get(name) == null) {
            throw validator.newValidationError(
                this,
                EigenbaseResource.instance().UnknownDatatypeName.ex(name));
        }

        if (null != collectionsTypeName) {
            final String collectionName = collectionsTypeName.getSimple();
            if (!(SqlTypeName.get(collectionName) != null)) {
                throw validator.newValidationError(
                    this,
                    EigenbaseResource.instance().UnknownDatatypeName.ex(
                        collectionName));
            }
        }

        RelDataTypeFactory typeFactory = validator.getTypeFactory();
        return deriveType(typeFactory);
    }

    /**
     * Does not throw an error if the type is not built-in.
     */
    public RelDataType deriveType(RelDataTypeFactory typeFactory)
    {
        String name = typeName.getSimple();

        SqlTypeName sqlTypeName = SqlTypeName.get(name);

        // NOTE jvs 15-Jan-2009:  earlier validation is supposed to
        // have caught these, which is why it's OK for them
        // to be assertions rather than user-level exceptions.
        RelDataType type;
        if ((precision >= 0) && (scale >= 0)) {
            assert (sqlTypeName.allowsPrecScale(true, true));
            type = typeFactory.createSqlType(sqlTypeName, precision, scale);
        } else if (precision >= 0) {
            assert (sqlTypeName.allowsPrecNoScale());
            type = typeFactory.createSqlType(sqlTypeName, precision);
        } else {
            assert (sqlTypeName.allowsNoPrecNoScale());
            type = typeFactory.createSqlType(sqlTypeName);
        }

        if (SqlTypeUtil.inCharFamily(type)) {
            // Applying Syntax rule 10 from SQL:99 spec section 6.22 "If TD is a
            // fixed-length, variable-length or large object character string,
            // then the collating sequence of the result of the <cast
            // specification> is the default collating sequence for the
            // character repertoire of TD and the result of the <cast
            // specification> has the Coercible coercibility characteristic."
            SqlCollation collation =
                new SqlCollation(SqlCollation.Coercibility.Coercible);

            Charset charset;
            if (null == charSetName) {
                charset = typeFactory.getDefaultCharset();
            } else {
                String javaCharSetName =
                    SqlUtil.translateCharacterSetName(charSetName);
                charset = Charset.forName(javaCharSetName);
            }
            type =
                typeFactory.createTypeWithCharsetAndCollation(
                    type,
                    charset,
                    collation);
        }

        if (null != collectionsTypeName) {
            final String collectionName = collectionsTypeName.getSimple();

            SqlTypeName collectionsSqlTypeName =
                SqlTypeName.get(collectionName);

            switch (collectionsSqlTypeName) {
            case MULTISET:
                type = typeFactory.createMultisetType(type, -1);
                break;

            default:
                throw Util.unexpected(collectionsSqlTypeName);
            }
        }

        return type;
    }
}

// End SqlDataTypeSpec.java
