/*
// $Id$
// Package org.eigenbase is a class library of database components.
// Copyright (C) 2002-2004 Disruptive Tech
// Copyright (C) 2003-2004 John V. Sichi
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

package org.eigenbase.util;

import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Stack;


/**
 * Walks over a tree, returning nodes in prefix order.  Objects which are an
 * instance of <code>Walkable</code> supply their children using
 * <code>getChildren()</code>; other objects are assumed to have no children.
 * Do not modify the tree during the enumeration. Example use:<code>Tree t;
 * Walker w = new Walker(t); while (w.hasMoreElements()) { Tree node = (Tree)
 * w.nextNode(); System.out.println(node.toString()); }</code>
 */
public class Walker implements Enumeration
{
    //~ Instance fields -------------------------------------------------------

    Frame currentFrame;
    Object nextNode;

    // The active parts of the tree from the root to nextNode are held in a
    // stack.  When the stack is empty, the enumeration finishes.  currentFrame
    // holds the frame of the 'current node' (the node last returned from
    // nextElement()) because it may no longer be on the stack.
    Stack stack;

    //~ Constructors ----------------------------------------------------------

    public Walker(Walkable root)
    {
        stack = new Stack();
        currentFrame = null;
        visit(null, root);
    }

    //~ Methods ---------------------------------------------------------------

    public final Object getAncestor(int iDepth)
    {
        Frame f = getAncestorFrame(iDepth);
        return (f == null) ? null : f.node;
    }

    public final Object getParent()
    {
        return getAncestor(1);
    }

    /**
     * get the ordinal within its parent node of the <code>iDepth</code>th
     * ancestor.
     */
    public int getAncestorOrdinal(int iDepth)
    {
        Frame f = getAncestorFrame(iDepth);
        return (f == null) ? (-1)
        : ((f.parent == null) ? 0 : arrayFind(f.parent.children, f.node));
    }

    /**
     * Override this function to prune the tree, or to allow objects which are
     * not Walkable to have children.
     */
    public Object [] getChildren(Object node)
    {
        if (node instanceof Walkable) {
            return ((Walkable) node).getChildren();
        } else {
            return null;
        }
    }

    /**
     * get the ordinal within its parent node of the current node.  Returns 0
     * for the root element.  Equivalent to getAncestorOrdinal(0).
     */
    public int getOrdinal()
    {
        // We can't use currentFrame.parent.iChild because moveToNext() may
        // have changed it.
        return (currentFrame.parent == null) ? 0
        : arrayFind(currentFrame.parent.children, currentFrame.node);
    }

    /**
     * returns the current object.  Not valid until nextElement() has been
     * called.
     */
    public Object currentElement()
    {
        return currentFrame.node;
    }

    public boolean hasMoreElements()
    {
        return nextNode != null;
    }

    /**
     * returns level in the tree of the current element (that is, last element
     * returned from nextElement()).  The level of the root element is 0.
     */
    public int level()
    {
        int i = 0;
        for (Frame f = currentFrame; f != null; f = f.parent) {
            i++;
        }
        return i;
    }

