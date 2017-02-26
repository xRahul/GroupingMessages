package in.rahulja.groupingmessages;


import android.provider.BaseColumns;

final class DatabaseContract {

    static final int DATABASE_VERSION = 2;
    static final String DATABASE_NAME = "groupMessage.db";
    private static final String TEXT_TYPE = " TEXT";
    private static final String INTEGER_TYPE = " INTEGER";
    private static final String FLOAT_TYPE = " REAL";
    private static final String TIME_TYPE = " DATETIME";
    private static final String DEFAULT = " DEFAULT";
    private static final String CURRENT_TIMESTAMP = " CURRENT_TIMESTAMP";
    private static final String COMMA_SEP = ",";
    private static final String COLUMN_CREATED_AT = "created_at";
    private static final String COLUMN_UPDATED_AT = "updated_at";
    private static final String PRIMARY_KEY = " PRIMARY KEY";
    private static final String CREATE_TABLE_PREFIX = "CREATE TABLE ";
    private static final String DROP_TABLE_IF_EXISTS = "DROP TABLE IF EXISTS ";
    private static final String CANT_INSTANTIATE_CLASS = "Can't instantiate class";
    private static final String CREATE_TRIGGER_FORMAT = "CREATE TRIGGER %s_update_at_trigger AFTER UPDATE ON %s FOR EACH ROW BEGIN UPDATE %s  SET %s = %s WHERE %s = old.%s; END";
    private static final String ALTER_TABLE_ADD_COLUMN = "ALTER TABLE %s ADD %s %s %s %s";

    // To prevent someone from accidentally instantiating the contract class,
    // make the constructor private.
    private DatabaseContract() {
    }

    abstract static class Config implements BaseColumns {
        static final String TABLE_NAME = "config";
        static final String KEY_NAME = "name";
        /**
         * The default sort order for this table
         */
        static final String DEFAULT_SORT_ORDER = KEY_NAME + " ASC";
        static final String KEY_VALUE = "value";
        static final String KEY_CREATED_AT = COLUMN_CREATED_AT;
        static final String KEY_UPDATED_AT = COLUMN_UPDATED_AT;
        static final String CREATE_TABLE = CREATE_TABLE_PREFIX +
                TABLE_NAME + " (" +
                _ID + INTEGER_TYPE + PRIMARY_KEY + COMMA_SEP +
                KEY_NAME + TEXT_TYPE + " not null unique " + COMMA_SEP +
                KEY_VALUE + TEXT_TYPE + COMMA_SEP +
                KEY_CREATED_AT + TIME_TYPE + DEFAULT + CURRENT_TIMESTAMP + COMMA_SEP +
                KEY_UPDATED_AT + TIME_TYPE + DEFAULT + CURRENT_TIMESTAMP + " )";
        static final String DELETE_TABLE = DROP_TABLE_IF_EXISTS + TABLE_NAME;
        static final String UPDATE_AT_TRIGGER =
                String.format(CREATE_TRIGGER_FORMAT, TABLE_NAME, TABLE_NAME, TABLE_NAME, KEY_UPDATED_AT, CURRENT_TIMESTAMP, _ID, _ID);
        /**
         * Array of all the columns. Makes for cleaner code
         */
        static final String[] KEY_ARRAY = {
                _ID,
                KEY_NAME,
                KEY_VALUE,
                KEY_CREATED_AT,
                KEY_UPDATED_AT
        };

        private Config() {
            throw new IllegalAccessError(CANT_INSTANTIATE_CLASS);
        }

    }

    abstract static class Category implements BaseColumns {
        static final String TABLE_NAME = "category";
        static final String KEY_NAME = "name";
        static final String KEY_COLOR = "color";
        static final String KEY_VISIBILITY = "visibility";
        static final String KEY_CREATED_AT = COLUMN_CREATED_AT;
        static final String KEY_UPDATED_AT = COLUMN_UPDATED_AT;
        /**
         * The default sort order for this table
         */
        static final String DEFAULT_SORT_ORDER = KEY_NAME + " ASC";
        static final String CREATE_TABLE = DatabaseContract.CREATE_TABLE_PREFIX +
                TABLE_NAME + " (" +
                _ID + INTEGER_TYPE + PRIMARY_KEY + COMMA_SEP +
                KEY_NAME + TEXT_TYPE + COMMA_SEP +
                KEY_COLOR + TEXT_TYPE + COMMA_SEP +
                KEY_VISIBILITY + INTEGER_TYPE + DEFAULT + " 1" + COMMA_SEP +
                KEY_CREATED_AT + TIME_TYPE + DEFAULT + CURRENT_TIMESTAMP + COMMA_SEP +
                KEY_UPDATED_AT + TIME_TYPE + DEFAULT + CURRENT_TIMESTAMP + " )";
        static final String DELETE_TABLE = DROP_TABLE_IF_EXISTS + TABLE_NAME;
        static final String UPDATE_AT_TRIGGER =
                String.format(CREATE_TRIGGER_FORMAT, TABLE_NAME, TABLE_NAME, TABLE_NAME, KEY_UPDATED_AT, CURRENT_TIMESTAMP, _ID, _ID);
        /**
         * Array of all the columns. Makes for cleaner code
         */
        static final String[] KEY_ARRAY = {
                _ID,
                KEY_NAME,
                KEY_COLOR,
                KEY_VISIBILITY,
                KEY_CREATED_AT,
                KEY_UPDATED_AT
        };

