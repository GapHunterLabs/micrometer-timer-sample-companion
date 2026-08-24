package dev.gaphunter.micrometertimersamplecompanion.detect

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class JavaTimerSampleFinderTest : BasePlatformTestCase() {

    fun `test sample started and never stopped is flagged`() {
        val file = myFixture.configureByText(
            "OrderService.java",
            """
            class OrderService {
                void placeOrder() {
                    Timer.Sample sample = Timer.start(registry);
                    doWork();
                }
            }
            """.trimIndent(),
        )
        assertEquals(1, JavaTimerSampleFinder.findAll(file).size)
    }

    fun `test sample started and stopped is not flagged`() {
        val file = myFixture.configureByText(
            "OrderService.java",
            """
            class OrderService {
                void placeOrder() {
                    Timer.Sample sample = Timer.start(registry);
                    doWork();
                    sample.stop(registry.timer("order.place"));
                }
            }
            """.trimIndent(),
        )
        assertTrue(JavaTimerSampleFinder.findAll(file).isEmpty())
    }

    fun `test sample stopped inside a branch is not flagged`() {
        val file = myFixture.configureByText(
            "OrderService.java",
            """
            class OrderService {
                void placeOrder(boolean fast) {
                    Timer.Sample sample = Timer.start(registry);
                    if (fast) {
                        sample.stop(registry.timer("order.place.fast"));
                    } else {
                        sample.stop(registry.timer("order.place.slow"));
                    }
                }
            }
            """.trimIndent(),
        )
        assertTrue(JavaTimerSampleFinder.findAll(file).isEmpty())
    }

    fun `test unrelated start-stop pair on a different type is not flagged`() {
        val file = myFixture.configureByText(
            "Scheduler.java",
            """
            class Scheduler {
                void run() {
                    Job sample = Job.start(config);
                    sample.execute();
                }
            }
            """.trimIndent(),
        )
        assertTrue(JavaTimerSampleFinder.findAll(file).isEmpty())
    }
}
