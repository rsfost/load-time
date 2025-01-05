package rsfost.loadtime;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FpsCalc
{
    private long frameCount = 0;
    private long lastSecondTime;

    public void frame()
    {
        ++frameCount;

        long currentTime = System.currentTimeMillis();
        long diff = currentTime - lastSecondTime;
        if (diff >= 1000)
        {
            float seconds = (float)diff/1000f;
            int fps = (int)((float)frameCount / seconds);
            log.info("fps: {}", fps);

            frameCount = 0;
            lastSecondTime = currentTime;
        }
    }
}
