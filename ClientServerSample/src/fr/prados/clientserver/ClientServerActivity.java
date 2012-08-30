package fr.prados.clientserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;

public class ClientServerActivity extends Activity
{
	public Server mServer;

	EditText mEditIp;

	TextView mStatus;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		ToggleButton toogleServer = (ToggleButton) findViewById(R.id.toggleServer);
		toogleServer.setOnCheckedChangeListener(new ToggleButton.OnCheckedChangeListener()
		{

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
			{
				if (isChecked)
				{
					if (mServer == null)
					{
						try
						{
							mServer = new Server();
							mServer.start();
							mStatus.setText(R.string.srv_started);
						}
						catch (IOException e)
						{
							e.printStackTrace();
						}
					}
				}
				else
				{
					if (mServer != null)
					{
						mServer.stopSrv();
						mServer = null;
						mStatus.setText(R.string.srv_stoped);
					}
				}
			}
		});
		Button connect = (Button) findViewById(R.id.connect);
		connect.setOnClickListener(new Button.OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				new AsyncTask<String, Void, String>()
				{
					@Override
					protected String doInBackground(String... params)
					{
						String ip = params[0];
						Socket socket = null;
						try
						{
							socket = new Socket(ip, Server.PORT);
							BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
							PrintWriter output = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
							output.println(Build.MODEL);
							output.flush();
							return input.readLine();
						}
						catch (Exception e)
						{
							return e.getMessage();
						}
						finally
						{
							if (socket != null)
							{
								try
								{
									socket.close();
								}
								catch (IOException e)
								{
									e.printStackTrace();
								}
							}
						}
					}

					@Override
					protected void onPostExecute(String result)
					{
						mStatus.setText(result);
					}
				}.execute(mEditIp.getText().toString());
			}
		});
		mEditIp = (EditText) findViewById(R.id.ip);
		mStatus = (TextView) findViewById(R.id.status);
		mStatus.setText("");
	}

	@Override
	public ClientServerApplication getApplicationContext()
	{
		return (ClientServerApplication)super.getApplicationContext();
	}
	
}