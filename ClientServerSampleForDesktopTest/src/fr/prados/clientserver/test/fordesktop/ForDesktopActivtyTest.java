package fr.prados.clientserver.test.fordesktop;

import android.util.Log;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;
import fr.prados.android.test.fordesktop.DesktopActivityInstrumentationTestCase2;
import fr.prados.clientserver.ClientServerActivity;
import fr.prados.clientserver.R;


public class ForDesktopActivtyTest extends DesktopActivityInstrumentationTestCase2<ClientServerActivity>
{
	private static final String TAG="TEST";
	
	ClientServerActivity mActivity;
	ToggleButton mToggleServer;
	Button mConnect;
	EditText mEditIp;
	TextView mStatus;

	public ForDesktopActivtyTest()
	{
		super("fr.prados.usbinvoke.sample", ClientServerActivity.class);
	}
	@Override
	protected void setUp() throws Exception
	{
		super.setUp();
		setActivityInitialTouchMode(false);
		mActivity=getActivity();
		mToggleServer=(ToggleButton)mActivity.findViewById(R.id.toggleServer);
		mConnect=(Button)mActivity.findViewById(R.id.connect);
		mEditIp=(EditText)mActivity.findViewById(R.id.ip);
        mStatus=(TextView)mActivity.findViewById(R.id.status);
	}
	public void testPreConditions() 
	{
	    assertFalse(mToggleServer.isChecked());
	} // end of testPreConditions() method definition

	public void testStartServer()
	{
		// Start server
		mActivity.runOnUiThread(
		      new Runnable() 
		      {
		        @Override
				public void run() 
		        {
		        	mToggleServer.requestFocus();
		        }
		      }
		    );		
		getInstrumentation().waitForIdleSync();
		sendKeys(KeyEvent.KEYCODE_ENTER);
		getInstrumentation().waitForIdleSync();
		assertNotNull(mActivity.mServer);
		assertTrue(mActivity.mServer.isAlive());
		
		Log.d(TAG,"wait next notification");
		pauseTest();
		Log.d(TAG,"receive notification");
		
		// Stop server
		getInstrumentation().waitForIdleSync();
		sendKeys(KeyEvent.KEYCODE_ENTER);
		try { Thread.sleep(1000); } catch (Exception e){}
		assertFalse(mActivity.mServer.isAlive());
		assertNull(mActivity.mServer);
	}
	public void testConnect()
	{
		final String ip;
		ip=getBundle().getString("target");
		mActivity.runOnUiThread(
		      new Runnable() 
		      {
		        @Override
				public void run() 
		        {
		          mEditIp.setText(ip);
		          mConnect.requestFocus();
		        }
		      }
		    );
		sendKeys(KeyEvent.KEYCODE_DPAD_CENTER);
		try { Thread.sleep(1000); } catch (Exception e) {}
		getInstrumentation().waitForIdleSync();
		assertTrue(mStatus.getText().toString().startsWith("Hello "));
	}
}
