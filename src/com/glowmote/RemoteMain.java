package com.glowmote;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class RemoteMain extends Activity implements
ColorPickerDialog.OnColorChangedListener{
	
	public static final String TAG = "GlowMote";
	private final static int REQUEST_ENABLE_BT = 1;
	private BluetoothAdapter mBluetoothAdapter = null;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    OutputStream mmOutputStream;
    InputStream mmInputStream;
    Thread workerThread;
    byte[] readBuffer;
    int readBufferPosition;
    volatile boolean stopWorker;
	private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
	//private static String address = "98:D3:31:50:0A:B2";
	private static String address = "20:13:05:16:63:85";
	private boolean deviceConnected = false, musicOn, cycleOn, powerOn;
	IntentFilter filter;
	Button menuBtn, brightnessUpBtn, brightnessDownBtn, flashBtn, stopFlashBtn, cycleBtn;
	ImageView connectionStatus;
	ToggleButton powerBtn, musicBtn;
	TextView cycleSpeedText;
	private int brightnessLvl, currentFlash, cycleSpeed;
	private String cycleStyle;
	final Context context = this;
	Toast msg;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		setContentView(R.layout.activity_remote_main);
		
		// Register the BroadcastReceiver
		filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
		filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
		registerReceiver(mReceiver, filter);
		
		checkBtStatus();
		
		powerBtn = (ToggleButton) findViewById(R.id.power_button);
		powerOn = PreferenceManager.getDefaultSharedPreferences(RemoteMain.this).getBoolean("powerOn", true);
		Button redBtn = (Button) findViewById(R.id.button1);
		Button orangeBtn1 = (Button) findViewById(R.id.button2);
		Button orangeBtn2 = (Button) findViewById(R.id.button3);
		Button orangeBtn3 = (Button) findViewById(R.id.button4);
		Button yellowBtn = (Button) findViewById(R.id.button5);
		Button greenBtn1 = (Button) findViewById(R.id.button6);
		Button greenBtn2 = (Button) findViewById(R.id.button7);
		Button greenBtn3 = (Button) findViewById(R.id.button8);
		Button greenBtn4 = (Button) findViewById(R.id.button9);
		Button blueBtn1 = (Button) findViewById(R.id.button10);
		Button blueBtn2 = (Button) findViewById(R.id.button11);
		Button blueBtn3 = (Button) findViewById(R.id.button12);
		Button purpleBtn1 = (Button) findViewById(R.id.button13);
		Button purpleBtn2 = (Button) findViewById(R.id.button14);
		Button purpleBtn3 = (Button) findViewById(R.id.button15);
		Button purpleBtn4 = (Button) findViewById(R.id.button16);
		Button pinkBtn1 = (Button) findViewById(R.id.button17);
		Button pinkBtn2 = (Button) findViewById(R.id.button18);
		Button whiteBtn = (Button) findViewById(R.id.button19);
		menuBtn = (Button) findViewById(R.id.menuBtn);
		connectionStatus = (ImageView) findViewById(R.id.connectionImage);
		musicBtn = (ToggleButton) findViewById(R.id.musicBtn);
		musicOn = PreferenceManager.getDefaultSharedPreferences(RemoteMain.this).getBoolean("musicOn", false);
		brightnessUpBtn = (Button) findViewById(R.id.button21);
		brightnessDownBtn = (Button) findViewById(R.id.button22);
		brightnessLvl = PreferenceManager.getDefaultSharedPreferences(RemoteMain.this).getInt("brightness", 5);
		flashBtn = (Button) findViewById(R.id.button23);
		stopFlashBtn = (Button) findViewById(R.id.stopBtn);
		currentFlash = PreferenceManager.getDefaultSharedPreferences(RemoteMain.this).getInt("flash", 0);
		cycleBtn = (Button) findViewById(R.id.button20);
		cycleOn = PreferenceManager.getDefaultSharedPreferences(RemoteMain.this).getBoolean("cycleOn", false);
		cycleStyle = PreferenceManager.getDefaultSharedPreferences(RemoteMain.this).getString("cycleStyle", "fade");
		cycleSpeed = PreferenceManager.getDefaultSharedPreferences(RemoteMain.this).getInt("cycleSpeed", 5);
		
		powerBtn.setClickable(false);
		musicBtn.setClickable(false);
		brightnessUpBtn.setClickable(false);
		brightnessDownBtn.setClickable(false);
		flashBtn.setClickable(false);
		stopFlashBtn.setClickable(false);
		cycleBtn.setClickable(false);
		
		stopFlashBtn.setVisibility(View.INVISIBLE);
		
		powerBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
		    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		    	if (isChecked && deviceConnected) {
		    		powerOn = true;
		    		PreferenceManager.getDefaultSharedPreferences(RemoteMain.this).edit().putBoolean("powerOn", powerOn).commit();
		    		/*
		    		if (brightnessLvl == 5) {
		    			writeData(getLastColor());
		    		}
		    		else {
		    			setBrightnessLvl(getLastColor(), "none");
		    		}
		    		*/
		    		if (musicOn) {
		    			musicBtn.setChecked(true);
		    			//writeData("music_on/");
		    			if (brightnessLvl == 5) {                       	
			    			writeData(getLastMusicColor());				    
			    		}													
			    		else {												
			    			setBrightnessLvl(getLastMusicColor(), "none");	
			    		}													
		    		}
		    		else {
		    			musicBtn.setChecked(false);
		    			//writeData("music_off/");
		    			if (brightnessLvl == 5) {                       
			    			writeData(getLastColor());				    
			    		}												
			    		else {											
			    			setBrightnessLvl(getLastColor(), "none");	
			    		}												
		    		}
		    		if (currentFlash != 0) {
		    			flashBtn.setBackgroundResource(R.drawable.flash_pattern_button_on);
		    			stopFlashBtn.setVisibility(View.VISIBLE);
		    			switch (currentFlash) {
		    			case 1:
		    				writeData("flash1/");
		    				break;
		    			case 2:
		    				writeData("flash2/");
		    				break;
		    			case 3:
		    				writeData("flash3/");
		    				break;
		    			case 4:
		    				writeData("flash4/");
		    				break;
		    			case 5:
		    				writeData("flash5/");
		    				break;
		    			case 6:
		    				writeData("flash6/");
		    				break;
		    			}
		    		}
		    		if (cycleOn) {
		    			cycleBtn.setBackgroundResource(R.drawable.cycle_button_on);
		    			brightnessUpBtn.setClickable(false);
			    		brightnessDownBtn.setClickable(false);
		    			turnCycleOn();
		    		}
		    		else {
		    			cycleBtn.setBackgroundResource(R.drawable.cycle_button);
		    			brightnessUpBtn.setClickable(true);
			    		brightnessDownBtn.setClickable(true);
		    		}
		    		musicBtn.setClickable(true);
		    		flashBtn.setClickable(true);
		    		stopFlashBtn.setClickable(true);
		    		cycleBtn.setClickable(true);
		    	}
		    	else if (!isChecked && deviceConnected) {
		    		powerOn = false;
		    		PreferenceManager.getDefaultSharedPreferences(RemoteMain.this).edit().putBoolean("powerOn", powerOn).commit();
		            msg.cancel();
		    		writeData("000000000/");
		            flashBtn.setBackgroundResource(R.drawable.flash_pattern_button);
		            stopFlashBtn.setVisibility(View.INVISIBLE);
		            musicBtn.setChecked(false);
		            cycleBtn.setBackgroundResource(R.drawable.cycle_button);
		        	musicBtn.setClickable(false);
		    		brightnessUpBtn.setClickable(false);
		    		brightnessDownBtn.setClickable(false);
		    		flashBtn.setClickable(false);
		    		stopFlashBtn.setClickable(false);
		    		cycleBtn.setClickable(false);
		        }
		    }
		});
		
		redBtn.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if(event.getAction()==MotionEvent.ACTION_DOWN){
					if (deviceConnected && powerBtn.isChecked()) {
						if (cycleOn) {
							cycleOn = false;
							cycleBtn.setBackgroundResource(R.drawable.cycle_button);
							brightnessUpBtn.setClickable(true);
				    		brightnessDownBtn.setClickable(true);
							PreferenceManager.getDefaultSharedPreferences(RemoteMain.this).edit().putBoolean("cycleOn", cycleOn).commit();
						}
						if (brightnessLvl == 5) {
							writeData("255000000/");  // red/green/blue
						}
						else {
							setBrightnessLvl("255000000/", "none");
						}
						saveLastColor("255000000/");
						saveLastMusicColor("255000000/"); 
					}
				}
				return false;
			}
		});
		orangeBtn1.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if(event.getAction()==MotionEvent.ACTION_DOWN){
					if (deviceConnected && powerBtn.isChecked()) {
						if (cycleOn) {
							cycleOn = false;
							cycleBtn.setBackgroundResource(R.drawable.cycle_button);
							brightnessUpBtn.setClickable(true);
				    		brightnessDownBtn.setClickable(true);
							PreferenceManager.getDefaultSharedPreferences(RemoteMain.this).edit().putBoolean("cycleOn", cycleOn).commit();
						}
						if (brightnessLvl == 5) {
							if (musicOn) {
								writeData("240020000/");
							}
							else {
								writeData("255020000/");  // red/green/blue
							}
						}
						else {
							if (musicOn) {
								setBrightnessLvl("240020000/", "none");
							}
							else {
								setBrightnessLvl("255020000/", "none");
							}
						}
						saveLastColor("255020000/");
						saveLastMusicColor("240020000/");  
					}
				}
				return false;
			}
		});
		orangeBtn2.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if(event.getAction()==MotionEvent.ACTION_DOWN){
					if (deviceConnected && powerBtn.isChecked()) {
						if (cycleOn) {
							cycleOn = false;
							cycleBtn.setBackgroundResource(R.drawable.cycle_button);
							brightnessUpBtn.setClickable(true);
				    		brightnessDownBtn.setClickable(true);
							PreferenceManager.getDefaultSharedPreferences(RemoteMain.this).edit().putBoolean("cycleOn", cycleOn).commit();
						}
						if (brightnessLvl == 5) {
							if (musicOn) {
								writeData("240050000/");
							}
							else {
								writeData("255050000/");  // red/green/blue
							}
						}
						else {
							if (musicOn) {
								setBrightnessLvl("240050000/", "none");
							}
							else {
								setBrightnessLvl("255050000/", "none");
							}
						}
						saveLastColor("255050000/");
						saveLastMusicColor("240050000/");  
					}
				}
				return false;
			}
		});
		orangeBtn3.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if(event.getAction()==MotionEvent.ACTION_DOWN){
					if (deviceConnected && powerBtn.isChecked()) {
						if (cycleOn) {
							cycleOn = false;
							cycleBtn.setBackgroundResource(R.drawable.cycle_button);
							brightnessUpBtn.setClickable(true);
				    		brightnessDownBtn.setClickable(true);
							PreferenceManager.getDefaultSharedPreferences(RemoteMain.this).edit().putBoolean("cycleOn", cycleOn).commit();
						}
						if (brightnessLvl == 5) {
							if (musicOn) {
								writeData("240100000/");
							}
							else {
								writeData("255100000/");  // red/green/blue
							}
						}
						else {
							if (musicOn) {
								setBrightnessLvl("240100000/", "none");
							}
							else {
								setBrightnessLvl("255100000/", "none");
							}
						}
						saveLastColor("255100000/");
						saveLastMusicColor("240100000/");  
					}
				}
				return false;
			}
		});
		yellowBtn.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if(event.getAction()==MotionEvent.ACTION_DOWN){
					if (deviceConnected && powerBtn.isChecked()) {
						if (cycleOn) {
							cycleOn = false;
							cycleBtn.setBackgroundResource(R.drawable.cycle_button);
							brightnessUpBtn.setClickable(true);
				    		brightnessDownBtn.setClickable(true);
							PreferenceManager.getDefaultSharedPreferences(RemoteMain.this).edit().putBoolean("cycleOn", cycleOn).commit();
						}
						if (brightnessLvl == 5) {
							writeData("255150000/");  // red/green/blue
						}
						else {
							setBrightnessLvl("255150000/", "none");
						}
						saveLastColor("255150000/");
						saveLastMusicColor("255150000/"); 
					}
				}
				return false;
			}
		});
		greenBtn1.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if(event.getAction()==MotionEvent.ACTION_DOWN){
					if (deviceConnected && powerBtn.isChecked()) {
						if (cycleOn) {
							cycleOn = false;
							cycleBtn.setBackgroundResource(R.drawable.cycle_button);
							brightnessUpBtn.setClickable(true);
				    		brightnessDownBtn.setClickable(true);
							PreferenceManager.getDefaultSharedPreferences(RemoteMain.this).edit().putBoolean("cycleOn", cycleOn).commit();
						}
						if (brightnessLvl == 5) {
							writeData("156255000/");  // red/green/blue
						}
						else {
							setBrightnessLvl("156255000/", "none");
						}
						saveLastColor("156255000/");
						saveLastMusicColor("156255000/"); 
					}
				}
				return false;
			}
		});
		greenBtn2.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if(event.getAction()==MotionEvent.ACTION_DOWN){
					if (deviceConnected && powerBtn.isChecked()) {
						if (cycleOn) {
							cycleOn = false;
							cycleBtn.setBackgroundResource(R.drawable.cycle_button);
							brightnessUpBtn.setClickable(true);
				    		brightnessDownBtn.setClickable(true);
							PreferenceManager.getDefaultSharedPreferences(RemoteMain.this).edit().putBoolean("cycleOn", cycleOn).commit();
						}
						if (brightnessLvl == 5) {
							writeData("090255000/");  // red/green/blue
						}
						else {
							setBrightnessLvl("090255000/", "none");
						}
						saveLastColor("090255000/");
						saveLastMusicColor("090255000/");
					}
				}
				return false;
			}
		});
		greenBtn3.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if(event.getAction()==MotionEvent.ACTION_DOWN){
					if (deviceConnected && powerBtn.isChecked()) {
						if (cycleOn) {
							cycleOn = false;
							cycleBtn.setBackgroundResource(R.drawable.cycle_button);
							brightnessUpBtn.setClickable(true);
				    		brightnessDownBtn.setClickable(true);
							PreferenceManager.getDefaultSharedPreferences(RemoteMain.this).edit().putBoolean("cycleOn", cycleOn).commit();
						}
						if (brightnessLvl == 5) {
							writeData("000255000/");  // red/green/blue
						}
						else {
							setBrightnessLvl("000255000/", "none");
						}
						saveLastColor("000255000/");
						saveLastMusicColor("000255000/");  
					}
				}
				return false;
			}
		});
		greenBtn4.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if(event.getAction()==MotionEvent.ACTION_DOWN){
					if (deviceConnected && powerBtn.isChecked()) {
						if (cycleOn) {
							cycleOn = false;
							cycleBtn.setBackgroundResource(R.drawable.cycle_button);
							brightnessUpBtn.setClickable(true);
				    		brightnessDownBtn.setClickable(true);
							PreferenceManager.getDefaultSharedPreferences(RemoteMain.this).edit().putBoolean("cycleOn", cycleOn).commit();
						}
						if (brightnessLvl == 5) {
							writeData("000255060/");  // red/green/blue
						}
						else {
							setBrightnessLvl("000255060/", "none");
						}
						saveLastColor("000255060/");
						saveLastMusicColor("000255060/"); 
					}
				}
				return false;
			}
		});
		blueBtn1.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if(event.getAction()==MotionEvent.ACTION_DOWN){
					if (deviceConnected && powerBtn.isChecked()) {
						if (cycleOn) {
							cycleOn = false;
							cycleBtn.setBackgroundResource(R.drawable.cycle_button);
							brightnessUpBtn.setClickable(true);
				    		brightnessDownBtn.setClickable(true);
							PreferenceManager.getDefaultSharedPreferences(RemoteMain.this).edit().putBoolean("cycleOn", cycleOn).commit();
						}
						if (brightnessLvl == 5) {
							if (musicOn) {
								writeData("000255100/");
							}
							else {
								writeData("000255222/");  // red/green/blue
							}
						}
						else {
							if (musicOn) {
								setBrightnessLvl("000255061/", "none");
							}
							else {
								setBrightnessLvl("000255222/", "none");
							}
						}
						saveLastColor("000255222/");
						saveLastMusicColor("000255100/");  
					}
				}
				return false;
			}
		});
		blueBtn2.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if(event.getAction()==MotionEvent.ACTION_DOWN){
					if (deviceConnected && powerBtn.isChecked()) {
						if (cycleOn) {
							cycleOn = false;
							cycleBtn.setBackgroundResource(R.drawable.cycle_button);
							brightnessUpBtn.setClickable(true);
				    		brightnessDownBtn.setClickable(true);
							PreferenceManager.getDefaultSharedPreferences(RemoteMain.this).edit().putBoolean("cycleOn", cycleOn).commit();
						}
						if (brightnessLvl == 5) {
							if (musicOn) {
								writeData("000180200/");
							}
							else {
								writeData("000180255/");  // red/green/blue
							}
						}
						else {
							if (musicOn) {
								setBrightnessLvl("000180200/", "none");
							}
							else {
								setBrightnessLvl("000180255/", "none");
							}
						}
						saveLastColor("000180255/");
						saveLastMusicColor("000180200/");  
					}
				}
				return false;
			}
		});
		blueBtn3.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if(event.getAction()==MotionEvent.ACTION_DOWN){
					if (deviceConnected && powerBtn.isChecked()) {
						if (cycleOn) {
							cycleOn = false;
							cycleBtn.setBackgroundResource(R.drawable.cycle_button);
							brightnessUpBtn.setClickable(true);
				    		brightnessDownBtn.setClickable(true);
							PreferenceManager.getDefaultSharedPreferences(RemoteMain.this).edit().putBoolean("cycleOn", cycleOn).commit();
						}
						if (brightnessLvl == 5) {
							writeData("000000255/");  // red/green/blue
						}
						else {
							setBrightnessLvl("000000255/", "none");
						}
						saveLastColor("000000255/");
						saveLastMusicColor("000000255/");  
					}
				}
				return false;
			}
		});
		purpleBtn1.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if(event.getAction()==MotionEvent.ACTION_DOWN){
					if (deviceConnected && powerBtn.isChecked()) {
						if (cycleOn) {
							cycleOn = false;
							cycleBtn.setBackgroundResource(R.drawable.cycle_button);
							brightnessUpBtn.setClickable(true);
				    		brightnessDownBtn.setClickable(true);
							PreferenceManager.getDefaultSharedPreferences(RemoteMain.this).edit().putBoolean("cycleOn", cycleOn).commit();
						}
						if (brightnessLvl == 5) {
							if (musicOn) {
								writeData("100000070/");
							}
							else {
								writeData("100000255/");  // red/green/blue
							}
						}
						else {
							if (musicOn) {
								setBrightnessLvl("100000070/", "none");
							}
							else {
								setBrightnessLvl("100000255/", "none");
							}
						}
						saveLastColor("100000255/");
						saveLastMusicColor("100000070/");  
					}
				}
				return false;
			}
		});
		purpleBtn2.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if(event.getAction()==MotionEvent.ACTION_DOWN){
					if (deviceConnected && powerBtn.isChecked()) {
						if (cycleOn) {
							cycleOn = false;
							cycleBtn.setBackgroundResource(R.drawable.cycle_button);
							brightnessUpBtn.setClickable(true);
				    		brightnessDownBtn.setClickable(true);
							PreferenceManager.getDefaultSharedPreferences(RemoteMain.this).edit().putBoolean("cycleOn", cycleOn).commit();
						}
						if (brightnessLvl == 5) {
							if (musicOn) {
								writeData("150000070/");
							}
							else {
								writeData("150000255/");  // red/green/blue
							}
						}
						else {
							if (musicOn) {
								setBrightnessLvl("150000070/", "none");
							}
							else {
								setBrightnessLvl("150000255/", "none");
							}
						}
						saveLastColor("150000255/");
						saveLastMusicColor("150000070/"); 
					}
				}
				return false;
			}
		});
		purpleBtn3.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if(event.getAction()==MotionEvent.ACTION_DOWN){
					if (deviceConnected && powerBtn.isChecked()) {
						if (cycleOn) {
							cycleOn = false;
							cycleBtn.setBackgroundResource(R.drawable.cycle_button);
							brightnessUpBtn.setClickable(true);
				    		brightnessDownBtn.setClickable(true);
							PreferenceManager.getDefaultSharedPreferences(RemoteMain.this).edit().putBoolean("cycleOn", cycleOn).commit();
						}
						if (brightnessLvl == 5) {
							if (musicOn) {
								writeData("190000070/");
							}
							else {
								writeData("190000255/");  // red/green/blue
							}
						}
						else {
							if (musicOn) {
								setBrightnessLvl("190000070/", "none");
							}
							else {
								setBrightnessLvl("190000255/", "none");
							}
						}
						saveLastColor("190000255/");
						saveLastMusicColor("190000070/");
					}
				}
				return false;
			}
		});
		purpleBtn4.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if(event.getAction()==MotionEvent.ACTION_DOWN){
					if (deviceConnected && powerBtn.isChecked()) {
						if (cycleOn) {
							cycleOn = false;
							cycleBtn.setBackgroundResource(R.drawable.cycle_button);
							brightnessUpBtn.setClickable(true);
				    		brightnessDownBtn.setClickable(true);
							PreferenceManager.getDefaultSharedPreferences(RemoteMain.this).edit().putBoolean("cycleOn", cycleOn).commit();
						}
						if (brightnessLvl == 5) {
							if (musicOn) {
								writeData("255000070/");
							}
							else {
								writeData("255000255/");  // red/green/blue
							}
						}
						else {
							if (musicOn) {
								setBrightnessLvl("255000070/", "none");
							}
							else {
								setBrightnessLvl("255000255/", "none");
							}
						}
						saveLastColor("255000255/");
						saveLastMusicColor("255000070/"); 
					}
				}
				return false;
			}
		});
		pinkBtn1.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if(event.getAction()==MotionEvent.ACTION_DOWN){
					if (deviceConnected && powerBtn.isChecked()) {
						if (cycleOn) {
							cycleOn = false;
							cycleBtn.setBackgroundResource(R.drawable.cycle_button);
							brightnessUpBtn.setClickable(true);
				    		brightnessDownBtn.setClickable(true);
							PreferenceManager.getDefaultSharedPreferences(RemoteMain.this).edit().putBoolean("cycleOn", cycleOn).commit();
						}
						if (brightnessLvl == 5) {
							if (musicOn) {
								writeData("255000040/");
							}
							else {
								writeData("255000150/");  // red/green/blue
							}
						}
						else {
							if (musicOn) {
								setBrightnessLvl("255000040/", "none");
							}
							else {
								setBrightnessLvl("255000150/", "none");
							}
						}
						saveLastColor("255000150/");
						saveLastMusicColor("255000040/");
					}
				}
				return false;
			}
		});
		pinkBtn2.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if(event.getAction()==MotionEvent.ACTION_DOWN){
					if (deviceConnected && powerBtn.isChecked()) {
						if (cycleOn) {
							cycleOn = false;
							cycleBtn.setBackgroundResource(R.drawable.cycle_button);
							brightnessUpBtn.setClickable(true);
				    		brightnessDownBtn.setClickable(true);
							PreferenceManager.getDefaultSharedPreferences(RemoteMain.this).edit().putBoolean("cycleOn", cycleOn).commit();
						}
						if (brightnessLvl == 5) {
							if (musicOn) {
								writeData("255000010/");
							}
							else {
								writeData("255000050/");  // red/green/blue
							}
						}
						else {
							if (musicOn) {
								setBrightnessLvl("255000010/", "none");
							}
							else {
								setBrightnessLvl("255000050/", "none");
							}
						}
						saveLastColor("255000050/");
						saveLastMusicColor("255000010/"); 
					}
				}
				return false;
			}
		});
		whiteBtn.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if(event.getAction()==MotionEvent.ACTION_DOWN){
					if (deviceConnected && powerBtn.isChecked()) {
						if (cycleOn) {
							cycleOn = false;
							cycleBtn.setBackgroundResource(R.drawable.cycle_button);
							brightnessUpBtn.setClickable(true);
				    		brightnessDownBtn.setClickable(true);
							PreferenceManager.getDefaultSharedPreferences(RemoteMain.this).edit().putBoolean("cycleOn", cycleOn).commit();
						}
						if (brightnessLvl == 5) {
							if (musicOn) {
								writeData("245255070/");
							}
							else {
								writeData("255255255/");  // red/green/blue
							}
						}
						else {
							if (musicOn) {
								setBrightnessLvl("245255070/", "none");
							}
							else {
								setBrightnessLvl("255255255/", "none");
							}
						}
						saveLastColor("255255255/");
						saveLastMusicColor("245255070/"); 
					}
				}
				return false;
			}
		});
		
		menuBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				PopupMenu mainMenu = new PopupMenu(RemoteMain.this, menuBtn); 
				mainMenu.getMenuInflater().inflate(R.menu.remote_main, mainMenu.getMenu());
				mainMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
					@Override
					public boolean onMenuItemClick(MenuItem item) {
						if (item.getTitle().equals("Connect device")) {
							if(!deviceConnected) {
								registerReceiver(mReceiver, filter);
								connectBt();
							}
							else {
								showMessage("Device already connected");
							}
						}
						else if (item.getTitle().equals("Disconnect device")) {
							if(deviceConnected) {
								try {
									closeBt();
								} catch (IOException ex) { }
								postConnectionStatus(false);
							}
							else {
								showMessage("No device connected");
							}
						}
						else if (item.getTitle().equals("Create a custom color")) {
							int color = PreferenceManager.getDefaultSharedPreferences(RemoteMain.this).getInt("color", Color.WHITE);
				            new ColorPickerDialog(RemoteMain.this, RemoteMain.this, color).show();
						}
						else {
							final Dialog SettingsDialog = new Dialog(context);
							SettingsDialog.setContentView(R.layout.settings_dialog);
							SettingsDialog.setTitle("Settings");
							
							Button dismissBtn = (Button) SettingsDialog.findViewById(R.id.dismissBtn);
							final RadioButton jumpBtn = (RadioButton) SettingsDialog.findViewById(R.id.jumpBtn);
							final RadioButton fadeBtn = (RadioButton) SettingsDialog.findViewById(R.id.fadeBtn);
							final Button plusBtn = (Button) SettingsDialog.findViewById(R.id.plusBtn);
							final Button minusBtn = (Button) SettingsDialog.findViewById(R.id.minusBtn);
							cycleSpeedText = (TextView) SettingsDialog.findViewById(R.id.speedText);
							
							if (cycleStyle.equals("jump")) {
								jumpBtn.setChecked(true);
								jumpBtn.setTextColor(Color.parseColor("#FFFFFF"));
								fadeBtn.setTextColor(Color.parseColor("#787878"));
							}
							else {
								fadeBtn.setChecked(true);
								fadeBtn.setTextColor(Color.parseColor("#FFFFFF"));
								jumpBtn.setTextColor(Color.parseColor("#787878"));
							}
							
							setCycleSpeedText(cycleSpeed);
							
							dismissBtn.setOnClickListener(new OnClickListener() {
								public void onClick(View v) {
									SettingsDialog.dismiss();
								}
							});
							jumpBtn.setOnTouchListener(new OnTouchListener() {
								@Override
								public boolean onTouch(View v, MotionEvent event) {
									if(event.getAction()==MotionEvent.ACTION_DOWN){
										jumpBtn.setChecked(true);
										jumpBtn.setTextColor(Color.parseColor("#FFFFFF"));
										fadeBtn.setTextColor(Color.parseColor("#787878"));
										PreferenceManager.getDefaultSharedPreferences(RemoteMain.this).edit().putString("cycleStyle", "jump").commit();
										cycleStyle = "jump";
										if (deviceConnected && powerBtn.isChecked() && cycleOn) {
											turnCycleOn();
										}
									}
									return false;
								}
							});
							fadeBtn.setOnTouchListener(new OnTouchListener() {
								@Override
								public boolean onTouch(View v, MotionEvent event) {
									if(event.getAction()==MotionEvent.ACTION_DOWN){
										fadeBtn.setChecked(true);
										fadeBtn.setTextColor(Color.parseColor("#FFFFFF"));
										jumpBtn.setTextColor(Color.parseColor("#787878"));
										PreferenceManager.getDefaultSharedPreferences(RemoteMain.this).edit().putString("cycleStyle", "fade").commit();
										cycleStyle = "fade";
										if (deviceConnected && powerBtn.isChecked() && cycleOn) {
											turnCycleOn();
										}
									}
									return false;
								}
							});
							plusBtn.setOnTouchListener(new OnTouchListener() {
								@Override
								public boolean onTouch(View v, MotionEvent event) {
									if(event.getAction()==MotionEvent.ACTION_DOWN){
										cycleSpeed--;
										setCycleSpeedText(cycleSpeed);
										PreferenceManager.getDefaultSharedPreferences(RemoteMain.this).edit().putInt("cycleSpeed", cycleSpeed).commit();
										if (cycleSpeed == 1) {
											plusBtn.setEnabled(false);
										}
										if (!minusBtn.isEnabled()) {
											minusBtn.setEnabled(true);
										}
										if (deviceConnected && powerBtn.isChecked() && cycleOn) {
											turnCycleOn();
										}
									}
									return false;
								}
							});
							minusBtn.setOnTouchListener(new OnTouchListener() {
								@Override
								public boolean onTouch(View v, MotionEvent event) {
									if(event.getAction()==MotionEvent.ACTION_DOWN){
										cycleSpeed++;
										setCycleSpeedText(cycleSpeed);
										PreferenceManager.getDefaultSharedPreferences(RemoteMain.this).edit().putInt("cycleSpeed", cycleSpeed).commit();
										if (cycleSpeed == 10) {
											minusBtn.setEnabled(false);
										}
										if (!plusBtn.isEnabled()) {
											plusBtn.setEnabled(true);
										}
										if (deviceConnected && powerBtn.isChecked() && cycleOn) {
											turnCycleOn();
										}
									}
									return false;
								}
							});
							
							SettingsDialog.show();
						}
						return true;
					}
				});
				mainMenu.show();
			}
		});
		
		musicBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
		    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		        if (isChecked && deviceConnected && powerBtn.isChecked()) {
		        	if (currentFlash != 0) {
		        		PreferenceManager.getDefaultSharedPreferences(RemoteMain.this).edit().putInt("flash", 0).commit();
						currentFlash = 0;
						showMessage("Flash Off");
						flashBtn.setBackgroundResource(R.drawable.flash_pattern_button);
						stopFlashBtn.setVisibility(View.INVISIBLE);
		        	}
		            //writeData("music_on/");
		            PreferenceManager.getDefaultSharedPreferences(RemoteMain.this).edit().putBoolean("musicOn", true).commit();
		            musicOn = true;		            
		            if (cycleOn) {
		            	turnCycleOn();
		            }
		            else {
		            	if (brightnessLvl == 5) {                       	
		            		writeData(getLastMusicColor());	
		            	}													
		            	else {												
		            		setBrightnessLvl(getLastMusicColor(), "none");
		            	}
		            }
		        }
		        else if (!isChecked && deviceConnected && powerBtn.isChecked()) {
		        	//writeData("music_off/");
		        	PreferenceManager.getDefaultSharedPreferences(RemoteMain.this).edit().putBoolean("musicOn", false).commit();
		        	musicOn = false;
		        	if (brightnessLvl == 5) {         
		    			writeData(getLastColor());		
		    		}								
		    		else {									
		    			setBrightnessLvl(getLastColor(), "none");
		    		}												
		        }
		    }
		});
		
		brightnessUpBtn.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if(event.getAction()==MotionEvent.ACTION_DOWN){
					if (deviceConnected && powerBtn.isChecked()) {
						setBrightnessLvl(getLastColor(), "raise");
					}
				}
				return false;
			}
		});
		
		brightnessDownBtn.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if(event.getAction()==MotionEvent.ACTION_DOWN){
					if (deviceConnected && powerBtn.isChecked()) {
						setBrightnessLvl(getLastColor(), "lower");
					}
				}
				return false;
			}
		});
		
		flashBtn.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if(event.getAction()==MotionEvent.ACTION_DOWN){
					if (deviceConnected && powerBtn.isChecked()) {
						if (musicOn) {
				        	PreferenceManager.getDefaultSharedPreferences(RemoteMain.this).edit().putBoolean("musicOn", false).commit();
							musicOn = false;
							musicBtn.setChecked(false);
							//writeData("music_off/");
						}
						else {
							switch (currentFlash) {
							case 0:
								PreferenceManager.getDefaultSharedPreferences(RemoteMain.this).edit().putInt("flash", 1).commit();
								currentFlash = 1;
								showMessage("Flash Slow");
								writeData("flash1/");
								stopFlashBtn.setVisibility(View.VISIBLE);
								flashBtn.setBackgroundResource(R.drawable.flash_pattern_button_on);
								break;
							case 1:
								PreferenceManager.getDefaultSharedPreferences(RemoteMain.this).edit().putInt("flash", 2).commit();
								currentFlash = 2;
								showMessage("Flash Med");
								writeData("flash2/");
								break;
							case 2:
								PreferenceManager.getDefaultSharedPreferences(RemoteMain.this).edit().putInt("flash", 3).commit();
								currentFlash = 3;
								showMessage("Flash Fast");
								writeData("flash3/");
								break;
							case 3:
								PreferenceManager.getDefaultSharedPreferences(RemoteMain.this).edit().putInt("flash", 4).commit();
								currentFlash = 4;
								showMessage("Burst Slow");
								writeData("flash4/");
								break;
							case 4:
								PreferenceManager.getDefaultSharedPreferences(RemoteMain.this).edit().putInt("flash", 5).commit();
								currentFlash = 5;
								showMessage("Burst Med");
								writeData("flash5/");
								break;
							case 5:
								PreferenceManager.getDefaultSharedPreferences(RemoteMain.this).edit().putInt("flash", 6).commit();
								currentFlash = 6;
								showMessage("Burst Fast");
								writeData("flash6/");
								break;
							case 6:
								PreferenceManager.getDefaultSharedPreferences(RemoteMain.this).edit().putInt("flash", 0).commit();
								currentFlash = 0;
								showMessage("Flash Off");
								writeData("flash0/");
								stopFlashBtn.setVisibility(View.INVISIBLE);
								flashBtn.setBackgroundResource(R.drawable.flash_pattern_button);
								break;
							}
						}
					}
				}
				return false;
			}
		});
		
		stopFlashBtn.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if(event.getAction()==MotionEvent.ACTION_DOWN){
					if (deviceConnected && powerBtn.isChecked()) {
						PreferenceManager.getDefaultSharedPreferences(RemoteMain.this).edit().putInt("flash", 0).commit();
						currentFlash = 0;
						showMessage("Flash Off");
						writeData("flash0/");
						flashBtn.setBackgroundResource(R.drawable.flash_pattern_button);
						stopFlashBtn.setVisibility(View.INVISIBLE);
					}
				}
				return false;
			}
		});
		
		cycleBtn.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction()==MotionEvent.ACTION_DOWN){
					if (deviceConnected && powerBtn.isChecked()) {
						if (!cycleOn) {
							cycleOn = true;
							brightnessUpBtn.setClickable(false);
				    		brightnessDownBtn.setClickable(false);
							cycleBtn.setBackgroundResource(R.drawable.cycle_button_on);
							turnCycleOn();
							PreferenceManager.getDefaultSharedPreferences(RemoteMain.this).edit().putBoolean("cycleOn", cycleOn).commit();
						}
					}
				}
				return false;
			}
		});

	}

	public void onDestroy() {
		super.onDestroy();
		msg.cancel();
		if (deviceConnected) {
			try {
				closeBt();
			} catch (IOException ex) { }
		}
	}
	
	public void onPause() {
		super.onPause();
		if (deviceConnected) {
			if (mBluetoothAdapter != null) {
				mBluetoothAdapter.cancelDiscovery();
			}
			unregisterReceiver(mReceiver);
		}
	}
	
	public void onResume() {
		super.onResume();
		registerReceiver(mReceiver, filter);
	}
	
	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
	    public void onReceive(Context context, Intent intent) {
	        String action = intent.getAction();
	        // When discovery finds a device
	        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
	            // Get the BluetoothDevice object from the Intent
	            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
	            if (device.getAddress().equals(address)) {
	            	try {
						mmSocket = mmDevice.createRfcommSocketToServiceRecord(MY_UUID);        
						mmSocket.connect();
						mmOutputStream = mmSocket.getOutputStream();
						mmInputStream = mmSocket.getInputStream();
					} catch (IOException e) {
						try {
							mmSocket.close();
						} catch (IOException e2) {
							Log.d(TAG, "Socket failed to close");
						}
						showMessage("Failed to connect");
						Log.d(TAG, "Socket creation failed");
					}
	            }
	            else {
	            	showMessage("Device not found");
	            }
	        }
	        else if(BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
	        	postConnectionStatus(true);
	        }
	        else if(BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
	        	postConnectionStatus(false);
	        }
	    }
	};
	
	private void checkBtStatus() {
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter == null) {
			showMessage("This device does not support bluetooth");
		}
		else {
			if (!mBluetoothAdapter.isEnabled()) {
			    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
			}
		}

	}
	
	private void connectBt() {
		if (mBluetoothAdapter.isEnabled()) {
			Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
			if (mmDevice == null) {
				mmDevice = mBluetoothAdapter.getRemoteDevice(address);
			}
			if (pairedDevices.contains(mmDevice)) {
				try {
					mmSocket = mmDevice.createRfcommSocketToServiceRecord(MY_UUID);
					mmSocket.connect();
					mmOutputStream = mmSocket.getOutputStream();
					mmInputStream = mmSocket.getInputStream();
				} catch (IOException e) {
					try {
						mmSocket.close();
					} catch (IOException e2) {
						Log.d(TAG, "Socket failed to close");
					}
					showMessage("Failed to connect");
					Log.d(TAG, "Socket creation failed");
				}
			}
			else {
				mBluetoothAdapter.startDiscovery();
			}
		}
		else {
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}
	}
	
	private void writeData(String data) {
		try {
			mmOutputStream = mmSocket.getOutputStream();
		} catch (IOException e) {
			Log.d(TAG, "Bug BEFORE Sending stuff", e);
		}

		String message = data;
		byte[] msgBuffer = message.getBytes();

		try {
			mmOutputStream.write(msgBuffer);
		} catch (IOException e) {
			Log.d(TAG, "Bug while sending stuff", e);
		}
	}
	
	void closeBt() throws IOException
    {
		if (mBluetoothAdapter != null) {
			mBluetoothAdapter.cancelDiscovery();
		}
		unregisterReceiver(mReceiver);
        stopWorker = true;
        mmOutputStream.close();
        mmInputStream.close();
        mmSocket.close();
    }
	
	private void showMessage(String message) {
		try {
			if (msg.getView().isShown()) {
				msg.cancel();
			}
		} catch (Exception e) {	}
		msg = Toast.makeText(RemoteMain.this, message, Toast.LENGTH_SHORT);
		msg.setDuration(1000);
		msg.setGravity(Gravity.CENTER, msg.getXOffset() / 2,
				msg.getYOffset() / 2);
		msg.show();
	}
	
	private String rgb2String(int rgb) {
		String rgbString;
		if (rgb < 10) {
			rgbString = "00" + Integer.toString(rgb);
		}
		else if (rgb < 100) {
			rgbString = "0" + Integer.toString(rgb);
		}
		else {
			rgbString = Integer.toString(rgb);
		}
		return rgbString;
	}
	
	private int[] String2rgb(String colorCode) {
		int r = Integer.parseInt(colorCode.substring(0, 3));
		int g = Integer.parseInt(colorCode.substring(3, 6));
		int b = Integer.parseInt(colorCode.substring(6, 9));
		int[] rgb = {r, g, b};
		return rgb;
	}
	
	private void setBrightnessLvl(String colorCode, String adjustBrightness) {
		if (adjustBrightness.equals("raise")) {
			if (brightnessLvl != 5) {
				brightnessLvl++;		
			}
		}
		else if (adjustBrightness.equals("lower")) {
			if (brightnessLvl != 0) {
				brightnessLvl--;
			}
		}
		String newRedStr, newGreenStr, newBlueStr;
		int[] oldValues = new int[2];
		oldValues = String2rgb(colorCode);
		switch (brightnessLvl) {
		case 0:
			if (!adjustBrightness.equals("none")) {
				showMessage("Brightness: 0");
			}
			writeData("000000000/");
			break;
		case 1:
			if (!adjustBrightness.equals("none")) {
				showMessage("Brightness: 1");
			}
			newRedStr = rgb2String(oldValues[0] / 135);
			newGreenStr = rgb2String(oldValues[1] / 135);
			newBlueStr = rgb2String(oldValues[2] / 135);
			writeData(newRedStr + newGreenStr + newBlueStr + "/");
			break;
		case 2:
			if (!adjustBrightness.equals("none")) {
				showMessage("Brightness: 2");
			}
			newRedStr = rgb2String(oldValues[0] / 45);
			newGreenStr = rgb2String(oldValues[1] / 45);
			newBlueStr = rgb2String(oldValues[2] / 45);
			writeData(newRedStr + newGreenStr + newBlueStr + "/");
			break;
		case 3:
			if (!adjustBrightness.equals("none")) {
				showMessage("Brightness: 3");
			}
			newRedStr = rgb2String(oldValues[0] / 15);
			newGreenStr = rgb2String(oldValues[1] / 15);
			newBlueStr = rgb2String(oldValues[2] / 15);
			writeData(newRedStr + newGreenStr + newBlueStr + "/");
			break;
		case 4:
			if (!adjustBrightness.equals("none")) {
				showMessage("Brightness: 4");
			}
			newRedStr = rgb2String(oldValues[0] / 5);
			newGreenStr = rgb2String(oldValues[1] / 5);
			newBlueStr = rgb2String(oldValues[2] / 5);
			writeData(newRedStr + newGreenStr + newBlueStr + "/");
			break;
		case 5:
			if (!adjustBrightness.equals("none")) {
				showMessage("Brightness: Max");
			}
			writeData(colorCode);
		}
		PreferenceManager.getDefaultSharedPreferences(RemoteMain.this).edit().putInt("brightness", brightnessLvl).commit();
	}
	
	private void turnCycleOn() {
		if (cycleStyle.equals("jump")) {
			if (cycleSpeed == 10) {
				if (musicOn) {
					writeData("cyclej" + Integer.toString(cycleSpeed) + "m" + "/");
				}
				else {
					writeData("cyclej" + Integer.toString(cycleSpeed) + "/");
				}
			}
			else {
				if (musicOn) {
					writeData("cyclej0" + Integer.toString(cycleSpeed) + "m" + "/");
				}
				else {
					writeData("cyclej0" + Integer.toString(cycleSpeed) + "/");
				}
			}
		}
		else if (cycleStyle.equals("fade")) {
			if (cycleSpeed == 10) {
				if (musicOn) {
					writeData("cyclef" + Integer.toString(cycleSpeed) + "m" + "/");
				}
				else {
					writeData("cyclef" + Integer.toString(cycleSpeed) + "/");
				}
			}
			else {
				if (musicOn) {
					writeData("cyclef0" + Integer.toString(cycleSpeed) + "m" + "/");
				}
				else {
					writeData("cyclef0" + Integer.toString(cycleSpeed) + "/");
				}
			}
		}
	}
	
	private void setCycleSpeedText(int cycleSpeed) {
		switch (cycleSpeed) {
		case 1:
			cycleSpeedText.setText("10");
			break;
		case 2:
			cycleSpeedText.setText("9");
			break;
		case 3:
			cycleSpeedText.setText("8");
			break;
		case 4:
			cycleSpeedText.setText("7");
			break;
		case 5:
			cycleSpeedText.setText("6");
			break;
		case 6:
			cycleSpeedText.setText("5");
			break;
		case 7:
			cycleSpeedText.setText("4");
			break;
		case 8:
			cycleSpeedText.setText("3");
			break;
		case 9:
			cycleSpeedText.setText("2");
			break;
		case 10:
			cycleSpeedText.setText("1");
			break;
		}
	}
	
	private void saveLastColor(String colorCode) {
		PreferenceManager.getDefaultSharedPreferences(RemoteMain.this).edit().putString("lastColor", colorCode).commit();
	}
	
	private String getLastColor() {
		return PreferenceManager.getDefaultSharedPreferences(RemoteMain.this).getString("lastColor", "255255255/");
	}
	
	private void saveLastMusicColor(String colorCode) {																			
		PreferenceManager.getDefaultSharedPreferences(RemoteMain.this).edit().putString("lastMusicColor", colorCode).commit();
	}
	
	private String getLastMusicColor() {
		return PreferenceManager.getDefaultSharedPreferences(RemoteMain.this).getString("lastMusicColor", "245255070/");
	}
	
	@Override
    public void colorChanged(int color) {
        PreferenceManager.getDefaultSharedPreferences(this).edit().putInt("color", color).commit();
        if (deviceConnected && powerBtn.isChecked()) {
        	if (cycleOn) {
				cycleOn = false;
				cycleBtn.setBackgroundResource(R.drawable.cycle_button);
				brightnessUpBtn.setClickable(true);
	    		brightnessDownBtn.setClickable(true);
				PreferenceManager.getDefaultSharedPreferences(RemoteMain.this).edit().putBoolean("cycleOn", cycleOn).commit();
			}
        	int r = (color >> 16) & 0xFF;
        	int g = (color >> 8) & 0xFF;
        	int b = (color >> 0) & 0xFF;
        	String rString, gString, bString;
        	if (musicOn) {
        		rString = rgb2String(double2Int(r * .95));
        		gString = rgb2String(g);
        		bString = rgb2String(double2Int(b * .25));
        	}
        	else {
        		rString = rgb2String(r);
        		gString = rgb2String(g);
        		bString = rgb2String(b);
        	}
        	writeData(rString + gString + bString + "/");
        	saveLastColor(rString + gString + bString + "/");
        }
	}
	
	private void postConnectionStatus(boolean isConnected) {
		if (isConnected) {
			deviceConnected = true;
        	showMessage("Connected");
        	connectionStatus.setImageResource(R.drawable.small_green_circle);
        	powerBtn.setClickable(true);
        	powerBtn.setBackgroundResource(R.drawable.power_button_selector);
        	if (powerOn) {
        		powerBtn.setChecked(true);
        	}
		}
		else {
			deviceConnected = false;
        	showMessage("Disconnected");
        	connectionStatus.setImageResource(R.drawable.small_red_circle);
        	powerBtn.setChecked(false);
        	powerBtn.setClickable(false);
        	flashBtn.setBackgroundResource(R.drawable.flash_pattern_button);
            stopFlashBtn.setVisibility(View.INVISIBLE);
            musicBtn.setChecked(false);
            cycleBtn.setBackgroundResource(R.drawable.cycle_button);
        	musicBtn.setClickable(false);
    		brightnessUpBtn.setClickable(false);
    		brightnessDownBtn.setClickable(false);
    		flashBtn.setClickable(false);
    		stopFlashBtn.setClickable(false);
    		cycleBtn.setClickable(false);
		}
	}
	
	public int double2Int(double value)
	{
		double valueAbs = Math.abs(value);
		int output = (int) valueAbs;
		double result = valueAbs - (double) output;
		if(result<.5){
		return value<0 ? -output : output;
		}
		else{
		return value<0 ? -(output+1) : output+1;
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present. Which it won't be.
		getMenuInflater().inflate(R.menu.remote_main, menu);
		return true;
	}
	
}
