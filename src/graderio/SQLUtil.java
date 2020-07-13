package graderio;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import emailgrader.GraderInfo;
import graderobjects.ContestDivision;
import graderobjects.ContestSubmission;
import graderobjects.ProblemDifficulty;
import graderobjects.ProgrammingLanguage;
import graderobjects.SubmissionState;

public class SQLUtil
{
    private Connection sqlConnection = null;

    public SQLUtil(String filePath) throws SQLException
    {
        boolean needReinit = false;
        if (sqlConnection == null)
        {
            needReinit = true;
        }
        else
        {
            if (sqlConnection.isClosed())
            {
                needReinit = true;
            }
        }
        if (needReinit)
        {
            try
            {
                Class.forName("org.sqlite.JDBC");
            }
            catch (ClassNotFoundException cnfe)
            {
                cnfe.printStackTrace();
            }

            sqlConnection = DriverManager.getConnection("jdbc:sqlite:" + filePath);
        }
        initTables();
    }

    public void initTables() throws SQLException
    {
        DatabaseMetaData dbm = this.sqlConnection.getMetaData();
        ResultSet tables = dbm.getTables(null, null, "submissionsDb", null);
        if (!tables.next())
        {
            String query = "CREATE TABLE submissionsDb(id INT, uid INT, senderEmail TEXT, submissionDate INT, "
                + "subject TEXT NULL, body TEXT NULL, state TEXT, contestDivision TEXT NULL, teamName TEXT NULL,"
                + " problemDifficulty TEXT NULL, programmingLanguage TEXT NULL, miscInfo TEXT NULL);";
            PreparedStatement stmt = sqlConnection.prepareStatement(query);
            stmt.execute();
        }
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
        if (!rs.next())
        {
            return null;
        }
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
            if (!rs.next())
            {
                return null;
            }
            // rs.first();
            return getSubmissionFromResultSet(rs);
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