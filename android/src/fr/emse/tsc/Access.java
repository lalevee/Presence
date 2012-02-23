package fr.emse.tsc;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

public class Access extends Activity {
	static final int DIALOG_ERROR = 1;
	static final int DIALOG_PRESENCE = 2;

	private String adresseIP = null;
	private int port = 0;
	private String wname = null;
	private Socket socket = null;
	private String errorMessage = null;

	private ObjectInputStream in = null;
	private ObjectOutputStream out = null;

	private Token token = null;
	ProgressThread progressThread = null;
	ProgressDialog progressDialog = null;

	private boolean connected = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.tsc_wen);
		adresseIP = Config.getWServer(this);
		port = Integer.parseInt(Config.getWPort(this));
		wname = Config.getWName(this);
		Log.i("ACCESS", "On Create: " + adresseIP);
		connect();
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog alert = null;
		switch(id) {
		case DIALOG_PRESENCE:
			progressDialog = new ProgressDialog(this);
			progressDialog.setTitle("TSC Access");
			progressDialog.setCancelable(true);
			progressDialog.setIndeterminate(true);
			progressDialog.setMessage("Presence attesting...");
			progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {						
				public void onCancel(DialogInterface dialog) {
					disconnect();							
				}
			});
			alert = progressDialog;
			break;
		case DIALOG_ERROR:
			AlertDialog.Builder error = new AlertDialog.Builder(this);
			error.setMessage("Error: ")
			.setCancelable(false)
			.setPositiveButton("Continue", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					finish();
				}
			});
			alert = error.create();
			break;
		}
		return alert;
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		switch (id) {
		case DIALOG_ERROR:
			((AlertDialog) dialog).setMessage(errorMessage);
			break;
		}
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.lwen_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.mQuit :
			if (connected) disconnect();
			finish();
			return true;
		}
		return false;
	}

	private void connect() {
		try {
			adresseIP = Config.getWServer(this);
			port = Integer.parseInt(Config.getWPort(this));
			wname = Config.getWName(this);

			Toast toast = Toast.makeText(Access.this, "Connect " + wname + " to " + adresseIP, Toast.LENGTH_SHORT);
			toast.setGravity(Gravity.CENTER, 0, 0);
			toast.show();

			InetAddress serverAddr = InetAddress.getByName(adresseIP);
			socket = new Socket(serverAddr, port);
			out = new ObjectOutputStream(socket.getOutputStream());
			in = new ObjectInputStream(socket.getInputStream());
			Log.i("CONNECT", "Connected to: " + adresseIP);

			WifiManager wm = (WifiManager) this.getSystemService(WIFI_SERVICE);
			WifiInfo wi = wm.getConnectionInfo();

			token = new Token(Enroll.token.getName(), Enroll.token.getIpAddress(), wi.getMacAddress(), Enroll.token.getChallenge());
			token.setCommand(Token.ACCESS);
			Log.i("CONNECT", "Token sent: " + token.toString());

			out.writeObject(token);
			out.flush();

			token = (Token) in.readObject();
			if (token.getResponse().equals(Token.KO)) {
				errorMessage = "Connexion error";
				showDialog(DIALOG_ERROR);
			}
			Log.i("CONNECT", "Token received: " + token.toString());
			connected = true;
			progressThread = new ProgressThread(handler);
			progressThread.start();
			showDialog(DIALOG_PRESENCE);
		}
		catch (Exception e) {
			Log.i("CONNECT", "Exception: " + e.getMessage());
			errorMessage = e.getMessage();
			showDialog(DIALOG_ERROR);
		}
	}

	private void disconnect() {
		connected = false;
		if (socket != null) {
			try {
				token.setCommand(Token.QUIT);
				out.writeObject(token);
				out.flush();
				in.close();
				out.close();
				socket.close();
				finish();
			} catch (IOException e) {
				errorMessage = e.getMessage();
				showDialog(DIALOG_ERROR);
			}
			finally {
				socket = null;
				in = null;
				out = null;
			}
		}
	}

	final Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			String response = msg.getData().getString("response");
			Log.i("HANDLER", "Message recu " + response);
			if (response.equals(Token.KO)) {
				dismissDialog(DIALOG_PRESENCE);
				disconnect();
				progressThread.setState(ProgressThread.STATE_DONE);
			}
		}
	};

	private class ProgressThread extends Thread {
		Handler mHandler;
		final static int STATE_DONE = 0;
		final static int STATE_RUNNING = 1;
		int mState;

		public ProgressThread(Handler handler) {
			mHandler = handler;
		}

		public void run() {
			mState = STATE_RUNNING;   
			Log.i("THREAD", "in execution");
			while (mState == STATE_RUNNING) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					Log.e("ERROR", "Thread Interrupted");
				}
				try {
					out.writeObject(token);
					out.flush();
					token = (Token) in.readObject();
				} catch (Exception e) {
					token.setResponse(Token.KO);
				}			
				Message msg = mHandler.obtainMessage();
				Bundle b = new Bundle();
				b.putString("response", token.getResponse());
				msg.setData(b);
				mHandler.sendMessage(msg);

			}
		}

		/* sets the current state for the thread,
		 * used to stop the thread */
		public void setState(int state) {
			mState = state;
		}
	}
}

