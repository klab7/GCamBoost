package org.klab.gcboost

import java.util.LinkedHashMap

class ZoomCam11Hook(private val module: MainHook) {
    fun patchZoomMapCam11(map: Map<*, *>): Map<*, *>? {
        if (map.isEmpty()) return null
        val values = map.values
        val firstItem = values.firstOrNull { it != null } ?: return null
        val clazz = firstItem.javaClass

        val fieldD = try { clazz.getDeclaredField("d") } catch (e: Throwable) { null } ?: return null
        val fieldE = try { clazz.getDeclaredField("e") } catch (e: Throwable) { null }
        val fieldB = try { clazz.getDeclaredField("b") } catch (e: Throwable) { null }
        val fieldC = try { clazz.getDeclaredField("c") } catch (e: Throwable) { null }

        fieldD.isAccessible = true
        fieldE?.isAccessible = true
        fieldB?.isAccessible = true
        fieldC?.isAccessible = true

        var modified = false
        val newMap = LinkedHashMap<Any?, Any?>()

        for ((key, item) in map) {
            if (item == null) {
                newMap[key] = null
                continue
            }

            val mode = fieldC?.get(item) as? Int
            val facing = fieldB?.get(item) as? Int
            val dList = try { fieldD.get(item) as? List<*> } catch (e: Throwable) { null }
            val eList = try { fieldE?.get(item) as? List<*> } catch (e: Throwable) { null }

            if (dList != null) {
                val floats = dList.filterIsInstance<Number>().map { it.toFloat() }
                val eFloats = eList?.filterIsInstance<Number>()?.map { it.toFloat() } ?: emptyList()

                if (DeviceHelper.isPortraitZoom(mode, facing, floats)) {
                    val targetD = DeviceHelper.getTargetPortraitFloats(floats)
                    val targetE = (eFloats + targetD).distinct().sorted()
                    if (floats != targetD) {
                        val patchedZoom = patchSingleZoomCam11(item, targetD, targetE, fieldD, fieldE)
                        if (patchedZoom != null) {
                            newMap[key] = patchedZoom
                            modified = true
                            continue
                        }
                    }
                } else if (DeviceHelper.is10xZoomEnabledForDevice() && DeviceHelper.hasValueList(floats, 5.0f) && !DeviceHelper.hasValueList(floats, 10.0f)) {
                    val targetD = floats.toMutableList().apply { if (!contains(10.0f)) add(10.0f) }.sorted()
                    val targetE = (eFloats + targetD).distinct().sorted()
                    val patchedZoom = patchSingleZoomCam11(item, targetD, targetE, fieldD, fieldE)
                    if (patchedZoom != null) {
                        newMap[key] = patchedZoom
                        modified = true
                        continue
                    }
                }
            }
            newMap[key] = item
        }

        return if (modified) newMap else null
    }
    private fun patchSingleZoomCam11(
        zoomObj: Any,
        newFloatsD: List<Float>,
        newFloatsE: List<Float>? = null,
        fieldD: java.lang.reflect.Field,
        fieldE: java.lang.reflect.Field? = null
    ): Any? {
        return try {
            val zoomClass = zoomObj.javaClass
            val iMethod = zoomClass.getMethod("I")
            val builder = iMethod.invoke(zoomObj)

            val fieldB = builder.javaClass.getField("b")
            val messageInBuilder = fieldB.get(builder)

            val defaultInstance = zoomClass.getField("DEFAULT_INSTANCE").get(null)
            val emptyListD = fieldD.get(defaultInstance)
            if (emptyListD != null) {
                fieldD.set(messageInBuilder, emptyListD)
            }

            val wMethod = builder.javaClass.getMethod("w", java.lang.Iterable::class.java)
            wMethod.invoke(builder, newFloatsD)

            if (fieldE != null && newFloatsE != null) {
                val emptyListE = fieldE.get(defaultInstance)
                if (emptyListE != null) {
                    fieldE.set(messageInBuilder, emptyListE)
                }
                val vMethod = builder.javaClass.getMethod("v", java.lang.Iterable::class.java)
                vMethod.invoke(builder, newFloatsE)
            }

            val buildMethod = builder.javaClass.getMethod("k")
            buildMethod.invoke(builder)
        } catch (e: Throwable) {
            null
        }
    }
}
