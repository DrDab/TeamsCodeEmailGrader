package graderio;

import java.io.File;

import emailgrader.GraderInfo;

@SuppressWarnings("all")
public class ExecutableLocator
{
    private GraderInfo graderInfo;
    
    public ExecutableLocator(GraderInfo graderInfo)
    {
        this.graderInfo = graderInfo;
    }
    
    public File getJava()
    {
        return new File(graderInfo.JAVA_LOCATION);
    }

    public File getJavac()
    {
        return new File(graderInfo.JAVAC_LOCATION);
    }

    public File getGCC()
    {
        return new File(graderInfo.GCC_LOCATION);
    }

    public File getGPP()
    {
        return new File(graderInfo.GPP_LOCATION);
    }

    public File getPython2()
    {
        return new File(graderInfo.PYTHON2_LOCATION);
    }

    public File getPython3()
    {
        return new File(graderInfo.PYTHON3_LOCATION);
    }

    public File getMCS()
    {
        return new File(graderInfo.CSHARP_BUILD_LOCATION);
    }

    public File getCSharpRunner()
    {
       return new File(graderInfo.CSHARP_RUN_LOCATION);
    }
}