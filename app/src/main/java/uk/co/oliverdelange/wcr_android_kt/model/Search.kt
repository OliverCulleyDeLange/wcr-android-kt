package uk.co.oliverdelange.wcr_android_kt.model

import com.arlib.floatingsearchview.suggestions.model.SearchSuggestion
import kotlinx.android.parcel.Parcelize

enum class SearchResultType {
    CRAG, SECTOR, TOPO, ROUTE, ROUTE_BOULDER, ROUTE_TRAD, ROUTE_SPORT
}

@Parcelize
class SearchSuggestionItem(val name: String?, val type: SearchResultType, val id: Long?) : SearchSuggestion {

    override fun getBody(): String? {
        return name
    }
}