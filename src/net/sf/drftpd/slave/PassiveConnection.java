/*
 * This file is part of DrFTPD, Distributed FTP Daemon.
 *
 * DrFTPD is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * DrFTPD is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DrFTPD; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package net.sf.drftpd.slave;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;

import net.sf.drftpd.util.PortRange;

import org.apache.log4j.Logger;


/**
 * @author mog
 * @version $Id: PassiveConnection.java,v 1.14 2004/11/05 20:51:15 zubov Exp $
 */
public class PassiveConnection extends Connection {
    private static final Logger logger = Logger.getLogger(PassiveConnection.class);
    private ServerSocket _serverSocket;

    public PassiveConnection(SSLContext ctx, PortRange portRange) throws IOException {
        if (ctx != null) {
            _serverSocket = portRange.getPort(ctx.getServerSocketFactory());
        } else {
            _serverSocket = portRange.getPort(ServerSocketFactory.getDefault());
        }
        _serverSocket.setSoTimeout(TIMEOUT);
    }

    public Socket connect() throws IOException {
        Socket sock = _serverSocket.accept();
        _serverSocket.close();
        _serverSocket = null;

        setSockOpts(sock);

        if (sock instanceof SSLSocket) {
            SSLSocket sslsock = (SSLSocket) sock;
            sslsock.setUseClientMode(false);
            sslsock.startHandshake();
        }

        return sock;
    }

    public int getLocalPort() {
        if (_serverSocket == null) {
            throw new NullPointerException("_serverSocket == null");
        }
        return _serverSocket.getLocalPort();
    }

    public void abort() {
        try {
            _serverSocket.close();
        } catch (IOException e) {
            logger.warn("failed to close() server socket", e);
        }
        _serverSocket = null;
    }
}
