package graderio;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import emailgrader.GraderInfo;
import graderobjects.ExecutionResultStatus;
import graderobjects.PostExecutionResults;
import graderobjects.ProgrammingLanguage;

public class ProgramIOUtil
{
    private ExecutableLocator eld;

    public ProgramIOUtil(GraderInfo graderInfo)
    {
        this.eld = new ExecutableLocator(graderInfo);
    }

    public PostExecutionResults compileProgram(String submissionId, File toCompile, ProgrammingLanguage language,
        long timeoutMs) throws InterruptedException
    {
        if (!toCompile.getParentFile().exists())
        {
            toCompile.getParentFile().mkdir();
        }
        ArrayList<String> commandArgs = new ArrayList<String>();

        switch (language)
        {
            case C:
                commandArgs.add(eld.getGCC());
                commandArgs.add(toCompile.getAbsolutePath());
                commandArgs.add("-lm");
                commandArgs.add("-O2");
                commandArgs.add("-o");
                commandArgs.add(toCompile.getParent() + "/toExecute");
                break;

            case C_PLUS_PLUS:
                commandArgs.add(eld.getGPP());
                commandArgs.add(toCompile.getAbsolutePath());
                commandArgs.add("--std=c++11");
                commandArgs.add("-lm");
                commandArgs.add("-O2");
                commandArgs.add("-o");
                commandArgs.add(toCompile.getParent() + "/toExecute");
                break;

            case JAVA:
                commandArgs.add(eld.getJavac());
                commandArgs.add("-source");
                commandArgs.add("1.8");
                commandArgs.add("-target");
                commandArgs.add("1.8");
                commandArgs.add(toCompile.getAbsolutePath());
                commandArgs.add("-d");
                commandArgs.add(toCompile.getParent());
                break;

            case C_SHARP:
                commandArgs.add(eld.getMCS());
                commandArgs.add("-out:toExecute.exe");
                commandArgs.add("-pkg:dotnet");
                commandArgs.add(toCompile.getName());
                break;

            case PYTHON_2:
            case PYTHON_3:
            default:
                return new PostExecutionResults(null, null, null, 0.0, ExecutionResultStatus.SUCCESS);
        }

        return runExecutable(null, commandArgs, null, submissionId, toCompile.getParentFile(), timeoutMs);
    }

    public PostExecutionResults runExecutable(File toRun, List<String> args, String stdin, String logName,
        File logFileDirectory, long timeoutMs) throws InterruptedException
    {
        List<String> pbArgs = new ArrayList<>();

        if (toRun != null)
        {
            pbArgs.add(toRun.toString());
        }

        if (args != null)
        {
            for (String arg : args)
            {
                pbArgs.add(arg);
            }
        }

        File programOutputFile = logFileDirectory == null ? new File(logName + "-program-output")
            : new File(logFileDirectory, logName + "-program-output");
        File programErrFile = logFileDirectory == null ? new File(logName + "-program-error")
            : new File(logFileDirectory, logName + "-program-error");

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
                return new PostExecutionResults(stdin, stdout, stderr, timeTaken,
                    stderr.trim().length() == 0 ? ExecutionResultStatus.SUCCESS
                        : ExecutionResultStatus.FAILED_PROGRAM_ERR);
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

    public PostExecutionResults runExecutable(File toRun, List<String> args, String stdin, String logName,
        long timeoutMs) throws InterruptedException
    {
        return runExecutable(toRun, args, stdin, logName, toRun.getParentFile(), timeoutMs);
    }

}
