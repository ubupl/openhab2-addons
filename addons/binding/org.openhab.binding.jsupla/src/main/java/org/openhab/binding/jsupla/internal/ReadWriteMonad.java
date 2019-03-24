package org.openhab.binding.jsupla.internal;

import org.eclipse.jdt.annotation.NonNull;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

public final class ReadWriteMonad<T> {
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final T t;

    public ReadWriteMonad(@NonNull final T t) {
        this.t = requireNonNull(t);
    }

    public void doInWriteLock(Consumer<T> consumer) {
        lock.writeLock().lock();
        try {
            consumer.accept(t);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void doInReadLock(Consumer<T> consumer) {
        lock.readLock().lock();
        try {
            consumer.accept(t);
        } finally {
            lock.readLock().unlock();
        }
    }
}
