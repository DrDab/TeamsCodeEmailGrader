package sheetsintegration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
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

public class SheetsAuthUtil
{
    private String applicationName;
    private String credentialJsonFileLocation;
    private String tokensDirectoryPath;
    private JsonFactory jsonFactory;
    private List<String> scopes;
    private NetHttpTransport httpTransport;

    public SheetsAuthUtil(String applicationName, String credentialJsonFileLocation, String tokensDirectoryPath,
        boolean readOnly) throws GeneralSecurityException, IOException
    {
        this.applicationName = applicationName;
        this.credentialJsonFileLocation = credentialJsonFileLocation;
        this.tokensDirectoryPath = tokensDirectoryPath;
        this.jsonFactory = JacksonFactory.getDefaultInstance();
        this.scopes = new ArrayList<String>();
        if (readOnly)
        {
            scopes.add(SheetsScopes.SPREADSHEETS_READONLY);
        }
        else
        {
            scopes.add(SheetsScopes.SPREADSHEETS);
            scopes.add(SheetsScopes.DRIVE);
            scopes.add(SheetsScopes.DRIVE_FILE);
        }
        this.httpTransport = GoogleNetHttpTransport.newTrustedTransport();
    }

    public Credential getCredentials() throws IOException
    {
        // Load client secrets.
        InputStream in = new FileInputStream(credentialJsonFileLocation);
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(this.jsonFactory, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(this.httpTransport, this.jsonFactory,
            clientSecrets, this.scopes)
                .setDataStoreFactory(new FileDataStoreFactory(new File(this.tokensDirectoryPath)))
                .setAccessType("offline").build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    public Sheets getSheetsService(Credential credentials)
    {
        Sheets service = new Sheets.Builder(this.httpTransport, this.jsonFactory, credentials)
            .setApplicationName(this.applicationName).build();
        return service;
    }
}
