/*
 * Created on 2003-jun-29
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package net.sf.drftpd.event;

/**
 * @author mog
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class GlftpdLog implements FtpListener {


	public void actionPerformed(FtpEvent event) {
		if(event.getCommand().equals("NUKE")) {
			NukeEvent nevent = (NukeEvent)event;
			
		}
	}
	

}
