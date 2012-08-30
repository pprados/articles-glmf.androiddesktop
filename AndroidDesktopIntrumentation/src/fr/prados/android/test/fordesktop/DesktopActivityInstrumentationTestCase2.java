package fr.prados.android.test.fordesktop;

import android.app.Activity;
import android.os.Bundle;
import android.test.ActivityInstrumentationTestCase2;

public abstract class DesktopActivityInstrumentationTestCase2<T extends Activity> 
	extends ActivityInstrumentationTestCase2<T>
{
	public DesktopActivityInstrumentationTestCase2(Class<T> activityClass)
	{
		super(activityClass);
	}
	@Deprecated
    public DesktopActivityInstrumentationTestCase2(String pkg, Class<T> activityClass)
	{
		super(pkg,activityClass);
	}
	public DesktopInstrumentationTestRunner getInstrumentation()
	{
		return (DesktopInstrumentationTestRunner)super.getInstrumentation();
	}
	protected Bundle getBundle()
	{
	    return getInstrumentation().getBundle();
	}
	protected void pauseTest()
	{
		getInstrumentation().pauseTest();
	}

}
