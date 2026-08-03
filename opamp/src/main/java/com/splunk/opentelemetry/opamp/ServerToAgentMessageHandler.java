/*
 * Copyright Splunk Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.splunk.opentelemetry.opamp;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.logging.Level.WARNING;

import com.splunk.opamp.remotecontrol.CommandDispatcher;
import com.splunk.opentelemetry.profiler.util.HelpfulExecutors;
import io.opentelemetry.opamp.client.OpampClient;
import io.opentelemetry.opamp.client.internal.response.MessageData;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.logging.Logger;
import opamp.proto.AgentConfigFile;
import opamp.proto.AgentRemoteConfig;

public class ServerToAgentMessageHandler {
  public static final String MAGIC_CMD_STRING = "COMMAND_HACKS";

  private static final Logger logger =
      Logger.getLogger(ServerToAgentMessageHandler.class.getName());
  private static final int MAX_MESSAGE_QUEUE_SIZE = 5;
  private final BlockingQueue<ServerMessage> serverMessageQueue;
  private final RemoteConfigProcessor remoteConfigProcessor;
  private final CommandDispatcher commandDispatcher;

  public static ServerToAgentMessageHandler createAndStart(
      RemoteConfigProcessor remoteConfigProcessor, CommandDispatcher commandDispatcher) {
    ExecutorService executor = HelpfulExecutors.newSingleThreadExecutor("Server Message Handler");
    ServerToAgentMessageHandler messageHandler =
        new ServerToAgentMessageHandler(
            new LinkedBlockingDeque<>(MAX_MESSAGE_QUEUE_SIZE),
            remoteConfigProcessor,
            commandDispatcher);

    messageHandler.start(executor);

    return messageHandler;
  }

  @VisibleForTesting
  ServerToAgentMessageHandler(
      BlockingQueue<ServerMessage> serverMessageQueue,
      RemoteConfigProcessor remoteConfigProcessor,
      CommandDispatcher commandDispatcher) {
    this.serverMessageQueue = serverMessageQueue;
    this.remoteConfigProcessor = remoteConfigProcessor;
    this.commandDispatcher = commandDispatcher;
  }

  @VisibleForTesting
  void start(ExecutorService executor) {
    executor.submit(this::messageProcessingLoop);
  }

  private void messageProcessingLoop() {
    while (true) {
      try {
        ServerMessage serverMessage = serverMessageQueue.take();
        processMessage(serverMessage);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        logger.fine("ServerToAgentMessageHandler is shutting down");
        return;
      } catch (Exception e) {
        logger.log(WARNING, "ServerToAgentMessageHandler encountered an unexpected exception", e);
      }
    }
  }

  public void handleMessage(MessageData message, OpampClient opampClient) {
    if (!serverMessageQueue.offer(new ServerMessage(message, opampClient))) {
      logger.severe("Message queue is full. Could not enqueue message " + message);
    }
  }

  private void processMessage(ServerMessage serverMessage) {
    AgentRemoteConfig remoteConfig = serverMessage.messageData.getRemoteConfig();
    if (remoteConfig != null) {

      if (remoteConfig.config.config_map.containsKey(MAGIC_CMD_STRING)) {
        AgentConfigFile agentConfigFile = remoteConfig.config.config_map.get(MAGIC_CMD_STRING);
        String contentType = agentConfigFile.content_type;
        String body = agentConfigFile.body.string(UTF_8);
        commandDispatcher.dispatch(contentType, body);
        if (remoteConfig.config.config_map.size() == 1) { // just this command
          return;
        }
      }

      remoteConfigProcessor.applyConfig(remoteConfig, serverMessage.opampClient);
    }
  }

  private static class ServerMessage {
    private final MessageData messageData;
    private final OpampClient opampClient;

    ServerMessage(MessageData messageData, OpampClient opampClient) {
      this.messageData = messageData;
      this.opampClient = opampClient;
    }
  }
}
