package org.jpos.tutorial;

import org.jdom2.Element;
import org.jpos.core.ConfigurationException;
import org.jpos.iso.*;
import org.jpos.q2.QBeanSupport;
import org.jpos.q2.iso.QMUX;
import org.jpos.space.Space;
import org.jpos.space.SpaceFactory;
import org.jpos.iso.ISOUtil;
import org.jpos.util.NameRegistrar;

import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Manages the logon/echo/logoff lifecycle for one or more MUXes.
 *
 * For each configured MUX this bean starts a Runner thread that:
 *  1. Waits until the channel is up (ready semaphore present in Space).
 *  2. Sends a logon request; on success records the session ID with a TTL.
 *  3. Sends periodic echo requests to keep the session alive.
 *  4. On stop, sends a logoff and exits.
 *
 * Re-logon is triggered automatically when:
 *  - The channel reconnects (session ID changes).
 *  - The logon TTL expires (logon-interval elapses without a reconnect).
 */
public class LogonManager extends QBeanSupport {

    private Space<String, Object> sp;
    private long timeout;
    private long echoInterval;
    private long logonInterval;
    private long initialDelay;
    private ISOMsg logonTemplate;
    private ISOMsg echoTemplate;
    private ISOMsg logoffTemplate;

    private CountDownLatch shutDownLatch;
    private CountDownLatch allDone;

    private final AtomicLong stan = new AtomicLong(0);

    // Space key prefixes — unique per LogonManager name + ready indicator
    private static final String LOGON_PREFIX = "logon-manager.logon.";
    private static final String ECHO_PREFIX  = "logon-manager.echo.";

    @Override
    @SuppressWarnings("unchecked")
    protected void initService() throws ConfigurationException {
        sp           = SpaceFactory.getSpace();
        timeout      = cfg.getLong("timeout",        30000L);
        echoInterval = cfg.getLong("echo-interval",  60000L);
        logonInterval= cfg.getLong("logon-interval", 86400000L);
        initialDelay = cfg.getLong("initial-delay",  1000L);

        Element persist = getPersist();
        logonTemplate  = readTemplate(persist, "logon");
        echoTemplate   = readTemplate(persist, "echo");
        logoffTemplate = readTemplate(persist, "logoff");
    }

    @Override
    protected void startService() {
        String[] muxes = cfg.getAll("mux");
        shutDownLatch  = new CountDownLatch(1);
        allDone        = new CountDownLatch(muxes.length);
        for (String mux : muxes)
            new Thread(new Runner(mux), getName() + "-" + mux).start();
    }

    @Override
    protected void stopService() {
        shutDownLatch.countDown();
        try {
            allDone.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) { }
    }

    private void relax(long ms) {
        try {
            shutDownLatch.await(ms, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ignored) { }
    }

    // ── Runner ────────────────────────────────────────────────────────────────

    public class Runner implements Runnable {
        private final String muxName;
        private MUX mux;
        private String readyKey;
        private String logonKey;
        private String echoKey;

        public Runner(String muxName) {
            this.muxName = muxName;
            try {
                mux = (MUX) NameRegistrar.get("mux." + muxName);
                String[] indicators = ((QMUX) mux).getReadyIndicatorNames();
                if (indicators != null && indicators.length > 0) {
                    readyKey = indicators[0];
                    logonKey = LOGON_PREFIX + readyKey + "." + getName();
                    echoKey  = ECHO_PREFIX  + readyKey + "." + getName();
                } else {
                    getLog().error("No ready indicator configured for mux: " + muxName);
                }
            } catch (NameRegistrar.NotFoundException e) {
                getLog().error("MUX not found: " + muxName);
            }
        }

        @Override
        public void run() {
            while (running() && readyKey != null) {
                // Block until the ChannelAdaptor publishes its ready semaphore
                Object sessionId = sp.rd(readyKey, 60000);
                if (sessionId == null) {
                    getLog().info("Channel not ready yet: " + readyKey);
                    continue;
                }

                try {
                    if (logonRequired(sessionId)) {
                        doLogon(sessionId);
                    } else if (echoRequired()) {
                        doEcho();
                    }
                } catch (Exception e) {
                    getLog().warn(e);
                }
                relax(1000);
            }

            // Clean shutdown — send logoff before the thread exits
            try {
                doLogoff();
            } catch (Exception e) {
                getLog().warn(e);
            }
            allDone.countDown();
        }

        /**
         * Logon is required when the current session ID differs from the one
         * recorded at the last successful logon. This fires on first connect,
         * on reconnect (new Date in Space), and after logon-interval expiry
         * (Space entry gone → rdp returns null → never equals sessionId).
         */
        private boolean logonRequired(Object sessionId) {
            return !sessionId.equals(sp.rdp(logonKey));
        }

        private boolean echoRequired() {
            return sp.rdp(echoKey) == null;
        }

        private void doLogon(Object sessionId) throws ISOException {
            ISOMsg resp = mux.request(buildMsg(logonTemplate), timeout);
            if (resp != null && "0000".equals(resp.getString(39))) {
                sp.inp(logonKey);                              // clear stale entry if any
                sp.out(logonKey, sessionId, logonInterval);    // record session with TTL
                getLog().info("Logon OK — " + muxName + " (session=" + sessionId + ")");
                relax(initialDelay);
            } else {
                getLog().warn("Logon failed on " + muxName
                    + (resp != null ? " rc=" + resp.getString(39) : " (no response)"));
            }
        }

        private void doEcho() throws ISOException {
            ISOMsg resp = mux.request(buildMsg(echoTemplate), timeout);
            if (resp != null) {
                sp.inp(echoKey);                               // clear stale entry if any
                sp.out(echoKey, new Object(), echoInterval);   // renew TTL
                getLog().info("Echo OK — " + muxName);
            } else {
                getLog().warn("Echo timed out on " + muxName);
            }
        }

        private void doLogoff() throws ISOException {
            if (logoffTemplate == null) return;
            getLog().info("Logging off " + muxName);
            sp.inp(logonKey);   // wipe logon marker so a future restart requires re-logon
            mux.request(buildMsg(logoffTemplate), timeout);
        }
    }

    // ── Message building ──────────────────────────────────────────────────────

    private ISOMsg buildMsg(ISOMsg template) throws ISOException {
        ISOMsg m = (ISOMsg) template.clone();
        m.set(7,  ISODate.getDateTime(new Date()));
        m.set(11, ISOUtil.zeropad(Long.toString(stan.incrementAndGet() % 1_000_000L), 6));
        return m;
    }

    /**
     * Reads a simple message template from an XML element.
     *
     * Expected format:
     * <pre>
     *   &lt;logon&gt;
     *     &lt;field id="0"  value="2800" /&gt;
     *     &lt;field id="70" value="001"  /&gt;
     *   &lt;/logon&gt;
     * </pre>
     */
    private ISOMsg readTemplate(Element config, String name) throws ConfigurationException {
        Element e = config.getChild(name);
        if (e == null) return null;
        try {
            ISOMsg m = new ISOMsg();
            for (Element field : e.getChildren("field")) {
                int    id  = Integer.parseInt(field.getAttributeValue("id"));
                String val = field.getAttributeValue("value");
                if (id == 0) m.setMTI(val);
                else         m.set(id, val);
            }
            return m;
        } catch (Exception ex) {
            throw new ConfigurationException("Can't parse '" + name + "' template", ex);
        }
    }
}
