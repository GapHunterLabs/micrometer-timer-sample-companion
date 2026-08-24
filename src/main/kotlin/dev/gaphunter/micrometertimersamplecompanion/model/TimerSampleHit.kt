package dev.gaphunter.micrometertimersamplecompanion.model

import com.intellij.psi.PsiElement

/** One `Timer.start(...)` call assigned to a local variable whose declaring method/function never calls `.stop(...)` on it. */
data class TimerSampleHit(val callElement: PsiElement)
