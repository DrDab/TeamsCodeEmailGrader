package graderio;

import java.sql.SQLException;

import graderobjects.ContestSubmission;
import graderobjects.ContestTeam;
import graderobjects.ProblemDifficulty;
import graderobjects.ProgrammingLanguage;
import graderobjects.SubmissionState;

@SuppressWarnings("unused")
public class SubmissionProcessorRunnable implements Runnable
{
    private SubmissionProcessor submissionProcessor;
    private SQLUtil sqlUtil;
    private EmailUtil emailUtil;
    private boolean finished;
    private long queryRate;

    public SubmissionProcessorRunnable(SubmissionProcessor submissionProcessor, SQLUtil sqlUtil, EmailUtil emailUtil,
        long queryRate)
    {
        this.submissionProcessor = submissionProcessor;
        this.sqlUtil = sqlUtil;
        this.emailUtil = emailUtil;
        this.finished = false;
        this.queryRate = queryRate;
    }

    private ProgrammingLanguage getProgrammingLanguage(String[] subjectCommaSplit)
    {
        if (subjectCommaSplit == null)
        {
            return null;
        }
        if (subjectCommaSplit.length < 4)
        {
            return null;
        }
        String p3 = subjectCommaSplit[3];
        if (p3 == null)
        {
            return null;
        }
        String toCheck = p3.toLowerCase().trim();

        if (toCheck.length() == 0)
        {
            return null;
        }

        if (toCheck.contains("java") && !toCheck.contains("script"))
        {
            return ProgrammingLanguage.JAVA;
        }
        if (toCheck.equals("c") || toCheck.equals("gcc") || toCheck.equals("gnu c") || toCheck.equals("c language"))
        {
            return ProgrammingLanguage.C;
        }
        if (toCheck.contains("c++") || toCheck.contains("cpp") || toCheck.contains("gpp")
            || (toCheck.contains("c") && toCheck.contains("plus")))
        {
            return ProgrammingLanguage.C_PLUS_PLUS;
        }
        if (toCheck.contains("c#") || (toCheck.contains("c") && toCheck.contains("sharp")))
        {
            return ProgrammingLanguage.C_SHARP;
        }
        if (toCheck.contains("python"))
        {
            if (toCheck.contains("2"))
            {
                return ProgrammingLanguage.PYTHON_2;
            }
            return ProgrammingLanguage.PYTHON_3;
        }
        return ProgrammingLanguage.OTHER;
    }

    private class ProblemBundle
    {
        public ProblemDifficulty problemDifficulty;
        public int problemNumRelative;

        public ProblemBundle(ProblemDifficulty problemDifficulty, int problemNumRelative)
        {
            this.problemDifficulty = problemDifficulty;
            this.problemNumRelative = problemNumRelative;
        }

        public int getAbsoluteId()
        {
            int owo = 0;
            switch (problemDifficulty)
            {
                case MEDIUM:
                    owo = 5;
                    break;

                case HARD:
                    owo = 10;
                    break;

                default:
                    break;
            }
            return owo + this.problemNumRelative;
        }

        public String toString()
        {
            return this.problemDifficulty.toString() + "," + this.problemNumRelative;
        }
    }

    public ProblemBundle getProblemBundle(String[] subjectCommaSplit)
    {
        if (subjectCommaSplit == null)
        {
            return null;
        }
        if (subjectCommaSplit.length < 3)
        {
            return null;
        }
        String p2 = subjectCommaSplit[2];
        if (p2 == null)
        {
            return null;
        }
        String toCheck = p2.toLowerCase().trim();

        if (toCheck.length() == 0)
        {
            return null;
        }

        ProblemDifficulty difficulty = null;
        int relNum = -1;

        if (toCheck.contains("eas"))
        {
            difficulty = ProblemDifficulty.EASY;
        }
        else if (toCheck.contains("m"))
        {
            difficulty = ProblemDifficulty.MEDIUM;
        }
        else if (toCheck.contains("h"))
        {
            difficulty = ProblemDifficulty.HARD;
        }

        if (toCheck.contains("1"))
        {
            relNum = 1;
        }
        if (toCheck.contains("2"))
        {
            relNum = 2;
        }
        if (toCheck.contains("3"))
        {
            relNum = 3;
        }
        if (toCheck.contains("4"))
        {
            relNum = 4;
        }
        if (toCheck.contains("5"))
        {
            relNum = 5;
        }

        return new ProblemBundle(difficulty, relNum);
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
                    System.out.println("checking pending sub " + cur.uid);
                    // only thing we read from header is language and problem
                    // number.
                    // we will read the division and team name from
                    // x-referencing the sender email.

                    String sender = cur.senderEmail;
                    ContestTeam senderTeam = this.sqlUtil.getTeamFromEmail(sender);
                    if (senderTeam == null)
                    {
                        cur.state = SubmissionState.PROCESSED_REJECTED;
                        cur.miscInfo = "UNKNOWN EMAIL";
                        // TODO: add reply that the email is unknown.

                        this.sqlUtil.updateSubmissionStatus(cur);
                        continue;
                    }

                    // read the programming language and difficulty.
                    // String[] subjectSplit = cur.subject.split(",");

                }
                Thread.sleep(this.queryRate);
            }
        }
        catch (SQLException | InterruptedException e)
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
