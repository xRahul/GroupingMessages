package in.rahulja.groupingmessages;


import android.provider.BaseColumns;

public final class DatabaseContract {

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "groupMessage.db";
    /* An array list of all the SQL create table statements */
    public static final String[] SQL_CREATE_TABLE_ARRAY = {
            Category.CREATE_TABLE
    };
    private static final String TEXT_TYPE = " TEXT";
    private static final String INTEGER_TYPE = " INTEGER";
    private static final String COMMA_SEP = ",";

    // To prevent someone from accidentally instantiating the contract class,
    // make the constructor private.
    private DatabaseContract() {
    }

    /* Inner class that defines the table contents */
    public static abstract class Category implements BaseColumns {
        public static final String TABLE_NAME = "category";
        public static final String KEY_NAME = "name";
        public static final String KEY_VISIBILITY = "visibility";

        /**
         * The default sort order for this table
         */
        public static final String DEFAULT_SORT_ORDER = KEY_NAME + " ASC";

        public static final String CREATE_TABLE = "CREATE TABLE " +
                TABLE_NAME + " (" +
                _ID + INTEGER_TYPE + " PRIMARY KEY" + COMMA_SEP +
                KEY_NAME + TEXT_TYPE + COMMA_SEP +
                KEY_VISIBILITY + INTEGER_TYPE + " )";
        public static final String DELETE_TABLE = "DROP TABLE IF EXISTS " + TABLE_NAME;

        /**
         * Array of all the columns. Makes for cleaner code
         */
        public static final String[] KEY_ARRAY = {
                KEY_NAME,
                KEY_VISIBILITY
        };

    }
}