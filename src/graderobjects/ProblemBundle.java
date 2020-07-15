package graderobjects;

public class ProblemBundle
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