        private Category() {
            throw new IllegalAccessError(CANT_INSTANTIATE_CLASS);
        }

    }

    abstract static class Sms implements BaseColumns {
        static final String TABLE_NAME = "sms";

        static final String KEY_DATE = "date";
        static final String KEY_PERSON = "person";
        static final String KEY_READ = "read";
        static final String KEY_SEEN = "seen";
        static final String KEY_SUBJECT = "subject";
        static final String KEY_BODY = "body";
        static final String KEY_ADDRESS = "address";
        static final String KEY_CATEGORY_ID = "category_id";
        static final String KEY_SIMILAR_TO = "similar_to";
        static final String KEY_SIM_SCORE = "similarity_score";
        static final String KEY_CREATED_AT = COLUMN_CREATED_AT;
        static final String KEY_UPDATED_AT = COLUMN_UPDATED_AT;
        static final String KEY_CLEANED_SMS = "cleaned_sms";
        static final String KEY_VISIBILITY = "visibility";
        static final int SENDER_CONTACT = 0;
        static final int SENDER_NUMBER = 1;
        static final int SENDER_COMPANY = 2;
        static final String[] SENDER_TYPES = new String[]{"contact", "number", "company"};
        static final String KEY_SENDER_TYPE = "sender_type";

        static final String DEFAULT_SORT_ORDER = KEY_DATE + " DESC";

        static final String DELETE_TABLE = DROP_TABLE_IF_EXISTS + TABLE_NAME;
        
        static final String UPDATE_AT_TRIGGER =
                String.format(CREATE_TRIGGER_FORMAT, TABLE_NAME, TABLE_NAME, TABLE_NAME, KEY_UPDATED_AT, CURRENT_TIMESTAMP, _ID, _ID);

        /**
         * Array of all the columns. Makes for cleaner code
         */
        static final String[] KEY_ARRAY = {
                _ID,
                KEY_DATE,
                KEY_PERSON,
                KEY_READ,
                KEY_SEEN,
                KEY_ADDRESS,
                KEY_SUBJECT,
                KEY_BODY,
                KEY_CLEANED_SMS,
                KEY_VISIBILITY,
                KEY_SENDER_TYPE,
                KEY_CATEGORY_ID,
                KEY_SIMILAR_TO,
                KEY_SIM_SCORE,
                KEY_CREATED_AT,
                KEY_UPDATED_AT
        };

        static final String CREATE_TABLE = DatabaseContract.CREATE_TABLE_PREFIX +
                TABLE_NAME + " (" +
                _ID + INTEGER_TYPE + PRIMARY_KEY + COMMA_SEP +
                KEY_DATE + INTEGER_TYPE + COMMA_SEP +
                KEY_PERSON + INTEGER_TYPE + COMMA_SEP +
                KEY_READ + INTEGER_TYPE + DEFAULT + " 0" + COMMA_SEP +
                KEY_SEEN + INTEGER_TYPE + DEFAULT + " 0" + COMMA_SEP +
                KEY_SUBJECT + TEXT_TYPE + COMMA_SEP +
                KEY_ADDRESS + TEXT_TYPE + COMMA_SEP +
                KEY_BODY + TEXT_TYPE + COMMA_SEP +
                KEY_CLEANED_SMS + TEXT_TYPE + DEFAULT + "''" + COMMA_SEP +
                KEY_VISIBILITY + INTEGER_TYPE + DEFAULT + " 1" + COMMA_SEP +
                KEY_SENDER_TYPE + INTEGER_TYPE + COMMA_SEP +
                KEY_CATEGORY_ID + INTEGER_TYPE + COMMA_SEP +
                KEY_SIMILAR_TO + INTEGER_TYPE + COMMA_SEP +
                KEY_SIM_SCORE + FLOAT_TYPE + DEFAULT + " 0.0" + COMMA_SEP +
                KEY_CREATED_AT + TIME_TYPE + DEFAULT + CURRENT_TIMESTAMP + COMMA_SEP +
                KEY_UPDATED_AT + TIME_TYPE + DEFAULT + CURRENT_TIMESTAMP + COMMA_SEP +
                " FOREIGN KEY (" + KEY_CATEGORY_ID + ") REFERENCES "
                + Category.TABLE_NAME + "(" + Category._ID + ")" +
                " FOREIGN KEY (" + KEY_SIMILAR_TO + ") REFERENCES "
                + TABLE_NAME + "(" + _ID + ")" + ");";

        static final String[] CHANGES_V2 = {
                String.format(ALTER_TABLE_ADD_COLUMN, TABLE_NAME, KEY_CLEANED_SMS, TEXT_TYPE, DEFAULT, "''"),
                String.format(ALTER_TABLE_ADD_COLUMN, TABLE_NAME, KEY_VISIBILITY, INTEGER_TYPE, DEFAULT, "1"),
                String.format(ALTER_TABLE_ADD_COLUMN, TABLE_NAME, KEY_SENDER_TYPE, INTEGER_TYPE, "", ""),
        };


        private Sms() {
            throw new IllegalAccessError(CANT_INSTANTIATE_CLASS);
        }

    }
}