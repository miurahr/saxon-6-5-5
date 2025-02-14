package com.icl.saxon.tinytree;
import com.icl.saxon.pattern.NodeTest;
import com.icl.saxon.om.NodeInfo;
import com.icl.saxon.om.AxisEnumeration;

/**
* Enumerate the following axis starting at a given node.
* The start node must not be a namespace or attribute node.
*/

final class FollowingEnumeration implements AxisEnumeration {

    private TinyDocumentImpl document;
    private TinyNodeImpl startNode;
    private int nextNodeNr;
    private NodeTest test;
    int last = -1;
    boolean includeDescendants;

    public FollowingEnumeration(TinyDocumentImpl doc, TinyNodeImpl node,
                                 NodeTest nodeTest, boolean includeDescendants) {
        document = doc;
        test = nodeTest;
        startNode = node;
        nextNodeNr = node.nodeNr;
        this.includeDescendants = includeDescendants;
        int depth = doc.depth[nextNodeNr];

        // skip the descendant nodes if any
        if (includeDescendants) {
            nextNodeNr++;
        } else {
            do {
                nextNodeNr++;
                if (nextNodeNr >= doc.numberOfNodes) {
                    nextNodeNr = -1;
                    return;
                }
            } while (doc.depth[nextNodeNr] > depth);
        }

        if (!test.matches(doc.nodeType[nextNodeNr], doc.nameCode[nextNodeNr])) {
            advance();
        }
    }

    private void advance() {
        do {
            nextNodeNr++;
            if (nextNodeNr >= document.numberOfNodes) {
                nextNodeNr = -1;
                return;
            }
        } while (!test.matches(document.nodeType[nextNodeNr], document.nameCode[nextNodeNr]));
    }

    public boolean hasMoreElements() {
        return nextNodeNr >= 0;
    }

    public NodeInfo nextElement() {
        TinyNodeImpl node = document.getNode(nextNodeNr);
        advance();
        return node;
    }

    public boolean isSorted() {
        return true;
    }

    public boolean isReverseSorted() {
        return false;
    }

    public boolean isPeer() {
        return false;
    }

    /**
    * Get the last position, that is the number of nodes in the enumeration
    */

    public int getLastPosition() {
        if (last >= 0) return last;
        FollowingEnumeration enm =
            new FollowingEnumeration(document, startNode, test, includeDescendants);
        last = 0;
        while (enm.hasMoreElements()) {
            enm.nextElement();
            last++;
        }
        return last;
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
