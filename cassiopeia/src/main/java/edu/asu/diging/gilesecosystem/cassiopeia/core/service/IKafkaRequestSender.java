package edu.asu.diging.gilesecosystem.cassiopeia.core.service;

import edu.asu.diging.gilesecosystem.cassiopeia.core.service.impl.RequestInfo;

public interface IKafkaRequestSender {

    public abstract void sendRequest(String requestId, String documentId,
           RequestInfo info);

}