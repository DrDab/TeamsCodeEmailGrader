package graderio;

import emailgrader.GraderInfo;

@SuppressWarnings("all")
public class ExecutableLocator
{
    public String getJava()
    {
        return GraderInfo.JAVA_LOCATION;
    }

    public String getJavac()
    {
        return GraderInfo.JAVAC_LOCATION;
    }

    public String getGCC()
    {
        return GraderInfo.GCC_LOCATION;
    }

    public String getGPP()
    {
        return GraderInfo.GPP_LOCATION;
    }

    public String getPython2()
    {
        return GraderInfo.PYTHON2_LOCATION;
    }

    public String getPython3()
    {
        return GraderInfo.PYTHON3_LOCATION;
    }

    public String getMCS()
    {
        return GraderInfo.CSHARP_BUILD_LOCATION;
    }

    public String getCSharpRunner()
    {
        return GraderInfo.CSHARP_RUN_LOCATION;
    }
}