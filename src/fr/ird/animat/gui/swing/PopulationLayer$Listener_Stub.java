// Stub class generated by rmic, do not edit.
// Contents subject to change without notice.

package fr.ird.animat.gui.swing;

public final class PopulationLayer$Listener_Stub extends java.rmi.server.RemoteStub implements fr.ird.animat.event.PopulationChangeListener, java.rmi.Remote
{
    private static final long serialVersionUID = 2;
    
    private static java.lang.reflect.Method $method_populationChanged_0;
    private static long hash;
    
    static {
	try {
	    $method_populationChanged_0 = fr.ird.animat.event.PopulationChangeListener.class.getMethod("populationChanged", new java.lang.Class[] {fr.ird.animat.event.PopulationChangeEvent.class});
	} catch (java.lang.NoSuchMethodException e) {
	    throw new java.lang.NoSuchMethodError(
		"stub class initialization failed");
	}
        hash = EnvironmentLayer$Listener_Stub.computeMethodHash($method_populationChanged_0);
    }
    
    // constructors
    public PopulationLayer$Listener_Stub(java.rmi.server.RemoteRef ref) {
	super(ref);
    }
    
    // methods from remote interfaces
    
    // implementation of populationChanged
    public void populationChanged(fr.ird.animat.event.PopulationChangeEvent event)
	throws java.rmi.RemoteException
    {
	try {
	    Object $result = ref.invoke(this, $method_populationChanged_0, new java.lang.Object[] {event}, hash);
	} catch (java.lang.RuntimeException e) {
	    throw e;
	} catch (java.rmi.RemoteException e) {
	    throw e;
	} catch (java.lang.Exception e) {
	    throw new java.rmi.UnexpectedException("undeclared checked exception", e);
	}
    }
}
