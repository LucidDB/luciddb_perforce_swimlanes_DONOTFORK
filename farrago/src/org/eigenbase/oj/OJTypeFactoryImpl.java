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

package org.eigenbase.oj;

import org.eigenbase.reltype.RelDataTypeField;
import org.eigenbase.reltype.RelDataType;
import org.eigenbase.reltype.RelDataTypeFactory;
import org.eigenbase.reltype.RelDataTypeFactoryImpl;
import org.eigenbase.oj.util.OJUtil;
import org.eigenbase.util.Util;
import openjava.mop.CannotExecuteException;
import openjava.mop.OJClass;
import openjava.ptree.util.ClassMap;
import openjava.ptree.util.SyntheticClass;

import java.util.HashMap;


/**
 * Implementation of {@link RelDataTypeFactory} based upon OpenJava's type
 * system.
 *
 * @author jhyde
 * @version $Id$
 *
 * @see openjava.mop.OJClass
 * @see RelDataTypeFactory
 * @since May 30, 2003
 */
public class OJTypeFactoryImpl extends RelDataTypeFactoryImpl
    implements OJTypeFactory
{
    //~ Instance fields -------------------------------------------------------

    private final HashMap mapOJClassToType = new HashMap();

    //~ Constructors ----------------------------------------------------------

    /**
     * Creates an <code>OJTypeFactoryImpl</code>.
     */
    public OJTypeFactoryImpl()
    {
    }

    //~ Methods ---------------------------------------------------------------

    // override RelDataTypeFactoryImpl
    public RelDataType createJavaType(Class clazz)
    {
        return toType(OJClass.forClass(clazz));
    }

    protected OJClass createOJClassForRecordType(
        OJClass declarer,RecordType recordType)
    {
        // convert to synthetic project type
        final RelDataTypeField [] fields = recordType.getFields();
        final String [] fieldNames = new String[fields.length];
        final OJClass [] fieldClasses = new OJClass[fields.length];
        for (int i = 0; i < fields.length; i++) {
            RelDataTypeField field = fields[i];
            fieldNames[i] = Util.toJavaId(field.getName(),i);
            final RelDataType fieldType = field.getType();
            fieldClasses[i] = OJUtil.typeToOJClass(declarer,fieldType);
        }
        return ClassMap.instance().createProject(
            declarer,
            fieldClasses,
            fieldNames);
    }

    public OJClass toOJClass(OJClass declarer,RelDataType type)
    {
        if (type instanceof OJScalarType) {
            return ((OJScalarType) type).ojClass;
        } else if (type instanceof JavaType) {
            JavaType scalarType = (JavaType) type;
            return OJClass.forClass(scalarType.clazz);
        } else if (type instanceof RecordType) {
            RecordType recordType = (RecordType) type;
            OJClass projectClass = createOJClassForRecordType(
                declarer,recordType);
            // store reverse mapping, so we will be able to convert
            // "projectClass" back to "type"
            mapOJClassToType.put(projectClass,type);
            return projectClass;
        } else if (type instanceof CrossType) {
            // convert to synthetic join type
            CrossType crossType = (RelDataTypeFactoryImpl.CrossType) type;
            final RelDataType [] types = crossType.types;
            final OJClass [] ojClasses = new OJClass[types.length];
            for (int i = 0; i < types.length; i++) {
                ojClasses[i] = OJUtil.typeToOJClass(declarer,types[i]);
            }
            final OJClass joinClass =
                ClassMap.instance().createJoin(declarer,ojClasses);

            // store reverse mapping, so we will be able to convert
            // "joinClass" back to "type"
            mapOJClassToType.put(joinClass,type);
            return joinClass;
        } else {
            throw Util.newInternal("Not an OJ type: " + type);
        }
    }

    public RelDataType toType(final OJClass ojClass)
    {
        RelDataType type = (RelDataType) mapOJClassToType.get(ojClass);
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
        mapOJClassToType.put(ojClass,type);
        return type;
    }

    //~ Inner Classes ---------------------------------------------------------

    /**
     * Type based upon an {@link OJClass}.
     *
     * <p>
     * Use this class only if the class is a 'pure' OJClass:
     *
     * <ul>
     * <li>
     * If the {@link OJClass} is based upon a Java class, call
     * {@link #createJavaType} instead.
     * </li>
     * <li>
     * If the {@link OJClass} is synthetic, call {@link #createProjectType} or
     * {@link #createJoinType} instead.
     * </li>
     * </ul>
     * </p>
     */
    private class OJScalarType extends TypeImpl
    {
        public RelDataType getArrayType() {
            return new OJScalarType(OJClass.arrayOf(ojClass));
        }

        private final OJClass ojClass;

        /**
         * Creates an <code>OJScalarType</code>
         *
         * @param ojClass Equivalent {@link OJClass}
         *
         * @pre ojClass != null
         * @pre !SyntheticClass.isJoinClass(ojClass)
         * @pre !SyntheticClass.isProjectClass(ojClass)
         */
        OJScalarType(OJClass ojClass)
        {
            super(new RelDataTypeField[1]);
            assert(ojClass != null);
            assert(!SyntheticClass.isJoinClass(ojClass));
            //assert(!SyntheticClass.isProjectClass(ojClass));
            fields[0] = new FieldImpl("this", 0, this);
            this.ojClass = ojClass;
            this.digest = computeDigest();
        }

        public RelDataTypeField getField(String fieldName)
        {
            return null;
        }

        public int getFieldCount()
        {
            return 1;
        }

        public int getFieldOrdinal(String fieldName)
        {
            return 0;
        }

        public RelDataTypeField [] getFields()
        {
            return new RelDataTypeField[0];
        }

        public RelDataType getComponentType() {
            OJClass colType = Util.guessRowType(ojClass);
            if (colType == null) {
                return null;
            }
            return toType(colType);
        }

        protected String computeDigest()
        {
            return "OJScalarType(" + ojClass + ")";
        }
    }
}


// End OJTypeFactoryImpl.java
