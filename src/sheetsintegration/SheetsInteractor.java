package sheetsintegration;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;

import graderobjects.ContestDivision;

public class SheetsInteractor
{
    private Sheets sheetsService;
    private String spreadsheetId;

    public SheetsInteractor(Sheets sheetsService, String spreadsheetId)
    {
        this.sheetsService = sheetsService;
        this.spreadsheetId = spreadsheetId;
    }

    public String getCellRange(String teamName, ContestDivision division, int problemAbsoluteId) throws IOException
    {
        if (teamName == null || division == null)
        {
            return null;
        }

        String rangePart0 = "";
        if (division == ContestDivision.ADVANCED)
        {
            rangePart0 = "Advanced";
        }
        else if (division == ContestDivision.INTERMEDIATE)
        {
            rangePart0 = "Intermediate";
        }

        String rangePart1 = "A2:A75";

        final String searchRange = rangePart0 + "!" + rangePart1;

        ValueRange response = this.sheetsService.spreadsheets().values().get(this.spreadsheetId, searchRange).execute();
        List<List<Object>> values = response.getValues();
        if (values == null || values.isEmpty())
        {
            return null;
        }
        else
        {
            for (int i = 0; i < values.size(); i++)
            {
                List<Object> row = values.get(i);
                String curTeam = row.get(0).toString();
                if (teamName.equals(curTeam))
                {
                    int rowActual = i + 2;
                    int colNumber = problemAbsoluteId;
                    char colLetter = (char) ('A' + colNumber);
                    return rangePart0 + "!" + colLetter + "" + rowActual;
                }
            }
        }
        return null;
    }

    public UpdateValuesResponse writeCellValue(String cellRange, String toWrite) throws IOException
    {
        if (cellRange == null || toWrite == null)
        {
            return null;
        }

        ValueRange body = new ValueRange().setValues(Arrays.asList(Arrays.asList(toWrite)));
        UpdateValuesResponse result = this.sheetsService.spreadsheets().values()
            .update(this.spreadsheetId, cellRange, body).setValueInputOption("RAW").execute();
        return result;
    }
    
    public UpdateValuesResponse writeCellValue(String cellRange, Integer toWrite) throws IOException
    {
        if (cellRange == null || toWrite == null)
        {
            return null;
        }

        ValueRange body = new ValueRange().setValues(Arrays.asList(Arrays.asList(toWrite)));
        UpdateValuesResponse result = this.sheetsService.spreadsheets().values()
            .update(this.spreadsheetId, cellRange, body).setValueInputOption("RAW").execute();
        return result;
    }

    // this method was intended to be a test public method. don't play with this
    // method. lol
    @SuppressWarnings("unused")
    private void doOwO() throws IOException
    {
        final String searchRange = "Intermediate!A2:A75";

        ValueRange response = this.sheetsService.spreadsheets().values().get(spreadsheetId, searchRange).execute();
        List<List<Object>> values = response.getValues();
        if (values == null || values.isEmpty())
        {
            return;
        }
        else
        {
            for (int i = 0; i < values.size(); i++)
            {
                List<Object> row = values.get(i);
                String curTeam = row.get(0).toString();
                System.out.println(curTeam);
            }
        }
    }

}
