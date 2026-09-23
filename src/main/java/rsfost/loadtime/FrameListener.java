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

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.Scene;
import net.runelite.api.events.PreMapLoad;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
import javax.inject.Inject;

@Slf4j
class FrameListener implements Runnable
{
	private static final long NANOS_PER_MILLI = 1_000_000L;
	// how often the watcher thread checks whether the map loader has started running
	private static final long WATCH_INTERVAL_NANOS = 500_000L;

	private final LoadTimePlugin plugin;
	private final Client client;
	private final ClientThread clientThread;

	private volatile Thread mapLoader;
	private Scene lastScene;
	private long lastFrameTime;
	private long mapLoadStartTime = -1;

	// The frame listener only notices the loader thread running at the end of a frame, so on its own it
	// starts the clock up to a frame late and understates every load by that much (up to 20 ms at 50 fps).
	// A watcher thread polls the loader's state every WATCH_INTERVAL_NANOS instead and records the start
	// here; the frame listener uses it when set and falls back to its own detection otherwise.
	private final AtomicLong watchedStartTime = new AtomicLong(-1);
	private Thread watcher;
	private volatile boolean watching;

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

	void reset()
	{
		mapLoader = null;
		lastScene = null;
		lastFrameTime = 0;
		mapLoadStartTime = -1;
		watchedStartTime.set(-1);
	}

	void startWatcher()
	{
		stopWatcher();
		watching = true;
		Thread t = new Thread(this::watch, "Load Time watcher");
		t.setDaemon(true);
		t.start();
		watcher = t;
	}

	void stopWatcher()
	{
		// no interrupt: the loop checks the flag every interval, and unpark ends the current wait early
		watching = false;
		Thread t = watcher;
		watcher = null;
		if (t != null)
		{
			LockSupport.unpark(t);
		}
	}

	private void watch()
	{
		boolean wasRunning = false;
		while (watching)
		{
			Thread loader = mapLoader;
			boolean running = loader != null && loader.getState() == Thread.State.RUNNABLE;
			if (running && !wasRunning)
			{
				// the loader may briefly sleep during a build (waiting for cache data) and run again;
				// only the first start since the last scene change counts
				watchedStartTime.compareAndSet(-1, System.nanoTime());
			}
			wasRunning = running;
			LockSupport.parkNanos(WATCH_INTERVAL_NANOS);
		}
	}

	@Override
	public void run()
	{
		final long currentTime = System.nanoTime();
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			lastScene = null;
			lastFrameTime = currentTime;
			watchedStartTime.set(-1);
			return;
		}

		Scene scene = player.getWorldView().getScene();
		if (lastScene == null)
		{
			// first frame with a scene: the load that built it started before we were tracking
			watchedStartTime.set(-1);
		}

		if (lastScene != null && lastScene != scene)
		{
			final long watchedStart = watchedStartTime.getAndSet(-1);
			if (watchedStart >= 0)
			{
				log.debug("Map load start from watcher, {} ms before the frame listener saw it",
					mapLoadStartTime < 0 ? -1 : (mapLoadStartTime - watchedStart) / NANOS_PER_MILLI);
				mapLoadStartTime = watchedStart;
			}
			else if (mapLoadStartTime < 0)
			{
				log.debug("Scene change with map load start <0. Using last frame time as load start.");
				mapLoadStartTime = lastFrameTime;
			}
			final long loadTime = (currentTime - mapLoadStartTime) / NANOS_PER_MILLI;
			final int startTick = client.getTickCount();
			clientThread.invokeLater(() ->
				plugin.announceLoadTime(loadTime) || client.getTickCount() != startTick);
			mapLoadStartTime = -1;
		}
		else if (mapLoader != null && mapLoader.getState() == Thread.State.RUNNABLE && mapLoadStartTime < 0)
		{
			mapLoadStartTime = currentTime;
			log.debug("Map load started at {}", mapLoadStartTime);
		}
		lastScene = scene;
		lastFrameTime = currentTime;
	}
}
