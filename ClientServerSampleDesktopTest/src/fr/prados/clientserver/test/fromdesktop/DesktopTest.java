package fr.prados.clientserver.test.fromdesktop;

import java.util.Map;
import java.util.regex.Pattern;

import fr.prados.android.test.fordesktop.AdbDevice;
import fr.prados.android.test.fordesktop.DesktopTestCase;


public class DesktopTest extends DesktopTestCase
{
	public DesktopTest(String name)
	{
		super(name);
	}
	@Override
	protected void setUp() throws Exception
	{
		super.setUp();
		assertTrue(mDevices.length>=2);
	}
	public AdbDevice getServer()
	{
		return mDevices[0];
	}
	public AdbDevice getClient()
	{
		return mDevices[1];
	}
	public void testConnection() throws Throwable
	{
		try
		{
			// 1. Restart the application in the server.
			// Start the server and wait the "continue"
			// Can't use "instrument" because I want to pause the test 
			getServer().shell("am instrument "+
				// Wait the startup
				"-w "+ 
				// If running in debugger
				(isDebug() ? "-e debug true " : "")+
				// Select the test to run and pause
				"-e class fr.prados.clientserver.test.fordesktop.ForDesktopActivtyTest#testStartServer "+
				// The package
				"fr.prados.clientserver.test.fordesktop/"+
				// The instrumentation with public method would be called by Desktop
				"fr.prados.clientserver.test.fordesktop.ClientServerDesktopInstrumentationTestRunner");
			
			// Connect to the application for send 'execute' command via IP
			getServer().connectApp();

			// Invoke ClientServerDesktopInstrumentationTestRunner.getIp()
			String ip=(String)getServer().execute("getIp");

			// Now, instrument the testConnect with the detected ip
			Map<String,Object> result=getClient().instrument(
					// Add parameter. Can be retreive with getBundle().getString("target")
					"-e target "+ip+" "+
					// The specific test case
					"-e class fr.prados.clientserver.test.fordesktop.ForDesktopActivtyTest#testConnect "+
					// the package
					"fr.prados.clientserver.test.fordesktop/"+
					// and instrumentation
					"fr.prados.clientserver.test.fordesktop.ClientServerDesktopInstrumentationTestRunner");
			// Retreive the result
			String stream=(String)result.get("stream");
			// And check in the unit test in client is good
			assertTrue(Pattern.compile("^OK.*",Pattern.MULTILINE).matcher(stream).find());
		}
		finally
		{
			try
			{
				getServer().continueTest();
			}
			catch (Throwable e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
