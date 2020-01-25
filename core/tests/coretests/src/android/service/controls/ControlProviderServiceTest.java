/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.service.controls;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.PendingIntent;
import android.content.IIntentSender;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.service.controls.actions.CommandAction;
import android.service.controls.actions.ControlAction;
import android.service.controls.actions.ControlActionWrapper;

import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.function.Consumer;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class ControlProviderServiceTest {

    private IBinder mToken = new Binder();
    @Mock
    private IControlsActionCallback.Stub mActionCallback;
    @Mock
    private IControlsLoadCallback.Stub mLoadCallback;
    @Mock
    private IControlsSubscriber.Stub mSubscriber;
    @Mock
    private IIntentSender mIIntentSender;

    private PendingIntent mPendingIntent;
    private FakeControlsProviderService mControlsProviderService;

    private IControlsProvider mControlsProvider;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        when(mActionCallback.asBinder()).thenCallRealMethod();
        when(mActionCallback.queryLocalInterface(any())).thenReturn(mActionCallback);
        when(mLoadCallback.asBinder()).thenCallRealMethod();
        when(mLoadCallback.queryLocalInterface(any())).thenReturn(mLoadCallback);
        when(mSubscriber.asBinder()).thenCallRealMethod();
        when(mSubscriber.queryLocalInterface(any())).thenReturn(mSubscriber);

        Bundle b = new Bundle();
        b.putBinder(ControlsProviderService.CALLBACK_TOKEN, mToken);
        Intent intent = new Intent();
        intent.putExtra(ControlsProviderService.CALLBACK_BUNDLE, b);

        mPendingIntent = new PendingIntent(mIIntentSender);

        mControlsProviderService = new FakeControlsProviderService();
        mControlsProvider = IControlsProvider.Stub.asInterface(
                mControlsProviderService.onBind(intent));
    }

    @Test
    public void testOnLoad_allStateless() throws RemoteException {
        Control control1 = new Control.StatelessBuilder("TEST_ID", mPendingIntent).build();
        Control control2 = new Control.StatelessBuilder("TEST_ID_2", mPendingIntent)
                .setDeviceType(DeviceTypes.TYPE_AIR_FRESHENER).build();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Control>> captor = ArgumentCaptor.forClass(List.class);

        ArrayList<Control> list = new ArrayList<>();
        list.add(control1);
        list.add(control2);

        mControlsProviderService.setControls(list);
        mControlsProvider.load(mLoadCallback);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        verify(mLoadCallback).accept(eq(mToken), captor.capture());
        List<Control> l = captor.getValue();
        assertEquals(2, l.size());
        assertTrue(equals(control1, l.get(0)));
        assertTrue(equals(control2, l.get(1)));
    }

    @Test
    public void testOnLoad_statefulConvertedToStateless() throws RemoteException {
        Control control = new Control.StatefulBuilder("TEST_ID", mPendingIntent)
                .setTitle("TEST_TITLE")
                .setStatus(Control.STATUS_OK)
                .build();
        Control statelessControl = new Control.StatelessBuilder(control).build();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Control>> captor = ArgumentCaptor.forClass(List.class);

        ArrayList<Control> list = new ArrayList<>();
        list.add(control);

        mControlsProviderService.setControls(list);
        mControlsProvider.load(mLoadCallback);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        verify(mLoadCallback).accept(eq(mToken), captor.capture());
        List<Control> l = captor.getValue();
        assertEquals(1, l.size());
        assertFalse(equals(control, l.get(0)));
        assertTrue(equals(statelessControl, l.get(0)));
        assertEquals(Control.STATUS_UNKNOWN, l.get(0).getStatus());
    }

    @Test
    public void testSubscribe() throws RemoteException {
        Control control = new Control.StatefulBuilder("TEST_ID", mPendingIntent)
                .setTitle("TEST_TITLE")
                .setStatus(Control.STATUS_OK)
                .build();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Control> controlCaptor =
                ArgumentCaptor.forClass(Control.class);
        ArgumentCaptor<IControlsSubscription.Stub> subscriptionCaptor =
                ArgumentCaptor.forClass(IControlsSubscription.Stub.class);

        ArrayList<Control> list = new ArrayList<>();
        list.add(control);

        mControlsProviderService.setControls(list);

        mControlsProvider.subscribe(new ArrayList<String>(), mSubscriber);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        verify(mSubscriber).onSubscribe(eq(mToken), subscriptionCaptor.capture());
        subscriptionCaptor.getValue().request(1);

        verify(mSubscriber).onNext(eq(mToken), controlCaptor.capture());
        Control c = controlCaptor.getValue();
        assertTrue(equals(c, list.get(0)));
    }

    @Test
    public void testOnAction() throws RemoteException {
        mControlsProvider.action("TEST_ID", new ControlActionWrapper(
                new CommandAction("", null)), mActionCallback);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        verify(mActionCallback).accept(mToken, "TEST_ID",
                ControlAction.RESPONSE_OK);
    }

    private static boolean equals(Control c1, Control c2) {
        if (c1 == c2) return true;
        if (c1 == null || c2 == null) return false;
        return Objects.equals(c1.getControlId(), c2.getControlId())
                && c1.getDeviceType() == c2.getDeviceType()
                && Objects.equals(c1.getTitle(), c2.getTitle())
                && Objects.equals(c1.getSubtitle(), c2.getSubtitle())
                && Objects.equals(c1.getStructure(), c2.getStructure())
                && Objects.equals(c1.getZone(), c2.getZone())
                && Objects.equals(c1.getAppIntent(), c2.getAppIntent())
                && Objects.equals(c1.getCustomIcon(), c2.getCustomIcon())
                && Objects.equals(c1.getCustomColor(), c2.getCustomColor())
                && c1.getStatus() == c2.getStatus()
                && Objects.equals(c1.getControlTemplate(), c2.getControlTemplate())
                && Objects.equals(c1.getStatusText(), c2.getStatusText());
    }

    static class FakeControlsProviderService extends ControlsProviderService {

        private List<Control> mControls;

        public void setControls(List<Control> controls) {
            mControls = controls;
        }

        @Override
        public void loadAvailableControls(Consumer<List<Control>> cb) {
            cb.accept(mControls);
        }

        @Override
        public Publisher<Control> publisherFor(List<String> ids) {
            return new Publisher<Control>() {
                public void subscribe(final Subscriber s) {
                    s.onSubscribe(new Subscription() {
                            public void request(long n) {
                                for (Control c : mControls) {
                                    s.onNext(c);
                                }
                            }
                            public void cancel() {}
                        });
                }
            };
        }

        @Override
        public void performControlAction(String controlId, ControlAction action,
                Consumer<Integer> cb) {
            cb.accept(ControlAction.RESPONSE_OK);
        }
    }
}

