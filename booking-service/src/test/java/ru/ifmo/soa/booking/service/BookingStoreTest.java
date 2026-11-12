package ru.ifmo.soa.booking.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookingStoreTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void cancelRemovesOnlySelectedPersonsBookings() {
        BookingStore store = store("single.db");

        assertTrue(store.sell(1, 10, 100));
        assertTrue(store.sell(2, 20, 200));
        assertFalse(store.sell(1, 30, 300));

        assertEquals(1, store.cancelByPerson(10));
        assertTrue(store.sell(1, 30, 300));
        assertFalse(store.sell(2, 30, 300));
    }

    @Test
    void instancesShareBookingsThroughDatabase() {
        BookingStore firstInstance = store("shared.db");
        BookingStore secondInstance = store("shared.db");

        assertTrue(firstInstance.sell(10, 100, 500));
        assertFalse(secondInstance.sell(10, 200, 600));

        assertEquals(1, secondInstance.cancelByPerson(100));
        assertTrue(firstInstance.sell(10, 200, 600));
    }

    @Test
    void concurrentInstancesCanSellTicketOnlyOnce() throws Exception {
        BookingStore firstInstance = store("concurrent.db");
        BookingStore secondInstance = store("concurrent.db");
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Future<Boolean> firstResult = executor.submit(() -> {
                start.await();
                return firstInstance.sell(20, 100, 500);
            });
            Future<Boolean> secondResult = executor.submit(() -> {
                start.await();
                return secondInstance.sell(20, 200, 600);
            });

            start.countDown();
            int successfulSales = (firstResult.get() ? 1 : 0)
                    + (secondResult.get() ? 1 : 0);
            assertEquals(1, successfulSales);
        } finally {
            executor.shutdownNow();
        }
    }

    private BookingStore store(String fileName) {
        return new BookingStore(temporaryDirectory.resolve(fileName));
    }
}
