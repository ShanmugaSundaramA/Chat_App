package com.ws.chat.azureblob;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.URISyntaxException;
import java.security.InvalidKeyException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;

@ExtendWith(MockitoExtension.class)
class AzureStorageBlobConfigTest {

     @InjectMocks
     private AzureStorageBlobConfig azureStorageBlobConfig;

     @Mock
     private CloudBlobClient cloudBlobClient;

     @Mock
     private CloudBlobContainer cloudBlobContainer;

     @Test
     void testCloudBlobClient() throws URISyntaxException, InvalidKeyException {
          assertNotNull(cloudBlobClient, "CloudBlobClient should not be null");
     }

     @Test
     void testCloudBlobContainer() throws URISyntaxException, StorageException, InvalidKeyException {
          assertNotNull(cloudBlobContainer, "CloudBlobContainer should not be null");
     }

     @Test
     void testAzureStorageBlobConfig() {
          assertNotNull(azureStorageBlobConfig, "AzureStorageBlobConfig should not be null");
     }
}
