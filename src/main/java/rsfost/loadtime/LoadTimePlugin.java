/*
 * Copyright (c) 2024, rsfost <https://github.com/rsfost>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package rsfost.loadtime;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.Value;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.Constants;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.DrawManager;
import net.runelite.client.util.Text;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@PluginDescriptor(
	name = "Load Time",
	description = "Estimate load times when moving over a specified distance"
)
public class LoadTimePlugin extends Plugin
{
	@Inject
	private Client client;
	@Inject
	private FrameListener frameListener;
	@Inject
	private EventBus eventBus;
	@Inject
	private DrawManager drawManager;
	@Inject
	private LoadTimeConfig config;
	@Inject
	private ChatMessageManager chatMessageManager;

	private Collection<Integer> regions;

	private WorldPoint lastWp;
	private long lastGameTickTime;
	private int lastRegionId;

	@Getter(AccessLevel.PACKAGE)
	@Setter(AccessLevel.PACKAGE)
	@Accessors(fluent = true)
	private boolean shouldAnnounce;

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		final long currentTime = System.currentTimeMillis();
		final WorldPoint currentWp = client.getLocalPlayer().getWorldLocation();
		final int currentRegionId = currentWp.getRegionID();

		if (lastWp != null && lastWp.distanceTo(currentWp) >= config.distanceThreshold() && includeRegion())
		{
			final long loadTime = Math.max(0,
				currentTime - lastGameTickTime - Constants.GAME_TICK_LENGTH);
			shouldAnnounce = true;
			announceLoadTime(loadTime, 1);
		}

		lastGameTickTime = currentTime;
		lastWp = currentWp;
		lastRegionId = currentRegionId;
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!event.getGroup().equals(LoadTimeConfig.GROUP))
		{
			return;
		}
		parseRegionIds();
	}

	@Override
	protected void startUp() throws Exception
	{
		parseRegionIds();
		drawManager.registerEveryFrameListener(frameListener);
		eventBus.register(frameListener);
	}

	@Override
	protected void shutDown() throws Exception
	{
		drawManager.unregisterEveryFrameListener(frameListener);
		eventBus.unregister(frameListener);
	}

	@Provides
	LoadTimeConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(LoadTimeConfig.class);
	}

	private boolean includeRegion()
	{
		if (regions.isEmpty())
		{
			return true;
		}

		final int currentRegionId = client.getLocalPlayer().getWorldLocation().getRegionID();

		switch (config.regionMode())
		{
			case DESTINATION_OR_ORIGIN:
				return regions.contains(lastRegionId) || regions.contains(currentRegionId);
			case DESTINATION_ONLY:
				return regions.contains(currentRegionId);
			case ORIGIN_ONLY:
				return regions.contains(lastRegionId);
		}
		return true;
	}

	void announceLoadTime(long time, int method)
	{
		final Color color;
		if (time < config.fastLoadTime())
		{
			color = config.fastLoadColor();
		}
		else if (time < config.mediumLoadTime())
		{
			color = config.mediumLoadColor();
		}
		else
		{
			color = config.slowLoadColor();
		}

		String runeliteMsg = new ChatMessageBuilder()
			.append(color, String.format("(method %d) Load time: %dms", method, time))
			.build();
		chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.CONSOLE)
			.runeLiteFormattedMessage(runeliteMsg)
			.build());
	}

	private void parseRegionIds()
	{
		String regionsConfig = config.regions();
		if (regionsConfig == null)
		{
			regions = new ArrayList<>(0);
			return;
		}

		regions = Text.fromCSV(regionsConfig).stream()
			.map(str ->
			{
				try
				{
					return Integer.parseInt(str);
				}
				catch (NumberFormatException ex)
				{
					return null;
				}
			})
			.filter(Objects::nonNull)
			.collect(Collectors.toSet());
	}
}
