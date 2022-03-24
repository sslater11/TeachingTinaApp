package com.example.tinasnewmaths;

import java.io.File;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

/**
 * @version 1.0
 *  this is the old version with reaction speeds which never got used.
 * @author simon
 *
 */

/* TODO:
 * move study_mode from Deck to DeckSettings.
 */

/* TODO:
 * My newest idea for reaction speed mode.
 * How do we know what speed the user needs the cards to be at?
 * Simple, we use their first reaction speed average to go by.
 * The user will be competing against themselves.
 * Maybe add about 500 milliseconds to the card, when writing the database.
 * The reason for this is because otherwise they might reach a peak speed that's just not possible to reach.
 * 
 * Also it should still use box numbers to find which cards to study.
 * The target reaction speed is just faster than the user's speed.
 * 
 * the game doesn't end until you have gotten an average speed for all answers.
 * 
 * Cards shouldn't be removed from this deck, if their average speed is set.
 * If a card's maximum speed has been reached, save it, but still use that card.
 * This way it stops there from being one last card in the group, which would mean they'd
 * see the same card for every answer, and would be able to get a low reaction speed. 
 * 
 * The settings should allow me to change the 'reaction speed buffer'.
 * The buffer is the amount of time to add on to the last reaction speed, to stop the speed from getting too fast.
 */




/*
 * TODO:
 * Deck Settings:
 * When changing from reaction speed, to study mode. Stay in the deck. any other setting, exit back to the main menu
 */



/*TODO: 
 * 1. Add the ability to add pictures for the question or answer.
 *         it should check if the question or answer string has a substring with "<image:(some_filename)>"
 *         Image files should be in a folder with the same name as the database file.
 *         the database file would NOT go inside this folder. Only it's images/audio would.
 * 2. Add the ability to play audio files.
 *         it should check if the question or answer string has a substring with
 *         "<audio:(some_filename)>"
 *         "<sound:(some_filename)>"
 */


// TODO make a timer function for setting the reaction time, and managing the 3 times.


/* INFO about this app, read this to understand it.
 * This app's SRS is based on the Leitner System.
 * 
 * ** The Leitner System **
 * Explanation:
 * 		The Leitner System was developed by Sebastian Leitner in the 1970s. 
 * 		It uses paper flashcards and a series of boxes.
 * 
 * 		Tomorrow you review the card again.
 * 		If you answer correctly, you put the flashcard in the second box,
 * 		which contains cards you review every 3 days.
 * 		If after 3 days you answer correctly again, it goes into the third box,
 * 		which gets reviewed after 5 days. And so on.
 * 
 * 		If at any time you answer incorrectly, the flashcard returns to the first box
 * 
 * ** My Modified Version of Leitner's System **
 * 		NNN = 5. Might make it smaller though
 * 		Each card will need to be answered correctly NNN times for it to go into the next 'box'.
 * 		The program won't end until we get all cards correct NNN times.
 * 		If you ever get a question wrong, the card will be put back into the first 'box'.
 * 
 * 		The 'box' will just be a number. The first 'box' will be number 1.
 * 		The 'box' number will be the number of days away from the date to review.
 *	 		E.g. if (box = 1), and (card's date = yesterday);  then review it.
 *			E.g. if (box = 2), and (card's date = yesterday);  then skip it.
 * 			E.g. if (box = 3), and (card's date = 3 days ago); then review it.
 * 		
 * 		Instead of just adding 1 to the number, for each day,
 * 		I have chosen to multiply the box number by a constant, which will give a slower increment than 1.
 * 		It will give a nice round curve for learning.
 * 		If we get the question correct, we will multiply the 'box' number by 1.4.
 * 		We will then ROUND the number, so it becomes an integer, when reading the database.
 * 		
 * 		Because the app will ask the same question NNN times, there's more chance that I will
 * 		get the question wrong.
 * 		If I get the question wrong once out of NNN tries, the card's box number will be reset.
 * 		This seems a bit unfair, because of human error, they might be stuck on the same card for DAYS.
 * 		So I should add a 'fails' counter, to keep track of how many times the user has failed.
 * 		If their fail count is greater than 2, or 3(on second thought, 3 might be a bit high),
 * 		then the card's box number will be reset.
 */

class RandomizedIndex {
	protected ArrayList<Integer> index_list = new ArrayList<Integer>();
	protected Deck deck;
	
	RandomizedIndex( Deck d ) {
		deck = d;
		ResetList();
	}
	
	public void ResetList() {
		index_list = new ArrayList<Integer>();
		
		// Loop through the deck, and add the index for each card as many times as needed.
		for( int i = 0; i < deck.getDeck().size(); i++ ) {
			int questions_left = getCardsRepeatCount(deck, i);
			
			if( questions_left >= 1 ) {
				// Add the index multiple times.
				for( int k = 0; k < questions_left; k++ ) {
					index_list.add(i);
				}
			} else {
				// If cards are NOT removable.
				if ( !deck.settings.areCardsRemovable() ) {
					// add just one index, to keep it in the list.
					index_list.add(i);
				}
			}
		}

		Collections.shuffle(index_list);
		System.out.println("index_list");
		System.out.println(index_list.toString());
	}
	
	
	public int getNext() { 
		if( index_list.size() > 1 ) {
			index_list.remove( 0 );
		} else {
			// The array is empty, or on it's last element, so reset the list.
			ResetList();
		}
		
		return getCurrent();
	}
	
	/**
	 * 
	 * @param deck
	 * @param cards_index
	 * @return Will return the number of questions needed to be asked for a SINGLE card (the current card).
	 * WARNING: It will return a zero OR negative number if the card has been learnt.
	 */
	public static int getCardsRepeatCount( Deck deck, int cards_index ) {
		ArrayList<Card> d = deck.getDeck();
		
		int success_limit = deck.settings.getSuccessLimit();
		int success_count = d.get(cards_index).getSuccessCount();
		int multiplier    = d.get(cards_index).getMultiplier();
		
		int questions_left = success_limit - success_count;
		questions_left = questions_left * multiplier;
		
		return questions_left;
	}
	/* TODO:
	 * Still need to make sure the randomized list will not repeat the same index too many times in a row.
	 * e.g. 1,1,1,1, 2,2,2, 3,3,3, 2,3
	 * 
	 * Maybe just loop through the list n times, and swap one or two of the notes randomly.
	 * Make sure I limit how many times the loop runs, otherwise it may end up being an infinite loop.
	 */
	public int getCurrent() {
		System.out.println(index_list.toString());
		return index_list.get(0);
	}
}

class DeckSettings {
	public    static final   float DEFAULT_CONFIG_VERSION = (float) 1.0;
	protected static final     int DEFAULT_FAIL_LIMIT    = 2; // The amount of times the user can get a question wrong, before it's box number resets to DEFAULT_BOX_NUM.
	protected static final     int DEFAULT_SUCCESS_LIMIT = 4; // Number of times to be asked the question successfully, before the card is learnt/finished.
	protected static final     int DEFAULT_CARD_LIMIT    = 3; // Maximum number of cards for the deck.
	protected static final boolean DEFAULT_ARE_CARDS_REMOVABLE  = false;
	protected static final boolean DEFAULT_IS_GROUP_MODE        = false;
	protected static final boolean DEFAULT_IS_GROUP_REVIEW_DATE = true; // Is the default group mode 'review date', or 'group name'.
	
