package de.ozhan.burak.android.vergissmeinnicht.gui;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ToggleButton;
import de.ozhan.burak.android.test4.R;

public class ViewTwoActivity extends Activity{

	ToggleButton tb;
	TextView tv, tvs, tvc;
	ProgressBar pgrb;
	private static BluetoothAdapter mBluetoothAdapter ;
	boolean bluetoothstatus;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.viewtwo);
		
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		bluetoothstatus = mBluetoothAdapter.isEnabled();
		
		IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
		registerReceiver(btStateChangeReceiver, filter);
		
		tvs = (TextView) findViewById(R.id.textViewStatus);
		tvc = (TextView) findViewById(R.id.textViewSettingsStatus);
		
		if (bluetoothstatus) {
			tvs.setText(getString(R.string.viewtwo_bt_ison));
		}else{
			tvs.setText(getString(R.string.viewtwo_bt_isoff));
		}
		
		tv = (TextView) findViewById(R.id.textView1);
		tv.setText(Html.fromHtml(getString(R.string.viewtwo_introduction)));
	
		tb = (ToggleButton) findViewById(R.id.toggleButton1);
		tb.setChecked(bluetoothstatus);
		tb.setOnClickListener(toggleHandler);
		
		pgrb = (ProgressBar) findViewById(R.id.progressBar2);
		
		if (bluetoothstatus) continueToNextActivity(this, true);

	}
	
	@Override
	protected void onDestroy(){
		super.onDestroy();
		unregisterReceiver(btStateChangeReceiver);
	}

	OnClickListener toggleHandler = new OnClickListener() {
		@Override
		public void onClick(View v) {
			boolean stat = ((ToggleButton)v).isChecked();
			Log.d("Pressed and stat was :",""+stat);
			if (stat) {
				Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivityForResult(enableBtIntent, 0);
			}
			if (!stat) {
				mBluetoothAdapter.disable();  
			}
		}
	};
	
	BroadcastReceiver btStateChangeReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
				final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
				Log.v("BT_STATE_CHANGED", "Extra state : "+state);
				switch (state) {
				case BluetoothAdapter.STATE_OFF:
					Log.v("BluetoothHandler","Status : BT is OFF");
					bluetoothstatus = false;
					pgrb.setVisibility(View.GONE);
					tvs.setText(getString(R.string.viewtwo_bt_isoff));
					break;
				case BluetoothAdapter.STATE_ON:
					Log.v("BluetoothHandler","Status : BT is ON");
					bluetoothstatus = true;
					tvs.setText(getString(R.string.viewtwo_bt_ison));
					pgrb.setVisibility(View.GONE);
					continueToNextActivity(context, false);
					break;
				case BluetoothAdapter.STATE_TURNING_OFF:
					Log.v("BluetoothHandler","Turning BT off");
					bluetoothstatus = false;
					tvs.setText(getString(R.string.viewtwo_bt_turningoff));
					tvc.setText("");
					pgrb.setVisibility(View.VISIBLE);
					break;
				case BluetoothAdapter.STATE_TURNING_ON:
					Log.v("BluetoothHandler","Turning BT on");
					tvs.setText(getString(R.string.viewtwo_bt_turningon));
					pgrb.setVisibility(View.VISIBLE);
					break;
				}
			}
		}
	};
	
	private void continueToNextActivity(final Context context, final boolean wasAlreadyActive) {
		final Handler handler = new Handler();
		
		final Runnable startNextActivity = new Runnable(){
			@Override
			public void run() {
				Intent myIntent = new Intent(context, ViewThreeActivity.class);
				startActivity(myIntent);
			}
		};
		Runnable showAndWait = new Runnable(){
			@Override
			public void run() {
				tvc.setText(getString((wasAlreadyActive)?R.string.viewtwo_wasalready:R.string.viewtwo_success));
				handler.postDelayed(startNextActivity, 1500);
			}
		};
		handler.postDelayed(showAndWait, (wasAlreadyActive)?600:400);
	}
}