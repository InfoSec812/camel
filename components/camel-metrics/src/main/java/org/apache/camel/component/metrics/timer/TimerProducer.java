/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.metrics.timer;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.component.metrics.AbstractMetricsProducer;
import org.apache.camel.component.metrics.timer.TimerEndpoint.TimerAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.camel.component.metrics.MetricsConstants.HEADER_TIMER_ACTION;
import static org.apache.camel.component.metrics.timer.TimerEndpoint.ENDPOINT_URI;

public class TimerProducer extends AbstractMetricsProducer<TimerEndpoint> {

    private static final Logger LOG = LoggerFactory.getLogger(TimerProducer.class);

    public TimerProducer(TimerEndpoint endpoint) {
        super(endpoint);
    }

    @Override
    protected void doProcess(Exchange exchange, TimerEndpoint endpoint, MetricRegistry registry, String metricsName) throws Exception {
        Message in = exchange.getIn();
        TimerAction action = endpoint.getAction();
        TimerAction finalAction = in.getHeader(HEADER_TIMER_ACTION, action, TimerAction.class);
        if (finalAction == TimerAction.start) {
            handleStart(exchange, registry, metricsName);
        } else if (finalAction == TimerAction.stop) {
            handleStop(exchange, registry, metricsName);
        } else {
            LOG.warn("No action provided for timer \"{}\"", metricsName);
        }
    }

    void handleStart(Exchange exchange, MetricRegistry registry, String metricsName) {
        String propertyName = getPropertyName(metricsName);
        Timer.Context context = getTimerContextFromExchange(exchange, propertyName);
        if (context == null) {
            Timer timer = registry.timer(metricsName);
            context = timer.time();
            exchange.setProperty(propertyName, context);
        } else {
            LOG.warn("Timer \"{}\" already running", metricsName);
        }
    }

    void handleStop(Exchange exchange, MetricRegistry registry, String metricsName) {
        String propertyName = getPropertyName(metricsName);
        Timer.Context context = getTimerContextFromExchange(exchange, propertyName);
        if (context != null) {
            context.stop();
            exchange.removeProperty(propertyName);
        } else {
            LOG.warn("Timer \"{}\" not found", metricsName);
        }
    }

    String getPropertyName(String metricsName) {
        return new StringBuilder(ENDPOINT_URI)
                .append(":")
                .append(metricsName)
                .toString();
    }

    Timer.Context getTimerContextFromExchange(Exchange exchange, String propertyName) {
        return exchange.getProperty(propertyName, Timer.Context.class);
    }
}
