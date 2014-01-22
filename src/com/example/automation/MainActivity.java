package com.example.automation;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.seecs.elevenbeedaniyal.automation.R;

public class MainActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener{

	public static final int ENABLED = 1;
	public static final int DISABLED = 0;
	//String[] menuOptions = {"CameraActivity","GeyserPopup","WaterTank","Room1Activity"};
	public static BluetoothAdapter mBluetoothAdapter;
	public final int FIND_BLUETOOTH_HUB = 10;
	private Handler uiHandler;
	private BluetoothThread commthread;
	static String TAG = "AutoTagMain";
	final String ROOM1_LIGHT_PREF = "room1_light_preference";
	final String ROOM1_FAN_PREF = "room1_fan_preference";
	final String ROOM2_LIGHT_PREF = "room2_light_preference";
	final String ROOM2_FAN_PREF = "room2_fan_preference";
	final String MOTOR_PREF = "motor_control_preference";
	final String ROOM1_AUTO_PREF = "room1_auto_preference";
	final String ROOM2_AUTO_PREF = "room2_auto_preference";
	final String GARDEN_PREF = "garden_water_control_preference";
	final String GEYSER_PREF = "geyser_preference";
	final static String GATE_CONTROL = "gatecontrol";
	final static String GATE_PREF = "gate_preference";
	private static final float TemperatureTextSize = 30;
	
	private Timer myTimer;
	static double Temperature_Room1=0;
	static double Temperature_Room2=0;
	static double Water_Height=0;
	
	static Boolean Room2= false;
	static Boolean Room1= false;
	
