package dev.gaphunter.micrometertimersamplecompanion.detect

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class KotlinTimerSampleFinderTest : BasePlatformTestCase() {

    fun `test sample started and never stopped is flagged`() {
        val file = myFixture.configureByText(
            "OrderService.kt",
            """
            class OrderService {
                fun placeOrder() {
                    val sample = Timer.start(registry)
                    doWork()
                }
            }
            """.trimIndent(),
        )
        assertEquals(1, KotlinTimerSampleFinder.findAll(file).size)
    }

    fun `test sample started and stopped is not flagged`() {
        val file = myFixture.configureByText(
            "OrderService.kt",
            """
            class OrderService {
                fun placeOrder() {
                    val sample = Timer.start(registry)
                    doWork()
                    sample.stop(registry.timer("order.place"))
                }
            }
            """.trimIndent(),
        )
        assertTrue(KotlinTimerSampleFinder.findAll(file).isEmpty())
    }

    fun `test sample stopped inside a branch is not flagged`() {
        val file = myFixture.configureByText(
            "OrderService.kt",
            """
            class OrderService {
                fun placeOrder(fast: Boolean) {
                    val sample = Timer.start(registry)
                    if (fast) {
                        sample.stop(registry.timer("order.place.fast"))
                    } else {
                        sample.stop(registry.timer("order.place.slow"))
                    }
                }
            }
            """.trimIndent(),
        )
        assertTrue(KotlinTimerSampleFinder.findAll(file).isEmpty())
    }

    fun `test sample stored as instance property is not flagged`() {
        val file = myFixture.configureByText(
            "OrderService.kt",
            """
            class OrderService(registry: MeterRegistry) {
                private val sample = Timer.start(registry)

                fun placeOrder() {
                    doWork()
                }
            }
            """.trimIndent(),
        )
        assertTrue(KotlinTimerSampleFinder.findAll(file).isEmpty())
    }
}
