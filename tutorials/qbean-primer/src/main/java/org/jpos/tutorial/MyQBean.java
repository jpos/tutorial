package org.jpos.tutorial;

import org.jpos.q2.QBean;

public class MyQBean implements MyQBeanMBean {
    int state;

    @Override
    public void init() {
        state = QBean.STARTING;
        printState();
    }

    @Override
    public void start() {
        state = QBean.STARTED;
        printState();
    }

    @Override
    public void stop()  {
        state = QBean.STOPPED;
        printState();
    }

    @Override
    public void destroy() {
        state = QBean.DESTROYED;
    }

    @Override
    public int getState() {
        return state;
    }

    @Override
    public String getStateAsString() {
        return state >= 0 ? QBean.stateString[state] : "Unknown";
    }

    private void printState() {
        System.out.println (getClass().getCanonicalName() + ":" + getStateAsString());
    }
}
