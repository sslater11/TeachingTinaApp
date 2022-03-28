package com.example.tinasnewmaths;

import libteachingtinadbmanager.*;

import java.io.File;
import java.util.ArrayList;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/* TODO:
 * Maybe change the way the deck is passed to the deck's activity.
 * At the moment we use Intent.putExtra(), and pass a string.
 * Passing an object is harder, but it's more efficient, so maybe work on this when everything else is done.
 */


class MenuButtonOnClickListener implements View.OnClickListener {
	protected Deck deck;
	protected MenuActivity activity;
	
	public MenuButtonOnClickListener(Deck d, MenuActivity a) {
		super();
		deck = d;
		activity = a;
	}
	
	@Override
	public void onClick(View v) {
        // Perform action on click
		studyDeck();
    }

	
	Deck getDeck() {
		return deck;
	}
	
	void studyDeck() {
		Intent openFlashcardActivity = null;
		if(        deck.getDeckGuiType() == DeckSettings.DECK_GUI_TYPE_FLASHCARDS ) {
			if( deck.settings.isGroupMode() ) {
				// Load FlashcardGroupActivity instead.
				System.out.println("Loading FlashcardGroup activity...");
				
				// Make the intent, and pass the deck to it.
				openFlashcardActivity = new Intent(activity, FlashcardGroupActivity.class);
			} else {
				// Load flashcard activity
				System.out.println("Loading Flashcards activity...");
				
				// Make the intent, and pass the deck to it.
				openFlashcardActivity = new Intent(activity, FlashcardActivity.class);
			}
		} else if( deck.getDeckGuiType() == DeckSettings.DECK_GUI_TYPE_MATHS ) {
			// Load maths activity
			System.out.println("Loading Maths activity...");
			// Make the intent, and pass the deck to it.
			openFlashcardActivity = new Intent(activity, MathsActivity.class);

		} else if( deck.getDeckGuiType() == DeckSettings.DECK_GUI_TYPE_READING_AND_SPELLING ) {
			// Load reading and spelling activity
			openFlashcardActivity = new Intent( activity, ReadingAndSpellingActivity.class );

		} else if( deck.getDeckGuiType() == DeckSettings.DECK_GUI_TYPE_KEYBOARD ) {
			// Load keyboard activity
			
		} else {
			System.out.println("Unknown deck gui type set. Exiting.");
			System.exit(700);
		}
		
		// Start the chosen activity.
		System.out.println(deck.getFilePath());
		openFlashcardActivity.putExtra("deck_file_path", deck.getFilePath());
		openFlashcardActivity.putExtra("deck_settings_file_path", deck.settings.getFilePath());
		activity.startActivity(openFlashcardActivity);
	}
}
public class MenuActivity extends Activity {
/*
	public void loadActivity( Deck deck ) {
		Intent openMathsActivity = new Intent(MenuActivity.this, MathsActivity.class);
		Bundle b = new Bundle();
		b.putSerializable("Deck",deck);
		openMathsActivity.putExtras(b);
		
		//Intent openMathsActivity = new Intent(MenuActivity.this, MathsActivity.class);
		//openMathsActivity.putExtra("Deck", deck);
		startActivity(openMathsActivity);
	}*/
	private void testListingFiles() {
	/*	String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			if (Build.VERSION.SDK_INT >= 23) {
				if (checkPermission()) {
					File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/");
					if (dir.exists()) {
						Log.d("path", dir.toString());
						File list[] = dir.listFiles();
						for (int i = 0; i < list.length; i++) {
							myList.add(list[i].getName());
							Log.d("cccccccc", list[i].toString() );
						}
					}
				} else {
					requestPermission(); // Code for permission
				}
			} else {
				File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/");
				if (dir.exists()) {
					Log.d("path", dir.toString());
					File list[] = dir.listFiles();
					for (int i = 0; i < list.length; i++) {
						myList.add(list[i].getName());
					}
					ArrayAdapter arrayAdapter = new ArrayAdapter(MainActivity.this, android.R.layout.simple_list_item_1, myList);
					listview.setAdapter(arrayAdapter);
				}
			}
		}

		// Android API 30 needs a different way of getting a list of files, so test for that.
		if( Build.VERSION.SDK_INT >= 30 ) {
			Log.d("bbbbbbbbbbbbb", "it's above it yay");
			Log.d("bbbbbbbbbbbbb", Environment.isExternalStorageManager() + "");
			//// Check if we have access to files
			//if( ! Environment.isExternalStorageManager() ) {

			//    //// Ask for the user to grant access to the external storage using the system settings.
			//	//Uri uri = Uri.parse("package:" + BuildConfig.APPLICATION_ID);
			//	//Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri);

			//	// Check if we need permission to access the external storage and ask for it.
			//	if (ContextCompat.checkSelfPermission(
			//			MenuActivity.this, Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION) ==
			//			PackageManager.PERMISSION_GRANTED) {
			//		// You can use the API that requires the permission.
			//		performAction(...);
			//	} else if (shouldShowRequestPermissionRationale(...)) {
			//		// In an educational UI, explain to the user why your app requires this
			//		// permission for a specific feature to behave as expected. In this UI,
			//		// include a "cancel" or "no thanks" button that allows the user to
			//		// continue using your app without granting the permission.
			//		showInContextUI(...);
			//	} else {
			//		// You can directly ask for the permission.
			//		// The registered ActivityResultCallback gets the result of this request.
			//		requestPermissionLauncher.launch(
			//				Manifest.permission.REQUESTED_PERMISSION);
			//	}

			//}

		} else {
			// get the list of files the old way.
		}

	 */
	}

