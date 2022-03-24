package com.example.tinasnewmaths;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

public class FinishedActivity extends Activity {
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_finished);
		
		int [] rand_smiley = {
			R.drawable.smiley1,
			R.drawable.smiley2,
			R.drawable.smiley3,
			R.drawable.smiley4,
			R.drawable.smiley5,
			R.drawable.smiley6,
			R.drawable.smiley7,
			R.drawable.smiley8,
			R.drawable.smiley9,
			R.drawable.smiley10,
			R.drawable.smiley11 };
		ImageButton smiley_button;
	
		smiley_button = (ImageButton) findViewById(R.id.smileyImageButton);
		
		int random = (int)((Math.random() * rand_smiley.length));
		random = rand_smiley[random];
		
		smiley_button.setImageResource(random);
		
		smiley_button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// Go back to the main menu.
				/*Intent openMainMenuActivity = new Intent("android.intent.action.MAIN");
				FinishedActivity.this.startActivity(openMainMenuActivity);*/
				
				Intent intent = new Intent(FinishedActivity.this, MenuActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);   
				startActivity(intent);
			}
		});
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		finish();
	}
}
