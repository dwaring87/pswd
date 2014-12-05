package com.waring.pswd;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This Activity displays the Login screen where a User enters their
 * username/nickname and their master password.  It is started by the
 * {@link com.waring.pswd.MainActivity} when a user is not logged in.
 * <p />
 * If a previously generated user token is not found for the user, prompt
 * the user to generate a new token and start {@link com.waring.pswd.TokenService}.
 * <p />
 * If a previously generated user token is found for the user, return to
 * the {@link com.waring.pswd.MainActivity}.
 */
public class LoginActivity extends Activity {
	
	// UI Elements

    /** The EditText for the Username */
	private EditText ET_username;

    /** The EditText for the Master Password */
	private EditText ET_password;

    /** The CheckBox to remember the Username */
	private CheckBox CB_username;

    /** The CheckBox to remember the Master Password */
	private CheckBox CB_password;

    /** The Login Button */
	private Button BT_login;


	/** The Broadcast Receiver that receives the token generation progress */
	private BroadcastReceiver BR_TOKEN_GEN;


    /**
     * Set up the UI elements for the login form.  If there are saved credentials available,
     * fill them into the form.
     * @param savedInstanceState
     */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setFinishOnTouchOutside(false);
		setContentView(R.layout.login);
		
		
		// Reset Container Visibility
		ScrollView SV_formContainer = (ScrollView) findViewById(R.id.login_form_container);
		ScrollView SV_generateContainer = (ScrollView) findViewById(R.id.login_generate_container);
		
		SV_formContainer.setVisibility(View.VISIBLE);
		SV_generateContainer.setVisibility(View.GONE);
		
		
		// Set no registration message
		TextView reg = (TextView) findViewById(R.id.login_registration);
		reg.setText(Html.fromHtml("<i>No registration is necessary, just pick a user name to identify yourself and a <u>very strong</u> master password to secure your site-specifc passwords.  Just use the same user name and master password each time you generate a site password.</i>"));
		
		
		// Get form elements
		ET_username = (EditText) findViewById(R.id.login_username);
		ET_password = (EditText) findViewById(R.id.login_password);
		CB_username = (CheckBox) findViewById(R.id.login_remember_username);
		CB_password = (CheckBox) findViewById(R.id.login_remember_password);
		BT_login = (Button) findViewById(R.id.login_button);
		
		
		// Read saved user name and master password
		SharedPreferences sharedPref = this.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
		String saved_username = PSWD.decrypt(sharedPref.getString(MainActivity.PREFS_ENC_USERNAME, ""));
		String saved_password = PSWD.decrypt(sharedPref.getString(MainActivity.PREFS_ENC_PASSWORD, ""));
		
		ET_username.setText(saved_username);
		ET_password.setText(saved_password);

        // Set remember checkboxes based on previously saved info
		if ( !saved_username.equals("") ) {
			CB_username.setChecked(true);
		}
		else {
			CB_username.setChecked(false);
		}
		
		if ( !saved_password.equals("") ) {
			CB_password.setChecked(true);
		}
		else {
			CB_password.setChecked(false);
		}
		
