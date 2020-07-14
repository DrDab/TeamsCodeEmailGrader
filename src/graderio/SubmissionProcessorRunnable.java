package graderio;

import java.sql.SQLException;

import graderobjects.ContestSubmission;
import graderobjects.SubmissionState;

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
            //System.out.println("Run block triggered");
            while (!submissionProcessor.isStopped())
            {
                //System.out.println("looping");
                while (sqlUtil.hasPendingSubmission())
                {
                    ContestSubmission cur = sqlUtil.getNextPendingSubmission();
                    System.out.println("checking pending sub " + cur.uid);
                    // only thing we read from header is language and problem number.
                    // we will read the division and team name from x-referencing the sender email.
                    /*
                    String sender = cur.senderEmail;
                    ContestTeam senderTeam = this.sqlUtil.getTeamFromEmail(sender);
                    if (senderTeam == null)
                    {
                        cur.state = SubmissionState.PROCESSED_REJECTED;
                        cur.miscInfo = "UNKNOWN EMAIL";
                        // TODO: add reply that the email is unknown.
                        continue;
                    }
                    */
                    cur.state = SubmissionState.PROCESSED_REJECTED;
                    sqlUtil.updateSubmissionStatus(cur);
                }
                Thread.sleep(1000L);
            }
        }
        catch (SQLException | InterruptedException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        //System.out.println("finished");
        this.finished = true;
    }
    
    public boolean isFinished()
    {
        return this.finished;
    }

}
