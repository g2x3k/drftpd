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
package org.drftpd.commands.sitemanagement;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.helpers.OptionConverter;
import org.drftpd.GlobalContext;
import org.drftpd.PropertyHelper;
import org.drftpd.commandmanager.CommandInterface;
import org.drftpd.commandmanager.CommandRequest;
import org.drftpd.commandmanager.CommandResponse;
import org.drftpd.commandmanager.ImproperUsageException;
import org.drftpd.commandmanager.StandardCommandManager;
import org.drftpd.event.ConnectionEvent;
import org.drftpd.event.FtpListener;
import org.drftpd.event.ReloadEvent;
import org.drftpd.event.LoadPluginEvent;
import org.drftpd.event.UnloadPluginEvent;
import org.drftpd.usermanager.NoSuchUserException;
import org.drftpd.usermanager.UserFileException;
import org.drftpd.vfs.DirectoryHandle;
import org.drftpd.vfs.InodeHandle;
import org.java.plugin.JpfException;
import org.java.plugin.PluginManager;
import org.java.plugin.boot.DefaultPluginsCollector;
import org.java.plugin.registry.PluginDescriptor;
import org.java.plugin.util.ExtendedProperties;

/**
 * @author mog
 * @author zubov
 * @version $Id$
 */
public class SiteManagementHandler extends CommandInterface {
	private static final Logger logger = Logger.getLogger(SiteManagementHandler.class);

	private static final String jpfConf = "conf/boot-master.properties";

	public CommandResponse doSITE_LIST(CommandRequest request) {

		CommandResponse response = StandardCommandManager.genericResponse("RESPONSE_200_COMMAND_OK");
		
		DirectoryHandle dir = request.getCurrentDirectory();
		InodeHandle target;
		if (request.hasArgument()) {
			try {
				target = dir.getInodeHandle(request.getArgument());
			} catch (FileNotFoundException e) {
				logger.debug("FileNotFound", e);
				return new CommandResponse(200, e.getMessage());
			}
		} else {
			target = dir;
		}
		
		List<InodeHandle> inodes;
		try {
			if (target.isFile()) {
				inodes = Collections.singletonList((InodeHandle)dir);
			} else {
				inodes = new ArrayList<InodeHandle>(dir.getInodeHandles());
			}
			Collections.sort(inodes);

			for (InodeHandle inode : inodes) {
				response.addComment(inode.toString());
			}
		} catch (FileNotFoundException e) {
			logger.debug("FileNotFound", e);
			return new CommandResponse(200, e.getMessage());
		}
		return response;
	}