		// Alert remember password (let the user know this is a bad idea)
		CB_password.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if ( isChecked ) {
					new AlertDialog.Builder(LoginActivity.this)
						.setTitle("Security Warning!")
						.setMessage("Are you really sure about this?!  The app will save your master password and anyone with access to this device will be able to generate your passwords! THIS IS NOT RECOMMENDED!")
						.setPositiveButton("Save", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								CB_password.setChecked(true);
							}
						})
						.setNegativeButton("Don't Save", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								CB_password.setChecked(false);
							}
						})
						.show();
				}
			}
		});


        /**
         * Process login
         * If a user token is found in the cache, return to the MainActivity
         * If a user token is not found, show the Generate User Token Screen, then start the TokenService
         */
		BT_login.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				final String username = ET_username.getText().toString();
				final String password = ET_password.getText().toString();
				
				SharedPreferences sharedPref = getBaseContext().getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
				final Editor edit = sharedPref.edit();
				
				
				// User name and Password Cannot be empty
				if ( username.equals("") ) {
					Toast.makeText(getBaseContext(), "Username cannot be blank", Toast.LENGTH_LONG).show();
					return;
				}
				
				if ( password.equals("") ) {
					Toast.makeText(getBaseContext(), "Master Password cannot be blank", Toast.LENGTH_LONG).show();
					return;
				}
				
				
				// Save user name, if requested
				if ( CB_username.isChecked() ) {
					edit.putString(MainActivity.PREFS_ENC_USERNAME, PSWD.encrypt(username));
				}
				else {
					edit.putString(MainActivity.PREFS_ENC_USERNAME, "");
				}
				
				// Save master password, if requested
				if ( CB_password.isChecked() ) {
					edit.putString(MainActivity.PREFS_ENC_PASSWORD, PSWD.encrypt(password));
				}
				else {
					edit.putString(MainActivity.PREFS_ENC_PASSWORD, "");
				}
				
				edit.commit();
				
				
				
				// Check for saved User Token
				boolean hasToken = PSWD.hasUserToken(LoginActivity.this, username);
				
				
				// Use Cached Token
				if ( hasToken ) {
					String usertoken = PSWD.getUserToken(LoginActivity.this, username);
					
					// Return to the Main Activity
					Intent returnIntent = new Intent();
					returnIntent.putExtra("username", username);
					returnIntent.putExtra("password", password);
					returnIntent.putExtra("token", usertoken);
					setResult(RESULT_OK, returnIntent);
					finish();
				}
				
				// Generate User Token
				else {
					
					// SHOW THE GENERATE USER TOKEN SCREEN
					final ScrollView SV_formContainer = (ScrollView) findViewById(R.id.login_form_container);
					final ScrollView SV_generateContainer = (ScrollView) findViewById(R.id.login_generate_container);
					
					SV_formContainer.setVisibility(View.GONE);
					SV_generateContainer.setVisibility(View.VISIBLE);
					
					
					// Hide Soft Keyboard
					InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(ET_password.getWindowToken(), 0);
					
					
					// Verify Password Field
					final EditText ET_password_verify = (EditText) findViewById(R.id.login_password_verify);
					ET_password_verify.setEnabled(true);
					
					// Back Button
					final Button BT_back = (Button) findViewById(R.id.login_button_back);
					BT_back.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							SV_formContainer.setVisibility(View.VISIBLE);
							SV_generateContainer.setVisibility(View.GONE);
						}
					});
					
					// Generate Button
					final Button BT_generate = (Button) findViewById(R.id.login_button_generate);
					BT_generate.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							// Get the second password
							String password2 = ET_password_verify.getText().toString();
							
							// Make sure passwords match
							if ( !password.equals(password2) ) {
								Toast.makeText(LoginActivity.this, "Passwords don't match", Toast.LENGTH_LONG).show();
								return;
							}
							
							
							// Disable Password Field and Buttons
							ET_password_verify.setEnabled(false);
							BT_generate.setEnabled(false);
							BT_back.setEnabled(false);
							
							
							// START THE TOKEN GENERATING SERVICE
							Intent i = new Intent(getBaseContext(), TokenService.class);
							i.putExtra("username", username);
							i.putExtra("password", password);
							getBaseContext().startService(i);
							
							
							// Return to MainActivity
							Intent returnIntent = new Intent();
							returnIntent.putExtra("username", username);
							returnIntent.putExtra("password", password);
							returnIntent.putExtra("token", "loading");
							setResult(RESULT_OK, returnIntent);
							finish();
						}
					});
				}
			}
		});
		
		
		
	}


    /**
     * Register the Token Broadcast Receiver
     */
	@Override
	public void onResume() {
		super.onResume();
		
		// Register Token BR
		// Close the LoginActivity when a Token is being generated
 		IntentFilter intentFilter = new IntentFilter(MainActivity.BR_INTENT);
        BR_TOKEN_GEN = new BroadcastReceiver() {
             @Override
             public void onReceive(Context context, Intent intent) {
            	 Intent returnIntent = new Intent();
            	 returnIntent.putExtra("username", intent.getStringExtra("username"));
            	 returnIntent.putExtra("password", intent.getStringExtra("password"));
            	 returnIntent.putExtra("token", "loading");
            	 LoginActivity.this.setResult(RESULT_OK, returnIntent);
            	 LoginActivity.this.finish();
             }
         };
         this.registerReceiver(BR_TOKEN_GEN, intentFilter);
	}


    /**
     * Unregister the Token Broadcast Receiver
     */
	@Override
	public void onPause() {
		super.onPause();
		
		// Unregister the DB Update Receiver
    	this.unregisterReceiver(BR_TOKEN_GEN);
	}
	
	
	
	
	
	
	@Override
	public void onBackPressed() {
	    moveTaskToBack(true);
	}
}
