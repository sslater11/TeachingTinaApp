/* © Copyright 2022, Simon Slater

This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, version 2 of the License.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package com.example.tinasnewmaths;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ScaleDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.BackgroundColorSpan;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.plattysoft.leonids.ParticleSystem;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import libteachingtinadbmanager.Card;
import libteachingtinadbmanager.CardDBManager;
import libteachingtinadbmanager.CardDBTagManager;
import libteachingtinadbmanager.DeckSettings;
import libteachingtinadbmanager.ReadingLessonDeck;
import libteachingtinadbmanager.SentenceAnalyzer;
import libteachingtinadbmanager.WordWithIndexes;

public class ReadingAndSpellingActivity extends FlashcardGroupActivity {
    protected String typed_string = "";
    ArrayList<Button> spelling_buttons;
    TextView txtTyped;
    TextView txtReadAlong;
    Button clear_user_input;
    Button btn_spelling_hint;
    boolean is_spelling_hint_enabled = false;
    boolean is_answer_shown = false;
    int MINIMUM_SPELLING_BUTTONS = 6;
    String alphabet = "abcdefghijklmnopqrstuvwxyz";
    ReadingLessonDeck reading_spelling_deck;
    private float USER_TEXT_FONT_SIZE = 72f;
    private float LETTER_BUTTONS_FONT_SIZE = 32f;
    private Typeface FONT_TYPEFACE = Typeface.SANS_SERIF;
    private TableLayout audio_buttons_table_layout;
    private boolean is_audio_playing = false;
    private SpannableStringBuilder spannable_string;
    private ArrayList<ReadAlongTiming> read_along_timings;

    // We do call super.onCreate(), we call another super method to pass the activity id so that the correct one is loaded.
    @SuppressLint("MissingSuperCall")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        String str_db_file = getIntent().getStringExtra("deck_file_path");
        db_file         = new File( str_db_file );
        db_config_file  = new File( getIntent().getStringExtra("deck_settings_file_path") );
        db_media_folder = new File( str_db_file.replace(".txt", "") );

        settings = CardDBManager.getConfig(db_config_file);
        group_names = CardDBManager.readDBGetGroupNames(db_file, settings);
        System.out.println(group_names);
        ArrayList<Card> tmp_deck = CardDBManager.readDBGetGroup(db_file, settings, group_names.get(0));
        reading_spelling_deck = new ReadingLessonDeck( tmp_deck, db_file, settings);
        setContentView(R.layout.activity_reading_and_spelling);

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

    }

    public void AddContent(ArrayList<String> list) {
        // Do nothing.
        // This There's no need for this method as we show the card's content differently for each question and answer for each mode(reading mode, spelling mode, sentence mode).

        //if( ReadingLessonDeck.isCardReadingMode( reading_spelling_deck.getCurrentCard() ) ) {
        //} else if( ReadingLessonDeck.isCardSpellingMode( reading_spelling_deck.getCurrentCard() ) ) {
        //} else if( ReadingLessonDeck.isCardSentenceMode( reading_spelling_deck.getCurrentCard() ) ) {
        //}
    }


    public void clearUserInput() {
        setTypedString("");
        txtTyped.setText("");
        displayUserInput();
    }

    @Override
    public void DisplayQuestion() {
        this.is_answer_shown = false;
        font_size = DEFAULT_FONT_SIZE * 2;

        if( ReadingLessonDeck.isCardReadingMode( reading_spelling_deck.getCurrentCard() ) ) {
            // Display just the word on the screen.
            String word = reading_spelling_deck.getCardText( reading_spelling_deck.getCurrentCard() ).get(0);
            this.txtTyped.setText( word );
            scroll_layout.addView( this.txtTyped );
        }
        else if( ReadingLessonDeck.isCardSpellingMode( reading_spelling_deck.getCurrentCard() ) ) {
            // Spelling mode.
            // It's spelling mode, so we need to completely change the layout to spelling mode.
            spelling_buttons = new ArrayList<Button>();

            // Get every letter in the word and make a button for it.
            String word = reading_spelling_deck.getCardText( reading_spelling_deck.getCurrentCard() ).get(0);
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
        if( ReadingLessonDeck.isCardSentenceMode( reading_spelling_deck.getCurrentCard() ) ) {
            String sentence = reading_spelling_deck.getCardText( reading_spelling_deck.getCurrentCard() ).get(0);

            // Load the timings for highlighting the words in time with the audio.

            // Get the read along timings file path as a string
            String temp_read_along_tag = reading_spelling_deck.getCardReadAlongTimings( reading_spelling_deck.getCurrentCard() ).get(0);
            // There's probably only one read-along-timings file, so get that one's path.
            String read_along_timings_file_name = CardDBTagManager.getReadAlongTimingsFilename(temp_read_along_tag);
            String read_along_timings_file_path = db_media_folder + "/" + read_along_timings_file_name;

            // Open the timings file.
            String line = "";
            BufferedReader br = null;
            try{
                br = new BufferedReader( new FileReader(read_along_timings_file_path));

                // Read in the string of timings.
                // There's only one line in the timings file, so just read that.
                line = br.readLine();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally{
                if( br != null ) {
                    try {
                        br.close();
                    } catch( IOException e ) {
                        e.printStackTrace();
                    }
                }
            }

            // Split the string by tabs as that's what's seperating each timing.
            String[] str_timings_list = line.split("\t");

            // Convert the string into an ArrayList of Timing objects.
            // We will then use this list to make ReadAlongTiming objects.
            ArrayList<Timings> timings_list = new ArrayList<Timings>();
            for( int i = 0; i < (str_timings_list.length /2); i++ ) {
                long start_position = Long.parseLong( str_timings_list[ i*2  ] );
                long end_position   = Long.parseLong( str_timings_list[(i*2)+1] );

                Timings t = new Timings( start_position, end_position );
                timings_list.add( t );
            }


            // Give each word it's own on click listener so the user can hear the word by it's self when they click it.
            // and add the word timings to the read_along_timings ArrayList.
            ArrayList<WordWithIndexes> words_list_with_indexes = SentenceAnalyzer.getWordsListWithIndexes( sentence );
            spannable_string = new SpannableStringBuilder();
            spannable_string.append(sentence);
            read_along_timings = new ArrayList<ReadAlongTiming>();
            for( int i = 0; i < words_list_with_indexes.size(); i++ ) {
                String audio_name = words_list_with_indexes.get(i).getWordWithIgnoredCharactersRemoved().toLowerCase();
                String audio_file_path = db_media_folder + "/" + audio_name + ".mp3";

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
        ArrayList<String> image_list = reading_spelling_deck.getCardImage( reading_spelling_deck.getCurrentCard() );
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
        ArrayList<String> audio_list = reading_spelling_deck.getCardAudio( reading_spelling_deck.getCurrentCard() );
        for (int i = 0; i < audio_list.size(); i++) {
            // Make an audio button and add the file's path.
            String audio_filepath = CardDBTagManager.getAudioFilename(audio_list.get(i));
            audio_filepath = db_media_folder + "/" + audio_filepath;

            if( i == 0 ) {
                boolean is_highlighting_enabled = ReadingLessonDeck.isCardSentenceMode( reading_spelling_deck.getCurrentCard() );
                audioArr.add(new MyAudioButtonWithWordHighlighting(this, audio_filepath, true, is_highlighting_enabled));
            } else {
                audioArr.add(new MyAudioButtonWithWordHighlighting(this, audio_filepath, false, false));
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
    public void ShowAnswerPressed() {
        this.is_answer_shown = true;
        if( ReadingLessonDeck.isCardReadingMode( reading_spelling_deck.getCurrentCard() ) ||
            ReadingLessonDeck.isCardSentenceMode( reading_spelling_deck.getCurrentCard() ) ) {

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
        else if( ReadingLessonDeck.isCardSpellingMode( reading_spelling_deck.getCurrentCard() ) ) {
            String user_input = getTypedString().toLowerCase();
            String word = reading_spelling_deck.getCardText(reading_spelling_deck.getCurrentCard()).get(0).toLowerCase();

            // Do nothing if they haven't even typed anything yet.
            if (user_input.length() == 0) {
                return;
            }
            if (user_input.compareToIgnoreCase(word) == 0) {
                // Answer is correct

                // Explosion of ticks celebration.
                // Fucking love this, major thanks to the Leonids library!
                AnimationExplodingTicks();

                if (is_spelling_hint_enabled) {
                    // Mark it as wrong, so they have to attempt it again without the spelling hint.
                    reading_spelling_deck.nextQuestion(false, false);
                } else {
                    // Since they didn't see a hint, mark it as correct.
                    reading_spelling_deck.nextQuestion(true, false);
                }

                // Reset for the next card.
                is_spelling_hint_enabled = false;

            } else {
                // Answer is wrong.

                // Red crosses shooting out of the top of the screen.
                // Fucking love this, major thanks to the Leonids library!
                AnimationCrossesFalling();

                // Mark the card as wrong and stay on the current card.
                // Set the hint to true so they can see the word.
                reading_spelling_deck.nextQuestion(false, true);
                is_spelling_hint_enabled = true;
            }
            clearUserInput();
            NextQuestion();
        }
    }


    public void AnimationExplodingTicks() {
        // Explosion of ticks celebration.
        // Fucking love this, major thanks to the Leonids library!
        int num_of_particles = 10000;
        int time_to_live = 10000;
        new ParticleSystem(this, num_of_particles, R.drawable.answer_tick_small, time_to_live)
                .setSpeedRange(0.20f, 1.5f)
                .setScaleRange(0.10f, 1f)
                .oneShot(bt_show_answer, num_of_particles);
    }

    public void AnimationCrossesFalling() {
        // Red crosses shooting out of the top of the screen.
        // Fucking love this, major thanks to the Leonids library!
        ParticleSystem particle = new ParticleSystem(this, 200, R.drawable.answer_cross_medium, 2500);
        particle.setSpeedModuleAndAngleRange(0.3f, 0.8f, 35, 145);
        particle.setRotationSpeed(180);
        particle.emit(findViewById(R.id.top_emitter), 30, 1000);
    }

    public void AnimationCrossesFallingShort() {
        // Red crosses shooting out of the top of the screen.
        // Fucking love this, major thanks to the Leonids library!
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
        reading_spelling_deck.nextQuestion(true, false);
        NextQuestion();
        AnimationExplodingTicks();
    }

    @Override
    public void IncorrectPressed() {
        // They got it wrong.
        // Stop the user from tapping the buttons whilst it's doing stuff.
        DisableButtons();
        clearUserInput();
        reading_spelling_deck.nextQuestion(false, false);
        NextQuestion();
        AnimationCrossesFalling();
    }

    //public void NextQuestion() {
    //    super.NextQuestion();

    //    setTypedString("");
    //    displayUserInput();
    //}
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
        String word = reading_spelling_deck.getCardText( reading_spelling_deck.getCurrentCard() ).get(0);
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
            String answer_word = reading_spelling_deck.getCardText( reading_spelling_deck.getCurrentCard() ).get(0);

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

        @Override
        public void onClick(View widget) {
            if( is_answer_shown == false ) {
                // When in sentence mode
                // They have not heard the audio and must have clicked
                // the word for a hint, so set the card to failed.
                reading_spelling_deck.nextQuestion(false, true);
                AnimationCrossesFallingShort();
            }
            // Play Audio File.
            MyMediaPlayer audio = new MyMediaPlayer();
            try {
                audio.setDataSource( this.audio_file_path );
                audio.prepare();
                audio.start();
                // Highlight the word we have clicked on so the user has visual feedback.
                if( read_along_timing != null ) {
                    ArrayList<ReadAlongTiming> t = new ArrayList<ReadAlongTiming>();
                    t.add( new ReadAlongTiming( 0, audio.getDuration(), this.word_with_indexes) );

                    ThreadHighlightWord thread_highlight_word = new ThreadHighlightWord( audio, t );
                    thread_highlight_word.start();
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
     * Colour every word in either red or black, depending on the timings we give it.
     * Used for highlight words in a sentence once the audio has started playing to simulate a read-along with me feature.
     */
    class ThreadHighlightWord extends Thread {
        MyMediaPlayer audio;
        public boolean keep_going = true;
        ArrayList<ReadAlongTiming> read_along_timings;

        ThreadHighlightWord( MyMediaPlayer audio, ArrayList<ReadAlongTiming> read_along_timings) {
            this.audio = audio;
            this.read_along_timings = read_along_timings;
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
                for (int i = 0; i < this.read_along_timings.size(); i++) {
                    this.read_along_timings.get(i).colourLabel( current_position );
                }
            }
        }
    }

    /**
     * This button can highlight words in a sentence once the audio has started playing to simulate a "read-along with me" feature.
     */
    class MyAudioButtonWithWordHighlighting extends MyAudioButton {
        public boolean keep_going = true;
        public boolean is_highlighting_enabled = false;

        public MyAudioButtonWithWordHighlighting( Context context, String audio_file, boolean autostart, boolean is_highlighting_enabled ) {
            super(context, audio_file, autostart);
            this.is_highlighting_enabled = is_highlighting_enabled;
            // Highlight our words as the audio plays.
            if( autostart && is_highlighting_enabled ) {
                ThreadHighlightWord thread_highlight_word = new ThreadHighlightWord( this.getMediaPlayer(), read_along_timings );
                thread_highlight_word.start();
            }
        }

        @Override
        public void onClick(View v) {
            //play media file.
            if( ! audio.isPlaying() ) {
                audio.start();
                if( this.is_highlighting_enabled ) {
                    ThreadHighlightWord thread_highlight_word = new ThreadHighlightWord( this.getMediaPlayer(), read_along_timings );
                    thread_highlight_word.start();
                }
            }
        }
    }

    class ReadAlongTiming {
        private boolean is_colour_red = false;
        private long start_position_in_millis = -1;
        private long end_position_in_millis = -1;
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
}

class Timings {
    public long start_in_millis;
    public long end_in_millis;
    Timings( long start_in_millis, long end_in_millis ) {
        this.start_in_millis = start_in_millis;
        this.end_in_millis = end_in_millis;
    }
}
