package com.jsrdxzw.flashsale.lock;

import java.util.concurrent.TimeUnit;

public interface DistributedLock {

    void lockInterruption(long leaseTime, TimeUnit unit) throws InterruptedException;

    boolean tryLock(long waitTime, long leaseTime, TimeUnit unit) throws InterruptedException;

    void lock(long leaseTime, TimeUnit unit);

    boolean forceUnlock();

    boolean isLocked();

    boolean isHeldByThread(long threadId);

    boolean isHeldByCurrentThread();

    int getHoldCount();

    long remainTimeToLive();
}