	static PreferencesMenu frag = null;
	static WaterLevelFragment waterLevelFragment = null;
	static TextView TemperatureView=null;
	
private Runnable GetValues = new Runnable(){
		
		public void run(){
		
			if (commthread!=null)
			{
				Temperature_Room1 = commthread.SendRecieve("t");
				Temperature_Room2 = commthread.SendRecieve("u");
				Water_Height = commthread.SendRecieve("d");
			}
			if (waterLevelFragment!=null)
			{
				waterLevelFragment.setLevel(Water_Height);			
			}
			
			
			if (Room1==true)
			{
				if (TemperatureView!=null)
				{
					TemperatureView.setText(String.valueOf(Temperature_Room1));
				}
				return;
			}
			if (Room2==true)
			{
				if (TemperatureView!=null)
				{
					TemperatureView.setText(String.valueOf(Temperature_Room2));
				}
				return;
			}
			
		}
	};
	
	
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
		
	}

	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		
		
		switch(item.getItemId
				())
		{
			case R.id.action_search:
				commthread.disconnect();
				
				SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
				SharedPreferences.Editor editor = sharedPref.edit();
				editor.putBoolean(getString(R.string.fragment_enable_preference), false);
				editor.commit();
				
				Intent intent = new Intent(this,com.example.automation.BluetoothPair.class);
				startActivityForResult(intent,FIND_BLUETOOTH_HUB);
				return true;
				
			default:
				return super.onOptionsItemSelected(item);
		}
		
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		

	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.main, menu);
	    
	    TemperatureView = (TextView)menu.findItem(R.id.temperature_textview).getActionView();
	    

		TemperatureView.setText("0.0");
        TemperatureView.setTextColor(Color.RED);
        TemperatureView.setTextSize(TemperatureTextSize);
	    
		return super.onCreateOptionsMenu(menu);
		
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
	}


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		uiHandler = new Handler();
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
	
		PreferenceManager.setDefaultValues(this, R.layout.automation_menu,false);
		
		Log.d(TAG, "[DEBUG]Setting fragment enabled preference default");
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putBoolean(getString(R.string.fragment_enable_preference), false);
		editor.apply();
		Log.d(TAG, "[DEBUG]Set fragment enabled preference default");
		
		Log.d(TAG, "[DEBUG]Setting gate preference default");
		editor.putBoolean(GATE_PREF, false);
		editor.apply();
		Log.d(TAG, "[DEBUG]Set gate preference default");

		Log.d(TAG, "[DEBUG]Setting garden preference default");
		editor.putBoolean(GARDEN_PREF, false);
		editor.apply();
		Log.d(TAG, "[DEBUG]Set garden preference default");

		
		
		if (frag!=null)
		{
			Log.d(TAG, "[DEBUG]Setting summary geyser preference");
			Preference connectionPref = frag.findPreference(GEYSER_PREF);
	        // Set summary to be the user-description for the selected value
	        connectionPref.setSummary(sharedPreferences.getString(GEYSER_PREF, "abc"));
		}
		
		myTimer = new Timer();
		myTimer.schedule(new TimerTask()
		{
		
			@Override
			public void run(){
				
				TaskforTimer();
			}
			
		}, 0, 10000);//0 milliseconds wait till first iteration and 10000 ms = 10 second delay between successive iterations
		
        
		
		FragmentTransaction  transaction = getFragmentManager().beginTransaction();
		transaction.replace(R.id.preferences_menu, (Fragment)new DisconnectedFragment());
		transaction.commit();
		
		
        Log.d(TAG, "[DEBUG]launching intent");
		Intent intent = new Intent(this,com.example.automation.BluetoothPair.class);
		startActivityForResult(intent,FIND_BLUETOOTH_HUB);
		
	}

	public void TaskforTimer()
	{
		this.runOnUiThread(GetValues);
	}
	
	
	
	public Handler returnHandler()
	{
		return uiHandler;
	} 


	static void toggleGate(Context ctx)
	{
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(ctx);
		SharedPreferences.Editor editor = sharedPreferences.edit();
		Boolean current = sharedPreferences.getBoolean(GATE_PREF, false);
		
		if (current == false)
		{
			editor.putBoolean(GATE_PREF, true);
			editor.apply();
			return;
		}
		
		else if (current == true)
		{
			editor.putBoolean(GATE_PREF, false);
			editor.apply();
			return;
		}
	}
	
	//Logic of the following two methods is that when these flags would be set, the next call to timer will display proper temperature.
	static void room2(Context ctx) //method to configure the actionbar textview to display room 2 temperature
	{
		Room2=true;
		Room1=false;
		
		if (TemperatureView!=null)
		{
			TemperatureView.setText(String.valueOf(Temperature_Room2));
		}
	}
	
	static void room1(Context ctx) //method to configure the actionbar textview to display room 1 temperature
	{
		Room1=true;
		Room2=false;
		if (TemperatureView!=null)
		{
			TemperatureView.setText(String.valueOf(Temperature_Room1));
		}
	}
	

	static void SwtichWaterLevelFragment(Context ctx) //method to switch fragment to display water level
	{
		 FragmentTransaction  transaction = ((Activity) ctx).getFragmentManager().beginTransaction();
		 transaction.replace(R.id.preferences_menu, new WaterLevelFragment());
		 transaction.addToBackStack(null);
		 transaction.commit();
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		
		if (sharedPreferences.getBoolean(getString(R.string.fragment_enable_preference),false)==true)
		{
			
	      if (key.equals(GEYSER_PREF)) {
	    	  
	    	  if (commthread==null)//to check whether bluetooth thread is active
	            {
	    		  return;
	            }
	    	  
	    	  	if(frag==null) // to check whether the frag object has correct value
	    	  	{
	    	  		Log.d(TAG, "[ERROR]frag null even when preference enabled[GEYSER_PREF_CHANGE]");
	    	  		return;
	    	  	}
	    	  		
	            ListPreference geyserPref = (ListPreference)frag.findPreference(key);
	            // Set summary to be the user-description for the selected value
	            geyserPref.setSummary(geyserPref.getEntry());
	            String value = geyserPref.getValue();
	            
	            if (value.equals("0"))
	            {
	            	commthread.sendMessage("S");
	            	return;
	            }
	            
	            if (value.equals("1"))
	            {
	            	commthread.sendMessage("K");
	            	return;
	            }
	            
	            if (value.equals("2"))
	            {
	            	commthread.sendMessage("P");
	            	return;
	            }
	            
	            if (value.equals("3"))
	            {
	            	commthread.sendMessage("W");
	            	return;
	            }
	            
	            if (value.equals("4"))
	            {
	            	commthread.sendMessage("X");
	            	return;
	            }
	            
	            if (value.equals("5"))
	            {
	            	commthread.sendMessage("Y");
	            	return;
	            }
	            
	            if (value.equals("6"))
	            {
	            	commthread.sendMessage("V");
	            	return;
	            }      
	            
	            //[DEBUG]
	            //[DEBUG]
	            return;
	        }
	      
	      if (key.equals(GARDEN_PREF)) {
	            if (commthread!=null)
	            {
	            	Boolean answer = sharedPreferences.getBoolean(key, false);
	            	if (answer==true)
	            		commthread.sendMessage("G");
	            	else
	            		commthread.sendMessage("J");
	            }
	            return;
	        }
	      
	      if (key.equals(ROOM1_AUTO_PREF)) {
	            if (commthread!=null)
	            {
	            	Boolean answer = sharedPreferences.getBoolean(key, false);
	            	if (answer==true)
	            		commthread.sendMessage("1");
	            	else
	            		commthread.sendMessage("2");
	            }
	            return;
	        }
	      
	      if (key.equals(ROOM2_AUTO_PREF)) {
	            if (commthread!=null)
	            {
	            	Boolean answer = sharedPreferences.getBoolean(key, false);
	            	if (answer==true)
	            		commthread.sendMessage("3");
	            	else
	            		commthread.sendMessage("4");
	            }
	            return;
	        }
	      
	      
	      if (key.equals(MOTOR_PREF)) {
	            if (commthread!=null)
	            {
	            	Boolean answer = sharedPreferences.getBoolean(key, false);
	            	if (answer==true)
	            		commthread.sendMessage("M");
	            	else
	            		commthread.sendMessage("N");
	            }
	            return;
	        }
	      
	      
	      if (key.equals(ROOM1_LIGHT_PREF)) {
	            if (commthread!=null)
	            {
	            	Boolean answer = sharedPreferences.getBoolean(key, false);
	            	if (answer==true)
	            		commthread.sendMessage("L");
	            	else
	            		commthread.sendMessage("I");
	            }
	            return;
	        }
	      if (key.equals(ROOM2_LIGHT_PREF)) {
	            if (commthread!=null)
	            {
	            	Boolean answer = sharedPreferences.getBoolean(key, false);
	            	if (answer==true)
	            		commthread.sendMessage("A");
	            	else
	            		commthread.sendMessage("B");
	            }
	            return;
	        }
	      if (key.equals(ROOM1_FAN_PREF)) {
	            if (commthread!=null)
	            {
	            	Boolean answer = sharedPreferences.getBoolean(key, false);
	            	if (answer==true)
	            		commthread.sendMessage("F");
	            	else
	            		commthread.sendMessage("H");
	            }
	            return;
	        }

	      if (key.equals(ROOM2_FAN_PREF)) {
	            if (commthread!=null)
	            {
	            	Boolean answer = sharedPreferences.getBoolean(key, false);
	            	if (answer==true)
	            		commthread.sendMessage("C");
	            	else
	            		commthread.sendMessage("D");
	            }
	            return;
	        }
	      if (key.equals(GATE_PREF)) {
	            if (commthread!=null)
	            {
	            	boolean answer = sharedPreferences.getBoolean(key, false);
	            	if (answer==true)
	            		commthread.sendMessage("O");
	            	else
	            		commthread.sendMessage("Z");
	            }
	            return;
	        }
	      
		}
        
        if (key.equals(getString(R.string.fragment_enable_preference))) {
        	Log.d(TAG, "[DEBUG]fragment enable preference changed");
           FragmentTransaction  transaction = getFragmentManager().beginTransaction();
           boolean answer = sharedPreferences.getBoolean(getString(R.string.fragment_enable_preference), true);
           if (answer == true)
           {
        	   Log.d(TAG, "[DEBUG]attaching Preference menu");
        	   frag = new PreferencesMenu();
        	   frag.setContext(this);
        	   transaction.replace(R.id.preferences_menu, frag);
           } 
           else if (answer == false)
           {
        	   Log.d(TAG, "[DEBUG]attching Disconnect fragment");
        	   transaction.replace(R.id.preferences_menu, (Fragment)new DisconnectedFragment());
           }
           transaction.commit();
          //[DEBUG] TemperatureView.setText("45.0");
           //[DEBUG]TemperatureView.setTextColor(Color.RED);
           
        }
    }

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		
		if (resultCode == RESULT_OK)
		{
			switch(requestCode)
			{
			case (FIND_BLUETOOTH_HUB):
				final BluetoothDevice device = data.getParcelableExtra("com.example.automation.MainActivity.FIND_BLUETOOTH_HUB");
				
				//Toast toast = Toast.makeText(this, device.getName(), Toast.LENGTH_LONG); //[DEBUG]
				//toast.show();
				//toast = Toast.makeText(this, device.getAddress(),Toast.LENGTH_LONG); //[DEBUG]
				//toast.show();
				mBluetoothAdapter.cancelDiscovery();
				Log.d(TAG, "creating bluetooth thread");
				commthread = new BluetoothThread(device,this);
				Log.d(TAG, "starting bluetooth thread");
				commthread.start();

				break;	
			}
		}
	}
	
	
	public static class PreferencesMenu extends PreferenceFragment{
		
		Context context = null;
		
		void setContext(Context ctx)
		{
			context = ctx;
		}
		
		@Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Can retrieve arguments from preference XML.
            Log.i("args", "Arguments: " + getArguments());

            frag = this;
            // Load the preferences from an XML resource
            addPreferencesFromResource(R.layout.automation_menu);
            
            //Setting onClickListener for gate preference
            Preference gate = this.findPreference(GATE_CONTROL);
            gate.setOnPreferenceClickListener(new OnPreferenceClickListener(){

				@Override
				public boolean onPreferenceClick(Preference preference) {
					// TODO Auto-generated method stub
					
					if (preference.getKey().equals(GATE_CONTROL))
					{
						toggleGate(context);
					}
					return false;
				}
            	
            });
            
            //Setting onClickListener for room1 preference for temperature display
            
            Preference room2 = this.findPreference("room2_screen_preference");
            room2.setOnPreferenceClickListener(new OnPreferenceClickListener(){

				@Override
				public boolean onPreferenceClick(Preference preference) {
					// TODO Auto-generated method stub
					
					if (preference.getKey().equals("room2_screen_preference"))
					{
						room2(context);
					}
					return false;
				}
            	
            });

          //Setting onClickListener for room1 preference for temperature display
            
            Preference room1 = this.findPreference("room1_screen_preference");
            room1.setOnPreferenceClickListener(new OnPreferenceClickListener(){

				@Override
				public boolean onPreferenceClick(Preference preference) {
					// TODO Auto-generated method stub
					
					if (preference.getKey().equals("room1_screen_preference"))
					{
						room1(context);
					}
					return false;
				}
            	
            });


            //Setting onclick listener for water level preference
            Preference WaterLevel = this.findPreference("water_level_preference");
            WaterLevel.setOnPreferenceClickListener(new OnPreferenceClickListener(){

				@Override
				public boolean onPreferenceClick(Preference preference) {
					// TODO Auto-generated method stub
					
					if (preference.getKey().equals("water_level_preference"))
					{
						SwtichWaterLevelFragment(context);
					}
					return false;
				}
            	
            });
            
        
		}
		

		
		
		@Override
		public void onPause() {
			// TODO Auto-generated method stub
			frag = null;
			super.onPause();
			
		}

		@Override
		public void onResume() {
			// TODO Auto-generated method stub
			frag = this;
			super.onResume();
			
		}

		
		
	}

	public static class DisconnectedFragment extends Fragment{
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
								 Bundle savedInstanceState) {
			// Inflate the layout for this fragment
			return inflater.inflate(R.layout.disconnected_fragment, container, false);
		}
	}
	
	public static class WaterLevelFragment extends Fragment{
		static TextView WaterLevelDisplay = null;
		
		public static void setLevel(double level)
		{
			if (WaterLevelDisplay != null)
				WaterLevelDisplay.setText(String.valueOf(level));
		}
		
		@Override
		public void onPause() {
			// TODO Auto-generated method stub
			waterLevelFragment = null;
			super.onPause();
		}

		@Override
		public void onResume() {
			// TODO Auto-generated method stub
			waterLevelFragment = this;
			super.onResume();
		}

		public View onCreateView(LayoutInflater inflater, ViewGroup container,
								 Bundle savedInstanceState) {
			// Inflate the layout for this fragment
			View v = inflater.inflate(R.layout.water_level_fragment, container, false);
			WaterLevelDisplay = (TextView) v.findViewById(R.id.water_level_textview);
			WaterLevelDisplay.setText(String.valueOf(Water_Height));
			return v;
		}
	}
		
}
	

