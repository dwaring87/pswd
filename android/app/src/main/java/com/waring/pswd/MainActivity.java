package com.waring.pswd;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * This Activity displays the options for generating a site-specific password in two tabs.  The
 * general tab has the domain and length, the advanced tab has the rest of the options.
 * <p />
 * If the user is not logged in, this will start the {@link com.waring.pswd.LoginActivity}.
 */
public class MainActivity extends Activity implements ActionBar.TabListener {
	
	
	// CURRENT USER INFORMATION

    /** The current user's username */
	private static String USERNAME = "";

    /** The current user's master password */
	private static String MASTER_PASSWORD = "";

    /** The current user's user token */
	private static String USER_TOKEN = "";
	


	// TABS

    /** The titles for the tabs */
	private String[] TAB_TITLES = new String[]{"General", "Advanced"};

    /** The number of tabs = length of titles */
    private final int TAB_COUNT = TAB_TITLES.length;

    /** A list to hold the Fragments */
	private List<Fragment> FRAG_LIST = new ArrayList<Fragment>();



	// STATE VARIABLES
    // Keys to hold the state values when restoring that app state

    /** The currently selected tab */
	private static final String STATE_SELECTED_NAVIGATION_ITEM = "selected_navigation_item";

    /** The current user's username */
    private static final String STATE_USERNAME = "state_username";

    /** The current user's master password */
	private static final String STATE_PASSWORD = "state_password";

    /** The current user's user token */
	private static final String STATE_TOKEN = "state_token";


	
	// DEFAULT SETTINGS

    /** The default length of the generated password */
	private static final int DEFAULT_LENGTH = 24;

    /** The default use of capital letters */
	private static final boolean DEFAULT_CAPS = true;

    /** The default use of symbol characters */
	private static final boolean DEFAULT_SYMBOLS = true;

    /** The default symbol characters to pick from */
	private static final String DEFAULT_SYMCHARS = "!@$*-_.?";

    /** The default number of hashes for generating the user token */
	protected static final int DEFAULT_K1 = 10000000;

    /** The default number of hashes for generating the site password */
	private static final int DEFAULT_K2 = 250;



	// SETTINGS KEYS
    // Keys to store values in the Shared Preferences

    /** The SharedPrefs file name */
	protected static final String PREFS_NAME = "PSWD_PREFS";

    /** The password's domain */
	private static final String PREFS_DOMAIN = "pref_domain";

    /** The length of the password */
	private static final String PREFS_LENGTH = "pref_length";

    /** The flag to include uppercase letters */
	private static final String PREFS_CAPS = "pref_caps";

    /** The flag to include symbol characters */
	private static final String PREFS_SYMBOLS = "pref_symbols";

    /** The symbol characters to pick from */
	private static final String PREFS_SYMCHARS = "pref_symchars";

    /** The number of hashes used to generate the password */
	private static final String PREFS_K2 = "pref_k2";

    /** A flag indicating the token generation has completed */
	protected static final String PREFS_TOKEN_GEN_COMPLETE = "pref_token_gen_complete"; 

    /** An encrypted copy of the remembered username for login */
	protected static final String PREFS_ENC_USERNAME = "pref_enc_username";

    /** An encrypted copy of the remembered master password for login */
	protected static final String PREFS_ENC_PASSWORD = "pref_enc_password";

    /** The amount of time elapsed before the user is automatically logged out */
	private final String PREFS_PAUSE_TIME = "pref_pause_time";

    /** The default amount of time that can elapse after onPause() is called before auto logout */
    private final long PREFS_MAX_PAUSE_TIME = 300000;   // 5 minutes
	
	
	/** Flag set to true when a token is being generated */
	protected static boolean GENERATING_TOKEN = false;


	/** Broadcast Receiver to Monitor Token Generation Progress */
	private BroadcastReceiver BR_TOKEN_GEN;

    /** The intent name for the broadcast receiver */
	protected static final String BR_INTENT = "GEN_TOKEN_BROADCAST";
	
	
	/** Login request code */
	private static final int LOGIN_REQUEST = 1;
	
	/** Display password request code */
	private static final int DISPLAY_REQUEST = 2;
	
	
	/** More info URL */
	private final String INFO_URL = "http://pswd.davidwaring.net/info.html";
	
