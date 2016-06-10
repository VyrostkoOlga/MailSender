package internet.smtp;

import java.util.ArrayList;
import java.util.List;

public class Runner
{
	public static void main( String [] args )
	{
		if (args.length < 3 )
		{
			help( );
		}
		else if ( args[0].equalsIgnoreCase("--help") || 
				(args[0].equalsIgnoreCase("--h")) || 
				(args[0].equalsIgnoreCase("/?")))
		{
			help( );
		}
		else
		{
			try
			{
				String server = args[0];
				int port = Integer.parseInt( args[1] );
			
				MailSender sender;
		
				List<String> toList = new ArrayList<String> ( );
				for (String one: args[2].split(" "))
				{
					toList.add(one);
				}
			
				if ( args.length == 5 )
				{
					String userName = args[3];
					String userPassword = args[4];
				
					sender = new MailSender( server, port, userName, userPassword ); 
				}
				else
				{
					sender = new MailSender( server, port );
				}
		
				sender.send("Here they are", toList, "Pictures");
			}
			catch ( Exception ex)
			{
				System.out.println(String.format("Could not send message: %s", ex));
			}
		}
	}
	
	public static void help( )
	{
		System.out.println( "Not enough arguments" );
		System.out.println( "USAGE: SMTP.jar smtp server port \"email1 [email2 email3 ...]\" [user_name] [user_password]");
	}

}