    public static void main(String [] args)
    {
        PrintWriter pw = new PrintWriter(System.out);
        Region usa =
            new Region("USA",
                new Region [] {
                    new Region(
                        "CA",
                        new Region [] {
                            new Region(
                                "San Francisco",
                                new Region [] {
                                    new Region(
                                        "WesternAddition",
                                        new Region [] {
                                            new Region("Haight", null)
                                        }), new Region("Soma", null)
                                }), new Region("Los Angeles", null)
                        }),
                    new Region(
                        "WA",
                        new Region [] {
                            new Region("Seattle", null),
                            new Region("Tacoma", null)
                        })
                });

        Walker walker = new Walker(usa);
        if (false) {
            while (walker.hasMoreElements()) {
                Region region = (Region) walker.nextElement();
                pw.println(region.name);
                pw.flush();
            }
        }

        Region.walkUntil(walker, "CA");
        walker.prune();
        Region region = (Region) walker.nextElement(); // should be WA
        pw.println(region.name);
        pw.flush();

        walker = new Walker(usa);
        Region.walkUntil(walker, "USA");
        walker.prune();
        region = (Region) walker.nextElement(); // should be null
        if (region == null) {
            pw.println("null");
        }
        pw.flush();

        walker = new Walker(usa);
        Region.walkUntil(walker, "Los Angeles");
        walker.prune();
        region = (Region) walker.nextElement(); // should be WA
        pw.println(region.name);
        pw.flush();

        walker = new Walker(usa);
        Region.walkUntil(walker, "Haight");
        walker.prune();
        region = (Region) walker.nextElement(); // should be Soma
        pw.println(region.name);
        pw.flush();

        walker = new Walker(usa);
        Region.walkUntil(walker, "Soma");
        walker.prune();
        region = (Region) walker.nextElement(); // should be Los Angeles
        pw.println(region.name);
        pw.flush();

        walker = new Walker(usa);
        Region.walkUntil(walker, "CA");
        walker.pruneSiblings();
        region = (Region) walker.nextElement(); // should be Los Angeles
        if (region == null) {
            pw.println("null");
            pw.flush();
        }

        walker = new Walker(usa);
        Region.walkUntil(walker, "Soma");
        walker.pruneSiblings();
        region = (Region) walker.nextElement(); // should be Los Angeles
        if (region == null) {
            pw.println("null");
            pw.flush();
        }
    }

    public Object nextElement()
    {
        moveToNext();
        return currentFrame.node;
    }

    /**
     * Tell walker that we don't want to visit any (more) children of this
     * node.  The next node visited will be (a return visit to) the node's
     * parent.  Not valid until nextElement() has been called.
     */
    public void prune()
    {
        if (currentFrame.children != null) {
            currentFrame.iChild = currentFrame.children.length;
        }

        //we need to make that next frame on the stack is not a child
        //of frame we just pruned. if it is, we need to prune it too
        if (this.hasMoreElements()) {
            Object nextFrameParentNode = ((Frame) stack.peek()).parent.node;
            if (nextFrameParentNode != currentFrame.node) {
                return;
            }

            //delete the child of current member from the stack
            stack.pop();
            if (currentFrame.parent != null) {
                currentFrame = currentFrame.parent;
            }
            nextElement();
        }
    }

    public void pruneSiblings()
    {
        prune();
        currentFrame = currentFrame.parent;
        if (currentFrame != null) {
            prune();
        }
    }

    /**
     * returns the <code>iDepth</code>th ancestor of the current element
     */
    private Frame getAncestorFrame(int iDepth)
    {
        for (Frame f = currentFrame; f != null; f = f.parent) {
            if (iDepth-- == 0) {
                return f;
            }
        }
        return null;
    }

    private static int arrayFind(
        Object [] array,
        Object o)
    {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == o) {
                return i;
            }
        }
        return -1;
    }

    private void moveToNext()
    {
        if (stack.empty()) {
            return;
        }

        currentFrame = (Frame) stack.peek();

        // Unwind stack until we find a level we have not completed.
        do {
            Frame frame = (Frame) stack.peek();
            if ((frame.children != null)
                    && (++frame.iChild < frame.children.length)) {
                // Here is an unvisited child.  Visit it.
                visit(frame, frame.children[frame.iChild]);
                return;
            }
            stack.pop();
        } while (!stack.empty());
        nextNode = null;
    }

    private void visit(
        Frame parent,
        Object node)
    {
        nextNode = node;
        stack.addElement(new Frame(parent, node));
    }

    //~ Inner Classes ---------------------------------------------------------

    class Frame
    {
        Frame parent;
        Object node;
        Object [] children;
        int iChild;

        Frame(
            Frame parent,
            Object node)
        {
            this.parent = parent;
            this.node = node;
            this.children = getChildren(node);
            this.iChild = -1; // haven't visited first child yet
        }
    }

    private static class Region implements Walkable
    {
        String name;
        Region [] children;

        Region(
            String name,
            Region [] children)
        {
            this.name = name;
            this.children = children;
        }

        public Object [] getChildren()
        {
            return children;
        }

        public static void walkUntil(
            Walker walker,
            String name)
        {
            while (walker.hasMoreElements()) {
                Region region = (Region) walker.nextElement();
                if (region.name.equals(name)) {
                    break;
                }
            }
        }
    }
}


// End Walker.java
