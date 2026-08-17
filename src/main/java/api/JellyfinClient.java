package api;

import api.dto.AuthRequest;
import api.dto.AuthResponse;
import api.dto.ItemsResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import config.Config;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;

public class JellyfinClient {

     private final UUID uuid = UUID.randomUUID();
     private final HttpClient client = HttpClient.newBuilder()
             .connectTimeout(Duration.ofSeconds(10))
             .build();
     private final ObjectMapper mapper;
     private String serverUrl;
     private String accessToken;
     private String userId;

     public JellyfinClient() {
          this.mapper = new ObjectMapper();
     }

     private String generateAuthHeader() {
          return "MediaBrowser Client=\"jellyfin-cli\", Device=\"cli\", DeviceId=\"%s\", Version=\"0.1.0\"".formatted(uuid.toString());
     }

     public AuthResponse auth(String username, String password, String serverUrl) throws IOException, InterruptedException {
          AuthRequest credentials = new AuthRequest(username, password);
          try {
               HttpRequest req = HttpRequest.newBuilder()
                       .uri(URI.create(serverUrl + "/Users/AuthenticateByName"))
                       .header("Accept", "application/json")
                       .header("Content-Type", "application/json")
                       .header("Authorization", generateAuthHeader())
                       .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(credentials)))
                       .timeout(Duration.ofSeconds(30))
                       .build();
               HttpResponse<String> response = client.send(req, HttpResponse.BodyHandlers.ofString());
               int statusCode = response.statusCode();
               if (statusCode >= 200 && statusCode < 300) {
                    AuthResponse auth = mapper.readValue(response.body(), AuthResponse.class);
                    this.accessToken = auth.accessToken();
                    this.userId = auth.user().id();
                    this.serverUrl = serverUrl;
                    return auth;
               }
          } catch (java.net.ConnectException | java.net.UnknownHostException | java.net.http.HttpConnectTimeoutException e) {
               System.out.println("Could not reach server at " + serverUrl + ". Check the server URL and your network connection.");
          } catch (Exception e) {
               // TODO: implement better logging
               e.printStackTrace();
          }
          return null;
     }

     public ItemsResponse getLibraries() {
          // * URL: serverUrl + "/Users/" + userId + "/Views"
          if (this.accessToken == null || this.accessToken.isEmpty()) {
               System.out.println("Must call jellyfinAuth() first");
               return null;
          }
          try {
               HttpRequest req = HttpRequest.newBuilder()
                       .uri(URI.create(serverUrl + "/Users/" + this.userId + "/Views"))
                       .header("Accept", "application/json")
                       .header("X-Emby-Token", this.accessToken)
                       .GET()
                       .timeout(Duration.ofSeconds(30))
                       .build();

               HttpResponse<String> response = client.send(req, HttpResponse.BodyHandlers.ofString());
               int statusCode = response.statusCode();
               if (statusCode >= 200 && statusCode < 300) {
                    ItemsResponse itemsResponse = mapper.readValue(response.body(), ItemsResponse.class);
                    return itemsResponse;
               }
          } catch (Exception e) {
               // TODO: implement better logging
               e.printStackTrace();
          }
          return null;
     }

     public ItemsResponse getItems(String parentId) {
          if (this.accessToken == null || accessToken.isEmpty()) {
               System.out.println("Must call jellyfinAuth() first");
               return null;
          }

          try {
               HttpRequest req = HttpRequest.newBuilder()
                       .uri(URI.create(serverUrl + "/Users/" + this.userId + "/Items?ParentId=" + parentId + "&SortBy=IndexNumber&SortOrder=Ascending"))
                       .header("Accept", "application/json")
                       .header("X-Emby-Token", this.accessToken)
                       .GET()
                       .timeout(Duration.ofSeconds(30))
                       .build();

               HttpResponse<String> response = client.send(req, HttpResponse.BodyHandlers.ofString());
               int statusCode = response.statusCode();
               if (statusCode >= 200 && statusCode < 300) {
                    ItemsResponse itemsResponse = mapper.readValue(response.body(), ItemsResponse.class);
                    return itemsResponse;
               }
          } catch (Exception e) {
               // TODO: implement better logging
               e.printStackTrace();
          }
          return null;
     }

     public boolean healthCheck(String serverUrl, String accessToken) {
          // point of this function is to check if the config stored on disk is fine
          try {
               HttpRequest req = HttpRequest.newBuilder()
                       .uri(URI.create(serverUrl + "/Users/Me")) // server url passed in from client, not this object's field
                       .header("Accept", "application/json")
                       .header("X-Emby-Token", accessToken) // access token from client, not this object's field
                       .GET()
                       .timeout(Duration.ofSeconds(30))
                       .build();

               HttpResponse<String> response = client.send(req, HttpResponse.BodyHandlers.ofString());
               int statusCode = response.statusCode();
               if (statusCode >= 200 && statusCode < 300) {
                    return true;
               }
          } catch (Exception e) {
               e.printStackTrace();
          }
          return false;
     }

     public void loadSession(Config config) {
          this.accessToken = config.accessToken();
          this.serverUrl = config.serverUrl();
          this.userId = config.userId();
     }

     public String getStreamUrl(String itemId) {
          if (this.accessToken == null || this.accessToken.isEmpty()) {
               System.out.println("Must call jellyfinAuth() first");
               return null;
          }
          return serverUrl + "/Videos/" + itemId + "/stream?static=true&api_key=" + accessToken;
     }
}
