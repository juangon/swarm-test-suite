/**
 * Copyright 2015-2016 Red Hat, Inc, and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.swarm.ts.hollow.jar.full.batch;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.AbstractItemWriter;
import javax.batch.api.chunk.ItemWriter;
import javax.inject.Inject;
import javax.inject.Named;

import org.wildfly.swarm.ts.hollow.jar.full.ProcessResult;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@Named
public class SimpleItemWriter extends AbstractItemWriter implements ItemWriter {

    @Inject
    private ProcessResult processResult;

    @Override
    public void open(final Serializable checkpoint) throws Exception {
        if (sleepTime != null) {
            sleepMillis = Long.parseLong(sleepTime);
        }
        if (failAt == 0) {
            failAt = -1;
        }
    }

    @Override
    public void writeItems(final List<Object> items) throws Exception {
        try {
            for (Object item : items) {
                processResult.addWrittenItems(item);
                if (counter.incrementAndGet() == failAt) {
                    throw new IllegalStateException("Failed at " + failAt + " per request");
                }
            }
        } finally {
            final long sleepMillis = this.sleepMillis;
            if (sleepMillis > 0) {
                TimeUnit.MILLISECONDS.sleep(sleepMillis);
            }

        }
    }

    private final AtomicInteger counter = new AtomicInteger();

    @BatchProperty(name = "writer.sleep")
    @Inject
    private String sleepTime;

    @BatchProperty(name = "writer.failAt")
    @Inject
    private long failAt;

    private volatile long sleepMillis = -1L;


}
