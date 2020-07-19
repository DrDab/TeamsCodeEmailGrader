package graderobjects;

import java.util.HashMap;

/*
 * Copyright (c) 2018-2020 TeamsCode, Victor Du
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

public class PostExecutionResults
{
    private String stdin;
    private String stdout;
    private String stderr;
    private double millis;
    public ExecutionResultStatus executionResultStatus;
    public HashMap<String, String> miscInfo;

    public PostExecutionResults(String stdin, String stdout, String stderr, double millis,
        ExecutionResultStatus executionResultStatus)
    {
        this(stdin, stdout, stderr, millis, executionResultStatus, new HashMap<String, String>());
    }

    public PostExecutionResults(String stdin, String stdout, String stderr, double millis,
        ExecutionResultStatus executionResultStatus, HashMap<String, String> miscInfo)
    {
        this.stdin = stdin;
        this.stdout = stdout;
        this.stderr = stderr;
        this.millis = millis;
        this.executionResultStatus = executionResultStatus;
        this.miscInfo = miscInfo;
    }

    public String getStdIn()
    {
        return this.stdin;
    }

    public String getStdOut()
    {
        return this.stdout;
    }

    public String getStdErr()
    {
        return this.stderr;
    }

    public double getMillis()
    {
        return this.millis;
    }
    
}