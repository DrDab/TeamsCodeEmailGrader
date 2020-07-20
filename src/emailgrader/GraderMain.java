package emailgrader;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.util.HashMap;

import javax.mail.Message;
import javax.mail.MessagingException;

import graderio.EmailUtil;
import graderio.ProgramIOUtil;
import graderio.SQLUtil;
import graderio.SubmissionProcessor;
import graderobjects.UIDMessageEncapsulator;
import sheetsintegration.SheetsAuthUtil;
import sheetsintegration.SheetsInteractor;

public class GraderMain
{
    public static void main(String[] args)
        throws IOException, MessagingException, SQLException, InterruptedException, GeneralSecurityException
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
                    HashMap<String, Byte[]> attachments = this.getAttachmentsFromMessage(message);
                    sqlUtil.addSubmissionToQueue(uid, senderEmail, dateReceived, subject, body, attachments);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        };
        ProgramIOUtil programIOUtil = new ProgramIOUtil();
        SheetsAuthUtil sheetsAuthUtil = GraderInfo.USE_SHEETS
            ? new SheetsAuthUtil(GraderInfo.SHEETS_APPLICATION_NAME, GraderInfo.SHEETS_CRED_FILE_LOCATION,
                GraderInfo.SHEETS_AUTH_TOKEN_FOLDER, false)
            : null;
        SheetsInteractor sheetsInteractor = GraderInfo.USE_SHEETS
            ? new SheetsInteractor(sheetsAuthUtil.getSheetsService(sheetsAuthUtil.getCredentials()),
                GraderInfo.SHEETS_SPREADSHEET_URLID)
            : null;
        SubmissionProcessor submissionProcessor = new SubmissionProcessor(sqlUtil, emailUtil, programIOUtil,
            sheetsInteractor, GraderInfo.PROCESSOR_QUERY_RATE, GraderInfo.MAX_PROCESSOR_THREADS);
        submissionProcessor.startProcessor();
        emailUtil.startQueryTask();
    }
}