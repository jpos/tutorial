package org.jpos.tutorial;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.MUX;
import org.jpos.q2.QBeanSupport;
import org.jpos.util.ISOUtil;
import org.jpos.util.NameRegistrar;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Periodically sends a 2800 Network Management Request via QMUX
 * and logs the response.
 */
public class SendRequest extends QBeanSupport implements Runnable {
    private final AtomicInteger stan = new AtomicInteger(0);
    private long interval;
    private long timeout;
    private String muxName;

    @Override
    protected void initService() {
        muxName  = cfg.get("mux",      "upstream");
        interval = cfg.getLong("interval", 60000L);
        timeout  = cfg.getLong("timeout",  30000L);
    }

    @Override
    protected void startService() {
        new Thread(this, "send-request").start();
    }

    @Override
    public void run() {
        while (running()) {
            try {
                MUX mux = (MUX) NameRegistrar.get("mux." + muxName);
                ISOMsg m = new ISOMsg();
                m.setMTI("2800");
                m.set(11, ISOUtil.zeropad(String.valueOf(stan.incrementAndGet()), 6));
                m.set(70, "301");  // Echo test

                getLog().info("sending 2800, STAN=" + m.getString(11));
                ISOMsg resp = mux.request(m, timeout);

                if (resp != null) {
                    getLog().info("response 2810, STAN=" + resp.getString(11)
                        + " rc=" + resp.getString(39));
                } else {
                    getLog().warn("2800 timed out after " + timeout + "ms");
                }
            } catch (NameRegistrar.NotFoundException e) {
                getLog().warn("MUX '" + muxName + "' not found, will retry");
            } catch (ISOException e) {
                getLog().warn(e);
            }
            ISOUtil.sleep(interval);
        }
    }
}
