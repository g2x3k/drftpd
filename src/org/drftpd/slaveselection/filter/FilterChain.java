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
package org.drftpd.slaveselection.filter;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;

import org.drftpd.slaveselection.SlaveSelectionManagerInterface;

import net.sf.drftpd.FatalException;
import net.sf.drftpd.NoAvailableSlaveException;
import net.sf.drftpd.master.RemoteSlave;
import net.sf.drftpd.master.SlaveManagerImpl;
import net.sf.drftpd.master.usermanager.User;
import net.sf.drftpd.remotefile.LinkedRemoteFileInterface;

/**
 * @author mog
 * @version $Id: FilterChain.java,v 1.4 2004/05/18 20:28:18 zubov Exp $
 */
public class FilterChain {
	private String _cfgfileName;
	private ArrayList _filters;
	private SlaveSelectionManagerInterface _sm;

	protected FilterChain() {
	}
	
	public FilterChain(SlaveSelectionManagerInterface sm, Properties p) {
		_sm = sm;
		reload(p);
	}

	public FilterChain(SlaveSelectionManagerInterface sm, String cfgFileName)
		throws FileNotFoundException, IOException {
		_sm = sm;
		_cfgfileName = cfgFileName;
		reload();
	}

	public RemoteSlave getBestSlave(
		ScoreChart sc,
		User user,
		InetAddress peer,
		char direction,
		LinkedRemoteFileInterface file)
		throws NoAvailableSlaveException {
		for (Iterator iter = _filters.iterator(); iter.hasNext();) {
			Filter filter = (Filter) iter.next();
			filter.process(sc, user, peer, direction, file);
		}
		RemoteSlave rslave = sc.getBestSlave();
		rslave.setLastDirection(direction, System.currentTimeMillis());
		if (rslave == null) throw new NoAvailableSlaveException("This is not supposed to be thrown");
		return rslave;
	}

	public void reload() throws FileNotFoundException, IOException {
		Properties p = new Properties();
		p.load(new FileInputStream(_cfgfileName));
		reload(p);
	}

	public void reload(Properties p) {
		ArrayList filters = new ArrayList();
		int i = 1;
		for (;; i++) {
			String type = p.getProperty(i + ".filter");
			if (type == null)
				break;
			if (type.indexOf('.') == -1) {
				type =
					"org.drftpd.slaveselection.filter."
						+ type.substring(0, 1).toUpperCase()
						+ type.substring(1)
						+ "Filter";
			}
			try {
				Class[] SIG =
					new Class[] {
						FilterChain.class,
						int.class,
						Properties.class };

				Filter filter =
					(Filter) Class.forName(type).getConstructor(
						SIG).newInstance(
						new Object[] { this, new Integer(i), p });
				filters.add(filter);
			} catch (Exception e) {
				throw new FatalException(i + ".filter = " + type, e);
			}
		}
		if (i == 1)
			throw new IllegalArgumentException();
		filters.trimToSize();
		_filters = filters;
	}

	public SlaveManagerImpl getSlaveManager() {
		return _sm.getSlaveManager();
	}
}
