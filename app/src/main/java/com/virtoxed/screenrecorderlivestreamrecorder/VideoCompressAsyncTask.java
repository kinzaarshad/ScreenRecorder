package com.virtoxed.screenrecorderlivestreamrecorder;
import android.content.Context;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.widget.MediaController;
import android.widget.Toast;

import com.virtoxed.screenrecorderlivestreamrecorder.videocompressor.VideoCompress;

import java.io.File;
import java.util.HashMap;

public class VideoCompressAsyncTask extends AsyncTask<String, String, String> {

    VideoCompress.CompressListener lis ;
    Context mContext;
    int dis = 0;

    public VideoCompressAsyncTask(Context context ){
        mContext = context;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }


    @Override
    protected String doInBackground(final String... paths) {

        try {

            VideoCompress.compressVideoMedium(paths[0], paths[1], lis = new VideoCompress.CompressListener() {

                @Override
                public void onStart() {

                }

                @Override
                public void onSuccess() {


                    if (dis == 0) {

                        SharedPrefUtil.getInstance(mContext).putString("1", "1");
                        CompressVideo.showfile();
                        Toast.makeText(mContext, "Compressed Video Saved in Gallery", Toast.LENGTH_SHORT).show();


                    } else {

                        File n = new File(paths[1]);
                        if (n.exists()) {
                            n.delete();
                        }
                    }
                }

                @Override
                public void onFail() {

                    Toast.makeText(mContext, "Failed to compress :", Toast.LENGTH_SHORT).show();

                }

                @Override
                public void onProgress(float percent) {

                    int p = (int) percent;
                    String st = SharedPrefUtil.getInstance(mContext).getString("stop");
                    if (dis == 0) {
                        if (st.equalsIgnoreCase("1")) {

                            dis = 1;
                        } else {

                            CompressVideo.updatepro(p);

                        }

                    }

                }


            });

        }catch (Exception e){
            System.out.println("Error = "+e.getMessage().toString());

        }

        return null;
    }

    @Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);

    }

    @Override
    protected void onPostExecute(String compressedFilePath) {
        super.onPostExecute(compressedFilePath);

    }

}
