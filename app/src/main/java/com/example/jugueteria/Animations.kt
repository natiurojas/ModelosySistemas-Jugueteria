package com.example.jugueteria

import android.view.View

class Animations {
    fun playButtonPressAnimation(vararg views: View, action: () -> Unit) {
        views.forEach { view ->
            view.animate().cancel()

            view.animate()
                .scaleX(0.94f)
                .scaleY(0.94f)
                .translationY(3f)
                .setDuration(70)
                .setInterpolator(
                    android.view.animation.AccelerateInterpolator()
                )
                .start()
        }

        views.last().animate()
            .setDuration(70)
            .withEndAction {
                action()

                views.forEach { view ->
                    view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .translationY(0f)
                        .setDuration(220)
                        .setInterpolator(
                            android.view.animation.OvershootInterpolator(2f)
                        )
                        .start()
                }
            }
            .start()
    }
    fun animatePopupIn(view: View) {
        view.alpha = 0f
        view.scaleX = 0.85f
        view.scaleY = 0.85f

        view.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(250)
            .setInterpolator(
                android.view.animation.OvershootInterpolator(1.5f)
            )
            .start()
    }
    // Animacion de los Popups o pantallas emergentes al aparecer
    fun animatePopupOut(view: View, onEnd: () -> Unit) {
        view.animate()
            .alpha(0f)
            .scaleX(0.9f)
            .scaleY(0.9f)
            .setDuration(150)
            .setInterpolator(
                android.view.animation.AccelerateInterpolator()
            )
            .withEndAction {
                onEnd()
            }
            .start()
    }
}