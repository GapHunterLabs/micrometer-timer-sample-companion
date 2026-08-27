package dev.gaphunter.micrometertimersamplecompanion.detect

import com.intellij.psi.JavaRecursiveElementWalkingVisitor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiLocalVariable
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.util.PsiTreeUtil
import dev.gaphunter.micrometertimersamplecompanion.model.TimerSampleHit

/**
 * Finds a local variable initialized with `Timer.start(...)` (Micrometer)
 * whose declaring method body never calls `.stop(...)` on that same
 * variable anywhere -- Micrometer's own reference docs show the sample
 * usage as exactly two steps, `Timer.Sample sample = Timer.start(registry);`
 * followed later by `sample.stop(registry.timer(...))`, with no mention of
 * a try/finally guard. A sample that's started but never stopped never
 * records its timing and (per a real upstream Micrometer issue) can leak
 * memory in long-task-timer bookkeeping if this happens repeatedly.
 *
 * **v0.1 scope, stated honestly:** flags only the fully-unambiguous case --
 * `.stop(...)` is never called on the variable *anywhere* in the method,
 * not even inside a branch. If `.stop(...)` is called on any path (even
 * one that doesn't cover every branch, e.g. only inside an `if`), this is
 * NOT flagged -- true control-flow/exception-path analysis is out of scope
 * for a v0.1 static text/PSI scanner, and a false "still leaks on the
 * exception path" positive is worse than staying silent on partial
 * coverage. Matches by simple method name (`start`/`stop`), not real type
 * resolution -- an unrelated `start()`/`stop()` pair on some other type
 * named `sample` is a possible (rare) false positive.
 */
object JavaTimerSampleFinder {

    fun findAll(file: PsiFile): List<TimerSampleHit> {
        val hits = mutableListOf<TimerSampleHit>()
        file.accept(object : JavaRecursiveElementWalkingVisitor() {
            override fun visitLocalVariable(variable: PsiLocalVariable) {
                super.visitLocalVariable(variable)
                hitFor(variable)?.let { hits += it }
            }
        })
        return hits
    }

    private fun hitFor(variable: PsiLocalVariable): TimerSampleHit? {
        val initializer = variable.initializer as? PsiMethodCallExpression ?: return null
        if (initializer.methodExpression.referenceName != "start") return null
        val qualifier = initializer.methodExpression.qualifierExpression ?: return null
        if (qualifier.text != "Timer") return null

        val varName = variable.name
        val containingMethod = PsiTreeUtil.getParentOfType(variable, PsiMethod::class.java) ?: return null
        val methodBody = containingMethod.body ?: return null

        var stopsVariable = false
        methodBody.accept(object : JavaRecursiveElementWalkingVisitor() {
            override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                super.visitMethodCallExpression(expression)
                if (expression.methodExpression.referenceName != "stop") return
                val stopQualifier = expression.methodExpression.qualifierExpression ?: return
                if (stopQualifier.text == varName) stopsVariable = true
            }
        })
        if (stopsVariable) return null

        return TimerSampleHit(leafOf(initializer))
    }

    /** Descends to a real leaf PSI element -- LineMarkerInfo must never anchor on a composite node. */
    private fun leafOf(element: PsiElement): PsiElement {
        var current = element
        while (current.firstChild != null) current = current.firstChild
        return current
    }
}
