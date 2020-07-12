package emailgrader;

import java.io.IOException;

import javax.mail.Message;
import javax.mail.MessagingException;

public class GraderMain 
{
	public static void main(String[] args) throws IOException, MessagingException
	{
		EmailUtil es = new EmailUtil("pop.gmail.com", 995, true, "smtp.gmail.com", 587, true, "email@gmail.com", "YourPasswordOwO", 10000L)
		{
			public void onReceivedMessageCallback(Message message)
			{
				try
				{
					Message[] messages = this.fetchInboxMessages(false);
		    		for (Message m : messages)
		    		{
		    			System.out.println("------------------------");
		    			String subject = m.getSubject();
		    			System.out.println("subject:" + subject);
		    			System.out.println("text:" + this.getTextFromMessage(m));
		    			if (subject.equals("reply"))
		    			{
		    				System.out.println("Replying");
		    				this.replyToInboxMessage(m, "Yo " + m.getFrom()[0].toString() + " whatssup!");
		    			}
		    			System.out.println("------------------------");
		    		}
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		};
		
		
	}
}
