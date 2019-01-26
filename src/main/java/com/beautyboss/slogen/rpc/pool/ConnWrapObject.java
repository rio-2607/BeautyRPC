package com.beautyboss.slogen.rpc.pool;

/**
 * Author : Slogen
 * Date   : 2019/1/26
 */
public class ConnWrapObject<T> {
    private final T object;
    private volatile ObjectState state = ObjectState.IDLE; // 1:idle, 2:borrowed
    private final long createTime = System.currentTimeMillis();
    private volatile long lastBorrowTime = createTime;
    private volatile long lastUseTime = createTime;
    private volatile long lastReturnTime = createTime;
    private volatile long borrowedCount = 0;

    public ConnWrapObject(T object) {
        this.object = object;
    }

    public T getObject()
    {
        return object;
    }

    public long getCreateTime()
    {
        return createTime;
    }

    public long getActiveTimeMillis()
    {
        // Take copies to avoid threading issues
        long rTime = lastReturnTime;
        long bTime = lastBorrowTime;

        if (rTime > bTime) {
            return rTime - bTime;
        } else {
            return System.currentTimeMillis() - bTime;
        }
    }

    public long getIdleTimeMillis()
    {
        final long elapsed = System.currentTimeMillis() - lastReturnTime;
        // elapsed may be negative if:
        // - another thread updates lastReturnTime during the calculation window
        // - System.currentTimeMillis() is not monotonic (e.g. system time is
        // set back)
        return elapsed >= 0 ? elapsed : 0;
    }

    public long getLastBorrowTime()
    {
        return lastBorrowTime;
    }

    public long getLastReturnTime()
    {
        return lastReturnTime;
    }

    public long getBorrowedCount()
    {
        return borrowedCount;
    }

    public long getLastUsedTime()
    {
        return lastUseTime;
    }

    enum ObjectState {
        IDLE, ALLOCATED
    }

    public synchronized boolean allocate()
    {
        if (state == ObjectState.IDLE) {
            state = ObjectState.ALLOCATED;
            lastBorrowTime = System.currentTimeMillis();
            lastUseTime = lastBorrowTime;
            borrowedCount++;
            return true;
        }

        return false;
    }

    public synchronized boolean deallocate()
    {
        if (state == ObjectState.ALLOCATED) {
            state = ObjectState.IDLE;
            lastReturnTime = System.currentTimeMillis();
            return true;
        }

        return false;
    }

    public ObjectState getState()
    {
        return state;
    }

    public void setState(ObjectState state)
    {
        this.state = state;
    }
}