	/* Deck GUI Types explained 
	 * Flashcards: Will show a question and an answer, will include audio and images.
	 * Maths:      Will give the user some sums to figure out. Will include addition, subtraction and multiplication.
	 * Keyboard:   Will ask the user to enter the answer using a keyboard. It will show them the correct answer, so they can see it spelt properly.
	 *             e.g. Will show tina a picture of a Bat, and ask her to spell it. 
	 */
	protected final static String[] DECK_GUI_TYPES = { "flashcard", "maths", "keyboard" }; 
	public final static int DECK_GUI_TYPE_FLASHCARDS       = 0;
	public final static int DECK_GUI_TYPE_MATHS            = 1;
	public final static int DECK_GUI_TYPE_KEYBOARD         = 2;

	protected int deck_gui_type;
	
	protected float config_version = DEFAULT_CONFIG_VERSION;
	
	
	protected int fail_limit    = DEFAULT_FAIL_LIMIT;
	protected int success_limit = DEFAULT_SUCCESS_LIMIT;
	protected int card_limit    = DEFAULT_CARD_LIMIT; // max number of cards for the deck.
	protected boolean are_cards_removable  = DEFAULT_ARE_CARDS_REMOVABLE; // Should we remove card from the deck if it's been learnt?
	protected boolean is_group_mode        = DEFAULT_IS_GROUP_MODE;
	protected boolean is_group_review_date = DEFAULT_IS_GROUP_REVIEW_DATE;
	
	protected File settings_file;

	/**
	 * Make a blank DeckSettings object
	 * The file parameter is just for getting the file's path.
	 * @param f
	 */
	DeckSettings(File f) {
		setFile(f);

		setDeckGuiType();
		setFailLimit();
		setSuccessLimit();
		setCardLimit();
		setAreCardsRemovable();
		setIsGroupMode();
	}
	
	public static boolean isDeckGuiTypeValid( String str ) {
		str = str.toLowerCase(Locale.US);
		for( int i = 0; i < DECK_GUI_TYPES.length; i++) {
			if( str.compareTo(DECK_GUI_TYPES[i]) == 0) {
				return true;
			}
		}
		return false;
	}
	public static boolean isDeckGuiTypeValid( int num ) {
		if( (num < DECK_GUI_TYPES.length) && (num >= 0) ) {
			return true;
		} else {
			return false;
		}
	}
	
	public static boolean isDeckGuiTypeMaths( String str ) {
		if( DECK_GUI_TYPES[DECK_GUI_TYPE_MATHS].compareTo(str) == 0 ) {
			return true;
		} else {
			return false;
		}
	}
	public static boolean isDeckGuiTypeMaths( int num ) {
		if( DECK_GUI_TYPE_MATHS == num ) {
			return true;
		} else {
			return false;
		}
	}
	
	public static boolean isDeckGuiTypeFlashcards( String str ) {
		if( DECK_GUI_TYPES[DECK_GUI_TYPE_FLASHCARDS].compareTo(str) == 0 ) {
			return true;
		} else {
			return false;
		}
	}
	public static boolean isDeckGuiTypeFlashcards( int num ) {
		if( DECK_GUI_TYPE_FLASHCARDS == num ) {
			return true;
		} else {
			return false;
		}
	}
	
	public static boolean isDeckGuiTypeKeyboard( String str ) {
		if( DECK_GUI_TYPES[DECK_GUI_TYPE_KEYBOARD].compareTo(str) == 0 ) {
			return true;
		} else {
			return false;
		}
	}
	public static boolean isDeckGuiTypeKeyboard( int num ) {
		if( DECK_GUI_TYPE_KEYBOARD == num ) {
			return true;
		} else {
			return false;
		}
	}
	
	public boolean isGroupMode() {
		return is_group_mode;
	}
	
	public boolean isGroupReviewDate() { 
		return is_group_review_date;
	}
	
	public boolean areCardsRemovable() {
		return are_cards_removable;
	}
	
	
	
	public void setDeckGuiType( int num ) {
		if( (num < DECK_GUI_TYPES.length) && (num >= 0) ) {
			deck_gui_type = num;
		} else {
			System.out.println("Erorr: Deck GUI Type doesn't exist: " + num);
			System.out.println("Setting Deck GUI Type as 'flashcards'.");
			deck_gui_type = DECK_GUI_TYPE_FLASHCARDS;
		}
	}
	public void setDeckGuiType() {
		// Set the Deck Gui Type to the default: 'flashcards'.
		setDeckGuiType(DECK_GUI_TYPE_FLASHCARDS);
	}
	
	public void setDeckGuiType( String str ) {
		// This line is just in case the loop doesn't find a match.
		deck_gui_type = -1;
		
		str = str.toLowerCase(Locale.US);
		for( int i = 0; i < DECK_GUI_TYPES.length; i++ ) {
			if( str.compareTo(DECK_GUI_TYPES[i]) == 0) {
				deck_gui_type = i; 
				break;
			}
		}
		
		if( deck_gui_type == -1 ) {
			System.out.println("Error: Deck GUI Type doesn't exist: " + str);
			System.out.println("Setting Deck GUI Type as 'flashcards'.");
			setDeckGuiType(DECK_GUI_TYPE_FLASHCARDS);
		}
	}
	
	public void setFailLimit( int num ) {
		if( num >= 1 ) {
			fail_limit = num;
		} else {
			System.out.println("Error: fail limit set to default, number supplied was less than 1.");
			fail_limit = DEFAULT_FAIL_LIMIT;
		}
	}
	public void setFailLimit() {
		setFailLimit(DEFAULT_FAIL_LIMIT);
	}
	
	public void setSuccessLimit( int num ) {
		if( num >= 1 ) {
			success_limit = num;
		} else {
			System.out.println("Error: success limit set to default, number supplied was less than 1.");
			setSuccessLimit( DEFAULT_SUCCESS_LIMIT );
		}
	}
	public void setSuccessLimit() {
		setSuccessLimit( DEFAULT_SUCCESS_LIMIT );
	}

	public void setCardLimit( int num ) {
		if( num >= 1 ) {
			card_limit = num;
		} else {
			System.out.println("Error: card limit set to default, number supplied was less than 1.");
			card_limit = DEFAULT_CARD_LIMIT;
		}
	}
	public void setCardLimit() {
		setCardLimit(DEFAULT_CARD_LIMIT);
	}

	public void setFile(File s) {
		settings_file = s;
	}
	
	public void setFilePath(String s) {
		settings_file = new File(s);
	}
	
	public void setAreCardsRemovable( boolean b ) {
		are_cards_removable = b;
	}
	
	
	public void setAreCardsRemovable() {
		setAreCardsRemovable(DEFAULT_ARE_CARDS_REMOVABLE);
	}
	
	public void setIsGroupMode( boolean b ) {
		is_group_mode = b;
	}
	
	public void setIsGroupMode() {
		setIsGroupMode(DEFAULT_IS_GROUP_MODE);
	}
	
	public void setIsGroupReviewDate( boolean b ) {
		is_group_review_date = b;
	}
	
	public void setIsGroupReviewDate() {
		setIsGroupReviewDate(DEFAULT_IS_GROUP_REVIEW_DATE);
	}
	
	
	public int getDeckGuiType() {
		return deck_gui_type;
	}

	public int getFailLimit() {
		return fail_limit;
	}
	
	public int getSuccessLimit () {
		return success_limit;
	}
	
	public int getCardLimit() {
		return card_limit;
	}
	
	public String getConfigVersionString() {
		String str = "" + config_version;
		return str;
	}
	
	public String getDeckGuiTypeString() {
		return DECK_GUI_TYPES[getDeckGuiType()];
	}

	public String getFailLimitString() {
		String str = "" + fail_limit;
		return str;
	}
	
	public String getSuccessLimitString() {
		String str = "" + success_limit;
		return str;
	}
	
	public String getCardLimitString() {
		String str = "" + card_limit;
		return str;
	}
	

	public String getFilePath() {
		return settings_file.getAbsolutePath();
	}
	public File getFile() {
		return settings_file;
	}

}


