/*
// Saffron preprocessor and data engine.
// Copyright (C) 2002-2004 Disruptive Tech
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
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

package sales;


/**
 * <code>SalesInMemory</code> holds the same data as {@link Sales}, and can be
 * used in code in an identical way, but the data is held in regular arrays.
 */
public class SalesInMemory
{
    public Dept [] depts =
        new Dept [] {
            new Dept(10, "Sales"), new Dept(20, "Marketing"),
            new Dept(30, "Accounts")
        };
    public Emp [] emps =
        new Emp [] {
            new Emp(100, "Fred", 10, "M", "San Francisco", true),
            new Emp(110, "Eric", 20, "M", "San Francisco", true),
            new Emp(110, "John", 40, "M", "Vancouver", true),
            new Emp(120, "Wilma", 20, "F", "Los Angeles", false)
        };
}


// End SalesInMemory.java
