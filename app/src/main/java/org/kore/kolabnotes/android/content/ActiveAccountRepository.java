package org.kore.kolabnotes.android.content;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * Created by koni on 12.03.15.
 */
public class ActiveAccountRepository {

    private static ActiveAccount currentActive;

    // Database fields
    private SQLiteDatabase database;
    private DatabaseHelper dbHelper;
    private String[] allColumns = { DatabaseHelper.COLUMN_ID,
            DatabaseHelper.COLUMN_ACCOUNT,
            DatabaseHelper.COLUMN_ROOT_FOLDER};

    public ActiveAccountRepository(Context context) {
        dbHelper = new DatabaseHelper(context);
    }

    public void open(){
        if(database == null || !database.isOpen()) {
            database = dbHelper.getWritableDatabase();
        }
    }

    public void close() {
        dbHelper.close();
    }

    public synchronized ActiveAccount switchAccount(String account, String rootFolder){
        open();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_ROOT_FOLDER, rootFolder);
        values.put(DatabaseHelper.COLUMN_ACCOUNT, account);

        int affectedRows = database.update(DatabaseHelper.TABLE_ACTIVEACCOUNT,
                values,
                null,
                null);

        currentActive = new ActiveAccount(account,rootFolder);
        close();
        return currentActive;
    }

    public synchronized ActiveAccount getActiveAccount() {
        if(currentActive == null) {
            open();
            Cursor cursor = database.query(DatabaseHelper.TABLE_ACTIVEACCOUNT,
                    allColumns,
                    null,
                    null,
                    null,
                    null,
                    null);

            if (cursor.moveToNext()) {
                currentActive = new ActiveAccount(cursor.getString(1), cursor.getString(2));
            }
            cursor.close();
            close();
        }
        return currentActive;
    }
}
