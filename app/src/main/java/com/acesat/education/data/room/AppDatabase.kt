package com.acesat.education.data.room

import android.content.Context
import androidx.room.Database
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentDao {
    @Query("SELECT * FROM students LIMIT 1")
    fun getStudent(): Flow<Student?>

    @Insert
    suspend fun insertStudent(student: Student): Long

    @Update
    suspend fun updateStudent(student: Student)
}

@Dao
interface AttemptDao {
    @Query("SELECT * FROM attempts WHERE studentId = :studentId ORDER BY timestamp DESC")
    fun getAttemptsFlow(studentId: Int): Flow<List<Attempt>>

    @Query("SELECT * FROM attempts WHERE studentId = :studentId ORDER BY timestamp DESC")
    suspend fun getAttempts(studentId: Int): List<Attempt>

    @Insert
    suspend fun insertAttempt(attempt: Attempt): Long
}

@Dao
interface WeakAreaDao {
    @Query("SELECT * FROM weak_areas WHERE studentId = :studentId ORDER BY proficiencyScore ASC")
    fun getWeakAreasFlow(studentId: Int): Flow<List<WeakArea>>

    @Query("SELECT * FROM weak_areas WHERE studentId = :studentId ORDER BY proficiencyScore ASC")
    suspend fun getWeakAreas(studentId: Int): List<WeakArea>

    @Insert
    suspend fun insertWeakArea(weakArea: WeakArea): Long

    @Update
    suspend fun updateWeakArea(weakArea: WeakArea)

    @Query("SELECT * FROM weak_areas WHERE studentId = :studentId AND category = :category LIMIT 1")
    suspend fun getWeakAreaByCategory(studentId: Int, category: String): WeakArea?
}

@Dao
interface StudyPlanDao {
    @Query("SELECT * FROM study_plans WHERE studentId = :studentId ORDER BY stepOrder ASC")
    fun getStudyPlansFlow(studentId: Int): Flow<List<StudyPlan>>

    @Query("SELECT * FROM study_plans WHERE studentId = :studentId ORDER BY stepOrder ASC")
    suspend fun getStudyPlans(studentId: Int): List<StudyPlan>

    @Insert
    suspend fun insertStudyPlanSteps(steps: List<StudyPlan>)

    @Update
    suspend fun updateStudyPlanStep(step: StudyPlan)

    @Query("DELETE FROM study_plans WHERE studentId = :studentId")
    suspend fun deleteStudyPlan(studentId: Int)
}

@Database(entities = [Student::class, Attempt::class, WeakArea::class, StudyPlan::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun studentDao(): StudentDao
    abstract fun attemptDao(): AttemptDao
    abstract fun weakAreaDao(): WeakAreaDao
    abstract fun studyPlanDao(): StudyPlanDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "acesat_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
