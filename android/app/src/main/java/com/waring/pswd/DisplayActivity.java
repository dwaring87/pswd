package com.waring.pswd;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class DisplayActivity extends Activity {
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setFinishOnTouchOutside(false);
		setContentView(R.layout.display);
		
		
		// Get final password and domain passed with Intent
		Bundle extras = getIntent().getExtras();
		String domain = "";
		String password = "";
		if (extras != null) {
			if ( extras.containsKey("password") && extras.containsKey("domain") ) {
				password = extras.getString("password");
				domain = extras.getString("domain");
			}
			else {
				this.finish();
			}
		}
		else {
			this.finish();
		}
		
		// Get UI Elements
		TextView TV_domain = (TextView) findViewById(R.id.display_line1);
		TV_domain.setText(Html.fromHtml("Your password for <b>" + domain + "</b> is:"));
		
		EditText ET_password = (EditText) findViewById(R.id.display_password);
		ET_password.setText(password);
		
		Button BT_dismiss = (Button) findViewById(R.id.display_dismiss);
		BT_dismiss.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				DisplayActivity.this.finish();
			}
		});
		
		Button BT_logout = (Button) findViewById(R.id.display_logout);
		BT_logout.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// Finish this Activity (and return to MainActivity)
				// with the Result extra to logout
				Bundle result = new Bundle();
			    result.putString("action", "logout");
			    Intent intent = new Intent();
			    intent.putExtras(result);
			    setResult(RESULT_OK, intent);
			    finish();
			}
		});
	}
	
	
	@Override
	public void onResume() {
		super.onResume();
	}
	
	@Override
	public void onPause() {
		super.onPause();
	}
	
}
