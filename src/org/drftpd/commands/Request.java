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
package org.drftpd.commands;

import java.io.FileNotFoundException;
import java.io.IOException;


import org.apache.log4j.Logger;
import org.drftpd.commandmanager.CommandHandler;
import org.drftpd.commandmanager.CommandHandlerFactory;
import org.drftpd.commandmanager.CommandManager;
import org.drftpd.commandmanager.CommandManagerFactory;
import org.drftpd.commandmanager.Reply;
import org.drftpd.commandmanager.UnhandledCommandException;
import org.drftpd.dynamicdata.Key;
import org.drftpd.event.DirectoryFtpEvent;
import org.drftpd.exceptions.FileExistsException;
import org.drftpd.master.BaseFtpConnection;
import org.drftpd.usermanager.NoSuchUserException;
import org.drftpd.vfs.DirectoryHandle;

/**
 * @author mog
 * @version $Id$
 */
public class Request implements CommandHandler, CommandHandlerFactory {
    public static final Key REQUESTSFILLED = new Key(Request.class,
            "requestsFilled", Integer.class);
    public static final Key REQUESTS = new Key(Request.class, "requests",
            Integer.class);
    private static final String FILLEDPREFIX = "FILLED-for.";
    private static final Logger logger = Logger.getLogger(Request.class);
    private static final String REQPREFIX = "REQUEST-by.";

    private Reply doSITE_REQFILLED(BaseFtpConnection conn) {
        if (!conn.getRequest().hasArgument()) {
            return Reply.RESPONSE_501_SYNTAX_ERROR;
        }

        DirectoryHandle currdir = conn.getCurrentDirectory();
        String reqname = conn.getRequest().getArgument().trim();
        try {
			for (DirectoryHandle dir : currdir.getDirectories()) {

			    if (!dir.getName().startsWith(REQPREFIX)) {
			        continue;
			    }

			    String username = dir.getName().substring(REQPREFIX.length());
			    String myreqname = username.substring(username.indexOf('-') + 1);
			    username = username.substring(0, username.indexOf('-'));

			    if (myreqname.equals(reqname)) {
			        String filledname = FILLEDPREFIX + username + "-" + myreqname;

			        try {
			            dir.renameTo(currdir.getNonExistentFileHandle(filledname));
			        } catch (IOException e) {
			            logger.warn("", e);
			            return new Reply(200, e.getMessage());
			        }

			        //if (conn.getConfig().checkDirLog(conn.getUserNull(), file)) {
			        conn.getGlobalContext().dispatchFtpEvent(new DirectoryFtpEvent(
			                conn.getUserNull(), "REQFILLED", dir));

			        //}
			        try {
			            conn.getUser().getKeyedMap().incrementObjectLong(REQUESTSFILLED);

			            //conn.getUser().addRequestsFilled();
			        } catch (NoSuchUserException e) {
			            e.printStackTrace();
			        }

			        return new Reply(200,
			            "OK, renamed " + myreqname + " to " + filledname);
			    }
			}
		} catch (FileNotFoundException e) {
			return new Reply(500, "Current directory does not exist, please CWD /");
		}

        return new Reply(200, "Couldn't find a request named " + reqname);
    }

    private Reply doSITE_REQUEST(BaseFtpConnection conn) {
        if (!conn.getGlobalContext().getConfig().checkPathPermission("request",
                    conn.getUserNull(), conn.getCurrentDirectory())) {
            return Reply.RESPONSE_530_ACCESS_DENIED;
        }

        if (!conn.getRequest().hasArgument()) {
            return Reply.RESPONSE_501_SYNTAX_ERROR;
        }

        String createdDirName = REQPREFIX + conn.getUserNull().getName() +
            "-" + conn.getRequest().getArgument().trim();

        try {
            DirectoryHandle createdDir;
			try {
				createdDir = conn.getCurrentDirectory()
				                                  .createDirectory(conn.getUserNull()
				                                                       .getName(),
				        conn.getUserNull().getGroup(), createdDirName);
			} catch (FileNotFoundException e) {
				return new Reply(500, "Current directory does not exist, please CWD /");
			}

            //if (conn.getConfig().checkDirLog(conn.getUserNull(), createdDir)) {
            conn.getGlobalContext().dispatchFtpEvent(new DirectoryFtpEvent(
                    conn.getUserNull(), "REQUEST", createdDir));

            conn.getUserNull().getKeyedMap().incrementObjectLong(REQUESTS);

            //conn.getUser().addRequests();
            return new Reply(257, "\"" + createdDir.getPath() +
                "\" created.");
        } catch (FileExistsException ex) {
            return new Reply(550,
                "directory " + createdDirName + " already exists");
        }
    }

    public Reply execute(BaseFtpConnection conn)
        throws UnhandledCommandException {
        String cmd = conn.getRequest().getCommand();

        if ("SITE REQUEST".equals(cmd)) {
            return doSITE_REQUEST(conn);
        }

        if ("SITE REQFILLED".equals(cmd)) {
            return doSITE_REQFILLED(conn);
        }

        throw UnhandledCommandException.create(Request.class, conn.getRequest());
    }

    public String[] getFeatReplies() {
        return null;
    }

    public CommandHandler initialize(BaseFtpConnection conn,
        CommandManager initializer) {
        return this;
    }

    public void load(CommandManagerFactory initializer) {
    }

    public void unload() {
    }
}