class BluetoothThread extends Thread
{

	BluetoothDevice device = null;
	String uuid = "00001101-0000-1000-8000-00805f9b34fb";
	BluetoothSocket socket = null;
	String TagThread = "BluetoothThread";
	OutputStream out = null;
	InputStream in = null;
	Context context = null;
	final int CONNECTED = 1;
	final int DISCONNECTED = 0;
	
	Boolean status = false;
	
	public BluetoothThread(BluetoothDevice Device, Context ctx) {
		super();
		// TODO Auto-generated constructor stub
		device = Device;
		context = ctx;
		
	}
	
	
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		super.run();
		connectToDevice();
	}



	public void connectToDevice()
	{
		
		final BluetoothDevice dev = device;
		BluetoothSocket tmp = null;
	
		
		
		//Check for dangling nulls
		if (dev == null)
			return;
		
		
		Log.d(TagThread,"creating Rfcomm channel");
		try
		{
			tmp = dev.createRfcommSocketToServiceRecord(UUID.fromString(uuid));
		}
		catch (IOException e)
		{
			return;
		}
		Log.d(TagThread,"Rfcomm channel created successfully. Now connecting");
		socket = tmp;
	
		
		
		//[DEBUG] [DEBUG]
		//SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
		//SharedPreferences.Editor editor = sharedPref.edit();
		//editor.putBoolean(context.getString(R.string.fragment_enable_preference), true);
		//editor.commit();
		//[DEBUG] [DEBUG]
		
		
		if (socket!=null)
		{
		try
		{
			if (socket!=null)
			{
				socket.connect();
			}
		}
		catch(IOException connectException)
		{
			try
			{
				socket.close();
			}
			catch(IOException e)
			{}
			return;
		}
	
		Log.d(TagThread,"Connected");
		}	
		manageConnection();
	}

	public void manageConnection()
	{
		
		OutputStream tmp = null;
		InputStream temp2 = null;
		
		if (socket!=null){
		try{
			tmp = socket.getOutputStream();
		}
		catch(IOException e)
		{
			return;
		}
		try{
			temp2 = socket.getInputStream();
		}
		catch(IOException e)
		{
			return;
		}
		
	}
		out = tmp;
		in = temp2;
		
		
		//Connected!!!!!
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor editor = sharedPref.edit();
		editor.putBoolean(context.getString(R.string.fragment_enable_preference), true);
		editor.apply();
		
		status = true;
		//Now sync state
		sync();
	
		
		//[DEBUG]
		
	}

	public void sync()
	{
		if (status == false)
			return;
		
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor editor = sharedPreferences.edit();
		
		Boolean value = sharedPreferences.getBoolean("gate_preference", false);
		editor.putBoolean("gate_preference", value);
			
		value = sharedPreferences.getBoolean("room1_light_preference", false);
		editor.putBoolean("room1_light_preference", value);
		
		value = sharedPreferences.getBoolean("room2_light_preference", false);
		editor.putBoolean("room2_light_preference", value);
		
		value = sharedPreferences.getBoolean("room2_fan_preference", false);
		editor.putBoolean("room2_fan_preference", value);
		
		value = sharedPreferences.getBoolean("room1_fan_preference", false);
		editor.putBoolean("room1_fan_preference", value);
	
		value = sharedPreferences.getBoolean("room1_auto_preference", false);
		editor.putBoolean("room1_auto_preference", value);
		
		value = sharedPreferences.getBoolean("room2_auto_preference", false);
		editor.putBoolean("room2_auto_preference", value);
		
		value = sharedPreferences.getBoolean("garden_water_control_preference", false);
		editor.putBoolean("garden_water_control_preference", value);
		
		value = sharedPreferences.getBoolean("motor_control_preference", false);
		editor.putBoolean("motor_control_preference", value);
		
		editor.apply();
		
	}
	
	public void sendMessage(String message)
	{
		Log.d(TagThread, "sendMessage"+message);
		if (out!=null)
		{
			try {
				out.write(message.getBytes());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				Log.d(TagThread, "disconnecting");
				disconnect();
				return;
			}
			Log.d(TagThread, "Done sending "+message);
		}
	}
	
	public double SendRecieve(String message)
	{
		double output=0;
		if (out!=null)
		{
			try {
				out.write(message.getBytes());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				disconnect();
				return 0;
			}
		}
		
		
		if (in!=null)
		{
		try {
			output = in.read();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return 0;
		}
		}
		return output;
	}
	
	public void disconnect()
	{
		
		status = false;
		if (out != null)
		{
			try {
				out.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				Log.d(TagThread, "In disconnect error closing stream");
			}
		}
		if (socket!=null)
		{
			try {
				socket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				Log.d(TagThread, "error closing socket");
			}
		}
		
		socket = null;
		device = null;
		out = null;
		
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor editor = sharedPref.edit();
		editor.putBoolean(context.getString(R.string.fragment_enable_preference), false);
		editor.apply();
	
	}
}
