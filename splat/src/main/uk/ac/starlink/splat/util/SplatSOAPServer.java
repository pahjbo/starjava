/*
 * Copyright (C) 2002-2004 Central Laboratory of the Research Councils
 *
 *  History:
 *     17-JUN-2002 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.util;

import java.net.URL;
import java.io.IOException;
import java.net.Socket;

import org.w3c.dom.Element;

import uk.ac.starlink.soap.AppHttpSOAPServer;
import uk.ac.starlink.splat.iface.SplatBrowser;
import uk.ac.starlink.splat.iface.PlotControlFrame;
import uk.ac.starlink.splat.data.SpecData;
import uk.ac.starlink.splat.data.NDXSpecDataImpl;

/**
 * Implements the SOAP web services offered by the SPLAT application. There is
 * only one instance of this class for the SPLAT application, but it does not
 * come into existence until the {@link #getInstance()} method is invoked.
 * <p>
 * The port used to communicate with this server is chosen by using a given
 * base number and searching from that for the first free port (the default
 * search point is 8081).
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class SplatSOAPServer
{
    /** 
     * The application HTTP/SOAP server 
     */
    private AppHttpSOAPServer server = null;

    /**
     * The instance of SplatBrowser that we're attached to.
     */
    private SplatBrowser browserMain = null;

    /** 
     * The port number being used for the server.
     */
    private int portNumber = 8081;

    /**
     * The instance.
     */
    private static SplatSOAPServer instance = null;

    /**
     * Get the instance. Uses lazy instantiation so object does not exist
     * until the first invocation of this method. Make sure that the
     * SplatBrowser to be used is set before making any of of this reference
     * {@link #setSplatBrowser}.
     */
    public static SplatSOAPServer getInstance()
    {
        if ( instance == null ) {
            try {
                instance = new SplatSOAPServer();
            }
            catch (IOException e) {
                e.printStackTrace();
                instance = null;
            }
        }
        return instance;
    }

    /**
     * Listen for remote RPC SOAP requests.
     */
    private SplatSOAPServer()
        throws IOException
    {
        //  Do nothing.
    }

    /**
     * Set the instance of SplatBrowser that we're attached to.
     */
    public void setSplatBrowser( SplatBrowser browserMain )
    {
        this.browserMain = browserMain;
    }

    /**
     * Get the instance of SplatBrowser that we're attached to.
     */
    public SplatBrowser getSplatBrowser()
    {
        return browserMain;
    }

    /**
     * Set the base port number to be used when starting the HTTP server.
     * This may not be the actual port used.
     */
    public void setPortNumber( int portNum ) 
    {
        portNumber = portNum;
    }

    /**
     * Return the port number being used by this application.
     */
    public int getPortNumber()
    {
        return portNumber;
    }

    /**
     * Start the remote services. Do not forget to use this method as
     * no services are available until after it is invoked. 
     */
    public void start()
    {
        //  Create the HTTP/SOAP server. Need our local description to
        //  define the SOAP services offered (by this class). 
        URL deployURL = SplatSOAPServer.class.getResource( "deploy.wsdd" );

        try {
            server = new AppHttpSOAPServer( portNumber );
            server.start();
            server.addSOAPService( deployURL );
            
            //  Port may have been switched, so get port value back.
            portNumber = server.getPort();
            System.out.println( "Remote services port: " + portNumber );
        }
        catch ( Exception e ) {
            e.printStackTrace();
            throw new RuntimeException( "Failed to start SPLAT SOAP services" );
        }
    }

    /**
     * Stop the remote control services.
     */
    public void stop()
    {
        if ( server != null ) {
            try {
                server.stop();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

//
// Define the actual services, these are mediated through a static
// class SOAPServices.
//
    /**
     * Display a spectrum by name.
     *
     * @param specspec the spectrum specification
     */
    public boolean displaySpectrum( String specspec )
    {
        PlotControlFrame plot = browserMain.displaySpectrum( specspec );
        return (plot == null);
    }


    /**
     * Accept an NDX as Element description and display it.
     */
    public void displayNDX( Element ndxElement )
    {
        try {
            NDXSpecDataImpl impl = new NDXSpecDataImpl( ndxElement );
            SpecData spectrum = new SpecData( impl );
            browserMain.addSpectrum( spectrum );
            browserMain.displaySpectrum( spectrum );
        }
        catch( SplatException e ) {
            e.printStackTrace();
        }
    }
}
