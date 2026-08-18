package com.nahid.order.saga;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class SagaManager {

    private static final int MAX_RETRY_ATTEMPTS = 3;

    private final List<SagaCommand> commands = new ArrayList<>();
    private final Deque<SagaCommand> executedCommands = new ArrayDeque<>();

    public void addStep(SagaCommand command) {
        commands.add(command);
    }

    public void execute() throws Exception {
        try {
            for (SagaCommand command : commands) {
                command.execute();
                executedCommands.push(command);
            }
        } catch (Exception ex) {
            rollbackAll();
            throw ex;
        }
    }

    private void rollbackAll() {
        while (!executedCommands.isEmpty()) {
            SagaCommand command = executedCommands.pop();

            for (int attempt = 0; attempt < MAX_RETRY_ATTEMPTS; attempt++) {
                try {
                    command.rollback();
                    break;
                } catch (Exception ex) {
                    if (attempt == MAX_RETRY_ATTEMPTS - 1) {
                        // Log critical error - manual intervention needed
                    }
                    sleep(1000L * (attempt + 1));
                }
            }
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
