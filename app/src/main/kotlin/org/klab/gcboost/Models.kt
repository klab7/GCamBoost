package org.klab.gcboost

object Models {
    val modelSixSevenPro = arrayOf(Flags.fCameraCoach, Flags.fQuickAccess, Flags.fLooks, Flags.fMantisPortrait, Flags.fTenXZoom)
    val modelEightPro = arrayOf(Flags.fCameraCoach, Flags.fPhotoSphere, Flags.fNightSightVideo, Flags.fFourK60HDR, Flags.fEightK, Flags.fEightK24FPS, Flags.fProStableVideo, Flags.fAddMe, Flags.fQuickAccess, Flags.fLooks, Flags.fMantisPortrait, Flags.fTenXZoom)
    val modelEight = arrayOf(Flags.fCameraCoach, Flags.fPhotoSphere, Flags.fAddMe, Flags.fQuickAccess, Flags.fLooks, Flags.fProControls)
    val modelNinePro = arrayOf(Flags.fCameraCoach, Flags.fPhotoSphere, Flags.fEightK24FPS, Flags.fQuickAccess, Flags.fLooks, Flags.fMantisPortrait)
    val modelNine = arrayOf(Flags.fCameraCoach, Flags.fPhotoSphere, Flags.fQuickAccess, Flags.fLooks, Flags.fProControls)
    val modelTenPro = arrayOf(Flags.fPhotoSphere, Flags.fQuickAccess, Flags.fLooks, Flags.fMantisPortrait)
    val modelTen = arrayOf(Flags.fPhotoSphere, Flags.fQuickAccess, Flags.fLooks, Flags.fProControls)
    val modelElevenPro = arrayOf(Flags.fPhotoSphere)
    val modelEleven = arrayOf(Flags.fPhotoSphere, Flags.fProControls)
    val modelDefault = arrayOf(Flags.fCameraCoach, Flags.fQuickAccess, Flags.fLooks, Flags.fProControls)

    val modelSixSevenProFalse = emptyArray<Array<String>>()
    val modelEightProFalse = emptyArray<Array<String>>()
    val modelEightFalse = emptyArray<Array<String>>()
    val modelNineProFalse = emptyArray<Array<String>>()
    val modelNineFalse = emptyArray<Array<String>>()
    val modelTenProFalse = emptyArray<Array<String>>()
    val modelTenFalse = emptyArray<Array<String>>()
    val modelElevenProFalse = emptyArray<Array<String>>()
    val modelElevenFalse = emptyArray<Array<String>>()
    val modelDefaultFalse = emptyArray<Array<String>>()
}