class CardsGroup {
	public static final int INDEX_TAG                  = 0; // This is the tag at the start of the line that indicates it will be a group line.
	public static final int INDEX_NAME                 = 1;
	public static final int INDEX_REVIEW_DATE          = 2;
	public static final int INDEX_BOX_NUM              = 3;
	public static final int INDEX_REVIEW_TIME          = 4;
	public static final int INDEX_DAILY_REVIEW_COUNT   = 5;
	public static final int INDEX_NUM_ATTRIBUTES       = 6;
	
	public static final String DEFAULT_GROUP_TAG = "Group";

	protected boolean has_been_updated = false; // Check if the update() method has been run yet.
	
	protected String group_name;
	protected String review_date;
	protected String review_time;
	protected float  box_num;
	protected int    daily_review_count;
	
	protected String orig_review_date;
	protected String orig_review_time;
	protected float  orig_box_num; // -1 for uninitialised check in setter method.
	protected int    orig_daily_review_count; // -1 for uninitialised check in setter method.
	public DeckSettings settings;


	/**
	 * Make a blank group.
	 * @param settings
	 */
	CardsGroup(DeckSettings s) {
		// Make a blank card group, so the user can set everything
		settings = s;
		
		setBoxNum();
		setName("Blank Group");
		setReviewDate();
		setReviewTime();
		setDailyReviewCount();
		
	}

	
	/**
	 * 
	 * @param settings
	 * @param review_date
	 * @param box_num
	 * @param review_time
	 * @param daily_review_count
	 */
	//CardsGroup(DeckSettings s, String review_date, float box_num, String review_time, int daily_review_count) {
	//	settings = s;
	//}
	/**
	 * 
	 * @param arr[]
	 * @param settings
	 */
	CardsGroup ( String arr[], DeckSettings s ) {
		settings = s;
		if ( (arr.length - INDEX_NUM_ATTRIBUTES) >= 0 ) {
			setOrigReviewDate      ( arr[INDEX_REVIEW_DATE]           );
			setOrigBoxNum          ( arr[INDEX_BOX_NUM]               );
			setOrigReviewTime      ( arr[INDEX_REVIEW_TIME]           );
			setOrigDailyReviewCount( arr[INDEX_DAILY_REVIEW_COUNT]    );

			setName            ( arr[INDEX_NAME]                  );
			setReviewDate      ( arr[INDEX_REVIEW_DATE]           );
			setBoxNum          ( arr[INDEX_BOX_NUM]               );
			setReviewTime      ( arr[INDEX_REVIEW_TIME]           );
			setDailyReviewCount( arr[INDEX_DAILY_REVIEW_COUNT]    );
			
			// If the review date is not today, then reset the card's daily_review_count and review_time.
			if ( MyDate.compare( getReviewDate(), 0 ) < 0 ) {
				setDailyReviewCount();
				setReviewTime();
			}
		} else {
			// Whoopsie, probably a bad database file.

			// No longer needed, because the variables are not final anymore
			// Just initialise these final variables to anything, to keep the compiler happy.
			//orig_review_date        = null;
			//orig_review_time        = null;
			//orig_box_num            = -1;
			//orig_daily_review_count =  0;

			System.out.println("Error in class 'CardGroup', at constructor. Array passed has too few elements.");
			System.out.println("Array must have at least "+ INDEX_NUM_ATTRIBUTES + " elements");
			System.out.println("Array only has " + arr.length + " elements.");
			System.exit(600);
		}
	}

	@Override
	public String toString() {
		String line;
		line = DEFAULT_GROUP_TAG;
		line += "\t" + getName();
		line += "\t" + getReviewDate();
		line += "\t" + getBoxNum();
		line += "\t" + getReviewTime();
		line += "\t" + getDailyReviewCount();
		
		return line;
	}


	public static boolean isReviewNeeded(DeckSettings settings, String date, float box_num, String time, int daily_review_count) {
		return Card.isReviewNeeded( settings, date, box_num, time, daily_review_count );
	}
	
	public boolean isReviewNeeded() {
		return isReviewNeeded( settings, getReviewDate(), getBoxNum(), getReviewTime(), getDailyReviewCount() );
	}
	
	// TODO: old code for my daily review. Keep for future reference.
	/*
	public boolean isReviewIntervalOver() {
		return Card.isReviewIntervalOver( settings, getReviewTime() );
	}
	public static boolean isReviewIntervalOver(DeckSettings settings, String time) {
		return Card.isReviewIntervalOver( settings, time );
	}*/

	
	
	public static boolean isLegalTag( String str ) {
		String tag = DEFAULT_GROUP_TAG;
		tag = tag.toLowerCase(Locale.US);
		tag = tag.trim();
		
		str = str.toLowerCase(Locale.US);
		str = str.trim();
		 if ( str.compareTo(tag) == 0 ) {
			 return true;
		 } else {
			 return false;
		 }

	}
	
	public static boolean isLegalName( String str ) {
		 if ( str.length() > 1 ) {
			 return true;
		 } else {
			 return false;
		 }

	}

	public static boolean isLegalReviewDate(String str) {
		return Card.isLegalReviewDate(str);
	}
	
	public static boolean isLegalReviewTime(String str) {
		return Card.isLegalReviewTime(str);
	}
	
	public static boolean isLegalBoxNum(float box) {
		return Card.isLegalBoxNum( box );
	}
	public static boolean isLegalBoxNum(String box) {
		return Card.isLegalBoxNum( box );
	}
	
	public static boolean isLegalDailyReviewCount( int num ) {
		return Card.isLegalDailyReviewCount( num );
	}
	public static boolean isLegalDailyReviewCount( String str ) {
		return Card.isLegalDailyReviewCount( str );
	}

	public boolean hasBeenReviewedToday() {
		return hasBeenReviewedToday( settings, getReviewDate(), getBoxNum(), getReviewTime(), getDailyReviewCount() );
	}
	
	public static boolean hasBeenReviewedToday(DeckSettings settings, String date, float box_num, String time, int daily_review_count) {
		return Card.hasBeenReviewedToday( settings, date, box_num, time, daily_review_count );
	}


	public boolean hasBeenUpdated() {
		return has_been_updated;
	}

	public String getName() {
		return group_name;
	}



	public String getReviewDate() {
		return review_date;
	}



	public String getReviewTime() {
		return review_time;
	}



	public float getBoxNum() {
		return box_num;
	}



	public int getDailyReviewCount() {
		return daily_review_count;
	}

	public int getOrigDailyReviewCount() {
		return orig_daily_review_count;
	}


	public String getOrigReviewDate() {
		return orig_review_date;
	}
	
	public String getOrigReviewTime() {
		return orig_review_time;
	}

	public float getOrigBoxNum() {
		return orig_box_num;
	}



	public String getOrigBoxNumString() {
		return "" + orig_box_num;
	}



	public void setName(String str) {
		group_name = str;
	}



	public void setReviewDate(String str) {
		if( MyDate.isLegalDate( str ) ) {
			review_date = str;
		} else {
			review_date = Card.DEFAULT_REVIEW_DATE;
		}
	}
	public void setReviewDate(Date new_date) {
		setReviewDate( MyDate.toString(new_date));
	}
	public void setReviewDate() {
		setReviewDate( MyDate.today() );
	}



	public void setReviewTime(String new_time) {
		if( MyDate.isLegalTime(new_time)) {
			review_time = new_time;
		} else {
			review_time = Card.DEFAULT_REVIEW_TIME;
		}
	}
	public void setReviewTime(Date new_time) {
		setReviewTime( MyDate.timeToString(new_time) );
	}
	public void setReviewTime() {
		setReviewTime( Card.DEFAULT_REVIEW_TIME );
	}



