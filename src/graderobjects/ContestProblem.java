package graderobjects;

import java.util.ArrayList;

public class ContestProblem
{
    public String name;
    public ArrayList<String[]> inputOutputArgs;

    public ContestProblem(String name, String i1, String i2, String i3, String o1, String o2, String o3)
    {
        this.name = name;
        this.inputOutputArgs = new ArrayList<String[]>();
        inputOutputArgs.add(new String[] {i1, o1});
        inputOutputArgs.add(new String[] {i2, o2});
        inputOutputArgs.add(new String[] {i3, o3});
    }
}
