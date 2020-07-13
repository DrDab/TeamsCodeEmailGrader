package graderobjects;

public class ContestSubmission 
{
	public long id;
	public String senderEmail;
	public long date;
	public String subject;
	public String body;
	public SubmissionState state;
	public ContestDivision contestDivision;
	public String teamName;
	public ProblemDifficulty problemDifficulty;
	public ProgrammingLanguage programmingLanguage;
	public String miscInfo;
	
	public ContestSubmission(long id, 
							 String senderEmail, 
							 long date,
							 String subject, 
							 String body, 
							 SubmissionState state,
							 ContestDivision contestDivision,
							 String teamName,
							 ProblemDifficulty problemDifficulty,
							 ProgrammingLanguage programmingLanguage, 
							 String miscInfo)
	{
		this.id = id;
		this.senderEmail = senderEmail;
		this.date = date;
		this.subject = subject;
		this.body = body;
		this.state = state;
		this.contestDivision = contestDivision;
		this.teamName = teamName;
		this.problemDifficulty = problemDifficulty;
		this.programmingLanguage = programmingLanguage;
		this.miscInfo = miscInfo;
	}
	
	public ContestSubmission(long id, String senderEmail, long date, String subject, String body, SubmissionState state)
	{
		this(id,
			 senderEmail, 
			 date,
			 subject, 
			 body, 
			 state, 
			 null, 
			 null,
			 null, 
			 null, 
			 null);
	}
}
