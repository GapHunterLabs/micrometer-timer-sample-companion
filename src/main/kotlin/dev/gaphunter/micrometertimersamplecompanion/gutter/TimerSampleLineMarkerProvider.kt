package dev.gaphunter.micrometertimersamplecompanion.gutter

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement
import dev.gaphunter.micrometertimersamplecompanion.detect.JavaTimerSampleFinder
import dev.gaphunter.micrometertimersamplecompanion.detect.KotlinTimerSampleFinder
import dev.gaphunter.micrometertimersamplecompanion.model.TimerSampleHit
import dev.gaphunter.micrometertimersamplecompanion.review.ReviewPrompt

class TimerSampleLineMarkerProvider : LineMarkerProviderDescriptor(), DumbAware {

    override fun getName(): String = "Micrometer Timer.Sample never stopped"

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? = null

    override fun collectSlowLineMarkers(elements: MutableList<out PsiElement>, result: MutableCollection<in LineMarkerInfo<*>>) {
        val file = elements.firstOrNull()?.containingFile ?: return
        val hits = when (file.language.id) {
            "JAVA" -> JavaTimerSampleFinder.findAll(file)
            "kotlin" -> KotlinTimerSampleFinder.findAll(file)
            else -> emptyList()
        }
        if (hits.isEmpty()) return

        val hitsByElement = hits.associateBy { it.callElement }
        for (element in elements) {
            val hit = hitsByElement[element] ?: continue
            result.add(buildMarker(hit))

            val path = file.virtualFile?.path ?: continue
            val lineNumber = file.viewProvider.document?.getLineNumber(element.textRange.startOffset) ?: -1
            ReviewPrompt.recordHit(file.project, "$path:$lineNumber")
        }
    }

    private fun buildMarker(hit: TimerSampleHit): LineMarkerInfo<PsiElement> {
        val tooltip = "This Timer.Sample is started here but .stop(...) is never called on it anywhere in this method -- " +
            "the timing is never recorded, and per a real Micrometer issue, samples that are started but never " +
            "stopped can leak in long-task-timer bookkeeping if this happens repeatedly"
        return LineMarkerInfo(
            hit.callElement,
            hit.callElement.textRange,
            TimerSampleIcons.RISK,
            { _: PsiElement -> tooltip },
            null,
            GutterIconRenderer.Alignment.RIGHT,
            { tooltip },
        )
    }
}
