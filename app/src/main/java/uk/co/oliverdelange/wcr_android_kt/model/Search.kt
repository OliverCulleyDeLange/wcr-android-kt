package uk.co.oliverdelange.wcr_android_kt.model

import com.arlib.floatingsearchview.suggestions.model.SearchSuggestion
import kotlinx.android.parcel.Parcelize

@Parcelize
class SearchSuggestionItem(val name: String, val location: Location) : SearchSuggestion {

    override fun getBody(): String? {
        return name
    }
}