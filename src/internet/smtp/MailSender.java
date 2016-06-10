package internet.smtp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.xml.bind.DatatypeConverter;

public class MailSender 
{
	private static MailSenderLogger logger = new MailSenderLogger( );
	
	//Default
	private String _userName = "vyrostkoolga@yandex.ru";
	private String _userPassword = "Admin000";
	
	//Default
	private String _host = "smtp.yandex.ru";
	private int _port = 465;
	
	//Determine if authorization is necessary
	private boolean _auth = false;
	private boolean _wantAuth = false;
	
	//Determine if we could use some extensions 
	private boolean _size = false;
	private boolean _pipelining = false;
	private boolean _starttls = false;
	
	//Message to send
	private MimeMessage _message;
	
	private BufferedReader _reader;
	private BufferedWriter _writer;
	private Socket _sock;
	
	public MailSender( )
	{
		//TODO: future initialization if it's necessary
	}
	
	public MailSender( String host, int port )
	{
		_host = host;
		_port = port;
	}
	
	public MailSender( String host, int port, String userName, String userPassword )
	{
		_host = host;
		_port = port;
		_userName = userName;
		_userPassword = userPassword;
		
		_wantAuth = true;
	}
	
	public static MailSenderLogger getLogger( ) {return logger;}
	
	private void createSocket( ) throws Exception
	{
		SSLSocketFactory ssf = ( SSLSocketFactory ) SSLSocketFactory.getDefault();
		try
		{
			_sock = ssf.createSocket( );
			_sock.connect(new InetSocketAddress( _host, _port ), 6000);
			
			_reader = new BufferedReader( new InputStreamReader(_sock.getInputStream()));
			_writer = new BufferedWriter( new OutputStreamWriter( _sock.getOutputStream()));
			
			connect( );
		}
		catch ( SSLException ex )
		{
			_sock = new Socket( );
			_sock.connect( new InetSocketAddress( _host, _port ), 6000 );
			
			_reader = new BufferedReader( new InputStreamReader(_sock.getInputStream()));
			_writer = new BufferedWriter( new OutputStreamWriter( _sock.getOutputStream()));
			
			connect( );
		}
		
		_sock.setSoTimeout( 5000 );
		
	}
	
	public void send( String text, List<String> to, String subject ) throws Exception
	{
		try
		{
			createSocket( );
			if ( _sock == null )
			{
				throw new Exception( "Could not connect" );
			}
			
			try
			{	
				if ( _pipelining )
				{
					pipelining( _userName, to, subject, text );
					disconnect( );
				}
				else
				{
					sendData( to, subject, text );
					disconnect( );
				}
			}
			finally
			{
				_writer.flush();
				_writer.close();
				_reader.close();
					
				_sock.close();
			}
		}
		catch ( Exception ex )
		{
			logger.writeClientLog(String.format("Could not send message: %s", ex) );
			throw ex;
		}
	}
	
	private void connect( ) throws Exception
	{
		//Hello
		writeAndFlush( "ehlo host" );
		
		String resp = _reader.readLine();
		logger.writeServerLog( resp );
		
		String code = resp.substring(0, 3);
		if (code.startsWith("5"))
		{
			throw new IOException( resp );
		}
		
		resp = _reader.readLine( );
		readExtensions( resp );
		
		if ( _starttls )
		{
			writeAndFlush( "STARTTLS" );
			resp = _reader.readLine( );
			readMultiLine( resp );
			
			SSLSocketFactory ssf = ( SSLSocketFactory ) SSLSocketFactory.getDefault();
			SSLSocket s = (SSLSocket) ssf.createSocket( _sock, _sock.getInetAddress().getHostAddress(), 
														_sock.getPort(), true);
			s.startHandshake( );
			_sock = s;
			
			_reader = new BufferedReader( new InputStreamReader(_sock.getInputStream()));
			_writer = new BufferedWriter( new OutputStreamWriter( _sock.getOutputStream()));
		}
		
		if ( _auth )
		{
			if ( ! _wantAuth )
			{
				throw new Exception( "Need auth to send letter, but auth is necessary");
			}
			
			//Authorization
			writeAndFlush( "auth login" );
			resp = _reader.readLine();
			readMultiLine( resp );
		
			writeAndFlush(DatatypeConverter.printBase64Binary(_userName.getBytes()));
			resp = _reader.readLine();
			readMultiLine( resp );
		
			writeAndFlush(DatatypeConverter.printBase64Binary(_userPassword.getBytes()));
			resp = _reader.readLine();
			readMultiLine( resp );
		}
	}
	
