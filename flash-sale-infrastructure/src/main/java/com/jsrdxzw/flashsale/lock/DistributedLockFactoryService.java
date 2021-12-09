package com.jsrdxzw.flashsale.lock;

public interface DistributedLockFactoryService {
    DistributedLock getDistributedLock(String key);
}
