package uk.co.oliverdelange.wcr_android_kt.factory

import timber.log.Timber
import uk.co.oliverdelange.wcr_android_kt.model.*

//TODO Test me

fun from(vGrade: VGrade): Grade {
    return Grade(vGrade.textRepresentation, GradeType.V, vGrade.colour)
}

fun from(fontGrade: FontGrade): Grade {
    return Grade(fontGrade.textRepresentation, GradeType.FONT, fontGrade.colour)
}

fun from(sportGrade: SportGrade): Grade {
    return Grade(sportGrade.textRepresentation, GradeType.SPORT, sportGrade.colour)
}

fun from(tradAdjectivalGrade: TradAdjectivalGrade, tradTechnicalGrade: TradTechnicalGrade): Grade {
    return Grade(tradAdjectivalGrade.textRepresentation + " " + tradTechnicalGrade.textRepresentation,
            GradeType.TRAD,
            tradAdjectivalGrade.colour
    )
}

fun from(textRepresentation: String): Grade? {
    Timber.v("Converting $textRepresentation into a grade")
    return when {
        textRepresentation.startsWith("f") -> from(FontGrade.values().first { it.textRepresentation == textRepresentation })
        textRepresentation.contains(" ") -> {
            val grades = textRepresentation.split(" ")
            val adj = TradAdjectivalGrade.values().first { it.textRepresentation == grades[0] }
            val tech = TradTechnicalGrade.values().first { it.textRepresentation == grades[1] }
            from(adj, tech)
        }
        textRepresentation.matches(Regex("V\\d+")) -> from(VGrade.values().first { it.textRepresentation == textRepresentation })
        else -> from(SportGrade.values().first { it.textRepresentation == textRepresentation })
    }
}