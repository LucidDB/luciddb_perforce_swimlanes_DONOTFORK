/*
// $Id$
// Farrago is an extensible data management system.
// Copyright (C) 2005 The Eigenbase Project
// Copyright (C) 2005 SQLstream, Inc.
// Copyright (C) 2005 Dynamo BI Corporation
// Portions Copyright (C) 2004 John V. Sichi
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
package net.sf.farrago.ddl;

import java.nio.charset.*;

import java.util.*;
import java.util.logging.*;

import net.sf.farrago.catalog.*;
import net.sf.farrago.cwm.core.*;
import net.sf.farrago.cwm.relational.*;
import net.sf.farrago.cwm.relational.enumerations.*;
import net.sf.farrago.fem.med.*;
import net.sf.farrago.fem.sql2003.*;
import net.sf.farrago.resource.*;
import net.sf.farrago.session.*;
import net.sf.farrago.trace.*;

import org.eigenbase.jmi.*;
import org.eigenbase.reltype.*;
import org.eigenbase.sql.*;
import org.eigenbase.sql.parser.*;
import org.eigenbase.sql.type.*;
import org.eigenbase.sql.validate.*;
import org.eigenbase.util.*;


/**
 * DdlHandler is an abstract base for classes which provide implementations for
 * the actions taken by {@link DdlValidator} on individual objects. See {@link
 * FarragoSessionDdlHandler} for an explanation.
 *
 * @author John V. Sichi
 * @version $Id$
 */
