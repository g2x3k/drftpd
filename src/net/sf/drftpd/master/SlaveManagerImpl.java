package net.sf.drftpd.master;

import java.io.FileWriter;
import java.io.IOException;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.RMISocketFactory;
import java.rmi.server.UnicastRemoteObject;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import net.sf.drftpd.permission.GlobRMISocketFactory;
import net.sf.drftpd.remotefile.LinkedRemoteFile;
import net.sf.drftpd.remotefile.XMLSerialize;
import net.sf.drftpd.slave.RemoteSlave;
import net.sf.drftpd.slave.SlaveStatus;
import net.sf.drftpd.slave.TransferImpl;
import org.jdom.Document;
import org.jdom.output.XMLOutputter;

public class SlaveManagerImpl
	extends UnicastRemoteObject
	implements SlaveManager {

	//	protected Hashtable slaves = new Hashtable();
	protected LinkedRemoteFile root;
	//	protected Vector slaves = new Vector();
	protected List slaves;

	public LinkedRemoteFile getRoot() {
		return root;
	}

	public SlaveManagerImpl(String url, List masks) throws RemoteException {
		this(url, new LinkedRemoteFile(), masks);
	}

	public SlaveManagerImpl(String url, LinkedRemoteFile root, List masks)
		throws RemoteException {
		super(0, (RMIClientSocketFactory) RMISocketFactory.getSocketFactory(),
					(RMIServerSocketFactory)new GlobRMISocketFactory(masks));
//		 (RMIServerSocketFactory) RMISocketFactory.getSocketFactory());

		//		super();
		//		super(0, new GlobRMISocketFactory(masks), new GlobRMISocketFactory(masks));
		this.root = root;
		slaves = root.getSlaves();
		/*
				RMIClientSocketFactory csf = RMISocketFactory.getSocketFactory();
				RMIServerSocketFactory ssf = new GlobRMISocketFactory(masks);
		*/
		//RMIServerSocketFactory ssf = RMISocketFactory.getSocketFactory();

		//SlaveManager stub = this;
		/*
				SlaveManager stub =
					(SlaveManager) exportObject(
						this,
						0,
						csf,
						ssf);
		*/
		//SlaveManager stub = (SlaveManager)
		//		UnicastRemoteObject.exportObject((Remote)this, 6666);
		try {
			Registry registry = LocateRegistry.createRegistry(1099);
			registry.bind("slavemanager", this);
		} catch (AlreadyBoundException ex) {
			ex.printStackTrace();
			return;
		}

		/*		} catch (java.rmi.ConnectException ex) {
					ex.printStackTrace();
					System.exit(-1);
					return;
				//java.rmi.ConnectIOException: Exception creating connection to: 213.114.146.61; nested exception is: 
				//	java.net.SocketException: errno: 101, error: Network is unreachable for fd: 7
				} catch(java.rmi.ConnectIOException ex) {
					System.out.println(ex.getMessage());
					System.out.println("Error binding slave, check the slavemanager.url property");
					System.exit(-1);
					return;			
				} catch (java.net.MalformedURLException ex) {
					ex.printStackTrace();
					System.out.println("The property slavemanager.url has an invalid URL.");
					System.exit(-1);
					return;
					//} catch(ClassNotFoundException ex) {
					//	System.out.println("ClassNotFoundException: "+ex.getMessage());
					//	System.out.println("rmiregistry was problably started with wrong parameters.");
				}
				*/
	}

	/**
	 * TODO: support hot swapping slavess
	 */
	public void addSlave(RemoteSlave slave, LinkedRemoteFile remoteroot)
		throws RemoteException {
		System.out.println(
			"SlaveManager.addSlave(): " + slave + " remoteroot: " + remoteroot);
		slave.setManager(this);
		slaves.add(slave);
		long millis = System.currentTimeMillis();
		root.merge(remoteroot);
		System.out.println(
			"merge() took " + (System.currentTimeMillis() - millis) + "ms");
		System.out.println("SlaveStatus: " + slave.getSlave().getSlaveStatus());
		//TODO: write XML representation of "LinkedRemoteFile root"
		Document doc = new Document(XMLSerialize.serialize(root));
		try {
			new XMLOutputter("    ", true).output(
				doc,
				new FileWriter("files.xml"));
		} catch (IOException ex) {
			System.err.println(
				"Warning, error saving database to \"files.xml\"");
			ex.printStackTrace();
		}
	}

	private Random rand = new Random();
	public RemoteSlave getASlave() {
		int num = rand.nextInt(slaves.size());
		System.out.println(
			"Slave "
				+ num
				+ " selected out of "
				+ slaves.size()
				+ " available slaves");
		return (RemoteSlave) slaves.get(num);
	}

	public RemoteSlave getASlave(char direction)
		throws NoAvailableSlaveException {
		RemoteSlave bestslave;
		SlaveStatus beststatus;
		{
			Iterator i = slaves.iterator();

			while (true) {
				if (!i.hasNext())
					throw new NoAvailableSlaveException();
				bestslave = (RemoteSlave) i.next();
				try {
					beststatus = bestslave.getStatus();
					break;
				} catch (RemoteException ex) {
					handleRemoteException(ex, bestslave);
					continue;
				}
			}

			while (i.hasNext()) {
				RemoteSlave slave = (RemoteSlave) i.next();
				SlaveStatus status;

				try {
					status = slave.getStatus();
				} catch (RemoteException ex) {
					handleRemoteException(ex, slave);
					continue;
				}
				float throughput, bestthroughput;
				if (direction == TransferImpl.TRANSFER_RECEIVING) {
					throughput = status.getThroughputReceiving();
					bestthroughput = beststatus.getThroughputReceiving();
				} else if (direction == TransferImpl.TRANSFER_SENDING) {
					throughput = status.getThroughputSending();
					bestthroughput = beststatus.getThroughputSending();
				} else {
					throughput = status.getThroughput();
					bestthroughput = beststatus.getThroughput();
				}

				if (throughput < bestthroughput) {
					bestslave = slave;
				}
			}
		}
		return bestslave;
	}

	public void handleRemoteException(RemoteException ex, RemoteSlave slave) {
		System.out.print(
			"Caught exception when trying to communicate with " + slave);
		if (!isFatalRemoteException(ex)) {
			System.out.println(". Non-fatal exception, not removing");
			return;
		}
		System.out.println(". Fatal exception, removing");
		ex.printStackTrace();
		root.unmerge(slave);
	}

	public boolean isFatalRemoteException(RemoteException ex) {
		return (ex instanceof java.rmi.ConnectException);
	}

	public int verifySlaves() {
		int removed = 0;
		synchronized (slaves) {
			for (Iterator i = slaves.iterator(); i.hasNext();) {
				RemoteSlave slave = (RemoteSlave) i.next();
				try {
					slave.getSlave().ping();
				} catch (RemoteException ex) {
					if (isFatalRemoteException(ex))
						i.remove();
					handleRemoteException(ex, slave);
					removed++;
				}
			}
		}
		return removed;
	}
}
