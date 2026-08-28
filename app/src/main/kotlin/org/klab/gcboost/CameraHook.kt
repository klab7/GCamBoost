package org.klab.gcboost

import android.os.Build
import android.util.Log
import io.github.libxposed.api.XposedModuleInterface
import java.lang.reflect.Modifier
import java.util.HashSet

class CameraHook(private val module: MainHook) {
    private val processedClasses = HashSet<String>()

    fun handlePackage(param: XposedModuleInterface.PackageLoadedParam) {
        module.logMessage(Log.INFO, "GCamBoost", "Hooking into GoogleCamera...")
        setupTraceHook(param)
    }

    private fun setupTraceHook(param: XposedModuleInterface.PackageLoadedParam) {
        try {
            val traceClass = param.defaultClassLoader.loadClass("android.os.Trace")
            val beginSection = traceClass.getDeclaredMethod("beginSection", String::class.java)
            module.hookMethod(beginSection).intercept { chain ->
                if (chain.args[0] == "ProcessStablePhInit#GcaConfig") {
                    val stack = Throwable().stackTrace
                    for (frame in stack) {
                        val className = frame.className
                        if (className.contains(".")) continue

                        try {
                            val clazz = param.defaultClassLoader.loadClass(className)
                            if (isGcaConfigClass(clazz)) {
                                if (processedClasses.add(className)) {
                                    module.logMessage(Log.INFO, "GCamBoost", "Found Config Class: $className")
                                    hookDetectedConfig(clazz)
                                    break
                                }
                            }
                        } catch (e: Throwable) { }
                    }
                }
                chain.proceed()
            }
        } catch (e: Throwable) {
            module.logMessage(Log.ERROR, "GCamBoost", "Trace hook failed: ${e.message}")
        }
    }

    private fun isGcaConfigClass(clazz: Class<*>): Boolean {
        val methods = clazz.declaredMethods
        val hasBool = methods.any {
            it.parameterTypes.size == 1 && it.returnType == java.lang.Boolean.TYPE && !Modifier.isStatic(it.modifiers)
        }
        val hasString = methods.any {
            it.parameterTypes.size == 1 && it.returnType == String::class.java && !Modifier.isStatic(it.modifiers)
        }
        return hasBool && hasString
    }

    private fun hookDetectedConfig(configClass: Class<*>) {
        configClass.declaredMethods.forEach { method ->
            if (Modifier.isStatic(method.modifiers)) return@forEach

            val params = method.parameterTypes
            val returnType = method.returnType

            if (params.size == 1) {
                if (returnType == java.lang.Boolean.TYPE) {
                    module.hookMethod(method).intercept { chain ->
                        val flagName = getNameFromFlagObject(chain.args[0])
                        if (flagName != null && isTargetFlag(flagName)) {
                            true
                        } else {
                            chain.proceed()
                        }
                    }
                }
            }
        }
    }

    private fun getNameFromFlagObject(obj: Any?): String? {
        if (obj == null) return null
        var curr: Class<*>? = obj.javaClass
        while (curr != null && curr != Any::class.java) {
            for (field in curr.declaredFields) {
                if (field.type == String::class.java) {
                    try {
                        field.isAccessible = true
                        val value = field.get(obj) as? String
                        if (value != null && (value.contains(".") || value.contains("_"))) return value
                    } catch (e: Throwable) {}
                }
            }
            curr = curr.superclass
        }
        return null
    }

    private fun isTargetFlag(name: String): Boolean {
        val device = Build.DEVICE
        val flagsToCheck = when (device) {
            "husky" -> Models.modelEightPro
            "shiba", "akita" -> Models.modelEight
            "komodo", "caiman", "comet" -> Models.modelNinePro
            "tokay", "tegu" -> Models.modelNine
            "mustang", "blazer", "rango" -> Models.modelTenPro
            "frankel", "stallion"  -> Models.modelTen
            "grizzly", "kodiak", "yogi" -> Models.modelElevenPro
            "cubs"  -> Models.modelEleven
            else -> Models.modelDefault
        }
        return flagsToCheck.any { it.contains(name) }
    }
}
