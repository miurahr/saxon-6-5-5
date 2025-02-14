package com.icl.saxon.tree;
import com.icl.saxon.expr.NodeSetExtent;
import com.icl.saxon.expr.XPathException;
import com.icl.saxon.om.AxisEnumeration;
import com.icl.saxon.om.EmptyEnumeration;
import com.icl.saxon.om.NodeInfo;
import com.icl.saxon.om.SingletonEnumeration;
import com.icl.saxon.output.Outputter;
import com.icl.saxon.pattern.AnyNodeTest;
import com.icl.saxon.pattern.NodeTest;
import com.icl.saxon.sort.LocalOrderComparer;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.transform.TransformerException;

/**
  * ParentNodeImpl is an implementation of a non-leaf node (specifically, an Element node
  * or a Document node)
  * @author Michael H. Kay
  */


abstract class ParentNodeImpl extends NodeImpl {

    private Object children = null;     // null for no children
                                        // a NodeInfo for a single child
                                        // a NodeInfo[] for >1 child

    protected int sequence;

    /**
    * Get the node sequence number (in document order). Sequence numbers are monotonic but not
    * consecutive. In the current implementation, parent nodes (elements and roots) have a zero
    * least-significant word, while namespaces, attributes, text nodes, comments, and PIs have
    * the top word the same as their owner and the bottom half reflecting their relative position.
    */

    protected final long getSequenceNumber() {
        return ((long)sequence)<<32;
    }

    /**
    * Determine if the node has any children.
    */

    public final boolean hasChildNodes() {
        return (children!=null);
    }

    /**
    * Get an enumeration of the children of this node
    */

    public final AxisEnumeration enumerateChildren(NodeTest test) {
        if (children==null) {
            return EmptyEnumeration.getInstance();
        } else if (children instanceof NodeImpl) {
            NodeImpl child = (NodeImpl)children;
            if (test.matches(child)) {
                return new SingletonEnumeration(child);
            } else {
                return EmptyEnumeration.getInstance();
            }
        } else {
            if (test instanceof AnyNodeTest) {
                return new ArrayEnumeration((NodeImpl[])children);
            } else {
                return new ChildEnumeration(this, test);
            }
        }
    }


    /**
    * Get the first child node of the element
    * @return the first child node of the required type, or null if there are no children
    */

    public final Node getFirstChild() {
        if (children==null) return null;
        if (children instanceof NodeImpl) return (NodeImpl)children;
        return ((NodeImpl[])children)[0];
    }

    /**
    * Get the last child node of the element
    * @return the last child of the element, or null if there are no children
    */

    public final Node getLastChild() {
        if (children==null) return null;
        if (children instanceof NodeImpl) return (NodeImpl)children;
        NodeImpl[] n = (NodeImpl[])children;
        return n[n.length-1];
    }

    /**
     * Return a <code>NodeList</code> that contains all children of this node. If
     * there are no children, this is a <code>NodeList</code> containing no
     * nodes.
     */

    public final NodeList getChildNodes() {
        if (hasChildNodes()) {
            try {
                return new NodeSetExtent(
                                enumerateChildren(AnyNodeTest.getInstance()),
                                LocalOrderComparer.getInstance());
            } catch (XPathException err) {
                return super.getChildNodes();
            }
        } else {
            return super.getChildNodes();
        }
    }

    /**
    * Get the nth child node of the element (numbering from 0)
    * @return the last child of the element, or null if there is no n'th child
    */

    protected final NodeImpl getNthChild(int n) {
        if (children==null) return null;
        if (children instanceof NodeImpl) {
            return (n==0 ? (NodeImpl)children : null);
        }
        NodeImpl[] nodes = (NodeImpl[])children;
        if (n<0 || n>=nodes.length) return null;
        return nodes[n];
    }


    /**
    * Return the string-value of the node, that is, the concatenation
    * of the character content of all descendent elements and text nodes.
    * @return the accumulated character content of the element, including descendant elements.
    */

