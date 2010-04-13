/*
// $Id$
// Package org.eigenbase is a class library of data management components.
// Copyright (C) 2005 The Eigenbase Project
// Copyright (C) 2002 SQLstream, Inc.
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
package org.eigenbase.util;

/**
 * A collection of terms.
 *
 * <p>(This is not a real class. It is here so that terms which do not map to
 * classes can be referenced in Javadoc.)</p>
 *
 * @author jhyde
 * @version $Id$
 * @since Nov 28, 2003
 */
public class Glossary
{
    //~ Static fields/initializers ---------------------------------------------

    /**
     * <p>This table shows how and where the Gang of Four patterns are applied.
     * The table uses information from the GoF book and from a course on
     * advanced object design taught by Craig Larman.</p>
     *
     * <p>The patterns are in three groups depicting frequency of use. The *
     * patterns in <b> <font color="lime">light green</font></b> are used <i>
     * frequently</i>. Those in <b><font color="#ffff00">yellow</font></b> have
     * <i>moderate</i> use. Patterns in <b><font color="red">red</font></b> are
     * <i>infrequently</i> used. The GoF column gives the original Gang Of Four
     * category for the pattern. The Problem and Pattern columns are from
     * Craig's refinement of the type of problems they apply to and a refinement
     * of the original three pattern categories.</p>
     *
     * <table cellSpacing="0" cols="6" cellPadding="3" border="1">
     * <caption align="bottom"><a
     * href="http://www.onr.com/user/loeffler/java/references.html#gof"><b>Gang
     * of Four Patterns</b></a></caption>
     * <tr>
     * <!-- Headers for each column -->
     *
     * <th>Pattern Name</th>
     * <th align="middle"><a href="#category">GOF Category</a></th>
     * <th align="middle">Problem</th>
     * <th align="middle">Pattern</th>
     * <th align="middle">Often Uses</th>
     * <th align="middle">Related To</th>
     * </tr>
     * <!-- Frequently used patterns have a lime background -->
     * <tr>
     * <td bgColor="lime"><a href="#AbstractFactoryPattern">Abstract Factory</a>
     * </td>
     * <td bgColor="teal"><font color="white">Creational</font></td>
     * <td>Creating Instances</td>
     * <td>Class/Interface Definition plus Inheritance</td>
     * <td><a href="#FactoryMethodPattern">Factory Method</a><br>
     * <a href="#PrototypePattern">Prototype</a><br>
     * <a href="#SingletonPattern">Singleton</a> with <a href="#FacadePattern">
     * Facade</a></td>
     * <td><a href="#FactoryMethodPattern">Factory Method</a><br>
     * <a href="#PrototypePattern">Prototype</a><br>
     * <a href="#SingletonPattern">Singleton</a></td>
     * </tr>
     * <tr>
     * <td bgColor="lime"><a href="#ObjectAdapterPattern">Object Adapter</a>
     * </td>
     * <td bgColor="silver">Structural</td>
     * <td>Interface</td>
     * <td>Wrap One</td>
     * <td align="middle">-</td>
     * <td><a href="#BridgePattern">Bridge</a><br>
     * <a href="#DecoratorPattern">Decorator</a><br>
     * <a href="#ProxyPattern">Proxy</a></td>
     * </tr>
     * <tr>
     * <td bgColor="lime"><a href="#CommandPattern">Command</a></td>
     * <td bgColor="maroon"><font color="white">Behavioral</font></td>
     * <td>Organization or Communication of Work<br>
     * Action/Response</td>
     * <td>Behavior Objects</td>
     * <td><a href="#CompositePattern">Composite</a></td>
     * <td><a href="#CompositePattern">Composite</a><br>
     * <a href="#MementoPattern">Memento</a><br>
     * <a href="#PrototypePattern">Prototype</a></td>
     * </tr>
     * <tr>
     * <td bgColor="lime"><a href="#CompositePattern">Composite</a></td>
     * <td bgColor="silver">Structural</td>
     * <td>Structural Decomposition of Objects or Subsystems</td>
     * <td>Wrap Many</td>
     * <td align="middle">-</td>
     * <td><a href="#DecoratorPattern">Decorator</a><br>
     * <a href="#IteratorPattern">Iterator</a><br>
     * <a href="#VisitorPattern">Visitor</a></td>
     * </tr>
     * <tr>
     * <td bgColor="lime"><a href="#DecoratorPattern">Decorator</a></td>
     * <td bgColor="silver">Structural</td>
     * <td>Instance Behavior</td>
     * <td>Wrap One</td>
     * <td align="middle">-</td>
     * <td><a href="#ObjectAdapterPattern">Object Adapter</a><br>
     * <a href="#CompositePattern">Composite</a><br>
     * <a href="#StrategyPattern">Strategy</a></td>
     * </tr>
     * <tr>
     * <td bgColor="lime"><a href="#FacadePattern">Facade</a></td>
     * <td bgColor="silver">Structural</td>
     * <td>Access Control<br>
     * &*nbsp;
     * <hr>
     * <p>Structural Decomposition of Objects or Subsystems</td>
     * <td>Wrap Many</td>
     * <td><a href="#SingletonPattern">Singleton</a> with <a
     * href="#AbstractFactoryPattern">Abstract Factory</a></td>
     * <td><a href="#AbstractFactoryPattern">Abstract Factory</a><br>
     * <a href="#MediatorPattern">Mediator</a></td>
     * </tr>
     * <tr>
     * <td bgColor="lime"><a href="#FlyweightPattern">Flyweight</a></td>
     * <td bgColor="silver">Structural</td>
     * <td>Shared Resource Handling</td>
     * <td>Object State or Values</td>
     * <td align="middle">-</td>
     * <td><a href="#SingletonPattern">Singleton</a><br>
     * <a href="#StatePattern">State</a><br>
     * <a href="#StrategyPattern">Strategy</a><br>
     * Shareable</td>
     * </tr>
     * <tr>
     * <td bgColor="lime"><a href="#IteratorPattern">Iterator</a></td>
     * <td bgColor="maroon"><font color="white">Behavioral</font></td>
     * <td>Traversal Algorithm<br>
     * &*nbsp;
     * <hr>
     * <p>Access Control</td>
     * <td>Low Coupling</td>
     * <td align="middle">-</td>
     * <td><a href="#CompositePattern">Composite</a><br>
     * <a href="#FactoryMethodPattern">Factory Method</a><br>
     * <a href="#MementoPattern">Memento</a></td>
     * </tr>
     * <tr>
     * <td bgColor="lime"><a href="#ObserverPattern">Observer</a></td>
     * <td bgColor="maroon"><font color="white">Behavioral</font></td>
     * <td>Event Response<br>
     * &*nbsp;
     * <hr>
     * <p>Organization or Communication of Work</td>
     * <td>Low Coupling</td>
     * <td align="middle">-</td>
     * <td><a href="#MediatorPattern">Mediator</a><br>
     * <a href="#SingletonPattern">Singleton</a></td>
     * </tr>
     * <tr>
     * <td bgColor="lime"><a href="#ProxyPattern">Proxy</a></td>
     * <td bgColor="silver">Structural</td>
     * <td>Access Control</td>
     * <td>Wrap One</td>
     * <td align="middle">-</td>
     * <td><a href="#ObjectAdapterPattern">Adapter</a><br>
     * <a href="#DecoratorPattern">Decorator</a></td>
     * </tr>
     * <tr>
     * <td bgColor="lime"><a href="#SingletonPattern">Singleton</a></td>
     * <td bgColor="teal"><font color="white">Creational</font></td>
     * <td>Access Control</td>
     * <td>Other</td>
     * <td align="middle">-</td>
     * <td><a href="#AbstractFactoryPattern">Abstract Factory</a><br>
     * <a href="#BuilderPattern">Builder</a><br>
     * <a href="#PrototypePattern">Prototype</a></td>
     * </tr>
     * <tr>
     * <td bgColor="lime"><a href="#StatePattern">State</a></td>
     * <td bgColor="maroon"><font color="white">Behavioral</font></td>
     * <td>Instance Behavior</td>
     * <td>Object State or Values</td>
     * <td><a href="#FlyweightPattern">Flyweight</a></td>
     * <td><a href="#FlyweightPattern">Flyweight</a><br>
     * <a href="#SingletonPattern">Singleton</a></td>
     * </tr>
     * <tr>
     * <td bgColor="lime"><a href="#StrategyPattern">Strategy</a></td>
     * <td bgColor="maroon"><font color="white">Behavioral</font></td>
     * <td>Single Algorithm</td>
     * <td>Behavior Objects</td>
     * <td align="middle">-</td>
     * <td><a href="#FlyweightPattern">Flyweight</a><br>
     * <a href="#StatePattern">State</a><br>
     * <a href="#TemplateMethodPattern">Template Method</a></td>
     * </tr>
     * <tr>
     * <td bgColor="lime"><a href="#TemplateMethodPattern">Template Method</a>
     * </td>
     * <td bgColor="maroon"><font color="white">Behavioral</font></td>
     * <td>Single Algorithm</td>
     * <td>Class or Interface Definition plus Inheritance</td>
     * <td align="middle">-</td>
     * <td><a href="#StrategyPattern">Strategy</a></td>
     * </tr>
     * <!-- Moderately use patterns have a yellow background -->
     * <tr>
     * <td bgColor="yellow"><a href="#ClassAdapterPattern">Class Adapter</a>
     * </td>
     * <td bgColor="silver">Structural</td>
     * <td>Interface</td>
     * <td>Class or Interface Definition plus Inheritance</td>
     * <td align="middle">-</td>
     * <td><a href="#BridgePattern">Bridge</a><br>
     * <a href="#DecoratorPattern">Decorator</a><br>
     * <a href="#ProxyPattern">Proxy</a></td>
     * </tr>
     * <tr>
     * <td bgColor="yellow"><a href="#BridgePattern">Bridge</a></td>
     * <td bgColor="silver">Structural</td>
     * <td>Implementation</td>
     * <td>Wrap One</td>
     * <td align="middle">-</td>
     * <td><a href="#AbstractFactoryPattern">Abstract Factory</a><br>
     * <a href="#ClassAdapterPattern">Class Adaptor</a></td>
     * </tr>
     * <tr>
     * <td bgColor="yellow"><a href="#BuilderPattern">Builder</a></td>
     * <td bgColor="teal"><font color="white">Creational</font></td>
     * <td>Creating Structures</td>
     * <td>Class or Interface Definition plus Inheritance</td>
     * <td align="middle">-</td>
     * <td><a href="#AbstractFactoryPattern">Abstract Factory</a><br>
     * <a href="#CompositePattern">Composite</a></td>
     * </tr>
     * <tr>
     * <td bgColor="yellow"><a href="#ChainOfResponsibilityPattern">Chain of
     * Responsibility</a></td>
     * <td bgColor="maroon"><font color="white">Behavioral</font></td>
     * <td>Single Algorithm<br>
     * &*nbsp;
     * <hr>
     * <p>Organization or Communication of Work</td>
     * <td>Low Coupling</td>
     * <td align="middle">-</td>
     * <td><a href="#CompositePattern">Composite</a></td>
     * </tr>
     * <tr>
     * <td bgColor="yellow"><a href="#FactoryMethodPattern">Factory Method</a>
     * </td>
     * <td bgColor="teal"><font color="white">Creational</font></td>
     * <td>Creating Instances</td>
     * <td>Class or Interface Definition plus Inheritance</td>
     * <td><a href="#TemplateMethodPattern">Template Method</a></td>
     * <td><a href="#AbstractFactoryPattern">Abstract Factory</a><br>
     * <a href="#TemplateMethodPattern">Template Method</a><br>
     * <a href="#PrototypePattern">Prototype</a></td>
     * </tr>
     * <tr>
     * <td bgColor="yellow"><a href="#MediatorPattern">Mediator</a></td>
     * <td bgColor="maroon"><font color="white">Behavioral</font></td>
     * <td>Interaction between Objects<br>
     * &*nbsp;
     * <hr>
     * <p>Organization or Communication of Work</td>
     * <td>Low Coupling</td>
     * <td align="middle">-</td>
     * <td><a href="#FacadePattern">Facade</a><br>
     * <a href="#ObserverPattern">Observer</a></td>
     * </tr>
     * <tr>
     * <td bgColor="yellow"><a href="#PrototypePattern">Prototype</a></td>
     * <td bgColor="teal"><font color="white">Creational</font></td>
     * <td>Creating Instances</td>
     * <td>Other</td>
     * <td align="middle">-</td>
     * <td><a href="#PrototypePattern">Prototype</a><br>
     * <a href="#CompositePattern">Composite</a><br>
     * <a href="#DecoratorPattern">Decorator</a></td>
     * </tr>
     * <tr>
     * <td bgColor="yellow"><a href="#VisitorPattern">Visitor</a></td>
     * <td bgColor="maroon"><font color="white">Behavioral</font></td>
     * <td>Single Algorithm</td>
     * <td>Behavior Objects</td>
     * <td align="middle">-</td>
     * <td><a href="#CompositePattern">Composite</a><br>
     * <a href="#VisitorPattern">Visitor</a></td>
     * </tr>
     * <!-- Seldom used patterns have a red background -->
     * <tr>
     * <td bgColor="red"><a href="#InterpreterPattern"><font color="white">
     * Interpreter</font></a></td>
     * <td bgColor="maroon"><font color="white">Behavioral</font></td>
     * <td>Organization or Communication of Work</td>
     * <td>Other</td>
     * <td align="middle">-</td>
     * <td><a href="#CompositePattern">Composite</a><br>
     * <a href="#FlyweightPattern">Flyweight</a><br>
     * <a href="#IteratorPattern">Iterator</a><br>
     * <a href="#VisitorPattern">Visitor</a></td>
     * </tr>
     * <tr>
     * <td bgColor="red"><a href="#MementoPattern"><font color="white">
     * Memento</font></a></td>
     * <td bgColor="maroon"><font color="white">Behavioral</font></td>
     * <td>Instance Management</td>
     * <td>Object State or Values</td>
     * <td align="middle">-</td>
     * <td><a href="#CommandPattern">Command</a><br>
     * <a href="#IteratorPattern">Iterator</a></td>
     * </tr>
     * </table>
     */
    public static final Glossary Pattern = null;

