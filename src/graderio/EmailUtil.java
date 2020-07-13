package graderio;

import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import javax.mail.BodyPart;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.UIDFolder;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.search.FlagTerm;

import graderobjects.UIDMessageEncapsulator;

public abstract class EmailUtil
{
    private String imapHost;
    private int imapPort;
    private boolean enableImapTls;
    private String smtpHost;
    private int smtpPort;
    private boolean enableSmtpTls;
    private String username;
    private String password;
    private long queryPeriod;

    private Properties properties;
    private Session emailSession;
    private Store store;

    private Transport smtpTransport;

    private Timer queryTimer;
    private TimerTask queryTask;
    private boolean queryTaskActive;

    public EmailUtil(String imapHost, int imapPort, boolean enableImapTls, String smtpHost, int smtpPort,
        boolean enableSmtpTls, String username, String password, long queryPeriod) throws MessagingException
    {
        this.imapHost = imapHost;
        this.imapPort = imapPort;
        this.enableImapTls = enableImapTls;
        this.smtpHost = smtpHost;
        this.smtpPort = smtpPort;
        this.enableSmtpTls = enableSmtpTls;
        this.username = username;
        this.password = password;
        this.queryPeriod = queryPeriod;
        this.properties = new Properties();
        this.properties.put("mail.store.protocol", "imaps");
        this.properties.put("mail.imap.host", this.imapHost);
        this.properties.put("mail.imap.port", this.imapPort + "");
        this.properties.put("mail.imap.starttls.enable", this.enableImapTls + "");
        this.properties.put("mail.smtp.auth", "true");
        this.properties.put("mail.smtp.starttls.enable", this.enableSmtpTls + "");
        this.properties.put("mail.smtp.host", this.smtpHost);
        this.properties.put("mail.smtp.port", this.smtpPort + "");
        this.emailSession = Session.getDefaultInstance(this.properties);
        this.smtpTransport = this.emailSession.getTransport("smtp");
        this.smtpTransport.connect(this.username, this.password);
        this.queryTask = new TimerTask()
        {
            public void run()
            {
                try
                {
                    System.out.println("Began query");
                    UIDMessageEncapsulator[] messageEncapsulators = fetchInboxMessages(false);
                    for (UIDMessageEncapsulator m : messageEncapsulators)
                    {
                        onReceivedMessageCallback(m);
                    }
                    System.out.println("End query");
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        };
        this.queryTaskActive = false;
    }

    public void startQueryTask()
    {
        this.queryTimer = new Timer();
        this.queryTimer.scheduleAtFixedRate(this.queryTask, 0L, this.queryPeriod);
        this.queryTaskActive = true;
    }

    public void stopQueryTask()
    {
        this.queryTimer.cancel();
        this.queryTaskActive = false;
    }

    public boolean isQueryTaskActive()
    {
        return this.queryTaskActive;
    }

    public void replyToInboxMessage(Message message, String replyBody) throws AddressException, MessagingException
    {
        Message replyMessage = new MimeMessage(this.emailSession);
        replyMessage = (MimeMessage) message.reply(false);
        replyMessage
            .setFrom(new InternetAddress(InternetAddress.toString(message.getRecipients(Message.RecipientType.TO))));
        replyMessage.setText(replyBody);
        replyMessage.setReplyTo(message.getReplyTo());
        this.smtpTransport.sendMessage(replyMessage, replyMessage.getAllRecipients());
    }

    public UIDMessageEncapsulator[] fetchInboxMessages(boolean read) throws MessagingException
    {
        // search for all "unseen" messages
        if (this.store != null)
        {
            this.store.close();
        }
        this.store = emailSession.getStore("imaps");
        this.store.connect(this.imapHost, this.imapPort, this.username, this.password);
        Folder emailFolder = store.getFolder("INBOX");
        UIDFolder uidFolder = (UIDFolder) emailFolder;
        emailFolder.open(Folder.READ_WRITE);
        Flags seen = new Flags(Flags.Flag.SEEN);
        FlagTerm unseenFlagTerm = new FlagTerm(seen, read);
        Message[] messages = emailFolder.search(unseenFlagTerm);
        UIDMessageEncapsulator[] encapsulators = new UIDMessageEncapsulator[messages.length];
        for (int i = 0; i < messages.length; i++)
        {
            encapsulators[i] = new UIDMessageEncapsulator(messages[i], uidFolder.getUID(messages[i]));
        }
        if (encapsulators.length > 0)
        {
            Arrays.sort(encapsulators);
        }
        return encapsulators;
    }

    public String getTextFromMessage(Message message) throws MessagingException, IOException
    {
        String result = "";
        if (message.isMimeType("text/plain"))
        {
            result = message.getContent().toString();
        }
        else if (message.isMimeType("multipart/*"))
        {
            MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();
            result = getTextFromMimeMultipart(mimeMultipart);
        }
        return result;
    }

    public String getTextFromMimeMultipart(MimeMultipart mimeMultipart) throws MessagingException, IOException
    {
        String result = "";
        int count = mimeMultipart.getCount();
        for (int i = 0; i < count; i++)
        {
            BodyPart bodyPart = mimeMultipart.getBodyPart(i);
            if (bodyPart.isMimeType("text/plain"))
            {
                result += bodyPart.getContent();
                break; // without break same text appears twice in my tests
            }

            else if (bodyPart.isMimeType("text/html"))
            {
                String html = (String) bodyPart.getContent();
                result = result + "\n" + html;
            }
            else if (bodyPart.getContent() instanceof MimeMultipart)
            {
                result = result + getTextFromMimeMultipart((MimeMultipart) bodyPart.getContent());
            }

        }
        return result;
    }

    public abstract void onReceivedMessageCallback(UIDMessageEncapsulator uidMessageEncapsulator);

}
