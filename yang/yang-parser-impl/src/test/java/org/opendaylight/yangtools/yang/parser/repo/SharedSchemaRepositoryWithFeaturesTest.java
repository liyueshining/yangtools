/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.yangtools.yang.parser.repo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.CheckedFuture;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import org.junit.Test;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.repo.api.SchemaContextFactory;
import org.opendaylight.yangtools.yang.model.repo.api.SchemaResolutionException;
import org.opendaylight.yangtools.yang.model.repo.api.SchemaSourceException;
import org.opendaylight.yangtools.yang.model.repo.api.SchemaSourceFilter;
import org.opendaylight.yangtools.yang.parser.util.ASTSchemaSource;
import org.opendaylight.yangtools.yang.parser.util.TextToASTTransformer;

public class SharedSchemaRepositoryWithFeaturesTest {

    @Test
    public void testSharedSchemaRepositoryWithSomeFeaturesSupported() throws Exception {
        Predicate<QName> isFeatureSupported = qName -> {
            Set<QName> supportedFeatures = new HashSet<>();
            supportedFeatures.add(QName.create("foobar-namespace", "1970-01-01", "test-feature-1"));

            return supportedFeatures.contains(qName);
        };

        final SharedSchemaRepository sharedSchemaRepository = new SharedSchemaRepository(
                "shared-schema-repo-with-features-test");

        final SettableSchemaProvider<ASTSchemaSource> foobar = getImmediateYangSourceProviderFromResource
                ("/if-feature-resolution-test/shared-schema-repository/foobar.yang");
        foobar.register(sharedSchemaRepository);
        foobar.setResult();

        final SchemaContextFactory fact = sharedSchemaRepository
                .createSchemaContextFactory(SchemaSourceFilter.ALWAYS_ACCEPT);

        final CheckedFuture<SchemaContext, SchemaResolutionException> testSchemaContextFuture =
                fact.createSchemaContext(Lists.newArrayList(foobar.getId()), isFeatureSupported);
        assertTrue(testSchemaContextFuture.isDone());
        assertSchemaContext(testSchemaContextFuture.checkedGet(), 1);

        Module module = testSchemaContextFuture.checkedGet().findModuleByName("foobar", null);
        assertNotNull(module);
        assertEquals(2, module.getChildNodes().size());

        ContainerSchemaNode testContainerA = (ContainerSchemaNode) module.getDataChildByName(
                QName.create(module.getQNameModule(), "test-container-a"));
        assertNotNull(testContainerA);
        LeafSchemaNode testLeafA = (LeafSchemaNode) testContainerA.getDataChildByName(
                QName.create(module.getQNameModule(), "test-leaf-a"));
        assertNotNull(testLeafA);

        ContainerSchemaNode testContainerB = (ContainerSchemaNode) module.getDataChildByName(
                QName.create(module.getQNameModule(), "test-container-b"));
        assertNull(testContainerB);

        ContainerSchemaNode testContainerC = (ContainerSchemaNode) module.getDataChildByName(
                QName.create(module.getQNameModule(), "test-container-c"));
        assertNotNull(testContainerC);
        LeafSchemaNode testLeafC = (LeafSchemaNode) testContainerC.getDataChildByName(
                QName.create(module.getQNameModule(), "test-leaf-c"));
        assertNotNull(testLeafC);
    }

