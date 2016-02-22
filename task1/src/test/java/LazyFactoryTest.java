import org.junit.Test;
import static org.junit.Assert.*;

import java.util.concurrent.CyclicBarrier;
import java.util.function.Supplier;

public class LazyFactoryTest {

    private class CountedSupplier<T> implements Supplier<T> {

        private int count = 0;
        private final Supplier<T> sup;

        CountedSupplier(Supplier<T> sup) {
            this.sup = sup;
        }

        public int getCount() {
            return  count;
        }

        public T get() {
            count++;
            return sup.get();
        }
    }

    private class SingleThreadTester<T> {
        private final Supplier<T> supplier;
        private final T expected;

        public SingleThreadTester(Supplier<T> supplier, T expected) {
            this.supplier = supplier;
            this.expected = expected;
        }

        public void run() {
            CountedSupplier<T> countedSupplier = new CountedSupplier<>(supplier);
            Lazy<T> lazy = LazyFactory.createLazy(countedSupplier);

            assertEquals(0, countedSupplier.getCount());
            T first = lazy.get();
            assertEquals(expected, first);
            assertEquals(1, countedSupplier.getCount());

            assertSame(first, lazy.get());
            assertSame(first, lazy.get());

            assertEquals(1, countedSupplier.getCount());
        }
    }

    @Test
    public void testLazySingleThread() {
        new SingleThreadTester<>(() -> "abc", "abc").run();
    }

    @Test
    public void testNullLazySingleThread() {
        new SingleThreadTester<>(() -> null, null).run();
    }

    private class MultiThreadTester<T> {
        private final Lazy<T> lazy;
        private final T expected;
        private final int threadCount;


        public MultiThreadTester(Lazy<T> lazy, T expected, int threadCount) {
            this.lazy = lazy;
            this.expected = expected;
            this.threadCount = threadCount;
        }

        public void run(){
            CyclicBarrier barrier = new CyclicBarrier(threadCount);
            Thread[] threads = new Thread[threadCount];

            Object[] answers = new Object[threadCount];
            for (int i = 0; i < threadCount; i++) {
                final int t = i;
                threads[t] = new Thread(() -> {
                    try {
                        barrier.await();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    answers[t] = lazy.get();

                    assertEquals(expected, answers[t]);
                    assertSame(answers[t], lazy.get());
                });
                threads[t].start();
            }
            for (int i = 0; i < threadCount; i++) {
                try {
                    threads[i].join();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            for (int i = 1; i < threadCount; i++) {
                assertSame(answers[i - 1], answers[i]);
            }
        }
    }

    @Test
    public void testLazyMultiThread() {
        CountedSupplier<String> countedSupplier = new CountedSupplier<>(() -> "abc");
        Lazy<String> lazyStr = LazyFactory.createLazyMT(countedSupplier);

        assertEquals(0, countedSupplier.getCount());
        new MultiThreadTester<>(lazyStr, "abc",  20).run();
        assertEquals(1, countedSupplier.getCount());
    }


    @Test
    public void testLazyMultiThreadLockFree() {
        CountedSupplier<String> countedSupplier = new CountedSupplier<>(() -> "abc");
        Lazy<String> lazyStr = LazyFactory.createLazyLockFree(countedSupplier);

        assertEquals(0, countedSupplier.getCount());
        new MultiThreadTester<>(lazyStr, "abc", 20).run();
    }

}
