/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.yang.model.repo.util;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;

import com.google.common.base.Charsets;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.yangtools.yang.model.repo.api.RevisionSourceIdentifier;
import org.opendaylight.yangtools.yang.model.repo.api.SchemaSourceException;
import org.opendaylight.yangtools.yang.model.repo.api.SchemaSourceRepresentation;
import org.opendaylight.yangtools.yang.model.repo.api.SourceIdentifier;
import org.opendaylight.yangtools.yang.model.repo.api.YangSchemaSourceRepresentation;
import org.opendaylight.yangtools.yang.model.repo.api.YangTextSchemaSource;
import org.opendaylight.yangtools.yang.model.repo.spi.PotentialSchemaSource;
import org.opendaylight.yangtools.yang.model.repo.spi.SchemaSourceProvider;
import org.opendaylight.yangtools.yang.model.repo.spi.SchemaSourceRegistration;
import org.opendaylight.yangtools.yang.model.repo.spi.SchemaSourceRegistry;

@RunWith(MockitoJUnitRunner.class)
public class InMemorySchemaSourceCacheTest {

    private static final Class<YangSchemaSourceRepresentation> representation = YangSchemaSourceRepresentation.class;
    private static final long lifetime = 1000l;
    private static final TimeUnit units = TimeUnit.MILLISECONDS;

    @Mock
    private SchemaSourceRegistry registry;
    @Mock
    private SchemaSourceRegistration<?> registration;

    @Before
    public void setUp() throws Exception {
        doReturn(this.registration).when(this.registry).registerSchemaSource(any(SchemaSourceProvider.class),
                any(PotentialSchemaSource.class));
    }

    @Test
    public void inMemorySchemaSourceCacheTest1() {
        final InMemorySchemaSourceCache<YangSchemaSourceRepresentation> inMemorySchemaSourceCache = InMemorySchemaSourceCache
                .createSoftCache(this.registry, InMemorySchemaSourceCacheTest.representation);
        Assert.assertNotNull(inMemorySchemaSourceCache);
        inMemorySchemaSourceCache.close();
    }

    @Test
    public void inMemorySchemaSourceCacheTest2() {
        final InMemorySchemaSourceCache<YangSchemaSourceRepresentation> inMemorySchemaSourceCache = InMemorySchemaSourceCache
                .createSoftCache(this.registry, InMemorySchemaSourceCacheTest.representation,
                        InMemorySchemaSourceCacheTest.lifetime, InMemorySchemaSourceCacheTest.units);
        Assert.assertNotNull(inMemorySchemaSourceCache);
        inMemorySchemaSourceCache.close();
    }

    @Test
    public void inMemorySchemaSourceCacheOfferAndGetSourcestest() throws Exception {
        final InMemorySchemaSourceCache<YangSchemaSourceRepresentation> inMemorySchemaSourceCache = InMemorySchemaSourceCache
                .createSoftCache(this.registry, InMemorySchemaSourceCacheTest.representation);
        final String content = "content";
        final YangTextSchemaSource source = new TestingYangSource("test", "2012-12-12", content);
        inMemorySchemaSourceCache.offer(source);
        final SourceIdentifier sourceIdentifier = RevisionSourceIdentifier.create("test", "2012-12-12");
        final CheckedFuture<? extends YangSchemaSourceRepresentation, SchemaSourceException> checkedSource = inMemorySchemaSourceCache
                .getSource(sourceIdentifier);
        Assert.assertNotNull(checkedSource);
        final YangSchemaSourceRepresentation yangSchemaSourceRepresentation = checkedSource.get();
        Assert.assertNotNull(yangSchemaSourceRepresentation);
        Assert.assertEquals(sourceIdentifier, yangSchemaSourceRepresentation.getIdentifier());
        inMemorySchemaSourceCache.close();
    }

    @Test(expected = ExecutionException.class)
    public void inMemorySchemaSourceCacheNullGetSourcestest() throws Exception {
        final InMemorySchemaSourceCache<YangSchemaSourceRepresentation> inMemorySchemaSourceCache = InMemorySchemaSourceCache
                .createSoftCache(this.registry, InMemorySchemaSourceCacheTest.representation);
        final SourceIdentifier sourceIdentifier = RevisionSourceIdentifier.create("test", "2012-12-12");
        final CheckedFuture<? extends YangSchemaSourceRepresentation, SchemaSourceException> checkedSource = inMemorySchemaSourceCache
                .getSource(sourceIdentifier);
        Assert.assertNotNull(checkedSource);
        checkedSource.get();
        inMemorySchemaSourceCache.close();
    }

    @Test
    public void inMemorySchemaSourceCache3test() throws Exception {
        final InMemorySchemaSourceCache<YangSchemaSourceRepresentation> inMemorySchemaSourceCache = InMemorySchemaSourceCache
                .createSoftCache(this.registry, InMemorySchemaSourceCacheTest.representation);

        final InMemorySchemaSourceCache<YangSchemaSourceRepresentation> inMemorySchemaSourceCache2 = InMemorySchemaSourceCache
                .createSoftCache(this.registry, representation, InMemorySchemaSourceCacheTest.lifetime,
                        InMemorySchemaSourceCacheTest.units);

        final String content = "content";
        final YangTextSchemaSource source = new TestingYangSource("test", "2012-12-12", content);
        inMemorySchemaSourceCache.offer(source);
        inMemorySchemaSourceCache2.offer(source);

        final SourceIdentifier sourceIdentifier = RevisionSourceIdentifier.create("test", "2012-12-12");
        final CheckedFuture<? extends YangSchemaSourceRepresentation, SchemaSourceException> checkedSource = inMemorySchemaSourceCache
                .getSource(sourceIdentifier);
        final CheckedFuture<? extends SchemaSourceRepresentation, SchemaSourceException> checkedSource2 = inMemorySchemaSourceCache2
                .getSource(sourceIdentifier);
        Assert.assertNotNull(checkedSource);
        Assert.assertNotNull(checkedSource2);

        Assert.assertEquals(checkedSource.get(), checkedSource2.get());
        inMemorySchemaSourceCache.close();
        inMemorySchemaSourceCache2.close();
    }

    private class TestingYangSource extends YangTextSchemaSource {

        private final String content;

        protected TestingYangSource(final String name, final String revision, final String content) {
            super(RevisionSourceIdentifier.create(name, Optional.fromNullable(revision)));
            this.content = content;
        }

        @Override
        protected MoreObjects.ToStringHelper addToStringAttributes(final MoreObjects.ToStringHelper toStringHelper) {
            return toStringHelper;
        }

        @Override
        public InputStream openStream() throws IOException {
            return new ByteArrayInputStream(this.content.getBytes(Charsets.UTF_8));
        }
    }

}
