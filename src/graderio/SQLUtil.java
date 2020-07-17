package graderio;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;

import emailgrader.GraderInfo;
import graderobjects.ContestDivision;
import graderobjects.ContestProblem;
import graderobjects.ContestSubmission;
import graderobjects.ContestTeam;
import graderobjects.ProblemDifficulty;
import graderobjects.ProgrammingLanguage;
import graderobjects.SubmissionState;

public class SQLUtil
{
    private Connection sqlConnection = null;

    public SQLUtil(String filePath) throws SQLException
    {
        try
        {
            Class.forName("org.sqlite.JDBC");
        }
        catch (ClassNotFoundException cnfe)
        {
            cnfe.printStackTrace();
        }

        this.sqlConnection = DriverManager.getConnection("jdbc:sqlite:" + filePath);
        initTables();
    }

    public boolean tableExists(String table) throws SQLException
    {
        DatabaseMetaData dbm = this.sqlConnection.getMetaData();
        ResultSet tables = dbm.getTables(null, null, table, null);
        return tables.next();
    }

    public void initTables() throws SQLException
    {
        if (!this.tableExists("submissionsDb"))
        {
            String query = "CREATE TABLE submissionsDb(id INT, uid INT, senderEmail TEXT, submissionDate INT, "
                + "subject TEXT NULL, body TEXT NULL, state TEXT, contestDivision TEXT NULL, teamName TEXT NULL,"
                + " problemDifficulty TEXT NULL, programmingLanguage TEXT NULL, miscInfo TEXT NULL);";
            PreparedStatement stmt = sqlConnection.prepareStatement(query);
            stmt.execute();
        }
        if (!this.tableExists("contestantsDb"))
        {
            String query = "CREATE TABLE contestantsDb(name TEXT, division TEXT, email1 TEXT NULL, email2 TEXT NULL, "
                + "email3 TEXT NULL, email4 TEXT NULL, email5 TEXT NULL, email6 TEXT NULL);";
            PreparedStatement stmt = sqlConnection.prepareStatement(query);
            stmt.execute();
        }
        if (!this.tableExists("problemsDb"))
        {
            String query = "CREATE TABLE problemsDb(name TEXT, i1 TEXT, i2 TEXT NULL, i3 TEXT NULL, o1 TEXT, o2 TEXT NULL, o3 TEXT NULL, intermediateId INT, advancedId INT)";
            PreparedStatement stmt = sqlConnection.prepareStatement(query);
            stmt.execute();
        }
    }

    private ContestProblem getProblemFromResultSet(ResultSet rs) throws SQLException
    {
        if (rs == null)
        {
            return null;
        }
        if (!rs.next())
        {
            return null;
        }
        return new ContestProblem(rs.getString("name"), rs.getString("i1"), rs.getString("i2"), rs.getString("i3"),
            rs.getString("o1"), rs.getString("o2"), rs.getString("o3"));
    }

    public ContestProblem getProblemById(int absoluteId, ContestDivision division) throws SQLException
    {
        String query = String.format("SELECT * FROM problemsDb WHERE %s = ?",
            division == ContestDivision.INTERMEDIATE ? "intermediateId" : "advancedId");
        PreparedStatement stmt = this.sqlConnection.prepareStatement(query);
        stmt.setInt(1, absoluteId);
        ResultSet rs = stmt.executeQuery();
        return getProblemFromResultSet(rs);
    }

    public ContestProblem getProblemByName(String name) throws SQLException
    {
        String query = "SELECT * FROM problemsDb WHERE name = ?";
        PreparedStatement stmt = this.sqlConnection.prepareStatement(query);
        stmt.setString(1, name);
        ResultSet rs = stmt.executeQuery();
        return getProblemFromResultSet(rs);
    }

    private ContestTeam getTeamFromResultSet(ResultSet rs) throws SQLException
    {
        if (rs == null)
        {
            return null;
        }
        if (!rs.next())
        {
            return null;
        }
        int limit = 6; // TODO: don't make the limit hard-coded
        String teamName = rs.getString("name");
        ContestDivision division = ContestDivision.valueOf(rs.getString("division"));
        ArrayList<String> emails = new ArrayList<String>();
        for (int i = 1; i <= limit; i++)
        {
            String uwu = rs.getString("email" + i);
            if (uwu != null)
            {
                emails.add(uwu);
            }
        }

        return new ContestTeam(teamName, division, emails);
    }

    public ContestTeam getTeamFromEmail(String email) throws SQLException
    {
        String query = "SELECT * FROM contestantsDb WHERE ";
        int limit = 6; // TODO: don't make the limit hard-coded
        for (int i = 1; i <= limit; i++)
        {
            query += "email" + i + " = ?";
            if (i != limit)
            {
                query += " OR ";
            }
        }
        query += ";";
        PreparedStatement stmt = this.sqlConnection.prepareStatement(query);
        for (int i = 1; i <= limit; i++)
        {
            stmt.setString(i, email);
        }
        ResultSet rs = stmt.executeQuery();
        return getTeamFromResultSet(rs);
    }

    public ContestTeam getTeamByName(String name) throws SQLException
    {
        String query = "SELECT * FROM contestantsDb WHERE name = ?";
        PreparedStatement stmt = this.sqlConnection.prepareStatement(query);
        stmt.setString(1, name);
        ResultSet rs = stmt.executeQuery();
        return getTeamFromResultSet(rs);
    }

