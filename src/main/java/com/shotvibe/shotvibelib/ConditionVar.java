package com.shotvibe.shotvibelib;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ConditionVar {
    public ConditionVar() {
        mLock = new ReentrantLock();
        mCondition = mLock.newCondition();
    }

    public void lock() {
        mLock.lock();
    }

    public void unlock() {
        mLock.unlock();
    }

    public void await() {
        try {
            mCondition.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void signal() {
        mCondition.signal();
    }

    public void signalAll() {
        mCondition.signalAll();
    }

    private Lock mLock;
    private Condition mCondition;
}