	public void setDailyReviewCount( int count ) {
		if( count < 0 ) {
			daily_review_count = Card.DEFAULT_DAILY_REVIEW_COUNT;
		} else {
			daily_review_count = count;
		}
	}
	public void setDailyReviewCount( String str ) {
		setDailyReviewCount( Integer.valueOf(str));
	}
	public void setDailyReviewCount() {
		setDailyReviewCount(Card.DEFAULT_DAILY_REVIEW_COUNT);
	}



	public void setBoxNum(float num) {
		if( num < 0 ) {
			// It's probably -1, uninitialised.
			box_num = Card.DEFAULT_BOX_NUM;
		} else {
			box_num = num;
		}
	}
	public void setBoxNum(String box) {
		setBoxNum( Float.valueOf(box) );
	}
	public void setBoxNum() {
		setBoxNum( Card.DEFAULT_BOX_NUM );
	}
	
	
	
	public void setOrigReviewDate(String str) {
		orig_review_date = str;
	}
	public void setOrigReviewDate(Date new_date) {
		setOrigReviewDate( MyDate.toString(new_date));
	}

	
	public void setOrigReviewTime(String new_time) {
		orig_review_time = new_time;
	}
	public void setOrigReviewTime(Date new_time) {
		setOrigReviewTime( MyDate.timeToString(new_time) );
	}

	
	public void setOrigBoxNum(float num) {
		orig_box_num = num;
	}
	public void setOrigBoxNum(String box) {
		setOrigBoxNum( Float.valueOf(box) );
	}

	
	public void setOrigDailyReviewCount( int count ) {
		orig_daily_review_count = count;
	}
	public void setOrigDailyReviewCount( String str ) {
		setOrigDailyReviewCount( Integer.valueOf(str));
	}

	
	
	public boolean isName( String str ) {
		if( getName().compareTo(str) == 0) {
			return true;
		} else {
			return false;
		}
	}


	public boolean compareOrigTo(CardsGroup other_group) {
		String str_date1 =             getOrigReviewDate();
		String str_date2 = other_group.getOrigReviewDate();
		
		String str_time1 =             getOrigReviewTime();
		String str_time2 = other_group.getOrigReviewTime();
		
		String str_name1 =             getName();
		String str_name2 = other_group.getName();
		if( str_date1.compareTo(str_date2) != 0 ) {
			System.out.println( getOrigReviewDate() );
			System.out.println( other_group.getOrigReviewDate() );
			return false;
			
		} else if( str_time1.compareTo(str_time2) != 0 ) {
			return false;

		} else if( str_name1.compareTo(str_name2) != 0 ) {
			return false;
	
		} else if( getOrigBoxNum() != other_group.getOrigBoxNum() ) {
			return false;
			
		} else if( getOrigDailyReviewCount() != other_group.getOrigDailyReviewCount() ) {
			return false;
			
		} else {
			// Both card's data matched up.
			return true;
		}
		
	}


	/**
	 * Will update the card's values for the:
	 *     Review Date
	 *     Review Time
	 *     Box Number
	 *     Daily Review Count
	 */
	public void update() {
		if( has_been_updated == false ) {
			has_been_updated = true;
			
			// Only update if it's group mode, AND the group was chosen by review date, not by group name.
			if( settings.isGroupMode() ) {
				if( settings.isGroupReviewDate() ) {
					setReviewDate ( MyDate.toString( MyDate.today() ) );
					setReviewTime ( MyDate.timeToString( MyDate.currentTime() ) );
					setDailyReviewCount( getDailyReviewCount() + 1 );
					
					// Only increment the box_num once a day.
					if( getDailyReviewCount() ==  1 ) {
						setBoxNum( getBoxNum() * Card.BOX_NUM_MULTIPLIER );
					}
				} else {
					/* Group was chosen by it's name.
					 * I don't think we need to do anything, since the group was chosen by name.
					 * This mode doesn't write to a database, because it'll mess up the box number algorithm.
					 */
				}
			}
		}
	}
}

class Card {
	// These constants are for accessing the 'contents' array.
	public static final int INDEX_REVIEW_DATE           = 0;
	public static final int INDEX_BOX_NUM               = 1;
	public static final int INDEX_REVIEW_TIME           = 2;
	public static final int INDEX_DAILY_REVIEW_COUNT    = 3;
	public static final int INDEX_USERS_REACTION_SPEED  = 4;
	public static final int INDEX_TARGET_REACTION_SPEED = 5;
	public static final int INDEX_NUM_ATTRIBUTES        = 6;
	
	public static final float BOX_NUM_MULTIPLIER = (float) 1.4;
	
	protected final int NUM_MULTIPLIER = 1; // Number of times the card is added to the deck's card list.
	
	public static final int    DEFAULT_BOX_NUM = 1; /* when update() is run, all box numbers will be
	                                             * multiplied by BOX_NUM_MULTIPILER, so the
	                                             * default box number is really (1 * 1.4) = 1.4
	                                             */
	public static final String DEFAULT_REVIEW_DATE = "01/01/2014";
	public static final String DEFAULT_REVIEW_TIME = "00:00:00"; // Set the default time to midnight, since it's the earliest time in the day
	public static final int    DEFAULT_DAILY_REVIEW_COUNT = 0; // This will be incremented by 1 on update(), so it's really equal to 1.
	public static final int    DEFAULT_USERS_REACT_SPEED  = -1;
	public static final int    DEFAULT_TARGET_REACT_SPEED = -1;
	
	protected String review_date;
	protected String review_time;
	protected float box_num;
	protected int daily_review_count;
	protected int users_react_speed;
	protected int target_react_speed;
	protected int avg_react_speed[] = new int[3]; // 3 is just the default number for just now.
	protected int index_avg_react_speed = 0;
	protected int multiplier = NUM_MULTIPLIER; // Number of times the card is added to the deck's card list, used to make it stand out more.
	
	// Store the data found in the database.
	// Then it can be used later on to compare the reviewed cards
	protected String orig_review_date;
	protected String orig_review_time;
	protected float orig_box_num;
	protected int orig_users_react_speed;
	protected int orig_target_react_speed;
	protected int orig_daily_review_count;
	
	protected String contents[]; // The question and answer the card holds.
	protected int fails     = 0;
	protected int successes = 0;

	protected boolean has_been_updated = false; // Check if the update() method has been run yet.
	
	protected DeckSettings settings;
	protected CardsGroup group;
	
	/**
	 * Making a blank card, which the user has to change everything.
	 */
	Card(DeckSettings s, CardsGroup g) {
		settings = s;
		group = g;
		review_date             = null;
		review_time             = null;
		orig_review_date        = null;
		orig_review_time        = null;

		box_num                 = -1;
		users_react_speed       = -1;
		target_react_speed      = -1;
		orig_box_num            = -1;
		orig_users_react_speed  = -1;
		orig_target_react_speed = -1;
		
		daily_review_count      = -1;
		orig_daily_review_count = -1;
	}
	
	Card ( String line, String delimiter, DeckSettings s, CardsGroup g ) {
		this(line.split(delimiter), s, g);
	}
	
	Card ( String line, DeckSettings s, CardsGroup g ) {
		this(line, "\t", s, g);
	}
	
