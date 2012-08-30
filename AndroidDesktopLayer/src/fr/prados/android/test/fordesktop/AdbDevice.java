package fr.prados.android.test.fordesktop;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Map;

public interface AdbDevice
{
	public void dispose();
	public void connectApp() throws UnknownHostException, IOException;
	/**
	 * Invoke method in android application.
	 * 
	 * @param name classname.static_method_name
	 * @param params serialisable parameters
	 * @return serialisable result
	 * @throws IOException
	 * @throws Throwable
	 */
	public Object execute(String name, Object... params) throws IOException, Throwable;
	/**
	 * Return property.
	 * 
	 * @param key
	 * @return Property.
	 */
	public String getProperty(String key);
	/**
	 * Return list of properties.
	 * 
	 * @return Collection of properties.
	 */
	public Collection<String> getPropertyList();
	/**
	 * Return system property.
	 * 
	 * @param key
	 * @return System property.
	 */
	public String getSystemProperty(String key);
	/**
	 * Install package.
	 * 
	 * @param path Path of the apk file.
	 * @return true if installed.
	 */
	boolean installPackage(String path);
	/**
	 * Remove package.
	 * 
	 * @param packageName Package name.
	 * @return true if uninstalled.
	 */
	public boolean removePackage(String packageName);
	/**
	 * Execute shell command.
	 * 
	 * @param cmd The command.
	 * @return The system out in a string.
	 */
	public String shell(String cmd);
	
	/**
	 * Instrument a package.
	 * 
	 * @param packageName The package.
	 * 
	 * @return The instrument result.
	 */
    public Map<String, Object> instrument(String packageName);
	/**
	 * Instrument a package.
	 * 
	 * @param packageName The package.
	 * @param args The arguments present in the bundle on onCreate method.
	 * 
	 * @return The instrument result.
	 */
    public Map<String, Object> instrument(String packageName, Map<String, String> args);
    
	/**
	 * Instrument a package.
	 * 
	 * @param packageName The package.
	 * @param filter filter
	 * @param args The arguments present in the bundle on onCreate method.
	 * 
	 * @return The instrument result.
	 */
    public Map<String, Object> instrument(String filter,String packageName, Map<String, String> args);

    /**
     * Wake the device.
     * @throws IOException
     */
    public void wake() throws IOException;

    /**
     * Continue the remote test in pause state.
     * @throws IOException
     * @throws Throwable
     */
    public void continueTest() throws IOException, Throwable;
}
