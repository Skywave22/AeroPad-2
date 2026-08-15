package com.bluepilot.remote.data.custom

import android.content.Context
import com.bluepilot.remote.model.custom.CustomButton
import com.bluepilot.remote.model.custom.CustomRemoteCodec
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * BLEK-PRO PACK v2 — persistence for user-built custom remote layouts.
 * Same battle-tested pattern as HostProfileStore: JSON file in app files,
 * reads never throw (corrupt file = empty), writes atomic-ish via temp.
 */
interface CustomRemote {
    val buttons: StateFlow<List<CustomButton>>
    fun add(button: CustomButton)
    fun remove(id: String)
}

@Singleton
class CustomRemoteStore @Inject constructor(
    @ApplicationContext private val context: Context
) : CustomRemote {

    private fun file() = java.io.File(context.filesDir, "custom_remote.json")

    private val _buttons = MutableStateFlow(load())
    override val buttons: StateFlow<List<CustomButton>> = _buttons.asStateFlow()

    private fun load(): List<CustomButton> = runCatching {
        CustomRemoteCodec.decode(file().takeIf { it.exists() }?.readText())
    }.getOrDefault(emptyList())

    private fun persist(list: List<CustomButton>) {
        _buttons.value = list
        runCatching {
            val tmp = java.io.File(context.filesDir, "custom_remote.tmp")
            tmp.writeText(CustomRemoteCodec.encode(list))
            tmp.renameTo(file())
        }.onFailure { Timber.e(it, "custom remote save failed") }
    }

    override fun add(button: CustomButton) =
        persist(CustomRemoteCodec.add(_buttons.value, button))

    override fun remove(id: String) =
        persist(CustomRemoteCodec.remove(_buttons.value, id))
}
