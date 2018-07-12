package net.timzh;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

class Trigger {
    private CountDownLatch latch = new CountDownLatch(1);

    void activate() {
        latch.countDown();
    }

    void assertActivated() {
        try {
            boolean triggered = latch.await(1, TimeUnit.SECONDS);
            assertTrue(triggered);
        } catch (InterruptedException ignored) {}
    }
}