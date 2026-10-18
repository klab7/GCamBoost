package org.klab.gcboost

import android.util.Log
import io.github.libxposed.api.XposedModuleInterface
import java.lang.reflect.Modifier
import java.util.HashSet

class CameraHook(private val module: MainHook) {
    private val processedClasses = HashSet<String>()
    private val zoomCam11Hook = ZoomCam11Hook(module)
    private val zoomCam10Hook = ZoomCam10Hook(module)

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
                            if (processedClasses.add(className)) {
                                if (isGcaConfigClass(clazz)) {
                                    module.logMessage(Log.INFO, "GCamBoost", "Found Config Class: $className")
                                    hookDetectedConfig(clazz)
                                }
                                hookZoomConfigInClass(clazz)
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
            it.parameterTypes.size == 1 && (it.returnType == java.lang.Boolean.TYPE || it.returnType == java.lang.Boolean::class.java) && !Modifier.isStatic(it.modifiers)
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
                if (returnType == java.lang.Boolean.TYPE || returnType == java.lang.Boolean::class.java) {
                    module.hookMethod(method).intercept { chain ->
                        val flagName = getNameFromFlagObject(chain.args[0])
                        if (flagName != null) {
                            if (DeviceHelper.isTargetFlag(flagName)) {
                                true
                            } else if (DeviceHelper.isTargetFalseFlag(flagName)) {
                                false
                            } else {
                                chain.proceed()
                            }
                        } else {
                            chain.proceed()
                        }
                    }
                } else if (returnType == java.lang.Float.TYPE || returnType == java.lang.Float::class.java) {
                    module.hookMethod(method).intercept { chain ->
                        val flagName = getNameFromFlagObject(chain.args[0])
                        if (flagName != null) {
                            val targetFloat = DeviceHelper.getTargetFloatFlag(flagName)
                            if (targetFloat != null) {
                                targetFloat
                            } else {
                                chain.proceed()
                            }
                        } else {
                            chain.proceed()
                        }
                    }
                } else if (returnType == java.lang.Integer.TYPE || returnType == java.lang.Integer::class.java) {
                    module.hookMethod(method).intercept { chain ->
                        val flagName = getNameFromFlagObject(chain.args[0])
                        if (flagName != null) {
                            val targetInt = DeviceHelper.getTargetIntFlag(flagName)
                            if (targetInt != null) {
                                targetInt
                            } else {
                                chain.proceed()
                            }
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

    private fun hookZoomConfigInClass(clazz: Class<*>) {
        if (!DeviceHelper.is10xZoomEnabledForDevice() && !DeviceHelper.isPortraitZoomEnabledForDevice()) return

        clazz.declaredMethods.forEach { method ->
            if (method.parameterTypes.isNotEmpty() || Modifier.isStatic(method.modifiers)) return@forEach
            val returnType = method.returnType

            if (returnType == Any::class.java ||
                Map::class.java.isAssignableFrom(returnType)
            ) {
                try {
                    module.hookMethod(method).intercept { chain ->
                        val result = chain.proceed()
                        if (result != null) {
                            when (result) {
                                is Map<*, *> -> {
                                    val patchedMap = patchZoomMap(result)
                                    if (patchedMap != null) {
                                        module.logMessage(Log.INFO, "GCamBoost", "Patched Zoom Map in ${clazz.name}#${method.name}")
                                        patchedMap
                                    } else {
                                        result
                                    }
                                }
                                else -> result
                            }
                        } else {
                            result
                        }
                    }
                } catch (e: Throwable) {}
            }
        }
    }

    private fun patchZoomMap(map: Map<*, *>): Map<*, *>? {
        if (map.isEmpty()) return null
        val values = map.values
        val firstItem = values.firstOrNull { it != null } ?: return null

        var hasZoomFloats = false
        for (item in values) {
            if (item == null) continue
            try {
                for (f in item.javaClass.declaredFields) {
                    f.isAccessible = true
                    val list = f.get(item) as? List<*>
                    if (list != null && list.any { it is Number && (it.toFloat() == 1.0f || it.toFloat() == 2.0f || it.toFloat() == 5.0f || it.toFloat() == 0.5f) }) {
                        hasZoomFloats = true
                        break
                    }
                }
            } catch (e: Throwable) {}
            if (hasZoomFloats) break
        }

        if (!hasZoomFloats) return null

        val zoomClass = firstItem.javaClass
        val isCam10 = try {
            zoomClass.getMethod("I")
            false
        } catch (e: Throwable) {
            true
        }

        return if (isCam10) {
            zoomCam10Hook.patchZoomMapCam10(map)
        } else {
            zoomCam11Hook.patchZoomMapCam11(map)
        }
    }
}
