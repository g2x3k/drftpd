package net.sf.drftpd.remotefile;

import java.util.ArrayList;
import java.util.Collection;

import net.sf.drftpd.master.usermanager.User;
import net.sf.drftpd.slave.RemoteSlave;

/**
 * Creates a single RemoteFile object that is not linked to any other objects.
 * 
 * Useful when doing RMI call and you do not want to send the entire
 * linked directory structure to the remote VM.
 * 
 * @author <a href="mailto:drftpd@mog.se">Morgan Christiansson</a>
 */
public class StaticRemoteFile extends RemoteFile {
	private String path;
	private long length;
	private long lastModified;
	private Collection slaves;
	
	public StaticRemoteFile(RemoteFile file) {
//		canRead = file.canRead();
//		canWrite = file.canWrite();
		this.lastModified = file.lastModified();
		this.length = file.length();
		//isHidden = file.isHidden();
		this.isDirectory = file.isDirectory();
		this.isFile = file.isFile();
		this.path = file.getPath();
		this.slaves = new ArrayList(0);
		/* serialize directory*/
		//slaves = file.getSlaves();
	}
	
	/**
	 * Creates a new RemoteFile from nothing.
	 * 
	 * If 'path' ends with "/" the RemoteFile will be marked as a directory
	 * 
	 * If this file has no owner 'owner' may be null, then "drftpd:drftpd will be used.
	 * 
	 * If lastModified is 0 it will be set to the currentTimeMillis.
	 */
	public StaticRemoteFile(Collection rslaves, String path, User owner, long size, long lastModified) {
		this.slaves = rslaves;
		this.path = path;
		if(path.endsWith("/")) {
			isDirectory = true;
			isFile = false;
		} else {
			isDirectory = false;
			isFile = true;
		}
		if(owner == null) {
			this.owner = owner.getUsername();
			group = owner.getGroup();
		} else {
			this.owner = "drftpd";
			group = "drftpd";
		}
		this.length = size;
		this.lastModified = lastModified;
	}
	
	/**
	 * @see java.lang.Object#equals(Object)
	 */
	public boolean equals(Object file) {
		if(!(file instanceof RemoteFile)) return false;
		return getPath().equals(((RemoteFile)file).getPath());
	}

	/**
	 * @see net.sf.drftpd.RemoteFile#getName()
	 */
	public String getName() {
		int index = path.lastIndexOf(RemoteFile.separatorChar);
		return path.substring(index + 1);
	}

	/**
	 * @see net.sf.drftpd.RemoteFile#getParent()
	 */
	public String getParent() {
		throw new NoSuchMethodError("getParent() does not exist in StaticRemoteFile");
	}

	/**
	 * @see net.sf.drftpd.RemoteFile#getPath()
	 */
	public String getPath() {
		return path;
	}

	/**
	 * @see net.sf.drftpd.remotefile.RemoteFileTree#listFiles()
	 */
	public RemoteFile[] listFiles() {
		throw new NoSuchMethodError("listFiles() does not exist in StaticRemoteFile");
	}

	/* (non-Javadoc)
	 * @see net.sf.drftpd.remotefile.RemoteFile#getFiles()
	 */
	public Collection getFiles() {
		throw new NoSuchMethodError("getFiles() does not exist in StaticRemoteFile");
	}

	/* (non-Javadoc)
	 * @see net.sf.drftpd.remotefile.RemoteFile#getSlaves()
	 */
	public Collection getSlaves() {
		return slaves;
	}

	/* (non-Javadoc)
	 * @see net.sf.drftpd.remotefile.RemoteFile#length()
	 */
	public long length() {
		return this.length;
	}

	/* (non-Javadoc)
	 * @see net.sf.drftpd.remotefile.RemoteFile#lastModified()
	 */
	public long lastModified() {
		return this.lastModified;
	}

}
