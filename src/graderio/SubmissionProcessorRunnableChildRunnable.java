package graderio;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;

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

public class SubmissionProcessorRunnableChildRunnable implements Runnable
{
    private SubmissionProcessorRunnable parentRunnable;
    private ContestSubmission contestSubmission;

    public SubmissionProcessorRunnableChildRunnable(SubmissionProcessorRunnable parentRunnable,
        ContestSubmission contestSubmission)
    {
        this.parentRunnable = parentRunnable;
        this.contestSubmission = contestSubmission;
    }

    @Override
    public void run()
    {
        this.parentRunnable.numThreads++;
        this.judgeSubmission();
        this.parentRunnable.numThreads--;
    }

    public void judgeSubmission()
    {
        try
        {
            String senderEmailStr = contestSubmission.senderEmail;
            InternetAddress address = new InternetAddress(senderEmailStr);
            String senderEmail = address.getAddress();

            if (GraderInfo.ENFORCE_TIME_LIMITS)
            {
                if (contestSubmission.date < GraderInfo.CONTEST_START_DATE
                    || contestSubmission.date > GraderInfo.CONTEST_END_DATE)
                {
                    InvalidationReason reason = InvalidationReason.TIME_LIMIT_VIOLATION;
                    parentRunnable.setSubmissionInvalid(contestSubmission, reason);
                    // TODO: add reply that the subject is missing.
                    String reply = String.format("Hey %s,\n\n"
                        + "We experienced an error while handling your submission. Reason: %s.\n"
                        + "This either means you submitted your solution before the contest start time, or after the contest end time.\n"
                        + "Your submission has not been graded, nor counted as part of the per-problem submission limit.\n"
                        + "\n\n" + "Best,\n\n" + "TeamsCode Staff\n"
                        + "NOTE: This reply was automatically generated. For technical assistance, please message TeamsCode contest organizers.",
                        contestSubmission.senderEmail, reason);
                    parentRunnable.sendEmailReply(contestSubmission.uid, reply);
                    parentRunnable.sqlUtil.updateSubmissionStatus(contestSubmission);
                    return;
                }
            }

            ContestTeam senderTeam = parentRunnable.sqlUtil.getTeamFromEmail(senderEmail);
            if (senderTeam == null)
            {
                InvalidationReason reason = InvalidationReason.INVALID_EMAIL_ACCESS_DENIED;
                parentRunnable.setSubmissionInvalid(contestSubmission, reason);
                // TODO: add reply that the subject is missing.
                String reply = String.format("Hey %s,\n\n"
                    + "We experienced an error while handling your submission. Reason: %s.\n"
                    + "To fix this issue, please use the email you signed up for this contest with to submit your code.\n"
                    + "Please resolve this issue. Your submission has not been graded, nor counted as part of the per-problem submission limit.\n"
                    + "\n\n" + "Best,\n\n" + "TeamsCode Staff\n"
                    + "NOTE: This reply was automatically generated. For technical assistance, please message TeamsCode contest organizers.",
                    contestSubmission.senderEmail, reason);
                parentRunnable.sendEmailReply(contestSubmission.uid, reply);
                parentRunnable.sqlUtil.updateSubmissionStatus(contestSubmission);
                return;
            }

            contestSubmission.teamName = senderTeam.teamName;
            contestSubmission.contestDivision = senderTeam.contestDivision;

            String subject = contestSubmission.subject;

            if (!parentRunnable.validateSubject(subject))
            {
                InvalidationReason reason = InvalidationReason.SUBJECT_MISSING;
                parentRunnable.setSubmissionInvalid(contestSubmission, reason);
                String reply = String.format("Hey %s (%s),\n\n"
                    + "We experienced an error while handling your submission. Reason: %s.\n"
                    + "To fix this issue, please write a valid subject, in the format as follows:.\n"
                    + "Division, Team Name, Problem number and difficulty, programming language. (example: Intermediate, Team Foo, Easy #1, Python 2)\n"
                    + "Your submission has not been graded, nor counted as part of the per-problem submission limit.\n"
                    + "\n\n" + "Best,\n\n" + "TeamsCode Staff\n"
                    + "NOTE: This reply was automatically generated. For technical assistance, please message TeamsCode contest organizers.",
                    contestSubmission.teamName, contestSubmission.senderEmail, reason);
                parentRunnable.sendEmailReply(contestSubmission.uid, reply);
                parentRunnable.sqlUtil.updateSubmissionStatus(contestSubmission);
                return;
            }

            String[] subjectSplit = subject.split(",");
            ProblemBundle pb = ParserUtil.getProblemBundle(subjectSplit);
            ProgrammingLanguage programmingLanguage = ParserUtil.getProgrammingLanguage(subjectSplit);

            if (!parentRunnable.validateProblemBundle(pb) || programmingLanguage == null)
            {
                InvalidationReason reason = InvalidationReason.SUBJECT_INVALID;
                parentRunnable.setSubmissionInvalid(contestSubmission, reason);
                String reply = String.format("Hey %s (%s),\n\n"
                    + "We experienced an error while handling your submission. Reason: %s.\n"
                    + "To fix this issue, please write a valid subject, in the format as follows:.\n"
                    + "Division, Team Name, Problem number and difficulty, programming language. (example: Intermediate, Team Foo, Easy #1, Python 2)\n"
                    + "Your submission has not been graded, nor counted as part of the per-problem submission limit.\n"
                    + "\n\n" + "Best,\n\n" + "TeamsCode Staff\n"
                    + "NOTE: This reply was automatically generated. For technical assistance, please message TeamsCode contest organizers.",
                    contestSubmission.teamName, contestSubmission.senderEmail, reason);
                parentRunnable.sendEmailReply(contestSubmission.uid, reply);
                parentRunnable.sqlUtil.updateSubmissionStatus(contestSubmission);
                return;
            }

            contestSubmission.programmingLanguage = programmingLanguage;
            contestSubmission.problemDifficulty = pb.problemDifficulty;
            contestSubmission.problemIdAbsolute = pb.getAbsoluteId();

            int submissions = parentRunnable.sqlUtil.getSubmissionCountPerProblemPerTeam(contestSubmission.teamName,
                contestSubmission.problemIdAbsolute);

            if (submissions >= GraderInfo.MAXIMUM_SUBMISSION_COUNT
                && contestSubmission.state != SubmissionState.AWAITING_PROCESSING_OVERRIDE_ATTEMPT_LIMITS)
            {
                InvalidationReason reason = InvalidationReason.SUBMISSION_COUNT_EXCEEDED;
                parentRunnable.setSubmissionInvalid(contestSubmission, reason);
                String reply = String.format("Hey %s (%s),\n\n"
                    + "We experienced an error while handling your submission. Reason: %s.\n"
                    + "It seems you have exceeded the submission count limit per problem, per team. (the limit is %d submissions/problem/team.)\n"
                    + "Your submission has not been graded, nor counted as part of the per-problem submission limit.\n"
                    + "\n\n" + "Best,\n\n" + "TeamsCode Staff\n"
                    + "NOTE: This reply was automatically generated. For technical assistance, please message TeamsCode contest organizers.",
                    contestSubmission.teamName, contestSubmission.senderEmail, reason,
                    GraderInfo.MAXIMUM_SUBMISSION_COUNT);
                parentRunnable.sendEmailReply(contestSubmission.uid, reply);
                parentRunnable.sqlUtil.updateSubmissionStatus(contestSubmission);
                return;
            }

            if (programmingLanguage == ProgrammingLanguage.OTHER)
            {
                InvalidationReason reason = InvalidationReason.FOREIGN_PROGRAMMING_LANG;
                parentRunnable.setSubmissionInvalid(contestSubmission, reason);
                String reply = String.format("Hey %s (%s),\n\n"
                    + "We experienced an error while handling your submission. Reason: %s.\n"
                    + "To fix this issue, please use a permitted language to submit your code. (allowed: C, C++, C#, Java, Python 2, Python 3)\n"
                    + "Your submission has not been graded, nor counted as part of the per-problem submission limit.\n"
                    + "\n\n" + "Best,\n\n" + "TeamsCode Staff\n"
                    + "NOTE: This reply was automatically generated. For technical assistance, please message TeamsCode contest organizers.",
                    contestSubmission.teamName, contestSubmission.senderEmail, reason);
                parentRunnable.sendEmailReply(contestSubmission.uid, reply);
                parentRunnable.sqlUtil.updateSubmissionStatus(contestSubmission);
                return;
            }

            ContestProblem problem = parentRunnable.sqlUtil.getProblemById(pb.getAbsoluteId(),
                senderTeam.contestDivision);

            File submissionDir = parentRunnable.programIOUtil.getExecutableParentFolder(contestSubmission.id);
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
                            attachmentTargeted = key.contains(".cpp") || key.contains(".cc") || key.contains(".C")
                                || key.contains(".cxx") || key.contains(".c++");
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
                    parentRunnable.setSubmissionInvalid(contestSubmission, reason);
                    String reply = String.format("Hey %s (%s),\n\n"
                        + "We experienced an error while handling your submission. Reason: %s.\n"
                        + "To fix this issue, please include a valid class name for the public class in your Java code.\n"
                        + "Your submission has not been graded, nor counted as part of the per-problem submission limit.\n"
                        + "\n\n" + "Best,\n\n" + "TeamsCode Staff\n"
                        + "NOTE: This reply was automatically generated. For technical assistance, please message TeamsCode contest organizers.",
                        contestSubmission.teamName, contestSubmission.senderEmail, reason);
                    parentRunnable.sendEmailReply(contestSubmission.uid, reply);
                    parentRunnable.sqlUtil.updateSubmissionStatus(contestSubmission);
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
                parentRunnable.setSubmissionInvalid(contestSubmission, reason);
                String reply = String.format("Hey %s (%s),\n\n"
                    + "We experienced an error while handling your submission. Reason: %s.\n"
                    + "Please fix this issue and resubmit your code.\n"
                    + "Your submission has not been graded, nor counted as part of the per-problem submission limit.\n"
                    + "\n\n" + "Best,\n\n" + "TeamsCode Staff\n"
                    + "NOTE: This reply was automatically generated. For technical assistance, please message TeamsCode contest organizers.",
                    contestSubmission.teamName, contestSubmission.senderEmail, reason);
                parentRunnable.sendEmailReply(contestSubmission.uid, reply);
                parentRunnable.sqlUtil.updateSubmissionStatus(contestSubmission);
                return;
            }

            // write the code to a file.
            File codeFile = new File(submissionDir, fileName);
            PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(codeFile)));
            pw.println(code);
            pw.close();

            // now that everything is validated, compile the code and
            // test it.
            PostExecutionResults compileResults = parentRunnable.programIOUtil.compileProgram(submissionName, codeFile,
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
                parentRunnable.setSubmissionInvalid(contestSubmission, reason);
                String reply = String.format("Hey %s (%s),\n\n"
                    + "We experienced an error while handling your submission. Reason: %s.\n"
                    + "Please fix this issue and resubmit your code.\n"
                    + "Your submission has not been graded, nor counted as part of the per-problem submission limit.\n"
                    + "\n\n" + "Best,\n\n" + "TeamsCode Staff\n"
                    + "NOTE: This reply was automatically generated. For technical assistance, please message TeamsCode contest organizers.",
                    contestSubmission.teamName, contestSubmission.senderEmail, reason);
                parentRunnable.sendEmailReply(contestSubmission.uid, reply);
                parentRunnable.sqlUtil.updateSubmissionStatus(contestSubmission);
                return;
            }

            // System.out.println(compiledFile);

            System.out.printf("Running submission %d, l=%s, pid=%s, team=%s\n", contestSubmission.id,
                programmingLanguage, problem.name, contestSubmission.teamName);

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
                String stdInString = parentRunnable.trimRight(new String(Files.readAllBytes(inputFile.toPath())));
                String stdOutString = parentRunnable.trimRight(new String(Files.readAllBytes(outputFile.toPath())));
                PostExecutionResults testCaseResults = parentRunnable.runTestCase(i, submissionName, compiledFile,
                    stdInString, programmingLanguage, GraderInfo.EXECUTE_TIME_LIMIT);
                String progStdOut = parentRunnable.trimRight(testCaseResults.getStdOut());
                String progStdErr = parentRunnable.trimRight(testCaseResults.getStdErr());
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

            String sheetsInfoStr = "";

            if (parentRunnable.sheetsInteractor != null)
            {
                String problemCellRange = parentRunnable.sheetsInteractor.getCellRangeForProblem(
                    contestSubmission.teamName, contestSubmission.contestDivision, pb.getAbsoluteId());
                if (problemCellRange != null)
                {
                    Integer prevScore = parentRunnable.sheetsInteractor.getCellValueInt(problemCellRange);
                    if (prevScore != null)
                    {
                        int bestScore = Math.max(score, prevScore);
                        parentRunnable.sheetsInteractor.writeCellValue(problemCellRange, bestScore);
                    }
                }
                String totalScoreCellRange = parentRunnable.sheetsInteractor
                    .getCellRangeForTeamTotalScore(contestSubmission.teamName, contestSubmission.contestDivision);
                if (totalScoreCellRange != null)
                {
                    Integer totalScore = parentRunnable.sheetsInteractor.getCellValueInt(totalScoreCellRange);
                    if (totalScore != null)
                    {
                        sheetsInfoStr = String.format("Your total score so far is: %d", totalScore);
                    }
                }
            }

            String reply = String.format("Hey %s (%s),\n\n"
                + "You passed %d out of %d test cases correct for problem [%s #%d]. (%d error cases) This is submission %d of %d allowed submissions.\n"
                + "%s\n\n\n" + "Best,\n\n" + "TeamsCode Staff\n"
                + "NOTE: This reply was automatically generated. For technical assistance, please message TeamsCode contest organizers.",
                contestSubmission.teamName, contestSubmission.senderEmail, score, GraderInfo.POINTS_PER_PROBLEM,
                pb.problemDifficulty.toString(), pb.problemNumRelative, errorCases, submissions + 1,
                GraderInfo.MAXIMUM_SUBMISSION_COUNT, sheetsInfoStr);
            parentRunnable.sendEmailReply(contestSubmission.uid, reply);

            contestSubmission.state = SubmissionState.PROCESSED_GRADED;
            contestSubmission.miscInfo = obj.toString();
            parentRunnable.sqlUtil.updateSubmissionStatus(contestSubmission);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

}
