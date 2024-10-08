/**
 * Copyright (c) 2012-2023 Nikita Koksharov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.corundumstudio.socketio.handler;

import com.corundumstudio.socketio.HandshakeData;
import io.netty.channel.Channel;
import io.netty.util.internal.PlatformDependent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

public class ClientsBox {
    private static final Logger log = LoggerFactory.getLogger(ClientsBox.class);

    private final Map<UUID, ClientHead> uuid2clients = PlatformDependent.newConcurrentHashMap();
    private final Map<Channel, ClientHead> channel2clients = PlatformDependent.newConcurrentHashMap();

    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

    public ClientsBox() {
        scheduledExecutorService.scheduleWithFixedDelay(() -> {
            List<UUID> disconnected = uuid2clients.entrySet()
                    .stream().filter(entry -> !entry.getValue().isConnected())
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
            disconnected.forEach(uuid -> {
                ClientHead clientHead = uuid2clients.remove(uuid);
                if (clientHead != null) {
                    log.warn("Client with sessionId {}-{} was disconnected but still exists in uuid2clients",
                            clientHead.getSessionId(), clientHead.getEngineIOVersion());
                }
            });
        }, 5, 5, java.util.concurrent.TimeUnit.SECONDS);
    }

    // TODO use storeFactory
    public HandshakeData getHandshakeData(UUID sessionId) {
        ClientHead client = uuid2clients.get(sessionId);
        if (client == null) {
            return null;
        }

        return client.getHandshakeData();
    }

    public void addClient(ClientHead clientHead) {
        uuid2clients.put(clientHead.getSessionId(), clientHead);
    }

    public ClientHead removeClient(UUID sessionId) {
        return uuid2clients.remove(sessionId);
    }

    public ClientHead get(UUID sessionId) {
        return uuid2clients.get(sessionId);
    }

    public void add(Channel channel, ClientHead clientHead) {
        channel2clients.put(channel, clientHead);
    }

    public void remove(Channel channel) {
        channel2clients.remove(channel);
    }


    public ClientHead get(Channel channel) {
        return channel2clients.get(channel);
    }

}