    public String getStringValue() {
        StringBuffer sb = null;

        NodeImpl next = (NodeImpl)getFirstChild();
        while (next!=null) {
            if (next instanceof TextImpl) {
                if (sb==null) {
                    sb = new StringBuffer();
                }
                sb.append(next.getStringValue());
            }
            next = next.getNextInDocument(this);
        }
        if (sb==null) return "";
        return sb.toString();
    }

    /**
    * Copy the string-value of this node to a given outputter
    */

    public void copyStringValue(Outputter out) throws TransformerException {
        NodeImpl next = (NodeImpl)getFirstChild();
        while (next!=null) {
            if (next.getNodeType()==NodeInfo.TEXT) {
                next.copyStringValue(out);
            }
            next = next.getNextInDocument(this);
        }
    }

    /**
    * Supply an array to be used for the array of children. For system use only.
    */

    public void useChildrenArray(NodeImpl[] array) {
        children = array;
    }

    /**
    * Add a child node to this node. For system use only. Note: normalizing adjacent text nodes
    * is the responsibility of the caller.
    */

    public void addChild(NodeImpl node, int index) {
        NodeImpl[] c;
        if (children == null) {
            c = new NodeImpl[10];
        } else if (children instanceof NodeImpl) {
            c = new NodeImpl[10];
            c[0] = (NodeImpl)children;
        } else {
            c = (NodeImpl[])children;
        }
        if (index >= c.length) {
            NodeImpl[] kids = new NodeImpl[c.length * 2];
            System.arraycopy(c, 0, kids, 0, c.length);
            c = kids;
        }
        c[index] = node;
        node.parent = this;
        node.index = index;
        children = c;
    }

    /**
    * Remove node at given index. Will always be followed by a renumberChildren().
    */

    public void removeChild(int index) {
        if (children instanceof NodeImpl) {
            children = null;
        } else {
            ((NodeImpl[])children)[index] = null;
        }
    }

    /**
    * Renumber the children of a given parent node. For system use only
    */

    public void renumberChildren() {
        int j = 0;
        if (children==null) {
            return;
        } else if (children instanceof NodeImpl) {
            ((NodeImpl)children).parent = this;
            ((NodeImpl)children).index = 0;
        } else {
            NodeImpl[] c = (NodeImpl[])children;
            for (int i=0; i<c.length; i++) {
                if (c[i]!=null) {
                    c[i].parent = this;
                    c[i].index = j;
                    c[j] = c[i];
                    j++;
                }
            }
            compact(j);
        }
    }

    /**
    * Drop a branch of the tree. The target element remains in the tree, but its children are
    * disconnected from the parent. Unless there are other references to the children (e.g. in
    * a variable) they will be deleted from memory next time the garbage collector comes round.
    */

    public void dropChildren() {
        // truncate the string buffer to remove any string content
        NodeImpl n = getNextInDocument(this);
        while (n!=null) {
            if (n instanceof TextImpl) {
                ((TextImpl)n).truncateToStart();
                break;
            }
            n = n.getNextInDocument(this);
        }
        // now remove all the child nodes
        children = null;
    }

    /**
    * Compact the space used by this node
    */

    public void compact(int size) {
        if (size==0) {
            children = null;
        } else if (size==1) {
            if (children instanceof NodeImpl[]) {
                children = ((NodeImpl[])children)[0];
            }
        } else {
            NodeImpl[] kids = new NodeImpl[size];
            System.arraycopy(children, 0, kids, 0, size);
            children = kids;
        }
    }

    /**
    * Get the node value as defined in the DOM. This is not the same as the XPath string-value.
    */

    public String getNodeValue() {
        return null;
    }
}


//
// The contents of this file are subject to the Mozilla Public License Version 1.0 (the "License");
// you may not use this file except in compliance with the License. You may obtain a copy of the
// License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations under the License.
//
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is
// Michael Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
