package com.nahid.order.saga;

public interface SagaCommand {

    void execute() throws Exception;

    void rollback() throws Exception;
}
