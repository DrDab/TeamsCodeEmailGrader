package graderio;

public class SubmissionProcessor
{
    private SQLUtil sqlUtil;
    private Thread processorThread;
    private EmailUtil emailUtil;
    private SubmissionProcessorRunnable submissionProcessorRunnable;
    private boolean stop;

    public SubmissionProcessor(SQLUtil sqlUtil, EmailUtil emailUtil)
    {
        this.sqlUtil = sqlUtil;
        this.emailUtil = emailUtil;
        this.stop = true;
        this.submissionProcessorRunnable = new SubmissionProcessorRunnable(this, this.sqlUtil, this.emailUtil);
        this.processorThread = new Thread(this.submissionProcessorRunnable);
    }

    public void startProcessor()
    {
        if (this.submissionProcessorRunnable.isFinished())
        {
            this.submissionProcessorRunnable = new SubmissionProcessorRunnable(this, this.sqlUtil, this.emailUtil);
            this.processorThread = new Thread(this.submissionProcessorRunnable);
        }
        this.stop = false;
        this.processorThread.start();
    }

    public void stopProcessor()
    {
        this.stop = true;
        while (!this.submissionProcessorRunnable.isFinished())
        {
            // wait until submission processor runnable finished before finishing method.
        }
    }
    
    public boolean isStopped()
    {
        return !this.stop;
    }
}
