package edu.asu.diging.gilesecosystem.cassiopeia.core.service.impl;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.ocr.TesseractOCRConfig;
import org.apache.tika.parser.ocr.TesseractOCRParser;
import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.xml.sax.SAXException;

import edu.asu.diging.gilesecosystem.cassiopeia.core.properties.Properties;
import edu.asu.diging.gilesecosystem.cassiopeia.core.service.IKafkaRequestSender;
import edu.asu.diging.gilesecosystem.cassiopeia.core.service.IOCRManager;
import edu.asu.diging.gilesecosystem.requests.IOCRRequest;
import edu.asu.diging.gilesecosystem.requests.RequestStatus;
import edu.asu.diging.gilesecosystem.septemberutil.properties.MessageType;
import edu.asu.diging.gilesecosystem.septemberutil.service.ISystemMessageHandler;
import edu.asu.diging.gilesecosystem.util.files.IFileStorageManager;
import edu.asu.diging.gilesecosystem.util.properties.IPropertiesManager;

@Service
public class OCRManager implements IOCRManager {


    @Autowired
    @Qualifier("fileStorageManager")
    private IFileStorageManager storageManager;

    @Autowired
    private IPropertiesManager propertyManager;
    
    @Autowired
    private IKafkaRequestSender kafkaRequestSender;

    @Autowired
    private ISystemMessageHandler messageHandler;

    /* (non-Javadoc)
     * @see edu.asu.diging.gilesecosystem.cassiopeia.core.service.impl.IOCRManager#processOCRRequest(edu.asu.diging.gilesecosystem.requests.IOCRRequest)
     */
    @Override
    public void processOCRRequest(IOCRRequest request) {

        storageManager.getAndCreateStoragePath(request.getRequestId(),
                request.getDocumentId(), null);
        byte[] image = downloadFile(request.getDownloadUrl());
        
        String tesseractBin = propertyManager.getProperty(Properties.TESSERACT_BIN_FOLDER);
        String tesseractData = propertyManager.getProperty(Properties.TESSERACT_DATA_FOLDER);
        boolean createHocr = propertyManager.getProperty(Properties.TESSERACT_CREATE_HOCR).equalsIgnoreCase("true");
        
        TesseractOCRConfig config = new TesseractOCRConfig();
        config.setTesseractPath(tesseractBin);
        config.setTessdataPath(tesseractData);
        config.setTimeout(new Integer(propertyManager.getProperty(Properties.TESSERACT_TIMEOUT)));
        
        ParseContext parseContext = new ParseContext();
        parseContext.set(TesseractOCRConfig.class, config);
        TesseractOCRParser ocrParser = new TesseractOCRParser(createHocr);
        
        Metadata metadata = new Metadata();
        BodyContentHandler handler = new BodyContentHandler();
        
        RequestInfo info = null;
        try (InputStream stream = new ByteArrayInputStream(image)) {
            ocrParser.parse(stream, handler, metadata, parseContext);
            String ocrResult = handler.toString();
            info = saveTextToFile(request.getRequestId(), request.getDocumentId(), ocrResult, request.getFilename(), ".txt");
            info.setUploadId(request.getUploadId());
            info.setFileId(request.getFileId());
            info.setStatus(RequestStatus.COMPLETE);
            info.setImageFilename(request.getFilename());
        } catch (SAXException | TikaException | IOException e) {
            messageHandler.handleMessage("Error during ocr.", e, MessageType.ERROR);
            info = new RequestInfo(null, 0, null);
            info.setUploadId(request.getUploadId());
            info.setFileId(request.getFileId());
            info.setStatus(RequestStatus.FAILED);
            info.setErrorMsg(e.getMessage());
            info.setImageFilename(request.getFilename());
        }
        
        
        kafkaRequestSender.sendRequest(request.getRequestId(), request.getDocumentId(), info);
    }
    
    private byte[] downloadFile(String url) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(new ByteArrayHttpMessageConverter());

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM));
        headers.set(
                "Authorization",
                "token "
                        + propertyManager
                                .getProperty(Properties.GILES_ACCESS_TOKEN));
        HttpEntity<String> entity = new HttpEntity<String>(headers);

        ResponseEntity<byte[]> response = restTemplate.exchange(url, HttpMethod.GET,
                entity, byte[].class);
        if (response.getStatusCode().equals(HttpStatus.OK)) {
            return response.getBody();
        }
        return null;
    }
    
    protected RequestInfo saveTextToFile(String requestId,
            String documentId, String pageText, String filename, String fileExtentions) {
        String docFolder = storageManager.getAndCreateStoragePath(requestId,
                documentId, null);

        if (!fileExtentions.startsWith(".")) {
            fileExtentions = "." + fileExtentions;
        }
        filename = filename + fileExtentions;

        String filePath = docFolder + File.separator + filename;
        File fileObject = new File(filePath);
        try {
            fileObject.createNewFile();
        } catch (IOException e) {
            messageHandler.handleMessage("Could not create file.", e, MessageType.ERROR);
            return null;
        }

        try {
            FileWriter writer = new FileWriter(fileObject);
            BufferedWriter bfWriter = new BufferedWriter(writer);
            bfWriter.write(pageText);
            bfWriter.close();
            writer.close();
        } catch (IOException e) {
            messageHandler.handleMessage("Could not write text to file.", e, MessageType.ERROR);
            return null;
        }

        String relativePath = storageManager.getFileFolderPathInBaseFolder(requestId, documentId, null);
        return new RequestInfo(relativePath + File.separator + filename, fileObject.length(), filename);
    }
}