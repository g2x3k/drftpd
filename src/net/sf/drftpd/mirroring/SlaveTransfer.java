package net.sf.drftpd.mirroring;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.rmi.RemoteException;

import net.sf.drftpd.master.RemoteSlave;
import net.sf.drftpd.remotefile.LinkedRemoteFile;
import net.sf.drftpd.slave.Transfer;

import org.apache.log4j.Logger;

/**
 * @author mog
 * @author zubov
 * @version $Id
 */
public class SlaveTransfer {
	class DstXfer extends Thread {
		private Transfer dstxfer;
		private Throwable e;
		public DstXfer(Transfer dstxfer) {
			this.dstxfer = dstxfer;
		}

		public long getChecksum() throws RemoteException {
			return dstxfer.getChecksum();
		}
		/**
		 * @return
		 */
		public int getLocalPort() throws RemoteException {
			return dstxfer.getLocalPort();
		}

		public void run() {
			try {
				dstxfer.receiveFile(
					_file.getParent(),
					'I',
					_file.getName(),
					0L);
			} catch (Throwable e) {
				this.e = e;
			}
		}
	}

	class SrcXfer extends Thread {
		private Throwable e;
		private Transfer srcxfer;
		public SrcXfer(Transfer srcxfer) {
			this.srcxfer = srcxfer;
		}

		public long getChecksum() throws RemoteException {
			return srcxfer.getChecksum();
		}

		public void run() {
			try {
				srcxfer.sendFile(_file.getPath(), 'I', 0L, true);
			} catch (Throwable e) {
				this.e = e;
			}
		}
	}
	private static final Logger logger = Logger.getLogger(SlaveTransfer.class);
	private RemoteSlave _destSlave;
	private LinkedRemoteFile _file;
	private RemoteSlave _sourceSlave;
	private boolean finished = false;
	private Throwable stackTrace;
	/**
	 * Slave to Slave Transfers
	 */
	public SlaveTransfer(
		LinkedRemoteFile file,
		RemoteSlave sourceSlave,
		RemoteSlave destSlave) {
		_file = file;
		_sourceSlave = sourceSlave;
		_destSlave = destSlave;
	}
	public void interruptibleSleepUntilFinished() throws Throwable {
		while (!finished) {
			try {
				Thread.sleep(1000); // 1 sec
				//System.err.println("slept 1 secs");
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		if (stackTrace != null)
			throw stackTrace;
	}
	/**
	 * Returns true if the crc passed, false otherwise
	 * @return
	 * @throws IOException
	 */
	public boolean transfer(boolean checkCRC) throws IOException {
		DstXfer dstxfer = new DstXfer(_destSlave.getSlave().listen(false));
		SrcXfer srcxfer =
			new SrcXfer(
				_sourceSlave.getSlave().connect(
					new InetSocketAddress(
						_destSlave.getInetAddress(),
						dstxfer.getLocalPort()),
					false));
		dstxfer.start();
		srcxfer.start();
		while (srcxfer.isAlive() && dstxfer.isAlive()) {
			Thread.yield();
		}
		if (srcxfer.e != null) {
			if (srcxfer.e instanceof IOException)
				throw (IOException) srcxfer.e;
			throw new RuntimeException(srcxfer.e);
		}
		if (dstxfer.e != null) {
			if (dstxfer.e instanceof IOException)
				throw (IOException) dstxfer.e;
			throw new RuntimeException(dstxfer.e);
		}
		if (!checkCRC) {
			// crc passes if we're not using it
			return true;
		}
		//		logger.debug("_file checksum = " + Checksum.formatChecksum(_file.getCheckSum()));
		//		logger.debug("srcxfer checksum = " + Checksum.formatChecksum(srcxfer.getChecksum()));
		//		logger.debug("dstxfer checksum = " + Checksum.formatChecksum(dstxfer.getChecksum()));
		if (_file.getCheckSumCached() == dstxfer.getChecksum()) {
			_file.addSlave(_destSlave);
			//logger.info("Checksum passed for file " + _file.getName());
			return true;
		}
		if (dstxfer.getChecksum() == 0) {
			_file.addSlave(_destSlave);
			//logger.info("Checksum for slave " + _destSlave + " is disabled, assuming " + _file.getName() + " is good");
			return true;
		}
		//logger.debug("Checksum for file " + _file.getName() + " : " + _file.getCheckSum());
		//logger.debug("Checksum from slave " + _destSlave.getName() + " : " + dstxfer.getChecksum());
		return false;
	}
}