    private long getNextSubmissionId() throws SQLException
    {
        String query = "SELECT * FROM submissionsDb ORDER BY id DESC";
        PreparedStatement stmt = this.sqlConnection.prepareStatement(query);
        ResultSet rs = stmt.executeQuery();
        if (!rs.next())
        {
            return GraderInfo.START_SUBMISSION_ID;
        }
        // rs.first();
        long id = rs.getLong("id");
        return (id < GraderInfo.START_SUBMISSION_ID) ? GraderInfo.START_SUBMISSION_ID : id + 1;
    }

    public ContestSubmission addSubmissionToQueue(long uid, String senderEmail, long date, String subject, String body)
        throws SQLException
    {
        synchronized (this.sqlConnection)
        {
            long id = this.getNextSubmissionId();
            ContestSubmission toAdd = new ContestSubmission(id, uid, senderEmail, date, subject, body,
                SubmissionState.AWAITING_PROCESSING);
            insertSubmission(toAdd);
            return toAdd;
        }
    }

    private void setStatementStringNullPossible(PreparedStatement stmt, int index, Object field) throws SQLException
    {
        if (field == null)
        {
            stmt.setNull(index, Types.VARCHAR);
        }
        else
        {
            stmt.setString(index, field.toString());
        }
    }

    private void insertSubmission(ContestSubmission submission) throws SQLException
    {
        String statement = "INSERT INTO submissionsDb(id, uid, senderEmail, submissionDate, subject, body, state, "
            + "contestDivision, teamName, problemDifficulty, programmingLanguage, miscInfo) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
        PreparedStatement stmt = this.sqlConnection.prepareStatement(statement);
        stmt.setLong(1, submission.id);
        stmt.setLong(2, submission.uid);
        stmt.setString(3, submission.senderEmail);
        stmt.setLong(4, submission.date);
        setStatementStringNullPossible(stmt, 5, submission.subject);
        setStatementStringNullPossible(stmt, 6, submission.body);
        stmt.setString(7, submission.state.toString());
        setStatementStringNullPossible(stmt, 8, submission.contestDivision);
        setStatementStringNullPossible(stmt, 9, submission.teamName);
        setStatementStringNullPossible(stmt, 10, submission.problemDifficulty);
        setStatementStringNullPossible(stmt, 11, submission.programmingLanguage);
        setStatementStringNullPossible(stmt, 12, submission.miscInfo);
        stmt.execute();
    }

    // email info should not be updatable, nor should the id of the submission
    // (for security reasons).
    public void updateSubmissionStatus(ContestSubmission submission) throws SQLException
    {
        synchronized (this.sqlConnection)
        {
            String statement = "UPDATE submissionsDb SET state = ?, contestDivision = ?, teamName = ?, problemDifficulty = ?, programmingLanguage = ?, miscInfo = ? WHERE id = ?";
            PreparedStatement stmt = this.sqlConnection.prepareStatement(statement);
            stmt.setString(1, submission.state.toString());
            setStatementStringNullPossible(stmt, 2, submission.contestDivision);
            setStatementStringNullPossible(stmt, 3, submission.teamName);
            setStatementStringNullPossible(stmt, 4, submission.problemDifficulty);
            setStatementStringNullPossible(stmt, 5, submission.programmingLanguage);
            setStatementStringNullPossible(stmt, 6, submission.miscInfo);
            stmt.setLong(7, submission.id);
            stmt.execute();
        }
    }

    private ContestSubmission getSubmissionFromResultSet(ResultSet rs) throws SQLException
    {
        if (rs == null)
        {
            return null;
        }
        if (!rs.next())
        {
            return null;
        }
        return new ContestSubmission(rs.getLong("id"), rs.getLong("uid"), rs.getString("senderEmail"),
            rs.getLong("submissionDate"), rs.getString("subject"), rs.getString("body"),
            rs.getString("state") == null ? null : SubmissionState.valueOf(rs.getString("state")),
            rs.getString("contestDivision") == null ? null : ContestDivision.valueOf(rs.getString("contestDivision")),
            rs.getString("teamName"),
            rs.getString("problemDifficulty") == null ? null
                : ProblemDifficulty.valueOf(rs.getString("problemDifficulty")),
            rs.getString("programmingLanguage") == null ? null
                : ProgrammingLanguage.valueOf(rs.getString("programmingLanguage")),
            rs.getString("miscInfo"));
    }

    public ContestSubmission getSubmissionById(long id) throws SQLException
    {
        String statement = "SELECT * FROM submissionsDb WHERE id = ?";
        PreparedStatement stmt = this.sqlConnection.prepareStatement(statement);
        stmt.setLong(1, id);
        ResultSet rs = stmt.executeQuery();
        return this.getSubmissionFromResultSet(rs);
    }

    public ContestSubmission getNextPendingSubmission() throws SQLException
    {
        synchronized (this.sqlConnection)
        {
            String query = "SELECT * FROM submissionsDb WHERE state = ? ORDER BY id ASC";
            PreparedStatement stmt = this.sqlConnection.prepareStatement(query);
            stmt.setString(1, SubmissionState.AWAITING_PROCESSING.toString());
            ResultSet rs = stmt.executeQuery();
            return this.getSubmissionFromResultSet(rs);
        }
    }

    public int getPendingSubmissionCount() throws SQLException
    {
        synchronized (this.sqlConnection)
        {
            String query = "SELECT * FROM submissionsDb WHERE state = ?";
            PreparedStatement stmt = this.sqlConnection.prepareStatement(query);
            stmt.setString(1, SubmissionState.AWAITING_PROCESSING.toString());
            ResultSet rs = stmt.executeQuery();
            int i = 0;
            while (rs.next())
            {
                i++;
            }
            return i;
        }
    }

    public boolean hasPendingSubmission() throws SQLException
    {
        return this.getPendingSubmissionCount() > 0;
    }

}