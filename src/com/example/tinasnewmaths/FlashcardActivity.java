package com.example.tinasnewmaths;

import libteachingtinadbmanager.*;

import java.io.File;
import java.util.ArrayList;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ScaleDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.text.Html;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;


/*class FlashcardDeck extends Deck {
	protected final int INDEX_QUESTION = 0;
	protected final int INDEX_ANSWER   = 1;

	FlashcardDeck(String db_filename, String db_config_filename) {
		super(db_filename, db_config_filename);
	}
	
	FlashcardDeck(File db_filename, File db_config_filename) {
		super( db_filename, db_config_filename );
	}
	
	FlashcardDeck( ArrayList<Card> d, File deck_file_path, DeckSettings s ) {
		super(d, deck_file_path, s);
	}
	
	public ArrayList<String> getQuestion() {
		ArrayList<String> list = CardDBTagManager.makeStringAList( getCurrentCard().getContent(INDEX_QUESTION) );
		return list;
	}
	
	@Override
	public ArrayList<String> getAnswer() {
		ArrayList<String> list = CardDBTagManager.makeStringAList( getCurrentCard().getContent(INDEX_ANSWER) );
		return list;
	}
}*/

class MyAudioButton extends ImageButton implements View.OnClickListener {
	protected String audio_file;
	protected MediaPlayer audio;
	protected boolean autostart;
	
	public MyAudioButton(Context context, String audio_file, boolean autostart) {
		super(context);
		
		setAudioFile( audio_file );
		
            audio = new MediaPlayer();
            try {
            	System.out.println( getAudioFile() );
            	System.out.println( getAudioFile() );
            	System.out.println( getAudioFile() );
            	System.out.println( getAudioFile() );
            	System.out.println( getAudioFile() );
            	System.out.println( getAudioFile() );
            	System.out.println( getAudioFile() );
            	audio.setDataSource( getAudioFile() );
            	audio.prepare();

            } catch (Exception e) {
                  // TODO Auto-generated catch block
                  e.printStackTrace();
            }
            
		setImageResource(R.drawable.ic_action_play);

            setOnClickListener(this);
            
            if( autostart ) {
            	audio.start();
            }
	}
	
	@Override
	public void onClick(View v) {
		//play media file.
		audio.start();
	}

	public void setAudioFile( String filepath ) {
		audio_file = filepath;
	}
	
	public String getAudioFile() {
		return audio_file;
	}
	
}

public class FlashcardActivity extends FlashcardBaseActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_flashcard);

		scroll_layout = (LinearLayout) findViewById(R.id.flashcard_scroll_layout);
		scroll_view   = (ScrollView)   findViewById(R.id.flashcard_scroll_view);

		bt_show_answer = (Button)      findViewById( R.id.bt_show_answer);
		bt_correct     = (ImageButton) findViewById( R.id.bt_correct );
		bt_incorrect   = (ImageButton) findViewById( R.id.bt_incorrect );
	
		String str_db_file = getIntent().getStringExtra("deck_file_path");
		db_file         = new File( str_db_file );
		db_config_file  = new File( getIntent().getStringExtra("deck_settings_file_path") );
		db_media_folder = new File( str_db_file.replace(".txt", "") );
		
		deck = new FlashcardDeck(db_file, db_config_file);
		

		bt_show_answer.setVisibility(View.VISIBLE);
		bt_correct.setVisibility  (View.GONE);
		bt_incorrect.setVisibility(View.GONE);

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
}