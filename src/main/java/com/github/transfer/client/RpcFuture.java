package com.github.transfer.client;

import com.github.transfer.codec.Request;
import com.github.transfer.codec.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.ReentrantLock;
import lombok.extern.slf4j.Slf4j;

/**
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2022-10-22
 */
@Slf4j
public class RpcFuture implements Future<Object> {

    private final Request request;
    private Response response;
    private final long startTime;

    private static final long timeThreshold = 5000;

    private final Mutex mutex;

    private ReentrantLock lock = new ReentrantLock();

    private final List<Callback> pendingCallbacks = new ArrayList<>();

    private final ThreadPoolExecutor executor =
            new ThreadPoolExecutor(16, 16, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(65535));

    public RpcFuture(Request request) {
        this.request = request;
        this.startTime = System.currentTimeMillis();
        this.mutex = new Mutex();
    }

    public void done(Response resp) {
        this.response = resp;
        boolean success = mutex.release(1);
        if (success) {
            invokeCallback();
        }
        long endTime = System.currentTimeMillis() - startTime;
        if (endTime > timeThreshold) {
            log.warn("the rpc response is too long, requestID: " + this.request.getRequestID());
        }
    }

    private void invokeCallback() {
        lock.lock();
        try {
            pendingCallbacks.stream().peek(this::doCallback);
        } finally {
            lock.unlock();
        }
    }

    private void doCallback(Callback callback) {
        final Response response = this.response;
        executor.submit(() -> {
            if (response.getThrowable() == null) {
                callback.success(response.getResult());
            } else {
                callback.failure(response.getThrowable());
            }
        });
    }

    public RpcFuture addCallback(Callback callback) {
        lock.lock();
        try {
            if (isDone()) {
                doCallback(callback);
            } else {
                this.pendingCallbacks.add(callback);
            }
            return this;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCancelled() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isDone() {
        return mutex.isDone();
    }

    @Override
    public Object get() throws InterruptedException, ExecutionException {
        mutex.acquire(-1);
        if (this.response != null) {
            return this.response.getResult();
        }
        return null;
    }

    @Override
    public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        boolean success = mutex.tryAcquireNanos(-1, unit.toNanos(timeout));
        if (success) {
            if (this.response != null) {
                return this.response.getResult();
            }
            return null;
        }
        throw new RuntimeException(
                "timeout, requestID: " + this.request.getRequestID() + " classname: " + this.request.getClassName()
                        + " methodName: " + this.request.getMethodName());
    }

    static class Mutex extends AbstractQueuedSynchronizer {
        enum MutexState {
            DONE(1),
            PENDING(0);

            private final int value;

            MutexState(int value) {
                this.value = value;
            }
        }

        @Override
        protected boolean tryAcquire(int arg) {
            return getState() == MutexState.DONE.value;
        }

        @Override
        protected boolean tryRelease(int arg) {
            if (getState() == MutexState.PENDING.value) {
                return compareAndSetState(MutexState.PENDING.value, MutexState.DONE.value);
            }
            return false;
        }

        public boolean isDone() {
            return getState() == MutexState.DONE.value;
        }
    }
}
