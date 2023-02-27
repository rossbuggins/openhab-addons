/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.transform.actionprofile.internal.profiles;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.profiles.ProfileCallback;
import org.openhab.core.thing.profiles.ProfileContext;
import org.openhab.core.thing.profiles.ProfileTypeUID;
import org.openhab.core.thing.profiles.StateProfile;
import org.openhab.core.transform.TransformationService;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Profile to offer the MapTransformationservice on an ItemChannelLink
 *
 * @author Stefan Triller - Initial contribution
 */
@NonNullByDefault
public class ActionProfile implements StateProfile {
    public static final ProfileTypeUID PROFILE_TYPE_UID = new ProfileTypeUID(
            TransformationService.TRANSFORM_PROFILE_SCOPE, "ACTION");

    private final Logger logger = LoggerFactory.getLogger(ActionProfile.class);

    private final TransformationService service;
    private final ProfileCallback callback;

    public ActionProfile(ProfileCallback callback, ProfileContext context, TransformationService service) {
        this.service = service;
        this.callback = callback;
    }

    @Override
    public ProfileTypeUID getProfileTypeUID() {
        return PROFILE_TYPE_UID;
    }

    @Override
    public void onStateUpdateFromItem(State state) {
        var thisState = state.toString();
        var newCommand = new StringType("ON");
        callback.handleCommand(newCommand);
    }

    @Override
    public void onCommandFromItem(Command command) {
    }

    @Override
    public void onCommandFromHandler(Command command) {
    }

    @Override
    public void onStateUpdateFromHandler(State state) {
    }
}
