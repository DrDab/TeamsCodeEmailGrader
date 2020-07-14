package graderio;

import java.sql.SQLException;

import graderobjects.ContestSubmission;

@SuppressWarnings("unused")
public class SubmissionProcessorRunnable implements Runnable
{
    private SubmissionProcessor submissionProcessor;
    private SQLUtil sqlUtil;
    private EmailUtil emailUtil;
    private boolean finished;
    
    public SubmissionProcessorRunnable(SubmissionProcessor submissionProcessor, SQLUtil sqlUtil, EmailUtil emailUtil)
    {
        this.submissionProcessor = submissionProcessor;
        this.sqlUtil = sqlUtil;
        this.emailUtil = emailUtil;
        this.finished = false;
    }

    @Override
    public void run()
    {
        try
        {
            while (!submissionProcessor.isStopped())
            {
                while (sqlUtil.hasPendingSubmission())
                {
                    ContestSubmission cur = sqlUtil.getNextPendingSubmission();
                    // only thing we read from header is language and problem number.
                    // we will read the division and team name from x-referencing the sender email.
                    String subject = cur.subject;
                    String sender = cur.senderEmail;
                    
                }
            }
        }
        catch (SQLException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        this.finished = true;
    }
    
    public boolean isFinished()
    {
        return this.isFinished();
    }

}
