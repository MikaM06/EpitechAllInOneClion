package eu.epitech.allinone.settings

import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service

@Service(Service.Level.APP)
@State(name = "EpitechSettings", storages = [Storage("epitech-all-in-one.xml")])
class EpitechSettings : SimplePersistentStateComponent<EpitechSettings.Options>(Options()) {

    class Options : BaseState() {
        var author by string("")
        var email by string("")
        var addAuthorLine by property(false)
        var epiclangPath by string("epiclang")
        var autoInclude by property(true)
        var includePaths by list<String>()
        var liveCheck by property(true)
        var liveCheckDelayMs by property(700)
    }

    companion object {
        fun getInstance(): EpitechSettings = service()
        val options: Options get() = getInstance().state
    }
}
