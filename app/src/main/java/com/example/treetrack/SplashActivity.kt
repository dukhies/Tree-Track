package com.example.treetrack

import android.animation.Animator
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.airbnb.lottie.LottieAnimationView

class SplashActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val animationView = LottieAnimationView(this).apply {
            setAnimation("tree_animation.json")
            repeatCount = 0
            playAnimation()
        }

        setContentView(animationView)

        // Listen for when the animation ends
        animationView.addAnimatorListener(object : Animator.AnimatorListener {
            override fun onAnimationEnd(animation: Animator) {
                // Navigate to LoginActivity after animation finishes
                startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
                finish()
            }

            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}
        })
    }
}