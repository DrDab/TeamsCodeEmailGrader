package graderio;

/*
 * Copyright (c) 2018 TeamsCode, Victor Du
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;

import org.json.JSONObject;

@SuppressWarnings("all")
public class ExecutableLocator
{
    private String directory;
    private File jsonFile;
    private String jsonData;
    private JSONObject mainObject;

    public ExecutableLocator()
    {
        try
        {
            this.directory = directory;
            File configFolder = new File("config");
            if (configFolder.exists())
            {
                if (configFolder.isFile())
                {
                    configFolder.delete();
                    configFolder = new File("config");
                    configFolder.mkdir();
                }
            }
            else
            {
                configFolder.mkdir();
            }
            jsonFile = new File(configFolder, "settings.json");
            if (!jsonFile.exists())
            {
                jsonFile.createNewFile();
                PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(jsonFile)));
                pw.println("{");
                pw.println("	\"java\":\"/usr/bin/java\",");
                pw.println("	\"javac\":\"/usr/bin/javac\",");
                pw.println("	\"gcc\":\"/usr/bin/gcc\",");
                pw.println("	\"g++\":\"/usr/bin/g++\",");
                pw.println("	\"python2\":\"/usr/bin/python2\",");
                pw.println("	\"python3\":\"/usr/bin/python3\",");
                pw.println("	\"csharp-build\":\"/usr/bin/mcs\",");
                pw.println("	\"csharp-run\":\"/usr/bin/mono\"");
                pw.println("}");
                pw.close();
            }
            String jsonData = "";
            try
            {
                jsonData = new String(Files.readAllBytes((jsonFile.toPath())));
            }
            catch (IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            mainObject = new JSONObject(jsonData);
        }
        catch (Exception e)
        {
            System.err.println("Couldn't load settings JSON: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public File getJava()
    {
        if (mainObject.has("java"))
        {
            return new File(mainObject.getString("java"));
        }
        return null;
    }

    public File getJavac()
    {
        if (mainObject.has("javac"))
        {
            return new File(mainObject.getString("javac"));
        }
        return null;
    }

    public File getGCC()
    {
        if (mainObject.has("gcc"))
        {
            return new File(mainObject.getString("gcc"));
        }
        return null;
    }

    public File getGPP()
    {
        if (mainObject.has("g++"))
        {
            return new File(mainObject.getString("g++"));
        }
        return null;
    }

    public File getPython2()
    {
        if (mainObject.has("python2"))
        {
            return new File(mainObject.getString("python2"));
        }
        return null;
    }

    public File getPython3()
    {
        if (mainObject.has("python3"))
        {
            return new File(mainObject.getString("python3"));
        }
        return null;
    }

    public File getMCS()
    {
        if (mainObject.has("csharp-build"))
        {
            return new File(mainObject.getString("csharp-build"));
        }
        return null;
    }

    public File getCSharpRunner()
    {
        if (mainObject.has("csharp-run"))
        {
            return new File(mainObject.getString("csharp-run"));
        }
        return null;
    }
}