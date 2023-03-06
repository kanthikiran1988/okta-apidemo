import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import org.json.JSONArray;
import org.json.JSONObject;

public class ApplicationReport {
    private static final String ORG = "test-admin.oktapreview.com";
    private static final String API_KEY = "your_api_key_here";
    private static final String FILE_NAME = "ApplicationReporting.csv";
    private static final String HEADER = "App Id,Embed Link";
    private static final int MAX_APPS = 100;
    
    public static void main(String[] args) {
        try {
            // Create CSV file and write header
            File file = new File(FILE_NAME);
            FileWriter fileWriter = new FileWriter(file);
            PrintWriter printWriter = new PrintWriter(fileWriter);
            printWriter.println(HEADER);
            
            String url = String.format("https://%s/api/v1/apps?limit=%d", ORG, MAX_APPS);
            String nextPage = url;
            
            while (nextPage != null) {
                // Make HTTP GET request to Okta API
                URL obj = new URL(nextPage);
                HttpURLConnection con = (HttpURLConnection) obj.openConnection();
                con.setRequestMethod("GET");
                con.setRequestProperty("Authorization", "SSWS " + API_KEY);
                con.setRequestProperty("Content-Type", "application/json");
                
                // Read response
                String response = "";
                Scanner scanner = new Scanner(con.getInputStream(), StandardCharsets.UTF_8.name());
                while (scanner.hasNextLine()) {
                    response += scanner.nextLine();
                }
                scanner.close();
                
                // Extract app ID and identity provider from JSON response
                JSONArray apps = new JSONArray(response);
                for (int i = 0; i < apps.length(); i++) {
                    JSONObject app = apps.getJSONObject(i);
                    String appId = app.getString("id");
                    String appUrl = app.getString("href");
                    String appIdentityProvider = app.getJSONObject("settings")
                        .getJSONObject("app").getString("identityProviderArn");
                    
                    // Write to CSV file
                    String csvInput = String.format("\"%s\",\"%s\"", appId, appIdentityProvider);
                    printWriter.println(csvInput);
                    
                    // Make HTTP GET request for detailed app info
                    obj = new URL(appUrl);
                    con = (HttpURLConnection) obj.openConnection();
                    con.setRequestMethod("GET");
                    con.setRequestProperty("Authorization", "SSWS " + API_KEY);
                    con.setRequestProperty("Content-Type", "application/json");
                    
                    // Read response
                    response = "";
                    scanner = new Scanner(con.getInputStream(), StandardCharsets.UTF_8.name());
                    while (scanner.hasNextLine()) {
                        response += scanner.nextLine();
                    }
                    scanner.close();
                    
                    // Do something with detailed app info
                    JSONObject appDetails = new JSONObject(response);
                    // ...
                }
                
                // Check for next page of apps
                nextPage = null;
                String linkHeader = con.getHeaderField("Link");
                if (linkHeader != null) {
                    String[] links = linkHeader.split(", ");
                    for (String link : links) {
                        String[] parts = link.split("; ");
                        String rel = parts[1].substring(5, parts[1].length() - 1);
                        if (rel.equals("next")) {
                            nextPage = parts[0].substring(1, parts[0].length() - 1);
                            break;
                        }
                    }
                }
            }
            
            printWriter.close();
            System.out.println("Report generated successfully.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
