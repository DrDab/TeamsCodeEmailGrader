package emailgrader;

public class GraderInfo 
{
    //
    // SQLUtil settings.
    //
    public static final String GRADER_DATA_SQLITE_FILE  = "graderdata.sqlite"; // the name of the SQLite3 file containing the grader data.
    public static final long START_SUBMISSION_ID        = 0L; // the ID number to call the first submission, counting up from there.
    
    //
    // EmailUtil settings.
    //
    public static final String IMAP_HOSTNAME            = "imap.gmail.com"; // the hostname the IMAP service runs on
    public static final int IMAP_PORT                   = 993; // the port IMAP service runs on for the given hostname
    public static final boolean IMAP_USE_TLS            = true; // whether to use TLS for IMAP authentication
    public static final String SMTP_HOSTNAME            = "smtp.gmail.com"; // the hostname the SMTP service runs on
    public static final int SMTP_PORT                   = 587; // the port SMTP service runs on for the given hostname
    public static final boolean SMTP_USE_TLS            = true; // whether to use TLS for SMTP authentication
    public static final String EMAIL_LOGIN_ADDR         = "user@gmail.com";  // TODO: change this!
    public static final String EMAIL_LOGIN_PWD          = "YourPasswordOwO"; // TODO: change this too!
    public static final long EMAIL_INBOX_REFRESH_RATE   = 10000L; // refresh rate for checking email inbox, milliseconds
    
    //
    // SubmissionProcessor settings.
    //
    public static final long PROCESSOR_QUERY_RATE       = 1000L; // query rate for submission processor to check queued submissions, milliseconds
    public static final String JAVA_LOCATION            = "/usr/bin/java";
    public static final String JAVAC_LOCATION           = "/usr/bin/javac";
    public static final String GCC_LOCATION             = "/usr/bin/gcc";
    public static final String GPP_LOCATION             = "/usr/bin/g++";
    public static final String PYTHON2_LOCATION         = "/usr/bin/python2";
    public static final String PYTHON3_LOCATION         = "/usr/bin/python3";
    public static final String CSHARP_BUILD_LOCATION    = "/usr/bin/mcs";
    public static final String CSHARP_RUN_LOCATION      = "/usr/bin/mono";
}
