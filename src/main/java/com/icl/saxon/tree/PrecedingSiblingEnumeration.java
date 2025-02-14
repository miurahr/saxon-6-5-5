package com.icl.saxon.tree;
import com.icl.saxon.pattern.NodeTest;

final class PrecedingSiblingEnumeration extends TreeEnumeration {

    public PrecedingSiblingEnumeration(NodeImpl node, NodeTest nodeTest) {
        super(node, nodeTest);
        advance();
    }

    protected void step() {
        next = (NodeImpl)next.getPreviousSibling();
    }

    /**
    * Get the last position, that is the number of nodes in the enumeration
    */

    public int getLastPosition() {
        if (last>=0) return last;
        PrecedingSiblingEnumeration enm =
            new PrecedingSiblingEnumeration(start, nodeTest);
        return enm.count();
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
