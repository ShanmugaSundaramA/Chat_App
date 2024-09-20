package com.ws.chat.azureblob;

import java.net.URISyntaxException;
import java.security.InvalidKeyException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class AzureStorageBlobConfig {

     @Value("${azure.storage.ConnectionString}")
     private String azureConnectionString;
     @Value("${azure.storage.container.name}")
     private String azureContainerName;

     @Bean
     CloudBlobClient cloudBlobClient() throws URISyntaxException,
               InvalidKeyException {

          CloudStorageAccount storageAccount = CloudStorageAccount
                    .parse(azureConnectionString);
          return storageAccount.createCloudBlobClient();
     }

     @Bean
     CloudBlobContainer testBlobContainer() throws URISyntaxException,
               StorageException,
               InvalidKeyException {

          return cloudBlobClient().getContainerReference(azureContainerName);
     }

}
