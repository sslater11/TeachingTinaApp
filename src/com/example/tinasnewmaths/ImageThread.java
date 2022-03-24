package com.example.tinasnewmaths;

public class ImageThread extends Thread {
    protected boolean is_answer_correct;
    protected FlashcardBaseActivity activity;

    public ImageThread(FlashcardBaseActivity activity, boolean is_answer_correct) {
        this.activity = activity;
        this.is_answer_correct = is_answer_correct;
    }

    public void updateImage(final int resource) {
        activity.runOnUiThread(new Runnable() {
            public void run() {
                activity.image_tick_or_cross.setImageResource(resource);
            }
        });
    }

    public void run() {
        // Show the tick or cross.
        if (is_answer_correct) {
            updateImage(R.drawable.answer_tick);
        } else {
            updateImage(R.drawable.answer_cross);
        }

        // Display a blank image.
        try {
            sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            updateImage(R.drawable.answer_empty);
        }
    }
}
