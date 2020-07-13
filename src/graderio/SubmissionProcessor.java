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
        this.stop = false;
        this.processorThread.start();
    }

    public void stopProcesor()
    {
        this.stop = true;
    }
    
    public boolean isStopped()
    {
        return this.stop;
    }
}
