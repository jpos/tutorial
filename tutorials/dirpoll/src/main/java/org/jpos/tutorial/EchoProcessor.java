/*
 * jPOS Project [http://jpos.org]
 * Copyright (C) 2000-2026 jPOS Software SRL
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.jpos.tutorial;

import org.jpos.util.DirPoll;

import java.nio.charset.StandardCharsets;

/**
 * A simple {@link DirPoll.Processor} that echoes back each request.
 *
 * <p>For every file that lands in the {@code request/} directory, DirPoll
 * calls {@link #process(String, byte[])} with the file name and its raw
 * content.  Whatever this method returns is written to the {@code response/}
 * directory under the same base name.  Returning {@code null} suppresses
 * the response file.
 */
public class EchoProcessor implements DirPoll.Processor {

    /**
     * Echo the request content back as a response.
     *
     * @param name    the request file name (e.g. {@code "order-123.req"})
     * @param request the raw bytes of the request file
     * @return response bytes to be written to the {@code response/} directory
     * @throws DirPoll.DirPollException on processing errors;
     *         set {@code retry=true} to have DirPoll requeue the request
     */
    @Override
    public byte[] process(String name, byte[] request) throws DirPoll.DirPollException {
        String content = new String(request, StandardCharsets.UTF_8).trim();
        String response = "ECHO [" + name + "]: " + content + System.lineSeparator();
        return response.getBytes(StandardCharsets.UTF_8);
    }
}
