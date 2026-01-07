package com.ntu.cloudgui.aggservice;

// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.mockito.InjectMocks;
// import org.mockito.Mock;
// import org.mockito.MockitoAnnotations;

// import static org.junit.jupiter.api.Assertions.*;
// import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.ArgumentMatchers.anyInt;
// import static org.mockito.ArgumentMatchers.anyLong;
// import static org.mockito.Mockito.*;

// import java.sql.SQLException;
// import java.util.ArrayList;
// import java.util.List;

// class FileProcessingServiceTest {

//     @Mock
//     private Configuration config;
//     @Mock
//     private EncryptionService encryptionService;
//     @Mock
//     private ChunkStorageService chunkStorageService;
//     @Mock
//     private CrcValidationService crcValidationService;
//     @Mock
//     private FileMetadataRepository fileMetadataRepository;
//     @Mock
//     private ChunkMetadataRepository chunkMetadataRepository;
//     @Mock
//     private DatabaseLoggingService loggingService;

//     @InjectMocks
//     private FileProcessingService fileProcessingService;

//     @BeforeEach
//     void setUp() {
//         MockitoAnnotations.openMocks(this);
//         // Provide a default chunk size
//         when(config.getChunkSize()).thenReturn(1024);
//     }

//     @Test
//     void testProcessAndStoreFile_Success() throws ProcessingException, SQLException {
//         // Arrange
//         String filename = "test.txt";
//         String username = "testuser";
//         byte[] fileData = "This is a test file.".getBytes();
//         byte[] encryptedData = "encrypted_data".getBytes();

//         when(encryptionService.encrypt(any(byte[].class))).thenReturn(encryptedData);
//         when(crcValidationService.calculateCrc32(any(byte[].class))).thenReturn(12345L);
//         when(chunkStorageService.storeChunk(any(byte[].class), anyLong(), anyInt())).thenReturn("fileserver1");
//         doNothing().when(fileMetadataRepository).save(any(FileMetadata.class));
//         doNothing().when(chunkMetadataRepository).save(any(ChunkMetadata.class));

//         // Act
//         long fileId = fileProcessingService.processAndStoreFile(filename, fileData, username);

//         // Assert
//         verify(encryptionService, times(1)).encrypt(fileData);
//         verify(fileMetadataRepository, times(1)).save(any(FileMetadata.class));
//         // Assuming the encrypted data is larger than one chunk
//         verify(chunkStorageService, atLeastOnce()).storeChunk(any(byte[].class), anyLong(), anyInt());
//         verify(chunkMetadataRepository, atLeastOnce()).save(any(ChunkMetadata.class));
//         verify(loggingService, atLeastOnce()).logEvent(eq(username), anyString(), anyString(), eq(LogEntry.Status.SUCCESS));
//     }

//     @Test
//     void testRetrieveAndReassembleFile_Success() throws ProcessingException, SQLException {
//         // Arrange
//         long fileId = 1L;
//         String username = "testuser";
//         byte[] decryptedData = "This is a test file.".getBytes();
//         byte[] encryptedChunk = "encrypted_chunk".getBytes();

//         ChunkMetadata chunkMetadata = new ChunkMetadata();
//         chunkMetadata.setFileServerName("fileserver1");
//         chunkMetadata.setCrc32(12345L);
//         List<ChunkMetadata> chunkList = new ArrayList<>();
//         chunkList.add(chunkMetadata);

//         when(chunkMetadataRepository.findByFileIdOrderByChunkIndexAsc(fileId)).thenReturn(chunkList);
//         when(chunkStorageService.retrieveChunk(anyString(), anyLong(), anyInt())).thenReturn(encryptedChunk);
//         when(crcValidationService.validateCrc32(any(byte[].class), anyLong())).thenReturn(true);
//         when(encryptionService.decrypt(any(byte[].class))).thenReturn(decryptedData);

//         // Act
//         byte[] result = fileProcessingService.retrieveAndReassembleFile(fileId, username);

//         // Assert
//         assertArrayEquals(decryptedData, result);
//         verify(chunkStorageService, times(1)).retrieveChunk("fileserver1", fileId, 0);
//         verify(crcValidationService, times(1)).validateCrc32(encryptedChunk, 12345L);
//         verify(encryptionService, times(1)).decrypt(any(byte[].class));
//         verify(loggingService, times(1)).logEvent(eq(username), eq("FILE_DOWNLOAD_COMPLETE"), anyString(), eq(LogEntry.Status.SUCCESS));
//     }

//     @Test
//     void testProcessAndStoreFile_RollbackOnFailure() throws ProcessingException, SQLException {
//         // Arrange
//         String filename = "test.txt";
//         String username = "testuser";
//         byte[] fileData = "This is a test file.".getBytes();
//         byte[] encryptedData = "encrypted_data".getBytes();

//         when(encryptionService.encrypt(any(byte[].class))).thenReturn(encryptedData);
//         when(crcValidationService.calculateCrc32(any(byte[].class))).thenReturn(12345L);
//         when(chunkStorageService.storeChunk(any(byte[].class), anyLong(), anyInt())).thenThrow(new ProcessingException("SFTP failed"));
//         doNothing().when(fileMetadataRepository).save(any(FileMetadata.class));

//         // Act & Assert
//         assertThrows(ProcessingException.class, () -> {
//             fileProcessingService.processAndStoreFile(filename, fileData, username);
//         });

//         // Verify that rollback logic was called
//         verify(loggingService, times(1)).logEvent(eq(username), eq("FILE_UPLOAD_FAILURE"), anyString(), eq(LogEntry.Status.FAILURE));
//     }
// }
