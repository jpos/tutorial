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
import org.jpos.util.LogEvent;
import org.jpos.util.Logger;
import org.jpos.util.SimpleLogSource;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * A {@link DirPoll.FileProcessor} that upper-cases the content of each
 * incoming file and logs the result via the jPOS logger.
 *
 * <p>Implementing {@link org.jpos.util.LogSource} (via {@link SimpleLogSource})
 * allows {@code DirPollAdaptor} to inject the Q2 logger automatically.
 * Using {@link Logger#log} directly is the idiomatic jPOS approach: it
 * produces properly structured log events with realm and timestamp, and
 * avoids the 500 ms batching delay of {@code LogEventOutputStream} that
 * backs the {@code redirect=stdout} capture mechanism.
 *
 * <p>{@link DirPoll.FileProcessor} receives a {@link File} handle pointing
 * to the request file after DirPoll has moved it into the {@code run/}
 * directory.  DirPoll archives or deletes the file once
 * {@link #process(File)} returns normally.
 *
 * <p>Unlike {@link DirPoll.Processor}, a {@code FileProcessor} does not
 * return a byte array.  If you need to write a response file, create it
 * directly in the {@code response/} directory from within this method.
 */
public class UpperCaseFileProcessor extends SimpleLogSource
        implements DirPoll.FileProcessor {

    /**
     * Upper-case the content of the request file and log the result.
     *
     * @param file the request file, already moved to the {@code run/} directory
     * @throws DirPoll.DirPollException on processing errors;
     *         set {@code retry=true} to have DirPoll requeue the request
     */
    @Override
    public void process(File file) throws DirPoll.DirPollException {
        try {
            String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            String upper   = content.trim().toUpperCase();

            LogEvent evt = new LogEvent(this, "upper-case-processor");
            evt.addMessage("file: " + file.getName());
            evt.addMessage("result: " + upper);
            Logger.log(evt);
        } catch (IOException e) {
            throw new DirPoll.DirPollException("Error reading " + file.getName(), e);
        }
    }
}
