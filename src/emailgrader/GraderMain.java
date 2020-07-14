package emailgrader;

import java.io.IOException;
import java.sql.SQLException;

import javax.mail.Message;
import javax.mail.MessagingException;

import graderio.EmailUtil;
import graderio.SQLUtil;
import graderio.SubmissionProcessor;
import graderobjects.UIDMessageEncapsulator;

public class GraderMain
{
    public static void main(String[] args) throws IOException, MessagingException, SQLException
    {
        SQLUtil sqlUtil = new SQLUtil("graderdata.sqlite");
        EmailUtil emailUtil = new EmailUtil("imap.gmail.com", 993, true, "smtp.gmail.com", 587, true, "user@gmail.com", "YourPasswordOwO", 10000L)
        {
            public void onReceivedMessageCallback(UIDMessageEncapsulator uidMessageEncapsulator)
            {
                try
                {
                    long uid = uidMessageEncapsulator.uid;
                    Message message = uidMessageEncapsulator.message;
                    String senderEmail = message.getFrom()[0].toString();
                    long dateReceived = message.getReceivedDate().getTime();
                    String subject = message.getSubject();
                    String body = this.getTextFromMessage(message);
                    sqlUtil.addSubmissionToQueue(uid, senderEmail, dateReceived, subject, body);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        };
        SubmissionProcessor submissionProcessor = new SubmissionProcessor(sqlUtil, emailUtil);
        submissionProcessor.startProcessor();
        emailUtil.startQueryTask();
    }
}