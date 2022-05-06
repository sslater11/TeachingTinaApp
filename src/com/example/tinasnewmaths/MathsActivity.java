/* © Copyright 2022, Simon Slater

This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, version 2 of the License.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package com.example.tinasnewmaths;

import libteachingtinadbmanager.*;

import java.io.File;
import java.util.ArrayList;

import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

class MathsDeck extends Deck {
	protected final int INDEX_NUM_1 = 0;
	protected final int INDEX_OP    = 1;
	protected final int INDEX_NUM_2 = 2;
	MathsDeck(String db_filename, String db_config_filename) {
		super(db_filename, db_config_filename);
	}
	MathsDeck( ArrayList<Card> d, File deck_file_path, DeckSettings s ) {
		super(d, deck_file_path, s);
	}
	
	public int getNum1() {
		return  Integer.valueOf( getCurrentCard().getContent(INDEX_NUM_1) );
	}
	
	public String getOperator() {
		return                   getCurrentCard().getContent(INDEX_OP);
	}

	public int getNum2() {
		return  Integer.valueOf( getCurrentCard().getContent(INDEX_NUM_2) );
	}
	
	// junk method because we don't need it, but it needs to be implemented.
	public ArrayList<String> getQuestion() {
		return null;
	}
	public String getSum() {
		String num1 = "" + getNum1();
		String num2 = "" + getNum2();
		String op = "" + getOperator();
		return num1 + " " + op + " " + num2 + " = ";
	}
	
	/**
	 * Will return a NULL value if the sum tries to divide by 0.
	 * Will return a NULL value if the operator(+, -, / and * ) is wrong
	 */
	public ArrayList<String> getAnswer() {
		int num1 = getNum1();
		int num2 = getNum2();
		String op = getOperator();
		
		String answer = "";
		 
		if      ( op.compareTo("+") == 0 ) {
			answer = "" + (num1 + num2);
		}
		else if ( op.compareTo("-") == 0 ) {
			answer = "" + (num1 - num2);
		}
		else if ( op.compareTo("*") == 0 || op.compareTo("x") == 0 ) {
			answer = "" + (num1 * num2);
		}
		else if ( op.compareTo("/") == 0 || op.compareTo("÷") == 0 ) {
			if (num2 == 0 ) {
				System.out.println("Error, trying to divide by 0. It's not possible on a pc.");
				return null;
			} else {
				answer = "" + (num1 / num2);
			}
		} else {
			System.out.println("Something went wrong. The op is wrong. op:'" + op +"'");
			System.out.println();
			System.exit(300);
			return null;
		}
		ArrayList<String> list = new ArrayList<String>();
		list.add(answer);
		return list;
	}

}


public class MathsActivity extends FlashcardBaseActivity {
	Button b0, b1, b2, b3, b4, b5, b6, b7, b8, b9, b_clear, b_enter;
	
	TextView tv_entered_numbers, tv_sum;
	
	String str_entered_numbers;
	
	int num1, num2;
	
	String operator;
	
	MathsDeck deck;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_maths);

		num1 = 0;
		num2 = 0;
		operator = "+";
		
		str_entered_numbers = "";
		b0 = (Button) findViewById(R.id.button0);
		b1 = (Button) findViewById(R.id.button1);
		b2 = (Button) findViewById(R.id.button2);
		b3 = (Button) findViewById(R.id.button3);
		b4 = (Button) findViewById(R.id.button4);
		b5 = (Button) findViewById(R.id.button5);
		b6 = (Button) findViewById(R.id.button6);
		b7 = (Button) findViewById(R.id.button7);
		b8 = (Button) findViewById(R.id.button8);
		b9 = (Button) findViewById(R.id.button9);
		b_enter = (Button) findViewById(R.id.button_enter);
		b_clear = (Button) findViewById(R.id.button_clear);
		
		image_tick_or_cross = (ImageView) findViewById(R.id.imageTickOrCrossMaths);
		image_tick_or_cross.setImageResource(R.drawable.answer_empty);

		tv_entered_numbers = (TextView) findViewById(R.id.textViewEnteredNumbers);
		tv_sum             = (TextView) findViewById(R.id.TextViewSum);

		System.out.println(Environment.getExternalStorageDirectory().getPath());
		System.out.println(Environment.getDataDirectory());

		
				
		
		// The test database file location on the emulator
		//String db_file = Environment.getExternalStorageDirectory().getPath() + "/TinasNewMaths_database_test.txt";
		// Use this line, f I get the
		// error: Trace: error opening trace file: No such file or directory (2)
		// It's something to do with Jelly Bean and the SD card in the emulator.
		//String db_file = "/mnt/sdcard/TinasNewMaths_database_test.txt";
