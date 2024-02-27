package com.play.stream.Starjams.MusicService.services;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import org.freedesktop.gstreamer.Bus;
import org.freedesktop.gstreamer.Gst;
import org.freedesktop.gstreamer.Pipeline;

import java.io.File;
import java.util.UUID;

import static org.apache.kafka.common.utils.Sanitizer.sanitize;

public class GStreamerStreamer {
    private Pipeline pipeline;
    private boolean saveStream;
    private String outputFilePath;
    private AmazonS3 s3client;
    private final CqlSession cqlSession;

    public GStreamerStreamer(String sourceUrl, boolean saveStream, CqlSession session) {
        this.saveStream = saveStream;
        this.cqlSession = session;
        if (saveStream) {
            // Initialize AWS S3 Client
            s3client = AmazonS3ClientBuilder.standard().build();
            // Initialize ScyllaDB Session
            outputFilePath = "local/path/to/save/" + UUID.randomUUID() + ".mp4"; // Example path, adjust as necessary
            String query = "INSERT INTO save_audio_streams (id, audio_string, created_at) VALUES (uuid(), 'Your text string here', toTimestamp(now()));\n";
            ResultSet resultSet = cqlSession.execute(query, sanitize(query));
        }
        Gst.init("GStreamerStreamer");
        setupPipeline(sourceUrl);
    }

    private void setupPipeline(String sourceUrl) {
        String basePipeline = "rtspsrc location=%s ! decodebin";
        String endPipeline = saveStream ? " ! x264enc ! mp4mux ! filesink location=" + outputFilePath : " ! autoaudiosink";
        String pipelineDesc = String.format(basePipeline + endPipeline, sourceUrl);
        pipeline = (Pipeline) Gst.parseLaunch(pipelineDesc);
        pipeline.getBus().connect((Bus.TAG) (bus, message) -> System.out.println(message.toString()));
    }

    public void start() {
        if (pipeline != null) {
            pipeline.play();
        }
    }

    public void stop() {
        if (pipeline != null) {
            pipeline.stop();
            if (saveStream) {
                saveAndStoreStreamInfo();
            }
        }
    }

    private void saveAndStoreStreamInfo() {
        // Save to S3
        String bucketName = "your-bucket-name";
        String s3Key = "saved_streams/" + new File(outputFilePath).getName();
        s3client.putObject(new PutObjectRequest(bucketName, s3Key, new File(outputFilePath)));

        // Generate S3 URL
        String s3Url = s3client.getUrl(bucketName, s3Key).toString();

        // Save URL in ScyllaDB
        String query = "INSERT INTO saved_audio_streams (id, s3_url) VALUES (?, ?)";
        ResultSet resultSet = cqlSession.execute(query, sanitize(query));
        //dbSession.execute(insertCQL, UUID.randomUUID(), s3Url);

        System.out.println("Stream saved to S3 and URL stored in ScyllaDB: " + s3Url);
    }

    // Include main method and any additional logic as needed
}
