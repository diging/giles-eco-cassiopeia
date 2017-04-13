package edu.asu.diging.gilesecosystem.cassiopeia.core.service;

import java.util.concurrent.Future;

import edu.asu.diging.gilesecosystem.cassiopeia.core.service.impl.ResendingResult;

public interface IRequestResender {

    public Future<ResendingResult> resendRequests();

}