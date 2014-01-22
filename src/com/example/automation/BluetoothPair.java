package com.example.automation;

import java.util.ArrayList;
import java.util.Set;
import android.os.Bundle;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import com.seecs.elevenbeedaniyal.automation.R;

public class BluetoothPair extends Activity {

	private static final int REQUEST_ENABLE_BT = 0;
	
	ArrayList<String> StringList;
	ArrayList<BluetoothDevice> DevicesArray = new ArrayList<BluetoothDevice>();
	ArrayAdapter<String> PairedDeviceNames;
	ListView DevicesListVar;
	Button vDiscoverButton;
	
	
	private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver(){
		public void onReceive(Context context,Intent intent)
		{
			String action = intent.getAction();
			
			if (action.equals(BluetoothDevice.ACTION_FOUND))
			{
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				PairedDeviceNames.add(device.getName()+"\n"+device.getAddress());
				DevicesArray.add(device);
				DevicesListVar.refreshDrawableState();
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_bluetooth_pair);
		
		StringList = new ArrayList<String>();
		PairedDeviceNames = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,StringList);
		Set<BluetoothDevice> PairedDevices = MainActivity.mBluetoothAdapter.getBondedDevices();

		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		registerReceiver(mBroadcastReceiver, filter);
		DevicesListVar = (ListView)findViewById(R.id.DevicesList);
		DevicesListVar.setAdapter(PairedDeviceNames);
		DevicesListVar.refreshDrawableState();
		
		vDiscoverButton = (Button) findViewById(R.id.DiscoverButton);
		vDiscoverButton.setOnClickListener(new OnClickListener(){
				public void onClick(View view)
				{
					MainActivity.mBluetoothAdapter.startDiscovery();				
				}	
			}	
		);
		
		DevicesListVar.setOnItemClickListener(new AdapterView.OnItemClickListener(){
				public void onItemClick(AdapterView<?> parent, View view, int position, long id)
				{	
					Intent intent = new Intent("com.example.automation.MainActivity.FIND_BLUETOOTH_HUB");
					intent.putExtra("com.example.automation.MainActivity.FIND_BLUETOOTH_HUB", DevicesArray.get(position));
					setResult(Activity.RESULT_OK,intent);
					finish();
				}	
			}	
		);
	
		setupBluetooth();
		populateArrayAdapter(PairedDevices);
	}


	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		this.unregisterReceiver(mBroadcastReceiver);
		MainActivity.mBluetoothAdapter.cancelDiscovery();
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		
		if (requestCode == REQUEST_ENABLE_BT)
		{
			if (resultCode != RESULT_OK)
			{	
				finish();
			}
		}
	}

	protected void setupBluetooth()
	{
		if (!MainActivity.mBluetoothAdapter.isEnabled())
		{
			Intent enablebt = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enablebt,REQUEST_ENABLE_BT);
		}
	}
	
	public void populateArrayAdapter(Set<BluetoothDevice> PairedDevices)
	{
		if (PairedDevices.size()>0)
		{
			for (BluetoothDevice device : PairedDevices)
			{
				PairedDeviceNames.add(device.getName()+"\n"+device.getAddress());
				DevicesArray.add(device);
			}	
		}
		else
			MainActivity.mBluetoothAdapter.startDiscovery();
		DevicesListVar.refreshDrawableState();	
	}
	
	
	

	
}
