package com.example.tinasnewmaths;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.Html;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.plattysoft.leonids.ParticleSystem;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import libteachingtinadbmanager.Card;
import libteachingtinadbmanager.CardDBManager;
import libteachingtinadbmanager.CardDBTagManager;
import libteachingtinadbmanager.DeckSettings;
import libteachingtinadbmanager.ReadingLessonDeck;

public class ReadingAndSpellingActivity extends FlashcardGroupActivity {
    protected String typed_string = "";
    ArrayList<Button> spelling_buttons;
    TextView txtTyped;
    Button clear_user_input;
    Button btn_spelling_hint;
    boolean is_reading_mode = false;
    boolean is_spelling_hint_enabled = false;
    int MINIMUM_SPELLING_BUTTONS = 6;
    String alphabet = "abcdefghijklmnopqrstuvwxyz";
    ReadingLessonDeck reading_spelling_deck;
    private float USER_TEXT_FONT_SIZE = 72f;
    private float LETTER_BUTTONS_FONT_SIZE = 32f;
    private Typeface FONT_TYPEFACE = Typeface.SANS_SERIF;

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

        if( is_reading_mode ) {


            for (int i = 0; i < list.size(); i++) {
                if (CardDBTagManager.hasFontSizeTag(list.get(i))) {
                    font_size = CardDBTagManager.getFontSize(list.get(i));
                } else if (CardDBTagManager.hasImageTag(list.get(i))) {
                    // Make an image box and add the image.
                    imgArr.add(new ImageView(this));
                    int img_index = imgArr.size() - 1;

                    String img_filename = CardDBTagManager.getImageFilename(list.get(i));

                    // TODO: fix this with a try and catch, just in case we try to load a picture that's not there.
                    //       just display an image that says no picture found.

                    // Get the image file as a bitmap, and set it to the image view.
                    Bitmap bitmap = BitmapFactory.decodeFile(db_media_folder + "/" + img_filename);
                    imgArr.get(img_index).setImageBitmap(bitmap);
                    // Make the image view fit the image. On larger images that have been shrunk, they contain whitespace for some reason.
                    // This gets rid of the whitespace.
                    imgArr.get(img_index).setAdjustViewBounds(false);

                    scroll_layout.addView(imgArr.get(img_index));
                } else if (CardDBTagManager.hasAudioTag(list.get(i))) {
                    // Make an audio button and add the file's path.
                    String audio_filepath = CardDBTagManager.getAudioFilename(list.get(i));
                    audio_filepath = db_media_folder + "/" + audio_filepath;

                    audioArr.add(new MyAudioButton(this, audio_filepath, true));
                    int audio_index = audioArr.size() - 1;


                    // TODO: fix this with a try and catch, just in case we try to load an audio file that's not there.
                    //       just display an image that says no audio found.


                    // Make the image view fit the image. On larger images that have been shrunk, they contain whitespace for some reason.
                    // This gets rid of the whitespace.
                    //imgArr.get(img_index).setAdjustViewBounds(true);

                    scroll_layout.addView(audioArr.get(audio_index));
                } else {
                    // Make a text box and add the text.
                    tvArr.add(new TextView(this));
                    int tv_index = tvArr.size() - 1;

                    // Using Html.fromHtml() to preserve the bold, italic and underline tags.
                    tvArr.get(tv_index).setText(Html.fromHtml(list.get(i)));
                    tvArr.get(tv_index).setTextSize(font_size);
                    tvArr.get(tv_index).setGravity(Gravity.CENTER_HORIZONTAL);
                    scroll_layout.addView(tvArr.get(tv_index));
                }
            }
        } else {
            // It's spelling mode, so add the components in the right order.

            // This is used to center the table layouts on the screen.
            // The table layout is used for the audio buttons.
            LinearLayout.LayoutParams linear_layout_parameters = new LinearLayout.LayoutParams( LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            linear_layout_parameters.gravity = Gravity.CENTER;

            // Make an array of audio buttons for each audio file in the flashcard.
            ArrayList<String> audio_list = reading_spelling_deck.getCardAudio( reading_spelling_deck.getCurrentCard() );
            for (int i = 0; i < audio_list.size(); i++) {
                // Make an audio button and add the file's path.
                String audio_filepath = CardDBTagManager.getAudioFilename(audio_list.get(i));
                audio_filepath = db_media_folder + "/" + audio_filepath;

                if( i == 0 ) {
                    audioArr.add(new MyAudioButton(this, audio_filepath, true));
                } else {
                    audioArr.add(new MyAudioButton(this, audio_filepath, false));
                }
            }

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

            // Create the table for the audio buttons to be in the same row.
            TableLayout audio_buttons_table_layout = new TableLayout( this );

            // Finally add the buttons.
            TableRow audio_tr = new TableRow(this );
            for( int i = 0; i < audioArr.size(); i++ ) {
                //audioArr.get(i).setGravity( Gravity.CENTER );
                audio_tr.addView( audioArr.get(i) );
            }
            audio_buttons_table_layout.addView( audio_tr );

            // Setup the parameters to make the hint button span the columns of the audio buttons
            TableRow.LayoutParams row_layout_parameters = new TableRow.LayoutParams();
            row_layout_parameters.span = audioArr.size();

            // Add the hint button to the table row.
            TableRow hint_tr = new TableRow( this );
            hint_tr.addView( btn_spelling_hint, row_layout_parameters );
            audio_buttons_table_layout.addView( hint_tr );

           // Add the images to the images layout.
            LinearLayout images_layout = new LinearLayout(this );
            for( int i = 0; i < imgArr.size(); i++ ) {
                images_layout.addView( imgArr.get(i) );
            }

            // Center the images to the middle of the screen.
            images_layout.setLayoutParams( linear_layout_parameters );
            images_layout.setGravity(Gravity.CENTER);

            // Center the grid into the middle of the screen.
            audio_buttons_table_layout.setLayoutParams( linear_layout_parameters );

            // Add the layouts to scroll layout.
            scroll_layout.addView( images_layout );
            scroll_layout.addView( audio_buttons_table_layout );
        }
    }


