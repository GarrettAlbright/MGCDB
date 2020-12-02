package pro.albright.mgcdb.Util;

import java.io.*;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TestSteamCxn extends SteamCxn {

  /**
   * The directory mock response files are stored in.
   */
  private Path testDataDir;

  public TestSteamCxn() {
    super("test");
    testDataDir = Paths.get("test-data").toAbsolutePath();
  }

  public TestSteamCxn(String steamKey) {
    super("test");
    testDataDir = Paths.get("test-data").toAbsolutePath();
  }

  /**
   * Load a mock response file instead of making a network request.
   * @param uri The URI to make the request to.
   * @return The response data.
   */
  @Override
  protected String fetchRawResponseBody(URI uri) {
    String filename = uriToLogFilename(uri);
    File bodyFile = new File(testDataDir.toFile(), filename);
    if (bodyFile.isFile() && bodyFile.canRead()) {
      try {
        // https://stackoverflow.com/a/14169760/11023
        FileInputStream fis = new FileInputStream(bodyFile);
        int fileLength = (int) bodyFile.length();
        byte[] data = new byte[fileLength];
        fis.read(data);
        fis.close();
        return new String(data, "UTF-8");
      }
      catch (Exception e) {
        e.printStackTrace();
        System.exit(StatusCodes.MOCK_RESPONSE_FAILED);
      }
    }
    System.err.printf("Mock response file %s doesn't exist or isn't readable%n", filename);
    System.exit(StatusCodes.MOCK_RESPONSE_FAILED);
    return null;
  }
}
