package graderio;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Store;
import javax.mail.internet.InternetAddress;

import org.json.JSONObject;

import emailgrader.GraderInfo;
import graderobjects.ContestProblem;
import graderobjects.ContestSubmission;
import graderobjects.ContestTeam;
import graderobjects.ExecutionResultStatus;
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
    private SQLUtil sqlUtil;
    private EmailUtil emailUtil;
    private ProgramIOUtil programIOUtil;
    private SheetsInteractor sheetsInteractor;
    private boolean finished;
    private long queryRate;

    public SubmissionProcessorRunnable(SubmissionProcessor submissionProcessor, SQLUtil sqlUtil, EmailUtil emailUtil,
        ProgramIOUtil programIOUtil, SheetsInteractor sheetsInteractor, long queryRate)
    {
        this.submissionProcessor = submissionProcessor;
        this.sqlUtil = sqlUtil;
        this.emailUtil = emailUtil;
        this.programIOUtil = programIOUtil;
        this.sheetsInteractor = sheetsInteractor;
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

    private void setSubmissionInvalid(ContestSubmission sub, InvalidationReason invalidationReason)
    {
        sub.state = SubmissionState.PROCESSED_REJECTED;
        JSONObject obj = new JSONObject();
        obj.put("invalidationReason", invalidationReason.toString());
        sub.miscInfo = obj.toString();
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

    private void sendEmailReply(long emailUid, String replyContent) throws MessagingException
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
                    // System.out.println("checking pending sub " + cur.uid);
                    this.judgeContestSubmission(cur);
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
    
    private void judgeContestSubmission(ContestSubmission contestSubmission) throws Exception
    {
        JSONObject miscInfoJSONObject = new JSONObject();

        String senderEmailStr = contestSubmission.senderEmail;
        InternetAddress address = new InternetAddress(senderEmailStr);
        String senderEmail = address.getAddress();
        
        if (GraderInfo.ENFORCE_TIME_LIMITS)
        {
            if (contestSubmission.date < GraderInfo.CONTEST_START_DATE || contestSubmission.date > GraderInfo.CONTEST_END_DATE)
            {
                InvalidationReason reason = InvalidationReason.TIME_LIMIT_VIOLATION;
                setSubmissionInvalid(contestSubmission, reason);
                // TODO: add reply that the subject is missing.
                String reply = String.format("Hey %s,\n\n"
                    + "We experienced an error while handling your submission. Reason: %s.\n"
                    + "This either means you submitted your solution before the contest start time, or after the contest end time.\n"
                    + "Your submission has not been graded, nor counted as part of the per-problem submission limit.\n"
                    + "\n\n" + "Best,\n\n" + "TeamsCode Staff\n"
                    + "NOTE: This reply was automatically generated. For technical assistance, please message TeamsCode contest organizers.",
                    contestSubmission.senderEmail, reason);
                sendEmailReply(contestSubmission.uid, reply);
                this.sqlUtil.updateSubmissionStatus(contestSubmission);
                return;
            }
        }

        ContestTeam senderTeam = this.sqlUtil.getTeamFromEmail(senderEmail);
        if (senderTeam == null)
        {
            InvalidationReason reason = InvalidationReason.INVALID_EMAIL_ACCESS_DENIED;
            setSubmissionInvalid(contestSubmission, reason);
            // TODO: add reply that the subject is missing.
            String reply = String.format("Hey %s,\n\n"
                + "We experienced an error while handling your submission. Reason: %s.\n"
                + "To fix this issue, please use the email you signed up for this contest with to submit your code.\n"
                + "Please resolve this issue. Your submission has not been graded, nor counted as part of the per-problem submission limit.\n"
                + "\n\n" + "Best,\n\n" + "TeamsCode Staff\n"
                + "NOTE: This reply was automatically generated. For technical assistance, please message TeamsCode contest organizers.",
                contestSubmission.senderEmail, reason);
            sendEmailReply(contestSubmission.uid, reply);
            this.sqlUtil.updateSubmissionStatus(contestSubmission);
            return;
        }

        contestSubmission.teamName = senderTeam.teamName;
        contestSubmission.contestDivision = senderTeam.contestDivision;

        String subject = contestSubmission.subject;

        if (!validateSubject(subject))
        {
            InvalidationReason reason = InvalidationReason.SUBJECT_MISSING;
            setSubmissionInvalid(contestSubmission, reason);
            String reply = String.format("Hey %s (%s),\n\n"
                + "We experienced an error while handling your submission. Reason: %s.\n"
                + "To fix this issue, please write a valid subject, in the format as follows:.\n"
                + "Division, Team Name, Problem number and difficulty, programming language. (example: Intermediate, Team Foo, Easy #1, Python 2)\n"
                + "Your submission has not been graded, nor counted as part of the per-problem submission limit.\n"
                + "\n\n" + "Best,\n\n" + "TeamsCode Staff\n"
                + "NOTE: This reply was automatically generated. For technical assistance, please message TeamsCode contest organizers.",
                contestSubmission.teamName, contestSubmission.senderEmail, reason);
            sendEmailReply(contestSubmission.uid, reply);
            this.sqlUtil.updateSubmissionStatus(contestSubmission);
            return;
        }

        String[] subjectSplit = subject.split(",");
        ProblemBundle pb = ParserUtil.getProblemBundle(subjectSplit);
        ProgrammingLanguage programmingLanguage = ParserUtil.getProgrammingLanguage(subjectSplit);

        if (!validateProblemBundle(pb) || programmingLanguage == null)
        {
            InvalidationReason reason = InvalidationReason.SUBJECT_INVALID;
            setSubmissionInvalid(contestSubmission, reason);
            String reply = String.format("Hey %s (%s),\n\n"
                + "We experienced an error while handling your submission. Reason: %s.\n"
                + "To fix this issue, please write a valid subject, in the format as follows:.\n"
                + "Division, Team Name, Problem number and difficulty, programming language. (example: Intermediate, Team Foo, Easy #1, Python 2)\n"
                + "Your submission has not been graded, nor counted as part of the per-problem submission limit.\n"
                + "\n\n" + "Best,\n\n" + "TeamsCode Staff\n"
                + "NOTE: This reply was automatically generated. For technical assistance, please message TeamsCode contest organizers.",
                contestSubmission.teamName, contestSubmission.senderEmail, reason);
            sendEmailReply(contestSubmission.uid, reply);
            this.sqlUtil.updateSubmissionStatus(contestSubmission);
            return;
        }

        contestSubmission.programmingLanguage = programmingLanguage;
        contestSubmission.problemDifficulty = pb.problemDifficulty;
        contestSubmission.problemIdAbsolute = pb.getAbsoluteId();

        int submissions = this.sqlUtil.getSubmissionCountPerProblemPerTeam(contestSubmission.teamName,
            contestSubmission.problemIdAbsolute);

        if (submissions >= GraderInfo.MAXIMUM_SUBMISSION_COUNT
            && contestSubmission.state != SubmissionState.AWAITING_PROCESSING_OVERRIDE_ATTEMPT_LIMITS)
        {
            InvalidationReason reason = InvalidationReason.SUBMISSION_COUNT_EXCEEDED;
            setSubmissionInvalid(contestSubmission, reason);
            String reply = String.format("Hey %s (%s),\n\n"
                + "We experienced an error while handling your submission. Reason: %s.\n"
                + "It seems you have exceeded the submission count limit per problem, per team. (the limit is %d submissions/problem/team.)\n"
                + "Your submission has not been graded, nor counted as part of the per-problem submission limit.\n"
                + "\n\n" + "Best,\n\n" + "TeamsCode Staff\n"
                + "NOTE: This reply was automatically generated. For technical assistance, please message TeamsCode contest organizers.",
                contestSubmission.teamName, contestSubmission.senderEmail, reason, GraderInfo.MAXIMUM_SUBMISSION_COUNT);
            sendEmailReply(contestSubmission.uid, reply);
            this.sqlUtil.updateSubmissionStatus(contestSubmission);
            return;
        }

        if (programmingLanguage == ProgrammingLanguage.OTHER)
        {
            InvalidationReason reason = InvalidationReason.FOREIGN_PROGRAMMING_LANG;
            setSubmissionInvalid(contestSubmission, reason);
            String reply = String.format("Hey %s (%s),\n\n"
                + "We experienced an error while handling your submission. Reason: %s.\n"
                + "To fix this issue, please use a permitted language to submit your code. (allowed: C, C++, C#, Java, Python 2, Python 3)\n"
                + "Your submission has not been graded, nor counted as part of the per-problem submission limit.\n"
                + "\n\n" + "Best,\n\n" + "TeamsCode Staff\n"
                + "NOTE: This reply was automatically generated. For technical assistance, please message TeamsCode contest organizers.",
                contestSubmission.teamName, contestSubmission.senderEmail, reason);
            sendEmailReply(contestSubmission.uid, reply);
            this.sqlUtil.updateSubmissionStatus(contestSubmission);
            return;
        }

        ContestProblem problem = this.sqlUtil.getProblemById(pb.getAbsoluteId(),
            senderTeam.contestDivision);

        File submissionDir = this.programIOUtil.getExecutableParentFolder(contestSubmission.id);
        String code = contestSubmission.body;
        String submissionName = "Upload_" + contestSubmission.id;
        String fileName = null;
        String compiledFileName = null;

        if (contestSubmission.attachmentData != null)
        {
            for (String key : contestSubmission.attachmentData.keySet())
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
                    Byte[] codeBytes = contestSubmission.attachmentData.get(key);
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
                InvalidationReason reason = InvalidationReason.INVALID_JAVA_CLASS_NAME;
                setSubmissionInvalid(contestSubmission, reason);
                String reply = String.format("Hey %s (%s),\n\n"
                    + "We experienced an error while handling your submission. Reason: %s.\n"
                    + "To fix this issue, please include a valid class name for the public class in your Java code.\n"
                    + "Your submission has not been graded, nor counted as part of the per-problem submission limit.\n"
                    + "\n\n" + "Best,\n\n" + "TeamsCode Staff\n"
                    + "NOTE: This reply was automatically generated. For technical assistance, please message TeamsCode contest organizers.",
                    contestSubmission.teamName, contestSubmission.senderEmail, reason);
                sendEmailReply(contestSubmission.uid, reply);
                this.sqlUtil.updateSubmissionStatus(contestSubmission);
                return;
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
            InvalidationReason reason = InvalidationReason.INVALID_SRC_FILE_NAME;
            setSubmissionInvalid(contestSubmission, reason);
            String reply = String.format("Hey %s (%s),\n\n"
                + "We experienced an error while handling your submission. Reason: %s.\n"
                + "Please fix this issue and resubmit your code.\n"
                + "Your submission has not been graded, nor counted as part of the per-problem submission limit.\n"
                + "\n\n" + "Best,\n\n" + "TeamsCode Staff\n"
                + "NOTE: This reply was automatically generated. For technical assistance, please message TeamsCode contest organizers.",
                contestSubmission.teamName, contestSubmission.senderEmail, reason);
            sendEmailReply(contestSubmission.uid, reply);
            this.sqlUtil.updateSubmissionStatus(contestSubmission);
            return;
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

        // if we compiled successfully, try running the program.
        compiledFileName = compileResults.miscInfo.get("compiledFileName");
        File compiledFile = new File(submissionDir, compiledFileName);

        if (compiledFile.exists() && (programmingLanguage == ProgrammingLanguage.C_PLUS_PLUS
            || programmingLanguage == ProgrammingLanguage.C))
        {
            compileResults.executionResultStatus = ExecutionResultStatus.SUCCESS;
        }

        if (compileResults.executionResultStatus != ExecutionResultStatus.SUCCESS)
        {
            InvalidationReason reason = InvalidationReason.COMPILE_ERROR;
            setSubmissionInvalid(contestSubmission, reason);
            String reply = String.format("Hey %s (%s),\n\n"
                + "We experienced an error while handling your submission. Reason: %s.\n"
                + "Please fix this issue and resubmit your code.\n"
                + "Your submission has not been graded, nor counted as part of the per-problem submission limit.\n"
                + "\n\n" + "Best,\n\n" + "TeamsCode Staff\n"
                + "NOTE: This reply was automatically generated. For technical assistance, please message TeamsCode contest organizers.",
                contestSubmission.teamName, contestSubmission.senderEmail, reason);
            sendEmailReply(contestSubmission.uid, reply);
            this.sqlUtil.updateSubmissionStatus(contestSubmission);
            return;
        }

        // System.out.println(compiledFile);

        System.out.printf("Running submission %d, l=%s, pid=%s, team=%s\n", contestSubmission.id, programmingLanguage,
            problem.name, contestSubmission.teamName);

        int score = 0;
        int errorCases = 0;

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
            System.out.printf("StdErr for %d: \"%s\"\n", i, progStdErr);
            if (progStdOut.equals(stdOutString))
            {
                score++;
                continue;
            }
            if (progStdErr.length() > 0)
            {
                errorCases++;
            }
        }

        System.out.printf("Score for %d: %d\n", contestSubmission.id, score);

        // TODO: send email reply with score.
        JSONObject obj = new JSONObject();
        obj.put("score", score);
        obj.put("errorCases", errorCases);
        
        if (this.sheetsInteractor != null)
        {
            String cellRange = this.sheetsInteractor.getCellRange(contestSubmission.teamName, contestSubmission.contestDivision, pb.getAbsoluteId());
            int bestScore = Math.max(score, this.sheetsInteractor.getCellValueInt(cellRange));
            this.sheetsInteractor.writeCellValue(cellRange, bestScore);
        }

        String reply = String.format("Hey %s (%s),\n\n"
            + "You passed %d out of %d test cases correct for problem [%s #%d]. (%d error cases) This is submission %d of %d allowed submissions.\n"
            + "\n\n" + "Best,\n\n" + "TeamsCode Staff\n"
            + "NOTE: This reply was automatically generated. For technical assistance, please message TeamsCode contest organizers.",
            contestSubmission.teamName, contestSubmission.senderEmail, score, GraderInfo.POINTS_PER_PROBLEM,
            pb.problemDifficulty.toString(), pb.problemNumRelative, errorCases, submissions + 1,
            GraderInfo.MAXIMUM_SUBMISSION_COUNT);
        sendEmailReply(contestSubmission.uid, reply);

        contestSubmission.state = SubmissionState.PROCESSED_GRADED;
        contestSubmission.miscInfo = obj.toString();
        this.sqlUtil.updateSubmissionStatus(contestSubmission);
    }

}
