package emailgrader;

import java.io.File;
import java.util.ArrayList;

import graderio.PostExecutionResults;
import graderio.ProgramIOUtil;

public class GraderMain 
{
	public static void main(String[] args)
	{
		ProgramIOUtil owo = new ProgramIOUtil();
		try {
			ArrayList<String> progArgs = new ArrayList<>();
			progArgs.add("-lh");
			progArgs.add("--sort=t");
			PostExecutionResults uwu = owo.runExecutable(new File("ls.exe"), progArgs, "", "test", 1000L);
			System.out.println(uwu.getStdOut());
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
