package edu.asu.diging.gilesecosystem.cassiopeia.core.kafka;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.kafka.annotation.KafkaListener;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.asu.diging.gilesecosystem.cassiopeia.core.service.IOCRManager;
import edu.asu.diging.gilesecosystem.requests.IOCRRequest;
import edu.asu.diging.gilesecosystem.requests.impl.OCRRequest;
import edu.asu.diging.gilesecosystem.septemberutil.properties.MessageType;
import edu.asu.diging.gilesecosystem.septemberutil.service.ISystemMessageHandler;

@PropertySource("classpath:/config.properties")
public class OCRRequestReceiver {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    
    @Autowired
    private IOCRManager ocrManager;

    @Autowired
    private ISystemMessageHandler messageHandler;
    
    @KafkaListener(id="cassiopeia.ocr", topics = "${topic_ocr_request}")
    public void receiveMessage(String message) {
        ObjectMapper mapper = new ObjectMapper();
        IOCRRequest request = null;
        try {
            request = mapper.readValue(message, OCRRequest.class);
        } catch (IOException e) {
            messageHandler.handleMessage("Could not unmarshall request.", e, MessageType.ERROR);
            // FIXME: handle this case
            return;
        }
        
        ocrManager.processOCRRequest(request);
    }
}