    /**
     * Provide an interface for creating families of related or dependent
     * objects without specifying their concrete classes. (See <a
     * href="http://c2.com/cgi/wiki?AbstractFactoryPattern">GoF</a>.)
     */
    public static final Glossary AbstractFactoryPattern = null;

    /**
     * Separate the construction of a complex object from its representation so
     * that the same construction process can create different representations.
     * (See <a href="http://c2.com/cgi/wiki?BuilderPattern">GoF</a>.)
     */
    public static final Glossary BuilderPattern = null;

    /**
     * Define an interface for creating an object, but let subclasses decide
     * which class to instantiate. Lets a class defer instantiation to
     * subclasses. (See <a href="http://c2.com/cgi/wiki?FactoryMethodPattern">
     * GoF</a>.)
     */
    public static final Glossary FactoryMethodPattern = null;

    /**
     * Specify the kinds of objects to create using a prototypical instance, and
     * create new objects by copying this prototype. (See <a
     * href="http://c2.com/cgi/wiki?PrototypePattern">GoF</a>.)
     */
    public static final Glossary PrototypePattern = null;

    /**
     * Ensure a class only has one instance, and provide a global point of
     * access to it. (See <a href="http://c2.com/cgi/wiki?SingletonPattern">
     * GoF</a>.)
     *
     * <p>Note that a common way of implementing a singleton, the so-called <a
     * href="http://www.cs.umd.edu/~pugh/java/memoryModel/DoubleCheckedLocking.html">
     * double-checked locking pattern</a>, is fatally flawed in Java. Don't use
     * it!</p>
     */
    public static final Glossary SingletonPattern = null;

