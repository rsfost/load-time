package rsfost.loadtime;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class LoadTimePluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(LoadTimePlugin.class);
		RuneLite.main(args);
	}
}