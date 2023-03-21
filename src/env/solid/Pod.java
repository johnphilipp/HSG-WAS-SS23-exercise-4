package solid;

import cartago.Artifact;
import cartago.OPERATION;
import cartago.OpFeedbackParam;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * A CArtAgO artifact that agent can use to interact with LDP containers in a Solid pod.
 */
public class Pod extends Artifact {

    private String podURL; // the location of the Solid pod 

  /**
   * Method called by CArtAgO to initialize the artifact. 
   *
   * @param podURL The location of a Solid pod
   */
    public void init(String podURL) {
        this.podURL = podURL;
        log("Pod artifact initialized for: " + this.podURL);
    }

  /**
   * CArtAgO operation for creating a Linked Data Platform container in the Solid pod
   *
   * @param containerName The name of the container to be created
   * 
   */
    @OPERATION
    public void createContainer(String containerName) {
        String body = "@prefix ldp: <http://www.w3.org/ns/ldp#>.\n"
                + "@prefix dcterms: <http://purl.org/dc/terms/>.\n"
                + "<> a ldp:Container, ldp:BasicContainer, ldp:Resource;\n"
                + "dcterms:title \"" + containerName + "\";\n"
                + "dcterms:description \"My Container\".";

        HttpRequest createContainerRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://solid.interactions.ics.unisg.ch/philipp/"))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "text/turtle")
                .header("Link", "<http://www.w3.org/ns/ldp#BasicContainer>; rel=\"type\"")
                .header("Slug", containerName + "/")
                .build();

        HttpClient httpClient = HttpClient.newHttpClient();

        try {
            HttpResponse<String> response = httpClient.send(createContainerRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 201) {
                log("Container created.");
            } else {
                log("There is a problem, response status: " + response.body());
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to create container.", e);
        }
    }

    @OPERATION
    public void publishData(String containerName, String fileName, Object[] data) throws IOException, InterruptedException {


        final var stringInput = createStringFromArray(data);
        final var newData = URI.create(("https://solid.interactions.ics.unisg.ch/philipp/" + containerName + "/" + fileName ));
        final var httpClient = HttpClient.newHttpClient();
        try {
            if (checkResourceExists(newData)) {
                final var request = HttpRequest.newBuilder()
                        .uri(newData)
                        .header("Content-Type", "text/plain")
                        .PUT(HttpRequest.BodyPublishers.ofString(stringInput))
                        .build();
                final var httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());


            } else {
                final var containerUri = URI.create(("https://solid.interactions.ics.unisg.ch/philipp/" + containerName + "/"));
                final var request = HttpRequest.newBuilder(containerUri)
                        .header("Slug", fileName)
                        .POST(HttpRequest.BodyPublishers.ofString(stringInput))
                        .header("Content-Type", "text/plain")
                        .build();
                final var httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.discarding());


            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    private boolean checkResourceExists(URI resourceUri) {
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(resourceUri)
                .GET()
                .build();
        try {
            HttpResponse<Void> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.discarding());
            return response.statusCode() == 200;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to check if new data exists.", e);
        }
    }




    /**
   * CArtAgO operation for reading data of a .txt file in a Linked Data Platform container of the Solid pod
   * 
   * @param containerName The name of the container where the .txt file resource is located
   * @param fileName The name of the .txt file resource that holds the data to be read
   * @param data An array whose elements are the data read from the .txt file
   */
    @OPERATION
    public void readData(String containerName, String fileName, OpFeedbackParam<Object[]> data) throws IOException, InterruptedException {
        data.set(readData(containerName, fileName));
    }

  /**
   * Method for reading data of a .txt file in a Linked Data Platform container of the Solid pod
   * 
   * @param containerName The name of the container where the .txt file resource is located
   * @param fileName The name of the .txt file resource that holds the data to be read
   * @return An array whose elements are the data read from the .txt file
   */


  public String[] readData(String containerName, String fileName) {
      String uri = "https://solid.interactions.ics.unisg.ch/philipp/" + containerName + "/" + fileName;

      HttpRequest getRequest = HttpRequest.newBuilder()
              .uri(URI.create(uri))
              .GET()
              .build();

      HttpClient client = HttpClient.newHttpClient();

      try {
          HttpResponse<String> response = client.send(getRequest, HttpResponse.BodyHandlers.ofString());
          String responseBody = response.body();
          String[] data = responseBody.split("\n");
          log("Data read from " + uri);
          return data;
      } catch (IOException | InterruptedException e) {
          throw new RuntimeException("Failed to read data from " + uri, e);
      }
  }

  /**
   * Method that converts an array of Object instances to a string, 
   * e.g. the array ["one", 2, true] is converted to the string "one\n2\ntrue\n"
   *
   * @param array The array to be converted to a string
   * @return A string consisting of the string values of the array elements separated by "\n"
   */
    public static String createStringFromArray(Object[] array) {
        StringBuilder sb = new StringBuilder();
        for (Object obj : array) {
            sb.append(obj.toString()).append("\n");
        }
        return sb.toString();
    }

  /**
   * Method that converts a string to an array of Object instances computed by splitting the given string with delimiter "\n"
   * e.g. the string "one\n2\ntrue\n" is converted to the array ["one", "2", "true"]
   *
   * @param str The string to be converted to an array
   * @return An array consisting of string values that occur by splitting the string around "\n"
   */
    public static Object[] createArrayFromString(String str) {
        return str.split("\n");
    }


  /**
   * CArtAgO operation for updating data of a .txt file in a Linked Data Platform container of the Solid pod
   * The method reads the data currently stored in the .txt file and publishes in the file the old data along with new data 
   * 
   * @param containerName The name of the container where the .txt file resource is located
   * @param fileName The name of the .txt file resource that holds the data to be updated
   * @param data An array whose elements are the new data to be added in the .txt file
   */
    @OPERATION
    public void updateData(String containerName, String fileName, Object[] data) throws IOException, InterruptedException {
        Object[] oldData = readData(containerName, fileName);
        Object[] allData = new Object[oldData.length + data.length];
        System.arraycopy(oldData, 0, allData, 0, oldData.length);
        System.arraycopy(data, 0, allData, oldData.length, data.length);
        publishData(containerName, fileName, allData);
    }
}