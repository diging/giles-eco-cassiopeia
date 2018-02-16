package edu.asu.diging.gilesecosystem.cassiopeia.core.service.impl;

import java.io.File;
import java.io.FilenameFilter;
import java.time.ZonedDateTime;
import java.util.concurrent.Future;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import edu.asu.diging.gilesecosystem.cassiopeia.core.service.IKafkaRequestSender;
import edu.asu.diging.gilesecosystem.cassiopeia.core.service.IRequestResender;
import edu.asu.diging.gilesecosystem.util.files.IFileStorageManager;

@Service
public class RequestResender implements IRequestResender {

    @Autowired
    @Qualifier("fileStorageManager")
    private IFileStorageManager storageManager;
    
    @Autowired
    private IKafkaRequestSender kafkaRequestSender;

    /* (non-Javadoc)
     * @see edu.asu.diging.gilesecosystem.cassiopeia.core.service.impl.IRequestResender#resendRequests()
     */
    
    @Override
    @Async
    public Future<ResendingResult> resendRequests() {
        String baseDir = storageManager.getBaseDirectoryWithFiletype();
        File baseDirFolder = new File(baseDir);
        File[] requestFolders = baseDirFolder.listFiles(new DirectoryFilter());
        
        int requestCounter = 0;
        for (File requestFolder : requestFolders) {
            String requestId = requestFolder.getName();
            File[] documentFolders = requestFolder.listFiles(new DirectoryFilter());
            // there should be just one
            for (File documentFolder : documentFolders) {
                String docId = documentFolder.getName();
                File[] textFiles = documentFolder.listFiles(new FilenameFilter() {
                    
                    @Override
                    public boolean accept(File dir, String name) {
                        return name.endsWith(".txt");                            
                    }
                });
                
                for (File textFile : textFiles) {
                    String relativePath = storageManager.getFileFolderPathInBaseFolder(requestId, docId, null);
                    String imageFilename = textFile.getName();
                    imageFilename = imageFilename.substring(0, imageFilename.lastIndexOf("."));
                    
                    RequestInfo info = new RequestInfo(relativePath + File.separator + textFile.getName(), textFile.length(), textFile.getName());
                    info.setImageFilename(imageFilename);
                    kafkaRequestSender.sendRequest(requestId, docId, info);
                    requestCounter++;
                }
            }                
        }
        
        return new AsyncResult<ResendingResult>(new ResendingResult(requestCounter, ZonedDateTime.now()));
    }
    
    private final class DirectoryFilter implements FilenameFilter {
        @Override
        public boolean accept(File dir, String name) {
            return new File(dir, name).isDirectory();
        }
    }
}