    @Test
    public void testSharedSchemaRepositoryWithAllFeaturesSupported() throws Exception {
        final SharedSchemaRepository sharedSchemaRepository = new SharedSchemaRepository(
                "shared-schema-repo-with-features-test");

        final SettableSchemaProvider<ASTSchemaSource> foobar = getImmediateYangSourceProviderFromResource
                ("/if-feature-resolution-test/shared-schema-repository/foobar.yang");
        foobar.register(sharedSchemaRepository);
        foobar.setResult();

        final SchemaContextFactory fact = sharedSchemaRepository
                .createSchemaContextFactory(SchemaSourceFilter.ALWAYS_ACCEPT);

        final CheckedFuture<SchemaContext, SchemaResolutionException> testSchemaContextFuture = fact
                .createSchemaContext(Lists.newArrayList(foobar.getId()));
        assertTrue(testSchemaContextFuture.isDone());
        assertSchemaContext(testSchemaContextFuture.checkedGet(), 1);

        Module module = testSchemaContextFuture.checkedGet().findModuleByName("foobar", null);
        assertNotNull(module);
        assertEquals(3, module.getChildNodes().size());

        ContainerSchemaNode testContainerA = (ContainerSchemaNode) module.getDataChildByName(
                QName.create(module.getQNameModule(), "test-container-a"));
        assertNotNull(testContainerA);
        LeafSchemaNode testLeafA = (LeafSchemaNode) testContainerA.getDataChildByName(
                QName.create(module.getQNameModule(), "test-leaf-a"));
        assertNotNull(testLeafA);

        ContainerSchemaNode testContainerB = (ContainerSchemaNode) module.getDataChildByName(
                QName.create(module.getQNameModule(), "test-container-b"));
        assertNotNull(testContainerB);
        LeafSchemaNode testLeafB = (LeafSchemaNode) testContainerB.getDataChildByName(
                QName.create(module.getQNameModule(), "test-leaf-b"));
        assertNotNull(testLeafB);

        ContainerSchemaNode testContainerC = (ContainerSchemaNode) module.getDataChildByName(
                QName.create(module.getQNameModule(), "test-container-c"));
        assertNotNull(testContainerC);
        LeafSchemaNode testLeafC = (LeafSchemaNode) testContainerC.getDataChildByName(
                QName.create(module.getQNameModule(), "test-leaf-c"));
        assertNotNull(testLeafC);
    }

    @Test
    public void testSharedSchemaRepositoryWithNoFeaturesSupported() throws Exception {
        Predicate<QName> isFeatureSupported = qName -> false;

        final SharedSchemaRepository sharedSchemaRepository = new SharedSchemaRepository(
                "shared-schema-repo-with-features-test");

        final SettableSchemaProvider<ASTSchemaSource> foobar = getImmediateYangSourceProviderFromResource
                ("/if-feature-resolution-test/shared-schema-repository/foobar.yang");
        foobar.register(sharedSchemaRepository);
        foobar.setResult();

        final SchemaContextFactory fact = sharedSchemaRepository
                .createSchemaContextFactory(SchemaSourceFilter.ALWAYS_ACCEPT);

        final CheckedFuture<SchemaContext, SchemaResolutionException> testSchemaContextFuture = fact
                .createSchemaContext(Lists.newArrayList(foobar.getId()), isFeatureSupported);
        assertTrue(testSchemaContextFuture.isDone());
        assertSchemaContext(testSchemaContextFuture.checkedGet(), 1);

        Module module = testSchemaContextFuture.checkedGet().findModuleByName("foobar", null);
        assertNotNull(module);
        assertEquals(1, module.getChildNodes().size());

        ContainerSchemaNode testContainerC = (ContainerSchemaNode) module.getDataChildByName(
                QName.create(module.getQNameModule(), "test-container-c"));
        assertNotNull(testContainerC);
        LeafSchemaNode testLeafC = (LeafSchemaNode) testContainerC.getDataChildByName(
                QName.create(module.getQNameModule(), "test-leaf-c"));
        assertNotNull(testLeafC);
    }

    private static SettableSchemaProvider<ASTSchemaSource> getImmediateYangSourceProviderFromResource(
            final String resourceName) throws Exception {
        final ResourceYangSource yangSource = new ResourceYangSource(resourceName);
        final CheckedFuture<ASTSchemaSource, SchemaSourceException> aSTSchemaSource = TextToASTTransformer.TRANSFORMATION.apply(yangSource);
        return SettableSchemaProvider.createImmediate(aSTSchemaSource.get(), ASTSchemaSource.class);
    }

    private static void assertSchemaContext(final SchemaContext schemaContext, final int moduleSize) {
        assertNotNull(schemaContext);
        assertEquals(moduleSize, schemaContext.getModules().size());
    }
}
