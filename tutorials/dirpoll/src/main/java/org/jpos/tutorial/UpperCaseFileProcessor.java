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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * A {@link DirPoll.FileProcessor} that upper-cases the content of each
 * incoming file and logs the result.
 *
 * <p>{@link DirPoll.FileProcessor} receives a {@link File} handle pointing
 * to the request file after DirPoll has moved it into the {@code run/}
 * directory.  The processor is responsible for everything: reading, acting,
 * and deciding what to do next.  DirPoll will archive or delete the file
 * once {@link #process(File)} returns normally.
 *
 * <p>Unlike {@link DirPoll.Processor}, a {@code FileProcessor} does not
 * return a byte array.  If you need to write a response file, create it
 * directly in the {@code response/} directory (or any other directory)
 * from within this method.
 */
public class UpperCaseFileProcessor implements DirPoll.FileProcessor {

    /**
     * Upper-case the content of the request file and print it to stdout.
     *
     * @param file the request file, already moved to the {@code run/} directory
     * @throws DirPoll.DirPollException on processing errors;
     *         set {@code retry=true} to have DirPoll requeue the request
     */
    @Override
    public void process(File file) throws DirPoll.DirPollException {
        try {
            String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            System.out.println("Processed [" + file.getName() + "]: " + content.trim().toUpperCase());
        } catch (IOException e) {
            throw new DirPoll.DirPollException("Error reading " + file.getName(), e);
        }
    }
}
