package com.ws.chat.azureblob;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;

import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.blob.ListBlobItem;
import com.microsoft.azure.storage.blob.SharedAccessBlobPermissions;
import com.microsoft.azure.storage.blob.SharedAccessBlobPolicy;

@Service
@Slf4j
@RequiredArgsConstructor
public class AzureBlobAdapter {

     private final CloudBlobContainer cloudBlobContainer;
     private final CloudBlobClient cloudBlobClient;
     @Value("${azure.storage.sasTokenDuration}")
     private int sasTokenDuration;
     private static final String IMAGE = "https://packagingapp.blob.core.windows.net/trove/20231024153135_no_image.jpg";

     public String generateContainerSasToken() throws StorageException, URISyntaxException {
          try {
               SharedAccessBlobPolicy sharedAccessPolicy = new SharedAccessBlobPolicy();
               sharedAccessPolicy.setPermissions(
                         EnumSet.of(
                                   SharedAccessBlobPermissions.READ,
                                   SharedAccessBlobPermissions.LIST));
               sharedAccessPolicy.setSharedAccessExpiryTime(
                         Date.from(Instant.now().plusSeconds(sasTokenDuration)));
               return cloudBlobContainer.generateSharedAccessSignature(sharedAccessPolicy, null);
          } catch (InvalidKeyException | StorageException e) {
               log.info(e.getMessage());
               return null;
          }
     }

     public String upload(MultipartFile multipartFile) {
          CloudBlockBlob blob = null;
          String fileName = null;
          try {
               DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
               LocalDateTime currentTime = LocalDateTime.now();
               String formattedTime = currentTime.format(formatter);
               fileName = formattedTime + "_" + multipartFile.getOriginalFilename();
               blob = cloudBlobContainer.getBlockBlobReference(fileName);
               blob.upload(multipartFile.getInputStream(), -1);
          } catch (URISyntaxException | IOException | StorageException e) {
               log.info(e.getMessage());
          }
          return fileName;
     }

     public List<URI> listBlobs(String containerName) {
          List<URI> uris = new ArrayList<>();
          try {
               CloudBlobContainer container = cloudBlobClient.getContainerReference(containerName);
               for (ListBlobItem blobItem : container.listBlobs()) {
                    uris.add(blobItem.getUri());
               }
          } catch (URISyntaxException | StorageException e) {
               log.info(e.getMessage());
          }
          return uris;
     }

     public URI getBlobUri(String blobName) {
          try {
               String sasToken = generateContainerSasToken();
               if (blobName == null || blobName.isEmpty()) {
                    return new URI(IMAGE + "?" + sasToken);
               }
               String containerName = "trove";
               CloudBlobContainer container = cloudBlobClient.getContainerReference(containerName);
               CloudBlockBlob blob = container.getBlockBlobReference(blobName);
               if (blob.exists()) {
                    return new URI(blob.getUri() + "?" + sasToken);
               } else {
                    return new URI(IMAGE + "?" + sasToken);
               }
          } catch (URISyntaxException | StorageException e) {
               log.info(e.getMessage());
          }
          return null;
     }

}
