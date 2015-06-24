package de.ozhan.burak.android.vergissmeinnicht.gui;

import java.util.ArrayList;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.movisens.smartgattlib.Descriptor;

import de.ozhan.burak.android.test4.R;
import de.ozhan.burak.android.vergissmeinnicht.bluetooth.FoundBluetoothDevice;

public class ViewConfigTrackerActivity extends Activity{

	final private String TAG= "ViewConfigTrackerActivity"; // for log output
	final private int ALERTUSERCALLED= 2731; // Just a random number for ID 

	String address;
	FoundBluetoothDevice mDevice;
	Handler handler;
	BluetoothGatt mBluetoothGatt;
	boolean alertOnPhone = true ,alertOnTracker = true;
	static boolean alertingUser;
	static boolean flagLostcompletely=false;

	// Services on Tracker
	private BluetoothGattCharacteristic buzzer;
	private BluetoothGattCharacteristic buttons;
	private BluetoothGattCharacteristic linkloss;

	// UI Elements
	TextView tv;
	ToggleButton tb;
	RadioButton rb1,rb2,rb3;
	RadioGroup rg;
	Button beepbuttn;

	// For the Graph
	XYPlot plot;
	SimpleXYSeries series1;
	ArrayList<Integer> rssil;
	private static final int HISTORY_SIZE = 150;   
	boolean rssiGraphActive=false;

