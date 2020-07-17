package graderobjects;

import java.util.ArrayList;

public class ContestProblem
{
    public String name;
    public ArrayList<String[]> inputOutputArgs;

    public ContestProblem(String name, String... inputOutputArr)
    {
        this.name = name;
        this.inputOutputArgs = new ArrayList<String[]>();

        ArrayList<String> inputFiles = new ArrayList<String>();
        ArrayList<String> outputFiles = new ArrayList<String>();

        int half = inputOutputArr.length / 2;
        for (int i = 0; i < inputOutputArr.length; i++)
        {
            if (i < half)
            {
                inputFiles.add(inputOutputArr[i]);
            }
            else
            {
                outputFiles.add(inputOutputArr[i]);
            }
        }

        for (int i = 0; i < half; i++)
        {
            String[] toAdd = new String[] { inputFiles.get(i), outputFiles.get(i) };
            this.inputOutputArgs.add(toAdd);
        }
    }
}
