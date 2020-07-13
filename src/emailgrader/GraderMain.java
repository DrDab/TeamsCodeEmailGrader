package emailgrader;

import java.io.IOException;

import javax.mail.Message;
import javax.mail.MessagingException;

import graderio.EmailUtil;
import graderobjects.UIDMessageEncapsulator;

public class GraderMain 
{
    public static void main(String[] args) throws IOException, MessagingException
    {
        EmailUtil es = new EmailUtil("imap.gmail.com", 993, true, "smtp.gmail.com", 587, true, "user@gmail.com", "YourPasswordOwO", 10000L)
        {
            public void onReceivedMessageCallback(UIDMessageEncapsulator uidMessageEncapsulator)
            {
                try
                {
                    System.out.println("------------------------");
                    Message m = uidMessageEncapsulator.message;
                    long uid = uidMessageEncapsulator.uid;
                    String subject = m.getSubject();
                    System.out.println("uid: " + uid);
                    System.out.println("subject:" + subject);
                    System.out.println("text:" + this.getTextFromMessage(m));
                    if (subject.equals("reply"))
                    {
                        System.out.println("Replying");
                        this.replyToMessage(m, "Yo " + m.getFrom()[0].toString() + " whatssup!");
                    }
                    System.out.println("------------------------");
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        };
        //es.startQueryTask();
        Message msg = es.getMessageByUid(230);
        System.out.println(msg.getSubject());
        System.out.println(es.getTextFromMessage(msg));
    }
}