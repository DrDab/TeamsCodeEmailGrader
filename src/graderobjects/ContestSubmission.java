package graderobjects;

import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONObject;

public class ContestSubmission
{
    public long id;
    public long uid;
    public String senderEmail;
    public long date;
    public String subject;
    public String body;
    public HashMap<String, Byte[]> attachmentData;
    public SubmissionState state;
    public ContestDivision contestDivision;
    public String teamName;
    public ProblemDifficulty problemDifficulty;
    public Integer problemIdAbsolute;
    public ProgrammingLanguage programmingLanguage;
    public String miscInfo;

    public ContestSubmission(long id, long uid, String senderEmail, long date, String subject, String body,
        HashMap<String, Byte[]> attachmentData, SubmissionState state, ContestDivision contestDivision, String teamName,
        ProblemDifficulty problemDifficulty, Integer problemIdAbsolute, ProgrammingLanguage programmingLanguage, String miscInfo)
    {
        this.id = id;
        this.uid = uid;
        this.senderEmail = senderEmail;
        this.date = date;
        this.subject = subject;
        this.body = body;
        this.attachmentData = attachmentData;
        this.state = state;
        this.contestDivision = contestDivision;
        this.teamName = teamName;
        this.problemDifficulty = problemDifficulty;
        this.problemIdAbsolute = problemIdAbsolute;
        this.programmingLanguage = programmingLanguage;
        this.miscInfo = miscInfo;
    }

    public ContestSubmission(long id, long uid, String senderEmail, long date, String subject, String body,
        HashMap<String, Byte[]> attachmentData, SubmissionState state)
    {
        this(id, uid, senderEmail, date, subject, body, attachmentData, state, null, null, null, null, null, null);
    }
    
    public JSONObject attachmentDataToJSONObject()
    {
        if (this.attachmentData == null)
        {
            return null;
        }
        JSONObject toReturn = new JSONObject();
        JSONArray jsonArrayOfAttachments = new JSONArray();
        for (String key : this.attachmentData.keySet())
        {
            JSONArray toReturnSubarrayL1 = new JSONArray();
            toReturnSubarrayL1.put(0, key);
            JSONArray toReturnSubarrayL2 = new JSONArray();
            Byte[] byteArray = this.attachmentData.get(key);
            for (int i = 0; i < byteArray.length; i++)
            {
                toReturnSubarrayL2.put(i, byteArray[i]);
            }
            toReturnSubarrayL1.put(1, toReturnSubarrayL2);
            jsonArrayOfAttachments.put(toReturnSubarrayL1);
        }
        toReturn.put("data", jsonArrayOfAttachments);
        return toReturn;
    }
}
