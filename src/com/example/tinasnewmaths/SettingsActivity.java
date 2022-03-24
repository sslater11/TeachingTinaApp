package com.example.tinasnewmaths;

import libteachingtinadbmanager.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;

/* TODO:
 * Disable the dangerous controls, like DeckGUIType and DBReadingMode.
 * Put these controls at the bottom, and Disable them. To enable them, the user has to tick a checkbox.
 * When they tick the checkbox, a dialogue with OK/Cancel will show, explaining the values should only be changed if they fully understand them.
 * Explain that they might not be able to change the values back.
 */

/* TODO:
 * Allow the users to change a deck's settings, without opening the deck.
 * This can be done by pressing the physical 'Menu' button, whilst on the Main Menu.
 * We will then load a new menu with all the decks, and they can click on a deck, and will be taken to it's settings page. 
 */

public class SettingsActivity extends Activity {
	private Spinner deck_gui, card_limit, success_limit, fail_limit;
	private CheckBox chk_are_cards_removable, chk_is_group_mode, chk_is_group_review_date;
	private TextView txt_is_group_review_date;
	private Button bt_save;
	
	private DeckSettings settings;

	
	
	
	@Override
	/*protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);
	}*/
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);
		
		// Get the settings
		String db_config_filename = getIntent().getStringExtra("deck_settings_file_path");
		File db_config_file = new File( db_config_filename );

		settings = CardDBManager.getConfig(db_config_file);
		
		bt_save = (Button) findViewById( R.id.bt_save_settings );
		txt_is_group_review_date = (TextView) findViewById( R.id.txt_is_group_review_date );
		
		// Settings variables
		deck_gui      = (Spinner) findViewById( R.id.deck_gui           );
		success_limit = (Spinner) findViewById( R.id.success_limit_list );
		fail_limit    = (Spinner) findViewById( R.id.fail_limit_list    );
		card_limit    = (Spinner) findViewById( R.id.card_limit_list    );
		
		chk_are_cards_removable  = (CheckBox) findViewById( R.id.chk_are_cards_removable  );
		chk_is_group_mode        = (CheckBox) findViewById( R.id.chk_is_group_mode        );
		chk_is_group_review_date = (CheckBox) findViewById( R.id.chk_is_group_review_date );
		
		
		// Show the user the current settings.
		deck_gui.setSelection              ( settings.getDeckGuiType()    );
		chk_are_cards_removable.setChecked ( settings.areCardsRemovable() );
		chk_is_group_mode.setChecked       ( settings.isGroupMode()       );
		chk_is_group_review_date.setChecked( settings.isGroupReviewDate() );
		EnableOrDisableGroupReviewDateBox();
		
		// Now generate the list for all Spinners.
		List<String> card_limit_counter    = new ArrayList<String>();
		List<String> fail_limit_counter    = new ArrayList<String>();
		List<String> success_limit_counter = new ArrayList<String>();
		List<String> deck_gui_list         = new ArrayList<String>();
		
		// Set the first element in the array to the current deck settings.
		card_limit_counter.add   ( settings.getCardLimitString()    );
		fail_limit_counter.add   ( settings.getFailLimitString()    );
		success_limit_counter.add( settings.getSuccessLimitString() );
		deck_gui_list.add        ( settings.getDeckGuiTypeString()  );
		
		// Set the arrays contents.
		for( int i = 1; i <= 20; i++ ) {
			card_limit_counter.add("" + i);
		}
		
		for( int i = 1; i <= 5; i++ ) {
			fail_limit_counter.add("" + i);
		}
		
		for( int i = 1; i <= 10; i++ ) {
			success_limit_counter.add("" + i);
		}
		
		for ( int i = 0; i < DeckSettings.DECK_GUI_TYPES.length; i++){
			deck_gui_list.add( DeckSettings.DECK_GUI_TYPES[i] );
		}
		
		// Set the lists to the contents of the array.
		ArrayAdapter<String> data_adapter_fl =
			new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, fail_limit_counter);
		data_adapter_fl.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		
		ArrayAdapter<String> data_adapter_cl =
			new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, card_limit_counter);
		data_adapter_cl.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		
		ArrayAdapter<String> data_adapter_sl =
			new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, success_limit_counter);
		data_adapter_sl.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		
		ArrayAdapter<String> data_adapter_dg =
			new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, deck_gui_list);
		data_adapter_dg.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		
		card_limit.setAdapter   (data_adapter_cl);
		fail_limit.setAdapter   (data_adapter_fl);
		success_limit.setAdapter(data_adapter_sl);
		deck_gui.setAdapter     (data_adapter_dg);
		
		bt_save.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				SavePressed();
			}
		});
		
		chk_is_group_mode.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				EnableOrDisableGroupReviewDateBox();
			}
		});

	}
	
	
	public void EnableOrDisableGroupReviewDateBox() {
		if( chk_is_group_mode.isChecked() ) {
			chk_is_group_review_date.setEnabled( true );
			txt_is_group_review_date.setEnabled( true );
		} else {
			chk_is_group_review_date.setEnabled( false );
			txt_is_group_review_date.setEnabled( false );
		}
	}

	public void SavePressed() {
		String str_deck_gui =                  String.valueOf(      deck_gui.getSelectedItem() );
		int i_card_limit    = Integer.valueOf( String.valueOf(    card_limit.getSelectedItem() ) );
		int i_fail_limit    = Integer.valueOf( String.valueOf(    fail_limit.getSelectedItem() ) );
		int i_success_limit = Integer.valueOf( String.valueOf( success_limit.getSelectedItem() ) );
		boolean b_are_cards_removable  = chk_are_cards_removable.isChecked();
		boolean b_is_group_mode        = chk_is_group_mode.isChecked();
		boolean b_is_group_review_date = chk_is_group_review_date.isChecked();
		
		
		
		settings.setDeckGuiType      ( str_deck_gui           );
		settings.setCardLimit        ( i_card_limit           );
		settings.setFailLimit        ( i_fail_limit           );
		settings.setSuccessLimit     ( i_success_limit        );
		settings.setAreCardsRemovable( b_are_cards_removable  );
		settings.setIsGroupMode      ( b_is_group_mode        );
		settings.setIsGroupReviewDate( b_is_group_review_date );
		
		CardDBManager.writeConfigFile(settings);
		
		// Go back to the main menu screen.
		Intent intent = new Intent(SettingsActivity.this, MenuActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);   
		startActivity(intent);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		finish();
	}

}
