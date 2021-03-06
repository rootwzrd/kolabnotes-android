package org.kore.kolabnotes.android.content;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.kore.kolab.notes.AuditInformation;
import org.kore.kolab.notes.Colors;
import org.kore.kolab.notes.Identification;
import org.kore.kolab.notes.Note;
import org.kore.kolab.notes.Tag;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by koni on 12.03.15.
 */
public class NoteTagRepository {
    private final static String NOTES_COLUMNS = "note."+DatabaseHelper.COLUMN_UID+
            ", note."+DatabaseHelper.COLUMN_PRODUCTID+
            ", note."+DatabaseHelper.COLUMN_CREATIONDATE+
            ", note."+DatabaseHelper.COLUMN_MODIFICATIONDATE+
            ", note."+DatabaseHelper.COLUMN_SUMMARY+
            ", note."+DatabaseHelper.COLUMN_DESCRIPTION+
            ", note."+DatabaseHelper.COLUMN_CLASSIFICATION+
            ", note."+DatabaseHelper.COLUMN_COLOR;

    private final static String QUERY_NOTES = "SELECT "+NOTES_COLUMNS+" from "+DatabaseHelper.TABLE_NOTES+" note, "+DatabaseHelper.TABLE_NOTE_TAGS+" notetags " +
            " where notetags."+DatabaseHelper.COLUMN_ACCOUNT+" = ? " +
            " and notetags."+DatabaseHelper.COLUMN_ROOT_FOLDER+" = ? " +
            " and notetags."+DatabaseHelper.COLUMN_IDTAG+" = ? " +
            " and notetags."+DatabaseHelper.COLUMN_IDNOTE+" = note."+ DatabaseHelper.COLUMN_UID+" " +
            " and notetags."+DatabaseHelper.COLUMN_ACCOUNT+" = note."+ DatabaseHelper.COLUMN_ACCOUNT+" "+
            " and notetags."+DatabaseHelper.COLUMN_ROOT_FOLDER+" = note."+ DatabaseHelper.COLUMN_ROOT_FOLDER+" "+
            " order by "+DatabaseHelper.COLUMN_MODIFICATIONDATE+" desc ";


    // Database fields
    private SQLiteDatabase database;
    private DatabaseHelper dbHelper;
    private String[] allColumns = { DatabaseHelper.COLUMN_ID,
            DatabaseHelper.COLUMN_IDTAG,
            DatabaseHelper.COLUMN_ACCOUNT,
            DatabaseHelper.COLUMN_ROOT_FOLDER,
            DatabaseHelper.COLUMN_IDNOTE};

    public NoteTagRepository(Context context) {
        dbHelper = new DatabaseHelper(context);
    }

    public void open() {
        database = dbHelper.getWritableDatabase();
    }

    public void openReadonly() {
        database = dbHelper.getReadableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public void insert(String account, String rootFolder, String uidNote, String tagname) {
        open();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_IDNOTE,uidNote);
        values.put(DatabaseHelper.COLUMN_IDTAG,tagname);
        values.put(DatabaseHelper.COLUMN_ROOT_FOLDER,rootFolder);
        values.put(DatabaseHelper.COLUMN_ACCOUNT,account);

        database.insert(DatabaseHelper.TABLE_NOTE_TAGS, null,values);
        close();
    }

    public void delete(String account, String rootFolder, String uidNote, String tagname) {
        open();

        database.delete(DatabaseHelper.TABLE_NOTE_TAGS,
                DatabaseHelper.COLUMN_ACCOUNT + " = '" + account + "' AND " +
                        DatabaseHelper.COLUMN_ROOT_FOLDER + " = '" + rootFolder + "' AND " +
                        DatabaseHelper.COLUMN_IDNOTE + " = '" + uidNote + "' AND " +
                        DatabaseHelper.COLUMN_IDTAG + " = '" + tagname + "' ",
                null);
        close();
    }

    public void delete(String account, String rootFolder, String uidNote) {
        open();

        database.delete(DatabaseHelper.TABLE_NOTE_TAGS,
                DatabaseHelper.COLUMN_ACCOUNT + " = '" + account + "' AND " +
                        DatabaseHelper.COLUMN_ROOT_FOLDER + " = '" + rootFolder + "' AND " +
                        DatabaseHelper.COLUMN_IDNOTE + " = '" + uidNote + "' ",
                null);
        close();
    }

    public List<String> getTagsFor(String account,String rootFolder, String noteuid) {
        openReadonly();
        List<String> tags = new ArrayList<String>();

        Cursor cursor = database.query(DatabaseHelper.TABLE_NOTE_TAGS,
                allColumns,
                DatabaseHelper.COLUMN_ACCOUNT + " = '" + account+"' AND "+
                DatabaseHelper.COLUMN_ROOT_FOLDER + " = '" + rootFolder+"' AND "+
                DatabaseHelper.COLUMN_IDNOTE+" = '"+noteuid+"' ",
                null,
                null,
                null,
                null);

        while (cursor.moveToNext()) {
            tags.add(cursorToTag(cursor));
        }
        cursor.close();
        close();
        return tags;
    }

    void cleanAccount(String account, String rootFolder){
        open();
        database.delete(DatabaseHelper.TABLE_NOTE_TAGS,
                DatabaseHelper.COLUMN_ACCOUNT + " = '" + account+"' AND "+
                DatabaseHelper.COLUMN_ROOT_FOLDER + " = '" + rootFolder+"' ",
                null);

        close();
    }

    public List<Note> getNotesWith(String account,String rootFolder,String tagname) {
        openReadonly();
        List<Note> notes = new ArrayList<Note>();

        Cursor cursor = database.rawQuery(QUERY_NOTES,new String[]{account,rootFolder,tagname});


        while (cursor.moveToNext()) {
            notes.add(cursorToNote(cursor));
        }
        cursor.close();
        close();

        //load the tags for each note
        for(Note note : notes){
            List<String> tags = getTagsFor(account, rootFolder, note.getIdentification().getUid());

            if (tags != null && tags.size() > 0) {
                for(String tag : tags){
                    note.addCategories(new Tag(tag));
                }
            }
        }

        return notes;
    }

    private String cursorToTag(Cursor cursor) {
        return cursor.getString(1);
    }

    private Note cursorToNote(Cursor cursor) {
        String uid = cursor.getString(0);
        String productId = cursor.getString(1);
        Long creationDate = cursor.getLong(2);
        Long modificationDate = cursor.getLong(3);
        String summary = cursor.getString(4);
        String description = cursor.getString(5);
        String classification = cursor.getString(6);
        String color = cursor.getString(7);

        AuditInformation audit = new AuditInformation(new Timestamp(creationDate),new Timestamp(modificationDate));
        Identification ident = new Identification(uid,productId);

        Note note = new Note(ident,audit, Note.Classification.valueOf(classification),description);
        note.setSummary(summary);
        note.setColor(Colors.getColor(color));

        return note;
    }
}
