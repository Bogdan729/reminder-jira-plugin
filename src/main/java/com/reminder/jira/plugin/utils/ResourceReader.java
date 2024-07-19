package com.reminder.jira.plugin.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ResourceReader {
   public String readResourceAsString(String resourcePath) {
      InputStream inputStream = this.getClass().getResourceAsStream(resourcePath);
      if (inputStream == null) {
         throw new IllegalArgumentException("Resource not found: " + resourcePath);
      } else {
         try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            String var6;
            try {
               StringBuilder content = new StringBuilder();

               String line;
               while((line = reader.readLine()) != null) {
                  content.append(line).append("\n");
               }

               var6 = content.toString();
            } catch (Throwable var8) {
               try {
                  reader.close();
               } catch (Throwable var7) {
                  var8.addSuppressed(var7);
               }

               throw var8;
            }

            reader.close();
            return var6;
         } catch (IOException var9) {
            throw new RuntimeException(var9);
         }
      }
   }
}
