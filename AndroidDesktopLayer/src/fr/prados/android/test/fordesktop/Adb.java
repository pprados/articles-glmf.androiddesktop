package fr.prados.android.test.fordesktop;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import com.android.ddmlib.AdbCommandRejectedException;
import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.ShellCommandUnresponsiveException;
import com.android.ddmlib.TimeoutException;
import com.android.sdklib.SdkConstants;

// Inspired by com.android.chimpchat.adb.AdbBackend
public class Adb
{
    private static Logger LOG = Logger.getLogger(Adb.class.getCanonicalName());
    private static final int CONNECTION_ITERATION_TIMEOUT_MS = 200;
    private final List<AdbDeviceImpl> devices = new ArrayList<AdbDeviceImpl>();
    
	AndroidDebugBridge mBridge;

	public Adb()
	{
		// [try to] ensure ADB is running
		String adbLocation = findAdb();

		AndroidDebugBridge.init(false /* debugger support */);

		mBridge = AndroidDebugBridge.createBridge(
			adbLocation, true /* forceNewBridge */);

	}

	// @Override
	public AdbDevice waitForConnection(long timeoutMs, String deviceIdRegex) throws TimeoutException, AdbCommandRejectedException, IOException, ShellCommandUnresponsiveException
	{
		do
		{
			IDevice device = findAttachedDevice(deviceIdRegex);
			// Only return the device when it is online
			if (device != null && device.getState() == IDevice.DeviceState.ONLINE)
			{
				AdbDeviceImpl chimpDevice = new AdbDeviceImpl(device);
				devices.add(chimpDevice);
				return chimpDevice;
			}

			try
			{
				Thread.sleep(CONNECTION_ITERATION_TIMEOUT_MS);
			}
			catch (InterruptedException e)
			{
				LOG.log(Level.SEVERE, "Error sleeping", e);
			}
			timeoutMs -= CONNECTION_ITERATION_TIMEOUT_MS;
		} while (timeoutMs > 0);

		// Timeout. Give up.
		return null;
	}
	public String[] getDevices()
	{
		IDevice[] devices=mBridge.getDevices();
		String[] devicesNames=new String[devices.length];
		for (int i=0;i<devices.length;++i)
		{
			devicesNames[i]=devices[i].getSerialNumber();
		}
		return devicesNames;
	}
//	@Override
	public void shutdown()
	{
		for (AdbDeviceImpl device : devices)
		{
			device.dispose();
		}
		AndroidDebugBridge.terminate();
	}

	private String findAdb()
	{
		String mrParentLocation = System.getProperty("fr.prados.usbinvoke.bindir"); //$NON-NLS-1$
		if (mrParentLocation==null)
		{
			String n="/"+IDevice.class.getName().replace('.', '/')+".class";
			URL url=IDevice.class.getResource(n);
			String f=url.getPath();
			if (f.startsWith("file:"))
			{
				mrParentLocation=new File(f.substring(5,f.lastIndexOf('!'))).getParentFile().getParent();
			}
		}
		
		// in the new SDK, adb is in the platform-tools, but when run from the
		// command line
		// in the Android source tree, then adb is next to monkeyrunner.
		if (mrParentLocation != null && mrParentLocation.length() != 0)
		{
			// check if there's a platform-tools folder
			File platformTools = new File(new File(mrParentLocation).getParent(), SdkConstants.FD_PLATFORM_TOOLS);
			if (platformTools.isDirectory())
			{
				return platformTools.getAbsolutePath() + File.separator + SdkConstants.FN_ADB;
			}

			return mrParentLocation + File.separator + SdkConstants.FN_ADB;
		}
		LOG.log(Level.SEVERE,"Can not detect adb process. Use -Dfr.prados.usbinvoke.bindir=<sdk> or or the ddms.jar from sdk");
		return SdkConstants.FN_ADB;
	}

	/**
	 * Checks the attached devices looking for one whose device id matches the
	 * specified regex.
	 * 
	 * @param deviceIdRegex the regular expression to match against
	 * @return the Device (if found), or null (if not found).
	 */
	private IDevice findAttachedDevice(String deviceIdRegex)
	{
		Pattern pattern = Pattern.compile(deviceIdRegex);
		for (IDevice device : mBridge.getDevices())
		{
			String serialNumber = device.getSerialNumber();
			if (pattern.matcher(
				serialNumber).matches())
			{
				return device;
			}
		}
		return null;
	}

}
