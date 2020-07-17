package graderio;

import graderobjects.ProblemBundle;
import graderobjects.ProblemDifficulty;
import graderobjects.ProgrammingLanguage;

public class ParserUtil
{
    public static String getMainJavaClassName(String code)
    {
        if (code == null)
        {
            return null;
        }
        
        int idx = code.indexOf("public class");

        if (idx == -1)
        {
            return null;
        }

        int start = idx + 13;
        int pos = start;

        String className = "";

        while (pos < code.length())
        {
            char c = code.charAt(pos);
            
            if (c == '{') 
            {
                break;
            }

            if (c == ' ' || c == 0xA || c == 0xD)
            {
                if (className.length() == 0)
                {
                    pos++;
                    continue;
                }
                else
                {
                    break;
                }
            }
            className += String.valueOf(c);

            pos++;
        }

        if (className.length() == 0)
        {
            className = null;
        }

        return className;
    }
    
    public static ProgrammingLanguage getProgrammingLanguage(String[] subjectCommaSplit)
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

    public static ProblemBundle getProblemBundle(String[] subjectCommaSplit)
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

        if (toCheck.contains("easy"))
        {
            difficulty = ProblemDifficulty.EASY;
        }
        else if (toCheck.contains("medium"))
        {
            difficulty = ProblemDifficulty.MEDIUM;
        }
        else if (toCheck.contains("hard"))
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
}