	Card ( String arr[], DeckSettings s, CardsGroup g) {
		settings = s;
		group = g;
		if ( (arr.length - INDEX_NUM_ATTRIBUTES) > 0 ) {
			setOrigReviewDate      ( arr[INDEX_REVIEW_DATE]           );
			setOrigReviewTime      ( arr[INDEX_REVIEW_TIME]           );
			setOrigBoxNum          ( arr[INDEX_BOX_NUM]               );
			setOrigUsersReactSpeed ( arr[INDEX_USERS_REACTION_SPEED ] );
			setOrigTargetReactSpeed( arr[INDEX_TARGET_REACTION_SPEED] );
			setOrigDailyReviewCount( arr[INDEX_DAILY_REVIEW_COUNT]    );
			
			setReviewDate      ( arr[INDEX_REVIEW_DATE]           );
			setReviewTime      ( arr[INDEX_REVIEW_TIME]           );
			setBoxNum          ( arr[INDEX_BOX_NUM]               );
			setUsersReactSpeed ( arr[INDEX_USERS_REACTION_SPEED ] );
			setTargetReactSpeed( arr[INDEX_TARGET_REACTION_SPEED] );
			setDailyReviewCount( arr[INDEX_DAILY_REVIEW_COUNT]    );
			
			// If the review date is not today, then reset the card's daily_review_count and review_time.
			if ( MyDate.compare( getReviewDate(), 0 ) < 0 ) {
				setDailyReviewCount();
				setReviewTime();
			}

			
			contents = new String[(arr.length - INDEX_NUM_ATTRIBUTES)];
			// Put the rest of the arr's contents into the 'contents' array.
		      for (int i = INDEX_NUM_ATTRIBUTES, k = 0;  i < arr.length;  i++, k++ ) {
		      	contents[k] = arr[i];
		      }
		} else {
			// Whoopsie, probably a bad database file.

			// No longer needed, because these variables are no longer final.
			// Just initialise these final variables to anything, to keep the compiler happy.
			//orig_review_date        = null;
			//orig_review_time        = null;
			//orig_box_num            = -1;
			//orig_users_react_speed  = -1;
			//orig_target_react_speed = -1;
			//orig_daily_review_count = -1;

			System.out.println("Error in class 'Card', at constructor. Array passed has too few elements.");
			System.out.println("Array must have at least "+ INDEX_NUM_ATTRIBUTES + " elements");
			System.out.println("Array only has " + arr.length + " elements.");
			System.exit(600);
		}
	}

	public static boolean isLegalReviewDate(String str) {
		return MyDate.isLegalDate(str);
	}
	
	public static boolean isLegalReviewTime(String str) {
		return MyDate.isLegalTime(str);
	}

	
	public static boolean isLegalBoxNum(float box) {
		if( box >= 1 ) {
			return true;
		} else {
			return false;
		}
	}
	public static boolean isLegalBoxNum( String str ) {
		 return isLegalBoxNum ( Float.valueOf( str )    );

	}
	
	public static boolean isLegalTargetReactSpeed(int speed) {
		if( speed >= -1 ) {
			return true;
		} else {
			return false;
		}
	}
	public static boolean isLegalTargetReactSpeed( String str ) {
		 return isLegalTargetReactSpeed( Integer.valueOf( str ) );
	}
	
	public static boolean isLegalUsersReactSpeed(int speed) {
		if( speed >= -1 ) {
			return true;
		} else {
			return false;
		}
	}
	public static boolean isLegalUsersReactSpeed( String str ) {
		 return isLegalUsersReactSpeed( Integer.valueOf( str ) );
	}
	
	
	public static boolean isLegalDailyReviewCount(int num) {
		if( num >= -1 ) {
			return true;
		} else {
			return false;
		}
	}
	public static boolean isLegalDailyReviewCount( String str ) {
		 return isLegalDailyReviewCount( Integer.valueOf( str ) );
	}

	public String getOrigReviewDate() {
		return orig_review_date;
	}
	
	public String getOrigReviewTime() {
		return orig_review_time;
	}
	
	public float getOrigBoxNum() {
		return orig_box_num;
	}
	
	public int getOrigDailyReviewCount() {
		return orig_daily_review_count;
	}
	

	public int getOrigUsersReactSpeed() {
		return orig_users_react_speed;
	}
	
	public int getOrigTargetReactSpeed() {
		return orig_target_react_speed;
	}
	
	public String getReviewDate() {
		return review_date;
	}
	
	public String getReviewTime() {
		return review_time;
	}
	
	public float getBoxNum() {
		return box_num;
	}

	public int getUsersReactSpeed() {
		return users_react_speed;
	}
	
	public int getTargetReactSpeed() {
		return target_react_speed;
	}
	
	public int getDailyReviewCount() {
		return daily_review_count;
	}
	
	public int getSuccessCount() {
		return successes;
	}
	
	public int getMultiplier() {
		return multiplier;
	}


	public void newReactSpeed( int speed ) {
		// Go to the next element, or just loop around to element 0.
		if ( index_avg_react_speed < (avg_react_speed.length - 1)) {
			index_avg_react_speed++;
		} else {
			index_avg_react_speed = 0;			
		}
		
		avg_react_speed[index_avg_react_speed] = speed;
	}
	
	public int getAverageReactSpeed() {
		//TODO: Need to check if the user has been asked at least a few times.
		//      Maybe just set the array elements to a really high number for the reaction speed.
		//      Return -1 if the user hasn't been asked enough questions.
		
		int total_time = 0;
		for( int i = 0; i < avg_react_speed.length; i++ ) {
			total_time += avg_react_speed[i];
		}
		
		int average = total_time / avg_react_speed.length;
		
		return average;
	}
	public void setReviewDate(String str) {
		if( MyDate.isLegalDate( str ) ) {
			review_date = str;
		} else {
			review_date = DEFAULT_REVIEW_DATE;
		}
	}
	public void setReviewDate(Date new_date) {
		setReviewDate( MyDate.toString(new_date) );
	}
	public void setReviewDate() {
		setReviewDate( DEFAULT_REVIEW_DATE );
	}
	
	
	public void setReviewTime(String new_time) {
		if( MyDate.isLegalTime(new_time)) {
			review_time = new_time;
		} else {
			review_time = DEFAULT_REVIEW_TIME;
		}
	}
	public void setReviewTime(Date new_time) {
		setReviewTime( MyDate.timeToString(new_time) );
	}
	public void setReviewTime() {
		setReviewTime( DEFAULT_REVIEW_TIME );
	}

	
	public void setBoxNum(float num) {
		if( num < 0 ) {
			// It's probably -1, uninitialised.
			box_num = DEFAULT_BOX_NUM;
		} else {
			box_num = num;
		}
	}
	public void setBoxNum(String box) {
		setBoxNum( Float.valueOf(box) );
	}
	
	
	public void setUsersReactSpeed(int speed) {
		if( speed < 0 ) {
			users_react_speed = DEFAULT_USERS_REACT_SPEED;
		} else {
			users_react_speed = speed;
		}
	}
	public void setUsersReactSpeed(String speed) {
		setUsersReactSpeed( Integer.valueOf(speed) );
	}
	
	
	public void setTargetReactSpeed(int speed) {
		if( speed < 0 ) {
			target_react_speed = DEFAULT_TARGET_REACT_SPEED;
		} else {
			target_react_speed = speed;
		}
	}
	public void setTargetReactSpeed(String speed) {
		setTargetReactSpeed( Integer.valueOf(speed) );
	}
	
	public void setMultiplier(int multiply ) {
		if( multiplier >= 1 ) {	
			multiplier  = multiply;
		} else {
			System.out.println("error in setMultiplier(). Number was less than 1, so setting it to 1");
			multiplier = 1;
		}
	}
	
