import org.junit.Test;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.concurrent.CyclicBarrier;
import java.util.function.Supplier;

public class LazyFactoryTest {

    public class CountedSupplier<T> implements Supplier<T> {

        private int count = 0;
        private Supplier<T> sup;

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

    @Test
    public void testReturnValue() {
        Supplier<String> s = () -> "abc";
        Lazy<String>  lazyStr = LazyFactory.createLazy(s);

        assertEquals(lazyStr.get(), s.get());
    }

    @Test
    public void testReturnNullValue() {
        Supplier<String> s = () -> null;
        Lazy<String>  lazyStr = LazyFactory.createLazy(s);

        assertEquals(lazyStr.get(), s.get());
    }

    @Test
    public void testSupplyValueOnes() {
        Supplier<String> sup = () -> "abc";
        CountedSupplier<String> countedSup = new CountedSupplier<>(sup);
        Lazy<String>  lazyStr = LazyFactory.createLazy(countedSup);
        lazyStr.get();
        lazyStr.get();

        assertEquals(lazyStr.get(), sup.get());
        assertTrue(countedSup.getCount() == 1);
    }

    @Test
    public void testSupplyNullValueOnes() {
        Supplier<String> sup = () -> null;
        CountedSupplier<String> countedSup = new CountedSupplier<>(sup);
        Lazy<String>  lazyStr = LazyFactory.createLazy(countedSup);
        lazyStr.get();
        lazyStr.get();

        assertEquals(lazyStr.get(), sup.get());
        assertTrue(countedSup.getCount() == 1);
    }

    @Test
    public void testReturnSameValueMT() {
        int n = 20;
        CyclicBarrier barrier = new CyclicBarrier(n);
        Thread[] threads = new Thread[n];

        Supplier<String> sup = () -> "abc";
        CountedSupplier<String> countedSup = new CountedSupplier<>(sup);
        Lazy<String> lazyStr = LazyFactory.createLazyMT(countedSup);

        String[] answers = new String[n];

        for (int i = 0; i < n; i++) {
            final int t = i;
            threads[t] = new Thread(() -> {
                try {
                    barrier.await();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                answers[t] = lazyStr.get();
            });
            threads[t].start();
        }
        for (int i = 0; i < n; i++) {
            try {
                threads[i].join();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        for (int i = 0; i < n; i++) {
            assertEquals("returned " + answers[i] ,answers[i], sup.get());
        }
        assertTrue(countedSup.getCount() == 1);
    }


    @Test
    public void testReturnSameValueLockFree() {
        int n = 20;
        CyclicBarrier barrier = new CyclicBarrier(n);
        Thread[] threads = new Thread[n];

        Supplier<String> sup = () -> "abc";
        Lazy<String> lazyStr = LazyFactory.createLazyLockFree(sup);

        String[] answers = new String[n];

        for (int i = 0; i < n; i++) {
            final int t = i;
            threads[t] = new Thread(() -> {
                try {
                    barrier.await();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                answers[t] = lazyStr.get();
                assertEquals(answers[t], "abc");
            });
            threads[t].start();
        }
        for (int i = 0; i < n; i++) {
            try {
                threads[i].join();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        for (int i = 0; i < n; i++) {
           assertEquals(answers[i], sup.get());
        }
    }

}
