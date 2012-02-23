package fr.emse.tsc;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

/**
 * @author Philippe Lalevée
 *
 */
public class DemoTSC extends Activity {
	
	public final static String KEY_LSERVER = "LSERVER";
	public final static String KEY_LPORT ="LPORT";
	public final static String KEY_LNAME = "LNAME";
	public final static String KEY_WSERVER = "WSERVER";
	public final static String KEY_WPORT ="WPORT";
	public final static String KEY_WNAME = "WNAME";

	private static final int MENU_CONFIG = Menu.FIRST;
	private static final int MENU_ABOUT = Menu.FIRST+2;
	private static final int MENU_QUIT = Menu.FIRST+3;
	private static final int DIALOG_QUIT = 0;
	
    private OnClickListener mLenListener = new OnClickListener() {
        public void onClick(View v) {
            Intent intent = new Intent(DemoTSC.this, Enroll.class);
            startActivity(intent);
        }
    };
    
    private OnClickListener mWenListener = new OnClickListener() {
        public void onClick(View v) {
        	if (Enroll.token == null || Enroll.token.getResponse().equals(Token.KO)) {
        		Toast toast = Toast.makeText(DemoTSC.this, "You must be enrolled ", Toast.LENGTH_LONG);
        		toast.setGravity(Gravity.CENTER, 0, 0);
        		toast.show();
        	}
        	else {
        		Intent intent = new Intent(DemoTSC.this, Access.class);
        		startActivity(intent);        		
        	}       	
        }
    };
    
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    	final Button bLen = (Button) findViewById(R.id.bLen);
        bLen.setOnClickListener(mLenListener);
    	final Button bWen = (Button) findViewById(R.id.bWen);
        bWen.setOnClickListener(mWenListener);
    }

	@Override
	protected Dialog onCreateDialog(int id) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("Are you sure you want to quit?")
			.setCancelable(false)
			.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					finish();
				}
			})
			.setNegativeButton("No", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					dialog.cancel();
				}});
		return builder.create();			
	}
		
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, MENU_CONFIG, 0, "Config")
        	.setShortcut('0', 'c')
        	.setIcon(android.R.drawable.ic_menu_preferences);
        menu.add(0, MENU_ABOUT, 0, "About")
        	.setShortcut('0', 'a')
        	.setIcon(android.R.drawable.ic_menu_info_details);
        menu.add(0, MENU_QUIT, 0, "Quit")
        	.setShortcut('0', 'q')
        	.setIcon(android.R.drawable.ic_menu_close_clear_cancel);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	Intent intent;
    	switch (item.getItemId()) {
    	case MENU_CONFIG:
        	intent = new Intent(this, Config.class);
        	startActivity(intent);
        	break;
        case MENU_ABOUT:
        	intent = new Intent(this, About.class);
        	startActivity(intent);
            break;
        case MENU_QUIT:
            showDialog(DIALOG_QUIT);
            break;
        }
        return super.onOptionsItemSelected(item);
    }
}