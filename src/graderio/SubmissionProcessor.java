package graderio;

import sheetsintegration.SheetsInteractor;

public class SubmissionProcessor
{
    private SQLUtil sqlUtil;
    private Thread processorThread;
    private EmailUtil emailUtil;
    private ProgramIOUtil programIOUtil;
    private SheetsInteractor sheetsInteractor;
    private SubmissionProcessorRunnable submissionProcessorRunnable;
    private boolean stop;
    private long queryRate;
    private int numThreadsMax;

    public SubmissionProcessor(SQLUtil sqlUtil, EmailUtil emailUtil, ProgramIOUtil programIOUtil,
        SheetsInteractor sheetsInteractor, long queryRate, int numThreadsMax)
    {
        this.sqlUtil = sqlUtil;
        this.emailUtil = emailUtil;
        this.programIOUtil = programIOUtil;
        this.sheetsInteractor = sheetsInteractor;
        this.stop = true;
        this.queryRate = queryRate;
        this.numThreadsMax = numThreadsMax;
        this.submissionProcessorRunnable = new SubmissionProcessorRunnable(this, this.sqlUtil, this.emailUtil,
            this.programIOUtil, this.sheetsInteractor, this.queryRate, this.numThreadsMax);
        this.processorThread = new Thread(this.submissionProcessorRunnable);
    }

    public void startProcessor()
    {
        if (this.submissionProcessorRunnable.isFinished())
        {
            this.submissionProcessorRunnable = new SubmissionProcessorRunnable(this, this.sqlUtil, this.emailUtil,
                this.programIOUtil, this.sheetsInteractor, this.queryRate, this.numThreadsMax);
            this.processorThread = new Thread(this.submissionProcessorRunnable);
        }
        this.stop = false;
        this.processorThread.start();
        // System.out.println("processor started");
    }

    public void stopProcessor()
    {
        this.stop = true;
        while (!this.submissionProcessorRunnable.isFinished())
        {
            // wait until submission processor runnable finished before
            // finishing method.
        }
    }

    public boolean isStopped()
    {
        return this.stop;
    }
}
