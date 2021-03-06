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
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;


class KeyboardDeck extends Deck {
	KeyboardDeck(String db_filename, String db_config_filename) {
		super(db_filename, db_config_filename);
	}
	KeyboardDeck( ArrayList<Card> d, File deck_file_path, DeckSettings s ) {
		super(d, deck_file_path, s);
	}
	@Override
	public ArrayList<String> getAnswer() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public ArrayList<String> getQuestion() {
		// TODO Auto-generated method stub
		return null;
	}
}

public class KeyboardActivity extends Activity {
	
	private KeyboardDeck deck = new KeyboardDeck(null, null, null);
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onPause() {
		super.onPause();
		finish();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.deck_menu, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// if they clicked on settings, go to the settings activity
		if (item.getItemId() == R.id.deck_menu_settings) {
			KeyboardActivity activity = KeyboardActivity.this;
			
			Intent openSettingsActivity = new Intent(activity, SettingsActivity.class);
			openSettingsActivity.putExtra("deck_settings_file_path", deck.settings.getFilePath());
			activity.startActivity(openSettingsActivity);

			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}
}
