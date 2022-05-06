/* © Copyright 2022, Simon Slater

This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, version 2 of the License.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package com.example.tinasnewmaths;

import libteachingtinadbmanager.*;

import java.io.File;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ScaleDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;


import java.io.File;
import java.util.ArrayList;

import android.app.Activity;

public class FlashcardGroupActivity extends FlashcardBaseActivity {
	// This will override the original deck from FlashcardBaseActivity.
	// It was originally of type FlashcardDeck()
	//FlashcardGroupDeck deck;
	DeckSettings settings;
	
	Spinner group_list;
	
	ArrayList<String> group_names;
	
	protected boolean is_level_spinners_first_time = true; // Stops the level spinner from running the first time.
	                                                       // The first time it's run is at initialisation, which I don't want!

	@Override
	protected void onCreate(Bundle savedInstanceState ) {
		super.onCreate(savedInstanceState);
		setContentView( R.layout.activity_flashcard_group );
		init();
	}
	protected void onCreate(Bundle savedInstanceState, int layout_id ) {
		super.onCreate(savedInstanceState);
		setContentView(layout_id);
		init();
	}

	private void init() {
		scroll_layout = (LinearLayout) findViewById(R.id.flashcard_group_scroll_layout);
		scroll_view   = (ScrollView)   findViewById(R.id.flashcard_group_scroll_view);

		group_list     = (Spinner)     findViewById( R.id.group_list);
		bt_show_answer = (Button)      findViewById( R.id.bt_fg_show_answer);
		bt_correct     = (ImageButton) findViewById( R.id.bt_fg_correct );
		bt_incorrect   = (ImageButton) findViewById( R.id.bt_fg_incorrect );

		String str_db_file = getIntent().getStringExtra("deck_file_path");
		db_file         = new File( str_db_file );
		db_config_file  = new File( getIntent().getStringExtra("deck_settings_file_path") );
		db_media_folder = new File( str_db_file.replace(".txt", "") );
		
		settings = CardDBManager.getConfig(db_config_file);
		
		if( settings.isGroupReviewDate() ) {
			// Load a group by review date.
			ArrayList<Card> tmp_deck = CardDBManager.readDBGetGroup(db_file, settings);
			deck = new FlashcardGroupDeck(tmp_deck, db_file, settings);
			
			setGroupChangeDisabled();
			group_names = new ArrayList<String>();
			group_names.add(deck.getCurrentCard().group.getGroupName());
			setGroupList(group_names);
		} else {
			// Get a list of group names and load the first group.
			// TODO:
			// Make an is_auto_play boolean for DeckSettings. If it's true, play the audio when a card is loaded, even when the deck is changed.
			group_names = CardDBManager.readDBGetGroupNames(db_file, settings);
			ArrayList<Card> tmp_deck = CardDBManager.readDBGetGroup(db_file, settings, group_names.get(0));
			deck = new FlashcardGroupDeck( tmp_deck, db_file, settings);
			
			setGroupChangeEnabled();
			setGroupList(group_names);
			
			group_list.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
				public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
					// This if statement stops it from running the first time.
					// If not used, this code will run when the spinner is initialised, which isn't what we want.
					
					if( is_level_spinners_first_time == false ) {
						String name = String.valueOf( group_list.getSelectedItem() );
						ArrayList<Card> tmp_deck = CardDBManager.readDBGetGroup(db_file, settings, name);
						deck = new FlashcardGroupDeck( tmp_deck, db_file, settings);
						NextQuestion();
					} else {
						// Set it to false, so it will run the next time.
						is_level_spinners_first_time = false;
					}
				}
				public void onNothingSelected(AdapterView<?> parentView) {
					// your code here
				}
			});
		}
		
		bt_show_answer.setVisibility(View.VISIBLE);
		bt_correct.setVisibility    (View.GONE);
		bt_incorrect.setVisibility  (View.GONE);

		DisableButtons();
		NextQuestion();
		
		bt_show_answer.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				ShowAnswerPressed();
			}
		});

		bt_correct.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				CorrectPressed();
			}
		});

		bt_incorrect.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				IncorrectPressed();
			}
		});
	}

	public void setGroupChangeDisabled() {
		group_list.setClickable( false );
	}
	
	public void setGroupChangeEnabled() {
		group_list.setClickable( true );
	}
	
	public void setGroupList(ArrayList<String> new_list) {
		/*ArrayList<String> new_list = new ArrayList<String>();
		
		
		if( settings.isGroupReviewDate() ) {
			CardDBManager.readDBGetGroupNames(db_file)
		}

		// Set the current level as the prompt.
		if( (! settings.isGroupReviewDate()) && deck != null ) {
			// Make the array list with the current group name at the start.
			new_list.add(deck.getCurrentCard().getGroupName());
			for( int i = 0; i < group_names.size(); i++ ) {
				new_list.add(group_names.get(i));
			}
		} else {
			new_list.add( deck.getCurrentCard().getGroupName() );
		}
*/
		// Add the ArrayList to the Spinner.
		ArrayAdapter<String> data_adapter_gl =
				new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, new_list);
		data_adapter_gl.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		
		group_list.setAdapter   (data_adapter_gl);
	}
	
	
	public void DisplayLevelFinished() {
		
	}
	
	@Override
	public void NextQuestion() {
		if( settings.isGroupReviewDate() ) {
			super.NextQuestion();
		} else {
			if( ! deck.isLearnt() ) {
				// It's not the last card, so continue.
				ResetQuestionField();
				DisplayQuestion();
				EnableButtons();
			} else {
				// Clear the screen.
				ResetQuestionField();
				DisableButtons();
				
				ArrayList<String> str = new ArrayList<String>();
				// Check if we're on the last level.
				String s_name1 = deck.getCurrentCard().group.getGroupName();
				String s_name2 = group_names.get( group_names.size() -1 );
				if( s_name1.compareTo(s_name2) == 0 ) {
					str.add("You've finished the last level!");
					str.add("Well Done!");
					str.add(" ");
					str.add("You Rock!");
				} else {
					str.add("You've finished this level.");
					str.add("Why not go on to the next!");
				}
				
				// Reset the font size for AddContent().
				font_size = DEFAULT_FONT_SIZE;
				AddContent(str);
			}
		}
	}
}
