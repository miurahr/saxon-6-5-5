package com.icl.saxon.functions;
import com.icl.saxon.*;
//import com.icl.saxon.om.Name;
import com.icl.saxon.om.Namespace;
import com.icl.saxon.om.NodeInfo;
import com.icl.saxon.expr.*;

import java.util.*;
//import java.lang.Math;
//import java.text.*;



public class Lang extends Function {

    /**
    * Function name (for diagnostics)
    */

    public String getName() {
        return "lang";
    };

    /**
    * Determine the data type of the expression
    * @return Value.BOOLEAN
    */

    public int getDataType() {
        return Value.BOOLEAN;
    }

    /**
    * Simplify and validate.
    */

    public Expression simplify() throws XPathException {
        checkArgumentCount(1, 1);
        argument[0] = argument[0].simplify();
        return this;
    }

    /**
    * Evaluate the function in a boolean context
    */

    public boolean evaluateAsBoolean(Context c) throws XPathException {
        return isLang(argument[0].evaluateAsString(c), c);
    }

    /**
    * Evaluate in a general context
    */

    public Value evaluate(Context c) throws XPathException {
        return new BooleanValue(evaluateAsBoolean(c));
    }

    /**
    * Determine the dependencies
    */

    public int getDependencies() {
        return Context.CONTEXT_NODE | argument[0].getDependencies();
    }

    /**
    * Reduce the dependencies
    */

    public Expression reduce(int dep, Context c) throws XPathException {
        Lang f = new Lang();
        f.addArgument(argument[0].reduce(dep, c));
        f.setStaticContext(getStaticContext());
        return f.simplify();
    }

    /**
    * Test whether the context node has the given language attribute
    * @param arglang the language being tested
    * @param context the context, to identify the context node
    */

    private static boolean isLang(String arglang, Context context) throws XPathException {

        NodeInfo node = context.getContextNodeInfo();

        String doclang=null;

        while(node!=null) {
            doclang = node.getAttributeValue(Namespace.XML, "lang");
            if (doclang!=null) break;
            node=(NodeInfo)node.getParent();
        }

        if (doclang==null) return false;

        if (arglang.equalsIgnoreCase(doclang)) return true;
        int hyphen = doclang.indexOf("-");
        if (hyphen<0) return false;
        doclang = doclang.substring(0, hyphen);
        if (arglang.equalsIgnoreCase(doclang)) return true;
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
