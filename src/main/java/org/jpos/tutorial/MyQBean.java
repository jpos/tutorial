package org.jpos.tutorial;

import org.jpos.q2.QBeanSupport;

public class MyQBean extends QBeanSupport {

    @Override
    protected void initService() {
        logState();
    }

    @Override
    protected void startService() {
        logState();
    }

    @Override
    protected void stopService()  {
        logState();
    }

    @Override
    protected void destroyService() {
        logState();
    }

    private void logState() {
        getLog().info (getName() + ":" + getStateAsString());
    }
}

