package org.klab.gcboost

import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface
import java.lang.reflect.Method

class MainHook : XposedModule() {

    override fun onPackageLoaded(param: XposedModuleInterface.PackageLoadedParam) {
        if (param.packageName == "com.google.android.GoogleCamera") {
            CameraHook(this).handlePackage(param)
        }
    }

    fun <T : Method> hookMethod(method: T) = hook(method)

    fun logMessage(priority: Int, tag: String, msg: String) = log(priority, tag, msg)
}
