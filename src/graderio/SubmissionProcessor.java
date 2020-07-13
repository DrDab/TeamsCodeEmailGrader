package graderio;

public class SubmissionProcessor
{
    private SQLUtil sqlUtil;
    private Thread processorThread;
    private SubmissionProcessorRunnable submissionProcessorRunnable;
    private boolean stop;

    public SubmissionProcessor(SQLUtil sqlUtil)
    {
        this.sqlUtil = sqlUtil;
        this.stop = true;
        this.submissionProcessorRunnable = new SubmissionProcessorRunnable(this, this.sqlUtil);
        this.processorThread = new Thread(this.submissionProcessorRunnable);
    }

    public void startProcessor()
    {
        if (this.submissionProcessorRunnable.isFinished())
        {
            this.submissionProcessorRunnable = new SubmissionProcessorRunnable(this, this.sqlUtil);
            this.processorThread = new Thread(this.submissionProcessorRunnable);
        }
        this.stop = false;
        this.processorThread.start();
    }

    public void stopProcessor()
    {
        while (!this.submissionProcessorRunnable.isFinished())
        {
            // wait until submission processor runnable finished before finishing method.
        }
        this.stop = true;
    }
    
    public boolean isStopped()
    {
        return this.stop;
    }
}
