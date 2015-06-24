package de.ozhan.burak.android.test4;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

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
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.movisens.smartgattlib.Descriptor;

import de.ozhan.burak.android.vergissmeinnicht.bluetooth.FoundBluetoothDevice;
import de.ozhan.burak.android.vergissmeinnicht.gui.ViewThreeActivity;

public class DevConnectActivity extends Activity {

	private BluetoothGatt mBluetoothGatt;
	private BluetoothAdapter mBluetoothAdapter;
	private BluetoothManager mBluetoothManager;
	private BluetoothGattCharacteristic buzzer;
	private BluetoothGattCharacteristic buttons;
	private BluetoothGattCharacteristic battery;
	private BluetoothGattCharacteristic linkloss;
	private static final int HISTORY_SIZE = 150;   

	TextView tv;
	TextView RRssi,LRssi;
	CheckBox cbl, cbr;
	Button buttonBeep;
	Button buttonLL;
	String address="";
	int pos;
	FoundBluetoothDevice dev;
	boolean burak = false;
	boolean lastNotifierState = false;
	boolean mbusy = false;
	boolean beeping = false;
	boolean forcedbeeping = false;
	long beeptill;
	XYPlot plot;
	SimpleXYSeries series1;
	ArrayList<Integer> rssil;

	Handler handler,beephandler;

	static int runcnt = 0;

	@Override
	protected void onDestroy(){
		super.onDestroy();
		handler.removeCallbacksAndMessages(null);
		mBluetoothGatt.disconnect();
		mBluetoothGatt.close();
		mBluetoothGatt = null;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.devconnect);
		Bundle extras = getIntent().getExtras();

		if (extras != null) {
			address = extras.getString("BSSID");
//			pos = extras.getInt("Position");
		}
		tv = (TextView) findViewById(R.id.textViewA);
		RRssi = (TextView) findViewById(R.id.textViewRSSIRemote);
		LRssi = (TextView) findViewById(R.id.textView3);
		cbl = (CheckBox) findViewById(R.id.checkBox1);
		cbr = (CheckBox) findViewById(R.id.checkBox2);

		rssil = new ArrayList<Integer>();

		plot = (XYPlot) findViewById(R.id.mySimpleXYPlot);
		series1 = new SimpleXYSeries("RSSI");
		series1.useImplicitXVals();
		plot.setRangeBoundaries(-120, 0, BoundaryMode.FIXED);
		plot.addSeries(series1, new LineAndPointFormatter(Color.RED, Color.GREEN, Color.BLUE, null));


		dev = ViewThreeActivity.vergissmeinnichtTracker;

		tv.setText(dev.toString());

		beephandler = new Handler();

		mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

		mBluetoothAdapter = mBluetoothManager.getAdapter();

