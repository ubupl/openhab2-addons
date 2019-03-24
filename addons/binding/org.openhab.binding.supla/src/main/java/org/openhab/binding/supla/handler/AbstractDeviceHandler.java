package org.openhab.binding.supla.handler;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.HSBType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.OpenClosedType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.library.types.UpDownType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.eclipse.smarthome.core.thing.ThingStatus.OFFLINE;
import static org.eclipse.smarthome.core.thing.ThingStatusDetail.CONFIGURATION_ERROR;

abstract class AbstractDeviceHandler extends BaseThingHandler {
    private final Logger logger = LoggerFactory.getLogger(AbstractDeviceHandler.class);

    AbstractDeviceHandler(final Thing thing) {
        super(thing);
    }

    @SuppressWarnings("deprecation")
    @Override
    public final void initialize() {
        try {
            internalInitialize();
        } catch (Exception e) {
            logger.error("Error occurred while initializing Supla device!", e);
            updateStatus(OFFLINE, CONFIGURATION_ERROR,
                    "Error occurred while initializing Supla device! " + e.getLocalizedMessage());
        }
    }

    protected abstract void internalInitialize() throws Exception;

    @Override
    public final void handleCommand(final ChannelUID channelUID, final Command command) {
        try {
            if (command instanceof RefreshType) {
                handleRefreshCommand(channelUID);
            } else if (command instanceof OnOffType) {
                handleOnOffCommand(channelUID, (OnOffType) command);
            } else if (command instanceof UpDownType) {
                handleUpDownCommand(channelUID, (UpDownType) command);
            } else if (command instanceof HSBType) {
                handleHsbCommand(channelUID, (HSBType) command);
            } else if (command instanceof OpenClosedType) {
                handleOpenClosedCommand(channelUID, (OpenClosedType) command);
            } else if (command instanceof PercentType) {
                handlePercentCommand(channelUID, (PercentType) command);
            } else if (command instanceof DecimalType) {
                handleDecimalCommand(channelUID, (DecimalType) command);
            } else {
                logger.warn("Does not know how to handle command `{}` on channel `{}`!", command, channelUID);
            }
        } catch (Exception ex) {
            logger.error("Error occurred while handling command `{}` on channel `{}`!",
                    command, channelUID, ex);
        }
    }

    protected abstract void handleRefreshCommand(@NonNull final ChannelUID channelUID) throws Exception;

    protected abstract void handleOnOffCommand(@NonNull final ChannelUID channelUID, @NonNull final OnOffType command) throws Exception;

    protected abstract void handleUpDownCommand(@NonNull final ChannelUID channelUID, @NonNull final UpDownType command) throws Exception;

    protected abstract void handleHsbCommand(@NonNull final ChannelUID channelUID, @NonNull final HSBType command) throws Exception;

    protected abstract void handleOpenClosedCommand(@NonNull final ChannelUID channelUID, @NonNull final OpenClosedType command) throws Exception;

    protected abstract void handlePercentCommand(@NonNull final ChannelUID channelUID, @NonNull final PercentType command) throws Exception;

    protected abstract void handleDecimalCommand(@NonNull final ChannelUID channelUID, @NonNull final DecimalType command) throws Exception;
}
