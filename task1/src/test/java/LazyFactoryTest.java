import org.junit.Test;
import static org.junit.Assert.*;

import java.util.concurrent.CyclicBarrier;
import java.util.function.Supplier;

public class LazyFactoryTest {

    public class CountedSupplier<T> implements Supplier<T> {

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

    class SingleThreadTester<T> {
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
            assertEquals(expected, lazy.get());
            assertEquals(1, countedSupplier.getCount());

            lazy.get();
            lazy.get();

            assertEquals(1, countedSupplier.getCount());
        }
    }

    @Test
    public void testLazySingleThread() {
        new SingleThreadTester<>(() -> "abc", "abc").run();
        new SingleThreadTester<>(() -> null, null).run();
    }

    class MultiThreadTester<T> {
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
                    assertEquals(expected, lazy.get());
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
        Supplier<String> sup = () -> "abc";
        CountedSupplier<String> countedSup = new CountedSupplier<>(sup);
        Lazy<String> lazyStr = LazyFactory.createLazyMT(countedSup);

        new MultiThreadTester<>(lazyStr, "abc",  20).run();

        assertEquals(1, countedSup.getCount());
    }


    @Test
    public void testLazyMultiThreadLockFree() {
        Supplier<String> sup = () -> "abc";
        Lazy<String> lazyStr = LazyFactory.createLazyLockFree(sup);

        new MultiThreadTester<>(lazyStr, "abc", 20).run();
    }

}
