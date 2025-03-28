/*
 * Copyright (c) 2025, rsfost <https://github.com/rsfost>
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

import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.Scene;
import net.runelite.api.events.PreMapLoad;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;

import javax.inject.Inject;

class FrameListener implements Runnable
{
	private static final long NANOS_PER_MILLI = 1_000_000L;

	private final LoadTimePlugin plugin;
	private final Client client;
	private final ClientThread clientThread;

	private Thread mapLoader;
	private Scene lastScene;
	private long mapLoadStartTime;

	@Inject
	public FrameListener(LoadTimePlugin plugin, Client client, ClientThread clientThread)
	{
		this.plugin = plugin;
		this.client = client;
		this.clientThread = clientThread;
	}

	@Subscribe
	public void onPreMapLoad(PreMapLoad event)
	{
		mapLoader = Thread.currentThread();
	}

	@Override
	public void run()
	{
		final long currentTime = System.nanoTime();
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			lastScene = null;
			return;
		}

		Scene scene = player.getWorldView().getScene();
		if (lastScene != null && lastScene != scene)
		{
			final long loadTime = (currentTime - mapLoadStartTime) / NANOS_PER_MILLI;
			final int startTick = client.getTickCount();
			clientThread.invokeLater(() ->
				plugin.announceLoadTime(loadTime) || client.getTickCount() != startTick);
			mapLoadStartTime = -1;
		}
		else if (mapLoader != null && mapLoader.getState() == Thread.State.RUNNABLE && mapLoadStartTime < 0)
		{
			mapLoadStartTime = currentTime;
		}
		lastScene = scene;
	}
}
