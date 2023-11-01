package com.example.imagepro;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;

import androidx.annotation.Nullable;

public class MyDatabaseHelper extends SQLiteOpenHelper {

    private Context context;
    public static final String DATABASE_NAME = "FaceDetection.db";
    public static final int DATABASE_VERSION = 1;

    public static final String TABLE_NAME = "My_detection";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_DATE = "detect_date";
    public static final String COLUMN_FIRST = "first_appearance";
    public static final String COLUMN_LAST = "last_appearance";

    public MyDatabaseHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String query = "CREATE TABLE " + TABLE_NAME +
                        " (" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COLUMN_NAME + " NAME, " +
                        COLUMN_DATE + " DATE, " +
                        COLUMN_FIRST + " TIME, " +
                        COLUMN_LAST + " TIME);";
        db.execSQL(query);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    void add(String name, String date, String first, String last) {
        /*
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();

        cv.put(COLUMN_NAME, name);
        cv.put(COLUMN_DATE, date);
        cv.put(COLUMN_FIRST, first);
        cv.put(COLUMN_LAST, last);

        long result = db.insert(TABLE_NAME, null, cv);
        System.out.println("okeee");

         */
        SQLiteDatabase db = this.getWritableDatabase();

        // Kiểm tra xem đã có bản ghi nào với name và date tương tự chưa
        Cursor cursor = db.query(TABLE_NAME, new String[]{COLUMN_ID}, COLUMN_NAME + " = ? AND " + COLUMN_DATE + " = ?", new String[]{name, date}, null, null, null);

        if (cursor.getCount() > 0) {
            // Bản ghi đã tồn tại, cập nhật giá trị last
            ContentValues cv = new ContentValues();
            cv.put(COLUMN_LAST, last);
            db.update(TABLE_NAME, cv, COLUMN_NAME + " = ? AND " + COLUMN_DATE + " = ?", new String[]{name, date});
        } else {
            // Bản ghi chưa tồn tại, thêm mới
            ContentValues cv = new ContentValues();
            cv.put(COLUMN_NAME, name);
            cv.put(COLUMN_DATE, date);
            cv.put(COLUMN_FIRST, first);
            cv.put(COLUMN_LAST, last);
            db.insert(TABLE_NAME, null, cv);
        }

        cursor.close();
    }
}
