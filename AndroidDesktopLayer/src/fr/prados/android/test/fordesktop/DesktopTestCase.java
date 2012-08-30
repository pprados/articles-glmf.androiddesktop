package fr.prados.android.test.fordesktop;

import java.util.logging.Logger;

import junit.framework.TestCase;

public abstract class DesktopTestCase extends TestCase
{
	protected static final Logger LOG = Logger.getLogger(DesktopTestCase.class.getName());
	
	static Adb sAdb;
	static String[] sDevicesNames;
	protected AdbDevice[] mDevices;
	public DesktopTestCase(String name)
	{
		super(name);
		if (sAdb==null)
		{
			sAdb=new Adb();
			try { Thread.sleep(1000); } catch (Throwable e) {}
			sDevicesNames=sAdb.getDevices();
		}
	}
	@Override
	protected void setUp() throws Exception
	{
		mDevices=new AdbDeviceImpl[sDevicesNames.length];
		for (int i=0;i<sDevicesNames.length;++i)
		{
			mDevices[i]=sAdb.waitForConnection(5000,sDevicesNames[i]);
		}
	}
	@Override
	protected void tearDown() throws Exception
	{
		for (int i=0;i<mDevices.length;++i)
		{
			mDevices[i].dispose();
		}
	}
	protected boolean isDebug()
	{ 
		return java.lang.management.ManagementFactory.getRuntimeMXBean().
			    getInputArguments().toString().indexOf("-agentlib:jdwp") > 0;		
	}

}
