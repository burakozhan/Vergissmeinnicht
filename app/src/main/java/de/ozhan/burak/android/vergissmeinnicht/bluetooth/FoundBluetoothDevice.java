package de.ozhan.burak.android.vergissmeinnicht.bluetooth;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.text.DecimalFormat;

import android.bluetooth.BluetoothDevice;
import android.util.Log;

public class FoundBluetoothDevice {
	
	public BluetoothDevice device;
	public Short rssi;
	String BondStateString;
	public long lastseen;
	double elapsed;
	DecimalFormat df = new DecimalFormat("0.0");
	
	
	/**
	 * Constructor 
	 * @param in_device The found Bluetooth device
	 * @param in_rssi   The RSSI level of the device
	 */
	public FoundBluetoothDevice( BluetoothDevice in_device , Short in_rssi , long in_lastseen){
		this.device = in_device;
		this.rssi = in_rssi;
		this.lastseen = in_lastseen;
		BondStateString = getStringBondState (device.getBondState());
	}
	
	//FIXME: Broken, does not do anything
	
	//TODO: This is not possible from this point in code, remove codeblock
	public int testNewRSSI(){
//		rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
		return -1;
	}
	
	/**
	 * Creates a Human readable String describing the BT device
	 * Is used for the Android ListView
	 */
	@Override
	public String toString(){
		elapsed = (System.currentTimeMillis()-lastseen)/1000.0d;
		return "Name: " + device.getName() + "\t\t Bond:" + BondStateString + "\nADDR: " + device.getAddress()+"\t\t RSSI: "+rssi;
	}
	
	/** compare two Bluetooth devices */
	public boolean equals (BluetoothDevice btd) {
		int diff = btd.getAddress().compareTo(this.device.getAddress());
//		return (diff==0)?true:false; //Wrong way to do this
		return (diff==0);
	}
	
	/** compare two Bluetooth devices */
	public boolean equals (FoundBluetoothDevice fbtd) {
		int diff = fbtd.device.getAddress().compareTo(this.device.getAddress());
		return (diff==0);
	}
	
	/** check if this is our Vergissmeinnicht Tracker  (Vmn = Vergissmeinnicht)*/
	public boolean isVmn () {
		if ( device.getName().compareTo("Vergissmeinnicht") == 0 ) {
			return true;
		} else {
			return false;
		}
	}
	
	/** get the Sting for the Bluetooth Bond status of the Device*/
	private String getStringBondState(int bondState) {
		try {
			return constantToFieldName(device, bondState);
		} catch (Exception e) {
			e.printStackTrace();
			Log.e("constantToFieldName", "Could not find within given fields");
			return "Failed to get Bond name";
		}
	}
	
	/** helper method that gets the Java Field Name for given constant value
	 *  Taken from Burak Özhan universal Library
	 *  */
	public static String constantToFieldName(Object obj, int constant) throws Exception {
		for (Field f : obj.getClass().getDeclaredFields()){
			if (f.getType().isAssignableFrom(Integer.TYPE)){
				int mod = f.getModifiers();
				if (Modifier.isStatic(mod) && Modifier.isPublic(mod) && Modifier.isFinal(mod)) {
					if ( Integer.parseInt(f.get(null).toString()) == constant ){
						return f.getName();
					}
				}  
			}
		}
		throw new Exception ("Could not find within given fields");
	}
}
