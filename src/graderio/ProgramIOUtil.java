package graderio;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import emailgrader.GraderInfo;
import graderobjects.ExecutionResultStatus;
import graderobjects.PostExecutionResults;
import graderobjects.ProgrammingLanguage;

public class ProgramIOUtil
{
    private ExecutableLocator eld;

    public ProgramIOUtil()
    {
        this.eld = new ExecutableLocator();
    }

    public File getExecutableParentFolder(long submissionId)
    {
        File toReturn = new File(GraderInfo.SUBMISSION_UPLOAD_FOLDER, submissionId + "");
        if (!toReturn.getParentFile().exists()
            || (toReturn.getParentFile().exists() && !toReturn.getParentFile().isDirectory()))
        {
            toReturn.getParentFile().mkdir();
        }
        if (!toReturn.exists() || (!toReturn.isDirectory() && toReturn.exists()))
        {
            toReturn.mkdir();
        }
        return toReturn;
    }

    public PostExecutionResults compileProgram(String submissionId, File toCompile, ProgrammingLanguage language,
        long timeoutMs) throws InterruptedException
    {
        if (!toCompile.getParentFile().exists())
        {
            toCompile.getParentFile().mkdir();
        }
        ArrayList<String> commandArgs = new ArrayList<String>();
        String compiledFileName = null;

        switch (language)
        {
            case C:
                compiledFileName = toCompile.getParent() + "/toExecute";
                commandArgs.add(eld.getGCC());
                commandArgs.add(toCompile.getAbsolutePath());
                commandArgs.add("-lm");
                commandArgs.add("-O2");
                commandArgs.add("-o");
                commandArgs.add(compiledFileName);
                break;

            case C_PLUS_PLUS:
                compiledFileName = toCompile.getParent() + "/toExecute";
                commandArgs.add(eld.getGPP());
                commandArgs.add(toCompile.getAbsolutePath());
                commandArgs.add("--std=c++11");
                commandArgs.add("-lm");
                commandArgs.add("-O2");
                commandArgs.add("-o");
                commandArgs.add(compiledFileName);
                break;

            case JAVA:
                compiledFileName = toCompile.getName().replace(".java", "");
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
                compiledFileName = "toExecute.exe";
                commandArgs.add(eld.getMCS());
                commandArgs.add("-out:toExecute.exe");
                commandArgs.add("-pkg:dotnet");
                commandArgs.add(toCompile.getName());
                break;

            case PYTHON_2:
            case PYTHON_3:
                compiledFileName = toCompile.getName();
                PostExecutionResults toReturn = new PostExecutionResults(null, null, null, 0.0, ExecutionResultStatus.SUCCESS);
                toReturn.miscInfo.put("compiledFileName", compiledFileName);
                return toReturn;
                
            default:
                return new PostExecutionResults(null, null, null, 0.0, ExecutionResultStatus.SUCCESS);
        }
        PostExecutionResults toReturn = runExecutable(null, commandArgs, null, submissionId + "_compile", toCompile.getParentFile(), timeoutMs);
        toReturn.miscInfo.put("compiledFileName", compiledFileName);
        return toReturn;
    }

    public PostExecutionResults runProgram(String submissionId, File toRun, String stdin, ProgrammingLanguage language,
        long timeoutMs) throws InterruptedException
    {
        if (!toRun.getParentFile().exists())
        {
            toRun.getParentFile().mkdir();
        }

        ArrayList<String> commandArgs = new ArrayList<String>();

        switch (language)
        {
            case JAVA:
                commandArgs.add(eld.getJava());
                commandArgs.add("-cp");
                commandArgs.add(toRun.getParent());
                commandArgs.add(toRun.getName().replaceAll(".class", ""));
                break;

            case C_SHARP:
                commandArgs.add(eld.getCSharpRunner());
                commandArgs.add(toRun.toString());
                break;

            case PYTHON_2:
                commandArgs.add(eld.getPython2());
                commandArgs.add(toRun.toString());
                break;

            case PYTHON_3:
                commandArgs.add(eld.getPython3());
                commandArgs.add(toRun.toString());
                break;

            case C:
            case C_PLUS_PLUS:
            default:
                commandArgs.add(toRun.toString());
                break;
        }

        return runExecutable(null, commandArgs, stdin, submissionId + "_execute", toRun.getParentFile(), timeoutMs);
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
