package net.sf.drftpd.master.command.plugins;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import net.sf.drftpd.Bytes;
import net.sf.drftpd.event.DirectoryFtpEvent;
import net.sf.drftpd.master.BaseFtpConnection;
import net.sf.drftpd.master.FtpReply;
import net.sf.drftpd.master.FtpRequest;
import net.sf.drftpd.master.command.CommandHandler;
import net.sf.drftpd.master.command.CommandManager;
import net.sf.drftpd.master.command.CommandManagerFactory;
import net.sf.drftpd.master.command.UnhandledCommandException;
import net.sf.drftpd.master.usermanager.NoSuchUserException;
import net.sf.drftpd.master.usermanager.User;
import net.sf.drftpd.master.usermanager.UserFileException;
import net.sf.drftpd.remotefile.LinkedRemoteFile;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * @author mog
 *
 * @version $Id: Pre.java,v 1.7 2004/02/04 17:13:13 mog Exp $
 */
public class Pre implements CommandHandler {

	private static final Logger logger = Logger.getLogger(Pre.class);

	/**
	 * Syntax: SITE PRE <RELEASEDIR> [SECTION]
	 * @param request
	 * @param out
	 */
	public FtpReply execute(BaseFtpConnection conn)
		throws UnhandledCommandException {
		FtpRequest request = conn.getRequest();
		if (!"SITE PRE".equals(request.getCommand()))
			throw UnhandledCommandException.create(Pre.class, request);
		if (!request.hasArgument()) {
			return FtpReply.RESPONSE_501_SYNTAX_ERROR;
		}
		String args[] = request.getArgument().split(" ");
		if (args.length != 2) {
			return FtpReply.RESPONSE_501_SYNTAX_ERROR;
		}
		LinkedRemoteFile section;
		try {
			section = conn.getCurrentDirectory().getRoot().lookupFile(args[1]);
		} catch (FileNotFoundException ex) {
			return new FtpReply(
				200,
				"Release dir not found: " + ex.getMessage());
		}

		LinkedRemoteFile preDir;
		try {
			preDir = conn.getCurrentDirectory().lookupFile(args[0]);
		} catch (FileNotFoundException e) {
			return FtpReply.RESPONSE_550_REQUESTED_ACTION_NOT_TAKEN;
		}
		if (!conn.getConfig().checkPre(conn.getUserNull(), preDir)) {
			return FtpReply.RESPONSE_530_ACCESS_DENIED;
		}

		FtpReply response = new FtpReply(200);

		//AWARD CREDITS
		Hashtable awards = new Hashtable();
		preAwardCredits(conn, preDir, awards);
		for (Iterator iter = awards.entrySet().iterator(); iter.hasNext();) {
			Map.Entry entry = (Map.Entry) iter.next();
			User owner = (User) entry.getKey();
			if (conn.getConfig().getCreditCheckRatio(preDir, owner) == 0) {
				Long award = (Long) entry.getValue();
				owner.updateCredits(award.longValue());
				response.addComment(
					"Awarded "
						+ Bytes.formatBytes(award.longValue())
						+ " to "
						+ owner.getUsername());
			}
		}

		//RENAME
		LinkedRemoteFile toDir;
		try {
			toDir = preDir.renameTo(section.getPath(), preDir.getName());
		} catch (IOException ex) {
			logger.warn("", ex);
			return new FtpReply(200, ex.getMessage());
		}

		//ANNOUNCE
		logger.debug("preDir after rename: " + preDir);
		conn.getConnectionManager().dispatchFtpEvent(
			new DirectoryFtpEvent(conn.getUserNull(), "PRE", toDir));

		return FtpReply.RESPONSE_200_COMMAND_OK;
	}

	public String[] getFeatReplies() {
		return null;
	}

	public CommandHandler initialize(
		BaseFtpConnection conn,
		CommandManager initializer) {
		return this;
	}
	public void load(CommandManagerFactory initializer) {
	}

	private void preAwardCredits(
		BaseFtpConnection conn,
		LinkedRemoteFile preDir,
		Hashtable awards) {
		for (Iterator iter = preDir.getFiles().iterator(); iter.hasNext();) {
			LinkedRemoteFile file = (LinkedRemoteFile) iter.next();
			User owner;
			try {
				owner = conn.getUserManager().getUserByName(file.getUsername());
			} catch (NoSuchUserException e) {
				logger.log(
					Level.INFO,
					"PRE: Cannot award credits to non-existing user",
					e);
				continue;
			} catch (UserFileException e) {
				logger.log(Level.WARN, "", e);
				continue;
			}
			Long total = (Long) awards.get(owner);
			if (total == null)
				total = new Long(0);
			total =
				new Long(
					total.longValue()
						+ (long) (file.length() * owner.getRatio()));
			awards.put(owner, total);
		}
	}
	public void unload() {
	}

}