    /**
     * Convert the interface of a class into another interface clients expect.
     * Lets classes work together that couldn't otherwise because of
     * incompatible interfaces. (See <a
     * href="http://c2.com/cgi/wiki?AdapterPattern">GoF</a>.)
     */
    public static final Glossary AdapterPattern = null;

    /**
     * Decouple an abstraction from its implementation so that the two can very
     * independently. (See <a href="http://c2.com/cgi/wiki?BridgePattern">
     * GoF</a>.)
     */
    public static final Glossary BridgePattern = null;

    /**
     * Compose objects into tree structures to represent part-whole hierarchies.
     * Lets clients treat individual objects and compositions of objects
     * uniformly. (See <a href="http://c2.com/cgi/wiki?CompositePattern">
     * GoF</a>.)
     */
    public static final Glossary CompositePattern = null;

    /**
     * Attach additional responsibilities to an object dynamically. Provides a
     * flexible alternative to subclassing for extending functionality. (See <a
     * href="http://c2.com/cgi/wiki?DecoratorPattern">GoF</a>.)
     */
    public static final Glossary DecoratorPattern = null;

    /**
     * Provide a unified interface to a set of interfaces in a subsystem.
     * Defines a higher-level interface that makes the subsystem easier to use.
     * (See <a href="http://c2.com/cgi/wiki?FacadePattern">GoF</a>.)
     */
    public static final Glossary FacadePattern = null;

