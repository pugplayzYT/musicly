package com.puggables.musically.ui.common

import androidx.navigation.NavController
import androidx.navigation.NavOptions
import com.puggables.musically.R

fun NavController.goHomeClearingBackStack() {
    val opts = NavOptions.Builder()
        .setPopUpTo(R.id.nav_graph, true) // nuke auth from backstack
        .build()
    this.navigate(R.id.action_global_homeFragment, null, opts)
}
