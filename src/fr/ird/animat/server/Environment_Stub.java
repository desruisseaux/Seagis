// Stub class generated by rmic, do not edit.
// Contents subject to change without notice.

package fr.ird.animat.server;

public final class Environment_Stub
    extends java.rmi.server.RemoteStub
    implements fr.ird.animat.Environment, java.rmi.Remote
{
    private static final long serialVersionUID = 2;
    
    private static final java.lang.reflect.Method $method_addEnvironmentChangeListener_0;
    private static final java.lang.reflect.Method $method_getClock_1;
    private static final java.lang.reflect.Method $method_getCoverage_2;
    private static final java.lang.reflect.Method $method_getCoverages;
    private static final java.lang.reflect.Method $method_getReport;
    private static final java.lang.reflect.Method $method_getParameters_3;
    private static final java.lang.reflect.Method $method_getPopulations_4;
    private static final java.lang.reflect.Method $method_newPopulation_5;
    private static final java.lang.reflect.Method $method_removeEnvironmentChangeListener_6;
    
    private static final long getCoverages_hash;
    private static final long getReport_hash;
    
    static {
	try {
	    $method_addEnvironmentChangeListener_0 = fr.ird.animat.Environment.class.getMethod("addEnvironmentChangeListener", new java.lang.Class[] {fr.ird.animat.event.EnvironmentChangeListener.class});
	    $method_getClock_1 = fr.ird.animat.Environment.class.getMethod("getClock", new java.lang.Class[] {});
	    $method_getCoverage_2 = fr.ird.animat.Environment.class.getMethod("getCoverage", new java.lang.Class[] {fr.ird.animat.Parameter.class});
	    $method_getCoverages = fr.ird.animat.Environment.class.getMethod("getCoverageNames", new java.lang.Class[] {});
	    $method_getReport = fr.ird.animat.Environment.class.getMethod("getReport", new java.lang.Class[] {boolean.class});
	    $method_getParameters_3 = fr.ird.animat.Environment.class.getMethod("getParameters", new java.lang.Class[] {});
	    $method_getPopulations_4 = fr.ird.animat.Environment.class.getMethod("getPopulations", new java.lang.Class[] {});
	    $method_newPopulation_5 = fr.ird.animat.Environment.class.getMethod("newPopulation", new java.lang.Class[] {});
	    $method_removeEnvironmentChangeListener_6 = fr.ird.animat.Environment.class.getMethod("removeEnvironmentChangeListener", new java.lang.Class[] {fr.ird.animat.event.EnvironmentChangeListener.class});
	} catch (java.lang.NoSuchMethodException e) {
	    throw new java.lang.NoSuchMethodError(
		"stub class initialization failed");
	}
        getCoverages_hash = fr.ird.animat.gui.swing.EnvironmentLayer$Listener_Stub.computeMethodHash($method_getCoverages);
        getReport_hash = fr.ird.animat.gui.swing.EnvironmentLayer$Listener_Stub.computeMethodHash($method_getReport);
    }
    
    // constructors
    public Environment_Stub(java.rmi.server.RemoteRef ref) {
	super(ref);
    }
    
    // methods from remote interfaces
    
    // implementation of addEnvironmentChangeListener(EnvironmentChangeListener)
    public void addEnvironmentChangeListener(fr.ird.animat.event.EnvironmentChangeListener $param_EnvironmentChangeListener_1)
	throws java.rmi.RemoteException
    {
	try {
	    ref.invoke(this, $method_addEnvironmentChangeListener_0, new java.lang.Object[] {$param_EnvironmentChangeListener_1}, -1514485067543907663L);
	} catch (java.lang.RuntimeException e) {
	    throw e;
	} catch (java.rmi.RemoteException e) {
	    throw e;
	} catch (java.lang.Exception e) {
	    throw new java.rmi.UnexpectedException("undeclared checked exception", e);
	}
    }
    
    // implementation of getClock()
    public fr.ird.animat.Clock getClock()
	throws java.rmi.RemoteException
    {
	try {
	    Object $result = ref.invoke(this, $method_getClock_1, null, 6357386218885171077L);
	    return ((fr.ird.animat.Clock) $result);
	} catch (java.lang.RuntimeException e) {
	    throw e;
	} catch (java.rmi.RemoteException e) {
	    throw e;
	} catch (java.lang.Exception e) {
	    throw new java.rmi.UnexpectedException("undeclared checked exception", e);
	}
    }
    
    // implementation of getCoverage(Parameter)
    public org.opengis.cv.CV_Coverage getCoverage(fr.ird.animat.Parameter $param_Parameter_1)
	throws java.rmi.RemoteException, java.util.NoSuchElementException
    {
	try {
	    Object $result = ref.invoke(this, $method_getCoverage_2, new java.lang.Object[] {$param_Parameter_1}, -3620930591303126415L);
	    return ((org.opengis.cv.CV_Coverage) $result);
	} catch (java.lang.RuntimeException e) {
	    throw e;
	} catch (java.rmi.RemoteException e) {
	    throw e;
	} catch (java.lang.Exception e) {
	    throw new java.rmi.UnexpectedException("undeclared checked exception", e);
	}
    }

    public java.lang.String[] getCoverageNames() throws java.rmi.RemoteException {
	try {
	    Object $result = ref.invoke(this, $method_getCoverages, null, getCoverages_hash);
	    return ((java.lang.String[]) $result);
	} catch (java.lang.RuntimeException e) {
	    throw e;
	} catch (java.rmi.RemoteException e) {
	    throw e;
	} catch (java.lang.Exception e) {
	    throw new java.rmi.UnexpectedException("undeclared checked exception", e);
	}
    }

    public fr.ird.animat.Report getReport(boolean full) throws java.rmi.RemoteException {
	try {
	    Object $result = ref.invoke(this, $method_getReport, new Object[]{Boolean.valueOf(full)}, getReport_hash);
	    return ((fr.ird.animat.Report) $result);
	} catch (java.lang.RuntimeException e) {
	    throw e;
	} catch (java.rmi.RemoteException e) {
	    throw e;
	} catch (java.lang.Exception e) {
	    throw new java.rmi.UnexpectedException("undeclared checked exception", e);
	}
    }
    
    // implementation of getParameters()
    public java.util.Set<fr.ird.animat.Parameter> getParameters()
	throws java.rmi.RemoteException
    {
	try {
	    Object $result = ref.invoke(this, $method_getParameters_3, null, 671810519507870084L);
	    return ((java.util.Set) $result);
	} catch (java.lang.RuntimeException e) {
	    throw e;
	} catch (java.rmi.RemoteException e) {
	    throw e;
	} catch (java.lang.Exception e) {
	    throw new java.rmi.UnexpectedException("undeclared checked exception", e);
	}
    }
    
    // implementation of getPopulations()
    public java.util.Set<fr.ird.animat.Population> getPopulations()
	throws java.rmi.RemoteException
    {
	try {
	    Object $result = ref.invoke(this, $method_getPopulations_4, null, 6982386256142598107L);
	    return ((java.util.Set) $result);
	} catch (java.lang.RuntimeException e) {
	    throw e;
	} catch (java.rmi.RemoteException e) {
	    throw e;
	} catch (java.lang.Exception e) {
	    throw new java.rmi.UnexpectedException("undeclared checked exception", e);
	}
    }
    
    // implementation of newPopulation()
    public fr.ird.animat.Population newPopulation()
	throws java.rmi.RemoteException
    {
	try {
	    Object $result = ref.invoke(this, $method_newPopulation_5, null, -8941090246867479019L);
	    return ((fr.ird.animat.Population) $result);
	} catch (java.lang.RuntimeException e) {
	    throw e;
	} catch (java.rmi.RemoteException e) {
	    throw e;
	} catch (java.lang.Exception e) {
	    throw new java.rmi.UnexpectedException("undeclared checked exception", e);
	}
    }
    
    // implementation of removeEnvironmentChangeListener(EnvironmentChangeListener)
    public void removeEnvironmentChangeListener(fr.ird.animat.event.EnvironmentChangeListener $param_EnvironmentChangeListener_1)
	throws java.rmi.RemoteException
    {
	try {
	    ref.invoke(this, $method_removeEnvironmentChangeListener_6, new java.lang.Object[] {$param_EnvironmentChangeListener_1}, 3993974988169722585L);
	} catch (java.lang.RuntimeException e) {
	    throw e;
	} catch (java.rmi.RemoteException e) {
	    throw e;
	} catch (java.lang.Exception e) {
	    throw new java.rmi.UnexpectedException("undeclared checked exception", e);
	}
    }
}