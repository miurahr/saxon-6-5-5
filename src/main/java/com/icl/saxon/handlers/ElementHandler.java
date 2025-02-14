package com.icl.saxon.handlers;
import com.icl.saxon.Context;
import javax.xml.transform.TransformerException;
import com.icl.saxon.NodeHandler;
import com.icl.saxon.om.NodeInfo;

/**
 * ElementHandler is a NodeHandler used to process elements. It is identical to
 * it parent class, NodeHandler, and exists only for type-checking on interfaces.
 * @author Michael H. Kay
 * @version 7 April 1999: generalisation of old ElementHandler
 */

public abstract class ElementHandler implements NodeHandler {

    /**
    * Define action to be taken at the start of a node.<BR>
    * This method must be implemented in a subclass.
    * @param node The NodeInfo object for the current node.
    * @exception SAXException Aborts the parse
    * @see NodeInfo
    */

    public abstract void start( NodeInfo node, Context context )
    throws TransformerException;

    public boolean needsStackFrame() {
        return false;
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