	/** Web version URL */
	private final String WEB_URL = "https://pswd.davidwaring.net/";


    /**
     * Get any intent extras for the user's information.  Set up the actionbar and tabs.
     * @param savedInstanceState
     */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		// Check Intent for Extras
		Intent intent = getIntent();
		if ( intent.hasExtra("username") ) {
			MainActivity.USERNAME = intent.getStringExtra("username");
		}
		if ( intent.hasExtra("password") ) {
			MainActivity.MASTER_PASSWORD = intent.getStringExtra("password");
		}
		if ( intent.hasExtra("token") ) {
			MainActivity.USER_TOKEN = intent.getStringExtra("token");
			GENERATING_TOKEN = false;
		}
		
		// Set up the action bar.
		final ActionBar actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		
		// Add Tabs
		for (int i=0; i < TAB_COUNT; i++) {
	        Tab tab = actionBar.newTab();
	        tab.setText(TAB_TITLES[i]);
	        tab.setTabListener(this);
	        actionBar.addTab(tab);
		}
	}


    /**
     * Check if a token is currently being generated.  If not and a user is not logged in,
     * start the {@link com.waring.pswd.LoginActivity}.  Register the token generator broadcast
     * receiver to monitor and show the progress of the token generation.
     */
	@Override
	protected void onResume() {
	    super.onResume();
	    
	    // Check for token gen complete flag
	    // If a token was just finished being generated, show progress as complete
	    final SharedPreferences prefs = this.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
	    boolean token_gen_complete = prefs.getBoolean(PREFS_TOKEN_GEN_COMPLETE, false);
	    if ( token_gen_complete ) {
	    	GENERATING_TOKEN = false;
	    	
	    	try {
	    		TabFragment tab = (TabFragment) FRAG_LIST.get(0);
				tab.updateProgress(100, "Complete");
				tab.genToken();
	    	}
	    	catch (Exception e) {}
	    	
	    	Editor edit = prefs.edit();
			edit.putBoolean(PREFS_TOKEN_GEN_COMPLETE, false);
			edit.commit();
	    }
	    
	    
	    // Get the time since last paused
	    long last_pause = prefs.getLong(PREFS_PAUSE_TIME, new Date().getTime());
	    long delta_pause = new Date().getTime() - last_pause;
	    
	    
	    // Show the Login Activity if we're not currently generating a token and:
	    //    the amount of time since last paused exceeds the time limit or
	    //    we're missing any user info
	    if ( !GENERATING_TOKEN && (delta_pause > PREFS_MAX_PAUSE_TIME || MainActivity.USERNAME.equals("")
                || MainActivity.MASTER_PASSWORD.equals("") || MainActivity.USER_TOKEN.equals("")) ) {
	    	logout(false);
	    }
	    else {
	    	ActionBar ab = getActionBar();
        	ab.setTitle("PSWD");
        	ab.setSubtitle(MainActivity.USERNAME);
	    }
	    
	    
	    // Register Token BR
 		IntentFilter intentFilter = new IntentFilter(BR_INTENT);
        BR_TOKEN_GEN = new BroadcastReceiver() {
        	@Override
        	public void onReceive(Context context, Intent intent) {
        		
        		// Get User Info sent with Intent
        		setUserInfo(intent.getStringExtra("username"), intent.getStringExtra("password"), intent.getStringExtra("token"));
            	 
        		// Get Action
        		String action = intent.getStringExtra("action");
        		
        		// If the token is still being generated...
        		if ( action.equals("loading") ) {
        			try {
        				GENERATING_TOKEN = true;        				
        				
        				// Update the Progress Information
        				TabFragment tab = (TabFragment) FRAG_LIST.get(0);
        				tab.updateProgress(intent.getIntExtra("progress", 0), intent.getStringExtra("remaining"));
        				tab.genToken();
        			}
 	            	catch (Exception e) {}
        		}
        		
        		// If the token generation has completed...
        		else if ( action.equals("complete") ) {
        			try {
        				GENERATING_TOKEN = false;
        				
        				Editor edit = prefs.edit();
        				edit.putBoolean(PREFS_TOKEN_GEN_COMPLETE, false);
        				edit.commit();
            			 
        				TabFragment tab = (TabFragment) FRAG_LIST.get(0);
        				tab.updateProgress(100, "Complete");
        				tab.genToken();
        			}
        			catch (Exception e) {}
        		}
        	}
        };
        this.registerReceiver(BR_TOKEN_GEN, intentFilter);
	}


    /**
     * When pausing, keep track of the time since last pause.  Unregister broadcast receiver.
     */
	@Override
	public void onPause() {
		super.onPause();
		
		// Log the pause time
		SharedPreferences prefs = this.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
		Editor edit = prefs.edit();
		edit.putLong(PREFS_PAUSE_TIME, new Date().getTime());
		edit.commit();
		
		// Unregister the DB Update Receiver
    	this.unregisterReceiver(BR_TOKEN_GEN);
	}
	
	
	/**
	 * Set the User Information and AB Sub Title
	 * @param username User Name
	 * @param password Master Password
	 * @param token User Token
	 */
	private void setUserInfo(String username, String password, String token) {
		MainActivity.USERNAME = username;
		MainActivity.MASTER_PASSWORD = password;
		MainActivity.USER_TOKEN = token;
		
		ActionBar ab = getActionBar();
    	ab.setTitle("PSWD");
    	ab.setSubtitle(MainActivity.USERNAME);
	}

	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
	    super.onConfigurationChanged(newConfig);
	}


    /**
     * Save and restore tab state
     * @param savedInstanceState
     */
	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		//Restore the previously serialized current tab position.
		if (savedInstanceState.containsKey(STATE_SELECTED_NAVIGATION_ITEM)) {
			getActionBar().setSelectedNavigationItem(savedInstanceState.getInt(STATE_SELECTED_NAVIGATION_ITEM));
		}
		
		if ( savedInstanceState.containsKey(STATE_USERNAME) ) {
			USERNAME = savedInstanceState.getString(STATE_USERNAME);
		}
		
		if ( savedInstanceState.containsKey(STATE_PASSWORD) ) {
			MASTER_PASSWORD = savedInstanceState.getString(STATE_PASSWORD);
		}
		
		if ( savedInstanceState.containsKey(STATE_TOKEN) ) {
			USER_TOKEN = savedInstanceState.getString(STATE_TOKEN);
		}
	}


    /**
     * Set the current tab state to be restored later
     * @param outState
     */
	@Override
	public void onSaveInstanceState(Bundle outState) {
		// Serialize the current tab position.
		outState.putInt(STATE_SELECTED_NAVIGATION_ITEM, getActionBar().getSelectedNavigationIndex());
		outState.putString(STATE_USERNAME, USERNAME);
		outState.putString(STATE_PASSWORD, MASTER_PASSWORD);
		outState.putString(STATE_TOKEN, USER_TOKEN);
	}


    /**
     * Create the actionbar options menu from menu/main.xml
     * @param menu
     * @return
     */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

    /**
     * Parse and handle option menu selections
     * @param item the selected menu item
     * @return
     */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		
		if ( id == R.id.action_info ) {
			Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(INFO_URL));
			startActivity(browserIntent);
		}
		else if ( id == R.id.action_web ) {
			Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(WEB_URL));
			startActivity(browserIntent);
		}
		else if ( id == R.id.action_logout_erase ) {
			logout(true);
		}
		else if (id == R.id.action_logout) {
			logout(false);
		}
		
		return super.onOptionsItemSelected(item);
	}
	
	
	
	
	
	
	
	
	
	
	
	
	// LOGIN
	
	/**
	 * Start the Login Activity
	 */
	public void login() {
		Intent intent = new Intent(this, LoginActivity.class);
		startActivityForResult(intent, LOGIN_REQUEST);
	}
	
	
	/**
	 * Receive the Result of the {@link com.waring.pswd.LoginActivity} and the
     * {@link com.waring.pswd.DisplayActivity}
     * <p />
     * When returning from the {@link com.waring.pswd.LoginActivity}, get the
     * logged in user's credentials
     * <p />
     * When returning from the {@link com.waring.pswd.DisplayActivity}, check
     * for a logout request
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// Process Returned Request Code
	    if (requestCode == LOGIN_REQUEST) {
	        
	    	// Activity Returned RESULT_OK
	        if (resultCode == RESULT_OK) {
	        	MainActivity.USERNAME = data.getStringExtra("username");
	        	MainActivity.MASTER_PASSWORD = data.getStringExtra("password");
	        	MainActivity.USER_TOKEN = data.getStringExtra("token");
	        	
	        	ActionBar ab = getActionBar();
	        	ab.setTitle("PSWD");
	        	ab.setSubtitle(MainActivity.USERNAME);
	        	
	        	if ( MainActivity.USER_TOKEN.equals("loading") ) {
	        		GENERATING_TOKEN = true;
	        		
	        		try {
	        			TabFragment tab = (TabFragment) FRAG_LIST.get(0);
	        			tab.genToken();
	            	}
	            	catch (Exception e) {}
	        	}
	        }
	    }
	    
	    // Process the result of the DisplayActivity
	    else if ( requestCode == DISPLAY_REQUEST ) {
	    	if ( resultCode == RESULT_OK ) {	
	    		// Get the requested action
	    		String action = data.getStringExtra("action");
	    		
	    		// If a logout is requested...
	    		if ( action.equals("logout") ) {
	    			logout(false);
	    		}
	    	}
	    }
	}
	
	
	
	// LOGOUT
	
	/**
	 * Log out the current user and show the Login Activity
	 * Erase the user's cached User Token, if requested
	 * See completeLogout() for Part 2
	 * @param erase true to erase the User Token
	 */
	private void logout(boolean erase) {
		
		// Remove cached User Token, if requested
    	if ( erase ) {
    		new AlertDialog.Builder(MainActivity.this)
			.setTitle("Erase User Token?")
			.setMessage("Are you sure you want to erase your user token?  To generate another password this will need to be regenerated.")
			.setPositiveButton("Erase", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					PSWD.eraseUserToken(MainActivity.this, USERNAME);
					completeLogout();
					return;
				}
			})
			.setNegativeButton("Save", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					completeLogout();
					return;
				}
			})
			.show();
    	}
		
    	
    	else {
			completeLogout();
    	}
	}
	
	/**
	 * Logout Part 2
	 * Reset User Information
	 * Clear Domain
	 * Show Login Activity
	 */
	private void completeLogout() {
		MainActivity.USERNAME = "";
		MainActivity.MASTER_PASSWORD = "";
		MainActivity.USER_TOKEN = "";
		
		ActionBar ab = getActionBar();
    	ab.setTitle("PSWD");
    	ab.setSubtitle("");
		
    	try {
			TabFragment tab = (TabFragment) FRAG_LIST.get(0);
			tab.clearDomain();
    	}
    	catch (Exception e) {}
    	
    	
		login();
	}
	
	
	
	
	// TAB CHANGES

    /**
     * Change to the selected tab
     * @param tab
     * @param fragmentTransaction
     */
	@Override
	public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
		
		Fragment f = null;
		TabFragment tf = null;
		
		if ( FRAG_LIST.size() > tab.getPosition() ) {
			f = FRAG_LIST.get(tab.getPosition());
		}
		
		if (f == null) {
			tf = new TabFragment();
			Bundle data = new Bundle();
			data.putInt(TabFragment.ARG_SECTION_NUMBER,  tab.getPosition());
			tf.setArguments(data);
			FRAG_LIST.add(tab.getPosition(), tf);
		}
		else {
			tf = (TabFragment) f;
		}
		
		fragmentTransaction.replace(android.R.id.content, tf);
	}

    /**
     * Remove the unselected tab
     * @param tab
     * @param fragmentTransaction
     */
	@Override
	public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
		if (FRAG_LIST.size() > tab.getPosition()) {
			fragmentTransaction.remove(FRAG_LIST.get(tab.getPosition()));
		}
	}
	  
	@Override
	public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {}

	
	
	
	
	
	/**
	 * Start the Password Generation
	 * Get all password options from the SharedPrefs
	 * @param context the Context of the MainActivity
	 */
	private static void generate(Context context) {
		
		// Get Options from Shared Preferences
		SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
		String domain = prefs.getString(PREFS_DOMAIN, "");
		int length = prefs.getInt(PREFS_LENGTH, DEFAULT_LENGTH);
		boolean caps = prefs.getBoolean(PREFS_CAPS, DEFAULT_CAPS);
		boolean symbols = prefs.getBoolean(PREFS_SYMBOLS, DEFAULT_SYMBOLS);
		String symchars = prefs.getString(PREFS_SYMCHARS, DEFAULT_SYMCHARS);
		int hashes = prefs.getInt(PREFS_K2, DEFAULT_K2);
		
		// If Domain is not entered...
		if ( domain.equals("") ) {
			Toast.makeText(context, "Enter a Domain", Toast.LENGTH_LONG).show();
			return;
		}
		
		
		// Get generated password
		String final_password = PSWD.generate(MainActivity.USERNAME, MainActivity.MASTER_PASSWORD, MainActivity.USER_TOKEN, domain, length, caps, symbols, symchars, hashes);
		
		
		// Copy to clipboard
		android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE); 
	    android.content.ClipData clip = android.content.ClipData.newPlainText("PSWD", final_password);
	    clipboard.setPrimaryClip(clip);
		
		
		// Display the Password
	    // Launch the DisplayActivity
	    Intent intent = new Intent(context, DisplayActivity.class);
	    intent.putExtra("domain", domain);
	    intent.putExtra("password", final_password);
	    ((Activity) context).startActivityForResult(intent, DISPLAY_REQUEST);
	}
	
	
	

	/**
	 * The Fragment containing the tab's content
	 */
	public static class TabFragment extends Fragment {
		/** The fragment argument representing the section number for this fragment. */
		private static final String ARG_SECTION_NUMBER = "section_number";
		private int index = -1;
		
		
		// UI Elements
		private EditText ET_DOMAIN;
		private Spinner SP_LENGTH;
		private Button BT_GENERATE;
		private LinearLayout LL_PROGRESS;
		private CheckBox CB_CAPS;
		private CheckBox CB_SYMBOLS;
		private EditText ET_SYMCHARS;
		private EditText ET_HASHES;
		
		private View ROOT;
		
		@Override
		public void onCreate(Bundle savedInstanceState) {		
			super.onCreate(savedInstanceState);
			Bundle data = getArguments();
			if ( data.containsKey(ARG_SECTION_NUMBER) ) {
				index = data.getInt(ARG_SECTION_NUMBER);
			}
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			ROOT = null;
			
			// Get user preferences
			SharedPreferences sharedPref = getActivity().getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
			final Editor edit = sharedPref.edit();
			int length = sharedPref.getInt(PREFS_LENGTH, DEFAULT_LENGTH);
			boolean caps = sharedPref.getBoolean(PREFS_CAPS, DEFAULT_CAPS);
			boolean symbols = sharedPref.getBoolean(PREFS_SYMBOLS, DEFAULT_SYMBOLS);
			String symchars = sharedPref.getString(PREFS_SYMCHARS, DEFAULT_SYMCHARS);
			int k2 = sharedPref.getInt(PREFS_K2, DEFAULT_K2);
			
			
			// GENERAL TAB
			if ( index == 0 ) {
				ROOT = inflater.inflate(R.layout.main_fragment, container, false);
				
				
				// Set up Domain EditText
				ET_DOMAIN = (EditText) ROOT.findViewById(R.id.general_domain);
				ET_DOMAIN.addTextChangedListener(new TextWatcher() {
					public void afterTextChanged(Editable s) {}
					
					public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
					
					public void onTextChanged(CharSequence s, int start, int before, int count) {
						String domain = s.toString();
						domain = domain.toLowerCase();
						domain = domain.replaceAll("\\s","");
						
						edit.putString(PREFS_DOMAIN, domain);
						edit.commit();
					}
				});
				
				
				// Set up Length Spinner
				SP_LENGTH = (Spinner) ROOT.findViewById(R.id.general_length);
				
				final List<String> length_array = new ArrayList<String>();
				for ( int i = 4; i <= 64; i++ ) {
					length_array.add(Integer.toString(i));
				}
				ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(getActivity().getBaseContext(), android.R.layout.simple_spinner_item, length_array);
				dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
				SP_LENGTH.setAdapter(dataAdapter);
				SP_LENGTH.setSelection(length - 4);
				
				SP_LENGTH.setOnItemSelectedListener(new OnItemSelectedListener() {
				    @Override
				    public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
				    	edit.putInt(PREFS_LENGTH, Integer.parseInt(length_array.get(position)));
				    	edit.commit();
				    }

				    @Override
				    public void onNothingSelected(AdapterView<?> parentView) {}
				});
				
				
				BT_GENERATE = (Button) ROOT.findViewById(R.id.generate_button);
				BT_GENERATE.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						generate(getActivity());
					}
				});
				
				LL_PROGRESS = (LinearLayout) ROOT.findViewById(R.id.general_progress_container);
				
				if ( MainActivity.GENERATING_TOKEN ) {
					LL_PROGRESS.setVisibility(View.VISIBLE);
					BT_GENERATE.setEnabled(false);
				}
				else {
					LL_PROGRESS.setVisibility(View.GONE);
					BT_GENERATE.setEnabled(true);
				}
				
				
			}
			
			
			// ADVANCED TAB
			else if ( index == 1 ) {
				ROOT = inflater.inflate(R.layout.advanced_fragment, container, false);
				
				// Caps
				CB_CAPS = (CheckBox) ROOT.findViewById(R.id.advanced_caps);
				CB_CAPS.setChecked(caps);
				CB_CAPS.setOnCheckedChangeListener(new OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						edit.putBoolean(PREFS_CAPS, isChecked);
						edit.commit();
					}
				});
				
				
				// Symbol Chars
				ET_SYMCHARS = (EditText) ROOT.findViewById(R.id.advanced_symchars);
				ET_SYMCHARS.setText(symchars);
				ET_SYMCHARS.setEnabled(symbols);
				ET_SYMCHARS.addTextChangedListener(new TextWatcher() {
					public void afterTextChanged(Editable s) {}
					
					public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
					
					public void onTextChanged(CharSequence s, int start, int before, int count) {
						edit.putString(PREFS_SYMCHARS, s.toString());
						edit.commit();
					}
				});
				
				
				// Symbols
				CB_SYMBOLS = (CheckBox) ROOT.findViewById(R.id.advanced_symbols);
				CB_SYMBOLS.setChecked(symbols);
				CB_SYMBOLS.setOnCheckedChangeListener(new OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						edit.putBoolean(PREFS_SYMBOLS, isChecked);
						edit.commit();
						
						ET_SYMCHARS.setEnabled(isChecked);
					}
				});
				
				
				
				// Hashes
				ET_HASHES = (EditText) ROOT.findViewById(R.id.advanced_k2);
				ET_HASHES.setText(Integer.toString(k2));
				ET_HASHES.addTextChangedListener(new TextWatcher() {
					public void afterTextChanged(Editable s) {}
					
					public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
					
					public void onTextChanged(CharSequence s, int start, int before, int count) {
						edit.putInt(PREFS_K2, Integer.parseInt(s.toString()));
						edit.commit();
					}
				});
			}
			
			return ROOT;
		}
		
		
		/**
		 * Toggle the visibility of the Progress info container and the enabled state
		 * of the Generate Button based on the GENERATING_TOKEN flag.
		 */
		public void genToken() {
			try {
				if ( MainActivity.GENERATING_TOKEN ) {
					LL_PROGRESS.setVisibility(View.VISIBLE);
					BT_GENERATE.setEnabled(false);
				}
				else {
					LL_PROGRESS.setVisibility(View.GONE);
					BT_GENERATE.setEnabled(true);
				}
			}
			catch (Exception e) {}
		}
		
		
		/**
		 * Update the progress of the Token Generation process
		 * @param progress the percent complete
		 * @param remaining the estimated time remaining
		 */
		public void updateProgress(int progress, String remaining) {
			try {
				ProgressBar pb = (ProgressBar) ROOT.findViewById(R.id.general_progress_bar);
				pb.setProgress(progress);
				
				TextView tv = (TextView) ROOT.findViewById(R.id.general_progress_text);
				tv.setText(remaining);
				
				TextView per = (TextView) ROOT.findViewById(R.id.general_progress_percent);
				per.setText(progress + "%");
			}
			catch (Exception e) {}
		}
		
		
		/**
		 * Remove the contents of the Domain field
		 */
		public void clearDomain() {
			try {
				ET_DOMAIN.setText("");
			}
			catch (Exception e) {}
		}
	}

}
