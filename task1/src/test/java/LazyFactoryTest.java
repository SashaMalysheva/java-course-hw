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
        int n = 10;
        CyclicBarrier barrier = new CyclicBarrier(n);
        Thread[] threads = new Thread[n];

        Supplier<String> sup = () -> "abc";
        CountedSupplier<String> countedSup = new CountedSupplier<>(sup);
        Lazy<String> lazyStr = LazyFactory.createLazyMT(countedSup);

        ArrayList<String> answers = new ArrayList<>(n);

        Runnable r = () -> {
            try {
                barrier.await();
            } catch (Exception e) {
                e.printStackTrace();
            }
            answers.add(lazyStr.get());
        };

        for (int i = 0; i < n; i++) {
            threads[i] = new Thread(r);
            threads[i].start();
        }
        for (int i = 0; i < n; i++) {
            try {
                threads[i].join();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        assertTrue(answers.size() == n);
        for (int i = 1; i < answers.size(); i++) {
            assertEquals(answers.get(i - 1), answers.get(i));
        }
        assertTrue(countedSup.getCount() == 1);
    }

    @Test
    public void testReturnSameValueLockFree() {
        int n = 10;
        CyclicBarrier barrier = new CyclicBarrier(n);
        Thread[] threads = new Thread[n];

        Supplier<String> sup = () -> "abc";
        Lazy<String> lazyStr = LazyFactory.createLazyLockFree(sup);

        ArrayList<String> answers = new ArrayList<>(n);

        Runnable r = () -> {
            try {
                barrier.await();
            } catch (Exception e) {
                e.printStackTrace();
            }
            answers.add(lazyStr.get());
        };

        for (int i = 0; i < n; i++) {
            threads[i] = new Thread(r);
            threads[i].start();
        }
        for (int i = 0; i < n; i++) {
            try {
                threads[i].join();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        assertTrue(answers.size() == n);
        for (int i = 1; i < answers.size(); i++) {
            assertEquals(answers.get(i - 1), answers.get(i));
        }
    }

}
