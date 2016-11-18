package edu.asu.diging.gilesecosystem.cassiopeia.core.service.impl;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Arrays;

import javax.annotation.PostConstruct;

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
import edu.asu.diging.gilesecosystem.cassiopeia.core.service.IOCRManager;
import edu.asu.diging.gilesecosystem.cassiopeia.rest.DownloadFileController;
import edu.asu.diging.gilesecosystem.requests.ICompletedOCRRequest;
import edu.asu.diging.gilesecosystem.requests.IOCRRequest;
import edu.asu.diging.gilesecosystem.requests.IRequestFactory;
import edu.asu.diging.gilesecosystem.requests.RequestStatus;
import edu.asu.diging.gilesecosystem.requests.exceptions.MessageCreationException;
import edu.asu.diging.gilesecosystem.requests.impl.CompletedOCRRequest;
import edu.asu.diging.gilesecosystem.requests.kafka.IRequestProducer;
import edu.asu.diging.gilesecosystem.util.files.IFileStorageManager;
import edu.asu.diging.gilesecosystem.util.properties.IPropertiesManager;

@Service
public class OCRManager implements IOCRManager {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    @Qualifier("fileStorageManager")
    private IFileStorageManager storageManager;

    @Autowired
    private IPropertiesManager propertyManager;
    
    @Autowired
    private IRequestFactory<ICompletedOCRRequest, CompletedOCRRequest> requestFactory;
    
    @Autowired
    private IRequestProducer requestProducer;
    
    
    @PostConstruct
    public void init() {
        requestFactory.config(CompletedOCRRequest.class);
    }

    /* (non-Javadoc)
     * @see edu.asu.diging.gilesecosystem.cassiopeia.core.service.impl.IOCRManager#processOCRRequest(edu.asu.diging.gilesecosystem.requests.IOCRRequest)
     */
    @Override
    public void processOCRRequest(IOCRRequest request) {

        String dirFolder = storageManager.getAndCreateStoragePath(request.getRequestId(),
                request.getDocumentId(), null);
        byte[] image = downloadFile(request.getDownloadUrl());
        
        String tesseractBin = propertyManager.getProperty(Properties.TESSERACT_BIN_FOLDER);
        String tesseractData = propertyManager.getProperty(Properties.TESSERACT_DATA_FOLDER);
        boolean createHocr = propertyManager.getProperty(Properties.TESSERACT_CREATE_HOCR).equalsIgnoreCase("true");
        
        TesseractOCRConfig config = new TesseractOCRConfig();
        config.setTesseractPath(tesseractBin);
        config.setTessdataPath(tesseractData);
        ParseContext parseContext = new ParseContext();
        parseContext.set(TesseractOCRConfig.class, config);
        TesseractOCRParser ocrParser = new TesseractOCRParser(createHocr);
        
        Metadata metadata = new Metadata();
        BodyContentHandler handler = new BodyContentHandler();
        
        String ocrResult = null;
        try (InputStream stream = new ByteArrayInputStream(image)) {
            ocrParser.parse(stream, handler, metadata, parseContext);
            ocrResult = handler.toString();
        } catch (SAXException | TikaException | IOException e) {
            logger.error("Error during ocr.", e);
            // FIXME: send to monitoring app
        }
        
        Text text = saveTextToFile(request.getRequestId(), request.getDocumentId(), ocrResult, request.getFilename(), ".txt");
        
        String restEndpoint = propertyManager.getProperty(Properties.BASE_URL);
        if (restEndpoint.endsWith("/")) {
            restEndpoint = restEndpoint.substring(0, restEndpoint.length()-1);
        }
        
        String fileEndpoint = restEndpoint + DownloadFileController.GET_FILE_URL
                .replace(DownloadFileController.REQUEST_ID_PLACEHOLDER, request.getRequestId())
                .replace(DownloadFileController.DOCUMENT_ID_PLACEHOLDER, request.getDocumentId())
                .replace(DownloadFileController.FILENAME_PLACEHOLDER, text.filename);
        
        ICompletedOCRRequest completedRequest = null;
        try {
            completedRequest = requestFactory.createRequest(request.getUploadId());
        } catch (InstantiationException | IllegalAccessException e) {
            logger.error("Could not create request.", e);
            // this should never happen if used correctly
        }
        
        completedRequest.setDocumentId(request.getDocumentId());
        completedRequest.setDownloadPath(text.path);
        completedRequest.setSize(text.size);
        completedRequest.setDownloadUrl(fileEndpoint);
        completedRequest.setFilename(request.getFilename());
        completedRequest.setFileid(request.getFileid());
        completedRequest.setRequestId(request.getRequestId());
        completedRequest.setStatus(RequestStatus.COMPLETE);
        completedRequest.setOcrDate(OffsetDateTime.now(ZoneId.of("UTC")).toString());
        completedRequest.setTextFilename(text.filename);
        
        try {
            requestProducer.sendRequest(completedRequest, propertyManager.getProperty(Properties.KAFKA_TOPIC_OCR_COMPLETE));
        } catch (MessageCreationException e) {
            logger.error("Could not send message.", e);
        }
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
    
    protected Text saveTextToFile(String requestId,
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
            logger.error("Could not create file.", e);
            return null;
        }

        try {
            FileWriter writer = new FileWriter(fileObject);
            BufferedWriter bfWriter = new BufferedWriter(writer);
            bfWriter.write(pageText);
            bfWriter.close();
            writer.close();
        } catch (IOException e) {
            logger.error("Could not write text to file.", e);
            return null;
        }

        String relativePath = storageManager.getFileFolderPathInBaseFolder(requestId, documentId, null);
        Text text = new Text(relativePath + File.separator + filename, fileObject.length(), filename);
        return text;
    }
    
    class Text {
        public String path;
        public long size;
        public String filename;
        
        public Text(String path, long size, String filename) {
            this.path = path;
            this.size = size;
            this.filename = filename;
        }
    }
}
