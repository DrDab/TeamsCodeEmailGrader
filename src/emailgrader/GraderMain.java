package emailgrader;

import java.io.IOException;
import java.sql.SQLException;

import javax.mail.MessagingException;

import graderio.SQLUtil;

public class GraderMain 
{
    public static void main(String[] args) throws IOException, MessagingException, SQLException
    {
        SQLUtil sqlUtil = new SQLUtil("graderdata.sqlite");
        
    }
}