public abstract class DdlHandler
    implements ReflectiveVisitor
{
    //~ Static fields/initializers ---------------------------------------------

    protected static final Logger tracer = FarragoTrace.getDdlValidatorTracer();

    //~ Instance fields --------------------------------------------------------

    protected final FarragoSessionDdlValidator validator;

    protected final FarragoRepos repos;

    /**
     * An instance of FarragoResource for use in throwing validation errors. The
     * name is intentionally short to keep line length under control.
     */
    protected final FarragoResource res;

    //~ Constructors -----------------------------------------------------------

    public DdlHandler(FarragoSessionDdlValidator validator)
    {
        this.validator = validator;
        repos = validator.getRepos();
        res = FarragoResource.instance();
    }

    //~ Methods ----------------------------------------------------------------

    public FarragoSessionDdlValidator getValidator()
    {
        return validator;
    }

    public void validateAttributeSet(CwmClass cwmClass)
    {
        List<FemAbstractAttribute> attributes =
            Util.filter(
                cwmClass.getFeature(),
                FemAbstractAttribute.class);
        validator.validateUniqueNames(
            cwmClass,
            attributes,
            false);

        for (FemAbstractAttribute attribute : attributes) {
            validateAttribute(attribute);
        }
    }

    public void validateBaseColumnSet(FemBaseColumnSet columnSet)
    {
        validateAttributeSet(columnSet);

        // REVIEW jvs 21-Jan-2005:  something fishy here...

        // Foreign tables should not support constraint definitions.  Eventually
        // we may want to allow this as a hint to the optimizer, but it's not
        // standard so for now we should prevent it.
        for (CwmModelElement obj : columnSet.getOwnedElement()) {
            if (!(obj instanceof FemAbstractKeyConstraint)) {
                continue;
            }
            throw res.ValidatorNoConstraintAllowed.ex(
                repos.getLocalizedObjectName(columnSet));
        }
    }

    public void validateAttribute(FemAbstractAttribute attribute)
    {
        // REVIEW jvs 26-Feb-2005:  This relies on the fact that
        // attributes always come before operations.  We'll need to
        // take this into account in the implementation of ALTER TYPE.
        int ordinal = attribute.getOwner().getFeature().indexOf(attribute);
        assert (ordinal != -1);
        attribute.setOrdinal(ordinal);

        if (attribute.getInitialValue() == null) {
            CwmExpression nullExpression = repos.newCwmExpression();
            nullExpression.setLanguage("SQL");
            nullExpression.setBody("NULL");
            attribute.setInitialValue(nullExpression);
        }

        // if NOT NULL not specified, default to nullable
        if (attribute.getIsNullable() == null) {
            attribute.setIsNullable(NullableTypeEnum.COLUMN_NULLABLE);
        }
        if (!attribute.getIsNullable().equals(
                NullableTypeEnum.COLUMN_NO_NULLS))
        {
            if (attribute instanceof FemStoredColumn) {
                // Store the original user declaration, since we might override
                // isNullable with derived information (for example, columns in
                // the primary key never allow nulls).  This is so that if a
                // ALTER TABLE later changes something (e.g. DROP PRIMARY KEY),
                // we'll know whether the user actually declared a NOT NULL
                // constraint or not.
                FemStoredColumn col = (FemStoredColumn) attribute;
                col.setDeclaredNullable(true);

                // Now, compute the derived nullability and overwrite
                // isNullable with that.  Note that local data wrappers
                // may also do more of this later; for example, FTRS
                // marks all columns in the clustered index as
                // NOT NULL.
                CwmClassifier owner = col.getOwner();
                FemPrimaryKeyConstraint primaryKey =
                    FarragoCatalogUtil.getPrimaryKey(owner);
                if (primaryKey != null) {
                    if (primaryKey.getFeature().contains(col)) {
                        col.setIsNullable(
                            NullableTypeEnum.COLUMN_NO_NULLS);
                    }
                }
            }
        }

        validateTypedElement(
            attribute,
            attribute.getOwner());

        if (attribute.getInitialValue() != null) {
            String defaultExpression = attribute.getInitialValue().getBody();

            if (!defaultExpression.equalsIgnoreCase("NULL")) {
                FarragoSession session = validator.newReentrantSession();
                try {
                    validateDefaultClause(
                        attribute, session, defaultExpression);
                } catch (Throwable ex) {
                    throw validator.newPositionalError(
                        attribute,
                        res.ValidatorBadDefaultClause.ex(
                            repos.getLocalizedObjectName(attribute),
                            ex));
                } finally {
                    validator.releaseReentrantSession(session);
                }
            }
        }
    }

    private void convertSqlToCatalogType(
        SqlDataTypeSpec dataType,
        FemSqltypedElement element)
    {
        CwmSqldataType type =
            validator.getStmtValidator().findSqldataType(
                dataType.getTypeName());

        element.setType(type);
        if (dataType.getPrecision() >= 0) {
            element.setPrecision(dataType.getPrecision());
        }
        if (dataType.getScale() >= 0) {
            element.setScale(dataType.getScale());
        }
        if (dataType.getCharSetName() != null) {
            element.setCharacterSetName(dataType.getCharSetName());
        }
    }

    public void validateTypedElement(
        FemAbstractTypedElement abstractElement,
        CwmNamespace cwmNamespace)
    {
        validateTypedElement(
            FarragoCatalogUtil.toFemSqltypedElement(abstractElement),
            cwmNamespace);
    }

    private void validateTypedElement(
        FemSqltypedElement element,
        CwmNamespace cwmNamespace)
    {
        final FemAbstractTypedElement abstractElement =
            element.getModelElement();

        Object typeObj = validator.getSqlDefinition(abstractElement);

        // Special handling for cursor and columnList types
        if (typeObj == null) {
            if (element.getType().getName().equals("CURSOR")
                || element.getType().getName().equals("COLUMN_LIST"))
            {
                // previously validated
                return;
            }
        }
        if (typeObj instanceof SqlIdentifier) {
            SqlIdentifier id = (SqlIdentifier) typeObj;
            assert (id.getSimple().equals("CURSOR")
                || id.getSimple().equals("COLUMN_LIST"));
            element.setType(
                validator.getStmtValidator().findSqldataType(id));
            element.setCollationName("");
            element.setCharacterSetName("");
            return;
        }

        SqlDataTypeSpec dataType = (SqlDataTypeSpec) typeObj;
        if (dataType != null) {
            convertSqlToCatalogType(dataType, element);

            try {
                validator.getStmtValidator().validateDataType(dataType);
            } catch (SqlValidatorException ex) {
                throw validator.newPositionalError(
                    abstractElement,
                    res.ValidatorDefinitionError.ex(
                        ex.getMessage(),
                        repos.getLocalizedObjectName(abstractElement)));
            }
        } else {
            // assume that we're revalidating a previously saved element
            // so we can skip type validation altogether
        }

        CwmSqldataType type = (CwmSqldataType) element.getType();
        SqlTypeName typeName = SqlTypeName.get(type.getName());

        // REVIEW jvs 23-Mar-2005:  For now, we attach the dependency to
        // a containing namespace.  For example, if a column is declared
        // with a UDT for its type, the containing table depends on the
        // type.  This isn't SQL-kosher; the dependency is supposed to
        // be at the column granularity.  To fix this, we need two things:
        // (1) the ability to declare dependencies from non-namespaces, and
        // (2) the ability to correctly cascade the DROP at the column level.
        if (type instanceof FemUserDefinedType) {
            boolean method = false;
            if (cwmNamespace instanceof FemRoutine) {
                FemRoutine routine = (FemRoutine) cwmNamespace;
                if (FarragoCatalogUtil.isRoutineMethod(routine)) {
                    if (routine.getSpecification().getOwner().equals(type)) {
                        // This is a method of the type in question.  In this
                        // case, we don't create a dependency, because the
                        // circularity would foul up DROP.
                        method = true;
                    }
                }
            }
            if (!method) {
                validator.createDependency(
                    cwmNamespace,
                    Collections.singleton(type));
            }
        }

        // NOTE: parser only generates precision, but CWM discriminates
        // precision from length, so we take care of it below
        SqlTypeFamily typeFamily = null;
        if (typeName != null) {
            typeFamily = SqlTypeFamily.getFamilyForSqlType(typeName);
        }
        if ((typeFamily == SqlTypeFamily.CHARACTER)
            || (typeFamily == SqlTypeFamily.BINARY))
        {
            // convert precision to length
            if (element.getPrecision() != null) {
                // Minimum column length for char and binary is 1
                if (element.getPrecision().intValue() == 0) {
                    element.setLength(Integer.valueOf(1));
                } else {
                    element.setLength(element.getPrecision());
                }
                element.setPrecision(null);
            }
        }

        if (typeFamily == SqlTypeFamily.CHARACTER) {
            // TODO jvs 18-April-2004:  Should be inheriting these defaults
            // from schema/catalog.
            if (JmiObjUtil.isBlank(element.getCharacterSetName())) {
                // NOTE: don't leave character set name implicit, since if the
                // default ever changed, that would invalidate existing data
                element.setCharacterSetName(
                    repos.getDefaultCharsetName());
            }
        }

        // now, enforce type-defined limits
        if (type instanceof CwmSqlsimpleType) {
            CwmSqlsimpleType simpleType = (CwmSqlsimpleType) type;
        } else if (type instanceof FemSqlcollectionType) {
            FemSqlcollectionType collectionType = (FemSqlcollectionType) type;
            FemSqltypeAttribute componentType =
                (FemSqltypeAttribute) collectionType.getFeature().get(0);
            validateAttribute(componentType);
        } else if (type instanceof FemUserDefinedType) {
            // nothing special to do for UDT's, which were already validated on
            // creation
        } else if (type instanceof FemSqlrowType) {
            FemSqlrowType rowType = (FemSqlrowType) type;
            for (
                FemAbstractAttribute column
                : Util.cast(rowType.getFeature(), FemAbstractAttribute.class))
            {
                validateAttribute(column);
            }
        } else {
            throw Util.needToImplement(type);
        }

        // REVIEW jvs 18-April-2004: I had to put these in because CWM
        // declares them as mandatory.  This is stupid, since CWM also says
        // these fields are inapplicable for non-character types.
        if (element.getCollationName() == null) {
            element.setCollationName("");
        }

        if (element.getCharacterSetName() == null) {
            element.setCharacterSetName("");
        }

        validator.getStmtValidator().setParserPosition(null);
    }

    /**
     * Adds position information to an exception.
     *
     * <p>This method is similar to {@link
     * SqlValidator#newValidationError(SqlNode, SqlValidatorException)} and
     * should be unified with it, if only we could figure out how.
     *
     * @param element Element which had the error, and is therefore the locus of
     * the exception
     * @param e Exception raised
     *
     * @return Exception with position information
     */
    private RuntimeException newContextException(
        CwmModelElement element,
        Exception e)
    {
        SqlParserPos pos = validator.getParserOffset(element);
        if (pos == null) {
            pos = SqlParserPos.ZERO;
        }
        return SqlUtil.newContextException(pos, e);
    }

    /**
     * Initializes a {@link CwmColumn} definition based on a {@link
     * RelDataTypeField}.
     *
     * <p>As well as calling {@link CwmColumn#setType(CwmClassifier)}, also
     * calls {@link CwmColumn#setPrecision(Integer)}, {@link
     * CwmColumn#setScale(Integer)} and {@link
     * CwmColumn#setIsNullable(NullableType)}.
     *
     * <p>If the column has no name, the name is initialized from the field
     * name; otherwise, the existing name is left unmodified.
     *
     * @param field input field
     * @param column on input, contains unintialized CwmColumn instance;
     * @param owner The object which is to own any anonymous datatypes created;
     * typically, the table which this column belongs to
     *
     * @pre field != null && column != null && owner != null
     */
    public void convertFieldToCwmColumn(
        RelDataTypeField field,
        CwmColumn column,
        CwmNamespace owner)
    {
        assert (field != null) && (column != null) && (owner != null) : "pre";
        if (column.getName() == null) {
            final String name = field.getName();
            assert name != null;
            column.setName(name);
        }
        convertTypeToCwmColumn(
            field.getType(),
            column,
            owner);
    }

    /**
     * Populates a {@link CwmColumn} object with type information.
     *
     * <p>As well as calling {@link CwmColumn#setType(CwmClassifier)}, also
     * calls {@link CwmColumn#setPrecision(Integer)}, {@link
     * CwmColumn#setScale(Integer)} and {@link
     * CwmColumn#setIsNullable(NullableType)}.
     *
     * <p>If the type is structured or a multiset, the implementation is
     * recursive.
     *
     * @param type Type to convert
     * @param column Column to populate with type information
     * @param owner The object which is to own any anonymous datatypes created;
     * typically, the table which this column belongs to
     */
    private void convertTypeToCwmColumn(
        RelDataType type,
        CwmColumn column,
        CwmNamespace owner)
    {
        CwmSqldataType cwmType;
        final SqlTypeName typeName = type.getSqlTypeName();
        if (typeName == SqlTypeName.ROW) {
            Util.permAssert(
                type.isStruct(),
                "type.isStruct()");
            FemSqlrowType rowType = repos.newFemSqlrowType();
            rowType.setName("SYS$ROW_" + rowType.refMofId());
            final RelDataTypeField [] fields = type.getFields();
            for (int i = 0; i < fields.length; i++) {
                RelDataTypeField subField = fields[i];
                FemSqltypeAttribute subColumn = repos.newFemSqltypeAttribute();
                convertFieldToCwmColumn(subField, subColumn, owner);
                rowType.getFeature().add(subColumn);
            }

            // Attach the anonymous type to the owner of the column, to ensure
            // that it is destroyed.
            owner.getOwnedElement().add(rowType);
            cwmType = rowType;
        } else if (type.getComponentType() != null) {
            final RelDataType componentType = type.getComponentType();
            final FemSqlmultisetType multisetType =
                repos.newFemSqlmultisetType();
            multisetType.setName("SYS$MULTISET_" + multisetType.refMofId());
            final FemAbstractAttribute attr = repos.newFemSqltypeAttribute();
            attr.setName("SYS$MULTISET_COMPONENT_" + attr.refMofId());
            convertTypeToCwmColumn(componentType, attr, owner);
            multisetType.getFeature().add(attr);

            // Attach the anonymous type to the owner of the column, to ensure
            // that it is destroyed.
            owner.getOwnedElement().add(multisetType);
            cwmType = multisetType;
        } else {
            cwmType =
                validator.getStmtValidator().findSqldataType(
                    type.getSqlIdentifier());
            Util.permAssert(cwmType != null, "cwmType != null");
            if (typeName != null) {
                if (typeName.allowsPrec()) {
                    column.setPrecision(type.getPrecision());
                    if (typeName.allowsScale()) {
                        column.setScale(type.getScale());
                    }
                }
            } else {
                throw Util.needToImplement(type);
            }
        }
        column.setType(cwmType);
        if (type.isNullable()) {
            column.setIsNullable(NullableTypeEnum.COLUMN_NULLABLE);
        } else {
            column.setIsNullable(NullableTypeEnum.COLUMN_NO_NULLS);
        }
        Charset charset = type.getCharset();
        if (charset != null) {
            column.setCharacterSetName(charset.name());
        }
        SqlCollation collation = type.getCollation();
        if (collation != null) {
            column.setCollationName(collation.getCollationName());
        }
    }

    public Throwable adjustExceptionParserPosition(
        CwmModelElement modelElement,
        Throwable ex)
    {
        if (!(ex instanceof EigenbaseContextException)) {
            return ex;
        }
        EigenbaseContextException contextExcn = (EigenbaseContextException) ex;

        // We have context information for the query, and
        // need to adjust the position to match the original
        // DDL statement.
        SqlParserPos offsetPos = validator.getParserOffset(modelElement);
        int line = contextExcn.getPosLine();
        int col = contextExcn.getPosColumn();
        int endLine = contextExcn.getEndPosLine();
        int endCol = contextExcn.getEndPosColumn();
        if (line == 1) {
            col += (offsetPos.getColumnNum() - 1);
        }
        line += (offsetPos.getLineNum() - 1);
        if (endLine == 1) {
            endCol += (offsetPos.getColumnNum() - 1);
        }
        endLine += (offsetPos.getLineNum() - 1);

        return SqlUtil.newContextException(
            line,
            col,
            endLine,
            endCol,
            ex.getCause());
    }

    private void validateDefaultClause(
        FemAbstractAttribute attribute,
        FarragoSession session,
        String defaultExpression)
    {
        String sql = "VALUES(" + defaultExpression + ")";
        RelDataType rowType;

        FarragoSessionStmtContext stmtContext = null;
        try {
            // null param def factory okay because we won't use dynamic params
            stmtContext = session.newStmtContext(null);

            stmtContext.prepare(sql, false);
            rowType = stmtContext.getPreparedRowType();
            assert (rowType.getFieldList().size() == 1);

            if (stmtContext.getPreparedParamType().getFieldList().size() > 0) {
                throw validator.newPositionalError(
                    attribute,
                    res.ValidatorBadDefaultParam.ex(
                        repos.getLocalizedObjectName(attribute)));
            }
        } finally {
            if (stmtContext != null) {
                stmtContext.closeAllocation();
            }
        }

        // SQL standard is very picky about what can go in a DEFAULT clause
        RelDataType sourceType = rowType.getFields()[0].getType();
        RelDataTypeFamily sourceTypeFamily = sourceType.getFamily();

        RelDataType targetType =
            validator.getTypeFactory().createCwmElementType(attribute);
        RelDataTypeFamily targetTypeFamily = targetType.getFamily();

        if (sourceTypeFamily != targetTypeFamily) {
            throw validator.newPositionalError(
                attribute,
                res.ValidatorBadDefaultType.ex(
                    repos.getLocalizedObjectName(attribute),
                    targetTypeFamily.toString(),
                    sourceTypeFamily.toString()));
        }

        // TODO:  additional rules from standard, like no truncation allowed.
        // Maybe just execute SELECT with and without cast to target type and
        // make sure the same value comes back.
    }
}

// End DdlHandler.java
