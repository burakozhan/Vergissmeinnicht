package de.ozhan.burak.android.test4;

import java.util.ArrayList;

import de.ozhan.burak.android.vergissmeinnicht.bluetooth.FoundBluetoothDevice;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class Test4MainActivity extends Activity {

	ToggleButton tb;
	TextView tv;
	TextView tvs;
	TextView tvscan;
	ProgressBar spi;
	Handler handler;
	BluetoothAdapter mBluetoothAdapter ;
	ListView lv;
	Button clearbtn,restartbtn;
	boolean flagForRestartListener = false;

	ArrayList<FoundBluetoothDevice> listItems=new ArrayList<FoundBluetoothDevice>();

	static ArrayAdapter<FoundBluetoothDevice> adapter ;

	@Override
	protected void onResume(){
		super.onResume();
		listItems.clear();
		adapter.notifyDataSetChanged();
		tvscan.setText("Stopped. Please start scan with Restart");
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_test4_main);

		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		adapter = new ArrayAdapter<FoundBluetoothDevice>(this, android.R.layout.simple_list_item_1, listItems);
		tv = (TextView) findViewById(R.id.textView3);
		tvs = (TextView) findViewById(R.id.textViewStatus);
		tvscan = (TextView) findViewById(R.id.textViewScan);
		spi = (ProgressBar) findViewById(R.id.progressBar2);
		lv = (ListView) findViewById(R.id.listView1);
		clearbtn = (Button) findViewById(R.id.buttonBut);
		restartbtn = (Button) findViewById(R.id.button2);
		tb = (ToggleButton) findViewById(R.id.toggleButton1);

		tvscan.setVisibility(View.GONE);
		tvscan.setText("Please start BLE scan with Restart");
		
		lv.setAdapter(adapter);
		clearbtn.setOnClickListener(clearBtnHandler);
		restartbtn.setOnClickListener(restartBtnHandler);
		tb.setOnClickListener(toggleHandler);
		lv.setOnItemClickListener(listViewClickListener);
		//		lv.setOnItemClickListener(listViewClickListener);

		IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
		registerReceiver(btStateChangeReceiver, filter); // FIXME : Don't forget to unregister during onDestroy

		IntentFilter filter3 = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
		registerReceiver(discoveryStarted, filter3); // FIXME : Don't forget to unregister during onDestroy

		IntentFilter filter4 = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		registerReceiver(discoveryStopped, filter4); // FIXME : Don't forget to unregister during onDestroy

		if (mBluetoothAdapter.isEnabled()) {
			tb.setChecked(true);
			tvscan.setVisibility(View.VISIBLE);
		}

	}

	@Override
	protected void onDestroy(){
		super.onDestroy();
		unregisterReceiver(btStateChangeReceiver);
		unregisterReceiver(discoveryStarted);
		unregisterReceiver(discoveryStopped);
		mBluetoothAdapter.disable();
	}

	OnClickListener clearBtnHandler = new OnClickListener() {
		@Override
		public void onClick(View v) {
			listItems.clear();
			adapter.notifyDataSetChanged();
		}
	};

	OnClickListener restartBtnHandler = new OnClickListener() {
		@Override
		public void onClick(View v) {
			if ( mBluetoothAdapter.getState() == BluetoothAdapter.STATE_OFF ){
				Toast toast = Toast.makeText(getApplicationContext(), "Bluetooth is OFF", Toast.LENGTH_SHORT);
				toast.show();
			} else {
				Log.v("resartBtnHandler", "restart was pressed");			
				mBluetoothAdapter.stopLeScan(callback);
				Log.v("resartBtnHandler", "discovery was cancelled");

				mBluetoothAdapter.startLeScan(callback);
				Log.v("resartBtnHandler", "discovery was restarted");
				
				tvscan.setText("Scanning for BLE Devices");
				tvscan.setVisibility(View.VISIBLE);
				
				handler = new Handler();
				Runnable runnable = new Runnable(){
				    @Override
					public void run() {
				        Toast.makeText(getApplicationContext(), "BLE Scan Stopped", Toast.LENGTH_SHORT).show();
						tvscan.setText("Stopped. Please start scan with Restart");
						mBluetoothAdapter.stopLeScan(callback);
				    }
				};

				handler.postDelayed(runnable, 15000);
			}
		}
	};

	OnItemClickListener listViewClickListener = new OnItemClickListener(){

		@Override
		public void onItemClick(AdapterView<?> arg0, View v, int position, long id) {
			Log.i("ClickListener", "id : "+id);
			Log.i("ClickListener", "po : "+position);
			Log.i("ClickListener", "name : "+ adapter.getItem(position).device.getName());
			mBluetoothAdapter.stopLeScan(callback);
			handler.removeCallbacksAndMessages(null);
			Intent myIntent = new Intent(v.getContext(), DevConnectActivity.class);
			myIntent.putExtra("BSSID", adapter.getItem(position).device.getAddress());
			myIntent.putExtra("Position", position);
			startActivityForResult(myIntent, 0);
			//			adapter.getItem(position).device
			// TODO Auto-generated method stub

		}

	};
	
	public static FoundBluetoothDevice getDeviceFromAdapter(int position){
		return adapter.getItem(position);
	}


	//FIXME Remove 