		final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);

		mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
		Log.i("conactv", "Attempting to start service discovery:" + mBluetoothGatt.discoverServices());

		Button closebuttn = (Button) findViewById(R.id.buttonClose);

		closebuttn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				mBluetoothGatt.disconnect();
				handler.removeCallbacksAndMessages(null);
				//            	mBluetoothGatt.close();
				Intent intent = new Intent();
				setResult(RESULT_OK, intent);
				finish();
			}
		});


		Button screenbuttn = (Button) findViewById(R.id.buttonScr);

		//		screenbuttn.setOnClickListener(new View.OnClickListener() {
		//			public void onClick(View view) {
		//				Log.i("Button Click Bitmap", " ---- Start ---- ");
		//				File root = Environment.getExternalStorageDirectory();
		//				File file = new File(root, "tomato53.txt");
		//				try {
		//					if (root.canWrite()) {
		//						FileWriter filewriter = new FileWriter(file);
		//						BufferedWriter out = new BufferedWriter(filewriter);
		//						out.write("This is just a very nice test ...");
		//						out.close();
		//					}
		//				} catch (IOException e) {
		//					Log.e("TAG", "Could not write file " + e.getMessage());
		//				}
		//				File mediaStorageDir = Environment.getExternalStorageDirectory();
		//				sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://"+ mediaStorageDir)));
		//				Log.i("Button Click Bitmap", " ---- End ---- ");
		//			}
		//		});
		screenbuttn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Log.i("Button Click Bitmap", " ---- Start ---- ");
				//								Process sh;
				//								try {
				//									SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH_mm_ss",Locale.GERMAN);
				//									String mDate = sdf.format(new Date());
				//
				//									sh = Runtime.getRuntime().exec("su");
				//									OutputStream  os = sh.getOutputStream();
				//									os.write(("/system/bin/screencap -p " + "/sdcard/img"+mDate+".png").getBytes("ASCII"));
				//									os.flush();
				//									os.close();
				//									sh.waitFor();
				//								} catch (Exception e) {
				//									// TODO Auto-generated catch block
				//									e.printStackTrace();
				//								}

				try {
					Bitmap bitmap;
					View v1 = tv.getRootView();
					Log.i("Button Click Bitmap", " ---- Has Rootview ---- ");
					v1.setDrawingCacheEnabled(true);
					bitmap = Bitmap.createBitmap(v1.getDrawingCache());
					Log.i("Button Click Bitmap", " ---- Bitmap created ---- ");
					v1.setDrawingCacheEnabled(false);
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH_mm_ss",Locale.GERMAN);
					String mDate = sdf.format(new Date());
					Log.i("Button Click Bitmap", " ---- Use Date as as ->"+mDate+"<- ---- ");
					File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Vergissmeinnicht");
					mediaStorageDir.mkdirs();
					File pFile = new File(mediaStorageDir.getPath()+File.separator+"ScrCap "+mDate+".png");

					Log.i("Button Click Bitmap", " ---- File opened ---- ");
					FileOutputStream fos = new FileOutputStream(pFile);
					Log.i("Button Click Bitmap", " ---- File outputstream ---- ");
					bitmap.compress(CompressFormat.PNG, 100, fos);
					Log.i("Button Click Bitmap", " ---- File compressed ? ---- ");
					fos.flush();
					fos.close();
					Log.i("Button Click Bitmap", " ---- File written ? ---- ");
				} catch (Exception e) {
					Log.e("Could not write Scrncap",""+e.getMessage());
				}
				File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory( Environment.DIRECTORY_PICTURES), "Vergissmeinnicht");
				sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://"+ mediaStorageDir)));

				Log.i("Button Click Bitmap", " ---- End ---- ");
			}
		});

		final Button buttonLL = (Button) findViewById(R.id.buttonLL);

		buttonLL.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Log.i("Button Listnr LinkLoss", " ---- Start ---- ");

				linkloss.setValue(2, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
				mBluetoothGatt.writeCharacteristic(linkloss);
				try { Thread.sleep(200); } catch (Exception e ) {Log.e("Sleep intterrupted",""); }

				Log.i("Button Listnr LinkLoss", " ---- END ---- ");
			}
		});

		final Button buttonBut = (Button) findViewById(R.id.buttonBut);

		buttonBut.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (buttons != null){

					Log.i("Button Click Listener", " ---- Start ---- ");
					mBluetoothGatt.setCharacteristicNotification(buttons, true);
					BluetoothGattDescriptor descriptor = buttons.getDescriptor(Descriptor.CLIENT_CHARACTERISTIC_CONFIGURATION);
					descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
					mBluetoothGatt.writeDescriptor(descriptor);
					Log.i("Button Click Listener", " ---- End ---- ");
				}else{
					Log.e("Error","buttons was null");
				}
			}
		});

		final Button beepbuttn = (Button) findViewById(R.id.buttonBeep);

		beepbuttn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				forcedbeeping = true;
				longbeep();
			}
		});

		handler = new Handler();
		final Runnable runnable = new Runnable(){
			@Override
			public void run() {
				mBluetoothGatt.readRemoteRssi();
				//				mBluetoothGatt.getConnectedDevices().get(0).
				handler.postDelayed(this,1500);
			}
		};

		handler.postDelayed(runnable, 2000);
	}
	

	private void minibeep ( final int i ) {
		Log.d("Minibeep", "called with "+i);
		if (!beeping){
			if (beeptill < System.currentTimeMillis()) {
				beeptill = System.currentTimeMillis()+(10000); // Do as if you beep for 10 seconds to avoid repetitive alarms
				beeping = true;

				byte[] bar = {2,0};
				buzzer.setValue( bar );
				Log.i("write 2:0(by minibeep):",""+mBluetoothGatt.writeCharacteristic(buzzer));
				try { Thread.sleep(100); } catch (Exception e) {Log.i("could not sleep","Failed : "+e.getMessage());}

				final Runnable beeprunoff = new Runnable() {
					@Override
					public void run() {
						byte[] bar = {0,0};
						buzzer.setValue( bar );
						Log.i("write 0:0(by minibeep):",""+mBluetoothGatt.writeCharacteristic(buzzer));
						try { Thread.sleep(100); } catch (Exception e) {Log.i("could not sleep","Failed : "+e.getMessage());}
						beeping = false;
					}
				};

				Runnable beeprun = new Runnable() {
					@Override
					public void run() {
						byte[] bar = {1,0};
						buzzer.setValue( bar );
						Log.i("write 1:0(by minibeep):",""+mBluetoothGatt.writeCharacteristic(buzzer));
						try { Thread.sleep(100); } catch (Exception e) {Log.i("could not sleep","Failed : "+e.getMessage());}
						beephandler.postDelayed(beeprunoff, (i*800));
					}
				};
				beephandler.postDelayed(beeprun, ((i/2)*900));
			}
		}
	}

	private void longbeep (){
		Log.d("Longbeep", "called with ");
		if (!beeping){
			beeptill = System.currentTimeMillis()+(12000);
			beeping = true;
			byte[] bar = {2,0};
			buzzer.setValue( bar );
			Log.i("write 2:0(by longbeep):",""+mBluetoothGatt.writeCharacteristic(buzzer));
			try { Thread.sleep(100); } catch (Exception e) {Log.i("could not sleep","Failed : "+e.getMessage());}
			
			final Runnable runnable = new Runnable(){
				@Override
				public void run() {
					beephandler.removeCallbacksAndMessages(null);
					byte[] bar = {0,0};
					buzzer.setValue( bar );
					Log.i("write 0:0(by beepoff):",""+mBluetoothGatt.writeCharacteristic(buzzer));
					beeping = !beeping;
					try { Thread.sleep(100); } catch (Exception e) {Log.i("could not sleep","Failed : "+e.getMessage());}
				}
			};
			beephandler.postDelayed(runnable, 12000);
		}
	}

	private void beepoff (){
		Log.d("beepoff", "called with ");
		if (beeping){
			if ( System.currentTimeMillis() > beeptill) {
				beephandler.removeCallbacksAndMessages(null);
				byte[] bar = {0,0};
				buzzer.setValue( bar );
				Log.i("write 0:0(by beepoff):",""+mBluetoothGatt.writeCharacteristic(buzzer));
				beeping = !beeping;
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
			Log.i("write 0:0(by beepkill):",""+mBluetoothGatt.writeCharacteristic(buzzer));
			beeping = false;
			try { Thread.sleep(100); } catch (Exception e) {Log.i("could not sleep","Failed : "+e.getMessage());}
		}
	}

	// Implements callback methods for GATT events that the app cares about.  For example,
	// connection change and services discovered.
	private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
		@Override
		public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
			Log.w("onCharacteristicRead", "onCharacteristicRead received: " + characteristic.getUuid() + " <-> "+ status);
			if (characteristic.getUuid().compareTo(battery.getUuid()) == 0){
				Log.i("Battery Status", "    BATT (bar)->"+ characteristic.getValue()[0]);
				Log.i("Battery Status", "    BATT (int)->"+ characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0));
			}
		}

		@Override
		public void onReadRemoteRssi(BluetoothGatt gatt, final int rssi, int status){
			Log.w("onReadRemoteRssi", "Remote RSSI is : "+rssi);
			DevConnectActivity.this.runOnUiThread(new Runnable(){
				@Override
				public void run(){
					RRssi.setText("Remote : "+rssi);
					rssil.add(rssi);
					if (series1.size() > HISTORY_SIZE) series1.removeFirst();
					series1.addLast(null, rssi);
					plot.redraw();
				}
			});
			if (rssi < -83) {
				//TODO: THROW non stopping Warning
				Log.w("Warning","Link very weak");
				longbeep();
			} else if (rssi > -35){
				// remove warning
				beepkill();
			} else if (rssi > -50){
				// remove warning
				beepoff();
			} else if (rssi > -70){
				// Do nothing
			} else {
				//TODO : THROW BIG WARNING ! 
				Log.w("Warning","Link level low, could break");
				minibeep(2);
			}
		}


		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {

			Log.i("onCharacteristicChanged", "onCharacteristicChanged received: " + characteristic.getUuid());

			if ( characteristic.getUuid().toString().contains("ffe1")){

				int keys = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
				if ( keys == 3 ) {
					DevConnectActivity.this.runOnUiThread(new Runnable(){
						@Override
						public void run(){
							cbl.setChecked(true);
							cbr.setChecked(true);
						}
					});
				}
				if ( keys == 2 ) {
					DevConnectActivity.this.runOnUiThread(new Runnable(){
						@Override
						public void run(){
							cbl.setChecked(false);
							cbr.setChecked(true);
						}
					});
				}
				if ( keys == 1 ) {
					DevConnectActivity.this.runOnUiThread(new Runnable(){
						@Override
						public void run(){
							cbl.setChecked(true);
							cbr.setChecked(false);
						}
					});
				}
				if ( keys == 0 ) {
					DevConnectActivity.this.runOnUiThread(new Runnable(){
						@Override
						public void run(){
							cbl.setChecked(false);
							cbr.setChecked(false);
						}
					});
				}
			}
		}

		@Override
		public void onDescriptorWrite (BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status){
			try {
				Log.i("onDescriptorWrite ", "Status: "+status+" which means "+constantToFieldName(gatt, status));
			} catch (Exception e ){
				Log.i("onDescriptorWrite ", "Status: "+status+" which means <could not determine>");
			}

		}
		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
			if (newState == BluetoothProfile.STATE_CONNECTED) {
				Log.i("onConnectionStateChange", "Connected to GATT server.");
				// Attempts to discover services after successful connection.
				Log.i("onConnectionStateChange", "Attempting to start service discovery:" + mBluetoothGatt.discoverServices());

			} else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
				Log.w("onConnectionStateChange", "Disconnected from GATT server.");
			}
		}

		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			Log.e("onServicesDiscovered","Run counter "+ runcnt++);
			try { Log.e("onServicesDiscovered", "Status: "+constantToFieldName(gatt,status));} catch (Exception e1) {Log.e("constantToFieldName", "failed");}
			Log.e("onServicesDiscovered", "Status: "+status);

			Log.w("conactv", "1onServicesDiscovered received: " + status);
			List<BluetoothGattService> l = mBluetoothGatt.getServices();

			Iterator<BluetoothGattService> i = l.iterator();
			BluetoothGattService mbgs;
			while (i.hasNext()){
				mbgs = i.next();
				Log.i("Service-UUID",""+mbgs.getUuid());
				//				Log.i(" Service Inst. ID", " "+mbgs.getInstanceId());
				String service = (""+mbgs.getUuid()).substring(4, 8);
				Log.i(" Service Inst. ID", service);
				List<BluetoothGattCharacteristic> li = mbgs.getCharacteristics();
				Iterator<BluetoothGattCharacteristic> bgci = li.iterator();
				BluetoothGattCharacteristic bgc ;
				while (bgci.hasNext()){
					bgc = bgci.next();
					Log.i("Charactrstc-UUID",""+bgc.getUuid());
					Log.i("  Charac Inst. ID", "   "+bgc.getInstanceId());
					Log.i("Descriptors count -> ", ""+bgc.getDescriptors().size());
					if ( new String(""+bgc.getUuid()).contains("2a06")) {
						if (service.equalsIgnoreCase("1802")){
							Log.d("Buzzer Found","1802:2a06 <->"+mbgs.getUuid().toString().substring(4,8)+ ":"+bgc.getUuid().toString().substring(4,8));
							buzzer = bgc;
						}
						if (service.equalsIgnoreCase("1803")){
							linkloss = bgc;
						}
					}
					if ( new String(""+bgc.getUuid()).contains("2a19")) {
						battery = bgc;
					}
					if ( new String(""+bgc.getUuid()).contains("ffe1")) {
						buttons = bgc;
						Log.i("bgc.Uuid.contains(ffe1)"," There was a UUID that matches FFE1");
						//						List<BluetoothGattDescriptor> bgdl = bgc.getDescriptors();
						//						Iterator<BluetoothGattDescriptor> bgdi = bgdl.iterator();
						//						BluetoothGattDescriptor bgd ;
						//						while (bgdi.hasNext()){
						//							bgd = bgdi.next();
						//							try { Log.i("getUUID()",""+bgd.getUuid());} catch (Exception e) {Log.i("getUUID()","Failed : "+e.getMessage());}
						//							try { Log.i("getValue()",""+bgd.getValue());} catch (Exception e) {Log.i("getValue()","Failed : "+e.getMessage());}
						//						}
					}
					if ( new String(""+bgc.getUuid()).contains("2a19")) {
						List<BluetoothGattDescriptor> bgdl = bgc.getDescriptors();
						Iterator<BluetoothGattDescriptor> bgdi = bgdl.iterator();
						BluetoothGattDescriptor bgd ;
						while (bgdi.hasNext()){
							bgd = bgdi.next();
							try { Log.i("getUUID()",""+bgd.getUuid());} catch (Exception e) {Log.i("getUUID()","Failed : "+e.getMessage());}
							try { Log.i("getValue()",""+bgd.getValue());} catch (Exception e) {Log.i("getValue()","Failed : "+e.getMessage());}
						}
					}
					if ( new String(""+bgc.getUuid()).contains("2a24")) {
						Log.i("getInstanceId()",""+bgc.getInstanceId());
						Log.i("getPermissions()",""+bgc.getPermissions());
						Log.i("getProperties()",""+bgc.getProperties());
						try { Log.i("getStringValue(0)",""+bgc.getStringValue(0));} catch (Exception e) {Log.i("getStringValue(0)","Failed : "+e.getMessage());}
						try { Log.i("getWriteType()",""+bgc.getWriteType());} catch (Exception e) {Log.i("getWriteType()","Failed : "+e.getMessage());}
						try { Log.i("getFloatValue(FF, 0)",""+bgc.getFloatValue(BluetoothGattCharacteristic.FORMAT_FLOAT, 0));} catch (Exception e) {Log.i("getFloatValue(FF, 0)","Failed : "+e.getMessage());}
						try { Log.i("getIntValue(FORMAT_, 0)",""+bgc.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0));} catch (Exception e) {Log.i("getIntValue(FORMAT_, 0)","Failed : "+e.getMessage());}
						try { Log.i("getValue()",""+bgc.getValue());} catch (Exception e) {Log.i("getValue()","Failed : "+e.getMessage());}
					}
				}
			}
			mBluetoothGatt.readCharacteristic(battery);
		}
	};

	public String constantToFieldName(Object obj, int constant) throws Exception {
		for (Field f : obj.getClass().getDeclaredFields()){
			int mod = f.getModifiers();
			if (Modifier.isStatic(mod) && Modifier.isPublic(mod) && Modifier.isFinal(mod)) {
				if ( Integer.parseInt(f.get(null).toString()) == constant ){
					return f.getName();
				}
			}        
		}
		throw new Exception ("Could not find within given fields");
	}


}
