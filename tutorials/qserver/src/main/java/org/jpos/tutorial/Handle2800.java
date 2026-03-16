package org.jpos.tutorial;

import org.jpos.iso.*;
import org.jpos.util.*;
import java.io.IOException;

public class Handle2800  extends Log implements ISORequestListener
{
    @Override
    public boolean process(ISOSource source, ISOMsg m) {
        try {
            if ("2800".equals(m.getMTI())) {
                ISOMsg r = (ISOMsg) m.clone();
                r.setResponseMTI();
                r.set(39, "0000");
                source.send(r);
                return true;
            }
        } catch (ISOException | IOException e) {
            warn(e);
        }
        return false;
    }
}
