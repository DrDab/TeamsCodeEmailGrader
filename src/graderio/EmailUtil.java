package graderio;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import javax.mail.BodyPart;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.UIDFolder;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.search.FlagTerm;

import emailgrader.GraderInfo;
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
    }

    private void initQueryTask()
    {
        this.queryTask = new TimerTask()
        {
            public void run()
            {
                try
                {
                    // System.out.println("Began query");
                    Store store = getStore();
                    UIDMessageEncapsulator[] messageEncapsulators = fetchInboxMessages(false, store);
                    for (UIDMessageEncapsulator m : messageEncapsulators)
                    {
                        onReceivedMessageCallback(m);
                    }
                    store.close();
                    // System.out.println("End query");
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
        this.initQueryTask();
        this.queryTimer = new Timer();
        this.queryTimer.scheduleAtFixedRate(this.queryTask, 0L, this.queryPeriod);
        this.queryTaskActive = true;
    }

    public void stopQueryTask()
    {
        this.queryTimer.cancel();
        this.queryTimer.purge();
        this.queryTaskActive = false;
    }

    public boolean isQueryTaskActive()
    {
        return this.queryTaskActive;
    }

    public void replyToMessage(Message message, String replyBody) throws AddressException, MessagingException
    {
        Message replyMessage = new MimeMessage(this.emailSession);
        replyMessage = (MimeMessage) message.reply(false);
        replyMessage
            .setFrom(new InternetAddress(InternetAddress.toString(message.getRecipients(Message.RecipientType.TO))));
        replyMessage.setText(replyBody);
        replyMessage.setReplyTo(message.getReplyTo());
        this.smtpTransport.sendMessage(replyMessage, replyMessage.getAllRecipients());
    }

    public Store getStore() throws MessagingException
    {
        Store store = emailSession.getStore("imaps");
        store.connect(this.imapHost, this.imapPort, this.username, this.password);
        return store;
    }

    public Message getMessageByUid(long uid, Store store) throws MessagingException
    {
        Folder emailFolder = store.getFolder("INBOX");
        emailFolder.open(Folder.READ_WRITE);
        UIDFolder uidFolder = (UIDFolder) emailFolder;
        Message toReturn = uidFolder.getMessageByUID(uid);
        return toReturn;
    }

    public UIDMessageEncapsulator[] fetchInboxMessages(boolean read, Store store) throws MessagingException
    {
        // search for all "unseen" messages
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

    public HashMap<String, Byte[]> getAttachmentsFromMessage(Message message) throws MessagingException, IOException
    {
        String contentType = message.getContentType();

        // store attachment file name, separated by comma
        String attachFiles = "";

        if (contentType.contains("multipart"))
        {
            // content may contain attachments
            Multipart multiPart = (Multipart) message.getContent();
            int numberOfParts = multiPart.getCount();
            for (int partCount = 0; partCount < numberOfParts; partCount++)
            {
                MimeBodyPart part = (MimeBodyPart) multiPart.getBodyPart(partCount);
                if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition()))
                {
                    // this part is attachment
                    String fileName = part.getFileName();
                    InputStream is = part.getInputStream();
                    int fileSize = is.available();
                    if (fileSize > GraderInfo.MAX_ATTACHMENT_SIZE)
                    {
                        continue;
                    }
                    byte[] buf = new byte[fileSize];
                    is.read(buf, 0, fileSize);
                }
            }

        }

        // print out details of each message
        System.out.println("\t Attachments: " + attachFiles);
        return null;
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
