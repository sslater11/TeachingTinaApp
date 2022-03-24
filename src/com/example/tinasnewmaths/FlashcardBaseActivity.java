package com.example.tinasnewmaths;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ScaleDrawable;
import android.text.Html;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;

import libteachingtinadbmanager.CardDBManager;
import libteachingtinadbmanager.CardDBTagManager;
import libteachingtinadbmanager.FlashcardDeck;

public class FlashcardBaseActivity extends Activity {
    protected Button bt_show_answer;
    protected ImageButton bt_correct, bt_incorrect;
    public ImageView image_tick_or_cross;
    protected ArrayList<TextView> tvArr;
    protected ArrayList<ImageView> imgArr;
    protected ArrayList<MyAudioButton> audioArr;

    protected LinearLayout scroll_layout;

    protected ScrollView scroll_view; // Used to scroll to the bottom of the scroll layout. Scroll when the answer is shown.

    protected File db_file, db_config_file, db_media_folder;

    protected final int DEFAULT_FONT_SIZE = 25;
    protected int font_size = DEFAULT_FONT_SIZE;

    protected FlashcardDeck deck;


    // TODO:
    // I don't think I'll need this, so delete it later, after testing
	/*@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}*/

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
            FlashcardBaseActivity activity = FlashcardBaseActivity.this;

            Intent openSettingsActivity = new Intent(activity, SettingsActivity.class);
            openSettingsActivity.putExtra("deck_settings_file_path", deck.settings.getFilePath());
            activity.startActivity(openSettingsActivity);

            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // TODO:
        // remove this, and test the app to see how it responds.
        // e.g. will it continue if I press the back button, go to the menu, and open the deck again, because this isn't what I want it to do.
        finish();
    }


    public void AddContent(ArrayList<String> list) {
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
    }

    public void DisplayQuestion() {
        font_size = DEFAULT_FONT_SIZE;

        // Add the question to the screen.
        AddContent(deck.getQuestion());
    }

    public void DisplayAnswer() {
        font_size = DEFAULT_FONT_SIZE;

        // Add the horizontal line image to separate the question from the answer.
        imgArr.add(new ImageView(this));
        int img_index = imgArr.size() - 1;

        // Horizontal line - stretch horizontally.
        // Get the image and stretch it to the layout's width
        Drawable dr = getResources().getDrawable(R.drawable.horizontal_line);
        dr = new ScaleDrawable(dr, 0, scroll_layout.getWidth(), -1).getDrawable();
        // Set the image, and make the image scale to it.
        imgArr.get(img_index).setImageDrawable(dr);
        imgArr.get(img_index).setScaleType(ImageView.ScaleType.FIT_XY);
        // Add put the horizontal bar image on screen.
        scroll_layout.addView(imgArr.get(img_index));

        // Show the answer
        AddContent(deck.getAnswer());

        // Scroll down, just in case the answer goes off the screen.
        // It has to be done on a thread for some reason.
        scroll_view.post(new Runnable() {
            public void run() {
                scroll_view.scrollTo(0, scroll_view.getBottom());
            }
        });
    }

    public void ShowAnswerPressed() {
        // Hide the "Show Answer" button, and show the tick and cross buttons.
        bt_show_answer.setVisibility(View.GONE);
        bt_correct.setVisibility(View.VISIBLE);
        bt_incorrect.setVisibility(View.VISIBLE);
        DisplayAnswer();
    }

    public void EnableButtons() {
        bt_correct.setEnabled(true);
        bt_incorrect.setEnabled(true);
        bt_show_answer.setEnabled(true);
    }

    public void DisableButtons() {
        bt_correct.setEnabled(false);
        bt_incorrect.setEnabled(false);
        bt_show_answer.setEnabled(false);
    }

    public void CorrectPressed() {
        // They got it right.
        // Stop the user from tapping the buttons whilst it's doing stuff.
        DisableButtons();
        deck.nextQuestion(true, false);
        NextQuestion();
    }

    public void IncorrectPressed() {
        // They got it wrong.
        // Stop the user from tapping the buttons whilst it's doing stuff.
        DisableButtons();
        deck.nextQuestion(false, false);
        NextQuestion();
    }

    /**
     * Will clear the question and answer field, ready for the next card.
     */
    public void ResetQuestionField() {
        // Hide the tick and cross button, and show the "Show Answer" button.
        bt_show_answer.setVisibility(View.VISIBLE);
        bt_correct.setVisibility(View.GONE);
        bt_incorrect.setVisibility(View.GONE);

        // Clear the screen of the current question and answer

        scroll_layout.removeAllViews();
        tvArr = new ArrayList<TextView>();
        imgArr = new ArrayList<ImageView>();
        audioArr = new ArrayList<MyAudioButton>();
    }

    public void NextQuestion() {
        // Check if we've finished the deck.
        if (deck == null) {
            System.out.println("nulled it here");
        }
        if (deck.isLearnt() == false) {
            // It's not the last card, so continue.
            ResetQuestionField();
            DisplayQuestion();
            EnableButtons();
        } else {
            // The deck is empty.
            // Write the output and go to the finish screen.
            System.out.println("You finished studying!.");

            CardDBManager.writeDB(deck.getFilePath(), deck.getLearntDeck(), deck.settings);

            Intent openFinishedActivity = new Intent("com.example.tinasnewmaths.FINISHEDACTIVITY");
            FlashcardBaseActivity.this.startActivity(openFinishedActivity);
        }
    }
}