//	BluetoothGattCallback mGattCallback_B = new BluetoothGattCallback() {
//		@Override
//		public void 	onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic){
//			Log.v("onCharacteristicChanged",""+characteristic.getInstanceId());
//		}
//		@Override
//		public void 	onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
//			Log.v("onCharacteristicRead",""+characteristic.getInstanceId());
//		}
//		@Override
//		public void 	onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
//			Log.v("onCharacteristicWrite",""+characteristic.getInstanceId());
//		}
//		@Override
//		public void 	onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
//			Log.v("onConnectionStateChange",""+gatt.getDevice().getName());
//		}
//		@Override
//		public void 	onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
//			Log.v("onDescriptorRead",""+descriptor.getUuid());
//		}
//		@Override
//		public void 	onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
//			Log.v("onDescriptorWrite",""+descriptor.getUuid());
//		}
//		@Override
//		public void 	onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
//			Log.v("onReadRemoteRssi",""+gatt.getDevice().getName());
//		}
//		@Override
//		public void 	onReliableWriteCompleted(BluetoothGatt gatt, int status) {
//			Log.v("onReliableWriteCompleted",""+gatt.getDevice().getName());
//		}
//		@Override
//		public void 	onServicesDiscovered(BluetoothGatt gatt, int status) {
//			Log.v("onServicesDiscovered",""+gatt.getDevice().getName());
//		}
//	};


	OnClickListener toggleHandler = new OnClickListener() {
		@Override
		public void onClick(View v) {
			boolean stat = ((ToggleButton)v).isChecked();
			Log.d("Pressed and stat was :",""+stat);
			if (stat) {
				Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivityForResult(enableBtIntent, 0);
				tvscan.setVisibility(View.VISIBLE);
				tvscan.setText("Please start BLE scan with Restart");
			}
			if (!stat) {
				mBluetoothAdapter.stopLeScan(callback);
				mBluetoothAdapter.disable(); 
				Log.v("toggleHandler", "bluetooth is offed");  
				tvscan.setVisibility(View.GONE);
			}
		}
	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.test4_main, menu);
		return true;
	}

	private final BroadcastReceiver discoveryStarted = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.v("discoveryStarted","BT discovery just started");
			tvs.setText("Status: Discovering...");
		}
	};

	private final BroadcastReceiver discoveryStopped = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.v("discoveryStarted","BT discovery just STOPPED");
			tvs.setText("Status: Inactive");
		}
	};


	BluetoothAdapter.LeScanCallback callback = new BluetoothAdapter.LeScanCallback(){

		@Override
		public void onLeScan(BluetoothDevice blueDevice, int rssi, byte[] scanRecord) {
			// TODO Auto-generated method stub

			Log.d("FOUND:",blueDevice.getName()+" "+blueDevice.getAddress().toString());

			Log.d("FOUND:","RSSI of "+blueDevice.getName()+" is "+rssi);
			FoundBluetoothDevice device = new FoundBluetoothDevice(blueDevice, (short) rssi, System.currentTimeMillis());
			updateorAppend(device);

		}



		private void updateorAppend(final FoundBluetoothDevice device) {
			for ( FoundBluetoothDevice fdb : listItems ){

				if ( fdb.equals(device)) {
					//					fdb.device = device.device;
					fdb.rssi = device.rssi;
					fdb.lastseen = device.lastseen;
					return;
				}

			}

			Test4MainActivity.this.runOnUiThread(new Runnable(){
				@Override
				public void run(){
					listItems.add(device);
					adapter.notifyDataSetChanged();
				}
			});

		}
	};

	private final BroadcastReceiver btStateChangeReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
				final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
				Log.v("BT_STATE_CHANGED", "Extra state : "+state);
				switch (state) {
				case BluetoothAdapter.STATE_OFF:
					tb.setChecked(false);
					spi.setVisibility(View.GONE);
					tvs.setText("Status : BT is OFF");
					break;
				case BluetoothAdapter.STATE_ON:
					tb.setChecked(true);
					spi.setVisibility(View.GONE);
					tvs.setText("Status : BT is ON");
					break;
				case BluetoothAdapter.STATE_TURNING_OFF:
					spi.setVisibility(View.VISIBLE);
					tvs.setText("Turning BT off");
					break;
				case BluetoothAdapter.STATE_TURNING_ON:
					spi.setVisibility(View.VISIBLE);
					tvs.setText("Turning BT on");
					break;
				}
			}
		}
	};

}
