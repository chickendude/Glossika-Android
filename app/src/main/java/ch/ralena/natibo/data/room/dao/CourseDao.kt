package ch.ralena.natibo.data.room.dao

import androidx.room.*
import ch.ralena.natibo.data.room.`object`.CourseRoom

@Dao
interface CourseDao {
	@Query("SELECT * FROM courseroom")
	suspend fun getAll(): List<CourseRoom>

	@Query("SELECT * FROM courseroom WHERE id = :id")
	suspend fun getCourseById(id: Int): CourseRoom

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun insert(course: CourseRoom)

	@Update
	suspend fun update(course: CourseRoom)

	@Delete
	suspend fun delete(course: CourseRoom)
}