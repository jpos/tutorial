package org.jpos.tutorial;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.transaction.Context;
import org.jpos.transaction.TransactionParticipant;

import java.io.Serializable;
import java.util.Set;

import static org.jpos.transaction.ContextConstants.REQUEST;
import static org.jpos.transaction.ContextConstants.RESPONSE;

/**
 * Handles Network Management Messages (NMM) — MTIs 0800/2800.
 *
 * Approved function codes (field 70):
 *   001 — Sign-On
 *   002 — Sign-Off
 *   301 — Echo test
 *
 * Any other function code is declined with rc=9100 and the
 * transaction is ABORTED so that SendResponse (an AbortParticipant)
 * can still send the decline back to the caller.
 */
public class HandleNMM implements TransactionParticipant {

    private static final Set<String> APPROVED = Set.of("001", "002", "301");

    @Override
    public int prepare(long id, Serializable context) {
        Context ctx = (Context) context;
        try {
            ISOMsg req = ctx.get(REQUEST.toString());
            if (req == null) {
                ctx.log("REQUEST not found in context");
                return ABORTED | READONLY;
            }

            ISOMsg resp = (ISOMsg) req.clone();
            resp.setResponseMTI();

            String f70 = req.getString(70);
            if (APPROVED.contains(f70)) {
                resp.set(39, "0000");
                ctx.put(RESPONSE.toString(), resp);
                ctx.log("approve mti=" + resp.getMTI() + " f70=" + f70);
                return PREPARED | READONLY;
            } else {
                resp.set(39, "9100");   // function not supported
                ctx.put(RESPONSE.toString(), resp);
                ctx.log("decline mti=" + resp.getMTI() + " f70=" + f70 + " (unknown function code)");
                return ABORTED | READONLY;  // SendResponse fires via prepareForAbort
            }
        } catch (ISOException e) {
            ctx.log(e);
            return ABORTED | READONLY;
        }
    }
}
