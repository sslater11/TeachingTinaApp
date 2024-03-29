/* © Copyright 2022, Simon Slater

This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, version 2 of the License.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package com.example.tinasnewmaths;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ScaleDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.BackgroundColorSpan;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.plattysoft.leonids.ParticleSystem;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

import libteachingtinadbmanager.CardDBTagManager;
import libteachingtinadbmanager.ReadingLessonCard;
import libteachingtinadbmanager.ReadingLessonDeck;
import libteachingtinadbmanager.SentenceAnalyzer;
import libteachingtinadbmanager.WordWithIndexes;
import libteachingtinadbmanager.sqlite_db.SQLiteReadingLessonHandler;

public class ReadingAndSpellingActivity extends FlashcardGroupActivity {
    protected String typed_string = "";
    ArrayList<Button> spelling_buttons;
    TextView txtTyped;
    TextView txtReadAlong;
    TextView txt_num_of_cards_to_reviews;
    TextView txt_num_of_new_cards;
    Button clear_user_input;
    Button btn_spelling_hint;
    boolean is_spelling_hint_enabled = false;
    boolean is_answer_shown = false;
    int MINIMUM_SPELLING_BUTTONS = 6;
    String alphabet = "abcdefghijklmnopqrstuvwxyz";
    ReadingLessonDeck deck;
    int num_of_reviews_today;
    int num_of_new_cards;
    private final float USER_TEXT_FONT_SIZE = 72f;
    private final float LETTER_BUTTONS_FONT_SIZE = 32f;
    private final Typeface FONT_TYPEFACE = Typeface.SANS_SERIF;
    private TableLayout audio_buttons_table_layout;
    private SpannableStringBuilder spannable_string;
    private ArrayList<ReadAlongTiming> read_along_timings;
    private long time_card_was_displayed_in_millis;

    private SQLiteDatabase reading_lesson_database;
    private SQLiteDatabase reading_lesson_stats_database;
    private final int db_version = 1;
    private final int stats_db_version = 1;
    String sqlite_db_file;
    String sqlite_stats_db_file;

    private MyUiUpdateHandler my_ui_handler; // Helps us update the UI from another thread.

    int MAX_DECK_SIZE = 5;

    @Override
    protected void onPause() {
        // Close the database, because I've set the super.onPause() method to actually kill the activity.
        reading_lesson_stats_database.close();
        super.onPause();
    }

    // We do call super.onCreate(), we call another super method to pass the activity id so that the correct one is loaded.
    @SuppressLint("MissingSuperCall")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        String str_db_file = getIntent().getStringExtra("deck_file_path");
        db_file         = new File( str_db_file );
        db_config_file  = new File( getIntent().getStringExtra("deck_settings_file_path") );
        db_media_folder = new File( str_db_file.replace(".txt", "") );

        setContentView(R.layout.activity_reading_and_spelling);

        my_ui_handler = new MyUiUpdateHandler();

        sqlite_db_file = db_file.getParent() + File.separator + "ReadingLessons.db";
        sqlite_stats_db_file = db_file.getParent() + File.separator + "ReadingLessonStats.db";

        ReadingLessonCardList all_cards;
        all_cards = loadCardsFromSQLiteDatabase();
        // Open  the statistics logging database
        ReadingLessonStatisticsDBHandler stats_db_handler = new ReadingLessonStatisticsDBHandler( this, sqlite_stats_db_file, stats_db_version );
        reading_lesson_stats_database = stats_db_handler.getWritableDatabase();

        // Study cards to review first
        deck = new ReadingLessonDeck( getCurrentReviewCards( all_cards ).getAllCards() );
        // If there were either no cards to review, or there's not many, learn some new ones.
        if( deck.size() <= 3 ) {
            deck = new ReadingLessonDeck( getCurrentNewCards(all_cards).getAllCards() );
        }

        // Create the spelling hint button.
        this.btn_spelling_hint = new Button(this);
        this.btn_spelling_hint.setText("?");
        this.btn_spelling_hint.setTextSize( LETTER_BUTTONS_FONT_SIZE );
        this.btn_spelling_hint.setTypeface( FONT_TYPEFACE );
        this.btn_spelling_hint.setOnClickListener( new ShowUserSpellingHint() );

        // Create the clear button.
        this.clear_user_input = new Button( this );
        this.clear_user_input.setText( "Clear" );
        this.clear_user_input.setTypeface( FONT_TYPEFACE );
        this.clear_user_input.setOnClickListener( new ClearUserInputListener() );

        // Create user input text view.
        this.txtTyped = new TextView(this );
        this.txtTyped.setTextSize( USER_TEXT_FONT_SIZE );
        this.txtTyped.setGravity( Gravity.CENTER_HORIZONTAL);

        // We are calling this after setting up the extra objects as we need some to be defined for the super.onCreate() method to run successfully.
        super.onCreate( savedInstanceState, R.layout.activity_reading_and_spelling);

        this.bt_show_answer.setText( "Check Answer" );
        // Count how many cards there are to review.
        this.txt_num_of_cards_to_reviews = (TextView) findViewById( R.id.txt_num_of_cards_to_review );
        updateCardsLeftToReview();
    }

    /**
     * Will log the current card in the stats table.
     * Will log the:
     *     time/date,
     *     time taken to answer,
     *     the box_num,
     *     card's id number,
     *     were they incorrect/correct,
     *     and if the spelling hint was used.
     * @param is_answer_correct
     */
    public void logStats( boolean is_answer_correct ) {
        int card_id = deck.getCurrentCard().getCardID();
        float box_num = deck.getCurrentCard().getBoxNum();
        long time_took_to_answer_in_millis = System.currentTimeMillis() - time_card_was_displayed_in_millis;

        // Convert the boolean values to ints.
        int int_is_answer_correct;
        int int_was_spelling_hint_used;
        if( is_answer_correct ) {
            int_is_answer_correct = 1;
        } else {
            int_is_answer_correct = 0;
        }

        if( is_spelling_hint_enabled ) {
            int_was_spelling_hint_used = 1;
        } else {
            int_was_spelling_hint_used = 0;
        }

        // Write to the database.
        ContentValues sql_insert_values = new ContentValues();
        sql_insert_values.putNull( ReadingLessonStatisticsDBHandler.STAT_ID ); // Make the primary key Null so that it will auto-increment.
        sql_insert_values.put( ReadingLessonStatisticsDBHandler.DATE_IN_MILLIS, System.currentTimeMillis() );
        sql_insert_values.put( ReadingLessonStatisticsDBHandler.CARD_ID, card_id );
        sql_insert_values.put( ReadingLessonStatisticsDBHandler.BOX_NUM, box_num );
        sql_insert_values.put( ReadingLessonStatisticsDBHandler.TIME_TO_ANSWER_IN_MILLIS, time_took_to_answer_in_millis );
        sql_insert_values.put( ReadingLessonStatisticsDBHandler.IS_ANSWER_CORRECT, int_is_answer_correct );
        sql_insert_values.put( ReadingLessonStatisticsDBHandler.WAS_SPELLING_HINT_USED, int_was_spelling_hint_used );

        reading_lesson_stats_database.insert(ReadingLessonStatisticsDBHandler.TABLE_NAME, null, sql_insert_values );
    }
    public void updateCardsLeftToReview() {
        this.txt_num_of_cards_to_reviews.setText( "Cards to review today: " + deck.countCardsLeftToStudy() );
    }

    public ReadingLessonCardList loadCardsFromSQLiteDatabase() {
        ReadingLessonCardList reading_lesson_cards = new ReadingLessonCardList();
        // Open the SQLite Database
        ReadingLessonDBHandler db_helper = new ReadingLessonDBHandler( this, sqlite_db_file, db_version );
        reading_lesson_database = db_helper.getReadableDatabase();
        Cursor cursor = reading_lesson_database.rawQuery("SELECT * FROM " + SQLiteReadingLessonHandler.TABLE_NAME + " ORDER BY " + SQLiteReadingLessonHandler.CARD_ID + " ASC;", new String[]{} );

        if( cursor.getCount() > 0 ) {
            // We have cards to learn.
            cursor.moveToFirst();
            while( ! cursor.isAfterLast() ) {
                int card_id                    = cursor.getInt(    cursor.getColumnIndex(SQLiteReadingLessonHandler.CARD_ID) );
                long date_in_millis            = cursor.getLong(   cursor.getColumnIndex(SQLiteReadingLessonHandler.DATE_IN_MILLIS) );
                float box_num                  = cursor.getFloat(  cursor.getColumnIndex(SQLiteReadingLessonHandler.BOX_NUM) );
                int reading_lesson_level       = cursor.getInt(    cursor.getColumnIndex(SQLiteReadingLessonHandler.READING_LESSON_LEVEL) );
                String sound_type              = cursor.getString( cursor.getColumnIndex(SQLiteReadingLessonHandler.SOUND_TYPE) );
                String sound_word_or_sentence  = cursor.getString( cursor.getColumnIndex(SQLiteReadingLessonHandler.SOUND_WORD_OR_SENTENCE) );
                int id_of_linked_card          = cursor.getInt(    cursor.getColumnIndex(SQLiteReadingLessonHandler.ID_OF_LINKED_CARD) );
                int is_spelling_mode_int       = cursor.getInt(    cursor.getColumnIndex(SQLiteReadingLessonHandler.IS_SPELLING_MODE) );
                boolean is_spelling_mode;
                if( is_spelling_mode_int == 0 ) {
                    is_spelling_mode = false;
                } else {
                    is_spelling_mode = true;
                }

                String card_text               = cursor.getString( cursor.getColumnIndex(SQLiteReadingLessonHandler.CARD_TEXT) );
                String card_images             = cursor.getString( cursor.getColumnIndex(SQLiteReadingLessonHandler.CARD_IMAGES) );
                String card_audio              = cursor.getString( cursor.getColumnIndex(SQLiteReadingLessonHandler.CARD_AUDIO) );
                String card_read_along_timings = cursor.getString( cursor.getColumnIndex(SQLiteReadingLessonHandler.CARD_READ_ALONG_TIMINGS) );
                cursor.moveToNext();

                // Create card.
                ReadingLessonCard card = new ReadingLessonCard(
                        card_id,
                        date_in_millis,
                        box_num,
                        reading_lesson_level,
                        sound_type,
                        sound_word_or_sentence,
                        id_of_linked_card,
                        is_spelling_mode,

                        card_text,
                        card_images,
                        card_audio,
                        card_read_along_timings
                );

                reading_lesson_cards.add( card );
            }
        } else {
            // No cards found.
            System.out.println( "No cards found!" );
        }

        cursor.close();
        reading_lesson_database.close();

        return reading_lesson_cards;
    }

    /**
     * Will look through the list and get the next new cards to learn.
     *
     * WARNING: Must take a list that is ordered by the CARD_ID in ascending order.
     * @param deck
     * @return
     */
    public ReadingLessonCardList getCurrentNewCards( ReadingLessonCardList deck) {

        // Scan through the deck.
        ReadingLessonCardList results_deck = new ReadingLessonCardList();
        for( int i = 0; i < deck.size(); i++ ) {

            // Exit this loop if we've added too many cards already.
            if( results_deck.size() >= MAX_DECK_SIZE ) {
                break;
            }

            // The rule for adding a new card.
            // If a card's study date is == -1, then we can add it.
            // If it is a sound and it has a linked card, add that linked card every time.
            // If it is a word and it has a linked card, only add the linked card if the card date == -1.
            // We don't want spelling and reading cards to be linked forever, only on the first showing.

            ReadingLessonCard card = deck.getCardByIndex( i );
            // Check that we've not already added this card to the list.
            if( ! results_deck.isCardInList( card.getCardID() ) ) {
                // New cards have a timestamp of -1.
                if ( card.getDateInMillis() == -1 ) {
                    // Add our card to study it.
                    results_deck.add( card );

                    // For every addition of a card, check if it's got a linked card, and see if we have added the linked card to the deck already.
                    if ( card.isASound() ) {
                        // If the linked card isn't in the results_deck already, add it.
                        ReadingLessonCard linked_card = deck.getCardByID( card.getIdOfLinkedCard() );
                        if ( linked_card != null && (! results_deck.isCardInList(linked_card.getCardID())) ) {
                            results_deck.add( linked_card );
                        }
                    }
                    else if( card.isAWord() ) {
                        // If a card is a new word, add both reading and spelling cards to be learnt together.

                        // If the linked card isn't in the results_deck already, add it.
                        ReadingLessonCard linked_card = deck.getCardByID( card.getIdOfLinkedCard() );
                        if ( linked_card != null && (! results_deck.isCardInList(linked_card.getCardID())) ) {
                            results_deck.add( linked_card );
                        }
                    }
                }
            }
        }

        return results_deck;
    }

    public ReadingLessonCardList getCurrentReviewCards( ReadingLessonCardList deck ) {
        // Scan through the deck.
        ReadingLessonCardList results_deck = new ReadingLessonCardList();
        for( int i = 0; i < deck.size(); i++ ) {

            // Exit this loop if we've added too many cards already.
            if( results_deck.size() >= MAX_DECK_SIZE ) {
                break;
            }

            // New cards have a timestamp of -1.
            // So skip adding those.
            ReadingLessonCard card = deck.getCardByIndex( i );
            if ( ( card.getDateInMillis() != -1) && (card.isReviewNeeded()) ) {
                // Check that we've not already added this card to the list.
                if ( !results_deck.isCardInList( card.getCardID() ) ) {
                    if ( card.isReviewNeeded() ) {
                        // Add our card to study it.
                        results_deck.add( card );

                        // For every addition of a card, check if it's got a linked card, and see if we have added the linked card to the deck already.
                        if ( deck.getCardByIndex(i).isASound() ) {
                            // If the linked card isn't in the results_deck already, add it.
                            ReadingLessonCard linked_card = deck.getCardByID( card.getIdOfLinkedCard() );

                            if ( linked_card != null && (! results_deck.isCardInList(linked_card.getCardID())) ) {
                                results_deck.add( linked_card );
                            }
                        } else if ( deck.getCardByIndex(i).isAWord() ) {
                            // If a card is a newly learnt word, add both reading and spelling cards to be learnt together.
                            // If the card is a review, don't add it, just review reading and spelling separately.
                            // We'll call it a review if the box_num is greater than 2.
                            // We'll call it a newly learnt word if the box_num is less than 2.

                            if ( deck.getCardByIndex(i).getBoxNum() < 2 ) {
                                // If the linked card isn't in the results_deck already, add it.
                                ReadingLessonCard linked_card = deck.getCardByID( card.getIdOfLinkedCard() );
                                if ( linked_card != null && (! results_deck.isCardInList(linked_card.getCardID())) ) {
                                    results_deck.add(linked_card);
                                }
                            }
                        }
                    }
                }
            }
        }

        return results_deck;
    }

    public void saveCardsToSQLiteDatabase( ArrayList<ReadingLessonCard> reading_lesson_cards ) {
        ReadingLessonDBHandler db_helper = new ReadingLessonDBHandler( this, sqlite_db_file, db_version );
        reading_lesson_database = db_helper.getWritableDatabase();

        long date_in_millis = new Date().getTime();
        for( int i = 0; i < reading_lesson_cards.size(); i++ ) {
            reading_lesson_cards.get( i ).update( date_in_millis );
            float box_num = reading_lesson_cards.get( i ).getBoxNum();

            ContentValues content_values = new ContentValues();
            content_values.put( "date_in_millis", date_in_millis );
            content_values.put( "box_num", box_num );

            int card_id = reading_lesson_cards.get( i ).getCardID();
            String[] str_card_id = new String[]{ card_id + "" };
            long result = reading_lesson_database.update( SQLiteReadingLessonHandler.TABLE_NAME, content_values, SQLiteReadingLessonHandler.CARD_ID + "=?",  str_card_id );
            if( result == -1 ) {
                System.out.println( "Failed to update sqlite database line." );
            } else {
                System.out.println( "Successfully updated database." );
            }
        }
    }

    public void AddContent(ArrayList<String> list) {
        // Do nothing.
        // This There's no need for this method as we show the card's content differently for each question and answer for each mode(reading mode, spelling mode, sentence mode).
    }


    public void clearUserInput() {
        setTypedString("");
        txtTyped.setText("");
    }

    @Override
    public void DisplayQuestion() {
        this.is_answer_shown = false;
        font_size = DEFAULT_FONT_SIZE * 2;

        if( deck.getCurrentCard().isASentence() ) {
            String sentence = deck.getCurrentCard().getCardText();

            // Load the timings for highlighting the words in time with the audio.

            // Split the string by tabs as that's what's seperating each timing.
            String timings = deck.getCurrentCard().getCardReadAlongTimings();
            String[] str_timings_list = timings.split("\t");

            // Convert the string into an ArrayList of Timing objects.
            // We will then use this list to make ReadAlongTiming objects.
            ArrayList<Timings> timings_list = new ArrayList<Timings>();
            for( int i = 0; i < (str_timings_list.length /2); i++ ) {
                long start_position = Long.parseLong( str_timings_list[ i*2  ] );
                long end_position   = Long.parseLong( str_timings_list[(i*2)+1] );

                Timings t = new Timings( start_position, end_position );
                timings_list.add( t );
            }


            // Give each word it's own onClickListener so the user can hear the word by it's self when they click it.
            // and add the word timings to the read_along_timings ArrayList.
            ArrayList<WordWithIndexes> words_list_with_indexes = SentenceAnalyzer.getWordsListWithIndexes( sentence );
            spannable_string = new SpannableStringBuilder();
            spannable_string.append(sentence);
            read_along_timings = new ArrayList<ReadAlongTiming>();
            for( int i = 0; i < words_list_with_indexes.size(); i++ ) {
                String audio_name = words_list_with_indexes.get(i).getWordWithIgnoredCharactersRemoved().toLowerCase();
                String audio_file_path = db_media_folder + File.separator + "words" + File.separator + audio_name + ".wav";

                int start_index = words_list_with_indexes.get( i ).getStartingIndex();
                int end_index   = words_list_with_indexes.get( i ).getEndingIndex();

                // Load the timings for highlighting the words in time with the audio.
                long start_in_millis = timings_list.get(i).start_in_millis;
                long end_in_millis   = timings_list.get(i).end_in_millis;
                ReadAlongTiming highlight_timing = new ReadAlongTiming( start_in_millis, end_in_millis, words_list_with_indexes.get(i) );
                read_along_timings.add( highlight_timing );

                // Set the on click action to play the audio file.
                MyClickableSpan clickable_span = new MyClickableSpan( sentence, audio_file_path, highlight_timing, words_list_with_indexes.get(i) );

                spannable_string.setSpan( clickable_span, start_index, end_index, 0 );
            }

            txtReadAlong = new TextView(this);

            // This line is also needed for making the text clickable.
            txtReadAlong.setMovementMethod(LinkMovementMethod.getInstance());

            txtReadAlong.setText( spannable_string , TextView.BufferType.SPANNABLE);
            txtReadAlong.setTypeface(FONT_TYPEFACE);
            txtReadAlong.setTextSize(USER_TEXT_FONT_SIZE);
            txtReadAlong.setGravity( Gravity.CENTER_HORIZONTAL);
            scroll_layout.addView(txtReadAlong);
        }
        else if( deck.getCurrentCard().isReadingMode() ) {
            // Display just the word on the screen.
            String text = deck.getCurrentCard().getCardText();
            spannable_string = new SpannableStringBuilder();
            spannable_string.append( text );

            txtReadAlong = new TextView(this);
            txtReadAlong.setText( spannable_string , TextView.BufferType.SPANNABLE);
            txtReadAlong.setTypeface(FONT_TYPEFACE);
            txtReadAlong.setTextSize(USER_TEXT_FONT_SIZE);
            txtReadAlong.setGravity( Gravity.CENTER_HORIZONTAL);
            scroll_layout.addView(txtReadAlong);
        }
        else if( deck.getCurrentCard().isSpellingMode() ) {
            // Spelling mode.
            // It's spelling mode, so we need to completely change the layout to spelling mode.

            // Remove these from their previous views, if they have them, because we want to add them to the new view later.
            if ( clear_user_input.getParent() != null ) {
                ((ViewGroup) clear_user_input.getParent()).removeView(clear_user_input);
            }
            if ( btn_spelling_hint.getParent() != null ) {
                ((ViewGroup) btn_spelling_hint.getParent()).removeView(btn_spelling_hint);
            }
            if ( txtTyped.getParent() != null ) {
                ((ViewGroup) txtTyped.getParent()).removeView(txtTyped);
            }

            spelling_buttons = new ArrayList<Button>();

            // Get every letter in the word and make a button for it.
            String word = deck.getCurrentCard().getCardText();
            for( int i = 0; i < word.length(); i++ ) {
                char letter = word.charAt(i);

                if( ! isLetterInButtonArray(spelling_buttons, letter) ){
                    SpellingButton b = new SpellingButton( this, letter );
                    b.setOnClickListener(new LetterListener( letter ) );
                    spelling_buttons.add( b );
                }
            }

            int max_column = 5;
            int num_random_letters_buttons_to_add = 0;
            if( spelling_buttons.size() < max_column ) {
                // If the word is less than 6, add more letters.
                num_random_letters_buttons_to_add = max_column - spelling_buttons.size();
            } else {
                // The word is bigger than or equal to 6 letters.
                // so add enough buttons to fit on another row.
                num_random_letters_buttons_to_add = max_column - (spelling_buttons.size() % max_column);
            }

            // Add a random letter button, but only if it's not in the current word.
            String temp_alphabet = alphabet;
            for( int i = 0; ( i < num_random_letters_buttons_to_add ); i++ ) {

                int letter_index = 0;

                boolean keep_going = true;
                while( keep_going ) {
                    if( temp_alphabet.length() != 0 ) {
                        // Get a random letter by it's index.
                        letter_index = (int)((Math.random() * 100) % temp_alphabet.length());
                        if ( isLetterInButtonArray(spelling_buttons, temp_alphabet.charAt(letter_index)) )
                        {
                            // Remove the unwanted letter
                            temp_alphabet = temp_alphabet.substring(0, letter_index) + temp_alphabet.substring(letter_index+1, temp_alphabet.length());
                        }
                        else
                        {
                            // We have found a letter, so create a button and stop the while loop.
                            keep_going = false;
                            Button letter_button = new SpellingButton(this, temp_alphabet.charAt(letter_index));
                            letter_button.setOnClickListener(new LetterListener(temp_alphabet.charAt(letter_index)));
                            spelling_buttons.add(letter_button);
                        }
                    } else {
                        keep_going = false;
                    }
                }
            }

            Collections.shuffle( spelling_buttons );


            //// Create the grid for the letter buttons.
            int row = (int) Math.ceil( (double)spelling_buttons.size() / (double)max_column );
            row += 1; // Increment so we have an extra row for the clear button to go in.
            TableLayout letters_table_layout = new TableLayout( this );

            // Finally add the buttons.
            TableRow tr = new TableRow(this );
            for( int i = 0; i < spelling_buttons.size(); i++ ) {
                spelling_buttons.get(i).setGravity( Gravity.CENTER );
                tr.addView( spelling_buttons.get(i) );
                if( (i != 0) && ((i+1) % max_column == 0) ) {
                    letters_table_layout.addView( tr );
                    tr = new TableRow( this );
                }
            }
            letters_table_layout.addView( tr );

            // Add the clear button to the last row of the grid, and make it span all the columns
            //letters_table_layout.setStretchAllColumns(false);

            // Setup the parameters to make the clear button span the columns
            TableRow.LayoutParams row_layout_parameters = new TableRow.LayoutParams();
            row_layout_parameters.span = max_column;

            tr = new TableRow( this );
            tr.addView( clear_user_input, row_layout_parameters );
            letters_table_layout.addView( tr );


            // Center the grid into the middle of the scroll view.
            LinearLayout.LayoutParams linear_layout_parameters = new LinearLayout.LayoutParams( LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            linear_layout_parameters.gravity = Gravity.CENTER;
            letters_table_layout.setLayoutParams( linear_layout_parameters );


            // Add the other components
            DisplayImagesAndAudio();
            // Setup the parameters to make the hint button span the columns of the audio buttons
            TableRow.LayoutParams hint_row_layout_parameters = new TableRow.LayoutParams();
            hint_row_layout_parameters.span = audioArr.size();

            // Add the hint button to the table row.
            TableRow hint_tr = new TableRow( this );
            hint_tr.addView( btn_spelling_hint, hint_row_layout_parameters );
            this.audio_buttons_table_layout.addView( hint_tr );


            // Add all the components in the desired order.
            scroll_layout.setGravity( Gravity.CENTER_HORIZONTAL );
            scroll_layout.setGravity( Gravity.FILL_HORIZONTAL );
            scroll_layout.addView( this.txtTyped );
            scroll_layout.addView( letters_table_layout );
        }
    }

    /*
    public void DisableButtons() {
        super.DisableButtons();

        if( spelling_buttons != null ) {
            for (int i = 0; i < spelling_buttons.size(); i++) {
                spelling_buttons.get(i).setEnabled(false);
            }
        }
    }
    public void EnableButtons() {
        super.EnableButtons();

        if( spelling_buttons != null ) {
            for (int i = 0; i < spelling_buttons.size(); i++) {
                spelling_buttons.get(i).setEnabled(true);
            }
        }
    }
*/
    public void DisplayImagesAndAudio() {
        // Make an array of images for each image in the flashcard.
        ArrayList<String> image_list = deck.getCurrentCard().getCardImagesAsArrayList();
        for (int i = 0; i < image_list.size(); i++) {
            // Make an image box and add the image.
            imgArr.add(new ImageView(this));

            String img_filename = CardDBTagManager.getImageFilename(image_list.get(i));

            // Get the image file as a bitmap, and set it to the image view.
            Bitmap bitmap = BitmapFactory.decodeFile(db_media_folder + "/" + img_filename);
            imgArr.get(i).setImageBitmap(bitmap);

            // Make the image view fit the image. On larger images that have been shrunk, they contain whitespace for some reason.
            // This gets rid of the whitespace.
            imgArr.get(i).setAdjustViewBounds(true);

            // Pad the images, so when they are displayed together, there's a small gap.
            imgArr.get(i).setPadding(10, 10,10,10);
        }


        // Make an array of audio buttons for each audio file in the flashcard.
        ArrayList<String> audio_list = deck.getCurrentCard().getCardAudioAsArrayList();

        String card_text = deck.getCurrentCard().getCardText();
        if( deck.getCurrentCard().isASound() ) {
            boolean is_highlighting_enabled = true;

            String audio_filepath = CardDBTagManager.getAudioFilename( audio_list.get(0) );
            audio_filepath = db_media_folder + File.separator + audio_filepath;

            ThreadHighlightLetters highlight_letters = new ThreadHighlightLetters( audio_filepath, deck.getCurrentCard().getCardText() );
            DisableButtons();
            highlight_letters.start();
            audioArr.add(new MyAudioButtonWithWordHighlighting(this, card_text, audio_filepath, false, is_highlighting_enabled));
        }
        else if( deck.getCurrentCard().isASentence() ) {
            boolean is_highlighting_enabled = true;

            // Make an audio button and add the file's path.
            String audio_filepath = CardDBTagManager.getAudioFilename(audio_list.get(0));
            audio_filepath = db_media_folder + File.separator + audio_filepath;

            audioArr.add(new MyAudioButtonWithWordHighlighting(this, card_text, audio_filepath, true, is_highlighting_enabled));
        }
        else {
            for (int i = 0; i < audio_list.size(); i++) {
                boolean is_highlighting_enabled = true;

                // Make an audio button and add the file's path.
                String audio_filepath = CardDBTagManager.getAudioFilename(audio_list.get(i));
                audio_filepath = db_media_folder + File.separator + audio_filepath;

                audioArr.add(new MyAudioButtonWithWordHighlighting(this, card_text, audio_filepath, true, is_highlighting_enabled));
            }
        }

        // Create the table for the audio buttons to be in the same row.
        this.audio_buttons_table_layout = new TableLayout( this );

        // This is used to center the table layouts on the screen.
        // The table layout is used for the audio buttons.
        LinearLayout.LayoutParams linear_layout_parameters = new LinearLayout.LayoutParams( LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        linear_layout_parameters.gravity = Gravity.CENTER;

        // Finally add the buttons.
        TableRow audio_tr = new TableRow(this );
        for( int i = 0; i < audioArr.size(); i++ ) {
            //audioArr.get(i).setGravity( Gravity.CENTER );
            audio_tr.addView( audioArr.get(i) );
        }
        this.audio_buttons_table_layout.addView( audio_tr );

        // Add the images to the images layout.
        LinearLayout images_layout = new LinearLayout(this );
        for( int i = 0; i < imgArr.size(); i++ ) {
            images_layout.addView( imgArr.get(i) );
        }

        // Center the images to the middle of the screen.
        images_layout.setLayoutParams( linear_layout_parameters );
        images_layout.setGravity(Gravity.CENTER);

        // Center the grid into the middle of the screen.
        this.audio_buttons_table_layout.setLayoutParams( linear_layout_parameters );

        // Add the layouts to scroll layout.
        scroll_layout.addView( images_layout );
        scroll_layout.addView( this.audio_buttons_table_layout );
    }

    //@Override
    public void NextQuestion() {
        // Check if we've finished the deck.
        if (deck == null) {
            System.out.println("nulled it here");
        }
        if (deck.isLearnt() == false) {
            // It's not the last card, so continue.
            ResetQuestionField();
            this.time_card_was_displayed_in_millis = System.currentTimeMillis();

            DisplayQuestion();
            EnableButtons();
        } else {
            // The deck is empty.
            // Write the output and go to the finish screen.
            System.out.println("You finished studying!.");

            saveCardsToSQLiteDatabase( deck.getLearntDeck() );

            reading_lesson_stats_database.close();

            Intent openFinishedActivity = new Intent("com.example.tinasnewmaths.FINISHEDACTIVITY");
            ReadingAndSpellingActivity.this.startActivity(openFinishedActivity);
        }
    }
    public void ShowAnswerPressed() {
        this.is_answer_shown = true;
        if( deck.getCurrentCard().isReadingMode() ||
            deck.getCurrentCard().isASentence() ) {
            // Horizontal line - stretch horizontally.
            // Get the image and stretch it to the layout's width
            Drawable dr = getResources().getDrawable(R.drawable.horizontal_line);
            dr = new ScaleDrawable(dr, 0, scroll_layout.getWidth(), -1).getDrawable();
            ImageView horizontal_line = new ImageView(this );
            horizontal_line.setImageDrawable( dr );
            horizontal_line.setScaleType(ImageView.ScaleType.FIT_XY);
            scroll_layout.addView( horizontal_line );

            // Draw the image and audio.
            DisplayImagesAndAudio();

            // Hide the "Check Answer" button, and show the tick and cross buttons.
            bt_show_answer.setVisibility(View.GONE);
            bt_correct.setVisibility(View.VISIBLE);
            bt_incorrect.setVisibility(View.VISIBLE);
        }
        else if( deck.getCurrentCard().isIsSpellingMode() ) {
            String user_input = getTypedString().toLowerCase();
            String word = deck.getCurrentCard().getCardText();

            // Do nothing if they haven't even typed anything yet.
            if (user_input.length() == 0) {
                return;
            }
            if (user_input.compareToIgnoreCase(word) == 0) {
                // Answer is correct

                // Explosion of ticks celebration.
                // I love this, major thanks to the Leonids library!
                AnimationCelebration();

                if (is_spelling_hint_enabled) {
                    // Mark it as wrong, so they have to attempt it again without the spelling hint.
                    logStats( false );
                    deck.nextQuestion(false, false);
                } else {
                    // Since they didn't see a hint, mark it as correct.
                    logStats( true );
                    deck.nextQuestion(true, false);
                }

                // Reset for the next card.
                is_spelling_hint_enabled = false;
                clearUserInput();

            } else {
                // Answer is wrong.

                // Red crosses shooting out of the top of the screen.
                // I love this, major thanks to the Leonids library!
                AnimationFailed();

                // Mark the card as wrong and stay on the current card.
                // Set the hint to true so they can see the word.
                logStats( false );
                deck.nextQuestion(false, true);
                is_spelling_hint_enabled = true;
                clearUserInput();
                displayUserInput();
            }
            NextQuestion();
        }
    }


    public void AnimationCelebration() {
        // Randomly select an animation
        final int NUM_OF_EFFECTS = 6;
        int rand_num = ( (int)(Math.random() * 10) ) % NUM_OF_EFFECTS;
        switch( rand_num ) {
            case 0:
                AnimationExplodingTicks();
                break;
            case 1:
                AnimationExplodingStars();
                break;
            case 2:
                AnimationExplodingTicksBottom();
                break;
            case 3:
                AnimationExplodingStarsBottom();
                break;
            case 4:
                AnimationTicksFloatingUpwardsGravity();
                break;
            case 5:
                AnimationSpinningStarsFloatingUpwardsGravity();
                break;
        }
    }

    public void AnimationFailed() {
        // Randomly select an animation
        final int NUM_OF_EFFECTS = 2;
        int rand_num = ( (int)(Math.random() * 10) ) % NUM_OF_EFFECTS;
        switch( rand_num ) {
            case 0:
                AnimationCrossesFalling();
                break;
            case 1:
                AnimationCrossesFallingShort();
                break;
        }
    }

    public void AnimationExplodingStars() {
        int num_of_particles = 10000;
        int time_to_live = 1200;
        new ParticleSystem(this, num_of_particles, R.drawable.star, time_to_live)
                .setSpeedRange(0.20f, 1.0f)
                .setScaleRange(0.10f, 1f)
                //.oneShot(bt_show_answer, num_of_particles);
                .emit(findViewById(R.id.flashcard_group_scroll_view), 75, 1000);
    }

    public void AnimationExplodingTicks() {
        int num_of_particles = 10000;
        int time_to_live = 1200;
        new ParticleSystem(this, num_of_particles, R.drawable.answer_tick_small, time_to_live)
                .setSpeedRange(0.10f, 1.0f)
                .setScaleRange(0.10f, 1f)
                .emit(findViewById(R.id.flashcard_group_scroll_view), 75, 1000);
    }

    public void AnimationExplodingTicksBottom() {
        int num_of_particles = 5000;
        int time_to_live = 5500;
        new ParticleSystem(this, num_of_particles, R.drawable.answer_tick_small, time_to_live)
                .setSpeedRange(0.20f, 1.5f)
                .setScaleRange(0.10f, 1f)
                .oneShot(bt_show_answer, num_of_particles);
    }

    public void AnimationExplodingStarsBottom() {
        int num_of_particles = 4000;
        int time_to_live = 2500;
        new ParticleSystem(this, num_of_particles, R.drawable.star, time_to_live)
                .setSpeedRange(0.20f, 0.5f)
                .setScaleRange(0.30f, 1f)
                .setFadeOut(500)
                .setRotationSpeedRange(200, 400)
                .oneShot(bt_show_answer, num_of_particles);
    }

    public void AnimationTicksFloatingUpwardsGravity() {
        new ParticleSystem(this, 1000, R.drawable.answer_tick_small, 1000)
                .setAcceleration(0.003f, 270)
                .setScaleRange(0.10f, 1f)
                .setSpeedByComponentsRange(0f, 0f, 0.0001f, 0.001f)
                .setFadeOut(200, new AccelerateInterpolator())
                .emitWithGravity(findViewById(R.id.flashcard_group_scroll_view), Gravity.NO_GRAVITY, 60, 2000);

    }
    public void AnimationSpinningStarsFloatingUpwardsGravity() {
        new ParticleSystem(this, 1000, R.drawable.star, 1000)
                .setAcceleration(0.003f, 270)
                .setScaleRange(0.10f, 1f)
                .setSpeedByComponentsRange(0f, 0f, 0.0001f, 0.001f)
                .setRotationSpeedRange(300, 600)
                .emitWithGravity(findViewById(R.id.flashcard_group_scroll_view), Gravity.NO_GRAVITY, 60, 2000);
    }

    public void AnimationCrossesFalling() {
        ParticleSystem particle = new ParticleSystem(this, 200, R.drawable.answer_cross_medium, 2500);
        particle.setSpeedModuleAndAngleRange(0.3f, 0.8f, 35, 145);
        particle.setRotationSpeed(180);
        particle.emit(findViewById(R.id.top_emitter), 30, 1000);
    }

    public void AnimationCrossesFallingShort() {
        ParticleSystem particle = new ParticleSystem(this, 200, R.drawable.answer_cross_medium, 2500);
        particle.setSpeedModuleAndAngleRange(0.3f, 0.8f, 35, 145);
        particle.setRotationSpeed(180);
        particle.emit(findViewById(R.id.top_emitter), 6, 500);
    }


    @Override
    public void CorrectPressed() {
        // They got it right.
        // Stop the user from tapping the buttons whilst it's doing stuff.
        DisableButtons();
        clearUserInput();
        logStats( true );
        deck.nextQuestion(true, false);
        NextQuestion();
        updateCardsLeftToReview();
        AnimationCelebration();
    }

    @Override
    public void IncorrectPressed() {
        // They got it wrong.
        // Stop the user from tapping the buttons whilst it's doing stuff.
        DisableButtons();
        clearUserInput();
        logStats( false );
        deck.nextQuestion(false, false);
        NextQuestion();
        updateCardsLeftToReview();
        AnimationFailed();
    }

    public boolean isLetterInButtonArray( ArrayList<Button> letter_buttons, char letter) {
        for( Button b : letter_buttons ) {
            if (b.getText().toString().compareToIgnoreCase("" + letter) == 0) {
                return true;
            }
        }
        return false;
    }

    class SpellingButton extends Button {
        String letter;

        SpellingButton(Context context, String letter ) {
            super( context );
            this.letter = letter;
            this.setTextSize( LETTER_BUTTONS_FONT_SIZE );
            this.setText( letter.toLowerCase() );
            // this font seems to have nice lowercase letters and i and l are easy to distinguish.
            this.setTypeface( FONT_TYPEFACE );
        }
        SpellingButton(Context context, char letter ) {
            this( context, "" + letter );
        }

    }
    public String getTypedString() {
        return this.typed_string;
    }
    public void setTypedString( String str ) {
        this.typed_string = str;
    }

    /*
    This will turn the user input font to black with a transparent green highlight.
    If the answer hint is to be shown, it'll show it as a light grey font.
    If they type anything wrong, the font will be bright red
   */
    public void displayUserInput() {
        String user_input = getTypedString();
        String word = deck.getCurrentCard().getCardText();
        SpannableStringBuilder builder = new SpannableStringBuilder();

        boolean keep_going = true;
        boolean are_letters_correct = true;
        int index = 0;
        while( keep_going ) {
            if( index > user_input.length() && index > word.length() ) {
                keep_going = false;
                break;
            }
            else if( index > user_input.length() ) {
                if( is_spelling_hint_enabled ) {
                    // Display the rest of the word as a hint
                    // Make the font colour light grey
                    word = word.substring( user_input.length(), word.length() );
                    SpannableString str2 = new SpannableString( word );
                    str2.setSpan(new ForegroundColorSpan(Color.LTGRAY), 0, word.length(), 0);
                    builder.append(str2);

                    keep_going = false;
                    break;
                }
            } else {
                // Check if the letters are correct or not.
                // Highlight them with a green background if they are,
                // If not, make their font red.
                if( (index < user_input.length()) && (index < word.length()) ) {
                    char ch1 = word.toLowerCase().charAt(index);
                    char ch2 = user_input.toLowerCase().charAt(index);
                    if ( (ch1 == ch2) && are_letters_correct ) {
                        // Highlight the font foreground a light transparent green.
                        // Make the font colour black.
                        int background_color = Color.parseColor("#3300ff00");
                        SpannableString spannable_str = new SpannableString( "" + user_input.charAt(index) );
                        spannable_str.setSpan(new ForegroundColorSpan(Color.BLACK), 0, 1, 0);
                        spannable_str.setSpan(new BackgroundColorSpan(background_color), 0, 1, 0);
                        builder.append(spannable_str);
                    } else {
                        // Make the font colour red.
                        are_letters_correct = false;
                        SpannableString spannable_str = new SpannableString( "" + user_input.charAt(index) );
                        spannable_str.setSpan(new ForegroundColorSpan(Color.RED), 0, 1, 0);
                        builder.append(spannable_str);
                    }
                } else if( index < user_input.length() ) {
                    // Make the font colour red.
                    are_letters_correct = false;
                    SpannableString spannable_str = new SpannableString( "" + user_input.charAt(index) );
                    spannable_str.setSpan(new ForegroundColorSpan(Color.RED), 0, 1, 0);
                    builder.append(spannable_str);
                }
            }
            index++;
        }
        txtTyped.setText( builder, TextView.BufferType.SPANNABLE);
    }

    class LetterListener implements View.OnClickListener {
        String letter;
        LetterListener( char letter ) {
            this.letter = "" + letter;
        }
        LetterListener( String letter ) {
            this.letter = letter;
        }
        @Override
        public void onClick(View v) {
            setTypedString( getTypedString() + letter );

            displayUserInput();

        }

        public boolean isLetterCorrect() {
            String typed_word = getTypedString();
            String answer_word = deck.getCurrentCard().getCardText();

            System.out.println( typed_word );
            // If the typed word is smaller, cut the answer word to size
            if( typed_word.length() < answer_word.length() ) {
                answer_word = answer_word.substring(0, typed_word.length() );
                System.out.println("tester: " + answer_word);
            }

            // Compare the two words.
            if( typed_word.compareTo( answer_word ) == 0 ) {
                return true;
            } else {
                return false;
            }
        }
    }

    class ClearUserInputListener implements View.OnClickListener {
        public void onClick(View v) {
            clearUserInput();
            displayUserInput();
        }
    }
    class ShowUserSpellingHint implements View.OnClickListener {
        public void onClick(View v) {
            // Erase all user input to show the hint clearly.
            setTypedString( "" );

            // With the spelling hint flag enabled, the displayUserInput will be ran and will show the hint.
            is_spelling_hint_enabled = true;
            displayUserInput();
        }
    }

    // Play the audio file for the word we have tapped on.
    class MyClickableSpan extends ClickableSpan {
        private String word;
        private String audio_file_path;
        private ReadAlongTiming read_along_timing;
        private WordWithIndexes word_with_indexes; // Used to pass the indexes of the word to highlight.

        MyClickableSpan( String word, String audio_file_path, ReadAlongTiming read_along_timing, WordWithIndexes word_with_indexes ) {
            // Set the audio file
            this.word = word;
            this.audio_file_path = audio_file_path;
            this.read_along_timing = read_along_timing;
            this.word_with_indexes = word_with_indexes;

        }

        public WordWithIndexes getWordsWithIndexes() {
            return word_with_indexes;
        }

        @Override
        public void onClick(View widget) {
            if( is_answer_shown == false ) {
                // When in sentence mode
                // They have not heard the audio and must have clicked
                // the word for a hint, so set the card to failed.
                deck.nextQuestion(false, true);
                AnimationCrossesFallingShort();
            }
            // Play Audio File.
            MyMediaPlayer audio = new MyMediaPlayer();
            try {
                // Highlight the word we have clicked on so the user has visual feedback.
                if( read_along_timing != null ) {
                    audio.setDataSource( this.audio_file_path );
                    audio.prepare();
                    ArrayList<ReadAlongTiming> t = new ArrayList<ReadAlongTiming>();
                    t.add( new ReadAlongTiming( 0, audio.getDuration(), this.word_with_indexes) );

                    audio.start();

                    ThreadHighlightWord thread_highlight_word = new ThreadHighlightWord( audio, this.word, t );
                    thread_highlight_word.start();
                } else {
                    audio.setDataSource( this.audio_file_path );
                    audio.prepare();
                    audio.start();
                }

            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        @Override
        // Stop it from changing the text to looking like a link.
        public void updateDrawState( TextPaint text_paint ) {
            // Do nothing.
            // Handy to keep these for use when debugging.
            //text_paint.setColor( Color.RED );
            //text_paint.setUnderlineText( true );
        }
    }

    /**
     * THIS IS COMPLETELY different than ThreadHighlightWord
     *
     * Colours in every letter in a sound/word, and plays the audio for each letter, followed by the audio of the sound/word.
     * The highlighted letter will be the colour red.
     *
     */
    class ThreadHighlightLetters extends Thread {
        MyMediaPlayer audio;
        String text;
        String audio_file;

        ThreadHighlightLetters( String audio_file, String text) {
            this.audio_file = audio_file;
            this.text = text;
        }

        public void run() {
            spannable_string = new SpannableStringBuilder();
            spannable_string.append(text);
            // Highlight each letter and play the audio for each letter as well.
            text = text.toLowerCase();
            for( int i = 0; i < text.length(); i++ ) {
                char letter = text.charAt(i);
                if( (letter >= 'a') && (letter <= 'z') ) {
                    // Highlight the letter.
                    spannable_string.setSpan( new ForegroundColorSpan(Color.BLACK), 0, text.length(), 0);
                    spannable_string.setSpan( new ForegroundColorSpan(Color.RED), i, i+1, 0);
                    // We can't touch the TextView from this thread, so use this method to run it on the Ui's thread.
                    runOnUiThread( new SetReadAlongColour( spannable_string ) );

                    // Play the audio for the letter.
                    String audio_file_path = db_media_folder + File.separator + "letters" + File.separator + letter + ".wav";
                    play_audio( audio_file_path );
                }
            }

            // Make the text black.
            spannable_string.setSpan( new ForegroundColorSpan(Color.BLACK), 0, text.length(), 0);
            runOnUiThread( new SetReadAlongColour( spannable_string ) );

            // Pause for half a second before playing the audio file for the sound/word.
            long pause_length = System.currentTimeMillis() + 500;
            while( System.currentTimeMillis() < pause_length ) {
                // Do nothing.
            }

            // Make the text red.
            spannable_string.setSpan( new ForegroundColorSpan(Color.RED), 0, text.length(), 0);
            runOnUiThread( new SetReadAlongColour( spannable_string ) );

            // Play the audio file sound/word.
            play_audio( audio_file );

            // Make the text black.
            spannable_string.setSpan( new ForegroundColorSpan(Color.BLACK), 0, text.length(), 0);
            runOnUiThread( new SetReadAlongColour( spannable_string ) );


            // Enable the answer buttons
            // Used a handler, because we can't access the UI elements from another thread, so this is
            // a thread safe way to call the UI.
            Message message = new Message();
            message.what = MyUiUpdateHandler.MESSAGE_ENABLE_BUTTONS;
            // Send message to main thread Handler.
            my_ui_handler.sendMessage(message);
        }

        void play_audio( String audio_file_path ) {
            audio = new MyMediaPlayer();
            try {
                audio.setDataSource( audio_file_path );
                audio.prepare();
                audio.start();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            boolean keep_going = true;
            while( keep_going ) {
                if( audio.getDuration() == -1 ) {
                    // Do nothing, although this should never happen, keep it here for debugging, just in case.
                    System.out.println("Audio may have not been initialized. Currently in ThreadHighlightWord");
                }

                long current_position = System.currentTimeMillis() - audio.getStartTimeInMillis();

                if( current_position >= audio.getDuration() ) {
                    keep_going = false;
                }
            }
        }
    }

    /**
     * Colour every word in either red or black, depending on the timings we give it.
     * Used for highlight words in a sentence once the audio has started playing to simulate a read-along with me feature.
     */
    class ThreadHighlightWord extends Thread {
        MyMediaPlayer audio;
        public boolean keep_going = true;
        ArrayList<ReadAlongTiming> read_along_timings;

        ThreadHighlightWord( MyMediaPlayer audio, String card_text, ArrayList<ReadAlongTiming> read_along_timings ) {
            this.audio = audio;
            this.read_along_timings = read_along_timings;

            if( read_along_timings == null ) {
                long start_pos_in_millis = 0;
                long end_pos_in_millis = audio.getDuration();
                WordWithIndexes words_with_indexes = new WordWithIndexes(card_text, 0, card_text.length() );

                new ReadAlongTiming( start_pos_in_millis,end_pos_in_millis, words_with_indexes );
            }
        }
        public void run() {
            this.keep_going = true;
            while( keep_going ) {
                if( this.audio.getDuration() == -1 ) {
                    // Do nothing, although this should never happen, keep it here for debugging, just in case.
                    System.out.println("Audio may have not been initialized. Currently in ThreadHighlightWord");
                }

                long current_position = System.currentTimeMillis() - this.audio.getStartTimeInMillis();

                // Plus 1 to current duration so we know that the colourLabel
                // will be called after the audio has finished playing too.
                if( current_position >= audio.getDuration() + 1 ) {
                    this.keep_going = false;
                }
                if( read_along_timings != null ) {
                    for (int i = 0; i < this.read_along_timings.size(); i++) {
                        this.read_along_timings.get(i).colourLabel(current_position);
                    }
                }
            }

            // Enable the answer buttons
            // Used a handler, because we can't access the UI elements from another thread, so this is
            // a thread safe way to call the UI.
            Message message = new Message();
            message.what = MyUiUpdateHandler.MESSAGE_ENABLE_BUTTONS;
            // Send message to main thread Handler.
            my_ui_handler.sendMessage(message);
        }
    }


    /**
     * Thread safe way to update the UI.
     */
    class MyUiUpdateHandler extends Handler {
        public static final int MESSAGE_ENABLE_BUTTONS = 1;
        @Override
        public void handleMessage(Message msg) {
            // Means the message is sent from child thread.
            if(msg.what == MESSAGE_ENABLE_BUTTONS)
            {
                // Update ui in using the main thread.
                EnableButtons();
            }
        }
    }

    /**
     * This button can highlight words in a sentence once the audio has started playing to simulate a "read-along with me" feature.
     */
    class MyAudioButtonWithWordHighlighting extends MyAudioButton {
        public boolean is_highlighting_enabled;
        private String card_text;

        public MyAudioButtonWithWordHighlighting( Context context, String card_text, String audio_file, boolean autostart, boolean is_highlighting_enabled ) {
            super(context, audio_file, autostart);
            this.is_highlighting_enabled = is_highlighting_enabled;
            this.card_text = card_text;
            // Highlight our words as the audio plays.
            if( autostart && is_highlighting_enabled ) {
                DisableButtons();
                ThreadHighlightWord thread_highlight_word = new ThreadHighlightWord( this.getMediaPlayer(), this.card_text, read_along_timings );
                audio.start();
                thread_highlight_word.start();
            }
        }

        @Override
        public void onClick(View v) {
            DisableButtons();
            // play media file.
            if( ! audio.isPlaying() ) {
                if( this.is_highlighting_enabled ) {
                    ThreadHighlightWord thread_highlight_word = new ThreadHighlightWord( this.getMediaPlayer(), this.card_text, read_along_timings );
                    audio.start();
                    thread_highlight_word.start();
                } else {
                    audio.start();
                }
            }
        }
    }

    class ReadAlongTiming {
        private boolean is_colour_red = false;
        private long start_position_in_millis;
        private long end_position_in_millis;
        private WordWithIndexes word_with_indexes;

        ReadAlongTiming( long start_position_in_millis, long end_position_in_millis, WordWithIndexes word_with_indexes ) {
            this.start_position_in_millis = start_position_in_millis;
            this.end_position_in_millis   = end_position_in_millis;
            this.word_with_indexes = word_with_indexes;
        }
        public long getStartPositionInMillis() {
            return this.start_position_in_millis;
        }
        public long getEndPositionInMillis() {
            return this.end_position_in_millis;
        }

        /**
         * Pass this the audio's current position of playback in millis and it will colour
         * the word red if we are inbetween the starting and ending millis for the read along timing.
         * @param position_in_millis
         */
        public void colourLabel( long position_in_millis ) {
            if( position_in_millis >= getStartPositionInMillis() && position_in_millis <= getEndPositionInMillis() ) {
                if( is_colour_red == false ) {
                    is_colour_red = true;
                    spannable_string.setSpan( new ForegroundColorSpan(Color.RED), word_with_indexes.getStartingIndex(), word_with_indexes.getEndingIndex(), 0);
                    // We can't touch the TextView from this thread, so use this method to run it on the Ui's thread.
                    runOnUiThread( new SetReadAlongColour( spannable_string ) );
                }
            } else {
                if( is_colour_red ) {
                    is_colour_red = false;
                    spannable_string.setSpan( new ForegroundColorSpan(Color.BLACK), word_with_indexes.getStartingIndex(), word_with_indexes.getEndingIndex(), 0);
                    runOnUiThread( new SetReadAlongColour( spannable_string ) );
                }
            }
        }
    }
    /**
     * Inner class dedicated to updating the txtReadAlong TextView
     * We need to access it from the same UI thread, so passing an instance
     * of this to runOnUiThread does that for us.
     */
    class SetReadAlongColour extends Thread {
        private SpannableStringBuilder spanny;
        SetReadAlongColour(SpannableStringBuilder spanny) {
            this.spanny = spanny;
        }
        public void run() {
            txtReadAlong.setText(spanny ,TextView.BufferType.SPANNABLE);
        }
    }
}

/**
 * Manages a list of ReadingLessonCard objects.
 * Allowing us to insert and find a card by a binary search for efficiency.
 */
class ReadingLessonCardList {
    ArrayList<ReadingLessonCard> all_cards;

    ReadingLessonCardList() {
        this.all_cards = new ArrayList<ReadingLessonCard>();
    }

    /**
     * Add a card to the list. This is done using a binary search.
     */
    public void add( ReadingLessonCard card ) {
        if( all_cards.size() == 0 ) {
            all_cards.add( card );
        } else {

            // Find the correct location to insert in the array
            // Using a binary search.
            int index = findCardInsertionIndexByCardID( card.getCardID(), 0, all_cards.size() -1 );

            // Add the card to the array
            all_cards.add( index + 1, card );

        }
    }

    /**
     * Recursive binary search through the list to find whether the card is in the list..
     * @param card_id
     * @return
     */
    public boolean isCardInList( int target_card_id ) {
        return __isCardInList( target_card_id, 0, all_cards.size() -1 );
    }

    /**
     * Recursive binary search through the list to find whether the card is in the list.
     * You must pass the last index, not the size of the array.
     * e.g. [ a, b, c ] last index is 2. array size is 3.
     * @param card_id
     * @param start_index
     * @param end_index
     * @return
     */
    private boolean __isCardInList( int target_card_id, int start, int end  )  {
        int left = start;
        int right = end;

        // Base cases
        if( left > right ) {
            return false;
        }
        else if( left == right ) {
            if( target_card_id == all_cards.get(left).getCardID() ) {
                return true;
            } else {
                return false;
            }
        }
        else {

            // search.
            int middle_index = ( left + right ) / 2;
            if( target_card_id < all_cards.get(middle_index).getCardID() ) {
                right = middle_index - 1;
                return __isCardInList(target_card_id, left, right);
            }
            else if( target_card_id > all_cards.get(middle_index).getCardID() ) {
                left = middle_index + 1;
                return __isCardInList(target_card_id, left, right);
            } else {
                // The target_card_id must be the same as the card at the middle_index.
                return true;
            }
        }
    }

    /**
     * Recursive binary search through the list to find the correct location to insert a new card.
     * @param card_id
     * @param start_index
     * @param end_index
     * @return
     */
    public int findCardInsertionIndexByCardID( int target_card_id, int start, int end  )  {

        int left = start;
        int right = end;

        // Base cases
        if( left >= right ) {
            if( left >= all_cards.size() ) {
                return all_cards.size() - 1;
            }

            if( target_card_id == all_cards.get( left ).getCardID() ) {
                return left;
            }
            else if( target_card_id < all_cards.get( left ).getCardID() ) {
                return left -1;

            }
            else if( target_card_id > all_cards.get( left ).getCardID() ) {
                return left;
            }
        }
        else {

            // search.
            int middle_index = ( left + right ) / 2;
            if( target_card_id < all_cards.get(middle_index).getCardID() ) {
                right = middle_index - 1;
                return findCardInsertionIndexByCardID(target_card_id, left, right);
            }
            else if( target_card_id > all_cards.get(middle_index).getCardID() ) {
                left = middle_index + 1;
                return findCardInsertionIndexByCardID(target_card_id, left, right);
            } else {
                return middle_index;
            }
        }

        // Should never reach this statement.
        return -1;
    }

    /**
     * Binary search through the list and return the card with the card ID.
     * Returns null if there is no card found.
     * @param all_cards
     * @param target_card_id
     * @return
     */
    public static ReadingLessonCard getCardByID( ReadingLessonCardList all_cards, int target_card_id ) {
        return __getCardByID( all_cards, target_card_id, 0, all_cards.getAllCards().size() );
    }

    /**
     * Binary search through the list and return the card with the card ID.
     * Returns null if there is no card found.
     * @param all_cards
     * @param target_card_id
     * @return
     */
    public ReadingLessonCard getCardByID( int target_card_id ) {
        return __getCardByID( this, target_card_id, 0, all_cards.size() );
    }

    /**
     * Recursive binary search.
     */
    private static ReadingLessonCard __getCardByID( ReadingLessonCardList deck, int target_card_id, int left, int right ) {
        // search.
        if( left <= right ) {
            int middle_index = ( left + right ) / 2;

            if( middle_index < deck.getAllCards().size() ) {
                int middle_card_id = deck.getAllCards().get(middle_index).getCardID();
                if( target_card_id == middle_card_id ) {
                    return deck.getAllCards().get(middle_index);
                }
                else if( target_card_id < middle_card_id ) {
                    right = middle_index - 1;
                    return __getCardByID( deck, target_card_id, left, right);
                }
                else if( target_card_id > middle_card_id ) {
                    left = middle_index + 1;
                    return __getCardByID( deck, target_card_id, left, right);
                }
            }
        }

        return null;
    }

    public ReadingLessonCard getCardByIndex( int index ) {
        return all_cards.get( index );
    }

    public ArrayList<ReadingLessonCard> getAllCards() {
        return this.all_cards;
    }

    public int size() {
        return this.all_cards.size();
    }
}
class Timings {
    public long start_in_millis;
    public long end_in_millis;
    Timings( long start_in_millis, long end_in_millis ) {
        this.start_in_millis = start_in_millis;
        this.end_in_millis = end_in_millis;
    }
}

class ReadingLessonDBHandler extends SQLiteOpenHelper {
    public ReadingLessonDBHandler( Context context, String db_name, int db_version ) {
        super( context, db_name, null, db_version );
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Do nothing because we download the database and use that.
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Do nothing as the database has stayed the same so far.
    }
}

class ReadingLessonStatisticsDBHandler extends SQLiteOpenHelper {
    public static String TABLE_NAME               = "reading_lesson_stats";
    public static String STAT_ID                  = "stat_id";
    public static String CARD_ID                  = "card_id";
    public static String DATE_IN_MILLIS           = "date_in_millis";
    public static String BOX_NUM                  = "box_num";
    public static String TIME_TO_ANSWER_IN_MILLIS = "time_to_answer_in_millis";
    public static String IS_ANSWER_CORRECT        = "is_answer_correct";
    public static String WAS_SPELLING_HINT_USED   = "was_spelling_hint_used";

    public ReadingLessonStatisticsDBHandler( Context context, String db_name, int db_version ) {
        super( context, db_name, null, db_version );
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql_create_table =
                "CREATE TABLE IF NOT EXISTS " + TABLE_NAME +
                        "( " +
                        STAT_ID                  + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        DATE_IN_MILLIS           + " INTEGER, " +
                        CARD_ID                  + " INTEGER, " +
                        BOX_NUM                  + " INTEGER, " +
                        TIME_TO_ANSWER_IN_MILLIS + " INTEGER, " +
                        IS_ANSWER_CORRECT        + " INTEGER, " +
                        WAS_SPELLING_HINT_USED   + " INTEGER" +
                        " );";
        db.execSQL( sql_create_table );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Do nothing as the database has stayed the same so far.
    }
}
