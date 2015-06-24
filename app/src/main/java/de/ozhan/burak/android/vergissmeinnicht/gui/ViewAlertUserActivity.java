package de.ozhan.burak.android.vergissmeinnicht.gui;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import de.ozhan.burak.android.test4.R;

public class ViewAlertUserActivity extends Activity{
	
	//GUI elements
	TextView tv1,tv2;
	Button mutebut;
	boolean manual;
	boolean lost;

	static boolean ringing;
	String address;
	BluetoothGatt mBluetoothGatt;
	NotificationManager mNotificationManager;
	Ringtone r;
	Handler h;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.viewalertuser);
		h = new Handler();
		
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			manual = extras.getBoolean("Manual");
			lost = extras.getBoolean("Lost");
			address = extras.getString("BSSID");
		}
		Log.wtf("alertuseractivity starts with","manual:"+manual+" linkloss:"+lost);
		
		tv1 = (TextView) findViewById(R.id.textView1);
		tv2 = (TextView) findViewById(R.id.textView2);
		
		if (manual){
			tv1.setText(Html.fromHtml(getString(R.string.viewalertuser_title_search)));
			tv2.setText(Html.fromHtml(getString(R.string.viewalertuser_introduction_search)));
		}
		if (lost) {
			tv1.setText(Html.fromHtml(getString(R.string.viewalertuser_title_linkloss)));
			tv2.setText(Html.fromHtml(getString(R.string.viewalertuser_introduction_linkloss)));
			NotificationCompat.Builder nb = new NotificationCompat.Builder(getApplicationContext());
			nb.setSmallIcon(R.drawable.ic_stat_notify_msg);
			nb.setContentTitle("Vergissmeinnicht");
			nb.setContentText("Sie haben ihren Tracker Verloren");
			nb.setOngoing(false);
			Notification n = nb.build();
			mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			mNotificationManager.notify(273, n);
		}
		if (!lost && !manual){
			tv1.setText(Html.fromHtml(getString(R.string.viewalertuser_title_toofar)));
			tv2.setText(Html.fromHtml(getString(R.string.viewalertuser_introduction_toofar)));
		}
		
		if (!ringing) {
			ringing = true;
			Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
			r = RingtoneManager.getRingtone(getApplicationContext(), notification);
			r.play();
		}
		
		mutebut = (Button) findViewById(R.id.button1);
		mutebut.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (ringing) {
					ringing = false;
					r.stop();
				}
				if (lost) mNotificationManager.cancel(273);
				if (manual) {
				    setResult(Activity.RESULT_OK, null);
					finish();
				}
				if (!lost && !manual) {
					setResult(Activity.RESULT_OK, null);
					finish();
				}
			}
		});
		
		if (lost) {
			BluetoothManager mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
			BluetoothAdapter mBluetoothAdapter = mBluetoothManager.getAdapter();

			final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
			try {
				mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
			}catch (Exception e){
				Log.e("connect Gatt","Could not connect to bluetooth gatt");
			}
		}
	}
	
	final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
			if (newState == BluetoothProfile.STATE_CONNECTED) {
				Log.i("onConnectionStateChange", "Connected to GATT server.");
				runOnUiThread(new Runnable(){
					@Override
					public void run() {
						tv2.setText("GEFUNDEN!");
					}
				});
				if (!ringing) {
					ringing = true;
					Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
					r = RingtoneManager.getRingtone(getApplicationContext(), notification);
					r.play();
					Runnable beeprun = new Runnable() {
						@Override
						public void run() {
							r.stop();
						}
					};
					h.postDelayed(beeprun, 2500);
				}
				if (lost) mNotificationManager.cancel(273);
				if (lost) finish();

			} else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
				Log.w("onConnectionStateChange", "Disconnected from GATT server.");
				tv1.setText(Html.fromHtml(getString(R.string.viewalertuser_title_linkloss)));
				tv2.setText(Html.fromHtml(getString(R.string.viewalertuser_introduction_linkloss)));
			}
		}
	};
}
