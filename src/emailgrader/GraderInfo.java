package emailgrader;

public class GraderInfo 
{
    //
    // SQLUtil settings.
    //
    public static final String GRADER_DATA_SQLITE_FILE      = "graderdata.sqlite"; // the name of the SQLite3 file containing the grader data.
    public static final long START_SUBMISSION_ID            = 0L; // the ID number to call the first submission, counting up from there.
    
    //
    // EmailUtil settings.
    //
    public static final String IMAP_HOSTNAME                = "imap.gmail.com"; // the hostname the IMAP service runs on
    public static final int IMAP_PORT                       = 993; // the port IMAP service runs on for the given hostname
    public static final boolean IMAP_USE_TLS                = true; // whether to use TLS for IMAP authentication
    public static final String SMTP_HOSTNAME                = "smtp.gmail.com"; // the hostname the SMTP service runs on
    public static final int SMTP_PORT                       = 587; // the port SMTP service runs on for the given hostname
    public static final boolean SMTP_USE_TLS                = true; // whether to use TLS for SMTP authentication
    public static final String EMAIL_LOGIN_ADDR             = "user@gmail.com";  // TODO: change this!
    public static final String EMAIL_LOGIN_PWD              = "YourPasswordOwO"; // TODO: change this too!
    public static final long EMAIL_INBOX_REFRESH_RATE       = 5000L; // refresh rate for checking email inbox, milliseconds
    public static final int MAX_ATTACHMENT_SIZE             = 50000; // maximum size of email attachment, bytes. (default = 50KB)
    
    //
    // SubmissionProcessorRunnable settings.
    //
    public static final int MAX_PROCESSOR_THREADS           = 8;
    public static final long PROCESSOR_QUERY_RATE           = 1000L; // query rate for submission processor to check queued submissions, milliseconds
    public static final String PROBLEM_SET_FOLDER           = "problems";
    public static final int MAXIMUM_SUBMISSION_COUNT        = 2; // maximum submission count per problem, per team.
    public static final int POINTS_PER_PROBLEM              = 3;
    public static final boolean ENFORCE_TIME_LIMITS         = false;
    public static final long CONTEST_START_DATE             = 1595707200000L - 300000L; // milliseconds past 1970 midnight (UTC) that the contest starts. (this corresponds to 7/25, 1255 hrs UTC-7)
    public static final long CONTEST_END_DATE               = 1595718000000L; // milliseconds past 1970 midnight (UTC) that the contest ends.   (this corresponds to 7/25, 1600 hrs UTC-7)
    public static final int MAX_NUM_TEAMS_PER_DIVISION      = 200;
    
    //
    // ProgramIOUtil settings.
    //
    public static final String JAVA_LOCATION                = "/usr/bin/java";
    public static final String JAVAC_LOCATION               = "/usr/bin/javac";
    public static final String GCC_LOCATION                 = "/usr/bin/gcc";
    public static final String GPP_LOCATION                 = "/usr/bin/g++";
    public static final String PYTHON2_LOCATION             = "/usr/bin/python2";
    public static final String PYTHON3_LOCATION             = "/usr/bin/python3";
    public static final String CSHARP_BUILD_LOCATION        = "/usr/bin/mcs";
    public static final String CSHARP_RUN_LOCATION          = "/usr/bin/mono";
    public static final String BASH_SHELL_LOCATION          = "/bin/bash";
    public static final String SUBMISSION_UPLOAD_FOLDER     = "submissions";
    public static final int COMPILE_MEM_LIMIT               = 1000000; // memory limit for program compilation, kilobytes.
    public static final int EXECUTE_MEM_LIMIT               = 256000;  // memory limit for program execution, kilobytes.
    public static final long COMPILE_TIME_LIMIT             = 3000L;
    public static final long EXECUTE_TIME_LIMIT             = 3000L;
    
    //
    // SheetsAuthUtil/SheetsInteractor settings.
    //
    public static final boolean USE_SHEETS                  = true;
    public static final String SHEETS_APPLICATION_NAME      = "TeamsCodeEmailGrader";
    public static final String SHEETS_CRED_FILE_LOCATION    = "sheetsinfo/credentials.json";
    public static final String SHEETS_AUTH_TOKEN_FOLDER     = "tokens";
    public static final String SHEETS_SPREADSHEET_URLID     = "1-frkc0jWCMBJ23HUELL0I0mpUn_-KyrpXOpSR53Ugc0";
}
