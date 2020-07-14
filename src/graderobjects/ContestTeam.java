package graderobjects;

import java.util.ArrayList;

public class ContestTeam
{
    public String teamName;
    public ContestDivision contestDivision;
    public ArrayList<String> emails;

    public ContestTeam(String teamName, ContestDivision contestDivision, ArrayList<String> emails)
    {
        this.teamName = teamName;
        this.contestDivision = contestDivision;
        this.emails = emails;
    }
}
