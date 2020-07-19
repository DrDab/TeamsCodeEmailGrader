package sheetsintegration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;

public class SheetsAuthUtil
{
    private String applicationName;
    private String credentialJsonFileLocation;
    private String tokensDirectoryPath;
    private JsonFactory jsonFactory;
    private List<String> scopes;
    private NetHttpTransport httpTransport;

    public SheetsAuthUtil(String applicationName, String credentialJsonFileLocation, String tokensDirectoryPath) throws GeneralSecurityException, IOException
    {
        this.applicationName = applicationName;
        this.credentialJsonFileLocation = credentialJsonFileLocation;
        this.tokensDirectoryPath = tokensDirectoryPath;
        this.jsonFactory = JacksonFactory.getDefaultInstance();
        this.scopes = Collections.singletonList(SheetsScopes.SPREADSHEETS_READONLY);
        this.httpTransport = GoogleNetHttpTransport.newTrustedTransport();
    }

    public Credential getCredentials() throws IOException
    {
        // Load client secrets.
        InputStream in = new FileInputStream(credentialJsonFileLocation);
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(this.jsonFactory, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(this.httpTransport, this.jsonFactory,
            clientSecrets, this.scopes).setDataStoreFactory(new FileDataStoreFactory(new File(this.tokensDirectoryPath)))
                .setAccessType("offline").build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }
    
    public Sheets getSheetsService(Credential credentials)
    {
        Sheets service = new Sheets.Builder(this.httpTransport, this.jsonFactory, credentials)
            .setApplicationName(this.applicationName)
            .build();
        return service;
    }
    
    @SuppressWarnings("rawtypes")
    public void test() throws IOException, GeneralSecurityException
    {
        final String spreadsheetId = "1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms";
        final String range = "Class Data!A2:E";
        
        Credential credentials = this.getCredentials();
        Sheets service = this.getSheetsService(credentials);
        
        ValueRange response = service.spreadsheets().values()
                .get(spreadsheetId, range)
                .execute();
        List<List<Object>> values = response.getValues();
        if (values == null || values.isEmpty()) {
            System.out.println("No data found.");
        } else {
            System.out.println("Name, Major");
            for (List row : values) {
                // Print columns A and E, which correspond to indices 0 and 4.
                System.out.printf("%s, %s\n", row.get(0), row.get(4));
            }
        }
    }
}
