package dev.kobalt.holdem.android.home

import com.zhuinden.simplestackextensions.fragments.DefaultFragmentKey
import kotlinx.parcelize.Parcelize

@Parcelize
data class HomeKey(private val placeholder: String = "") :
    DefaultFragmentKey() { // generate reliable `toString()` for no-args data class
    override fun instantiateFragment() = HomeFragment()
}