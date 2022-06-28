package ch.bfh.adaid.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface RuleDao {
    @Query("SELECT * FROM rule")
    List<Rule> getAll();

    @Query("SELECT * FROM rule WHERE id = (:ruleId)")
    Rule getById(long ruleId);

    @Query("SELECT * FROM rule WHERE name LIKE (:name) LIMIT 1")
    Rule findByName(String name);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(Rule rule);

    @Update
    int update(Rule rule);

    @Delete
    int delete(Rule rule);

    @Query("DELETE FROM rule")
    void deleteAll();
}
