package com.ws.chat.azureblob;

import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.blob.ListBlobItem;

@ExtendWith(MockitoExtension.class)
class AzureBlobAdapterTest {

     @Mock
     private CloudBlobContainer cloudBlobContainer;

     @Mock
     private CloudBlobClient cloudBlobClient;

     @InjectMocks
     private AzureBlobAdapter azureBlobAdapter;

     MultipartFile multipartFile = new MockMultipartFile(
               "profile.jpg",
               "profile.jpg",
               "image/jpeg",
               new byte[10]);

     @Test
     void generateContainerSasTokenTest() throws StorageException, URISyntaxException, InvalidKeyException {

          when(cloudBlobContainer.generateSharedAccessSignature(any(), any())).thenReturn("mockSasToken");
          String sasToken = azureBlobAdapter.generateContainerSasToken();
          assertNotNull(sasToken);
     }

     @Test
     void uploadTest() throws URISyntaxException, StorageException {

          CloudBlockBlob cloudBlockBlob = mock(CloudBlockBlob.class);
          when(cloudBlobContainer.getBlockBlobReference(any())).thenReturn(cloudBlockBlob);
          String fileName = azureBlobAdapter.upload(multipartFile);

          assertNotNull(fileName);
     }

     @Test
     void listBlobsTest() throws StorageException, URISyntaxException {
          List<ListBlobItem> uris = new ArrayList<>();
          ListBlobItem mockBlobItem = mock(ListBlobItem.class);
          uris.add(mockBlobItem);

          CloudBlobContainer container = mock(CloudBlobContainer.class);
          when(cloudBlobClient.getContainerReference(any())).thenReturn(container);
          when(container.listBlobs()).thenReturn(uris);
          List<URI> result = azureBlobAdapter.listBlobs("containerName");
          assertNotNull(result);
     }

     @Test
     void getBlobUriTest() throws URISyntaxException, StorageException {

          CloudBlobContainer container = mock(CloudBlobContainer.class);
          CloudBlockBlob cloudBlockBlob = mock(CloudBlockBlob.class);

          when(cloudBlobClient.getContainerReference(any())).thenReturn(container);
          when(container.getBlockBlobReference(any())).thenReturn(cloudBlockBlob);
          when(cloudBlockBlob.exists()).thenReturn(true);
          URI result = azureBlobAdapter.getBlobUri("1234_profile.jpg");
          assertNotNull(result);

     }

     @Test
     void getBlobUriReturnNoImageTest() throws URISyntaxException, StorageException {

          CloudBlobContainer container = mock(CloudBlobContainer.class);
          CloudBlockBlob cloudBlockBlob = mock(CloudBlockBlob.class);

          when(cloudBlobClient.getContainerReference(any())).thenReturn(container);
          when(container.getBlockBlobReference(any())).thenReturn(cloudBlockBlob);
          when(cloudBlockBlob.exists()).thenReturn(false);
          URI result = azureBlobAdapter.getBlobUri("1234_profile.jpg");
          assertNotNull(result);

     }

     @Test
     void generateContainerSasTokenExceptionTest() throws StorageException, URISyntaxException, InvalidKeyException {

          when(cloudBlobContainer.generateSharedAccessSignature(any(), any()))
                    .thenThrow(new InvalidKeyException("Invalid key"));

          String sasToken = azureBlobAdapter.generateContainerSasToken();
          assertNull(sasToken);
     }

     @Test
     void uploadExceptionTest() throws URISyntaxException, StorageException {

          when(cloudBlobContainer.getBlockBlobReference(anyString()))
                    .thenThrow(new StorageException("Error", "Upload error", 500, null, null));
          String fileName = azureBlobAdapter.upload(multipartFile);
          assertNotNull(fileName);
     }

     @Test
     void listBlobsExceptionTest() throws URISyntaxException, StorageException {

          when(cloudBlobClient.getContainerReference(anyString()))
                    .thenThrow(new URISyntaxException("URI", "Syntax error"));

          List<URI> uris = azureBlobAdapter.listBlobs("containerName");
          assertTrue(uris.isEmpty());
     }

     @Test
     void getBlobUriNullBlobNameExceptionTest() throws StorageException, InvalidKeyException {

          when(cloudBlobContainer.generateSharedAccessSignature(
                    any(),
                    any()))
                    .thenThrow(new StorageException("Error", "Access error", 500, null, null));

          URI uri = azureBlobAdapter.getBlobUri(null);
          assertNotNull(uri);
     }

     @Test
     void getBlobUriInvalidBlobNameExceptionTest() throws StorageException, URISyntaxException {

          when(cloudBlobClient.getContainerReference(anyString()))
                    .thenThrow(new StorageException("Error", "Container access error", 500, null, null));

          URI uri = azureBlobAdapter.getBlobUri("invalidBlobName");
          assertNull(uri);
     }
}
