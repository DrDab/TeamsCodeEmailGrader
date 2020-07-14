package graderio;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import graderobjects.ExecutionResultStatus;
import graderobjects.PostExecutionResults;

public class ProgramIOUtil
{
    private ExecutableLocator eld;

    public ProgramIOUtil()
    {
        this.eld = new ExecutableLocator();
    }

    public PostExecutionResults runExecutable(File toRun, List<String> args, String stdin, String logName,
        long timeoutMs) throws InterruptedException
    {
        System.out.println(toRun);
        List<String> pbArgs = new ArrayList<>();
        pbArgs.add(toRun.toString());
        if (args != null)
        {
            for (String arg : args)
            {
                pbArgs.add(arg);
            }
        }
        File programOutputFile = toRun.getParent() == null ? new File(logName + "-program-output")
            : new File(toRun.getParent(), logName + "-program-output");
        File programErrFile = toRun.getParent() == null ? new File(logName + "-program-error")
            : new File(toRun.getParent(), logName + "-program-error");
        try
        {
            ArrayList<Integer> al = new ArrayList<Integer>();
            ProcessBuilder pb = new ProcessBuilder(pbArgs);
            // pb.directory(new File(toRun.getParent()));
            pb.redirectOutput(programOutputFile);
            pb.redirectError(programErrFile);
            Process runProcess = pb.start();

            if (stdin != null)
            {
                OutputStream stdInStream = runProcess.getOutputStream();
                BufferedWriter inputWriter = new BufferedWriter(new OutputStreamWriter(stdInStream));
                inputWriter.write(stdin);
                inputWriter.close();
            }

            double start = System.nanoTime();
            new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        Thread.sleep((long) timeoutMs);
                        if (runProcess.isAlive())
                        {
                            runProcess.destroy();
                            al.add(1);
                        }
                    }
                    catch (InterruptedException e)
                    {
                        e.printStackTrace();
                    }
                }

            }).start();
            runProcess.waitFor();

            double timeTaken = (System.nanoTime() - start) / 1000000.0;
            byte[] stdoutarr = Files.readAllBytes(programOutputFile.toPath());
            byte[] stderrarr = Files.readAllBytes(programErrFile.toPath());
            String stdout = new String(stdoutarr);
            String stderr = new String(stderrarr);

            if (al.size() == 0)
            {
                return new PostExecutionResults(stdin, stdout, stderr, timeTaken, ExecutionResultStatus.SUCCESS);
            }
            else
            {
                return new PostExecutionResults(stdin, stdout, stderr, timeTaken,
                    ExecutionResultStatus.FAILED_TIMED_OUT);
            }
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return new PostExecutionResults(e.getMessage(), e.getMessage(), e.getMessage(), 0.0,
                ExecutionResultStatus.FAILED_COMMAND_ERR);
        }
    }

}
