package com.waring.pswd;

import java.util.ArrayList;
import java.util.List;

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
import android.text.Html;
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

public class MainActivity extends Activity implements ActionBar.TabListener {
	
	
	// CURRENT USER INFORMATION
	private static String USERNAME = "";
	private static String MASTER_PASSWORD = "";
	private static String USER_TOKEN = "";
	
	
	// TABS
	private String[] TAB_TITLES = new String[]{"General", "Advanced"};
	private final int TAB_COUNT = TAB_TITLES.length;
	private List<Fragment> FRAG_LIST = new ArrayList<Fragment>();
	
	// STATE VARIABLES
	private static final String STATE_SELECTED_NAVIGATION_ITEM = "selected_navigation_item";
	private static final String STATE_USERNAME = "state_username";
	private static final String STATE_PASSWORD = "state_password";
	private static final String STATE_TOKEN = "state_token";
	
	
	// DEFAULT SETTINGS
	private static final int DEFAULT_LENGTH = 24;
	private static final boolean DEFAULT_CAPS = true;
	private static final boolean DEFAULT_SYMBOLS = true;
	private static final String DEFAULT_SYMCHARS = "!@$*-_.?";
	protected static final int DEFAULT_K1 = 10000000;
	private static final int DEFAULT_K2 = 250;
	
	// SETTINGS KEYS
	protected static final String PREFS_NAME = "PSWD_PREFS";
	private static final String PREFS_DOMAIN = "pref_domain";
	private static final String PREFS_LENGTH = "pref_length";
	private static final String PREFS_CAPS = "pref_caps";
	private static final String PREFS_SYMBOLS = "pref_symbols";
	private static final String PREFS_SYMCHARS = "pref_symchars";
	private static final String PREFS_K2 = "pref_k2";
	
	protected static final String PREFS_TOKEN_GEN_COMPLETE = "pref_token_gen_complete"; 
	
	protected static final String PREFS_ENC_USERNAME = "pref_enc_username";
	protected static final String PREFS_ENC_PASSWORD = "pref_enc_password";
	
	
	// Flag for Token Generation
	protected static boolean GENERATING_TOKEN = false;
	
	// Broadcast Receiver to Monitor Token Generation Progress
	private BroadcastReceiver BR_TOKEN_GEN;
	protected static final String BR_INTENT = "GEN_TOKEN_BROADCAST";
	
	
	// Login Request Code
	private static final int LOGIN_REQUEST = 1;
	
	
	// More Information URL
	private final String INFO_URL = "http://pswd.davidwaring.net/info.html";
	
	// Web Version URL
	private final String WEB_URL = "https://pswd.davidwaring.net/";
	
	
	
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
	    
	    
	    // Show the Login Activity if we're missing user info and a token is not being generated
	    if ( !GENERATING_TOKEN && (MainActivity.USERNAME.equals("") || MainActivity.MASTER_PASSWORD.equals("") || MainActivity.USER_TOKEN.equals("")) ) {
	    	login();
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
	
	
	@Override
	public void onPause() {
		super.onPause();
		
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
	
	
	
	// CONFIG CHANGE
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
	    super.onConfigurationChanged(newConfig);
	}

	
	
	
	// SAVE AND RESTORE TAB STATE
	
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

	  
	@Override
	public void onSaveInstanceState(Bundle outState) {
		// Serialize the current tab position.
		outState.putInt(STATE_SELECTED_NAVIGATION_ITEM, getActionBar().getSelectedNavigationIndex());
		outState.putString(STATE_USERNAME, USERNAME);
		outState.putString(STATE_PASSWORD, MASTER_PASSWORD);
		outState.putString(STATE_TOKEN, USER_TOKEN);
	}
	
	
	

	
	
	
	// ACTION BAR MENU
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		
		if ( id == R.id.action_info ) {
			Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(INFO_URL));
			startActivity(browserIntent);
		}
		else if ( id == R.id.action_info ) {
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
	 * Receive the Result of the Login Activity
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
	}
	
	
	
	// LOGOUT
	
	/**
	 * Log out the current user and show the Login Activity
	 * Erase the user's cached User Token, if requested
	 * See completeLogout() for Part 2
	 * @param erase true to erase the User Token
	 */
	public void logout(boolean erase) {
		
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
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle("Generated Password")
			.setMessage(Html.fromHtml("Your password for <b>" + domain + "</b> is:<br /><br />" + final_password + "<br /><br /><font size='10'><i>Password is copied to clipboard.  Long-press a text field to paste.</i></font>"))
			.setCancelable(true)
			.setPositiveButton("Dismiss", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					dialog.dismiss();
				}
			});
		AlertDialog alert = builder.create();
		alert.show();
	}
	
	
	

	/**
	 * The fragment containing the tab's content
	 */
	public static class TabFragment extends Fragment {
		/**
		 * The fragment argument representing the section number for this
		 * fragment.
		 */
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