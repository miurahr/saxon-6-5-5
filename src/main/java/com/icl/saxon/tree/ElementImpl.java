package com.icl.saxon.tree;
import com.icl.saxon.om.NodeInfo;
import com.icl.saxon.om.DocumentInfo;
import com.icl.saxon.om.NamespaceException;
import com.icl.saxon.om.NamePool;
import com.icl.saxon.om.Namespace;
import com.icl.saxon.om.Name;
import com.icl.saxon.expr.NodeSetExtent;
import com.icl.saxon.output.Outputter;
import com.icl.saxon.pattern.NameTest;
import com.icl.saxon.pattern.NamespaceTest;

import java.util.Vector;
import org.w3c.dom.*;

import javax.xml.transform.TransformerException;

/**
  * ElementImpl implements an element with no attributes or namespace declarations.<P>
  * This class is an implementation of NodeInfo. For elements with attributes or
  * namespace declarations, class ElementWithAttributes is used.
  * @author Michael H. Kay
  */

// The name of the element and its attributes are now namespace-resolved by the
// parser. However, this class retains the ability to do namespace resolution for other
// names, for example variable and template names in a stylesheet.

public class ElementImpl extends ParentNodeImpl
    implements Element {

    private static AttributeCollection emptyAtts = new AttributeCollection((NamePool)null);

    protected int nameCode;
    protected DocumentImpl root;

    /**
    * Construct an empty ElementImpl
    */

    public ElementImpl() {}

    /**
    * Set the name code. Used when creating a dummy element in the Stripper
    */

    public void setNameCode(int nameCode) {
    	this.nameCode = nameCode;
    }

    /**
    * Initialise a new ElementImpl with an element name
    * @param name The element name, with namespaces resolved
    * @param atts The attribute list: always null
    * @param parent The parent node
    */

    public void initialise(int nameCode, AttributeCollection atts, NodeInfo parent,
                            String baseURI, int lineNumber, int sequenceNumber) {
        this.nameCode = nameCode;
        this.parent = (ParentNodeImpl)parent;
        this.sequence = sequenceNumber;
        this.root = (DocumentImpl)parent.getDocumentRoot();
        root.setLineNumber(sequenceNumber, lineNumber);
        root.setSystemId(sequenceNumber, baseURI);
    }

    /**
    * Set the system ID of this node. This method is provided so that a NodeInfo
    * implements the javax.xml.transform.Source interface, allowing a node to be
    * used directly as the Source of a transformation
    */

    public void setSystemId(String uri) {
        root.setSystemId(sequence, uri);
    }

	/**
	* Get the root node
	*/

	public DocumentInfo getDocumentRoot() {
		return root;
	}

    /**
    * Get the system ID of the entity containing this element node.
    */

    public final String getSystemId() {
        return ((DocumentImpl)getDocumentRoot()).getSystemId(sequence);
    }

    /**
    * Get the base URI of this element node. This will be the same as the System ID unless
    * xml:base has been used.
    */

    public String getBaseURI() {
        String xmlBase = getAttributeValue(Namespace.XML, "base");
        if (xmlBase!=null) {
            return xmlBase;
        }
        String startSystemId = getSystemId();
        String parentSystemId = parent.getSystemId();
        if (startSystemId.equals(parentSystemId)) {
            return parent.getBaseURI();
        } else {
            return startSystemId;
        }
    }

    /**
    * Set the line number of the element within its source document entity
    */

    public void setLineNumber(int line) {
        ((DocumentImpl)getDocumentRoot()).setLineNumber(sequence, line);
    }


    /**
    * Get the line number of the node within its source document entity
    */

    public int getLineNumber() {
        return ((DocumentImpl)getDocumentRoot()).getLineNumber(sequence);
    }


	/**
	* Get the nameCode of the node. This is used to locate the name in the NamePool
	*/

	public int getNameCode() {
		return nameCode;
	}

    /**
    * Get a character string that uniquely identifies this node within this document
    * (The calling code will prepend a document identifier)
    * @return a string.
    */

    public String generateId() {
        return "e" + sequence;
    }

    /**
    * Search the NamespaceList for a given prefix, returning the corresponding URI.
    * @param prefix The prefix to be matched. To find the default namespace, supply ""
    * @return The URI code corresponding to this namespace. If it is an unnamed default namespace,
    * return Namespace.NULL_CODE.
    * @throws NamespaceException if the prefix has not been declared on this NamespaceList.
    */

    public short getURICodeForPrefix(String prefix) throws NamespaceException {
    	// this is actually never called; it's used only in a Stylesheet, and in a Stylesheet
    	// we always use the version on ElementWithAttributes
        if (prefix.equals("xml")) return Namespace.XML_CODE;
        if (parent.getNodeType()==NodeInfo.ROOT) {
            if (prefix.equals("")) {
            	return Namespace.NULL_CODE;
            }
            throw new NamespaceException(prefix);
        } else {
            return ((ElementImpl)parent).getURICodeForPrefix(prefix);
        }
    }

    /**
    * Search the NamespaceList for a given URI, returning the corresponding prefix.
    * @param uri The URI to be matched.
    * @return The prefix corresponding to this URI. If not found, return null. If there is
    * more than one prefix matching the URI, the first one found is returned. If the URI matches
    * the default namespace, return an empty string.
    */

    public String getPrefixForURI(String uri) {
        if (parent.getNodeType()==NodeInfo.ROOT) {
            return null;
        } else {
            return ((ElementImpl)parent).getPrefixForURI(uri);
        }
    }

    /**
    * Make a NameCode, using this Element as the context for namespace resolution.
    * The name will be entered in the namepool: therefore this method should not be
    * called once the name pool is sealed.
    * @param qname The name as written, in the form "[prefix:]localname"
    * @boolean useDefault Defines the action when there is no prefix. If true, use
    * the default namespace URI (as for element names). If false, use no namespace URI
    * (as for attribute names).
    */

    public final int makeNameCode(String qname, boolean useDefault)
    throws NamespaceException {

		NamePool namePool = getNamePool();
		String prefix = Name.getPrefix(qname);
        if (prefix.equals("")) {
            short uriCode = 0;

            if (useDefault) {
                uriCode = getURICodeForPrefix(prefix);
            }

			return namePool.allocate(prefix, uriCode, qname);

        } else {
            String localName = Name.getLocalName(qname);
            short uriCode = getURICodeForPrefix(prefix);
			return namePool.allocate(prefix, uriCode, localName);
        }

	}

    /**
    * Make the set of all namespace nodes associated with this element.
    * @param owner The element owning these namespace nodes.
    * @param list a Vector containing NamespaceImpl objects representing the namespaces
    * in scope for this element; the method appends nodes to this Vector, which should
    * initially be empty. Note that the returned list will never contain the XML namespace
    * (to get this, the NamespaceEnumeration class adds it itself). The list WILL include
    * an entry for the undeclaration xmlns=""; again it is the job of NamespaceEnumeration
    * to ignore this, since it doesn't represent a true namespace node.
    * @param addXML Add the XML namespace node to the list
    */

    public void addNamespaceNodes(ElementImpl owner, Vector list, boolean addXML) {
        // just add the namespaces defined on the ancestor nodes

        if (parent.getNodeType()!=NodeInfo.ROOT) {
            ((ElementImpl)parent).addNamespaceNodes(owner, list, false);
        }
        if (addXML) {
        	int nsxml = (1<<16) + 1;
            list.addElement(
                new NamespaceImpl(this, nsxml, list.size()+1)
                );
        }
    }

    /**
    * Output all namespace nodes associated with this element.
    * @param out The relevant outputter
    */

    public void outputNamespaceNodes(Outputter out, boolean includeAncestors) throws TransformerException {

        // just add the namespaces defined on the ancestor nodes. We rely on the outputter
        // to eliminate multiple declarations of the same prefix

        if (includeAncestors) {
            if (!(parent instanceof DocumentInfo)) {
                ((ElementImpl)parent).outputNamespaceNodes(out, true);
            }
        }
    }



    /**
    * Return the type of node.
    * @return NodeInfo.ELEMENT
    */

    public final short getNodeType() {
        return ELEMENT;
    }

    /**
    * Get the attribute list for this element.
    * @return The attribute list. This will not include any
    * namespace attributes. The attribute names will be in expanded form, with prefixes
    * replaced by URIs
    */

    public AttributeCollection getAttributeList() {
        return emptyAtts;
    }

    /**
     *  Find the value of a given attribute of this element. <BR>
     *  This is a short-cut method; the full capability to examine
     *  attributes is offered via the getAttributeList() method. <BR>
     *  The attribute may either be one that was present in the original XML document,
     *  or one that has been set by the application using setAttribute(). <BR>
     *  @param name the name of an attribute. There must be no prefix in the name.
     *  @return the value of the attribute, if it exists, otherwise null
     */

    public String getAttributeValue( String name ) {
        return null;
    }


    /**
    * Set the value of an attribute on the current element.
    * @throws DOMException (always): the Saxon tree is immutable
    */

    public void setAttribute(String name, String value ) throws DOMException {
        disallowUpdate();
    }

    /**
    * Copy this node to a given outputter (supporting xsl:copy-of)
    */

    public void copy(Outputter out) throws TransformerException {
        copy(out, true);
    }

    /**
    * Copy this node to a given outputter (supporting xsl:copy-of)
    * @param out The outputter
    * @param allNamespaces true if namespaces for ancestor nodes must be output
    */

    public void copy(Outputter out, boolean allNamespaces) throws TransformerException {
    	int nc = getNameCode();
        out.writeStartTag(nc);

        // output the namespaces

        outputNamespaceNodes(out, allNamespaces);

        // output the children

        NodeImpl next = (NodeImpl)getFirstChild();
        while (next!=null) {
            if (next instanceof ElementImpl) {
                ((ElementImpl)next).copy(out, false);
            } else {
                next.copy(out);
            }
            next = (NodeImpl)next.getNextSibling();
        }

        out.writeEndTag(nc);
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