	public void setDailyReviewCount( int num ) {
		if( num >= 0 ) {
			daily_review_count = num;
		} else {
			System.out.println("there was a problew with setDailyReviewCount( num ) to 0");
			daily_review_count = 0;
		}
	}
	public void setDailyReviewCount( String str ) {
		setDailyReviewCount( Integer.valueOf(str) );
	}
	public void setDailyReviewCount() {
		// Default to 0.
		setDailyReviewCount( DEFAULT_DAILY_REVIEW_COUNT );
	}
	public void setOrigReviewDate(String str) {
		orig_review_date = str;
	}
	public void setOrigReviewDate(Date new_date) {
		setOrigReviewDate( MyDate.toString(new_date));
	}

	
	public void setOrigReviewTime(String new_time) {
		orig_review_time = new_time;
	}
	public void setOrigReviewTime(Date new_time) {
		setOrigReviewTime( MyDate.timeToString(new_time) );
	}

	
	public void setOrigBoxNum(float num) {
		orig_box_num = num;
	}
	public void setOrigBoxNum(String box) {
		setOrigBoxNum( Float.valueOf(box) );
	}
	
	
	public void setOrigUsersReactSpeed( int count ) {
		orig_users_react_speed = count;
	}
	public void setOrigUsersReactSpeed( String str ) {
		setOrigUsersReactSpeed( Integer.valueOf(str));
	}
	
	
	public void setOrigTargetReactSpeed( int count ) {
		orig_target_react_speed = count;
	}
	public void setOrigTargetReactSpeed( String str ) {
		setOrigTargetReactSpeed( Integer.valueOf(str));
	}
	
	public void setOrigDailyReviewCount( int count ) {
		orig_daily_review_count = count;
	}
	public void setOrigDailyReviewCount( String str ) {
		setOrigDailyReviewCount( Integer.valueOf(str));
	}

	public void failed() {
		fails++;
		if( fails >= settings.getFailLimit() ) {
			setBoxNum(DEFAULT_BOX_NUM);
			// Only update the group's box number if it's a group mode, AND it's got it's group by review date, not by group name.
			if( settings.isGroupMode() && settings.isGroupReviewDate() ) {
				group.setBoxNum(DEFAULT_BOX_NUM);
			}
		}
		successes = 0;
	}
	
	public void success() {
		successes++;
	}
	
	public boolean isLearnt() {
		if ( successes >= settings.getSuccessLimit() ) {
			return true;
		} else {
			return false;
		}

	}
	
	public boolean hasBeenUpdated() {
		return has_been_updated;
	}
	
	public String getContent(int idx) {
		if ( idx < contents.length ) {
			return contents[idx];
		} else {
			return "Error in getContent(). Array element doesn't exist in the card's 'contents' array";
		}
	}
	
	public String toString() {
		String line;
		line  =        getReviewDate();
		line += "\t" + getBoxNum();
		line += "\t" + getReviewTime();
		line += "\t" + getDailyReviewCount();
		line += "\t" + getUsersReactSpeed();
		line += "\t" + getTargetReactSpeed();
		
		for( int i = 0; i < contents.length; i++ ) {
			line += "\t" + contents[i];
		}
		
		return line;
	}
	// TODO: old code for my daily review, keep for future reference
	/*
	public boolean isReviewIntervalOver() {
		return isReviewIntervalOver( settings, getReviewTime() );
	}
	public static boolean isReviewIntervalOver(DeckSettings settings, String time) {
		int mins = settings.getReviewInterval();
		// Check the review interval, to see if enough time has passed since the last review.
		if( MyDate.compareTime(time, mins) <= 0) {
			return true;
		} else {
			return false;
		}
		
	}*/

	public boolean isReviewNeeded() {
		return isReviewNeeded( settings, getReviewDate(), getBoxNum(), getReviewTime(), getDailyReviewCount() );
	}
	
	public static boolean isReviewNeeded(DeckSettings settings, String date, float box_num, String time, int daily_review_count) {
		/* TODO:
		 * Add a way to figure out if the review is needed for reaction speed mode.
		 * It's currently just for question limit mode.
		 */
		int days = Math.round( box_num );
		// TODO: old code for my daily review. Keep for future reference.
		//int mins = settings.getReviewInterval();
	
		if( MyDate.compare(date, 0) == 0 ) {
			// TODO: This commented out code is the old code for checking if a review is needed. keep it for future reference.
			/*
			// The card is for today.
			// Check if the review count and interval allow a review.
			if( daily_review_count < settings.getDailyReviewLimit() ) {
				// Review count is lower than the limit, so it might need reviewing.
				
				// Check the review interval, to see if enough time has passed since the last review.
				if( isReviewIntervalOver(settings, time) ) {
					return true;
				}
			}
			*/
			// All the other checks failed, so return false.
			return false;
		} else if ( MyDate.compare(date, days) <= 0 ) {
			System.out.println( MyDate.compare(date, days) + "      " + date + "   " + box_num + "   " + time + "   " +  daily_review_count);
			// Card's box number and date indicates the card is old, and will need a review
			return true;
		} else {
			// The card is to be reviewed in the future.
			return false;
		}
	}
	
	public boolean hasBeenReviewedToday() {
		return hasBeenReviewedToday( settings, getReviewDate(), getBoxNum(), getReviewTime(), getDailyReviewCount() );
	}
	
	public static boolean hasBeenReviewedToday(DeckSettings settings, String date, float box_num, String time, int daily_review_count) {
		if( MyDate.compare(date, 0) == 0 ) {
			// The card was last reviewed today.
			return true;
		} else {
			// The card was not reviewed today.
			return false;
		}
	}
	//public boolean isSameAs( Card other_card ) {
	/**
	 * Will compare the original values to that of another card's original values.
	 * @param other_card of type 'Card'
	 * @return boolean as true, if cards have the same content.
	 */
	public boolean compareOrigTo( Card other_card ) {
		
		String str_date1 =            getOrigReviewDate();
		String str_date2 = other_card.getOrigReviewDate();
		
		String str_time1 =             getOrigReviewTime();
		String str_time2 = other_card.getOrigReviewTime();
		
		if( str_date1.compareTo(str_date2) != 0 ) {
			return false;

		} else if( str_time1.compareTo(str_time2) != 0 ) {
			return false;

		} else if( getOrigBoxNum() != other_card.getOrigBoxNum() ) {
			return false;

		} else if( getOrigDailyReviewCount() != other_card.getOrigDailyReviewCount() ) {
			return false;

		} else {
			// Both card's data matches up so far.
			// Now check the cards contents against each other.
			for( int i = 0; i < contents.length; i++ ) {
				String str1 =            getContent(i);
				String str2 = other_card.getContent(i);
				if ( str1.compareTo(str2) != 0) {
					return false;
				}
			}

			return true;
		}
	}
	
	/**
	 * Will update the card's values for the:
	 *     Review Date
	 *     Review Time
	 *     Box Number
	 *     Daily Review Count
	 *     Users Reaction Speed
	 *     Target Reaction Speed
	 */
	public void update() {
		group.update();

		if( has_been_updated == false ) {
			has_been_updated = true;
			
			setReviewDate( MyDate.toString( MyDate.today() ) );
			setReviewTime( MyDate.timeToString( MyDate.currentTime() ) );
			setUsersReactSpeed ( getAverageReactSpeed() );
			setTargetReactSpeed( getAverageReactSpeed() );
			setDailyReviewCount( getDailyReviewCount() + 1 );
			
			// Only update if it's group mode, AND the group was chosen by review date, not by group name.
			if( settings.isGroupMode() ) {
				if( settings.isGroupReviewDate() ) {
					setUsersReactSpeed( getAverageReactSpeed() );
					
					// Only update the card's box number, if it's <= group's box number.
					// This stops the user from completing the deck, and making the card's box number get too big.
					// They would be able to practice daily, and get one card in the group wrong, resetting the groups' box number, and incrementing the others.
					// Repeated daily to a ridiculously high box number.
					if( getBoxNum() < group.getBoxNum() ) {
						// Only increment the box_num once a day.
						if( getDailyReviewCount() ==  1 ) {
							setBoxNum( getBoxNum() * BOX_NUM_MULTIPLIER );
						}
					}
				} else {
					/* Group was chosen by it's name.
					 * I don't think we need to do anything, since the group was chosen by name.
					 * This mode doesn't write to a database, because it'll mess up the box number algorithm.
					 */
				}
			} else {
				// Only increment the box_num once a day.
				if( getDailyReviewCount() ==  1 ) {
					setBoxNum( getBoxNum() * BOX_NUM_MULTIPLIER );
				}
			}
		}
	}
}