    /**
     * Use sharing to support large numbers of fine-grained objects efficiently.
     * (See <a href="http://c2.com/cgi/wiki?FlyweightPattern">GoF</a>.)
     */
    public static final Glossary FlyweightPattern = null;

    /**
     * Provide a surrogate or placeholder for another object to control access
     * to it. (See <a href="http://c2.com/cgi/wiki?ProxyPattern">GoF</a>.)
     */
    public static final Glossary ProxyPattern = null;

    /**
     * Avoid coupling the sender of a request to its receiver by giving more
     * than one object a chance to handle the request. Chain the receiving
     * objects and pass the request along the chain until an object handles it.
     * (See <a href="http://c2.com/cgi/wiki?ChainOfResponsibilityPattern">
     * GoF</a>.)
     */
    public static final Glossary ChainOfResponsibilityPattern = null;

    /**
     * Encapsulate a request as an object, thereby letting you parameterize
     * clients with different requests, queue or log requests, and support
     * undoable operations. (See <a
     * href="http://c2.com/cgi/wiki?CommandPattern">GoF</a>.)
     */
    public static final Glossary CommandPattern = null;

    /**
     * Given a language, define a representation for its grammar along with an
     * interpreter that uses the representation to interpret sentences in the
     * language. (See <a href="http://c2.com/cgi/wiki?InterpreterPattern">
     * GoF</a>.)
     */
    public static final Glossary InterpreterPattern = null;

