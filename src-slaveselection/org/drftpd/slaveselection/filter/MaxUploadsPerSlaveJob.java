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

import java.net.InetAddress;
import java.util.Properties;


import org.drftpd.GlobalContext;
import org.drftpd.exceptions.NoAvailableSlaveException;
import org.drftpd.jobmanager.Job;
import org.drftpd.jobmanager.JobManager;
import org.drftpd.master.RemoteSlave;
import org.drftpd.usermanager.User;
import org.drftpd.vfs.InodeHandleInterface;

/**
 * @author mog
 * @version $Id$
 */
public class MaxUploadsPerSlaveJob extends Filter {
	private GlobalContext _gctx;

	public MaxUploadsPerSlaveJob(FilterChain fc, int i, Properties p) {
		_gctx = fc.getGlobalContext();
	}

	public MaxUploadsPerSlaveJob(GlobalContext gctx) {
		_gctx = gctx;
	}

	public void process(ScoreChart scorechart, User user, InetAddress peer,
			char direction, InodeHandleInterface dir, RemoteSlave sourceSlave)
			throws NoAvailableSlaveException {
		process(scorechart, sourceSlave);
	}

	public void process(ScoreChart scorechart, RemoteSlave sourceSlave) {
		if (sourceSlave == null)
			return;
		JobManager jm = _gctx.getJobManager();
		for (Job job : jm.getAllJobsFromQueue()) {
			synchronized (job) {
				if (job.isTransferring()) {
					if (job.getSourceSlave().equals(sourceSlave))
						scorechart.removeSlaveScore(job.getDestinationSlave());
				}
			}
		}
	}
}