class MyDate {
	protected final static String STR_DATE_FORMAT = "dd/MM/yyyy";
	protected final static String STR_TIME_FORMAT = "HH:mm:ss";

	/**
	 * 
	 * @param d
	 * @param cards_box_num
	 * @return -1 if before today, 0 is today, 1 if after today
	 */
	public static int compare(String d, int cards_box_num){
		Date cards_date;
		try {
			cards_date = fromString(d);
		} catch (ParseException e) {
			e.printStackTrace();
			cards_date = new Date();
		}
		Date todays_date = today();
		
		// Add the box number to the day as days.
		// This gives an easy review algorithm.
		cards_date = addDays(cards_date, cards_box_num);
		System.out.println("cards_future_date = " + cards_date.toString());
		//System.out.println("todays date = " + todays_date.toString());
		
		// Check if the review should be now or in the future.
		if        ( cards_date.before(todays_date) ) {
			// It's before.
			System.out.println(cards_date.toString() + " before " + todays_date.toString());
			return -1;
		} else if ( cards_date.after (todays_date) ) {
			// It's after.
			System.out.println(cards_date.toString() + " after " + todays_date.toString());
			return 1;
		} else {
			// It's equal.
			System.out.println(cards_date.toString() + " equal " + todays_date.toString());
			return 0;
		}
		
	}
	
	/**
	 * 
	 * @param d
	 * @param cards_box_num
	 * @return -1 if before today, 0 is today, 1 if after today
	 */
	public static int compare(Date d, int cards_box_num){
		String str = toString(d);
		
		return compare(str, cards_box_num);
	}

	// TODO: removed all of the things for my old way of reviewing decks.
	// Kept this function, because it might come in useful
	/**
	 * 
	 * @param d
	 * @param review_interval
	 * @return -1 if before now, 0 is now, 1 if after now
	 */
	public static int compareTime(String d, int review_interval){
		Date cards_time;
		try {
			cards_time = timeFromString(d);
		} catch (ParseException e) {
			e.printStackTrace();
			cards_time = new Date();
		}
		Date current_time = currentTime();
		
		// Add the box number to the day as days.
		// This gives an easy review algorithm.
		//System.out.println("cards_current_time = " + cards_time.toString());
		
		cards_time = addMinutes(cards_time, review_interval);
		//System.out.println("cards_future_time = " + cards_time.toString());
		//System.out.println("todays time = " + cards_time.toString());
		
		// Check if the review should be now or in the future.
		if        ( cards_time.before(current_time) ) {
			// It's before.
			//System.out.println(cards_time.toString() + cards_time.toString() + " is before " + current_time);
			return -1;
		} else if ( cards_time.after (current_time) ) {
			// It's after.
			//System.out.println(cards_time.toString() + cards_time.toString() + " is after "  + current_time);
			return 1;
		} else {
			// It's equal.
			//System.out.println(cards_time.toString() + cards_time.toString() + " is equal "  + current_time);
			return 0;
		}
		
	}
	
	/**
	 * 
	 * @param d
	 * @param review_interval
	 * @return -1 if before, 0 if equal, 1 if after
	 */
	public static int compareTime(Date d, int review_interval){
		String str = timeToString(d);
		
		return compareTime(str, review_interval);
	}

	public static String toString(Date date1) {
		String str_date;
		
		//Set the format
		SimpleDateFormat sdf = new SimpleDateFormat(STR_DATE_FORMAT);
		
		str_date = sdf.format(date1);
		
		return str_date;
	}
	
	public static String timeToString(Date date1) {
		String str_date;
		
		//Set the format
		SimpleDateFormat sdf = new SimpleDateFormat(STR_TIME_FORMAT);
		
		str_date = sdf.format(date1);
		
		return str_date;
	}

	public static Date fromString( String date1 ) throws ParseException {
		Date date2;
		try {
			date2 = new SimpleDateFormat(STR_DATE_FORMAT).parse(date1);
			return date2;
		} catch (ParseException e) {
			// Make the code calling this deal with the exception.
			throw e;
			//e.printStackTrace();
			//System.exit(500);
			//return new Date();
		}
	}
	
	public static Date timeFromString( String date1 ) throws ParseException {
		Date date2;
		try {
			date2 = new SimpleDateFormat(STR_TIME_FORMAT).parse(date1);
			return date2;
		} catch (ParseException e) {
			// Make the code calling this deal with the exception.
			throw e;
			//e.printStackTrace();
			//System.exit(500);
			//return new Date();
		}
	}
	
