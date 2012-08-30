package fr.prados.clientserver.test.fordesktop;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import fr.prados.android.test.fordesktop.DesktopInstrumentationTestRunner;


// Here, you can add all public method can be invoked by the desktop to manage the instrumentation.
public class ClientServerDesktopInstrumentationTestRunner extends DesktopInstrumentationTestRunner
{
	public String getIp()
	{
		WifiManager wifiManager = (WifiManager) getContext().getSystemService(Context.WIFI_SERVICE);
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		int i = wifiInfo.getIpAddress();
		return 
		        ( i & 0xFF) + "." +
				((i >> 8 ) & 0xFF) + "." +
		        ((i >> 16 ) & 0xFF) + "." +
				((i >> 24 ) & 0xFF );
	}
}
