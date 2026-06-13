package org.klab.gcboost

import android.util.Log
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface
import java.lang.reflect.Modifier
import java.util.HashSet

class MainHook : XposedModule() {
    private val processedClasses = HashSet<String>()

    override fun onPackageLoaded(param: XposedModuleInterface.PackageLoadedParam) {
        if (param.packageName != "com.google.android.GoogleCamera") return

        log(Log.INFO, "GCamBoost", "Hooking into GoogleCamera...")
        setupTraceHook(param)
    }


    private fun setupTraceHook(param: XposedModuleInterface.PackageLoadedParam) {
        try {
            val traceClass = param.defaultClassLoader.loadClass("android.os.Trace")
            val beginSection = traceClass.getDeclaredMethod("beginSection", String::class.java)
            hook(beginSection).intercept { chain ->
                if (chain.args[0] == "ProcessStablePhInit#GcaConfig") {
                    val stack = Throwable().stackTrace
                    for (frame in stack) {
                        val className = frame.className
                        if (className.contains(".")) continue

                        try {
                            val clazz = param.defaultClassLoader.loadClass(className)
                            if (isGcaConfigClass(clazz)) {
                                if (processedClasses.add(className)) {
                                    log(Log.INFO, "GCamBoost", "Found Config Class: $className")
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
            log(Log.ERROR, "GCamBoost", "Trace hook failed: ${e.message}")
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
                    hook(method).intercept { chain ->
                        val flagName = getNameFromFlagObject(chain.args[0])
                        if (flagName != null && isTargetFlag(flagName)) {
                          //  log(Log.DEBUG, "GCamBoost", "Set $flagName to True")
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
        return name == "camera.cottage_enabled" || //Add Me

                // Night Sight Video
                name == "camcorder_vega" ||
                name == "camcorder.vega_eligible" ||
                name == "camcorder.surface_share"  ||

                // 4K 60FPS (Video Boost)
                name == "camcorder_enable_onyx" ||
                name == "camcorder.enable_onyx_eligible"||

                // 8K (Video Boost)
                name == "camcorder.bison_eligible" ||
                name == "camcorder_bison" ||

                // 8K 24FPS (Video Boost)
                name == "camcorder_axinite" ||

                // Pro Stable (Video Boost)
                name == "camcorder_jasper" ||

                // Camera Coach
                name == "camera.enable_burrata" ||
                name == "camera.burrata_eligible" ||

                // Photo Sphere
                name == "lightcycle_enabled" ||

                // Super Res Zoom in Video Boost. Enables 20x zoom but it doesn't auto switch to Tele lens
                name == "camcorder.rose_eligible" ||
                name == "camcorder_rose" ||

                // Supposed to enable UW & tele lens in Video Boost/Night Sight video
                name == "camcorder_shortite" ||
                name == "camcorder.topaz"

    }
}

/*
Discovered but not included flags. Feel free to try them anyway.

// 100x Zoom. Local model is specific to Tensor G5, so doesn't work
name == "camera.enable_centaur_setting" ||
name == "camera.enable_centaur_chip" ||
name == "camera.enable_centaur_chip_in_app_flow" ||
name == "camera.enable_boba_jelly" ||
name == "camera.boba_jelly_eligible" ||

// C2PA Content Credentials. Does nothing without hardware support
name == "camera.blanket" ||
name == "camera.blanket_eligible" ||
*/
