package de.qyotta.eventstore;

import static com.jayway.awaitility.Awaitility.await;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jayway.awaitility.Duration;

@SuppressWarnings("nls")
public class InMemoryEventstoreProvider {

   private static final Logger LOGGER = LoggerFactory.getLogger(InMemoryEventstoreProvider.class.getName());

   private static final String EVENTSTORE_DIR = "/tmp/eventstore/";
   private static final String EVENTSTORE_VERSION = "3.9.4";
   private static final String EVENTSTORE_OSX_NAME = "EventStore-OSS-MacOSX-v" + EVENTSTORE_VERSION;
   private static final String EVENTSTORE_MAC_DOWNLOAD = "http://download.geteventstore.com/binaries/" + EVENTSTORE_OSX_NAME + ".tar.gz";
   private static final String EVENTSTORE_UBUNTU_NAME = "EventStore-OSS-Ubuntu-14.04-v" + EVENTSTORE_VERSION;
   private static final String EVENTSTORE_UBUNTU_DOWNLOAD_URL = "http://download.geteventstore.com/binaries/" + EVENTSTORE_UBUNTU_NAME + ".tar.gz";

   private Process eventStoreProcess;

   public void installIfNeeded() {
      if (!isEventstoreInstalled()) {
         LOGGER.info("Eventstore not installed. Will install now.");
         installEventstore();
      }
   }

   private boolean isEventstoreInstalled() {
      return new File(EVENTSTORE_DIR).exists();
   }

   private void installEventstore() {
      if (isMac()) {
         download(EVENTSTORE_MAC_DOWNLOAD);
      } else if (isUnix()) {
         download(EVENTSTORE_UBUNTU_DOWNLOAD_URL);
      }

      try {
         Runtime.getRuntime()
               .exec("tar -xf /tmp/eventstore.tar.gz -C /tmp/");
         if (isMac()) {
            FileUtils.moveDirectory(new File("/tmp/" + EVENTSTORE_OSX_NAME), new File(EVENTSTORE_DIR));
         } else {
            FileUtils.moveDirectory(new File("/tmp/" + EVENTSTORE_UBUNTU_NAME), new File(EVENTSTORE_DIR));
         }
      } catch (final IOException e) {
         e.printStackTrace();
      }
   }

   private boolean isUnix() {
      final String property = System.getProperty("os.name");
      return property.contains("nix") || property.contains("nux");
   }

   private boolean isMac() {
      final String property = System.getProperty("os.name");
      return property.contains("Mac");
   }

   private void download(final String downloadUrl) {
      FileOutputStream fos = null;
      final String targetFilePath = "/tmp/eventstore.tar.gz";
      try {
         fos = new FileOutputStream(targetFilePath);

         final URL website = new URL(downloadUrl);
         final ReadableByteChannel rbc = Channels.newChannel(website.openStream());
         fos.getChannel()
               .transferFrom(rbc, 0, Long.MAX_VALUE);
      } catch (final Exception e) {
         throw new RuntimeException(e);
      } finally {
         if (fos != null) {
            try {
               fos.close();
               LOGGER.info("Downloading '" + downloadUrl + "' to '" + targetFilePath + "' done.");
            } catch (final IOException e) {
               throw new RuntimeException(e);
            }
         }
      }
   }

   public void start() {
      final long start = System.currentTimeMillis();
      installIfNeeded();
      final String command = EVENTSTORE_DIR
            + "eventstored --int-tcp-port=3334 --int-http-port=4445 --ext-tcp-port=3335 --ext-http-port=4446 --mem-db --run-projections=all --start-standard-projections=true";

      try {
         eventStoreProcess = Runtime.getRuntime()
               .exec("/usr/bin/pkill eventstored");

         System.out.println("Execute command: " + command);
         eventStoreProcess = Runtime.getRuntime()
               .exec(command);
         await().atMost(Duration.FIVE_SECONDS)
               .until(() -> {
                  return isRunning();
               });
         final long duration = System.currentTimeMillis() - start;
         System.out.println("****************************************************************************");
         System.out.println("STARTING EVENTSTORE TOOK " + duration + " MILLIES");
         System.out.println("****************************************************************************");
      } catch (final Exception e) {
         e.printStackTrace();
      }
   }

   public void stop() {
      if (eventStoreProcess != null) {
         eventStoreProcess.destroyForcibly();
      }
   }

   public boolean isRunning() {
      try {
         final HttpURLConnection connection = (HttpURLConnection) new URL("http://127.0.0.1:4446").openConnection();
         connection.setRequestMethod("HEAD");
         final int responseCode = connection.getResponseCode();
         if (responseCode == 404) {
            return false;
         }
         return true;
      } catch (final ProtocolException e) {
         return false;
      } catch (final IOException e) {
         return false;
      }
   }
}
