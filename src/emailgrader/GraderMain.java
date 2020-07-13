package emailgrader;

import java.io.IOException;
import java.sql.SQLException;

import javax.mail.MessagingException;

import graderio.SQLUtil;
import graderobjects.ContestSubmission;

public class GraderMain 
{
    public static void main(String[] args) throws IOException, MessagingException, SQLException
    {
        SQLUtil util = new SQLUtil("graderdata.sqlite");
        util.addSubmissionToQueue(2L, "testEmail4@gmail.com", 6122969425L, "Test Subject 3", "Test Body 3 OwO");
        ContestSubmission subTest = util.getNextPendingSubmission();
        System.out.println(subTest.id);
        System.out.println(subTest.body);
        System.out.println(subTest.state);
        long subsLeft = util.getPendingSubmissionCount();
        System.out.println(subsLeft);
        subTest.miscInfo = "*notices ur test* OwO whats this UwU";
        util.updateSubmissionStatus(subTest);
    }
}