    public void clearUserInput() {
        setTypedString("");
        txtTyped.setText("");
        displayUserInput();
    }

    @Override
    public void DisplayQuestion() {
        font_size = DEFAULT_FONT_SIZE * 2;

        if( deck.getCurrentCard().group.getGroupName().compareTo("Words") == 0 ) {
            if( is_reading_mode ) {
                // Display just the word on the screen.
                AddContent( reading_spelling_deck.getCardText( reading_spelling_deck.getCurrentCard() ) );
            } else {
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
                if( spelling_buttons.size() < MINIMUM_SPELLING_BUTTONS ) {
                    // If the word is less than 6, add more letters.
                    num_random_letters_buttons_to_add = MINIMUM_SPELLING_BUTTONS - spelling_buttons.size();
                } else {
                    // The word is bigger than or equal to 6 letters.
                    // so add enough buttons to fit on another row.
                    num_random_letters_buttons_to_add = max_column - (spelling_buttons.size() % max_column);
                }
                for( int i = 0; i < num_random_letters_buttons_to_add; i++ ) {
                    // Add a random letter, but only if it's not in the current word.
                    int letter_index = (int)((Math.random() * 100) % 26);
                    while( isLetterInButtonArray(spelling_buttons, alphabet.charAt(letter_index)) ) {
                        letter_index = (int)((Math.random() * 100) % 26);
                    }
                    Button letter_button = new SpellingButton(this, alphabet.charAt( letter_index ));
                    letter_button.setOnClickListener(new LetterListener( alphabet.charAt(letter_index) ) );
                    spelling_buttons.add( letter_button );
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
                    //grid_layout.addView(spelling_buttons.get(i));
                }

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
                //AddContent( reading_spelling_deck.getCardAudio( reading_spelling_deck.getCurrentCard() ) );
                AddContent( reading_spelling_deck.getCardImage( reading_spelling_deck.getCurrentCard() ) );


                // Add all the components in the desired order.

                scroll_layout.setGravity( Gravity.CENTER_HORIZONTAL );
                scroll_layout.setGravity( Gravity.FILL_HORIZONTAL );
                scroll_layout.addView( this.txtTyped );
                scroll_layout.addView( letters_table_layout );
            }
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
    public void ShowAnswerPressed() {
        String user_input = getTypedString().toLowerCase();
        String word = reading_spelling_deck.getCardText( reading_spelling_deck.getCurrentCard() ).get(0).toLowerCase();

        // Do nothing if they haven't even typed anything yet.
        if( user_input.length() == 0 ) {
            return;
        }
        if( user_input.compareToIgnoreCase( word ) == 0 ) {
            // Answer is correct

            // Explosion of ticks celebration.
            // Fucking love this, major thanks to the Leonids library!
            int num_of_particles = 10000;
            int time_to_live = 10000;
            new ParticleSystem(this, num_of_particles, R.drawable.answer_tick_small, time_to_live)
                    .setSpeedRange(0.20f, 1.5f)
                    .setScaleRange(0.10f, 1f)
                    .oneShot(bt_show_answer, num_of_particles);


            if( is_spelling_hint_enabled ) {
                // Mark it as wrong, so they have to attempt it again without the spelling hint.
                reading_spelling_deck.nextQuestion(false, false);
            }
            else {
                // Since they didn't see a hint, mark it as correct.
                reading_spelling_deck.nextQuestion(true, false);
            }

            // Reset for the next card.
            is_spelling_hint_enabled = false;

        } else {
            // Answer is wrong.

            // Red crosses shooting out of the top of the screen.
            // Fucking love this, major thanks to the Leonids library!
            ParticleSystem particle = new ParticleSystem(this, 200, R.drawable.answer_cross_medium, 2500);
            particle.setSpeedModuleAndAngleRange(0.3f, 0.8f, 35, 145);
            particle.setRotationSpeed(180);
            particle.emit(findViewById(R.id.top_emitter), 30, 1000);

            // Mark the card as wrong and stay on the current card.
            // Set the hint to true so they can see the word.
            reading_spelling_deck.nextQuestion(false, true);
            is_spelling_hint_enabled = true;
        }
        NextQuestion();
        setTypedString("");
        displayUserInput();
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
}