	/**
	 * Will return today's date as Date() object.
	 * Will return a null value if fromString() throws a ParseException, which should never happen, but I'm still documenting it :).
	 * @return
	 */
	public static Date today() {
		//Get the current date
		Calendar currentDate = Calendar.getInstance();
		
		//format it as a string
		SimpleDateFormat formatter= new SimpleDateFormat(STR_DATE_FORMAT);
		String dateNow = formatter.format(currentDate.getTime());
		
		// Convert the string to a Date object and return it		
		try {
			return fromString(dateNow);
		} catch (ParseException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Will return the current time as a Date() object.
	 * Will return a null value if timeFromString() throws a ParseException, which should never happen, but I'm still documenting it :).
	 * @return
	 */
	public static Date currentTime() {
		//Get the current date
		Calendar currentDate = Calendar.getInstance();
		
		//format it as a string
		SimpleDateFormat formatter= new SimpleDateFormat(STR_TIME_FORMAT);
		String dateNow = formatter.format(currentDate.getTime());
		
		// Convert the string to a Date object and return it		
		try {
			return timeFromString(dateNow);
		} catch (ParseException e) {
			e.printStackTrace();
			return null;
		}
	}
	/**
	 * @param d is of type 'Date'
	 * @param days is an integer 
	 * @return The result of d + days.
	 */
	public static Date addDays(Date d, int days) {
		//Date new_date = new Date();
		// Convert days to milliseconds and add it to date 
		//new_date.setTime( d.getTime() + (days*1000*60*60*24) );
	    
		//return new_date;
		
		
		Calendar c = Calendar.getInstance();
		c.setTime( d );
		c.add(Calendar.DATE, days);  // number of days to add
		
		return c.getTime();
	}
	
	/**
	 * @param d is a String of a date e.g. "dd/MM/yyyy".
	 * @param days is an integer 
	 * @return The result of d + days.
	 */
	public static String addDays(String d, int days) {
		// Convert string to a date.
		Date new_date = new Date();
		
		try {
			new_date = fromString(d);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		// Get the result
		new_date = addDays(new_date, days);
		
		// Return the result
		String str_new_date = toString(new_date);
		
		return str_new_date;
	}
	
	/**
	 * @param d is of type 'Date'
	 * @param mins is an integer 
	 * @return The result of d + mins.
	 */
	public static Date addMinutes(Date d, int mins) {
		//Date new_date = new Date();
		// Convert minutes to milliseconds and add it to date 
		//new_date.setTime( d.getTime() + (mins*1000*60) );
		
		//return new_date;
		
		Calendar c = Calendar.getInstance();
		c.setTime( d );
		c.add(Calendar.MINUTE, mins);  // number of minutes to add
		
		return c.getTime();

	}
	
	/**
	 * @param d is a String of a date e.g. "dd/MM/yyyy".
	 * @param mins is an integer 
	 * @return The result of d + mins.
	 */
	public static String addMinutes(String d, int mins) {
		// Convert string to a date.
		Date new_date = new Date();
		
		try {
			new_date = timeFromString(d);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		// Get the result
		new_date = addMinutes(new_date, mins);
		
		// Return the result
		String str_new_date = timeToString(new_date);
		
		return str_new_date;
	}
	
	public static boolean isLegalDate( String str ) {
		boolean b = true;
		try {
			fromString(str);
		} catch( ParseException e ) {
			b = false;
		}
		
		return b;
	}
	
	public static boolean isLegalTime( String str ) {
		boolean b = true;
		try {
			timeFromString(str);
		} catch( ParseException e ) {
			b = false;
		}
		
		return b;
	}
}



abstract public class Deck implements Serializable{
	ArrayList<Card>        deck = new ArrayList<Card>();
	ArrayList<Card> learnt_deck = new ArrayList<Card>();
	
	public DeckSettings settings;
 	
 	protected RandomizedIndex deck_index;

	protected File db_file;
	protected File db_config_file;
	
	
	// can either be = "reaction_speed", or "question_limit".
	protected String study_mode;
	
	Deck (String db_filename, String db_config_filename) {
		this( new File(db_filename), new File(db_config_filename) );
	}
	
	Deck( File db_filename, File db_config_filename ) {
		//setStudyModeToReactSpeed();
		setStudyModeToQuestionLimit();

		// Read database and send the cards found to the deck.
//		db_file        = new File(Environment.getExternalStorageDirectory(), db_filename) ;
//		db_config_file = new File(Environment.getExternalStorageDirectory(), db_config_filename) ;
		db_file        = db_filename;
		db_config_file = db_config_filename;
		
		settings = CardDBManager.getConfig(db_config_file);
		deck     = CardDBManager.readDB(db_file, settings);
		
		deck_index = new RandomizedIndex( this ); 
	}
	
	
	Deck( ArrayList<Card> d, File deck_file_path, DeckSettings s ) {
		db_file = deck_file_path;
		db_config_file = s.getFile();

		//setStudyModeToReactSpeed();
		setStudyModeToQuestionLimit();

		deck = d;
		settings = s;
		
		deck_index = new RandomizedIndex( this );
	}
	
	public int getDeckGuiType() {
		return settings.getDeckGuiType();
	}
	
	public String getFilePath() {
		return db_file.getAbsolutePath();
	}

	
	public boolean checkAnswer( String users_answer ) {
		if ( users_answer == getAnswer().get(0) ) {
			return true;
		} else {
			return false;
		}
	}
	
	public boolean checkAnswer( int users_answer ) {
		return checkAnswer( "" + users_answer );
	}

	
	public boolean isLearnt() {
		if( settings.areCardsRemovable() ) {
			if( deck.size() == 0 ) {
				return true;
			} else {
				return false;
			}
		} else {
			boolean is_deck_learnt = true;
			for( int i = 0; i < deck.size(); i++ ) {
				if( deck.get(i).isLearnt() == false ) {
					is_deck_learnt = false;
					break;
				}
			}
			
			return is_deck_learnt;
		}
	}
	
	public abstract ArrayList<String> getAnswer();
	
	protected Card getCurrentCard() {
		return deck.get(deck_index.getCurrent());
	}
	
	
	public boolean isStudyModeReactSpeed() {
		if ( study_mode == "reaction_speed" ) {
			return true;
		} else {
			return false;
		}
	}
	
	public boolean isStudyModeQuestionLimit() {
		if ( study_mode == "question_limit" ) {
			return true;
		} else {
			System.out.println("Study mode is: " + study_mode);
			return false;
		}
	}
	
	
	
	public void setStudyModeToReactSpeed() {
		study_mode = "reaction_speed";
	}
	
	public void setStudyModeToQuestionLimit() {
		study_mode = "question_limit";
	}
	
	
	public void nextQuestion( boolean is_answer_correct, boolean stay_on_incorrect_card ) {
		if( is_answer_correct ) {
			// Just get the next index.
			/* TODO:
			 * Log reaction speed, if it's reaction mode.
			 * 
			 */

			getCurrentCard().success();
		
			// Check if the card should be removed from the deck.
			if( settings.areCardsRemovable() ) {
				// Remove the card.
				// Check if the current card has been learnt, and move it to the learnt_deck.
				if( getCurrentCard().isLearnt() ) {
					// Update the card's details, used for writing to the database.
					learnt_deck.add( getCurrentCard() );
					deck.remove( deck_index.getCurrent() );
				}
			}
			
			deck_index.getNext();
		} else {
			// They got it wrong, so the card's success count has been reset to 0.
			// Now there are more reviews needed, so reset the index list.
			getCurrentCard().failed();
			if( stay_on_incorrect_card == false ) {
				deck_index.ResetList();
			}
		}
	
		if( isLearnt() ) {
			System.out.println("nextQuestion() has been called, and the deck has now been learnt.");
			System.out.println("It was probably learnt during this last execution of it, so does that make this message meaningless?");
		}
	}

	public ArrayList<Card> getDeck() {
		return deck;
	}
	
	public ArrayList<Card> getLearntDeck() {
		if( settings.areCardsRemovable() ) {
			// The cards were moved to this deck.
			
			// Update all the cards in the deck.
			for(int i = 0; i < learnt_deck.size(); i++ ) {
				if ( learnt_deck.get(i).hasBeenUpdated() == false ) {
					learnt_deck.get(i).update();
				}
			}
			return learnt_deck;
		} else {
			// The cards stayed in this deck.
			
			// update all the cards in this deck.
			for(int i = 0; i < deck.size(); i++ ) {
				if ( deck.get(i).hasBeenUpdated() == false ) {
					deck.get(i).update();
				}
			}
			return deck;
		}
	}

	
	public String addHTMLRow( String str) {
			str = "        <td>" + str + "</td>\n";
			return str;
	}
	
	public String toHTML() {
		String html = "<table>";
	
		html += "    <tr>\n";
		html += addHTMLRow("Question");
		html += addHTMLRow("Box Num");
		html += addHTMLRow("Question");
		ArrayList<Card> current_deck = getLearntDeck();
		for(int i = 0; i < current_deck.size(); i++) {
			html += "    <tr>\n";
			html += addHTMLRow( current_deck.get(i).getContent(0) );
			html += addHTMLRow( "" + current_deck.get(i).getBoxNum() );
			html += "    </tr>\n";
			
		}
		html += "</table>";
		
		return html;
	}

	
	
	/*public String[][] duplicate2dArray(int num_of_duplicates, String[][] arr){
		String[][] rand = new String[arr.length * num_of_duplicates][arr[0].length];

		// Copy the same array several times.
		// This method works best for shuffling.
		for ( int i = 0, ordered_index = 0;
		      i < rand.length;
		      i++, ordered_index++ ) {
			
			if ( ordered_index >= arr.length ) {
				ordered_index = 0;
			}
			for ( int k = 0; k < rand[i].length; k++) {
				rand[i][k] = arr[ordered_index][k];
			}
		}
		
		return arr;
	}*/

	
	/*static void shuffle2dArray(String[][] ar) {
		Random rand = new Random();
		for (int i = ar.length - 1; i > 0; i--) {
			int index = rand.nextInt(i + 1);
			// Simple swap
			for ( int k = ar[index].length - 1; k >= 0; k-- ) {
				String a = ar[index][k];
				ar[index][k] = ar[i][k];
				ar[i][k] = a;
			}
		}
	}*/

}
