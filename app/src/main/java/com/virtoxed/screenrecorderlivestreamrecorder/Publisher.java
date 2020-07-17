package com.virtoxed.screenrecorderlivestreamrecorder;

import android.media.projection.MediaProjection;

import androidx.annotation.NonNull;

public interface Publisher {

    /**
     * switch camera mode between {@link CameraMode#FRONT} and {@link CameraMode#BACK}
     */


    /**
     * start publishing video and audio data
     */
    void startPublishing();

    /**
     * stop publishing video and audio data.
     */
    void stopPublishing();

    /**
     * @return if the Publisher is publishing data.
     */
    boolean isPublishing();


    class Builder {

        /**
         * Default Values
         */
        public static final int DEFAULT_WIDTH = 720;
        public static final int DEFAULT_HEIGHT = 1280;

        public static final int DEFAULT_WIDTH_LAND = 1280;
        public static final int DEFAULT_HEIGHT_LAND = 720;

        public static final int DEFAULT_AUDIO_BITRATE = 64000;
        public static final int DEFAULT_VIDEO_BITRATE = 4000*1024;
        public static final int DEFAULT_DENSITY = 300;


        /**
         * Required Parameters
         */

        private String url;

        /**
         * Optional Parameters
         */

        private int width;
        private int height;
        private int audioBitrate;
        private int videoBitrate;
        private int density;
        private PublisherListener listener;
        private MediaProjection mediaProjection;



        /**
         * Constructor of the {@link Builder}
         */
        public Builder() {

        }

        /**
         * Set the GLSurfaceView used for preview.
         * this parameter is required
         */

        /**
         * Set the RTMP url
         * this parameter is required
         */
        public Builder setUrl(@NonNull String url) {
            this.url = url;
            return this;
        }

        /**
         * Set the size of video stream.
         * these parameters are optional
         */
        public Builder setSize(int width, int height) {
            this.width = width;
            this.height = height;
            return this;
        }

        /**
         * Set the audio bitrate used for RTMP Streaming
         * this parameter is optional
         */
        public Builder setAudioBitrate(int audioBitrate) {
            this.audioBitrate = audioBitrate;
            return this;
        }

        /**
         * Set the video bitrate used for RTMP Streaming
         * this parameter is optional
         */
        public Builder setVideoBitrate(int videoBitrate) {
            this.videoBitrate = videoBitrate;
            return this;
        }

        /**
         * Set the density used for RTMP Streaming
         * this parameter is optional
         */
        public Builder setDensity(int density) {
            this.density = density;
            return this;
        }

        /**
         * Set the MediaProject to record screen Api 21 above
         * this parameter is optional
         */
        public Builder setMediaProjection(@NonNull MediaProjection projection) {
            this.mediaProjection = projection;
            return this;
        }




        /**
         * Set the {@link PublisherListener}
         * this parameter is optional
         */
        public Builder setListener(PublisherListener listener) {
            this.listener = listener;
            return this;
        }

        /**
         * @return the created RtmpPublisher
         */
        public RtmpPublisher build() {

            if (url == null || url.isEmpty()) {
                throw new IllegalStateException("url should not be empty or null");
            }
            if (url == null || height <= 0) {
                height = DEFAULT_HEIGHT_LAND;
            }
            if (url == null || width <= 0) {
                width = DEFAULT_WIDTH_LAND;
            }
            if (url == null || audioBitrate <= 0) {
                audioBitrate = DEFAULT_AUDIO_BITRATE;
            }
            if (url == null || videoBitrate <= 0) {
                videoBitrate = DEFAULT_VIDEO_BITRATE;
            }
            if (url == null || density <= 0) {
                density = DEFAULT_DENSITY;
            }
            if(mediaProjection == null){
                throw new RuntimeException("MediaProjection is null, please check setMediaProjection(...) method");
            }


            return new RtmpPublisher(url, width, height, audioBitrate, videoBitrate, density, listener, mediaProjection);
        }

    }
}
