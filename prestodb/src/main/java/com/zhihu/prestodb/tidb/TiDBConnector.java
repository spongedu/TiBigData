/*
 * Copyright 2020 Zhihu.
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
package com.zhihu.prestodb.tidb;

import com.facebook.airlift.bootstrap.LifeCycleManager;
import com.facebook.airlift.log.Logger;
import com.facebook.presto.spi.connector.*;
import com.facebook.presto.spi.transaction.IsolationLevel;
import com.zhihu.prestodb.tidb.optimization.TiDBPlanOptimizerProvider;

import javax.inject.Inject;

import static com.facebook.presto.spi.transaction.IsolationLevel.REPEATABLE_READ;
import static com.facebook.presto.spi.transaction.IsolationLevel.checkConnectorSupports;
import static java.util.Objects.requireNonNull;

public final class TiDBConnector
        implements Connector
{
    private static final Logger log = Logger.get(TiDBConnector.class);

    private final LifeCycleManager lifeCycleManager;
    private final TiDBMetadata metadata;
    private final TiDBSplitManager splitManager;
    private final TiDBRecordSetProvider recordSetProvider;
    private final ConnectorPlanOptimizerProvider planOptimizerProvider;

    @Inject
    public TiDBConnector(
            LifeCycleManager lifeCycleManager,
            TiDBMetadata metadata,
            TiDBSplitManager splitManager,
            TiDBRecordSetProvider recordSetProvider,
            TiDBPlanOptimizerProvider planOptimizerProvider)
    {
        this.lifeCycleManager = requireNonNull(lifeCycleManager, "lifeCycleManager is null");
        this.metadata = requireNonNull(metadata, "metadata is null");
        this.splitManager = requireNonNull(splitManager, "splitManager is null");
        this.recordSetProvider = requireNonNull(recordSetProvider, "recordSetProvider is null");
        this.planOptimizerProvider = requireNonNull(planOptimizerProvider, "planOptimizerProvider is null");
    }

    @Override
    public ConnectorTransactionHandle beginTransaction(IsolationLevel isolationLevel, boolean readOnly)
    {
        checkConnectorSupports(REPEATABLE_READ, isolationLevel);
        return new TiDBTransactionHandle();
    }

    @Override
    public ConnectorMetadata getMetadata(ConnectorTransactionHandle transactionHandle)
    {
        return metadata;
    }

    @Override
    public ConnectorSplitManager getSplitManager()
    {
        return splitManager;
    }

    @Override
    public ConnectorRecordSetProvider getRecordSetProvider()
    {
        return recordSetProvider;
    }

    @Override
    public ConnectorPlanOptimizerProvider getConnectorPlanOptimizerProvider()
    {
        return planOptimizerProvider;
    }

    @Override
    public final void shutdown()
    {
        try {
            lifeCycleManager.stop();
        }
        catch (Exception e) {
            log.error(e, "Error shutting down connector");
        }
    }
}
