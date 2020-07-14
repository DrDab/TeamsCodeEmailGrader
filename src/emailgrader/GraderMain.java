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
    public static void main(String[] args) throws IOException, MessagingException, SQLException, InterruptedException
    {
        SQLUtil sqlUtil = new SQLUtil(GraderInfo.GRADER_DATA_SQLITE_FILE);
        EmailUtil emailUtil = new EmailUtil(GraderInfo.IMAP_HOSTNAME, GraderInfo.IMAP_PORT, GraderInfo.IMAP_USE_TLS,
            GraderInfo.SMTP_HOSTNAME, GraderInfo.SMTP_PORT, GraderInfo.SMTP_USE_TLS, GraderInfo.EMAIL_LOGIN_ADDR,
            GraderInfo.EMAIL_LOGIN_PWD, GraderInfo.EMAIL_INBOX_REFRESH_RATE)
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