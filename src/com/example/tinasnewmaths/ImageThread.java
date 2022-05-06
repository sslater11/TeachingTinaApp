/* © Copyright 2022, Simon Slater

This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, version 2 of the License.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

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
