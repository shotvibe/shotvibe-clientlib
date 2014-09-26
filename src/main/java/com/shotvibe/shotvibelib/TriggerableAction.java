package com.shotvibe.shotvibelib;

public abstract class TriggerableAction<T> {
    public TriggerableAction() {
        mRunState = RunState.NOT_RUNNING;
    }

    public void trigger(ThreadUtil.Executor executor) {
        boolean shouldStart = false;
        synchronized (this) {
            switch (mRunState) {
                case NOT_RUNNING:
                    shouldStart = true;
                    mRunState = RunState.RUNNING;
                    break;
                case RUNNING:
                    mRunState = RunState.REFRESH_TRIGGERED;
                    break;
                case REFRESH_TRIGGERED:
                    // Nothing needs to be done
                    break;
            }
        }

        if (shouldStart) {
            executor.execute(new ThreadUtil.Runnable() {
                @Override
                public void run() {
                    boolean done = false;
                    T result = null; // Initialize to null to satisfy the compiler, even though it will definitely be initialized from runAction
                    while (!done) {
                        result = runAction();

                        synchronized (this) {
                            if (mRunState == RunState.RUNNING) {
                                mRunState = RunState.NOT_RUNNING;
                                done = true;
                            } else if (mRunState == RunState.REFRESH_TRIGGERED) {
                                mRunState = RunState.RUNNING;
                            } else {
                                throw new IllegalStateException("IllegalState: mRunState == " + mRunState);
                            }
                        }
                    }

                    actionComplete(result);
                }
            });
        }
    }

    public abstract T runAction();
    public void actionComplete(T result) { }

    private enum RunState {
        NOT_RUNNING,
        RUNNING,
        REFRESH_TRIGGERED
    }

    private RunState mRunState;
}
