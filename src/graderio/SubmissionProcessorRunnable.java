package graderio;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;

import javax.mail.internet.InternetAddress;

import emailgrader.GraderInfo;
import graderobjects.ContestProblem;
import graderobjects.ContestSubmission;
import graderobjects.ContestTeam;
import graderobjects.ExecutionResultStatus;
import graderobjects.PostExecutionResults;
import graderobjects.ProblemBundle;
import graderobjects.ProgrammingLanguage;
import graderobjects.SubmissionState;

@SuppressWarnings("unused")
public class SubmissionProcessorRunnable implements Runnable
{
    private SubmissionProcessor submissionProcessor;
    private SQLUtil sqlUtil;
    private EmailUtil emailUtil;
    private ProgramIOUtil programIOUtil;
    private boolean finished;
    private long queryRate;

    public SubmissionProcessorRunnable(SubmissionProcessor submissionProcessor, SQLUtil sqlUtil, EmailUtil emailUtil,
        ProgramIOUtil programIOUtil, long queryRate)
    {
        this.submissionProcessor = submissionProcessor;
        this.sqlUtil = sqlUtil;
        this.emailUtil = emailUtil;
        this.programIOUtil = programIOUtil;
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
        if (pb.getAbsoluteId() < 1 || pb.getAbsoluteId() > 15)
        {
            return false;
        }

        return true;
    }

    private void setSubmissionInvalid(ContestSubmission sub, String miscInfo)
    {
        sub.state = SubmissionState.PROCESSED_REJECTED;
        sub.miscInfo = miscInfo;
    }

    private PostExecutionResults runTestCase(int testCaseNum, String submissionName, File compiledFile, String stdin,
        ProgrammingLanguage programmingLanguage, long timeoutMs) throws InterruptedException
    {
        PostExecutionResults runResults = this.programIOUtil.runProgram(submissionName + "_" + testCaseNum,
            compiledFile, stdin, programmingLanguage, timeoutMs);
        return runResults;
    }

