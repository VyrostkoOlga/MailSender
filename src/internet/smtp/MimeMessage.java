package internet.smtp;

import java.io.File;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.imageio.ImageIO;

public class MimeMessage 
{
	private List<String> _to;
	private String _from;
	private String _text;
	private String _subject;
	
	private File _curDir;
	
	private List<String> _atts;
	
	public MimeMessage( List<String> to, String from, String subject, String text )
	{
		_to = to;
		_from = from;
		_text = text;
		_subject = subject;
		
		_curDir = new File( new File( "./" ).getAbsolutePath() ).getParentFile();
		_atts = new ArrayList<String>( );
		addAttachments( _curDir );
	}
	
	public String getMessage( ) throws Exception
	{
		StringBuffer buffer = new StringBuffer( );
		
		buffer.append(String.format("FROM: %s <%s>\n", getLogical( _from ), _from) );
		for (String one: _to)
		{
			buffer.append(String.format("TO: %s <%s>\n", getLogical( one ), one ) );
		}
		buffer.append(String.format("SUBJECT: %s\n", _subject ) );
		buffer.append("Content-Type: multipart/mixed; boundary=gc0p4Jq0M2Yt08jU534c0p\n\n");
		
		buffer.append("--gc0p4Jq0M2Yt08jU534c0p\n");
		buffer.append(String.format("Content-Type: text/plain; charset=%s\n", 
									System.getProperty("file.encoding")));
		buffer.append("Content-Transfer-Encoding: quoted-printable\n\n");
		buffer.append(_text);
		buffer.append("\n");
		
		String current;
		String currentType;
		File f;
		for (String one: _atts)
		{
			current = addAttachment( one );
			if (current != null)
			{
				f = new File( one );
				currentType = FileHelper.getFileType( one );
				buffer.append("--gc0p4Jq0M2Yt08jU534c0p\n");
				buffer.append(String.format("Content-Disposition: attachment; filename=\"%s\"\n", 
											f.getAbsolutePath() ) );
				buffer.append("Content-Transfer-Encoding: base64\n");
				buffer.append(String.format("Content-Type: %s; name=\"%s\"\n\n", currentType, f.getName() ) );
				buffer.append(addAttachment(one));
				buffer.append("\n\n");
			}
		}
		
		buffer.append("--gc0p4Jq0M2Yt08jU534c0p--");
		return new String(buffer);
	}
	
	private String getLogical( String email )
	{
		try
		{
			return email.substring(0, email.indexOf("@") );
		}
		catch (Exception ex)
		{
			return email;
		}
	}
	
	public void addAttachments( File dir )
	{
		for (File file: dir.listFiles())
		{
			if (file.isDirectory())
			{
				addAttachments( file );
			}
			else if (file.isFile())
			{
				if (file.canRead() && !file.isHidden())
				{
					try
					{
						if (ImageIO.read(file) != null)
						{
							_atts.add(file.getAbsolutePath());
						}
					}
					catch (Exception ex)
					{
						
					}
				}
			}
		}
	}
	
	public String addAttachment( String file ) throws Exception
	{
		try
		{
			File curFile = new File( file );
			byte [ ] bytes = FileHelper.readBinary( curFile.getAbsolutePath() );
			return Base64.getMimeEncoder().encodeToString( bytes );
		}
		catch (Exception ex)
		{
			MailSender.getLogger().writeErrorLog( String.format( "Could not add attachment %s: %s", 
																file, ex.getMessage() ) );
			return null;
		}
	}
}
