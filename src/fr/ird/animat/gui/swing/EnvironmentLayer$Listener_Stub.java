// Stub class generated by rmic, do not edit.
// Contents subject to change without notice.

package fr.ird.animat.gui.swing;

public final class EnvironmentLayer$Listener_Stub extends java.rmi.server.RemoteStub implements fr.ird.animat.event.EnvironmentChangeListener, java.rmi.Remote
{
    private static final long serialVersionUID = 2;
    
    private static java.lang.reflect.Method $method_environmentChanged_0;
    private static long hash;
    
    static {
	try {
	    $method_environmentChanged_0 = fr.ird.animat.event.EnvironmentChangeListener.class.getMethod("environmentChanged", new java.lang.Class[] {fr.ird.animat.event.EnvironmentChangeEvent.class});
	} catch (java.lang.NoSuchMethodException e) {
	    throw new java.lang.NoSuchMethodError(
		"stub class initialization failed");
	}
        hash = computeMethodHash($method_environmentChanged_0);
    }
    
    // constructors
    public EnvironmentLayer$Listener_Stub(java.rmi.server.RemoteRef ref) {
	super(ref);
    }
    
    // methods from remote interfaces
    
    // implementation of environmentChanged
    public void environmentChanged(fr.ird.animat.event.EnvironmentChangeEvent event)
	throws java.rmi.RemoteException
    {
	try {
	    Object $result = ref.invoke(this, $method_environmentChanged_0, new java.lang.Object[] {event}, hash);
	} catch (java.lang.RuntimeException e) {
	    throw e;
	} catch (java.rmi.RemoteException e) {
	    throw e;
	} catch (java.lang.Exception e) {
	    throw new java.rmi.UnexpectedException("undeclared checked exception", e);
	}
    }

    /**
     * Compute the "method hash" of a remote method.  The method hash
     * is a long containing the first 64 bits of the SHA digest from
     * the UTF encoded string of the method name and descriptor.
     */
    public static long computeMethodHash(java.lang.reflect.Method m) {
	long hash = 0;
	java.io.ByteArrayOutputStream sink = new java.io.ByteArrayOutputStream(127);
	try {
	    java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA");
	    java.io.DataOutputStream out = new java.io.DataOutputStream(
		new java.security.DigestOutputStream(sink, md));

	    String s = getMethodNameAndDescriptor(m);
	    out.writeUTF(s);

	    // use only the first 64 bits of the digest for the hash
	    out.flush();
	    byte hasharray[] = md.digest();
	    for (int i = 0; i < Math.min(8, hasharray.length); i++) {
		hash += ((long) (hasharray[i] & 0xFF)) << (i * 8);
	    }
	} catch (java.io.IOException ignore) {
	    /* can't happen, but be deterministic anyway. */
	    hash = -1;
	} catch (java.security.NoSuchAlgorithmException complain) {
	    throw new SecurityException(complain.getMessage());
	}
	return hash;
    }

    /**
     * Return a string consisting of the given method's name followed by
     * its "method descriptor", as appropriate for use in the computation
     * of the "method hash".
     *
     * See section 4.3.3 of The Java Virtual Machine Specification for
     * the definition of a "method descriptor".
     */
    private static String getMethodNameAndDescriptor(java.lang.reflect.Method m) {
	StringBuffer desc = new StringBuffer(m.getName());
	desc.append('(');
	Class[] paramTypes = m.getParameterTypes();
	for (int i = 0; i < paramTypes.length; i++) {
	    desc.append(getTypeDescriptor(paramTypes[i]));
	}
	desc.append(')');
	Class returnType = m.getReturnType();
	if (returnType == void.class) {	// optimization: handle void here
	    desc.append('V');
	} else {
	    desc.append(getTypeDescriptor(returnType));
	}
	return desc.toString();
    }

    /**
     * Get the descriptor of a particular type, as appropriate for either
     * a parameter or return type in a method descriptor.
     */
    private static String getTypeDescriptor(Class type) {
	if (type.isPrimitive()) {
	    if (type == int.class) {
		return "I";
	    } else if (type == boolean.class) {
		return "Z";
	    } else if (type == byte.class) {
		return "B";
	    } else if (type == char.class) {
		return "C";
	    } else if (type == short.class) {
		return "S";
	    } else if (type == long.class) {
		return "J";
	    } else if (type == float.class) {
		return "F";
	    } else if (type == double.class) {
		return "D";
	    } else if (type == void.class) {
		return "V";
	    } else {
		throw new Error("unrecognized primitive type: " + type);
	    }
	} else if (type.isArray()) {
	    /*
	     * According to JLS 20.3.2, the getName() method on Class does
	     * return the VM type descriptor format for array classes (only);
	     * using that should be quicker than the otherwise obvious code:
	     *
	     *     return "[" + getTypeDescriptor(type.getComponentType());
	     */
	    return type.getName().replace('.', '/');
	} else {
	    return "L" + type.getName().replace('.', '/') + ";";
	}
    }
}
