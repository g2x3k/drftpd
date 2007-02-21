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
package org.drftpd.commandmanager;

import org.drftpd.master.BaseFtpConnection;
import org.drftpd.vfs.DirectoryHandle;

/**
 * @author djb61
 * @version $Id$
 */
public interface CommandRequestInterface {

	public void setArgument(String argument);

	public void setCommand(String command);

	public void setConnection(BaseFtpConnection connection);

	public void setCurrentDirectory(DirectoryHandle currentDirectory);

	public void setUser(String currentUser);

	public String getArgument();

	public String getCommand();

	public BaseFtpConnection getConnection();

	public DirectoryHandle getCurrentDirectory();

	public String getUser();
}
