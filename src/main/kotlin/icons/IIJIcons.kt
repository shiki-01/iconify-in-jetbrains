package icons

import com.intellij.openapi.util.IconLoader.getIcon
import javax.swing.Icon

object IIJIcons {
    @JvmField
    var iconToolWindow: Icon = load("/icons/iconToolWindow.svg")

    @JvmStatic
    fun load(path: String): Icon {
        return getIcon(path, IIJIcons::class.java)
    }
}