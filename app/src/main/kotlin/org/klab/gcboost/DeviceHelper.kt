package org.klab.gcboost

import android.os.Build
import kotlin.math.abs

object DeviceHelper {
    fun getDeviceFlags(isFalse: Boolean = false): Array<out Any> {
        val device = Build.DEVICE
        if (isFalse) {
            return when (device) {
                "raven", "cheetah", "felix" -> Models.modelSixSevenProFalse
                "husky" -> Models.modelEightProFalse
                "shiba", "akita" -> Models.modelEightFalse
                "komodo", "caiman", "comet" -> Models.modelNineProFalse
                "tokay", "tegu" -> Models.modelNineFalse
                "mustang", "blazer", "rango" -> Models.modelTenProFalse
                "frankel", "stallion" -> Models.modelTenFalse
                "grizzly", "kodiak", "yogi" -> Models.modelElevenProFalse
                "cubs" -> Models.modelElevenFalse
                else -> Models.modelDefaultFalse
            }
        }
        return when (device) {
            "raven", "cheetah", "felix" -> Models.modelSixSevenPro
            "husky" -> Models.modelEightPro
            "shiba", "akita" -> Models.modelEight
            "komodo", "caiman", "comet" -> Models.modelNinePro
            "tokay", "tegu" -> Models.modelNine
            "mustang", "blazer", "rango" -> Models.modelTenPro
            "frankel", "stallion" -> Models.modelTen
            "grizzly", "kodiak", "yogi" -> Models.modelElevenPro
            "cubs" -> Models.modelEleven
            else -> Models.modelDefault
        }
    }

    fun isTargetFlag(name: String): Boolean {
        return getDeviceFlags().any { group ->
            group is Array<*> && group.contains(name)
        }
    }

    fun isTargetFalseFlag(name: String): Boolean {
        return getDeviceFlags(isFalse = true).any { group ->
            group is Array<*> && group.contains(name)
        }
    }

    fun is10xZoomEnabledForDevice(): Boolean {
        return getDeviceFlags().any { group ->
            group is Array<*> && group.contains("pseudo_flag.10x_zoom")
        }
    }

    fun isPortraitZoomEnabledForDevice(): Boolean {
        return getDeviceFlags().any { group ->
            group === Flags.fMantisPortrait
        }
    }

    fun getTargetFloatFlag(name: String): Float? {
        for (group in getDeviceFlags()) {
            if (group is Map<*, *>) {
                val value = group[name]
                if (value is Float) {
                    return value
                }
            }
        }
        return null
    }

    fun getTargetIntFlag(name: String): Int? {
        for (group in getDeviceFlags()) {
            if (group is Map<*, *>) {
                val value = group[name]
                if (value is Int) {
                    return value
                }
            }
        }
        return null
    }

    fun isPortraitZoom(mode: Int?, facing: Int?, floats: List<Float>): Boolean {
        if (!isPortraitZoomEnabledForDevice()) return false
        if (mode == 21 && (facing == 1 || facing == null)) return true
        if (!hasValueList(floats, 0.5f) && (hasValueList(floats, 1.5f) || hasValueList(floats, 1.5172f) || (hasValueList(floats, 1.0f) && hasValueList(floats, 2.0f)))) {
            return true
        }
        return false
    }

    fun getTargetPortraitFloats(currentFloats: List<Float>): List<Float> {
        val countFive = currentFloats.count { isApprox(it, 5.0f) }
        val hasFive = hasValueList(currentFloats, 5.0f)
        return if (countFive > 1) {
            listOf(1.5f, 2.0f, 3.0f, 5.0f)
        } else if (hasFive) {
            listOf(1.5f, 2.0f, 3.0f, 5.0f)
        } else {
            listOf(1.5f, 2.0f, 3.0f)
        }
    }

    fun hasValueList(list: List<Float>, value: Float): Boolean {
        return list.any { isApprox(it, value) }
    }

    fun hasValueArr(arr: FloatArray, value: Float): Boolean {
        return arr.any { isApprox(it, value) }
    }

    fun isApprox(a: Float, b: Float): Boolean {
        return abs(a - b) < 0.01f
    }
}
