package ar.jornadas.data

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import retrofit2.http.GET
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class FichaRemota(val id: String, val title: String, val track: String, val day: Int,
    val time: String, val room: String, val speaker: String, val summary: String)

interface ProgramaApi {
    @GET("programa") suspend fun consultar(): List<FichaRemota>
}

@Entity(tableName = "charlas")
data class Charla(@PrimaryKey val id: String, val titulo: String, val tema: String, val dia: Int,
    val hora: String, val sala: String, val ponente: String, val resumen: String,
    val favorita: Boolean = false)

@Entity(tableName = "estado_descarga")
data class DescargaEstado(@PrimaryKey val clave: String = "unica", val descargado: Boolean)

@Dao
interface CarteleraDao {
    @Query("SELECT * FROM charlas ORDER BY dia, hora, sala")
    fun observar(): Flow<List<Charla>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertar(fichas: List<Charla>)

    @Query("""
        UPDATE charlas SET titulo = :titulo, tema = :tema, dia = :dia,
        hora = :hora, sala = :sala, ponente = :ponente, resumen = :resumen
        WHERE id = :id
    """)
    suspend fun actualizar(id: String, titulo: String, tema: String, dia: Int,
        hora: String, sala: String, ponente: String, resumen: String)

    @Query("DELETE FROM charlas WHERE id NOT IN (:ids)")
    suspend fun eliminarRetiradas(ids: List<String>)

    @Query("DELETE FROM charlas")
    suspend fun eliminarTodas()

    @Query("UPDATE charlas SET favorita = NOT favorita WHERE id = :id")
    suspend fun alternar(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun marcarDescargado(estado: DescargaEstado)

    @Query("SELECT COUNT(*) FROM estado_descarga WHERE clave = 'unica' AND descargado = 1")
    suspend fun fueDescargado(): Int

    @Query("SELECT COUNT(*) FROM estado_descarga WHERE clave = 'unica' AND descargado = 1")
    fun observarDescargado(): Flow<Int>
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS estado_descarga (clave TEXT NOT NULL PRIMARY KEY, descargado INTEGER NOT NULL)"
        )
        db.execSQL(
            "INSERT OR IGNORE INTO estado_descarga (clave, descargado) SELECT 'unica', 1 WHERE (SELECT COUNT(*) FROM charlas) > 0"
        )
    }
}

@Database(entities = [Charla::class, DescargaEstado::class], version = 2, exportSchema = false)
abstract class Archivo : RoomDatabase() {
    abstract fun cartelera(): CarteleraDao
}
