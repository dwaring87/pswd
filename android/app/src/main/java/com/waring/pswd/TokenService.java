package com.waring.pswd;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.util.Log;

import com.waring.pswd.PSWD.HashListener;

public class TokenService extends Service {
	
	
	private final int id = 1;
	
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		
		// Get User Name and Master Password from Intent
		Bundle bundle = intent.getExtras();
		final String username = bundle.getString("username");
		final String password = bundle.getString("password");
		
		// Set up a Notification
		final NotificationManager mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		final Builder mBuilder = new NotificationCompat.Builder(this);
		mBuilder.setContentTitle("Generating User Token")
		    .setTicker("Generating User Token...")
		    .setSmallIcon(R.drawable.ic_notification)
		    .setOngoing(true);
		mNotifyManager.notify(id, mBuilder.build());
		
		
		// Generate a new User Token in a new Thread
		new Thread(
		    new Runnable() {
		        @Override
		        public void run() {
		            
		        	// Notification click Intent
		        	Intent notificationIntent = new Intent(TokenService.this, MainActivity.class);
		            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		            final PendingIntent intent = PendingIntent.getActivity(TokenService.this, 0, notificationIntent, 0);
		        	
		            // Call the Generate User Token Function
		            String usertoken = PSWD.generateUserToken(username, password, new HashListener() {
						
		            	/**
		            	 * HashListener updateProgress()
		            	 * Update the Ongoing Notification
		            	 * Send a Broadcast Intent with the Progress
		            	 *     -- this is received in LoginActivity and MainActivity
		            	 */
		            	@Override
						public void updateProgress(int progress, String remaining) {
							Log.d("PSWD", "Listener Progress: " + progress + " (" + remaining + ")");
							
							mBuilder.setProgress(100, progress, false)
								.setContentText(remaining)
								.setContentIntent(intent)
								.setContentInfo(progress + "%");
		                    mNotifyManager.notify(id, mBuilder.build());
		                    
		                    // Send Loading Broadcast Intent
		                    Intent broadcast = new Intent(MainActivity.BR_INTENT);
		                    broadcast.putExtra("username", username);
		                    broadcast.putExtra("password", password);
		                    broadcast.putExtra("token", "");
		                    broadcast.putExtra("action", "loading");
		                    broadcast.putExtra("progress", progress);
		                    broadcast.putExtra("remaining", remaining);
		        	        getBaseContext().sendBroadcast(broadcast);
						}
		            	
					});
	            	
		            
		            // Save User Token to SharedPreferences cache
		            PSWD.saveUserToken(TokenService.this, username, usertoken);
		            
		            // Create complete flag
		            // This is used to tell the MainActivity that the Token Generation process is complete
		            // if the Activity does not receive the Broadcast (ie, Activity in background)
		            // the next time it is resumed
		            SharedPreferences prefs = TokenService.this.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
		            Editor edit = prefs.edit();
		            edit.putBoolean(MainActivity.PREFS_TOKEN_GEN_COMPLETE, true);
		            edit.commit();
		            
		        	// Notification click Intent
		            notificationIntent = new Intent(TokenService.this, MainActivity.class);
		            notificationIntent.putExtra("username", username);
		            notificationIntent.putExtra("password", password);
		            notificationIntent.putExtra("token", usertoken);
		            final PendingIntent completeIntent = PendingIntent.getActivity(TokenService.this, 0, notificationIntent, 0);
		            
		            // Update final Notification
		            mBuilder.setContentText("User Token Generated")
		            	.setTicker("User Token Generated")
		            	.setContentInfo("100%")
		            	.setContentIntent(completeIntent)
		            	.setOngoing(false)
		            	.setAutoCancel(true)
	                    .setProgress(0,0,false);
		            mNotifyManager.notify(id, mBuilder.build());
		            
		            
		            // Send Complete Broadcast Intent
                    Intent broadcast = new Intent(MainActivity.BR_INTENT);
                    broadcast.putExtra("username", username);
                    broadcast.putExtra("password", password);
                    broadcast.putExtra("token", usertoken);
                    broadcast.putExtra("action", "complete");
        	        getBaseContext().sendBroadcast(broadcast);
		        }
		    }
		).start();
		
		
		
		return Service.START_NOT_STICKY;
	}
	
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	

}