	private void requestPermission() {
	/*	if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,  android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
			Toast.makeText(MainActivity.this, "Write External Storage permission allows us to read  files. Please allow this permission in App Settings.", Toast.LENGTH_LONG).show();
		} else {
			ActivityCompat.requestPermissions(MainActivity.this, new String[]
					{android.Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
		}

	 */
	}


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_menu);
		testListingFiles();
		String folder_name = "Teaching Tina";
		
		ArrayList<Button> bArr = new ArrayList<Button>();
		ArrayList<Deck>         db_deck          = new ArrayList<Deck>();
		ArrayList<DeckSettings> db_deck_settings = new ArrayList<DeckSettings>();
		
		ArrayList<File> db_files        = CardDBManager.scanDirectory(folder_name);
		ArrayList<File> db_config_files = CardDBManager.scanDirectory(folder_name);
		
		TextView tv_load_deck = (TextView) findViewById(R.id.textViewLoadDeck);

		if( (db_files != null) &&  (db_files.size() >  0) ) {
			// Set the config files extension to '.conf'.
			for( int i = 0; i < db_config_files.size(); i++ ) {
				String str = db_config_files.get(i).getAbsoluteFile().toString();
				str = str.split("\\.txt")[0];
				Log.d("tinasnewmaths", "aaaaaaaaaaaaaaaaa" + str);
				str = str + ".conf";
				db_config_files.set( i,  new File(str) );
			}
			

			/*
			 * Make a button for each database, and add it to the layout.
			 */
			LinearLayout mainLinearLayout = (LinearLayout) findViewById(R.id.menu_linear_layout);
			for( int i = 0; i < db_files.size(); i++ ) {
				//now create button dynamically like this
				bArr.add( new Button(this) );
				
				// Get the database's name.
				String str = db_files.get(i).getName();
				str = str.split(".txt")[0];
				
				// Set the button's text and add it to the view.
				bArr.get(i).setText(str);
				bArr.get(i).setEnabled(false);
				mainLinearLayout.addView(bArr.get(i));
				
			}
			
			/*
			 * Scan each database file and enable buttons for those with reviews.
			 */
			for( int i = 0; i < db_files.size(); i++ ) {
				// Load the settings into the DeckSettings ArrayList.
				db_deck_settings.add( CardDBManager.getConfig(db_config_files.get(i)) );
				// Get a deck, and check it.
				ArrayList<Card> tmp_deck = CardDBManager.readDB(db_files.get(i), db_deck_settings.get(i));
				
				if( tmp_deck == null ) {
					db_deck.add(null);
					
					// Set the button's text.
					String str = db_files.get(i).getName();
					str = str.split(".txt")[0];
					str = str + " - Error in file.";
					bArr.get(i).setText( str );
				} else if( tmp_deck.size() == 0 ) {
					// The deck doesn't need a review, add a null to the deck list, since it won't be opened anyway.
					db_deck.add(null);
					
					// Set the button's text.
					String str = db_files.get(i).getName();
					str = str.split(".txt")[0];
					str = str + " - No Reviews.";
					bArr.get(i).setText( str );
				} else {
					Deck d;
					if ( /***/ db_deck_settings.get(i).getDeckGuiType() == DeckSettings.DECK_GUI_TYPE_FLASHCARDS ) {
						if( db_deck_settings.get(i).isGroupMode() ) {
							d = new FlashcardGroupDeck ( tmp_deck, db_files.get(i), db_deck_settings.get(i) );
						} else {
							d = new FlashcardDeck      ( tmp_deck, db_files.get(i), db_deck_settings.get(i) );
						}
					} else if( db_deck_settings.get(i).getDeckGuiType() == DeckSettings.DECK_GUI_TYPE_MATHS ) {
						d = new MathsDeck          ( tmp_deck, db_files.get(i), db_deck_settings.get(i) );

					} else if( db_deck_settings.get(i).getDeckGuiType() == DeckSettings.DECK_GUI_TYPE_KEYBOARD ) {
						d = new KeyboardDeck       ( tmp_deck, db_files.get(i), db_deck_settings.get(i) );
					} else if( db_deck_settings.get(i).getDeckGuiType() == DeckSettings.DECK_GUI_TYPE_READING_AND_SPELLING) {
						d = new ReadingLessonDeck ( tmp_deck, db_files.get(i), db_deck_settings.get(i) );
					} else {
						// This should never happen.
						System.out.println("Error: deck gui type is wrong.");
						d = null;
					}

					db_deck.add( d );
					// Set the onClickListener for just the enabled buttons
					bArr.get(i).setOnClickListener( new MenuButtonOnClickListener(db_deck.get(i), MenuActivity.this) );
					bArr.get(i).setTextSize(25);
					bArr.get(i).setEnabled(true);
				}
			}
			
			
			
			System.out.println("no null");
		} else {
			// Display this message to the user in a text box.
			File location = new File(Environment.getExternalStorageDirectory(), folder_name);
			
			float text_size;
			String text;
			
			
			if( db_files == null ) {
				// No folder found.
				text = "No Folder Found.\n";
			} else {
				// No files found in the folder.
				text = "No Files Found.\n";
			}
			text = text
			     + "\n"
			     + "Make sure you have the folder \"Teaching Tina\" on your SD Card.\n"
			     + "Make sure you have some database files ending with '.txt' in that folder.\n"
			     + "\n"
			     + "If you have the folder on your SD card, maybe try it on android's internal storage\n"
			     + "Some android phones sway the internal and external storage...\n"
			     + "\n"
			     + "This app looked for the folder here:\n"
			     + location.getAbsolutePath();
			
			text_size = (float) 20;
			tv_load_deck.setText(text);
			tv_load_deck.setTextSize(text_size);
			
			
			System.out.println("No folder/files found.");
			System.out.println("Make sure you have the folder \"Teaching Tina\" on your SD Card");
			System.out.println("Make sure you have some database files ending with '.txt'");
			System.out.println();
			System.out.println("If you have the folder on your SD card, maybe try it on androids internal storage");
			System.out.println("Some android phones sway the internal and external storage...");
			System.out.println("nulled it");
		}

	}

	@Override
	protected void onPause() {
		super.onPause();
		finish();
	}

}
