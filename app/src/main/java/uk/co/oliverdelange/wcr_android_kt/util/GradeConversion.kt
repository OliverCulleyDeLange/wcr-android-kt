package uk.co.oliverdelange.wcr_android_kt.util

import uk.co.oliverdelange.wcr_android_kt.model.FontGrade
import uk.co.oliverdelange.wcr_android_kt.model.FontGrade.*
import uk.co.oliverdelange.wcr_android_kt.model.VGrade
import uk.co.oliverdelange.wcr_android_kt.model.VGrade.*

fun fontToV(fGrade: FontGrade): VGrade {
    when (fGrade) {
        fThree -> return VGrade.VB
        fThreeP, fFour, fFourP -> return VGrade.V0
        fFive -> return VGrade.V1
        fFiveP -> return VGrade.V2
        fSixA, fSixAP -> return VGrade.V3
        fSixB, fSixBP -> return VGrade.V4
        fSixC, fSixCP -> return VGrade.V5
        fSevenA -> return VGrade.V6
        fSevenAP -> return VGrade.V7
        fSevenB, fSevenBP -> return VGrade.V8
        fSevenC -> return VGrade.V9
        fSevenCP -> return VGrade.V10
        fEightA -> return VGrade.V11
        fEightAP -> return VGrade.V12
        fEightB -> return VGrade.V13
        fEightBP -> return VGrade.V14
        fEightC -> return VGrade.V15
        fEightCP -> return VGrade.V15
        else -> return VGrade.VB
    }
}

fun vToFont(vGrade: VGrade): FontGrade {
    when (vGrade) {
        VB -> return FontGrade.fThree
        V0 -> return FontGrade.fFour
        V1 -> return FontGrade.fFive
        V2 -> return FontGrade.fFiveP
        V3 -> return FontGrade.fSixA
        V4 -> return FontGrade.fSixB
        V5 -> return FontGrade.fSixC
        V6 -> return FontGrade.fSevenA
        V7 -> return FontGrade.fSevenAP
        V8 -> return FontGrade.fSevenB
        V9 -> return FontGrade.fSevenC
        V10 -> return FontGrade.fSevenCP
        V11 -> return FontGrade.fEightA
        V12 -> return FontGrade.fEightAP
        V13 -> return FontGrade.fEightB
        V14 -> return FontGrade.fEightBP
        V15 -> return FontGrade.fEightC
        else -> return FontGrade.fThree // FIXME have an unknown grade state to fall back onto?
    }
}

