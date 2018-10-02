package edu.asu.diging.gilesecosystem.cassiopeia.core.service.impl;

import java.time.OffsetDateTime;
import java.time.ZoneId;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.asu.diging.gilesecosystem.cassiopeia.core.properties.Properties;
import edu.asu.diging.gilesecosystem.cassiopeia.core.service.IKafkaRequestSender;
import edu.asu.diging.gilesecosystem.cassiopeia.rest.DownloadFileController;
import edu.asu.diging.gilesecosystem.requests.ICompletedOCRRequest;
import edu.asu.diging.gilesecosystem.requests.IRequestFactory;
import edu.asu.diging.gilesecosystem.requests.RequestStatus;
import edu.asu.diging.gilesecosystem.requests.exceptions.MessageCreationException;
import edu.asu.diging.gilesecosystem.requests.impl.CompletedOCRRequest;
import edu.asu.diging.gilesecosystem.requests.kafka.IRequestProducer;
import edu.asu.diging.gilesecosystem.septemberutil.properties.MessageType;
import edu.asu.diging.gilesecosystem.septemberutil.service.ISystemMessageHandler;
import edu.asu.diging.gilesecosystem.util.properties.IPropertiesManager;

@Service
public class KafkaRequestSender implements IKafkaRequestSender {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private IPropertiesManager propertyManager;

    @Autowired
    private IRequestFactory<ICompletedOCRRequest, CompletedOCRRequest> requestFactory;

    @Autowired
    private IRequestProducer requestProducer;

    @Autowired
    private ISystemMessageHandler messageHandler;

    @PostConstruct
    public void init() {
        requestFactory.config(CompletedOCRRequest.class);
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.asu.diging.gilesecosystem.cassiopeia.core.service.impl.
     * IKafkaRequestSender#sendRequest(java.lang.String, java.lang.String,
     * java.lang.String, java.lang.String,
     * edu.asu.diging.gilesecosystem.cassiopeia.core.service.impl.RequestInfo)
     */
    @Override
    public void sendRequest(String requestId, String documentId, RequestInfo info) {
        String restEndpoint = propertyManager.getProperty(Properties.BASE_URL);
        StringBuffer errorMsgs;
        if (restEndpoint.endsWith("/")) {
            restEndpoint = restEndpoint.substring(0, restEndpoint.length() - 1);
        }

        String fileEndpoint = null;

        if (info.getStatus() == RequestStatus.COMPLETE) {
            fileEndpoint = restEndpoint + DownloadFileController.GET_FILE_URL
                    .replace(DownloadFileController.REQUEST_ID_PLACEHOLDER, requestId)
                    .replace(DownloadFileController.DOCUMENT_ID_PLACEHOLDER, documentId)
                    .replace(DownloadFileController.FILENAME_PLACEHOLDER,
                            info.getFilename());
        }

        ICompletedOCRRequest completedRequest = null;
        try {
            completedRequest = requestFactory.createRequest(requestId,
                    info.getUploadId());
        } catch (InstantiationException | IllegalAccessException e) {
            messageHandler.handleMessage("Could not create request.", e,
                    MessageType.ERROR);
            // this should never happen if used correctly
        }

        completedRequest.setDocumentId(documentId);
        completedRequest.setDownloadPath(info.getPath());
        completedRequest.setSize(info.getSize());
        completedRequest.setDownloadUrl(fileEndpoint);
        completedRequest.setFilename(info.getImageFilename());
        completedRequest.setFileId(info.getFileId());
        completedRequest.setStatus(info.getStatus());
        completedRequest.setErrorMsg(info.getErrorMsg());
        completedRequest.setOcrDate(OffsetDateTime.now(ZoneId.of("UTC")).toString());
        completedRequest.setTextFilename(info.getFilename());
       
		if ((fileEndpoint == null) || (fileEndpoint.contains("null"))) { // check for null value in url
			errorMsgs = new StringBuffer(info.getErrorMsg());

			if (errorMsgs.length() != 0) {
				if (!errorMsgs.toString().endsWith(".")) {
					errorMsgs.append(".");
				}
			   errorMsgs.append("Also,");
			}
			errorMsgs.append("File End Point is null or having null component vales in URL");

			completedRequest.setErrorMsg(errorMsgs.toString());
			completedRequest.setStatus(RequestStatus.FAILED);
		}
        try {
            requestProducer.sendRequest(completedRequest,
                    propertyManager.getProperty(Properties.KAFKA_TOPIC_OCR_COMPLETE));
        } catch (MessageCreationException e) {
            messageHandler.handleMessage("Could not send message.", e, MessageType.ERROR);
        }
    }

}
