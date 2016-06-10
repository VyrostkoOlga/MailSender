package internet.smtp;

import java.io.File;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

public class MailSenderLogger 
{
	private static final Logger logger = Logger.getLogger( MailSender.class.getName() );
	
	public MailSenderLogger( )
	{
		try
		{
			File f = new File( "./log/server.log" );
			f.getParentFile().mkdirs();
			f.createNewFile();
			logger.addHandler( new FileHandler( f.getPath() ) );
		}
		catch (Exception ex)
		{
			System.out.println("Could not create server log file. Use System.out");
			ex.printStackTrace();
		}
	}
	
	public void writeClientLog( String text )
	{
		logger.info(String.format("[Client: %s]", text) );
	}
	
	public void writeServerLog( String text )
	{
		logger.info( String.format("[Server: %s]", text));
	}
	
	public void writeErrorLog( String text )
	{
		logger.info( String.format( "[Error: %s]", text) );
	}
}
