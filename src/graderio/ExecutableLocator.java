package graderio;

import emailgrader.GraderInfo;

@SuppressWarnings("all")
public class ExecutableLocator
{
    private GraderInfo graderInfo;

    public ExecutableLocator(GraderInfo graderInfo)
    {
        this.graderInfo = graderInfo;
    }

    public String getJava()
    {
        return graderInfo.JAVA_LOCATION;
    }

    public String getJavac()
    {
        return graderInfo.JAVAC_LOCATION;
    }

    public String getGCC()
    {
        return graderInfo.GCC_LOCATION;
    }

    public String getGPP()
    {
        return graderInfo.GPP_LOCATION;
    }

    public String getPython2()
    {
        return graderInfo.PYTHON2_LOCATION;
    }

    public String getPython3()
    {
        return graderInfo.PYTHON3_LOCATION;
    }

    public String getMCS()
    {
        return graderInfo.CSHARP_BUILD_LOCATION;
    }

    public String getCSharpRunner()
    {
        return graderInfo.CSHARP_RUN_LOCATION;
    }
}