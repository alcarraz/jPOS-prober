/*
 * jpos-prober [https://github.com/alcarraz/jpos-prober]
 *
 * Copyright (C) 2024.  Andrés Alcarraz
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https:www.gnu.org/licenses/>.
 *
 */

package org.jpos.probe;

import org.jpos.iso.ISOException;
import org.jpos.iso.channel.XMLChannel;
import org.jpos.iso.packager.XMLPackager;

import java.io.IOException;
import java.io.InputStream;

public class XMLInputFileChannel extends XMLChannel implements AutoCloseable{
    public XMLInputFileChannel() throws ISOException {
        this(System.in);
    }
    public XMLInputFileChannel(InputStream in) throws ISOException {
        this(in, new XMLPackager());
    }

    public XMLInputFileChannel(XMLPackager packager){
        this(System.in, packager);
    }

    public XMLInputFileChannel(InputStream in, XMLPackager packager) {
        connect(in, null);
        setPackager(packager);
        
    }

    @Override
    public void close() throws IOException {
        disconnect();
    }

    @Override
    public boolean isConnected() {
        return super.isConnected() || (usable && serverIn != null);
    }
}
