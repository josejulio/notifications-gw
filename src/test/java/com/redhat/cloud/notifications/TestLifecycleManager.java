/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
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
package com.redhat.cloud.notifications;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.mockserver.client.MockServerClient;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MockServerContainer;

import java.util.HashMap;
import java.util.Map;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

/**
 * @author hrupp
 */
public class TestLifecycleManager implements QuarkusTestResourceLifecycleManager {

    private KafkaContainer kafkaContainer;
    private MockServerContainer mockEngineServer;
    private MockServerClient mockServerClient;

    @Override
    public Map<String, String> start() {
        System.out.println("++++  TestLifecycleManager start +++");
        Map<String, String> properties = new HashMap<>();

        setupKafka(properties);
        setupMockServer(properties);

        System.out.println("+ -- Running with properties: " + properties);
        return properties;
    }

    @Override
    public void stop() {
        if (kafkaContainer != null) {
            kafkaContainer.stop();
        }

        // Helper to debug mock server issues
//           System.err.println(mockServerClient.retrieveLogMessages(request()));
//           System.err.println(mockServerClient.retrieveRecordedRequests(request()));

    }

    private void setupKafka(Map<String, String> properties) {
        kafkaContainer = new KafkaContainer();
        kafkaContainer.start();
        String boostrapServers = kafkaContainer.getBootstrapServers();
        properties.put("kafka.bootstrap.servers", boostrapServers);
    }

    private void setupMockServer(Map<String, String> properties) {
        mockEngineServer = new MockServerContainer();

        // set up mock engine
        mockEngineServer.start();
        String mockServerUrl = "http://" + mockEngineServer.getContainerIpAddress() + ":" + mockEngineServer.getServerPort();
        properties.put("rbac/mp-rest/url", mockServerUrl);
        mockServerClient = new MockServerClient(mockEngineServer.getContainerIpAddress(), mockEngineServer.getServerPort());

        String xRhIdentity = TestHelpers.encodeIdentityInfo("test","user");
        String access = TestHelpers.getFileAsString("rbac_example_full_access.json");

        mockServerClient
            .when(request()
                    .withPath("/api/rbac/v1/access/")
                    .withHeader("x-rh-identity", xRhIdentity)
            )
            .respond(response()
                    .withStatusCode(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(access)
            );
    }

}
