package fr.prados.android.test.fordesktop;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.android.ddmlib.AdbCommandRejectedException;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.InstallException;
import com.android.ddmlib.ShellCommandUnresponsiveException;
import com.android.ddmlib.TimeoutException;

// Inspired by com.android.chimpchat.adb.AdbChimpDevice
public class AdbDeviceImpl implements AdbDevice
{
	private static final int DEST_PORT = 1088;
	private static final long TIMEOUT_WAIT_MONKEY=2000L;
    
	private static int sBasePort = 12520;

	private static final Logger LOG = Logger.getLogger(AdbDeviceImpl.class.getName());

	private static final long MANAGER_CREATE_TIMEOUT_MS = 30 * 1000; // 30 seconds

	private static final long MANAGER_CREATE_WAIT_TIME_MS = 1000; // wait 1 second

	private final ExecutorService executor = Executors.newSingleThreadExecutor();

	private final IDevice mDevice;

	private Socket mMonkeySocket;

	private BufferedWriter mMonkeyWriter;

	private BufferedReader mMonkeyReader;

	private final int mLocalPort;

	private Socket mRemoteAppSocket;

	private ObjectOutputStream mRemoteAppOutput;

	private ObjectInputStream mRemoteAppInput;

	public AdbDeviceImpl(IDevice device) throws TimeoutException, AdbCommandRejectedException, IOException, ShellCommandUnresponsiveException
	{
		mDevice = device;
		mLocalPort = sBasePort++;
		init("127.0.0.1", mLocalPort+1);
		mDevice.createForward(mLocalPort, DEST_PORT);
	}

