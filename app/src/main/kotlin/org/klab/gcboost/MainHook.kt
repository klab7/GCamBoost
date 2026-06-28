package org.klab.gcboost

import android.util.Log
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface
import java.lang.reflect.Modifier
import java.util.HashSet
import android.os.Build

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
        val device = Build.DEVICE
        val flagsToCheck = when (device) {
            "husky" -> modelEightPro
            "komodo", "caiman", "comet" -> modelNinePro
            "mustang", "blazer", "frankel" -> modelTenPro
            "frankel", "stallion"  -> modelTen
            else -> modelDefault
        }
        return flagsToCheck.any { it.contains(name) }
    }
    val fCameraCoach = arrayOf("camera.enable_burrata", "camera.burrata_eligible")
    val fPhotoSphere = arrayOf("lightcycle_enabled")
    val fNightSightVideo = arrayOf("camcorder_vega", "camcorder.vega_eligible")
    val fFourK60HDR = arrayOf("camcorder_enable_onyx", "camcorder.enable_onyx_eligible")
    val fEightK = arrayOf("camcorder.bison_eligible", "camcorder_bison")
    val fEightK24FPS = arrayOf("camcorder_axinite")
    val fProStableVideo = arrayOf("camcorder_jasper")
    val fAddMe = arrayOf("camera.cottage_enabled")
    val fAutoBestTake = arrayOf("camera.squad_detector", "camera.squad_hdrplus", "camera.squad_hdrplus_eligibiliity")

    val modelEightPro = arrayOf(fCameraCoach, fPhotoSphere, fNightSightVideo, fFourK60HDR, fEightK, fEightK24FPS, fProStableVideo, fAddMe, fAutoBestTake)
    val modelNinePro = arrayOf(fCameraCoach, fPhotoSphere, fEightK24FPS, fAutoBestTake)
    val modelTenPro = arrayOf(fPhotoSphere)
    val modelTen = arrayOf(fPhotoSphere)
    val modelDefault = arrayOf(fPhotoSphere, fCameraCoach, fAutoBestTake)
}

/*
Discovered but not included flags. Feel free to try them anyway.

    // 100x Zoom. Local model is specific to Tensor G5, so doesn't work
    val fHundredXZoom = arrayOf("camera.enable_centaur_setting", "camera.enable_centaur_chip", "camera.enable_centaur_chip_in_app_flow", "camera.enable_boba_jelly", "camera.boba_jelly_eligible")

    // High-Res Portrait - requires hardware support
    val fHighResPortrait = arrayOf("camera.crawfish_enabled", "camera.gouda.enable_unbinned_crop", "camera.gouda.support_unbinned_crop")

    // C2PA Content Credentials. Does nothing without hardware support
    val fC2PA = arrayOf("camera.blanket", "camera.blanket_eligible")

    // Super Res Zoom in Video Boost. Enables 20x zoom but doesn't auto switch to Tele lens, so pretty useless
    val fSuperResZoomVideo = arrayOf("camcorder.rose_eligible", "camcorder_rose")

    // Supposed to enable Tele lens in Night Sight video, but does nothing
    val fNightSightVideoTele = arrayOf("camcorder_shortite")

    // Supposed to enable UW lens in Night Sight video, but does nothing
    val fNightSightVideoUW = arrayOf("camcorder.topaz")

*/
