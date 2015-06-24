package de.ozhan.burak.android.vergissmeinnicht.gui;

import java.util.ArrayList;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import de.ozhan.burak.android.test4.R;
import de.ozhan.burak.android.vergissmeinnicht.bluetooth.FoundBluetoothDevice;

public class ViewThreeActivity extends Activity {
	TextView tv,tvfound;
	ListView lv;
	BluetoothAdapter mBluetoothAdapter;
	ArrayList<FoundBluetoothDevice> listItems=new ArrayList<FoundBluetoothDevice>();
	static ArrayAdapter<FoundBluetoothDevice> adapter ;
	Handler handler;
	
	static public FoundBluetoothDevice vergissmeinnichtTracker;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.viewthree);
		
		handler = new Handler();
		
		tv = (TextView) findViewById(R.id.textView1);
		tv.setText(Html.fromHtml(getString(R.string.viewthree_introduction)));
		
		lv = (ListView) findViewById(R.id.listView1);
				
		tvfound = (TextView) findViewById(R.id.textView3);
		tvfound.setVisibility(View.INVISIBLE);
		adapter = new ArrayAdapter<FoundBluetoothDevice>(this, android.R.layout.simple_list_item_1, listItems);
		lv.setAdapter(adapter);
		
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		Log.i("ViewThreeActivity","Adapter is enabled : "+mBluetoothAdapter.isEnabled());
		boolean scanstart = mBluetoothAdapter.startLeScan(callback);
		Log.i("ViewThreeActivity","startLeScan started with status: "+scanstart);
		Log.i("ViewThreeActivity","isDiscovering() "+mBluetoothAdapter.isDiscovering());


	}
	
	@Override
	protected void onResume(){
		super.onResume();
		listItems.clear();
		adapter.notifyDataSetChanged();
//		mBluetoothAdapter.startLeScan(callback);
	}

	BluetoothAdapter.LeScanCallback callback = new BluetoothAdapter.LeScanCallback(){

		@Override
		public void onLeScan(BluetoothDevice blueDevice, int rssi, byte[] scanRecord) {

			Log.d("FOUND:",blueDevice.getName()+" "+blueDevice.getAddress().toString());
			FoundBluetoothDevice device = new FoundBluetoothDevice(blueDevice, (short) rssi, System.currentTimeMillis());
			
			if ( device.isVmn() ) {
				Log.i("ViewThreeActivity"," We have found ourselves a Vergissmeinnicht Tracker ! ");
				vergissmeinnichtTracker = device;
				handler.postDelayed(startNextActivity, 1500);
			}
			updateorAppend(device);
		}
		private void updateorAppend(final FoundBluetoothDevice device) {
			for ( FoundBluetoothDevice fdb : listItems ){
				if ( fdb.equals(device)) {
					fdb.rssi = device.rssi;
					fdb.lastseen = device.lastseen;
					return;
				}
			}
			ViewThreeActivity.this.runOnUiThread( new Runnable(){
				@Override
				public void run(){
					if (device.isVmn()) tvfound.setVisibility(View.VISIBLE);
					listItems.add(device);
					adapter.notifyDataSetChanged();
				}
			});
		}
	};
	
	final Runnable startNextActivity = new Runnable(){
		@Override
		public void run() {
			mBluetoothAdapter.stopLeScan(callback);
//			Intent myIntent = new Intent( tv.getRootView().getContext() , DevConnectActivity.class);
			Intent myIntent = new Intent( tv.getRootView().getContext() , ViewConfigTrackerActivity.class);
			myIntent.putExtra("BSSID", vergissmeinnichtTracker.device.getAddress());
			startActivityForResult(myIntent, 0);
		}
	};
}
