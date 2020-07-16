package graderio;

import java.sql.SQLException;

import graderobjects.ContestSubmission;
import graderobjects.ContestTeam;
import graderobjects.ProblemBundle;
import graderobjects.SubmissionState;

@SuppressWarnings("unused")
public class SubmissionProcessorRunnable implements Runnable
{
    private SubmissionProcessor submissionProcessor;
    private SQLUtil sqlUtil;
    private EmailUtil emailUtil;
    private boolean finished;
    private long queryRate;

    public SubmissionProcessorRunnable(SubmissionProcessor submissionProcessor, SQLUtil sqlUtil, EmailUtil emailUtil,
        long queryRate)
    {
        this.submissionProcessor = submissionProcessor;
        this.sqlUtil = sqlUtil;
        this.emailUtil = emailUtil;
        this.finished = false;
        this.queryRate = queryRate;
    }
    
    private boolean validateSubject(String subject)
    {
        if (subject == null)
        {
            return false;
        }
        subject = subject.trim();
        if (subject.length() == 0)
        {
            return false;
        }
        return true;
    }
    
    private boolean validateProblemBundle(ProblemBundle pb)
    {
        if (pb == null)
        {
            return false;
        }
        if (pb.problemDifficulty == null || pb.problemNumRelative == -1)
        {
            return false;
        }
        
        return true;
    }

    @Override
    public void run()
    {
        try
        {
            // System.out.println("Run block triggered");
            while (!this.submissionProcessor.isStopped())
            {
                // System.out.println("looping");
                while (this.sqlUtil.hasPendingSubmission())
                {
                    ContestSubmission cur = sqlUtil.getNextPendingSubmission();
                    //System.out.println("checking pending sub " + cur.uid);
                    // only thing we read from header is language and problem
                    // number.
                    // we will read the division and team name from
                    // x-referencing the sender email.

                    String sender = cur.senderEmail;
                    ContestTeam senderTeam = this.sqlUtil.getTeamFromEmail(sender);
                    if (senderTeam == null)
                    {
                        cur.state = SubmissionState.PROCESSED_REJECTED;
                        cur.miscInfo = "UNKNOWN EMAIL";
                        // TODO: add reply that the email is unknown.

                        this.sqlUtil.updateSubmissionStatus(cur);
                        continue;
                    }

                    String subject = cur.subject;
                    
                    if (!validateSubject(subject))
                    {
                        cur.state = SubmissionState.PROCESSED_REJECTED;
                        cur.miscInfo = "SUBJECT MISSING";
                        // TODO: add reply that the subject is missing.
                        
                        this.sqlUtil.updateSubmissionStatus(cur);
                        continue;
                    }
                    
                    String[] subjectSplit = subject.split(",");
                    ProblemBundle pb = ParserUtil.getProblemBundle(subjectSplit);
                    
                    if (!validateProblemBundle(pb))
                    {
                        cur.state = SubmissionState.PROCESSED_REJECTED;
                        cur.miscInfo = "SUBJECT FORMAT INCORRECT";
                        // TODO: add reply that the subject is incorrectly formatted.
                        
                        this.sqlUtil.updateSubmissionStatus(cur);
                        continue;
                    }
                    
                    
                }
                Thread.sleep(this.queryRate);
            }
        }
        catch (SQLException | InterruptedException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // System.out.println("finished");
        this.finished = true;
    }

    public boolean isFinished()
    {
        return this.finished;
    }

}
