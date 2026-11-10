package com.example.telepse.ui.screen

/** Matches web: `shouldShowProgressAmounts = !hideProgressBarAmount`. */
fun shouldShowProgressBarAmounts(hideProgressBarAmount: Boolean?): Boolean {
    return hideProgressBarAmount != true
}