	private void sendHeaders( String from, List<String> to ) throws Exception
	{
		if ( _size )
		{
			String mes = _message.getMessage( );
			writeAndFlush( String.format("mail from: <%s> SIZE=%s", from, mes.getBytes().length ) );
		}
		else
		{
			writeAndFlush( String.format("mail from: <%s>", from ) );
		}
		
		String resp = _reader.readLine();
		readMultiLine(resp);
		
		for (String one: to)
		{
			_writer.write(String.format( "RCPT TO:<%s>\n", one ) );
			logger.writeClientLog(String.format( "RCPT TO:<%s>\n", one ) );
		}
		_writer.flush( );
		
		resp = _reader.readLine();
		readMultiLine( resp );
	}
	
	private void sendData( List<String> to, String subject, String text ) throws Exception
	{
		_message = new MimeMessage( to, _userName,  subject, text );
		
		sendHeaders( _userName, to );
		
		writeAndFlush("DATA");
		
		String resp = _reader.readLine();
		readMultiLine( resp );
		
		String mes = _message.getMessage( );
		_writer.write( mes );
		_writer.write("\r\n");
		_writer.write(".\r\n");
		_writer.flush();
		
		resp = _reader.readLine();
		readMultiLine( resp );
	}
	
	private void disconnect( ) throws IOException
	{
		writeAndFlush("QUIT");
		
		String resp = _reader.readLine();
		readMultiLine( resp );
	}
	
	private void pipelining( String from, List<String> to, String subject, String text ) throws Exception
	{	
		_message = new MimeMessage( to, _userName,  subject, text );
		String mes = _message.getMessage( );
		
		StringBuffer buffer = new StringBuffer( );
		if ( _size )
		{
			long length = mes.getBytes( ).length;
			buffer.append(String.format("MAIL FROM: <%s> SIZE=%s\n", from, length ) );
		}
		else
		{
			buffer.append(String.format("MAIL FROM: <%s>\n", from ) );
		}
		
		for (String one: to)
		{
			buffer.append( String.format("RCPT TO: <%s>\n", one) );
		}
		buffer.append("DATA");
		writeAndFlush( new String( buffer ) );
		
		//For from
		String resp = _reader.readLine( );
		readMultiLine( resp );
		//For to
		resp = _reader.readLine( );
		readMultiLine( resp );
		//For data
		resp = _reader.readLine( );
		readMultiLine( resp );
		
		_writer.write( _message.getMessage( ) );
		_writer.flush( );
		_writer.write("\r\n.\r\n");
		_writer.flush( );
		
		resp = _reader.readLine( );
		readMultiLine( resp );
	}
	
	private void readMultiLine( String resp ) throws IOException
	{
		logger.writeServerLog( resp );
		
		String code = resp.substring(0, 3);
		if (code.startsWith("5"))
		{
			throw new IOException( resp );
		}
		
		String start = String.format("%s ", code);
		while (! resp.startsWith( start ) )
		{
			resp = _reader.readLine();
			logger.writeServerLog( resp );
		}
	}
	
	private void readExtensions( String resp ) throws IOException
	{
		logger.writeServerLog( resp );
		
		String code = resp.substring(0, 3);
		if (code.startsWith("5"))
		{
			throw new IOException( resp );
		}
		
		String start = String.format("%s ", code);
		while (! resp.startsWith( start ) )
		{
			if ( resp.contains( "SIZE" ) )
			{
				_size = true;
			}
			else if (resp.contains("PIPELINING"))
			{
				_pipelining = true;
			}
			else if ( resp.contains( "STARTTLS") )
			{
				_starttls = true;
			}
			else if ( resp.contains("AUTH") )
			{
				_auth = true;
			}
			
			resp = _reader.readLine( );
			logger.writeServerLog( resp );
		}
	}
	
	private void writeAndFlush( String line ) throws IOException
	{
		logger.writeClientLog(line);
		
		_writer.write( String.format("%s\n", line ) );
		_writer.flush( );
	}
}
