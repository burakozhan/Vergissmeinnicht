package de.ozhan.burak.android.vergissmeinnicht.gui;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import de.ozhan.burak.android.test4.R;

public class ViewOneActivity extends Activity{
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.viewone);

		TextView tv = (TextView) findViewById(R.id.textView1);
		tv.setText(Html.fromHtml(getString(R.string.viewone_introduction)));

		Button b = (Button) findViewById(R.id.configbuttonBeep);
		b.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				Log.i("viewOne", "ClickListener for next item was called");
				Intent myIntent = new Intent(v.getContext(), ViewTwoActivity.class );
				startActivity(myIntent);
				BluetoothAdapter.getDefaultAdapter().disable();
			}
		});
	}
}

