package net.sf.drftpd.master;

import java.io.IOException;
import java.io.Writer;
import java.io.OutputStreamWriter;
import java.io.OutputStream;
import java.net.Socket;

//import ranab.util.Message;


/**
 * Writer object used by the server. It has the spying capability.
 *
 * @author <a href="mailto:rana_b@yahoo.com">Rana Bhattacharyya</a>
 */
public
class FtpWriter extends Writer {

    private OutputStreamWriter mOriginalWriter;
    /*
    private SpyConnectionInterface mSpy;
    private FtpConfig mConfig;
    */
    
    /**
     * Constructor - set the actual writer object
     */
    public FtpWriter(OutputStream soc) throws IOException {
        mOriginalWriter = new OutputStreamWriter(soc); 
        //mConfig = config;
    }
    
    /**
     * Get the spy object to get what the user is writing.
     */
    /*
    public SpyConnectionInterface getSpyObject() {
        return mSpy;
    }
    */

    /**
     * Set the connection spy object.
     */
    /*
    public void setSpyObject(SpyConnectionInterface spy) {
        mSpy = spy;
    }
    */

    /**
     * Spy print. Monitor server response.
     */
    private void spyResponse(String str) throws IOException {
	System.out.println(str);
	/*
        final SpyConnectionInterface spy = mSpy;
        if (spy != null) {
            Message msg = new Message() {
                public void execute() {
                    try {
                        spy.response(str);
                    }
                    catch(Exception ex) {
                        mSpy = null;
                        mConfig.getLogger().error(ex);
                    }
                }
            };
            mConfig.getMessageQueue().add(msg);
        }
	*/
    }

    /**
     * Write a character array.
     */
    public void write(char[] cbuf) throws IOException {
        String str = new String(cbuf);
        spyResponse(str);
        mOriginalWriter.write(cbuf);
        mOriginalWriter.flush();
    }
    
    /**
     * Write a portion of character array
     */
    public void write(char[] cbuf, int off, int len) throws IOException {
        String str = new String(cbuf, off, len);
        spyResponse(str);
        mOriginalWriter.write(cbuf, off, len);
        mOriginalWriter.flush();
    }
    
    /**
     * Write a single character
     */
    public void write(int c) throws IOException {
        String str = "" + (char)c;
        spyResponse(str);
        mOriginalWriter.write(c);
        mOriginalWriter.flush();
    }
    
    /**
     * Write a string
     */
    public void write(String str) throws IOException {
        spyResponse(str);
        mOriginalWriter.write(str);
        mOriginalWriter.flush();
    }
    
    /**
     * Write a portion of the string.
     */
    public void write(String str, int off, int len) throws IOException {
        String strpart = str.substring(off, len);
        spyResponse(strpart);
        mOriginalWriter.write(str, off, len);
        mOriginalWriter.flush();
    }
    
    /**
     * Close writer.
     */
    public void close() throws IOException {
        mOriginalWriter.close();
    }
    
    /**
     * Flush the stream
     */
    public void flush() throws IOException {
        mOriginalWriter.flush();
    }
    
}
