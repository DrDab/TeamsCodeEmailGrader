package graderobjects;

public class ContestSubmission 
{
	public String submissionEmail;
	public ContestDivision division;
	public String teamName;
	public ProblemDifficulty difficulty;
	public int problemDivisionNumber;
	public ProgrammingLanguage programmingLanguage;
	
	public ContestSubmission(String submissionEmail,
			ContestDivision division,
			String teamName,
			ProblemDifficulty difficulty,
			int problemDivisionNumber,
			ProgrammingLanguage programmingLanguage)
	{
		this.submissionEmail = submissionEmail;
		this.division = division;
		this.teamName = teamName;
		this.difficulty = difficulty;
		this.problemDivisionNumber = problemDivisionNumber;
		this.programmingLanguage = programmingLanguage;
	}
	
	public int getProblemId()
	{
		int owo = 0;
		switch (this.difficulty)
		{
			case EASY:
				owo = 1;
				break;
				
			case MEDIUM:
				owo = 6;
				break;
				
			case HARD:
				owo = 11;
				break;
				
			default:
				break;
		}
		return owo + this.problemDivisionNumber;
	}
}
