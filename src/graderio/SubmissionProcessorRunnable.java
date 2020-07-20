package graderio;

import java.io.File;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Store;

import org.json.JSONObject;

import graderobjects.ContestSubmission;
import graderobjects.InvalidationReason;
import graderobjects.PostExecutionResults;
import graderobjects.ProblemBundle;
import graderobjects.ProgrammingLanguage;
import graderobjects.SubmissionState;
import sheetsintegration.SheetsInteractor;

@SuppressWarnings("unused")
public class SubmissionProcessorRunnable implements Runnable
{
    private SubmissionProcessor submissionProcessor;
    SQLUtil sqlUtil;
    private EmailUtil emailUtil;
    ProgramIOUtil programIOUtil;
    SheetsInteractor sheetsInteractor;
    private boolean finished;
    private long queryRate;
    int numThreads;
    private int numThreadsMax;

    public SubmissionProcessorRunnable(SubmissionProcessor submissionProcessor, SQLUtil sqlUtil, EmailUtil emailUtil,
        ProgramIOUtil programIOUtil, SheetsInteractor sheetsInteractor, long queryRate, int numThreadsMax)
    {
        this.submissionProcessor = submissionProcessor;
        this.sqlUtil = sqlUtil;
        this.emailUtil = emailUtil;
        this.programIOUtil = programIOUtil;
        this.sheetsInteractor = sheetsInteractor;
        this.finished = false;
        this.queryRate = queryRate;
        this.numThreadsMax = numThreadsMax;
        this.numThreads = 0;
    }

    boolean validateSubject(String subject)
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

    boolean validateProblemBundle(ProblemBundle pb)
    {
        if (pb == null)
        {
            return false;
        }
        if (pb.problemDifficulty == null || pb.problemNumRelative == -1)
        {
            return false;
        }
        if (pb.getAbsoluteId() < 1 || pb.getAbsoluteId() > 15)
        {
            return false;
        }

        return true;
    }

    void setSubmissionInvalid(ContestSubmission sub, InvalidationReason invalidationReason)
    {
        sub.state = SubmissionState.PROCESSED_REJECTED;
        JSONObject obj = new JSONObject();
        obj.put("invalidationReason", invalidationReason.toString());
        sub.miscInfo = obj.toString();
    }

    PostExecutionResults runTestCase(int testCaseNum, String submissionName, File compiledFile, String stdin,
        ProgrammingLanguage programmingLanguage, long timeoutMs) throws InterruptedException
    {
        PostExecutionResults runResults = this.programIOUtil.runProgram(submissionName + "_" + testCaseNum,
            compiledFile, stdin, programmingLanguage, timeoutMs);
        return runResults;
    }

    String trimRight(String toTrim)
    {
        return toTrim.replaceAll("\\s+$", "");
    }

    void sendEmailReply(long emailUid, String replyContent) throws MessagingException
    {
        Store store = this.emailUtil.getStore();
        Message replyTo = this.emailUtil.getMessageByUid(emailUid, store);
        this.emailUtil.replyToMessage(replyTo, replyContent);
        store.close();
    }

    @Override
    public void run()
    {
        while (!this.submissionProcessor.isStopped())
        {
            try
            {
                // System.out.println("looping");
                while (this.sqlUtil.hasPendingSubmission())
                {
                    ContestSubmission cur = sqlUtil.getNextPendingSubmission();
                    cur.state = SubmissionState.CURRENTLY_PROCESSING;
                    this.sqlUtil.updateSubmissionStatus(cur);
                    spawnChildJudgingThread(cur);
                }
                Thread.sleep(this.queryRate);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        // System.out.println("finished");
        this.finished = true;
    }

    public boolean isFinished()
    {
        return this.finished;
    }

    // alternative name: spawnAsianParentingThread
    private void spawnChildJudgingThread(ContestSubmission contestSubmission)
    {
        // are we at our thread limit?
        // if so, wait until the thread count is less than the thread limit.
        // otherwise, go on and spawn a thread.
        while (this.numThreads >= this.numThreadsMax)
        { // wait until thread count is less than thread limit before going on.
        }
        
        SubmissionProcessorRunnableChildRunnable childRunnable = new SubmissionProcessorRunnableChildRunnable(this,
            contestSubmission);
        new Thread(childRunnable).start();
    }

}