	@Override
	public void dispose()
	{
		// this command drops the connection, so handle it here
		synchronized (this)
		{
			try
			{
				sendMonkeyEventAndGetResponse("quit");
			}
			catch (SocketException e)
			{
				// flush was called after the call had been written, so it tried
				// flushing to a
				// broken pipe.
			}
			catch (IOException e)
			{
				// TODO Auto-generated catch block
			}
		}
		if (mRemoteAppSocket!=null)
		{
			closeAppSocket();
			
			try
			{
				mMonkeySocket.close();
				mMonkeyReader.close();
				mMonkeyWriter.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			mMonkeySocket=null;
			mMonkeyReader=null;
			mMonkeyWriter=null;
		}
		executor.shutdown();
	}

	private void closeAppSocket()
	{
		if (mRemoteAppInput!=null)
		{
			try
			{
				mRemoteAppInput.close();
				mRemoteAppOutput.close();
				mRemoteAppSocket.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			mRemoteAppSocket=null;
			mRemoteAppOutput=null;
			mRemoteAppInput=null;
		}
	}

	private void init(String address, int port) throws IOException, TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException
	{
		mDevice.createForward(
			port, port);

		String command = "monkey --port " + port;
		executeAsyncCommand(
			command, new LoggingOutputReceiver(LOG, Level.FINE));

		// Sleep for a second to give the command time to execute.
		try
		{
			Thread.sleep(TIMEOUT_WAIT_MONKEY);
		}
		catch (InterruptedException e)
		{
			LOG.log(
				Level.SEVERE, "Unable to sleep", e);
		}

		InetAddress addr = InetAddress.getByName(address);

		// We have a tough problem to solve here. "monkey" on the device gives
		// us no indication
		// when it has started up and is ready to serve traffic. If you try too
		// soon, commands
		// will fail. To remedy this, we will keep trying until a single command
		// (in this case,
		// wake) succeeds.
		boolean success = false;
		long start = System.currentTimeMillis();

		while (!success)
		{
			long now = System.currentTimeMillis();
			long diff = now - start;
			if (diff > MANAGER_CREATE_TIMEOUT_MS)
			{
				LOG.severe("Timeout while trying to create chimp mananger");
				return;
			}

			try
			{
				Thread.sleep(MANAGER_CREATE_WAIT_TIME_MS);
			}
			catch (InterruptedException e)
			{
				LOG.log(
					Level.SEVERE, "Unable to sleep", e);
			}

			try
			{
				mMonkeySocket = new Socket(addr, port);
				mMonkeyWriter = new BufferedWriter(new OutputStreamWriter(mMonkeySocket.getOutputStream()));
				mMonkeyReader = new BufferedReader(new InputStreamReader(mMonkeySocket.getInputStream()));

			}
			catch (IOException e)
			{
				LOG.log(
					Level.FINE, "Unable to connect socket", e);
				success = false;
				continue;
			}

			try
			{
				wake();
			}
			catch (IOException e)
			{
				LOG.log(
					Level.FINE, "Unable to wake up device", e);
				success = false;
				continue;
			}
			success = true;
		}
		if (!success)
			throw new IOException("Impossible to connect");
	}

	/**
	 * Wake the device up from sleep.
	 * 
	 * @throws IOException on error communicating with the device
	 */
	@Override
	public void wake() throws IOException
	{
		sendMonkeyEvent("wake");
	}

	@Override
	public void continueTest() throws IOException, Throwable
	{
		execute("continueTest");
	}
	@Override
	public boolean installPackage(String path)
	{
		try
		{
			String result = mDevice.installPackage(
				path, true);
			if (result != null)
			{
				LOG.log(
					Level.SEVERE, "Got error installing package: " + result);
				return false;
			}
			return true;
		}
		catch (InstallException e)
		{
			LOG.log(
				Level.SEVERE, "Error installing package: " + path, e);
			return false;
		}
	}

	@Override
	public boolean removePackage(String packageName)
	{
		try
		{
			String result = mDevice.uninstallPackage(packageName);
			if (result != null)
			{
				LOG.log(
					Level.SEVERE, "Got error uninstalling package " + packageName + ": " + result);
				return false;
			}
			return true;
		}
		catch (InstallException e)
		{
			LOG.log(
				Level.SEVERE, "Error installing package: " + packageName, e);
			return false;
		}
	}

	@Override
	public String getSystemProperty(String key)
	{
		return mDevice.getProperty(key);
	}

	@Override
	public String getProperty(String key)
	{
		try
		{
			synchronized (this)
			{
				String response = sendMonkeyEventAndGetResponse("getvar " + key);
				if (!parseResponseForSuccess(response))
				{
					return null;
				}
				return parseResponseForExtra(response);
			}
		}
		catch (IOException e)
		{
			LOG.log(
				Level.SEVERE, "Unable to get variable: " + key, e);
			return null;
		}
	}

	@Override
	public Collection<String> getPropertyList()
	{
		try
		{
			synchronized (this)
			{
				String response = sendMonkeyEventAndGetResponse("listvar");
				if (!parseResponseForSuccess(response))
				{
					Collections.emptyList();
				}
				String extras = parseResponseForExtra(response);
				String[] split = extras.split(" ");
				ArrayList<String> list = new ArrayList<String>(split.length);
				Collections.addAll(
					list, split);
				return list;
			}
		}
		catch (IOException e)
		{
			LOG.log(
				Level.SEVERE, "Unable to get variable list", e);
			return null;
		}
	}

	/**
	 * Parse a monkey response string to get the extra data returned.
	 * 
	 * @param monkeyResponse the response
	 * @return any extra data that was returned, or empty string if there was
	 *         nothing.
	 */
	private String parseResponseForExtra(String monkeyResponse)
	{
		int offset = monkeyResponse.indexOf(':');
		if (offset < 0)
		{
			return "";
		}
		return monkeyResponse.substring(offset + 1);
	}

//	private void executeSyncCommand(final String command, final LoggingOutputReceiver logger) throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException
//	{
//		mDevice.executeShellCommand(command, logger);
//	}
	private void executeAsyncCommand(final String command, final LoggingOutputReceiver logger)
	{
		executor.submit(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					mDevice.executeShellCommand(
						command, logger);
				}
				catch (TimeoutException e)
				{
					LOG.log(
						Level.SEVERE, "Error starting command: " + command, e);
					throw new RuntimeException(e);
				}
				catch (AdbCommandRejectedException e)
				{
					LOG.log(
						Level.SEVERE, "Error starting command: " + command, e);
					throw new RuntimeException(e);
				}
				catch (ShellCommandUnresponsiveException e)
				{
					// This happens a lot
					LOG.log(
						Level.INFO, "Error starting command: " + command, e);
					throw new RuntimeException(e);
				}
				catch (IOException e)
				{
					LOG.log(
						Level.SEVERE, "Error starting command: " + command, e);
					throw new RuntimeException(e);
				}
			}
		});
	}

	/**
	 * This function allows the communication bridge between the host and the
	 * device to be invisible to the script for internal needs. It splits a
	 * command into monkey events and waits for responses for each over an adb
	 * tcp socket.
	 * 
	 * @param command the monkey command to send to the device
	 * @return true on success.
	 * @throws IOException on error communicating with the device
	 */
	private boolean sendMonkeyEvent(String command) throws IOException
	{
		synchronized (this)
		{
			String monkeyResponse = sendMonkeyEventAndGetResponse(command);
			return parseResponseForSuccess(monkeyResponse);
		}
	}

	/**
	 * Parse a monkey response string to see if the command succeeded or not.
	 * 
	 * @param monkeyResponse the response
	 * @return true if response code indicated success.
	 */
	private boolean parseResponseForSuccess(String monkeyResponse)
	{
		if (monkeyResponse == null)
		{
			return false;
		}
		// return on ok
		if (monkeyResponse.startsWith("OK"))
		{
			return true;
		}

		return false;
	}

	/**
	 * This function allows the communication bridge between the host and the
	 * device to be invisible to the script for internal needs. It splits a
	 * command into monkey events and waits for responses for each over an adb
	 * tcp socket. Returns on an error, else continues and sets up last
	 * response.
	 * 
	 * @param command the monkey command to send to the device
	 * @return the (unparsed) response returned from the monkey.
	 * @throws IOException on error communicating with the device
	 */
	private String sendMonkeyEventAndGetResponse(String command) throws IOException
	{
		command = command.trim();
		LOG.info("Monkey Command: " + command + ".");

		// send a single command and get the response
		mMonkeyWriter.write(command + "\n");
		mMonkeyWriter.flush();
		return mMonkeyReader.readLine();
	}

	@Override
	public void connectApp() throws UnknownHostException, IOException
	{
		mRemoteAppSocket = new Socket("127.0.0.1", mLocalPort);
		mRemoteAppOutput = new ObjectOutputStream(mRemoteAppSocket.getOutputStream());
		mRemoteAppInput = new ObjectInputStream(mRemoteAppSocket.getInputStream());
	}

	@Override
	public String shell(String cmd)
	{
		CommandOutputCapture capture = new CommandOutputCapture();
		try
		{
			mDevice.executeShellCommand(
				cmd, capture);
		}
		catch (TimeoutException e)
		{
			LOG.log(
				Level.SEVERE, "Error executing command: " + cmd, e);
			return null;
		}
		catch (ShellCommandUnresponsiveException e)
		{
			LOG.log(
				Level.SEVERE, "Error executing command: " + cmd, e);
			return null;
		}
		catch (AdbCommandRejectedException e)
		{
			LOG.log(
				Level.SEVERE, "Error executing command: " + cmd, e);
			return null;
		}
		catch (IOException e)
		{
			LOG.log(
				Level.SEVERE, "Error executing command: " + cmd, e);
			return null;
		}
		return capture.toString();
	}

	@Override
	public Object execute(String name, Object... params) throws IOException, Throwable
	{
		try
		{
			if (mRemoteAppOutput==null)
				throw new IllegalArgumentException("Connect to application before invoke methods");
			Request request = new Request();
			request.name = name;
			request.args = params;
			mRemoteAppOutput.writeObject(request);
			mRemoteAppOutput.flush();
			Response response = (Response) mRemoteAppInput.readObject();
			if (response.exception == null)
			{
				return response.result;
			}
			else
				throw response.exception;
		}
		catch (ClassNotFoundException e)
		{
			LOG.log(
				Level.SEVERE, "Error execute remote method", e);
			throw new Error(e);
		}
	}
	
    @Override
    public Map<String, Object> instrument(String packageName)
    {
    	return instrument("",packageName,null);
    }
    @Override
    public Map<String, Object> instrument(String packageName, Map<String, String> args)
    {
    	return instrument("",packageName,args); // FIXME: filter util ?
    }
    @Override
    public Map<String, Object> instrument(String filter,String packageName, Map<String, String> args) 
    {
    	closeAppSocket();
    	StringBuilder builder=new StringBuilder("am instrument -w -r ");
    	if (isDebug()) builder.append("-e debug true ");
    	//args.entrySet()
    	if (args!=null)
    	{
	    	for (Map.Entry<String, String> entry:args.entrySet())
	    	{
	    		builder.append("-e ").append(entry.getKey()).append(" ").append(entry.getValue());
	    	}
    	}
    	builder.append(" ").append(packageName);
    	String result=shell(builder.toString());
        return convertInstrumentResult(result);
    }
	protected boolean isDebug()
	{ 
		return java.lang.management.ManagementFactory.getRuntimeMXBean().
			    getInputArguments().toString().indexOf("-agentlib:jdwp") > 0;		
	}

    /**
     * Convert the instrumentation result into it's Map representation.
     *
     * @param result the result string
     * @return the new map
     */
    private static Map<String, Object> convertInstrumentResult(String result) {
    	Map<String, Object> map = new HashMap<String,Object>();
    	if (result==null)
            return map;
        Pattern pattern = Pattern.compile("^INSTRUMENTATION_(\\w+): ", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(result);

        int previousEnd = 0;
        String previousWhich = null;

        while (matcher.find()) {
            if ("RESULT".equals(previousWhich)) {
                String resultLine = result.substring(previousEnd, matcher.start()).trim();
                // Look for the = in the value, and split there
                int splitIndex = resultLine.indexOf("=");
                String key = resultLine.substring(0, splitIndex);
                String value = resultLine.substring(splitIndex + 1);

                map.put(key, value);
            }

            previousEnd = matcher.end();
            previousWhich = matcher.group(1);
        }
        if ("RESULT".equals(previousWhich)) {
            String resultLine = result.substring(previousEnd, matcher.start()).trim();
            // Look for the = in the value, and split there
            int splitIndex = resultLine.indexOf("=");
            String key = resultLine.substring(0, splitIndex);
            String value = resultLine.substring(splitIndex + 1);

            map.put(key, value);
        }
        return map;
    }
	
}
