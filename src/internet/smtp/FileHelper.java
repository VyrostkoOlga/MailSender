package internet.smtp;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.activation.FileDataSource;

public class FileHelper 
{
	public static byte [ ] readBinary( String filename ) throws IOException
	{
		List<Integer> resultBytes = new ArrayList<Integer> ( );
		
		BufferedInputStream is = new BufferedInputStream( new FileInputStream( filename ) );
		try
		{
			int b ;
		
			while ((b = is.read()) >= 0)
			{
				resultBytes.add(b);
			}
			
			byte [] bytes = new byte[resultBytes.size( )];
			for (int i=0; i< resultBytes.size( ); i++)
			{
				bytes[i] = resultBytes.get(i).byteValue();
			}
			return bytes;
		}
		finally
		{
			is.close();
		}
	}
	
	public static String getFileType( String filename ) throws IOException
	{	
        return new FileDataSource( filename ).getContentType();
	}

}
