package ar.jornadas.ui

import android.app.Application
import androidx.lifecycle.*
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import ar.jornadas.JornadasApp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

class ProgramaModel(app: Application, private val estado: SavedStateHandle) : AndroidViewModel(app) {

    companion object {
        @Volatile private var yaRecargo = false

        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = extras[APPLICATION_KEY]!!
                val savedState = extras.createSavedStateHandle()
                return ProgramaModel(app, savedState) as T
            }
        }
    }

    private val servicios = (app as JornadasApp).servicios
    val mesa = servicios.mesa
    private val repo = servicios.agenda
    val charlas = repo.programa.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val dia: MutableStateFlow<Int> = MutableStateFlow(estado["dia"] ?: 0)
    val detalle: MutableStateFlow<String?> = MutableStateFlow(estado["detalle"])
    val temaIdx: MutableStateFlow<Int> = MutableStateFlow(estado["temaIdx"] ?: 0)

    val aviso = MutableStateFlow("")
    val cargando = MutableStateFlow(false)
    val descargado: StateFlow<Boolean> = repo.descargado
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val enVuelo = AtomicInteger(0)

    fun abrirFicha(id: String) {
        estado["detalle"] = id
        detalle.value = id
    }

    fun cerrarFicha() {
        estado["detalle"] = null
        detalle.value = null
    }

    fun seleccionarDia(idx: Int) {
        estado["dia"] = idx
        dia.value = idx
    }

    fun seleccionarTema(idx: Int) {
        estado["temaIdx"] = idx
        temaIdx.value = idx
    }

    fun recargarSiEsNecesario() {
        if (!yaRecargo) {
            yaRecargo = true
            recargar()
        }
    }

    fun recargar() {
        if (enVuelo.get() >= 2) return
        viewModelScope.launch {
            if (enVuelo.getAndIncrement() == 0) cargando.value = true
            aviso.value = ""
            try {
                repo.sincronizar()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                aviso.value = if (descargado.value) "Usando datos guardados" else "Sin conexión"
            } finally {
                if (enVuelo.decrementAndGet() == 0) cargando.value = false
            }
        }
    }

    fun marcar(id: String) { viewModelScope.launch { repo.cambiarFavorita(id) } }
}