    /**
     * Provide a way to access the elements of an aggregate object sequentially
     * without exposing its underlying representation. (See <a
     * href="http://c2.com/cgi/wiki?IteratorPattern">GoF</a>.)
     */
    public static final Glossary IteratorPattern = null;

    /**
     * Define an object that encapsulates how a set of objects interact.
     * Promotes loose coupling by keeping objects from referring to each other
     * explicitly, and it lets you vary their interaction independently. (See <a
     * href="http://c2.com/cgi/wiki?MediatorPattern">GoF</a>.)
     */
    public static final Glossary MediatorPattern = null;

    /**
     * Without violating encapsulation, capture and externalize an objects's
     * internal state so that the object can be restored to this state later.
     * (See <a href="http://c2.com/cgi/wiki?MementoPattern">GoF</a>.)
     */
    public static final Glossary MementoPattern = null;

    /**
     * Define a one-to-many dependency between objects so that when one object
     * changes state, all its dependents are notified and updated automatically.
     * (See <a href="http://c2.com/cgi/wiki?ObserverPattern">GoF</a>.)
     */
    public static final Glossary ObserverPattern = null;

    /**
     * Allow an object to alter its behavior when its internal state changes.
     * The object will appear to change its class. (See <a
     * href="http://c2.com/cgi/wiki?StatePattern">GoF</a>.)
     */
    public static final Glossary StatePattern = null;

    /**
     * Define a family of algorithms, encapsulate each one, and make them
     * interchangeable. Lets the algorithm vary independently from clients that
     * use it. (See <a href="http://c2.com/cgi/wiki?StrategyPattern">GoF</a>.)
     */
    public static final Glossary StrategyPattern = null;

