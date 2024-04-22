package edu.utap.kal

import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import androidx.core.text.clearSpans
import java.io.Serializable

data class UserCard (
    val UID: String,
    val username: SpannableString,
    val bio: String,
//    val followingUIDs: List<String> = listOf(),
): Serializable {

    // NB: This changes the behavior of lists of RedditPosts.  I want posts fetched
    // at two different times to compare as equal.  By default, they will be different
    // objects with different hash codes.
//    override fun equals(other: Any?) : Boolean =
//        if (other is User) {
//            key == other.key
//        } else {
//            false
//        }

    // Most of the logic in this file comes from the Reddit HW

    private fun findAndSetSpan(fulltext: SpannableString, subtext: String): Boolean {
        if (subtext.isEmpty()) return true
        val i = fulltext.indexOf(subtext, ignoreCase = true)
        if (i == -1) return false
        fulltext.setSpan(
            ForegroundColorSpan(Color.CYAN), i, i + subtext.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return true
    }

    private fun clearSpan(str: SpannableString?) {
        str?.clearSpans()
        // This is here because I think going from one span to none
        // does not register as a change to the ListAdapter, so the
        // last searched for letter stays CYAN.  Not sure why
        str?.setSpan(
            ForegroundColorSpan(Color.GRAY), 0, 0,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    fun searchFor(searchTerm: String): Boolean {
        // clearSpan(username) // try moving to the end idk
        if (searchTerm.isEmpty()) {
            clearSpan(username)
            return false
        }
        return findAndSetSpan(username, searchTerm)
    }

}
