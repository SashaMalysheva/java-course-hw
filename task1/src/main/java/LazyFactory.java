import java.util.concurrent.atomic.AtomicMarkableReference;
import java.util.function.Supplier;

public class LazyFactory {

    public static <T> Lazy<T> createLazy(final Supplier<T> supplier) {
        return new Lazy<T>() {
            private boolean calc;
            private T res;

            public T get() {
                if (calc) {
                    return res;
                }
                res = supplier.get();
                calc = true;
                return res;
            }
        };
    }

    public static <T> Lazy<T> createLazyMT(final Supplier<T> supplier) {
        return new Lazy<T>() {
            private volatile boolean calc;
            private T res;

            public synchronized T get() {
                if (calc) {
                    return  res;
                }
                res = supplier.get();
                calc = true;
                return res;
            }
        };
    }

    public static <T> Lazy<T> createLazyLockFree(final Supplier<T> supplier) {
        return new Lazy<T>() {
            AtomicMarkableReference<T> res = new AtomicMarkableReference<>(null, false);

            public T get() {
                if (!res.isMarked()) {
                    res.set(supplier.get(), true);
                }
                return res.getReference();
            }
        };
    }
}
