/*
// $Id$
// Package org.eigenbase is a class library of data management components.
// Copyright (C) 2005-2005 The Eigenbase Project
// Copyright (C) 2002-2005 Disruptive Tech
// Copyright (C) 2005-2005 LucidEra, Inc.
// Portions Copyright (C) 2003-2005 John V. Sichi
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
package org.eigenbase.oj;

import java.util.*;

import openjava.mop.*;

import org.eigenbase.oj.util.*;
import org.eigenbase.reltype.*;
import org.eigenbase.runtime.*;
import org.eigenbase.sql.type.*;
import org.eigenbase.util.*;


/**
 * Implementation of {@link RelDataTypeFactory} based upon OpenJava's type
 * system.
 *
 * @author jhyde
 * @version $Id$
 * @see openjava.mop.OJClass
 * @see RelDataTypeFactory
 * @since May 30, 2003
 */
public class OJTypeFactoryImpl
    extends SqlTypeFactoryImpl
    implements OJTypeFactory
{

    //~ Instance fields --------------------------------------------------------

    protected final HashMap<OJClass,RelDataType> mapOJClassToType = new HashMap<OJClass, RelDataType>();

    private final OJClassMap ojClassMap;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates an <code>OJTypeFactoryImpl</code>.
     */
    public OJTypeFactoryImpl()
    {
        this(new OJClassMap(SyntheticObject.class));
    }

    protected OJTypeFactoryImpl(OJClassMap ojClassMap)
    {
        this.ojClassMap = ojClassMap;
    }

    //~ Methods ----------------------------------------------------------------

    // override RelDataTypeFactoryImpl
    public RelDataType createJavaType(Class clazz)
    {
        return toType(OJClass.forClass(clazz));
    }

    // override RelDataTypeFactoryImpl
    public RelDataType createArrayType(
        RelDataType elementType,
        long maxCardinality)
    {
        OJClass ojClass = OJUtil.typeToOJClass(elementType, this);
        return new OJScalarType(OJClass.arrayOf(ojClass));
    }

    protected OJClass createOJClassForRecordType(
        OJClass declarer,
        RelDataType recordType)
    {
        // convert to synthetic project type
        final RelDataTypeField [] fields = recordType.getFields();
        final String [] fieldNames = new String[fields.length];
        final OJClass [] fieldClasses = new OJClass[fields.length];
        for (int i = 0; i < fields.length; i++) {
            RelDataTypeField field = fields[i];
            fieldNames[i] = Util.toJavaId(
                    field.getName(),
                    i);
            final RelDataType fieldType = field.getType();
            fieldClasses[i] = OJUtil.typeToOJClass(declarer, fieldType, this);
        }
        return ojClassMap.createProject(declarer, fieldClasses,
                fieldNames);
    }

    public OJClass toOJClass(
        OJClass declarer,
        RelDataType type)
    {
        if (type instanceof OJScalarType) {
            return ((OJScalarType) type).ojClass;
        } else if (type instanceof JavaType) {
            JavaType scalarType = (JavaType) type;
            return OJClass.forClass(scalarType.getJavaClass());
        } else if (type instanceof RelRecordType) {
            RelRecordType recordType = (RelRecordType) type;
            OJClass projectClass =
                createOJClassForRecordType(declarer, recordType);

            // store reverse mapping, so we will be able to convert
            // "projectClass" back to "type"
            mapOJClassToType.put(projectClass, type);
            return projectClass;
        } else if (type instanceof RelCrossType) {
            // convert to synthetic join type
            RelCrossType crossType = (RelCrossType) type;
            final RelDataType [] types = crossType.types;
            final OJClass [] ojClasses = new OJClass[types.length];
            for (int i = 0; i < types.length; i++) {
                ojClasses[i] = OJUtil.typeToOJClass(declarer, types[i], this);
            }
            final OJClass joinClass =
                ojClassMap.createJoin(declarer, ojClasses);

            // store reverse mapping, so we will be able to convert
            // "joinClass" back to "type"
            mapOJClassToType.put(joinClass, type);
            return joinClass;
        } else {
            throw Util.newInternal("Not an OJ type: " + type);
        }
    }

    public RelDataType toType(final OJClass ojClass)
    {
        RelDataType type = mapOJClassToType.get(ojClass);
        if (type != null) {
            return type;
        }
        Class clazz;
        try {
            clazz = ojClass.getByteCode();
        } catch (CannotExecuteException e) {
            clazz = null;
        }
        if (clazz != null) {
            type = super.createJavaType(clazz);
        } else {
            type = canonize(new OJScalarType(ojClass));
        }
        mapOJClassToType.put(ojClass, type);
        return type;
    }

    //~ Inner Classes ----------------------------------------------------------

    /**
     * Type based upon an {@link OJClass}.
     *
     * <p>Use this class only if the class is a 'pure' OJClass:
     *
     * <ul>
     * <li>If the {@link OJClass} is based upon a Java class, call {@link
     * #createJavaType} instead.</li>
     * <li>If the {@link OJClass} is synthetic, call
     * {@link OJTypeFactoryImpl#createStructType}
     * or {@link OJTypeFactoryImpl#createJoinType} instead.</li>
     * </ul>
     * </p>
     */
    private class OJScalarType
        extends RelDataTypeImpl
    {
        private final OJClass ojClass;

        /**
         * Creates an <code>OJScalarType</code>
         *
         * @param ojClass Equivalent {@link OJClass}
         *
         * @pre ojClass != null
         * @pre !OJSyntheticClass.isJoinClass(ojClass)
         * @pre !OJSyntheticClass.isProjectClass(ojClass)
         */
        OJScalarType(OJClass ojClass)
        {
            super(null);
            assert (ojClass != null);
            assert (!OJSyntheticClass.isJoinClass(ojClass));

            // REVIEW jvs 23-Sept-2004:  find out who commented this out and
            // why

            //assert(!OJSyntheticClass.isProjectClass(ojClass));
            this.ojClass = ojClass;
            computeDigest();
        }

        public RelDataType getComponentType()
        {
            OJClass colType = OJUtil.guessRowType(ojClass);
            if (colType == null) {
                return null;
            }
            return toType(colType);
        }

        protected void generateTypeString(StringBuilder sb, boolean withDetail)
        {
            sb.append("OJScalarType(");
            sb.append(ojClass);
            sb.append(")");
        }
    }
}

// End OJTypeFactoryImpl.java
