package com.icl.saxon;
import com.icl.saxon.expr.*;


/**
* The Bindery class holds information about variables and their values.
*
* Variables are identified by a Binding object. Values can be any object, though values of XSL
* variables will always be of class Value.
*/

public final class Bindery  {

    private Object[] globals;    // global variables and parameters
    private boolean[] busy;
    private Object[][] stack = new Object[20][];    // stack for local variables and parameters
    private Object[] currentStackFrame;
    private ParameterSet globalParameters;          // supplied global parameters
    private int top = -1;
    private int allocated = 0;
    private int globalSpace = 0;
    private int localSpace = 0;

    /**
    * Define how many slots are needed for global variables
    */

    public void allocateGlobals(int n) {
        globalSpace = n;
        globals = new Object[n];
        busy = new boolean[n];
        for (int i=0; i<n; i++) {
            globals[i] = null;
            busy[i] = false;
        }
    }

    /**
    * Define global parameters
    * @param params The ParameterSet passed in by the user, eg. from the command line
    */

    public void defineGlobalParameters(ParameterSet params) {
        globalParameters = params;
    }

    /**
    * Use global parameter. This is called when a global xsl:param element is processed.
    * If a parameter of the relevant name was supplied, it is bound to the xsl:param element.
    * Otherwise the method returns false, so the xsl:param default will be evaluated
    * @param fingerprint The fingerprint of the parameter
    * @param binding The XSLParam element to bind its value to
    * @return true if a parameter of this name was supplied, false if not
    */

    public boolean useGlobalParameter(int fingerprint, Binding binding) {
        if (globalParameters==null) return false;
        Value val = globalParameters.get(fingerprint);
        if (val==null) return false;
        globals[binding.getSlotNumber()] = val;
        return true;
    }

    /**
    * Define global variable
    * @param name the name of the variable
    * @param value the value of the variable
    * @throws SAXException if the variable is already declared
    */

    public void defineGlobalVariable(Binding binding, Value value) {
        globals[binding.getSlotNumber()] = value;
    }

    /**
    * Set/Unset a flag to indicate that a particular global variable is currently being
    * evaluated.
    * @throws XPathException If an attempt is made to set the flag when it is already set, this means
    * the definition of the variable is circular.
    */

    public void setExecuting(Binding binding, boolean executing)
    throws XPathException {
        int slot = binding.getSlotNumber();
        if (executing) {
            if (busy[slot]) {
                throw new XPathException(
                            "Circular definition of variable " +
                                         binding.getVariableName());
            }
            // It would be better to detect circular references statically
            // at compile time. However, this is not always possible, because they
            // can arise via execution of templates or stylesheet functions.
            busy[slot]=true;
        } else {
            busy[slot]=false;
        }
    }

    /**
    * Test if global variable has already been evaluated
    */

    public boolean isEvaluated(Binding binding) {
        return globals[binding.getSlotNumber()]!=null;
    }

    /**
    * Define how many slots are needed for local variables. We work on the basis of
    * "one size fits all": all stackframes are allocated as large as the largest one needed
    */

    public void allocateLocals(int n) {
        if (n>localSpace) {
            localSpace = n;
        }
    }

    /**
    * Start a new stack frame for local variables
    */

    public void openStackFrame(ParameterSet localParameters) {
        if (++top >= allocated) {
            if (allocated==stack.length) {
                Object[][] stack2 = new Object[allocated*2][];
                System.arraycopy(stack, 0, stack2, 0, allocated);
                stack = stack2;
            }
            currentStackFrame = new Object[localSpace+1];
            stack[top]=currentStackFrame;
            allocated++;
        } else {
            currentStackFrame = stack[top];
        }

        currentStackFrame[0]=localParameters;
        for (int i=1; i<currentStackFrame.length; i++) {
            currentStackFrame[i] = null;
        }
    }

    /**
    * Close the current stack frame for local variables
    */

    public void closeStackFrame() {
        top--;
        currentStackFrame = (top<0 ? null : stack[top]);
    }

    /**
    * Use local parameter. This is called when a local xsl:param element is processed.
    * If a parameter of the relevant name was supplied, it is bound to the xsl:param element.
    * Otherwise the method returns false, so the xsl:param default will be evaluated
    * @param fingerprint The fingerprint of the parameter name
    * @param binding The XSLParam element to bind its value to
    * @return true if a parameter of this name was supplied, false if not
    */

    public boolean useLocalParameter(int fingerprint, Binding binding) {
    	ParameterSet params = (ParameterSet)currentStackFrame[0];
    	if (params==null) return false;
    	Value val = params.get(fingerprint);
    	currentStackFrame[binding.getSlotNumber()+1] = val;
    	return val!=null;
    }

    /**
    * Get local parameter. This method is available to user-written node handlers invoked
    * via the saxon:handler interface, it allows them to retrieve the values of parameters
    * set up within a calling XSL template.
    * @name The name of the parameter (an absolute/expanded name, i.e. URI plus local part)
    * @return The value of the parameter, or null if not supplied
    */

    public Value getLocalParameter(int fingerprint) {
        ParameterSet params = (ParameterSet)currentStackFrame[0];
        if (params==null) return null;
        return (Value)params.get(fingerprint);
    }

    /**
    * Define local variable
    * @param name the name of the variable
    * @param value the value of the variable
    */

    public void defineLocalVariable(Binding binding, Value value) {
        if (currentStackFrame==null) {
            throw new IllegalArgumentException("Can't define local variable: stack is empty");
        }
        currentStackFrame[binding.getSlotNumber()+1] = value;
    }

    /**
    * Get the value of a variable
    * @param binding the Binding that establishes the unique instance of the variable
    * @return the Value of the variable if defined, null otherwise.
    */

    public Value getValue(Binding binding) {
        if (binding.isGlobal()) {
            return (Value)globals[binding.getSlotNumber()];
        } else {
            if (currentStackFrame != null) {
                return (Value)currentStackFrame[binding.getSlotNumber()+1];
            } else {
                return null;
            }
        }
    }

    /**
    * Get the value of a variable in the given frame
    * @param binding the Binding that establishes the unique instance of the variable
    * @param frameId the id of the frame, see getFrameId
    * @return the Value of the variable if defined, null otherwise.
    */

    public Value getValue( Binding binding, int frameId ) { // e.g.
        if (binding.isGlobal()) {
            return (Value)globals[binding.getSlotNumber()];
        } else {
	        Object[] theStackFrame = stack[frameId];
            if (theStackFrame != null) {
                return (Value)theStackFrame[binding.getSlotNumber()+1];
            } else {
                return null;
            }
        }
    }

    /**
    * Get the id of the current frame.
    * @return an id, that may be given to getValue(Binding,int)
    */

    public int getFrameId() { // e.g.
      return top;
    }

    /**
    * Assign a new value to a variable
    * @param name the name of the local or global variable or parameter (without a $ sign)
    * @return the Value of the variable
    * @throws SAXException if the variable has not been declared
    */

    public void assignVariable( Binding binding, Value value ) {
        if (binding.isGlobal()) {
            defineGlobalVariable(binding, value);
        } else {
            defineLocalVariable(binding, value);
        }
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
// Contributor(s):
// Portions marked "e.g." are from Edwin Glaser (edwin@pannenleiter.de)
//
//