    /**
     * Define the skeleton of an algorithm in an operation, deferring some steps
     * to subclasses. Lets subclasses redefine certain steps of an algorithm
     * without changing the algorithm's structure. (See <a
     * href="http://c2.com/cgi/wiki?TemplateMethodPattern">GoF</a>.)
     */
    public static final Glossary TemplateMethodPattern = null;

    /**
     * Represent an operation to be performed on the elments of an object
     * structure. Lets you define a new operation without changing the classes
     * of the elements on which it operates. (See <a
     * href="http://c2.com/cgi/wiki?VisitorPattern">GoF</a>.)
     */
    public static final Glossary VisitorPattern = null;

    /**
     * The official SQL-92 standard (ISO/IEC 9075:1992). To reference this
     * standard from methods that implement its rules, use the &#64;sql.92
     * custom block tag in Javadoc comments; for the tag body, use the format
     * <code>&lt;SectionId&gt; [ ItemType &lt;ItemId&gt; ]</code>, where
     *
     * <ul>
     * <li><code>SectionId</code> is the numbered or named section in the table
     * of contents, e.g. "Section 4.18.9" or "Annex A"
     * <li><code>ItemType</code> is one of { Table, Syntax Rule, Access Rule,
     * General Rule, or Leveling Rule }
     * <li><code>ItemId</code> is a dotted path expression to the specific item
     * </ul>
     *
     * For example,
     *
     * <pre><code>&#64;sql.92 Section 11.4 Syntax Rule 7.c
     *</code></pre>
     *
     * is a well-formed reference to the rule for the default character set to
     * use for column definitions of character type.
     *
     * <p>Note that this tag is a block tag (like &#64;see) and cannot be used
     * inline.
     */
    public static final Glossary Sql92 = null;

    /**
     * The official SQL:1999 standard (ISO/IEC 9075:1999), which is broken up
     * into five parts. To reference this standard from methods that implement
     * its rules, use the &#64;sql.99 custom block tag in Javadoc comments; for
     * the tag body, use the format <code>&lt;PartId&gt; &lt;SectionId&gt; [
     * ItemType &lt;ItemId&gt; ]</code>, where
     *
     * <ul>
     * <li><code>PartId</code> is the numbered part (up to Part 5)
     * <li><code>SectionId</code> is the numbered or named section in the part's
     * table of contents, e.g. "Section 4.18.9" or "Annex A"
     * <li><code>ItemType</code> is one of { Table, Syntax Rule, Access Rule,
     * General Rule, or Conformance Rule }
     * <li><code>ItemId</code> is a dotted path expression to the specific item
     * </ul>
     *
     * For example,
     *
     * <pre><code>&#64;sql.99 Part 2 Section 11.4 Syntax Rule 7.b
     *</code></pre>
     *
     * is a well-formed reference to the rule for the default character set to
     * use for column definitions of character type.
     *
     * <p>Note that this tag is a block tag (like &#64;see) and cannot be used
     * inline.
     */
    public static final Glossary Sql99 = null;

    /**
     * The official SQL:2003 standard (ISO/IEC 9075:2003), which is broken up
     * into numerous parts. To reference this standard from methods that
     * implement its rules, use the &#64;sql.2003 custom block tag in Javadoc
     * comments; for the tag body, use the format <code>&lt;PartId&gt;
     * &lt;SectionId&gt; [ ItemType &lt;ItemId&gt; ]</code>, where
     *
     * <ul>
     * <li><code>PartId</code> is the numbered part
     * <li><code>SectionId</code> is the numbered or named section in the part's
     * table of contents, e.g. "Section 4.11.2" or "Annex A"
     * <li><code>ItemType</code> is one of { Table, Syntax Rule, Access Rule,
     * General Rule, or Conformance Rule }
     * <li><code>ItemId</code> is a dotted path expression to the specific item
     * </ul>
     *
     * For example,
     *
     * <pre><code>&#64;sql.2003 Part 2 Section 11.4 Syntax Rule 10.b
     *</code></pre>
     *
     * is a well-formed reference to the rule for the default character set to
     * use for column definitions of character type.
     *
     * <p>Note that this tag is a block tag (like &#64;see) and cannot be used
     * inline.
     */
    public static final Glossary Sql2003 = null;
}

// End Glossary.java
