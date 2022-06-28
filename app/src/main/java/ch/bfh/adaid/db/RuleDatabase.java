package ch.bfh.adaid.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/**
 * Room database. Class is a singleton as each database instance has a heavy cost on resources.
 * <p>
 * Source: https://developer.android.com/codelabs/android-room-with-a-view#7
 *
 * @author Niklaus Leuenberger
 */
@Database(entities = {Rule.class}, version = 1, exportSchema = false)
public abstract class RuleDatabase extends RoomDatabase {

    /**
     * RuleDao getter. Is abstract but gets automatically implemented by androidx.room.
     *
     * @return Rule database access object.
     */
    public abstract RuleDao ruleDao();

    private static volatile RuleDatabase INSTANCE; // singleton instance

    /**
     * Get the singleton instance of the database.
     *
     * @param context Application context.
     * @return The singleton instance of the database.
     */
    public static RuleDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (RuleDatabase.class) {
                if (INSTANCE == null) {
                    // Instantiate / build the database if no instance exists yet. If the version of
                    // the database changed, the database will be destructively updated.
                    // TODO: Add migration logic. (https://medium.com/androiddevelopers/understanding-migrations-with-room-f01e04b07929)
                    INSTANCE = Room.databaseBuilder(context, RuleDatabase.class, "rule_database")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
