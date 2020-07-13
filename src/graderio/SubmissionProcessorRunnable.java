package graderio;

import java.sql.SQLException;

import graderobjects.ContestSubmission;

public class SubmissionProcessorRunnable implements Runnable
{
    private SubmissionProcessor submissionProcessor;
    private SQLUtil sqlUtil;
    
    public SubmissionProcessorRunnable(SubmissionProcessor submissionProcessor, SQLUtil sqlUtil)
    {
        this.submissionProcessor = submissionProcessor;
        this.sqlUtil = sqlUtil;
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
                    // validate the submission. check the header. (does the team match up? does the division match up? are all fields included?)
                    // if validated, go on.
                    // if not validated, update the state of the submission to PROCESSED_REJECTED, and update miscellaneous information as to why. continue.
                    // after validating submission, grade the submission.
                    // after grading, update the state of the submission to PROCESSED_GRADED and onto the next submission we go
                    
                }
            }
        }
        catch (SQLException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
