package fr.prados.clientserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class Server extends Thread
{
	public static final int PORT=3000;
	private ServerSocket srvsocket;
	public Server() throws IOException
	{
	}
	@Override
	public void run()
	{
		try
		{
			srvsocket=new ServerSocket(PORT);
			for (;;)
			{
				try
				{
					Socket socket=srvsocket.accept();
					BufferedReader input=new BufferedReader(new InputStreamReader(socket.getInputStream()));
					PrintWriter output=new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
					output.println("Hello " +input.readLine());
					output.flush();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	public void stopSrv()
	{
		try
		{
			srvsocket.close();
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