	// Stuff for the beeps
	boolean beeping = false;
	boolean forcedbeeping = false;
	long beeptill;
	Handler beephandler;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.viewconfigtracker);
		handler = new Handler();
		beephandler = new Handler();
		alertingUser = false;
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			address = extras.getString("BSSID");
		}
		//		mDevice = ViewThreeActivity.vergissmeinnichtTracker;
		tv = (TextView) findViewById(R.id.textView2);
		tv.setText(Html.fromHtml(getString(R.string.viewconfig_introduction)));

		// Radiobuttons
		rb1 = (RadioButton) findViewById(R.id.radioButton1);
		rb2 = (RadioButton) findViewById(R.id.radioButton2);
		rb3 = (RadioButton) findViewById(R.id.radioButton3);
		rb2.setChecked(true);
		rg = (RadioGroup) findViewById(R.id.radioWarnType);
		rg.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				if(checkedId == R.id.radioButton1 ){ 
					Log.i("RADIO BUTTON CHANGED","Key 1");
					alertOnPhone = false;
					alertOnTracker = true;
				}
				if(checkedId == R.id.radioButton2 ){ 
					Log.i("RADIO BUTTON CHANGED","Key 2");
					alertOnPhone = true;
					alertOnTracker = true;
				}
				if(checkedId == R.id.radioButton3 ){ 
					Log.i("RADIO BUTTON CHANGED","Key 3");
					alertOnPhone = true;
					alertOnTracker = false;
				}
			}
		});

		// Toggle Button Handler, sets a flag for the graph and speed up rssi request
		tb = (ToggleButton) findViewById(R.id.configToggleButton);
		tb.setOnClickListener( new OnClickListener() {
			@Override
			public void onClick(View v) {
				rssiGraphActive = ((ToggleButton)v).isChecked();
				if (rssiGraphActive) {
					// reset plot if scanning has restarted
					plot.removeSeries(series1);
					series1 = new SimpleXYSeries("RSSI");
					series1.useImplicitXVals();
					plot.addSeries(series1, new LineAndPointFormatter(Color.RED, Color.GREEN, Color.BLUE, null));
					plot.redraw();
				}
			}
		});

		// Setup graph for signal strength indicator
		rssil = new ArrayList<Integer>();
		plot = (XYPlot) findViewById(R.id.mySimpleXYPlot);
		series1 = new SimpleXYSeries("RSSI");
		series1.useImplicitXVals();
		plot.setRangeBoundaries(-100, -10, BoundaryMode.FIXED);
		plot.addSeries(series1, new LineAndPointFormatter(Color.RED, Color.GREEN, Color.BLUE, null));
		plot.getLegendWidget().setVisible(false);

		beepbuttn = (Button) findViewById(R.id.buttonBeep);
		beepbuttn.setActivated(false);

		beepbuttn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				forcedbeeping = true;
				longbeep();
			}
		});

		BluetoothManager mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		BluetoothAdapter mBluetoothAdapter = mBluetoothManager.getAdapter();

		final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);

		try {
			mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
		}catch (Exception e){
			Log.e("connect Gatt","Could not connect to bluetooth gatt");
		}

		// This cannot possibly work ! Gatt not connected when attempting to discover services !   
		// Log.i("ViewConfigTrackerActivity", "Attempting to start service discovery:" + mBluetoothGatt.discoverServices());
		//		try { Thread.sleep(200); } catch ( Exception e ) {Log.e("Sleep intterrupted",""); }

	}


	/**
	 * This registers to the Services of the Bluetooth Tracker, to keep updated
	 */
	private void registerToServices(){

		linkloss.setValue(2, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
		mBluetoothGatt.writeCharacteristic(linkloss);
		try { Thread.sleep(200); } catch ( Exception e ) {Log.e("Sleep intterrupted",""); }

		// Workaround, following code only works when called from another thread. 
		// No idea why, so this workaround is used
		Runnable keyrun = new Runnable() {
			@Override
			public void run() {
				// Both setNotification and setValue by hand need to be called,
				// again no idea why, this construct is used.
				mBluetoothGatt.setCharacteristicNotification(buttons, true);
				BluetoothGattDescriptor descriptor = buttons.getDescriptor(Descriptor.CLIENT_CHARACTERISTIC_CONFIGURATION);
				descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
				Log.v("","SetNotification service for keys : "+mBluetoothGatt.writeDescriptor(descriptor));
				try { Thread.sleep(200); } catch ( Exception e ) {Log.e("Sleep intterrupted",""); }
				beepbuttn.setActivated(true);
			}
		};
		handler.postDelayed(keyrun, 10);

	}

	/**
	 * Requests current signal strength of phone as seen by the Tracker on a regular basis
	 * period in between requests is reduced when the graph for tracking is shown. 
	 */
	void startRSSIListening(){
		final Runnable runnable = new Runnable(){
			@Override
			public void run() {
				mBluetoothGatt.readRemoteRssi();
				// repeat this request, with shorter interval time if graph is being shown 
				handler.postDelayed(this,(rssiGraphActive)?400:2500);
			}
		};
		handler.postDelayed(runnable, 2000);
	}

	private void minibeep ( final int i ) {
		Log.d("Minibeep", "called with "+i);
		if (!beeping){

			beeping = true;
			mBluetoothGatt.setCharacteristicNotification(buzzer, true);
			byte[] bar = {2,0};
			if(buzzer == null) { Log.e("MiniBeep","buzzer is zero at this step");} else {Log.d("Minibeep","Buzzerok");}
			buzzer.setValue( bar );
			Log.i("write 2:0 (by minibeep): ",""+mBluetoothGatt.writeCharacteristic(buzzer));
			try { Thread.sleep(100); } catch (Exception e) {Log.i("could not sleep","Failed : "+e.getMessage());}

			final Runnable beeprunoff = new Runnable() {
				@Override
				public void run() {
					byte[] bar = {0,0};
					buzzer.setValue( bar );
					Log.i("write 0:0 (by minibeep): ",""+mBluetoothGatt.writeCharacteristic(buzzer));
					try { Thread.sleep(100); } catch (Exception e) {Log.i("could not sleep","Failed : "+e.getMessage());}
					beeping = false;
				}
			};

			Runnable beeprun = new Runnable() {
				@Override
				public void run() {
					byte[] bar = {1,0};
					buzzer.setValue( bar );
					Log.i("write 1:0 (by minibeep): ",""+mBluetoothGatt.writeCharacteristic(buzzer));
					try { Thread.sleep(100); } catch (Exception e) {Log.i("could not sleep","Failed : "+e.getMessage());}
					beephandler.postDelayed(beeprunoff, (i*800));
				}
			};
			beephandler.postDelayed(beeprun, ((i/2)*900));
		}
	}

	private void longbeep (){
		Log.d("Longbeep", "called with ");
		if (!beeping){
			beeptill = System.currentTimeMillis()+(12000);
			beeping = true;
			mBluetoothGatt.setCharacteristicNotification(buzzer, true);
			byte[] bar = {2,0};
			buzzer.setValue( bar );
			Log.i("write 2:0 (by longbeep): ",""+mBluetoothGatt.writeCharacteristic(buzzer));
			try { Thread.sleep(100); } catch (Exception e) {Log.i("could not sleep","Failed : "+e.getMessage());}
			final Runnable beeprunoff = new Runnable() {
				@Override
				public void run() {
					byte[] bar = {0,0};
					buzzer.setValue( bar );
					Log.i("write 0:0 (by minibeep): ",""+mBluetoothGatt.writeCharacteristic(buzzer));
					try { Thread.sleep(100); } catch (Exception e) {Log.i("could not sleep","Failed : "+e.getMessage());}
					beeping = false;
					forcedbeeping = false;
				}
			};
			beephandler.postDelayed(beeprunoff, (12000));
		}
	}

	private void beepoff (){
		Log.d("beepoff", "called with ");
		if (beeping){
			if ( System.currentTimeMillis() > beeptill) {
				beephandler.removeCallbacksAndMessages(null);
				byte[] bar = {0,0};
				buzzer.setValue( bar );
				Log.i("write 0:0 (by beepoff): ",""+mBluetoothGatt.writeCharacteristic(buzzer));
				beeping = false;
				try { Thread.sleep(100); } catch (Exception e) {Log.i("could not sleep","Failed : "+e.getMessage());}
			}
		}
	}

	private void beepkill (){
		if (beeping && !forcedbeeping) {
			Log.d("beepkill", "called with ");
			beeptill = 0 ;
			beephandler.removeCallbacksAndMessages(null);
			byte[] bar = {0,0};
			buzzer.setValue( bar );
			Log.i("write 0:0 (by beepkill): ",""+mBluetoothGatt.writeCharacteristic(buzzer));
			beeping = false;
			try { Thread.sleep(100); } catch (Exception e) {Log.i("could not sleep","Failed : "+e.getMessage());}
		}
	}

	//	// Implements callback methods for GATT events that the app cares about.  For example,
	//	// connection change and services discovered.
	//	final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
	//		@Override
	//		public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
	//			Log.w("onCharacteristicRead", "onCharacteristicRead received: " + characteristic.getUuid() + " <-> "+ status);
	//		}
	//
	//		public void onReadRemoteRssi(BluetoothGatt gatt, final int rssi, int status){
	//			Log.w("onReadRemoteRssi", "Remote RSSI is : "+rssi);
	//			runOnUiThread(new Runnable(){
	//				public void run(){
	//					rssil.add(rssi);
	//					if (series1.size() > HISTORY_SIZE) series1.removeFirst();
	//					series1.addLast(null, rssi);
	//					plot.redraw();
	//				}
	//			});
	//			if (rssi < -83) {
	//				Log.w("Warning","Link very weak");
	//				longbeep();
	//			} else if (rssi > -35){
	//				// remove warning
	//				beepkill();
	//			} else if (rssi > -50){
	//				// remove warning
	//				beepoff();
	//			} else if (rssi > -70){
	//				// Do nothing
	//			} else {
	//				Log.w("Warning","Link level low, could break");
	//				minibeep(2);
	//			}
	//		}
	//
	//
	//		@Override
	//		public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
	//
	//			Log.i("onCharacteristicChanged", "onCharacteristicChanged received: " + characteristic.getUuid());
	//
	//			if ( characteristic.getUuid().toString().contains("ffe1")){
	//
	//				int keys = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
	//				Log.i("Keys pressed", ""+keys);
	//			}
	//		}
	//
	//		@Override
	//		public void onDescriptorWrite (BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status){
	//			Log.i("onDescriptorWrite returned with", "Status: "+status+" which means <could not determine>");
	//
	//		}
	//		@Override
	//		public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
	//			if (newState == BluetoothProfile.STATE_CONNECTED) {
	//				Log.i("onConnectionStateChange", "Connected to GATT server.");
	//				// Attempts to discover services after successful connection.
	//				Log.i("onConnectionStateChange", "Attempting to start service discovery:" + mBluetoothGatt.discoverServices());
	//
	//			} else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
	//				Log.w("onConnectionStateChange", "Disconnected from GATT server.");
	//			}
	//		}
	//
	//		@Override
	//		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
	//			Log.e("onServicesDiscovered", "Status: "+status);
	//
	////			Log.w("conactv", "1onServicesDiscovered received: " + status);
	////			List<BluetoothGattService> l = mBluetoothGatt.getServices();
	////
	////			Iterator<BluetoothGattService> i = l.iterator();
	////			BluetoothGattService mbgs;
	////			while (i.hasNext()){
	////				mbgs = i.next();
	////				Log.i("Service-UUID",""+mbgs.getUuid());
	////				//				Log.i(" Service Inst. ID", " "+mbgs.getInstanceId());
	////				String service = (""+mbgs.getUuid()).substring(4, 8);
	////				Log.i(" Service Inst. ID", service);
	////				List<BluetoothGattCharacteristic> li = mbgs.getCharacteristics();
	////				Iterator<BluetoothGattCharacteristic> bgci = li.iterator();
	////				BluetoothGattCharacteristic bgc ;
	////				while (bgci.hasNext()){
	////					bgc = bgci.next();
	////					Log.i("Charactrstc-UUID",""+bgc.getUuid());
	////					Log.i("  Charac Inst. ID", "   "+bgc.getInstanceId());
	////					Log.i("This has so many Descriptors -> ", ""+bgc.getDescriptors().size());
	////					if ( new String(""+bgc.getUuid()).contains("2a06")) {
	////						if (service.equalsIgnoreCase("1802")){
	////							Log.d("Buzzer Found","1802:2a06 <->"+mbgs.getUuid().toString().substring(4,8)+ ":"+bgc.getUuid().toString().substring(4,8));
	////							buzzer = bgc;
	////						}
	////						if (service.equalsIgnoreCase("1803")){
	////							linkloss = bgc;
	////						}
	////					}
	////
	////					if ( new String(""+bgc.getUuid()).contains("ffe1")) {
	////						buttons = bgc;
	////						Log.i("bgc.getUuid()).contains(ffe1)"," There was a UUID that matches FFE1");
	////						//						List<BluetoothGattDescriptor> bgdl = bgc.getDescriptors();
	////						//						Iterator<BluetoothGattDescriptor> bgdi = bgdl.iterator();
	////						//						BluetoothGattDescriptor bgd ;
	////						//						while (bgdi.hasNext()){
	////						//							bgd = bgdi.next();
	////						//							try { Log.i("getUUID()",""+bgd.getUuid());} catch (Exception e) {Log.i("getUUID()","Failed : "+e.getMessage());}
	////						//							try { Log.i("getValue()",""+bgd.getValue());} catch (Exception e) {Log.i("getValue()","Failed : "+e.getMessage());}
	////						//						}
	////					}
	////					if ( new String(""+bgc.getUuid()).contains("2a19")) {
	////						List<BluetoothGattDescriptor> bgdl = bgc.getDescriptors();
	////						Iterator<BluetoothGattDescriptor> bgdi = bgdl.iterator();
	////						BluetoothGattDescriptor bgd ;
	////						while (bgdi.hasNext()){
	////							bgd = bgdi.next();
	////							try { Log.i("getUUID()",""+bgd.getUuid());} catch (Exception e) {Log.i("getUUID()","Failed : "+e.getMessage());}
	////							try { Log.i("getValue()",""+bgd.getValue());} catch (Exception e) {Log.i("getValue()","Failed : "+e.getMessage());}
	////						}
	////					}
	////					if ( new String(""+bgc.getUuid()).contains("2a24")) {
	////						Log.i("getInstanceId()",""+bgc.getInstanceId());
	////						Log.i("getPermissions()",""+bgc.getPermissions());
	////						Log.i("getProperties()",""+bgc.getProperties());
	////						try { Log.i("getStringValue(0)",""+bgc.getStringValue(0));} catch (Exception e) {Log.i("getStringValue(0)","Failed : "+e.getMessage());}
	////						try { Log.i("getWriteType()",""+bgc.getWriteType());} catch (Exception e) {Log.i("getWriteType()","Failed : "+e.getMessage());}
	////						try { Log.i("getFloatValue(FORMAT_FLOAT, 0)",""+bgc.getFloatValue(BluetoothGattCharacteristic.FORMAT_FLOAT, 0));} catch (Exception e) {Log.i("getFloatValue(FORMAT_FLOAT, 0)","Failed : "+e.getMessage());}
	////						try { Log.i("getIntValue(FORMAT_, 0)",""+bgc.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0));} catch (Exception e) {Log.i("getIntValue(FORMAT_, 0)","Failed : "+e.getMessage());}
	////						try { Log.i("getValue()",""+bgc.getValue());} catch (Exception e) {Log.i("getValue()","Failed : "+e.getMessage());}
	////					}
	////				}
	////			}
	//		}
	//	};

	final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
		@Override
		public void 	onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
			Log.i("onCharacteristicChanged", "onCharacteristicChanged received: " + characteristic.getUuid());

			if ( characteristic.getUuid().toString().contains("ffe1")){
				int keys = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
				Log.i("Keys pressed", ""+keys);
				if (keys == 1)alertUser(true,true,false);
			}
		}

		@Override
		public void 	onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
			if (newState == BluetoothProfile.STATE_CONNECTED) {
				Log.i("onConnectionStateChange", "Connected to GATT server.");

				// Attempts to discover services after successful connection.
				Log.i("onConnectionStateChange", "Attempting to start service discovery:" + mBluetoothGatt.discoverServices());

			} else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
				Log.w("onConnectionStateChange", "Disconnected from GATT server.");
				flagLostcompletely = true;
				alertUser(true,false,true);
			}
		}

		@Override
		public void 	onReadRemoteRssi(BluetoothGatt gatt, final int rssi, int status) {
			Log.w("onReadRemoteRssi", "Remote RSSI is : "+rssi);
			if (rssiGraphActive){
				runOnUiThread(new Runnable(){
					@Override
					public void run(){
						rssil.add(rssi);
						if (series1.size() > HISTORY_SIZE) series1.removeFirst();
						series1.addLast(null, rssi);
						plot.redraw();
					}
				});
			}
			if (rssi < -83) {
				Log.w("Warning","Link very weak");
				if (alertOnTracker) longbeep();
				if (alertOnPhone) alertUser(true,false,false);
			} else if (rssi > -35){
				// remove warning
				beepkill();
			} else if (rssi > -50){
				// remove warning
				beepoff();
			} else if (rssi > -75){
				// Do nothing
			} else {
				Log.w("Warning","Link level low, could break");
				if (beeptill < System.currentTimeMillis()) {
					beeptill = System.currentTimeMillis()+(10000); // Do as if you beep for 10 seconds to avoid repetitive alarms
				}
				if (alertOnTracker) minibeep(3);
				if (alertOnPhone) alertUser(false,false,false);
			}
		}
		@Override
		public void 	onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
			Log.i("onDescriptorRead", "onDescriptorRead received: " + descriptor.getUuid() + " with status : "+ status);
		}
		@Override
		public void 	onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
			Log.i("onDescriptorWrite", "onDescriptorWrite received: " + descriptor.getUuid() + " with status : "+ status);
		}
		@Override
		public void 	onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
			Log.i("onCharacteristicRead", "onCharacteristicRead received: " + characteristic.getUuid() + " with status : "+ status);
		}
		@Override
		public void 	onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
			Log.i("onCharacteristicWrite", "onCharacteristicWrite received: " + characteristic.getUuid() + " with status : "+ status);
		}

		/** 
		 * This handles the found services
		 * It picks the 3 services we are interested in and stores a pointer to their "configurations descriptor"s
		 */
		@Override
		public void onServicesDiscovered( BluetoothGatt gatt, int status) {

			// Iterate over all Services
			for (BluetoothGattService mbgs:gatt.getServices()){
				String thisServiceIs = (""+mbgs.getUuid()).substring(4, 8);

				// Iterate over all Characteristics one service has
				for (BluetoothGattCharacteristic mbgc:mbgs.getCharacteristics()){
					String thisCharIs = (""+mbgc.getUuid()).substring(4, 8);

					// If this is the characteristic to configure settings, then look if it the right service
					if ( thisCharIs.equalsIgnoreCase("2a06")) {
						// this finds the buzzer Service
						if (thisServiceIs.equalsIgnoreCase("1802")){
							Log.v(TAG+"onServicesDiscovered", "Found buzzer");
							buzzer = mbgc;
						}
						// this finds the linkloss service
						if (thisServiceIs.equalsIgnoreCase("1803")){
							Log.v(TAG+"onServicesDiscovered", "Found linkloss");
							linkloss = mbgc;
						}
					}

					// Button Service has it's own special case of settings characteristic, because it does NOT follow Bluetooth specifications.
					if (thisCharIs.equalsIgnoreCase("ffe1")){
						if (thisServiceIs.equalsIgnoreCase("ffe0")) {
							Log.v(TAG+"onServicesDiscovered", "Found buttons");
							buttons = mbgc;
						}
					}
				}
			}
			// When all services are discovered register to desired services
			registerToServices();
			// as last step start the looper for RSSI checking
			startRSSIListening();
		}
	};

	private void alertUser(boolean persistent, boolean manual, boolean linklost) {
		Log.wtf("alertuser was called with : ", "pers:"+persistent+" manual:"+manual+" linkloss:"+linklost+" alerting:"+alertingUser);
			if (persistent) {
				if(!alertingUser) {
					alertingUser = true;
					flagLostcompletely = false; // reset flag only if not already alerting

				Log.e("","No comeback until user presses button.");
						
				Intent myIntent = new Intent( tv.getRootView().getContext() , ViewAlertUserActivity.class);
				myIntent.putExtra("BSSID", address);
				myIntent.putExtra("Manual", manual);
				myIntent.putExtra("Lost", linklost);
				startActivityForResult(myIntent, ALERTUSERCALLED);
				}	
			}
			if (!persistent) {
				Log.e("","Short error");
			}

	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	    // Check which request we're responding to
	    if (requestCode == ALERTUSERCALLED) {
//	    	alertingUser = false;
	        // Make sure the request was successful
	        if (resultCode == RESULT_OK) {
		    	alertingUser = false;
		    	Log.wtf("aletring is reset", "alerting:"+alertingUser);
		    	if ( flagLostcompletely ) alertUser(true,false,true);
	        }
	    }
	}
}
