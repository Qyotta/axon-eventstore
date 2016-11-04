package de.qyotta.eventstore.utils;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.client.cache.HttpCacheEntry;
import org.apache.http.client.cache.HttpCacheStorage;
import org.apache.http.client.cache.HttpCacheUpdateCallback;
import org.apache.http.impl.client.cache.CacheConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("nls")
public class FileCacheStorage implements HttpCacheStorage, Closeable {
   private static final Logger LOGGER = LoggerFactory.getLogger(FileCacheStorage.class.getName());
   private final File cacheDir;

   public FileCacheStorage(final CacheConfig config, File cacheDir) {
      this.cacheDir = cacheDir;
   }

   @Override
   public HttpCacheEntry getEntry(final String url) throws IOException {
      synchronized (this) {
         return loadCacheEntry(url);
      }
   }

   @Override
   public void putEntry(final String url, final HttpCacheEntry entry) throws IOException {
      synchronized (this) {
         saveCacheEntry(url, entry);
      }
   }

   @Override
   public void removeEntry(final String url) throws IOException {
      final File cache = getCacheFile(url);
      if (cache != null && cache.exists()) {
         synchronized (this) {
            cache.delete();
         }
      }
   }

   @Override
   public void updateEntry(final String url, final HttpCacheUpdateCallback callback) throws IOException {
      synchronized (this) {
         final HttpCacheEntry existing = loadCacheEntry(url);
         final HttpCacheEntry updated = callback.update(existing);
         saveCacheEntry(url, updated);
      }
   }

   private void saveCacheEntry(String url, HttpCacheEntry entry) {
      ObjectOutputStream stream = null;
      try {
         final File cache = getCacheFile(url);
         stream = new ObjectOutputStream(new FileOutputStream(cache));
         stream.writeObject(entry);
         stream.close();
      } catch (final Exception e) {
         LOGGER.error("Faled to save cache entry " + entry, e);
      }
   }

   private HttpCacheEntry loadCacheEntry(String url) {
      HttpCacheEntry entry = null;
      final File cache = getCacheFile(url);
      if (cache != null && cache.exists()) {
         synchronized (this) {
            ObjectInputStream stream = null;
            try {
               stream = new ObjectInputStream(new FileInputStream(cache));
               entry = (HttpCacheEntry) stream.readObject();
               stream.close();
            } catch (final Exception e) {
               LOGGER.error("Faled to load cache entry " + entry, e);
            }
         }
      }
      return entry;
   }

   private File getCacheFile(String url) {
      return new File(cacheDir, DigestUtils.sha256Hex(url));
   }

   @Override
   public void close() throws IOException {
      //
   }
}
