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

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

import java.awt.Color;

@ConfigGroup(LoadTimeConfig.GROUP)
public interface LoadTimeConfig extends Config
{
    String GROUP = "loadtime";

    @ConfigSection(
        name = "Color options",
        description = "How to color load time message for certain load times",
        position = 100,
        closedByDefault = false
    )
    String colorOptionsSection = "colorOptions";

    @ConfigItem(
        keyName = "distanceThreshold",
        name = "Distance threshold",
        description = "Minimum distance moved in a single game tick to calculate load time",
        position = 1
    )
    default int distanceThreshold()
    {
        return 50;
    }

    @ConfigItem(
        keyName = "regions",
        name = "Region IDs",
        description = "Comma-separated list of region IDs to include for load time calculation",
        position = 2
    )
    default String regions()
    {
        return "";
    }

    @ConfigItem(
        keyName = "regionMode",
        name = "Region mode",
        description = "Whether to treat above list as destination or origin regions, or both",
        position = 3
    )
    default RegionMode regionMode()
    {
        return RegionMode.DESTINATION_ONLY;
    }

    @ConfigItem(
        keyName = "fastLoadTime",
        name = "Fast load time",
        description = "Maximum fast load time",
        position = 1,
        section = colorOptionsSection
    )
    default int fastLoadTime()
    {
        return 50;
    }

    @ConfigItem(
        keyName = "fastLoadTimeColor",
        name = "Fast load color",
        description = "Color for fast load times",
        position = 2,
        section = colorOptionsSection
    )
    default Color fastLoadColor()
    {
        return Color.GREEN;
    }

    @ConfigItem(
        keyName = "mediumLoadTime",
        name = "Medium load time",
        description = "Maximum medium load time",
        position = 3,
        section = colorOptionsSection
    )
    default int mediumLoadTime()
    {
        return 100;
    }

    @ConfigItem(
        keyName = "mediumLoadTimeColor",
        name = "Medium load color",
        description = "Color for medium load times",
        position = 4,
        section = colorOptionsSection
    )
    default Color mediumLoadColor()
    {
        return Color.YELLOW;
    }

    @ConfigItem(
        keyName = "slowLoadTimeColor",
        name = "Slow load color",
        description = "Color for slow load times",
        position = 5,
        section = colorOptionsSection
    )
    default Color slowLoadColor()
    {
        return Color.RED;
    }
}
