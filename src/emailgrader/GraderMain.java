package emailgrader;

import java.io.IOException;
import java.sql.SQLException;

import javax.mail.MessagingException;

import graderio.SQLUtil;

public class GraderMain 
{
	public static void main(String[] args) throws IOException, MessagingException, SQLException
	{
		SQLUtil util = new SQLUtil("graderdata.sqlite");
		util.addSubmissionToQueue("poop@gmail.com", 6969420L, "Uh Oh", "stimky");
	}
}
