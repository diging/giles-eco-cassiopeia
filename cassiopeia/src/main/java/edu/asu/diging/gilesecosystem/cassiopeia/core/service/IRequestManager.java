package edu.asu.diging.gilesecosystem.cassiopeia.core.service;

import java.util.concurrent.ExecutionException;

import edu.asu.diging.gilesecosystem.cassiopeia.core.service.impl.ResendingResult;

public interface IRequestManager {

    public abstract void startResendingRequests();

    public abstract ResendingResult getResendingResults() throws InterruptedException,
            ExecutionException;

}