//		String db_file = "TinasNewMaths_database_test.txt";
//		String db_config_file = "TinasNewMaths_database_test.conf";
		
		/*Intent intent = getIntent();
		Deck d = (Deck)intent.getSerializableExtra("Deck");*/
		String db_file        = getIntent().getStringExtra("deck_file_path");
		String db_config_file = getIntent().getStringExtra("deck_settings_file_path");
		
		deck = new MathsDeck(db_file, db_config_file);
		
		// Display the sum.
		tv_sum.setText(deck.getSum());
		
		b0.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				//tv_entered_numbers.setText("Yay");
				AddNumberPressed(0);
			}
		});
		
		b1.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				AddNumberPressed(1);
			}
		});

		b2.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				AddNumberPressed(2);
			}
		});

		b3.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				AddNumberPressed(3);
			}
		});

		b4.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				AddNumberPressed(4);
			}
		});

		b5.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				AddNumberPressed(5);
			}
		});

		b6.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				AddNumberPressed(6);
			}
		});

		b7.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				AddNumberPressed(7);
			}
		});

		b8.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				AddNumberPressed(8);
			}
		});

		b9.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				AddNumberPressed(9);
			}
		});

		b_clear.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				ClearPressed();
			}
		});

		b_enter.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				EnterPressed(v);
			}
		});

	}
	
	@Override
	protected void onPause() {
		super.onPause();
		finish();
	}

	public void AddNumberPressed(int num) {
		String new_str = Integer.toString(num);
		str_entered_numbers = str_entered_numbers + new_str;
		tv_entered_numbers.setText(str_entered_numbers);
	}
	
	public void ClearPressed(){
		str_entered_numbers = "";
		tv_entered_numbers.setText(str_entered_numbers);
	}
	
	public void EnterPressed(View v) {
		// Must check the sum
		// Then clear using ClearPressed();
		
		if( str_entered_numbers.compareTo("") == 0) {
			// just exit, if it's an empty string, the enter button was probably pressed by accident
			return;
		}
		
		if (str_entered_numbers.compareTo(deck.getAnswer().get(0)) == 0 ){
			// Show the tick to show they got it right.
			ImageThread img_t = new ImageThread(this, true);
			img_t.start();

			// They got it right.
			deck.nextQuestion(true, true);
			
			// Check if it's the last sum.
			if ( deck.isLearnt() == false) {
				ClearPressed();
				tv_sum.setText( deck.getSum() );
			} else {
				System.out.println("You finished the game.");
				System.out.println("You finished the game.");
				System.out.println("You finished the game.");
				
				CardDBManager.writeDB(deck.getFilePath(), deck.getLearntDeck(), deck.settings);
				Intent openFinishedActivity = new Intent("com.example.tinasnewmaths.FINISHEDACTIVITY");
				MathsActivity.this.startActivity(openFinishedActivity);
			}
			
		} else {
			// Show they got it wrong.
			ImageThread img_t = new ImageThread(this, false);
			img_t.start();
			ClearPressed();

			deck.nextQuestion(false, true);
		}
	}
}