	public CommandResponse doSITE_LOADPLUGIN(CommandRequest request) throws ImproperUsageException {

		/*if (!request.hasArgument()) {
			return new CommandResponse(500, "Usage: site load className");
		}

		FtpListener ftpListener = getFtpListener(request
				.getArgument());

		if (ftpListener == null) {
			return new CommandResponse(500,
					"Was not able to find the class you are trying to load");
		}

		GlobalContext.getGlobalContext().addFtpListener(ftpListener);*/

		if (!request.hasArgument()) {
			throw new ImproperUsageException();
		}
		ExtendedProperties jpfProps = new ExtendedProperties();
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(jpfConf);
			jpfProps.load(fis);
		}
		catch (IOException e) {
			logger.debug("Exception loading JPF properties",e);
			return new CommandResponse(500,e.getMessage());
		}
		finally {
			if (fis != null) {
				try {
					fis.close();
				}
				catch (IOException e) {
					logger.debug("Exception closing input stream",e);
				}
			}
		}
		PluginManager manager = PluginManager.lookup(this);
		DefaultPluginsCollector collector = new DefaultPluginsCollector();
		try {
			collector.configure(jpfProps);
		}
		catch (Exception e) {
			logger.debug("Exception configuring plugins collector",e);
			return new CommandResponse(500,e.getMessage());
		}
		try {
			manager.publishPlugins(collector.collectPluginLocations().toArray(new PluginManager.PluginLocation[0]));
		}
		catch (JpfException e) {
			logger.debug("Exception publishing plugins", e);
			return new CommandResponse(500,e.getMessage());
		}
		GlobalContext.getEventService().publish(new LoadPluginEvent(request.getArgument()));
		return new CommandResponse(200, "Successfully loaded plugin");
	}

	public CommandResponse doSITE_PLUGINS(CommandRequest request) {

		/*CommandResponse response = new CommandResponse(200, "Command ok");
		response.addComment("Plugins loaded:");
		for (FtpListener listener : GlobalContext.getGlobalContext().getFtpListeners()) {
			response.addComment(listener.getClass().getName());
		}
		return response;*/
		CommandResponse response = StandardCommandManager.genericResponse("RESPONSE_200_COMMAND_OK");
		response.addComment("Plugins loaded:");
		for (PluginDescriptor pluginDesc : PluginManager.lookup(this).getRegistry().getPluginDescriptors()) {
			response.addComment(pluginDesc.getId());
		}
		return response;
	}

	public CommandResponse doSITE_RELOAD(CommandRequest request) {

		try {
			GlobalContext.getGlobalContext().getSectionManager().reload();
			GlobalContext.getGlobalContext().reloadFtpConfig();
			GlobalContext.getGlobalContext().getSlaveSelectionManager().reload();

			try {
				GlobalContext.getGlobalContext().getJobManager()
						.reload();
			} catch (IllegalStateException e1) {
				// not loaded, don't reload
			}

			GlobalContext.getEventService().publish(new ReloadEvent(PluginManager.lookup(this).getPluginFor(this).getDescriptor().getId()));
			// can't reload commands for now
			//conn.getGlobalContext().getConnectionManager()
				//	.getCommandManagerFactory().reload();

		} catch (IOException e) {
			logger.log(Level.FATAL, "Error reloading config", e);

			return new CommandResponse(200, e.getMessage());
		}
		try {
			GlobalContext.getGlobalContext().dispatchFtpEvent(
					new ConnectionEvent(GlobalContext.getGlobalContext().getUserManager().getUserByName(request.getUser()), "RELOAD"));
		} catch (NoSuchUserException e1) {
			return new CommandResponse(500, "You apparently don't exist as a user anymore");
		} catch (UserFileException e1) {
			return new CommandResponse(500, "You're userfile is broken");
		}

		// ugly hack to clear resourcebundle cache
		// see
		// http://developer.java.sun.com/developer/bugParade/bugs/4212439.html
		try {
			Field cacheList = ResourceBundle.class
					.getDeclaredField("cacheList");
			cacheList.setAccessible(true);
			((Map) cacheList.get(ResourceBundle.class)).clear();
			cacheList.setAccessible(false);
		} catch (Exception e) {
			logger.error("", e);
		}

		try {
			OptionConverter.selectAndConfigure(
					new URL(PropertyHelper.getProperty(System.getProperties(),
							"log4j.configuration")), null, LogManager
							.getLoggerRepository());
		} catch (MalformedURLException e) {
			logger.error(e);
			return new CommandResponse(500, e.getMessage());
		} finally {
		}
		return StandardCommandManager.genericResponse("RESPONSE_200_COMMAND_OK");
	}

	public CommandResponse doSITE_SHUTDOWN(CommandRequest request) {

		String message;

		if (!request.hasArgument()) {
			message = "Service shutdown issued by "
					+ request.getUser();
		} else {
			message = request.getArgument();
		}

		GlobalContext.getGlobalContext().shutdown(message);

		return StandardCommandManager.genericResponse("RESPONSE_200_COMMAND_OK");
	}

	public CommandResponse doSITE_UNLOADPLUGIN(CommandRequest request) throws ImproperUsageException {
		/*if (!request.hasArgument()) {
			return new CommandResponse(500, "Usage: site unload className");
		}
		for (FtpListener ftpListener : GlobalContext.getGlobalContext()
				.getFtpListeners()) {
			if (ftpListener.getClass().getName().equals(
					"org.drftpd.plugins." + request.getArgument())
					|| ftpListener.getClass().getName().equals(
							request.getArgument())) {
				try {
					ftpListener.unload();
				} catch (RuntimeException e) {
					return new CommandResponse(200,
							"Exception unloading plugin, plugin removed");
				} finally {
					GlobalContext.getGlobalContext().delFtpListener(ftpListener);
				}
				return new CommandResponse(200, "Successfully unloaded your plugin");
			}
		}

		return new CommandResponse(500, "Could not find your plugin on the site");*/
		if (!request.hasArgument()) {
			throw new ImproperUsageException();
		}
		PluginManager manager = PluginManager.lookup(this);
		PluginDescriptor pluginDesc;
		try {
			pluginDesc = PluginManager.lookup(this).getRegistry()
				.getPluginDescriptor(request.getArgument());
		}
		catch (IllegalArgumentException e) {
			return new CommandResponse(500, "No such plugin loaded");
		}
		GlobalContext.getEventService().publish(new UnloadPluginEvent(request.getArgument()));
		manager.deactivatePlugin(request.getArgument());
		if (manager.isPluginActivated(pluginDesc)) {
			return new CommandResponse(500, "Unable to unload plugin");
		}
		manager.getRegistry().unregister(new String[] {request.getArgument()});
		return new CommandResponse(200, "Successfully unloaded your plugin");
	}

	private FtpListener getFtpListener(String arg) {
		FtpListener ftpListener = null;

		try {
			ftpListener = (FtpListener) Class.forName(
					"org.drftpd.plugins." + arg).newInstance();
		} catch (InstantiationException e) {
			logger
					.error(
							"Was not able to create an instance of the class, did not load",
							e);

			return null;
		} catch (IllegalAccessException e) {
			logger.error("This will not happen, I do not exist", e);
			return null;
		} catch (ClassNotFoundException e) {
		}

		if (ftpListener == null) {
			try {
				ftpListener = (FtpListener) Class.forName(arg).newInstance();
			} catch (InstantiationException e) {
				logger
						.error(
								"Was not able to create an instance of the class, did not load",
								e);

				return null;
			} catch (IllegalAccessException e) {
				logger.error("This will not happen, I do not exist", e);

				return null;
			} catch (ClassNotFoundException e) {
				return null;
			}
		}

		return ftpListener;
	}
}
