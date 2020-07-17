package graderio;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;

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

        return true;
    }

    private void setSubmissionInvalid(ContestSubmission sub, String miscInfo)
    {
        sub.state = SubmissionState.PROCESSED_REJECTED;
        sub.miscInfo = miscInfo;
    }
    
    private PostExecutionResults runTestCase(int testCaseNum, String submissionName, File compiledFile, String stdin, ProgrammingLanguage programmingLanguage, long timeoutMs) throws InterruptedException
    {
        PostExecutionResults runResults = this.programIOUtil.runProgram(submissionName + "_" + testCaseNum, compiledFile, stdin, programmingLanguage, timeoutMs);
        return runResults;
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
                    // System.out.println("checking pending sub " + cur.uid);
                    // only thing we read from header is language and problem
                    // number.
                    // we will read the division and team name from
                    // x-referencing the sender email.

                    String sender = cur.senderEmail;
                    ContestTeam senderTeam = this.sqlUtil.getTeamFromEmail(sender);
                    if (senderTeam == null)
                    {
                        setSubmissionInvalid(cur, "INVALID_EMAIL_ACCESS_DENIED");
                        // TODO: add reply that the email is unknown.

                        this.sqlUtil.updateSubmissionStatus(cur);
                        continue;
                    }

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

                    if (programmingLanguage == ProgrammingLanguage.JAVA)
                    {
                        String className = ParserUtil.getMainJavaClassName(code);
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
                    PostExecutionResults compileResults = this.programIOUtil.compileProgram(submissionName,
                        codeFile, programmingLanguage, 3000L);

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
                    
                    int score = 0;
                    for (int i = 1; i <= 3; i++)
                    {
                        
                        //PostExecutionResults testCaseResults = this.runTestCase(i, submissionName, compiledFile, stdin, programmingLanguage, 3000L);
                        // if win, score++
                    }
                    
                    
                }
                Thread.sleep(this.queryRate);
            }
        }
        catch (SQLException | InterruptedException | IOException e)
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
