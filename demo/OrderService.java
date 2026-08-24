// Demo data for Micrometer Timer Sample Companion -- used with
// `./gradlew runIde` to capture the real Marketplace screenshot. Open
// this file, the warning should appear on the Timer.start() line.

class OrderService {

    void placeOrderUnsafely(Order order) {
        // Sample started here but .stop(...) is never called anywhere
        // in this method -- FLAGGED. The timing for this order is
        // silently never recorded.
        Timer.Sample sample = Timer.start(registry);
        repository.save(order);
    }

    void placeOrderSafely(Order order) {
        Timer.Sample sample = Timer.start(registry);
        repository.save(order);
        // Matching stop() call present -- NOT flagged.
        sample.stop(registry.timer("order.place", "status", "ok"));
    }
}
