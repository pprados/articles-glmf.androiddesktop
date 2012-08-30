package fr.prados.android.test.fordesktop;


import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import android.os.Bundle;
import android.util.Log;

public class DesktopInstrumentationTestRunner 
extends android.test.InstrumentationTestRunner

{
	public static final String TAG="Instrument";
	private Bundle mArguments;
	private Server mServer;

	public DesktopInstrumentationTestRunner()
	{
		mServer=new Server();
		mServer.start();
	}

	public class Server extends Thread
	{
		
		@Override
		public void run()
		{
			ServerSocket srvSocket;
			try
			{
				Log.i(TAG,"Start server on port 1088)");
				srvSocket=new ServerSocket(1088,0,InetAddress.getLocalHost()); // Limited to localhost (usb)
				ObjectInputStream input;
				ObjectOutputStream output;
				
				for (;;)
				{
					try
					{
						Socket clientSocket=srvSocket.accept();
						Log.i(TAG,"Server accept client)");
						input = new ObjectInputStream(clientSocket.getInputStream());
						output = new ObjectOutputStream(clientSocket.getOutputStream());
						for (;;)
						{
							Log.i(TAG,"Execute...");
							execute(input,output);
						}
					}
					catch (EOFException e)
					{
						// Ignore
					}
					catch (IOException e)
					{
						Log.e(TAG,"Exception",e);
					}
					catch (ClassNotFoundException e)
					{
						Log.e(TAG,"Exception",e);
					}
					
				}
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		private void execute(ObjectInputStream input,ObjectOutputStream output) throws OptionalDataException, ClassNotFoundException, IOException
		{
			Object instance=null;
			Request request=(Request)input.readObject();
			Log.i(TAG,"execute "+request.name+"(...)");
			int index=request.name.lastIndexOf('.');
			String classname;
			if (index==-1)
			{
				classname=DesktopInstrumentationTestRunner.this.getClass().getName();
				instance=DesktopInstrumentationTestRunner.this;
			}
			else
				classname=request.name.substring(0,index);
			String methodname=request.name.substring(index+1);
			Method[] methods=Class.forName(classname).getMethods();
			for (int i=0;i<methods.length;++i)
			{
				if (methods[i].getName().equals(methodname))
				{
					Response response=new Response();
					try
					{
						response.result=methods[i].invoke(instance, request.args);
						Log.v(TAG,"return "+response.result);
					}
					catch (Throwable e)
					{
						response.exception=e;
						Log.v(TAG,"exception "+response.result);
					}
					output.writeObject(response);
					return;
				}
			}
		}
	}
	
	public synchronized void continueTest()
	{
		notify();
	}
	public synchronized void pauseTest()
	{
		try
		{
			Log.d(TAG,"Pause test...");
			wait();
			Log.d(TAG,"Continue test.");
		}
		catch (InterruptedException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public Bundle getBundle()
	{
		return mArguments;
	}
	@Override
	public void onCreate(Bundle arguments)
	{
		super.onCreate(arguments);
		Log.d("Instrument","onCreate");
		mArguments=arguments;
	}
}
