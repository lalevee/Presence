package fr.emse.tsc;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

public class Enroll extends Activity {

	private static final int DIALOG_QUIT = 0;
	
	private ObjectInputStream in = null;
	private ObjectOutputStream out = null;

	private Socket socket = null;
	private String addressIP = null;
	private int port = 0;
	private String lname = null;

	static Token token = null;

	void showToast(String message) {
		Toast toast = Toast.makeText(this, "Enroll: " + message, Toast.LENGTH_LONG);
		toast.setGravity(Gravity.CENTER, 0, 0);
		toast.show();
	}
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.tsc_len);
		addressIP = Config.getLServer(this);
		port = Integer.parseInt(Config.getLPort(this));
		lname = Config.getLName(this);
		Log.i("ENROLL", "On Create: " + addressIP);
		connect();
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		AlertDialog alert = null;
		switch (id) {
		case DIALOG_QUIT: 
			builder.setMessage("Are you sure you want to quit?")
			.setCancelable(false)
			.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					disconnect();
					finish();
				}
			})
			.setNegativeButton("No", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					dialog.cancel();
				}
			});
			break;
		}
		alert = builder.create();
		return alert;			
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
        	showDialog(DIALOG_QUIT);
        	return true;
        }
        return false;
    }

	void connect() {
		showToast(lname + " to " + addressIP);
		try {
			InetAddress serverAddr = InetAddress.getByName(addressIP);
			socket = new Socket(serverAddr, port);
			out = new ObjectOutputStream(socket.getOutputStream());
			in = new ObjectInputStream(socket.getInputStream());

			WifiManager wm = (WifiManager) this.getSystemService(WIFI_SERVICE);
			WifiInfo wi = wm.getConnectionInfo();

			token = new Token(lname, "", wi.getMacAddress(), "");
			token.setCommand(Token.ENROLL);

			out.writeObject(token);
			out.flush();

			token = (Token) in.readObject();
			if (token.getResponse().equals(Token.KO)) {
				Log.d("TSC", "Enroll: server says KO");
				showToast("server says KO");
				disconnect();
			}
			Log.i("TSC", "Enroll: server says OK");			
			token.setCommand(Token.QUIT);
			out.writeObject(token);
			disconnect();
			finish();
		}
		catch (Exception e) {
			showToast("Exception " + e.getMessage());
			if (socket != null) {
				try {
					if (in != null) in.close();
					if (out != null) out.close();
					socket.close();
				}
				catch (IOException e1) { }
				finally {
					in = null;
					out = null;
					socket = null;
				}
			}
		}
	}

	void disconnect() {
		if (socket != null) {
			try {
				token.setCommand(Token.QUIT);
				out.writeObject(token);
				out.flush();
				in.close();
				out.close();
				socket.close();
			}
			catch (IOException e) {
				showToast("Exception " + e.getMessage());
			}
			finally {
				socket = null;
				in = null;
				out = null;
			}
		}
	}

}
