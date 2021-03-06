/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.yangtools.util.concurrent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.opendaylight.yangtools.util.concurrent.AsyncNotifyingListeningExecutorServiceTest.testListenerCallback;
import static org.opendaylight.yangtools.util.concurrent.CommonTestUtils.SUBMIT_CALLABLE;
import static org.opendaylight.yangtools.util.concurrent.CommonTestUtils.SUBMIT_RUNNABLE;
import static org.opendaylight.yangtools.util.concurrent.CommonTestUtils.SUBMIT_RUNNABLE_WITH_RESULT;
import com.google.common.base.Supplier;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yangtools.util.concurrent.CommonTestUtils.Invoker;

/**
 * Unit tests for DeadlockDetectingListeningExecutorService.
 *
 * @author Thomas Pantelis
 */
public class DeadlockDetectingListeningExecutorServiceTest {

    interface InitialInvoker {
        void invokeExecutor( ListeningExecutorService executor, Runnable task );
    }

    static final InitialInvoker SUBMIT = new InitialInvoker() {
        @Override
        public void invokeExecutor( final ListeningExecutorService executor, final Runnable task ) {
            executor.submit( task );
        }
    };

    static final InitialInvoker EXECUTE = new InitialInvoker() {
        @Override
        public void invokeExecutor( final ListeningExecutorService executor, final Runnable task ) {
            executor.execute( task );
        }
    };

    @SuppressWarnings("serial")
    public static class TestDeadlockException extends Exception {
    }

    private static final Supplier<Exception> DEADLOCK_EXECUTOR_SUPPLIER = new Supplier<Exception>() {
        @Override
        public Exception get() {
            return new TestDeadlockException();
        }
    };

    DeadlockDetectingListeningExecutorService executor;

    @Before
    public void setup() {
    }

    @After
    public void tearDown() {
        if (executor != null ) {
            executor.shutdownNow();
        }
    }

    DeadlockDetectingListeningExecutorService newExecutor() {
        return new DeadlockDetectingListeningExecutorService( Executors.newSingleThreadExecutor(),
                DEADLOCK_EXECUTOR_SUPPLIER );
    }

    @Test
    public void testBlockingSubmitOffExecutor() throws Exception {

        executor = newExecutor();

        // Test submit with Callable.

        ListenableFuture<String> future = executor.submit( new Callable<String>() {
            @Override
            public String call() throws Exception{
                return "foo";
            }
        } );

        assertEquals( "Future result", "foo", future.get( 5, TimeUnit.SECONDS ) );

        // Test submit with Runnable.

        executor.submit( new Runnable() {
            @Override
            public void run(){
            }
        } ).get();

        // Test submit with Runnable and value.

        future = executor.submit( new Runnable() {
            @Override
            public void run(){
            }
        }, "foo" );

        assertEquals( "Future result", "foo", future.get( 5, TimeUnit.SECONDS ) );
    }

    @Test
    public void testNonBlockingSubmitOnExecutorThread() throws Throwable {

        executor = newExecutor();

        testNonBlockingSubmitOnExecutorThread( SUBMIT, SUBMIT_CALLABLE );
        testNonBlockingSubmitOnExecutorThread( SUBMIT, SUBMIT_RUNNABLE );
        testNonBlockingSubmitOnExecutorThread( SUBMIT, SUBMIT_RUNNABLE_WITH_RESULT );

        testNonBlockingSubmitOnExecutorThread( EXECUTE, SUBMIT_CALLABLE );
    }

    void testNonBlockingSubmitOnExecutorThread( final InitialInvoker initialInvoker,
            final Invoker invoker ) throws Throwable {

        final AtomicReference<Throwable> caughtEx = new AtomicReference<>();
        final CountDownLatch futureCompletedLatch = new CountDownLatch( 1 );

        Runnable task = new Runnable() {
            @SuppressWarnings({ "unchecked", "rawtypes" })
            @Override
            public void run() {

                Futures.addCallback( invoker.invokeExecutor( executor, null ), new FutureCallback() {
                    @Override
                    public void onSuccess( final Object result ) {
                        futureCompletedLatch.countDown();
                    }

                    @Override
                    public void onFailure( final Throwable t ) {
                        caughtEx.set( t );
                        futureCompletedLatch.countDown();
                    }
                } );
            }

        };

        initialInvoker.invokeExecutor( executor, task );

        assertTrue( "Task did not complete - executor likely deadlocked",
                futureCompletedLatch.await( 5, TimeUnit.SECONDS ) );

        if (caughtEx.get() != null ) {
            throw caughtEx.get();
        }
    }

    @Test
    public void testBlockingSubmitOnExecutorThread() throws Exception {

        executor = newExecutor();

        testBlockingSubmitOnExecutorThread( SUBMIT, SUBMIT_CALLABLE );
        testBlockingSubmitOnExecutorThread( SUBMIT, SUBMIT_RUNNABLE );
        testBlockingSubmitOnExecutorThread( SUBMIT, SUBMIT_RUNNABLE_WITH_RESULT );

        testBlockingSubmitOnExecutorThread( EXECUTE, SUBMIT_CALLABLE );
    }

    void testBlockingSubmitOnExecutorThread( final InitialInvoker initialInvoker,
            final Invoker invoker ) throws Exception {

        final AtomicReference<Throwable> caughtEx = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch( 1 );

        Runnable task = new Runnable() {
            @Override
            public void run() {

                try {
                    invoker.invokeExecutor( executor, null ).get();
                } catch( ExecutionException e ) {
                    caughtEx.set( e.getCause() );
                } catch( Throwable e ) {
                    caughtEx.set( e );
                } finally {
                    latch.countDown();
                }
            }

        };

        initialInvoker.invokeExecutor( executor, task );

        assertTrue( "Task did not complete - executor likely deadlocked",
                latch.await( 5, TimeUnit.SECONDS ) );

        assertNotNull( "Expected exception thrown", caughtEx.get() );
        assertEquals( "Caught exception type", TestDeadlockException.class, caughtEx.get().getClass() );
    }

    @Test
    public void testListenableFutureCallbackWithExecutor() throws InterruptedException {

        String listenerThreadPrefix = "ListenerThread";
        ExecutorService listenerExecutor = Executors.newFixedThreadPool( 1,
                new ThreadFactoryBuilder().setNameFormat( listenerThreadPrefix + "-%d" ).build() );

        executor = new DeadlockDetectingListeningExecutorService(
                Executors.newSingleThreadExecutor(
                        new ThreadFactoryBuilder().setNameFormat( "SingleThread" ).build() ),
                        DEADLOCK_EXECUTOR_SUPPLIER, listenerExecutor );

        try {
            testListenerCallback( executor, SUBMIT_CALLABLE, listenerThreadPrefix );
            testListenerCallback( executor, SUBMIT_RUNNABLE, listenerThreadPrefix );
            testListenerCallback( executor, SUBMIT_RUNNABLE_WITH_RESULT, listenerThreadPrefix );
        } finally {
            listenerExecutor.shutdownNow();
        }
    }
}
