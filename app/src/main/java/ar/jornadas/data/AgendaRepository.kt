package ar.jornadas.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AgendaRepository(private val api: ProgramaApi, private val dao: CarteleraDao,
    private val db: androidx.room.RoomDatabase) {
    val programa = dao.observar()
    val descargado: Flow<Boolean> = dao.observarDescargado().map { it > 0 }

    suspend fun sincronizar() {
        val remotas = api.consultar()
        val charlas = remotas.map {
            Charla(it.id, it.title, it.track, it.day, it.time, it.room, it.speaker, it.summary)
        }
        db.withTransaction {
            if (charlas.isEmpty()) {
                dao.eliminarTodas()
            } else {
                dao.insertar(charlas)
                charlas.forEach { c ->
                    dao.actualizar(c.id, c.titulo, c.tema, c.dia, c.hora, c.sala, c.ponente, c.resumen)
                }
                dao.eliminarRetiradas(charlas.map { it.id })
            }
            dao.marcarDescargado(DescargaEstado(descargado = true))
        }
    }

    suspend fun cambiarFavorita(id: String) = dao.alternar(id)
}