    private String trimRight(String toTrim)
    {
        return toTrim.replaceAll("\\s+$", "");
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
                    // System.out.println("checking pending sub " + cur.uid);
                    // only thing we read from header is language and problem
                    // number.
                    // we will read the division and team name from
                    // x-referencing the sender email.

                    String senderEmailStr = cur.senderEmail;
                    InternetAddress address = new InternetAddress(senderEmailStr);
                    String senderEmail = address.getAddress();

                    ContestTeam senderTeam = this.sqlUtil.getTeamFromEmail(senderEmail);
                    if (senderTeam == null)
                    {
                        setSubmissionInvalid(cur, "INVALID_EMAIL_ACCESS_DENIED");
                        // TODO: add reply that the email is unknown.

                        this.sqlUtil.updateSubmissionStatus(cur);
                        continue;
                    }

                    cur.teamName = senderTeam.teamName;
                    cur.contestDivision = senderTeam.contestDivision;

                    String subject = cur.subject;

                    if (!validateSubject(subject))
                    {
                        setSubmissionInvalid(cur, "SUBJECT_MISSING");
                        // TODO: add reply that the subject is missing.

                        this.sqlUtil.updateSubmissionStatus(cur);
                        continue;
                    }

                    String[] subjectSplit = subject.split(",");
                    ProblemBundle pb = ParserUtil.getProblemBundle(subjectSplit);
                    ProgrammingLanguage programmingLanguage = ParserUtil.getProgrammingLanguage(subjectSplit);

                    if (!validateProblemBundle(pb) || programmingLanguage == null)
                    {
                        setSubmissionInvalid(cur, "SUBJECT_INVALID");
                        // TODO: add reply that the subject is incorrectly
                        // formatted.

                        this.sqlUtil.updateSubmissionStatus(cur);
                        continue;
                    }

                    cur.programmingLanguage = programmingLanguage;
                    cur.problemDifficulty = pb.problemDifficulty;
                    cur.problemIdAbsolute = pb.getAbsoluteId();

                    if (this.sqlUtil.getSubmissionCountPerProblemPerTeam(cur.teamName,
                        cur.problemIdAbsolute) >= GraderInfo.MAXIMUM_SUBMISSION_COUNT
                        && cur.state != SubmissionState.AWAITING_PROCESSING_OVERRIDE_ATTEMPT_LIMITS)
                    {
                        setSubmissionInvalid(cur, "SUBMISSION_COUNT_EXCEEDED");
                        // TODO: add reply that submission limit for the problem
                        // has been exceeded.

                        this.sqlUtil.updateSubmissionStatus(cur);
                        continue;
                    }

                    if (programmingLanguage == ProgrammingLanguage.OTHER)
                    {
                        setSubmissionInvalid(cur, "FOREIGN_PROGRAMMING_LANG");
                        // TODO: add reply that specified language is
                        // unrecognized.

                        this.sqlUtil.updateSubmissionStatus(cur);
                        continue;
                    }

                    ContestProblem problem = this.sqlUtil.getProblemById(pb.getAbsoluteId(),
                        senderTeam.contestDivision);

                    File submissionDir = this.programIOUtil.getExecutableParentFolder(cur.id);
                    String code = cur.body;
                    String submissionName = "Upload_" + cur.id;
                    String fileName = null;
                    String compiledFileName = null;

                    if (cur.attachmentData != null)
                    {
                        for (String key : cur.attachmentData.keySet())
                        {
                            boolean attachmentTargeted = false;
                            switch (programmingLanguage)
                            {
                                case JAVA:
                                    attachmentTargeted = key.contains(".java");
                                    break;

                                case C_PLUS_PLUS:
                                    attachmentTargeted = key.contains(".cpp");
                                    break;

                                case C:
                                    attachmentTargeted = key.contains(".c");
                                    break;

                                case PYTHON_2:
                                case PYTHON_3:
                                    attachmentTargeted = key.contains(".py");
                                    break;

                                case C_SHARP:
                                    attachmentTargeted = key.contains(".cs");
                                    break;

                                // we should never reach here anyway, but if so,
                                // don't assign an extension.
                                case OTHER:
                                default:
                                    break;
                            }

                            if (attachmentTargeted)
                            {
                                Byte[] codeBytes = cur.attachmentData.get(key);
                                // turn codeBytes into a primitive, before
                                // initializing a string on it.
                                byte[] codeBytesPrimitive = new byte[codeBytes.length];
                                for (int i = 0; i < codeBytes.length; i++)
                                {
                                    codeBytesPrimitive[i] = (byte) codeBytes[i];
                                }
                                code = new String(codeBytesPrimitive);
                                break;
                            }
                        }
                    }

                    if (programmingLanguage == ProgrammingLanguage.JAVA)
                    {
                        String className = ParserUtil.getMainJavaClassName(code);
                        if (className == null)
                        {
                            setSubmissionInvalid(cur, "INVALID_JAVA_CLASS_NAME");
                            // TODO: add reply that java class name is
                            // unrecognized.

                            this.sqlUtil.updateSubmissionStatus(cur);
                            continue;
                        }
                        fileName = className + ".java";
                    }
                    else
                    {
                        fileName = String.valueOf(submissionName);
                        switch (programmingLanguage)
                        {
                            case C_PLUS_PLUS:
                                fileName += ".cpp";
                                break;

                            case C:
                                fileName += ".c";
                                break;

                            case PYTHON_2:
                            case PYTHON_3:
                                fileName += ".py";
                                break;

                            case C_SHARP:
                                fileName += ".cs";
                                break;

                            // we should never reach here anyway, but if so,
                            // don't assign an extension.
                            case OTHER:
                            default:
                                break;
                        }
                    }

                    if (fileName == null)
                    {
                        setSubmissionInvalid(cur, "INVALID_SRC_FILE_NAME");
                        // TODO: add reply that source file name is invalid, and
                        // to
                        // declare the method with main method as public class
                        // if using Java.

                        this.sqlUtil.updateSubmissionStatus(cur);
                        continue;
                    }

                    // write the code to a file.
                    File codeFile = new File(submissionDir, fileName);
                    PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(codeFile)));
                    pw.println(code);
                    pw.close();

                    // now that everything is validated, compile the code and
                    // test it.
                    PostExecutionResults compileResults = this.programIOUtil.compileProgram(submissionName, codeFile,
                        programmingLanguage, GraderInfo.COMPILE_TIME_LIMIT);

                    if (compileResults.getExecutionResultStatus() != ExecutionResultStatus.SUCCESS)
                    {
                        setSubmissionInvalid(cur, compileResults.getExecutionResultStatus() + "");
                        // TODO: add reply that compile failed for whatever
                        // reason given.

                        this.sqlUtil.updateSubmissionStatus(cur);
                        continue;
                    }

                    // if we compiled successfully, try running the program.
                    compiledFileName = compileResults.miscInfo.get("compiledFileName");
                    File compiledFile = new File(submissionDir, compiledFileName);

                    System.out.printf("Running submission %d, l=%s, pid=%s, team=%s\n", cur.id, programmingLanguage,
                        problem.name, cur.teamName);

                    int score = 0;
                    for (int i = 0; i < problem.inputOutputArgs.size(); i++)
                    {
                        System.out.printf("Test %d\n", i);
                        String[] ioSet = problem.inputOutputArgs.get(i);
                        String inputFileName = ioSet[0];
                        String outputFileName = ioSet[1];
                        File problemSetFolder = new File(GraderInfo.PROBLEM_SET_FOLDER, problem.name);
                        File inputFile = new File(problemSetFolder, inputFileName);
                        File outputFile = new File(problemSetFolder, outputFileName);
                        // read stdin and expected stdout for files.
                        String stdInString = trimRight(new String(Files.readAllBytes(inputFile.toPath())));
                        String stdOutString = trimRight(new String(Files.readAllBytes(outputFile.toPath())));
                        PostExecutionResults testCaseResults = this.runTestCase(i, submissionName, compiledFile,
                            stdInString, programmingLanguage, GraderInfo.EXECUTE_TIME_LIMIT);
                        String progStdOut = trimRight(testCaseResults.getStdOut());
                        String progStdErr = trimRight(testCaseResults.getStdErr());
                        System.out.printf("StdOut for %d: \"%s\"\n", i, progStdOut);
                        if (progStdOut.equals(stdOutString))
                        {
                            score++;
                            continue;
                        }
                        if (progStdErr.length() > 0)
                        {
                            // error case
                            // TODO: set state to processed error, send email
                            // that the program threw an error. do not elaborate
                            // on error (for security purposes). update
                            // submission status and continue.
                        }
                    }

                    System.out.printf("Score for %d: %d\n", cur.id, score);

                    // TODO: send email reply with score.
                    cur.miscInfo = "score=" + score;
                    cur.state = SubmissionState.PROCESSED_GRADED;
                    this.sqlUtil.updateSubmissionStatus(cur);
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

}
